import java.util.ArrayDeque;
import java.util.Deque;
import java.util.PriorityQueue;
import java.util.TreeMap;

/*
================================================================================
 SECTION 1: RESTATE THE PROBLEM
================================================================================

 Problem (LeetCode 1438 - "Longest Continuous Subarray With Absolute Diff
 Less Than or Equal to Limit"):

 Given an integer array `nums` and an integer `limit`, find the length of the
 longest CONTIGUOUS subarray such that for ANY two elements picked from within
 that subarray, |a - b| <= limit. Equivalently: within the chosen window,
 (max element - min element) <= limit.

 In my own words: I need to find the widest "window" I can slide over the
 array where the spread between the largest and smallest value inside that
 window never exceeds `limit`. The subarray must be contiguous (not a
 subsequence) and non-empty.

 Key constraints / inputs / outputs / assumptions I'm noting up front:
   - Input: int[] nums, int limit (limit >= 0, per LeetCode constraints)
   - Output: a single int -- the length of the longest valid window
   - "Subarray" = contiguous slice, order matters, no reordering allowed
   - The condition is about max-min of the window, not adjacent-pair diffs
   - nums can contain negative numbers, duplicates, and can be any comparable
     integer range within standard 32-bit int bounds
   - The array is guaranteed non-empty per constraints, so an answer >= 1
     always exists

================================================================================
 SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
================================================================================

 1. Q: What is the size range of `nums`? Could it be up to 10^5 or larger?
    A (assumed): Up to 10^5, per LeetCode constraints (1 <= nums.length <= 1e5).
       This rules out anything worse than O(n log n).

 2. Q: What is the range of values in `nums` and of `limit`?
    A (assumed): -1e9 <= nums[i] <= 1e9, 0 <= limit <= 1e9. Values fit in
       a standard `int`; no overflow concerns for max-min since both are
       within int range and their difference still fits in int/long safely.

 3. Q: Can `limit` be 0? What does that mean?
    A (assumed): Yes. limit = 0 means every element in the window must be
       IDENTICAL, since max - min <= 0 forces max == min.

 4. Q: Are duplicate values in `nums` common, and do they need special
    handling?
    A (assumed): Duplicates are allowed and common. They should not break
       the algorithm -- ties in min/max just mean multiple indices share
       the extreme value.

 5. Q: Should I return the length only, or also the actual subarray
    (start/end indices)?
    A (assumed): Return only the length (an int), matching the LeetCode
       signature. I'll note how to extend to returning indices if asked.

 6. Q: Is the array guaranteed non-empty? What if it has exactly 1 element?
    A (assumed): Yes, non-empty (length >= 1). A single-element array always
       returns 1, since a single-element window trivially satisfies
       max - min == 0 <= limit (limit >= 0).

 7. Q: Do I need to worry about concurrency / multiple threads calling this
    on the same array simultaneously?
    A (assumed): No. This is a single-threaded, single-call algorithmic
       problem. I won't add synchronization overhead.

 8. Q: Is there a preference for using only built-in language constructs vs.
    a custom data structure (e.g., is a "balanced BST from scratch" expected)?
    A (assumed): Using standard library structures (ArrayDeque, TreeMap,
       PriorityQueue) is fine and expected in an interview setting; I don't
       need to hand-roll a balanced BST.

================================================================================
 SECTION 3: EXAMPLES & EDGE CASES
================================================================================

 Example 1 (Normal case):
   nums = [8, 2, 4, 7], limit = 4
   - Windows to consider: [8] ok(0), [8,2] diff=6 > 4 invalid,
     [2,4] diff=2 ok, [2,4,7] diff=5 > 4 invalid, [4,7] diff=3 ok
   - Longest valid contiguous window is [2,4] or [4,7], length 2.
   - Expected output: 2

 Example 2 (Larger normal case with a clean long run):
   nums = [10, 1, 2, 4, 7, 2], limit = 5
   - The subarray [2,4,7,2] has max=7, min=2, diff=5 <= 5 -> valid, length 4.
   - Expected output: 4

 Example 3 (Edge case -- limit = 0, forces all-equal windows):
   nums = [4, 2, 2, 2, 4, 4, 2, 2], limit = 0
   - Every element in the window must be identical.
   - Longest run of a single repeated value: indices [1..3] = [2,2,2]
     (length 3) ties with... let's check [4,4] at index 4..5 length 2,
     and [2,2] at end length 2. The longest run of a repeated constant is
     [2,2,2] length 3.
   - Expected output: 3
   - This exercises the "tie-breaking / boundary" dimension: multiple runs
     of different values compete, and we must correctly track the actual
     maximum without letting a later shorter run overwrite a better answer.

 Additional edge cases called out (not full walk-throughs, but noted):
   - nums.length == 1 -> answer is always 1.
   - All elements identical -> answer is nums.length (any limit >= 0 works).
   - limit is very large (>= max-min of whole array) -> answer is
     nums.length (the entire array is one valid window).
   - Strictly increasing or strictly decreasing array with small limit ->
     answer may be as small as 1 or 2 depending on gaps between consecutive
     elements.

================================================================================
 SECTION 4 & 5: ALL POSSIBLE SOLUTIONS (across applicable paradigms)
================================================================================

 Before diving in, a quick PARADIGM SWEEP (explicitly ruling things out is a
 valued interview behavior, not just finding the right approach):

   - Sorting-based: NOT directly applicable. Sorting nums would destroy the
     contiguity/order information that defines a "subarray" -- the problem
     is fundamentally about contiguous index ranges, not about the multiset
     of values. (Sorting appears only *inside* a fixed window as a brute
     force sub-step, not as a top-level strategy.)

   - Hashing-based (HashMap/HashSet for membership): NOT directly
     applicable. Hashing gives O(1) membership/frequency lookups, but this
     problem needs ORDER STATISTICS (current min and max of a sliding
     window), which plain hashing does not provide. An ordered structure
     (TreeMap, heap, or monotonic deque) is required instead.

   - Divide and Conquer: Possible in theory (split array in half, solve
     each half, then handle the case where the optimal window straddles the
     midpoint -- similar in spirit to the classic "maximum subarray"
     divide-and-conquer), but it adds real complexity for handling the
     cross-boundary window and does not beat O(n) or O(n log n). I'm ruling
     it out as impractical for this problem; I mention it only for
     completeness.

   - Greedy: The sliding window technique itself has a greedy flavor (the
     left pointer only ever moves forward, and we never need to reconsider
     a smaller window once the current one is valid), but it's not a
     standalone "greedy choice" algorithm in the classical sense -- it's
     folded into the sliding window approaches below rather than being its
     own separate approach.

   - Dynamic Programming: NOT a natural fit. There's no clean "optimal
     substructure" recurrence here where dp[i] depends on dp[i-1] in a way
     that's simpler than just tracking a sliding window's min/max directly.
     Any DP formulation collapses into the sliding window idea anyway.

   - Tree / Graph traversal: NOT applicable. There is no tree or graph
     structure inherent to this problem (aside from using a balanced BST
     like TreeMap as an *implementation detail* for order statistics, which
     is covered under "Hashing-based alternative" / Approach 2 below).

 Given that sweep, the approaches I'll actually implement are:
   Approach 1: Brute Force (nested loops, track running min/max)
   Approach 2: Sliding Window + Two TreeMaps (ordered multiset)
   Approach 3: Sliding Window + Two Heaps (lazy deletion)
   Approach 4: Binary Search on Answer Length + Sparse Table (range min/max)
   Approach 5: Sliding Window + Two Monotonic Deques  <-- OPTIMAL
*/

