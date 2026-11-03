/**
 * 731. My Calendar II
 * 
 * ============================================================================
 * 1. PROBLEM STATEMENT
 * ============================================================================
 * You are implementing a program to use as your calendar. We can add a new event 
 * if adding the event will not cause a triple booking.
 * 
 * A triple booking happens when three events have some non-empty intersection 
 * (i.e., some moment is common to all three events).
 * The event can be represented as a pair of integers startTime and endTime that 
 * represents a booking on the half-open interval [startTime, endTime).
 * 
 * Implement the MyCalendarTwo class:
 * - MyCalendarTwo() Initializes the calendar object.
 * - boolean book(int startTime, int endTime) Returns true if the event can be 
 *   added successfully without causing a triple booking. Otherwise, return false.
 * 
 * Constraints:
 * - 0 <= start < end <= 10^9
 * - At most 1000 calls will be made to book.
 * 
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (To ask the interviewer)
 * ============================================================================
 * 1. If an event causes a triple booking and returns false, do we discard it completely?
 *    (Yes, it should not be added to the calendar at all. This means our data structure 
 *    must support rollback or pre-validation).
 * 2. What is the maximum number of `book` calls we expect?
 *    (Constraints say 1000. This is crucial because it means an O(N) per-call approach 
 *    yielding O(N^2) overall time is perfectly optimal. We don't need a heavy Segment Tree).
 * 3. Does a half-open interval mean `[10, 20)` and `[20, 30)` do not overlap?
 *    (Correct. The exact touch at 20 does not count as a double booking).
 * 4. Can the start time and end time be extremely large?
 *    (Yes, up to 10^9. We cannot use a fixed-size Difference Array of 1 billion elements).
 * 
 * ============================================================================
 * 3. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement & Constraint: 
 * We must track overlapping intervals and specifically reject any region that exceeds 
 * a depth of 2. Since coordinate bounds are 10^9, arrays are out. Since calls are 
 * limited to 1000, O(N) per call is perfectly acceptable.
 * 
 * --- Approach 1: Two Lists (Tracking the Overlaps) ---
 * 1. Idea: Keep one list of all normal `bookings`, and a separate list representing 
 *    the exact intersecting regions of `doubleBookings`. 
 * 2. What I'd naturally try: When a new event comes, first check if it intersects 
 *    ANY interval in the `doubleBookings` list. If it does, that's a triple booking -> reject!
 *    If it doesn't, it's safe. Find all its intersections with the normal `bookings` 
 *    list, add those intersections to `doubleBookings`, and add the event to `bookings`.
 * 3. Why it works: We explicitly compute and isolate the "danger zones".
 * 4. Why it's too slow/costly for massive datasets (but fine here): Computing 
 *    intersections manually becomes cumbersome if we allow up to K bookings (e.g., My Calendar III).
 * 5. What work is repeated: We iterate through potentially large lists of past events 
 *    even if they are historically far away.
 * 6. The pivot observation: The intersection of two intervals `[s1, e1)` and `[s2, e2)` 
 *    is strictly `[Math.max(s1, s2), Math.min(e1, e2))`. If the max start is strictly 
 *    less than the min end, they overlap.
 * 
 * Time Complexity: O(N) per `book()` call — because we iterate through the `doubleBookings` and `bookings` lists of max size N.
 * Space Complexity: O(N) — because we store up to N events and their overlapping regions.
 * 
 * --- Approach 2: Line Sweep with TreeMap (Difference Array) ---
 * 1. Idea: Decouple the start and end times into independent timeline events. 
 *    A start is `+1` overlapping event, an end is `-1` event.
 * 2. What I'd naturally try: Use a `TreeMap<Integer, Integer>` to maintain sorted 
 *    chronological order. Add `+1` at `start` and `-1` at `end`. Iterate through 
 *    the map values accumulating a running sum. If the sum hits 3, we rollback the 
 *    `+1` and `-1` and return false.
 * 3. Why it works: The prefix sum of a difference array exactly equals the concurrent 
 *    overlapping intervals at any given coordinate.
 * 4. Why this is the ceiling for general K-bookings: It gracefully handles arbitrary 
 *    depths. However, in Java, for just K=2 (Triple Booking), the TreeMap iteration 
 *    is practically slower than the Two Lists approach due to Object unboxing.
 * 
 * Time Complexity: O(N) per `book()` call — O(log N) to insert into TreeMap, but O(N) to iterate and validate the prefix sum.
 * Space Complexity: O(N) — because the TreeMap stores at most 2N coordinates.
 * 
 * Verdict for 45-minute interview:
 * Write Approach 1 (Two Lists). It is usually much faster in practice for N=1000 
 * because iterating over an ArrayList of primitives/simple objects in Java is 
 * blazing fast compared to TreeMap entry iteration. However, I will also provide 
 * the TreeMap approach because it scales beautifully if the interviewer asks 
 * "What if we wanted to prevent 10-bookings?"
 * 
 * ============================================================================
 * 4. EDGE CASES
 * ============================================================================
 * - Exactly touching intervals: `[10, 20)` and `[20, 30)`. `Math.max(10, 20) = 20`, 
 *   `Math.min(20, 30) = 20`. `20 < 20` is FALSE. Correctly identified as no overlap.
 * - Enveloping: Calendar has `[10, 50)`. New is `[20, 30)`. Intersection is `[20, 30)`.
 * - Identical bookings: `[10, 20)`, `[10, 20)`, `[10, 20)`. The 3rd one correctly 
 *   fails because `[10, 20)` is already in the `doubleBookings` list.
 * 
 * ============================================================================
 * 5. KEY INSIGHT, DIAGRAM & DRY RUN
 * ============================================================================
 * > Key Insight: 
 * "To prevent a depth of 3, explicitly track the regions of depth 2."
 * If you know exactly where the calendar is double-booked, a new event only causes 
 * a failure if it touches one of those specific double-booked zones.
 * 
 * Diagram: Intersection Logic
 * Event A:    [10 ........................ 40)
 * Event B:                 [25 ........................ 55)
 * 
 * max(10, 25) = 25
 * min(40, 55) = 40
 * Overlap region:          [25 ........... 40) -> Add this to doubleBookings!
 * 
 * Dry Run (Two Lists Approach):
 * `book(10, 20)` -> safe. bookings = [[10, 20]], doubles = []
 * `book(50, 60)` -> safe. bookings = [[10, 20], [50, 60]], doubles = []
 * `book(10, 40)` -> touches [10, 20]. 
 *                   Overlap = [max(10,10), min(20,40)) = [10, 20).
 *                   bookings = [[10, 20], [50, 60], [10, 40]]
 *                   doubles = [[10, 20]]
 * `book(5, 15)`  -> Checks doubles: touches [10, 20]! 
 *                   max(5,10) = 10, min(15,20) = 15. 10 < 15 is TRUE!
 *                   Triple booking detected! Returns false.
 * 
 * ============================================================================
 * 6. PITFALLS & PATTERN RECOGNITION
 * ============================================================================
 * Pitfalls:
 * - Incomplete rollback in TreeMap: If you increment `start` and decrement `end`, 
 *   and then iteration fails, you MUST decrement `start` and increment `end` to 
 *   undo the operation. If you forget to clean up 0 values, the map bloats.
 * - Checking bookings before doubleBookings: If you update the lists before validating 
 *   the entire new interval, you pollute the state. Validate against `doubleBookings` 
 *   FIRST, and only if clear, update the states.
 * 
 * Pattern Recognition: 
 * "Small fixed limits (K=2) -> Intersecting tracking. Arbitrary limits -> Line Sweep."
 * 
 * ============================================================================
 * 7. INTERVIEW SCRIPT
 * ============================================================================
 * "Since we want to avoid triple bookings, one effective way is to explicitly track 
 * where double bookings exist. I'll maintain two lists: one for all bookings, and 
 * one for the intersection regions representing double bookings. When a new event 
 * arrives, I first check if it overlaps with any double booking. If it does, we 
 * reject it immediately. If it doesn't, we iterate through the normal bookings, 
 * calculate the intersections with the new event, add those to the double booking 
 * list, and finally save the event. Calculating an intersection is just checking 
 * if `max(start1, start2) < min(end1, end2)`. Since the max calls are 1000, this 
 * O(N) per call operation is extremely fast and avoids the overhead of TreeMaps."
 * 
 * ============================================================================
 * 8. FOLLOW-UPS
 * ============================================================================
 * Q: What if we needed to support at most K bookings instead of 2? (My Calendar III)
 * A: The Two Lists approach becomes messy because we'd need K-1 lists. We would 
 *    switch to the TreeMap Line Sweep approach, validating if the prefix sum exceeds K.
 * 
 * Q: What if there are 100,000 calls instead of 1,000? 
 * A: O(N) per call becomes O(N^2) overall, which will Time Limit Exceed. 
 *    We would need a dynamically allocated Segment Tree with Lazy Propagation, 
 *    capable of updating ranges and querying the max depth in O(log N) time.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MyCalendarIISolution {

    // ========================================================================
    // APPROACH 1: TWO LISTS (Tracking Overlaps) - Most practical for N=1000
    // ========================================================================
    static class MyCalendarTwoLists {
        private List<int[]> bookings;
        private List<int[]> doubleBookings;

        public MyCalendarTwoLists() {
            bookings = new ArrayList<>();
            doubleBookings = new ArrayList<>();
        }

        /**
         * Time Complexity: O(N) per book() call.
         * Space Complexity: O(N) overall to store lists.
         */
        public boolean book(int start, int end) {
            // 1. Verify against all known double-booked regions.
            // If it overlaps with ANY double booking, it's a triple booking.
            for (int[] db : doubleBookings) {
                // Intersection logic
                if (Math.max(start, db[0]) < Math.min(end, db[1])) {
                    return false; // Triple booking detected
                }
            }

            // 2. No triple booking found. It's safe to add.
            // But first, we must record any NEW double bookings this creates.
            for (int[] b : bookings) {
                int overlapStart = Math.max(start, b[0]);
                int overlapEnd = Math.min(end, b[1]);
                
                // If there is a valid overlap region
                if (overlapStart < overlapEnd) {
                    doubleBookings.add(new int[]{overlapStart, overlapEnd});
                }
            }

            // 3. Finally, add the new event to the normal bookings.
            bookings.add(new int[]{start, end});
            return true;
        }
    }

    // ========================================================================
    // APPROACH 2: LINE SWEEP WITH TREEMAP (Difference Array)
    // ========================================================================
    static class MyCalendarTwoTreeMap {
        private TreeMap<Integer, Integer> map;

        public MyCalendarTwoTreeMap() {
            map = new TreeMap<>();
        }

        /**
         * Time Complexity: O(N) per book() call (due to iteration).
         * Space Complexity: O(N) overall to store map keys.
         */
        public boolean book(int start, int end) {
            // Tentatively add the event to our timeline
            map.put(start, map.getOrDefault(start, 0) + 1);
            map.put(end, map.getOrDefault(end, 0) - 1);

            int activeBookings = 0;

            // Sweep the timeline chronologically
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                activeBookings += entry.getValue();

                // If at any point we hit 3 concurrent bookings, it's invalid
                if (activeBookings == 3) {
                    // ROLLBACK the tentative addition
                    map.put(start, map.get(start) - 1);
                    if (map.get(start) == 0) {
                        map.remove(start);
                    }
                    
                    map.put(end, map.get(end) + 1);
                    if (map.get(end) == 0) {
                        map.remove(end);
                    }
                    
                    return false;
                }
            }

            // Survived the sweep, booking is confirmed
            return true;
        }
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================
    public static void main(String[] args) {
        System.out.println("--- Testing Two Lists Approach ---");
        MyCalendarTwoLists cal1 = new MyCalendarTwoLists();
        
        System.out.println("Book [10, 20): " + cal1.book(10, 20)); // true
        System.out.println("Book [50, 60): " + cal1.book(50, 60)); // true
        System.out.println("Book [10, 40): " + cal1.book(10, 40)); // true (double books [10, 20])
        System.out.println("Book [5, 15):  " + cal1.book(5, 15));  // false (triple books [10, 15])
        System.out.println("Book [5, 10):  " + cal1.book(5, 10));  // true
        System.out.println("Book [25, 55): " + cal1.book(25, 55)); // true
        
        System.out.println("\n--- Testing TreeMap Approach ---");
        MyCalendarTwoTreeMap cal2 = new MyCalendarTwoTreeMap();
        
        System.out.println("Book [10, 20): " + cal2.book(10, 20)); // true
        System.out.println("Book [50, 60): " + cal2.book(50, 60)); // true
        System.out.println("Book [10, 40): " + cal2.book(10, 40)); // true 
        System.out.println("Book [5, 15):  " + cal2.book(5, 15));  // false 
        System.out.println("Book [5, 10):  " + cal2.book(5, 10));  // true
        System.out.println("Book [25, 55): " + cal2.book(25, 55)); // true
    }
}

