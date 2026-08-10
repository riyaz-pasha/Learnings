import java.util.*;

/**
 * ================================================================================
 *  GOOGLE-STYLE MOCK INTERVIEW WALKTHROUGH
 *  Problem: "Task Scheduler" (LeetCode 621)
 * ================================================================================
 *  This single file documents the *entire* interview process end-to-end:
 *  understanding, clarification, examples, every viable approach (worst to best),
 *  a comparison table, the recommended approach, a production-quality deep dive,
 *  a manual dry run, a closing summary, follow-ups, and common candidate traps.
 *
 *  All code compiles under a modern JDK (21+/24+). Run main() to cross-validate
 *  every implementation against each other on a shared set of test cases.
 * ================================================================================
 */
class TaskScheduler {

    /* ============================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ============================================================================
     *
     * In my own words:
     *   I'm given a list of CPU tasks, each labeled with an uppercase letter
     *   A-Z (so at most 26 distinct task TYPES, though many tasks of the same
     *   type can repeat in the input). I also get a non-negative integer `n`,
     *   the mandatory "cooldown" — after running a task of a given type, I
     *   cannot run another task of that SAME type until at least `n` other
     *   intervals (tasks or idle slots) have passed.
     *
     *   Each task takes exactly 1 CPU interval (unit time). In any interval,
     *   the CPU either executes some task or sits idle. I may reorder tasks
     *   freely (the input order is NOT the execution order — I just need to
     *   decide a legal schedule). I must return the MINIMUM total number of
     *   intervals (including idle ones) needed to finish every task in the
     *   input, honoring the cooldown constraint for every task type.
     *
     * Inputs:
     *   - tasks: char[] (or String), 1 <= tasks.length <= 10^3, values in 'A'..'Z'
     *   - n: int, 0 <= n <= 100 (cooldown length between same-type tasks)
     *
     * Output:
     *   - A single int: the minimum number of CPU intervals to complete all tasks.
     *
     * Key implicit assumptions I'm calling out:
     *   - We only need the COUNT of intervals, not the actual schedule itself
     *     (though every approach below can be extended to reconstruct one).
     *   - "n other intervals" includes idle slots, not just other tasks — i.e.
     *     idle time also satisfies the cooldown.
     *   - There is exactly one CPU (single-core). No parallel execution.
     */


    /* ============================================================================
     * SECTION 2: CLARIFYING QUESTIONS  (with assumed answers)
     * ============================================================================
     *
     * Q1. Is `tasks` guaranteed non-empty and only uppercase A-Z, per constraints?
     *     A: Yes, per constraints (1 <= length <= 1000, uppercase letters only).
     *        I will still defensively validate null/empty in production code.
     *
     * Q2. Can n be 0? What does that mean operationally?
     *     A: Yes, 0 <= n <= 100. n = 0 means no cooldown at all — answer is
     *        simply tasks.length, since tasks can run back-to-back.
     *
     * Q3. Does "n intervals between same task" mean n intervals must ELAPSE
     *     (i.e., the next same task can run at time t + n + 1), or must the
     *     same task wait until it has been idle-or-other for n intervals?
     *     A: Standard LeetCode semantics: if task A runs at time t, the next
     *        occurrence of A can run no earlier than time t + n + 1. E.g. with
     *        n = 2: A _ _ A is legal (positions 0 and 3), A _ A is not.
     *
     * Q4. Do I need to return the actual schedule (sequence of tasks/idles),
     *     or just the count of intervals?
     *     A: Just the minimum count. I'll mention the schedule is easily
     *        derivable as an extension.
     *
     * Q5. Is there only a single CPU, or could there be multiple cores /
     *     concurrent execution?
     *     A: Single CPU — one task (or idle) per interval, strictly sequential.
     *
     * Q6. Are all 26 letters possible task types, or could the "alphabet" be
     *     larger (e.g., lowercase, digits, multi-char task IDs)?
     *     A: Only uppercase A-Z per constraints, so at most 26 distinct types.
     *        This lets me use a fixed-size array instead of a general hash map.
     *
     * Q7. Should the solution be optimized for time, space, or code clarity
     *     first, given this is an interview setting?
     *     A: I'll present a correctness-first simulation, then optimize to the
     *        best asymptotic and constant-factor solution, explaining the
     *        trade-off at each step — that's what's being evaluated.
     *
     * Q8. Are ties among task frequencies (e.g., two letters both being most
     *     frequent) something I need to handle explicitly?
     *     A: Yes — this is actually the crux of correctness. My formula must
     *        count how many task types share the maximum frequency, not just
     *        identify one of them.
     */


