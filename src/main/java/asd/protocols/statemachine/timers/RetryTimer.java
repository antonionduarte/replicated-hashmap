package asd.protocols.statemachine.timers;

import asd.protocols.statemachine.StateMachine;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;
import pt.unl.fct.di.novasys.network.data.Host;

public class RetryTimer extends ProtoTimer {

    public static final short ID = StateMachine.ID + 1;

    public final Host peer;

    public RetryTimer(Host peer) {
        super(ID);

        this.peer = peer;
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
