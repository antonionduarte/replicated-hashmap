package asd.protocols.statemachine;

import java.util.ArrayList;
import java.util.List;

public class BatchBuilder {
    private final List<Operation> operations;

    public BatchBuilder() {
        this.operations = new ArrayList<>();
    }

    public void append(Operation operation) {
        this.operations.add(operation);
    }

    public Batch build() {
        var operations = new Operation[this.operations.size()];
        this.operations.toArray(operations);
        this.operations.clear();
        return new Batch(operations);
    }

    public int size() {
        return this.operations.size();
    }
}
