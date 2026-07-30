# DIY: Jump Game IV

## Problem statement

You're given an array `arr`. A pointer starts at index 0. Find the minimum number of steps needed to reach the last index, where from index `i` the pointer can move to:

- `i + 1`, if `i + 1 < arr.length`.
- `i - 1`, if `i - 1 >= 0`.
- Any index `j` where `arr[i] == arr[j]` and `i != j`.

### Input

```java
arr = {23, 11, 44, 5, 6, 9, 11, 16}
```

### Output

```
3
```

## Coding exercise

Implement `jumpGame(arr)`, returning the minimum number of steps from index 0 to the last index.

This is the exact same pattern as [Feature #3: Meeting Activity](03-feature-3-meeting-activity.md) — there, Zoom's mini-game found the minimum jumps for a sprite climbing a staircase; here it's the bare pattern, same rules, no story. Model indices as graph nodes, group same-valued indices with a hash map for O(1) lookup, and run BFS from index 0 — the first time you reach the last index is the minimum step count.

## Solution

```java
import java.util.*;

class Solution {
    public static int jumpGame(int[] arr) {
        int n = arr.length;
        if (n <= 1) {
            return 0;
        }

        Map<Integer, List<Integer>> valueToIndices = new HashMap<>();
        for (int i = 0; i < n; i++) {
            valueToIndices.computeIfAbsent(arr[i], v -> new ArrayList<>()).add(i);
        }

        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(0);
        boolean[] visited = new boolean[n];
        visited[0] = true;
        int step = 0;

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            for (int s = 0; s < levelSize; s++) {
                int node = queue.poll();
                if (node == n - 1) {
                    return step;
                }

                List<Integer> neighbors = new ArrayList<>();
                if (valueToIndices.containsKey(arr[node])) {
                    neighbors.addAll(valueToIndices.get(arr[node]));
                    valueToIndices.remove(arr[node]); // fully explored, avoid reprocessing
                }
                if (node + 1 < n) neighbors.add(node + 1);
                if (node - 1 >= 0) neighbors.add(node - 1);

                for (int next : neighbors) {
                    if (!visited[next]) {
                        visited[next] = true;
                        queue.add(next);
                    }
                }
            }
            step++;
        }
        return -1;
    }

    public static void main(String[] args) {
        int[] arr = {23, 11, 44, 5, 6, 9, 11, 16};
        System.out.println(jumpGame(arr));
        // 3
    }
}
```

## Complexity measures

Let **n** be the length of `arr`.

- **Time:** `O(n)` — every index is enqueued at most once, and each value group in the hash map is expanded exactly once, since it's deleted right after use.
- **Space:** `O(n)` — the hash map, visited array, and BFS queue each hold at most n entries.
