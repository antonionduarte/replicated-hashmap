package asd.paxos;

import java.util.List;
import java.util.Optional;

import asd.paxos.proposal.Proposal;
import asd.paxos.proposal.ProposalNumber;
import asd.paxos.proposal.ProposalValue;

public class Paxos {
    private final PaxosIO paxosIO;
    private final Proposer proposer;
    private final Acceptor acceptor;
    private final Learner learner;

    public Paxos(PaxosIO paxosIO, List<ProcessId> membership, PaxosConfig config) {
        this.paxosIO = paxosIO;
        this.proposer = new Proposer(paxosIO, membership, membership, config.majorityTimeout);
        this.acceptor = new Acceptor(paxosIO);
        this.learner = new Learner(paxosIO);
    }

    public Paxos(PaxosIO paxosIO, List<ProcessId> membership) {
        this(paxosIO, membership, new PaxosConfig());
    }

    public ProcessId getProcessId() {
        return this.paxosIO.getProcessId();
    }

    public boolean canPropose() {
        return this.proposer.canPropose() && !this.learner.hasDecided();
    }

    public void propose(ProposalValue value) {
        this.proposer.propose(value);
    }

    public void receivePrepareRequest(ProcessId processId, ProposalNumber proposalNumber) {
        this.acceptor.onPrepareRequest(processId, proposalNumber);
    }

    public void receivePrepareOk(ProcessId processId, ProposalNumber proposalNumber, Optional<Proposal> highestAccept) {
        this.proposer.receivePrepareOk(processId, proposalNumber, highestAccept);
    }

    public void receiveAcceptRequest(ProcessId processId, Proposal proposal) {
        this.acceptor.onAcceptRequest(processId, proposal);
    }

    public void receiveAcceptOk(ProcessId processId, ProposalNumber proposalNumber) {
        this.proposer.receiveAcceptOk(processId, proposalNumber);
    }

    public void receiveDecide(ProcessId processId, ProposalValue proposal) {
        this.learner.onDecide(proposal);
    }

    public void triggerTimer(int timerId) {
        this.proposer.triggerTimer(timerId);
    }
}
