import java.util.*;

/**
 * PROBLEM ANALYSIS: MEETING ROOMS II
 * ====================================
 * 
 * PROBLEM UNDERSTANDING:
 * Given an array of meeting time intervals [start, end], find the minimum
 * number of conference rooms required to schedule all meetings.
 * 
 * Example:
 * Input: [[0,30],[5,10],[15,20]]
 * Output: 2
 * Explanation: Meeting [0,30] overlaps with both [5,10] and [15,20]
 *              So we need 2 rooms at minimum
 * 
 * KEY INSIGHTS:
 * 1. Meetings overlap when one starts before another ends
 * 2. Need to track how many meetings are happening simultaneously
 * 3. The maximum simultaneous meetings = minimum rooms needed
 * 4. This is essentially finding the maximum "depth" of overlapping intervals
 * 
 * CRITICAL REALIZATION:
 * - At any point in time, the number of rooms needed equals
 *   the number of meetings currently in progress
 * - We need to find the peak of this count
 * 
 * INTERVIEW APPROACH - HOW TO THINK THROUGH THIS:
 * ================================================
 * 
 * Step 1: Understand with timeline
 * Meetings: [[0,30],[5,10],[15,20]]
 * 
 * Timeline:
 * 0        5   10      15  20      30
 * |--------|---|-------|---|-------|
 * [=============================]     Meeting 1
 *     [====]                          Meeting 2
 *                [====]               Meeting 3
 * 
 * At time 5: 2 meetings (1 and 2) → need 2 rooms
 * At time 15: 2 meetings (1 and 3) → need 2 rooms
 * Peak = 2 rooms
 * 
 * Step 2: Identify approaches
 * - Brute Force: Check all pairs for overlap O(n²)
 * - Chronological Ordering: Process events in time order
 * - Min Heap: Track when rooms become free
 * - Sweep Line: Separate start/end events
 * 
 * Step 3: Choose optimal approach
 * - Min Heap is intuitive and efficient
 * - Sweep Line is elegant mathematical solution
 * - Both are O(n log n)
 * 
 * APPROACHES TO DISCUSS:
 * 1. Min Heap (Priority Queue) - O(n log n) time, O(n) space [RECOMMENDED]
 * 2. Sweep Line Algorithm - O(n log n) time, O(n) space [OPTIMAL]
 * 3. Chronological Ordering - O(n log n) time, O(n) space
 * 4. TreeMap - O(n log n) time, O(n) space
 */

/**
 * APPROACH 1: MIN HEAP (PRIORITY QUEUE) - RECOMMENDED FOR INTERVIEWS
 * ===================================================================
 * 
 * INTUITION:
 * - Sort meetings by start time
 * - Use min heap to track end times of ongoing meetings
 * - For each meeting:
 *   - Remove all meetings that have ended (end time <= current start)
 *   - Add current meeting's end time to heap
 *   - Heap size = rooms needed at this moment
 * - Maximum heap size = minimum rooms needed
 * 
 * WHY IT WORKS:
 * - Heap always contains meetings currently in progress
 * - Heap size represents rooms currently occupied
 * - We track the maximum occupancy
 * 
 * ALGORITHM:
 * 1. Sort meetings by start time
 * 2. Create min heap (sorted by end time)
 * 3. For each meeting:
 *    a. Remove meetings from heap that end before current starts
 *    b. Add current meeting's end time to heap
 *    c. Track maximum heap size
 * 4. Return maximum size
 * 
 * TIME: O(n log n) - sorting + n heap operations
 * SPACE: O(n) - heap can contain all meetings
 */
class Solution {
    
    public int minMeetingRooms(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }
        
        // Sort meetings by start time
        Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));
        
        // Min heap to track end times of ongoing meetings
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        
        // Process each meeting
        for (int[] meeting : intervals) {
            int start = meeting[0];
            int end = meeting[1];
            
            // Remove all meetings that have ended by the time current meeting starts
            while (!minHeap.isEmpty() && minHeap.peek() <= start) {
                minHeap.poll();
            }
            
            // Add current meeting's end time
            minHeap.offer(end);
        }
        
        // The maximum size of heap is the answer
        // At the peak, heap contained all overlapping meetings
        return minHeap.size();
    }
}

/**
 * APPROACH 1b: MIN HEAP WITH EXPLICIT TRACKING
 * =============================================
 * 
 * Same algorithm but explicitly tracks the maximum
 */
class SolutionMinHeapExplicit {
    
    public int minMeetingRooms(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }
        
        // Sort by start time
        Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
        
        // Min heap stores end times
        PriorityQueue<Integer> endTimes = new PriorityQueue<>();
        int maxRooms = 0;
        
