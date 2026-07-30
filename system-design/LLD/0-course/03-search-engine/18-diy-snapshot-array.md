# DIY: Snapshot Array

## Problem statement

Implement `SnapshotArray`:

- `SnapshotArray(length)` — initialize `length` indices, all starting at `0`.
- `set(idx, val)` — set index `idx` to `val`.
- `snapshot()` — save the current state, return `snapId` (the count of snapshots taken so far, minus 1 — i.e. the first snapshot is id `0`).
- `get(idx, snapId)` — return index `idx`'s value as of that snapshot.

### Input

```java
SnapshotArray snapshotArr = new SnapshotArray(3);
snapshotArr.set(0, 4);
snapshotArr.snapshot();
snapshotArr.get(0, 0);
snapshotArr.set(1, 6);
snapshotArr.snapshot();
snapshotArr.get(1, 1);
```

### Output

```java
4
6
```

## Coding exercise

Implement the `SnapshotArray` class.

Exactly [Feature #8: Distributed Process Coordinator](08-feature-8-distributed-process-coordinator.md) — a `current` map for live values and a list of map snapshots taken on demand.

## Solution

```java
import java.util.*;

class SnapshotArray {
    private final Map<Integer, Integer> current;
    private final List<Map<Integer, Integer>> snapshots;

    public SnapshotArray(int length) {
        current = new HashMap<>();
        snapshots = new ArrayList<>();
    }

    public void set(int idx, int val) {
        current.put(idx, val);
    }

    public int snapshot() {
        snapshots.add(new HashMap<>(current));
        return snapshots.size() - 1;
    }

    public int get(int idx, int snapId) {
        return snapshots.get(snapId).getOrDefault(idx, 0);
    }

    public static void main(String[] args) {
        SnapshotArray snapshotArr = new SnapshotArray(3);
        snapshotArr.set(0, 4);
        snapshotArr.snapshot();
        System.out.println(snapshotArr.get(0, 0)); // 4

        snapshotArr.set(1, 6);
        snapshotArr.snapshot();
        System.out.println(snapshotArr.get(1, 1)); // 6
    }
}
```

## Complexity measures

Let **n** be `length` and **m** be the number of snapshots.

| Method | Time |
|---|---|
| `set()` | `O(1)` |
| `snapshot()` | `O(n)` |
| `get()` | `O(1)` |

### Space Complexity

`O(n × m)`.
