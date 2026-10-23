/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Given a data stream input of non-negative integers, summarize the numbers 
 * seen so far as a list of disjoint intervals.
 * 
 * Implement the SummaryRanges class:
 * - SummaryRanges(): Initializes the object with an empty stream.
 * - void addNum(int value): Adds the integer value to the stream.
 * - int[][] getIntervals(): Returns a summary of the integers in the stream 
 *   currently as a list of disjoint intervals [starti, endi]. The answer 
 *   should be sorted by starti.
 * 
 * Constraints:
 * - 0 <= value <= 10^4
 * - At most 3 * 10^4 calls to addNum and getIntervals.
 * - At most 10^2 calls to getIntervals.
 * - Follow up: What if there are lots of merges and the number of disjoint 
 *   intervals is small compared to the size of the data stream?
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. What is the ratio of `addNum` to `getIntervals` calls?
 *    (The constraints specify 30,000 adds but only 100 gets. This means the 
 *    system is extremely write-heavy. We must prioritize O(1) or O(log N) adds).
 * 2. Are the stream values strictly bounded to 10^4?
 *    (Yes, which makes a boolean array feasible. However, answering the follow-up 
 *    requires a solution that scales even if values hit 10^9).
 * 3. Can duplicates arrive in the stream?
 *    (Yes, data streams often have duplicates. Adding an already-tracked number 
 *    should be a no-op).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: Track a growing set of numbers and dynamically compress 
 *   contiguous sequences into intervals. The binding constraint is doing this 
 *   efficiently as new numbers constantly bridge the gaps between existing intervals.
 * 
 * --- Approach 1: Brute Force Boolean Array ---
 * 1. Intuition: The values are bounded to 10^4. We can just use an array of booleans 
 *    to represent the presence of each number.
 * 2. Try: `boolean[] seen = new boolean[10001]`. 
 *    `addNum(val)` -> `seen[val] = true`.
 *    `getIntervals()` -> Iterate from 0 to 10000. When you see `true`, mark the start. 
 *    Keep going until `false`, mark the end, and save the interval.
 * 3. Why it works: Array access is instant. It perfectly handles duplicates.
 * 4. Why it's too costly: The follow-up hints at a small number of intervals compared 
 *    to stream size. If we only track `[1, 10000]`, `getIntervals()` still scans 
 *    10,000 array slots to find 1 interval. Furthermore, it completely breaks if 
 *    values can reach 10^9 (Out of Memory).
 * 5. Bottleneck: Re-evaluating the entire domain space on every query.
 * 
 * Time Complexity: addNum: O(1). getIntervals: O(V) where V is the max value (10000).
 * Space Complexity: O(V) — allocating an array for the entire value domain.
 * 
 * --- Approach 2: HashSet + Sort (Lazy Evaluation) ---
 * 1. Intuition: If `getIntervals` is called rarely (max 100 times), maybe we just 
 *    collect numbers and only do the work when asked.
 * 2. Try: `addNum(val)` -> `set.add(val)`.
 *    `getIntervals()` -> dump set to an `ArrayList`, sort it, and sequentially 
 *    group adjacent numbers into intervals.
 * 3. Why it works: We only process elements actually seen, avoiding the O(V) space.
 * 4. Why it's suboptimal: Even though `getIntervals` is rare, sorting up to 30,000 
 *    elements 100 times is 3,000,000 operations. It doesn't take advantage of the 
 *    "lots of merges" hint.
 * 5. Bottleneck: Re-sorting and re-grouping the entire history of the stream from scratch.
 * 
 * Time Complexity: addNum: O(1). getIntervals: O(N log N) where N is numbers seen.
 * Space Complexity: O(N) — storing every unique number.
 * 
 * --- Approach 3: TreeMap of Disjoint Intervals (Optimal & Follow-up Answer) ---
 * 1. Intuition: We only care about the *boundaries* of contiguous blocks. If we maintain 
 *    disjoint intervals in a Balanced BST, we can dynamically merge them.
 * 2. Try: `TreeMap<Integer, Integer> map` mapping `start -> end`.
 *    `addNum(val)` -> Find the interval starting just before `val` (floor). Find the 
 *    interval starting just after `val` (higher). Check if `val` connects them!
 * 3. Why it works: A TreeMap keeps intervals sorted by their start times. Because they 
 *    are disjoint, finding the correct neighborhood to insert or merge takes O(log I) time.
 * 4. Which to write in a 45-min interview? Approach 3. It specifically addresses the 
 *    follow-up by keeping the memory footprint strictly proportional to the number of 
 *    *intervals* (I), not the size of the stream (N).
 * 
 * Time Complexity: addNum: O(log I) — TreeMap lookups/updates. getIntervals: O(I) — 
 *   iterating the map to build the result matrix.
 * Space Complexity: O(I) — where I is the number of disjoint intervals (I <= N).
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - `val` is already inside an existing interval: 
 *   e.g., Map has `[1, 5]`. We add `3`. `floorKey` is `1`. `map.get(1)` is `5`. 
 *   Since `5 >= 3`, we do nothing and return.
 * - `val` bridges two intervals perfectly:
 *   e.g., Map has `[1, 2]` and `[4, 5]`. We add `3`. `floorKey` is `1`, `higherKey` is `4`.
 *   `val` extends `1` and precedes `4`. We must merge them into a single `[1, 5]` and 
 *   delete the `[4, 5]` entry.
 * - Map is empty: `floorKey` and `higherKey` will be null. Must safely handle nulls 
 *   and just insert `[val, val]`.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "The 4-State Routing." When adding a number to a sorted list of 
 *   disjoint intervals, only 4 things can happen:
 *   1. It merges the Left and Right intervals together.
 *   2. It extends the Left interval's tail.
 *   3. It extends the Right interval's head.
 *   4. It forms a brand new disjoint interval.
 * 
 * ASCII DRY RUN (TreeMap Operations)
 * Call: addNum(1), addNum(3), addNum(2)
 * 
 * 1. addNum(1) -> Map is empty. 
 *    State 4: Disjoint. Put [1 -> 1].
 *    Map: { 1: 1 }
 * 
 * 2. addNum(3) -> floor is 1 (end: 1). higher is null.
 *    Does 3 equal 1 + 1? No.
 *    State 4: Disjoint. Put [3 -> 3].
 *    Map: { 1: 1, 3: 3 }
 * 
 * 3. addNum(2) -> floor is 1 (end: 1). higher is 3 (end: 3).
 *    Does 2 extend Left? (1 + 1 == 2) -> YES.
 *    Does 2 extend Right? (3 - 1 == 2) -> YES.
 *    State 1: Merge Both!
 *    Update Left's end to Right's end: map.put(1, 3).
 *    Destroy Right: map.remove(3).
 *    Map: { 1: 3 }
 * 
 * Pitfalls:
 * - Extending the "Right" interval: If `val` touches the Right interval (e.g., add `2` 
 *   when Right is `[3, 3]`), the new interval is `[2, 3]`. Because our map keys are 
 *   the *start* times, we MUST remove the old key `3` and put the new key `2`. You 
 *   cannot simply update a key in a TreeMap.
 * 
 * Pattern Recognition: "Disjoint continuous blocks" -> TreeMap<Start, End>.
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q: What if there are lots of merges and the number of disjoint intervals is 
 *    small compared to the size of the data stream?
 * A: This is exactly what the TreeMap Approach optimizes. If the stream is 1,000,000 
 *    contiguous numbers, the TreeMap will only hold a single node `[1, 1000000]`. 
 *    `addNum` takes O(1) (log 1 = 0 steps), and `getIntervals` takes O(1). 
 *    The HashSet or Array approaches would consume massive amounts of memory and CPU 
 *    scanning time.
 */

