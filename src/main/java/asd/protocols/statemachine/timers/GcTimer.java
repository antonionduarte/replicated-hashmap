package asd.protocols.statemachine.timers;

import asd.protocols.statemachine.StateMachine;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class GcTimer extends ProtoTimer {
    public static final short ID = StateMachine.ID + 7;

    public GcTimer() {
        super(ID);
    }

    @Override
    public GcTimer clone() {
        return this;
    }
}
