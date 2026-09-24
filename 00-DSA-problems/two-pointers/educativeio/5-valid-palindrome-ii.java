/*
================================================================================
 GOOGLE-STYLE MOCK INTERVIEW TRANSCRIPT
 Problem: "Valid Palindrome II"
 Candidate Prep File — Single compilable Java source
================================================================================
*/

import java.util.*;

public class ValidPalindromeII {

    /*
    ============================================================================
    SECTION 1: RESTATE THE PROBLEM
    ============================================================================
    In my own words:
        Given a string `s` consisting only of English letters, determine
        whether `s` can become a palindrome if we are allowed to delete
        AT MOST one character from it (deleting zero characters is also
        allowed — i.e., the string may already be a palindrome).

    Key constraints:
        - 1 <= s.length() <= 10^5
        - s contains only English letters (no digits, spaces, punctuation).
        - We may remove 0 or 1 characters (never more).

    Input:
        - A single String `s`.

    Output:
        - A boolean: true if `s` is a palindrome, or can be made one by
          removing exactly one character; false otherwise.

    Assumptions to be confirmed with interviewer (see Section 2):
        - Comparison is case-sensitive unless told otherwise.
        - We do not need to report WHICH character was removed — only
          whether it's possible.
        - String is immutable input; we should not mutate it in place.
    ============================================================================
    */

    /*
    ============================================================================
    SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
    ============================================================================
    1. Q: Is the comparison case-sensitive (e.g., is "Aa" a palindrome)?
       A (assumed): Yes, case-sensitive. Treat 'A' and 'a' as different chars,
          since the problem doesn't mention case-folding.

    2. Q: Can the string be empty?
       A (assumed): No — constraint guarantees length >= 1.

    3. Q: Do we need to return the resulting palindrome string, or the index
          removed, or just a boolean?
       A (assumed): Just a boolean — "can it be done," not "how."

    4. Q: Is "at most one" inclusive of removing zero characters (i.e., is an
          already-valid palindrome a valid answer)?
       A (assumed): Yes, "at most one" includes the zero-removal case.

    5. Q: Are there non-letter characters (spaces, punctuation, digits) we
          need to filter out first?
       A (assumed): No — constraint guarantees only English letters, so no
          pre-cleaning/normalization step is required.

    6. Q: What's the expected input scale, and do we need better than O(n^2)?
       A (assumed): n up to 10^5, so O(n^2) (10^10 ops) is too slow in the
          worst case — an O(n) or O(n log n) solution is expected for the
          "optimal" bar, though we should still discuss brute force first.

    7. Q: Is this a single call, or will this function be invoked repeatedly
          on many strings (should we worry about amortized/repeated calls,
          thread-safety, etc.)?
       A (assumed): Single, stateless call — no concurrency concerns.

    8. Q: Should we handle Unicode / multi-byte characters?
       A (assumed): No — "English letters" implies plain ASCII A-Z/a-z.
    ============================================================================
    */

    /*
    ============================================================================
    SECTION 3: EXAMPLES & EDGE CASES
    ============================================================================
    Example 1 (Normal case):
        Input:  "abca"
        Trace:  pointers meet mismatch at s[1]='b' vs s[2]='c'.
                Try skipping left ('b') -> "aca" -> palindrome. TRUE.
        Output: true

    Example 2 (Edge case — trivial/minimum length):
        Input:  "a"
        Trace:  single character is trivially a palindrome, 0 removals needed.
        Output: true

    Example 3 (Boundary / tie-breaking case — mismatch requires checking
               BOTH branches, and only one of them works):
        Input:  "cbbcc"
        Trace:  left=0('c'), right=4('c') match.
                left=1('b'), right=3('c') MISMATCH.
                Branch A: skip s[left]  -> check "bcc"[1..3]="bc"? not pal.
                          Actually check substring s[2..3] = "bc" -> not pal.
                Branch B: skip s[right] -> check substring s[1..2] = "bb" -> palindrome!
                Since Branch B succeeds, overall answer is TRUE.
                This demonstrates why BOTH branches must be checked at the
                first mismatch — checking only one branch would wrongly
                return false in some inputs (a classic bug).

    Additional edge cases worth mentioning aloud in the interview:
        - Two-character strings ("ab") -> always true (remove either char).
        - Strings with no valid single-removal fix, e.g. "abc" -> false.
        - Strings that are already palindromes with even/odd length,
          e.g. "abba", "aba" -> true with zero removals.
    ============================================================================
    */

