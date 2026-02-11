import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Maximum Weight Independent Set in Tree
 *
 * dp[node][0] = maximum sum in subtree if we SKIP this node
 * dp[node][1] = maximum sum in subtree if we TAKE this node
 *
 * Answer = max(dp[root][0], dp[root][1])
 *
 * Time Complexity: O(n)
 * Space Complexity: O(n)
 */
class MaximumIndependentSetTree {

    private List<List<Integer>> graph;
    private int[] val;

    // dp[node][0] = skip
    // dp[node][1] = take
    private long[][] dp;

    // visited[node] tells whether dp[node] is already computed
    private boolean[] visited;

    public long maxIndependentSetSum(int n, int[] val, int[][] edges) {
        this.val = val;

        // -------------------------
        // Build adjacency list
        // -------------------------
        graph = new ArrayList<>();
        for (int i = 0; i < n; i++)
            graph.add(new ArrayList<>());

        for (int[] e : edges) {
            int u = e[0], v = e[1];
            graph.get(u).add(v);
            graph.get(v).add(u);
        }

        dp = new long[n][2];
        visited = new boolean[n];

        int root = 0;

        // compute dp for the whole tree rooted at root
        dfs(root, -1);

        return Math.max(dp[root][0], dp[root][1]);
    }

    /**
     * DFS computes dp[node][0] and dp[node][1]
     *
     * dp[node][1] (take) = val[node] + sum(dp[child][0])
     * dp[node][0] (skip) = sum(max(dp[child][0], dp[child][1]))
     */
    private void dfs(int node, int parent) {

        // memoization check
        if (visited[node])
            return;
        visited[node] = true;

        long take = val[node]; // if we take this node, start with its value
        long skip = 0; // if we skip this node

        for (int child : graph.get(node)) {
            if (child == parent)
                continue;

            dfs(child, node);

            // If we TAKE current node, we must SKIP child
            take += dp[child][0];

            // If we SKIP current node, child can be taken or skipped
            skip += Math.max(dp[child][0], dp[child][1]);
        }

        dp[node][0] = skip;
        dp[node][1] = take;
    }
}

/**
 * Maximum Weight Independent Set in Tree (Iterative DP)
 *
 * dp[node][0] = maximum sum in subtree if we SKIP this node
 * dp[node][1] = maximum sum in subtree if we TAKE this node
 *
 * Transition:
 *   take(node) = val[node] + Σ skip(child)
 *   skip(node) = Σ max(skip(child), take(child))
 *
 * Time Complexity: O(n)
 * Space Complexity: O(n)
 */
class MaximumIndependentSetTreeIterative {

    public long maxIndependentSetSum(int n, int[] val, int[][] edges) {

        // -------------------------
        // Step 1: Build adjacency list
        // -------------------------
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());

        for (int[] e : edges) {
            int u = e[0], v = e[1];
            graph.get(u).add(v);
            graph.get(v).add(u);
        }

        int root = 0;

        // dp[node][0] = skip
        // dp[node][1] = take
        long[][] dp = new long[n][2];

        // parent[node] = parent of node in rooted tree
        int[] parent = new int[n];
        Arrays.fill(parent, -1);

        // order list will store DFS order
        // reversing it gives postorder
        List<Integer> order = new ArrayList<>();

        // -------------------------
        // Step 2: Iterative DFS to build parent[] and order[]
        // -------------------------
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(root);
        parent[root] = root; // mark visited

        while (!stack.isEmpty()) {
            int node = stack.pop();
            order.add(node);

            for (int nei : graph.get(node)) {
                if (parent[nei] == -1) {
                    parent[nei] = node;
                    stack.push(nei);
                }
            }
        }

        // -------------------------
        // Step 3: Process nodes in reverse order (postorder)
        // -------------------------
        for (int i = order.size() - 1; i >= 0; i--) {
            int node = order.get(i);

            long take = val[node]; // if we take this node
            long skip = 0;         // if we skip this node

            for (int child : graph.get(node)) {
                if (child == parent[node]) continue;

                // If we TAKE node => child must be SKIPPED
                take += dp[child][0];

                // If we SKIP node => child can be taken or skipped
                skip += Math.max(dp[child][0], dp[child][1]);
            }

            dp[node][0] = skip;
            dp[node][1] = take;
        }

        // Final answer from root
        return Math.max(dp[root][0], dp[root][1]);
    }
}


/**
 * Maximum Weight Independent Set in a Tree
 *
 * Independent Set:
 * A set of nodes such that no two chosen nodes are directly connected by an
 * edge.
 *
 * Goal:
 * Pick a subset of nodes with maximum sum of values.
 *
 * This is a classic Tree DP problem.
 */
class MaximumIndependentSetTree1 {

