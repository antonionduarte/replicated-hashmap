package asd.protocols.statemachine;

public class CommandQueue {

    public static record OrderedCommand(int instance, Command command) {
    }

    public CommandQueue() {
    }

    public void insert(int instance, Command command) {
        // TODO
    }

    public boolean hasReadyCommand() {
        // TODO
        return false;
    }

    public OrderedCommand popReadyCommand() {
        // TODO
        return null;
    }

    public boolean hasMissingInstance() {
        // TODO
        return false;
    }

    public int firstMissingInstance() {
        return 0;
    }

    public void setLastExecutedInstance(int instance) {
        // TODO
    }
}
