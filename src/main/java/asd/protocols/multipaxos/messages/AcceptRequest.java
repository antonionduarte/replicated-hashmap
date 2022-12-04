package asd.protocols.multipaxos.messages;

import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public class AcceptRequest extends ProtoMessage {
	public static short ID = Agreement.ID + 3;

	public AcceptRequest() {
		super(ID);
	}
}