        for (int[] interval : intervals) {
            // Free up rooms for meetings that have ended
            while (!endTimes.isEmpty() && endTimes.peek() <= interval[0]) {
                endTimes.poll();
            }
            
            // Allocate room for current meeting
            endTimes.offer(interval[1]);
            
            // Track maximum rooms needed
            maxRooms = Math.max(maxRooms, endTimes.size());
        }
        
        return maxRooms;
    }
}

/**
 * APPROACH 2: SWEEP LINE ALGORITHM (MOST ELEGANT)
 * ================================================
 * 
 * INTUITION:
 * - Treat start and end as separate events
 * - Sort all events chronologically
 * - Process events: +1 for start, -1 for end
 * - Track running count and maximum
 * 
 * VISUALIZATION:
 * Meetings: [[0,30],[5,10],[15,20]]
 * 
 * Events:
 * Time:  0   5   10  15  20  30
 * Start: +1  +1      +1      
 * End:           -1      -1  -1
 * Count: 1   2   1   2   1   0
 *            ↑       ↑
 *         Peak = 2
 * 
 * ALGORITHM:
 * 1. Create separate arrays for start times and end times
 * 2. Sort both arrays
 * 3. Use two pointers to process events chronologically
 * 4. When start < end: new meeting starts (+1)
 * 5. When end <= start: meeting ends (-1)
 * 6. Track maximum concurrent meetings
 * 
 * TIME: O(n log n) - sorting
 * SPACE: O(n) - separate arrays
 */
class SolutionSweepLine {
    
    public int minMeetingRooms(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }
        
        int n = intervals.length;
        
        // Separate start and end times
        int[] starts = new int[n];
        int[] ends = new int[n];
        
        for (int i = 0; i < n; i++) {
            starts[i] = intervals[i][0];
            ends[i] = intervals[i][1];
        }
        
        // Sort both arrays
        Arrays.sort(starts);
        Arrays.sort(ends);
        
        int rooms = 0;
        int maxRooms = 0;
        int startPtr = 0;
        int endPtr = 0;
        
        // Process all events
        while (startPtr < n) {
            // If a meeting starts before another ends
            if (starts[startPtr] < ends[endPtr]) {
                rooms++;  // Need a new room
                maxRooms = Math.max(maxRooms, rooms);
                startPtr++;
            } else {
                // A meeting ends, free up a room
                rooms--;
                endPtr++;
            }
        }
        
        return maxRooms;
    }
}

/**
 * APPROACH 3: CHRONOLOGICAL ORDERING WITH EVENTS
 * ===============================================
 * 
 * Create event objects for better clarity
 */
class SolutionEvents {
    
    class Event {
        int time;
        boolean isStart;
        
        Event(int time, boolean isStart) {
            this.time = time;
            this.isStart = isStart;
        }
    }
    
    public int minMeetingRooms(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }
        
        List<Event> events = new ArrayList<>();
        
        // Create events for each start and end
        for (int[] interval : intervals) {
            events.add(new Event(interval[0], true));  // Start event
            events.add(new Event(interval[1], false)); // End event
        }
        
        // Sort events chronologically
        // If times are equal, process end before start (room becomes free first)
        Collections.sort(events, (a, b) -> {
            if (a.time != b.time) {
                return a.time - b.time;
            }
            // End events before start events at same time
            return a.isStart ? 1 : -1;
        });
        
        int rooms = 0;
        int maxRooms = 0;
        
        for (Event event : events) {
            if (event.isStart) {
                rooms++;
                maxRooms = Math.max(maxRooms, rooms);
            } else {
                rooms--;
            }
        }
        
        return maxRooms;
    }
}

/**
 * APPROACH 4: TREEMAP (ALTERNATIVE)
 * ==================================
 * 
 * Use TreeMap to automatically maintain chronological order
 */
class SolutionTreeMap {
    
    public int minMeetingRooms(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }
        
        // TreeMap: time -> delta (number of meetings starting/ending)
        TreeMap<Integer, Integer> timeline = new TreeMap<>();
        
        for (int[] interval : intervals) {
            timeline.put(interval[0], timeline.getOrDefault(interval[0], 0) + 1);  // Start
            timeline.put(interval[1], timeline.getOrDefault(interval[1], 0) - 1);  // End
        }
        
        int rooms = 0;
        int maxRooms = 0;
        
        for (int delta : timeline.values()) {
            rooms += delta;
            maxRooms = Math.max(maxRooms, rooms);
        }
        
        return maxRooms;
    }
}

