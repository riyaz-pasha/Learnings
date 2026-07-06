import java.util.*;

/* ============================================================================================
 * PROBLEM: SQUARES OF A SORTED ARRAY
 * ============================================================================================
 * Given an integer array `nums` sorted in non-decreasing order, return a new array of the
 * squares of each number, also sorted in non-decreasing order.
 *
 * This file is structured as a full mock Google-style DSA interview walkthrough.
 * ============================================================================================
 */

/* ============================================================================================
 * SECTION 1: RESTATE THE PROBLEM
 * ============================================================================================
 * In my own words:
 *   - I'm given an array of integers that is already sorted in non-decreasing order.
 *   - Crucially, the array CAN contain negative numbers, zero, and positive numbers, since
 *     "sorted" here does NOT mean "all non-negative" — it just means each element is >= the
 *     previous one (e.g., [-7, -3, 0, 2, 5] is valid input).
 *   - I need to produce a NEW array where every element is squared, and that new array must
 *     itself be sorted in non-decreasing order.
 *
 * Key constraints / inputs / outputs / assumptions:
 *   - Input: int[] nums, sorted non-decreasing, length n >= 0.
 *   - Output: int[] result, same length n, containing squares of nums, sorted non-decreasing.
 *   - The tricky part: squaring a sorted array with negative numbers does NOT preserve order.
 *     E.g., [-4, -1, 0, 3, 10] squared naively -> [16, 1, 0, 9, 100], which is NOT sorted.
 *     We need [0, 1, 9, 16, 100].
 *   - This is why the problem is non-trivial despite input already being sorted — the
 *     "V-shape" of squared negatives + positives is the core challenge.
 * ============================================================================================
 */

/* ============================================================================================
 * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
 * ============================================================================================
 * 1. Q: What is the expected size of `nums`? Could it be empty or a single element?
 *    A (assumed): n can range from 0 to 10^5 (LeetCode-style constraint). Must handle n=0 and n=1.
 *
 * 2. Q: Can `nums` contain negative numbers, zero, or only non-negative numbers?
 *    A (assumed): Yes, it can contain any mix of negative, zero, and positive integers.
 *
 * 3. Q: Are there any bounds on the integer values that could cause overflow when squaring?
 *    A (assumed): Values fit in the range [-10^4, 10^4], so squares fit safely in an int
 *      (max 10^8), but I will code defensively as if this weren't guaranteed (see notes).
 *
 * 4. Q: Should I return a brand-new array, or is in-place modification acceptable/preferred?
 *    A (assumed): Return a new array; do not mutate the input (common interview convention,
 *      also avoids subtle aliasing bugs).
 *
 * 5. Q: Are duplicate values allowed in the input (e.g., multiple -3's or multiple 3's)?
 *    A (assumed): Yes, duplicates are allowed, and the output must still be correctly sorted
 *      (stability of relative order doesn't matter since we're comparing squared values, but
 *      correctness of the final sorted sequence does).
 *
 * 6. Q: Is there any requirement about time/space complexity, or is any correct solution fine
 *    for a first pass?
 *    A (assumed): Aim for O(n) time and O(n) space for the final/optimal solution, but I'll
 *      start with a naive approach and improve it, as is standard interview practice.
 *
 * 7. Q: Is this a single-threaded, single-call function, or do I need to worry about
 *    concurrency / repeated calls on shared state?
 *    A (assumed): Single-threaded, single call. No concurrency concerns.
 *
 * 8. Q: Should the function handle `null` input, or can I assume `nums` is always non-null?
 *    A (assumed): Assume non-null per typical interview convention, but I'll guard against it
 *      defensively in production-quality code.
 * ============================================================================================
 */

/* ============================================================================================
 * SECTION 3: EXAMPLES & EDGE CASES
 * ============================================================================================
 * Example 1 (Normal case):
 *   Input:  nums = [-4, -1, 0, 3, 10]
 *   Output: [0, 1, 9, 16, 100]
 *   Reasoning: squares are 16, 1, 0, 9, 100 -> sorted ascending -> 0,1,9,16,100
 *
 * Example 2 (Edge case — empty and single-element arrays):
 *   Input:  nums = []          -> Output: []
 *   Input:  nums = [5]         -> Output: [25]
 *   Input:  nums = [-5]        -> Output: [25]
 *   Reasoning: trivial arrays should pass through without special-casing if the algorithm
 *   is written generally (pointers simply never move, or loop never executes).
 *
 * Example 3 (Boundary / tie-breaking case — duplicates and symmetric magnitudes):
 *   Input:  nums = [-7, -3, -3, 2, 3, 3, 7]
 *   Output: [4, 9, 9, 9, 9, 49, 49]
 *   Reasoning: -3 and 3 both square to 9 (appearing twice each -> four 9's total), and
 *   -7 and 7 both square to 49. This tests that when two candidate squares are EQUAL, either
 *   one can be picked first (order among equal values doesn't matter since they're identical
 *   values), and the algorithm must not skip/drop/duplicate incorrectly.
 * ============================================================================================
 */

