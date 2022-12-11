package asd.paxos;

import java.util.TreeMap;

public class Configurations {

    private final TreeMap<Integer, Membership> configurations;

    public Configurations() {
        this.configurations = new TreeMap<>();
    }

    public void set(int slot, Membership configuration) {
        var old = this.configurations.put(slot, configuration);
        if (old != null && !configuration.equals(old))
            throw new IllegalStateException("Cannot change configuration at slot " + slot);
    }

    public Membership get(int slot) {
        assert this.contains(slot);
        return this.configurations.get(slot);
    }

    public boolean contains(int slot) {
        // return this.configurations.floorKey(slot) != null; ???
        return this.configurations.containsKey(slot);
    }

    public int size() {
        return this.configurations.size();
    }
}
