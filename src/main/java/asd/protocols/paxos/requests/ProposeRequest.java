package asd.protocols.paxos.requests;

import asd.AsdUtils;
import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class ProposeRequest extends ProtoRequest {

    public static final short ID = PaxosProtocol.ID + 2;

    public final byte[] operation;

    public ProposeRequest(byte[] operation) {
        super(ID);

        this.operation = operation;
    }

    @Override
    public String toString() {
        return "ProposeRequest [operation=" + AsdUtils.sha256Hex(this.operation) + "]";
    }
}
