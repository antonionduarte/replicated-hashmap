package asd.protocols.paxos.messages;

import java.io.IOException;

import asd.protocols.paxos.ProposalNumber;
import asd.protocols.paxos.PaxosProtocol;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class PrepareRequestMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID + 4;

    public final int instance;
    public final ProposalNumber proposalNumber;

    public PrepareRequestMessage(int instance, ProposalNumber messageNumber) {
        super(ID);

        this.instance = instance;
        this.proposalNumber = messageNumber;
    }

    public static final ISerializer<PrepareRequestMessage> serializer = new ISerializer<PrepareRequestMessage>() {
        @Override
        public void serialize(PrepareRequestMessage prepareRequestMessage, ByteBuf out) throws IOException {
            out.writeInt(prepareRequestMessage.instance);
            ProposalNumber.serializer.serialize(prepareRequestMessage.proposalNumber, out);
        }

        @Override
        public PrepareRequestMessage deserialize(ByteBuf in) throws IOException {
            int instance = in.readInt();
            ProposalNumber messageNumber = ProposalNumber.serializer.deserialize(in);
            return new PrepareRequestMessage(instance, messageNumber);
        }
    };

    @Override
    public String toString() {
        return "PrepareRequestMessage [instance=" + instance + ", messageNumber=" + proposalNumber + "]";
    }

}
