import java.util.*;

/**
 * ============================================================================
 * MOCK GOOGLE INTERVIEW — "Maximize Capital (IPO)"
 * ============================================================================
 * This single file walks through the full interview arc for this problem:
 * restatement, clarifications, examples, every viable approach (brute force
 * through optimal), a comparison table, the recommended approach, a
 * production-quality deep dive, a manual trace, a closing summary, follow-up
 * questions, and common candidate mistakes.
 *
 * Compile & run locally with:
 *   javac MaximizeCapitalIPO.java && java MaximizeCapitalIPO
 * ============================================================================
 */
class MaximizeCapitalIPO {

    /* ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     * In my own words:
     *
     *   I'm an investor with a starting pot of capital `c`. I'm handed a list
     *   of `n` candidate projects. Project i costs `capital[i]` to *unlock*
     *   (i.e. I need at least that much cash on hand to start it) and, once
     *   completed, pays out `profits[i]`, which is added to my cash on hand.
     *   I may complete at most `k` DISTINCT projects (no repeats), chosen and
     *   ordered however I like, as long as at the moment I start each project
     *   my current capital is >= that project's capital requirement.
     *
     *   Goal: choose which projects to do (up to k of them) and the order to
     *   do them in, to maximize my final capital.
     *
     * Key inputs:
     *   - int[] profits   (length n, profits[i] >= 0)
     *   - int[] capital   (length n, capital[i]  >= 0)
     *   - int   k         (max number of projects, 1 <= k <= 1000)
     *   - long  c         (starting capital, 0 <= c <= 1e9)
     *
     * Output:
     *   - A single integer/long: the maximum achievable final capital.
     *
     * Explicit constraints given:
     *   1 <= k <= 1000
     *   0 <= c <= 1e9
     *   1 <= n <= 1000, n == profits.length == capital.length
     *   0 <= profits[i] <= 1e4
     *   0 <= capital[i] <= 1e9
     *   Answer guaranteed to fit in a 32-bit signed integer.
     *
     * Implicit assumption worth flagging out loud: this is EXACTLY LeetCode
     * 502 "IPO" — I'll say so if I recognize it, then proceed to solve it
     * properly rather than just reciting a memorized answer.
     * ======================================================================== */


    /* ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (asked aloud, with assumed answers)
     * ========================================================================
     * 1. Q: Can I do FEWER than k projects if that's optimal (e.g. all
     *       remaining projects are unaffordable)?
     *    A: Yes — "at most k" is explicit in the problem. Stopping early when
     *       no project is affordable is expected, not an error.
     *
     * 2. Q: Are project profits and capitals guaranteed non-negative?
     *    A: Yes, per constraints (profits[i] >= 0, capital[i] >= 0). I don't
     *       need to defend against negative profit projects logically, but
     *       I'll still validate inputs defensively in production code.
     *
     * 3. Q: Can a project have capital requirement 0? Can profit be 0?
     *    A: Yes to both — these are valid edge values I must handle (a free
     *       project, or a project that doesn't change capital).
     *
     * 4. Q: If starting capital c already exceeds every capital[i], do I just
     *       greedily take the k highest-profit projects?
     *    A: Yes — that falls out naturally from the general algorithm, but
     *       it's a useful special case to sanity-check my solution against.
     *
     * 5. Q: Are project capital requirements or profits unique, or can there
     *       be duplicates/ties?
     *    A: Duplicates are allowed and common; my solution must not assume
     *       distinct values. Tie-breaking on which equal-profit project to
     *       pick doesn't affect the final capital value, so any consistent
     *       tie-break is fine.
     *
     * 6. Q: Should I return the max capital only, or also which projects were
     *       chosen / the order?
     *    A: For this problem, return only the final maximum capital (int).
     *       I'll note in follow-ups how I'd extend to also return the chosen
     *       indices.
     *
     * 7. Q: What are realistic bounds on n and k for complexity purposes?
     *    A: n, k <= 1000, so anything up to roughly O(n^2) or
     *       O((n + k) log n) is comfortably fast; exponential subset
     *       enumeration (2^n or n^k) is NOT acceptable except as a brute
     *       force baseline discussed for correctness, not submitted as final.
     *
     * 8. Q: Is this single-threaded / no concurrency concerns for this
     *       exercise?
     *    A: Correct — assume single investor, single-threaded execution. I'll
     *       address concurrency only if asked in follow-ups.
     * ======================================================================== */


    /* ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     * Example A (normal case):
     *   profits = [1, 2, 3], capital = [0, 1, 1], k = 2, c = 0
     *   - Only project 0 (capital 0) is affordable initially -> take it,
     *     profit 1, capital becomes 1.
     *   - Now capital 1 unlocks projects 1 (capital 1, profit 2) and
     *     2 (capital 1, profit 3) -> take the higher profit, project 2.
     *   - Capital becomes 1 + 3 = 4. k exhausted (2 projects done).
     *   - Expected output: 4
     *
     * Example B (edge case — starting capital already covers everything):
     *   profits = [5, 2, 8], capital = [0, 0, 0], k = 1, c = 100
     *   - Every project is affordable immediately; with k = 1 I simply take
     *     the single highest-profit project (8).
     *   - Expected output: 108
     *
     * Example C (boundary / tie & starvation case):
     *   profits = [3, 3], capital = [5, 5], k = 5, c = 0
     *   - Starting capital 0 can't afford ANY project (both need 5).
     *   - Even though k = 5 allows up to 5 projects, zero are ever
     *     affordable, so we must stop immediately rather than looping
     *     forever or crashing on an empty "candidates" structure.
     *   - Expected output: 0 (unchanged — no project ever becomes reachable)
     *   This also covers the tie case: profits are equal (3 and 3), so
     *   picking either first is fine — the final answer is order-independent
     *   here since both would need capital we never reach anyway.
     * ======================================================================== */


