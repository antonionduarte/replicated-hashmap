package asd.paxos.multi;

import asd.paxos.single.PaxosCmd;

public interface MultipaxosIO {

	void push(MultipaxosCmd cmd);

}
