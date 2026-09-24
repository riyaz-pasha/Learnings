import java.util.*;

/*
 * ============================================================================
 * LEETCODE 1094 — CAR POOLING
 * MOCK GOOGLE ONSITE INTERVIEW TRANSCRIPT
 * ============================================================================
 * Format: Java 21+ single-file source (compatible with `java -ea File.java`
 * or `javac File.java && java -ea File`).
 * ============================================================================
 */

class CarPooling {

    /*
     * ========================================================================
     * SECTION 1 — RESTATE THE PROBLEM
     * ========================================================================
     * In my own words: I have a car that only moves eastward on a number
     * line (no U-turns, no backtracking). I'm given a list of "trips" —
     * each trip is a group of passengers that boards at some kilometer
     * marker `from` and exits at a later marker `to`. Multiple trips can
     * overlap in time/space (e.g., one group boards while another is
     * already in the car). I need to determine if there is ANY point along
     * the route where the number of passengers simultaneously in the car
     * exceeds `capacity`. If capacity is never exceeded, return true;
     * otherwise return false.
     *
     * Inputs:
     *   - int capacity: max simultaneous passengers, 1 <= capacity <= 10^5
     *   - int[][] trips: trips[i] = [numPassengers, from, to]
     *       1 <= trips.length <= 1000
     *       1 <= numPassengers <= 100
     *       0 <= from < to <= 1000
     *
     * Output:
     *   - boolean: true if every trip can be served without ever exceeding
     *     capacity, false otherwise.
     *
     * Key assumption to confirm: a passenger dropped off AT kilometer X and
     * a passenger picked up AT kilometer X are NOT simultaneously in the
     * car at that exact point — i.e., the interval is a half-open range
     * [from, to). This is the standard convention for this problem and
     * matters for boundary/tie-breaking cases (Section 3).
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 2 — CLARIFYING QUESTIONS (asked to interviewer, with assumed
     * answers so I can proceed without blocking on a response)
     * ========================================================================
     * 1. Q: Is the interval [from, to] inclusive on both ends, or is it
     *      half-open [from, to)? Does a drop-off at km X free up a seat
     *      that a pickup at km X can immediately use?
     *    A (assumed): Half-open [from, to). Drop-offs at a location are
     *      processed before pickups at that same location. This matches
     *      LeetCode's official test cases.
     *
     * 2. Q: Can `trips` be empty?
     *    A (assumed): Constraints say length >= 1, so no need to
     *      special-case empty input, but I'll handle it gracefully anyway
     *      (defensive coding — returns true trivially).
     *
     * 3. Q: Can numPassengers be 0 or negative?
     *    A (assumed): No — constraint guarantees 1 <= numPassengers <= 100.
     *
     * 4. Q: Can `from` equal `to` (zero-length trip)?
     *    A (assumed): No — constraint guarantees from < to strictly.
     *
     * 5. Q: Is `trips` sorted in any way (by pickup location, e.g.)?
     *    A (assumed): No, assume arbitrary/unsorted order — I must not
     *      rely on input ordering.
     *
     * 6. Q: What's the realistic upper bound on `to`? Is it always small
     *      (<=1000) as stated, or should my solution generalize to large
     *      or sparse coordinates (e.g., up to 10^9)?
     *    A (assumed): Per constraints, to <= 1000, so a fixed-size bucket
     *      array is safe and optimal. I'll also mention how I'd generalize
     *      (coordinate compression / TreeMap) if that bound were relaxed,
     *      since interviewers often probe this as a follow-up.
     *
     * 7. Q: Is this a one-shot batch query (all trips known up front), or
     *      do trips arrive online / need to support future insertions and
     *      re-validation (streaming)?
     *    A (assumed): Batch — all trips are known up front. I'll note the
     *      online variant as a follow-up.
     *
     * 8. Q: Do I need to identify WHICH trip(s) cause the overflow, or
     *      just a boolean answer?
     *    A (assumed): Just the boolean, per the stated return type. I'll
     *      mention how to extend for diagnostics if asked.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 3 — EXAMPLES & EDGE CASES
     * ========================================================================
     * Example 1 (normal case):
     *   trips = [[2,1,5],[3,3,7]], capacity = 4
     *   - [0,1): 0 passengers
     *   - [1,3): trip A active -> 2 passengers
     *   - [3,5): trip A + trip B active -> 2 + 3 = 5 passengers > 4
     *   => FALSE (capacity exceeded during [3,5))
     *
     * Example 2 (normal case, fits exactly):
     *   trips = [[2,1,5],[3,5,7]], capacity = 5
     *   - [1,5): 2 passengers
     *   - [5,7): trip A has ended (drop-off AT 5), trip B starts AT 5 -> 3
     *   Max simultaneous = 3 <= 5 => TRUE
     *
     * Example 3 (boundary / tie-breaking case — tests half-open interval
     * convention explicitly):
     *   trips = [[3,0,3],[4,3,6]], capacity = 4
     *   - Trip A occupies [0,3): 3 passengers.
     *   - Trip B occupies [3,6): 4 passengers.
     *   - At km=3, trip A has JUST dropped off (exits the car) and trip B
     *     JUST boards. They are never simultaneously counted at km=3.
     *   - Max simultaneous = max(3, 4) = 4 <= 4 => TRUE
     *   If the interval were treated as inclusive on `to`, this would
     *   incorrectly yield 3+4=7 at km=3 and return FALSE — this is exactly
     *   why the half-open convention from Section 1/2 matters.
     *
     * Edge case (single trip, capacity exactly matches):
     *   trips = [[100,0,1000]], capacity = 100 => TRUE (exactly fits)
     *   trips = [[100,0,1000]], capacity = 99  => FALSE (off by one)
     *
     * Edge case (many disjoint trips, never overlapping):
     *   trips = [[50,0,10],[50,10,20],[50,20,30]], capacity = 50 => TRUE
     *   (sequential, never overlapping thanks to half-open convention)
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 4/5/6 — PARADIGM SURVEY (paradigms considered, including
     * those explicitly ruled out)
     * ========================================================================
     * APPLICABLE (implemented below):
     *   - Brute force / simulation        -> Approach 1
     *   - Sorting-based (event scan)      -> Approach 2
     *   - Hashing/bucket-counting         -> Approach 3 (difference array;
     *                                        this IS the bucket/counting
     *                                        paradigm applied over the
     *                                        bounded coordinate range)
     *   - Two pointer                     -> Approach 2 (sorted pickups vs
     *                                        sorted drop-offs, merged with
     *                                        two pointers)
     *   - Heap / priority queue           -> Approach 4
     *   - Advanced structure (TreeMap /
     *     ordered map, generalizes to
     *     segment tree territory)         -> Approach 5
     *
     * RULED OUT (with justification):
     *   - Divide & Conquer: No natural recursive decomposition — the
     *     "max simultaneous passengers" signal is a global running sum
     *     over a timeline, not a property that splits cleanly and merges
     *     via a comparator (unlike, say, closest-pair). Would add
     *     complexity without any complexity-class benefit.
     *   - Dynamic Programming: There's no optimal-substructure / overlapping
     *     subproblems relationship to exploit — we're not optimizing a
     *     choice, we're aggregating fixed, non-optional deltas over a
     *     timeline. DP would be strictly more machinery for no gain.
     *   - Greedy (as a distinct paradigm): There's no sequential
     *     "choice" to make greedily — every trip is mandatory and
     *     unconditional, so there's nothing to greedily accept/reject.
     *     (Approach 2's sort-and-scan is really the sorting paradigm, not
     *     greedy decision-making.)
     *   - Tree / Graph traversal: No graph structure is implied by the
     *     problem — locations are points on a line, not nodes with edges.
     *   - Trie: No string/prefix structure exists in this problem.
     *   - Segment Tree: Could be used to support range-add / range-max
     *     with point updates, but for a single batch query over a bounded
     *     range [0, 1000], it is strictly more code and the same
     *     asymptotic complexity as the difference array (Approach 3).
     *     I'd only reach for it if this were an ONLINE problem requiring
     *     interleaved trip insertions and capacity checks (see Follow-Up
     *     Questions, Section 12).
     *   - Monotonic stack / deque: No "next greater/smaller element" or
     *     window-extremum structure here — this isn't that shape of
     *     problem.
     *   - Binary search: Doesn't apply directly to finding the answer
     *     itself (the answer is a boolean, not a monotonic threshold to
     *     search over) — though a peak-finding variant of Approach 2 could
     *     binary search for the FIRST violating index if that were asked
     *     as a follow-up.
     * ========================================================================
     */

