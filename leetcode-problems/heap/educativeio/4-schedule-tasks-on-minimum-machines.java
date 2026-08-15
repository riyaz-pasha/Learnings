import java.util.*;

/*
 * ============================================================================
 * GOOGLE MOCK ONSITE — MINIMUM MACHINES TO SCHEDULE TASKS
 * ============================================================================
 * This file is written as a full interview transcript. Every major phase of
 * a real onsite (restatement -> clarification -> examples -> brute force ->
 * optimal -> dry run -> follow-ups -> traps) is captured as a labeled block
 * comment, in the order an interviewer expects to see them.
 *
 * NOTE ON PROBLEM IDENTITY: This is the classic "Meeting Rooms II" family
 * (interval partitioning / minimum resource allocation), with one important
 * twist: tasks[i] = [start, end) is a HALF-OPEN interval — a machine that
 * finishes a task at time t is immediately free to start a new task AT time
 * t. This is explicitly called out in the prompt ("begin executing a new
 * task immediately after completing the previous one").
 * ============================================================================
 */
class MinimumMachinesScheduling {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     * In my own words:
     *   I'm given n tasks, each with a start time and an end time. I need to
     *   assign every task to a "machine" such that no machine is ever running
     *   two tasks at the same instant. Machines are unlimited in supply, but
     *   I'm being scored on how FEW machines I actually need to use. I must
     *   return that minimum count — not the actual assignment (unless asked
     *   as a follow-up).
     *
     * Key constraints & I/O:
     *   - Input: int[][] tasks, where tasks[i] = {start_i, end_i}
     *   - Output: a single integer — the minimum number of machines required
     *   - 1 <= tasks.length <= 10^3
     *   - 0 <= start_i < end_i <= 10^4   (so end times are tightly bounded —
     *     this is a strong hint that a counting/bucket approach is viable)
     *
     * Key semantic detail (the "gotcha"):
     *   - A task occupies the half-open interval [start, end).
     *   - If task A ends at time 5 and task B starts at time 5, the SAME
     *     machine can run both. They do NOT count as overlapping.
     *   - This is different from some "meeting rooms" variants where touching
     *     endpoints DO conflict — I will confirm this explicitly with the
     *     interviewer in Section 2.
     *
     * Core insight (restated as a graph/interval problem):
     *   The minimum number of machines equals the MAXIMUM NUMBER OF TASKS
     *   THAT ARE SIMULTANEOUSLY ACTIVE at any single point in time. This is
     *   a known result (equivalent to the chromatic number of an interval
     *   graph, which for interval graphs equals the size of the largest
     *   clique — i.e., max overlap).
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (asked to interviewer, with assumed
     * answers so I can proceed without blocking)
     * ========================================================================
     * Q1: Do touching intervals count as overlapping — i.e., if one task ends
     *     at time t and another starts at time t, can the same machine serve
     *     both back-to-back?
     *     ASSUMED: Yes, same machine can be reused. Intervals are [start, end).
     *
     * Q2: Are start/end times integers, and are they guaranteed given as
     *     start_i < end_i (no zero-length tasks)?
     *     ASSUMED: Yes — guaranteed by constraints (0 <= start < end <= 10^4).
     *
     * Q3: Can tasks list be empty?
     *     ASSUMED: Constraints say length >= 1, so no need to special-case
     *     empty input, but I'll guard for it defensively anyway.
     *
     * Q4: Do I need to return WHICH machine executes each task (an actual
     *     assignment/schedule), or just the count?
     *     ASSUMED: Just the minimum count for the primary solution; I'll
     *     mention how to extend to a full assignment as a follow-up.
     *
     * Q5: Can two tasks have identical [start, end) — i.e., duplicate
     *     intervals?
     *     ASSUMED: Yes, duplicates are allowed and must each get a machine
     *     (or share, if their combined overlap with others allows it).
     *
     * Q6: Is this a one-shot batch computation, or do tasks arrive online
     *     (streaming) and I need to answer incrementally?
     *     ASSUMED: Batch/offline — the full tasks array is known upfront.
     *     I'll mention the online variant as a follow-up.
     *
     * Q7: Given the tight bound end_i <= 10^4, is it fair game to exploit
     *     that bound with a counting/bucket approach, or should the solution
     *     generalize to arbitrarily large time values?
     *     ASSUMED: Both are valuable — I'll present the general O(n log n)
     *     solution as primary, and mention the bucket-counting O(n + maxTime)
     *     solution as a constraint-aware optimization.
     *
     * Q8: Is thread-safety / concurrency relevant (multiple threads computing
     *     this simultaneously)?
     *     ASSUMED: No — this is a single-threaded, deterministic computation
     *     over a static input array.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (normal case):
     *   tasks = [[0,3],[2,6],[5,8],[7,10]]
     *   Timeline:
     *     [0,3)         M1: ---***-------------
     *     [2,6)         M2: -----***----------- (overlaps [0,3) during [2,3))
     *     [5,8)         M1: reused, since M1 freed at 3, task starts at 5 >= 3
     *     [7,10)        M2: reused, since M2 freed at 6, task starts at 7 >= 6
     *   Peak overlap = 2 (e.g. at time 2.5, both [0,3) and [2,6) are active)
     *   Answer: 2
     *
     * Example 2 (edge case — touching endpoints, single task, minimal input):
     *   tasks = [[1,4]]
     *   Only one task exists; trivially needs 1 machine.
     *   Answer: 1
     *
     *   tasks = [[0,5],[5,10],[10,15]]
     *   Every task starts exactly when the previous ends -> NO overlap at all
     *   under half-open semantics -> a single machine handles all three
     *   sequentially.
     *   Answer: 1  <-- this is the critical boundary case that trips people
     *   up if they treat touching endpoints as overlapping.
     *
     * Example 3 (tie-breaking / dense overlap boundary case):
     *   tasks = [[1,5],[1,5],[1,5]]
     *   Three identical intervals, all mutually overlapping the entire time.
     *   Answer: 3 (need one machine per task, no reuse possible)
     *
     *   tasks = [[1,10],[2,3],[4,5],[6,7]]
     *   One long task spans the others; the others are disjoint from each
     *   other but each overlaps the long task.
     *   Peak overlap = 2 (long task + whichever short task is active)
     *   Answer: 2
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * ------------------------------------------------------------------------
     * Paradigm coverage note (dimensions explicitly ruled out):
     *   - Divide & Conquer: No natural way to split intervals and merge
     *     partial "max overlap" results cheaper than just sweeping; skipped.
     *   - Dynamic Programming: No overlapping subproblems / optimal
     *     substructure beyond what greedy already captures optimally
     *     (interval graph coloring has a greedy-optimal solution); skipped.
     *   - Tree/Graph traversal (DFS/BFS): Modeling this as an interval graph
     *     and coloring via traversal is possible in theory but is strictly
     *     worse (O(n^2) edges) than the sweep-line formulations below;
     *     skipped as impractical.
     *   - Hashing-based: There's no natural key-lookup structure here — the
     *     problem is fundamentally about temporal ORDER, not membership
     *     lookup, so classic hashing doesn't apply. (Bucket counting in
     *     Approach 5 LOOKS hash-like but is really direct array indexing
     *     exploiting bounded time values, not hashing.)
     *   - Trie: No prefix/string structure exists; not applicable.
     *   - Segment Tree / BIT: Could maintain a range-max structure over time
     *     buckets to answer "max concurrent tasks" with point updates and a
     *     global max query, but this strictly adds complexity over the
     *     difference-array approach (Approach 5) for this offline/batch
     *     problem — I'll mention it as a follow-up for the ONLINE variant
     *     (Section 12) where it actually earns its keep.
     * ========================================================================
     */

    /*
     * ------------------------------------------------------------------------
     * APPROACH 1: Brute Force Simulation
     * ------------------------------------------------------------------------
     * Core idea:
     *   Sort tasks by start time. Maintain a list of "machine free times."
     *   For each task, linearly scan all machines to find one that's free
     *   (freeTime <= task.start). If found, reuse it (update its free time).
     *   If none found, allocate a new machine.
     *
     * Data structure / paradigm: Greedy simulation + linear scan (List).
     *
     * Time Complexity: O(n^2) — for each of n tasks, we may scan up to n
     *   machine free-times in the worst case (e.g., all tasks mutually
     *   overlapping, so no machine is ever reusable early on).
     * Space Complexity: O(n) — one free-time entry per machine, up to n.
     *
     * Pros:
     *   - Extremely easy to reason about and verify by hand.
     *   - Zero risk of subtle heap/pointer bugs.
     * Cons:
     *   - Quadratic time; won't scale, though n <= 10^3 here makes it FEASIBLE
     *     (10^6 ops), just not the "right" answer for an interview.
     * When to use:
     *   - Only as a warm-up/starting point to build intuition, or when n is
     *     tiny and code simplicity trumps performance. I would explicitly
     *     state this is my starting point, then optimize.
     * ------------------------------------------------------------------------
     */
    static int minMachinesBruteForce(int[][] tasks) {
        if (tasks == null || tasks.length == 0) return 0;

        // Sort tasks by start time so we process them in chronological order.
        int[][] sortedTasks = tasks.clone();
        Arrays.sort(sortedTasks, (a, b) -> Integer.compare(a[0], b[0]));

        // machineFreeAt.get(i) = the time at which machine i becomes free.
        List<Integer> machineFreeAt = new ArrayList<>();

        for (int[] task : sortedTasks) {
            int start = task[0];
            int end = task[1];

            int reusableMachineIndex = -1;
            // Linear scan every existing machine for one that's free in time.
            for (int machineIndex = 0; machineIndex < machineFreeAt.size(); machineIndex++) {
                if (machineFreeAt.get(machineIndex) <= start) {
                    reusableMachineIndex = machineIndex;
                    break; // first-fit is sufficient for correctness here
                }
            }

            if (reusableMachineIndex == -1) {
                // No machine was free in time -> allocate a brand-new machine.
                machineFreeAt.add(end);
            } else {
                // Reuse the found machine; update when it becomes free next.
                machineFreeAt.set(reusableMachineIndex, end);
            }
        }

        return machineFreeAt.size();
    }


    /*
     * ------------------------------------------------------------------------
     * APPROACH 2: Sorting + Binary Search on Machine End Times
     * ------------------------------------------------------------------------
     * Core idea:
     *   Same greedy simulation as Approach 1, but keep the machine free-times
     *   in a SORTED array/list at all times. Instead of a linear scan, binary
     *   search for the largest free-time <= task.start. This is exactly the
     *   "patience sorting" trick used in Longest Increasing Subsequence.
     *
     * Data structure / paradigm: Greedy + Binary Search + sorted list
     *   (TreeMap<Integer,Integer> multiset-of-free-times, or a sorted
     *   ArrayList with manual binary search).
     *
     * Time Complexity: O(n log n) — sorting tasks O(n log n), then n binary
     *   searches/insertions, each O(log n) for search + O(n) for array
     *   shifting UNLESS we use a structure with O(log n) insert too (e.g. a
     *   TreeMap keyed by free-time with counts, shown below) -> true
     *   O(n log n) overall.
     * Space Complexity: O(n) — up to n machine free-times stored.
     *
     * Pros:
     *   - Same conceptual simplicity as brute force, but asymptotically
     *     optimal.
     *   - TreeMap gives us O(log n) floor-lookup AND O(log n) update, so we
     *     avoid the O(n) array-shift cost of a plain sorted ArrayList.
     * Cons:
     *   - Slightly more machinery than Approach 4 (two-pointer sweep) below
     *     for what is ultimately the same asymptotic result.
     * When to use:
     *   - Great middle-ground when you also want to eventually track WHICH
     *     machine (or a proxy key) is reused, and don't want a full heap.
     * ------------------------------------------------------------------------
     */
    static int minMachinesBinarySearch(int[][] tasks) {
        if (tasks == null || tasks.length == 0) return 0;

        int[][] sortedTasks = tasks.clone();
        Arrays.sort(sortedTasks, (a, b) -> Integer.compare(a[0], b[0]));

        // TreeMap<freeTime, countOfMachinesFreeAtThisTime> acts as a sorted
        // multiset. floorKey() gives O(log n) "largest free time <= start".
        TreeMap<Integer, Integer> freeTimeCounts = new TreeMap<>();
        int machinesInUse = 0;

        for (int[] task : sortedTasks) {
            int start = task[0];
            int end = task[1];

            Integer reusableFreeTime = freeTimeCounts.floorKey(start);
            if (reusableFreeTime != null) {
                // A machine is available -> reuse it (remove one instance of
                // that free-time, since that specific machine is no longer
                // free at reusableFreeTime — it's now busy until `end`).
                int remaining = freeTimeCounts.get(reusableFreeTime) - 1;
                if (remaining == 0) {
                    freeTimeCounts.remove(reusableFreeTime);
                } else {
                    freeTimeCounts.put(reusableFreeTime, remaining);
                }
            } else {
                // No machine available -> we need a brand new one.
                machinesInUse++;
            }
            // Whichever machine handled this task (new or reused) is now
            // busy and will next be free at `end`.
            freeTimeCounts.merge(end, 1, Integer::sum);
        }

        return machinesInUse;
    }


    /*
     * ------------------------------------------------------------------------
     * APPROACH 3: Sorting + Min-Heap (Priority Queue) — the "classic" answer
     * ------------------------------------------------------------------------
     * Core idea:
     *   Sort tasks by start time. Maintain a min-heap of "machine free
     *   times." For each task, if the heap's minimum free-time <= task.start,
     *   pop it (reuse that machine) and push the new end time. Otherwise,
     *   push a new end time without popping (new machine). The heap size
     *   trajectory's PEAK is not what we track directly — instead, the
     *   heap's final size after processing all tasks IS the answer (heap
     *   only grows when no reuse was possible).
     *
     * Data structure / paradigm: Greedy + Min-Heap (PriorityQueue).
     *
     * Time Complexity: O(n log n) — sorting O(n log n), plus n heap
     *   operations each O(log n).
     * Space Complexity: O(n) — heap holds at most n free-times.
     *
     * Pros:
     *   - THE textbook / most-recognized solution to "Meeting Rooms II" —
     *     an interviewer will almost always accept this immediately.
     *   - Naturally extends to tracking actual machine IDs (store
     *     (freeTime, machineId) pairs) for a real assignment / follow-up.
     * Cons:
     *   - Slightly more code/machinery than the two-pointer sweep (Approach
     *     4) for the same asymptotic complexity when you only need the
     *     COUNT, not an assignment.
     * When to use:
     *   - Default choice when you anticipate a follow-up requiring the
     *     actual task-to-machine assignment, or an online/streaming variant.
     * ------------------------------------------------------------------------
     */
    static int minMachinesHeap(int[][] tasks) {
        if (tasks == null || tasks.length == 0) return 0;

        int[][] sortedTasks = tasks.clone();
        Arrays.sort(sortedTasks, (a, b) -> Integer.compare(a[0], b[0]));

        // Min-heap of machine free-times; top = machine that frees up soonest.
        PriorityQueue<Integer> machineFreeTimes = new PriorityQueue<>();

        for (int[] task : sortedTasks) {
            int start = task[0];
            int end = task[1];

            if (!machineFreeTimes.isEmpty() && machineFreeTimes.peek() <= start) {
                // The earliest-freeing machine is free in time -> reuse it.
                machineFreeTimes.poll();
            }
            // Either way, some machine (reused or brand-new) is now busy
            // until `end`, so push its new free-time onto the heap.
            machineFreeTimes.add(end);
        }

        // Final heap size = number of distinct machines that were ever needed.
        return machineFreeTimes.size();
    }


    /*
     * ------------------------------------------------------------------------
     * APPROACH 4: Two-Pointer Chronological Sweep (RECOMMENDED — see Sec. 8)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Extract all start times into one sorted array and all end times into
     *   another sorted array. Walk through "events" in chronological order
     *   using two pointers. Every time we encounter a start event, we need
     *   one more machine UNLESS an end event happened at or before it (in
     *   which case a machine just freed up and can be reused) — track a
     *   running "machines currently active" counter and its running maximum.
     *   The key subtlety: process END events before START events when times
     *   tie, because [start, end) is half-open (Section 2, Q1).
     *
     * Data structure / paradigm: Sorting + Two Pointers (sweep line).
     *
     * Time Complexity: O(n log n) — dominated by sorting the two arrays.
     *   The sweep itself is O(n) — each pointer advances at most n times.
     * Space Complexity: O(n) — two auxiliary arrays of size n (O(1) extra
     *   beyond the sort itself, no heap object overhead).
     *
     * Pros:
     *   - Same O(n log n) time as the heap approach, but with lower constant
     *     factors (no heap sift-up/down operations, just array comparisons).
     *   - Very easy to explain visually as a timeline sweep — reads cleanly.
     *   - No dependency on task identity — pure counting, minimal state.
     * Cons:
     *   - Doesn't directly tell you WHICH machine ran which task (heap-based
     *     Approach 3 is better if that's needed as a follow-up).
     * When to use:
     *   - Default choice when only the COUNT is required, which is exactly
     *     what this problem asks for. This is what I'd write first in the
     *     interview.
     * ------------------------------------------------------------------------
     */
    static int minMachinesTwoPointerSweep(int[][] tasks) {
        int n = tasks.length;
        if (n == 0) return 0;

        int[] startTimes = new int[n];
        int[] endTimes = new int[n];
        for (int i = 0; i < n; i++) {
            startTimes[i] = tasks[i][0];
            endTimes[i] = tasks[i][1];
        }
        Arrays.sort(startTimes);
        Arrays.sort(endTimes);

        int activeMachines = 0;
        int maxActiveMachines = 0;
        int startPointer = 0;
        int endPointer = 0;

        while (startPointer < n) {
            // CRITICAL BOUNDARY RULE: process an end event whose time is
            // <= the current start event's time FIRST — because a task
            // ending at time t frees its machine in time for a task that
            // starts at that same time t (half-open interval semantics).
            if (endTimes[endPointer] <= startTimes[startPointer]) {
                activeMachines--;      // a machine just freed up
                endPointer++;
            } else {
                activeMachines++;      // this start event needs a machine
                maxActiveMachines = Math.max(maxActiveMachines, activeMachines);
                startPointer++;
            }
        }

        return maxActiveMachines;
    }


    /*
     * ------------------------------------------------------------------------
     * APPROACH 5: Difference Array / Bucket Counting (exploits bounded
     * end_i <= 10^4 from the constraints — a constraint-aware optimization)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Since 0 <= start < end <= 10^4, the number of distinct time points is
     *   small and BOUNDED regardless of n. Build a difference array `delta`
     *   over the time domain: delta[start]++ (a task begins, demand +1) and
     *   delta[end]-- (a task ends, demand -1, since [start,end) frees exactly
     *   at `end`). Prefix-summing `delta` at every time point gives the
     *   number of concurrently active tasks at that instant; the answer is
     *   the maximum prefix sum. This is the same "bucket counting" pattern
     *   filed for LC 1094 (Car Pooling) in my pattern library — preferred
     *   over the heap when coordinate bounds are small.
     *
     * Data structure / paradigm: Difference array (bucket counting / sweep).
     *
     * Time Complexity: O(n + maxTime) where maxTime = 10^4 here — this beats
     *   O(n log n) whenever n is large relative to maxTime, and is at worst
     *   comparable given the problem's n <= 10^3, maxTime <= 10^4 bounds.
     * Space Complexity: O(maxTime) — a fixed-size array of 10,001 buckets,
     *   independent of n.
     *
     * Pros:
     *   - No sorting, no heap, no comparator — pure array indexing; fastest
     *     in practice for these exact constraints, and dead simple to prove
     *     correct.
     *   - Directly shows the interviewer I noticed and exploited the tight
     *     bound in the constraints (0 <= start < end <= 10^4) — this is a
     *     strong signal of attentiveness in a real interview.
     * Cons:
     *   - Does NOT generalize if end_i were relaxed to, say, 10^9 or
     *     floating-point times — the array would become infeasible. In that
     *     case Approach 6 (TreeMap sweep / coordinate compression) is the
     *     fallback.
     * When to use:
     *   - Whenever the interviewer confirms the time domain is small and
     *     bounded (as it explicitly is here). I would mention this as a
     *     "given the stated constraints, I can actually do better" remark
     *     AFTER presenting the general O(n log n) solution, to show range
     *     without prematurely over-fitting to the specific bound.
     * ------------------------------------------------------------------------
     */
    static int minMachinesDifferenceArray(int[][] tasks) {
        if (tasks == null || tasks.length == 0) return 0;

        final int MAX_TIME = 10_001; // covers end_i up to 10^4 inclusive, plus 1 slot
        int[] delta = new int[MAX_TIME + 1];

        for (int[] task : tasks) {
            int start = task[0];
            int end = task[1];
            delta[start]++;   // demand increases by 1 starting at `start`
            delta[end]--;     // demand decreases by 1 exactly at `end`
                               // (half-open interval -> frees in time to
                               // serve another task starting at `end`)
        }

        int activeMachines = 0;
        int maxActiveMachines = 0;
        for (int time = 0; time <= MAX_TIME; time++) {
            activeMachines += delta[time];
            maxActiveMachines = Math.max(maxActiveMachines, activeMachines);
        }

        return maxActiveMachines;
    }


    /*
     * ------------------------------------------------------------------------
     * APPROACH 6: TreeMap Sweep Line (generalization for large / sparse /
     * non-integer time domains — the fallback when Approach 5's bound
     * assumption is relaxed)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Identical logic to the difference array, but instead of a fixed-size
     *   array indexed directly by time, use a TreeMap<Long, Integer> keyed
     *   by the actual time values (only the O(n) time values that actually
     *   appear are stored). Iterating the TreeMap in sorted key order gives
     *   the same prefix-sum sweep, but works for ANY time domain — huge
     *   integers, negative times, or (with a Comparator) even
     *   floating-point/timestamp types.
     *
     * Data structure / paradigm: TreeMap sweep line (sorted-key sweep,
     *   coordinate compression in spirit).
     *
     * Time Complexity: O(n log n) — n TreeMap insertions/updates at
     *   O(log n) each, then an O(n) traversal of the map in sorted order.
     * Space Complexity: O(n) — at most 2n distinct keys.
     *
     * Pros:
     *   - Fully general: no assumption whatsoever on the magnitude or
     *     density of time values.
     *   - Same conceptual simplicity as the difference array, just swapping
     *     "array index" for "sorted map key."
     * Cons:
     *   - Strictly worse constant factors than Approach 5 when the bound IS
     *     small (as it is here) — O(n log n) with tree-node overhead vs.
     *     O(n + maxTime) with flat array access.
     * When to use:
     *   - This is the approach I'd pivot to if the interviewer said "now
     *     assume end_i can be up to 10^18" or "times are floating-point
     *     timestamps" as a follow-up (see Section 12).
     * ------------------------------------------------------------------------
     */
    static int minMachinesTreeMapSweep(int[][] tasks) {
        if (tasks == null || tasks.length == 0) return 0;

        // TreeMap<time, netDemandChangeAtThatTime>
        TreeMap<Integer, Integer> delta = new TreeMap<>();

        for (int[] task : tasks) {
            int start = task[0];
            int end = task[1];
            delta.merge(start, 1, Integer::sum);   // demand +1 at start
            delta.merge(end, -1, Integer::sum);    // demand -1 at end
        }

        int activeMachines = 0;
        int maxActiveMachines = 0;
        // TreeMap.entrySet() iterates in ascending key order automatically.
        for (int netChange : delta.values()) {
            activeMachines += netChange;
            maxActiveMachines = Math.max(maxActiveMachines, activeMachines);
        }

        return maxActiveMachines;
    }


    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     * Approach                       | Time            | Space        | Best For                                  | Limitations
     * --------------------------------|-----------------|--------------|-------------------------------------------|------------------------------------------
     * 1. Brute Force Simulation       | O(n^2)          | O(n)         | Warm-up / building intuition               | Too slow to be the final answer
     * 2. Sorting + Binary Search      | O(n log n)      | O(n)         | Want sorted-structure access to free times | More machinery than sweep for count-only
     * 3. Sorting + Min-Heap           | O(n log n)      | O(n)         | Textbook answer; need machine IDs/online   | Heap constant factor > two-pointer sweep
     * 4. Two-Pointer Sweep (RECOMMENDED)| O(n log n)    | O(n)         | Count-only, cleanest, lowest overhead      | No machine-ID assignment out of the box
     * 5. Difference Array (bucket)    | O(n + maxTime)  | O(maxTime)   | Exploiting the given 10^4 time bound        | Breaks if time domain is relaxed/huge
     * 6. TreeMap Sweep                | O(n log n)      | O(n)         | Arbitrary/huge/sparse time domains         | Slower constants than Approach 5 when bound is small
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     * I would present APPROACH 4 (Two-Pointer Chronological Sweep) as my
     * final answer, for these reasons:
     *
     *   1. Clarity: it's the easiest of the O(n log n) solutions to explain
     *      on a whiteboard — "walk through time, track how many tasks are
     *      simultaneously alive, report the peak" is intuitive and visual.
     *   2. Coding speed: no heap API subtleties (comparator direction, peek
     *      vs poll ordering) — just two sorted arrays and two pointers.
     *   3. Interviewer expectations: this problem is a well-known pattern
     *      (interval partitioning / Meeting Rooms II); interviewers expect
     *      either the heap or the sweep solution at O(n log n), and the
     *      sweep is generally regarded as the more elegant of the two when
     *      only a count is needed.
     *   4. Optimality: O(n log n) is optimal in the comparison-sort model
     *      for this problem (you must at minimum sort the events).
     *
     * I would ALSO proactively mention Approach 5 (difference array) once
     * the primary solution is accepted — "given the problem explicitly
     * bounds end_i <= 10^4, I can trade the O(n log n) sort for an O(n +
     * maxTime) counting pass over a fixed array, which is faster here since
     * maxTime is a small constant relative to n." This demonstrates breadth
     * without over-engineering the first pass at the problem.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE — PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ========================================================================
     * Polished version of Approach 4, with full defensive checks and comments
     * explaining every decision, as I would write it live in the interview
     * after whiteboarding the plan.
     * ========================================================================
     */
    static int minMachinesRequired(int[][] tasks) {
        // Defensive guard: constraints promise length >= 1, but real
        // production code should not assume the caller respects that.
        if (tasks == null || tasks.length == 0) {
            return 0;
        }

        int taskCount = tasks.length;

        // Step 1: Extract start times and end times into two separate arrays.
        // We deliberately do NOT sort the original 2D array in place — we
        // only need the two 1D projections, sorted independently. This is
        // valid because we're counting concurrent demand, not tracking which
        // specific task maps to which specific machine.
        int[] startTimes = new int[taskCount];
        int[] endTimes = new int[taskCount];
        for (int i = 0; i < taskCount; i++) {
            startTimes[i] = tasks[i][0];
            endTimes[i] = tasks[i][1];
        }

        // Step 2: Sort both arrays independently. This gives us the
        // chronological order of "task begins" events and "task ends"
        // events, which is all the sweep needs — the pairing between a
        // specific start and its specific end is irrelevant to the COUNT.
        Arrays.sort(startTimes);
        Arrays.sort(endTimes);

        // Step 3: Sweep through events in time order using two pointers.
        int activeMachineCount = 0;   // machines currently busy
        int peakMachineCount = 0;     // running maximum -> final answer
        int startPointer = 0;         // index into startTimes
        int endPointer = 0;           // index into endTimes

        while (startPointer < taskCount) {
            int nextStart = startTimes[startPointer];
            int nextEnd = endTimes[endPointer];

            // BOUNDARY RULE (the single most important line in this
            // solution): if a task ends at exactly the same time another
            // begins, retire the finishing machine FIRST. This correctly
            // encodes the half-open [start, end) semantics confirmed in
            // Section 2, Q1 — touching endpoints do NOT require a new
            // machine.
            if (nextEnd <= nextStart) {
                activeMachineCount--;
                endPointer++;
            } else {
                activeMachineCount++;
                peakMachineCount = Math.max(peakMachineCount, activeMachineCount);
                startPointer++;
            }
        }

        return peakMachineCount;
    }


    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     * Tracing minMachinesRequired() on Example 1 from Section 3:
     *   tasks = [[0,3],[2,6],[5,8],[7,10]]
     *
     * startTimes sorted = [0, 2, 5, 7]
     * endTimes   sorted = [3, 6, 8, 10]
     *
     * Initial state: activeMachineCount = 0, peakMachineCount = 0,
     *                startPointer = 0, endPointer = 0
     *
     * Step | nextStart | nextEnd | Decision                | active | peak | startPtr | endPtr
     * -----|-----------|---------|--------------------------|--------|------|----------|-------
     *  1   | 0         | 3       | 3 > 0 -> START event      |   1    |  1   |    1     |   0
     *  2   | 2         | 3       | 3 > 2 -> START event      |   2    |  2   |    2     |   0
     *  3   | 5         | 3       | 3 <= 5 -> END event        |   1    |  2   |    2     |   1
     *  4   | 5         | 6       | 6 > 5 -> START event      |   2    |  2   |    3     |   1
     *  5   | 7         | 6       | 6 <= 7 -> END event        |   1    |  2   |    3     |   2
     *  6   | 7         | 8       | 8 > 7 -> START event      |   2    |  2   |    4     |   2
     *
     * Loop ends: startPointer (4) == taskCount (4).
     * Return peakMachineCount = 2.  <-- Matches expected answer from Sec. 3.
     *
     * Sanity check against the touching-endpoint edge case:
     *   tasks = [[0,5],[5,10],[10,15]]
     *   startTimes = [0,5,10], endTimes = [5,10,15]
     *   Step 1: nextStart=0, nextEnd=5 -> 5>0 -> START, active=1, peak=1
     *   Step 2: nextStart=5, nextEnd=5 -> 5<=5 -> END FIRST, active=0
     *   Step 2b: nextStart=5, nextEnd=10 -> 10>5 -> START, active=1, peak=1
     *   Step 3: nextStart=10, nextEnd=10 -> 10<=10 -> END FIRST, active=0
     *   Step 3b: nextStart=10, nextEnd=15 -> 15>10 -> START, active=1, peak=1
     *   Return peak=1. Confirms same-time touching does NOT need a 2nd
     *   machine, as required by the half-open interval semantics.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     * - The problem reduces to "maximum number of intervals simultaneously
     *   active," a classic interval-scheduling / interval-graph-coloring
     *   result.
     * - All O(n log n) approaches (binary search, heap, two-pointer sweep,
     *   TreeMap sweep) are asymptotically equivalent; I chose the two-pointer
     *   sweep for minimal code and clearest mental model when only a COUNT is
     *   needed.
     * - The difference-array approach is strictly faster given THIS
     *   problem's explicit constraint (end_i <= 10^4), trading generality for
     *   speed — a valid and expected trade-off to surface once bounds are
     *   confirmed.
     * - Known limitation of the final solution: minMachinesRequired() reports
     *   only the COUNT, not a concrete task-to-machine assignment. Section 12
     *   covers how to extend it if that's required.
     * - Assumption baked into the final solution: half-open interval
     *   semantics (touching endpoints do not conflict), confirmed in
     *   Section 2. If that assumption is wrong, only the comparison operator
     *   in the boundary rule (<= vs <) needs to flip — everything else is
     *   unchanged.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     * 1. "Can you also return the actual assignment of tasks to machines,
     *    not just the count?" -> Switch to Approach 3 (min-heap of
     *    (freeTime, machineId) pairs); when reusing, record which machine ID
     *    served each task.
     *
     * 2. "What if tasks arrive one at a time in an online/streaming fashion,
     *    and you must answer 'machines needed so far' after each arrival?"
     *    -> The two-pointer sweep needs the full array sorted upfront, so it
     *    doesn't work online. The min-heap (Approach 3) DOES work online:
     *    process each new task against the heap immediately.
     *
     * 3. "What if end_i can be as large as 10^9 or even floating-point
     *    timestamps?" -> The difference-array approach (Approach 5) breaks
     *    (array too large); fall back to the TreeMap sweep (Approach 6),
     *    which has no dependency on the magnitude of time values.
     *
     * 4. "What if touching intervals DO count as a conflict (closed
     *    intervals [start, end])?" -> Flip the boundary comparison from
     *    `nextEnd <= nextStart` to `nextEnd < nextStart`... more precisely,
     *    process START before END when they tie, e.g. `nextEnd < nextStart`
     *    for the end-first branch (or add a small epsilon-based tie-break).
     *
     * 5. "Each machine now has a maximum number of tasks it can run per day
     *    (capacity constraint) — how does that change the algorithm?" -> No
     *    longer pure interval partitioning; becomes a bin-packing-flavored
     *    problem, likely requiring a greedy-with-heap-of-remaining-capacity
     *    or flow-based formulation.
     *
     * 6. "Can you parallelize this computation for very large n (say 10^8
     *    tasks)?" -> The difference-array/bucket approach parallelizes
     *    cleanly: partition tasks across workers, build partial delta
     *    arrays, sum them (associative), then do a single sequential prefix
     *    scan over the bounded time domain.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     * 1. Half-open vs. closed interval confusion (THE classic trap here):
     *    treating a task ending at time t and another starting at time t as
     *    overlapping, which over-counts machines. Always confirm this with
     *    the interviewer explicitly — don't assume.
     *
     * 2. Sorting only ONE of the two arrays (e.g. sorting by start but
     *    forgetting that end times must ALSO be sorted independently for the
     *    two-pointer sweep to be correct) — the sweep relies on both arrays
     *    being individually monotonic, NOT on preserving the original
     *    start-end pairing.
     *
     * 3. Off-by-one on the difference-array bucket size: forgetting that
     *    `end` can equal the array's upper bound (10^4) and indexing out of
     *    bounds, or under-allocating the array by exactly one slot.
     *
     * 4. Conflating "final heap/array size" with "peak size": in the
     *    min-heap approach, the ANSWER is the heap's size after processing
     *    all tasks (since we only grow it when reuse fails) — candidates
     *    sometimes mistakenly try to track a separate running maximum for
     *    the heap approach, which is unnecessary and can introduce bugs if
     *    done incorrectly (e.g., using heap.size() as a proxy for
     *    concurrently-active before the current task is even pushed).
     * ========================================================================
     */


    /*
     * ========================================================================
     * VERIFICATION HARNESS: assertion tests + randomized stress test
     * cross-validating all approaches against the brute-force oracle.
     * Run with: java -ea MinimumMachinesScheduling.java
     * ========================================================================
     */
    public static void main(String[] args) {
        runNamedAssertions();
        runRandomizedStressTest(3000, 12345L);
        System.out.println("All tests passed across all six approaches.");
    }

    private static void runNamedAssertions() {
        // Example 1: normal overlapping case.
        int[][] example1 = {{0, 3}, {2, 6}, {5, 8}, {7, 10}};
        assertAllApproaches("Example1_NormalOverlap", example1, 2);

        // Example 2a: single task.
        int[][] example2a = {{1, 4}};
        assertAllApproaches("Example2a_SingleTask", example2a, 1);

        // Example 2b: touching endpoints chain -> must reuse across all.
        int[][] example2b = {{0, 5}, {5, 10}, {10, 15}};
        assertAllApproaches("Example2b_TouchingEndpoints", example2b, 1);

        // Example 3a: fully identical, fully overlapping intervals.
        int[][] example3a = {{1, 5}, {1, 5}, {1, 5}};
        assertAllApproaches("Example3a_IdenticalIntervals", example3a, 3);

        // Example 3b: one long task spans several short disjoint tasks.
        int[][] example3b = {{1, 10}, {2, 3}, {4, 5}, {6, 7}};
        assertAllApproaches("Example3b_LongSpanningTask", example3b, 2);

        // Boundary: minimal possible interval width (start = end - 1).
        int[][] minimalWidth = {{0, 1}, {0, 1}, {1, 2}};
        assertAllApproaches("MinimalWidthBoundary", minimalWidth, 2);

        System.out.println("Named assertions passed.");
    }

    private static void assertAllApproaches(String label, int[][] tasks, int expected) {
        assert minMachinesBruteForce(tasks) == expected
                : label + " failed on Brute Force";
        assert minMachinesBinarySearch(tasks) == expected
                : label + " failed on Binary Search";
        assert minMachinesHeap(tasks) == expected
                : label + " failed on Min-Heap";
        assert minMachinesTwoPointerSweep(tasks) == expected
                : label + " failed on Two-Pointer Sweep";
        assert minMachinesDifferenceArray(tasks) == expected
                : label + " failed on Difference Array";
        assert minMachinesTreeMapSweep(tasks) == expected
                : label + " failed on TreeMap Sweep";
        assert minMachinesRequired(tasks) == expected
                : label + " failed on production-quality final solution";
    }

    private static void runRandomizedStressTest(int trialCount, long seed) {
        Random random = new Random(seed);

        for (int trial = 0; trial < trialCount; trial++) {
            int taskCount = 1 + random.nextInt(60); // small n keeps brute force fast
            int[][] tasks = new int[taskCount][2];
            for (int i = 0; i < taskCount; i++) {
                int start = random.nextInt(50);
                int end = start + 1 + random.nextInt(50); // guarantees start < end
                tasks[i][0] = start;
                tasks[i][1] = end;
            }

            int expected = minMachinesBruteForce(tasks); // oracle
            int binarySearchResult = minMachinesBinarySearch(tasks);
            int heapResult = minMachinesHeap(tasks);
            int sweepResult = minMachinesTwoPointerSweep(tasks);
            int diffArrayResult = minMachinesDifferenceArray(tasks);
            int treeMapResult = minMachinesTreeMapSweep(tasks);
            int finalResult = minMachinesRequired(tasks);

            assert binarySearchResult == expected
                    : "Mismatch (BinarySearch) on trial " + trial + ": " + Arrays.deepToString(tasks);
            assert heapResult == expected
                    : "Mismatch (Heap) on trial " + trial + ": " + Arrays.deepToString(tasks);
            assert sweepResult == expected
                    : "Mismatch (Sweep) on trial " + trial + ": " + Arrays.deepToString(tasks);
            assert diffArrayResult == expected
                    : "Mismatch (DiffArray) on trial " + trial + ": " + Arrays.deepToString(tasks);
            assert treeMapResult == expected
                    : "Mismatch (TreeMap) on trial " + trial + ": " + Arrays.deepToString(tasks);
            assert finalResult == expected
                    : "Mismatch (Final) on trial " + trial + ": " + Arrays.deepToString(tasks);
        }

        System.out.println("Randomized stress test passed (" + trialCount + " trials).");
    }
}

