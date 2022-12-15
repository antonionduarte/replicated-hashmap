package asd.protocols.agreement.notifications;

import java.util.List;

import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;
import pt.unl.fct.di.novasys.network.data.Host;

public class JoinedNotification extends ProtoNotification {
    public static final short ID = PaxosProtocol.ID + 2;

    public final int joinSlot;
    public final List<Host> membership;

    public JoinedNotification(int joinInstance, List<Host> membership) {
        super(ID);

        this.joinSlot = joinInstance;
        this.membership = membership;
    }

    @Override
    public String toString() {
        return "JoinedNotification [joinSlot=" + joinSlot + ", membership=" + membership + "]";
    }
}
