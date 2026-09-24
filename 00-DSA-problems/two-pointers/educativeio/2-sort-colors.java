import java.util.*;

/*
 * ============================================================================
 * TABLE OF CONTENTS  (line numbers filled in after final assembly)
 * ============================================================================
 * 01. RESTATE THE PROBLEM ..................................... line 30
 * 02. CLARIFYING QUESTIONS ..................................... line 60
 * 03. EXAMPLES & EDGE CASES .................................... line 111
 * 04. ALL POSSIBLE SOLUTIONS (overview) ........................ line 143
 *     04a. Approach 1: Sorting-Based (Library Sort) ............ line 156
 *     04b. Approach 2: Counting Sort (Array-based, Two-Pass) ... line 200
 *     04c. Approach 3: Hashing-Based (HashMap Counting) ........ line 260
 *     04d. Approach 4: Heap / Priority Queue Sort ............... line 324
 *     04e. Approach 5: Two-Pointer / Dutch National Flag ........ line 376
 * 05. PARADIGMS CONSIDERED BUT SKIPPED ......................... line 472
 * 06. APPROACHES COMPARISON TABLE ............................... line 496
 * 07. RECOMMENDED APPROACH FOR INTERVIEW ........................ line 511
 * 08. DEEP DIVE: OPTIMAL SOLUTION ............................... line 542
 * 09. DRY RUN / TRACE ........................................... line 637
 * 10. CLOSING SUMMARY ........................................... line 683
 * 11. FOLLOW-UP QUESTIONS ........................................ line 711
 * 12. WHAT CANDIDATES TYPICALLY MISS ............................. line 749
 * 13. MAIN METHOD (demo / self-test harness) ..................... line 781
 * ============================================================================
 */

/*
 * ============================================================================
 * SECTION 01: RESTATE THE PROBLEM
 * ============================================================================
 * In my own words:
 *
 *   I'm given an integer array `nums` where every element is one of exactly
 *   three values: 0 (red), 1 (white), or 2 (blue). I need to rearrange the
 *   array IN PLACE so that all 0s come first, followed by all 1s, followed
 *   by all 2s. This is essentially a "3-way partition" / restricted sorting
 *   problem — I'm not sorting arbitrary integers, I'm sorting a fixed
 *   alphabet of 3 categories.
 *
 * Key constraints & assumptions (as stated in the prompt):
 *   - Input: an array `nums` of length n, values restricted to {0, 1, 2}.
 *   - Output: the SAME array, mutated in place, in sorted category order.
 *   - I may NOT call a library sort function (Arrays.sort, Collections.sort).
 *   - Bonus/stretch goal explicitly called out by the interviewer: solve it
 *     in a single pass over the array using O(1) extra space (no counting
 *     arrays, no auxiliary output array).
 *
 * Implicit assumptions I will state out loud and confirm:
 *   - The array is mutable (not an immutable/fixed collection type).
 *   - "In place" means O(1) *auxiliary* space — the output array itself
 *     doesn't count against the space bound.
 *   - Values are guaranteed to be exactly 0, 1, or 2 (no need to validate
 *     or handle out-of-range values, though I'll mention defensive checks).
 * ============================================================================
 */

/*
 * ============================================================================
 * SECTION 02: CLARIFYING QUESTIONS
 * ============================================================================
 * These are questions I would actually ask the interviewer before coding,
 * along with the answers I'll assume if they say "use your best judgment."
 *
 * Q1: What is the expected size of `nums`? Could it be empty, or huge
 *     (millions of elements)?
 *     A1 (assumed): n can range from 0 up to ~10^5 or more; solution must
 *     be O(n) time to be considered acceptable at scale.
 *
 * Q2: Are the values strictly limited to {0, 1, 2}, or could there be
 *     invalid/out-of-range values (e.g., negative numbers, 3+)?
 *     A2 (assumed): Guaranteed to be exactly {0, 1, 2} per problem
 *     statement, but I'll add an optional defensive check in production code.
 *
 * Q3: Should the relative order of equal elements be preserved (i.e., does
 *     this need to be a STABLE sort)?
 *     A3 (assumed): No — since all 0s are identical, all 1s are identical,
 *     and all 2s are identical, "stability" is not observable/meaningful
 *     here. Any rearrangement that groups them correctly is valid.
 *
 * Q4: Can `nums` be null, or have length 0 or 1?
 *     A4 (assumed): Treat null as invalid input (throw or no-op depending
 *     on API contract); length 0 or 1 arrays are already "sorted" — return
 *     immediately.
 *
 * Q5: Do I need to return a new array, or mutate in place and return void?
 *     A5 (assumed): Mutate in place; method signature returns void, matching
 *     the classic LeetCode "Sort Colors" signature.
 *
 * Q6: Is this array accessed concurrently by other threads (do I need
 *     thread safety)?
 *     A6 (assumed): No concurrent access — single-threaded context. I'll
 *     mention that if concurrency were required, we'd need external
 *     synchronization since the algorithm mutates shared state.
 *
 * Q7: Are duplicates expected, and if so, is there any upper bound on how
 *     many of each color there might be?
 *     A7 (assumed): Yes, heavy duplication is the norm (only 3 distinct
 *     values across n elements), no bound other than n itself.
 *
 * Q8: Is minimizing the number of SWAPS important (e.g., if elements were
 *     expensive to move, like large structs), or just asymptotic complexity?
 *     A8 (assumed): Asymptotic time/space complexity is the primary metric;
 *     I'll note that the one-pass solution also happens to minimize swaps
 *     to at most O(n).
 * ============================================================================
 */

