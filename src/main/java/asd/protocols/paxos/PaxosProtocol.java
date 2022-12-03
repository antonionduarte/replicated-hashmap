package asd.protocols.paxos;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.ProcessId;
import asd.paxos.ProposalValue;
import asd.paxos.single.Paxos;
import asd.paxos.single.PaxosCmdQueue;
import asd.paxos.single.PaxosConfig;
import asd.protocols.PaxosBabel;
import asd.protocols.agreement.Agreement;
import asd.protocols.agreement.notifications.DecidedNotification;
import asd.protocols.agreement.notifications.JoinedNotification;
import asd.protocols.agreement.requests.AddReplicaRequest;
import asd.protocols.agreement.requests.ProposeRequest;
import asd.protocols.agreement.requests.RemoveReplicaRequest;
import asd.protocols.paxos.messages.AcceptOkMessage;
import asd.protocols.paxos.messages.AcceptRequestMessage;
import asd.protocols.paxos.messages.DecidedMessage;
import asd.protocols.paxos.messages.PrepareOkMessage;
import asd.protocols.paxos.messages.PrepareRequestMessage;
import asd.protocols.paxos.timer.PaxosTimer;
import asd.protocols.statemachine.StateMachine;
import asd.protocols.statemachine.notifications.ChannelReadyNotification;
import asd.protocols.statemachine.notifications.UnchangedConfigurationNotification;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;

public class PaxosProtocol extends GenericProtocol implements Agreement {
    private static class InstanceState {
        public final Set<ProcessId> membership;
        public final List<ProcessId> membershipList;
        public final Paxos paxos;
        public final PaxosCmdQueue queue;
        public final HashMap<Integer, Long> timerIdToBabelTimerId;
        public boolean decided;
        public ProposalValue originalProposal;

        public InstanceState(ProcessId id, PaxosConfig config, Set<ProcessId> membership) {
            var queue = new PaxosCmdQueue();
            config = PaxosConfig.builder(config)
                    .withProposers(List.copyOf(membership))
                    .withAcceptors(List.copyOf(membership))
                    .withLearners(List.copyOf(membership))
                    .build();
            this.membership = Collections.unmodifiableSet(membership);
            this.membershipList = Collections.unmodifiableList(List.copyOf(membership));
            this.paxos = new Paxos(id, queue, config);
            this.queue = queue;
            this.timerIdToBabelTimerId = new HashMap<>();
            this.decided = false;
        }
    }

    private static final Logger logger = LogManager.getLogger(PaxosProtocol.class);

    public final static String NAME = "Paxos";

    private ProcessId id;
    private boolean joined;

    /// Instance tracking
    // nextInstance is the first instance that we don't know the decision for.
    private final TreeMap<Integer, InstanceState> instances;
    private final PaxosCmdQueue executeQueue;
    private int nextInstance;

    /// Proposal
    private final ArrayDeque<ProposalValue> proposalQueue;

    public PaxosProtocol(Properties props) throws HandlerRegistrationException {
        super(NAME, ID);

        this.id = null;
        this.joined = false;

        /// Instance tracking
        this.instances = new TreeMap<>();
        this.executeQueue = new PaxosCmdQueue();
        this.nextInstance = 0;

        /// Proposal queue
        this.proposalQueue = new ArrayDeque<>();

        /*--------------------- Register Timer Handlers ----------------------------- */
        this.registerTimerHandler(PaxosTimer.ID, this::onPaxosTimer);

        /*--------------------- Register Request Handlers ----------------------------- */
        this.registerRequestHandler(ProposeRequest.ID, this::onProposeRequest);
        this.registerRequestHandler(AddReplicaRequest.ID, this::onAddReplicaRequest);
        this.registerRequestHandler(RemoveReplicaRequest.ID, this::onRemoveReplicaRequest);

        /*--------------------- Register Notification Handlers ----------------------------- */
        this.subscribeNotification(JoinedNotification.ID, this::onJoined);
        this.subscribeNotification(ChannelReadyNotification.ID, this::onChannelReady);
        this.subscribeNotification(UnchangedConfigurationNotification.ID, this::onUnchangedConfiguration);

        /*-------------------- Register Channel Event ------------------------------- */

    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
    }

