/**
 * 732. My Calendar III
 * 
 * ============================================================================
 * 1. PROBLEM STATEMENT
 * ============================================================================
 * You are implementing a program to use as your calendar. We can add a new event 
 * without failing, but we want to know the maximum number of overlapping events 
 * (a k-booking) at any point in time.
 * 
 * A k-booking happens when k events have some non-empty intersection.
 * The events are represented as half-open intervals [startTime, endTime).
 * 
 * Implement the MyCalendarThree class:
 * - MyCalendarThree() Initializes the object.
 * - int book(int startTime, int endTime) Adds the event to the calendar and 
 *   returns an integer k representing the largest integer such that there exists 
 *   a k-booking in the calendar.
 * 
 * Constraints:
 * - 0 <= startTime < endTime <= 10^9
 * - At most 400 calls will be made to book.
 * 
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (To ask the interviewer)
 * ============================================================================
 * 1. Do we ever reject a booking in this version?
 *    (No. Unlike My Calendar I and II, we accept every booking. We just return 
 *    the new peak congestion/depth).
 * 2. Does a half-open interval mean `[10, 20)` and `[20, 30)` do not overlap?
 *    (Correct. They share a boundary, but not a moment in time, so depth is 1).
 * 3. What is the maximum number of `book` calls we expect?
 *    (Constraints say 400. This is the biggest hint: an O(N) per-call approach 
 *    yielding O(N^2) total time will perfectly pass, taking at most 160,000 ops).
 * 4. Are the timestamps large?
 *    (Yes, up to 10^9. We cannot allocate an array of size 1 billion).
 * 
 * ============================================================================
 * 3. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement & Constraint: 
 * We need to dynamically track the maximum depth of overlapping intervals. 
 * The massive coordinate space (10^9) prevents standard array allocation, while 
 * the small number of queries (400) provides a generous time allowance.
 * 
 * --- Approach 1: Flat Difference Array (Mental Sandbox / Brute Force) ---
 * 1. Idea: Allocate an array of size 10^9. When booking `[start, end)`, add +1 
 *    at `start` and -1 at `end`. Then iterate the entire array to find the max prefix sum.
 * 2. Why it fails: Memory! An integer array of size 10^9 requires ~4GB of RAM, 
 *    which causes an OutOfMemoryError. Scanning it 400 times causes Time Limit Exceeded.
 * 3. The pivot observation: Most of that 1 billion-element array is filled with 0s. 
 *    The state only changes at the exact moments an event starts or ends.
 * 
 * Time Complexity: O(MAX_TIME) per call -> TLE. Space: O(MAX_TIME) -> OOM.
 * 
 * --- Approach 2: Sparse Difference Array via TreeMap (Optimal for N=400) ---
 * 1. Idea: Use a `TreeMap<Integer, Integer>` to store only the non-zero deltas. 
 *    A start is a `+1` event, an end is a `-1` event.
 * 2. What I'd naturally try: `map.put(start, map.getOrDefault(start, 0) + 1)`. 
 *    Iterate through `map.values()` accumulating a running sum, and track the max.
 * 3. Why it works: The prefix sum of a difference array exactly equals the concurrent 
 *    overlapping intervals at any given coordinate. The TreeMap automatically keeps 
 *    these boundaries sorted chronologically.
 * 4. Why this is the ceiling for interviews: It is 15 lines of highly readable, 
 *    bug-free code. For N=400 calls, iterating up to 800 keys takes virtually no time.
 * 
 * Time Complexity: O(N) per `book()` call — O(log N) to insert, O(N) to sweep. Total O(N^2).
 * Space Complexity: O(N) — stores at most 2N coordinates.
 * 
 * --- Approach 3: Dynamic Segment Tree (Advanced Optimal for large N) ---
 * 1. Idea: If N was 100,000, sweeping the TreeMap (O(N^2)) would fail. We would need 
 *    a data structure that can add +1 to a range `[start, end-1]` and query the global 
 *    max in O(log(MAX_TIME)).
 * 2. What I'd naturally try: An Implicit Segment Tree with Lazy Propagation. Because 
 *    we only instantiate tree nodes when a specific time range is booked, it uses 
 *    space proportional to the queries, not the 10^9 coordinate space.
 * 3. Why it works: Range updates (`+1`) and global max queries become strictly logarithmic.
 * 
 * Time Complexity: O(log(MAX_TIME)) per call.
 * Space Complexity: O(N log(MAX_TIME)).
 * 
 * Verdict for 45-minute interview:
 * Write Approach 2 (TreeMap). It is the expected, idiomatic solution. I will also 
 * provide Approach 3 (Segment Tree) to demonstrate how a Staff/Principal engineer 
 * handles extreme scale.
 * 
 * ============================================================================
 * 4. EDGE CASES
 * ============================================================================
 * - Exact boundary touch: `[10, 20)` and `[20, 30)`. At time 20, we have a -1 and a +1. 
 *   Because they are processed at the same coordinate, the running sum correctly 
 *   absorbs both, staying perfectly flat without a false spike.
 * - Identical bookings: `[10, 20)`, `[10, 20)`, `[10, 20)`. `getOrDefault` handles 
 *   the accumulation (+3 at 10, -3 at 20) flawlessly.
 * 
 * ============================================================================
 * 5. KEY INSIGHT, DIAGRAM & DRY RUN
 * ============================================================================
 * > Key Insight: 
 * "Max k-booking is just the peak of the prefix sum."
 * By treating intervals as decoupled start (+1) and end (-1) events, we project 
 * them onto a timeline. The highest running total on that timeline is our answer.
 * 
 * Diagram: Prefix Sum Sweep
 * Bookings: [10, 20), [50, 60), [10, 40)
 * 
 * TreeMap Keys:   10      20      40      50      60
 * Deltas:         +2      -1      -1      +1      -1
 * 
 * Sweep:
 * At 10: 0 + 2 = 2 (Max: 2)
 * At 20: 2 - 1 = 1
 * At 40: 1 - 1 = 0
 * At 50: 0 + 1 = 1
 * At 60: 1 - 1 = 0
 * Return Max = 2!
 * 
 * ============================================================================
 * 6. PITFALLS & PATTERN RECOGNITION
 * ============================================================================
 * Pitfalls:
 * - Overwriting map values: Doing `map.put(start, 1)` instead of accumulating 
 *   with `getOrDefault`. Multiple events can start or end at the exact same millisecond!
 * - Sweeping incorrectly: You must sweep the *entire* map on every call. You cannot 
 *   just look at the local neighborhood of the new interval, because inserting a new 
 *   interval might bridge two smaller peaks into a new global maximum.
 * 
 * Pattern Recognition: 
 * "Overlaps + Arbitrary limits + Small N = TreeMap Difference Array."
 * This single pattern solves My Calendar II, My Calendar III, Car Pooling, and 
 * Corporate Flight Bookings.
 * 
 * ============================================================================
 * 7. INTERVIEW SCRIPT
 * ============================================================================
 * "To find the maximum overlapping intervals at any point, we can view this as a 
 * Difference Array problem. We want to add +1 to the start of an event and -1 to the end.
 * Since the coordinate space is up to 10^9, we can't allocate a flat array. But since 
 * the number of bookings is small (400), we can use a sparse representation via a TreeMap. 
 * For every booking, we record the boundary changes, and then sweep through the 
 * map's values keeping a running total. The peak of this running total is our 
 * max k-booking. This takes O(N) per call, scaling perfectly for 400 calls."
 * 
 * ============================================================================
 * 8. FOLLOW-UPS
 * ============================================================================
 * Q: What if N is 100,000? Sweeping the map takes O(N^2) and will TLE.
 * A: We would switch to a Dynamic Segment Tree with Lazy Propagation. Instead of 
 *    sweeping a timeline, we push the `+1` interval directly down a tree structure 
 *    representing the `[0, 10^9]` range. Updating and querying the global max 
 *    both drop to strictly O(log(MAX_TIME)).
 */

