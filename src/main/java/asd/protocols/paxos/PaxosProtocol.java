package asd.protocols.paxos;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.Membership;
import asd.paxos.Paxos;
import asd.paxos.PaxosCmd;
import asd.paxos.PaxosConfig;
import asd.paxos.PaxosLog;
import asd.paxos.ProcessId;
import asd.paxos.Proposal;
import asd.paxos.multi.MultiPaxos;
import asd.paxos.single.SinglePaxos;
import asd.protocols.PaxosBabel;
import asd.protocols.agreement.Agreement;
import asd.protocols.agreement.notifications.DecidedNotification;
import asd.protocols.agreement.notifications.JoinedNotification;
import asd.protocols.agreement.notifications.LeaderChanged;
import asd.protocols.agreement.requests.MemberAddRequest;
import asd.protocols.agreement.requests.ProposeRequest;
import asd.protocols.agreement.requests.MemberRemoveRequest;
import asd.protocols.agreement.requests.MembershipUnchangedRequest;
import asd.protocols.paxos.messages.AcceptOkMessage;
import asd.protocols.paxos.messages.AcceptRequestMessage;
import asd.protocols.paxos.messages.DecidedMessage;
import asd.protocols.paxos.messages.PrepareOkMessage;
import asd.protocols.paxos.messages.PrepareRequestMessage;
import asd.protocols.paxos.timer.DebugTimer;
import asd.protocols.paxos.timer.PaxosTimer;
import asd.protocols.statemachine.notifications.ChannelReadyNotification;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;

public class PaxosProtocol extends GenericProtocol implements Agreement {
	private static final Logger logger = LogManager.getLogger(PaxosProtocol.class);

	public final static String NAME = "Paxos";

	private final static String VARIANT_SINGLE = "single";
	private final static String VARIANT_MULTI = "multi";

	private ProcessId id;
	private boolean joined;
	private Paxos paxos;
	private final String paxosVariant;
	private final Map<Pair<Integer, Integer>, Long> timers;

	public PaxosProtocol(Properties props) throws HandlerRegistrationException {
		super(NAME, ID);

		this.id = null;
		this.joined = false;
		this.paxos = null;
		this.paxosVariant = props.getProperty("paxos_variant");
		this.timers = new HashMap<>();

		/*--------------------- Register Timer Handlers ----------------------------- */
		this.registerTimerHandler(PaxosTimer.ID, this::onPaxosTimer);
		this.registerTimerHandler(DebugTimer.ID, this::onDebugTimer);

		/*--------------------- Register Request Handlers ----------------------------- */
		this.registerRequestHandler(ProposeRequest.ID, this::onProposeRequest);
		this.registerRequestHandler(MemberAddRequest.ID, this::onAddReplicaRequest);
		this.registerRequestHandler(MemberRemoveRequest.ID, this::onRemoveReplicaRequest);
		this.registerRequestHandler(MembershipUnchangedRequest.ID, this::onMembershipUnchanged);

		/*--------------------- Register Notification Handlers ----------------------------- */
		this.subscribeNotification(JoinedNotification.ID, this::onJoined);
		this.subscribeNotification(ChannelReadyNotification.ID, this::onChannelReady);

		/*-------------------- Register Channel Event ------------------------------- */

	}

	@Override
	public void init(Properties props) throws HandlerRegistrationException, IOException {
		this.setupPeriodicTimer(new DebugTimer(), 0, 5000);
	}

	/*--------------------------------- Helpers ---------------------------------------- */

	private void execute() {
		while (!this.paxos.isEmpty()) {
			var command = this.paxos.pop();
			this.process(command);
		}
	}