    /**
     * Time Complexity: O(n)
     * - Building adjacency list: O(n)
     * - DFS traversal to compute parent/order: O(n)
     * - DP computation (each edge visited once): O(n)
     *
     * Space Complexity: O(n)
     * - Graph adjacency list: O(n)
     * - parent array: O(n)
     * - order list: O(n)
     * - dpTake/dpSkip arrays: O(n)
     */
    public long maxIndependentSetSum(int n, int[] val, int[][] edges) {

        // -------------------------------
        // STEP 1: Build adjacency list
        // -------------------------------
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        // Since it's an undirected tree, add both directions
        for (int[] e : edges) {
            int u = e[0], v = e[1];
            graph.get(u).add(v);
            graph.get(v).add(u);
        }

        // We can pick any node as root (tree DP works for any root)
        int root = 0;

        // -------------------------------
        // STEP 2: Build parent[] and DFS order
        // -------------------------------
        // parent[x] = parent of node x in rooted tree
        // This avoids revisiting the parent edge.
        int[] parent = new int[n];
        Arrays.fill(parent, -1);

        // order[] stores nodes in DFS visitation order.
        // Later, reversing it gives a postorder traversal
        // (children processed before parent).
        List<Integer> order = new ArrayList<>();

        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(root);
        parent[root] = root; // mark root as visited

        // DFS traversal (iterative)
        while (!stack.isEmpty()) {
            int node = stack.pop();
            order.add(node);

            for (int nei : graph.get(node)) {
                if (parent[nei] == -1) { // not visited yet
                    parent[nei] = node;
                    stack.push(nei);
                }
            }
        }

        // -------------------------------
        // STEP 3: Tree DP arrays
        // -------------------------------
        // dpTake[node] = max sum in subtree if we TAKE this node
        // dpSkip[node] = max sum in subtree if we SKIP this node
        long[] dpTake = new long[n];
        long[] dpSkip = new long[n];

        // -------------------------------
        // STEP 4: Process nodes in postorder
        // -------------------------------
        // Reverse order ensures children are computed before parent
        for (int i = order.size() - 1; i >= 0; i--) {
            int node = order.get(i);

            // If we take this node, we add its value
            // and we MUST skip all children.
            long take = val[node];

            // If we skip this node, children can be either taken or skipped.
            long skip = 0;

            for (int nei : graph.get(node)) {

                // Ignore parent edge to prevent going upward
                if (nei == parent[node])
                    continue;

                // TAKE node => children must be SKIPPED
                take += dpSkip[nei];

                // SKIP node => children can be taken or skipped (choose best)
                skip += Math.max(dpTake[nei], dpSkip[nei]);
            }

            dpTake[node] = take;
            dpSkip[node] = skip;
        }

        // Root answer = max of taking root or skipping root
        return Math.max(dpTake[root], dpSkip[root]);
    }
}

/**
 * Maximum Weight Independent Set in a Tree (Recursive + Memo)
 *
 * Independent Set:
 * No two selected nodes are directly connected.
 *
 * DP Idea:
 * For each node, we compute:
 *
 * take(node) = maximum sum in subtree if we INCLUDE this node
 * skip(node) = maximum sum in subtree if we EXCLUDE this node
 *
 * Recurrence:
 * take(node) = val[node] + Σ skip(child)
 * skip(node) = Σ max(take(child), skip(child))
 */
class MaximumIndependentSetTree2 {

    private List<List<Integer>> graph;
    private int[] val;

    // memoTake[node] = computed dpTake value (if not null)
    // memoSkip[node] = computed dpSkip value (if not null)
    private Long[] memoTake;
    private Long[] memoSkip;

    /**
     * Time Complexity: O(n)
     * - Each node is computed once for take() and skip()
     *
     * Space Complexity: O(n)
     * - adjacency list: O(n)
     * - memo arrays: O(n)
     * - recursion stack: O(n) worst case (skew tree)
     */
    public long maxIndependentSetSum(int n, int[] val, int[][] edges) {
        this.val = val;

        // -------------------------------
        // STEP 1: Build adjacency list
        // -------------------------------
        graph = new ArrayList<>();
        for (int i = 0; i < n; i++)
            graph.add(new ArrayList<>());

        for (int[] e : edges) {
            int u = e[0], v = e[1];
            graph.get(u).add(v);
            graph.get(v).add(u);
        }

        // -------------------------------
        // STEP 2: Initialize memo arrays
        // -------------------------------
        memoTake = new Long[n];
        memoSkip = new Long[n];

        // We can root the tree at any node, choose 0
        int root = 0;

        long takeRoot = take(root, -1);
        long skipRoot = skip(root, -1);

        return Math.max(takeRoot, skipRoot);
    }

    /**
     * take(node, parent) means:
     * maximum sum in subtree rooted at node
     * IF we INCLUDE this node.
     *
     * If we include node, we cannot include its children.
     */
    private long take(int node, int parent) {

        // Memoization: if already computed, reuse
        if (memoTake[node] != null) {
            return memoTake[node];
        }

        // Start with taking current node's value
        long sum = val[node];

        // Since node is taken, all children must be skipped
        for (int nei : graph.get(node)) {
            if (nei == parent)
                continue;
            sum += skip(nei, node);
        }

        memoTake[node] = sum;
        return sum;
    }

    /**
     * skip(node, parent) means:
     * maximum sum in subtree rooted at node
     * IF we EXCLUDE this node.
     *
     * If we skip node, each child can be either taken or skipped.
     */
    private long skip(int node, int parent) {

        // Memoization: if already computed, reuse
        if (memoSkip[node] != null) {
            return memoSkip[node];
        }

        long sum = 0;

        // Since node is skipped, children can be either taken or skipped
        for (int nei : graph.get(node)) {
            if (nei == parent)
                continue;

            long takeChild = take(nei, node);
            long skipChild = skip(nei, node);

            sum += Math.max(takeChild, skipChild);
        }

        memoSkip[node] = sum;
        return sum;
    }
}
