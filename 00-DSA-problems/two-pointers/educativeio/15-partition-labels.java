import java.util.*;

/* ================================================================================================
 * FILE: PartitionLabels.java
 * TOPIC: "Partition Labels" — split a string into the maximum number of contiguous parts such that
 *        every character appears in at most one part.
 *
 * This file is written exactly as I would walk an interviewer through the problem in a live Google
 * onsite / phone-screen setting: restating the problem, asking clarifying questions, working
 * examples, enumerating every viable approach (naive -> optimal), comparing them, then producing a
 * production-quality implementation of the one I'd actually code in the interview.
 * ================================================================================================
 */
public class PartitionLabels {

    /* ============================================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ============================================================================================
     *
     * In my own words:
     *   I'm given a string `s`. I need to cut it into contiguous, non-overlapping substrings
     *   (a "partition" of the string, read left to right, with no gaps and no reordering) such
     *   that:
     *
     *     - Every character that appears in `s` appears in EXACTLY ONE of these substrings
     *       (i.e., if 'b' shows up in part 2, it cannot also show up in part 1 or part 3).
     *     - Concatenating the parts in order reproduces `s` exactly.
     *     - Among all partitions satisfying the above, I want the one with the MAXIMUM number
     *       of parts (i.e., cut as finely as possible).
     *
     *   The output is not the substrings themselves, but a list of their LENGTHS, in order.
     *
     * Key constraints / inputs / outputs / assumptions (typical for this LeetCode-style problem,
     * to be confirmed with the interviewer in Section 2):
     *   - Input: a single string `s`, assumed to contain only lowercase English letters ('a'-'z').
     *   - Output: List<Integer> — the sizes of the partitions, in left-to-right order.
     *   - The maximum-cardinality partition satisfying the "each letter in one part" constraint
     *     is provably UNIQUE (this falls out of the interval-merging argument in Section 4), so
     *     there's no ambiguity about "which" maximal partition to return.
     *   - This is fundamentally an INTERVAL MERGING problem in disguise: each character defines
     *     an interval [first index, last index] that must be fully contained in one part, and
     *     overlapping intervals must be merged.
     */


    /* ============================================================================================
     * SECTION 2: CLARIFYING QUESTIONS
     * ============================================================================================
     *
     * Q1: What is the character set of `s`? Only lowercase 'a'-'z', or can it include uppercase,
     *     digits, Unicode, or whitespace?
     *     ASSUMED ANSWER: Lowercase English letters only ('a'-'z'), consistent with the example.
     *     (I'll note where the solution would change for a larger/unbounded alphabet.)
     *
     * Q2: What is the maximum length of `s`? Do I need to worry about very large inputs
     *     (10^6+) that rule out anything worse than O(n log n) or O(n)?
     *     ASSUMED ANSWER: Up to ~10^5 characters. I should aim for O(n) or O(n log n).
     *
     * Q3: Can `s` be empty or null?
     *     ASSUMED ANSWER: `s` is non-null; length can be 0, in which case the answer is an
     *     empty list.
     *
     * Q4: Is the maximal partition guaranteed to be unique, or if there are ties in "number of
     *     parts", do you want a specific one (e.g., lexicographically smallest partition sizes)?
     *     ASSUMED ANSWER: It is provably unique for this problem — no tie-breaking logic needed.
     *     (I will justify this uniqueness claim in Section 4.)
     *
     * Q5: Should the solution be case-sensitive (i.e., is 'A' a different character from 'a')?
     *     ASSUMED ANSWER: Yes, if uppercase were allowed it would be treated as distinct from
     *     lowercase. For this problem we assume lowercase only, so this is moot but worth asking.
     *
     * Q6: Do we need to return the actual substrings, or just their lengths?
     *     ASSUMED ANSWER: Just the lengths, per the problem statement ("Return a list of integers
     *     representing the sizes of these partitions").
     *
     * Q7: Is this a one-shot batch computation, or will this run in a concurrent / streaming
     *     context (e.g., `s` arriving incrementally, or many threads calling this simultaneously)?
     *     ASSUMED ANSWER: Single-threaded, one-shot batch computation on a fully materialized
     *     string. (I'll mention thread-safety only if asked — my final method uses only local
     *     state, so it's trivially thread-safe for concurrent calls with different inputs.)
     *
     * Q8: Should I validate malformed input (e.g., throw on null) or assume well-formed input?
     *     ASSUMED ANSWER: Throw an IllegalArgumentException on null input; otherwise assume
     *     well-formed lowercase input per Q1.
     */


    /* ============================================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ============================================================================================
     *
     * Example 1 (Normal case): s = "bcbcdd"
     *   - 'b' occurs at indices [0, 2], 'c' occurs at indices [1, 3] -> these ranges overlap
     *     (1 <= 2), so 'b' and 'c' must be merged into one part covering indices [0, 3] -> "bcbc".
     *   - 'd' occurs at indices [4, 5], which doesn't overlap with [0,3] -> separate part "dd".
     *   - Expected output: [4, 2]
     *
     * Example 2 (Edge case: every character distinct):
     *   s = "abcdef"
     *   - Every character occurs exactly once, at its own index. No overlaps at all.
     *   - Expected output: [1, 1, 1, 1, 1, 1] (each character is its own partition).
     *
     * Example 2b (Edge case: all identical characters):
     *   s = "aaaaa"
     *   - Only one distinct character spanning the whole string -> cannot be split at all.
     *   - Expected output: [5]
     *
     * Example 2c (Edge case: empty string):
     *   s = ""
     *   - No characters at all.
     *   - Expected output: [] (empty list)
     *
     * Example 3 (Boundary / "chained overlap" case — tests that merging cascades correctly):
     *   s = "eccbbbbdec"
     *   - 'e': first=0, last=8
     *   - 'c': first=1, last=9
     *   - 'b': first=2, last=5
     *   - 'd': first=7, last=7
     *   - 'b' [2,5] is nested inside 'e' [0,8]; 'd' [7,7] is nested inside both; 'c' [1,9]
     *     extends the merged window from 8 out to 9. Everything ends up chained into ONE
     *     giant interval [0, 9] because each character's range overlaps the running merged
     *     window, even though no single pair looks obviously connected at a glance.
     *   - Expected output: [10] (the entire string is one partition)
     *   - This is the classic "off-by-one on the merge boundary" trap: candidates who only
     *     check adjacent characters (not the running MAX of the merged window) get this wrong.
     */