/**
 * ============================================================================
 * PROBLEM STATEMENT
 * ============================================================================
 * We are given an input array, tasks, where tasks[i] = [start_i, end_i]
 * represents the start and end times of n tasks. Our goal is to schedule 
 * these tasks on machines given the following criteria:
 * 1. A machine can execute only one task at a time.
 * 2. A machine can begin executing a new task immediately after completing 
 *    the previous one.
 * 3. An unlimited number of machines are available.
 * 
 * Find the minimum number of machines required to complete these n tasks.
 * 
 * CONSTRAINTS:
 * - 1 <= tasks.length <= 10^3
 * - 0 <= tasks[i].start < tasks[i].end <= 10^4
 * 
 * ============================================================================
 * VISUALIZATION OF THE PROBLEM
 * ============================================================================
 * Imagine tasks as blocks of time on a timeline. When tasks overlap, they 
 * cannot share the same machine. The maximum number of overlapping tasks at 
 * any single point in time dictates the minimum number of machines required.
 * 
 * Example: tasks = [[0, 30], [5, 10], [15, 20]]
 * 
 * Timeline:
 * 0    5    10   15   20   25   30
 * |----|----|----|----|----|----|
 * [-----------------------------] Task 1 (Machine 1)
 *      [----]                     Task 2 (Machine 2)
 *                [----]           Task 3 (Machine 2 - reused)
 * 
 * Max overlap is 2 (at time 5-10, and 15-20). So, 2 machines are needed.
 * ============================================================================
 */
