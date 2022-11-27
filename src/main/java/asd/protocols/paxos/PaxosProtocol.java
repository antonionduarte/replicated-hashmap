package asd.protocols.paxos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.Paxos;
import asd.paxos.PaxosIO;
import asd.paxos.ProcessId;
import asd.paxos.proposal.Proposal;
import asd.paxos.proposal.ProposalNumber;
import asd.paxos.proposal.ProposalValue;
import asd.protocols.agreement.Agreement;
import asd.protocols.paxos.notifications.DecidedNotification;
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

    private static class IO implements PaxosIO {
        public final PaxosProtocol protocol;
        public final ProcessId processId;
        public final int instance;

        public IO(PaxosProtocol protocol, ProcessId processId, int instance) {
            this.protocol = protocol;
            this.processId = processId;
            this.instance = instance;
        }

        @Override
        public ProcessId getProcessId() {
            return this.processId;
        }

        @Override
        public void decided(ProposalValue value) {
            var pair = PaxosBabel.operationFromBytes(value.data);
            var operationId = pair.getLeft();
            var operation = pair.getRight();
            var notification = new DecidedNotification(this.instance, operationId, operation);
            logger.trace("Decided UUID {} with payload size {}", operationId, operation.length);
            this.protocol.triggerNotification(notification);
        }

        @Override
        public void sendPrepareRequest(ProcessId processId, ProposalNumber proposalNumber) {
            var message = new PrepareRequestMessage(this.instance, proposalNumber);
            this.protocol.sendMessage(message, PaxosBabel.hostFromProcessId(processId));
        }

        @Override
        public void sendPrepareOk(
                ProcessId processId,
                ProposalNumber proposalNumber,
                Optional<Proposal> highestAccept) {
            var message = new PrepareOkMessage(this.instance, proposalNumber, highestAccept);
            this.protocol.sendMessage(message, PaxosBabel.hostFromProcessId(processId));
        }

        @Override
        public void sendAcceptRequest(ProcessId processId, Proposal proposal) {
            var message = new AcceptRequestMessage(this.instance, proposal);
            this.protocol.sendMessage(message, PaxosBabel.hostFromProcessId(processId));
        }

        @Override
        public void sendAcceptOk(ProcessId processId, ProposalNumber proposalNumber) {
            var message = new AcceptOkMessage(this.instance, proposalNumber);
            this.protocol.sendMessage(message, PaxosBabel.hostFromProcessId(processId));
        }

        @Override
        public void sendDecide(ProcessId processId, ProposalValue proposal) {
            var message = new DecidedMessage(this.instance, proposal);
            this.protocol.sendMessage(message, PaxosBabel.hostFromProcessId(processId));
        }
    }

    private final int paxos_alpha;

    private Host self;
    private ProcessId selfProcessId;
    private final HashMap<Integer, Paxos> instances;
    private int nextInstance;
    private List<ProcessId> latestMembership;

    public PaxosProtocol(Properties props) throws HandlerRegistrationException {
        super(NAME, ID);

        this.paxos_alpha = Integer.parseInt(props.getProperty("paxos_alpha"));

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

        var instance = this.instances.get(msg.instance);
        if (instance == null) {
            logger.warn("Received AcceptOkMessage for unknown instance {}", msg.instance);
            return;
        }

        var processId = PaxosBabel.hostToProcessId(host);
        instance.receiveAcceptOk(processId, msg.messageNumber);
    }

    private void onAcceptRequest(AcceptRequestMessage msg, Host host, short sourceProto, int channelId) {
        logger.info("Received AcceptRequestMessage {} from {}", msg, host);

        var instance = this.instances.get(msg.instance);
        if (instance == null) {
            logger.warn("Received AcceptRequestMessage for unknown instance {}", msg.instance);
            return;
        }

        var processId = PaxosBabel.hostToProcessId(host);
        instance.receiveAcceptRequest(processId, msg.proposal);
    }

    private void onDecided(DecidedMessage msg, Host host, short sourceProto, int channelId) {
        logger.info("Received DecidedMessage {} from {}", msg, host);

        var instance = this.instances.get(msg.instance);
        if (instance == null) {
            logger.warn("Received DecidedMessage for unknown instance {}", msg.instance);
            return;
        }

        var processId = PaxosBabel.hostToProcessId(host);
        instance.receiveDecide(processId, msg.value);
    }

    private void onPrepareOk(PrepareOkMessage msg, Host host, short sourceProto, int channelId) {
        logger.info("Received PrepareOkMessage {} from {}", msg, host);

        var instance = this.instances.get(msg.instance);
        if (instance == null) {
            logger.warn("Received PrepareOkMessage for unknown instance {}", msg.instance);
            return;
        }

        var processId = PaxosBabel.hostToProcessId(host);
        instance.receivePrepareOk(processId, msg.proposalNumber, msg.acceptedProposal);
    }

    private void onPrepareRequest(PrepareRequestMessage msg, Host host, short sourceProto, int channelId) {
        logger.info("Received PrepareRequestMessage {} from {}", msg, host);

        var instance = this.instances.get(msg.instance);
        if (instance == null) {
            logger.warn("Received PrepareRequestMessage for unknown instance {}", msg.instance);
            return;
        }

        var processId = PaxosBabel.hostToProcessId(host);
        instance.receivePrepareRequest(processId, msg.proposalNumber);
    }

    /*--------------------------------- Request Handlers ---------------------------------------- */

    private void onProposeRequest(ProposeRequest request, short sourceProto) {
        logger.info("Received propose request {}", request);
        logger.trace("Propose request payload size {}", request.operation.length);

        var instance = this.instances.get(0);
        var payload = PaxosBabel.operationToBytes(request.operationId, request.operation);
        instance.propose(new ProposalValue(payload));
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
        this.selfProcessId = PaxosBabel.hostToProcessId(this.self);

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

        this.latestMembership = notification.membership.stream().map(PaxosBabel::hostToProcessId).toList();
        for (; this.nextInstance <= notification.joinInstance; ++this.nextInstance)
            this.instances.put(this.nextInstance,
                    new Paxos(new IO(this, this.selfProcessId, this.nextInstance), this.latestMembership));
    }
}
