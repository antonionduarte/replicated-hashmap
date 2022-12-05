package asd.paxos;

import asd.paxos.single.PaxosIO;

import java.util.ArrayDeque;
import java.util.Queue;

public class AgreementCmdQueue implements PaxosIO {

    private final Queue<AgreementCmd> queue;

    public AgreementCmdQueue() {
        this.queue = new ArrayDeque<>();
    }

    @Override
    public void push(AgreementCmd cmd) {
        this.queue.add(cmd);
    }

    public AgreementCmd pop() {
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
