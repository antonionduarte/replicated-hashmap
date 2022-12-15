package asd.protocols.statemachine;

import asd.protocols.agreement.Agreement;
import asd.protocols.agreement.notifications.DecidedNotification;
import asd.protocols.agreement.notifications.JoinedNotification;
import asd.protocols.agreement.notifications.LeaderChanged;
import asd.protocols.agreement.requests.MemberAddRequest;
import asd.protocols.agreement.requests.ProposeRequest;
import asd.protocols.agreement.requests.MemberRemoveRequest;
import asd.protocols.agreement.requests.MembershipUnchangedRequest;
import asd.protocols.app.HashApp;
import asd.protocols.app.requests.CurrentStateReply;
import asd.protocols.app.requests.CurrentStateRequest;
import asd.protocols.app.requests.InstallStateRequest;
import asd.protocols.statemachine.commands.Batch;
import asd.protocols.statemachine.commands.BatchBuilder;
import asd.protocols.statemachine.commands.BatchHash;
import asd.protocols.statemachine.commands.Command;
import asd.protocols.statemachine.commands.CommandQueue;
import asd.protocols.statemachine.commands.OrderedCommand;
import asd.protocols.statemachine.messages.OrderCommand;
import asd.protocols.statemachine.messages.SystemJoin;
import asd.protocols.statemachine.messages.SystemJoinReply;
import asd.protocols.statemachine.notifications.ChannelReadyNotification;
import asd.protocols.statemachine.notifications.ExecuteNotification;
import asd.protocols.statemachine.requests.OrderRequest;
import asd.protocols.statemachine.timers.BatchBuildTimer;
import asd.protocols.statemachine.timers.CheckLeaderTimeoutTimer;
import asd.protocols.statemachine.timers.CommandQueueTimer;
import asd.protocols.statemachine.timers.OrderBatchTimer;
import asd.protocols.statemachine.timers.ProposeNoopTimer;
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
import java.time.Instant;
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

	private static enum State {
		JOINING, ACTIVE
	}

	// A batch sent to the leader, and the time it was sent.
	private static record SentBatchEntry(Instant instant, Batch batch) {
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

	/// Leader tracking
	private Optional<Host> leader;
	private final Map<BatchHash, SentBatchEntry> leaderSentBatches;
	private final Duration leaderTimeoutDuration;
	private Instant leaderLastMessage;

	/// JoinRequest tracking
	// Keeps track of any peers that sent us a join request so we can send them a
	// reply.
	private final Queue<Host> joiningReplicas;

	/// Command Queueing
	// commandQueue is a queue of commands that are waiting to be executed.
	// commandQueueTimer is -1 if no active timer.
	// after the timer fires, a proposal is made for the first missing instance.
	private final CommandQueue commandQueue;
	private final Duration commandQueueTimeout;
	private long commandQueueTimer;

	/// Duplicate operation tracking
	// Keep track of all executed operation UUIDs, for a certain ttl, so we can
	/// detect duplicates.
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

		// Leader tracking
		this.leader = Optional.empty();
		this.leaderSentBatches = new HashMap<>();
		this.leaderTimeoutDuration = Duration.parse(props.getProperty("statemachine_leader_timeout"));
		this.leaderLastMessage = Instant.now();

		/// Membership tracking
		/// JoinRequest tracking
		this.joiningReplicas = new LinkedList<>();

		/// Command queueing
		this.commandQueue = new CommandQueue();
		this.commandQueueTimeout = Duration.parse(props.getProperty("statemachine_command_queue_timeout"));
		this.commandQueueTimer = -1;

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

		/*--------------------- Register Message Serializers ---------------------- */
		registerMessageSerializer(channelId, OrderCommand.ID, OrderCommand.serializer);
		registerMessageSerializer(channelId, SystemJoin.ID, SystemJoin.serializer);
		registerMessageSerializer(channelId, SystemJoinReply.ID, SystemJoinReply.serializer);

		/*--------------------- Register Message Handlers ----------------------------- */
		registerMessageHandler(channelId, OrderCommand.ID, this::uponOrderCommand);
		registerMessageHandler(channelId, SystemJoin.ID, this::uponSystemJoin);
		registerMessageHandler(channelId, SystemJoinReply.ID, this::uponSystemJoinReply);

		/*--------------------- Register Request Handlers ----------------------------- */
		registerRequestHandler(OrderRequest.REQUEST_ID, this::uponOrderRequest);

		/*--------------------- Register Reply Handlers ----------------------------- */
		registerReplyHandler(CurrentStateReply.REQUEST_ID, this::uponCurrentStateReply);

		/*--------------------- Register Notification Handlers ----------------------------- */
		subscribeNotification(DecidedNotification.ID, this::uponDecidedNotification);
		subscribeNotification(LeaderChanged.ID, this::uponLeaderChanged);

		/*--------------------- Register Timer Handlers ----------------------------- */
		registerTimerHandler(BatchBuildTimer.ID, this::uponBatchBuild);
		registerTimerHandler(RetryTimer.ID, this::uponRetryTimer);
		registerTimerHandler(OrderBatchTimer.ID, this::uponOrderBatchTimer);
		registerTimerHandler(CheckLeaderTimeoutTimer.ID, this::uponCheckLeaderTimeout);
		registerTimerHandler(ProposeNoopTimer.ID, this::uponProposeNoop);
	}

	@Override
	public void init(Properties props) {
		// Inform the state machine protocol about the channel we created in the
		// constructor
		triggerNotification(new ChannelReadyNotification(channelId, self));

		var leaderTimeoutRandomness = (long) ((double) this.leaderTimeoutDuration.toMillis() * Math.random());
		setupPeriodicTimer(new CheckLeaderTimeoutTimer(), 0,
				this.leaderTimeoutDuration.toMillis() + leaderTimeoutRandomness);

		setupPeriodicTimer(new ProposeNoopTimer(), 0, this.leaderTimeoutDuration.toMillis() / 3);

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

		this.leaderLastMessage = Instant.now();
		switch (command.getKind()) {
			case BATCH -> {
				var batch = command.getBatch();
				this.leaderSentBatches.remove(batch.hash);
				logger.debug("Executing batch {} with size {}", batch.hash, batch.operations.length);

				for (var operation : batch.operations) {
					if (!this.executedOperations.contains(operation.operationId)) {
						var notification = new ExecuteNotification(operation.operationId, operation.operation);
						this.executedOperations.set(operation.operationId);
						this.triggerNotification(notification);
					} else {
						logger.warn("Skipping operation {} as it has already been executed", operation.operationId);
					}
				}
				this.notifyMembershipUnchanged(instance);
			}
			case JOIN -> {
				var join = command.getJoin();

				this.membership.add(join.host);
				openConnection(join.host);

				if (this.joiningReplicas.contains(join.host)) {
					sendRequest(new CurrentStateRequest(instance), HashApp.PROTO_ID);
				}
				this.notifyMemberAdded(instance, join.host);
			}
			case LEAVE -> {
				var leave = command.getLeave();
				if (this.membership.remove(leave.host))
					this.notifyMemberRemoved(instance, leave.host);
				else
					this.notifyMembershipUnchanged(instance);
			}
			case NOOP -> {
				this.notifyMembershipUnchanged(instance);
			}
		}
	}

	private void proposeCommand(Command command) {
		assert this.state == State.ACTIVE;
		assert this.leader.isEmpty() || this.isLeader();

		if (command.getKind() == Command.Kind.BATCH) {
			var batch = command.getBatch();
			logger.debug("Ordering batch {} with size {}", batch.hash, batch.operations.length);
		}

		var request = new ProposeRequest(command.toBytes());
		this.sendRequest(request, Agreement.ID);
	}

	private void sendCommandToLeader(Command command) {
		assert this.state == State.ACTIVE;

		if (this.leader.isEmpty() || this.isLeader()) {
			this.proposeCommand(command);
		} else {
			var message = new OrderCommand(command);
			var leader = this.leader.get();
			this.sendMessage(message, leader);
		}
	}

	private void finalizeBatch() {
		var batch = this.batchBuilder.build();
		logger.debug("Building batch with size: {}", batch.operations.length);
		this.sendCommandToLeader(batch);
	}

	private void appendOperationToBatch(Operation operation) {
		assert this.state == State.ACTIVE;

		this.batchBuilder.append(operation);
		if (this.batchBuildTimer == -1) {
			if (this.batchBuildTimeout.isZero()) {
				this.finalizeBatch();
			} else {
				var timer = new BatchBuildTimer();
				var timeout = this.batchBuildTimeout.toMillis();
				this.batchBuildTimer = this.setupTimer(timer, timeout);
			}
		}
	}

	private boolean isLeader() {
		return this.leader.isPresent() && this.leader.get().equals(this.self);
	}

	private void proposeNoop() {
		var command = Command.noop();
		this.proposeCommand(command);
	}

	private void notifyMemberAdded(int slot, Host host) {
		var notification = new MemberAddRequest(slot, host);
		this.sendRequest(notification, Agreement.ID);
	}

	private void notifyMemberRemoved(int slot, Host host) {
		var notification = new MemberRemoveRequest(slot, host);
		this.sendRequest(notification, Agreement.ID);
	}

	private void notifyMembershipUnchanged(int slot) {
		var notification = new MembershipUnchangedRequest(slot);
		this.sendRequest(notification, Agreement.ID);
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
				this.appendOperationToBatch(operation);
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
		sendMessage(new SystemJoinReply(membership, reply.getInstance() + 1, reply.getState()), joiningReplica);
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

	private void uponLeaderChanged(LeaderChanged notification, short sourceProto) {
		logger.debug("Received leader changed notification: {}", notification);
		logger.info("Leader changed to: {}", notification.leader);
		this.leader = Optional.of(notification.leader);
		for (var cmd : notification.commands)
			this.sendCommandToLeader(Command.fromBytes(cmd));
	}

	/*--------------------------------- Messages ---------------------------------------- */

	private void uponOrderCommand(OrderCommand message, Host sender, short sourceProto, int channelId) {
		if (this.state != State.ACTIVE) {
			logger.warn("Received order batch while not active, ignoring");
			return;
		}
		if (!this.membership.contains(sender)) {
			logger.warn("Received order batch from replica that is not part of the system, ignoring");
			return;
		}
		if (!this.isLeader()) {
			logger.warn("Received order batch from replica while not being the leader, ignoring");
			return;
		}
		logger.debug("Received order batch from replica: {}", sender);
		this.proposeCommand(message.command);
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
		this.proposeCommand(command);
	}

	private void uponSystemJoinReply(SystemJoinReply systemJoinReply, Host host, short sourceProto, int channelId) {
		if (this.state == State.ACTIVE) {
			logger.warn("Received system join reply while active, ignoring");
			return;
		}

		logger.info("Joining system at instance {} with membership {}", systemJoinReply.instance,
				systemJoinReply.membership);

		this.membership = new LinkedList<>(systemJoinReply.membership);
		this.membership.forEach(this::openConnection);

		this.commandQueue.setLastExecutedInstance(systemJoinReply.instance - 1);

		var installRequest = new InstallStateRequest(systemJoinReply.state);
		this.sendRequest(installRequest, HashApp.PROTO_ID);

		logger.info("System join reply instance = {}, membership = {}", systemJoinReply.instance,
				systemJoinReply.membership);
		var joinNotification = new JoinedNotification(systemJoinReply.instance, systemJoinReply.membership);
		this.triggerNotification(joinNotification);
		this.state = State.ACTIVE;
	}

	/* -------------------------- TCPChannel Events -------------------------- */
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
						// sendRequest(new ProposeRequest(command.toBytes()), Agreement.ID);
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

	private void uponRetryTimer(RetryTimer timer, long timerId) {
		logger.warn("Retrying connection to {}", timer.peer);
		retriesPerPeer.put(timer.peer, retriesPerPeer.get(timer.peer) + 1);
		openConnection(timer.peer);
	}

	private void uponBatchBuild(BatchBuildTimer timer, long timerId) {
		assert this.batchBuildTimer == timerId;
		this.batchBuildTimer = -1;
		this.finalizeBatch();
	}

	private void uponOrderBatchTimer(OrderBatchTimer timer, long timerId) {
		assert this.leaderSentBatches.isEmpty() || this.leader.isPresent();

		var deadline = Instant.now().minus(Duration.ofSeconds(5)); // TODO make this configurable
		for (var entry : this.leaderSentBatches.entrySet()) {
			var hash = entry.getKey();
			var sent = entry.getValue();
			var batch = sent.batch;
			// TODO: is this even needed?
		}
	}

	private void uponCheckLeaderTimeout(CheckLeaderTimeoutTimer timer, long timerId) {
		if (this.isLeader() || this.leader.isEmpty())
			return;

		var elapsed = Duration.between(Instant.now(), this.leaderLastMessage);
		if (elapsed.compareTo(this.leaderTimeoutDuration) < 0)
			return;

		logger.info("Leader {} timed out, attempting to become leader", this.leader.get());
		this.proposeNoop();
	}

	private void uponProposeNoop(ProposeNoopTimer timer, long timerId) {
		if (!this.isLeader())
			return;
		this.proposeNoop();
	}

}
