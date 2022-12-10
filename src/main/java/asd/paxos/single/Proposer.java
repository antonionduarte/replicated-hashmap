package asd.paxos.single;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.Ballot;
import asd.paxos.CommandQueue;
import asd.paxos.PaxosCmd;
import asd.paxos.PaxosConfig;
import asd.paxos.PaxosLog;
import asd.paxos.ProcessId;
import asd.paxos.Proposal;
import asd.paxos.ProposalValue;

class Proposer {
    private static final Logger logger = LogManager.getLogger(Proposer.class);

    private static enum Phase {
        PREPARE, ACCEPT, DECIDED
    }

    private final int slot;
    private final ProcessId id;
    private final CommandQueue queue;
    private final Set<ProcessId> acceptors;
    private final Set<ProcessId> learners;
    private final int quorumSize;
    private final Duration majorityTimeout;

    private Ballot currentBallot;
    private Ballot proposalBallot;
    // Can be null if we don't have a proposal for this instance yet.
    private ProposalValue proposalValue;
    private Phase currentPhase;
    private Set<ProcessId> currentOks;
    private int currentTimerId;

    public Proposer(int slot, ProcessId id, CommandQueue queue, PaxosConfig config) {
        this.slot = slot;
        this.id = id;
        this.queue = queue;
        this.acceptors = new HashSet<>(config.membership.acceptors);
        this.learners = new HashSet<>(config.membership.learners);
        this.quorumSize = (this.acceptors.size() / 2) + 1;
        this.majorityTimeout = config.majorityTimeout;

        this.currentBallot = new Ballot(id, 0);
        this.proposalBallot = new Ballot();
        this.proposalValue = null;
        this.currentPhase = Phase.PREPARE;
        this.currentOks = new HashSet<>();
        this.currentTimerId = 0;
    }

    public ProcessId getProcessId() {
        return this.id;
    }

    public boolean canPropose() {
        return this.currentPhase == Phase.PREPARE && this.proposalValue == null;
    }

    public void propose(ProposalValue value) {
        if (this.proposalValue != null)
            throw new IllegalStateException("Already have a proposal");
        assert this.currentPhase == Phase.PREPARE;

        PaxosLog.log("propose", "value", value);
        logger.debug("Proposing value {}", value);
        this.proposalValue = value;
        this.acceptors.forEach(acceptor -> {
            this.queue.push(PaxosCmd.prepareRequest(acceptor, this.currentBallot, this.slot));
        });
        this.queue.push(PaxosCmd.setupTimer(this.slot, this.currentTimerId, this.majorityTimeout));
    }

    public void receivePrepareOk(ProcessId processId, Ballot ballot, Optional<Proposal> highestAccept) {
        if (this.proposalValue == null)
            throw new IllegalStateException("Don't have a proposal");

        if (!this.acceptors.contains(processId)) {
            logger.debug("Ignoring prepareOk from {} because it's not an acceptor", processId);
            return;
        }

        if (this.currentPhase != Phase.PREPARE) {
            logger.debug("Ignoring prepareOk from {} because we're in phase {}", processId, this.currentPhase);
            return;
        }

        if (!ballot.equals(this.currentBallot)) {
            logger.debug("Ignoring prepareOk from {} because it's for a different ballot", processId);
            return;
        }

        PaxosLog.log("received-prepare-ok",
                "from", processId,
                "ballot", ballot,
                "highestAccept", highestAccept.orElse(null));

        if (highestAccept.isPresent()) {
            var accept = highestAccept.get();
            if (this.proposalBallot.compare(accept.ballot) != Ballot.Order.GREATER) {
                PaxosLog.log("updated-proposal",
                        "old", new Proposal(this.proposalBallot, this.proposalValue),
                        "new", accept);
                this.proposalValue = accept.value;
                this.proposalBallot = accept.ballot;
                logger.debug("Updated proposal to {}", accept);
            }
        }

        this.currentOks.add(processId);
        if (this.currentOks.size() == this.quorumSize) {
            var sentProposal = new Proposal(this.currentBallot, this.proposalValue);
            PaxosLog.log("majority-prepare-ok",
                    "currentBallot", this.currentBallot,
                    "proposal", new Proposal(this.proposalBallot, this.proposalValue),
                    "sentProposal", sentProposal);

            logger.debug("Got quorum of prepareOks, moving to Accept phase");
            this.currentPhase = Phase.ACCEPT;
            this.currentOks.clear();
            this.acceptors.forEach(acceptor -> {
                this.queue.push(PaxosCmd.acceptRequest(acceptor, sentProposal, this.slot));
            });
        }
    }

    public void receiveAcceptOk(ProcessId processId, Ballot ballot) {
        if (this.proposalValue == null)
            throw new IllegalStateException("Don't have a proposal");

        if (!this.acceptors.contains(processId)) {
            logger.debug("Ignoring acceptOk from {} because it's not an acceptor", processId);
            return;
        }

        if (this.currentPhase != Phase.ACCEPT) {
            logger.debug("Ignoring acceptOk from {} because we're in phase {}", processId, this.currentPhase);
            return;
        }

        if (!ballot.equals(this.currentBallot)) {
            logger.debug("Ignoring acceptOk from {} because it's for a different ballot", processId);
            return;
        }

        PaxosLog.log("received-accept-ok",
                "from", processId,
                "ballot", ballot);

        this.currentOks.add(processId);
        if (this.currentOks.size() == this.quorumSize) {
            PaxosLog.log("majority-accept-ok",
                    "currentBallot", this.currentBallot,
                    "value", this.proposalValue);

            logger.debug("Got quorum of acceptOks, moving to Decided phase");
            this.queue.push(PaxosCmd.cancelTimer(this.slot, this.currentTimerId));
            this.currentTimerId += 1;
            this.currentPhase = Phase.DECIDED;
            this.currentOks.clear();
            this.learners.forEach(learner -> {
                this.queue.push(PaxosCmd.decided(learner, this.proposalValue, this.slot));
            });
        }
    }

    public void triggerTimer(int timerId) {
        assert this.currentTimerId == timerId;

        PaxosLog.log("majority-timeout",
                "phase", this.currentPhase,
                "currentBallot", this.currentBallot,
                "proposalBallot", this.proposalBallot,
                "proposalValue", this.proposalValue);

        logger.debug("Majority timeout triggered");
        this.currentTimerId += 1;
        this.currentPhase = Phase.PREPARE;
        this.currentBallot = new Ballot(this.id,
                Math.max(this.currentBallot.sequenceNumber, this.proposalBallot.sequenceNumber) + 1);
        // Reset the proposalBallot so that if we receive a proposal value from a
        // prepareOk then we replace our value with that one since that value's ballot
        // will always be greater than the default ballot
        this.proposalBallot = new Ballot();
        this.currentOks.clear();
        this.acceptors.forEach(acceptor -> {
            this.queue.push(PaxosCmd.prepareRequest(acceptor, this.currentBallot, this.slot));
        });
        this.queue.push(PaxosCmd.setupTimer(this.slot, this.currentTimerId, this.getRandomisedMajorityTimeout()));
    }

    public void moveToDecided() {
        this.queue.push(PaxosCmd.cancelTimer(this.slot, this.currentTimerId));
        this.currentPhase = Phase.DECIDED;
    }

    private Duration getRandomisedMajorityTimeout() {
        return Duration.ofMillis((long) (this.majorityTimeout.toMillis() * (1 + Math.random())));
    }
}
