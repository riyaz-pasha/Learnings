/**
 * STANDALONE STUDY NOTE: FRUITS INTO BASKETS II
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Can each basket hold more than one type of fruit?" (No, rule 2 states each basket can hold only one type of fruit once assigned).
 * - "Must we always pick the *leftmost* available basket that satisfies the capacity requirement?" (Yes, rule 1 specifies the leftmost available basket with capacity >= fruit quantity).
 * - "What should be returned if a fruit type cannot be placed anywhere?" (It remains unplaced, and we return the total count of unplaced fruit types at the end).
 * - "What are the constraints on n?" (n ranges from 1 to 100, with fruit quantities and basket capacities up to 1000, making an O(N^2) simulation approach extremely fast and well within time limits).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to simulate placing each fruit type from left to right into the leftmost available basket that has sufficient capacity. 
 * The binding constraint is keeping track of used baskets so they aren't reused, while adhering strictly to the greedy 
 * left-to-right search order.
 * 
 * --- APPROACH 1: BRUTE FORCE / GREEDY SIMULATION (Boolean Visited Tracker) ---
 * 1. What I'd naturally try: Maintain a boolean array `vis` of size `n` to track which baskets have already been claimed. 
 *    Assume initially that all `n` fruits are unplaced (`unplacedCount = n`). For each fruit, scan the baskets from left to right. 
 *    The moment we find an available basket (`!vis[i]`) with sufficient capacity (`baskets[i] >= fruit`), we claim it (`vis[i] = true`), 
 *    decrement `unplacedCount`, and break out of the basket loop to move to the next fruit.
 * 2. Why it works: It perfectly mirrors the problem's greedy rules and naturally enforces the "leftmost available basket" constraint.
 * 3. Why it's efficient enough: With `n <= 100`, an O(N^2) nested loop performs at most 10,000 operations, which executes in a fraction 
 *    of a millisecond.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N^2) — for each of the N fruits, we scan up to N baskets.
 *    - Space Complexity: O(N) — for the boolean visited array tracking basket usage.
 * 
 * --- APPROACH 2: OPTIMAL (Segment Tree / Advanced Range Query - for large N) ---
 * 1. The Observation: If N were up to 10^5 (as in follow-up versions like Fruits Into Baskets III), an O(N^2) scan would time out, 
 *    requiring a Segment Tree or Fenwick Tree with binary search to find the leftmost valid basket in O(N log N) time. 
 *    However, given the constraints of this specific problem ($N \le 100$), the O(N^2) simulation is optimal in terms of code simplicity 
 *    and execution speed.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - All fruits successfully placed: Returns 0 unplaced fruits.
 * - No baskets have sufficient capacity: Returns `n` (all fruits remain unplaced).
 * - Single element arrays: `fruits = [5], baskets = [3]` -> Returns 1.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Greedy Left-to-Right Allocation:
 * 
 * Fruits:  [4, 2, 5]
 * Baskets: [3, 5, 4] (vis: [F, F, F])
 * 
 * - Fruit 0 (Qty = 4):
 *   - Scan baskets: 
 *     - Basket 0 (cap 3): 3 >= 4? No.
 *     - Basket 1 (cap 5): 5 >= 4? YES! Place fruit 0 here. Mark vis[1] = true.
 * - Fruit 1 (Qty = 2):
 *   - Scan baskets:
 *     - Basket 0 (cap 3): 3 >= 2 and not used? YES! Place fruit 1 here. Mark vis[0] = true.
 * - Fruit 2 (Qty = 5):
 *   - Scan baskets:
 *     - Basket 0: used.
 *     - Basket 1: used.
 *     - Basket 2 (cap 4): 4 >= 5? No.
 *   - No basket found! Fruit 2 remains unplaced.
 * 
 * Total unplaced = 1.
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Simulation Approach)
 * Target: fruits = [3, 6, 1], baskets = [6, 4, 7]
 * 
 * - Init: n = 3, vis = [false, false, false], unplaced = 3
 * - Fruit 0 (Qty = 3):
 *   - i = 0 (cap 6): 6 >= 3 and !vis[0] -> match! vis[0] = true, unplaced = 2. Break.
 * - Fruit 1 (Qty = 6):
 *   - i = 0 (cap 6): vis[0] is true (skip).
 *   - i = 1 (cap 4): 4 >= 6? No.
 *   - i = 2 (cap 7): 7 >= 6 and !vis[2] -> match! vis[2] = true, unplaced = 1. Break.
 * - Fruit 2 (Qty = 1):
 *   - i = 0 (cap 6): used.
 *   - i = 1 (cap 4): 4 >= 1 and !vis[1] -> match! vis[1] = true, unplaced = 0. Break.
 * 
 * Result: 0 unplaced fruits.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "To solve this, we can simulate the allocation process using a greedy approach. Since each fruit type must go 
 * into the leftmost available basket that has sufficient capacity, and each basket can only hold one type of fruit, 
 * we can maintain a boolean array to track which baskets have been used. We start by assuming all fruits are unplaced. 
 * Then, for each fruit, we scan the baskets from left to right. The first unused basket whose capacity meets or exceeds 
 * the fruit quantity is assigned to that fruit, marked as used, and we move to the next fruit. Given that n is at most 100, 
 * this straightforward simulation runs in O(N^2) time and O(N) space, which is extremely efficient and clean."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if n was up to 10^5 and capacities were large?
 * A: An O(N^2) nested loop would TLE. We would use a Segment Tree that supports range maximum queries to find 
 *    and update the leftmost valid basket in O(N log N) time.
 */