/*
 * ============================================================================
 * SECTION 03: EXAMPLES & EDGE CASES
 * ============================================================================
 *
 * Example 1 (Normal case):
 *   Input:  nums = [2, 0, 2, 1, 1, 0]
 *   Output: [0, 0, 1, 1, 2, 2]
 *   Walkthrough: Two 0s, two 1s, two 2s — after partitioning, they should
 *   appear grouped in that exact order.
 *
 * Example 2 (Edge case — empty / trivial array):
 *   Input:  nums = []
 *   Output: []
 *   Walkthrough: Nothing to do; algorithm should handle n = 0 gracefully
 *   without index-out-of-bounds errors. Similarly, nums = [1] should return
 *   [1] unchanged (single element is trivially sorted).
 *
 * Example 3 (Boundary / tie-breaking-like case — all same value, and a
 * case that stresses pointer boundaries):
 *   Input:  nums = [1, 1, 1, 1]
 *   Output: [1, 1, 1, 1]  (unchanged — no 0s or 2s ever move)
 *
 *   Input:  nums = [2, 0]
 *   Output: [0, 2]
 *   Walkthrough: This tiny 2-element case is a great boundary stress test
 *   for the three-pointer algorithm because the "low" and "high" pointers
 *   start adjacent to (or overlapping) the "mid" pointer almost
 *   immediately, which is exactly where off-by-one bugs tend to surface.
 * ============================================================================
 */

/*
 * ============================================================================
 * SECTION 04: ALL POSSIBLE SOLUTIONS (overview)
 * ============================================================================
 * I will walk from the most naive approach to the most optimal, covering:
 *   1. Sorting-based (library sort)               -- baseline / disallowed
 *   2. Counting Sort (array-based, two-pass)       -- brute-force-ish, O(n)
 *   3. Hashing-based (HashMap counting)            -- same idea via HashMap
 *   4. Heap / Priority Queue                        -- overkill but valid
 *   5. Two-Pointer / Dutch National Flag (optimal)  -- one pass, O(1) space
 * ============================================================================
 */

/*
 * ----------------------------------------------------------------------------
 * APPROACH 1: Sorting-Based (Library Sort) -- SECTION 04a
 * ----------------------------------------------------------------------------
 * Core idea (plain English):
 *   Just call a general-purpose comparison sort on the array. Since 0 < 1 < 2
 *   as integers, a standard ascending sort produces exactly the grouping we
 *   want. This is the "obvious" first idea and a good way to open the
 *   discussion, even though the problem statement explicitly forbids it.
 *
 * Paradigm: General-purpose sorting (comparison sort / library call).
 *
 * Time Complexity:  O(n log n) — comparison sorts cannot beat this lower
 *   bound in the general case (Arrays.sort on primitives uses a Dual-Pivot
 *   Quicksort, average O(n log n), worst case O(n^2) for pathological
 *   inputs, though rare in practice for primitives).
 * Space Complexity: O(log n) auxiliary (recursion stack for quicksort-style
 *   sorts) to O(n) if a merge-sort-based sort is used for objects.
 *
 * Pros:
 *   - Trivial to write, hard to get wrong, leverages battle-tested library
 *     code.
 * Cons:
 *   - Explicitly disallowed by the problem statement.
 *   - Asymptotically worse than necessary — ignores the fact that there are
 *     only 3 distinct values, which is exploitable structure.
 *
 * When to use in practice:
 *   - Never for THIS problem (forbidden), but it's the right call in real
 *     production code when you have arbitrary, unbounded-cardinality data
 *     and no special structure to exploit.
 * ----------------------------------------------------------------------------
 */
final class Approach1SortingBased {
    // Included only for completeness / to show the baseline; violates the
    // "no library sort" constraint, so I would say this out loud and move on.
    static void sortColors(int[] nums) {
        if (nums == null) {
            return;
        }
        Arrays.sort(nums); // <-- explicitly disallowed by the problem statement
    }
}

