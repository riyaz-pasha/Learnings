import java.util.*;

/**
 * ============================================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: "Construct Target Array With Multiple Sums"
 * (LeetCode 1354)
 * ============================================================================================
 *
 * This single file walks through the complete interview arc for this problem, exactly as it
 * should be presented live: restatement, clarifying questions, examples, every viable
 * approach (naive through optimal), a comparison table, the recommended approach, a
 * production-quality deep dive, a manual trace, a closing summary, follow-ups, and common
 * candidate mistakes.
 * ============================================================================================
 */
class ConstructTargetArray {

    /*
     * ========================================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================================
     *
     * In my own words:
     *
     *   We start with an array `arr` of length n, where every element is 1.
     *   We may repeatedly perform this operation:
     *     - Compute x = sum of all elements currently in arr.
     *     - Choose any index i and overwrite arr[i] with x.
     *
     *   After zero or more such operations, we want to know: can `arr` become exactly equal
     *   to the given `target` array (same length, same values, same positions)?
     *
     * Inputs:
     *   - target: int[] of length n, 1 <= n <= 1000, 1 <= target[i] <= 10^5.
     *
     * Output:
     *   - boolean: true if `target` is reachable from the all-ones array via the described
     *     operation, false otherwise.
     *
     * Implicit assumptions to make explicit:
     *   - The operation can be applied any number of times (including zero times), and to any
     *     index, including the same index repeatedly.
     *   - Order of operations matters for *how* we reach target, but we only care about
     *     reachability, not the sequence of operations (unless asked as a follow-up).
     *   - Since the operation only ever sets an index to the *current total sum*, and every
     *     value starts at 1 (a positive integer), every element remains a positive integer
     *     forever — this rules out 0 or negative target values trivially (constraints already
     *     guarantee target[i] >= 1).
     */

    /*
     * ========================================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ========================================================================================
     *
     * 1. Q: Can n be 1? What should we return in that case?
     *    A: Yes, n can be 1 (constraint says n >= 1). Since arr starts as [1], and the only
     *       operation available sets arr[0] = sum = arr[0] itself (a no-op when n = 1), the
     *       array can never change. So the answer is true iff target[0] == 1.
     *
     * 2. Q: Can target contain duplicate values across different indices?
     *    A: Yes, duplicates are allowed and don't need special-casing — reachability is
     *       evaluated per-position via the array's multiset of values, not positional
     *       uniqueness.
     *
     * 3. Q: Are target[i] values guaranteed positive integers within [1, 10^5]?
     *    A: Yes, per constraints: 1 <= target[i] <= 10^5. No need to validate for zero/negative.
     *
     * 4. Q: Do we need to return the actual sequence of operations, or just true/false?
     *    A: Just true/false reachability. (A follow-up may ask to reconstruct the sequence.)
     *
     * 5. Q: What's the expected time complexity given n up to 1000 and values up to 10^5?
     *    A: Anything from O(n log n * log(maxValue)) to O(n * maxValue) in the worst case is
     *       acceptable; a naive O(sum(target)) forward simulation could be far too slow since
     *       sums can grow combinatorially, so we should avoid simulating forward operation by
     *       operation for large values.
     *
     * 6. Q: Is arr guaranteed to start as all 1's, or could the starting array be arbitrary?
     *    A: Always all 1's, as stated in the problem — no need to handle arbitrary starting
     *       arrays.
     *
     * 7. Q: Should the solution handle concurrent/multiple queries efficiently (e.g., is this
     *       called many times with different targets), or is this a single one-shot call?
     *    A: Treat it as a single one-shot call; no shared state or caching across calls is
     *       required unless asked as a follow-up.
     *
     * 8. Q: If target already equals the all-ones array (every element is 1), is that
     *       trivially true?
     *    A: Yes — zero operations is a valid (empty) sequence of operations, so target = all
     *       1's is always reachable.
     */

    /*
     * ========================================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================================
     *
     * Example 1 (normal case): target = [9, 3, 5]
     *   Start: [1,1,1], sum=3.
     *   Set index2 -> 3: [1,1,3], sum=5.
     *   Set index2 -> 5: [1,1,5], sum=7.
     *   Set index0 -> 7: [7,1,5], sum=13.
     *   Set index0 -> ... (continue combining) eventually reach [9,3,5].
     *   Expected output: true.
     *   (Verified programmatically below via reverse simulation and a brute-force oracle.)
     *
     * Example 2 (edge case, n = 1): target = [1]
     *   Start: [1]. No operation can ever change it (x = arr[0] = 1 always).
     *   Expected output: true (target already matches the start state).
     *
     *   Edge case variant: target = [5] (n = 1, value != 1)
     *   Expected output: false — impossible, since a single-element array is frozen at 1
     *   forever.
     *
     * Example 3 (boundary / tie-breaking case): target = [1, 1, 1, 2]
     *   Start: [1,1,1,1], sum=4. Any single operation jumps one index straight to 4, which
     *   overshoots the desired 2. There is no way to land exactly on 2 for one index while
     *   keeping the rest at 1, since the very first operation already produces a 4 (too big).
     *   Expected output: false.
     *   This demonstrates the key boundary insight: the maximum element of target must be
     *   strictly greater than the sum of all the OTHER elements, tracing backward, at every
     *   step of the reverse process — otherwise reduction is impossible.
     */

