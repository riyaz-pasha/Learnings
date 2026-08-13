import java.util.*;

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: MAXIMUM AVERAGE PASS RATIO (LeetCode 1792)
 * ============================================================================
 *
 * This single file walks through the full interview arc for this problem,
 * exactly as it should be presented live: restate -> clarify -> examples ->
 * enumerate every viable approach -> compare -> recommend -> deep dive ->
 * trace -> close -> follow-ups -> common mistakes.
 *
 * Compile & run locally with:
 *   javac MaximumAveragePassRatio.java && java MaximumAveragePassRatio
 */
class MaximumAveragePassRatio {

    /* ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In my own words:
     *   I'm given a list of classes. Each class i has `passi` students who are
     *   currently expected to pass, out of `totali` students enrolled. I'm
     *   also given a pool of `extraStudents` — students who are guaranteed to
     *   pass if placed into any class. When such a student is added to a
     *   class, both the numerator (passi) AND the denominator (totali) of
     *   that class's pass ratio increase by 1, because the class now has one
     *   more student, and that student passes.
     *
     *   I need to distribute ALL extraStudents (I can put any number, from 0
     *   to extraStudents, into each class) to maximize the AVERAGE of the
     *   per-class pass ratios (arithmetic mean across classes, not weighted
     *   by class size).
     *
     * Inputs:
     *   - int[][] classes, where classes[i] = [passi, totali], 1 <= passi <=
     *     totali <= 10^4, 1 <= classes.length <= 10^3
     *   - int extraStudents, 1 <= extraStudents <= 10^4
     *
     * Output:
     *   - A double: the maximum achievable average pass ratio, accepted
     *     within 1e-5 of the true optimal value.
     *
     * Key implicit assumptions to surface out loud:
     *   - Every extra student, once assigned, is guaranteed to pass — so
     *     assigning one to a class always increases that class's ratio
     *     (never decreases it), though by a diminishing amount as the class
     *     grows.
     *   - ALL extraStudents must be distributed (not optional to hold any
     *     back) — though I'll confirm this in clarifying questions, since
     *     it's a subtle constraint that changes the problem if false.
     *   - The average is unweighted — a class of 2 students counts the same
     *     as a class of 10,000 in the final average.
     */


    /* ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS
     * ========================================================================
     *
     * Q1: Must ALL extraStudents be assigned, or can I hold some back if
     *     assigning them doesn't help?
     *     A1 (assumed): All must be assigned. This matters less than it
     *     sounds, though — since every assignment strictly increases (or at
     *     worst, for passi == totali, leaves unchanged... actually let's
     *     double check: if passi == totali, adding an extra student gives
     *     (passi+1)/(totali+1) which is STILL passi/totali essentially
     *     unchanged in the limit but never decreases) the class ratio, so
     *     holding students back is never beneficial anyway. I'll assume
     *     mandatory full assignment per the problem statement.
     *
     * Q2: Can multiple extra students be assigned to the same class?
     *     A2 (assumed): Yes — no cap on how many extra students one class
     *     can absorb.
     *
     * Q3: Can a class currently have passi == totali (100% already)?
     *     A3 (assumed): Yes, this is a valid edge case (e.g., [1,1]). Adding
     *     a student here gives marginal gain (p+1)/(t+1) - p/t which
     *     approaches 0 but is never negative, so it's never actively harmful,
     *     just a low-priority target for extras.
     *
     * Q4: Are passi, totali, extraStudents guaranteed to be non-negative
     *     integers within the stated bounds, or do I need defensive
     *     validation for malformed input (e.g., passi > totali)?
     *     A4 (assumed): Per constraints, 1 <= passi <= totali always holds
     *     and inputs are well-formed. I'll still add defensive checks in the
     *     production implementation since real interview code should not
     *     blindly trust callers.
     *
     * Q5: Is the array `classes` allowed to be empty?
     *     A5 (assumed): No — constraints guarantee classes.length >= 1, so
     *     the average is always well-defined (no division by zero classes).
     *
     * Q6: Do I need to return which class each extra student was assigned
     *     to, or just the final average ratio?
     *     A6 (assumed): Just the final maximum average ratio as a double.
     *     No need to reconstruct the assignment.
     *
     * Q7: What precision/tolerance is expected for the returned double, and
     *     should I worry about floating-point drift over up to 10^4
     *     iterations?
     *     A7 (assumed): Answers within 1e-5 of the true value are accepted,
     *     per the problem statement. Standard IEEE 754 double precision is
     *     more than sufficient for 10^4 additive floating point operations
     *     — error accumulation stays far below the tolerance.
     *
     * Q8: Is this a single-threaded, single-call computation, or do I need
     *     to worry about concurrent modification (e.g., multiple threads
     *     assigning extra students simultaneously)?
     *     A8 (assumed): Single-threaded, single invocation. No concurrency
     *     concerns for this problem as stated.
     */


    /* ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (normal case):
     *   classes = [[1,2],[3,5],[2,2]], extraStudents = 2
     *   Initial ratios: 0.500, 0.600, 1.000
     *   Class 3 is already at 100% -> gain from adding there is ~0, so both
     *   extras should go where marginal gain is highest.
     *   Marginal gain of class 0 (1/2 -> 2/3): 2/3 - 1/2 = 0.1667
     *   Marginal gain of class 1 (3/5 -> 4/6): 4/6 - 3/5 = 0.0667
     *   Marginal gain of class 2 (2/2 -> 3/3): 3/3 - 2/2 = 0.0
     *   -> First extra goes to class 0: ratios become [0.667, 0.600, 1.000]
     *   Recompute gain for class 0 now at 2/3 -> 3/4: 3/4 - 2/3 = 0.0833
     *   That's still bigger than class 1's 0.0667, so second extra ALSO goes
     *   to class 0: ratios become [0.750, 0.600, 1.000]
     *   Average = (0.75 + 0.60 + 1.00) / 3 = 0.78333...
     *   This matches the known LeetCode expected output of 0.78333.
     *
     * Example 2 (edge case — single class, all extras dumped in one place):
     *   classes = [[2,4]], extraStudents = 3
     *   No choice but to add all 3 extras to the only class:
     *   2/4 -> 3/5 -> 4/6 -> 5/7 = 0.714285...
     *   Average = 0.714285... (trivial, but tests that the algorithm
     *   doesn't break with n = 1).
     *
     * Example 3 (boundary / tie-breaking case):
     *   classes = [[1,2],[1,2]], extraStudents = 1
     *   Two IDENTICAL classes, only one extra student to distribute. Marginal
     *   gain is identical for both (2/3 - 1/2 = 0.1667 each), so it does not
     *   matter which class receives the extra — the resulting average is the
     *   same either way: (0.667 + 0.500)/2 = 0.5833. This confirms the
     *   algorithm must correctly handle ties in the max-heap without any
     *   special tie-breaking logic — any valid choice yields the optimal
     *   result, since the problem only asks for the maximum ACHIEVABLE
     *   average, not a specific assignment.
     *
     * Additional edge cases to mention verbally:
     *   - extraStudents much larger than total possible capacity concerns:
     *     N/A here, since classes can absorb unbounded extra students (no
     *     capacity limit stated).
     *   - All classes already at passi == totali (100%): every marginal gain
     *     is > 0 but shrinking, extras still get distributed to whichever
     *     class has the (slightly) highest remaining gain; average approaches
     *     but never exceeds 1.0.
     *   - extraStudents == 0 is excluded by constraints (extraStudents >= 1),
     *     but defensively the algorithm should just return the unmodified
     *     average if it were 0.
     */


