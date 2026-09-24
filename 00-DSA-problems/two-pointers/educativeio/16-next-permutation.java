import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/*
================================================================================
 GOOGLE-STYLE MOCK INTERVIEW WALKTHROUGH
 Problem: Next Permutation  (LeetCode 31)
================================================================================
*/

/*
================================================================================
 SECTION 1: RESTATE THE PROBLEM
================================================================================
 In my own words:

 I'm given an array `nums` of positive integers. Among all possible
 arrangements (permutations) of these numbers, if I imagine listing every
 permutation in strict lexicographic (dictionary) order, I need to find the
 permutation that comes IMMEDIATELY AFTER the current arrangement of `nums`,
 and rearrange `nums` into that arrangement.

 - Input:  an array of positive integers, e.g. [4, 5, 6]
 - Output: no return value expected (or the same array) — the transformation
           happens IN-PLACE, overwriting `nums`.
 - Special case: if `nums` is already the lexicographically LARGEST
           permutation (i.e., sorted in strictly descending order), there is
           no "next" one, so we wrap around to the SMALLEST permutation
           (sorted ascending).
 - Constraint: must run using only O(1) EXTRA space (in-place), which
           immediately rules out approaches that materialize all permutations
           or use auxiliary data structures proportional to n.

 Key assumptions I'll state up front (to be confirmed in clarifying
 questions below):
   - Duplicates may or may not be present in `nums`.
   - The array is non-empty (or I need to handle empty/size-1 gracefully).
   - "In-place" means O(1) auxiliary space; the array itself is mutated.
================================================================================
*/

/*
================================================================================
 SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
================================================================================
 1. Q: What is the maximum size of `nums`? Should I optimize for very large n?
    A (assumed): n can be up to ~10^5, so I should aim for O(n) time.

 2. Q: Can `nums` contain duplicate values (e.g., [1, 1, 5])?
    A (assumed): Yes, duplicates are allowed. The algorithm must still find
       the correct lexicographic successor, treating equal values as
       indistinguishable in ordering but distinguishable as array slots.

 3. Q: Can `nums` contain negative numbers or zero?
    A (assumed): Problem states "positive integers," so all values are >= 1,
       but the algorithm doesn't actually rely on positivity — it only needs
       total ordering (via `<` and `>`), so it would work for any comparable
       integers too.

 4. Q: What should happen if `nums` has length 0 or 1?
    A (assumed): Length 0 or 1 has exactly one permutation, so the array is
       returned unchanged (it's trivially "already at the max," and wrapping
       to the min gives the same array back).

 5. Q: Should the function return a new array, or mutate `nums` in place?
    A (assumed): Mutate in place per the problem statement; no return value
       is required, though I'll have the method return the array reference
       for convenience/testability.

 6. Q: Is extra O(1) space a hard constraint, or just a preference?
    A (assumed): Hard constraint for the final production solution. I will,
       however, present a brute-force approach that violates this for
       completeness/comparison, and explicitly flag that it doesn't meet
       the requirement.

 7. Q: Is this a single-threaded call, or could `nums` be accessed
    concurrently while I'm mutating it?
    A (assumed): Single-threaded, no concurrent access — no synchronization
       needed.

 8. Q: Should I validate input (null array, null elements)?
    A (assumed): I'll add a defensive null-check in the production-quality
       version but assume a well-formed, non-null `int[]` for simplicity in
       exploratory approaches.
================================================================================
*/

/*
================================================================================
 SECTION 3: EXAMPLES & EDGE CASES
================================================================================
 Example 1 (Normal case):
   Input:  [1, 2, 3]
   Output: [1, 3, 2]
   Reasoning: The permutations of {1,2,3} in lex order are:
     123, 132, 213, 231, 312, 321
   The one right after 123 is 132.

 Example 2 (Boundary case — already the maximum permutation):
   Input:  [3, 2, 1]
   Output: [1, 2, 3]
   Reasoning: 321 is the lexicographically largest arrangement, so we wrap
   around to the smallest: 123.

 Example 3 (Tie-breaking / duplicates case):
   Input:  [1, 1, 5]
   Output: [1, 5, 1]
   Reasoning: Permutations of the MULTISET {1,1,5} in lex order:
     1 1 5, 1 5 1, 5 1 1
   Next after "1 1 5" is "1 5 1". This demonstrates that duplicate values
   don't break the algorithm — we just compare values, not positions.

 Example 4 (Edge case — single element / empty):
   Input:  [7]        -> Output: [7]        (only one permutation exists)
   Input:  []         -> Output: []         (nothing to permute)
================================================================================
*/

