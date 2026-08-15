import java.util.*;

/**
 * =====================================================================================
 * GOOGLE-STYLE MOCK ONSITE INTERVIEW — TRAPPING RAIN WATER II  (LeetCode 407)
 * =====================================================================================
 *
 * Run with:  java TrappingRainWater2D.java   (Java 21+ single-file source-launch mode)
 * =====================================================================================
 */
class TrappingRainWater2D {

    /*
     * =================================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * =================================================================================
     * We are given an m x n grid of non-negative integers, heightMap[i][j], representing
     * the elevation of a unit column of terrain at row i, column j. Imagine it raining
     * indefinitely over this terrain. Water can settle in "bowls" formed by surrounding
     * higher terrain, but any water that would spill off the edge of the grid escapes
     * (the grid boundary is effectively open to infinity — there is no wall holding
     * water in along the perimeter).
     *
     * We must return the TOTAL VOLUME of water trapped after the rain settles into a
     * stable equilibrium, where volume is measured in unit cubes (1 unit of height over
     * 1 unit cell = 1 unit of volume).
     *
     * Key constraints:
     *   - 1 <= m, n <= 200        → grid has at most 40,000 cells
     *   - 0 <= heightMap[i][j] <= 20,000
     *
     * Inputs:  int[][] heightMap
     * Outputs: a single long/int representing total trapped volume
     *
     * Core insight (stated up front, as I would in a real interview): the water level
     * that any interior cell can sustain equals the MINIMUM, over all paths from that
     * cell to the grid boundary, of the MAXIMUM height encountered along that path
     * ("minimax path" / bottleneck shortest path). This reframes the problem as a
     * shortest-path variant on a grid graph rather than a pure simulation problem —
     * that reframing is the crux of the interview.
     * =================================================================================
     */

    /*
     * =================================================================================
     * SECTION 2: CLARIFYING QUESTIONS (asked to interviewer, with assumed answers)
     * =================================================================================
     * 1. Q: Can the grid be non-rectangular (ragged rows)?
     *    A: No — assume a strict rectangular m x n grid, as guaranteed by constraints.
     *
     * 2. Q: Are heights guaranteed non-negative? Can they be zero?
     *    A: Yes, 0 <= height <= 20,000 per constraints; zero is a valid "sea level" cell.
     *
     * 3. Q: Does water trapped along the boundary count (can boundary cells hold water)?
     *    A: No — boundary cells are open to the "outside," so by definition they never
     *       hold trapped water; they only serve as spill/escape points.
     *
     * 4. Q: Should the return type be int or can volume overflow a 32-bit int?
     *    A: Max volume ≈ 200*200*20000 = 8x10^8, which fits in a signed 32-bit int
     *       (max ~2.1x10^9), but I'll accumulate in a long defensively to avoid any
     *       overflow risk during intermediate arithmetic, then return as int/long
     *       per the function signature the interviewer wants.
     *
     * 5. Q: Is the grid guaranteed non-empty (m >= 1, n >= 1)?
     *    A: Yes per constraints, but I will still defensively validate null/empty input.
     *
     * 6. Q: Do we need to support concurrent/streaming updates to the height map, or is
     *       this a single, static, one-shot computation?
     *    A: One-shot, static computation — no concurrency requirements.
     *
     * 7. Q: Should ties in height (multiple cells at the same elevation) be broken any
     *       particular way, e.g., does processing order among equal heights matter?
     *    A: No — the final trapped volume is invariant to tie-breaking order among
     *       cells of equal height; any consistent tie-break (e.g., insertion order) is
     *       fine.
     *
     * 8. Q: What are realistic performance expectations — is O(mn log(mn)) acceptable,
     *       or do we need sub-linearithmic time?
     *    A: O(mn log(mn)) is expected and optimal for this problem; mn <= 40,000 makes
     *       this trivially fast (well under a second).
     * =================================================================================
     */

    /*
     * =================================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * =================================================================================
     * Example 1 (Normal case — classic LeetCode example):
     *   heightMap = [[1,4,3,1,3,2],
     *                [3,2,1,3,2,4],
     *                [2,3,3,2,3,1]]
     *   Expected trapped volume = 4
     *
     * Example 2 (Edge case — degenerate grid, single row/column or 1x1):
     *   heightMap = [[5]]                → 0 (no interior cells at all; trivially 0)
     *   heightMap = [[1,2,3,4]]           → 0 (single row = entirely boundary, no water)
     *   Rationale: with m == 1 or n == 1, EVERY cell is on the boundary, so no cell can
     *   ever be "interior," hence trapped volume is always 0. Must special-case or the
     *   general algorithm naturally returns 0 anyway (good self-check).
     *
     * Example 3 (Tie-breaking / boundary case — symmetric bowl with uniform walls):
     *   heightMap = [[3,3,3],
     *                [3,1,3],
     *                [3,3,3]]
     *   All 8 boundary cells share height 3 (a tie). The center cell (height 1) is
     *   surrounded uniformly, so regardless of which equal-height boundary cell is
     *   processed first by the algorithm, the center's water level resolves to 3.
     *   Expected trapped volume = 3 - 1 = 2.
     *   This example specifically stresses correctness under heap tie-breaking.
     * =================================================================================
     */

