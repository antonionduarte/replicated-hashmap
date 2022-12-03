package asd.protocols.statemachine;

import asd.protocols.agreement.Agreement;
import asd.protocols.agreement.notifications.DecidedNotification;
import asd.protocols.agreement.notifications.JoinedNotification;
import asd.protocols.agreement.requests.AddReplicaRequest;
import asd.protocols.agreement.requests.ProposeRequest;
import asd.protocols.agreement.requests.RemoveReplicaRequest;
import asd.protocols.app.HashApp;
import asd.protocols.app.requests.CurrentStateReply;
import asd.protocols.app.requests.CurrentStateRequest;
import asd.protocols.app.requests.InstallStateRequest;
import asd.protocols.statemachine.commands.BatchBuilder;
import asd.protocols.statemachine.commands.Command;
import asd.protocols.statemachine.commands.CommandQueue;
import asd.protocols.statemachine.commands.OrderedCommand;
import asd.protocols.statemachine.messages.SystemJoin;
import asd.protocols.statemachine.messages.SystemJoinReply;
import asd.protocols.statemachine.notifications.ChannelReadyNotification;
import asd.protocols.statemachine.notifications.ExecuteNotification;
import asd.protocols.statemachine.notifications.UnchangedConfigurationNotification;
import asd.protocols.statemachine.requests.OrderRequest;
import asd.protocols.statemachine.timers.BatchBuildTimer;
import asd.protocols.statemachine.timers.CommandQueueTimer;
import asd.protocols.statemachine.timers.RetryTimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.*;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.*;

/**
 * This is NOT fully functional StateMachine implementation. This is simply an
 * example of things you can do, and can be
 * used as a starting point.
 * <p>
 * You are free to change/delete anything in this class, including its fields.
 * The only thing that you cannot change are
 * the notifications/requests between the StateMachine and the APPLICATION You
 * can change the requests/notification
 * between the StateMachine and AGREEMENT protocol, however make sure it is
 * coherent with the specification shown in the
 * project description.
 * <p>
 * Do not assume that any logic implemented here is correct, think for yourself!
 */
public class StateMachine extends GenericProtocol {

	private enum State {
		JOINING, ACTIVE
	}

	// Protocol information, to register in babel
	public static final String PROTOCOL_NAME = "StateMachine";
	public static final short ID = 200;
	public static final int MAX_RETRIES = 5;
	public static final int RETRY_AFTER = 100;

	private static final Logger logger = LogManager.getLogger(StateMachine.class);
	private final Host self; // My own address/port
	private final int channelId; // ID of the created channel
	private State state;
	private List<Host> membership;

	private final Map<Host, Integer> retriesPerPeer;
	private final Queue<OrderRequest> pendingRequests;
	private List<Host> potentialContacts;

	/// JoinRequest tracking
	// Keeps track of any peers that sent us a join request so we can send them a
	// reply.
	private final Queue<Host> joiningReplicas;

	/// Command Queueing
	// commandQueueTimer is -1 if no active timer.
	// after the timer fires, a proposal is made for the first missing instance.
	private final CommandQueue commandQueue;
	private final Duration commandQueueTimeout;
	private long commandQueueTimer;

	private final TtlSet<UUID> executedOperations;

	/// Operation batching
	// batchBuildTimer is -1 if there is no active timer.
	// after the timer fires, the batch is built and sent to the agreement protocol.
	private final BatchBuilder batchBuilder;
	private final Duration batchBuildTimeout;
	private long batchBuildTimer;