public class FruitsIntoBasketsII {

    /**
     * APPROACH 1: OPTIMAL SIMULATION (Greedy Left-to-Right Scan with Visited Tracker)
     * Time: O(N^2), Space: O(N)
     */
    public int numOfUnplacedFruits(int[] fruits, int[] baskets) {
        int n = fruits.length;
        
        // [Why needed]: Tracks whether each basket has already been claimed by a fruit type.
        boolean[] usedBaskets = new boolean[n];
        
        // [What it does]: Start by assuming all fruits are unplaced, then decrement as we successfully place them.
        int unplacedCount = n;

        // Process each fruit type from left to right
        for (int fruitQty : fruits) {
            boolean placed = false;

            // Scan baskets from left to right to find the leftmost available valid basket
            for (int j = 0; j < n; j++) {
                // [Why needed]: Check two conditions:
                // 1. Basket capacity must be >= fruit quantity.
                // 2. Basket must not have been used already.
                if (!usedBaskets[j] && baskets[j] >= fruitQty) {
                    usedBaskets[j] = true; // Claim this basket
                    unplacedCount--;       // Successfully placed one more fruit type
                    placed = true;
                    break;                 // Stop searching once the leftmost valid basket is found
                }
            }
        }

        return unplacedCount;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Greedy simulation with a visited status array (`boolean[] usedBaskets`).
     * 2. Key observation: Scanning from left to right ensures we always satisfy the "leftmost available basket" constraint.
     * 3. Memorize vs Understand: Understand why starting with `unplacedCount = n` and decrementing on success is cleaner 
     *    than counting failures.
     * 4. Most common trap: Forgetting to `break` out of the basket loop after a successful placement, which would cause 
     *    a single fruit to claim multiple baskets.
     * 5. Mental trigger: "Leftmost available basket allocation" -> Nested loops + `boolean[] usedBaskets` + `break`.
     */
    public static void main(String[] args) {
        FruitsIntoBasketsII solution = new FruitsIntoBasketsII();

        // Test Case 1: Standard Example 1
        int[] fruits1 = {4, 2, 5};
        int[] baskets1 = {3, 5, 4};
        System.out.println("Test 1 (Standard): " + solution.numOfUnplacedFruits(fruits1, baskets1)); // Expected: 1

        // Test Case 2: Standard Example 2
        int[] fruits2 = {3, 6, 1};
        int[] baskets2 = {6, 4, 7};
        System.out.println("Test 2 (All placed): " + solution.numOfUnplacedFruits(fruits2, baskets2)); // Expected: 0
    }
}



/*
 * ================================================================
 * PROBLEM: Fruit Into Baskets
 * ================================================================
 *
 * STATEMENT:
 *
 * You are given two integer arrays, fruits and baskets, each of
 * length n.
 *
 * fruits[i] represents the quantity of the ith type of fruit.
 * baskets[j] represents the capacity of the jth basket.
 *
 * Place fruit types into baskets from left to right using these rules:
 *
 * 1. Each fruit must be placed in the LEFTMOST AVAILABLE basket
 *    whose capacity is greater than or equal to its quantity.
 *
 * 2. Each basket can hold only one type of fruit.
 *
 * 3. If a fruit cannot be placed in any available basket, it remains
 *    unplaced.
 *
 * Return the number of fruit types that remain unplaced.
 *
 * CONSTRAINTS:
 *
 * n == fruits.length == baskets.length
 * 1 <= n <= 100
 * 1 <= fruits[i], baskets[i] <= 1000
 *
 *
 * ================================================================
 * EXAMPLE
 * ================================================================
 *
 * Input:
 * fruits  = [4, 2, 5]
 * baskets = [3, 5, 4]
 *
 * Output:
 * 1
 *
 * DRY RUN:
 *
 * Initially:
 * baskets = [3, 5, 4]
 * used    = [false, false, false]
 * unplaced = 0
 *
 * FRUIT 1: quantity = 4
 *
 * Basket 0: capacity = 3
 *   3 < 4, so this basket cannot hold the fruit.
 *
 * Basket 1: capacity = 5
 *   5 >= 4, and this basket is available.
 *   Place the fruit in basket 1 and mark it as used.
 *
 * used = [false, true, false]
 *
 * FRUIT 2: quantity = 2
 *
 * Basket 0: capacity = 3
 *   3 >= 2, and this basket is available.
 *   Place the fruit in basket 0 and mark it as used.
 *
 * used = [true, true, false]
 *
 * FRUIT 3: quantity = 5
 *
 * Basket 0: already used, skip it.
 * Basket 1: already used, skip it.
 * Basket 2: capacity = 4, which is less than 5.
 *
 * No suitable basket exists, so increment unplaced.
 *
 * Final answer = 1
 *
 *
 * ================================================================
 * APPROACH 1: GREEDY + TWO LOOPS
 * ================================================================
 *
 * INTUITION:
 *
 * Process fruits in their original left-to-right order.
 *
 * For each fruit:
 *   1. Start checking baskets from index 0.
 *   2. Skip baskets that are already occupied.
 *   3. Select the first available basket whose capacity is >=
 *      the current fruit quantity.
 *   4. Mark that basket as used.
 *   5. Stop searching because the leftmost eligible basket has
 *      already been found.
 *   6. If no basket is found, increment the unplaced count.
 *
 * WHY GREEDY?
 *
 * The problem explicitly requires the leftmost eligible basket.
 * We do not need to compare all possible allocations or find a
 * globally optimal assignment.
 *
 * Scanning from left to right and choosing the first valid basket
 * satisfies the rule directly.
 *
 * WHY DO WE NEED USED[]?
 *
 * Each basket can hold only one fruit type.
 * Therefore, once a basket is selected, it must not be selected again.
 *
 * WHY DO WE NEED BREAK?
 *
 * The first eligible basket is the required answer for the current
 * fruit. Continuing the search could incorrectly select a later basket.
 *
 *
 * ================================================================
 * JAVA IMPLEMENTATION
 * ================================================================
 */

class Solution {

