package asd.paxos.multi;

import asd.paxos.ProcessId;

public class Learner {
	private final MultiPaxosIO io;
	private final ProcessId id;

	// provavelmente nao ha muita diferenca entre o paxos e multipaxos nos learners
	// (e se calhar nem preciso deles)
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
