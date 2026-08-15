import java.util.*;

/*
 * =====================================================================================
 * SECTION 1: RESTATE THE PROBLEM
 * =====================================================================================
 *
 * We are given an array `sticks` of positive integers, where sticks[i] is the length
 * of the i-th stick. We may repeatedly pick any two sticks and connect them; the cost
 * of connecting two sticks is the SUM of their current lengths, and the result is a
 * new single stick whose length equals that sum (it goes back into the pool). We keep
 * doing this until only one stick remains. We want the MINIMUM total cost across all
 * connection operations to reduce the whole array down to one stick.
 *
 * Key observations before coding anything:
 *   - Every connection reduces the stick count by exactly 1, so for n sticks we always
 *     perform exactly (n - 1) connections (0 connections if n == 1).
 *   - A stick's length contributes to the total cost once for every connection it
 *     participates in (directly, or indirectly once it has been merged into a bigger
 *     stick). This is structurally identical to building an optimal merge tree — i.e.
 *     it's the same greedy structure as Huffman coding / optimal file merge problems.
 *   - Because cost only depends on the multiset of lengths (order sticks appear in the
 *     input array is irrelevant), this is fundamentally a "always combine the two
 *     cheapest available piles" greedy problem.
 *
 * Constraints given:
 *   1 <= sticks.length <= 10^3
 *   1 <= sticks[i]      <= 10^3
 *
 * Inputs:  int[] sticks (n between 1 and 1000, each value between 1 and 1000)
 * Outputs: a single long/int representing the minimum total connection cost.
 *
 * Assumptions to validate with the interviewer (see Section 2): sticks.length can be 1
 * (cost 0), the array is not sorted, and we only care about total cost, not the actual
 * sequence of merges performed (though we could reconstruct it if asked).
 */

/*
 * =====================================================================================
 * SECTION 2: CLARIFYING QUESTIONS (asked to interviewer, with assumed answers)
 * =====================================================================================
 *
 * 1. Q: Can `sticks` be empty (length 0)?
 *    A (assumed): No — constraints say length >= 1, so we don't need to handle the
 *       empty-array case, but I will defensively guard against it anyway.
 *
 * 2. Q: If there is only one stick, what should the output be?
 *    A (assumed): 0 — no connections are needed, so no cost is incurred.
 *
 * 3. Q: Can stick lengths repeat (duplicates)?
 *    A (assumed): Yes, duplicates are allowed and should be handled naturally by the
 *       algorithm (no special-casing needed since we work with a multiset of lengths).
 *
 * 4. Q: Do we need to return the actual sequence/order of merges, or just the total
 *       minimum cost?
 *    A (assumed): Just the total minimum cost, as a single integer/long value.
 *
 * 5. Q: What are the bounds on n and stick length — could we overflow a 32-bit int
 *       when summing costs?
 *    A (assumed): n <= 1000, lengths <= 1000, so max total cost is bounded by roughly
 *       n * max_length * log(n) which comfortably fits in a 32-bit int (well under
 *       2^31 - 1), but I'll accumulate in a `long` anyway as a defensive habit in case
 *       constraints are relaxed later — costs nothing and removes an entire class of
 *       overflow bugs.
 *
 * 6. Q: Is this single-threaded / do we need to worry about concurrent access to the
 *       stick pool?
 *    A (assumed): No concurrency concerns — this is a single-threaded, single-call
 *       computation.
 *
 * 7. Q: Are all stick lengths guaranteed positive integers (no zero or negative
 *       lengths)?
 *    A (assumed): Yes, per constraints, 1 <= sticks[i] <= 10^3, so no need to filter
 *       or validate against zero/negative values, though I will still validate input
 *       defensively in the production version.
 *
 * 8. Q: Should the original input array be mutated, or must it be left untouched?
 *    A (assumed): Treat the input as read-only / do not mutate the caller's array —
 *       copy values into whatever internal structure I use.
 */

