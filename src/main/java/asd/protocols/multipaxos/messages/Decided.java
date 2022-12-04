package asd.protocols.multipaxos.messages;

import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public class Decided extends ProtoMessage {
	public static short ID = Agreement.ID + 4;

	public Decided() {
		super(ID);
	}
}
