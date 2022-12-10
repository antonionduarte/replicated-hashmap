package asd.protocols.statemachine.messages;

import asd.protocols.statemachine.StateMachine;
import asd.protocols.statemachine.commands.Batch;
import asd.protocols.statemachine.commands.Command;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class OrderBatch extends ProtoMessage {

    public static final short ID = StateMachine.ID + 3;

    public final Batch batch;

    public OrderBatch(Batch batch) {
        super(ID);
        this.batch = batch;
    }

    public static ISerializer<OrderBatch> serializer = new ISerializer<OrderBatch>() {
        @Override
        public void serialize(OrderBatch orderBatch, ByteBuf out) {
            var data = orderBatch.batch.toBytes();
            out.writeInt(data.length);
            out.writeBytes(data);
        }

        @Override
        public OrderBatch deserialize(ByteBuf in) {
            var size = in.readInt();
            var data = new byte[size];
            in.readBytes(data);
            return new OrderBatch(Command.fromBytes(data).getBatch());
        }
    };
}