/**
 * CORE PATTERN & TAKEAWAYS
 * 
 * Core pattern: To prevent a target depth of K (where K is small, like 2), 
 * explicitly track regions of depth K-1. For arbitrary or large K, use a TreeMap Line Sweep.
 * 
 * Key observation: The math for interval intersection is universally powerful. 
 * Two intervals overlap if and only if `Math.max(start1, start2) < Math.min(end1, end2)`. 
 * This elegant one-liner saves you from writing out complex nested if-statements 
 * (checking if start is inside, if end is inside, if it fully envelops, etc).
 * 
 * Most common trap: Forgetting to clean up zero-values during a TreeMap rollback. 
 * If a key resolves back to 0 changes, you must `map.remove(key)`, otherwise the map 
 * permanently stores redundant nodes, bloating memory and degrading iteration speed.
 * 
 * One-line mental trigger: "Triple booking? Keep a list of all double-booked intersections."
 */







































import java.util.*;

class MyCalendarTwo {

    /*
     * TreeMap maintains timestamps in sorted order.
     *
     * Key   = timestamp
     * Value = change in active booking count at that timestamp.
     *
     * Positive value means bookings start.
     * Negative value means bookings end.
     *
     * WHY TreeMap?
     * We need chronological processing of all events.
     * TreeMap maintains sorted keys automatically.
     */
    private final TreeMap<Integer, Integer> timeline = new TreeMap<>();