/*
 * ----------------------------------------------------------------------------
 * APPROACH 2: Counting Sort (Array-Based, Two-Pass) -- SECTION 04b
 * ----------------------------------------------------------------------------
 * Core idea (plain English):
 *   Since there are only 3 possible values, count how many 0s, 1s, and 2s
 *   exist in a first pass. Then, in a second pass, overwrite the array:
 *   write that many 0s, then that many 1s, then that many 2s.
 *
 * Paradigm: Counting sort (a non-comparison sort exploiting small,
 *   bounded value range) — this is legitimately a form of "brute force
 *   that exploits problem structure" and does NOT call a library sort.
 *
 * Time Complexity: O(n) — one pass to count (O(n)) + one pass to overwrite
 *   (O(n)) = O(2n) = O(n).
 * Space Complexity: O(1) — only a fixed-size counts array of length 3 is
 *   used; this does not scale with input size, so it is constant extra
 *   space. (Note: this DOES satisfy the O(1) space bonus, but NOT the
 *   "one pass" bonus, since it requires two full traversals.)
 *
 * Pros:
 *   - Simple, easy to explain and verify correctness.
 *   - Already O(n) time and O(1) space — strong solution on its own.
 *   - Naturally generalizes to "sort array with k distinct bounded values"
 *     (k-way counting sort), a genuinely useful real-world pattern.
 * Cons:
 *   - Requires two full passes over the array instead of one.
 *   - Slightly more moving parts (need to track write index across 3
 *     segments) than the single-pass three-pointer trick.
 *
 * When to use in practice:
 *   - Great default choice, and arguably what most engineers would ship —
 *     easy to read, easy to maintain, no subtle pointer invariants.
 *   - Use when code clarity / maintainability outweighs shaving off a
 *     constant-factor second pass.
 * ----------------------------------------------------------------------------
 */
final class Approach2CountingSort {
    static void sortColors(int[] nums) {
        if (nums == null || nums.length < 2) {
            return; // 0 or 1 elements are trivially sorted
        }

        // Pass 1: count occurrences of each color (0, 1, 2).
        int[] colorCounts = new int[3];
        for (int value : nums) {
            colorCounts[value]++;
        }

        // Pass 2: overwrite the array in sorted order using the counts.
        int writeIndex = 0;
        for (int color = 0; color < colorCounts.length; color++) {
            for (int occurrence = 0; occurrence < colorCounts[color]; occurrence++) {
                nums[writeIndex] = color;
                writeIndex++;
            }
        }
    }
}

/*
 * ----------------------------------------------------------------------------
 * APPROACH 3: Hashing-Based (HashMap Counting) -- SECTION 04c
 * ----------------------------------------------------------------------------
 * Core idea (plain English):
 *   Functionally identical to Approach 2, but store the counts in a
 *   HashMap<Integer, Integer> instead of a fixed-size array. This is worth
 *   presenting explicitly because the prompt asks me to cover a
 *   "hashing-based" dimension, and it demonstrates I understand WHEN
 *   hashing helps versus when it's overkill.
 *
 * Paradigm: Hashing / frequency map.
 *
 * Time Complexity: O(n) average — HashMap get/put is O(1) amortized
 *   average case (O(n) worst case with pathological hash collisions,
 *   effectively never observed with Integer keys in the JDK).
 * Space Complexity: O(k) where k = number of distinct values = 3 here,
 *   so effectively O(1), but conceptually O(k) in general.
 *
 * Pros:
 *   - Generalizes trivially if the "alphabet" of categories were unknown
 *     or much larger / non-contiguous (e.g., colors were arbitrary
 *     strings or large sparse integers instead of {0,1,2}).
 * Cons:
 *   - Pure overhead here: boxing/unboxing Integers, hashing overhead, and
 *     worse cache locality than a 3-element primitive array — strictly
 *     worse constant factors than Approach 2 for this specific problem.
 *   - Signals to the interviewer that I might not notice when a simpler
 *     data structure (a plain array of size 3) is sufficient.
 *
 * When to use in practice:
 *   - NOT for this problem — I would explicitly say "since the category
 *     count is fixed and tiny (3), I'd use a primitive array instead of a
 *     HashMap." I'm including this mainly to show I understand the
 *     hashing paradigm and its trade-offs, and to explicitly justify
 *     rejecting it here.
 *   - DO use this pattern for the generalized version: "group elements by
 *     an arbitrary/large key" (e.g., grouping by category strings).
 * ----------------------------------------------------------------------------
 */
