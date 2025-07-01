import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
/*
 * There is a directed graph of n colored nodes and m edges. The nodes are
 * numbered from 0 to n - 1.
 * 
 * You are given a string colors where colors[i] is a lowercase English letter
 * representing the color of the ith node in this graph (0-indexed). You are
 * also given a 2D array edges where edges[j] = [aj, bj] indicates that there is
 * a directed edge from node aj to node bj.
 * 
 * A valid path in the graph is a sequence of nodes x1 -> x2 -> x3 -> ... -> xk
 * such that there is a directed edge from xi to xi+1 for every 1 <= i < k. The
 * color value of the path is the number of nodes that are colored the most
 * frequently occurring color along that path.
 * 
 * Return the largest color value of any valid path in the given graph, or -1 if
 * the graph contains a cycle.
 * 
 * Example 1:
 * Input: colors = "abaca", edges = [[0,1],[0,2],[2,3],[3,4]]
 * Output: 3
 * Explanation: The path 0 -> 2 -> 3 -> 4 contains 3 nodes that are colored "a"
 * (red in the above image).
 * 
 * Example 2:
 * Input: colors = "a", edges = [[0,0]]
 * Output: -1
 * Explanation: There is a cycle from 0 to 0.
 */

class LargestColorValue {

    /**
     * Solution 1: Topological Sort with DP (RECOMMENDED)
     * Time Complexity: O(n + m) where n = nodes, m = edges
     * Space Complexity: O(n * 26) = O(n) for DP table
     */
    public int largestPathValue1(String colors, int[][] edges) {
        int n = colors.length();

        // Build adjacency list and calculate in-degrees
        List<List<Integer>> graph = new ArrayList<>();
        int[] indegree = new int[n];

        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
            indegree[edge[1]]++;
        }

        // DP table: dp[node][color] = max count of 'color' ending at 'node'
        int[][] dp = new int[n][26];
        Queue<Integer> queue = new LinkedList<>();

        // Initialize: add nodes with in-degree 0 to queue
        for (int i = 0; i < n; i++) {
            if (indegree[i] == 0) {
                queue.offer(i);
                // Initialize the color count for this node
                dp[i][colors.charAt(i) - 'a'] = 1;
            }
        }

        int processed = 0; // Count of processed nodes for cycle detection
        int maxColorValue = 0;