    public boolean book(int startTime, int endTime) {

        /*
         * STEP 1: Temporarily add the new booking.
         *
         * At startTime:
         *     Active bookings increase by 1.
         *
         * At endTime:
         *     Active bookings decrease by 1.
         *
         * merge() combines changes if multiple events
         * happen at the same timestamp.
         */

        timeline.merge(startTime, 1, Integer::sum);
        timeline.merge(endTime, -1, Integer::sum);

        /*
         * STEP 2: Sweep through all timestamps.
         *
         * activeBookings represents how many events
         * are active immediately after processing
         * the current timestamp.
         *
         * WHY?
         * For half-open intervals [start, end):
         *
         * - A booking starting at time t is active from t.
         * - A booking ending at time t is no longer active
         *   from t onwards.
         *
         * Combining both changes at the same timestamp
         * gives the correct count for the interval after t.
         */

        int activeBookings = 0;

        for (int change : timeline.values()) {

            activeBookings += change;

            /*
             * STEP 3: Detect triple booking.
             *
             * If three or more bookings are simultaneously
             * active, the new event must be rejected.
             */

            if (activeBookings >= 3) {

                /*
                 * STEP 4: Roll back the temporary booking.
                 *
                 * Undo the +1 at startTime.
                 * Undo the -1 at endTime.
                 *
                 * This restores the exact previous timeline.
                 */

                timeline.merge(startTime, -1, Integer::sum);
                timeline.merge(endTime, 1, Integer::sum);

                /*
                 * Remove timestamps whose net change
                 * has become zero.
                 *
                 * This is important because zero-valued
                 * entries have no effect on the sweep.
                 */

                if (timeline.get(startTime) == 0) {
                    timeline.remove(startTime);
                }

                if (timeline.get(endTime) == 0) {
                    timeline.remove(endTime);
                }

                return false;
            }
        }

        /*
         * STEP 5: No triple booking was found.
         *
         * Keep the temporary changes permanently.
         * The new event is successfully booked.
         */

        return true;
    }
}







































