package asd.protocols.paxos.messages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import asd.paxos2.ProposalValue;
import asd.paxos2.Ballot;
import asd.paxos2.ProcessId;
import asd.protocols.paxos.PaxosBabel;
import asd.protocols.paxos.PaxosProtocol;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class AcceptRequestMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID + 2;

    public final int instance;
    public final List<ProcessId> membership;
    public final Ballot ballot;
    public final ProposalValue value;

    public AcceptRequestMessage(int instance, List<ProcessId> membership, Ballot ballot, ProposalValue value) {
        super(ID);

        this.instance = instance;
        this.membership = membership;
        this.ballot = ballot;
        this.value = value;
    }

    public static final ISerializer<AcceptRequestMessage> serializer = new ISerializer<AcceptRequestMessage>() {
        @Override
        public void serialize(AcceptRequestMessage acceptRequestMessage, ByteBuf out) throws IOException {
            out.writeInt(acceptRequestMessage.instance);
            out.writeInt(acceptRequestMessage.membership.size());
            for (var p : acceptRequestMessage.membership)
                PaxosBabel.processIdSerializer.serialize(p, out);
            PaxosBabel.ballotSerializer.serialize(acceptRequestMessage.ballot, out);
            PaxosBabel.proposalValueSerializer.serialize(acceptRequestMessage.value, out);
        }

        @Override
        public AcceptRequestMessage deserialize(ByteBuf in) throws IOException {
            int instance = in.readInt();
            int membershipSize = in.readInt();
            List<ProcessId> membership = new ArrayList<>(membershipSize);
            for (int i = 0; i < membershipSize; i++)
                membership.add(PaxosBabel.processIdSerializer.deserialize(in));
            Ballot ballot = PaxosBabel.ballotSerializer.deserialize(in);
            ProposalValue value = PaxosBabel.proposalValueSerializer.deserialize(in);
            return new AcceptRequestMessage(instance, membership, ballot, value);
        }
    };

}
