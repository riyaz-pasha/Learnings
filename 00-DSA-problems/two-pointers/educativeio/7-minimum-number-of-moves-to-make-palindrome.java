/* ============================================================================================
 * FILE: PalindromeMinSwaps.java
 * CONTEXT: Mock Google onsite interview — Data Structures & Algorithms round.
 * CANDIDATE PRESENTATION FORMAT: Every stage of the interview conversation is captured as a
 * labeled block comment, in the exact order a senior engineer would walk through it live.
 * ============================================================================================ */

import java.util.*;

public class PalindromeMinSwaps {

    /* ========================================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ----------------------------------------------------------------------------------------
     * In my own words: I'm given a lowercase string `s`. I may repeatedly swap two ADJACENT
     * characters. I need the minimum number of such adjacent swaps required to turn `s` into
     * *some* palindrome (I get to choose which palindrome target — I'm not told a specific one).
     *
     * Key details:
     *   - Input:  a single string s, 1 <= s.length <= 2000, lowercase English letters only.
     *   - Output: a single integer — the minimum count of adjacent-position swaps.
     *   - Guarantee: the input is ALWAYS convertible into a palindrome (i.e., at most one
     *     character has an odd frequency count). I do not need to handle the "impossible" case
     *     for correctness, though I'll mention how I'd defend against it defensively.
     *   - "Move" == one adjacent transposition, i.e., swap(s[i], s[i+1]). This is NOT the same
     *     as "swap any two indices," which would be a much easier problem (see Follow-Ups).
     *   - I am free to choose ANY valid palindrome as the target — the algorithm must implicitly
     *     search over all valid target palindromes and pick the one reachable in fewest swaps.
     * ======================================================================================== */


    /* ========================================================================================
     * SECTION 2: CLARIFYING QUESTIONS (asked to the interviewer, with assumed answers)
     * ----------------------------------------------------------------------------------------
     * 1. Q: Is the string guaranteed to be convertible to a palindrome, or should I detect and
     *       reject impossible inputs?
     *    A (assumed): Guaranteed convertible, per the problem statement. I'll still note a
     *       O(26) frequency-parity check I could add defensively for production code.
     *
     * 2. Q: What is the character set — strictly lowercase 'a'-'z', or could there be uppercase,
     *       digits, unicode, or whitespace?
     *    A (assumed): Strictly lowercase English letters, confirmed by constraints.
     *
     * 3. Q: What's the maximum length of `s`? Does that hint at the expected time complexity?
     *    A (assumed): n <= 2000, so O(n^2) (~4,000,000 ops) is comfortably within budget for a
     *       typical 1-2 second time limit. This shapes which approach I should present as final.
     *
     * 4. Q: Do I need to return the resulting palindrome string too, or just the move count?
     *    A (assumed): Just the integer count. I'll note how to also reconstruct the palindrome
     *       as a trivial extension of the chosen algorithm.
     *
     * 5. Q: Are swaps restricted to ADJACENT indices only (as stated), or is this a hint I should
     *       double check because arbitrary-index swaps make this trivial?
     *    A (assumed): Adjacent only — this is what makes the problem non-trivial (it's really an
     *       "adjacent-swap sorting" / inversion-counting problem in disguise).
     *
     * 6. Q: If there are multiple characters that could be paired multiple ways (duplicates),
     *       does the interviewer expect me to prove *why* a specific greedy tie-break is optimal?
     *    A (assumed): Yes — I should be ready to justify the greedy choice (matching from the
     *       outside-in, preferring the closest valid partner from the far end) at a high level.
     *
     * 7. Q: Should the solution be thread-safe / callable concurrently on shared mutable state?
     *    A (assumed): No — single-threaded, stateless function call, no shared state.
     *
     * 8. Q: Is this a one-off call, or will it be invoked many times (e.g., per request in a
     *       service), which might push me toward the more complex O(n log n) structure even
     *       though n is small?
     *    A (assumed): One-off / occasional calls. Given n <= 2000, I will optimize for CODE
     *       CLARITY and CORRECTNESS over asymptotic complexity, but I'll mention the O(n log n)
     *       alternative in case constraints tighten (e.g., n up to 10^6, or high QPS).
     * ======================================================================================== */


