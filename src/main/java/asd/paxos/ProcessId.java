package asd.paxos;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.ISerializer;

public class ProcessId implements Comparable<ProcessId> {
    private final long id;

    public ProcessId(long id) {
        this.id = id;
    }

    public static final ISerializer<ProcessId> serializer = new ISerializer<ProcessId>() {
        @Override
        public void serialize(ProcessId processId, ByteBuf out) throws IOException {
            out.writeLong(processId.id);
        }

        @Override
        public ProcessId deserialize(ByteBuf in) throws IOException {
            return new ProcessId(in.readLong());
        }
    };

    @Override
    public int compareTo(ProcessId o) {
        return Long.compare(this.id, o.id);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
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
        ProcessId other = (ProcessId) obj;
        if (id != other.id)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ProcessId [id=" + id + "]";
    }

}