/*
================================================================================
 SECTION 4 & 5: ALL POSSIBLE APPROACHES
 (Grouped by paradigm; paradigms that don't meaningfully apply are called
  out with a one-line reason instead of a forced implementation.)

 Paradigms NOT applicable, and why:
   - Hashing:               There's no "lookup" or "membership" sub-problem
                             here; ordering, not identity, drives the logic.
   - Dynamic Programming:   No overlapping subproblems or optimal substructure
                             to exploit — this is a direct structural
                             transformation, not an optimization over states.
   - Tree / Graph traversal: No hierarchical or relational structure exists
                             between elements to traverse.
   - Heap / Priority Queue: We don't need repeated extraction of min/max
                             across a dynamic set — the "find successor"
                             step touches a single value, not a running
                             top-k style query.
   - Divide & Conquer:      The problem isn't decomposable into independent
                             subproblems that combine — the key insight
                             (find pivot from the right) is inherently
                             sequential/local to the suffix.
   - Trie / Segment Tree:   No prefix-string or range-query structure is
                             involved.
   - Monotonic Stack/Deque: Not needed as an explicit structure, though the
                             optimal solution DOES rely on an implicit
                             monotonic (non-increasing) suffix property —
                             discussed inline in Approach 3.

 Paradigms that ARE applicable:
   - Brute Force / Naive              -> Approach 1
   - Greedy                            -> Approach 2 (core insight)
   - Two Pointer                       -> Approach 2 (suffix reversal)
   - Binary Search                     -> Approach 3 (successor search)
   - Sorting-based                     -> discussed as part of Approach 1
                                          and as the conceptual basis for why
                                          reversing the suffix is equivalent
                                          to sorting it in Approach 2.
================================================================================
*/

public final class NextPermutation {

    private NextPermutation() {
        // Utility class; not meant to be instantiated.
    }

    /*
    ============================================================================
     APPROACH 1: Brute Force — Generate All Permutations, Sort, Find Next
    ============================================================================
     Core idea:
       Generate every permutation of `nums`, sort them all in lexicographic
       order, locate the index of the current arrangement, and return the
       one immediately after it (wrapping to the first if we're at the end).

     Data structures / paradigm:
       Backtracking (permutation generation) + Sorting.

     Time Complexity:  O(n! * n log(n!))
       - There are n! permutations.
       - Generating them all costs O(n! * n).
       - Sorting n! permutations, each of length n, costs O(n! * log(n!) * n)
         for the comparisons.
     Space Complexity: O(n! * n)
       - We materialize every permutation in memory.

     Subtle correctness note:
       When `nums` contains duplicate values, naive swap-based generation
       produces the SAME arrangement more than once (swapping two equal
       values yields an identical array). These duplicates must be removed
       before searching for "the next" permutation, or the search can land
       on a repeat of the current arrangement instead of the true next
       distinct one. This implementation dedupes via a TreeSet keyed on a
       lexicographic comparator.

     Pros:
       - Conceptually simple; obviously correct; easy to verify against by
         brute-force in unit tests.
     Cons:
       - Factorial time and space — completely impractical beyond n ~ 8-10.
       - VIOLATES the problem's O(1) extra-space constraint outright.
       - Would never be accepted as a final interview answer.

     When to use in practice:
       - Never in production. Only useful as an oracle to validate the
         optimal solution against, on small inputs, during testing.
    ============================================================================
    */
    static int[] bruteForceNextPermutation(int[] nums) {
        if (nums == null || nums.length <= 1) {
            return nums; // Nothing to permute.
        }

        List<int[]> rawPermutations = new ArrayList<>();
        generateAllPermutations(nums.clone(), 0, rawPermutations);

        // IMPORTANT: when `nums` contains duplicate values, the swap-based
        // generator above produces the same arrangement multiple times
        // (e.g. swapping two equal values yields an identical array). We
        // must dedupe before searching for "the next" arrangement, or we'll
        // find a duplicate of the CURRENT permutation instead of the next
        // DISTINCT one. A TreeSet with a lexicographic comparator both
        // sorts and dedupes in one step.
        TreeSet<int[]> distinctSortedPermutations = new TreeSet<>(Arrays::compare);
        distinctSortedPermutations.addAll(rawPermutations);
        List<int[]> allPermutations = new ArrayList<>(distinctSortedPermutations);

        // Find the index matching the current arrangement of `nums`.
        int currentIndex = -1;
        for (int i = 0; i < allPermutations.size(); i++) {
            if (Arrays.equals(allPermutations.get(i), nums)) {
                currentIndex = i;
                break;
            }
        }

        // Wrap around if we're already at the last (largest) permutation.
        int nextIndex = (currentIndex + 1) % allPermutations.size();
        int[] result = allPermutations.get(nextIndex);
        System.arraycopy(result, 0, nums, 0, nums.length);
        return nums;
    }

