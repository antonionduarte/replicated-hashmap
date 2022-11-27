package asd;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;

import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.protocols.paxos.PaxosProtocol;
import asd.protocols.statemachine.StateMachine;
import asd.protocols.statemachine.requests.OrderRequest;

public class InteractivePaxos extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(InteractivePaxos.class);

    public static final short ID = 8000;
    public static final String NAME = "Interactive Paxos";

    private final PaxosProtocol paxos;

    public InteractivePaxos(PaxosProtocol kad) throws HandlerRegistrationException {
        super(NAME, ID);
        this.paxos = kad;

    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        new Thread(new Runnable() {

            @Override
            public void run() {
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
                                sendRequest(request, StateMachine.PROTOCOL_ID);
                                logger.info("Sent order request {}", request);
                            }
                            default -> System.out.println("Unknown command " + components[0]);
                        }
                    }
                }
            }

        }).start();
    }
}
