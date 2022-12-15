package asd.protocols.statemachine.messages;

import asd.protocols.statemachine.StateMachine;
import asd.protocols.statemachine.commands.Command;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class OrderCommand extends ProtoMessage {

    public static final short ID = StateMachine.ID + 3;

    public final Command command;

    public OrderCommand(Command command) {
        super(ID);
        this.command = command;
    }

    public static ISerializer<OrderCommand> serializer = new ISerializer<OrderCommand>() {
        @Override
        public void serialize(OrderCommand orderCommand, ByteBuf out) {
            var data = orderCommand.command.toBytes();
            out.writeInt(data.length);
            out.writeBytes(data);
        }

        @Override
        public OrderCommand deserialize(ByteBuf in) {
            var size = in.readInt();
            var data = new byte[size];
            in.readBytes(data);
            return new OrderCommand(Command.fromBytes(data).getBatch());
        }
    };
}
