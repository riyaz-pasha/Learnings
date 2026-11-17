/**
 * 0. ## Problem Statement
 * Given a 0-indexed string `s` and `k` point-update queries (updating a character at a specific index),
 * return an array of length `k` where the i-th element is the length of the longest substring 
 * consisting of only one repeating character after the i-th query.
 * 
 * 
 * 1. ## Clarifying questions
 * Q: Are the queries cumulative?
 * A: Yes, each query mutates the string permanently for all future queries. (Crucial, as it means we must maintain state).
 * 
 * Q: Do we only need the max length for the *entire* string, or will we be asked to query specific ranges [left, right]?
 * A: Only the entire string. (This simplifies our Segment Tree, as we don't need a complex `queryRange` function; the root node will always hold the answer).
 * 
 * Q: What is the alphabet?
 * A: Lowercase English letters. (Good to know, though our logic only requires checking equality).
 * 
 * 
 * 2. ## The reasoning journey
 * Constraint Check: String length `N` and number of queries `K` are up to 10^5. 
 * An O(N) operation per query results in 10^10 operations, leading to Time Limit Exceeded (TLE). 
 * The binding constraint is that we must maintain and query the string's state in O(log N) time.
 * 
 * Approach 1: Brute Force
 * - Idea: Mutate a `char[]` array for each update, then linearly scan it to count the max consecutive identical characters.
 * - Why it works: It directly simulates the problem.
 * - Why it's too slow: A single character change only affects local boundaries, but we are discarding all prior knowledge and recounting the entire string.
 * - Time Complexity: O(N) per query, O(K * N) total — because we scan the whole string of length N for each of the K queries.
 * - Space Complexity: O(N) — because we store the `char[]` array of the string.
 * 
 * Approach 2: TreeSet of Intervals
 * - Idea: Maintain contiguous segments of identical characters as intervals `[start, end, char]` in a balanced BST (like `TreeSet` in Java).
 * - Why it works: When a char changes, we remove the old interval, potentially split it, and check if the new char merges with left/right adjacent intervals.
 * - Why it's risky: While O(log N) time, interval splitting/merging is notorious for edge cases (updating an edge, updating the middle, updating to the same char).
 * - Time Complexity: O(log N) per query — because we do a few BST lookups and modifications.
 * - Space Complexity: O(N) — because in the worst case (e.g., "ababab"), we store N intervals.
 * 
 * Approach 3: Segment Tree (Recursive) -> The Optimal Choice
 * - Idea: Use a divide-and-conquer tree. If we know the longest repeating string in the Left half and the Right half, can we combine them? 
 *   Yes, IF we also track the prefix and suffix of each half. If the suffix of Left matches the prefix of Right, they merge!
 * - Why it works: Updating a character only affects exactly one leaf and its log(N) ancestors. The root always holds the global max.
 * - Time Complexity: O(log N) per query — because we traverse one path from root to leaf and back.
 * - Space Complexity: O(N) — because the tree requires 4*N nodes.
 * 
 * Approach 4: Segment Tree (Iterative) -> The High-Performance Alternative
 * - Idea: Flatten the recursive Segment Tree into a 1D array, building from the bottom up.
 * - Why it works: Eliminates recursion overhead (`StackOverflowError` risk and method frame allocation).
 * - The Catch: For non-commutative operations (where Left + Right != Right + Left, which is true for string concatenation), standard 2N iterative trees scramble the contiguous order unless N is a power of 2. We MUST pad N to the next power of 2.
 * - Time Complexity: O(log N) per query — because we just jump `index /= 2` to the root.
 * - Space Complexity: O(N) — requires an array of size 2 * (Next Power of 2), which is at most 4N.
 * 
 * 
 * 3. ## Edge cases
 * - N = 1 (Single character string): Update affects the whole string. Boundary logic (`preLen == size`) must handle `size 1` properly.
 * - Updating to the *same* character: The tree state won't change, but we still do the O(log N) update. Can be optimized with a fast-fail check.
 * - A single character bridges two massive segments into one: e.g., "aaaa b aaaa" -> update index 4 to 'a' -> "aaaaaaaaa". The merge logic handles this natively.
 * 
 * 
 * 4. ## Also include
 * 
 * Key insight: 
 * "How do I merge two string segments in O(1) time?" 
 * We don't care about the middle of the strings. We only care about the boundaries!
 * To merge `L` and `R`, the new max length is `max(L.max, R.max)`. 
 * BUT, if `L`'s suffix character equals `R`'s prefix character, a new sequence forms across the boundary: `L.suffixLength + R.prefixLength`.
 * 
 * Dry run of Merge:
 * Let L = "aaab" (size 4), R = "baaa" (size 4)
 * L states: prefC='a', prefL=3, sufC='b', sufL=1, maxL=3
 * R states: prefC='b', prefL=1, sufC='a', sufL=3, maxL=3
 * Merging L and R:
 * - Does L's suffix match R's prefix? ('b' == 'b'). YES.
 * - Boundary sequence length = L.sufL (1) + R.prefL (1) = 2.
 * - New maxL = max(L.maxL(3), R.maxL(3), boundary(2)) = 3.
 * - New prefL = L.prefL (3). (Since L wasn't ALL 'b', it doesn't extend into R).
 * - New sufL = R.sufL (3).
 * Result: prefC='a', prefL=3, sufC='a', sufL=3, maxL=3, size=8.
 * 
 * Pitfalls:
 * - Iterative Tree Scrambling: If N=5, a standard 2N iterative array puts leaves at indices 5,6,7,8,9. 
 *   Node 4 merges 8 and 9. Node 2 merges 4 and 5. 
 *   Wait! Node 2 is merging (Leaf 8+9) with (Leaf 5). Original array elements [3,4] are being placed BEFORE element [0]! 
 *   For range minimums this is fine, but for contiguous Strings, order matters. You MUST pad N to the next power of 2 for Iterative Segment Trees handling contiguous order.
 * - Forgetting that if a child is entirely one character (e.g., "aaa"), its prefix CAN merge through the entire node and extend into the other child. 
 *   Condition: `if (L.prefLen == L.size && L.sufC == R.prefC)`.
 * 
 * Pattern recognition:
 * - "Point update + Global/Range Query on contiguous data = Segment Tree". 
 * - Transfers to: Maximum Subarray Sum (maintaining max prefix sum, max suffix sum).
 * 
 * Interview script:
 * "Since we are doing point updates and need the maximum repeating substring, an O(N) scan per query will TLE. 
 * I will use a Segment Tree. To allow two adjacent tree nodes to merge, each node must track its prefix character and length, 
 * suffix character and length, the max repeating length internally, and its total size. 
 * When updating, we update a leaf and recalculate its ancestors. The root node will always contain the global maximum. 
 * This gives us O(log N) time per query and O(N) space."
 * 
 * 
 * 5. ## Follow-ups
 * Q: What if instead of the whole string, the query asked for the longest repeating character in a specific range [start, end]?
 * A: We would write a `query(node, start, end)` function. When the range splits across two children, we would return a temporary `Node` that is the result of `merge(query(left), query(right))`. Our state tracking is perfectly suited for this.
 * 
 * Q: What if we needed to insert or delete characters, changing the string length dynamically?
 * A: A static array Segment Tree wouldn't work. We would need a Treap (Randomized Binary Search Tree) or a Splay Tree, where each node maintains the segment states but can structurally split and merge to support O(log N) inserts/deletes.
 */