class LongestSubarrayAbsDiffLimit {

    /*
    ----------------------------------------------------------------------------
     APPROACH 1: Brute Force (Nested Loops with Running Min/Max)
    ----------------------------------------------------------------------------
     Core idea: For every starting index i, extend the window rightward one
     element at a time, maintaining the running min and max seen so far in
     O(1) per extension. The moment the window becomes invalid (max-min >
     limit), break out early -- extending further from the same start can
     only keep it invalid or worse, since we're just adding more elements.

     Paradigm: Brute force enumeration of all (start, end) pairs, pruned
     with an early-exit once a window breaks.

     Time Complexity: O(n^2) worst case (e.g., strictly sorted array with a
     huge limit forces every inner loop to run to completion).
     Space Complexity: O(1) extra space.

     Pros:
       - Trivial to reason about and verify correctness against.
       - No auxiliary data structures -- easy to code under pressure.
       - Great as a "correctness oracle" for testing optimized approaches.
     Cons:
       - Quadratic time; will TLE on n = 1e5 constraints.
     When to use: Only as a warm-up / sanity-check baseline, or if n is
     guaranteed small (e.g., n <= ~2000). Not acceptable as a final answer
     for the stated constraints.
    */
    static int longestSubarrayBruteForce(int[] nums, int limit) {
        int n = nums.length;
        int bestLength = 0;
        for (int start = 0; start < n; start++) {
            int runningMin = nums[start];
            int runningMax = nums[start];
            for (int end = start; end < n; end++) {
                runningMin = Math.min(runningMin, nums[end]);
                runningMax = Math.max(runningMax, nums[end]);
                if (runningMax - runningMin > limit) {
                    break; // Further extension from `start` cannot help.
                }
                bestLength = Math.max(bestLength, end - start + 1);
            }
        }
        return bestLength;
    }

