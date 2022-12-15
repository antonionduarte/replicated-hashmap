package asd.protocols.agreement.requests;

import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class MembershipUnchangedRequest extends ProtoRequest {

    public static final short ID = Agreement.ID + 4;

    // The slot for which the membership is unchanged
    public final int slot;

    public MembershipUnchangedRequest(int slot) {
        super(ID);

        this.slot = slot;
    }

    @Override
    public String toString() {
        return "MembershipUnchangedRequest [slot=" + slot + "]";
    }
}
