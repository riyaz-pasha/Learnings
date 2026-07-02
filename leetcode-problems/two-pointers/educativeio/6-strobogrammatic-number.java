import java.util.*;

/**
 * ============================================================================
 * STROBOGRAMMATIC NUMBER — MOCK GOOGLE INTERVIEW WALKTHROUGH
 * ============================================================================
 * This single file simulates exactly how I (as a senior Google engineer)
 * would run this problem end-to-end in a real onsite/phone interview:
 * understanding, clarifying, examples, brute force -> optimal, complexity
 * analysis, trade-offs, a polished final solution, a dry run, and follow-ups.
 * ============================================================================
 */
public class StrobogrammaticNumber {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     * In my own words:
     *
     *   We're given a string `num` that represents an integer (digits only,
     *   no sign, no leading zeros unless the number is literally "0"). We
     *   need to determine whether the number looks identical when the
     *   string is rotated 180 degrees — i.e., flipped upside down, as if
     *   you rotated a piece of paper with the digits written on it.
     *
     *   Only certain digits survive a 180-degree rotation and still look
     *   like valid digits:
     *     0 -> 0
     *     1 -> 1
     *     6 -> 9
     *     8 -> 8
     *     9 -> 6
     *   All other digits (2, 3, 4, 5, 7) become garbage/invalid when
     *   rotated, so any string containing them can never be strobogrammatic.
     *
     *   Rotating the whole STRING 180 degrees does two things simultaneously:
     *     (a) reverses the order of the characters (what was last is now
     *         first, because the paper is spun around), and
     *     (b) maps each individual character to its rotated counterpart.
     *
     *   So the number is strobogrammatic if and only if, reading the string
     *   from both ends inward, each pair of mirrored characters maps to
     *   each other correctly under the rotation table above (and the middle
     *   character, if the length is odd, must be self-symmetric: 0, 1, or 8).
     *
     * Inputs:
     *   - A single string `num`, 1 <= num.length() <= 50, digits only.
     *
     * Outputs:
     *   - boolean: true if strobogrammatic, false otherwise.
     *
     * Explicit assumptions:
     *   - No sign characters ('+'/'-') are present.
     *   - No leading zeros unless the number is exactly "0".
     *   - We do not need to interpret the numeric VALUE at all — this is a
     *     pure string/character-mapping problem.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ========================================================================
     * 1. Q: Can `num` contain a negative sign or a decimal point?
     *    A (assumed): No — problem guarantees digits only, so no sign or
     *       decimal handling is needed.
     *
     * 2. Q: Is the empty string a valid input?
     *    A (assumed): No — constraints guarantee length >= 1.
     *
     * 3. Q: Can the input contain leading zeros (e.g., "00")?
     *    A (assumed): No, except when the number itself is "0". So "00" is
     *       not a valid input per constraints, but I will still write code
     *       that behaves sensibly if it appeared (it would just evaluate
     *       normally as a character-mapping check).
     *
     * 4. Q: What's the max length, and does that affect algorithm choice?
     *    A (assumed): Max length is 50 — trivially small. Any O(n) or even
     *       O(n^2) approach is fast enough; this constraint mainly signals
     *       "don't overthink performance, focus on correctness/clarity."
     *
     * 5. Q: Should the solution treat this as a string problem or convert to
     *    a numeric type (int/long/BigInteger) at any point?
     *    A (assumed): Pure string problem. Converting to a number is
     *       unnecessary, lossy for leading zero edge cases, and could
     *       overflow for 50-digit numbers without BigInteger — so we stay
     *       in string-land entirely.
     *
     * 6. Q: Is the check case-sensitive / could letters appear (like a hex
     *    string)?
     *    A (assumed): No — input is guaranteed digits only (0-9).
     *
     * 7. Q: Do we need to support Unicode full-width digits or other digit
     *    representations?
     *    A (assumed): No — standard ASCII '0'-'9' only.
     *
     * 8. Q: Is this a single query, or will this function be called
     *    repeatedly on many numbers (i.e., should we optimize for repeated
     *    calls, such as precomputing a lookup structure)?
     *    A (assumed): Single query per call; no shared state needed across
     *       calls, though I'll use a static/final mapping table so repeated
     *       calls are cheap anyway.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     * Example 1 (Normal case — TRUE):
     *   num = "69"
     *   Rotate 180: '6' at front maps to '9', '9' at back maps to '6',
     *   and the string reverses order -> "69" becomes "69" again. TRUE.
     *
     * Example 2 (Normal case — FALSE):
     *   num = "962"
     *   '2' has no valid rotation (2 is not in {0,1,6,8,9}) -> immediately
     *   FALSE regardless of anything else.
     *
     * Example 3 (Edge case — single character):
     *   num = "8"
     *   Single self-symmetric digit, rotates to itself. TRUE.
     *   Contrast: num = "6" -> rotates to "9", not equal to "6". FALSE,
     *   because a lone 6 or 9 is NOT strobogrammatic by itself (it needs a
     *   partner on the other side to complete the swap).
     *
     * Example 4 (Boundary / tie-breaking case — odd length middle digit):
     *   num = "689"
     *   Length 3. Outer pair: '6' (front) and '9' (back) must map to each
     *   other: rotate('6') = '9' == num[2] ✓, rotate('9') = '6' == num[0] ✓.
     *   Middle digit '8' must map to itself: rotate('8') = '8' ✓.
     *   Result: TRUE.
     *   Contrast: num = "68" (even length) — rotate('6')='9' != num[1]='8'.
     *   FALSE. This shows why the middle-character rule only applies to
     *   ODD length strings, and is a common off-by-one trap.
     *
     * Example 5 (Edge case — zero):
     *   num = "0"
     *   Self-symmetric. TRUE.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * ========================================================================
     * Paradigm applicability note up front:
     *   - Sorting-based: NOT applicable — order of characters matters
     *     (it's a positional/mirroring check, not a multiset check).
     *   - Two pointer: APPLICABLE — this is the optimal approach.
     *   - Hashing-based: APPLICABLE — used for the digit-rotation lookup
     *     table (a HashMap), combined with either brute force or two
     *     pointer.
     *   - Divide & conquer / recursion: APPLICABLE — natural recursive
     *     formulation (peel off outer pair, recurse on the inner
     *     substring). Essentially two-pointer expressed recursively.
     *   - Greedy: Arguably the two-pointer scan IS a greedy left-to-right
     *     validation, but it doesn't fit the classic "greedy choice with
     *     exchange argument" mold well enough to list separately — I fold
     *     it into two-pointer.
     *   - Dynamic Programming: NOT applicable — there's no overlapping
     *     subproblem or optimal-substructure optimization target; each
     *     position's validity is an independent, deterministic O(1) check,
     *     not something that benefits from memoization.
     *   - Tree / graph traversal: NOT applicable — no branching decisions,
     *     no graph structure to explore.
     *   - Heap / priority queue: NOT applicable — no notion of ordering by
     *     priority/min/max is involved.
     *   - Binary search: NOT applicable — there's no monotonic search space
     *     to binary search over.
     *   - Monotonic stack / deque: NOT applicable — no "next greater
     *     element"-style windowed comparison; a simple two-pointer scan
     *     already gives O(1) space without needing a stack.
     *   - Trie / segment tree: NOT applicable — no prefix-sharing or range
     *     query structure needed; overkill for a fixed small-length string
     *     check.
     * ========================================================================
     */

    // ------------------------------------------------------------------------
    // Shared rotation lookup table used by multiple approaches below.
    // Maps a digit character to what it becomes after a 180-degree rotation.
    // Digits 2,3,4,5,7 are intentionally absent -> map lookups for them will
    // return null / not-found, signaling "cannot be strobogrammatic."
    // ------------------------------------------------------------------------
    private static final Map<Character, Character> ROTATION_MAP = Map.of(
            '0', '0',
            '1', '1',
            '6', '9',
            '8', '8',
            '9', '6'
    );

    /*
     * ------------------------------------------------------------------------
     * Approach 1: Brute Force — Build the Fully Rotated String, Then Compare
     * ------------------------------------------------------------------------
     * Core idea (plain English):
     *   Simulate the rotation literally: walk the string from the END to
     *   the START (because rotating 180 degrees reverses order), translate
     *   each character using the rotation map, and append it to a result
     *   buffer. If ANY character has no valid rotation, fail immediately.
     *   Finally, compare the fully constructed "rotated string" to the
     *   original string.
     *
     * Data structure / paradigm:
     *   Hashing (map lookup) + StringBuilder construction. This is the most
     *   literal, "just simulate the definition" approach.
     *
     * Time Complexity: O(n) — one pass to build the rotated string, one
     *   O(n) string comparison at the end. So O(n) total, but with higher
     *   constant factor than two-pointer due to building a whole new string.
     * Space Complexity: O(n) — the StringBuilder holds a full copy of the
     *   rotated string.
     *
     * Pros:
     *   - Extremely easy to reason about / obviously correct — it directly
     *     mirrors the problem definition, which is great for explaining to
     *     an interviewer or for a first correctness pass.
     *   - Easy to unit test in isolation (you can inspect the intermediate
     *     rotated string).
     * Cons:
     *   - Wastes O(n) extra space building a string we don't strictly need.
     *   - Slightly more work than necessary (full construction + full
     *     comparison) when we could bail out early and use O(1) space.
     *
     * When to use in practice:
     *   Good as a warm-up / first-draft solution to show the interviewer
     *   you understand the definition, or if downstream code actually
     *   needs the rotated string itself (not just a boolean). Not what
     *   I'd ship as the final optimized answer.
     * ------------------------------------------------------------------------
     */
    public static boolean isStrobogrammaticBruteForce(String num) {
        int length = num.length();
        StringBuilder rotated = new StringBuilder();

        // Walk from the last character to the first, translating each one.
        for (int index = length - 1; index >= 0; index--) {
            char originalChar = num.charAt(index);
            Character rotatedChar = ROTATION_MAP.get(originalChar);

            // If this digit has no valid 180-degree rotation, we can stop.
            if (rotatedChar == null) {
                return false;
            }
            rotated.append(rotatedChar);
        }

        // The number is strobogrammatic iff the rotated string equals the original.
        return rotated.toString().equals(num);
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 2: Two-Pointer Scan (Optimal)
     * ------------------------------------------------------------------------
     * Core idea (plain English):
     *   We never actually need to build the rotated string. We only need
     *   to check, for every mirrored pair of positions (left, right =
     *   n-1-left), whether rotating the left character produces the right
     *   character (and vice versa is implied by symmetry of the map). We
     *   walk two pointers inward from both ends simultaneously. If the
     *   string has odd length, the single middle character must rotate to
     *   itself (must be 0, 1, or 8).
     *
     * Data structure / paradigm:
     *   Two pointers + hashing (map lookup for O(1) rotation checks).
     *
     * Time Complexity: O(n) — each pointer traverses at most n/2 positions,
     *   each with an O(1) map lookup.
     * Space Complexity: O(1) — no extra data structures scale with input
     *   size (the rotation map is a fixed-size constant of 5 entries).
     *
     * Pros:
     *   - Optimal in both time and space.
     *   - Fails fast — exits on the very first mismatched pair.
     *   - Clean, iterative, no recursion overhead / call stack risk.
     * Cons:
     *   - Marginally less "obviously correct at a glance" than the brute
     *     force version, since it's checking pairs rather than literally
     *     constructing the rotation — worth a one-line comment to clarify.
     *
     * When to use in practice:
     *   This is the version I would actually write and submit in an
     *   interview or in production code — it's simple, optimal, and easy
     *   to explain in under a minute.
     * ------------------------------------------------------------------------
     */
    public static boolean isStrobogrammaticTwoPointer(String num) {
        int leftIndex = 0;
        int rightIndex = num.length() - 1;

        while (leftIndex <= rightIndex) {
            char leftChar = num.charAt(leftIndex);
            char rightChar = num.charAt(rightIndex);

            Character rotatedLeft = ROTATION_MAP.get(leftChar);

            // leftChar itself isn't a rotatable digit (2,3,4,5,7) -> immediate fail.
            if (rotatedLeft == null) {
                return false;
            }

            // The rotation of the left character must equal the right character.
            // This single check correctly also covers the middle character case
            // when leftIndex == rightIndex, since rotatedLeft must then equal
            // leftChar itself (only true for 0, 1, 8).
            if (rotatedLeft != rightChar) {
                return false;
            }

            leftIndex++;
            rightIndex--;
        }

        return true;
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 3: Recursive / Divide-and-Conquer Formulation
     * ------------------------------------------------------------------------
     * Core idea (plain English):
     *   Express the exact same two-pointer logic recursively: the string
     *   is strobogrammatic if its outermost pair of characters mirror
     *   correctly AND the inner substring (with that pair removed) is
     *   also strobogrammatic. Base cases: an empty string (even length
     *   fully consumed) or a single self-symmetric character (odd length
     *   middle) are trivially strobogrammatic.
     *
     * Data structure / paradigm:
     *   Divide and conquer / recursion + hashing for the lookup.
     *
     * Time Complexity: O(n) — n/2 recursive calls, each doing O(1) work
     *   (aside from substring creation).
     * Space Complexity: O(n) in practice — Java's String.substring (post
     *   Java 7) copies character data, so each recursive call allocates a
     *   new smaller string; that's O(n) cumulative extra space, plus O(n)
     *   recursion call-stack depth in the worst case.
     *
     * Pros:
     *   - Elegant, reads almost like the mathematical definition
     *     ("mirror match + recurse on the middle").
     *   - Nice to mention verbally to show you can see the recursive
     *     structure, even if you implement the iterative version.
     * Cons:
     *   - Strictly worse than Approach 2 in space complexity (substring
     *     allocations + call stack) for zero benefit in return.
     *   - Recursion depth up to 25 for n=50 is totally fine here, but this
     *     pattern would be risky to default to on much larger inputs.
     *
     * When to use in practice:
     *   Mostly pedagogical / to demonstrate you understand multiple
     *   framings. I would mention this approach verbally but would NOT
     *   write it as my primary submitted solution given Approach 2 is
     *   strictly better.
     * ------------------------------------------------------------------------
     */
    public static boolean isStrobogrammaticRecursive(String num) {
        return isStrobogrammaticRecursiveHelper(num, 0, num.length() - 1);
    }

    private static boolean isStrobogrammaticRecursiveHelper(String num, int leftIndex, int rightIndex) {
        // Base case: pointers crossed -> every pair matched successfully.
        if (leftIndex > rightIndex) {
            return true;
        }

        char leftChar = num.charAt(leftIndex);
        Character rotatedLeft = ROTATION_MAP.get(leftChar);

        if (rotatedLeft == null) {
            return false;
        }

        // Base case (odd-length middle): leftIndex == rightIndex means this is
        // the single center character, which must rotate to itself.
        if (rotatedLeft != num.charAt(rightIndex)) {
            return false;
        }

        // Recurse inward on the remaining substring between the two pointers.
        return isStrobogrammaticRecursiveHelper(num, leftIndex + 1, rightIndex - 1);
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 4 (Rejected / Discussed-Only): Sort-and-Compare
     * ------------------------------------------------------------------------
     * Why I'm not implementing this as real code:
     *   Sorting is fundamentally the wrong tool here because strobogrammatic-
     *   ness depends on POSITIONAL mirroring (character at index i pairs
     *   with character at index n-1-i), not on the multiset of characters
     *   present. For example, "1689" and "6891" contain the same sorted
     *   digits but are not equivalent under this problem's rules. Sorting
     *   would destroy exactly the positional information we need, so this
     *   approach is a dead end. I mention it only to show it was considered
     *   and consciously ruled out.
     * ------------------------------------------------------------------------
     */

    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                        | Time  | Space | Best For                          | Limitations                                  |
     * |----------------------------------|-------|-------|-----------------------------------|-----------------------------------------------|
     * | 1. Brute Force (build rotation)  | O(n)  | O(n)  | Demonstrating the definition       | Extra allocation not needed for a boolean out |
     * | 2. Two-Pointer (Optimal)         | O(n)  | O(1)  | Production / interview submission  | Slightly less "literal" than brute force      |
     * | 3. Recursive Divide & Conquer    | O(n)  | O(n)  | Showing alternate framing verbally | Substring copies + call stack overhead        |
     * | 4. Sort-and-Compare              | N/A   | N/A   | Nothing — conceptually invalid     | Destroys positional info; incorrect approach  |
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
     * ========================================================================
     * I would present Approach 2 (Two-Pointer Scan) as my final solution:
     *
     *   - Clarity: The mirrored-pair logic is easy to explain out loud in
     *     one or two sentences and easy for the interviewer to follow live.
     *   - Coding speed: It's about 15 lines, no helper recursion, low risk
     *     of bugs — I can write and verify it within a few minutes.
     *   - Interviewer expectations: For a problem explicitly tagged "easy"
     *     with n <= 50, interviewers expect a clean O(n) time, O(1) extra
     *     space solution with correct handling of the odd-length middle
     *     character — this hits all of those marks.
     *   - Optimality: O(n) time / O(1) space is asymptotically optimal —
     *     you cannot do better than reading each character once, since
     *     every character must be inspected to confirm correctness.
     *
     * I would mention Approach 1 briefly as "the direct simulation" to show
     * I understand the problem, then pivot to Approach 2 as the refined,
     * production-quality version.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE — POLISHED, PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ========================================================================
     */
    public static boolean isStrobogrammatic(String num) {
        // Defensive check: per constraints this shouldn't happen, but a
        // production-quality method should not silently misbehave on null.
        Objects.requireNonNull(num, "num must not be null");

        // Two pointers converging from opposite ends of the string.
        int leftIndex = 0;
        int rightIndex = num.length() - 1;

        // We only need to walk halfway (leftIndex <= rightIndex covers the
        // exact center character too, when the length is odd).
        while (leftIndex <= rightIndex) {
            char leftChar = num.charAt(leftIndex);

            // Look up what leftChar becomes after a 180-degree rotation.
            // A null result means leftChar is one of {2,3,4,5,7}, which has
            // no valid rotated form at all -> automatic failure.
            Character rotatedLeftChar = ROTATION_MAP.get(leftChar);
            if (rotatedLeftChar == null) {
                return false;
            }

            // For the string to look identical after rotation, the rotated
            // version of the LEFT character must equal the character
            // currently sitting at the mirrored RIGHT position.
            //
            // Special case handled for free: when leftIndex == rightIndex
            // (odd-length middle character), this check becomes
            // "rotatedLeftChar == leftChar itself", which is only true for
            // 0, 1, and 8 — exactly the self-symmetric digits we want.
            if (rotatedLeftChar != num.charAt(rightIndex)) {
                return false;
            }

            // Move both pointers one step inward toward the center.
            leftIndex++;
            rightIndex--;
        }

        // Every mirrored pair (and the middle character, if any) validated successfully.
        return true;
    }

    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     * Tracing isStrobogrammatic("1689") step by step.
     *
     * Initial state: num = "1689", leftIndex = 0, rightIndex = 3
     *
     * Iteration 1:
     *   leftChar        = num.charAt(0) = '1'
     *   rotatedLeftChar = ROTATION_MAP.get('1') = '1'
     *   num.charAt(rightIndex=3) = '9'
     *   Check: rotatedLeftChar ('1') != rightChar ('9') -> TRUE (they differ)
     *   -> return false immediately.
     *
     * Result: isStrobogrammatic("1689") = false.
     * (Makes sense: rotating "1689" 180 degrees actually produces "6891",
     *  not "1689", so they are not equal -> correctly false.)
     *
     * ------------------------------------------------------------------------
     * Second trace, a TRUE case: isStrobogrammatic("886")... wait, let's
     * trace a genuinely strobogrammatic 4-digit example instead: "8008".
     *
     * Initial state: num = "8008", leftIndex = 0, rightIndex = 3
     *
     * Iteration 1:
     *   leftChar = '8', rotatedLeftChar = '8'
     *   num.charAt(3) = '8'
     *   Check: '8' == '8' -> OK, no early return.
     *   leftIndex becomes 1, rightIndex becomes 2.
     *
     * Iteration 2:
     *   leftChar = num.charAt(1) = '0', rotatedLeftChar = '0'
     *   num.charAt(rightIndex=2) = '0'
     *   Check: '0' == '0' -> OK, no early return.
     *   leftIndex becomes 2, rightIndex becomes 1.
     *
     * Loop condition check: leftIndex(2) <= rightIndex(1)? -> FALSE.
     * Loop exits.
     *
     * Result: isStrobogrammatic("8008") = true.
     * (Correct: "8008" rotated 180 degrees reads as "8008".)
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     * - All viable approaches run in O(n) time; they differ mainly in
     *   space usage and elegance.
     * - The two-pointer scan (Approach 2 / Section 9) is the recommended
     *   final answer: O(n) time, O(1) extra space, fails fast, and its
     *   correctness follows directly from the problem's mirror-symmetry
     *   definition.
     * - The brute-force "build the rotated string" approach is a fine
     *   stepping stone for explaining intuition but carries unnecessary
     *   O(n) space.
     * - The recursive formulation is equivalent in logic to two-pointer but
     *   strictly worse in space due to substring allocation and call stack.
     * - Sorting-based, DP, graph/tree, heap, binary search, and
     *   trie/segment-tree paradigms are all inapplicable to this problem's
     *   structure, as explained in Section 4/5.
     * - Known assumptions/limitations of the final solution: it assumes
     *   `num` is non-null and contains only ASCII digit characters, per
     *   the stated constraints; it does not validate the "no leading
     *   zeros" constraint itself since that's a property of valid input,
     *   not something this function needs to enforce.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     * 1. "Can you generate ALL strobogrammatic numbers of a given length n?"
     *    (This is LeetCode 247 — a natural recursive/backtracking extension
     *    building strings from the outside in, choosing valid digit pairs.)
     *
     * 2. "Can you count how many strobogrammatic numbers exist within a
     *    numeric range [low, high]?" (LeetCode 248 — combines generation
     *    with range/length filtering and possibly digit-DP for efficiency
     *    at scale.)
     *
     * 3. "What if the input could be extremely large — say, up to 10^7
     *    digits? Does your solution still hold up?" (Yes — still O(n)
     *    time, O(1) space; discuss avoiding unnecessary allocations, and
     *    potentially streaming input instead of loading it fully.)
     *
     * 4. "What if this function will be called millions of times on
     *    different numbers — how would you optimize for throughput?"
     *    (Discuss avoiding boxed Character lookups — e.g., replace the
     *    HashMap<Character,Character> with a fixed-size int[] or char[]
     *    array indexed by (digit - '0') for branch-free, allocation-free
     *    O(1) lookups; discuss JIT warmup, avoiding autoboxing.)
     *
     * 5. "How would you handle concurrent calls from multiple threads?"
     *    (The method is already thread-safe as written — no shared mutable
     *    state; ROTATION_MAP is an immutable, effectively-final Map.of(),
     *    and each call only touches local variables and its own input.)
     *
     * 6. "What if we also had to support other bases (e.g., hexadecimal
     *    digits) or other rotation alphabets (e.g., letters that look the
     *    same upside down)?" (Discuss generalizing ROTATION_MAP to be an
     *    injected/parameterized lookup table rather than hardcoded digits,
     *    turning this into a general "rotationally symmetric under a given
     *    mapping" checker.)
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     * 1. Forgetting the odd-length middle character must be SELF-symmetric
     *    (only 0, 1, or 8 are valid alone) — candidates often only check
     *    pairs and forget to validate a lone middle digit, incorrectly
     *    accepting things like "6" or "9" as strobogrammatic on their own,
     *    or mishandling the middle index in an odd-length string.
     *
     * 2. Off-by-one errors in the two-pointer loop condition — using
     *    `leftIndex < rightIndex` instead of `leftIndex <= rightIndex`
     *    silently skips validating the middle character in odd-length
     *    strings, which is a subtle bug that only manifests on odd inputs.
     *
     * 3. Confusing "reversing the string" with "rotating the string" —
     *    candidates sometimes just reverse the string and compare it to
     *    the original (that would test for a PALINDROME, not a
     *    strobogrammatic number). For example, "11" is a palindrome AND
     *    strobogrammatic, but "69" is strobogrammatic and NOT a palindrome
     *    ("69" reversed is "96", which is wrong; the correct rotation
     *    check requires mapping 6->9 and 9->6, not merely reversing).
     *
     * 4. Forgetting that digits 2, 3, 4, 5, 7 have NO valid rotation at
     *    all — some candidates only build a mapping for the 5 valid
     *    digits and forget to explicitly handle/reject the other 5,
     *    leading to a NullPointerException on unboxing or a silently
     *    incorrect true/false result if the map lookup isn't null-checked.
     * ========================================================================
     */

    /*
     * ========================================================================
     * MAIN METHOD — Simple demonstration / sanity checks for all approaches.
     * ========================================================================
     */
    public static void main(String[] args) {
        String[] testCases = {
                "69",     // true
                "962",    // false
                "8",      // true
                "6",      // false
                "689",    // true
                "68",     // false
                "0",      // true
                "1689",   // false
                "8008"    // true
        };

        for (String testCase : testCases) {
            boolean bruteForceResult = isStrobogrammaticBruteForce(testCase);
            boolean twoPointerResult = isStrobogrammaticTwoPointer(testCase);
            boolean recursiveResult = isStrobogrammaticRecursive(testCase);
            boolean finalResult = isStrobogrammatic(testCase);

            System.out.printf(
                    "num=%-6s | bruteForce=%-5s | twoPointer=%-5s | recursive=%-5s | final=%-5s%n",
                    testCase, bruteForceResult, twoPointerResult, recursiveResult, finalResult
            );

            // Sanity check: all four approaches must always agree.
            assert bruteForceResult == twoPointerResult;
            assert twoPointerResult == recursiveResult;
            assert recursiveResult == finalResult;
        }
    }
}
