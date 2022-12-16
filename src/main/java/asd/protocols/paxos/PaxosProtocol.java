package asd.protocols.paxos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.Membership;
import asd.paxos.Paxos;
import asd.paxos.PaxosCmd;
import asd.paxos.PaxosConfig;
import asd.paxos.ProcessId;
import asd.paxos.Proposal;
import asd.paxos.multi.MultiPaxos;
import asd.paxos.single.SinglePaxos;
import asd.protocols.PaxosBabel;
import asd.protocols.agreement.Agreement;
import asd.protocols.agreement.notifications.DecidedNotification;
import asd.protocols.agreement.notifications.JoinedNotification;
import asd.protocols.agreement.notifications.LeaderChanged;
import asd.protocols.agreement.requests.GcRequest;
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

	// We need to keep track of learned values and membership the moment we join the
	// system so we dont lose them because the Join notification can take some time
	// to arrive. We can replay these commands later when we create the paxos
	// instance.
	private final ArrayList<PaxosCmd> prejoinQueue;

	public PaxosProtocol(Properties props) throws HandlerRegistrationException {
		super(NAME, ID);

		this.id = null;
		this.joined = false;
		this.paxos = null;
		this.paxosVariant = props.getProperty("paxos_variant");
		this.timers = new HashMap<>();
		this.prejoinQueue = new ArrayList<>();

		/*--------------------- Register Timer Handlers ----------------------------- */
		this.registerTimerHandler(PaxosTimer.ID, this::onPaxosTimer);
		this.registerTimerHandler(DebugTimer.ID, this::onDebugTimer);

		/*--------------------- Register Request Handlers ----------------------------- */
		this.registerRequestHandler(ProposeRequest.ID, this::onProposeRequest);
		this.registerRequestHandler(MemberAddRequest.ID, this::onAddReplicaRequest);
		this.registerRequestHandler(MemberRemoveRequest.ID, this::onRemoveReplicaRequest);
		this.registerRequestHandler(MembershipUnchangedRequest.ID, this::onMembershipUnchanged);
		this.registerRequestHandler(GcRequest.ID, this::onGcRequest);

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

	private void execute(PaxosCmd... commands) {
		this.paxos.push(commands);
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
				logger.trace("Triggered DecidedNotification for slot {}", cmd.slot());
			}
			case SETUP_TIMER -> {
				var cmd = command.getSetupTimer();
				var timer = new PaxosTimer(cmd.slot(), cmd.timerId());
				var timeout = cmd.timeout().toMillis();
				logger.trace("Setting up timer {} for slot {} with timeout {}ms", cmd.timerId(),
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
					logger.trace("Cancelling timer {} for slot {}", cmd.timerId(), cmd.slot());
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
				var message = new AcceptRequestMessage(accept.slot(), List.copyOf(membership.acceptors),
						accept.proposal());

				assert !accept.processId().equals(this.id);
				var host = PaxosBabel.hostFromProcessId(accept.processId());
				logger.trace("Sending AcceptRequestMessage to {}", host);
				this.sendMessage(message, host);
			}
			case LEARN -> {
				var decided = command.getLearn();
				var membership = this.paxos.membership(decided.slot());
				var message = new DecidedMessage(decided.slot(), List.copyOf(membership.acceptors), decided.value());

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
						prepareOk.accepted());

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
						List.copyOf(membership.acceptors),
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
		var timerIdPair = Pair.of(timer.slot, timer.timerId);
		if (!this.timers.containsKey(timerIdPair)) {
			logger.debug("Ignoring canceled timer {} for slot {}", timer.timerId, timer.slot);
			return;
		}
		this.execute(PaxosCmd.timerExpired(timer.slot, timer.timerId));
	}

	private void onDebugTimer(DebugTimer timer, long timerId) {
		if (this.paxos == null)
			return;
		this.paxos.printDebug();
	}

	/*--------------------------------- Message Handlers ---------------------------------------- */

	private void onAcceptOk(AcceptOkMessage msg, Host host, short sourceProto, int channelId) {
		if (this.paxos == null)
			return;

		logger.trace("Received AcceptOkMessage from {} on slot {}", host, msg.slot);
		var processId = PaxosBabel.hostToProcessId(host);
		var slot = msg.slot;
		this.execute(PaxosCmd.acceptOk(slot, processId, msg.ballot));
	}

	private void onAcceptRequest(AcceptRequestMessage msg, Host host, short sourceProto, int channelId) {
		if (this.paxos == null)
			return;

		logger.trace("Received accept request from {} for slot {}", host, msg.slot);
		var processId = PaxosBabel.hostToProcessId(host);
		var slot = msg.slot;
		var proposal = new Proposal(msg.proposal.ballot, msg.proposal.value);
		var membership = new Membership(msg.membership);
		if (this.paxos == null) {
			this.prejoinQueue.add(PaxosCmd.membershipDiscovered(slot, membership));
		} else {
			this.execute(
					PaxosCmd.membershipDiscovered(slot, membership),
					PaxosCmd.acceptRequest(slot, processId, proposal));
		}
	}

	private void onDecided(DecidedMessage msg, Host host, short sourceProto, int channelId) {
		logger.trace("Received decided message for slot {}", msg.slot);
		var processId = PaxosBabel.hostToProcessId(host);
		var slot = msg.slot;
		var value = msg.value;
		var membership = new Membership(msg.membership);
		var cmd1 = PaxosCmd.membershipDiscovered(slot, membership);
		var cmd2 = PaxosCmd.learn(slot, processId, value);
		if (this.paxos == null) {
			this.prejoinQueue.add(cmd1);
			this.prejoinQueue.add(cmd2);
		} else {
			this.execute(cmd1, cmd2);
		}
	}

	private void onPrepareOk(PrepareOkMessage msg, Host host, short sourceProto, int channelId) {
		if (this.paxos == null)
			return;

		logger.trace("Received prepare ok from {} for slot {}", host, msg.slot);
		var processId = PaxosBabel.hostToProcessId(host);
		var slot = msg.slot;
		this.execute(PaxosCmd.prepareOk(slot, processId, msg.ballot, msg.accepted));
	}

	private void onPrepareRequest(PrepareRequestMessage msg, Host host, short sourceProto, int channelId) {
		logger.trace("Received prepare request from {} for slot {}", host, msg.slot);
		var processId = PaxosBabel.hostToProcessId(host);
		var slot = msg.slot;
		var membership = new Membership(msg.membership);
		if (this.paxos == null) {
			this.prejoinQueue.add(PaxosCmd.membershipDiscovered(slot, membership));
		} else {
			this.execute(PaxosCmd.membershipDiscovered(slot, membership),
					PaxosCmd.prepareRequest(slot, processId, msg.ballot));
		}
	}

	/*--------------------------------- Request Handlers ---------------------------------------- */

	private void onProposeRequest(ProposeRequest request, short sourceProto) {
		logger.trace("Received propose request: {}", request);
		var strategy = request.takeLeadership ? PaxosCmd.ProposeStrategy.TakeLeadership
				: PaxosCmd.ProposeStrategy.Return;
		this.execute(PaxosCmd.propose(request.command, strategy));
	}

	private void onAddReplicaRequest(MemberAddRequest request, short sourceProto) {
		logger.info("Adding replica to membership: {}", request.replica);
		this.execute(PaxosCmd.memberAdded(request.slot, PaxosBabel.hostToProcessId(request.replica)));
	}

	private void onRemoveReplicaRequest(MemberRemoveRequest request, short sourceProto) {
		logger.info("Removing replica from membership: {}", request.replica);
		this.execute(PaxosCmd.memberRemoved(request.slot, PaxosBabel.hostToProcessId(request.replica)));
	}

	private void onMembershipUnchanged(MembershipUnchangedRequest request, short sourceProto) {
		logger.trace("Membership unchanged");
		this.execute(PaxosCmd.membershipUnchanged(request.slot));
	}

	private void onGcRequest(GcRequest request, short sourceProto) {
		logger.trace("Received GC request");
		this.paxos.gc(request.upToSlot);
	}

	/*--------------------------------- Notification Handlers ---------------------------------------- */

	private void onChannelReady(ChannelReadyNotification notification, short sourceProto) {
		logger.info("Received channel ready notification {}", notification);

		var channelId = notification.getChannelId();
		this.registerSharedChannel(channelId);
		this.id = PaxosBabel.hostToProcessId(notification.getMyself());

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
		// slots
		// after our join slot.
		// assert this.slots.isEmpty();

		logger.info("Joined notification received: {}", notification);

		if (this.joined)
			throw new RuntimeException("Already joined");
		this.joined = true;

		var membership = notification.membership.stream().map(PaxosBabel::hostToProcessId).toList();
		var config = PaxosConfig
				.builder()
				.withInitialSlot(notification.slot)
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

		this.execute(this.prejoinQueue.toArray(PaxosCmd[]::new));
		this.prejoinQueue.clear();
	}
}
