package asd;

import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.protocols.app.HashApp;
import asd.protocols.app.requests.GetRequest;
import asd.protocols.app.requests.GetResponse;
import asd.protocols.app.requests.PutRequest;
import asd.protocols.app.requests.PutResponse;
import asd.protocols.statemachine.StateMachine;
import asd.protocols.statemachine.notifications.ChannelReadyNotification;
import asd.protocols.statemachine.requests.OrderRequest;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;

public class InteractivePaxos extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(InteractivePaxos.class);

    public static final short ID = 8000;
    public static final String NAME = "Interactive Paxos";

    public InteractivePaxos() throws HandlerRegistrationException {
        super(NAME, ID);

        this.subscribeNotification(ChannelReadyNotification.ID, this::onChannelReady);
        this.registerReplyHandler(GetResponse.ID, this::onGetResponse);
        this.registerReplyHandler(PutResponse.ID, this::onPutResponse);
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        var protocol = this;
        new Thread(new Runnable() {

            @Override
            public void run() {
                long opid = 0;
                try (var scanner = new Scanner(System.in)) {
                    while (true) {
                        var line = scanner.nextLine();
                        var components = line.split(" ");

                        switch (components[0]) {
                            case "exit" -> System.exit(0);
                            case "order" -> {
                                var operation = components[1];
                                var operationId = UUID.randomUUID();
                                var request = new OrderRequest(operationId, operation.getBytes());
                                sendRequest(request, StateMachine.ID);
                                logger.info("Sent order request {}", request);
                            }
                            case "get" -> {
                                var key = components[1];
                                var message = new GetRequest(opid++, key);
                                logger.info("SENDING READ {}", message.operationId);
                                protocol.sendRequest(message, HashApp.PROTO_ID);
                            }
                            case "set" -> {
                                var key = components[1];
                                var value = components[2];
                                var message = new PutRequest(opid++, key, value.getBytes());
                                logger.info("SENDING WRITE {}", message.operationId);
                                protocol.sendRequest(message, HashApp.PROTO_ID);
                            }
                            default -> System.out.println("Unknown command " + components[0]);
                        }
                    }
                }
            }

        }).start();
    }

    private void onChannelReady(ChannelReadyNotification notification, short sourceProto) {
        var channelId = notification.getChannelId();
        this.registerSharedChannel(channelId);
    }

    private void onGetResponse(GetResponse notification, short sourceProto) {
        logger.info("Received get response {} = {}", notification.operationId, new String(notification.value));
    }

    private void onPutResponse(PutResponse notification, short sourceProto) {
        logger.info("Received put response {}", notification.operationId);
    }
}
