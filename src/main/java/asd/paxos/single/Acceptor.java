package asd.paxos.single;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.Ballot;
import asd.paxos.ProcessId;

class Acceptor {
    private static final Logger logger = LogManager.getLogger(Acceptor.class);

    private final ProcessId id;
    private final PaxosIO io;

    private Ballot promise;
    private Optional<Proposal> accepted;
    private Set<ProcessId> proposers;

    public Acceptor(ProcessId id, PaxosIO io, PaxosConfig config) {
        this.id = id;
        this.io = io;

        this.promise = new Ballot();
        this.accepted = Optional.empty();
        this.proposers = new HashSet<>(config.proposers);
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
        this.io.push(PaxosCmd.sendPrepareOk(processId, ballot, this.accepted));
        logger.debug("Sending prepare ok to {} with ballot {}", processId, ballot);
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
        this.io.push(PaxosCmd.sendAcceptOk(processId, promise));
        logger.debug("Sending accept ok to {} with ballot {}", processId, proposal.ballot);
    }

}
