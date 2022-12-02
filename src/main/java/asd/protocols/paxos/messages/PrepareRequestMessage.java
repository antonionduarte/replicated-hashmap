package asd.protocols.paxos.messages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import asd.paxos2.Ballot;
import asd.paxos2.ProcessId;
import asd.protocols.paxos.PaxosBabel;
import asd.protocols.paxos.PaxosProtocol;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class PrepareRequestMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID + 4;

    public final int instance;
    public final List<ProcessId> membership;
    public final Ballot ballot;

    public PrepareRequestMessage(int instance, List<ProcessId> membership, Ballot ballot) {
        super(ID);

        this.instance = instance;
        this.membership = membership;
        this.ballot = ballot;
    }

    public static final ISerializer<PrepareRequestMessage> serializer = new ISerializer<PrepareRequestMessage>() {
        @Override
        public void serialize(PrepareRequestMessage prepareRequestMessage, ByteBuf out) throws IOException {
            out.writeInt(prepareRequestMessage.instance);
            out.writeInt(prepareRequestMessage.membership.size());
            for (var p : prepareRequestMessage.membership)
                PaxosBabel.processIdSerializer.serialize(p, out);
            PaxosBabel.ballotSerializer.serialize(prepareRequestMessage.ballot, out);
        }

        @Override
        public PrepareRequestMessage deserialize(ByteBuf in) throws IOException {
            int instance = in.readInt();
            int membershipSize = in.readInt();
            List<ProcessId> membership = new ArrayList<>(membershipSize);
            for (int i = 0; i < membershipSize; i++)
                membership.add(PaxosBabel.processIdSerializer.deserialize(in));
            Ballot ballot = PaxosBabel.ballotSerializer.deserialize(in);
            return new PrepareRequestMessage(instance, membership, ballot);
        }
    };

}
