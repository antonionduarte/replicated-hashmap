package asd.paxos.multi;

import asd.paxos.Ballot;
import asd.paxos.ProcessId;
import asd.paxos.ProposalValue;

import java.util.Optional;

public class MultiPaxos {
	private final MultiPaxosIO multipaxosIO;

	private final Proposer proposer;
	private final Learner learner;
	private final Acceptor acceptor;

	private ProcessId leaderId;

	public MultiPaxos(ProcessId id, MultiPaxosIO io, MultipaxosConfig config) {
		this.multipaxosIO = io;
		this.proposer = new Proposer(multipaxosIO, id, config);
		this.learner = new Learner(multipaxosIO, id, config);
		this.acceptor = new Acceptor(multipaxosIO, id, config);
	}

	public boolean canPropose() {
		return this.proposer.canPropose() && !this.learner.hasDecided();
	}

	public void receivePrepareRequest(ProcessId processId, Ballot ballot, MultipaxosConfig config) {
		this.acceptor.onPrepareRequest(processId, ballot, config);
	}

	public void receivePrepareOk(ProcessId processId, Ballot ballot, Proposal highestAccept, MultipaxosConfig config) {
		this.proposer.receivePrepareOk(processId, ballot, highestAccept, config);
	}

	public void receiveAcceptRequest(ProcessId processId, Proposal proposal) {
		this.acceptor.onAcceptRequest(processId, proposal);
	}

	public void receiveAcceptOk(ProcessId processId, Ballot ballot, MultipaxosConfig config) {
		this.proposer.receiveAcceptOk(processId, ballot, config);
	}

	public void setLeader(ProcessId leaderId) {
		this.leaderId = leaderId;
	}

	public void receiveDecide(ProcessId processId, ProposalValue proposal) {
		// TODO;
	}

}
