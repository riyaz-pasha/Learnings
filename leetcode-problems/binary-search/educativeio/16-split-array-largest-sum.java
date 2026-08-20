import java.util.*;

/**
 * =====================================================================================
 * MOCK GOOGLE ONSITE INTERVIEW TRANSCRIPT
 * Problem: Split Array Largest Sum   (LeetCode 410, "Hard")
 * =====================================================================================
 *
 * This single file is structured as a full interview walkthrough. Each section is
 * clearly labeled with a block comment. Read top to bottom the way you would narrate
 * it live to an interviewer.
 */
class SplitArrayLargestSum {

    /*
     * =================================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * =================================================================================
     *
     * "I'm given an array of non-negative integers `nums` and an integer `k`. I need to
     * split `nums` into exactly `k` contiguous, non-empty subarrays (I cannot reorder
     * or skip elements — the split points just carve the array into k consecutive
     * pieces). Each split has a 'cost', defined as the largest sum among its k pieces.
     * Among all valid ways to split into k pieces, I want to choose the split that
     * MINIMIZES that largest-piece sum, and return that minimized value (a single
     * integer, not the partition itself, unless asked)."
     *
     * Key constraints (from the prompt):
     *   - 1 <= nums.length <= 10^3
     *   - 0 <= nums[i] <= 10^4
     *   - 1 <= k <= nums.length
     *
     * Key observations to state out loud:
     *   - Subarrays must be CONTIGUOUS (this is a partition problem, not a subset
     *     problem) — this immediately rules out approaches that rely on reordering
     *     (sorting) or arbitrary grouping (hashing).
     *   - This is a classic "minimize the maximum" optimization — a strong signal
     *     for "binary search on the answer" combined with a greedy feasibility check.
     *   - Values are non-negative, which matters: it guarantees prefix sums are
     *     monotonically non-decreasing, which is what makes the greedy feasibility
     *     check well-defined and correct.
     */

    /*
     * =================================================================================
     * SECTION 2: CLARIFYING QUESTIONS
     * =================================================================================
     *
     * Q1: Can `nums` contain zeros?
     *     A1 (assumed): Yes, constraint says 0 <= nums[i], so zeros are explicitly
     *         allowed. This matters for the greedy check's edge cases.
     *
     * Q2: Can `k` equal `nums.length`? Can `k` equal 1?
     *     A2 (assumed): Yes to both — constraint says 1 <= k <= nums.length. k == n
     *         means every element is its own subarray (answer = max(nums)). k == 1
     *         means no splitting at all (answer = sum(nums)).
     *
     * Q3: Do I need to return the actual partition (the split indices), or just the
     *     minimized largest sum as an integer?
     *     A3 (assumed): Just the integer value, per the problem statement. I'll
     *         mention that reconstructing the partition is a trivial follow-up (one
     *         more greedy pass) if asked.
     *
     * Q4: Are duplicate values in `nums` something I need to handle specially?
     *     A4 (assumed): No special handling needed — duplicates don't break
     *         contiguous-sum logic; they're a non-issue here (unlike, say, a
     *         subset/combination problem where duplicates cause redundant branches).
     *
     * Q5: Is `nums` guaranteed non-empty, and can it contain negative numbers?
     *     A5 (assumed): Non-empty is guaranteed (length >= 1). No negatives, per
     *         constraints — this is important, because binary search on the answer
     *         relies on prefix sums being monotonic, which breaks with negatives.
     *
     * Q6: What are the practical bounds on runtime? Given n <= 1000 and values up to
     *     10^4, what complexity is acceptable?
     *     A6 (assumed): n <= 1000, so O(n^2) or even O(n^2 * k) is technically
     *         tolerable, but I should still aim for the asymptotically better
     *         O(n log(sum(nums))) solution to demonstrate strong technique.
     *
     * Q7: Is this a single call, or should I expect many repeated queries against the
     *     same array with different k (i.e., do I need to worry about amortization
     *     or precomputation across calls)?
     *     A7 (assumed): Single call for now. I'll note that if this were called
     *         repeatedly with different k on the same array, precomputing prefix
     *         sums once (already O(n)) is the only reusable work; the binary search
     *         itself is cheap enough that no further caching is needed.
     *
     * Q8: Any concurrency or thread-safety requirements (e.g., is `nums` mutated
     *     concurrently while I compute)?
     *     A8 (assumed): No — this is a single-threaded, pure-function computation
     *         over an immutable input array.
     */

    /*
     * =================================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * =================================================================================
     *
     * Example 1 (normal case):
     *   nums = [7, 2, 5, 10, 8], k = 2
     *   Best split: [7,2,5] and [10,8]  -> sums 14 and 18 -> largest = 18
     *   (Alternative [7,2] / [5,10,8] gives sums 9 and 23 -> worse, largest = 23)
     *   Answer: 18
     *
     * Example 2 (edge case: k == nums.length, every element is its own subarray):
     *   nums = [1, 4, 4], k = 3
     *   Forced split: [1] [4] [4] -> largest sum = 4
     *   Answer: 4  (this is simply max(nums))
     *
     * Example 3 (boundary / tie-breaking case: k == 1, and an array containing 0s):
     *   nums = [0, 0, 0, 10, 0], k = 1
     *   Only one valid split: the whole array -> sum = 10
     *   Answer: 10  (this is simply sum(nums))
     *   This also stresses the "zeros are allowed" clarification from Section 2.
     *
     *   A second boundary case worth mentioning verbally: when two different split
     *   points produce the SAME largest sum, any of them is an acceptable answer
     *   since we only return the minimized value, not the split itself — so there is
     *   no real "tie-breaking rule" to design, which simplifies the problem versus,
     *   say, interval-scheduling problems where tie-breaks affect correctness.
     */

