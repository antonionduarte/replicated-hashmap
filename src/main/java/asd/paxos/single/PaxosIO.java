package asd.paxos.single;

import asd.paxos.AgreementCmd;

public interface PaxosIO {
    void push(AgreementCmd cmd);
}
