package asd.paxos;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;

public class ProposalValueHash {
    public final byte[] digest;

    private ProposalValueHash(byte[] digest) {
        this.digest = digest;
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
        result = prime * result + Arrays.hashCode(digest);
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
        if (!Arrays.equals(digest, other.digest))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ProposalValueHash [hash=" + Hex.encodeHexString(this.digest) + "]";
    }
}