/**
 * APPROACH 5: BRUTE FORCE (FOR COMPARISON)
 * =========================================
 * 
 * Check overlaps explicitly - O(n²)
 */
class SolutionBruteForce {
    
    public int minMeetingRooms(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }
        
        int maxRooms = 0;
        
        // For each time point in any interval
        for (int[] interval : intervals) {
            int overlaps = 0;
            
            // Count how many intervals contain this start time
            for (int[] other : intervals) {
                // Check if 'other' overlaps with time = interval[0]
                if (other[0] <= interval[0] && interval[0] < other[1]) {
                    overlaps++;
                }
            }
            
            maxRooms = Math.max(maxRooms, overlaps);
        }
        
        return maxRooms;
    }
}

/**
 * TEST CASES
 * ==========
 */
class TestMeetingRoomsII {
    
    public static void runTest(String testName, int[][] intervals, int expected) {
        System.out.println("\n" + testName);
        System.out.println("Intervals: " + Arrays.deepToString(intervals));
        
        Solution sol = new Solution();
        int result = sol.minMeetingRooms(intervals);
        
        System.out.println("Result: " + result);
        System.out.println("Expected: " + expected);
        System.out.println("Status: " + (result == expected ? "✓ PASS" : "✗ FAIL"));
        
        // Visualize the timeline
        if (intervals.length <= 5) {
            visualizeTimeline(intervals);
        }
    }
    
    private static void visualizeTimeline(int[][] intervals) {
        System.out.println("\nTimeline Visualization:");
        Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
        
        for (int i = 0; i < intervals.length; i++) {
            System.out.printf("Meeting %d: [%d, %d)\n", 
                i + 1, intervals[i][0], intervals[i][1]);
        }
        
        // Show overlaps
        System.out.println("\nOverlap Analysis:");
        for (int i = 0; i < intervals.length; i++) {
            List<Integer> overlaps = new ArrayList<>();
            for (int j = 0; j < intervals.length; j++) {
                if (i != j && intervalsOverlap(intervals[i], intervals[j])) {
                    overlaps.add(j + 1);
                }
            }
            if (!overlaps.isEmpty()) {
                System.out.printf("Meeting %d overlaps with: %s\n", 
                    i + 1, overlaps);
            }
        }
    }
    
    private static boolean intervalsOverlap(int[] a, int[] b) {
        return a[0] < b[1] && b[0] < a[1];
    }
    
    public static void main(String[] args) {
        System.out.println("=== MEETING ROOMS II - COMPREHENSIVE TESTING ===");
        
        // Test Case 1: Basic overlapping
        runTest(
            "Test 1: Basic case",
            new int[][]{{0, 30}, {5, 10}, {15, 20}},
            2
        );
        
        // Test Case 2: No overlaps
        runTest(
            "Test 2: No overlaps (sequential)",
            new int[][]{{7, 10}, {2, 4}},
            1
        );
        
        // Test Case 3: All overlapping
        runTest(
            "Test 3: All overlap",
            new int[][]{{1, 5}, {2, 6}, {3, 7}, {4, 8}},
            4
        );
        
        // Test Case 4: Nested intervals
        runTest(
            "Test 4: Nested meetings",
            new int[][]{{1, 10}, {2, 3}, {4, 5}, {6, 7}},
            2
        );
        
        // Test Case 5: Single meeting
        runTest(
            "Test 5: Single meeting",
            new int[][]{{1, 5}},
            1
        );
        
        // Test Case 6: Same start times
        runTest(
            "Test 6: Same start time",
            new int[][]{{0, 10}, {0, 20}, {0, 30}},
            3
        );
        
        // Test Case 7: Back-to-back meetings
        runTest(
            "Test 7: Back-to-back (no overlap)",
            new int[][]{{0, 5}, {5, 10}, {10, 15}},
            1
        );
        
        // Test Case 8: One meeting ends as another starts
        runTest(
            "Test 8: Touch but don't overlap",
            new int[][]{{0, 5}, {5, 10}},
            1
        );
        
        // Test Case 9: Complex scenario
        runTest(
            "Test 9: Complex overlapping",
            new int[][]{{0, 30}, {5, 10}, {15, 20}, {20, 25}, {25, 30}},
            2
        );
        
        // Test Case 10: Large numbers
        runTest(
            "Test 10: Large time values",
            new int[][]{{1000000, 2000000}, {1500000, 1800000}},
            2
        );
        
        // Compare all approaches
        System.out.println("\n\n=== APPROACH COMPARISON ===");
        int[][] testIntervals = {{0, 30}, {5, 10}, {15, 20}};
        
        System.out.println("\nTest intervals: " + Arrays.deepToString(testIntervals));
        
        Solution sol1 = new Solution();
        System.out.println("Min Heap approach: " + sol1.minMeetingRooms(testIntervals));
        
        SolutionSweepLine sol2 = new SolutionSweepLine();
        System.out.println("Sweep Line approach: " + sol2.minMeetingRooms(testIntervals));
        
        SolutionEvents sol3 = new SolutionEvents();
        System.out.println("Events approach: " + sol3.minMeetingRooms(testIntervals));
        
        SolutionTreeMap sol4 = new SolutionTreeMap();
        System.out.println("TreeMap approach: " + sol4.minMeetingRooms(testIntervals));
        
        // Demonstrate step-by-step for understanding
        System.out.println("\n\n=== STEP-BY-STEP DEMONSTRATION ===");
        demonstrateMinHeap();
        System.out.println();
        demonstrateSweepLine();
    }
    
