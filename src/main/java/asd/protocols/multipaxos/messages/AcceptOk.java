package asd.protocols.multipaxos.messages;

import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public class AcceptOk extends ProtoMessage {
	public static short ID = Agreement.ID + 2;

	public AcceptOk() {
		super(ID);
	}
}
