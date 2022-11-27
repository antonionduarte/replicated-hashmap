package asd.paxos.proposal;

import asd.paxos.ProcessId;

public class ProposalNumber {
    public static enum Order {
        LESS,
        EQUAL,
        GREATER
    }

    public final ProcessId processId;
    public final int sequenceNumber;

    public ProposalNumber() {
        this.processId = null;
        this.sequenceNumber = 0;
    }

    public ProposalNumber(long processId, int sequenceNumber) {
        this.processId = new ProcessId(processId);
        this.sequenceNumber = sequenceNumber;
    }

    public ProposalNumber(ProcessId processId, int sequenceNumber) {
        this.processId = processId;
        this.sequenceNumber = sequenceNumber;
    }

    public Order compare(ProposalNumber other) {
        if (this.sequenceNumber < other.sequenceNumber) {
            return Order.LESS;
        } else if (this.sequenceNumber > other.sequenceNumber) {
            return Order.GREATER;
        } else {
            if (this.processId == null)
                return Order.LESS;
            var hostCompare = this.processId.compareTo(other.processId);
            if (hostCompare == 0)
                return Order.EQUAL;
            else if (hostCompare < 0)
                return Order.LESS;
            else
                return Order.GREATER;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((processId == null) ? 0 : processId.hashCode());
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
        return "ProposalNumber [host=" + processId + ", number=" + sequenceNumber + "]";
    }

}