    /*
     * ========================================================================
     * APPROACH 1: Brute Force / Direct Simulation
     * ------------------------------------------------------------------------
     * Core idea: Maintain an array `occupancy` indexed by kilometer marker
     * (0..1000). For every trip, walk every kilometer in [from, to) and add
     * numPassengers to that slot. Then scan the array for any slot that
     * exceeds capacity.
     *
     * Paradigm: Direct simulation / brute force.
     *
     * Time Complexity: O(n * maxDistance) where n = trips.length and
     *   maxDistance = 1000 (bound on `to`). Each trip can touch up to 1000
     *   array slots, so worst case ~ 1000 * 1000 = 10^6 operations. This is
     *   the naive approach specifically because it re-touches every
     *   kilometer of every trip's range individually instead of using
     *   endpoint deltas.
     * Space Complexity: O(maxDistance) for the occupancy array.
     *
     * Pros: Trivial to reason about and verify correct; great as a
     *   stress-test oracle.
     * Cons: Wasteful — redundantly re-adds the same passenger count to
     *   every km in a trip's range instead of just marking the two
     *   endpoints. Does not scale if `to` were allowed to be large
     *   (e.g., 10^9) or if trip ranges were long.
     *
     * When to use: Never in production / interview final answer — but
     * exactly the right first thing to say out loud to show you understand
     * the problem, and to use as a correctness oracle for testing.
     * ========================================================================
     */
    static boolean bruteForce(int[][] trips, int capacity) {
        if (trips == null || trips.length == 0) return true;

        final int MAX_LOCATION = 1001; // locations are 0..1000 inclusive
        int[] occupancy = new int[MAX_LOCATION];

        for (int[] trip : trips) {
            int numPassengers = trip[0];
            int from = trip[1];
            int to = trip[2];
            // Naively touch every kilometer in [from, to) — this is the
            // "wasteful" part that later approaches optimize away.
            for (int km = from; km < to; km++) {
                occupancy[km] += numPassengers;
                if (occupancy[km] > capacity) {
                    return false; // early exit once violated
                }
            }
        }
        return true;
    }

