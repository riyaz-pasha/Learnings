/* =====================================================================================
 * FILE: NextPalindromeInterview.java
 * MOCK GOOGLE ONSITE INTERVIEW — FULL WALKTHROUGH
 *
 * PROBLEM STATEMENT (as given by interviewer):
 *   Given a numeric string `numStr` that is itself a palindrome, return the smallest
 *   palindrome strictly larger than `numStr` that can be produced by rearranging the
 *   exact digits of `numStr`. If no such palindrome exists, return "".
 *
 * Example:
 *   input  = "123321"
 *   valid palindromes from these digits: "213312","231132","312213","132231","321123"
 *   output = "132231"   (smallest one that is > "123321")
 * =====================================================================================
 */

import java.util.*;

public class NextPalindromeInterview {

    /* =================================================================================
     * SECTION 1 — RESTATE THE PROBLEM
     * =================================================================================
     * In my own words:
     *   I'm given a string of digits, numStr, and I'm told it already reads the same
     *   forwards and backwards (it's a palindrome). I need to rearrange ITS OWN digits
     *   (not add, remove, or change any digit — just reorder them) to build a NEW
     *   palindrome whose numeric value is strictly greater than numStr, and among all
     *   such palindromes, I must return the smallest one. If it's impossible to build
     *   any larger palindrome from this exact multiset of digits, I return "".
     *
     * Key constraints / inputs / outputs / assumptions to call out explicitly:
     *   - Input: a single String, `numStr`, consisting only of characters '0'-'9'.
     *   - `numStr` is guaranteed (by the problem) to already be a palindrome.
     *   - Output: a String — either a rearranged palindrome strictly greater than
     *     numStr, or "" if none exists.
     *   - Every output palindrome must use EXACTLY the same digit multiset as numStr
     *     (same length, same frequency of each digit) — this is a rearrangement
     *     (permutation) problem, not a digit-generation problem.
     *   - "Larger" means larger as a number. Since every candidate is a permutation of
     *     the same digits, every candidate has the SAME LENGTH as numStr, so ordinary
     *     lexicographic string comparison is equivalent to numeric comparison — no
     *     BigInteger arithmetic is required.
     * =================================================================================
     */


    /* =================================================================================
     * SECTION 2 — CLARIFYING QUESTIONS (asked to interviewer, with assumed answers)
     * =================================================================================
     * Q1: What's the maximum length of numStr — do I need to worry about huge inputs
     *     (e.g., 10^5+ digits), or is n small?
     *     A (assumed): Could be large (up to ~10^5). Solution should be near-linear,
     *     not factorial/exponential.
     *
     * Q2: Is numStr guaranteed to already be a valid palindrome, or should my code
     *     defensively verify that?
     *     A (assumed): Guaranteed by the problem statement, but production code should
     *     still validate defensively and fail fast on bad input.
     *
     * Q3: Can numStr contain leading zeros, e.g., is "0" or "00" a valid input?
     *     A (assumed): Only "0" itself (or repeated zero digits like "0","000") could
     *     start with '0', consistent with it representing a valid number; otherwise no
     *     leading zeros in a multi-digit input.
     *
     * Q4: Must the OUTPUT also avoid leading zeros?
     *     A (assumed): Yes — outputs represent numbers too. (I'll show later that the
     *     optimal algorithm can never introduce a leading zero given Q3's assumption.)
     *
     * Q5: Am I rearranging the EXACT multiset of digits, or can I add/drop digits?
     *     A (assumed): Exact same multiset only — the problem explicitly says
     *     "rearranging its digits."
     *
     * Q6: What should happen on null or empty input?
     *     A (assumed): Not a valid input per constraints (length >= 1), but I'll guard
     *     against it defensively (throw IllegalArgumentException, or return "").
     *
     * Q7: Since all candidates share numStr's length, is lexicographic string
     *     comparison equivalent to numeric comparison?
     *     A (assumed): Yes, exactly — this simplifies the implementation a lot.
     *
     * Q8: Will this function be called once, or many times in a hot path (should I
     *     worry about amortized cost / allocation pressure)?
     *     A (assumed): Treat each call independently; the optimal solution is already
     *     O(n) time / O(n) space so it's fine even under repeated calls.
     * =================================================================================
     */


