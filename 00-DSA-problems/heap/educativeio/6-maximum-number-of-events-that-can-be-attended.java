import java.util.*;

/*
================================================================================
 GOOGLE ONSITE MOCK INTERVIEW
 Problem: Maximum Number of Events That Can Be Attended  (LeetCode 1353, Hard)
 Tag family: Greedy + Min-Heap / Sorting / Union-Find ("next free day" family)
================================================================================
*/

class MaxEventsAttended {

    /*
    ============================================================
     SECTION 1: RESTATE THE PROBLEM
    ============================================================
     We are given an array `events`, where events[i] = [startDay_i, endDay_i].
     Each event can be attended on ANY single day within the inclusive range
     [startDay_i, endDay_i] -- not the whole range, just ONE day of our choosing
     from that window.

     Rules:
       - We may attend at most ONE event per day (our calendar has 1 slot/day).
       - Each event, once attended, is "used up" -- we cannot attend the same
         event again on a different day.
       - We want to MAXIMIZE the total count of distinct events attended.

     Input:  int[][] events, 1 <= events.length <= 1e5, events[i].length == 2,
             1 <= startDay_i <= endDay_i <= 1e5.
     Output: int -- the maximum number of events attendable.

     Key insight to confirm with the interviewer: this is NOT interval
     scheduling in the classic "non-overlapping intervals" sense (LC 435),
     because events CAN overlap in their day-ranges -- we just need to pick
     ONE valid day per event such that no two chosen days collide, and we
     want to serve as many events as possible (skipping events entirely is
     allowed).
    */

    /*
    ============================================================
     SECTION 2: CLARIFYING QUESTIONS (asked to interviewer, with assumed answers)
    ============================================================
     Q1: Are startDay_i and endDay_i always integers, and is the range
         inclusive on both ends?
         A: Yes, both inclusive, as stated ("from startDay_i through endDay_i
            inclusive").

     Q2: Can two different events share the exact same [start, end] range?
         A: Yes, duplicates are allowed. They are treated as independent
            events -- we can still attend at most one distinct event per day,
            but two identical-range events are two separate opportunities
            that might land on two different days.

     Q3: Is the day range bounded by 1e5 always, or could start/end be
         arbitrarily large (e.g., up to 1e9) in a follow-up?
         A: For this version, bounded by 1e5 as given in constraints. I'll
            flag that a follow-up with day values up to 1e9 would need
            coordinate compression (I'll cover this in Follow-Up Questions).

     Q4: Do we need to return WHICH events were attended, or just the count?
         A: Just the maximum count (int return type), per the problem
            statement ("Return the maximum number of events...").

     Q5: Is the events array guaranteed to be sorted in any way (by start day,
         for instance)?
         A: No, assume unsorted input; we sort ourselves as part of the
            algorithm.

     Q6: What should we return for an empty events array?
         A: Per constraints, events.length >= 1, so this can't happen, but
            defensively we'd return 0.

     Q7: Is this a single-threaded, single-pass computation, or do we need to
         support concurrent/streaming updates (events added dynamically)?
         A: Single batch computation -- all events known upfront. (I'll note
            a streaming variant as a follow-up.)

     Q8: Any constraint on memory, or should we optimize purely for time?
         A: Standard interview assumption -- optimize time first, but keep
            space reasonable (O(N) or O(N + D) is fine, D = max day value).
    */

    /*
    ============================================================
     SECTION 3: EXAMPLES & EDGE CASES
    ============================================================

     Example 1 (Normal case):
       events = [[1,2],[2,3],[3,4],[1,2]]
       Optimal: Day1 -> event[0]([1,2]), Day2 -> event[1]([2,3]) or event[3],
                Day3 -> event[2]([3,4]), Day4 unused (no event covers it after
                assignment) OR reorder differently.
       One valid assignment: Day1->[1,2], Day2->[2,3], Day3->[3,4],
                              Day? -> [1,2] duplicate can't reuse day1/day2
                              since both taken; its range is [1,2] and both
                              days are gone, so it CANNOT be attended.
       Answer = 4? Let's verify: we have 4 events, only 4 distinct usable
       days appear across ranges {1,2,3,4}. Greedy (smallest end-day first
       among available) achieves: Day1->[1,2](evtA), Day2->[1,2](evtD),
       Day3->[2,3](evtB), Day4->[3,4](evtC). All 4 attended.
       Answer = 4.

     Example 2 (Edge case -- all events identical, single day):
       events = [[1,1],[1,1],[1,1]]
       Only day 1 exists across all ranges, and we can attend at most one
       event per day, so no matter how many duplicate single-day events we
       have, only ONE can ever be attended.
       Answer = 1.

     Example 3 (Boundary / tie-breaking case -- overlapping ranges requiring
                 correct greedy tie-break by earliest end day):
       events = [[1,4],[4,4],[2,2],[3,4],[1,1]]
       If we greedily attend events in the wrong order (e.g., by start day
       only, ignoring end day), we might "waste" day 1 on the wide event
       [1,4] and miss that [1,1] and [4,4] have NO flexibility -- they must
       be scheduled on their only valid day before it's taken by a wider
       event. Correct greedy (always serve the available event with the
       EARLIEST end day first) yields:
         Day1 -> [1,1] (must-use, no flexibility)
         Day2 -> [2,2] (must-use)
         Day3 -> [1,4] or [3,4] (pick the one ending soonest among available:
                  [3,4] ends at 4, [1,4] ends at 4 -- tie, pick either)
         Day4 -> [4,4] (must-use) -- but if day4 already consumed by the
                  other, we still have one more with end=4 to place... only
                  one slot on day4 exists.
       Careful trace: available at day3 = {[1,4], [3,4]} (both cover day3),
       plus [4,4] not yet available (covers day 4 only... wait [4,4] starts
       at 4). At day3, candidates covering day3: [1,4], [3,4]. Pick smaller
       end day (tie, both=4) -> attend one, say [3,4]. Day4: candidates
       covering day4 still open: [1,4] (still not attended), [4,4]. Pick
       smaller end (tie=4) -> attend [1,4]. [4,4] now has NO day left.
       Total attended = [1,1], [2,2], [3,4], [1,4] = 4 out of 5. The 5th
       event [4,4] cannot be served since day 4 is already taken.
       Answer = 4.
       This demonstrates WHY the greedy must prioritize the event with the
       smallest END day among those currently available on the current day
       -- not the smallest start day, and not arrival order.
    */

    /*
    ============================================================
     SECTION 4 & 5: ALL POSSIBLE APPROACHES
     (brute force -> sorting -> greedy+heap -> union-find -> treeset)
     Paradigms not applicable are explained briefly at the end.
    ============================================================
    */

