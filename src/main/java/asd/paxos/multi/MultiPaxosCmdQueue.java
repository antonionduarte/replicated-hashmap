package asd.paxos.multi;

import java.util.ArrayDeque;
import java.util.Queue;

public class MultiPaxosCmdQueue implements MultiPaxosIO {

    private final Queue<MultiPaxosCmd> queue;

    public MultiPaxosCmdQueue() {
        this.queue = new ArrayDeque<>();
    }

    @Override
    public void push(MultiPaxosCmd cmd) {
        this.queue.add(cmd);
    }

    public MultiPaxosCmd pop() {
        return this.queue.remove();
    }

    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    public void transferTo(MultiPaxosIO io) {
        while (!this.isEmpty()) {
            io.push(this.pop());
        }
    }

    public void clear() {
        this.queue.clear();
    }
}