    /* =================================================================================
     * SECTION 3 — EXAMPLES & EDGE CASES
     * =================================================================================
     * Example 1 (normal case):
     *   input  = "123321"
     *   output = "132231"
     *   (given in the prompt; confirms the general algorithm below.)
     *
     * Example 2 (edge case — no answer exists):
     *   input  = "5445"
     *   digits {4:2, 5:2}. Only two palindromes possible: "4554" and "5445".
     *   "5445" is already the LARGEST arrangement -> output = ""
     *   Also trivially: input = "9" (single digit) -> output = "" (nothing to rearrange).
     *
     * Example 3 (boundary / duplicate-digit tie-breaking case):
     *   input  = "122221"
     *   digits {1:2, 2:4}. This exercises duplicate digits WITHIN the half that must be
     *   permuted, so any correct algorithm must not skip or double count identical
     *   arrangements.
     *   output = "212212"
     *   (walked through in the Dry Run section below.)
     * =================================================================================
     */


    /* =================================================================================
     * SECTION 4/5 — ALL POSSIBLE APPROACHES
     * =================================================================================
     */

    /* --------------------------------------------------------------------------------
     * Approach 1: Brute Force — Generate All Full-Length Permutations
     * --------------------------------------------------------------------------------
     * Idea (plain English):
     *   Generate every permutation of ALL n digits (via classic recursive swapping),
     *   keep only the ones that (a) read as palindromes and (b) are strictly greater
     *   than numStr, and track the smallest such candidate seen.
     *
     * Paradigm: Naive brute-force recursive permutation generation.
     *
     * Time Complexity:  O(n! * n)  — n! permutations, each costs O(n) to build/compare.
     * Space Complexity: O(n) recursion depth (plus O(n) per generated candidate string).
     *
     * Pros:
     *   - Trivial to reason about correctness; a great "obviously correct" starting point.
     *   - No special insight required.
     * Cons:
     *   - Factorial blow-up; unusable beyond n ~ 10.
     *   - Wastes enormous time regenerating duplicate permutations when digits repeat
     *     (e.g., "1111...1" still explores n! branches even though there's only 1
     *     distinct permutation).
     * When to use: Never in production; only as a warm-up / correctness oracle for
     * testing the optimal solution on small inputs.
     * -------------------------------------------------------------------------------- */
    public static String bruteForceAllPermutations(String numStr) {
        char[] chars = numStr.toCharArray();
        String[] best = { null }; // boxed in array so the recursive helper can mutate it
        permuteAndCheck(chars, 0, numStr, best);
        return best[0] == null ? "" : best[0];
    }

    private static void permuteAndCheck(char[] chars, int start, String original, String[] best) {
        if (start == chars.length) {
            String candidate = new String(chars);
            if (isPalindrome(candidate) && candidate.compareTo(original) > 0) {
                if (best[0] == null || candidate.compareTo(best[0]) < 0) {
                    best[0] = candidate;
                }
            }
            return;
        }
        for (int i = start; i < chars.length; i++) {
            swap(chars, start, i);
            permuteAndCheck(chars, start + 1, original, best); // recurse to fix next slot
            swap(chars, start, i);                             // backtrack
        }
    }


