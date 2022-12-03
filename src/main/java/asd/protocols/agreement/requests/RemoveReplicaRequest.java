package asd.protocols.agreement.requests;

import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

public class RemoveReplicaRequest extends ProtoRequest {
    public static final short ID = PaxosProtocol.ID + 3;

    public final int instance;
    public final Host replica;

    public RemoveReplicaRequest(int instance, Host replica) {
        super(ID);

        this.instance = instance;
        this.replica = replica;
    }

    @Override
    public String toString() {
        return "RemoveReplicaRequest [instance=" + instance + ", replica=" + replica + "]";
    }

}
