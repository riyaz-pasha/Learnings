/**
 * 2158. Amount of New Area Painted Each Day
 * 
 * ============================================================================
 * 1. PROBLEM STATEMENT
 * ============================================================================
 * There is a long and thin painting that can be represented by a number line. 
 * You are given a 0-indexed 2D integer array paint of length n, where 
 * paint[i] = [starti, endi]. This means that on the ith day you need to paint 
 * the area between starti and endi.
 * 
 * Painting the same area multiple times will create an uneven painting so you 
 * only want to paint each area of the painting at most once.
 * 
 * Return an integer array worklog of length n, where worklog[i] is the amount 
 * of new area that you painted on the ith day.
 * 
 * Constraints:
 * - 1 <= paint.length <= 10^5
 * - paint[i].length == 2
 * - 0 <= starti < endi <= 5 * 10^4
 * 
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (To ask the interviewer)
 * ============================================================================
 * 1. What is the maximum possible coordinate for `end_i`? 
 *    (Crucial: 50,000 is small enough to allocate a fixed array. If it were 10^9, 
 *    we would be forced to use a TreeMap or a dynamically allocated Segment Tree).
 * 2. Are the intervals half-open [start, end)?
 *    (Yes, painting from 1 to 4 means painting segments [1,2], [2,3], [3,4], which 
 *    is exactly length 3. This maps perfectly to 0-indexed integer tracking).
 * 3. Can a day have zero new area painted?
 *    (Yes, if a later day's interval is completely swallowed by previous days, 
 *    it should record 0 new work).
 * 4. Are we answering these "online" (as they come) or "offline"?
 *    (The problem provides the array upfront, but an online-capable approach 
 *    where we don't need to look ahead is strictly better).
 * 
 * ============================================================================
 * 3. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement & Constraint: 
 * We need to process intervals sequentially and count the unpainted segments. 
 * The binding constraint is the maximum coordinate limit (50,000) versus the 
 * number of days (100,000). 
 * 
 * --- Approach 1: True Brute Force ---
 * 1. Idea: Keep a boolean array `painted` of size 50,001. For each day, loop 
 *    from `start` to `end - 1`. If `!painted[i]`, mark it true and increment work.
 * 2. What I'd naturally try: `for (int i = start; i < end; i++) if (!arr[i]) ...`
 * 3. Why it works: It literally simulates the brush hitting every single inch.
 * 4. Why it's too slow/costly: A single day could span 50,000 units. Doing that 
 *    100,000 times yields 5 billion operations. (Time Limit Exceeded).
 * 5. What work is repeated: We step through long stretches of *already painted* 
 *    areas inch-by-inch, re-evaluating `painted[i]` repeatedly.
 * 6. The pivot observation: If we know a stretch is painted, we shouldn't step 
 *    through it. We should FAST-FORWARD to the end of the painted block.
 * 
 * Time Complexity: O(N * M) — where N is days, M is max interval length, because we check every inch.
 * Space Complexity: O(M) — because we store a boolean array up to the max coordinate.
 * 
 * --- Approach 2: Disjoint Set / Jump Array (Optimal for M = 50,000) ---
 * 1. Idea: Use a tracking array where `nextUnpainted[i]` points to the next 
 *    available unpainted spot. Initially, `nextUnpainted[i] = i`.
 * 2. What I'd naturally try: When painting at `i`, record that it's painted by 
 *    pointing it to `i + 1`. If we ever ask about `i` again, we jump to `i + 1`. 
 *    To make multiple jumps fast, use Union-Find path compression!
 * 3. Why it works: If coordinates [10 to 50] are painted, asking for the next 
 *    unpainted spot at 10 will instantly return 51 in O(1) amortized time.
 * 4. Why this is the ceiling: We touch each unpainted coordinate exactly once. 
 *    Already painted coordinates are skipped instantly. 
 * 
 * Time Complexity: O(N * \alpha(M) + M) — because path compression makes the amortized jump O(1), meaning we do work proportional to the actual area painted.
 * Space Complexity: O(M) — because we allocate a parent array of size M (50,000).
 * 
 * --- Approach 3: TreeMap Interval Merging (Generalization for M = 10^9) ---
 * 1. Idea: If the coordinates were up to 10^9, the Jump Array would cause an OOM. 
 *    Instead, use a `TreeMap<Integer, Integer>` to maintain disjoint painted intervals.
 * 2. What I'd naturally try: Find all existing intervals overlapping `[start, end)`. 
 *    Calculate the intersection lengths, subtract them from `end - start`, remove 
 *    the old intervals, and insert a new merged interval.
 * 3. Why it works: Math handles the overlaps without needing to visit every coordinate.
 * 
 * Time Complexity: O(N log N) — because interval merges amortize to O(log N) per day.
 * Space Complexity: O(N) — because the TreeMap stores at most N disjoint intervals.
 * 
 * Verdict for 45-minute interview:
 * I would write Approach 2 (Jump Array / DSU). Because the problem strictly bounds 
 * coordinates to 50,000, this approach is wildly faster in practice, significantly 
 * shorter to write, and proves mastery of Disjoint Set logic. I would verbally 
 * explain Approach 3 as a follow-up for 10^9 bounds.
 * 
 * ============================================================================
 * 4. EDGE CASES
 * ============================================================================
 * - Fully Enveloped: Day 1: [10, 50], Day 2: [20, 30]. Day 2 should yield 0 work.
 * - Exact Touching: Day 1: [10, 20], Day 2: [20, 30]. Day 2 yields 10 work.
 * - Bridging the Gap: Day 1: [10, 20], Day 2: [30, 40], Day 3: [15, 35]. 
 *   Day 3 bridges them, finding 10 units of unpainted space ([20, 30]).
 * 
 * ============================================================================
 * 5. KEY INSIGHT, DIAGRAM & DRY RUN
 * ============================================================================
 * > Key Insight: 
 * "Don't walk through painted territory; teleport over it."
 * By treating the number line as a graph where each painted node points to the 
 * next node on the right, finding the next unpainted spot becomes a standard 
 * Union-Find `find()` operation with path compression.
 * 
 * ASCII Diagram: Jump Array Path Compression
 * Day 1 paints [1, 4]. 
 * nextUnpainted[1] = 2
 * nextUnpainted[2] = 3
 * nextUnpainted[3] = 4
 * 
 * Day 2 paints [2, 5].
 * start = 2. find(2) traces 2 -> 3 -> 4. 
 * Path compression collapses this: nextUnpainted[2] now points directly to 4!
 * We instantly jump to 4, paint it, point it to 5, and we're done. Work = 1.
 * 
 * Dry Run:
 * paint = [[1, 4], [2, 5]]
 * Initial: arr[i] = i
 * 
 * i=0, paint [1, 4]:
 * - curr = find(1) = 1. < 4. work=1. arr[1]=2.
 * - curr = find(2) = 2. < 4. work=2. arr[2]=3.
 * - curr = find(3) = 3. < 4. work=3. arr[3]=4.
 * - curr = find(4) = 4. Stop. Result[0] = 3.
 * 
 * i=1, paint [2, 5]:
 * - curr = find(2) -> arr[2]=3 -> arr[3]=4. Returns 4. Path compressed so arr[2]=4.
 * - curr = 4. < 5. work=1. arr[4]=5.
 * - curr = find(5) = 5. Stop. Result[1] = 1.
 * 
 * ============================================================================
 * 6. PITFALLS & PATTERN RECOGNITION
 * ============================================================================
 * Pitfalls:
 * - Allocating an array that is exactly 50,000. If `end` can be 50,000, the array 
 *   MUST be sized 50,001 or 50,002 to safely query the boundary without IndexOutofBounds.
 * - Forgetting path compression: If you just do `while (painted[curr]) curr++;`, 
 *   it devolves back to O(N * M) brute force because you iterate the entire block 
 *   one by one every time.
 * 
 * Pattern Recognition: 
 * "Skipping contiguous processed blocks -> Union-Find (DSU)."
 * This is the exact same pattern used in:
 * - "Longest Consecutive Sequence" (skip tracking)
 * - "Design a File System" (finding next available block)
 * 
 * ============================================================================
 * 7. INTERVIEW SCRIPT
 * ============================================================================
 * "We need to calculate the new area painted each day without double-counting. 
 * A brute force boolean array would TLE because we'd step through already-painted 
 * stretches repeatedly. 
 * Because the maximum coordinate is only 50,000, we can use a Jump Array, which 
 * acts exactly like a Disjoint Set Union (Union-Find). I'll initialize an array 
 * where each coordinate points to itself, meaning it is unpainted. 
 * When I paint a coordinate, I point it to `coordinate + 1`. If I ever encounter 
 * an already painted coordinate, I use a `find()` method with path compression 
 * to instantly teleport to the end of the painted block. This drops our time 
 * complexity to nearly O(N + M) and space to O(M)."
 * 
 * ============================================================================
 * 8. FOLLOW-UPS
 * ============================================================================
 * Q: What if the coordinates could go up to 10^9? (Massive coordinates)
 * A: We can't allocate a Jump Array of size 1 billion. We would switch to a 
 *    TreeMap representing disjoint intervals. When a new interval arrives, we 
 *    calculate its total length, subtract the lengths of any overlapping intervals 
 *    found via `floorKey` and `higherKey`, and then merge them all together.
 * 
 * Q: What if we needed to answer queries about the total painted area in a range [L, R]?
 * A: We would need a Segment Tree with Lazy Propagation. It can apply range 
 *    updates (paint 1) and answer range sum queries in O(log(MAX_COORD)).
 */

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class AmountOfNewAreaPaintedEachDay {

    /**
     * APPROACH 2: Disjoint Set / Jump Array (Optimal for M = 50,000)
     * Time Complexity: O(N * \alpha(M) + M) ~ O(N + M)
     * Space Complexity: O(M) where M is max coordinate
     */
    public int[] amountPaintedDSU(int[][] paint) {
        int n = paint.length;
        int[] result = new int[n];
        
        // Constraints say max coordinate is 50,000. 
        // We use 50,002 to comfortably handle 'i + 1' leaps at the extreme right boundary.
        int[] nextUnpainted = new int[50002];
        
        // Initialize: every coordinate points to itself, meaning it is unpainted.
        for (int i = 0; i < nextUnpainted.length; i++) {
            nextUnpainted[i] = i;
        }
        
        for (int i = 0; i < n; i++) {
            int start = paint[i][0];
            int end = paint[i][1];
            int work = 0;
            
            // Teleport to the first actual unpainted coordinate in our range
            int curr = findNextUnpainted(start, nextUnpainted);
            
            while (curr < end) {
                // We found an unpainted spot! Paint it.
                work++;
                
                // Point this coordinate to the next adjacent coordinate.
                // This acts as our "Union" step.
                nextUnpainted[curr] = curr + 1;
                
                // Fast-forward to the NEXT available unpainted coordinate.
                curr = findNextUnpainted(curr, nextUnpainted);
            }
            
            result[i] = work;
        }
        
        return result;
    }
    
    /**
     * Helper: Union-Find 'find' with Path Compression.
     * Recursively finds the true next unpainted spot and flattens the tree.
     */
    private int findNextUnpainted(int i, int[] nextUnpainted) {
        // If a coordinate points to itself, it is definitively unpainted.
        if (nextUnpainted[i] == i) {
            return i;
        }
        // Path Compression: directly link this coordinate to the final answer.
        // This ensures future queries jump the entire block in O(1) amortized.
        return nextUnpainted[i] = findNextUnpainted(nextUnpainted[i], nextUnpainted);
    }

    /**
     * APPROACH 3: TreeMap Generalization (Optimal if constraints were 10^9)
     * Included as the follow-up implementation.
     * Time Complexity: O(N log N)
     * Space Complexity: O(N)
     */
    public int[] amountPaintedTreeMap(int[][] paint) {
        int[] result = new int[paint.length];
        
        // TreeMap stores non-overlapping intervals: Key = Start, Value = End
        TreeMap<Integer, Integer> map = new TreeMap<>();
        
        for (int i = 0; i < paint.length; i++) {
            int start = paint[i][0];
            int end = paint[i][1];
            int totalWork = end - start;
            
            // Find the interval that starts just before or at our 'start'
            Map.Entry<Integer, Integer> floor = map.floorEntry(start);
            
            // If the floor interval overlaps with our current interval, 
            // merge their starts and subtract the overlapping area.
            if (floor != null && floor.getValue() > start) {
                // Overlap amount is the min of the two ends minus our start
                int overlap = Math.min(floor.getValue(), end) - start;
                totalWork -= overlap;
                
                // Expand our start boundary to engulf the floor interval
                start = floor.getKey();
                // Expand our end boundary if the floor interval reaches further
                end = Math.max(end, floor.getValue());
                
                map.remove(floor.getKey());
            }
            
            // Check for intervals that start strictly after our new 'start' 
            // but before our 'end', meaning they are engulfed or overlapped.
            Map.Entry<Integer, Integer> ceiling = map.ceilingEntry(start);
            
            while (ceiling != null && ceiling.getKey() < end) {
                // Calculate how much of the ceiling interval overlaps with us
                int overlap = Math.min(end, ceiling.getValue()) - ceiling.getKey();
                totalWork -= overlap;
                
                // Expand our end boundary to engulf the ceiling interval
                end = Math.max(end, ceiling.getValue());
                
                map.remove(ceiling.getKey());
                ceiling = map.ceilingEntry(start);
            }
            
            // Record the new combined interval
            map.put(start, end);
            result[i] = totalWork;
        }
        
        return result;
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================
    public static void main(String[] args) {
        AmountOfNewAreaPaintedEachDay solution = new AmountOfNewAreaPaintedEachDay();
        
        // Test Case 1: Example sequence
        int[][] paint1 = {{1, 4}, {4, 7}, {5, 8}};
        System.out.println("Test 1 (Expected [3, 3, 1]): " 
            + Arrays.toString(solution.amountPaintedDSU(paint1)));
            
        // Test Case 2: Overlapping and enveloping
        int[][] paint2 = {{1, 4}, {5, 8}, {4, 7}, {1, 8}};
        // Day 1: [1,4] -> 3
        // Day 2: [5,8] -> 3
        // Day 3: [4,7] -> bridges [4,5], paints [4,5] -> 1
        // Day 4: [1,8] -> completely painted already -> 0
        System.out.println("Test 2 (Expected [3, 3, 1, 0]): " 
            + Arrays.toString(solution.amountPaintedDSU(paint2)));
            
        // Test Case 3: Complete overlap immediately
        int[][] paint3 = {{1, 5}, {2, 4}};
        System.out.println("Test 3 (Expected [4, 0]): " 
            + Arrays.toString(solution.amountPaintedDSU(paint3)));
            
        // Cross-checking the two approaches
        System.out.println("\nCross-checking approaches on Test 2:");
        System.out.println("DSU Jump Array: " + Arrays.toString(solution.amountPaintedDSU(paint2)));
        System.out.println("TreeMap Merge:  " + Arrays.toString(solution.amountPaintedTreeMap(paint2)));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern: Fast-forwarding via Disjoint Set Union (Path Compression).
 * 
 * Key observation: When bounded tightly by space (50,000 coords), an array can 
 * elegantly model a directed graph. Path compression flattens contiguous painted 
 * ranges, turning an O(M) brute force loop into O(1) teleports.
 * 
 * Memorize vs Understand: 
 * UNDERSTAND how `next[curr] = curr + 1` acts as a Union, joining a newly 
 * painted node to the next unpainted node. The `find()` acts as the teleport.
 * 
 * Most common trap: `IndexOutOfBounds`. If you paint up to coordinate 50,000, 
 * marking it painted requires looking at `50,000 + 1`. Always size the DSU 
 * array to MAX_COORD + 2 to be safe.
 * 
 * One-line mental trigger: "Skip painted chunks? Union-Find to next unpainted."
 */








































import java.util.Map;
import java.util.TreeMap;

class Solution {

    public int[] amountPainted(int[][] paint) {

        int n = paint.length;

        int[] answer = new int[n];

        /*
         * Maintains disjoint, non-adjacent painted intervals.
         *
         * Key   = interval start (inclusive)
         * Value = interval end (exclusive)
         *
         * Example:
         *
         * {1=5, 8=12}
         *
         * Means:
         * [1,5) and [8,12) are already painted.
         */
        TreeMap<Integer, Integer> intervals = new TreeMap<>();

        for (int day = 0; day < n; day++) {

            int start = paint[day][0];
            int end = paint[day][1];

            /*
             * PART 1: Calculate newly painted area.
             *
             * cursor represents the first coordinate
             * that has not yet been accounted for
             * within the current interval.
             */
            int cursor = start;
            int newArea = 0;

            /*
             * Check whether an existing interval starts
             * before or at start and extends beyond start.
             *
             * floorEntry(start) gives the closest existing
             * interval whose start <= start.
             */
            Map.Entry<Integer, Integer> previous =
                intervals.floorEntry(start);

            if (previous != null && previous.getValue() > start) {

                /*
                 * This interval covers the beginning
                 * of the requested painting interval.
                 */
                cursor = Math.max(cursor, previous.getValue());
            }

            /*
             * Find the first interval starting at or after
             * the original start.
             */
            Map.Entry<Integer, Integer> entry =
                intervals.ceilingEntry(start);

            /*
             * Examine all existing intervals that overlap
             * the current requested interval.
             */
            while (entry != null && entry.getKey() < end) {

                int existingStart = entry.getKey();
                int existingEnd = entry.getValue();

                /*
                 * If there is a gap between cursor and
                 * existingStart, that gap is newly painted.
                 */
                if (existingStart > cursor) {

                    newArea += existingStart - cursor;
                }

                /*
                 * Skip the already-painted portion.
                 */
                cursor = Math.max(cursor, existingEnd);

                if (cursor >= end) {
                    break;
                }

                /*
                 * Move to the next existing interval.
                 */
                entry = intervals.higherEntry(existingStart);
            }

            /*
             * If some part of [start, end) remains uncovered,
             * that remaining suffix is newly painted.
             */
            if (cursor < end) {
                newArea += end - cursor;
            }

            answer[day] = newArea;

            /*
             * PART 2: Merge the new interval into the map.
             *
             * Start with the new interval itself.
             */
            int mergedStart = start;
            int mergedEnd = end;

            /*
             * Include a previous interval if it overlaps
             * or touches the new interval.
             *
             * Touching intervals can be merged because
             * [1,3) U [3,5) = [1,5).
             */
            Map.Entry<Integer, Integer> candidate =
                intervals.floorEntry(start);

            if (candidate != null && candidate.getValue() >= start) {

                mergedStart = candidate.getKey();

                mergedEnd = Math.max(
                    mergedEnd,
                    candidate.getValue()
                );

                intervals.remove(candidate.getKey());
            }

            /*
             * Remove and merge all following intervals
             * whose starting coordinate is <= mergedEnd.
             *
             * Since intervals are sorted and disjoint,
             * each removed interval is merged only once
             * during its lifetime.
             */
            candidate = intervals.ceilingEntry(mergedStart);

            while (candidate != null &&
                   candidate.getKey() <= mergedEnd) {

                mergedEnd = Math.max(
                    mergedEnd,
                    candidate.getValue()
                );

                intervals.remove(candidate.getKey());

                candidate = intervals.ceilingEntry(mergedStart);
            }

            /*
             * Insert the final merged interval.
             */
            intervals.put(mergedStart, mergedEnd);
        }

        return answer;
    }
}

/*
 * TIME COMPLEXITY:
 *
 * O(n log n) amortized.
 *
 * Each day performs an ordered lookup in O(log n).
 *
 * More importantly, each existing interval can be removed
 * and merged only once over the entire algorithm.
 *
 * There are at most O(n) inserted intervals and O(n)
 * total removals.
 *
 * Therefore, all merging operations together cost
 * O(n log n).
 *
 * SPACE COMPLEXITY:
 *
 * O(n) for the TreeMap.
 *
 * ADVANTAGE:
 *
 * Does not depend on the maximum coordinate.
 *
 * DISADVANTAGE:
 *
 * More complicated implementation than successor DSU.
 *
 * We must carefully handle:
 * - Previous overlapping intervals.
 * - Following overlapping intervals.
 * - Adjacent intervals.
 * - Gaps between painted intervals.
 */











































class Solution {

    private static final int MAX = 50_000;

    private final int[] covered = new int[4 * MAX];

    /*
     * lazy[node] = true means the entire segment represented
     * by this node is already painted.
     */
    private final boolean[] lazy = new boolean[4 * MAX];

    public int[] amountPainted(int[][] paint) {

        int n = paint.length;
        int[] answer = new int[n];

        for (int day = 0; day < n; day++) {

            int start = paint[day][0];
            int end = paint[day][1];

            /*
             * Convert [start, end) into [start, end - 1].
             *
             * Query how many positions are already painted.
             */
            int alreadyPainted = query(
                1, 0, MAX - 1,
                start, end - 1
            );

            /*
             * Total interval length - previously painted length.
             */
            answer[day] = (end - start) - alreadyPainted;

            /*
             * Mark the entire interval as painted.
             *
             * Even if some positions were already painted,
             * assigning the whole range to painted is safe.
             */
            update(
                1, 0, MAX - 1,
                start, end - 1
            );
        }

        return answer;
    }

    /*
     * Returns the number of painted positions
     * in the intersection with [queryLeft, queryRight].
     */
    private int query(
        int node,
        int left,
        int right,
        int queryLeft,
        int queryRight
    ) {

        /*
         * No overlap.
         */
        if (right < queryLeft || left > queryRight) {
            return 0;
        }

        /*
         * Complete overlap.
         *
         * The node already stores the total number
         * of painted positions in this segment.
         */
        if (queryLeft <= left && right <= queryRight) {
            return covered[node];
        }

        /*
         * Partial overlap.
         *
         * Propagate pending full-paint information
         * before accessing children.
         */
        pushDown(node, left, right);

        int mid = left + (right - left) / 2;

        int leftResult = query(
            node * 2,
            left, mid,
            queryLeft, queryRight
        );

        int rightResult = query(
            node * 2 + 1,
            mid + 1, right,
            queryLeft, queryRight
        );

        return leftResult + rightResult;
    }

    /*
     * Paint every position in [queryLeft, queryRight].
     *
     * Range assignment, not range addition:
     * Painting an already-painted position must not
     * increase its covered count again.
     */
    private void update(
        int node,
        int left,
        int right,
        int queryLeft,
        int queryRight
    ) {

        /*
         * No overlap.
         */
        if (right < queryLeft || left > queryRight) {
            return;
        }

        /*
         * Complete overlap.
         *
         * Every position in this segment is now painted.
         */
        if (queryLeft <= left && right <= queryRight) {

            covered[node] = right - left + 1;

            lazy[node] = true;

            return;
        }

        /*
         * Partial overlap: propagate pending updates.
         */
        pushDown(node, left, right);

        int mid = left + (right - left) / 2;

        update(
            node * 2,
            left, mid,
            queryLeft, queryRight
        );

        update(
            node * 2 + 1,
            mid + 1, right,
            queryLeft, queryRight
        );

        /*
         * Recalculate the number of painted positions
         * from the two children.
         */
        covered[node] =
            covered[node * 2] +
            covered[node * 2 + 1];
    }

    /*
     * Push a pending "paint entire segment" operation
     * to both children.
     */
    private void pushDown(int node, int left, int right) {

        if (!lazy[node] || left == right) {
            return;
        }

        int mid = left + (right - left) / 2;

        int leftChild = node * 2;
        int rightChild = node * 2 + 1;

        /*
         * The left child represents [left, mid].
         */
        covered[leftChild] = mid - left + 1;
        lazy[leftChild] = true;

        /*
         * The right child represents [mid + 1, right].
         */
        covered[rightChild] = right - mid;
        lazy[rightChild] = true;

        /*
         * Parent's lazy operation has been propagated.
         */
        lazy[node] = false;
    }
}

/*
 * TIME COMPLEXITY:
 *
 * O(n log M)
 *
 * Each day performs:
 * - One range query: O(log M)
 * - One range update: O(log M)
 *
 * Therefore, total O(n log M).
 *
 * SPACE COMPLEXITY:
 *
 * O(M) for the segment tree arrays.
 *
 * WHY USE THIS APPROACH?
 *
 * Segment trees support more general range operations.
 *
 * If the problem changes to support partial coverage,
 * coverage counts, range sums, or other queries,
 * a segment tree can be adapted.
 *
 * For this particular problem, however, successor DSU
 * is simpler and avoids unnecessary range-tree overhead.
 */