    /* --------------------------------------------------------------------------------
     * Approach 2: Combinatorial Backtracking on the Half-Multiset (pruned search)
     * --------------------------------------------------------------------------------
     * Idea (plain English):
     *   Key realization: a palindrome is completely determined by its FIRST HALF (the
     *   second half must mirror it, and the middle digit, if any, is fixed). So instead
     *   of permuting all n digits, only permute the n/2 digits of the left half, using
     *   the standard "skip adjacent duplicates in sorted order" trick to avoid
     *   generating the same arrangement twice. Every generated half is guaranteed to
     *   produce a palindrome by construction, so no isPalindrome() check is needed.
     *
     * Paradigm: Backtracking / DFS over a multiset with duplicate-pruning.
     *
     * Time Complexity:  O(D * n) where D = number of DISTINCT permutations of the half
     *                    multiset (D <= (n/2)! in the worst case of all-distinct digits).
     * Space Complexity: O(n) recursion depth + O(D) implicit branching work.
     *
     * Pros:
     *   - Massive improvement over Approach 1 (search space shrinks from n! to at most
     *     (n/2)!, and duplicate digits shrink it further via pruning).
     *   - Conceptually simple: "only the half matters" is the seed of the optimal idea.
     * Cons:
     *   - Still exponential in the worst case (e.g., 20 distinct digits in the half).
     *   - More code than the optimal approach, for no asymptotic benefit at scale.
     * When to use: Reasonable as an intermediate step while deriving the optimal
     * solution out loud in an interview, or if n is provably tiny and clarity trumps
     * everything. Not for production at scale.
     * -------------------------------------------------------------------------------- */
    public static String backtrackHalfPermutations(String numStr) {
        int n = numStr.length();
        if (n == 0) return "";
        int half = n / 2;
        boolean odd = (n % 2 == 1);
        char mid = odd ? numStr.charAt(half) : '\0';

        char[] halfChars = numStr.substring(0, half).toCharArray();
        Arrays.sort(halfChars); // sorting enables the adjacent-duplicate skip below

        boolean[] used = new boolean[halfChars.length];
        String[] best = { null };
        StringBuilder current = new StringBuilder();

        backtrackHelper(halfChars, used, current, numStr, mid, odd, best);
        return best[0] == null ? "" : best[0];
    }

    private static void backtrackHelper(char[] halfChars, boolean[] used, StringBuilder current,
                                         String original, char mid, boolean odd, String[] best) {
        if (current.length() == halfChars.length) {
            StringBuilder fullBuilder = new StringBuilder(current);
            if (odd) fullBuilder.append(mid);
            fullBuilder.append(new StringBuilder(current).reverse()); // mirror -> guaranteed palindrome
            String candidate = fullBuilder.toString();
            if (candidate.compareTo(original) > 0) {
                if (best[0] == null || candidate.compareTo(best[0]) < 0) {
                    best[0] = candidate;
                }
            }
            return;
        }
        for (int idx = 0; idx < halfChars.length; idx++) {
            if (used[idx]) continue;
            // Skip duplicate branches: if the identical digit right before this one
            // hasn't been used at this recursion level, using this one would just
            // regenerate an arrangement we've already explored.
            if (idx > 0 && halfChars[idx] == halfChars[idx - 1] && !used[idx - 1]) continue;
            used[idx] = true;
            current.append(halfChars[idx]);
            backtrackHelper(halfChars, used, current, original, mid, odd, best);
            current.deleteCharAt(current.length() - 1);
            used[idx] = false;
        }
    }


    /* --------------------------------------------------------------------------------
     * Approach 3 (OPTIMAL): Greedy "Next Permutation" on the Half String
     * --------------------------------------------------------------------------------
     * Idea (plain English):
     *   Push the "only the half matters" insight all the way: since the full
     *   palindrome's value is a strictly increasing function of its left half's value
     *   (same length => same middle/mirrored structure => comparing the whole string
     *   reduces exactly to comparing the left halves), the answer is simply: take the
     *   classic "next permutation" of the left half's characters. If that exists,
     *   mirror it into a full palindrome; if it doesn't (half is already in maximal
     *   descending order), no larger palindrome can be formed -> return "".
     *
     * Paradigm: Greedy, using the textbook in-place "next permutation" algorithm
     *   (same one behind C++'s std::next_permutation). It naturally and correctly
     *   handles duplicate digits with zero extra bookkeeping.
     *
     * Time Complexity:  O(n)  — next-permutation is O(n) worst case (single pass +
     *                    bounded pointer walk + reverse), assembly is O(n).
     * Space Complexity: O(n) — one char[] copy of the half, plus the result buffer.
     *
     * Pros:
     *   - Linear time, linear space — scales to very large inputs.
     *   - Short, well-known algorithm; low bug surface once the key insight is found.
     *   - Handles duplicate digits for free (no dedup logic needed, unlike Approach 2).
     * Cons:
     *   - Requires spotting the non-obvious insight (palindrome ordering == half
     *     ordering) — without it, this approach isn't discoverable.
     *   - Assumes input is already known to be a valid, well-formed palindrome (per
     *     problem guarantee); a malformed input needs a pre-check (see production code).
     * When to use: Always, in production, once the insight is confirmed — it strictly
     * dominates Approaches 1 and 2 in both time and space.
     * -------------------------------------------------------------------------------- */
    public static String nextPalindromeGreedy(String numStr) {
        int n = numStr.length();
        if (n == 0) return "";
        int half = n / 2;
        boolean odd = (n % 2 == 1);

        char[] leftHalf = numStr.substring(0, half).toCharArray();
        boolean hasNext = nextPermutation(leftHalf);
        if (!hasNext) return "";

        StringBuilder result = new StringBuilder(n);
        result.append(leftHalf);
        if (odd) result.append(numStr.charAt(half));
        result.append(new StringBuilder(new String(leftHalf)).reverse());
        return result.toString();
    }