class TaskScheduler {

    /**
     * Using Java 14+ Records to represent a Task.
     * This provides a clean, immutable data carrier with built-in accessors,
     * equals(), hashCode(), and toString() methods.
     */
    public record Task(int start, int end) implements Comparable<Task> {
        @Override
        public int compareTo(Task other) {
            // Sort primarily by start time. If start times are equal, by end time.
            if (this.start != other.start) {
                return Integer.compare(this.start, other.start);
            }
            return Integer.compare(this.end, other.end);
        }
    }

    /**
     * ========================================================================
     * SOLUTION 1: MIN-HEAP (Priority Queue)
     * ========================================================================
     * EXPLANATION:
     * 1. Sort the tasks based on their start times.
     * 2. Use a Min-Heap to keep track of the *end times* of tasks currently 
     *    running on machines. The top of the heap is the machine that will 
     *    become free the earliest.
     * 3. Iterate through the sorted tasks:
     *    - If the earliest freeing machine (heap top) is free before or at 
     *      the start of the current task, we can reuse it! Remove the top element.
     *    - Add the current task's end time to the heap (either taking over the 
     *      reused machine or taking a new one).
     * 4. The size of the heap at the end represents the number of machines needed.
     * 
     * COMPLEXITY:
     * - Time: O(N log N) - Sorting takes O(N log N), and each heap insertion/
     *   extraction takes O(log N).
     * - Space: O(N) - In the worst case (all overlapping), the heap stores N elements.
     * ========================================================================
     */
    public static int minMachinesMinHeap(int[][] tasksArray) {
        if (tasksArray == null || tasksArray.length == 0) return 0;

        // Convert 2D array to an array of Task records for cleaner modern Java
        Task[] tasks = new Task[tasksArray.length];
        for (int i = 0; i < tasksArray.length; i++) {
            tasks[i] = new Task(tasksArray[i][0], tasksArray[i][1]);
        }

        // Sort tasks by start time
        Arrays.sort(tasks);

        // Min-heap to store the end times of tasks running on machines
        PriorityQueue<Integer> activeMachines = new PriorityQueue<>();

        for (Task task : tasks) {
            // If the machine that finishes earliest is done before or exactly when 
            // the current task starts, we can reuse it.
            if (!activeMachines.isEmpty() && activeMachines.peek() <= task.start()) {
                activeMachines.poll(); // Free the machine
            }
            // Allocate the current task to a machine (reused or new)
            activeMachines.offer(task.end());
        }

        return activeMachines.size();
    }