    /* ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES (paradigm sweep)
     * ========================================================================
     * Paradigms explicitly ruled OUT, with justification (stated in the
     * interview before diving into code, to show breadth of consideration):
     *
     *  - Two pointer / sliding window: There's no contiguous "window" over an
     *    ordered sequence whose validity we're shrinking/growing — project
     *    selection isn't positional. Not applicable in its classic form. (A
     *    single advancing pointer over capital-sorted projects DOES appear
     *    inside the optimal solution, but as a monotonic-frontier technique,
     *    not sliding-window.)
     *
     *  - Divide and conquer: There's no natural way to split the project list
     *    in half, solve independently, and cheaply merge — the "which
     *    projects are affordable" state depends on the running capital, which
     *    doesn't decompose across an arbitrary split. Not a good fit.
     *
     *  - Dynamic programming (knapsack-style): capital[i] and c range up to
     *    1e9, so any DP indexed by "current capital" is infeasible (no way to
     *    build a table with 1e9 states). A DP indexed by (project index,
     *    projects used) with capital gating still needs to track achievable
     *    capital as a *value*, not an index, so classic bounded-knapsack DP
     *    doesn't transfer cleanly. Not used.
     *
     *  - Tree / graph traversal: No graph/tree structure is implied by the
     *    problem (no dependencies between projects). Not applicable.
     *
     *  - Binary search (as a primary paradigm): Binary search could locate
     *    "how many sorted-by-capital projects are affordable so far" inside a
     *    scan, but it doesn't replace the core greedy+heap strategy, and
     *    plain linear advancement of a pointer already achieves the same
     *    result in the same overall complexity. Mentioned as a minor
     *    optimization variant, not a standalone approach.
     *
     *  - Monotonic stack/deque: There's no notion of "pop while smaller/
     *    larger than current" over a sequence that applies here. Not
     *    applicable.
     *
     *  - Trie / segment tree: No prefix/range-query structure is needed; a
     *    segment tree *could* replace the heap to support range-max queries
     *    over capital-sorted profits, but it's strictly more machinery for no
     *    complexity benefit here. Not used.
     *
     * Paradigms genuinely applicable, covered below as full approaches:
     *   - Brute force / backtracking over subsets
     *   - Sorting-based + greedy linear rescanning
     *   - Sorting-based + greedy with a max-heap (priority queue)  <-- OPTIMAL
     * ======================================================================== */


    /* ------------------------------------------------------------------------
     * APPROACH 1: Brute Force (Backtracking over all subsets of size <= k)
     * ------------------------------------------------------------------------
     * Core idea:
     *   At each of up to k "rounds", try EVERY still-unused, currently
     *   affordable project as the next pick, recurse, and take the best
     *   result over all branches. This explores the full decision tree of
     *   which projects to pick and in what order.
     *
     * Paradigm: exhaustive recursive search / backtracking.
     *
     * Time Complexity: O(n^k) in the worst case — at each of k levels we may
     *   branch into up to n choices. For n = k = 1000 this is astronomically
     *   infeasible; even for n = k = 10 it's 10^10 branches. Only usable for
     *   tiny inputs (e.g. as an oracle to validate faster solutions).
     * Space Complexity: O(k) for recursion depth (plus O(n) for a "used"
     *   marker array).
     *
     * Pros:
     *   - Trivially, obviously correct — a great way to open the interview
     *     and to build a validation oracle for randomized testing.
     *   - No tricky invariants; easy to reason about.
     * Cons:
     *   - Exponential blow-up, useless at real input sizes (n, k up to 1000).
     * When to use:
     *   - Never in production for this size. Useful only as a correctness
     *     oracle for fuzz-testing the optimal solution on tiny random cases.
     * ------------------------------------------------------------------------ */
    static long bruteForceMaxCapital(int[] profits, int[] capital, int k, long startingCapital) {
        int n = profits.length;
        boolean[] used = new boolean[n];
        return bruteForceHelper(profits, capital, used, k, startingCapital);
    }

    private static long bruteForceHelper(int[] profits, int[] capital, boolean[] used,
                                          int projectsRemaining, long currentCapital) {
        if (projectsRemaining == 0) {
            return currentCapital;
        }
        long best = currentCapital; // doing nothing further is always a valid option
        for (int i = 0; i < profits.length; i++) {
            if (!used[i] && capital[i] <= currentCapital) {
                used[i] = true;
                long candidate = bruteForceHelper(
                        profits, capital, used, projectsRemaining - 1, currentCapital + profits[i]);
                best = Math.max(best, candidate);
                used[i] = false; // backtrack
            }
        }
        return best;
    }


