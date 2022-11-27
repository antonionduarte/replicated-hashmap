package asd.protocols.paxos.messages;

import java.io.IOException;

import asd.paxos.proposal.ProposalValue;
import asd.protocols.paxos.PaxosBabel;
import asd.protocols.paxos.PaxosProtocol;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class DecidedMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID + 5;

    public final int instance;
    public final ProposalValue value;

    public DecidedMessage(int instance, ProposalValue value) {
        super(ID);

        this.instance = instance;
        this.value = value;
    }

    public static final ISerializer<DecidedMessage> serializer = new ISerializer<DecidedMessage>() {
        @Override
        public void serialize(DecidedMessage decidedMessage, ByteBuf out) throws IOException {
            out.writeInt(decidedMessage.instance);
            PaxosBabel.proposalValueSerializer.serialize(decidedMessage.value, out);
        }

        @Override
        public DecidedMessage deserialize(ByteBuf in) throws IOException {
            int instance = in.readInt();
            var value = PaxosBabel.proposalValueSerializer.deserialize(in);
            return new DecidedMessage(instance, value);
        }
    };

    @Override
    public String toString() {
        return "DecidedMessage [instance=" + instance + ", value=" + value + "]";
    }
}
