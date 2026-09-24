import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Meeting Rooms III
 *
 * We simulate room assignment using two heaps:
 *
 * availableRooms: min-heap of free room numbers
 * busyRooms: min-heap of (endTime, roomNumber)
 *
 * Rule:
 * - If room is available at meeting start time, assign lowest room number
 * - Otherwise delay meeting until earliest finishing room becomes free
 * - If multiple rooms free at same time, choose lowest room number
 *
 * Time Complexity:
 *   Sorting meetings: O(m log m)
 *   Each meeting causes heap operations: O(log n)
 *   Total: O(m log m + m log n)
 *
 * Space Complexity:
 *   O(n) heaps + O(n) counts
 */
class MeetingRoomsIII {

    record BusyRoom(long endTime, int room) {}

    public int mostBooked(int n, int[][] meetings) {

        // Sort meetings by original start time
        Arrays.sort(meetings, Comparator.comparingInt(a -> a[0]));

        // MinHeap of available room numbers
        PriorityQueue<Integer> availableRooms = new PriorityQueue<>();
        for (int i = 0; i < n; i++) {
            availableRooms.offer(i);
        }

        // MinHeap of busy rooms sorted by earliest end time then smallest room number
        PriorityQueue<BusyRoom> busyRooms = new PriorityQueue<>(
                (a, b) -> {
                    if (a.endTime != b.endTime) {
                        return Long.compare(a.endTime, b.endTime);
                    }
                    return Integer.compare(a.room, b.room);
                }
        );

        // count[i] = how many meetings room i has hosted
        int[] count = new int[n];

        for (int[] meeting : meetings) {
            long start = meeting[0];
            long end = meeting[1];
            long duration = end - start;

            // -------------------------------
            // Free all rooms that are done before meeting start
            // -------------------------------
            while (!busyRooms.isEmpty() && busyRooms.peek().endTime <= start) {
                availableRooms.offer(busyRooms.poll().room);
            }

            // -------------------------------
            // Assign room
            // -------------------------------
            if (!availableRooms.isEmpty()) {
                // Room available, take smallest room number
                int room = availableRooms.poll();
                count[room]++;

                // Meeting ends at original end time
                busyRooms.offer(new BusyRoom(end, room));

            } else {
                // No room available => delay meeting
                BusyRoom earliest = busyRooms.poll();

                int room = earliest.room;
                long freeTime = earliest.endTime;

                count[room]++;

                // Meeting starts when room becomes free, same duration
                long newEnd = freeTime + duration;

                busyRooms.offer(new BusyRoom(newEnd, room));
            }
        }

        // -------------------------------
        // Find room with max meetings
        // tie => smallest room id
        // -------------------------------
        int bestRoom = 0;
        for (int i = 1; i < n; i++) {
            if (count[i] > count[bestRoom]) {
                bestRoom = i;
            }
        }

        return bestRoom;
    }
}

