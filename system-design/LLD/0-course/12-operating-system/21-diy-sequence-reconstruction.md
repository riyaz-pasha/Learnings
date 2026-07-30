# DIY: Sequence Reconstruction

## Problem statement

Check whether the original sequence `org` — a permutation of the integers `1` to `n` — can be **uniquely** reconstructed from a list of shorter sequences `seqs`. Reconstruction means building the shortest common supersequence of all the sequences in `seqs` (the shortest sequence for which every sequence in `seqs` is a subsequence). Return whether exactly one such sequence exists, and that it matches `org`.

### Input

```java
org = {1, 2, 3}
seqs = {{1, 2}, {1, 3}, {2, 3}}
```

### Output

```java
true
```

(`{1,2}`, `{1,3}`, and `{2,3}` are all subsequences of `org`, and `org` is the only sequence they can be reconstructed into.)

## Coding exercise

Implement `sequenceReconstruction(org, seqs)`.

This is the same underlying pattern as [Feature #3: Schedule Processes](03-feature-3-schedule-processes.md), pushed one step further: each pair of adjacent numbers in a `seqs` entry is a dependency edge (`seqs[i][j]` must come before `seqs[i][j+1]`), so this is topological sort again — but now we need to verify the topological order isn't just *valid*, it's the *only* valid one, and that it equals `org`.

## Solution

```java
import java.util.*;

class Solution {
    public static boolean sequenceReconstruction(int[] org, int[][] seqs) {
        Map<Integer, Set<Integer>> graph = new HashMap<>();
        Map<Integer, Integer> inDegree = new HashMap<>();

        for (int[] seq : seqs) {
            for (int num : seq) {
                graph.putIfAbsent(num, new HashSet<>());
                inDegree.putIfAbsent(num, 0);
            }
        }
        if (graph.size() != org.length) return false;

        for (int[] seq : seqs) {
            for (int i = 0; i < seq.length - 1; i++) {
                int parent = seq[i], child = seq[i + 1];
                if (graph.get(parent).add(child)) {
                    inDegree.put(child, inDegree.get(child) + 1);
                }
            }
        }

        Queue<Integer> queue = new LinkedList<>();
        for (int key : inDegree.keySet()) {
            if (inDegree.get(key) == 0) queue.add(key);
        }

        int idx = 0;
        while (!queue.isEmpty()) {
            if (queue.size() > 1) return false; // more than one candidate = not unique.
            int node = queue.poll();
            if (idx >= org.length || org[idx] != node) return false;
            idx++;
            for (int child : graph.get(node)) {
                inDegree.put(child, inDegree.get(child) - 1);
                if (inDegree.get(child) == 0) queue.add(child);
            }
        }
        return idx == org.length;
    }

    public static void main(String[] args) {
        int[] org = {1, 2, 3};
        int[][] seqs = {{1, 2}, {1, 3}, {2, 3}};
        System.out.println(sequenceReconstruction(org, seqs));
        // true
    }
}
```

Same Kahn's-algorithm BFS as before, but with two extra checks folded in: the queue must never hold more than one ready-to-process node at a time (if it does, there are two valid next choices, so the order isn't unique), and the node popped at each step must match `org` at that same position — any mismatch immediately disqualifies `org` as the reconstruction.

## Complexity measures

Let **n** be the length of `org` and **m** be the total number of elements across all sequences in `seqs`.

- **Time:** `O(n + m)` — building the graph and running the BFS both scale with the total number of edges and nodes.
- **Space:** `O(n + m)` — the graph, in-degree map, and queue.
