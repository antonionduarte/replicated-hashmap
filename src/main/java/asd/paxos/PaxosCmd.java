package asd.paxos;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaxosCmd {
    public static enum Kind {
        NEW_LEADER, DECIDE, MEMBER_ADDED, MEMBER_REMOVED, MEMBERSHIP_UNCHANGED, MEMBERSHIP_DISCOVERED, PROPOSE, PREPARE_REQUEST, PREPARE_OK, ACCEPT_REQUEST, ACCEPT_OK, LEARN, SETUP_TIMER, CANCEL_TIMER, TIMER_EXPIRED,
    }

    /**
     * A value has been decided.
     * 
     * @apiNote Origin: Paxos.
     *          This is only issued once per slot.
     *          The user is responsible for issuing one of the Membership commands
     *          to ensure that the membership for the next slot is known.
     *          Not doing so will cause the system to stall.
     * @see MemberAdded MemberAdded MemberRemoved MembershipUnchanged
     * 
     * @param slot
     *            The slot at which the value has been decided.
     * @param value
     *            The decided value.
     */
    public static record Decide(int slot, ProposalValue value) {
    }

    /**
     * A new leader has been elected. All pending commands should be forwarded to
     * this new leader. If the leader is the local process then the pending list is
     * empty as the commands don't need to be forwarded.
     * 
     * @apiNote Origin: Paxos
     * 
     * @param leader
     *            The id of the new leader.
     * @param pending
     *            The list of pending commands that should be proposed to the new
     *            leader.
     * 
     */
    public static record NewLeader(ProcessId leader, List<byte[]> pending) {
    }

    /**
     * A new member has been added to the membership at the given slot.
     * The new member will start participating at slot `slot`.
     * It is invalid to issue this command after a Propose command for the same
     * slot.
     * 
     * @apiNote Origin: User
     * 
     * @param slot
     *            The slot at which the member starts participating.
     * @param processId
     *            The id of the new member.
     */
    public static record MemberAdded(int slot, ProcessId processId) {
    }

    /**
     * A member has been removed from the membership at the given slot.
     * 
     * @apiNote Origin: User
     * 
     * @param slot
     *            The slot at which the member stops participating.
     */
    public static record MemberRemoved(int slot, ProcessId processId) {
    }

    /**
     * The membership has not changed at the given slot.
     * 
     * @apiNote Origin: User
     * 
     * @param slot
     *            The slot at which the membership has not changed.
     */
    public static record MembershipUnchanged(int slot) {
    }

    /**
     * The membership has been discovered at the given slot.
     * 
     * @apiNote Origin: User
     * 
     * @param slot
     *            The slot at which the membership has been discovered.
     * @param membership
     *            The membership.
     * 
     */
    public static record MembershipDiscovered(int slot, Membership membership) {
    }

    /**
     * A command has been proposed.
     * 
     * @apiNote: Origin: User
     * 
     * @param command
     *            The command to be proposed.
     */
    public static record Propose(byte[] command) {
    }

    /**
     * A prepare request should be delivered to another process.
     * 
     * @apiNote Origin: Paxos/User.
     *          If this command is issued by the user then `processId` must be the
     *          id of the local process.
     * 
     * @param slot
     *            The slot of the prepare request.
     * @param processId
     *            The id of the process to which the prepare request should be
     *            delivered.
     * @param ballot
     *            The ballot of the prepare request.
     * 
     */
    public static record PrepareRequest(
            int slot,
            ProcessId processId,
            Ballot ballot) {
    }

    /**
     * A prepare ok should be delivered.
     * 
     * @apiNote Origin: Paxos/User.
     *          If this command is issued by the user then `processId` must be the
     *          id of the local process.
     * 
     * @param slot
     *            The slot of the prepare ok.
     * @param processId
     *            The id of the process to which the prepare ok should be delivered.
     * @param ballot
     *            The ballot of the prepare ok.
     * @param highestAccept
     *            The highest accepted proposal.
     * 
     */
    public static record PrepareOk(
            int slot,
            ProcessId processId,
            Ballot ballot,
            List<ProposalSlot> accepted) {
    }

    /**
     * An accept request should be delivered.
     * 
     * @apiNote Origin: Paxos/User.
     *          If this command is issued by the user then `processId` must be the
     *          id of the local process.
     * 
     * @param slot
     *            The slot of the accept request.
     * @param processId
     *            The id of the process to which the accept request should be
     *            delivered.
     * @param proposal
     *            The proposal of the accept request.
     */
    public static record AcceptRequest(
            int slot,
            ProcessId processId,
            Proposal proposal) {
    }

    /**
     * An accept ok should be delivered.
     * 
     * @apiNote Origin: Paxos/User.
     *          If this command is issued by the user then `processId` must be the
     *          id of
     *          the local process.
     * 
     * @param slot
     *            The slot of the accept ok.
     * @param processId
     *            The id of the process to which the accept ok should be delivered.
     * @param ballot
     *            The ballot of the accept ok.
     */
    public static record AcceptOk(
            int slot,
            ProcessId processId,
            Ballot ballot) {
    }

    /**
     * A learn should be delivered.
     * 
     * @apiNote Origin: Paxos/User.
     *          If this command is issued by the user then `processId` must be the
     *          id of
     *          the local process.
     * 
     * @param slot
     *            The slot of the learn.
     * @param processId
     *            The id of the process to which the learn should be delivered.
     * @param value
     *            The value of the learn.
     */
    public static record Learn(
            int slot,
            ProcessId processId,
            ProposalValue value) {
    }

    /**
     * A oneshot timer should be setup.
     * 
     * @apiNote Origin: Paxos
     * 
     * @param slot
     *            The slot for which the timer should be setup.
     * @param timerId
     *            The id of the timer.
     * @param timeout
     *            The timeout of the timer.
     */
    public static record SetupTimer(
            int slot,
            int timerId,
            Duration timeout) {
    }

    /**
     * A timer should be cancelled.
     * 
     * @apiNote Origin: Paxos
     * 
     * @param slot
     *            The slot for which the timer should be cancelled.
     * @param timerId
     *            The id of the timer.
     */
    public static record CancelTimer(
            int slot,
            int timerId) {
    }

    /**
     * A timer has expired.
     * 
     * @apiNote Origin: User
     * 
     * @param slot
     *            The slot for which the timer has expired.
     * @param timerId
     *            The id of the timer.
     */
    public static record TimerExpired(
            int slot,
            int timerId) {
    }

    private final Kind kind;
    private final Object payload;

    private PaxosCmd(Kind kind, Object payload) {
        this.kind = kind;
        this.payload = payload;
    }

    public Kind getKind() {
        return kind;
    }

    public Decide getDecide() {
        return (Decide) payload;
    }

    public NewLeader getNewLeader() {
        return (NewLeader) payload;
    }

    public MemberAdded getMemberAdded() {
        return (MemberAdded) payload;
    }

    public MemberRemoved getMemberRemoved() {
        return (MemberRemoved) payload;
    }

    public MembershipUnchanged getMembershipUnchanged() {
        return (MembershipUnchanged) payload;
    }

    public MembershipDiscovered getMembershipDiscovered() {
        return (MembershipDiscovered) payload;
    }

    public Propose getPropose() {
        return (Propose) payload;
    }

    public PrepareOk getPrepareOk() {
        return (PrepareOk) payload;
    }

    public PrepareRequest getPrepareRequest() {
        return (PrepareRequest) payload;
    }

    public AcceptOk getAcceptOk() {
        return (AcceptOk) payload;
    }

    public AcceptRequest getAcceptRequest() {
        return (AcceptRequest) payload;
    }

    public Learn getLearn() {
        return (Learn) payload;
    }

    public SetupTimer getSetupTimer() {
        return (SetupTimer) payload;
    }

    public CancelTimer getCancelTimer() {
        return (CancelTimer) payload;
    }

    public TimerExpired getTimerExpired() {
        return (TimerExpired) payload;
    }

    public static PaxosCmd decide(int slot, ProposalValue value) {
        return new PaxosCmd(Kind.DECIDE, new Decide(slot, value));
    }

    public static PaxosCmd newLeader(ProcessId leader) {
        return new PaxosCmd(Kind.NEW_LEADER, new NewLeader(leader, List.of()));
    }

    public static PaxosCmd newLeader(ProcessId leader, List<byte[]> pending) {
        return new PaxosCmd(Kind.NEW_LEADER, new NewLeader(leader, new ArrayList<>(pending)));
    }

    public static PaxosCmd memberAdded(int slot, ProcessId processId) {
        return new PaxosCmd(Kind.MEMBER_ADDED, new MemberAdded(slot, processId));
    }

    public static PaxosCmd memberRemoved(int slot, ProcessId processId) {
        return new PaxosCmd(Kind.MEMBER_REMOVED, new MemberRemoved(slot, processId));
    }

    public static PaxosCmd membershipUnchanged(int slot) {
        return new PaxosCmd(Kind.MEMBERSHIP_UNCHANGED, new MembershipUnchanged(slot));
    }

    public static PaxosCmd membershipDiscovered(int slot, Membership membership) {
        return new PaxosCmd(Kind.MEMBERSHIP_DISCOVERED, new MembershipDiscovered(slot, membership));
    }

    public static PaxosCmd propose(byte[] command) {
        return new PaxosCmd(Kind.PROPOSE, new Propose(command));
    }

    public static PaxosCmd prepareRequest(int slot, ProcessId processId, Ballot ballot) {
        return new PaxosCmd(Kind.PREPARE_REQUEST, new PrepareRequest(slot, processId, ballot));
    }

    public static PaxosCmd prepareOk(
            int slot,
            ProcessId processId,
            Ballot ballot,
            List<ProposalSlot> accepted) {
        return new PaxosCmd(Kind.PREPARE_OK, new PrepareOk(slot, processId, ballot, accepted));
    }

    public static PaxosCmd acceptRequest(int slot, ProcessId processId, Proposal proposal) {
        return new PaxosCmd(Kind.ACCEPT_REQUEST, new AcceptRequest(slot, processId, proposal));
    }

    public static PaxosCmd acceptOk(int slot, ProcessId processId, Ballot ballot) {
        return new PaxosCmd(Kind.ACCEPT_OK, new AcceptOk(slot, processId, ballot));
    }

    public static PaxosCmd learn(int slot, ProcessId processId, ProposalValue value) {
        return new PaxosCmd(Kind.LEARN, new Learn(slot, processId, value));
    }

    public static PaxosCmd setupTimer(int slot, int timerId, Duration timeout) {
        return new PaxosCmd(Kind.SETUP_TIMER, new SetupTimer(slot, timerId, timeout));
    }

    public static PaxosCmd cancelTimer(int slot, int timerId) {
        return new PaxosCmd(Kind.CANCEL_TIMER, new CancelTimer(slot, timerId));
    }

    public static PaxosCmd timerExpired(int slot, int timerId) {
        return new PaxosCmd(Kind.TIMER_EXPIRED, new TimerExpired(slot, timerId));
    }

}
