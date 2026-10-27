/**
 * 759. Employee Free Time (LeetCode Premium)
 * 
 * ============================================================================
 * 1. PROBLEM STATEMENT
 * ============================================================================
 * We are given a list `schedule` of employees, which represents the working time 
 * for each employee.
 * Each employee has a list of non-overlapping Intervals, and these intervals 
 * are in sorted order by start time.
 * 
 * Return the list of finite intervals representing common, positive-length free 
 * time for all employees, also in sorted order.
 * 
 * Example 1:
 * Input: schedule = [[[1,2],[5,6]], [[1,3]], [[4,10]]]
 * Output: [[3,4]]
 * Explanation:
 * There are a total of three employees, and all common free time intervals 
 * would be [-inf, 1], [3, 4], [10, inf]. We discard any intervals that contain 
 * inf as they aren't finite.
 * 
 * Constraints:
 * - 1 <= schedule.length , schedule[i].length <= 50
 * - 0 <= schedule[i].start < schedule[i].end <= 10^8
 * 
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (To ask the interviewer)
 * ============================================================================
 * 1. Are the intervals for an individual employee strictly non-overlapping and sorted?
 *    (Prevents us from assuming we need to merge intervals within an employee's schedule first).
 * 2. Does a free time interval of length 0 count? (e.g., one shift ends at 5, another starts at 5).
 *    (Clarifies inequality strictness: usually, [5,5] is not a valid positive-length free time).
 * 3. How should we handle the boundaries (infinity)? 
 *    (Confirms that we only return "finite" intervals bounded by the earliest overall start and latest overall end).
 * 4. Can an employee's schedule be empty?
 *    (Ensures we handle empty lists gracefully to avoid NullPointers or IndexOutOfBounds).
 * 5. Are the start and end times small enough for an array, or do they get massive?
 *    (Constrains us: times up to 10^8 mean a fixed-size Difference Array will cause OutOfMemory, forcing sorting/heaps).
 * 
 * ============================================================================
 * 3. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement & Constraint: 
 * We need to find the "gaps" in the union of all intervals. 
 * The times can reach 10^8, so we cannot use a Line Sweep with a massive fixed array. 
 * We must use interval merging.
 * 
 * --- Approach 1: Flatten and Sort (The "Naive" but Acceptable Approach) ---
 * 1. Idea: Ignore the fact that individual employee schedules are already sorted. 
 *    Just dump every single interval into one giant list and sort it by start time.
 * 2. What I'd naturally try: `List<Interval> all = new ArrayList<>();` add everything, 
 *    `Collections.sort(all, (a,b)->a.start-b.start)`. Then iterate through keeping 
 *    track of the `maxEnd` seen so far. If the next interval starts after `maxEnd`, 
 *    we found a gap!
 * 3. Why it works: Once all intervals are sorted globally, the problem reduces to 
 *    Merge Intervals (LeetCode 56). Any gap between merged intervals is free time.
 * 4. Why it's suboptimal: We are doing a full O(N log N) sort on all intervals, 
 *    completely ignoring the fact that each employee's sublist is *already* sorted.
 * 5. What work is repeated: We are comparing intervals against each other that 
 *    were already in proper relative order within their original employee lists.
 * 6. The pivot observation: We have K sorted lists. When we need to process elements 
 *    from K sorted lists in chronological order, we can use a Min-Heap (K-Way Merge) 
 *    to combine them faster than a full sort.
 *    
 * Time Complexity: O(N log N) — because we dump all N intervals into a list and sort them from scratch.
 * Space Complexity: O(N) — because we create a new flattened list of size N to hold all intervals.
 * 
 * --- Approach 2: Min-Heap / K-Way Merge (Optimal) ---
 * 1. Idea: Keep a Min-Heap of size K (one interval per employee). Pop the earliest 
 *    interval, update our `maxEnd` watermark, and if there's a gap, record it. 
 *    Then push the next interval from that *same* employee into the heap.
 * 2. What I'd naturally try: `PriorityQueue<int[]> pq`. Store `[start_time, employee_idx, interval_idx]`. 
 *    Initialize the heap with the first interval of every employee.
 * 3. Why it works: The heap always serves up the next chronological interval across 
 *    the entire company, allowing us to sweep through time while only keeping K elements 
 *    in the heap instead of N.
 * 4. Why this is the ceiling: We process each of the N intervals exactly once, and 
 *    each insertion/extraction into the heap of size K takes O(log K). 
 * 
 * Time Complexity: O(N log K) — because we process N total intervals, and the heap never exceeds size K (number of employees).
 * Space Complexity: O(K) — because the Priority Queue only stores at most K elements at any given time (excluding the result list).
 * 
 * Verdict for 45-minute interview:
 * Start by verbally explaining Approach 1 (Flatten + Sort) as it proves you know how to solve it immediately. 
 * Then propose Approach 2 (Min-Heap) as the optimization. I would write Approach 2. 
 * It shows mastery of advanced data structure patterns (K-Way Merge) and scales perfectly 
 * for large data streams where employees might have thousands of shifts.
 * 
 * ============================================================================
 * 4. EDGE CASES
 * ============================================================================
 * - Continuous shifts: [[1, 2], [2, 3], [3, 4]]. `maxEnd` perfectly aligns with the next `start`.
 *   Since 2 == 2, no gap is formed. Correctly yields no free time.
 * - Single employee: Just one list of intervals. The heap size is 1, and the gaps between 
 *   their shifts are correctly identified.
 * - Fully overlapping shifts: All employees work [0, 100]. Heap processes all, `maxEnd` 
 *   stays 100, no gaps found.
 * - Disjoint shifts across employees: Emp1 works [1,2], Emp2 works [3,4], Emp3 works [5,6].
 *   Creates multiple gaps: [2,3] and [4,5].
 * 
 * ============================================================================
 * 5. KEY INSIGHT, DIAGRAM & DRY RUN
 * ============================================================================
 * > Key Insight: 
 * "Free time is just the inverse of Merged Intervals."
 * If you merge all working hours together into solid blocks of time, the spaces 
 * between those solid blocks are the finite free times. Tracking the `maxEnd` 
 * is the only state you need to find when a block ends and a new one hasn't started.
 * 
 * ASCII Diagram: K-Way Merge
 * Emp 0: [1,2]       [5,6]
 * Emp 1: [1,3]
 * Emp 2:       [4,10]
 * 
 * Heap pops chronologically:
 * 1. [1,2] -> maxEnd = 2
 * 2. [1,3] -> starts at 1 (<= 2). No gap. maxEnd = max(2,3) = 3.
 * 3. [4,10]-> starts at 4 (> 3). GAP FOUND: [3, 4]. maxEnd = 10.
 * 4. [5,6] -> starts at 5 (<= 10). No gap. maxEnd = max(10,6) = 10.
 * Result: [[3, 4]]
 * 
 * Dry Run (Optimal Approach):
 * schedule = [[[1,2],[5,6]], [[1,3]]]
 * PQ = [ (start=1, e=0, i=0), (start=1, e=1, i=0) ]
 * maxEnd = -1 (uninitialized)
 * 
 * Pop 1: (start=1, e=0, i=0) -> Interval [1,2]. 
 *   maxEnd is -1, so set maxEnd = 2.
 *   Push next for e=0: (start=5, e=0, i=1) -> Interval [5,6].
 *   PQ = [ (1, 1, 0), (5, 0, 1) ]
 * 
 * Pop 2: (start=1, e=1, i=0) -> Interval [1,3].
 *   start (1) <= maxEnd (2). No gap.
 *   update maxEnd = max(2, 3) = 3.
 *   Next for e=1? None.
 *   PQ = [ (5, 0, 1) ]
 * 
 * Pop 3: (start=5, e=0, i=1) -> Interval [5,6].
 *   start (5) > maxEnd (3). GAP! Add [3, 5] to result.
 *   update maxEnd = max(3, 6) = 6.
 *   Next for e=0? None.
 *   PQ = empty.
 * 
 * Return [[3, 5]].
 * 
 * ============================================================================
 * 6. PITFALLS & PATTERN RECOGNITION
 * ============================================================================
 * Pitfalls:
 * - Forgetting to use `Math.max(maxEnd, current.end)`. If a completely nested interval 
 *   is processed (e.g., [1, 10] then [2, 3]), `maxEnd` must remain 10. If you just set 
 *   it to the current interval's end (3), you will falsely report a gap starting at 3.
 * - Comparing the current interval against the *previous* interval instead of `maxEnd`. 
 *   Multiple intervals can overlap and extend the boundary independently.
 * 
 * Pattern Recognition: 
 * "K Sorted Lists -> Min-Heap."
 * When the input is a list of lists, and EACH internal list is already sorted, 
 * immediately think of K-Way Merge (LeetCode 23: Merge k Sorted Lists).
 * 
 * ============================================================================
 * 7. INTERVIEW SCRIPT
 * ============================================================================
 * "To find the common free time, we essentially need to merge all the working intervals 
 * and look for the gaps between those merged blocks. A brute force way is to flatten 
 * all intervals into a single array and sort them by start time, which costs O(N log N).
 * 
 * But we can do better. We are given K lists of intervals, and each is already sorted. 
 * This perfectly fits the K-Way Merge pattern. I'll use a Min-Heap holding the next 
 * available interval for each employee. By continually popping the earliest starting 
 * interval, we sweep through time. I'll maintain a `maxEnd` variable representing the 
 * furthest working hour we've seen so far. If I pop an interval that starts strictly 
 * after `maxEnd`, I've found a pocket of free time! This brings our time complexity 
 * down to O(N log K) and space down to O(K) for the heap."
 * 
 * ============================================================================
 * 8. FOLLOW-UPS
 * ============================================================================
 * Q: What if there are thousands of employees, but the schedule lists are too massive 
 *    to fit in memory simultaneously?
 * A: The K-Way Merge shines here. Since the heap only holds K elements (one per employee), 
 *    we don't need to load the entire lists into memory. We can use iterators/streams 
 *    that pull the next interval from disk or a database as needed. The space remains O(K).
 * 
 * Q: What if employees are in different time zones?
 * A: We would need a preprocessing step to normalize all intervals to UTC/GMT before 
 *    inserting them into the heap.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

// Mock Interval class required by the problem
class Interval {
    public int start;
    public int end;
    public Interval() {}
    public Interval(int _start, int _end) {
        start = _start;
        end = _end;
    }
    // Added toString for easy testing output
    @Override
    public String toString() {
        return "[" + start + ", " + end + "]";
    }
}

public class EmployeeFreeTime {

    /**
     * APPROACH 1: Flatten and Sort (Suboptimal but straightforward)
     * Time Complexity: O(N log N)
     * Space Complexity: O(N)
     */
    public List<Interval> employeeFreeTimeFlatten(List<List<Interval>> schedule) {
        List<Interval> allIntervals = new ArrayList<>();
        
        // Dump everything into one giant list
        for (List<Interval> employee : schedule) {
            allIntervals.addAll(employee);
        }
        
        // Sort globally by start time
        Collections.sort(allIntervals, (a, b) -> Integer.compare(a.start, b.start));
        
        List<Interval> freeTime = new ArrayList<>();
        if (allIntervals.isEmpty()) return freeTime;
        
        int maxEnd = allIntervals.get(0).end;
        
        // Sweep and find gaps
        for (int i = 1; i < allIntervals.size(); i++) {
            Interval current = allIntervals.get(i);
            
            if (current.start > maxEnd) {
                // Gap found! The space between the furthest end we've seen and this start.
                freeTime.add(new Interval(maxEnd, current.start));
            }
            
            // Expand our merged boundary if needed
            maxEnd = Math.max(maxEnd, current.end);
        }
        
        return freeTime;
    }

    /**
     * APPROACH 2: Min-Heap / K-Way Merge (Optimal)
     * Time Complexity: O(N log K) 
     * Space Complexity: O(K) 
     */
    public List<Interval> employeeFreeTimeHeap(List<List<Interval>> schedule) {
        List<Interval> freeTime = new ArrayList<>();
        
        // Heap stores int array: [interval_start, employee_index, interval_index]
        // Sorting by interval_start ensures we process chronologically
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
        
        // 1. Initialize heap with the first interval of every employee
        for (int empIdx = 0; empIdx < schedule.size(); empIdx++) {
            List<Interval> empSchedule = schedule.get(empIdx);
            if (!empSchedule.isEmpty()) {
                pq.offer(new int[] {empSchedule.get(0).start, empIdx, 0});
            }
        }
        
        if (pq.isEmpty()) return freeTime;
        
        // Initialize maxEnd to the earliest starting interval
        int[] first = pq.peek();
        int maxEnd = schedule.get(first[1]).get(first[2]).end;
        
        // 2. K-Way Merge Sweep
        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int empIdx = current[1];
            int intervalIdx = current[2];
            Interval interval = schedule.get(empIdx).get(intervalIdx);
            
            // If this interval starts AFTER the furthest working block we've seen,
            // we have found a common pocket of free time.
            if (interval.start > maxEnd) {
                freeTime.add(new Interval(maxEnd, interval.start));
            }
            
            // Update our watermark to the furthest end time seen so far
            maxEnd = Math.max(maxEnd, interval.end);
            
            // 3. Push the next chronological interval for this specific employee
            if (intervalIdx + 1 < schedule.get(empIdx).size()) {
                Interval nextInterval = schedule.get(empIdx).get(intervalIdx + 1);
                pq.offer(new int[] {nextInterval.start, empIdx, intervalIdx + 1});
            }
        }
        
        return freeTime;
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================
    public static void main(String[] args) {
        EmployeeFreeTime solution = new EmployeeFreeTime();
        
        // Helper to construct inputs quickly
        // Test 1: [[[1,2],[5,6]], [[1,3]], [[4,10]]]
        List<List<Interval>> schedule1 = new ArrayList<>();
        schedule1.add(List.of(new Interval(1, 2), new Interval(5, 6)));
        schedule1.add(List.of(new Interval(1, 3)));
        schedule1.add(List.of(new Interval(4, 10)));
        
        System.out.println("Test 1 (Expected [[3, 4]]): " 
            + solution.employeeFreeTimeHeap(schedule1));
            
        // Test 2: [[[1,3],[6,7]], [[2,4]], [[2,5],[9,12]]]
        List<List<Interval>> schedule2 = new ArrayList<>();
        schedule2.add(List.of(new Interval(1, 3), new Interval(6, 7)));
        schedule2.add(List.of(new Interval(2, 4)));
        schedule2.add(List.of(new Interval(2, 5), new Interval(9, 12)));
        
        System.out.println("Test 2 (Expected [[5, 6], [7, 9]]): " 
            + solution.employeeFreeTimeHeap(schedule2));
            
        // Test 3 (Edge Case): Nested intervals [[[1,10]], [[2,3], [4,5]]]
        List<List<Interval>> schedule3 = new ArrayList<>();
        schedule3.add(List.of(new Interval(1, 10)));
        schedule3.add(List.of(new Interval(2, 3), new Interval(4, 5)));
        
        System.out.println("Test 3 (Expected []): " 
            + solution.employeeFreeTimeHeap(schedule3));
            
        // Cross-checking the two approaches
        System.out.println("\nCross-checking approaches on Test 2:");
        System.out.println("Flatten & Sort: " + solution.employeeFreeTimeFlatten(schedule2));
        System.out.println("K-Way Merge:    " + solution.employeeFreeTimeHeap(schedule2));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern: K-Way Merge (Min-Heap).
 * 
 * Key observation: The schedules are given as K individual lists that are ALREADY 
 * SORTED. Exploiting this pre-sorted state transforms the problem from an 
 * O(N log N) generic interval merge into a much faster O(N log K) stream processing.
 * 
 * Memorize vs Understand: 
 * UNDERSTAND the `maxEnd` logic. A gap only exists if a new interval's start is 
 * greater than the MAXIMUM end time seen across all processed intervals. Just checking
 * against the immediate prior interval is not enough.
 * 
 * Most common trap: Setting `maxEnd = current.end`. Always use `Math.max()` to 
 * avoid shrinking your known working hours when a small, fully nested shift occurs.
 * 
 * One-line mental trigger: "K sorted lists? Min-heap with [start, list_idx, item_idx]."
 */





























import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class Solution {

    public List<Interval> employeeFreeTime(
            List<List<Interval>> schedule) {

        // STEP 1: Combine every employee's busy intervals into one list.
        //
        // WHY?
        // We need times when EVERY employee is free. Equivalently,
        // we need the gaps between the UNION of everyone's busy intervals.
        //
        // Example:
        // Employee A: [1, 3], [6, 7]
        // Employee B: [2, 4]
        //
        // Combined: [1, 3], [6, 7], [2, 4]
        //
        // Employee identity no longer matters after flattening.
        // We copy interval references into a new list; we don't modify
        // the original employee schedules.
        List<Interval> intervals = new ArrayList<>();

        for (List<Interval> employee : schedule) {
            intervals.addAll(employee);
        }

        List<Interval> freeTimes = new ArrayList<>();

        // Defensive handling: there is no finite gap to return if
        // there are no busy intervals at all.
        if (intervals.isEmpty()) {
            return freeTimes;
        }

        // STEP 2: Sort intervals by start time.
        //
        // WHY?
        // Sorting lets us process busy periods from left to right.
        // After sorting, we can determine whether the next interval
        // extends the current busy period or begins after a free gap.
        //
        // N = total number of intervals across all employees.
        // Time: O(N log N).
        intervals.sort(Comparator.comparingInt(a -> a.start));

        // STEP 3: Track the end of the combined busy period.
        //
        // We do NOT need to build a separate list of merged intervals.
        // We only need the end of the current merged busy period.
        //
        // Example: [1, 5] and [2, 8] combine into [1, 8].
        int lastBusyEnd = intervals.getFirst().end;

        // The first interval initializes the busy period.
        // Start scanning from the second interval.
        for (int i = 1; i < intervals.size(); i++) {
            Interval current = intervals.get(i);

            // CASE 1: Current interval overlaps or touches busy time.
            //
            // Example:
            // Current busy period: [1, 5]
            // Next interval:       [3, 8]
            //
            // They combine into [1, 8]. There is no common free time
            // between them because at least one employee is busy
            // throughout the combined period.
            //
            // Math.max is essential:
            // [1, 10] followed by [2, 4] must still end at 10.
            if (current.start <= lastBusyEnd) {
                lastBusyEnd = Math.max(lastBusyEnd, current.end);
            }

            // CASE 2: Current interval starts strictly after busy time.
            //
            // Example:
            // Previous busy period ends at 5.
            // Next busy interval starts at 8.
            //
            // Since all intervals are sorted, every interval starting
            // before 8 has already been processed. None extends past 5.
            // Therefore, [5, 8] is a common free interval.
            else {
                freeTimes.add(new Interval(lastBusyEnd, current.start));

                // We have finished the previous busy period.
                // Begin tracking the next one.
                lastBusyEnd = current.end;
            }
        }

        // We intentionally do not add time before the first busy interval
        // or after the last busy interval: only finite free intervals count.
        //
        // Time: O(N log N), dominated by sorting.
        // Space: O(N) for the flattened list and O(F) for the result,
        // where F is the number of free intervals.
        return freeTimes;
    }
}









import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class Solution {

    public List<Interval> employeeFreeTime(
            List<List<Interval>> schedule) {

        /*
         * STEP 1: Flatten every employee's busy intervals into one list.
         *
         * WHY?
         * Common free time depends on all employees together.
         * We don't need to know which employee owns an interval;
         * we only need to know when someone is busy.
         *
         * We store references to the input intervals and never mutate them.
         *
         * Let N = total number of intervals across all employees.
         * Flattening takes O(N) time and O(N) additional space.
         */
        List<Interval> all = new ArrayList<>();

        for (List<Interval> employee : schedule) {
            all.addAll(employee);
        }

        /*
         * STEP 2: Sort all intervals by their start time.
         *
         * WHY?
         * When intervals are processed chronologically, every interval
         * that could overlap the current busy range has an earlier
         * or equal start time and has already been considered.
         *
         * This lets us detect a free gap using only one running endpoint.
         *
         * Time: O(N log N)
         */
        all.sort(Comparator.comparingInt(interval -> interval.start));

        /*
         * STEP 3: Sweep the sorted intervals.
         *
         * lastBusyEnd represents the farthest endpoint reached by
         * the union of all busy intervals processed so far.
         *
         * We don't need to store merged intervals separately.
         * We can produce free intervals as soon as we discover a gap.
         */
        List<Interval> freeTimes = new ArrayList<>();

        // LeetCode guarantees a non-empty schedule, but this guard
        // makes the method safe for an empty outer list as well.
        if (all.isEmpty()) {
            return freeTimes;
        }

        // The first interval establishes the initial busy range.
        int lastBusyEnd = all.getFirst().end;

        for (int i = 1; i < all.size(); i++) {
            Interval current = all.get(i);

            /*
             * CASE 1: Overlapping or touching busy intervals.
             *
             * Example:
             * Existing coverage: [1, 5]
             * Current interval:  [3, 7]
             *
             * The combined busy range becomes [1, 7].
             *
             * Use MAX because a nested interval must not shorten
             * the endpoint:
             * [1, 10] followed by [3, 4] still ends at 10.
             *
             * Touching intervals also have no positive-length gap:
             * [1, 5] and [5, 8] leave no free time between them.
             */
            if (current.start <= lastBusyEnd) {
                lastBusyEnd = Math.max(lastBusyEnd, current.end);
            } else {
                /*
                 * CASE 2: A positive-length gap exists.
                 *
                 * Example:
                 * Existing busy coverage ends at 5.
                 * The next interval starts at 8.
                 *
                 * No employee is busy during [5, 8], so this is
                 * common free time.
                 *
                 * Why is this gap safe to report?
                 * All intervals were sorted by start time. Every
                 * interval starting before 8 has already been
                 * processed, and none extends beyond time 5.
                 *
                 * Record the gap, then begin tracking the next
                 * busy range. The current interval is disjoint from
                 * the previous range, so its end becomes the new end.
                 */
                freeTimes.add(
                    new Interval(lastBusyEnd, current.start)
                );

                lastBusyEnd = current.end;
            }
        }

        /*
         * STEP 4: Return the result.
         *
         * We intentionally do not add free time before the first
         * busy interval or after the final busy interval.
         *
         * The problem asks only for finite common free intervals.
         *
         * Time for the sweep: O(N)
         * Auxiliary space beyond the flattened input and output: O(1)
         *
         * Total time: O(N log N)
         * Total space: O(N), including the flattened list and output.
         */
        return freeTimes;
    }
}



















































import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

class Solution {

    // Identifies one interval in one employee's schedule.
    //
    // employee: which employee owns this interval
    // index:    position of the interval in that employee's schedule
    //
    // A record provides an immutable, compact data carrier.
    private record Cursor(int employee, int index) {}

    public List<Interval> employeeFreeTime(
            List<List<Interval>> schedule) {

        List<Interval> freeTimes = new ArrayList<>();

        // STEP 1: Create a min-heap ordered by interval start time.
        //
        // The heap stores at most ONE interval per employee.
        // Each Cursor points to the next interval to process.
        //
        // For each cursor, retrieve its interval and compare start times.
        // The earliest available interval is always at the heap's top.
        PriorityQueue<Cursor> minHeap = new PriorityQueue<>(
            Comparator.comparingInt(cursor ->
                schedule.get(cursor.employee())
                        .get(cursor.index()).start)
        );

        // STEP 2: Insert the first interval of every non-empty schedule.
        //
        // Because each employee's intervals are sorted, we can reveal
        // that employee's remaining intervals one at a time.
        for (int employee = 0; employee < schedule.size(); employee++) {
            if (!schedule.get(employee).isEmpty()) {
                minHeap.offer(new Cursor(employee, 0));
            }
        }

        // No intervals means there are no finite gaps to report.
        if (minHeap.isEmpty()) {
            return freeTimes;
        }

        // STEP 3: Initialize the first busy period using the earliest
        // interval in the heap.
        //
        // Removing an interval also allows us to insert the next interval
        // from the same employee.
        Cursor first = minHeap.poll();
        Interval firstInterval = schedule.get(first.employee())
                                         .get(first.index());

        int lastBusyEnd = firstInterval.end;

        // Advance the employee whose first interval we just consumed.
        int nextIndex = first.index() + 1;
        if (nextIndex < schedule.get(first.employee()).size()) {
            minHeap.offer(new Cursor(first.employee(), nextIndex));
        }

        // STEP 4: Repeatedly process the globally earliest remaining
        // interval. The heap maintains chronological order for us.
        while (!minHeap.isEmpty()) {
            Cursor cursor = minHeap.poll();

            Interval current = schedule.get(cursor.employee())
                                       .get(cursor.index());

            // If this interval overlaps or touches the current busy
            // period, extend the period to the farthest endpoint.
            if (current.start <= lastBusyEnd) {
                lastBusyEnd = Math.max(lastBusyEnd, current.end);
            } else {
                // A gap exists between the previous busy period and
                // the current interval. No employee is busy in this gap.
                freeTimes.add(new Interval(lastBusyEnd, current.start));

                // Start tracking the current, separate busy period.
                lastBusyEnd = current.end;
            }

            // Advance only the employee whose interval was processed.
            //
            // This keeps at most one pending interval per employee.
            // Since the employee's schedule is sorted, its next interval
            // is the next candidate that employee can contribute.
            int next = cursor.index() + 1;
            List<Interval> employeeSchedule = schedule.get(cursor.employee());

            if (next < employeeSchedule.size()) {
                minHeap.offer(new Cursor(cursor.employee(), next));
            }
        }

        // N intervals are each inserted and removed once.
        //
        // K = number of employees.
        // Time: O(N log K).
        // Auxiliary space: O(K) for the heap, excluding output.
        return freeTimes;
    }
}



import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

class Solution {

    /*
     * A heap entry identifies the next interval to process:
     *
     * employeeIndex = which employee owns the interval
     * intervalIndex = which interval within that employee's schedule
     *
     * A record is immutable and makes a good compact data carrier.
     */
    private record Cursor(int employeeIndex, int intervalIndex) {}

    public List<Interval> employeeFreeTime(
            List<List<Interval>> schedule) {

        List<Interval> freeTimes = new ArrayList<>();

        /*
         * The heap contains at most one next-unprocessed interval
         * per employee.
         *
         * The comparator looks up each cursor's interval and orders
         * cursors by interval start time.
         *
         * Thus, peek()/poll() gives the globally earliest remaining
         * interval without sorting all N intervals.
         */
        PriorityQueue<Cursor> minHeap = new PriorityQueue<>(
            Comparator.comparingInt(cursor ->
                schedule.get(cursor.employeeIndex())
                        .get(cursor.intervalIndex()).start)
        );

        /*
         * Initially, insert the first interval from every employee.
         *
         * The original problem guarantees each employee has a
         * non-empty schedule. The check also supports empty lists.
         */
        for (int employee = 0; employee < schedule.size(); employee++) {
            if (!schedule.get(employee).isEmpty()) {
                minHeap.offer(new Cursor(employee, 0));
            }
        }

        // If there are no intervals, there are no finite gaps to report.
        if (minHeap.isEmpty()) {
            return freeTimes;
        }

        /*
         * The earliest interval establishes the initial busy coverage.
         * Polling it also frees its employee's cursor slot so we can
         * insert that employee's next interval.
         */
        Cursor first = minHeap.poll();
        Interval firstInterval = schedule.get(first.employeeIndex())
                                         .get(first.intervalIndex());

        int lastBusyEnd = firstInterval.end;

        // Add that employee's next interval, if one exists.
        int nextIndex = first.intervalIndex() + 1;
        if (nextIndex < schedule.get(first.employeeIndex()).size()) {
            minHeap.offer(
                new Cursor(first.employeeIndex(), nextIndex)
            );
        }

        /*
         * Repeatedly consume the globally earliest remaining interval.
         * Each employee contributes its intervals in their existing
         * sorted order.
         */
        while (!minHeap.isEmpty()) {
            Cursor cursor = minHeap.poll();

            Interval current = schedule.get(cursor.employeeIndex())
                                       .get(cursor.intervalIndex());

            /*
             * As in the sorting solution:
             *
             * start <= lastBusyEnd means overlapping/touching busy time.
             * Otherwise, [lastBusyEnd, start] is a common free interval.
             */
            if (current.start <= lastBusyEnd) {
                lastBusyEnd = Math.max(lastBusyEnd, current.end);
            } else {
                freeTimes.add(new Interval(lastBusyEnd, current.start));
                lastBusyEnd = current.end;
            }

            /*
             * Advance only the employee whose interval was consumed.
             *
             * Because each employee's schedule is already sorted,
             * its next interval cannot start before its current interval.
             * Inserting this successor maintains the heap's global
             * chronological ordering.
             */
            int next = cursor.intervalIndex() + 1;
            List<Interval> employeeSchedule =
                schedule.get(cursor.employeeIndex());

            if (next < employeeSchedule.size()) {
                minHeap.offer(new Cursor(cursor.employeeIndex(), next));
            }
        }

        /*
         * Each of the N intervals is inserted into and removed from
         * the heap exactly once.
         *
         * Time: O(N log K), where K is the number of employees.
         * Auxiliary space: O(K) for the heap.
         * Output space: O(F), where F is the number of free intervals.
         */
        return freeTimes;
    }
}

