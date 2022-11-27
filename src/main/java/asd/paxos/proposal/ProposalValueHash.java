package asd.paxos.proposal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;

public class ProposalValueHash {
    private final byte[] hash;

    private ProposalValueHash(byte[] hash) {
        this.hash = hash;
    }

    public static ProposalValueHash fromData(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return new ProposalValueHash(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(hash);
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
        ProposalValueHash other = (ProposalValueHash) obj;
        if (!Arrays.equals(hash, other.hash))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ProposalValueHash [hash=" + Hex.encodeHexString(this.hash) + "]";
    }
}