import java.util.Map;
import java.util.TreeMap;

public class SummaryRangesStudy {

    static class SummaryRanges {
        // Core data structure mapping the 'start' of an interval to its 'end'.
        // Invariant: All intervals are disjoint and merged when contiguous.
        private TreeMap<Integer, Integer> intervals;

        /** 
         * Initializes the object with an empty stream. 
         */
        public SummaryRanges() {
            intervals = new TreeMap<>();
        }

        /** 
         * Adds the integer value to the stream.
         * Time Complexity: O(log I) where I is the current number of disjoint intervals.
         */
        public void addNum(int val) {
            // O(log I) check if the exact start exists
            if (intervals.containsKey(val)) {
                return;
            }

            // Find the interval that starts strictly before or at 'val'
            Integer leftStart = intervals.floorKey(val);
            // Find the interval that starts strictly after 'val'
            Integer rightStart = intervals.higherKey(val);

            // 1. Is 'val' completely swallowed by the left interval?
            if (leftStart != null && intervals.get(leftStart) >= val) {
                return; // Nothing to do, 'val' is already covered.
            }

            // Determine if 'val' physically touches the boundaries of its neighbors
            boolean touchesLeft = (leftStart != null && intervals.get(leftStart) == val - 1);
            boolean touchesRight = (rightStart != null && rightStart == val + 1);

            // 2. State 1: Merge Both
            // 'val' perfectly bridges the gap between left and right intervals
            if (touchesLeft && touchesRight) {
                // Update the left interval's tail to absorb the right interval's tail
                intervals.put(leftStart, intervals.get(rightStart));
                // Obliterate the right interval since it's now part of the left
                intervals.remove(rightStart);
            } 
            // 3. State 2: Merge Left
            // 'val' extends the tail of the left interval
            else if (touchesLeft) {
                intervals.put(leftStart, val);
            } 
            // 4. State 3: Merge Right
            // 'val' extends the head of the right interval
            else if (touchesRight) {
                // Because the Map Key represents the head, we must insert the new 
                // shifted head 'val' and copy the old tail, then delete the old head.
                intervals.put(val, intervals.get(rightStart));
                intervals.remove(rightStart);
            } 
            // 5. State 4: Disjoint
            // 'val' is an island, touching nothing
            else {
                intervals.put(val, val);
            }
        }

