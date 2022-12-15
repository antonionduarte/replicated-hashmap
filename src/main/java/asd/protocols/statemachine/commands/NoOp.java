package asd.protocols.statemachine.commands;

public class NoOp extends Command {
    protected NoOp() {
        super(Kind.NOOP);
    }

    @Override
    public boolean equals(Object arg0) {
        return (arg0 instanceof NoOp);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
