/**
 * 0. ## Problem Statement
 * Given an integer array nums, handle multiple queries of two types:
 * 1. update(index, val): Updates the value of nums[index] to be val.
 * 2. sumRange(left, right): Returns the sum of elements between indices left and right inclusive.
 * 
 * 1. ## Clarifying questions
 * Q: Are the updates relative (add `val` to current) or absolute (replace with `val`)?
 * A: Absolute. (Prevents bugs where we do `nums[i] += val` instead of overwriting).
 * 
 * Q: What is the expected ratio of updates to sum queries?
 * A: It matters because if updates are rare, a prefix-sum array is O(1) for sums. If mixed, we need a balanced approach.
 * 
 * Q: Will `left` and `right` always be within valid bounds of the array, and will `left <= right`?
 * A: Yes. (Saves us from writing boilerplate boundary-check code).
 * 
 * Q: Are we allowed to modify the input array, or do we need to treat it as immutable?
 * A: We can modify or ignore it, but we usually build an internal data structure anyway to speed up operations.
 * 
 * 
 * 2. ## The reasoning journey
 * Constraint Check: We can have up to 30,000 elements and 30,000 queries. If each query takes O(N), 
 * 30k * 30k = 900 million operations, which will likely Time Limit Exceeded (TLE) in Java (usually 10^8 ops/sec limit). 
 * The binding constraint is keeping *both* operations much faster than O(N).
 * 
 * Approach 1: Brute Force (What I'd naturally try first)
 * - Idea: Just keep the array. `update` replaces a value. `sumRange` loops from `left` to `right` adding them up.
 * - Why it works: It's the literal definition of the problem.
 * - Why it's too slow: `sumRange` repeats the exact same additions if we query overlapping ranges multiple times.
 * - Time Complexity: O(1) for update — because we just do `nums[index] = val`. O(N) for sumRange — because in the worst case (left=0, right=N-1), we iterate the whole array.
 * - Space Complexity: O(1) extra space — because we only store the input array in-place.
 * 
 * Approach 2: Prefix Sums (Trying to optimize the query)
 * - Idea: Precalculate a `prefix` array where `prefix[i]` is the sum of `nums[0...i-1]`.
 * - Why it works: `sumRange(L, R)` becomes `prefix[R+1] - prefix[L]`, caching the work of addition.
 * - Why it's too slow: When we `update(index, val)`, every single prefix sum after `index` becomes invalid.
 * - Time Complexity: O(N) for update — because we have to rebuild the prefix array from `index` to the end. O(1) for sumRange — because it's a simple subtraction.
 * - Space Complexity: O(N) extra space — because we allocate the `prefix` array.
 * 
 * Approach 3: Square Root Decomposition (The intermediate balance)
 * - Idea: What if we don't cache everything (Prefix sum) and don't cache nothing (Brute force)? We group the array into blocks of size √N. We cache the sum of each block.
 * - Why it works: `update` just changes the element and updates its specific block's sum. `sumRange` adds full blocks together, and only iterates element-by-element for the ragged edges.
 * - Time Complexity: O(1) for update — because we update 1 element and 1 block sum. O(√N) for sumRange — because at most we traverse √N blocks and √N individual elements.
 * - Space Complexity: O(√N) extra space — because we store an array of block sums.
 * 
 * Approach 4: Segment Tree (The Optimal solution)
 * - Idea: Instead of 1 flat level of blocks (√N), let's create a *hierarchy* of blocks. Pairs of elements form a block of 2. Pairs of 2s form blocks of 4, up to a root representing the whole array. This forms a binary tree.
 * - Why it works: To update an element, we only update its leaf and its direct ancestors up to the root (height of tree is log N). To query a range, we combine the pre-computed sums of the largest possible sub-trees that fit exactly in our range.
 * - Time Complexity: O(log N) for update — because we traverse exactly one path from leaf to root. O(log N) for sumRange — because a range can be constructed from at most 2 nodes per level of the tree.
 * - Space Complexity: O(N) extra space — because a binary tree with N leaves has exactly 2N-1 nodes in total.
 * 
 * The Choice: I would write the **Iterative Segment Tree** (Approach 4). It achieves optimal O(log N) time for both operations. While a recursive Segment tree is standard, the iterative array-based version is remarkably short, avoids recursion overhead (`StackOverflowError` risk), and is practically bug-free once you grasp the index math.
 * 
 * 
 * 3. ## Edge cases
 * - N = 1 (Single element array): The tree is tiny, `left == right == 0`. Must ensure tree building doesn't go out of bounds.
 * - `left == right`: Querying a single element. Should just return that element.
 * - High frequency updates on the exact same index: Must efficiently overwrite without breaking tree invariants.
 * - Updates that don't actually change the value (`nums[index] == val`): We can optimize by returning early.
 * 
 * 
 * 4. ## Also include
 * 
 * Key insight: 
 * How do we cache sums so that 1 update doesn't ruin everything?
 * By ensuring any given element belongs to exactly O(log N) cached groups, rather than O(N) cached groups like in Prefix Sums. A binary tree achieves exactly this.
 * 
 * Diagram (Iterative Segment tree array layout):
 * For nums = [1, 3, 5], N = 3.
 * We allocate an array `tree` of size 2 * N = 6.
 * Elements go into the second half: indices [3, 4, 5].
 * Parents are calculated as `tree[i] = tree[i*2] + tree[i*2+1]`.
 * 
 * Index:     0     1     2     3     4     5
 * Role:    (DUD) (0-2) (0-1)(0-0) (1-1)(2-2)
 * Value:     0     9     4     1     3     5
 * 
 * Notice: 
 * - Leaf values are at indices N to 2N-1.
 * - The parent of node `i` is `i / 2`.
 * - The sibling of node `i` is `i ^ 1` (i XOR 1).
 * 
 * Dry run of `sumRange(0, 2)`:
 * L = 0 + N = 3. R = 2 + N = 5. Loop while(L <= R).
 * Iteration 1:
 * - L = 3. Is it a right child? (L % 2 == 1). Yes. It has no left sibling in our range.
 *   Add `tree[3]` (1) to sum. Move L right: `L++` -> 4.
 * - R = 5. Is it a left child? (R % 2 == 0). No. (R % 2 == 1, it's a right child).
 *   Its left sibling is inside our range, so we don't process R alone yet, we defer to the parent.
 * - Move to parents: L = 4 / 2 = 2. R = 5 / 2 = 2.
 * Iteration 2:
 * - L = 2, R = 2.
 * - L = 2. Right child? No. 
 * - R = 2. Left child? Yes. It has no right sibling in our range.
 *   Add `tree[2]` (4) to sum. Move R left: `R--` -> 1.
 * - Move to parents: L = 2 / 2 = 1. R = 1 / 2 = 0.
 * End Loop (L > R). Sum = 1 + 4 = 5. Wait, 1+3+5 = 9!
 * Ah, let's look at the actual tree trace in the dry run:
 * Tree: [0, 9, 4, 1, 3, 5] (Wait, 1+3=4, 4+5=9. Parent of 4 and 5? No!)
 * Let's properly trace the build:
 * i=2: tree[2] = tree[4]+tree[5] = 3+5=8. 
 * i=1: tree[1] = tree[2]+tree[3] = 8+1=9.
 * Correct Tree: [0, 9, 8, 1, 3, 5]. 
 * Dry run with correct tree:
 * L = 3, R = 5.
 * Iter 1: L=3 (odd) -> sum += tree[3] (1). L goes to 4.
 * R=5 (odd) -> R stays 5.
 * L=2, R=2.
 * Iter 2: L=2 (even) -> L stays 2.
 * R=2 (even) -> sum += tree[2] (8). R goes to 1.
 * L=1, R=0. Loop breaks.
 * Sum = 1 + 8 = 9. Correct!
 * 
 * Pitfalls:
 * - Trying to allocate a 2*N array for a recursive segment tree. Recursive segment trees need 4*N space because they are balanced perfectly from the top down. The iterative tree cleverly maps elements directly to bottom indices and works upwards, needing exactly 2*N space.
 * - Off-by-one errors with `R`. In the iterative array, `R` is inclusive, so we must add N to it directly.
 * 
 * Pattern recognition:
 * - "When you see point updates and range queries on an array, think Segment Tree or Fenwick Tree."
 * - Transfers to: Range Minimum Query, counting inversions, booking schedules (lazy propagation).
 * 
 * Interview script:
 * "Since we need both updates and range sums to be fast, a static prefix sum array won't work because updates take O(N). If we use a Segment Tree, we can reduce both operations to O(log N). I'll implement an iterative, array-based Segment Tree because it's extremely space-efficient — exactly 2N memory — and avoids recursion overhead. To query a range, we check if our left pointer is a right child; if so, we add it directly and move inward. We do the mirror check for the right pointer. That gives us O(log N) time and O(N) space."
 * 
 * 
 * 5. ## Follow-ups
 * Q: What if the problem asked for Range Minimum instead of Range Sum?
 * A: The Segment Tree structure remains exactly the same. We just replace `+` with `Math.min()`. (Note: A Fenwick Tree / BIT cannot easily do Range Minimum, which makes Segment Tree a much stronger pattern to master).
 * 
 * Q: What if we needed to add a value to a *range* of elements instead of a single element? (e.g., `updateRange(left, right, val)`)
 * A: We would need a recursive Segment Tree with "Lazy Propagation." We'd defer updating child nodes until they are actually queried, storing pending updates in a secondary `lazy` array.
 * 
 * Q: What if the array is incredibly large (e.g. 10^9 elements) but sparse?
 * A: We can't allocate an array of size 2*10^9. We would use a "Dynamic Segment Tree" where nodes are created on-demand as objects with left/right pointers, or use a HashMap.
 */

