package asd.protocols.paxos.timer;

import asd.protocols.paxos.PaxosProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class ForceProposalTimer extends ProtoTimer {

    public static final short ID = PaxosProtocol.ID + 1;

    public final int instance;

    public ForceProposalTimer(int instance) {
        super(ID);
        this.instance = instance;
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
