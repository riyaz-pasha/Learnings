import java.util.*;

/**
 * ============================================================================
 * LeetCode 287 - Find the Duplicate Number
 * ============================================================================
 *
 * Problem
 * -------
 * Given an array nums containing n + 1 integers where:
 *
 *      1 <= nums[i] <= n
 *
 * There is exactly ONE duplicate number, although it may appear
 * multiple times.
 *
 * Constraints:
 *
 * 1. Cannot modify the input array.
 * 2. Must use O(1) extra space.
 *
 * Example:
 *
 * nums = [1,3,4,2,2]
 *
 * Answer = 2
 *
 *
 * ============================================================================
 * FIRST THOUGHT PROCESS
 * ============================================================================
 *
 * Let's think about the obvious solutions first.
 *
 * 1) Brute Force
 *
 * Compare every pair.
 *
 * Time  : O(n²)
 * Space : O(1)
 *
 * Too slow.
 *
 *
 * --------------------------------------------------
 *
 * 2) Sort the array
 *
 * After sorting:
 *
 * 1 2 2 3 4
 *
 * duplicate becomes adjacent.
 *
 * But...
 *
 * NOT ALLOWED because sorting modifies the array.
 *
 *
 * --------------------------------------------------
 *
 * 3) HashSet
 *
 * Store every number.
 *
 * If already present -> duplicate.
 *
 * Time  : O(n)
 * Space : O(n)
 *
 * Also NOT ALLOWED because extra space is O(n).
 *
 *
 * ============================================================================
 * IMPORTANT OBSERVATION
 * ============================================================================
 *
 * Interview Hint:
 *
 * Whenever you see BOTH:
 *
 *      values are in range [1...n]
 *
 * AND
 *
 *      array size = n + 1
 *
 * STOP.
 *
 * Don't immediately think about arrays.
 *
 * Think:
 *
 *      "Can these values themselves be treated as pointers?"
 *
 *
 * ============================================================================
 * WHY CAN VALUES BE USED AS POINTERS?
 * ============================================================================
 *
 * Every value lies between:
 *
 *      1 ... n
 *
 * Those are ALL valid indices in the array.
 *
 *
 * Example:
 *
 * nums = [1,3,4,2,2]
 *
 *
 * Index
 *
 *      0   1   2   3   4
 *      -------------------
 *      1   3   4   2   2
 *
 *
 * Instead of reading this as an array...
 *
 * Think of each value as the NEXT POINTER.
 *
 *
 * Index 0 points to index 1
 *
 *      0 -> 1
 *
 * because nums[0] = 1
 *
 *
 * Index 1 points to index 3
 *
 *      1 -> 3
 *
 * because nums[1] = 3
 *
 *
 * Index 3 points to index 2
 *
 *      3 -> 2
 *
 * because nums[3] = 2
 *
 *
 * Index 2 points to index 4
 *
 *      2 -> 4
 *
 * because nums[2] = 4
 *
 *
 * Index 4 points to index 2
 *
 *      4 -> 2
 *
 * because nums[4] = 2
 *
 *
 * ============================================================================
 * GRAPH REPRESENTATION
 * ============================================================================
 *
 *                 0
 *                 |
 *                 v
 *                 1
 *                 |
 *                 v
 *                 3
 *                 |
 *                 v
 *                 2
 *                 |
 *                 v
 *                 4
 *                 |
 *                 |
 *                 +------+
 *                        |
 *                        v
 *                        2
 *
 *
 * Notice something?
 *
 * We are going around forever:
 *
 *      2 -> 4 -> 2 -> 4 -> 2 ...
 *
 *
 * That's a CYCLE.
 *
 *
 * ============================================================================
 * WHY DOES DUPLICATE CREATE A CYCLE?
 * ============================================================================
 *
 * Imagine:
 *
 * Two different indices contain the same value.
 *
 * Example:
 *
 * nums = [1,3,4,2,2]
 *
 * Both:
 *
 * index 3
 *
 * and
 *
 * index 4
 *
 * point to:
 *
 * index 2
 *
 *
 * Multiple paths merge into the same node.
 *
 * Once paths merge,
 * they can never split again.
 *
 * Eventually,
 * we revisit a previously visited node.
 *
 * That forms a cycle.
 *
 *
 * This is guaranteed because:
 *
 * We have:
 *
 *      n + 1 nodes
 *
 * but only
 *
 *      n possible destinations.
 *
 * By the Pigeonhole Principle,
 * some destination MUST repeat.
 *
 *
 * ============================================================================
 * HUGE INTERVIEW PATTERN
 * ============================================================================
 *
 * Whenever you see:
 *
 *      values are valid indices
 *
 * ask yourself:
 *
 *      "Can I model this array as a linked list?"
 *
 *
 * This trick appears in several interview problems.
 *
 *
 * ============================================================================
 * NOW WHICH ALGORITHM FINDS A CYCLE IN A LINKED LIST?
 * ============================================================================
 *
 * Floyd's Cycle Detection
 *
 * also called
 *
 * Tortoise and Hare Algorithm
 *
 *
 * ============================================================================
 * PHASE 1
 * Find an intersection point inside the cycle.
 * ============================================================================
 *
 * Two pointers:
 *
 * slow = moves 1 step
 *
 * fast = moves 2 steps
 *
 *
 * Eventually:
 *
 * fast catches slow.
 *
 *
 * Exactly the same algorithm used for
 * detecting a loop in a linked list.
 *
 *
 * ============================================================================
 * DRY RUN
 * ============================================================================
 *
 * nums = [1,3,4,2,2]
 *
 *
 * Initial
 *
 * slow = nums[0] = 1
 * fast = nums[0] = 1
 *
 *
 * -------------------
 *
 * Iteration 1
 *
 * slow = nums[1]
 *      = 3
 *
 * fast = nums[ nums[1] ]
 *      = nums[3]
 *      = 2
 *
 *
 * slow = 3
 * fast = 2
 *
 *
 * -------------------
 *
 * Iteration 2
 *
 * slow = nums[3]
 *      = 2
 *
 * fast = nums[ nums[2] ]
 *      = nums[4]
 *      = 2
 *
 *
 * Both meet at 2.
 *
 *
 * IMPORTANT:
 *
 * This meeting point is NOT necessarily
 * the duplicate.
 *
 * It is just SOMEWHERE inside the cycle.
 *
 *
 * ============================================================================
 * PHASE 2
 * Find the beginning of the cycle.
 * ============================================================================
 *
 * Create another pointer:
 *
 * finder = nums[0]
 *
 *
 * Move:
 *
 * finder -> one step
 *
 * slow -> one step
 *
 *
 * Wherever they meet
 * is the START of the cycle.
 *
 *
 * That start node is exactly
 * the duplicate number.
 *
 *
 * ============================================================================
 * WHY DOES PHASE 2 WORK?
 * ============================================================================
 *
 * Suppose:
 *
 * Distance from start to cycle = L
 *
 * Distance inside cycle until meeting = X
 *
 * Cycle length = C
 *
 *
 * Slow travels:
 *
 *      L + X
 *
 *
 * Fast travels:
 *
 *      2(L + X)
 *
 *
 * Difference:
 *
 *      L + X
 *
 * is exactly one or more complete cycles.
 *
 * Therefore:
 *
 *      L + X = k × C
 *
 *
 * Rearranging:
 *
 *      L = k × C - X
 *
 *
 * Which means:
 *
 * One pointer starting from beginning
 *
 * and
 *
 * One pointer starting from meeting point
 *
 * will reach the cycle entrance together.
 *
 *
 * ============================================================================
 * FINAL ALGORITHM
 * ============================================================================
 *
 * Phase 1
 *
 * 1. Create slow and fast pointers.
 * 2. Move slow by one.
 * 3. Move fast by two.
 * 4. Continue until they meet.
 *
 *
 * Phase 2
 *
 * 5. Create finder pointer at beginning.
 * 6. Move finder and slow one step each.
 * 7. Meeting point is duplicate.
 *
 *
 * ============================================================================
 * TIME COMPLEXITY
 * ============================================================================
 *
 * Phase 1 : O(n)
 *
 * Phase 2 : O(n)
 *
 * Total:
 *
 * O(n)
 *
 *
 * ============================================================================
 * SPACE COMPLEXITY
 * ============================================================================
 *
 * Only three integer variables are used.
 *
 * O(1)
 *
 *
 * ============================================================================
 * INTERVIEW RECOGNITION CHECKLIST
 * ============================================================================
 *
 * If you see ALL of these:
 *
 * ✓ Values are in range [1...n]
 *
 * ✓ Array length is n+1
 *
 * ✓ Cannot modify array
 *
 * ✓ Constant extra space
 *
 * Immediately ask:
 *
 *      "Can values become pointers?"
 *
 * If YES,
 *
 * think:
 *
 *      Linked List
 *
 * If Linked List,
 *
 * think:
 *
 *      Floyd's Cycle Detection
 *
 * ============================================================================
 */