    /*
     * =================================================================================
     * SECTION 4 & 5: ALL POSSIBLE SOLUTIONS (across applicable paradigms)
     * =================================================================================
     *
     * Paradigms considered and whether they apply:
     *
     *   - Brute force / naive           -> APPLIES (Approach 1: try every split)
     *   - Sorting-based                 -> DOES NOT APPLY: subarrays must stay
     *                                       contiguous in original order; sorting
     *                                       would destroy the array's structure.
     *   - Hashing-based                 -> DOES NOT APPLY: there's no
     *                                       lookup/frequency/pairing sub-problem
     *                                       here; contiguous-sum partitioning gives
     *                                       hashing nothing to index.
     *   - Two pointer / sliding window  -> DOES NOT APPLY DIRECTLY: sliding window
     *                                       solves "find a contiguous range meeting
     *                                       a condition" for a SINGLE window; here
     *                                       we must simultaneously place k-1 cut
     *                                       points, which a two-pointer scan cannot
     *                                       track on its own. (It DOES reappear
     *                                       as the inner feasibility check driving
     *                                       our greedy/binary-search solution.)
     *   - Divide and conquer            -> DOES NOT naturally apply: there's no
     *                                       clean way to split the array in half,
     *                                       solve each half independently, and
     *                                       merge, because the optimal k-way split
     *                                       doesn't decompose that way (the split
     *                                       counts on each side interact).
     *   - Greedy (alone, no verification) -> DOES NOT WORK STANDALONE: a naive
     *                                       greedy like "keep adding until adding
     *                                       the next element would exceed the
     *                                       current running max" has no notion of
     *                                       what threshold to target, so it cannot
     *                                       run without an outer search driving it.
     *                                       It DOES work as a feasibility check
     *                                       inside binary search (Approach 3).
     *   - Dynamic programming           -> APPLIES (Approach 2: partition DP)
     *   - Tree / graph traversal        -> DOES NOT APPLY: no graph/tree structure
     *                                       in the problem.
     *   - Heap / priority queue         -> DOES NOT APPLY: no "always take the
     *                                       smallest/largest of a changing set"
     *                                       sub-problem; nothing to prioritize.
     *   - Binary search                 -> APPLIES (Approach 3: binary search on
     *                                       the answer value, the optimal solution)
     *   - Monotonic stack / deque       -> DOES NOT APPLY: no need to maintain a
     *                                       monotonic window of indices/values.
     *   - Trie / segment tree / advanced -> DOES NOT APPLY: no prefix-matching or
     *                                       range-query structure needed beyond a
     *                                       simple prefix-sum array.
     *
     * So the three genuinely meaningful approaches are: Brute Force, DP, and
     * Binary-Search-on-Answer (+ Greedy feasibility check).
     */

    /*
     * ---------------------------------------------------------------------------------
     * Approach 1: Brute Force — Recursive Enumeration of All Split Points
     * ---------------------------------------------------------------------------------
     * Core idea:
     *   Recursively decide where the LAST subarray starts. For a suffix nums[i..n-1]
     *   that still needs to be split into `piecesRemaining` parts, try every possible
     *   split point j (i <= j < n) for the first piece nums[i..j], and recurse on the
     *   remainder nums[j+1..n-1] with piecesRemaining-1. Take the split that minimizes
     *   the maximum piece sum across all choices.
     *
     * Paradigm: exhaustive recursion / backtracking over partitions.
     *
     * Time Complexity: O(n^(k)) in the worst case (roughly C(n-1, k-1) ways to place
     *   k-1 cut points among n-1 gaps, each explored with extra work to sum pieces) —
     *   exponential and only viable for tiny n and k. Precisely, without memoization,
     *   this revisits the same (i, piecesRemaining) states repeatedly, which is
     *   exactly the redundancy that DP (Approach 2) eliminates.
     * Space Complexity: O(k) recursion depth, plus O(n) for prefix sums.
     *
     * Pros:
     *   - Trivial to reason about correctness; a great way to open the interview and
     *     confirm you understand the problem before optimizing.
     *   - Naturally extends to reconstructing the actual split if asked.
     * Cons:
     *   - Exponential blow-up; useless for n up to 10^3 as given in constraints.
     * When to use:
     *   - Only as a warm-up / correctness oracle for testing, or for extremely small
     *     inputs (n <= ~12). Never ship this for the given constraints.
     */
    static int splitArrayBruteForce(int[] nums, int k) {
        int n = nums.length;
        long[] prefixSums = new long[n + 1];
        for (int index = 0; index < n; index++) {
            prefixSums[index + 1] = prefixSums[index] + nums[index];
        }
        return (int) bruteForceRecurse(prefixSums, 0, n, k);
    }

    // Returns the minimized largest-piece sum for nums[startIndex..n-1] split into
    // exactly piecesRemaining contiguous, non-empty pieces.
    private static long bruteForceRecurse(long[] prefixSums, int startIndex, int n, int piecesRemaining) {
        if (piecesRemaining == 1) {
            // Only one piece left: it must be the entire remaining suffix.
            return prefixSums[n] - prefixSums[startIndex];
        }
        long best = Long.MAX_VALUE;
        // Try every possible end index (exclusive) for the FIRST piece of this suffix.
        // The first piece must leave at least (piecesRemaining - 1) elements behind
        // for the remaining pieces, each of which needs >= 1 element.
        for (int firstPieceEnd = startIndex + 1; firstPieceEnd <= n - (piecesRemaining - 1); firstPieceEnd++) {
            long firstPieceSum = prefixSums[firstPieceEnd] - prefixSums[startIndex];
            long restResult = bruteForceRecurse(prefixSums, firstPieceEnd, n, piecesRemaining - 1);
            long candidate = Math.max(firstPieceSum, restResult);
            best = Math.min(best, candidate);
        }
        return best;
    }

