/**
 * 0. ## Problem Statement
 * Given an array of n integers, process q queries of two types:
 * 1. update(k, u): Update the value at position k (1-based) to u.
 * 2. query(a, b): Find the minimum value in the range [a, b] (1-based, inclusive).
 * 
 * 
 * 1. ## Clarifying questions
 * Q: Is the input indexing 1-based or 0-based?
 * A: 1-based. (Prevents off-by-one errors; we must shift inputs by -1 internally).
 * 
 * Q: Can the values fit in a standard 32-bit signed integer?
 * A: Yes, max value is 10^9, which fits in Java's `int` (up to ~2.14 * 10^9). We can safely use `Integer.MAX_VALUE` as our "infinity" identity element.
 * 
 * Q: Will `a` always be less than or equal to `b`?
 * A: Yes. (Saves us from writing `Math.min(a, b)` bounds correction).
 * 
 * 
 * 2. ## The reasoning journey
 * Constraint Check: N and Q are up to 200,000. An O(N) query takes ~40 billion operations total, causing a Time Limit Exceeded (TLE). We need queries and updates to run in sub-linear time, ideally O(log N) or O(1).
 * 
 * Approach 1: Brute Force (What I'd naturally try first)
 * - Idea: Modify the array directly for updates. For queries, loop from index `a` to `b` and track the minimum.
 * - Why it works: Directly simulates the problem definition.
 * - Why it's too slow: Overlapping queries repeatedly scan the same elements.
 * - Time Complexity: O(1) for update — because it's a direct array assignment. O(N) for query — because we potentially scan the whole array.
 * - Space Complexity: O(1) extra space — because we only need the original array.
 * 
 * Approach 2: Square Root Decomposition (The intermediate balance)
 * - Idea: Divide the array of size N into √N blocks of size √N. Store the minimum of each block.
 * - Why it works: To query a range, we take the minimum of fully enclosed blocks in O(1) each, and iterate element-by-element only for the partial blocks at the boundaries.
 * - Why it's too slow (for some strict limits): Q * √N operations is ~90 million. It might barely pass, but it's not optimal.
 * - Time Complexity: O(1) for update — because we update 1 element and recalculate 1 block min in O(√N) or O(1) depending on implementation. O(√N) for query — because we touch at most √N blocks and √N individual elements.
 * - Space Complexity: O(√N) extra space — because we store an array of block minimums.
 * 
 * Approach 3: Segment Tree (The Optimal solution)
 * - Idea: Build a binary tree where each node represents the minimum of its children. Leaves are the array elements.
 * - Why it works: Any contiguous range can be perfectly covered by at most O(log N) pre-computed tree nodes. Updates only affect one path from leaf to root.
 * - Time Complexity: O(log N) for update — because we traverse exactly one path (height of tree) from leaf to root. O(log N) for query — because a range relies on at most 2 nodes per level of the tree.
 * - Space Complexity: O(N) extra space — because a perfectly sized iterative segment tree for N elements requires exactly an array of size 2N.
 * 
 * The Choice: Iterative Segment Tree (Approach 3). It hits the strict O(log N) time bound, and doing it iteratively avoids recursion overhead, requiring only a simple 2N array.
 * 
 * 
 * 3. ## Edge cases
 * - N = 1 (Single element array): `a == b == 1`. Tree must not go out of bounds during build.
 * - `a == b`: Point query. Should simply return the element at that index.
 * - Update doesn't change the minimum: `arr[k]` becomes larger, forcing the tree to pull the minimum from the sibling. Handled naturally by recalculating `Math.min(left, right)`.
 * - 1-based indexing from input: `k`, `a`, and `b` must be aggressively decremented before interacting with our 0-based internal logic.
 * 
 * 
 * 4. ## Also include
 * 
 * Key insight: 
 * For Range Minimum Queries (RMQ), what is the "identity" element? For Range Sum, adding 0 changes nothing. For RMQ, taking `Math.min(X, Integer.MAX_VALUE)` changes nothing. We must initialize our "out of bounds" or starting comparisons with `MAX_VALUE`.
 * 
 * Diagram (Iterative Segment tree array layout):
 * For nums = [3, 2, 4, 5], N = 4. Tree size = 8.
 * Elements go into the second half: indices [4, 5, 6, 7].
 * Parents are calculated as `tree[i] = min(tree[i*2], tree[i*2+1])`.
 * 
 * Index:     0     1       2       3       4     5     6     7
 * Role:    (DUD) (0-3)   (0-1)   (2-3)   (0-0) (1-1) (2-2) (3-3)
 * Value:     0     2       2       4       3     2     4     5
 * 
 * Dry run of `query(1, 4)` -> internal 0-indexed `query(0, 3)`:
 * L = 0 + N = 4. R = 3 + N = 7. Loop while(L <= R). min_val = MAX_VALUE.
 * Iteration 1:
 * - L = 4. Even (left child). Safe to defer to parent. L stays 4.
 * - R = 7. Odd (right child). Safe to defer to parent. R stays 7.
 * - L = 2, R = 3.
 * Iteration 2:
 * - L = 2. Even. Safe.
 * - R = 3. Odd. Safe.
 * - L = 1, R = 1.
 * Iteration 3:
 * - L = 1. Odd (right child of 0, effectively). `min_val = min(MAX, tree[1])` = 2. L becomes 2.
 * - R = 1. Odd. `R` becomes 0.
 * End Loop (L > R). Return 2. Correct!
 * 
 * Pitfalls:
 * - Forgetting that CSES/Competitive Programming inputs are 1-based. `update(1, 5)` means index 0 in a standard array.
 * - Using `0` as the initial minimum. It will falsely return 0 if all array elements are positive. Always use `Integer.MAX_VALUE`.
 * 
 * Pattern recognition:
 * - "Point update + Range associative query (Sum, Min, Max, XOR) = Segment Tree."
 * - Transfers to: finding the maximum contiguous subarray sum, counting elements smaller than X in a range.
 * 
 * Interview script:
 * "Since we are doing point updates and range minimum queries on up to 200,000 elements, we need logarithmic time bounds. I will build an iterative segment tree. I'll allocate an array of size 2N, place the raw elements in the second half, and build the minimums upwards. For a query, I'll walk inward from the left and right boundaries, picking up values when a pointer lands on an inner child. That gives us O(log N) time and O(N) space."
 * 
 * 
 * 5. ## Follow-ups
 * Q: What if we needed the *index* of the minimum value, not just the value itself?
 * A: We would store indices in the segment tree instead of values. When comparing two children, we compare `arr[tree[left]]` and `arr[tree[right]]`, and store the index of the smaller one.
 * 
 * Q: What if the updates were Range Updates, like "add u to all elements in range [a, b]"?
 * A: An iterative segment tree gets very complex for range updates. We would switch to a Recursive Segment Tree with "Lazy Propagation," deferring updates to children until they are strictly needed.
 * 
 * Q: What if the array was static (no updates), and we had millions of range minimum queries?
 * A: We could use a Sparse Table. It takes O(N log N) time to build, but answers Range Minimum Queries in O(1) time, which is strictly faster than a Segment Tree's O(log N) query.
 */

