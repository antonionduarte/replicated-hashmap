package asd.protocols.paxos.messages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import asd.paxos.Ballot;
import asd.paxos.ProposalSlot;
import asd.protocols.PaxosBabel;
import asd.protocols.paxos.PaxosProtocol;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class PrepareOkMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID;

    public final int slot;
    public final Ballot ballot;
    public final List<ProposalSlot> accepted;

    public PrepareOkMessage(int slot, Ballot proposalNumber) {
        this(slot, proposalNumber, List.of());
    }

    public PrepareOkMessage(int slot, Ballot proposalNumber, ProposalSlot accepted) {
        this(slot, proposalNumber, List.of(accepted));
    }

    public PrepareOkMessage(int slot, Ballot ballot, List<ProposalSlot> accepted) {
        super(ID);

        this.slot = slot;
        this.ballot = ballot;
        this.accepted = Collections.unmodifiableList(accepted);
    }

    public static final ISerializer<PrepareOkMessage> serializer = new ISerializer<PrepareOkMessage>() {
        @Override
        public void serialize(PrepareOkMessage msg, ByteBuf buf) throws IOException {
            buf.writeInt(msg.slot);
            PaxosBabel.ballotSerializer.serialize(msg.ballot, buf);
            buf.writeInt(msg.accepted.size());
            for (var p : msg.accepted)
                PaxosBabel.proposalSlotSerializer.serialize(p, buf);
        }

        @Override
        public PrepareOkMessage deserialize(ByteBuf buf) throws IOException {
            int instance = buf.readInt();
            Ballot ballot = PaxosBabel.ballotSerializer.deserialize(buf);
            int acceptedSize = buf.readInt();
            List<ProposalSlot> accepted = new ArrayList<>(acceptedSize);
            for (int i = 0; i < acceptedSize; i++)
                accepted.add(PaxosBabel.proposalSlotSerializer.deserialize(buf));
            return new PrepareOkMessage(instance, ballot, Collections.unmodifiableList(accepted));
        }
    };

}
