package asd.protocols.multipaxos.messages;

import asd.paxos.Ballot;
import asd.paxos.multi.Proposal;
import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public class PrepareOk extends ProtoMessage {
	public static short ID = Agreement.ID + 5;

	private final int instance;
	private final Ballot ballot;
	private final Proposal acceptedProposal;
	private final boolean decided;

	public PrepareOk(int instance, Ballot ballot, Proposal acceptedProposal, boolean decided) {
		super(ID);

		this.instance = instance;
		this.ballot = ballot;
		this.acceptedProposal = acceptedProposal;
		this.decided = decided;
	}

	public int getInstance() {
		return instance;
	}

	public Ballot getBallot() {
		return ballot;
	}

	public Proposal getAcceptedProposal() {
		return acceptedProposal;
	}

	public boolean isDecided() {
		return decided;
	}
}
