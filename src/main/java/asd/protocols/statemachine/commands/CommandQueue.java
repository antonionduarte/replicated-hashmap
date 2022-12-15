package asd.protocols.statemachine.commands;

import java.util.PriorityQueue;
import java.util.Queue;

public class CommandQueue {

    private final Queue<OrderedCommand> queue;
    private int lastExecutedInstance;

    public CommandQueue() {
        this.queue = new PriorityQueue<>();
        this.lastExecutedInstance = -1;
    }

    public void insert(int instance, Command command) {
        queue.add(new OrderedCommand(instance, command));
    }

    public boolean hasReadyCommand() {
        return !queue.isEmpty() && queue.peek().instance() == lastExecutedInstance + 1;
    }

    public OrderedCommand popReadyCommand() {
        assert this.hasReadyCommand();
        lastExecutedInstance++;
        return queue.poll();
    }

    public boolean hasMissingInstance() {
        return !queue.isEmpty() && !hasReadyCommand();
    }

    public int firstMissingInstance() {
        return lastExecutedInstance + 1;
    }

    public void setLastExecutedInstance(int instance) {
        this.lastExecutedInstance = instance;
    }

    public int getLastExecutedInstance() {
        return this.lastExecutedInstance;
    }
}