public class SquaresOfSortedArray {

    /* ========================================================================================
     * SECTION 4 & 5/6: ALL POSSIBLE APPROACHES
     * ========================================================================================
     * Paradigms considered and applicability:
     *   - Brute force            -> APPLICABLE (baseline: square then sort)
     *   - Sorting-based           -> APPLICABLE (same as brute force, sorting is the mechanism)
     *   - Hashing-based           -> NOT APPLICABLE: no lookup/frequency/uniqueness problem here
     *   - Two pointer             -> APPLICABLE (optimal solution, exploits sorted input)
     *   - Sliding window          -> NOT APPLICABLE: no contiguous subarray/window being sized
     *   - Divide and conquer      -> NOT APPLICABLE beyond what merge-sort already offers;
     *                                would just reimplement sorting with worse constants
     *   - Greedy                  -> NOT APPLICABLE: no locally-optimal-choice structure; this
     *                                is a merge problem, not a greedy selection problem
     *   - Dynamic programming     -> NOT APPLICABLE: no overlapping subproblems/optimal substructure
     *   - Tree / graph traversal  -> NOT APPLICABLE: no hierarchical or graph relationship
     *   - Heap / priority queue   -> APPLICABLE (alternative: viable but strictly dominated by
     *                                two-pointer; included for completeness)
     *   - Binary search           -> APPLICABLE (used to locate the negative/positive pivot,
     *                                then merge two sorted runs — a variant of two-pointer)
     *   - Monotonic stack/deque   -> NOT APPLICABLE: no "next greater/smaller element" structure
     *   - Trie / segment tree     -> NOT APPLICABLE: no prefix/range-query structure needed
     * ========================================================================================
     */

    /* ----------------------------------------------------------------------------------------
     * APPROACH 1: Brute Force — Square Every Element, Then Sort
     * ----------------------------------------------------------------------------------------
     * Core idea:
     *   Ignore the fact that the input is sorted. Square every element, then sort the
     *   resulting array using a general-purpose sort. This is the "obvious first idea."
     *
     * Data structure / paradigm:
     *   Plain array + built-in comparison sort (dual-pivot quicksort for primitives in Java).
     *
     * Time Complexity: O(n log n) — dominated by the sort.
     * Space Complexity: O(n) for the output array (Arrays.sort on primitives sorts in-place,
     *   so no extra auxiliary array beyond the output itself; ~O(log n) internal sort recursion
     *   stack in the worst case).
     *
     * Pros:
     *   - Extremely simple to write and explain; low risk of bugs.
     *   - Doesn't require noticing the "V-shape" insight.
     * Cons:
     *   - Wastes the fact that input is already sorted — O(n log n) instead of achievable O(n).
     *   - An interviewer will almost always ask "can you do better, given the input is sorted?"
     *
     * When to use in practice:
     *   - Fine as a warm-up / first correct solution to anchor the conversation.
     *   - Acceptable in real production code only if simplicity outweighs the performance
     *     benefit and n is small/non-critical-path.
     * ----------------------------------------------------------------------------------------
     */
    public static int[] bruteForceSquareThenSort(int[] nums) {
        if (nums == null) {
            throw new IllegalArgumentException("nums must not be null");
        }
        int n = nums.length;
        int[] squares = new int[n];
        for (int i = 0; i < n; i++) {
            squares[i] = nums[i] * nums[i];
        }
        Arrays.sort(squares); // O(n log n)
        return squares;
    }

