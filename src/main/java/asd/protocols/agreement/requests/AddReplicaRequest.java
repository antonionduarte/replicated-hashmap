package asd.protocols.agreement.requests;

import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

public class AddReplicaRequest extends ProtoRequest {
    public static final short ID = PaxosProtocol.ID + 1;

    public final int instance;
    public final Host replica;

    public AddReplicaRequest(int instance, Host replica) {
        super(ID);

        this.instance = instance;
        this.replica = replica;
    }

    @Override
    public String toString() {
        return "AddReplicaRequest [instance=" + instance + ", replica=" + replica + "]";
    }

}
