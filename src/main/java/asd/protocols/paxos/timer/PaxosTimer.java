package asd.protocols.paxos.timer;

import asd.protocols.paxos.PaxosProtocol2;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class PaxosTimer extends ProtoTimer {
    public static final short ID = PaxosProtocol2.ID + 1;

    public final int instance;
    public final int timerId;

    public PaxosTimer(int instance, int timerId) {
        super(ID);

        this.instance = instance;
        this.timerId = timerId;
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }

}