/*
 * =====================================================================================
 * SECTION 3: EXAMPLES & EDGE CASES
 * =====================================================================================
 *
 * Example 1 (normal case): sticks = [2, 4, 3]
 *   - Always merge the two smallest available sticks.
 *   - Smallest two: 2 and 3 -> cost 5, pool becomes [4, 5]
 *   - Smallest two: 4 and 5 -> cost 9, pool becomes [9]
 *   - Total cost = 5 + 9 = 14
 *
 * Example 2 (edge case — single stick / minimal input):
 *   sticks = [5]
 *   - Only one stick, no merges needed.
 *   - Total cost = 0
 *
 *   Edge case — two sticks:
 *   sticks = [1, 1000]
 *   - Only one possible merge: cost = 1001
 *   - Total cost = 1001
 *
 * Example 3 (tie-breaking / boundary case): sticks = [1, 1, 1, 1]
 *   - Multiple sticks share the same length, so ties in "which two smallest" can be
 *     broken arbitrarily without affecting optimality (any valid tie-break yields the
 *     same total cost because the merge structure is symmetric under equal values).
 *   - Merge 1,1 -> cost 2, pool = [1, 1, 2]
 *   - Merge 1,1 -> cost 2, pool = [2, 2]
 *   - Merge 2,2 -> cost 4, pool = [4]
 *   - Total cost = 2 + 2 + 4 = 8
 *   - This demonstrates the algorithm is well-defined even with heavy duplication,
 *     and that tie-breaking strategy does not matter for correctness.
 */

/*
 * =====================================================================================
 * SECTION 4 & 5: ALL POSSIBLE SOLUTIONS (across applicable paradigms)
 * =====================================================================================
 *
 * Paradigm sweep — quick pass over the standard toolbox before committing to one:
 *
 *   - Brute force / naive           -> APPLICABLE (Approach 1): try every possible
 *                                       pairing order; correct but exponential.
 *   - Sorting-based                 -> APPLICABLE (Approach 2): repeatedly re-sort and
 *                                       merge the two smallest; correct but wasteful.
 *   - Hashing-based                 -> NOT APPLICABLE: there's no lookup/grouping-by-
 *                                       key need here; cost depends purely on relative
 *                                       ordering of magnitudes, not identity/frequency
 *                                       lookups, so a hash map buys us nothing.
 *   - Two pointer / sliding window  -> NOT DIRECTLY APPLICABLE in the classic sense
 *                                       (no contiguous window over indices to slide),
 *                                       but a close cousin — the "two queues" technique
 *                                       — reuses the two-pointer *idea* (two synced
 *                                       cursors advancing over sorted/produced data) to
 *                                       avoid a heap entirely (Approach 3).
 *   - Divide and conquer            -> NOT APPLICABLE: sub-problems here aren't
 *                                       independent, contiguous partitions of the input
 *                                       (unlike, say, merge sort) — the choice of what
 *                                       to merge next depends globally on the current
 *                                       minimum across the whole remaining pool, so
 *                                       splitting the array in half doesn't decompose
 *                                       the problem cleanly.
 *   - Greedy                        -> APPLICABLE (Approaches 3 & 4): always merge the
 *                                       two currently-smallest sticks. Exchange-argument
 *                                       proof below.
 *   - Dynamic programming           -> NOT APPLICABLE / NOT NEEDED: this is the classic
 *                                       "optimal merge pattern" problem, which is a
 *                                       textbook example where a *provably optimal
 *                                       greedy* replaces what might look like it needs
 *                                       DP over subsets (there's no useful overlapping-
 *                                       subproblem/optimal-substructure formulation here
 *                                       that beats the O(n log n) greedy — a DP over
 *                                       which subsets to merge would be exponential
 *                                       state space with no benefit).
 *   - Tree / graph traversal        -> CONCEPTUALLY RELATED, not separately implemented:
 *                                       the sequence of merges forms a full binary tree
 *                                       (identical in spirit to a Huffman tree) where
 *                                       leaves are original sticks and each internal
 *                                       node's value is the sum of its children; total
 *                                       cost = sum of all internal node values = sum of
 *                                       (leaf value * leaf depth). The heap-based greedy
 *                                       implicitly builds this tree, so I won't build an
 *                                       explicit tree structure as a separate approach.
 *   - Heap / priority queue         -> APPLICABLE (Approach 4, RECOMMENDED / OPTIMAL):
 *                                       min-heap gives O(n log n) with the simplest,
 *                                       most idiomatic code.
 *   - Binary search                 -> NOT APPLICABLE: there's no monotonic predicate
 *                                       over a search space we're narrowing down (we're
 *                                       not searching for a threshold value or index).
 *   - Monotonic stack / deque       -> PARTIALLY RELATED: the two-queue approach
 *                                       (Approach 3) uses two FIFO queues in a way that
 *                                       keeps each queue monotonically non-decreasing,
 *                                       which is the closest this problem gets to a
 *                                       monotonic-structure technique.
 *   - Trie / segment tree / etc.    -> NOT APPLICABLE: no prefix/range-query structure
 *                                       is needed; we only ever need "give me the
 *                                       current minimum," which a heap already does
 *                                       optimally.
 */

