/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design a Range Module data structure that effectively tracks half-open 
 * intervals [left, right) and allows querying these ranges. 
 * 
 * Implement the RangeModule class:
 * - Constructor(): Initializes the data structure.
 * - Add Range(left, right): Adds the interval [left, right). Overlapping 
 *   intervals should be merged.
 * - Query Range(left, right): Returns true if EVERY real number in [left, right) 
 *   is currently being tracked.
 * - Remove Range(left, right): Removes tracking for the interval [left, right), 
 *   splitting or truncating existing tracked ranges as necessary.
 * 
 * Constraints: 1 <= left < right <= 10^4. At most 10^3 calls.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Are the coordinates strictly bounded to $10^4$ as stated, or should the design scale?
 *    (A boolean array is perfectly fine for $10^4$, but fails if ranges scale to $10^9$. A TreeMap scales infinitely).
 * 2. Does "real number" imply continuous space rather than discrete integers?
 *    (Yes. Tracking `[1, 2)` and `[2, 3)` means $2.5$ is tracked. Disjoint integers like `1, 2` are not enough; we must track bounds).
 * 3. Are `left` and `right` guaranteed to be valid where `left < right`?
 *    (Yes, the constraints guarantee it, saving us from validation boilerplate).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: Track contiguous blocks of space dynamically. The binding 
 *   constraint is that intervals merge and split, changing the total number of 
 *   tracked segments unpredictably.
 * 
 * --- Approach 1: Brute Force Boolean Array ---
 * 1. Intuition: Since maximum `right` is $10^4$, we can just use an array of booleans.
 *    Index `i` represents the continuous interval `[i, i+1)`.
 * 2. Try: `boolean[] tracked = new boolean[10001]`. 
 *    `addRange(left, right)`: set array values from `left` to `right - 1` to true.
 *    `queryRange(left, right)`: loop from `left` to `right - 1`. If any is false, return false.
 * 3. Why it works: Half-open intervals map perfectly to discrete array indices (e.g., `[1, 3)` modifies index 1 and 2).
 * 4. Why it's too costly (at scale): The time complexity scales with the physical distance 
 *    between `left` and `right`, not the number of operations. If constraints were up to $10^9$, 
 *    this would immediately result in an Out Of Memory error and Time Limit Exceeded.
 * 5. Bottleneck: Representing continuous space with discrete, linear memory blocks.
 * 
 * Time Complexity: O(R) per operation where R is the size of the range.
 * Space Complexity: O(M) where M is the maximum coordinate ($10^4$).
 * 
 * --- Approach 2: Balanced BST (TreeMap of Disjoint Intervals) [OPTIMAL] ---
 * 1. Intuition: Instead of storing every point, just store the boundaries. Keep a collection 
 *    of disjoint (non-overlapping) intervals sorted by their start times.
 * 2. Try: Use a `TreeMap<Integer, Integer>` where `Key = left`, `Value = right`.
 *    - `Add`: Find the interval that starts just before our new `left`. If it overlaps, 
 *      merge them. Then look at all subsequent intervals that start before our new `right`. 
 *      Swallow them, expanding our `right` bound.
 *    - `Remove`: Find overlapping intervals. If an interval completely engulfs our removal 
 *      range, chop its middle out, resulting in two separate intervals.
 *    - `Query`: Find the interval that starts just before or exactly at `left`. Check if 
 *      its `right` extends far enough to cover our query `right`.
 * 3. Why it works: Red-Black Trees (TreeMap) provide O(log N) operations to find the nearest 
 *    boundaries (`floorKey`, `ceilingKey`), skipping the empty space completely.
 * 4. Which to write in a 45-min interview?
 *    Approach 2. Even though Approach 1 technically passes the $10^4$ constraint, interviewers 
 *    use this problem to test your ability to handle $10^9$ coordinate geometry using BSTs.
 * 
 * Time Complexity: 
 * - Add/Remove: Amortized O(log N) — because while we might delete K overlapping intervals in a single operation, each interval is added and deleted at most once over its lifetime.
 * - Query: O(log N) — a single `floorKey` lookup.
 * Space Complexity: O(N) — storing N disjoint intervals in the tree.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Exact Boundary Touching: Adding `[10, 15)` and `[15, 20)`. The `floorKey` logic must 
 *   check `map.get(floor) >= left` to ensure `15 >= 15` triggers a seamless merge into `[10, 20)`.
 * - The Encompassing Split: Removing `[15, 20)` from `[10, 30)`. This splits a single interval 
 *   into two: `[10, 15)` and `[20, 30)`.
 * - Removing Exact Matches: If we remove `[10, 15)` and there's an interval `[10, 30)`, we must 
 *   truncate it to `[15, 30)` but ALSO remove the original key `10` from the map entirely.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Continuous coverage depends entirely on the disjoint invariant." 
 *   If the map guarantees that no two intervals overlap and all adjacent intervals are merged, 
 *   then querying `[left, right)` requires exactly ONE check: does the single interval starting 
 *   before `left` extend past `right`?
 * 
 * ASCII DRY RUN (Add Range Merge Loop)
 * Map: `[10, 15)`, `[20, 25)`, `[30, 35)`
 * Call: `addRange(12, 32)`
 * 
 * Step 1: `floorKey(12)` is 10. `get(10)` is 15. 
 *         15 >= 12 (Overlaps!). Expand new bounds: left=10, right=max(32, 15)=32.
 *         Remove 10.
 * 
 * Step 2: `ceilingKey(10)` is 20. 
 *         20 <= 32 (Overlaps!). Expand new bounds: right=max(32, 25)=32.
 *         Remove 20.
 * 
 * Step 3: `ceilingKey(10)` is 30.
 *         30 <= 32 (Overlaps!). Expand new bounds: right=max(32, 35)=35.
 *         Remove 30.
 * 
 * Step 4: `ceilingKey(10)` is null. Loop ends.
 *         Put final merged interval: `[10, 35)`.
 * 
 * Pitfalls:
 * - `ConcurrentModificationException`: Modifying a TreeMap while iterating over its `values()` 
 *   or a `subMap` will crash Java. The safest pattern is a `while` loop repeatedly querying 
 *   `ceilingKey()` and removing it instantly.
 * 
 * Pattern Recognition: "Dynamic overlapping intervals" -> TreeMap of Disjoint Segments.
 * 
 * Interview Script snippet:
 * "Since we're tracking a continuous number line, a boolean array wastes memory and won't 
 * scale to large coordinate bounds. Instead, I'll maintain a TreeMap of disjoint, sorted intervals 
 * mapping left bounds to right bounds. When adding or removing, I'll use `floorKey` to handle 
 * the left-side overlap, and a `ceilingKey` loop to merge or delete subsequent subsumed intervals, 
 * maintaining the disjoint invariant for O(log N) querying."
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: How would you track the total length of all covered ranges?
 * A1: I would maintain a `totalLength` instance variable. During `addRange`, when we merge 
 *     or remove intervals, I subtract their length from `totalLength`. Finally, I add the 
 *     length of the newly merged `[left, right)` interval. Same logic in reverse for `removeRange`.
 * 
 * Q2: What if we needed to support querying the total covered length *within* a specific window?
 * A2: The TreeMap would take O(K) where K is the number of intervals in the window. To get O(log N) 
 *     queries for dynamic range sums, we would need to upgrade from a TreeMap to a custom 
 *     Dynamic Segment Tree.
 */

