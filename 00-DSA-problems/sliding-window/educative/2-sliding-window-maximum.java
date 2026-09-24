import java.util.*;

/*
================================================================================
 SLIDING WINDOW MAXIMUM  (LeetCode 239 — Google-tagged, Hard)
 Mock Google Onsite Interview Transcript
================================================================================
*/

class SlidingWindowMaximum {

    /*
    ============================================================================
    SECTION 1: RESTATE THE PROBLEM
    ============================================================================
    In my own words:

    I'm given an integer array `nums` of length n, and a window size `w`.
    A window of exactly `w` contiguous elements slides across the array,
    one index at a time, starting at index 0 and ending when the window's
    right edge reaches the last element. At EVERY position of the window,
    I need to report the maximum value contained in that window.

    - Input:  nums (int[]), w (int, window size)
    - Output: int[] of length (n - w + 1), where result[i] = max(nums[i..i+w-1])
    - Implicit assumption: w <= n and w >= 1. If w == n, there's exactly one
      window and one output value. If w == 1, the output is just nums itself.

    This is fundamentally a "give me a running aggregate (max) over a moving
    range" problem — the classic tension is: recomputing the max from scratch
    at every step is wasteous, so I need a way to maintain "candidate maxima"
    incrementally as the window slides, discarding stale/dominated candidates.
    */

    /*
    ============================================================================
    SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
    ============================================================================
    1. Q: What are the bounds on n (array length) and w (window size)?
       A: Assume n up to 10^5, values fit in a 32-bit int (e.g., -10^4 to 10^4).
          This rules out anything worse than O(n log n) as "too slow" in
          spirit, though I'll still discuss brute force for completeness.

    2. Q: Can w be larger than n, or zero/negative?
       A: Assume 1 <= w <= n. I will still guard against w <= 0 or w > n
          defensively and throw an IllegalArgumentException.

    3. Q: Can nums contain duplicate values?
       A: Yes. Duplicates are common and must be handled correctly — if the
          current maximum value appears again later in the window, that's
          fine, we just need SOME index holding the max value in range.

    4. Q: Can nums contain negative numbers?
       A: Yes, assume no restriction on sign.

    5. Q: What should happen if nums is empty or w == 0?
       A: Return an empty array. I will treat this as a valid edge case,
          not an error, unless the interviewer says otherwise.

    6. Q: Do I need to return the maximum VALUES, or their INDICES?
       A: Values, per the problem statement ("find the maximum value").

    7. Q: Is this a single batch call, or do I need to support this as a
          streaming API (new elements arrive one at a time, "add" then
          "query current max")?
       A: Assume batch (whole array given upfront) for the core solution,
          but I'll mention how the optimal approach extends naturally to
          a streaming/online setting (this often comes up as a follow-up).

    8. Q: Is there concurrency — multiple threads pushing into the same
          window structure?
       A: Assume single-threaded for the core problem; I'll mention
          thread-safety only if asked as a follow-up.
    */

    /*
    ============================================================================
    SECTION 3: EXAMPLES & EDGE CASES
    ============================================================================

    Example 1 (Normal case):
        nums = [1, 3, -1, -3, 5, 3, 6, 7], w = 3
        Windows:
          [1, 3, -1]        -> max = 3
          [3, -1, -3]       -> max = 3
          [-1, -3, 5]       -> max = 5
          [-3, 5, 3]        -> max = 5
          [5, 3, 6]         -> max = 6
          [3, 6, 7]         -> max = 7
        Output: [3, 3, 5, 5, 6, 7]

    Example 2 (Edge case: w == n, single window):
        nums = [4, 2, 9, 1], w = 4
        Only one window: the whole array -> max = 9
        Output: [9]

    Example 3 (Boundary / tie-breaking case: duplicates & a max that "expires"):
        nums = [9, 9, 8, 7, 6], w = 2
        Windows:
          [9, 9] -> max = 9   (tie between two equal values — either index works)
          [9, 8] -> max = 9   (the FIRST 9 is about to leave the window next step)
          [8, 7] -> max = 8
          [7, 6] -> max = 7
        Output: [9, 9, 8, 7]
        This exercises the "expiration" logic: after step 2, the earlier 9
        slides out of range, and my data structure must correctly drop it
        WITHOUT accidentally also dropping the second, still-valid 9.
    */