class MinCostConnectSticks {

    /*
     * -------------------------------------------------------------------------------
     * APPROACH 1: Brute Force (try every possible merge order)
     * -------------------------------------------------------------------------------
     * Core idea: At each step, try merging EVERY possible pair of currently available
     * sticks, recurse on the resulting (smaller) pool, and take the minimum total cost
     * over all choices. This explores the entire space of merge orders.
     *
     * Data structure / paradigm: plain recursion / exhaustive search over an ArrayList.
     *
     * Time complexity: At depth k there are roughly C(size_k, 2) choices, and the
     *   recursion depth is (n - 1). This blows up combinatorially — on the order of
     *   O(n! * n) / roughly O((n^2)^n) in the worst case (extremely loose bound, but
     *   the point is it's super-exponential). Only tractable for n <= ~7-8 in practice.
     * Space complexity: O(n) per recursive call stack frame for the copied list, and
     *   recursion depth O(n), so O(n^2) auxiliary space along the active call path.
     *
     * Pros: Trivially, obviously correct — a great "reference oracle" for testing
     *   faster approaches against on small inputs.
     * Cons: Completely infeasible for n up to 1000 (the actual constraint) — would
     *   never finish. Exists purely as a correctness baseline.
     *
     * When to use: Never in production, and never as your final interview answer for
     *   this problem — but very useful to mention briefly to show you understand the
     *   full brute-force search space, and to cross-validate smaller test cases against
     *   the optimal solution in a test harness.
     */
    static long bruteForceMinCost(int[] sticksInput) {
        if (sticksInput.length <= 1) {
            return 0L;
        }
        // Guard rail: brute force is only ever invoked on tiny inputs in this file.
        if (sticksInput.length > 8) {
            throw new IllegalArgumentException(
                "bruteForceMinCost is only intended for demonstration on n <= 8 " +
                "(exponential blow-up beyond that) — use the optimal heap approach instead.");
        }
        List<Long> pool = new ArrayList<>();
        for (int length : sticksInput) {
            pool.add((long) length);
        }
        return bruteForceRecurse(pool);
    }

    private static long bruteForceRecurse(List<Long> pool) {
        if (pool.size() == 1) {
            return 0L; // no more merges needed
        }
        long bestCost = Long.MAX_VALUE;
        // Try every unordered pair (i, j) as the next merge.
        for (int i = 0; i < pool.size(); i++) {
            for (int j = i + 1; j < pool.size(); j++) {
                List<Long> nextPool = new ArrayList<>(pool);
                long mergedValue = nextPool.get(i) + nextPool.get(j);
                // Remove the larger index first to keep the smaller index valid.
                nextPool.remove(j);
                nextPool.remove(i);
                nextPool.add(mergedValue);
                long candidateCost = mergedValue + bruteForceRecurse(nextPool);
                bestCost = Math.min(bestCost, candidateCost);
            }
        }
        return bestCost;
    }

