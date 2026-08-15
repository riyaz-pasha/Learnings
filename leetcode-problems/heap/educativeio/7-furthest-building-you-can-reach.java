import java.util.*;

/*
================================================================================
 MOCK GOOGLE ONSITE INTERVIEW — FULL WRITE-UP
 Problem: Furthest Building You Can Reach  (LeetCode 1642, Google-tagged, Medium)
================================================================================
*/

class FurthestBuildingInterview {

    /*
    ============================================================================
     SECTION 1: RESTATE THE PROBLEM
    ============================================================================
     In my own words:

     I'm given an array `heights` of building heights, and I walk from
     building 0 towards the last building, one step at a time (building i to
     building i+1). Moving to a shorter-or-equal building is free. Moving to a
     taller building costs "climbing height" equal to the height difference,
     and I can pay for that climb in one of two currencies:
         - bricks: consumed exactly (1 brick per unit of height difference),
                   and bricks are a shared, depleting numeric budget.
         - ladders: a single ladder covers an ENTIRE climb regardless of how
                   tall it is, but I only have a limited count of ladders and
                   each ladder is "all or nothing" (can't split a climb across
                   a ladder and bricks).

     I want to return the index of the furthest building I can reach before
     I run out of both bricks and ladders on some required climb.

     Key constraints/inputs/outputs:
       - Input: int[] heights (1 <= heights.length <= 1e5, 1 <= heights[i] <= 1e6)
       - Input: int bricks (0 <= bricks <= 1e9)
       - Input: int ladders (0 <= ladders <= heights.length)
       - Output: a single int — the furthest reachable building index.
       - If I can reach the very last building, I return heights.length - 1.

     Implicit assumption to confirm: I always CAN move to building 0 (starting
     point, free), and downhill/flat moves never cost anything.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 2: CLARIFYING QUESTIONS (asked to the interviewer, with assumed
     answers so I can proceed without blocking on a real answer)
    ============================================================================
     1. Q: Can `heights` be empty, or have exactly one building?
        A (assumed): heights.length >= 1 per constraints. If length == 1, I'm
        already at the last building, so the answer is trivially 0.

     2. Q: Are `bricks` and `ladders` ever negative?
        A (assumed): No, both are guaranteed >= 0 per constraints.

     3. Q: If heights[i+1] == heights[i], does that cost anything?
        A (assumed): No — the problem says cost only applies when
        heights[i+1] > heights[i]; equal height is a free move, same as downhill.

     4. Q: Can I use a ladder on a climb and "save" bricks for a smaller climb
        later, i.e., is the assignment of ladders-to-climbs entirely up to me
        (not forced to be first-come-first-served)?
        A (assumed): Yes — I can choose which climbs get ladders and which get
        bricks, in any order, as long as I never use more ladders total than I
        have and never let cumulative used bricks exceed the budget at any point.

     5. Q: Do I need to output the actual assignment of bricks/ladders to
        climbs, or just the furthest index?
        A (assumed): Just the furthest reachable index — no reconstruction
        of the assignment required.

     6. Q: What are realistic bounds I should design for — is n up to 1e5 a
        hard performance constraint (i.e., do I need better than O(n^2))?
        A (assumed): Yes, n up to 1e5 means O(n log n) or O(n log ladders) is
        expected; O(n^2) or exponential will time out.

     7. Q: Is `bricks` a hard integer that could overflow a 32-bit int if I
        naively sum up to 1e5 climbs each up to 1e6 in height difference?
        A (assumed): Worst case cumulative brick usage could be up to
        1e5 * 1e6 = 1e11, which overflows int (max ~2.1e9). I should
        accumulate brick usage in a `long`.

     8. Q: Are ladders reusable, or one-time-use per climb?
        A (assumed): Each ladder is single-use per climb (I "spend" it), and I
        have exactly `ladders` of them total across the whole journey.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 3: EXAMPLES & EDGE CASES
    ============================================================================

     Example 1 (normal case):
       heights = [4, 2, 7, 6, 9, 14, 12], bricks = 5, ladders = 1
       Climbs (only where heights[i+1] > heights[i]):
         idx0->1: 2->4? wait let's index correctly: heights[1]=2 <= heights[0]=4 -> free
         idx1->2: 7 > 2, diff = 5
         idx2->3: 6 <= 7 -> free
         idx3->4: 9 > 6, diff = 3
         idx4->5: 14 > 9, diff = 5
         idx5->6: 12 <= 14 -> free
       Three real climbs: 5, 3, 5. With 1 ladder + 5 bricks, best play is to
       give the ladder to one of the size-5 climbs and pay bricks for the
       other two (3 + 5 = 8 > 5 bricks budget) -- so actually give the ladder
       to whichever climb, greedily, keeps cumulative bricks <= 5.
       Optimal: ladder -> climb of 5 (say idx4->5), bricks pay 5 (idx1->2) and
       3 (idx3->4) = 8 total, which EXCEEDS 5-brick budget. So I must instead
       ladder the LARGER cost and pick the best two out of three to cover with
       bricks such that their sum <= 5. Covering just the diff=5 with bricks
       (uses all 5) leaves diff=3 needing a ladder too — but only 1 ladder
       exists, and diff=5 (the other one) would then have nothing. So the
       true optimal is: ladder on one of the two 5s, bricks cover the 3, and
       bricks remaining (5-3=2) are insufficient for the other 5. Furthest
       reachable is right after that failure. Expected answer: index 4.
       (This matches the well-known LeetCode example.)

     Example 2 (edge case — no climbs needed at all):
       heights = [10, 8, 6, 4, 2], bricks = 0, ladders = 0
       Every step is downhill, so zero resources are ever needed.
       Expected answer: 4 (heights.length - 1), even with zero resources.

     Example 3 (tie-breaking / boundary case — resources exactly exhausted):
       heights = [1, 5, 1, 2, 3, 4, 10000], bricks = 4, ladders = 1
       Climbs: 1->5 (diff 4), 1->2 (diff 1), 2->3 (diff 1), 3->4 (diff 1),
               4->10000 (diff 9996).
       With ladders=1, the clear right call is to reserve the ladder for the
       enormous 9996 climb, and pay bricks for 4+1+1+1 = 7... but budget is
       only 4 bricks. So bricks alone can't cover 4,1,1,1 (sum 7 > 4 budget).
       The greedy heap approach handles this by dynamically deciding, at each
       point, which of the SMALLEST costs currently "on credit" via ladder
       should be demoted to bricks. This example exercises the boundary where
       resources run out exactly at the point of exhaustion, and validates
       that off-by-one handling (returning the index BEFORE the failed climb,
       not the failed climb's own index) is correct.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 4 & 5: ALL POSSIBLE SOLUTIONS (paradigm survey)
    ============================================================================
     Paradigms considered and whether they apply:

       - Brute force / naive           -> APPLICABLE (Approach 1)
       - Sorting-based                  -> Not standalone; sorting appears only
                                           implicitly inside heap operations.
       - Hashing-based                  -> NOT APPLICABLE: there's no
                                           lookup/grouping/frequency structure
                                           to exploit here; costs are positional
                                           and consumed in a resource-budget
                                           sense, not deduplicated or counted.
       - Two pointer / sliding window   -> NOT APPLICABLE: the "window" here
                                           isn't a contiguous subarray whose
                                           validity can be checked by simple
                                           pointer expansion/contraction — the
                                           decision of ladder-vs-bricks per
                                           climb is a global resource-allocation
                                           choice, not a monotonic window.
       - Divide and conquer             -> NOT APPLICABLE: there's no clean way
                                           to combine solutions of two halves,
                                           since the bricks/ladders budget is a
                                           single shared resource threading
                                           through the entire array; splitting
                                           the array loses global optimality.
       - Greedy                         -> APPLICABLE (Approaches 3 & 4) —
                                           greedy is provably optimal here
                                           because ladders are best "spent" on
                                           the largest climbs (exchange argument).
       - Dynamic programming            -> APPLICABLE (Approach 2) — DP over
                                           "ladders used so far" is a valid,
                                           if less efficient, formulation.
       - Tree / graph traversal         -> NOT APPLICABLE: no graph/tree
                                           structure; the path is already a
                                           fixed linear sequence of buildings.
       - Heap / priority queue          -> APPLICABLE (Approaches 3 & 4) — the
                                           optimal solutions are heap-based
                                           greedy.
       - Binary search                  -> NOT USEFULLY APPLICABLE: while
                                           "can I reach index k" is monotonic
                                           in k (if I can reach k I can reach
                                           k-1), verifying "can I reach k" still
                                           costs O(k) or O(k log ladders), so
                                           binary search over k adds a log(n)
                                           factor ON TOP of an already-linear
                                           check, making it strictly worse than
                                           the single-pass heap approach. Not
                                           worth implementing.
       - Monotonic stack / deque        -> NOT APPLICABLE: no "next greater
                                           element"-style relationship being
                                           queried; costs aren't popped based on
                                           relative ordering along the array,
                                           only by their raw magnitude.
       - Trie / segment tree / advanced -> NOT APPLICABLE: no prefix/range
                                           query structure needed; this is a
                                           pure single-pass resource allocation
                                           problem.
    ============================================================================
    */

