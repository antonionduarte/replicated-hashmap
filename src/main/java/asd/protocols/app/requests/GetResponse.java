package asd.protocols.app.requests;

import asd.protocols.app.HashApp;
import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

public class GetResponse extends ProtoReply {
    public static final short ID = HashApp.PROTO_ID + 50;

    public final long operationId;
    public final byte[] value;

    public GetResponse(long operationId, byte[] value) {
        super(ID);
        this.operationId = operationId;
        this.value = value;
    }
}
