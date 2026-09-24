import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeMap;

class MinMeetingRoomsII {
    public int minMeetingRooms(int[][] intervals) {
        List<int[]> events = new ArrayList<>();

        for (int[] interval : intervals) {
            events.add(new int[] { interval[0], 1 }); // start of the meeting
            events.add(new int[] { interval[1], -1 }); // end of the meeting
        }

        // Sort by time; if same, end (-1) comes before start (+1)
        events.sort((a, b) -> a[0] == b[0] ? a[1] - b[1] : a[0] - b[0]);

        int activeMeetings = 0, maxRooms = 0;
        for (int[] event : events) {
            activeMeetings += event[1];
            maxRooms = Math.max(maxRooms, activeMeetings);
        }
        return maxRooms;
    }
}



class MinMeetingRoomsII2 {

    /**
     * Returns the minimum number of meeting rooms required
     * so that no meetings overlap.
     */
    public int minMeetingRooms(int[][] intervals) {

        // =========================
        // 1. Convert meetings to timeline events
        // =========================

        List<MeetingEvent> timeline = new ArrayList<>();

        for (int[] interval : intervals) {
            int startTime = interval[0];
            int endTime = interval[1];

            timeline.add(new MeetingEvent(startTime, EventType.START));
            timeline.add(new MeetingEvent(endTime, EventType.END));
        }

        // =========================
        // 2. Sort events chronologically
        // =========================

        // If two events occur at the same time:
        // END must come before START (free room before allocating)
        timeline.sort(Comparator
                .comparingInt(MeetingEvent::time)
                .thenComparing(MeetingEvent::type)
        );

        // =========================
        // 3. Sweep line to count rooms
        // =========================

        int activeMeetings = 0;
        int maxRoomsRequired = 0;

        for (MeetingEvent event : timeline) {

            // START → +1 room, END → -1 room
            activeMeetings += event.type().delta();

            // Track maximum simultaneous meetings
            maxRoomsRequired = Math.max(maxRoomsRequired, activeMeetings);
        }

        return maxRoomsRequired;
    }

    // =========================
    // Supporting Types
    // =========================

    /**
     * Represents a point in time where a meeting
     * either starts or ends.
     */
    static record MeetingEvent(int time, EventType type) { }

    /**
     * Meeting event type with room impact.
     */
    enum EventType {
        END(-1),    // must be processed before START at same time
        START(+1);

        private final int delta;

        EventType(int delta) {
            this.delta = delta;
        }

        int delta() {
            return delta;
        }
    }
}


/**
 * PROBLEM ANALYSIS:
 * ==================
 * Given an array of meeting time intervals [start, end], find the minimum number
 * of conference rooms required to schedule all meetings.
 * 
 * KEY INSIGHTS:
 * 1. At any point in time, # of rooms needed = # of overlapping meetings
 * 2. Need to find the maximum number of simultaneous meetings
 * 3. A meeting starting at time t conflicts with a meeting ending at time t? 
 *    NO - a room becomes free exactly when a meeting ends
 * 4. Multiple valid approaches: sorting, sweep line, priority queue
 * 
 * PROBLEM VARIATIONS:
 * - Meeting Rooms I: Check if person can attend all (no overlap)
 * - Meeting Rooms II: Find minimum rooms needed (this problem)
 * - Meeting Rooms III: With limited rooms, which rooms used most (harder)
 * 
 * WHY DIFFERENT APPROACHES WORK:
 * 1. Min-Heap: Track end times of ongoing meetings
 * 2. Two-Pointer: Separate start/end times, count overlaps
 * 3. Sweep Line: Process events (start/end) chronologically
 * 4. Chronological Ordering: Simulate timeline
 * 
 * INTERVIEW APPROACH:
 * ===================
 * 1. Understand the problem: overlapping intervals
 * 2. Realize we need to track concurrent meetings
 * 3. Think about when rooms are needed vs freed
 * 4. Choose approach based on clarity and efficiency
 * 
 * HOW TO COME UP WITH SOLUTION:
 * ==============================
 * Step 1: Brute force - check all pairs for overlap → O(n²)
 * Step 2: Better idea - sort meetings, track when rooms free up
 * Step 3: Use min-heap to track earliest-ending meeting
 * Step 4: Optimize further with two-pointer approach
 */