        /** 
         * Returns a summary of the integers as a list of disjoint intervals.
         * Time Complexity: O(I) where I is the current number of disjoint intervals.
         */
        public int[][] getIntervals() {
            // The constraint asks for a 2D array matrix representation
            int[][] result = new int[intervals.size()][2];
            int index = 0;
            
            // The TreeMap iterates in strict sorted order by the key (start time)
            for (Map.Entry<Integer, Integer> entry : intervals.entrySet()) {
                result[index][0] = entry.getKey();
                result[index][1] = entry.getValue();
                index++;
            }
            
            return result;
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        SummaryRanges summaryRanges = new SummaryRanges();
        
        System.out.println("Adding 1...");
        summaryRanges.addNum(1);      // arr = [1]
        printIntervals(summaryRanges.getIntervals()); // Expected: [[1, 1]]
        
        System.out.println("Adding 3...");
        summaryRanges.addNum(3);      // arr = [1, 3]
        printIntervals(summaryRanges.getIntervals()); // Expected: [[1, 1], [3, 3]]
        
        System.out.println("Adding 7...");
        summaryRanges.addNum(7);      // arr = [1, 3, 7]
        printIntervals(summaryRanges.getIntervals()); // Expected: [[1, 1], [3, 3], [7, 7]]
        
        // This add bridges 1 and 3
        System.out.println("Adding 2 (Merge Both!)...");
        summaryRanges.addNum(2);      // arr = [1, 2, 3, 7]
        printIntervals(summaryRanges.getIntervals()); // Expected: [[1, 3], [7, 7]]
        
        // This add touches the left of 7
        System.out.println("Adding 6 (Merge Right!)...");
        summaryRanges.addNum(6);      // arr = [1, 2, 3, 6, 7]
        printIntervals(summaryRanges.getIntervals()); // Expected: [[1, 3], [6, 7]]
        
        // Duplicate check
        System.out.println("Adding 2 again (No-Op)...");
        summaryRanges.addNum(2);
        printIntervals(summaryRanges.getIntervals()); // Expected: [[1, 3], [6, 7]]
    }
    