final class Approach3HashingBased {
    static void sortColors(int[] nums) {
        if (nums == null || nums.length < 2) {
            return;
        }

        // Frequency map keyed by color value -> count of occurrences.
        Map<Integer, Integer> colorFrequency = new HashMap<>();
        for (int value : nums) {
            colorFrequency.merge(value, 1, Integer::sum);
        }

        // Overwrite in ascending key order (0, then 1, then 2).
        int writeIndex = 0;
        for (int color = 0; color <= 2; color++) {
            int occurrences = colorFrequency.getOrDefault(color, 0);
            for (int i = 0; i < occurrences; i++) {
                nums[writeIndex] = color;
                writeIndex++;
            }
        }
    }
}

/*
 * ----------------------------------------------------------------------------
 * APPROACH 4: Heap / Priority Queue Sort -- SECTION 04d
 * ----------------------------------------------------------------------------
 * Core idea (plain English):
 *   Push every element into a min-heap, then pop elements off in ascending
 *   order and write them back into the array. This is a generic sorting
 *   technique (heapsort-via-PriorityQueue) that works for ANY comparable
 *   values, not just our 3-value alphabet.
 *
 * Paradigm: Heap / priority queue.
 *
 * Time Complexity: O(n log n) — n insertions at O(log n) each, plus n
 *   extractions at O(log n) each.
 * Space Complexity: O(n) — the heap itself stores all n elements
 *   (auxiliary structure, not in-place).
 *
 * Pros:
 *   - General-purpose, works even if we didn't know the value range ahead
 *     of time, or if "colors" could be arbitrary comparable objects.
 * Cons:
 *   - Strictly worse than Approaches 2, 3, and 5 on both time and space
 *     for THIS problem — it ignores the small, bounded value-range
 *     structure entirely.
 *   - Not in-place (violates O(1) space goal).
 *
 * When to use in practice:
 *   - Essentially never for this specific problem. I'd mention it mainly
 *     to demonstrate breadth, then immediately explain why it's dominated
 *     by the other approaches here. It becomes relevant for problems like
 *     "merge k sorted lists" or "top-k elements," not bounded 3-way sorts.
 * ----------------------------------------------------------------------------
 */
final class Approach4HeapBased {
    static void sortColors(int[] nums) {
        if (nums == null || nums.length < 2) {
            return;
        }

        PriorityQueue<Integer> minHeap = new PriorityQueue<>(nums.length);
        for (int value : nums) {
            minHeap.offer(value);
        }

        int writeIndex = 0;
        while (!minHeap.isEmpty()) {
            nums[writeIndex] = minHeap.poll();
            writeIndex++;
        }
    }
}

/*
 * ----------------------------------------------------------------------------
 * APPROACH 5 (OPTIMAL): Two-Pointer / Dutch National Flag -- SECTION 04e
 * ----------------------------------------------------------------------------
 * Core idea (plain English):
 *   Maintain THREE pointers/regions while making a single left-to-right
 *   scan with a "current" pointer `mid`:
 *     - Everything strictly before `low`   is known to be all 0s.
 *     - Everything from `low` to `mid - 1` is known to be all 1s.
 *     - Everything strictly after `high`   is known to be all 2s.
 *     - Everything from `mid` to `high` is UNEXPLORED.
 *   At each step, look at nums[mid]:
 *     - If it's 0: swap it with nums[low], then advance both low and mid
 *       (the swapped-in value at mid is now known to be a 1 or was already
 *       processed, safe to advance mid too).
 *     - If it's 1: it's already in the correct region; just advance mid.
 *     - If it's 2: swap it with nums[high], then decrement high WITHOUT
 *       advancing mid (the value swapped in from nums[high] is
 *       unexplored and must still be classified).
 *   The scan ends when mid > high — at that point everything is
 *   classified into its correct region.
 *
 *   This is the classic "Dutch National Flag" algorithm, named by Edsger
 *   Dijkstra after the three horizontal bands of the Dutch flag
 *   (red/white/blue) — which is a nice, memorable way to reference it by
 *   name in an interview.
 *
 * Paradigm: Two/three-pointer in-place partitioning (a specialized,
 *   3-way variant of the classic Lomuto/Hoare partition scheme used in
 *   quicksort).
 *
 * Time Complexity: O(n) — `mid` only ever moves forward or stays; `high`
 *   only ever moves backward. The region between `mid` and `high` shrinks
 *   by at least 1 on every iteration, so the loop runs at most n times.
 *   Each iteration does O(1) work (a comparison and at most one swap).
 * Space Complexity: O(1) — only a constant number of index variables
 *   (low, mid, high) are used; sorting happens via in-place swaps.
 *
 * Pros:
 *   - Meets BOTH bonus requirements simultaneously: single pass AND
 *     O(1) auxiliary space — strictly optimal on both axes.
 *   - No library sort call, satisfies the explicit constraint.
 *   - Minimizes swaps (each element is swapped at most a constant number
 *     of times).
 * Cons:
 *   - More subtle to implement correctly than counting sort — the "don't
 *     advance mid after swapping with high" rule is a classic off-by-one
 *     trap (explained further in Section 12).
 *   - Slightly harder to generalize beyond exactly 3 categories (counting
 *     sort generalizes to k categories far more naturally).
 *
 * When to use in practice:
 *   - This is the canonical interview-optimal answer for "Sort Colors" /
 *     3-way partitioning problems, and the same technique underlies
 *     3-way quicksort partitioning for arrays with many duplicate keys
 *     (Bentley-McIlroy 3-way partitioning), so it's a genuinely reusable
 *     real-world pattern, not just a party trick for this one problem.
 * ----------------------------------------------------------------------------
 */