import java.util.Arrays;

public class LongestSubstringRepeatingChar {

    // Common Node structure used by both Segment Tree implementations
    static class Node {
        int maxL, preL, sufL, size;
        char preC, sufC;

        // Default empty node
        Node() {
            this.size = 0;
            this.maxL = 0;
            this.preL = 0;
            this.sufL = 0;
            this.preC = ' '; // dummy
            this.sufC = ' '; // dummy
        }

        // Leaf node initialization
        Node(char c) {
            this.size = 1;
            this.maxL = 1;
            this.preL = 1;
            this.sufL = 1;
            this.preC = c;
            this.sufC = c;
        }

        // Copies state from another node (used to avoid creating new objects)
        void copyFrom(Node other) {
            this.maxL = other.maxL;
            this.preL = other.preL;
            this.sufL = other.sufL;
            this.size = other.size;
            this.preC = other.preC;
            this.sufC = other.sufC;
        }
    }

    /**
     * Merges two child nodes into a parent node. 
     * Extracted as a common utility for both Recursive and Iterative approaches.
     */
    private static void merge(Node parent, Node left, Node right) {
        // Handle empty/dummy nodes safely
        if (left.size == 0) { parent.copyFrom(right); return; }
        if (right.size == 0) { parent.copyFrom(left); return; }

        parent.size = left.size + right.size;
        parent.preC = left.preC;
        parent.sufC = right.sufC;

        // 1. Calculate new prefix length
        parent.preL = left.preL;
        // If the left child is made entirely of one character AND it matches the right child's prefix
        if (left.preL == left.size && left.sufC == right.preC) {
            parent.preL += right.preL;
        }

        // 2. Calculate new suffix length
        parent.sufL = right.sufL;
        // If the right child is entirely one character AND it matches the left child's suffix
        if (right.sufL == right.size && right.preC == left.sufC) {
            parent.sufL += left.sufL;
        }

        // 3. Calculate new max length
        // It's at least the max of either child
        parent.maxL = Math.max(left.maxL, right.maxL);
        // But it could be the bridge where they meet!
        if (left.sufC == right.preC) {
            parent.maxL = Math.max(parent.maxL, left.sufL + right.preL);
        }
    }

