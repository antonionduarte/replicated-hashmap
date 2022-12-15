package asd.paxos;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PaxosConfig {
    public static class Builder {
        private int initialSlot = 0;
        private Duration timeout = Duration.ofSeconds(1);
        private Collection<ProcessId> proposers = Collections.emptyList();
        private Collection<ProcessId> acceptors = Collections.emptyList();
        private Collection<ProcessId> learners = Collections.emptyList();

        public Builder withInitialSlot(int initialSlot) {
            this.initialSlot = initialSlot;
            return this;
        }

        public Builder withMajorityTimeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder withProposers(Collection<ProcessId> proposers) {
            this.proposers = proposers;
            return this;
        }

        public Builder withAcceptors(Collection<ProcessId> acceptors) {
            this.acceptors = acceptors;
            return this;
        }

        public Builder withLearners(Collection<ProcessId> learners) {
            this.learners = learners;
            return this;
        }

        public Builder withMembership(Membership membership) {
            this.proposers = List.copyOf(membership.proposers);
            this.acceptors = List.copyOf(membership.acceptors);
            this.learners = List.copyOf(membership.learners);
            return this;
        }

        public PaxosConfig build() {
            return new PaxosConfig(this.initialSlot, this.timeout, this.proposers, this.acceptors, this.learners);
        }
    }

    public final int initialSlot;
    public final Duration majorityTimeout;
    public final Membership membership;

    private PaxosConfig(
            int initialSlot,
            Duration majorityTimeout,
            Collection<ProcessId> proposers,
            Collection<ProcessId> acceptors,
            Collection<ProcessId> learners) {
        this.initialSlot = initialSlot;
        this.majorityTimeout = majorityTimeout;
        this.membership = new Membership(proposers, acceptors, learners);
    }

    public PaxosConfig() {
        this(0, Duration.ofSeconds(1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    public static Builder builder() {
        return new PaxosConfig.Builder();
    }

    public static Builder builder(PaxosConfig initial) {
        return new PaxosConfig.Builder()
                .withInitialSlot(initial.initialSlot)
                .withMajorityTimeout(initial.majorityTimeout)
                .withProposers(initial.membership.proposers)
                .withAcceptors(initial.membership.acceptors)
                .withLearners(initial.membership.learners);
    }

}