    /* ============================================================================================
     * SECTION 4 & 5 & 6: ALL POSSIBLE APPROACHES
     * ============================================================================================
     * I'll cover every paradigm from the prompt. For irrelevant paradigms, I explain why they
     * don't meaningfully apply rather than forcing an artificial solution:
     *
     *   - Divide & Conquer: SKIPPED. A recursive split of `s` into halves doesn't reduce work,
     *     because determining valid cut points fundamentally requires knowing the full first/last
     *     occurrence range of every character, which crosses the midpoint arbitrarily. Any D&C
     *     scheme degenerates into "compute intervals, then merge them" — i.e., it collapses into
     *     Approach 2/3 below with no asymptotic or conceptual benefit, just added recursion
     *     overhead.
     *
     *   - Binary Search: SKIPPED. Binary search needs a monotonic predicate over a sorted search
     *     space (e.g., "is answer >= x?"). Here there's no such monotonic property to exploit —
     *     partition boundaries are determined by a running maximum, not a searchable threshold.
     *
     *   - Trie / Segment Tree: SKIPPED. The alphabet is fixed and tiny (<=26), so there is no
     *     prefix-matching need (rules out Trie) and no range-query need beyond tracking a simple
     *     running max over a 26-length array (rules out Segment Tree — it would only add
     *     complexity with zero benefit at this scale). A Segment Tree would become relevant only
     *     if the alphabet were unbounded/huge and we needed range-max queries dynamically, which
     *     isn't the case here.
     *
     * The following ARE covered, in increasing order of quality:
     *   Approach 1: Brute Force Backtracking                          (naive)
     *   Approach 2: Hashing + Sorting-Based Interval Merge             (sorting + hashing)
     *   Approach 3: Monotonic-Stack Interval Merge                     (monotonic stack)
     *   Approach 4: Union-Find (Disjoint Set) Character Grouping       (graph/tree paradigm)
     *   Approach 5: Heap / Priority-Queue Interval Merge                (heap)
     *   Approach 6: DP-Style Boundary Marking                          (dynamic programming, reframed)
     *   Approach 7: Greedy Single-Pass Sliding Window  <-- OPTIMAL      (two-pointer / greedy)
     */


    /* ------------------------------------------------------------------------------------------
     * APPROACH 1: Brute Force Backtracking (Naive)
     * ------------------------------------------------------------------------------------------
     * IDEA:
     *   Try every possible way of cutting the string into contiguous pieces. For each candidate
     *   cut, verify that no character straddles the boundary (i.e., every character used in a
     *   piece does not appear anywhere outside that piece). Among all VALID partitions, keep the
     *   one with the most pieces.
     *
     * DATA STRUCTURE / PARADIGM: Backtracking / recursive enumeration + brute-force validity check
     *   via a HashSet.
     *
     * TIME COMPLEXITY: Exponential. There are 2^(n-1) ways to place cut points in a string of
     *   length n, and each validity check costs O(n). Overall O(n * 2^n) in the worst case.
     *
     * SPACE COMPLEXITY: O(n) for recursion depth + O(n) for the current partition being built,
     *   plus O(1) auxiliary per validity check (26-letter alphabet) -> O(n) total.
     *
     * PROS:
     *   - Trivial to prove correct; a great warm-up / sanity-check oracle for testing faster
     *     solutions against small random inputs.
     * CONS:
     *   - Utterly infeasible beyond tiny strings (n > ~20).
     * WHEN TO USE:
     *   - Never in production or for n > ~20. Useful only as a correctness oracle in unit tests.
     * ------------------------------------------------------------------------------------------ */
    static final class BruteForceApproach {

        static List<Integer> solve(String s) {
            List<Integer> best = new ArrayList<>();
            backtrack(s, 0, new ArrayDeque<>(), best);
            return best;
        }

        private static void backtrack(String s, int start, Deque<Integer> currentSizes,
                                       List<Integer> best) {
            int n = s.length();
            if (start == n) {
                if (currentSizes.size() > best.size()) {
                    best.clear();
                    best.addAll(currentSizes);
                }
                return;
            }
            // Try every possible end point for the NEXT piece starting at `start`.
            for (int end = start + 1; end <= n; end++) {
                if (isSelfContained(s, start, end)) {
                    currentSizes.addLast(end - start);
                    backtrack(s, end, currentSizes, best);
                    currentSizes.removeLast(); // undo choice
                }
            }
        }

        // A candidate piece s[start, end) is valid only if none of its characters appear
        // anywhere OUTSIDE that range.
        private static boolean isSelfContained(String s, int start, int end) {
            Set<Character> charsInPiece = new HashSet<>();
            for (int i = start; i < end; i++) {
                charsInPiece.add(s.charAt(i));
            }
            for (int i = 0; i < s.length(); i++) {
                if (i >= start && i < end) continue; // inside the piece, fine
                if (charsInPiece.contains(s.charAt(i))) {
                    return false; // character leaked outside the piece
                }
            }
            return true;
        }
    }