    /*
     * -------------------------------------------------------------------------------
     * APPROACH 2: Sorting-Based Repeated Greedy (re-sort every iteration)
     * -------------------------------------------------------------------------------
     * Core idea: Same greedy insight as the optimal solution ("always merge the two
     * smallest sticks currently available") but implemented naively: after every
     * single merge, re-sort the ENTIRE remaining collection from scratch to find the
     * new two smallest values, rather than maintaining a structure that supports
     * efficient repeated minimum extraction.
     *
     * Data structure / paradigm: sorting (Arrays.sort / Collections.sort) + greedy.
     *
     * Time complexity: O(n) merges, each triggering an O(n log n) sort of the
     *   remaining pool => O(n^2 log n) overall.
     * Space complexity: O(n) for the working list (plus O(n) per sort call
     *   internally, not asymptotically dominant).
     *
     * Pros: Very easy to reason about and verify correctness of — the greedy choice
     *   is explicit and visible at every step (just look at index 0 and 1 after
     *   sorting). Good stepping stone to explain the greedy insight before optimizing
     *   the "get the two smallest" operation with a heap.
     * Cons: Wasteful — we don't need to re-sort the ENTIRE array every time; we only
     *   need the two smallest values, and a heap maintains that incrementally in
     *   O(log n) per operation instead of O(n log n).
     *
     * When to use: Fine for very small n or as an intermediate explanatory step in an
     *   interview narrative, but I would not present this as my final answer — I'd
     *   proactively note "we're re-doing more work than necessary here; a min-heap
     *   gets us the same greedy choice in O(log n) instead of O(n log n) per step."
     */
    static long sortingBasedMinCost(int[] sticksInput) {
        if (sticksInput.length <= 1) {
            return 0L;
        }
        List<Long> pool = new ArrayList<>();
        for (int length : sticksInput) {
            pool.add((long) length);
        }
        long totalCost = 0L;
        while (pool.size() > 1) {
            Collections.sort(pool); // O(n log n) every single iteration
            long smallest = pool.remove(0);
            long secondSmallest = pool.remove(0);
            long mergedValue = smallest + secondSmallest;
            totalCost += mergedValue;
            pool.add(mergedValue);
        }
        return totalCost;
    }

    /*
     * -------------------------------------------------------------------------------
     * APPROACH 3: Two-Queue Greedy (sort once, no heap needed)
     * -------------------------------------------------------------------------------
     * Core idea: Sort the sticks once up front. Maintain two FIFO queues:
     *   - `originalsQueue` holds the original sticks in sorted (ascending) order.
     *   - `mergedQueue` holds newly created merged sticks, which are ALWAYS produced
     *     in non-decreasing order (a classic and important invariant — proven below).
     *   At each step, the next smallest available value is always at the front of one
     *   of these two queues, so we just compare the two front elements (treating an
     *   empty queue as +infinity) and pop the smaller one. This is a well-known
     *   optimization of the Huffman-style merge for the special case where all "leaf"
     *   weights are known and sorted up front.
     *
     *   Why merged values come out non-decreasing: each new merged value is the sum of
     *   the two smallest values available AT THE TIME of merging. Since we always
     *   consume the smallest two available values first, each subsequent merge sums
     *   values that are >= the values summed in the previous merge, so the merged
     *   values themselves form a non-decreasing sequence. This is what lets a simple
     *   queue (not a heap) work correctly here.
     *
     * Data structure / paradigm: sorting + two-pointer-style dual queues (greedy).
     *
     * Time complexity: O(n log n) for the single initial sort, then O(n) for the
     *   merge loop (each of the n-1 merges does O(1) work) => O(n log n) overall.
     * Space complexity: O(n) for the two queues.
     *
     * Pros: Same asymptotic complexity as the heap approach, but often faster in
     *   practice (lower constant factor) since queue push/pop is O(1) with no
     *   log-factor re-heapify cost per operation. Also a nice "I know an optimization
     *   the interviewer might not expect" flex.
     * Cons: Slightly more subtle to get right (the non-decreasing invariant needs to
     *   be understood and explained, not just implemented) — a bit more error-prone
     *   under interview time pressure than "just throw everything in a PriorityQueue."
     *
     * When to use: Great to mention as a follow-up optimization after presenting the
     *   heap solution, especially if the interviewer asks "can you avoid the log n
     *   factor per operation?" I would NOT lead with this as my first answer, since
     *   the heap approach is simpler to state and code correctly under time pressure.
     */
    static long twoQueueMinCost(int[] sticksInput) {
        if (sticksInput.length <= 1) {
            return 0L;
        }
        int[] sortedSticks = sticksInput.clone();
        Arrays.sort(sortedSticks); // single O(n log n) sort

        Deque<Long> originalsQueue = new ArrayDeque<>();
        for (int length : sortedSticks) {
            originalsQueue.addLast((long) length);
        }
        Deque<Long> mergedQueue = new ArrayDeque<>();

        long totalCost = 0L;
        while (originalsQueue.size() + mergedQueue.size() > 1) {
            long first = popSmallestFront(originalsQueue, mergedQueue);
            long second = popSmallestFront(originalsQueue, mergedQueue);
            long mergedValue = first + second;
            totalCost += mergedValue;
            mergedQueue.addLast(mergedValue); // appended values stay non-decreasing
        }
        return totalCost;
    }