    /* ---------------------------------------------------------------------
       APPROACH 1: Brute Force Backtracking (exponential)
       -----------------------------------------------------------------
       Core idea: Try every event, and for every event try every day in its
       [start, end] range, recursively deciding to attend-or-skip and
       marking days used. Take the maximum count over all branches.
       This is the textbook "try everything" baseline you'd sketch on a
       whiteboard before optimizing.

       Data structure / paradigm: plain recursion + a "used days" boolean
       set, i.e. exhaustive search / backtracking.

       Time Complexity: O((D+1)^N) worst case (each event can pick any of up
       to D days or skip) -- fully exponential. In practice pruned somewhat
       by used-day checks, but still exponential.
       Space Complexity: O(N) recursion depth + O(D) for the used-day set.

       Pros:
         - Trivial to reason about correctness (it IS the definition of the
           problem, tried exhaustively).
         - Good for validating optimized solutions against small inputs
           (this is exactly the role it plays in our stress test later).
       Cons:
         - Utterly infeasible for N or D anywhere near the given constraints
           (1e5). Only usable for N <= ~8-10 in a test harness.
       When to use: Never in production / never as your submitted interview
       answer. Only as a correctness oracle for stress-testing the optimal
       solution on tiny random inputs.
    --------------------------------------------------------------------- */
    static int maxEventsBruteForce(int[][] events) {
        int n = events.length;
        if (n == 0) return 0;
        int maxDay = 0;
        for (int[] event : events) maxDay = Math.max(maxDay, event[1]);
        boolean[] dayUsed = new boolean[maxDay + 1]; // dayUsed[d] = true if day d already has an event
        return backtrack(events, 0, dayUsed);
    }

    // Recursively decide, for event index `eventIndex`, which day (or none) to use.
    private static int backtrack(int[][] events, int eventIndex, boolean[] dayUsed) {
        if (eventIndex == events.length) return 0;

        // Option A: skip this event entirely.
        int best = backtrack(events, eventIndex + 1, dayUsed);

        // Option B: try every day in this event's range that is still free.
        int startDay = events[eventIndex][0];
        int endDay = events[eventIndex][1];
        for (int day = startDay; day <= endDay; day++) {
            if (!dayUsed[day]) {
                dayUsed[day] = true;
                best = Math.max(best, 1 + backtrack(events, eventIndex + 1, dayUsed));
                dayUsed[day] = false; // backtrack
            }
        }
        return best;
    }

    /* ---------------------------------------------------------------------
       APPROACH 2: Naive Day-by-Day Full Scan (correct, but slow)
       -----------------------------------------------------------------
       Core idea: Walk through every calendar day from 1 to maxDay. On each
       day, linearly scan ALL events to find the still-unattended event that
       (a) covers this day, and (b) has the smallest end day among those
       candidates -- then attend it. This is the CORRECT greedy rule, just
       implemented with a brute-force "find the best candidate" scan instead
       of a heap, so it's slow but instructive: it shows the greedy idea
       without the heap machinery.

       Data structure / paradigm: greedy selection via linear scan each day.

       Time Complexity: O(D * N) -- for each of up to D days, scan up to N
       events. With D, N <= 1e5 this is up to 1e10 operations -- too slow.
       Space Complexity: O(N) for the "attended" boolean array.

       Pros:
         - Demonstrates the correct greedy RULE (earliest end-day first)
           without needing to explain heap mechanics -- good stepping stone
           when talking through your thought process out loud.
       Cons:
         - Quadratic-ish in D*N, fails constraints (1e5 * 1e5).
       When to use: As a mid-point explanation in the interview narrative --
       "here's the greedy idea in its simplest form, now let me make the
       'find best candidate' step fast with a heap" -- but not as your final
       submission.
    --------------------------------------------------------------------- */
    static int maxEventsNaiveDayScan(int[][] events) {
        int n = events.length;
        if (n == 0) return 0;
        int maxDay = 0;
        for (int[] event : events) maxDay = Math.max(maxDay, event[1]);

        boolean[] attended = new boolean[n];
        int attendedCount = 0;

        for (int day = 1; day <= maxDay; day++) {
            int bestEventIndex = -1;
            int bestEndDay = Integer.MAX_VALUE;
            // Linear scan: find the unattended event covering `day` with smallest end day.
            for (int eventIndex = 0; eventIndex < n; eventIndex++) {
                if (attended[eventIndex]) continue;
                int startDay = events[eventIndex][0];
                int endDay = events[eventIndex][1];
                if (startDay <= day && day <= endDay && endDay < bestEndDay) {
                    bestEndDay = endDay;
                    bestEventIndex = eventIndex;
                }
            }
            if (bestEventIndex != -1) {
                attended[bestEventIndex] = true;
                attendedCount++;
            }
        }
        return attendedCount;
    }