    /*
    ============================================================================
    SECTION 4 & 5: ALL POSSIBLE APPROACHES
    ============================================================================

    Paradigms considered but NOT applicable (stated up front per interview
    best practice — shows breadth of thought without wasting time coding
    irrelevant ideas):
        - Sorting-based:      Sorting destroys the positional/order information
                               that palindrome-checking fundamentally depends on.
        - Hashing-based:       No natural key/value grouping helps here; we need
                               order-sensitive comparison, not membership checks.
        - Heap / Priority Queue: No ordering-by-priority concept applies.
        - Binary Search:       No sorted/monotonic search space to exploit.
        - Monotonic Stack/Deque: No "next greater/smaller" style relationship
                               exists in this problem.
        - Trie / Segment Tree: No prefix-sharing or range-query structure needed.
        - Graph traversal:     No graph structure is naturally present.
    These are skipped below; only genuinely applicable paradigms are coded.
    ============================================================================
    */

    /*
    ----------------------------------------------------------------------------
    Approach 1: Brute Force — "Try Removing Each Character"
    ----------------------------------------------------------------------------
    Core idea:
        For every index i in the string, build a new string with the
        character at index i removed, and check whether that new string
        is a palindrome. If ANY such removal yields a palindrome (or the
        original string is already one), return true.

    Data structure / paradigm:
        Plain iteration + string manipulation (no special structure).

    Time Complexity:  O(n^2)
        - n choices of index to remove.
        - Each removal + palindrome check costs O(n).
    Space Complexity: O(n)
        - Each candidate string built is O(n); not counting original input.

    Pros:
        - Extremely easy to reason about and verify correctness.
        - Good warm-up / sanity-check baseline before optimizing.
    Cons:
        - O(n^2) time is far too slow for n = 10^5 (~10^10 operations).
        - Excess string allocations cause GC pressure in Java.

    When to use:
        - Only for very small inputs, or as a first pass to validate the
          optimal solution's correctness in unit tests.
    ----------------------------------------------------------------------------
    */
    public static boolean isValidPalindromeBruteForce(String s) {
        // Try the "no removal" case first.
        if (isPalindromeRange(s, 0, s.length() - 1)) {
            return true;
        }
        // Try removing each index one at a time.
        for (int removeIndex = 0; removeIndex < s.length(); removeIndex++) {
            StringBuilder candidateBuilder = new StringBuilder(s);
            candidateBuilder.deleteCharAt(removeIndex);
            String candidate = candidateBuilder.toString();
            if (isPalindromeRange(candidate, 0, candidate.length() - 1)) {
                return true;
            }
        }
        return false;
    }