class Solution {

    public int findDuplicate(int[] nums) {

        /*
         * ---------------------------------------------
         * Phase 1
         *
         * Find the meeting point inside the cycle.
         *
         * Both pointers start from the first "node"
         * represented by nums[0].
         * ---------------------------------------------
         */
        int slow = nums[0];
        int fast = nums[0];

        do {

            // Move one step.
            slow = nums[slow];

            // Move two steps.
            fast = nums[nums[fast]];

        } while (slow != fast);

        /*
         * ---------------------------------------------
         * Phase 2
         *
         * Move another pointer from the beginning.
         *
         * Both pointers now move one step.
         *
         * Their meeting point is the beginning
         * of the cycle, which is the duplicate.
         * ---------------------------------------------
         */
        int finder = nums[0];

        while (finder != slow) {

            finder = nums[finder];
            slow = nums[slow];
        }

        return finder;
    }
}

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: FIND THE DUPLICATE NUMBER
 * LeetCode #287
 * ============================================================================
 *
 * This file simulates a complete, structured technical interview walkthrough,
 * covering every stage from problem restatement to a production-quality
 * optimal solution with full analysis.
 */
class FindDuplicateNumber {

    /*
     * ========================================================================
     * 1. RESTATE THE PROBLEM
     * ========================================================================
     *
     * We are given an array `nums` of length n+1, containing integers in the
     * range [1, n] (inclusive). Since there are n+1 slots but only n distinct
     * possible values, by the Pigeonhole Principle at least one value must
     * repeat.
     *
     * Guarantee: exactly ONE value is duplicated, but that value may appear
     * two or more times in the array (i.e., total duplicate occurrences could
     * be more than 2, but only one distinct value is ever duplicated).
     *
     * Goal: Return that duplicated value.
     *
     * Hard constraints (these are the crux of the problem's difficulty):
     *   (a) You must NOT modify the input array `nums`.
     *   (b) You must use only O(1) (constant) extra space.
     *
     * Without these two constraints, this problem is trivial (sort it, or
     * throw it in a HashSet). The constraints are what make this a genuinely
     * hard problem — they rule out sorting the array in-place and rule out
     * any extra array/set/map proportional to n.
     *
     * Input:  int[] nums, length n+1, values in [1, n]
     * Output: a single int — the repeated value
     * Assumption: input is always valid per problem statement (no need to
     *             defensively handle "no duplicate" or "multiple distinct
     *             duplicates" cases, though I will validate input defensively
     *             in the production implementation).
     */


