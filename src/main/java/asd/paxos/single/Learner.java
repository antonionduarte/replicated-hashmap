package asd.paxos.single;

import asd.paxos.CommandQueue;
import asd.paxos.PaxosCmd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.ProcessId;
import asd.paxos.ProposalValue;
import asd.slog.SLog;
import asd.slog.SLogger;

class Learner {
    private static final Logger logger = LogManager.getLogger(Learner.class);
    private static final SLogger slogger = SLog.logger(Learner.class);

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
            slogger.log("learned", "slot", this.slot, "value", value);
            logger.debug("Decided on value {}", value);
            this.value = value;
            this.queue.push(PaxosCmd.decide(this.slot, value));
        } else if (!this.value.equals(value)) {
            slogger.log("decision-conflict", "slot", this.slot);
            throw new IllegalStateException("Two different values were decided");
        } else {
            logger.debug("Ignoring duplicate decide");
            slogger.log("learn-duplicate", "slot", this.slot);
        }
    }
}
