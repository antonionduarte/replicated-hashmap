package asd.protocols.multipaxos;

import asd.paxos.ProcessId;
import asd.paxos.ProposalValue;
import asd.paxos.multi.Multipaxos;
import asd.paxos.multi.MultipaxosConfig;
import asd.paxos.single.PaxosCmd;
import asd.paxos.single.PaxosCmdQueue;
import asd.protocols.PaxosBabel;
import asd.protocols.agreement.Agreement;
import asd.protocols.agreement.notifications.JoinedNotification;
import asd.protocols.agreement.requests.AddReplicaRequest;
import asd.protocols.agreement.requests.ProposeRequest;
import asd.protocols.agreement.requests.RemoveReplicaRequest;
import asd.protocols.multipaxos.messages.*;
import asd.protocols.paxos.PaxosProtocol;
import asd.protocols.paxos.messages.*;
import asd.protocols.statemachine.notifications.ChannelReadyNotification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.util.*;

public class MultipaxosProtocol extends GenericProtocol {
	public final static String NAME = "Paxos";

	private static final Logger logger = LogManager.getLogger(PaxosProtocol.class);

	private boolean joined;
	private boolean deciding;
	private ProcessId id;

	private final ArrayDeque<ProposalValue> proposalQueue;
	private final PaxosCmdQueue queue;

	/* Multipaxos */

	public final Set<ProcessId> membership;
	public final List<ProcessId> membershipList;

	/* Instance tracking */

	private Multipaxos multipaxos;

	public MultipaxosProtocol(Properties props) throws HandlerRegistrationException, IOException {
		super(NAME, Agreement.ID);

		this.id = null;
		this.joined = false;
		this.deciding = false;

		// Multipaxos
		this.multipaxos = null;
		this.queue = new PaxosCmdQueue();

		// Membership
		this.membership = new HashSet<>();
		this.membershipList = new ArrayList<>();

		// Proposal Queue
		this.proposalQueue = new ArrayDeque<>();

		/*--------------------- Register Timer Handlers ----------------------------- */

		/*--------------------- Register Request Handlers ----------------------------- */

		/*--------------------- Register Notification Handlers ----------------------------- */


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
			}
		}
	}

	private void uponProposeCommand(PaxosCmd cmd) {
		// TODO;
	}

	private void uponDecidedCommand(PaxosCmd cmd) {
		var decided = cmd.getDecided();

		this.deciding = false;

		// TODO;
	}

	private void uponSendPrepareRequestCommand(PaxosCmd cmd) {
		var prepare = cmd.getSendPrepareRequest();
		var message = new PrepareRequest(); // TODO;

		// TODO: This should only happen if this Node wants to suggest being the new leader.

		if (prepare.processId().equals(this.id)) {
			logger.trace("Sending PrepareRequestMessage to self");
			this.multipaxos.receivePrepareRequest(prepare.processId(), prepare.ballot());
		} else {
			var host = PaxosBabel.hostFromProcessId(prepare.processId());
			logger.trace("Sending PrepareRequestMessage to {}", host);
			this.sendMessage(message, host);
		}
	}

	private void uponSendPrepareOkCommand(PaxosCmd cmd) {
		var prepareOk = cmd.getSendPrepareOk();
	}

	private void uponSendAcceptRequestCommand(PaxosCmd cmd) {
		var sendAcceptRequest = cmd.getSendAcceptRequest();
	}

	private void uponSendAcceptOkCommand(PaxosCmd cmd) {
		var sendAcceptOk = cmd.getSendAcceptOk();
	}

	private void uponSendDecidedCommand(PaxosCmd cmd) {
		var sendDecided = cmd.getSendDecided();
	}

	private void uponSetupTimerCommand(PaxosCmd cmd) {
		var setupTimer = cmd.getSetupTimer();
	}

	private void uponCancelTimerCommand(PaxosCmd cmd) {
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
		// TODO;
	}

	private void onPrepareOk(PrepareOk msg, Host host, short sourceProto, int channelId) {
		logger.trace("Received prepare ok from {}", host);
		// TODO;
	}

	private void onPrepareRequest(PrepareRequest msg, Host host, short sourceProto, int channelId) {
		logger.trace("Received prepare request from {}", host);
		// TODO;
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

		/*--------------------- Register Message Serializers ---------------------- */

		/* this.registerMessageSerializer(channelId, AcceptOkMessage.ID, AcceptOk.serializer);
		this.registerMessageSerializer(channelId, AcceptRequestMessage.ID, AcceptRequest.serializer);
		this.registerMessageSerializer(channelId, DecidedMessage.ID, Decided.serializer);
		this.registerMessageSerializer(channelId, PrepareOkMessage.ID, PrepareOk.serializer);
		this.registerMessageSerializer(channelId, PrepareRequestMessage.ID, PrepareRequest.serializer); */

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
