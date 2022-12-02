package asd.paxos2;

import java.util.Arrays;

public class ProposalValue {
    public final byte[] data;
    public final ProposalValueHash hash;

    public ProposalValue(byte[] data) {
        this.data = data;
        this.hash = ProposalValueHash.fromData(data);
    }

    public ProposalValue(String data) {
        this(data.getBytes());
    }

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