    // Helper to print 2D arrays neatly
    private static void printIntervals(int[][] intervals) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < intervals.length; i++) {
            sb.append("[").append(intervals[i][0]).append(", ").append(intervals[i][1]).append("]");
            if (i < intervals.length - 1) sb.append(", ");
        }
        sb.append("]");
        System.out.println(sb.toString());
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: TreeMap (Start -> End) with 4-State Routing.
 * Key Observation: When data scales, representing discrete space (booleans) is highly 
 * inefficient. Tracking the boundaries of contiguous blocks in a Balanced BST reduces 
 * the memory footprint to O(Intervals) and modification time to O(log Intervals).
 * What to Memorize: The 4 merge states: 1) Touches left & right -> bridge. 2) Touches left 
 * -> stretch tail. 3) Touches right -> shift head (requires remove/put). 4) None -> isolate.
 * Most Common Trap: Updating the head of a "Right" interval by mutating the map entry directly. 
 * Since the head is the TreeMap's Key, you must explicitly `remove()` the old key and `put()` 
 * the new key to maintain tree structure.
 * Mental Trigger: "Merge disjoint blocks dynamically" -> "TreeMap mapping Start->End".
 * ============================================================================
 */












































import java.util.Map;
import java.util.TreeMap;

class SummaryRanges {

    /*
     * Each map entry represents exactly one maximal interval:
     *
     *     key   = inclusive interval start
     *     value = inclusive interval end
     *
     * Example:
     *     1 -> 3
     *     7 -> 9
     *
     * represents [1, 3] and [7, 9].
     *
     * TreeMap keeps keys sorted and supports floorEntry() and
     * ceilingEntry() in O(log k), where k is the number of intervals.
     *
     * INVARIANT:
     *   1. Every interval contains only values seen so far.
     *   2. Intervals are sorted by start.
     *   3. Intervals do not overlap.
     *   4. Intervals are not adjacent: if one ends at e, the next
     *      interval must start strictly greater than e + 1.
     *
     * The fourth property means every interval is already as large
     * as possible. Adjacent intervals should always have been merged.
     */
    private final TreeMap<Integer, Integer> intervals = new TreeMap<>();

    public SummaryRanges() {
        // TreeMap starts empty, so the stream initially has no intervals.
    }

