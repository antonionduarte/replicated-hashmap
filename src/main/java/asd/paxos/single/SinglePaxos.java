package asd.paxos.single;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.TreeMap;

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

class SlotState {
    public final int slot;
    public final Proposer proposer;
    public final Acceptor acceptor;
    public final Learner learner;
    public Optional<ProposalValue> originalProposal;

    public SlotState(int slot, ProcessId id, CommandQueue queue, PaxosConfig config) {
        this.slot = slot;
        this.proposer = new Proposer(slot, id, queue, config);
        this.acceptor = new Acceptor(slot, id, queue, config);
        this.learner = new Learner(slot, id, queue);
        this.originalProposal = Optional.empty();
    }

    public boolean canPropose() {
        return this.proposer.canPropose() && !this.learner.hasDecided();
    }

    public boolean isDecided() {
        return this.learner.hasDecided();
    }
}

public class SinglePaxos implements Paxos {

    private static final Logger logger = LogManager.getLogger(SinglePaxos.class);

    private final ProcessId id;
    private final PaxosConfig config;
    private final Configurations configurations;
    private final CommandQueue input;
    private final CommandQueue output;
    private final TreeMap<Integer, SlotState> slots;
    private final ArrayDeque<ProposalValue> proposalQueue;
    private int currentSlot;

    public SinglePaxos(ProcessId processId, PaxosConfig config) {
        this.id = processId;
        this.config = config;
        this.configurations = new Configurations();
        this.input = new CommandQueue();
        this.output = new CommandQueue();
        this.slots = new TreeMap<>();
        this.proposalQueue = new ArrayDeque<>();
        this.currentSlot = config.initialSlot;

        this.configurations.set(config.initialSlot, config.membership);
    }

    private void execute() {
        while (!this.input.isEmpty()) {
            while (!this.input.isEmpty()) {
                var command = this.input.pop();
                this.process(command);
            }
            this.tryPropose();
        }
    }

    private void process(PaxosCmd cmd) {
        switch (cmd.getKind()) {
            case ACCEPT_OK -> {
                var command = cmd.getAcceptOk();
                var state = this.tryGetSlot(command.slot());
                state.proposer.receiveAcceptOk(command.processId(), command.ballot());
            }
            case ACCEPT_REQUEST -> {
                var command = cmd.getAcceptRequest();
                var state = this.tryGetSlot(command.slot());
                state.acceptor.onAcceptRequest(command.processId(), command.proposal());
            }
            case CANCEL_TIMER -> throw new IllegalArgumentException("Unexpected CANCEL_TIMER");
            case LEARN -> {
                var command = cmd.getLearn();
                var state = this.tryGetSlot(command.slot());

                if (state.isDecided())
                    return;

                state.learner.onDecide(command.value());
                state.proposer.moveToDecided();

                if (state.originalProposal.isPresent())
                    if (!command.value().equals(state.originalProposal.get()))
                        this.proposalQueue.addFirst(state.originalProposal.get());
            }
            case PREPARE_OK -> {
                var command = cmd.getPrepareOk();
                var state = this.tryGetSlot(command.slot());
                state.proposer.receivePrepareOk(command.processId(), command.ballot(), command.highestAccept());
            }
            case PREPARE_REQUEST -> {
                var command = cmd.getPrepareRequest();
                var state = this.tryGetSlot(command.slot());
                state.acceptor.onPrepareRequest(command.processId(), command.ballot());
            }
            case SETUP_TIMER -> throw new IllegalArgumentException("Unexpected SETUP_TIMER");
            case TIMER_EXPIRED -> {
                var command = cmd.getTimerExpired();
                this.slots.get(command.slot()).proposer.triggerTimer(command.timerId());
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
                this.proposalQueue.add(new ProposalValue(command.command()));
                this.tryPropose();
            }
            case DECIDE -> throw new IllegalArgumentException("Unexpected DECIDE");
            case NEW_LEADER -> throw new IllegalArgumentException("Unexpected NEW_LEADER");
            default -> throw new IllegalArgumentException("Unexpected value: " + cmd.getKind());
        }
    }

    private void tryPropose() {
        while (this.slots.containsKey(this.currentSlot) && this.slots.get(this.currentSlot).isDecided())
            this.currentSlot += 1;

        var state = this.tryGetSlot(this.currentSlot);
        if (state == null) {
            /*- If the state is null that means that we don't know its membership yet.
            *   We have to wait until we receive a message from another member with the membership
            *   or the state machine tells us that our previous decision did not change the membership. */
            // PaxosLog.log("waiting-for-membership");
            return;
        }

        if (!this.proposalQueue.isEmpty() && state.canPropose()) {
            assert state.originalProposal.isEmpty();
            state.originalProposal = Optional.of(this.proposalQueue.remove());
            logger.debug("Proposing {} on instance {}", state.originalProposal, this.currentSlot);
            state.proposer.propose(state.originalProposal.get());
        }
    }

    private SlotState tryGetSlot(int slot) {
        if (!this.configurations.contains(slot))
            return null;

        var state = this.slots.get(slot);
        if (state != null)
            return state;

        var membership = this.configurations.get(slot);
        if (membership == null)
            return null;

        var config = PaxosConfig.builder(this.config).withMembership(membership).build();
        state = new SlotState(slot, this.id, this.output, config);

        this.slots.put(slot, state);
        return state;
    }

    @Override
    public void push(PaxosCmd cmd) {
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

    public Membership membership(int slot) {
        return this.configurations.get(slot);
    }

}
