package asd.paxos.single;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import asd.paxos.AgreementCmd;
import org.junit.Test;

import asd.paxos.Ballot;
import asd.paxos.ProposalValue;

public class ProposerTest {
    @Test
    public void initialProposeSendsPrepareToAllAcceptors() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(1, 5, 0);
        var config = PaxosConfig.builder()
                .withProposers(membership.proposerList())
                .withAcceptors(membership.acceptorList()).build();
        var proposer = new Proposer(membership.proposers[0], queue.getIO(membership.proposers[0]), config);

        proposer.propose(new ProposalValue("test"));
        {
            var commands = queue.popAll();
            assertTrue(commands.stream().allMatch(c -> c.sender.equals(proposer.getProcessId())));

            var expectedIds = new HashSet<>(Arrays.asList(membership.acceptors));
            var obtainedIds = commands.stream().filter(Message::isSendPrepareRequest)
                    .map(Message::getSendPrepareRequest)
                    .map(AgreementCmd.SendPrepareRequest::processId).collect(Collectors.toSet());
            assertEquals(expectedIds, obtainedIds);
        }
    }

    @Test
    public void sendsAcceptRequestOnMajority() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(1, 5, 0);
        var config = PaxosConfig.builder()
                .withProposers(membership.proposerList())
                .withAcceptors(membership.acceptorList()).build();
        var proposer = new Proposer(membership.proposers[0], queue.getIO(membership.proposers[0]), config);

        proposer.propose(new ProposalValue("test"));
        queue.popAll();

        // Receive 3 OK's
        proposer.receivePrepareOk(membership.acceptors[0], new Ballot(0, 0), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[1], new Ballot(0, 0), Optional.empty());
        assertTrue(queue.isEmpty());
        proposer.receivePrepareOk(membership.acceptors[2], new Ballot(0, 0), Optional.empty());
        {
            var commands = queue.popAll();
            var expectedIds = new HashSet<>(Arrays.asList(membership.acceptors));
            var obtainedIds = commands.stream().filter(Message::isSendAcceptRequest).map(Message::getSendAcceptRequest)
                    .map(AgreementCmd.SendAcceptRequest::processId).collect(Collectors.toSet());
            assertEquals(expectedIds, obtainedIds);
        }
        proposer.receivePrepareOk(membership.acceptors[3], new Ballot(0, 0), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[4], new Ballot(0, 0), Optional.empty());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void doesNotSentAcceptRequestsWithDuplicateOks() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(1, 5, 0);
        var config = PaxosConfig.builder()
                .withProposers(membership.proposerList())
                .withAcceptors(membership.acceptorList()).build();
        var proposer = new Proposer(membership.proposers[0], queue.getIO(membership.proposers[0]), config);

        proposer.propose(new ProposalValue("test"));
        queue.popAll();

        // Receive 3 OK's
        proposer.receivePrepareOk(membership.acceptors[0], new Ballot(0, 0), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[0], new Ballot(0, 0), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[1], new Ballot(0, 0), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[1], new Ballot(0, 0), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[1], new Ballot(0, 0), Optional.empty());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void sendsDecideOnMajorityAccepts() {
        var queue = new PaxosCommandQueue();
        var membership = PaxosTestUtils.createMembershipTriple(1, 5, 0);
        var config = PaxosConfig.builder()
                .withProposers(membership.proposerList())
                .withAcceptors(membership.acceptorList()).build();
        var proposer = new Proposer(membership.proposers[0], queue.getIO(membership.proposers[0]), config);

        proposer.propose(new ProposalValue("test"));
        queue.popAll();

        // Receive 3 OK's
        proposer.receivePrepareOk(membership.acceptors[0], new Ballot(0, 0), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[1], new Ballot(0, 0), Optional.empty());
        proposer.receivePrepareOk(membership.acceptors[2], new Ballot(0, 0), Optional.empty());
        queue.popAll();

        // Receive 3 Accepts
        proposer.receiveAcceptOk(membership.acceptors[0], new Ballot(0, 0));
        proposer.receiveAcceptOk(membership.acceptors[1], new Ballot(0, 0));
        assertTrue(queue.isEmpty());
        proposer.receiveAcceptOk(membership.acceptors[2], new Ballot(0, 0));
        {
            var commands = queue.popAll();
            var expectedIds = new HashSet<>(Arrays.asList(membership.learners));
            var obtainedIds = commands.stream().filter(Message::isSendDecided).map(Message::getSendDecided)
                    .map(AgreementCmd.SendDecided::processId).collect(Collectors.toSet());
            assertEquals(expectedIds, obtainedIds);
        }
        proposer.receiveAcceptOk(membership.acceptors[3], new Ballot(0, 0));
        proposer.receiveAcceptOk(membership.acceptors[4], new Ballot(0, 0));
        assertTrue(queue.isEmpty());
    }

}
