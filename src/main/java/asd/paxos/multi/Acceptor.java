package asd.paxos.multi;

import asd.paxos.Ballot;
import asd.paxos.ProcessId;

import asd.paxos.single.PaxosCmd;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Acceptor {
	private static final Logger logger = LogManager.getLogger(asd.paxos.multi.Acceptor.class);

	private final MultiPaxosIO io;
	private final ProcessId id;

	private Optional<Proposal> accepted; // slot -> proposal
	private Ballot promise;
	private Set<ProcessId> proposers;

	public Acceptor(MultiPaxosIO io, ProcessId id, MultipaxosConfig config) {
		this.io = io;
		this.id = id;

		this.accepted = Optional.empty();
		this.promise = new Ballot();
		this.proposers = new HashSet<>(config.proposers);
	}

	public void onPrepareRequest(ProcessId processId, Ballot ballot, MultipaxosConfig config) {
		if (!proposers.contains(processId)) {
			logger.debug("Ignoring prepare request from unknown proposer {}", processId);
			return;
		}
		if (ballot.compare(this.promise) != Ballot.Order.GREATER) {
			logger.debug("Ignoring prepare request from {} with ballot {}", processId, ballot);
			return;
		}

		this.promise = ballot;
		this.io.push(MultiPaxosCmd.sendPrepareOk(processId, ballot, this.accepted));
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
		this.io.push(MultiPaxosCmd.sendAcceptOk(processId, promise));
		logger.debug("Sending accept ok to {} with ballot {}", processId, proposal.ballot);
	}
}
