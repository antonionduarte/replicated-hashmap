package asd.paxos;

import java.time.Duration;

public class PaxosConfig {
    final Duration majorityTimeout;

    /**
     * 
     * @param majorityTimeout How long to wait for a majority response before
     *                        retrying with a higher proposal number.
     */
    public PaxosConfig(Duration majorityTimeout) {
        this.majorityTimeout = majorityTimeout;
    }

    public PaxosConfig() {
        this(Duration.ofSeconds(1));
    }
}
