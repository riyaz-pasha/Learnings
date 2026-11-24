import java.util.*;

class Solution {

    /*
     * Represents a candidate cell in the A* search.
     *
     * row, col = Current cell coordinates.
     *
     * g = Actual cost from the starting cell to this cell.
     *     It counts edges/moves, not visited cells.
     *
     * h = Estimated remaining cost to the destination.
     *
     * f = g + h.
     *     This is the priority used by A*.
     *
     * Records are immutable data carriers, making them
     * convenient for storing states in a PriorityQueue.
     */
    private record State(
            int row,
            int col,
            int g,
            int h,
            int f
    ) {}

    /*
     * All eight possible movement directions.
     *
     * Each move has a cost of exactly 1.
     */
    private static final int[][] DIRS = {
        {-1, -1}, {-1, 0}, {-1, 1},
        { 0, -1},           { 0, 1},
        { 1, -1}, { 1, 0},  { 1, 1}
    };

    public int shortestPathBinaryMatrix(int[][] grid) {

        int n = grid.length;

        /*
         * A valid path requires both endpoints to be open.
         *
         * This also handles the n = 1 case:
         * grid = [[0]] -> answer 1
         * grid = [[1]] -> answer -1
         */
        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) {
            return -1;
        }

        /*
         * bestG[r][c] stores the smallest known number
         * of moves required to reach cell (r,c).
         *
         * Initially, every cell is unreachable.
         */
        int[][] bestG = new int[n][n];

        for (int[] row : bestG) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        /*
         * A* uses a min-priority queue.
         *
         * The cell with the smallest f = g + h
         * is expanded first.
         *
         * Tie-breaking:
         * If two cells have the same f, prefer the one
         * with the smaller h (closer estimated distance).
         *
         * This tie-breaking is optional and does not affect
         * the correctness of A*.
         */
        PriorityQueue<State> pq = new PriorityQueue<>(
            Comparator.comparingInt(State::f)
                      .thenComparingInt(State::h)
        );

        /*
         * Starting cell:
         *
         * g = 0 because we have made zero moves.
         *
         * h = Chebyshev distance to the destination.
         *
         * f = g + h.
         */
        int startH = heuristic(0, 0, n - 1, n - 1);

        bestG[0][0] = 0;

        pq.offer(new State(
            0,
            0,
            0,
            startH,
            startH
        ));

        /*
         * Continue while there are promising cells to explore.
         */
        while (!pq.isEmpty()) {

            /*
             * Extract the cell with the smallest estimated
             * total path cost.
             */
            State current = pq.poll();

            int row = current.row();
            int col = current.col();
            int g = current.g();

            /*
             * Skip outdated states.
             *
             * A cell might have been inserted into the priority
             * queue multiple times with different g values.
             *
             * If a better path was discovered later, bestG
             * contains the improved value.
             *
             * This old queue entry must not be expanded.
             */
            if (g > bestG[row][col]) {
                continue;
            }

            /*
             * Destination reached.
             *
             * Because Chebyshev distance is consistent,
             * the first non-outdated destination state
             * removed from the priority queue has the
             * optimal path cost.
             *
             * g counts moves/edges.
             * The problem asks for visited cells.
             *
             * Therefore, return g + 1.
             */
            if (row == n - 1 && col == n - 1) {
                return g + 1;
            }

            /*
             * Explore all eight neighboring cells.
             */
            for (int[] dir : DIRS) {

                int nr = row + dir[0];
                int nc = col + dir[1];

                /*
                 * Ignore cells outside matrix boundaries.
                 */
                if (nr < 0 || nc < 0 || nr >= n || nc >= n) {
                    continue;
                }

                /*
                 * Ignore blocked cells.
                 *
                 * A* can only travel through cells containing 0.
                 */
                if (grid[nr][nc] == 1) {
                    continue;
                }

                /*
                 * Every movement costs exactly 1.
                 *
                 * Therefore, the new path cost is:
                 *
                 * newG = currentG + 1
                 */
                int newG = g + 1;

                /*
                 * Relaxation:
                 *
                 * Only consider this neighbor if we found
                 * a strictly better path to it.
                 *
                 * If newG >= bestG[nr][nc], the existing path
                 * is already equally good or better.
                 */
                if (newG >= bestG[nr][nc]) {
                    continue;
                }

                /*
                 * Update the best known distance.
                 */
                bestG[nr][nc] = newG;

                /*
                 * Estimate remaining distance using Chebyshev.
                 *
                 * Since all eight directions cost 1:
                 *
                 * h = max(rowDifference, colDifference)
                 */
                int newH = heuristic(
                    nr,
                    nc,
                    n - 1,
                    n - 1
                );

                /*
                 * Estimated total path cost:
                 *
                 * f = actual cost + estimated remaining cost
                 */
                int newF = newG + newH;

                /*
                 * Insert the improved state.
                 *
                 * PriorityQueue automatically arranges states
                 * according to the smallest f value.
                 */
                pq.offer(new State(
                    nr,
                    nc,
                    newG,
                    newH,
                    newF
                ));
            }
        }