    /*
     * ---------------------------------------------------------------------------------
     * Approach 2: Dynamic Programming — Partition DP over (prefix length, pieces used)
     * ---------------------------------------------------------------------------------
     * Core idea:
     *   Define dp[i][p] = the minimized largest-piece sum when splitting the FIRST i
     *   elements (nums[0..i-1]) into exactly p pieces. To compute dp[i][p], try every
     *   possible boundary j for the LAST piece (nums[j..i-1]), and combine:
     *       dp[i][p] = min over j of max(dp[j][p-1], sum(nums[j..i-1]))
     *   Base case: dp[0][0] = 0. Answer is dp[n][k].
     *   This is exactly the brute force recursion above, but memoized — the same
     *   (i, p) subproblems recur constantly across branches, so caching collapses the
     *   exponential tree into a polynomial-size DP table.
     *
     * Paradigm: dynamic programming (2D table), with prefix sums for O(1) range-sum
     *   lookups.
     *
     * Time Complexity: O(n^2 * k) — for each of n prefix lengths and k piece counts,
     *   we try up to n possible boundaries j.
     * Space Complexity: O(n * k) for the DP table (can be reduced to O(n) with rolling
     *   arrays since dp[i][p] only depends on row p-1, but I'll keep the full table
     *   for clarity unless asked to optimize space).
     *
     * Pros:
     *   - Fully polynomial, correct, and easy to prove correct via the recurrence.
     *   - A very natural, defensible "first real solution" to present after brute
     *     force — shows you can systematically remove redundant recomputation.
     * Cons:
     *   - O(n^2 * k) is noticeably worse than the O(n log(sum)) binary search
     *     solution — with n = 1000, this is up to ~10^9 operations in the worst
     *     case (k close to n), which is borderline/too slow for a tight time limit.
     * When to use:
     *   - Great as an "intermediate, defensible" answer, or when you additionally
     *     need to reconstruct the actual partition easily (backtracking through the
     *     DP table is very natural). Use the binary search approach when performance
     *     is the priority and only the minimized value (not the partition) is needed.
     */
    static int splitArrayDP(int[] nums, int k) {
        int n = nums.length;
        long[] prefixSums = new long[n + 1];
        for (int index = 0; index < n; index++) {
            prefixSums[index + 1] = prefixSums[index] + nums[index];
        }

        long INFINITY = Long.MAX_VALUE / 2;
        // dp[i][p] = min largest-piece sum splitting nums[0..i-1] into p pieces.
        long[][] dp = new long[n + 1][k + 1];
        for (long[] row : dp) {
            Arrays.fill(row, INFINITY);
        }
        dp[0][0] = 0;

        for (int prefixLength = 1; prefixLength <= n; prefixLength++) {
            int maxPiecesForThisLength = Math.min(prefixLength, k);
            for (int pieces = 1; pieces <= maxPiecesForThisLength; pieces++) {
                // Try every boundary j: last piece is nums[j..prefixLength-1].
                for (int boundary = pieces - 1; boundary < prefixLength; boundary++) {
                    if (dp[boundary][pieces - 1] == INFINITY) {
                        continue; // unreachable sub-state
                    }
                    long lastPieceSum = prefixSums[prefixLength] - prefixSums[boundary];
                    long candidate = Math.max(dp[boundary][pieces - 1], lastPieceSum);
                    if (candidate < dp[prefixLength][pieces]) {
                        dp[prefixLength][pieces] = candidate;
                    }
                }
            }
        }
        return (int) dp[n][k];
    }

    /*
     * ---------------------------------------------------------------------------------
     * Approach 3 (OPTIMAL): Binary Search on the Answer + Greedy Feasibility Check
     * ---------------------------------------------------------------------------------
     * Core idea:
     *   Reframe the question: instead of directly computing the minimized largest
     *   sum, ask "CAN we split nums into at most k pieces such that every piece's
     *   sum is <= some candidate threshold X?" This feasibility check is a simple,
     *   fast GREEDY linear scan: walk through nums, accumulate a running sum into
     *   the current piece, and whenever adding the next element would exceed X,
     *   close off the current piece and start a new one. Count how many pieces this
     *   requires; if it's <= k, X is feasible.
     *
     *   This feasibility function is MONOTONIC in X: if threshold X works, any
     *   threshold > X also works (you'd only need the same or fewer pieces). That
     *   monotonicity is exactly what licenses BINARY SEARCH over the answer space.
     *
     *   Search bounds:
     *     lo = max(nums)     -- any single element must fit in its own piece, so the
     *                           answer can never be smaller than the largest element.
     *     hi = sum(nums)     -- the trivial k=1 case; the answer can never need to
     *                           exceed putting everything in one piece.
     *   Binary search for the SMALLEST X in [lo, hi] for which canSplit(X) is true.
     *
     * Paradigm: binary search on the answer (a numeric search space), driven by a
     *   greedy O(n) feasibility check. (This is where the "sliding window" spirit
     *   reappears: canSplit does a single linear pass, growing a window/running sum
     *   until it must reset — but it is not solving the whole problem by itself, it's
     *   the verifier inside the outer binary search.)
     *
     * Time Complexity: O(n log(sum(nums))). The binary search runs O(log(sum(nums)))
     *   iterations (sum(nums) <= 10^3 * 10^4 = 10^7, so ~24 iterations), and each
     *   iteration's feasibility check is O(n). With n <= 1000, this is trivially fast.
     * Space Complexity: O(1) extra space (beyond the input array) — no DP table, no
     *   recursion stack of meaningful depth.
     *
     * Pros:
     *   - Asymptotically the best of the three approaches, and simple to implement
     *     once you see the "binary search on the answer" framing.
     *   - O(1) auxiliary space — much lighter than the DP table.
     *   - The greedy feasibility check is easy to prove correct: it is the "most
     *     greedy" packing (always defers splitting as long as possible), and any
     *     feasible split into <= k pieces for threshold X implies this greedy
     *     packing also uses <= k pieces (exchange-argument style: greedy never does
     *     worse than any other valid packing at the same threshold, because closing
     *     a piece as late as possible only gives future pieces more room, never less).
     * Cons:
     *   - Doesn't directly hand you the split indices (though they fall out for free
     *     by re-running canSplit with the final answer and recording the cut points).
     *   - Slightly less "obviously correct" at first glance than DP — requires
     *     explicitly arguing the monotonicity property to an interviewer.
     * When to use:
     *   - This is the production-quality choice: best asymptotic complexity, best
     *     space usage, and it directly generalizes to a large family of "minimize
     *     the maximum" / "maximize the minimum" problems (e.g., Koko Eating Bananas,
     *     Capacity To Ship Packages Within D Days, Magnetic Force Between Balls).
     */
    static int splitArrayBinarySearch(int[] nums, int k) {
        long lowerBound = 0;  // will become max(nums)
        long upperBound = 0;  // will become sum(nums)
        for (int value : nums) {
            lowerBound = Math.max(lowerBound, value);
            upperBound += value;
        }

        // Binary search for the smallest feasible threshold.
        while (lowerBound < upperBound) {
            long midThreshold = lowerBound + (upperBound - lowerBound) / 2;
            if (canSplitWithinThreshold(nums, k, midThreshold)) {
                // midThreshold works; try to do even better (search lower half,
                // keeping midThreshold itself as a candidate).
                upperBound = midThreshold;
            } else {
                // midThreshold is too small; we need more room.
                lowerBound = midThreshold + 1;
            }
        }
        return (int) lowerBound; // lowerBound == upperBound: the minimized largest sum
    }