    /* ------------------------------------------------------------------------
     * APPROACH 2: Sort by Capital + Repeated Linear Scan (no heap)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Sort projects by capital requirement ascending. For each of the k
     *   rounds, linearly scan all NOT-YET-USED projects whose capital
     *   requirement is <= current capital, and greedily pick the one with
     *   the highest profit among them. The GREEDY CLAIM (proved below in the
     *   optimal approach's discussion) is: at every step, taking the highest
     *   -profit AFFORDABLE project is always at least as good as any other
     *   choice, because affordability only grows monotonically with capital,
     *   so nothing is ever "lost" by taking the best available profit now.
     *
     * Paradigm: sorting + greedy.
     *
     * Time Complexity: O(n log n) to sort, then O(k * n) for the k rounds of
     *   linear scanning -> O(n log n + k * n). With n, k <= 1000 this is at
     *   most ~10^6 operations — fast enough in practice, but asymptotically
     *   worse than the heap approach when k and n are both large.
     * Space Complexity: O(n) for the sorted copy / used-flags.
     *
     * Pros:
     *   - Much simpler to write correctly under interview pressure than a
     *     heap-based two-pointer scan — fewer moving parts.
     *   - Still well within time limits for the given constraints.
     * Cons:
     *   - Re-scans already-known-affordable projects every round instead of
     *     remembering them — wasted work. Doesn't scale if constraints were
     *     tightened (e.g. n, k up to 10^5).
     * When to use:
     *   - A reasonable "first working solution" to state and code quickly,
     *     then explicitly improve upon — a good interview narrative move if
     *     time allows implementing two versions.
     * ------------------------------------------------------------------------ */
    static long greedyLinearScanMaxCapital(int[] profits, int[] capital, int k, long startingCapital) {
        int n = profits.length;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        // Sort indices by capital requirement ascending.
        Arrays.sort(order, Comparator.comparingInt(i -> capital[i]));

        boolean[] used = new boolean[n];
        long currentCapital = startingCapital;

        for (int round = 0; round < k; round++) {
            int bestIndex = -1;
            int bestProfit = -1;
            for (int idx : order) {
                if (used[idx] || capital[idx] > currentCapital) continue;
                if (profits[idx] > bestProfit) {
                    bestProfit = profits[idx];
                    bestIndex = idx;
                }
            }
            if (bestIndex == -1) break; // nothing affordable -> stop early (Section 3, Example C)
            used[bestIndex] = true;
            currentCapital += profits[bestIndex];
        }
        return currentCapital;
    }


    /* ------------------------------------------------------------------------
     * APPROACH 3 (OPTIMAL): Sort by Capital + Max-Heap of Affordable Profits
     * ------------------------------------------------------------------------
     * Core idea:
     *   Sort projects by capital requirement ascending. Maintain a pointer
     *   that advances through this sorted list, pushing every project's
     *   PROFIT into a max-heap the moment its capital requirement becomes
     *   <= current capital (i.e. it "unlocks"). At each of the k rounds, pop
     *   the maximum profit off the heap (the best currently-affordable
     *   choice) and add it to capital — which may unlock more projects for
     *   the pointer to push next round. Stop early if the heap is ever empty
     *   with rounds remaining.
     *
     *   Why greedy is correct: among all currently affordable projects,
     *   taking the highest-profit one first is never worse than taking any
     *   other, because (a) capital only increases over time, so the set of
     *   "affordable" projects only grows — nothing becomes unaffordable
     *   later — and (b) we're allowed to eventually take ANY subset of
     *   currently-affordable projects in ANY order up to the same k-project
     *   budget, so exchanging a lower-profit pick for the highest-profit
     *   available pick strictly cannot decrease the final total, and can
     *   only help unlock more future projects sooner. This is a textbook
     *   exchange-argument greedy proof.
     *
     * Paradigm: sorting + greedy + heap / priority queue (monotonic
     *   affordability frontier via an advancing pointer).
     *
     * Time Complexity: O(n log n) to sort + O(n log n) total heap pushes
     *   (each project pushed at most once) + O(k log n) for k pops
     *   -> O((n + k) log n) overall. With n, k <= 1000 this is trivially
     *   fast (well under 10^4 heap operations).
     * Space Complexity: O(n) for the sorted array and the heap.
     *
     * Pros:
     *   - Asymptotically optimal for this problem shape; avoids the
     *     redundant re-scanning of Approach 2.
     *   - Clean two-phase mental model: "unlock" phase (pointer sweep) and
     *     "spend" phase (heap pop) — easy to explain on a whiteboard.
     * Cons:
     *   - Slightly more code/machinery than Approach 2 (need a heap +
     *     careful pointer bookkeeping); marginally higher constant factor
     *     for very small n.
     * When to use:
     *   - This is the version I'd present as my final answer in a real
     *     interview: it is the standard, expected optimal solution for
     *     LeetCode 502 "IPO" and demonstrates command of the
     *     sort + greedy + heap pattern Google interviewers look for.
     * ------------------------------------------------------------------------ */
    static long greedyHeapMaxCapital(int[] profits, int[] capital, int k, long startingCapital) {
        int n = profits.length;

        // Pair each project's (capital, profit) and sort ascending by capital.
        // Using a primitive-friendly int[][] avoids boxing overhead of Project objects
        // at this stage; we only wrap into a lightweight record for the heap itself.
        int[][] projectsByCapital = new int[n][2];
        for (int i = 0; i < n; i++) {
            projectsByCapital[i][0] = capital[i];
            projectsByCapital[i][1] = profits[i];
        }
        Arrays.sort(projectsByCapital, Comparator.comparingInt(project -> project[0]));

        // Max-heap on profit: every project pushed here is CURRENTLY affordable.
        PriorityQueue<Integer> affordableProfits = new PriorityQueue<>(Collections.reverseOrder());

        long currentCapital = startingCapital;
        int nextUnlockedIndex = 0; // pointer into projectsByCapital

        for (int round = 0; round < k; round++) {
            // "Unlock" phase: push every project whose capital requirement is now met.
            while (nextUnlockedIndex < n && projectsByCapital[nextUnlockedIndex][0] <= currentCapital) {
                affordableProfits.offer(projectsByCapital[nextUnlockedIndex][1]);
                nextUnlockedIndex++;
            }

            // "Spend" phase: no affordable project left -> can never unlock more, stop.
            if (affordableProfits.isEmpty()) {
                break;
            }
            currentCapital += affordableProfits.poll();
        }
        return currentCapital;
    }


