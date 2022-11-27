package asd.paxos;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;

import asd.paxos.proposal.Proposal;
import asd.paxos.proposal.ProposalNumber;
import asd.paxos.proposal.ProposalValue;

public class PaxosCommandQueue {
    private static class QueueIO implements PaxosIO {
        private final Queue<Command> queue;
        private final ProcessId processId;

        public QueueIO(Queue<Command> queue, ProcessId processId) {
            this.queue = queue;
            this.processId = processId;
        }

        @Override
        public ProcessId getProcessId() {
            return processId;
        }

        @Override
        public void decided(ProposalValue value) {
            queue.add(Command.from(processId, new Command.Decided(value)));
        }

        @Override
        public void sendPrepareRequest(ProcessId processId, ProposalNumber proposalNumber) {
            this.queue.add(Command.from(this.processId, new Command.PrepareRequest(processId, proposalNumber)));
        }

        @Override
        public void sendPrepareOk(ProcessId processId, ProposalNumber proposalNumber,
                Optional<Proposal> highestAccept) {
            this.queue
                    .add(Command.from(this.processId, new Command.PrepareOk(processId, proposalNumber, highestAccept)));
        }

        @Override
        public void sendAcceptRequest(ProcessId processId, Proposal proposal) {
            this.queue.add(Command.from(this.processId, new Command.AcceptRequest(processId, proposal)));
        }

        @Override
        public void sendAcceptOk(ProcessId processId, ProposalNumber proposalNumber) {
            this.queue.add(Command.from(this.processId, new Command.AcceptOk(processId, proposalNumber)));
        }

        @Override
        public void sendDecide(ProcessId processId, ProposalValue proposal) {
            this.queue.add(Command.from(this.processId, new Command.Decide(processId, proposal)));
        }

    }

    private final Queue<Command> queue;

    public PaxosCommandQueue() {
        this.queue = new ArrayDeque<>();
    }

    public PaxosIO getIO(ProcessId processId) {
        return new QueueIO(this.queue, processId);
    }

    public Command pop() {
        return this.queue.remove();
    }

}