import java.util.TreeMap;

public class RangeModuleStudy {

    static class RangeModule {
        // Core data structure: Key = Left bound, Value = Right bound.
        // Invariant: All intervals stored are strictly disjoint and merged if touching.
        private TreeMap<Integer, Integer> map;

        /**
         * Initializes a new instance of the data structure.
         */
        public RangeModule() {
            map = new TreeMap<>();
        }

        /**
         * Adds the half-open interval [left, right) to the tracked ranges.
         * Merges overlapping or touching ranges.
         * Time: Amortized O(log N).
         */
        public void addRange(int left, int right) {
            // 1. Check if the interval immediately preceding or encompassing 'left' overlaps.
            Integer floor = map.floorKey(left);
            
            // If it exists and its right bound touches or overlaps our left bound
            if (floor != null && map.get(floor) >= left) {
                left = floor; // Expand our left bound to encompass it
                right = Math.max(right, map.get(floor)); // Expand right if necessary
                map.remove(floor); // Remove the old disjoint interval
            }
            
            // 2. Swallow any subsequent intervals that start before our new 'right' bound.
            // Using a while loop with ceilingKey prevents ConcurrentModificationException.
            Integer curr = map.ceilingKey(left);
            while (curr != null && curr <= right) {
                // Expand our right bound to encompass this interval
                right = Math.max(right, map.get(curr));
                map.remove(curr);
                // Fetch the next one
                curr = map.ceilingKey(left);
            }
            
            // 3. Insert the final perfectly merged interval
            map.put(left, right);
        }

        /**
         * Returns true if every real number within [left, right) is tracked.
         * Time: O(log N).
         */
        public boolean queryRange(int left, int right) {
            // Because intervals are perfectly merged, we only need to check ONE interval:
            // the one that starts exactly at or just before our query's left bound.
            Integer floor = map.floorKey(left);
            
            // If it exists, does its right bound stretch far enough to cover our query?
            return floor != null && map.get(floor) >= right;
        }

