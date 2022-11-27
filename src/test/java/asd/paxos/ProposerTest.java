package asd.paxos;

import java.util.List;

import org.junit.Test;

import asd.paxos.proposal.ProposalValue;

public class ProposerTest {
    @Test
    public void simple() {
        var queue = new PaxosCommandQueue();

        var a1 = new Acceptor(queue.getIO(new ProcessId(1)));
        var a2 = new Acceptor(queue.getIO(new ProcessId(2)));
        var a3 = new Acceptor(queue.getIO(new ProcessId(3)));
        var as = List.of(a1.getProcessId(), a2.getProcessId(), a3.getProcessId());
        var ls = List.<ProcessId>of();

        var val = new ProposalValue(new byte[] { 1, 2, 3 });
        var p1 = new Proposer(queue.getIO(new ProcessId(1)), as, ls);

        p1.propose(val);
    }
}
