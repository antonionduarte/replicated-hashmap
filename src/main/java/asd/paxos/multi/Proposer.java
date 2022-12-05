package asd.paxos.multi;

import asd.paxos.Ballot;
import asd.paxos.ProcessId;
import asd.paxos.Proposal;
import asd.paxos.ProposalValue;
import asd.paxos.AgreementCmd;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.*;

public class Proposer {
	private static enum Phase {
		PREPARE, ACCEPT, DECIDED
	}

	public static class Slot {
		public List<ProcessId> acceptors;
		public List<ProcessId> learners;
		public int quorumSize;
		public int timerId;

		public Proposal currentProposal; // may be null
		public Ballot currentBallot;
		public Phase phase;
		public Set<ProcessId> oks;
		public Set<ProcessId> prepareOks;

		public Slot(List<ProcessId> acceptors, List<ProcessId> learners) {
			this.acceptors = acceptors;
			this.learners = learners;
			this.quorumSize = (this.acceptors.size() / 2) + 1;

			this.currentProposal = null;
			this.phase = Phase.ACCEPT;
			this.oks = new HashSet<>();
			this.timerId = -1;
		}
	}

	private static final Logger logger = LogManager.getLogger(asd.paxos.multi.Proposer.class);

	private final MultipaxosIO multipaxosIO;
	private final Map<Integer, Slot> slots;
	private final ProcessId id;
	private final Duration majorityTimeout;
	private int currentSlot;
	private ProcessId leaderId;

	public Proposer(MultipaxosIO multipaxosIO, ProcessId id, MultipaxosConfig config) {
		this.multipaxosIO = multipaxosIO;
		this.id = id;
		this.currentSlot = 0;
		this.leaderId = null;
		this.slots = new HashMap<>();
		this.majorityTimeout = config.majorityTimeout;
	}

	// try to become le leader
	public void propose(ProposalValue value, MultipaxosConfig config) {
		assert this.leaderId != id; // we must not already be the leader, i think

		this.currentSlot++;
		this.slots.put(this.currentSlot, new Slot(config.acceptors, config.learners));
		this.slots.get(this.currentSlot).phase = Phase.PREPARE;

		// send prepare to all acceptors
		this.slots.get(currentSlot).acceptors.forEach(acceptor -> {
			this.multipaxosIO.push(AgreementCmd.sendPrepareRequest(acceptor, new Ballot(this.id, 0)));
		});
	}

	public void receivePrepareOk(ProcessId processId, Ballot ballot, Proposal highestAccept, MultipaxosConfig config) {
		if (this.slots.get(currentSlot).currentProposal == null)
			throw new IllegalStateException("Don't have a proposal");

		if (this.slots.get(currentSlot).phase != Phase.PREPARE)
			throw new IllegalStateException("Not in prepare phase");

		// TODO: Detect if leader is behind current slot? I think?
		// Se o lider tiver recebido uma prepareOk de um acceptor, com um Proposal com optional present, 
		// tem de aceitar esse proposal, se ainda nao tiver aceite nenhum proposal, ou se proposal tiver um ballot maior
	}

	public boolean canPropose() {
		return false; // TODO talvez nao seja necessario no multipaxos
	}

	public void receiveAcceptOk(ProcessId processId, Ballot ballot, MultipaxosConfig config) {
		var currentSlot = slots.computeIfAbsent(this.currentSlot, (slot) -> new Slot(config.acceptors, config.learners));

		if (currentSlot.phase != Phase.ACCEPT) {
			logger.debug("Ignoring acceptOk from {} because we're in phase {}", processId, currentSlot.phase);
			return;
		}

		if (!ballot.equals(currentSlot.currentProposal.ballot)) {
			logger.debug("Ignoring acceptOk from {} because it's for a different ballot", processId);
			return;
		}

		currentSlot.oks.add(processId);

		if (currentSlot.oks.size() == currentSlot.quorumSize) {
			logger.debug("Got quorum of acceptOks, moving to Decided phase");
			this.multipaxosIO.push(AgreementCmd.cancelTimer(currentSlot.timerId));
			currentSlot.timerId++;
			currentSlot.phase = Phase.DECIDED;

			currentSlot.learners.forEach(learner -> {
				this.multipaxosIO.push(AgreementCmd.sendDecided(learner, currentSlot.currentProposal.value));
			});
		}
	}
}
