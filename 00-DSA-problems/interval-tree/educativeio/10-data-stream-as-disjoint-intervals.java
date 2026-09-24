import java.util.*;

/*
================================================================================
 GOOGLE ONSITE MOCK INTERVIEW TRANSCRIPT
 Problem: LeetCode 352 — "Data Stream as Disjoint Intervals" (Summary Ranges)
 Track: Coding / Data Structures & Algorithms
 Target level: Senior SWE
================================================================================
*/

/*
================================================================================
 SECTION 1: RESTATE THE PROBLEM
================================================================================
 In my own words:

 I'm asked to design a class that ingests a STREAM of non-negative integers,
 one value at a time via addNum(value), and at any point can answer
 getIntervals(): "what is the current set of numbers I've seen so far,
 expressed as the minimal list of disjoint closed intervals [start_i, end_i],
 sorted by start_i?"

 Key properties:
   - The underlying "set" of numbers seen is a SET, not a multiset — duplicate
     addNum(value) calls must not change the summary (idempotent insert).
   - Two numbers x and x+1 both present means they belong to the SAME interval
     (numbers are "connected" if consecutive integers).
   - Every returned interval is disjoint from every other (no overlap, and by
     construction no adjacency either — if intervals were adjacent they'd have
     already been merged into one).
   - Output must be sorted ascending by start_i.

 Inputs / Outputs:
   - Constructor: no input, initializes empty stream.
   - addNum(int value): 0 <= value <= 10^4. Returns nothing (void).
   - getIntervals(): returns List<int[]> (or int[][]) of [start_i, end_i] pairs.

 Scale:
   - Up to 3 * 10^4 total calls to addNum + getIntervals combined.
   - Up to 100 of those calls are getIntervals() specifically (so addNum
     dominates the call volume — this matters for choosing where to spend
     complexity budget).

 Implicit assumption I'm confirming: this is an ONLINE / incremental problem
 (values trickle in over time interleaved with queries), NOT a batch problem
 where I get the whole array up front and just print merged intervals once
 (that would be trivial: sort once, merge once, done). The "stream" framing
 is the whole point — it forces me to think about incremental maintenance.
================================================================================
*/

/*
================================================================================
 SECTION 2: CLARIFYING QUESTIONS  (ask these before writing code)
================================================================================
 Q1. Is the value domain truly fixed at [0, 10^4], or could that bound change
     in a follow-up (e.g., int range, or unbounded long)?
     ASSUMED ANSWER: Treat [0, 10^4] as a hard constraint for the main
     solution, but be ready to discuss what changes if it's relaxed.

 Q2. Can addNum be called with a value it has already seen? Should duplicates
     be silently ignored?
     ASSUMED ANSWER: Yes, duplicates will occur, and must be silently
     ignored — the summary must stay identical to the "set so far".

 Q3. Does getIntervals() need to return a fresh snapshot each call, or can I
     hand back a live/cached reference that the caller might read after more
     addNum calls mutate it?
     ASSUMED ANSWER: Return a fresh, independent snapshot every call; the
     caller must not see it change out from under them later.

 Q4. What exact output shape is expected — inclusive [start, end] pairs, with
     a single isolated value represented as [v, v]?
     ASSUMED ANSWER: Yes — inclusive ranges, singletons as [v, v].

 Q5. Do I need to support concurrent addNum/getIntervals calls from multiple
     threads, or is this single-threaded?
     ASSUMED ANSWER: Single-threaded for the core solution; concurrency is
     fair game as a follow-up discussion, not a requirement to build now.

 Q6. Is there a hard per-call latency requirement, or is amortized cost across
     the whole call sequence acceptable?
     ASSUMED ANSWER: Amortized/aggregate cost across all ~3*10^4 calls is
     what matters; no single-call real-time SLA.

 Q7. Since intervals are disjoint, can two returned intervals ever share the
     same start_i, requiring a tie-break rule?
     ASSUMED ANSWER: No — disjointness guarantees unique starts, so "sorted
     by start_i" has no ties to break. Good to confirm out loud so I'm not
     over-engineering a comparator.

 Q8. Should I optimize primarily for O(number of intervals) memory/work
     rather than O(number of distinct values), given getIntervals() is called
     far less often (<=100 times) than addNum (<=3*10^4 times)?
     ASSUMED ANSWER: Yes — the interviewer wants to see that asymmetry
     reflected in the chosen data structure.
================================================================================
*/

/*
================================================================================
 SECTION 3: EXAMPLES & EDGE CASES
================================================================================
 Example 1 (NORMAL CASE — classic trace):
   addNum(1) -> [[1,1]]
   addNum(3) -> [[1,1],[3,3]]
   addNum(7) -> [[1,1],[3,3],[7,7]]
   addNum(2) -> [[1,3],[7,7]]                 (2 bridges 1 and 3)
   addNum(6) -> [[1,3],[6,7]]                 (6 attaches to 7 from the left)
   getIntervals() -> [[1,3],[6,7]]

 Example 2 (EDGE CASE — empty stream):
   new SummaryRanges(); getIntervals() -> []   (no addNum calls yet at all)

 Example 3 (BOUNDARY / TIE CASE — domain extremes + duplicate + exact-gap
 fill, which forces the "merge on both sides" branch that many candidates
 miss):
   addNum(0)     -> [[0,0]]                    (lower bound of domain)
   addNum(0)     -> [[0,0]]                    (duplicate must be a no-op)
   addNum(10000) -> [[0,0],[10000,10000]]      (upper bound of domain)
   addNum(9999)  -> [[0,0],[9999,10000]]       (extends the right interval)
   addNum(1)     -> [[0,1],[9999,10000]]       (extends the left interval)
   Then later, filling the exact single-value gap between two intervals
   forces a two-sided merge, e.g. continuing:
   addNum(2), addNum(3), ..., addNum(9998) eventually addNum(9998) fuses
   [0..9997] and [9999,10000] into one interval [0,10000] the moment the last
   gap value is inserted — this "merge-both-neighbors-and-delete-one-entry"
   case is the trickiest branch to implement correctly.
================================================================================
*/