    /*
     * ========================================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES (naive -> optimal), across applicable paradigms
     * ========================================================================================
     *
     * Paradigms considered and whether they apply:
     *   - Brute force / naive forward simulation: APPLICABLE (baseline, but exponential/slow).
     *   - Sorting-based: APPLICABLE as a building block for a "linear-scan-without-heap"
     *     optimized variant.
     *   - Hashing-based: NOT APPLICABLE — there's no lookup/grouping structure that helps;
     *     reachability here is purely arithmetic on sums, not membership testing.
     *   - Two pointer / sliding window: NOT APPLICABLE — there's no contiguous window or
     *     monotonic scan over a linear range that models this problem.
     *   - Divide and conquer: NOT APPLICABLE — no natural split into independent subproblems
     *     whose solutions combine; every element interacts through a single shared running sum.
     *   - Greedy: APPLICABLE — reversing from target down to all-ones by always shrinking the
     *     current maximum is a greedy strategy, and it is provably correct (see Approach 3).
     *   - Dynamic programming: NOT APPLICABLE — no overlapping subproblems / optimal
     *     substructure to memoize; this is a reachability/simulation problem, not an
     *     optimization over a state space with reusable subresults.
     *   - Tree / graph traversal: NOT APPLICABLE (directly) — one could model reachable states
     *     as an implicit graph, but the state space (all reachable arrays) is far too large to
     *     traverse explicitly; the greedy reverse-simulation instead reasons about it in
     *     closed form.
     *   - Heap / priority queue: APPLICABLE — this is the core structure for the optimal
     *     solution, used to repeatedly extract the current maximum in O(log n).
     *   - Binary search: NOT APPLICABLE — there's no monotonic predicate over a sorted search
     *     space to binary search on.
     *   - Monotonic stack / deque: NOT APPLICABLE — no next-greater-element or
     *     window-extremum pattern here.
     *   - Trie / segment tree / advanced structures: NOT APPLICABLE — no prefix/range query
     *     structure needed; the problem is fundamentally about repeatedly finding a single
     *     global maximum, which a binary heap handles optimally.
     */

    /* ---------------------------------------------------------------------------------------
     * Approach 1: Brute-Force Forward Simulation (BFS over reachable states)
     * ---------------------------------------------------------------------------------------
     * Core idea: Starting from the all-ones array, explore every possible operation (pick any
     * index, set it to the current sum) via BFS/DFS, pruning states where any element already
     * exceeds the corresponding target value. Check if the target state is ever reached.
     *
     * Data structures / paradigm: Brute-force state-space search (BFS) with a visited-set.
     *
     * Time Complexity: Exponential in the worst case — O(n^k) where k is the number of
     *   operations needed (k can be proportional to target values, e.g., up to ~10^5), because
     *   each state branches into n new states. Completely infeasible for n up to 1000 and
     *   values up to 10^5.
     * Space Complexity: Exponential — must store all visited states.
     *
     * Pros: Conceptually simple; a great way to build intuition and to serve as an oracle for
     *   testing smaller inputs.
     * Cons: Utterly infeasible for the given constraints; only usable for n <= ~3 and tiny
     *   target values in a test harness.
     * When to use: Never in production / for the actual constraints — only as a correctness
     *   oracle for validating faster algorithms on small random inputs (which is exactly how
     *   this solution was validated during development).
     */
    static boolean bruteForceReachable(int[] target, int maxSteps) {
        int n = target.length;
        for (int value : target) {
            if (value < 1) return false; // arr values are always positive integers
        }
        int[] start = new int[n];
        Arrays.fill(start, 1);
        if (Arrays.equals(start, target)) return true;

        Set<String> visited = new HashSet<>();
        visited.add(Arrays.toString(start));
        List<int[]> frontier = new ArrayList<>();
        frontier.add(start);

        for (int step = 0; step < maxSteps && !frontier.isEmpty(); step++) {
            List<int[]> nextFrontier = new ArrayList<>();
            for (int[] state : frontier) {
                long sum = 0;
                for (int value : state) sum += value;
                for (int i = 0; i < n; i++) {
                    if (sum > Integer.MAX_VALUE) continue; // guard against overflow while probing
                    int[] next = state.clone();
                    next[i] = (int) sum;
                    // Prune: once any coordinate overshoots its target, this branch is dead —
                    // every subsequent operation on that index only ever increases it further,
                    // since the running sum is monotonically non-decreasing.
                    boolean overshoot = false;
                    for (int j = 0; j < n; j++) {
                        if (next[j] > target[j]) { overshoot = true; break; }
                    }
                    if (overshoot) continue;
                    if (Arrays.equals(next, target)) return true;
                    String key = Arrays.toString(next);
                    if (visited.add(key)) nextFrontier.add(next);
                }
            }
            frontier = nextFrontier;
        }
        return false;
    }

