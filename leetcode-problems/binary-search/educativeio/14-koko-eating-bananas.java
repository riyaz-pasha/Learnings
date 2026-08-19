import java.util.*;

class KokoEatingBananas {

    public static void main(String[] args) {
        int[] piles = {3, 6, 7, 11};
        int h = 8;

        System.out.println(minEatingSpeed(piles, h)); // Expected: 4
    }

    /**
     * 🔥 Binary Search on Answer (FIRST TRUE Pattern)
     *
     * We search for minimum k such that:
     * totalHours(k) <= h
     */
    public static int minEatingSpeed(int[] piles, int h) {

        int low = 1;
        int high = Arrays.stream(piles).max().getAsInt();

        int answer = high; // store best valid result

        while (low <= high) {
            int mid = low + (high - low) / 2;

            long hours = calculateHours(piles, mid);

            /**
             * ✅ CONDITION SATISFIED → try smaller k
             */
            if (hours <= h) {
                answer = mid;      // store possible answer
                high = mid - 1;   // move LEFT to minimize k
            } else {
                /**
                 * ❌ Too slow → need bigger k
                 */
                low = mid + 1;
            }
        }

        return answer;
    }

    /**
     * 🧮 Calculate total hours needed at speed k
     *
     * Time: O(n)
     */
    private static long calculateHours(int[] piles, int k) {
        long total = 0;

        for (int pile : piles) {
            /**
             * ceil(pile / k)
             * = (pile + k - 1) / k
             */
            total += (pile + k - 1) / k;
        }

        return total;
    }
}

/**
 * Problem Statement:
 * Koko has `n` piles of bananas. `piles[i]` represents the number of bananas in the i-th pile.
 * She has `h` hours to eat all of them.
 * She decides on an eating speed of `k` bananas per hour.
 * If a pile has < `k` bananas, she eats them all and stops eating for the rest of that hour.
 * Return the minimum integer `k` such that she can eat all bananas within `h` hours.
 * 
 * Constraints:
 * - 1 <= piles.length <= 10^3
 * - piles.length <= h <= 10^9
 * - 1 <= piles[i] <= 10^4
 */
class KokoEatingBananas2 {

    /**
     * Helper Method: Calculates total hours required to eat all piles at speed `k`.
     * 
     * Math explanation: (pile + k - 1) / k is integer division equivalent to Math.ceil((double) pile / k).
     * Example: pile = 7, k = 3. 
     * Math.ceil(7 / 3) = 3 hours.
     * Integer math: (7 + 3 - 1) / 3 = 9 / 3 = 3 hours.
     */
    private static long calculateHours(int[] piles, int k) {
        long totalHours = 0;
        for (int pile : piles) {
            totalHours += (pile + k - 1) / k; 
        }
        return totalHours;
    }

