package asd.protocols.agreement.notifications;

import java.util.Collections;
import java.util.List;

import asd.protocols.agreement.Agreement;
import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;
import pt.unl.fct.di.novasys.network.data.Host;

public class LeaderChanged extends ProtoNotification {

    public static final short ID = Agreement.ID + 3;

    // The new leader
    public final Host leader;

    // Commands that were not yet decided and should be forwarded to the new leader
    public final List<byte[]> commands;

    public LeaderChanged(Host leader, List<byte[]> commands) {
        super(ID);
        this.leader = leader;
        this.commands = Collections.unmodifiableList(List.copyOf(commands));
    }

    @Override
    public String toString() {
        return "LeaderChanged [leader=" + leader + ", commands=" + commands.size() + "]";
    }
}
