package asd.protocols.multipaxos.messages;

import asd.paxos.Ballot;
import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public class AcceptOk extends ProtoMessage {
	public static short ID = Agreement.ID + 2;

	private final int instance;
	private final Ballot ballot;

	public AcceptOk(int instance, Ballot ballot) {
		super(ID);

		this.instance = instance;
		this.ballot = ballot;
	}

	public int getInstance() {
		return instance;
	}

	public Ballot getBallot() {
		return ballot;
	}
}