    // Standard backtracking permutation generator via swapping.
    private static void generateAllPermutations(int[] array, int startIndex, List<int[]> output) {
        if (startIndex == array.length) {
            output.add(array.clone());
            return;
        }
        for (int i = startIndex; i < array.length; i++) {
            swap(array, startIndex, i);
            generateAllPermutations(array, startIndex + 1, output);
            swap(array, startIndex, i); // backtrack
        }
    }

    private static void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    /*
    ============================================================================
     APPROACH 2: Greedy Pivot + Linear Scan Successor + Reverse (OPTIMAL)
    ============================================================================
     Core idea:
       The key structural insight: to find the NEXT permutation, we want to
       make the SMALLEST possible change to the RIGHTMOST possible suffix.

       1. Scan from the right to find the first index `pivotIndex` where
          nums[pivotIndex] < nums[pivotIndex + 1]. This is the rightmost
          position where increasing the value can produce a larger number.
          Everything to the right of pivotIndex is in non-increasing order
          (this is the "implicit monotonic suffix" mentioned earlier).
       2. If no such index exists, the whole array is non-increasing — it's
          the largest permutation — so just reverse the entire array to get
          the smallest permutation and return.
       3. Otherwise, scan from the right again to find the smallest value in
          the suffix that is still LARGER than nums[pivotIndex] (the
          "successor"). Because the suffix is non-increasing, the first
          value from the right that exceeds nums[pivotIndex] is guaranteed
          to be the smallest such value.
       4. Swap nums[pivotIndex] with that successor.
       5. Reverse the suffix (everything after pivotIndex) to put it back
          into ascending (smallest lexicographic) order — reversing works
          because the suffix is still non-increasing after the swap.

     Data structures / paradigm:
       Greedy (choose the rightmost/smallest valid change) + Two Pointer
       (for the reversal step).

     Time Complexity:  O(n)
       - Three linear passes in the worst case: find pivot, find successor,
         reverse suffix. Each is O(n), so overall O(n).
     Space Complexity: O(1)
       - Only a few index variables; all mutation happens in place.

     Pros:
       - Meets the O(1) space constraint exactly.
       - Single pass logic is easy to explain and reason about once the
         insight ("find pivot, find successor, reverse") is understood.
       - Optimal time complexity — cannot do better than O(n) since we may
         need to touch every element (e.g., to detect the fully-descending
         case).
     Cons:
       - The insight isn't obvious the first time you see this problem;
         requires recognizing the "monotonic suffix" property.

     When to use in practice:
       - This is the production-quality answer. Always prefer this.
    ============================================================================
    */
    static int[] nextPermutationOptimal(int[] nums) {
        if (nums == null || nums.length <= 1) {
            return nums;
        }

        int lastIndex = nums.length - 1;

        // Step 1: Find the pivot — rightmost index where nums[i] < nums[i+1].
        int pivotIndex = lastIndex - 1;
        while (pivotIndex >= 0 && nums[pivotIndex] >= nums[pivotIndex + 1]) {
            pivotIndex--;
        }

        // Step 2 & 3: If a pivot exists, find its successor and swap.
        if (pivotIndex >= 0) {
            int successorIndex = lastIndex;
            // Suffix is non-increasing, so the first element from the right
            // that's strictly greater than nums[pivotIndex] is the smallest
            // valid successor.
            while (nums[successorIndex] <= nums[pivotIndex]) {
                successorIndex--;
            }
            swap(nums, pivotIndex, successorIndex);
        }
        // If pivotIndex == -1, the whole array is non-increasing (the
        // largest permutation) — we skip straight to reversing everything,
        // which wraps around to the smallest permutation.

        // Step 4: Reverse the suffix starting right after the pivot.
        reverseSuffix(nums, pivotIndex + 1, lastIndex);

        return nums;
    }