final class Approach5DutchNationalFlag {
    static void sortColors(int[] nums) {
        if (nums == null || nums.length < 2) {
            return;
        }

        int low = 0;               // boundary: [0, low)      is all 0s
        int mid = 0;                // scanning cursor: [low, mid) is all 1s
        int high = nums.length - 1; // boundary: (high, n)     is all 2s

        while (mid <= high) {
            switch (nums[mid]) {
                case 0 -> {
                    swap(nums, low, mid);
                    low++;
                    mid++; // safe to advance: value now at mid came from the
                           // "1s region" (or was itself just placed), already classified
                }
                case 1 -> mid++; // already in correct place, just move on
                case 2 -> {
                    swap(nums, mid, high);
                    high--; // do NOT advance mid: the newly swapped-in value at
                            // mid is unexplored and must still be classified
                }
                default -> throw new IllegalArgumentException(
                        "Value out of expected range {0,1,2}: " + nums[mid]);
            }
        }
    }

    private static void swap(int[] nums, int indexA, int indexB) {
        int temp = nums[indexA];
        nums[indexA] = nums[indexB];
        nums[indexB] = temp;
    }
}

/*
 * ============================================================================
 * SECTION 05: PARADIGMS CONSIDERED BUT SKIPPED
 * ============================================================================
 * - Divide & Conquer: Could shoehorn a merge-sort-style split/merge, but
 *   it would be strictly worse (O(n log n) time, O(n) space) with no
 *   upside — the problem has no recursive substructure worth exploiting.
 * - Dynamic Programming: No overlapping subproblems or optimal
 *   substructure to exploit; this is a partitioning/counting problem, not
 *   an optimization-over-choices problem. Not applicable.
 * - Tree / Graph Traversal: There's no tree/graph structure in the input;
 *   not applicable.
 * - Binary Search: No sorted search space to binary search over before
 *   the array is sorted, and the output isn't a single index/value lookup.
 *   Not applicable.
 * - Monotonic Stack / Deque: These solve "next greater/smaller element"
 *   style problems; there's no such relationship to track here. Not
 *   applicable.
 * - Trie / Segment Tree: These structures shine for prefix queries or
 *   range queries; there is no such query pattern in this problem. Not
 *   applicable.
 * ============================================================================
 */

/*
 * ============================================================================
 * SECTION 06: APPROACHES COMPARISON TABLE
 * ============================================================================
 *
 * | Approach                          | Time       | Space | Best For                                   | Limitations                                    |
 * |------------------------------------|-----------|-------|--------------------------------------------|-------------------------------------------------|
 * | 1. Sorting-Based (library sort)   | O(n log n)| O(log n)-O(n) | Quick baseline discussion only      | Forbidden by problem; ignores 3-value structure |
 * | 2. Counting Sort (array)          | O(n)       | O(1)  | Clear, maintainable, production-friendly  | Requires two passes, not "one pass"             |
 * | 3. Hashing-Based (HashMap)        | O(n) avg   | O(k)~O(1) | Generalizing to arbitrary/unknown categories | Overkill here; boxing + hashing overhead     |
 * | 4. Heap / Priority Queue          | O(n log n)| O(n)  | Generic comparable sorting, unrelated problems | Not in-place; asymptotically dominated     |
 * | 5. Dutch National Flag (2-3 ptr)  | O(n)       | O(1)  | THE interview-optimal answer; real single pass | Slightly trickier pointer invariants        |
 * ============================================================================
 */