    /*
    ----------------------------------------------------------------------------
     Approach 1: Brute Force Recursion (try every bricks-vs-ladder choice)
    ----------------------------------------------------------------------------
     Core idea: At each climb, branch into two universes — "pay with bricks"
     and "pay with a ladder" — and recursively explore both, taking the max
     furthest index achieved. This exhaustively tries every possible
     assignment of resources to climbs.

     Paradigm: Plain recursion / exhaustive search (backtracking).

     Time Complexity: O(2^n) worst case — every climb can branch two ways.
     Space Complexity: O(n) for recursion call stack depth.

     Pros:
       - Trivial to reason about correctness; a great "let me start simple"
         opener in an interview to show I understand the problem before
         optimizing.
       - Naturally explores every combination, so it's a solid brute-force
         oracle for stress-testing the optimized solutions.
     Cons:
       - Exponential blowup — completely infeasible for n up to 1e5.
       - Recomputes overlapping subproblems (no memoization here), though even
         with memoization on (index, bricksRemaining, laddersRemaining) the
         bricksRemaining dimension has too large a domain (up to 1e9) to memoize
         practically.
     When to use: Only as a mental warm-up / correctness oracle for small n in
     a stress test — never in production or as a final interview answer.
    ----------------------------------------------------------------------------
    */
    static int furthestBuildingBruteForce(int[] heights, int bricks, int ladders) {
        return bruteForceHelper(heights, 0, bricks, ladders);
    }