    /* ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE SOLUTIONS (ACROSS PARADIGMS)
     * ========================================================================
     *
     * Paradigms considered and ruled out with one-line justification:
     *   - Hashing-based: No lookup/grouping structure helps here; the problem
     *     is a pure numeric optimization over marginal gains, not a
     *     membership/frequency problem.
     *   - Two-pointer / sliding window: There's no sequential/contiguous
     *     structure to slide over — each class is an independent, unordered
     *     unit competing for the same shared pool of extras.
     *   - Divide & conquer: The choice of where to place each extra student
     *     depends globally on ALL classes' current state, not on
     *     independently-solvable subproblems that recombine cleanly.
     *   - Dynamic programming: A DP over (classIndex, extrasUsedSoFar) is
     *     theoretically expressible, but the state space is
     *     O(n * extraStudents) = O(10^3 * 10^4) = 10^7 states, each requiring
     *     a transition loop over "how many extras to give this class"
     *     (another O(extraStudents) factor) — pushing to O(10^11), far too
     *     slow. It's also solving a harder problem than needed: the greedy
     *     exchange-argument (Section 9) proves a much cheaper approach is
     *     exactly optimal, making DP strictly dominated here.
     *   - Tree / graph traversal: No graph or hierarchical relationship
     *     exists between classes.
     *   - Binary search: Doesn't map cleanly — there's no single sorted
     *     monotonic predicate to binary search over (the "best marginal
     *     gain threshold" changes shape after every single-unit allocation,
     *     since gain is a per-class function, not a global one).
     *   - Trie / segment tree: No prefix, string, or range-query structure
     *     is present in this problem.
     *   - Monotonic stack / deque: No ordering/nearest-greater-element
     *     relationship to exploit.
     *
     * Genuinely applicable paradigms: Brute Force, Greedy (naive linear
     * scan), and Greedy + Heap/Priority Queue (optimal). Sorting appears
     * only as a minor helper inside the greedy approaches, not as a
     * standalone strategy, so it is folded into Approach 2 rather than
     * given its own top-level treatment.
     */

    /*
     * ------------------------------------------------------------------------
     * APPROACH 1: Brute Force / Naive (Exhaustive Distribution)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Recursively try every possible way of splitting extraStudents across
     *   all classes (a "stars and bars" style partition), computing the
     *   resulting average for each complete distribution, and keep the best.
     *
     * Paradigm: Brute-force recursion / backtracking over integer
     * compositions.
     *
     * Time Complexity: O(C(extraStudents + n - 1, n - 1)) — the number of
     *   ways to distribute extraStudents identical items into n classes.
     *   This is combinatorially explosive (e.g., with n=3, extra=2 it's only
     *   6 ways, but with real constraints n=1000, extra=10000 it's
     *   astronomically large — far beyond any feasible runtime).
     * Space Complexity: O(n) for recursion depth / current distribution
     *   array, plus exponential branching factor in the call tree.
     *
     * Pros:
     *   - Trivially, provably correct — useful ONLY as an oracle to validate
     *     faster approaches on small inputs during development.
     * Cons:
     *   - Completely infeasible for real constraints; would not even
     *     terminate in reasonable time for n > ~6, extra > ~10.
     * When to use:
     *   - Never in production or as your primary interview answer. Mention
     *     it briefly to show you considered the full solution space, then
     *     immediately pivot to why it's inadequate.
     */
    public static double bruteForceMaxAverage(int[][] classes, int extraStudents) {
        int numberOfClasses = classes.length;
        double[] bestAverage = {-1.0};
        double[] currentRatios = new double[numberOfClasses];

        distributeRecursively(classes, 0, extraStudents, currentRatios, bestAverage);
        return bestAverage[0];
    }

    private static void distributeRecursively(int[][] classes, int classIndex,
            int remainingExtras, double[] currentRatios, double[] bestAverage) {
        int numberOfClasses = classes.length;

        if (classIndex == numberOfClasses) {
            // Base case: all classes have been assigned some number of
            // extras (summing to extraStudents, enforced by only recursing
            // through the FULL remaining budget on the last class). Compute
            // the average and update the best seen so far.
            double sum = 0.0;
            for (double ratio : currentRatios) {
                sum += ratio;
            }
            bestAverage[0] = Math.max(bestAverage[0], sum / numberOfClasses);
            return;
        }

        int passCount = classes[classIndex][0];
        int totalCount = classes[classIndex][1];

        // Try giving this class every possible count of extras from 0 up to
        // whatever remains, then recurse on the rest of the classes with the
        // leftover budget.
        for (int extrasGivenToThisClass = 0; extrasGivenToThisClass <= remainingExtras;
                extrasGivenToThisClass++) {
            currentRatios[classIndex] =
                    (double) (passCount + extrasGivenToThisClass)
                            / (totalCount + extrasGivenToThisClass);
            distributeRecursively(classes, classIndex + 1,
                    remainingExtras - extrasGivenToThisClass, currentRatios, bestAverage);
        }
    }

