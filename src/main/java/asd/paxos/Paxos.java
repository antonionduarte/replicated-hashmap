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

    public Paxos(PaxosIO paxosIO, List<ProcessId> membership) {
        this.paxosIO = paxosIO;
        this.proposer = new Proposer(paxosIO, membership, membership);
        this.acceptor = new Acceptor(paxosIO);
    }

    public ProcessId getProcessId() {
        return this.paxosIO.getProcessId();
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
    }
}
