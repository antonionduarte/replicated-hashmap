package asd.protocols.statemachine.messages;

import asd.protocols.statemachine.StateMachine;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class SystemGc extends ProtoMessage {
    public static final short ID = StateMachine.ID + 4;

    public final int latestExecutedSlot;

    public SystemGc(int latestExecutedSlot) {
        super(ID);

        this.latestExecutedSlot = latestExecutedSlot;
    }

    public static ISerializer<SystemGc> serializer = new ISerializer<SystemGc>() {
        @Override
        public void serialize(SystemGc systemGc, ByteBuf out) {
            out.writeInt(systemGc.latestExecutedSlot);
        }

        @Override
        public SystemGc deserialize(ByteBuf in) {
            return new SystemGc(in.readInt());
        }
    };
}