/*
================================================================================
 SECTION 4-6: ALL POSSIBLE APPROACHES
 (paradigms genuinely applicable to this problem, naive -> optimal)
================================================================================

 Paradigms deliberately SKIPPED, with justification:
   - Dynamic Programming: N/A. There's no optimization objective with
     overlapping subproblems / optimal substructure here — this is pure
     online set-merge bookkeeping, not a "best value" computation.
   - Greedy (as a standalone paradigm): N/A as a *choice-among-alternatives*
     technique — there is no decision point with competing options to
     greedily pick from; the merge outcome for a given value is fully
     deterministic given the current interval set, not a greedy heuristic.
   - Tree / Graph traversal: N/A. There's no explicit graph to walk (the
     Union-Find approach below uses a *forest* internally, but that's a
     bookkeeping structure, not something we traverse/search over).
   - Heap / Priority Queue: N/A. Nothing here resembles "give me the
     current min/max/kth" — order is fully maintained structurally instead.
   - Divide & Conquer: N/A. The stream arrives incrementally; there's no
     static array to recursively split, and D&C doesn't help maintain
     state across incremental updates.
   - Monotonic Stack / Deque: N/A. No sliding-window or "next greater
     element"-style monotonic property to exploit.
   - Trie / Segment Tree: Technically possible (a segment tree over the
     bounded domain [0, 10^4] could track "is this value present" plus
     support range queries), but it adds real implementation complexity
     for *no* asymptotic win over the TreeMap/Union-Find approaches below
     given this problem's exact requirements. Worth mentioning as a natural
     extension point if the interviewer asks for range-sum-style queries
     (see Follow-Up Questions).
================================================================================
*/

class SummaryRangesInterviewPrep {

    /* ----------------------------------------------------------------------
     * APPROACH 1: Brute Force — Unsorted Set + Sort-on-Query
     * ----------------------------------------------------------------------
     * Core idea: Just remember every distinct value we've ever seen in a
     * HashSet (dedup is free). On every getIntervals() call, dump the set
     * into a list, sort it from scratch, then do a single linear pass to
     * glue together consecutive runs into intervals.
     *
     * Data structure / paradigm: Hashing (HashSet) + sorting.
     *
     * Time complexity:
     *   addNum:        O(1) average (HashSet insert).
     *   getIntervals:  O(n log n) EVERY call, where n = distinct values seen
     *                  so far — we re-sort from scratch each time, throwing
     *                  away all work from previous queries.
     * Space complexity: O(n) to store all distinct values.
     *
     * Pros:
     *   - Trivial to write correctly under interview pressure.
     *   - Great as a correctness ORACLE to stress-test smarter approaches
     *     against (see stress test in main()).
     * Cons:
     *   - Wasteful: recomputes the entire sort/merge on every query even if
     *     nothing changed between two getIntervals() calls.
     *   - Doesn't exploit "getIntervals() is called far less than addNum()".
     * When to use: as a warm-up answer to show you can solve it at all, or
     * as a verification oracle — never as your final answer in a Google
     * interview once you've identified the value/interval-count asymmetry.
     * ------------------------------------------------------------------- */
    static class BruteForceOracle {
        private final Set<Integer> seenValues = new HashSet<>();

        void addNum(int value) {
            seenValues.add(value); // Set semantics handle de-duplication for free.
        }

        List<int[]> getIntervals() {
            List<Integer> sortedValues = new ArrayList<>(seenValues);
            Collections.sort(sortedValues); // O(n log n) — recomputed every call, by design ("brute").

            List<int[]> mergedIntervals = new ArrayList<>();
            int index = 0;
            while (index < sortedValues.size()) {
                int intervalStart = sortedValues.get(index);
                int intervalEnd = intervalStart;
                // Extend the run while the next value is exactly one greater.
                while (index + 1 < sortedValues.size()
                        && sortedValues.get(index + 1) == intervalEnd + 1) {
                    intervalEnd = sortedValues.get(++index);
                }
                mergedIntervals.add(new int[]{intervalStart, intervalEnd});
                index++;
            }
            return mergedIntervals;
        }
    }

    /* ----------------------------------------------------------------------
     * APPROACH 2: Sorted Array + Binary Search Insertion
     * ----------------------------------------------------------------------
     * Core idea: Keep a single ArrayList<Integer> that is ALWAYS sorted and
     * duplicate-free. On addNum, binary search for the insertion point and
     * insert there. On getIntervals, the array is already sorted, so a
     * single linear scan builds the merged intervals — no re-sorting needed.
     *
     * Data structure / paradigm: Binary search + sorted array maintenance.
     * This is the approach that most directly demonstrates the "binary
     * search" dimension of the problem.
     *
     * Time complexity:
     *   addNum:       O(log n) to locate the slot (Collections.binarySearch),
     *                 but O(n) worst case to physically shift elements to
     *                 make room for the insert (ArrayList.add(index, val)).
     *                 So addNum is O(n) overall, dominated by the shift.
     *   getIntervals: O(n) single linear pass (array already sorted).
     * Space complexity: O(n) for the array of distinct values.
     *
     * Pros:
     *   - Conceptually simple: "sorted array, binary search where to put
     *     the new element."
     *   - getIntervals() never needs to re-sort, unlike Approach 1.
     *   - Good cache locality (contiguous array) versus tree-based structures.
     * Cons:
     *   - The insertion shift is O(n) worst case, so with up to 3*10^4
     *     addNum calls this can degrade toward O(n^2) total in the worst
     *     case (e.g., inserting in reverse sorted order every time).
     *   - Still O(n) — proportional to distinct VALUES, not to the (often
     *     much smaller) number of INTERVALS.
     * When to use: fine for small/bounded workloads, or when you want
     * predictable array-based memory layout and don't expect adversarial
     * insertion order. Not the interview-optimal answer here.
     * ------------------------------------------------------------------- */
    static class SortedArrayBinarySearchApproach {
        private final List<Integer> sortedDistinctValues = new ArrayList<>();

        void addNum(int value) {
            int searchResult = Collections.binarySearch(sortedDistinctValues, value);
            if (searchResult >= 0) {
                return; // Value already present -> duplicate -> no-op.
            }
            int insertionPoint = -(searchResult) - 1; // Standard binarySearch "not found" decoding.
            sortedDistinctValues.add(insertionPoint, value); // O(n) worst-case shift.
        }