    /* ============================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ============================================================================
     *
     * Example 1 (normal case):
     *   tasks = "AAABBB", n = 2
     *   One optimal schedule: A B idle A B idle A B  -> 8 intervals.
     *   (A and B each need to be 3 apart from their own repeats; interleaving
     *    them with idle slots fills the gaps.)
     *   Expected output: 8
     *
     * Example 2 (edge case — no cooldown):
     *   tasks = "AAABBB", n = 0
     *   No cooldown needed at all, so we just run every task back-to-back:
     *   A A A B B B (or any order) -> 6 intervals.
     *   Expected output: 6  (this shows the "idle formula" must be capped
     *   below by tasks.length — no idle time is ever required when there
     *   are enough OTHER distinct tasks, or when n = 0.)
     *
     * Example 3 (boundary / tie-breaking case — many task types, no idle needed):
     *   tasks = "ABCDE", n = 2
     *   All distinct — plenty of "filler" tasks exist to cover any cooldown,
     *   so no idle time is ever forced: A B C D E -> 5 intervals.
     *   Expected output: 5  (demonstrates that when max frequency = 1, i.e.
     *   nothing repeats, the cooldown constraint is trivially satisfied.)
     *
     * Example 4 (boundary — extreme skew, heavy idling forced):
     *   tasks = "AAAAAAABC" (seven A's, one B, one C), n = 2
     *   A needs 2 slots of "breathing room" after every occurrence except the
     *   last. With only 2 filler tasks (B, C) available, most of that
     *   breathing room must be idle time.
     *   Expected output: 19  (worked out precisely in the Dry Run section).
     */


    /* ============================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     *  (naive -> optimal, annotated with paradigm, complexity, pros/cons, when to use)
     * ============================================================================ */