    /* ========================================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ----------------------------------------------------------------------------------------
     * Example 1 (normal case):
     *   s = "aabb"
     *   One optimal path: "aabb" -> "abab" -> "abba"   => 2 moves.
     *   Answer: 2
     *
     * Example 2 (edge case — already a palindrome / single character):
     *   s = "a"            => Answer: 0 (trivially a palindrome, no swaps possible or needed)
     *   s = "aa"           => Answer: 0 (already a palindrome)
     *
     * Example 3 (boundary / tie-breaking case — odd length, middle character must migrate):
     *   s = "baa"
     *   Only valid palindrome target reachable is "aba".
     *   "baa" -> "aba" via swap(index0, index1) => 1 move.
     *   This exercises the subtle case where the unique odd-frequency character ('b') has NO
     *   matching partner and must be walked, one adjacent swap at a time, toward the center.
     *   Answer: 1
     *
     * Example 4 (longer boundary case with an interior odd character):
     *   s = "aabcb"
     *   Target palindrome: "abcba".
     *   Trace (used again in the Dry Run section): Answer: 3
     * ======================================================================================== */


    /* ========================================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * ----------------------------------------------------------------------------------------
     * PARADIGMS EXPLICITLY NOT APPLICABLE (one-liners, per instructions):
     *   - Pure Sorting-based:      Sorting destroys the positional/order information that the
     *                              swap-cost accounting fundamentally depends on — not useful.
     *   - Pure Hashing-based:      A frequency hash map is only useful to check FEASIBILITY
     *                              (odd-count parity), not to compute the swap count itself.
     *   - Sliding Window:          There's no contiguous "window" being grown/shrunk here.
     *   - Divide & Conquer:        No natural way to split the string, solve halves, and merge
     *                              swap-costs, because pairings can cross the midpoint.
     *   - Dynamic Programming:     DP solves a DIFFERENT problem — "minimum insertions to make a
     *                              palindrome" (LeetCode 1312, via LCS with reverse). Adjacent-
     *                              swap COUNT does not decompose into overlapping subproblems the
     *                              same way; DP is not the standard/intended tool here.
     *   - Heap / Priority Queue:   No "top-k" or ordering-by-priority need — greedy with direct
     *                              positional lookups is sufficient and faster.
     *   - Monotonic Stack/Deque:   No "next greater/smaller element" structure applies.
     *   - Trie:                    No prefix-matching over multiple strings is involved.
     *   - Binary Search (standalone): Not used top-level, but DOES appear as a building block
     *                              inside the advanced Fenwick-tree approach (Approach 3) via
     *                              TreeSet.lower()/higher(), which are O(log n) binary searches.
     *
     * APPLICABLE PARADIGMS COVERED BELOW:
     *   - Brute Force / Graph BFS       (Approach 1)
     *   - Greedy + Two-Pointer          (Approach 2 — RECOMMENDED for this interview)
     *   - Greedy + Two-Pointer + Fenwick Tree (segment-tree-family) + TreeSet binary search
     *                                    (Approach 3 — asymptotically optimal, advanced)
     * ======================================================================================== */