    /*
     * ========================================================================
     * APPROACH 2: Sorting Events + Two-Pointer Sweep
     * ------------------------------------------------------------------------
     * Core idea: Split each trip into two "events": a pickup event at
     * `from` (+numPassengers) and a drop-off event at `to`
     * (-numPassengers). Sort pickups by location and drop-offs by location
     * separately, then merge-scan both sorted lists with two pointers in
     * increasing order of location, applying drop-offs before pickups when
     * locations tie (to honor the half-open interval convention from
     * Section 3). Track a running passenger total and its max.
     *
     * Paradigm: Sorting + two-pointer merge (classic "interval sweep").
     *
     * Time Complexity: O(n log n) for sorting the pickup and drop-off
     *   arrays; the merge scan itself is O(n). Overall O(n log n).
     * Space Complexity: O(n) for the two sorted event arrays.
     *
     * Pros: Does not depend on the coordinate range being small — works
     *   even if `to` could be up to 10^9, since we only ever look at the
     *   n*2 actual event points, not every integer km. Very general.
     * Cons: More code than the bucket/difference-array approach; sorting
     *   overhead (n log n) is asymptotically worse than the O(n + maxLoc)
     *   bucket approach GIVEN that maxLoc is small here.
     *
     * When to use: This is the approach I'd reach for if the interviewer
     * relaxed the constraint on `to` to be very large or non-integer —
     * i.e., when the coordinate range can't be used as an array size.
     * ========================================================================
     */
    static boolean sortingTwoPointer(int[][] trips, int capacity) {
        if (trips == null || trips.length == 0) return true;

        int tripCount = trips.length;
        int[] pickups = new int[tripCount];   // 'from' locations
        int[] dropoffs = new int[tripCount];  // 'to' locations
        // Parallel arrays keyed by the SAME sort order as pickups/dropoffs
        // would be awkward, so instead store passenger deltas alongside
        // each location using a small helper array-of-arrays for clarity.
        int[][] pickupEvents = new int[tripCount][2];  // {location, passengers}
        int[][] dropoffEvents = new int[tripCount][2]; // {location, passengers}

        for (int i = 0; i < tripCount; i++) {
            int numPassengers = trips[i][0];
            int from = trips[i][1];
            int to = trips[i][2];
            pickupEvents[i][0] = from;
            pickupEvents[i][1] = numPassengers;
            dropoffEvents[i][0] = to;
            dropoffEvents[i][1] = numPassengers;
        }

        // Sort both event lists by location.
        Arrays.sort(pickupEvents, Comparator.comparingInt(event -> event[0]));
        Arrays.sort(dropoffEvents, Comparator.comparingInt(event -> event[0]));

        int pickupPointer = 0;
        int dropoffPointer = 0;
        int currentPassengers = 0;

        while (pickupPointer < tripCount) {
            int nextPickupLocation = pickupEvents[pickupPointer][0];
            int nextDropoffLocation = dropoffEvents[dropoffPointer][0];

            // Process drop-offs before pickups at the same location so
            // that a seat freed at location X is available for a pickup
            // AT location X (half-open interval semantics).
            if (nextDropoffLocation <= nextPickupLocation) {
                currentPassengers -= dropoffEvents[dropoffPointer][1];
                dropoffPointer++;
            } else {
                currentPassengers += pickupEvents[pickupPointer][1];
                pickupPointer++;
                if (currentPassengers > capacity) {
                    return false;
                }
            }
        }
        return true; // all pickups processed without violating capacity
    }

    /*
     * ========================================================================
     * APPROACH 3: Difference Array / Bucket Counting  (RECOMMENDED —
     * see Section 7 for justification)
     * ------------------------------------------------------------------------
     * Core idea: Because `to` is bounded by a small constant (<=1000), we
     * can allocate a fixed-size delta array indexed by location. For each
     * trip, instead of touching every km in [from, to) (Approach 1's
     * waste), we only touch the two ENDPOINTS: delta[from] += passengers
     * and delta[to] -= passengers. A single left-to-right prefix-sum pass
     * then reconstructs the running occupancy at every location, and we
     * track its maximum against capacity.
     *
     * Paradigm: Difference array (a specialized form of bucket/counting
     * technique) — O(1) range-update, O(maxLoc) single reconstruction pass.
     *
     * Time Complexity: O(n + maxLocation). Each trip contributes O(1) work
     *   (two array writes), and the final prefix-sum sweep is O(maxLocation)
     *   = O(1001), a constant here. This is optimal given the bounded
     *   coordinate range.
     * Space Complexity: O(maxLocation) for the delta array — also
     *   effectively O(1) since maxLocation is a fixed constant (1001).
     *
     * Pros: Simplest possible optimal solution given the constraints;
     *   minimal code, no sorting, no comparator logic, easy to get right
     *   under interview time pressure; fewer opportunities for
     *   off-by-one bugs than the two-pointer merge.
     * Cons: Relies on the coordinate range being small and known in
     *   advance (bounded array size). Does NOT generalize if `to` could be
     *   arbitrarily large or non-integer (would need Approach 5 instead).
     *
     * When to use: Exactly this problem, as constrained (to <= 1000) — this
     * is the approach I'd code as my final answer.
     * ========================================================================
     */
    static boolean differenceArray(int[][] trips, int capacity) {
        if (trips == null || trips.length == 0) return true;

        final int MAX_LOCATION = 1001; // locations range over [0, 1000]
        int[] passengerDelta = new int[MAX_LOCATION + 1]; // +1 to safely
        // write a -passengers delta at `to` even when to == MAX_LOCATION-1
        // without a separate bounds check.

        for (int[] trip : trips) {
            int numPassengers = trip[0];
            int from = trip[1];
            int to = trip[2];
            passengerDelta[from] += numPassengers; // passengers board
            passengerDelta[to] -= numPassengers;   // passengers alight
        }

        int currentPassengers = 0;
        for (int location = 0; location <= MAX_LOCATION; location++) {
            currentPassengers += passengerDelta[location];
            if (currentPassengers > capacity) {
                return false;
            }
        }
        return true;
    }

