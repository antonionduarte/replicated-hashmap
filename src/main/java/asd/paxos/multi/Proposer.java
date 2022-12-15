package asd.paxos.multi;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.Ballot;
import asd.paxos.CommandQueue;
import asd.paxos.Configurations;
import asd.paxos.PaxosCmd;
import asd.paxos.PaxosConfig;
import asd.paxos.ProcessId;
import asd.paxos.Proposal;
import asd.paxos.ProposalSlot;
import asd.paxos.ProposalValue;
import asd.slog.SLog;
import asd.slog.SLogger;

public class Proposer {
    private static enum State {
        WAITING_PROPOSAL, WAITING_PREPARE_OK, WAITING_ACCEPT_OK,
    }

    private static final Logger logger = LogManager.getLogger(Proposer.class);
    private static final SLogger slogger = SLog.logger(Proposer.class);

    private final ProcessId id;
    private final CommandQueue queue;
    private final Configurations configurations;
    private final Duration majorityTimeout;

    private State state;
    private Ballot ballot;
    private ProposalValue originalProposalValue;
    private ProposalValue activeProposalValue;
    private TreeMap<Integer, Proposal> proposals;
    private int currslot;
    private Set<ProcessId> curracceptors;
    private Set<ProcessId> curroks;
    private int currtimer;
    private boolean hasLead;

    public Proposer(ProcessId processId, CommandQueue queue, PaxosConfig config, Configurations configurations) {
        this.id = processId;
        this.queue = queue;
        this.configurations = configurations;
        this.majorityTimeout = config.majorityTimeout;

        this.state = State.WAITING_PROPOSAL;
        this.ballot = new Ballot(this.id, 0);
        this.originalProposalValue = null;
        this.activeProposalValue = null;
        this.proposals = new TreeMap<>();
        this.currslot = config.initialSlot;
        this.curracceptors = Set.copyOf(config.membership.acceptors);
        this.curroks = new HashSet<>();
        this.currtimer = 0;
        this.hasLead = false;
    }

    public boolean canPropose() {
        return this.state == State.WAITING_PROPOSAL && this.configurations.contains(this.currslot);
    }

    public int getCurrentSlot() {
        return this.currslot;
    }

    public void propose(ProposalValue value) {
        assert this.canPropose();

        this.originalProposalValue = value;
        this.curracceptors = Set.copyOf(this.configurations.get(this.currslot).acceptors);
        this.curroks.clear();

        logger.debug("Acceptors for slot {}: {}", this.currslot, this.curracceptors);

        if (!this.hasLead) {
            slogger.log("send-prepare-request",
                    "slot", this.currslot,
                    "lead", this.hasLead);
            this.state = State.WAITING_PREPARE_OK;
            this.curracceptors.forEach(acceptor -> {
                this.queue.push(PaxosCmd.prepareRequest(this.currslot, acceptor, this.ballot));
            });
        } else {
            slogger.log("send-accept-request",
                    "slot", this.currslot,
                    "hasLead", this.hasLead);
            // TODO: Check if this.ballot is correct
            this.state = State.WAITING_ACCEPT_OK;
            this.activeProposalValue = this.originalProposalValue;

            var proposal = new Proposal(this.ballot, this.activeProposalValue);
            this.curracceptors.forEach(acceptor -> {
                this.queue.push(PaxosCmd.acceptRequest(this.currslot, acceptor, proposal));
            });
        }
        this.setupTimer(this.getRandomisedMajorityTimeout());
    }

