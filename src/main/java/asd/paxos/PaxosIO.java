package asd.paxos;

import java.util.Optional;

import asd.paxos.proposal.Proposal;
import asd.paxos.proposal.ProposalNumber;
import asd.paxos.proposal.ProposalValue;

public interface PaxosIO {
    /**
     * Get the process id of the current process.
     * 
     * @return the process id of the current process
     */
    ProcessId getProcessId();

    /**
     * Send a prepare request to an acceptor.
     * 
     * @param processId      The acceptor to send the request to.
     * @param proposalNumber The proposal number to send.
     */
    void sendPrepareRequest(ProcessId processId, ProposalNumber proposalNumber);

    /**
     * Sends a prepare-ok message to a proposer.
     * 
     * @param processId      The proposer to send the message to.
     * @param proposalNumber The proposal number of the prepare-ok message.
     * @param highestAccept  The highest proposal number that the acceptor has
     *                       accepted.
     */
    void sendPrepareOk(ProcessId processId, ProposalNumber proposalNumber, Optional<Proposal> highestAccept);

    /**
     * Sends an accept request to an acceptor.
     * 
     * @param processId The acceptor to send the request to.
     * @param proposal  The proposal to send.
     */
    void sendAcceptRequest(ProcessId processId, Proposal proposal);

    /**
     * Sends an accept-ok message to a proposer.
     * 
     * @param processId      The proposer to send the message to.
     * @param proposalNumber The proposal number of the accept-ok message.
     */
    void sendAcceptOk(ProcessId processId, ProposalNumber proposalNumber);

    /**
     * Sends a decided message to a learner.
     * 
     * @param processId The learner to send the message to.
     * @param proposal  The proposal value that was decided.
     */
    void sendDecide(ProcessId processId, ProposalValue proposal);
}
