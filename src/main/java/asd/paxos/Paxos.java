package asd.paxos;

// TODO: Update a proposer's ballot when acceptor receives a higher ballot

public interface Paxos {
        void push(PaxosCmd... commands);

        boolean isEmpty();

        PaxosCmd pop();

        Membership membership(int slot);

        // Debugging is fun
        void printDebug();
}