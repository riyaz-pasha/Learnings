/**
 * 253. Meeting Rooms II
 * 
 * ============================================================================
 * 1. PROBLEM STATEMENT
 * ============================================================================
 * Given an array of meeting time intervals where intervals[i] = [start_i, end_i], 
 * return the minimum number of conference rooms required.
 * 
 * Example 1:
 * Input: intervals = [[0,30],[5,10],[15,20]]
 * Output: 2
 * 
 * Example 2:
 * Input: intervals = [[7,10],[2,4]]
 * Output: 1
 * 
 * Constraints:
 * - 1 <= intervals.length <= 10^4
 * - 0 <= start_i < end_i <= 10^6
 * 
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (To ask the interviewer)
 * ============================================================================
 * 1. If one meeting ends at time T and another starts at time T, do they overlap?
 *    (Crucial for boundary logic: usually [1, 2] and [2, 3] can share 1 room, meaning no overlap).
 * 2. Are the input intervals guaranteed to be sorted by start time?
 *    (Prevents bugs; assume no, meaning we must sort them ourselves).
 * 3. Can the start time and end time be exactly the same (e.g., [5,5])?
 *    (Clarifies 0-duration meetings; constraints say start < end, so no).
 * 4. Could the timestamps be extremely large (e.g., up to 2^31 - 1)?
 *    (If yes, we can't use an array-based frequency map/line sweep of size max_time).
 * 5. Do we just need the count of rooms, or do we need to return the actual schedule/assignments?
 *    (Changes whether we can just count overlaps or if we must group intervals into actual lists).
 * 
 * ============================================================================
 * 3. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement & Constraint: 
 * We need to find the maximum number of concurrent overlaps at any single point in time.
 * The bottleneck is checking overlaps dynamically without comparing every single interval 
 * against every other interval, which would explode at N = 10,000.
 * 
 * --- Approach 1: Simulation (Brute Force) ---
 * 1. Idea: Keep a list of "rooms" (each room is a list of meetings). For every new meeting, 
 *    check if any existing room is free. If yes, add it there. If not, open a new room.
 * 2. What I'd naturally try: Sort intervals by start time. Create an `ArrayList<Integer>` 
 *    tracking the end time of the meeting currently occupying each room.
 * 3. Why it works: It literally simulates a receptionist booking rooms.
 * 4. Why it's too slow/costly: To place a meeting, we might have to scan every existing 
 *    room to find a free one.
 * 5. What work is repeated: We scan rooms that are busy for a long time over and over 
 *    just to find the *one* room that frees up earliest.
 * 6. The pivot observation: We don't care about ALL rooms, we only care about the room 
 *    that frees up the EARLIEST. If the earliest-freeing room isn't available, NO room is available.
 * 
 * Time Complexity: O(N^2) — because for each of the N intervals, we might scan up to N existing rooms.
 * Space Complexity: O(N) — because we store the end times for up to N rooms.
 * 
 * --- Approach 2: Min-Heap (Intermediate / Most Intuitive) ---
 * 1. Idea: Instead of scanning all rooms, keep the rooms' end times in a Min-Heap.
 * 2. What I'd naturally try: Sort meetings by start time. Use a `PriorityQueue` of integers. 
 *    For each meeting, if `start >= heap.peek()`, pop the top (room freed) and push the 
 *    new `end`. If not, just push the new `end` (new room needed).
 * 3. Why it works: The top of the heap is always the room that frees up earliest.
 * 4. Why it's costly: We are doing heap operations (push/pop) for every meeting. 
 *    While log(N) is fast, managing a dynamic data structure has overhead.
 * 5. What work is repeated: We are pairing specific start times with specific end times. 
 *    But to just count the *maximum concurrent meetings*, we don't actually need to know 
 *    *which* meeting ended, just that *a* meeting ended.
 * 6. The pivot observation: The timeline of starts and ends can be treated as completely 
 *    independent events. A start is a +1 to active meetings, an end is a -1. 
 * 
 * Time Complexity: O(N log N) — because we sort the N intervals, and do heap insertions/deletions taking O(log N) each.
 * Space Complexity: O(N) — because the Min-Heap can grow to hold N end times in the worst case (all overlapping).
 * 
 * --- Approach 3: Chronological Sorting / Two Pointers (Optimal) ---
 * 1. Idea: Extract all start times into one array, and all end times into another. 
 *    Sort both. Walk through time using two pointers.
 * 2. What I'd naturally try: Pointer `s` for starts, `e` for ends. If `starts[s] < ends[e]`, 
 *    a meeting started before the earliest one ended (overlap! room++). If `>=` a meeting 
 *    ended, so a room frees up (room--).
 * 3. Why it works: It perfectly tracks the active number of meetings in chronological order 
 *    without simulating the actual pairings. 
 * 4. Why this is the ceiling: We must process elements in time order, so sorting is strictly 
 *    required (unless we use a bounded array line sweep, but constraints say time <= 10^6).
 * 
 * Time Complexity: O(N log N) — because we extract and sort two arrays of size N. The two-pointer pass is just O(N).
 * Space Complexity: O(N) — because we create two auxiliary arrays of size N to hold the starts and ends.
 * 
 * Verdict for 45-minute interview:
 * Both Approach 2 (Min-Heap) and Approach 3 (Two Pointers) are highly acceptable and optimal. 
 * I would write Approach 2 (Min-Heap) if asked to also track/return the schedule. But for just 
 * counting rooms, Approach 3 (Two Pointers) is cleaner, uses less overhead, and shows a deep 
 * understanding of how to decouple intervals into timeline events. I'll provide both below, 
 * focusing the dry run on Two Pointers.
 * 
 * ============================================================================
 * 4. EDGE CASES
 * ============================================================================
 * - Adjacent times: [[1, 5], [5, 10]]. These do not overlap. (start = 5, end = 5 -> end processes first).
 * - Fully nested: [[1, 10], [2, 3], [4, 5]]. The outer meeting envelops multiple small ones.
 * - Same start, different ends: [[1, 5], [1, 10], [1, 2]]. 
 * - Empty array: Intervals = []. Returns 0.
 * 
 * ============================================================================
 * 5. KEY INSIGHT, DIAGRAM & DRY RUN
 * ============================================================================
 * > Key Insight: 
 * "Start times and end times do not need to stay tethered together." 
 * Once we just want to count maximum concurrency, a meeting starting is just a "+1 room", 
 * and *any* meeting ending is a "-1 room". By sorting starts and ends separately, 
 * we just process these +1 and -1 events in chronological order.
 * 
 * Diagram: Chronological Timeline
 * Intervals: [[0,30], [5,10], [15,20]]
 * 
 * Starts Array (Sorted): [0, 5, 15]
 * Ends Array (Sorted):   [10, 20, 30]
 * 
 * Event Timeline:
 * Time:   0       5       10       15       20       30
 * Event:  S(0)    S(5)    E(10)    S(15)    E(20)    E(30)
 * Rooms:  1       2       1        2        1        0
 * Max Rooms = 2.
 * 
 * Dry Run (Two Pointers on [[0,30], [5,10], [15,20]]):
 * starts = [0, 5, 15], ends = [10, 20, 30]
 * s = 0, e = 0, active = 0, max = 0
 * 
 * 1. starts[0](0) < ends[0](10) -> Meeting starts! 
 *    active = 1, max = 1. s increments to 1.
 * 2. starts[1](5) < ends[0](10) -> Meeting starts! 
 *    active = 2, max = 2. s increments to 2.
 * 3. starts[2](15) >= ends[0](10) -> Meeting ends!
 *    active = 1. e increments to 1.
 * 4. starts[2](15) < ends[1](20) -> Meeting starts!
 *    active = 2, max = 2. s increments to 3.
 * 5. `s` is out of bounds (3). We stop. 
 *    (All remaining ends will just decrement `active`, so max cannot increase).
 * Result = 2.
 * 
 * ============================================================================
 * 6. PITFALLS & PATTERN RECOGNITION
 * ============================================================================
 * Pitfalls:
 * - Using `<` instead of `<=` when comparing start and end. If start == end 
 *   (e.g., starting at 5, ending at 5), you must process the END first to free 
 *   the room before the new meeting takes it. So if `starts[s] == ends[e]`, 
 *   you must move the `e` pointer!
 * - Confusing the Two-Pointer array with the actual intervals. `starts[i]` and 
 *   `ends[i]` DO NOT belong to the same meeting anymore. They are just raw events.
 * 
 * Pattern Recognition: 
 * "When you see maximum overlap or concurrent events over time, think Min-Heap or Sweep Line."
 * Transfers to:
 * - Minimum Number of Arrows to Burst Balloons
 * - Car Pooling
 * - Number of Airplanes in the Sky
 * 
 * ============================================================================
 * 7. INTERVIEW SCRIPT
 * ============================================================================
 * "To find the minimum number of rooms, we're basically looking for the maximum 
 * number of concurrent meetings at any point in time. 
 * 
 * One way is to sort the meetings by start time and use a Min-Heap to track the 
 * end times of active meetings. If a new meeting starts after the earliest ending 
 * meeting in the heap, we can reuse that room by popping the heap. 
 * 
 * An even leaner way is to separate the start and end times into two arrays and 
 * sort them. Then we use two pointers. If the next chronological event is a start, 
 * we increment our active room count. If it's an end, we decrement it. We track 
 * the maximum active count seen. This completely removes the overhead of a heap.
 * It'll cost O(N log N) to sort the arrays, and O(N) space. Let's write the 
 * two-pointer approach."
 * 
 * ============================================================================
 * 8. FOLLOW-UPS
 * ============================================================================
 * Q: What if we also needed to return which room each meeting was assigned to?
 * A: The two-pointer approach falls apart because we severed the link between starts 
 *    and ends. We would have to use the Min-Heap approach, but instead of just 
 *    storing `end_time`, the heap would store an object/array: `[end_time, room_id]`.
 * 
 * Q: What if the timestamps were small (e.g., between 0 and 2400 for military time)?
 * A: We wouldn't need sorting or heaps. We could use a Difference Array (Line Sweep) 
 *    of size 2401. `array[start]++`, `array[end]--`, and just do a single prefix 
 *    sum pass. Time complexity drops to O(N + T) where T is 2400.
 */