    /*
     * ========================================================================
     * 2. CLARIFYING QUESTIONS (with assumed answers)
     * ========================================================================
     *
     * Q1: What is the range of n? Could this be small (n < 10) or huge
     *     (n ~ 10^7)? This affects whether O(n log n) vs O(n) matters.
     *     A1: Assume n can be up to ~10^5 to 10^7, so we should aim for
     *         O(n) or O(n log n) time; O(n^2) will not scale.
     *
     * Q2: Is `nums` guaranteed non-null and of length >= 2 (i.e., n >= 1)?
     *     A2: Yes, assume valid, non-null input of length n+1 with n >= 1.
     *
     * Q3: Can the duplicate value appear more than twice?
     *     A3: Yes — the problem only guarantees one *distinct* value repeats,
     *         it could appear 2, 3, or up to n+1 times in an adversarial case
     *         (e.g., all elements are the same value).
     *
     * Q4: Are all other values (non-duplicates) guaranteed to appear exactly
     *     once?
     *     A4: Yes, per problem constraints — only one distinct value repeats;
     *         every other value in [1, n] appears exactly once.
     *
     * Q5: Can I use recursion (which uses O(log n) or O(n) stack space)? Does
     *     that count against the O(1) space constraint?
     *     A5: For this interview, assume iterative solutions are preferred
     *         and recursive call-stack space should be avoided or minimized
     *         since it could count as extra space in a strict reading.
     *
     * Q6: Is the array read-only in the sense that I can't even temporarily
     *     swap two elements and swap them back?
     *     A6: Correct — treat `nums` as strictly read-only. No mutation, not
     *         even temporary swaps.
     *
     * Q7: Do I need to return all indices where the duplicate occurs, or just
     *     the value itself?
     *     A7: Just the duplicated value itself.
     *
     * Q8: Is thread-safety or concurrent access to `nums` a concern?
     *     A8: No, assume single-threaded, synchronous execution.
     */


    /*
     * ========================================================================
     * 3. EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (Normal case):
     *   nums = [1, 3, 4, 2, 2]   (n = 4, length = 5)
     *   Expected output: 2
     *   Reasoning: values 1,2,3,4 should each appear once; 2 appears twice.
     *
     * Example 2 (Edge case — duplicate appears many times):
     *   nums = [3, 1, 3, 4, 2]   -> wait, let's use a clean "all same" case:
     *   nums = [2, 2, 2, 2, 2]   (n = 4, length = 5)
     *   Expected output: 2
     *   Reasoning: an adversarial case where every slot holds the duplicate.
     *              This stresses cycle-detection-based solutions especially.
     *
     * Example 3 (Boundary / tie-breaking-like case — smallest possible n):
     *   nums = [1, 1]            (n = 1, length = 2)
     *   Expected output: 1
     *   Reasoning: smallest valid input. Only possible value is 1, and it
     *              must repeat since there are 2 slots for 1 possible value.
     *              There's no real "tie" in this problem since exactly one
     *              distinct value is guaranteed to be the duplicate — but
     *              this boundary case verifies our solution doesn't special
     *              case n=1 incorrectly (e.g., off-by-one in loop bounds).
     */


    /*
     * ========================================================================
     * 4 & 5. ALL POSSIBLE APPROACHES
     * ========================================================================
     *
     * Paradigms considered and explicitly ruled out:
     *   - Divide & Conquer: No natural way to split this array and merge
     *     partial duplicate-detection results without extra space or
     *     violating the "don't modify" constraint. Skipped.
     *   - Dynamic Programming: There's no overlapping subproblem / optimal
     *     substructure here — this is a detection problem, not an
     *     optimization problem. Skipped.
     *   - Greedy: No greedy choice property applies; we can't make locally
     *     optimal picks that build toward the answer. Skipped.
     *   - Heap / Priority Queue: No ordering/priority semantics needed;
     *     would only add O(n) space and O(n log n) time for no benefit
     *     over simpler approaches. Skipped.
     *   - Monotonic Stack/Deque: No notion of a "next greater/smaller
     *     element" or windowed monotonic property exists here. Skipped.
     *   - Trie / Segment Tree: These structures shine for prefix queries or
     *     range queries; there's no such structure in this problem, and
     *     both would require non-constant extra space anyway. Skipped.
     *
     * Paradigms that DO apply, covered below in increasing order of quality:
     *   Approach 1: Brute Force (nested loop)
     *   Approach 2: Sorting-based
     *   Approach 3: Hashing-based (HashSet)
     *   Approach 4: Bit Manipulation (bit-counting)
     *   Approach 5: Binary Search on Value Range
     *   Approach 6: Floyd's Cycle Detection (Tortoise and Hare) -- OPTIMAL
     */

