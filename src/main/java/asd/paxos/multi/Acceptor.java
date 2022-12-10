package asd.paxos.multi;

import asd.paxos.Ballot;
import asd.paxos.ProcessId;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class Acceptor {
	private static final Logger logger = LogManager.getLogger(asd.paxos.multi.Acceptor.class);

	private final MultiPaxosIO io;
	private final ProcessId id;

	private final Map<Integer, Proposal> accepted; // slot -> proposal
	private Ballot promise;

	public Acceptor(MultiPaxosIO io, ProcessId id, MultipaxosConfig config) {
		this.io = io;
		this.id = id;

		this.accepted = new HashMap<>();
		this.promise = new Ballot();
	}

	public void onPrepareRequest(ProcessId processId, Ballot ballot, MultipaxosConfig config) {

	}

	public void onAcceptRequest(ProcessId processId, Proposal proposal) {
		var slot = proposal.slot;
		var ballot = proposal.ballot;

		var currentPromise = this.promise;

		if (ballot.compare(currentPromise) != Ballot.Order.LESS) {
			logger.debug("Ignoring accept request from {} with proposal {}", processId, proposal);
			return;
		}

		promise = ballot;
		this.accepted.put(slot, proposal);
		this.io.push(MultiPaxosCmd.sendAcceptOk(processId, proposal.ballot, proposal.slot));

		logger.debug("Sending accept ok to {} with ballot {}", processId, proposal.ballot);
	}
}