        List<int[]> getIntervals() {
            List<int[]> mergedIntervals = new ArrayList<>();
            int index = 0;
            while (index < sortedDistinctValues.size()) {
                int intervalStart = sortedDistinctValues.get(index);
                int intervalEnd = intervalStart;
                while (index + 1 < sortedDistinctValues.size()
                        && sortedDistinctValues.get(index + 1) == intervalEnd + 1) {
                    intervalEnd = sortedDistinctValues.get(++index);
                }
                mergedIntervals.add(new int[]{intervalStart, intervalEnd});
                index++;
            }
            return mergedIntervals;
        }
    }

    /* ----------------------------------------------------------------------
     * APPROACH 3: Sorted Set (TreeSet<Integer>)
     * ----------------------------------------------------------------------
     * Core idea: Swap the array for a balanced-BST-backed TreeSet, which
     * gives O(log n) insertion WITHOUT the O(n) shifting cost of an array,
     * while still iterating in sorted order for getIntervals().
     *
     * Data structure / paradigm: Balanced BST (red-black tree) via TreeSet;
     * this is "hashing-based" thinking evolved into "sorted-structure"
     * thinking, and also leans on binary-search-style tree operations
     * internally.
     *
     * Time complexity:
     *   addNum:       O(log n) (TreeSet.add is a red-black tree insert;
     *                 duplicate handling is automatic via Set semantics).
     *   getIntervals: O(n) — must still walk every distinct value to find
     *                 where runs break, even if there are only a handful of
     *                 intervals covering thousands of values.
     * Space complexity: O(n).
     *
     * Pros:
     *   - Fixes Approach 2's O(n) insertion cost -> true O(log n) addNum.
     *   - Still simple to reason about; no shifting logic to get wrong.
     * Cons:
     *   - getIntervals() is still O(n), proportional to distinct VALUES
     *     rather than to the number of INTERVALS — wasteful when the stream
     *     is highly clustered (e.g. 30,000 consecutive integers = 1
     *     interval, yet we still touch all 30,000 entries on every query).
     * When to use: a solid middle-ground answer if you want to show
     * incremental improvement over the brute force before landing on the
     * fully optimal interval-based approach.
     * ------------------------------------------------------------------- */
    static class SortedSetApproach {
        private final TreeSet<Integer> distinctValues = new TreeSet<>();

        void addNum(int value) {
            distinctValues.add(value); // TreeSet dedups automatically, O(log n).
        }

        List<int[]> getIntervals() {
            List<int[]> mergedIntervals = new ArrayList<>();
            Integer runStart = null;
            Integer runPrev = null;
            for (int currentValue : distinctValues) { // in-order traversal = sorted order
                if (runStart == null) {
                    runStart = currentValue;
                    runPrev = currentValue;
                } else if (currentValue == runPrev + 1) {
                    runPrev = currentValue; // extend current run
                } else {
                    mergedIntervals.add(new int[]{runStart, runPrev}); // close current run
                    runStart = currentValue;
                    runPrev = currentValue;
                }
            }
            if (runStart != null) {
                mergedIntervals.add(new int[]{runStart, runPrev}); // flush final run
            }
            return mergedIntervals;
        }
    }

    /* ----------------------------------------------------------------------
     * APPROACH 4 (RECOMMENDED / OPTIMAL): TreeMap of Intervals (start -> end)
     * ----------------------------------------------------------------------
     * Core idea: Stop tracking individual VALUES altogether — directly
     * maintain the INTERVALS themselves in a TreeMap<Integer,Integer>
     * keyed by start_i, mapping to end_i. On addNum(value), use
     * floorEntry/higherEntry (both O(log m), m = number of intervals) to:
     *   (a) detect if `value` is already covered by an existing interval
     *       (duplicate) -> no-op,
     *   (b) detect if `value` is adjacent to the interval on its left
     *       (extends it rightward),
     *   (c) detect if `value` is adjacent to the interval on its right
     *       (extends it leftward),
     *   (d) both (b) and (c) at once -> fuse two intervals into one and
     *       delete the now-redundant right entry,
     *   (e) neither -> insert a brand-new singleton interval [value, value].
     *
     * Data structure / paradigm: Balanced BST (TreeMap / red-black tree)
     * used directly as an INTERVAL container, not a value container. This
     * is the key insight that separates this approach from Approach 3.
     *
     * Time complexity:
     *   addNum:       O(log m), where m = number of CURRENT intervals
     *                 (m <= n, often m << n for clustered streams).
     *   getIntervals: O(m) — just copy the TreeMap's entries; no scanning
     *                 of individual values at all.
     * Space complexity: O(m) — proportional to interval COUNT, not to the
     * number of distinct values ever inserted. This is strictly better
     * than Approaches 1-3 whenever the stream clusters into few intervals.
     *
     * Pros:
     *   - Both operations are near-optimal and scale with the RIGHT
     *     quantity (interval count), matching the problem's actual shape.
     *   - Directly answers Clarifying Question 8 (optimize for interval
     *     count, since getIntervals is rare but addNum is frequent).
     * Cons:
     *   - More intricate merge logic (4 branches) -> more surface area for
     *     off-by-one bugs (see "What Candidates Typically Miss").
     * When to use: this is the production-grade, interview-optimal answer
     * for the general (possibly unbounded-domain) version of this problem.
     * ------------------------------------------------------------------- */
    static class TreeMapIntervalApproach {
        private final TreeMap<Integer, Integer> intervalsByStart = new TreeMap<>();

        void addNum(int value) {
            Map.Entry<Integer, Integer> floorEntry = intervalsByStart.floorEntry(value);
            if (floorEntry != null && floorEntry.getValue() >= value) {
                return; // `value` already falls inside an existing interval -> duplicate.
            }

            boolean mergesWithLeftNeighbor = floorEntry != null && floorEntry.getValue() == value - 1;
            Map.Entry<Integer, Integer> higherEntry = intervalsByStart.higherEntry(value);
            boolean mergesWithRightNeighbor = higherEntry != null && higherEntry.getKey() == value + 1;

            if (mergesWithLeftNeighbor && mergesWithRightNeighbor) {
                // `value` is the single missing link between two intervals -> fuse them.
                intervalsByStart.put(floorEntry.getKey(), higherEntry.getValue());
                intervalsByStart.remove(higherEntry.getKey());
            } else if (mergesWithLeftNeighbor) {
                intervalsByStart.put(floorEntry.getKey(), value); // extend left interval's end rightward.
            } else if (mergesWithRightNeighbor) {
                int extendedEnd = higherEntry.getValue();
                intervalsByStart.remove(higherEntry.getKey()); // old start key is no longer correct.
                intervalsByStart.put(value, extendedEnd);      // re-key under the new, smaller start.
            } else {
                intervalsByStart.put(value, value); // brand-new isolated singleton interval.
            }
        }

