# DIY: Course Schedule

## Problem statement

There are `numCourses` courses, labeled `0` to `numCourses - 1`. You're given `prerequisites`, where `prerequisites[i] = [ai, bi]` means you must take course `bi` before course `ai`.

Return `true` if it's possible to finish all courses, `false` otherwise.

### Input

```java
numCourses = 3
prerequisites = [[1, 0], [2, 1]]
```

### Output

```java
true
```

## Coding exercise

Implement `canFinish(numCourses, prerequisites)`.

This is the exact same pattern as [Feature #3: Schedule Processes](03-feature-3-schedule-processes.md) — there, the OS needed to find a valid run order given process dependencies; here we only need to know whether a valid order *exists at all*, not what it is. The approach is the same Kahn's-algorithm BFS: build the graph and in-degree counts, repeatedly peel off vertices with in-degree `0`, and check whether every vertex eventually got peeled off. If any are left stuck with a nonzero in-degree at the end, there's a cycle and the courses can't all be finished.

## Solution

```java
import java.util.*;

class Solution {
    public static boolean canFinish(int numCourses, int[][] prerequisites) {
        Map<Integer, List<Integer>> graph = new HashMap<>();
        int[] inDegree = new int[numCourses];
        for (int i = 0; i < numCourses; i++) graph.put(i, new ArrayList<>());

        for (int[] p : prerequisites) {
            int course = p[0], pre = p[1];
            graph.get(pre).add(course);
            inDegree[course]++;
        }

        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < numCourses; i++) {
            if (inDegree[i] == 0) queue.add(i);
        }

        int visited = 0;
        while (!queue.isEmpty()) {
            int course = queue.poll();
            visited++;
            for (int next : graph.get(course)) {
                if (--inDegree[next] == 0) queue.add(next);
            }
        }
        return visited == numCourses;
    }

    public static void main(String[] args) {
        System.out.println(canFinish(3, new int[][]{{1, 0}, {2, 1}}));
        // true
        System.out.println(canFinish(2, new int[][]{{1, 0}, {0, 1}}));
        // false (cycle: 0 needs 1, 1 needs 0)
    }
}
```

We run the same BFS topological sort as the feature, but instead of building an ordered list, we just count how many courses got peeled off (`visited`). If a cycle exists somewhere, the courses inside it never reach an in-degree of `0`, so `visited` ends up less than `numCourses` — that's our signal to return `false`.

## Complexity measures

Let **V** be `numCourses` and **E** be the number of prerequisite pairs.

- **Time:** `O(V + E)` — every vertex is processed once and every edge is traversed once.
- **Space:** `O(V + E)` — the adjacency list and in-degree array.