    /* ----------------------------------------------------------------------------------------
     * APPROACH 2: Two Pointer (Optimal)
     * ----------------------------------------------------------------------------------------
     * Core idea:
     *   Because the input is sorted, the LARGEST squares can only come from the two ENDS of
     *   the array (the most negative value or the most positive value — whichever has the
     *   larger absolute value). So we walk two pointers inward from both ends, always picking
     *   the larger of the two squared endpoint values and placing it at the END of the result
     *   array, working backwards to the front. This fills the output array in descending order
     *   of magnitude, which is ascending order in the output positions.
     *
     * Data structure / paradigm:
     *   Two-pointer technique (converging pointers), single pass.
     *
     * Time Complexity: O(n) — each element is visited exactly once across both pointers.
     * Space Complexity: O(n) for the output array (required, since we must return a new array);
     *   O(1) additional auxiliary space beyond that.
     *
     * Pros:
     *   - Optimal time complexity; single linear pass.
     *   - No sorting needed at all — directly exploits the problem's sortedness.
     *   - Simple once the "fill from the back" insight clicks.
     * Cons:
     *   - Slightly less obvious/intuitive than brute force; requires the "V-shape" insight.
     *   - Filling the result array back-to-front can feel unintuitive on first exposure.
     *
     * When to use in practice:
     *   - Always preferred for this exact problem — no meaningful downside versus brute force.
     * ----------------------------------------------------------------------------------------
     */
    public static int[] twoPointerSquares(int[] nums) {
        if (nums == null) {
            throw new IllegalArgumentException("nums must not be null");
        }
        int n = nums.length;
        int[] result = new int[n];

        int leftPointer = 0;
        int rightPointer = n - 1;
        int writePosition = n - 1; // fill result from the back (largest squares first)

        while (leftPointer <= rightPointer) {
            int leftSquare = nums[leftPointer] * nums[leftPointer];
            int rightSquare = nums[rightPointer] * nums[rightPointer];

            if (leftSquare > rightSquare) {
                result[writePosition] = leftSquare;
                leftPointer++;
            } else {
                result[writePosition] = rightSquare;
                rightPointer--;
            }
            writePosition--;
        }
        return result;
    }

    /* ----------------------------------------------------------------------------------------
     * APPROACH 3: Binary Search for Pivot + Merge Two Sorted Runs
     * ----------------------------------------------------------------------------------------
     * Core idea:
     *   Use binary search to find the "pivot" index — the first index where nums[i] >= 0.
     *   This splits the array into two sorted runs when viewed by ABSOLUTE VALUE:
     *     - Left run (all negatives), which is sorted descending in absolute value as we move
     *       left-to-right (so read it right-to-left to get ascending absolute value).
     *     - Right run (zero/positives), already ascending in absolute value.
     *   Then merge these two "sorted-by-absolute-value" runs like classic merge-sort's merge
     *   step, squaring as we go.
     *
     * Data structure / paradigm:
     *   Binary search (to locate pivot) + two-pointer merge (classic merge-sort merge step).
     *   This is essentially a more "explicit" restructuring of Approach 2, useful to show I
     *   understand the underlying structure even more deeply.
     *
     * Time Complexity: O(log n) for the binary search + O(n) for the merge = O(n) overall.
     * Space Complexity: O(n) for the output array; O(1) additional auxiliary space.
     *
     * Pros:
     *   - Demonstrates deeper insight into the "two sorted runs" structure.
     *   - Still linear overall.
     * Cons:
     *   - More code, more edge cases (empty runs, all-negative, all-positive arrays), more
     *     surface area for off-by-one bugs versus Approach 2.
     *   - No asymptotic benefit over Approach 2 — strictly more complex for the same result.
     *
     * When to use in practice:
     *   - Rarely, if ever, preferred over Approach 2 for THIS problem. Worth mentioning to show
     *     range, but Approach 2 is what I'd actually ship.
     * ----------------------------------------------------------------------------------------
     */
    public static int[] binarySearchPivotThenMerge(int[] nums) {
        if (nums == null) {
            throw new IllegalArgumentException("nums must not be null");
        }
        int n = nums.length;
        int[] result = new int[n];
        if (n == 0) {
            return result;
        }

        // Binary search for the first index where nums[index] >= 0.
        int low = 0;
        int high = n; // high = n means "no non-negative found" (all negative)
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (nums[mid] >= 0) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        int pivot = low; // first index with nums[pivot] >= 0 (or n if none exists)

        // Left run: indices [0, pivot - 1], all negative, read right-to-left for ascending |value|
        // Right run: indices [pivot, n - 1], all non-negative, already ascending
        int leftRunPointer = pivot - 1;
        int rightRunPointer = pivot;
        int writePosition = 0;

        while (leftRunPointer >= 0 && rightRunPointer < n) {
            int leftAbsSquare = nums[leftRunPointer] * nums[leftRunPointer];
            int rightAbsSquare = nums[rightRunPointer] * nums[rightRunPointer];
            if (leftAbsSquare <= rightAbsSquare) {
                result[writePosition++] = leftAbsSquare;
                leftRunPointer--;
            } else {
                result[writePosition++] = rightAbsSquare;
                rightRunPointer++;
            }
        }
        // Drain whichever run remains
        while (leftRunPointer >= 0) {
            result[writePosition++] = nums[leftRunPointer] * nums[leftRunPointer];
            leftRunPointer--;
        }
        while (rightRunPointer < n) {
            result[writePosition++] = nums[rightRunPointer] * nums[rightRunPointer];
            rightRunPointer++;
        }
        return result;
    }