	private void process(PaxosCmd command) {
		switch (command.getKind()) {
			case DECIDE -> {
				var cmd = command.getDecide();
				var notification = new DecidedNotification(cmd.slot(), cmd.value().data);
				this.triggerNotification(notification);
				logger.trace("Triggered DecidedNotification for instance {}", cmd.slot());
			}
			case SETUP_TIMER -> {
				var cmd = command.getSetupTimer();
				var timer = new PaxosTimer(cmd.slot(), cmd.timerId());
				var timeout = cmd.timeout().toMillis();
				logger.debug("Setting up timer {} for instance {} with timeout {}ms", cmd.timerId(),
						cmd.slot(),
						timeout);
				var babelTimerId = this.setupTimer(timer, timeout);
				var timerPair = Pair.of(cmd.slot(), cmd.timerId());
				this.timers.put(timerPair, babelTimerId);
			}
			case CANCEL_TIMER -> {
				var cmd = command.getCancelTimer();
				var timerPair = Pair.of(cmd.slot(), cmd.timerId());
				var babelTimerId = this.timers.remove(timerPair);
				if (babelTimerId != null) {
					logger.debug("Cancelling timer {} for instance {}", cmd.timerId(), cmd.slot());
					this.cancelTimer(babelTimerId);
				}
			}
			case ACCEPT_OK -> {
				var acceptOk = command.getAcceptOk();
				var message = new AcceptOkMessage(
						acceptOk.slot(),
						acceptOk.ballot());

				assert !acceptOk.processId().equals(this.id);
				var host = PaxosBabel.hostFromProcessId(acceptOk.processId());
				logger.trace("Sending AcceptOkMessage to {}", host);
				this.sendMessage(message, host);
			}
			case ACCEPT_REQUEST -> {
				var accept = command.getAcceptRequest();
				var membership = this.paxos.membership(accept.slot());
				var message = new AcceptRequestMessage(accept.slot(), membership.acceptors, accept.proposal());

				assert !accept.processId().equals(this.id);
				var host = PaxosBabel.hostFromProcessId(accept.processId());
				logger.trace("Sending AcceptRequestMessage to {}", host);
				this.sendMessage(message, host);
			}
			case LEARN -> {
				var decided = command.getLearn();
				var membership = this.paxos.membership(decided.slot());
				var message = new DecidedMessage(decided.slot(), membership.acceptors, decided.value());

				assert !decided.processId().equals(this.id);
				var host = PaxosBabel.hostFromProcessId(decided.processId());
				logger.trace("Sending DecidedMessage to {}", host);
				this.sendMessage(message, host);
			}
			case PREPARE_OK -> {
				var prepareOk = command.getPrepareOk();
				var message = new PrepareOkMessage(
						prepareOk.slot(),
						prepareOk.ballot(),
						prepareOk.highestAccept(),
						false);

				assert !prepareOk.processId().equals(this.id);
				var host = PaxosBabel.hostFromProcessId(prepareOk.processId());
				logger.trace("Sending PrepareOkMessage to {}", host);
				this.sendMessage(message, host);
			}
			case PREPARE_REQUEST -> {
				var prepare = command.getPrepareRequest();
				var membership = this.paxos.membership(prepare.slot());
				var message = new PrepareRequestMessage(
						prepare.slot(),
						membership.acceptors,
						prepare.ballot());

				assert !prepare.processId().equals(this.id);
				var host = PaxosBabel.hostFromProcessId(prepare.processId());
				logger.trace("Sending PrepareRequestMessage to {}", host);
				this.sendMessage(message, host);
			}
			case NEW_LEADER -> {
				var newLeader = command.getNewLeader();
				var host = PaxosBabel.hostFromProcessId(newLeader.leader());
				var notification = new LeaderChanged(host, newLeader.pending());
				this.triggerNotification(notification);
			}
			case PROPOSE, MEMBER_ADDED, MEMBER_REMOVED, MEMBERSHIP_DISCOVERED, MEMBERSHIP_UNCHANGED, TIMER_EXPIRED -> {
				logger.error("Unexpected command {}", command);
				throw new IllegalStateException("Unexpected command " + command);
			}
		}
	}

	/*--------------------------------- Timer Handlers ---------------------------------------- */

	private void onPaxosTimer(PaxosTimer timer, long timerId) {
		PaxosLog.withContext(this.id, timer.slot, () -> {
			var timerIdPair = Pair.of(timer.slot, timer.timerId);
			if (!this.timers.containsKey(timerIdPair)) {
				logger.debug("Ignoring canceled timer {} for instance {}", timer.timerId, timer.slot);
				PaxosLog.log("trigger-canceled-timer", "slot", timer.slot);
				return;
			}
			this.paxos.push(PaxosCmd.timerExpired(timer.slot, timer.timerId));
			this.execute();
		});
	}

	private void onDebugTimer(DebugTimer timer, long timerId) {
		if (this.paxos == null)
			return;
		this.paxos.printDebug();
	}

	/*--------------------------------- Message Handlers ---------------------------------------- */

	private void onAcceptOk(AcceptOkMessage msg, Host host, short sourceProto, int channelId) {
		PaxosLog.withContext(this.id, msg.instance, () -> {
			logger.trace("Received AcceptOkMessage from {} on instance {}", host, msg.instance);
			var processId = PaxosBabel.hostToProcessId(host);
			var instance = msg.instance;
			this.paxos.push(PaxosCmd.acceptOk(processId, msg.ballot, instance));
			this.execute();
		});
	}

	private void onAcceptRequest(AcceptRequestMessage msg, Host host, short sourceProto, int channelId) {
		PaxosLog.withContext(this.id, msg.instance, () -> {
			logger.trace("Received accept request from {} for instance {}", host, msg.instance);
			var processId = PaxosBabel.hostToProcessId(host);
			var instance = msg.instance;
			var proposal = new Proposal(msg.proposal.ballot, msg.proposal.value);
			var membership = new Membership(msg.membership);
			this.paxos.push(PaxosCmd.membershipDiscovered(instance, membership));
			this.paxos.push(PaxosCmd.acceptRequest(processId, proposal, instance));
			this.execute();
		});
	}

	private void onDecided(DecidedMessage msg, Host host, short sourceProto, int channelId) {
		PaxosLog.withContext(this.id, msg.instance, () -> {
			logger.trace("Received decided message for instance {}", msg.instance);
			var processId = PaxosBabel.hostToProcessId(host);
			var instance = msg.instance;
			var value = msg.value;
			var membership = new Membership(msg.membership);
			this.paxos.push(PaxosCmd.membershipDiscovered(instance, membership));
			this.paxos.push(PaxosCmd.learn(processId, value, instance));
			this.execute();
		});
	}