    private static int bruteForceHelper(int[] heights, int currentIndex, int bricksRemaining, int laddersRemaining) {
        if (currentIndex == heights.length - 1) {
            return currentIndex; // reached the last building, nothing further to try
        }
        int heightDifference = heights[currentIndex + 1] - heights[currentIndex];
        if (heightDifference <= 0) {
            // free move downhill or flat, no branching needed
            return bruteForceHelper(heights, currentIndex + 1, bricksRemaining, laddersRemaining);
        }
        int bestReachable = currentIndex; // fallback: we can't proceed past here
        // Branch 1: pay this climb with bricks, if we can afford it
        if (bricksRemaining >= heightDifference) {
            int reachUsingBricks = bruteForceHelper(heights, currentIndex + 1, bricksRemaining - heightDifference, laddersRemaining);
            bestReachable = Math.max(bestReachable, reachUsingBricks);
        }
        // Branch 2: pay this climb with a ladder, if we have one
        if (laddersRemaining > 0) {
            int reachUsingLadder = bruteForceHelper(heights, currentIndex + 1, bricksRemaining, laddersRemaining - 1);
            bestReachable = Math.max(bestReachable, reachUsingLadder);
        }
        return bestReachable;
    }

    /*
    ----------------------------------------------------------------------------
     Approach 2: Dynamic Programming over "ladders used so far"
    ----------------------------------------------------------------------------
     Core idea: Define dp[j] = the minimum total bricks spent to have made
     legal progress up to the current building, given that EXACTLY j ladders
     have been used so far. As we scan left to right and hit a climb of size
     d > 0, each state dp[j] can transition to:
       - dp[j]   (unchanged index j): pay this climb with bricks -> dp[j] + d
       - dp[j+1]: pay this climb with a ladder -> dp[j] unchanged, ladder count +1
     After processing each climb, if EVERY dp[j] exceeds the brick budget, we
     can no longer make progress and must stop.

     Paradigm: Dynamic programming (knapsack-style state = ladders used).

     Time Complexity: O(n * ladders) — for each of n buildings we scan up to
     `ladders + 1` DP states.
     Space Complexity: O(ladders) — we only keep the current dp array (rolling
     from the previous one).

     Pros:
       - Conceptually clean state definition; easy to explain and prove
         correct via a straightforward induction on "minimum bricks for a
         given ladder count."
       - Naturally generalizes if the problem were modified (e.g., ladders had
         varying "capacities" or costs) — DP formulations tend to be more
         flexible under such variant pressure than pure greedy arguments.
     Cons:
       - O(n * ladders) can degrade to O(n^2) when ladders is close to n,
         which is significantly worse than the O(n log ladders) heap solution.
       - More bookkeeping/code than the greedy heap approach for no benefit in
         the base version of this problem.
     When to use: Good to mention as an alternative during the interview to
     show DP fluency, but I would not lead with it as my final answer given
     ladders can be as large as n (making this approach 1e10 operations in the
     worst case — too slow).
    ----------------------------------------------------------------------------
    */
    static int furthestBuildingDP(int[] heights, int bricks, int ladders) {
        int totalBuildings = heights.length;
        // dp[j] = minimum bricks spent so far, having used EXACTLY j ladders.
        // Use a large sentinel to represent "this ladder-count is unreachable."
        final long UNREACHABLE = Long.MAX_VALUE / 4;
        long[] dp = new long[ladders + 1];
        Arrays.fill(dp, UNREACHABLE);
        dp[0] = 0L; // zero ladders used, zero bricks spent, at building 0

        for (int currentIndex = 0; currentIndex < totalBuildings - 1; currentIndex++) {
            int heightDifference = heights[currentIndex + 1] - heights[currentIndex];
            if (heightDifference <= 0) {
                continue; // free move, dp array is untouched
            }
            long[] nextDp = dp.clone();
            for (int laddersUsedSoFar = 0; laddersUsedSoFar <= ladders; laddersUsedSoFar++) {
                if (dp[laddersUsedSoFar] >= UNREACHABLE) {
                    continue; // this ladder-count was never reachable, skip
                }
                // Option A: pay this climb with bricks, ladder count stays the same
                long bricksIfWePayWithBricks = dp[laddersUsedSoFar] + heightDifference;
                if (bricksIfWePayWithBricks < nextDp[laddersUsedSoFar]) {
                    nextDp[laddersUsedSoFar] = bricksIfWePayWithBricks;
                }
                // Option B: pay this climb with a ladder, ladder count increases by 1
                if (laddersUsedSoFar + 1 <= ladders && dp[laddersUsedSoFar] < nextDp[laddersUsedSoFar + 1]) {
                    nextDp[laddersUsedSoFar + 1] = dp[laddersUsedSoFar];
                }
            }
            dp = nextDp;

            // If every ladder-count state now requires more bricks than we have,
            // we cannot legally make this climb -- furthest reachable is currentIndex.
            boolean anyStateAffordable = false;
            for (long bricksSpentInState : dp) {
                if (bricksSpentInState <= bricks) {
                    anyStateAffordable = true;
                    break;
                }
            }
            if (!anyStateAffordable) {
                return currentIndex;
            }
        }
        return totalBuildings - 1;
    }