    /* ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     * Approach                  | Time              | Space | Best For                      | Limitations
     * --------------------------|--------------------|-------|-------------------------------|---------------------------------
     * 1. Brute Force Backtrack  | O(n^k)             | O(k)  | Tiny n,k; correctness oracle  | Exponential; unusable at scale
     * 2. Sort + Linear Rescan   | O(n log n + k*n)   | O(n)  | Quick first-pass solution     | Re-scans wastefully every round
     * 3. Sort + Max-Heap (OPT)  | O((n+k) log n)     | O(n)  | Production / interview answer | Slightly more bookkeeping code
     * ======================================================================== */


    /* ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
     * ========================================================================
     * I would present Approach 3 (Sort + Max-Heap) as my final solution:
     *
     *   - It is asymptotically optimal for this problem shape and matches
     *     what a Google interviewer expects as the "target" solution for
     *     this exact problem family (greedy + heap over a sorted frontier).
     *   - It's fast to code confidently in ~10-15 minutes once the greedy
     *     insight is stated and justified via the exchange argument.
     *   - It cleanly demonstrates two paradigms at once (sorting/greedy AND
     *     heap/priority-queue), which interviewers like to see combined
     *     correctly.
     *   - I'd still mention Approach 2 verbally as "the simpler O(n log n +
     *     k*n) version I'd write first if I were worried about heap
     *     bookkeeping bugs under time pressure," to show I understand the
     *     trade-off between coding speed and asymptotic optimality — but I
     *     would code Approach 3 as my committed final answer.
     * ======================================================================== */


    /* ========================================================================
     * SECTION 9: DEEP DIVE — PRODUCTION-QUALITY OPTIMAL IMPLEMENTATION
     * ======================================================================== */

    /**
     * A single project's economics, used only inside the optimal solver so the
     * max-heap can be ordered directly by profit without index bookkeeping.
     *
     * @param capitalRequired minimum capital needed to start this project (>= 0)
     * @param profit          capital gained upon completing this project (>= 0)
     */
    private record Project(long capitalRequired, long profit) {}

    /**
     * Computes the maximum achievable capital after completing at most
     * {@code maxProjects} distinct projects, starting from
     * {@code startingCapital}, where project {@code i} requires capital
     * {@code capital[i]} to start and yields {@code profits[i]} upon
     * completion.
     *
     * <p><b>Algorithm:</b> sort projects by capital requirement ascending.
     * Repeatedly, for up to {@code maxProjects} rounds: (1) push every
     * project that has just become affordable into a max-heap keyed by
     * profit, then (2) take the highest-profit affordable project. This is
     * optimal by an exchange argument: since affordability is monotonic in
     * capital (never revoked), always taking the best currently-available
     * profit dominates any alternative choice at that step.</p>
     *
     * @param profits         profit of each project, profits[i] >= 0
     * @param capital         capital required to start each project, capital[i] >= 0
     * @param maxProjects     maximum number of distinct projects to complete (k >= 1)
     * @param startingCapital initial capital available (>= 0)
     * @return the maximum capital achievable
     * @throws IllegalArgumentException if inputs are null, mismatched in length,
     *                                   or contain invalid (negative) values
     */
    static long maxCapitalOptimal(int[] profits, int[] capital, int maxProjects, long startingCapital) {
        // ---- Defensive input validation (production code should never trust callers blindly) ----
        if (profits == null || capital == null) {
            throw new IllegalArgumentException("profits and capital arrays must not be null");
        }
        if (profits.length != capital.length) {
            throw new IllegalArgumentException("profits and capital must be the same length");
        }
        if (profits.length == 0) {
            throw new IllegalArgumentException("at least one project must be provided");
        }
        if (maxProjects < 1) {
            throw new IllegalArgumentException("maxProjects (k) must be >= 1");
        }
        if (startingCapital < 0) {
            throw new IllegalArgumentException("startingCapital must be >= 0");
        }
        for (int i = 0; i < profits.length; i++) {
            if (profits[i] < 0 || capital[i] < 0) {
                throw new IllegalArgumentException("profits and capital values must be non-negative (index " + i + ")");
            }
        }

        int projectCount = profits.length;

        // Build immutable Project records and sort by capital requirement ascending.
        // This lets us sweep a single pointer forward as capital grows, never revisiting
        // a project once it has been considered for unlocking.
        Project[] projectsByCapital = new Project[projectCount];
        for (int i = 0; i < projectCount; i++) {
            projectsByCapital[i] = new Project(capital[i], profits[i]);
        }
        Arrays.sort(projectsByCapital, Comparator.comparingLong(Project::capitalRequired));

        // Max-heap of profits for all projects currently affordable but not yet completed.
        // We deliberately store only the profit (long) here: once a project is pushed,
        // its capital requirement is irrelevant — we've already confirmed it's affordable,
        // and it can never become "more" or "less" affordable again.
        PriorityQueue<Long> affordableProfits = new PriorityQueue<>(Collections.reverseOrder());

        long currentCapital = startingCapital;
        // Deliberate `long`, not `int`, even though the final answer is guaranteed to fit
        // in a signed 32-bit integer: intermediate sums during accumulation could otherwise
        // be a source of silent overflow bugs if this method were ever reused with looser
        // guarantees. This is a defensive choice, not an oversight.

        int nextUnlockedIndex = 0; // index into projectsByCapital of the next project to consider

        for (int projectsCompleted = 0; projectsCompleted < maxProjects; projectsCompleted++) {

            // Phase 1: unlock every project whose capital requirement is now satisfied.
            // Sentinel-free loop: nextUnlockedIndex naturally terminates the sweep,
            // so no magic "not found" sentinel value is needed here.
            while (nextUnlockedIndex < projectCount
                    && projectsByCapital[nextUnlockedIndex].capitalRequired() <= currentCapital) {
                affordableProfits.offer(projectsByCapital[nextUnlockedIndex].profit());
                nextUnlockedIndex++;
            }

            // Phase 2: if nothing is affordable, no future capital increase is possible
            // either (capital only grows via completed projects), so we can stop early
            // rather than looping through the remaining rounds doing nothing.
            if (affordableProfits.isEmpty()) {
                break;
            }

            // Greedily take the best currently-affordable project.
            currentCapital += affordableProfits.poll();
        }

        return currentCapital;
    }