    /**
     * ========================================================================
     * SOLUTION 2: CHRONOLOGICAL ORDERING (Two Pointers)
     * ========================================================================
     * EXPLANATION:
     * 1. Extract all start times into one array and all end times into another.
     * 2. Sort both arrays independently. We only care about chronological events
     *    (when a task starts vs when *any* task ends), not which task ends when.
     * 3. Use two pointers (one for starts, one for ends).
     * 4. If a task starts before the earliest ending task finishes, we need a 
     *    new machine.
     * 5. If a task starts after or at the exact time an older task finishes, 
     *    a machine frees up, so we can reuse it. We move the end pointer.
     * 
     * VISUAL:
     * Starts: [0, 5, 15]
     * Ends:   [10, 20, 30]
     * 
     * Pointers: s=0, e=0. 
     * starts[s]=0 < ends[e]=10 -> need machine (machines=1). s++
     * starts[s]=5 < ends[e]=10 -> need machine (machines=2). s++
     * starts[s]=15 >= ends[e]=10 -> machine freed! ends[e] processed. e++, s++
     * 
     * COMPLEXITY:
     * - Time: O(N log N) - Sorting the two arrays.
     * - Space: O(N) - Storing the start and end arrays.
     * ========================================================================
     */
    public static int minMachinesChronological(int[][] tasks) {
        if (tasks == null || tasks.length == 0) return 0;

        int n = tasks.length;
        int[] starts = new int[n];
        int[] ends = new int[n];

        for (int i = 0; i < n; i++) {
            starts[i] = tasks[i][0];
            ends[i] = tasks[i][1];
        }

        Arrays.sort(starts);
        Arrays.sort(ends);

        int startPointer = 0;
        int endPointer = 0;
        int machinesRequired = 0;
        int maxMachines = 0;

        while (startPointer < n) {
            // If a task starts before the previous one ends, we need an extra machine
            if (starts[startPointer] < ends[endPointer]) {
                machinesRequired++;
                maxMachines = Math.max(maxMachines, machinesRequired);
                startPointer++;
            } else {
                // A task ended, so a machine is freed up
                machinesRequired--;
                endPointer++;
            }
        }

        return maxMachines;
    }

