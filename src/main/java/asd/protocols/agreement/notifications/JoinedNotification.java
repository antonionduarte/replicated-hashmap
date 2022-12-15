package asd.protocols.agreement.notifications;

import java.util.List;

import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;
import pt.unl.fct.di.novasys.network.data.Host;

public class JoinedNotification extends ProtoNotification {
    public static final short ID = Agreement.ID + 2;

    // The slot at which the replica should start participating.
    // The replica was added to the system in the slot before this one.
    public final int slot;

    // The membership at the slot at which the replica was added to the system with
    // the current replica.
    public final List<Host> membership;

    public JoinedNotification(int slot, List<Host> membership) {
        super(ID);

        this.slot = slot;
        this.membership = membership;
    }

    @Override
    public String toString() {
        return "JoinedNotification [slot=" + slot + ", membership=" + membership + "]";
    }
}