import java.util.Scanner;

public class DynamicRangeMinimumQueries {
    
    private int[] tree;
    private int n;

    /**
     * Initializes the tree with the given array.
     * Time Complexity: O(N) — because we insert N leaves and compute N-1 parents in a single pass.
     * Space Complexity: O(N) — because the tree array strictly requires 2N slots.
     */
    public DynamicRangeMinimumQueries(int[] nums) {
        n = nums.length;
        tree = new int[2 * n];
        
        // 1. Insert leaves into the second half of the array
        for (int i = 0; i < n; i++) {
            tree[n + i] = nums[i];
        }
        
        // 2. Build the tree by computing parents bottom-up
        // The parent of node i is i / 2.
        for (int i = n - 1; i > 0; i--) {
            tree[i] = Math.min(tree[2 * i], tree[2 * i + 1]);
        }
    }
    
    /**
     * Updates the value at the given 0-based index.
     * Time Complexity: O(log N) — because we traverse exactly one path from leaf to root.
     * Space Complexity: O(1) extra space — because the traversal is done iteratively in-place.
     */
    public void update(int index, int value) {
        // Shift logical index to tree leaf index
        index += n;
        tree[index] = value;
        
        // Bubble the updated minimum up to the root
        while (index > 1) {
            index /= 2;
            tree[index] = Math.min(tree[2 * index], tree[2 * index + 1]);
        }
    }
    
