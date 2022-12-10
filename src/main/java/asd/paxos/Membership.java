package asd.paxos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Membership {
    public final List<ProcessId> proposers;
    public final List<ProcessId> acceptors;
    public final List<ProcessId> learners;

    public Membership(List<ProcessId> members) {
        this(members, members, members);
    }

    public Membership(List<ProcessId> proposers, List<ProcessId> acceptors, List<ProcessId> learners) {
        this.proposers = Collections.unmodifiableList(List.copyOf(proposers));
        this.acceptors = Collections.unmodifiableList(List.copyOf(acceptors));
        this.learners = Collections.unmodifiableList(List.copyOf(learners));
    }

    public Membership with(ProcessId processId) {
        var membership = this.without(processId);
        var proposers = new ArrayList<>(membership.proposers);
        proposers.add(processId);
        var acceptors = new ArrayList<>(membership.acceptors);
        acceptors.add(processId);
        var learners = new ArrayList<>(membership.learners);
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