	public StateMachine(Properties props) throws IOException, HandlerRegistrationException {
		super(PROTOCOL_NAME, ID);

		String address = props.getProperty("babel_address");
		String port = props.getProperty("statemachine_port");

		logger.info("Listening on {}:{}", address, port);
		this.self = new Host(InetAddress.getByName(address), Integer.parseInt(port));

		this.retriesPerPeer = new HashMap<>();
		this.pendingRequests = new LinkedList<>();

		/// Membership tracking
		/// JoinRequest tracking
		this.joiningReplicas = new LinkedList<>();

		/// Command queueing
		this.commandQueue = new CommandQueue();
		this.commandQueueTimeout = Duration.parse(props.getProperty("statemachine_command_queue_timeout"));
		this.commandQueueTimer = -1;

		/// Duplicate operation tracking
		/// Duplicate operation tracking
		// duplicateOperationTtl is the amount of time that we remember an operation id
		// after it was executed for the first time.
		Duration duplicateOperationTtl = Duration.parse(props.getProperty("statemachine_duplicate_operation_ttl"));
		this.executedOperations = new TtlSet<>(duplicateOperationTtl);

		/// Operation batching
		this.batchBuilder = new BatchBuilder();
		this.batchBuildTimeout = Duration.parse(props.getProperty("statemachine_batch_build_timeout"));
		this.batchBuildTimer = -1;

		Properties channelProps = new Properties();
		channelProps.setProperty(TCPChannel.ADDRESS_KEY, address);
		channelProps.setProperty(TCPChannel.PORT_KEY, port); // The port to bind to
		channelProps.setProperty(TCPChannel.HEARTBEAT_INTERVAL_KEY, "1000");
		channelProps.setProperty(TCPChannel.HEARTBEAT_TOLERANCE_KEY, "3000");
		channelProps.setProperty(TCPChannel.CONNECT_TIMEOUT_KEY, "1000");
		channelId = createChannel(TCPChannel.NAME, channelProps);

		/*-------------------- Register Channel Events ------------------------------- */
		registerChannelEventHandler(channelId, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
		registerChannelEventHandler(channelId, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);
		registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
		registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
		registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);
		// registerChannelEventHandler(channelId, ..., this::uponMsgFail);

		/*--------------------- Register Message Handlers ----------------------------- */
		registerMessageHandler(channelId, SystemJoin.ID, this::uponSystemJoin);
		registerMessageHandler(channelId, SystemJoinReply.ID, this::uponSystemJoinReply);

		/*--------------------- Register Request Handlers ----------------------------- */
		registerRequestHandler(OrderRequest.REQUEST_ID, this::uponOrderRequest);

		/*--------------------- Register Reply Handlers ----------------------------- */
		registerReplyHandler(CurrentStateReply.REQUEST_ID, this::uponCurrentStateReply);

		/*--------------------- Register Notification Handlers ----------------------------- */
		subscribeNotification(DecidedNotification.ID, this::uponDecidedNotification);

		/*--------------------- Register Timer Handlers ----------------------------- */
		registerTimerHandler(BatchBuildTimer.ID, this::uponBatchBuild);
		registerTimerHandler(RetryTimer.ID, this::uponRetryTimer);
	}

	@Override
	public void init(Properties props) {
		// Inform the state machine protocol about the channel we created in the
		// constructor
		triggerNotification(new ChannelReadyNotification(channelId, self));

		String host = props.getProperty("statemachine_initial_membership");
		System.out.println("MEMBERSHIP = " + host);
		String[] hosts = host.split(",");
		List<Host> initialMembership = new LinkedList<>();
		for (String s : hosts) {
			String[] hostElements = s.split(":");
			Host h;
			try {
				h = new Host(InetAddress.getByName(hostElements[0]), Integer.parseInt(hostElements[1]));
			} catch (UnknownHostException e) {
				throw new AssertionError("Error parsing statemachine_initial_membership", e);
			}
			initialMembership.add(h);
		}

		if (initialMembership.contains(self)) {
			state = State.ACTIVE;
			logger.info("Starting in ACTIVE as I am part of initial membership");
			// I'm part of the initial membership, so I'm assuming the system is
			// bootstrapping
			membership = new LinkedList<>(initialMembership);
			membership.forEach(this::openConnection);
			triggerNotification(new JoinedNotification(0, membership));
		} else {
			state = State.JOINING;
			logger.info("Starting in JOINING as I am not part of initial membership");
			// You have to do something to join the system and know which instance you
			// joined
			// (and copy the state of that instance)
			membership = new LinkedList<>();
			potentialContacts = initialMembership;
			var contact = initialMembership.get(0);
			openConnection(contact);
		}

	}

	/*--------------------------------- Helpers ---------------------------------------- */

