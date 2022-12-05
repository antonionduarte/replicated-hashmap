package asd.paxos.multi;

import asd.paxos.AgreementCmd;

public interface MultipaxosIO {

	void push(AgreementCmd cmd);

}