    public void addNum(int value) {

        /*
         * STEP 1: Find the closest interval starting at or before value.
         *
         * floorEntry(value) returns the entry with the greatest key
         * that is <= value.
         *
         * This is the only interval that could already contain value
         * or connect to value from the left.
         */
        Map.Entry<Integer, Integer> left =
                intervals.floorEntry(value);

        /*
         * STEP 2: Handle duplicates.
         *
         * Because left.start <= value, if left.end >= value, then:
         *
         *     left.start <= value <= left.end
         *
         * Therefore, value is already represented in the summary.
         *
         * We must return immediately. Otherwise, we might create
         * duplicate or overlapping intervals.
         */
        if (left != null && left.getValue() >= value) {
            return;
        }

        /*
         * STEP 3: Find the closest interval starting at or after value.
         *
         * Since the duplicate case has already returned, a ceiling
         * entry, if present, starts strictly after value.
         *
         * This is the only interval that could connect from the right.
         */
        Map.Entry<Integer, Integer> right =
                intervals.ceilingEntry(value);

        /*
         * STEP 4: Determine whether value touches either neighbor.
         *
         * Left connection:
         *     The left interval ends at value - 1.
         *
         * Right connection:
         *     The right interval starts at value + 1.
         *
         * Given the problem's bound (value <= 10,000), value + 1
         * cannot overflow an int.
         */
        boolean joinsLeft =
                left != null && left.getValue() == value - 1;

        boolean joinsRight =
                right != null && right.getKey() == value + 1;

        if (joinsLeft && joinsRight) {

            /*
             * CASE 1: BRIDGE BOTH INTERVALS.
             *
             * Before:
             *     left.start -> value - 1
             *     value + 1  -> right.end
             *
             * After:
             *     left.start -> right.end
             *
             * Keep the left interval's start and extend its end
             * across value to the end of the right interval.
             *
             * Remove the right interval because it is now part of
             * the merged interval.
             */
            int mergedStart = left.getKey();
            int mergedEnd = right.getValue();
            int rightStart = right.getKey();

            intervals.put(mergedStart, mergedEnd);
            intervals.remove(rightStart);

        } else if (joinsLeft) {

            /*
             * CASE 2: EXTEND THE LEFT INTERVAL.
             *
             * Example:
             *     [1, 3] + 4 -> [1, 4]
             *
             * The start remains unchanged; only the end moves.
             *
             * The existing left entry is mutable through put().
             * No new interval is required.
             */
            intervals.put(left.getKey(), value);

        } else if (joinsRight) {

            /*
             * CASE 3: EXTEND THE RIGHT INTERVAL.
             *
             * Example:
             *     [5, 7] + 4 -> [4, 7]
             *
             * This case is slightly trickier:
             *
             * The map is keyed by interval START, and that start
             * changes from 5 to 4.
             *
             * Therefore, we must remove the old key and insert
             * the interval again with the new start.
             */
            int oldStart = right.getKey();
            int oldEnd = right.getValue();

            intervals.remove(oldStart);
            intervals.put(value, oldEnd);

        } else {

            /*
             * CASE 4: CREATE A NEW SINGLETON INTERVAL.
             *
             * Neither neighboring interval is consecutive to value.
             *
             * Example:
             *     [1, 3], [7, 9] + 5
             *       -> [1, 3], [5, 5], [7, 9]
             *
             * A new number that is not adjacent to either interval
             * must be represented by its own interval [value, value].
             */
            intervals.put(value, value);
        }
    }

    public int[][] getIntervals() {

        /*
         * TreeMap iterates in ascending key order, which is exactly
         * the order required by the problem.
         *
         * We return a fresh array instead of exposing the internal map.
         * Callers can modify the returned arrays without corrupting
         * the internal interval structure.
         */
        int[][] result = new int[intervals.size()][2];

        int index = 0;

        for (Map.Entry<Integer, Integer> interval
                : intervals.entrySet()) {

            result[index][0] = interval.getKey();
            result[index][1] = interval.getValue();
            index++;
        }

        return result;
    }
}

/*
COMPLEXITY
Let:
    k = number of currently disjoint intervals
    n = number of distinct values received so far

addNum(value):
    Time: O(log k).
        - floorEntry() costs O(log k).
        - ceilingEntry() costs O(log k).
        - Each put() or remove() costs O(log k).
        - There are only a constant number of tree operations.
        Therefore, the total remains O(log k).
        For an empty map, insertion is O(1); O(log(k + 1)) is a
        convenient unified bound.

    Auxiliary space: O(1) per operation, excluding the stored map.
    Persistent space: O(k), because only the disjoint intervals
                      are stored.

getIntervals():
    Time: O(k), because every interval must be copied into the result.
    Auxiliary space: O(1) beyond the returned array and its contents.
    Output space: O(k).

TOTAL PERSISTENT SPACE: O(k), rather than O(n).

WHY THIS IS THE BEST GENERAL-PURPOSE APPROACH:
    - We store the summary rather than every number.
    - Insertions locate relevant neighbors in logarithmic time.
    - Duplicate values do not alter the map.
    - The returned intervals are already sorted.
    - If thousands of values collapse into one interval, we store
      just one map entry.

FOLLOW-UP BENEFIT:
    When k is much smaller than n, the memory savings are substantial.
    This is exactly the situation described by the problem's follow-up.
*/


























