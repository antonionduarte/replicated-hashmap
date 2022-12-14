package asd.protocols.statemachine.timers;

import asd.protocols.statemachine.StateMachine;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class ProposeNoopTimer extends ProtoTimer {
    public static final short ID = StateMachine.ID + 6;

    public ProposeNoopTimer() {
        super(ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