    /* ========================================================================
     * SECTION 10: DRY RUN / TRACE (Example A from Section 3)
     * ========================================================================
     * Input: profits = [1, 2, 3], capital = [0, 1, 1], k = 2, c = 0
     *
     * Step 0 (setup):
     *   projectsByCapital sorted by capital ascending:
     *     [(capital=0, profit=1), (capital=1, profit=2), (capital=1, profit=3)]
     *     (original indices 0, then 1 and 2 — either order between the two
     *      capital=1 entries is fine since we sort a stable-enough comparator
     *      and correctness doesn't depend on their relative order here)
     *   currentCapital = 0, nextUnlockedIndex = 0, affordableProfits = []
     *
     * Round 1 (projectsCompleted = 0):
     *   Unlock phase: projectsByCapital[0].capitalRequired = 0 <= currentCapital(0) -> push profit 1
     *                 affordableProfits = [1]; nextUnlockedIndex = 1
     *                 projectsByCapital[1].capitalRequired = 1 > currentCapital(0) -> stop unlocking
     *   Spend phase: heap not empty -> poll max = 1
     *                currentCapital = 0 + 1 = 1
     *   State after round 1: currentCapital = 1, nextUnlockedIndex = 1, affordableProfits = []
     *
     * Round 2 (projectsCompleted = 1):
     *   Unlock phase: projectsByCapital[1].capitalRequired = 1 <= currentCapital(1) -> push profit 2
     *                 affordableProfits = [2]; nextUnlockedIndex = 2
     *                 projectsByCapital[2].capitalRequired = 1 <= currentCapital(1) -> push profit 3
     *                 affordableProfits = [3, 2]; nextUnlockedIndex = 3 (all unlocked)
     *   Spend phase: heap not empty -> poll max = 3
     *                currentCapital = 1 + 3 = 4
     *   State after round 2: currentCapital = 4, nextUnlockedIndex = 3, affordableProfits = [2]
     *
     * Loop ends: projectsCompleted reached maxProjects (k = 2).
     * Return currentCapital = 4.  Matches expected output from Section 3. ✔
     * ======================================================================== */


    /* ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     * - Brute force backtracking (O(n^k)) is correct but exponential; it only
     *   serves as a correctness oracle for small inputs during testing.
     * - Sort + linear rescan (O(n log n + k*n)) is a solid, simpler first
     *   working solution, comfortably within the given constraints (n, k <=
     *   1000), but does redundant re-scanning every round.
     * - Sort + max-heap (O((n+k) log n)) is the optimal, production-ready
     *   solution and the one I'd commit to in an interview: it eliminates
     *   the redundant rescanning by remembering which projects are already
     *   known-affordable in a heap, ordered so the best choice is always O(1)
     *   to access.
     * - Known assumptions/limitations of the final solution: it assumes all
     *   profits/capitals are non-negative (per constraints, and defensively
     *   validated); it returns only the final capital value, not which
     *   projects were chosen or in what order (see Follow-Up Question 5 for
     *   the extension); and it assumes single-threaded, non-streaming input
     *   (all projects known up front).
     * ======================================================================== */


    /* ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     * 1. "What if n and k could be up to 10^5 or 10^6 instead of 10^3?"
     *    -> The O((n+k) log n) heap solution already scales fine; I'd discuss
     *       whether I/O or memory (boxed Long in the heap) becomes the new
     *       bottleneck, and consider a primitive long-based heap to cut GC
     *       pressure.
     *
     * 2. "What if projects could have NEGATIVE profit (a project that costs
     *     you capital to complete)?"
     *    -> The greedy exchange argument still holds for choosing among
     *       affordable projects (never take a negative-profit one unless
     *       forced), but I'd need to explicitly skip negative-profit options
     *       instead of blindly popping the heap max each round if the max
     *       could ever be negative and taking it would reduce capital.
     *
     * 3. "Can you also return WHICH projects were selected, not just the
     *     final capital?"
     *    -> Yes: store original indices alongside profit in the heap entries
     *       (instead of a bare Long), and append each polled project's index
     *       to a result list as it's taken.
     *
     * 4. "What if the project list arrives as a stream and k can change at
     *     query time (multiple queries against the same project set)?"
     *    -> I'd separate "build" from "query": pre-sort once, and for each
     *       query replay only the unlock+spend loop from a fresh heap state,
     *       or maintain a persistent structure if queries share prefixes of
     *       capital growth (advanced: offline processing sorted by query
     *       starting capital).
     *
     * 5. "What if we must complete EXACTLY k projects, not 'at most k' — and
     *     it's acceptable/required to fail if that's impossible?"
     *    -> After the loop, check whether projectsCompleted reached k before
     *       the heap ran empty; if not, signal infeasibility (e.g. throw or
     *       return a sentinel/Optional) instead of silently returning a
     *       partial result.
     *
     * 6. "How would this change if multiple investors competed for the same
     *     projects (shared project pool, first-come-first-served)?"
     *    -> This becomes a fundamentally different combinatorial/allocation
     *       problem (closer to auction or matching theory) — greedy alone is
     *       no longer obviously optimal per-investor since taking a project
     *       now has an opportunity cost imposed on competitors; I'd flag this
     *       as out of scope for a single greedy pass and worth a separate
     *       design discussion.
     * ======================================================================== */