    /* ----------------------------------------------------------------------------------------
     * APPROACH 4 (Bonus, for completeness): Max-Heap / Priority Queue
     * ----------------------------------------------------------------------------------------
     * Core idea:
     *   Push all squared values into a max-heap, then pop repeatedly, filling the result array
     *   from the back. Included only to show breadth of knowledge — it is strictly dominated
     *   by Approach 2 for this problem.
     *
     * Data structure / paradigm: Priority Queue (binary heap).
     *
     * Time Complexity: O(n log n) — n insertions and n extractions, each O(log n).
     * Space Complexity: O(n) for the heap plus O(n) for the output.
     *
     * Pros: Conceptually simple; generalizes to unsorted input (where two-pointer wouldn't
     *   directly apply).
     * Cons: Strictly worse than Approach 2 here since we HAVE sorted input; unnecessary
     *   O(log n) factor and heap overhead.
     *
     * When to use in practice:
     *   - Only if the input were NOT sorted and full sorting was also undesirable for some
     *     other reason (rare). Not recommended for this problem.
     * ----------------------------------------------------------------------------------------
     */
    public static int[] maxHeapSquares(int[] nums) {
        if (nums == null) {
            throw new IllegalArgumentException("nums must not be null");
        }
        int n = nums.length;
        int[] result = new int[n];
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        for (int value : nums) {
            maxHeap.offer(value * value);
        }
        for (int writePosition = n - 1; writePosition >= 0; writePosition--) {
            result[writePosition] = maxHeap.poll();
        }
        return result;
    }

    /* ========================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================================
     * Approach                          | Time       | Space | Best For                | Limitations
     * ----------------------------------|------------|-------|-------------------------|--------------------------------
     * 1. Brute Force (Square + Sort)    | O(n log n) | O(n)  | Quick correct baseline  | Ignores sorted input; slower
     * 2. Two Pointer (Optimal)          | O(n)       | O(n)  | Production use, interviews| Requires the V-shape insight
     * 3. Binary Search Pivot + Merge    | O(n)       | O(n)  | Showing deeper structure| More code/edge cases, no gain
     * 4. Max-Heap / Priority Queue      | O(n log n) | O(n)  | Unsorted input variants | Unneeded overhead here
     * ========================================================================================
     */

    /* ========================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================================
     * I would present Approach 2 (Two Pointer) as my final solution:
     *   - It is optimal: O(n) time, O(n) space (the minimum possible, since we must produce
     *     an n-element output and must inspect every input element at least once).
     *   - It is fast to code correctly under interview time pressure — a single while loop,
     *     no nested structures, no library sort call to reason about.
     *   - It directly demonstrates that I noticed and exploited the key structural property
     *     of the input (sortedness + the negative/positive "V-shape" under squaring), which
     *     is exactly what interviewers are probing for in this problem.
     *   - I would still mention Approach 1 first as a warm-up ("here's the obvious O(n log n)
     *     solution... but since the input is sorted, we can do better") to show a structured
     *     thought process, then upgrade to Approach 2.
     * ========================================================================================
     */

