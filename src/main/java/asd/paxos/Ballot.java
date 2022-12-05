package asd.paxos;

public class Ballot implements Comparable<Ballot> {
    public static enum Order {
        LESS, EQUAL, GREATER
    }

    public final ProcessId processId;
    public final long sequenceNumber;

    public Ballot() {
        this.processId = null;
        this.sequenceNumber = 0;
    }

    public Ballot(long processId, long sequenceNumber) {
        this.processId = new ProcessId(processId);
        this.sequenceNumber = sequenceNumber;
    }

    public Ballot(ProcessId processId, long sequenceNumber) {
        this.processId = processId;
        this.sequenceNumber = sequenceNumber;
    }

    public Ballot withIncSeqNumber() {
        return this.withIncSeqNumber(1);
    }

    public Ballot withIncSeqNumber(long inc) {
        return new Ballot(this.processId, this.sequenceNumber + inc);
    }

    public Ballot max(Ballot other) {
        if (this.compare(other) == Order.GREATER)
            return this;
        else
            return other;
    }

    public Order compare(Ballot other) {
        var number = this.compareTo(other);
        if (number == 0)
            return Order.EQUAL;
        else if (number < 0)
            return Order.LESS;
        else
            return Order.GREATER;
    }

    @Override
    public int compareTo(Ballot other) {
        if (this.sequenceNumber < other.sequenceNumber) {
            return -1;
        } else if (this.sequenceNumber > other.sequenceNumber) {
            return 1;
        } else {
            if (this.processId == null)
                return -1;
            return this.processId.compareTo(other.processId);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((processId == null) ? 0 : processId.hashCode());
        result = prime * result + (int) (sequenceNumber ^ (sequenceNumber >>> 32));
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
        Ballot other = (Ballot) obj;
        if (processId == null) {
            if (other.processId != null)
                return false;
        } else if (!processId.equals(other.processId))
            return false;
        if (sequenceNumber != other.sequenceNumber)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Ballot [processId=" + processId + ", sequenceNumber=" + sequenceNumber + "]";
    }
}