    /*
    ============================================================================
    SECTION 4-6: ALL POSSIBLE APPROACHES
    ============================================================================

    Paradigms considered:
      - Brute Force              -> APPLICABLE (baseline)
      - Sorting-based            -> NOT meaningfully applicable: sorting each
                                     window destroys positional/expiry info and
                                     costs O(w log w) per window for no benefit
                                     over a straight scan; skipped beyond this note.
      - Hashing                  -> NOT applicable: a hash map doesn't help
                                     find a MAX (no ordering), so skipped.
      - Two Pointer / Sliding
        Window                   -> This IS the shape of the problem; realized
                                     concretely via the monotonic deque approach.
      - Divide & Conquer         -> NOT naturally applicable here (no clean way
                                     to combine "max of two halves" that
                                     respects a moving window boundary better
                                     than existing approaches); skipped.
      - Greedy                   -> Embedded inside the monotonic deque
                                     approach (greedily discard dominated
                                     elements) — covered there.
      - Dynamic Programming      -> NOT applicable: there's no optimal
                                     substructure/overlapping subproblem here
                                     beyond "max of a range", which is better
                                     served by deque/heap/segment tree; skipped.
      - Tree / Graph traversal   -> NOT applicable (no graph structure).
      - Heap / Priority Queue    -> APPLICABLE (with lazy deletion).
      - Binary Search            -> NOT applicable: values aren't sorted and
                                     there's no monotonic predicate to search
                                     over; skipped.
      - Monotonic Stack/Deque    -> APPLICABLE and OPTIMAL for this problem.
      - Segment Tree / Sparse
        Table (advanced)         -> APPLICABLE as a general-purpose alternative,
                                     especially valuable if range-max queries
                                     of ARBITRARY (not just fixed-size sliding)
                                     ranges are needed later.
    */

    // ---------------------------------------------------------------------
    // Approach 1: Brute Force (Naive Scan)
    // ---------------------------------------------------------------------
    /*
     * Core idea: For every window start index, scan all w elements in that
     * window and track the max directly. No auxiliary data structure needed.
     *
     * Paradigm: Direct simulation / nested loop.
     *
     * Time Complexity: O(n * w) — for each of the (n - w + 1) windows, we do
     *                  O(w) work scanning it.
     * Space Complexity: O(1) extra (excluding the O(n - w + 1) output array).
     *
     * Pros:
     *   - Trivial to write correctly under interview pressure; great as a
     *     verified "oracle" to stress-test faster solutions against.
     *   - Zero risk of subtle bugs.
     * Cons:
     *   - Quadratic-ish blowup when w is large (e.g., w ~ n/2) — becomes
     *     O(n^2), too slow for n ~ 10^5.
     * When to use: Only as a warm-up / correctness oracle, or when n and w
     * are both guaranteed tiny. Not acceptable as a final answer in an
     * onsite for the stated constraints.
     */
    public static int[] bruteForce(int[] nums, int windowSize) {
        validateInput(nums, windowSize);
        int n = nums.length;
        if (n == 0) return new int[0];

        int outputLength = n - windowSize + 1;
        int[] result = new int[outputLength];

        for (int windowStart = 0; windowStart < outputLength; windowStart++) {
            int currentMax = Integer.MIN_VALUE;
            for (int offset = 0; offset < windowSize; offset++) {
                currentMax = Math.max(currentMax, nums[windowStart + offset]);
            }
            result[windowStart] = currentMax;
        }
        return result;
    }