    // Standard in-place "next permutation" — mutates arr into the next greater
    // arrangement; returns false if arr is already the maximal arrangement.
    private static boolean nextPermutation(char[] arr) {
        int n = arr.length;
        int i = n - 2;
        while (i >= 0 && arr[i] >= arr[i + 1]) i--;
        if (i < 0) return false;              // already fully descending -> maximal
        int j = n - 1;
        while (arr[j] <= arr[i]) j--;         // find smallest suffix digit > arr[i]
        swap(arr, i, j);
        reverseRange(arr, i + 1, n - 1);      // suffix is now minimal (ascending)
        return true;
    }


    /* =================================================================================
     * SECTION 6 — PARADIGMS CONSIDERED BUT NOT APPLICABLE (one-line reasons)
     * =================================================================================
     * - Sorting-based (standalone):    Sorting alone doesn't encode "next greater
     *                                   arrangement"; it's only a minor sub-step inside
     *                                   next-permutation, not an independent approach.
     * - Hashing-based:                 Only used trivially (digit frequency via a
     *                                   10-bucket array) to validate palindrome
     *                                   formability; not a distinguishing strategy here.
     * - Two pointer / sliding window:  No contiguous subarray/substring property is
     *                                   being optimized; nothing to "slide."
     * - Divide and conquer:            No natural independent subproblems to split and
     *                                   recombine — palindrome-ness is one global,
     *                                   non-separable constraint.
     * - Dynamic programming:           No overlapping subproblems / optimal substructure
     *                                   to exploit; this is an ordering/permutation
     *                                   problem, not an optimization-over-choices one.
     * - Tree / graph traversal:        No graph or tree structure is inherent here.
     * - Heap / priority queue:         We don't need "k smallest of many streamed
     *                                   candidates" — the answer is derived directly.
     * - Binary search:                 No monotonic predicate over an easily-indexable
     *                                   search space that beats the O(n) direct method.
     * - Monotonic stack / deque:       No need to maintain ordered elements while
     *                                   scanning; not that shape of problem.
     * - Trie / segment tree / etc.:    No prefix-matching or range-query requirement.
     * =================================================================================
     */


    /* =================================================================================
     * SECTION 7 — APPROACHES COMPARISON TABLE
     * =================================================================================
     * Approach                      | Time         | Space  | Best For              | Limitations
     * -------------------------------------------------------------------------------------------------
     * 1. Brute Force (all perms)    | O(n! * n)    | O(n)   | tiny n, sanity oracle | unusable beyond n~10, wastes work on dup digits
     * 2. Backtracking (half only)   | O(D * n)*    | O(n)   | moderate n, teaching  | still exponential worst case (D up to (n/2)!)
     * 3. Next Permutation (OPTIMAL) | O(n)         | O(n)   | production, any n     | needs the half-ordering insight; assumes valid palindrome input
     *
     * * D = number of distinct permutations of the half-digit multiset.
     * =================================================================================
     */


