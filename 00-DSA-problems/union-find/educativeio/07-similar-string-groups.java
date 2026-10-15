/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given an array of strings `strs` (where all strings are anagrams of each other), 
 * two strings are "similar" if they are exactly the same or can be made identical 
 * by swapping exactly two characters in one of the strings.
 * We define a "similarity group" as a set of strings connected by similarities.
 * Return the total number of similarity groups.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. Does the input contain duplicate strings?
 *    (Yes, exact matches are considered "similar" (0 swaps). We must handle duplicates properly).
 * 2. Are we guaranteed that all strings are strictly anagrams of each other?
 *    (Yes. This is crucial because it means if two strings differ in exactly 2 positions, a single swap WILL fix them. We don't have to check character content, just positional differences).
 * 3. Is transitivity implied for groups? 
 *    (Yes, if A is similar to B, and B is similar to C, A and C are in the same group even if they differ by > 2 characters).
 * 4. What are the maximum bounds for N (number of strings) and L (length of each string)?
 *    (Both are <= 300. This means an O(N^2 * L) algorithm takes about 300*300*300 = 27,000,000 operations, which easily runs well within a 1-second time limit).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * The Binding Constraint: We need to group elements based on a transitive property 
 * (similarity). The core challenge is efficiently checking similarity and properly 
 * merging groups without over-complicating the graph state.
 * 
 * --- Approach 1: Depth-First Search (DFS) or Breadth-First Search (BFS) ---
 * Idea: Treat each string as a node in an undirected graph. There is an edge between 
 * two nodes if they are similar (differ in exactly 0 or 2 positions).
 * What I'd naturally try:
 *   1. Create an Adjacency List `List<List<Integer>> graph`.
 *   2. Use a nested loop to compare every string with every other string. If similar, add edges.
 *   3. Iterate through nodes 0 to N-1. If a node is unvisited, launch a DFS to mark all 
 *      connected nodes as visited, and increment the `groupCount`.
 * Why it works: Standard connected components algorithm.
 * Why it's slightly heavy: Building the explicit N x N adjacency list requires O(N^2) memory 
 * arrays, and DFS recursion depth could technically reach O(N), risking StackOverflow on 
 * constrained environments, though safe for N=300.
 * 
 * Time Complexity: O(N^2 * L) — because we compare every pair of strings (O(N^2)), and each comparison takes O(L) time. DFS traversal takes O(N^2) in the worst case.
 * Space Complexity: O(N^2) — because the adjacency list can have up to N*(N-1)/2 edges if all strings are similar, plus O(N) for the visited array and recursion stack.
 * 
 * --- Approach 2: Union-Find / Disjoint Set (Optimal) ---
 * Idea: Skip building the graph entirely! We only care about *counting* the groups, 
 * not traversing the paths between them. 
 * We initialize a Union-Find structure where every string is its own group (count = N). 
 * We use a nested loop to compare all pairs (i, j). 
 * If `strs[i]` and `strs[j]` are similar, we simply `union(i, j)`. Every successful 
 * union operation merges two components, so we decrement our global group count.
 * 
 * Time Complexity: O(N^2 * L) — We still check all pairs (N^2), doing an O(L) string comparison. The Union-Find operations take amortized O(1) time. The overall time is dominated by string comparisons.
 * Space Complexity: O(N) — We only need a `parent` array of size N for the Disjoint Set. This is a massive memory improvement over the O(N^2) adjacency list!
 * 
 * Interview Decision: I would write **Union-Find**. It is explicitly designed to solve 
 * "equivalence class" / "group counting" problems, requires significantly less memory setup 
 * than DFS/BFS, and demonstrates clean architectural thinking.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * 1. N = 1: Array of one string. Output should be 1. (UF initialized to N handles this).
 * 2. Identical Strings: `["abc", "abc"]`. The difference is 0. Condition `diff == 0 || diff == 2` handles this.
 * 3. Completely disconnected strings: `["abc", "bca", "cab"]`. Each requires 3+ positional changes. Result = 3 groups.
 * 
 * ============================================================================
 * 4. ALSO INCLUDE
 * ============================================================================
 * --- Key Insight ---
 * Ask yourself: "How do I mathematically define 'one swap away' for anagrams?"
 * Correctness intuition: Because the strings are guaranteed to have the exact same 
 * characters, if they differ in exactly 2 positional indices, swapping those two 
 * specific characters is mathematically guaranteed to make them identical.
 * 
 * --- Examples & ASCII ---
 * strs = ["tars", "rats", "arts", "star"]
 * 
 * "tars" vs "rats" -> differ at index 0 and 1. Diff = 2. (Similar)
 * "rats" vs "arts" -> differ at index 0 and 1. Diff = 2. (Similar)
 * "arts" vs "star" -> differ at 0, 1, 2, 3. Diff = 4. (NOT similar)
 * 
 * Groupings:
 * [tars] - [rats] - [arts]      [star]
 * \______________________/     \______/
 *         Group 1               Group 2
 * 
 * Result: 2 groups. Note that "tars" and "arts" are in the same group because they 
 * connect transitively through "rats", even though they differ by 4 positions!
 * 
 * --- Dry Run (Union-Find) ---
 * Input: ["tars", "rats", "arts", "star"]
 * Init UF: parent = [0,1,2,3], count = 4
 * 
 * i=0 ("tars"):
 *   j=1 ("rats"): diff=2 -> Similar! union(0, 1). parent=[0,0,2,3], count=3.
 *   j=2 ("arts"): diff=4 -> Skip.
 *   j=3 ("star"): diff=4 -> Skip.
 * i=1 ("rats"):
 *   j=2 ("arts"): diff=2 -> Similar! union(1, 2). root(1)=0, root(2)=2. parent=[0,0,0,3], count=2.
 *   j=3 ("star"): diff=4 -> Skip.
 * i=2 ("arts"):
 *   j=3 ("star"): diff=4 -> Skip.
 * 
 * Final count = 2.
 * 
 * --- Pitfalls ---
 * 1. Re-checking characters: Doing an expensive character-frequency map check. The prompt 
 *    says "all strings are anagrams of each other", so checking frequencies is wasted CPU cycles.
 * 2. Returning early on diff: If you count differences and find 1, you might be tempted to exit. 
 *    Wait, anagrams can NEVER differ by exactly 1 character! If one changes, another must change to balance it.
 * 3. Early termination: `if (diff > 2) return false;` is a crucial optimization inside the 
 *    string comparison loop to avoid parsing 300 chars when the first 3 don't match.
 * 
 * --- Pattern Recognition ---
 * When you see: "Groups based on similarity", "Transitive relations", "Connected components".
 * Think: Union-Find (Disjoint Set).
 * Transfers to: Number of Islands, Accounts Merge, Graph Valid Tree.
 * 
 * --- Interview Script ---
 * "Since we are trying to group strings based on a transitive relationship, this is fundamentally 
 * a connected components problem. Rather than building an adjacency list and running DFS, which 
 * requires O(N^2) space, I'll use a Union-Find data structure. I'll initialize N disconnected groups. 
 * Then I'll iterate through all unique pairs of strings. Because they are guaranteed to be anagrams, 
 * two strings are similar if they differ in exactly 0 or 2 positions. If they are similar, I'll union 
 * their sets and decrement the total group count. This perfectly tracks transitivity in O(N^2 * L) 
 * time and only O(N) space."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if N is massive (e.g., 100,000) but L is very small (e.g., 10)?
 * A: O(N^2 * L) would become (10^10 * 10) = 100 Billion operations (Time Limit Exceeded). 
 *    Instead, we'd pivot: for each string, generate ALL possible valid swaps. A string of length 10 
 *    has (10 choose 2) = 45 possible valid swaps. We check if those swapped strings exist in a 
 *    HashSet of the input. This drops the time to O(N * L^2), which is (10^5 * 100) = 10,000,000 operations.
 * Q2: What if they weren't guaranteed to be anagrams?
 * A: Our `isSimilar` function would fail because "abc" and "def" might differ in 3 positions, 
 *    but "aba" and "aca" might differ in 1. We would need to add a frequency array check to ensure 
 *    they actually hold the same characters before verifying the 2-position difference.
 * 
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core pattern: Disjoint Set / Union-Find over pairs.
 * Key observation: Anagrams are 1 swap away iff they differ in exactly 0 or 2 positions.
 * Memorize: Early exit `if (diff > 2) return false;` in the similarity checker.
 * Most common trap: Trying to build an explicit N x N adjacency matrix for DFS.
 * Mental trigger: "Groups of similar items" -> Union-Find.
 */

 public class SimilarStringGroups {

    public int numSimilarGroups(String[] strs) {
        int n = strs.length;
        UnionFind uf = new UnionFind(n);
        
        // Compare every unique pair of strings
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                // If they are similar, group them
                if (isSimilar(strs[i], strs[j])) {
                    uf.union(i, j);
                }
            }
        }
        
        // The number of remaining disjoint sets is our answer
        return uf.getGroupCount();
    }
    
    /**
     * Determines if two strings are identical or 1 swap away.
     * Since the prompt guarantees they are anagrams, we only need to count 
     * how many indices have non-matching characters.
     */
    private boolean isSimilar(String a, String b) {
        int diffCount = 0;
        
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                diffCount++;
                
                // Early optimization: A single swap can only fix exactly 2 mismatches. 
                // If we find 3 or more, it's impossible to fix with one swap.
                if (diffCount > 2) {
                    return false;
                }
            }
        }
        
        // 0 means identical, 2 means exactly 1 swap away.
        // (Note: diffCount == 1 is impossible for valid anagrams).
        return diffCount == 0 || diffCount == 2;
    }

    /**
     * Standard Disjoint Set (Union-Find) tailored to count connected components.
     */
    private class UnionFind {
        private int[] parent;
        private int groupCount;
        
        public UnionFind(int n) {
            parent = new int[n];
            groupCount = n; // Start with N isolated groups
            
            for (int i = 0; i < n; i++) {
                parent[i] = i; // Every node is its own root
            }
        }
        
        // Find the root of node 'i' using Path Compression
        public int find(int i) {
            if (parent[i] == i) {
                return i;
            }
            // Point the node directly to its ultimate root to flatten the tree
            return parent[i] = find(parent[i]);
        }
        
        // Unites the groups containing 'i' and 'j'
        public void union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            
            // If they don't already share a root, merge them
            if (rootI != rootJ) {
                parent[rootI] = rootJ;
                // Two groups merged into one, so the total distinct groups decreases
                groupCount--;
            }
        }
        
        public int getGroupCount() {
            return groupCount;
        }
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING / CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        SimilarStringGroups solution = new SimilarStringGroups();

        // Test Case 1: Standard transitive chain
        // "tars" and "rats" are similar (diff 2)
        // "rats" and "arts" are similar (diff 2)
        // "star" is isolated
        String[] strs1 = {"tars", "rats", "arts", "star"};
        System.out.println("Test 1 Result: " + solution.numSimilarGroups(strs1)); // Expected: 2

        // Test Case 2: All identical strings (0 diff)
        String[] strs2 = {"omv", "omv"};
        System.out.println("Test 2 Result: " + solution.numSimilarGroups(strs2)); // Expected: 1

        // Test Case 3: Totally disconnected
        // All differ by 3+ positions
        String[] strs3 = {"abc", "bca", "cab"};
        System.out.println("Test 3 Result: " + solution.numSimilarGroups(strs3)); // Expected: 3
        
        // Test Case 4: Single string
        String[] strs4 = {"alone"};
        System.out.println("Test 4 Result: " + solution.numSimilarGroups(strs4)); // Expected: 1
    }
}



