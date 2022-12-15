package asd.paxos.multi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import asd.paxos.CommandQueue;
import asd.paxos.Configurations;
import asd.paxos.Membership;
import asd.paxos.Paxos;
import asd.paxos.PaxosCmd;
import asd.paxos.PaxosConfig;
import asd.paxos.ProcessId;
import asd.paxos.ProposalValue;
import asd.slog.SLog;
import asd.slog.SLogger;

public class MultiPaxos implements Paxos {
    private static final Logger logger = LogManager.getLogger(MultiPaxos.class);
    private static final SLogger slogger = SLog.logger(MultiPaxos.class);

    private final ProcessId id;
    private final PaxosConfig config;
    private final Configurations configurations;
    private final CommandQueue input;
    private final CommandQueue preprocess;
    private final CommandQueue output;
    private final ArrayDeque<ProposalValue> proposalQueue;

    private final Proposer proposer;
    private final Acceptor acceptor;
    private final Learner learner;

    private Optional<ProcessId> currentLeader;

    public MultiPaxos(ProcessId processId, PaxosConfig config) {
        this.id = processId;
        this.config = config;
        this.configurations = new Configurations();
        this.input = new CommandQueue();
        this.preprocess = new CommandQueue();
        this.output = new CommandQueue();
        this.proposalQueue = new ArrayDeque<>();

        this.proposer = new Proposer(processId, this.preprocess, config, this.configurations);
        this.acceptor = new Acceptor(processId, this.preprocess, this.configurations);
        this.learner = new Learner(processId, this.preprocess);

        this.currentLeader = Optional.empty();

        this.configurations.set(config.initialSlot, config.membership);
    }

    @Override
    public void push(PaxosCmd... commands) {
        for (var cmd : commands)
            this.input.push(cmd);
        this.execute();
    }

    @Override
    public boolean isEmpty() {
        return this.output.isEmpty();
    }

    @Override
    public PaxosCmd pop() {
        return this.output.pop();
    }

    @Override
    public Membership membership(int slot) {
        return this.configurations.get(slot);
    }

    private void execute() {
        while (!this.input.isEmpty()) {
            while (!this.input.isEmpty()) {
                var command = this.input.pop();
                this.process(command);
            }
            this.tryPropose();
            this.preprocess();
        }

        assert this.input.isEmpty();
        assert this.preprocess.isEmpty();
    }