/*
 * 
 * # ✅ Why 1 Heap Is Not Enough (Conceptually)
 * 
 * If you keep only **one heap** like this:
 * 
 * ```
 * (endTime, roomId)
 * ```
 * 
 * This heap always gives you the room that becomes free the earliest.
 * 
 * But the problem has **two different selection rules**:
 * 
 * ---
 * 
 * # 🎯 Rule A (when rooms are available)
 * 
 * If multiple rooms are free at meeting start time:
 * 
 * 👉 choose the **smallest roomId**
 * 
 * That means we need to know:
 * 
 * which rooms are currently free
 * among them, pick smallest roomId
 * 
 * ---
 * 
 * # 🎯 Rule B (when all rooms are busy)
 * 
 * If no room is free:
 * 
 * 👉 choose the room with the **earliest endTime**
 * (tie → smallest roomId)
 * 
 * That means we need:
 * 
 * room with smallest endTime
 * 
 * ---
 * 
 * # 🚨 Problem with Single Heap
 * 
 * Suppose heap contains:
 * 
 * ```
 * (100, room 0)
 * (5, room 3)
 * (7, room 1)
 * ```
 * 
 * Now current meeting starts at `start = 50`.
 * 
 * Rooms with endTime <= 50 are free:
 * 
 * room 3 (end 5)
 * room 1 (end 7)
 * 
 * Now rule says:
 * 👉 pick smallest roomId among free = **room 1**
 * 
 * But if you only have `(endTime, roomId)` heap:
 * 
 * you pop room 3 (because it ends earliest)
 * but you don't know room 1 is also free unless you pop more
 * 
 * So you'd have to:
 * 
 * pop all rooms with endTime <= start
 * collect them temporarily
 * pick smallest roomId
 * push remaining back
 * 
 * That becomes **extra work every meeting**.
 * 
 * ---
 * 
 * # ✅ Two Heaps Cleanly Separate Two Concepts
 * 
 * ## Heap 1: available rooms
 * 
 * Stores only free rooms:
 * 
 * ```
 * (roomId)
 * ```
 * 
 * so we can instantly get smallest roomId.
 * 
 * ## Heap 2: busy rooms
 * 
 * Stores busy rooms:
 * 
 * ```
 * (endTime, roomId)
 * ```
 * 
 * so we can instantly get earliest finishing room.
 * 
 * ---
 * 
 * # 🔥 Why Two Heaps Is the Standard Correct Solution
 * 
 * Because the problem requires:
 * 
 * min by roomId (when free)
 * min by endTime (when busy)
 * 
 * Those are two different orderings → two different heaps.
 * 
 * ---
 * 
 * # ✅ Can You Do It With One Heap?
 * 
 * Yes, but you’ll end up doing something like:
 * 
 * pop all rooms that are free
 * store them temporarily
 * pick min roomId
 * push others back
 * 
 * That makes the solution:
 * 
 * more complex
 * more error-prone
 * potentially worse performance
 * 
 * In Google interview terms:
 * 👉 **Two heaps is the clean, expected L5 approach.**
 * 
 * ---
 * 
 */

/**
 * PROBLEM ANALYSIS AND APPROACH
 * ==============================
 * 
 * PROBLEM UNDERSTANDING:
 * - We have n rooms numbered 0 to n-1
 * - Meetings have start and end times [start, end) - half-closed interval
 * - Need to allocate rooms based on specific rules
 * - Return the room that held the most meetings (lowest number if tie)
 * 
 * KEY RULES:
 * 1. Use the lowest numbered available room
 * 2. If no room available, delay the meeting (same duration)
 * 3. When room becomes free, prioritize meetings by ORIGINAL start time
 * 
 * INTERVIEW APPROACH - HOW TO THINK THROUGH THIS:
 * ================================================
 * 
 * Step 1: Clarify the problem
 * - "So meetings must use the lowest available room number?"
 * - "Delayed meetings keep their original duration?"
 * - "We prioritize by original start time, not delayed start time?"
 * 
 * Step 2: Think about data structures
 * - Need to track: which rooms are free/busy
 * - Need to track: when each room becomes free
 * - Need to handle: delayed meetings in priority order
 * 
 * Step 3: Identify the core challenge
 * - This is a SIMULATION problem with PRIORITY QUEUES
 * - Two priority queues needed:
 *   a) Available rooms (min heap by room number)
 *   b) Busy rooms (min heap by end time, then room number)
 * - One priority queue for delayed meetings (min heap by original start time)
 * 
 * Step 4: Walk through an example
 * - Trace through Example 1 step by step
 * - Identify edge cases: all rooms busy, multiple meetings ending at same time
 * 
 * Step 5: Think about complexity
 * - Time: O(m log(m + n)) where m = meetings
 * - Space: O(m + n)
 * 
 * COMMON PITFALLS IN INTERVIEWS:
 * ===============================
 * 1. Forgetting that delayed meetings keep ORIGINAL start time for priority
 * 2. Not handling the case where multiple rooms free up at the same time
 * 3. Incorrect heap comparators (especially for busy rooms)
 * 4. Off-by-one errors with half-closed intervals
 * 5. Not tracking meeting counts per room correctly
 */