        while (!queue.isEmpty()) {
            int current = queue.poll();
            processed++;

            // Update the maximum color value
            for (int color = 0; color < 26; color++) {
                maxColorValue = Math.max(maxColorValue, dp[current][color]);
            }

            // Process all neighbors
            for (int neighbor : graph.get(current)) {
                // Update DP values for the neighbor
                for (int color = 0; color < 26; color++) {
                    if (color == colors.charAt(neighbor) - 'a') {
                        // If this color matches neighbor's color, increment count
                        dp[neighbor][color] = Math.max(dp[neighbor][color], dp[current][color] + 1);
                    } else {
                        // Otherwise, just propagate the count
                        dp[neighbor][color] = Math.max(dp[neighbor][color], dp[current][color]);
                    }
                }

                indegree[neighbor]--;
                if (indegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // Check for cycle: if not all nodes are processed, there's a cycle
        return processed == n ? maxColorValue : -1;
    }

    /**
     * Solution 2: DFS with Memoization and Cycle Detection
     * Time Complexity: O(n * 26) = O(n)
     * Space Complexity: O(n * 26) = O(n)
     */
    public int largestPathValue2(String colors, int[][] edges) {
        int n = colors.length();

        // Build adjacency list
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
        }

        // States: 0 = unvisited, 1 = visiting (in current path), 2 = visited
        int[] state = new int[n];
        // DP table: dp[node][color] = max count of 'color' starting from 'node'
        int[][] dp = new int[n][26];

        int maxColorValue = 0;

        for (int i = 0; i < n; i++) {
            if (state[i] == 0) {
                int result = dfs(i, graph, colors, state, dp);
                if (result == -1) {
                    return -1; // Cycle detected
                }
                maxColorValue = Math.max(maxColorValue, result);
            }
        }

        return maxColorValue;
    }

    private int dfs(int node, List<List<Integer>> graph, String colors, int[] state, int[][] dp) {
        if (state[node] == 1) {
            return -1; // Cycle detected
        }

        if (state[node] == 2) {
            // Already computed, return max value for this node
            int maxValue = 0;
            for (int color = 0; color < 26; color++) {
                maxValue = Math.max(maxValue, dp[node][color]);
            }
            return maxValue;
        }

        state[node] = 1; // Mark as visiting

        // Initialize DP for current node
        dp[node][colors.charAt(node) - 'a'] = 1;

        // Explore all neighbors
        for (int neighbor : graph.get(node)) {
            int neighborResult = dfs(neighbor, graph, colors, state, dp);
            if (neighborResult == -1) {
                return -1; // Cycle detected
            }

            // Update DP values based on neighbor's results
            for (int color = 0; color < 26; color++) {
                if (color == colors.charAt(node) - 'a') {
                    dp[node][color] = Math.max(dp[node][color], dp[neighbor][color] + 1);
                } else {
                    dp[node][color] = Math.max(dp[node][color], dp[neighbor][color]);
                }
            }
        }

        state[node] = 2; // Mark as visited

        // Return the maximum color value from this node
        int maxValue = 0;
        for (int color = 0; color < 26; color++) {
            maxValue = Math.max(maxValue, dp[node][color]);
        }
        return maxValue;
    }

    /**
     * Solution 3: Optimized Topological Sort (Space Optimized)
     * Time Complexity: O(n + m)
     * Space Complexity: O(n * 26) = O(n)
     */
    public int largestPathValue3(String colors, int[][] edges) {
        int n = colors.length();

        List<List<Integer>> graph = new ArrayList<>();
        int[] indegree = new int[n];

        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
            indegree[edge[1]]++;
        }

        // DP table with better space utilization
        int[][] colorCount = new int[n][26];
        Queue<Integer> queue = new LinkedList<>();

        // Initialize nodes with in-degree 0
        for (int i = 0; i < n; i++) {
            if (indegree[i] == 0) {
                queue.offer(i);
            }
            // Initialize each node's own color count
            colorCount[i][colors.charAt(i) - 'a'] = 1;
        }

        int processed = 0;
        int result = 1; // At least 1 if graph is valid

        while (!queue.isEmpty()) {
            int current = queue.poll();
            processed++;

            // Update global maximum
            for (int c = 0; c < 26; c++) {
                result = Math.max(result, colorCount[current][c]);
            }

            // Process neighbors
            for (int neighbor : graph.get(current)) {
                // Update neighbor's color counts
                for (int c = 0; c < 26; c++) {
                    int newCount = colorCount[current][c];
                    if (c == colors.charAt(neighbor) - 'a') {
                        newCount++;
                    }
                    colorCount[neighbor][c] = Math.max(colorCount[neighbor][c], newCount);
                }

                indegree[neighbor]--;
                if (indegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        return processed == n ? result : -1;
    }

    /**
     * Solution 4: DFS with Enhanced Cycle Detection
     * Time Complexity: O(n + m)
     * Space Complexity: O(n)
     */
    public int largestPathValue4(String colors, int[][] edges) {
        int n = colors.length();

        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
        }

        // 0: white (unvisited), 1: gray (visiting), 2: black (visited)
        int[] color = new int[n];
        int[][] memo = new int[n][26];
        boolean[] computed = new boolean[n];

        int maxResult = 0;

        for (int i = 0; i < n; i++) {
            if (color[i] == 0) {
                int result = dfsEnhanced(i, graph, colors, color, memo, computed);
                if (result == -1) {
                    return -1;
                }
                maxResult = Math.max(maxResult, result);
            }
        }

        return maxResult;
    }

    private int dfsEnhanced(int node, List<List<Integer>> graph, String colors,
            int[] color, int[][] memo, boolean[] computed) {
        if (color[node] == 1) {
            return -1; // Cycle detected
        }

        if (computed[node]) {
            int maxVal = 0;
            for (int c = 0; c < 26; c++) {
                maxVal = Math.max(maxVal, memo[node][c]);
            }
            return maxVal;
        }

        color[node] = 1; // Mark as visiting

        // Initialize with current node's color
        memo[node][colors.charAt(node) - 'a'] = 1;

        for (int neighbor : graph.get(node)) {
            int neighborMax = dfsEnhanced(neighbor, graph, colors, color, memo, computed);
            if (neighborMax == -1) {
                return -1;
            }

            // Update memo based on neighbor's values
            for (int c = 0; c < 26; c++) {
                int count = memo[neighbor][c];
                if (c == colors.charAt(node) - 'a') {
                    count++;
                }
                memo[node][c] = Math.max(memo[node][c], count);
            }
        }

        color[node] = 2; // Mark as visited
        computed[node] = true;

        int maxVal = 0;
        for (int c = 0; c < 26; c++) {
            maxVal = Math.max(maxVal, memo[node][c]);
        }
        return maxVal;
    }

    // Test methods
    public static void main(String[] args) {
        LargestColorValue solution = new LargestColorValue();

        // Test Case 1
        String colors1 = "abaca";
        int[][] edges1 = { { 0, 1 }, { 0, 2 }, { 2, 3 }, { 3, 4 } };
        System.out.println("Test Case 1:");
        System.out.println("Colors: " + colors1);
        System.out.println("Expected: 3");
        System.out.println("Solution 1: " + solution.largestPathValue1(colors1, edges1));
        System.out.println("Solution 2: " + solution.largestPathValue2(colors1, edges1));
        System.out.println("Solution 3: " + solution.largestPathValue3(colors1, edges1));
        System.out.println("Solution 4: " + solution.largestPathValue4(colors1, edges1));
        System.out.println();

        // Test Case 2 (Cycle)
        String colors2 = "a";
        int[][] edges2 = { { 0, 0 } };
        System.out.println("Test Case 2 (Cycle):");
        System.out.println("Colors: " + colors2);
        System.out.println("Expected: -1");
        System.out.println("Solution 1: " + solution.largestPathValue1(colors2, edges2));
        System.out.println("Solution 2: " + solution.largestPathValue2(colors2, edges2));
        System.out.println("Solution 3: " + solution.largestPathValue3(colors2, edges2));
        System.out.println("Solution 4: " + solution.largestPathValue4(colors2, edges2));
        System.out.println();

        // Test Case 3
        String colors3 = "abc";
        int[][] edges3 = { { 0, 1 }, { 1, 2 } };
        System.out.println("Test Case 3:");
        System.out.println("Colors: " + colors3);
        System.out.println("Expected: 1");
        System.out.println("Solution 1: " + solution.largestPathValue1(colors3, edges3));
        System.out.println("Solution 2: " + solution.largestPathValue2(colors3, edges3));
        System.out.println("Solution 3: " + solution.largestPathValue3(colors3, edges3));
        System.out.println("Solution 4: " + solution.largestPathValue4(colors3, edges3));
    }

}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1 - Topological Sort with DP (RECOMMENDED):
 * - Time Complexity: O(n + m)
 * Each node is processed exactly once
 * Each edge is processed exactly once
 * For each node, we update 26 color counts (constant)
 * - Space Complexity: O(n * 26) = O(n)
 * DP table stores color counts for each node
 * Additional space for adjacency list and queue
 * 
 * Solution 2 - DFS with Memoization:
 * - Time Complexity: O(n * 26) = O(n)
 * Each node is visited at most once
 * For each node, we compute values for 26 colors
 * - Space Complexity: O(n * 26) = O(n)
 * Memoization table and recursion stack
 * 
 * Solution 3 - Optimized Topological Sort:
 * - Time Complexity: O(n + m)
 * - Space Complexity: O(n * 26) = O(n)
 * - Similar to Solution 1 but with better constant factors
 * 
 * Solution 4 - Enhanced DFS:
 * - Time Complexity: O(n + m)
 * - Space Complexity: O(n * 26) = O(n)
 * - Uses three-color DFS for cycle detection
 * 
 * KEY INSIGHTS:
 * 1. **Cycle Detection is Critical**: Must return -1 if cycle exists
 * 2. **DP on DAG**: Track maximum color count for each color at each node
 * 3. **Topological Ordering**: Process nodes in dependency order
 * 4. **Color Propagation**: When moving from node A to B, increment count if
 * colors match
 * 
 * ALGORITHM CHOICE:
 * - **Solution 1 (Topological Sort)**: Best for interviews - clear and
 * efficient
 * - **Solution 2 (DFS)**: Good alternative, more recursive thinking
 * - **Solution 3**: Optimized version of Solution 1
 * - **Solution 4**: Enhanced cycle detection with DFS
 * 
 * OPTIMIZATION OPPORTUNITIES:
 * 1. Early termination if maximum possible value is reached
 * 2. Pruning paths that cannot lead to optimal solutions
 * 3. Space optimization by processing in batches
 * 4. Parallel processing for independent components
 * 
 * ERROR HANDLING:
 * - Empty graph handling
 * - Self-loops detection
 * - Invalid input validation
 * - Memory optimization for large graphs
 */
