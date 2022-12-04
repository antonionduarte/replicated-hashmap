package asd.paxos.multi;

import asd.paxos.Ballot;
import asd.paxos.ProcessId;
import asd.paxos.Proposal;

import java.util.Optional;

public class Proposer {
	private static enum Phase {
		PREPARE, ACCEPT, DECIDED
	}

	private final MultipaxosIO multipaxosIO;
	private final ProcessId id;

	private ProcessId leaderId;

	public Proposer(MultipaxosIO multipaxosIO, ProcessId id, MultipaxosConfig config) {
		this.multipaxosIO = multipaxosIO;
		this.id = id;
	}

	public void receivePrepareOk(ProcessId processId, Ballot ballot, Optional<Proposal> highestAccept) {

	}

	public boolean canPropose() {
		return false; // TODO
	}

	public void receiveAcceptOk(ProcessId processId, Ballot ballot) {

	}

	public void onPrepareRequest(ProcessId processId, Ballot ballot) {

	}
}