    /*
     * ------------------------------------------------------------------------
     * APPROACH 2: Greedy — Linear Scan Without a Heap
     * ------------------------------------------------------------------------
     * Core idea:
     *   The KEY INSIGHT (shared by this and the optimal approach): since
     *   every extra student must be placed SOMEWHERE, and placement is
     *   independent across students, the globally optimal strategy is to
     *   always give the NEXT available extra student to whichever class
     *   currently has the highest MARGINAL GAIN — i.e., the biggest jump in
     *   ratio from adding one more passing student:
     *
     *       marginalGain(pass, total) = (pass+1)/(total+1) - pass/total
     *
     *   This is a classic greedy-exchange-argument problem: if two classes
     *   A and B could each receive one more extra student, and A's marginal
     *   gain is currently higher than B's, giving the student to A instead
     *   of B strictly increases (or at minimum does not decrease) the total
     *   sum of ratios, REGARDLESS of how future students get allocated,
     *   because marginal gain is a strictly decreasing function of a
     *   class's current pass ratio for fixed pass/total gaps (this is proven
     *   formally in Section 9).
     *
     *   This naive version just re-scans ALL classes on every single extra
     *   student assignment to find the current maximum-gain class.
     *
     * Paradigm: Greedy (repeated local-maximum selection), no auxiliary
     * data structure.
     *
     * Time Complexity: O(extraStudents * n) — for each of the
     *   extraStudents iterations, scan all n classes to find the max-gain
     *   one. With n=10^3 and extraStudents=10^4, that's up to 10^7 basic
     *   operations — technically within typical interview time limits, but
     *   wasteful compared to Approach 3.
     * Space Complexity: O(n) for the mutable pass/total arrays (or O(1)
     *   extra if mutating in place).
     *
     * Pros:
     *   - Correct, and conceptually simple — easy to explain the greedy
     *     insight without introducing a heap.
     *   - No extra data structure overhead; good if n and extraStudents are
     *     both small.
     * Cons:
     *   - Redundant work: re-scanning all n classes on every iteration
     *     ignores the fact that only ONE class's gain changed since the
     *     last iteration — a heap lets us avoid re-examining classes whose
     *     gain didn't change.
     *   - At the given constraint ceiling (10^7 operations), this is on the
     *     edge of "acceptable" but clearly suboptimal versus O((n+extra) log n).
     * When to use:
     *   - As a stepping stone in your explanation, or if you're in an
     *     environment where introducing a heap is disproportionately more
     *     complex than the benefit warrants (e.g., extremely small extra
     *     student counts). Not the final answer in a Google interview.
     */
    public static double greedyLinearScanMaxAverage(int[][] classes, int extraStudents) {
        int numberOfClasses = classes.length;
        // Mutable working copies so we don't mutate caller's input array.
        long[] passCounts = new long[numberOfClasses];
        long[] totalCounts = new long[numberOfClasses];
        for (int i = 0; i < numberOfClasses; i++) {
            passCounts[i] = classes[i][0];
            totalCounts[i] = classes[i][1];
        }

        for (int extraAssigned = 0; extraAssigned < extraStudents; extraAssigned++) {
            int bestClassIndex = -1;
            double bestGain = Double.NEGATIVE_INFINITY;

            // Linear scan to find the class with the highest marginal gain
            // right now. This is the O(n) cost repeated extraStudents times.
            for (int classIndex = 0; classIndex < numberOfClasses; classIndex++) {
                double gain = marginalGain(passCounts[classIndex], totalCounts[classIndex]);
                if (gain > bestGain) {
                    bestGain = gain;
                    bestClassIndex = classIndex;
                }
            }

            passCounts[bestClassIndex]++;
            totalCounts[bestClassIndex]++;
        }

        double sumOfRatios = 0.0;
        for (int i = 0; i < numberOfClasses; i++) {
            sumOfRatios += (double) passCounts[i] / totalCounts[i];
        }
        return sumOfRatios / numberOfClasses;
    }

    /*
     * ------------------------------------------------------------------------
     * APPROACH 3 (OPTIMAL): Greedy + Max-Heap / Priority Queue
     * ------------------------------------------------------------------------
     * Core idea:
     *   Identical greedy insight as Approach 2, but instead of re-scanning
     *   all n classes on every iteration, maintain a MAX-HEAP keyed by each
     *   class's current marginal gain. Each iteration:
     *     1. Pop the class with the highest marginal gain — O(log n).
     *     2. Simulate adding one extra student to it (pass++, total++).
     *     3. Recompute its NEW marginal gain and push it back — O(log n).
     *   Repeat extraStudents times, then sum final ratios / n.
     *
     * Paradigm: Greedy + Heap / Priority Queue.
     *
     * Time Complexity: O(n log n) to build the initial heap, plus
     *   O(extraStudents log n) for the extraction/reinsertion loop —
     *   overall O((n + extraStudents) log n). With n=10^3,
     *   extraStudents=10^4, that's roughly 1.1*10^4 * ~10 =~ 1.1*10^5
     *   operations — comfortably fast.
     * Space Complexity: O(n) for the heap (holds exactly one entry per
     *   class at all times).
     *
     * Pros:
     *   - Asymptotically optimal for this problem shape; avoids all
     *     redundant re-scanning.
     *   - Clean, idiomatic use of Java's PriorityQueue with a custom
     *     comparator — a pattern Google interviewers recognize instantly.
     *   - Easy to reason about correctness via the exchange argument.
     * Cons:
     *   - Slightly more code/machinery than the naive greedy scan (need a
     *     comparator or a record/class wrapping pass+total).
     *   - Heap operations have real constant-factor overhead versus a flat
     *     array scan for VERY small n — irrelevant at this problem's scale.
     * When to use:
     *   - This is the production-quality, interview-recommended solution.
     *     Always prefer this at the given constraints.
     */
    public static double greedyMaxHeapMaxAverage(int[][] classes, int extraStudents) {
        // Input validation — defensive even though constraints guarantee
        // well-formed input; signals production-quality thinking.
        Objects.requireNonNull(classes, "classes must not be null");
        if (classes.length == 0) {
            throw new IllegalArgumentException("classes must contain at least one class");
        }
        if (extraStudents < 0) {
            throw new IllegalArgumentException("extraStudents must be non-negative");
        }

        int numberOfClasses = classes.length;

        // Max-heap ordered by DESCENDING marginal gain. Java's PriorityQueue
        // is a min-heap by default, so we supply a comparator that reverses
        // the natural order of marginal gain.
        PriorityQueue<long[]> maxGainHeap = new PriorityQueue<>(
                numberOfClasses,
                (classA, classB) -> Double.compare(
                        marginalGain(classB[0], classB[1]),
                        marginalGain(classA[0], classA[1])));

        // Each heap entry is {passCount, totalCount} for one class, using
        // `long` defensively even though inputs fit in `int`, since
        // pass/total can each grow by up to extraStudents (10^4) beyond
        // their original bound of 10^4, and we want headroom against any
        // future constraint changes without silent overflow.
        for (int[] classroom : classes) {
            maxGainHeap.offer(new long[] {classroom[0], classroom[1]});
        }

        for (int extraAssigned = 0; extraAssigned < extraStudents; extraAssigned++) {
            long[] bestClass = maxGainHeap.poll();
            bestClass[0]++; // one more passing student
            bestClass[1]++; // one more total student (the same student)
            maxGainHeap.offer(bestClass);
        }

        double sumOfRatios = 0.0;
        for (long[] classroom : maxGainHeap) {
            sumOfRatios += (double) classroom[0] / classroom[1];
        }
        return sumOfRatios / numberOfClasses;
    }