    /* ----------------------------------------------------------------------------------------
     * APPROACH 1: Brute Force — BFS Shortest Path over the "Adjacent Swap" Graph
     * ----------------------------------------------------------------------------------------
     * CORE IDEA:
     *   Model every distinct string reachable from `s` via adjacent swaps as a node in an
     *   implicit graph, with an edge between two strings if one adjacent swap transforms one
     *   into the other. The answer is the length of the shortest path from `s` to the NEAREST
     *   node that is a palindrome. BFS guarantees shortest path in an unweighted graph.
     *
     * DATA STRUCTURES / PARADIGM: Graph traversal (BFS), HashSet for visited-state dedup.
     *
     * TIME COMPLEXITY:  O(n! ) states in the worst case (or more precisely bounded by the number
     *                    of distinct permutations of the multiset of characters), each state
     *                    expanding into O(n) neighbors examined in O(n) time each -> factorial /
     *                    exponential. Completely infeasible beyond tiny n (n > ~8-10).
     * SPACE COMPLEXITY:  Same order — every visited permutation is stored.
     *
     * PROS:
     *   - Trivially, provably correct (BFS = shortest path by construction).
     *   - Zero algorithmic insight required — good sanity-check oracle for testing.
     * CONS:
     *   - Exponential blow-up; totally impractical for n as small as 12-15, let alone 2000.
     * WHEN TO USE:
     *   - Only as a correctness oracle for brute-force testing smaller approaches against, never
     *     in production, and never as an interview "final answer."
     * -------------------------------------------------------------------------------------- */
    static int bruteForceBFS(String input) {
        // Hard safety cap — factorial growth makes this unusable beyond tiny strings.
        if (input.length() > 8) {
            throw new IllegalArgumentException(
                "bruteForceBFS is a correctness oracle only; capped at length 8 to avoid blow-up.");
        }
        if (isPalindrome(input)) {
            return 0;
        }
        Queue<String> frontier = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        frontier.offer(input);
        visited.add(input);

        int movesSoFar = 0;
        while (!frontier.isEmpty()) {
            movesSoFar++;
            int levelSize = frontier.size();
            for (int levelIndex = 0; levelIndex < levelSize; levelIndex++) {
                char[] currentChars = frontier.poll().toCharArray();
                for (int swapPosition = 0; swapPosition < currentChars.length - 1; swapPosition++) {
                    char[] neighborChars = currentChars.clone();
                    swapAdjacent(neighborChars, swapPosition);
                    String neighborString = new String(neighborChars);
                    if (visited.contains(neighborString)) {
                        continue;
                    }
                    if (isPalindrome(neighborString)) {
                        return movesSoFar;
                    }
                    visited.add(neighborString);
                    frontier.offer(neighborString);
                }
            }
        }
        // Should never happen given the problem's guarantee that a palindrome is reachable.
        throw new IllegalStateException("No palindrome reachable — violates problem guarantee.");
    }

    private static void swapAdjacent(char[] chars, int position) {
        char temp = chars[position];
        chars[position] = chars[position + 1];
        chars[position + 1] = temp;
    }

    private static boolean isPalindrome(String candidate) {
        int leftIndex = 0;
        int rightIndex = candidate.length() - 1;
        while (leftIndex < rightIndex) {
            if (candidate.charAt(leftIndex) != candidate.charAt(rightIndex)) {
                return false;
            }
            leftIndex++;
            rightIndex--;
        }
        return true;
    }


