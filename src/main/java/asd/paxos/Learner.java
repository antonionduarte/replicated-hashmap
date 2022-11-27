package asd.paxos;

import asd.paxos.proposal.ProposalValue;

class Learner {
    private final PaxosIO paxosIO;
    private ProposalValue value;

    public Learner(PaxosIO paxosIO) {
        this.paxosIO = paxosIO;
        this.value = null;
    }

    public ProcessId getProcessId() {
        return paxosIO.getProcessId();
    }

    public boolean hasDecided() {
        return this.value != null;
    }

    public void onDecide(ProposalValue value) {
        if (this.value == null) {
            this.value = value;
            this.paxosIO.decided(value);
        } else if (!this.value.equals(value)) {
            throw new IllegalStateException("Two different values were decided");
        }
    }
}
