package asd.paxos.multi;

import asd.paxos.ProcessId;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class MultipaxosConfig {
	public static class Builder {
		private Duration majorityTimeout = Duration.ofSeconds(1);
		private List<ProcessId> proposers = Collections.emptyList();
		private List<ProcessId> acceptors = Collections.emptyList();
		private List<ProcessId> learners = Collections.emptyList();

		public Builder withMajorityTimeout(Duration timeout) {
			this.majorityTimeout = timeout;
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

		public MultipaxosConfig build() {
			return new MultipaxosConfig(this.majorityTimeout, this.proposers, this.acceptors, this.learners);
		}
	}

	public final Duration majorityTimeout;
	public final List<ProcessId> proposers;
	public final List<ProcessId> acceptors;
	public final List<ProcessId> learners;

	public MultipaxosConfig(
			Duration majorityTimeout,
			List<ProcessId> proposers,
			List<ProcessId> acceptors,
			List<ProcessId> learners) {
		this.majorityTimeout = majorityTimeout;
		this.proposers = Collections.unmodifiableList(List.copyOf(proposers));
		this.acceptors = Collections.unmodifiableList(List.copyOf(acceptors));
		this.learners = Collections.unmodifiableList(List.copyOf(learners));
	}

	public MultipaxosConfig() {
		this(Duration.ofSeconds(1), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
	}

	public static Builder builder() {
		return new Builder();
	}
}