    /* --------------------------------------------------------------------------
     * APPROACH 1: Brute-Force Simulation via Max-Heap + Cooldown Queue
     * --------------------------------------------------------------------------
     * Core idea:
     *   Simulate the CPU tick by tick. At every interval, greedily run whichever
     *   AVAILABLE task type currently has the highest remaining count (this
     *   greedy choice is safe: running the most frequent task first leaves the
     *   most "room" to interleave everything else before it must repeat).
     *   Tasks that just ran get parked in a cooldown queue with the timestamp
     *   at which they become eligible again.
     *
     * Paradigm: Greedy + Priority Queue (max-heap) simulation, plus hashing
     *   (frequency counting via a fixed-size array standing in for a hash map).
     *
     * Time Complexity: O(T log 26), where T is the total number of intervals
     *   in the final schedule. Since the heap holds at most 26 elements, log 26
     *   is a constant, so effectively O(T) ~= O(N) for this problem's bounds
     *   (T is bounded by the same formula Approach 3 computes directly).
     * Space Complexity: O(26) for the heap + cooldown queue = O(1) relative to N.
     *
     * Pros:
     *   - Directly models the real-world process; easy to reason about and to
     *     extend into actually reconstructing the schedule.
     *   - Provably correct via the exchange-argument greedy justification
     *     (always doing the most-constrained task first never hurts).
     * Cons:
     *   - More code, more edge cases to get right live (heap re-insertion
     *     timing, cooldown queue draining) under interview pressure.
     *   - Slower constant factor than the closed-form formula for large T.
     *
     * When to use: Great as your FIRST correct solution to establish trust and
     *   to set up the reasoning that leads to the optimal formula. Also the
     *   right choice if the interviewer asks you to output the actual schedule.
     * -------------------------------------------------------------------------- */
    public static int leastIntervalBruteForceHeap(char[] tasks, int n) {
        // Frequency count via a fixed-size array (equivalent to hashing on 26 keys).
        int[] frequency = new int[26];
        for (char task : tasks) {
            frequency[task - 'A']++;
        }

        // Max-heap keyed on remaining count so we always schedule the most
        // frequent still-available task type first.
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        for (int count : frequency) {
            if (count > 0) {
                maxHeap.offer(count);
            }
        }

        int elapsedIntervals = 0;
        // Each entry: {remainingCount, intervalAtWhichItReenters}
        Queue<int[]> coolingDown = new LinkedList<>();

        while (!maxHeap.isEmpty() || !coolingDown.isEmpty()) {
            elapsedIntervals++;

            if (!maxHeap.isEmpty()) {
                int remainingAfterThisRun = maxHeap.poll() - 1;
                if (remainingAfterThisRun > 0) {
                    coolingDown.offer(new int[]{remainingAfterThisRun, elapsedIntervals + n});
                }
            }
            // else: nothing is eligible this tick -> CPU is idle (we still counted the interval).

            // Release every task whose cooldown expires exactly at this interval.
            while (!coolingDown.isEmpty() && coolingDown.peek()[1] == elapsedIntervals) {
                maxHeap.offer(coolingDown.poll()[0]);
            }
        }

        return elapsedIntervals;
    }


    /* --------------------------------------------------------------------------
     * APPROACH 2: Sorting-Based Greedy ("Chunking" / Idle-Slot Fill)
     * --------------------------------------------------------------------------
     * Core idea:
     *   Sort the 26 frequency counts descending. Picture laying out
     *   (maxFrequency - 1) full "rows" of width (n + 1), one row per repeat of
     *   the most frequent task, followed by a final partial row. This carves
     *   out (maxFrequency - 1) * n guaranteed idle slots. Then greedily use
     *   every OTHER task type to fill those idle slots instead of leaving them
     *   idle -- each other type can contribute at most (maxFrequency - 1) of
     *   its occurrences to the idle rows (one per row, since it also can't
     *   repeat within the same row without violating some other cooldown).
     *   Whatever idle capacity is left over after filling is unavoidable idle
     *   time.
     *
     * Paradigm: Sorting + Greedy layout reasoning.
     *
     * Time Complexity: O(N) to count + O(26 log 26) to sort, which is O(1)
     *   since 26 is a fixed constant -> overall O(N).
     * Space Complexity: O(26) = O(1) for the frequency/sorted arrays.
     *
     * Pros:
     *   - Very visual/intuitive to explain on a whiteboard ("rows and gaps").
     *   - Same asymptotic complexity as the optimal formula.
     * Cons:
     *   - The sort is unnecessary work: we only ever need the max and the
     *     count of ties at the max, not a full ordering of all 26 counts.
     *   - Slightly more code than the direct formula for the same result.
     *
     * When to use: A good middle ground if you want a visual "gap filling"
     *   narrative but don't want to reason through the max-only formula
     *   directly. In practice, prefer Approach 3 once you see the pattern.
     * -------------------------------------------------------------------------- */
    public static int leastIntervalSortingGreedy(char[] tasks, int n) {
        int[] frequency = new int[26];
        for (char task : tasks) {
            frequency[task - 'A']++;
        }

        // Sort descending so index 0 is the most frequent task type.
        Integer[] sortedFrequency = Arrays.stream(frequency).boxed().toArray(Integer[]::new);
        Arrays.sort(sortedFrequency, Collections.reverseOrder());

        int maxFrequency = sortedFrequency[0];
        // Guaranteed idle slots carved out by the most frequent task's cooldowns.
        int idleSlots = (maxFrequency - 1) * n;

        // Let every other task type fill as much of that idle capacity as it can.
        for (int i = 1; i < 26 && sortedFrequency[i] > 0; i++) {
            idleSlots -= Math.min(sortedFrequency[i], maxFrequency - 1);
        }

        idleSlots = Math.max(idleSlots, 0); // Can't have negative idle time.
        return tasks.length + idleSlots;
    }


