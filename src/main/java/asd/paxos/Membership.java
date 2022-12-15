package asd.paxos;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Membership {
    public final Set<ProcessId> proposers;
    public final Set<ProcessId> acceptors;
    public final Set<ProcessId> learners;

    public Membership(Collection<ProcessId> members) {
        this(members, members, members);
    }

    public Membership(
            Collection<ProcessId> proposers,
            Collection<ProcessId> acceptors,
            Collection<ProcessId> learners) {
        this.proposers = Collections.unmodifiableSet(Set.copyOf(proposers));
        this.acceptors = Collections.unmodifiableSet(Set.copyOf(acceptors));
        this.learners = Collections.unmodifiableSet(Set.copyOf(learners));
    }

    public Membership with(ProcessId processId) {
        var membership = this.without(processId);
        var proposers = new HashSet<>(membership.proposers);
        proposers.add(processId);
        var acceptors = new HashSet<>(membership.acceptors);
        acceptors.add(processId);
        var learners = new HashSet<>(membership.learners);
        learners.add(processId);
        return new Membership(proposers, acceptors, learners);
    }

    public Membership without(ProcessId processId) {
        var proposers = this.proposers.stream().filter(p -> !p.equals(processId)).toList();
        var acceptors = this.acceptors.stream().filter(p -> !p.equals(processId)).toList();
        var learners = this.learners.stream().filter(p -> !p.equals(processId)).toList();
        return new Membership(proposers, acceptors, learners);
    }

    @Override
    public String toString() {
        return "Membership [proposers=" + proposers + ", acceptors=" + acceptors + ", learners=" + learners + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((proposers == null) ? 0 : proposers.hashCode());
        result = prime * result + ((acceptors == null) ? 0 : acceptors.hashCode());
        result = prime * result + ((learners == null) ? 0 : learners.hashCode());
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
        Membership other = (Membership) obj;
        if (proposers == null) {
            if (other.proposers != null)
                return false;
        } else if (!proposers.equals(other.proposers))
            return false;
        if (acceptors == null) {
            if (other.acceptors != null)
                return false;
        } else if (!acceptors.equals(other.acceptors))
            return false;
        if (learners == null) {
            if (other.learners != null)
                return false;
        } else if (!learners.equals(other.learners))
            return false;
        return true;
    }

}