    private static void demonstrateMinHeap() {
        System.out.println("MIN HEAP APPROACH:");
        int[][] intervals = {{0, 30}, {5, 10}, {15, 20}};
        
        Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
        System.out.println("Sorted intervals: " + Arrays.deepToString(intervals));
        
        PriorityQueue<Integer> heap = new PriorityQueue<>();
        
        for (int i = 0; i < intervals.length; i++) {
            System.out.println("\nProcessing meeting " + (i + 1) + ": [" + 
                intervals[i][0] + ", " + intervals[i][1] + ")");
            
            while (!heap.isEmpty() && heap.peek() <= intervals[i][0]) {
                System.out.println("  Room freed (meeting ended at " + heap.poll() + ")");
            }
            
            heap.offer(intervals[i][1]);
            System.out.println("  Room allocated (until " + intervals[i][1] + ")");
            System.out.println("  Current rooms needed: " + heap.size());
            System.out.println("  Heap state: " + heap);
        }
        
        System.out.println("\nFinal answer: " + heap.size() + " rooms");
    }
    
    private static void demonstrateSweepLine() {
        System.out.println("SWEEP LINE APPROACH:");
        int[][] intervals = {{0, 30}, {5, 10}, {15, 20}};
        
        int[] starts = {0, 5, 15};
        int[] ends = {30, 10, 20};
        
        Arrays.sort(starts);
        Arrays.sort(ends);
        
        System.out.println("Start times: " + Arrays.toString(starts));
        System.out.println("End times: " + Arrays.toString(ends));
        
        int rooms = 0, maxRooms = 0;
        int s = 0, e = 0;
        
        System.out.println("\nProcessing events:");
        while (s < starts.length) {
            if (starts[s] < ends[e]) {
                rooms++;
                System.out.printf("Time %d: Meeting starts, rooms = %d\n", starts[s], rooms);
                maxRooms = Math.max(maxRooms, rooms);
                s++;
            } else {
                rooms--;
                System.out.printf("Time %d: Meeting ends, rooms = %d\n", ends[e], rooms);
                e++;
            }
        }
        
        System.out.println("\nFinal answer: " + maxRooms + " rooms");
    }
}