    /* ---------------------------------------------------------------------
       APPROACH 3: Sorting + Min-Heap Greedy  (OPTIMAL, recommended)
       -----------------------------------------------------------------
       Core idea: Sort events by START day. Sweep the calendar day by day
       from the earliest start to the latest end. On each day:
         1. Push all events whose startDay == current day into a MIN-HEAP
            keyed by endDay (so the heap top is always the event expiring
            soonest among those currently available).
         2. Pop and discard any heap-top events whose endDay < current day
            (they expired unattended -- we were "too greedy" elsewhere or
            they simply had no free day).
         3. If the heap is non-empty, pop the top (smallest endDay) and
            attend it today -- this is the greedy exchange argument: among
            all events available today, the one with the least remaining
            flexibility (soonest deadline) should be consumed first, because
            wider-range events can still be served on a later day.

       Data structure / paradigm: Greedy + Min-Heap (PriorityQueue), classic
       "earliest deadline first" scheduling.

       Correctness (exchange argument): if on day d we have a choice between
       event A (ends soon) and event B (ends later), and we instead pick B,
       then A might expire before we get another chance, while B still has
       future days available. Swapping to attend A today and B later (if
       possible) never makes the schedule worse. Formal exchange-argument
       proof, standard for this interval/day-assignment family.

       Time Complexity: O((N + D) log N), where D = max end day (<= 1e5).
       We sort events O(N log N), and we do at most D day-steps, each with
       O(log N) heap push/pop operations (each event pushed/popped once
       overall, plus one "check top" per day).
       Space Complexity: O(N) for the heap.

       Pros:
         - Textbook optimal complexity for the given constraints.
         - Clean exchange-argument proof of correctness -- easy to explain
           and defend in an interview.
         - Directly generalizes: still correct even with duplicate ranges.
       Cons:
         - Complexity has a D term (day sweep), which is fine here since
           D <= 1e5, but would need adjustment (coordinate compression) if
           day values could be up to 1e9 (see Follow-Up Questions).
       When to use: This is the approach I'd code in a live interview --
       best balance of optimal complexity, provable correctness, and
       coding speed.
    --------------------------------------------------------------------- */
    static int maxEventsGreedyHeap(int[][] events) {
        int n = events.length;
        if (n == 0) return 0;

        // Sort events by start day ascending so we can sweep day-by-day and
        // know exactly which events "arrive" on each day.
        int[][] sortedEvents = events.clone();
        Arrays.sort(sortedEvents, (eventA, eventB) -> Integer.compare(eventA[0], eventB[0]));

        int maxDay = 0;
        for (int[] event : sortedEvents) maxDay = Math.max(maxDay, event[1]);

        // Min-heap keyed by endDay: top is the event expiring soonest among
        // those currently available to attend.
        PriorityQueue<Integer> availableEndDays = new PriorityQueue<>();

        int attendedCount = 0;
        int eventPointer = 0; // index into sortedEvents, next event to "arrive"

        for (int currentDay = 1; currentDay <= maxDay; currentDay++) {
            // Step 1: add every event that starts today.
            while (eventPointer < n && sortedEvents[eventPointer][0] == currentDay) {
                availableEndDays.offer(sortedEvents[eventPointer][1]);
                eventPointer++;
            }

            // Step 2: discard events that have already expired (endDay < today).
            while (!availableEndDays.isEmpty() && availableEndDays.peek() < currentDay) {
                availableEndDays.poll();
            }

            // Step 3: attend the event expiring soonest, if any is available.
            if (!availableEndDays.isEmpty()) {
                availableEndDays.poll();
                attendedCount++;
            }

            // Minor optimization: if no events remain to arrive and the heap
            // is empty, we can stop early.
            if (eventPointer >= n && availableEndDays.isEmpty()) break;
        }
        return attendedCount;
    }

    /* ---------------------------------------------------------------------
       APPROACH 4: Sort by End Day + Union-Find "Next Free Day"  (OPTIMAL alt.)
       -----------------------------------------------------------------
       Core idea: Sort events by endDay ascending (serve the most urgent /
       least flexible events first -- same greedy priority as Approach 3,
       but realized differently). For each event, find the earliest FREE day
       >= startDay and <= endDay using a Union-Find (Disjoint Set Union)
       structure where find(day) returns the next unused day at or after
       `day`. If that free day is within [startDay, endDay], attend the
       event on that day and "union" that day to (day - 1) so future queries
       skip it. Otherwise the event cannot be attended.

       Data structure / paradigm: Union-Find over the bounded day domain
       [1, maxDay] -- same family as LC 352's bounded-domain Union-Find
       pattern already in this pattern library, applied here to "find next
       available slot" instead of "merge intervals".

       Time Complexity: O(N log N) for sorting + O(N * alpha(D)) for the
       union-find operations (alpha = inverse Ackermann, effectively
       constant). Overall O(N log N), which is asymptotically as good as
       Approach 3 and avoids the O(D) day-sweep term entirely.
       Space Complexity: O(D) for the DSU parent array (D = max day).

       Pros:
         - No day-by-day sweep, so its complexity depends only on N (once
           sorted), not on D -- strictly better than Approach 3 when D >> N
           (e.g., N = 100 but days range up to 1e5).
         - Reuses a pattern already in the library (bounded-domain
           Union-Find), reducing new concepts introduced.
       Cons:
         - Slightly more intricate to implement correctly (path compression
           on "next free" queries, off-by-one on day 1) than the heap
           approach.
         - Requires knowing maxDay upfront to size the DSU array -- fine
           here, but needs coordinate compression if day values are huge.
       When to use: Preferred when D (day range) is much larger than N
       (sparse events over a huge day range), since it removes the O(D)
       term. For this problem's constraints (both N, D <= 1e5) either
       Approach 3 or Approach 4 is acceptable; I'd mention this as the
       "alternative optimal" to show breadth.
    --------------------------------------------------------------------- */
    static int maxEventsUnionFind(int[][] events) {
        int n = events.length;
        if (n == 0) return 0;

        int maxDay = 0;
        for (int[] event : events) maxDay = Math.max(maxDay, event[1]);

        // parent[d] = next free day at-or-after d (path-compressed).
        // Index range [0, maxDay]; we use 0 as a permanent "sentinel/no slot" root.
        int[] parent = new int[maxDay + 2];
        for (int day = 0; day <= maxDay + 1; day++) parent[day] = day;

        // Sort by endDay so the least-flexible events are assigned first.
        int[][] sortedByEnd = events.clone();
        Arrays.sort(sortedByEnd, (eventA, eventB) -> Integer.compare(eventA[1], eventB[1]));

        int attendedCount = 0;
        for (int[] event : sortedByEnd) {
            int startDay = event[0];
            int endDay = event[1];
            int freeDay = find(parent, startDay);
            if (freeDay <= endDay) {
                attendedCount++;
                // Mark freeDay as used by unioning it to freeDay - 1's root,
                // so the next query for this day returns the next free one.
                parent[freeDay] = find(parent, freeDay + 1);
            }
            // else: no free day in range, event cannot be attended.
        }
        return attendedCount;
    }

    // Path-compressed find: returns the smallest free day >= day.
    private static int find(int[] parent, int day) {
        if (parent[day] != day) {
            parent[day] = find(parent, parent[day]); // path compression
        }
        return parent[day];
    }

