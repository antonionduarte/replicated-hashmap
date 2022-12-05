package asd.paxos.single;

import asd.paxos.ProcessId;

public class Message {
    public final ProcessId sender;
    public final PaxosCmd command;

    public Message(ProcessId sender, PaxosCmd command) {
        this.sender = sender;
        this.command = command;
    }

    public ProcessId getSender() {
        return sender;
    }

    public boolean isDecided() {
        return this.command.getKind() == PaxosCmd.Kind.Decided;
    }

    public PaxosCmd.Decided getDecided() {
        return this.command.getDecided();
    }

    public boolean isSendPrepareRequest() {
        return this.command.getKind() == PaxosCmd.Kind.SendPrepareRequest;
    }

    public PaxosCmd.SendPrepareRequest getSendPrepareRequest() {
        return this.command.getSendPrepareRequest();
    }

    public boolean isPrepareOk() {
        return this.command.getKind() == PaxosCmd.Kind.SendPrepareOk;
    }

    public PaxosCmd.SendPrepareOk getSendPrepareOk() {
        return this.command.getSendPrepareOk();
    }

    public boolean isSendAcceptRequest() {
        return this.command.getKind() == PaxosCmd.Kind.SendAcceptRequest;
    }

    public PaxosCmd.SendAcceptRequest getSendAcceptRequest() {
        return this.command.getSendAcceptRequest();
    }

    public boolean isSendAcceptOk() {
        return this.command.getKind() == PaxosCmd.Kind.SendAcceptOk;
    }

    public PaxosCmd.SendAcceptOk getSendAcceptOk() {
        return this.command.getSendAcceptOk();
    }

    public boolean isSendDecided() {
        return this.command.getKind() == PaxosCmd.Kind.SendDecided;
    }

    public PaxosCmd.SendDecided getSendDecided() {
        return this.command.getSendDecided();
    }

    public boolean isSetupTimer() {
        return this.command.getKind() == PaxosCmd.Kind.SetupTimer;
    }

    public PaxosCmd.SetupTimer getSetupTimer() {
        return this.command.getSetupTimer();
    }

    public boolean isCancelTimer() {
        return this.command.getKind() == PaxosCmd.Kind.CancelTimer;
    }

    public PaxosCmd.CancelTimer getCancelTimer() {
        return this.command.getCancelTimer();
    }

}
