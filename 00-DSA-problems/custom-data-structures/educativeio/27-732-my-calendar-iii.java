/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * You are given some events [startTime, endTime). A k-booking happens when k 
 * events have some non-empty intersection (time common to all k events).
 * 
 * Implement the MyCalendarThree class:
 * - MyCalendarThree() Initializes the object.
 * - int book(int startTime, int endTime) Returns an integer k representing the 
 *   largest integer such that there exists a k-booking in the calendar.
 * 
 * Constraints:
 * - 0 <= startTime < endTime <= 10^9
 * - At most 400 calls will be made to book.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Are the intervals fully inclusive, or half-open? 
 *    (Half-open [start, end), meaning an event ending at 20 and one starting at 20 do not overlap).
 * 2. Is the number of queries strictly bounded to 400?
 *    (Crucial constraint: Determines if we can use an O(N) sweep line per query, or if we are forced to build an O(log W) Dynamic Segment Tree).
 * 3. Can start and end times be exactly the same?
 *    (Constraints state startTime < endTime, saving us from handling zero-length phantom events).
 * 4. Can the same exact interval be booked multiple times?
 *    (Yes. Our data structure must track density/frequency, not just presence).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: Track the maximum density of overlapping intervals over a 
 *   massive continuous domain (up to 10^9), updating the maximum on every insertion.
 * 
 * --- Approach 1: The Brute Force Array (Time Slots) ---
 * 1. Intuition: If we want to know how many events overlap at time T, just keep a 
 *    tally for every possible time T.
 * 2. Try: `int[] timeline = new int[1000000000]`. 
 *    For every `book(start, end)`, run a loop `for (int i = start; i < end; i++) timeline[i]++`.
 *    The max value in the array is the answer.
 * 3. Why it works: Literally simulates the calendar minute by minute.
 * 4. Why it's too costly: Time and Space. An array of size 10^9 requires 4GB of RAM. 
 *    A single booking of `book(0, 10^9)` takes 1 billion operations, instantly causing 
 *    Memory Limit Exceeded and Time Limit Exceeded.
 * 5. Bottleneck: Representing continuous empty space takes just as much memory as active space.
 * 
 * Time Complexity: O(E) per query, where E is the duration of the event (up to 10^9).
 * Space Complexity: O(W) where W is the maximum possible time (10^9).
 * 
 * --- Approach 2: Sweep Line / Boundary Count (Optimal for N=400) ---
 * 1. Intuition: An overlap only changes at the exact moment an event starts or ends. 
 *    Everything between the boundaries is constant. We only need to record the "deltas" (changes).
 * 2. Try: Use a `TreeMap<Integer, Integer>`. 
 *    When an event starts at S, overlap increases: `map.put(S, +1)`. 
 *    When it ends at E, overlap decreases: `map.put(E, -1)`.
 *    To find the max overlap, iterate through the map in chronological order, keeping a running 
 *    sum of the deltas. The highest peak of this running sum is our k-booking!
 * 3. Why it works: We compress a 1 billion slot timeline into at most 800 boundary points.
 * 4. Which to write in a 45-min interview?
 *    Approach 2. For N=400, iterating over 800 elements takes less than a millisecond. 
 *    It is elegant, concise, and perfectly maps to the "Sweep Line" paradigm. 
 *    (Note: If N was 100,000, this O(N) iteration per query would TLE, forcing a Dynamic Segment Tree).
 * 
 * Time Complexity: O(N) per `book` call — where N is the number of boundaries (at most 800). We do an O(log N) insertion, followed by an O(N) iteration over the map values.
 * Space Complexity: O(N) — storing exactly two boundary points per event in the TreeMap.
 * 
 * --- Approach 3: Dynamic Segment Tree with Lazy Propagation (For N = 10^5) ---
 * 1. Intuition: If we couldn't afford O(N) per query, we'd need O(log W) updates.
 * 2. Try: A Segment Tree where nodes represent time ranges. `node.val` tracks the max overlap 
 *    in that range. We add nodes dynamically only when that specific time range is booked.
 * 3. Cost check: Overkill for N=400, highly prone to implementation bugs in an interview setting, 
 *    but the definitive answer if the interviewer scales up the constraints.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Back-to-back events: `book(10, 20)` and `book(20, 30)`. 
 *   At time 20, the map gets a `-1` and a `+1`. Because we iterate through chronologically 
 *   and sum them, they perfectly cancel out in the running tally, preventing a false overlap peak.
 * - Fully encapsulated events: `book(10, 50)` and `book(20, 30)`. 
 *   The running sum hits 1 at 10, hits 2 at 20, drops to 1 at 30, and drops to 0 at 50. Works flawlessly.
 * - Duplicate events: `book(10, 20)` twice. 
 *   Map value at 10 becomes `+2`. Map value at 20 becomes `-2`. Tally instantly reflects the overlap.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Events are just topographical elevation." 
 *   Imagine walking along the timeline. Every `startTime` is a step UP a mountain. 
 *   Every `endTime` is a step DOWN. The maximum k-booking is simply the highest 
 *   altitude you reach during the walk.
 * 
 * ASCII DRY RUN (Sweep Line)
 * Calls: book(10, 20), book(15, 25)
 * 
 * 1. book(10, 20):
 *    Map: { 10: +1,  20: -1 }
 *    Sweep: 
 *      @10: sum = 1 (Max = 1)
 *      @20: sum = 0
 *    Returns 1.
 * 
 * 2. book(15, 25):
 *    Map: { 10: +1,  15: +1,  20: -1,  25: -1 }
 *    Sweep:
 *      @10: sum = 1
 *      @15: sum = 2 (Max = 2)  <-- Peak overlap!
 *      @20: sum = 1
 *      @25: sum = 0
 *    Returns 2.
 * 
 * Pitfalls:
 * - Using a standard `HashMap`: HashMaps do not preserve key order. Sweeping line 
 *   relies entirely on evaluating time chronologically. You MUST use a `TreeMap`.
 * - Returning the value of a specific node: The max overlap isn't tied to the *last inserted* 
 *   event. Inserting an event at time 5 might increase a peak at time 20. We must check 
 *   the entire timeline.
 * 
 * Pattern Recognition: "Overlapping intervals + Density" -> Sweep Line with TreeMap.
 * 
 * Interview Script snippet:
 * "Since the time domain is massive (10^9), simulating it as an array is impossible. 
 * However, the number of operations is very small (400). This perfectly sets up a Sweep Line 
 * algorithm. I'll use a TreeMap to record the 'deltas' — adding 1 at the start of an event 
 * and subtracting 1 at the end. For every booking, I'll insert the endpoints in O(log N) time, 
 * then sweep chronologically through the map's values in O(N) time to find the highest running sum."
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: What if `book` is called 100,000 times instead of 400?
 * A1: Sweeping an O(N) map 100,000 times yields 10^10 operations, which will TLE. 
 *     We would have to transition to a Dynamic Segment Tree with Lazy Propagation. 
 *     The segment tree would manage the domain [0, 10^9]. When an interval is booked, 
 *     we dynamically instantiate nodes and lazily push updates down, bringing query 
 *     time to strict O(log W) where W is the maximum time constraint.
 */