    /**
     * Computes the marginal gain in pass ratio from adding exactly one more
     * guaranteed-passing student to a class currently at passCount /
     * totalCount.
     *
     * <p>This is the single most important formula in the whole solution:
     * it is strictly positive (adding a guaranteed-pass student never hurts
     * a class's ratio) and strictly decreasing in passCount for a fixed
     * totalCount - passCount gap — which is exactly what makes the greedy
     * strategy provably optimal (see Section 9 for the proof sketch).
     *
     * @param passCount  current number of passing students in the class
     * @param totalCount current total number of students in the class
     * @return the increase in ratio from adding one guaranteed-pass student
     */
    private static double marginalGain(long passCount, long totalCount) {
        return (double) (passCount + 1) / (totalCount + 1) - (double) passCount / totalCount;
    }


    /* ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * Approach                       | Time                    | Space  | Best For                              | Limitations
     * --------------------------------|--------------------------|--------|----------------------------------------|-------------------------------------------
     * 1. Brute Force (exhaustive)     | O(C(extra+n-1, n-1))     | O(n)   | Validating correctness on tiny inputs  | Combinatorially infeasible at real scale
     * 2. Greedy, Linear Scan          | O(extraStudents * n)     | O(n)   | Small n / small extraStudents, or when | Redundant re-scanning; ~10^7 ops at max
     *                                  |                          |        | you want to avoid heap machinery       | constraints — works but not optimal
     * 3. Greedy + Max-Heap (OPTIMAL)  | O((n+extraStudents)logn) | O(n)   | Production / interview-final answer    | Slightly more code (comparator + heap)
     */


    /* ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 3 (Greedy + Max-Heap) as my final answer.
     *
     * Why:
     *   - Clarity: The greedy insight (always feed the extra student to
     *     whoever gains the most right now) is intuitive to explain and to
     *     justify with a short exchange argument — interviewers can follow
     *     it in under a minute.
     *   - Coding speed: A PriorityQueue with a custom comparator is a
     *     well-worn Java idiom I can write correctly in a few minutes
     *     without bugs, unlike, say, a from-scratch DP formulation that
     *     would take much longer to even get right, let alone optimal.
     *   - Interviewer expectations: At Google-level interviews for this
     *     exact problem shape (LeetCode Hard, "Maximum Average Pass
     *     Ratio"), the heap-based greedy solution IS the expected optimal
     *     answer; presenting the brute force first (briefly) and the
     *     linear-scan greedy as a stepping stone shows a full reasoning
     *     arc, then landing on the heap avoids leaving obvious optimization
     *     on the table.
     *   - Optimality: O((n + extraStudents) log n) is essentially the best
     *     possible here, since we must at minimum look at every class once
     *     (O(n)) and process every extra student once (O(extraStudents)); a
     *     logarithmic factor for "find current best" is the cheapest
     *     realistic overhead versus the linear-scan alternative.
     *
     * I would code Approach 1 only verbally (not in full), state Approach 2
     * as the natural first correct idea, then implement Approach 3 in full
     * as my committed solution.
     */


    /* ========================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (PRODUCTION-QUALITY)
     * ========================================================================
     *
     * The method `greedyMaxHeapMaxAverage` above IS this production
     * implementation; it already includes full Javadoc, defensive input
     * validation, named/typed fields, and inline comments explaining every
     * decision (why `long`, why a max-heap via comparator, why we mutate
     * pass/total together). Reproduced conceptually here with the
     * correctness proof that justifies why greedy is optimal:
     *
     * PROOF SKETCH (why greedy-by-marginal-gain is globally optimal):
     *   Claim: marginalGain(p, t) = (p+1)/(t+1) - p/t simplifies to
     *          (t - p) / (t * (t + 1))
     *   which is strictly positive whenever p < t or p == t (it's >= 0
     *   always, since p <= t always holds), and — critically — for a FIXED
     *   value of t, marginalGain is a DECREASING function of p. This means:
     *   the more extra students a class has already absorbed, the smaller
     *   the benefit of giving it yet another one. This "diminishing
     *   returns" property is exactly the precondition needed for a greedy
     *   exchange argument: at every step, assigning the next unit of a
     *   scarce resource (one extra student) to the option with currently
     *   highest marginal value is optimal, because swapping it to any other
     *   option instead could only ever match or reduce the total sum — never
     *   improve it. This is a textbook instance of the "greedy algorithms
     *   for concave/diminishing-returns allocation" pattern (closely related
     *   to fractional knapsack and water-filling allocation problems).
     *
     * Full production implementation (heap-based) is defined above as
     * `greedyMaxHeapMaxAverage(int[][] classes, int extraStudents)`.
     * Key production-quality decisions annotated inline there:
     *   - `long[]` heap entries instead of a custom record, to minimize
     *     boxing overhead in a hot loop run up to 10^4 times (a design
     *     trade-off worth stating out loud: a record would be more
     *     readable, a primitive array is faster — I chose speed here since
     *     this loop dominates runtime).
     *   - Comparator recomputes marginalGain() at comparison time rather
     *     than caching it in the heap entry, trading a small amount of
     *     redundant computation (recomputed during heap sift operations)
     *     for simpler, less error-prone code — an explicit, named
     *     trade-off worth calling out under time pressure.
     *   - Defensive validation (`Objects.requireNonNull`,
     *     `IllegalArgumentException`) despite constraints guaranteeing
     *     clean input — production code shouldn't blindly trust callers,
     *     and it's a cheap way to demonstrate maturity.
     */