    /*--------------------------------- Helpers ---------------------------------------- */

    private InstanceState upsertInstanceWithMembership(int instance, List<ProcessId> membership) {
        var state = instances.get(instance);
        if (state == null) {
            state = new InstanceState(id, new PaxosConfig(), Set.copyOf(membership));
            instances.put(instance, state);
        } else if (!membership.stream().allMatch(state.membership::contains)) {
            throw new IllegalArgumentException("Membership missmatch on instance " + instance);
        }
        return state;
    }

    private void executeInstanceQueue(int instance) {
        var state = instances.get(instance);
        assert state != null;

        logger.debug("Executing instance {} queue", instance);
        while (!state.queue.isEmpty()) {
            state.queue.transferTo(this.executeQueue);
            while (!this.executeQueue.isEmpty()) {
                var cmd = this.executeQueue.pop();
                switch (cmd.getKind()) {
                    case Decided -> {
                        var decided = cmd.getDecided();

                        assert !state.decided;
                        state.decided = true;

                        if (state.originalProposal != null && !decided.value().equals(state.originalProposal))
                            this.proposalQueue.addFirst(state.originalProposal);

                        var operation = decided.value().data;
                        var notification = new DecidedNotification(instance, operation);
                        this.triggerNotification(notification);
                        logger.trace("Triggered DecidedNotification for instance {}", instance);
                    }
                    case SendPrepareRequest -> {
                        var prepare = cmd.getSendPrepareRequest();
                        var message = new PrepareRequestMessage(
                                instance,
                                state.membershipList,
                                prepare.ballot());
                        if (prepare.processId().equals(this.id)) {
                            logger.trace("Sending PrepareRequestMessage to self");
                            state.paxos.receivePrepareRequest(prepare.processId(), prepare.ballot());
                        } else {
                            var host = PaxosBabel.hostFromProcessId(prepare.processId());
                            logger.trace("Sending PrepareRequestMessage to {}", host);
                            this.sendMessage(message, host);
                        }
                    }
                    case SendPrepareOk -> {
                        var prepareOk = cmd.getSendPrepareOk();
                        var message = new PrepareOkMessage(
                                instance,
                                prepareOk.ballot(),
                                prepareOk.highestAccept(),
                                false);
                        if (prepareOk.processId().equals(this.id)) {
                            logger.trace("Sending PrepareOkMessage to self");
                            state.paxos.receivePrepareOk(prepareOk.processId(), prepareOk.ballot(),
                                    prepareOk.highestAccept());
                        } else {
                            var host = PaxosBabel.hostFromProcessId(prepareOk.processId());
                            logger.trace("Sending PrepareOkMessage to {}", host);
                            this.sendMessage(message, host);
                        }
                    }
                    case SendAcceptRequest -> {
                        var accept = cmd.getSendAcceptRequest();
                        var message = new AcceptRequestMessage(instance, state.membershipList, accept.proposal());
                        if (accept.processId().equals(this.id)) {
                            logger.trace("Sending AcceptRequestMessage to self");
                            state.paxos.receiveAcceptRequest(accept.processId(), accept.proposal());
                        } else {
                            var host = PaxosBabel.hostFromProcessId(accept.processId());
                            logger.trace("Sending AcceptRequestMessage to {}", host);
                            this.sendMessage(message, host);
                        }
                    }
                    case SendAcceptOk -> {
                        var acceptOk = cmd.getSendAcceptOk();
                        var message = new AcceptOkMessage(
                                instance,
                                acceptOk.ballot());
                        if (acceptOk.processId().equals(this.id)) {
                            logger.trace("Sending AcceptOkMessage to self");
                            state.paxos.receiveAcceptOk(acceptOk.processId(), acceptOk.ballot());
                        } else {
                            var host = PaxosBabel.hostFromProcessId(acceptOk.processId());
                            logger.trace("Sending AcceptOkMessage to {}", host);
                            this.sendMessage(message, host);
                        }
                    }
                    case SendDecided -> {
                        var decided = cmd.getSendDecided();
                        var message = new DecidedMessage(instance, state.membershipList, decided.value());
                        if (decided.processId().equals(this.id)) {
                            logger.trace("Sending DecidedMessage to self");
                            state.paxos.receiveDecided(decided.processId(), decided.value());
                        } else {
                            var host = PaxosBabel.hostFromProcessId(decided.processId());
                            logger.trace("Sending DecidedMessage to {}", host);
                            this.sendMessage(message, host);
                        }
                    }
                    case SetupTimer -> {
                        var setup = cmd.getSetupTimer();
                        var timer = new PaxosTimer(instance, setup.timerId());
                        var timeout = setup.timeout().toMillis();

                        logger.trace("Setting up timer {} for instance {} with timeout {}ms", setup.timerId(), instance,
                                timeout);
                        var babelTimerId = this.setupTimer(timer, timeout);
                        state.timerIdToBabelTimerId.put(setup.timerId(), babelTimerId);
                    }
                    case CancelTimer -> {
                        var cancel = cmd.getCancelTimer();
                        var babelTimerId = state.timerIdToBabelTimerId.remove(cancel.timerId());
                        logger.trace("Cancelling timer {} for instance {}", cancel.timerId(), instance);
                        if (babelTimerId != null)
                            this.cancelTimer(babelTimerId);
                    }
                }
            }
        }
    }

