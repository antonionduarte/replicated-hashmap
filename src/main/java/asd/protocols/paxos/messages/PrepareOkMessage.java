package asd.protocols.paxos.messages;

import java.io.IOException;
import java.util.Optional;

import asd.protocols.paxos.PaxosBabel;
import asd.protocols.paxos.PaxosProtocol;
import asd.paxos.proposal.Proposal;
import asd.paxos.proposal.ProposalNumber;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class PrepareOkMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID;

    public final int instance;
    public final ProposalNumber proposalNumber;
    public final Optional<Proposal> acceptedProposal;

    public PrepareOkMessage(int instance, ProposalNumber proposalNumber) {
        this(instance, proposalNumber, Optional.empty());
    }

    public PrepareOkMessage(int instance, ProposalNumber proposalNumber, Proposal acceptedProposal) {
        this(instance, proposalNumber, Optional.of(acceptedProposal));
    }

    public PrepareOkMessage(int instance, ProposalNumber proposalNumber, Optional<Proposal> acceptedProposal) {
        super(ID);

        this.instance = instance;
        this.proposalNumber = proposalNumber;
        this.acceptedProposal = acceptedProposal;
    }

    public static final ISerializer<PrepareOkMessage> serializer = new ISerializer<PrepareOkMessage>() {
        @Override
        public void serialize(PrepareOkMessage msg, ByteBuf buf) throws IOException {
            buf.writeInt(msg.instance);
            PaxosBabel.proposalNumberSerializer.serialize(msg.proposalNumber, buf);
            buf.writeBoolean(msg.acceptedProposal.isPresent());
            if (msg.acceptedProposal.isPresent()) {
                PaxosBabel.proposalSerializer.serialize(msg.acceptedProposal.get(), buf);
            }
        }

        @Override
        public PrepareOkMessage deserialize(ByteBuf buf) throws IOException {
            int instance = buf.readInt();
            ProposalNumber proposalNumber = PaxosBabel.proposalNumberSerializer.deserialize(buf);
            boolean hasAcceptedProposal = buf.readBoolean();
            Optional<Proposal> acceptedProposal = Optional.empty();
            if (hasAcceptedProposal) {
                acceptedProposal = Optional.of(PaxosBabel.proposalSerializer.deserialize(buf));
            }
            return new PrepareOkMessage(instance, proposalNumber, acceptedProposal);
        }
    };

    @Override
    public String toString() {
        return "PrepareOkMessage [instance=" + instance + ", proposalNumber=" + proposalNumber + ", acceptedProposal="
                + acceptedProposal + "]";
    }
}
