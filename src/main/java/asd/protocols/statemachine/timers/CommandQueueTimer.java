package asd.protocols.statemachine.timers;

import asd.protocols.statemachine.StateMachine;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class CommandQueueTimer extends ProtoTimer {
    public static final short ID = StateMachine.ID + 3;

    public CommandQueueTimer() {
        super(ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