/*
 * ============================================================================
 * SECTION 07: RECOMMENDED APPROACH FOR INTERVIEW
 * ============================================================================
 * I would present Approach 5 (Dutch National Flag / three-pointer
 * partitioning) as my final solution, for these reasons:
 *
 *   1. Optimality: it is simultaneously O(n) time AND O(1) space in a
 *      SINGLE pass — it directly satisfies the interviewer's explicit
 *      "bonus" ask, so presenting anything less leaves points on the
 *      table.
 *   2. Interviewer expectations: "Sort Colors" is a well-known problem
 *      specifically designed to test whether a candidate knows the Dutch
 *      National Flag technique; interviewers are listening for this by
 *      name and for the three-pointer invariant reasoning.
 *   3. Coding speed: once the invariant is understood, the implementation
 *      is ~15 lines and has no tricky data structures — fast to write
 *      and easy to trace on a whiteboard.
 *   4. Clarity: I can explain correctness rigorously via the loop
 *      invariant on regions [0,low), [low,mid), unexplored, (high,n) —
 *      this is exactly the kind of invariant-driven reasoning interviewers
 *      want to hear articulated out loud.
 *
 * I would still MENTION Approach 2 (counting sort) first as a stepping
 * stone — it's correct, O(n)/O(1), and easy to derive — and explicitly
 * frame Approach 5 as "now let's push it to a true single pass," since
 * narrating the progression from a good solution to the optimal one is
 * good interview signal.
 * ============================================================================
 */

/*
 * ============================================================================
 * SECTION 08: DEEP DIVE - OPTIMAL SOLUTION (production-quality)
 * ============================================================================
 * Fully documented, defensive, production-grade version of the Dutch
 * National Flag solution. This is the version I'd actually type in the
 * final minutes of the interview.
 * ============================================================================
 */
final class OptimalSortColors {

    // Named constants instead of magic numbers 0/1/2 -- improves readability
    // and makes intent explicit at every call site.
    private static final int RED = 0;
    private static final int WHITE = 1;
    private static final int BLUE = 2;

    /**
     * Sorts {@code nums} in place so that all 0s (red) come before all 1s
     * (white), which come before all 2s (blue).
     *
     * <p>Implements the Dutch National Flag algorithm: a single
     * left-to-right scan using three index pointers that partition the
     * array into four logical regions:
     * <pre>
     *   [0, low)      -> known RED   (0)
     *   [low, mid)    -> known WHITE (1)
     *   [mid, high]   -> UNEXPLORED
     *   (high, n-1]   -> known BLUE  (2)
     * </pre>
     *
     * @param nums array containing only values in {0, 1, 2}; mutated in place.
     * @throws IllegalArgumentException if any element is outside {0, 1, 2}.
     */
    static void sortColors(int[] nums) {
        // Defensive guard: production code should never trust its inputs
        // blindly, even if the interview problem guarantees well-formed data.
        if (nums == null) {
            throw new IllegalArgumentException("nums must not be null");
        }
        if (nums.length < 2) {
            return; // 0 or 1 elements: already trivially sorted, nothing to do
        }

        int low = 0;                 // next position to place a RED (0)
        int mid = 0;                 // current element under inspection
        int high = nums.length - 1;  // next position (from the right) to place a BLUE (2)

        // Invariant maintained at the top of every loop iteration:
        //   nums[0 .. low)     == RED   (fully classified)
        //   nums[low .. mid)   == WHITE (fully classified)
        //   nums[mid .. high]  == unclassified / unexplored
        //   nums[high+1 .. n)  == BLUE  (fully classified)
        while (mid <= high) {
            int currentValue = nums[mid];

            if (currentValue == RED) {
                swap(nums, low, mid);
                // The value now at `mid` came from the WHITE region we've
                // already fully classified (or is the same RED value if
                // low == mid), so it's safe to advance both pointers.
                low++;
                mid++;
            } else if (currentValue == WHITE) {
                // Already in its correct region; simply widen the WHITE
                // region by moving the scanning cursor forward.
                mid++;
            } else if (currentValue == BLUE) {
                swap(nums, mid, high);
                // Crucially, do NOT advance `mid` here: the element just
                // swapped in from `high` is unexplored and must still be
                // classified on the NEXT iteration. Only shrink the
                // unexplored region from the right by decrementing `high`.
                high--;
            } else {
                // Defensive check: fail loudly rather than silently
                // corrupting the array on malformed input.
                throw new IllegalArgumentException(
                        "Unexpected value " + currentValue + " at index " + mid
                                + "; expected one of {0, 1, 2}");
            }
        }
    }

    /** Swaps the elements at the two given indices within {@code array}. */
    private static void swap(int[] array, int indexA, int indexB) {
        if (indexA == indexB) {
            return; // micro-optimization: avoid a pointless self-swap
        }
        int temp = array[indexA];
        array[indexA] = array[indexB];
        array[indexB] = temp;
    }
}