    /* ----------------------------------------------------------------------------------------
     * APPROACH 2 (RECOMMENDED): Greedy Two-Pointer with In-Place Array Simulation
     * ----------------------------------------------------------------------------------------
     * CORE IDEA:
     *   Work inward from both ends with pointers `left` and `right`.
     *     - If s[left] == s[right], they already form a valid palindrome pair — shrink both
     *       pointers inward, no cost.
     *     - Otherwise, scan from `right` back toward `left` for the NEAREST index `matchIndex`
     *       whose character equals s[left]. Bubble that character (via a run of adjacent swaps)
     *       out to position `right`. Each single-position bubble is exactly one adjacent swap,
     *       so the cost of this step is (right - matchIndex).
     *     - Special case: if NO other occurrence of s[left] exists between left and right, then
     *       s[left] must be the unique odd-frequency character that will end up as the middle
     *       of the palindrome. Nudge it one position to the right (1 swap) and re-evaluate —
     *       this naturally walks it toward the center over subsequent iterations.
     *   Why "nearest from the right" is the correct greedy choice: bubbling the CLOSEST matching
     *   character to the boundary minimizes the number of OTHER characters it has to cross,
     *   which is exactly what minimizes total adjacent-swap cost (this is an inversion-count
     *   minimization argument — matching the closest candidate never crosses more elements than
     *   necessary, and never blocks a better future pairing, since all skipped elements remain
     *   in their relative order for later rounds).
     *
     * DATA STRUCTURES / PARADIGM: Two-pointer + greedy; mutable char[] as a stand-in for the
     *   physical array being rearranged.
     *
     * TIME COMPLEXITY:  O(n^2). Outer greedy processes O(n) pointer-narrowing rounds; each round
     *   does an O(n) linear scan-back to find a match AND up to O(n) physical adjacent swaps to
     *   bubble it into place. For n = 2000, that's ~4,000,000 primitive operations — comfortably
     *   fast (well under typical 1-2 second limits).
     * SPACE COMPLEXITY:  O(n) for the mutable character array (O(1) extra beyond the input copy).
     *
     * PROS:
     *   - Simple to explain, simple to code correctly under interview time pressure.
     *   - No auxiliary data structures beyond a char array — very low bug surface.
     *   - Meets performance requirements exactly at the given constraint (n <= 2000).
     * CONS:
     *   - Not asymptotically optimal; degrades quadratically if n were much larger.
     * WHEN TO USE:
     *   - Exactly this problem's stated constraints. This is what I'd write and submit live.
     * WHEN NOT TO USE:
     *   - If follow-up constraints push n to ~10^5-10^6, see Approach 3.
     * -------------------------------------------------------------------------------------- */
    static int minSwapsArraySimulation(String input) {
        char[] chars = input.toCharArray();
        int leftPointer = 0;
        int rightPointer = chars.length - 1;
        int totalMoves = 0;

        while (leftPointer < rightPointer) {
            int matchIndex = rightPointer;
            // Scan backward from the right boundary for the nearest character equal to s[left].
            while (matchIndex > leftPointer && chars[matchIndex] != chars[leftPointer]) {
                matchIndex--;
            }

            if (matchIndex == leftPointer) {
                // No partner found anywhere in the active range: chars[left] is the (unique)
                // odd-frequency character destined for the palindrome's center. Nudge it one
                // step toward the middle and let the loop re-evaluate from the same boundaries.
                swapAdjacent(chars, leftPointer);
                totalMoves++;
                // Deliberately do NOT move leftPointer/rightPointer here — we re-scan next
                // iteration with the character that is now at `leftPointer`.
            } else {
                // Bubble the matched character from matchIndex out to rightPointer, one
                // adjacent swap at a time. Each swap here costs exactly 1 move.
                for (int bubblePosition = matchIndex; bubblePosition < rightPointer; bubblePosition++) {
                    swapAdjacent(chars, bubblePosition);
                    totalMoves++;
                }
                leftPointer++;
                rightPointer--;
            }
        }
        return totalMoves;
    }