    /* ========================================================================================
     * SECTION 9: DEEP DIVE — POLISHED, PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ========================================================================================
     */
    public static int[] sortedSquares(int[] nums) {
        // Defensive null check — production code should never assume non-null input,
        // even if the interviewer said we could assume it.
        if (nums == null) {
            throw new IllegalArgumentException("Input array 'nums' must not be null.");
        }

        int elementCount = nums.length;
        int[] squaredResult = new int[elementCount];

        // Edge case: empty array. Loop below naturally handles this (never executes),
        // but calling it out explicitly documents intent for future readers.
        if (elementCount == 0) {
            return squaredResult; // returns an empty int[]
        }

        // Two pointers converging from the outside in. Because the array is sorted, the
        // element with the LARGEST absolute value (and therefore the largest square) must
        // be at one of the two ends at any point in time.
        int leftPointer = 0;
        int rightPointer = elementCount - 1;

        // We fill the result array from the END toward the START, because at each step we
        // are placing the CURRENT LARGEST remaining square, which belongs at the highest
        // still-unfilled index.
        int writeIndex = elementCount - 1;

        while (leftPointer <= rightPointer) {
            // Use long-free arithmetic here deliberately: per our clarifying assumption,
            // |nums[i]| <= 10^4, so squares are bounded by 10^8, safely within int range.
            // (If the interviewer relaxes that constraint, switch these to long to avoid
            // overflow, then narrow/cast when writing into the int[] result, or change the
            // result type to long[].)
            int leftValueSquared = nums[leftPointer] * nums[leftPointer];
            int rightValueSquared = nums[rightPointer] * nums[rightPointer];

            if (leftValueSquared > rightValueSquared) {
                squaredResult[writeIndex] = leftValueSquared;
                leftPointer++; // the left candidate was consumed; advance inward
            } else {
                // On a tie (leftValueSquared == rightValueSquared), it does not matter which
                // one we pick first — the values are identical, so either choice yields the
                // same correct sequence. We arbitrarily favor the right side on ties here.
                squaredResult[writeIndex] = rightValueSquared;
                rightPointer--; // the right candidate was consumed; advance inward
            }
            writeIndex--; // move to the next (lower) position to fill
        }

        return squaredResult;
    }

    /* ========================================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================================
     * Tracing sortedSquares() on nums = [-4, -1, 0, 3, 10]  (n = 5)
     *
     * Initial state:
     *   squaredResult = [_, _, _, _, _]
     *   leftPointer = 0, rightPointer = 4, writeIndex = 4
     *
     * Iteration 1:
     *   nums[leftPointer=0] = -4  -> leftValueSquared  = 16
     *   nums[rightPointer=4] = 10 -> rightValueSquared = 100
     *   100 > 16, so we place rightValueSquared (100) at index 4.
     *   squaredResult = [_, _, _, _, 100]
     *   rightPointer-- -> rightPointer = 3
     *   writeIndex--   -> writeIndex = 3
     *
     * Iteration 2:
     *   nums[leftPointer=0] = -4 -> leftValueSquared  = 16
     *   nums[rightPointer=3] = 3 -> rightValueSquared = 9
     *   16 > 9, so we place leftValueSquared (16) at index 3.
     *   squaredResult = [_, _, _, 16, 100]
     *   leftPointer++ -> leftPointer = 1
     *   writeIndex--  -> writeIndex = 2
     *
     * Iteration 3:
     *   nums[leftPointer=1] = -1 -> leftValueSquared  = 1
     *   nums[rightPointer=3] = 3 -> rightValueSquared = 9
     *   9 > 1 (not >), condition "leftValueSquared > rightValueSquared" is false (1 > 9 is false)
     *   so we place rightValueSquared (9) at index 2.
     *   squaredResult = [_, _, 9, 16, 100]
     *   rightPointer-- -> rightPointer = 2
     *   writeIndex--   -> writeIndex = 1
     *
     * Iteration 4:
     *   nums[leftPointer=1] = -1 -> leftValueSquared  = 1
     *   nums[rightPointer=2] = 0 -> rightValueSquared = 0
     *   1 > 0 is true, so we place leftValueSquared (1) at index 1.
     *   squaredResult = [_, 1, 9, 16, 100]
     *   leftPointer++ -> leftPointer = 2
     *   writeIndex--  -> writeIndex = 0
     *
     * Iteration 5:
     *   leftPointer (2) <= rightPointer (2), loop continues.
     *   nums[leftPointer=2] = 0 -> leftValueSquared  = 0
     *   nums[rightPointer=2] = 0 -> rightValueSquared = 0
     *   0 > 0 is false, so we place rightValueSquared (0) at index 0.
     *   squaredResult = [0, 1, 9, 16, 100]
     *   rightPointer-- -> rightPointer = 1
     *   writeIndex--   -> writeIndex = -1
     *
     * Loop condition check: leftPointer (2) <= rightPointer (1) is FALSE -> loop exits.
     *
     * Final result: [0, 1, 9, 16, 100]  ✓ matches expected output from Example 1.
     * ========================================================================================
     */

