package asd.protocols.multipaxos.messages;

import asd.paxos.ProcessId;
import asd.paxos.Proposal;
import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

import java.util.List;

public class AcceptRequest extends ProtoMessage {
	public static short ID = Agreement.ID + 3;

	private final int instance;
	private final List<ProcessId> membership;
	private final Proposal proposal;

	public AcceptRequest(int instance, List<ProcessId> membership, Proposal proposal) {
		super(ID);

		this.instance = instance;
		this.membership = membership;
		this.proposal = proposal;
	}

	public int getInstance() {
		return instance;
	}

	public List<ProcessId> getMembership() {
		return membership;
	}

	public Proposal getProposal() {
		return proposal;
	}
}
