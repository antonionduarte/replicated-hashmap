package asd.paxos2.single;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import asd.paxos2.ProcessId;

public class PaxosConfig {
    public final Duration majorityTimeout;
    public final List<ProcessId> membership;

    private PaxosConfig(Duration majorityTimeout, List<ProcessId> membership) {
        this.majorityTimeout = majorityTimeout;
        this.membership = membership;
    }

    public PaxosConfig() {
        this(
                Duration.ofSeconds(1),
                List.of());
    }

    /**
     * 
     * @param majorityTimeout
     *            How long to wait for a majority response before
     *            retrying with a higher proposal number.
     */
    public PaxosConfig withMajorityTimeout(Duration majorityTimeout) {
        return new PaxosConfig(majorityTimeout, this.membership);
    }

    /**
     * 
     * @param membership
     *            The list of all processes in the system.
     */
    public PaxosConfig withMembership(List<ProcessId> membership) {
        return new PaxosConfig(this.majorityTimeout, Collections.unmodifiableList(membership));
    }

}