    private static void reverseSuffix(int[] nums, int left, int right) {
        while (left < right) {
            swap(nums, left, right);
            left++;
            right--;
        }
    }

    /*
    ============================================================================
     APPROACH 3: Greedy Pivot + Binary Search Successor + Reverse
    ============================================================================
     Core idea:
       Identical to Approach 2 for finding the pivot and reversing the
       suffix, EXCEPT: since the suffix (from pivotIndex+1 to the end) is
       guaranteed to be sorted in non-increasing order, we can use BINARY
       SEARCH to locate the successor (the rightmost element greater than
       nums[pivotIndex]) instead of a linear scan.

       This doesn't change the asymptotic complexity of the overall
       algorithm (the pivot-finding and reversal steps are still O(n)), but
       it's worth presenting because interviewers often ask "can you use
       binary search here?" — it shows you recognize the sorted-suffix
       property and can exploit it.

     Data structures / paradigm:
       Greedy + Binary Search (over the sorted suffix) + Two Pointer
       (reversal).

     Time Complexity:  O(n)
       - Pivot search: O(n). Binary search for successor: O(log n).
         Reversal: O(n). Dominated by the O(n) steps, so still O(n) overall
         — binary search doesn't help asymptotically here, but demonstrates
         the technique.
     Space Complexity: O(1)
       - Iterative binary search uses only a few index variables.

     Pros:
       - Demonstrates recognition of the sorted-suffix invariant.
       - Good answer to a "can you optimize the successor search?"
         follow-up question.
     Cons:
       - Doesn't actually improve overall complexity (the O(n) pivot scan
         and reversal already dominate) — so it's a "nice to know," not a
         strict improvement. Slightly more code/complexity for no real gain.

     When to use in practice:
       - Rarely needed in production since it offers no real speedup here,
         but valuable to mention verbally as a follow-up optimization to
         show algorithmic range.
    ============================================================================
    */
    static int[] nextPermutationBinarySearchVariant(int[] nums) {
        if (nums == null || nums.length <= 1) {
            return nums;
        }

        int lastIndex = nums.length - 1;

        // Step 1: Find pivot (same as Approach 2).
        int pivotIndex = lastIndex - 1;
        while (pivotIndex >= 0 && nums[pivotIndex] >= nums[pivotIndex + 1]) {
            pivotIndex--;
        }

        if (pivotIndex >= 0) {
            // Step 2: Binary search the non-increasing suffix
            // [pivotIndex + 1, lastIndex] for the rightmost element that is
            // strictly greater than nums[pivotIndex].
            int successorIndex = findSuccessorViaBinarySearch(nums, pivotIndex + 1, lastIndex, nums[pivotIndex]);
            swap(nums, pivotIndex, successorIndex);
        }

        reverseSuffix(nums, pivotIndex + 1, lastIndex);
        return nums;
    }