    /* ----------------------------------------------------------------------------------------
     * APPROACH 3 (ADVANCED / ASYMPTOTICALLY OPTIMAL): Greedy Two-Pointer + Fenwick Tree
     *                                                  + Per-Character TreeSet Index
     * ----------------------------------------------------------------------------------------
     * CORE IDEA:
     *   Same greedy pairing logic as Approach 2, but we NEVER physically shift array elements.
     *   Instead we track which ORIGINAL indices are still "active" (unpaired) using:
     *     - A global TreeSet<Integer> `active`            -> O(log n) access to current
     *       leftmost/rightmost unpaired index (`first()`/`last()`), and O(log n) neighbor
     *       queries (`higher()`), replacing the need to physically shrink an array.
     *     - A Map<Character, TreeSet<Integer>> `byChar`   -> O(log n) query for "nearest active
     *       occurrence of character c below index r", replacing the O(n) linear back-scan.
     *     - A Fenwick Tree (Binary Indexed Tree) over original indices, storing 1 for active /
     *       0 for consumed -> O(log n) range-count queries, replacing the O(n) bubble loop that
     *       physically counted "how many positions to swap past."
     *   The subtle "no partner found" case from Approach 2 is preserved: when the character at
     *   the left boundary has no active partner, we perform a logical (not physical) identity
     *   swap between that slot and its immediate neighbor's slot, updating only the `byChar`
     *   index — this is provably bounded to O(n) total such events across the whole run (the
     *   unique odd-frequency character can only be nudged at most n times, since its rank only
     *   ever increases), keeping the whole algorithm at O(n log n).
     *
     * DATA STRUCTURES / PARADIGM: Fenwick Tree (segment-tree family) + balanced BST (TreeSet)
     *   binary search + greedy two-pointer.
     *
     * TIME COMPLEXITY:  O(n log n) — O(n) total pairing/shift events, each touching a constant
     *   number of TreeSet / Fenwick operations at O(log n) apiece.
     * SPACE COMPLEXITY:  O(n) for the active set, per-character index, and Fenwick tree array.
     *
     * PROS:
     *   - Scales to much larger n (10^5 - 10^7) where O(n^2) would time out.
     *   - Still deterministic and provably optimal (mirrors the proven-correct greedy).
     * CONS:
     *   - Meaningfully more code, more edge cases (the logical-swap bookkeeping), higher constant
     *     factor and bug surface — overkill for n <= 2000.
     * WHEN TO USE:
     *   - If the interviewer's follow-up loosens the constraint to large n, or this function
     *     will be called extremely frequently in a hot path.
     * WHEN NOT TO USE:
     *   - As presented here, for n <= 2000 — Approach 2 already finishes in milliseconds and is
     *     far less risky to get right under interview time pressure.
     * -------------------------------------------------------------------------------------- */
    static long minSwapsFenwickTree(String input) {
        int n = input.length();
        if (n <= 1) {
            return 0;
        }

        // slotChar[idx] = the character LOGICALLY occupying original slot `idx` right now.
        // Only mutated during the rare "no partner found" logical-swap branch.
        char[] slotChar = input.toCharArray();

        TreeSet<Integer> active = new TreeSet<>();
        Map<Character, TreeSet<Integer>> byChar = new HashMap<>();
        FenwickTree fenwick = new FenwickTree(n);

        for (int index = 0; index < n; index++) {
            active.add(index);
            byChar.computeIfAbsent(slotChar[index], key -> new TreeSet<>()).add(index);
            fenwick.update(index, 1);
        }

        long totalMoves = 0;

        while (active.size() > 1) {
            int leftIndex = active.first();
            int rightIndex = active.last();

            if (slotChar[leftIndex] == slotChar[rightIndex]) {
                // Already a matching outer pair — consume both, zero cost.
                removeActiveIndex(leftIndex, active, byChar, slotChar, fenwick);
                removeActiveIndex(rightIndex, active, byChar, slotChar, fenwick);
                continue;
            }

            TreeSet<Integer> sameCharPositions = byChar.get(slotChar[leftIndex]);
            Integer matchIndex = sameCharPositions.lower(rightIndex); // largest active idx < right

            if (matchIndex != null && matchIndex > leftIndex) {
                // Cost = number of currently-active indices strictly after matchIndex, up to
                // and including rightIndex — exactly mirrors the "bubble distance" in Approach 2,
                // but computed in O(log n) instead of physically counted in O(n).
                totalMoves += fenwick.rangeSum(matchIndex + 1, rightIndex);
                removeActiveIndex(leftIndex, active, byChar, slotChar, fenwick);
                removeActiveIndex(matchIndex, active, byChar, slotChar, fenwick);
            } else {
                // No partner in range: slotChar[leftIndex] is the unique odd-frequency
                // character. Logically nudge it one slot to the right (costs 1 move) without
                // touching the Fenwick tree or the `active` set — both slots remain active.
                int nextIndex = active.higher(leftIndex);
                totalMoves += 1;

                byChar.get(slotChar[leftIndex]).remove(leftIndex);
                byChar.get(slotChar[nextIndex]).remove(nextIndex);

                char temp = slotChar[leftIndex];
                slotChar[leftIndex] = slotChar[nextIndex];
                slotChar[nextIndex] = temp;

                byChar.computeIfAbsent(slotChar[leftIndex], key -> new TreeSet<>()).add(leftIndex);
                byChar.computeIfAbsent(slotChar[nextIndex], key -> new TreeSet<>()).add(nextIndex);
            }
        }
        return totalMoves;
    }

