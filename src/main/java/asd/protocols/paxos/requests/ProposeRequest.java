package asd.protocols.paxos.requests;

import java.util.Arrays;
import java.util.UUID;

import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class ProposeRequest extends ProtoRequest {
    public static final short ID = PaxosProtocol.ID + 2;

    public final int instance;
    public final UUID operationId;
    public final byte[] operation;

    public ProposeRequest(int instance, UUID operationId, byte[] operation) {
        super(ID);

        this.instance = instance;
        this.operationId = operationId;
        this.operation = operation;
    }

    @Override
    public String toString() {
        return "ProposeRequest [instance=" + instance + ", operationId=" + operationId + ", operation="
                + Arrays.toString(operation) + "]";
    }

}