    /* =================================================================================
     * SECTION 8 — RECOMMENDED APPROACH FOR THE INTERVIEW
     * =================================================================================
     * I would present Approach 3 (Next Permutation on the Half String).
     * Why:
     *   - It's asymptotically optimal: O(n) time, O(n) space, versus factorial/expo
     *     alternatives — this matters a lot if the interviewer probes "what if n is
     *     10^5?"
     *   - It's fast to CODE correctly (~20-30 lines), using a well-known textbook
     *     algorithm (next permutation) that most interviewers recognize and trust,
     *     which reduces "did I just get lucky" risk during live coding.
     *   - It clearly demonstrates the core insight the interviewer is testing for
     *     (palindrome ordering reduces to half-string ordering) rather than hiding it
     *     inside brute force.
     *   - It naturally and correctly handles duplicate digits without extra dedup
     *     logic, reducing bug surface under interview time pressure.
     * I'd mention Approaches 1 and 2 briefly to show breadth and to justify WHY
     * Approach 3 is worth the extra insight, then implement Approach 3 (or the
     * production-hardened version below) as the final answer.
     * =================================================================================
     */


    /* =================================================================================
     * SECTION 10 — DRY RUN / TRACE  (worked by hand here, executed in main() below)
     * =================================================================================
     * Using Example 3: numStr = "122221"
     *
     *   n = 6, half = 3, odd = false
     *   leftHalf = ['1','2','2']   (numStr.substring(0,3))
     *
     *   nextPermutation(['1','2','2']):
     *     i starts at n-2 = 1: arr[1]='2', arr[2]='2' -> '2' >= '2' is true -> i-- => i=0
     *       arr[0]='1', arr[1]='2' -> '1' >= '2' is false -> stop. i = 0 (pivot)
     *     j starts at n-1 = 2: arr[2]='2' > arr[0]='1' -> stop. j = 2 (successor)
     *     swap(arr, 0, 2)  =>  arr = ['2','2','1']
     *     reverseRange(arr, i+1=1, n-1=2)  =>  reverse indices [1,2]: ['2','1','2']
     *     final leftHalf = "212",  return true
     *
     *   Assembly:
     *     result = "212"                (leftHalf)
     *     odd == false, so no middle digit appended
     *     result += reverse("212") = "212"   (palindrome itself)
     *     result = "212212"
     *
     *   Check: "212212" reversed is "212212" -> valid palindrome. ✓
     *   Check: "212212" > "122221" -> '2' > '1' at index 0 -> true. ✓
     *   This matches the earlier hand-derivation in Section 3.
     * =================================================================================
     */


    /* =================================================================================
     * SECTION 11 — CLOSING SUMMARY
     * =================================================================================
     * - Approach 1 (full brute force) is correct but factorial — a warm-up only.
     * - Approach 2 prunes to the half-multiset with dedup, shrinking the search space
     *   drastically, but remains exponential in the worst case (all-distinct half).
     * - Approach 3 exploits the fact that a palindrome's value is a monotonic function
     *   of its left half, collapsing the problem to a single classic "next
     *   permutation" call — O(n) time, O(n) space, and the right answer for production.
     * - Key assumption carried through: numStr is guaranteed to already be a valid
     *   palindrome digit string; the production solution defensively re-validates this.
     * - Known limitation: this solution only rearranges the EXISTING digit multiset —
     *   it cannot find a larger palindrome that would require adding, removing, or
     *   changing digits (that's a different, harder problem — see follow-ups).
     * =================================================================================
     */


    /* =================================================================================
     * SECTION 12 — FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * =================================================================================
     * 1. What if numStr is NOT guaranteed to be a palindrome — how do you first check
     *    whether ANY palindrome is formable from its digits (at most one odd-count
     *    digit), and then find the smallest palindrome overall from that multiset?
     * 2. How would you find the smallest palindrome strictly greater than numStr if you
     *    could use a DIFFERENT digit count/length (not just a rearrangement)?
     * 3. Can you find the K-th smallest palindrome greater than numStr formed from the
     *    same digits, without enumerating all of them (hint: apply next-permutation
     *    K times, or compute directly via a rank/combinatorics argument)?
     * 4. How would you adapt this to find the LARGEST palindrome smaller than numStr
     *    (a "previous permutation" variant)?
     * 5. Does the half-ordering insight still hold if the alphabet isn't digits 0-9 but
     *    arbitrary characters (e.g., lowercase letters)? What changes, if anything?
     * 6. If this runs as a hot-path service function called millions of times, how
     *    would you handle input validation cost, malformed/malicious input, and very
     *    large n (streaming vs. materializing the full string)?
     * =================================================================================
     */


