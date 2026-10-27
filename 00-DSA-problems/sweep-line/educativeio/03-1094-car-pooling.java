/**
 * 1094. Car Pooling
 * 
 * ============================================================================
 * 1. PROBLEM STATEMENT
 * ============================================================================
 * You are driving a vehicle with a specific `capacity` of empty seats. The vehicle 
 * only drives east (cannot turn around).
 * You are given an array `trips` where trips[i] = [numPassengers, from, to] 
 * indicates the number of passengers and their pickup and drop-off locations.
 * Passengers are dropped off BEFORE new passengers are picked up at the same location.
 * 
 * Return true if it is possible to pick up and drop off all passengers without 
 * ever exceeding the car's capacity, or false otherwise.
 * 
 * Constraints:
 * - 1 <= trips.length <= 1000
 * - 1 <= numPassengers <= 100
 * - 0 <= from < to <= 1000
 * - 1 <= capacity <= 10^5
 * 
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (To ask the interviewer)
 * ============================================================================
 * 1. Are the locations strictly bounded between 0 and 1000 as stated?
 *    (If unbounded/large like 10^9, we can't use a fixed array and must use a TreeMap or sorting).
 * 2. What happens exactly at the drop-off location if we also have a pick-up there?
 *    (The problem states drop-offs happen BEFORE pick-ups, which means capacity naturally 
 *    frees up first. Good to confirm this sequencing).
 * 3. Can the `trips` array be out of chronological order?
 *    (Prevents the assumption that we can just process the array left-to-right without sorting).
 * 4. Do we need to optimize for a massive number of trips (large N) or massive distances?
 *    (Clarifies the bottleneck: is it N or the distance limit?).
 * 
 * ============================================================================
 * 3. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement & Constraint: 
 * We need to track the concurrent overlap of "passengers in the car" at any given 
 * location and ensure it never spikes above `capacity`. The binding constraint here 
 * is that the max location is strictly <= 1000, which is extremely small!
 * 
 * --- Approach 1: Simulation (Brute Force) ---
 * 1. Idea: We can create an array representing the number of passengers at every 
 *    single kilometer mark. For each trip, we loop from `from` to `to - 1` and 
 *    add the passengers to each kilometer.
 * 2. What I'd naturally try: `int[] car = new int[1001]`. For trip in trips, 
 *    `for (int i = from; i < to; i++) { car[i] += passengers; }`.
 * 3. Why it works: It literally simulates the weight of the car at every inch of the road.
 * 4. Why it's too slow/costly: We are repeatedly adding passengers to the intermediate 
 *    kilometers of long trips.
 * 5. What work is repeated: If a trip goes from km 0 to km 1000, we do 1000 additions 
 *    just to say "they are still in the car".
 * 6. The pivot observation: The number of people in the car ONLY changes when a 
 *    door opens (pick-up or drop-off). The stretches of road in between are static.
 *    
 * Time Complexity: O(N * D) — where N is trips and D is the max distance of a trip.
 * Space Complexity: O(max(to)) — to hold the road array.
 * 
 * --- Approach 2: Sorting and Min-Heap (Intermediate) ---
 * 1. Idea: Treat this like Meeting Rooms. Sort the trips by pickup location. Use a 
 *    Min-Heap to track drop-off locations. 
 * 2. What I'd naturally try: Sort `trips` by `from`. Iterate through, while the heap's 
 *    top drop-off is <= current `from`, pop it and subtract passengers. Then add 
 *    current passengers to total, push current drop-off to heap, check capacity.
 * 3. Why it works: We dynamically process the timeline chronologically.
 * 4. Why it's less than ideal here: We have to sort the intervals and manage a heap. 
 *    O(N log N) is fast, but given the location constraint (1000), we can do better.
 * 5. What work is repeated: We are sorting arbitrary objects and managing a complex 
 *    data structure when the coordinate space is small enough to just index directly.
 * 6. The pivot observation: Since locations are <= 1000, we don't need to sort N items. 
 *    We can use the array indices themselves as a perfect, pre-sorted timeline.
 *    
 * Time Complexity: O(N log N) — because we sort the N trips by start time.
 * Space Complexity: O(N) — because the Min-Heap stores up to N active trips.
 * 
 * --- Approach 3: Difference Array / Line Sweep (Optimal) ---
 * 1. Idea: Create an array of size 1001. A pick-up is a `+passengers` at index `from`. 
 *    A drop-off is a `-passengers` at index `to`. 
 * 2. What I'd naturally try: Iterate through trips, updating `array[from]` and `array[to]`. 
 *    Then, sweep through the array from 0 to 1000, keeping a running total of passengers.
 * 3. Why it works: The prefix sum at any point exactly equals the net passengers currently 
 *    in the car. Because `to` is processed simultaneously with any `from` at the same index,
 *    and we check capacity *after* combining them, drop-offs naturally offset pick-ups!
 * 4. Why this is the ceiling: We do one pass over the N trips (O(N)), and one pass over 
 *    the 1001 locations (O(1000)). No sorting required.
 * 
 * Time Complexity: O(N + 1000) — because we loop through N trips, then loop through 1001 locations.
 * Space Complexity: O(1000) = O(1) — because the array size is strictly fixed at 1001, regardless of N.
 * 
 * Verdict for 45-minute interview:
 * I would write Approach 3 (Difference Array). It leverages the constraints perfectly 
 * to achieve O(N) time and O(1) space, and the code is extremely concise and elegant.
 * 
 * ============================================================================
 * 4. EDGE CASES
 * ============================================================================
 * - Simultaneous drop-off and pick-up: Trip A drops 5 people at km 5, Trip B picks up 
 *   5 people at km 5. The car is at capacity before km 5. (Prefix sum handles this 
 *   perfectly because +5 and -5 cancel out to 0 net change before we check capacity).
 * - Exact capacity reached: Reaching exactly `capacity` is allowed. Must use `>` to fail.
 * - Out-of-order input: Input trips aren't sorted. (Difference array doesn't care, 
 *   it maps straight to indices).
 * - Single passenger trip testing bounds.
 * 
 * ============================================================================
 * 5. KEY INSIGHT, DIAGRAM & DRY RUN
 * ============================================================================
 * > Key Insight: 
 * "Coordinate mapping." When the range of coordinates (time/location) is small, 
 * you can completely bypass sorting events by just using an array where the index 
 * IS the coordinate.
 * 
 * Diagram:
 * Trips: [[2, 1, 5], [3, 3, 7]], Capacity: 4
 * 
 * Indices:  0   1   2   3   4   5   6   7
 * Changes:  0  +2   0  +3   0  -2   0  -3
 * 
 * Sweep (Prefix Sum):
 * km 0: 0
 * km 1: 0 + 2 = 2  (2 <= 4, OK)
 * km 2: 2 + 0 = 2  (2 <= 4, OK)
 * km 3: 2 + 3 = 5  (5 > 4, FAIL!) Return false.
 * 
 * Dry Run:
 * Let's trace the failing state.
 * 1. Initialization: timestamps = int[1001]
 * 2. Process [2, 1, 5]: 
 *    timestamps[1] += 2
 *    timestamps[5] -= 2
 * 3. Process [3, 3, 7]:
 *    timestamps[3] += 3
 *    timestamps[7] -= 3
 * 4. Sweep:
 *    i=0: current = 0
 *    i=1: current += 2 -> 2. 2 <= 4.
 *    i=2: current += 0 -> 2. 2 <= 4.
 *    i=3: current += 3 -> 5. 5 > 4 -> return false.
 * 
 * ============================================================================
 * 6. PITFALLS & PATTERN RECOGNITION
 * ============================================================================
 * Pitfalls:
 * - Doing `timestamps[to - 1] -= passengers`: The problem explicitly states you 
 *   are dropped off BEFORE new passengers are picked up at `to`. So at the exact 
 *   moment of `to`, you are gone. Placing the `-passengers` exactly at `to` ensures 
 *   that when we sum the changes for that kilometer, the drop-offs free up the seats 
 *   for the pick-ups happening at that exact same index.
 * 
 * Pattern Recognition: 
 * "When you need to track capacity/overlap over a bounded 1D line, use a Difference Array."
 * Same as: Maximum Population Year, Corporate Flight Bookings.
 * 
 * ============================================================================
 * 7. INTERVIEW SCRIPT
 * ============================================================================
 * "My first instinct is to sort these trips chronologically and simulate the car 
 * picking up and dropping off, perhaps using a Min-Heap. That would be O(N log N).
 * However, looking at the constraints, the maximum location is only 1000. 
 * Because the coordinate space is so small, we don't need to sort at all. 
 * We can use a difference array of size 1001. For each trip, I'll add the passengers 
 * to the `from` index, and subtract them at the `to` index. Then, a single sweep 
 * left-to-right maintaining a running sum will tell me the exact occupancy of the 
 * car at every kilometer. If it ever exceeds capacity, I return false. 
 * This brings our time complexity down to O(N + 1000) and space to O(1)."
 * 
 * ============================================================================
 * 8. FOLLOW-UPS
 * ============================================================================
 * Q: What if the locations could go up to 10^9? (e.g., cross-country driving).
 * A: The fixed array of size 10^9 would waste too much memory and time. We would switch 
 *    to a `TreeMap<Integer, Integer>`. We put `from` and `to` as keys, and accumulate 
 *    the passenger changes as values. Iterating through the keys of the TreeMap 
 *    gives us the chronological sweep. Time becomes O(N log N) and Space O(N).
 */