    private static void removeActiveIndex(int index, TreeSet<Integer> active,
            Map<Character, TreeSet<Integer>> byChar, char[] slotChar, FenwickTree fenwick) {
        active.remove(index);
        byChar.get(slotChar[index]).remove(index);
        fenwick.update(index, -1);
    }

    /** Standard 1-indexed-internally Fenwick Tree (Binary Indexed Tree) over a 0-indexed range. */
    static class FenwickTree {
        private final int[] tree;
        private final int size;

        FenwickTree(int size) {
            this.size = size;
            this.tree = new int[size + 1];
        }

        void update(int zeroIndexedPosition, int delta) {
            for (int position = zeroIndexedPosition + 1; position <= size; position += position & (-position)) {
                tree[position] += delta;
            }
        }

        int prefixSum(int zeroIndexedInclusiveEnd) {
            int sum = 0;
            for (int position = zeroIndexedInclusiveEnd + 1; position > 0; position -= position & (-position)) {
                sum += tree[position];
            }
            return sum;
        }

        int rangeSum(int zeroIndexedLeft, int zeroIndexedRight) {
            if (zeroIndexedLeft > zeroIndexedRight) {
                return 0;
            }
            int total = prefixSum(zeroIndexedRight);
            if (zeroIndexedLeft > 0) {
                total -= prefixSum(zeroIndexedLeft - 1);
            }
            return total;
        }
    }


    /* ========================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ----------------------------------------------------------------------------------------
     * Approach                          | Time       | Space | Best For              | Limitations
     * ----------------------------------|------------|-------|------------------------|--------------------------------
     * 1. Brute Force BFS                | O(n!)-ish  | O(n!) | Correctness oracle,    | Totally infeasible beyond
     *    (exhaustive swap graph)        | exponential| exp.  | tiny n testing         | n ~ 8-10
     * ----------------------------------|------------|-------|------------------------|--------------------------------
     * 2. Greedy Two-Pointer + Array     | O(n^2)     | O(n)  | Interview submission;  | Quadratic; not ideal if n
     *    Simulation (RECOMMENDED)       |            |       | n <= ~5000-10000       | grows into the 10^5-10^7 range
     * ----------------------------------|------------|-------|------------------------|--------------------------------
     * 3. Greedy + Fenwick Tree +        | O(n log n) | O(n)  | Large n, high QPS,     | More code, more edge cases,
     *    TreeSet index                  |            |       | production hot paths   | higher constant factor
     * ======================================================================================== */


    /* ========================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR THIS INTERVIEW
     * ----------------------------------------------------------------------------------------
     * I would present APPROACH 2 (Greedy Two-Pointer + Array Simulation) as my final answer.
     *
     * Reasoning:
     *   - Given n <= 2000, O(n^2) is ~4,000,000 primitive operations — trivially fast, well
     *     within any reasonable time limit, so there is no PRACTICAL need for O(n log n).
     *   - It is far simpler to derive, explain, and code correctly under interview time pressure
     *     than the Fenwick-tree variant, which has a much larger bug surface (the "logical
     *     swap" bookkeeping for the odd-character case is easy to get subtly wrong live).
     *   - Interviewers grading this exact problem (a known "Hard" tier question) generally
     *     expect the O(n^2) greedy solution as the accepted optimal answer BECAUSE the
     *     constraint n <= 2000 was deliberately chosen to make it so.
     *   - I would explicitly mention Approach 3 verbally as a "if constraints were tighter,
     *     here's how I'd scale it" follow-up, to demonstrate depth without over-engineering
     *     the primary solution — this is exactly the judgment a senior engineer should show.
     * ======================================================================================== */