        List<int[]> getIntervals() {
            List<int[]> result = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : intervalsByStart.entrySet()) {
                result.add(new int[]{entry.getKey(), entry.getValue()});
            }
            return result; // TreeMap iteration is already sorted by key (start_i).
        }
    }

    /* ----------------------------------------------------------------------
     * APPROACH 5 (BONUS / ADVANCED STRUCTURE): Union-Find over Bounded Domain
     * ----------------------------------------------------------------------
     * Core idea: The problem constraint 0 <= value <= 10^4 gives us a fixed,
     * small, KNOWN domain. That means we can allocate arrays sized to the
     * domain up front and use a Disjoint Set Union (DSU) forest where each
     * interval's canonical root is always its RIGHTMOST member. On
     * addNum(value): mark it visited, then union it with value-1 and/or
     * value+1 if they're already visited, always attaching the smaller
     * (left) root under the larger (right) root so the root stays the
     * interval's rightmost point. A side map tracks each root's interval
     * START, updated on every union.
     *
     * Data structure / paradigm: Union-Find / Disjoint Set Union with path
     * compression, exploiting the problem's bounded-domain constraint —
     * this is the "did you notice the tight bound?" bonus answer.
     *
     * Time complexity:
     *   addNum:       O(alpha(n)) amortized (~O(1) in practice) thanks to
     *                 path compression — the fastest addNum of all five
     *                 approaches, and this matters because addNum is called
     *                 up to 3*10^4 times versus getIntervals' <=100 times.
     *   getIntervals: O(m log m), m = current interval count — we must sort
     *                 the (root -> start) entries by start since DSU roots
     *                 don't come out in any particular order.
     * Space complexity: O(V) for the parent/visited arrays (V = domain size,
     *                 a fixed 10,002 here) plus O(m) for the start map.
     *
     * Pros:
     *   - Fastest possible addNum among all approaches shown.
     *   - Nicely demonstrates constraint-driven design — a strong signal in
     *     a senior interview.
     * Cons:
     *   - ONLY works because the domain is small and known ahead of time;
     *     does not generalize to arbitrary/unbounded integer streams (see
     *     Follow-Up Questions).
     *   - More moving parts (DSU + auxiliary start map) than the TreeMap
     *     approach, and getIntervals needs an explicit sort.
     * When to use: propose this as a "given the constraint, here's an even
     * faster addNum" bonus after presenting Approach 4, especially since
     * this exact problem's call ratio (30,000 addNum : 100 getIntervals)
     * rewards optimizing addNum aggressively.
     * ------------------------------------------------------------------- */
    static class UnionFindBoundedApproach {
        private static final int MAX_VALUE = 10_000;
        private final int[] parent;          // DSU parent pointers, sized with a safety buffer.
        private final boolean[] isVisited;   // has this exact value been added before?
        private final Map<Integer, Integer> intervalStartOfRoot; // root (rightmost val) -> interval start

        UnionFindBoundedApproach() {
            // +2 buffer so checking value+1 at value == MAX_VALUE never goes out of array bounds.
            parent = new int[MAX_VALUE + 2];
            isVisited = new boolean[MAX_VALUE + 2];
            intervalStartOfRoot = new HashMap<>();
            for (int i = 0; i < parent.length; i++) {
                parent[i] = i; // everyone starts as their own root.
            }
        }

        private int find(int x) {
            while (parent[x] != x) {
                parent[x] = parent[parent[x]]; // path halving for near-O(1) amortized find.
                x = parent[x];
            }
            return x;
        }

        // Attaches root(a) underneath root(b); root(b) survives. We always call this so that
        // `b` is the more-rightward element, keeping the surviving root == interval's rightmost value.
        private void union(int a, int b) {
            int rootA = find(a);
            int rootB = find(b);
            if (rootA == rootB) {
                return;
            }
            int mergedStart = Math.min(
                    intervalStartOfRoot.getOrDefault(rootA, a),
                    intervalStartOfRoot.getOrDefault(rootB, b));
            parent[rootA] = rootB;
            intervalStartOfRoot.remove(rootA);
            intervalStartOfRoot.put(rootB, mergedStart);
        }

        void addNum(int value) {
            if (value < 0 || value > MAX_VALUE) {
                throw new IllegalArgumentException("value out of the bounded domain [0, " + MAX_VALUE + "]");
            }
            if (isVisited[value]) {
                return; // duplicate -> no-op.
            }
            isVisited[value] = true;
            intervalStartOfRoot.put(value, value); // starts life as its own singleton interval.

            if (isVisited[value + 1]) {
                union(value, value + 1); // fuse with the interval to the right.
            }
            if (value - 1 >= 0 && isVisited[value - 1]) {
                union(value - 1, value); // fuse with the interval to the left (using the *updated* root of `value`).
            }
        }

        List<int[]> getIntervals() {
            List<int[]> result = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : intervalStartOfRoot.entrySet()) {
                int root = entry.getKey();     // root == rightmost value of the interval, by construction.
                int start = entry.getValue();
                result.add(new int[]{start, root});
            }
            result.sort((left, right) -> Integer.compare(left[0], right[0])); // DSU roots aren't naturally ordered.
            return result;
        }
    }

    /*
    ============================================================================
     SECTION 7: APPROACHES COMPARISON TABLE
    ============================================================================
     Approach                         | Time (addNum / getIntervals)     | Space  | Best For                                  | Limitations
     ----------------------------------|-----------------------------------|--------|--------------------------------------------|------------------------------------------
     1. Brute Force (sort-on-query)    | O(1) avg  / O(n log n) every call | O(n)   | Quick correctness oracle, warm-up answer   | Re-sorts from scratch every getIntervals()
     2. Sorted Array + Binary Search   | O(n) worst / O(n)                 | O(n)   | Small/bounded workloads, cache locality     | O(n) shift on insert can degrade to O(n^2)
     3. Sorted Set (TreeSet)           | O(log n) / O(n)                   | O(n)   | Simple incremental de-dup, no shifting cost | getIntervals scales with values, not intervals
     4. TreeMap of Intervals (OPTIMAL) | O(log m) / O(m)                   | O(m)   | General production answer, any domain size  | Trickiest merge logic (4 branches)
     5. Union-Find (bounded domain)    | O(alpha(n)) ~O(1) / O(m log m)    | O(V+m) | Maximizing addNum throughput under a known,
                                                                                       small, fixed domain                        | Requires a bounded/known domain up front
     (n = distinct values seen so far; m = current number of disjoint intervals, m <= n; V = fixed domain size)
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
    ============================================================================
     I would present APPROACH 4 (TreeMap of Intervals) as my primary answer:

       - It directly stores what the problem actually asks for (intervals),
         rather than individual values, so both operations scale with the
         quantity that matters (m = interval count) instead of the
         (potentially much larger) count of raw values seen.
       - It's a general solution that does NOT depend on the value domain
         being small/bounded — it would still work correctly if the
         constraint were relaxed to arbitrary 32/64-bit integers, which
         signals robustness to the interviewer.
       - It's implementable cleanly in ~20-25 lines using only
         java.util.TreeMap, which is fast to code correctly under time
         pressure and easy to narrate branch-by-branch while writing it.
       - It strikes the right clarity/optimality balance expected at a
         senior level: not the most naive thing that works, but also not
         over-engineered relative to what the constraints demand.

     After landing on Approach 4, I would proactively mention APPROACH 5
     (Union-Find over the bounded domain) as a "given we know 0 <= value <=
     10^4, here's how I'd push addNum even closer to O(1) if that ratio of
     30,000 addNum calls to only 100 getIntervals calls turned out to matter
     in practice" — this demonstrates awareness of the specific constraints
     given, which is exactly the kind of depth Google interviewers reward,
     without over-committing to it as the primary/default answer since it
     sacrifices generality for that speed.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 9: DEEP DIVE — PRODUCTION-QUALITY OPTIMAL SOLUTION
    ============================================================================
     This is the fully polished version of Approach 4, written the way I'd
     leave it in the final minutes of the interview: defensive input
     validation, exhaustive comments on every branch, and matching the exact
     class/method names LeetCode expects for this problem.
    ============================================================================
    */
    static class SummaryRanges {

        // Keyed by each interval's start_i, valued by that interval's end_i.
        // Invariant maintained at all times: for any two entries (s1,e1) and
        // (s2,e2) with s1 < s2, we always have e1 + 1 < s2 (strictly disjoint
        // AND non-adjacent — adjacent intervals are always merged immediately).
        private final TreeMap<Integer, Integer> intervalsByStart;

        public SummaryRanges() {
            this.intervalsByStart = new TreeMap<>();
        }

        public void addNum(int value) {
            if (value < 0) {
                // Defensive check: the stated constraint guarantees non-negative
                // input, but a production method shouldn't silently corrupt
                // state if that invariant is ever violated upstream.
                throw new IllegalArgumentException("addNum expects a non-negative value, got: " + value);
            }

            // Step 1: find the interval (if any) whose start is <= value —
            // the only candidate that could already CONTAIN value, or that
            // could be extended from the left to reach value.
            Map.Entry<Integer, Integer> intervalAtOrBeforeValue = intervalsByStart.floorEntry(value);

            // Step 2: if that interval's end already reaches at least `value`,
            // then `value` is already a member of the stream -> idempotent no-op.
            if (intervalAtOrBeforeValue != null && intervalAtOrBeforeValue.getValue() >= value) {
                return;
            }

            // Step 3: determine adjacency to the left neighbor (the interval we
            // just looked up) and to the right neighbor (the very next interval
            // by start, if one exists).
            boolean touchesLeftNeighbor =
                    intervalAtOrBeforeValue != null && intervalAtOrBeforeValue.getValue() == value - 1;

            Map.Entry<Integer, Integer> nextIntervalByStart = intervalsByStart.higherEntry(value);
            boolean touchesRightNeighbor =
                    nextIntervalByStart != null && nextIntervalByStart.getKey() == value + 1;

            // Step 4: apply exactly one of the four possible merge outcomes.
            if (touchesLeftNeighbor && touchesRightNeighbor) {
                // `value` is the last missing integer bridging two previously
                // separate intervals -> fuse them into a single interval and
                // discard the now-redundant right-hand entry.
                int fusedStart = intervalAtOrBeforeValue.getKey();
                int fusedEnd = nextIntervalByStart.getValue();
                intervalsByStart.put(fusedStart, fusedEnd);
                intervalsByStart.remove(nextIntervalByStart.getKey());

            } else if (touchesLeftNeighbor) {
                // Simply stretch the left interval's end forward by one.
                intervalsByStart.put(intervalAtOrBeforeValue.getKey(), value);

            } else if (touchesRightNeighbor) {
                // Stretch the right interval's start backward by one. Because
                // the map is keyed by start, this requires removing the old
                // key and re-inserting under the new (smaller) key.
                int preservedEnd = nextIntervalByStart.getValue();
                intervalsByStart.remove(nextIntervalByStart.getKey());
                intervalsByStart.put(value, preservedEnd);

            } else {
                // `value` doesn't touch anything -> it becomes its own brand
                // new singleton interval [value, value].
                intervalsByStart.put(value, value);
            }
        }

        public List<int[]> getIntervals() {
            // Fresh snapshot every call, per Clarifying Question 3 — callers
            // must never observe a previously returned list mutate later.
            List<int[]> snapshot = new ArrayList<>(intervalsByStart.size());
            for (Map.Entry<Integer, Integer> interval : intervalsByStart.entrySet()) {
                snapshot.add(new int[]{interval.getKey(), interval.getValue()});
            }
            return snapshot; // TreeMap.entrySet() iterates in ascending key order already.
        }
    }

    /*
    ============================================================================
     SECTION 10: DRY RUN / TRACE  (Example 1 from Section 3, using SummaryRanges)
    ============================================================================
     State shown as the TreeMap's contents { start -> end } after each call.

     Initial:                              {}

     addNum(1):
       floorEntry(1) = null -> no left neighbor, no right neighbor.
       Action: insert singleton.           {1: 1}

     addNum(3):
       floorEntry(3) = (1,1); 1 < 3 so not a duplicate.
       touchesLeftNeighbor: 1 == 3-1? No (1 != 2).
       higherEntry(3) = null -> no right neighbor.
       Action: insert singleton.           {1: 1, 3: 3}

     addNum(7):
       floorEntry(7) = (3,3); not a duplicate (3 < 7).
       touchesLeftNeighbor: 3 == 6? No.
       higherEntry(7) = null.
       Action: insert singleton.           {1: 1, 3: 3, 7: 7}

     addNum(2):
       floorEntry(2) = (1,1); 1 < 2, not a duplicate.
       touchesLeftNeighbor: 1 == 2-1=1? YES.
       higherEntry(2) = (3,3); touchesRightNeighbor: 3 == 2+1=3? YES.
       Action: BOTH-SIDES MERGE -> fusedStart=1, fusedEnd=3; remove key 3.
                                            {1: 3, 7: 7}

     addNum(6):
       floorEntry(6) = (1,3); 3 < 6, not a duplicate.
       touchesLeftNeighbor: 3 == 5? No.
       higherEntry(6) = (7,7); touchesRightNeighbor: 7 == 6+1=7? YES.
       Action: RIGHT MERGE -> remove key 7, insert (6,7).
                                            {1: 3, 6: 7}

     getIntervals():
       Walk entries in ascending key order -> [[1,3],[6,7]]   MATCHES Example 1.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 11: CLOSING SUMMARY
    ============================================================================
     - All five approaches are functionally correct; they differ only in
       where they spend complexity budget, and in whether they exploit the
       problem's specific constraints.
     - Approaches 1-3 all pay a cost proportional to the number of DISTINCT
       VALUES seen (n), which can be far larger than the number of INTERVALS
       (m) once the stream clusters — this is the central inefficiency the
       optimal approach eliminates.
     - Approach 4 (TreeMap of Intervals) is the right general-purpose,
       domain-agnostic answer: O(log m) addNum, O(m) getIntervals, O(m) space.
     - Approach 5 (Union-Find) trades generality for even faster addNum by
       leaning on the problem's stated bounded domain [0, 10^4] — a valid
       trade-off to surface but not to lead with, since it stops working the
       moment that bound is relaxed.
     - Known limitations / assumptions baked into SummaryRanges (Section 9):
         * Assumes single-threaded access (Clarifying Question 5).
         * Assumes value >= 0 is enforced by the caller / stream source, with
           a defensive IllegalArgumentException as a backstop.
         * Assumes "duplicate insert is a silent no-op" is the desired
           semantics (Clarifying Question 2), not an error condition.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
    ============================================================================
     1. "What if the value domain were NOT bounded (e.g., arbitrary 32-bit or
        64-bit integers)?" -> Union-Find (Approach 5) stops being viable since
        it needs domain-sized arrays; TreeMap (Approach 4) is unaffected and
        remains the correct general answer.
     2. "Now support removeNum(value) as well." -> Discuss how this changes
        the data structure choice: TreeMap intervals can be SPLIT (an
        interval [3,9] minus value 6 becomes [3,5] and [7,9]), which is more
        delicate than the pure-merge logic shown here.
     3. "Can getIntervals() report just the COUNT of intervals in O(1)
        instead of the full list?" -> Maintain a running intervalCount field,
        incremented/decremented alongside the TreeMap mutations, instead of
        computing size() or iterating.
     4. "What if addNum/getIntervals can be called concurrently from multiple
        threads?" -> Discuss ConcurrentSkipListMap as a lock-free-ish
        drop-in replacement for TreeMap, versus coarse-grained locking, and
        the trade-offs (throughput vs. correctness guarantees on snapshots).
     5. "Support a rangeSum(start, end) query — sum of all numbers added that
        fall within [start, end]." -> This is where a Fenwick tree / segment
        tree over the bounded domain becomes genuinely justified, unlike in
        the base problem.
     6. "What if this needs to scale beyond one machine's memory (billions of
        addNum calls, needs persistence)?" -> Discuss sharding intervals by
        value range across nodes, write-ahead logging for durability, and
        periodic compaction of the interval store.
    ============================================================================
    */

    /*
    ============================================================================
     SECTION 13: WHAT CANDIDATES TYPICALLY MISS
    ============================================================================
     1. Forgetting the duplicate check entirely (or getting it wrong) — e.g.
        checking `floorEntry.getKey() == value` instead of
        `floorEntry.getValue() >= value`. The former only catches duplicates
        that happen to be an interval's START, missing duplicates that are
        interior/end values of a larger interval (like re-adding 2 to [1,3]).
     2. The "merge BOTH sides" branch (Section 3's boundary example) is the
        one most candidates skip or get wrong under time pressure — many
        implementations only check left-OR-right and forget that fusing two
        intervals requires DELETING the now-redundant right-hand entry, not
        just updating the left one (which would silently leave a stale,
        duplicate/overlapping entry in the map).
     3. Off-by-one errors in the adjacency checks: using
        `floorEntry.getValue() == value` (should be `value - 1`) or
        `higherEntry.getKey() == value` (should be `value + 1`) — these are
        easy to typo and both compile fine, but silently produce wrong
        merges (or fail to merge adjacent intervals at all).
     4. In the bounded-domain Union-Find approach specifically: forgetting
        the `value - 1 >= 0` guard before checking `isVisited[value - 1]`,
        causing an ArrayIndexOutOfBoundsException the first time addNum(0)
        is called — a classic boundary bug at the domain's lower edge.
    ============================================================================
    */

    // ------------------------------------------------------------------------
    // DEMONSTRATION + RANDOMIZED STRESS TEST (run with `java -ea` to enable
    // the assertions used throughout, per established verification practice).
    // ------------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("=== Example demonstrations ===");
        demonstrateExamples();

        System.out.println();
        System.out.println("=== Dry run trace (Section 10) reproduced programmatically ===");
        runDryRunTrace();

        System.out.println();
        System.out.println("=== Randomized stress test: all 5 approaches vs. brute-force oracle ===");
        runRandomizedStressTest();

        System.out.println();
        System.out.println("All demonstrations and stress tests completed successfully.");
    }

    private static void demonstrateExamples() {
        // Example 1: normal case.
        SummaryRanges example1 = new SummaryRanges();
        for (int value : new int[]{1, 3, 7, 2, 6}) {
            example1.addNum(value);
        }
        System.out.println("Example 1 (normal): " + intervalsToString(example1.getIntervals()));
        assert intervalsToString(example1.getIntervals()).equals("[[1,3],[6,7]]") : "Example 1 mismatch!";

        // Example 2: empty stream edge case.
        SummaryRanges example2 = new SummaryRanges();
        System.out.println("Example 2 (empty stream): " + intervalsToString(example2.getIntervals()));
        assert example2.getIntervals().isEmpty() : "Example 2 should be empty!";

        // Example 3: boundary + duplicate + domain extremes.
        SummaryRanges example3 = new SummaryRanges();
        example3.addNum(0);
        example3.addNum(0);      // duplicate, must be a no-op
        example3.addNum(10000);
        example3.addNum(9999);
        example3.addNum(1);
        System.out.println("Example 3 (boundary/duplicate): " + intervalsToString(example3.getIntervals()));
        assert intervalsToString(example3.getIntervals()).equals("[[0,1],[9999,10000]]") : "Example 3 mismatch!";
    }

    private static void runDryRunTrace() {
        SummaryRanges tracedInstance = new SummaryRanges();
        int[] streamValues = {1, 3, 7, 2, 6};
        for (int value : streamValues) {
            tracedInstance.addNum(value);
            System.out.println("  after addNum(" + value + "): " + intervalsToString(tracedInstance.getIntervals()));
        }
        assert intervalsToString(tracedInstance.getIntervals()).equals("[[1,3],[6,7]]") : "Dry run trace mismatch!";
    }

    private static void runRandomizedStressTest() {
        final int NUM_TRIALS = 2000;
        final int OPERATIONS_PER_TRIAL = 60;
        final int VALUE_DOMAIN_MAX = 10_000; // matches problem constraint, required by Approach 5.
        Random random = new Random(42); // fixed seed for reproducibility.

        for (int trial = 0; trial < NUM_TRIALS; trial++) {
            BruteForceOracle oracle = new BruteForceOracle();
            SortedArrayBinarySearchApproach sortedArrayImpl = new SortedArrayBinarySearchApproach();
            SortedSetApproach sortedSetImpl = new SortedSetApproach();
            TreeMapIntervalApproach treeMapImpl = new TreeMapIntervalApproach();
            UnionFindBoundedApproach unionFindImpl = new UnionFindBoundedApproach();

            StringBuilder operationLog = new StringBuilder(); // for a debuggable assertion message.

            for (int operation = 0; operation < OPERATIONS_PER_TRIAL; operation++) {
                // Bias toward addNum (matches the real call-ratio: addNum >> getIntervals).
                boolean isGetIntervalsCall = random.nextInt(10) == 0;

                if (isGetIntervalsCall) {
                    operationLog.append("get;");
                    String oracleResult = intervalsToString(oracle.getIntervals());
                    assert oracleResult.equals(intervalsToString(sortedArrayImpl.getIntervals()))
                            : "Trial " + trial + " SortedArray mismatch. Ops: " + operationLog;
                    assert oracleResult.equals(intervalsToString(sortedSetImpl.getIntervals()))
                            : "Trial " + trial + " SortedSet mismatch. Ops: " + operationLog;
                    assert oracleResult.equals(intervalsToString(treeMapImpl.getIntervals()))
                            : "Trial " + trial + " TreeMap mismatch. Ops: " + operationLog;
                    assert oracleResult.equals(intervalsToString(unionFindImpl.getIntervals()))
                            : "Trial " + trial + " UnionFind mismatch. Ops: " + operationLog;
                } else {
                    int value = random.nextInt(VALUE_DOMAIN_MAX + 1); // [0, 10000] inclusive.
                    operationLog.append("add(").append(value).append(");");
                    oracle.addNum(value);
                    sortedArrayImpl.addNum(value);
                    sortedSetImpl.addNum(value);
                    treeMapImpl.addNum(value);
                    unionFindImpl.addNum(value);
                }
            }

            // Always verify final state at the end of each trial too.
            String finalOracleResult = intervalsToString(oracle.getIntervals());
            assert finalOracleResult.equals(intervalsToString(sortedArrayImpl.getIntervals()))
                    : "Trial " + trial + " final SortedArray mismatch. Ops: " + operationLog;
            assert finalOracleResult.equals(intervalsToString(sortedSetImpl.getIntervals()))
                    : "Trial " + trial + " final SortedSet mismatch. Ops: " + operationLog;
            assert finalOracleResult.equals(intervalsToString(treeMapImpl.getIntervals()))
                    : "Trial " + trial + " final TreeMap mismatch. Ops: " + operationLog;
            assert finalOracleResult.equals(intervalsToString(unionFindImpl.getIntervals()))
                    : "Trial " + trial + " final UnionFind mismatch. Ops: " + operationLog;
        }

        System.out.println("  " + NUM_TRIALS + " randomized trials x " + OPERATIONS_PER_TRIAL
                + " ops each, cross-validated across all 4 non-oracle approaches vs. the brute-force oracle: PASS");
    }

    // Canonical string form "[[s1,e1],[s2,e2],...]" used to compare interval lists across approaches.
    private static String intervalsToString(List<int[]> intervals) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < intervals.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            int[] interval = intervals.get(i);
            builder.append("[").append(interval[0]).append(",").append(interval[1]).append("]");
        }
        builder.append("]");
        return builder.toString();
    }
}