class Solution {

    public int numSimilarGroups(String[] strs) {
        int n = strs.length;

        // STEP 1:
        // Initially, every string is a separate group.
        //
        // Example:
        // strs = ["tars", "rats", "arts", "star"]
        //
        // Index:  0      1      2      3
        // Groups: {0}    {1}    {2}    {3}
        //
        // Therefore, the initial group count is 4.
        UnionFind uf = new UnionFind(n);

        // STEP 2:
        // Compare every pair of strings.
        //
        // i represents the first string.
        // j represents the second string.
        //
        // Start j at i + 1 because:
        // - We don't need to compare a string with itself.
        // - Similarity is symmetric, so comparing (0, 1)
        //   is sufficient; we don't need to compare (1, 0).
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {

                // STEP 3:
                // If these two strings are directly similar,
                // their indices must belong to the same group.
                if (areSimilar(strs[i], strs[j])) {

                    // This merges their groups ONLY if they
                    // are currently separate groups.
                    uf.union(i, j);
                }
            }
        }

        // STEP 4:
        // After checking every pair, all required connections
        // have been processed. The remaining component count
        // is the number of similarity groups.
        return uf.getGroupCount();
    }

    private boolean areSimilar(String x, String y) {

        // Remember the positions where x and y differ.
        // We only need to remember at most two positions.
        int firstMismatch = -1;
        int secondMismatch = -1;

        int mismatchCount = 0;

        // Compare corresponding characters in both strings.
        for (int i = 0; i < x.length(); i++) {

            // If the characters match, this position is fine.
            if (x.charAt(i) == y.charAt(i)) {
                continue;
            }

            // Otherwise, this position is a mismatch.
            mismatchCount++;

            // A single swap can change at most two positions.
            // A third mismatch proves that one swap is insufficient.
            if (mismatchCount > 2) {
                return false;
            }

            // Store the first mismatching position.
            if (firstMismatch == -1) {
                firstMismatch = i;
            } else {
                // Store the second mismatching position.
                secondMismatch = i;
            }
        }

        // CASE 1:
        // No mismatches means the strings are identical.
        if (mismatchCount == 0) {
            return true;
        }

        // CASE 2:
        // Exactly two mismatches are allowed.
        //
        // Because all input strings are anagrams, the two
        // mismatched characters must be swapped versions
        // of each other.
        //
        // Verify explicitly that swapping these positions works.
        return mismatchCount == 2
                && x.charAt(firstMismatch) == y.charAt(secondMismatch)
                && x.charAt(secondMismatch) == y.charAt(firstMismatch);
    }

    private static class UnionFind {

        // parent[i] tells us the parent of node i.
        // A root is identified by parent[root] == root.
        private final int[] parent;

        // size[root] stores the number of nodes in that root's
        // component. We use it to keep the trees shallow.
        private final int[] size;

        // Number of connected components currently remaining.
        private int groups;

        UnionFind(int n) {
            parent = new int[n];
            size = new int[n];

            // Initially, every node is a separate component.
            groups = n;

            for (int i = 0; i < n; i++) {
                parent[i] = i; // Every node starts as its own root.
                size[i] = 1;   // Every component initially has one node.
            }
        }

        int find(int node) {

            // If node is its own parent, it is the root.
            if (parent[node] == node) {
                return node;
            }

            // PATH COMPRESSION:
            // Recursively find the root and make this node point
            // directly to it.
            //
            // This shortens the path for future find() calls.
            parent[node] = find(parent[node]);

            return parent[node];
        }

        void union(int a, int b) {

            // Find the roots of the components containing a and b.
            int rootA = find(a);
            int rootB = find(b);

            // If both roots are identical, these nodes are already
            // connected. Merging them again would change nothing.
            if (rootA == rootB) {
                return;
            }

            // UNION BY SIZE:
            // Keep the larger component as the parent.
            // This prevents unnecessarily tall trees.
            if (size[rootA] < size[rootB]) {
                int temp = rootA;
                rootA = rootB;
                rootB = temp;
            }

            // Attach the smaller component's root to the larger one.
            parent[rootB] = rootA;

            // The resulting component contains nodes from both trees.
            size[rootA] += size[rootB];

            // Two previously separate components have become one.
            groups--;
        }

        int getGroupCount() {
            return groups;
        }
    }
}