public class RangeSumQueryMutable {
    
    // tree array to hold our segment tree. 
    // Example: For N=3, size is 6. 
    // tree[3], tree[4], tree[5] hold the actual array values.
    // tree[1], tree[2] hold the sums of their children.
    private int[] tree;
    private int n;

    /**
     * Initializes the object with the integer array nums.
     * Time Complexity: O(N) — because we visit each element to copy it, then calculate parents in one O(N) pass.
     * Space Complexity: O(N) — because we allocate an array of size 2N to hold the tree.
     */
    public RangeSumQueryMutable(int[] nums) {
        if (nums.length > 0) {
            n = nums.length;
            tree = new int[n * 2]; // Needs exactly 2N space
            
            // 1. Insert leaf nodes in the second half of the tree.
            // For nums=[1, 3, 5], n=3.
            // tree[3]=1, tree[4]=3, tree[5]=5
            for (int i = n, j = 0; i < 2 * n; i++, j++) {
                tree[i] = nums[j];
            }
            
            // 2. Build the tree by calculating parents from the leaves up to the root.
            // We loop backwards from n-1 down to 1.
            // For n=3:
            // i=2: tree[2] = tree[4] + tree[5]
            // i=1: tree[1] = tree[2] + tree[3]
            for (int i = n - 1; i > 0; --i) {
                tree[i] = tree[i * 2] + tree[i * 2 + 1];
            }
        }
    }
    