    /* --------------------------------------------------------------------------
     * APPROACH 3 (OPTIMAL): Hashing + Closed-Form Math Formula
     * --------------------------------------------------------------------------
     * Core idea:
     *   We don't need the full sorted order of all 26 frequencies -- only two
     *   numbers matter: the maximum frequency (maxFrequency) and how many
     *   distinct task types share that maximum (countOfMaxFrequency).
     *
     *   The most frequent task type forces (maxFrequency - 1) "gaps" of width
     *   (n + 1) between its own occurrences, plus one final slot for its last
     *   occurrence. If MULTIPLE task types tie for the max frequency, each of
     *   them needs one slot in that same final row (they can't help fill each
     *   other's cooldown gaps since they're equally constrained), giving:
     *
     *       formulaBasedLength = (maxFrequency - 1) * (n + 1) + countOfMaxFrequency
     *
     *   This is a LOWER BOUND forced purely by the most-constrained task(s).
     *   However, if there are enough OTHER distinct task types to fill every
     *   gap with real work, no idle time is needed at all, and the true
     *   answer is simply tasks.length. Hence:
     *
     *       answer = max(tasks.length, formulaBasedLength)
     *
     * Paradigm: Greedy correctness argument + Hashing (frequency counting via
     *   fixed-size array in place of a HashMap<Character, Integer>).
     *
     * Time Complexity: O(N) -- one pass to count frequencies, one constant
     *   O(26) pass to find max, one constant O(26) pass to count ties.
     * Space Complexity: O(26) = O(1), independent of N.
     *
     * Pros:
     *   - Optimal asymptotic AND constant factor: no heap, no sort, no queue.
     *   - Short, easy to verify, easy to state as a closed-form proof.
     * Cons:
     *   - The correctness proof (why the "chunking" lower bound is also
     *     achievable) is non-trivial to derive from scratch under pressure --
     *     it's the kind of thing you want to have reasoned through before
     *     the interview, or build up to via Approaches 1 and 2.
     *   - Doesn't directly give you the schedule itself (only the count).
     *
     * When to use: This is the production-quality answer -- optimal in every
     *   dimension, and what you should converge on for the final interview
     *   answer after demonstrating the reasoning behind it.
     * -------------------------------------------------------------------------- */
    public static int leastIntervalOptimal(char[] tasks, int n) {
        int[] frequency = new int[26];
        for (char task : tasks) {
            frequency[task - 'A']++;
        }

        int maxFrequency = 0;
        for (int count : frequency) {
            maxFrequency = Math.max(maxFrequency, count);
        }

        int countOfMaxFrequency = 0;
        for (int count : frequency) {
            if (count == maxFrequency) {
                countOfMaxFrequency++;
            }
        }

        int formulaBasedLength = (maxFrequency - 1) * (n + 1) + countOfMaxFrequency;
        return Math.max(tasks.length, formulaBasedLength);
    }