class MyCalendarTwo {

    /*
     * Dynamic segment-tree node.
     *
     * Every node represents some coordinate interval [left, right).
     *
     * max:
     *     Maximum booking count anywhere inside this node's range.
     *
     * lazy:
     *     A pending increment applied to the entire node range.
     *     It has not necessarily been pushed to its children.
     *
     * left / right:
     *     Dynamically allocated children.
     */
    private static class Node {

        int max;
        int lazy;

        Node left;
        Node right;
    }

    /*
     * Root represents the complete coordinate domain.
     *
     * Constraints:
     * 0 <= startTime < endTime <= 10^9
     *
     * We use [0, 1_000_000_000).
     */
    private final Node root = new Node();

    public boolean book(int startTime, int endTime) {

        /*
         * STEP 1: Temporarily increase booking counts
         * over the requested interval.
         *
         * We query the maximum after applying the update.
         */

        update(
            root,
            0,
            1_000_000_000,
            startTime,
            endTime,
            1
        );

        /*
         * STEP 2: Check whether any point has three bookings.
         *
         * root.max represents the maximum booking count
         * anywhere in the entire coordinate domain.
         */

        if (root.max >= 3) {

            /*
             * STEP 3: Roll back the rejected booking.
             *
             * Decrease the same interval by 1.
             *
             * This restores the previous calendar state.
             */

            update(
                root,
                0,
                1_000_000_000,
                startTime,
                endTime,
                -1
            );

            return false;
        }

        return true;
    }