class MeetingRoomsII {
    
    // ============================================================================
    // APPROACH 1: MIN-HEAP / PRIORITY QUEUE (MOST INTUITIVE)
    // ============================================================================
    // Time Complexity: O(n log n) - sorting + heap operations
    // Space Complexity: O(n) - heap can hold all meetings
    // Best for interviews - clear logic and efficient
    
    /**
     * Min-Heap Approach:
     * 1. Sort meetings by start time
     * 2. Use min-heap to track end times of ongoing meetings
     * 3. For each meeting:
     *    - Remove meetings that have ended (start >= earliest end)
     *    - Add current meeting's end time to heap
     *    - Track maximum heap size (concurrent meetings)
     */
    public int minMeetingRooms(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }
        
        // Sort meetings by start time
        Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
        
        // Min-heap to track end times of ongoing meetings
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        
        // Add first meeting's end time
        minHeap.offer(intervals[0][1]);
        
        // Process remaining meetings
        for (int i = 1; i < intervals.length; i++) {
            // If earliest-ending meeting has ended, room is free
            if (intervals[i][0] >= minHeap.peek()) {
                minHeap.poll(); // Free up the room
            }
            
            // Add current meeting's end time (allocate a room)
            minHeap.offer(intervals[i][1]);
        }
        
        // Heap size = number of rooms needed
        return minHeap.size();
    }
    
    // ============================================================================
    // APPROACH 2: TWO-POINTER (MOST ELEGANT)
    // ============================================================================
    // Time Complexity: O(n log n) - sorting
    // Space Complexity: O(n) - separate arrays for starts and ends
    // Very elegant and efficient
    
    /**
     * Two-Pointer Approach:
     * 1. Separate start and end times into different arrays
     * 2. Sort both arrays independently
     * 3. Use two pointers to traverse chronologically
     * 4. When meeting starts → need a room (count++)
     * 5. When meeting ends → free a room (count--)
     * 6. Track maximum count
     */
    public int minMeetingRoomsTwoPointer(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }
        
        int n = intervals.length;
        int[] starts = new int[n];
        int[] ends = new int[n];
        
        // Separate start and end times
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
        
        // Process events chronologically
        while (startPtr < n) {
            if (starts[startPtr] < ends[endPtr]) {
                // Meeting starts - need a room
                rooms++;
                startPtr++;
            } else {
                // Meeting ends - free a room
                rooms--;
                endPtr++;
            }
            
            maxRooms = Math.max(maxRooms, rooms);
        }
        
        return maxRooms;
    }
    
    // ============================================================================
    // APPROACH 3: SWEEP LINE / EVENT-BASED (MOST GENERAL)
    // ============================================================================
    // Time Complexity: O(n log n) - sorting events
    // Space Complexity: O(n) - event list
    // General technique applicable to many interval problems
    
    /**
     * Sweep Line Approach:
     * 1. Create events for each start and end time
     * 2. Sort events chronologically
     * 3. Process events: start → +1, end → -1
     * 4. Track maximum concurrent count
     * 
     * IMPORTANT: If start and end at same time, process END first
     * (room becomes available before new meeting starts)
     */
    public int minMeetingRoomsSweepLine(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }
        
        List<Event> events = new ArrayList<>();
        
        // Create events
        for (int[] interval : intervals) {
            events.add(new Event(interval[0], true));  // Start event
            events.add(new Event(interval[1], false)); // End event
        }
        
        // Sort events
        // If same time, end events come before start events
        Collections.sort(events, (a, b) -> {
            if (a.time != b.time) {
                return a.time - b.time;
            }
            // End event (false) should come before start event (true)
            return Boolean.compare(a.isStart, b.isStart);
        });
        
        int rooms = 0;
        int maxRooms = 0;
        
        // Process events
        for (Event event : events) {
            if (event.isStart) {
                rooms++;
            } else {
                rooms--;
            }
            maxRooms = Math.max(maxRooms, rooms);
        }
        
        return maxRooms;
    }
    
    /**
     * Event representation for sweep line
     */
    class Event {
        int time;
        boolean isStart;
        
        Event(int time, boolean isStart) {
            this.time = time;
            this.isStart = isStart;
        }
    }
    
    // ============================================================================
    // APPROACH 4: CHRONOLOGICAL ORDERING (ALTERNATIVE)
    // ============================================================================
    // Time Complexity: O(n log n)
    // Space Complexity: O(n)
    // Similar to sweep line but using TreeMap
    
    /**
     * TreeMap approach to maintain chronological order
     */
    public int minMeetingRoomsTreeMap(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }
        
        // TreeMap: time -> delta (starts - ends at that time)
        TreeMap<Integer, Integer> timeline = new TreeMap<>();
        
        for (int[] interval : intervals) {
            timeline.put(interval[0], timeline.getOrDefault(interval[0], 0) + 1);
            timeline.put(interval[1], timeline.getOrDefault(interval[1], 0) - 1);
        }
        
        int rooms = 0;
        int maxRooms = 0;
        
        for (int delta : timeline.values()) {
            rooms += delta;
            maxRooms = Math.max(maxRooms, rooms);
        }
        
        return maxRooms;
    }
    
    // ============================================================================
    // APPROACH 5: BRUTE FORCE (FOR UNDERSTANDING)
    // ============================================================================
    // Time Complexity: O(n²)
    // Space Complexity: O(n)
    // Not recommended but helps understand the problem
    
    /**
     * Brute force: For each meeting, count how many overlap with it
     * Answer = maximum overlap count
     */
    public int minMeetingRoomsBruteForce(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }
        
        int maxRooms = 0;
        
        // For each time point, count concurrent meetings
        for (int[] current : intervals) {
            int count = 0;
            for (int[] other : intervals) {
                // Check if they overlap
                if (isOverlapping(current, other)) {
                    count++;
                }
            }
            maxRooms = Math.max(maxRooms, count);
        }
        
        return maxRooms;
    }
    
    /**
     * Check if two intervals overlap
     */
    private boolean isOverlapping(int[] a, int[] b) {
        return a[0] < b[1] && b[0] < a[1];
    }
    
    // ============================================================================
    // TEST CASES WITH DETAILED EXPLANATIONS
    // ============================================================================
    
    public static void main(String[] args) {
        MeetingRoomsII solution = new MeetingRoomsII();
        
        // Test Case 1: Basic example
        System.out.println("Test 1: Basic overlapping meetings");
        int[][] intervals1 = {{0,30},{5,10},{15,20}};
        System.out.println("Meetings: [[0,30],[5,10],[15,20]]");
        System.out.println("Timeline:");
        System.out.println("  0    5   10   15   20        30");
        System.out.println("  |====|====|====|====|=========|");
        System.out.println("  [--------Meeting 1-----------]");
        System.out.println("       [M2-]");
        System.out.println("                 [M3--]");
        System.out.println("Expected: 2 rooms");
        System.out.println("Got (Heap): " + solution.minMeetingRooms(intervals1));
        System.out.println("Got (Two-Pointer): " + solution.minMeetingRoomsTwoPointer(intervals1));
        System.out.println("Got (Sweep): " + solution.minMeetingRoomsSweepLine(intervals1));
        /*
         * At time 5-10: Meetings 1 and 2 overlap → need 2 rooms
         * At time 15-20: Meetings 1 and 3 overlap → need 2 rooms
         * Maximum: 2 rooms
         */
        
        // Test Case 2: All overlapping
        System.out.println("\nTest 2: All meetings overlap");
        int[][] intervals2 = {{0,10},{5,15},{10,20}};
        System.out.println("Meetings: [[0,10],[5,15],[10,20]]");
        System.out.println("Expected: 2 rooms (10 is boundary case)");
        System.out.println("Got: " + solution.minMeetingRooms(intervals2));
        /*
         * [0-10]
         *      [5-15]
         *           [10-20]
         * At time 10: Meeting 1 ends, Meeting 3 starts
         * Meeting 1 and 2 overlap: need 2 rooms
         * Meeting 2 and 3 overlap: need 2 rooms
         * Maximum: 2 rooms
         */
        
        // Test Case 3: No overlap
        System.out.println("\nTest 3: No overlapping meetings");
        int[][] intervals3 = {{0,5},{5,10},{10,15}};
        System.out.println("Meetings: [[0,5],[5,10],[10,15]]");
        System.out.println("Expected: 1 room (back-to-back)");
        System.out.println("Got: " + solution.minMeetingRooms(intervals3));
        /*
         * [0-5][5-10][10-15]
         * Each meeting starts exactly when previous ends
         * Can use same room for all
         */
        
        // Test Case 4: Single meeting
        System.out.println("\nTest 4: Single meeting");
        int[][] intervals4 = {{0,10}};
        System.out.println("Meetings: [[0,10]]");
        System.out.println("Expected: 1 room");
        System.out.println("Got: " + solution.minMeetingRooms(intervals4));
        
        // Test Case 5: Empty input
        System.out.println("\nTest 5: No meetings");
        int[][] intervals5 = {};
        System.out.println("Meetings: []");
        System.out.println("Expected: 0 rooms");
        System.out.println("Got: " + solution.minMeetingRooms(intervals5));
        
        // Test Case 6: All concurrent
        System.out.println("\nTest 6: All meetings at same time");
        int[][] intervals6 = {{0,10},{0,10},{0,10},{0,10}};
        System.out.println("Meetings: [[0,10],[0,10],[0,10],[0,10]]");
        System.out.println("Expected: 4 rooms (all concurrent)");
        System.out.println("Got: " + solution.minMeetingRooms(intervals6));
        
        // Test Case 7: Complex overlapping
        System.out.println("\nTest 7: Complex pattern");
        int[][] intervals7 = {{1,5},{2,6},{3,7},{4,8},{5,9}};
        System.out.println("Meetings: [[1,5],[2,6],[3,7],[4,8],[5,9]]");
        System.out.println("Timeline:");
        System.out.println("  1 2 3 4 5 6 7 8 9");
        System.out.println("  [---]           ");
        System.out.println("    [---]         ");
        System.out.println("      [---]       ");
        System.out.println("        [---]     ");
        System.out.println("          [---]   ");
        System.out.println("Expected: 4 rooms (at time 4-5)");
        System.out.println("Got: " + solution.minMeetingRooms(intervals7));
        /*
         * At time 4: Meetings [1,5], [2,6], [3,7], [4,8] all active
         * Need 4 rooms
         */
        
        // Test Case 8: Unsorted input
        System.out.println("\nTest 8: Unsorted meetings");
        int[][] intervals8 = {{15,20},{5,10},{0,30}};
        System.out.println("Meetings (unsorted): [[15,20],[5,10],[0,30]]");
        System.out.println("Expected: 2 rooms");
        System.out.println("Got: " + solution.minMeetingRooms(intervals8));
        /*
         * After sorting: [[0,30],[5,10],[15,20]]
         * Same as Test Case 1
         */
    }
}

