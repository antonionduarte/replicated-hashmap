package asd.protocols.paxos.messages;

import java.io.IOException;
import java.util.Optional;

import asd.paxos2.Ballot;
import asd.paxos2.single.Proposal;
import asd.protocols.paxos.PaxosBabel;
import asd.protocols.paxos.PaxosProtocol;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class PrepareOkMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID;

    public final int instance;
    public final Ballot ballot;
    public final Optional<Proposal> acceptedProposal;
    public final boolean decided;

    public PrepareOkMessage(int instance, Ballot proposalNumber) {
        this(instance, proposalNumber, Optional.empty(), false);
    }

    public PrepareOkMessage(int instance, Ballot proposalNumber, Proposal acceptedProposal, boolean decided) {
        this(instance, proposalNumber, Optional.of(acceptedProposal), decided);
    }

    public PrepareOkMessage(int instance, Ballot ballot, Optional<Proposal> acceptedProposal, boolean decided) {
        super(ID);

        assert acceptedProposal.isPresent() || !decided;

        this.instance = instance;
        this.ballot = ballot;
        this.acceptedProposal = acceptedProposal;
        this.decided = decided;
    }

    public static final ISerializer<PrepareOkMessage> serializer = new ISerializer<PrepareOkMessage>() {
        @Override
        public void serialize(PrepareOkMessage msg, ByteBuf buf) throws IOException {
            buf.writeInt(msg.instance);
            PaxosBabel.ballotSerializer.serialize(msg.ballot, buf);
            buf.writeBoolean(msg.acceptedProposal.isPresent());
            if (msg.acceptedProposal.isPresent()) {
                PaxosBabel.proposalSerializer.serialize(msg.acceptedProposal.get(), buf);
            }
            buf.writeBoolean(msg.decided);
        }

        @Override
        public PrepareOkMessage deserialize(ByteBuf buf) throws IOException {
            int instance = buf.readInt();
            Ballot ballot = PaxosBabel.ballotSerializer.deserialize(buf);
            Optional<Proposal> acceptedProposal = Optional.empty();
            if (buf.readBoolean()) {
                acceptedProposal = Optional.of(PaxosBabel.proposalSerializer.deserialize(buf));
            }
            boolean decided = buf.readBoolean();
            return new PrepareOkMessage(instance, ballot, acceptedProposal, decided);
        }
    };

}