    /*
     * =================================================================================
     * SECTION 4 & 5: PARADIGM SWEEP
     * =================================================================================
     * APPLICABLE:
     *   - Heap / Priority Queue: models a Dijkstra-like "always expand the currently
     *     lowest-known boundary" greedy frontier — this is the optimal approach.
     *   - Greedy: the heap expansion order is fundamentally a greedy strategy — proven
     *     correct because expanding the globally lowest frontier cell first guarantees
     *     its final water level is already correct (classic Dijkstra-style exchange
     *     argument, detailed in Section 8).
     *   - BFS / Graph traversal: the flood-fill frontier expansion is a graph traversal
     *     over the implicit 4-connected grid graph.
     *   - Union-Find / advanced structure: an alternative "minimum bottleneck spanning
     *     tree" formulation (Kruskal's algorithm) solves the same minimax-path problem.
     *   - Dynamic Programming (relaxation-style, not classic tabulation): brute-force
     *     approach below relaxes water[i][j] = max(height[i][j], min over neighbors)
     *     repeatedly to a fixed point — a Bellman-Ford-like relaxation rather than true
     *     DP, since there's no acyclic dependency order (mentioned explicitly, not
     *     mislabeled as textbook DP).
     *
     * NOT APPLICABLE (one-line reasons):
     *   - Two-pointer / sliding window: relies on monotonic structure over a linear
     *     sequence (as in the 1-D Trapping Rain Water); a 2-D grid has no single
     *     linear order that preserves the necessary monotonicity.
     *   - Divide & Conquer: no clean way to split the grid into independent subgrids
     *     and cheaply combine trapped-water results, because water paths can cross any
     *     proposed partition boundary.
     *   - Binary search: no monotonic predicate over a single scalar search space that
     *     directly yields the answer (we're not searching for one global threshold —
     *     the trapped level differs per cell).
     *   - Trie: no prefix/string structure present in this problem at all.
     *   - Segment tree: no range-query/range-update pattern over an ordered index
     *     space that this problem naturally reduces to.
     * =================================================================================
     */

    /*
     * =================================================================================
     * APPROACH 1: Brute Force — Iterative Relaxation to Fixed Point
     * =================================================================================
     * IDEA:
     *   Define water[i][j] as the eventual water level (NOT the trapped depth) at cell
     *   (i,j). For boundary cells, water[i][j] = height[i][j] (no trapping possible).
     *   For interior cells, initialize water[i][j] = +infinity (an upper bound, e.g.
     *   the max height in the grid), then repeatedly relax:
     *
     *       water[i][j] = max( height[i][j], min( water[i][j], water[neighbor] ) )
     *                                          for each of the 4 neighbors
     *
     *   Repeat this full sweep over the whole grid until nothing changes. This is
     *   exactly a Bellman-Ford-style relaxation of the "minimax path to boundary"
     *   recurrence, applied naively without any smart ordering.
     *
     * DATA STRUCTURE / PARADIGM: plain 2D array relaxation (naive, unordered DP-like
     * fixed-point iteration).
     *
     * TIME COMPLEXITY: O((m*n)^2) worst case — each full sweep is O(mn), and in an
     * adversarial arrangement a cell's value can be revised up to O(mn) times before
     * convergence (analogous to Bellman-Ford needing up to V iterations).
     * SPACE COMPLEXITY: O(m*n) for the water[][] array.
     *
     * PROS: trivial to reason about and code correctly under interview pressure; no
     *   subtle heap/DSU bookkeeping; easy to convince yourself (and the interviewer)
     *   it's correct via the relaxation invariant.
     * CONS: quadratic in mn — with mn up to 40,000, worst case ~1.6 billion relaxation
     *   operations, far too slow for the given constraints in the worst case.
     *
     * WHEN TO USE: only as a warm-up/correctness baseline to cross-validate the
     * optimal solution on small inputs — never in production or as a final interview
     * answer for these constraints.
     * =================================================================================
     */
    static long bruteForceRelaxation(int[][] heightMap) {
        if (heightMap == null || heightMap.length == 0 || heightMap[0].length == 0) return 0;
        int rows = heightMap.length, cols = heightMap[0].length;
        if (rows <= 2 || cols <= 2) return 0; // no interior cells possible

        int maxHeight = 0;
        for (int[] row : heightMap) for (int h : row) maxHeight = Math.max(maxHeight, h);

        long[][] water = new long[rows][cols];
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            for (int colIndex = 0; colIndex < cols; colIndex++) {
                boolean isBoundary = rowIndex == 0 || rowIndex == rows - 1 || colIndex == 0 || colIndex == cols - 1;
                water[rowIndex][colIndex] = isBoundary ? heightMap[rowIndex][colIndex] : maxHeight;
            }
        }

        int[] deltaRow = {-1, 1, 0, 0};
        int[] deltaCol = {0, 0, -1, 1};
        boolean changedInThisPass = true;
        while (changedInThisPass) {
            changedInThisPass = false;
            for (int rowIndex = 1; rowIndex < rows - 1; rowIndex++) {
                for (int colIndex = 1; colIndex < cols - 1; colIndex++) {
                    for (int direction = 0; direction < 4; direction++) {
                        int neighborRow = rowIndex + deltaRow[direction];
                        int neighborCol = colIndex + deltaCol[direction];
                        long candidateLevel = Math.max(heightMap[rowIndex][colIndex],
                                Math.min(water[rowIndex][colIndex], water[neighborRow][neighborCol]));
                        if (candidateLevel < water[rowIndex][colIndex]) {
                            water[rowIndex][colIndex] = candidateLevel;
                            changedInThisPass = true;
                        }
                    }
                }
            }
        }