    /* ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     * 1. Using strict `<` instead of `<=` when checking affordability
     *    (`capital[i] <= currentCapital`). Off-by-one here silently excludes
     *    a project that is EXACTLY affordable — a classic boundary bug that
     *    passes most random tests but fails on exact-match capital cases
     *    (see Section 3's Example C-style boundary values).
     *
     * 2. Forgetting the early-exit when the heap is empty. Without it, the
     *    loop either throws (NoSuchElementException on an empty-queue poll)
     *    or silently does nothing useful for remaining iterations — better
     *    to break explicitly and make the "no more affordable projects"
     *    condition an intentional, visible branch.
     *
     * 3. Re-pushing or re-considering an already-unlocked project. Using a
     *    single monotonically-advancing pointer (nextUnlockedIndex) instead
     *    of re-scanning from index 0 each round is what separates the
     *    O((n+k) log n) solution from the accidentally-quadratic version —
     *    candidates under pressure often revert to re-scanning without
     *    noticing the asymptotic cost.
     *
     * 4. Using `int` for the running capital accumulator. Even though the
     *    FINAL answer is guaranteed to fit in a 32-bit signed int, candidates
     *    who default to `int` throughout are one constraint change away from
     *    an overflow bug; explicitly reasoning about and choosing `long` for
     *    intermediate accumulation (as done in Section 9) is the kind of
     *    detail that separates a strong candidate from an average one.
     * ======================================================================== */


    /* ========================================================================
     * TEST HARNESS — hand-crafted edge cases + randomized fuzz testing
     * against the brute-force oracle, cross-validating all three approaches.
     * ======================================================================== */
    public static void main(String[] args) {
        runHandCraftedTests();
        runRandomizedFuzzTests();
        System.out.println("All tests passed.");
    }

    private static void runHandCraftedTests() {
        // Example A: normal case
        check(4, new int[]{1, 2, 3}, new int[]{0, 1, 1}, 2, 0);

        // Example B: everything affordable immediately, k = 1
        check(108, new int[]{5, 2, 8}, new int[]{0, 0, 0}, 1, 100);

        // Example C: nothing ever affordable
        check(0, new int[]{3, 3}, new int[]{5, 5}, 5, 0);

        // Single project, exactly affordable at the boundary (capital == c)
        check(17, new int[]{10}, new int[]{7}, 1, 7);

        // k larger than n: should just take every affordable project once
        check(6, new int[]{1, 2, 3}, new int[]{0, 0, 0}, 10, 0);

        // Zero-profit and zero-capital projects mixed in
        check(5, new int[]{0, 5, 0}, new int[]{0, 0, 100}, 3, 0);

        System.out.println("Hand-crafted tests passed.");
    }

    private static void check(long expected, int[] profits, int[] capital, int k, long startingCapital) {
        long bruteResult = bruteForceMaxCapital(profits, capital, k, startingCapital);
        long linearScanResult = greedyLinearScanMaxCapital(profits, capital, k, startingCapital);
        long heapResult = greedyHeapMaxCapital(profits, capital, k, startingCapital);
        long optimalResult = maxCapitalOptimal(profits, capital, k, startingCapital);

        if (bruteResult != expected || linearScanResult != expected
                || heapResult != expected || optimalResult != expected) {
            throw new AssertionError(String.format(
                    "Mismatch for profits=%s capital=%s k=%d c=%d -> expected=%d brute=%d linear=%d heap=%d optimal=%d",
                    Arrays.toString(profits), Arrays.toString(capital), k, startingCapital,
                    expected, bruteResult, linearScanResult, heapResult, optimalResult));
        }
    }

    private static void runRandomizedFuzzTests() {
        Random random = new Random(42);
        int trials = 3000;
        for (int trial = 0; trial < trials; trial++) {
            // Keep n and k small here so the O(n^k) brute-force oracle stays tractable.
            int n = 1 + random.nextInt(6);
            int k = 1 + random.nextInt(4);
            long startingCapital = random.nextInt(11);

            int[] profits = new int[n];
            int[] capital = new int[n];
            for (int i = 0; i < n; i++) {
                profits[i] = random.nextInt(11);
                capital[i] = random.nextInt(11);
            }

            long bruteResult = bruteForceMaxCapital(profits, capital, k, startingCapital);
            long linearScanResult = greedyLinearScanMaxCapital(profits, capital, k, startingCapital);
            long heapResult = greedyHeapMaxCapital(profits, capital, k, startingCapital);
            long optimalResult = maxCapitalOptimal(profits, capital, k, startingCapital);

            if (bruteResult != linearScanResult || bruteResult != heapResult || bruteResult != optimalResult) {
                throw new AssertionError(String.format(
                        "Fuzz mismatch on trial %d: profits=%s capital=%s k=%d c=%d -> brute=%d linear=%d heap=%d optimal=%d",
                        trial, Arrays.toString(profits), Arrays.toString(capital), k, startingCapital,
                        bruteResult, linearScanResult, heapResult, optimalResult));
            }
        }
        System.out.println("Randomized fuzz tests passed (" + trials + " trials).");
    }
}

