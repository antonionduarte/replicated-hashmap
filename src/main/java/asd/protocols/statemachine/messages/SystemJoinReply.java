package asd.protocols.statemachine.messages;

import asd.protocols.statemachine.StateMachine;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.data.Host;

import java.util.List;

public class SystemJoinReply extends ProtoMessage {

    public static final short MSG_ID = StateMachine.PROTOCOL_ID + 2;

    public final List<Host> membership;
    public final int instance;
    public final byte[] state;

    public SystemJoinReply(List<Host> membership, int instance, byte[] state) {
        super(MSG_ID);
        this.membership = membership;
        this.instance = instance;
        this.state = state;
    }
}
