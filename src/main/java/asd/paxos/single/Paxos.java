package asd.paxos.single;

import java.util.Optional;

import asd.paxos.Ballot;
import asd.paxos.ProcessId;
import asd.paxos.ProposalValue;

public class Paxos {
    private final ProcessId id;
    private final Proposer proposer;
    private final Acceptor acceptor;
    private final Learner learner;

    public Paxos(ProcessId id, PaxosIO io, PaxosConfig config) {
        this.id = id;
        this.proposer = new Proposer(id, io, config);
        this.acceptor = new Acceptor(id, io, config);
        this.learner = new Learner(id, io);
    }

    public ProcessId getProcessId() {
        return this.id;
    }

    public boolean canPropose() {
        return this.proposer.canPropose() && !this.learner.hasDecided();
    }

    public void propose(ProposalValue value) {
        this.proposer.propose(value);
    }

    public void receivePrepareRequest(ProcessId processId, Ballot ballot) {
        this.acceptor.onPrepareRequest(processId, ballot);
    }

    public void receivePrepareOk(ProcessId processId, Ballot ballot, Optional<Proposal> highestAccept) {
        this.proposer.receivePrepareOk(processId, ballot, highestAccept);
    }

    public void receiveAcceptRequest(ProcessId processId, Proposal proposal) {
        this.acceptor.onAcceptRequest(processId, proposal);
    }

    public void receiveAcceptOk(ProcessId processId, Ballot ballot) {
        this.proposer.receiveAcceptOk(processId, ballot);
    }

    public void receiveDecided(ProcessId processId, ProposalValue proposal) {
        this.learner.onDecide(proposal);
    }

    public void triggerTimer(int timerId) {
        this.proposer.triggerTimer(timerId);
    }
}
