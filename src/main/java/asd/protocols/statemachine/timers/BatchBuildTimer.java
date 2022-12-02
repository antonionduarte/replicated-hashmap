package asd.protocols.statemachine.timers;

import asd.protocols.statemachine.StateMachine;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class BatchBuildTimer extends ProtoTimer {
    public static final short ID = StateMachine.ID + 2;

    public BatchBuildTimer() {
        super(ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