    /*
    ----------------------------------------------------------------------------
     Approach 3: Greedy + Max-Heap ("pay bricks first, retroactively upgrade
     the most expensive climb to a ladder when over budget")
    ----------------------------------------------------------------------------
     Core idea: Greedily pay every climb with bricks as we go, tracking each
     brick-paid cost in a max-heap. The moment cumulative bricks used exceeds
     the budget, we retroactively "undo" the single MOST expensive brick
     payment made so far (pop the max from the heap) and replace it with a
     ladder instead -- since demoting the biggest cost frees up the most
     bricks per ladder spent. If we're over budget and have no ladders left to
     rescue us, we've found our answer.

     Paradigm: Greedy + heap (priority queue), backed by an exchange argument:
     it is never worse to have used a ladder on the largest climb than on any
     smaller one, so retroactively fixing up the max is always safe/optimal.

     Time Complexity: O(n log n) — each climb is pushed onto the heap once and
     popped at most once, each heap operation O(log n).
     Space Complexity: O(n) worst case — the heap can hold up to n climb costs
     if bricks are abundant and few demotions occur.

     Pros:
       - Intuitive "greedy then fix mistakes" framing that's easy to narrate
         out loud in an interview.
       - Same asymptotic ballpark as the optimal Approach 4, and correct.
     Cons:
       - Heap can grow to size O(n), whereas Approach 4's heap is bounded by
         `ladders`, which is often much smaller than n -- so Approach 4 is
         strictly better in practice when ladders << n.
       - Slightly more subtle to explain WHY demoting the max is safe versus
         Approach 4's more directly intuitive "give ladders to the biggest
         climbs" framing.
     When to use: A solid alternative if I think of "bricks-first" more
     naturally than "ladders-first" -- functionally equivalent optimality to
     Approach 4, just less space-efficient.
    ----------------------------------------------------------------------------
    */
    static int furthestBuildingMaxHeap(int[] heights, int bricks, int ladders) {
        // Max-heap of every climb cost currently being paid for with bricks.
        PriorityQueue<Integer> costsCurrentlyPaidWithBricks = new PriorityQueue<>(Collections.reverseOrder());
        long bricksUsedSoFar = 0L;
        int laddersRemaining = ladders;

        for (int currentIndex = 0; currentIndex < heights.length - 1; currentIndex++) {
            int heightDifference = heights[currentIndex + 1] - heights[currentIndex];
            if (heightDifference <= 0) {
                continue; // free move
            }
            // Tentatively pay this climb with bricks.
            costsCurrentlyPaidWithBricks.offer(heightDifference);
            bricksUsedSoFar += heightDifference;

            // If we've overspent, retroactively upgrade the single most
            // expensive brick-paid climb to use a ladder instead.
            if (bricksUsedSoFar > bricks) {
                if (laddersRemaining == 0) {
                    // No bricks left in budget, no ladders left to rescue us.
                    return currentIndex;
                }
                int mostExpensiveClimbSoFar = costsCurrentlyPaidWithBricks.poll();
                bricksUsedSoFar -= mostExpensiveClimbSoFar;
                laddersRemaining--;
            }
        }
        return heights.length - 1;
    }