class MeetingRoomsIIISolution {
    
    /**
     * MAIN SOLUTION - SIMULATION WITH PRIORITY QUEUES
     * 
     * INTUITION:
     * - Process meetings in order of original start time
     * - Track available and busy rooms using heaps
     * - When processing a meeting, first free up any rooms that are done
     * - Then assign to lowest available room, or delay if none available
     */
    public int mostBooked(int n, int[][] meetings) {
        // Step 1: Sort meetings by start time
        // This ensures we process meetings in chronological order
        Arrays.sort(meetings, (a, b) -> Integer.compare(a[0], b[0]));
        
        // Step 2: Initialize data structures
        
        // Available rooms: min heap by room number
        // Why: Always want the lowest numbered room
        PriorityQueue<Integer> availableRooms = new PriorityQueue<>();
        for (int i = 0; i < n; i++) {
            availableRooms.offer(i);
        }
        
        // Busy rooms: min heap by [end_time, room_number]
        // Why: Need to know which room becomes free first
        // Secondary sort by room number for deterministic behavior
        PriorityQueue<long[]> busyRooms = new PriorityQueue<>((a, b) -> {
            if (a[0] != b[0]) return Long.compare(a[0], b[0]); // end time
            return Long.compare(a[1], b[1]); // room number
        });
        
        // Track meeting count per room
        int[] meetingCount = new int[n];
        
        // Step 3: Process each meeting
        for (int[] meeting : meetings) {
            long start = meeting[0];
            long end = meeting[1];
            long duration = end - start;
            
            // Free up all rooms that are done by current meeting's start time
            // This is CRITICAL: process all rooms that become free
            while (!busyRooms.isEmpty() && busyRooms.peek()[0] <= start) {
                long[] room = busyRooms.poll();
                availableRooms.offer((int) room[1]); // room number
            }
            
            // Case 1: There's an available room
            if (!availableRooms.isEmpty()) {
                int room = availableRooms.poll();
                busyRooms.offer(new long[]{end, room});
                meetingCount[room]++;
            } 
            // Case 2: All rooms busy - delay the meeting
            else {
                // Find the room that becomes free earliest
                long[] earliestRoom = busyRooms.poll();
                long roomEndTime = earliestRoom[0];
                int roomNumber = (int) earliestRoom[1];
                
                // Schedule meeting in this room
                // New end time = room's end time + duration
                long newEndTime = roomEndTime + duration;
                busyRooms.offer(new long[]{newEndTime, roomNumber});
                meetingCount[roomNumber]++;
            }
        }
        
        // Step 4: Find room with most meetings (lowest number if tie)
        int maxMeetings = 0;
        int resultRoom = 0;
        for (int i = 0; i < n; i++) {
            if (meetingCount[i] > maxMeetings) {
                maxMeetings = meetingCount[i];
                resultRoom = i;
            }
        }
        
        return resultRoom;
    }
    