/*
 * ============================================================================
 * SECTION 09: DRY RUN / TRACE
 * ============================================================================
 * Tracing OptimalSortColors.sortColors on Example 1:
 *   nums = [2, 0, 2, 1, 1, 0]   (indices: 0 1 2 3 4 5)
 *
 * Initial state: low = 0, mid = 0, high = 5
 *
 * Iteration 1: mid(0) <= high(5). nums[mid] = nums[0] = 2 (BLUE)
 *   -> swap(nums, mid=0, high=5): nums = [0, 0, 2, 1, 1, 2]
 *   -> high-- => high = 4   (mid stays at 0)
 *   State: low=0, mid=0, high=4, nums=[0, 0, 2, 1, 1, 2]
 *
 * Iteration 2: mid(0) <= high(4). nums[mid] = nums[0] = 0 (RED)
 *   -> swap(nums, low=0, mid=0): no-op (same index), nums unchanged
 *   -> low++ => low = 1, mid++ => mid = 1
 *   State: low=1, mid=1, high=4, nums=[0, 0, 2, 1, 1, 2]
 *
 * Iteration 3: mid(1) <= high(4). nums[mid] = nums[1] = 0 (RED)
 *   -> swap(nums, low=1, mid=1): no-op
 *   -> low++ => low = 2, mid++ => mid = 2
 *   State: low=2, mid=2, high=4, nums=[0, 0, 2, 1, 1, 2]
 *
 * Iteration 4: mid(2) <= high(4). nums[mid] = nums[2] = 2 (BLUE)
 *   -> swap(nums, mid=2, high=4): nums = [0, 0, 1, 1, 2, 2]
 *   -> high-- => high = 3   (mid stays at 2)
 *   State: low=2, mid=2, high=3, nums=[0, 0, 1, 1, 2, 2]
 *
 * Iteration 5: mid(2) <= high(3). nums[mid] = nums[2] = 1 (WHITE)
 *   -> mid++ => mid = 3
 *   State: low=2, mid=3, high=3, nums=[0, 0, 1, 1, 2, 2]
 *
 * Iteration 6: mid(3) <= high(3). nums[mid] = nums[3] = 1 (WHITE)
 *   -> mid++ => mid = 4
 *   State: low=2, mid=4, high=3, nums=[0, 0, 1, 1, 2, 2]
 *
 * Loop condition check: mid(4) <= high(3)? FALSE -> loop terminates.
 *
 * Final array: [0, 0, 1, 1, 2, 2]  ✅ matches expected output from
 * Example 1 in Section 03. Total iterations: 6 (== n), total swaps: 2
 * effective swaps (the other two were harmless no-ops on equal indices),
 * confirming O(n) time and O(1) auxiliary space.
 * ============================================================================
 */

/*
 * ============================================================================
 * SECTION 10: CLOSING SUMMARY
 * ============================================================================
 * - Approach 1 (library sort) is the fastest to write but is explicitly
 *   forbidden and ignores the bounded-value structure of the problem.
 * - Approach 2 (counting sort, array) is a strong, simple, production-
 *   friendly O(n)/O(1) solution but needs two passes.
 * - Approach 3 (HashMap counting) is functionally equivalent to Approach 2
 *   but adds unnecessary overhead for a fixed, tiny category count; useful
 *   only if the category set were large/unknown.
 * - Approach 4 (heap) is a generic O(n log n)/O(n) sort, dominated by
 *   every other approach here; useful only for genuinely unrelated
 *   problems (top-k, merging sorted streams, etc.).
 * - Approach 5 (Dutch National Flag) is the optimal, single-pass, O(1)-
 *   space solution and is what I would submit as my final answer.
 *
 * Known limitations / assumptions of the final solution:
 *   - Assumes values are exactly in {0, 1, 2}; a defensive exception is
 *     thrown otherwise rather than silently corrupting data.
 *   - Not thread-safe: concurrent mutation of `nums` during the sort would
 *     produce undefined behavior; external synchronization would be
 *     required in a concurrent context.
 *   - Not a stable sort in the traditional sense, but since all elements
 *     within a color are indistinguishable, stability is a non-issue here.
 * ============================================================================
 */

