# DIY: Is Graph Bipartite?

## Problem statement

You're given an undirected graph. Determine whether it's bipartite — that is, whether its nodes can be split into two independent groups, A and B, such that every edge connects a node in A to a node in B (never two nodes within the same group).

### Input

```java
// graph = {{1}, {0, 3}, {3}, {1, 2}}
// (node i is connected to every index listed in graph[i])
```

### Output

```java
// true
// nodes split as A = {0, 3}, B = {1, 2}
```

## Coding exercise

Implement `isBipartite(graph)`.

This is exactly [Feature #4: Split Users into Two Groups](04-feature-4-split-users-into-two-groups.md) — the same "following" graph, phrased as the standard, unadorned bipartite-check problem.

## Solution

```java
import java.util.Arrays;

class Solution {
    public static boolean isBipartite(int[][] graph) {
        int n = graph.length;
        int[] color = new int[n];
        Arrays.fill(color, -1);

        for (int i = 0; i < n; i++) {
            if (color[i] == -1) {
                if (!dfs(graph, color, i, 0)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean dfs(int[][] graph, int[] color, int node, int c) {
        color[node] = c;
        for (int neighbor : graph[node]) {
            if (color[neighbor] == color[node]) {
                return false;
            }
            if (color[neighbor] == -1) {
                if (!dfs(graph, color, neighbor, 1 - c)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void main(String[] args) {
        int[][] graph = {{1}, {0, 3}, {3}, {1, 2}};
        System.out.println(isBipartite(graph)); // true
    }
}
```

## Solution walkthrough

We try to greedily 2-color the graph via DFS: color node 0 with color `0`, then every uncolored neighbor gets the opposite color, recursively. Starting from node 0: color 0 gets `0`. Its neighbor 1 gets `1`. Node 1's neighbor 3 gets `0` (opposite of 1's color). Node 3's neighbor 2 gets `1`. No neighbor ever conflicts with an already-assigned color of the same value, so the coloring succeeds — nodes `{0, 3}` end up color `0`, and `{1, 2}` end up color `1`, matching the expected split.

## Complexity measures

Let **n** be the number of nodes and **m** be the number of edges.

### Time Complexity

`O(n + m)` — DFS visits every node once and every edge once from each endpoint.

### Space Complexity

`O(n)` — the `color` array and the DFS recursion stack are each proportional to the number of nodes.