    private void tryPropose() {
        while (this.instances.containsKey(this.nextInstance) && this.instances.get(this.nextInstance).decided)
            this.nextInstance += 1;

        var state = this.instances.get(this.nextInstance);
        if (state == null) {
            /*- If the next instance is null that means that we don't know its membership yet.
            *   We have to wait until we receive a message from another member with the membership
            *   or the state machine tells us that our previous decision did not change the membership. */
            return;
        }

        logger.debug("Trying to propose on instance {} with queue size = {} and canPropose = {}",
                this.nextInstance, proposalQueue.size(), state.paxos.canPropose());

        if (!this.proposalQueue.isEmpty() && state.paxos.canPropose()) {
            assert state.originalProposal == null;
            state.originalProposal = this.proposalQueue.remove();
            logger.debug("Proposing {} on instance {}", state.originalProposal, this.nextInstance);
            state.paxos.propose(state.originalProposal);
            this.executeInstanceQueue(this.nextInstance);
        }
    }

    /*--------------------------------- Timer Handlers ---------------------------------------- */

    private void onPaxosTimer(PaxosTimer timer, long timerId) {
        var instance = timer.instance;
        var state = instances.get(instance);
        assert state != null;

        var babelTimerId = state.timerIdToBabelTimerId.remove(timer.timerId);
        assert babelTimerId != null;

        logger.trace("Triggering timer {} on instance {}", timer.timerId, instance);
        state.paxos.triggerTimer(timer.timerId);
        executeInstanceQueue(instance);
    }

    /*--------------------------------- Message Handlers ---------------------------------------- */

    private void onAcceptOk(AcceptOkMessage msg, Host host, short sourceProto, int channelId) {
        logger.trace("Received AcceptOkMessage from {} on instance {}", host, msg.instance);
        var processId = PaxosBabel.hostToProcessId(host);
        var instance = msg.instance;
        var state = this.instances.get(instance);
        state.paxos.receiveAcceptOk(processId, msg.ballot);
        this.executeInstanceQueue(instance);
        this.tryPropose();
    }