        /**
         * Removes tracking for every real number within [left, right).
         * Splits or truncates existing intervals to maintain the invariant.
         * Time: Amortized O(log N).
         */
        public void removeRange(int left, int right) {
            // 1. Handle the interval that starts before our removal range, but overlaps into it.
            Integer floor = map.floorKey(left);
            if (floor != null && map.get(floor) > left) {
                int end = map.get(floor);
                
                // Truncate it so it stops strictly at 'left'
                if (floor < left) {
                    map.put(floor, left);
                } else {
                    // If the interval started exactly at 'left', removing the prefix completely deletes it
                    map.remove(floor);
                }
                
                // If the interval stretched completely PAST our removal range, we must split it!
                if (end > right) {
                    map.put(right, end);
                    // Since intervals are disjoint, no other interval can overlap our removal range.
                    return; 
                }
            }
            
            // 2. Destroy or truncate intervals that START strictly inside our removal range.
            Integer curr = map.ceilingKey(left);
            while (curr != null && curr < right) {
                int end = map.get(curr);
                map.remove(curr); // This interval starts in a dead zone, destroy it
                
                // If its tail extends beyond our removal zone, preserve the tail and stop 
                // (because disjoint invariant means nothing else can overlap further down).
                if (end > right) {
                    map.put(right, end);
                    break;
                }
                curr = map.ceilingKey(left);
            }
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        RangeModule rm = new RangeModule();
        
        // 1. Add [10, 20)
        rm.addRange(10, 20);
        System.out.println("Query [10, 14): " + rm.queryRange(10, 14)); // true
        System.out.println("Query [13, 15): " + rm.queryRange(13, 15)); // true
        System.out.println("Query [16, 22): " + rm.queryRange(16, 22)); // false (stops at 20)
        
        // 2. Remove [14, 16) - The Encompassing Split
        // Tracked becomes: [10, 14) and [16, 20)
        rm.removeRange(14, 16);
        System.out.println("Query [10, 14) after split: " + rm.queryRange(10, 14)); // true
        System.out.println("Query [13, 15) after split: " + rm.queryRange(13, 15)); // false
        System.out.println("Query [16, 20) after split: " + rm.queryRange(16, 20)); // true
        
        // 3. Multi-Merge Add
        // Adding [12, 18) bridges the gap and merges everything back to [10, 20)
        rm.addRange(12, 18);
        System.out.println("Query [10, 20) after bridging: " + rm.queryRange(10, 20)); // true
        
        // 4. Multi-Remove 
        // Tracked: [10, 20). Remove [5, 25).
        rm.removeRange(5, 25);
        System.out.println("Query [10, 20) after nuke: " + rm.queryRange(10, 20)); // false
    }
}

















import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

class RangeModule {

    /*
     * OUR REPRESENTATION
     *
     * Each TreeMap entry represents one tracked interval:
     *
     *     key   = interval's inclusive start
     *     value = interval's exclusive end
     *
     * Example:
     *
     *     ranges.put(2, 8);
     *     ranges.put(12, 18);
     *     ranges.put(25, 30);
     *
     * Represents:
     *
     *     [2, 8)    [12, 18)    [25, 30)
     *
     * IMPORTANT INVARIANT:
     *
     * Intervals are sorted, non-overlapping, and non-adjacent.
     *
     * For example, we never store both [2, 8) and [8, 12).
     * Those intervals would be merged into [2, 12).
     *
     * Why maintain this invariant?
     *
     * 1. Addition only needs to merge affected intervals.
     * 2. A query can find coverage with one predecessor lookup.
     * 3. Removal only needs to process intersecting intervals.
     */
    private final TreeMap<Integer, Integer> ranges = new TreeMap<>();

    /*
     * Constructor
     *
     * Initially, nothing is tracked, so the map is empty.
     *
     * Time:  O(1)
     * Space: O(1)
     */
    public RangeModule() {
    }

