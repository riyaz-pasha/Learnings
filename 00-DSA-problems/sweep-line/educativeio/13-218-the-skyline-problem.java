/**
 * 218. The Skyline Problem
 * 
 * ============================================================================
 * 1. PROBLEM STATEMENT
 * ============================================================================
 * Given the locations and heights of all buildings (perfect rectangles on a flat surface),
 * return the skyline formed by these buildings.
 * 
 * Input: buildings = [[left_i, right_i, height_i], ...]
 * Output: [[x1, y1], [x2, y2], ...] sorted by x-coordinate.
 * 
 * A key point is the left endpoint of a horizontal segment. The last point has y=0.
 * Consecutive horizontal lines of equal height must be merged.
 * 
 * Constraints:
 * - 1 <= buildings.length <= 10^4
 * - 0 <= left_i < right_i <= 2^31 - 1
 * - 1 <= height_i <= 2^31 - 1
 * - buildings sorted by left_i
 * 
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (To ask the interviewer)
 * ============================================================================
 * 1. Can coordinates and heights be extremely large?
 *    (Yes, up to 2^31 - 1. This immediately eliminates any array-based sweeping where index = x-coordinate).
 * 2. What if two buildings share the exact same start or end coordinate?
 *    (This is the crux of the problem. We must carefully order events at the same x-coordinate 
 *    to avoid outputting false intermediate heights).
 * 3. What if a building ends at the exact x-coordinate where another begins?
 *    (If they are the same height, the skyline shouldn't dip to 0. We must process the new 
 *    building's start BEFORE the old building's end).
 * 4. Are we guaranteed valid building structures (left < right, height > 0)?
 *    (Yes, as per constraints).
 * 
 * ============================================================================
 * 3. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement & Constraint: 
 * We need to track the "maximum height" at any given x-coordinate. However, the 
 * coordinate space is up to 2 billion, making flat array mapping impossible. 
 * We must use an event-driven approach (Sweep Line).
 * 
 * --- Approach 1: True Brute Force (Array Simulation) ---
 * 1. Idea: Allocate an array of size `max_right`. Loop over every building and 
 *    update `arr[x] = max(arr[x], height)` for every x from `left` to `right-1`. 
 *    Finally, scan the array to find points where the height changes.
 * 2. What I'd naturally try: `int[] skyline = new int[MAX_X]`.
 * 3. Why it works: It perfectly simulates casting a shadow onto a wall.
 * 4. Why it's too slow/costly: `MAX_X` is 2^31 - 1. Allocating an array this large 
 *    requires ~8GB of contiguous RAM (OutOfMemoryError), and looping takes O(N * W) 
 *    which causes Time Limit Exceeded.
 * 5. What work is repeated: We update and check millions of empty or static coordinates.
 * 6. The pivot observation: The skyline ONLY changes at the left or right edges of buildings.
 * 
 * Time Complexity: O(N * MAX_WIDTH) — updating every individual coordinate.
 * Space Complexity: O(MAX_RIGHT) — storing the simulated skyline array.
 * 
 * --- Approach 2: Event Sorting + Priority Queue (Max-Heap) ---
 * 1. Idea: Deconstruct each building into two events: a "Start" event and an "End" event.
 *    Sweep through events from left to right. Maintain active heights in a Max-Heap.
 * 2. What I'd naturally try: Sort events by X. At a start event, add height to heap. 
 *    At an end event, remove height from heap. If `heap.peek()` (max active height) 
 *    changes after an event, record a new key point.
 * 3. Why it works: The max-heap always naturally bubbles the tallest active building to the top.
 * 4. Why it's too slow: In Java, `PriorityQueue.remove(Object)` takes O(N) time because 
 *    it must do a linear scan to find the object before removing it.
 * 
 * Time Complexity: O(N^2) — N events, and removing from the heap takes O(N).
 * Space Complexity: O(N) — storing N events and up to N heights in the heap.
 * 
 * --- Approach 3: Event Sorting + TreeMap (Optimal) ---
 * 1. Idea: Replace the Priority Queue with a `TreeMap<Integer, Integer>` acting as 
 *    a frequency map (`height -> count`). 
 * 2. Why it works: `TreeMap.lastKey()` gives us the max height in O(log N) time, 
 *    and finding/removing a specific height also takes strictly O(log N) time.
 * 3. Why this is the ceiling: We process 2N events, and each event updates a balanced 
 *    BST in O(log N) time. This is the optimal mathematical bound for dynamic maximum tracking.
 * 
 * Time Complexity: O(N log N) — 2N events, sorting takes O(N log N), TreeMap ops take O(log N).
 * Space Complexity: O(N) — storing 2N events and up to N heights in the TreeMap.
 * 
 * Verdict for 45-minute interview:
 * Write Approach 3 (TreeMap). It guarantees O(N log N) performance and prevents the 
 * silent O(N^2) trap of Java's Priority Queue. It requires demonstrating a brilliant 
 * custom tie-breaking sorting logic.
 * 
 * ============================================================================
 * 4. EDGE CASES (The Tie-Breaking Rules)
 * ============================================================================
 * When multiple events share the exact same X-coordinate, order matters massively:
 * 1. Two starts at same X: Process the TALLER one first. (If shorter is processed first, 
 *    we'd draw a false intermediate point before jumping to the taller one).
 * 2. Two ends at same X: Process the SHORTER one first. (If taller ends first, 
 *    the max dips to 0 before the shorter one catches it, creating a false gap).
 * 3. One start, one end at same X: Process the START first. (If the end processes first, 
 *    the skyline might dip to 0 before the new building props it back up).
 * 
 * ============================================================================
 * 5. KEY INSIGHT, DIAGRAM & DRY RUN
 * ============================================================================
 * > Key Insight & The Legendary Trick:
 * How do we elegantly handle all three tie-breaking rules without a massive if-else block?
 * Encode a Start event's height as a NEGATIVE number, and End event as POSITIVE.
 * When sorting by X, if X's are equal, just sort by `Integer.compare(h1, h2)`!
 * - Two starts (both negative): more negative (taller) comes first. (Rule 1 solved)
 * - Two ends (both positive): smaller positive (shorter) comes first. (Rule 2 solved)
 * - Start & End: negative (start) comes before positive (end). (Rule 3 solved)
 * 
 * ASCII Diagram: Tie-breaking start/end touch
 * B1: [2, 5, 10], B2: [5, 8, 10]
 * At X=5, B1 ends (+10), B2 starts (-10).
 * Sort by height: -10 comes before +10.
 * B2 starts, map has {10: 2}. Max is 10.
 * B1 ends, map has {10: 1}. Max is 10.
 * Skyline never dips! No false key points generated.
 * 
 * Dry Run:
 * Buildings: [[2,9,10], [3,7,15]]
 * Events: [2, -10], [3, -15], [7, 15], [9, 10]
 * TreeMap initialized with {0: 1} (the ground).
 * 
 * 1. Process [2, -10]: (Start 10)
 *    Add 10 to map. Map: {0:1, 10:1}.
 *    Max is 10. Previous max was 0. Changed! Add point [2, 10].
 * 
 * 2. Process [3, -15]: (Start 15)
 *    Add 15 to map. Map: {0:1, 10:1, 15:1}.
 *    Max is 15. Previous max was 10. Changed! Add point [3, 15].
 * 
 * 3. Process [7, 15]: (End 15)
 *    Remove 15 from map. Map: {0:1, 10:1}.
 *    Max is 10. Previous max was 15. Changed! Add point [7, 10].
 * 
 * 4. Process [9, 10]: (End 10)
 *    Remove 10 from map. Map: {0:1}.
 *    Max is 0. Previous max was 10. Changed! Add point [9, 0].
 * 
 * Result: [[2,10], [3,15], [7,10], [9,0]].
 * 
 * ============================================================================
 * 6. PITFALLS & PATTERN RECOGNITION
 * ============================================================================
 * Pitfalls:
 * - Priority Queue `remove(Object)` trap: A standard PriorityQueue in Java removes 
 *   elements by equality in O(N). This makes the Sweep Line O(N^2). Use TreeMap.
 * - Missing the ground: If you don't initialize the TreeMap with `height 0`, when 
 *   the last building is removed, `lastKey()` throws NoSuchElementException instead 
 *   of returning 0.
 * - Map value cleanup: When decrementing a height's frequency in TreeMap, if it 
 *   hits 0, you MUST `map.remove(height)`. Otherwise, `lastKey()` still sees the key!
 * 
 * Pattern Recognition: 
 * "Overlapping intervals + Max/Min value tracking = Sweep Line + TreeMap."
 * Same pattern as: Falling Squares, Maximum Population Year, Meeting Rooms III.
 * 
 * ============================================================================
 * 7. INTERVIEW SCRIPT
 * ============================================================================
 * "To build the skyline, we only care about the moments when the maximum active 
 * building height changes. Since coordinates can reach 2 billion, I'll use an 
 * event-driven Sweep Line approach. I'll split each building into a start event 
 * and an end event. 
 * 
 * To handle tricky edge cases—like buildings touching, or starting at the same 
 * point—I'll use a neat trick: representing start heights as negative and end 
 * heights as positive. This allows a simple ascending sort to perfectly order 
 * ties (taller starts first, shorter ends first, starts before ends).
 * 
 * I'll sweep through these sorted events, maintaining the active heights in a 
 * TreeMap acting as a frequency map. This gives me O(log N) insertions and 
 * removals, avoiding the O(N) removal penalty of a standard Java PriorityQueue. 
 * Whenever `treeMap.lastKey()` changes after an event, I record a new key point."
 * 
 * ============================================================================
 * 8. FOLLOW-UPS
 * ============================================================================
 * Q: What if N is millions, and TreeMap has too much object overhead in Java?
 * A: We could use a Segment Tree with coordinate compression. We collect all unique 
 *    X-coordinates, map them to indices 0 to K, and use a Segment Tree to apply 
 *    range maximum updates (lazy propagation). Time: O(N log N).
 * 
 * Q: How would you parallelize this if the city was huge?
 * A: We could use a Divide and Conquer approach (similar to Merge Sort). Recursively 
 *    split the buildings array in half, compute the skyline for each half, and 
 *    then merge the two skylines in O(N) time by sweeping through their key points 
 *    with two pointers.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public class SkylineProblem {

    /**
     * APPROACH: Sweep Line + TreeMap (Optimal)
     * Time Complexity: O(N log N)
     * Space Complexity: O(N)
     */
    public List<List<Integer>> getSkyline(int[][] buildings) {
        List<List<Integer>> result = new ArrayList<>();
        List<int[]> events = new ArrayList<>();
        
        // 1. Deconstruct buildings into start and end events
        for (int[] b : buildings) {
            // [x-coordinate, height]
            // TRICK: Start height is NEGATIVE, End height is POSITIVE
            events.add(new int[]{b[0], -b[2]});
            events.add(new int[]{b[1], b[2]});
        }
        
        // 2. Sort events by X coordinate.
        // If X coordinates are equal, sort by height value.
        // Because of our trick (- for start, + for end), this correctly orders:
        // - Two starts: more negative (taller) comes first
        // - Two ends: smaller positive (shorter) comes first
        // - Start & End: negative (start) comes before positive (end)
        Collections.sort(events, (a, b) -> {
            if (a[0] != b[0]) {
                return Integer.compare(a[0], b[0]);
            } else {
                return Integer.compare(a[1], b[1]);
            }
        });
        
        // 3. TreeMap to keep track of active heights.
        // Key = Height, Value = Frequency (number of buildings currently at this height)
        TreeMap<Integer, Integer> heightCounts = new TreeMap<>();
        
        // Initialize with ground level (height 0) to ensure the skyline gracefully 
        // drops to 0 when no buildings are active.
        heightCounts.put(0, 1);
        
        // Track the previous maximum height to know when a change occurs
        int prevMaxHeight = 0;
        
        // 4. Sweep through the timeline
        for (int[] event : events) {
            int x = event[0];
            int height = event[1];
            
            if (height < 0) {
                // It's a START event. 
                // Convert back to positive height and add to map.
                int h = -height;
                heightCounts.put(h, heightCounts.getOrDefault(h, 0) + 1);
            } else {
                // It's an END event.
                // Decrement the frequency of this height.
                int count = heightCounts.get(height);
                if (count == 1) {
                    // CRITICAL: Must completely remove the key if count hits 0,
                    // otherwise lastKey() will still see it!
                    heightCounts.remove(height);
                } else {
                    heightCounts.put(height, count - 1);
                }
            }
            
            // What is the tallest building active right now?
            int currMaxHeight = heightCounts.lastKey();
            
            // If the max height changed, we have a new key point in the skyline
            if (currMaxHeight != prevMaxHeight) {
                List<Integer> point = new ArrayList<>();
                point.add(x);
                point.add(currMaxHeight);
                result.add(point);
                
                prevMaxHeight = currMaxHeight;
            }
        }
        
        return result;
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================
    public static void main(String[] args) {
        SkylineProblem solution = new SkylineProblem();
        
        // Test Case 1: Standard overlapping buildings
        int[][] buildings1 = {{2, 9, 10}, {3, 7, 15}, {5, 12, 12}, {15, 20, 10}, {19, 24, 8}};
        System.out.println("Test 1 (Standard):");
        // Expected: [[2, 10], [3, 15], [7, 12], [12, 0], [15, 10], [20, 8], [24, 0]]
        System.out.println(solution.getSkyline(buildings1));
        
        // Test Case 2: Nested buildings (Fully enclosed)
        int[][] buildings2 = {{0, 2, 3}, {2, 5, 3}};
        System.out.println("\nTest 2 (Adjacent same height):");
        // Expected: [[0, 3], [5, 0]]
        System.out.println(solution.getSkyline(buildings2));
        
        // Test Case 3: Exact same start, different heights
        int[][] buildings3 = {{2, 10, 5}, {2, 5, 10}};
        System.out.println("\nTest 3 (Same start coordinate):");
        // Expected: [[2, 10], [5, 5], [10, 0]]
        // Without tie-breaking trick, might output [[2, 5], [2, 10], ...] which is invalid.
        System.out.println(solution.getSkyline(buildings3));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern: Sweep Line + TreeMap.
 * 
 * Key observation: The skyline only changes when the absolute maximum active 
 * building height changes. We can model this by maintaining a set of active 
 * heights in a self-balancing binary search tree.
 * 
 * Memorize vs Understand: 
 * MEMORIZE the tie-breaking trick: Start = -h, End = +h. `Integer.compare(a,b)` 
 * magically solves all three edge-case overlap rules perfectly.
 * UNDERSTAND why `PriorityQueue` fails in Java for this problem. `pq.remove(obj)` 
 * is an O(N) operation because a heap only guarantees fast lookup for the root, 
 * not arbitrary elements. `TreeMap` guarantees O(log N) arbitrary removals.
 * 
 * One-line mental trigger: "Skyline? Start -H, End +H, sort by X, track max in TreeMap."
 */







































import java.util.*;

class Solution {

    /*
     * Each building generates two events:
     *
     * 1. START event: A building becomes active.
     * 2. END event:   A building stops being active.
     *
     * x        -> Coordinate where the event occurs.
     * height   -> Height of the building.
     * isStart  -> true if building starts, false if it ends.
     *
     * Using a record gives us an immutable data carrier.
     */
    private record Event(int x, int height, boolean isStart) {
    }

    public List<List<Integer>> getSkyline(int[][] buildings) {

        /*
         * =========================================================
         * STEP 1: CONVERT BUILDINGS INTO EVENTS
         * =========================================================
         *
         * Every building is represented as:
         *
         * [left, right, height]
         *
         * It generates exactly TWO events:
         *
         * (left,  height, START)
         * (right, height, END)
         *
         * Example:
         *
         * Building = [2, 9, 10]
         *
         * Events:
         * (2, 10, START)
         * (9, 10, END)
         *
         * WHY?
         *
         * We don't need to examine every coordinate.
         * The skyline can change only when a building starts
         * or ends.
         *
         * Therefore, we convert the problem into processing
         * these events from left to right.
         *
         * Number of events = 2 * n.
         */
        List<Event> events = new ArrayList<>();

        for (int[] building : buildings) {

            int left = building[0];
            int right = building[1];
            int height = building[2];

            // Building becomes active at its left boundary.
            events.add(new Event(left, height, true));

            // Building becomes inactive at its right boundary.
            events.add(new Event(right, height, false));
        }

        /*
         * =========================================================
         * STEP 2: SORT EVENTS
         * =========================================================
         *
         * We process events in increasing order of x-coordinate.
         *
         * However, multiple events can occur at the same x.
         *
         * We use the following tie-breaking rules:
         *
         * 1. START events before END events.
         *
         * 2. Among START events:
         *    Higher heights come first.
         *
         * 3. Among END events:
         *    Lower heights come first.
         *
         * WHY THESE RULES?
         *
         * Consider two buildings:
         *
         * [0, 2, 3]
         * [2, 5, 3]
         *
         * At x = 2:
         * - First building ends.
         * - Second building starts.
         *
         * The skyline height remains 3.
         *
         * Processing START before END avoids temporarily
         * removing the existing height before adding its
         * replacement.
         *
         * For different heights, the ordering also follows
         * the conventional sweep-line event ordering.
         *
         * IMPORTANT:
         *
         * We still process ALL events having the same x-coordinate
         * before calculating the skyline.
         *
         * Consequently, the final output does not depend on
         * intermediate states at the same coordinate.
         *
         * Time complexity: O(n log n)
         */
        events.sort(
            Comparator.comparingInt(Event::x)

                .thenComparing((a, b) -> {

                    // Case 1: Both events are START events.
                    // Process taller buildings first.
                    if (a.isStart() && b.isStart()) {
                        return Integer.compare(
                            b.height(), a.height()
                        );
                    }

                    // Case 2: Both events are END events.
                    // Process shorter buildings first.
                    if (!a.isStart() && !b.isStart()) {
                        return Integer.compare(
                            a.height(), b.height()
                        );
                    }

                    // Case 3: One START and one END event.
                    // START must come before END.
                    return Boolean.compare(
                        b.isStart(), a.isStart()
                    );
                })
        );

        /*
         * =========================================================
         * STEP 3: MAINTAIN ACTIVE BUILDING HEIGHTS
         * =========================================================
         *
         * We use a TreeMap as a MULTISET.
         *
         * Key   = Building height.
         * Value = Number of currently active buildings having
         *         that height.
         *
         * Example:
         *
         * {
         *     10 -> 2,
         *     12 -> 1,
         *     15 -> 3
         * }
         *
         * This means:
         * - Two active buildings have height 10.
         * - One active building has height 12.
         * - Three active buildings have height 15.
         *
         * WHY DO WE NEED FREQUENCIES?
         *
         * Multiple buildings can have identical heights.
         *
         * If one building of height 10 ends, another building
         * of height 10 might still be active.
         *
         * Therefore, we cannot simply remove height 10.
         * We must decrement its frequency.
         *
         * WHY TREEMAP?
         *
         * TreeMap maintains keys in sorted order.
         *
         * Therefore:
         *
         * activeHeights.lastKey()
         *
         * gives us the maximum active building height.
         *
         * Insert       : O(log n)
         * Remove       : O(log n)
         * Find maximum : O(log n)
         *
         * Initially, no building is active.
         *
         * We insert height 0 to represent the ground.
         *
         * This guarantees that lastKey() always works,
         * even when no buildings are active.
         */
        TreeMap<Integer, Integer> activeHeights = new TreeMap<>();

        activeHeights.put(0, 1);

        /*
         * Stores the height of the previously generated
         * skyline segment.
         *
         * Initially, the skyline is at ground level (height 0).
         *
         * We compare every new maximum height with this value.
         */
        int previousHeight = 0;

        /*
         * Stores the final skyline key points.
         *
         * Every key point has the format:
         *
         * [x, height]
         */
        List<List<Integer>> skyline = new ArrayList<>();

        /*
         * Index pointing to the next event to process.
         */
        int i = 0;

        /*
         * =========================================================
         * STEP 4: PROCESS EVENTS FROM LEFT TO RIGHT
         * =========================================================
         *
         * We process all events having the same x-coordinate
         * as one group.
         *
         * WHY?
         *
         * Multiple buildings may start/end at the same coordinate.
         *
         * We must first apply ALL changes at that coordinate.
         * Only then can we calculate the actual skyline height.
         *
         * This prevents duplicate or incorrect intermediate
         * key points.
         */
        while (i < events.size()) {

            // Current coordinate being processed.
            int currentX = events.get(i).x();

            /*
             * =====================================================
             * STEP 5: PROCESS ALL EVENTS AT currentX
             * =====================================================
             *
             * We process every event whose x-coordinate equals
             * currentX.
             */
            while (i < events.size()
                    && events.get(i).x() == currentX) {

                Event event = events.get(i);

                int height = event.height();

                if (event.isStart()) {

                    /*
                     * BUILDING STARTS
                     *
                     * Increase the frequency of this height.
                     *
                     * Example:
                     *
                     * Before:
                     * {10 -> 1, 15 -> 1}
                     *
                     * A new building of height 10 starts.
                     *
                     * After:
                     * {10 -> 2, 15 -> 1}
                     *
                     * merge() either inserts the height with
                     * frequency 1 or increments its existing
                     * frequency.
                     */
                    activeHeights.merge(
                        height,
                        1,
                        Integer::sum
                    );

                } else {

                    /*
                     * BUILDING ENDS
                     *
                     * Decrease the frequency of this height.
                     *
                     * There are two possibilities:
                     *
                     * Case 1: Frequency == 1
                     *
                     * This is the last active building having
                     * this height.
                     *
                     * Remove the height completely.
                     *
                     * Case 2: Frequency > 1
                     *
                     * Other buildings of the same height
                     * are still active.
                     *
                     * Decrement the frequency by one.
                     *
                     * computeIfPresent() performs both operations
                     * in one expression.
                     *
                     * Returning null from a TreeMap remapping
                     * function removes that key.
                     */
                    activeHeights.computeIfPresent(
                        height,
                        (key, frequency) ->
                            frequency == 1
                                ? null
                                : frequency - 1
                    );
                }

                /*
                 * IMPORTANT:
                 *
                 * Move to the next event INSIDE this loop.
                 *
                 * Otherwise, we repeatedly process the same
                 * event and the loop never terminates.
                 */
                i++;
            }

            /*
             * =====================================================
             * STEP 6: FIND THE CURRENT SKYLINE HEIGHT
             * =====================================================
             *
             * All events at currentX have now been processed.
             *
             * The largest active height determines the skyline.
             *
             * Example:
             *
             * activeHeights:
             * {
             *     0  -> 1,
             *     10 -> 2,
             *     12 -> 1,
             *     15 -> 1
             * }
             *
             * lastKey() = 15
             *
             * Therefore, current skyline height = 15.
             *
             * Time complexity: O(log n)
             */
            int currentHeight = activeHeights.lastKey();

            /*
             * =====================================================
             * STEP 7: GENERATE A KEY POINT ONLY IF HEIGHT CHANGES
             * =====================================================
             *
             * We don't need to add a point if the skyline height
             * remains unchanged.
             *
             * Example:
             *
             * Previous height = 15
             * Current height  = 15
             *
             * No key point is generated.
             *
             * But:
             *
             * Previous height = 15
             * Current height  = 12
             *
             * Add:
             *
             * [currentX, 12]
             *
             * This guarantees that consecutive horizontal
             * segments never have the same height.
             *
             * It also guarantees that the final building
             * termination produces a point with height 0.
             */
            if (currentHeight != previousHeight) {

                skyline.add(
                    List.of(currentX, currentHeight)
                );

                // Update the height for the next comparison.
                previousHeight = currentHeight;
            }
        }

        /*
         * Every building has now started and ended.
         *
         * The final key point has height 0 because all buildings
         * are inactive after the rightmost building ends.
         */
        return skyline;
    }
}