    // Greedy feasibility check: can nums be split into at most k contiguous pieces,
    // each with sum <= threshold? O(n) single pass.
    private static boolean canSplitWithinThreshold(int[] nums, int k, long threshold) {
        int piecesUsed = 1;
        long currentPieceSum = 0;
        for (int value : nums) {
            // A single element larger than threshold can never fit in any piece.
            // (Not strictly required given lowerBound starts at max(nums), but kept
            // for defensive clarity / reusability of this helper.)
            if (value > threshold) {
                return false;
            }
            if (currentPieceSum + value > threshold) {
                // Close current piece, start a new one with this element.
                piecesUsed++;
                currentPieceSum = value;
                if (piecesUsed > k) {
                    return false; // early exit: already need too many pieces
                }
            } else {
                currentPieceSum += value;
            }
        }
        return true;
    }

    /*
     * =================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * =================================================================================
     *
     * | Approach                        | Time            | Space   | Best For                        | Limitations                                   |
     * |----------------------------------|-----------------|---------|----------------------------------|------------------------------------------------|
     * | 1. Brute Force (recursion)       | O(n^k) exp.     | O(k)    | Correctness oracle, tiny n       | Exponential; unusable for n up to 10^3         |
     * | 2. DP (partition DP)             | O(n^2 * k)      | O(n*k)  | Defensible mid-tier solution;    | Up to ~10^9 ops worst case; more memory than   |
     * |                                  |                 |         | easy partition reconstruction    | needed just for the value                      |
     * | 3. Binary Search + Greedy (OPT)  | O(n log(sum))   | O(1)    | Production use; best perf/space  | Doesn't hand back split indices directly       |
     *
     * (sum = sum(nums), bounded by n * max(nums) <= 10^3 * 10^4 = 10^7 here.)
     */

    /*
     * =================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * =================================================================================
     *
     * I would present Approach 3 (Binary Search on the Answer + Greedy Feasibility
     * Check) as my final solution, but I'd get there deliberately in this order:
     *
     *   1. State the brute force recursion first (Section 4, Approach 1) out loud,
     *      to lock in a correct mental model of the recurrence — WITHOUT necessarily
     *      writing all of it, just enough to describe dp[i][p].
     *   2. Immediately point out the overlapping subproblems and present the DP
     *      version (Approach 2) as a fully correct, polynomial fallback — this is a
     *      safe, defensible answer if I get stuck optimizing further.
     *   3. Pivot to the binary-search framing by asking: "the largest piece sum is
     *      always somewhere between max(nums) and sum(nums) — can I binary search
     *      that range instead of building a DP table?" This is the key insight that
     *      unlocks the optimal solution, and stating it explicitly shows strong
     *      pattern recognition (interviewers explicitly look for "minimize the
     *      maximum" => "binary search on the answer" as a signal of maturity).
     *   4. Implement Approach 3, prove the monotonicity property that justifies
     *      binary search, and prove the greedy feasibility check is correct via the
     *      exchange argument described above.
     *
     * Why this is the right final answer: best time complexity (O(n log(sum)) beats
     * O(n^2 * k) decisively at n=1000), best space complexity (O(1) vs O(n*k)), it's
     * fast to code once the framing "clicks" (~15-20 lines), and it generalizes to
     * an entire well-known family of interview problems, which signals strong pattern
     * recognition to the interviewer.
     */

    /*
     * =================================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (production-quality)
     * =================================================================================
     * See `splitArrayOptimal` below. Functionally identical to `splitArrayBinarySearch`
     * above, reproduced here as the "final answer" with exhaustive Javadoc, defensive
     * input validation, and the naming/polish I'd want in a real code review.
     */

    /**
     * Computes the minimized largest subarray sum when splitting {@code nums} into
     * exactly {@code k} non-empty, contiguous subarrays.
     *
     * <p>Strategy: binary search over the space of possible answers (candidate
     * "largest sum" thresholds), using a linear-time greedy check to test whether a
     * given threshold is achievable with at most {@code k} pieces. The feasibility
     * predicate is monotonic in the threshold, which is what makes binary search
     * valid here.</p>
     *
     * @param nums non-negative integers to split; must be non-empty
     * @param k    number of contiguous, non-empty pieces to split into;
     *             must satisfy 1 <= k <= nums.length
     * @return the minimized value of the largest piece sum across all valid splits
     * @throws IllegalArgumentException if inputs violate the stated constraints
     */
    static int splitArrayOptimal(int[] nums, int k) {
        // --- Defensive validation (would confirm with interviewer whether this
        //     level of guarding is expected; including it shows production instinct). ---
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("nums must be non-empty");
        }
        if (k < 1 || k > nums.length) {
            throw new IllegalArgumentException("k must satisfy 1 <= k <= nums.length");
        }

        // --- Establish binary search bounds ---
        // lowestPossibleAnswer: no split can do better than the single largest
        // element, since that element alone must live in some piece.
        // highestPossibleAnswer: the worst case is putting everything in one piece
        // (equivalent to k = 1), so the true answer never needs to exceed this.
        long lowestPossibleAnswer = 0;
        long highestPossibleAnswer = 0;
        for (int value : nums) {
            lowestPossibleAnswer = Math.max(lowestPossibleAnswer, value);
            highestPossibleAnswer += value;
        }

        // --- Binary search for the minimal feasible threshold ---
        // Invariant maintained throughout the loop: the true answer lies within
        // [lowestPossibleAnswer, highestPossibleAnswer].
        while (lowestPossibleAnswer < highestPossibleAnswer) {
            // Use lo + (hi - lo) / 2, not (lo + hi) / 2, to avoid any risk of
            // overflow — defensive habit even though these values are well within
            // long range here (long by default for accumulations/thresholds).
            long candidateThreshold = lowestPossibleAnswer + (highestPossibleAnswer - lowestPossibleAnswer) / 2;

            if (isFeasible(nums, k, candidateThreshold)) {
                // candidateThreshold achieves the split within k pieces: it's a
                // valid answer, but maybe we can push lower. Keep it as the current
                // best by narrowing the upper bound to it (not candidateThreshold - 1,
                // since we must not exclude a threshold that IS achievable).
                highestPossibleAnswer = candidateThreshold;
            } else {
                // candidateThreshold is too tight: we needed more than k pieces.
                // Every value <= candidateThreshold is therefore also infeasible,
                // so we can safely exclude candidateThreshold itself going forward.
                lowestPossibleAnswer = candidateThreshold + 1;
            }
        }

