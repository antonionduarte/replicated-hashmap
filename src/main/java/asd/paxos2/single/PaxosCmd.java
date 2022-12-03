package asd.paxos2.single;

import java.time.Duration;
import java.util.Optional;

import asd.paxos2.Ballot;
import asd.paxos2.ProcessId;
import asd.paxos2.ProposalValue;

public class PaxosCmd {
    public static enum Kind {
        Decided, SendPrepareRequest, SendPrepareOk, SendAcceptRequest, SendAcceptOk, SendDecided, SetupTimer, CancelTimer
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
            Optional<Proposal> highestAccept) {
    }

    // Issued when the proposer wants to send an accept request to an acceptor.
    public static record SendAcceptRequest(
            ProcessId processId,
            Proposal proposal) {
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

    private final Kind kind;
    private final Object value;

    private PaxosCmd(Kind kind, Object value) {
        this.kind = kind;
        this.value = value;
    }

    public Kind getKind() {
        return kind;
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

    public static PaxosCmd decided(ProposalValue value) {
        return new PaxosCmd(Kind.Decided, new Decided(value));
    }

    public static PaxosCmd sendPrepareRequest(ProcessId processId, Ballot ballot) {
        return new PaxosCmd(Kind.SendPrepareRequest, new SendPrepareRequest(processId, ballot));
    }

    public static PaxosCmd sendPrepareOk(ProcessId processId, Ballot ballot, Optional<Proposal> highestAccept) {
        return new PaxosCmd(Kind.SendPrepareOk, new SendPrepareOk(processId, ballot, highestAccept));
    }

    public static PaxosCmd sendAcceptRequest(ProcessId processId, Proposal proposal) {
        return new PaxosCmd(Kind.SendAcceptRequest, new SendAcceptRequest(processId, proposal));
    }

    public static PaxosCmd sendAcceptOk(ProcessId processId, Ballot ballot) {
        return new PaxosCmd(Kind.SendAcceptOk, new SendAcceptOk(processId, ballot));
    }

    public static PaxosCmd sendDecided(ProcessId processId, ProposalValue value) {
        return new PaxosCmd(Kind.SendDecided, new SendDecided(processId, value));
    }

    public static PaxosCmd setupTimer(int timerId, Duration timeout) {
        return new PaxosCmd(Kind.SetupTimer, new SetupTimer(timerId, timeout));
    }

    public static PaxosCmd cancelTimer(int timerId) {
        return new PaxosCmd(Kind.CancelTimer, new CancelTimer(timerId));
    }
}
