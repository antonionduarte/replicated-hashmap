package asd.paxos.single;

import java.util.Arrays;
import java.util.List;

import asd.paxos.ProcessId;

public class PaxosTestUtils {
    public static class MembershipTriple {
        public final ProcessId[] proposers;
        public final ProcessId[] acceptors;
        public final ProcessId[] learners;

        public MembershipTriple(ProcessId[] proposers, ProcessId[] acceptors, ProcessId[] learners) {
            this.proposers = proposers;
            this.acceptors = acceptors;
            this.learners = learners;
        }

        public List<ProcessId> proposerList() {
            return Arrays.asList(this.proposers);
        }

        public List<ProcessId> acceptorList() {
            return Arrays.asList(this.acceptors);
        }

        public List<ProcessId> learnerList() {
            return Arrays.asList(this.learners);
        }
    }

    public static MembershipTriple createMembershipTriple(int proposers, int acceptors, int learners) {
        var proposerIds = new ProcessId[proposers];
        for (var i = 0; i < proposers; i++)
            proposerIds[i] = new ProcessId(i);

        var acceptorIds = new ProcessId[acceptors];
        for (var i = 0; i < acceptors; i++)
            acceptorIds[i] = new ProcessId(i);

        var learnerIds = new ProcessId[learners];
        for (var i = 0; i < learners; i++)
            learnerIds[i] = new ProcessId(i);

        return new MembershipTriple(proposerIds, acceptorIds, learnerIds);
    }
}