    /* ============================================================================
     * SECTION 6: PARADIGMS CONSIDERED AND RULED OUT
     * ============================================================================
     *
     * - Two Pointer / Sliding Window: Not applicable. There's no contiguous
     *   subarray/window property being expanded or shrunk -- the problem is
     *   about scheduling a whole multiset of tasks under a global cooldown
     *   constraint, not about a moving range over the input.
     *
     * - Divide and Conquer: Not applicable. There's no clean way to split the
     *   task multiset into independent subproblems that recombine cheaply --
     *   every task type's placement interacts with every other type through
     *   the shared timeline.
     *
     * - Dynamic Programming: Not needed. A DP over (time elapsed, per-type
     *   cooldown state) is theoretically expressible but has an intractable
     *   state space for no benefit, since the greedy "chunking" argument
     *   yields a proven-optimal closed-form answer in O(N). DP would be
     *   strictly worse here and is a common over-engineering trap.
     *
     * - Tree / Graph Traversal: Not applicable. There's no hierarchical or
     *   graph relationship between tasks to traverse.
     *
     * - Binary Search: Not the natural fit. One COULD binary search on the
     *   candidate answer T and check feasibility in O(26) per check, but that
     *   adds a log factor for no gain when a direct O(N) formula exists.
     *
     * - Monotonic Stack / Deque: Not applicable. There's no need to maintain
     *   a monotonic ordering over a positional sequence; the decision is
     *   driven purely by aggregate frequency counts.
     *
     * - Trie / Segment Tree: Not applicable. The alphabet is fixed at 26
     *   symbols, handled trivially with a fixed-size array -- no prefix or
     *   range-query structure is needed.
     */


    /* ============================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ============================================================================
     *
     * Approach                          | Time         | Space  | Best For                        | Limitations
     * ----------------------------------|--------------|--------|---------------------------------|--------------------------------------------
     * 1. Brute-Force Heap Simulation    | O(T log 26)  | O(26)  | Proving correctness; producing   | Larger constant factor; more code/edge
     *    (Greedy + PQ)                  | ~= O(N)      |        | the actual schedule, not just    | cases to get right live.
     *                                   |              |        | the count.                       |
     * ----------------------------------|--------------|--------|---------------------------------|--------------------------------------------
     * 2. Sorting-Based Greedy           | O(N +        | O(26)  | Whiteboard-friendly "rows and    | Sorting is unnecessary work; we only ever
     *    (Chunking)                     | 26 log 26)   |        | gaps" visualization.             | need the max and its tie count.
     *                                   | ~= O(N)      |        |                                   |
     * ----------------------------------|--------------|--------|---------------------------------|--------------------------------------------
     * 3. Hashing + Math Formula         | O(N)         | O(26)  | Final production answer: optimal | Requires a clear correctness proof for the
     *    (OPTIMAL)                      |              | = O(1) | in time, space, and code size.   | chunking lower bound / achievability.
     */


    /* ============================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
     * ============================================================================
     *
     * I would present Approach 3 (Hashing + Math Formula) as my FINAL answer,
     * but I would not jump straight to it cold. My interview strategy:
     *
     *   1. State the brute-force simulation (Approach 1) out loud as the
     *      obviously-correct baseline, to show I understand the mechanics of
     *      the cooldown constraint and can reason about greedy correctness
     *      (always run the most-constrained task first).
     *   2. Observe that the simulation's behavior has a predictable SHAPE --
     *      the most frequent task dictates a lower bound on idle time -- and
     *      derive the chunking argument (Approach 2) as the bridge from
     *      "simulate it" to "compute it directly."
     *   3. Recognize that sorting all 26 buckets is overkill -- only the max
     *      and the count of ties at the max matter -- and collapse to the
     *      O(N), O(1)-extra-space formula (Approach 3).
     *
     * This progression demonstrates exactly what interviewers are evaluating:
     * not just landing on the optimal answer, but the reasoning path that
     * gets there, plus the judgment to proactively pitch the optimization
     * before being asked "can you do better?" I'd code Approach 3 as my
     * submitted solution, with Approach 1 sketched verbally/on the whiteboard
     * as justification.
     */


    /* ============================================================================
     * SECTION 9: DEEP DIVE -- PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ============================================================================ */

