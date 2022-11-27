package asd.paxos.proposal;

public class Proposal {
    public final ProposalNumber number;
    public final ProposalValue value;

    public Proposal(ProposalNumber messageNumber, ProposalValue value) {
        this.number = messageNumber;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Proposal [number=" + number + ", value=" + value + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((number == null) ? 0 : number.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        Proposal other = (Proposal) obj;
        if (number == null) {
            if (other.number != null)
                return false;
        } else if (!number.equals(other.number))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

}
