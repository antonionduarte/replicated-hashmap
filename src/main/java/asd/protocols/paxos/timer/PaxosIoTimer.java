package asd.protocols.paxos.timer;

import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class PaxosIoTimer extends ProtoTimer {
    public static final short ID = PaxosProtocol.ID + 1;

    public final int instance;
    public final int timerId;

    public PaxosIoTimer(int instance, int timerId) {
        super(ID);

        this.instance = instance;
        this.timerId = timerId;
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }

}
