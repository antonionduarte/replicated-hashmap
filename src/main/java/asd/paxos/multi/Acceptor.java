package asd.paxos.multi;

import asd.paxos.ProcessId;
import asd.paxos.Proposal;

public class Acceptor {
	private final MultipaxosIO multipaxosIO;
	private final ProcessId id;

	public Acceptor(MultipaxosIO multipaxosIO, ProcessId id, MultipaxosConfig config) {
		this.multipaxosIO = multipaxosIO;
		this.id = id;
	}

	public void onPrepareRequest(ProcessId processId, Proposal proposal) {

	}

	public void onAcceptRequest(ProcessId processId, Proposal proposal) {

	}
}