    /*
    ----------------------------------------------------------------------------
     Approach 4 (OPTIMAL): Greedy + Min-Heap ("assume every climb gets a
     ladder, demote the smallest ladder-assigned climb to bricks when we run
     out of ladders")
    ----------------------------------------------------------------------------
     Core idea: Walk left to right. For every climb, tentatively assign it a
     ladder by pushing its cost onto a min-heap. The moment the heap size
     exceeds our ladder count, we know the SMALLEST cost currently sitting in
     the heap is the "least valuable" use of a ladder -- so we pop it out and
     pay for it with bricks instead, keeping ladders reserved for the largest
     climbs. If bricks go negative after that, we've gone as far as we can.

     Paradigm: Greedy + heap (priority queue). Exchange argument: ladders are
     strictly more valuable on larger climbs (a ladder "saves" exactly
     heightDifference bricks, so it should always be spent on the biggest
     heightDifference values available).

     Time Complexity: O(n log ladders) -- the heap never holds more than
     `ladders + 1` elements at a time, so every push/pop is O(log ladders)
     instead of O(log n).
     Space Complexity: O(ladders) -- heap size is capped at ladders + 1.

     Pros:
       - Best asymptotic complexity of all approaches when ladders << n
         (a very common real-world case).
       - Extremely short, clean, and easy to narrate: "give ladders to the
         biggest climbs, everything else pays bricks."
       - Directly generalizes: if constraints changed (e.g., ladders very
         large), this degrades gracefully to O(n log n), never worse.
     Cons:
       - Requires the (short) exchange-argument justification to convince an
         interviewer it's optimal, rather than being "obviously" correct like
         brute force.
       - Like Approach 3, uses a boxed Integer heap in Java, which has minor
         autoboxing overhead -- fine at this problem's scale (n <= 1e5).
     When to use: This is my recommended interview answer -- see Section 8.
    ----------------------------------------------------------------------------
    */
    static int furthestBuildingMinHeap(int[] heights, int bricks, int ladders) {
        // Min-heap of climb costs currently assigned a ladder.
        PriorityQueue<Integer> laddersAssignedClimbCosts = new PriorityQueue<>();
        int bricksRemaining = bricks;

        for (int currentIndex = 0; currentIndex < heights.length - 1; currentIndex++) {
            int heightDifference = heights[currentIndex + 1] - heights[currentIndex];
            if (heightDifference <= 0) {
                continue; // free move downhill/flat
            }
            laddersAssignedClimbCosts.offer(heightDifference);

            if (laddersAssignedClimbCosts.size() > ladders) {
                // We've used more "virtual ladders" than we actually have --
                // demote the SMALLEST ladder-assigned climb to bricks, since
                // ladders are most valuable on the largest climbs.
                bricksRemaining -= laddersAssignedClimbCosts.poll();
            }
            if (bricksRemaining < 0) {
                // Bricks budget exhausted and no ladder available to help.
                return currentIndex;
            }
        }
        return heights.length - 1;
    }

    /*
    ============================================================================
     SECTION 7: APPROACHES COMPARISON TABLE
    ============================================================================
     Approach                | Time              | Space        | Best For                                | Limitations
     -------------------------------------------------------------------------------------------------------------------------------
     1. Brute Force Recursion| O(2^n)             | O(n)         | Correctness oracle / tiny n stress tests| Exponential; unusable at n = 1e5
     2. DP over ladders used | O(n * ladders)     | O(ladders)   | Showing DP fluency; small `ladders`     | Degrades to O(n^2) when ladders ~ n
     3. Greedy + Max-Heap    | O(n log n)         | O(n)         | "Bricks-first" mental model             | Heap can grow to O(n); not as tight as Approach 4
     4. Greedy + Min-Heap    | O(n log ladders)   | O(ladders)   | Production / interview-optimal answer   | Requires exchange-argument justification
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
    ============================================================================
     I would present Approach 4 (Greedy + Min-Heap) as my final answer:

       - It is asymptotically the best of all approaches, O(n log ladders),
         and since `ladders <= heights.length` this is never worse than
         O(n log n), and often much better when ladders is small relative to n.
       - It's short enough to write correctly and quickly under interview
         time pressure (roughly 15 lines of core logic).
       - The greedy exchange argument ("a ladder is worth exactly
         heightDifference bricks, so always keep ladders on the largest
         climbs") is a clean, convincing 30-second proof sketch I can give
         verbally before coding, which is exactly what a Google interviewer
         wants to hear before I start typing.
       - It naturally handles the "return the index where I got stuck" output
         format, since I can return `currentIndex` immediately upon failure
         mid-scan without any post-processing.

     I'd mention the DP approach (2) briefly as "another valid but slower
     alternative," and Approach 3 as "an equally-optimal sibling that some
     people find equally intuitive," to demonstrate breadth without wasting
     interview time coding both.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 9: DEEP DIVE -- OPTIMAL SOLUTION (production-quality)
    ============================================================================
    */