    public int numOfUnplacedFruits(int[] fruits, int[] baskets) {

        // Both arrays have the same length.
        int n = fruits.length;

        // used[j] tells us whether basket j has already been occupied.
        // Initially, all baskets are available because Java initializes
        // every boolean array element to false.
        boolean[] used = new boolean[n];

        // Number of fruit types that could not be placed.
        int unplaced = 0;

        // Process every fruit in its original left-to-right order.
        for (int i = 0; i < n; i++) {

            // Assume this fruit has not been placed yet.
            // We change this to true only after finding a suitable basket.
            boolean placed = false;

            // Search baskets from left to right.
            // Increasing j guarantees that the first eligible basket
            // is the leftmost eligible basket.
            for (int j = 0; j < n; j++) {

                // An occupied basket cannot accept another fruit type.
                if (used[j]) {
                    continue;
                }

                // Check whether the available basket can hold this fruit.
                // Equality is allowed: capacity == quantity is sufficient.
                if (baskets[j] >= fruits[i]) {

                    // Reserve the basket so later fruits cannot use it.
                    used[j] = true;

                    // Record that the current fruit was successfully placed.
                    placed = true;

                    // Stop immediately: we must use the leftmost
                    // available eligible basket, not a later one.
                    break;
                }
            }

            // If the inner loop finishes without finding a basket,
            // this fruit type remains unplaced.
            if (!placed) {
                unplaced++;
            }
        }

        // Return the total number of fruit types that remain unplaced.
        return unplaced;
    }
}


/*
 * ================================================================
 * CORRECTNESS REASONING
 * ================================================================
 *
 * For every fruit, the inner loop examines baskets in increasing
 * index order.
 *
 * Occupied baskets are skipped.
 *
 * Therefore, the first basket satisfying
 *
 *     baskets[j] >= fruits[i]
 *
 * is necessarily the leftmost available eligible basket.
 *
 * Once selected, the basket is marked as occupied, preventing reuse.
 *
 * If no eligible basket is found, placed remains false and the
 * unplaced count is incremented.
 *
 * Since every fruit is processed exactly once, the final count
 * is exactly the number of unplaced fruit types.
 *
 *
 * ================================================================
 * TIME AND SPACE COMPLEXITY
 * ================================================================
 *
 * TIME COMPLEXITY: O(n^2)
 *
 * There are n fruits. In the worst case, each fruit checks all n
 * baskets, resulting in at most n * n basket checks.
 *
 * SPACE COMPLEXITY: O(n)
 *
 * The used[] array requires n boolean entries.
 * Apart from this array, the algorithm uses only a constant number
 * of variables.
 *
 * With n <= 100, at most 10,000 basket checks are required.
 * This is easily efficient enough for the given constraints.
 *
 *
 * ================================================================
 * EDGE CASES
 * ================================================================
 *
 * 1. All fruits fit:
 *    fruits  = [1, 2]
 *    baskets = [2, 3]
 *    Answer  = 0
 *
 * 2. No fruit fits:
 *    fruits  = [5, 6]
 *    baskets = [2, 3]
 *    Answer  = 2
 *
 * 3. Exact capacity match:
 *    fruits  = [5]
 *    baskets = [5]
 *    Answer  = 0
 *
 * 4. Multiple eligible baskets:
 *    fruits  = [2]
 *    baskets = [5, 3]
 *    Answer  = 0
 *    Basket 0 must be selected because it is the leftmost eligible one.
 *
 * 5. Competing fruits:
 *    fruits  = [4, 3]
 *    baskets = [4, 3]
 *    Answer  = 0
 *    The first fruit gets basket 0; the second gets basket 1.
 *
 * 6. All baskets occupied before all fruits are processed:
 *    Any remaining fruit cannot be placed and must be counted.
 *
 *
 * ================================================================
 * INTERVIEW FOLLOW-UP QUESTIONS
 * ================================================================
 *
 * Q1. Can we improve the time complexity?
 *
 * Answer:
 * Yes. A segment tree can store the maximum available capacity in
 * each range. For each fruit, search for the leftmost basket whose
 * capacity is sufficient, then mark that basket unavailable.
 *
 * Time: O(n log n)
 * Space: O(n)
 *
 * However, this is unnecessary when n <= 100.
 *
 * Q2. Why can't we sort baskets by capacity?
 *
 * Answer:
 * Sorting changes the original positions. The problem requires
 * the leftmost eligible basket, so original basket order matters.
 *
 * Q3. Why can't we simply count baskets with insufficient capacity?
 *
 * Answer:
 * Basket availability changes after each placement. A basket that
 * can hold one fruit may be occupied before a later fruit is processed.
 * We must simulate the allocations in order.
 *
 * Q4. Why is capacity >= quantity, not capacity > quantity?
 *
 * Answer:
 * A basket can hold a fruit when its capacity exactly equals the
 * fruit quantity. The comparison must therefore use >=.
 *
 * Q5. Is the greedy approach correct?
 *
 * Answer:
 * Yes. The problem mandates choosing the leftmost available eligible
 * basket for every fruit. The algorithm follows that exact rule.
 * There is no need to optimize an alternative assignment.
 *
 *
 * ================================================================
 * INTERVIEW APPROACH: HOW TO THINK ABOUT THE SOLUTION
 * ================================================================
 *
 * Step 1: Identify the order requirement.
 *         Fruits must be processed from left to right.
 *
 * Step 2: Identify the selection requirement.
 *         Select the leftmost available basket with enough capacity.
 *
 * Step 3: Identify the state that changes.
 *         A basket becomes unavailable after being used.
 *
 * Step 4: Choose a simple data structure.
 *         A boolean[] tracks basket availability.
 *
 * Step 5: Translate the rules into loops.
 *         Outer loop processes fruits.
 *         Inner loop searches baskets.
 *
 * Step 6: Track failures.
 *         If a fruit cannot be placed, increment unplaced.
 *
 * Final recommendation:
 * Use the two-loop greedy solution for this problem's constraints.
 * It is simple, easy to explain, and follows the problem statement
 * directly.
 *
 * ================================================================
 */




























/*
 * ================================================================
 * PROBLEM: Fruit Into Baskets — Segment Tree Solution
 * ================================================================
 *
 * STATEMENT:
 *
 * You are given two integer arrays, fruits and baskets, each of
 * length n.
 *
 * fruits[i] represents the quantity of the ith type of fruit.
 * baskets[j] represents the capacity of the jth basket.
 *
 * Process fruits from left to right. For every fruit:
 *
 * 1. Find the LEFTMOST AVAILABLE basket whose capacity is greater
 *    than or equal to the fruit quantity.
 *
 * 2. Each basket can hold only one type of fruit.
 *
 * 3. If no eligible basket exists, the fruit remains unplaced.
 *
 * Return the number of fruit types that remain unplaced.
 *
 * CONSTRAINTS:
 *
 * n == fruits.length == baskets.length
 * 1 <= n <= 100
 * 1 <= fruits[i], baskets[i] <= 1000
 *
 *
 * ================================================================
 * APPROACH: SEGMENT TREE
 * ================================================================
 *
 * INTUITION:
 *
 * In the brute-force solution, for every fruit, we scan baskets
 * from left to right until we find a suitable available basket.
 *
 * In the worst case, this takes O(n^2) time.
 *
 * Can we avoid checking every basket?
 *
 * Yes. Instead of examining baskets individually, organize them
 * in a Segment Tree.
 *
 * Each tree node represents a range of basket indices and stores
 * the MAXIMUM AVAILABLE CAPACITY in that range.
 *
 * For example:
 *
 * baskets = [3, 5, 4, 7]
 *
 *                         [0..3] max = 7
 *                        /             \
 *                [0..1] max = 5     [2..3] max = 7
 *                 /       \           /       \
 *             [0] = 3   [1] = 5    [2] = 4   [3] = 7
 *
 * Suppose the current fruit quantity is 6.
 *
 * The left subtree [0..1] has maximum capacity 5.
 * Since 5 < 6, neither basket 0 nor basket 1 can hold the fruit.
 *
 * We can skip the entire left subtree!
 *
 * We then search the right subtree [2..3], whose maximum is 7.
 *
 * Basket 2 has capacity 4, so it cannot hold the fruit.
 * Basket 3 has capacity 7, so we select basket 3.
 *
 * After using basket 3, update its capacity in the tree to 0.
 *
 * WHY STORE THE MAXIMUM?
 *
 * If a segment's maximum available capacity is less than the
 * current fruit quantity, every basket in that segment is too small.
 *
 * Therefore, we can safely skip that entire segment.
 *
 * WHY SEARCH LEFT BEFORE RIGHT?
 *
 * The problem requires the LEFTMOST eligible basket.
 *
 * A segment tree's left child represents earlier basket indices
 * than its right child.
 *
 * Therefore:
 *   1. Search the left child first if it might contain a valid basket.
 *   2. Otherwise, search the right child.
 *
 * This guarantees that we find the leftmost eligible basket.
 *
 * WHY UPDATE A USED BASKET TO ZERO?
 *
 * All basket capacities are positive (>= 1).
 *
 * Setting a used basket's capacity to zero makes it impossible
 * for that basket to satisfy any valid fruit quantity.
 *
 * We then update the maximum values along the path back to the root.
 *
 *
 * ================================================================
 * COMPLETE JAVA IMPLEMENTATION
 * ================================================================
 */

class Solution {