    // Pops and returns the smaller of the two queues' front elements.
    private static long popSmallestFront(Deque<Long> originalsQueue, Deque<Long> mergedQueue) {
        if (originalsQueue.isEmpty()) {
            return mergedQueue.pollFirst();
        }
        if (mergedQueue.isEmpty()) {
            return originalsQueue.pollFirst();
        }
        return (originalsQueue.peekFirst() <= mergedQueue.peekFirst())
                ? originalsQueue.pollFirst()
                : mergedQueue.pollFirst();
    }

    /*
     * -------------------------------------------------------------------------------
     * APPROACH 4 (RECOMMENDED / OPTIMAL): Min-Heap Greedy
     * -------------------------------------------------------------------------------
     * Core idea: Push all stick lengths into a min-heap (PriorityQueue). Repeatedly
     * pop the two smallest sticks, merge them (cost += sum), and push the merged
     * stick back into the heap. Stop when one stick remains.
     *
     * Correctness (exchange argument / greedy-choice proof): Suppose an optimal
     * solution does NOT merge the two globally smallest sticks, x and y, at the very
     * first opportunity. Consider the merge tree it produces: x and y each appear at
     * SOME depth in that tree. Because it's a full binary merge tree, total cost
     * equals sum over all original sticks of (value * depth-in-tree). Standard
     * exchange-argument logic (identical to the Huffman coding optimality proof)
     * shows that swapping x and y into the two deepest leaf positions (i.e., merging
     * them first) can never increase — and can only decrease or keep equal — the
     * total weighted cost, because they are the two smallest values and therefore
     * benefit most from being multiplied by the largest depth as early merges. This
     * is exactly the classical Huffman-coding / optimal-merge-pattern proof; by
     * induction on the remaining sticks after this swap, always merging the two
     * current smallest sticks is optimal at every step.
     *
     * Data structure / paradigm: min-heap (java.util.PriorityQueue) + greedy.
     *
     * Time complexity: n-1 merges, each doing 2 removals + 1 insertion on a heap of
     *   size O(n) => O(n log n) overall. Building the initial heap is O(n) (heapify)
     *   or O(n log n) if built via repeated offers — Java's PriorityQueue(Collection)
     *   constructor uses the O(n) heapify path, which I use below.
     * Space complexity: O(n) for the heap.
     *
     * Pros: Simplest correct implementation to write under interview time pressure —
     *   idiomatic use of java.util.PriorityQueue, minimal room for subtle bugs, easy
     *   to explain and reason about on a whiteboard. Matches interviewer expectations
     *   for this well-known problem class ("optimal merge pattern" / Huffman-style).
     * Cons: Marginally higher constant factor than the two-queue approach (Approach
     *   3) since every heap operation carries a log n re-heapify cost, whereas the
     *   two-queue method does O(1) work per step after a single sort.
     *
     * When to use: This is what I would code as my primary interview answer — it is
     *   optimal in asymptotic complexity, simple, idiomatic, and low-risk to
     *   implement correctly live. I would verbally mention the two-queue
     *   optimization as a "did you know" follow-up rather than leading with it.
     */
    static long minHeapMinCost(int[] sticksInput) {
        if (sticksInput.length <= 1) {
            return 0L;
        }
        PriorityQueue<Long> minHeap = new PriorityQueue<>(sticksInput.length);
        for (int length : sticksInput) {
            minHeap.offer((long) length);
        }
        long totalCost = 0L;
        while (minHeap.size() > 1) {
            long smallest = minHeap.poll();
            long secondSmallest = minHeap.poll();
            long mergedValue = smallest + secondSmallest;
            totalCost += mergedValue;
            minHeap.offer(mergedValue);
        }
        return totalCost;
    }

