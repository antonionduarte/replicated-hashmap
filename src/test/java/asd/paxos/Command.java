package asd.paxos;

import java.time.Duration;
import java.util.Optional;

import asd.paxos.proposal.Proposal;
import asd.paxos.proposal.ProposalNumber;
import asd.paxos.proposal.ProposalValue;

public class Command {
    public static record Decided(ProposalValue value) {
    }

    public static record PrepareRequest(ProcessId processId, ProposalNumber proposalNumber) {
    }

    public static record PrepareOk(
            ProcessId processId,
            ProposalNumber proposalNumber,
            Optional<Proposal> highestAccept) {
    }

    public static record AcceptRequest(ProcessId processId, Proposal proposal) {
    }

    public static record AcceptOk(ProcessId processId, ProposalNumber proposalNumber) {
    }

    public static record Decide(ProcessId processId, ProposalValue proposal) {
    }

    public static record SetupTimer(int timerId, Duration interval) {
    }

    public static record CancelTimer(int timerId) {
    }

    public final ProcessId sender;
    public final Object command;

    private Command(ProcessId sender, Object command) {
        this.sender = sender;
        this.command = command;
    }

    public static Command from(ProcessId sender, Object command) {
        assert command instanceof Decided
                || command instanceof PrepareRequest
                || command instanceof PrepareOk
                || command instanceof AcceptRequest
                || command instanceof AcceptOk
                || command instanceof Decide
                || command instanceof SetupTimer
                || command instanceof CancelTimer;
        return new Command(sender, command);
    }

    public ProcessId getSender() {
        return sender;
    }

    public boolean isDecided() {
        return command instanceof Decided;
    }

    public Decided getDecided() {
        return (Decided) command;
    }

    public boolean isPrepareRequest() {
        return command instanceof PrepareRequest;
    }

    public PrepareRequest getPrepareRequest() {
        return (PrepareRequest) command;
    }

    public boolean isPrepareOk() {
        return command instanceof PrepareOk;
    }

    public PrepareOk getPrepareOk() {
        return (PrepareOk) command;
    }

    public boolean isAcceptRequest() {
        return command instanceof AcceptRequest;
    }

    public AcceptRequest getAcceptRequest() {
        return (AcceptRequest) command;
    }

    public boolean isAcceptOk() {
        return command instanceof AcceptOk;
    }

    public AcceptOk getAcceptOk() {
        return (AcceptOk) command;
    }

    public boolean isDecide() {
        return command instanceof Decide;
    }

    public Decide getDecide() {
        return (Decide) command;
    }

    public boolean isSetupTimer() {
        return command instanceof SetupTimer;
    }

    public SetupTimer getSetupTimer() {
        return (SetupTimer) command;
    }

    public boolean isCancelTimer() {
        return command instanceof CancelTimer;
    }

    public CancelTimer getCancelTimer() {
        return (CancelTimer) command;
    }

}
