package asd.protocols.agreement.requests;

import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

public class RemoveReplicaRequest extends ProtoRequest {
    public static final short ID = PaxosProtocol.ID + 3;

    // The instance at which it was decided to remove the replica.
    // The replica will stop participating in the instance after this one.
    public final int instance;

    // The replica to remove
    public final Host replica;

    /**
     * 
     * @param instance
     *            The instance at which it was decided to remove the replica.
     * @param replica
     *            The replica to remove
     */
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
