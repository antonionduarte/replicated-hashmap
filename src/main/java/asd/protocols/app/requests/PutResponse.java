package asd.protocols.app.requests;

import asd.protocols.app.HashApp;
import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

public class PutResponse extends ProtoReply {
    public static final short ID = HashApp.PROTO_ID + 52;
    public final long operationId;

    public PutResponse(long operationId) {
        super(ID);

        this.operationId = operationId;
    }
}
