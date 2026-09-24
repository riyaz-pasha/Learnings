import java.util.*;

/*
 * ================================================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: "MEETING ROOMS II"
 * ================================================================================================
 *
 * This single file walks through the entire interview lifecycle for the problem below, exactly
 * as it should be presented in a real onsite/virtual Google interview: understanding, examples,
 * a full sweep of applicable paradigms, complexity analysis, a production-quality optimal
 * solution, a manual trace, and interviewer follow-ups.
 *
 * Every "section" below is a labeled block comment. Runnable code lives in the class body.
 * ================================================================================================
 */

class MeetingRoomsII {

    /*
     * ============================================================================================
     * SECTION 1 — RESTATE THE PROBLEM
     * ============================================================================================
     *
     * In my own words:
     *   I'm given a list of meetings, each with a start time and an end time. I need to figure out
     *   the minimum number of conference rooms required so that every meeting can be scheduled
     *   without two meetings using the same room at the same time.
     *
     * Key detail that changes the problem subtly:
     *   The end time is EXCLUSIVE. That means a meeting [1, 5) occupies the room during
     *   [1, 5) — NOT including instant 5. So a meeting [5, 10) can start the very moment the
     *   first one ends, using the SAME room. Two meetings [1,5) and [5,10) do NOT conflict.
     *   This affects every comparison I write: I must treat "end == start" as "no overlap"
     *   (i.e., use <=, not <, when freeing a room at a boundary).
     *
     * Inputs:
     *   - intervals: int[][] where intervals[i] = {start_i, end_i}
     *   - 1 <= intervals.length <= 10^3
     *   - 0 <= start_i < end_i <= 10^6
     *   - Implicitly: start_i < end_i is guaranteed (no zero-length or malformed meetings)
     *
     * Output:
     *   - A single integer: the minimum number of rooms needed to host all meetings simultaneously
     *     with no room double-booked at any instant.
     *
     * Assumptions I'll state explicitly (and validate in Section 2):
     *   - Meetings are 1-indexed conceptually but stored 0-indexed in the array.
     *   - The array is NOT guaranteed to be sorted by start time.
     *   - There is no requirement to report WHICH meeting goes in WHICH room — just the count.
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 2 — CLARIFYING QUESTIONS (with assumed answers)
     * ============================================================================================
     *
     * 1. Q: Is the end time strictly exclusive for ALL meetings, or could there be a mix?
     *    A: Exclusive for all meetings, consistently. [1,5) and [5,9) never conflict.
     *
     * 2. Q: Can `intervals` be empty?
     *    A: Constraints say length >= 1, but I'll defensively handle an empty array by
     *       returning 0 rather than throwing.
     *
     * 3. Q: Can two meetings have identical start and end times (exact duplicates)?
     *    A: Yes, duplicates are allowed and should be treated as a genuine conflict
     *       (they need separate rooms since they fully overlap).
     *
     * 4. Q: Are start/end times always non-negative integers, and is there an upper bound I can
     *       exploit (e.g., for a counting/bucket approach)?
     *    A: Yes — 0 <= start < end <= 10^6. That bound is small enough to make a sweep-line /
     *       difference-array approach with an array of size ~10^6 viable in both time and space.
     *
     * 5. Q: Do I need to return which room each meeting is assigned to, or just the count?
     *    A: Just the minimum count of rooms.
     *
     * 6. Q: Is the input array mutable — am I allowed to sort it in place, or must I preserve
     *       the original order for the caller?
     *    A: Assume I should NOT mutate the caller's array; I'll copy indices/times before sorting.
     *
     * 7. Q: Should the solution be thread-safe / handle concurrent calls?
     *    A: No concurrency requirement — this is a single-threaded, single-call computation.
     *
     * 8. Q: What's the expected scale in a follow-up (is n=10^3 the true ceiling, or could this
     *       be pushed to 10^5–10^6 in a follow-up)?
     *    A: For this problem n <= 10^3, so even an O(n^2) approach technically passes, but I'll
     *       still aim for the optimal O(n log n) solution since that's what's expected, and I'll
     *       discuss how it scales in the follow-up section.
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 3 — EXAMPLES & EDGE CASES
     * ============================================================================================
     *
     * Example 1 (Normal case):
     *   intervals = [[0,30], [5,10], [15,20]]
     *   - [0,30) is one long meeting.
     *   - [5,10) and [15,20) both fall inside [0,30) but don't overlap each other.
     *   - At time 5..10, two meetings are active ([0,30) and [5,10)) -> need 2 rooms.
     *   - At time 15..20, two meetings are active ([0,30) and [15,20)) -> still 2 rooms.
     *   Answer: 2
     *
     * Example 2 (Edge case — no overlap at all, touching boundaries):
     *   intervals = [[1,5], [5,10], [10,15]]
     *   - Because end is exclusive, [1,5) ends exactly when [5,10) starts — same room reusable.
     *   - Same logic for [5,10) -> [10,15).
     *   Answer: 1
     *   This is the case most candidates get WRONG if they use strict "<" instead of "<=" when
     *   comparing end-of-one-meeting to start-of-next during the sweep.
     *
     * Example 3 (Boundary / tie-breaking case — everyone overlaps at one instant, plus duplicates):
     *   intervals = [[0,10], [0,10], [5,15], [5,7]]
     *   - At time 5..7, ALL FOUR meetings are simultaneously active.
     *   - Duplicates [0,10] and [0,10] each need their own room.
     *   Answer: 4
     *   This tests: (a) duplicate intervals count as independent conflicts, and
     *   (b) correct handling when multiple starts and ends collide at the same timestamp
     *   (tie-breaking rule: process ALL end events at a timestamp before start events at that
     *   same timestamp, consistent with the exclusive-end semantics).
     *
     * Additional edge cases I'll test in code (see main()):
     *   - Single meeting: [[5,10]]                     -> 1
     *   - All identical meetings: [[2,4],[2,4],[2,4]]   -> 3
     *   - All disjoint meetings: [[1,2],[3,4],[5,6]]    -> 1
     *   - Empty array: []                               -> 0
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 4 & 5 — ALL POSSIBLE APPROACHES (paradigm sweep)
     * ============================================================================================
     *
     * Paradigms considered and whether they apply:
     *
     *   Brute force / naive           -> APPLICABLE (Approach 1)
     *   Sorting-based                 -> APPLICABLE (Approach 2 — chronological ordering)
     *   Hashing / counting            -> APPLICABLE (Approach 4 — difference array over time domain)
     *   Two pointer / sliding window  -> APPLICABLE (Approach 2 IS a two-pointer sweep on sorted
     *                                     start/end arrays)
     *   Heap / priority queue         -> APPLICABLE (Approach 3 — classic textbook solution)
     *   Binary search                 -> Not a standalone paradigm here; at best it's a minor
     *                                     helper (e.g., binary-searching a sorted end-time list
     *                                     inside a room-count structure), but it doesn't change
     *                                     the asymptotic complexity or lead anywhere better than
     *                                     Approaches 2/3. Skipped as its own approach.
     *   Divide and conquer            -> NOT NATURALLY APPLICABLE. There's no clean way to merge
     *                                     "max overlap counts" from two halves of an unsorted
     *                                     interval list in less than the cost of just sorting the
     *                                     whole thing, so D&C buys nothing over sorting.
     *   Dynamic programming           -> NOT APPLICABLE. There's no optimal-substructure /
     *                                     overlapping-subproblems relationship here — the answer
     *                                     is a global maximum of concurrently active intervals,
     *                                     which is a counting/sweep problem, not a DP one.
     *   Tree / graph traversal        -> NOT NATURALLY APPLICABLE. Meetings aren't naturally
     *                                     nodes/edges of a graph we need to traverse; modeling it
     *                                     as an interval graph and finding max clique would be
     *                                     massive overkill for what a sweep line solves in
     *                                     O(n log n).
     *   Monotonic stack / deque       -> NOT APPLICABLE. Monotonic stacks solve "next greater/
     *                                     smaller element" style problems; there's no monotonic
     *                                     ordering property to exploit for counting concurrent
     *                                     intervals.
     *   Trie / segment tree / BIT     -> POSSIBLE BUT OVERKILL. A segment tree with range-add /
     *                                     range-max could track "concurrent meetings" over the
     *                                     time domain, matching the difference-array idea but
     *                                     with O(n log(maxTime)) instead of O(maxTime) — useful
     *                                     only if the time domain were far larger (e.g., 10^18)
     *                                     and had to be coordinate-compressed. Given max = 10^6,
     *                                     the plain difference array (Approach 4) already does
     *                                     this in O(maxTime) with a simpler implementation, so a
     *                                     segment tree adds complexity without benefit here.
     *
     * Four approaches are implemented below, from naive to optimal.
     * ============================================================================================
     */