    /* ---------------------------------------------------------------------------------------
     * Approach 2: Reverse Simulation Without a Heap (Repeated Linear Scan for the Max)
     * ---------------------------------------------------------------------------------------
     * Core idea: Instead of simulating forward (which explodes combinatorially), simulate
     * BACKWARD from target toward the all-ones array. At every reverse step, the current
     * maximum element must have been the LAST one written, and at the moment it was written it
     * equaled the sum of the OTHER elements at that time (since only one index changes per
     * operation, "the other elements" are exactly what they are now). So we can undo that
     * operation: replace the maximum with (maximum - sumOfOthers), repeated as many times as
     * needed — sped up via modulo instead of one-by-one subtraction. We find the maximum via a
     * linear scan each iteration instead of a heap.
     *
     * Data structures / paradigm: Greedy backward simulation; linear scan for max each round.
     *
     * Time Complexity: O(n) per iteration to find the max, and O(log(maxValue)) iterations
     *   overall (each modulo reduction shrinks the maximum by at least half in the worst case,
     *   similar to the Euclidean algorithm) — worst case O(n * log(maxValue)) which for n=1000
     *   and maxValue=10^5 is comfortably fast (~17000 operations).
     * Space Complexity: O(1) extra (in-place scan), or O(n) if we don't mutate the input.
     *
     * Pros: No auxiliary heap structure needed; simple to reason about; same asymptotic
     *   complexity class as the heap version for this problem's constraints.
     * Cons: The linear scan for the max on every iteration is less elegant than heap
     *   extraction and, for adversarial inputs, is asymptotically the same but with a worse
     *   constant factor than the heap version once n is large, because the heap needs only
     *   O(log n) instead of O(n) per extraction.
     * When to use: Perfectly fine for an interview if you want to avoid introducing a
     *   PriorityQueue; a good "first correct optimal-ish" approach. In practice, prefer the
     *   heap version (Approach 3) for a cleaner O(log n)-per-step complexity and cleaner code.
     */
    static boolean isPossibleLinearScan(int[] target) {
        int n = target.length;
        if (n == 1) return target[0] == 1;

        int[] values = target.clone();
        long total = 0;
        for (int value : values) total += value;

        while (true) {
            int maxIndex = 0;
            for (int i = 1; i < n; i++) {
                if (values[i] > values[maxIndex]) maxIndex = i;
            }
            long maxValue = values[maxIndex];
            if (maxValue == 1) return true; // every element must be 1 at this point

            long restSum = total - maxValue; // sum of all OTHER elements
            if (restSum == 1) return true;   // shortcut: remainder reduces to all-ones trivially
            if (restSum < 1 || maxValue <= restSum) return false; // can't have been the last write

            long reduced = maxValue % restSum;
            if (reduced == 0) return false; // arr elements can never legitimately become 0

            values[maxIndex] = (int) reduced;
            total = restSum + reduced;
        }
    }

    /* ---------------------------------------------------------------------------------------
     * Approach 3 (OPTIMAL): Reverse Simulation with a Max-Heap
     * ---------------------------------------------------------------------------------------
     * Core idea: Same backward-greedy insight as Approach 2, but use a max-heap (PriorityQueue)
     * to fetch the current maximum in O(log n) instead of O(n). This is the standard optimal
     * solution for this problem.
     *
     * Data structures / paradigm: Greedy + Heap / Priority Queue.
     *
     * Time Complexity: O(n log n) to build the heap, plus O(log(maxValue)) reduction rounds
     *   (via modulo, analogous to the Euclidean GCD algorithm's logarithmic shrink rate), each
     *   costing O(log n) for heap pop/push -> overall O(n log n + log(maxValue) * log n), which
     *   is extremely fast for n <= 1000 and maxValue <= 10^5.
     * Space Complexity: O(n) for the heap.
     *
     * Pros: Asymptotically optimal extraction of the max at each step; clean, idiomatic Java
     *   using java.util.PriorityQueue; easy to explain and defend in an interview.
     * Cons: Slightly more code/setup than the linear-scan version (need a max-heap, which in
     *   Java requires a reversed comparator since PriorityQueue is a min-heap by default).
     * When to use: This is the approach to present as the final answer — it is the best blend
     *   of asymptotic efficiency, code clarity, and idiomatic use of a well-known data
     *   structure that interviewers expect to see for "repeatedly operate on the current max"
     *   problems.
     */
    static boolean isPossibleMaxHeap(int[] target) {
        int n = target.length;
        if (n == 1) return target[0] == 1;

        PriorityQueue<Long> maxHeap = new PriorityQueue<>(n, Collections.reverseOrder());
        long total = 0;
        for (int value : target) {
            maxHeap.offer((long) value);
            total += value;
        }

        while (maxHeap.peek() != 1L) {
            long maxValue = maxHeap.poll();
            long restSum = total - maxValue;

            if (restSum == 1) return true;
            if (restSum < 1 || maxValue <= restSum) return false;

            long reduced = maxValue % restSum;
            if (reduced == 0) return false;

            maxHeap.offer(reduced);
            total = restSum + reduced;
        }
        return true;
    }