        // Loop invariant guarantees lowestPossibleAnswer == highestPossibleAnswer
        // at this point, and that value is the minimized largest-piece sum.
        return (int) lowestPossibleAnswer;
    }

    /**
     * Greedy feasibility check: determines whether {@code nums} can be partitioned
     * into at most {@code maxPieces} contiguous, non-empty pieces such that every
     * piece's sum is less than or equal to {@code threshold}.
     *
     * <p>Greedy strategy: extend the current piece for as long as possible; only
     * start a new piece when the next element would push the running sum over the
     * threshold. This is provably optimal for this feasibility question — deferring
     * a split as long as possible can only leave MORE room (never less) for the
     * pieces that follow, so it never uses more pieces than any other valid packing
     * at the same threshold would.</p>
     *
     * @param nums      the array being split (assumed non-negative, non-empty)
     * @param maxPieces the maximum number of pieces allowed
     * @param threshold the maximum permitted sum for any single piece
     * @return true if such a partition exists, false otherwise
     */
    private static boolean isFeasible(int[] nums, int maxPieces, long threshold) {
        int piecesUsedSoFar = 1;     // we always start with one open piece
        long runningPieceSum = 0;    // sum of the currently-open piece

        for (int currentValue : nums) {
            if (runningPieceSum + currentValue > threshold) {
                // Would overflow the current piece's budget: close it and open a
                // fresh piece starting at currentValue.
                piecesUsedSoFar++;
                runningPieceSum = currentValue;

                // Early exit: no point scanning further once we've already blown
                // the piece budget.
                if (piecesUsedSoFar > maxPieces) {
                    return false;
                }
            } else {
                runningPieceSum += currentValue;
            }
        }
        return true; // fit within maxPieces pieces at this threshold
    }

    /*
     * =================================================================================
     * SECTION 10: DRY RUN / TRACE (using the optimal solution)
     * =================================================================================
     *
     * Tracing splitArrayOptimal(nums = [7, 2, 5, 10, 8], k = 2):
     *
     * Setup:
     *   lowestPossibleAnswer  = max(nums)  = 10
     *   highestPossibleAnswer = sum(nums)  = 7+2+5+10+8 = 32
     *
     * Iteration 1:
     *   lo=10, hi=32 -> candidateThreshold = 10 + (32-10)/2 = 10 + 11 = 21
     *   isFeasible(nums, k=2, threshold=21)?
     *     piece1: 7 -> 9 -> 14 (adding 10 would make 24 > 21) => close piece1 at 14,
     *              piecesUsedSoFar becomes 2, runningPieceSum = 10
     *     piece2: 10 -> 18 (adding 8 would make 26... wait check: 10+8=18 <= 21, OK)
     *              runningPieceSum = 18
     *     End of array, piecesUsedSoFar = 2 <= k=2 -> FEASIBLE (true)
     *   Since feasible: highestPossibleAnswer = 21   (lo=10, hi=21)
     *
     * Iteration 2:
     *   lo=10, hi=21 -> candidateThreshold = 10 + (21-10)/2 = 10 + 5 = 15
     *   isFeasible(nums, k=2, threshold=15)?
     *     piece1: 7 -> 9 -> 14 (adding 10 -> 24 > 15) => close piece1 at 14,
     *              piecesUsedSoFar=2, runningPieceSum=10
     *     piece2: 10 -> (adding 8 -> 18 > 15) => close piece2, piecesUsedSoFar=3,
     *              runningPieceSum=8
     *     piecesUsedSoFar=3 > k=2 -> INFEASIBLE (false), early exit
     *   Since infeasible: lowestPossibleAnswer = 15 + 1 = 16   (lo=16, hi=21)
     *
     * Iteration 3:
     *   lo=16, hi=21 -> candidateThreshold = 16 + (21-16)/2 = 16 + 2 = 18
     *   isFeasible(nums, k=2, threshold=18)?
     *     piece1: 7 -> 9 -> 14 (adding 10 -> 24 > 18) => close piece1 at 14,
     *              piecesUsedSoFar=2, runningPieceSum=10
     *     piece2: 10 -> 18 (adding 8 -> 18, and 18 <= 18, OK) => runningPieceSum=18
     *     piecesUsedSoFar=2 <= k=2 -> FEASIBLE (true)
     *   Since feasible: highestPossibleAnswer = 18   (lo=16, hi=18)
     *
     * Iteration 4:
     *   lo=16, hi=18 -> candidateThreshold = 16 + (18-16)/2 = 16 + 1 = 17
     *   isFeasible(nums, k=2, threshold=17)?
     *     piece1: 7 -> 9 -> 14 (adding 10 -> 24 > 17) => close piece1 at 14,
     *              piecesUsedSoFar=2, runningPieceSum=10
     *     piece2: 10 -> (adding 8 -> 18 > 17) => close piece2, piecesUsedSoFar=3
     *     piecesUsedSoFar=3 > k=2 -> INFEASIBLE (false)
     *   Since infeasible: lowestPossibleAnswer = 17 + 1 = 18   (lo=18, hi=18)
     *
     * Loop condition lowestPossibleAnswer < highestPossibleAnswer is now false
     * (18 < 18 is false) -> loop terminates.
     *
     * Return (int) lowestPossibleAnswer = 18.
     *
     * This matches the hand-computed optimal split from Section 3, Example 1:
     * [7,2,5] (sum 14) and [10,8] (sum 18) -> largest sum = 18. Correct.
     */

    /*
     * =================================================================================
     * SECTION 11: CLOSING SUMMARY
     * =================================================================================
     *
     * - Brute force establishes correctness but is exponential (O(n^k)) — a starting
     *   point only, never a shippable answer for n up to 10^3.
     * - Partition DP (O(n^2 * k), O(n*k) space) is a fully correct, polynomial
     *   fallback, and is the natural choice if the interviewer also wants the actual
     *   split reconstructed (trace back through the DP table).
     * - Binary search on the answer + greedy feasibility check (O(n log(sum(nums))),
     *   O(1) space) is the optimal and recommended solution: best time and space,
     *   and it generalizes to an entire class of "minimize the maximum" problems.
     *
     * Known assumptions / limitations of the final solution:
     *   - Relies on nums containing only non-negative integers (guaranteed by the
     *     constraints) — this is what makes prefix sums monotonic and the greedy
     *     feasibility check well-defined. With negative numbers, this entire
     *     approach (and the problem's framing) would need to be rethought.
     *   - Returns only the minimized largest sum, not the split itself; recovering
     *     the split is a trivial O(n) follow-up (re-run isFeasible with the final
     *     answer and record indices where pieces close).
     *   - All accumulation uses `long` even though the final answer fits in an int
     *     here (max possible sum is 10^3 * 10^4 = 10^7, well within int range) — a
     *     deliberate defensive habit against overflow, cheap to keep and easy to
     *     justify if asked why.
     */

    /*
     * =================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * =================================================================================
     *
     * 1. "Can you also return the actual partition (the split indices), not just the
     *    minimized value?" -> Re-run isFeasible with the final answer, recording the
     *    index at which each piece closes; O(n) extra work, no extra asymptotic cost.
     *
     * 2. "What if nums could contain negative numbers?" -> Prefix sums are no longer
     *    monotonic, so the greedy feasibility check breaks (a piece's sum could
     *    decrease as you add more elements), and the binary-search-on-answer
     *    framing itself becomes invalid; you'd likely need to fall back to the DP
     *    formulation (Approach 2), which doesn't rely on monotonicity.
     *
     * 3. "What if instead of minimizing the maximum piece sum, we wanted to minimize
     *    the DIFFERENCE between the largest and smallest piece sums?" -> A
     *    fundamentally different optimization; binary search on a single answer
     *    value no longer directly applies since we're optimizing a spread, not a
     *    max — likely pushes back toward DP or a different search formulation.
     *
     * 4. "What if this function will be called many times with different k values on
     *    the same nums array — how would you optimize for repeated queries?" ->
     *    Prefix sums (O(n)) are reusable across calls as-is; the O(n log(sum)) cost
     *    per call is already small, so the main lever is just avoiding redundant
     *    prefix-sum recomputation, not algorithmic restructuring.
     *
     * 5. "Can you solve this iteratively without recursion for the DP approach, and
     *    can you reduce its space to O(n)?" -> Yes: since dp[i][p] only depends on
     *    row p-1, roll two 1D arrays (previous row / current row) instead of a full
     *    2D table, dropping space from O(n*k) to O(n).
     *
     * 6. "How would this change if subarrays did NOT need to be contiguous (i.e., we
     *    could freely group elements into k subsets)?" -> This becomes a much
     *    harder partition/bin-covering style problem (related to multiprocessor
     *    scheduling / bin packing), generally NP-hard in general, though the same
     *    binary-search-on-answer + greedy-or-DP feasibility idea can still serve as
     *    a heuristic or exact solution for small n via bitmask DP.
     */

    /*
     * =================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * =================================================================================
     *
     * 1. Binary search bounds off-by-one: using `hi = candidateThreshold - 1` when
     *    the threshold IS feasible (instead of `hi = candidateThreshold`), which can
     *    incorrectly exclude the true minimal answer from the search space. The
     *    correct move on "feasible" is to narrow hi down TO candidateThreshold
     *    (inclusive), never past it.
     *
     * 2. Forgetting the `value > threshold` / starting lowerBound at max(nums)
     *    safeguard: without it, if a single element exceeds a candidate threshold
     *    mid-search, the greedy check can silently miscount pieces instead of
     *    immediately recognizing infeasibility — a classic silent failure that
     *    passes most random tests but fails on inputs with one very large element.
     *
     * 3. Using `int` for prefix sums / thresholds and silently overflowing:
     *    sum(nums) can be up to 10^3 * 10^4 = 10^7 here, which technically still
     *    fits in an int, but the habit of accumulating in `long` matters a lot once
     *    constraints are tightened (e.g., LeetCode variants with nums[i] up to 10^9)
     *    — defaulting to `long` for all accumulations avoids having to remember to
     *    special-case this later.
     *
     * 4. Miscounting pieces in the greedy check: initializing `piecesUsedSoFar = 0`
     *    instead of `1` (forgetting that the very first element always starts an
     *    open piece before any comparison happens), which throws off the piece
     *    count by exactly one and causes off-by-one feasibility errors that are
     *    easy to miss in casual testing but show up immediately on edge cases like
     *    k == nums.length.
     */

    /*
     * =================================================================================
     * TEST HARNESS
     * =================================================================================
     * Cross-validates all three approaches against each other on the examples from
     * Section 3, plus a few additional randomized-style spot checks.
     */
    public static void main(String[] args) {
        List<TestCase> testCases = new ArrayList<>();
        testCases.add(new TestCase(new int[]{7, 2, 5, 10, 8}, 2, 18));   // Section 3, Example 1
        testCases.add(new TestCase(new int[]{1, 4, 4}, 3, 4));            // Section 3, Example 2
        testCases.add(new TestCase(new int[]{0, 0, 0, 10, 0}, 1, 10));    // Section 3, Example 3
        testCases.add(new TestCase(new int[]{1, 2, 3, 4, 5}, 1, 15));     // k = 1 sanity check
        testCases.add(new TestCase(new int[]{1, 2, 3, 4, 5}, 5, 5));      // k = n sanity check
        testCases.add(new TestCase(new int[]{2, 3, 1, 1, 1, 1}, 3, 4));   // extra check

        int passed = 0;
        for (TestCase testCase : testCases) {
            int bruteForceResult = splitArrayBruteForce(testCase.nums, testCase.k);
            int dpResult = splitArrayDP(testCase.nums, testCase.k);
            int binarySearchResult = splitArrayBinarySearch(testCase.nums, testCase.k);
            int optimalResult = splitArrayOptimal(testCase.nums, testCase.k);

            boolean allAgree = bruteForceResult == testCase.expected
                    && dpResult == testCase.expected
                    && binarySearchResult == testCase.expected
                    && optimalResult == testCase.expected;

            System.out.printf(
                    "nums=%-28s k=%d | expected=%d | bruteForce=%d dp=%d binarySearch=%d optimal=%d | %s%n",
                    Arrays.toString(testCase.nums), testCase.k, testCase.expected,
                    bruteForceResult, dpResult, binarySearchResult, optimalResult,
                    allAgree ? "PASS" : "FAIL"
            );
            if (allAgree) {
                passed++;
            }
        }
        System.out.printf("%n%d / %d test cases passed across all four implementations.%n",
                passed, testCases.size());
    }

    // Simple record-style holder for a test case: input array, k, and expected answer.
    private static final class TestCase {
        final int[] nums;
        final int k;
        final int expected;

        TestCase(int[] nums, int k, int expected) {
            this.nums = nums;
            this.k = k;
            this.expected = expected;
        }
    }
}

