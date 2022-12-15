package asd.protocols.paxos.messages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import asd.paxos.Ballot;
import asd.paxos.ProcessId;
import asd.protocols.PaxosBabel;
import asd.protocols.paxos.PaxosProtocol;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class PrepareRequestMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID + 4;

    public final int slot;
    public final List<ProcessId> membership;
    public final Ballot ballot;

    public PrepareRequestMessage(int slot, List<ProcessId> membership, Ballot ballot) {
        super(ID);

        this.slot = slot;
        this.membership = membership;
        this.ballot = ballot;
    }

    public static final ISerializer<PrepareRequestMessage> serializer = new ISerializer<PrepareRequestMessage>() {
        @Override
        public void serialize(PrepareRequestMessage prepareRequestMessage, ByteBuf out) throws IOException {
            out.writeInt(prepareRequestMessage.slot);
            out.writeInt(prepareRequestMessage.membership.size());
            for (var p : prepareRequestMessage.membership)
                PaxosBabel.processIdSerializer.serialize(p, out);
            PaxosBabel.ballotSerializer.serialize(prepareRequestMessage.ballot, out);
        }

        @Override
        public PrepareRequestMessage deserialize(ByteBuf in) throws IOException {
            int slot = in.readInt();
            int membershipSize = in.readInt();
            List<ProcessId> membership = new ArrayList<>(membershipSize);
            for (int i = 0; i < membershipSize; i++)
                membership.add(PaxosBabel.processIdSerializer.deserialize(in));
            Ballot ballot = PaxosBabel.ballotSerializer.deserialize(in);
            return new PrepareRequestMessage(slot, membership, ballot);
        }
    };

}
