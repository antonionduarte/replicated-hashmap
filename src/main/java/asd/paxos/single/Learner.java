package asd.paxos.single;

import asd.paxos.PaxosLog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.ProcessId;
import asd.paxos.ProposalValue;

class Learner {
    private static final Logger logger = LogManager.getLogger(Learner.class);

    private final ProcessId id;
    private final PaxosIO io;

    private ProposalValue value;

    public Learner(ProcessId id, PaxosIO io) {
        this.id = id;
        this.io = io;

        this.value = null;
    }

    public ProcessId getId() {
        return id;
    }

    public boolean hasDecided() {
        return this.value != null;
    }

    public void onDecide(ProposalValue value) {
        if (this.value == null) {
            PaxosLog.log("learned", "value", value);
            logger.debug("Decided on value {}", value);
            this.value = value;
            this.io.push(PaxosCmd.decided(value));
        } else if (!this.value.equals(value)) {
            PaxosLog.log("decision-conflict");
            throw new IllegalStateException("Two different values were decided");
        } else {
            logger.debug("Ignoring duplicate decide");
            PaxosLog.log("learn-duplicate");
        }
    }
}
