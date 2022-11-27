package asd.paxos;

import java.util.Optional;

import asd.paxos.proposal.Proposal;
import asd.paxos.proposal.ProposalNumber;

public class Acceptor {
    private final PaxosIO paxosIO;
    private ProposalNumber highestPromise;
    private Optional<Proposal> highestAccept;

    public Acceptor(PaxosIO paxosIO) {
        this.paxosIO = paxosIO;
        this.highestPromise = new ProposalNumber();
        this.highestAccept = Optional.empty();
    }

    public ProcessId getProcessId() {
        return paxosIO.getProcessId();
    }

    public void onPrepareRequest(ProcessId processId, ProposalNumber proposalNumber) {
        if (proposalNumber.compare(this.highestPromise) != ProposalNumber.Order.GREATER)
            return;

        this.highestPromise = proposalNumber;
        this.paxosIO.sendPrepareOk(processId, proposalNumber, this.highestAccept);
    }

    public void onAcceptRequest(ProcessId processId, Proposal proposal) {
        if (proposal.number.compare(this.highestPromise) == ProposalNumber.Order.LESS)
            return;

        this.highestPromise = proposal.number;
        this.highestAccept = Optional.of(proposal);
        this.paxosIO.sendAcceptOk(processId, proposal.number);
    }
}
