import java.util.*;

/*
================================================================================
 GOOGLE ONSITE MOCK INTERVIEW TRANSCRIPT
 Problem: Number of the Smallest Unoccupied Chair  (LeetCode 1942, isomorphic)
 Candidate pattern-library session
================================================================================
*/

class SmallestUnoccupiedChair {

    /*
    ============================================================================
    SECTION 1: RESTATE THE PROBLEM
    ============================================================================
    In my own words:

    - n friends attend a party. Friend i arrives at times[i][0] and leaves at
      times[i][1]. Arrival times are strictly less than leaving times, and all
      arrival times are pairwise unique (leaving times may repeat or coincide
      with other arrival times).
    - There are infinitely many chairs numbered 0, 1, 2, ... When a friend
      arrives, they take the SMALLEST-numbered chair that is currently free.
    - When a friend leaves, their chair becomes free again immediately -- if
      another friend arrives at the exact same timestamp, that friend may take
      the just-vacated chair (departure is processed "at or before" an
      arrival with the same timestamp).
    - Given a specific index `targetFriend`, return the chair number that
      friend ends up sitting in.

    Inputs:
      - int[][] times, where times[i] = [arrival_i, leaving_i]
      - int targetFriend, an index into times

    Output:
      - A single int: the chair number assigned to friend `targetFriend`.

    Implicit assumptions to validate with interviewer in Section 2:
      - times is not necessarily given sorted by arrival time.
      - At most n friends are ever present simultaneously, so the maximum
        chair index ever used is bounded by n - 1 (this is the key insight
        that turns "infinitely many chairs" into a bounded resource problem).
    ============================================================================
    */


    /*
    ============================================================================
    SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
    ============================================================================
    1. Q: What are the bounds on n and on the time values?
       A (assumed): 1 <= n <= 1e5, 1 <= arrival_i < leaving_i <= 1e5. Large
          enough that O(n^2) risks TLE and O(n log n) is the expected bar.

    2. Q: Are arrival times guaranteed unique? Are leaving times guaranteed
          unique, and can a leaving time collide with another arrival time?
       A (assumed): Arrival times are unique (stated in constraints). Leaving
          times are NOT guaranteed unique and MAY collide with arrival times.
          When they collide, the departure is processed first, freeing the
          chair for the simultaneous arrival.

    3. Q: Is the input array already sorted by arrival time?
       A (assumed): No -- treat it as arbitrary order and sort as needed.

    4. Q: Do we need to output chair assignments for every friend, or just
          the one target friend?
       A (assumed): Just the target friend for this call, but the algorithm
          naturally computes all assignments as a byproduct, so I'll design
          for "compute everyone, answer the one asked."

    5. Q: Could this function be called many times with different
          targetFriend values but the same `times` array?
       A (assumed): Possibly, in a follow-up. For a single call we recompute;
          I'll flag in Follow-Ups how to amortize repeated queries.

    6. Q: Is there any concurrency concern -- could friends arrive/leave from
          multiple threads simultaneously?
       A (assumed): No, this is an offline batch computation over a static
          input array; no concurrency handling is required.

    7. Q: What should happen with duplicate arrival times?
       A (assumed): Guaranteed not to happen per constraints, so no
          tie-breaking rule is needed for two arrivals at the same instant.

    8. Q: Is targetFriend guaranteed to be a valid index (0 <= targetFriend
          < n)?
       A (assumed): Yes, per constraints; no bounds-checking required, though
          production code will still guard defensively.
    ============================================================================
    */


    /*
    ============================================================================
    SECTION 3: EXAMPLES & EDGE CASES
    ============================================================================

    Example 1 (normal case):
      times = [[1,4],[2,3],[4,6]], targetFriend = 1
      - Friend0 arrives@1 -> chair 0 (nothing occupied yet)
      - Friend1 arrives@2 -> chair 1 (chair 0 still occupied until t=4)
      - Friend1 leaves@3  -> chair 1 freed
      - Friend2 arrives@4 -> friend0 leaves@4 too (processed first), so
        chairs {0,1} are both free; friend2 takes smallest = chair 0
      - Answer for targetFriend=1 -> chair 1

    Example 2 (boundary / simultaneous arrival+departure tie):
      times = [[3,10],[1,5],[2,6]], targetFriend = 0
      - Sort by arrival: friend1@1 (leaves5), friend2@2 (leaves6),
        friend0@3 (leaves10)
      - Friend1 arrives@1 -> chair 0
      - Friend2 arrives@2 -> chair 1
      - Friend0 arrives@3 -> chairs {0,1} both occupied (leaves at 5 and 6,
        both > 3) -> chair 2 (a genuinely new chair, testing the
        "next fresh chair" counter path)
      - Answer for targetFriend=0 -> chair 2

    Example 3 (edge case: single friend / minimal input):
      times = [[5,10]], targetFriend = 0
      - Only one friend, no contention at all -> chair 0
      - Exercises the trivial base case and guards against off-by-one errors
        when heaps/structures start empty.

    Additional edge cases considered:
      - Exact tie between a departure and an arrival at the same timestamp
        (covered in Example 1) -- must free BEFORE assigning, using "<=" not
        "<" in the release condition.
      - A friend who leaves right as another friend's chair search happens,
        where multiple departures share the same leaving time -- all must be
        released before the new arrival is processed.
      - times.length == n could be as large as 1e5 -- must avoid true
        "infinite chairs" simulation (e.g., no unbounded array indexed by
        chair number without a cap); cap chair space at n since at most n
        friends are ever simultaneously present.
    ============================================================================
    */


