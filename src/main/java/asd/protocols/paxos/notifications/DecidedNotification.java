package asd.protocols.paxos.notifications;

import java.util.Arrays;
import java.util.UUID;

import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;

public class DecidedNotification extends ProtoNotification {
    public static final short ID = PaxosProtocol.ID + 1;

    public final int instance;
    public final UUID operationId;
    public final byte[] operation;

    public DecidedNotification(int instance, UUID operationId, byte[] operation) {
        super(ID);

        this.instance = instance;
        this.operationId = operationId;
        this.operation = operation;
    }

    @Override
    public String toString() {
        return "DecidedNotification [instance=" + instance + ", operationId=" + operationId + ", operation="
                + Arrays.toString(operation) + "]";
    }

}
