package asd.protocols.agreement.requests;

import asd.AsdUtils;
import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class ProposeRequest extends ProtoRequest {

    public static final short ID = Agreement.ID + 2;

    public final byte[] command;
    public final boolean takeLeadership;

    public ProposeRequest(byte[] operation, boolean takeLeadership) {
        super(ID);

        this.command = operation;
        this.takeLeadership = takeLeadership;
    }

    @Override
    public String toString() {
        return "ProposeRequest [command=" + AsdUtils.sha256Hex(this.command)
                + ", takeLeadership=" + takeLeadership + "]";
    }
}