    /*
     * ========================================================================
     * APPROACH 4: Heap / Priority Queue (ongoing-trip tracking)
     * ------------------------------------------------------------------------
     * Core idea: Sort trips by `from` (pickup location). Maintain a
     * min-heap of currently "active" trips' drop-off locations. Sweep
     * through trips in pickup order; before admitting a new trip, pop all
     * heap entries whose drop-off location is <= the new trip's pickup
     * location (they've already exited). Track a running passenger count
     * that increases on each admitted trip and decreases whenever a heap
     * entry is popped. This mirrors Approach 2's logic but uses a heap
     * instead of a second sorted array to manage "who's currently in the
     * car" ordered by exit time.
     *
     * Paradigm: Heap / priority queue (classic "meeting rooms" / interval
     * scheduling pattern).
     *
     * Time Complexity: O(n log n) — sorting trips by `from` is O(n log n),
     *   and each trip is pushed/popped from the heap at most once, each
     *   heap operation O(log n).
     * Space Complexity: O(n) for the heap and sorted trip references.
     *
     * Pros: Doesn't require the coordinate range to be bounded. Naturally
     *   extends to "how many passengers are in the car at query time T?"
     *   style follow-ups, and to online/streaming variants where trips are
     *   added incrementally (push new trip, evict stale heap entries).
     * Cons: Heap operations carry a higher constant factor than a plain
     *   array scan; more moving parts (comparator, heap object overhead)
     *   than Approach 3 for no asymptotic benefit given this problem's
     *   bounded input size.
     *
     * When to use: If asked to support an ONLINE variant (trips arriving
     * over time, need capacity check after each insertion) or if we only
     * need to answer "what is occupancy at a specific instant," a heap of
     * active intervals is a very natural, extensible structure.
     * ========================================================================
     */
    static boolean heapBased(int[][] trips, int capacity) {
        if (trips == null || trips.length == 0) return true;

        int[][] tripsSortedByPickup = trips.clone();
        Arrays.sort(tripsSortedByPickup, Comparator.comparingInt(trip -> trip[1]));

        // Min-heap of {dropoffLocation, numPassengers} ordered by
        // dropoffLocation, representing trips currently "in the car".
        PriorityQueue<int[]> activeTrips =
                new PriorityQueue<>(Comparator.comparingInt(entry -> entry[0]));

        int currentPassengers = 0;

        for (int[] trip : tripsSortedByPickup) {
            int numPassengers = trip[0];
            int from = trip[1];
            int to = trip[2];

            // Evict every active trip that has already dropped off its
            // passengers by the time this trip's pickup occurs.
            while (!activeTrips.isEmpty() && activeTrips.peek()[0] <= from) {
                int[] finishedTrip = activeTrips.poll();
                currentPassengers -= finishedTrip[1];
            }

            currentPassengers += numPassengers;
            if (currentPassengers > capacity) {
                return false;
            }
            activeTrips.offer(new int[]{to, numPassengers});
        }
        return true;
    }

    /*
     * ========================================================================
     * APPROACH 5: TreeMap-Based Sweep (generalized / unbounded coordinates)
     * ------------------------------------------------------------------------
     * Core idea: Same delta-accumulation idea as Approach 3 (difference
     * array), but instead of a fixed-size array indexed by location, use a
     * TreeMap<Integer, Integer> keyed by location, storing only the
     * locations that actually appear as an endpoint (sparse
     * representation). Iterating a TreeMap in key order gives us the
     * sorted locations for free, so we do a single pass over
     * entrySet() accumulating the running passenger delta.
     *
     * Paradigm: Ordered map (self-balancing BST) — this is the "generalize
     * to a sparse/segment-tree-like structure" answer for when the
     * coordinate range is NOT small.
     *
     * Time Complexity: O(n log n) — up to 2n insertions into the TreeMap,
     *   each O(log n), followed by an O(n) in-order traversal.
     * Space Complexity: O(n) — only real endpoints are stored, versus
     *   Approach 3's O(maxLocation).
     *
     * Pros: Works even if `to` were up to 10^9 or arbitrary (e.g.,
     *   floating point / GPS coordinates after discretization) — space
     *   scales with the number of TRIPS, not the coordinate range. This is
     *   the most "production-realistic" generalization of the bucket idea.
     * Cons: Higher constant factor than a raw array (tree node overhead,
     *   pointer chasing, boxing of Integer keys/values); more complex to
     *   write correctly under time pressure than Approach 3.
     *
     * When to use: If the interviewer relaxes `to <= 1000` to something
     * unbounded, this is my go-to — it keeps the same "accumulate deltas,
     * sweep in order" idea as the optimal solution but drops the
     * dependency on a small coordinate range.
     * ========================================================================
     */
    static boolean treeMapBased(int[][] trips, int capacity) {
        if (trips == null || trips.length == 0) return true;

        TreeMap<Integer, Integer> passengerDeltaByLocation = new TreeMap<>();

        for (int[] trip : trips) {
            int numPassengers = trip[0];
            int from = trip[1];
            int to = trip[2];
            passengerDeltaByLocation.merge(from, numPassengers, Integer::sum);
            passengerDeltaByLocation.merge(to, -numPassengers, Integer::sum);
        }

        int currentPassengers = 0;
        for (int delta : passengerDeltaByLocation.values()) {
            currentPassengers += delta;
            if (currentPassengers > capacity) {
                return false;
            }
        }
        return true;
    }

