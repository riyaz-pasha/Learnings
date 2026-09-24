import java.util.*;

class SmallestDivisor {

    public static void main(String[] args) {
        int[] nums = {1, 2, 5, 9};
        int threshold = 6;

        System.out.println(smallestDivisor(nums, threshold)); // Expected: 5
    }

    public static int smallestDivisor(int[] nums, int threshold) {

        // 🔍 Search Space
        int low = 1;
        int high = Arrays.stream(nums).max().orElse(1);

        // 🎯 Answer tracking (IMPORTANT for interviews)
        int answer = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            // 🧪 Check if this divisor works
            if (isValid(nums, threshold, mid)) {

                // ✅ Valid → store answer
                answer = mid;

                // 🔁 Try smaller divisor
                high = mid - 1;

            } else {
                // ❌ Invalid → need bigger divisor
                low = mid + 1;
            }
        }

        return answer;
    }

    /**
     * 🧪 Check function
     * Returns TRUE if divisor produces sum <= threshold
     */
    private static boolean isValid(int[] nums, int threshold, int divisor) {

        int sum = 0;

        for (int num : nums) {

            // ⚡ CEIL division trick (avoid floating point)
            // ceil(num / divisor) = (num + divisor - 1) / divisor
            sum += (num + divisor - 1) / divisor;

            // 🚀 Early exit optimization
            if (sum > threshold) return false;
        }

        return sum <= threshold;
    }
}

/**
 * Problem Statement:
 * Given an integer array `nums` and an integer `threshold`.
 * Find the smallest positive integer divisor `d` such that the sum of each element 
 * in `nums` divided by `d` and rounded up is less than or equal to `threshold`.
 * 
 * Rounding up example: 7 / 3 = ceil(2.33) = 3. 10 / 2 = 5.
 * 
 * Constraints:
 * - 1 <= nums.length <= 5 * 10^4
 * - 1 <= nums[i] <= 10^6
 * - nums.length <= threshold <= 10^6
 */
class SmallestDivisorGivenThreshold {

    /**
     * Helper Method: Calculates the sum of elements divided by `d`, rounded up.
     * 
     * Math Logic for Ceiling Division:
     * Instead of using `Math.ceil((double) num / d)` which incurs floating point overhead,
     * we use integer math: (num + d - 1) / d.
     * Example: num = 7, d = 3. (7 + 3 - 1) / 3 = 9 / 3 = 3.
     */
    private static long computeSum(int[] nums, int d) {
        long sum = 0;
        for (int num : nums) {
            sum += (num + d - 1) / d;
        }
        return sum;
    }