    /* ========================================================================================
     * SECTION 9: DEEP DIVE — POLISHED, PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ----------------------------------------------------------------------------------------
     * This is the version I'd actually write on the whiteboard / in the shared editor,
     * hardened with input validation and defensive comments for production use.
     * ======================================================================================== */
    static int minMovesToMakePalindrome(String s) {
        // --- Defensive validation (would mention verbally even though guaranteed by the
        //     problem statement — production code should never trust its inputs blindly). ---
        if (s == null) {
            throw new IllegalArgumentException("Input string must not be null.");
        }
        if (s.isEmpty()) {
            return 0; // Vacuously a palindrome.
        }

        char[] characters = s.toCharArray();
        int leftPointer = 0;
        int rightPointer = characters.length - 1;
        int totalMoves = 0;

        // Invariant maintained on every iteration: characters[0 .. leftPointer-1] mirrors
        // characters[rightPointer+1 .. end] perfectly; only the "active window"
        // [leftPointer, rightPointer] still needs to be resolved.
        while (leftPointer < rightPointer) {

            // Step 1: does the active window already have matching outer characters?
            if (characters[leftPointer] == characters[rightPointer]) {
                leftPointer++;
                rightPointer--;
                continue; // Zero-cost pairing; shrink the window and move on.
            }

            // Step 2: search backward from the right boundary for the CLOSEST character
            // equal to characters[leftPointer]. Searching from the far end (rather than the
            // near end) is what guarantees we never cross more elements than necessary.
            int matchIndex = rightPointer;
            while (matchIndex > leftPointer && characters[matchIndex] != characters[leftPointer]) {
                matchIndex--;
            }

            if (matchIndex == leftPointer) {
                // Step 3a: no partner exists in the active window at all — this character is
                // the (unique, guaranteed-at-most-one) odd-frequency character that must
                // eventually rest at the palindrome's center. Nudge it one step inward.
                swapAdjacent(characters, leftPointer);
                totalMoves++;
                // Pointers intentionally unchanged: next iteration re-examines the (new)
                // character now sitting at leftPointer.
            } else {
                // Step 3b: bubble the matched character from matchIndex to rightPointer via
                // a contiguous run of adjacent swaps. This costs exactly (rightPointer -
                // matchIndex) moves and preserves the relative order of every other character.
                for (int bubblePosition = matchIndex; bubblePosition < rightPointer; bubblePosition++) {
                    swapAdjacent(characters, bubblePosition);
                    totalMoves++;
                }
                leftPointer++;
                rightPointer--;
            }
        }
        return totalMoves;
    }


    /* ========================================================================================
     * SECTION 10: DRY RUN / TRACE
     * ----------------------------------------------------------------------------------------
     * Tracing minMovesToMakePalindrome("aabcb") step by step.
     * Initial array: ['a','a','b','c','b']   leftPointer=0, rightPointer=4, totalMoves=0
     *
     * Iteration 1:
     *   characters[left]='a', characters[right]='b'  -> mismatch.
     *   Scan back from right(4) for 'a': index4='b' no, index3='c' no, index2='b' no,
     *                                     index1='a' YES -> matchIndex=1.
     *   matchIndex(1) != leftPointer(0) -> bubble branch.
     *   Bubble loop bubblePosition = 1 .. 3:
     *     bubblePosition=1: swap(1,2): ['a','b','a','c','b'] totalMoves=1
     *     bubblePosition=2: swap(2,3): ['a','b','c','a','b'] totalMoves=2
     *     bubblePosition=3: swap(3,4): ['a','b','c','b','a'] totalMoves=3
     *   leftPointer=1, rightPointer=3.
     *
     * Iteration 2:
     *   characters[left]='b' (index1), characters[right]='b' (index3) -> match!
     *   leftPointer=2, rightPointer=2.
     *
     * Loop condition leftPointer < rightPointer is now false (2 < 2 is false) -> STOP.
     *
     * Final array: ['a','b','c','b','a'] -> "abcba", a valid palindrome.
     * Final totalMoves = 3.   Matches the expected answer from Section 3, Example 4.
     * ======================================================================================== */