    /* ------------------------------------------------------------------------------------------
     * APPROACH 2: Hashing + Sorting-Based Interval Merge
     * ------------------------------------------------------------------------------------------
     * IDEA:
     *   For each distinct character, compute its [firstIndex, lastIndex] span using a HashMap
     *   (hashing paradigm). Every occurrence of that character MUST live inside whichever final
     *   partition contains that span. This turns the problem into classic "merge overlapping
     *   intervals": sort the per-character intervals by start index, then sweep left to right,
     *   merging any interval that overlaps the currently-merged interval.
     *
     * DATA STRUCTURE / PARADIGM: HashMap (hashing) + sorting + interval-merge sweep.
     *
     * TIME COMPLEXITY: O(n) to build the map + O(k log k) to sort intervals, where k <= 26 is the
     *   number of distinct characters. Since k is bounded by the alphabet size, this is
     *   effectively O(n).
     *
     * SPACE COMPLEXITY: O(k) = O(26) = O(1) for the intervals, O(n) is NOT needed beyond the
     *   input itself.
     *
     * PROS:
     *   - Directly exposes the "interval merge" mental model, which is very interview-friendly
     *     to explain and reason about.
     *   - Generalizes trivially to arbitrary alphabets (not just 26 lowercase letters).
     * CONS:
     *   - Slightly more code and one extra sort compared to the fully optimal Approach 7.
     * WHEN TO USE:
     *   - Great to mention as the "aha" bridge from brute force to optimal, especially if the
     *     interviewer relaxes the "26 lowercase letters" constraint to a large/unbounded alphabet.
     * ------------------------------------------------------------------------------------------ */
    static final class IntervalMergeSortApproach {

        static List<Integer> solve(String s) {
            if (s.isEmpty()) return new ArrayList<>();

            // Hashing step: map each character to its [first, last] occurrence span.
            Map<Character, int[]> spanByChar = new HashMap<>();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                spanByChar.computeIfAbsent(c, k -> new int[]{i, i});
                spanByChar.get(c)[1] = i; // extend the "last seen" index
            }

            // Sorting step: order intervals by start index so the sweep below is correct.
            List<int[]> intervals = new ArrayList<>(spanByChar.values());
            intervals.sort(Comparator.comparingInt(interval -> interval[0]));