    private void onAcceptRequest(AcceptRequestMessage msg, Host host, short sourceProto, int channelId) {
        logger.trace("Received accept request from {} for instance {}", host, msg.instance);
        var processId = PaxosBabel.hostToProcessId(host);
        var instance = msg.instance;
        var state = this.upsertInstanceWithMembership(instance, msg.membership);
        state.paxos.receiveAcceptRequest(processId, msg.proposal);
        this.executeInstanceQueue(instance);
        this.tryPropose();
    }

    private void onDecided(DecidedMessage msg, Host host, short sourceProto, int channelId) {
        logger.trace("Received decided message for instance {}", msg.instance);
        var processId = PaxosBabel.hostToProcessId(host);
        var instance = msg.instance;
        var state = this.upsertInstanceWithMembership(instance, msg.membership);
        state.paxos.receiveDecided(processId, msg.value);
        this.executeInstanceQueue(instance);
        this.tryPropose();
    }

    private void onPrepareOk(PrepareOkMessage msg, Host host, short sourceProto, int channelId) {
        logger.trace("Received prepare ok from {} for instance {}", host, msg.instance);
        var processId = PaxosBabel.hostToProcessId(host);
        var instance = msg.instance;
        var state = this.instances.get(instance);
        state.paxos.receivePrepareOk(processId, msg.ballot, msg.acceptedProposal);
        this.executeInstanceQueue(instance);
        this.tryPropose();
    }

    private void onPrepareRequest(PrepareRequestMessage msg, Host host, short sourceProto, int channelId) {
        logger.trace("Received prepare request from {} for instance {}", host, msg.instance);
        var processId = PaxosBabel.hostToProcessId(host);
        var instance = msg.instance;
        var state = this.upsertInstanceWithMembership(instance, msg.membership);
        state.paxos.receivePrepareRequest(processId, msg.ballot);
        this.executeInstanceQueue(instance);
        this.tryPropose();
    }

    /*--------------------------------- Request Handlers ---------------------------------------- */

    private void onProposeRequest(ProposeRequest request, short sourceProto) {
        logger.debug("Received propose request: {}", request);
        var value = new ProposalValue(request.operation);
        this.proposalQueue.add(value);
        this.tryPropose();
    }

    private void onAddReplicaRequest(AddReplicaRequest request, short sourceProto) {
        logger.info("Received add replica request {}", request);
        if (request.instance == nextInstance && !this.instances.containsKey(request.instance)) {
            var membership = new ArrayList<>(this.instances.get(request.instance - 1).membership);
            var replicaId = PaxosBabel.hostToProcessId(request.replica);
            membership.add(replicaId);
            this.upsertInstanceWithMembership(request.instance, membership);
        }
    }

    private void onRemoveReplicaRequest(RemoveReplicaRequest request, short sourceProto) {
        logger.info("Received remove replica request {}", request);
        if (request.instance == nextInstance && !this.instances.containsKey(request.instance)) {
            logger.info("Removing replica from membership: {}", request.replica);
            var replicaId = PaxosBabel.hostToProcessId(request.replica);
            var membership = this.instances.get(request.instance - 1).membership.stream()
                    .filter(id -> !id.equals(replicaId))
                    .toList();
            this.upsertInstanceWithMembership(request.instance, membership);
        }
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
        assert this.instances.isEmpty();

        logger.info("Joined notification received: {}", notification);

        if (this.joined)
            throw new RuntimeException("Already joined");
        this.joined = true;

        var membership = notification.membership.stream().map(PaxosBabel::hostToProcessId).toList();
        this.nextInstance = notification.joinInstance;
        this.upsertInstanceWithMembership(notification.joinInstance, membership);
    }

    private void onUnchangedConfiguration(UnchangedConfigurationNotification notification, short sourceProto) {
        assert sourceProto == StateMachine.ID;

        var membership = this.instances.get(notification.instance).membership.stream().toList();
        this.upsertInstanceWithMembership(notification.instance + 1, membership);
        this.tryPropose();
    }
}
