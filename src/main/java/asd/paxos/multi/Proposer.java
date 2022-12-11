package asd.paxos.multi;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.Ballot;
import asd.paxos.CommandQueue;
import asd.paxos.Configurations;
import asd.paxos.PaxosCmd;
import asd.paxos.PaxosConfig;
import asd.paxos.PaxosLog;
import asd.paxos.ProcessId;
import asd.paxos.Proposal;
import asd.paxos.ProposalValue;

public class Proposer {
    private static enum State {
        WAITING_PROPOSAL, WAITING_PREPARE_OK, WAITING_PROPOSE_OK,
    }

    private static final Logger logger = LogManager.getLogger(Proposer.class);

    private final ProcessId id;
    private final CommandQueue queue;
    private final Configurations configurations;
    private final Duration majorityTimeout;

    private State state;
    private Ballot ballot;
    private Ballot proposalBallot;
    private ProposalValue proposal;
    private int currslot;
    private Set<ProcessId> curracceptors;
    private Set<ProcessId> curroks;
    private int currtimer;

    public Proposer(ProcessId processId, CommandQueue queue, PaxosConfig config, Configurations configurations) {
        this.id = processId;
        this.queue = queue;
        this.configurations = configurations;
        this.majorityTimeout = config.majorityTimeout;

        this.state = State.WAITING_PROPOSAL;
        this.ballot = new Ballot(this.id, 0);
        this.proposalBallot = new Ballot();
        this.proposal = null;
        this.currslot = config.initialSlot;
        this.curracceptors = Set.copyOf(config.membership.acceptors);
        this.curroks = new HashSet<>();
        this.currtimer = 0;
    }

    public boolean canPropose() {
        return this.state == State.WAITING_PROPOSAL && this.configurations.contains(this.currslot);
    }

    public void propose(ProposalValue value) {
        assert this.canPropose();

        this.state = State.WAITING_PREPARE_OK;
        this.curracceptors = Set.copyOf(this.configurations.get(this.currslot).acceptors);
        this.curroks.clear();

        this.curracceptors.forEach(acceptor -> {
            this.queue.push(PaxosCmd.prepareRequest(acceptor, this.ballot, this.currslot));
        });
        this.setupTimer(this.getRandomisedMajorityTimeout());
    }

    public void onPrepareOk(int slot, ProcessId processId, Ballot ballot, Optional<Proposal> highestAccept) {
        if (!this.curracceptors.contains(processId)) {
            logger.warn("Received prepare-ok from non-acceptor {}", processId);
            return;
        }
        if (this.state != State.WAITING_PREPARE_OK) {
            logger.warn("Received prepare-ok in state {}", this.state);
            return;
        }
        if (this.currslot != slot) {
            logger.warn("Received prepare-ok for slot {} in state {}", slot, this.state);
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
                        "old", new Proposal(this.proposalBallot, this.proposal),
                        "new", accept);
                this.proposal = accept.value;
                this.proposalBallot = accept.ballot;
                logger.debug("Updated proposal to {}", accept);
            }
        }

        this.curroks.add(processId);
        if (this.curroks.size() == this.getMajority()) {
            var proposal = new Proposal(this.ballot, this.proposal);
            this.state = State.WAITING_PROPOSE_OK;
            this.curroks.clear();

            this.curracceptors.forEach(acceptor -> {
                this.queue.push(PaxosCmd.acceptRequest(acceptor, proposal, this.currslot));
            });
        }
    }

    public void onAcceptOk(int slot, ProcessId processId, Ballot ballot) {
        if (!this.curracceptors.contains(processId)) {
            logger.warn("Received accept-ok from non-acceptor {}", processId);
            return;
        }
        if (this.state != State.WAITING_PROPOSE_OK) {
            logger.warn("Received accept-ok in state {}", this.state);
            return;
        }
        if (this.currslot != slot || !this.ballot.equals(ballot)) {
            logger.warn("Received accept-ok for slot {} in state {}", slot, this.state);
            return;
        }

        this.curroks.add(processId);
        if (this.curroks.size() == this.getMajority()) {
            var learners = this.configurations.get(this.currslot).learners;
            learners.forEach(learner -> {
                this.queue.push(PaxosCmd.learn(learner, this.proposal, this.currslot));
            });

            this.cancelTimer();
            this.state = State.WAITING_PROPOSAL;
            this.proposalBallot = new Ballot();
            this.currslot += 1;
            this.proposal = null;
            this.curracceptors = null;
            this.curroks.clear();
        }
    }

    public void onTimer(int timerId) {
        assert this.currtimer == timerId;

        PaxosLog.log("majority-timeout",
                "state", this.state,
                "ballot", this.ballot);

        this.state = State.WAITING_PREPARE_OK;
        this.ballot = this.ballot.withIncSeqNumber();
        this.proposalBallot = new Ballot();
        this.curracceptors.forEach(acceptor -> {
            this.queue.push(PaxosCmd.prepareRequest(acceptor, this.ballot, this.currslot));
        });
        this.setupTimer(this.getRandomisedMajorityTimeout());
    }

    public Optional<ProposalValue> preempt() {
        var prop = Optional.ofNullable(this.proposal);
        this.proposal = null;
        this.curracceptors = null;
        this.state = State.WAITING_PROPOSAL;
        this.proposalBallot = new Ballot();
        this.cancelTimer();
        return prop;
    }

    private void setupTimer(Duration duration) {
        this.currtimer += 1;
        this.queue.push(PaxosCmd.setupTimer(0, this.currtimer, duration));
    }

    private void cancelTimer() {
        this.queue.push(PaxosCmd.cancelTimer(0, this.currtimer));
        this.currtimer += 1;
    }

    private Duration getRandomisedMajorityTimeout() {
        return Duration.ofMillis((long) (this.majorityTimeout.toMillis() * (1 + Math.random())));
    }

    private int getMajority() {
        return this.curracceptors.size() / 2 + 1;
    }
}