    /*
     * ========================================================================
     * SECTION 7 — APPROACHES COMPARISON TABLE
     * ========================================================================
     * Approach                  | Time            | Space         | Best For                                   | Limitations
     * --------------------------|-----------------|---------------|--------------------------------------------|--------------------------------------------
     * 1. Brute Force            | O(n * maxLoc)   | O(maxLoc)     | Correctness oracle / warm-up explanation    | Wasteful; doesn't scale if range grows
     * 2. Sorting + Two Pointer  | O(n log n)      | O(n)          | Large/unbounded coordinate ranges           | More code than needed given small bound here
     * 3. Difference Array       | O(n + maxLoc)   | O(maxLoc)     | THIS problem (to <= 1000) — optimal & simple| Needs a known small bound on coordinates
     * 4. Heap / Priority Queue  | O(n log n)      | O(n)          | Online/streaming trips, point-in-time query | Higher constant factor; unnecessary complexity here
     * 5. TreeMap Sweep          | O(n log n)      | O(n)          | Unbounded/sparse coordinates, production use| Slower constants than array; boxing overhead
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 8 — RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     * I would present Approach 3 (Difference Array / Bucket Counting) as my
     * final answer, for these reasons:
     *
     * 1. Optimality given constraints: The problem explicitly bounds
     *    `to <= 1000`, which is a strong signal the interviewer wants a
     *    solution that exploits that bound. O(n + maxLocation) is the best
     *    achievable complexity here, and maxLocation is a small constant.
     * 2. Coding speed & low bug surface: It requires no sorting, no
     *    comparators, no heap/tree bookkeeping — just two array writes per
     *    trip and one linear sweep. Under interview time pressure, fewer
     *    moving parts means fewer off-by-one mistakes.
     * 3. Clarity for the interviewer: The "delta array + prefix sum" idea
     *    is a well-known, easily explained pattern (closely related to
     *    range-update problems), so it communicates strong fundamentals
     *    without over-engineering.
     * 4. Demonstrates range: I'd verbally walk through Approach 1 first
     *    (to show I understand the problem literally), then present
     *    Approach 3 as the optimized solution, and proactively mention
     *    Approaches 2/4/5 as what I'd use if the coordinate bound were
     *    relaxed — this demonstrates breadth without over-coding in the
     *    room.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 9 — DEEP DIVE: OPTIMAL SOLUTION (production-quality)
     * ========================================================================
     */