    /* ---------------------------------------------------------------------
       APPROACH 5: Sort by End Day + TreeSet "ceiling" Greedy  (OPTIMAL alt.)
       -----------------------------------------------------------------
       Core idea: Same greedy family as Approach 4, but instead of a
       Union-Find over the bounded day domain, use a balanced BST (TreeSet)
       of all currently-free days. Events MUST be processed sorted by
       endDay ascending (least flexibility / soonest deadline first) --
       sorting by startDay instead is a subtle correctness trap (see note
       below). For each event, find the smallest free day >= startDay via
       ceiling(); if it's <= endDay, attend it and remove that day from the
       set.

       CORRECTNESS TRAP CALLED OUT EXPLICITLY: it is tempting to sort by
       startDay (since that's the value we call ceiling() with), but this
       is WRONG. Counter-example: events = [[3,5],[3,3],[4,4]]. Sorted by
       start, [3,5] gets processed before [3,3] and may consume day 3,
       starving the inflexible event [3,3] entirely. Sorted by END day,
       [3,3] (end=3) and [4,4] (end=4) are correctly served before the more
       flexible [3,5] (end=5), which can still fall back to day 5. This
       exact scenario is included in the stress test below and caught this
       bug during development.

       Data structure / paradigm: Sorted set / balanced BST greedy (a more
       "off-the-shelf" cousin of the DSU approach).

       Time Complexity: O(D log D) to initialize the TreeSet with all days
       1..D, plus O(N log D) for the N ceiling/remove operations. Overall
       O((D + N) log D).
       Space Complexity: O(D) for the TreeSet holding every day.

       Pros:
         - Very easy to state and implement correctly (no path-compression
           subtlety like Union-Find).
       Cons:
         - Initializing the TreeSet with all D days costs O(D log D) time
           and O(D) space upfront, which is strictly worse than the
           Union-Find approach's near-O(N) amortized behavior when D >> N.
       When to use: Good "I know a simpler alternative" mention to show
       breadth, but I'd prefer Approach 3 (heap) or Approach 4 (DSU) as the
       actual submission.
    --------------------------------------------------------------------- */
    static int maxEventsTreeSet(int[][] events) {
        int n = events.length;
        if (n == 0) return 0;

        int maxDay = 0;
        for (int[] event : events) maxDay = Math.max(maxDay, event[1]);

        TreeSet<Integer> freeDays = new TreeSet<>();
        for (int day = 1; day <= maxDay; day++) freeDays.add(day);

        // Sort by END day (NOT start day) -- least-flexible events must be
        // served first, or a wide-range event can steal a day out from
        // under a narrow-range one that had no alternative. See the
        // correctness-trap note above.
        int[][] sortedByEnd = events.clone();
        Arrays.sort(sortedByEnd, (eventA, eventB) -> Integer.compare(eventA[1], eventB[1]));

        int attendedCount = 0;
        for (int[] event : sortedByEnd) {
            int startDay = event[0];
            int endDay = event[1];
            Integer freeDay = freeDays.ceiling(startDay); // smallest free day >= startDay
            if (freeDay != null && freeDay <= endDay) {
                freeDays.remove(freeDay);
                attendedCount++;
            }
        }
        return attendedCount;
    }

    /*
    ============================================================
     PARADIGMS CONSIDERED BUT NOT APPLICABLE (one-line reasons)
    ============================================================
     - Two Pointer / Sliding Window: doesn't fit -- there's no single
       contiguous "window" being grown/shrunk over a sorted array of values;
       the day-assignment decision depends on a priority among many
       simultaneously-open events, which is exactly what a heap/DSU models,
       not a two-pointer window.
     - Divide and Conquer: no natural way to split events into independent
       halves and cheaply merge results, since day-slot conflicts can span
       across any split point (an event from the left half and one from the
       right half can compete for the same day).
     - Dynamic Programming: the state would need to track "which days are
       used so far," which is exponential in the day range -- there's no
       polynomial-size DP state that captures enough information, which is
       precisely why the greedy exchange-argument approach is used instead.
     - Tree / Graph Traversal (BFS/DFS): no natural graph/tree structure to
       traverse; this is a scheduling/assignment problem, not a
       connectivity or path problem.
     - Binary Search (on the answer): doesn't apply directly -- "can we
       attend k events?" isn't monotonic in a way that a simple feasibility
       check would exploit better than the direct greedy construction.
     - Trie: irrelevant -- no string/prefix structure involved.
     - Segment Tree: POSSIBLE alternative to Union-Find/TreeSet for the
       "find next free day" query (range-min query + point update), but it
       adds implementation complexity without asymptotic benefit over the
       DSU approach for this problem, so I mention it only as a footnote.
    */

    /*
    ============================================================
     SECTION 7: APPROACHES COMPARISON TABLE
    ============================================================
     Approach                          | Time              | Space  | Best For                                  | Limitations
     ----------------------------------|-------------------|--------|-------------------------------------------|--------------------------------------------
     1. Brute Force Backtracking       | O((D+1)^N)        | O(N+D) | Correctness oracle for stress tests only   | Infeasible beyond N ~ 8-10
     2. Naive Day-by-Day Full Scan     | O(D * N)          | O(N)   | Explaining the greedy RULE without a heap  | Too slow for N, D up to 1e5 (~1e10 ops)
     3. Sorting + Min-Heap (greedy)    | O((N + D) log N)  | O(N)   | The go-to interview answer; provably       | Has an O(D) sweep term (fine here, D<=1e5)
                                       |                   |        | optimal, clean exchange-argument proof     |
     4. Sort by End + Union-Find       | O(N log N + N a(D))| O(D)  | Sparse events over a huge day range        | Slightly trickier DSU implementation
     5. Sort by Start + TreeSet        | O((D+N) log D)    | O(D)   | Simple-to-reason-about alternative to DSU  | O(D log D) upfront TreeSet init cost
     (a(D) = inverse Ackermann function, effectively O(1))
    */

    /*
    ============================================================
     SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
    ============================================================
     I would present APPROACH 3 (Sorting + Min-Heap Greedy) as my final
     answer:
       - It matches the well-known optimal complexity for this exact
         LeetCode-Hard problem, so it meets interviewer expectations for
         "the" intended solution.
       - The exchange-argument correctness proof ("always serve the event
         expiring soonest among those currently available") is short,
         intuitive, and easy to state out loud, which matters a lot for
         communicating clearly under interview time pressure.
       - It's fast to code correctly: sort, sweep, heap push/pop -- roughly
         15-20 lines, low risk of subtle bugs compared to Union-Find's
         path-compression edge cases.
       - I would VERBALLY mention Approach 4 (Union-Find) as a superior
         alternative when the day range D is much larger than N (sparse
         events), to demonstrate breadth without spending interview time
         coding a second full solution.
    */