    /**
     * Updates the value of nums[index] to be val.
     * Time Complexity: O(log N) — because we start at a leaf and divide the index by 2 at most log2(N) times to reach the root.
     * Space Complexity: O(1) extra space — because we update the tree in-place without a recursion stack.
     */
    public void update(int index, int val) {
        // Shift index to the leaf's position in our tree array
        index += n; 
        
        // Update the leaf directly
        tree[index] = val;
        
        // Bubble up the change to the root.
        // We traverse up by dividing the index by 2.
        while (index > 1) {
            // Find the parent's index
            int parent = index / 2;
            
            // Recompute the parent as the sum of its two children.
            // The left child is always at an even index (parent * 2).
            // The right child is always at an odd index (parent * 2 + 1).
            int leftChild = parent * 2;
            int rightChild = parent * 2 + 1;
            
            tree[parent] = tree[leftChild] + tree[rightChild];
            
            // Move up to the parent for the next iteration
            index = parent;
        }
    }
    
    /**
     * Returns the sum of the elements of nums between indices left and right inclusive.
     * Time Complexity: O(log N) — because in each step we divide our range boundaries by 2, processing at most 2 nodes per level of the tree.
     * Space Complexity: O(1) extra space — because we calculate the sum iteratively using variables, no recursion.
     */
    public int sumRange(int left, int right) {
        // Shift logical indices to the actual leaf indices in the tree
        left += n;
        right += n;
        
        int sum = 0;
        
        // Shrink the range from the edges inward, climbing the tree.
        while (left <= right) {
            
            // If 'left' is an odd number, it is the RIGHT child of its parent.
            // This means its parent includes a left sibling that is OUTSIDE our requested range.
            // Therefore, we must add 'left's value directly to our sum, 
            // and move our left boundary inward (rightward) by doing left++.
            if ((left % 2) == 1) {
                sum += tree[left];
                left++;
            }
            
            // If 'right' is an even number, it is the LEFT child of its parent.
            // This means its parent includes a right sibling that is OUTSIDE our requested range.
            // Therefore, we must add 'right's value directly to our sum,
            // and move our right boundary inward (leftward) by doing right--.
            if ((right % 2) == 0) {
                sum += tree[right];
                right--;
            }
            
            // Move both pointers up one level in the tree
            left /= 2;
            right /= 2;
        }
        
        return sum;
    }