    /**
     * ========================================================================
     * SOLUTION 3: LINE SWEEP (Difference Array)
     * ========================================================================
     * EXPLANATION:
     * Because the problem constraints state that tasks[i].end <= 10^4, we can 
     * use an array to track the timeline instead of sorting.
     * 
     * 1. Create an array `timeline` of size 10005.
     * 2. For every task [start, end], increment `timeline[start]` by 1 (a machine 
     *    is taken) and decrement `timeline[end]` by 1 (a machine is released).
     * 3. Sweep through the `timeline` array from left to right, maintaining a 
     *    running sum. The maximum value of this running sum at any point is 
     *    the minimum number of machines required.
     * 
     * VISUAL:
     * Tasks: [1, 4], [2, 5], [4, 6]
     * Timeline Array changes:
     * Index:   0   1   2   3   4   5   6
     * Change:  0  +1  +1   0  -1+1 -1  -1  (Note at index 4: one ends (-1), one starts (+1))
     * Net:     0  +1  +1   0   0  -1  -1
     * 
     * Running Sum:
     * Index:   0   1   2   3   4   5   6
     * Sum:     0   1   2   2   2   1   0
     * Max sum is 2. (2 machines needed)
     * 
     * COMPLEXITY:
     * - Time: O(N + K) where K is the maximum possible end time (10^4). 
     *   Since K is small, this acts like O(N), making it exceptionally fast!
     * - Space: O(K) for the timeline array.
     * ========================================================================
     */
    public static int minMachinesLineSweep(int[][] tasks) {
        if (tasks == null || tasks.length == 0) return 0;

        // Constraint says end <= 10^4, so array size of 10005 is safe.
        int MAX_TIME = 10005;
        int[] timeline = new int[MAX_TIME];

        for (int[] task : tasks) {
            timeline[task[0]]++; // Machine acquired
            timeline[task[1]]--; // Machine released
        }

        int currentMachines = 0;
        int maxMachines = 0;

        // Sweep across the timeline
        for (int change : timeline) {
            currentMachines += change;
            if (currentMachines > maxMachines) {
                maxMachines = currentMachines;
            }
        }

        return maxMachines;
    }

