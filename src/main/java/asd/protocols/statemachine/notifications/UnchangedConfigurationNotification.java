package asd.protocols.statemachine.notifications;

import asd.protocols.statemachine.StateMachine;
import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;

public class UnchangedConfigurationNotification extends ProtoNotification {

    public static final short ID = StateMachine.ID + 3;

    public final int instance;

    public UnchangedConfigurationNotification(int instance) {
        super(ID);
        this.instance = instance;
    }
}
