# DIY: Number of Provinces

## Problem statement

There are `n` cities. If city `c1` is connected to `c2`, and `c2` is connected to `c3`, then `c1` and `c3` are indirectly connected. A **province** is a group of directly or indirectly connected cities, with no other city belonging to it.

Given an `n x n` matrix `isConnected` where `isConnected[i][j] == 1` means cities `i` and `j` are directly connected, return the total number of provinces.

**Constraints:** `1 <= n <= 200`, `isConnected[i][i] == 1`, `isConnected[i][j] == isConnected[j][i]`.

### Input

```java
// Sample 1:
isConnected = [[1,1,0],[1,1,0],[0,0,1]]

// Sample 2:
isConnected = [[1,0,0],[0,1,0],[0,0,1]]
```

### Output

```java
// Sample 1:
2

// Sample 2:
3
```

## Coding exercise

Implement `findProvincesNum(isConnected)`.

This is the exact algorithm from [Feature #1: Friend Circles](01-feature-1-friend-circles.md) — "province" is just another word for "connected component," same as a friend circle.

## Solution

```java
class Solution {

    public static int findProvincesNum(int[][] isConnected) {
        int n = isConnected.length;
        boolean[] visited = new boolean[n];
        int provinces = 0;

        for (int city = 0; city < n; city++) {
            if (!visited[city]) {
                visited[city] = true;
                dfs(isConnected, n, visited, city);
                provinces++;
            }
        }

        return provinces;
    }

    private static void dfs(int[][] isConnected, int n, boolean[] visited, int city) {
        for (int neighbor = 0; neighbor < n; neighbor++) {
            if (isConnected[city][neighbor] == 1 && !visited[neighbor] && neighbor != city) {
                visited[neighbor] = true;
                dfs(isConnected, n, visited, neighbor);
            }
        }
    }

    public static void main(String[] args) {
        int[][] sample1 = {{1, 1, 0}, {1, 1, 0}, {0, 0, 1}};
        System.out.println(findProvincesNum(sample1)); // 2

        int[][] sample2 = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        System.out.println(findProvincesNum(sample2)); // 3
    }
}
```

## Complexity measures

Let **n** be the number of cities.

- **Time:** `O(n²)` — every cell of the matrix is examined a constant number of times across all DFS calls.
- **Space:** `O(n)` — the `visited` array plus recursion depth.
