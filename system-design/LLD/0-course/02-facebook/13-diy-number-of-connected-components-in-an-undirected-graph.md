# DIY: Number of Connected Components in an Undirected Graph

## Problem statement

Given an undirected graph with `n` nodes and an edge list `edges` (where `edges[i] = [xi, yi]` means an edge between `xi` and `yi`), return the number of connected components.

**Constraints:** `edges[i].length == 2`, `0 <= xi <= yi < n`, `xi != yi`, no repeated edges.

### Input

```java
// Sample 1:
edges = [[0,1],[1,2],[3,4]], n = 5

// Sample 2:
edges = [[0,1],[1,2],[2,3],[3,4]], n = 5
```

### Output

```java
// Sample 1:
2

// Sample 2:
1
```

## Coding exercise

Implement `countConnectedComp(edges, n)`.

Same idea as [Feature #1: Friend Circles](01-feature-1-friend-circles.md) — count connected components with DFS. The only difference: the graph here arrives as an **edge list**, not an adjacency matrix, so build an adjacency list first.

## Solution

```java
import java.util.*;

class Solution {

    public static int countConnectedComp(int[][] edges, int n) {
        List<List<Integer>> adjacency = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjacency.add(new ArrayList<>());
        }
        for (int[] edge : edges) {
            adjacency.get(edge[0]).add(edge[1]);
            adjacency.get(edge[1]).add(edge[0]);
        }

        boolean[] visited = new boolean[n];
        int components = 0;

        for (int node = 0; node < n; node++) {
            if (!visited[node]) {
                visited[node] = true;
                dfs(adjacency, visited, node);
                components++;
            }
        }

        return components;
    }

    private static void dfs(List<List<Integer>> adjacency, boolean[] visited, int node) {
        for (int neighbor : adjacency.get(node)) {
            if (!visited[neighbor]) {
                visited[neighbor] = true;
                dfs(adjacency, visited, neighbor);
            }
        }
    }

    public static void main(String[] args) {
        int[][] sample1 = {{0, 1}, {1, 2}, {3, 4}};
        System.out.println(countConnectedComp(sample1, 5)); // 2

        int[][] sample2 = {{0, 1}, {1, 2}, {2, 3}, {3, 4}};
        System.out.println(countConnectedComp(sample2, 5)); // 1
    }
}
```

## Complexity measures

Let **n** be the number of nodes and **e** be the number of edges.

- **Time:** `O(n + e)` — building the adjacency list is `O(e)`, and DFS visits every node and edge once.
- **Space:** `O(n + e)` — the adjacency list plus the `visited` array.
