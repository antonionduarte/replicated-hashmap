package asd.paxos.multi;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.Ballot;
import asd.paxos.CommandQueue;
import asd.paxos.PaxosCmd;
import asd.paxos.PaxosConfig;
import asd.paxos.PaxosLog;
import asd.paxos.ProcessId;
import asd.paxos.Proposal;

public class Acceptor {
    private static final Logger logger = LogManager.getLogger(Acceptor.class);

    private final ProcessId id;
    private final CommandQueue queue;

    private Ballot promise;
    private TreeMap<Integer, Proposal> accepted;
    private Set<ProcessId> proposers;

    public Acceptor(ProcessId id, CommandQueue queue, PaxosConfig config) {
        this.id = id;
        this.queue = queue;
        this.promise = new Ballot();
        this.accepted = new TreeMap<>();
        this.proposers = Set.copyOf(config.membership.proposers);
    }

    public ProcessId getId() {
        return id;
    }

    public void onPrepareRequest(int slot, ProcessId processId, Ballot ballot) {
        if (!proposers.contains(processId)) {
            logger.debug("Ignoring prepare request from unknown proposer {}", processId);
            return;
        }
        if (ballot.compare(this.promise) != Ballot.Order.GREATER) {
            logger.debug("Ignoring prepare request from {} with ballot {}", processId, ballot);
            return;
        }

        var proposal = Optional.ofNullable(this.accepted.get(slot));
        this.promise = ballot;
        this.queue.push(PaxosCmd.newLeader(processId, new ArrayList<>()));
        this.queue.push(PaxosCmd.prepareOk(processId, ballot, proposal, slot));
        logger.debug("Sending prepare ok to {} with ballot {}", processId, ballot);
        PaxosLog.log("send-prepare-ok",
                "proposer", processId,
                "ballot", ballot);
    }

    public void onAcceptRequest(int slot, ProcessId processId, Proposal proposal) {
        if (!proposers.contains(processId)) {
            logger.debug("Ignoring accept request from unknown proposer {}", processId);
            return;
        }
        if (proposal.ballot.compare(this.promise) == Ballot.Order.LESS) {
            logger.debug("Ignoring accept request from {} with proposal {}", processId, proposal);
            return;
        }

        this.promise = proposal.ballot;
        this.accepted.put(slot, proposal);
        this.queue.push(PaxosCmd.acceptOk(processId, promise, slot));
        logger.debug("Sending accept ok to {} with ballot {}", processId, proposal.ballot);
        PaxosLog.log("send-accept-ok",
                "proposer", processId,
                "ballot", proposal.ballot);
    }
}
