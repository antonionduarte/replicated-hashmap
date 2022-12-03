package asd.protocols.statemachine.commands;

import pt.unl.fct.di.novasys.network.data.Host;

import java.io.*;

// StateMachine command
public abstract class Command {

    public enum Kind {
        NOOP, // No operation
        BATCH, // Batch of operations
        JOIN, // Add a new replica to the membership
        LEAVE, // Remove a replica from the membership
    }

    private final Kind kind;

    protected Command(Kind kind) {
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    public Batch getBatch() {
        if (kind != Kind.BATCH)
            throw new IllegalStateException("Command is not a batch");
        return (Batch) this;
    }

    public Join getJoin() {
        if (kind != Kind.JOIN)
            throw new IllegalStateException("Not a join command");
        return (Join) this;
    }

    public Leave getLeave() {
        if (kind != Kind.LEAVE)
            throw new IllegalStateException("Not a leave command");
        return (Leave) this;
    }

    public byte[] toBytes() {
        try {
            return createPrefix(this.kind);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Command fromBytes(byte[] bytes) {
        var dis = new DataInputStream(new ByteArrayInputStream(bytes));
        try {
            var kind = fromPrefix(dis.readInt());
            switch (kind) {
                case NOOP -> {
                    return new NoOp();
                }
                case BATCH -> {
                    return new Batch(dis.readAllBytes());
                }
                case JOIN -> {
                    return new Join(dis.readAllBytes());
                }
                case LEAVE -> {
                    return new Leave(dis.readAllBytes());
                }
                default -> throw new IllegalArgumentException("Illegal command kind from bytes");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Command join(Host join) {
        return new Join(join);
    }

    public static Command leave(Host leave) {
        return new Leave(leave);
    }

    public static Command noop() {
        return new NoOp();
    }

    private static byte[] createPrefix(Kind kind) throws IOException {
        var baos = new ByteArrayOutputStream();
        var dos = new DataOutputStream(baos);
        dos.writeInt(kind.ordinal());
        return baos.toByteArray();
    }

    private static Kind fromPrefix(int prefix) {
        if (prefix >= Kind.values().length || prefix < 0)
            throw new IllegalArgumentException("Illegal command kind prefix");
        return Kind.values()[prefix];
    }
}
