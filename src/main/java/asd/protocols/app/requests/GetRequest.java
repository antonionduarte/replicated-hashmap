package asd.protocols.app.requests;

import asd.protocols.app.HashApp;
import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class GetRequest extends ProtoRequest {
    public static final short ID = HashApp.PROTO_ID + 50;

    public final long operationId;
    public final String key;

    public GetRequest(long operationId, String key) {
        super(ID);
        this.operationId = operationId;
        this.key = key;
    }
}