import java.util.Map;
import java.util.TreeMap;

public class MyCalendarIIISolution {

    // ========================================================================
    // APPROACH 2: TREEMAP / LINE SWEEP (Optimal for N=400)
    // ========================================================================
    static class MyCalendarThree {
        // TreeMap keeps the chronological timeline sorted automatically
        private TreeMap<Integer, Integer> timeline;

        public MyCalendarThree() {
            timeline = new TreeMap<>();
        }

        /**
         * Time Complexity: O(N) per call.
         * Space Complexity: O(N) overall.
         */
        public int book(int startTime, int endTime) {
            // 1. Record the state changes (+1 for start, -1 for drop-off)
            // MUST use getOrDefault because multiple events can share a boundary!
            timeline.put(startTime, timeline.getOrDefault(startTime, 0) + 1);
            timeline.put(endTime, timeline.getOrDefault(endTime, 0) - 1);

            int maxK = 0;
            int activeEvents = 0;

            // 2. Sweep the timeline chronologically to find the peak depth
            for (int delta : timeline.values()) {
                activeEvents += delta;
                
                if (activeEvents > maxK) {
                    maxK = activeEvents;
                }
            }

            return maxK;
        }
    }

    // ========================================================================
    // APPROACH 3: DYNAMIC SEGMENT TREE (Advanced Optimal for massive N)
    // ========================================================================
    static class MyCalendarThreeSegmentTree {
        