    /* =================================================================================
     * SECTION 13 — WHAT CANDIDATES TYPICALLY MISS
     * =================================================================================
     * 1. Not realizing only the FIRST HALF needs to be permuted — many candidates try
     *    to generate full n-digit permutations and filter for palindromes, which is an
     *    unnecessary n! -> (n/2)! -> n blow-up they could have avoided entirely.
     * 2. Off-by-one / mishandling of the middle character in odd-length strings —
     *    either forgetting it, or accidentally including it in the "half" being
     *    permuted (which corrupts its parity/count and breaks correctness).
     * 3. Forgetting the "no next permutation exists" case (half already in maximal
     *    descending order) and crashing or returning null instead of "".
     * 4. Assuming next-permutation-style algorithms need special handling for duplicate
     *    digits — either failing to verify the standard algorithm already handles
     *    duplicates correctly (it does, via >= / <= comparisons), or bolting on
     *    unnecessary and bug-prone manual dedup logic on top of it.
     * =================================================================================
     */


    /* =================================================================================
     * SHARED UTILITY METHODS (used by Approaches 1-3's demo implementations above)
     * =================================================================================
     */
    private static boolean isPalindrome(String value) {
        int left = 0, right = value.length() - 1;
        while (left < right) {
            if (value.charAt(left) != value.charAt(right)) return false;
            left++;
            right--;
        }
        return true;
    }

    private static void swap(char[] array, int i, int j) {
        char temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    private static void reverseRange(char[] array, int from, int to) {
        while (from < to) {
            swap(array, from, to);
            from++;
            to--;
        }
    }


    /* =================================================================================
     * MAIN — demonstrates all approaches on the examples, plus the production solver
     * =================================================================================
     */
    public static void main(String[] args) {
        String[] testInputs = { "123321", "5445", "9", "11", "1221", "1234321", "122221" };

        for (String input : testInputs) {
            System.out.println("Input: " + input);
            System.out.println("  Approach 1 (Brute Force)      : " + bruteForceAllPermutations(input));
            System.out.println("  Approach 2 (Backtracking Half): " + backtrackHalfPermutations(input));
            System.out.println("  Approach 3 (Next Permutation) : " + nextPalindromeGreedy(input));
            System.out.println("  Production Solver             : " + NextPalindromeSolver.nextPalindrome(input));
            System.out.println();
        }

        runTrace("122221");
    }

    private static void runTrace(String numStr) {
        System.out.println("=== DRY RUN TRACE for input: " + numStr + " ===");
        int half = numStr.length() / 2;
        char[] leftHalf = numStr.substring(0, half).toCharArray();
        System.out.println("Extracted left half : " + new String(leftHalf));
        boolean advanced = nextPermutation(leftHalf);
        System.out.println("Next permutation     : " + new String(leftHalf) + " (advanced=" + advanced + ")");
        System.out.println("Assembled palindrome : " + nextPalindromeGreedy(numStr));
    }
}


/* =====================================================================================
 * SECTION 9 — DEEP DIVE: OPTIMAL SOLUTION (PRODUCTION-QUALITY VERSION)
 * =====================================================================================
 * Self-contained, defensively-validated implementation of Approach 3, suitable to hand
 * in as the final interview answer.
 * ===================================================================================== */
final class NextPalindromeSolver {

    private NextPalindromeSolver() {
        // Utility class — no instances.
    }

