package asd.paxos.single;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import asd.paxos.AgreementCmd;
import asd.paxos.ProcessId;

public class PaxosCommandQueue {
    private static class QueueIO implements PaxosIO {
        private final Queue<Message> queue;
        private final ProcessId processId;

        public QueueIO(Queue<Message> queue, ProcessId processId) {
            this.queue = queue;
            this.processId = processId;
        }

        @Override
        public void push(AgreementCmd cmd) {
            this.queue.add(new Message(this.processId, cmd));
        }

    }

    private final Queue<Message> queue;

    public PaxosCommandQueue() {
        this.queue = new ArrayDeque<>();
    }

    public PaxosIO getIO(ProcessId processId) {
        return new QueueIO(this.queue, processId);
    }

    public Message pop() {
        return this.queue.remove();
    }

    public List<Message> popAll() {
        var commands = new ArrayList<Message>();
        while (!this.queue.isEmpty()) {
            commands.add(this.queue.remove());
        }
        return commands;
    }

    public boolean isEmpty() {
        return this.queue.isEmpty();
    }
}