    /*
     * ADD RANGE
     *
     * Add every point in [left, right) to the tracked set.
     *
     * CORE IDEA:
     *
     * Imagine drawing the new interval on the number line.
     * Every existing interval that overlaps or touches it must
     * become part of one larger interval.
     *
     * Example:
     *
     * Existing:
     *     [2, 8)    [12, 18)    [25, 30)
     *
     * Add:
     *                    [14, 27)
     *
     * First, [14, 27) merges with [12, 18):
     *     [12, 27)
     *
     * That expanded interval then merges with [25, 30):
     *     [12, 30)
     *
     * Final:
     *     [2, 8)    [12, 30)
     *
     * WHY CAN'T WE JUST INSERT [14, 27)?
     *
     * Because that would leave overlapping intervals in the map.
     * Our query algorithm relies on the invariant that intervals
     * are already merged and disjoint.
     *
     * HOW WE FIND THE INTERVALS TO MERGE:
     *
     * floorEntry(start) finds the interval starting at or before
     * the new interval's left endpoint.
     *
     * If that interval ends at or after start, it overlaps or
     * touches the new interval. We move start backward to its start.
     *
     * Then ceilingEntry(start) finds the first interval whose start
     * is at least the expanded start.
     *
     * We keep visiting intervals while their starts are <= end.
     * Notice that end may grow during the loop!
     *
     * We collect keys first and remove them after traversal so that
     * successor navigation is easy to follow.
     */
    public void addRange(int left, int right) {

        // The interval we are building begins as the requested range.
        int start = left;
        int end = right;

        // Store the keys of old intervals that will be absorbed.
        // We delete them after identifying every interval to merge.
        var keysToRemove = new ArrayList<Integer>();

        /*
         * STEP 1: Look for an interval that begins before or at left.
         *
         * Example:
         *
         * Existing: [12, 18)
         * New:         [14, 27)
         *
         * floorEntry(14) returns 12 -> 18.
         *
         * Since 18 >= 14, the existing interval overlaps the new one.
         * We must include its entire range in the merged result.
         */
        Map.Entry<Integer, Integer> previous = ranges.floorEntry(start);

        if (previous != null && previous.getValue() >= start) {

            // The merged interval must begin where the old interval begins.
            start = previous.getKey();

            // Keep whichever interval has the larger right endpoint.
            end = Math.max(end, previous.getValue());

            // The predecessor will be merged into the new interval.
            keysToRemove.add(previous.getKey());
        }

        /*
         * STEP 2: Find the first interval at or after the expanded start.
         *
         * ceilingEntry(start) returns the smallest key >= start.
         *
         * If we found an overlapping predecessor in STEP 1, this
         * lookup may return that same interval. That is intentional:
         * it will be collected for removal by the loop below.
         */
        Map.Entry<Integer, Integer> current = ranges.ceilingEntry(start);

        /*
         * STEP 3: Merge every interval that overlaps or touches.
         *
         * Why current.getKey() <= end?
         *
         * An interval starting before end overlaps the new interval.
         * An interval starting exactly at end is adjacent:
         *
         *     [10, 20) and [20, 30)
         *
         * Their union is [10, 30), so we merge adjacency too.
         *
         * Why might this loop need multiple iterations?
         *
         * Merging one interval can increase end, which may make
         * another previously distant interval reachable.
         */
        while (current != null && current.getKey() <= end) {

            // Extend the merged interval to include the current interval.
            end = Math.max(end, current.getValue());

            // Remember this interval's key so we can delete it later.
            keysToRemove.add(current.getKey());

            // Move to the next interval in sorted order.
            current = ranges.higherEntry(current.getKey());
        }

        /*
         * STEP 4: Remove the old intervals that we merged.
         *
         * Removing by key is O(log n) per entry in a TreeMap.
         * Duplicate keys are harmless if a key was collected twice.
         */
        for (int key : keysToRemove) {
            ranges.remove(key);
        }

        /*
         * STEP 5: Insert the single merged interval.
         *
         * The map is now back to our invariant:
         * sorted, disjoint, non-adjacent intervals.
         */
        ranges.put(start, end);
    }

    /*
     * QUERY RANGE
     *
     * Return true if every point in [left, right) is tracked.
     *
     * CORE IDEA:
     *
     * Because intervals are merged, a query must fit completely
     * inside ONE stored interval.
     *
     * Example:
     *
     * Stored: [2, 8)    [12, 18)
     *
     * Query [13, 17):
     *     true, because [13, 17) is inside [12, 18).
     *
     * Query [13, 19):
     *     false, because the stored interval ends at 18.
     *
     * Query [8, 12):
     *     false, because this range is not tracked.
     *
     * floorEntry(left) finds the interval with the greatest start
     * that is <= left. If no such interval exists, left is uncovered.
     *
     * If an interval is found, it covers the whole query exactly when
     *
     *     interval.start <= left
     *     AND
     *     right <= interval.end
     *
     * The first condition is already guaranteed by floorEntry(left).
     * Therefore, we only need to check interval.end >= right.
     *
     * Time:  O(log n)
     * Space: O(1)
     */
    public boolean queryRange(int left, int right) {

        // Find the interval that starts closest to left without passing it.
        Map.Entry<Integer, Integer> interval = ranges.floorEntry(left);

        // No interval starts at or before left: left is not covered.
        if (interval == null) {
            return false;
        }

        // The interval must extend to the query's exclusive right endpoint.
        // If it ends earlier, the query contains an uncovered portion.
        return interval.getValue() >= right;
    }

