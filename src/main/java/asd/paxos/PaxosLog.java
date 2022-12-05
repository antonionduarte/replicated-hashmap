package asd.paxos;

import java.io.OutputStream;
import java.time.Clock;
import java.util.Stack;

import org.apache.commons.codec.binary.Hex;

import asd.paxos.single.Proposal;
import asd.protocols.PaxosBabel;

public class PaxosLog {
    private static record Context(ProcessId processId, int instance) {
    }

    public static interface Invokable {
        public void invoke();
    }

    private static final Clock clock = Clock.systemUTC();
    private static OutputStream outputStream = null;
    private static Stack<Context> contextStack = new Stack<>();

    public static void init(String filename) {
        if (PaxosLog.outputStream != null)
            throw new IllegalStateException("Cannot initialize twice");
        try {
            PaxosLog.outputStream = new java.io.FileOutputStream(filename);
        } catch (java.io.FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void withContext(ProcessId processId, int instance, Invokable invokable) {
        PaxosLog.contextStack.push(new Context(processId, instance));
        try {
            invokable.invoke();
        } finally {
            PaxosLog.contextStack.pop();
        }
    }

    public static void log(String event, Object... kv) {
        if (!PaxosLog.shoudLog())
            return;
        var sb = new StringBuilder();
        sb.append(event);
        for (var i = 0; i < kv.length; i += 2) {
            var k = kv[i];
            var v = kv[i + 1];
            sb.append(" ");
            sb.append(k);
            sb.append("=");
            if (v instanceof ProcessId)
                sb.append(prettyProcessId((ProcessId) v));
            else if (v instanceof ProposalValue)
                sb.append(prettyProposalValue((ProposalValue) v));
            else if (v instanceof Ballot)
                sb.append(prettyBallot((Ballot) v));
            else if (v instanceof Proposal)
                sb.append(prettyProposal((Proposal) v));
            else
                sb.append(v);
        }
        PaxosLog.write(sb.toString());
    }

    private static void write(String s) {
        var context = PaxosLog.getContext();
        try {
            var now = clock.instant();
            var nano_now = now.getEpochSecond() * 1_000_000_000 + ((long) now.getNano());
            var header = String.format("%d process=%s instance=%d\t",
                    nano_now, prettyProcessId(context.processId), context.instance);
            PaxosLog.outputStream.write(header.getBytes());
            PaxosLog.outputStream.write(s.getBytes());
            PaxosLog.outputStream.write("\n".getBytes());
            PaxosLog.outputStream.flush();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean shoudLog() {
        if (PaxosLog.outputStream == null || PaxosLog.contextStack.isEmpty())
            return false;
        return true;
    }

    private static Context getContext() {
        if (PaxosLog.contextStack.isEmpty())
            throw new IllegalStateException("No context");
        return PaxosLog.contextStack.peek();
    }

    private static String prettyProcessId(ProcessId processId) {
        if (processId == null)
            return "<null>";
        return PaxosBabel.hostFromProcessId(processId).toString();
    }

    private static String prettyProposal(Proposal proposal) {
        if (proposal == null)
            return "<null>";
        return String.format("%s:%s", prettyBallot(proposal.ballot), prettyProposalValue(proposal.value));
    }

    private static String prettyProposalValue(ProposalValue proposalValue) {
        if (proposalValue == null)
            return "<null>";
        return Hex.encodeHexString(proposalValue.hash.digest);
    }

    private static String prettyBallot(Ballot ballot) {
        if (ballot == null)
            return "<null>";
        return String.format("%s:%d", prettyProcessId(ballot.processId), ballot.sequenceNumber);
    }
}