/**
 * ============================================================
 *                SUMMARY RANGES (ALL APPROACHES)
 * ============================================================
 *
 * Problem:
 * --------
 * Maintain a stream of numbers and return disjoint intervals.
 *
 * Example:
 * addNum(1) -> [1,1]
 * addNum(3) -> [1,1], [3,3]
 * addNum(2) -> [1,3]   (merge happens)
 *
 * ------------------------------------------------------------
 * 🧠 INTERVIEW THOUGHT PROCESS:
 * ------------------------------------------------------------
 *
 * Every new number can:
 *
 * 1. Start a new interval
 * 2. Extend an existing interval
 * 3. Merge two intervals
 *
 * So we need:
 *   - Find closest LEFT interval
 *   - Find closest RIGHT interval
 *
 * 👉 That leads to:
 *     TreeMap (Sorted Map) with floorKey() and ceilingKey()
 *
 * ------------------------------------------------------------
 * APPROACHES INCLUDED:
 * ------------------------------------------------------------
 *
 * 1. TreeMap (Optimal & Interview Preferred)
 * 2. TreeSet + Rebuild Intervals
 * 3. Boolean Array (Constraint-Based Optimization)
 *
 * ------------------------------------------------------------
 */


/* ============================================================
 *  APPROACH 1: TreeMap (BEST / INTERVIEW STANDARD)
 * ============================================================
 *
 * Idea:
 * -----
 * Store intervals as:
 *   start -> end
 *
 * Why TreeMap?
 * ------------
 * - Sorted order maintained automatically
 * - floorKey(value) -> left interval
 * - ceilingKey(value) -> right interval
 *
 * We handle 4 cases:
 *
 * 1. Already covered
 * 2. Merge both intervals
 * 3. Extend left interval
 * 4. Extend right interval
 * 5. Create new interval
 *
 * ------------------------------------------------------------
 * Time Complexity:
 *   addNum()       -> O(log N)
 *   getIntervals() -> O(N)
 *
 * Space Complexity:
 *   O(N)
 * ------------------------------------------------------------
 */