    /**
     * Returns the index of the furthest building reachable given a brick
     * budget and a number of ladders, where climbing from building i to
     * building i+1 costs (heights[i+1] - heights[i]) resources if that value
     * is positive, payable either in bricks (consumed 1-for-1) or by a single
     * ladder (covers the entire climb regardless of size).
     *
     * Strategy: greedily assume every uphill climb is covered by a ladder,
     * tracked in a min-heap. Whenever we've "virtually" used more ladders
     * than we actually have, demote the cheapest ladder-covered climb to
     * bricks instead -- this is always safe because ladders save the most
     * bricks when reserved for the largest climbs (exchange argument).
     *
     * @param heights building heights, 1 <= heights.length <= 1e5
     * @param bricks  brick budget, 0 <= bricks <= 1e9
     * @param ladders ladder count, 0 <= ladders <= heights.length
     * @return furthest reachable building index (heights.length - 1 if the
     *         entire array is reachable)
     */
    static int furthestBuildingOptimal(int[] heights, int bricks, int ladders) {
        // Defensive input validation -- good interview hygiene, even though
        // constraints guarantee well-formed input; costs nothing at this scale.
        Objects.requireNonNull(heights, "heights must not be null");
        if (heights.length == 0) {
            throw new IllegalArgumentException("heights must contain at least one building");
        }
        if (heights.length == 1) {
            return 0; // already at the (only, last) building
        }

        // Min-heap holding the costs of climbs currently "assigned" a ladder.
        // Capped at size `ladders + 1` at any instant, which is what keeps
        // this approach's heap operations cheap (O(log ladders) not O(log n)).
        PriorityQueue<Integer> laddersAssignedClimbCosts = new PriorityQueue<>();

        // Use a plain int for bricksRemaining: constraints guarantee bricks
        // fits in an int (<= 1e9), and we only ever subtract single climb
        // costs (<= 1e6 each) from it one at a time, so no overflow risk here
        // -- unlike a naive running SUM of all climbs, which we deliberately
        // avoid needing.
        int bricksRemaining = bricks;

        for (int currentIndex = 0; currentIndex < heights.length - 1; currentIndex++) {
            int heightDifference = heights[currentIndex + 1] - heights[currentIndex];

            if (heightDifference <= 0) {
                // Downhill or flat -- always free, no resource bookkeeping needed.
                continue;
            }

            // Tentatively assign this climb a ladder.
            laddersAssignedClimbCosts.offer(heightDifference);

            // If we've "over-promised" ladders, demote the cheapest promise
            // (smallest climb currently on a ladder) to bricks instead. This
            // keeps ladders reserved for the biggest climbs seen so far,
            // which is always at least as good as any other assignment.
            if (laddersAssignedClimbCosts.size() > ladders) {
                int cheapestLadderAssignedClimb = laddersAssignedClimbCosts.poll();
                bricksRemaining -= cheapestLadderAssignedClimb;
            }

            // If bricks went negative, we truly cannot afford this climb
            // under any assignment of the resources we have -- stop here.
            if (bricksRemaining < 0) {
                return currentIndex;
            }
        }

        // Made it through every climb -- we can reach the last building.
        return heights.length - 1;
    }

