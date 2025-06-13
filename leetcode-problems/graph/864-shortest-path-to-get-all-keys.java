/*
 * You are given an m x n grid grid where:
 * 
 * '.' is an empty cell.
 * '#' is a wall.
 * '@' is the starting point.
 * Lowercase letters represent keys.
 * Uppercase letters represent locks.
 * You start at the starting point and one move consists of walking one space in
 * one of the four cardinal directions. You cannot walk outside the grid, or
 * walk into a wall.
 * 
 * If you walk over a key, you can pick it up and you cannot walk over a lock
 * unless you have its corresponding key.
 * 
 * For some 1 <= k <= 6, there is exactly one lowercase and one uppercase letter
 * of the first k letters of the English alphabet in the grid. This means that
 * there is exactly one key for each lock, and one lock for each key; and also
 * that the letters used to represent the keys and locks were chosen in the same
 * order as the English alphabet.
 * 
 * Return the lowest number of moves to acquire all keys. If it is impossible,
 * return -1.
 */

import java.util.*;

class ShortestPathAllKeys {

    // Solution 1: BFS with State (Position + Keys Bitmask)
    public int shortestPathAllKeys(String[] grid) {
        int m = grid.length, n = grid[0].length();

        // Find start position and count total keys
        int startRow = -1, startCol = -1, totalKeys = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                char c = grid[i].charAt(j);
                if (c == '@') {
                    startRow = i;
                    startCol = j;
                } else if (c >= 'a' && c <= 'f') {
                    totalKeys++;
                }
            }
        }

        // BFS with state: (row, col, keys_bitmask)
        Queue<State> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        State start = new State(startRow, startCol, 0, 0);
        queue.offer(start);
        visited.add(start.getKey());

        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        int targetMask = (1 << totalKeys) - 1; // All keys collected

        while (!queue.isEmpty()) {
            State curr = queue.poll();

            // Check if all keys collected
            if (curr.keys == targetMask) {
                return curr.moves;
            }

            // Try all 4 directions
            for (int[] dir : directions) {
                int newRow = curr.row + dir[0];
                int newCol = curr.col + dir[1];

                if (newRow < 0 || newRow >= m || newCol < 0 || newCol >= n) {
                    continue;
                }

                char cell = grid[newRow].charAt(newCol);

                // Cannot pass through walls
                if (cell == '#')
                    continue;

                // Check if we can pass through locks
                if (cell >= 'A' && cell <= 'F') {
                    int lockIndex = cell - 'A';
                    if ((curr.keys & (1 << lockIndex)) == 0) {
                        continue; // Don't have the key
                    }
                }

                // Calculate new key state
                int newKeys = curr.keys;
                if (cell >= 'a' && cell <= 'f') {
                    int keyIndex = cell - 'a';
                    newKeys |= (1 << keyIndex);
                }

                State newState = new State(newRow, newCol, newKeys, curr.moves + 1);
                String stateKey = newState.getKey();

                if (!visited.contains(stateKey)) {
                    visited.add(stateKey);
                    queue.offer(newState);
                }
            }
        }

        return -1; // Cannot collect all keys
    }

    // State class for BFS
    static class State {
        int row, col, keys, moves;

        State(int row, int col, int keys, int moves) {
            this.row = row;
            this.col = col;
            this.keys = keys;
            this.moves = moves;
        }

        String getKey() {
            return row + "," + col + "," + keys;
        }
    }

    // Solution 2: BFS with 3D Visited Array (More Memory Efficient)
    public int shortestPathAllKeysOptimized(String[] grid) {
        int m = grid.length, n = grid[0].length();

        int startRow = -1, startCol = -1, totalKeys = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                char c = grid[i].charAt(j);
                if (c == '@') {
                    startRow = i;
                    startCol = j;
                } else if (c >= 'a' && c <= 'f') {
                    totalKeys++;
                }
            }
        }

        // 3D visited array: [row][col][keys_bitmask]
        boolean[][][] visited = new boolean[m][n][1 << totalKeys];
        Queue<int[]> queue = new LinkedList<>();

        queue.offer(new int[] { startRow, startCol, 0, 0 }); // row, col, keys, moves
        visited[startRow][startCol][0] = true;

        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        int targetMask = (1 << totalKeys) - 1;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1], keys = curr[2], moves = curr[3];

            if (keys == targetMask) {
                return moves;
            }

            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow < 0 || newRow >= m || newCol < 0 || newCol >= n) {
                    continue;
                }

                char cell = grid[newRow].charAt(newCol);
                if (cell == '#')
                    continue;

                // Check locks
                if (cell >= 'A' && cell <= 'F') {
                    int lockIndex = cell - 'A';
                    if ((keys & (1 << lockIndex)) == 0) {
                        continue;
                    }
                }

                // Update keys
                int newKeys = keys;
                if (cell >= 'a' && cell <= 'f') {
                    int keyIndex = cell - 'a';
                    newKeys |= (1 << keyIndex);
                }

                if (!visited[newRow][newCol][newKeys]) {
                    visited[newRow][newCol][newKeys] = true;
                    queue.offer(new int[] { newRow, newCol, newKeys, moves + 1 });
                }
            }
        }

        return -1;
    }

    // Solution 3: A* Algorithm with Heuristic
    public int shortestPathAllKeysAStar(String[] grid) {
        int m = grid.length, n = grid[0].length();

        int startRow = -1, startCol = -1, totalKeys = 0;
        List<int[]> keyPositions = new ArrayList<>();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                char c = grid[i].charAt(j);
                if (c == '@') {
                    startRow = i;
                    startCol = j;
                } else if (c >= 'a' && c <= 'f') {
                    totalKeys++;
                    keyPositions.add(new int[] { i, j, c - 'a' });
                }
            }
        }

        PriorityQueue<AStarState> pq = new PriorityQueue<>((a, b) -> a.fScore - b.fScore);
        Set<String> visited = new HashSet<>();

        int heuristic = calculateHeuristic(startRow, startCol, 0, keyPositions);
        AStarState start = new AStarState(startRow, startCol, 0, 0, heuristic);
        pq.offer(start);
        visited.add(start.getKey());

        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        int targetMask = (1 << totalKeys) - 1;

        while (!pq.isEmpty()) {
            AStarState curr = pq.poll();

            if (curr.keys == targetMask) {
                return curr.gScore;
            }

            for (int[] dir : directions) {
                int newRow = curr.row + dir[0];
                int newCol = curr.col + dir[1];

                if (newRow < 0 || newRow >= m || newCol < 0 || newCol >= n) {
                    continue;
                }

                char cell = grid[newRow].charAt(newCol);
                if (cell == '#')
                    continue;

                if (cell >= 'A' && cell <= 'F') {
                    int lockIndex = cell - 'A';
                    if ((curr.keys & (1 << lockIndex)) == 0) {
                        continue;
                    }
                }

                int newKeys = curr.keys;
                if (cell >= 'a' && cell <= 'f') {
                    int keyIndex = cell - 'a';
                    newKeys |= (1 << keyIndex);
                }

                int newGScore = curr.gScore + 1;
                int newHScore = calculateHeuristic(newRow, newCol, newKeys, keyPositions);
                AStarState newState = new AStarState(newRow, newCol, newKeys, newGScore, newGScore + newHScore);

                String stateKey = newState.getKey();
                if (!visited.contains(stateKey)) {
                    visited.add(stateKey);
                    pq.offer(newState);
                }
            }
        }

        return -1;
    }

    // Calculate heuristic: minimum distance to collect all remaining keys
    private int calculateHeuristic(int row, int col, int keys, List<int[]> keyPositions) {
        int minDist = 0;
        for (int[] keyPos : keyPositions) {
            int keyIndex = keyPos[2];
            if ((keys & (1 << keyIndex)) == 0) { // Key not collected
                minDist += Math.abs(row - keyPos[0]) + Math.abs(col - keyPos[1]);
            }
        }
        return minDist;
    }

    // A* State class
    static class AStarState {
        int row, col, keys, gScore, fScore;

        AStarState(int row, int col, int keys, int gScore, int fScore) {
            this.row = row;
            this.col = col;
            this.keys = keys;
            this.gScore = gScore;
            this.fScore = fScore;
        }

        String getKey() {
            return row + "," + col + "," + keys;
        }
    }

    // Solution 4: Dijkstra's Algorithm
    public int shortestPathAllKeysDijkstra(String[] grid) {
        int m = grid.length, n = grid[0].length();

        int startRow = -1, startCol = -1, totalKeys = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                char c = grid[i].charAt(j);
                if (c == '@') {
                    startRow = i;
                    startCol = j;
                } else if (c >= 'a' && c <= 'f') {
                    totalKeys++;
                }
            }
        }

        // Priority queue for Dijkstra
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[3] - b[3]);
        Set<String> visited = new HashSet<>();

        pq.offer(new int[] { startRow, startCol, 0, 0 }); // row, col, keys, distance

        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        int targetMask = (1 << totalKeys) - 1;

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int row = curr[0], col = curr[1], keys = curr[2], dist = curr[3];

            String stateKey = row + "," + col + "," + keys;
            if (visited.contains(stateKey))
                continue;
            visited.add(stateKey);

            if (keys == targetMask) {
                return dist;
            }

            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow < 0 || newRow >= m || newCol < 0 || newCol >= n) {
                    continue;
                }

                char cell = grid[newRow].charAt(newCol);
                if (cell == '#')
                    continue;

                if (cell >= 'A' && cell <= 'F') {
                    int lockIndex = cell - 'A';
                    if ((keys & (1 << lockIndex)) == 0) {
                        continue;
                    }
                }

                int newKeys = keys;
                if (cell >= 'a' && cell <= 'f') {
                    int keyIndex = cell - 'a';
                    newKeys |= (1 << keyIndex);
                }

                String newStateKey = newRow + "," + newCol + "," + newKeys;
                if (!visited.contains(newStateKey)) {
                    pq.offer(new int[] { newRow, newCol, newKeys, dist + 1 });
                }
            }
        }

        return -1;
    }

    // Test method
    public static void main(String[] args) {
        ShortestPathAllKeys solution = new ShortestPathAllKeys();

        // Test case 1
        String[] grid1 = { "@.a.#", "###.#", "b.A.B" };
        System.out.println("Test 1 - Expected: 8, Got: " +
                solution.shortestPathAllKeys(grid1));

        // Test case 2
        String[] grid2 = { "@..aA", "..B#.", "....b" };
        System.out.println("Test 2 - Expected: 6, Got: " +
                solution.shortestPathAllKeys(grid2));

        // Test case 3
        String[] grid3 = { "@Aa" };
        System.out.println("Test 3 - Expected: -1, Got: " +
                solution.shortestPathAllKeys(grid3));

        // Test case 4 - Complex grid
        String[] grid4 = {
                "@...a",
                "..#..",
                ".....",
                "....A"
        };
        System.out.println("Test 4 - Complex: " +
                solution.shortestPathAllKeys(grid4));

        // Performance comparison
        System.out.println("\nPerformance Comparison:");
        String[] largeGrid = {
                "@.a.#.b.#.c",
                "###.#.#.#.#",
                "A.B.#.C.#.#",
                "#.#.#.#.#.#",
                "#.#.#.#.#.#"
        };

        long start = System.currentTimeMillis();
        int result1 = solution.shortestPathAllKeys(largeGrid);
        long time1 = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        int result2 = solution.shortestPathAllKeysOptimized(largeGrid);
        long time2 = System.currentTimeMillis() - start;

        System.out.println("Standard BFS: " + result1 + " (Time: " + time1 + "ms)");
        System.out.println("Optimized BFS: " + result2 + " (Time: " + time2 + "ms)");
    }

}