    /*
     * --------------------------------------------------------------------------------------------
     * APPROACH 1: Brute Force — Pairwise Overlap Counting
     * --------------------------------------------------------------------------------------------
     * Core idea:
     *   For every meeting i, count how many OTHER meetings overlap with it at the instant it
     *   starts. The maximum such count (+1 for the meeting itself) across all meetings is a lower
     *   bound on rooms needed. Because "maximum simultaneous overlap" is always achieved at the
     *   start time of *some* meeting, checking overlaps at each meeting's start time is sufficient
     *   to find the true maximum concurrency.
     *
     * Data structure / paradigm: none beyond nested loops — pure brute force.
     *
     * Time Complexity: O(n^2) — for each of n meetings, scan all n meetings to count overlaps.
     * Space Complexity: O(1) extra (ignoring input storage).
     *
     * Pros:
     *   - Trivial to reason about and verify by hand; almost no room for implementation bugs.
     *   - Good as a warm-up / correctness baseline to cross-check optimal solutions against.
     * Cons:
     *   - Quadratic time — won't scale past a few thousand meetings.
     *   - Doesn't generalize well to follow-up variants (e.g., streaming input).
     *
     * When to use in practice:
     *   Only as a sanity-check baseline in tests, or if n is guaranteed tiny (e.g., n <= ~200).
     *   Never as the "final answer" in an interview once an optimal approach is expected.
     * --------------------------------------------------------------------------------------------
     */
    public static int minMeetingRoomsBruteForce(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }

