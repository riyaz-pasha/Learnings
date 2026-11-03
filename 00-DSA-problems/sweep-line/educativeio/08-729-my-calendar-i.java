/**
 * 729. My Calendar I
 * 
 * ============================================================================
 * 1. PROBLEM STATEMENT
 * ============================================================================
 * You are implementing a program to use as your calendar. We can add a new event 
 * if adding the event will not cause a double booking.
 * 
 * A double booking happens when two events have some non-empty intersection.
 * The events are given as half-open intervals [startTime, endTime).
 * 
 * Implement the MyCalendar class:
 * - MyCalendar() Initializes the calendar object.
 * - boolean book(int startTime, int endTime) Returns true if the event can be 
 *   added successfully without causing a double booking. Otherwise, return false.
 * 
 * Constraints:
 * - 0 <= start < end <= 10^9
 * - At most 1000 calls will be made to book.
 * 
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (To ask the interviewer)
 * ============================================================================
 * 1. Are the events guaranteed to be half-open? Meaning, if one event ends at 10 
 *    and another starts at 10, is that considered an overlap?
 *    (Crucial for boundary logic: half-open means [5, 10) and [10, 15) do NOT overlap).
 * 2. What is the maximum number of `book` calls we expect?
 *    (Constraints say 1000. This tells us an O(N^2) brute force is acceptable 
 *    to pass, but we should definitely aim for O(N log N) to demonstrate scalability).
 * 3. Are the bookings coming in dynamically (online), or do we have the full 
 *    list of booking requests upfront (offline)?
 *    (They come one by one. This means we cannot just sort a master list of all 
 *    requests once. We need a dynamic data structure that maintains order).
 * 4. Can the start time and end time be identical? 
 *    (Constraints say start < end, so 0-duration events won't happen).
 * 
 * ============================================================================
 * 3. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement & Constraint: 
 * We need to maintain a collection of non-overlapping intervals and dynamically 
 * check if a new interval intersects with any existing ones. Because queries are 
 * online (arrive one-by-one), the bottleneck is how fast we can find potential overlaps.
 * 
 * --- Approach 1: Brute Force List (Acceptable for N=1000) ---
 * 1. Idea: Just keep a flat list of all successfully booked intervals. Every time 
 *    a new request comes in, loop through the entire list and check for overlap.
 * 2. What I'd naturally try: `List<int[]> calendar`. Loop through it. The overlap 
 *    condition for two intervals [s1, e1) and [s2, e2) is `max(s1, s2) < min(e1, e2)`.
 * 3. Why it works: It exhaustively verifies the new event against every single 
 *    confirmed event on the calendar.
 * 4. Why it's too slow/costly at scale: If N gets large (e.g., 100,000 bookings), 
 *    scanning the entire list for every single new booking leads to O(N^2) total time.
 * 5. What work is repeated: We check intervals that are in completely different 
 *    centuries or years, nowhere near our target booking time.
 * 6. The pivot observation: If we keep the intervals sorted by start time, we 
 *    don't need to check ALL past events. We only need to check the event immediately 
 *    BEFORE our new start time, and the event immediately AFTER our new start time.
 *    
 * Time Complexity: O(N) per `book()` call, O(N^2) total — because we iterate the whole list.
 * Space Complexity: O(N) — because we store N events in an ArrayList.
 * 
 * --- Approach 2: Binary Search Tree / TreeMap (Optimal) ---
 * 1. Idea: Maintain the intervals in a balanced Binary Search Tree, keyed by `startTime`.
 * 2. What I'd naturally try: Java's `TreeMap<Integer, Integer>`. When `book(start, end)` 
 *    is called, use `calendar.floorKey(start)` to find the event just before, and 
 *    `calendar.ceilingKey(start)` to find the event just after.
 * 3. Why it works: If the previous event finishes before or exactly when our new 
 *    event starts (`prevEnd <= start`), AND the next event starts after or exactly 
 *    when our new event finishes (`end <= nextStart`), there is no overlap!
 * 4. Why this is the ceiling: It turns an O(N) linear scan into an O(log N) tree 
 *    traversal. It dynamically maintains sorted order as new events are added.
 * 
 * Time Complexity: O(log N) per `book()` call, O(N log N) total — because TreeMap lookup and insertion take logarithmic time.
 * Space Complexity: O(N) — because the TreeMap stores up to N valid bookings.
 * 
 * Verdict for 45-minute interview:
 * I would write Approach 2 (TreeMap). It demonstrates knowledge of standard library 
 * tree structures, handles edge cases cleanly, and is the absolute optimal way to 
 * manage dynamic interval insertions. 
 * 
 * ============================================================================
 * 4. EDGE CASES
 * ============================================================================
 * - Exact boundary touch: `book(10, 20)` then `book(20, 30)`. 
 *   Must return true. Half-open intervals mean 20 is exclusive for the first.
 * - Completely enveloped: Calendar has `[10, 20)`. Try `book(12, 15)`.
 *   Must fail.
 * - Completely enveloping: Calendar has `[12, 15)`. Try `book(10, 20)`.
 *   Must fail.
 * - Empty calendar: The very first `book` call. 
 *   TreeMap will return null for floor and ceiling keys; code must handle nulls.
 * 
 * ============================================================================
 * 5. KEY INSIGHT, DIAGRAM & DRY RUN
 * ============================================================================
 * > Key Insight: 
 * "Sorted intervals only conflict with their immediate neighbors."
 * If you sort non-overlapping intervals, a new interval can ONLY possibly overlap 
 * with the one directly to its left (floor) or directly to its right (ceiling). 
 * All others are shielded.
 * 
 * Diagram:
 * Current Calendar: [10, 20), [30, 40), [50, 60)
 * Trying to book: [22, 28)
 * 
 * floorKey(22) -> 10. Value (end time) -> 20. 
 * Does 20 <= 22? Yes. No overlap on the left.
 * 
 * ceilingKey(22) -> 30. 
 * Does 28 (new end) <= 30? Yes. No overlap on the right.
 * -> Book it!
 * 
 * Dry Run (Optimal Approach):
 * `MyCalendarTreeMap cal = new MyCalendarTreeMap();`
 * 1. `cal.book(10, 20)`
 *    floorKey(10) -> null. ceilingKey(10) -> null.
 *    Success! TreeMap: {10: 20}
 * 
 * 2. `cal.book(15, 25)`
 *    floorKey(15) -> 10. End time is 20.
 *    Condition: `prevEnd <= start` -> `20 <= 15`. FALSE!
 *    Overlap detected with previous event. Returns false.
 * 
 * 3. `cal.book(20, 30)`
 *    floorKey(20) -> 10. End time is 20.
 *    Condition: `prevEnd <= start` -> `20 <= 20`. TRUE.
 *    ceilingKey(20) -> null. 
 *    Success! TreeMap: {10: 20, 20: 30}
 * 
 * ============================================================================
 * 6. PITFALLS & PATTERN RECOGNITION
 * ============================================================================
 * Pitfalls:
 * - Overlap math: The standard overlap for `[s1, e1)` and `[s2, e2)` is 
 *   `s1 < e2 && s2 < e1`. Trying to write out exhaustive logic (starts inside, 
 *   ends inside, engulfs, enveloped) usually leads to buggy spaghetti code. 
 *   Stick to the simple intersection condition or the neighbor boundaries.
 * - NullPointerExceptions: In Java, `floorKey` and `ceilingKey` return `null` 
 *   if no such key exists. Attempting to auto-unbox these into `int` without 
 *   checking will crash. Always use `Integer`.
 * 
 * Pattern Recognition: 
 * "Dynamic disjoint intervals -> TreeMap (or custom BST)."
 * Any time a problem asks you to maintain a running schedule, booking system, 
 * or range tracker where data arrives sequentially, immediately reach for a 
 * self-balancing binary search tree.
 * 
 * ============================================================================
 * 7. INTERVIEW SCRIPT
 * ============================================================================
 * "Since we are receiving booking requests dynamically, we can't just sort an array 
 * of all requests upfront. A brute force approach would keep a list of booked events 
 * and check the new event against every single past event in O(N) time per booking.
 * 
 * However, we don't need to check all events. If our calendar is sorted, a new 
 * event only risks overlapping with the event strictly before it, and the event 
 * strictly after it. I'll use a TreeMap where the keys are start times and values 
 * are end times. For each booking, I'll grab the `floorKey` and `ceilingKey`. 
 * If the previous event finishes before my new start time, and my new end time 
 * finishes before the next event starts, it's a valid booking. This drops the 
 * time complexity to O(log N) per booking."
 * 
 * ============================================================================
 * 8. FOLLOW-UPS
 * ============================================================================
 * Q: What if we are allowed to double-book, but not triple-book? (My Calendar II)
 * A: The TreeMap logic breaks down because intervals can now overlap. We would 
 *    switch to a Difference Array (Line Sweep) using a `TreeMap<Integer, Integer>` 
 *    to store deltas (+1 for start, -1 for end). We iterate through the map to 
 *    maintain a running sum, and if it exceeds 2, we rollback the insertion and return false.
 * 
 * Q: What if N is millions, and TreeMap has too much object overhead in Java?
 * A: We could build a custom Segment Tree (or a dynamic implicit Segment Tree) 
 *    to track booked ranges, or write our own array-backed BST to avoid the node 
 *    allocation overhead of `java.util.TreeMap`.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class MyCalendarSolution {

    // ========================================================================
    // APPROACH 1: BRUTE FORCE (List)
    // ========================================================================
    static class MyCalendarBruteForce {
        // We just store intervals as int[] pairs
        private List<int[]> calendar;

        public MyCalendarBruteForce() {
            calendar = new ArrayList<>();
        }

        /**
         * Time Complexity: O(N) per call
         * Space Complexity: O(N) total
         */
        public boolean book(int start, int end) {
            for (int[] event : calendar) {
                // Two intervals [s1, e1) and [s2, e2) overlap if:
                // max(s1, s2) < min(e1, e2)
                // Equivalently: one starts before the other ends, AND vice versa.
                if (start < event[1] && event[0] < end) {
                    return false; // Double booking detected!
                }
            }
            // No overlaps found, safe to book
            calendar.add(new int[]{start, end});
            return true;
        }
    }

    // ========================================================================
    // APPROACH 2: BINARY SEARCH TREE (Optimal)
    // ========================================================================
    static class MyCalendarTreeMap {
        // TreeMap automatically sorts entries by Key (which is our startTime).
        // Key = start time, Value = end time.
        private TreeMap<Integer, Integer> calendar;

        public MyCalendarTreeMap() {
            calendar = new TreeMap<>();
        }

        /**
         * Time Complexity: O(log N) per call
         * Space Complexity: O(N) total
         */
        public boolean book(int start, int end) {
            // Find the event that starts immediately before or at our target 'start'
            Integer prevStart = calendar.floorKey(start);
            
            // Find the event that starts immediately after our target 'start'
            Integer nextStart = calendar.ceilingKey(start);
            
            // Check left neighbor:
            // If there IS a previous event, its end time must be <= our start time.
            if (prevStart != null && calendar.get(prevStart) > start) {
                return false;
            }
            
            // Check right neighbor:
            // If there IS a next event, its start time must be >= our end time.
            if (nextStart != null && nextStart < end) {
                return false;
            }
            
            // We passed both guard rails. It perfectly fits in the gap!
            calendar.put(start, end);
            return true;
        }
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================
    public static void main(String[] args) {
        System.out.println("--- Testing Optimal TreeMap Approach ---");
        MyCalendarTreeMap myCalendar = new MyCalendarTreeMap();
        
        // Example 1
        System.out.println("Booking [10, 20): " + myCalendar.book(10, 20)); // Expected: true
        System.out.println("Booking [15, 25): " + myCalendar.book(15, 25)); // Expected: false
        System.out.println("Booking [20, 30): " + myCalendar.book(20, 30)); // Expected: true
        
        // Edge Case: Enveloped
        System.out.println("Booking [12, 18): " + myCalendar.book(12, 18)); // Expected: false
        
        // Edge Case: Engulfing
        System.out.println("Booking [5, 40):  " + myCalendar.book(5, 40));  // Expected: false

        // Valid gap fill
        System.out.println("Booking [30, 40): " + myCalendar.book(30, 40)); // Expected: true
        
        System.out.println("\n--- Cross-Checking with Brute Force ---");
        MyCalendarBruteForce bfCalendar = new MyCalendarBruteForce();
        System.out.println("Booking [10, 20): " + bfCalendar.book(10, 20)); // Expected: true
        System.out.println("Booking [15, 25): " + bfCalendar.book(15, 25)); // Expected: false
        System.out.println("Booking [20, 30): " + bfCalendar.book(20, 30)); // Expected: true
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern: Dynamic Disjoint Intervals -> TreeMap.
 * 
 * Key observation: When keeping track of non-overlapping intervals, you don't 
 * need to compare a new interval against the whole history. By keeping intervals 
 * sorted by start time, you only ever need to check the exact floor and ceiling 
 * neighbors.
 * 
 * Memorize vs Understand: 
 * UNDERSTAND why `prevEnd <= start` and `end <= nextStart` guarantees no overlaps 
 * anywhere else in the tree. Because the tree is strictly sorted and non-overlapping, 
 * if you clear the immediate neighbors, no distant neighbor can magically reach you.
 * 
 * Most common trap: Auto-unboxing `null`. Calling `.floorKey()` returns `Integer`, 
 * which can be null. Assigning it directly to an `int` throws a NullPointerException 
 * when the tree is empty.
 * 
 * One-line mental trigger: "Online booking without overlaps? FloorKey, CeilingKey, insert."
 */




























import java.util.Map;
import java.util.TreeMap;

class MyCalendar {

    /*
     * TreeMap maintains all booking start times in sorted order.
     *
     * KEY:
     *     Booking start time.
     *
     * VALUE:
     *     Booking end time.
     *
     * Example:
     *
     * 10 -> 20
     * 30 -> 40
     * 50 -> 60
     *
     * Represents:
     *
     * [10,20), [30,40), [50,60)
     *
     * WHY TREEMAP?
     *
     * We need to efficiently find:
     * 1. The previous booking.
     * 2. The next booking.
     *
     * TreeMap is backed by a Red-Black Tree and provides
     * O(log N) lookup and insertion.
     */
    private final TreeMap<Integer, Integer> calendar;

    public MyCalendar() {
        calendar = new TreeMap<>();
    }

    public boolean book(int startTime, int endTime) {

        /*
         * STEP 1: Find the previous booking.
         *
         * floorEntry(startTime) returns the entry with the
         * greatest key <= startTime.
         *
         * In other words:
         * Find the booking whose start time is closest to
         * startTime from the left, including equality.
         *
         * Example:
         *
         * Existing:
         * 10 -> 20
         * 30 -> 40
         * 50 -> 60
         *
         * New booking starts at 35.
         *
         * floorEntry(35) returns:
         * 30 -> 40
         */
        Map.Entry<Integer, Integer> previous =
                calendar.floorEntry(startTime);

        /*
         * STEP 2: Check whether the previous booking overlaps.
         *
         * Previous booking:
         * [previous.start, previous.end)
         *
         * New booking:
         * [startTime, endTime)
         *
         * They overlap if:
         *
         * previous.end > startTime
         *
         * WHY STRICTLY GREATER?
         *
         * If previous.end == startTime:
         *
         * Previous: [10,20)
         * New:      [20,30)
         *
         * They do not overlap because 20 is excluded
         * from the previous half-open interval.
         */
        if (previous != null && previous.getValue() > startTime) {
            return false;
        }

        /*
         * STEP 3: Find the next booking.
         *
         * ceilingEntry(startTime) returns the entry with
         * the smallest key >= startTime.
         *
         * Example:
         *
         * Existing:
         * 10 -> 20
         * 30 -> 40
         * 50 -> 60
         *
         * New booking starts at 25.
         *
         * ceilingEntry(25) returns:
         * 30 -> 40
         */
        Map.Entry<Integer, Integer> next =
                calendar.ceilingEntry(startTime);

        /*
         * STEP 4: Check whether the next booking overlaps.
         *
         * Next booking:
         * [next.start, next.end)
         *
         * New booking:
         * [startTime, endTime)
         *
         * They overlap if:
         *
         * endTime > next.start
         *
         * If endTime == next.start, the intervals are adjacent,
         * which is perfectly valid for half-open intervals.
         */
        if (next != null && endTime > next.getKey()) {
            return false;
        }

        /*
         * STEP 5: Both neighboring bookings are safe.
         *
         * Therefore, accept the booking.
         *
         * Store:
         * startTime -> endTime
         *
         * TreeMap automatically maintains sorted key order.
         */
        calendar.put(startTime, endTime);

        return true;
    }
}
























class MyCalendar {

    /*
     * ROOT represents the root of our Binary Search Tree.
     *
     * Every node represents one successfully accepted booking.
     *
     * IMPORTANT INVARIANT:
     *
     * No two intervals stored in this BST overlap.
     *
     * BST ordering:
     *
     * LEFT SUBTREE:
     * Every interval lies completely before or adjacent to
     * the current interval.
     *
     * RIGHT SUBTREE:
     * Every interval lies completely after or adjacent to
     * the current interval.
     *
     * Example:
     *
     *                  [20, 30)
     *                  /      \
     *             [10, 15)  [40, 50)
     *
     * This is an ordinary, unbalanced BST.
     */
    private Node root;

    public MyCalendar() {
        root = null;
    }

    public boolean book(int startTime, int endTime) {

        /*
         * PHASE 1: VALIDATE THE BOOKING.
         *
         * canInsert() traverses the BST and checks whether
         * the new interval conflicts with any existing interval.
         *
         * WHY SEPARATE VALIDATION FROM INSERTION?
         *
         * If we insert while checking, we might modify the tree
         * before discovering a conflict.
         *
         * By checking first, the tree remains unchanged when
         * a booking is rejected.
         */
        if (!canInsert(root, startTime, endTime)) {
            return false;
        }

        /*
         * PHASE 2: INSERT THE BOOKING.
         *
         * We reach this point only if the new interval does not
         * overlap with any existing interval.
         *
         * insert() follows the same BST navigation rules and
         * creates a new node at the appropriate position.
         *
         * IMPORTANT:
         * We must assign the returned node to root because
         * the tree might initially be empty.
         */
        root = insert(root, startTime, endTime);

        return true;
    }

    /*
     * Checks whether [startTime, endTime) can be inserted
     * into the subtree rooted at 'node'.
     *
     * This is a recursive BST search.
     *
     * We do NOT need to inspect every node.
     * We only follow one root-to-leaf path.
     */
    private boolean canInsert(
            Node node,
            int startTime,
            int endTime
    ) {

        /*
         * BASE CASE:
         *
         * We reached an empty position.
         *
         * This means no existing interval along the search path
         * conflicts with our new interval.
         *
         * Since the BST invariant guarantees that intervals
         * in the other branches are also ordered and non-overlapping,
         * the booking is valid.
         */
        if (node == null) {
            return true;
        }

        /*
         * CASE 1: NEW INTERVAL BELONGS TO THE LEFT SUBTREE.
         *
         * Existing interval: [node.start, node.end)
         * New interval:      [startTime, endTime)
         *
         * Condition:
         *
         * endTime <= node.start
         *
         * Example:
         *
         * Existing: [20, 30)
         * New:      [10, 20)
         *
         * The new interval ends exactly when the existing interval
         * starts.
         *
         * Because intervals are HALF-OPEN, they do not overlap.
         *
         * Therefore, search the left subtree.
         */
        if (endTime <= node.start) {

            return canInsert(
                    node.left,
                    startTime,
                    endTime
            );
        }

        /*
         * CASE 2: NEW INTERVAL BELONGS TO THE RIGHT SUBTREE.
         *
         * Condition:
         *
         * startTime >= node.end
         *
         * Example:
         *
         * Existing: [20, 30)
         * New:      [30, 40)
         *
         * The new interval starts exactly when the existing
         * interval ends.
         *
         * Again, they do not overlap.
         *
         * Therefore, search the right subtree.
         */
        else if (startTime >= node.end) {

            return canInsert(
                    node.right,
                    startTime,
                    endTime
            );
        }

        /*
         * CASE 3: OVERLAP FOUND.
         *
         * Neither of the two non-overlapping conditions holds.
         *
         * Therefore:
         *
         * startTime < node.end
         * AND
         * node.start < endTime
         *
         * This is precisely the interval-overlap condition.
         *
         * Example:
         *
         * Existing: [20, 30)
         * New:      [25, 35)
         *
         * Both intervals share the time range [25, 30).
         *
         * Reject immediately.
         */
        else {

            return false;
        }
    }

    /*
     * Inserts a previously validated interval into the BST.
     *
     * IMPORTANT:
     * This method assumes the interval has already been validated
     * using canInsert().
     *
     * Therefore, we do not need to check for overlap again.
     *
     * The method returns the root of the updated subtree.
     */
    private Node insert(
            Node node,
            int startTime,
            int endTime
    ) {

        /*
         * BASE CASE:
         *
         * We found an empty position.
         *
         * Create a new node representing the accepted booking.
         */
        if (node == null) {
            return new Node(startTime, endTime);
        }

        /*
         * CASE 1: Insert into the left subtree.
         *
         * The new interval finishes before or exactly when
         * the current interval starts.
         *
         * This is the same navigation rule used in canInsert().
         */
        if (endTime <= node.start) {

            /*
             * Recursively insert into the left subtree.
             *
             * Assign the returned subtree root to node.left.
             *
             * This assignment is important because recursion
             * may create a completely new node.
             */
            node.left = insert(
                    node.left,
                    startTime,
                    endTime
            );
        }

        /*
         * CASE 2: Insert into the right subtree.
         *
         * Because canInsert() already validated the booking,
         * the new interval must belong on the right whenever
         * it does not belong on the left.
         *
         * This means:
         *
         * startTime >= node.end
         */
        else {

            node.right = insert(
                    node.right,
                    startTime,
                    endTime
            );
        }

        /*
         * Return the current node.
         *
         * This preserves the root of the current subtree
         * while allowing recursive calls to update child pointers.
         */
        return node;
    }

    /*
     * Represents one calendar booking.
     *
     * Each node stores:
     *
     * start = booking start time
     * end   = booking end time
     *
     * left  = earlier non-overlapping bookings
     * right = later non-overlapping bookings
     */
    private static class Node {

        final int start;
        final int end;

        Node left;
        Node right;

        Node(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}
