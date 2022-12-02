package asd.protocols.statemachine;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class TtlSet<T> {
    private static record Entry<T>(Instant instant, T value) {
    }

    private final Set<T> set;
    private final Queue<Entry<T>> queue;
    private final Duration ttl;

    public TtlSet(Duration ttl) {
        this.set = new HashSet<>();
        this.queue = new ArrayDeque<>();
        this.ttl = ttl;
    }

    public void set(T value) {
        this.clean();
        if (set.add(value))
            queue.add(new Entry<>(Instant.now(), value));
    }

    public boolean contains(T value) {
        return set.contains(value);
    }

    private void clean() {
        var now = Instant.now();
        while (!queue.isEmpty() && queue.peek().instant().plus(ttl).isBefore(now))
            set.remove(queue.poll().value);
    }
}
