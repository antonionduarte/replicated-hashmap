package asd.protocols.statemachine;

import asd.protocols.agreement.Agreement;
import asd.protocols.paxos.notifications.DecidedNotification;
import asd.protocols.paxos.notifications.JoinedNotification;
import asd.protocols.paxos.requests.AddReplicaRequest;
import asd.protocols.paxos.requests.ProposeRequest;
import asd.protocols.statemachine.notifications.ChannelReadyNotification;
import asd.protocols.statemachine.notifications.ExecuteNotification;
import asd.protocols.statemachine.requests.OrderRequest;
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
	// Protocol information, to register in babel
	public static final String PROTOCOL_NAME = "StateMachine";
	public static final short PROTOCOL_ID = 200;
	public static final int MAX_RETRIES = 3;
	public static final int RETRY_AFTER = 1000;

	private static final Logger logger = LogManager.getLogger(StateMachine.class);
	private final Host self; // My own address/port
	private final int channelId; // ID of the created channel
	private State state;
	private List<Host> membership;
	private int nextInstance;

	private final Map<Host, Integer> retriesPerPeer;
	private final List<OrderRequest> pendingRequests;

	public StateMachine(Properties props) throws IOException, HandlerRegistrationException {
		super(PROTOCOL_NAME, PROTOCOL_ID);
		nextInstance = 0;

		String address = props.getProperty("babel_address");
		String port = props.getProperty("statemachine_port");

		logger.info("Listening on {}:{}", address, port);
		this.self = new Host(InetAddress.getByName(address), Integer.parseInt(port));

		this.retriesPerPeer = new HashMap<>();
		this.pendingRequests = new LinkedList<>();

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

		/*--------------------- Register Request Handlers ----------------------------- */
		registerRequestHandler(OrderRequest.REQUEST_ID, this::uponOrderRequest);

		/*--------------------- Register Notification Handlers ----------------------------- */
		subscribeNotification(DecidedNotification.ID, this::uponDecidedNotification);

		registerTimerHandler(RetryTimer.TIMER_ID, this::uponRetryTimer);
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
			membership = new LinkedList<>(initialMembership);
			membership.forEach(this::openConnection);

			sendRequest(new AddReplicaRequest(0, self), Agreement.PROTOCOL_ID);
		}

	}

	/*--------------------------------- Requests ---------------------------------------- */
	private void uponOrderRequest(OrderRequest request, short sourceProto) {
		logger.debug("Received request: " + request);
		logger.trace("Received order request with payload size: " + request.getOperation().length);
		if (state == State.JOINING) {
			// Do something smart (like buffering the requests)
			pendingRequests.add(request);
		} else if (state == State.ACTIVE) {
			// Also do something smarter, we don't want an infinite number of instances
			// active
			// Maybe you should modify what is it that you are proposing so that you
			// remember that this
			// operation was issued by the application (and not an internal operation, check
			// the uponDecidedNotification)
			sendRequest(new ProposeRequest(nextInstance++, request.getOpId(), request.getOperation()),
					Agreement.PROTOCOL_ID);
		}
	}

	/*--------------------------------- Notifications ---------------------------------------- */
	private void uponDecidedNotification(DecidedNotification notification, short sourceProto) {
		logger.debug("Received notification: " + notification);
		// Maybe we should make sure operations are executed in order?
		// You should be careful and check if this operation is an application operation
		// (and send it up)
		// or if this is an operations that was executed by the state machine itself (in
		// which case you should execute)

		triggerNotification(new ExecuteNotification(notification.operationId, notification.operation));
	}

	/*--------------------------------- Messages ---------------------------------------- */
	private void uponMsgFail(ProtoMessage msg, Host host, short destProto, Throwable throwable, int channelId) {
		// If a message fails to be sent, for whatever reason, log the message and the
		// reason
		logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
	}

	/*
	 * --------------------------------- TCPChannel Events
	 * ----------------------------
	 */
	private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
		logger.info("Connection to {} is up", event.getNode());
		retriesPerPeer.remove(event.getNode());
	}

	private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
		logger.debug("Connection to {} is down, cause {}", event.getNode(), event.getCause());
	}

	private void uponOutConnectionFailed(OutConnectionFailed<ProtoMessage> event, int channelId) {
		logger.debug("Connection to {} failed, cause: {}", event.getNode(), event.getCause());
		if (membership.contains(event.getNode())) {
			retriesPerPeer.putIfAbsent(event.getNode(), 0);
			if (retriesPerPeer.get(event.getNode()) < MAX_RETRIES)
				setupTimer(new RetryTimer(event.getNode()), RETRY_AFTER);
			else
				retriesPerPeer.remove(event.getNode());
		}
		// Maybe we don't want to do this forever. At some point we assume he is no
		// longer there.
		// Also, maybe wait a little bit before retrying, or else you'll be trying 1000s
		// of times per second
		if (membership.contains(event.getNode())) {
			openConnection(event.getNode());
		}
	}

	private void uponRetryTimer(RetryTimer timer, long l) {
		retriesPerPeer.put(timer.peer, retriesPerPeer.get(timer.peer) + 1);
		openConnection(timer.peer);
	}

	private void uponInConnectionUp(InConnectionUp event, int channelId) {
		logger.trace("Connection from {} is up", event.getNode());
	}

	private void uponInConnectionDown(InConnectionDown event, int channelId) {
		logger.trace("Connection from {} is down, cause: {}", event.getNode(), event.getCause());
	}

	private enum State {
		JOINING, ACTIVE
	}

}
