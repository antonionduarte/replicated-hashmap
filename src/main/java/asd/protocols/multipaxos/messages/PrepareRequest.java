package asd.protocols.multipaxos.messages;

import asd.paxos.Ballot;
import asd.paxos.ProcessId;
import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

import java.util.List;

public class PrepareRequest extends ProtoMessage {
	public static short ID = Agreement.ID + 6;

	private final int instance;
	private final List<ProcessId> membership;
	private final Ballot ballot;

	public PrepareRequest(int instance, List<ProcessId> membership, Ballot ballot) {
		super(ID);

		this.instance = instance;
		this.membership = membership;
		this.ballot = ballot;
	}

	public Ballot getBallot() {
		return ballot;
	}

	public int getInstance() {
		return instance;
	}

	public List<ProcessId> getMembership() {
		return membership;
	}
}