    /*
    ============================================================
     SECTION 9: DEEP DIVE -- OPTIMAL SOLUTION (production-quality)
    ============================================================
     This is a polished re-presentation of Approach 3 with defensive input
     validation and exhaustive inline reasoning, as I'd write it live.
    */
    static int maxEvents(int[][] events) {
        // Defensive checks -- in a real interview I'd ask whether input
        // validation is expected; here we guard against null/empty input.
        if (events == null || events.length == 0) return 0;

        int eventCount = events.length;

        // Work on a copy so we never mutate the caller's array (good hygiene).
        int[][] eventsSortedByStart = events.clone();

        // Sort by start day: this lets us sweep the calendar day-by-day and
        // know precisely which events "become available" on each day,
        // using a simple advancing pointer instead of re-scanning.
        Arrays.sort(eventsSortedByStart, (eventA, eventB) -> Integer.compare(eventA[0], eventB[0]));

        // The last possible day we could ever need to consider is the
        // largest endDay across all events -- no event can be attended
        // after its own deadline, so sweeping past this is pointless.
        int lastRelevantDay = 0;
        for (int[] event : eventsSortedByStart) {
            lastRelevantDay = Math.max(lastRelevantDay, event[1]);
        }

        // Min-heap of endDays for events that are currently "open" (their
        // start day has arrived, their end day has not yet passed, and they
        // have not yet been attended). The heap top is always the event
        // with the least remaining flexibility -- the correct greedy choice.
        PriorityQueue<Integer> openEventEndDays = new PriorityQueue<>();

        int eventsAttended = 0;
        int nextEventToOpen = 0; // pointer into eventsSortedByStart

        for (int today = 1; today <= lastRelevantDay; today++) {

            // (1) Open every event whose window starts today.
            while (nextEventToOpen < eventCount
                    && eventsSortedByStart[nextEventToOpen][0] == today) {
                openEventEndDays.offer(eventsSortedByStart[nextEventToOpen][1]);
                nextEventToOpen++;
            }

            // (2) Expire (discard) any open events whose window has already
            // closed as of today -- they could never be attended and would
            // otherwise incorrectly occupy the heap top.
            while (!openEventEndDays.isEmpty() && openEventEndDays.peek() < today) {
                openEventEndDays.poll();
            }

            // (3) Greedily attend the open event that expires soonest, if
            // any exists -- this is the exchange-argument-optimal choice.
            if (!openEventEndDays.isEmpty()) {
                openEventEndDays.poll();
                eventsAttended++;
            }

            // (4) Early exit: nothing left to open and nothing left open ->
            // no future day can add or attend anything further.
            if (nextEventToOpen >= eventCount && openEventEndDays.isEmpty()) {
                break;
            }
        }

        return eventsAttended;
    }

    /*
    ============================================================
     SECTION 10: DRY RUN / TRACE
    ============================================================
     Using Example 3 from Section 3:
       events = [[1,4],[4,4],[2,2],[3,4],[1,1]]

     After sorting by start day:
       [[1,4],[1,1],[2,2],[3,4],[4,4]]
       (Note: [1,4] and [1,1] both start at day 1; their relative order
       between themselves doesn't matter for correctness -- both get pushed
       to the heap before day 1's processing continues.)

     lastRelevantDay = max(endDay) = 4.

     --- Day 1 ---
       Open events starting today: [1,4] (end=4), [1,1] (end=1).
       Heap after opening: {1, 4}  (min-heap by end day; showing sorted view)
       Expire step: heap top = 1, 1 >= today(1), nothing expires.
       Attend: pop smallest end day = 1 -> attend event [1,1]. attended=1.
       Heap after attending: {4}
       nextEventToOpen now points at [2,2] (index 2).

     --- Day 2 ---
       Open events starting today: [2,2] (end=2).
       Heap after opening: {2, 4}
       Expire step: top=2 >= 2, nothing expires.
       Attend: pop smallest = 2 -> attend event [2,2]. attended=2.
       Heap after attending: {4}
       nextEventToOpen now points at [3,4] (index 3).

     --- Day 3 ---
       Open events starting today: [3,4] (end=4).
       Heap after opening: {4, 4}
       Expire step: top=4 >= 3, nothing expires.
       Attend: pop smallest = 4 -> attend one of the end=4 events
               (say the one that was [1,4]; ties are broken arbitrarily by
               the heap, both are valid). attended=3.
       Heap after attending: {4}   (the remaining end=4 entry, representing
                                     either [3,4] or [1,4] -- whichever
                                     wasn't popped)
       nextEventToOpen now points at [4,4] (index 4).

     --- Day 4 ---
       Open events starting today: [4,4] (end=4).
       Heap after opening: {4, 4}
       Expire step: top=4 >= 4, nothing expires.
       Attend: pop smallest = 4 -> attend one of the two remaining end=4
               events. attended=4.
       Heap after attending: {4}  (one event, either the original [3,4]/
                                    [1,4] leftover or [4,4], is now stuck
                                    unattended -- it simply never gets a day
                                    since day 4 was the last day and only
                                    one slot existed).
       nextEventToOpen == eventCount(5) and heap is non-empty, so loop
       continues but lastRelevantDay(4) is already the top of the range;
       loop ends after today=4.

     Final result: eventsAttended = 4.  Matches Section 3's hand-derived
     answer of 4 (one of the five events, whichever ends up left in the
     heap, is unavoidably dropped since only 4 distinct days exist across
     the whole input).
    */

    /*
    ============================================================
     SECTION 11: CLOSING SUMMARY
    ============================================================
     - Brute force backtracking (Approach 1) defines correctness but is
       exponential -- useful only as a stress-test oracle.
     - The naive day-by-day linear scan (Approach 2) reveals the correct
       greedy RULE (always serve the soonest-expiring available event) in
       an easy-to-explain but O(D*N) way.
     - The min-heap sweep (Approach 3, RECOMMENDED) realizes that same
       greedy rule efficiently: O((N + D) log N) time, O(N) space, with a
       clean exchange-argument correctness proof.
     - Union-Find (Approach 4) and TreeSet (Approach 5) are asymptotically
       comparable alternatives; DSU is preferable when the day range D is
       much larger than N, since it removes the O(D) sweep term.
     - Known assumptions/limitations of the final `maxEvents` solution:
         * Assumes day values fit in a reasonable bounded range (here,
           <= 1e5) so that sweeping day-by-day and/or sizing arrays by
           maxDay is tractable; would need coordinate compression for much
           larger day values (see Follow-Up Questions).
         * Assumes events.length fits comfortably in memory for an O(N)
           heap and O(N log N) sort -- true for N <= 1e5.
         * Returns only the count, not which specific events/days were
           chosen (trivially extendable by recording the popped event
           index alongside its end day in the heap, if needed).
    */

