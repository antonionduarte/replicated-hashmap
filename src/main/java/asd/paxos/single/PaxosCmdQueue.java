package asd.paxos.single;

import java.util.ArrayDeque;
import java.util.Queue;

public class PaxosCmdQueue implements PaxosIO {

    private final Queue<PaxosCmd> queue;

    public PaxosCmdQueue() {
        this.queue = new ArrayDeque<>();
    }

    @Override
    public void push(PaxosCmd cmd) {
        this.queue.add(cmd);
    }

    public PaxosCmd pop() {
        return this.queue.remove();
    }

    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    public void transferTo(PaxosIO io) {
        while (!this.isEmpty()) {
            io.push(this.pop());
        }
    }

    public void clear() {
        this.queue.clear();
    }
}
