package asd.paxos;

// TODO: Update a proposer's ballot when acceptor receives a higher ballot

public interface Paxos {
        /**
         * Push and execute the given commands.
         * 
         * @param commands
         *                The commands to push and execute.
         */
        void push(PaxosCmd... commands);

        /**
         * Check if the output queue is empty.
         * 
         * @return True if the output queue is empty, false otherwise.
         */
        boolean isEmpty();

        /**
         * Pop a command from the output queue.
         * 
         * 
         * @return The command popped from the output queue.
         */
        PaxosCmd pop();

        /**
         * Get the membership at the given slot.
         * 
         * @param slot
         *                The slot to get the membership at.
         */
        Membership membership(int slot);

        /**
         * Garbage collect all slots up to and including the given slot.
         * 
         * @param slot
         *                The slot to garbage collect up to.
         */
        void gc(int slot);

        // Debugging is fun
        void printDebug();
}