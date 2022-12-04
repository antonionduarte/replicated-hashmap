package asd.protocols.multipaxos.messages;

import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public class PrepareOk extends ProtoMessage {
	public static short ID = Agreement.ID + 5;

	public PrepareOk() {
		super(ID);
	}
}