    /**
     * ========================================================================
     * MAIN METHOD: Executing and verifying the examples
     * ========================================================================
     */
    public static void main(String[] args) {
        int[][] test1 = {{0, 30}, {5, 10}, {15, 20}};
        int[][] test2 = {{7, 10}, {2, 4}};
        int[][] test3 = {{1, 5}, {2, 6}, {4, 8}, {5, 7}}; // overlapping heavily
        
        System.out.println("Test Case 1: [[0, 30], [5, 10], [15, 20]]");
        System.out.println("Expected: 2");
        System.out.println("Heap Solution:          " + minMachinesMinHeap(test1));
        System.out.println("Chronological Solution: " + minMachinesChronological(test1));
        System.out.println("Line Sweep Solution:    " + minMachinesLineSweep(test1));
        System.out.println("--------------------------------------------------");

        System.out.println("Test Case 2: [[7, 10], [2, 4]]");
        System.out.println("Expected: 1");
        System.out.println("Heap Solution:          " + minMachinesMinHeap(test2));
        System.out.println("Chronological Solution: " + minMachinesChronological(test2));
        System.out.println("Line Sweep Solution:    " + minMachinesLineSweep(test2));
        System.out.println("--------------------------------------------------");

        System.out.println("Test Case 3: [[1, 5], [2, 6], [4, 8], [5, 7]]");
        System.out.println("Expected: 3");
        System.out.println("Heap Solution:          " + minMachinesMinHeap(test3));
        System.out.println("Chronological Solution: " + minMachinesChronological(test3));
        System.out.println("Line Sweep Solution:    " + minMachinesLineSweep(test3));
    }
}

