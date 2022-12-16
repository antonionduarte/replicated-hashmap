package asd.paxos.multi;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.Ballot;
import asd.paxos.CommandQueue;
import asd.paxos.Configurations;
import asd.paxos.PaxosCmd;
import asd.paxos.ProcessId;
import asd.paxos.Proposal;
import asd.paxos.ProposalSlot;
import asd.slog.SLog;
import asd.slog.SLogger;

public class Acceptor {
    private static final Logger logger = LogManager.getLogger(Acceptor.class);
    private static final SLogger slogger = SLog.logger(Acceptor.class);

    private final ProcessId id;
    private final CommandQueue queue;
    private final Configurations configurations;

    private Ballot promise;
    private TreeMap<Integer, Proposal> accepted;

    public Acceptor(ProcessId id, CommandQueue queue, Configurations configurations) {
        this.id = id;
        this.queue = queue;
        this.configurations = configurations;
        this.promise = new Ballot();
        this.accepted = new TreeMap<>();
    }

    public ProcessId getId() {
        return id;
    }

    public void onPrepareRequest(int slot, ProcessId processId, Ballot ballot) {
        // NOTE: This might not be ideal for large memberships
        var proposers = this.configurations.get(slot).proposers;
        if (!proposers.contains(processId)) {
            logger.debug("Ignoring prepare request from unknown proposer {}", processId);
            return;
        }
        if (ballot.compare(this.promise) == Ballot.Order.LESS) {
            logger.debug("Ignoring prepare request from {} with ballot {}", processId, ballot);
            return;
        }

        var accepted = this.getProposalSlotsStartingAt(slot);

        this.promise = ballot;
        this.queue.push(
                PaxosCmd.newLeader(processId, new ArrayList<>()),
                PaxosCmd.prepareOk(slot, processId, ballot, accepted));

        logger.trace("Sending prepare ok to {} with ballot {}", processId, ballot);
        slogger.log("send-prepare-ok",
                "slot", slot,
                "proposer", processId,
                "ballot", ballot);
    }

    public void onAcceptRequest(int slot, ProcessId processId, Proposal proposal) {
        // NOTE: This might not be ideal for large memberships
        var proposers = this.configurations.get(slot).proposers;
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
        this.queue.push(PaxosCmd.acceptOk(slot, processId, promise));

        logger.trace("Sending accept ok to {} with ballot {}", processId, proposal.ballot);
        slogger.log("send-accept-ok",
                "slot", slot,
                "proposer", processId,
                "ballot", proposal.ballot);
    }

    public void removeUpTo(int slot) {
        this.accepted.headMap(slot).clear();
    }

    private List<ProposalSlot> getProposalSlotsStartingAt(int slot) {
        var slots = new ArrayList<ProposalSlot>();
        var view = this.accepted.tailMap(slot, true);
        for (var entry : view.entrySet())
            slots.add(new ProposalSlot(entry.getKey(), entry.getValue()));
        return slots;
    }
}