	private void executeOrderedCommand(OrderedCommand orderedCommand) {
		assert this.state == State.ACTIVE;

		var command = orderedCommand.command();
		var instance = orderedCommand.instance();
		logger.debug("Executing command {} for instance {}", command, instance);

		switch (command.getKind()) {
			case BATCH -> {
				var batch = command.getBatch();
				for (var operation : batch.operations) {
					if (this.executedOperations.contains(operation.operationId)) {
						logger.warn("Skipping operation {} as it has already been executed", operation.operationId);
						continue;
					}

					var notification = new ExecuteNotification(operation.operationId, operation.operation);
					this.executedOperations.set(operation.operationId);
					this.triggerNotification(notification);

					var unchangedNotification = new UnchangedConfigurationNotification(instance);
					this.triggerNotification(unchangedNotification);
				}
			}
			case JOIN -> {
				var join = command.getJoin();
				var request = new AddReplicaRequest(instance, join.host);
				this.sendRequest(request, Agreement.ID);
				this.membership.add(join.host);
				openConnection(join.host);

				if (this.joiningReplicas.contains(join.host)) {
					sendRequest(new CurrentStateRequest(instance), HashApp.PROTO_ID);
				}
			}
			case LEAVE -> {
				var leave = command.getLeave();
				if (this.membership.remove(leave.host)) {
					var request = new RemoveReplicaRequest(instance, leave.host);
					this.sendRequest(request, Agreement.ID);
				}
			}
			case NOOP -> {
				var notification = new UnchangedConfigurationNotification(instance);
				this.triggerNotification(notification);
			}
		}
	}

	private void buildBatch() {
		var batch = this.batchBuilder.build();
		var request = new ProposeRequest(batch.toBytes());
		logger.debug("Sending batch with size: {}", batch.operations.length);
		this.sendRequest(request, Agreement.ID);
	}

	/*--------------------------------- Requests ---------------------------------------- */

	private void uponOrderRequest(OrderRequest request, short sourceProto) {
		logger.debug("Received request: " + request);
		logger.trace("Received order request with payload size: " + request.getOperation().length);

		switch (this.state) {
			case JOINING -> {
				logger.debug("Received order request while joining, adding to pending requests, size: "
						+ pendingRequests.size());
				pendingRequests.add(request);
			}
			case ACTIVE -> {
				logger.debug("Received order request while active, adding to batch builder, size: "
						+ batchBuilder.size());
				var operation = new Operation(request.getOperationId(), request.getOperation());
				this.batchBuilder.append(operation);
				if (this.batchBuildTimer == -1) {
					if (this.batchBuildTimeout.isZero())
						this.buildBatch();
					else
						this.batchBuildTimer = this.setupTimer(new BatchBuildTimer(),
								this.batchBuildTimeout.toMillis());
				}
			}
		}
	}

	/*--------------------------------- Replies ---------------------------------------- */

	private void uponCurrentStateReply(CurrentStateReply reply, short i) {
		var joiningReplica = joiningReplicas.poll();
		if (joiningReplica == null) {
			logger.warn("CurrentStateReply with no joining replica");
			return;
		}
		sendMessage(new SystemJoinReply(membership, reply.getInstance(), reply.getState()), joiningReplica);
	}

	/*--------------------------------- Notifications ---------------------------------------- */

	private void uponDecidedNotification(DecidedNotification notification, short sourceProto) {
		logger.trace("Received decided notification for instance " + notification.instance);

		var instance = notification.instance;
		var command = Command.fromBytes(notification.operation);

		this.commandQueue.insert(instance, command);
		if (this.commandQueue.hasMissingInstance() && this.commandQueueTimer == -1)
			this.commandQueueTimer = this.setupTimer(new CommandQueueTimer(), this.commandQueueTimeout.toMillis());

		while (this.commandQueue.hasReadyCommand())
			this.executeOrderedCommand(this.commandQueue.popReadyCommand());
	}

	/*--------------------------------- Messages ---------------------------------------- */
	private void uponMsgFail(ProtoMessage msg, Host host, short destProto, Throwable throwable, int channelId) {
		// If a message fails to be sent, for whatever reason, log the message and the
		// reason
		logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
	}

