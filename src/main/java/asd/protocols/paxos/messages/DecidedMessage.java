package asd.protocols.paxos.messages;

import asd.AsdUtils;
import asd.protocols.paxos.PaxosProtocol;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class DecidedMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID + 5;

    public final int instance;
    public final byte[] value;

    public DecidedMessage(int instance, byte[] value) {
        super(ID);

        this.instance = instance;
        this.value = value;
    }

    public static final ISerializer<DecidedMessage> serializer = new ISerializer<DecidedMessage>() {
        @Override
        public void serialize(DecidedMessage decidedMessage, ByteBuf out) {
            out.writeInt(decidedMessage.instance);
            out.writeBytes(decidedMessage.value);
        }

        @Override
        public DecidedMessage deserialize(ByteBuf in) {
            int instance = in.readInt();
            byte[] value = new byte[in.readableBytes()];
            in.readBytes(value);
            return new DecidedMessage(instance, value);
        }
    };

    @Override
    public String toString() {
        return "DecidedMessage [instance=" + instance + ", value=" + AsdUtils.sha256Hex(value) + "]";
    }
}