/**
 * Problem Statement:
 * Given an integer array `nums` and an integer `k`, split `nums` into `k` non-empty 
 * contiguous subarrays such that the largest sum among these subarrays is minimized.
 * Return this minimized largest sum.
 * 
 * Constraints:
 * - 1 <= nums.length <= 10^3
 * - 0 <= nums[i] <= 10^4
 * - 1 <= k <= nums.length
 */
class SplitArrayLargestSum2 {

    /**
     * Helper Method: Greedily checks if it is possible to split the array into at most 
     * `k` subarrays such that no subarray sum exceeds `maxSumAllowed`.
     */
    private static boolean canSplit(int[] nums, int k, int maxSumAllowed) {
        int subarrayCount = 1;
        int currentSum = 0;

        for (int num : nums) {
            // If adding this number exceeds the allowed max, we must start a new subarray
            if (currentSum + num > maxSumAllowed) {
                subarrayCount++;
                currentSum = num;
                
                // If we need more than 'k' subarrays, this maxSumAllowed is too small
                if (subarrayCount > k) {
                    return false;
                }
            } else {
                currentSum += num;
            }
        }
        return true;
    }

    /**
     * SOLUTION 1: Iterative Binary Search on the Answer Space (Optimal)
     * 
     * Time Complexity: O(N * log(Sum - Max)) where N is length of nums.
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION & LOGIC:
     * We don't binary search indices; we binary search the ACTUAL TARGET SUM.
     * What is the absolute smallest possible max sum? 
     * - If k = nums.length, every element is its own subarray. The max sum is max(nums).
     * What is the absolute largest possible max sum?
     * - If k = 1, there is only 1 subarray. The max sum is sum(nums).
     * 
     * nums = [7, 2, 5, 10, 8], k = 2
     * Search Space Bounds:
     * low = max(7, 2, 5, 10, 8) = 10
     * high = sum(7, 2, 5, 10, 8) = 32
     * 
     * Iteration 1:
     * L = 10, H = 32. mid = 21.
     * Can we split into <= 2 subarrays where no sum > 21?
     * [7, 2, 5] (sum 14) | [10, 8] (sum 18). Yes, it took exactly 2 pieces!
     * 21 is valid. Save result = 21. Can we do even smaller? H = 20.
     * 
     * Iteration 2:
     * L = 10, H = 20. mid = 15.
     * Can we split into <= 2 subarrays where no sum > 15?
     * [7, 2, 5] (sum 14) | [10] (sum 10) | [8] (sum 8). Took 3 pieces.
     * 3 > 2 pieces. So 15 is too small. L = 16.
     * 
     * Ultimately converges on result = 18.
     */
    public static int splitArrayIterativeBS(int[] nums, int k) {
        // Range lower bound: max element. Range upper bound: sum of all elements.
        int low = Arrays.stream(nums).max().orElse(0);
        int high = Arrays.stream(nums).sum();
        
        int result = high; // Explicit result variable initialized to the max possible valid answer

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (canSplit(nums, k, mid)) {
                // If mid is a valid max sum, store it and try to find an even smaller one
                result = mid;
                high = mid - 1;
            } else {
                // mid is too small (requires too many splits), search higher
                low = mid + 1;
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search on Answer Space
     * 
     * Time Complexity: O(N * log(Sum - Max))
     * Space Complexity: O(log(Sum - Max)) - Call stack overhead.
     * 
     * EXPLANATION:
     * A purely functional translation of the optimal iterative logic. 
     * Uses `currentResult` to explicitly track the minimum valid max sum across recursive calls.
     */
    public static int splitArrayRecursiveBSWrapper(int[] nums, int k) {
        int max = Arrays.stream(nums).max().orElse(0);
        int sum = Arrays.stream(nums).sum();
        return splitArrayRecursiveBS(nums, k, max, sum, sum);
    }

    private static int splitArrayRecursiveBS(int[] nums, int k, int low, int high, int currentResult) {
        int result = currentResult; // Explicitly track result

        if (low > high) {
            return result; // Base case: binary search range exhausted
        }

        int mid = low + (high - low) / 2;

        if (canSplit(nums, k, mid)) {
            // Valid max sum, record it and try for a smaller one
            result = splitArrayRecursiveBS(nums, k, low, mid - 1, mid);
        } else {
            // Invalid max sum, must increase it
            result = splitArrayRecursiveBS(nums, k, mid + 1, high, result);
        }

        return result;
    }

    /**
     * SOLUTION 3: Top-Down Dynamic Programming (Memoization)
     * 
     * Time Complexity: O(N^2 * K)
     * Space Complexity: O(N * K) for memoization table
     * 
     * EXPLANATION:
     * We define a state dp(i, k) representing the answer for the subarray `nums[i...n-1]` 
     * split into `k` parts. We try placing a split at every possible index `j` from `i` 
     * to `n - k`, keeping track of the sum of the left part and recursively solving for the right.
     */
    public static int splitArrayTopDownDP(int[] nums, int k) {
        int n = nums.length;
        int[][] memo = new int[n][k + 1];
        for (int[] row : memo) {
            Arrays.fill(row, -1);
        }
        
        // Precompute prefix sums for O(1) subarray sum queries
        int[] prefixSum = new int[n + 1];
        for (int i = 0; i < n; i++) {
            prefixSum[i + 1] = prefixSum[i] + nums[i];
        }

        return dfs(0, k, nums, prefixSum, memo);
    }

    private static int dfs(int startIndex, int splitsRemaining, int[] nums, int[] prefixSum, int[][] memo) {
        int n = nums.length;
        
        // Base case: 1 split remaining means taking the rest of the array
        if (splitsRemaining == 1) {
            return prefixSum[n] - prefixSum[startIndex];
        }

        if (memo[startIndex][splitsRemaining] != -1) {
            return memo[startIndex][splitsRemaining];
        }

        int minLargestSplitSum = Integer.MAX_VALUE;

        // Try every possible split position for the current chunk
        for (int i = startIndex; i <= n - splitsRemaining; i++) {
            int firstSplitSum = prefixSum[i + 1] - prefixSum[startIndex];
            
            // The largest sum in this configuration is the max of the first chunk 
            // and the optimal configuration of the remaining chunks
            int largestSplitSum = Math.max(firstSplitSum, dfs(i + 1, splitsRemaining - 1, nums, prefixSum, memo));
            
            // We want to minimize the largest split sum across all possible configurations
            minLargestSplitSum = Math.min(minLargestSplitSum, largestSplitSum);
            
            // Pruning optimization: if the first split sum exceeds the min largest seen so far, 
            // continuing will only increase the first chunk sum, so we can stop searching further.
            if (firstSplitSum >= minLargestSplitSum) {
                break;
            }
        }

        return memo[startIndex][splitsRemaining] = minLargestSplitSum;
    }

    /**
     * SOLUTION 4: Bottom-Up Dynamic Programming
     * 
     * Time Complexity: O(N^2 * K)
     * Space Complexity: O(N * K)
     * 
     * EXPLANATION:
     * dp[i][j] = the minimized largest sum splitting `nums[0...i-1]` into `j` parts.
     * We populate the table systematically.
     */
    public static int splitArrayBottomUpDP(int[] nums, int k) {
        int n = nums.length;
        int[][] dp = new int[n + 1][k + 1];
        
        for (int[] row : dp) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }
        dp[0][0] = 0; // 0 elements split into 0 parts has sum 0

        int[] prefixSum = new int[n + 1];
        for (int i = 0; i < n; i++) {
            prefixSum[i + 1] = prefixSum[i] + nums[i];
        }

        for (int i = 1; i <= n; i++) { // For every array length up to n
            for (int j = 1; j <= k; j++) { // For every possible number of splits
                for (int p = 0; p < i; p++) { // For every possible split point p before i
                    int currentSubarraySum = prefixSum[i] - prefixSum[p];
                    
                    // The largest sum forming j parts ending at i is the max of:
                    // 1. the best sum forming j-1 parts up to p
                    // 2. the sum of the final piece from p to i
                    int maxInThisConfiguration = Math.max(dp[p][j - 1], currentSubarraySum);
                    
                    // Keep the minimum across all possible previous split points
                    dp[i][j] = Math.min(dp[i][j], maxInThisConfiguration);
                }
            }
        }

        return dp[n][k];
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record to structure the test cases elegantly.
     */
    public record TestCase(int[] nums, int k, int expected) {}

    public static void main(String[] args) {
        // Defined Test Cases based on boundaries, standard examples, and edge cases
        TestCase[] testCases = {
            new TestCase(new int[]{7, 2, 5, 10, 8}, 2, 18),        // Standard Example 1
            new TestCase(new int[]{1, 2, 3, 4, 5}, 2, 9),          // Standard Example 2
            new TestCase(new int[]{1, 4, 4}, 3, 4),                // Splits == Array Length
            new TestCase(new int[]{1, 4, 4}, 1, 9),                // Splits == 1
            new TestCase(new int[]{10, 20, 30, 40}, 2, 70),        // Growing array
            new TestCase(new int[]{0, 0, 0, 0}, 2, 0),             // Zeroes array
            new TestCase(IntStream.rangeClosed(1, 100).toArray(), 10, 515) // Larger stress test
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int resIterativeBS = splitArrayIterativeBS(tc.nums(), tc.k());
            int resRecursiveBS = splitArrayRecursiveBSWrapper(tc.nums(), tc.k());
            int resTopDownDP   = splitArrayTopDownDP(tc.nums(), tc.k());
            
            // Limit bottom-up DP execution on massive arrays for test snappiness, 
            // though for N=100 it runs instantly, we'll keep it active.
            int resBottomUpDP  = splitArrayBottomUpDP(tc.nums(), tc.k());

            boolean passed = (resIterativeBS == tc.expected()) &&
                             (resRecursiveBS == tc.expected()) &&
                             (resTopDownDP == tc.expected()) &&
                             (resBottomUpDP == tc.expected());

            // Neat printing logic
            String arrStr = Arrays.toString(tc.nums());
            if (arrStr.length() > 25) arrStr = arrStr.substring(0, 22) + "...]";

            System.out.printf("Test %d | k: %-2d | Nums: %-25s -> Expected: %-4d | Passed: %b%n",
                    i + 1, tc.k(), arrStr, tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] IterBS: %d, RecBS: %d, TopDP: %d, BotDP: %d%n",
                        resIterativeBS, resRecursiveBS, resTopDownDP, resBottomUpDP);
            }
        }
    }
}