import java.util.Map;
import java.util.TreeMap;

public class CarPooling {

    /**
     * APPROACH 3: Difference Array / Line Sweep (Optimal for small coordinates)
     * Time Complexity: O(N + MAX_LOC) -> roughly O(N) since MAX_LOC is 1000.
     * Space Complexity: O(MAX_LOC) -> O(1) auxiliary space since it's bounded at 1001.
     */
    public boolean carPoolingDifferenceArray(int[][] trips, int capacity) {
        // timestamps[i] stores the NET change of passengers at kilometer 'i'.
        // For example, if 3 get on and 2 get off at km 5, timestamps[5] = +1.
        int[] timestamps = new int[1001];
        
        // 1. Record the passenger changes (deltas) at specific locations
        for (int[] trip : trips) {
            int passengers = trip[0];
            int from = trip[1];
            int to = trip[2];
            
            // Passengers board at 'from'
            timestamps[from] += passengers;
            
            // Passengers leave at 'to'. 
            // Placed EXACTLY at 'to' because they vacate the seat before new ones board.
            timestamps[to] -= passengers;
        }
        
        // 2. Sweep chronologically to calculate running capacity
        int currentPassengers = 0;
        for (int i = 0; i <= 1000; i++) {
            // Apply the net change for this specific kilometer
            currentPassengers += timestamps[i];
            
            // If the car is overloaded at any point, the schedule is impossible
            if (currentPassengers > capacity) {
                return false;
            }
        }
        
        // If we survived the whole trip without exceeding capacity, it's possible!
        return true;
    }

