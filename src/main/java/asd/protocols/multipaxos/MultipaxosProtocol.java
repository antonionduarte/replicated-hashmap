package asd.protocols.multipaxos;

import java.io.IOException;
import java.util.*;

import asd.paxos.multi.MultipaxosConfig;
import asd.protocols.agreement.notifications.DecidedNotification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.ProcessId;
import asd.paxos.ProposalValue;
import asd.paxos.multi.MultiPaxos;
import asd.paxos.multi.MultiPaxosCmd;
import asd.paxos.multi.MultiPaxosCmdQueue;
import asd.protocols.PaxosBabel;
import asd.protocols.agreement.Agreement;
import asd.protocols.agreement.notifications.JoinedNotification;
import asd.protocols.agreement.requests.AddReplicaRequest;
import asd.protocols.agreement.requests.ProposeRequest;
import asd.protocols.agreement.requests.RemoveReplicaRequest;
import asd.protocols.multipaxos.messages.AcceptOk;
import asd.protocols.multipaxos.messages.AcceptRequest;
import asd.protocols.multipaxos.messages.Decided;
import asd.protocols.multipaxos.messages.PrepareOk;
import asd.protocols.multipaxos.messages.PrepareRequest;
import asd.protocols.paxos.PaxosProtocol;
import asd.protocols.paxos.messages.AcceptOkMessage;
import asd.protocols.paxos.messages.AcceptRequestMessage;
import asd.protocols.paxos.messages.DecidedMessage;
import asd.protocols.paxos.messages.PrepareOkMessage;
import asd.protocols.paxos.messages.PrepareRequestMessage;
import asd.protocols.statemachine.notifications.ChannelReadyNotification;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;

public class MultipaxosProtocol extends GenericProtocol {
	public final static String NAME = "MultiPaxos";

	private static final Logger logger = LogManager.getLogger(PaxosProtocol.class);

	private boolean joined;
	private boolean deciding;
	private ProcessId id;
	private ProcessId leaderId;

	private final ArrayDeque<ProposalValue> proposalQueue;
	private final MultiPaxosCmdQueue queue;

	/* Multipaxos */

	public final Set<ProcessId> membership;
	public final List<ProcessId> membershipList;
	public final MultipaxosConfig multipaxosConfig;

	/* Timeouts */

	private int majorityTimeout;
	private int leaderTimeout;

	/* Instance tracking */

	private MultiPaxos multipaxos;

	public MultipaxosProtocol(Properties props) throws HandlerRegistrationException, IOException {
		super(NAME, Agreement.ID);

		this.id = null;
		this.joined = false;
		this.deciding = false;

		// Multipaxos
		this.multipaxos = null;
		this.queue = new MultiPaxosCmdQueue();

		// Membership
		this.membership = new HashSet<>();
		this.membershipList = new ArrayList<>();

		// Config
		this.multipaxosConfig = MultipaxosConfig.builder()
				.withAcceptors(List.copyOf(this.membership))
				.withLearners(List.copyOf(this.membership))
				.withProposers(List.copyOf(this.membership))
				.build();

		// Proposal Queue
		this.proposalQueue = new ArrayDeque<>();

		/*--------------------- Register Timer Handlers ----------------------------- */

		/*--------------------- Register Request Handlers ----------------------------- */
		this.registerRequestHandler(ProposeRequest.ID, this::onProposeRequest);
		this.registerRequestHandler(AddReplicaRequest.ID, this::onAddReplicaRequest);
		this.registerRequestHandler(RemoveReplicaRequest.ID, this::onRemoveReplicaRequest);

		/*--------------------- Register Notification Handlers ----------------------------- */
		this.subscribeNotification(JoinedNotification.ID, this::onJoined);
		this.subscribeNotification(ChannelReadyNotification.ID, this::onChannelReady);
		// this.subscribeNotification(UnchangedConfigurationNotification.ID,
		//
		// this::onUnchangedConfiguration); TODO;

	}

	@Override
	public void init(Properties properties) throws HandlerRegistrationException, IOException {
	}

	private void executeCommandQueue() {
		while (!this.queue.isEmpty()) {
			var cmd = this.queue.pop();
			switch (cmd.getKind()) {
				case Decided -> uponDecidedCommand(cmd);
				case SetupTimer -> uponSetupTimerCommand(cmd);
				case CancelTimer -> uponCancelTimerCommand(cmd);
				case SendDecided -> uponSendDecidedCommand(cmd);
				case SendPrepareRequest -> uponSendPrepareRequestCommand(cmd);
				case SendPrepareOk -> uponSendPrepareOkCommand(cmd);
				case SendAcceptRequest -> uponSendAcceptRequestCommand(cmd);
				case SendAcceptOk -> uponSendAcceptOkCommand(cmd);
				case NewLeader -> uponNewLeaderCommand(cmd);
			}
		}
	}

	private void uponDecidedCommand(MultiPaxosCmd cmd) {
		var decided = cmd.getDecided();
		var operation = decided.value().data;
		// var notification = new DecidedNotification(operation); TODO;
		// this.triggerNotification(notification);
	}

