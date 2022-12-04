package asd.paxos.multi;

import asd.paxos.Ballot;
import asd.paxos.ProcessId;
import asd.paxos.Proposal;
import asd.paxos.ProposalValue;

import java.util.Optional;

public class Multipaxos {
	private final MultipaxosIO multipaxosIO;
	private final Proposer proposer;
	private final Learner learner;
	private final Acceptor acceptor;

	private ProcessId leaderId;

	public Multipaxos(ProcessId id, MultipaxosIO io, MultipaxosConfig config) {
		this.multipaxosIO = io;
		this.proposer = new Proposer(multipaxosIO, id, config);
		this.learner = new Learner(multipaxosIO, id, config);
		this.acceptor = new Acceptor(multipaxosIO, id, config);
	}

	public boolean canPropose() {
		return this.proposer.canPropose() && !this.learner.hasDecided();
	}

	public void receivePrepareRequest(ProcessId processId, Ballot ballot) {
		this.proposer.onPrepareRequest(processId, ballot);
		this.leaderId = processId;
	}

	public void receivePrepareOk(ProcessId processId, Ballot ballot, Optional<Proposal> highestAccept) {
		this.proposer.receivePrepareOk(processId, ballot, highestAccept);
	}

	public void receiveAcceptRequest(ProcessId processId, Proposal proposal) {
		this.acceptor.onAcceptRequest(processId, proposal);
	}

	public void receiveAcceptOk(ProcessId processId, Ballot ballot) {
		this.proposer.receiveAcceptOk(processId, ballot);
	}

	public void receiveDecide(ProcessId processId, ProposalValue proposal) {
		// TODO;
	}

}
