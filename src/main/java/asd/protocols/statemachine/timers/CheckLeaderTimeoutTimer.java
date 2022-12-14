package asd.protocols.statemachine.timers;

import asd.protocols.statemachine.StateMachine;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class CheckLeaderTimeoutTimer extends ProtoTimer {
    public static final short ID = StateMachine.ID + 5;

    public CheckLeaderTimeoutTimer() {
        super(ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }

}
