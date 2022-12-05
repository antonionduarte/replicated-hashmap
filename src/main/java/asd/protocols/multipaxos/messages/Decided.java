package asd.protocols.multipaxos.messages;

import asd.paxos.ProcessId;
import asd.paxos.ProposalValue;
import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

import java.util.List;

public class Decided extends ProtoMessage {
	public static short ID = Agreement.ID + 4;

	private final int instance;
	private final List<ProcessId> membership;
	private final ProposalValue value;

	public Decided(int instance, List<ProcessId> membership, ProposalValue value) {
		super(ID);

		this.instance = instance;
		this.membership = membership;
		this.value = value;
	}

	public List<ProcessId> getMembership() {
		return membership;
	}

	public int getInstance() {
		return instance;
	}

	public ProposalValue getValue() {
		return value;
	}
}
