package asd.paxos.single2;

import asd.paxos.CommandQueue;
import asd.paxos.PaxosCmd;
import asd.paxos.PaxosLog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.ProcessId;
import asd.paxos.ProposalValue;

class Learner {
    private static final Logger logger = LogManager.getLogger(Learner.class);

    private final int slot;
    private final ProcessId id;
    private final CommandQueue queue;

    private ProposalValue value;

    public Learner(int slot, ProcessId id, CommandQueue queue) {
        this.slot = slot;
        this.id = id;
        this.queue = queue;

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
            this.queue.push(PaxosCmd.decide(this.slot, value));
        } else if (!this.value.equals(value)) {
            PaxosLog.log("decision-conflict");
            throw new IllegalStateException("Two different values were decided");
        } else {
            logger.debug("Ignoring duplicate decide");
            PaxosLog.log("learn-duplicate");
        }
    }
}