    /*
     * REMOVE RANGE
     *
     * Stop tracking every point in [left, right).
     *
     * CORE IDEA:
     *
     * Find all intervals that intersect the removal range.
     * Delete each affected interval.
     * Restore any portions that lie outside the removal range.
     *
     * Example:
     *
     * Existing: [10, 30)
     * Remove:        [16, 22)
     *
     * Left portion survives:
     *     [10, 16)
     *
     * Right portion survives:
     *     [22, 30)
     *
     * Final:
     *     [10, 16)    [22, 30)
     *
     * The removal can split one interval into two, which is why
     * we must preserve both sides independently.
     *
     * TWO INTERVALS OVERLAP WHEN:
     *
     *     existingStart < right && left < existingEnd
     *
     * Strict inequalities are important for half-open intervals.
     *
     * For example, [10, 15) and [15, 20) do not overlap.
     *
     * Time:  O((k + 1) log n), where k is the number of affected intervals.
     * Space: O(k), for the keys and surviving interval pieces.
     */
    public void removeRange(int left, int right) {

        // Keys of intervals that intersect the removal range.
        var keysToRemove = new ArrayList<Integer>();

        // Surviving left and right pieces of affected intervals.
        var piecesToKeep = new ArrayList<Interval>();

        /*
         * STEP 1: Find where to begin.
         *
         * floorEntry(left) may return an interval that extends across left.
         *
         * Example:
         *     Existing: [10, 30)
         *     Remove:       [16, 22)
         *
         * floorEntry(16) returns 10 -> 30.
         * We must process it because it overlaps the removal range.
         *
         * But if the predecessor ends at or before left, it cannot overlap.
         * In that case, start from ceilingEntry(left), the first interval
         * whose start is at or after left.
         */
        Map.Entry<Integer, Integer> current = ranges.floorEntry(left);

        if (current == null || current.getValue() <= left) {
            current = ranges.ceilingEntry(left);
        }

        /*
         * STEP 2: Visit intervals that start before right.
         *
         * Once an interval starts at or beyond right, it cannot overlap
         * the removal range, so we can stop.
         */
        while (current != null && current.getKey() < right) {

            int start = current.getKey();
            int end = current.getValue();

            /*
             * Does this interval actually overlap the removal range?
             *
             * Because the intervals are sorted, only the first candidate
             * could end at or before left. Later candidates start at or
             * after left and are considered only if start < right.
             *
             * The end > left condition explicitly checks for intersection.
             */
            if (end > left) {

                // This entire old interval must be removed before rebuilding.
                keysToRemove.add(start);

                /*
                 * CASE 1: A left portion survives.
                 *
                 * Existing: [10, 30)
                 * Remove:       [16, 22)
                 *
                 * Since 10 < 16, preserve [10, 16).
                 *
                 * If start == left, there is no left portion to preserve.
                 */
                if (start < left) {
                    piecesToKeep.add(new Interval(start, left));
                }

                /*
                 * CASE 2: A right portion survives.
                 *
                 * Since 30 > 22, preserve [22, 30).
                 *
                 * If end == right, there is no right portion to preserve.
                 */
                if (end > right) {
                    piecesToKeep.add(new Interval(right, end));
                }

                /*
                 * If neither condition is true, the old interval lies
                 * entirely inside the removal range and disappears.
                 *
                 * If both are true, the removal splits it into two.
                 */
            }

            // Save the next entry before modifying the map.
            current = ranges.higherEntry(start);
        }

        /*
         * STEP 3: Delete every affected original interval.
         *
         * We delayed deletion until traversal was complete so that
         * higherEntry() could safely find the next entry.
         */
        for (int key : keysToRemove) {
            ranges.remove(key);
        }

        /*
         * STEP 4: Restore surviving pieces.
         *
         * The pieces were created from disjoint intervals, so they cannot
         * overlap each other or any unaffected interval.
         */
        for (Interval piece : piecesToKeep) {
            ranges.put(piece.start(), piece.end());
        }
    }

    /*
     * A Java record provides a concise immutable pair for an interval.
     *
     * Example:
     *     new Interval(10, 16) represents [10, 16).
     *
     * It is used only as temporary storage while removing intervals.
     */
    private record Interval(int start, int end) {}
}





