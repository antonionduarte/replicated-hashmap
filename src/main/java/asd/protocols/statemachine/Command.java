package asd.protocols.statemachine;

import pt.unl.fct.di.novasys.network.data.Host;

// StateMachine command
public class Command {
    public static enum Kind {
        BATCH, // Batch of operations
        JOIN, // Add a new replica to the membership
        LEAVE, // Remove a replica from the membership
        NOOP, // No operation
    }

    public static record Join(Host host) {
    }

    public static record Leave(Host host) {
    }

    private final Kind kind;
    private final Object command;

    private Command(Kind kind, Object command) {
        this.kind = kind;
        this.command = command;
    }

    public Kind getKind() {
        return kind;
    }

    public Batch getBatch() {
        if (kind != Kind.BATCH)
            throw new IllegalStateException("Command is not a batch");
        return (Batch) command;
    }

    public Join getJoin() {
        if (kind != Kind.JOIN)
            throw new IllegalStateException("Not a join command");
        return (Join) command;
    }

    public Leave getLeave() {
        if (kind != Kind.LEAVE)
            throw new IllegalStateException("Not a leave command");
        return (Leave) command;
    }

    public byte[] toBytes() {
        // TODO
        return new byte[0];
    }

    public static Command batch(Batch batch) {
        return new Command(Kind.BATCH, batch);
    }

    public static Command join(Host join) {
        return new Command(Kind.JOIN, new Join(join));
    }

    public static Command leave(Host leave) {
        return new Command(Kind.LEAVE, new Leave(leave));
    }

    public static Command noop() {
        return new Command(Kind.NOOP, null);
    }

    public static Command fromBytes(byte[] bytes) {
        // TODO
        return null;
    }
}