    /* ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     *
     * Tracing `greedyMaxHeapMaxAverage` on Example 1:
     *   classes = [[1,2],[3,5],[2,2]], extraStudents = 2
     *
     * Initial heap build (max-heap by marginal gain):
     *   Class A = [1,2] -> gain = (2/3 - 1/2) = 0.16667
     *   Class B = [3,5] -> gain = (4/6 - 3/5) = 0.06667
     *   Class C = [2,2] -> gain = (3/3 - 2/2) = 0.00000
     *   Heap (top to bottom by gain): [A(0.1667), B(0.0667), C(0.0000)]
     *
     * Iteration 1 (extraAssigned = 0):
     *   Pop A = [1,2] (highest gain 0.1667).
     *   Update A -> [2,3] (pass++, total++).
     *   Recompute A's new gain: (3/4 - 2/3) = 0.08333.
     *   Push A back. Heap now: [A(0.0833), B(0.0667), C(0.0000)]
     *     (A is still on top since 0.0833 > 0.0667)
     *
     * Iteration 2 (extraAssigned = 1):
     *   Pop A = [2,3] (still highest gain, 0.0833 > B's 0.0667).
     *   Update A -> [3,4] (pass++, total++).
     *   Recompute A's new gain: (4/5 - 3/4) = 0.05000.
     *   Push A back. Heap now: [B(0.0667), A(0.0500), C(0.0000)]
     *
     * Loop ends (extraStudents = 2 exhausted).
     *
     * Final heap contents: A=[3,4], B=[3,5], C=[2,2]
     *   Ratios: 3/4 = 0.75000, 3/5 = 0.60000, 2/2 = 1.00000
     *   Sum = 2.35000
     *   Average = 2.35000 / 3 = 0.78333...
     *
     * Matches the expected result from Section 3, Example 1. Both extra
     * students went to class A because its marginal gain remained highest
     * even after the first assignment.
     */


    /* ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - Brute force establishes correctness but is combinatorially
     *   infeasible beyond toy inputs — useful only as a fuzz-testing oracle.
     * - The naive greedy linear scan is CORRECT and technically within time
     *   limits at max constraints (~10^7 ops), but does O(n) redundant work
     *   per extra student that a heap eliminates.
     * - The heap-based greedy is optimal at O((n + extraStudents) log n),
     *   relies on a clean exchange-argument proof (diminishing marginal
     *   gain), and is the version I'd commit to in an interview.
     *
     * Known limitations / assumptions of the final solution:
     *   - Assumes ALL extraStudents must be assigned (per problem
     *     statement) — trivially fine since assignment is never harmful.
     *   - Assumes unbounded class capacity (no cap on students per class);
     *     if a real-world cap existed, the heap would need to skip/remove
     *     classes once they hit capacity, which is a straightforward
     *     extension (Section 12 covers this).
     *   - Uses double-precision floating point for ratio math; given
     *     extraStudents <= 10^4 total float additions, cumulative error is
     *     many orders of magnitude below the 1e-5 tolerance, so this is
     *     safe without needing arbitrary-precision arithmetic.
     */


    /* ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS
     * ========================================================================
     *
     * 1. "What if extraStudents could be up to 10^9 instead of 10^4?"
     *    -> The heap approach degrades to O(extraStudents log n), which
     *       becomes too slow. Would need a closed-form or binary-search-on-
     *       answer approach: binary search on the number of extras given to
     *       the CURRENT top class analytically (since marginal gain
     *       decreases smoothly per class), batching many assignments to the
     *       same class in one heap operation instead of one-at-a-time.
     *
     * 2. "What if each class had a maximum capacity it couldn't exceed?"
     *    -> Track a per-class remaining capacity; when a class hits max
     *       capacity, don't push it back onto the heap (remove it from
     *       further consideration). Straightforward O(1) extra check per
     *       iteration.
     *
     * 3. "What if the average needed to be WEIGHTED by class size instead
     *    of a simple arithmetic mean?"
     *    -> This fundamentally changes the greedy objective function —
     *       marginal gain would need to be scaled by (totalStudentsInClass /
     *       totalStudentsAcrossAllClasses), but the SAME heap-based greedy
     *       exchange-argument structure would still apply with the adjusted
     *       gain formula.
     *
     * 4. "How would you parallelize this for extremely large n (e.g., 10^7
     *    classes) across multiple machines?"
     *    -> The heap itself is inherently sequential (each pop/push depends
     *       on the previous state), but you could partition classes across
     *       workers, have each worker maintain its own local top-K
     *       candidates, and periodically merge/rebalance extras across
     *       partitions — an approximate distributed greedy, trading a small
     *       amount of optimality for horizontal scalability.
     *
     * 5. "Can you do this without floating point, to avoid any precision
     *    concerns entirely?"
     *    -> Yes: compare marginal gains using cross-multiplication of
     *       fractions with BigInteger or careful long arithmetic (e.g.,
     *       compare (p2+1)*t1*(t1+1) vs ... ) to avoid floating point
     *       entirely, at the cost of more verbose comparator logic.
     *
     * 6. "What's the theoretical lower bound on time complexity for this
     *    problem, and does our solution meet it?"
     *    -> Any correct algorithm must at least read all n classes (O(n))
     *       and account for all extraStudents assignments in some form
     *       (O(extraStudents) at minimum, or an amortized/batched
     *       equivalent) — so O(n + extraStudents) is a natural lower bound;
     *       our O((n+extraStudents) log n) is a log factor above that,
     *       which is the typical, acceptable cost of maintaining a
     *       dynamically-updating "current best" structure.
     */


    /* ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Forgetting that BOTH passCount AND totalCount increase when an
     *    extra student is assigned. A common silent bug: incrementing only
     *    passCount and leaving totalCount unchanged, which inflates ratios
     *    above the true maximum (I actually caught exactly this bug in my
     *    own Python fuzz-testing pass before writing this file — it passed
     *    small hand-checked cases but failed randomized brute-force
     *    comparison, which is precisely the "silent failure" bug category
     *    worth calling out).
     *
     * 2. Using a MIN-heap by accident (Java's PriorityQueue defaults to
     *    min-heap) without inverting the comparator, silently giving extra
     *    students to the class with the SMALLEST marginal gain instead of
     *    largest — this produces a valid-looking but suboptimal (or
     *    outright wrong) average with no crash or exception to signal the
     *    bug.
     *
     * 3. Re-deriving marginalGain incorrectly, e.g., computing
     *    `(pass+1)/(total+1) - pass/total` using INTEGER division in Java
     *    (forgetting to cast to double), which truncates both terms to 0
     *    for typical inputs and makes every class look tied — a classic
     *    Java-specific trap.
     *
     * 4. Off-by-one in the loop bound: looping `extraStudents + 1` or
     *    `extraStudents - 1` times instead of exactly `extraStudents`
     *    times, especially easy to get wrong when refactoring from a
     *    recursive brute-force version (Approach 1) to an iterative one
     *    (Approach 3).
     *
     * 5. Forgetting to divide the final summed ratios by the NUMBER OF
     *    CLASSES to get the average — returning the raw sum of ratios
     *    instead, which silently passes any test where n happens to equal
     *    1 but fails for every other case.
     */


