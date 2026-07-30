# DIY: Walls and Gates

## Problem statement

You are given an `m x n` grid, `rooms`, where each cell is one of:

- `-1` — a wall or obstacle
- `0` — a gate
- `2147483647` (`Integer.MAX_VALUE`, used as `INF`) — an empty room

Fill each empty room with the distance to its nearest gate. If a gate is unreachable from a room, leave that room as `INF`.

### Input

```java
rooms = [
    [2147483647, -1, 0, 2147483647],
    [2147483647, 2147483647, 2147483647, -1],
    [2147483647, -1, 2147483647, -1],
    [0, -1, 2147483647, 2147483647]
]
```

### Output

```java
[
    [3, -1, 0, 1],
    [2, 2, 1, -1],
    [1, -1, 2, -1],
    [0, -1, 3, 4]
]
```

## Coding exercise

Implement `wallsAndGates(rooms)`, modifying the grid in place.

This is the exact same pattern as [Feature #12: Warehouse and Drop Points](12-feature-12-warehouse-and-drop-points.md) — there, a warehouse robot needed the shortest trip from open floor space to the nearest drop point; here it's the bare pattern with no story attached. Instead of running a BFS from each empty room outward (slow, and it recomputes overlapping distances), start a single multi-source BFS from *all* the gates at once, pushed into the queue together. The first time BFS reaches any empty room, that's guaranteed to be its shortest distance to the nearest gate, since BFS explores in order of distance.

## Solution

```java
import java.util.*;

class Solution {
    public static void wallsAndGates(int[][] rooms) {
        if (rooms.length == 0) return;
        int m = rooms.length, n = rooms[0].length;

        // Seed the BFS queue with every gate at once — this is what makes it
        // "multi-source": each room's first visit is from its nearest gate.
        Queue<int[]> queue = new LinkedList<>();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (rooms[i][j] == 0) {
                    queue.offer(new int[]{i, j});
                }
            }
        }

        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            for (int[] dir : directions) {
                int ni = cell[0] + dir[0];
                int nj = cell[1] + dir[1];
                // Only expand into untouched empty rooms — walls and
                // already-visited rooms are skipped.
                if (ni >= 0 && ni < m && nj >= 0 && nj < n && rooms[ni][nj] == Integer.MAX_VALUE) {
                    rooms[ni][nj] = rooms[cell[0]][cell[1]] + 1;
                    queue.offer(new int[]{ni, nj});
                }
            }
        }
    }

    public static void main(String[] args) {
        int INF = Integer.MAX_VALUE;
        int[][] rooms = {
            {INF, -1, 0, INF},
            {INF, INF, INF, -1},
            {INF, -1, INF, -1},
            {0, -1, INF, INF}
        };

        wallsAndGates(rooms);
        for (int[] row : rooms) {
            System.out.println(Arrays.toString(row));
        }
        // [3, -1, 0, 1]
        // [2, 2, 1, -1]
        // [1, -1, 2, -1]
        // [0, -1, 3, 4]
    }
}
```

## Complexity measures

Let **m x n** be the size of the grid.

- **Time:** `O(m * n)` — every cell enters the BFS queue and is expanded at most once.
- **Space:** `O(m * n)` — the queue can hold up to every cell in the grid in the worst case.