    /*
    ============================================================================
    SECTION 4 & 5: ALL POSSIBLE SOLUTIONS
    ============================================================================
    Paradigms considered and their applicability:

      - Brute force / naive simulation ................ APPLICABLE (Approach 1)
      - Sorting-based ................................. APPLICABLE (core of all approaches)
      - Hashing-based .................................. NOT NEEDED: we never need
            arbitrary key->value lookups; friend index simply travels alongside
            the sorted arrival records as an int, so a HashMap would only add
            overhead with no benefit over a parallel array.
      - Two pointer / sliding window ................... PARTIALLY APPLICABLE:
            the optimal solution advances through arrival-sorted friends with
            a single pointer while draining a heap of departures -- a
            two-pointer *flavor* -- but pure two-pointer alone cannot answer
            "what is the smallest free chair" without an auxiliary ordered
            structure (heap or segment tree), so it's folded into Approach 2
            rather than listed standalone.
      - Divide and conquer ............................. NOT APPLICABLE: the
            problem is an inherently sequential event-stream simulation;
            there's no way to split it into independent subproblems whose
            solutions merge to give correct global chair assignments (chair
            reuse depends on the entire history so far).
      - Greedy ......................................... APPLICABLE (implicit in
            Approaches 2 & 3: always take the smallest available chair is a
            greedy, provably-optimal local choice).
      - Dynamic programming ............................ NOT APPLICABLE: there is
            no overlapping-subproblem / optimal-substructure formulation here;
            chair assignment is a direct simulation, not an optimization over
            choices with memoizable state.
      - Tree / graph traversal .......................... NOT APPLICABLE: no
            graph or tree relationship exists between friends or chairs.
      - Heap / priority queue ........................... APPLICABLE (Approach 2,
            the primary optimal approach).
      - Binary search ................................... NOT DIRECTLY APPLICABLE
            to the core problem (no monotonic predicate to search over), but a
            segment-tree "leftmost free slot" query (Approach 3) is
            conceptually a binary search over the tree's structure.
      - Monotonic stack / deque ......................... NOT APPLICABLE: chair
            release order is governed by leaving time, not by a monotonic
            push/pop discipline.
      - Trie / segment tree / advanced structures ....... APPLICABLE (Approach 3,
            using a segment tree as an order-statistics structure to find the
            leftmost free chair in O(log n), useful when the problem is
            extended to support range queries or many repeated lookups).
    ============================================================================
    */


    /*
    ----------------------------------------------------------------------------
    APPROACH 1: Brute Force Simulation with Linear Scan
    ----------------------------------------------------------------------------
    Core idea:
      Sort friends by arrival time. Maintain a boolean[] occupied array sized
      n (since at most n chairs are ever needed) and a parallel int[]
      chairLeavingTime array recording, for each occupied chair, when its
      current occupant leaves. For every arriving friend: (a) linearly scan
      all chairs to free any whose leaving time has passed, then (b) linearly
      scan from chair 0 upward to find the first unoccupied chair.

    Data structure / paradigm: plain arrays, linear scan (no auxiliary
    ordered structure).

    Time Complexity: O(n^2) -- for each of the n arrivals we may scan up to
      n chairs twice (once to release, once to find the smallest free one).

    Space Complexity: O(n) -- occupied[] and chairLeavingTime[] each sized n,
      plus O(n) for the sorted arrival order.

    Pros:
      - Trivial to reason about and verify by hand; minimal risk of subtle
        bugs; excellent as a correctness oracle for stress testing.
      - No auxiliary data structures beyond arrays -- very low constant
        factor for small n.

    Cons:
      - Quadratic time is unacceptable at n = 1e5 (10^10 operations).
      - Doesn't showcase algorithmic sophistication expected at this level.

    When to use:
      - Never in production for the stated constraints; useful only as a
        correctness oracle during development/testing, or if n is tiny
        (n <= ~2000) and simplicity trumps performance.
    ----------------------------------------------------------------------------
    */
    static int bruteForceSimulation(int[][] times, int targetFriend) {
        int n = times.length;

        // Pair each friend's (arrival, leaving) with their original index so
        // we can report the target's chair after sorting by arrival time.
        Integer[] orderByArrival = new Integer[n];
        for (int i = 0; i < n; i++) orderByArrival[i] = i;
        Arrays.sort(orderByArrival, (a, b) -> Integer.compare(times[a][0], times[b][0]));

        boolean[] occupied = new boolean[n];        // occupied[c] == true if chair c is taken
        int[] chairLeavingTime = new int[n];         // leaving time of whoever sits in chair c
        Arrays.fill(chairLeavingTime, -1);

        int[] chairOfFriend = new int[n];

        for (int friendIndex : orderByArrival) {
            int arrival = times[friendIndex][0];

            // Step (a): release every chair whose occupant has already left
            // (leaving time <= current arrival time handles the same-instant
            // departure-before-arrival rule).
            for (int chair = 0; chair < n; chair++) {
                if (occupied[chair] && chairLeavingTime[chair] <= arrival) {
                    occupied[chair] = false;
                }
            }

            // Step (b): linear scan for the smallest free chair.
            int assignedChair = -1;
            for (int chair = 0; chair < n; chair++) {
                if (!occupied[chair]) {
                    assignedChair = chair;
                    break;
                }
            }

            occupied[assignedChair] = true;
            chairLeavingTime[assignedChair] = times[friendIndex][1];
            chairOfFriend[friendIndex] = assignedChair;
        }

        return chairOfFriend[targetFriend];
    }


