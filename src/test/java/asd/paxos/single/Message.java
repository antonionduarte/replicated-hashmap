package asd.paxos.single;

import asd.paxos.AgreementCmd;
import asd.paxos.ProcessId;

public class Message {
    public final ProcessId sender;
    public final AgreementCmd command;

    public Message(ProcessId sender, AgreementCmd command) {
        this.sender = sender;
        this.command = command;
    }

    public ProcessId getSender() {
        return sender;
    }

    public boolean isDecided() {
        return this.command.getKind() == AgreementCmd.Kind.Decided;
    }

    public AgreementCmd.Decided getDecided() {
        return this.command.getDecided();
    }

    public boolean isSendPrepareRequest() {
        return this.command.getKind() == AgreementCmd.Kind.SendPrepareRequest;
    }

    public AgreementCmd.SendPrepareRequest getSendPrepareRequest() {
        return this.command.getSendPrepareRequest();
    }

    public boolean isPrepareOk() {
        return this.command.getKind() == AgreementCmd.Kind.SendPrepareOk;
    }

    public AgreementCmd.SendPrepareOk getSendPrepareOk() {
        return this.command.getSendPrepareOk();
    }

    public boolean isSendAcceptRequest() {
        return this.command.getKind() == AgreementCmd.Kind.SendAcceptRequest;
    }

    public AgreementCmd.SendAcceptRequest getSendAcceptRequest() {
        return this.command.getSendAcceptRequest();
    }

    public boolean isSendAcceptOk() {
        return this.command.getKind() == AgreementCmd.Kind.SendAcceptOk;
    }

    public AgreementCmd.SendAcceptOk getSendAcceptOk() {
        return this.command.getSendAcceptOk();
    }

    public boolean isSendDecided() {
        return this.command.getKind() == AgreementCmd.Kind.SendDecided;
    }

    public AgreementCmd.SendDecided getSendDecided() {
        return this.command.getSendDecided();
    }

    public boolean isSetupTimer() {
        return this.command.getKind() == AgreementCmd.Kind.SetupTimer;
    }

    public AgreementCmd.SetupTimer getSetupTimer() {
        return this.command.getSetupTimer();
    }

    public boolean isCancelTimer() {
        return this.command.getKind() == AgreementCmd.Kind.CancelTimer;
    }

    public AgreementCmd.CancelTimer getCancelTimer() {
        return this.command.getCancelTimer();
    }

}
