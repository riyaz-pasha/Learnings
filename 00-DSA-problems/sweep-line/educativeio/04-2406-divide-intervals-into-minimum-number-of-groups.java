/**
 * 2406. Divide Intervals Into Minimum Number of Groups
 * 
 * ============================================================================
 * 1. PROBLEM STATEMENT
 * ============================================================================
 * You are given a 2D integer array intervals where intervals[i] = [left_i, right_i] 
 * represents the inclusive interval [left_i, right_i].
 * You must divide the intervals into groups such that no two intervals in the 
 * same group intersect.
 * Return the minimum number of groups you need.
 * 
 * Note: Two intervals intersect if they share at least one common number 
 * (e.g., [1, 5] and [5, 8] DO intersect at 5).
 * 
 * Constraints:
 * - 1 <= intervals.length <= 10^5
 * - 1 <= left_i <= right_i <= 10^6
 * 
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (To ask the interviewer)
 * ============================================================================
 * 1. Are the interval boundaries strictly inclusive? 
 *    (Yes. This means [1, 5] and [5, 8] overlap, which completely flips the equality 
 *    logic compared to standard "meeting room" problems where [1,5] and [5,8] do not overlap).
 * 2. What is the maximum possible coordinate for right_i?
 *    (Constraint says 10^6. This is a massive hint that an O(MAX_COORD) array is feasible).
 * 3. Can left_i == right_i (a single point interval like [5,5])?
 *    (Yes, constraints say left <= right. A point still occupies space in a group).
 * 4. Do we need to return the actual grouping, or just the number of groups?
 *    (Just the number, which allows us to decouple the intervals and just count maximum overlaps).
 * 
 * ============================================================================
 * 3. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement & Constraint: 
 * We need to find the maximum number of overlapping intervals at any single point. 
 * If 3 intervals overlap at coordinate 'X', we absolutely need at least 3 groups 
 * to hold them. Thus, "Minimum Groups" is mathematically identical to "Maximum 
 * Concurrent Overlaps".
 * 
 * --- Approach 1: Simulation / Group Tracking (Brute Force) ---
 * 1. Idea: Maintain a list of active groups (recording the maximum right_i in each).
 *    Sort intervals by start time. Try to place each interval into an existing group.
 * 2. What I'd naturally try: `List<Integer> groups = new ArrayList<>()`. For each 
 *    interval, loop through `groups`. If `group_end < interval_start`, update that 
 *    group's end. If no group fits, add a new group.
 * 3. Why it works: It actively builds valid groups.
 * 4. Why it's too slow/costly: Finding an available group takes O(G) time where G is 
 *    the number of groups. If all intervals overlap, we create N groups, leading to O(N^2).
 * 5. What work is repeated: Scanning the same occupied groups over and over.
 * 6. The pivot observation: We just need to know the *earliest* freeing group.
 * 
 * Time Complexity: O(N^2) — checking every interval against every group.
 * Space Complexity: O(N) — storing group ends.
 * 
 * --- Approach 2: Min-Heap (Intermediate) ---
 * 1. Idea: Instead of scanning all groups, keep the group end times in a Min-Heap.
 * 2. What I'd naturally try: Sort intervals by start time. If the new interval's start 
 *    is > heap.peek() (the earliest ending group), pop the heap and push the new end 
 *    time. Otherwise, just push the new end time (creating a new group).
 * 3. Why it works: The top of the heap is always the optimal (earliest) room to reuse.
 * 4. Why it's costly: Sorting takes O(N log N), and heap operations take O(N log N). 
 *    This is optimal for unbound coordinates, but we can do better for bounded ones.
 * 5. What work is repeated: Managing a dynamic tree structure (Heap) when the coordinate 
 *    space (10^6) is small enough to map directly.
 * 
 * Time Complexity: O(N log N) — sorting array and managing the heap.
 * Space Complexity: O(N) — heap stores at most N elements.
 * 
 * --- Approach 3: Difference Array / Line Sweep (Optimal) ---
 * 1. Idea: Treat the coordinate line as an array. When an interval starts, add 1 to 
 *    active overlaps. When it completely finishes, subtract 1.
 * 2. What I'd naturally try: Array of size 10^6 + 2. For `[left, right]`, `arr[left]++`. 
 *    Crucially, because `right` is INCLUSIVE, the interval is still active AT `right`. 
 *    It only frees up at `right + 1`. So, `arr[right + 1]--`.
 * 3. Why it works: The prefix sum at any index exactly equals the number of overlapping 
 *    intervals at that exact coordinate.
 * 4. Why this is the ceiling: We just do a single O(N) pass to record changes, and a 
 *    single O(MAX_VAL) pass to find the max prefix sum. No sorting needed!
 * 
 * Time Complexity: O(N + MAX_VAL) — one pass over the N intervals, one pass over 10^6 coordinates.
 * Space Complexity: O(MAX_VAL) — a fixed-size array of 1,000,002 integers.
 * 
 * Verdict for 45-minute interview:
 * I would write the Difference Array (Approach 3). It demonstrates paying attention 
 * to constraints (10^6 is perfectly sized for an array in Java, taking ~4MB of RAM) 
 * and drastically simplifies the code while beating O(N log N) time.
 * 
 * ============================================================================
 * 4. EDGE CASES
 * ============================================================================
 * - Exact boundary overlaps: [1, 5] and [5, 8]. Because 5 is inclusive, max overlap 
 *   at 5 is 2. (Difference array handles this via `right + 1` drop-off).
 * - Single point intervals: [3, 3]. Needs a group just for that instant.
 * - Complete nesting: [1, 10], [2, 9], [3, 8]. Max groups should be 3.
 * - Disjoint intervals: [1, 2], [4, 5], [7, 8]. Max groups should be 1.
 * 
 * ============================================================================
 * 5. KEY INSIGHT, DIAGRAM & DRY RUN
 * ============================================================================
 * > Key Insight: 
 * "Inclusive boundaries push the drop-off to +1." 
 * If a person rents a room from day 1 to day 5 (inclusive), the room is NOT free on day 5. 
 * It is only free on day 6. Thus, your difference array event is `-1 at index 6`.
 * 
 * Diagram: Difference Array for [[1, 5], [5, 8]]
 * Coord:    1   2   3   4   5   6   7   8   9
 * Changes: +1               +1 -1          -1
 * P-Sum:    1   1   1   1   2   1   1   1   0
 * 
 * See how coordinate 5 correctly registers 2 overlapping intervals!
 * 
 * Dry Run (Optimal Approach):
 * Intervals = [[1,3],[5,6],[8,10],[11,13]]
 * Sweep Array initialized to 0.
 * 
 * 1. [1, 3]: arr[1] += 1, arr[4] -= 1
 * 2. [5, 6]: arr[5] += 1, arr[7] -= 1
 * 3. [8, 10]: arr[8] += 1, arr[11] -= 1
 * 4. [11, 13]: arr[11] += 1, arr[14] -= 1
 * 
 * Prefix Sum Sweep:
 * arr[1]: 1 (Max: 1)
 * arr[4]: 1 - 1 = 0
 * arr[5]: 0 + 1 = 1 (Max: 1)
 * arr[7]: 1 - 1 = 0
 * arr[8]: 0 + 1 = 1 (Max: 1)
 * arr[11]: 0 + 1(from [11,13]) - 1(from [8,10]) = 0 (Wait, 8-10 ends at 11, 11-13 starts at 11)
 * Let's look closer at index 11: 
 * prefix sum before 11 was 1 (from index 8). 
 * At 11, arr[11] has (+1 from start, -1 from end) = 0 change. 
 * So at 11, prefix sum remains 1! Max groups = 1.
 * 
 * ============================================================================
 * 6. PITFALLS & PATTERN RECOGNITION
 * ============================================================================
 * Pitfalls:
 * - Off-by-one on Difference Array: Doing `sweep[right]--` instead of `sweep[right + 1]--`.
 *   This treats intervals as exclusive, causing [1,5] and [5,8] to falsely report 1 group.
 * - Array Out of Bounds: If `right_i` can be 10^6, `right_i + 1` is 1,000,001. 
 *   You MUST allocate an array of size at least 1,000,002.
 * - Two-Pointers Equality trap: If using Two-Pointers, `starts[s] <= ends[e]` MUST 
 *   evaluate to TRUE (overlap) for equality, meaning you process the start before the end.
 *   In Meeting Rooms II (exclusive), it was `starts[s] < ends[e]`.
 * 
 * Pattern Recognition: 
 * "Max Overlaps + Small Coordinate Bounds = Difference Array of size MAX_COORD."
 * 
 * ============================================================================
 * 7. INTERVIEW SCRIPT
 * ============================================================================
 * "The problem asks for the minimum groups so intervals don't intersect. This is 
 * exactly equivalent to finding the maximum number of intervals that overlap at 
 * any single point. Since the maximum coordinate is bounded at 1 million, we can 
 * completely avoid O(N log N) sorting. I'll use a Line Sweep with a Difference Array. 
 * For every interval, I'll add 1 to its start coordinate. Because the interval is 
 * inclusive, it only frees up the group at `right + 1`, so I'll subtract 1 at 
 * `right + 1`. Finally, a single pass calculating the prefix sum will reveal the 
 * peak number of active intervals. This gives O(N + MAX_COORD) time and O(MAX_COORD) space."
 * 
 * ============================================================================
 * 8. FOLLOW-UPS
 * ============================================================================
 * Q: What if the interval coordinates could be up to 10^9?
 * A: We can't use an array of size 10^9 (wastes too much memory). We would have 
 *    two choices:
 *    1. Two Arrays: Extract all starts into one array, all ends into another. 
 *       Sort both. Walk through with two pointers. Time: O(N log N), Space: O(N).
 *    2. TreeMap: Use `map.put(left, get+1)` and `map.put(right+1, get-1)`. 
 *       Iterate through the values. Time: O(N log N), Space: O(N).
 */

