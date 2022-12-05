package asd.paxos.single;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

import asd.paxos.ProposalValue;
import asd.paxos.Ballot;

public class AcceptorTest {
    @Test
    public void acceptInitialPrepareRequest() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(5, 1, 0);
        var config = PaxosConfig.builder()
                .withProposers(membership.proposerList())
                .withAcceptors(membership.acceptorList()).build();
        var acceptor = new Acceptor(membership.acceptors[0], queue.getIO(membership.acceptors[0]), config);

        acceptor.onPrepareRequest(membership.proposers[0], new Ballot(0, 1));
        {
            var commands = queue.popAll();
            assertEquals(1, commands.size());
            var command = commands.get(0);
            assertTrue(command.isPrepareOk());
            assertEquals(membership.proposers[0], command.getSendPrepareOk().processId());
            assertEquals(new Ballot(0, 1), command.getSendPrepareOk().ballot());
        }
    }

    @Test
    public void acceptHigherPrepareRequest() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(5, 1, 0);
        var config = PaxosConfig.builder()
                .withProposers(membership.proposerList())
                .withAcceptors(membership.acceptorList()).build();
        var acceptor = new Acceptor(membership.acceptors[0], queue.getIO(membership.acceptors[0]), config);

        acceptor.onPrepareRequest(membership.proposers[0], new Ballot(0, 1));
        queue.popAll();

        acceptor.onPrepareRequest(membership.proposers[0], new Ballot(0, 2));
        {
            var commands = queue.popAll();
            assertEquals(1, commands.size());
            var command = commands.get(0);
            assertTrue(command.isPrepareOk());
            assertEquals(membership.proposers[0], command.getSendPrepareOk().processId());
            assertEquals(new Ballot(0, 2), command.getSendPrepareOk().ballot());
        }
    }

    @Test
    public void rejectLowerPrepareRequest() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(5, 1, 0);
        var config = PaxosConfig.builder()
                .withProposers(membership.proposerList())
                .withAcceptors(membership.acceptorList()).build();
        var acceptor = new Acceptor(membership.acceptors[0], queue.getIO(membership.acceptors[0]), config);

        acceptor.onPrepareRequest(membership.proposers[0], new Ballot(0, 2));
        queue.popAll();

        acceptor.onPrepareRequest(membership.proposers[0], new Ballot(0, 1));
        assertTrue(queue.popAll().isEmpty());
    }

    @Test
    public void acceptHigherAcceptRequest() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(5, 1, 0);
        var config = PaxosConfig.builder()
                .withProposers(membership.proposerList())
                .withAcceptors(membership.acceptorList()).build();
        var acceptor = new Acceptor(membership.acceptors[0], queue.getIO(membership.acceptors[0]), config);

        acceptor.onPrepareRequest(membership.proposers[0], new Ballot(0, 1));
        queue.popAll();

        acceptor.onAcceptRequest(
                membership.proposers[0],
                new Proposal(new Ballot(0, 2), new ProposalValue("test")));
        {
            var commands = queue.popAll();
            assertEquals(1, commands.size());
            var command = commands.get(0);
            assertTrue(command.isSendAcceptOk());
            assertEquals(membership.proposers[0], command.getSendAcceptOk().processId());
            assertEquals(new Ballot(0, 2), command.getSendAcceptOk().ballot());
        }
    }

    @Test
    public void sendsHighestAcceptOnHigherAcceptRequest() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(5, 1, 0);
        var config = PaxosConfig.builder()
                .withProposers(membership.proposerList())
                .withAcceptors(membership.acceptorList()).build();
        var acceptor = new Acceptor(membership.acceptors[0], queue.getIO(membership.acceptors[0]), config);

        acceptor.onPrepareRequest(membership.proposers[0], new Ballot(0, 1));
        queue.popAll();

        acceptor.onAcceptRequest(
                membership.proposers[0],
                new Proposal(new Ballot(0, 2), new ProposalValue("test")));
        queue.popAll();

        acceptor.onPrepareRequest(membership.proposers[0], new Ballot(0, 3));
        {
            var commands = queue.popAll();
            assertEquals(1, commands.size());
            var command = commands.get(0);
            assertTrue(command.isPrepareOk());
            assertEquals(membership.proposers[0], command.getSendPrepareOk().processId());
            assertEquals(new Ballot(0, 3), command.getSendPrepareOk().ballot());
            assertEquals(
                    Optional.of(new Proposal(new Ballot(0, 2), new ProposalValue("test"))),
                    command.getSendPrepareOk().highestAccept());
        }
    }

    @Test
    public void rejectLowerAcceptRequest() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(5, 1, 0);
        var config = PaxosConfig.builder()
                .withProposers(membership.proposerList())
                .withAcceptors(membership.acceptorList()).build();
        var acceptor = new Acceptor(membership.acceptors[0], queue.getIO(membership.acceptors[0]), config);

        acceptor.onPrepareRequest(membership.proposers[0], new Ballot(0, 2));
        queue.popAll();

        acceptor.onAcceptRequest(
                membership.proposers[0],
                new Proposal(new Ballot(0, 1), new ProposalValue("test")));
        assertTrue(queue.popAll().isEmpty());
    }
}