    /*
     * Each tree node stores the maximum available capacity
     * among the baskets represented by that node.
     *
     * We use 4 * n slots because this is a safe standard allocation
     * size for a segment tree over n elements.
     */
    private int[] tree;

    private int n;

    /*
     * Builds the segment tree.
     *
     * Every leaf represents one basket.
     * Every internal node stores the maximum of its two children.
     *
     * Time: O(n)
     */
    private void build(int[] baskets, int node, int left, int right) {

        // Base case: this segment contains exactly one basket.
        if (left == right) {
            tree[node] = baskets[left];
            return;
        }

        // Find the midpoint to divide the current range into two halves.
        int mid = left + (right - left) / 2;

        // Recursively build the left half.
        build(baskets, node * 2, left, mid);

        // Recursively build the right half.
        build(baskets, node * 2 + 1, mid + 1, right);

        // Store the maximum capacity in the entire current range.
        // Each child already stores the maximum of its own range.
        tree[node] = Math.max(
            tree[node * 2],
            tree[node * 2 + 1]
        );
    }

    /*
     * Finds the LEFTMOST available basket whose capacity is >= target.
     *
     * Parameters:
     * node   = current segment tree node
     * left   = first basket index represented by this node
     * right  = last basket index represented by this node
     * target = quantity of the current fruit
     *
     * Returns:
     *   The index of the leftmost eligible basket, or -1 if none exists.
     *
     * IMPORTANT:
     * We search the left child before the right child to preserve
     * the leftmost-basket requirement.
     *
     * Time: O(log n) for this maximum-based leftmost search.
     */
    private int findLeftmost(int node, int left, int right, int target) {

        /*
         * PRUNING CONDITION:
         *
         * If the maximum available capacity in this segment is smaller
         * than target, no basket in this segment can hold the fruit.
         *
         * Skip this entire segment immediately.
         */
        if (tree[node] < target) {
            return -1;
        }

        /*
         * BASE CASE:
         *
         * If left == right, the segment contains one basket.
         *
         * We already know tree[node] >= target, so this basket is valid.
         */
        if (left == right) {
            return left;
        }

        int mid = left + (right - left) / 2;

        /*
         * Search the left half first.
         *
         * It contains smaller indices, so if an eligible basket exists
         * there, it must be the leftmost eligible basket overall.
         */
        int result = findLeftmost(
            node * 2,
            left,
            mid,
            target
        );

        /*
         * If the left subtree returned a valid index, use it.
         *
         * Do not search the right subtree because a valid basket
         * has already been found in an earlier position.
         */
        if (result != -1) {
            return result;
        }

        /*
         * No eligible basket exists in the left half.
         *
         * Search the right half to find the leftmost eligible basket
         * among the remaining positions.
         */
        return findLeftmost(
            node * 2 + 1,
            mid + 1,
            right,
            target
        );
    }

