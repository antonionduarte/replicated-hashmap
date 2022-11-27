package asd.paxos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

import asd.paxos.proposal.Proposal;
import asd.paxos.proposal.ProposalNumber;
import asd.paxos.proposal.ProposalValue;

public class AcceptorTest {
    @Test
    public void acceptInitialPrepareRequest() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(5, 1, 0);
        var acceptor = new Acceptor(queue.getIO(membership.acceptors[0]));

        acceptor.onPrepareRequest(membership.proposers[0], new ProposalNumber(0, 1));
        {
            var commands = queue.popAll();
            assertEquals(1, commands.size());
            var command = commands.get(0);
            assertTrue(command.isPrepareOk());
            assertEquals(membership.proposers[0], command.getPrepareOk().processId());
            assertEquals(new ProposalNumber(0, 1), command.getPrepareOk().proposalNumber());
        }
    }

    @Test
    public void acceptHigherPrepareRequest() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(5, 1, 0);
        var acceptor = new Acceptor(queue.getIO(membership.acceptors[0]));

        acceptor.onPrepareRequest(membership.proposers[0], new ProposalNumber(0, 1));
        queue.popAll();

        acceptor.onPrepareRequest(membership.proposers[0], new ProposalNumber(0, 2));
        {
            var commands = queue.popAll();
            assertEquals(1, commands.size());
            var command = commands.get(0);
            assertTrue(command.isPrepareOk());
            assertEquals(membership.proposers[0], command.getPrepareOk().processId());
            assertEquals(new ProposalNumber(0, 2), command.getPrepareOk().proposalNumber());
        }
    }

    @Test
    public void rejectLowerPrepareRequest() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(5, 1, 0);
        var acceptor = new Acceptor(queue.getIO(membership.acceptors[0]));

        acceptor.onPrepareRequest(membership.proposers[0], new ProposalNumber(0, 2));
        queue.popAll();

        acceptor.onPrepareRequest(membership.proposers[0], new ProposalNumber(0, 1));
        assertTrue(queue.popAll().isEmpty());
    }

    @Test
    public void acceptHigherAcceptRequest() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(5, 1, 0);
        var acceptor = new Acceptor(queue.getIO(membership.acceptors[0]));

        acceptor.onPrepareRequest(membership.proposers[0], new ProposalNumber(0, 1));
        queue.popAll();

        acceptor.onAcceptRequest(
                membership.proposers[0],
                new Proposal(new ProposalNumber(0, 2), new ProposalValue("test")));
        {
            var commands = queue.popAll();
            assertEquals(1, commands.size());
            var command = commands.get(0);
            assertTrue(command.isAcceptOk());
            assertEquals(membership.proposers[0], command.getAcceptOk().processId());
            assertEquals(new ProposalNumber(0, 2), command.getAcceptOk().proposalNumber());
        }
    }

    @Test
    public void sendsHighestAcceptOnHigherAcceptRequest() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(5, 1, 0);
        var acceptor = new Acceptor(queue.getIO(membership.acceptors[0]));

        acceptor.onPrepareRequest(membership.proposers[0], new ProposalNumber(0, 1));
        queue.popAll();

        acceptor.onAcceptRequest(
                membership.proposers[0],
                new Proposal(new ProposalNumber(0, 2), new ProposalValue("test")));
        queue.popAll();

        acceptor.onPrepareRequest(membership.proposers[0], new ProposalNumber(0, 3));
        {
            var commands = queue.popAll();
            assertEquals(1, commands.size());
            var command = commands.get(0);
            assertTrue(command.isPrepareOk());
            assertEquals(membership.proposers[0], command.getPrepareOk().processId());
            assertEquals(new ProposalNumber(0, 3), command.getPrepareOk().proposalNumber());
            assertEquals(
                    Optional.of(new Proposal(new ProposalNumber(0, 2), new ProposalValue("test"))),
                    command.getPrepareOk().highestAccept());
        }
    }

    @Test
    public void rejectLowerAcceptRequest() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(5, 1, 0);
        var acceptor = new Acceptor(queue.getIO(membership.acceptors[0]));

        acceptor.onPrepareRequest(membership.proposers[0], new ProposalNumber(0, 2));
        queue.popAll();

        acceptor.onAcceptRequest(
                membership.proposers[0],
                new Proposal(new ProposalNumber(0, 1), new ProposalValue("test")));
        assertTrue(queue.popAll().isEmpty());
    }
}