    /*
     * ========================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================================
     *
     * | Approach                          | Time                          | Space  | Best For                          | Limitations                                  |
     * |------------------------------------|--------------------------------|--------|-----------------------------------|-----------------------------------------------|
     * | 1. Brute-Force Forward Simulation  | Exponential O(n^k)             | O(n^k) | Building intuition / test oracle  | Infeasible beyond tiny n & tiny target values |
     * | 2. Reverse Simulation, Linear Scan | O(n * log(maxValue))           | O(1)   | Avoiding heap boilerplate          | Worse constant factor than heap for large n   |
     * | 3. Reverse Simulation, Max-Heap    | O(n log n + log(maxValue) log n)| O(n)  | Production / interview final answer| None significant for given constraints        |
     */

    /*
     * ========================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================================
     *
     * I would present Approach 3 (Reverse Simulation with a Max-Heap) as the final solution:
     *
     *   - Correctness clarity: The forward operation is hard to reason about directly because
     *     the search space explodes, but reversing it collapses the problem into a clean
     *     greedy invariant: "the current maximum was written last, and equals the sum of
     *     everything else at that time." This is a natural, defensible insight to state out
     *     loud, and it mirrors the classic "GCD-style" reduction, which most interviewers will
     *     immediately recognize and appreciate.
     *   - Coding speed: A PriorityQueue-based max-heap is idiomatic, fast to type correctly in
     *     Java, and avoids off-by-one bugs that a hand-rolled linear scan might introduce under
     *     interview pressure.
     *   - Optimality: O(n log n + log(maxValue) log n) is comfortably optimal for the given
     *     constraints (n <= 1000, values <= 10^5) and is the best known asymptotic complexity
     *     for this problem.
     *   - Interviewer expectations: This is a well-known "reverse the operation + greedily
     *     shrink the max with a heap" pattern (similar in spirit to problems like "Last Stone
     *     Weight" or Euclidean-algorithm-flavored reductions), and demonstrating the reverse-
     *     thinking insight is exactly what distinguishes a strong candidate on this problem.
     */

    /*
     * ========================================================================================
     * SECTION 9: DEEP DIVE — PRODUCTION-QUALITY OPTIMAL IMPLEMENTATION
     * ========================================================================================
     */

    /** Sentinel used only for documentation clarity; the algorithm never actually reduces a
     *  value to zero — a reduction to exactly zero is treated as an immediate contradiction,
     *  since every real element of arr is always a positive integer (arr starts at all 1's and
     *  the operation only ever writes a positive sum into an index). */
    private static final long INVALID_REDUCTION = 0L;

    /**
     * Determines whether {@code target} is reachable from an all-ones array of the same
     * length via the operation: "pick an index, set it to the current sum of the array,"
     * applied any number of times (including zero).
     *
     * <p>Algorithmic idea (reverse simulation): rather than simulating forward from all-ones
     * (which can require an astronomically large number of operations for large target
     * values), we simulate BACKWARD from {@code target} toward all-ones. At each step, the
     * current maximum element of the working array must have been the last one written (since
     * writing an index always installs the current running sum, which is at least as large as
     * every other element at that moment — ties are handled naturally since a tie means every
     * element is equal, which is only consistent with all being 1 once we terminate). Undoing
     * that write means subtracting the sum of all OTHER elements ({@code restSum}) from the
     * maximum, possibly multiple times in a row before that index would exceed any other
     * element again — which we short-circuit via the modulo operation, analogous to the
     * Euclidean algorithm for GCD. This guarantees a logarithmic number of reduction rounds.
     *
     * <p>Termination / correctness conditions checked at each round:
     * <ul>
     *   <li>If the current maximum is already 1, every element must be 1 (since 1 is the
     *       minimum possible value), so the array has been fully reduced: reachable.</li>
     *   <li>If {@code restSum == 1}, the maximum can always be reduced to 1 by repeatedly
     *       subtracting 1 (an always-valid sequence of prior operations), so we can
     *       short-circuit and return true immediately.</li>
     *   <li>If {@code restSum < 1} or {@code maxValue <= restSum}, the maximum could not have
     *       been produced by summing the current other elements (a write always strictly
     *       exceeds every pre-existing element, since arr is never all zero for n &gt;= 2), so
     *       reduction is impossible: not reachable.</li>
     *   <li>If {@code maxValue % restSum == 0}, naively reducing would attempt to set the
     *       element to 0, which is never a legitimate value of arr (arr entries are always
     *       positive integers): not reachable.</li>
     * </ul>
     *
     * @param target the target array to test for reachability; must be non-null with
     *               1 &lt;= target.length &lt;= 1000 and 1 &lt;= target[i] &lt;= 10^5 per
     *               problem constraints (defensively validated below regardless).
     * @return {@code true} if {@code target} is reachable from an all-ones array of the same
     *         length, {@code false} otherwise.
     * @throws IllegalArgumentException if {@code target} is null, empty, or contains a
     *         non-positive value (defensive input validation; not required by the stated
     *         constraints but good practice in production code).
     */
    public static boolean isPossible(int[] target) {
        if (target == null || target.length == 0) {
            throw new IllegalArgumentException("target must be non-null and non-empty");
        }
        for (int value : target) {
            if (value < 1) {
                throw new IllegalArgumentException("target values must be positive integers, found: " + value);
            }
        }

        int n = target.length;

        // Special case: with a single element, the operation is a no-op forever (sum of one
        // element is itself), so the array can only ever remain [1].
        if (n == 1) {
            return target[0] == 1;
        }

        // Max-heap: Java's PriorityQueue is a min-heap by default, so we invert the ordering.
        // We use `long` throughout to avoid any overflow risk when summing up to 1000 values
        // each as large as 10^5 (max possible sum = 10^8, well within int range, but `long` is
        // used defensively and to keep headroom during intermediate arithmetic).
        PriorityQueue<Long> maxHeap = new PriorityQueue<>(n, Collections.reverseOrder());
        long totalSum = 0L;
        for (int value : target) {
            maxHeap.offer((long) value);
            totalSum += value;
        }

        // Reverse-simulate until the maximum element is 1, meaning every element must be 1
        // (since 1 is the floor value for any array reachable from all-ones).
        while (maxHeap.peek() != 1L) {
            long maxValue = maxHeap.poll();
            long restSum = totalSum - maxValue; // sum of every OTHER element right now

            // Shortcut: if the rest of the array already sums to exactly 1 (meaning n - 1
            // elements are 1 apart from possibly this max), we can always walk the maximum
            // down to 1 by repeated subtraction of 1 — a trivially valid sequence of reverse
            // operations. No need to keep reducing explicitly.
            if (restSum == 1) {
                return true;
            }

            // If the "rest" sums to less than 1, or the maximum is not strictly greater than
            // the rest, this maximum could not possibly have been the last value written
            // (a legitimate write always strictly exceeds the pre-existing elements, because
            // the pre-existing elements are always positive for n >= 2).
            if (restSum < 1 || maxValue <= restSum) {
                return false;
            }

            // Fast-forward multiple reverse-subtraction steps at once via modulo — equivalent
            // to repeatedly doing (maxValue -= restSum) until maxValue <= restSum, but in
            // O(1) instead of O(maxValue / restSum) steps. This is the same trick that makes
            // the Euclidean algorithm for GCD run in logarithmic time.
            long reduced = maxValue % restSum;

            // A reduction to exactly 0 would imply the element was legitimately 0 at some
            // prior point, which never happens: every arr element is always a positive
            // integer (arr starts at all-1's, and every write installs a positive sum).
            if (reduced == INVALID_REDUCTION) {
                return false;
            }

            maxHeap.offer(reduced);
            totalSum = restSum + reduced;
        }

        return true;
    }