class SummaryRangesTreeMap {

    private TreeMap<Integer, Integer> map;

    public SummaryRangesTreeMap() {
        map = new TreeMap<>();
    }

    public void addNum(int value) {

        // Find closest intervals
        Integer left = map.floorKey(value);
        Integer right = map.ceilingKey(value);

        // Case 1: Already inside an interval
        if (left != null && map.get(left) >= value) {
            return; // duplicate / already covered
        }

        boolean mergeLeft = (left != null && map.get(left) + 1 == value);
        boolean mergeRight = (right != null && right - 1 == value);

        // Case 2: Merge both sides
        if (mergeLeft && mergeRight) {
            int newStart = left;
            int newEnd = map.get(right);

            map.put(newStart, newEnd);
            map.remove(right);
        }
        // Case 3: Extend left
        else if (mergeLeft) {
            map.put(left, map.get(left) + 1);
        }
        // Case 4: Extend right
        else if (mergeRight) {
            int rightEnd = map.get(right);
            map.remove(right);
            map.put(value, rightEnd);
        }
        // Case 5: New interval
        else {
            map.put(value, value);
        }
    }

    public int[][] getIntervals() {
        int[][] res = new int[map.size()][2];
        int i = 0;

        for (var entry : map.entrySet()) {
            res[i][0] = entry.getKey();
            res[i][1] = entry.getValue();
            i++;
        }

        return res;
    }
}


