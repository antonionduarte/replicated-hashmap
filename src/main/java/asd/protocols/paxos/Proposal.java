package asd.protocols.paxos;

import java.io.IOException;
import java.util.Arrays;

import asd.AsdUtils;
import asd.protocols.paxos.messages.PrepareOkMessage;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.ISerializer;

public class Proposal {
    public final ProposalNumber number;
    public final byte[] value;

    public Proposal(ProposalNumber messageNumber, byte[] value) {
        this.number = messageNumber;
        this.value = value;
    }

    public static final ISerializer<Proposal> serializer = new ISerializer<Proposal>() {
        @Override
        public void serialize(Proposal msg, ByteBuf buf) throws IOException {
            ProposalNumber.serializer.serialize(msg.number, buf);
            buf.writeInt(msg.value.length);
            buf.writeBytes(msg.value);
        }

        @Override
        public Proposal deserialize(ByteBuf buf) throws IOException {
            ProposalNumber messageNumber = ProposalNumber.serializer.deserialize(buf);
            int valueLength = buf.readInt();
            byte[] value = new byte[valueLength];
            buf.readBytes(value);
            return new Proposal(messageNumber, value);
        }
    };

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((number == null) ? 0 : number.hashCode());
        result = prime * result + Arrays.hashCode(value);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Proposal other = (Proposal) obj;
        if (number == null) {
            if (other.number != null)
                return false;
        } else if (!number.equals(other.number))
            return false;
        if (!Arrays.equals(value, other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Proposal [messageNumber=" + number + ", value=" + AsdUtils.sha256Hex(value) + "]";
    }

}