	private void uponNewLeaderCommand(MultiPaxosCmd cmd) {
		// TODO: Trigger notification indicating that a new leader has been elected
		this.leaderId = cmd.getNewLeader().processId();
	}

	private void uponSendPrepareRequestCommand(MultiPaxosCmd cmd) {
		var prepare = cmd.getSendPrepareRequest();
		// var message = new PrepareRequestMessage(, prepare.ballot());

		if (prepare.processId().equals(this.id)) {
			logger.trace("Sending PrepareRequestMessage to self");
			// this.multipaxos.receivePrepareRequest(prepare.processId(), prepare.ballot());
		} else {
			var host = PaxosBabel.hostFromProcessId(prepare.processId());
			logger.trace("Sending PrepareRequestMessage to {}", host);
			// this.sendMessage(message, host);
		}
	}

	private void uponSendPrepareOkCommand(MultiPaxosCmd cmd) {
		var prepareOk = cmd.getSendPrepareOk();
		// var message = new PrepareOk(prepareOk.ballot(), prepareOk.highestAccept(), DECIDED);
	}

	private void uponSendAcceptRequestCommand(MultiPaxosCmd cmd) {
		var sendAcceptRequest = cmd.getSendAcceptRequest();

	}

	private void uponSendAcceptOkCommand(MultiPaxosCmd cmd) {
		var sendAcceptOk = cmd.getSendAcceptOk();
	}

	private void uponSendDecidedCommand(MultiPaxosCmd cmd) {
		var sendDecided = cmd.getSendDecided();
	}

	private void uponSetupTimerCommand(MultiPaxosCmd cmd) {
		var setupTimer = cmd.getSetupTimer();
	}

	private void uponCancelTimerCommand(MultiPaxosCmd cmd) {
		var cancelTimer = cmd.getCancelTimer();
	}

	/*--------------------------------- Timer Handlers ---------------------------------------- */

	/*--------------------------------- Message Handlers ---------------------------------------- */

	private void onAcceptOk(AcceptOk msg, Host host, short sourceProto, int channelId) {
		logger.trace("Received AcceptOkMessage from {}", host);
		// TODO;
	}

	private void onAcceptRequest(AcceptRequest msg, Host host, short sourceProto, int channelId) {
		logger.trace("Received accept request from {}", host);
		// TODO;
	}

	private void onDecided(Decided msg, Host host, short sourceProto, int channelId) {
		logger.trace("Received decided message");
		var processId = PaxosBabel.hostToProcessId(host);
		// this.multipaxos.receiveDecided(processId, msg.instance(), msg.value()); TODO;
	}

	private void onPrepareOk(PrepareOk msg, Host host, short sourceProto, int channelId) {
		logger.trace("Received prepare ok from {}", host);
		var processId = PaxosBabel.hostToProcessId(host);
		var proposal = Optional.ofNullable(msg.getAcceptedProposal());
		this.multipaxos.receivePrepareOk(processId, msg.getBallot(), proposal, this.multipaxosConfig);
		this.executeCommandQueue();
	}

	private void onPrepareRequest(PrepareRequest msg, Host host, short sourceProto, int channelId) {
		logger.trace("Received prepare request from {}", host);
		var processId = PaxosBabel.hostToProcessId(host);
		this.multipaxos.receivePrepareRequest(processId, msg.getBallot(), this.multipaxosConfig);
		this.executeCommandQueue();
	}

	/*--------------------------------- Request Handlers ---------------------------------------- */

	private void onProposeRequest(ProposeRequest request, short sourceProto) {
		logger.debug("Received propose request: {}", request);
		// TODO;
	}

	private void onAddReplicaRequest(AddReplicaRequest request, short sourceProto) {
		logger.info("Received add replica request {}", request);
		// TODO;
	}

	private void onRemoveReplicaRequest(RemoveReplicaRequest request, short sourceProto) {
		logger.info("Received remove replica request {}", request);
		// TODO;
	}

	/*--------------------------------- Notification Handlers ---------------------------------------- */

	private void onChannelReady(ChannelReadyNotification notification, short sourceProto) {
		logger.info("Received channel ready notification {}", notification);

		var channelId = notification.getChannelId();
		this.registerSharedChannel(channelId);
		this.id = PaxosBabel.hostToProcessId(notification.getMyself());

		/*
		 * -------------------- Register Message Serializers -----------
		 * 
		 * 
		 * 
		 * 
		 * * this.registerMessageSerializer(channelId, AcceptOkMessage.I
		 * ,
		 * * AcceptOk.serializer);
		 * 
		 * 
		 * this.registerMessageSerializer(channelId, AcceptRequestMessage.ID,
		 * AcceptRequest.serializer);
		 * this.registerMessageSerializer(channelId, DecidedMessage.ID,
		 * Decided.serializer);
		 * this.registerMessageSerializer(channelId, PrepareOkMessage.ID,
		 * PrepareOk.serializer);
		 * this.registerMessageSerializer(channelId, PrepareRequestMessage.ID,
		 * PrepareRequest.serializer);
		 */

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
		logger.info("Received joined notification {}", notification);
		// TODO;
	}

}
