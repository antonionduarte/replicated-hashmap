package asd.protocols.paxos.timer;

import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class MajorityTimeoutTimer extends ProtoTimer {
    public static final short ID = PaxosProtocol.ID + 2;

    public final int instance;

    public MajorityTimeoutTimer(int instance) {
        super(ID);
        this.instance = instance;
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }

}
