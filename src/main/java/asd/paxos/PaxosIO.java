package asd.paxos;

import java.time.Duration;
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
     * Called when this instance of paxos has decided on a value.
     * Only called once per paxos instance.
     * 
     * @param proposedValue The value that has been decided.
     */
    void decided(ProposalValue proposedValue);

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

    /**
     * Create a periodic timer with the given interval.
     * If the given timerId is already in use, the old timer is cancelled and a new
     * one is created. The first timer event is sent after the given interval.
     * 
     * @param timerId  The id of the timer.
     * @param interval The interval of the timer.
     */
    void setupTimer(int timerId, Duration interval);

    /**
     * Cancel the timer with the given timerId.
     * Calling this method on a timer that does not exit has no effect.
     * 
     * @param timerId The id of the timer to cancel.
     */
    void cancelTimer(int timerId);
}