	private void uponSystemJoin(SystemJoin systemJoin, Host sender, short sourceProto, int channelId) {
		if (this.state != State.ACTIVE) {
			logger.warn("Received system join while not active, ignoring");
			return;
		}
		if (this.membership.contains(sender)) {
			logger.warn("Received system join from replica that is already part of the system, ignoring");
			return;
		}
		joiningReplicas.add(sender);
		var command = Command.join(sender);
		sendRequest(new ProposeRequest(command.toBytes()), Agreement.ID);
	}

	private void uponSystemJoinReply(SystemJoinReply systemJoinReply, Host host, short sourceProto, int channelId) {
		if (this.state == State.ACTIVE) {
			logger.warn("Received system join reply while active, ignoring");
			return;
		}

		logger.debug("Joining system at instance {} with membership {}", systemJoinReply.instance,
				systemJoinReply.membership);

		this.membership = new LinkedList<>(systemJoinReply.membership);
		this.membership.forEach(this::openConnection);

		this.commandQueue.setLastExecutedInstance(systemJoinReply.instance - 1);

		var installRequest = new InstallStateRequest(systemJoinReply.state);
		this.sendRequest(installRequest, HashApp.PROTO_ID);

		var joinNotification = new JoinedNotification(systemJoinReply.instance, systemJoinReply.membership);
		this.triggerNotification(joinNotification);
		this.state = State.ACTIVE;
	}

	/*
	 * --------------------------------- TCPChannel Events
	 * ----------------------------
	 */
	private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
		logger.info("Connection to {} is up", event.getNode());
		retriesPerPeer.remove(event.getNode());
		if (state == State.JOINING)
			sendMessage(new SystemJoin(), event.getNode());
	}

	private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
		logger.debug("Connection to {} is down, cause {}, retrying connection", event.getNode(), event.getCause());
		if (event.getCause() != null)
			event.getCause().printStackTrace();
		if (membership.contains(event.getNode()))
			openConnection(event.getNode()); // retry to connect some times before proposing leave
	}

	private void uponOutConnectionFailed(OutConnectionFailed<ProtoMessage> event, int channelId) {

		logger.debug("Connection to {} failed, cause: {}", event.getNode(), event.getCause());
		// Maybe we don't want to do this forever. At some point we assume he is no
		// longer there.
		// Also, maybe wait a little bit before retrying, or else you'll be trying 1000s
		// of times per second
		retriesPerPeer.putIfAbsent(event.getNode(), 0);
		if (retriesPerPeer.get(event.getNode()) < MAX_RETRIES)
			setupTimer(new RetryTimer(event.getNode()), RETRY_AFTER);
		else {
			retriesPerPeer.remove(event.getNode());
			switch (state) {
				case JOINING -> {
					potentialContacts.remove(event.getNode());
					if (!potentialContacts.isEmpty())
						openConnection(potentialContacts.get(0));
				}
				case ACTIVE -> {
					if (membership.contains(event.getNode())) {
						var command = Command.leave(event.getNode());
						// TODO not sure how to make only one replica propose this, maybe just deal with
						// it
						sendRequest(new ProposeRequest(command.toBytes()), Agreement.ID);
					}
				}
			}
			if (state == State.JOINING) {
				potentialContacts.remove(event.getNode());
				if (!potentialContacts.isEmpty())
					openConnection(potentialContacts.get(0));
			}
		}
	}

	private void uponInConnectionUp(InConnectionUp event, int channelId) {
		logger.trace("Connection from {} is up", event.getNode());
	}

	private void uponInConnectionDown(InConnectionDown event, int channelId) {
		logger.trace("Connection from {} is down, cause: {}", event.getNode(), event.getCause());
		if (event.getCause() != null)
			event.getCause().printStackTrace();
	}

	/*--------------------------------- Timers ---------------------------------------- */

	private void uponRetryTimer(RetryTimer timer, long l) {
		logger.warn("Retrying connection to {}", timer.peer);
		retriesPerPeer.put(timer.peer, retriesPerPeer.get(timer.peer) + 1);
		openConnection(timer.peer);
	}

	private void uponBatchBuild(BatchBuildTimer timer, long timerId) {
		assert this.batchBuildTimer == timerId;
		this.batchBuildTimer = -1;
		this.buildBatch();
	}

}