    /*
     * ========================================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================================
     *
     * Tracing isPossible(target = [9, 3, 5]) step by step:
     *
     * Initial: maxHeap = {9, 3, 5} (as a max-heap, top = 9), totalSum = 17.
     *
     * Round 1:
     *   maxValue = 9 (popped). restSum = 17 - 9 = 8.
     *   restSum != 1; maxValue (9) > restSum (8) -> valid.
     *   reduced = 9 % 8 = 1.
     *   reduced != 0 -> push 1 back. maxHeap = {8, 3, 1... } i.e. {3, 5, 1} plus the untouched
     *   values -> effectively {3, 5, 1}. totalSum = 8 + 1 = 9.
     *   (Working values now conceptually: [1, 3, 5], since 9 -> 1.)
     *
     * Round 2:
     *   maxHeap top = 5. maxValue = 5 (popped). restSum = 9 - 5 = 4.
     *   restSum != 1; maxValue (5) > restSum (4) -> valid.
     *   reduced = 5 % 4 = 1.
     *   reduced != 0 -> push 1 back. Working values now: [1, 3, 1]. totalSum = 4 + 1 = 5.
     *
     * Round 3:
     *   maxHeap top = 3. maxValue = 3 (popped). restSum = 5 - 3 = 2.
     *   restSum != 1; maxValue (3) > restSum (2) -> valid.
     *   reduced = 3 % 2 = 1.
     *   reduced != 0 -> push 1 back. Working values now: [1, 1, 1]. totalSum = 2 + 1 = 3.
     *
     * Loop condition check: maxHeap.peek() == 1 -> loop terminates.
     *
     * Return true.
     *
     * This matches forward intuition: [9,3,5] is indeed constructible (verified by both the
     * brute-force oracle and the optimal solution during development-time fuzz testing).
     */

    /*
     * ========================================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================================
     *
     * - The naive forward simulation is conceptually the most direct reading of the problem,
     *   but explodes exponentially and is unusable at the given scale (n up to 1000, values up
     *   to 10^5) — it's only useful as a correctness oracle for small inputs during testing.
     * - Reversing the operation is the critical insight: the current maximum was always the
     *   last element written, and it always equals the sum of every other element at that
     *   moment, letting us walk backward toward the all-ones array.
     * - Using modulo instead of repeated subtraction (the "fast doubling" trick shared with the
     *   Euclidean GCD algorithm) is what brings the reduction rounds down from potentially
     *   O(maxValue) to O(log(maxValue)).
     * - A max-heap gives O(log n) access to the current maximum per round, versus O(n) with a
     *   plain linear scan — asymptotically better for large n, though both pass comfortably
     *   within the given constraints.
     * - Known assumptions / limitations of the final solution:
     *     - Assumes target values are always positive integers (validated defensively).
     *     - Uses `long` arithmetic defensively; not strictly required given the stated bounds
     *       (max total sum is 1000 * 10^5 = 10^8, safely within `int` range), but it costs
     *       nothing and future-proofs against constraint changes.
     *     - Returns only reachability (true/false), not the sequence of operations — a
     *       follow-up would be required to reconstruct the actual operation sequence.
     */