/**
 * COMPLEXITY ANALYSIS
 * ===================
 * 
 * Approach 1 - Min Heap:
 * Time:  O(n log n)
 *        - Sorting: O(n log n)
 *        - n meetings, each: O(log n) heap operation
 *        - Total: O(n log n)
 * Space: O(n) - heap can contain all meetings
 * 
 * Approach 2 - Sweep Line:
 * Time:  O(n log n)
 *        - Sorting starts: O(n log n)
 *        - Sorting ends: O(n log n)
 *        - Processing: O(n)
 *        - Total: O(n log n)
 * Space: O(n) - two separate arrays
 * 
 * Approach 3 - Events:
 * Time:  O(n log n)
 *        - Creating events: O(n)
 *        - Sorting: O(n log n)
 *        - Processing: O(n)
 * Space: O(n) - event list
 * 
 * Approach 4 - TreeMap:
 * Time:  O(n log n)
 *        - n insertions: O(n log n)
 *        - Iteration: O(n)
 * Space: O(n) - TreeMap
 * 
 * Approach 5 - Brute Force:
 * Time:  O(n²) - check all pairs
 * Space: O(1)
 * 
 * 
 * INTERVIEW STRATEGY
 * ==================
 * 
 * 1. CLARIFY THE PROBLEM:
 *    Q: "Are intervals inclusive or exclusive?"
 *    A: Half-open [start, end) - end is exclusive
 *    
 *    Q: "Can meetings have same start time?"
 *    A: Yes
 *    
 *    Q: "Can a meeting start when another ends?"
 *    A: Yes, [0,5) and [5,10) don't overlap
 *    
 *    Q: "Are intervals sorted?"
 *    A: Not necessarily, need to sort
 * 
 * 2. START WITH INTUITION:
 *    "The problem asks for minimum rooms needed.
 *     This is equivalent to finding the maximum number
 *     of meetings happening at the same time.
 *     
 *     I need to identify when meetings overlap."
 * 
 * 3. DRAW A TIMELINE:
 *    "Let me visualize: [[0,30],[5,10],[15,20]]
 *     
 *     0        10       20       30
 *     |--------|--------|--------|
 *     [========================]  Meeting 1
 *         [===]                   Meeting 2
 *                 [===]           Meeting 3
 *     
 *     At time 5: 2 meetings active
 *     At time 15: 2 meetings active
 *     Maximum = 2 rooms needed"
 * 
 * 4. EXPLAIN APPROACH (MIN HEAP):
 *    "I'll use a min heap approach:
 *     
 *     1. Sort meetings by start time
 *     2. Use heap to track when rooms become free
 *     3. For each meeting:
 *        - Remove meetings that ended
 *        - Allocate a room
 *        - Heap size = rooms in use
 *     4. Maximum heap size = answer
 *     
 *     The heap always contains end times of ongoing meetings."
 * 
 * 5. WALK THROUGH EXAMPLE:
 *    Intervals: [[0,30],[5,10],[15,20]]
 *    
 *    After sorting: [[0,30],[5,10],[15,20]]
 *    
 *    Step 1: Process [0,30]
 *      Heap: [30]
 *      Rooms: 1
 *    
 *    Step 2: Process [5,10]
 *      Nothing ends before 5
 *      Heap: [10, 30]
 *      Rooms: 2
 *    
 *    Step 3: Process [15,20]
 *      10 <= 15, remove it
 *      Heap: [20, 30]
 *      Rooms: 2
 *    
 *    Answer: 2
 * 
 * 6. MENTION ALTERNATIVE (SWEEP LINE):
 *    "There's also a sweep line approach:
 *     - Separate start and end events
 *     - Sort both
 *     - Process chronologically
 *     - Track running count
 *     
 *     Both are O(n log n), heap is more intuitive."
 * 
 * 7. CODE INCREMENTALLY:
 *    - First: Sort intervals
 *    - Second: Create heap
 *    - Third: Process each interval
 *    - Fourth: Track maximum
 * 
 * 8. EDGE CASES:
 *    ✓ No meetings (return 0)
 *    ✓ Single meeting (return 1)
 *    ✓ No overlaps (return 1)
 *    ✓ All overlap (return n)
 *    ✓ Back-to-back meetings
 *    ✓ Same start times
 *    ✓ Nested intervals
 * 
 * 9. OPTIMIZE:
 *    "Could optimize by:
 *     - Early termination if heap size > n/2
 *     - Using array instead of heap if small n
 *     - Preprocessing to remove impossible cases"
 * 
 * 10. COMMON MISTAKES:
 *     ✗ Not sorting intervals first
 *     ✗ Using max heap instead of min heap
 *     ✗ Comparing end <= start (should be end <= start)
 *     ✗ Forgetting that [5,10) and [10,15) don't overlap
 *     ✗ Returning heap.size() at end (should track max)
 * 
 * FOLLOW-UP QUESTIONS:
 * ====================
 * 
 * Q: "What if we want to know which meetings go in which room?"
 * A: Would need to track room assignments explicitly
 *    Could use map: room_id -> list of meetings
 * 
 * Q: "What if meetings have priorities?"
 * A: Would need to consider priority in scheduling
 *    Might need to reject/delay lower priority meetings
 * 
 * Q: "What if we want to minimize room changes?"
 * A: Different optimization goal - would need DP or greedy
 *    with different criteria
 * 
 * Q: "What if rooms have different capacities?"
 * A: Would need to match meeting size to room capacity
 *    More complex assignment problem
 * 
 * Q: "How to handle cancellations in real-time?"
 * A: Would need dynamic data structure
 *    Could use interval tree for efficient updates
 * 
 * RELATED PROBLEMS:
 * =================
 * - Meeting Rooms (simpler: check if any overlap)
 * - Merge Intervals
 * - Insert Interval
 * - Non-overlapping Intervals
 * - My Calendar I/II/III
 * 
 * RECOMMENDED SOLUTION:
 * Approach 1 (Min Heap) is the best for interviews.
 * It's intuitive, efficient, and easy to explain.
 * Mention Sweep Line as an elegant alternative.
 */
