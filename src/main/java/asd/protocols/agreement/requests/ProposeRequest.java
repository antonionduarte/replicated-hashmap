package asd.protocols.agreement.requests;

import asd.AsdUtils;
import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class ProposeRequest extends ProtoRequest {

    public static final short ID = PaxosProtocol.ID + 2;

    public final byte[] command;

    public ProposeRequest(byte[] operation) {
        super(ID);

        this.command = operation;
    }

    @Override
    public String toString() {
        return "ProposeRequest [command=" + AsdUtils.sha256Hex(this.command) + "]";
    }
}