import java.util.Arrays;
import java.util.PriorityQueue;

public class MeetingRoomsII {

    /**
     * APPROACH 2: Min-Heap
     * Time Complexity: O(N log N) — sorting the intervals + N heap insertions
     * Space Complexity: O(N) — heap can contain up to N elements
     */
    public int minMeetingRoomsHeap(int[][] intervals) {
        if (intervals == null || intervals.length == 0) return 0;
        
        // 1. Sort the intervals by start time
        // e.g., [[15,20], [0,30], [5,10]] -> [[0,30], [5,10], [15,20]]
        Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));
        
        // 2. Initialize a Min-Heap to keep track of end times
        // The top of the heap is ALWAYS the room that frees up earliest.
        PriorityQueue<Integer> earliestFrees = new PriorityQueue<>();
        
        // 3. Iterate through the sorted meetings
        for (int[] meeting : intervals) {
            // If the earliest freeing room is available before this meeting starts,
            // we can reuse the room! Pop it out.
            // Notice: >= handles adjacent meetings (e.g., end at 10, start at 10)
            if (!earliestFrees.isEmpty() && earliestFrees.peek() <= meeting[0]) {
                earliestFrees.poll(); 
            }
            
            // Regardless of whether we reused a room or needed a new one, 
            // this meeting claims a room and will end at meeting[1].
            earliestFrees.offer(meeting[1]);
        }
        
        // The number of elements left in the heap is the maximum number of 
        // concurrent rooms we had open at once.
        return earliestFrees.size();
    }

    /**
     * APPROACH 3: Chronological Sorting / Two Pointers (Optimal / Leanest)
     * Time Complexity: O(N log N) — creating and sorting the arrays
     * Space Complexity: O(N) — two auxiliary arrays of size N
     */
    public int minMeetingRoomsTwoPointers(int[][] intervals) {
        if (intervals == null || intervals.length == 0) return 0;
        
        int n = intervals.length;
        
        // 1. Decouple starts and ends into separate arrays
        // e.g., starts: [0, 5, 15] 
        int[] starts = new int[n];
        // e.g., ends: [30, 10, 20]
        int[] ends = new int[n];
        
        for (int i = 0; i < n; i++) {
            starts[i] = intervals[i][0];
            ends[i] = intervals[i][1];
        }
        
        // 2. Sort both arrays
        // Now ends becomes [10, 20, 30] - completely detached from their original starts!
        Arrays.sort(starts);
        Arrays.sort(ends);
        
        // 3. Walk through the timeline using two pointers
        int startPtr = 0, endPtr = 0;
        int activeRooms = 0;
        int maxRooms = 0;
        
        // We only need to iterate while there are still meetings starting.
        // Once all meetings have started, activeRooms can only decrease.
        while (startPtr < n) {
            // Is the next chronological event a START or an END?
            // Strictly less (<) means a meeting starts before the earliest ending meeting finishes.
            if (starts[startPtr] < ends[endPtr]) {
                activeRooms++;
                startPtr++;
            } else {
                // Otherwise (starts >= ends), a meeting ends before or exactly when 
                // the next one starts. A room frees up.
                activeRooms--;
                endPtr++;
            }
            
            // Track the maximum concurrency we've seen
            maxRooms = Math.max(maxRooms, activeRooms);
        }
        
        return maxRooms;
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================
    public static void main(String[] args) {
        MeetingRoomsII solution = new MeetingRoomsII();
        
        // Test Case 1: Standard overlapping case
        int[][] intervals1 = {{0, 30}, {5, 10}, {15, 20}};
        System.out.println("Test 1 (Expected 2): " 
            + solution.minMeetingRoomsTwoPointers(intervals1));
            
        // Test Case 2: Non-overlapping consecutive meetings
        int[][] intervals2 = {{7, 10}, {2, 4}};
        System.out.println("Test 2 (Expected 1): " 
            + solution.minMeetingRoomsTwoPointers(intervals2));
            
        // Test Case 3: Edge Case - Exact boundary touch
        // Meeting 1 ends at 5, Meeting 2 starts at 5. Should reuse room.
        int[][] intervals3 = {{1, 5}, {5, 10}};
        System.out.println("Test 3 (Expected 1): " 
            + solution.minMeetingRoomsTwoPointers(intervals3));
            
        // Test Case 4: Fully nested meetings
        int[][] intervals4 = {{1, 10}, {2, 3}, {4, 5}};
        System.out.println("Test 4 (Expected 2): " 
            + solution.minMeetingRoomsTwoPointers(intervals4));
            
        // Cross-checking approaches to ensure identical behavior
        System.out.println("\nCross-checking approaches on Test 4:");
        System.out.println("Min-Heap:      " + solution.minMeetingRoomsHeap(intervals4));
        System.out.println("Two Pointers:  " + solution.minMeetingRoomsTwoPointers(intervals4));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern: Timeline Event Extraction (Sweep Line concept).
 * 
 * Key observation: To find maximum concurrency, you don't need to know WHICH 
 * meeting ended, only THAT a meeting ended. This allows you to completely 
 * detach start times from end times and sort them independently.
 * 
 * Memorize vs Understand: 
 * UNDERSTAND why the Two-Pointer array works: you are essentially sweeping a 
 * line across time, and the pointers just tell you the next event (start or end).
 * 
 * Most common trap: Mishandling equality. If start == end, process the END 
 * first so you don't artificially spike the concurrent room count.
 * 
 * One-line mental trigger: "Find max simultaneous intervals? Sort starts, sort ends, walk both pointers."
 */











































import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class Solution {

    /*
     * Each event represents a change in the number of active meetings.
     *
     * time:
     *   The timestamp at which the event occurs.
     *
     * roomChange:
     *   +1 means a meeting starts and occupies a room.
     *   -1 means a meeting ends and frees a room.
     *
     * A record is appropriate here because an event is a simple,
     * immutable data carrier. Java automatically provides its
     * constructor, accessors, equals(), hashCode(), and toString().
     */
    private record Event(int time, int roomChange) {}

    public int minMeetingRooms(int[][] intervals) {

        // An empty schedule needs no meeting rooms.
        // This guard also prevents accessing intervals[0] unnecessarily.
        if (intervals == null || intervals.length == 0) {
            return 0;
        }

        /*
         * Every meeting contributes exactly two events:
         *   1. One start event.
         *   2. One end event.
         *
         * Therefore, there will be exactly 2 * n events for n meetings.
         * Providing the initial capacity avoids unnecessary list resizing.
         */
        List<Event> events = new ArrayList<>(2 * intervals.length);

        for (int[] interval : intervals) {
            int startTime = interval[0];
            int endTime = interval[1];

            // A meeting starting increases simultaneous room usage by one.
            events.add(new Event(startTime, +1));

            // A meeting ending decreases simultaneous room usage by one.
            events.add(new Event(endTime, -1));
        }

        /*
         * Sort events chronologically.
         *
         * First comparator:
         *   Earlier timestamps must be processed first.
         *
         * Second comparator:
         *   When timestamps are equal, sort by roomChange ascending.
         *   Since -1 < +1, ending events are processed before starting
         *   events at the same timestamp.
         *
         * Example:
         *   Meetings [1, 5] and [5, 8] need only ONE room.
         *   At time 5, the first meeting ends before the second begins.
         *
         * This tie-breaking rule implements the half-open interval
         * convention [startTime, endTime).
         */
        events.sort(
            Comparator.comparingInt(Event::time)
                      .thenComparingInt(Event::roomChange)
        );

        // Number of meetings currently occupying rooms as we sweep time.
        int activeMeetings = 0;

        // Maximum simultaneous meetings observed so far.
        // This is the minimum number of rooms needed for the full schedule.
        int maximumRoomsRequired = 0;

        /*
         * Process every event in chronological order.
         *
         * We do not need to assign actual room IDs. We only track how
         * many meetings are active at each event.
         */
        for (Event event : events) {

            // Apply the event:
            // +1 for a start, -1 for an end.
            activeMeetings += event.roomChange();

            /*
             * Every active meeting needs a separate room.
             *
             * If four meetings are simultaneously active, at least four
             * rooms are necessary. Conversely, if the active count never
             * exceeds four, four rooms are sufficient.
             */
            maximumRoomsRequired = Math.max(
                maximumRoomsRequired,
                activeMeetings
            );
        }

        return maximumRoomsRequired;
    }
}










































import java.util.Arrays;
import java.util.PriorityQueue;

class Solution {

    /*
     * Represents one meeting with its start and end timestamps.
     *
     * Keeping these values together makes the code more readable than
     * repeatedly accessing interval[0] and interval[1].
     */
    private record Meeting(int startTime, int endTime) {}

    /*
     * Represents the time at which an allocated room becomes available.
     *
     * We store one Room record for each currently occupied room.
     * The record contains the end time of the meeting using that room.
     *
     * A record is useful because this value is immutable: once inserted,
     * the room's availability time does not change.
     */
    private record Room(int availableAt) {}

    public int minMeetingRooms(int[][] intervals) {

        // No meetings means no rooms are needed.
        if (intervals == null || intervals.length == 0) {
            return 0;
        }

        /*
         * Convert the input arrays into meaningful Meeting records.
         *
         * We create a separate array instead of sorting the caller's
         * input directly, so the original intervals remain unchanged.
         *
         * The conversion takes O(n) time and O(n) additional space.
         */
        Meeting[] meetings = Arrays.stream(intervals)
            .map(interval -> new Meeting(interval[0], interval[1]))
            .toArray(Meeting[]::new);

        /*
         * Meetings must be processed in nondecreasing start-time order.
         *
         * Why?
         * When processing a meeting, all meetings that start earlier
         * have already been considered. This makes it safe to reuse a
         * room whose previous meeting has already ended.
         *
         * Integer.compare avoids the potential integer overflow that
         * can occur with subtraction-based comparators.
         */
        Arrays.sort(
            meetings,
            Comparator.comparingInt(Meeting::startTime)
        );

        /*
         * The priority queue is a min-heap ordered by availability time.
         *
         * roomAvailability.peek() returns the room that becomes free
         * earliest.
         *
         * Invariant:
         * Every entry represents a room currently assigned to a meeting
         * that has not yet been released while processing the current
         * meeting.
         */
        PriorityQueue<Room> roomAvailability = new PriorityQueue<>(
            Comparator.comparingInt(Room::availableAt)
        );

        // Track the maximum number of simultaneously occupied rooms.
        int maximumRoomsRequired = 0;

        for (Meeting meeting : meetings) {
            int startTime = meeting.startTime();
            int endTime = meeting.endTime();

            /*
             * Release all rooms whose meetings have ended by this
             * meeting's start time.
             *
             * Why <= instead of <?
             * A room becomes reusable at the exact moment its meeting
             * ends. For example, [1, 5] and [5, 8] can share one room.
             *
             * Why while instead of if?
             * Several meetings may have ended before this meeting starts.
             * We must remove every finished meeting, not just one.
             */
            while (!roomAvailability.isEmpty()
                    && roomAvailability.peek().availableAt() <= startTime) {
                roomAvailability.poll();
            }

            /*
             * Now every room remaining in the heap is occupied by a
             * meeting that overlaps the current meeting's start.
             *
             * Insert the current meeting's end time to reserve a room
             * until this meeting finishes.
             *
             * If a room was freed above, its heap entry was removed and
             * this insertion represents reusing that room. We only need
             * the number of rooms, not their actual identities.
             */
            roomAvailability.offer(new Room(endTime));

            /*
             * The heap size now equals the number of rooms required by
             * the meetings processed so far at this point in time.
             *
             * Its maximum is the answer.
             *
             * We do not simply return the final heap size because rooms
             * may become free later. The maximum heap size records the
             * peak occupancy across the entire schedule.
             */
            maximumRoomsRequired = Math.max(
                maximumRoomsRequired,
                roomAvailability.size()
            );
        }

        return maximumRoomsRequired;
    }
}