    /**
     * Cross-check execution on typical and edge cases.
     */
    public static void main(String[] args) {
        // Typical Case: From the problem description
        System.out.println("--- Typical Case ---");
        int[] nums1 = {1, 3, 5};
        RangeSumQueryMutable st = new RangeSumQueryMutable(nums1);
        System.out.println("Sum [0, 2] (Expect 9): " + st.sumRange(0, 2));
        st.update(1, 2); // Array becomes [1, 2, 5]
        System.out.println("After updating index 1 to 2:");
        System.out.println("Sum [0, 2] (Expect 8): " + st.sumRange(0, 2));

        // Edge Case 1: Single element array
        System.out.println("\n--- Edge Case: Single Element ---");
        int[] nums2 = {10};
        RangeSumQueryMutable st2 = new RangeSumQueryMutable(nums2);
        System.out.println("Sum [0, 0] (Expect 10): " + st2.sumRange(0, 0));
        st2.update(0, 42);
        System.out.println("Sum [0, 0] after update (Expect 42): " + st2.sumRange(0, 0));

        // Edge Case 2: Array size not a power of 2 (e.g. 5 elements)
        System.out.println("\n--- Edge Case: Size not power of 2 ---");
        int[] nums3 = {2, 4, 6, 8, 10};
        RangeSumQueryMutable st3 = new RangeSumQueryMutable(nums3);
        System.out.println("Sum [1, 3] (Expect 4+6+8=18): " + st3.sumRange(1, 3));
        st3.update(2, 0); // Array becomes [2, 4, 0, 8, 10]
        System.out.println("Sum [1, 3] after update (Expect 4+0+8=12): " + st3.sumRange(1, 3));
    }
}

/**
 * 7. ## Short summary
 * - Core pattern: Segment Tree (Iterative array-based).
 * - Key observation: Point updates break static prefix sums. Grouping elements hierarchically in a binary tree balances update and query times to O(log N).
 * - What's worth memorizing: The iterative tree size is exactly 2N. Leaves start at N. Left child is `i*2`, right child is `i*2+1`. Parent is `i/2`.
 * - Most common trap: Trying to use Prefix Sums when updates are frequent, resulting in Time Limit Exceeded.
 * - One-line mental trigger: "Mutable array + range queries = Segment Tree."
 */








































class NumArray {

    private final int n;
    private final int[] tree;

    public NumArray(int[] nums) {

        this.n = nums.length;

        // 4*N is a convenient upper bound for a recursive segment tree.
        this.tree = new int[4 * n];

        build(nums, 0, 0, n - 1);
    }

    private void build(
            int[] nums,
            int node,
            int start,
            int end
    ) {

        // A leaf represents exactly one array element.
        if (start == end) {
            tree[node] = nums[start];
            return;
        }

        int mid = start + (end - start) / 2;

        // Build the left half.
        build(nums, 2 * node + 1, start, mid);

        // Build the right half.
        build(nums, 2 * node + 2, mid + 1, end);

        // Parent stores the sum of both children.
        tree[node] =
                tree[2 * node + 1] +
                tree[2 * node + 2];
    }

    public void update(int index, int val) {

        update(0, 0, n - 1, index, val);
    }

    private void update(
            int node,
            int start,
            int end,
            int index,
            int val
    ) {

        if (start == end) {
            tree[node] = val;
            return;
        }

        int mid = start + (end - start) / 2;

        // Follow the child whose interval contains the target index.
        if (index <= mid) {
            update(2 * node + 1, start, mid, index, val);
        } else {
            update(2 * node + 2, mid + 1, end, index, val);
        }

        // Recalculate the current node after updating its child.
        tree[node] =
                tree[2 * node + 1] +
                tree[2 * node + 2];
    }

    public int sumRange(int left, int right) {

        return query(0, 0, n - 1, left, right);
    }

    private int query(
            int node,
            int start,
            int end,
            int left,
            int right
    ) {

        // Case 1: No overlap.
        // This node contributes nothing to the requested range.
        if (right < start || end < left) {
            return 0;
        }

        // Case 2: Complete overlap.
        // The node's stored sum is exactly what we need.
        if (left <= start && end <= right) {
            return tree[node];
        }

        // Case 3: Partial overlap.
        // Explore both children and combine their answers.
        int mid = start + (end - start) / 2;

        int leftSum = query(
                2 * node + 1,
                start,
                mid,
                left,
                right
        );

        int rightSum = query(
                2 * node + 2,
                mid + 1,
                end,
                left,
                right
        );

        return leftSum + rightSum;
    }
}

/*
 * COMPLEXITY:
 *
 * Constructor: O(N)
 * update():    O(log N)
 * sumRange():  O(log N)
 * Space:       O(N)
 *
 * WHY USE A SEGMENT TREE?
 *
 * Segment Trees are more flexible than Fenwick Trees.
 *
 * They can be extended to support:
 * - Range minimum / maximum queries.
 * - Range updates with lazy propagation.
 * - GCD queries.
 * - Custom associative operations.
 *
 * For simple range sums with point updates, a Fenwick Tree
 * is usually shorter and easier to implement.
 */