    private void update(
        Node node,
        int left,
        int right,
        int queryLeft,
        int queryRight,
        int value
    ) {

        /*
         * CASE 1: Complete coverage.
         *
         * The requested interval completely covers
         * this segment-tree node.
         *
         * We can update the entire node in O(1).
         *
         * lazy records the pending update for descendants.
         */

        if (queryLeft <= left && right <= queryRight) {

            node.max += value;
            node.lazy += value;

            return;
        }

        /*
         * CASE 2: Partial coverage.
         *
         * Push any pending lazy update to the children
         * before recursively updating them.
         */

        int mid = left + (right - left) / 2;

        pushDown(node);

        /*
         * Recurse left if the query intersects the left child.
         *
         * Left child range:
         * [left, mid)
         */

        if (queryLeft < mid) {

            if (node.left == null) {
                node.left = new Node();
            }

            update(
                node.left,
                left,
                mid,
                queryLeft,
                queryRight,
                value
            );
        }

        /*
         * Recurse right if the query intersects the right child.
         *
         * Right child range:
         * [mid, right)
         */

        if (queryRight > mid) {

            if (node.right == null) {
                node.right = new Node();
            }

            update(
                node.right,
                mid,
                right,
                queryLeft,
                queryRight,
                value
            );
        }

        /*
         * STEP 3: Recalculate this node's maximum.
         *
         * WHY?
         * The maximum booking count in this range is the
         * maximum of its children's maximum values,
         * plus this node's own lazy increment.
         *
         * Null children represent untouched ranges
         * with a maximum booking count of zero.
         */

        int leftMax = node.left == null ? 0 : node.left.max;

        int rightMax = node.right == null ? 0 : node.right.max;

        node.max = node.lazy + Math.max(leftMax, rightMax);
    }

    private void pushDown(Node node) {

        /*
         * No pending update exists.
         * Nothing needs to be propagated.
         */

        if (node.lazy == 0) {
            return;
        }

        /*
         * Create children only when required.
         */

        if (node.left == null) {
            node.left = new Node();
        }

        if (node.right == null) {
            node.right = new Node();
        }

        /*
         * Propagate the parent's pending increment
         * to both children.
         *
         * Because the parent increment applies to its
         * entire range, both children receive it.
         */

        node.left.max += node.lazy;
        node.left.lazy += node.lazy;

        node.right.max += node.lazy;
        node.right.lazy += node.lazy;

        /*
         * The update has now been propagated.
         * Clear the parent's lazy value.
         */

        node.lazy = 0;
    }
}

