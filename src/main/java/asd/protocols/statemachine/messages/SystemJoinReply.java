package asd.protocols.statemachine.messages;

import asd.protocols.statemachine.StateMachine;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SystemJoinReply extends ProtoMessage {

    public static final short ID = StateMachine.ID + 2;

    public final List<Host> membership;
    public final int instance;
    public final byte[] state;

    public SystemJoinReply(List<Host> membership, int instance, byte[] state) {
        super(ID);
        this.membership = membership;
        this.instance = instance;
        this.state = state;
    }

    public static final ISerializer<SystemJoinReply> serializer = new ISerializer<>() {
        @Override
        public void serialize(SystemJoinReply systemJoinReply, ByteBuf byteBuf) throws IOException {
            byteBuf.writeInt(systemJoinReply.membership.size());
            for (Host host : systemJoinReply.membership) {
                Host.serializer.serialize(host, byteBuf);
            }
            byteBuf.writeInt(systemJoinReply.instance);
            byteBuf.writeInt(systemJoinReply.state.length);
            byteBuf.writeBytes(systemJoinReply.state);
        }

        @Override
        public SystemJoinReply deserialize(ByteBuf byteBuf) throws IOException {
            int size = byteBuf.readInt();
            List<Host> membership = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                membership.add(Host.serializer.deserialize(byteBuf));
            }
            int instance = byteBuf.readInt();
            int stateSize = byteBuf.readInt();
            byte[] state = new byte[stateSize];
            byteBuf.readBytes(state);
            return new SystemJoinReply(membership, instance, state);
        }
    };
}