    /*
     * =================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * =================================================================================
     *
     * | Approach                    | Time              | Space  | Best For                    | Limitations                          |
     * |------------------------------|-------------------|--------|------------------------------|----------------------------------------|
     * | 1. Brute Force               | Super-exponential | O(n^2) | Correctness oracle, n <= ~8  | Totally infeasible for real n (<=1000) |
     * | 2. Sorting-Based (re-sort)   | O(n^2 log n)      | O(n)   | Teaching the greedy insight  | Re-sorts far more than necessary       |
     * | 3. Two-Queue Greedy          | O(n log n)        | O(n)   | Lowest constant factor       | Trickier invariant to explain/prove    |
     * | 4. Min-Heap Greedy (OPTIMAL) | O(n log n)        | O(n)   | Interview default, production| Slightly higher constant than Approach 3|
     */

    /*
     * =====================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * =====================================================================================
     *
     * I would present Approach 4 (Min-Heap Greedy) as my primary solution:
     *   - It achieves the optimal O(n log n) time complexity.
     *   - It is the fastest to code correctly under interview time pressure — Java's
     *     PriorityQueue does all the heavy lifting, and the loop body is 5 lines.
     *   - It maps directly onto the well-known "optimal merge pattern" / Huffman
     *     coding pattern that most interviewers are specifically testing for with this
     *     problem, so it demonstrates recognition of the underlying pattern.
     *   - It's easy to explain and prove correct on a whiteboard via the exchange
     *     argument, which is exactly the kind of rigor a Google interviewer wants to
     *     hear, without needing extra machinery (sorting invariants) that a two-queue
     *     approach requires.
     * After landing this solution, I would proactively mention the two-queue
     * optimization (Approach 3) as a "here's a further optimization if you want zero
     * heap overhead" follow-up, to signal depth without over-engineering my primary
     * answer.
     */

    /*
     * =====================================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (PRODUCTION-QUALITY)
     * =====================================================================================
     */

    /**
     * Computes the minimum total cost required to connect all sticks into a single
     * stick, where the cost of connecting two sticks equals the sum of their current
     * lengths.
     *
     * <p>Algorithm: greedily merges the two currently-smallest sticks at every step,
     * using a min-heap to efficiently retrieve the current minimum. This is optimal
     * by the same exchange-argument proof that underlies Huffman coding / optimal
     * merge pattern problems: always combining the two globally smallest weights
     * first minimizes the total sum of (value * merge-depth) across all leaves.
     *
     * @param sticks array of positive stick lengths (must be non-null; per problem
     *               constraints 1 &lt;= sticks.length &lt;= 1000 and
     *               1 &lt;= sticks[i] &lt;= 1000, but this method defensively
     *               validates rather than trusting the caller blindly)
     * @return the minimum total connection cost as a {@code long} (a long is used
     *         defensively to eliminate any possibility of overflow even if the
     *         constraints were relaxed in a follow-up question)
     * @throws IllegalArgumentException if {@code sticks} is null, empty, or contains
     *                                   a non-positive length
     */
    static long minCostToConnectSticks(int[] sticks) {
        // --- Defensive input validation -------------------------------------------
        if (sticks == null) {
            throw new IllegalArgumentException("sticks must not be null");
        }
        if (sticks.length == 0) {
            throw new IllegalArgumentException(
                "sticks must contain at least one element per problem constraints");
        }
        for (int length : sticks) {
            if (length <= 0) {
                throw new IllegalArgumentException(
                    "stick lengths must be positive, found: " + length);
            }
        }

        // --- Base case: a single stick needs zero connections ----------------------
        if (sticks.length == 1) {
            return 0L;
        }

        // --- Core greedy loop using a min-heap --------------------------------------
        // Boxed Long is used (rather than primitive int) so we can store merged sums
        // safely; PriorityQueue<Long> orders by natural (ascending) Long comparison,
        // which gives us O(log n) access to the current minimum on every operation.
        PriorityQueue<Long> minHeap = new PriorityQueue<>(sticks.length);
        for (int length : sticks) {
            minHeap.offer((long) length);
        }

        long totalCost = 0L;
        // Exactly (n - 1) merges are performed; loop naturally terminates when one
        // stick (the final combined stick) remains in the heap.
        while (minHeap.size() > 1) {
            long smallest = minHeap.poll();       // O(log n)
            long secondSmallest = minHeap.poll();  // O(log n)
            long mergedStickLength = smallest + secondSmallest;
            totalCost += mergedStickLength;        // accumulate this merge's cost
            minHeap.offer(mergedStickLength);      // O(log n): feed result back in
        }

        return totalCost;
    }