    /*
     * ========================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================================
     *
     * 1. "Can you reconstruct and print the actual sequence of operations (indices chosen) that
     *     transforms all-ones into target, not just true/false?"
     *     -> Record each (index, value) reduction during the reverse pass, then replay the
     *        recorded operations in reverse order to get the forward sequence.
     *
     * 2. "What if target[i] could be as large as 10^18 instead of 10^5?"
     *     -> The algorithm is already logarithmic in the max value, so it still works; just
     *        need to use `long` (or BigInteger if values could exceed 64-bit range) throughout,
     *        which the given implementation already does defensively.
     *
     * 3. "What if n could be up to 10^6 instead of 1000?"
     *     -> Still fine: O(n log n) heap construction and O(log(maxValue) * log n) reduction
     *        rounds both scale gracefully; the bottleneck would shift to heap operation
     *        constant factors, potentially motivating a specialized array-based binary heap
     *        instead of Java's boxed `PriorityQueue<Long>` to reduce autoboxing overhead.
     *
     * 4. "Can you solve this without extra space, i.e., O(1) auxiliary space?"
     *     -> Approach 2 (linear scan for the max, mutating the input array in place) achieves
     *        O(1) auxiliary space at the cost of O(n) per round instead of O(log n).
     *
     * 5. "What if the operation allowed setting arr[i] to x - arr[i] instead of x (i.e.
     *     excluding the current element from the sum)? How would the algorithm change?"
     *     -> This changes the invariant: the "last written" element would satisfy a different
     *        arithmetic relationship, requiring the reverse-derivation to be reworked from
     *        scratch based on the new operation's semantics.
     *
     * 6. "How would you test this solution thoroughly, given how easy it is to get the
     *     boundary conditions (restSum == 0, restSum == 1, reduced == 0) wrong?"
     *     -> Cross-validate against a brute-force BFS oracle on small random inputs (exactly as
     *        done in the test harness below), plus targeted edge cases (n=1, all-ones target,
     *        adversarial "just barely invalid" arrays like [1,1,1,2]).
     */

    /*
     * ========================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================================
     *
     * 1. Forgetting the n == 1 special case: with a single element, the operation is a
     *    permanent no-op (sum of one element is itself), so target must be exactly [1]. Missing
     *    this causes an infinite loop or wrong answer for [k] where k != 1.
     *
     * 2. Not short-circuiting when restSum == 1: without this shortcut, the algorithm is still
     *    *correct* (it will eventually reduce the maximum down via modulo), but many naive
     *    implementations instead loop by literal subtraction, which can degrade to O(maxValue)
     *    iterations instead of O(log(maxValue)) — a silent performance bug that only shows up
     *    on large adversarial inputs, not on small hand-written test cases.
     *
     * 3. Off-by-one on the "last-write must exceed the rest" check: it's tempting to write
     *    `maxValue < restSum` as the failure condition instead of `maxValue <= restSum`. But if
     *    maxValue == restSum, the modulo reduction would produce exactly 0, which is invalid —
     *    candidates who don't check this boundary correctly (or who forget the
     *    `reduced == 0 -> return false` check after modulo) will silently accept invalid arrays
     *    like [1,1,1,2], producing wrong answers.
     *
     * 4. Using `int` instead of `long` for the running total sum: with n up to 1000 and values
     *    up to 10^5, the maximum possible sum is 10^8, which technically still fits in `int`
     *    (max ~2.1 * 10^9) for THIS problem's constraints — but candidates often don't verify
     *    this bound out loud, and reflexively defaulting to `long` for any accumulating sum is
     *    the safer habit that also survives constraint changes (see Follow-Up #2).
     */

