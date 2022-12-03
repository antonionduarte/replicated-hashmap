package asd.paxos2.single;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos2.Ballot;
import asd.paxos2.ProcessId;
import asd.paxos2.ProposalValue;

class Proposer {
    private static final Logger logger = LogManager.getLogger(Proposer.class);

    private static enum Phase {
        PREPARE, ACCEPT, DECIDED
    }

    private final ProcessId id;
    private final PaxosIO io;
    private final Set<ProcessId> acceptors;
    private final Set<ProcessId> learners;
    private final int quorumSize;
    private final Duration majorityTimeout;

    // Can be null if we don't have a proposal for this instance yet.
    private boolean isOriginalProposal;
    private Proposal currentProposal;
    private Phase currentPhase;
    private Set<ProcessId> currentOks;
    private int currentTimerId;

    public Proposer(ProcessId id, PaxosIO io, PaxosConfig config) {
        this.id = id;
        this.io = io;
        this.acceptors = new HashSet<>(config.membership);
        this.learners = new HashSet<>(config.membership);
        this.quorumSize = (this.acceptors.size() / 2) + 1;
        this.majorityTimeout = config.majorityTimeout;

        this.isOriginalProposal = false;
        this.currentProposal = null;
        this.currentPhase = Phase.PREPARE;
        this.currentOks = new HashSet<>();
        this.currentTimerId = 0;
    }

    public ProcessId getProcessId() {
        return this.id;
    }

    public boolean canPropose() {
        return this.currentPhase == Phase.PREPARE && this.currentProposal == null;
    }

    public void propose(ProposalValue value) {
        if (this.currentProposal != null)
            throw new IllegalStateException("Already have a proposal");
        assert this.currentPhase == Phase.PREPARE;

        logger.debug("Proposing value {}", value);
        this.isOriginalProposal = true;
        this.currentProposal = new Proposal(new Ballot(this.id, 0), value);
        this.acceptors.forEach(acceptor -> {
            this.io.push(PaxosCmd.sendPrepareRequest(acceptor, this.currentProposal.ballot));
        });
        this.io.push(PaxosCmd.setupTimer(this.currentTimerId, this.majorityTimeout));
    }

    public void receivePrepareOk(ProcessId processId, Ballot ballot, Optional<Proposal> highestAccept) {
        if (this.currentProposal == null)
            throw new IllegalStateException("Don't have a proposal");

        if (this.currentPhase != Phase.PREPARE) {
            logger.debug("Ignoring prepareOk from {} because we're in phase {}", processId, this.currentPhase);
            return;
        }

        if (!ballot.equals(this.currentProposal.ballot)) {
            logger.debug("Ignoring prepareOk from {} because it's for a different ballot", processId);
            return;
        }

        if (highestAccept.isPresent()) {
            var accept = highestAccept.get();
            if (this.isOriginalProposal || accept.ballot.compare(this.currentProposal.ballot) == Ballot.Order.GREATER) {
                this.currentProposal = accept;
                this.isOriginalProposal = false;
                logger.debug("Updated proposal to {}", this.currentProposal);
            }
        }

        this.currentOks.add(processId);
        if (this.currentOks.size() == this.quorumSize)

        {
            logger.debug("Got quorum of prepareOks, moving to Accept phase");
            this.currentPhase = Phase.ACCEPT;
            this.currentOks.clear();
            this.acceptors.forEach(acceptor -> {
                this.io.push(PaxosCmd.sendAcceptRequest(acceptor, this.currentProposal));
            });
        }
    }

    public void receiveAcceptOk(ProcessId processId, Ballot ballot) {
        if (this.currentProposal == null)
            throw new IllegalStateException("Don't have a proposal");

        if (this.currentPhase != Phase.ACCEPT) {
            logger.debug("Ignoring acceptOk from {} because we're in phase {}", processId, this.currentPhase);
            return;
        }

        if (!ballot.equals(this.currentProposal.ballot)) {
            logger.debug("Ignoring acceptOk from {} because it's for a different ballot", processId);
            return;
        }

        this.currentOks.add(processId);
        if (this.currentOks.size() == this.quorumSize) {
            logger.debug("Got quorum of acceptOks, moving to Decided phase");
            this.io.push(PaxosCmd.cancelTimer(this.currentTimerId));
            this.currentTimerId += 1;
            this.currentPhase = Phase.DECIDED;
            this.currentOks.clear();
            this.learners.forEach(learner -> {
                this.io.push(PaxosCmd.sendDecided(learner, this.currentProposal.value));
            });
        }
    }

    public void triggerTimer(int timerId) {
        assert this.currentTimerId == timerId;

        logger.debug("Majority timeout triggered");
        this.currentTimerId += 1;
        this.currentPhase = Phase.PREPARE;
        this.currentProposal = this.currentProposal.withIncSeqNumber();
        this.currentOks.clear();
        this.acceptors.forEach(acceptor -> {
            this.io.push(PaxosCmd.sendPrepareRequest(acceptor, this.currentProposal.ballot));
        });
        this.io.push(PaxosCmd.setupTimer(this.currentTimerId, this.getRandomisedMajorityTimeout()));
    }

    private Duration getRandomisedMajorityTimeout() {
        return Duration.ofMillis((long) (this.majorityTimeout.toMillis() * (1 + Math.random())));
    }
}