    /**
     * Returns the minimum value in the 0-based inclusive range [left, right].
     * Time Complexity: O(log N) — because boundaries move up the tree, touching at most 2 nodes per level.
     * Space Complexity: O(1) extra space — iterative calculation requires only a few primitive variables.
     */
    public int query(int left, int right) {
        // Shift boundaries to tree leaf indices
        left += n;
        right += n;
        int minResult = Integer.MAX_VALUE;
        
        while (left <= right) {
            // If left is a right child (odd), it does not include its left sibling.
            // We must consume its value directly and move the boundary rightward.
            if (left % 2 == 1) {
                minResult = Math.min(minResult, tree[left]);
                left++;
            }
            
            // If right is a left child (even), it does not include its right sibling.
            // We must consume its value directly and move the boundary leftward.
            if (right % 2 == 0) {
                minResult = Math.min(minResult, tree[right]);
                right--;
            }
            
            // Move up to the next level of the tree
            left /= 2;
            right /= 2;
        }
        
        return minResult;
    }

    /**
     * Main method to test against the provided example case.
     */
    public static void main(String[] args) {
        // Example input:
        // 8 4
        // 3 2 4 5 1 1 5 3
        // 2 1 4
        // 2 5 6
        // 1 2 3
        // 2 1 4
        
        int[] nums = {3, 2, 4, 5, 1, 1, 5, 3};
        DynamicRangeMinimumQueries rmq = new DynamicRangeMinimumQueries(nums);
        
        System.out.println("--- Running Example Queries ---");
        
        // Query 1: 2 1 4 -> Min in [1, 4] (1-based)
        // Convert to 0-based: [0, 3] -> expecting min(3, 2, 4, 5) = 2
        System.out.println(rmq.query(1 - 1, 4 - 1)); 
        
        // Query 2: 2 5 6 -> Min in [5, 6] (1-based)
        // Convert to 0-based: [4, 5] -> expecting min(1, 1) = 1
        System.out.println(rmq.query(5 - 1, 6 - 1));
        
        // Query 3: 1 2 3 -> Update index 2 to 3 (1-based)
        // Convert to 0-based: update index 1 to 3. Array becomes [3, 3, 4, 5, 1, 1, 5, 3]
        rmq.update(2 - 1, 3);
        
        // Query 4: 2 1 4 -> Min in [1, 4] (1-based)
        // Convert to 0-based: [0, 3] -> expecting min(3, 3, 4, 5) = 3
        System.out.println(rmq.query(1 - 1, 4 - 1));
    }
}

/**
 * 7. ## Short summary
 * - Core pattern: Segment Tree (Iterative array-based for Minimum).
 * - Key observation: Range minimum is associative. Unlike Range Sum, Range Min identity is `MAX_VALUE`.
 * - What's worth memorizing: `left % 2 == 1` consumes left pointer. `right % 2 == 0` consumes right pointer. Shift index by N.
 * - Most common trap: Forgetting to convert 1-based indices from competitive programming inputs into 0-based internal logic.
 * - One-line mental trigger: "Point update + Range Minimum = Segment Tree."
 */