class SplitArrayLargestSum3 {

    public static void main(String[] args) {
        int[] nums = {7, 2, 5, 10, 8};
        int k = 2;

        System.out.println(splitArray(nums, k)); // Expected: 18
    }

    public static int splitArray(int[] nums, int k) {

        // ------------------------------------------------------------
        // Step 1: Define Search Space
        // ------------------------------------------------------------
        int low = Arrays.stream(nums).max().getAsInt(); // minimum possible answer
        int high = Arrays.stream(nums).sum();           // maximum possible answer

        int answer = high; // store best valid answer

        // ------------------------------------------------------------
        // Step 2: Binary Search on Answer
        // ------------------------------------------------------------
        while (low <= high) {
            int mid = low + (high - low) / 2;

            // --------------------------------------------------------
            // Check if this mid is VALID
            // --------------------------------------------------------
            if (canSplit(nums, k, mid)) {
                answer = mid;     // store better (smaller) answer
                high = mid - 1;   // try to minimize further
            } else {
                low = mid + 1;    // increase allowed sum
            }
        }

        return answer;
    }

    /**
     * Check function:
     * Can we split array into <= k subarrays
     * such that each subarray sum <= maxAllowedSum ?
     */
    private static boolean canSplit(int[] nums, int k, int maxAllowedSum) {

        int subarrays = 1; // we start with 1 subarray
        int currentSum = 0;

        for (int num : nums) {

            // If adding this element exceeds allowed sum,
            // we must create a new subarray
            if (currentSum + num > maxAllowedSum) {
                subarrays++;
                currentSum = num;

                // If we need more than k subarrays → invalid
                if (subarrays > k) return false;
            } else {
                currentSum += num;
            }
        }

        return true; // valid split
    }
}
