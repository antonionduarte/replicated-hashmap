package asd.paxos;

// TODO: Update a proposer's ballot when acceptor receives a higher ballot

public interface Paxos {
        void push(PaxosCmd cmd);

        boolean isEmpty();

        PaxosCmd pop();

        Membership membership(int slot);

        enum Variant {
                SINGLE,
                MULTI
        }
}