/**
 * ============================================================================
 * PROBLEM STATEMENT
 * ============================================================================
 * An investor is looking to maximize their capital by undertaking a set of 
 * profitable projects. Due to limited time and resources, they can complete 
 * at most k distinct projects.
 * 
 * There are n available projects. Each project i has:
 * - A profit of profits[i] earned upon completion.
 * - A minimum capital requirement of capitals[i] needed to start.
 * 
 * The investor starts with an initial capital of c. After completing a project, 
 * its profit is immediately added to the investor's current capital.
 * 
 * The goal is to choose up to k different projects in a way that maximizes the 
 * investor’s final capital. Return the maximum capital achievable.
 * 
 * CONSTRAINTS:
 * - 1 <= k <= 10^3
 * - 0 <= c <= 10^9
 * - n == profits.length == capitals.length
 * - 1 <= n <= 10^3
 * - 0 <= profits[i] <= 10^4
 * - 0 <= capitals[i] <= 10^9
 * 
 * ============================================================================
 * VISUALIZATION OF THE PROBLEM
 * ============================================================================
 * This is a classic Greedy Algorithm problem. 
 * Why greedy? Because all profits are non-negative. Doing a project only 
 * INCREASES our capital, which only UNLOCKS more potential projects. It never 
 * restricts our future choices. Therefore, our best move is always to pick the 
 * MOST profitable project we can currently afford.
 * 
 * Example: k = 2, c = 0, profits = [1, 2, 3], capitals = [0, 1, 1]
 * 
 * 1. Initial State: Capital = 0. We can do up to 2 projects.
 *    Affordable projects:
 *    - Project 0: capital 0, profit 1  <-- ONLY this one is affordable.
 * 
 * 2. Pick Project 0:
 *    New Capital = 0 + 1 = 1.
 *    Remaining k = 1.
 * 
 * 3. State: Capital = 1.
 *    Affordable projects (not yet done):
 *    - Project 1: capital 1, profit 2
 *    - Project 2: capital 1, profit 3  <-- Pick this one! Highest profit.
 * 
 * 4. Pick Project 2:
 *    New Capital = 1 + 3 = 4.
 *    Remaining k = 0.
 * 
 * Result: 4
 * ============================================================================
 */
class MaximizeCapital {

    /**
     * Using Java 14+ Records to tie a project's capital and profit together.
     * This makes sorting and mapping extremely clean.
     */
    public record Project(int capital, int profit) implements Comparable<Project> {
        @Override
        public int compareTo(Project other) {
            // Sort primarily by capital required in ascending order
            return Integer.compare(this.capital, other.capital);
        }
    }

    /**
     * Using Java Record for test cases to keep execution clean.
     */
    public record TestCase(int k, int initialCapital, int[] profits, int[] capitals, int expected) {}

    /**
     * ========================================================================
     * SOLUTION 1: SORTING + MAX-HEAP (Priority Queue) - MOST OPTIMAL
     * ========================================================================
     * EXPLANATION:
     * 1. Bind `profits` and `capitals` together into an array of `Project` objects.
     * 2. Sort the array of projects by their `capital` requirement ascending.
     * 3. Maintain a Max-Heap to store the `profits` of all projects we can 
     *    currently afford.
     * 4. Loop `k` times:
     *    - Iterate through the sorted projects and push the profits of all 
     *      projects that cost <= our current capital into the Max-Heap.
     *    - Once we've added all affordable projects to the heap, pop the top 
     *      of the Max-Heap (the most profitable affordable project).
     *    - Add that profit to our current capital.
     *    - If the Max-Heap is empty, it means we can't afford ANY more projects, 
     *      so we break early.
     * 
     * COMPLEXITY:
     * - Time: O(N log N + K log N). 
     *   Sorting N projects takes O(N log N).
     *   In the worst case, we push all N projects to the heap O(N log N).
     *   We pop from the heap up to K times O(K log N).
     * - Space: O(N) to store the projects array and the Max-Heap.
     * ========================================================================
     */
    public static int findMaximizedCapitalOptimal(int k, int c, int[] profits, int[] capitals) {
        int n = profits.length;
        Project[] projects = new Project[n];
        
        for (int i = 0; i < n; i++) {
            projects[i] = new Project(capitals[i], profits[i]);
        }
        
        // Sort projects by required capital
        Arrays.sort(projects);
        
        // Max-Heap to store the profits of affordable projects
        PriorityQueue<Integer> maxProfitHeap = new PriorityQueue<>((a, b) -> Integer.compare(b, a));
        
        int currentCapital = c;
        int projectIndex = 0;
        
        for (int i = 0; i < k; i++) {
            // Add all projects we can afford with our current capital to the heap
            while (projectIndex < n && projects[projectIndex].capital() <= currentCapital) {
                maxProfitHeap.offer(projects[projectIndex].profit());
                projectIndex++;
            }
            
            // If we can't afford any projects, we must stop
            if (maxProfitHeap.isEmpty()) {
                break;
            }
            
            // Do the most profitable project
            currentCapital += maxProfitHeap.poll();
        }
        
        return currentCapital;
    }

