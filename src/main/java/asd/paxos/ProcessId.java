package asd.paxos;

public class ProcessId implements Comparable<ProcessId> {
    private final long id;

    public ProcessId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @Override
    public int compareTo(ProcessId o) {
        if (o == null)
            return 1;
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
