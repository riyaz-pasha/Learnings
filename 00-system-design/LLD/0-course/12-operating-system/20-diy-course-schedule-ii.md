# DIY: Course Schedule II

## Problem statement

There are `n` courses labeled `0` to `n - 1`. Given the total number of courses `n` and an array of prerequisite pairs, where `prerequisites[i] = [ai, bi]` means course `bi` must be taken before course `ai`, return an order in which one could take all the courses.

### Input

```java
n = 4
prerequisites = {{1, 0}, {2, 0}, {3, 1}, {3, 2}}
```

### Output

```java
{0, 2, 1, 3}
```

(Another equally valid ordering is `{0, 1, 2, 3}`.)

## Coding exercise

Implement `findOrder(n, prerequisites)`.

This is the exact same pattern as [Feature #3: Schedule Processes](03-feature-3-schedule-processes.md) — there, the OS needed to find a valid process run order given dependency pairs; here it's the identical bare topological-sort problem. The approach is the same Kahn's-algorithm BFS: build the graph and in-degree counts, repeatedly peel off vertices with in-degree `0` into the result order, and return an empty result if a cycle prevents all courses from being peeled off.

## Solution

```java
import java.util.*;

class Solution {
    public static int[] findOrder(int n, int[][] prerequisites) {
        Map<Integer, List<Integer>> graph = new HashMap<>();
        int[] inDegree = new int[n];
        for (int i = 0; i < n; i++) graph.put(i, new ArrayList<>());

        for (int[] p : prerequisites) {
            int course = p[0], pre = p[1];
            graph.get(pre).add(course);
            inDegree[course]++;
        }

        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) queue.add(i);
        }

        int[] order = new int[n];
        int idx = 0;
        while (!queue.isEmpty()) {
            int course = queue.poll();
            order[idx++] = course;
            for (int next : graph.get(course)) {
                if (--inDegree[next] == 0) queue.add(next);
            }
        }

        return idx == n ? order : new int[0]; // empty array signals a cycle.
    }

    public static void main(String[] args) {
        int[] order = findOrder(4, new int[][]{{1, 0}, {2, 0}, {3, 1}, {3, 2}});
        System.out.println(Arrays.toString(order));
        // [0, 1, 2, 3]
    }
}
```

This is the same BFS topological sort from Feature #3, just with courses instead of processes: we peel off every vertex with in-degree `0`, append it to the result, and decrement its neighbors' in-degrees, repeating until the queue empties. If not every course got peeled off, a cycle exists and no valid order is possible.

## Complexity measures

Let **V** be `n` and **E** be the number of prerequisite pairs.

- **Time:** `O(V + E)` — every vertex and edge is processed exactly once.
- **Space:** `O(V + E)` — the adjacency list, in-degree array, and result array.