    /*
    ============================================================================
     SECTION 10: DRY RUN / TRACE
    ============================================================================
     Tracing furthestBuildingOptimal on Example 1:
       heights = [4, 2, 7, 6, 9, 14, 12], bricks = 5, ladders = 1

     Initial state: laddersAssignedClimbCosts = [], bricksRemaining = 5

     currentIndex=0: heights[1]=2, heights[0]=4 -> diff = -2 <= 0 -> free, skip.
       State unchanged: heap=[], bricksRemaining=5

     currentIndex=1: heights[2]=7, heights[1]=2 -> diff = 5 (uphill!)
       Push 5 onto heap -> heap=[5]
       heap.size()=1 <= ladders(1) -> no demotion needed.
       bricksRemaining=5 >= 0 -> continue.
       State: heap=[5], bricksRemaining=5

     currentIndex=2: heights[3]=6, heights[2]=7 -> diff = -1 <= 0 -> free, skip.
       State unchanged: heap=[5], bricksRemaining=5

     currentIndex=3: heights[4]=9, heights[3]=6 -> diff = 3 (uphill!)
       Push 3 onto heap -> heap=[3,5] (min-heap root is 3)
       heap.size()=2 > ladders(1) -> demote the smallest: poll() removes 3.
         bricksRemaining = 5 - 3 = 2
       heap is now [5]
       bricksRemaining=2 >= 0 -> continue.
       State: heap=[5], bricksRemaining=2

     currentIndex=4: heights[5]=14, heights[4]=9 -> diff = 5 (uphill!)
       Push 5 onto heap -> heap=[5,5]
       heap.size()=2 > ladders(1) -> demote the smallest: poll() removes 5.
         bricksRemaining = 2 - 5 = -3
       heap is now [5]
       bricksRemaining=-3 < 0 -> WE FAIL HERE.
       Return currentIndex = 4.

     Final answer: 4 -- matches the expected result from Section 3, Example 1.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 11: CLOSING SUMMARY
    ============================================================================
     - Brute force (Approach 1) establishes correctness intuition and doubles
       as a stress-test oracle, but is exponential and never production-viable.
     - The DP formulation (Approach 2) is a legitimate alternative with a
       clean state definition, but its O(n * ladders) time is worse than the
       heap approaches whenever ladders is large, which the constraints
       explicitly allow (ladders can be as large as n).
     - Both heap-based greedy approaches (3 and 4) achieve near-linear time
       and are provably optimal via the same exchange argument -- ladders
       should always be reserved for the largest climbs. Approach 4 is
       strictly more space-efficient since its heap is capped at `ladders`
       elements rather than growing to `n`.
     - Final solution (furthestBuildingOptimal) runs in O(n log ladders) time
       and O(ladders) space, which comfortably handles the stated constraints
       (n up to 1e5).
     - Known assumptions baked into the final solution: heights, bricks, and
       ladders are well-formed per the stated constraints; a single-building
       array trivially returns index 0; equal-height moves are free.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
    ============================================================================
     1. "What if ladders could partially cover a climb (i.e., a ladder covers
        up to some fixed max height, and any excess still needs bricks)?"
        -- This breaks the simple heap exchange argument; would likely need a
        modified DP or a more careful greedy with partial-credit accounting.

     2. "What if bricks also had a fixed 'capacity per climb' (i.e., you can't
        spend more than some max on a single climb even with enough total
        bricks)?" -- Would require checking that constraint per-climb before
        even considering the ladder/brick choice; could still layer on top of
        the min-heap greedy with an extra feasibility check.

     3. "Can you reconstruct WHICH climbs used ladders vs bricks, not just the
        furthest index?" -- Yes: track heap contents and demotions across the
        scan instead of discarding them; O(n) extra bookkeeping.

     4. "What if this needs to run as a streaming algorithm where heights
        arrive one at a time and you must answer 'furthest so far' after each
        insertion?" -- The min-heap approach already supports this online,
        since it only ever looks at the newest climb and existing heap state;
        no need to reprocess prior buildings.

     5. "How would you parallelize this for extremely large n (billions of
        buildings)?" -- The greedy heap approach is inherently sequential
        (each decision depends on prior state), so parallelizing directly is
        hard; you'd likely need a different formulation, e.g., segment-based
        DP with mergeable per-segment (min-bricks-for-k-ladders) summaries,
        merged in a divide-and-conquer / map-reduce style.

     6. "What if ladders had different 'strengths' (each ladder can only cover
        climbs up to some max height)?" -- Would need to sort ladders by
        strength and climbs by size, and use a matching strategy (e.g., greedy
        two-pointer or a bipartite matching/flow formulation) rather than a
        simple uniform min-heap.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 13: WHAT CANDIDATES TYPICALLY MISS
    ============================================================================
     1. Off-by-one on the return value: candidates often return the index of
        the FAILED climb's destination (currentIndex + 1) instead of the last
        successfully reached building (currentIndex). The correct answer is
        always the index BEFORE the climb that couldn't be paid for.

     2. Integer overflow: summing all climb costs into a running `int` total
        (as opposed to checking against the budget incrementally, or using a
        `long` accumulator) can silently overflow given up to 1e5 climbs of up
        to 1e6 each (worst case ~1e11), producing wrong answers instead of a
        crash -- a dangerous silent bug.

     3. Using a max-heap sized by `n` and calling it equivalent to the
        `ladders`-capped min-heap: it IS correct (Approach 3), but candidates
        who don't realize the min-heap can be capped at `ladders` elements
        miss the tighter O(n log ladders) bound and undersell their own
        solution's efficiency.

     4. Forgetting that equal heights (heights[i+1] == heights[i]) are free:
        some candidates only special-case strictly decreasing heights and
        accidentally charge a "0-cost climb" through the heap/bricks logic,
        which is harmless numerically but wastes a heap slot / ladder
        needlessly if not explicitly skipped with the `<= 0` check.
    ============================================================================
    */

