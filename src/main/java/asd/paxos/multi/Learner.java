package asd.paxos.multi;

import asd.paxos.ProcessId;

public class Learner {
	private final MultiPaxosIO io;
	private final ProcessId id;

	// TODO: Probably there aren't many differences between Paxos and Multipaxos in the learners.
	public Learner(MultiPaxosIO io, ProcessId id, MultipaxosConfig config) {
		this.io = io;
		this.id = id;
	}

	public boolean hasDecided() {
		return false; // TODO
	}

	public void onDecide() {

	}
}