    // ============================================================================
    // Approach 3: Recursive Segment Tree (Optimal, Standard)
    // ============================================================================
    static class RecursiveSegmentTree {
        Node[] tree;
        char[] arr;
        int n;

        public RecursiveSegmentTree(String s) {
            n = s.length();
            arr = s.toCharArray();
            tree = new Node[4 * n];
            for (int i = 0; i < tree.length; i++) tree[i] = new Node();
            build(1, 0, n - 1);
        }

        private void build(int node, int start, int end) {
            if (start == end) {
                tree[node] = new Node(arr[start]);
                return;
            }
            int mid = start + (end - start) / 2;
            int leftChild = 2 * node;
            int rightChild = 2 * node + 1;
            
            build(leftChild, start, mid);
            build(rightChild, mid + 1, end);
            
            merge(tree[node], tree[leftChild], tree[rightChild]);
        }

        public void update(int node, int start, int end, int idx, char val) {
            if (start == end) {
                tree[node] = new Node(val);
                return;
            }
            int mid = start + (end - start) / 2;
            int leftChild = 2 * node;
            int rightChild = 2 * node + 1;
            
            if (idx <= mid) {
                update(leftChild, start, mid, idx, val);
            } else {
                update(rightChild, mid + 1, end, idx, val);
            }
            
            // Recompute current node after child updates
            merge(tree[node], tree[leftChild], tree[rightChild]);
        }
    }

    public int[] longestRepeatingRecursive(String s, String queryCharacters, int[] queryIndices) {
        RecursiveSegmentTree st = new RecursiveSegmentTree(s);
        int[] ans = new int[queryIndices.length];
        
        for (int i = 0; i < queryIndices.length; i++) {
            st.update(1, 0, st.n - 1, queryIndices[i], queryCharacters.charAt(i));
            ans[i] = st.tree[1].maxL; // Root node always holds the global max
        }
        
        return ans;
    }


    // ============================================================================
    // Approach 4: Iterative Segment Tree (High Performance, avoids recursion stack)
    // ============================================================================
    static class IterativeSegmentTree {
        Node[] tree;
        int paddedN;

        public IterativeSegmentTree(String s) {
            int n = s.length();
            
            // To preserve contiguous order for non-commutative operations, 
            // we MUST pad N to the next power of 2.
            paddedN = 1;
            while (paddedN < n) {
                paddedN *= 2;
            }
            
            tree = new Node[2 * paddedN];
            for (int i = 0; i < tree.length; i++) tree[i] = new Node();
            
            // 1. Initialize leaves
            for (int i = 0; i < n; i++) {
                tree[paddedN + i] = new Node(s.charAt(i));
            }
            // (Indices from paddedN + n to 2*paddedN - 1 remain empty dummy nodes of size 0)
            
            // 2. Build parents bottom-up
            for (int i = paddedN - 1; i > 0; i--) {
                merge(tree[i], tree[i * 2], tree[i * 2 + 1]);
            }
        }

        public void update(int idx, char val) {
            // Shift index to leaf position
            idx += paddedN;
            tree[idx] = new Node(val);
            
            // Bubble up to root
            idx /= 2;
            while (idx > 0) {
                merge(tree[idx], tree[idx * 2], tree[idx * 2 + 1]);
                idx /= 2;
            }
        }
    }

