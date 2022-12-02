package asd.protocols.paxos.messages;

import java.io.IOException;
import asd.paxos2.Ballot;
import asd.protocols.paxos.PaxosBabel;
import asd.protocols.paxos.PaxosProtocol;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class AcceptOkMessage extends ProtoMessage {
    public static final short ID = PaxosProtocol.ID + 1;

    public final int instance;
    public final Ballot ballot;

    public AcceptOkMessage(int instance, Ballot ballot) {
        super(ID);

        this.instance = instance;
        this.ballot = ballot;
    }

    public static final ISerializer<AcceptOkMessage> serializer = new ISerializer<AcceptOkMessage>() {
        @Override
        public void serialize(AcceptOkMessage acceptOkMessage, ByteBuf out) throws IOException {
            out.writeInt(acceptOkMessage.instance);
            PaxosBabel.ballotSerializer.serialize(acceptOkMessage.ballot, out);
        }

        @Override
        public AcceptOkMessage deserialize(ByteBuf in) throws IOException {
            int instance = in.readInt();
            Ballot ballot = PaxosBabel.ballotSerializer.deserialize(in);
            return new AcceptOkMessage(instance, ballot);
        }
    };

}
