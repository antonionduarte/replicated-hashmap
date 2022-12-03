package asd.protocols.statemachine.commands;

import pt.unl.fct.di.novasys.network.data.Host;

import java.io.*;
import java.net.InetAddress;

public class Join extends Command {

    public final Host host;

    protected Join(Host host) {
        super(Kind.JOIN);
        this.host = host;
    }

    protected Join(byte[] bytes) throws IOException {
        super(Kind.JOIN);
        var dis = new DataInputStream(new ByteArrayInputStream(bytes));
        var port = dis.readInt();
        var address = InetAddress.getByAddress(dis.readAllBytes());
        this.host = new Host(address, port);
    }

    @Override
    public byte[] toBytes() {
        var baos = new ByteArrayOutputStream();
        var dos = new DataOutputStream(baos);
        try {
            dos.write(super.toBytes());
            dos.writeInt(host.getPort());
            dos.write(host.getAddress().getAddress());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return baos.toByteArray();
    }
}