    /*
     * =====================================================================================
     * SECTION 10: DRY RUN / TRACE
     * =====================================================================================
     *
     * Tracing minCostToConnectSticks(int[]{2, 4, 3}) step by step:
     *
     * Initial heap after offering all values: [2, 3, 4]   (min-heap order)
     * totalCost = 0
     *
     * Iteration 1:
     *   smallest = poll() -> 2        heap now: [3, 4]
     *   secondSmallest = poll() -> 3  heap now: [4]
     *   mergedStickLength = 2 + 3 = 5
     *   totalCost = 0 + 5 = 5
     *   offer(5)                      heap now: [4, 5]
     *
     * Iteration 2:
     *   smallest = poll() -> 4        heap now: [5]
     *   secondSmallest = poll() -> 5  heap now: []
     *   mergedStickLength = 4 + 5 = 9
     *   totalCost = 5 + 9 = 14
     *   offer(9)                      heap now: [9]
     *
     * Loop condition check: minHeap.size() == 1 -> loop ends.
     *
     * Return totalCost = 14   <-- matches Example 1's hand-computed answer.
     */

    /*
     * =====================================================================================
     * SECTION 11: CLOSING SUMMARY
     * =====================================================================================
     *
     * All four approaches are correct; they differ purely in efficiency:
     *   - Brute force is an exponential correctness oracle only, never viable for the
     *     actual constraint of n up to 1000.
     *   - The naive sorting approach demonstrates the greedy insight but pays an
     *     unnecessary O(n) penalty per step by re-sorting the entire pool.
     *   - The two-queue approach and the min-heap approach are both optimal at
     *     O(n log n) time / O(n) space; the min-heap version is what I'd write first
     *     in an interview for its simplicity and low bug-risk, with the two-queue
     *     version mentioned as a lower-constant-factor follow-up.
     * Known assumptions/limitations of the final solution:
     *   - Assumes stick lengths fit comfortably such that a `long` accumulator never
     *     overflows (true for the given constraints with wide margin).
     *   - Assumes the caller wants only the total cost, not the merge sequence itself
     *     (trivial to extend by recording each merge's operands if needed).
     *   - Input array is not mutated; a fresh heap is built from its values.
     */

    /*
     * =====================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * =====================================================================================
     *
     * 1. "Can you also return the actual sequence of merges, not just the total cost?"
     *    -> Track each (a, b, merged) triple in a list as you go; O(n) extra space,
     *       no change to asymptotic time complexity.
     *
     * 2. "What if n could be up to 10^7 instead of 10^3 — does your solution still
     *     work?"
     *    -> Yes, O(n log n) with a heap or two-queue approach scales fine; I'd favor
     *       the two-queue approach here since it avoids per-operation heap overhead
     *       and has a smaller constant factor at large scale.
     *
     * 3. "What if stick lengths could be extremely large (e.g., up to 10^9)?"
     *    -> Already using `long` for the accumulator/heap values, so this is handled;
     *       I'd double check the maximum possible total cost against Long.MAX_VALUE
     *       (it's astronomically far from overflowing even at that scale for n up to
     *       10^7).
     *
     * 4. "Can you connect more than two sticks at once (e.g., cost = sum of any k
     *     sticks), and how would that change the algorithm?"
     *    -> This becomes the "k-ary Huffman merge" variant; the greedy still holds
     *       (always merge the k smallest), but you may need to pad the input with
     *       zero-length dummy sticks so (n - 1) is divisible by (k - 1) for the merge
     *       tree to be a valid full k-ary tree — a classic subtlety in k-ary Huffman.
     *
     * 5. "What if merging two sticks of the same length were free (cost 0)?"
     *    -> Changes the greedy criterion; you'd need to special-case equal-length
     *       pairs and prioritize those merges first, which likely requires a
     *       different data structure (e.g., grouping by length) rather than a plain
     *       min-heap.
     *
     * 6. "Could you solve this in a streaming fashion, where sticks arrive one at a
     *     time and you must maintain the running minimum total cost so far?"
     *    -> This is fundamentally harder — the optimal merge order can change
     *       retroactively as new (potentially smaller) sticks arrive, so you cannot
     *       simply extend a running total; you'd likely need to recompute from the
     *       heap's current contents whenever a final answer is queried, which the
     *       heap-based approach still supports incrementally between queries.
     */

