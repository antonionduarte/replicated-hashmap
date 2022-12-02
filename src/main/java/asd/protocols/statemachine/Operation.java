package asd.protocols.statemachine;

import java.util.UUID;

// An operation from the application
public class Operation {
    public final UUID operationId;
    public final byte[] operation;

    public Operation(UUID operationId, byte[] operation) {
        this.operationId = operationId;
        this.operation = operation;
    }
}