    /*
    ----------------------------------------------------------------------------
     APPROACH 2: Sliding Window + Two TreeMaps (Ordered Multiset)
    ----------------------------------------------------------------------------
     Core idea: Maintain a classic two-pointer sliding window [left, right].
     Use two TreeMap<Integer, Integer> instances as ordered multisets (value
     -> count) to track every element currently inside the window: one to
     fetch the current minimum (firstKey) and one to fetch the current
     maximum (lastKey) in O(log n). When the window becomes invalid, shrink
     from the left, decrementing counts and removing keys whose count hits
     zero.

     Paradigm: Sliding window + balanced BST for order statistics.

     Time Complexity: O(n log n) -- each element is inserted once and
     removed at most once, each operation costing O(log n).
     Space Complexity: O(n) worst case for the TreeMaps.

     Pros:
       - Clean, well-understood, and easy to explain ("just a sliding
         window with an ordered multiset for min/max").
       - Naturally handles duplicates via the count map.
       - Good "safe" fallback if the optimal deque trick isn't recalled.
     Cons:
       - log n factor is unnecessary given a linear-time alternative exists.
       - Two TreeMaps double the constant-factor overhead vs. a single
         structure.
     When to use: A strong, defensible first pass in an interview when the
     monotonic deque insight hasn't been raised yet, or when the interviewer
     wants to see comfort with ordered map/multiset APIs.
    */
    static int longestSubarrayTreeMap(int[] nums, int limit) {
        TreeMap<Integer, Integer> windowMinTracker = new TreeMap<>();
        TreeMap<Integer, Integer> windowMaxTracker = new TreeMap<>();
        int left = 0;
        int bestLength = 0;

        for (int right = 0; right < nums.length; right++) {
            int currentValue = nums[right];
            windowMinTracker.merge(currentValue, 1, Integer::sum);
            windowMaxTracker.merge(currentValue, 1, Integer::sum);

            // Shrink the window from the left while it's invalid.
            while (windowMaxTracker.lastKey() - windowMinTracker.firstKey() > limit) {
                int leftValue = nums[left];
                decrementOrRemove(windowMinTracker, leftValue);
                decrementOrRemove(windowMaxTracker, leftValue);
                left++;
            }

            bestLength = Math.max(bestLength, right - left + 1);
        }
        return bestLength;
    }

    // Helper: decrement a key's count, removing it entirely if it hits zero.
    private static void decrementOrRemove(TreeMap<Integer, Integer> map, int key) {
        int updatedCount = map.get(key) - 1;
        if (updatedCount == 0) {
            map.remove(key);
        } else {
            map.put(key, updatedCount);
        }
    }

    /*
    ----------------------------------------------------------------------------
     APPROACH 3: Sliding Window + Two Heaps (Lazy Deletion)
    ----------------------------------------------------------------------------
     Core idea: Same two-pointer sliding window skeleton, but instead of
     TreeMaps, use a min-heap and a max-heap of (value, index) pairs. Since
     standard PriorityQueue doesn't support arbitrary removal efficiently,
     we use LAZY DELETION: when shrinking the window, we don't search the
     heap for the stale entry -- we simply pop entries off the top of each
     heap whenever the top's index falls outside the current window bounds.

     Paradigm: Heap / priority queue with lazy deletion (a very common
     interview pattern for "sliding window + extremum" problems).

     Time Complexity: O(n log n) -- each index is pushed once and popped at
     most once from each heap.
     Space Complexity: O(n) for the heaps.

     Pros:
       - Demonstrates the widely-applicable "lazy deletion in a heap"
         pattern, useful across many sliding-window-with-extremum problems.
       - Avoids needing an ordered multiset API; PriorityQueue is very
         commonly known.
     Cons:
       - Same asymptotic complexity as the TreeMap approach but with the
         added subtlety of lazy deletion bookkeeping (index staleness).
       - Slightly trickier to get right than TreeMap (must remember to
         clean BOTH heaps' stale tops before reading the current min/max).
     When to use: Good alternative to TreeMap if you're more comfortable
     with heaps, or if an interviewer specifically wants to see the lazy
     deletion pattern demonstrated.
    */
    static int longestSubarrayTwoHeaps(int[] nums, int limit) {
        // Each entry: {value, index}. Min-heap orders by value ascending;
        // max-heap orders by value descending.
        PriorityQueue<int[]> minHeap = new PriorityQueue<>((a, b) -> a[0] - b[0]);
        PriorityQueue<int[]> maxHeap = new PriorityQueue<>((a, b) -> b[0] - a[0]);

        int left = 0;
        int bestLength = 0;

        for (int right = 0; right < nums.length; right++) {
            minHeap.offer(new int[] { nums[right], right });
            maxHeap.offer(new int[] { nums[right], right });

            // Lazily discard stale entries (index < left) from both tops.
            while (minHeap.peek()[1] < left) {
                minHeap.poll();
            }
            while (maxHeap.peek()[1] < left) {
                maxHeap.poll();
            }

            while (maxHeap.peek()[0] - minHeap.peek()[0] > limit) {
                left++;
                while (minHeap.peek()[1] < left) {
                    minHeap.poll();
                }
                while (maxHeap.peek()[1] < left) {
                    maxHeap.poll();
                }
            }

            bestLength = Math.max(bestLength, right - left + 1);
        }
        return bestLength;
    }