    /**
     * Determines whether all given trips can be completed without the
     * number of simultaneous passengers ever exceeding {@code capacity}.
     *
     * <p>Algorithm: difference-array sweep. Because every trip's endpoints
     * lie within the fixed range [0, 1000] (per problem constraints), we
     * can represent passenger count CHANGES at each kilometer marker in a
     * fixed-size array, then reconstruct the actual running occupancy with
     * a single left-to-right prefix sum. This avoids ever touching more
     * than O(1) array cells per trip, unlike a naive simulation that walks
     * every kilometer of every trip's range.
     *
     * @param trips    array of [numPassengers, from, to] triples; from < to
     * @param capacity maximum simultaneous passengers the car can hold
     * @return true if capacity is never exceeded, false otherwise
     * @throws IllegalArgumentException if a trip's fields violate the
     *                                   documented constraints
     */
    static boolean carPooling(int[][] trips, int capacity) {
        // Defensive input validation — an interviewer will often probe
        // whether you validate assumptions rather than trusting input
        // blindly, even when constraints are "guaranteed."
        if (trips == null) {
            throw new IllegalArgumentException("trips must not be null");
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (trips.length == 0) {
            return true; // vacuously true: no trips to violate capacity
        }

        // Upper bound on `to` per problem constraints (0 <= from < to <= 1000).
        // We size the delta array to MAX_LOCATION + 1 so that a drop-off
        // delta write at to == MAX_LOCATION never needs a special-case
        // bounds check.
        final int MAX_LOCATION = 1000;
        int[] passengerDelta = new int[MAX_LOCATION + 2];

        for (int[] trip : trips) {
            if (trip.length != 3) {
                throw new IllegalArgumentException("each trip must have exactly 3 fields");
            }
            int numPassengers = trip[0];
            int from = trip[1];
            int to = trip[2];

            if (numPassengers < 1 || from < 0 || to > MAX_LOCATION || from >= to) {
                throw new IllegalArgumentException(
                        "invalid trip: " + Arrays.toString(trip));
            }

            // Range-update in O(1): mark passengers boarding at `from`
            // and the corresponding "give back the seats" at `to`. The
            // half-open [from, to) convention is realized naturally here
            // because the -numPassengers delta is applied exactly AT
            // `to`, so when the prefix-sum sweep below reaches index
            // `to`, those passengers are already removed before any
            // pickup delta AT the same index `to` is added — both deltas
            // are combined at the same array slot and applied together
            // in the single sweep pass, which correctly reflects
            // "drop-offs free seats for same-location pickups."
            passengerDelta[from] += numPassengers;
            passengerDelta[to] -= numPassengers;
        }

        // Single linear reconstruction pass: running sum = current
        // occupancy at each kilometer marker.
        int currentPassengers = 0;
        for (int location = 0; location <= MAX_LOCATION; location++) {
            currentPassengers += passengerDelta[location];
            if (currentPassengers > capacity) {
                return false; // capacity violated somewhere in [0, location]
            }
        }
        return true;
    }

    /*
     * ========================================================================
     * SECTION 10 — DRY RUN / TRACE
     * ========================================================================
     * Using Example 1 from Section 3: trips = [[2,1,5],[3,3,7]], capacity = 4
     *
     * Initial: passengerDelta = [0,0,0,0,0,0,0,0,...] (size 1002, all zero)
     *
     * Processing trip [2,1,5] (numPassengers=2, from=1, to=5):
     *   passengerDelta[1] += 2  -> passengerDelta[1] = 2
     *   passengerDelta[5] -= 2  -> passengerDelta[5] = -2
     *
     * Processing trip [3,3,7] (numPassengers=3, from=3, to=7):
     *   passengerDelta[3] += 3  -> passengerDelta[3] = 3
     *   passengerDelta[7] -= 3  -> passengerDelta[7] = -3
     *
     * Resulting sparse deltas: index 1 -> +2, index 3 -> +3,
     *                          index 5 -> -2, index 7 -> -3  (rest 0)
     *
     * Prefix-sum sweep (currentPassengers running total):
     *   location=0: delta=0,  currentPassengers=0            (0 <= 4, ok)
     *   location=1: delta=+2, currentPassengers=2             (2 <= 4, ok)
     *   location=2: delta=0,  currentPassengers=2             (2 <= 4, ok)
     *   location=3: delta=+3, currentPassengers=5             (5 >  4, VIOLATION)
     *   -> return false immediately at location=3
     *
     * Final result: false, matching the expected answer from Section 3
     * (capacity is exceeded during [3,5) where both trips overlap:
     * 2 + 3 = 5 > 4).
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 11 — CLOSING SUMMARY
     * ========================================================================
     * - All five approaches are functionally equivalent (verified below via
     *   randomized stress testing against the brute-force oracle); they
     *   differ only in complexity, generality, and code complexity.
     * - Approach 3 (difference array) is optimal specifically BECAUSE this
     *   problem bounds coordinates to [0, 1000]. If that constraint were
     *   relaxed, Approaches 2, 4, or 5 (all O(n log n), coordinate-range
     *   independent) become the right choice instead.
     * - Known limitation of my final solution (`carPooling`): it assumes
     *   the stated constraint `to <= 1000` holds; I added explicit
     *   validation that throws IllegalArgumentException if violated,
     *   rather than silently producing wrong answers or an
     *   ArrayIndexOutOfBoundsException.
     * - Assumption baked into all approaches: intervals are half-open
     *   [from, to) — a drop-off and a pickup at the same location do NOT
     *   count as simultaneous occupancy.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 12 — FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     * 1. "What if `to` could be up to 10^9?" -> Switch to Approach 2, 4, or
     *    5 (coordinate-range-independent, O(n log n)).
     * 2. "What if trips arrive one at a time (streaming), and you must
     *    answer 'still feasible?' after each insertion?" -> Approach 4's
     *    heap-of-active-trips generalizes naturally: maintain running
     *    occupancy incrementally rather than re-sweeping from scratch.
     * 3. "Can you report the exact location(s) where capacity is first
     *    violated, not just true/false?" -> Trivial extension: instead of
     *    returning false immediately, record `location` and (optionally)
     *    which trips are active there (requires tracking trip identities,
     *    not just deltas).
     * 4. "What if capacity itself can change over time (e.g., some
     *    passengers get off at intermediate 'checkpoints' unrelated to
     *    trips)?" -> Model checkpoints as additional zero-passenger
     *    'capacity change' events merged into the same sweep.
     * 5. "How would you parallelize this for billions of trips?" -> Split
     *    trips into shards, compute partial delta arrays (or partial
     *    TreeMaps) per shard, then merge deltas by location (map-reduce
     *    style) before the final prefix-sum sweep.
     * 6. "What if multiple cars/capacities need to be checked against the
     *    same trip set?" -> Precompute the max simultaneous occupancy once
     *    (O(n + maxLoc)), then answer each capacity query in O(1) by
     *    comparing against that precomputed maximum.
     * ========================================================================
     */

    /*
     * ========================================================================
     * SECTION 13 — WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     * 1. Boundary/interval convention bugs: Treating [from, to] as fully
     *    inclusive (adding the -passengers delta at to+1 instead of to, or
     *    checking capacity using >= instead of >) silently breaks the
     *    "drop-off frees a seat for a same-location pickup" case (Example 3
     *    in Section 3). This is the single most common bug on this
     *    problem.
     * 2. Off-by-one on array sizing: Since `to` can equal 1000, the delta
     *    array must have size >= 1001 (or 1002, to safely write a delta AT
     *    index 1000 without special-casing); sizing it to exactly 1000
     *    causes an ArrayIndexOutOfBoundsException.
     * 3. Forgetting the running-sum reset / misunderstanding "difference
     *    array": Some candidates try to track max occupancy per trip
     *    independently instead of realizing deltas must be SWEPT once,
     *    left to right, accumulating a running total — that accumulation
     *    is what makes the O(1)-per-trip update correct.
     * 4. Not validating early exit correctly: Returning false immediately
     *    the first time currentPassengers > capacity is correct and an
     *    easy optimization, but candidates sometimes finish the whole
     *    sweep and then check "was it ever violated," using an extra
     *    boolean flag unnecessarily — not wrong, just less clean, and a
     *    good moment to demonstrate you're thinking about early exits.
     * ========================================================================
     */

    /*
     * ========================================================================
     * BONUS — NAMED ASSERTIONS & RANDOMIZED STRESS TEST
     * (Established verification discipline: cross-validate every approach
     * against the brute-force oracle via named assertions plus randomized
     * trials with a fixed seed, run with `java -ea CarPooling.java`.)
     * ========================================================================
     */
    public static void main(String[] args) {
        runNamedAssertions();
        runRandomizedStressTest(3000, 42L);
        System.out.println("All named assertions and stress tests passed.");
    }

    private static void runNamedAssertions() {
        // --- Example 1: normal overlap causing violation ---
        int[][] example1Trips = {{2, 1, 5}, {3, 3, 7}};
        assert !carPooling(example1Trips, 4) : "example1_shouldExceedCapacity";
        assert !bruteForce(example1Trips, 4) : "example1_bruteForce";
        assert !sortingTwoPointer(example1Trips, 4) : "example1_sortingTwoPointer";
        assert !heapBased(example1Trips, 4) : "example1_heapBased";
        assert !treeMapBased(example1Trips, 4) : "example1_treeMapBased";

        // --- Example 2: fits exactly at capacity ---
        int[][] example2Trips = {{2, 1, 5}, {3, 5, 7}};
        assert carPooling(example2Trips, 5) : "example2_fitsExactly";
        assert bruteForce(example2Trips, 5) : "example2_bruteForce";

        // --- Example 3: half-open boundary convention ---
        int[][] example3Trips = {{3, 0, 3}, {4, 3, 6}};
        assert carPooling(example3Trips, 4) : "example3_halfOpenBoundary_shouldFit";
        assert bruteForce(example3Trips, 4) : "example3_bruteForce_halfOpenBoundary";

        // --- Edge: single trip exactly matches capacity ---
        int[][] singleTripExact = {{100, 0, 1000}};
        assert carPooling(singleTripExact, 100) : "singleTrip_exactCapacity_shouldFit";
        assert !carPooling(singleTripExact, 99) : "singleTrip_offByOne_shouldFail";

        // --- Edge: sequential disjoint trips never overlap ---
        int[][] disjointTrips = {{50, 0, 10}, {50, 10, 20}, {50, 20, 30}};
        assert carPooling(disjointTrips, 50) : "disjointTrips_shouldFit";

        // --- Edge: empty trips list is vacuously true ---
        assert carPooling(new int[0][0], 1) : "emptyTrips_vacuouslyTrue";

        // --- Validation: invalid trip triggers IllegalArgumentException ---
        boolean threwOnInvalidTrip = false;
        try {
            carPooling(new int[][]{{5, 3, 3}}, 10); // from == to, invalid
        } catch (IllegalArgumentException expected) {
            threwOnInvalidTrip = true;
        }
        assert threwOnInvalidTrip : "invalidTrip_shouldThrow";
    }

    private static void runRandomizedStressTest(int trialCount, long seed) {
        Random random = new Random(seed);
        for (int trial = 0; trial < trialCount; trial++) {
            int tripCount = 1 + random.nextInt(15);
            int[][] trips = new int[tripCount][3];
            for (int i = 0; i < tripCount; i++) {
                int from = random.nextInt(50);
                int to = from + 1 + random.nextInt(50); // guarantee from < to
                int numPassengers = 1 + random.nextInt(20);
                trips[i] = new int[]{numPassengers, from, to};
            }
            int capacity = 1 + random.nextInt(60);

            boolean expected = bruteForce(trips, capacity);
            boolean actualDifferenceArray = carPooling(trips, capacity);
            boolean actualSortingTwoPointer = sortingTwoPointer(trips, capacity);
            boolean actualHeapBased = heapBased(trips, capacity);
            boolean actualTreeMapBased = treeMapBased(trips, capacity);

            assert expected == actualDifferenceArray
                    : "stressTest_mismatch_differenceArray_trial" + trial
                    + " trips=" + Arrays.deepToString(trips) + " capacity=" + capacity;
            assert expected == actualSortingTwoPointer
                    : "stressTest_mismatch_sortingTwoPointer_trial" + trial;
            assert expected == actualHeapBased
                    : "stressTest_mismatch_heapBased_trial" + trial;
            assert expected == actualTreeMapBased
                    : "stressTest_mismatch_treeMapBased_trial" + trial;
        }
    }
}

/**
 * Car Pooling Problem
 *
 * PROBLEM UNDERSTANDING:
 * ----------------------
 * We are given:
 * - trips[i] = [numPassengers, from, to]
 * - capacity of the car
 *
 * The car moves ONLY forward (eastward).
 *
 * At any point:
 *   passengers inside car <= capacity
 *
 * GOAL:
 * Return true if we can complete all trips without exceeding capacity.
 *
 *
 * ---------------------------------------------------------------
 * 🧠 INTERVIEW THINKING PROCESS:
 * ---------------------------------------------------------------
 *
 * Instead of thinking in terms of trips,
 * THINK IN TERMS OF EVENTS happening along a timeline.
 *
 * Each trip creates 2 events:
 *   + passengers at 'from'
 *   - passengers at 'to'
 *
 * Now the problem becomes:
 *   "Does cumulative sum ever exceed capacity?"
 *
 *
 * ---------------------------------------------------------------
 * ✅ SOLUTIONS INCLUDED:
 * ---------------------------------------------------------------
 *
 * 1. Difference Array (BEST / MOST OPTIMAL)
 * 2. Sweep Line (Event Sorting)
 * 3. Min Heap (Simulation)
 *
 *
 * ---------------------------------------------------------------
 */
class CarPoolingSolutions {

