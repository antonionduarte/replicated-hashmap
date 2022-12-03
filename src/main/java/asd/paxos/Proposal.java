package asd.paxos;

public class Proposal {
    public final Ballot ballot;
    public final int slot;
    public final ProposalValue value;

    public Proposal(Ballot ballot, int slot, ProposalValue value) {
        this.ballot = ballot;
        this.slot = slot;
        this.value = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ballot == null) ? 0 : ballot.hashCode());
        result = prime * result + (int) (slot ^ (slot >>> 32));
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
        if (ballot == null) {
            if (other.ballot != null)
                return false;
        } else if (!ballot.equals(other.ballot))
            return false;
        if (slot != other.slot)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Proposal [ballot=" + ballot + ", slot=" + slot + ", value=" + value + "]";
    }
}
