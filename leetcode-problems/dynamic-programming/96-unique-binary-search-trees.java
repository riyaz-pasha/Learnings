// Given an integer n, return the number of structurally unique BST's (binary search trees)
// which has exactly n nodes of unique values from 1 to n.

/**
 * Solution 1: Dynamic Programming (Bottom-up)
 * Time: O(n²), Space: O(n)
 */
class Solution1 {

    public int numTrees(int n) {
        // dp[i] represents number of unique BSTs with i nodes
        int[] dp = new int[n + 1];
        dp[0] = 1; // Empty tree
        dp[1] = 1; // Single node tree

        // For each number of nodes from 2 to n
        for (int nodes = 2; nodes <= n; nodes++) {
            // Try each value as root (1 to nodes)
            for (int root = 1; root <= nodes; root++) {
                // Left subtree has (root-1) nodes
                // Right subtree has (nodes-root) nodes
                int leftTrees = dp[root - 1];
                int rightTrees = dp[nodes - root];
                dp[nodes] += leftTrees * rightTrees;
            }
        }

        return dp[n];
    }

}

/**
 * Solution 2: Memoized Recursion (Top-down)
 * Time: O(n²), Space: O(n)
 */
class Solution2 {

    private Integer[] memo;

    public int numTrees(int n) {
        memo = new Integer[n + 1];
        return helper(n);
    }

    private int helper(int n) {
        if (n <= 1)
            return 1;
        if (memo[n] != null)
            return memo[n];

        int result = 0;
        for (int i = 1; i <= n; i++) {
            result += helper(i - 1) * helper(n - i);
        }

        memo[n] = result;
        return result;
    }

}

/**
 * Solution 3: Mathematical Formula (Catalan Number)
 * Time: O(n), Space: O(1)
 * 
 * The nth Catalan number: C(n) = (2n)! / ((n+1)! * n!)
 * Or: C(n) = C(n-1) * 2(2n-1) / (n+1)
 */
class Solution3 {

    public int numTrees(int n) {
        long catalan = 1;

        for (int i = 0; i < n; i++) {
            catalan = catalan * 2 * (2 * i + 1) / (i + 2);
        }

        return (int) catalan;
    }

}

/**
 * Solution 4: Clean DP with explanation
 * Time: O(n²), Space: O(n)
 */
class Solution4 {

    public int numTrees(int n) {
        int[] G = new int[n + 1];
        G[0] = G[1] = 1;

        for (int i = 2; i <= n; ++i) {
            for (int j = 1; j <= i; ++j) {
                G[i] += G[j - 1] * G[i - j];
            }
        }

        return G[n];
    }

}

/*
 * EXPLANATION:
 * 
 * The problem asks for the number of structurally unique BSTs with n nodes
 * containing values 1 to n.
 * 
 * Key insight: For any root value i (1 ≤ i ≤ n):
 * - Left subtree contains values 1 to i-1 (i-1 nodes)
 * - Right subtree contains values i+1 to n (n-i nodes)
 * - Total combinations = (ways to arrange left) × (ways to arrange right)
 * 
 * This gives us the recurrence:
 * G(n) = Σ(i=1 to n) G(i-1) × G(n-i)
 * 
 * Where G(n) is the number of unique BSTs with n nodes.
 * 
 * Base cases:
 * - G(0) = 1 (empty tree)
 * - G(1) = 1 (single node)
 * 
 * Examples:
 * - n=1: [1] → 1 tree
 * - n=2: [1,2] and [2,1] → 2 trees
 * - n=3: 5 trees with different structures
 * 
 * This sequence follows Catalan numbers: 1, 1, 2, 5, 14, 42, ...
 * 
 * Time Complexities:
 * - DP Solutions: O(n²) - we fill n entries, each taking O(n) time
 * - Catalan Formula: O(n) - direct calculation
 * - Space: O(n) for DP array, O(1) for formula approach
 */


// Tracing for n = 4:
// dp array initialized: [1, 1, 0, 0, 0]

// For each number of nodes from 2 to n
// nodes = 2:
//   root = 1:
//     leftTrees = dp[0] = 1
//     rightTrees = dp[1] = 1
//     dp[2] += 1 * 1 = 1
//     dp array: [1, 1, 1, 0, 0]
//   root = 2:
//     leftTrees = dp[1] = 1
//     rightTrees = dp[0] = 1
//     dp[2] += 1 * 1 = 1
//     dp array: [1, 1, 2, 0, 0] (dp[2] is now 2)

// nodes = 3:
//   root = 1:
//     leftTrees = dp[0] = 1
//     rightTrees = dp[2] = 2
//     dp[3] += 1 * 2 = 2
//     dp array: [1, 1, 2, 2, 0]
//   root = 2:
//     leftTrees = dp[1] = 1
//     rightTrees = dp[1] = 1
//     dp[3] += 1 * 1 = 1
//     dp array: [1, 1, 2, 3, 0] (dp[3] is now 3)
//   root = 3:
//     leftTrees = dp[2] = 2
//     rightTrees = dp[0] = 1
//     dp[3] += 2 * 1 = 2
//     dp array: [1, 1, 2, 5, 0] (dp[3] is now 5)

// nodes = 4:
//   root = 1:
//     leftTrees = dp[0] = 1
//     rightTrees = dp[3] = 5
//     dp[4] += 1 * 5 = 5
//     dp array: [1, 1, 2, 5, 5]
//   root = 2:
//     leftTrees = dp[1] = 1
//     rightTrees = dp[2] = 2
//     dp[4] += 1 * 2 = 2
//     dp array: [1, 1, 2, 5, 7] (dp[4] is now 7)
//   root = 3:
//     leftTrees = dp[2] = 2
//     rightTrees = dp[1] = 1
//     dp[4] += 2 * 1 = 2
//     dp array: [1, 1, 2, 5, 9] (dp[4] is now 9)
//   root = 4:
//     leftTrees = dp[3] = 5
//     rightTrees = dp[0] = 1
//     dp[4] += 5 * 1 = 5
//     dp array: [1, 1, 2, 5, 14] (dp[4] is now 14)

// Returns dp[4] = 14