    // ---------------------------------------------------------------------
    // Approach 2: Max-Heap (Priority Queue) with Lazy Deletion
    // ---------------------------------------------------------------------
    /*
     * Core idea: Maintain a max-heap of (value, index) pairs for elements
     * seen so far. To get the current window's max, peek the heap top;
     * if that top's index has fallen out of the current window's left
     * boundary, pop it (lazy deletion) and keep peeking until the top is
     * valid. Because heap entries are never actively removed from the
     * middle, we tolerate stale entries and just skip them when they
     * surface at the top.
     *
     * Paradigm: Heap / Priority Queue + lazy deletion.
     *
     * Time Complexity: O(n log n) — each element is pushed once (O(log n))
     *                  and popped at most once (O(log n) amortized).
     * Space Complexity: O(n) — worst case the heap holds all n elements
     *                  before stale ones are lazily popped.
     *
     * Pros:
     *   - Conceptually simple to explain: "keep a max-heap, throw away
     *     anything that's expired."
     *   - Generalizes easily if the problem were relaxed to arbitrary
     *     range-max queries instead of a fixed sliding window.
     * Cons:
     *   - O(n log n) instead of O(n) — strictly worse than the deque
     *     approach for this specific fixed-window problem.
     *   - Slightly fiddly to get the lazy-deletion loop exactly right.
     * When to use: If I've already presented the optimal O(n) deque
     * solution and the interviewer asks "what if the window could shrink
     * and grow arbitrarily, not just slide by one?" — the heap approach
     * (or a balanced BST / TreeMap of counts) adapts more gracefully to
     * that variant than a monotonic deque does.
     */
    public static int[] maxHeapApproach(int[] nums, int windowSize) {
        validateInput(nums, windowSize);
        int n = nums.length;
        if (n == 0) return new int[0];

        int outputLength = n - windowSize + 1;
        int[] result = new int[outputLength];

        // Max-heap ordered by value descending; ties broken arbitrarily.
        PriorityQueue<int[]> maxHeap = new PriorityQueue<>(
                (a, b) -> Integer.compare(b[0], a[0]) // b before a => max-heap on value
        );

        for (int currentIndex = 0; currentIndex < n; currentIndex++) {
            maxHeap.offer(new int[]{nums[currentIndex], currentIndex});

            int windowLeftBoundary = currentIndex - windowSize + 1;

            // Lazily discard heap-top entries whose index has expired
            // (fallen to the left of the current window).
            while (!maxHeap.isEmpty() && maxHeap.peek()[1] < windowLeftBoundary) {
                maxHeap.poll();
            }

            if (windowLeftBoundary >= 0) {
                result[windowLeftBoundary] = maxHeap.peek()[0];
            }
        }
        return result;
    }

    // ---------------------------------------------------------------------
    // Approach 3: Monotonic Deque (OPTIMAL)
    // ---------------------------------------------------------------------
    /*
     * Core idea: Maintain a deque of INDICES such that the corresponding
     * values are in strictly decreasing order from front to back. The
     * front of the deque is always the index of the current window's max.
     *
     * Two greedy pruning rules keep the deque small and always correct:
     *   1. Before inserting the new index, pop from the BACK any index
     *      whose value is <= the new value — those elements can never
     *      again be the max of any future window (the new, later, and
     *      at-least-as-large element dominates them forever). This is the
     *      "greedy discard of dominated candidates" pattern.
     *   2. Before reading the max, pop from the FRONT any index that has
     *      fallen out of the window's left boundary (expired).
     *
     * Paradigm: Monotonic Deque (a specialization of monotonic stack,
     * applied at both ends) — the canonical O(n) technique for
     * "max/min over all fixed-size sliding windows."
     *
     * Time Complexity: O(n) — each index is pushed onto the deque exactly
     *                  once and popped from the deque at most once,
     *                  across the ENTIRE run (amortized analysis), giving
     *                  total work O(n), not per-window O(n).
     * Space Complexity: O(w) — the deque holds at most w indices at once,
     *                  since anything older than w positions has either
     *                  expired or been dominated and evicted.
     *
     * Pros:
     *   - Optimal time complexity, linear in n.
     *   - No comparator overhead, no log factor — just index bookkeeping.
     *   - Naturally extends to a streaming/online setting: each new value
     *     is processed in amortized O(1).
     * Cons:
     *   - Less immediately intuitive than the heap approach; the
     *     "why does popping dominated elements from the back preserve
     *     correctness" argument needs to be stated clearly to convince
     *     an interviewer this isn't accidentally dropping a future max.
     * When to use: This is the production-grade answer for the EXACT
     * problem as stated (fixed-size window, batch or streaming). Use this
     * unless the window's size itself needs to change dynamically, in
     * which case a heap/TreeMap-based structure is more flexible.
     */
    public static int[] monotonicDeque(int[] nums, int windowSize) {
        validateInput(nums, windowSize);
        int n = nums.length;
        if (n == 0) return new int[0];

        int outputLength = n - windowSize + 1;
        int[] result = new int[outputLength];

        // Deque stores INDICES into nums, maintained with strictly
        // decreasing nums-values from front to back.
        Deque<Integer> candidateIndices = new ArrayDeque<>();

        for (int currentIndex = 0; currentIndex < n; currentIndex++) {
            // Rule 1: evict indices that have expired out of the window.
            int windowLeftBoundary = currentIndex - windowSize + 1;
            if (!candidateIndices.isEmpty() && candidateIndices.peekFirst() < windowLeftBoundary) {
                candidateIndices.pollFirst();
            }

            // Rule 2: evict indices from the back whose values are
            // dominated by (i.e., <=) the incoming value — they can never
            // be a future window's max.
            while (!candidateIndices.isEmpty() && nums[candidateIndices.peekLast()] <= nums[currentIndex]) {
                candidateIndices.pollLast();
            }

            candidateIndices.offerLast(currentIndex);

            // Once the first full window has been formed, record the max.
            if (currentIndex >= windowSize - 1) {
                result[windowLeftBoundary] = nums[candidateIndices.peekFirst()];
            }
        }
        return result;
    }