    public int[] longestRepeatingIterative(String s, String queryCharacters, int[] queryIndices) {
        IterativeSegmentTree st = new IterativeSegmentTree(s);
        int[] ans = new int[queryIndices.length];
        
        for (int i = 0; i < queryIndices.length; i++) {
            st.update(queryIndices[i], queryCharacters.charAt(i));
            ans[i] = st.tree[1].maxL; // Root node at index 1
        }
        return ans;
    }


    // ============================================================================
    // Approach 1: Brute Force (For comparison and small constraints)
    // ============================================================================
    public int[] longestRepeatingBruteForce(String s, String queryCharacters, int[] queryIndices) {
        char[] arr = s.toCharArray();
        int[] ans = new int[queryIndices.length];
        
        for (int i = 0; i < queryIndices.length; i++) {
            // 1. Point Update
            arr[queryIndices[i]] = queryCharacters.charAt(i);
            
            // 2. Linear Scan
            int max = 1, current = 1;
            for (int j = 1; j < arr.length; j++) {
                if (arr[j] == arr[j - 1]) {
                    current++;
                } else {
                    current = 1;
                }
                max = Math.max(max, current);
            }
            ans[i] = max;
        }
        return ans;
    }


    // ============================================================================
    // Main Method for Cross-Checking
    // ============================================================================
    public static void main(String[] args) {
        LongestSubstringRepeatingChar solver = new LongestSubstringRepeatingChar();
        
        System.out.println("--- Typical Case ---");
        String s1 = "babacc";
        String qChars1 = "bcb";
        int[] qIdx1 = {1, 3, 3};
        
        System.out.println("Brute Force: " + Arrays.toString(solver.longestRepeatingBruteForce(s1, qChars1, qIdx1)));
        System.out.println("Recursive  : " + Arrays.toString(solver.longestRepeatingRecursive(s1, qChars1, qIdx1)));
        System.out.println("Iterative  : " + Arrays.toString(solver.longestRepeatingIterative(s1, qChars1, qIdx1)));
        // Expect: [3, 3, 4]
        
        System.out.println("\n--- Edge Case: Single Element ---");
        String s2 = "a";
        String qChars2 = "b";
        int[] qIdx2 = {0};
        System.out.println("Recursive  : " + Arrays.toString(solver.longestRepeatingRecursive(s2, qChars2, qIdx2)));
        System.out.println("Iterative  : " + Arrays.toString(solver.longestRepeatingIterative(s2, qChars2, qIdx2)));
        // Expect: [1]
        
        System.out.println("\n--- Edge Case: N Not Power of 2 (Demonstrating Padding) ---");
        String s3 = "abcde"; // N = 5
        String qChars3 = "aaaaa";
        int[] qIdx3 = {0, 1, 2, 3, 4};
        System.out.println("Iterative  : " + Arrays.toString(solver.longestRepeatingIterative(s3, qChars3, qIdx3)));
        // Expect: [1, 2, 3, 4, 5]
    }
}

/**
 * 7. ## Short summary
 * - Core pattern: Segment Tree tracking boundary properties (prefix/suffix lengths and characters).
 * - Key observation: Two adjacent substrings can merge to form a longer repeating string *only* if the left suffix matches the right prefix.
 * - What's worth memorizing: The node state requires exactly `preL`, `sufL`, `maxL`, `size`, `preC`, `sufC` to accurately calculate parent state in O(1).
 * - Most common trap: Attempting an Iterative Segment Tree for non-commutative operations (like strings) without padding N to a power of 2. It will silently scramble the array order.
 * - One-line mental trigger: "Longest contiguous property with point updates = Segment Tree mapping prefixes to suffixes."
 */










































class Solution {

    // Each node summarizes a contiguous interval.
    //
    // length    = total number of characters in this interval.
    // prefix    = longest repeating-character prefix.
    // suffix    = longest repeating-character suffix.
    // best      = longest repeating-character substring anywhere.
    // leftChar  = first character in this interval.
    // rightChar = last character in this interval.
    //
    // A record is useful here because segment summaries are immutable.
    private record Node(
        int length,
        int prefix,
        int suffix,
        int best,
        char leftChar,
        char rightChar
    ) {}

    private char[] chars;
    private Node[] tree;
    private int n;