    private void process(PaxosCmd cmd) {
        switch (cmd.getKind()) {
            case ACCEPT_OK -> {
                var command = cmd.getAcceptOk();
                this.proposer.onAcceptOk(command.slot(), command.processId(), command.ballot());
            }
            case ACCEPT_REQUEST -> {
                var command = cmd.getAcceptRequest();
                this.acceptor.onAcceptRequest(command.slot(), command.processId(), command.proposal());
            }
            case CANCEL_TIMER -> throw new IllegalArgumentException("Unexpected CANCEL_TIMER");
            case LEARN -> {
                var command = cmd.getLearn();

                if (this.proposer.getCurrentSlot() == command.slot()) {
                    var proposal = this.proposer.preempt();
                    if (proposal.isPresent() && !proposal.get().equals(command.value()))
                        this.proposalQueue.addFirst(proposal.get());
                }

                if (this.currentLeader.isEmpty()) {
                    // If we join while there is a leader, we need to know who it is
                    // If we don't have a leader and receive a LEARN, we can assume the sender is
                    // the leader.
                    assert !command.processId().equals(this.id);
                    this.currentLeader = Optional.of(command.processId());
                    this.output.push(PaxosCmd.newLeader(command.processId()));
                }

                this.learner.onLearn(command.slot(), command.value());
            }
            case PREPARE_OK -> {
                var command = cmd.getPrepareOk();
                this.proposer.onPrepareOk(
                        command.slot(),
                        command.processId(),
                        command.ballot(),
                        command.accepted());
            }
            case PREPARE_REQUEST -> {
                var command = cmd.getPrepareRequest();
                this.acceptor.onPrepareRequest(command.slot(), command.processId(), command.ballot());
            }
            case SETUP_TIMER -> throw new IllegalArgumentException("Unexpected SETUP_TIMER");
            case TIMER_EXPIRED -> {
                var command = cmd.getTimerExpired();
                this.proposer.onTimer(command.timerId());
            }
            case MEMBERSHIP_DISCOVERED -> {
                var command = cmd.getMembershipDiscovered();
                this.configurations.set(command.slot(), command.membership());
            }
            case MEMBERSHIP_UNCHANGED -> {
                var command = cmd.getMembershipUnchanged();
                this.configurations.set(command.slot() + 1, this.configurations.get(command.slot()));
            }
            case MEMBER_ADDED -> {
                var command = cmd.getMemberAdded();
                var membership = this.configurations.get(command.slot());
                this.configurations.set(command.slot() + 1, membership.with(command.processId()));
            }
            case MEMBER_REMOVED -> {
                var command = cmd.getMemberRemoved();
                var membership = this.configurations.get(command.slot());
                this.configurations.set(command.slot() + 1, membership.without(command.processId()));
            }
            case PROPOSE -> {
                var command = cmd.getPropose();
                if (command.stragety() == PaxosCmd.ProposeStrategy.Return && this.currentLeader.isPresent()
                        && !this.currentLeader.get().equals(this.id)) {
                    this.output.push(PaxosCmd.newLeader(this.currentLeader.get(), List.of(command.command())));
                    return;
                }

                if (!this.configurations.contains(this.proposer.getCurrentSlot())) {
                    logger.debug("Propose issued before membership is known. See `PaxosCmd::Decide` for details.");
                }

                this.proposalQueue.add(new ProposalValue(command.command()));
                slogger.log("queued", "size", this.proposalQueue.size());
                this.tryPropose();
            }
            case DECIDE -> throw new IllegalArgumentException("Unexpected DECIDE");
            case NEW_LEADER -> throw new IllegalArgumentException("Unexpected NEW_LEADER");
            default -> throw new IllegalArgumentException("Unexpected value: " + cmd.getKind());
        }
    }

    private void preprocess() {
        while (!this.preprocess.isEmpty()) {
            var command = this.preprocess.pop();
            switch (command.getKind()) {
                case ACCEPT_OK -> {
                    var cmd = command.getAcceptOk();
                    if (cmd.processId().equals(this.id))
                        this.input.push(command);
                    else
                        this.output.push(command);
                }
                case ACCEPT_REQUEST -> {
                    var cmd = command.getAcceptRequest();
                    if (cmd.processId().equals(this.id))
                        this.input.push(command);
                    else
                        this.output.push(command);
                }
                case LEARN -> {
                    var cmd = command.getLearn();
                    if (cmd.processId().equals(this.id))
                        this.input.push(command);
                    else
                        this.output.push(command);
                }
                case NEW_LEADER -> {
                    var cmd = command.getNewLeader();
                    var pending = new ArrayList<byte[]>(cmd.pending());
                    if (!cmd.leader().equals(this.id)) {
                        var val = this.proposer.preempt();
                        if (val.isPresent())
                            pending.add(val.get().data);
                        while (!this.proposalQueue.isEmpty())
                            pending.add(this.proposalQueue.remove().data);
                    }
                    slogger.log("leader", "pending", pending.size());
                    this.output.push(PaxosCmd.newLeader(cmd.leader(), pending));
                    this.currentLeader = Optional.of(cmd.leader());
                }
                case PREPARE_OK -> {
                    var cmd = command.getPrepareOk();
                    if (cmd.processId().equals(this.id))
                        this.input.push(command);
                    else
                        this.output.push(command);
                }
                case PREPARE_REQUEST -> {
                    var cmd = command.getPrepareRequest();
                    if (cmd.processId().equals(this.id))
                        this.input.push(command);
                    else
                        this.output.push(command);
                }
                default -> this.output.push(command);
            }
        }
    }

    @Override
    public void gc(int slot) {
        var upto = Math.max(0, slot - 1);
        this.configurations.removeUpTo(upto);
        this.acceptor.removeUpTo(upto);
        this.learner.removeUpTo(upto);
    }

    @Override
    public void printDebug() {
    }

    private void tryPropose() {
        if (!this.proposer.canPropose() || this.proposalQueue.isEmpty())
            return;

        var value = this.proposalQueue.remove();
        this.proposer.propose(value);
    }
}
