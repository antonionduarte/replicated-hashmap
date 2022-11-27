package asd.protocols.paxos.notifications;

import java.util.List;

import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;
import pt.unl.fct.di.novasys.network.data.Host;

public class JoinedNotification extends ProtoNotification {
    public static final short ID = PaxosProtocol.ID + 2;

    public final int joinInstance;
    public final List<Host> membership;

    public JoinedNotification(int joinInstance, List<Host> membership) {
        super(ID);

        this.joinInstance = joinInstance;
        this.membership = membership;
    }

    @Override
    public String toString() {
        return "JoinedNotification [joinInstance=" + joinInstance + ", membership=" + membership + "]";
    }
}