/* ============================================================
 *  APPROACH 2: TreeSet + BUILD INTERVALS
 * ============================================================
 *
 * Idea:
 * -----
 * Store all numbers in sorted order using TreeSet.
 *
 * When getIntervals() is called:
 *   -> Scan and build intervals
 *
 * ------------------------------------------------------------
 * Why this works:
 * ------------------------------------------------------------
 * TreeSet keeps numbers sorted.
 * So consecutive numbers form intervals.
 *
 * ------------------------------------------------------------
 * Downsides:
 * ------------------------------------------------------------
 * - Rebuild intervals every time → expensive
 * - Not optimal when getIntervals() is frequent
 *
 * ------------------------------------------------------------
 * Time Complexity:
 *   addNum()       -> O(log N)
 *   getIntervals() -> O(N)
 *
 * Space Complexity:
 *   O(N)
 * ------------------------------------------------------------
 */
class SummaryRangesTreeSet {

    private TreeSet<Integer> set;

    public SummaryRangesTreeSet() {
        set = new TreeSet<>();
    }

    public void addNum(int value) {
        set.add(value); // duplicates automatically ignored
    }

    public int[][] getIntervals() {

        List<int[]> res = new ArrayList<>();

        Integer start = null;
        Integer prev = null;

        for (int num : set) {

            // Start new interval
            if (start == null) {
                start = num;
            }
            // Break in continuity -> close previous interval
            else if (num != prev + 1) {
                res.add(new int[]{start, prev});
                start = num;
            }

            prev = num;
        }

        // Add last interval
        if (start != null) {
            res.add(new int[]{start, prev});
        }

        return res.toArray(new int[res.size()][]);
    }
}


