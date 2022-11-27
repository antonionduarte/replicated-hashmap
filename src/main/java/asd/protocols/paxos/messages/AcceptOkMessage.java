package asd.protocols.paxos.messages;

import java.io.IOException;

import asd.protocols.paxos.ProposalNumber;
import asd.protocols.paxos.PaxosProtocol;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class AcceptOkMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID + 1;

    public final int instance;
    public final ProposalNumber messageNumber;

    public AcceptOkMessage(int instance, ProposalNumber messageNumber) {
        super(ID);

        this.instance = instance;
        this.messageNumber = messageNumber;
    }

    public static final ISerializer<AcceptOkMessage> serializer = new ISerializer<AcceptOkMessage>() {
        @Override
        public void serialize(AcceptOkMessage acceptOkMessage, ByteBuf out) throws IOException {
            out.writeInt(acceptOkMessage.instance);
            ProposalNumber.serializer.serialize(acceptOkMessage.messageNumber, out);
        }

        @Override
        public AcceptOkMessage deserialize(ByteBuf in) throws IOException {
            int instance = in.readInt();
            ProposalNumber messageNumber = ProposalNumber.serializer.deserialize(in);
            return new AcceptOkMessage(instance, messageNumber);
        }
    };

    @Override
    public String toString() {
        return "AcceptOkMessage [instance=" + instance + ", messageNumber=" + messageNumber + "]";
    }
}