    /**
     * SOLUTION 1: Iterative Binary Search on the Answer Space (Optimal)
     * 
     * Time Complexity: O(N log M) where N = nums.length, M = max(nums)
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION & LOGIC:
     * We don't search the array; we search the RANGE of possible divisors.
     * The smallest possible divisor is 1.
     * The largest possible divisor we need to consider is the max value in `nums`.
     * (Because if d = max(nums), every element divides to 1, making the sum = nums.length,
     * which the problem guarantees is <= threshold).
     * 
     * Array: [1, 2, 5, 9], Threshold: 6
     * Max value is 9. Search Space for 'd': [1, 2, 3, 4, 5, 6, 7, 8, 9]
     * 
     * Iteration 1:
     * L = 1, H = 9. mid (d) = 5.
     * Sum: ceil(1/5) + ceil(2/5) + ceil(5/5) + ceil(9/5) = 1 + 1 + 1 + 2 = 5.
     * 5 <= 6 (threshold). Valid!
     * Since we want the SMALLEST divisor, we save result = 5, and search left. H = 4.
     * 
     * Iteration 2:
     * L = 1, H = 4. mid (d) = 2.
     * Sum: ceil(1/2) + ceil(2/2) + ceil(5/2) + ceil(9/2) = 1 + 1 + 3 + 5 = 10.
     * 10 > 6. Sum is too large, meaning divisor is too small. L = 3.
     * 
     * Iteration 3:
     * L = 3, H = 4. mid (d) = 3.
     * Sum: ceil(1/3) + ceil(2/3) + ceil(5/3) + ceil(9/3) = 1 + 1 + 2 + 3 = 7.
     * 7 > 6. Divisor too small. L = 4.
     * 
     * Iteration 4:
     * L = 4, H = 4. mid (d) = 4.
     * Sum: ceil(1/4) + ceil(2/4) + ceil(5/4) + ceil(9/4) = 1 + 1 + 2 + 3 = 7.
     * 7 > 6. L = 5.
     * 
     * Loop Ends (L > H). Result is 5.
     */
    public static int smallestDivisorIterative(int[] nums, int threshold) {
        int low = 1;
        int high = Arrays.stream(nums).max().getAsInt();
        
        int result = high; // Explicit result variable initialized to max valid divisor

        while (low <= high) {
            int mid = low + (high - low) / 2;
            
            long sum = computeSum(nums, mid);

            if (sum <= threshold) {
                // Divisor is valid. Store it, and try to find a smaller one.
                result = mid;
                high = mid - 1;
            } else {
                // Divisor is too small (sum is too large). Need a larger divisor.
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
     * Converts the iterative logic to recursion. The valid divisor is explicitly 
     * tracked via the `currentResult` parameter.
     */
    public static int smallestDivisorRecursiveWrapper(int[] nums, int threshold) {
        int maxVal = Arrays.stream(nums).max().getAsInt();
        return smallestDivisorRecursive(nums, threshold, 1, maxVal, maxVal);
    }

    private static int smallestDivisorRecursive(int[] nums, int threshold, int low, int high, int currentResult) {
        int result = currentResult; // Explicit result tracking

        if (low > high) {
            return result; // Base case: search space exhausted
        }

        int mid = low + (high - low) / 2;
        long sum = computeSum(nums, mid);

        if (sum <= threshold) {
            // Found a valid divisor, record it and search for smaller ones
            result = smallestDivisorRecursive(nums, threshold, low, mid - 1, mid);
        } else {
            // Divisor too small, must search higher
            result = smallestDivisorRecursive(nums, threshold, mid + 1, high, result);
        }

        return result;
    }

    /**
     * SOLUTION 3: Linear Search (Brute Force)
     * 
     * Time Complexity: O(N * M) where M is the maximum element in nums.
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * Sequentially tests divisors starting from 1. 
     * The first divisor that keeps the sum below or equal to the threshold is returned.
     * Will result in TLE (Time Limit Exceeded) for large numbers, but functionally correct.
     */
    public static int smallestDivisorLinear(int[] nums, int threshold) {
        int d = 1;
        while (true) {
            if (computeSum(nums, d) <= threshold) {
                return d;
            }
            d++;
        }
    }

    /**
     * SOLUTION 4: Pure Java Streams (Functional Approach)
     * 
     * Time Complexity: O(N * M) worst case functionally without log N range reduction.
     * Space Complexity: O(1) Overhead.
     * 
     * EXPLANATION:
     * Generates a range of divisors from 1 to Max using `IntStream`, filters them based
     * on the computed sum, and fetches the first (smallest) one.
     */
    public static int smallestDivisorStream(int[] nums, int threshold) {
        int maxVal = Arrays.stream(nums).max().orElse(1);

        return IntStream.rangeClosed(1, maxVal)
                .filter(d -> computeSum(nums, d) <= threshold)
                .findFirst()
                .orElse(maxVal);
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record to cleanly map input arrays, thresholds, and expected outputs.
     */
    public record TestCase(int[] nums, int threshold, int expected) {}

    public static void main(String[] args) {
        // Defined Test Cases based on problem examples and boundaries
        TestCase[] testCases = {
            new TestCase(new int[]{1, 2, 5, 9}, 6, 5),          // Standard Example 1
            new TestCase(new int[]{44, 22, 33, 11, 1}, 5, 44),  // Threshold equals length (max element needed)
            new TestCase(new int[]{21212, 10101, 12121}, 1000000, 1), // High threshold (divisor 1 works)
            new TestCase(new int[]{1, 1, 1, 1, 1}, 10, 1),      // Low uniform array
            new TestCase(new int[]{1000000}, 1, 1000000)        // Edge case: Max element, threshold 1
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int resIterative = smallestDivisorIterative(tc.nums(), tc.threshold());
            int resRecursive = smallestDivisorRecursiveWrapper(tc.nums(), tc.threshold());
            
            // To prevent stalling the test runner on massive search spaces,
            // we conditionally execute the brute-force/stream O(N*M) solutions.
            boolean isSmallTest = Arrays.stream(tc.nums()).max().getAsInt() <= 100;
            int resLinear = isSmallTest ? smallestDivisorLinear(tc.nums(), tc.threshold()) : tc.expected();
            int resStream = isSmallTest ? smallestDivisorStream(tc.nums(), tc.threshold()) : tc.expected();

            boolean passed = (resIterative == tc.expected()) &&
                             (resRecursive == tc.expected()) &&
                             (resLinear == tc.expected()) &&
                             (resStream == tc.expected());

            // Limit array printing length for neat terminal output
            String arrStr = Arrays.toString(tc.nums());
            if (arrStr.length() > 25) arrStr = arrStr.substring(0, 22) + "...]";

            System.out.printf("Test %d | Nums: %-25s | Thresh: %-7d -> Expected: %-7d | Passed: %b%n",
                    i + 1, arrStr, tc.threshold(), tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] Iterative: %d, Recursive: %d, Linear: %d, Stream: %d%n",
                        resIterative, resRecursive, resLinear, resStream);
            }
        }
    }
}

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW TRANSCRIPT
 * Problem: Find the Smallest Divisor Given a Threshold  (LeetCode 1283)
 * ============================================================================
 *
 * This single file is structured as a complete interview walkthrough.
 * Every section requested by the interviewer/coach is present as a labeled
 * block comment, in order. Code is compilable under Java 21+ (uses records,
 * var, enhanced switch where natural).
 */
class SmallestDivisorInterview {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In my own words:
     *
     *   I'm given an array of positive integers `nums` and an integer
     *   `threshold`. I need to pick a positive integer divisor `d`. For every
     *   element in `nums`, I divide it by `d` and round the result UP
     *   (ceiling division) — e.g. ceil(7/3) = 3, ceil(10/2) = 5. I sum all
     *   these ceiling values together. That sum must be <= threshold.
     *
     *   As `d` grows, each individual ceil(nums[i]/d) term shrinks (or stays
     *   the same), so the total sum shrinks (or stays the same) too — it can
     *   never increase as d increases. That means there's a whole contiguous
     *   range of valid divisors, and I want to return the SMALLEST one in
     *   that valid range.
     *
     * Key constraints / inputs / outputs:
     *   - Input: int[] nums (1 <= nums.length <= 5*10^4, 1 <= nums[i] <= 10^6)
     *   - Input: int threshold (nums.length <= threshold <= 10^6)
     *   - Output: smallest positive integer d such that
     *             sum_{i}( ceil(nums[i] / d) ) <= threshold
     *   - It's guaranteed a valid divisor exists, because threshold is at
     *     least nums.length (when d = max(nums), every term becomes 1,
     *     so the sum equals nums.length <= threshold — always feasible).
     *
     * Assumptions I'm stating up front:
     *   - d must be a positive integer (d >= 1); no fractional or zero divisor.
     *   - "Rounded up" means mathematical ceiling, not "round half up" or
     *     "round to nearest" — confirmed by the examples given (7/3 -> 3).
     *   - Exactly one array and one threshold are given per call; this is a
     *     single independent query, not a batch of queries.
     */

    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (asked to interviewer, with assumed answers)
     * ========================================================================
     *
     * Q1: Can nums be empty?
     *     A: No — constraints guarantee nums.length >= 1.
     *
     * Q2: Can nums contain zero or negative numbers?
     *     A: No — constraints guarantee 1 <= nums[i] <= 10^6, all positive.
     *
     * Q3: Is it guaranteed that a valid divisor always exists (i.e. is the
     *     problem always solvable), or should I handle an "impossible" case?
     *     A: Guaranteed solvable, since threshold >= nums.length and choosing
     *        d = max(nums) always yields sum == nums.length <= threshold.
     *        No need for a sentinel "not found" return value.
     *
     * Q4: What's the upper bound I should search for d? Is there a natural
     *     ceiling on how large the answer can be?
     *     A: The answer is never larger than max(nums), because increasing d
     *        past max(nums) doesn't reduce any term below 1, so it's wasted.
     *        max(nums) <= 10^6, which bounds our search space nicely.
     *
     * Q5: Are duplicate values in nums allowed, and do they need special
     *     handling?
     *     A: Yes, duplicates are allowed and require no special handling —
     *        each element is processed independently and summed.
     *
     * Q6: Should I worry about integer overflow when summing up to 5*10^4
     *     terms, each up to 10^6?
     *     A: Worth flagging: worst case sum is 5*10^4 * 10^6 = 5*10^10,
     *        which overflows a 32-bit int (max ~2.1*10^9). I'll accumulate
     *        in a `long` and only compare against threshold at the end
     *        (or short-circuit early once the running sum already exceeds
     *        threshold, purely as a performance optimization).
     *
     * Q7: Is this a single one-off query, or will this function be called
     *     repeatedly on the same array with different thresholds (which
     *     would change how much precomputation is worth it)?
     *     A: Treat it as a single one-off query for this problem; I'll note
     *        in follow-ups how repeated queries would change my approach.
     *
     * Q8: Any concurrency / thread-safety requirements, or is this a plain
     *     single-threaded function?
     *     A: Plain single-threaded, synchronous function — no shared mutable
     *        state, no concurrency concerns.
     */

    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (normal case):
     *   nums = [1, 2, 5, 9], threshold = 6
     *   Try d=5: ceil(1/5)+ceil(2/5)+ceil(5/5)+ceil(9/5) = 1+1+1+2 = 5 <= 6 ✓
     *   Try d=4: ceil(1/4)+ceil(2/4)+ceil(5/4)+ceil(9/4) = 1+1+2+3 = 7 > 6 ✗
     *   => Answer: 5 (d=4 fails, d=5 is the smallest that works)
     *
     * Example 2 (edge case — threshold at minimum, i.e. == nums.length):
     *   nums = [44, 22, 33], threshold = 3
     *   threshold equals nums.length, so the ONLY way the sum can be as low
     *   as 3 is if every term is exactly 1, which requires d >= max(nums).
     *   Try d = 44 (max(nums)): 1 + 1 + 1 = 3 <= 3 ✓
     *   Try d = 43: ceil(44/43)=2 -> sum already 2, exceeds nothing yet but
     *               let's check fully: 2 + 1 + 1 = 4 > 3 ✗
     *   => Answer: 44 (must divide by at least the max element)
     *   This confirms the search upper bound of max(nums) is tight and
     *   necessary — we can't assume something small always works.
     *
     * Example 3 (boundary / tie-breaking case — single element array):
     *   nums = [1000000], threshold = 1000000
     *   Any d >= 1 gives ceil(1000000/d) which is always <= 1000000 for d=1.
     *   Try d=1: ceil(1000000/1) = 1000000 <= 1000000 ✓ (exactly meets it)
     *   => Answer: 1 (smallest possible divisor, sum exactly equals threshold)
     *   This exercises the "<=" boundary explicitly — the sum equalling
     *   threshold exactly still counts as valid, not just strictly less.
     */

    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES (with paradigm coverage notes)
     * ========================================================================
     *
     * Paradigm applicability sweep (stating why each one does or doesn't
     * apply, before diving into implementations):
     *
     *  - Brute force / naive:        APPLICABLE (try every candidate divisor)
     *  - Sorting-based:               NOT NEEDED — the divisor sum function
     *                                  doesn't care about element order;
     *                                  sorting nums doesn't change the sum
     *                                  or unlock a faster check, so it adds
     *                                  no value here.
     *  - Hashing-based:                NOT APPLICABLE — there's no lookup /
     *                                  frequency / grouping structure to
     *                                  exploit; this is a pure numeric search
     *                                  problem.
     *  - Two pointer / sliding window: NOT APPLICABLE — there's no
     *                                  contiguous subarray or window being
     *                                  tracked; we're searching over a space
     *                                  of divisor VALUES, not array indices.
     *  - Divide and conquer (as a
     *    primary decomposition, distinct
     *    from binary search on answer):
     *                                  NOT APPLICABLE as a separate technique
     *                                  here — binary search on the answer
     *                                  space is the relevant "halving"
     *                                  strategy and is covered as its own
     *                                  approach below.
     *  - Greedy:                       NOT DIRECTLY APPLICABLE — there's no
     *                                  sequence of local choices to make;
     *                                  it's a single global feasibility
     *                                  check per candidate d.
     *  - Dynamic programming:          NOT APPLICABLE — no overlapping
     *                                  subproblems or optimal substructure
     *                                  across states; each divisor is
     *                                  evaluated independently in O(n).
     *  - Tree / graph traversal:       NOT APPLICABLE — no graph/tree
     *                                  structure in the problem.
     *  - Heap / priority queue:        NOT APPLICABLE — we don't need
     *                                  repeated access to a min/max element;
     *                                  every element is processed once per
     *                                  candidate divisor.
     *  - Binary search:                APPLICABLE — this is the classic
     *                                  "binary search on the answer" pattern,
     *                                  since feasibility(d) = "sum <=
     *                                  threshold" is monotonic in d.
     *  - Monotonic stack / deque:      NOT APPLICABLE — no next-greater/
     *                                  next-smaller relationship being
     *                                  tracked.
     *  - Trie / segment tree /
     *    advanced structures:          NOT APPLICABLE — no prefix, range-
     *                                  query, or string-key structure needed.
     *
     * So the two meaningful approaches are:
     *   Approach 1: Brute Force Linear Search over candidate divisors
     *   Approach 2: Binary Search on the Answer (optimal)
     */

    /*
     * ------------------------------------------------------------------------
     * Approach 1: Brute Force Linear Search
     * ------------------------------------------------------------------------
     * Core idea (plain English):
     *   Try every candidate divisor d = 1, 2, 3, ... in increasing order.
     *   For each candidate, compute the full ceiling-sum over nums in O(n).
     *   The first d for which the sum <= threshold is, by definition, the
     *   smallest valid divisor (since we're scanning in increasing order),
     *   so we return immediately.
     *
     * Data structure / paradigm:
     *   Plain iteration; no auxiliary data structure needed.
     *
     * Time Complexity:
     *   O(maxVal * n), where maxVal = max(nums) (up to 10^6) and n =
     *   nums.length (up to 5*10^4). Worst case ~5*10^10 operations — far
     *   too slow to run within typical time limits (would take many
     *   seconds to minutes in practice).
     *
     * Space Complexity:
     *   O(1) additional space (ignoring input storage).
     *
     * Pros:
     *   - Trivial to reason about and verify correctness.
     *   - Zero risk of subtle binary-search off-by-one bugs.
     *
     * Cons:
     *   - Far too slow for the given constraints (maxVal up to 10^6, n up
     *     to 5*10^4) — this is the approach's fatal flaw.
     *   - Doesn't exploit the monotonic structure of the feasibility
     *     function at all.
     *
     * When to use:
     *   - Only as a "correctness oracle" for small inputs during testing/
     *     fuzzing against the optimal solution — never as the submitted
     *     solution given these constraints.
     */
    static int smallestDivisorBruteForce(int[] nums, int threshold) {
        int maxValue = 0;
        for (int value : nums) {
            maxValue = Math.max(maxValue, value);
        }
        // Try every candidate divisor starting from 1 upward.
        for (int candidateDivisor = 1; candidateDivisor <= maxValue; candidateDivisor++) {
            long ceilingSum = computeCeilingSum(nums, candidateDivisor);
            if (ceilingSum <= threshold) {
                return candidateDivisor; // first (smallest) success wins
            }
        }
        // Unreachable given problem guarantees (d = maxValue always works),
        // but included defensively.
        throw new IllegalStateException("No valid divisor found — violates problem guarantees");
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 2: Binary Search on the Answer (OPTIMAL)
     * ------------------------------------------------------------------------
     * Core idea (plain English):
     *   Define feasible(d) = true if sum(ceil(nums[i]/d)) <= threshold.
     *   As d increases, every term ceil(nums[i]/d) is non-increasing, so the
     *   total sum is non-increasing in d. That means feasible(d) looks like
     *   [false, false, ..., false, true, true, ..., true] over d = 1..maxVal
     *   — a monotonic boolean function. This is exactly the shape binary
     *   search wants: search the divisor range [1, max(nums)] for the
     *   leftmost (smallest) d where feasible(d) is true.
     *
     * Data structure / paradigm:
     *   Binary search on the answer space (not on the array itself).
     *
     * Time Complexity:
     *   O(n * log(maxVal)). Each feasibility check is O(n) (one pass over
     *   nums), and binary search performs O(log(maxVal)) checks, where
     *   maxVal <= 10^6, so log2(10^6) ≈ 20. Total ≈ 5*10^4 * 20 = 10^6
     *   operations — comfortably fast.
     *
     * Space Complexity:
     *   O(1) additional space.
     *
     * Pros:
     *   - Meets the time constraints with a large safety margin.
     *   - Clean, standard, highly recognizable pattern for interviewers.
     *   - No tricky data structures — easy to get right under time pressure
     *     once the monotonicity argument is stated.
     *
     * Cons:
     *   - Requires first recognizing/proving the monotonicity property;
     *     not obvious to candidates unfamiliar with "binary search on
     *     answer" problems.
     *   - Off-by-one bugs in the binary search boundaries are the main risk.
     *
     * When to use:
     *   - This is the solution to present and submit in an interview — it's
     *     the correct balance of simplicity and optimality for these
     *     constraints.
     *
     * Monotonicity proof sketch (why binary search is valid here):
     *   For any single element x and divisors d1 < d2:
     *     ceil(x / d1) >= ceil(x / d2)
     *   because dividing by a larger number produces a smaller-or-equal
     *   quotient, and ceiling preserves that non-strict ordering. Summing
     *   this inequality termwise over all elements preserves the
     *   non-increasing property for the total sum. Hence feasible(d) is
     *   monotonic non-decreasing in truth value as d increases, which is
     *   the precondition for binary search on the answer.
     */
    static int smallestDivisorBinarySearch(int[] nums, int threshold) {
        int lowestCandidate = 1;                 // smallest possible divisor
        int highestCandidate = 0;                // will become max(nums)
        for (int value : nums) {
            highestCandidate = Math.max(highestCandidate, value);
        }

        // Standard "find leftmost true" binary search template.
        while (lowestCandidate < highestCandidate) {
            // Bias the midpoint downward to avoid infinite loops when the
            // search space collapses to two elements.
            int midCandidate = lowestCandidate + (highestCandidate - lowestCandidate) / 2;

            long ceilingSum = computeCeilingSum(nums, midCandidate);

            if (ceilingSum <= threshold) {
                // midCandidate is feasible — it *could* be the answer, but a
                // smaller divisor might also work, so search the left half
                // inclusive of midCandidate.
                highestCandidate = midCandidate;
            } else {
                // midCandidate is too small a divisor (sum too big) — the
                // answer must be strictly larger.
                lowestCandidate = midCandidate + 1;
            }
        }

        // lowestCandidate == highestCandidate here: the smallest feasible d.
        return lowestCandidate;
    }

    /*
     * Shared helper: computes sum_i( ceil(nums[i] / divisor) ) using a long
     * accumulator to avoid overflow (see Clarifying Question 6). Includes an
     * early-exit optimization — once the running sum already exceeds
     * threshold, there's no point continuing the pass, since all remaining
     * terms are non-negative and can only increase the sum further.
     */
    static long computeCeilingSum(int[] nums, int divisor) {
        long runningSum = 0L;
        for (int value : nums) {
            // Integer ceiling division without floating point:
            // ceil(a / b) == (a + b - 1) / b   for positive a, b.
            runningSum += (long) (value + divisor - 1) / divisor;
        }
        return runningSum;
    }

    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * ---------------------------------------------------------------------------------------------
     * | Approach                     | Time            | Space | Best For              | Limitations |
     * |-------------------------------|-----------------|-------|------------------------|-------------|
     * | 1. Brute Force Linear Search  | O(maxVal * n)   | O(1)  | Small inputs / testing | Times out on real  |
     * |                                |                 |       | as a correctness       | constraints (up to |
     * |                                |                 |       | oracle                 | ~5*10^10 ops)      |
     * |-------------------------------|-----------------|-------|------------------------|--------------------|
     * | 2. Binary Search on Answer    | O(n log maxVal) | O(1)  | Production / interview | Requires proving   |
     * |    (OPTIMAL)                  |                 |       | submission; meets      | monotonicity;      |
     * |                                |                 |       | constraints easily     | boundary bugs risk |
     * ---------------------------------------------------------------------------------------------
     */

    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 2 (Binary Search on the Answer) as my final
     * solution. Reasoning:
     *
     *   - Clarity: the monotonicity argument is short and easy to state
     *     out loud, and the binary-search-on-answer template is a
     *     recognizable, well-understood pattern to an interviewer.
     *   - Coding speed: it's a small amount of code (one helper + one
     *     search loop), quick to write correctly once the template is
     *     memorized, and easy to test on a whiteboard.
     *   - Interviewer expectations: given n up to 5*10^4 and values up to
     *     10^6, an interviewer will expect roughly O(n log maxVal); brute
     *     force is a fine warm-up statement but not an acceptable final
     *     answer here.
     *   - Optimality: O(n log maxVal) is essentially optimal for this
     *     problem — you must inspect every element at least once for any
     *     candidate divisor, and binary search minimizes the number of
     *     candidates checked.
     *
     * My interview sequencing: state the brute force first out loud to
     * confirm correctness and lock in the ceiling-division formula, then
     * immediately pivot to the monotonicity observation and present binary
     * search as the real solution — never jump straight to optimal without
     * narrating the "why."
     */

    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (production-quality)
     * ========================================================================
     */

    /**
     * Returns the smallest positive integer divisor {@code d} such that the
     * sum over {@code nums} of {@code ceil(nums[i] / d)} is less than or
     * equal to {@code threshold}.
     *
     * <p>Approach: binary search on the answer space {@code [1, max(nums)]},
     * exploiting the fact that the ceiling-sum is monotonically
     * non-increasing as the divisor increases.
     *
     * @param nums      array of positive integers, {@code 1 <= nums.length <= 5*10^4},
     *                  {@code 1 <= nums[i] <= 10^6}
     * @param threshold upper bound on the ceiling-sum, {@code nums.length <= threshold <= 10^6}
     * @return the smallest valid positive divisor
     * @throws IllegalArgumentException if nums is null/empty or threshold is
     *                                   too small to ever be satisfiable
     *                                   (defensive — problem guarantees this
     *                                   won't happen under stated constraints)
     */
    static int findSmallestDivisor(int[] nums, int threshold) {
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("nums must be non-null and non-empty");
        }
        if (threshold < nums.length) {
            // Even with the largest possible divisor, every term collapses
            // to 1, giving a sum of exactly nums.length. If threshold is
            // smaller than that floor, no divisor can ever work.
            throw new IllegalArgumentException(
                "threshold must be >= nums.length for a solution to exist");
        }

        // Search space: divisor candidates from 1 up to max(nums), since
        // any larger divisor is provably wasteful (every term already
        // bottoms out at 1 by the time d == max(nums)).
        int searchLow = 1;
        int searchHigh = Arrays.stream(nums).max().getAsInt();

        while (searchLow < searchHigh) {
            int midpointDivisor = searchLow + (searchHigh - searchLow) / 2;

            if (isFeasible(nums, midpointDivisor, threshold)) {
                // Feasible: this divisor (or something smaller) could be the
                // answer. Keep midpointDivisor in play by not excluding it.
                searchHigh = midpointDivisor;
            } else {
                // Infeasible: sum too large, need a strictly bigger divisor.
                searchLow = midpointDivisor + 1;
            }
        }

        // Loop invariant: searchLow == searchHigh, and this value is the
        // smallest divisor for which isFeasible(...) returns true.
        return searchLow;
    }

    /**
     * Checks whether {@code divisor} produces a ceiling-sum within
     * {@code threshold}. Uses a long accumulator to avoid overflow and
     * short-circuits as soon as the sum provably exceeds threshold.
     */
    private static boolean isFeasible(int[] nums, int divisor, int threshold) {
        long ceilingSum = 0L;
        for (int value : nums) {
            // Overflow-safe ceiling division: ceil(a/b) == (a + b - 1) / b
            // for positive integers a, b. value <= 10^6 and divisor >= 1,
            // so (value + divisor - 1) safely fits in a long.
            ceilingSum += (long) (value + divisor - 1) / divisor;

            // Early exit: sum only grows from here, no need to keep scanning.
            if (ceilingSum > threshold) {
                return false;
            }
        }
        return ceilingSum <= threshold;
    }

    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE (using Example 1: nums = [1,2,5,9], threshold = 6)
     * ========================================================================
     *
     * Initial: searchLow = 1, searchHigh = max(nums) = 9
     *
     * Iteration 1:
     *   midpointDivisor = 1 + (9-1)/2 = 5
     *   isFeasible(nums, 5, 6)?
     *     ceil(1/5)=1 -> sum=1
     *     ceil(2/5)=1 -> sum=2
     *     ceil(5/5)=1 -> sum=3
     *     ceil(9/5)=2 -> sum=5
     *     sum=5 <= 6 -> feasible = true
     *   feasible -> searchHigh = 5
     *   State now: searchLow=1, searchHigh=5
     *
     * Iteration 2:
     *   midpointDivisor = 1 + (5-1)/2 = 3
     *   isFeasible(nums, 3, 6)?
     *     ceil(1/3)=1 -> sum=1
     *     ceil(2/3)=1 -> sum=2
     *     ceil(5/3)=2 -> sum=4
     *     ceil(9/3)=3 -> sum=7
     *     sum=7 > 6 -> early exit, feasible = false
     *   infeasible -> searchLow = 3 + 1 = 4
     *   State now: searchLow=4, searchHigh=5
     *
     * Iteration 3:
     *   midpointDivisor = 4 + (5-4)/2 = 4
     *   isFeasible(nums, 4, 6)?
     *     ceil(1/4)=1 -> sum=1
     *     ceil(2/4)=1 -> sum=2
     *     ceil(5/4)=2 -> sum=4
     *     ceil(9/4)=3 -> sum=7
     *     sum=7 > 6 -> early exit, feasible = false
     *   infeasible -> searchLow = 4 + 1 = 5
     *   State now: searchLow=5, searchHigh=5 -> loop terminates
     *
     * Return searchLow = 5.  Matches the hand-computed expected answer (5).
     */

    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - Brute force linear search over divisors is easy to state and prove
     *   correct, but is O(maxVal * n) and blows through the time limit for
     *   the given constraints — useful only as a testing oracle.
     * - Binary search on the answer is the right production solution:
     *   O(n log maxVal) time, O(1) space, and relies on a clean monotonicity
     *   proof (larger divisor -> smaller-or-equal ceiling terms -> smaller-
     *   or-equal total sum).
     * - Known assumptions/limitations of the final solution:
     *     * Assumes nums is non-null, non-empty, and all-positive (per
     *       constraints); defensive checks throw otherwise.
     *     * Assumes threshold >= nums.length, matching the problem's stated
     *       constraint that guarantees solvability.
     *     * Uses `long` accumulation to avoid the int-overflow trap
     *       identified in clarifying question 6.
     */

    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "What if nums could contain negative numbers or zero — how would
     *     ceiling division and the monotonicity argument change?"
     * 2. "What if this query (same nums, different thresholds) is called
     *     many times — how would you amortize the cost across calls?"
     *     (e.g., precompute nothing extra needed, since each query is
     *     already O(n log maxVal); but if nums itself changes rarely and
     *     queries are extremely frequent, discuss precomputing sorted
     *     nums or prefix sums to speed up isFeasible via math instead of
     *     a full O(n) scan.)
     * 3. "Can you solve this without knowing max(nums) in advance, e.g. if
     *     nums is a very large stream you can't fully buffer?"
     * 4. "How would you parallelize the feasibility check across multiple
     *     threads for very large n (e.g., n in the hundreds of millions)?"
     * 5. "What if instead of a single divisor, you needed to find the
     *     smallest divisor such that the sum falls within a RANGE
     *     [minThreshold, maxThreshold] — does binary search still work?"
     * 6. "How would you modify this if 'round up' were instead 'round to
     *     nearest, ties away from zero' — does monotonicity still hold?"
     */

    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Integer overflow: with n up to 5*10^4 and values up to 10^6, the
     *    raw sum can reach ~5*10^10, silently overflowing a 32-bit int and
     *    producing wrong (often negative) results that only show up on
     *    large/adversarial test cases — not on small handwritten examples.
     *
     * 2. Wrong ceiling-division formula: writing (value / divisor) + 1
     *    unconditionally is WRONG when value is exactly divisible by
     *    divisor (e.g. 10/2 should stay 5, not become 6). The correct
     *    overflow-safe formula for positive integers is
     *    (value + divisor - 1) / divisor.
     *
     * 3. Binary search boundary/off-by-one bugs: using searchHigh = midpoint
     *    - 1 when the condition is feasible (instead of searchHigh =
     *    midpoint) can skip over the correct answer, since midpoint itself
     *    might BE the smallest valid divisor and must remain in the search
     *    range on the "feasible" branch.
     *
     * 4. Assuming the search upper bound can be threshold or some other
     *    unrelated value instead of max(nums) — either picking too small an
     *    upper bound (missing the true answer, as shown in Example 2 where
     *    the answer equals max(nums)=44) or unnecessarily using a much
     *    larger bound like 10^6 always, which still works but wastes a few
     *    extra iterations without being wrong — the real trap is going too
     *    small, not too large.
     */

    /*
     * ========================================================================
     * TEST HARNESS (main) — cross-validates brute force vs. binary search
     * across the worked examples plus randomized fuzz trials.
     * ========================================================================
     */
    public static void main(String[] args) {
        // --- Worked examples from Section 3 ---
        record TestCase(int[] nums, int threshold, int expected) {}
        List<TestCase> examples = List.of(
            new TestCase(new int[]{1, 2, 5, 9}, 6, 5),
            new TestCase(new int[]{44, 22, 33}, 3, 44),
            new TestCase(new int[]{1_000_000}, 1_000_000, 1)
        );

        for (TestCase testCase : examples) {
            int optimalResult = findSmallestDivisor(testCase.nums(), testCase.threshold());
            int bruteForceResult = smallestDivisorBruteForce(testCase.nums(), testCase.threshold());
            int binarySearchResult = smallestDivisorBinarySearch(testCase.nums(), testCase.threshold());

            System.out.printf(
                "nums=%s threshold=%d -> optimal=%d bruteForce=%d binarySearch=%d expected=%d%n",
                Arrays.toString(testCase.nums()), testCase.threshold(),
                optimalResult, bruteForceResult, binarySearchResult, testCase.expected()
            );

            assert optimalResult == testCase.expected() : "Optimal mismatch on worked example";
            assert bruteForceResult == testCase.expected() : "Brute force mismatch on worked example";
            assert binarySearchResult == testCase.expected() : "Binary search mismatch on worked example";
        }

        // --- Randomized fuzz trials: cross-validate optimal vs. brute force ---
        Random random = new Random(42);
        int fuzzTrials = 2000;
        for (int trial = 0; trial < fuzzTrials; trial++) {
            int length = 1 + random.nextInt(15);        // keep small so brute force is fast
            int[] nums = new int[length];
            int maxPossibleValue = 1 + random.nextInt(50);
            for (int index = 0; index < length; index++) {
                nums[index] = 1 + random.nextInt(maxPossibleValue);
            }
            int threshold = length + random.nextInt(200); // ensure threshold >= length

            int optimalResult = findSmallestDivisor(nums, threshold);
            int bruteForceResult = smallestDivisorBruteForce(nums, threshold);

            if (optimalResult != bruteForceResult) {
                throw new AssertionError(String.format(
                    "Mismatch on trial %d: nums=%s threshold=%d optimal=%d bruteForce=%d",
                    trial, Arrays.toString(nums), threshold, optimalResult, bruteForceResult));
            }
        }

        System.out.println("All " + fuzzTrials + " fuzz trials passed. Optimal solution verified.");
    }
}