    /*
     * ------------------------------------------------------------------
     * Approach 1: Brute Force (Nested Loop)
     * ------------------------------------------------------------------
     * Core idea: For each element, scan the rest of the array to see if any
     * other element matches it. First match found is the duplicate.
     *
     * Data structure / paradigm: None — pure exhaustive comparison.
     *
     * Time Complexity: O(n^2) — for each of n+1 elements, we scan up to n
     *                  other elements.
     * Space Complexity: O(1) — no extra structures, only loop indices.
     *
     * Pros:
     *   - Trivial to write correctly, no risk of subtle bugs.
     *   - Satisfies both constraints (no mutation, O(1) space).
     * Cons:
     *   - Quadratic time is unacceptable for large n (e.g., n = 10^6 means
     *     ~10^12 operations).
     * When to use: Only for tiny inputs, or as a first "correctness oracle"
     * to validate faster approaches during development/testing. Would not
     * present this as a final answer in a real Google interview beyond
     * mentioning it exists.
     */
    public static int findDuplicateBruteForce(int[] nums) {
        for (int outerIndex = 0; outerIndex < nums.length; outerIndex++) {
            for (int innerIndex = outerIndex + 1; innerIndex < nums.length; innerIndex++) {
                if (nums[outerIndex] == nums[innerIndex]) {
                    return nums[outerIndex];
                }
            }
        }
        throw new IllegalArgumentException("No duplicate found — invalid input per problem constraints.");
    }

    /*
     * ------------------------------------------------------------------
     * Approach 2: Sorting-based
     * ------------------------------------------------------------------
     * Core idea: If the array were sorted, the duplicate would be the first
     * pair of adjacent equal elements. BUT we cannot modify `nums` in place,
     * so we must copy it first, which costs O(n) space — violating the O(1)
     * space constraint. I include this approach for completeness and to
     * show I recognize the trade-off, but flag that it fails the space
     * requirement as strictly stated.
     *
     * Data structure / paradigm: Sorting (Arrays.sort uses dual-pivot
     * quicksort for primitives).
     *
     * Time Complexity: O(n log n) for the sort.
     * Space Complexity: O(n) for the defensive copy (violates constraint!).
     *                    (Arrays.sort on primitives itself is in-place,
     *                    O(log n) stack space for its internal recursion,
     *                    but the copy dominates.)
     *
     * Pros:
     *   - Conceptually simple, easy to explain and code under pressure.
     * Cons:
     *   - Violates the O(1) space constraint due to the mandatory copy.
     *   - Slower than optimal O(n) approaches.
     * When to use: Only if the "no modification" constraint were relaxed
     * AND O(n) extra space were acceptable — e.g., a simpler variant of
     * this problem. Not viable here, but worth stating explicitly in an
     * interview to show awareness of the constraint's implications.
     */
    public static int findDuplicateSorting(int[] nums) {
        int[] sortedCopy = Arrays.copyOf(nums, nums.length); // O(n) space — violates constraint
        Arrays.sort(sortedCopy);
        for (int index = 1; index < sortedCopy.length; index++) {
            if (sortedCopy[index] == sortedCopy[index - 1]) {
                return sortedCopy[index];
            }
        }
        throw new IllegalArgumentException("No duplicate found — invalid input per problem constraints.");
    }

