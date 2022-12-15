package asd.protocols.agreement.requests;

import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

public class MemberRemoveRequest extends ProtoRequest {
    public static final short ID = Agreement.ID + 3;

    // The slot at which it was decided to remove the replica.
    // The replica will stop participating in the slot after this one.
    public final int slot;

    // The replica to remove
    public final Host replica;

    /**
     * 
     * @param slot
     *            The slot at which it was decided to remove the replica.
     * @param replica
     *            The replica to remove
     */
    public MemberRemoveRequest(int slot, Host replica) {
        super(ID);

        this.slot = slot;
        this.replica = replica;
    }

    @Override
    public String toString() {
        return "RemoveReplicaRequest [slot=" + slot + ", replica=" + replica + "]";
    }

}