import java.util.Map;
import java.util.TreeMap;

class SummaryRangesFastInsert {

    /*
     * Store only disjoint intervals.
     *
     * key   = inclusive start of an interval
     * value = inclusive end of an interval
     *
     * Example:
     *   1 -> 3
     *   7 -> 9
     *
     * represents [1,3], [7,9].
     *
     * Why TreeMap?
     *   floorEntry(x) finds the interval starting at or immediately
     *   before x.
     *
     *   ceilingEntry(x) finds the interval starting at or immediately
     *   after x.
     *
     *   These searches take O(log k), where k is the number of intervals.
     *
     * INVARIANT:
     *   All intervals are sorted, disjoint, and non-adjacent.
     *   Adjacent intervals must always be merged.
     */
    private final TreeMap<Integer, Integer> intervals = new TreeMap<>();

    public void addNum(int value) {

        // Find the interval that could contain value or connect from left.
        var left = intervals.floorEntry(value);

        // If value is already covered, the summary does not change.
        if (left != null && left.getValue() >= value) {
            return;
        }

        // Find the next interval to the right, if one exists.
        var right = intervals.ceilingEntry(value);

        // Does the incoming value connect to either interval?
        boolean joinsLeft =
                left != null && left.getValue() == value - 1;

        boolean joinsRight =
                right != null && right.getKey() == value + 1;

        if (joinsLeft && joinsRight) {

            // BRIDGE:
            // [left.start, value - 1] + value + [value + 1, right.end]
            // becomes [left.start, right.end].
            //
            // Keep the left key, extend its end, and delete the right key.
            intervals.put(left.getKey(), right.getValue());
            intervals.remove(right.getKey());

        } else if (joinsLeft) {

            // EXTEND LEFT:
            // [1,3] + 4 -> [1,4].
            // The start/key does not change.
            intervals.put(left.getKey(), value);

        } else if (joinsRight) {

            // EXTEND RIGHT:
            // [5,7] + 4 -> [4,7].
            //
            // The key is the interval start, so the old entry must
            // be removed before inserting the interval with its new key.
            int oldEnd = right.getValue();

            intervals.remove(right.getKey());
            intervals.put(value, oldEnd);

        } else {

            // ISOLATED VALUE:
            // Neither neighbor is consecutive, so create [value,value].
            intervals.put(value, value);
        }
    }

    public int[][] getIntervals() {

        // Every map entry is already one complete interval.
        // TreeMap iteration is sorted by interval start.
        int[][] result = new int[intervals.size()][2];

        int i = 0;

        for (var entry : intervals.entrySet()) {
            result[i][0] = entry.getKey();
            result[i][1] = entry.getValue();
            i++;
        }

        // Return a fresh array, so callers cannot corrupt internal state.
        return result;
    }
}

/*
 * COMPLEXITY
 *
 * addNum(value):
 *   O(log(k + 1)) time.
 *   We perform a constant number of balanced-tree lookups, insertions,
 *   and removals. The number of intervals is k.
 *
 * getIntervals():
 *   O(k) time to copy all intervals into the output array.
 *
 * Persistent space:
 *   O(k), because we store only the current disjoint intervals.
 *
 * Best when:
 *   - addNum() is called frequently.
 *   - The number of intervals is relatively small.
 *   - We need to keep the interval summary correct after every insertion.
 */










































import java.util.ArrayList;
import java.util.List;

class SummaryRangesFastRetrieve {

    /*
     * Each element represents one interval:
     *
     *     [start, end]
     *
     * Example:
     *     [1, 3], [7, 9], [12, 15]
     *
     * INVARIANTS:
     * 1. Intervals are sorted by their starting values.
     * 2. Intervals never overlap.
     * 3. Intervals are never adjacent.
     *
     * We store intervals instead of individual numbers to keep the
     * data structure compact when many values belong to the same range.
     */
    private final List<int[]> intervals = new ArrayList<>();