    /**
     * ========================================================================
     * SOLUTION 2: BRUTE FORCE (O(N * K))
     * ========================================================================
     * EXPLANATION:
     * Given the constraints state N <= 10^3 and K <= 10^3, an O(N * K) approach 
     * takes at most 1,000,000 operations. This is perfectly acceptable and will 
     * easily pass on platforms like LeetCode for these specific constraints.
     * 
     * 1. Loop `k` times.
     * 2. In each iteration, scan the entire array of projects.
     * 3. Find the project that is affordable (capital <= current_capital) AND 
     *    has the maximum profit.
     * 4. Add the max profit to current_capital.
     * 5. Mark the chosen project as used (e.g., set its capital requirement to 
     *    Infinity, or use a boolean array).
     * 6. If no affordable project is found in a pass, break early.
     * 
     * COMPLEXITY:
     * - Time: O(K * N). We do K passes, and in each pass, we check N items.
     * - Space: O(N) for a boolean array to track used projects (or O(1) if we 
     *   mutate the input, but we avoid mutating input here to be safe).
     * ========================================================================
     */
    public static int findMaximizedCapitalBruteForce(int k, int c, int[] profits, int[] capitals) {
        int n = profits.length;
        boolean[] used = new boolean[n];
        int currentCapital = c;
        
        for (int i = 0; i < k; i++) {
            int maxProfit = -1;
            int bestProjectIndex = -1;
            
            // Scan all projects to find the best affordable one
            for (int j = 0; j < n; j++) {
                if (!used[j] && capitals[j] <= currentCapital) {
                    if (profits[j] > maxProfit) {
                        maxProfit = profits[j];
                        bestProjectIndex = j;
                    }
                }
            }
            
            // If no project could be afforded, stop
            if (bestProjectIndex == -1) {
                break;
            }
            
            // Execute the project
            currentCapital += maxProfit;
            used[bestProjectIndex] = true; // Mark as done
        }
        
        return currentCapital;
    }

    /**
     * ========================================================================
     * MAIN METHOD: Executing and verifying the examples
     * ========================================================================
     */
    public static void main(String[] args) {
        List<TestCase> tests = List.of(
            new TestCase(
                2, 0, 
                new int[]{1, 2, 3}, 
                new int[]{0, 1, 1}, 
                4
            ),
            new TestCase(
                3, 0, 
                new int[]{1, 2, 3}, 
                new int[]{0, 1, 2}, 
                6
            ),
            new TestCase(
                1, 2, 
                new int[]{10, 20, 30}, 
                new int[]{0, 1, 3}, 
                22 // Starts at 2. Can afford 10 (c=0) or 20 (c=1). Max is 20. 2+20 = 22.
            )
        );

        for (int i = 0; i < tests.size(); i++) {
            TestCase tc = tests.get(i);
            System.out.println("Test Case " + (i + 1) + ":");
            System.out.println("K: " + tc.k() + " | Initial Capital: " + tc.initialCapital());
            System.out.println("Profits:  " + Arrays.toString(tc.profits()));
            System.out.println("Capitals: " + Arrays.toString(tc.capitals()));
            System.out.println("Expected Final Capital: " + tc.expected());
            
            int resOptimal = findMaximizedCapitalOptimal(tc.k(), tc.initialCapital(), tc.profits().clone(), tc.capitals().clone());
            int resBrute = findMaximizedCapitalBruteForce(tc.k(), tc.initialCapital(), tc.profits().clone(), tc.capitals().clone());
            
            System.out.println("Optimal (Heap) Result: " + resOptimal);
            System.out.println("Brute Force Result:    " + resBrute);
            System.out.println("--------------------------------------------------");
        }
    }
}

class Solution {

    /**
     * Record to model a Project clearly
     * capital -> required capital to start
     * profit  -> profit earned after completion
     */
    record Project(int capital, int profit) {}

    public int findMaximizedCapital(int k, int currentCapital, int[] profits, int[] capital) {

        int n = profits.length;

        // Step 1: Convert input arrays into a list of Project records
        List<Project> projects = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            projects.add(new Project(capital[i], profits[i]));
        }

        // Step 2: Sort projects by required capital (ascending)
        // So we can process feasible projects in order
        projects.sort(Comparator.comparingInt(Project::capital));

        /**
         * Max Heap to always pick the most profitable project
         * among currently feasible ones
         */
        PriorityQueue<Project> maxProfitHeap =
                new PriorityQueue<>((a, b) -> Integer.compare(b.profit(), a.profit()));
        // PriorityQueue<Project> maxProfitHeap =
        //        new PriorityQueue<>(Comparator.comparingInt(Project::profit).reversed());        

        int index = 0;

        // Step 3: We can do at most k projects
        for (int i = 0; i < k; i++) {

            /**
             * Add all projects that can be afforded with current capital
             * Since projects are sorted by capital, we can move linearly
             */
            while (index < n && projects.get(index).capital() <= currentCapital) {
                maxProfitHeap.offer(projects.get(index));
                index++;
            }

            /**
             * If no feasible project exists, we cannot proceed further
             */
            if (maxProfitHeap.isEmpty()) {
                break;
            }

            /**
             * Pick the project with maximum profit
             * Greedy choice ensures optimal result
             */
            Project bestProject = maxProfitHeap.poll();

            /**
             * Add profit to current capital
             */
            currentCapital += bestProject.profit();
        }

        return currentCapital;
    }
}
