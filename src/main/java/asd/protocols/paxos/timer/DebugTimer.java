package asd.protocols.paxos.timer;

import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class DebugTimer extends ProtoTimer {
    public static final short ID = PaxosProtocol.ID + 2;

    public DebugTimer() {
        super(ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }

}