    // ---------------------------------------------------------------------
    // Approach 4: Sparse Table (Advanced Structure — Static Range-Max)
    // ---------------------------------------------------------------------
    /*
     * Core idea: Precompute, for every index i and every power-of-two
     * length 2^k, the max of the range [i, i + 2^k - 1]. Any arbitrary
     * range [l, r] can then be answered in O(1) by combining two
     * overlapping precomputed ranges that together cover [l, r].
     *
     * Paradigm: Sparse Table (a form of range-query preprocessing, related
     * to segment trees but specialized to STATIC arrays and idempotent
     * operations like max/min/gcd).
     *
     * Time Complexity: O(n log n) preprocessing, O(1) per query, so
     *                  O(n log n) total for all (n - w + 1) fixed-size
     *                  window queries.
     * Space Complexity: O(n log n) for the table.
     *
     * Pros:
     *   - Answers ANY range-max query (not just fixed-size sliding
     *     windows) in O(1) after preprocessing — much more general.
     *   - No amortized-analysis argument needed; each query is trivially
     *     O(1).
     * Cons:
     *   - O(n log n) space is overkill for this specific fixed-window
     *     problem — strictly worse than the O(w) space deque approach.
     *   - Only works for STATIC arrays (no updates); a segment tree would
     *     be needed instead if nums could be mutated between queries.
     *   - More code, more room for off-by-one bugs in the log-table setup.
     * When to use: If the interviewer relaxes the problem to "answer many
     * arbitrary range-max queries on a fixed array," this is a strong
     * general-purpose tool. For THIS problem exactly, it's a valid but
     * suboptimal-in-space alternative I'd mention, not lead with.
     */
    public static int[] sparseTableApproach(int[] nums, int windowSize) {
        validateInput(nums, windowSize);
        int n = nums.length;
        if (n == 0) return new int[0];

        int maxLog = Integer.numberOfTrailingZeros(Integer.highestOneBit(Math.max(n, 1))) + 1;
        int[][] sparseTable = new int[maxLog][n];
        sparseTable[0] = Arrays.copyOf(nums, n);

        for (int level = 1; level < maxLog; level++) {
            int rangeLength = 1 << level;
            for (int startIndex = 0; startIndex + rangeLength <= n; startIndex++) {
                int halfLength = 1 << (level - 1);
                sparseTable[level][startIndex] = Math.max(
                        sparseTable[level - 1][startIndex],
                        sparseTable[level - 1][startIndex + halfLength]
                );
            }
        }

        int outputLength = n - windowSize + 1;
        int[] result = new int[outputLength];
        int levelForWindow = Integer.numberOfTrailingZeros(Integer.highestOneBit(windowSize));

        for (int windowStart = 0; windowStart < outputLength; windowStart++) {
            int rightAlignedStart = windowStart + windowSize - (1 << levelForWindow);
            result[windowStart] = Math.max(
                    sparseTable[levelForWindow][windowStart],
                    sparseTable[levelForWindow][rightAlignedStart]
            );
        }
        return result;
    }

