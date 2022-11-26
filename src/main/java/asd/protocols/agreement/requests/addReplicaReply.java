package asd.protocols.agreement.requests;

import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

public class addReplicaReply extends ProtoReply {

    public static short REPLY_ID = Agreement.PROTOCOL_ID + 1;

    public addReplicaReply() {
        super(REPLY_ID);
    }
}
