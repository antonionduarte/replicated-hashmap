package asd.protocols.paxos;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos2.Ballot;
import asd.paxos2.ProcessId;
import asd.paxos2.ProposalValue;
import asd.paxos2.Proposal;
import asd.protocols.agreement.Agreement;
import asd.protocols.paxos.messages.AcceptOkMessage;
import asd.protocols.paxos.messages.AcceptRequestMessage;
import asd.protocols.paxos.messages.DecidedMessage;
import asd.protocols.paxos.messages.PrepareOkMessage;
import asd.protocols.paxos.messages.PrepareRequestMessage;
import asd.protocols.paxos.notifications.DecidedNotification;
import asd.protocols.paxos.notifications.JoinedNotification;
import asd.protocols.paxos.requests.AddReplicaRequest;
import asd.protocols.paxos.requests.ProposeRequest;
import asd.protocols.paxos.requests.RemoveReplicaRequest;
import asd.protocols.paxos.timer.ForceProposalTimer;
import asd.protocols.paxos.timer.MajorityTimeoutTimer;
import asd.protocols.statemachine.StateMachine;
import asd.protocols.statemachine.notifications.ChannelReadyNotification;
import asd.protocols.statemachine.notifications.UnchangedConfigurationNotification;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.data.Host;

public class PaxosProtocol extends GenericProtocol implements Agreement {
    private static enum ProposePhase {
        PREPARE,
        ACCEPT,
    }

    private static class Instance {
        public final Set<ProcessId> membership;
        public final int quorumSize;

        // Acceptor
        public Ballot highestPromisedBallot;
        public Ballot highestAcceptedBallot;
        public Optional<ProposalValue> highestAcceptedValue;

        /// Learner
        public Optional<ProposalValue> decidedValue;

        /// Proposer
        public boolean isProposing;
        public boolean isOriginalProposeValue;
        public ProposePhase proposePhase;
        public Set<ProcessId> proposeOks;
        public ProposalValue proposeValue;
        public Ballot proposeBallot;

        public Instance(Set<ProcessId> membership) {
            this.membership = membership;
            this.quorumSize = membership.size() / 2 + 1;

            /// Acceptor
            this.highestPromisedBallot = new Ballot();
            this.highestAcceptedBallot = new Ballot();
            this.highestAcceptedValue = Optional.empty();

            /// Learner
            this.decidedValue = Optional.empty();

            /// Proposer
            this.isProposing = false;
            this.isOriginalProposeValue = false;
            this.proposePhase = ProposePhase.PREPARE;
            this.proposeOks = new HashSet<>();
            this.proposeValue = null;
            this.proposeBallot = null;
        }

        public boolean isDecided() {
            return decidedValue.isPresent();
        }

    }

    private static final Logger logger = LogManager.getLogger(PaxosProtocol.class);

    public final static String NAME = "Paxos";

    private ProcessId id;
    private boolean joined;

    /// Instance tracking
    // nextInstance is the first instance that we don't know the decision for.
    private final TreeMap<Integer, Instance> instances;
    private int nextInstance;

    /// Proposal
    // proposalTimeout is how long we wait to attempt to propose if the current
    // instance is not decided but already has an accepted value.
    // proposalTimeoutTimer is -1 if there is no active timer.
    // majorityTimeoutTimer is -1 if there is no active timer.
    private final Queue<byte[]> proposalQueue;
    private final Duration proposalTimeout;
    private long proposalTimeoutTimer;
    private long majorityTimeoutTimer;