    /**
     * Computes the minimum number of CPU intervals required to execute all
     * given tasks, honoring a mandatory cooldown of {@code n} intervals
     * between any two executions of the same task type.
     *
     * <p><b>Algorithm:</b> Frequency-count the tasks (fixed 26-slot array,
     * since inputs are restricted to 'A'-'Z'). The task type(s) with the
     * highest frequency dictate a hard lower bound on the schedule length:
     * {@code (maxFrequency - 1) * (n + 1) + countOfMaxFrequency}. If there
     * are enough other distinct task types to fill every cooldown gap with
     * real work, no idle slots are needed at all, so the true answer is the
     * larger of that lower bound and simply {@code tasks.length}.
     *
     * @param tasks array of uppercase task-type characters ('A'-'Z'); must be
     *              non-null and non-empty per problem constraints
     * @param n     mandatory cooldown between repeats of the same task type;
     *              must be non-negative
     * @return the minimum number of CPU intervals (including any forced idle
     *         slots) needed to complete every task
     * @throws IllegalArgumentException if {@code tasks} is null/empty or
     *                                  {@code n} is negative
     */
    public static int leastIntervalProduction(char[] tasks, int n) {
        // --- Defensive validation: fail fast on contract violations. ---
        if (tasks == null || tasks.length == 0) {
            throw new IllegalArgumentException("tasks must be non-null and non-empty");
        }
        if (n < 0) {
            throw new IllegalArgumentException("n (cooldown) must be non-negative");
        }

        // --- Step 1: Frequency count. Fixed-size array stands in for a hash
        //     map since the alphabet is provably limited to 26 uppercase
        //     letters -- this avoids boxing/hashing overhead entirely. ---
        final int ALPHABET_SIZE = 26;
        int[] taskTypeFrequency = new int[ALPHABET_SIZE];
        for (char task : tasks) {
            taskTypeFrequency[task - 'A']++;
        }

        // --- Step 2: Identify the maximum frequency among all task types.
        //     This is the single most-constrained task type -- it dictates
        //     the tightest lower bound on total schedule length. ---
        int maxFrequency = 0;
        for (int frequency : taskTypeFrequency) {
            if (frequency > maxFrequency) {
                maxFrequency = frequency;
            }
        }

        // --- Step 3: Count how many distinct task types are tied at that
        //     maximum frequency. Each tied type needs its own slot in the
        //     final "row" of the schedule, since they're equally
        //     constrained and can't absorb each other's cooldown gaps. ---
        int countOfMaxFrequency = 0;
        for (int frequency : taskTypeFrequency) {
            if (frequency == maxFrequency) {
                countOfMaxFrequency++;
            }
        }

        // --- Step 4: Compute the "chunking" lower bound. (maxFrequency - 1)
        //     full cycles of width (n + 1), plus one final partial row
        //     holding every tied max-frequency task type. ---
        int chunkingLowerBound = (maxFrequency - 1) * (n + 1) + countOfMaxFrequency;

        // --- Step 5: The true answer is never less than simply running
        //     every task once (no idle time is EVER required beyond what's
        //     forced by the most frequent type). ---
        return Math.max(tasks.length, chunkingLowerBound);
    }


    /* ============================================================================
     * SECTION 10: DRY RUN / TRACE (using the optimal solution)
     * ============================================================================
     *
     * Example: tasks = "AAAAAAABC" (A x7, B x1, C x1), n = 2
     *
     * Step 1 -- Frequency count (taskTypeFrequency array, only non-zero shown):
     *     'A' -> 7
     *     'B' -> 1
     *     'C' -> 1
     *   (all other 23 letters remain 0)
     *
     * Step 2 -- Find maxFrequency:
     *     Scan all 26 slots -> maxFrequency = 7  (from 'A')
     *
     * Step 3 -- Count ties at maxFrequency:
     *     Scan all 26 slots for frequency == 7 -> only 'A' matches
     *     countOfMaxFrequency = 1
     *
     * Step 4 -- Compute chunking lower bound:
     *     chunkingLowerBound = (maxFrequency - 1) * (n + 1) + countOfMaxFrequency
     *                        = (7 - 1) * (2 + 1) + 1
     *                        = 6 * 3 + 1
     *                        = 19
     *
     * Step 5 -- Compare against tasks.length:
     *     tasks.length = 9
     *     answer = max(9, 19) = 19
     *
     * Sanity-check via a concrete legal schedule (A B C idle idle repeated):
     *     A B C _ _ A B C _ _ A _ _ A _ _ A _ _ A
     *     positions:  0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18
     *   Every 'A' pair is exactly 3 apart (satisfies n = 2), and total
     *   length is 19, confirming the formula's output.
     *
     * Final trace result: leastIntervalProduction("AAAAAAABC", 2) == 19
     */


