package asd.paxos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

import asd.paxos.Command.AcceptRequest;
import asd.paxos.Command.PrepareRequest;
import asd.paxos.proposal.ProposalNumber;
import asd.paxos.proposal.ProposalValue;

public class ProposerTest {
    @Test
    public void initialProposeSendsPrepareToAllAcceptors() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(1, 5, 0);
        var proposer = new Proposer(
                queue.getIO(membership.proposers[0]),
                membership.acceptorList(),
                membership.learnerList());

        proposer.propose(new ProposalValue("test"));
        {
            var commands = queue.popAll();
            assertTrue(commands.stream().allMatch(c -> c.sender.equals(proposer.getProcessId())));

            var expectedIds = new HashSet<>(Arrays.asList(membership.acceptors));
            var obtainedIds = commands.stream().filter(Command::isPrepareRequest).map(Command::getPrepareRequest)
                    .map(PrepareRequest::processId).collect(Collectors.toSet());
            assertEquals(expectedIds, obtainedIds);
        }
    }

    @Test
    public void sendsAcceptRequestOnMajority() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(1, 5, 0);
        var proposer = new Proposer(
                queue.getIO(membership.proposers[0]),
                membership.acceptorList(),
                membership.learnerList());

        proposer.propose(new ProposalValue("test"));
        queue.popAll();

        // Receive 3 OK's
        proposer.receivePrepareOk(membership.acceptors[0], new ProposalNumber(0, 1), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[1], new ProposalNumber(0, 1), Optional.empty());
        assertTrue(queue.isEmpty());
        proposer.receivePrepareOk(membership.acceptors[2], new ProposalNumber(0, 1), Optional.empty());
        {
            var commands = queue.popAll();
            var expectedIds = new HashSet<>(Arrays.asList(membership.acceptors));
            var obtainedIds = commands.stream().filter(Command::isAcceptRequest).map(Command::getAcceptRequest)
                    .map(AcceptRequest::processId).collect(Collectors.toSet());
            assertEquals(expectedIds, obtainedIds);
        }
        proposer.receivePrepareOk(membership.acceptors[3], new ProposalNumber(0, 1), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[4], new ProposalNumber(0, 1), Optional.empty());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void doesNotSentAcceptRequestsWithDuplicateOks() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(1, 5, 0);
        var proposer = new Proposer(
                queue.getIO(membership.proposers[0]),
                membership.acceptorList(),
                membership.learnerList());

        proposer.propose(new ProposalValue("test"));
        queue.popAll();

        // Receive 3 OK's
        proposer.receivePrepareOk(membership.acceptors[0], new ProposalNumber(0, 1), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[0], new ProposalNumber(0, 1), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[1], new ProposalNumber(0, 1), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[1], new ProposalNumber(0, 1), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[1], new ProposalNumber(0, 1), Optional.empty());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void sendsDecideOnMajorityAccepts() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(1, 5, 0);
        var proposer = new Proposer(
                queue.getIO(membership.proposers[0]),
                membership.acceptorList(),
                membership.learnerList());

        proposer.propose(new ProposalValue("test"));
        queue.popAll();

        // Receive 3 OK's
        proposer.receivePrepareOk(membership.acceptors[0], new ProposalNumber(0, 1), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[1], new ProposalNumber(0, 1), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[2], new ProposalNumber(0, 1), Optional.empty());
        queue.popAll();

        // Receive 3 Accepts
        proposer.receiveAcceptOk(membership.acceptors[0], new ProposalNumber(0, 1));
        proposer.receiveAcceptOk(membership.acceptors[1], new ProposalNumber(0, 1));
        assertTrue(queue.isEmpty());
        proposer.receiveAcceptOk(membership.acceptors[2], new ProposalNumber(0, 1));
        {
            var commands = queue.popAll();
            var expectedIds = new HashSet<>(Arrays.asList(membership.learners));
            var obtainedIds = commands.stream().filter(Command::isDecide).map(Command::getDecide)
                    .map(Command.Decide::processId).collect(Collectors.toSet());
            assertEquals(expectedIds, obtainedIds);
        }
        proposer.receiveAcceptOk(membership.acceptors[3], new ProposalNumber(0, 1));
        proposer.receiveAcceptOk(membership.acceptors[4], new ProposalNumber(0, 1));
        assertTrue(queue.isEmpty());
    }
}