    /* ========================================================================================
     * SECTION 11: CLOSING SUMMARY
     * ----------------------------------------------------------------------------------------
     *   - Brute Force BFS is correct but exponential; useful only as a testing oracle.
     *   - The Greedy Two-Pointer + Array Simulation (O(n^2)) is the right tool for THIS
     *     problem's stated constraints (n <= 2000): simple, low-risk, fast enough.
     *   - The Fenwick Tree + TreeSet variant (O(n log n)) is available if constraints scale up,
     *     at the cost of materially more implementation complexity.
     *   - Core assumption baked into all three solutions: the input is guaranteed convertible
     *     to a palindrome (at most one odd-frequency character). Without that guarantee, an
     *     O(26) frequency-parity pre-check should be added to fail fast on invalid input.
     *   - All solutions assume "move" strictly means an ADJACENT swap; this is central to why
     *     the problem is non-trivial in the first place.
     * ======================================================================================== */


    /* ========================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ----------------------------------------------------------------------------------------
     * 1. "What if n could be as large as 10^6?" -> Pivot to Approach 3 (O(n log n)); discuss why
     *    O(n^2) would now time out (10^12 ops).
     * 2. "What if swaps could be between ANY two indices, not just adjacent ones?" -> The problem
     *    becomes trivial: any arrangement is reachable in O(n) "moves" via cycle decomposition;
     *    you'd just need to check a target palindrome is achievable and count misplaced pairs.
     * 3. "What if the string might NOT be convertible into a palindrome?" -> Add an O(26)
     *    frequency-parity check up front; return -1 or throw a descriptive exception.
     * 4. "Can you also return the resulting palindrome, not just the count?" -> Trivial extension:
     *    return the final `characters` array (or String) alongside totalMoves.
     * 5. "How would you handle Unicode / case-insensitive matching / uppercase letters?" ->
     *    Discuss normalizing case first, and that the algorithm is agnostic to alphabet size
     *    (the O(n log n) version's per-character TreeSet map generalizes trivially).
     * 6. "This function is called millions of times per second on short strings — how would you
     *    optimize further?" -> Discuss avoiding object allocation (reuse buffers), possibly
     *    batching, and whether O(n^2) is still fine for SHORT strings even at high QPS.
     * ======================================================================================== */


    /* ========================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ----------------------------------------------------------------------------------------
     * 1. Confusing this with "Minimum Insertions to Make a String Palindrome" (LeetCode 1312),
     *    which IS a classic DP/LCS problem. This problem's ADJACENT-SWAP cost model does not
     *    reduce to that DP — candidates who reach for DP here usually get stuck or produce an
     *    unrelated (wrong) recurrence.
     * 2. Forgetting the odd-length "no partner found" branch entirely, or handling it by
     *    incorrectly advancing both pointers — this either infinite-loops or silently returns
     *    a wrong (too-low) move count on inputs with a genuine odd-frequency character.
     * 3. Searching for the matching partner from the WRONG side (e.g., nearest to `left` instead
     *    of nearest to `right`), which can still produce *a* palindrome but not necessarily in
     *    the MINIMUM number of moves — an easy way to fail hidden test cases that specifically
     *    check optimality, not just palindrome-ness.
     * 4. Off-by-one errors in the bubble loop bounds (`< rightPointer` vs `<= rightPointer`) or,
     *    in the advanced version, Fenwick tree range boundaries — both are classic sources of
     *    silent one-off miscounts that only surface on specific edge-length inputs.
     * ======================================================================================== */


    /* ========================================================================================
     * DEMONSTRATION / SELF-TEST HARNESS
     * ======================================================================================== */
    public static void main(String[] args) {
        String[] testInputs = {"aabb", "a", "aa", "baa", "aabcb"};
        int[] expectedAnswers = {2, 0, 0, 1, 3};

        for (int testIndex = 0; testIndex < testInputs.length; testIndex++) {
            String input = testInputs[testIndex];
            int expected = expectedAnswers[testIndex];

            int approach2Result = minMovesToMakePalindrome(input);
            long approach3Result = minSwapsFenwickTree(input);
            int approach1Result = input.length() <= 8 ? bruteForceBFS(input) : -1; // skip if too big

            System.out.printf(
                "s=\"%s\" | expected=%d | Approach2(O(n^2))=%d | Approach3(O(n log n))=%d | Approach1(BFS)=%s%n",
                input, expected, approach2Result, approach3Result,
                approach1Result == -1 ? "skipped (too large)" : String.valueOf(approach1Result));
        }
    }
}