    /*
     * ========================================================================================
     * TEST HARNESS: hand-crafted edge cases + randomized fuzz trials vs. brute-force oracle
     * ========================================================================================
     */
    public static void main(String[] args) {
        System.out.println("=== Hand-crafted test cases ===");
        runCase(new int[]{9, 3, 5}, true);
        runCase(new int[]{1, 1, 1, 2}, false);
        runCase(new int[]{8, 5}, true);
        runCase(new int[]{1}, true);
        runCase(new int[]{5}, false);
        runCase(new int[]{1, 1, 1, 1}, true);
        runCase(new int[]{100000, 1}, true); // restSum==1 shortcut; reachable via 99999 unit increments
        runCase(new int[]{10, 1}, true);      // same pattern on a smaller scale

        System.out.println("\n=== Randomized fuzz trials vs. brute-force oracle ===");
        Random random = new Random(20260816L);
        int trials = 2000;
        int mismatches = 0;
        for (int trial = 0; trial < trials; trial++) {
            int n = 1 + random.nextInt(3); // keep n small (1..3) so brute force stays feasible
            int maxValue = new int[]{3, 5, 8, 12}[random.nextInt(4)];
            int[] target = new int[n];
            for (int i = 0; i < n; i++) {
                target[i] = 1 + random.nextInt(maxValue);
            }

            boolean expected = bruteForceReachable(target, 40);
            boolean actualHeap = isPossibleMaxHeap(target);
            boolean actualLinear = isPossibleLinearScan(target);
            boolean actualProduction = isPossible(target);

            if (expected != actualHeap || expected != actualLinear || expected != actualProduction) {
                mismatches++;
                System.out.printf(
                    "MISMATCH target=%s expected=%b heap=%b linear=%b production=%b%n",
                    Arrays.toString(target), expected, actualHeap, actualLinear, actualProduction);
            }
        }
        System.out.println("Fuzz trials: " + trials + ", mismatches: " + mismatches);

        System.out.println("\n=== Large-scale sanity run (n up to 1000, values up to 10^5) ===");
        Random largeRandom = new Random(42L);
        for (int trial = 0; trial < 20; trial++) {
            int n = 1 + largeRandom.nextInt(1000);
            int[] target = new int[n];
            for (int i = 0; i < n; i++) {
                target[i] = 1 + largeRandom.nextInt(100000);
            }
            long start = System.nanoTime();
            boolean result = isPossible(target);
            long elapsedMicros = (System.nanoTime() - start) / 1000;
            System.out.printf("n=%d -> result=%b (%d microseconds)%n", n, result, elapsedMicros);
        }
    }

    private static void runCase(int[] target, boolean expected) {
        boolean actual = isPossible(target);
        String status = (actual == expected) ? "PASS" : "FAIL";
        System.out.printf("[%s] target=%s expected=%b actual=%b%n",
            status, Arrays.toString(target), expected, actual);
    }
}

/**
 * ============================================================================
 * PROBLEM STATEMENT
 * ============================================================================
 * You are given an array target of n integers.
 * Starting from an array arr of size n where every element is 1, you may 
 * perform the following operation any number of times:
 * 1. Let x be the sum of all current elements in arr.
 * 2. Pick an index i and set arr[i] = x.
 * 
 * Your task is to return True if it’s possible to construct target from arr, 
 * otherwise return False.
 * 
 * CONSTRAINTS:
 * - n == target.length
 * - 1 <= n <= 1000
 * - 1 <= target[i] <= 10^5
 * 
 * ============================================================================
 * VISUALIZATION OF THE PROBLEM
 * ============================================================================
 * It is much easier to work BACKWARDS from the `target` array to the `[1, 1...]` array.
 * 
 * Forward Operation:
 * arr = [1, 1, 1] -> sum = 3 -> replace one 1 -> [1, 3, 1]
 * Note that the NEWLY added element (3) is strictly greater than the sum of 
 * the rest of the elements (1 + 1 = 2). This means the LARGEST element in the 
 * array is always the one that was most recently added.
 * 
 * Reverse Operation:
 * 1. Find the maximum element in the target. Let's call it M.
 * 2. Let the sum of all other elements be S (i.e., S = totalSum - M).
 * 3. The previous value of the element before M was formed must be (M - S).
 * 4. Replace M with (M - S). 
 * 5. Repeat until all elements are 1.
 * 
 * Optimization (Using Modulo):
 * If M is very large and S is very small (e.g., M = 100, S = 2), subtracting 
 * S repeatedly is slow. Since we subtract S until the element is less than S, 
 * we can just use the modulo operator: M_new = M % S.
 * 
 * Example 1: target = [9, 3, 5]
 * - Heap (max first): [9, 5, 3], Total Sum = 17
 * - Max = 9, RestSum = 17 - 9 = 8.
 * - Next value for 9 is 9 % 8 = 1.
 * - Heap: [5, 3, 1], Total Sum = 9
 * - Max = 5, RestSum = 9 - 5 = 4. 
 * - Next value for 5 is 5 % 4 = 1.
 * - Heap: [3, 1, 1], Total Sum = 5
 * - Max = 3, RestSum = 5 - 3 = 2.
 * - Next value for 3 is 3 % 2 = 1.
 * - Heap: [1, 1, 1]. All 1s! Return TRUE.
 * ============================================================================
 */
class ConstructTargetArray {

    /**
     * Java 14+ Record for concise, immutable test cases representation.
     */
    public record TestCase(int[] target, boolean expected) {}