    /**
     * GENERALIZED APPROACH: TreeMap (Optimal if locations were up to 10^9)
     * Time Complexity: O(N log N) — due to TreeMap insertions
     * Space Complexity: O(N) — storing up to 2N events in the map
     */
    public boolean carPoolingTreeMap(int[][] trips, int capacity) {
        // TreeMap automatically sorts the keys (locations) chronologically.
        TreeMap<Integer, Integer> timeline = new TreeMap<>();
        
        // 1. Populate the events
        for (int[] trip : trips) {
            int passengers = trip[0];
            int from = trip[1];
            int to = trip[2];
            
            timeline.put(from, timeline.getOrDefault(from, 0) + passengers);
            timeline.put(to, timeline.getOrDefault(to, 0) - passengers);
        }
        
        // 2. Sweep through the sorted events
        int currentPassengers = 0;
        for (int netChange : timeline.values()) {
            currentPassengers += netChange;
            if (currentPassengers > capacity) {
                return false;
            }
        }
        
        return true;
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================
    public static void main(String[] args) {
        CarPooling solution = new CarPooling();
        
        // Test Case 1: Standard failure case (Example 1)
        int[][] trips1 = {{2, 1, 5}, {3, 3, 7}};
        int capacity1 = 4;
        System.out.println("Test 1 (Expected false): " 
            + solution.carPoolingDifferenceArray(trips1, capacity1));
            
        // Test Case 2: Standard success case (Example 2)
        int[][] trips2 = {{2, 1, 5}, {3, 3, 7}};
        int capacity2 = 5;
        System.out.println("Test 2 (Expected true): " 
            + solution.carPoolingDifferenceArray(trips2, capacity2));
            
        // Test Case 3: Edge Case - Simultaneous pickup and drop-off
        // Car capacity 3. Trip A: 3 people from 1->5. Trip B: 3 people from 5->8.
        // At km 5, A leaves (-3) and B enters (+3). Max capacity should remain 3.
        int[][] trips3 = {{3, 1, 5}, {3, 5, 8}};
        int capacity3 = 3;
        System.out.println("Test 3 (Expected true): " 
            + solution.carPoolingDifferenceArray(trips3, capacity3));
            
        // Test Case 4: Unsorted input array
        int[][] trips4 = {{3, 5, 8}, {2, 1, 3}, {1, 2, 6}};
        int capacity4 = 3;
        // km 1: 2
        // km 2: 2 + 1 = 3
        // km 3: 3 - 2 = 1
        // km 5: 1 + 3 = 4 -> Fails!
        System.out.println("Test 4 (Expected false): " 
            + solution.carPoolingDifferenceArray(trips4, capacity4));
            
        // Cross-check with TreeMap generalized approach
        System.out.println("\nCross-checking approaches on Test 4:");
        System.out.println("Difference Array: " + solution.carPoolingDifferenceArray(trips4, capacity4));
        System.out.println("TreeMap Sweep:    " + solution.carPoolingTreeMap(trips4, capacity4));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern: Difference Array (Line Sweep).
 * 
 * Key observation: Look strictly at the constraints. When a sequence involves 
 * intervals but the coordinate bound is tiny (1000), drop sorting and just use 
 * an array map. Index = Time/Location.
 * 
 * Memorize vs Understand: 
 * UNDERSTAND how `array[from] += X` and `array[to] -= X` creates a boundary. 
 * A prefix sum resolves these boundaries exactly back into the active state.
 * 
 * Most common trap: Array out of bounds (using `timestamps[1000]` requires an 
 * array of size 1001) or offsetting the drop-off (-1) incorrectly.
 * 
 * One-line mental trigger: "Small fixed bounds + overlapping weights = Difference Array."
 */









































import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class Solution {

    // Represents a change in the number of passengers at a specific location.
    //
    // location: The kilometer where the passenger count changes.
    // passengerChange:
    //   Positive value -> passengers are picked up.
    //   Negative value -> passengers are dropped off.
    //
    // Example:
    // Event(3, 2)  -> Pick up 2 passengers at kilometer 3.
    // Event(5, -2) -> Drop off 2 passengers at kilometer 5.
    //
    // A record is useful here because each event is an immutable data carrier.
    private record Event(int location, int passengerChange) {
    }

    public boolean carPooling(int[][] trips, int capacity) {

        // STEP 1: Convert every trip into two events.
        //
        // Each trip is represented as:
        // [numberOfPassengers, pickupLocation, dropOffLocation]
        //
        // Instead of simulating every kilometer, we track only the locations
        // where the number of passengers changes.
        //
        // For a trip [2, 1, 5]:
        //   At kilometer 1, the car picks up 2 passengers: +2.
        //   At kilometer 5, the car drops off 2 passengers: -2.
        //
        // Why is this enough?
        // Between pickup and drop-off locations, the passenger count remains
        // unchanged. Therefore, we only need to process these event locations.
        //
        // Every trip produces exactly two events, so there are 2 * n events.
        List<Event> events = new ArrayList<>(2 * trips.length);

        for (int[] trip : trips) {

            int numberOfPassengers = trip[0];
            int pickupLocation = trip[1];
            int dropOffLocation = trip[2];

            // Positive change: passengers enter the car at the pickup location.
            events.add(new Event(pickupLocation, numberOfPassengers));

            // Negative change: passengers leave the car at the drop-off location.
            events.add(new Event(dropOffLocation, -numberOfPassengers));
        }

        // STEP 2: Sort all events by location in ascending order.
        //
        // We must process locations from west to east because the car only
        // travels east. The order of the input trips is irrelevant.
        //
        // If two events have the same location, sort by passengerChange:
        // negative values (drop-offs) come before positive values (pickups).
        //
        // Why?
        // The problem states that passengers are dropped off before new
        // passengers are picked up at the same location.
        //
        // Example:
        // Event(5, -3) comes before Event(5, +2).
        //
        // We will also group all events at the same location below, so the
        // final occupancy check happens after all changes at that location.
        events.sort(
            Comparator.comparingInt(Event::location)
                      .thenComparingInt(Event::passengerChange)
        );

        // STEP 3: Sweep through the sorted events and track occupancy.
        //
        // currentPassengers represents the number of passengers currently
        // inside the car after processing all previously visited locations.
        int currentPassengers = 0;

        // Index of the next event that has not yet been processed.
        int eventIndex = 0;

        while (eventIndex < events.size()) {

            // Identify the next location on the road.
            int currentLocation = events.get(eventIndex).location();

            // STEP 4: Process every event at this same location.
            //
            // Multiple trips can start or end at the same kilometer.
            // We combine all their passenger changes before checking capacity.
            //
            // Because the events are sorted, all events for currentLocation
            // appear consecutively.
            while (eventIndex < events.size()
                    && events.get(eventIndex).location() == currentLocation) {

                Event event = events.get(eventIndex);

                // Apply the event:
                //   Positive change -> occupancy increases.
                //   Negative change -> occupancy decreases.
                currentPassengers += event.passengerChange();

                eventIndex++;
            }

            // STEP 5: Check capacity after all events at this location.
            //
            // All drop-offs and pickups at this location have now been applied.
            // Therefore, currentPassengers is the occupancy after processing
            // the location, consistent with the problem's drop-off-first rule.
            //
            // If occupancy exceeds capacity at any location, the trips cannot
            // all be completed using this vehicle.
            if (currentPassengers > capacity) {
                return false;
            }
        }

        // Every event was processed without exceeding capacity.
        // Each trip contributed an equal positive pickup and negative drop-off,
        // so the passenger count also returns to zero after the final event.
        return true;
    }
}

/*
 * TIME COMPLEXITY: O(n log n)
 *
 * Let n = trips.length.
 *
 * 1. Creating events:
 *    Each trip creates two events, so this takes O(n) time.
 *
 * 2. Sorting events:
 *    There are 2n events. Sorting them takes O(2n log(2n)),
 *    which simplifies to O(n log n).
 *
 * 3. Processing events:
 *    Each event is visited exactly once across the nested loops.
 *    The total processing time is O(n), not O(n^2).
 *
 * Overall:
 *    O(n) + O(n log n) + O(n) = O(n log n).
 *
 * SPACE COMPLEXITY: O(n)
 *
 * We store two Event records per trip, resulting in 2n events.
 * Since constant factors are ignored in asymptotic analysis, auxiliary
 * space is O(n).
 */











































class Solution {

    public boolean carPooling(int[][] trips, int capacity) {

        // WHY THIS IS THE BEST FIT FOR THESE CONSTRAINTS:
        // Locations are bounded: 0 <= location <= 1000.
        // We can allocate an array indexed directly by location.
        //
        // Instead of sorting 2n events, we accumulate every change
        // at its exact location in constant time.
        //
        // TIME COMPLEXITY: O(n + M)
        // n = number of trips.
        // M = maximum location, at most 1000.
        // Building the array takes O(n), and scanning it takes O(M).
        //
        // SPACE COMPLEXITY: O(M)
        // We use one integer array of size M + 1.
        //
        // With the given constraints, this is effectively O(n + 1000)
        // time and O(1000) auxiliary space.

        int maxLocation = 0;

        for (int[] trip : trips) {
            maxLocation = Math.max(maxLocation, trip[2]);
        }

        // change[x] stores the net number of passengers entering
        // or leaving the car at location x.
        int[] change = new int[maxLocation + 1];

        for (int[] trip : trips) {
            int passengers = trip[0];
            int from = trip[1];
            int to = trip[2];

            // Passengers enter the car at the pickup location.
            change[from] += passengers;

            // Passengers leave the car at the drop-off location.
            change[to] -= passengers;
        }

        int passengers = 0;

        // Sweep from west to east.
        // Applying the net change at each location is sufficient:
        // all drop-offs and pickups at that location are combined.
        for (int location = 0;
             location <= maxLocation;
             location++) {

            passengers += change[location];

            // Because all changes at this location are combined,
            // passengers represents occupancy after all drop-offs
            // and pickups at the location have been handled.
            if (passengers > capacity) {
                return false;
            }
        }

        return true;
    }
}
