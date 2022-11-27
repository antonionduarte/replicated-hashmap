package asd.paxos;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import asd.paxos.proposal.Proposal;
import asd.paxos.proposal.ProposalNumber;
import asd.paxos.proposal.ProposalValue;

public class Proposer {
    private static final int INITIAL_PROPOSAL_NUMBER = 1;

    private static enum Phase {
        PREPARE,
        ACCEPT,
        DECIDED
    }

    private final PaxosIO paxosIO;
    private final ProcessId processId;
    private final Set<ProcessId> acceptors;
    private final Set<ProcessId> learners;
    private final int quorumSize;

    // Can be null if we don't have a proposal for this instance yet.
    private Proposal currentProposal;
    private Phase currentPhase;
    private Set<ProcessId> currentOks;

    public Proposer(PaxosIO paxosIO, List<ProcessId> acceptors, List<ProcessId> learners) {
        this.paxosIO = paxosIO;
        this.processId = paxosIO.getProcessId();
        this.acceptors = new HashSet<>(acceptors);
        this.learners = new HashSet<>(learners);
        this.quorumSize = (this.acceptors.size() / 2) + 1;

        this.currentProposal = null;
        this.currentPhase = Phase.PREPARE;
        this.currentOks = new HashSet<>();
    }

    public ProcessId getProcessId() {
        return this.processId;
    }

    public void propose(ProposalValue value) {
        if (this.currentProposal != null)
            throw new IllegalStateException("Already have a proposal");
        assert this.currentPhase == Phase.PREPARE;

        this.currentProposal = new Proposal(new ProposalNumber(this.processId, INITIAL_PROPOSAL_NUMBER), value);
        this.acceptors.forEach(acceptor -> this.paxosIO.sendPrepareRequest(acceptor, this.currentProposal.number));
    }

    public void receivePrepareOk(ProcessId processId, ProposalNumber proposalNumber, Optional<Proposal> highestAccept) {
        if (this.currentProposal == null)
            throw new IllegalStateException("Don't have a proposal");

        if (this.currentPhase != Phase.PREPARE)
            return;

        if (!proposalNumber.equals(this.currentProposal.number))
            return;

        if (highestAccept.isPresent()) {
            var accept = highestAccept.get();
            if (accept.number.compare(this.currentProposal.number) == ProposalNumber.Order.GREATER)
                this.currentProposal = accept;
        }

        this.currentOks.add(processId);
        if (this.currentOks.size() == this.quorumSize) {
            this.currentPhase = Phase.ACCEPT;
            this.currentOks.clear();
            this.acceptors.forEach(acceptor -> this.paxosIO.sendAcceptRequest(acceptor, this.currentProposal));
        }
    }

    public void receiveAcceptOk(ProcessId processId, ProposalNumber proposalNumber) {
        if (this.currentProposal == null)
            throw new IllegalStateException("Don't have a proposal");

        if (this.currentPhase != Phase.ACCEPT)
            return;

        if (!proposalNumber.equals(this.currentProposal.number))
            return;

        this.currentOks.add(processId);
        if (this.currentOks.size() == this.quorumSize) {
            this.currentPhase = Phase.DECIDED;
            this.currentOks.clear();
            this.learners.forEach(learner -> this.paxosIO.sendDecide(learner, this.currentProposal.value));
        }
    }
}