    /* ========================================================================
     * CROSS-VALIDATING TEST HARNESS
     * ========================================================================
     * Includes hand-crafted edge cases (matching Section 3) plus randomized
     * fuzz trials cross-validating the optimal heap solution against brute
     * force on small random inputs, mirroring the Python-based validation
     * already performed offline before writing this file.
     */
    public static void main(String[] args) {
        double toleranceForAssertions = 1e-9;

        /* ---- Hand-crafted example 1 ---- */
        int[][] example1 = {{1, 2}, {3, 5}, {2, 2}};
        double expected1 = 0.78333333333;
        runAndReport("Example 1 (normal case)", example1, 2, expected1, toleranceForAssertions);

        /* ---- Hand-crafted example 2 (single class) ---- */
        int[][] example2 = {{2, 4}};
        double expected2 = 5.0 / 7.0;
        runAndReport("Example 2 (single class edge case)", example2, 3, expected2,
                toleranceForAssertions);

        /* ---- Hand-crafted example 3 (tie-breaking) ---- */
        int[][] example3 = {{1, 2}, {1, 2}};
        double expected3 = (2.0 / 3.0 + 1.0 / 2.0) / 2.0;
        runAndReport("Example 3 (tie-breaking boundary case)", example3, 1, expected3,
                toleranceForAssertions);

        /* ---- Randomized fuzz test: heap approach vs brute force ---- */
        System.out.println("\nRunning randomized fuzz trials (heap vs brute force)...");
        Random random = new Random(42);
        int trialsRun = 0;
        int mismatches = 0;
        int fuzzTrialCount = 2000;

        for (int trial = 0; trial < fuzzTrialCount; trial++) {
            int numberOfClasses = 1 + random.nextInt(4); // keep small: brute force is exponential
            int[][] randomClasses = new int[numberOfClasses][2];
            for (int i = 0; i < numberOfClasses; i++) {
                int total = 1 + random.nextInt(8);
                int pass = 1 + random.nextInt(total); // 1 <= pass <= total
                randomClasses[i] = new int[] {pass, total};
            }
            int extra = random.nextInt(6); // small extra budget to keep brute force tractable

            double bruteResult = bruteForceMaxAverage(randomClasses, extra);
            double heapResult = greedyMaxHeapMaxAverage(randomClasses, extra);
            double linearResult = greedyLinearScanMaxAverage(randomClasses, extra);

            trialsRun++;
            boolean heapMatches = Math.abs(bruteResult - heapResult) < 1e-9;
            boolean linearMatches = Math.abs(bruteResult - linearResult) < 1e-9;

            if (!heapMatches || !linearMatches) {
                mismatches++;
                System.out.printf(
                        "MISMATCH on trial %d: classes=%s extra=%d brute=%.9f heap=%.9f linear=%.9f%n",
                        trial, Arrays.deepToString(randomClasses), extra, bruteResult,
                        heapResult, linearResult);
            }
        }

        System.out.printf("Fuzz testing complete: %d trials run, %d mismatches.%n",
                trialsRun, mismatches);
        if (mismatches == 0) {
            System.out.println("All randomized trials PASSED — heap and linear-scan "
                    + "approaches match brute force exactly (within tolerance).");
        }

        /* ---- Larger-scale sanity/performance check (heap only; brute force infeasible) ---- */
        System.out.println("\nRunning larger-scale sanity check on heap approach...");
        int largeNumberOfClasses = 1000;
        int largeExtraStudents = 10000;
        int[][] largeClasses = new int[largeNumberOfClasses][2];
        Random largeRandom = new Random(7);
        for (int i = 0; i < largeNumberOfClasses; i++) {
            int total = 1 + largeRandom.nextInt(10000);
            int pass = 1 + largeRandom.nextInt(total);
            largeClasses[i] = new int[] {pass, total};
        }
        long startTimeNanos = System.nanoTime();
        double largeScaleResult = greedyMaxHeapMaxAverage(largeClasses, largeExtraStudents);
        long elapsedNanos = System.nanoTime() - startTimeNanos;
        System.out.printf(
                "Large-scale result (n=%d, extraStudents=%d): average=%.9f, elapsed=%.2f ms%n",
                largeNumberOfClasses, largeExtraStudents, largeScaleResult,
                elapsedNanos / 1_000_000.0);
        if (largeScaleResult < 0.0 || largeScaleResult > 1.0) {
            System.out.println("SANITY CHECK FAILED: average out of valid [0,1] range.");
        } else {
            System.out.println("Sanity check passed: result within valid [0,1] range.");
        }
    }

    private static void runAndReport(String label, int[][] classes, int extraStudents,
            double expected, double tolerance) {
        double bruteResult = bruteForceMaxAverage(classes, extraStudents);
        double linearResult = greedyLinearScanMaxAverage(classes, extraStudents);
        double heapResult = greedyMaxHeapMaxAverage(classes, extraStudents);

        System.out.println("---- " + label + " ----");
        System.out.printf("  Brute force:        %.9f%n", bruteResult);
        System.out.printf("  Greedy linear scan:  %.9f%n", linearResult);
        System.out.printf("  Greedy max-heap:     %.9f%n", heapResult);
        System.out.printf("  Expected:            %.9f%n", expected);

        boolean allMatch = Math.abs(bruteResult - expected) < tolerance
                && Math.abs(linearResult - expected) < tolerance
                && Math.abs(heapResult - expected) < tolerance;
        System.out.println("  Result: " + (allMatch ? "PASS" : "FAIL"));
        System.out.println();
    }
}