import java.util.Arrays;

public class MinimumGroups {

    /**
     * APPROACH 1: Difference Array (Optimal for bounded coordinates)
     * Time Complexity: O(N + MAX_VAL) — N to map intervals, MAX_VAL to sweep.
     * Space Complexity: O(MAX_VAL) — Array of size 1,000,002.
     */
    public int minGroupsSweep(int[][] intervals) {
        // The maximum possible right coordinate is 10^6.
        // We need right + 1 for the drop-off, which is 1,000,001.
        // Array size 1,000,002 ensures index 1,000,001 exists safely.
        int[] sweep = new int[1000002];
        
        // 1. Record the boundary events
        for (int[] interval : intervals) {
            int left = interval[0];
            int right = interval[1];
            
            // Interval starts -> group claimed
            sweep[left]++;
            
            // Interval ends -> group is freed the moment AFTER 'right'
            // This guarantees [1, 5] and [5, 8] overlap at 5.
            sweep[right + 1]--;
        }
        
        // 2. Sweep to find maximum concurrent overlaps
        int maxGroups = 0;
        int activeIntervals = 0;
        
        for (int i = 0; i < sweep.length; i++) {
            activeIntervals += sweep[i];
            if (activeIntervals > maxGroups) {
                maxGroups = activeIntervals;
            }
        }
        
        return maxGroups;
    }

