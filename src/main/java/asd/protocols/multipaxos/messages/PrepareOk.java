package asd.protocols.multipaxos.messages;

import asd.paxos.Ballot;
import asd.paxos.multi.Proposal;
import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public class PrepareOk extends ProtoMessage {
	public static short ID = Agreement.ID + 5;

	private final Ballot ballot;
	private final Proposal acceptedProposal;
	private final boolean decided;

	public PrepareOk(Ballot ballot, Proposal acceptedProposal, boolean decided) {
		super(ID);

		this.ballot = ballot;
		this.acceptedProposal = acceptedProposal;
		this.decided = decided;
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