/* ============================================================
 *  APPROACH 3: BOOLEAN ARRAY (CONSTRAINT OPTIMIZED)
 * ============================================================
 *
 * Idea:
 * -----
 * Since values are limited (0 <= value <= 10^4),
 * we can use a boolean array.
 *
 * ------------------------------------------------------------
 * How it works:
 * ------------------------------------------------------------
 * - Mark presence using boolean array
 * - Scan array to build intervals
 *
 * ------------------------------------------------------------
 * When to use:
 * ------------------------------------------------------------
 * - When constraints are small
 * - Memory is not a concern
 *
 * ------------------------------------------------------------
 * Downsides:
 * ------------------------------------------------------------
 * - Fixed size memory
 * - Always scans entire array
 *
 * ------------------------------------------------------------
 * Time Complexity:
 *   addNum()       -> O(1)
 *   getIntervals() -> O(MAX_RANGE) ≈ O(10^4)
 *
 * Space Complexity:
 *   O(10^4)
 * ------------------------------------------------------------
 */
class SummaryRangesArray {

    private boolean[] seen;

    public SummaryRangesArray() {
        seen = new boolean[10001]; // max value constraint
    }

    public void addNum(int value) {
        seen[value] = true;
    }

    public int[][] getIntervals() {

        List<int[]> res = new ArrayList<>();
        int i = 0;

        while (i < seen.length) {

            if (!seen[i]) {
                i++;
                continue;
            }

            int start = i;

            // expand interval
            while (i < seen.length && seen[i]) {
                i++;
            }

            res.add(new int[]{start, i - 1});
        }

        return res.toArray(new int[res.size()][]);
    }
}
