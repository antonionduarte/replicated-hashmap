package asd.protocols.paxos;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

public class ProposalNumber {
    public static enum Order {
        LESS,
        EQUAL,
        GREATER
    }

    private final Host host;
    private final int sequenceNumber;

    public ProposalNumber() {
        this.host = null;
        this.sequenceNumber = 0;
    }

    public ProposalNumber(Host host, int sequenceNumber) {
        this.host = host;
        this.sequenceNumber = sequenceNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public Order compare(ProposalNumber other) {
        if (this.sequenceNumber < other.sequenceNumber) {
            return Order.LESS;
        } else if (this.sequenceNumber > other.sequenceNumber) {
            return Order.GREATER;
        } else {
            if (this.host == null)
                return Order.LESS;
            var hostCompare = this.host.compareTo(other.host);
            if (hostCompare == 0)
                return Order.EQUAL;
            else if (hostCompare < 0)
                return Order.LESS;
            else
                return Order.GREATER;
        }
    }

    public static final ISerializer<ProposalNumber> serializer = new ISerializer<ProposalNumber>() {
        @Override
        public void serialize(ProposalNumber messageNumber, ByteBuf out) throws IOException {
            Host.serializer.serialize(messageNumber.host, out);
            out.writeInt(messageNumber.sequenceNumber);
        }

        @Override
        public ProposalNumber deserialize(ByteBuf in) throws IOException {
            var host = Host.serializer.deserialize(in);
            var number = in.readInt();
            return new ProposalNumber(host, number);
        }
    };

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + sequenceNumber;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProposalNumber other = (ProposalNumber) obj;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        if (sequenceNumber != other.sequenceNumber)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ProposalNumber [host=" + host + ", number=" + sequenceNumber + "]";
    }

}
