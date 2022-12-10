package asd.paxos;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaxosConfig {
    public static class Builder {
        private int initialSlot = 0;
        private Duration timeout = Duration.ofSeconds(1);
        private List<ProcessId> proposers = Collections.emptyList();
        private List<ProcessId> acceptors = Collections.emptyList();
        private List<ProcessId> learners = Collections.emptyList();

        public Builder withInitialSlot(int initialSlot) {
            this.initialSlot = initialSlot;
            return this;
        }

        public Builder withMajorityTimeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder withProposers(List<ProcessId> proposers) {
            this.proposers = proposers;
            return this;
        }

        public Builder withAcceptors(List<ProcessId> acceptors) {
            this.acceptors = acceptors;
            return this;
        }

        public Builder withLearners(List<ProcessId> learners) {
            this.learners = learners;
            return this;
        }

        public Builder withMembership(Membership membership) {
            this.proposers = new ArrayList<>(membership.proposers);
            this.acceptors = new ArrayList<>(membership.acceptors);
            this.learners = new ArrayList<>(membership.learners);
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
            List<ProcessId> proposers,
            List<ProcessId> acceptors,
            List<ProcessId> learners) {
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
