package asd.protocols.statemachine.commands;

import asd.protocols.statemachine.Operation;

import java.io.*;
import java.util.Arrays;
import java.util.UUID;

public class Batch extends Command {

    public final Operation[] operations;
    public final BatchHash hash;

    protected Batch(Operation[] operations) {
        super(Kind.BATCH);
        this.operations = operations;
        this.hash = new BatchHash(operations);
    }

    protected Batch(byte[] bytes) throws IOException {
        super(Kind.BATCH);

        var dis = new DataInputStream(new ByteArrayInputStream(bytes));
        var numOps = dis.readInt();
        this.operations = new Operation[numOps];
        for (int i = 0; i < numOps; i++) {
            var mostSignificant = dis.readLong();
            var leastSignificant = dis.readLong();
            var uuid = new UUID(mostSignificant, leastSignificant);
            var operationSize = dis.readInt();
            var operationData = new byte[operationSize];
            dis.read(operationData);
            this.operations[i] = new Operation(uuid, operationData);
        }

        this.hash = new BatchHash(this.operations);
    }

    @Override
    public byte[] toBytes() {
        var baos = new ByteArrayOutputStream();
        var dos = new DataOutputStream(baos);
        try {
            dos.write(super.toBytes());
            dos.writeInt(operations.length);
            for (var op : operations) {
                dos.writeLong(op.operationId.getMostSignificantBits());
                dos.writeLong(op.operationId.getLeastSignificantBits());
                dos.writeInt(op.operation.length);
                dos.write(op.operation);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return baos.toByteArray();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(operations);
        result = prime * result + ((hash == null) ? 0 : hash.hashCode());
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
        Batch other = (Batch) obj;
        return this.hash.equals(other.hash);
    }
}