    /*
    ============================================================
     SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
    ============================================================
     1. "What if startDay/endDay could be up to 1e9 instead of 1e5?"
        -> Coordinate-compress the distinct day values that actually matter
           (event start/end days), and instead of sweeping every integer
           day, sweep only over these compressed "candidate days," using
           the Union-Find or TreeSet approach restricted to that compressed
           domain -- avoids ever allocating an O(D) array.

     2. "What if we also need to return WHICH events (or which days) were
         chosen, not just the count?"
        -> Store (endDay, originalEventIndex) pairs in the heap instead of
           just endDay, and record the attended event's index and the
           `today` value into a result list whenever we pop-and-attend.

     3. "What if each event could be attended on multiple days (i.e., you
         get credit once per day you show up, up to the event's day range),
         instead of just once total?"
        -> This changes the problem substantially -- it becomes closer to
           interval scheduling for maximum "coverage," potentially solvable
           via a different greedy (e.g., always fill the day with any
           available event) since events no longer have a single "used up"
           state; would need to re-derive the greedy invariant.

     4. "What if events had weights/values, and we want to maximize total
         value instead of total count?"
        -> This becomes weighted interval scheduling, which generally
           requires DP (often with a Fenwick/segment tree over day-indexed
           DP states) rather than the simple exchange-argument greedy,
           since greedy-by-earliest-deadline is not optimal once weights
           are introduced.

     5. "Can you support streaming events (events arrive online, and you
         must decide immediately whether to attend)?"
        -> Requires an online/competitive-ratio algorithm instead of an
           offline optimal one; the current approach assumes all events are
           known upfront and freely reorderable via sorting.

     6. "How would you parallelize this for very large N across multiple
         machines?"
        -> The sorting step parallelizes well (distributed merge sort); the
           sequential day-sweep with a shared heap is the harder part to
           parallelize since state (which days are used) is inherently
           sequential -- would likely need to partition by day-ranges and
           merge boundary conflicts carefully.
    */

    /*
    ============================================================
     SECTION 13: WHAT CANDIDATES TYPICALLY MISS
    ============================================================
     1. Sorting by the WRONG key: sorting only by endDay (without also
        handling "which events have arrived yet") or sorting only by
        startDay and then greedily picking by start day rather than by
        end day -- both lead to wrong answers. The critical insight is:
        sort by start day to know arrival order, but CHOOSE by end day
        (via the heap) to decide priority among currently available events.

     2. Forgetting to EXPIRE stale heap entries before attending: if you
        don't pop off events whose endDay < today, the heap can hand you an
        event that's already unattendable, causing you to either
        under-count or (worse) silently "attend" an invalid event if you
        don't check its end day against today.

     3. Off-by-one on inclusive ranges: treating `endDay` as exclusive (i.e.
        looping `day < endDay` instead of `day <= endDay`), or initializing
        `lastRelevantDay` using startDay instead of endDay, silently drops
        the last valid day of every event's window.

     4. Assuming the natural sort order of the input already reflects start
        day order, and skipping the explicit sort -- LeetCode's given
        examples are sometimes pre-sorted, which lures candidates into
        omitting `Arrays.sort(...)`, which then fails on the actual (often
        unsorted) hidden test cases.

     5. (Specific to "find next free day" variants like Approach 4/5): when
        using a ceiling()-style query to place each event on the earliest
        available day, sorting the events by START day feels natural
        because that's the value passed into ceiling() -- but it is WRONG.
        You must sort by END day (least flexibility first), or a wide-range
        event processed too early can steal the one day a narrow-range
        event needed. This exact bug was caught live by the randomized
        stress test on events=[[3,5],[3,3],[4,4]] during development of
        this very solution -- a good real-world reminder that stress
        testing against a brute-force oracle catches correctness traps
        that hand-picked examples miss.
    */

    /*
    ============================================================
     SECTION 12 (cont'd): NAMED ASSERTION TESTS
    ============================================================
    */
    static void runAssertionTests() {
        // Test 1: Example 1 from Section 3.
        int[][] example1 = {{1,2},{2,3},{3,4},{1,2}};
        assert maxEvents(example1) == 4 : "Example 1 failed";

        // Test 2: Example 2 -- all duplicate single-day events.
        int[][] example2 = {{1,1},{1,1},{1,1}};
        assert maxEvents(example2) == 1 : "Example 2 failed";

        // Test 3: Example 3 -- tie-break / boundary case.
        int[][] example3 = {{1,4},{4,4},{2,2},{3,4},{1,1}};
        assert maxEvents(example3) == 4 : "Example 3 failed";

        // Test 4: Single event.
        int[][] singleEvent = {{5,7}};
        assert maxEvents(singleEvent) == 1 : "Single event test failed";

        // Test 5: Fully disjoint events -- attend all.
        int[][] disjoint = {{1,1},{2,2},{3,3},{4,4}};
        assert maxEvents(disjoint) == 4 : "Disjoint events test failed";

        // Test 6: LeetCode's own canonical example.
        int[][] leetcodeExample = {{1,2},{2,3},{3,4}};
        assert maxEvents(leetcodeExample) == 3 : "LeetCode canonical example failed";

        // Test 7: The "sort by end day, not start day" correctness trap
        // (see What Candidates Typically Miss, item 5). A wide event
        // [3,5] must NOT be allowed to steal day 3 or day 4 from the two
        // inflexible single-day events [3,3] and [4,4].
        int[][] flexibilityTrap = {{3,5},{3,3},{4,4}};
        assert maxEvents(flexibilityTrap) == 3 : "Flexibility-trap test failed";

        // Cross-validate every approach against maxEvents on the same inputs.
        int[][][] allTestCases = {example1, example2, example3, singleEvent, disjoint, leetcodeExample, flexibilityTrap};
        for (int[][] testCase : allTestCases) {
            int expected = maxEvents(testCase);
            assert maxEventsNaiveDayScan(testCase) == expected : "Naive day scan mismatch";
            assert maxEventsGreedyHeap(testCase) == expected : "Greedy heap mismatch";
            assert maxEventsUnionFind(testCase) == expected : "Union-Find mismatch";
            assert maxEventsTreeSet(testCase) == expected : "TreeSet mismatch";
            assert maxEventsBruteForce(testCase) == expected : "Brute force mismatch";
        }

        System.out.println("All named assertion tests passed.");
    }