class MinimumMachines {

    // Record is a clean way to represent one task.
    record Task(int start, int end) {}

    public int minMachines(int[][] tasks) {

        /*
         * Convert int[][] → Task[]
         *
         * Example:
         * [1, 4] → Task(1, 4)
         * [2, 5] → Task(2, 5)
         *
         * We also sort by start time in the same stream.
         *
         * After this:
         * Task[] is sorted by increasing start time.
         */
        Task[] sortedTasks = Arrays.stream(tasks)
                .map(task -> new Task(task[0], task[1]))
                .sorted(Comparator.comparingInt(Task::start))
                .toArray(Task[]::new);

        /*
         * Min heap containing the END times of tasks
         * currently running on machines.
         *
         * The smallest end time is always at the top.
         *
         * Why do we need the earliest end time?
         *
         * Suppose:
         *
         * Machine 1 → finishes at 4
         * Machine 2 → finishes at 7
         *
         * New task starts at 5.
         *
         * Machine 1 is the best machine to reuse because
         * it became available at time 4.
         */
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();

        int maxMachines = 0;

        for (Task task : sortedTasks) {

            /*
             * If the machine that becomes free earliest
             * is available before this task starts,
             * we can reuse that machine.
             *
             * <= is important.
             *
             * Example:
             *
             * Existing task: [1, 4]
             * New task:      [4, 7]
             *
             * At time 4, the first task has finished and
             * the second task can immediately start.
             *
             * Therefore, we reuse the same machine.
             */
            if (!minHeap.isEmpty() && minHeap.peek() <= task.start()) {
                minHeap.poll();
            }

            /*
             * This task now occupies a machine.
             *
             * Store its end time so that we know when
             * this machine becomes available again.
             */
            minHeap.offer(task.end());

            /*
             * Heap size = number of machines currently
             * occupied.
             *
             * We need the maximum number of machines that
             * were simultaneously occupied.
             */
            maxMachines = Math.max(maxMachines, minHeap.size());
        }

        return maxMachines;
    }
}