    /*
     * =====================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * =====================================================================================
     *
     * 1. Forgetting to push the MERGED stick back into the data structure — candidates
     *    sometimes just discard the merged value and only track the cost, which
     *    produces completely wrong results since future merges depend on the new
     *    combined stick being available.
     *
     * 2. Using a max-heap or forgetting to explicitly configure ascending order —
     *    Java's PriorityQueue is a min-heap by default for Comparable elements, but
     *    candidates coming from languages where the default is a max-heap (or who
     *    mis-remember Java's default) sometimes pass a reversed comparator by
     *    accident and silently get the wrong (maximized) answer.
     *
     * 3. Off-by-one on the loop/termination condition — looping `while (heap is not
     *    empty)` instead of `while (heap.size() > 1)` causes an attempt to poll a
     *    non-existent "second smallest" element on the final iteration (either a
     *    NullPointerException from unboxing a null Long, or an infinite/incorrect
     *    loop), instead of correctly stopping once exactly one stick remains.
     *
     * 4. Not handling the single-stick (n == 1) case explicitly — some candidates'
     *    loops technically handle it correctly by accident, but many forget to reason
     *    about it out loud, which interviewers specifically listen for as a sign of
     *    edge-case awareness. Also, candidates using `int` instead of `long` for the
     *    cost accumulator can silently overflow if constraints are later relaxed in a
     *    follow-up question — worth mentioning proactively even when not strictly
     *    required by the stated constraints.
     */

    /*
     * =====================================================================================
     * TEST HARNESS — cross-validates all four approaches against each other
     * =====================================================================================
     */
    public static void main(String[] args) {
        List<int[]> testCases = List.of(
            new int[]{2, 4, 3},                 // normal case -> expected 14
            new int[]{5},                       // edge case: single stick -> expected 0
            new int[]{1, 1000},                 // edge case: two sticks -> expected 1001
            new int[]{1, 1, 1, 1},              // tie-breaking / duplicates -> expected 8
            new int[]{1, 8, 3, 5},              // normal case with mixed values
            new int[]{1000, 1000, 1000, 1000, 1000}, // max-value duplicates, boundary check
            new int[]{6, 4, 9, 5, 2}            // another mixed normal case
        );

        for (int[] testCase : testCases) {
            long optimalResult = minCostToConnectSticks(testCase);
            long heapResult = minHeapMinCost(testCase);
            long twoQueueResult = twoQueueMinCost(testCase);
            long sortingResult = sortingBasedMinCost(testCase);

            System.out.println("Input: " + Arrays.toString(testCase));
            System.out.println("  Optimal (production):   " + optimalResult);
            System.out.println("  Min-Heap Greedy:         " + heapResult);
            System.out.println("  Two-Queue Greedy:        " + twoQueueResult);
            System.out.println("  Sorting-Based Greedy:    " + sortingResult);

            boolean allAgree = (optimalResult == heapResult)
                    && (optimalResult == twoQueueResult)
                    && (optimalResult == sortingResult);

            // Only run brute force cross-check for small inputs (guarded internally too).
            if (testCase.length <= 8) {
                long bruteForceResult = bruteForceMinCost(testCase);
                System.out.println("  Brute Force (oracle):    " + bruteForceResult);
                allAgree = allAgree && (optimalResult == bruteForceResult);
            }

            System.out.println("  All approaches agree:    " + allAgree);
            System.out.println();

            if (!allAgree) {
                throw new AssertionError("Mismatch detected for input: " + Arrays.toString(testCase));
            }
        }

        System.out.println("All test cases passed across all approaches.");
    }
}
