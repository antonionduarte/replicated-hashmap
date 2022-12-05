package asd.protocols.agreement.requests;

import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

public class AddReplicaRequest extends ProtoRequest {
    public static final short ID = PaxosProtocol.ID + 1;

    // The instance at which it was decided to add the replica.
    // The replica will start participating in the instance after this one.
    public final int instance;

    // The replica to add
    public final Host replica;

    /**
     * 
     * @param instance
     *            The instance at which it was decided to add the replica.
     * 
     * @param replica
     *            The replica to add
     */
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