    /**
     * APPROACH 2: Two Pointers (Optimal if coordinates were unbounded, e.g. 10^9)
     * Time Complexity: O(N log N) — Due to sorting the arrays.
     * Space Complexity: O(N) — Auxiliary arrays for starts and ends.
     */
    public int minGroupsTwoPointers(int[][] intervals) {
        int n = intervals.length;
        int[] starts = new int[n];
        int[] ends = new int[n];
        
        // 1. Decouple starts and ends
        for (int i = 0; i < n; i++) {
            starts[i] = intervals[i][0];
            ends[i] = intervals[i][1];
        }
        
        // 2. Sort chronologically
        Arrays.sort(starts);
        Arrays.sort(ends);
        
        int s = 0, e = 0;
        int activeGroups = 0, maxGroups = 0;
        
        // 3. Walk through events
        while (s < n) {
            // CRUCIAL DIFFERENCE from Meeting Rooms II:
            // If starts[s] == ends[e], the new interval starts at the exact 
            // same coordinate the old one ends. Because boundaries are INCLUSIVE,
            // they OVERLAP. We must process the start (+) before the end (-).
            // Hence, '<=' instead of '<'.
            if (starts[s] <= ends[e]) {
                activeGroups++;
                s++;
            } else {
                activeGroups--;
                e++;
            }
            
            maxGroups = Math.max(maxGroups, activeGroups);
        }
        
        return maxGroups;
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================
    public static void main(String[] args) {
        MinimumGroups solution = new MinimumGroups();
        
        // Test Case 1: Standard case with extensive overlapping
        int[][] intervals1 = {{5, 10}, {6, 8}, {1, 5}, {2, 3}, {1, 10}};
        System.out.println("Test 1 (Expected 3): " 
            + solution.minGroupsSweep(intervals1));
            
        // Test Case 2: Non-overlapping consecutive meetings
        int[][] intervals2 = {{1, 3}, {5, 6}, {8, 10}, {11, 13}};
        System.out.println("Test 2 (Expected 1): " 
            + solution.minGroupsSweep(intervals2));
            
        // Test Case 3: Edge Case - Exact boundary touch (Inclusive Overlap)
        // [1, 5] and [5, 8] overlap at 5. Should require 2 groups.
        int[][] intervals3 = {{1, 5}, {5, 8}};
        System.out.println("Test 3 (Expected 2): " 
            + solution.minGroupsSweep(intervals3));
            
        // Test Case 4: Fully nested and single point intervals
        int[][] intervals4 = {{1, 10}, {3, 3}, {3, 8}};
        System.out.println("Test 4 (Expected 3): " 
            + solution.minGroupsSweep(intervals4));
            
        // Cross-checking approaches to ensure identical behavior
        System.out.println("\nCross-checking approaches on Test 3 (Boundary Touch):");
        System.out.println("Difference Array: " + solution.minGroupsSweep(intervals3));
        System.out.println("Two Pointers:     " + solution.minGroupsTwoPointers(intervals3));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern: Difference Array (Line Sweep) for bounded coordinates.
 * 
 * Key observation: Inclusive interval bounds mean that if an interval ends at 
 * 'right', it is still occupying the group at that coordinate. The drop-off 
 * (where the group is finally freed) must occur at 'right + 1'.
 * 
 * Memorize vs Understand: 
 * UNDERSTAND how inclusive vs exclusive boundaries affect logic.
 * Exclusive (Meeting Rooms): arr[right]--, or (start < end)
 * Inclusive (This problem): arr[right + 1]--, or (start <= end)
 * 
 * Most common trap: Array out of bounds exception by sizing the array to exactly 
 * the max constraint (10^6) without leaving room for the `right + 1` drop-off.
 * 
 * One-line mental trigger: "Find max inclusive overlaps? sweep[left]++, sweep[right+1]--."
 */









































import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class SolutionSweepLine {

    // Immutable data carrier for one event.
    // delta = +1 means an interval starts.
    // delta = -1 means an interval ends.
    private record Event(int position, int delta) {}

    public int minGroups(int[][] intervals) {
        List<Event> events = new ArrayList<>(2 * intervals.length);

        for (int[] interval : intervals) {
            int left = interval[0];
            int right = interval[1];

            events.add(new Event(left, +1));
            events.add(new Event(right, -1));
        }

        // Sort by coordinate.
        // For equal coordinates, +1 must come before -1 because the
        // intervals are inclusive and both contain the shared endpoint.
        events.sort(
            Comparator.comparingInt(Event::position)
                .thenComparingInt(Event::delta).reversed()
        );

        // IMPORTANT:
        // Reversing the entire comparator above reverses BOTH fields.
        // Although this puts +1 before -1, it also sorts coordinates
        // in descending order, which is incorrect for a sweep line.
        //
        // Use a comparator that sorts coordinates ascending and only
        // reverses the tie-breaker for delta instead.
        events.sort(
            Comparator.comparingInt(Event::position)
                .thenComparing(
                    Comparator.comparingInt(Event::delta).reversed()
                )
        );

        int active = 0;
        int maxOverlap = 0;

        for (Event event : events) {
            // Apply each event in the specified order.
            active += event.delta();

            // The maximum number of simultaneous intervals is the
            // minimum number of groups required.
            maxOverlap = Math.max(maxOverlap, active);
        }

        // Time: O(n log n)
        // - There are 2n events.
        // - Sorting 2n events costs O(n log n).
        // - Scanning the events costs O(n).
        //
        // Auxiliary space: O(n)
        // - Two events are stored for every interval.
        return maxOverlap;
    }
}








































class SolutionDifferenceArray {

    public int minGroups(int[][] intervals) {
        // The maximum coordinate is 10^6, so right + 1 is at most
        // 1,000,001. Allocate one additional slot for that decrement.
        final int MAX_COORDINATE = 1_000_000;
        int[] difference = new int[MAX_COORDINATE + 2];

        for (int[] interval : intervals) {
            int left = interval[0];
            int right = interval[1];

            // Start contributing at left.
            difference[left]++;

            // Stop contributing AFTER right.
            // This right + 1 update is what preserves inclusive endpoints.
            difference[right + 1]--;
        }

        int active = 0;
        int answer = 0;

        for (int position = 1;
             position <= MAX_COORDINATE;
             position++) {

            // Prefix sum reconstructs the number of intervals containing
            // the current integer coordinate.
            active += difference[position];

            answer = Math.max(answer, active);
        }

        // Time: O(n + M), where M = 10^6 is the maximum coordinate.
        // - n updates, followed by a scan of the coordinate range.
        //
        // Auxiliary space: O(M), for the difference array.
        //
        // This is attractive when the coordinate range is small and known.
        // It is less general than the heap and sweep-line solutions:
        // if coordinates are huge, an array of size M is impractical.
        return answer;
    }
}
