package asd.paxos;

import java.time.Duration;
import java.util.Optional;

import asd.paxos.single.Proposal;

// TODO should create different for paxos and multipaxos, because the proposal of both protocols is different
// and the proposal used by command thingy uses the proposal of the single paxos
public class AgreementCmd {
    public static enum Kind {
        Decided, SendPrepareRequest, SendPrepareOk, SendAcceptRequest, SendAcceptOk, SendDecided, SetupTimer, CancelTimer, NewLeader
    }

    // Issued when a decision was learned.
    public static record Decided(ProposalValue value) {
    }

    // Issued when the proposer wants to send a prepare request to an acceptor.
    public static record SendPrepareRequest(
            ProcessId processId,
            Ballot ballot) {
    }

    // Issued when the acceptor wants to send a prepare-ok message to a proposer.
    public static record SendPrepareOk(
            ProcessId processId,
            Ballot ballot,
            Optional<asd.paxos.single.Proposal> highestAccept) {
    }

    // Issued when the proposer wants to send an accept request to an acceptor.
    public static record SendAcceptRequest(
            ProcessId processId,
            asd.paxos.single.Proposal proposal) {
    }

    // Issued when the acceptor wants to send an accept-ok message to a proposer.
    public static record SendAcceptOk(
            ProcessId processId,
            Ballot ballot) {
    }

    // Issued when the proposer wants to send a decided message to a learner.
    public static record SendDecided(
            ProcessId processId,
            ProposalValue value) {
    }

    // Issued when the proposer wants to setup a timer.
    // The timer is a one-shot timer and expires after the given timeout.
    public static record SetupTimer(
            int timerId,
            Duration timeout) {
    }

    // Issued when the proposer wants to cancel a timer.
    public static record CancelTimer(
            int timerId) {
    }

    // Issued when a new leader has been elected
    public static record NewLeader(
            ProcessId processId) {
    }

    private final Kind kind;
    private final Object value;

    private AgreementCmd(Kind kind, Object value) {
        this.kind = kind;
        this.value = value;
    }

    public Kind getKind() {
        return kind;
    }

    public NewLeader getNewLeader() {
        if (kind != Kind.NewLeader)
            throw new IllegalStateException("getNewLeader() called on " + kind);
        return (NewLeader) value;
    }

    public Decided getDecided() {
        if (kind != Kind.Decided)
            throw new IllegalStateException("Not a decided command");
        return (Decided) value;
    }

    public SendPrepareRequest getSendPrepareRequest() {
        if (kind != Kind.SendPrepareRequest)
            throw new IllegalStateException("Not a send prepare request command");
        return (SendPrepareRequest) value;
    }

    public SendPrepareOk getSendPrepareOk() {
        if (kind != Kind.SendPrepareOk)
            throw new IllegalStateException("Not a send prepare ok command");
        return (SendPrepareOk) value;
    }

    public SendAcceptRequest getSendAcceptRequest() {
        if (kind != Kind.SendAcceptRequest)
            throw new IllegalStateException("Not a send accept request command");
        return (SendAcceptRequest) value;
    }

    public SendAcceptOk getSendAcceptOk() {
        if (kind != Kind.SendAcceptOk)
            throw new IllegalStateException("Not a send accept ok command");
        return (SendAcceptOk) value;
    }

    public SendDecided getSendDecided() {
        if (kind != Kind.SendDecided)
            throw new IllegalStateException("Not a send decided command");
        return (SendDecided) value;
    }

    public SetupTimer getSetupTimer() {
        if (kind != Kind.SetupTimer)
            throw new IllegalStateException("Not a setup timer command");
        return (SetupTimer) value;
    }

    public CancelTimer getCancelTimer() {
        if (kind != Kind.CancelTimer)
            throw new IllegalStateException("Not a cancel timer command");
        return (CancelTimer) value;
    }

    public static AgreementCmd decided(ProposalValue value) {
        return new AgreementCmd(Kind.Decided, new Decided(value));
    }

    public static AgreementCmd sendPrepareRequest(ProcessId processId, Ballot ballot) {
        return new AgreementCmd(Kind.SendPrepareRequest, new SendPrepareRequest(processId, ballot));
    }

    public static AgreementCmd sendPrepareOk(ProcessId processId, Ballot ballot, Optional<asd.paxos.single.Proposal> highestAccept) {
        return new AgreementCmd(Kind.SendPrepareOk, new SendPrepareOk(processId, ballot, highestAccept));
    }

    public static AgreementCmd sendAcceptRequest(ProcessId processId, Proposal proposal) {
        return new AgreementCmd(Kind.SendAcceptRequest, new SendAcceptRequest(processId, proposal));
    }

    public static AgreementCmd sendAcceptOk(ProcessId processId, Ballot ballot) {
        return new AgreementCmd(Kind.SendAcceptOk, new SendAcceptOk(processId, ballot));
    }

    public static AgreementCmd sendDecided(ProcessId processId, ProposalValue value) {
        return new AgreementCmd(Kind.SendDecided, new SendDecided(processId, value));
    }

    public static AgreementCmd setupTimer(int timerId, Duration timeout) {
        return new AgreementCmd(Kind.SetupTimer, new SetupTimer(timerId, timeout));
    }

    public static AgreementCmd cancelTimer(int timerId) {
        return new AgreementCmd(Kind.CancelTimer, new CancelTimer(timerId));
    }
}
