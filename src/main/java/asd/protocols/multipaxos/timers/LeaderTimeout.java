package asd.protocols.multipaxos.timers;

import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class LeaderTimeout extends ProtoTimer {

	public static final short ID = Agreement.ID + 1;

	public LeaderTimeout() {
		super(ID);
	}

	@Override
	public ProtoTimer clone() {
		return this;
	}
}