    /* ========================================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================================
     * - Brute Force (square + sort): simplest to reason about, O(n log n), good warm-up answer,
     *   but ignores the sorted-input structure and is not what I'd ship.
     * - Two Pointer (recommended): O(n) time, O(n) space, exploits the sorted input directly,
     *   simple to implement correctly, and is the answer I'd finalize in an interview.
     * - Binary Search Pivot + Merge: also O(n), demonstrates a deeper structural understanding
     *   (explicit "two sorted runs" view), but adds complexity with no asymptotic benefit.
     * - Max-Heap: included for completeness; strictly dominated here, would only be relevant if
     *   the input were NOT sorted.
     *
     * Known assumptions / limitations of the final (Two Pointer) solution:
     *   - Assumes values are small enough that squaring doesn't overflow `int` (per clarifying
     *     question #3). If this assumption doesn't hold, switch to `long` arithmetic/output.
     *   - Assumes `nums` is non-null (guarded defensively with an explicit exception either way).
     *   - Returns a NEW array; does not mutate the input, per clarifying question #4.
     * ========================================================================================
     */

    /* ========================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================================
     * 1. "Can you solve this in-place, without allocating a new array?" (Trickier: naive
     *    in-place overwriting can clobber values you still need to read; would need careful
     *    ordering or a temporary buffer — arguably defeats the "no extra space" goal.)
     * 2. "What if the array is extremely large (billions of elements) and doesn't fit in
     *    memory — how would you adapt this to a streaming or external-memory setting?"
     * 3. "What if `nums` were NOT sorted — how would your approach change?" (Answer: two-pointer
     *    no longer applies directly; fall back to square-then-sort, O(n log n), or a heap-based
     *    approach if only the top-k largest squares are needed.)
     * 4. "How would you handle this if the array could contain very large values causing
     *    integer overflow on squaring?" (Switch to `long` arithmetic and/or `long[]` output.)
     * 5. "Can you extend this to return the k largest squares only, instead of all of them?"
     *    (Two pointer still works: just stop after k writes, or use a bounded max-heap.)
     * 6. "How would this change if you needed the result sorted in non-increasing order
     *    instead?" (Trivial adaptation: write from the front instead of the back.)
     * ========================================================================================
     */

    /* ========================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================================
     * 1. Assuming the input array only contains non-negative numbers because it's "sorted,"
     *    and therefore believing squaring preserves order. This is the single most common
     *    trap — "sorted" does not mean "all non-negative."
     * 2. Off-by-one errors in the two-pointer loop condition — using `<` instead of `<=` for
     *    `leftPointer <= rightPointer`, which causes the middle element (when leftPointer ==
     *    rightPointer) to be skipped and left as a default 0 in the result array.
     * 3. Forgetting to decrement `writeIndex` on every iteration (not just some branches),
     *    which causes the result array to be filled incorrectly or with gaps.
     * 4. Not handling ties correctly — assuming the tie-break direction matters when it
     *    doesn't (since equal squares are equal values, either pointer can be advanced first
     *    without changing correctness) — leading to unnecessary defensive complexity or,
     *    conversely, an actual bug if the wrong pointer is advanced without updating the
     *    correct index.
     * ========================================================================================
     */

    /* ========================================================================================
     * MAIN METHOD — Demonstrates all approaches against the examples from Section 3
     * ========================================================================================
     */
    public static void main(String[] args) {
        int[][] testCases = {
            {-4, -1, 0, 3, 10},
            {},
            {5},
            {-5},
            {-7, -3, -3, 2, 3, 3, 7}
        };

        for (int[] testCase : testCases) {
            System.out.println("Input: " + Arrays.toString(testCase));
            System.out.println("  Brute Force:        " + Arrays.toString(bruteForceSquareThenSort(testCase)));
            System.out.println("  Two Pointer:         " + Arrays.toString(twoPointerSquares(testCase)));
            System.out.println("  Binary Search+Merge: " + Arrays.toString(binarySearchPivotThenMerge(testCase)));
            System.out.println("  Max-Heap:            " + Arrays.toString(maxHeapSquares(testCase)));
            System.out.println("  Final (sortedSquares):" + Arrays.toString(sortedSquares(testCase)));
            System.out.println();
        }
    }
}