    public void onPrepareOk(int slot, ProcessId processId, Ballot ballot, List<ProposalSlot> accepted) {
        if (this.currslot != slot) {
            logger.trace("Received prepare-ok for slot {} in state {}", slot, this.state);
            return;
        }
        if (this.state != State.WAITING_PREPARE_OK) {
            logger.trace("Received prepare-ok in state {}", this.state);
            return;
        }
        if (!this.curracceptors.contains(processId)) {
            logger.debug("Received prepare-ok from non-acceptor {}", processId);
            return;
        }

        slogger.log("received-prepare-ok",
                "slot", this.currslot,
                "from", processId,
                "ballot", ballot);

        for (var pslot : accepted) {
            var current = this.proposals.get(pslot.slot);
            if (current == null || pslot.proposal.ballot.compare(current.ballot) == Ballot.Order.GREATER)
                this.proposals.put(pslot.slot, pslot.proposal);
        }

        this.curroks.add(processId);
        if (this.curroks.size() == this.getMajority()) {

            var proposal = switch (this.proposals.size()) {
                case 0 -> new Proposal(this.ballot, this.originalProposalValue);
                default -> {
                    var highestAcceptedEntry = this.proposals.lastEntry();
                    var highestAcceptedSlot = highestAcceptedEntry.getKey();
                    var highestAcceptedValue = highestAcceptedEntry.getValue().value;

                    var proposalValue = switch ((highestAcceptedSlot >= slot) ? 1 : 0) {
                        case 1 -> {
                            // Fast-forward to the highest accepted slot
                            this.currslot = highestAcceptedSlot;
                            this.curracceptors = Set.copyOf(this.configurations.get(this.currslot).acceptors);
                            this.proposals.remove(highestAcceptedSlot);
                            yield highestAcceptedValue;
                        }
                        default /* 0 */ -> this.originalProposalValue;
                    };

                    // In this implementation of MultiPaxos we only do one proposal at a time
                    // Since we removed the proposal for the current slot above
                    // then we can just assume that every other proposal that is in the map
                    // has already been decided since we only keep the highest proposal for each
                    // slot and we contacted a majority of acceptors
                    for (var entry : this.proposals.entrySet()) {
                        var pslot = entry.getKey();
                        var value = entry.getValue().value;
                        this.queue.push(PaxosCmd.learn(pslot, this.id, value));
                    }

                    yield new Proposal(this.ballot, proposalValue);
                }
            };

            this.state = State.WAITING_ACCEPT_OK;
            this.curroks.clear();
            this.hasLead = true;
            this.activeProposalValue = proposal.value;

            this.curracceptors.forEach(acceptor -> {
                this.queue.push(PaxosCmd.acceptRequest(this.currslot, acceptor, proposal));
            });
        }
    }

    public void onAcceptOk(int slot, ProcessId processId, Ballot ballot) {
        if (this.currslot != slot || !this.ballot.equals(ballot)) {
            logger.trace("Received accept-ok for slot {} in state {}", slot, this.state);
            return;
        }
        if (this.state != State.WAITING_ACCEPT_OK) {
            logger.trace("Received accept-ok in state {}", this.state);
            return;
        }
        if (!this.curracceptors.contains(processId)) {
            logger.debug("Received accept-ok from non-acceptor {}", processId);
            return;
        }

        this.curroks.add(processId);
        if (this.curroks.size() == this.getMajority()) {
            var learners = this.configurations.get(this.currslot).learners;
            learners.forEach(learner -> {
                this.queue.push(PaxosCmd.learn(this.currslot, learner, this.activeProposalValue));
            });

            this.cancelTimer();
            this.state = State.WAITING_PROPOSAL;
            this.currslot += 1;
            this.originalProposalValue = null;
            this.activeProposalValue = null;
            this.proposals.clear();
            this.curracceptors = Set.of();
            this.curroks.clear();
        }
    }

    public void onTimer(int timerId) {
        assert this.currtimer == timerId;

        slogger.log("majority-timeout",
                "slot", this.currslot,
                "state", this.state,
                "ballot", this.ballot);

        this.state = State.WAITING_PREPARE_OK;
        this.ballot = this.ballot.withIncSeqNumber();
        this.hasLead = false;
        this.curracceptors.forEach(acceptor -> {
            this.queue.push(PaxosCmd.prepareRequest(this.currslot, acceptor, this.ballot));
        });
        this.setupTimer(this.getRandomisedMajorityTimeout());
    }

    public void onLearn(int slot, ProposalValue value) {

    }

    public Optional<ProposalValue> preempt() {
        var prop = Optional.ofNullable(this.originalProposalValue);
        this.originalProposalValue = null;
        this.activeProposalValue = null;
        this.proposals.clear();
        this.curracceptors = Set.of();
        this.state = State.WAITING_PROPOSAL;
        this.hasLead = false;
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
