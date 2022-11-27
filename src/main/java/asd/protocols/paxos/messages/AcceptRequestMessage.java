package asd.protocols.paxos.messages;

import java.io.IOException;

import asd.protocols.paxos.PaxosBabel;
import asd.protocols.paxos.PaxosProtocol;
import asd.paxos.proposal.Proposal;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class AcceptRequestMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID + 2;

    public final int instance;
    public final Proposal proposal;

    public AcceptRequestMessage(int instance, Proposal proposal) {
        super(ID);

        this.instance = instance;
        this.proposal = proposal;
    }

    public static final ISerializer<AcceptRequestMessage> serializer = new ISerializer<AcceptRequestMessage>() {
        @Override
        public void serialize(AcceptRequestMessage acceptRequestMessage, ByteBuf out) throws IOException {
            out.writeInt(acceptRequestMessage.instance);
            PaxosBabel.proposalSerializer.serialize(acceptRequestMessage.proposal, out);
        }

        @Override
        public AcceptRequestMessage deserialize(ByteBuf in) throws IOException {
            int instance = in.readInt();
            Proposal proposal = PaxosBabel.proposalSerializer.deserialize(in);
            return new AcceptRequestMessage(instance, proposal);
        }
    };

    @Override
    public String toString() {
        return "AcceptRequestMessage [instance=" + instance + ", proposal=" + proposal + "]";
    }
}
