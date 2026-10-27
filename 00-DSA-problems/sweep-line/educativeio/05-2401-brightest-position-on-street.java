/**
 * 2021. Brightest Position on Street
 * 
 * ============================================================================
 * 1. PROBLEM STATEMENT
 * ============================================================================
 * A street is represented by a number line. You are given a 2D integer array 
 * lights where lights[i] = [position_i, range_i].
 * A light illuminates the street from [position_i - range_i, position_i + range_i] (inclusive).
 * 
 * Return the brightest position on the street (the coordinate illuminated by 
 * the highest number of lights).
 * If there are multiple positions with the same maximum brightness, return the 
 * smallest (earliest) position.
 * 
 * Constraints:
 * - 1 <= lights.length <= 10^5
 * - -10^8 <= position_i <= 10^8
 * - 0 <= range_i <= 10^8
 * 
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (To ask the interviewer)
 * ============================================================================
 * 1. Are the coordinates bounded to small arrays, or can they be huge?
 *    (Huge. Ranges can go from -2*10^8 to 2*10^8. A fixed-size array will cause an OutOfMemoryError).
 * 2. Is the illumination range strictly inclusive on both ends?
 *    (Yes. A light spanning to X means X is illuminated, and the darkness starts exactly at X + 1).
 * 3. Can the input array be empty?
 *    (Constraints say length >= 1, so we don't need to handle a null/empty array edge case).
 * 4. What if two completely different segments of the street have the same max brightness?
 *    (The problem specifies returning the *smallest* coordinate, so we must sweep left-to-right 
 *    and strictly update the max using `>` instead of `>=`).
 * 
 * ============================================================================
 * 3. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement & Constraint: 
 * We need to find the maximum concurrent overlapping intervals. The binding constraint 
 * here is the massive coordinate space (spanning ~400,000,000 units), making a standard 
 * Difference Array (which requires size 4*10^8) completely unfeasible for memory.
 * 
 * --- Approach 1: True Brute Force ---
 * 1. Idea: For every light, loop through its exact illuminated range and add +1 to a Map.
 * 2. What I'd naturally try: `for (int pos = start; pos <= end; pos++) map.put(pos, map.getOrDefault+1)`.
 * 3. Why it works: It perfectly simulates the exact brightness at every point.
 * 4. Why it's too slow/costly: A single light could have a range of 200,000,000. Looping 
 *    through that for 100,000 lights will cause a massive Time Limit Exceeded (O(N * R)).
 * 5. What work is repeated: We are repeatedly incrementing the middle points of the light 
 *    ranges when the brightness there is completely static.
 * 6. The pivot observation: Just like meeting rooms, the brightness ONLY changes at the 
 *    exact points where a light begins or ends. We only need to process these boundaries.
 * 
 * Time Complexity: O(N * R) — where R is the range of the lights.
 * Space Complexity: O(U) — where U is the number of unique coordinates.
 * 
 * --- Approach 2: Line Sweep with TreeMap (Intermediate) ---
 * 1. Idea: Instead of an array, use a `TreeMap<Integer, Integer>` to store the events.
 *    A light starting is `+1` at `start`, and ending is `-1` at `end + 1`.
 * 2. What I'd naturally try: Iterate lights, populate the TreeMap. Because a TreeMap 
 *    sorts its keys automatically, iterating through its `.entrySet()` gives a chronological 
 *    sweep of the timeline.
 * 3. Why it works: It ignores the massive empty spaces between lights and only processes 
 *    the coordinates where state changes.
 * 4. Why it's costly: `TreeMap` has significant overhead in Java. It creates a new Node 
 *    object for every key, causes autoboxing (int to Integer), and insertion is O(log N) 
 *    per element, making it noticeably slower in practice than arrays.
 * 5. What work is repeated: The overhead of dynamic tree rebalancing.
 * 6. The pivot observation: We know exactly how many events there will be (2 * N). 
 *    We don't need a dynamic tree; we can just dump all events into a flat 2D array and sort it.
 * 
 * Time Complexity: O(N log N) — from N insertions into the TreeMap.
 * Space Complexity: O(N) — storing up to 2N keys in the Map.
 * 
 * --- Approach 3: Event Sorting (Optimal) ---
 * 1. Idea: Extract all starts and ends into a fixed `int[][] events = new int[2*N][2]`. 
 *    Sort the array chronologically. 
 * 2. What I'd naturally try: `events[i] = [start, 1]`, `events[i+1] = [end + 1, -1]`. 
 *    Sort by the first index (the coordinate). Sweep through keeping a running total.
 * 3. Why it works: It behaves identically to the TreeMap but operates entirely on primitives 
 *    with zero object-creation overhead during the sweep.
 * 4. Why this is the ceiling: It strictly bounds space to exactly 2N, and O(N log N) is 
 *    the mathematical lower bound for sorting arbitrary coordinates.
 * 
 * Time Complexity: O(N log N) — because we sort an array of 2N events.
 * Space Complexity: O(N) — because we allocate a 2D array of size 2N x 2.
 * 
 * Verdict for 45-minute interview:
 * I would implement Approach 3 (Event Sorting). It is highly idiomatic for competitive 
 * programming and interviews, completely bypasses Java's autoboxing overhead, and clearly 
 * demonstrates mastery over custom comparators.
 * 
 * ============================================================================
 * 4. EDGE CASES
 * ============================================================================
 * - Exact boundary touch: Light A is [-2, 2], Light B is [3, 5]. 
 *   Because 2 is inclusive, Light A drops off at 3. Light B starts at 3. 
 *   If we sort events at coordinate 3, we MUST process the drop-off (-1) BEFORE 
 *   the start (+1) so the running total doesn't artificially spike to 2.
 * - Negative coordinates: Arrays can't have negative indices, which is why we 
 *   MUST use sorting/maps rather than a Difference Array.
 * - Completely disjoint lights: Overlap is 1, returns the start position of the very first light.
 * 
 * ============================================================================
 * 5. KEY INSIGHT, DIAGRAM & DRY RUN
 * ============================================================================
 * > Key Insight: 
 * "When coordinate bounds exceed 10^7, arrays crash. Fall back to Event Sorting."
 * Convert intervals into decoupled events: [start, +weight] and [end + 1, -weight].
 * 
 * Diagram: Event Sorting logic on exact boundary touches
 * Light A: [-2, 2], Light B: [3, 5]
 * Events at coord 3:  A ends (-1)  and  B starts (+1).
 * 
 * If we process (+1) first:
 * Brightness: 1 (from A) -> 2 (from B starting) -> 1 (from A ending).
 * We falsely record a max brightness of 2!
 * 
 * If we process (-1) first:
 * Brightness: 1 (from A) -> 0 (from A ending) -> 1 (from B starting).
 * Max brightness correctly stays 1.
 * 
 * Dry Run:
 * lights = [[-3, 2], [1, 2]]
 * Intervals: [-5, -1] and [-1, 3].
 * Events generated:
 * E1: [-5, 1], E2: [0, -1]  (Because -1 is inclusive, drops off at 0)
 * E3: [-1, 1], E4: [4, -1]
 * 
 * Sorted Events:
 * [-5, 1], [-1, 1], [0, -1], [4, -1]
 * 
 * Sweep:
 * Coord -5: curr = 1. Max = 1, Pos = -5.
 * Coord -1: curr = 2. Max = 2, Pos = -1.
 * Coord  0: curr = 1.
 * Coord  4: curr = 0.
 * Result: Max brightness 2 at position -1.
 * 
 * ============================================================================
 * 6. PITFALLS & PATTERN RECOGNITION
 * ============================================================================
 * Pitfalls:
 * - Sorting Comparator Trap: Forgetting to explicitly dictate tie-breaking logic. 
 *   If two events happen at the same coordinate, the `-1` MUST be processed before 
 *   the `+1`.
 * - Off-by-one End Range: Using `pos + range` as the drop-off instead of `pos + range + 1`. 
 *   The problem states illumination is inclusive.
 * 
 * Pattern Recognition: 
 * "Massive coordinates + Intervals = Decoupled Event Sorting."
 * This pattern completely replaces Meeting Rooms / Car Pooling array sweeps when 
 * coordinates can be negative or > 10^7.
 * 
 * ============================================================================
 * 7. INTERVIEW SCRIPT
 * ============================================================================
 * "Because the coordinates range up to hundreds of millions, we can't use a fixed 
 * difference array. We'd run out of memory instantly. 
 * Instead, I'll extract the start and end of each light's illumination as discrete events. 
 * A light spanning `[start, end]` creates a `+1` event at `start`, and a `-1` event 
 * at `end + 1`. I'll put all 2N events into a 2D array and sort them chronologically.
 * If two events land on the exact same coordinate, I'll process the drop-off (`-1`) 
 * first to avoid artificially inflating the concurrent brightness. 
 * Sweeping through this sorted array will give me the exact brightness at every 
 * critical point in O(N log N) time."
 * 
 * ============================================================================
 * 8. FOLLOW-UPS
 * ============================================================================
 * Q: What if we needed to return the *entire segment* that is brightest, not just one point?
 * A: Instead of saving a single `pos`, we track when the brightness hits `maxBrightness` 
 *    (saving it as `segmentStart`), and when it drops below `maxBrightness` 
 *    (saving `[segmentStart, current_event_coord - 1]` to a list of max segments).
 * 
 * Q: What if lights were added dynamically and we had to query the brightest position 
 *    constantly?
 * A: Event sorting requires a full O(N log N) sort every time. To support dynamic 
 *    updates, we would use a Segment Tree with Lazy Propagation (using coordinate 
 *    compression because of the massive range). Updates become O(log N) and queries O(1).
 */

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class BrightestPositionOnStreet {

    /**
     * APPROACH 3: Event Sorting (Optimal & Leanest)
     * Time Complexity: O(N log N) 
     * Space Complexity: O(N) 
     */
    public int brightestPosition(int[][] lights) {
        int n = lights.length;
        // Every light creates 2 events: a start and a drop-off
        int[][] events = new int[2 * n][2];
        
        // 1. Decouple lights into timeline events
        int idx = 0;
        for (int[] light : lights) {
            int pos = light[0];
            int range = light[1];
            
            int start = pos - range;
            // Inclusive range means it is still bright at (pos + range).
            // The darkness/drop-off begins at (pos + range + 1).
            int dropOff = pos + range + 1;
            
            events[idx++] = new int[]{start, 1};
            events[idx++] = new int[]{dropOff, -1};
        }
        
        // 2. Sort events chronologically
        Arrays.sort(events, (a, b) -> {
            // If coordinates are different, sort by coordinate ascending
            if (a[0] != b[0]) {
                return Integer.compare(a[0], b[0]);
            }
            // CRUCIAL: If coordinates are the same, process -1 before 1.
            // This prevents a false peak when one light ends exactly as another begins.
            return Integer.compare(a[1], b[1]);
        });
        
        // 3. Sweep the timeline
        int maxBrightness = -1;
        int brightestPos = 0;
        int currentBrightness = 0;
        
        for (int[] event : events) {
            currentBrightness += event[1];
            
            // Strictly greater (>) ensures we naturally keep the *smallest* coordinate
            // when there are multiple positions with the exact same max brightness.
            if (currentBrightness > maxBrightness) {
                maxBrightness = currentBrightness;
                brightestPos = event[0];
            }
        }
        
        return brightestPos;
    }

    /**
     * APPROACH 2: TreeMap Line Sweep (Intermediate / Alternative)
     * Time Complexity: O(N log N)
     * Space Complexity: O(N)
     * 
     * Note: Included to show how to handle this if requested to use Collections.
     * Generally slower than sorting a primitive 2D array due to autoboxing.
     */
    public int brightestPositionTreeMap(int[][] lights) {
        TreeMap<Integer, Integer> sweep = new TreeMap<>();
        
        for (int[] light : lights) {
            int start = light[0] - light[1];
            int dropOff = light[0] + light[1] + 1;
            
            sweep.put(start, sweep.getOrDefault(start, 0) + 1);
            sweep.put(dropOff, sweep.getOrDefault(dropOff, 0) - 1);
        }
        
        int maxBrightness = -1;
        int brightestPos = 0;
        int currentBrightness = 0;
        
        // TreeMap naturally iterates keys in ascending order
        for (Map.Entry<Integer, Integer> entry : sweep.entrySet()) {
            currentBrightness += entry.getValue();
            
            if (currentBrightness > maxBrightness) {
                maxBrightness = currentBrightness;
                brightestPos = entry.getKey();
            }
        }
        
        return brightestPos;
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================
    public static void main(String[] args) {
        BrightestPositionOnStreet solution = new BrightestPositionOnStreet();
        
        // Test Case 1: Standard overlapping lights
        int[][] lights1 = {{-3, 2}, {1, 2}, {3, 3}};
        System.out.println("Test 1 (Expected -1): " 
            + solution.brightestPosition(lights1));
            
        // Test Case 2: Exact boundary touch logic
        // Light A illuminates [-2, 2]. Light B illuminates [3, 5]. 
        // No overlap! Max brightness is 1, earliest is -2.
        int[][] lights2 = {{0, 2}, {4, 1}};
        System.out.println("Test 2 (Expected -2): " 
            + solution.brightestPosition(lights2));
            
        // Test Case 3: Multiple peaks of same brightness
        // Light A: [0, 2]. Light B: [5, 7]. 
        // Max brightness is 1. Earliest position is 0.
        int[][] lights3 = {{1, 1}, {6, 1}};
        System.out.println("Test 3 (Expected 0): " 
            + solution.brightestPosition(lights3));
            
        // Test Case 4: Completely nested lights
        // Light A: [-10, 10]. Light B: [0, 2].
        // Max brightness is 2, earliest position is 0.
        int[][] lights4 = {{0, 10}, {1, 1}};
        System.out.println("Test 4 (Expected 0): " 
            + solution.brightestPosition(lights4));
            
        // Cross-check approaches
        System.out.println("\nCross-checking approaches on Test 1:");
        System.out.println("Event Sorting: " + solution.brightestPosition(lights1));
        System.out.println("TreeMap:       " + solution.brightestPositionTreeMap(lights1));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern: Event Sorting (Decoupled Intervals).
 * 
 * Key observation: When coordinate ranges are massive (e.g., millions/billions), 
 * standard array-based Line Sweeping crashes due to memory limits. You MUST 
 * pivot to sorting events dynamically.
 * 
 * Memorize vs Understand: 
 * UNDERSTAND the tie-breaking comparator. If coordinates are equal, you MUST process 
 * the drop-off (-1) before the start (+1). Otherwise, your running total will 
 * temporarily count a light that is starting and a light that is ending as 
 * "concurrent" for an instant, returning a false peak.
 * 
 * Most common trap: Using `pos + range` as the drop-off point. Because illumination 
 * is inclusive, the light is still active AT `pos + range`. It only stops at `+ 1`.
 * 
 * One-line mental trigger: "Massive coordinates? Extract starts and ends to 2D array, sort, and sweep."
 */









































import java.util.Arrays;
import java.util.Comparator;

class Solution {

    /*
     * CORE IDEA: SWEEP LINE + DIFFERENCE EVENTS
     *
     * Each light is represented by:
     *     lights[i] = [position, range]
     *
     * It illuminates the inclusive interval:
     *     [position - range, position + range]
     *
     * A naive solution would calculate the brightness at every street
     * coordinate. That is inefficient because coordinates can be very large.
     *
     * Instead, brightness changes only at specific coordinates:
     *
     *     left       -> brightness increases by 1
     *     right + 1  -> brightness decreases by 1
     *
     * Why right + 1?
     * The interval is inclusive, so the light must illuminate `right`.
     * Its contribution must disappear only at the next integer coordinate.
     *
     * Example:
     *     Light interval = [2, 5]
     *
     *     Event at 2:  +1
     *     Event at 6:  -1
     *
     *     Coordinates 2, 3, 4 and 5 receive this light's contribution.
     *
     * Once all events are sorted, we sweep from left to right and maintain
     * the cumulative brightness. This cumulative sum equals the number of
     * lights illuminating the current coordinate.
     *
     * WHY USE A RECORD?
     * Event is an immutable data carrier. It keeps the coordinate and its
     * brightness change together, making the event array easy to understand.
     */
    private record Event(long coordinate, int delta) {}

    public int brightestPosition(int[][] lights) {

        int n = lights.length;

        /*
         * Every light creates exactly two events:
         *
         *     1. Start event: left endpoint, delta = +1
         *     2. End event:   right endpoint + 1, delta = -1
         *
         * Therefore, for n lights, we need exactly 2 * n events.
         *
         * We use long for event coordinates because calculating
         * position + range + 1 can exceed the int range in general.
         *
         * The problem's final answer fits in int, but intermediate
         * calculations should still use long to prevent overflow.
         */
        Event[] events = new Event[2 * n];

        for (int i = 0; i < n; i++) {

            // Convert the input values to long BEFORE arithmetic.
            // This ensures subtraction and addition use 64-bit arithmetic.
            long position = lights[i][0];
            long range = lights[i][1];

            long left = position - range;
            long right = position + range;

            /*
             * Convert this inclusive interval into two events.
             *
             * Example: position = 4, range = 2
             *
             *     left  = 2
             *     right = 6
             *
             *     Event(2, +1)
             *     Event(7, -1)
             *
             * The running brightness increases at 2 and decreases at 7.
             * Therefore, this light contributes at coordinates 2 through 6.
             */
            events[2 * i] = new Event(left, +1);
            events[2 * i + 1] = new Event(right + 1L, -1);
        }

        /*
         * Sort all events by their coordinate in ascending order.
         *
         * Why?
         * We must process brightness changes in the order in which we
         * encounter them while moving from left to right along the street.
         *
         * Events with the same coordinate become adjacent after sorting.
         * We will process all such events together before measuring brightness.
         *
         * Comparator.comparingLong avoids narrowing coordinates to int.
         */
        Arrays.sort(
            events,
            Comparator.comparingLong(Event::coordinate)
        );

        /*
         * currentBrightness:
         *     The total number of active lights at the coordinate currently
         *     being processed.
         *
         * maxBrightness:
         *     The greatest brightness seen so far.
         *
         * answer:
         *     The leftmost coordinate at which maxBrightness is achieved.
         *
         * Initially, maxBrightness = -1 because brightness cannot be negative.
         * This ensures that the first valid event coordinate establishes
         * the initial answer, even if there is only one light.
         */
        int currentBrightness = 0;
        int maxBrightness = -1;
        long answer = 0;

        int i = 0;

        while (i < events.length) {

            // The next coordinate at which at least one brightness change
            // occurs. Since events are sorted, coordinates never decrease.
            long coordinate = events[i].coordinate();

            /*
             * IMPORTANT: PROCESS ALL EVENTS AT THE SAME COORDINATE.
             *
             * Multiple lights may start or stop contributing at the same
             * coordinate. Their net effect must be applied before we inspect
             * the brightness at that coordinate.
             *
             * Example:
             *     Event(5, +1)
             *     Event(5, +1)
             *     Event(5, -1)
             *
             * Net change at coordinate 5 = +1.
             *
             * We accumulate all three deltas, so the brightness measured
             * at 5 is the correct total.
             */
            while (i < events.length
                    && events[i].coordinate() == coordinate) {

                currentBrightness += events[i].delta();
                i++;
            }

            /*
             * WHY IS currentBrightness CORRECT HERE?
             *
             * We have processed every event at this coordinate.
             * Each light contributes +1 at its left endpoint and -1 just
             * after its right endpoint.
             *
             * Consequently, the cumulative sum equals the number of lights
             * illuminating this coordinate.
             *
             * Brightness remains constant until the next event coordinate.
             * Therefore, checking the current coordinate is enough: if this
             * segment has the maximum brightness, its leftmost coordinate
             * is the first event coordinate of that segment.
             */

            /*
             * WHY USE STRICTLY GREATER THAN (>) INSTEAD OF >=?
             *
             * We process coordinates in ascending order.
             *
             * If a coordinate has brightness greater than every previous
             * coordinate, it becomes the new answer.
             *
             * If a later coordinate has the SAME maximum brightness, we
             * must preserve the earlier answer because the problem requests
             * the smallest coordinate among all maximum-brightness positions.
             *
             * Example:
             *     Position 3 -> brightness 4
             *     Position 5 -> brightness 4
             *
             * The answer must remain 3.
             */
            if (currentBrightness > maxBrightness) {
                maxBrightness = currentBrightness;
                answer = coordinate;
            }
        }

        /*
         * The official problem constraints guarantee that the answer fits
         * in int, which is the required return type.
         *
         * If a generalized version allowed answers outside the int range,
         * the method's return type would need to be long.
         */
        return (int) answer;
    }

    /*
     * CORRECTNESS ARGUMENT
     *
     * 1. Every light contributes +1 at its left endpoint and -1 immediately
     *    after its inclusive right endpoint.
     *
     * 2. Thus, after processing all events at coordinate x, the cumulative
     *    sum equals the true brightness at x.
     *
     * 3. Brightness cannot change between consecutive event coordinates,
     *    so no unprocessed coordinate can introduce a new brightness value.
     *
     * 4. Since we visit event coordinates in ascending order and update the
     *    answer only on a STRICT increase, the final answer is the smallest
     *    coordinate attaining the global maximum brightness.
     *
     * TIME COMPLEXITY:
     *     Creating events: O(n)
     *     Sorting 2n events: O(n log n)
     *     Sweeping all events: O(n)
     *
     *     Total: O(n log n)
     *
     * AUXILIARY SPACE:
     *     The event array contains exactly 2n entries.
     *     Total: O(n), excluding sorting implementation workspace.
     *
     *     The record objects are also O(n) in number.
     */
}










































import java.util.TreeMap;

class Solution {

    public int brightestPosition(int[][] lights) {

        /*
         * CORE IDEA: STORE BRIGHTNESS CHANGES IN A SORTED MAP
         *
         * We use a TreeMap where:
         *
         *     key   = street coordinate
         *     value = net brightness change at that coordinate
         *
         * Example:
         *     events[2] = +2
         *     events[5] = -1
         *     events[8] = -1
         *
         * This means:
         *     At coordinate 2, brightness increases by 2.
         *     At coordinate 5, brightness decreases by 1.
         *     At coordinate 8, brightness decreases by 1.
         *
         * WHY TREEMAP?
         *
         * 1. Its keys are automatically maintained in ascending order.
         * 2. We do not need to create a separate event array and sort it.
         * 3. If multiple lights produce events at the same coordinate,
         *    merge() combines their deltas into a single net change.
         *
         * TreeMap uses a balanced search tree, so inserting or updating
         * an event takes O(log m), where m is the number of distinct keys.
         */
        TreeMap<Long, Integer> events = new TreeMap<>();

        for (int[] light : lights) {

            // Convert to long before performing endpoint arithmetic.
            long position = light[0];
            long range = light[1];

            long left = position - range;
            long right = position + range;

            /*
             * A light illuminates every integer coordinate in [left, right].
             *
             * At left:
             *     Add 1 because this light begins contributing.
             *
             * At right + 1:
             *     Subtract 1 because this light no longer contributes.
             *
             * Example:
             *     Interval = [3, 7]
             *
             *     events[3] += 1
             *     events[8] -= 1
             *
             * This ensures the light contributes at 3, 4, 5, 6 and 7,
             * but not at 8.
             */

            /*
             * merge(key, value, Integer::sum):
             *
             * If the coordinate does not exist, insert the supplied delta.
             * If it already exists, add the new delta to its existing value.
             *
             * For example, if three lights start at coordinate 4:
             *     events[4] becomes +3.
             *
             * If one light ends immediately before coordinate 4:
             *     its -1 event is also combined at coordinate 4.
             *
             * The resulting map value is the NET change at that coordinate.
             */
            events.merge(left, +1, Integer::sum);
            events.merge(right + 1L, -1, Integer::sum);
        }

        /*
         * Initialize the sweep state.
         *
         * currentBrightness:
         *     Running sum of all event deltas processed so far.
         *
         * maxBrightness:
         *     Largest brightness observed at an event coordinate.
         *
         * answer:
         *     Leftmost coordinate at which that maximum occurs.
         *
         * We initialize maxBrightness to -1 so that the first event
         * establishes the answer. Every actual brightness is nonnegative.
         */
        int currentBrightness = 0;
        int maxBrightness = -1;
        long answer = 0;

        /*
         * TreeMap.entrySet() iterates over entries in ascending key order.
         *
         * This gives us the same left-to-right sweep as the sorted-array
         * solution without explicitly sorting an event array.
         */
        for (var event : events.entrySet()) {

            long coordinate = event.getKey();
            int delta = event.getValue();

            /*
             * Apply the NET brightness change at this coordinate.
             *
             * Because merge() combined all events at a coordinate,
             * a single addition is sufficient.
             *
             * Example:
             *     events[10] = +2
             *
             * If currentBrightness was 3 before coordinate 10:
             *     currentBrightness becomes 5.
             *
             * The result is the actual brightness at coordinate 10.
             */
            currentBrightness += delta;

            /*
             * WHY IS THE CUMULATIVE SUM THE BRIGHTNESS?
             *
             * Every light contributes exactly:
             *     +1 when its interval begins
             *     -1 immediately after its interval ends
             *
             * Before the first event, no light contributes.
             * Every subsequent event adjusts the number of active lights.
             *
             * Therefore, after applying the net delta at coordinate x,
             * currentBrightness equals the number of lights covering x.
             */

            /*
             * UPDATE THE ANSWER ONLY ON A STRICT INCREASE.
             *
             * Since TreeMap visits coordinates in ascending order,
             * the first coordinate that achieves a particular maximum
             * is necessarily the smallest coordinate achieving it.
             *
             * If a later coordinate has the same brightness, do nothing:
             * the earlier coordinate is already the correct answer.
             *
             * Using >= here would be WRONG because it could replace the
             * leftmost answer with a later coordinate having equal brightness.
             */
            if (currentBrightness > maxBrightness) {
                maxBrightness = currentBrightness;
                answer = coordinate;
            }
        }

        /*
         * Return the leftmost position with the highest brightness.
         * The problem guarantees that this coordinate fits in int.
         */
        return (int) answer;
    }

    /*
     * CORRECTNESS ARGUMENT
     *
     * 1. Every light is translated into the correct pair of difference events.
     *
     * 2. TreeMap stores each coordinate's net change and visits coordinates
     *    in increasing order.
     *
     * 3. The cumulative sum after applying each net change is the true
     *    brightness at that coordinate.
     *
     * 4. Brightness stays constant between event coordinates. Therefore,
     *    the global maximum must be observable at one of the event coordinates.
     *
     * 5. The answer changes only when brightness strictly increases.
     *    Because coordinates are processed from smallest to largest, ties
     *    retain the smallest coordinate.
     *
     * TIME COMPLEXITY:
     *     There are 2n event insertions or updates.
     *     Each TreeMap operation costs O(log m), where m <= 2n.
     *     Building the map: O(n log n).
     *     Iterating through its m entries: O(n).
     *
     *     Total: O(n log n).
     *
     * AUXILIARY SPACE:
     *     At most 2n distinct event coordinates are stored.
     *     Total: O(n).
     */
}

