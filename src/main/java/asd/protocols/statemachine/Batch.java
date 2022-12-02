package asd.protocols.statemachine;

public class Batch {
    public final Operation[] operations;

    public Batch(Operation[] operations) {
        this.operations = operations;
    }
}
