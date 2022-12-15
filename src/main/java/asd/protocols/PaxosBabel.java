package asd.protocols;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import asd.paxos.Ballot;
import asd.paxos.ProcessId;
import asd.paxos.ProposalValue;
import asd.paxos.Proposal;
import asd.paxos.ProposalSlot;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

public class PaxosBabel {
    public static ProcessId hostToProcessId(Host host) {
        var addr = Ints.fromByteArray(host.getAddress().getAddress());
        var port = host.getPort();
        return new ProcessId((((long) addr) << 16) | port);
    }

    public static Host hostFromProcessId(ProcessId processId) {
        try {
            var addr = (int) (processId.getId() >> 16);
            var port = (int) (processId.getId() & 0xFFFF);
            return new Host(InetAddress.getByAddress(Ints.toByteArray(addr)), port);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] operationToBytes(UUID operationId, byte[] operation) {
        var buffer = new byte[16 + operation.length];
        System.arraycopy(Longs.toByteArray(operationId.getMostSignificantBits()), 0, buffer, 0, 8);
        System.arraycopy(Longs.toByteArray(operationId.getLeastSignificantBits()), 0, buffer, 8, 8);
        System.arraycopy(operation, 0, buffer, 16, operation.length);
        return buffer;
    }

    public static Pair<UUID, byte[]> operationFromBytes(byte[] buffer) {
        var msl = Longs.fromBytes(buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5], buffer[6],
                buffer[7]);
        var lsl = Longs.fromBytes(buffer[8], buffer[9], buffer[10], buffer[11], buffer[12], buffer[13], buffer[14],
                buffer[15]);
        var operationId = new UUID(msl, lsl);
        var operation = new byte[buffer.length - 16];
        System.arraycopy(buffer, 16, operation, 0, operation.length);
        return Pair.of(operationId, operation);
    }

    public static final ISerializer<Ballot> ballotSerializer = new ISerializer<Ballot>() {
        @Override
        public void serialize(Ballot ballot, ByteBuf out) throws IOException {
            processIdSerializer.serialize(ballot.processId, out);
            out.writeLong(ballot.sequenceNumber);
        }

        @Override
        public Ballot deserialize(ByteBuf in) throws IOException {
            var processId = processIdSerializer.deserialize(in);
            var number = in.readLong();
            return new Ballot(processId, number);
        }
    };

    public static final ISerializer<ProposalSlot> proposalSlotSerializer = new ISerializer<ProposalSlot>() {
        @Override
        public void serialize(ProposalSlot msg, ByteBuf buf) throws IOException {
            buf.writeInt(msg.slot);
            proposalSerializer.serialize(msg.proposal, buf);
        }

        @Override
        public ProposalSlot deserialize(ByteBuf buf) throws IOException {
            var slot = buf.readInt();
            var proposal = proposalSerializer.deserialize(buf);
            return new ProposalSlot(slot, proposal);
        }
    };

    public static final ISerializer<Proposal> proposalSerializer = new ISerializer<Proposal>() {
        @Override
        public void serialize(Proposal msg, ByteBuf buf) throws IOException {
            ballotSerializer.serialize(msg.ballot, buf);
            proposalValueSerializer.serialize(msg.value, buf);
        }

        @Override
        public Proposal deserialize(ByteBuf buf) throws IOException {
            var ballot = ballotSerializer.deserialize(buf);
            var value = proposalValueSerializer.deserialize(buf);
            return new Proposal(ballot, value);
        }
    };

    public static final ISerializer<ProposalValue> proposalValueSerializer = new ISerializer<ProposalValue>() {
        @Override
        public void serialize(ProposalValue msg, ByteBuf buf) throws IOException {
            buf.writeInt(msg.data.length);
            buf.writeBytes(msg.data);
        }

        @Override
        public ProposalValue deserialize(ByteBuf buf) throws IOException {
            int length = buf.readInt();
            byte[] data = new byte[length];
            buf.readBytes(data);
            return new ProposalValue(data);
        }
    };

    public static final ISerializer<ProcessId> processIdSerializer = new ISerializer<ProcessId>() {
        @Override
        public void serialize(ProcessId processId, ByteBuf out) throws IOException {
            out.writeLong(processId.getId());
        }

        @Override
        public ProcessId deserialize(ByteBuf in) throws IOException {
            return new ProcessId(in.readLong());
        }
    };
}