	private void onPrepareOk(PrepareOkMessage msg, Host host, short sourceProto, int channelId) {
		PaxosLog.withContext(this.id, msg.instance, () -> {
			logger.trace("Received prepare ok from {} for instance {}", host, msg.instance);
			var processId = PaxosBabel.hostToProcessId(host);
			var instance = msg.instance;
			var proposal = msg.acceptedProposal.map(p -> new Proposal(p.ballot, p.value));
			this.paxos.push(PaxosCmd.prepareOk(processId, msg.ballot, proposal, instance));
			this.execute();
		});
	}

	private void onPrepareRequest(PrepareRequestMessage msg, Host host, short sourceProto, int channelId) {
		PaxosLog.withContext(this.id, msg.instance, () -> {
			logger.trace("Received prepare request from {} for instance {}", host, msg.instance);
			var processId = PaxosBabel.hostToProcessId(host);
			var instance = msg.instance;
			var membership = new Membership(msg.membership);
			this.paxos.push(PaxosCmd.membershipDiscovered(instance, membership));
			this.paxos.push(PaxosCmd.prepareRequest(processId, msg.ballot, instance));
			this.execute();
		});
	}

	/*--------------------------------- Request Handlers ---------------------------------------- */

	private void onProposeRequest(ProposeRequest request, short sourceProto) {
		logger.trace("Received propose request: {}", request);
		this.paxos.push(PaxosCmd.propose(request.command));
		this.execute();
	}

	private void onAddReplicaRequest(MemberAddRequest request, short sourceProto) {
		logger.info("Adding replica to membership: {}", request.replica);
	}

	private void onRemoveReplicaRequest(MemberRemoveRequest request, short sourceProto) {
		logger.info("Removing replica from membership: {}", request.replica);
		this.paxos.push(PaxosCmd.memberRemoved(request.slot, PaxosBabel.hostToProcessId(request.replica)));
		this.execute();
	}

	private void onMembershipUnchanged(MembershipUnchangedRequest request, short sourceProto) {
		logger.trace("Membership unchanged");
		this.paxos.push(PaxosCmd.membershipUnchanged(request.slot));
		this.execute();
	}

	/*--------------------------------- Notification Handlers ---------------------------------------- */

	private void onChannelReady(ChannelReadyNotification notification, short sourceProto) {
		logger.info("Received channel ready notification {}", notification);

		var channelId = notification.getChannelId();
		this.registerSharedChannel(channelId);
		this.id = PaxosBabel.hostToProcessId(notification.getMyself());
		PaxosLog.init("paxos-" + notification.getMyself().getPort() + ".log");

		/*--------------------- Register Message Serializers ---------------------- */
		this.registerMessageSerializer(channelId, AcceptOkMessage.ID, AcceptOkMessage.serializer);
		this.registerMessageSerializer(channelId, AcceptRequestMessage.ID, AcceptRequestMessage.serializer);
		this.registerMessageSerializer(channelId, DecidedMessage.ID, DecidedMessage.serializer);
		this.registerMessageSerializer(channelId, PrepareOkMessage.ID, PrepareOkMessage.serializer);
		this.registerMessageSerializer(channelId, PrepareRequestMessage.ID, PrepareRequestMessage.serializer);

		/*--------------------- Register Message Handlers -------------------------- */
		try {
			this.registerMessageHandler(channelId, AcceptOkMessage.ID, this::onAcceptOk);
			this.registerMessageHandler(channelId, AcceptRequestMessage.ID, this::onAcceptRequest);
			this.registerMessageHandler(channelId, DecidedMessage.ID, this::onDecided);
			this.registerMessageHandler(channelId, PrepareOkMessage.ID, this::onPrepareOk);
			this.registerMessageHandler(channelId, PrepareRequestMessage.ID, this::onPrepareRequest);
		} catch (HandlerRegistrationException e) {
			throw new RuntimeException(e);
		}
	}

	private void onJoined(JoinedNotification notification, short sourceProto) {
		// This does not have to be true since by the time we receive the joined
		// notification,
		// we may have already received messages from other replicas that have started
		// instances
		// after our join instance.
		// assert this.instances.isEmpty();

		logger.info("Joined notification received: {}", notification);

		if (this.joined)
			throw new RuntimeException("Already joined");
		this.joined = true;

		var membership = notification.membership.stream().map(PaxosBabel::hostToProcessId).toList();
		var config = PaxosConfig
				.builder()
				.withInitialSlot(notification.joinInstance)
				.withProposers(membership)
				.withAcceptors(membership)
				.withLearners(membership)
				.build();
		assert membership.contains(this.id);

		this.paxos = switch (this.paxosVariant) {
			case VARIANT_SINGLE -> {
				logger.info("Using single paxos variant");
				yield new SinglePaxos(this.id, config);
			}
			case VARIANT_MULTI -> {
				logger.info("Using multi paxos variant");
				yield new MultiPaxos(this.id, config);
			}
			default -> throw new RuntimeException("Unknown Paxos variant");
		};
	}
}
