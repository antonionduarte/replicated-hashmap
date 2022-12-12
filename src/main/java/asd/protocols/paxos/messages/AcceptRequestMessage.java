package asd.protocols.paxos.messages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import asd.paxos.ProcessId;
import asd.paxos.Proposal;
import asd.protocols.PaxosBabel;
import asd.protocols.paxos.PaxosProtocol;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class AcceptRequestMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID + 2;

    public final int instance;
    public final List<ProcessId> membership;
    public final Proposal proposal;

    public AcceptRequestMessage(int instance, List<ProcessId> membership, Proposal proposal) {
        super(ID);
        assert membership != null;
        assert membership.stream().allMatch(p -> p != null);
        assert proposal != null;

        this.instance = instance;
        this.membership = membership;
        this.proposal = proposal;
    }

    public static final ISerializer<AcceptRequestMessage> serializer = new ISerializer<AcceptRequestMessage>() {
        @Override
        public void serialize(AcceptRequestMessage acceptRequestMessage, ByteBuf out) throws IOException {
            out.writeInt(acceptRequestMessage.instance);
            out.writeInt(acceptRequestMessage.membership.size());
            for (var p : acceptRequestMessage.membership)
                PaxosBabel.processIdSerializer.serialize(p, out);
            PaxosBabel.proposalSerializer.serialize(acceptRequestMessage.proposal, out);
        }

        @Override
        public AcceptRequestMessage deserialize(ByteBuf in) throws IOException {
            int instance = in.readInt();
            int membershipSize = in.readInt();
            List<ProcessId> membership = new ArrayList<>(membershipSize);
            for (int i = 0; i < membershipSize; i++)
                membership.add(PaxosBabel.processIdSerializer.deserialize(in));
            var proposal = PaxosBabel.proposalSerializer.deserialize(in);
            return new AcceptRequestMessage(instance, membership, proposal);
        }
    };

}