/*
 * ============================================================================
 * SECTION 11: FOLLOW-UP QUESTIONS
 * ============================================================================
 * 1. "What if there were k distinct colors instead of exactly 3?" ->
 *    Discuss generalizing to Approach 2's k-way counting sort (still
 *    O(n) time, O(k) space), since the three-pointer trick doesn't
 *    generalize cleanly beyond 3 categories without recursion/multiple
 *    passes.
 * 2. "What if the array is extremely large and doesn't fit in memory
 *    (external sorting)?" -> Discuss chunked/streaming counting sort:
 *    count in one streaming pass, then write results back in a second
 *    streaming pass, since counts alone (3 integers) fit trivially in
 *    memory regardless of n.
 * 3. "What if `nums` were a linked list instead of an array?" -> The
 *    three-pointer swap-based approach doesn't translate directly (no O(1)
 *    random access/swap); instead, discuss building three separate
 *    sub-lists (red/white/blue) in one pass and concatenating them —
 *    O(n) time, O(1) extra space (just pointers, reusing existing nodes).
 * 4. "How would you parallelize this for a multi-core / distributed
 *    setting?" -> Each worker counts colors in its chunk in parallel
 *    (map phase), counts are summed (reduce phase), then a second
 *    parallel pass writes each chunk's elements to their globally
 *    computed offset ranges (this is essentially parallel counting sort).
 * 5. "Can you prove the three-pointer algorithm terminates and is
 *    correct?" -> Walk through the loop invariant from Section 08/09:
 *    the unexplored region [mid, high] strictly shrinks by at least 1
 *    every iteration (mid++ in two of three branches, high-- in the
 *    third), guaranteeing termination in at most n steps; the invariant
 *    on the four regions guarantees correctness at termination.
 * 6. "What if we needed the ORIGINAL relative order preserved within each
 *    color (a stability requirement) for some reason?" -> Explain that
 *    Approach 2/3 (counting + rewrite in original relative order per
 *    bucket) can be made stable trivially, but Approach 5's swap-based
 *    approach is NOT stable — swaps can reorder equal elements.
 * ============================================================================
 */

/*
 * ============================================================================
 * SECTION 12: WHAT CANDIDATES TYPICALLY MISS
 * ============================================================================
 * 1. Advancing `mid` after a swap with `high`: This is THE classic bug.
 *    When nums[mid] == 2, the value swapped in from nums[high] has NOT
 *    been classified yet — advancing `mid` in this branch would skip
 *    inspecting it entirely, silently producing an incorrectly sorted
 *    array (often only visible on inputs with consecutive 2s near the
 *    end, e.g. [2, 2, 0]).
 * 2. Off-by-one on the loop condition: using `mid < high` instead of
 *    `mid <= high` causes the algorithm to stop one element early and
 *    leave the last unexplored element unclassified.
 * 3. Forgetting the `low == mid` no-op case: when swapping nums[low] and
 *    nums[mid] where low == mid, some candidates write swap logic that
 *    assumes the two indices are always different, which is harmless
 *    here (self-swap is a no-op) but worth calling out explicitly to show
 *    awareness — and can matter if swap() has side effects in other
 *    contexts.
 * 4. Confusing this with a STABLE sort requirement: candidates sometimes
 *    over-engineer a stability-preserving version when the problem never
 *    asked for one (since all elements of the same color are identical,
 *    "stability" is meaningless here) — a good candidate proactively
 *    clarifies this rather than assuming.
 * 5. Not handling n = 0 or n = 1 explicitly: while the three-pointer loop
 *    technically handles these correctly (loop body simply never
 *    executes, or high starts at -1), explicitly calling out these edge
 *    cases during the interview signals rigor even if no special-case
 *    code branch is strictly required.
 * ============================================================================
 */

/*
 * ============================================================================
 * SECTION 13: MAIN METHOD (demo / self-test harness)
 * ============================================================================
 * Public entry point that exercises all five approaches against the
 * examples from Section 03, printing results for manual verification.
 * ============================================================================
 */
public class SortColorsInterviewPrep {

    public static void main(String[] args) {
        int[][] testCases = {
                {2, 0, 2, 1, 1, 0},   // Example 1: normal case
                {},                    // Example 2: empty array edge case
                {1},                   // Example 2b: single-element edge case
                {1, 1, 1, 1},          // Example 3a: all identical values
                {2, 0}                 // Example 3b: boundary/pointer-stress case
        };

        for (int[] testCase : testCases) {
            System.out.println("Input: " + Arrays.toString(testCase));

            runAndPrint("Approach 1 (Sorting-Based)", testCase, Approach1SortingBased::sortColors);
            runAndPrint("Approach 2 (Counting Sort)", testCase, Approach2CountingSort::sortColors);
            runAndPrint("Approach 3 (Hashing-Based)", testCase, Approach3HashingBased::sortColors);
            runAndPrint("Approach 4 (Heap-Based)", testCase, Approach4HeapBased::sortColors);
            runAndPrint("Approach 5 (Dutch Nat'l Flag)", testCase, Approach5DutchNationalFlag::sortColors);
            runAndPrint("Optimal (Production Version)", testCase, OptimalSortColors::sortColors);

            System.out.println("----------------------------------------------------");
        }
    }

    /** Copies the input, runs the given sort implementation, and prints the result. */
    private static void runAndPrint(String label, int[] original, java.util.function.Consumer<int[]> sortFn) {
        int[] copy = Arrays.copyOf(original, original.length);
        sortFn.accept(copy);
        System.out.println("  " + label + " -> " + Arrays.toString(copy));
    }
}
