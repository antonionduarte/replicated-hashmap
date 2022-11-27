package asd.protocols.app.requests;

import asd.protocols.app.HashApp;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class PutRequest extends ProtoRequest {
    public static final short ID = HashApp.PROTO_ID + 51;
    public final long operationId;
    public final String key;
    public final byte[] value;

    public PutRequest(long operationId, String key, byte[] value) {
        super(ID);
        this.operationId = operationId;
        this.key = key;
        this.value = value;
    }
}