            // Sweep + merge step.
            List<Integer> partitionSizes = new ArrayList<>();
            int mergedStart = intervals.get(0)[0];
            int mergedEnd = intervals.get(0)[1];
            for (int i = 1; i < intervals.size(); i++) {
                int[] interval = intervals.get(i);
                if (interval[0] <= mergedEnd) {
                    // Overlaps the current merged window -> extend it.
                    mergedEnd = Math.max(mergedEnd, interval[1]);
                } else {
                    // No overlap -> the current merged window is a finished partition.
                    partitionSizes.add(mergedEnd - mergedStart + 1);
                    mergedStart = interval[0];
                    mergedEnd = interval[1];
                }
            }
            partitionSizes.add(mergedEnd - mergedStart + 1); // flush the last window
            return partitionSizes;
        }
    }


    /* ------------------------------------------------------------------------------------------
     * APPROACH 3: Monotonic-Stack Interval Merge
     * ------------------------------------------------------------------------------------------
     * IDEA:
     *   Identical setup to Approach 2 (hash map of per-character spans, sorted by start), but
     *   the merge step is performed with an explicit stack: push the first interval; for each
     *   subsequent interval, if it overlaps the interval on top of the stack, merge INTO the
     *   top-of-stack entry (extend its end); otherwise push a new entry. Because entries are
     *   processed in sorted-start order, the stack's "end" values are non-decreasing from bottom
     *   to top — the textbook "monotonic stack for merge-intervals" pattern.
     *
     * DATA STRUCTURE / PARADIGM: ArrayDeque used as a monotonic stack.
     *
     * TIME COMPLEXITY: O(n) to build spans + O(k log k) sort + O(k) stack sweep -> effectively
     *   O(n) since k <= 26.
     *
     * SPACE COMPLEXITY: O(k) for the stack.
     *
     * PROS:
     *   - Demonstrates the general-purpose "merge intervals via monotonic stack" template, which
     *     is reusable for many other interval problems (e.g., merge overlapping meeting rooms).
     * CONS:
     *   - Functionally identical output to Approach 2 with slightly more bookkeeping (stack push/
     *     pop vs. two running variables) — no real advantage here since we never need to look
     *     back more than one level.
     * WHEN TO USE:
     *   - Useful to mention if the interviewer specifically wants to see the monotonic-stack
     *     interval-merge template, or as a natural stepping stone if this problem is a follow-up
     *     to "Merge Intervals" (LeetCode 56).
     * ------------------------------------------------------------------------------------------ */
    static final class MonotonicStackMergeApproach {

        static List<Integer> solve(String s) {
            if (s.isEmpty()) return new ArrayList<>();

            Map<Character, int[]> spanByChar = new HashMap<>();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                spanByChar.computeIfAbsent(c, k -> new int[]{i, i});
                spanByChar.get(c)[1] = i;
            }

            List<int[]> intervals = new ArrayList<>(spanByChar.values());
            intervals.sort(Comparator.comparingInt(interval -> interval[0]));

            Deque<int[]> mergeStack = new ArrayDeque<>();
            for (int[] interval : intervals) {
                if (!mergeStack.isEmpty() && interval[0] <= mergeStack.peek()[1]) {
                    // Overlaps the top of the stack -> extend it in place.
                    mergeStack.peek()[1] = Math.max(mergeStack.peek()[1], interval[1]);
                } else {
                    mergeStack.push(interval.clone());
                }
            }

            // Stack is in reverse (last merged on top) -> collect and reverse to restore order.
            List<int[]> merged = new ArrayList<>(mergeStack);
            Collections.reverse(merged);

            List<Integer> partitionSizes = new ArrayList<>();
            for (int[] interval : merged) {
                partitionSizes.add(interval[1] - interval[0] + 1);
            }
            return partitionSizes;
        }
    }


    /* ------------------------------------------------------------------------------------------
     * APPROACH 4: Union-Find (Disjoint Set Union) Character Grouping
     * ------------------------------------------------------------------------------------------
     * IDEA:
     *   Model each of the 26 letters as a node in a graph. For character c, every OTHER character
     *   that appears anywhere between c's first and last occurrence must end up in the same final
     *   partition as c — so we UNION c with every character encountered in that span. After
     *   processing all characters, each connected component (found via DSU) represents one final
     *   partition; its overall span is the min(first) to max(last) among its members.
     *
     * DATA STRUCTURE / PARADIGM: Union-Find / Disjoint Set Union (graph connectivity paradigm).
     *
     * TIME COMPLEXITY: O(n * α(26)) ≈ O(n). We do one O(1)-ish union-find op per character
     *   position (n of them total across all spans in the worst case), each near O(1) amortized
     *   with path compression + union by rank.
     *
     * SPACE COMPLEXITY: O(26) for the DSU parent array and group ranges -> O(1).
     *
     * PROS:
     *   - Elegant graph-theoretic framing: "characters that must co-occur in a partition are
     *     connected components." Great to bring up as an alternative mental model, especially in
     *     a follow-up conversation about generalizing to arbitrary grouping constraints.
     * CONS:
     *   - More code/machinery (DSU with path compression) than necessary for what is fundamentally
     *     a simple interval-merge problem — overkill for the direct problem as stated.
     * WHEN TO USE:
     *   - Bring this up as a "graph paradigm" alternative to show breadth, or if the interviewer
     *     extends the problem to something like "group characters that must never be separated
     *     under a more complex adjacency rule" where DSU shines.
     * ------------------------------------------------------------------------------------------ */
    static final class UnionFindApproach {

        private static final class DisjointSetUnion {
            private final int[] parent;

            DisjointSetUnion(int size) {
                parent = new int[size];
                for (int i = 0; i < size; i++) parent[i] = i;
            }

            int find(int x) {
                if (parent[x] != x) {
                    parent[x] = find(parent[x]); // path compression
                }
                return parent[x];
            }

            void union(int a, int b) {
                int rootA = find(a);
                int rootB = find(b);
                if (rootA != rootB) parent[rootA] = rootB;
            }
        }

        static List<Integer> solve(String s) {
            if (s.isEmpty()) return new ArrayList<>();

            int[] firstOccurrence = new int[26];
            int[] lastOccurrence = new int[26];
            Arrays.fill(firstOccurrence, -1);

            for (int i = 0; i < s.length(); i++) {
                int letterIndex = s.charAt(i) - 'a';
                if (firstOccurrence[letterIndex] == -1) {
                    firstOccurrence[letterIndex] = i;
                }
                lastOccurrence[letterIndex] = i;
            }

            DisjointSetUnion dsu = new DisjointSetUnion(26);
            for (int letterIndex = 0; letterIndex < 26; letterIndex++) {
                if (firstOccurrence[letterIndex] == -1) continue; // letter not present
                // Every character seen within this letter's span must be unioned with it.
                for (int pos = firstOccurrence[letterIndex]; pos <= lastOccurrence[letterIndex]; pos++) {
                    int otherLetterIndex = s.charAt(pos) - 'a';
                    dsu.union(letterIndex, otherLetterIndex);
                }
            }

            // Aggregate [min-first, max-last] per connected component (root).
            Map<Integer, int[]> spanByRoot = new HashMap<>();
            for (int letterIndex = 0; letterIndex < 26; letterIndex++) {
                if (firstOccurrence[letterIndex] == -1) continue;
                int root = dsu.find(letterIndex);
                int[] span = spanByRoot.computeIfAbsent(root,
                        k -> new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE});
                span[0] = Math.min(span[0], firstOccurrence[letterIndex]);
                span[1] = Math.max(span[1], lastOccurrence[letterIndex]);
            }

            List<int[]> spans = new ArrayList<>(spanByRoot.values());
            spans.sort(Comparator.comparingInt(span -> span[0]));

            List<Integer> partitionSizes = new ArrayList<>();
            for (int[] span : spans) {
                partitionSizes.add(span[1] - span[0] + 1);
            }
            return partitionSizes;
        }
    }


    /* ------------------------------------------------------------------------------------------
     * APPROACH 5: Heap / Priority-Queue Interval Merge
     * ------------------------------------------------------------------------------------------
     * IDEA:
     *   Same per-character spans as Approach 2, but instead of sorting up front, push all spans
     *   into a min-heap ordered by start index, then repeatedly poll and merge — a pattern that
     *   generalizes better if intervals were arriving dynamically/streaming rather than all at
     *   once (e.g., merging live meeting-room intervals as they're scheduled).
     *
     * DATA STRUCTURE / PARADIGM: PriorityQueue (binary heap).
     *
     * TIME COMPLEXITY: O(n) to build spans + O(k log k) for heap operations, k <= 26 -> O(n)
     *   overall.
     *
     * SPACE COMPLEXITY: O(k) for the heap.
     *
     * PROS:
     *   - Natural fit if intervals arrive incrementally / need to be merged online rather than
     *     as one static batch.
     * CONS:
     *   - For this static, one-shot problem, a heap is strictly more overhead than simply sorting
     *     a small (<=26-element) list — no benefit here, since we already have all data upfront.
     * WHEN TO USE:
     *   - Prefer this only if the problem is reframed as a streaming/online interval-merge
     *     variant. For the problem as given, it's a reasonable "I also considered..." mention but
     *     not what I'd actually submit.
     * ------------------------------------------------------------------------------------------ */
    static final class HeapMergeApproach {

        static List<Integer> solve(String s) {
            if (s.isEmpty()) return new ArrayList<>();

            Map<Character, int[]> spanByChar = new HashMap<>();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                spanByChar.computeIfAbsent(c, k -> new int[]{i, i});
                spanByChar.get(c)[1] = i;
            }

            PriorityQueue<int[]> minHeapByStart =
                    new PriorityQueue<>(Comparator.comparingInt(interval -> interval[0]));
            minHeapByStart.addAll(spanByChar.values());

            List<Integer> partitionSizes = new ArrayList<>();
            int[] current = minHeapByStart.poll(); // heap guaranteed non-empty (s non-empty)
            while (!minHeapByStart.isEmpty()) {
                int[] next = minHeapByStart.poll();
                if (next[0] <= current[1]) {
                    current[1] = Math.max(current[1], next[1]); // merge
                } else {
                    partitionSizes.add(current[1] - current[0] + 1);
                    current = next;
                }
            }
            partitionSizes.add(current[1] - current[0] + 1); // flush last window
            return partitionSizes;
        }
    }


    /* ------------------------------------------------------------------------------------------
     * APPROACH 6: DP-Style Boundary Marking (Equivalent Reformulation)
     * ------------------------------------------------------------------------------------------
     * IDEA:
     *   Reframe partition-finding as filling in a boolean array `isCutPoint[i]`, where
     *   `isCutPoint[i]` is true iff index i is the LAST index of some partition. We fill this by
     *   maintaining `farthestReachRequired[i]` = the max "last occurrence" among all characters
     *   seen in s[0..i], then i is a cut point exactly when i == farthestReachRequired[i]. This is
     *   presented in a DP-flavored style (state = running "reach" value, transition = extend reach
     *   with each new character), but it's important to be transparent in an interview: this does
     *   NOT introduce genuine overlapping subproblems or optimal substructure beyond what greedy
     *   already captures — it is the same recurrence as Approach 7, just with the intent made
     *   explicit as a table-filling recurrence. I include it because the prompt asks for a DP
     *   treatment, but I'd tell the interviewer directly that classic DP (memoized recursion with
     *   branching choices) doesn't apply here since there's no actual choice being optimized over
     *   at each step — the "reach" is fully determined by the input, not by a decision.
     *
     * DATA STRUCTURE / PARADIGM: Bottom-up DP array (degenerate — single unique transition).
     *
     * TIME COMPLEXITY: O(n) — one pass to compute last-occurrence, one pass to fill the DP array.
     * SPACE COMPLEXITY: O(n) for the `isCutPoint` boolean array (worse than Approach 7's O(1)).
     *
     * PROS:
     *   - Useful pedagogically to show the recurrence explicitly as a table.
     * CONS:
     *   - Uses O(n) extra space for no benefit over Approach 7, which computes the same thing
     *     with O(1) extra space and no explicit array.
     * WHEN TO USE:
     *   - Essentially never in practice for THIS problem — included for completeness since the
     *     prompt asks for a DP treatment, but I'd flag to the interviewer that it's a strict
     *     downgrade from Approach 7 with no compensating benefit.
     * ------------------------------------------------------------------------------------------ */
    static final class DpBoundaryMarkingApproach {

        static List<Integer> solve(String s) {
            int n = s.length();
            if (n == 0) return new ArrayList<>();

            int[] lastOccurrence = new int[26];
            for (int i = 0; i < n; i++) {
                lastOccurrence[s.charAt(i) - 'a'] = i;
            }

            // dpReach[i] = the farthest index that must be included in the same partition as
            // index i, given everything seen in s[0..i].
            int[] dpReach = new int[n];
            dpReach[0] = lastOccurrence[s.charAt(0) - 'a'];
            for (int i = 1; i < n; i++) {
                int reachForThisChar = lastOccurrence[s.charAt(i) - 'a'];
                dpReach[i] = Math.max(dpReach[i - 1], reachForThisChar); // "transition"
            }

            List<Integer> partitionSizes = new ArrayList<>();
            int partitionStart = 0;
            for (int i = 0; i < n; i++) {
                if (i == dpReach[i]) { // this index is a valid cut point
                    partitionSizes.add(i - partitionStart + 1);
                    partitionStart = i + 1;
                }
            }
            return partitionSizes;
        }
    }


    /* ------------------------------------------------------------------------------------------
     * APPROACH 7 (RECOMMENDED / OPTIMAL): Greedy Single-Pass Sliding Window
     * ------------------------------------------------------------------------------------------
     * IDEA:
     *   Precompute the LAST occurrence index of every character in one pass (O(1) space, since
     *   the alphabet is bounded at 26). Then make a single left-to-right sweep with a "window"
     *   [partitionStart, windowEnd]: as we visit each index i, extend windowEnd to be the max of
     *   its current value and the last-occurrence of s[i]. The moment i == windowEnd, we know
     *   every character seen so far in this window has been fully "closed out" (its last
     *   occurrence has already been passed) — so this is a safe, greedy cut point. Because we
     *   always take the EARLIEST possible valid cut (greedy), and any character's true last index
     *   forces the window forward, this greedy choice is provably optimal: it can never merge two
     *   partitions that didn't NEED to be merged, and it always merges partitions that DO need to
     *   be merged (proving both maximality and uniqueness of the result).
     *
     * DATA STRUCTURE / PARADIGM: Two-pointer / sliding window + greedy, backed by a fixed-size
     *   array (hashing-lite lookup table).
     *
     * TIME COMPLEXITY: O(n) — exactly two linear passes over `s` (one to compute last-occurrence,
     *   one to sweep and cut), no sorting needed since we never leave index order.
     *
     * SPACE COMPLEXITY: O(1) — a fixed 26-element array, independent of input size (plus O(k) for
     *   the output list, which is required regardless of approach).
     *
     * PROS:
     *   - Optimal time AND optimal space among all approaches.
     *   - No sorting, no auxiliary data structures beyond a fixed-size array.
     *   - Single clean pass — easy to code correctly under interview time pressure and easy to
     *     explain/prove correct on a whiteboard.
     * CONS:
     *   - Relies on the bounded-alphabet assumption (26 letters) for true O(1) space; for an
     *     unbounded alphabet you'd swap the array for a HashMap, which degrades to O(k) space
     *     but keeps O(n) time — a graceful, easy-to-state fallback.
     * WHEN TO USE:
     *   - This is what I would code in a real interview, and what I'd ship in production: optimal
     *     complexity, minimal code, easy to verify correctness on the spot.
     * ------------------------------------------------------------------------------------------ */
    static final class GreedyOptimalApproach {

        static List<Integer> solve(String s) {
            if (s.isEmpty()) return new ArrayList<>();

            int[] lastOccurrenceIndex = new int[26];
            for (int i = 0; i < s.length(); i++) {
                lastOccurrenceIndex[s.charAt(i) - 'a'] = i;
            }

            List<Integer> partitionSizes = new ArrayList<>();
            int partitionStart = 0;
            int windowEnd = 0;
            for (int i = 0; i < s.length(); i++) {
                windowEnd = Math.max(windowEnd, lastOccurrenceIndex[s.charAt(i) - 'a']);
                if (i == windowEnd) {
                    partitionSizes.add(i - partitionStart + 1);
                    partitionStart = i + 1;
                }
            }
            return partitionSizes;
        }
    }


    /* ============================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ============================================================================================
     *
     * | Approach                                | Time             | Space  | Best For                         | Limitations                                            |
     * |------------------------------------------|------------------|--------|-----------------------------------|---------------------------------------------------------|
     * | 1. Brute Force Backtracking               | O(n * 2^n)       | O(n)   | Correctness oracle for tests       | Infeasible beyond tiny n; exponential blowup             |
     * | 2. Hashing + Sorting Interval Merge        | O(n + k log k)   | O(k)   | Explaining interval-merge model    | Extra sort vs. optimal; k<=26 so effectively O(n)        |
     * | 3. Monotonic-Stack Interval Merge          | O(n + k log k)   | O(k)   | Reusable interval-merge template   | Same output as #2, extra bookkeeping, no real gain here  |
     * | 4. Union-Find Character Grouping           | O(n * α(26))     | O(1)   | Graph/connectivity framing         | More machinery than needed for this specific problem     |
     * | 5. Heap / Priority-Queue Interval Merge     | O(n + k log k)   | O(k)   | Streaming/online interval merges   | Heap overhead unnecessary for static, one-shot input     |
     * | 6. DP-Style Boundary Marking                | O(n)             | O(n)   | Showing the recurrence as a table  | O(n) space for no benefit over Approach 7; not "real" DP |
     * | 7. Greedy Sliding Window (OPTIMAL)          | O(n)             | O(1)   | Production use & interview coding  | Assumes bounded alphabet for true O(1) (else O(k) map)   |
     *
     * (k = number of distinct characters, bounded by 26 for lowercase English letters.)
     */


    /* ============================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
     * ============================================================================================
     *
     * I would present APPROACH 7 (Greedy Single-Pass Sliding Window):
     *
     *   - Optimality: It achieves the best possible time (O(n), a single necessary pass over the
     *     input, since every character must be examined at least once) and the best possible
     *     space (O(1) beyond the output), so there is no complexity trade-off to defend.
     *   - Coding speed: It's about 10 lines of actual logic — very fast and low-risk to write
     *     correctly under interview time pressure, with few opportunities for bugs.
     *   - Clarity of proof: The greedy-choice argument is short and whiteboard-friendly: "the
     *     window can only close once every character seen so far has had its last occurrence
     *     accounted for; closing at the earliest such point can never break correctness and
     *     always maximizes the partition count."
     *   - Interviewer expectations: For a problem at this level, interviewers expect candidates
     *     to recognize the "last occurrence + greedy window" pattern and land on O(n)/O(1). This
     *     approach directly meets that bar and signals strong pattern recognition.
     *
     * I would mention Approach 2 (sorting-based interval merge) briefly as the "generalizes to
     * unbounded alphabets" fallback, since it's a natural, low-effort talking point that shows
     * depth without needing to fully code it.
     */


    /* ============================================================================================
     * SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (PRODUCTION-QUALITY)
     * ============================================================================================
     */
    static final class Solution {

        /**
         * Partitions {@code s} into the maximum number of contiguous, non-overlapping substrings
         * such that each character of {@code s} appears in exactly one substring.
         *
         * <p>Time complexity: O(n), where n = s.length() — two linear passes.
         * Space complexity: O(1) auxiliary (fixed 26-entry array), plus O(k) for the returned
         * list, where k is the number of partitions (k <= n).
         *
         * @param s a string containing only lowercase English letters ('a'-'z'); must not be null
         * @return an immutable-friendly, ordered list of partition sizes, left to right
         * @throws IllegalArgumentException if {@code s} is null
         */
        static List<Integer> partitionLabels(String s) {
            if (s == null) {
                throw new IllegalArgumentException("Input string must not be null.");
            }
            if (s.isEmpty()) {
                return List.of(); // no characters -> no partitions
            }

            final int alphabetSize = 26;

            // PASS 1: Record the LAST index at which each letter occurs. This is the only
            // global information the greedy sweep needs — it's what lets us decide, at any
            // point, "have I seen the final occurrence of every character in my current
            // partition yet?"
            int[] lastOccurrenceIndex = new int[alphabetSize];
            for (int i = 0; i < s.length(); i++) {
                lastOccurrenceIndex[s.charAt(i) - 'a'] = i;
                // Overwriting on every occurrence naturally leaves the LAST index once the
                // loop finishes, with no extra branching needed.
            }

            // PASS 2: Greedy sweep. `windowEnd` tracks the farthest index that MUST be included
            // in the partition currently being built, given every character seen so far in it.
            List<Integer> partitionSizes = new ArrayList<>();
            int partitionStart = 0; // start index of the partition currently being built
            int windowEnd = 0;      // farthest index the current partition must extend to

            for (int currentIndex = 0; currentIndex < s.length(); currentIndex++) {
                char currentChar = s.charAt(currentIndex);

                // Every time we see a character, our partition can never end before that
                // character's own last occurrence — so we extend the window if needed.
                windowEnd = Math.max(windowEnd, lastOccurrenceIndex[currentChar - 'a']);

                // If we've reached the farthest required index, no character we've seen so far
                // can force the window any further — this is a valid, and greedily optimal
                // (earliest possible), place to cut.
                if (currentIndex == windowEnd) {
                    int partitionLength = currentIndex - partitionStart + 1;
                    partitionSizes.add(partitionLength);
                    partitionStart = currentIndex + 1; // next partition starts right after
                }
            }

            return partitionSizes;
        }
    }


    /* ============================================================================================
     * SECTION 10: DRY RUN / TRACE
     * ============================================================================================
     *
     * Tracing Solution.partitionLabels on s = "bcbcdd" (indices: 0=b,1=c,2=b,3=c,4=d,5=d).
     *
     * PASS 1 — build lastOccurrenceIndex:
     *   i=0 'b' -> lastOccurrenceIndex['b'-'a'] = 0
     *   i=1 'c' -> lastOccurrenceIndex['c'-'a'] = 1
     *   i=2 'b' -> lastOccurrenceIndex['b'-'a'] = 2   (overwritten)
     *   i=3 'c' -> lastOccurrenceIndex['c'-'a'] = 3   (overwritten)
     *   i=4 'd' -> lastOccurrenceIndex['d'-'a'] = 4
     *   i=5 'd' -> lastOccurrenceIndex['d'-'a'] = 5   (overwritten)
     *   Final: lastOccurrenceIndex['b']=2, ['c']=3, ['d']=5   (all other letters remain 0/unused)
     *
     * PASS 2 — greedy sweep (partitionStart=0, windowEnd=0 initially):
     *
     *   currentIndex=0, char='b': windowEnd = max(0, lastOcc['b']=2) = 2
     *       currentIndex(0) != windowEnd(2) -> keep going
     *       state: partitionStart=0, windowEnd=2
     *
     *   currentIndex=1, char='c': windowEnd = max(2, lastOcc['c']=3) = 3
     *       currentIndex(1) != windowEnd(3) -> keep going
     *       state: partitionStart=0, windowEnd=3
     *
     *   currentIndex=2, char='b': windowEnd = max(3, lastOcc['b']=2) = 3   (no change)
     *       currentIndex(2) != windowEnd(3) -> keep going
     *       state: partitionStart=0, windowEnd=3
     *
     *   currentIndex=3, char='c': windowEnd = max(3, lastOcc['c']=3) = 3   (no change)
     *       currentIndex(3) == windowEnd(3) -> CUT!
     *       partitionLength = 3 - 0 + 1 = 4  -> partitionSizes = [4]
     *       partitionStart = 4
     *
     *   currentIndex=4, char='d': windowEnd = max(3, lastOcc['d']=5) = 5
     *       currentIndex(4) != windowEnd(5) -> keep going
     *       state: partitionStart=4, windowEnd=5
     *
     *   currentIndex=5, char='d': windowEnd = max(5, lastOcc['d']=5) = 5   (no change)
     *       currentIndex(5) == windowEnd(5) -> CUT!
     *       partitionLength = 5 - 4 + 1 = 2  -> partitionSizes = [4, 2]
     *       partitionStart = 6
     *
     *   Loop ends (currentIndex reaches s.length()=6).
     *
     * FINAL RESULT: [4, 2]  — matching "bcbc" (length 4) and "dd" (length 2). Confirmed correct.
     */


    /* ============================================================================================
     * SECTION 11: CLOSING SUMMARY
     * ============================================================================================
     *
     * - All seven approaches produce the same correct output; they differ only in efficiency and
     *   conceptual framing.
     * - The brute-force approach (Approach 1) is exponential and serves only as a correctness
     *   oracle.
     * - Approaches 2, 3, and 5 all implement the same underlying "merge overlapping character
     *   intervals" idea, differing only in the data structure used to perform the merge (sorted
     *   list, monotonic stack, heap) — useful to mention for breadth but functionally redundant
     *   with each other.
     * - Approach 4 (Union-Find) offers a genuinely different conceptual lens (graph connectivity)
     *   at the cost of extra machinery, with no efficiency gain over Approach 7 for this problem.
     * - Approach 6 (DP framing) is included to satisfy the "dynamic programming" angle explicitly,
     *   but I'm upfront that it's a relabeling of the same recurrence as Approach 7 with strictly
     *   worse (O(n) vs O(1)) space — I would say this directly if asked "can DP help here?"
     * - Approach 7 is the one I would actually submit: O(n) time, O(1) auxiliary space, minimal
     *   code, and a short, provable greedy-correctness argument.
     * - KNOWN ASSUMPTIONS / LIMITATIONS of the final solution:
     *     - Assumes `s` contains only lowercase English letters ('a'-'z'); the 26-element array
     *       would need to become a HashMap<Character, Integer> for a larger/unbounded alphabet
     *       (same O(n) time, O(k) space instead of true O(1)).
     *     - Assumes `s` is non-null (throws IllegalArgumentException otherwise); an empty string
     *       returns an empty list by design.
     *     - Assumes the maximal partition is desired and is unique, which holds by the greedy-
     *       exchange argument sketched in Approach 7's rationale.
     */


    /* ============================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ============================================================================================
     *
     * 1. "What if the alphabet were Unicode instead of just lowercase English letters?"
     *      -> Swap the fixed 26-element array for a HashMap<Character, Integer>; time stays
     *         O(n), space becomes O(k) where k = number of distinct characters actually present.
     *
     * 2. "What if `s` were extremely large (e.g., streamed from disk / network, too big for
     *    memory)?"
     *      -> Would need a streaming two-pass strategy: one pass to build last-occurrence info
     *         (which requires either buffering or a prior full pass), then a second pass replaying
     *         the stream to emit cuts — discuss trade-offs of requiring the data twice vs.
     *         precomputing last-occurrences via an index/metadata service.
     *
     * 3. "Can you return the actual substrings instead of just their lengths, without extra
     *    passes?"
     *      -> Yes — accumulate substring boundaries during the same sweep and call
     *         s.substring(partitionStart, currentIndex + 1) at each cut point; still O(n) time
     *         overall (substring creation is O(length) each, summing to O(n) total).
     *
     * 4. "What if we wanted the MINIMUM number of partitions instead of the maximum?"
     *      -> Trivial: the minimum is always 1 (the whole string as a single partition) unless
     *         additional constraints are added — worth clarifying what "minimum" would even mean
     *         here, since it's a degenerate case without further constraints.
     *
     * 5. "How would you test this solution?"
     *      -> Property-based tests comparing against BruteForceApproach on random short strings;
     *         explicit edge cases (empty string, single character, all-same character, all-
         *         distinct characters, adversarial chained-overlap strings like Example 3).
     *
     * 6. "Can this be parallelized for very large inputs?"
     *      -> Pass 1 (computing last-occurrence per character) is trivially parallelizable via a
     *         map-reduce style max-merge across chunks. Pass 2 (the greedy sweep) is inherently
     *         sequential because each cut decision depends on the running `windowEnd`, but chunk
     *         boundaries could be handled by having each chunk pre-compute local candidate cuts
     *         and then reconciling across chunk boundaries in a lightweight merge step.
     */


    /* ============================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ============================================================================================
     *
     * 1. Comparing against the PREVIOUS character's last occurrence instead of the RUNNING
     *    MAXIMUM (`windowEnd`). Example 3 ("eccbbbbdec") is specifically designed to catch this:
     *    a candidate who only checks "does this character's last occurrence go past the
     *    immediately preceding character's last occurrence" will cut too early and produce an
     *    invalid partition, because the chain of overlaps isn't directly adjacent.
     *
     * 2. Off-by-one errors in partition length: forgetting the "+1" when computing
     *    `currentIndex - partitionStart + 1`, or setting the next `partitionStart` to
     *    `currentIndex` instead of `currentIndex + 1`.
     *
     * 3. Assuming the input alphabet without asking — hardcoding a 26-element array without
     *    confirming with the interviewer that `s` is lowercase-only, which silently breaks (or
     *    requires an ArrayIndexOutOfBoundsException-prone fix) the moment uppercase, digits, or
     *    Unicode characters appear.
     *
     * 4. Forgetting the empty-string edge case, which should return an empty list rather than
     *    a list containing a zero or throwing an exception — an easy miss if the loop bounds
     *    aren't checked before use.
     */


    /* ============================================================================================
     * MAIN METHOD — quick demonstration / cross-validation of all approaches against each other
     * ============================================================================================
     */
    public static void main(String[] args) {
        List<String> testCases = List.of(
                "bcbcdd",
                "abcdef",
                "aaaaa",
                "",
                "eccbbbbdec"
        );

        for (String testCase : testCases) {
            List<Integer> bruteForceResult = BruteForceApproach.solve(testCase);
            List<Integer> sortMergeResult = IntervalMergeSortApproach.solve(testCase);
            List<Integer> stackMergeResult = MonotonicStackMergeApproach.solve(testCase);
            List<Integer> unionFindResult = UnionFindApproach.solve(testCase);
            List<Integer> heapMergeResult = HeapMergeApproach.solve(testCase);
            List<Integer> dpResult = DpBoundaryMarkingApproach.solve(testCase);
            List<Integer> optimalResult = Solution.partitionLabels(testCase);

            System.out.println("Input: \"" + testCase + "\"");
            System.out.println("  BruteForce   -> " + bruteForceResult);
            System.out.println("  SortMerge    -> " + sortMergeResult);
            System.out.println("  StackMerge   -> " + stackMergeResult);
            System.out.println("  UnionFind    -> " + unionFindResult);
            System.out.println("  HeapMerge    -> " + heapMergeResult);
            System.out.println("  DP-style     -> " + dpResult);
            System.out.println("  Optimal(rec) -> " + optimalResult);

            boolean allAgree = bruteForceResult.equals(sortMergeResult)
                    && bruteForceResult.equals(stackMergeResult)
                    && bruteForceResult.equals(unionFindResult)
                    && bruteForceResult.equals(heapMergeResult)
                    && bruteForceResult.equals(dpResult)
                    && bruteForceResult.equals(optimalResult);
            System.out.println("  All approaches agree: " + allAgree);
            System.out.println();
        }
    }
}
