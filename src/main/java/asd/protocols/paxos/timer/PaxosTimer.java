package asd.protocols.paxos.timer;

import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class PaxosTimer extends ProtoTimer {
    public static final short ID = PaxosProtocol.ID + 1;

    public final int slot;
    public final int timerId;

    public PaxosTimer(int instance, int timerId) {
        super(ID);

        this.slot = instance;
        this.timerId = timerId;
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }

}
