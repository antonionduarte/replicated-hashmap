package asd.protocols.multipaxos.messages;

import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public class PrepareRequest extends ProtoMessage {
	public static short ID = Agreement.ID + 6;
	public PrepareRequest() {
		super(ID);
	}
}