/**
 * COMPREHENSIVE INTERVIEW GUIDE:
 * ===============================
 * 
 * 1. PROBLEM RECOGNITION:
 * ----------------------
 * Key phrases that identify this pattern:
 * - "Minimum number of [resource]" → Optimization
 * - "Schedule all meetings" → Interval scheduling
 * - "Conference rooms" / "Platforms" / "Servers" → Resource allocation
 * - Overlapping intervals → Need to track concurrency
 * 
 * This is a CLASSIC interval scheduling / resource allocation problem!
 * 
 * 2. INITIAL OBSERVATIONS TO SHARE:
 * ---------------------------------
 * a) "Need to find maximum number of concurrent meetings"
 * b) "A room is freed when a meeting ends"
 * c) "If meeting starts exactly when another ends, can use same room"
 * d) "Must handle intervals that are not sorted"
 * e) "This is about tracking overlapping intervals efficiently"
 * 
 * 3. APPROACHES COMPARISON:
 * -------------------------
 * 
 * MIN-HEAP APPROACH:
 * + Most intuitive: "allocate room, free room"
 * + Natural simulation of the scheduling process
 * + Easy to explain in interview
 * - Slightly more space (heap can grow to n)
 * RECOMMEND: Best for interviews
 * 
 * TWO-POINTER APPROACH:
 * + Most elegant and clean code
 * + Easy to understand once you see it
 * + Minimal space overhead
 * - Less obvious at first
 * RECOMMEND: Show as optimization if time permits
 * 
 * SWEEP LINE APPROACH:
 * + General technique for many interval problems
 * + Shows algorithmic maturity
 * + Handles complex variants easily
 * - More code to write
 * 
 * BRUTE FORCE:
 * - O(n²) - too slow
 * + Good for understanding the problem
 * 
 * 4. SOLUTION WALKTHROUGH (MIN-HEAP):
 * -----------------------------------
 * Step 1: Sort meetings by start time
 *         Why? Process chronologically
 * 
 * Step 2: Create min-heap to track end times
 *         Why? Quickly find which room frees up first
 * 
 * Step 3: For each meeting:
 *         a) If current start >= earliest end:
 *            - That room is free, reuse it (poll from heap)
 *         b) Add current meeting's end time to heap
 *            - Represents allocating a room
 * 
 * Step 4: Heap size = rooms needed
 *         Why? Each element in heap = one ongoing meeting
 * 
 * 5. CRITICAL EDGE CASE - BOUNDARY CONDITION:
 * -------------------------------------------
 * Question: If meeting ends at time t and another starts at time t,
 *           do they overlap?
 * 
 * Answer: NO - they DON'T overlap!
 * 
 * Example: [0,10] and [10,20]
 * - Meeting 1 ends at 10
 * - Meeting 2 starts at 10
 * - Can use SAME room (room is freed exactly at 10)
 * 
 * Code handles this correctly:
 * if (start >= earliestEnd) → Can reuse room
 * 
 * IMPORTANT: Use >= not just >
 * 
 * 6. COMPLEXITY ANALYSIS:
 * -----------------------
 * MIN-HEAP:
 * Time: O(n log n)
 *   - Sorting: O(n log n)
 *   - Heap operations: n × O(log n) = O(n log n)
 *   - Total: O(n log n)
 * Space: O(n) - heap can contain all meetings in worst case
 * 
 * TWO-POINTER:
 * Time: O(n log n)
 *   - Sorting starts: O(n log n)
 *   - Sorting ends: O(n log n)
 *   - Two-pointer scan: O(n)
 *   - Total: O(n log n)
 * Space: O(n) - two arrays
 * 
 * SWEEP LINE:
 * Time: O(n log n)
 *   - Creating events: O(n)
 *   - Sorting events: O(n log n)
 *   - Processing: O(n)
 * Space: O(n) - event list
 * 
 * ALL approaches have same time complexity!
 * 
 * 7. WHY TWO-POINTER WORKS:
 * -------------------------
 * KEY INSIGHT: We don't need to track WHICH meetings are ongoing,
 *              just HOW MANY.
 * 
 * Separate starts and ends, then process chronologically:
 * - When we see a start time → meeting begins → rooms++
 * - When we see an end time → meeting ends → rooms--
 * - Track maximum rooms value
 * 
 * Example: [[0,30],[5,10],[15,20]]
 * Starts: [0, 5, 15]
 * Ends:   [10, 20, 30]
 * 
 * Process:
 * Time 0:  start → rooms = 1
 * Time 5:  start → rooms = 2
 * Time 10: end   → rooms = 1
 * Time 15: start → rooms = 2
 * Time 20: end   → rooms = 1
 * Time 30: end   → rooms = 0
 * 
 * Maximum: 2 ✓
 * 
 * 8. EDGE CASES TO DISCUSS:
 * -------------------------
 * ✓ Empty input → return 0
 * ✓ Single meeting → return 1
 * ✓ No overlaps (back-to-back) → return 1
 * ✓ All meetings overlap → return n
 * ✓ Meetings start/end at same time → can share room
 * ✓ Unsorted input → must sort first
 * ✓ Same meeting multiple times → treat as separate
 * 
 * 9. COMMON MISTAKES TO AVOID:
 * ----------------------------
 * ✗ Using > instead of >= for boundary check
 * ✗ Forgetting to sort meetings first
 * ✗ Thinking meetings at same time conflict
 * ✗ Not handling empty input
 * ✗ Assuming input is already sorted
 * ✗ Counting rooms incorrectly (off by one)
 * ✗ In sweep line, wrong order for same-time events
 * ✗ Not tracking maximum, only final count
 * 
 * 10. INTERVIEW COMMUNICATION STRATEGY:
 * -------------------------------------
 * 1. Clarify: "Can a meeting start when another ends?" (Yes, same room)
 * 2. Observe: "Need to find maximum concurrent meetings"
 * 3. Approach: "I'll use min-heap to track when rooms free up"
 * 4. Walk through: Draw timeline for small example
 * 5. Code: Write clean implementation
 * 6. Test: Verify with boundary cases
 * 7. Optimize: Mention two-pointer as alternative
 * 8. Complexity: "O(n log n) time, O(n) space"
 * 
 * 11. DETAILED EXAMPLE WALKTHROUGH (MIN-HEAP):
 * --------------------------------------------
 * Input: [[0,30],[5,10],[15,20]]
 * 
 * After sorting: [[0,30],[5,10],[15,20]]
 * 
 * Step 1: Meeting [0,30]
 *   Heap: [30]
 *   Rooms: 1
 * 
 * Step 2: Meeting [5,10]
 *   Check: 5 >= 30? No
 *   Can't reuse room
 *   Heap: [10, 30]
 *   Rooms: 2
 * 
 * Step 3: Meeting [15,20]
 *   Check: 15 >= 10? Yes!
 *   Room freed at 10, can reuse
 *   Poll 10 from heap
 *   Add 20
 *   Heap: [20, 30]
 *   Rooms: 2
 * 
 * Final answer: 2 ✓
 * 
 * 12. VARIATIONS OF THIS PROBLEM:
 * -------------------------------
 * - Meeting Rooms I: Can attend all? (check no overlap)
 * - Meeting Rooms II: Minimum rooms (this problem)
 * - Meeting Rooms III: With k rooms, which used most?
 * - Car Pooling: Similar but with capacity constraints
 * - CPU Scheduling: Similar concept, different context
 * - Interval Intersection: Find overlapping intervals
 * 
 * 13. FOLLOW-UP QUESTIONS YOU MIGHT GET:
 * --------------------------------------
 * Q: "What if we want to know WHICH meetings go in each room?"
 * A: Modify heap to store (endTime, roomId), track assignments
 * 
 * Q: "What if meetings have priorities?"
 * A: Need different algorithm, possibly greedy with priority queue
 * 
 * Q: "Can you handle recurring meetings?"
 * A: Expand recurring meetings into individual instances first
 * 
 * Q: "What if some meetings can be rescheduled?"
 * A: Much harder - becomes optimization problem
 * 
 * Q: "How to minimize room switches for a person?"
 * A: Need to track person-to-room assignments (graph coloring)
 * 
 * Q: "What if rooms have different capacities?"
 * A: Need to match room capacity to meeting size
 * 
 * 14. OPTIMIZATION DISCUSSIONS:
 * -----------------------------
 * Q: "Can we do better than O(n log n)?"
 * A: No, sorting is necessary. O(n log n) is optimal.
 * 
 * Q: "Which approach uses least space?"
 * A: All are O(n). Two-pointer might have lower constant factor.
 * 
 * Q: "What if n is very large?"
 * A: Could use counting sort if time range is limited
 *    But typically O(n log n) is fine even for n=10^6
 * 
 * 15. REAL-WORLD APPLICATIONS:
 * ----------------------------
 * - Conference room scheduling
 * - Railway platform allocation
 * - Server/processor allocation
 * - Classroom scheduling
 * - Operating room scheduling
 * - Vehicle routing (pickup/dropoff)
 * - CPU task scheduling
 * 
 * 16. KEY INSIGHTS TO EMPHASIZE:
 * ------------------------------
 * ✓ "This is about finding maximum concurrent intervals"
 * ✓ "Min-heap tracks when rooms become available"
 * ✓ "Boundary case: start == end doesn't overlap"
 * ✓ "Sorting is crucial for chronological processing"
 * ✓ "Two-pointer is more elegant but same complexity"
 * ✓ "All approaches are O(n log n) - optimal"
 * 
 * 17. TIME MANAGEMENT (45 MIN INTERVIEW):
 * ---------------------------------------
 * 0-5 min:   Understand problem, clarify edge cases
 * 5-10 min:  Explain min-heap approach
 * 10-25 min: Implement min-heap solution
 * 25-30 min: Walk through test case
 * 30-35 min: Discuss complexity
 * 35-40 min: Explain two-pointer alternative
 * 40-45 min: Handle follow-ups
 * 
 * Remember: This is a MEDIUM problem that appears frequently in interviews.
 * The min-heap solution is intuitive and shows good problem-solving skills.
 * Being able to also explain the two-pointer approach demonstrates deep
 * understanding!
 */