    /*
    ============================================================================
    SECTION 7: APPROACHES COMPARISON TABLE
    ============================================================================
    | Approach                | Time         | Space   | Best For                          | Limitations                                   |
    |--------------------------|--------------|---------|------------------------------------|------------------------------------------------|
    | Brute Force              | O(n * w)     | O(1)    | Correctness oracle, tiny inputs    | Quadratic-ish; too slow at scale               |
    | Max-Heap + Lazy Delete   | O(n log n)   | O(n)    | Variable/dynamic window variants   | Log factor unnecessary for fixed window        |
    | Monotonic Deque          | O(n)         | O(w)    | THIS problem, exactly as stated    | Correctness argument less obvious than heap    |
    | Sparse Table             | O(n log n)   | O(n log n) | Arbitrary (non-sliding) range-max queries | Overkill space here; static array only  |
    ============================================================================
    */

    /*
    ============================================================================
    SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
    ============================================================================
    I would present the Monotonic Deque approach (Approach 3) as my final
    answer, but I would NOT jump straight there. My interview plan:

      1. State the brute force O(n*w) approach out loud in ~30 seconds to
         show I understand the problem and have a correctness baseline.
      2. Mention the heap-based O(n log n) approach as an intermediate
         improvement, since it's a very natural first optimization
         ("keep the biggest few candidates around") and demonstrates I can
         reason about lazy deletion.
      3. Then introduce the monotonic deque and explain WHY it's correct:
         an element can be safely discarded from consideration the moment
         a later, equal-or-larger element appears, because it will never
         again be the max of any window while that later element is still
         in range. This is the key insight interviewers want to hear
         articulated, not just coded.
      4. Implement the deque solution cleanly, and dry-run it on an example
         to prove correctness on the spot.

    This ordering (brute force -> heap -> deque) demonstrates full command
    of the problem space and optimization reasoning, not just memorized
    the "trick." It also matches this candidate's established interview
    strategy: state brute force, build a clean intermediate solution, then
    proactively deliver the optimal one.
    */

    /*
    ============================================================================
    SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (production-quality)
    ============================================================================
    See `monotonicDeque` above for the polished, fully-commented
    implementation. Key production-quality decisions already baked in:
      - Explicit input validation via `validateInput` (fail fast on bad w).
      - ArrayDeque instead of LinkedList (no node-object overhead, O(1)
        amortized operations at both ends).
      - Deque stores indices, not values, so we can detect expiry cheaply
        by comparing against `windowLeftBoundary` without a separate map.
      - Clear, named intermediate variables (`windowLeftBoundary`,
        `candidateIndices`) instead of terse single-letter names, for
        readability under interview whiteboard/live-coding conditions.
    */

    /*
    ============================================================================
    SECTION 10: DRY RUN / TRACE
    ============================================================================
    Tracing `monotonicDeque` on nums = [1, 3, -1, -3, 5, 3, 6, 7], windowSize = 3.
    Deque holds INDICES; shown as (index:value) pairs for clarity.

    currentIndex=0, value=1:
        windowLeftBoundary = 0 - 3 + 1 = -2 (no expiry check fires)
        Back-evict: deque empty, nothing to evict.
        Push index 0.  Deque: [(0:1)]
        currentIndex(0) < windowSize-1(2) -> no output yet.

    currentIndex=1, value=3:
        windowLeftBoundary = -1
        Back-evict: nums[0]=1 <= 3 -> pop index 0.  Deque: []
        Push index 1.  Deque: [(1:3)]
        Still < windowSize-1 -> no output yet.

    currentIndex=2, value=-1:
        windowLeftBoundary = 0
        Front-expiry check: front index 1 >= 0, not expired.
        Back-evict: nums[1]=3 <= -1? No (3 > -1) -> stop, no eviction.
        Push index 2.  Deque: [(1:3), (2:-1)]
        currentIndex(2) >= windowSize-1(2) -> output result[0] = nums[front=1] = 3.  ✓ matches expected

    currentIndex=3, value=-3:
        windowLeftBoundary = 1
        Front-expiry: front index 1 >= 1, not expired.
        Back-evict: nums[2]=-1 <= -3? No -> stop.
        Push index 3.  Deque: [(1:3), (2:-1), (3:-3)]
        Output result[1] = nums[front=1] = 3.  ✓ matches expected

    currentIndex=4, value=5:
        windowLeftBoundary = 2
        Front-expiry: front index 1 < 2 -> expired, pop.  Deque: [(2:-1), (3:-3)]
            Re-check new front: index 2 >= 2, not expired. Stop.
        Back-evict: nums[3]=-3 <= 5 -> pop.  nums[2]=-1 <= 5 -> pop.  Deque: []
        Push index 4.  Deque: [(4:5)]
        Output result[2] = nums[front=4] = 5.  ✓ matches expected

    currentIndex=5, value=3:
        windowLeftBoundary = 3
        Front-expiry: front index 4 >= 3, not expired.
        Back-evict: nums[4]=5 <= 3? No -> stop.
        Push index 5.  Deque: [(4:5), (5:3)]
        Output result[3] = nums[front=4] = 5.  ✓ matches expected

    currentIndex=6, value=6:
        windowLeftBoundary = 4
        Front-expiry: front index 4 >= 4, not expired.
        Back-evict: nums[5]=3 <= 6 -> pop.  nums[4]=5 <= 6 -> pop.  Deque: []
        Push index 6.  Deque: [(6:6)]
        Output result[4] = nums[front=6] = 6.  ✓ matches expected

    currentIndex=7, value=7:
        windowLeftBoundary = 5
        Front-expiry: front index 6 >= 5, not expired.
        Back-evict: nums[6]=6 <= 7 -> pop.  Deque: []
        Push index 7.  Deque: [(7:7)]
        Output result[5] = nums[front=7] = 7.  ✓ matches expected

    Final result: [3, 3, 5, 5, 6, 7] — matches Example 1 exactly.
    */