    /*
    ----------------------------------------------------------------------------
     APPROACH 4: Binary Search on Answer Length + Sparse Table (Range Min/Max)
    ----------------------------------------------------------------------------
     Core idea: The predicate "does there EXIST a window of length L that is
     valid?" is monotonic in a practical sense for the purposes of binary
     search over L (if we can't find any valid window of length L, larger
     lengths are checked directly rather than assumed impossible -- so this
     is technically binary searching for the maximum L for which the
     predicate holds, checked via a full O(n) scan per candidate L using
     O(1) range-min/range-max queries from a precomputed sparse table).
     For each candidate length L, slide a fixed-size window of length L
     across the array and use the sparse table to fetch max/min of that
     window in O(1), checking validity in O(n) total per L. Binary search
     over L costs O(log n) iterations.

     Paradigm: Binary search on the answer + advanced data structure
     (sparse table) for O(1) range min/max queries after O(n log n)
     preprocessing.

     Time Complexity: O(n log n) for sparse table construction, plus
     O(n log n) for the binary search phase (O(log n) candidate lengths,
     each verified in O(n)) -> O(n log n) overall.
     Space Complexity: O(n log n) for the sparse table.

     Pros:
       - Showcases range-query preprocessing (sparse table), a broadly
         reusable technique for "immutable array, many range min/max
         queries" problems.
       - Interesting if the interviewer specifically asks "how would you
         solve this with binary search" as a follow-up.
     Cons:
       - Considerably more code and setup than the sliding window
         approaches, for the same O(n log n) complexity -- strictly worse
         constant factor and complexity in practice.
       - Sparse tables assume a static (immutable) array; doesn't extend to
         a streaming/online version of the problem.
     When to use: Rarely the first choice here; valuable mainly as a
     "structurally different" follow-up answer to show breadth, or when the
     interviewer explicitly wants to explore binary-search-on-answer plus
     range query structures.
    */
    static int longestSubarrayBinarySearchSparseTable(int[] nums, int limit) {
        int n = nums.length;
        SparseTable sparseTable = new SparseTable(nums);

        int lo = 1;
        int hi = n;
        int bestLength = 1; // n >= 1 guarantees at least length 1 works.

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (existsValidWindowOfLength(nums, sparseTable, mid, limit)) {
                bestLength = mid;
                lo = mid + 1; // Try to find an even longer valid window.
            } else {
                hi = mid - 1;
            }
        }
        return bestLength;
    }

    // Checks whether ANY window of the given fixed length is valid.
    private static boolean existsValidWindowOfLength(
            int[] nums, SparseTable sparseTable, int length, int limit) {
        for (int start = 0; start + length - 1 < nums.length; start++) {
            int end = start + length - 1;
            int windowMax = sparseTable.queryMax(start, end);
            int windowMin = sparseTable.queryMin(start, end);
            if (windowMax - windowMin <= limit) {
                return true;
            }
        }
        return false;
    }

    // Sparse table supporting O(1) range max/min queries after O(n log n) build.
    private static final class SparseTable {
        private final int[][] maxTable;
        private final int[][] minTable;
        private final int[] log2;

        SparseTable(int[] nums) {
            int n = nums.length;
            log2 = new int[n + 1];
            for (int i = 2; i <= n; i++) {
                log2[i] = log2[i / 2] + 1;
            }
            int levels = log2[n] + 1;
            maxTable = new int[levels][n];
            minTable = new int[levels][n];
            maxTable[0] = nums.clone();
            minTable[0] = nums.clone();

            for (int level = 1; level < levels; level++) {
                int halfSpan = 1 << (level - 1);
                for (int i = 0; i + (1 << level) <= n; i++) {
                    maxTable[level][i] = Math.max(
                            maxTable[level - 1][i],
                            maxTable[level - 1][i + halfSpan]);
                    minTable[level][i] = Math.min(
                            minTable[level - 1][i],
                            minTable[level - 1][i + halfSpan]);
                }
            }
        }

        int queryMax(int left, int right) {
            int level = log2[right - left + 1];
            return Math.max(maxTable[level][left], maxTable[level][right - (1 << level) + 1]);
        }

        int queryMin(int left, int right) {
            int level = log2[right - left + 1];
            return Math.min(minTable[level][left], minTable[level][right - (1 << level) + 1]);
        }
    }

    /*
    ----------------------------------------------------------------------------
     APPROACH 5 (OPTIMAL): Sliding Window + Two Monotonic Deques
    ----------------------------------------------------------------------------
     See the dedicated "Deep Dive" section below for the fully-commented,
     production-quality implementation of this approach.
     A short preview of the core idea:

     Maintain a two-pointer window [left, right] plus TWO monotonic deques
     of INDICES:
       - maxDeque: values are strictly decreasing front-to-back, so the
         front always holds the index of the current window's maximum.
       - minDeque: values are strictly increasing front-to-back, so the
         front always holds the index of the current window's minimum.
     As `right` advances, pop from the back of each deque any index whose
     value is dominated by nums[right] (they can never again be the
     extremum while nums[right] remains in the window), then push `right`.
     If nums[maxDeque.front] - nums[minDeque.front] > limit, advance `left`
     and pop from the FRONT of either deque if its front index has fallen
     out of the window.

     Paradigm: Sliding window + monotonic deque (the same core trick behind
     "sliding window maximum," LeetCode 239).

     Time Complexity: O(n) -- every index is pushed onto each deque exactly
     once and popped at most once (from either end), across the whole run.
     Space Complexity: O(n) worst case for the two deques (e.g., a strictly
     monotonic input array).

     Pros:
       - Truly optimal: linear time, no log factor.
       - No reliance on TreeMap/heap APIs -- just ArrayDeque, very fast in
         practice due to low constant factors.
       - The same "two monotonic deques" pattern generalizes to several
         other sliding-window-extremum problems.
     Cons:
       - The monotonic deque invariant is less immediately obvious than
         TreeMap/heap and is easier to get subtly wrong under pressure
         (e.g., forgetting to pop stale fronts, or using the wrong
         comparison direction for "strictly decreasing/increasing").
     When to use: This is the production / final answer for the stated
     constraints (n up to 1e5+) -- it's strictly better than every other
     approach above in time complexity while using no more than linear
     space.
    */

    /*
    ============================================================================
     SECTION 7: APPROACHES COMPARISON TABLE
    ============================================================================

     | Approach                              | Time       | Space  | Best For                                   | Limitations                                       |
     |----------------------------------------|-----------|--------|--------------------------------------------|----------------------------------------------------|
     | 1. Brute Force                         | O(n^2)     | O(1)   | Small n, correctness oracle for testing     | TLEs on n = 1e5; unusable as final answer          |
     | 2. Sliding Window + Two TreeMaps        | O(n log n) | O(n)   | Safe fallback, clear ordered-multiset story | Unneeded log factor; two structures = overhead     |
     | 3. Sliding Window + Two Heaps (lazy del)| O(n log n) | O(n)   | Showcasing lazy-deletion heap pattern       | Trickier correctness (must clean both heap tops)   |
     | 4. Binary Search + Sparse Table         | O(n log n) | O(n log n) | Demonstrating range-query preprocessing | Most code; static array only; no better than above |
     | 5. Sliding Window + Two Monotonic Deques| O(n)       | O(n)   | Production / final interview answer         | Invariant subtler to reason about under pressure   |

    ============================================================================
     SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
    ============================================================================

     In a real Google interview, I would:
       1. State the brute force O(n^2) approach FIRST, out loud, in ~30
          seconds, to lock in a correct baseline and confirm the problem
          statement with the interviewer (Approach 1).
       2. Immediately note it won't scale to n = 1e5, and propose the
          sliding window idea. If I want a clean, quickly-verifiable
          intermediate step, I'd mention the TreeMap approach (Approach 2)
          as "a sliding window with an ordered multiset for min/max" --
          this is safe, explainable, and correct.
       3. Then PIVOT to the monotonic deque solution (Approach 5) as the
          final, optimal answer, explicitly noting it drops the log n
          factor by replacing the ordered multiset with two monotonic
          deques of indices -- the same trick as "Sliding Window Maximum."

     This mirrors the pattern of presenting a clean, verifiable intermediate
     solution first, then pivoting to the optimal O(n) solution -- it
     demonstrates both safety (I can produce a correct answer quickly) and
     depth (I recognize and can implement the truly optimal structure).

     Approach 5 (monotonic deques) is what I'd actually WRITE as my final
     code, because:
       - It's O(n), strictly better than every alternative.
       - It uses only ArrayDeque<Integer> -- fast to type, no custom
         comparators needed (unlike the heap approach).
       - It's a well-known, teachable pattern that reviewers recognize and
         respect.

    ============================================================================
     SECTION 9: DEEP DIVE -- OPTIMAL SOLUTION (PRODUCTION QUALITY)
    ============================================================================
    */

    /**
     * Returns the length of the longest contiguous subarray of {@code nums}
     * such that the absolute difference between any two elements within
     * that subarray is at most {@code limit}.
     *
     * <p>Implementation notes: uses a sliding window bounded by two
     * monotonic deques of indices -- one tracking the maximum, one tracking
     * the minimum -- so that the current window's max and min are always
     * available in O(1) via the front of each deque.
     *
     * @param nums  the input array; must be non-null and non-empty
     * @param limit the maximum allowed absolute difference within a window;
     *              expected to be non-negative per problem constraints
     * @return the length of the longest valid contiguous subarray
     * @throws IllegalArgumentException if {@code nums} is null or empty
     */
    static int longestSubarray(int[] nums, int limit) {
        // Defensive checks -- production code should never trust its inputs,
        // even if the interview problem statement guarantees them.
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("nums must be non-null and non-empty");
        }

        // maxCandidateIndices: indices whose values form a strictly
        // decreasing sequence front-to-back. The front is always the index
        // of the current window's maximum value.
        Deque<Integer> maxCandidateIndices = new ArrayDeque<>();

        // minCandidateIndices: indices whose values form a strictly
        // increasing sequence front-to-back. The front is always the index
        // of the current window's minimum value.
        Deque<Integer> minCandidateIndices = new ArrayDeque<>();

        int windowStart = 0;
        int longestValidWindowLength = 0;

        for (int windowEnd = 0; windowEnd < nums.length; windowEnd++) {
            int currentValue = nums[windowEnd];

            // Maintain the max deque's strictly-decreasing invariant: any
            // trailing index whose value is <= currentValue can never again
            // be the maximum while currentValue remains in the window, so
            // discard it.
            while (!maxCandidateIndices.isEmpty()
                    && nums[maxCandidateIndices.peekLast()] < currentValue) {
                maxCandidateIndices.pollLast();
            }
            maxCandidateIndices.offerLast(windowEnd);

            // Symmetric maintenance for the min deque's strictly-increasing
            // invariant.
            while (!minCandidateIndices.isEmpty()
                    && nums[minCandidateIndices.peekLast()] > currentValue) {
                minCandidateIndices.pollLast();
            }
            minCandidateIndices.offerLast(windowEnd);

            // Shrink the window from the left while the max-min spread
            // exceeds the allowed limit. Each shrink advances windowStart
            // by exactly one, and we retire any deque front that has just
            // fallen out of the window.
            while (nums[maxCandidateIndices.peekFirst()] - nums[minCandidateIndices.peekFirst()] > limit) {
                windowStart++;
                if (maxCandidateIndices.peekFirst() < windowStart) {
                    maxCandidateIndices.pollFirst();
                }
                if (minCandidateIndices.peekFirst() < windowStart) {
                    minCandidateIndices.pollFirst();
                }
            }

            // The window [windowStart, windowEnd] is now guaranteed valid;
            // record its length if it's the best seen so far.
            int currentWindowLength = windowEnd - windowStart + 1;
            longestValidWindowLength = Math.max(longestValidWindowLength, currentWindowLength);
        }

        return longestValidWindowLength;
    }

    /*
    ============================================================================
     SECTION 10: DRY RUN / TRACE (Optimal Solution)
    ============================================================================

     Tracing longestSubarray(nums = [10, 1, 2, 4, 7, 2], limit = 5):

     Initial: maxCandidateIndices = [], minCandidateIndices = [], windowStart = 0, best = 0

     windowEnd=0, currentValue=10:
       maxDeque: pop none (empty) -> push 0        -> maxDeque = [0]        (values: [10])
       minDeque: pop none (empty) -> push 0        -> minDeque = [0]        (values: [10])
       Check: nums[0]-nums[0] = 10-10 = 0 <= 5 -> no shrink
       windowLength = 0-0+1 = 1 -> best = 1

     windowEnd=1, currentValue=1:
       maxDeque: nums[back]=10, 10 < 1? No -> keep -> push 1 -> maxDeque=[0,1] (values:[10,1])
       minDeque: nums[back]=10, 10 > 1? Yes -> pop 0 -> now empty -> push 1 -> minDeque=[1] (values:[1])
       Check: nums[maxFront=0]=10 - nums[minFront=1]=1 = 9 > 5 -> SHRINK
         windowStart=1; maxDeque front(0) < 1? yes -> pop -> maxDeque=[1] (values:[1])
                        minDeque front(1) < 1? no -> keep -> minDeque=[1]
         Recheck: nums[1]-nums[1] = 1-1 = 0 <= 5 -> stop shrinking
       windowLength = 1-1+1 = 1 -> best stays 1

     windowEnd=2, currentValue=2:
       maxDeque: nums[back]=1, 1<2? yes -> pop 1 -> empty -> push 2 -> maxDeque=[2] (values:[2])
       minDeque: nums[back]=1, 1>2? no -> keep -> push 2 -> minDeque=[1,2] (values:[1,2])
       Check: nums[2]-nums[1] = 2-1 = 1 <= 5 -> no shrink
       windowLength = 2-1+1 = 2 -> best = 2

     windowEnd=3, currentValue=4:
       maxDeque: nums[back]=2, 2<4? yes -> pop 2 -> empty -> push 3 -> maxDeque=[3] (values:[4])
       minDeque: nums[back]=2, 2>4? no -> keep -> push 3 -> minDeque=[1,2,3] (values:[1,2,4])
       Check: nums[3]-nums[1] = 4-1 = 3 <= 5 -> no shrink
       windowLength = 3-1+1 = 3 -> best = 3

     windowEnd=4, currentValue=7:
       maxDeque: nums[back]=4, 4<7? yes -> pop 3 -> empty -> push 4 -> maxDeque=[4] (values:[7])
       minDeque: nums[back]=4, 4>7? no -> keep -> push 4 -> minDeque=[1,2,3,4] (values:[1,2,4,7])
       Check: nums[4]-nums[1] = 7-1 = 6 > 5 -> SHRINK
         windowStart=2; maxFront(4)<2? no; minFront(1)<2? yes -> pop -> minDeque=[2,3,4] (values:[2,4,7])
         Recheck: nums[4]-nums[2] = 7-2 = 5 <= 5 -> stop shrinking
       windowLength = 4-2+1 = 3 -> best stays 3

     windowEnd=5, currentValue=2:
       maxDeque: nums[back]=7, 7<2? no -> keep -> push 5 -> maxDeque=[4,5] (values:[7,2])
       minDeque: nums[back]=7, 7>2? yes -> pop 4 -> back now 4(val4), 4>2? yes -> pop 3
                 -> back now 2(val2), 2>2? no -> keep -> push 5 -> minDeque=[2,5] (values:[2,2])
       Check: nums[maxFront=4]=7 - nums[minFront=2]=2 = 5 <= 5 -> no shrink
       windowLength = 5-2+1 = 4 -> best = 4

     Final answer: 4  (matches Example 2's expected output)

    ============================================================================
     SECTION 11: CLOSING SUMMARY
    ============================================================================

     - Brute force (O(n^2)) is correct and simple but won't scale; useful
       only as a warm-up and as a testing oracle.
     - TreeMap and two-heap approaches both achieve O(n log n) via ordered
       multiset / lazy-deletion-heap order statistics; either is a strong,
       safe "first working optimized solution" to present.
     - Binary search + sparse table also reaches O(n log n) but with more
       code and a static-array assumption -- mainly useful to show
       breadth, not as the primary answer.
     - The two-monotonic-deque approach is optimal at O(n) time / O(n)
       space and is what I'd submit as final code.
     - Known assumptions/limitations of the final solution:
         * Assumes `nums` fits in memory and `limit` is non-negative
           (if negative limits were allowed, the answer would trivially be
           0 for any window with >1 distinct value, but LeetCode guarantees
           limit >= 0).
         * Not thread-safe / not designed for concurrent mutation of `nums`
           mid-computation (not required by the problem).
         * Optimized for a static, in-memory array; does not directly
           extend to an infinite/streaming input without modification
           (see Follow-Up Questions below).

    ============================================================================
     SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
    ============================================================================

     1. "Can you return the actual subarray (start and end indices), not
        just its length?"
        -> Track windowStart/windowEnd whenever longestValidWindowLength
           updates, and return those alongside the length.

     2. "What if the array is a live stream and you can't store the whole
        thing in memory?"
        -> The two-monotonic-deque approach actually adapts reasonably well
           to a streaming/online setting since it only ever needs the
           current window's worth of indices, not the full array history
           -- though you'd need bounded memory guarantees on window size.

     3. "What if instead of `max - min <= limit` you needed the sum of
        absolute pairwise differences to be bounded?"
        -> That's a fundamentally different problem; the monotonic deque
           trick wouldn't directly apply, since pairwise sum isn't
           expressible via just a single running max/min.

     4. "How would you parallelize this across multiple threads or
        machines for a huge array?"
        -> Split into overlapping chunks (overlap by a safe margin),
           compute local answers per chunk in parallel, then handle
           boundary-straddling windows separately and merge.

     5. "What if `limit` could be negative?"
        -> Per constraints it can't, but conceptually a negative limit
           would make every window of size > 1 invalid (since max - min
           >= 0 always for size > 1), so the answer would degenerate to 1.

     6. "Can you solve this using only O(1) extra space (in addition to the
        input array)?"
        -> Not with this class of techniques while remaining O(n) time in
           general; O(1) extra space would likely force back to an O(n^2)-
           style approach, or a much more specialized amortized trick --
           worth discussing trade-offs rather than claiming a magic O(1)
           solution exists.

    ============================================================================
     SECTION 13: WHAT CANDIDATES TYPICALLY MISS
    ============================================================================

     1. Forgetting to check BOTH deques' fronts for staleness after
        advancing windowStart -- if only one deque is cleaned, the other
        can silently report an extremum value that's no longer inside the
        window, producing wrong answers on inputs where the min and max
        indices leave the window at different times (see windowEnd=4 in
        the dry run above, where only minDeque's front needed popping).

     2. Using non-strict comparisons when maintaining the monotonic
        deques (e.g., popping only when strictly less/greater instead of
        less-than-or-equal/greater-than-or-equal, or vice versa) --
        getting this backwards either breaks the monotonic invariant or
        silently drops a legitimate tie-breaking candidate index. The
        safe convention: pop trailing entries that are DOMINATED by (i.e.,
        no better than) the incoming value, so ties in favor of the newer,
        more-recent index are fine to keep only one of.

     3. Off-by-one errors in window length calculation
        (windowEnd - windowStart + 1) or in the shrink condition (using
        >= instead of > against `limit`, which would incorrectly reject
        windows where max - min == limit exactly).

     4. Assuming the window only ever needs to shrink by one step -- it's
        tempting to write `if` instead of `while` when checking whether
        the window is valid, but a single new element can require
        shrinking the window by more than one position in general sliding
        window problems (though for THIS specific problem the amortized
        analysis guarantees at most one shrink per step is common in
        practice, defensively coding with `while` is the correct and safe
        habit).
    */

    /*
    ============================================================================
     TEST HARNESS -- cross-validates all five approaches against each other
     on the examples and edge cases discussed above, plus a randomized
     stress test against the brute-force oracle.
    ============================================================================
    */
    public static void main(String[] args) {
        int[][] testArrays = {
            { 8, 2, 4, 7 },
            { 10, 1, 2, 4, 7, 2 },
            { 4, 2, 2, 2, 4, 4, 2, 2 },
            { 1 },
            { 5, 5, 5, 5 },
        };
        int[] testLimits = { 4, 5, 0, 0, 100 };
        int[] expected = { 2, 4, 3, 1, 4 };

        for (int caseIndex = 0; caseIndex < testArrays.length; caseIndex++) {
            int[] nums = testArrays[caseIndex];
            int limit = testLimits[caseIndex];

            int bruteForceResult = longestSubarrayBruteForce(nums, limit);
            int treeMapResult = longestSubarrayTreeMap(nums, limit);
            int twoHeapsResult = longestSubarrayTwoHeaps(nums, limit);
            int binarySearchResult = longestSubarrayBinarySearchSparseTable(nums, limit);
            int optimalResult = longestSubarray(nums, limit);

            boolean allMatch = bruteForceResult == expected[caseIndex]
                    && treeMapResult == expected[caseIndex]
                    && twoHeapsResult == expected[caseIndex]
                    && binarySearchResult == expected[caseIndex]
                    && optimalResult == expected[caseIndex];

            System.out.printf(
                "Case %d | expected=%d | brute=%d treeMap=%d heaps=%d binSearch=%d optimal=%d | %s%n",
                caseIndex, expected[caseIndex], bruteForceResult, treeMapResult,
                twoHeapsResult, binarySearchResult, optimalResult,
                allMatch ? "PASS" : "FAIL");
        }

        // Randomized cross-validation stress test: brute force is the
        // oracle; every optimized approach must agree with it on every
        // trial.
        java.util.Random random = new java.util.Random(42);
        boolean anyMismatch = false;
        for (int trial = 0; trial < 2000; trial++) {
            int length = 1 + random.nextInt(15);
            int[] nums = new int[length];
            for (int i = 0; i < length; i++) {
                nums[i] = random.nextInt(21) - 10; // values in [-10, 10]
            }
            int limit = random.nextInt(21); // limit in [0, 20]

            int oracle = longestSubarrayBruteForce(nums, limit);
            int optimal = longestSubarray(nums, limit);
            int treeMap = longestSubarrayTreeMap(nums, limit);
            int heaps = longestSubarrayTwoHeaps(nums, limit);
            int binSearch = longestSubarrayBinarySearchSparseTable(nums, limit);

            if (oracle != optimal || oracle != treeMap || oracle != heaps || oracle != binSearch) {
                anyMismatch = true;
                System.out.println("MISMATCH on trial " + trial + ": nums="
                        + java.util.Arrays.toString(nums) + ", limit=" + limit
                        + " -> oracle=" + oracle + " optimal=" + optimal
                        + " treeMap=" + treeMap + " heaps=" + heaps
                        + " binSearch=" + binSearch);
            }
        }
        System.out.println("Randomized stress test (2000 trials): "
                + (anyMismatch ? "FAILURES FOUND" : "ALL PASSED"));
    }
}