    /*
    ----------------------------------------------------------------------------
    Approach 2: Dynamic Programming — "Longest Palindromic Subsequence (LPS)"
    ----------------------------------------------------------------------------
    Core idea:
        A string can be made a palindrome by deleting exactly k characters
        if and only if its Longest Palindromic Subsequence (LPS) has length
        (n - k). So we can check: is LPS(s) >= n - 1? If the LPS covers all
        but at most one character, at most one deletion suffices.
        (Note: This is a slightly looser check than "delete one character
        results in a palindrome" — LPS-based deletion doesn't require the
        remaining characters stay contiguous in the same way, but for this
        specific problem the two notions coincide because deleting one
        character from a string of length n always leaves a subsequence of
        length n-1, and that subsequence being a palindrome is exactly what
        we test in Approach 3 as well.)

    Data structure / paradigm:
        Classic interval DP, same recurrence as Longest Common Subsequence
        of s and reverse(s).

    Time Complexity:  O(n^2)  — filling an n x n DP table.
    Space Complexity: O(n^2)  — the DP table itself (can be reduced to O(n)
        with rolling arrays, but that obscures the classic presentation).

    Pros:
        - Generalizes naturally to "at most K removals" (just compare LPS
          length to n - K), which is a common interview follow-up.
        - Reinforces DP fundamentals — good if interviewer wants to see DP.
    Cons:
        - O(n^2) time AND O(n^2) space is unacceptable for n = 10^5
          (10^10 cells — will not fit in memory or time budget).
        - Overkill for a problem that has an O(n) two-pointer solution.

    When to use:
        - When the problem is generalized to "at most K deletions" for
          arbitrary K > 1, on small-to-medium n. Not appropriate here given
          the stated constraints, but valuable to mention for breadth.
    ----------------------------------------------------------------------------
    */
    public static boolean isValidPalindromeDP(String s) {
        int n = s.length();
        if (n <= 1) return true;

        String reversed = new StringBuilder(s).reverse().toString();

        // dp[i][j] = length of Longest Common Subsequence of s[0..i-1] and
        // reversed[0..j-1]. LCS(s, reverse(s)) == Longest Palindromic
        // Subsequence of s.
        int[][] dp = new int[n + 1][n + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                if (s.charAt(i - 1) == reversed.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        int longestPalindromicSubsequenceLength = dp[n][n];
        // We can afford to delete at most 1 character.
        return longestPalindromicSubsequenceLength >= n - 1;
    }

    /*
    ----------------------------------------------------------------------------
    Approach 3: Two Pointer Greedy — OPTIMAL SOLUTION
    ----------------------------------------------------------------------------
    Core idea:
        Walk inward from both ends with two pointers, `left` and `right`.
        As long as characters match, keep moving inward. The FIRST time we
        hit a mismatch, we have only two possible fixes: skip the left
        character, or skip the right character. Greedily try both remaining
        sub-ranges — if EITHER forms a palindrome, the answer is true.
        Because a real palindrome can have at most one mismatching pair
        before we're forced to use our one allowed deletion, we never need
        to branch more than once — giving linear time.

    Data structure / paradigm:
        Two-pointer / greedy scanning. No auxiliary structure needed.

    Time Complexity:  O(n)
        - The initial two-pointer walk is O(n).
        - On mismatch, we do at most two O(n) sub-checks, but this happens
          only once (we return immediately after), so total work is O(n).
    Space Complexity: O(1)
        - Only a few integer pointers; no extra string copies (we scan
          using indices into the original string via a helper that also
          takes only indices, not substrings).

    Pros:
        - Meets the O(n) time / O(1) space bar expected for this problem.
        - Simple to code correctly under interview time pressure once the
          "check both branches on first mismatch" insight is stated.
        - No extra memory allocation — ideal for n up to 10^5.
    Cons:
        - Slightly more subtle to get right than brute force (must remember
          to check BOTH skip-left and skip-right branches).
        - Generalizing to "at most K deletions" is not as natural as the DP
          approach — would need a different technique (e.g., DP or
          recursion with memoized deletion budget) for K > 1.

    When to use:
        - This is the production/interview-ready answer for exactly this
          problem statement ("at most one" removal). Use this.
    ----------------------------------------------------------------------------
    */
    public static boolean isValidPalindromeOptimal(String s) {
        int left = 0;
        int right = s.length() - 1;

        while (left < right) {
            if (s.charAt(left) == s.charAt(right)) {
                left++;
                right--;
            } else {
                // First mismatch: try skipping either the left or the right
                // character. If either resulting range is a palindrome,
                // one deletion suffices.
                boolean skipLeftWorks = isPalindromeRange(s, left + 1, right);
                boolean skipRightWorks = isPalindromeRange(s, left, right - 1);
                return skipLeftWorks || skipRightWorks;
            }
        }
        // No mismatch ever found -> already a palindrome, zero removals needed.
        return true;
    }

    /*
    ----------------------------------------------------------------------------
    Approach 4: Recursive / Divide-and-Conquer Variant (equivalent to
    Approach 3, presented recursively)
    ----------------------------------------------------------------------------
    Core idea:
        Same greedy insight as Approach 3, but expressed as a recursive
        helper that carries a "deletions remaining" budget. On mismatch, it
        recurses into both possible sub-problems (skip left / skip right)
        with the budget decremented by one. This is a natural way to present
        the "divide into sub-problems" mental model, and it generalizes
        cleanly to "at most K deletions" by changing the initial budget.

    Data structure / paradigm:
        Recursion / divide-and-conquer with an implicit call stack.

    Time Complexity:  O(n) for K=1 (branches only once, same reasoning as
        Approach 3). In general for budget K, worst case is O(n * 2^K),
        which is why this technique is only practical for small K.
    Space Complexity: O(n) worst case due to recursion call stack depth
        (each matching step recurses inward).

    Pros:
        - Elegant, generalizes to K > 1 deletions with a one-line change.
        - Clearly shows the "branch on mismatch" idea, useful for
          explaining Approach 3's correctness to an interviewer.
    Cons:
        - Recursion stack depth O(n) risks StackOverflowError in Java for
          n = 10^5 unless converted to iteration (tail-call elimination is
          NOT guaranteed by the JVM).
        - No practical benefit over Approach 3 for K=1; strictly worse on
          space.

    When to use:
        - Good for whiteboard explanation of the underlying idea, or if the
          interviewer pivots to "what if K deletions are allowed?" But for
          production code with n up to 10^5 and K=1, prefer Approach 3.
    ----------------------------------------------------------------------------
    */
    public static boolean isValidPalindromeRecursive(String s) {
        return canFormPalindrome(s, 0, s.length() - 1, 1);
    }

    private static boolean canFormPalindrome(String s, int left, int right, int deletionsRemaining) {
        while (left < right && s.charAt(left) == s.charAt(right)) {
            left++;
            right--;
        }
        if (left >= right) {
            return true; // Fully matched inward -> palindrome.
        }
        if (deletionsRemaining == 0) {
            return false; // Mismatch found but no budget left to fix it.
        }
        // Branch: try skipping either side, consuming one deletion.
        return canFormPalindrome(s, left + 1, right, deletionsRemaining - 1)
                || canFormPalindrome(s, left, right - 1, deletionsRemaining - 1);
    }

    /*
    ----------------------------------------------------------------------------
    Shared helper: checks if s[start..end] (inclusive) is a palindrome using
    O(1) extra space and O(range length) time. Used by Approaches 1, 3, 4.
    ----------------------------------------------------------------------------
    */
    private static boolean isPalindromeRange(String s, int start, int end) {
        while (start < end) {
            if (s.charAt(start) != s.charAt(end)) {
                return false;
            }
            start++;
            end--;
        }
        return true;
    }

    /*
    ============================================================================
    SECTION 7: APPROACHES COMPARISON TABLE
    ============================================================================

    | Approach                     | Time      | Space  | Best For                              | Limitations                                   |
    |-------------------------------|-----------|--------|----------------------------------------|------------------------------------------------|
    | 1. Brute Force                | O(n^2)    | O(n)   | Small n, correctness baseline/testing  | Too slow for n = 10^5                          |
    | 2. DP (LPS length)             | O(n^2)    | O(n^2) | Generalizing to "at most K" deletions  | O(n^2) space infeasible for n = 10^5; overkill |
    | 3. Two Pointer Greedy (OPTIMAL)| O(n)      | O(1)   | Exactly this problem (K = 1)           | Doesn't generalize cleanly to K > 1             |
    | 4. Recursive Divide & Conquer  | O(n)*     | O(n)   | Explaining the idea; K > 1 variants    | Recursion stack risk at n = 10^5; O(n) space    |

    * O(n) specifically for K=1; degrades to O(n * 2^K) for general K.
    ============================================================================
    */

    /*
    ============================================================================
    SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
    ============================================================================
    I would present Approach 3 (Two Pointer Greedy) as my final answer:

        - It hits the optimal O(n) time / O(1) space bar, which interviewers
          at Google explicitly look for once "brute force" has been
          verbally dismissed as too slow for n = 10^5.
        - It's fast to code correctly (roughly 10-12 lines), leaving time
          for testing and follow-up discussion.
        - The key insight ("only branch once, check both skip-left and
          skip-right") is easy to explain out loud and demonstrates clear
          algorithmic reasoning, which is exactly what's being evaluated.
        - I would mention the DP/LPS approach (Approach 2) verbally as a
          "generalizes better to K > 1" alternative to show breadth, without
          spending time coding it unless asked.
    ============================================================================
    */

    /*
    ============================================================================
    SECTION 9: DEEP DIVE — POLISHED OPTIMAL SOLUTION (PRODUCTION QUALITY)
    ============================================================================
    */

    /**
     * Determines whether {@code input} can be made into a palindrome by
     * deleting at most one character.
     *
     * <p>Algorithm: two-pointer scan from both ends toward the middle.
     * On the first character mismatch, at most one of the two characters
     * involved can be the "extra" one to delete — so we test both
     * possibilities against the remaining sub-range. If neither works,
     * no single deletion can fix the string.</p>
     *
     * @param input a non-null string of English letters, length in [1, 10^5]
     * @return true if {@code input} is already a palindrome, or becomes one
     *         after removing exactly one character; false otherwise
     * @throws IllegalArgumentException if {@code input} is null
     */
    public static boolean canFormPalindromeByRemovingAtMostOneChar(String input) {
        if (input == null) {
            // Defensive check: constraints guarantee a valid string, but
            // production code should never trust callers blindly.
            throw new IllegalArgumentException("Input string must not be null.");
        }

        int leftPointer = 0;
        int rightPointer = input.length() - 1;

        // Walk inward while characters match; this is the "free" portion
        // of the string that requires no deletion.
        while (leftPointer < rightPointer) {
            char leftChar = input.charAt(leftPointer);
            char rightChar = input.charAt(rightPointer);

            if (leftChar == rightChar) {
                leftPointer++;
                rightPointer--;
                continue;
            }

            // Mismatch found. We have exactly one deletion budget left,
            // so we must decide: drop leftChar, or drop rightChar?
            // There's no way to know in advance which is correct, so we
            // simply try both and short-circuit on success.
            boolean palindromeIfLeftCharRemoved =
                    isPalindromeRange(input, leftPointer + 1, rightPointer);
            boolean palindromeIfRightCharRemoved =
                    isPalindromeRange(input, leftPointer, rightPointer - 1);

            // This is the ONLY branching point in the whole algorithm,
            // which is precisely why the overall complexity stays O(n)
            // rather than exploding combinatorially.
            return palindromeIfLeftCharRemoved || palindromeIfRightCharRemoved;
        }

        // Pointers crossed or met without ever mismatching: the string was
        // already a palindrome, so zero deletions were needed.
        return true;
    }

    /*
    ============================================================================
    SECTION 10: DRY RUN / TRACE
    ============================================================================
    Tracing canFormPalindromeByRemovingAtMostOneChar("cbbcc"):

        Initial: leftPointer = 0, rightPointer = 4
        String indices:  c(0) b(1) b(2) c(3) c(4)

        Step 1: leftChar = s[0] = 'c', rightChar = s[4] = 'c' -> MATCH
                 leftPointer -> 1, rightPointer -> 3

        Step 2: leftChar = s[1] = 'b', rightChar = s[3] = 'c' -> MISMATCH
                 Branch A (drop leftChar 'b'):
                     check range [leftPointer+1, rightPointer] = [2, 3]
                     s[2..3] = "bc" -> 'b' != 'c' -> NOT a palindrome
                 Branch B (drop rightChar 'c'):
                     check range [leftPointer, rightPointer-1] = [1, 2]
                     s[1..2] = "bb" -> 'b' == 'b' -> IS a palindrome
                 palindromeIfLeftCharRemoved  = false
                 palindromeIfRightCharRemoved = true
                 return false || true = TRUE

        Final result: true
        (Matches Example 3 from Section 3 — dropping the 'c' at index 3
         yields "cbbc"... note: actually removing index 3 from "cbbcc"
         yields "cbbc" is NOT quite what we tested; what we verified is
         that the INNER range s[1..2]="bb" is a palindrome once we exclude
         s[3]. This correctly reflects that deleting index 3 leaves
         "c" + "bb" + "c" = "cbbc", which IS a palindrome. Trace confirmed.)
    ============================================================================
    */

    /*
    ============================================================================
    SECTION 11: CLOSING SUMMARY
    ============================================================================
        - Brute Force (O(n^2)) is correct but far too slow for n = 10^5;
          useful only as a testing oracle for small inputs.
        - DP/LPS (O(n^2) time & space) is elegant and generalizes to K > 1
          deletions, but its space usage is infeasible at this scale.
        - Two Pointer Greedy (O(n) time, O(1) space) is the right answer for
          this exact problem — it exploits the fact that K = 1 means we can
          never branch more than once.
        - Recursive Divide & Conquer is conceptually identical to the greedy
          approach but pays an O(n) stack-space cost; valuable mainly as a
          stepping stone toward K > 1 variants.

        Known limitations / assumptions of the final solution:
        - Assumes case-sensitive comparison (confirmed as an assumption in
          Section 2 — would need a `Character.toLowerCase()` normalization
          step if case-insensitivity is required).
        - Assumes input contains only English letters, per constraints — no
          whitespace/punctuation stripping is performed.
        - Assumes "at most one" includes zero removals (already-palindrome
          strings return true).
    ============================================================================
    */

    /*
    ============================================================================
    SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
    ============================================================================
    1. "What if we allow up to K deletions instead of just 1?"
       -> Pivot to the DP/LPS approach (Approach 2), or memoized recursion.

    2. "What if the string can contain spaces/punctuation that should be
        ignored when checking palindrome-ness (like the classic 'valid
        palindrome' problem)?"
       -> Add a normalization/filtering pass before the two-pointer scan.

    3. "What if we need to know WHICH character to remove, not just whether
        it's possible?"
       -> Return the index (leftPointer or rightPointer) at the branch
          point that succeeded, instead of a boolean.

    4. "How would you handle this if the string were streamed and we
        couldn't hold it all in memory / random-access it?"
       -> Discuss the need for a two-ended stream or reading the whole
          string first since palindrome checks inherently need both ends;
          possibly discuss a Deque-based streaming buffer.

    5. "Can you do this without any extra function calls / recursion, i.e.,
        fully iterative, for a language/runtime with strict stack limits?"
       -> Point out that Approach 3 already is fully iterative -- no
          recursion needed at all, unlike Approach 4.

    6. "What's the largest number of characters we might need to compare in
        the worst case, and can you prove the O(n) bound?"
       -> Walk through the argument that the two-pointer walk plus the
          single branch's two O(n) sub-scans sum to at most ~2n character
          comparisons total, i.e., O(n).
    ============================================================================
    */

    /*
    ============================================================================
    SECTION 13: WHAT CANDIDATES TYPICALLY MISS
    ============================================================================
    1. Checking only ONE branch on mismatch (e.g., always dropping the left
       character) instead of trying BOTH skip-left and skip-right. This is
       the single most common bug — see Example 3 ("cbbcc") where only the
       skip-right branch succeeds.

    2. Off-by-one errors in the sub-range bounds after a mismatch — it must
       be [left+1, right] and [left, right-1], NOT [left+1, right-1] (which
       would incorrectly skip two characters instead of one).

    3. Forgetting the "zero deletions" case — i.e., not returning true when
       the pointers cross without ever mismatching (the string was already
       a palindrome).

    4. Using String concatenation/substring() inside the two-pointer loop
       (e.g., building a new String for every comparison), which silently
       turns an intended O(n) solution into O(n^2) due to substring copy
       costs — always compare via charAt() on index ranges instead.
    ============================================================================
    */

    /*
    ============================================================================
    MAIN METHOD — Quick manual verification of all approaches on the
    examples from Section 3 (not a substitute for a real test framework,
    but demonstrates the code compiles and behaves as traced).
    ============================================================================
    */
    public static void main(String[] args) {
        String[] testInputs = {"abca", "a", "cbbcc", "ab", "abc", "abba"};
        boolean[] expected =   {true,   true, true,    true, false, true};

        for (int i = 0; i < testInputs.length; i++) {
            String input = testInputs[i];
            boolean bruteForceResult = isValidPalindromeBruteForce(input);
            boolean dpResult = isValidPalindromeDP(input);
            boolean optimalResult = isValidPalindromeOptimal(input);
            boolean recursiveResult = isValidPalindromeRecursive(input);
            boolean productionResult = canFormPalindromeByRemovingAtMostOneChar(input);

            System.out.printf(
                "Input=%-8s Expected=%-5s BruteForce=%-5s DP=%-5s Optimal=%-5s Recursive=%-5s Production=%-5s%n",
                input, expected[i], bruteForceResult, dpResult, optimalResult,
                recursiveResult, productionResult
            );
        }
    }
}