/*
 * BITMASKING EXPLANATION FOR KEY COLLECTION AND LOCK CHECKING:
 * 
 * In this problem, we must track which keys ('a' to 'f') we collect on a grid.
 * Instead of using a Set to store keys (which is slow to compare and memory-heavy),
 * we use an integer bitmask. Each of the 6 least significant bits (bits 0 to 5)
 * represents whether we have a specific key.
 *
 * Each bit position corresponds to a key:
 *     Bit 0 → 'a' → (1 << 0) → binary: 000001
 *     Bit 1 → 'b' → (1 << 1) → binary: 000010
 *     Bit 2 → 'c' → (1 << 2) → binary: 000100
 *     Bit 3 → 'd' → (1 << 3) → binary: 001000
 *     Bit 4 → 'e' → (1 << 4) → binary: 010000
 *     Bit 5 → 'f' → (1 << 5) → binary: 100000
 *
 * ───────────────────────────────────────────────────────────────
 * 1. COLLECTING A KEY:
 * ───────────────────────────────────────────────────────────────
 * When we step on a key cell like 'b', we update our bitmask by turning on the corresponding bit:
 *     keys |= (1 << ('b' - 'a'));
 * Example:
 *     keys = 000001 (only 'a')
 *     now we collect 'b' → set bit 1 → keys = 000011 (binary) → we now have 'a' and 'b'
 *
 * ───────────────────────────────────────────────────────────────
 * 2. CHECKING IF WE HAVE A KEY FOR A LOCK:
 * ───────────────────────────────────────────────────────────────
 * When we step on a lock like 'B', we can only pass through it if we have the matching key 'b'.
 * To check this, we look at the corresponding bit in our keys bitmask:
 *     (keys & (1 << ('B' - 'A'))) != 0
 *
 * Here's what this does:
 * - 'B' - 'A' = 1 → refers to bit 1 (key 'b')
 * - (1 << 1) = 000010
 * - If (keys & 000010) is not zero → it means we already have the 'b' key
 *
 * Example:
 *     Suppose keys = 000011 → this means we have 'a' (bit 0) and 'b' (bit 1)
 *     If we're on lock 'B':
 *         (keys & (1 << ('B' - 'A'))) → (000011 & 000010) = 000010 → valid (non-zero)
 *         We can open lock 'B'
 *
 *     If we’re on lock 'C':
 *         (keys & (1 << ('C' - 'A'))) → (000011 & 000100) = 000000 → invalid (zero)
 *         We cannot open lock 'C' because we don’t have key 'c'
 *
 * ───────────────────────────────────────────────────────────────
 * 3. CHECKING IF ALL KEYS ARE COLLECTED:
 * ───────────────────────────────────────────────────────────────
 * At the start, we calculate a totalKeys mask by setting bits for every key present in the grid.
 * For example, if keys 'a', 'b', and 'd' are in the grid:
 *     totalKeys = (1 << 0) | (1 << 1) | (1 << 3) = 00001101 (binary)
 * 
 * During traversal, once our current keys bitmask matches totalKeys:
 *     keys == totalKeys → we have collected all required keys → return the steps
 *
 * ───────────────────────────────────────────────────────────────
 * WHY BITMASKING?
 * ───────────────────────────────────────────────────────────────
 * - Efficient: just an int to represent up to 6 booleans (on/off)
 * - Fast comparisons: (keys == totalKeys) is quicker than comparing sets
 * - Easy to use with visited states: visited[row][col][keyMask]
 * - Only 64 (2⁶) key combinations → memory usage stays small
 */
