package asd.paxos.multi;

import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.CommandQueue;
import asd.paxos.PaxosCmd;
import asd.paxos.ProcessId;
import asd.paxos.ProposalValue;
import asd.slog.SLog;
import asd.slog.SLogger;

class Learner {
    private static final Logger logger = LogManager.getLogger(Learner.class);
    private static final SLogger slogger = SLog.logger(Learner.class);

    private final ProcessId id;
    private final CommandQueue queue;

    private TreeMap<Integer, ProposalValue> values;

    public Learner(ProcessId id, CommandQueue queue) {
        this.id = id;
        this.queue = queue;

        this.values = new TreeMap<>();
    }

    public ProcessId getId() {
        return id;
    }

    public boolean hasDecided(int slot) {
        return this.values.containsKey(slot);
    }

    public int lowestUnknownSlot() {
        if (this.values.isEmpty())
            return 0;
        if (this.values.lastKey() - this.values.firstKey() == this.values.size())
            return this.values.lastKey() + 1;
        for (int i = this.values.firstKey(); i <= this.values.lastKey(); i++)
            if (!this.values.containsKey(i))
                return i;
        throw new IllegalStateException("Should not happen");
    }

    public void onLearn(int slot, ProposalValue value) {
        var current = this.values.get(slot);
        slogger.log("learned",
                "slot", slot,
                "value", value);
        if (current == null) {
            slogger.log("decided",
                    "slot", slot,
                    "value", value);
            logger.debug("Decided on value {}", value);
            this.values.put(slot, value);
            this.queue.push(PaxosCmd.decide(slot, value));
        } else if (!current.equals(value)) {
            slogger.log("conflict", "slot", slot);
            throw new IllegalStateException("Two different values were decided");
        } else {
            logger.debug("Ignoring duplicate decide");
            slogger.log("learn-duplicate", "slot", slot);
        }
    }

    public void removeUpTo(int slot) {
        this.values.headMap(slot).clear();
    }
}
