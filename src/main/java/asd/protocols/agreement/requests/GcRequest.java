package asd.protocols.agreement.requests;

import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class GcRequest extends ProtoRequest {

    public static final short ID = Agreement.ID + 5;

    // The slot up to, inclusive, which the garbage collector should run
    public final int upToSlot;

    public GcRequest(int upToSlot) {
        super(ID);

        this.upToSlot = upToSlot;
    }

}
