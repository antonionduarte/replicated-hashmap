package asd.protocols.statemachine.timers;

import asd.protocols.statemachine.StateMachine;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

// Triggered when the state machine should check if any of the batches it sent to the leader have timed out.
public class OrderBatchTimer extends ProtoTimer {

    public static final short ID = StateMachine.ID + 4;

    public OrderBatchTimer() {
        super(ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }

}