    // Binary search over a non-increasing range [low, high] to find the
    // rightmost index whose value is strictly greater than `pivotValue`.
    private static int findSuccessorViaBinarySearch(int[] nums, int low, int high, int pivotValue) {
        int resultIndex = low; // The pivot's immediate successor is guaranteed to exist.
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (nums[mid] > pivotValue) {
                // A valid candidate; try to move further right (since the
                // range is non-increasing, larger indices holding a value
                // > pivotValue are still valid and closer to pivotValue).
                resultIndex = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return resultIndex;
    }

    /*
    ============================================================================
     SECTION 7: APPROACHES COMPARISON TABLE
    ============================================================================

     Approach                              | Time              | Space | Best For                                   | Limitations
     ---------------------------------------|-------------------|-------|--------------------------------------------|--------------------------------------------
     1. Brute Force (generate + sort)       | O(n! * n log n!)  | O(n!) | Correctness oracle for tests on tiny n      | Factorial blowup; violates O(1) space rule
     2. Greedy + Linear Successor + Reverse | O(n)              | O(1)  | Production use; the standard interview ans. | Requires recognizing the pivot/suffix insight
     3. Greedy + Binary Search Successor    | O(n)              | O(1)  | Showing binary-search awareness as follow-up| No real asymptotic gain over Approach 2

    ============================================================================
    */

    /*
    ============================================================================
     SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
    ============================================================================
     I would present APPROACH 2 (Greedy Pivot + Linear Scan Successor +
     Reverse) as my primary answer.

     Why:
       - It's the ONLY approach among those considered that satisfies the
         problem's explicit O(1) extra-space, in-place requirement.
       - It runs in O(n) time, which is optimal — any correct algorithm must
         inspect at least the descending suffix in the worst case (e.g., to
         confirm the array is fully descending), so we can't beat O(n).
       - It's fast to code correctly under interview time pressure once the
         three-step structure (find pivot -> find successor -> reverse) is
         internalized, and it's easy to narrate clearly to the interviewer.
       - I would mention Approach 3 (binary search variant) verbally as a
         "here's a further refinement if you're curious" follow-up, since it
         demonstrates range without adding real value to the core solution —
         but I would NOT lead with it, since introducing binary search adds
         code complexity for zero asymptotic benefit here.
       - I would mention Approach 1 only if asked to first describe a naive
         solution, purely to set up the contrast before optimizing.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 9: DEEP DIVE — PRODUCTION-QUALITY OPTIMAL SOLUTION
    ============================================================================
     This is the polished version of Approach 2 I would actually write on a
     whiteboard / shared doc, with defensive checks and full documentation.
    ============================================================================
    */

    /**
     * Rearranges {@code nums} in place into the lexicographically next
     * greater permutation of its elements. If no such permutation exists
     * (i.e., {@code nums} is already the largest possible permutation, sorted
     * in strictly descending order), rearranges it into the lowest possible
     * order (sorted ascending) instead.
     *
     * <p>Algorithm (three linear passes, O(1) extra space):
     * <ol>
     *   <li>Scan from the right to find the "pivot" — the rightmost index
     *       {@code i} such that {@code nums[i] < nums[i + 1]}. Everything
     *       strictly to the right of the pivot is non-increasing.</li>
     *   <li>If a pivot is found, scan from the right again to find the
     *       "successor" — the rightmost value in the suffix that is still
     *       strictly greater than {@code nums[pivot]} — and swap it with the
     *       pivot.</li>
     *   <li>Reverse the suffix after the pivot to restore it to ascending
     *       (smallest lexicographic) order.</li>
     * </ol>
     *
     * @param nums array of integers to permute in place; must not be null
     * @throws IllegalArgumentException if {@code nums} is null
     */
    public static void nextPermutation(int[] nums) {
        if (nums == null) {
            throw new IllegalArgumentException("nums must not be null");
        }

        final int NO_PIVOT_FOUND = -1;

        // Arrays of length 0 or 1 have exactly one permutation; nothing to do.
        if (nums.length <= 1) {
            return;
        }

        int lastIndex = nums.length - 1;

        /*
         * STEP 1: Find the pivot index.
         * We walk from the second-to-last element toward the front, looking
         * for the first place (from the right) where the array "dips" —
         * i.e., where the current element is smaller than the one right
         * after it. Everything after this dip is currently in its highest
         * possible arrangement (non-increasing order) relative to itself.
         */
        int pivotIndex = lastIndex - 1;
        while (pivotIndex >= 0 && nums[pivotIndex] >= nums[pivotIndex + 1]) {
            pivotIndex--;
        }

        /*
         * STEP 2: If we found a valid pivot, find its successor and swap.
         * We want the SMALLEST value in the suffix that is still bigger
         * than nums[pivotIndex], so that the increase at pivotIndex is as
         * small as possible (preserving lexicographic minimality after the
         * change). Because the suffix is non-increasing, scanning from the
         * right and stopping at the first value greater than nums[pivotIndex]
         * gives us exactly that smallest qualifying value.
         */
        if (pivotIndex != NO_PIVOT_FOUND) {
            int successorIndex = lastIndex;
            while (nums[successorIndex] <= nums[pivotIndex]) {
                successorIndex--;
            }
            swapElements(nums, pivotIndex, successorIndex);
        }
        // If pivotIndex == NO_PIVOT_FOUND, nums is fully non-increasing
        // (the max permutation); we skip the swap and fall through to the
        // reversal below, which turns the entire array into ascending order.

        /*
         * STEP 3: Reverse everything after the pivot.
         * The suffix was non-increasing before the swap (and remains so,
         * structurally, aside from the swapped-in value); reversing it
         * yields the smallest possible arrangement of those elements,
         * which is exactly what "next permutation" requires.
         */
        reverseSuffixInPlace(nums, pivotIndex + 1, lastIndex);
    }

    private static void swapElements(int[] nums, int indexA, int indexB) {
        int temp = nums[indexA];
        nums[indexA] = nums[indexB];
        nums[indexB] = temp;
    }

    private static void reverseSuffixInPlace(int[] nums, int left, int right) {
        while (left < right) {
            swapElements(nums, left, right);
            left++;
            right--;
        }
    }

    /*
    ============================================================================
     SECTION 10: DRY RUN / TRACE
    ============================================================================
     Tracing nextPermutation() on input: [1, 3, 5, 4, 2]

     Initial state: nums = [1, 3, 5, 4, 2], lastIndex = 4

     --- STEP 1: Find pivot ---
       pivotIndex starts at lastIndex - 1 = 3
       Check nums[3]=4 vs nums[4]=2: 4 >= 2 -> true, decrement. pivotIndex = 2
       Check nums[2]=5 vs nums[3]=4: 5 >= 4 -> true, decrement. pivotIndex = 1
       Check nums[1]=3 vs nums[2]=5: 3 >= 5 -> false. STOP.
       => pivotIndex = 1 (value 3)

     --- STEP 2: Find successor and swap ---
       successorIndex starts at lastIndex = 4
       nums[4]=2 <= nums[1]=3 -> true, decrement. successorIndex = 3
       nums[3]=4 <= nums[1]=3 -> false. STOP.
       => successorIndex = 3 (value 4)
       Swap nums[1] and nums[3]:
         Before: [1, 3, 5, 4, 2]
         After:  [1, 4, 5, 3, 2]

     --- STEP 3: Reverse suffix after pivot ---
       Reverse range [pivotIndex + 1, lastIndex] = [2, 4]
       Before reversal: [1, 4, 5, 3, 2]  (subarray to reverse: [5, 3, 2])
       left=2, right=4: swap nums[2],nums[4] -> [1, 4, 2, 3, 5]
       left=3, right=3: left == right, loop ends.

     Final result: [1, 4, 2, 3, 5]

     Sanity check: [1,3,5,4,2] -> next permutation should indeed be
     [1,4,2,3,5] under standard lexicographic permutation ordering. Correct.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 11: CLOSING SUMMARY
    ============================================================================
     - Brute force (Approach 1) is correct but factorial in time and space —
       useful only as a correctness oracle for testing, never as a real
       answer, and it explicitly violates the problem's O(1) space
       constraint.
     - The optimal solution (Approach 2) runs in O(n) time and O(1) space by
       exploiting a single structural insight: find the rightmost "dip"
       (pivot), swap in the smallest valid larger value from the
       non-increasing suffix, then reverse that suffix to minimize it.
     - The binary-search variant (Approach 3) is asymptotically equivalent
       to Approach 2 for this specific problem (still O(n) overall because
       pivot-finding and reversal dominate), but is worth mentioning as an
       optimization-aware follow-up.
     - Known assumptions/limitations of the final solution:
         * Assumes nums is non-null (throws IllegalArgumentException
           otherwise); a null array is not a valid "empty" case.
         * Handles duplicate values correctly since comparisons use <, <=,
           >= rather than relying on element uniqueness.
         * Assumes standard 32-bit int range; no overflow concerns since we
           never perform arithmetic on the values themselves, only compare
           and swap them.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
    ============================================================================
     1. "Can you find the PREVIOUS lexicographic permutation instead?"
        -> Mirror the algorithm: find the rightmost index where
           nums[i] > nums[i+1], find the largest value in the suffix smaller
           than nums[i], swap, then reverse the suffix (which will now need
           to become descending, not ascending).

     2. "What if you needed the k-th next permutation efficiently, not just
        the immediate next one?"
        -> A single-swap-and-reverse approach doesn't generalize directly;
           you'd want a factorial-number-system (Lehmer code) approach that
           computes the k-th permutation directly in O(n^2) or O(n log n)
           with a Fenwick tree, rather than calling nextPermutation() k times
           (which would be O(k * n)).

     3. "How would this change if the array were extremely large and stored
        on disk / couldn't fit in memory?"
        -> You'd need to stream from the end backward to find the pivot and
           successor, then perform the reversal via a two-pass
           read-and-rewrite over the on-disk suffix, since random access
           patterns from the end are still required.

     4. "What if elements were not directly comparable primitives, but
        objects requiring a custom comparator?"
        -> Replace direct `<`, `<=`, `>=` comparisons with calls to the
           comparator; logic is otherwise unchanged since it only relies on
           total ordering, not on numeric properties.

     5. "Can you do this concurrently across multiple threads for a huge
        array?"
        -> The pivot-finding and successor-finding steps are inherently
           sequential/dependent (each step needs the previous step's
           result), so meaningful parallelism is limited; at best you could
           parallelize the final suffix reversal across independent
           index-pair swaps.

     6. "What's the relationship between this algorithm and generating all
        permutations in order?"
        -> Calling nextPermutation() repeatedly starting from the sorted
           array generates every permutation exactly once, in lexicographic
           order, in O(n) amortized time per call — useful for enumerating
           permutations without the O(n!) space blowup of Approach 1.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 13: WHAT CANDIDATES TYPICALLY MISS
    ============================================================================
     1. Forgetting the "no pivot found" case: candidates often code the pivot
        scan and successor scan correctly but forget that when the array is
        fully descending, pivotIndex ends at -1, and the reversal step must
        still run (reversing the ENTIRE array) to produce the ascending
        wrap-around result. Skipping this leaves a descending array
        unchanged, which is wrong.

     2. Using strict `<` instead of `<=` in the successor search: since
        duplicates are allowed, the successor scan must skip values EQUAL to
        the pivot too (nums[successorIndex] <= nums[pivotIndex]), not just
        values less than it — otherwise you might swap with an equal value,
        producing an arrangement that isn't actually "next."

     3. Reversing the wrong range: a common off-by-one is reversing
        [pivotIndex, lastIndex] instead of [pivotIndex + 1, lastIndex] —
        accidentally including the pivot itself in the reversal, which
        corrupts the just-placed successor value.

     4. Assuming the suffix must be re-sorted with a full sort: candidates
        who don't spot the "the suffix is already non-increasing" invariant
        sometimes call Arrays.sort() on the suffix (an O(n log n) fix that
        also isn't wrong, but is less efficient and signals they missed the
        cleaner O(n) reversal insight). It's worth explicitly stating this
        invariant out loud to the interviewer to show you see it.
    ============================================================================
    */

    /*
    ============================================================================
     DEMO / TEST HARNESS
    ============================================================================
    */
    public static void main(String[] args) {
        runDemo("Normal case", new int[] {1, 2, 3});
        runDemo("Boundary case (max permutation, wraps to min)", new int[] {3, 2, 1});
        runDemo("Duplicates case", new int[] {1, 1, 5});
        runDemo("Single element", new int[] {7});
        runDemo("Empty array", new int[] {});
        runDemo("Trace example", new int[] {1, 3, 5, 4, 2});

        // Cross-check optimal solution against brute force on small inputs.
        System.out.println("\n--- Cross-checking optimal vs brute force on small permutations ---");
        int[][] smallTestCases = {
            {1, 2, 3}, {3, 2, 1}, {1, 1, 2}, {2, 1, 1}, {1, 3, 2}
        };
        for (int[] testCase : smallTestCases) {
            int[] viaOptimal = nextPermutationViaCopy(testCase, arr -> {
                nextPermutation(arr); // returns void, so wrap to fit InPlaceOp's signature
                return arr;
            });
            int[] viaBrute = nextPermutationViaCopy(testCase, arr -> bruteForceNextPermutation(arr));
            int[] viaBinarySearch = nextPermutationViaCopy(testCase, arr -> nextPermutationBinarySearchVariant(arr));
            boolean allMatch = Arrays.equals(viaOptimal, viaBrute) && Arrays.equals(viaOptimal, viaBinarySearch);
            System.out.printf(
                "Input=%-12s Optimal=%-12s Brute=%-12s BinarySearchVariant=%-12s Match=%b%n",
                Arrays.toString(testCase),
                Arrays.toString(viaOptimal),
                Arrays.toString(viaBrute),
                Arrays.toString(viaBinarySearch),
                allMatch
            );
        }
    }

    private interface InPlaceOp {
        int[] apply(int[] nums);
    }

    private static int[] nextPermutationViaCopy(int[] original, InPlaceOp operation) {
        int[] copy = original.clone();
        operation.apply(copy);
        return copy;
    }

    private static void runDemo(String label, int[] input) {
        int[] copy = input.clone();
        nextPermutation(copy);
        System.out.println(label + ": " + Arrays.toString(input) + " -> " + Arrays.toString(copy));
    }
}
