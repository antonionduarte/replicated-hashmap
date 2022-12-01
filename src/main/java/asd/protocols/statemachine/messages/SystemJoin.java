package asd.protocols.statemachine.messages;

import asd.protocols.statemachine.StateMachine;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;

public class SystemJoin extends ProtoMessage {

    public static final short MSG_ID = StateMachine.PROTOCOL_ID + 1;

    public SystemJoin() {
        super(MSG_ID);
    }

    public static final ISerializer<SystemJoin> serializer = new ISerializer<>() {
        @Override
        public void serialize(SystemJoin systemJoin, ByteBuf byteBuf) throws IOException {
        }

        @Override
        public SystemJoin deserialize(ByteBuf byteBuf) throws IOException {
            return new SystemJoin();
        }
    };
}
