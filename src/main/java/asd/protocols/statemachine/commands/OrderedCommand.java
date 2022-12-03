package asd.protocols.statemachine.commands;

public record OrderedCommand(int instance, Command command) implements Comparable<OrderedCommand> {

    @Override
    public int compareTo(OrderedCommand other) {
        return this.instance - other.instance;
    }
}