    /*
     * Cache the most recently generated result.
     *
     * null means the interval list has changed since the last retrieval.
     *
     * IMPORTANT:
     * To achieve O(1) retrieval on a cache hit, we return the same array.
     * Therefore, callers must not modify the returned array or its rows.
     */
    private int[][] cachedResult;

    public void addNum(int value) {

        /*
         * STEP 1: Find the insertion position using binary search.
         *
         * We want the FIRST index whose interval start is GREATER than
         * value.
         *
         * Example:
         *
         *     intervals = [[1,3], [7,9], [12,15]]
         *     value     = 5
         *
         *     index 0: start = 1  <= 5
         *     index 1: start = 7  >  5  <-- answer
         *
         * So:
         *     insertionIndex = 1
         *     left neighbor  = intervals[0] = [1,3]
         *     right neighbor = intervals[1] = [7,9]
         *
         * If no interval starts greater than value, the answer is
         * intervals.size(), meaning insertion would happen at the end.
         */
        int insertionIndex = findInsertionIndex(value);

        /*
         * STEP 2: Identify the two possible neighboring intervals.
         *
         * The left neighbor is immediately before the insertion position.
         * The right neighbor is exactly at the insertion position.
         *
         * Either neighbor might not exist at the boundaries of the list.
         */
        int[] left = insertionIndex > 0
                ? intervals.get(insertionIndex - 1)
                : null;

        int[] right = insertionIndex < intervals.size()
                ? intervals.get(insertionIndex)
                : null;

        /*
         * STEP 3: Handle duplicates.
         *
         * Because intervals are sorted, if value is already present,
         * it must belong to the left neighbor.
         *
         * Example:
         *     left = [1,5], value = 3
         *
         *     left.start <= value <= left.end
         *
         * Nothing needs to change.
         */
        if (left != null && left[1] >= value) {
            return;
        }

        /*
         * STEP 4: Check whether value connects to either neighbor.
         *
         * It connects to the left if:
         *     left.end == value - 1
         *
         * It connects to the right if:
         *     right.start == value + 1
         *
         * Example:
         *     left  = [1,3]
         *     value = 4
         *
         *     3 == 4 - 1, so value extends the left interval.
         */
        boolean connectsLeft =
                left != null && left[1] == value - 1;

        boolean connectsRight =
                right != null && right[0] == value + 1;

        /*
         * CASE 1: Connects to BOTH neighbors.
         *
         * Example:
         *
         *     Before:
         *         [1,2]   [4,6]
         *
         *     Add:
         *         3
         *
         *     After:
         *         [1,6]
         *
         * The left interval keeps its starting value.
         * Its end becomes the right interval's end.
         * Then the right interval is removed.
         */
        if (connectsLeft && connectsRight) {

            left[1] = right[1];

            // Removing the right interval preserves sorted order.
            intervals.remove(insertionIndex);
        }

        /*
         * CASE 2: Connects ONLY to the left.
         *
         * Example:
         *     [1,3] + 4 -> [1,4]
         *
         * Only the left interval's end changes.
         */
        else if (connectsLeft) {

            left[1] = value;
        }

        /*
         * CASE 3: Connects ONLY to the right.
         *
         * Example:
         *     [5,7] + 4 -> [4,7]
         *
         * The right interval's start moves backward to value.
         * Its position in the list does not change because value
         * belongs immediately before its old start.
         */
        else if (connectsRight) {

            right[0] = value;
        }

        /*
         * CASE 4: Connects to NEITHER neighbor.
         *
         * Example:
         *     [1,3], [7,9] + 5
         *
         *     becomes:
         *         [1,3], [5,5], [7,9]
         *
         * Insert the singleton interval at the binary-search position.
         */
        else {

            intervals.add(
                    insertionIndex,
                    new int[]{value, value}
            );
        }

        /*
         * STEP 5: Invalidate the cached result.
         *
         * The interval list changed, so a previously generated array
         * no longer represents the current summary.
         *
         * We invalidate the cache only when the summary actually changes.
         */
        cachedResult = null;
    }