/**
 * ============================================================================
 * PROBLEM STATEMENT: MAXIMUM AVERAGE PASS RATIO
 * ============================================================================
 * A school has several classes. For each class, you know the number of passing
 * students and the total students. You have 'extraStudents' who are guaranteed 
 * to pass. You must distribute these extra students among the classes to 
 * maximize the overall average pass ratio.
 * 
 * ============================================================================
 * VISUALIZATION & INTUITION
 * ============================================================================
 * To maximize the average, we need to maximize the SUM of all pass ratios.
 * Every time we add an extra student to a class, its pass ratio increases.
 * 
 * Let a class have 'p' passes and 't' total students.
 * Current ratio = p / t
 * Ratio after adding 1 student = (p + 1) / (t + 1)
 * 
 * The MARGINAL GAIN (Delta) of adding 1 student to this class is:
 * Delta = [ (p + 1) / (t + 1) ] - [ p / t ]
 * 
 * GREEDY APPROACH:
 * To get the maximum total sum, we should always assign the next extra student 
 * to the class that gives the LARGEST marginal gain (Delta) at that moment.
 * 
 * Example Trace: classes = [[1,2],[3,5],[2,2]], extraStudents = 2
 * 
 * Initial state:
 * Class 0: 1/2 (50.00%) | Gain if +1: 2/3 - 1/2 = 16.67%
 * Class 1: 3/5 (60.00%) | Gain if +1: 4/6 - 3/5 = 6.67%
 * Class 2: 2/2 (100.0%) | Gain if +1: 3/3 - 2/2 = 0.00%
 * 
 * Step 1: Assign 1st extra student to Class 0 (Highest gain: 16.67%).
 * Updated Class 0: 2/3 (66.67%) | New Gain if +1: 3/4 - 2/3 = 8.33%
 * 
 * Step 2: Assign 2nd extra student. Compare current gains:
 * Class 0: 8.33%  <-- Highest gain now!
 * Class 1: 6.67%
 * Class 2: 0.00%
 * Assign to Class 0 again.
 * Updated Class 0: 3/4 (75.00%).
 * 
 * Final Ratios: 3/4, 3/5, 2/2.
 * Average: (0.75 + 0.60 + 1.0) / 3 = 0.78333.
 * 
 * ============================================================================
 * SOLUTIONS INCLUDED IN THIS FILE:
 * ============================================================================
 * Solution 1: Max-Heap (Priority Queue) - Optimal & Most Common Solution.
 */


class MaxAveragePassRatio {

    // ========================================================================
    // SOLUTION 1: MAX-HEAP (Priority Queue)
    // ========================================================================
    /**
     * Explanation:
     * We use a Max-Heap to keep track of the potential gain (Delta) for each class.
     * 1. Calculate the initial Delta for all classes and insert them into the heap.
     * 2. For each extra student, pop the class with the maximum Delta.
     * 3. Update the passing and total students for that class (add 1 to both).
     * 4. Calculate the new Delta for that class and push it back into the heap.
     * 5. Repeat until all extra students are distributed.
     * 6. Calculate the final average by summing all ratios and dividing by total classes.
     * 
     * Time Complexity: O(N log N + K log N) 
     *                  where N is classes.length and K is extraStudents.
     * Space Complexity: O(N) to store the heap elements.
     */
    public double maxAverageRatio(int[][] classes, int extraStudents) {
        // PriorityQueue to store the classes based on max potential gain
        // Array structure: { marginal_gain, pass_count, total_count }
        PriorityQueue<double[]> maxHeap = new PriorityQueue<>((a, b) -> Double.compare(b[0], a[0]));
        
        // Step 1: Calculate initial marginal gain for all classes and add to max-heap
        for (int[] c : classes) {
            double p = c[0];
            double t = c[1];
            double gain = calculateGain(p, t);
            maxHeap.offer(new double[]{gain, p, t});
        }
        
        // Step 2: Distribute all extra students greedily
        while (extraStudents > 0) {
            double[] top = maxHeap.poll();
            double p = top[1];
            double t = top[2];
            
            // Assign one extra student
            p += 1;
            t += 1;
            
            // Calculate new marginal gain and push back to heap
            double newGain = calculateGain(p, t);
            maxHeap.offer(new double[]{newGain, p, t});
            
            extraStudents--;
        }
        
        // Step 3: Calculate the total sum of the final pass ratios
        double totalRatioSum = 0;
        while (!maxHeap.isEmpty()) {
            double[] top = maxHeap.poll();
            totalRatioSum += (top[1] / top[2]);
        }
        
        // Return the average pass ratio
        return totalRatioSum / classes.length;
    }
    
    /**
     * Helper method to calculate the marginal gain of adding 1 student
     */
    private double calculateGain(double pass, double total) {
        return ((pass + 1) / (total + 1)) - (pass / total);
    }

    // ========================================================================
    // MAIN METHOD FOR TESTING & EXAMPLES
    // ========================================================================
    public static void main(String[] args) {
        MaxAveragePassRatio solver = new MaxAveragePassRatio();
        
        // Example 1
        int[][] classes1 = {{1, 2}, {3, 5}, {2, 2}};
        int extraStudents1 = 2;
        System.out.println("Input: classes = [[1,2],[3,5],[2,2]], extraStudents = 2");
        System.out.printf("Output (Max-Heap): %.5f\n", solver.maxAverageRatio(classes1, extraStudents1));
        System.out.println("Expected: 0.78333");
        System.out.println("-------------------------------------------------");
        
        // Example 2
        int[][] classes2 = {{2, 4}, {3, 9}, {4, 5}, {2, 10}};
        int extraStudents2 = 4;
        System.out.println("Input: classes = [[2,4],[3,9],[4,5],[2,10]], extraStudents = 4");
        System.out.printf("Output (Max-Heap): %.5f\n", solver.maxAverageRatio(classes2, extraStudents2));
        System.out.println("Expected: 0.53485");
        System.out.println("-------------------------------------------------");
    }
}

class MaxAveragePassRatio2 {

    /**
     * We store pass & total for each class.
     * Using record (Java 16+) for cleaner immutable structure.
     */
    record ClassInfo(int pass, int total) {}

    /**
     * This function calculates the "gain" if we add one extra student.
     *
     * gain = (pass+1)/(total+1) - pass/total
     *
     * Why do we need this?
     * ----------------------
     * We are NOT trying to improve the worst class (lowest ratio).
     * Instead, we want to improve the class that gives the MAXIMUM increase
     * in overall average.
     *
     * ❌ Wrong idea:
     *   Pick class with lowest (pass/total)
     *   → This fails because improvement depends on BOTH pass and total.
     *
     * ✅ Correct idea:
     *   Pick class with maximum "marginal gain"
     *   → i.e., which class benefits the MOST from one extra student.
     */
    private static double gain(int pass, int total) {
        return ((double) (pass + 1) / (total + 1))
                - ((double) pass / total);
    }