    /*
    ============================================================================
    SECTION 11: CLOSING SUMMARY
    ============================================================================
    - Brute force (O(n*w)) is correct but too slow at scale; useful only as
      an oracle for testing.
    - Heap + lazy deletion (O(n log n)) is a solid intermediate step and the
      more flexible choice if window boundaries can move arbitrarily rather
      than just sliding by one.
    - Monotonic deque (O(n) time, O(w) space) is the optimal and recommended
      solution for the fixed-size sliding window as stated — it is what I'd
      write on the whiteboard as my final answer.
    - Sparse table (O(n log n) preprocessing, O(1) query) is a good answer
      to "what if queries are arbitrary ranges, not just sliding windows,"
      but is not the right tool for this exact problem due to extra space.
    - Known assumption/limitation of the final solution: it assumes a
      static, in-memory array known up front (or elements arriving in a
      simple append-only stream); if elements could be removed from the
      middle of the window arbitrarily (not just expiring off the left),
      the monotonic deque invariant would break and a heap/balanced-BST
      approach would be required instead.
    */

    /*
    ============================================================================
    SECTION 12: FOLLOW-UP QUESTIONS
    ============================================================================
    1. What if the array is a live, infinite stream and I can't store it
       all in memory — can you support this online, one element at a time?
       (Yes — `monotonicDeque`'s per-element logic is already exactly this;
       just emit `result` values as they're computed instead of storing them.)
    2. What if I need the minimum instead of the maximum, or both
       simultaneously? (Mirror the deque logic with the inequality flipped,
       or maintain two deques in parallel.)
    3. What if the window size itself can change dynamically at runtime
       (grow/shrink between queries)? (Monotonic deque invariant breaks;
       switch to a heap with lazy deletion, or a TreeMap<value, count>.)
    4. How would this change if n is up to 10^9 and must be processed in a
       distributed/parallel fashion (e.g., MapReduce-style)? (Partition
       into overlapping chunks of size >= w, solve each chunk locally, and
       stitch boundary windows together.)
    5. Can you support concurrent readers querying "the current window max"
       while a single writer pushes new elements? (Wrap the deque with a
       read-write lock, or use an immutable snapshot-per-update strategy.)
    6. How would you extend this to a 2D matrix with a sliding w x w window
       (max in every w x w submatrix)? (Apply the 1D sliding window max
       row-wise, then column-wise on the row-wise results.)
    */

    /*
    ============================================================================
    SECTION 13: WHAT CANDIDATES TYPICALLY MISS
    ============================================================================
    1. Off-by-one on when the first output is produced: the first valid
       window only exists once currentIndex >= windowSize - 1; candidates
       often either emit early (before the window is full) or emit late
       (skipping the first valid window in the output array).
    2. Popping from the back with strict "<" instead of "<=": if you only
       evict values strictly less than the incoming value (and keep equal
       values), the deque can grow unnecessarily with duplicate maxima and,
       worse, an interviewer can construct a case where the wrong (stale)
       equal-valued duplicate is reported once the fresher one should have
       taken over — using "<=" is what guarantees only one deque entry per
       distinct "current champion" value survives.
    3. Forgetting to check FRONT expiry before reading the max on every
       single iteration (not just once at the start) — the front element
       can expire on ANY step, not only right after a window boundary
       change, and skipping this check produces a stale/wrong max.
    4. Assuming the deque must be re-scanned to fix expired middle entries:
       candidates sometimes try to remove arbitrary expired indices from
       the middle of the deque, not realizing the invariant guarantees
       expired indices can ONLY ever be at the very front, since indices
       are pushed in increasing order.
    */