        long totalVolume = 0;
        for (int rowIndex = 1; rowIndex < rows - 1; rowIndex++)
            for (int colIndex = 1; colIndex < cols - 1; colIndex++)
                totalVolume += water[rowIndex][colIndex] - heightMap[rowIndex][colIndex];
        return totalVolume;
    }

    /*
     * =================================================================================
     * APPROACH 2: Queue-Driven Relaxation (SPFA-style optimization of Approach 1)
     * =================================================================================
     * IDEA:
     *   Same relaxation recurrence as Approach 1, but instead of blindly re-sweeping
     *   the entire grid every pass, only re-examine a cell's neighbors when that cell's
     *   own water level just changed (classic SPFA-style optimization of Bellman-Ford).
     *   Seed a queue with boundary cells (whose levels are already final), then
     *   propagate outward, pushing a neighbor back onto the queue whenever its level
     *   is lowered.
     *
     * DATA STRUCTURE / PARADIGM: FIFO queue-based relaxation (SPFA), graph traversal.
     *
     * TIME COMPLEXITY: worst case still O((m*n)^2) under adversarial inputs (same
     *   theoretical bound as SPFA vs. Bellman-Ford), but in practice far fewer
     *   re-relaxations occur because we skip converged regions entirely.
     * SPACE COMPLEXITY: O(m*n) for the water[][] array and the queue.
     *
     * PROS: substantially faster in practice than Approach 1 with almost no added
     *   conceptual complexity; still doesn't require a heap or union-find.
     * CONS: no improved worst-case theoretical guarantee — still not safe for the
     *   stated constraints against adversarial inputs; more subtle to prove
     *   termination/correctness than the heap approach.
     *
     * WHEN TO USE: reasonable "first optimization" to mention if asked "how would you
     *   speed up Approach 1 without changing data structures?" — good for showing
     *   iterative improvement, but not the final answer.
     * =================================================================================
     */
    static long queueRelaxation(int[][] heightMap) {
        if (heightMap == null || heightMap.length == 0 || heightMap[0].length == 0) return 0;
        int rows = heightMap.length, cols = heightMap[0].length;
        if (rows <= 2 || cols <= 2) return 0;

        int maxHeight = 0;
        for (int[] row : heightMap) for (int h : row) maxHeight = Math.max(maxHeight, h);

        long[][] water = new long[rows][cols];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            for (int colIndex = 0; colIndex < cols; colIndex++) {
                boolean isBoundary = rowIndex == 0 || rowIndex == rows - 1 || colIndex == 0 || colIndex == cols - 1;
                water[rowIndex][colIndex] = isBoundary ? heightMap[rowIndex][colIndex] : maxHeight;
                if (isBoundary) queue.add(new int[]{rowIndex, colIndex});
            }
        }

        int[] deltaRow = {-1, 1, 0, 0};
        int[] deltaCol = {0, 0, -1, 1};
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int currentRow = current[0], currentCol = current[1];
            for (int direction = 0; direction < 4; direction++) {
                int neighborRow = currentRow + deltaRow[direction];
                int neighborCol = currentCol + deltaCol[direction];
                if (neighborRow < 0 || neighborRow >= rows || neighborCol < 0 || neighborCol >= cols) continue;
                long candidateLevel = Math.max(heightMap[neighborRow][neighborCol],
                        Math.min(water[neighborRow][neighborCol], water[currentRow][currentCol]));
                if (candidateLevel < water[neighborRow][neighborCol]) {
                    water[neighborRow][neighborCol] = candidateLevel;
                    queue.add(new int[]{neighborRow, neighborCol});
                }
            }
        }

        long totalVolume = 0;
        for (int rowIndex = 1; rowIndex < rows - 1; rowIndex++)
            for (int colIndex = 1; colIndex < cols - 1; colIndex++)
                totalVolume += water[rowIndex][colIndex] - heightMap[rowIndex][colIndex];
        return totalVolume;
    }

    /*
     * =================================================================================
     * APPROACH 3: Union-Find — Minimum Bottleneck Spanning Tree (Kruskal-style)
     * =================================================================================
     * IDEA:
     *   Model the grid as a graph: one node per cell, an undirected edge between every
     *   pair of 4-adjacent cells with weight = max(height[u], height[v]). The water
     *   level sustainable at any cell equals the "minimax path" value to the nearest
     *   boundary cell — i.e., the minimum possible maximum edge weight along any path
     *   to the boundary.
     *
     *   Classical result: in a Minimum (bottleneck) Spanning Tree built via Kruskal's
     *   algorithm (process edges in ascending weight order, union endpoints if they're
     *   in different components), the unique tree path between any two nodes has the
     *   minimum possible maximum edge weight among ALL paths between them in the
     *   original graph. So: (1) build the MST with Union-Find, (2) run a multi-source
     *   BFS from all boundary cells over the MST edges only, propagating the running
     *   max edge weight, to get each cell's sustainable water level directly.
     *
     * DATA STRUCTURE / PARADIGM: Union-Find (Disjoint Set Union) + Kruskal's MST +
     *   BFS over the resulting tree.
     *
     * TIME COMPLEXITY: O(mn * alpha(mn)) for the union-find operations, dominated by
     *   O(mn log(mn)) for sorting the ~2mn edges. BFS phase is O(mn).
     *   Overall: O(mn log(mn)).
     * SPACE COMPLEXITY: O(m*n) for DSU arrays, edge list, and adjacency list.
     *
     * PROS: elegant "no heap needed" alternative; a great answer to the interviewer's
     *   likely follow-up "can you solve this without a priority queue?"; showcases
     *   graph-theory depth (minimax path <-> MST bottleneck property).
     * CONS: meaningfully more code and more subtle correctness argument than the heap
     *   approach; requires a full second traversal (BFS) after building the MST) so it
     *   is not simpler in practice despite matching asymptotic complexity.
     *
     * WHEN TO USE: as a strong verbal/algorithmic follow-up discussion point, or when
     *   explicitly asked to avoid heaps; not typically the first implementation choice
     *   in an interview given the added complexity for equal asymptotic complexity.
     * =================================================================================
     */
    static final class DisjointSetUnion {
        private final int[] parent;
        private final int[] rank;

        DisjointSetUnion(int size) {
            parent = new int[size];
            rank = new int[size];
            for (int i = 0; i < size; i++) parent[i] = i;
        }

        int find(int node) {
            while (parent[node] != node) {
                parent[node] = parent[parent[node]]; // path halving
                node = parent[node];
            }
            return node;
        }

        boolean union(int firstNode, int secondNode) {
            int firstRoot = find(firstNode), secondRoot = find(secondNode);
            if (firstRoot == secondRoot) return false;
            if (rank[firstRoot] < rank[secondRoot]) { int temp = firstRoot; firstRoot = secondRoot; secondRoot = temp; }
            parent[secondRoot] = firstRoot;
            if (rank[firstRoot] == rank[secondRoot]) rank[firstRoot]++;
            return true;
        }
    }

    static long unionFindBottleneckMst(int[][] heightMap) {
        if (heightMap == null || heightMap.length == 0 || heightMap[0].length == 0) return 0;
        int rows = heightMap.length, cols = heightMap[0].length;
        if (rows <= 2 || cols <= 2) return 0;

        // Build weighted edge list: edge weight = max height of its two endpoints.
        List<int[]> edges = new ArrayList<>(); // {weight, nodeA, nodeB}
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            for (int colIndex = 0; colIndex < cols; colIndex++) {
                int nodeId = rowIndex * cols + colIndex;
                if (colIndex + 1 < cols) {
                    int rightNeighbor = nodeId + 1;
                    int weight = Math.max(heightMap[rowIndex][colIndex], heightMap[rowIndex][colIndex + 1]);
                    edges.add(new int[]{weight, nodeId, rightNeighbor});
                }
                if (rowIndex + 1 < rows) {
                    int downNeighbor = nodeId + cols;
                    int weight = Math.max(heightMap[rowIndex][colIndex], heightMap[rowIndex + 1][colIndex]);
                    edges.add(new int[]{weight, nodeId, downNeighbor});
                }
            }
        }
        edges.sort(Comparator.comparingInt(edge -> edge[0]));

        // Kruskal: build MST, recording accepted edges into an adjacency list.
        DisjointSetUnion dsu = new DisjointSetUnion(rows * cols);
        List<List<int[]>> mstAdjacency = new ArrayList<>(rows * cols); // each entry: {neighborId, weight}
        for (int i = 0; i < rows * cols; i++) mstAdjacency.add(new ArrayList<>());
        for (int[] edge : edges) {
            int weight = edge[0], nodeA = edge[1], nodeB = edge[2];
            if (dsu.union(nodeA, nodeB)) {
                mstAdjacency.get(nodeA).add(new int[]{nodeB, weight});
                mstAdjacency.get(nodeB).add(new int[]{nodeA, weight});
            }
        }

        // Multi-source BFS from all boundary cells over MST edges, propagating max weight.
        int[] sustainedLevel = new int[rows * cols];
        boolean[] visited = new boolean[rows * cols];
        ArrayDeque<Integer> bfsQueue = new ArrayDeque<>();
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            for (int colIndex = 0; colIndex < cols; colIndex++) {
                if (rowIndex == 0 || rowIndex == rows - 1 || colIndex == 0 || colIndex == cols - 1) {
                    int nodeId = rowIndex * cols + colIndex;
                    visited[nodeId] = true;
                    sustainedLevel[nodeId] = heightMap[rowIndex][colIndex];
                    bfsQueue.add(nodeId);
                }
            }
        }
        while (!bfsQueue.isEmpty()) {
            int currentNode = bfsQueue.poll();
            for (int[] edge : mstAdjacency.get(currentNode)) {
                int neighborNode = edge[0], edgeWeight = edge[1];
                if (!visited[neighborNode]) {
                    visited[neighborNode] = true;
                    sustainedLevel[neighborNode] = Math.max(sustainedLevel[currentNode], edgeWeight);
                    bfsQueue.add(neighborNode);
                }
            }
        }

        long totalVolume = 0;
        for (int rowIndex = 1; rowIndex < rows - 1; rowIndex++) {
            for (int colIndex = 1; colIndex < cols - 1; colIndex++) {
                int nodeId = rowIndex * cols + colIndex;
                totalVolume += sustainedLevel[nodeId] - heightMap[rowIndex][colIndex];
            }
        }
        return totalVolume;
    }

    /*
     * =================================================================================
     * APPROACH 4 (OPTIMAL): Multi-Source Min-Heap Flood Fill (Dijkstra-style)
     * =================================================================================
     * IDEA:
     *   Treat every boundary cell as a starting "wall" of known, final height. Push all
     *   boundary cells into a min-heap keyed by height. Repeatedly pop the globally
     *   lowest-height frontier cell — this is provably the one cell in the current
     *   frontier whose final water level can no longer be affected by anything not
     *   yet processed (exactly the Dijkstra greedy-exchange argument: any unprocessed
     *   cell has height >= the popped cell's key, so it can never lower the popped
     *   cell's water level further). For each unvisited neighbor of the popped cell,
     *   its sustained water level = max(popped cell's key, neighbor's own height); any
     *   excess over the neighbor's own height is trapped volume. Push the neighbor with
     *   that new key and mark visited.
     *
     * DATA STRUCTURE / PARADIGM: min-heap (PriorityQueue) + greedy + BFS/graph
     *   traversal — essentially Dijkstra's algorithm with boundary cells as multiple
     *   sources and edge "cost" defined by the max-height relaxation rule.
     *
     * TIME COMPLEXITY: O(m*n*log(m*n)) — every one of the mn cells is pushed and popped
     *   from the heap exactly once, each heap operation costing O(log(mn)).
     * SPACE COMPLEXITY: O(m*n) for the visited array and the heap (holds at most O(mn)
     *   entries).
     *
     * PROS: matches the theoretical lower bound for this class of solution, clean and
     *   well-known correctness proof (Dijkstra greedy-exchange), moderate code length,
     *   easy to explain and defend live.
     * CONS: requires correctly identifying and seeding ALL boundary cells up front and
     *   getting the visited-marking timing right (mark on push, not on pop, to avoid
     *   duplicate heap entries) — a classic source of off-by-one/logic bugs under
     *   interview pressure.
     *
     * WHEN TO USE: this is the approach to implement as your final answer in a real
     *   interview — optimal complexity, reasonable code size, strong well-known
     *   correctness argument.
     * =================================================================================
     */
    static long optimalMinHeapFloodFill(int[][] heightMap) {
        if (heightMap == null || heightMap.length == 0 || heightMap[0].length == 0) return 0;
        int rows = heightMap.length, cols = heightMap[0].length;
        if (rows <= 2 || cols <= 2) return 0;

        boolean[][] visited = new boolean[rows][cols];
        // Each heap entry: {height, row, col} — ordered by height ascending.
        PriorityQueue<int[]> frontier = new PriorityQueue<>(Comparator.comparingInt(entry -> entry[0]));

        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            for (int colIndex = 0; colIndex < cols; colIndex++) {
                if (rowIndex == 0 || rowIndex == rows - 1 || colIndex == 0 || colIndex == cols - 1) {
                    frontier.offer(new int[]{heightMap[rowIndex][colIndex], rowIndex, colIndex});
                    visited[rowIndex][colIndex] = true;
                }
            }
        }

        int[] deltaRow = {-1, 1, 0, 0};
        int[] deltaCol = {0, 0, -1, 1};
        long totalVolume = 0;

        while (!frontier.isEmpty()) {
            int[] current = frontier.poll();
            int currentHeight = current[0], currentRow = current[1], currentCol = current[2];
            for (int direction = 0; direction < 4; direction++) {
                int neighborRow = currentRow + deltaRow[direction];
                int neighborCol = currentCol + deltaCol[direction];
                if (neighborRow < 0 || neighborRow >= rows || neighborCol < 0 || neighborCol >= cols) continue;
                if (visited[neighborRow][neighborCol]) continue;
                visited[neighborRow][neighborCol] = true;
                int neighborHeight = heightMap[neighborRow][neighborCol];
                int sustainedLevel = Math.max(currentHeight, neighborHeight);
                totalVolume += sustainedLevel - neighborHeight;
                frontier.offer(new int[]{sustainedLevel, neighborRow, neighborCol});
            }
        }
        return totalVolume;
    }

    /*
     * =================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * =================================================================================
     * | Approach                          | Time              | Space  | Best For                          | Limitations                              |
     * |------------------------------------|-------------------|--------|-----------------------------------|-------------------------------------------|
     * | 1. Brute Force Relaxation          | O((mn)^2)         | O(mn)  | Correctness baseline on tiny grids| Far too slow at mn = 40,000               |
     * | 2. Queue-Driven (SPFA) Relaxation  | O((mn)^2) worst   | O(mn)  | "First optimization" discussion   | No improved worst-case guarantee          |
     * | 3. Union-Find / Bottleneck MST     | O(mn log(mn))     | O(mn)  | "No heap allowed" follow-up       | More code, two-phase (MST + BFS)          |
     * | 4. Min-Heap Flood Fill (OPTIMAL)   | O(mn log(mn))     | O(mn)  | Primary interview answer          | Needs careful visited/push-time handling  |
     * =================================================================================
     */

    /*
     * =================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * =================================================================================
     * I would present APPROACH 4 (Multi-Source Min-Heap Flood Fill) as my primary
     * solution:
     *   - It achieves the optimal known time complexity, O(mn log(mn)), for this
     *     problem class.
     *   - Its correctness argument (Dijkstra-style greedy exchange) is standard,
     *     well-known, and quick to state and defend verbally.
     *   - Its code footprint is smaller and less bug-prone than the Union-Find/MST
     *     alternative, which needs two full phases (Kruskal + a second BFS pass).
     *   - It's the canonical, widely-recognized solution for this problem, which
     *     matches interviewer expectations for a strong onsite performance.
     * I would still volunteer the Union-Find alternative proactively once the heap
     * solution is coded and verified, to demonstrate depth — mentioning the minimax
     * path / bottleneck spanning tree connection before being asked.
     * =================================================================================
     */

    /*
     * =================================================================================
     * SECTION 9: DEEP DIVE — PRODUCTION-QUALITY OPTIMAL IMPLEMENTATION
     * =================================================================================
     */

    /**
     * Computes the total volume of rainwater trapped on a 2D elevation map.
     * <p>
     * Uses a multi-source min-heap flood-fill (Dijkstra-style greedy expansion),
     * treating every boundary cell as an initial known-height "wall" and expanding
     * inward through the globally lowest unvisited frontier cell at each step.
     *
     * @param heightMap non-null, non-ragged m x n grid of non-negative elevations,
     *                   with 1 &lt;= m, n &lt;= 200 and 0 &lt;= height &lt;= 20,000
     * @return total trapped water volume in unit cubes
     * @throws IllegalArgumentException if heightMap is null, empty, ragged, or
     *                                   contains negative heights
     */
    public static long trapRainWater(int[][] heightMap) {
        validateInput(heightMap);

        int rows = heightMap.length;
        int cols = heightMap[0].length;
        // Grids where every cell is on the boundary (single row or column) can never
        // trap water — short-circuit to avoid unnecessary heap work.
        if (rows <= 2 || cols <= 2) return 0;

        boolean[][] visited = new boolean[rows][cols];
        // Min-heap ordered by height; entries store {height, row, col} to avoid
        // allocating a wrapper object per cell.
        PriorityQueue<int[]> boundaryFrontier =
                new PriorityQueue<>(Comparator.comparingInt(entry -> entry[0]));

        // Seed the heap with every boundary cell — these are "final" from the start,
        // since nothing outside the grid can raise or lower their water level.
        for (int colIndex = 0; colIndex < cols; colIndex++) {
            addBoundaryCell(heightMap, visited, boundaryFrontier, 0, colIndex);
            addBoundaryCell(heightMap, visited, boundaryFrontier, rows - 1, colIndex);
        }
        for (int rowIndex = 1; rowIndex < rows - 1; rowIndex++) {
            addBoundaryCell(heightMap, visited, boundaryFrontier, rowIndex, 0);
            addBoundaryCell(heightMap, visited, boundaryFrontier, rowIndex, cols - 1);
        }

        int[] deltaRow = {-1, 1, 0, 0};
        int[] deltaCol = {0, 0, -1, 1};
        long trappedVolume = 0;

        while (!boundaryFrontier.isEmpty()) {
            int[] current = boundaryFrontier.poll();
            int currentSustainedHeight = current[0];
            int currentRow = current[1];
            int currentCol = current[2];

            for (int direction = 0; direction < 4; direction++) {
                int neighborRow = currentRow + deltaRow[direction];
                int neighborCol = currentCol + deltaCol[direction];

                // Bounds check.
                if (neighborRow < 0 || neighborRow >= rows || neighborCol < 0 || neighborCol >= cols) {
                    continue;
                }
                // Skip cells already finalized (either original boundary or already
                // popped/pushed by an earlier, lower-or-equal frontier expansion).
                if (visited[neighborRow][neighborCol]) {
                    continue;
                }

                // Mark visited at PUSH time (not pop time) — this is the critical
                // detail that prevents duplicate stale entries from bloating the heap
                // and ensures each cell contributes to the total exactly once.
                visited[neighborRow][neighborCol] = true;

                int neighborOwnHeight = heightMap[neighborRow][neighborCol];
                // The neighbor's water level is bounded below by the wall it just
                // spilled over (currentSustainedHeight) and below by its own ground
                // height — whichever is higher determines the sustained level.
                int neighborSustainedLevel = Math.max(currentSustainedHeight, neighborOwnHeight);

                trappedVolume += neighborSustainedLevel - neighborOwnHeight;

                boundaryFrontier.offer(new int[]{neighborSustainedLevel, neighborRow, neighborCol});
            }
        }

        return trappedVolume;
    }

    /** Validates structural and value invariants of the input grid, failing fast. */
    private static void validateInput(int[][] heightMap) {
        if (heightMap == null || heightMap.length == 0 || heightMap[0] == null || heightMap[0].length == 0) {
            throw new IllegalArgumentException("heightMap must be non-null and non-empty");
        }
        int expectedCols = heightMap[0].length;
        for (int[] row : heightMap) {
            if (row == null || row.length != expectedCols) {
                throw new IllegalArgumentException("heightMap must be a non-ragged rectangular grid");
            }
            for (int height : row) {
                if (height < 0) {
                    throw new IllegalArgumentException("heightMap values must be non-negative");
                }
            }
        }
    }

    /** Helper to seed a single boundary cell into the frontier heap, marking it visited. */
    private static void addBoundaryCell(int[][] heightMap, boolean[][] visited,
                                         PriorityQueue<int[]> frontier, int rowIndex, int colIndex) {
        if (!visited[rowIndex][colIndex]) {
            visited[rowIndex][colIndex] = true;
            frontier.offer(new int[]{heightMap[rowIndex][colIndex], rowIndex, colIndex});
        }
    }

    /*
     * =================================================================================
     * SECTION 10: DRY RUN / TRACE
     * =================================================================================
     * Tracing trapRainWater() on the tie-breaking example from Section 3:
     *
     *   heightMap = [[3,3,3],
     *                [3,1,3],
     *                [3,3,3]]
     *
     * INITIAL SEEDING (all 8 boundary cells pushed, visited = true for all of them):
     *   heap contents (height, row, col), in ascending-height order (all tie at 3):
     *     (3,0,0) (3,0,1) (3,0,2) (3,1,0) (3,1,2) (3,2,0) (3,2,1) (3,2,2)
     *   visited grid:
     *     [ T T T ]
     *     [ T F T ]     <- center (1,1) not yet visited
     *     [ T T T ]
     *   trappedVolume = 0
     *
     * POP (3,0,0): neighbors (−1,0) out of bounds, (1,0) visited, (0,−1) out of bounds,
     *   (0,1) visited → no new work.
     *
     * POP (3,0,1): neighbors (−1,1) out of bounds, (1,1) UNVISITED → this is the center!
     *   visited[1][1] = true
     *   neighborOwnHeight = 1
     *   neighborSustainedLevel = max(3, 1) = 3
     *   trappedVolume += 3 - 1 = 2   → trappedVolume = 2
     *   push (3,1,1) onto heap
     *   (also checks (0,0) visited, (0,2) visited → no-ops)
     *
     * POP (3,0,2): all 4 neighbors already visited → no new work.
     * POP (3,1,0): all 4 neighbors already visited (center now visited too) → no new work.
     * POP (3,1,2): all 4 neighbors already visited → no new work.
     * POP (3,2,0): all 4 neighbors already visited → no new work.
     * POP (3,2,1): all 4 neighbors already visited → no new work.
     * POP (3,2,2): all 4 neighbors already visited → no new work.
     * POP (3,1,1) [the center, pushed earlier]: all 4 neighbors are boundary cells,
     *   already visited → no new work.
     *
     * Heap empty → loop terminates.
     * FINAL RESULT: trappedVolume = 2  ✓ matches expected answer from Section 3.
     * =================================================================================
     */

    /*
     * =================================================================================
     * SECTION 11: CLOSING SUMMARY
     * =================================================================================
     * - Brute-force relaxation (Approach 1) and its queue-driven variant (Approach 2)
     *   are correct and simple to reason about but do not meet the performance bar for
     *   mn up to 40,000 in the worst case — useful only as correctness oracles.
     * - The Union-Find / bottleneck-MST approach (Approach 3) matches the optimal
     *   asymptotic complexity and is a strong "alternative paradigm" talking point, but
     *   costs more code and a two-phase structure for no asymptotic benefit over the
     *   heap solution.
     * - The multi-source min-heap flood fill (Approach 4) is the recommended, optimal,
     *   production-quality solution: O(mn log(mn)) time, O(mn) space, with a clean
     *   Dijkstra-style correctness proof.
     * - Known assumptions/limitations of the final solution: assumes a well-formed
     *   rectangular, non-null grid with non-negative integer heights (enforced via
     *   validateInput); assumes single-threaded, one-shot evaluation (no incremental
     *   update support); volume is accumulated in a long to eliminate any overflow risk
     *   even though a 32-bit int would technically suffice for the stated constraints.
     * =================================================================================
     */

    /*
     * =================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * =================================================================================
     * 1. "What if the grid were far larger — say 10^6 x 10^6 and sparse?"
     *    → Discuss switching to a sparse/coordinate representation and possibly
     *      parallelizing the initial boundary seeding; the heap approach's O(mn log mn)
     *      becomes the bottleneck, motivating external-memory or distributed variants.
     *
     * 2. "Can you compute this without extra O(mn) space, e.g., in-place?"
     *    → Partially: the input array can't be reused to store visited/water-level
     *      state without corrupting height data needed later, unless we encode extra
     *      bits into unused high bits of each int (heights fit in 15 bits, leaving
     *      room) — a valid but riskier space-optimization to discuss.
     *
     * 3. "How would you support incremental updates (one cell's height changes) without
     *     recomputing from scratch?"
     *    → This is essentially dynamic/incremental Dijkstra — nontrivial; discuss
     *      localized re-relaxation from the changed cell outward, bounded by how far
     *      the change can propagate.
     *
     * 4. "What if diagonal neighbors (8-connectivity) also counted as connected?"
     *    → Trivial change: add the 4 diagonal deltas to the neighbor-expansion loops
     *      in any approach; asymptotic complexity is unchanged.
     *
     * 5. "Could you parallelize the min-heap approach across multiple threads?"
     *    → The strict Dijkstra ordering by height is hard to parallelize directly;
     *      discuss "delta-stepping" style bucket-based relaxation as a parallelizable
     *      alternative that approximates the correct processing order.
     *
     * 6. "How does this relate to the 1-D Trapping Rain Water problem — could you reuse
     *     that two-pointer technique here?"
     *    → No — two-pointer relies on a strict linear left-to-right / right-to-left
     *      ordering with monotonic max-so-far tracking; in 2D there's no single
     *      linear traversal order that preserves the required monotonicity, which is
     *      exactly why this problem needs a graph-search-based technique instead.
     * =================================================================================
     */

    /*
     * =================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * =================================================================================
     * 1. Marking visited at POP time instead of PUSH time — this allows the same cell
     *    to be pushed onto the heap multiple times by different neighbors before being
     *    processed, silently inflating both runtime and (if not guarded) the trapped
     *    volume total via double-counting.
     *
     * 2. Forgetting the m <= 2 or n <= 2 degenerate case — candidates sometimes write
     *    boundary-detection logic that assumes at least one interior row/column exists,
     *    causing index-out-of-bounds or incorrect nonzero output on single-row/column
     *    grids where every cell is boundary by definition.
     *
     * 3. Using the WRONG relaxation formula — writing
     *      neighborLevel = max(currentHeight, neighborHeight)
     *    is correct, but a common bug is instead writing
     *      neighborLevel = currentHeight
     *    (forgetting to clamp against the neighbor's own ground height), which
     *    incorrectly allows water to sit BELOW the neighbor's own terrain, or even
     *    produces negative trapped volume for interior cells taller than their
     *    "spilling" neighbor.
     *
     * 4. Assuming the greedy heap order can be replaced by a simple sort-once
     *    preprocessing step — candidates sometimes try to pre-sort all cells by height
     *    once and process in that fixed order, which breaks correctness because a
     *    cell's TRUE sustained water level depends on the max height along its path
     *    from the boundary, which can only be discovered dynamically as the frontier
     *    expands — not from the cell's own static height alone.
     * =================================================================================
     */

    /*
     * =================================================================================
     * TEST HARNESS / CROSS-VALIDATION main()
     * =================================================================================
     */
    public static void main(String[] args) {
        List<int[][]> testCases = new ArrayList<>();
        List<Long> expectedResults = new ArrayList<>();

        // Normal case (classic LeetCode example).
        testCases.add(new int[][]{
                {1, 4, 3, 1, 3, 2},
                {3, 2, 1, 3, 2, 4},
                {2, 3, 3, 2, 3, 1}
        });
        expectedResults.add(4L);

        // Edge case: 1x1 grid.
        testCases.add(new int[][]{{5}});
        expectedResults.add(0L);

        // Edge case: single row.
        testCases.add(new int[][]{{1, 2, 3, 4}});
        expectedResults.add(0L);

        // Edge case: single column.
        testCases.add(new int[][]{{1}, {2}, {3}, {4}});
        expectedResults.add(0L);

        // Tie-break / boundary case: symmetric uniform bowl.
        testCases.add(new int[][]{
                {3, 3, 3},
                {3, 1, 3},
                {3, 3, 3}
        });
        expectedResults.add(2L);

        // Larger symmetric bowl (classic 5x5 example).
        testCases.add(new int[][]{
                {3, 3, 3, 3, 3},
                {3, 2, 2, 2, 3},
                {3, 2, 1, 2, 3},
                {3, 2, 2, 2, 3},
                {3, 3, 3, 3, 3}
        });
        expectedResults.add(10L);

        // Flat grid — no possible trapping regardless of shape.
        testCases.add(new int[][]{
                {5, 5, 5, 5},
                {5, 5, 5, 5},
                {5, 5, 5, 5}
        });
        expectedResults.add(0L);

        // Max-value stress case (near constraint boundary values, small grid).
        testCases.add(new int[][]{
                {20000, 20000, 20000},
                {20000, 0, 20000},
                {20000, 20000, 20000}
        });
        expectedResults.add(20000L);

        int passedCount = 0;
        for (int testIndex = 0; testIndex < testCases.size(); testIndex++) {
            int[][] grid = testCases.get(testIndex);
            long expected = expectedResults.get(testIndex);

            long bruteForceResult = bruteForceRelaxation(grid);
            long queueResult = queueRelaxation(grid);
            long unionFindResult = unionFindBottleneckMst(grid);
            long optimalResult = optimalMinHeapFloodFill(grid);
            long productionResult = trapRainWater(grid);

            boolean allMatch = bruteForceResult == expected
                    && queueResult == expected
                    && unionFindResult == expected
                    && optimalResult == expected
                    && productionResult == expected;

            System.out.printf(
                    "Test #%d: expected=%d | bruteForce=%d | queueRelax=%d | unionFind=%d | optimalHeap=%d | production=%d | %s%n",
                    testIndex + 1, expected, bruteForceResult, queueResult, unionFindResult,
                    optimalResult, productionResult, allMatch ? "PASS" : "FAIL");

            if (allMatch) passedCount++;
        }

        System.out.printf("%n%d / %d test cases passed across all five implementations.%n",
                passedCount, testCases.size());
    }
}