    /*
    ----------------------------------------------------------------------------
    APPROACH 2: Sorting + Dual Min-Heap  (RECOMMENDED / OPTIMAL)
    ----------------------------------------------------------------------------
    Core idea:
      Sort friends by arrival time (this is the "single pointer" scan over
      time). Maintain:
        - `occupiedByLeaving`: a min-heap of (leavingTime, chairNumber),
          ordered by leavingTime, representing currently-seated friends.
        - `freeChairs`: a min-heap of chair numbers that have been vacated
          and are available for reuse.
        - `nextFreshChair`: a counter for chairs that have never been used.
      For each friend in arrival order: first pop every entry from
      `occupiedByLeaving` whose leavingTime <= this friend's arrival time,
      pushing its chair into `freeChairs`. Then assign the friend the
      smallest chair from `freeChairs` if any exist, otherwise mint a new
      chair via `nextFreshChair++`. Push (thisLeavingTime, assignedChair)
      onto `occupiedByLeaving`.

    Data structure / paradigm: greedy simulation driven by two priority
      queues -- one ordered by time (to know what to release next), one
      ordered by chair number (to know the smallest reusable chair).

    Time Complexity: O(n log n) -- O(n log n) to sort by arrival, and each
      friend causes O(1) amortized heap pushes/pops across both heaps, each
      O(log n), for O(n log n) total.

    Space Complexity: O(n) -- both heaps hold at most n elements combined at
      any time, plus O(n) for the sorted order and the answer array.

    Pros:
      - Optimal asymptotic complexity, comfortably passes n = 1e5.
      - Maps directly onto Java's PriorityQueue -- fast to write correctly
        under interview time pressure.
      - Greedy correctness is easy to argue: always taking the smallest
        available resource is optimal here because chair identity has no
        cost difference other than its number, so there's never a benefit
        to "saving" a small chair for later.

    Cons:
      - Two separate heaps add a small constant-factor and some bookkeeping
        compared to a single structure.
      - Slightly more moving parts than the brute force, so it's a bit more
        error-prone around the same-timestamp release rule.

    When to use:
      - This is the general-purpose, go-to solution for the stated
        constraints and what I would code in an actual onsite.
    ----------------------------------------------------------------------------
    */
    static int optimalDualHeap(int[][] times, int targetFriend) {
        int n = times.length;

        // Sort friend indices by arrival time -- this is our "event
        // timeline" pointer; we never need to re-sort or re-scan it.
        Integer[] orderByArrival = new Integer[n];
        for (int i = 0; i < n; i++) orderByArrival[i] = i;
        Arrays.sort(orderByArrival, (a, b) -> Integer.compare(times[a][0], times[b][0]));

        // Min-heap of currently occupied chairs, ordered by leaving time, so
        // we always know which chair frees up next.
        PriorityQueue<int[]> occupiedByLeaving =
                new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0])); // {leavingTime, chair}

        // Min-heap of chair numbers that have been vacated and can be reused.
        PriorityQueue<Integer> freeChairs = new PriorityQueue<>();

        int nextFreshChair = 0;
        int[] chairOfFriend = new int[n];

        for (int friendIndex : orderByArrival) {
            int arrival = times[friendIndex][0];
            int leaving = times[friendIndex][1];

            // Release every chair whose occupant's leaving time is <= this
            // arrival. "<=" (not "<") is what correctly models "a friend
            // leaving at the same instant frees the chair for the arriver."
            while (!occupiedByLeaving.isEmpty() && occupiedByLeaving.peek()[0] <= arrival) {
                int[] freed = occupiedByLeaving.poll();
                freeChairs.offer(freed[1]);
            }

            // Prefer reusing the smallest vacated chair; only mint a brand
            // new chair number if nothing is available for reuse.
            int assignedChair = freeChairs.isEmpty() ? nextFreshChair++ : freeChairs.poll();

            occupiedByLeaving.offer(new int[]{leaving, assignedChair});
            chairOfFriend[friendIndex] = assignedChair;
        }

        return chairOfFriend[targetFriend];
    }


    /*
    ----------------------------------------------------------------------------
    APPROACH 3: Sorting + Segment Tree (Order-Statistics "Leftmost Free Slot")
    ----------------------------------------------------------------------------
    Core idea:
      Cap the chair universe at size n (at most n friends are ever
      simultaneously seated). Build a segment tree over indices [0, n-1]
      where each leaf stores 1 if that chair is free, 0 if occupied, and
      each internal node stores the count of free chairs in its range. To
      assign a chair, descend the tree always preferring the left child if
      it has any free chairs, else recurse right -- this finds the leftmost
      free index in O(log n). Releasing/occupying a chair is a point update
      that also runs in O(log n). We still sort by arrival and use a small
      min-heap purely to know *when* to release chairs (same as Approach 2),
      but chair *selection* now uses the segment tree instead of a heap.

    Data structure / paradigm: segment tree as an order-statistics structure
      (supports "find k-th / leftmost free element" and point update), a
      textbook advanced-structure technique.

    Time Complexity: O(n log n) -- same asymptotic class as Approach 2:
      O(n log n) to sort, O(log n) per point update/leftmost-query, O(n) of
      them.

    Space Complexity: O(n) -- segment tree array sized ~4n, plus O(n) for
      sort order and answer array.

    Pros:
      - Generalizes gracefully: if a follow-up asks "how many chairs are
        occupied in range [lo, hi] at a given moment?" or "what's the k-th
        smallest free chair?", the segment tree answers these in O(log n)
        with minor extensions, whereas the dual-heap approach does not
        support arbitrary range queries at all.
      - Deterministic O(log n) worst case per operation (heaps are also
        O(log n) but with different constants; segment tree here is more
        naturally suited to range-style follow-ups).

    Cons:
      - Meaningfully more code and more places to introduce off-by-one bugs
        (tree bounds, recursive descent) than a PriorityQueue-based solution.
      - Same big-O as Approach 2 but higher constant factor for this exact
        problem -- there's no query variant being asked here, so the extra
        power goes unused.

    When to use:
      - I would NOT lead with this in a first pass under interview time
        pressure. I would mention it proactively as a "here's how I'd
        extend this if you added range queries" follow-up, and code it only
        if explicitly asked to support such an extension.
    ----------------------------------------------------------------------------
    */
    static int segmentTreeApproach(int[][] times, int targetFriend) {
        int n = times.length;

        Integer[] orderByArrival = new Integer[n];
        for (int i = 0; i < n; i++) orderByArrival[i] = i;
        Arrays.sort(orderByArrival, (a, b) -> Integer.compare(times[a][0], times[b][0]));

        // Small heap purely to know WHEN chairs free up (time-ordering),
        // chair SELECTION is delegated to the segment tree below.
        PriorityQueue<int[]> occupiedByLeaving =
                new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0])); // {leavingTime, chair}

        FreeChairSegmentTree freeChairTree = new FreeChairSegmentTree(n);
        int[] chairOfFriend = new int[n];

        for (int friendIndex : orderByArrival) {
            int arrival = times[friendIndex][0];
            int leaving = times[friendIndex][1];

            while (!occupiedByLeaving.isEmpty() && occupiedByLeaving.peek()[0] <= arrival) {
                int[] freed = occupiedByLeaving.poll();
                freeChairTree.markFree(freed[1]);
            }

            int assignedChair = freeChairTree.leftmostFreeChair();
            freeChairTree.markOccupied(assignedChair);

            occupiedByLeaving.offer(new int[]{leaving, assignedChair});
            chairOfFriend[friendIndex] = assignedChair;
        }

        return chairOfFriend[targetFriend];
    }

    /** Segment tree tracking free-chair counts to support O(log n) point
     *  update and "leftmost free index" queries over a fixed-size range. */
    static final class FreeChairSegmentTree {
        private final int size;
        private final int[] freeCount; // freeCount[node] = # free chairs in that node's range

        FreeChairSegmentTree(int size) {
            this.size = size;
            this.freeCount = new int[4 * Math.max(size, 1)];
            if (size > 0) build(1, 0, size - 1);
        }

        private void build(int node, int rangeStart, int rangeEnd) {
            if (rangeStart == rangeEnd) {
                freeCount[node] = 1; // every chair starts free
                return;
            }
            int mid = (rangeStart + rangeEnd) / 2;
            build(2 * node, rangeStart, mid);
            build(2 * node + 1, mid + 1, rangeEnd);
            freeCount[node] = freeCount[2 * node] + freeCount[2 * node + 1];
        }

        void markOccupied(int chairIndex) { update(1, 0, size - 1, chairIndex, 0); }

        void markFree(int chairIndex) { update(1, 0, size - 1, chairIndex, 1); }

        private void update(int node, int rangeStart, int rangeEnd, int targetIndex, int newValue) {
            if (rangeStart == rangeEnd) {
                freeCount[node] = newValue;
                return;
            }
            int mid = (rangeStart + rangeEnd) / 2;
            if (targetIndex <= mid) update(2 * node, rangeStart, mid, targetIndex, newValue);
            else update(2 * node + 1, mid + 1, rangeEnd, targetIndex, newValue);
            freeCount[node] = freeCount[2 * node] + freeCount[2 * node + 1];
        }

        /** Returns the smallest free chair index, assuming at least one exists. */
        int leftmostFreeChair() { return query(1, 0, size - 1); }

        private int query(int node, int rangeStart, int rangeEnd) {
            if (rangeStart == rangeEnd) return rangeStart;
            int mid = (rangeStart + rangeEnd) / 2;
            // Always prefer the left half if it has ANY free chair -- this
            // is what guarantees we find the smallest index overall.
            if (freeCount[2 * node] > 0) return query(2 * node, rangeStart, mid);
            return query(2 * node + 1, mid + 1, rangeEnd);
        }
    }


    /*
    ============================================================================
    SECTION 7: APPROACHES COMPARISON TABLE
    ============================================================================
    Approach                          | Time       | Space | Best For                                  | Limitations
    -----------------------------------|------------|-------|-------------------------------------------|--------------------------------------------
    1. Brute Force Linear Scan         | O(n^2)     | O(n)  | Small n, correctness oracle, teaching     | TLE at n = 1e5; not production-viable
    2. Sorting + Dual Min-Heap         | O(n log n) | O(n)  | General-purpose optimal interview answer  | No native support for range/rank queries
    3. Sorting + Segment Tree          | O(n log n) | O(n)  | Follow-ups needing range queries or ranks | More code, higher constant factor, same Big-O
    ============================================================================
    */


    /*
    ============================================================================
    SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
    ============================================================================
    I would present Approach 2 (Sorting + Dual Min-Heap) as my primary
    solution:

    - It is asymptotically optimal at O(n log n), matching interviewer
      expectations for this problem class (comparable to "meeting rooms II"
      style scheduling problems).
    - It is fast to code correctly under time pressure -- Java's
      PriorityQueue with a comparator handles both heaps cleanly in a few
      lines, versus the extra recursive scaffolding a segment tree needs.
      Ratio of "code volume" to "insight demonstrated" is excellent.
    - The greedy argument (always reuse the smallest free chair; if none
      exists, mint the next integer) is easy to state and justify out loud,
      which matters as much as the code itself in an onsite.
    - I would proactively mention the segment tree variant (Approach 3) as
      an extension point if asked about range queries, k-th smallest free
      chair, or many repeated queries against a slowly-changing chair set --
      demonstrating breadth without over-engineering the base solution.
    ============================================================================
    */


    /*
    ============================================================================
    SECTION 9: DEEP DIVE -- PRODUCTION-QUALITY OPTIMAL IMPLEMENTATION
    ============================================================================
    (See `optimalDualHeap` above for the primary logic; a slightly hardened,
    fully-annotated production wrapper is provided below with explicit input
    validation and defensive checks suitable for a real codebase.)
    ----------------------------------------------------------------------------
    */
    static int smallestUnoccupiedChair(int[][] times, int targetFriend) {
        // --- Defensive input validation (production code, not interview code) ---
        if (times == null || times.length == 0) {
            throw new IllegalArgumentException("times must be a non-empty array.");
        }
        int n = times.length;
        if (targetFriend < 0 || targetFriend >= n) {
            throw new IllegalArgumentException("targetFriend index out of bounds.");
        }
        for (int[] friendTimes : times) {
            if (friendTimes == null || friendTimes.length != 2) {
                throw new IllegalArgumentException("Each entry must be [arrival, leaving].");
            }
            if (friendTimes[0] >= friendTimes[1]) {
                throw new IllegalArgumentException("Arrival time must be strictly before leaving time.");
            }
        }

        // --- Core algorithm (identical logic to optimalDualHeap) ---
        Integer[] orderByArrival = new Integer[n];
        for (int i = 0; i < n; i++) orderByArrival[i] = i;
        // Sort strictly by arrival time; arrivals are guaranteed unique per
        // the problem constraints, so no tie-break comparator is needed.
        Arrays.sort(orderByArrival, (left, right) -> Integer.compare(times[left][0], times[right][0]));

        // Min-heap of {leavingTime, chairNumber} for everyone currently seated,
        // ordered by leavingTime so we always know what frees up next.
        PriorityQueue<int[]> occupiedByLeaving = new PriorityQueue<>(
                Comparator.comparingInt(entry -> entry[0])
        );

        // Min-heap of chair numbers available for immediate reuse.
        PriorityQueue<Integer> freeChairs = new PriorityQueue<>();

        int nextFreshChair = 0;                 // next never-before-used chair number
        int[] chairAssignedTo = new int[n];      // chairAssignedTo[friendIndex] = chair number

        for (int friendIndex : orderByArrival) {
            int arrivalTime = times[friendIndex][0];
            int leavingTime = times[friendIndex][1];

            // Free every chair whose occupant has left by (or exactly at)
            // this friend's arrival. Using "<=" correctly models the rule
            // that a same-instant departure frees the chair for the arriver.
            while (!occupiedByLeaving.isEmpty() && occupiedByLeaving.peek()[0] <= arrivalTime) {
                int[] vacated = occupiedByLeaving.poll();
                int vacatedChair = vacated[1];
                freeChairs.offer(vacatedChair);
            }

            // Greedy choice: reuse the smallest freed chair if one exists;
            // otherwise this friend needs a brand-new chair number.
            int assignedChair = freeChairs.isEmpty() ? nextFreshChair++ : freeChairs.poll();

            occupiedByLeaving.offer(new int[]{leavingTime, assignedChair});
            chairAssignedTo[friendIndex] = assignedChair;
        }

        return chairAssignedTo[targetFriend];
    }


    /*
    ============================================================================
    SECTION 10: DRY RUN / TRACE
    ============================================================================
    Tracing `smallestUnoccupiedChair` on:
      times = [[1,4],[2,3],[4,6]], targetFriend = 1

    Sorted by arrival: friend0(arr=1,leave=4), friend1(arr=2,leave=3),
                        friend2(arr=4,leave=6)   -- already in this order.

    Initial state: occupiedByLeaving = [], freeChairs = [], nextFreshChair = 0

    Step 1 -- process friend0 (arrival=1, leaving=4):
      - Release phase: occupiedByLeaving empty, nothing to release.
      - freeChairs empty -> assignedChair = nextFreshChair++ = 0
      - occupiedByLeaving = [(leave=4, chair=0)]
      - chairAssignedTo[0] = 0

    Step 2 -- process friend1 (arrival=2, leaving=3):
      - Release phase: peek is (4,0); 4 <= 2 is false -> release nothing.
      - freeChairs empty -> assignedChair = nextFreshChair++ = 1
      - occupiedByLeaving = [(leave=3, chair=1), (leave=4, chair=0)]
      - chairAssignedTo[1] = 1

    Step 3 -- process friend2 (arrival=4, leaving=6):
      - Release phase:
          peek (3,1); 3 <= 4 true -> pop, freeChairs = [1]
          peek (4,0); 4 <= 4 true -> pop, freeChairs = [0, 1]
          occupiedByLeaving now empty -> stop releasing
      - freeChairs non-empty -> assignedChair = freeChairs.poll() = 0
        (freeChairs becomes [1])
      - occupiedByLeaving = [(leave=6, chair=0)]
      - chairAssignedTo[2] = 0

    Final: chairAssignedTo = [0, 1, 0]
    Answer for targetFriend = 1  ->  chair 1   (matches expected result)
    ============================================================================
    */


    /*
    ============================================================================
    SECTION 11: CLOSING SUMMARY
    ============================================================================
    - Approach 1 (brute force linear scan) is easy to trust but quadratic;
      good only as a correctness oracle or for tiny n.
    - Approach 2 (sorting + dual min-heap) is the recommended, optimal,
      interview-ready O(n log n) solution -- greedy chair reuse driven by a
      time-ordered heap plus a chair-ordered heap.
    - Approach 3 (segment tree) matches Approach 2's Big-O but adds real
      extensibility for range/rank-style follow-up queries, at the cost of
      more code and a higher constant factor for the base problem as stated.
    - Known assumptions / limitations of the final solution:
        * Assumes arrival times are unique (per constraints) -- if that
          guarantee were dropped, we would need an explicit tie-break rule
          for simultaneous arrivals.
        * Assumes at most n chairs are ever needed simultaneously, which
          always holds since there are only n friends total -- this bounds
          memory even though the problem describes "infinitely many chairs."
        * Single-query design: for many repeated queries against the same
          `times` array, precomputing chairAssignedTo[] once (as both
          Approaches 2 and 3 already do internally) and reusing it avoids
          redundant O(n log n) recomputation -- see Follow-Up Questions.
    ============================================================================
    */


    /*
    ============================================================================
    SECTION 12: FOLLOW-UP QUESTIONS
    ============================================================================
    1. "What if targetFriend is queried many times (Q queries) against the
        same `times` array?" -> Precompute chairAssignedTo[] once in
        O(n log n), then answer each query in O(1); do not recompute per query.

    2. "What if we also need to support 'how many chairs are occupied at
        time T' or 'what is the k-th smallest occupied chair at time T'?"
        -> This is exactly where the segment tree (Approach 3) shines;
        extend it to answer rank/range queries directly instead of just
        leftmost-free.

    3. "What if arrival times are NOT guaranteed unique?" -> Define an
        explicit tie-break (e.g., process by a secondary key such as input
        order or friend index) since simultaneous arrivals would otherwise
        race for the same smallest chair non-deterministically.

    4. "What if n scales to 1e7 or higher, or this runs as a streaming
        service processing arrivals/departures in real time rather than as
        a static batch?" -> Discuss switching to an online algorithm: same
        dual-heap idea but processing true real-time events instead of a
        presorted static array, plus discuss heap memory footprint at scale.

    5. "Can you bound the maximum chair number ever used, and does that
        change your data structure choice?" -> Yes -- it's bounded by n,
        which is exactly what makes the segment tree (fixed-size array)
        viable instead of needing a dynamic/unbounded structure.

    6. "How would you test this?" -> Randomized stress testing against the
        brute-force oracle (as done in this file's main method), plus
        targeted edge cases: single friend, all friends overlapping fully,
        no overlap at all (everyone sequential), and mass simultaneous
        departures at one timestamp feeding a mass simultaneous arrival.
    ============================================================================
    */


    /*
    ============================================================================
    SECTION 13: WHAT CANDIDATES TYPICALLY MISS
    ============================================================================
    1. Using strict "<" instead of "<=" when deciding whether to release a
       chair relative to the current arrival time -- this silently breaks
       the "simultaneous departure frees the chair for a simultaneous
       arrival" rule and produces wrong answers only on tie cases, which is
       easy to miss without a dedicated boundary test.

    2. Forgetting that "infinitely many chairs" is a red herring -- the
       reachable chair numbers are bounded by n (total friend count), which
       is what makes a fixed-size segment tree or bounded array valid at
       all. Candidates sometimes over-engineer an "unbounded" structure.

    3. Sorting only by arrival time and forgetting that friends must be
       reconnected back to their ORIGINAL index afterward to report the
       correct targetFriend's chair -- a classic "lost the mapping after
       sorting" bug. Using Integer[] of indices (or an {arrival, leaving,
       originalIndex} triple) avoids this.

    4. Assuming a single heap is sufficient. Candidates often reach for one
       PriorityQueue and try to make it serve double duty as both "who
       leaves next" and "smallest free chair," which doesn't work because
       those are two different orderings over two different domains (time
       vs. chair number) -- this problem genuinely needs two structures (or
       one heap + one segment tree).
    ============================================================================
    */


    /*
    ============================================================================
    VERIFICATION HARNESS: Named assertions + randomized stress testing
    ============================================================================
    */
    public static void main(String[] args) {
        runNamedAssertions();
        runRandomizedStressTest(3000, 42L);
        System.out.println("All tests (named assertions + randomized stress test) passed.");
    }

    private static void runNamedAssertions() {
        // --- Example 1 from Section 3 ---
        int[][] example1 = {{1, 4}, {2, 3}, {4, 6}};
        assertEquals("Example1-target1-bruteForce", 1, bruteForceSimulation(example1, 1));
        assertEquals("Example1-target1-dualHeap", 1, optimalDualHeap(example1, 1));
        assertEquals("Example1-target1-segmentTree", 1, segmentTreeApproach(example1, 1));
        assertEquals("Example1-target2-dualHeap", 0, optimalDualHeap(example1, 2));

        // --- Example 2 from Section 3 (fresh-chair path) ---
        int[][] example2 = {{3, 10}, {1, 5}, {2, 6}};
        assertEquals("Example2-target0-bruteForce", 2, bruteForceSimulation(example2, 0));
        assertEquals("Example2-target0-dualHeap", 2, optimalDualHeap(example2, 0));
        assertEquals("Example2-target0-segmentTree", 2, segmentTreeApproach(example2, 0));

        // --- Example 3 from Section 3 (trivial single friend) ---
        int[][] example3 = {{5, 10}};
        assertEquals("Example3-target0-dualHeap", 0, optimalDualHeap(example3, 0));
        assertEquals("Example3-target0-segmentTree", 0, segmentTreeApproach(example3, 0));

        // --- Fully sequential (no overlap at all): everyone should get chair 0 ---
        int[][] sequential = {{1, 2}, {3, 4}, {5, 6}, {7, 8}};
        for (int friend = 0; friend < sequential.length; friend++) {
            assertEquals("Sequential-target" + friend, 0, optimalDualHeap(sequential, friend));
        }

        // --- Fully overlapping (nobody ever leaves before the last arrives) ---
        int[][] overlapping = {{1, 100}, {2, 100}, {3, 100}, {4, 100}};
        // Sorted by arrival, each successive friend must mint a fresh chair: 0,1,2,3
        assertEquals("Overlapping-target0", 0, optimalDualHeap(overlapping, 0));
        assertEquals("Overlapping-target1", 1, optimalDualHeap(overlapping, 1));
        assertEquals("Overlapping-target2", 2, optimalDualHeap(overlapping, 2));
        assertEquals("Overlapping-target3", 3, optimalDualHeap(overlapping, 3));

        // --- Production wrapper sanity check against Example 1 ---
        assertEquals("Production-wrapper-example1", 1, smallestUnoccupiedChair(example1, 1));

        System.out.println("Named assertions passed.");
    }

    private static void runRandomizedStressTest(int trialCount, long seed) {
        Random random = new Random(seed);

        for (int trial = 0; trial < trialCount; trial++) {
            int n = 1 + random.nextInt(40); // small n keeps brute force oracle fast

            // Generate n unique arrival times by shuffling a range, then a
            // strictly-greater leaving time for each.
            List<Integer> arrivalPool = new ArrayList<>();
            for (int value = 1; value <= n * 3; value++) arrivalPool.add(value);
            Collections.shuffle(arrivalPool, random);

            int[][] times = new int[n][2];
            for (int friend = 0; friend < n; friend++) {
                int arrival = arrivalPool.get(friend);
                int maxExtra = 5;
                int leaving = arrival + 1 + random.nextInt(maxExtra); // guarantees leaving > arrival
                times[friend][0] = arrival;
                times[friend][1] = leaving;
            }

            int targetFriend = random.nextInt(n);

            int expected = bruteForceSimulation(times, targetFriend);
            int actualDualHeap = optimalDualHeap(times, targetFriend);
            int actualSegmentTree = segmentTreeApproach(times, targetFriend);
            int actualProductionWrapper = smallestUnoccupiedChair(times, targetFriend);

            if (expected != actualDualHeap) {
                throw new AssertionError("Mismatch (dual heap) on trial " + trial
                        + " times=" + Arrays.deepToString(times)
                        + " target=" + targetFriend
                        + " expected=" + expected + " actual=" + actualDualHeap);
            }
            if (expected != actualSegmentTree) {
                throw new AssertionError("Mismatch (segment tree) on trial " + trial
                        + " times=" + Arrays.deepToString(times)
                        + " target=" + targetFriend
                        + " expected=" + expected + " actual=" + actualSegmentTree);
            }
            if (expected != actualProductionWrapper) {
                throw new AssertionError("Mismatch (production wrapper) on trial " + trial
                        + " times=" + Arrays.deepToString(times)
                        + " target=" + targetFriend
                        + " expected=" + expected + " actual=" + actualProductionWrapper);
            }
        }

        System.out.println("Randomized stress test passed (" + trialCount + " trials, seed=" + seed + ").");
    }

    private static void assertEquals(String label, int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError(label + " -> expected=" + expected + " actual=" + actual);
        }
    }
}