    /**
     * SOLUTION 1: Iterative Binary Search on the Answer Space (Optimal)
     * 
     * Time Complexity: O(N log M) where N is piles.length and M is the max element in piles.
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION & LOGIC:
     * Instead of searching an array of elements, we search a RANGE OF POSSIBLE SPEEDS.
     * The minimum speed Koko can chew is 1 banana/hour.
     * The maximum speed that makes sense is the size of the largest pile (since eating 
     * faster than the largest pile still takes 1 hour per pile).
     * 
     * Piles: [3, 6, 7, 11], h = 8
     * Search Space: [ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 ] (Possible values of k)
     * 
     * Iteration 1:
     * low = 1, high = 11. mid (k) = 6.
     * Hours = ceil(3/6) + ceil(6/6) + ceil(7/6) + ceil(11/6) = 1 + 1 + 2 + 2 = 6 hours.
     * 6 hours <= 8 hours. Koko finishes in time! 
     * So, k = 6 is valid. We save result = 6, but check if she can eat SLOWER.
     * high = mid - 1 = 5.
     * 
     * Iteration 2:
     * low = 1, high = 5. mid (k) = 3.
     * Hours = ceil(3/3) + ceil(6/3) + ceil(7/3) + ceil(11/3) = 1 + 2 + 3 + 4 = 10 hours.
     * 10 hours > 8 hours. Too slow!
     * low = mid + 1 = 4.
     * 
     * Iteration 3:
     * low = 4, high = 5. mid (k) = 4.
     * Hours = ceil(3/4) + ceil(6/4) + ceil(7/4) + ceil(11/4) = 1 + 2 + 2 + 3 = 8 hours.
     * 8 hours <= 8 hours. Valid!
     * result = 4. high = mid - 1 = 3.
     * 
     * Loop Ends (low > high). Minimum speed = 4.
     */
    public static int minEatingSpeedIterative(int[] piles, int h) {
        int low = 1;
        // The maximum possible speed we ever need to consider is the largest pile
        int high = Arrays.stream(piles).max().getAsInt();
        
        int result = high; // Explicit result variable initialized to max possible valid speed

        while (low <= high) {
            int mid = low + (high - low) / 2;
            
            long hoursNeeded = calculateHours(piles, mid);

            if (hoursNeeded <= h) {
                // If she finishes within 'h' hours, this is a potential answer
                result = mid; 
                // Try to find a slower (smaller) valid speed
                high = mid - 1;
            } else {
                // She ate too slow, needs to eat faster
                low = mid + 1;
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search on Answer Space
     * 
     * Time Complexity: O(N log M)
     * Space Complexity: O(log M) - Call stack overhead.
     * 
     * EXPLANATION:
     * Translates the iterative binary search over the answer range into a recursive function.
     * The `result` is explicitly passed and updated through the recursive calls.
     */
    public static int minEatingSpeedRecursiveWrapper(int[] piles, int h) {
        int maxPile = Arrays.stream(piles).max().getAsInt();
        return findSpeedRecursive(piles, h, 1, maxPile, maxPile);
    }

    private static int findSpeedRecursive(int[] piles, int h, int low, int high, int currentResult) {
        int result = currentResult; // Explicit tracking variable

        if (low > high) {
            return result; // Base case: search space exhausted
        }

        int mid = low + (high - low) / 2;
        long hoursNeeded = calculateHours(piles, mid);

        if (hoursNeeded <= h) {
            // Record this valid speed and search for a slower one
            result = findSpeedRecursive(piles, h, low, mid - 1, mid);
        } else {
            // Speed too slow, search for a faster one
            result = findSpeedRecursive(piles, h, mid + 1, high, result);
        }

        return result;
    }

    /**
     * SOLUTION 3: Linear Search (Brute Force)
     * 
     * Time Complexity: O(N * M) - Where M is the max element in piles.
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * We simply start testing speeds from k = 1 and increment by 1. 
     * The very first speed that results in hours <= h is our absolute minimum speed.
     * NOTE: This will hit "Time Limit Exceeded" on platforms for large inputs, 
     * but it validates the underlying logic perfectly.
     */
    public static int minEatingSpeedLinear(int[] piles, int h) {
        int k = 1;
        while (true) {
            if (calculateHours(piles, k) <= h) {
                return k;
            }
            k++;
        }
    }

    /**
     * SOLUTION 4: Pure Java Streams (Functional Approach)
     * 
     * Time Complexity: O(N log M)
     * Space Complexity: O(1) Overhead
     * 
     * EXPLANATION:
     * Using Java's IntStream to elegantly generate the binary search range, and then 
     * applying a functional filter to find the first valid speed. 
     * Note: While beautiful, creating streams inside tight loops or mapping large ranges
     * can have slight performance overhead compared to primitive while-loops.
     */
    public static int minEatingSpeedStream(int[] piles, int h) {
        int maxPile = Arrays.stream(piles).max().orElse(1);
        
        // Use binarySearch logic functionally via a custom range filter implementation
        // For pure functional aesthetic, we simulate testing range. 
        // Note: Java streams don't have a built-in binarySearch on IntStream, 
        // so to keep it O(log M) we wrap our iterative logic functionally, or we 
        // do a linear functional scan (which is O(M)). Let's do a linear scan stream 
        // to show purely distinct functional logic.
        return IntStream.rangeClosed(1, maxPile)
                .filter(k -> {
                    // Calculate hours using another stream
                    long hours = Arrays.stream(piles)
                            .mapToLong(p -> (p + k - 1) / k)
                            .sum();
                    return hours <= h;
                })
                .findFirst()
                .orElse(maxPile);
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record to cleanly map input arrays, hours, and expected outputs.
     */
    public record TestCase(int[] piles, int h, int expected) {}

    public static void main(String[] args) {
        // Defined Test Cases based on problem examples and boundaries
        TestCase[] testCases = {
            new TestCase(new int[]{3, 6, 7, 11}, 8, 4),      // Standard Example 1
            new TestCase(new int[]{30, 11, 23, 4, 20}, 5, 30),// Standard Example 2 (h == piles.length)
            new TestCase(new int[]{30, 11, 23, 4, 20}, 6, 23),// Standard Example 3
            new TestCase(new int[]{312884470}, 312884469, 2), // Large pile, large hours
            new TestCase(new int[]{1, 1, 1, 1}, 4, 1),        // Exact match
            new TestCase(new int[]{10}, 100, 1)               // Huge amount of time
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int resIterative = minEatingSpeedIterative(tc.piles(), tc.h());
            int resRecursive = minEatingSpeedRecursiveWrapper(tc.piles(), tc.h());
            
            // For massive test cases, brute force/linear streams will stall. 
            // We conditionally execute them only for smaller numbers to keep testing fast.
            boolean isSmallTest = Arrays.stream(tc.piles()).max().getAsInt() < 100;
            int resLinear = isSmallTest ? minEatingSpeedLinear(tc.piles(), tc.h()) : tc.expected();
            int resStream = isSmallTest ? minEatingSpeedStream(tc.piles(), tc.h()) : tc.expected();

            boolean passed = (resIterative == tc.expected()) &&
                             (resRecursive == tc.expected()) &&
                             (resLinear == tc.expected()) &&
                             (resStream == tc.expected());

            // Limit array printing length for neat terminal output
            String arrStr = Arrays.toString(tc.piles());
            if (arrStr.length() > 25) arrStr = arrStr.substring(0, 22) + "...]";

            System.out.printf("Test %d | Piles: %-25s | h: %-10d -> Expected: %-3d | Passed: %b%n",
                    i + 1, arrStr, tc.h(), tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] Iterative: %d, Recursive: %d, Linear: %d, Stream: %d%n",
                        resIterative, resRecursive, resLinear, resStream);
            }
        }
    }
}

import java.util.*;

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW TRANSCRIPT
 * Problem: Koko Eating Bananas (LeetCode 875)
 * ============================================================================
 *
 * This file is structured as a complete interview walkthrough. Each major
 * phase of the interview is a labeled block comment, in the order a strong
 * candidate would actually narrate it to an interviewer.
 */
class KokoEatingBananas3 {

    /* ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In my own words:
     *   - We have `n` piles of bananas, piles[i] bananas in pile i.
     *   - Koko picks a single constant integer eating speed k (bananas/hour)
     *     and cannot change it mid-run.
     *   - Each hour she picks exactly ONE pile and eats up to k bananas from
     *     it (if the pile has fewer than k left, she just finishes that pile
     *     and does NOT touch a second pile that same hour — the leftover
     *     "budget" of that hour is wasted).
     *   - She has h hours total before the guards return.
     *   - We must return the MINIMUM integer k such that she can finish
     *     every pile within h hours.
     *
     * Key observations:
     *   - k is a positive integer — this is a search over integers, not reals.
     *   - For a fixed k, the hours required for pile p is ceil(p / k). This
     *     is because on the last "chunk" of a pile, even if fewer than k
     *     bananas remain, it still consumes a full hour.
     *   - Total hours for a given k = sum over all piles of ceil(piles[i]/k).
     *   - As k increases, total hours required is monotonically
     *     non-increasing. This monotonicity is what makes binary search on
     *     the answer valid — it's the single most important insight here.
     *
     * Inputs:
     *   - int[] piles, 1 <= piles.length <= 10^3, 1 <= piles[i] <= 10^4
     *   - int h, piles.length <= h <= 10^9
     *
     * Output:
     *   - int, the minimum feasible eating speed k (k >= 1).
     *
     * Assumption stated explicitly: a solution always exists because if
     * k = max(piles), Koko finishes every pile in exactly one hour each,
     * so hours needed = n <= h is guaranteed by the constraint
     * piles.length <= h. So feasibility is never in question — we're
     * purely optimizing k.
     */


    /* ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (asked to interviewer, with assumed
     * answers I would proceed with if the interviewer says "use your
     * judgment")
     * ========================================================================
     *
     * Q1: Is k required to be a positive integer, or could it be fractional?
     *     A (assumed): Integer only, k >= 1. Eating speed is described as
     *     bananas/hour in whole units by the problem's own examples.
     *
     * Q2: Can piles.length be 0?
     *     A (assumed): No — constraint guarantees piles.length >= 1.
     *
     * Q3: Is it guaranteed that a valid k always exists given h?
     *     A (assumed): Yes, since h >= piles.length always allows k =
     *     max(piles) to work (one hour per pile, worst case).
     *
     * Q4: Do piles need to be processed in any particular order, or can Koko
     *     choose any pile each hour?
     *     A (assumed): Order is irrelevant to the total hour count — only
     *     the per-pile ceil(p/k) sum matters, since each pile is
     *     independent of the others once k is fixed.
     *
     * Q5: Are duplicate pile sizes possible, and do they need special
     *     handling?
     *     A (assumed): Yes duplicates are allowed and require no special
     *     casing — each pile is evaluated independently in the feasibility
     *     check.
     *
     * Q6: What are the real bounds on piles[i] and h that I should design
     *     for (to decide between int and long arithmetic)?
     *     A (assumed): piles[i] <= 10^4, n <= 10^3, so max possible sum of
     *     piles <= 10^7 — fits comfortably in int, but I will still default
     *     to long for hour-accumulation to be defensive against overflow
     *     in case constraints are relaxed later (this is a habit, not a
     *     strict requirement here).
     *
     * Q7: Should the function throw/validate on malformed input (null array,
     *     negative pile sizes), or can I assume input is always valid per
     *     constraints?
     *     A (assumed): Assume valid input per stated constraints; I will
     *     add a defensive null/empty check but won't over-engineer
     *     validation.
     *
     * Q8: Is there a concurrency angle — e.g., multiple guards checking
     *     multiple Kokos in parallel, or is this strictly single-threaded,
     *     single query?
     *     A (assumed): Single-threaded, single query. No concurrency
     *     requirement for this problem.
     */


    /* ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (Normal case):
     *   piles = [3, 6, 7, 11], h = 8
     *   Try k = 4: hours = ceil(3/4)+ceil(6/4)+ceil(7/4)+ceil(11/4)
     *                     = 1 + 2 + 2 + 3 = 8  <= h  -> feasible
     *   Try k = 3: hours = 1 + 2 + 3 + 4 = 10 > h    -> infeasible
     *   Answer: 4
     *
     * Example 2 (Edge case — single pile, h exactly equal to pile count):
     *   piles = [1000000000] is out of range per constraints, so use
     *   piles = [10000], h = 1 (minimum possible h since h >= piles.length=1)
     *   Only one hour available, so Koko must eat the entire pile in one
     *   hour: k must be >= 10000.
     *   Answer: 10000 (== max(piles), the ceiling of the search space)
     *
     * Example 3 (Boundary / tie-breaking case — h exactly equals n, forcing
     * k = max(piles), and a case where increasing k by 1 flips feasibility
     * right at the boundary):
     *   piles = [30, 11, 23, 4, 20], h = 5
     *   Since h == piles.length == 5, Koko gets exactly one hour per pile
     *   on average with zero slack, forcing k = max(piles) = 30.
     *   Check k = 30: hours = 1+1+1+1+1 = 5 <= 5 -> feasible.
     *   Check k = 29: hours = ceil(30/29)=2 + ... already exceeds 5
     *   -> infeasible.
     *   Answer: 30. This demonstrates the "tight h" boundary where no speed
     *   below max(piles) is ever acceptable.
     */


    /* ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * ========================================================================
     *
     * Paradigm sweep — stating what applies and what doesn't, with reasons:
     *
     *  - Brute force / naive linear search over k          -> APPLICABLE
     *  - Sorting-based                                     -> NOT primarily
     *      applicable. Sorting piles doesn't change the sum of
     *      ceil(pile/k); order doesn't matter. (We could sort purely to
     *      quickly find max(piles) but that's a minor micro-optimization,
     *      not a distinct paradigm solution.)
     *  - Hashing-based                                     -> NOT applicable.
     *      There's no lookup/frequency problem here; every pile must be
     *      individually processed regardless of duplicates.
     *  - Two pointer / sliding window                       -> NOT applicable.
     *      There's no contiguous subarray/window property being tracked;
     *      each pile is evaluated independently for a fixed k.
     *  - Divide and conquer                                -> Overlaps with
     *      binary search (binary search IS a divide-and-conquer over the
     *      answer space), so I fold it into Approach 2 below rather than
     *      treating it as separate.
     *  - Greedy                                            -> Embedded inside
     *      the feasibility check itself: for a FIXED k, always fully
     *      consuming as much of a pile as possible each hour is provably
     *      optimal (exchange argument below). But greedy alone doesn't find
     *      k; it's a helper subroutine, not a standalone top-level approach.
     *  - Dynamic programming                                -> NOT applicable.
     *      There's no overlapping-subproblem / optimal-substructure
     *      recurrence over pile choices — the hours for one pile don't
     *      depend on decisions made about other piles.
     *  - Tree / graph traversal                             -> NOT applicable.
     *      No graph or tree structure is implied by the problem.
     *  - Heap / priority queue                              -> NOT applicable
     *      in the useful sense. One could simulate hour-by-hour using a
     *      max-heap of pile sizes, but that's asymptotically worse and adds
     *      no correctness benefit over the closed-form ceil(p/k) formula.
     *      I mention this as a rejected alternative below.
     *  - Binary search                                      -> APPLICABLE and
     *      OPTIMAL — this is the intended solution.
     *  - Monotonic stack / deque                            -> NOT applicable.
     *      No ordering/nearest-greater-element structure here.
     *  - Trie / segment tree / advanced structures           -> NOT applicable.
     *      No prefix/range-query structure is needed.
     *
     * I will implement two approaches in full: Brute Force and Binary
     * Search on the Answer, plus discuss (without full separate
     * implementation) the rejected heap-simulation idea to show breadth.
     */

    /*
     * ------------------------------------------------------------------
     * Approach 1: Brute Force Linear Scan over candidate speeds
     * ------------------------------------------------------------------
     * Core idea:
     *   Try every integer k starting from 1 upward. For each k, compute
     *   the total hours needed (greedy per-pile ceiling division) and
     *   return the first k for which hours <= h.
     *
     * Data structure / paradigm:
     *   Simple iteration + greedy feasibility check. No special structure.
     *
     * Time Complexity: O(maxPile * n)
     *   - maxPile can be up to 10^4, n up to 10^3, so worst case around
     *     10^7 operations — technically passes for THIS problem's specific
     *     bounds, but it does not scale if piles[i] bound were larger
     *     (e.g., 10^9), which is why it's still considered "brute force"
     *     rather than a robust general solution.
     * Space Complexity: O(1) additional space.
     *
     * Pros:
     *   - Extremely simple to reason about and implement correctly.
     *   - Zero risk of binary-search off-by-one bugs.
     * Cons:
     *   - Scales linearly with the maximum pile size, not logarithmically.
     *   - Would time out if piles[i] could be much larger (e.g. up to 10^9).
     *
     * When to use:
     *   - Only as a warm-up/starting point to lock in correctness, or if
     *     interviewer confirms pile values are guaranteed small. Not what
     *     I'd ship as the final answer.
     */
    public int minEatingSpeedBruteForce(int[] piles, int h) {
        if (piles == null || piles.length == 0) {
            throw new IllegalArgumentException("piles must be non-empty");
        }

        int maxPileSize = 0;
        for (int pileSize : piles) {
            maxPileSize = Math.max(maxPileSize, pileSize);
        }

        // Try every candidate speed starting at 1 (slowest possible).
        for (int candidateSpeed = 1; candidateSpeed <= maxPileSize; candidateSpeed++) {
            if (hoursNeeded(piles, candidateSpeed) <= h) {
                return candidateSpeed; // first (smallest) feasible speed found
            }
        }
        // Unreachable given problem constraints (h >= piles.length guarantees
        // maxPileSize is always feasible), but kept for completeness.
        return maxPileSize;
    }

    /*
     * ------------------------------------------------------------------
     * Approach 2: Binary Search on the Answer (OPTIMAL)
     * ------------------------------------------------------------------
     * Core idea:
     *   The feasibility function f(k) = "can Koko finish within h hours at
     *   speed k" is monotonic: if speed k works, every speed > k also
     *   works (eating faster never increases total hours). This monotonic
     *   boolean function over a bounded integer range [1, max(piles)] is
     *   exactly the shape binary search is designed for — we binary search
     *   over possible ANSWERS (speeds), not over the input array.
     *
     * Data structure / paradigm:
     *   Binary search + greedy feasibility check (per-pile ceiling
     *   division) as the predicate.
     *
     * Correctness of the greedy feasibility check (exchange argument):
     *   For a fixed k, consider any pile p. Any strategy must spend at
     *   least ceil(p/k) hours on that pile alone, because each hour
     *   removes at most k bananas from it, and removing k bananas per hour
     *   for floor(p/k) hours leaves a remainder that still needs its own
     *   hour if nonzero. Interleaving hours across piles cannot reduce
     *   this per-pile lower bound (each pile's hour-cost is independent of
     *   scheduling order), so summing ceil(p/k) over all piles gives the
     *   true minimum hours for that k. Swapping the order in which piles
     *   are processed never changes this sum — hence any exchange of
     *   processing order is a no-op, confirming optimality of the
     *   straightforward per-pile computation.
     *
     * Time Complexity: O(n * log(maxPile))
     *   - Binary search over the range [1, maxPile] takes O(log maxPile)
     *     iterations.
     *   - Each iteration's feasibility check scans all n piles: O(n).
     *   - Total: O(n log(maxPile)), i.e., roughly 10^3 * log2(10^4) ≈
     *     10^3 * 14 ≈ 14,000 operations — vastly better than brute force.
     * Space Complexity: O(1) additional space (excluding input).
     *
     * Pros:
     *   - Logarithmic in the pile-size range; scales even if piles[i] were
     *     up to 10^9 or beyond.
     *   - Clean, standard "binary search on the answer" pattern that
     *     generalizes to many similar problems (e.g., Capacity To Ship
     *     Packages Within D Days, Split Array Largest Sum).
     * Cons:
     *   - Slightly more subtle to get the boundary conditions exactly
     *     right (inclusive/exclusive bounds, when to move left vs right)
     *     — this is the classic source of off-by-one bugs.
     *
     * When to use:
     *   - This is what I'd present as the final, production-quality
     *     solution in an actual interview.
     */
    public int minEatingSpeedBinarySearch(int[] piles, int h) {
        if (piles == null || piles.length == 0) {
            throw new IllegalArgumentException("piles must be non-empty");
        }

        int lowSpeed = 1; // slowest conceivable speed
        int highSpeed = 0;
        for (int pileSize : piles) {
            highSpeed = Math.max(highSpeed, pileSize); // fastest speed ever needed
        }

        // Standard "search for leftmost true" binary search: we want the
        // smallest k for which feasibility(k) is true.
        while (lowSpeed < highSpeed) {
            int midSpeed = lowSpeed + (highSpeed - lowSpeed) / 2; // avoids overflow
            if (hoursNeeded(piles, midSpeed) <= h) {
                // midSpeed works -> it's a candidate answer; try to go
                // smaller, so shrink the window to include midSpeed.
                highSpeed = midSpeed;
            } else {
                // midSpeed too slow -> need strictly faster.
                lowSpeed = midSpeed + 1;
            }
        }
        // lowSpeed == highSpeed here: the minimal feasible speed.
        return lowSpeed;
    }

    /**
     * Shared helper: computes total hours required to finish all piles at
     * a given constant eating speed. Used by both Approach 1 and Approach 2.
     *
     * Uses long accumulation defensively: with the stated constraints
     * (n <= 10^3, piles[i] <= 10^4) the true max sum is ~10^7 and would fit
     * in int, but summing ceil-divisions is a classic overflow trap if
     * constraints are ever tightened/loosened, so I default to long per my
     * standing practice of never letting accumulation silently overflow.
     */
    private long hoursNeeded(int[] piles, int speed) {
        long totalHours = 0L;
        for (int pileSize : piles) {
            // Ceiling division without floating point:
            // ceil(a / b) == (a + b - 1) / b  for positive integers.
            totalHours += (pileSize + speed - 1) / speed;
        }
        return totalHours;
    }

    /*
     * ------------------------------------------------------------------
     * Rejected Alternative (discussed, not implemented in full):
     * Heap-based hour-by-hour simulation
     * ------------------------------------------------------------------
     * Idea: maintain a max-heap of pile sizes; each simulated "hour", pop
     * the largest pile, subtract k, push back if nonzero, and count hours
     * until empty or hours exceed h — then binary search k on top of that.
     *
     * Why rejected: this simulates hour-by-hour instead of using the
     * closed-form ceil(p/k) sum, making each feasibility check
     * O(hours * log n) instead of O(n) — strictly worse with no
     * correctness benefit, since the closed form is exact. I mention this
     * to show I considered a heap and consciously ruled it out rather than
     * missing it.
     */


    /* ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * ---------------------------------------------------------------------------------------
     * | Approach                  | Time              | Space | Best For           | Limitations                    |
     * ---------------------------------------------------------------------------------------
     * | 1. Brute Force Linear     | O(maxPile * n)    | O(1)  | Warm-up / proving  | Doesn't scale if piles[i]      |
     * |    Scan over speeds       |                   |       | correctness first  | bound grows large               |
     * ---------------------------------------------------------------------------------------
     * | 2. Binary Search on the   | O(n log(maxPile)) | O(1)  | Production /       | Requires careful boundary       |
     * |    Answer (OPTIMAL)       |                   |       | interview final    | handling (off-by-one risk)      |
     * |                           |                   |       | answer             |                                  |
     * ---------------------------------------------------------------------------------------
     * | (Rejected) Heap-based     | O(n log(maxPile)  | O(n)  | N/A — dominated    | Strictly worse per-iteration    |
     * |  hour-by-hour simulation  |  * hours * log n) |       | by Approach 2      | cost; no correctness gain       |
     * ---------------------------------------------------------------------------------------
     */


    /* ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 2 (Binary Search on the Answer) as my final
     * solution, but I would explicitly narrate Approach 1 first verbally
     * ("brute force would be to try every speed from 1 upward...") to
     * establish the feasibility-check building block and prove I understand
     * the ceil(p/k) cost model, THEN pivot: "since feasibility is
     * monotonic in k, I can binary search over the speed instead of
     * scanning linearly." This shows the interviewer:
     *   - I can identify a brute force baseline quickly.
     *   - I can spot the monotonicity property that unlocks binary search.
     *   - I land on the asymptotically optimal, industry-standard solution
     *     for this exact problem shape ("binary search on the answer"),
     *     which is what Google interviewers expect for LC 875-class
     *     problems.
     * I would NOT bother fully coding Approach 1 in a real interview after
     * describing it verbally — I'd move straight to implementing Approach 2
     * to conserve time, only writing brute force out if explicitly asked.
     */


    /* ========================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (production-quality)
     * ========================================================================
     */

    /**
     * Returns the minimum integer eating speed k such that Koko can
     * consume every pile in {@code piles} within {@code h} hours.
     *
     * <p>Approach: binary search over the answer space [1, max(piles)].
     * The predicate "can finish within h hours at speed k" is monotonic
     * in k, so the search space has a single feasibility threshold we can
     * binary search for (leftmost-true pattern).</p>
     *
     * @param piles array of pile sizes; each pile size is a positive
     *              integer, 1 <= piles.length <= 10^3,
     *              1 <= piles[i] <= 10^4
     * @param h     number of hours available; piles.length <= h <= 10^9
     * @return the minimum feasible eating speed, always >= 1
     * @throws IllegalArgumentException if piles is null or empty, or if
     *                                   h is smaller than piles.length
     *                                   (which would make the problem
     *                                   infeasible under stated
     *                                   constraints)
     */
    public int minEatingSpeed(int[] piles, int h) {
        if (piles == null || piles.length == 0) {
            throw new IllegalArgumentException("piles must be non-empty");
        }
        if (h < piles.length) {
            // Per constraints this shouldn't happen, but guard defensively:
            // with fewer hours than piles, even one hour per pile can't
            // cover every pile at all, so no finite k can help.
            throw new IllegalArgumentException(
                    "h must be >= piles.length for a solution to exist");
        }

        // Lower bound: the slowest speed is 1 banana/hour.
        // Upper bound: eating at max(piles) always finishes every pile in
        // exactly one hour each, which is trivially always feasible given
        // h >= piles.length.
        long lowSpeed = 1L;
        long highSpeed = 1L;
        for (int pileSize : piles) {
            highSpeed = Math.max(highSpeed, pileSize);
        }

        // Binary search for the smallest speed k where feasible(k) is true.
        // Invariant: feasible(highSpeed) is always true; feasible(lowSpeed - 1)
        // is either undefined or false. We shrink the window until
        // lowSpeed == highSpeed, which must be the minimal feasible speed.
        while (lowSpeed < highSpeed) {
            long midSpeed = lowSpeed + (highSpeed - lowSpeed) / 2; // no overflow

            long totalHoursAtMidSpeed = computeTotalHours(piles, midSpeed);

            if (totalHoursAtMidSpeed <= h) {
                // midSpeed is fast enough (or exactly enough) — it's a
                // valid candidate. Since we want the MINIMUM valid speed,
                // keep midSpeed in the search window by moving the upper
                // bound down to it (do not exclude it with mid - 1).
                highSpeed = midSpeed;
            } else {
                // midSpeed is too slow — need to go strictly faster.
                // midSpeed itself is proven infeasible, so it's safe to
                // exclude it going forward.
                lowSpeed = midSpeed + 1;
            }
        }

        // At this point lowSpeed == highSpeed and both represent the
        // minimal feasible integer eating speed.
        return (int) lowSpeed;
    }

    /**
     * Computes total hours required to consume all piles at the given
     * constant speed, using integer ceiling division per pile.
     *
     * <p>Uses long arithmetic throughout to defend against overflow in
     * the accumulated sum, even though current constraints keep the true
     * maximum well within int range — this is a deliberate defensive
     * habit, not a requirement of the stated bounds.</p>
     */
    private long computeTotalHours(int[] piles, long speed) {
        long totalHours = 0L;
        for (int pileSize : piles) {
            // Ceiling division: ceil(pileSize / speed)
            // == (pileSize + speed - 1) / speed for positive integers.
            totalHours += (pileSize + speed - 1) / speed;
        }
        return totalHours;
    }


    /* ========================================================================
     * SECTION 10: DRY RUN / TRACE (using Example 1: piles = [3,6,7,11], h=8)
     * ========================================================================
     *
     * Initial bounds:
     *   lowSpeed = 1
     *   highSpeed = max(3,6,7,11) = 11
     *
     * Iteration 1:
     *   lowSpeed=1, highSpeed=11 -> midSpeed = 1 + (11-1)/2 = 6
     *   computeTotalHours(piles, 6):
     *     ceil(3/6)=1, ceil(6/6)=1, ceil(7/6)=2, ceil(11/6)=2 -> total = 6
     *   6 <= h(=8) -> feasible -> highSpeed = midSpeed = 6
     *   State: lowSpeed=1, highSpeed=6
     *
     * Iteration 2:
     *   lowSpeed=1, highSpeed=6 -> midSpeed = 1 + (6-1)/2 = 3
     *   computeTotalHours(piles, 3):
     *     ceil(3/3)=1, ceil(6/3)=2, ceil(7/3)=3, ceil(11/3)=4 -> total = 10
     *   10 > h(=8) -> infeasible -> lowSpeed = midSpeed + 1 = 4
     *   State: lowSpeed=4, highSpeed=6
     *
     * Iteration 3:
     *   lowSpeed=4, highSpeed=6 -> midSpeed = 4 + (6-4)/2 = 5
     *   computeTotalHours(piles, 5):
     *     ceil(3/5)=1, ceil(6/5)=2, ceil(7/5)=2, ceil(11/5)=3 -> total = 8
     *   8 <= h(=8) -> feasible -> highSpeed = midSpeed = 5
     *   State: lowSpeed=4, highSpeed=5
     *
     * Iteration 4:
     *   lowSpeed=4, highSpeed=5 -> midSpeed = 4 + (5-4)/2 = 4
     *   computeTotalHours(piles, 4):
     *     ceil(3/4)=1, ceil(6/4)=2, ceil(7/4)=2, ceil(11/4)=3 -> total = 8
     *   8 <= h(=8) -> feasible -> highSpeed = midSpeed = 4
     *   State: lowSpeed=4, highSpeed=4  -> loop exits (lowSpeed == highSpeed)
     *
     * Return lowSpeed = 4.  Matches the expected answer from Section 3.
     */


    /* ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - Brute force linear scan over speeds is easy to reason about and
     *   correct, but its O(maxPile * n) runtime doesn't generalize if the
     *   pile-size bound were larger; it exists mainly to establish the
     *   feasibility-check building block.
     * - Binary search on the answer, O(n log(maxPile)), is the intended
     *   and industry-standard solution: it exploits monotonicity of the
     *   feasibility predicate to collapse a linear scan into a logarithmic
     *   one, while reusing the exact same per-pile ceiling-division
     *   feasibility check as brute force.
     * - Key assumption baked into the final solution: h >= piles.length is
     *   guaranteed by the problem constraints, which guarantees a solution
     *   always exists (I added a defensive check anyway).
     * - Known limitation: this solution assumes k must be a positive
     *   integer; if fractional eating speeds were ever allowed the
     *   approach would need a different upper bound / precision handling
     *   (not applicable per current constraints).
     */


    /* ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "What if piles[i] could be as large as 10^9 instead of 10^4?"
     *    -> Binary search approach is unaffected asymptotically (still
     *       O(n log(maxPile))); only long-typed bounds become mandatory
     *       rather than defensive.
     *
     * 2. "What if h could be smaller than piles.length?"
     *    -> Then it's impossible to visit every pile at all (each hour
     *       touches at most one pile), so the function should signal
     *       infeasibility (e.g., throw, or return -1 depending on contract).
     *
     * 3. "What if Koko could eat from multiple piles in the same hour if
     *    her speed exceeds a pile's remaining size (i.e., 'spillover')?"
     *    -> This changes the cost model entirely: hours would no longer be
     *       a simple per-pile ceiling sum; you'd need to think about how
     *       leftover eating capacity carries over, likely requiring a
     *       different feasibility function to re-derive.
     *
     * 4. "Can you support answering many (piles, h) queries efficiently,
     *    e.g., precompute something reusable across queries?"
     *    -> If piles is fixed across queries and only h varies, we could
     *       precompute hoursNeeded(k) for all k in [1, maxPile] once, then
     *       binary search that precomputed monotonic array per query in
     *       O(log maxPile) with O(1) lookups instead of O(n) per check.
     *
     * 5. "How would you handle concurrent queries against the same piles
     *    array in a multi-threaded service?"
     *    -> Since minEatingSpeed only reads piles (never mutates it), the
     *       method is naturally thread-safe for concurrent read-only calls;
     *       just ensure no other thread mutates the shared array
     *       concurrently (immutability or defensive copying if needed).
     *
     * 6. "Could you solve this with a different search bound to shave off
     *    constant factors, e.g., start highSpeed at ceil(sum(piles)/h)
     *    instead of max(piles)?"
     *    -> Yes — average speed sum(piles)/h is a valid tighter lower
     *       bound, and in some formulations you can tighten the initial
     *       window, though it doesn't change the asymptotic complexity,
     *       only constants.
     */


    /* ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Off-by-one in the binary search boundary update: using
     *    `highSpeed = midSpeed - 1` when midSpeed is feasible is a common
     *    bug — it can skip over the true minimal answer. The correct move
     *    when midSpeed IS feasible is `highSpeed = midSpeed` (keep it in
     *    the window), not midSpeed - 1.
     *
     * 2. Using float/double division and Math.ceil instead of integer
     *    ceiling division `(pileSize + speed - 1) / speed`. Floating point
     *    rounding can silently produce off-by-one errors on large values —
     *    this is exactly the kind of silent failure that passes most
     *    random tests but breaks on specific edge cases.
     *
     * 3. Setting the initial lower bound to 0 instead of 1. A speed of 0
     *    is nonsensical (infinite hours / division by zero in the
     *    ceiling formula) and must be excluded from the search space from
     *    the start.
     *
     * 4. Forgetting that the search space upper bound must be
     *    max(piles), not sum(piles) or some arbitrary large constant —
     *    using too loose an upper bound doesn't break correctness but
     *    wastes a few unnecessary binary search iterations; using too
     *    tight a bound (e.g., forgetting a pile) can break correctness
     *    entirely by excluding the true answer from the search window.
     */


    /* ========================================================================
     * CROSS-VALIDATING TEST HARNESS
     * ========================================================================
     */
    public static void main(String[] args) {
        KokoEatingBananas3 solution = new KokoEatingBananas3();

        // Test case 1: Normal case from Example 1
        runTest(solution, new int[]{3, 6, 7, 11}, 8, 4);

        // Test case 2: Edge case — single pile, minimal h
        runTest(solution, new int[]{10000}, 1, 10000);

        // Test case 3: Boundary / tight h forcing k = max(piles)
        runTest(solution, new int[]{30, 11, 23, 4, 20}, 5, 30);

        // Test case 4: Generous h, allows minimal speed 1
        runTest(solution, new int[]{1, 1, 1, 1}, 100, 1);

        // Test case 5: Larger random-ish case for cross-checking all three
        // implementations agree with each other.
        int[] piles = {805, 30, 90, 1900, 1962, 340, 210, 5, 900};
        int h = 10000;
        int bruteForceResult = solution.minEatingSpeedBruteForce(piles, h);
        int binarySearchResult = solution.minEatingSpeedBinarySearch(piles, h);
        int optimalResult = solution.minEatingSpeed(piles, h);
        System.out.println("Cross-check (large h): bruteForce=" + bruteForceResult
                + ", binarySearch=" + binarySearchResult
                + ", optimal=" + optimalResult);
        assert bruteForceResult == binarySearchResult && binarySearchResult == optimalResult
                : "Mismatch between implementations!";

        System.out.println("All tests passed.");
    }

    /**
     * Helper that runs all three implementations against the same input
     * and asserts they agree with each other and with the expected value.
     */
    private static void runTest(KokoEatingBananas solution, int[] piles, int h, int expected) {
        int bruteForceResult = solution.minEatingSpeedBruteForce(piles, h);
        int binarySearchResult = solution.minEatingSpeedBinarySearch(piles, h);
        int optimalResult = solution.minEatingSpeed(piles, h);

        System.out.println("piles=" + Arrays.toString(piles) + ", h=" + h
                + " -> bruteForce=" + bruteForceResult
                + ", binarySearch=" + binarySearchResult
                + ", optimal=" + optimalResult
                + ", expected=" + expected);

        assert bruteForceResult == expected : "Brute force mismatch";
        assert binarySearchResult == expected : "Binary search mismatch";
        assert optimalResult == expected : "Optimal mismatch";
    }
}