    /* ============================================================================
     * SECTION 11: CLOSING SUMMARY
     * ============================================================================
     *
     * - All three approaches are correct and share the same O(N) asymptotic
     *   time complexity for this problem's bounds, but they differ sharply
     *   in constant factor and code complexity:
     *     Approach 1 (heap simulation) is the most "obviously correct" and
     *       the most extensible (it naturally generalizes to reconstructing
     *       the actual schedule), at the cost of more moving parts.
     *     Approach 2 (sorting) is a useful conceptual stepping stone but
     *       does strictly more work (a sort) than necessary.
     *     Approach 3 (math formula) is the leanest: O(N) time, O(1) extra
     *       space, and the fewest lines of code -- the right final answer.
     *
     * - Key assumption baked into the optimal formula: the alphabet is fixed
     *   at 26 uppercase letters. If that assumption changes (see Follow-Ups),
     *   the fixed-size array becomes a HashMap, but the formula's LOGIC is
     *   unchanged.
     *
     * - Known limitation: the optimal formula returns only the interval
     *   COUNT, not a concrete schedule. Reconstructing an actual schedule
     *   requires falling back to something like Approach 1's simulation (or
     *   directly materializing the "chunking" layout from Approach 2).
     */


    /* ============================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ============================================================================
     *
     * 1. "Can you now also return the actual schedule (not just the count)?"
     *    -> Extend Approach 1: instead of just tracking counts in the heap,
     *       track (taskChar, remainingCount) pairs and append the executed
     *       char (or '.' for idle) to an output list each tick.
     *
     * 2. "What if the task alphabet isn't limited to 26 uppercase letters
     *    (e.g., arbitrary strings as task IDs)?"
     *    -> Swap the fixed-size int[26] array for a HashMap<String, Integer>
     *       frequency count; the max/tie-count/formula logic is unchanged.
     *
     * 3. "What if different task TYPES have different cooldown periods
     *    (not a single global n)?"
     *    -> The clean closed-form formula breaks down because different
     *       types now impose different constraints; this pushes you back
     *       toward the heap-based simulation (Approach 1), now storing a
     *       per-type cooldown alongside each entry.
     *
     * 4. "What if we have multiple CPUs (k-way parallel execution)?"
     *    -> Generalizes to a scheduling problem across k machines; the
     *       single-formula approach no longer directly applies. You'd adapt
     *       the heap simulation to pop up to k tasks per tick (subject to
     *       their individual cooldowns) or model it as a more general
     *       load-balancing / interval-scheduling problem.
     *
     * 5. "Tasks arrive online/in a stream rather than all upfront -- can you
     *    still schedule optimally?"
     *    -> No longer possible to compute a global closed-form answer up
     *       front; you'd need an online/greedy heuristic (e.g., always run
     *       the currently-most-frequent-and-eligible task) which may not be
     *       provably optimal without knowing future arrivals.
     *
     * 6. "n can now be as large as 10^9 (or tasks.length up to 10^7) --
     *    does your solution still hold up?"
     *    -> The O(N) formula approach is unaffected by n's magnitude (n only
     *       appears in an O(1) arithmetic expression), but the heap
     *       simulation (Approach 1) would become far too slow since it
     *       ticks once per interval, T ~ maxFrequency * n -- reinforcing
     *       why the closed-form formula is the right production choice.
     */


