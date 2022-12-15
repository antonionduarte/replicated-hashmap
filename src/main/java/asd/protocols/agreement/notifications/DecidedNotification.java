package asd.protocols.agreement.notifications;

import asd.AsdUtils;
import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;

public class DecidedNotification extends ProtoNotification {
    public static final short ID = Agreement.ID + 1;

    public final int instance;
    public final byte[] operation;

    public DecidedNotification(int instance, byte[] operation) {
        super(ID);

        this.instance = instance;
        this.operation = operation;
    }

    @Override
    public String toString() {
        return "DecidedNotification [instance=" + instance + ", operation=" + AsdUtils.sha256Hex(this.operation) + "]";
    }

}
