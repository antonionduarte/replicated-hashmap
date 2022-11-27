package asd.paxos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import asd.paxos.proposal.ProposalValue;

public class LearnerTest {
    @Test
    public void onlyDecidesOnce() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(5, 1, 1);
        var learner = new Learner(queue.getIO(membership.learners[0]));

        learner.onDecide(new ProposalValue("test"));
        {
            var commands = queue.popAll();
            assertEquals(1, commands.size());
            var command = commands.get(0);
            assertTrue(command.isDecided());
            assertEquals(new ProposalValue("test"), command.getDecided().value());
        }

        learner.onDecide(new ProposalValue("test"));
        {
            var commands = queue.popAll();
            assertEquals(0, commands.size());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void failsOnDifferentDecisions() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(5, 1, 1);
        var learner = new Learner(queue.getIO(membership.learners[0]));

        learner.onDecide(new ProposalValue("test"));
        learner.onDecide(new ProposalValue("test2"));
    }

}
