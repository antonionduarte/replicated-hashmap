package asd.paxos;

public interface Paxos {
        void push(PaxosCmd cmd);

        boolean isEmpty();

        PaxosCmd pop();

        Membership membership(int slot);
}