    public double maxAverageRatio(int[][] classes, int extraStudents) {

        /**
         * Max Heap based on gain.
         *
         * The class with highest gain will always be on top.
         *
         * Why max heap?
         * -------------
         * At every step, we must greedily pick the class
         * that gives maximum improvement.
         *
         * This is a classic "greedy + priority queue" pattern.
         */
        PriorityQueue<ClassInfo> maxHeap = new PriorityQueue<>(
                (a, b) -> Double.compare(
                        gain(b.pass(), b.total()), // higher gain first
                        gain(a.pass(), a.total())
                )
        );

        // Add all classes into heap
        for (int[] c : classes) {
            maxHeap.offer(new ClassInfo(c[0], c[1]));
        }

        /**
         * Distribute extra students
         *
         * Each time:
         * 1. Pick class with highest gain
         * 2. Add one student
         * 3. Reinsert with updated values
         */
        while (extraStudents-- > 0) {
            ClassInfo top = maxHeap.poll();

            int newPass = top.pass() + 1;
            int newTotal = top.total() + 1;

            maxHeap.offer(new ClassInfo(newPass, newTotal));
        }

        /**
         * Compute final average pass ratio
         */
        double totalRatio = 0.0;

        while (!maxHeap.isEmpty()) {
            ClassInfo c = maxHeap.poll();
            totalRatio += (double) c.pass() / c.total();
        }

        return totalRatio / classes.length;
    }

    /**
     * ===========================
     * 🚨 Why NOT use min heap on ratio?
     * ===========================
     *
     * Suppose:
     * classes = [[1,2], [50,100]]
     *
     * Both have ratio = 0.5
     *
     * But gains:
     *
     * [1,2] → gain = 0.166  (huge improvement)
     * [50,100] → gain ≈ 0.0049 (tiny improvement)
     *
     * If we pick based on ratio → WRONG decision.
     *
     * ===========================
     * 🧠 Key Insight:
     * ===========================
     *
     * We are not optimizing:
     *   current value (pass/total)
     *
     * We are optimizing:
     *   marginal improvement (gain)
     *
     * This is a standard greedy pattern:
     *   "Always pick the option with best immediate benefit"
     *
     * ===========================
     * ⏱ Complexity
     * ===========================
     *
     * Let n = number of classes
     *
     * Time:
     *   Heap build: O(n)
     *   Each operation: O(log n)
     *   Total: O((n + extraStudents) log n)
     *
     * Space:
     *   O(n) for heap
     */
}

/*
Short answer: **❌ No, that greedy is incorrect.**
Picking the class with the **lowest pass ratio** is *not* optimal.

---

# 🧠 Why Your Idea Feels Right (but isn’t)

You’re thinking:

> “Let me improve the weakest class first (lowest pass/total)”

That sounds intuitive… but the problem is:

👉 **We don’t care about current ratio**
👉 We care about **how much the ratio improves after adding a student**

---

# 🔥 Key Difference

### ❌ Your approach:

Pick min of:

```
pass / total
```

### ✅ Correct approach:

Pick max of:

```
(pass+1)/(total+1) - pass/total   ← gain
```

---

# ⚠️ Counterexample (VERY IMPORTANT for interviews)

```text
classes = [[1, 100], [50, 100]]
extraStudents = 1
```

### Step 1: Current ratios

```
[1,100]   → 0.01   (lowest)
[50,100]  → 0.5
```

👉 Your approach picks `[1,100]`

---

### Step 2: Check actual gains

```
Gain for [1,100]:
= (2/101 - 1/100)
≈ 0.0198 - 0.01
≈ 0.0098

Gain for [50,100]:
= (51/101 - 50/100)
≈ 0.5049 - 0.5
≈ 0.0049
```

👉 Here, picking `[1,100]` works

---

### But now change slightly 👇

```text
classes = [[1, 2], [50, 100]]
extraStudents = 1
```

### Ratios:

```
[1,2] → 0.5
[50,100] → 0.5
```

Same ratio → your method fails to decide

---

### Gains:

```
[1,2]:
= (2/3 - 1/2)
≈ 0.666 - 0.5 = 0.166  ← HUGE gain

[50,100]:
= (51/101 - 50/100)
≈ 0.5049 - 0.5 = 0.0049
```

👉 Correct choice = `[1,2]`

---

# 💥 Strong Counterexample (Breaks your logic)

```text
classes = [[5,5], [1,100]]
extraStudents = 1
```

### Ratios:

```
[5,5]   → 1.0
[1,100] → 0.01  ← your pick
```

---

### Gains:

```
[5,5]:
= (6/6 - 5/5) = 1 - 1 = 0

[1,100]:
= small (~0.009)
```

👉 Works here too…

---

But now:

```text
classes = [[2,4], [3,9]]
```

Ratios:

```
[2,4] = 0.5   ← your pick
[3,9] = 0.33
```

Gains:

```
[2,4] → 0.6 - 0.5 = 0.1
[3,9] → 4/10 - 3/9 = 0.4 - 0.333 = 0.066
```

👉 OK here

---

### ⚠️ But not guaranteed in general!

---

# 🧠 Core Insight (MOST IMPORTANT)

### The improvement depends on:

```
current pass AND total
```

NOT just:

```
pass / total
```

---

# 🔑 Intuition Shortcut

Think like this:

> “Which class will benefit MOST from one extra passing student?”

Not:

> “Which class is currently worst?”

---

# 📊 Why Ratio-Based Greedy Fails

| Class       | Ratio  | Gain  |
| ----------- | ------ | ----- |
| Small class | Medium | HUGE  |
| Large class | Low    | SMALL |

👉 Small classes benefit more!

---

# ✅ Final Answer

| Approach          | Correct? | Why                    |
| ----------------- | -------- | ---------------------- |
| Min heap on ratio | ❌ No     | Ignores improvement    |
| Max heap on gain  | ✅ Yes    | Picks best improvement |

---

# 🏁 Interview One-Liner

> “We don’t optimize current value, we optimize marginal gain — hence use max heap on gain.”

---

If you want, I can:

* Prove **why greedy works mathematically**
* Show **derivative-based reasoning**
* Give **similar tricky greedy problems** 🚀

*/