        int maxConcurrentMeetings = 0;

        // For each meeting, treat its start time as a probe instant and count how many
        // meetings (including itself) are active at that instant.
        for (int probeIndex = 0; probeIndex < intervals.length; probeIndex++) {
            int probeInstant = intervals[probeIndex][0];
            int concurrentAtProbe = 0;

            for (int[] meeting : intervals) {
                int meetingStart = meeting[0];
                int meetingEnd = meeting[1]; // exclusive
                // A meeting is active at probeInstant if meetingStart <= probeInstant < meetingEnd.
                if (meetingStart <= probeInstant && probeInstant < meetingEnd) {
                    concurrentAtProbe++;
                }
            }

            maxConcurrentMeetings = Math.max(maxConcurrentMeetings, concurrentAtProbe);
        }

        return maxConcurrentMeetings;
    }

    /*
     * --------------------------------------------------------------------------------------------
     * APPROACH 2: Chronological Ordering — Two Pointers over Sorted Start/End Arrays
     * --------------------------------------------------------------------------------------------
     * Core idea:
     *   Split each meeting into a start event and an end event. Sort all start times and all
     *   end times independently. Walk through start times in order; each time a new meeting
     *   starts, check whether the earliest-ending currently-occupied room has already freed up
     *   (i.e., its end time <= this start time). If so, reuse that room (advance the end pointer);
     *   otherwise, we need a brand-new room. Track the running count and its maximum.
     *
     * Data structure / paradigm: sorting + two-pointer sweep.
     *
     * Time Complexity: O(n log n) — dominated by sorting the two arrays of size n.
     * Space Complexity: O(n) — two auxiliary arrays of start/end times.
     *
     * Pros:
     *   - Optimal time complexity, and arguably the cleanest, most interview-friendly proof of
     *     correctness (it's a direct simulation of "rooms freeing up over time").
     *   - No heap bookkeeping — just two integer arrays and two indices.
     * Cons:
     *   - Slightly less "obviously extensible" than the heap approach if a follow-up asks you to
     *     also report room ASSIGNMENTS (heap tracks this more naturally via a priority queue of
     *     "room becomes free at time X").
     *
     * When to use in practice:
     *   This is my preferred production choice when I only need the COUNT of rooms — it avoids
     *   heap overhead and is very easy to reason about and test.
     * --------------------------------------------------------------------------------------------
     */
    public static int minMeetingRoomsChronologicalOrdering(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }

        int meetingCount = intervals.length;
        int[] startTimes = new int[meetingCount];
        int[] endTimes = new int[meetingCount];

        for (int i = 0; i < meetingCount; i++) {
            startTimes[i] = intervals[i][0];
            endTimes[i] = intervals[i][1];
        }

        Arrays.sort(startTimes);
        Arrays.sort(endTimes);

        int startPointer = 0;
        int endPointer = 0;
        int roomsInUse = 0;
        int maxRoomsNeeded = 0;

        while (startPointer < meetingCount) {
            // A meeting is starting. First check if it can reuse a room that has already
            // freed up. Because end is EXCLUSIVE, a room freeing at time T can be reused by a
            // meeting starting at time T — hence "<=" here, not "<".
            if (startTimes[startPointer] >= endTimes[endPointer]) {
                // Earliest-ending occupied room is free by now: reuse it.
                roomsInUse--;
                endPointer++;
            }

            // Either way, this meeting now occupies a room (new or reused).
            roomsInUse++;
            maxRoomsNeeded = Math.max(maxRoomsNeeded, roomsInUse);
            startPointer++;
        }

        return maxRoomsNeeded;
    }

    /*
     * --------------------------------------------------------------------------------------------
     * APPROACH 3: Min-Heap of End Times (Priority Queue) — Classic Textbook Solution
     * --------------------------------------------------------------------------------------------
     * Core idea:
     *   Sort meetings by start time. Maintain a min-heap keyed by end time, where each heap entry
     *   represents a room currently in use and the time it becomes free. For each meeting in
     *   start-time order: if the room with the earliest free time is already free (its end time
     *   <= this meeting's start time), pop it and reuse that room (update its end time to this
     *   meeting's end time). Otherwise, push a brand-new room. The heap's size at any point is the
     *   number of rooms currently in use; the maximum size reached is the answer.
     *
     * Data structure / paradigm: sorting + min-heap (priority queue) — greedy room reuse.
     *
     * Time Complexity: O(n log n) — sorting is O(n log n); each of n meetings does at most one
     *   heap push and one heap pop, each O(log n).
     * Space Complexity: O(n) — heap holds up to n room end-times in the worst case.
     *
     * Pros:
     *   - This is the canonical, most widely recognized solution for "Meeting Rooms II" — an
     *     interviewer will immediately recognize the pattern and it generalizes cleanly to
     *     related problems (e.g., "k employees needed", "CPU task scheduling with cooldown").
     *   - Naturally extends to report room ASSIGNMENTS if asked as a follow-up (store room IDs
     *     in the heap entries).
     * Cons:
     *   - Slightly more code/machinery than Approach 2 for the same asymptotic complexity.
     *   - Heap operations have higher constant factors than simple array index bumps.
     *
     * When to use in practice:
     *   This is the approach I will PRESENT FIRST in the interview (see Section 8 for why), and
     *   it's the one I'll polish into production quality in Section 9.
     * --------------------------------------------------------------------------------------------
     */
    public static int minMeetingRoomsMinHeap(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }

        // Sort meetings by start time so we process them in chronological order.
        int[][] sortedMeetings = intervals.clone();
        Arrays.sort(sortedMeetings, Comparator.comparingInt(meeting -> meeting[0]));

        // Min-heap of end times: the room that frees up soonest is always at the top.
        PriorityQueue<Integer> roomEndTimes = new PriorityQueue<>();

        for (int[] meeting : sortedMeetings) {
            int meetingStart = meeting[0];
            int meetingEnd = meeting[1];

            // If the earliest-freeing room is already free (end <= start, exclusive-end rule),
            // reuse it instead of allocating a new one.
            if (!roomEndTimes.isEmpty() && roomEndTimes.peek() <= meetingStart) {
                roomEndTimes.poll();
            }

            // Either allocate a new room or re-occupy the reused one, tracked by this meeting's
            // end time.
            roomEndTimes.offer(meetingEnd);
        }

        // Heap size at the end equals the peak number of rooms simultaneously in use, because we
        // only ever remove a room when it's reused (never shrinking below the true concurrent
        // peak) and every meeting adds exactly one entry.
        return roomEndTimes.size();
    }

    /*
     * --------------------------------------------------------------------------------------------
     * APPROACH 4: Sweep Line via Difference (Counting) Array — Hashing/Counting-based
     * --------------------------------------------------------------------------------------------
     * Core idea:
     *   Because start/end times are bounded (0 <= time <= 10^6), allocate a "delta" array over
     *   the entire time domain. For each meeting [start, end), increment delta[start] by 1 (a
     *   room becomes occupied) and decrement delta[end] by 1 (a room becomes free at that exact
     *   instant, consistent with exclusive end). Then take a running prefix sum across the delta
     *   array; the maximum prefix sum at any instant is the peak number of concurrently active
     *   meetings, i.e., the answer.
     *
     * Data structure / paradigm: counting / difference array (a bucket-based relative of hashing —
     *   we're using direct-address counting instead of a hash map because the key domain is small
     *   and dense enough to index directly).
     *
     * Time Complexity: O(n + maxTime) — O(n) to populate the delta array, O(maxTime) to sweep it.
     *   With maxTime = 10^6 this is extremely fast in practice, though technically NOT purely a
     *   function of n alone.
     * Space Complexity: O(maxTime) — a fixed array sized to the time domain, regardless of n.
     *
     * Pros:
     *   - Very simple to implement correctly, and naturally handles the exclusive-end rule cleanly
     *     (decrement lands exactly on the end instant).
     *   - No sorting or heap machinery — just array increments and a linear scan.
     * Cons:
     *   - Space usage depends on the VALUE RANGE of the timestamps, not just on n. If times were
     *     up to 10^9 or 10^18 instead of 10^6, this approach would need coordinate compression
     *     first (mapping the sparse timestamps to a dense 0..2n-1 range) to stay memory-feasible —
     *     at which point its complexity converges to O(n log n) anyway, i.e., no better than
     *     Approaches 2/3.
     *   - Wastes memory when n is tiny but the max timestamp is large (e.g., 3 meetings with a
     *     timestamp of 10^6 still allocates a million-entry array).
     *
     * When to use in practice:
     *   Great when the time domain is known to be small/dense (e.g., minute-of-day scheduling,
     *   0..1440). Given this problem's explicit bound of 10^6, it's a perfectly valid optimal
     *   approach, but I'd default to Approach 2 or 3 since they're insensitive to the value range.
     * --------------------------------------------------------------------------------------------
     */
    public static int minMeetingRoomsSweepLineCounting(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }

        int maxTimeBound = 0;
        for (int[] meeting : intervals) {
            maxTimeBound = Math.max(maxTimeBound, meeting[1]); // end times are the largest values
        }

        // delta[t] = net change in "active meeting count" at instant t.
        // Sized to maxTimeBound + 1 so the decrement at the largest end time is always in range.
        int[] delta = new int[maxTimeBound + 1];

        for (int[] meeting : intervals) {
            int meetingStart = meeting[0];
            int meetingEnd = meeting[1]; // exclusive
            delta[meetingStart] += 1;   // a room becomes occupied at meetingStart
            delta[meetingEnd] -= 1;     // that room becomes free exactly at meetingEnd
        }

        int currentActiveMeetings = 0;
        int maxActiveMeetings = 0;

        for (int timeInstant = 0; timeInstant <= maxTimeBound; timeInstant++) {
            currentActiveMeetings += delta[timeInstant];
            maxActiveMeetings = Math.max(maxActiveMeetings, currentActiveMeetings);
        }

        return maxActiveMeetings;
    }

    /*
     * ============================================================================================
     * SECTION 7 — APPROACHES COMPARISON TABLE
     * ============================================================================================
     *
     * Approach                          | Time            | Space        | Best For                          | Limitations
     * ----------------------------------|-----------------|--------------|-----------------------------------|--------------------------------------------
     * 1. Brute Force (pairwise)         | O(n^2)          | O(1)         | Baseline / correctness oracle      | Doesn't scale; not interview-final-answer
     * 2. Chronological Ordering (2-ptr) | O(n log n)      | O(n)         | Production use, count-only queries | Less natural if room IDs must be reported
     * 3. Min-Heap (Priority Queue)      | O(n log n)      | O(n)         | Interview-standard, extensible     | Slightly more code/constant factor than #2
     * 4. Sweep Line / Difference Array  | O(n + maxTime)  | O(maxTime)   | Small/dense bounded time domains   | Space tied to value range, not n; needs
     *                                    |                 |              |                                    | coordinate compression if range is huge
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 8 — RECOMMENDED APPROACH FOR THE INTERVIEW
     * ============================================================================================
     *
     * I would present APPROACH 3 (Min-Heap of End Times) as my primary solution, then proactively
     * mention Approach 2 (Chronological Ordering) as a space/constant-factor optimization I could
     * swap in if room *assignment* isn't needed — this is the same "anchor on a well-known correct
     * solution, then pitch the optimization" narrative strategy that works well for showing
     * structural insight rather than just pattern-matching.
     *
     * Why the heap approach as the anchor:
     *   - It is THE canonical solution interviewers expect for "Meeting Rooms II" — recognizing
     *     and articulating the greedy "reuse the earliest-freeing room" invariant is exactly what's
     *     being evaluated.
     *   - It generalizes immediately to natural follow-ups (reporting room assignments, k-server
     *     style problems, CPU scheduling with cooldowns) without restructuring the algorithm.
     *   - It's fast to code correctly under interview pressure: sort + single loop + heap push/pop.
     *
     * Why I'd still mention Approach 2:
     *   - Same O(n log n) time, but O(n) space with lower constant factors (plain arrays, no heap
     *     allocations per operation) — a good "I know how to squeeze this further" remark.
     *
     * Why NOT approach 4 as the primary pitch despite being technically optimal for this bound:
     *   - Its complexity secretly depends on maxTime, not just n; a good interviewer will probe
     *     "what if timestamps were up to 10^9?" and I'd rather lead with an approach whose
     *     complexity is a clean function of n alone, then mention Approach 4 as a nice bounded-
     *     domain trick.
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 9 — DEEP DIVE: OPTIMAL SOLUTION (PRODUCTION-QUALITY)
     * ============================================================================================
     * Polished version of Approach 3, the min-heap solution, with full Javadoc and defensive
     * input validation suitable for a production codebase.
     * ============================================================================================
     */

    /**
     * Computes the minimum number of meeting rooms required to schedule all given meetings
     * without any two meetings sharing a room at the same instant.
     *
     * <p>End times are treated as exclusive: a meeting {@code [a, b)} does not conflict with a
     * meeting {@code [b, c)} that starts exactly when the first one ends.
     *
     * @param intervals array of meetings, where each meeting is {@code {start, end}} with
     *                  {@code 0 <= start < end}. Must not be {@code null}; individual entries
     *                  must not be {@code null} and must have length 2.
     * @return the minimum number of rooms needed; {@code 0} if {@code intervals} is empty.
     * @throws IllegalArgumentException if {@code intervals} is {@code null}, or any entry is
     *                                   {@code null}, malformed, or has {@code start >= end}.
     */
    public static int minMeetingRoomsOptimal(int[][] intervals) {
        if (intervals == null) {
            throw new IllegalArgumentException("intervals must not be null");
        }
        if (intervals.length == 0) {
            return 0; // No meetings, no rooms needed.
        }

        // Defensive validation: fail fast on malformed input rather than producing a silently
        // wrong answer. In a real production system, garbage input should never pass unnoticed.
        for (int[] meeting : intervals) {
            if (meeting == null || meeting.length != 2) {
                throw new IllegalArgumentException("Each meeting must be a non-null {start, end} pair");
            }
            if (meeting[0] >= meeting[1]) {
                throw new IllegalArgumentException(
                        "start must be strictly less than end: got [" + meeting[0] + ", " + meeting[1] + ")");
            }
        }

        // Sort a COPY of the input by start time so we never mutate the caller's array.
        int[][] meetingsByStartTime = intervals.clone();
        Arrays.sort(meetingsByStartTime, Comparator.comparingInt(meeting -> meeting[0]));

        // Min-heap keyed by "when does this occupied room become free". The room at the top of
        // the heap is always the one that frees up soonest.
        PriorityQueue<Integer> roomFreeAtTime = new PriorityQueue<>(meetingsByStartTime.length);

        int peakRoomsInUse = 0;

        for (int[] meeting : meetingsByStartTime) {
            int meetingStart = meeting[0];
            int meetingEnd = meeting[1];

            // Reuse the earliest-freeing room if it has already freed up by the time this
            // meeting starts. Exclusive-end semantics mean "freed at meetingStart" still counts
            // as free (hence <=, not <).
            if (!roomFreeAtTime.isEmpty() && roomFreeAtTime.peek() <= meetingStart) {
                roomFreeAtTime.poll();
            }

            // Occupy a room (new or just-freed) until this meeting's end time.
            roomFreeAtTime.offer(meetingEnd);

            // The heap's current size is the number of rooms simultaneously occupied right now;
            // track the running peak.
            peakRoomsInUse = Math.max(peakRoomsInUse, roomFreeAtTime.size());
        }

        return peakRoomsInUse;
    }

    /*
     * ============================================================================================
     * SECTION 10 — DRY RUN / TRACE (using the optimal min-heap solution)
     * ============================================================================================
     *
     * Tracing intervals = [[0,30], [5,10], [15,20]] through minMeetingRoomsOptimal:
     *
     * Step 0: Validate input -> all start < end, OK. Sort by start time:
     *         meetingsByStartTime = [[0,30], [5,10], [15,20]]  (already sorted here)
     *         roomFreeAtTime = {}  (empty heap)
     *         peakRoomsInUse = 0
     *
     * Step 1: meeting = [0, 30]
     *         heap is empty -> cannot reuse.
     *         offer(30) -> roomFreeAtTime = {30}
     *         peakRoomsInUse = max(0, 1) = 1
     *
     * Step 2: meeting = [5, 10]
     *         heap.peek() = 30; is 30 <= 5? No -> cannot reuse, need a new room.
     *         offer(10) -> roomFreeAtTime = {10, 30}
     *         peakRoomsInUse = max(1, 2) = 2
     *
     * Step 3: meeting = [15, 20]
     *         heap.peek() = 10; is 10 <= 15? Yes -> reuse! poll() -> roomFreeAtTime = {30}
     *         offer(20) -> roomFreeAtTime = {20, 30}
     *         peakRoomsInUse = max(2, 2) = 2   (size stayed at 2: one popped, one pushed)
     *
     * Final result: peakRoomsInUse = 2   -> matches Example 1's expected answer.
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 11 — CLOSING SUMMARY
     * ============================================================================================
     *
     * All four approaches are correct; they differ in complexity profile and code shape:
     *   - Brute force (O(n^2)) is a correctness oracle, never a final answer at scale.
     *   - Chronological ordering and min-heap are both O(n log n) / O(n) and are the two
     *     legitimate "optimal" answers for this problem; the heap version is the more commonly
     *     expected interview answer and extends more naturally to room-assignment follow-ups.
     *   - The difference-array sweep is O(n + maxTime) / O(maxTime) — asymptotically excellent
     *     given this problem's explicit 10^6 bound, but its cost is tied to the VALUE RANGE of
     *     timestamps rather than purely to n, so it degrades (or requires coordinate compression)
     *     if that bound were relaxed.
     *
     * Known limitations / assumptions of the final production solution
     * (minMeetingRoomsOptimal):
     *   - Assumes start < end strictly for every meeting (validated defensively, throws otherwise).
     *   - Assumes timestamps fit in a Java int; times up to 10^6 comfortably do.
     *   - Not thread-safe for concurrent mutation of the input array during execution (a private
     *     sorted copy is made, but this is a single-threaded, single-call computation as clarified
     *     in Section 2).
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 12 — FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ============================================================================================
     *
     * 1. "Can you also return which room each meeting is assigned to, not just the count?"
     *    -> Extend the heap to store (endTime, roomId) pairs; when reusing, assign the popped
     *       room's ID to the new meeting; when allocating fresh, assign the next unused room ID.
     *
     * 2. "What if meetings arrive as a live stream and you must answer 'rooms needed so far' after
     *     each new meeting, without re-processing everything?"
     *    -> Maintain the heap incrementally in an online fashion; each new meeting does one
     *       O(log n) heap operation instead of a full re-sort, but you lose the ability to
     *       "look ahead" at meetings that haven't arrived yet — the running peak is only valid
     *       up to the meetings seen so far.
     *
     * 3. "What if the time domain could be up to 10^18 instead of 10^6?"
     *    -> Approach 4 (difference array) becomes infeasible as-is; you'd coordinate-compress the
     *       timestamps first (sort unique times, map to dense indices), which effectively turns
     *       it back into an O(n log n) solution — no better than Approaches 2/3.
     *
     * 4. "Can you solve it with O(1) extra space (ignoring the input and output)?"
     *    -> Not while preserving O(n log n) time and needing to track concurrently active rooms,
     *       unless in-place sorting of the original array (and reuse of the array itself as
     *       scratch space) is allowed; even then, the heap or start/end pointers still need O(n)
     *       conceptually. Discuss the trade-off rather than claiming a false O(1) solution.
     *
     * 5. "How would this change if meetings could be cancelled/rescheduled after the fact?"
     *    -> A static offline algorithm no longer suffices; you'd want a dynamic interval data
     *       structure (e.g., a balanced BST / order-statistics tree keyed by time, or an
     *       interval tree) supporting insert/delete with O(log n) updates to the running max
     *       concurrency.
     *
     * 6. "What if some meetings have priority and lower-priority meetings can be dropped instead
     *     of adding a new room, given a fixed room budget?"
     *    -> This becomes a weighted interval scheduling / room-budgeted admission problem, which
     *       shifts toward greedy-by-priority or DP-based interval scheduling depending on the
     *       exact objective (maximize meetings held vs. maximize priority sum).
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * SECTION 13 — WHAT CANDIDATES TYPICALLY MISS
     * ============================================================================================
     *
     * 1. Exclusive-end off-by-one errors: using strict "<" instead of "<=" when deciding whether a
     *    room has freed up. This silently over-counts rooms whenever a meeting starts exactly when
     *    another ends (Example 2 above is designed to expose this bug immediately).
     *
     * 2. Assuming input is pre-sorted by start time. Many candidates write correct sweep logic but
     *    forget to sort first, producing wrong answers on unsorted test data (which is the default
     *    assumption per Section 1/2).
     *
     * 3. Forgetting that duplicate/identical intervals are legitimate independent conflicts (they
     *    are NOT collapsed into one meeting) — Example 3 is designed to catch this.
     *
     * 4. Confusing "count how many meetings overlap with meeting i" with "count how many meetings
     *    are active at any GIVEN INSTANT" in the brute-force approach — the correct probe instants
     *    are the meeting START times (concurrency peaks always occur at a start event), not
     *    arbitrary or end-time instants. Probing only end times, or probing the wrong instant
     *    entirely, produces an undercount.
     * ============================================================================================
     */

    /*
     * ============================================================================================
     * TEST HARNESS — cross-validates all four approaches against each other on the same inputs.
     * ============================================================================================
     */
    public static void main(String[] args) {
        List<int[][]> testCases = new ArrayList<>();
        List<Integer> expected = new ArrayList<>();

        // Example 1: normal case
        testCases.add(new int[][]{{0, 30}, {5, 10}, {15, 20}});
        expected.add(2);

        // Example 2: edge case — touching boundaries, exclusive end means no overlap
        testCases.add(new int[][]{{1, 5}, {5, 10}, {10, 15}});
        expected.add(1);

        // Example 3: boundary / tie-breaking case — everyone overlaps, plus duplicates
        testCases.add(new int[][]{{0, 10}, {0, 10}, {5, 15}, {5, 7}});
        expected.add(4);

        // Single meeting
        testCases.add(new int[][]{{5, 10}});
        expected.add(1);

        // All identical meetings
        testCases.add(new int[][]{{2, 4}, {2, 4}, {2, 4}});
        expected.add(3);

        // All disjoint meetings
        testCases.add(new int[][]{{1, 2}, {3, 4}, {5, 6}});
        expected.add(1);

        // Empty array
        testCases.add(new int[][]{});
        expected.add(0);

        boolean allPassed = true;

        for (int testIndex = 0; testIndex < testCases.size(); testIndex++) {
            int[][] input = testCases.get(testIndex);
            int expectedRooms = expected.get(testIndex);

            int bruteForceResult = minMeetingRoomsBruteForce(input);
            int chronologicalResult = minMeetingRoomsChronologicalOrdering(input);
            int heapResult = minMeetingRoomsMinHeap(input);
            int sweepLineResult = minMeetingRoomsSweepLineCounting(input);
            int optimalResult = minMeetingRoomsOptimal(input);

            boolean testPassed =
                    bruteForceResult == expectedRooms
                            && chronologicalResult == expectedRooms
                            && heapResult == expectedRooms
                            && sweepLineResult == expectedRooms
                            && optimalResult == expectedRooms;

            allPassed &= testPassed;

            System.out.printf(
                    "Test %d: input=%s expected=%d | bruteForce=%d chronological=%d heap=%d sweepLine=%d optimal=%d -> %s%n",
                    testIndex + 1,
                    Arrays.deepToString(input),
                    expectedRooms,
                    bruteForceResult,
                    chronologicalResult,
                    heapResult,
                    sweepLineResult,
                    optimalResult,
                    testPassed ? "PASS" : "FAIL");
        }

        System.out.println();
        System.out.println(allPassed ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
    }
}
