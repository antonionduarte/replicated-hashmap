package asd.paxos.single;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import asd.paxos.ProcessId;

public class PaxosConfig {
    public static class Builder {
        private Duration timeout = Duration.ofSeconds(1);
        private List<ProcessId> proposers = Collections.emptyList();
        private List<ProcessId> acceptors = Collections.emptyList();
        private List<ProcessId> learners = Collections.emptyList();

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

        public PaxosConfig build() {
            return new PaxosConfig(this.timeout, this.proposers, this.acceptors, this.learners);
        }
    }

    public final Duration majorityTimeout;
    public final List<ProcessId> proposers;
    public final List<ProcessId> acceptors;
    public final List<ProcessId> learners;

    private PaxosConfig(
            Duration majorityTimeout,
            List<ProcessId> proposers,
            List<ProcessId> acceptors,
            List<ProcessId> learners) {
        this.majorityTimeout = majorityTimeout;
        this.proposers = Collections.unmodifiableList(List.copyOf(proposers));
        this.acceptors = Collections.unmodifiableList(List.copyOf(acceptors));
        this.learners = Collections.unmodifiableList(List.copyOf(learners));
    }

    public PaxosConfig() {
        this(Duration.ofSeconds(1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    public static Builder builder() {
        return new PaxosConfig.Builder();
    }

    public static Builder builder(PaxosConfig initial) {
        return new PaxosConfig.Builder()
                .withMajorityTimeout(initial.majorityTimeout)
                .withProposers(initial.proposers)
                .withAcceptors(initial.acceptors)
                .withLearners(initial.learners);
    }

}