    /*
    ============================================================
     RANDOMIZED STRESS TEST (cross-validates all approaches against the
     brute-force oracle on small random inputs, fixed seed for reproducibility)
    ============================================================
    */
    static void runStressTest() {
        long seed = 42L;
        Random random = new Random(seed);
        int trials = 3000;
        int maxEventsPerTrial = 7;  // kept small so brute force stays feasible
        int maxDayValue = 6;

        for (int trial = 0; trial < trials; trial++) {
            int eventCount = 1 + random.nextInt(maxEventsPerTrial);
            int[][] events = new int[eventCount][2];
            for (int i = 0; i < eventCount; i++) {
                int startDay = 1 + random.nextInt(maxDayValue);
                int endDay = startDay + random.nextInt(maxDayValue - startDay + 1);
                events[i][0] = startDay;
                events[i][1] = endDay;
            }

            int expected = maxEventsBruteForce(events);
            int naive = maxEventsNaiveDayScan(events);
            int heap = maxEventsGreedyHeap(events);
            int unionFind = maxEventsUnionFind(events);
            int treeSet = maxEventsTreeSet(events);
            int optimal = maxEvents(events);

            if (naive != expected || heap != expected || unionFind != expected
                    || treeSet != expected || optimal != expected) {
                System.out.println("MISMATCH on trial " + trial
                        + " events=" + Arrays.deepToString(events)
                        + " expected=" + expected
                        + " naive=" + naive
                        + " heap=" + heap
                        + " unionFind=" + unionFind
                        + " treeSet=" + treeSet
                        + " optimal=" + optimal);
                throw new AssertionError("Stress test mismatch on trial " + trial);
            }
        }
        System.out.println("Stress test passed: " + trials + " random trials, all approaches agree with brute-force oracle.");
    }

    public static void main(String[] args) {
        runAssertionTests();
        runStressTest();

        // Final sanity print for the three walked examples.
        System.out.println("Example 1 result: " + maxEvents(new int[][]{{1,2},{2,3},{3,4},{1,2}}));
        System.out.println("Example 2 result: " + maxEvents(new int[][]{{1,1},{1,1},{1,1}}));
        System.out.println("Example 3 result: " + maxEvents(new int[][]{{1,4},{4,4},{2,2},{3,4},{1,1}}));
    }
}

class Solution {

    // A record is perfect here because an Event is just data.
    record Event(int start, int end) {}

    public int maxEvents(int[][] events) {

        // Convert int[][] -> Event[] and sort by start day.
        Event[] sortedEvents = Arrays.stream(events)
                .map(event -> new Event(event[0], event[1]))
                .sorted(Comparator.comparingInt(Event::start))
                .toArray(Event[]::new);

        // Min-heap:
        // The event with the earliest end day gets highest priority.
        PriorityQueue<Event> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(Event::end));

        int attended = 0;
        int i = 0;
        int day = 0;

        while (i < sortedEvents.length || !minHeap.isEmpty()) {

            /*
             * If there are no currently available events,
             * jump directly to the next event's start day.
             *
             * This avoids unnecessarily iterating through empty days.
             */
            if (minHeap.isEmpty()) {
                day = Math.max(day, sortedEvents[i].start());
            }

            /*
             * Add every event that has already started.
             *
             * These events are now candidates to be attended.
             */
            while (i < sortedEvents.length
                    && sortedEvents[i].start() <= day) {

                minHeap.offer(sortedEvents[i]);
                i++;
            }

            /*
             * Remove events that can no longer be attended.
             *
             * Their end day is before today.
             */
            while (!minHeap.isEmpty()
                    && minHeap.peek().end() < day) {

                minHeap.poll();
            }

            /*
             * Attend the event that expires earliest.
             *
             * This is the greedy choice:
             * save events with later deadlines for later days.
             */
            if (!minHeap.isEmpty()) {
                minHeap.poll();
                attended++;
                day++;
            }
        }

        return attended;
    }
}

/**
 * ============================================================================
 * PROBLEM STATEMENT
 * ============================================================================
 * You are given an array events, where each events[i] = [startDay_i, endDay_i] 
 * representing an event that can be attended on any single day from startDay_i 
 * through endDay_i inclusive. 
 * 
 * Rules:
 * 1. You can attend at most one event per day.
 * 2. Each event can be attended at most once.
 * 
 * Return the maximum number of events you can attend.
 * 
 * CONSTRAINTS:
 * - 1 <= events.length <= 10^5
 * - events[i].length == 2
 * - 1 <= startDay_i <= endDay_i <= 10^5
 * 
 * ============================================================================
 * VISUALIZATION OF THE PROBLEM
 * ============================================================================
 * Imagine events as overlapping intervals on a calendar. The greedy choice is 
 * always to attend the event that is going to END first (earliest deadline), 
 * provided we can fit it in an available day.
 * 
 * Example: events = [[1, 2], [2, 3], [3, 4], [1, 2]]
 * 
 * Timeline:
 * Day 1:  [E1:1-2], [E4:1-2]
 * Day 2:  [E1:1-2], [E4:1-2], [E2:2-3]
 * Day 3:  [E2:2-3], [E3:3-4]
 * Day 4:  [E3:3-4]
 * 
 * Greedy Choice Execution:
 * - Day 1: E1 and E4 end on Day 2. Pick E1.
 * - Day 2: E4 ends on Day 2, E2 ends on Day 3. Pick E4 (ends earlier!).
 * - Day 3: E2 ends on Day 3, E3 ends on Day 4. Pick E2.
 * - Day 4: Pick E3.
 * Total attended: 4.
 * ============================================================================
 */
class MaxEventsToAttend {

    /**
     * Using Java 14+ Records to represent an Event cleanly.
     * This provides a clean, immutable data carrier.
     */
    public record Event(int startDay, int endDay) implements Comparable<Event> {
        @Override
        public int compareTo(Event other) {
            // Primarily sort by start day. 
            if (this.startDay != other.startDay) {
                return Integer.compare(this.startDay, other.startDay);
            }
            return Integer.compare(this.endDay, other.endDay);
        }
    }

    /**
     * ========================================================================
     * SOLUTION 1: MIN-HEAP (Priority Queue) - MOST OPTIMAL/STANDARD
     * ========================================================================
     * EXPLANATION:
     * 1. Sort the events by their start day.
     * 2. Use a Min-Heap to keep track of the *end days* of events that have 
     *    started and are currently available to attend.
     * 3. Iterate day by day:
     *    - Add all events starting on the current day to the Min-Heap.
     *    - Remove any events from the Min-Heap that have already ended 
     *      (endDay < current day).
     *    - If the Min-Heap is not empty, pop the event that ends earliest 
     *      (heap top), increment our attended count, and move to the next day.
     *    - Optimization: If the heap is empty, jump the current day to the 
     *      start day of the next available event in our sorted array.
     * 
     * COMPLEXITY:
     * - Time: O(N log N) - Sorting the array takes O(N log N). Each event is 
     *   pushed and popped from the Priority Queue exactly once, taking O(N log N).
     * - Space: O(N) - Storing events in the heap.
     * ========================================================================
     */
    public static int maxEventsMinHeap(int[][] eventsArray) {
        if (eventsArray == null || eventsArray.length == 0) return 0;

        // Sort events primarily by start time
        Arrays.sort(eventsArray, (a, b) -> Integer.compare(a[0], b[0]));
        
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        int maxAttended = 0;
        int i = 0;
        int n = eventsArray.length;
        int currentDay = 0;

        while (i < n || !minHeap.isEmpty()) {
            // Optimization: If no events are available, fast-forward to the next event's start day
            if (minHeap.isEmpty()) {
                currentDay = eventsArray[i][0];
            }
            
            // Add all events that start on or before the current day
            while (i < n && eventsArray[i][0] <= currentDay) {
                minHeap.offer(eventsArray[i][1]); // Store the end day
                i++;
            }
            
            // Attend the event that ends the earliest
            minHeap.poll();
            maxAttended++;
            currentDay++;
            
            // Remove events that are no longer available (they ended before the new currentDay)
            while (!minHeap.isEmpty() && minHeap.peek() < currentDay) {
                minHeap.poll();
            }
        }
        
        return maxAttended;
    }