class SmallestChair {

    /*
     * Record to represent each friend.
     *
     * Why record?
     * - Immutable data holder (safe & clean)
     * - Better readability than int[] (arrival, leave, index)
     * - Self-documenting → avoids confusion like arr[0], arr[1]
     */
    record Friend(int arrival, int leave, int index) {}

    /*
     * Record to represent an occupied chair
     *
     * We store:
     * - leaveTime → when this chair becomes free
     * - chairNumber → which chair is occupied
     */
    record Occupied(int leaveTime, int chairNumber) {}

    public int smallestChair(int[][] times, int targetFriend) {

        int n = times.length;

        /*
         * STEP 1: Convert input into Friend objects
         *
         * Why?
         * - We want to preserve original index (important for target friend)
         * - Improves readability vs raw arrays
         */
        Friend[] friends = new Friend[n];

        for (int i = 0; i < n; i++) {
            friends[i] = new Friend(times[i][0], times[i][1], i);
        }

        /*
         * STEP 2: Sort friends by arrival time
         *
         * WHY?
         * - We simulate events in chronological order
         * - This is a classic "timeline / sweep line" pattern
         */
        Arrays.sort(friends, Comparator.comparingInt(Friend::arrival));

        /*
         * STEP 3: Min Heap for occupied chairs
         *
         * Heap ordered by leaveTime
         *
         * WHY?
         * - We need to know which chair becomes free earliest
         * - So we can release it before assigning new chairs
         */
        PriorityQueue<Occupied> occupied = new PriorityQueue<>(
                Comparator.comparingInt(Occupied::leaveTime)
        );

        /*
         * STEP 4: Min Heap for free chairs
         *
         * WHY?
         * - We must always assign the smallest available chair
         * - Min heap gives O(log n) access to smallest
         */
        PriorityQueue<Integer> freeChairs = new PriorityQueue<>();

        /*
         * This tracks the next unused chair number
         *
         * Example:
         * First time → chair 0
         * Next → chair 1, 2, 3 ...
         */
        int nextChair = 0;

        /*
         * STEP 5: Process each friend in order of arrival
         */
        for (Friend f : friends) {

            int arrival = f.arrival();
            int leave = f.leave();
            int index = f.index();

            /*
             * STEP 5A: Free chairs before assigning
             *
             * VERY IMPORTANT EDGE CASE:
             * If someone leaves at same time another arrives,
             * that chair SHOULD be available.
             *
             * Hence: <= (NOT <)
             */
            while (!occupied.isEmpty() && occupied.peek().leaveTime() <= arrival) {
                Occupied freed = occupied.poll();
                freeChairs.offer(freed.chairNumber());
            }

            /*
             * STEP 5B: Assign chair
             *
             * Case 1: Some chairs are free → take smallest
             * Case 2: No free chairs → create new one
             */
            int assignedChair;

            if (!freeChairs.isEmpty()) {
                assignedChair = freeChairs.poll();
            } else {
                assignedChair = nextChair++;
            }

            /*
             * STEP 5C: If this is target friend → return immediately
             *
             * WHY?
             * - We don't need full simulation
             * - Early exit saves time
             */
            if (index == targetFriend) {
                return assignedChair;
            }

            /*
             * STEP 5D: Mark this chair as occupied
             */
            occupied.offer(new Occupied(leave, assignedChair));
        }

        // Should never reach here
        return -1;
    }
}