    /*
    ============================================================================
     BONUS SECTION: VERIFICATION -- ASSERTION TESTS + RANDOMIZED STRESS TEST
     (cross-validating all four approaches against the brute-force oracle,
     matching the standard verification discipline used across this pattern
     library: named assertions first, then a randomized stress test with a
     fixed seed for reproducibility.)
    ============================================================================
    */

    private static void runNamedAssertionTests() {
        // Test 1: the worked Example 1 from Section 3.
        int[] example1 = {4, 2, 7, 6, 9, 14, 12};
        assert furthestBuildingOptimal(example1, 5, 1) == 4 : "Example 1 failed on optimal";
        assert furthestBuildingMaxHeap(example1, 5, 1) == 4 : "Example 1 failed on max-heap";
        assert furthestBuildingDP(example1, 5, 1) == 4 : "Example 1 failed on DP";
        assert furthestBuildingBruteForce(example1, 5, 1) == 4 : "Example 1 failed on brute force";

        // Test 2: all downhill, zero resources needed (Example 2).
        int[] example2 = {10, 8, 6, 4, 2};
        assert furthestBuildingOptimal(example2, 0, 0) == 4 : "Example 2 failed on optimal";
        assert furthestBuildingMaxHeap(example2, 0, 0) == 4 : "Example 2 failed on max-heap";
        assert furthestBuildingDP(example2, 0, 0) == 4 : "Example 2 failed on DP";
        assert furthestBuildingBruteForce(example2, 0, 0) == 4 : "Example 2 failed on brute force";

        // Test 3: boundary/tie-breaking case (Example 3).
        int[] example3 = {1, 5, 1, 2, 3, 4, 10000};
        int expected3 = furthestBuildingBruteForce(example3, 4, 1);
        assert furthestBuildingOptimal(example3, 4, 1) == expected3 : "Example 3 failed on optimal";
        assert furthestBuildingMaxHeap(example3, 4, 1) == expected3 : "Example 3 failed on max-heap";
        assert furthestBuildingDP(example3, 4, 1) == expected3 : "Example 3 failed on DP";

        // Test 4: single building -- trivially reachable.
        int[] singleBuilding = {42};
        assert furthestBuildingOptimal(singleBuilding, 0, 0) == 0 : "Single building failed on optimal";
        assert furthestBuildingBruteForce(singleBuilding, 0, 0) == 0 : "Single building failed on brute force";

        // Test 5: abundant resources -- should always reach the end.
        int[] flatThenSpike = {1, 1, 1, 1, 1000};
        assert furthestBuildingOptimal(flatThenSpike, 1000000, 0) == 4 : "Abundant bricks failed on optimal";
        assert furthestBuildingOptimal(flatThenSpike, 0, 1) == 4 : "Single sufficient ladder failed on optimal";

        System.out.println("All named assertion tests passed.");
    }

    private static void runRandomizedStressTest() {
        final long RANDOM_SEED = 42L; // fixed seed for reproducibility
        Random random = new Random(RANDOM_SEED);
        final int TRIAL_COUNT = 2000;
        final int MAX_BUILDINGS_FOR_BRUTE_FORCE = 14; // keep 2^n tractable

        for (int trial = 0; trial < TRIAL_COUNT; trial++) {
            int buildingCount = 1 + random.nextInt(MAX_BUILDINGS_FOR_BRUTE_FORCE);
            int[] heights = new int[buildingCount];
            for (int i = 0; i < buildingCount; i++) {
                heights[i] = 1 + random.nextInt(20); // small range to force ties/repeats
            }
            int bricks = random.nextInt(15);
            int ladders = random.nextInt(buildingCount + 1);

            int bruteForceResult = furthestBuildingBruteForce(heights, bricks, ladders);
            int optimalResult = furthestBuildingOptimal(heights, bricks, ladders);
            int maxHeapResult = furthestBuildingMaxHeap(heights, bricks, ladders);
            int dpResult = furthestBuildingDP(heights, bricks, ladders);

            if (bruteForceResult != optimalResult || bruteForceResult != maxHeapResult || bruteForceResult != dpResult) {
                throw new AssertionError(String.format(
                    "Mismatch on trial %d: heights=%s bricks=%d ladders=%d -> bruteForce=%d optimal=%d maxHeap=%d dp=%d",
                    trial, Arrays.toString(heights), bricks, ladders,
                    bruteForceResult, optimalResult, maxHeapResult, dpResult));
            }
        }
        System.out.println("Randomized stress test passed: " + TRIAL_COUNT + " trials, all four approaches agree with the brute-force oracle.");
    }

    public static void main(String[] args) {
        runNamedAssertionTests();
        runRandomizedStressTest();

        // Quick demonstration matching the dry run in Section 10.
        int[] demoHeights = {4, 2, 7, 6, 9, 14, 12};
        System.out.println("Demo result (expected 4): " + furthestBuildingOptimal(demoHeights, 5, 1));
    }
}
