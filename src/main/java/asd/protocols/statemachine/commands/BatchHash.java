package asd.protocols.statemachine.commands;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;

import com.google.common.primitives.Longs;

import asd.protocols.statemachine.Operation;

public class BatchHash {
    private final byte[] digest;

    BatchHash(Operation[] operations) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (var operation : operations) {
                digest.update(Longs.toByteArray(operation.operationId.getMostSignificantBits()));
                digest.update(Longs.toByteArray(operation.operationId.getLeastSignificantBits()));
            }
            this.digest = digest.digest();
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
        BatchHash other = (BatchHash) obj;
        if (!Arrays.equals(digest, other.digest))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return Hex.encodeHexString(this.digest);
    }
}