    /**
     * Returns the smallest palindrome strictly greater than {@code numStr} that can be
     * formed using exactly the same multiset of digits as {@code numStr}, or "" if no
     * such palindrome exists.
     *
     * Precondition: numStr is non-null, non-empty, digits only ('0'-'9'), and is
     * itself already a valid palindrome.
     *
     * Time Complexity:  O(n)
     * Space Complexity: O(n)
     */
    public static String nextPalindrome(String numStr) {
        validateInput(numStr);

        int length = numStr.length();
        int halfLength = length / 2;
        boolean hasMiddleDigit = (length % 2 == 1);

        // Only the first half determines the whole palindrome's value: position
        // (length - 1 - k) is always forced to mirror position k, and a fixed middle
        // digit (if any) never moves. So we only need the NEXT GREATER arrangement
        // of this half — classic "next permutation".
        char[] leftHalf = numStr.substring(0, halfLength).toCharArray();

        boolean advanced = nextPermutationInPlace(leftHalf);
        if (!advanced) {
            // leftHalf is already the lexicographically largest arrangement of its
            // own digits -> no larger palindrome can be built from this multiset.
            return "";
        }

        return assemblePalindrome(leftHalf, numStr, halfLength, hasMiddleDigit);
    }

    private static void validateInput(String numStr) {
        if (numStr == null || numStr.isEmpty()) {
            throw new IllegalArgumentException("numStr must be a non-null, non-empty digit string.");
        }
        for (int index = 0; index < numStr.length(); index++) {
            char character = numStr.charAt(index);
            if (character < '0' || character > '9') {
                throw new IllegalArgumentException("numStr must contain only digits 0-9. Found: " + character);
            }
        }
        if (!isPalindrome(numStr)) {
            throw new IllegalArgumentException("numStr must already be a palindrome: " + numStr);
        }
    }

    private static boolean isPalindrome(String value) {
        int left = 0;
        int right = value.length() - 1;
        while (left < right) {
            if (value.charAt(left) != value.charAt(right)) return false;
            left++;
            right--;
        }
        return true;
    }

    /**
     * Rearranges {@code digits} in place into the next lexicographically greater
     * arrangement (standard "next permutation" algorithm). Returns false if
     * {@code digits} is already the maximum arrangement of its own characters.
     */
    private static boolean nextPermutationInPlace(char[] digits) {
        int n = digits.length;

        // Step 1: find the rightmost index `pivot` where digits[pivot] < digits[pivot+1].
        // This is the rightmost position we can still increase to get something bigger.
        int pivot = n - 2;
        while (pivot >= 0 && digits[pivot] >= digits[pivot + 1]) {
            pivot--;
        }
        if (pivot < 0) {
            // Entire array is non-increasing: this IS the largest arrangement possible.
            return false;
        }

        // Step 2: find the rightmost index `successor` right of pivot whose digit is
        // strictly greater than digits[pivot]. Because the suffix [pivot+1, n-1] is
        // non-increasing, this finds the smallest digit in that suffix that still
        // exceeds digits[pivot] — the minimal legal increase.
        int successor = n - 1;
        while (digits[successor] <= digits[pivot]) {
            successor--;
        }

        // Step 3: swap in that just-slightly-larger digit at the pivot position.
        swap(digits, pivot, successor);

        // Step 4: the suffix after pivot is still non-increasing post-swap; reverse it
        // to make it non-decreasing (smallest possible ordering), guaranteeing the
        // overall result is the SMALLEST arrangement greater than the original.
        reverse(digits, pivot + 1, n - 1);

        return true;
    }

    private static void swap(char[] array, int i, int j) {
        char temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    private static void reverse(char[] array, int from, int to) {
        while (from < to) {
            swap(array, from, to);
            from++;
            to--;
        }
    }

    private static String assemblePalindrome(char[] newLeftHalf, String originalNumStr,
                                              int halfLength, boolean hasMiddleDigit) {
        StringBuilder palindromeBuilder = new StringBuilder(originalNumStr.length());
        palindromeBuilder.append(newLeftHalf);
        if (hasMiddleDigit) {
            // The middle digit's identity/parity is untouched — it never participates
            // in the permutation and never moves position.
            palindromeBuilder.append(originalNumStr.charAt(halfLength));
        }
        for (int index = newLeftHalf.length - 1; index >= 0; index--) {
            palindromeBuilder.append(newLeftHalf[index]);
        }
        return palindromeBuilder.toString();
    }
}
