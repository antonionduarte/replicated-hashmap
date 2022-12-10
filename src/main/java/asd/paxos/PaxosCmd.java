package asd.paxos;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class PaxosCmd {
    public static enum Kind {
        NEW_LEADER, DECIDE, MEMBER_ADDED, MEMBER_REMOVED, MEMBERSHIP_UNCHANGED, MEMBERSHIP_DISCOVERED, PROPOSE, PREPARE_REQUEST, PREPARE_OK, ACCEPT_REQUEST, ACCEPT_OK, DECIDED, SETUP_TIMER, CANCEL_TIMER, TIMER_EXPIRED,
    }

    public static record Decide(int slot, ProposalValue value) {
    }

    public static record NewLeader(ProcessId leader, List<byte[]> commands) {
    }

    public static record MemberAdded(int slot, ProcessId processId) {
    }

    public static record MemberRemoved(int slot, ProcessId processId) {
    }

    public static record MembershipUnchanged(int slot) {
    }

    public static record MembershipDiscovered(int slot, Membership membership) {
    }

    public static record Propose(byte[] command) {
    }

    public static record PrepareRequest(
            ProcessId processId,
            Ballot ballot,
            int slot) {
    }

    public static record PrepareOk(
            ProcessId processId,
            Ballot ballot,
            Optional<Proposal> highestAccept,
            int slot) {
    }

    public static record AcceptRequest(
            ProcessId processId,
            Proposal proposal,
            int slot) {
    }

    public static record AcceptOk(
            ProcessId processId,
            Ballot ballot,
            int slot) {
    }

    public static record Decided(
            ProcessId processId,
            ProposalValue value,
            int slot) {
    }

    public static record SetupTimer(
            int slot,
            int timerId,
            Duration timeout) {
    }

    public static record CancelTimer(
            int slot,
            int timerId) {
    }

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

    public Decided getDecided() {
        return (Decided) payload;
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

    public static PaxosCmd newLeader(ProcessId leader, List<byte[]> commands) {
        return new PaxosCmd(Kind.NEW_LEADER, new NewLeader(leader, commands));
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

    public static PaxosCmd prepareRequest(ProcessId processId, Ballot ballot, int slot) {
        return new PaxosCmd(Kind.PREPARE_REQUEST, new PrepareRequest(processId, ballot, slot));
    }

    public static PaxosCmd prepareOk(
            ProcessId processId,
            Ballot ballot,
            Optional<Proposal> highestAccept,
            int slot) {
        return new PaxosCmd(Kind.PREPARE_OK, new PrepareOk(processId, ballot, highestAccept, slot));
    }

    public static PaxosCmd acceptRequest(ProcessId processId, Proposal proposal, int slot) {
        return new PaxosCmd(Kind.ACCEPT_REQUEST, new AcceptRequest(processId, proposal, slot));
    }

    public static PaxosCmd acceptOk(ProcessId processId, Ballot ballot, int slot) {
        return new PaxosCmd(Kind.ACCEPT_OK, new AcceptOk(processId, ballot, slot));
    }

    public static PaxosCmd decided(ProcessId processId, ProposalValue value, int slot) {
        return new PaxosCmd(Kind.DECIDED, new Decided(processId, value, slot));
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