    /*
     * Marks one basket as unavailable.
     *
     * We set its capacity to zero, then recompute the maximum values
     * for all ancestor nodes on the path back to the root.
     *
     * Parameters:
     * node  = current segment tree node
     * left  = first index in this node's segment
     * right = last index in this node's segment
     * index = basket index that has just been used
     *
     * Time: O(log n)
     */
    private void update(int node, int left, int right, int index) {

        // We have reached the leaf representing the selected basket.
        if (left == right) {

            // All valid fruit quantities are positive, so zero
            // represents an unavailable basket.
            tree[node] = 0;
            return;
        }

        int mid = left + (right - left) / 2;

        // Follow the unique path containing the basket being updated.
        if (index <= mid) {

            // The basket belongs to the left child.
            update(node * 2, left, mid, index);

        } else {

            // The basket belongs to the right child.
            update(node * 2 + 1, mid + 1, right, index);
        }

        /*
         * Recalculate the maximum after updating the child.
         *
         * The other child's maximum remains unchanged.
         *
         * This keeps the segment tree correct for future searches.
         */
        tree[node] = Math.max(
            tree[node * 2],
            tree[node * 2 + 1]
        );
    }

    /*
     * MAIN METHOD
     *
     * Processes fruits from left to right, finds the leftmost eligible
     * basket using the segment tree, and marks it as unavailable.
     */
    public int numOfUnplacedFruits(int[] fruits, int[] baskets) {

        n = fruits.length;

        // Allocate enough nodes for the segment tree.
        tree = new int[4 * n];

        // Initially, all baskets are available.
        // Build the tree so each node stores its segment's maximum.
        build(baskets, 1, 0, n - 1);

        // Count fruit types that cannot be placed.
        int unplaced = 0;

        // Process fruit types in the required left-to-right order.
        for (int fruit : fruits) {

            /*
             * Find the leftmost available basket with sufficient
             * capacity.
             *
             * The root (node 1) represents the entire basket array.
             */
            int basketIndex = findLeftmost(
                1,
                0,
                n - 1,
                fruit
            );

            // A return value of -1 means no suitable basket exists.
            if (basketIndex == -1) {

                // This fruit remains unplaced.
                unplaced++;

            } else {

                /*
                 * The leftmost eligible basket has been found.
                 *
                 * Mark it unavailable and update the tree so subsequent
                 * fruits cannot use it.
                 */
                update(1, 0, n - 1, basketIndex);
            }
        }

        // Return the total number of fruit types that remain unplaced.
        return unplaced;
    }
}


/*
 * ================================================================
 * DRY RUN
 * ================================================================
 *
 * Input:
 *
 * fruits  = [4, 2, 5]
 * baskets = [3, 5, 4]
 *
 * STEP 1: BUILD THE SEGMENT TREE
 *
 * The leaf values initially are:
 *
 * basket 0 = 3
 * basket 1 = 5
 * basket 2 = 4
 *
 * The tree stores:
 *
 *                    [0..2] max = 5
 *                   /             \
 *           [0..1] max = 5       [2] = 4
 *             /       \
 *          [0] = 3  [1] = 5
 *
 *
 * STEP 2: PROCESS FRUIT 4
 *
 * Start at the root [0..2], maximum = 5.
 *
 * 5 >= 4, so a valid basket might exist.
 *
 * Search left child [0..1], maximum = 5.
 *
 * Search its left child [0], capacity = 3.
 * 3 < 4, so return -1 and skip basket 0.
 *
 * Search its right child [1], capacity = 5.
 * 5 >= 4, so return index 1.
 *
 * Basket 1 is selected.
 *
 * Update basket 1 to zero.
 *
 * Current capacities:
 * [3, 0, 4]
 *
 * Updated root maximum = 4.
 *
 *
 * STEP 3: PROCESS FRUIT 2
 *
 * Root maximum = 4, so a suitable basket exists.
 *
 * Search left child [0..1], maximum = 3.
 *
 * Search basket 0, capacity = 3.
 * 3 >= 2, so return index 0.
 *
 * Basket 0 is selected and updated to zero.
 *
 * Current capacities:
 * [0, 0, 4]
 *
 * Updated root maximum = 4.
 *
 *
 * STEP 4: PROCESS FRUIT 5
 *
 * Root maximum = 4.
 *
 * Since 4 < 5, the root itself proves that no basket can hold
 * this fruit. The entire tree is skipped in a single check.
 *
 * No suitable basket exists.
 *
 * unplaced = 1
 *
 *
 * FINAL ANSWER: 1
 *
 *
 * ================================================================
 * CORRECTNESS PROOF
 * ================================================================
 *
 * INVARIANT:
 *
 * Each segment tree node stores the maximum capacity among the
 * currently available baskets in its represented segment.
 *
 * 1. BUILD:
 *    The leaves store individual basket capacities, and each parent
 *    stores the maximum of its children. Therefore, every node has
 *    the correct maximum after construction.
 *
 * 2. SEARCH:
 *    If a segment's maximum is less than the fruit quantity, no
 *    basket in that segment is eligible, so pruning is safe.
 *
 *    Otherwise, the algorithm searches the left child first.
 *    If that child contains an eligible basket, the returned basket
 *    is the leftmost eligible basket. If not, the algorithm searches
 *    the right child.
 *
 *    Thus, the search returns the leftmost eligible basket, or -1
 *    when no eligible basket exists.
 *
 * 3. UPDATE:
 *    After a basket is selected, its leaf is set to zero. Recomputing
 *    the maximum of every ancestor maintains the invariant.
 *
 * 4. PROCESSING:
 *    Fruits are processed in their original order. Each successful
 *    placement uses the required leftmost available basket. Every
 *    failed placement increments the counter exactly once.
 *
 * Therefore, the returned count equals the number of unplaced fruits.
 *
 *
 * ================================================================
 * COMPLEXITY ANALYSIS
 * ================================================================
 *
 * BUILD:
 * O(n)
 *
 * Every tree node is built once, and a segment tree contains O(n)
 * nodes.
 *
 * SEARCH:
 * O(log n)
 *
 * A maximum-based leftmost search descends through the tree, pruning
 * segments that cannot contain an eligible basket. It follows at
 * most one promising root-to-leaf path.
 *
 * UPDATE:
 * O(log n)
 *
 * Updating one basket changes only its leaf and its ancestors.
 *
 * TOTAL TIME:
 *
 * Build:             O(n)
 * n searches:        O(n log n)
 * At most n updates: O(n log n)
 *
 * Total = O(n + n log n) = O(n log n)
 *
 * AUXILIARY SPACE:
 *
 * Segment tree: O(n)
 * Recursion stack: O(log n)
 *
 * Total auxiliary space = O(n)
 *
 *
 * ================================================================
 * EDGE CASES
 * ================================================================
 *
 * 1. All fruits fit:
 *    fruits  = [1, 2]
 *    baskets = [2, 3]
 *    Answer  = 0
 *
 * 2. No fruit fits:
 *    fruits  = [5, 6]
 *    baskets = [2, 3]
 *    Answer  = 2
 *
 * 3. Exact capacity match:
 *    fruits  = [5]
 *    baskets = [5]
 *    Answer  = 0
 *
 * 4. Leftmost basket must win:
 *    fruits  = [2]
 *    baskets = [5, 3]
 *    Answer  = 0
 *    Basket 0 is selected, not basket 1.
 *
 * 5. A basket is used only once:
 *    fruits  = [2, 2]
 *    baskets = [2, 3]
 *    Answer  = 0
 *    The first fruit uses basket 0; the second uses basket 1.
 *
 * 6. All baskets have been used:
 *    Any remaining fruit is unplaced because every basket's stored
 *    available capacity is zero.
 *
 *
 * ================================================================
 * INTERVIEW FOLLOW-UP QUESTIONS
 * ================================================================
 *
 * Q1. Why is a segment tree useful here?
 *
 * Answer:
 * It stores the maximum available capacity of each range, allowing
 * us to skip an entire range when its maximum is too small.
 *
 * Q2. Why not use a normal maximum query and binary search?
 *
 * Answer:
 * The basket capacities are not sorted, and availability changes
 * after every placement. Binary search on the original array is
 * therefore not directly applicable. The segment tree supports
 * range maxima, leftmost eligible searches, and point updates.
 *
 * Q3. Why must we search the left child first?
 *
 * Answer:
 * The left child contains earlier basket indices. Searching it first
 * guarantees that an eligible basket there is preferred over every
 * eligible basket in the right child.
 *
 * Q4. Why is setting the leaf to zero safe?
 *
 * Answer:
 * All fruit quantities are positive. Zero is smaller than every
 * possible fruit quantity, so a used basket can never be selected.
 *
 * Q5. What if capacities could be zero or negative?
 *
 * Answer:
 * Using zero as the unavailable marker would no longer be safe for
 * nonpositive fruit quantities. We would need a separate availability
 * representation or a sentinel smaller than every allowed quantity.
 *
 * Q6. What is the difference from the brute-force solution?
 *
 * Answer:
 * Brute force checks baskets individually and costs O(n^2) time.
 * The segment tree skips unsuitable ranges and costs O(n log n).
 *
 * Q7. Which solution should we use for this problem?
 *
 * Answer:
 * Since n <= 100, the greedy two-loop solution is simpler and
 * sufficiently fast. The segment tree is valuable when constraints
 * are much larger or when practicing advanced data structures.
 *
 *
 * ================================================================
 * INTERVIEW APPROACH: HOW TO DERIVE THIS SOLUTION
 * ================================================================
 *
 * Step 1:
 * Identify the expensive operation: repeatedly searching for the
 * leftmost available basket that can hold the current fruit.
 *
 * Step 2:
 * Ask what information can eliminate multiple basket checks.
 * The maximum capacity in a range can prove that no basket in
 * that range is eligible.
 *
 * Step 3:
 * Use a segment tree to store range maximums.
 *
 * Step 4:
 * Search the left child before the right child to preserve the
 * leftmost-index requirement.
 *
 * Step 5:
 * After placing a fruit, update its basket to unavailable and
 * recompute the maximums along the path to the root.
 *
 * Step 6:
 * Count fruits for which the search returns -1.
 *
 * FINAL RECOMMENDATION:
 *
 * Understand the brute-force greedy solution first.
 * Then learn how segment-tree maximums allow whole ranges to be
 * skipped, reducing the total time from O(n^2) to O(n log n).
 *
 * ================================================================
 */




