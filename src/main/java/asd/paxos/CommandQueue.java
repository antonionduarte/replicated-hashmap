package asd.paxos;

import java.util.ArrayDeque;
import java.util.Queue;

public class CommandQueue {
    private final Queue<PaxosCmd> queue;

    public CommandQueue() {
        this.queue = new ArrayDeque<>();
    }

    public void push(PaxosCmd command) {
        this.queue.add(command);
    }

    public void push(PaxosCmd... commands) {
        for (var command : commands)
            this.queue.add(command);
    }

    public PaxosCmd pop() {
        assert !this.isEmpty();
        return this.queue.remove();
    }

    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    public void transferTo(CommandQueue target) {
        while (!this.isEmpty()) {
            target.push(this.pop());
        }
    }

    public void clear() {
        this.queue.clear();
    }
}