    /*
    ============================================================================
    HELPER: INPUT VALIDATION
    ============================================================================
    */
    private static void validateInput(int[] nums, int windowSize) {
        if (nums == null) {
            throw new IllegalArgumentException("nums must not be null");
        }
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be positive");
        }
        if (nums.length > 0 && windowSize > nums.length) {
            throw new IllegalArgumentException("windowSize must not exceed nums.length");
        }
    }

    /*
    ============================================================================
    MAIN: ASSERTION-BASED TEST HARNESS + RANDOMIZED STRESS TEST
    ============================================================================
    Following established verification discipline: every optimized approach
    is cross-validated against the brute-force oracle, both on hand-picked
    examples and on randomized stress tests.
    */
    public static void main(String[] args) {
        // --- Hand-verified examples from Section 3 ---
        int[] example1 = {1, 3, -1, -3, 5, 3, 6, 7};
        int[] expected1 = {3, 3, 5, 5, 6, 7};

        int[] example2 = {4, 2, 9, 1};
        int[] expected2 = {9};

        int[] example3 = {9, 9, 8, 7, 6};
        int[] expected3 = {9, 9, 8, 7};

        for (var testCase : new Object[][]{
                {example1, 3, expected1},
                {example2, 4, expected2},
                {example3, 2, expected3}
        }) {
            int[] nums = (int[]) testCase[0];
            int windowSize = (int) testCase[1];
            int[] expected = (int[]) testCase[2];

            assert Arrays.equals(bruteForce(nums, windowSize), expected)
                    : "bruteForce failed on " + Arrays.toString(nums);
            assert Arrays.equals(maxHeapApproach(nums, windowSize), expected)
                    : "maxHeapApproach failed on " + Arrays.toString(nums);
            assert Arrays.equals(monotonicDeque(nums, windowSize), expected)
                    : "monotonicDeque failed on " + Arrays.toString(nums);
            assert Arrays.equals(sparseTableApproach(nums, windowSize), expected)
                    : "sparseTableApproach failed on " + Arrays.toString(nums);
        }

        // --- Edge case: empty array ---
        assert bruteForce(new int[0], 1).length == 0;
        assert monotonicDeque(new int[0], 1).length == 0;

        // --- Randomized stress test: optimal approaches vs. brute-force oracle ---
        Random random = new Random(42);
        for (int trial = 0; trial < 2000; trial++) {
            int n = 1 + random.nextInt(50);
            int[] randomNums = new int[n];
            for (int i = 0; i < n; i++) {
                randomNums[i] = random.nextInt(21) - 10; // values in [-10, 10]
            }
            int windowSize = 1 + random.nextInt(n);

            int[] oracleResult = bruteForce(randomNums, windowSize);
            int[] heapResult = maxHeapApproach(randomNums, windowSize);
            int[] dequeResult = monotonicDeque(randomNums, windowSize);
            int[] sparseResult = sparseTableApproach(randomNums, windowSize);

            assert Arrays.equals(oracleResult, heapResult)
                    : "Mismatch (heap) on " + Arrays.toString(randomNums) + " w=" + windowSize;
            assert Arrays.equals(oracleResult, dequeResult)
                    : "Mismatch (deque) on " + Arrays.toString(randomNums) + " w=" + windowSize;
            assert Arrays.equals(oracleResult, sparseResult)
                    : "Mismatch (sparse) on " + Arrays.toString(randomNums) + " w=" + windowSize;
        }

        System.out.println("All assertions passed (run with -ea to enable assertions).");
        System.out.println("Example 1 result: " + Arrays.toString(monotonicDeque(example1, 3)));
        System.out.println("Example 2 result: " + Arrays.toString(monotonicDeque(example2, 4)));
        System.out.println("Example 3 result: " + Arrays.toString(monotonicDeque(example3, 2)));
    }
}
