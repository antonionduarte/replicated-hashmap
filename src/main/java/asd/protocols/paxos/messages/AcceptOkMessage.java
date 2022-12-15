package asd.protocols.paxos.messages;

import java.io.IOException;

import asd.paxos.Ballot;
import asd.protocols.PaxosBabel;
import asd.protocols.paxos.PaxosProtocol;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class AcceptOkMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID + 1;

    public final int slot;
    public final Ballot ballot;

    public AcceptOkMessage(int slot, Ballot ballot) {
        super(ID);

        this.slot = slot;
        this.ballot = ballot;
    }

    public static final ISerializer<AcceptOkMessage> serializer = new ISerializer<AcceptOkMessage>() {
        @Override
        public void serialize(AcceptOkMessage acceptOkMessage, ByteBuf out) throws IOException {
            out.writeInt(acceptOkMessage.slot);
            PaxosBabel.ballotSerializer.serialize(acceptOkMessage.ballot, out);
        }

        @Override
        public AcceptOkMessage deserialize(ByteBuf in) throws IOException {
            int slot = in.readInt();
            Ballot ballot = PaxosBabel.ballotSerializer.deserialize(in);
            return new AcceptOkMessage(slot, ballot);
        }
    };

}