    /**
     * ========================================================================
     * SOLUTION 2: DISJOINT SET UNION (UNION-FIND) - VERY FAST
     * ========================================================================
     * EXPLANATION:
     * 1. Sort the events by their *end day* first. If end days are equal, by start day.
     *    (Greedy principle: always process events that finish earlier).
     * 2. We maintain a timeline using a Disjoint Set (Union-Find). 
     *    `parent[d]` points to the earliest available day >= d.
     * 3. For each event, we ask the DSU: "What is the next available day starting 
     *    from this event's start day?"
     * 4. If the returned day is <= the event's end day, we attend it on that day.
     * 5. We then merge that day with the next day (d + 1) in the DSU so future 
     *    queries will skip the used day.
     * 
     * COMPLEXITY:
     * - Time: O(N log N + N * \alpha(D)), where D is max days (100,000) and 
     *   \alpha is the Inverse Ackermann function (nearly constant time).
     * - Space: O(D) - The parent array takes space proportional to the max day.
     * ========================================================================
     */
    public static int maxEventsUnionFind(int[][] events) {
        if (events == null || events.length == 0) return 0;

        // Sort by end time to greedily process events that expire first
        Arrays.sort(events, (a, b) -> Integer.compare(a[1], b[1]));

        int maxDay = 0;
        for (int[] event : events) {
            maxDay = Math.max(maxDay, event[1]);
        }

        // Initialize DSU parent array up to maxDay + 1
        int[] parent = new int[maxDay + 2];
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }

        int maxAttended = 0;
        for (int[] event : events) {
            int start = event[0];
            int end = event[1];

            // Find the earliest available day on or after 'start'
            int availableDay = find(parent, start);

            if (availableDay <= end) {
                maxAttended++;
                // Mark this day as used by linking it to the next day
                parent[availableDay] = find(parent, availableDay + 1);
            }
        }

        return maxAttended;
    }

    // Standard Union-Find 'find' method with path compression
    private static int find(int[] parent, int i) {
        if (parent[i] == i) {
            return i;
        }
        return parent[i] = find(parent, parent[i]);
    }

    /**
     * ========================================================================
     * SOLUTION 3: TREESET (Available Days Tracking)
     * ========================================================================
     * EXPLANATION:
     * 1. Similar to the DSU approach, sort events by *end day* first.
     * 2. Pre-populate a TreeSet with all possible days (1 to 100,000).
     * 3. For each event, use TreeSet's `ceiling()` method to find the earliest 
     *    available day >= the event's startDay.
     * 4. If that day exists and is <= the event's endDay, we attend the event.
     * 5. Remove that day from the TreeSet so it cannot be used again.
     * 
     * COMPLEXITY:
     * - Time: O(N log N + N log D) - Sorting events + querying/removing from TreeSet. 
     *   (D is max days up to 100,000).
     * - Space: O(D) - The TreeSet stores all available days initially.
     * ========================================================================
     */
    public static int maxEventsTreeSet(int[][] events) {
        if (events == null || events.length == 0) return 0;

        // Sort by end day
        Arrays.sort(events, (a, b) -> Integer.compare(a[1], b[1]));

        int maxDay = 0;
        for (int[] event : events) {
            maxDay = Math.max(maxDay, event[1]);
        }

        // Populate TreeSet with all days
        TreeSet<Integer> availableDays = new TreeSet<>();
        for (int i = 1; i <= maxDay; i++) {
            availableDays.add(i);
        }

        int maxAttended = 0;
        for (int[] event : events) {
            // Find the lowest day >= event start day
            Integer dayToAttend = availableDays.ceiling(event[0]);
            
            if (dayToAttend != null && dayToAttend <= event[1]) {
                maxAttended++;
                availableDays.remove(dayToAttend); // Mark as used
            }
        }

        return maxAttended;
    }

    /**
     * ========================================================================
     * MAIN METHOD: Executing and verifying the examples
     * ========================================================================
     */
    public static void main(String[] args) {
        int[][] test1 = {{1, 2}, {2, 3}, {3, 4}};
        int[][] test2 = {{1, 2}, {2, 3}, {3, 4}, {1, 2}};
        int[][] test3 = {{1, 4}, {4, 4}, {2, 2}, {3, 4}, {1, 1}}; 
        
        System.out.println("Test Case 1: [[1, 2], [2, 3], [3, 4]]");
        System.out.println("Expected: 3");
        System.out.println("Min-Heap Solution:   " + maxEventsMinHeap(test1.clone()));
        System.out.println("Union-Find Solution: " + maxEventsUnionFind(test1.clone()));
        System.out.println("TreeSet Solution:    " + maxEventsTreeSet(test1.clone()));
        System.out.println("--------------------------------------------------");

        System.out.println("Test Case 2: [[1, 2], [2, 3], [3, 4], [1, 2]]");
        System.out.println("Expected: 4");
        System.out.println("Min-Heap Solution:   " + maxEventsMinHeap(test2.clone()));
        System.out.println("Union-Find Solution: " + maxEventsUnionFind(test2.clone()));
        System.out.println("TreeSet Solution:    " + maxEventsTreeSet(test2.clone()));
        System.out.println("--------------------------------------------------");

        System.out.println("Test Case 3: [[1, 4], [4, 4], [2, 2], [3, 4], [1, 1]]");
        System.out.println("Expected: 4");
        System.out.println("Min-Heap Solution:   " + maxEventsMinHeap(test3.clone()));
        System.out.println("Union-Find Solution: " + maxEventsUnionFind(test3.clone()));
        System.out.println("TreeSet Solution:    " + maxEventsTreeSet(test3.clone()));
    }
}