    public PaxosProtocol(Properties props) throws HandlerRegistrationException {
        super(NAME, ID);

        this.id = null;
        this.joined = false;

        /// Instance tracking
        this.instances = new TreeMap<>();
        this.nextInstance = 0;

        /// Proposal queue
        this.proposalQueue = new ArrayDeque<>();
        this.proposalTimeout = Duration.parse(props.getProperty("paxos_proposal_timeout"));
        this.proposalTimeoutTimer = -1;
        this.majorityTimeoutTimer = -1;

        /*--------------------- Register Timer Handlers ----------------------------- */
        this.registerTimerHandler(ForceProposalTimer.ID, this::onForceProposal);
        this.registerTimerHandler(MajorityTimeoutTimer.ID, this::onMajorityTimeout);

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

    private Instance upsertInstanceWithMembership(int instance, List<ProcessId> membership) {
        var data = this.instances.get(instance);
        if (data == null) {
            data = new Instance(new HashSet<>(membership));
            this.instances.put(instance, data);
        } else {
            if (!membership.stream().allMatch(data.membership::contains))
                throw new RuntimeException("Membership missmatch for instance " + instance);
        }
        return data;
    }

    private Instance getInstance(int instance) {
        var data = this.instances.get(instance);
        if (data == null)
            throw new RuntimeException("Instance " + instance + " not found");
        return data;
    }

    private void propose(boolean force) {
        /*- If the next instance is null that means that we don't know its membership yet.
        *   We have to wait until we receive a message from another member with the membership
        *   or the state machine tells us that our previous decision did not change the membership. */
        var data = this.instances.get(this.nextInstance);
        if (data == null || data.isProposing || (this.proposalQueue.isEmpty() && !force)) {
            logger.debug("Not proposing, data = {}, data.isProposing = {}, proposalQueue.isEmpty() = {}, force = {}",
                    data, data != null && data.isProposing, this.proposalQueue.isEmpty(), force);
            return;
        }
        assert !data.isDecided();

        // Dont attempt to propose if someone is already trying and we are not forcing.
        if (data.highestAcceptedValue.isPresent() && !force) {
            if (this.proposalTimeoutTimer == -1) {
                logger.debug("Scheduling proposal timeout");
                var timer = new ForceProposalTimer(this.nextInstance);
                var timeout = (long) (this.proposalTimeout.toMillis() * (Math.random() + 1.0));
                this.proposalTimeoutTimer = this.setupTimer(timer, timeout);
            }
            return;
        }

        var ballot = new Ballot(this.id, data.highestPromisedBallot.sequenceNumber + 1);
        var value = force ? data.highestAcceptedValue.get() : new ProposalValue(this.proposalQueue.remove());

        assert value != null;
        assert data.isProposing == false;
        assert data.isOriginalProposeValue == false;
        assert data.proposePhase == ProposePhase.PREPARE;
        assert data.proposeOks.isEmpty();
        assert data.proposeValue == null;
        assert data.proposeBallot == null;

        data.isProposing = true;
        data.isOriginalProposeValue = !force;
        data.proposeOks.add(this.id);
        data.proposeValue = value;
        data.proposeBallot = ballot;

        // Update acceptor state
        data.highestPromisedBallot = ballot;

        var instance = this.nextInstance;
        var membership = data.membership.stream().toList();
        var request = new PrepareRequestMessage(instance, membership, ballot);
        var majorityTimer = new MajorityTimeoutTimer(instance);

        // TODO: Make this configurable
        this.majorityTimeoutTimer = this.setupTimer(majorityTimer, 2000);
        this.sendToMembership(membership, request);
        // In case we are the only member of the group.
        this.checkProposalMajority(instance);
        logger.debug("Sent prepare request for instance {}, ballot = {} to {} members", instance, ballot,
                membership.size());
        logger.trace("Members = {}", membership.stream().map(PaxosBabel::hostFromProcessId).toList());
    }

    private void checkProposalMajority(int instance) {
        var data = this.instances.get(instance);
        assert data.isProposing && !data.isDecided();

        logger.trace("Checking proposal majority for instance {} and phase {}", instance, data.proposePhase);
        var hasMajority = data.proposeOks.size() >= data.quorumSize;
        if (!hasMajority) {
            logger.trace("Proposal majority not reached for instance {} got {}/{}",
                    instance, data.proposeOks.size(), data.quorumSize);
            return;
        }
        logger.trace("Proposal majority reached for instance {} got {}/{}",
                instance, data.proposeOks.size(), data.quorumSize);

        var membership = data.membership.stream().toList();
        switch (data.proposePhase) {
            case PREPARE -> {
                var request = new AcceptRequestMessage(instance, membership, data.proposeBallot, data.proposeValue);
                data.proposePhase = ProposePhase.ACCEPT;
                data.proposeOks.clear();
                if (data.highestPromisedBallot.equals(data.proposeBallot))
                    data.proposeOks.add(this.id);
                this.sendToMembership(membership, request);
            }
            case ACCEPT -> {
                var message = new DecidedMessage(instance, membership, data.proposeValue);
                var notification = new DecidedNotification(instance, UUID.randomUUID(), data.proposeValue.data);
                data.decidedValue = Optional.of(data.proposeValue);
                data.proposeOks.clear();
                this.tryAdvanceNextInstance();
                this.cancelTimer(this.majorityTimeoutTimer);
                this.sendToMembership(membership, message);
                this.triggerNotification(notification);
            }
        }
    }

    private void sendToMembership(Collection<ProcessId> membership, ProtoMessage message) {
        for (var member : membership) {
            if (member.equals(this.id))
                continue;

            var host = PaxosBabel.hostFromProcessId(member);
            this.sendMessage(message, host);
        }
    }

    private void tryAdvanceNextInstance() {
        while (this.instances.containsKey(this.nextInstance) && this.instances.get(this.nextInstance).isDecided())
            this.nextInstance += 1;
        logger.trace("Next instance is now {}", this.nextInstance);
    }

    /*--------------------------------- Timer Handlers ---------------------------------------- */

    public void onForceProposal(ForceProposalTimer timer, long timerId) {
        assert timer.instance == this.nextInstance;
        this.proposalTimeoutTimer = -1;
        this.propose(true);
    }

    public void onMajorityTimeout(MajorityTimeoutTimer timer, long timerId) {
        assert timerId == this.majorityTimeoutTimer;
        assert timer.instance == this.nextInstance;
        var data = this.instances.get(timer.instance);
        assert data.isProposing;

        logger.debug("Majority timeout for instance {}, proposePhase = {}", timer.instance, data.proposePhase);
        this.majorityTimeoutTimer = -1;

        if (data.isOriginalProposeValue)
            this.proposalQueue.add(data.proposeValue.data);

        data.isProposing = false;
        data.isOriginalProposeValue = false;
        data.proposePhase = ProposePhase.PREPARE;
        data.proposeOks.clear();
        data.proposeValue = null;
        data.proposeBallot = null;

        this.propose(false);
    }

    /*--------------------------------- Message Handlers ---------------------------------------- */

    private void onAcceptOk(AcceptOkMessage msg, Host host, short sourceProto, int channelId) {
        var data = this.getInstance(msg.instance);
        assert data.isProposing;

        if (data.isDecided() || data.proposePhase != ProposePhase.ACCEPT
                || msg.ballot.compare(data.proposeBallot) != Ballot.Order.EQUAL) {
            logger.trace(
                    "Ignoring accept ok for instance {}, proposePhase = {}, isDecided = {}, ballot = {}, proposeBallot = {}",
                    msg.instance, data.proposePhase, data.isDecided(), msg.ballot, data.proposeBallot);
            return;
        }

        logger.trace("Received accept ok for instance {}, ballot = {}", msg.instance, msg.ballot);

        var senderId = PaxosBabel.hostToProcessId(host);
        data.proposeOks.add(senderId);
        this.checkProposalMajority(msg.instance);
    }

    private void onAcceptRequest(AcceptRequestMessage msg, Host host, short sourceProto, int channelId) {
        var data = this.upsertInstanceWithMembership(msg.instance, msg.membership);

        if (msg.ballot.compare(data.highestPromisedBallot) == Ballot.Order.LESS) {
            logger.trace("Rejecting accept request for instance {}, ballot = {}", msg.instance, msg.ballot);
            return; // TODO: maybe reject?
        }

        logger.trace("Accepting request for instance {} with ballot {} and host {}", msg.instance, msg.ballot, host);

        data.highestPromisedBallot = msg.ballot;
        data.highestAcceptedBallot = msg.ballot;
        data.highestAcceptedValue = Optional.of(msg.value);

        var response = new AcceptOkMessage(msg.instance, data.highestAcceptedBallot);
        this.sendMessage(response, host);
    }

    private void onDecided(DecidedMessage msg, Host host, short sourceProto, int channelId) {
        var data = this.upsertInstanceWithMembership(msg.instance, msg.membership);

        if (data.isDecided()) {
            if (!data.decidedValue.get().equals(msg.value))
                throw new RuntimeException("Decided value missmatch for instance " + msg.instance);
            logger.warn("Received decided message for instance {} but already decided", msg.instance);
            return;
        }

        data.decidedValue = Optional.of(msg.value);

        var operation = msg.value.data;
        var notification = new DecidedNotification(msg.instance, UUID.randomUUID(), operation);
        this.triggerNotification(notification);
        this.tryAdvanceNextInstance();
    }

    private void onPrepareOk(PrepareOkMessage msg, Host host, short sourceProto, int channelId) {
        var data = this.getInstance(msg.instance);
        assert data.isProposing;

        if (data.isDecided() || data.proposePhase != ProposePhase.PREPARE || !data.proposeBallot.equals(msg.ballot)) {
            logger.trace(
                    "Not accepting prepareOk for instance {} with ballot {} and host {} and decided = {} and phase = {}",
                    msg.instance, msg.ballot, host, data.isDecided(), data.proposePhase);
            return;
        }

        logger.trace("Accepting prepareOk for instance {} with ballot {} and host {}", msg.instance, msg.ballot, host);

        if (msg.acceptedProposal.isPresent()) {
            var acceptedProposal = msg.acceptedProposal.get();
            assert acceptedProposal.slot == msg.instance;
            if (acceptedProposal.ballot.compare(data.proposeBallot) == Ballot.Order.GREATER) {
                if (data.isOriginalProposeValue)
                    this.proposalQueue.add(data.proposeValue.data);
                data.isOriginalProposeValue = false;
                data.proposeValue = acceptedProposal.value;
            }
        }

        var senderId = PaxosBabel.hostToProcessId(host);
        data.proposeOks.add(senderId);
        this.checkProposalMajority(msg.instance);
    }

    private void onPrepareRequest(PrepareRequestMessage msg, Host host, short sourceProto, int channelId) {
        var data = this.upsertInstanceWithMembership(msg.instance, msg.membership);

        if (msg.ballot.compare(data.highestPromisedBallot) != Ballot.Order.GREATER) {
            logger.trace("Rejecting prepare request for instance {}, ballot = {}", msg.instance, msg.ballot);
            return;
        }

        logger.trace("Accepting prepare request for instance {} with ballot {} and host {}", msg.instance, msg.ballot,
                host);

        data.highestPromisedBallot = msg.ballot;

        var acceptedProposal = data.highestAcceptedValue
                .or(() -> data.decidedValue)
                .map(v -> new Proposal(data.highestAcceptedBallot, msg.instance, v));
        var response = new PrepareOkMessage(
                msg.instance,
                msg.ballot,
                acceptedProposal,
                data.isDecided());
        this.sendMessage(response, host);
    }

    /*--------------------------------- Request Handlers ---------------------------------------- */

    private void onProposeRequest(ProposeRequest request, short sourceProto) {
        this.proposalQueue.add(request.operation);
        logger.debug("Added operation to queue, size is now {}", this.proposalQueue.size());
        this.propose(false);
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
        this.propose(false);
    }
}