        /*
         * No reachable path to the destination was found.
         */
        return -1;
    }

    /*
     * Chebyshev distance heuristic.
     *
     * This is the minimum number of moves required to reach
     * the destination if there are no obstacles.
     *
     * Why max?
     *
     * A diagonal move reduces both row and column differences
     * simultaneously by one.
     *
     * Once one difference becomes zero, the remaining moves
     * are straight movements.
     *
     * Therefore:
     *
     * h = max(abs(row - targetRow),
     *         abs(col - targetCol))
     *
     * This heuristic is:
     * 1. Admissible: Never overestimates actual remaining cost.
     * 2. Consistent: Satisfies h(u) <= cost(u,v) + h(v).
     */
    private int heuristic(
            int row,
            int col,
            int targetRow,
            int targetCol) {

        return Math.max(
            Math.abs(row - targetRow),
            Math.abs(col - targetCol)
        );
    }
}


























import java.util.*;

class Solution {

    // Each BFS state stores the current cell and its path length.
    private record Cell(int row, int col, int distance) {}

    private static final int[][] DIRS = {
        {-1, -1}, {-1, 0}, {-1, 1},
        { 0, -1},           { 0, 1},
        { 1, -1}, { 1, 0},  { 1, 1}
    };

    public int shortestPathBinaryMatrix(int[][] grid) {

        int n = grid.length;

        // Starting or ending cell blocked means no valid path exists.
        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) {
            return -1;
        }

        // Tracks whether a cell has already entered the BFS queue.
        boolean[][] visited = new boolean[n][n];

        // FIFO queue guarantees exploration in increasing path length.
        Queue<Cell> queue = new ArrayDeque<>();

        // Initial path length is 1 because the starting cell is counted.
        queue.offer(new Cell(0, 0, 1));
        visited[0][0] = true;

        while (!queue.isEmpty()) {

            Cell current = queue.poll();

            int row = current.row();
            int col = current.col();
            int distance = current.distance();

            // BFS explores shortest paths first.
            // Therefore, the first time we reach the destination,
            // its distance is guaranteed to be optimal.
            if (row == n - 1 && col == n - 1) {
                return distance;
            }

            // Explore all eight neighboring cells.
            for (int[] dir : DIRS) {

                int nr = row + dir[0];
                int nc = col + dir[1];

                // Check matrix boundaries.
                if (nr < 0 || nc < 0 || nr >= n || nc >= n) {
                    continue;
                }

                // Skip blocked cells and cells already discovered.
                if (grid[nr][nc] == 1 || visited[nr][nc]) {
                    continue;
                }

                // Mark when ENQUEUING, not when dequeuing.
                // This prevents the same cell from entering the queue
                // multiple times through different neighboring cells.
                visited[nr][nc] = true;

                // Moving to a neighboring cell increases path length by 1.
                queue.offer(new Cell(nr, nc, distance + 1));
            }
        }

        // Queue exhausted: destination cannot be reached.
        return -1;
    }
}














import java.util.*;

class Solution {

    // A record keeps the BFS state immutable and concise.
    private record Cell(int row, int col, int distance) {}

    // All eight possible movement directions.
    private static final int[][] DIRS = {
        {-1, -1}, {-1, 0}, {-1, 1},
        { 0, -1},           { 0, 1},
        { 1, -1}, { 1, 0},  { 1, 1}
    };

    public int shortestPathBinaryMatrix(int[][] grid) {

        int n = grid.length;

        // Both endpoints must be open.
        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) {
            return -1;
        }

        Queue<Cell> queue = new ArrayDeque<>();

        // Starting cell counts as the first visited cell.
        queue.offer(new Cell(0, 0, 1));

        // Mark the starting cell as visited.
        // This prevents it from being added again.
        grid[0][0] = 1;

        while (!queue.isEmpty()) {

            Cell current = queue.poll();

            int row = current.row();
            int col = current.col();
            int distance = current.distance();

            // First arrival at the destination gives the shortest path.
            if (row == n - 1 && col == n - 1) {
                return distance;
            }

            // Explore all eight neighboring cells.
            for (int[] dir : DIRS) {

                int nr = row + dir[0];
                int nc = col + dir[1];

                // Ignore cells outside the matrix.
                if (nr < 0 || nc < 0 || nr >= n || nc >= n) {
                    continue;
                }

                // A value of 1 means either blocked or already visited.
                if (grid[nr][nc] == 1) {
                    continue;
                }

                // Mark visited immediately when adding to the queue.
                // This avoids duplicate queue entries.
                grid[nr][nc] = 1;

                // Every move adds exactly one cell to the path length.
                queue.offer(new Cell(nr, nc, distance + 1));
            }
        }

        // No path exists.
        return -1;
    }
}