    /**
     * ALTERNATIVE SOLUTION - MORE EXPLICIT DELAYED QUEUE
     * 
     * This version explicitly maintains a queue of delayed meetings
     * Useful if the problem is extended to track individual meeting delays
     */
    public int mostBookedAlternative(int n, int[][] meetings) {
        Arrays.sort(meetings, (a, b) -> Integer.compare(a[0], b[0]));
        
        PriorityQueue<Integer> availableRooms = new PriorityQueue<>();
        for (int i = 0; i < n; i++) {
            availableRooms.offer(i);
        }
        
        // [end_time, room_number]
        PriorityQueue<long[]> busyRooms = new PriorityQueue<>((a, b) -> {
            if (a[0] != b[0]) return Long.compare(a[0], b[0]);
            return Long.compare(a[1], b[1]);
        });
        
        // [original_start, duration, index] - for tracking purposes
        PriorityQueue<long[]> delayedMeetings = new PriorityQueue<>((a, b) -> 
            Long.compare(a[0], b[0])
        );
        
        int[] meetingCount = new int[n];
        
        for (int i = 0; i < meetings.length; i++) {
            long start = meetings[i][0];
            long end = meetings[i][1];
            long duration = end - start;
            
            // Process delayed meetings and current meeting together
            delayedMeetings.offer(new long[]{start, duration, i});
            
            // Free up rooms
            while (!busyRooms.isEmpty() && !delayedMeetings.isEmpty()) {
                long nextMeetingTime = delayedMeetings.peek()[0];
                long nextRoomFreeTime = busyRooms.peek()[0];
                
                // If room becomes free before or at next meeting time
                if (nextRoomFreeTime <= nextMeetingTime) {
                    long[] room = busyRooms.poll();
                    availableRooms.offer((int) room[1]);
                } else {
                    break;
                }
            }
            
            // Assign meetings to rooms
            while (!delayedMeetings.isEmpty() && !availableRooms.isEmpty()) {
                long[] meeting = delayedMeetings.poll();
                int room = availableRooms.poll();
                long meetingStart = Math.max(meeting[0], 
                    busyRooms.isEmpty() ? 0 : busyRooms.peek()[0]);
                busyRooms.offer(new long[]{meetingStart + meeting[1], room});
                meetingCount[room]++;
            }
        }
        
        // Handle remaining delayed meetings
        while (!delayedMeetings.isEmpty()) {
            long[] meeting = delayedMeetings.poll();
            long[] room = busyRooms.poll();
            long newEnd = room[0] + meeting[1];
            busyRooms.offer(new long[]{newEnd, room[1]});
            meetingCount[(int) room[1]]++;
        }
        
        int maxMeetings = 0;
        int resultRoom = 0;
        for (int i = 0; i < n; i++) {
            if (meetingCount[i] > maxMeetings) {
                maxMeetings = meetingCount[i];
                resultRoom = i;
            }
        }
        
        return resultRoom;
    }

}

/**
 * COMPLEXITY ANALYSIS
 * ===================
 * 
 * Time Complexity: O(m * log(m + n))
 * - Sorting meetings: O(m log m)
 * - For each meeting (m meetings):
 *   - Free up rooms: O(n log n) in worst case
 *   - Assign room: O(log n)
 * - Total: O(m log m + m * n log n) = O(m * log(m + n))
 * 
 * Space Complexity: O(m + n)
 * - availableRooms heap: O(n)
 * - busyRooms heap: O(n)
 * - meetingCount array: O(n)
 * - Total: O(n)
 * 
 * INTERVIEW TIPS
 * ==============
 * 
 * 1. START WITH CLARIFYING QUESTIONS:
 *    - "Can meetings have the same start time?" (Yes, problem says all unique)
 *    - "What happens if multiple rooms free up at the same time?" (Use lowest)
 *    - "Can meeting duration be 0?" (No, end > start)
 * 
 * 2. WALK THROUGH A SMALL EXAMPLE:
 *    - Draw a timeline
 *    - Show room assignments visually
 *    - Trace through delayed meetings
 * 
 * 3. IDENTIFY THE KEY INSIGHT:
 *    - "This is a simulation problem"
 *    - "We need to track time progression"
 *    - "Priority queues handle 'next available' elegantly"
 * 
 * 4. BUILD INCREMENTALLY:
 *    - First: Just assign to rooms without delays
 *    - Second: Add delay logic
 *    - Third: Track counts and find max
 * 
 * 5. HANDLE EDGE CASES:
 *    - Single room
 *    - All meetings fit without delay
 *    - All meetings need delay
 *    - Tie in meeting counts
 * 
 * 6. USE LONG FOR TIME:
 *    - Prevents overflow when meetings are delayed significantly
 *    - Safer than int for time calculations
 * 
 * 7. EXPLAIN YOUR THOUGHT PROCESS:
 *    - "I'm using a min heap for available rooms to get the lowest number"
 *    - "I'm using a min heap for busy rooms sorted by end time"
 *    - "This gives us O(log n) operations"
 */
