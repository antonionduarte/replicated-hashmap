package asd.paxos;

public class ProposalSlot {
    public final int slot;
    public final Proposal proposal;

    public ProposalSlot(int slot, Proposal proposal) {
        this.slot = slot;
        this.proposal = proposal;
    }

    @Override
    public String toString() {
        return "ProposalSlot [slot=" + slot + ", proposal=" + proposal + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + slot;
        result = prime * result + ((proposal == null) ? 0 : proposal.hashCode());
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
        ProposalSlot other = (ProposalSlot) obj;
        if (slot != other.slot)
            return false;
        if (proposal == null) {
            if (other.proposal != null)
                return false;
        } else if (!proposal.equals(other.proposal))
            return false;
        return true;
    }
}