        // Node representing a time range [L, R]
        class Node {
            int maxOverlap; // Maximum overlapping intervals in this range
            int lazy;       // Pending +1 increments to be pushed to children
            Node left, right;
        }

        private Node root;
        private final int MAX_TIME = (int) 1e9;

        public MyCalendarThreeSegmentTree() {
            root = new Node();
        }

        /**
         * Time Complexity: O(log(MAX_TIME)) per call (~30 operations).
         * Space Complexity: O(N * log(MAX_TIME)).
         */
        public int book(int startTime, int endTime) {
            // Note: problem is [startTime, endTime), so the inclusive update 
            // is up to endTime - 1.
            update(root, 0, MAX_TIME, startTime, endTime - 1, 1);
            
            // The global maximum overlap is always sitting at the root!
            return root.maxOverlap;
        }

        private void update(Node node, int L, int R, int l, int r, int val) {
            // If the current range [L, R] is completely inside the target update range [l, r]
            if (l <= L && R <= r) {
                node.maxOverlap += val;
                node.lazy += val;
                return;
            }
            
            // Otherwise, we need to traverse down, so lazily create children if missing
            int mid = L + (R - L) / 2;
            if (node.left == null) node.left = new Node();
            if (node.right == null) node.right = new Node();

            // Push pending lazy updates down to children to ensure accuracy
            if (node.lazy != 0) {
                node.left.maxOverlap += node.lazy;
                node.left.lazy += node.lazy;
                
                node.right.maxOverlap += node.lazy;
                node.right.lazy += node.lazy;
                
                node.lazy = 0; // Clear the lazy tag
            }

            // Partially overlap: route to left/right branches
            if (l <= mid) {
                update(node.left, L, mid, l, r, val);
            }
            if (r > mid) {
                update(node.right, mid + 1, R, l, r, val);
            }

            // Pull up the true maximum from the children
            node.maxOverlap = Math.max(node.left.maxOverlap, node.right.maxOverlap);
        }
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================
    public static void main(String[] args) {
        System.out.println("--- Testing TreeMap Sweep Approach ---");
        MyCalendarThree cal1 = new MyCalendarThree();
        
        System.out.println("Book [10, 20): " + cal1.book(10, 20)); // Expected: 1
        System.out.println("Book [50, 60): " + cal1.book(50, 60)); // Expected: 1
        System.out.println("Book [10, 40): " + cal1.book(10, 40)); // Expected: 2 (Overlap [10, 20))
        System.out.println("Book [5, 15):  " + cal1.book(5, 15));  // Expected: 3 (Overlap [10, 15))
        System.out.println("Book [5, 10):  " + cal1.book(5, 10));  // Expected: 3 (Peak is still 3)
        System.out.println("Book [25, 55): " + cal1.book(25, 55)); // Expected: 3
        
        System.out.println("\n--- Testing Segment Tree Approach ---");
        MyCalendarThreeSegmentTree cal2 = new MyCalendarThreeSegmentTree();
        
        System.out.println("Book [10, 20): " + cal2.book(10, 20)); // 1
        System.out.println("Book [50, 60): " + cal2.book(50, 60)); // 1
        System.out.println("Book [10, 40): " + cal2.book(10, 40)); // 2
        System.out.println("Book [5, 15):  " + cal2.book(5, 15));  // 3
        System.out.println("Book [5, 10):  " + cal2.book(5, 10));  // 3
        System.out.println("Book [25, 55): " + cal2.book(25, 55)); // 3
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern: Difference Array (Line Sweep). 
 * For extreme scaling (N > 10,000), use Dynamic Segment Tree.
 * 
 * Key observation: You do not need to construct the physical intersections. 
 * A timeline naturally reveals intersections through the amplitude of its 
 * prefix sum.
 * 
 * Memorize vs Understand: 
 * UNDERSTAND why the loop `for (int delta : timeline.values())` works without 
 * looking at the keys. We just care about the height of the peaks, not where 
 * they happen on the calendar.
 * 
 * Most common trap: Forgetting `getOrDefault`. If two meetings end at 4:00 PM, 
 * replacing `-1` with `-1` means you permanently lose track of a meeting, 
 * artificially inflating the rest of the timeline's active count.
 * 
 * One-line mental trigger: "Max K-Booking? TreeMap difference array, return max prefix sum."
 */










































import java.util.Map;
import java.util.TreeMap;

class MyCalendarThree {

    /*
     * TreeMap maintains all event coordinates in sorted order.
     *
     * Key   = timestamp
     * Value = net change in active bookings at that timestamp.
     *
     * Example:
     *
     * [10, 20) produces:
     *
     * 10 -> +1
     * 20 -> -1
     *
     * Multiple bookings can modify the same timestamp.
     */
    private final TreeMap<Integer, Integer> events;

    public MyCalendarThree() {
        events = new TreeMap<>();
    }

    public int book(int startTime, int endTime) {

        /*
         * STEP 1: Record the starting event.
         *
         * WHY +1?
         * A booking becomes active at startTime.
         */
        events.merge(startTime, 1, Integer::sum);

        /*
         * STEP 2: Record the ending event.
         *
         * WHY -1?
         * The booking is no longer active at endTime.
         *
         * This correctly models [startTime, endTime).
         */
        events.merge(endTime, -1, Integer::sum);

        /*
         * STEP 3: Sweep through all event coordinates.
         *
         * WHY?
         * The number of active bookings changes only at event
         * coordinates.
         *
         * Between two consecutive coordinates, the number of
         * active bookings remains constant.
         */
        int activeBookings = 0;

        /*
         * This stores the maximum number of simultaneous bookings
         * encountered during the sweep.
         */
        int maximumBookings = 0;

        /*
         * TreeMap.values() iterates in ascending key order.
         *
         * Therefore, we process all time changes chronologically.
         */
        for (int change : events.values()) {

            /*
             * Apply the net change at the current timestamp.
             */
            activeBookings += change;

            /*
             * Update the maximum overlap.
             */
            maximumBookings = Math.max(
                    maximumBookings,
                    activeBookings
            );
        }

        /*
         * Return the maximum overlap after this booking.
         */
        return maximumBookings;
    }
}










































class MyCalendarThreeSegmentTree {

    /*
     * Coordinate boundaries.
     *
     * The original problem guarantees:
     * 0 <= startTime < endTime <= 10^9.
     *
     * We model the event coordinates using [0, 10^9].
     */
    private static final int MIN = 0;
    private static final int MAX = 1_000_000_000;

    private static class Node {

        /*
         * Maximum prefix sum in this segment.
         *
         * This represents the maximum overlap contribution
         * encountered while sweeping through this segment,
         * assuming the count before the segment is zero.
         */
        int max;

        /*
         * Sum of all event deltas in this segment.
         *
         * This is the net change contributed by the segment.
         */
        int sum;

        Node left;
        Node right;
    }

    private final Node root = new Node();

    public int book(int startTime, int endTime) {

        /*
         * Add +1 at startTime.
         *
         * A booking becomes active at its starting coordinate.
         */
        update(root, MIN, MAX, startTime, 1);

        /*
         * Add -1 at endTime.
         *
         * A booking is no longer active at its ending coordinate.
         */
        update(root, MIN, MAX, endTime, -1);

        /*
         * The root's maximum prefix sum represents the maximum
         * number of simultaneously active bookings.
         */
        return root.max;
    }

    private void update(
            Node node,
            int low,
            int high,
            int index,
            int delta
    ) {

        /*
         * BASE CASE:
         *
         * We have reached the leaf corresponding to index.
         * Apply the event delta.
         */
        if (low == high) {
            node.sum += delta;
            node.max = Math.max(0, node.sum);
            return;
        }

        /*
         * Find the midpoint safely.
         *
         * Using low + (high - low) / 2 avoids integer overflow.
         */
        int mid = low + (high - low) / 2;

        /*
         * Recursively update only the child containing index.
         *
         * Dynamic allocation:
         * We create a node only when an update needs that branch.
         */
        if (index <= mid) {

            if (node.left == null) {
                node.left = new Node();
            }

            update(node.left, low, mid, index, delta);

        } else {

            if (node.right == null) {
                node.right = new Node();
            }

            update(node.right, mid + 1, high, index, delta);
        }

        /*
         * STEP 1: Calculate the total sum of this segment.
         *
         * Missing children represent segments with no event
         * changes, so their sum is zero.
         */
        int leftSum = node.left == null ? 0 : node.left.sum;
        int rightSum = node.right == null ? 0 : node.right.sum;

        node.sum = leftSum + rightSum;

        /*
         * STEP 2: Calculate maximum prefix sum.
         *
         * A maximum prefix is either:
         *
         * 1. Entirely within the left segment.
         * 2. Entire left segment + a prefix of the right segment.
         *
         * Missing children contribute zero.
         */
        int leftMax = node.left == null ? 0 : node.left.max;
        int rightMax = node.right == null ? 0 : node.right.max;

        node.max = Math.max(
                leftMax,
                leftSum + rightMax
        );
    }
}