    /**
     * ========================================================================
     * SOLUTION: MAX-HEAP (Priority Queue) with MODULO OPTIMIZATION
     * ========================================================================
     * EXPLANATION:
     * 1. Check base case for single element array. If n==1, target must be [1].
     * 2. Insert all elements into a Max-Heap and calculate the total sum.
     * 3. Loop as long as the maximum element (heap root) is strictly > 1:
     *    a. Extract the max element (M).
     *    b. Calculate the sum of the remaining elements (S = totalSum - M).
     *    c. Base Case / Shortcut: If S == 1, it's always possible (we can just 
     *       repeatedly subtract 1 until M becomes 1). Return true.
     *    d. Invalid conditions: If S == 0 (no other elements) or M <= S 
     *       (the max element isn't larger than the rest, which breaks the rule 
     *       of how elements are built forward), return false.
     *    e. Calculate the original value: nextM = M % S. 
     *    f. If nextM == 0, it means it would reduce to 0, but elements must be 
     *       >= 1. Return false.
     *    g. Push nextM back to the heap and update the total sum.
     * 
     * COMPLEXITY:
     * - Time: O(N + N * log N * log M) 
     *   Building the heap takes O(N log N). Because of the modulo operation, 
     *   the max element decreases by at least half at each step, making the 
     *   number of operations per element logarithmic in respect to its value.
     * - Space: O(N) to store the elements in the Max-Heap Priority Queue.
     * ========================================================================
     */
    public static boolean isPossible(int[] target) {
        // Edge case: length 1 array can only be valid if the element is 1
        if (target.length == 1) {
            return target[0] == 1;
        }

        // Max-Heap to track the largest element efficiently
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        long totalSum = 0;

        for (int num : target) {
            maxHeap.offer(num);
            totalSum += num;
        }

        // Keep simulating backward until the maximum element in the heap is 1
        while (maxHeap.peek() > 1) {
            int maxElement = maxHeap.poll();
            long restSum = totalSum - maxElement;

            // If the sum of the rest of the elements is 1, we can always reach 1
            // by subtracting 1 repeatedly. (e.g., [100, 1] -> [99, 1] -> ... -> [1, 1])
            if (restSum == 1) {
                return true;
            }

            // If restSum is 0 (can happen if n=1, caught earlier, but safe guard)
            // Or if maxElement is less than or equal to restSum, we can't step backward.
            // Example: [1, 1, 1, 2], max=2, restSum=3. 2 <= 3, impossible forward step!
            if (restSum == 0 || maxElement <= restSum) {
                return false;
            }

            // Reverse the addition step using modulo for speed
            int previousElement = (int) (maxElement % restSum);

            // An array element can never drop below 1
            if (previousElement == 0) {
                return false;
            }

            // Put the reduced element back into the heap and update total sum
            maxHeap.offer(previousElement);
            totalSum = restSum + previousElement;
        }

        return true;
    }

    /**
     * ========================================================================
     * MAIN METHOD: Executing and verifying the examples
     * ========================================================================
     */
    public static void main(String[] args) {
        List<TestCase> tests = List.of(
            new TestCase(new int[]{9, 3, 5}, true),
            new TestCase(new int[]{1, 1, 1, 2}, false), // [1,1,1,1] forward would be [1,1,1,4]
            new TestCase(new int[]{8, 5}, true),
            new TestCase(new int[]{2}, false), // Length 1 array must be exactly [1]
            new TestCase(new int[]{1}, true),
            new TestCase(new int[]{1, 1000000000}, true) // Tests large gaps, needs modulo efficiency
        );

        for (int i = 0; i < tests.size(); i++) {
            TestCase tc = tests.get(i);
            System.out.println("Test Case " + (i + 1) + ":");
            
            // Print target array snippet (truncated if too long)
            System.out.print("Target Array: [");
            for(int j=0; j < tc.target().length; j++) {
                System.out.print(tc.target()[j] + (j == tc.target().length - 1 ? "" : ", "));
            }
            System.out.println("]");
            
            System.out.println("Expected: " + tc.expected());
            
            // Execution
            boolean result = isPossible(tc.target().clone());
            System.out.println("Result:   " + result);
            System.out.println("Match?    " + (result == tc.expected() ? "✅" : "❌"));
            System.out.println("--------------------------------------------------");
        }
    }
}


class Solution {

    public boolean isPossible(int[] target) {

        // Max Heap → always process the largest element first
        PriorityQueue<Integer> maxHeap =
                new PriorityQueue<>(Comparator.reverseOrder());

        long totalSum = 0;

        // Build heap + calculate total sum
        for (int num : target) {
            maxHeap.offer(num);
            totalSum += num;
        }

        // Reverse simulation: reduce target → [1,1,1,...]
        while (true) {

            int max = maxHeap.poll();          // largest element
            long restSum = totalSum - max;     // sum of remaining elements

            /*
             * ✅ Base Case:
             * If the largest element is 1 OR remaining sum is 1,
             * we can always form the array of all 1s.
             */
            if (max == 1 || restSum == 1) {
                return true;
            }

            /*
             * ❌ Impossible Cases:
             * 1. restSum == 0 → only one element existed initially
             * 2. restSum >= max → cannot reach this configuration
             */
            if (restSum == 0 || restSum >= max) {
                return false;
            }

            /*
             * 🔑 Core Reverse Logic:
             * Instead of subtracting repeatedly:
             *   prev = max - restSum (slow)
             *
             * Use modulo to jump directly:
             *   prev = max % restSum
             */
            int prev = (int) (max % restSum);

            /*
             * ❌ If modulo becomes 0 → no valid previous state
             */
            if (prev == 0) {
                return false;
            }

            // Update total sum with the reduced value
            totalSum = restSum + prev;

            // Push reduced value back into heap
            maxHeap.offer(prev);
        }
    }
}