    /*
     * Binary-search helper:
     *
     * Returns the index of the FIRST interval whose start > value.
     *
     * If every interval starts <= value, returns intervals.size().
     *
     * WHY USE AN EXPLICIT ANSWER VARIABLE?
     *
     * Instead of treating low/high as the final answer implicitly,
     * we maintain:
     *
     *     answer = best index found so far
     *
     * Whenever an interval starts > value, it is a valid candidate.
     * We save its index and continue searching to the LEFT for an
     * earlier valid candidate.
     *
     * This is the classic "find the first element satisfying a condition"
     * binary-search pattern.
     */
    private int findInsertionIndex(int value) {

        int low = 0;
        int high = intervals.size() - 1;

        // If no interval satisfies the condition, insertion is at the end.
        int answer = intervals.size();

        while (low <= high) {

            int mid = low + (high - low) / 2;

            int intervalStart = intervals.get(mid)[0];

            if (intervalStart > value) {

                /*
                 * This interval satisfies our condition.
                 *
                 * Save it as the current best answer.
                 *
                 * But there might be an earlier interval whose start
                 * is also > value, so continue searching to the LEFT.
                 */
                answer = mid;
                high = mid - 1;

            } else {

                /*
                 * This interval starts <= value.
                 *
                 * It cannot be the answer because we want a start
                 * strictly greater than value.
                 *
                 * Therefore, search to the RIGHT.
                 */
                low = mid + 1;
            }
        }

        return answer;
    }

    public int[][] getIntervals() {

        /*
         * CACHE HIT:
         *
         * If nothing changed since the last retrieval, reuse the
         * previously generated array.
         *
         * This is O(1), because we simply return a reference.
         */
        if (cachedResult != null) {
            return cachedResult;
        }

        /*
         * CACHE MISS:
         *
         * Construct a new snapshot of the current intervals.
         *
         * ArrayList is already sorted, so we just copy the intervals
         * in order. No additional sorting is necessary.
         */
        cachedResult = new int[intervals.size()][2];

        for (int i = 0; i < intervals.size(); i++) {

            int[] currentInterval = intervals.get(i);

            cachedResult[i][0] = currentInterval[0];
            cachedResult[i][1] = currentInterval[1];
        }

        return cachedResult;
    }
}

/*
 * COMPLEXITY ANALYSIS
 *
 * Let k be the number of disjoint intervals.
 *
 * addNum(value):
 *
 *   Binary search:
 *       O(log(k + 1))
 *
 *   Finding the neighbors:
 *       O(1), because ArrayList supports indexed access in O(1).
 *
 *   Extending an interval:
 *       O(1).
 *
 *   Inserting or removing an interval:
 *       O(k) in the worst case, because ArrayList may shift elements.
 *
 *   Overall:
 *       O(k) worst-case time.
 *
 * getIntervals():
 *
 *   Cache miss:
 *       O(k) time to copy all intervals into a new array.
 *
 *   Cache hit:
 *       O(1) time to return the existing reference.
 *
 * Persistent space:
 *       O(k) for the interval list.
 *       O(k) for the cached result when populated.
 *
 * TRADE-OFF:
 *
 *   This design optimizes repeated retrievals when no updates occur
 *   between calls. It does not optimize insertion: inserting into the
 *   middle of an ArrayList may require shifting O(k) intervals.
 *
 * API WARNING:
 *
 *   Callers must not modify the returned int[][] or its inner arrays.
 *   If the API requires a fresh, independently mutable result every time,
 *   return a defensive copy instead; then retrieval takes O(k) even
 *   when the cached snapshot already exists.
 */