    /* ============================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ============================================================================
     *
     * 1. Forgetting the outer max(tasks.length, formulaBasedLength) guard.
     *    Candidates often return the chunking formula directly, which is
     *    WRONG whenever there are enough distinct filler tasks to eliminate
     *    idle time entirely (see Example 3: "ABCDE", n=2 -> formula alone
     *    would need care to not under/over count; the guard is what
     *    guarantees correctness in the "no repeats" regime).
     *
     * 2. Off-by-one in the chunking formula: writing
     *    (maxFrequency) * (n + 1) instead of (maxFrequency - 1) * (n + 1).
     *    The "-1" matters because the LAST occurrence of the max-frequency
     *    task doesn't need a trailing cooldown gap after it.
     *
     * 3. Only tracking ONE task type at the max frequency instead of
     *    COUNTING TIES. If two or more letters share the max frequency
     *    (e.g., Example 1: A and B both appear 3 times), each needs its own
     *    slot in the final row -- missing this undercounts the answer.
     *
     * 4. Reaching for Dynamic Programming or an explicit search over
     *    schedules because the problem "smells" like scheduling/DP. This
     *    problem has a clean greedy/counting solution; over-engineering
     *    with DP wastes interview time and usually doesn't even lead to a
     *    correct or efficient solution given the state-space size.
     */


    /* ============================================================================
     * CROSS-VALIDATION TEST HARNESS
     * ============================================================================
     * Runs all three implementations against a shared set of test cases and
     * asserts they agree with each other and with hand-computed expected
     * values, exactly as I'd sanity-check my solution live in an interview.
     * ============================================================================ */
    public static void main(String[] args) {
        record TestCase(String tasks, int n, int expected) {}

        List<TestCase> testCases = List.of(
                new TestCase("AAABBB", 2, 8),   // Example 1: normal case
                new TestCase("AAABBB", 0, 6),   // Example 2: no cooldown
                new TestCase("ABCDE", 2, 5),    // Example 3: all distinct, no idle forced
                new TestCase("AAAAAAABC", 2, 19), // Example 4: extreme skew, heavy idling
                new TestCase("A", 5, 1),        // single task, large cooldown irrelevant
                new TestCase("AA", 0, 2),       // two identical tasks, zero cooldown
                new TestCase("AA", 1, 3),       // two identical tasks: A _ A
                new TestCase("AAAA", 3, 13)     // (4-1)*(3+1)+1 = 13, tasks.length=4 -> max=13
        );

        int passed = 0;
        for (TestCase testCase : testCases) {
            char[] taskArray = testCase.tasks().toCharArray();

            int resultBruteForce = leastIntervalBruteForceHeap(taskArray, testCase.n());
            int resultSorting = leastIntervalSortingGreedy(taskArray, testCase.n());
            int resultOptimal = leastIntervalOptimal(taskArray, testCase.n());
            int resultProduction = leastIntervalProduction(taskArray, testCase.n());

            boolean allAgree = resultBruteForce == testCase.expected()
                    && resultSorting == testCase.expected()
                    && resultOptimal == testCase.expected()
                    && resultProduction == testCase.expected();

            System.out.printf(
                    "tasks=%-12s n=%-3d expected=%-4d brute=%-4d sorting=%-4d optimal=%-4d production=%-4d -> %s%n",
                    testCase.tasks(), testCase.n(), testCase.expected(),
                    resultBruteForce, resultSorting, resultOptimal, resultProduction,
                    allAgree ? "PASS" : "FAIL"
            );

            if (allAgree) {
                passed++;
            }
        }

        System.out.printf("%n%d / %d test cases passed across all four implementations.%n",
                passed, testCases.size());
    }
}
