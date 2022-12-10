package asd.paxos.single;

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

class Acceptor {
    private static final Logger logger = LogManager.getLogger(Acceptor.class);

    private final int slot;
    private final ProcessId id;
    private final CommandQueue queue;

    private Ballot promise;
    private Optional<Proposal> accepted;
    private Set<ProcessId> proposers;

    public Acceptor(int slot, ProcessId id, CommandQueue queue, PaxosConfig config) {
        this.slot = slot;
        this.id = id;
        this.queue = queue;

        this.promise = new Ballot();
        this.accepted = Optional.empty();
        this.proposers = new HashSet<>(config.membership.proposers);
    }

    public ProcessId getId() {
        return id;
    }

    public void onPrepareRequest(ProcessId processId, Ballot ballot) {
        if (!proposers.contains(processId)) {
            logger.debug("Ignoring prepare request from unknown proposer {}", processId);
            return;
        }
        if (ballot.compare(this.promise) != Ballot.Order.GREATER) {
            logger.debug("Ignoring prepare request from {} with ballot {}", processId, ballot);
            return;
        }

        this.promise = ballot;
        this.queue.push(PaxosCmd.prepareOk(processId, ballot, this.accepted, this.slot));
        logger.debug("Sending prepare ok to {} with ballot {}", processId, ballot);
        PaxosLog.log("send-prepare-ok",
                "proposer", processId,
                "ballot", ballot);
    }

    public void onAcceptRequest(ProcessId processId, Proposal proposal) {
        if (!proposers.contains(processId)) {
            logger.debug("Ignoring accept request from unknown proposer {}", processId);
            return;
        }
        if (proposal.ballot.compare(this.promise) == Ballot.Order.LESS) {
            logger.debug("Ignoring accept request from {} with proposal {}", processId, proposal);
            return;
        }

        this.promise = proposal.ballot;
        this.accepted = Optional.of(proposal);
        this.queue.push(PaxosCmd.acceptOk(processId, promise, this.slot));
        logger.debug("Sending accept ok to {} with ballot {}", processId, proposal.ballot);
        PaxosLog.log("send-accept-ok",
                "proposer", processId,
                "ballot", proposal.ballot);
    }

}
