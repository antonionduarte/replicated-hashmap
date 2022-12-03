package asd.protocols.paxos.messages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import asd.paxos.ProcessId;
import asd.paxos.ProposalValue;
import asd.protocols.PaxosBabel;
import asd.protocols.paxos.PaxosProtocol;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class DecidedMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID + 5;

    public final int instance;
    public final List<ProcessId> membership;
    public final ProposalValue value;

    public DecidedMessage(int instance, List<ProcessId> membership, ProposalValue value) {
        super(ID);

        this.instance = instance;
        this.membership = membership;
        this.value = value;
    }

    public static final ISerializer<DecidedMessage> serializer = new ISerializer<DecidedMessage>() {
        @Override
        public void serialize(DecidedMessage decidedMessage, ByteBuf out) throws IOException {
            out.writeInt(decidedMessage.instance);
            out.writeInt(decidedMessage.membership.size());
            for (var pid : decidedMessage.membership)
                PaxosBabel.processIdSerializer.serialize(pid, out);
            PaxosBabel.proposalValueSerializer.serialize(decidedMessage.value, out);
        }

        @Override
        public DecidedMessage deserialize(ByteBuf in) throws IOException {
            int instance = in.readInt();
            int membershipSize = in.readInt();
            List<ProcessId> membership = new ArrayList<>(membershipSize);
            for (int i = 0; i < membershipSize; i++)
                membership.add(PaxosBabel.processIdSerializer.deserialize(in));
            ProposalValue value = PaxosBabel.proposalValueSerializer.deserialize(in);
            return new DecidedMessage(instance, membership, value);
        }
    };

    @Override
    public String toString() {
        return "DecidedMessage [instance=" + instance + ", value=" + value + "]";
    }
}
