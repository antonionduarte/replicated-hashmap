package asd.paxos.proposal;

import java.io.IOException;
import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.ISerializer;

public class ProposalValue {
    private final byte[] data;
    private final ProposalValueHash hash;

    public ProposalValue(byte[] data) {
        this.data = data;
        this.hash = ProposalValueHash.fromData(data);
    }

    public static final ISerializer<ProposalValue> serializer = new ISerializer<ProposalValue>() {
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(data);
        result = prime * result + ((hash == null) ? 0 : hash.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ProposalValue))
            return false;
        return this.hash.equals(((ProposalValue) obj).hash);
    }

    @Override
    public String toString() {
        return "ProposalValue [hash=" + hash + "]";
    }
}
