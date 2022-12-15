package asd.protocols.agreement.requests;

import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

public class MemberAddRequest extends ProtoRequest {
    public static final short ID = PaxosProtocol.ID + 1;

    // The slot at which it was decided to add the replica.
    // The replica will start participating in the slot after this one.
    public final int slot;

    // The replica to add
    public final Host replica;

    /**
     * 
     * @param slot
     *            The slot at which it was decided to add the replica.
     * 
     * @param replica
     *            The replica to add
     */
    public MemberAddRequest(int slot, Host replica) {
        super(ID);

        this.slot = slot;
        this.replica = replica;
    }

    @Override
    public String toString() {
        return "AddReplicaRequest [slot=" + slot + ", replica=" + replica + "]";
    }

}
