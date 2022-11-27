package asd.protocols.paxos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.protocols.agreement.Agreement;
import asd.protocols.paxos.messages.AcceptOkMessage;
import asd.protocols.paxos.messages.AcceptRequestMessage;
import asd.protocols.paxos.messages.DecidedMessage;
import asd.protocols.paxos.messages.PrepareOkMessage;
import asd.protocols.paxos.messages.PrepareRequestMessage;
import asd.protocols.paxos.notifications.JoinedNotification;
import asd.protocols.paxos.requests.AddReplicaRequest;
import asd.protocols.paxos.requests.ProposeRequest;
import asd.protocols.paxos.requests.RemoveReplicaRequest;
import asd.protocols.statemachine.notifications.ChannelReadyNotification;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;

public class PaxosProtocol extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(PaxosProtocol.class);

    public final static short ID = Agreement.PROTOCOL_ID;
    public final static String NAME = "Paxos";

    private static class PaxosInstance {
        // Common
        public final Set<Host> membership;

        // Proposer
        public Proposal proposal;
        public int currentPrepareOkCount;
        public boolean sentAcceptRequest;
        public int currentAcceptOkCount;

        // Acceptor
        public ProposalNumber highestPromise;
        public Optional<Proposal> highestAccept;

        public PaxosInstance(Set<Host> membership) {
            this.membership = membership;

            // Proposer
            this.proposal = null;
            this.currentPrepareOkCount = 0;
            this.sentAcceptRequest = false;
            this.currentAcceptOkCount = 0;

            // Acceptor
            this.highestPromise = new ProposalNumber();
            this.highestAccept = Optional.empty();
        }

        public int quoromSize() {
            return membership.size() / 2 + 1;
        }
    }

    private Host self;
    private final HashMap<Integer, PaxosInstance> instances;
    private int nextInstance;
    private List<Host> latestMembership;

    public PaxosProtocol() throws HandlerRegistrationException {
        super(NAME, ID);

        this.instances = new HashMap<>();
        this.nextInstance = 0;
        this.latestMembership = new ArrayList<>();

        /*--------------------- Register Timer Handlers ----------------------------- */

        /*--------------------- Register Request Handlers ----------------------------- */
        this.registerRequestHandler(ProposeRequest.ID, this::onProposeRequest);
        this.registerRequestHandler(AddReplicaRequest.ID, this::onAddReplicaRequest);
        this.registerRequestHandler(RemoveReplicaRequest.ID, this::onRemoveReplicaRequest);

        /*--------------------- Register Notification Handlers ----------------------------- */
        this.subscribeNotification(JoinedNotification.ID, this::onJoined);
        this.subscribeNotification(ChannelReadyNotification.NOTIFICATION_ID, this::onChannelReady);

        /*-------------------- Register Channel Event ------------------------------- */

    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
    }

    /*--------------------------------- Message Handlers ---------------------------------------- */

    private void onAcceptOk(AcceptOkMessage msg, Host host, short sourceProto, int channelId) {
        logger.info("Received AcceptOkMessage {} from {}", msg, host);

        var paxosInstance = instances.get(msg.instance);
        if (!msg.messageNumber.equals(paxosInstance.proposal.number))
            return;

        paxosInstance.currentAcceptOkCount++;
        if (paxosInstance.currentAcceptOkCount != paxosInstance.quoromSize())
            return;

        logger.info("Proposed value {} was decided", paxosInstance.proposal.value);
        var message = new DecidedMessage(msg.instance, paxosInstance.proposal.value);
        for (var replica : paxosInstance.membership)
            this.sendMessage(message, replica);
    }

    private void onAcceptRequest(AcceptRequestMessage msg, Host host, short sourceProto, int channelId) {
        logger.info("Received AcceptRequestMessage {} from {}", msg, host);

        var paxosInstance = instances.get(msg.instance);
        if (msg.proposal.number.compare(paxosInstance.highestPromise) == ProposalNumber.Order.LESS)
            return;

        paxosInstance.highestPromise = msg.proposal.number;
        paxosInstance.highestAccept = Optional.of(msg.proposal);

        var message = new AcceptOkMessage(msg.instance, msg.proposal.number);
        this.sendMessage(message, host);
    }

    private void onDecided(DecidedMessage msg, Host host, short sourceProto, int channelId) {
        logger.info("Received DecidedMessage {} from {}", msg, host);

    }

    private void onPrepareOk(PrepareOkMessage msg, Host host, short sourceProto, int channelId) {
        logger.info("Received PrepareOkMessage {} from {}", msg, host);

        var paxosInstance = this.instances.get(msg.instance);
        if (!paxosInstance.proposal.number.equals(msg.proposalNumber))
            return;

        paxosInstance.currentPrepareOkCount++;
        if (paxosInstance.currentPrepareOkCount < paxosInstance.quoromSize())
            return;

        if (paxosInstance.sentAcceptRequest)
            return;

        paxosInstance.sentAcceptRequest = true;
        var request = new AcceptRequestMessage(msg.instance, paxosInstance.proposal);
        for (var replica : paxosInstance.membership)
            this.sendMessage(request, replica);
    }

    private void onPrepareRequest(PrepareRequestMessage msg, Host host, short sourceProto, int channelId) {
        logger.info("Received PrepareRequestMessage {} from {}", msg, host);

        var paxosInstance = this.instances.computeIfAbsent(msg.instance,
                (__) -> new PaxosInstance(new HashSet<>(this.latestMembership)));

        if (msg.proposalNumber.compare(paxosInstance.highestPromise) != ProposalNumber.Order.GREATER)
            return;

        paxosInstance.highestPromise = msg.proposalNumber;
        var message = new PrepareOkMessage(msg.instance, msg.proposalNumber, paxosInstance.highestAccept);
        this.sendMessage(message, host);
    }

    /*--------------------------------- Request Handlers ---------------------------------------- */

    private void onProposeRequest(ProposeRequest request, short sourceProto) {
        logger.info("Received propose request {}", request);

        var instance = this.nextInstance++;
        var paxosInstance = new PaxosInstance(new HashSet<>(this.latestMembership));
        this.instances.put(instance, paxosInstance);

        var proposalNumber = new ProposalNumber(this.self, paxosInstance.highestPromise.getSequenceNumber() + 1);
        var message = new PrepareRequestMessage(instance, proposalNumber);
        paxosInstance.proposal = new Proposal(proposalNumber, request.operation);
        for (var replica : paxosInstance.membership)
            this.sendMessage(message, replica);
    }

    private void onAddReplicaRequest(AddReplicaRequest request, short sourceProto) {
        logger.info("Received add replica request {}", request);

    }

    private void onRemoveReplicaRequest(RemoveReplicaRequest request, short sourceProto) {
        logger.info("Received remove replica request {}", request);

    }

    /*--------------------------------- Notification Handlers ---------------------------------------- */

    private void onChannelReady(ChannelReadyNotification notification, short sourceProto) {
        logger.info("Received channel ready notification {}", notification);

        var channelId = notification.getChannelId();
        this.registerSharedChannel(channelId);
        this.self = notification.getMyself();

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
        logger.info("Joined notification received: {}", notification);

        this.latestMembership = notification.membership;
    }
}