    /**
     * =============================================================
     * ✅ APPROACH 1: DIFFERENCE ARRAY (PREFIX SUM)
     * =============================================================
     *
     * 💡 IDEA:
     * Instead of simulating trips, we track how passenger count changes.
     *
     * For each trip:
     *   diff[from] += passengers
     *   diff[to]   -= passengers
     *
     * Then compute prefix sum to simulate journey.
     *
     *
     * 🧠 WHY THIS WORKS:
     * This is same as range addition technique.
     * We're marking "entry" and "exit" points.
     *
     *
     * ⏱ TIME COMPLEXITY:
     * O(N + 1000) ≈ O(N)
     *
     * 🧠 SPACE COMPLEXITY:
     * O(1000) → constant (given constraints)
     *
     *
     * ✅ BEST WHEN:
     * - Locations are bounded (0–1000 here)
     * - You want fastest solution
     */
    public static boolean carPoolingDiffArray(int[][] trips, int capacity) {

        // Max possible location is 1000
        int[] diff = new int[1001];

        // Step 1: Mark passenger changes
        for (int[] trip : trips) {
            int passengers = trip[0];
            int from = trip[1];
            int to = trip[2];

            diff[from] += passengers; // pickup
            diff[to] -= passengers;   // drop
        }

        // Step 2: Prefix sum → simulate journey
        int currentPassengers = 0;

        for (int i = 0; i <= 1000; i++) {
            currentPassengers += diff[i];

            // Check capacity
            if (currentPassengers > capacity) {
                return false;
            }
        }

        return true;
    }