import java.util.TreeMap;

public class MyCalendarThreeStudy {

    static class MyCalendarThree {
        
        // TreeMap automatically sorts keys (timestamps) in ascending chronological order.
        // Key = Timestamp, Value = Net change in concurrent events (Delta)
        private TreeMap<Integer, Integer> timeline;

        /**
         * Initializes the object.
         */
        public MyCalendarThree() {
            timeline = new TreeMap<>();
        }

        /**
         * Returns an integer k representing the largest k-booking in the calendar.
         * Time Complexity: O(N) per call, where N is total boundary points.
         * Space Complexity: O(N) overall to store boundaries.
         */
        public int book(int startTime, int endTime) {
            // STEP 1: Add the endpoints to the Sweep Line.
            // An event starting increases active overlaps by 1.
            timeline.put(startTime, timeline.getOrDefault(startTime, 0) + 1);
            
            // An event ending decreases active overlaps by 1.
            // Half-open interval [start, end) means it ends EXACTLY at 'endTime'.
            timeline.put(endTime, timeline.getOrDefault(endTime, 0) - 1);
            
            // STEP 2: Sweep across the chronological timeline to find the peak.
            int maxK = 0;
            int activeEvents = 0;
            
            // Since we only need to sum the deltas chronologically, we iterate over values().
            // TreeMap's values() returns them strictly ordered by their corresponding Keys.
            for (int delta : timeline.values()) {
                activeEvents += delta;
                
                // Track the highest "altitude" (max concurrent events) we hit during the sweep.
                if (activeEvents > maxK) {
                    maxK = activeEvents;
                }
            }
            
            return maxK;
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        MyCalendarThree myCalendarThree = new MyCalendarThree();
        
        // Return 1
        System.out.println("Booking [10, 20), Max Overlap: " + myCalendarThree.book(10, 20)); 
        
        // Return 1 (Disjoint from previous)
        System.out.println("Booking [50, 60), Max Overlap: " + myCalendarThree.book(50, 60)); 
        
        // Return 2 (Overlaps with [10, 20])
        System.out.println("Booking [10, 40), Max Overlap: " + myCalendarThree.book(10, 40)); 
        
        // Return 3 (Overlaps with [10, 20] and [10, 40])
        System.out.println("Booking [5, 15), Max Overlap: " + myCalendarThree.book(5, 15)); 
        
        // Return 3 (Adds density at start, but doesn't beat peak of 3)
        System.out.println("Booking [5, 10), Max Overlap: " + myCalendarThree.book(5, 10)); 
        
        // Return 3
        System.out.println("Booking [25, 55), Max Overlap: " + myCalendarThree.book(25, 55)); 
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: Sweep Line / Boundary Counting via TreeMap.
 * Key Observation: Instead of iterating over massive amounts of continuous time, 
 * we only care about the exact moments when a state changes (starts and ends). 
 * Sorting these boundary points chronologically allows us to calculate density 
 * at any time using a simple running sum.
 * What to Memorize: `map.put(S, map.getOrDefault(S, 0) + 1); map.put(E, map.getOrDefault(E, 0) - 1);` 
 * followed by a chronological iteration over `map.values()`.
 * Most Common Trap: Assuming the peak overlap must involve the newly inserted event. 
 * A newly inserted event might alter the landscape such that a peak occurs somewhere 
 * entirely different. You must sweep the whole map to be sure.
 * Mental Trigger: "Max Overlapping Intervals" -> "Sweep Line (+1 Start, -1 End)".
 * ============================================================================
 */

















































import java.util.TreeMap;

class MyCalendarThree {

    /*
     * CORE IDEA: SWEEP LINE
     *
     * Every booking [startTime, endTime) produces two changes:
     *
     *     startTime -> +1
     *     endTime   -> -1
     *
     * deltaByTime stores the NET change at each timestamp.
     *
     * Why TreeMap?
     * We must process timestamps in ascending order to calculate the
     * running number of active bookings. TreeMap maintains sorted keys.
     *
     * Why store deltas instead of bookings?
     * Our answer depends on how many bookings are active, not on their
     * individual identities. We can summarize all starts and ends at
     * each timestamp with one integer.
     */
    private final TreeMap<Integer, Integer> deltaByTime = new TreeMap<>();

    public int book(int startTime, int endTime) {

        /*
         * Add the new booking's two boundary events.
         *
         * merge() adds to the existing value when a timestamp already
         * exists. This is essential when multiple bookings share a
         * start or end time.
         *
         * Example:
         * If two bookings start at time 10, deltaByTime.get(10) becomes 2.
         */
        deltaByTime.merge(startTime, 1, Integer::sum);
        deltaByTime.merge(endTime, -1, Integer::sum);

        int activeBookings = 0;
        int maximumOverlap = 0;

        /*
         * SWEEP FROM LEFT TO RIGHT
         *
         * Because the map is ordered, values() visits the net changes
         * in chronological order.
         *
         * Invariant:
         * After processing each timestamp, activeBookings equals the
         * number of bookings active immediately after that timestamp.
         *
         * The active count stays constant until the next timestamp.
         * Therefore, the maximum of these running counts is the global
         * maximum overlap.
         */
        for (int change : deltaByTime.values()) {
            activeBookings += change;

            // Preserve the maximum across the entire calendar, not just
            // the portion affected by the most recent booking.
            maximumOverlap = Math.max(maximumOverlap, activeBookings);
        }

        /*
         * Complexity:
         *
         * Let n be the number of bookings after this call.
         * There are at most 2n distinct timestamps.
         *
         * Two TreeMap updates: O(log n).
         * Full sweep:           O(n).
         * Total per book():     O(n).
         *
         * Across N bookings:    O(N^2) worst-case total time.
         * Space:                O(N) for the distinct timestamps.
         *
         * Why this is appropriate:
         * The problem permits at most 400 bookings, so the straightforward
         * sweep is efficient and much easier to maintain than a segment tree.
         */
        return maximumOverlap;
    }
}
