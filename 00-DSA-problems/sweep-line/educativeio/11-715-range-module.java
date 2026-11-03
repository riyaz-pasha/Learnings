/**
 * 715. Range Module
 * 
 * ============================================================================
 * 1. PROBLEM STATEMENT
 * ============================================================================
 * A Range Module is a data structure that tracks ranges of numbers, represented 
 * as half-open intervals [left, right).
 * 
 * Implement the RangeModule class:
 * - RangeModule() Initializes the object.
 * - void addRange(int left, int right): Adds the half-open interval [left, right), 
 *   tracking every real number in that interval. Overlapping/touching intervals 
 *   must be merged.
 * - boolean queryRange(int left, int right): Returns true if EVERY real number 
 *   in the interval [left, right) is currently being tracked.
 * - void removeRange(int left, int right): Stops tracking every real number 
 *   in the half-open interval [left, right).
 * 
 * Constraints:
 * - 1 <= left < right <= 10^9
 * - At most 10^4 calls will be made in total.
 * 
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (To ask the interviewer)
 * ============================================================================
 * 1. What happens if two intervals exactly touch? (e.g., [10, 15) and [15, 20))
 *    (They MUST be merged into [10, 20). If not merged, querying [10, 20) would 
 *    falsely return false because our code might see them as separate blocks).
 * 2. What happens if we remove a chunk from the exact middle of an interval?
 *    (It splits the existing interval into two separate intervals. e.g., 
 *    removing [14, 16) from [10, 20) creates [10, 14) and [16, 20)).
 * 3. Can the coordinate range go up to 10^9? 
 *    (Yes, which strictly rules out flat boolean arrays or fixed-size Line Sweeps).
 * 
 * ============================================================================
 * 3. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement & Constraint: 
 * We need to maintain a set of disjoint intervals, with the ability to merge them, 
 * split them, and query for complete envelopment. The coordinate space is massive (10^9).
 * 
 * --- Approach 1: List of Disjoint Intervals (Brute Force) ---
 * 1. Idea: Maintain an `ArrayList<int[]>` of disjoint, sorted intervals. 
 * 2. What I'd naturally try: For `addRange`, iterate through the list, find overlaps, 
 *    merge them into one giant interval, remove the old ones, and insert the new one.
 * 3. Why it works: It perfectly captures the disjoint nature of the tracked ranges.
 * 4. Why it's too slow/costly: `ArrayList` insertions and deletions take O(N) time 
 *    because they require shifting elements. With 10^4 operations, O(N^2) could pass, 
 *    but it's inefficient and clumsy to write safely.
 * 
 * --- Approach 2: TreeMap (Optimal & Expected) ---
 * 1. Idea: Use a `TreeMap<Integer, Integer>` where `key = left` and `value = right`. 
 *    TreeMaps automatically keep keys sorted and allow O(log N) lookups for the 
 *    nearest intervals.
 * 2. What I'd naturally try: 
 *    - Query: Use `floorKey(left)` to find the interval starting just before or at `left`. 
 *      If its `right >= query_right`, the range is fully tracked.
 *    - Add: Find all intervals that overlap OR touch the new `[left, right)`. 
 *      Expand `left` and `right` to engulf them, remove them from the map, and put the new one.
 *    - Remove: Find all intervals that overlap `[left, right)`. Remove them. 
 *      If the removed interval extended before `left`, re-insert the left chunk. 
 *      If it extended after `right`, re-insert the right chunk.
 * 3. Why it works: It mathematically guarantees intervals stay disjoint and fully merged.
 * 4. Why this is the ceiling: Operations are amortized O(log N). Although an `addRange` 
 *    might delete K overlapping intervals, those K intervals were added exactly once. 
 *    Thus, across all queries, the time spent deleting is bounded by the time spent inserting.
 * 
 * Verdict for 45-minute interview:
 * Write Approach 2 (TreeMap). It tests deep understanding of binary search tree APIs 
 * (floorKey, ceilingKey/higherKey) and edge-case heavy interval math.
 * 
 * ============================================================================
 * 4. EDGE CASES
 * ============================================================================
 * - Touching bounds on ADD: `[10, 15)` + `[15, 20)`. 15 == 15. MUST merge!
 * - Touching bounds on REMOVE: `[10, 15)` - `[15, 20)`. 15 == 15. MUST NOT overlap!
 * - Full Engulfment on REMOVE: `[10, 20)` - `[5, 25)`. The interval is completely destroyed.
 * - Internal Split on REMOVE: `[10, 20)` - `[14, 16)`. Becomes `[10, 14)` and `[16, 20)`.
 * 
 * ============================================================================
 * 5. KEY INSIGHT, DIAGRAM & DRY RUN
 * ============================================================================
 * > Key Insight: 
 * "Add includes touching bounds. Remove strictly excludes them."
 * When adding `[left, right)`, an existing interval `[start, end)` overlaps if 
 * `start <= right` and `end >= left`. (Notice the `<=` and `>=`).
 * When removing, an existing interval overlaps if `start < right` and `end > left`.
 * 
 * Diagram: Internal Split on Remove
 * Tracking: [10 ........................ 20)
 * Remove:               [14 .. 16)
 * 
 * 1. Find overlapping: floorKey(14) -> 10. End is 20. Overlaps!
 * 2. Remove [10, 20) from map.
 * 3. Does 10 < 14? Yes. Add [10, 14).
 * 4. Does 20 > 16? Yes. Add [16, 20).
 * Final Tracking: [10, 14) and [16, 20)
 * 
 * Dry Run:
 * 1. addRange(10, 20):
 *    Map is empty. Puts {10: 20}.
 * 2. removeRange(14, 16):
 *    floorKey(14) is 10. Overlaps!
 *    Removes 10. Re-adds {10: 14} and {16: 20}. Map: {10: 14, 16: 20}.
 * 3. queryRange(10, 14):
 *    floorKey(10) is 10. Value is 14. Is 14 >= 14? TRUE.
 * 4. queryRange(13, 15):
 *    floorKey(13) is 10. Value is 14. Is 14 >= 15? FALSE.
 * 
 * ============================================================================
 * 6. PITFALLS & PATTERN RECOGNITION
 * ============================================================================
 * Pitfalls:
 * - ConcurrentModificationException: If you do `map.remove(key)` while iterating 
 *   over `map.keySet()`, Java will crash. You MUST either use `higherKey()` 
 *   dynamically to get the next key, or collect keys to delete in a `List` and 
 *   delete them after the loop.
 * - Using standard binary search arrays: Sorting arrays takes O(N log N) every 
 *   time an element is added. The TreeMap is required to keep insertion O(log N).
 * 
 * Pattern Recognition: 
 * "Dynamic Interval Management -> TreeMap of Disjoint Intervals."
 * Same pattern as: Data Stream as Disjoint Intervals, My Calendar I.
 * 
 * ============================================================================
 * 7. INTERVIEW SCRIPT
 * ============================================================================
 * "Because the coordinates range up to 10^9, we cannot use an array. We need a 
 * data structure that dynamically maintains a set of disjoint intervals. A TreeMap 
 * is perfect for this. Keys are interval starts, values are interval ends.
 * 
 * For `queryRange`, we just find the `floorKey` of our target left bound. If that 
 * interval's right bound stretches past our target right bound, we're fully covered.
 * 
 * For `addRange`, we find all intervals that overlap or touch our new interval, 
 * merge them by taking the min of the starts and max of the ends, remove the old 
 * ones, and put the unified interval.
 * 
 * For `removeRange`, we find all overlapping intervals, remove them, and if they 
 * stick out past our removal boundaries, we slice them and re-insert the remaining 
 * left or right chunks. This keeps all operations at an amortized O(log N) time."
 * 
 * ============================================================================
 * 8. FOLLOW-UPS
 * ============================================================================
 * Q: What if `queryRange` was called 10 million times, but `add/remove` were rare?
 * A: The current O(log N) lookup is already optimal. But we could serialize the 
 *    TreeMap into a flat array of `int[]` after every modification. Querying a 
 *    flat array via binary search is cache-friendly and faster in practice than 
 *    traversing tree nodes.
 * 
 * Q: What if all operations are called 100,000 times?
 * A: We could use a Dynamic Implicit Segment Tree with Lazy Propagation. It maps 
 *    the massive 10^9 space lazily, turning range updates and queries into strict 
 *    O(log(10^9)) = O(30) operations, completely avoiding the worst-case O(K) 
 *    deletion loops of a TreeMap.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class RangeModule {

    // TreeMap to maintain disjoint intervals. Key = start, Value = end.
    private TreeMap<Integer, Integer> map;

    public RangeModule() {
        map = new TreeMap<>();
    }
    
    /**
     * Time Complexity: Amortized O(log N).
     * While it might remove K intervals, each interval is added exactly once.
     */
    public void addRange(int left, int right) {
        // 1. Find the first possible overlapping/touching interval
        Integer start = map.floorKey(left);
        
        // If the floor interval doesn't touch/overlap our new 'left',
        // start looking from the interval strictly after 'left'.
        // Notice the `<` logic: if map.get(start) == left, they TOUCH, so we keep it to merge!
        if (start == null || map.get(start) < left) {
            start = map.ceilingKey(left);
        }
        
        List<Integer> keysToRemove = new ArrayList<>();
        
        // 2. Iterate through all intervals that overlap or touch [left, right)
        // Notice `<= right`: if start == right, they TOUCH, so we merge!
        while (start != null && start <= right) {
            // Engulf the overlapping interval
            left = Math.min(left, start);
            right = Math.max(right, map.get(start));
            
            // Mark for deletion to avoid ConcurrentModificationException
            keysToRemove.add(start);
            
            // Move to the next interval
            start = map.higherKey(start);
        }
        
        // 3. Apply deletions and insert the unified interval
        for (int k : keysToRemove) {
            map.remove(k);
        }
        map.put(left, right);
    }
    
    /**
     * Time Complexity: O(log N)
     * strictly one binary search (floorKey) lookup.
     */
    public boolean queryRange(int left, int right) {
        // Find the single interval that starts at or before 'left'
        Integer start = map.floorKey(left);
        
        // If it exists, does its end stretch far enough to cover 'right'?
        return start != null && map.get(start) >= right;
    }
    
    /**
     * Time Complexity: Amortized O(log N).
     */
    public void removeRange(int left, int right) {
        // 1. Find the first possible overlapping interval
        Integer start = map.floorKey(left);
        
        // If the floor interval ends at or before 'left', it doesn't overlap.
        // Notice the `<=` logic: if map.get(start) == left, they touch but DO NOT overlap.
        // We only care about strict overlaps for removal!
        if (start == null || map.get(start) <= left) {
            start = map.ceilingKey(left);
        }
        
        List<Integer> keysToRemove = new ArrayList<>();
        List<int[]> rangesToAdd = new ArrayList<>();
        
        // 2. Iterate through all strictly overlapping intervals
        // Notice `< right`: if start == right, they touch but DO NOT overlap.
        while (start != null && start < right) {
            int end = map.get(start);
            keysToRemove.add(start);
            
            // Slicing logic:
            // If the existing interval sticks out to the left of our removal boundary,
            // we keep that left chunk.
            if (start < left) {
                rangesToAdd.add(new int[]{start, left});
            }
            
            // If the existing interval sticks out to the right of our removal boundary,
            // we keep that right chunk.
            if (end > right) {
                rangesToAdd.add(new int[]{right, end});
            }
            
            // Move to the next interval
            start = map.higherKey(start);
        }
        
        // 3. Apply deletions and insertions safely
        for (int k : keysToRemove) {
            map.remove(k);
        }
        for (int[] range : rangesToAdd) {
            map.put(range[0], range[1]);
        }
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================
    public static void main(String[] args) {
        System.out.println("--- Testing RangeModule ---");
        RangeModule rm = new RangeModule();
        
        // Test Case 1: Standard logic from Example 1
        rm.addRange(10, 20);
        rm.removeRange(14, 16);
        System.out.println("queryRange(10, 14) Expected true  | Got: " + rm.queryRange(10, 14));
        System.out.println("queryRange(13, 15) Expected false | Got: " + rm.queryRange(13, 15));
        System.out.println("queryRange(16, 17) Expected true  | Got: " + rm.queryRange(16, 17));
        
        // Test Case 2: Adjacent Additions (Touching bounds)
        rm = new RangeModule();
        rm.addRange(10, 15);
        rm.addRange(15, 20); // Should merge into [10, 20)
        System.out.println("queryRange(10, 20) Expected true  | Got: " + rm.queryRange(10, 20));
        
        // Test Case 3: Complete Engulfing Remove
        rm.removeRange(5, 25); // Erases [10, 20) completely
        System.out.println("queryRange(10, 15) Expected false | Got: " + rm.queryRange(10, 15));
        
        // Test Case 4: Multiple disjoint intervals merging
        rm = new RangeModule();
        rm.addRange(1, 5);
        rm.addRange(10, 15);
        rm.addRange(20, 25);
        rm.addRange(3, 22); // Engulfs and bridges [1, 5), [10, 15), and part of [20, 25)
        // Should result in [1, 25)
        System.out.println("queryRange(2, 24)  Expected true  | Got: " + rm.queryRange(2, 24));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern: Dynamic Disjoint Intervals via TreeMap.
 * 
 * Key observation: 
 * Interval manipulation depends heavily on strict vs loose inequalities.
 * - When ADDING, touching intervals (`end == left` or `start == right`) MUST merge.
 * - When REMOVING, touching intervals DO NOT overlap and must be ignored.
 * 
 * Memorize vs Understand: 
 * UNDERSTAND how `floorKey` and `higherKey` allow you to pinpoint the exact 
 * subset of intervals that interact with your target range, ignoring the rest 
 * of the 10^9 coordinate space.
 * 
 * Most common trap: ConcurrentModificationException. Calling `map.remove(key)` 
 * inside a loop where you are actively requesting `map.higherKey()` can lead to 
 * broken iterators or crashes depending on the exact loop structure. Collecting 
 * keys in a `List` first is the bulletproof interview approach.
 * 
 * One-line mental trigger: "Track disjoint ranges? TreeMap <Start, End> with floorKey overlaps."
 */










































import java.util.Map;
import java.util.TreeMap;

class RangeModule {

    /*
     * DATA STRUCTURE
     *
     * TreeMap<start, end>
     *
     * Example:
     *
     * 10 -> 20
     * 30 -> 40
     * 50 -> 60
     *
     * Represents:
     *
     * [10, 20)
     * [30, 40)
     * [50, 60)
     *
     * INVARIANT:
     *
     * 1. Intervals are sorted by start.
     * 2. Intervals never overlap.
     * 3. Intervals never touch.
     *
     * Therefore, for consecutive intervals:
     *
     * previousEnd < nextStart
     *
     * Why use TreeMap?
     *
     * TreeMap is implemented using a Red-Black Tree.
     * floorEntry(), ceilingEntry(), and put() take O(log N).
     *
     * N = number of currently tracked intervals.
     */
    private final TreeMap<Integer, Integer> intervals;

    public RangeModule() {
        intervals = new TreeMap<>();
    }

    /*
     * ADD RANGE
     *
     * Adds every number in [left, right).
     *
     * Example:
     *
     * Existing:
     * [10, 20) [30, 40) [50, 60)
     *
     * addRange(15, 45)
     *
     * Result:
     * [10, 45) [50, 60)
     *
     * APPROACH:
     *
     * 1. Find an interval that may overlap or touch the new interval
     *    from the left.
     *
     * 2. Merge all intervals that overlap or touch the growing range.
     *
     * 3. Insert the final merged interval.
     */
    public void addRange(int left, int right) {

        // The interval being constructed.
        // It expands whenever we encounter an overlapping interval.
        int mergedStart = left;
        int mergedEnd = right;

        /*
         * STEP 1: Find the interval immediately before or at left.
         *
         * floorEntry(left):
         * Returns the entry with the greatest key <= left.
         *
         * We need to check this interval because it may overlap
         * or touch the new interval from the left.
         */
        Map.Entry<Integer, Integer> entry =
                intervals.floorEntry(left);

        if (entry != null && entry.getValue() >= mergedStart) {

            /*
             * The existing interval overlaps or touches the new interval.
             *
             * Example:
             *
             * Existing: [10, 20)
             * New:           [15, 25)
             *
             * Merge:
             * [10, 25)
             *
             * Equality is included because adjacent intervals
             * should also be merged.
             */
            mergedStart = Math.min(mergedStart, entry.getKey());
            mergedEnd = Math.max(mergedEnd, entry.getValue());

            // Remove the old interval because it is now part
            // of the merged interval.
            intervals.remove(entry.getKey());
        }

        /*
         * STEP 2: Merge all intervals starting inside or at
         * the growing merged interval's right boundary.
         *
         * ceilingEntry(mergedStart):
         * Returns the smallest key >= mergedStart.
         *
         * We repeatedly merge until no more intervals overlap
         * or touch the current merged interval.
         */
        entry = intervals.ceilingEntry(mergedStart);

        while (entry != null && entry.getKey() <= mergedEnd) {

            // Expand the right boundary if necessary.
            mergedEnd = Math.max(mergedEnd, entry.getValue());

            // Remove the old interval before inserting the merged one.
            intervals.remove(entry.getKey());

            /*
             * The right boundary may have expanded.
             * Therefore, retrieve the next candidate using the
             * updated mergedEnd/merged interval.
             */
            entry = intervals.ceilingEntry(mergedStart);
        }

        /*
         * STEP 3: Insert the final merged interval.
         *
         * At this point, no existing interval overlaps or touches it.
         */
        intervals.put(mergedStart, mergedEnd);
    }

    /*
     * QUERY RANGE
     *
     * Returns true if every real number in [left, right)
     * is currently being tracked.
     *
     * APPROACH:
     *
     * Find the interval with the greatest start <= left.
     *
     * If that interval ends at or beyond right,
     * the entire query is covered.
     */
    public boolean queryRange(int left, int right) {

        /*
         * Find the interval that could contain left.
         *
         * floorEntry(left) returns the interval with the greatest
         * starting point that does not exceed left.
         */
        Map.Entry<Integer, Integer> entry =
                intervals.floorEntry(left);

        /*
         * If no such interval exists, left is not tracked.
         *
         * Otherwise, the interval must extend at least until right.
         *
         * end >= right is correct because both intervals are half-open.
         *
         * Example:
         *
         * Tracked: [10, 20)
         * Query:   [12, 20)
         *
         * This is fully covered.
         */
        return entry != null && entry.getValue() >= right;
    }

    /*
     * REMOVE RANGE
     *
     * Removes every tracked number in [left, right).
     *
     * Example:
     *
     * Existing:
     * [10, 30) [40, 60)
     *
     * removeRange(15, 45)
     *
     * Result:
     * [10, 15) [45, 60)
     *
     * APPROACH:
     *
     * 1. Find the first interval that overlaps the removal range.
     *
     * 2. Remove every overlapping interval.
     *
     * 3. Preserve the portions outside the removal range.
     */
    public void removeRange(int left, int right) {

        /*
         * STEP 1: Find the first possible overlapping interval.
         *
         * floorEntry(left) might overlap from the left.
         *
         * If it doesn't overlap, start with ceilingEntry(left).
         */
        Map.Entry<Integer, Integer> entry =
                intervals.floorEntry(left);

        if (entry == null || entry.getValue() <= left) {
            entry = intervals.ceilingEntry(left);
        }

        /*
         * Store the remainders separately.
         *
         * Why?
         *
         * We are deleting intervals while iterating.
         * Collecting the remainders first avoids modifying
         * the map with new intervals during traversal.
         */
        Integer leftRemainderStart = null;
        Integer leftRemainderEnd = null;

        Integer rightRemainderStart = null;
        Integer rightRemainderEnd = null;

        while (entry != null && entry.getKey() < right) {

            int start = entry.getKey();
            int end = entry.getValue();

            /*
             * This interval overlaps [left, right).
             *
             * We know:
             *
             * start < right
             *
             * And because we selected the first possible overlap,
             * its end is > left.
             */

            // Preserve the left part if it exists.
            //
            // Existing: [10, 30)
            // Removal:       [15, 20)
            // Left part: [10, 15)
            if (start < left) {
                leftRemainderStart = start;
                leftRemainderEnd = left;
            }

            // Preserve the right part if it exists.
            //
            // Existing: [10, 30)
            // Removal:       [15, 20)
            // Right part:          [20, 30)
            if (end > right) {
                rightRemainderStart = right;
                rightRemainderEnd = end;
            }

            /*
             * Remove the original interval.
             *
             * If the removal interval is inside an existing interval,
             * this operation effectively splits it into two pieces.
             */
            intervals.remove(start);

            /*
             * If the current interval extends beyond right,
             * no later interval can overlap the removal range.
             *
             * Why?
             *
             * Existing intervals are non-overlapping and sorted.
             */
            if (end >= right) {
                break;
            }

            // Find the next interval whose start is >= current start.
            entry = intervals.ceilingEntry(left);
        }

        /*
         * STEP 2: Insert the preserved remainders.
         *
         * There can be at most:
         *
         * - One left remainder.
         * - One right remainder.
         *
         * All fully covered intervals have been deleted.
         */
        if (leftRemainderStart != null) {
            intervals.put(leftRemainderStart, leftRemainderEnd);
        }

        if (rightRemainderStart != null) {
            intervals.put(rightRemainderStart, rightRemainderEnd);
        }
    }
}









































class RangeModuleSegmentTree {

    private static final int MIN = 1;
    private static final int MAX = 1_000_000_000;

    /*
     * Node states:
     *
     * 0 = Entire segment is untracked.
     * 1 = Entire segment is tracked.
     * 2 = Segment has mixed coverage.
     *
     * lazy:
     *
     * -1 = No pending assignment.
     *  0 = Assign entire segment as untracked.
     *  1 = Assign entire segment as tracked.
     */
    private static class Node {

        Node left;
        Node right;

        int state = 0;
        int lazy = 0;
    }

    private final Node root = new Node();

    public void addRange(int left, int right) {

        // Convert [left, right) to inclusive integer cells.
        update(root, MIN, MAX, left, right - 1, 1);
    }

    public void removeRange(int left, int right) {

        update(root, MIN, MAX, left, right - 1, 0);
    }

    public boolean queryRange(int left, int right) {

        // Every unit cell in [left, right - 1] must be tracked.
        return query(root, MIN, MAX, left, right - 1);
    }

    private void update(
            Node node,
            int start,
            int end,
            int queryStart,
            int queryEnd,
            int value) {

        // No intersection.
        if (queryStart > end || queryEnd < start) {
            return;
        }

        // Full coverage: assign the entire node.
        if (queryStart <= start && end <= queryEnd) {

            node.state = value;
            node.lazy = value;

            // Existing children are no longer needed to represent
            // this uniform segment.
            node.left = null;
            node.right = null;

            return;
        }

        // Create children only when necessary.
        pushDown(node);

        int mid = start + (end - start) / 2;

        if (queryStart <= mid) {
            update(
                    node.left,
                    start,
                    mid,
                    queryStart,
                    queryEnd,
                    value
            );
        }

        if (queryEnd > mid) {
            update(
                    node.right,
                    mid + 1,
                    end,
                    queryStart,
                    queryEnd,
                    value
            );
        }

        // Recalculate the parent's state.
        node.state =
                node.left.state == node.right.state
                        ? node.left.state
                        : 2;
    }

    private boolean query(
            Node node,
            int start,
            int end,
            int queryStart,
            int queryEnd) {

        // No intersection contributes no uncovered portion.
        if (queryStart > end || queryEnd < start) {
            return true;
        }

        // If the entire current segment is tracked,
        // every queried portion inside it is also tracked.
        if (node.state == 1) {
            return true;
        }

        // If the entire current segment is untracked,
        // any non-empty intersection fails the query.
        if (node.state == 0) {
            return false;
        }

        int mid = start + (end - start) / 2;

        // Mixed state: inspect the relevant children.
        pushDown(node);

        boolean leftResult = true;
        boolean rightResult = true;

        if (queryStart <= mid) {
            leftResult = query(
                    node.left,
                    start,
                    mid,
                    queryStart,
                    queryEnd
            );
        }

        if (queryEnd > mid) {
            rightResult = query(
                    node.right,
                    mid + 1,
                    end,
                    queryStart,
                    queryEnd
            );
        }

        return leftResult && rightResult;
    }

    private void pushDown(Node node) {

        // A leaf cannot have children.
        // In this problem the smallest coordinate cell has length 1.
        if (node.left != null || node.right != null) {
            return;
        }

        // Create two children inheriting the parent's current state.
        node.left = new Node();
        node.right = new Node();

        node.left.state = node.state;
        node.right.state = node.state;

        node.left.lazy = node.state == 2 ? -1 : node.state;
        node.right.lazy = node.state == 2 ? -1 : node.state;

        node.lazy = -1;
    }
}