    /**
     * =============================================================
     * ✅ APPROACH 2: SWEEP LINE (EVENT SORTING)
     * =============================================================
     *
     * 💡 IDEA:
     * Convert trips into events:
     *   (location, +passengers) → pickup
     *   (location, -passengers) → drop
     *
     * Then:
     *   1. Sort events by location
     *   2. If same location → drop FIRST (important!)
     *   3. Track running sum
     *
     *
     * 🧠 CRITICAL INSIGHT:
     * If pickup and drop happen at same location:
     *   → Drop must happen BEFORE pickup
     *   → Otherwise we falsely exceed capacity
     *
     *
     * ⏱ TIME COMPLEXITY:
     * O(N log N)
     *
     * 🧠 SPACE COMPLEXITY:
     * O(N)
     *
     *
     * ✅ BEST WHEN:
     * - Coordinates are large / unbounded
     * - Cannot use array indexing
     */
    public static boolean carPoolingSweepLine(int[][] trips, int capacity) {

        List<int[]> events = new ArrayList<>();

        // Step 1: Create events
        for (int[] trip : trips) {
            events.add(new int[]{trip[1], trip[0]});   // pickup
            events.add(new int[]{trip[2], -trip[0]});  // drop
        }

        // Step 2: Sort events
        // If same location → drop first (negative first)
        events.sort((a, b) -> {
            if (a[0] == b[0]) {
                return a[1] - b[1];
            }
            return a[0] - b[0];
        });

        // Step 3: Traverse events
        int current = 0;

        for (int[] event : events) {
            current += event[1];

            if (current > capacity) {
                return false;
            }
        }

        return true;
    }


    /**
     * =============================================================
     * ✅ APPROACH 3: MIN HEAP (SIMULATION)
     * =============================================================
     *
     * 💡 IDEA:
     * Simulate trips in order of pickup location.
     *
     * Use min heap based on drop location:
     *   - Remove passengers whose trip ended
     *   - Add new passengers
     *
     *
     * 🧠 HOW IT WORKS:
     * 1. Sort trips by start location
     * 2. Use min heap (based on end location)
     * 3. Remove completed trips
     * 4. Add current trip
     * 5. Check capacity
     *
     *
     * ⏱ TIME COMPLEXITY:
     * O(N log N)
     *
     * 🧠 SPACE COMPLEXITY:
     * O(N)
     *
     *
     * ✅ BEST WHEN:
     * - Real-time simulation needed
     * - Streaming data scenario
     */
    public static boolean carPoolingMinHeap(int[][] trips, int capacity) {

        // Step 1: Sort trips by start location
        Arrays.sort(trips, Comparator.comparingInt(a -> a[1]));

        // Min heap sorted by drop location
        PriorityQueue<int[]> pq =
                new PriorityQueue<>(Comparator.comparingInt(a -> a[2]));

        int currentPassengers = 0;

        for (int[] trip : trips) {
            int passengers = trip[0];
            int start = trip[1];
            int end = trip[2];

            // Step 2: Remove completed trips
            while (!pq.isEmpty() && pq.peek()[2] <= start) {
                currentPassengers -= pq.poll()[0];
            }

            // Step 3: Add current trip
            pq.offer(trip);
            currentPassengers += passengers;

            // Step 4: Check capacity
            if (currentPassengers > capacity) {
                return false;
            }
        }

        return true;
    }


    /**
     * =============================================================
     * 🧪 MAIN METHOD (TESTING)
     * =============================================================
     */
    public static void main(String[] args) {

        int[][] trips = {
                {2, 1, 5},
                {3, 3, 7}
        };

        int capacity = 4;

        System.out.println("Diff Array: " +
                carPoolingDiffArray(trips, capacity));

        System.out.println("Sweep Line: " +
                carPoolingSweepLine(trips, capacity));

        System.out.println("Min Heap: " +
                carPoolingMinHeap(trips, capacity));
    }
}