    public int[] longestRepeating(
            String s,
            String queryCharacters,
            int[] queryIndices) {

        // Convert the immutable String into a mutable character array.
        chars = s.toCharArray();

        n = chars.length;

        // A segment tree generally requires at most 4*n nodes.
        tree = new Node[4 * n];

        // Build summaries for all intervals.
        build(1, 0, n - 1);

        int k = queryIndices.length;
        int[] answer = new int[k];

        // Process queries sequentially because each update
        // affects all subsequent queries.
        for (int i = 0; i < k; i++) {

            int index = queryIndices[i];
            char newChar = queryCharacters.charAt(i);

            // Apply the point update.
            update(1, 0, n - 1, index, newChar);

            // The root represents the complete string [0, n-1].
            // Therefore, root.best is the answer.
            answer[i] = tree[1].best();
        }

        return answer;
    }

    // ---------------------------------------------------------
    // BUILD
    // ---------------------------------------------------------

    private void build(int node, int start, int end) {

        // Base case:
        // A leaf represents exactly one character.
        if (start == end) {

            char c = chars[start];

            tree[node] = new Node(
                1,  // length
                1,  // prefix
                1,  // suffix
                1,  // best
                c,  // leftChar
                c   // rightChar
            );

            return;
        }

        int mid = start + (end - start) / 2;

        // Build the left half.
        build(node * 2, start, mid);

        // Build the right half.
        build(node * 2 + 1, mid + 1, end);

        // Combine the two child summaries.
        tree[node] = merge(
            tree[node * 2],
            tree[node * 2 + 1]
        );
    }

    // ---------------------------------------------------------
    // MERGE
    // ---------------------------------------------------------

    private Node merge(Node left, Node right) {

        // Total interval length is the sum of child lengths.
        int length = left.length() + right.length();

        // The first character of the combined interval
        // always comes from the left child.
        char leftChar = left.leftChar();

        // The last character always comes from the right child.
        char rightChar = right.rightChar();

        // Initially, the combined prefix is the left prefix.
        int prefix = left.prefix();

        // We can extend the prefix into the right child only if:
        //
        // 1. The ENTIRE left child is one repeating character.
        // 2. The last character of left equals the first of right.
        //
        // left.prefix() == left.length() means the entire
        // left interval consists of the same character.
        if (left.prefix() == left.length()
                && left.rightChar() == right.leftChar()) {

            prefix += right.prefix();
        }

        // Initially, the combined suffix is the right suffix.
        int suffix = right.suffix();

        // We can extend the suffix into the left child only if:
        //
        // 1. The ENTIRE right child is one repeating character.
        // 2. The boundary characters match.
        if (right.suffix() == right.length()
                && left.rightChar() == right.leftChar()) {

            suffix += left.suffix();
        }

        // The longest repeating substring can be:
        //
        // 1. Completely inside the left child.
        // 2. Completely inside the right child.
        // 3. Crossing the boundary between the children.
        //
        // Initially consider the first two possibilities.
        int best = Math.max(left.best(), right.best());

        // If boundary characters match, the left suffix
        // and right prefix form one larger repeating substring.
        if (left.rightChar() == right.leftChar()) {

            int crossingLength =
                left.suffix() + right.prefix();

            best = Math.max(best, crossingLength);
        }

        // Return the summary of the combined interval.
        return new Node(
            length,
            prefix,
            suffix,
            best,
            leftChar,
            rightChar
        );
    }

    // ---------------------------------------------------------
    // POINT UPDATE
    // ---------------------------------------------------------

    private void update(
            int node,
            int start,
            int end,
            int index,
            char newChar) {

        // We have reached the leaf representing the target index.
        if (start == end) {

            // Update the actual string representation.
            chars[index] = newChar;

            // Rebuild the leaf summary.
            tree[node] = new Node(
                1,
                1,
                1,
                1,
                newChar,
                newChar
            );

            return;
        }

        int mid = start + (end - start) / 2;

        // Determine which child contains the target index.
        if (index <= mid) {

            update(
                node * 2,
                start,
                mid,
                index,
                newChar
            );

        } else {

            update(
                node * 2 + 1,
                mid + 1,
                end,
                index,
                newChar
            );
        }

        // Only ancestors of the updated leaf need recomputation.
        //
        // The unaffected child already contains a valid summary.
        // Re-merging the children updates all affected intervals.
        tree[node] = merge(
            tree[node * 2],
            tree[node * 2 + 1]
        );
    }
}