    /*
     * ------------------------------------------------------------------
     * Approach 3: Hashing-based (HashSet)
     * ------------------------------------------------------------------
     * Core idea: Walk through the array once, inserting each value into a
     * HashSet. The first value that fails to insert (already present) is
     * the duplicate.
     *
     * Data structure / paradigm: Hashing (HashSet for O(1) average lookup).
     *
     * Time Complexity: O(n) average case (HashSet add/contains is O(1)
     *                   amortized).
     * Space Complexity: O(n) — the HashSet grows to hold up to n distinct
     *                   values (violates constraint!).
     *
     * Pros:
     *   - Very fast, simple, and a natural first instinct for "find
     *     duplicate" style problems.
     *   - Does not modify the input array.
     * Cons:
     *   - O(n) extra space violates the constant-space requirement.
     * When to use: This would be my go-to in production code if the O(1)
     * space constraint didn't exist — it's simple, fast, and safe. In an
     * interview, I'd mention this immediately as the "obvious" solution,
     * then pivot to explain why the constraints rule it out, which signals
     * I understood the problem's actual challenge.
     */
    public static int findDuplicateHashing(int[] nums) {
        Set<Integer> seenValues = new HashSet<>(); // O(n) space — violates constraint
        for (int value : nums) {
            if (!seenValues.add(value)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No duplicate found — invalid input per problem constraints.");
    }

    /*
     * ------------------------------------------------------------------
     * Approach 4: Bit Manipulation (Bit-Counting)
     * ------------------------------------------------------------------
     * Core idea: For each bit position (0 to 31), count how many numbers in
     * [1, n] have that bit set (expected count), versus how many numbers in
     * `nums` actually have that bit set (actual count). If actualCount >
     * expectedCount for a bit position, the duplicate number has that bit
     * set; otherwise it doesn't. Reconstruct the answer bit by bit.
     *
     * Data structure / paradigm: Bit manipulation, no extra structures.
     *
     * Time Complexity: O(n log n) — we scan the entire array (n+1 elements)
     *                   once for each of ~log(n) bit positions (32 for int).
     * Space Complexity: O(1) — only a few counter variables per bit pass.
     *
     * Pros:
     *   - Genuinely O(1) space and doesn't modify the array — satisfies
     *     both hard constraints.
     *   - Clever, shows strong bit-manipulation intuition.
     * Cons:
     *   - O(n log n) time is slower than the optimal O(n) approach.
     *   - Harder to explain quickly and more error-prone under interview
     *     pressure (easy to mess up the expected-count formula).
     *   - Less intuitive to a reviewer skimming the code later — worse for
     *     production maintainability.
     * When to use: Good to mention as an alternative that satisfies both
     * constraints, especially if the interviewer wants to see bit-trick
     * fluency. Not my first choice for the "best" answer since Floyd's
     * algorithm is both faster (O(n)) and equally O(1) space.
     */
    public static int findDuplicateBitManipulation(int[] nums) {
        int n = nums.length - 1; // values range over [1, n]
        int answer = 0;

        // Java's int is 32 bits; iterate every bit position.
        for (int bitPosition = 0; bitPosition < 32; bitPosition++) {
            int bitMask = 1 << bitPosition;
            int expectedCountInRange = 0; // count of numbers in [1, n] with this bit set
            int actualCountInArray = 0;   // count of numbers in nums with this bit set

            for (int candidate = 1; candidate <= n; candidate++) {
                if ((candidate & bitMask) != 0) {
                    expectedCountInRange++;
                }
            }
            for (int value : nums) {
                if ((value & bitMask) != 0) {
                    actualCountInArray++;
                }
            }

            // If more numbers in nums have this bit set than expected,
            // the duplicate value must have this bit set (it's being
            // "double counted" due to appearing more than once).
            if (actualCountInArray > expectedCountInRange) {
                answer |= bitMask;
            }
        }
        return answer;
    }

    /*
     * ------------------------------------------------------------------
     * Approach 5: Binary Search on Value Range
     * ------------------------------------------------------------------
     * Core idea: Binary search NOT over array indices, but over the *range
     * of possible values* [1, n]. For a candidate midpoint `mid`, count how
     * many elements in `nums` are <= mid. If that count exceeds `mid`
     * (Pigeonhole Principle), the duplicate lies in [1, mid]; otherwise it
     * lies in (mid, n]. Narrow the search range each iteration.
     *
     * Data structure / paradigm: Binary search (on answer/value space, a
     * classic "binary search on the answer" pattern).
     *
     * Time Complexity: O(n log n) — O(log n) iterations, each doing an O(n)
     *                   linear scan to count elements <= mid.
     * Space Complexity: O(1) — only a handful of counter/pointer variables.
     *
     * Pros:
     *   - Satisfies both hard constraints (no mutation, O(1) space).
     *   - Elegant application of the "binary search on answer" pattern,
     *     which generalizes well to many other problems (a strong signal
     *     of pattern recognition to an interviewer).
     *   - Easier to reason about correctness than bit manipulation.
     * Cons:
     *   - O(n log n) time — slower than the optimal O(n) Floyd's approach.
     * When to use: A very strong second choice. I'd present this if I
     * couldn't recall Floyd's cycle detection, or as a stepping stone in
     * the interview narrative before arriving at the optimal solution.
     */
    public static int findDuplicateBinarySearch(int[] nums) {
        int lowValue = 1;
        int highValue = nums.length - 1; // n

        while (lowValue < highValue) {
            int midValue = lowValue + (highValue - lowValue) / 2;

            int countLessOrEqualToMid = 0;
            for (int value : nums) {
                if (value <= midValue) {
                    countLessOrEqualToMid++;
                }
            }

            // Pigeonhole: if more than midValue numbers are <= midValue,
            // the duplicate must be in the lower half [lowValue, midValue].
            if (countLessOrEqualToMid > midValue) {
                highValue = midValue;
            } else {
                lowValue = midValue + 1;
            }
        }
        return lowValue; // lowValue == highValue, the duplicate value
    }

    /*
     * ------------------------------------------------------------------
     * Approach 6: Floyd's Cycle Detection (Tortoise and Hare) -- OPTIMAL
     * ------------------------------------------------------------------
     * Core idea: Treat `nums` as an implicit linked list / functional graph:
     * for each index i, there's a "next pointer" to index nums[i]. Because
     * values are in [1, n] and there are n+1 indices [0, n], every index
     * maps to a valid next index, and because one value repeats, at least
     * two indices point to the same next index — meaning the "linked list"
     * MUST contain a cycle (it can't be a simple non-cyclic chain, since
     * that would require n+1 distinct nodes forming a path over only n
     * possible values ahead). Finding the duplicate value reduces exactly
     * to finding the entry point of that cycle — the classic "Linked List
     * Cycle II" problem, solved with Floyd's Tortoise and Hare technique.
     *
     * Data structure / paradigm: Two-pointer technique / cycle detection
     * (implicit graph traversal over a functional graph).
     *
     * Time Complexity: O(n) — each phase (finding intersection, finding
     *                  cycle entrance) takes O(n) in the worst case, and
     *                  they run sequentially, so total is O(n) + O(n) = O(n).
     * Space Complexity: O(1) — only two integer pointers (slow, fast) and
     *                   a loop counter; no extra structures at all.
     *
     * Pros:
     *   - Meets BOTH hard constraints optimally: O(n) time, O(1) space, and
     *     zero mutation of the input array.
     *   - This is the theoretically optimal solution for this exact
     *     problem given its constraints.
     * Cons:
     *   - Conceptually the least intuitive approach — requires recognizing
     *     the "array as functional graph / linked list" reduction, which
     *     is a non-obvious insight.
     *   - Easy to make subtle mistakes (e.g., initializing pointers
     *     incorrectly) if the underlying cycle-detection math isn't fully
     *     internalized.
     * When to use: This is the answer to present as the final, production
     * solution in a Google-level interview — it's the fastest and most
     * space-efficient approach that fully respects both constraints.
     */
    public static int findDuplicateFloyd(int[] nums) {
        // Phase 1: Detect that a cycle exists and find ANY meeting point
        // inside the cycle (not necessarily the entrance).
        int slowPointer = nums[0];
        int fastPointer = nums[0];
        do {
            slowPointer = nums[slowPointer];             // move 1 step
            fastPointer = nums[nums[fastPointer]];        // move 2 steps
        } while (slowPointer != fastPointer);

        // Phase 2: Find the entrance to the cycle, which is mathematically
        // guaranteed to be the duplicate value. Reset one pointer to the
        // start; advance both one step at a time until they meet again.
        slowPointer = nums[0];
        while (slowPointer != fastPointer) {
            slowPointer = nums[slowPointer];
            fastPointer = nums[fastPointer];
        }
        return slowPointer;
    }


    /*
     * ========================================================================
     * 6. APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                       | Time       | Space | Best For                          | Limitations                                   |
     * |---------------------------------|------------|-------|-----------------------------------|------------------------------------------------|
     * | 1. Brute Force                  | O(n^2)     | O(1)  | Tiny inputs, correctness oracle   | Unusable at scale                               |
     * | 2. Sorting-based                | O(n log n) | O(n)  | When copy/mutation allowed        | Violates O(1) space (needs defensive copy)      |
     * | 3. Hashing (HashSet)            | O(n)       | O(n)  | General production code, no       | Violates O(1) space constraint                  |
     * |                                  |            |       | space constraint                  |                                                  |
     * | 4. Bit Manipulation              | O(n log n) | O(1)  | Showing bit-trick fluency         | Slower than optimal; harder to explain quickly  |
     * | 5. Binary Search on Value Range  | O(n log n) | O(1)  | Strong fallback if Floyd's        | Slower than optimal O(n) solution               |
     * |                                  |            |       | forgotten; teaches useful pattern |                                                  |
     * | 6. Floyd's Cycle Detection       | O(n)       | O(1)  | THE optimal, production-grade     | Least intuitive; requires graph/linked-list     |
     * |    (Tortoise and Hare)           |            |       | answer for this exact problem     | reduction insight                               |
     */


    /*
     * ========================================================================
     * 7. RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 6 (Floyd's Tortoise and Hare) as my final
     * answer, but I would narrate my way there rather than jumping straight
     * to it, because:
     *
     *   1. Clarity of communication: Starting by naming the HashSet approach
     *      as the "obvious" O(n) solution shows I immediately see the easy
     *      path, and then explicitly identifying *why* it violates the O(1)
     *      space constraint demonstrates I read the constraints carefully.
     *   2. Bridging insight: I'd mention Binary Search on Value Range as a
     *      solution that satisfies both constraints, to show I have more
     *      than one tool — this also serves as a fallback if I stumble on
     *      the Floyd's reduction.
     *   3. Optimality: Floyd's algorithm is strictly better on time (O(n) vs
     *      O(n log n)) while matching everyone else on O(1) space, making it
     *      the objectively best final answer.
     *   4. Interviewer expectations: This exact problem (LeetCode 287) is a
     *      well-known "does the candidate know the linked-list-cycle
     *      reduction" signal question at this level — presenting Floyd's
     *      algorithm confidently, and explaining *why* the array behaves
     *      like a linked list with a cycle, is exactly what's expected for a
     *      strong signal.
     *   5. Coding speed: Once the insight is clear, the code itself is only
     *      ~10 lines and quick to write correctly under time pressure.
     */


    /*
     * ========================================================================
     * 8. DEEP DIVE: OPTIMAL SOLUTION (PRODUCTION-QUALITY)
     * ========================================================================
     */

    /** Sentinel used to make the "not found" failure mode explicit and typed. */
    private static final int NO_DUPLICATE_SENTINEL = -1;

    /**
     * Finds the single duplicated value in {@code nums}, where {@code nums}
     * has length {@code n + 1} and every element lies in the inclusive range
     * {@code [1, n]}. Exactly one distinct value is guaranteed to repeat
     * (though it may repeat more than twice); this method locates and
     * returns that value.
     *
     * <p>Algorithm: Floyd's Tortoise and Hare cycle detection, applied to
     * the array interpreted as a functional graph where each index {@code i}
     * has an implicit edge to index {@code nums[i]}. Because there are n+1
     * indices mapping into only n possible target values, the pigeonhole
     * principle guarantees a cycle exists, and the entrance to that cycle is
     * provably the duplicated value.
     *
     * <p>Time Complexity:  O(n) — two sequential linear-time phases.
     * <p>Space Complexity: O(1) — only two int pointers are used.
     * <p>The input array {@code nums} is never mutated.
     *
     * @param nums array of length n+1 with values in [1, n]; must be
     *             non-null and satisfy the problem's structural guarantees
     * @return the single duplicated value present in {@code nums}
     * @throws IllegalArgumentException if {@code nums} is null or too short
     *         to possibly satisfy the problem's preconditions
     */
    public static int findDuplicateOptimal(int[] nums) {
        // --- Input validation (defensive, production-quality) ---
        if (nums == null) {
            throw new IllegalArgumentException("nums must not be null.");
        }
        if (nums.length < 2) {
            // Need at least n=1, length=2 to have a well-formed instance
            // of this problem (one value in [1,1] appearing twice).
            throw new IllegalArgumentException(
                "nums must have length >= 2 to contain a valid duplicate per problem constraints.");
        }

        final int upperBoundInclusive = nums.length - 1; // this is "n"

        // Defensive range check: every value must lie in [1, n]. This also
        // implicitly protects the array-indexing below (nums[value]) from
        // ever throwing ArrayIndexOutOfBoundsException on malformed input.
        for (int value : nums) {
            if (value < 1 || value > upperBoundInclusive) {
                throw new IllegalArgumentException(
                    "All values in nums must lie within [1, " + upperBoundInclusive + "]. Found: " + value);
            }
        }

        // --- Phase 1: Find a meeting point inside the cycle ---
        // Start both pointers at the "virtual head", index 0's target.
        // (index 0 is always a safe start since values are indices 1..n,
        // and nums[0] is guaranteed to be a valid target index itself.)
        int slowPointer = nums[0];
        int fastPointer = nums[nums[0]];

        while (slowPointer != fastPointer) {
            slowPointer = nums[slowPointer];        // tortoise: 1 step
            fastPointer = nums[nums[fastPointer]];  // hare: 2 steps
        }

        // --- Phase 2: Find the entrance to the cycle ---
        // Mathematical guarantee (standard cycle-detection proof): resetting
        // one pointer to the start and advancing both at the same speed
        // causes them to meet exactly at the cycle's entrance, which
        // corresponds to the duplicated value.
        slowPointer = 0; // restart from the true array start (index 0)
        while (slowPointer != fastPointer) {
            slowPointer = nums[slowPointer];
            fastPointer = nums[fastPointer];
        }

        return slowPointer; // == fastPointer; this is the duplicate value
    }


    /*
     * ========================================================================
     * 9. DRY RUN / TRACE
     * ========================================================================
     *
     * Tracing findDuplicateOptimal() on: nums = [1, 3, 4, 2, 2]
     * (n = 4, indices 0..4)
     *
     * View as a functional graph: index -> nums[index]
     *   0 -> 1
     *   1 -> 3
     *   2 -> 4
     *   3 -> 2
     *   4 -> 2
     *
     * PHASE 1 (find any meeting point):
     *   Initialize: slowPointer = nums[0] = 1
     *               fastPointer = nums[nums[0]] = nums[1] = 3
     *
     *   Iteration 1: slowPointer != fastPointer (1 != 3), so continue.
     *       slowPointer = nums[1] = 3
     *       fastPointer = nums[nums[3]] = nums[2] = 4
     *       State: slowPointer=3, fastPointer=4
     *
     *   Iteration 2: slowPointer != fastPointer (3 != 4), so continue.
     *       slowPointer = nums[3] = 2
     *       fastPointer = nums[nums[4]] = nums[2] = 4
     *       State: slowPointer=2, fastPointer=4
     *
     *   Iteration 3: slowPointer != fastPointer (2 != 4), so continue.
     *       slowPointer = nums[2] = 4
     *       fastPointer = nums[nums[4]] = nums[2] = 4
     *       State: slowPointer=4, fastPointer=4  --> MATCH, exit loop.
     *
     * PHASE 2 (find cycle entrance):
     *   Reset: slowPointer = 0 (array start), fastPointer stays at 4.
     *
     *   Check: slowPointer(0) != fastPointer(4), continue.
     *       slowPointer = nums[0] = 1
     *       fastPointer = nums[4] = 2
     *       State: slowPointer=1, fastPointer=2
     *
     *   Check: slowPointer(1) != fastPointer(2), continue.
     *       slowPointer = nums[1] = 3
     *       fastPointer = nums[2] = 4
     *       State: slowPointer=3, fastPointer=4
     *
     *   Check: slowPointer(3) != fastPointer(4), continue.
     *       slowPointer = nums[3] = 2
     *       fastPointer = nums[4] = 2
     *       State: slowPointer=2, fastPointer=2  --> MATCH, exit loop.
     *
     *   Return slowPointer = 2.
     *
     * Result: 2 — matches expected output for Example 1. Correct!
     */


    /*
     * ========================================================================
     * 10. CLOSING SUMMARY
     * ========================================================================
     *
     * - Six approaches were considered, spanning brute force, sorting,
     *   hashing, bit manipulation, binary search on the value range, and
     *   Floyd's cycle detection.
     * - Only the bit-manipulation, binary-search, and Floyd's-cycle-
     *   detection approaches satisfy BOTH hard constraints (no mutation,
     *   O(1) space); the sorting and hashing approaches are natural but
     *   ultimately disqualified by the space constraint.
     * - Floyd's Tortoise and Hare is the optimal solution: O(n) time,
     *   O(1) space, zero mutation — strictly dominating every other
     *   constraint-satisfying approach on time complexity.
     * - Known assumptions/limitations of the final solution:
     *     * Assumes the input strictly satisfies the stated preconditions
     *       (length n+1, values in [1,n], exactly one distinct duplicate).
     *       Defensive validation is included, but behavior on malformed
     *       input beyond that validation (e.g., multiple distinct
     *       duplicates) is undefined by problem design, not by the
     *       algorithm itself.
     *     * The algorithm relies on 0 being a safe, never-repeated "virtual
     *       head" index — this holds because values are constrained to
     *       [1, n], so index 0 is never itself pointed to by any value
     *       (only indices 1..n can be targets), it works purely as an
     *       entry point into the functional graph, not as a graph value.
     */


    /*
     * ========================================================================
     * 11. FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "What if there could be multiple distinct duplicated values instead
     *     of just one?" — How would the cycle-detection approach need to
     *     change (it wouldn't directly generalize; you'd likely need a
     *     different technique, since the functional-graph-has-exactly-one-
     *     cycle-entrance property depends on there being only one repeated
     *     value).
     * 2. "What if the array could be modified in place — does that open up
     *     a simpler/faster approach?" — Yes, you could use the "negative
     *     marking" technique (flip sign of nums[abs(value)] as a visited
     *     marker) for O(n) time, O(1) space, without needing Floyd's — but
     *     it mutates the array.
     * 3. "Can you find the duplicate using recursion instead of iteration in
     *     the cycle-detection phases? What are the trade-offs?" — Possible,
     *     but adds O(n) call-stack space in the worst case, undermining the
     *     O(1) space guarantee; iterative is preferred here.
     * 4. "How would your solution change if n could be extremely large
     *     (billions), larger than fits in memory as an array?" — Discuss
     *     streaming/external-memory approaches, or bucket-based
     *     approximate counting (e.g., Bloom filters) if exactness could be
     *     relaxed.
     * 5. "Can this be parallelized?" — The bit-manipulation and binary-
     *     search approaches parallelize naturally (independent counting per
     *     bit / per range partition can run concurrently); Floyd's
     *     algorithm is inherently sequential due to pointer-chasing
     *     dependencies, so it doesn't parallelize well.
     * 6. "What if duplicates could include values outside [1, n]?" — The
     *     entire cycle-detection reduction breaks down since it depends on
     *     the [1, n] range mapping cleanly onto valid array indices; you'd
     *     need range validation and possibly a hashing-based approach
     *     instead.
     */


    /*
     * ========================================================================
     * 12. WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Off-by-one in pointer initialization: Some candidates initialize
     *    both slowPointer and fastPointer to nums[0] and then run the
     *    do-while loop identically to the standard "Linked List Cycle II"
     *    solution — which is fine — but others incorrectly initialize
     *    fastPointer to nums[nums[nums[0]]] (three hops instead of two),
     *    breaking the pointer synchronization needed for correctness.
     * 2. Forgetting Phase 2 must restart from a TRUE starting point (index
     *    0), not from wherever slowPointer happens to be after Phase 1.
     *    Restarting from the wrong node breaks the cycle-entrance-finding
     *    guarantee.
     * 3. Assuming HashSet/sorting are acceptable "final" answers without
     *    explicitly acknowledging they violate the O(1) space constraint —
     *    interviewers want to see the constraint recognized and reasoned
     *    about, not silently ignored.
     * 4. Misunderstanding why a cycle must exist at all: candidates who
     *    can't articulate the pigeonhole-principle reasoning (n+1 indices,
     *    n possible target values, so some index must be reached by two
     *    different "predecessors", forcing a cycle) tend to apply Floyd's
     *    algorithm as memorized boilerplate without real understanding,
     *    which falls apart under interviewer follow-up questions.
     */


    /*
     * ========================================================================
     * 13. TEST HARNESS — Cross-validates every approach against each other
     * ========================================================================
     */
    public static void main(String[] args) {
        int[][] testCases = {
            {1, 3, 4, 2, 2},   // Example 1: normal case, expected 2
            {2, 2, 2, 2, 2},   // Example 2: edge case, all duplicates, expected 2
            {1, 1},            // Example 3: boundary case, smallest n, expected 1
            {3, 1, 3, 4, 2},   // Additional: duplicate not at the "obvious" spot, expected 3
            {1, 2, 3, 4, 4}    // Additional: duplicate is the max value, expected 4
        };

        for (int[] testCase : testCases) {
            int bruteForceResult = findDuplicateBruteForce(testCase);
            int sortingResult = findDuplicateSorting(testCase);
            int hashingResult = findDuplicateHashing(testCase);
            int bitManipulationResult = findDuplicateBitManipulation(testCase);
            int binarySearchResult = findDuplicateBinarySearch(testCase);
            int floydResult = findDuplicateFloyd(testCase);
            int optimalResult = findDuplicateOptimal(testCase);

            boolean allAgree = bruteForceResult == sortingResult
                && sortingResult == hashingResult
                && hashingResult == bitManipulationResult
                && bitManipulationResult == binarySearchResult
                && binarySearchResult == floydResult
                && floydResult == optimalResult;

            System.out.println("Input: " + Arrays.toString(testCase));
            System.out.println("  BruteForce=" + bruteForceResult
                + " Sorting=" + sortingResult
                + " Hashing=" + hashingResult
                + " BitManip=" + bitManipulationResult
                + " BinarySearch=" + binarySearchResult
                + " Floyd=" + floydResult
                + " Optimal=" + optimalResult);
            System.out.println("  All approaches agree: " + allAgree);
            System.out.println();

            if (!allAgree) {
                throw new AssertionError("Mismatch detected for input: " + Arrays.toString(testCase));
            }
        }

        System.out.println("All test cases passed across all approaches.");
    }
}
