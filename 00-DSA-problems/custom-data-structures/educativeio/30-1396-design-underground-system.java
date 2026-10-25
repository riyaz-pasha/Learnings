/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * An underground railway system keeps track of customer travel times between 
 * different stations to calculate average travel times.
 * 
 * Implement the UndergroundSystem class:
 * - checkIn(id, stationName, t): A customer with ID checks in at time t.
 * - checkOut(id, stationName, t): A customer with ID checks out at time t.
 * - getAverageTime(startStation, endStation): Returns the average travel time.
 * 
 * Constraints:
 * - 1 <= id, t <= 10^6
 * - station names are 1 to 10 alphanumeric characters.
 * - Max 20,000 calls.
 * - A -> B is different from B -> A.
 * - All calls are chronologically consistent.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Can the same user travel multiple times?
 *    (Yes, the system must handle a user checking in, checking out, and checking in again later).
 * 2. Will the sum of travel times for a specific route exceed the 32-bit integer limit?
 *    (A trip could take 10^6 time units, and with 20,000 trips, the sum could reach 2 * 10^{10}, 
 *    which exceeds the `int` limit of ~2.14 * 10^9. We MUST use `long` or `double` for the total sum).
 * 3. Does checking out clear the check-in data for that user?
 *    (Yes. To prevent memory leaks in a long-running system, we must delete the check-in 
 *    record once the trip completes).
 * 4. Are `startStation` and `endStation` guaranteed to have at least one completed trip before querying?
 *    (Yes, the constraints guarantee this, saving us from a divide-by-zero check).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: Pair a delayed event (check-out) with its origin event (check-in), 
 *   compute the duration, and efficiently aggregate these durations by route.
 * 
 * --- Approach 1: The Historical Log (Brute Force) ---
 * 1. Intuition: Store every completed trip in a massive list.
 * 2. Try: 
 *    - `checkIn`: Save `(station, time)` in a map keyed by `id`.
 *    - `checkOut`: Look up the `id`, calculate `duration = checkoutTime - checkinTime`. 
 *      Append `(startStation, endStation, duration)` to a global `List<Trip>`.
 *    - `getAverageTime`: Iterate through the ENTIRE `List<Trip>`. If the start and end 
 *      stations match, add to a running sum and increment a counter. Return `sum / count`.
 * 3. Why it works: It perfectly captures all data and calculates exact averages.
 * 4. Why it's too costly: If 10,000 trips happen on the "Leyton -> Waterloo" route, querying 
 *    the average requires iterating through all 10,000 records every single time.
 * 5. Bottleneck: Lazy calculation on reads. We are re-summing historical data that never changes.
 * 
 * Time Complexity: checkIn/checkOut: O(1). getAverageTime: O(N) where N is total completed trips.
 * Space Complexity: O(N) to store every single trip in history.
 * 
 * --- Approach 2: On-the-Fly Aggregation (Optimal) ---
 * 1. Intuition: We don't actually care about individual historical trips. We only care about 
 *    the `total sum` of travel times and the `total count` of trips for a given route.
 * 2. Try: 
 *    - `checkInMap`: Maps `id` -> `(startStation, checkInTime)`.
 *    - `routeStatsMap`: Maps a route string `"startStation->endStation"` -> `(totalTime, count)`.
 * 3. Why it works: When a user checks out, we immediately compute their trip duration, add it 
 *    to the `totalTime` for that route, and increment the `count`. We then discard the individual trip.
 * 4. Which to write in a 45-min interview?
 *    Approach 2. It demonstrates the fundamental data engineering principle of pre-aggregating 
 *    metrics to achieve O(1) reads, trading a tiny bit of write work for massive read speedups.
 * 
 * Time Complexity: O(1) for ALL methods (checkIn, checkOut, getAverageTime).
 * Space Complexity: O(U + R) where U is the number of currently active users (in-transit) 
 *                   and R is the number of unique routes. (Vastly less than O(N) total trips).
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Integer Overflow on Sum: Solved by using `long` or `double` for the total time accumulator.
 * - Route key collisions: A route string must be safely delimited. If we just concatenate 
 *   `start + end`, then "A" + "BC" is "ABC", which collides with "AB" + "C". Since station 
 *   names are alphanumeric, using a comma `start + "," + end` guarantees unique route keys.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Forget the past, remember the metrics." 
 *   Once a trip is completed and added to the aggregate statistics (sum and count), 
 *   the individual trip data is completely useless. Delete it to save memory.
 * 
 * ASCII DRY RUN
 * 1. checkIn(45, "Leyton", 3)
 *    checkInMap: { 45 -> ("Leyton", 3) }
 * 
 * 2. checkIn(32, "Paradise", 8)
 *    checkInMap: { 45 -> ("Leyton", 3), 32 -> ("Paradise", 8) }
 * 
 * 3. checkOut(45, "Waterloo", 15)
 *    a. Fetch 45 from checkInMap: Origin="Leyton", Time=3.
 *    b. Remove 45 from checkInMap (Memory freed!).
 *    c. Route = "Leyton,Waterloo". Duration = 15 - 3 = 12.
 *    d. routeStatsMap: { "Leyton,Waterloo" -> (sum: 12, count: 1) }
 * 
 * 4. getAverageTime("Leyton", "Waterloo")
 *    a. Route = "Leyton,Waterloo".
 *    b. Fetch from routeStatsMap: sum = 12, count = 1.
 *    c. Return 12.0 / 1 = 12.0.
 * 
 * Pitfalls:
 * - Forgetting to remove the user from the `checkInMap` upon checkout. In a real-world system 
 *   running for months, this would cause a massive memory leak as the map grows indefinitely.
 * 
 * Pattern Recognition: "Pairing delayed events + Aggregation" -> Two HashMaps (One for pending state, one for aggregated state).
 * 
 * Interview Script snippet:
 * "Since we only care about the average, storing a list of all historical trips wastes space 
 * and makes querying O(N). I'll use two maps. The first map tracks active trips by User ID. 
 * The second map tracks aggregated statistics (total time and count) keyed by a unique route 
 * string. Upon checkout, I calculate the duration, update the aggregate stats in O(1), and 
 * crucially, remove the user from the active trips map to prevent memory leaks."
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: What if we needed to get the average time for the last 7 days only?
 * A1: The simple running total fails because we can't "subtract" trips that age out without 
 *     knowing exactly when they happened. We would need to keep historical data. To optimize, 
 *     we could maintain a sliding window using a Queue for each route, or bucket trips by day, 
 *     evicting buckets older than 7 days.
 * 
 * Q2: How would this scale in a distributed system where check-ins and check-outs happen on different servers?
 * A2: We would decouple this. The `checkIn` server writes to a distributed cache (like Redis). 
 *     The `checkOut` server reads from Redis, computes the duration, and publishes an event 
 *     (e.g., to Kafka). An aggregator service consumes these events and updates the route 
 *     statistics in a database.
 */

import java.util.HashMap;
import java.util.Map;

public class UndergroundSystemStudy {

    static class UndergroundSystem {

        /**
         * Helper class to store the check-in state of a user.
         */
        private static class CheckInInfo {
            String station;
            int time;

            CheckInInfo(String station, int time) {
                this.station = station;
                this.time = time;
            }
        }

        /**
         * Helper class to store the aggregated statistics of a route.
         */
        private static class RouteStats {
            long totalTime; // Use long to prevent integer overflow for highly traveled routes
            int tripCount;

            RouteStats(long totalTime, int tripCount) {
                this.totalTime = totalTime;
                this.tripCount = tripCount;
            }
        }

        // Map 1: Tracks customers currently in transit. Key = Customer ID.
        private Map<Integer, CheckInInfo> activeTrips;
        
        // Map 2: Tracks aggregated trip metrics. Key = "startStation,endStation".
        private Map<String, RouteStats> routeData;

        public UndergroundSystem() {
            activeTrips = new HashMap<>();
            routeData = new HashMap<>();
        }

        /**
         * A customer checks in at the station at time t.
         * Time Complexity: O(1)
         * Space Complexity: O(1) auxiliary per call.
         */
        public void checkIn(int id, String stationName, int t) {
            // Record the active trip
            activeTrips.put(id, new CheckInInfo(stationName, t));
        }

        /**
         * A customer checks out from the station at time t.
         * Time Complexity: O(1)
         * Space Complexity: O(1) auxiliary per call.
         */
        public void checkOut(int id, String stationName, int t) {
            // 1. Retrieve AND REMOVE the check-in info. 
            // Removing is critical to prevent memory leaks in long-running systems.
            CheckInInfo checkInInfo = activeTrips.remove(id);
            
            // 2. Calculate the trip duration
            String startStation = checkInInfo.station;
            int travelTime = t - checkInInfo.time;
            
            // 3. Generate a safe, unique route key
            String routeKey = startStation + "," + stationName;
            
            // 4. Update the aggregate metrics for this route
            RouteStats stats = routeData.getOrDefault(routeKey, new RouteStats(0, 0));
            stats.totalTime += travelTime;
            stats.tripCount += 1;
            
            // Save it back to the map (necessary if it was newly created by getOrDefault)
            routeData.put(routeKey, stats);
        }

        /**
         * Returns the average time it takes to travel from startStation to endStation.
         * Time Complexity: O(1)
         */
        public double getAverageTime(String startStation, String endStation) {
            String routeKey = startStation + "," + endStation;
            
            // Constraints guarantee this route has been traveled at least once,
            // so we don't strictly need a null check or divide-by-zero protection here,
            // but it's good practice.
            RouteStats stats = routeData.get(routeKey);
            
            // Cast to double to ensure floating point division
            return (double) stats.totalTime / stats.tripCount;
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        UndergroundSystem system = new UndergroundSystem();

        // 1. Initial check-ins
        system.checkIn(45, "Leyton", 3);
        system.checkIn(32, "Paradise", 8);
        system.checkIn(27, "Leyton", 10);
        
        // 2. Check-outs
        system.checkOut(45, "Waterloo", 15); // Leyton -> Waterloo in 12
        system.checkOut(27, "Waterloo", 20); // Leyton -> Waterloo in 10
        system.checkOut(32, "Cambridge", 22); // Paradise -> Cambridge in 14
        
        // 3. Averages
        System.out.println("Paradise -> Cambridge: " + 
            system.getAverageTime("Paradise", "Cambridge")); // Expected: 14.0
            
        System.out.println("Leyton -> Waterloo: " + 
            system.getAverageTime("Leyton", "Waterloo")); // Expected: 11.0 ((12 + 10) / 2)
            
        // 4. Additional trip on existing route
        system.checkIn(10, "Leyton", 24);
        system.checkOut(10, "Waterloo", 38); // Leyton -> Waterloo in 14
        
        System.out.println("Leyton -> Waterloo after 3rd trip: " + 
            system.getAverageTime("Leyton", "Waterloo")); // Expected: 12.0 ((12 + 10 + 14) / 3)
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: Two HashMaps (Pending State -> Aggregated State).
 * Key Observation: Storing the history of all completed trips is wasteful and slows 
 * down queries. By maintaining a running sum and count for each route, we can instantly 
 * answer average queries in O(1) time.
 * What to Memorize: Use a comma `,` delimiter when building route strings to prevent 
 * key collisions (e.g., `start + "," + end`). 
 * Most Common Trap: Integer overflow on the `totalTime` variable. Always use a `long` 
 * for running sums of values that can individually reach high constraints (like timestamps).
 * Mental Trigger: "Pairing delayed events and averaging" -> "Check-in Map + Route Stats Map".
 * ============================================================================
 */












































import java.util.HashMap;
import java.util.Map;

class UndergroundSystem {

    /*
     * Immutable check-in data.
     *
     * A Java record provides:
     * - A compact constructor declaration.
     * - Final record components.
     * - Accessors such as station() and time().
     *
     * A customer can have at most one active trip, so customer ID is the key.
     */
    private record CheckIn(String station, int time) {}

    /*
     * A route is directional.
     *
     * Route("Leyton", "Waterloo") is NOT equal to
     * Route("Waterloo", "Leyton").
     *
     * Records automatically implement value-based equals() and hashCode(),
     * making them convenient and safe HashMap keys.
     */
    private record Route(String start, String end) {}

    /*
     * A completed route needs only:
     * 1. The sum of all journey durations.
     * 2. The number of completed journeys.
     *
     * This is deliberately a class rather than a record because its fields
     * are updated incrementally whenever a new trip completes.
     */
    private static final class RouteStats {
        private long totalDuration;
        private long journeyCount;

        private void addJourney(int duration) {
            totalDuration += duration;
            journeyCount++;
        }

        private double average() {
            // Cast before division to ensure floating-point division.
            return (double) totalDuration / journeyCount;
        }
    }

    /*
     * Map 1: Customer ID -> currently active trip.
     *
     * Contains only customers who have checked in but have not checked out.
     *
     * Map 2: Directed route -> aggregate statistics.
     *
     * Contains only routes for which at least one journey has completed.
     */
    private final Map<Integer, CheckIn> activeTrips = new HashMap<>();
    private final Map<Route, RouteStats> statsByRoute = new HashMap<>();

    public UndergroundSystem() {
        // HashMap fields are initialized above; no additional setup is needed.
    }

    public void checkIn(int id, String stationName, int t) {
        /*
         * Save the customer's origin and entry time.
         *
         * Time: O(1) average for HashMap.put().
         * Space: O(1) additional space for this active trip.
         *
         * The problem guarantees consistent calls, so this customer does not
         * already have another active check-in that needs special handling.
         */
        activeTrips.put(id, new CheckIn(stationName, t));
    }

    public void checkOut(int id, String stationName, int t) {
        /*
         * Step 1: Retrieve and remove the active trip.
         *
         * remove() both retrieves the original check-in and ensures the
         * completed customer is no longer counted as active.
         *
         * Time: O(1) average.
         */
        CheckIn checkIn = activeTrips.remove(id);

        /*
         * Step 2: Calculate the duration.
         *
         * Since the events are valid and chronological, t > checkIn.time().
         */
        int duration = t - checkIn.time();

        /*
         * Step 3: Identify the directed route.
         *
         * The origin comes from the customer's check-in, not from the
         * destination or from any global station state.
         */
        Route route = new Route(checkIn.station(), stationName);

        /*
         * Step 4: Find or create the route aggregate.
         *
         * computeIfAbsent creates RouteStats the first time this route occurs.
         * Every later journey for the same route updates the same aggregate.
         */
        RouteStats stats = statsByRoute.computeIfAbsent(
            route,
            ignored -> new RouteStats()
        );

        /*
         * Step 5: Incorporate this journey exactly once.
         *
         * totalDuration += duration
         * journeyCount  += 1
         *
         * We never need to retain this individual duration afterward.
         *
         * Time: O(1) average for the map operations and aggregate update.
         */
        stats.addJourney(duration);
    }

    public double getAverageTime(String startStation, String endStation) {
        /*
         * Constructing the key from the two requested stations retrieves
         * exactly the matching direction of travel.
         *
         * The problem guarantees at least one completed journey for the route,
         * so statsByRoute.get(route) will not return null.
         */
        Route route = new Route(startStation, endStation);
        RouteStats stats = statsByRoute.get(route);

        /*
         * The average is already represented by the running sum and count.
         * No loop, sorting, or historical journey retrieval is required.
         *
         * Time: O(1) average.
         */
        return stats.average();
    }

    /*
     * FINAL COMPLEXITY ANALYSIS
     *
     * Let:
     *   C = maximum/current number of active customers being stored.
     *   R = number of distinct routes that have completed journeys.
     *
     * checkIn:
     *   O(1) average time.
     *   O(1) additional space per newly active customer.
     *
     * checkOut:
     *   O(1) average time.
     *   O(1) additional space when a new route first appears.
     *
     * getAverageTime:
     *   O(1) average time because it performs a constant number of hash-map
     *   operations and reads two numeric fields.
     *
     * Total space:
     *   O(C + R).
     *
     * Why the HashMap complexity is described as average:
     *   HashMap lookups and updates are O(1) on average under normal hashing
     *   behavior; pathological collision scenarios can have higher costs.
     *
     * Why this is optimal:
     *   Each operation processes a constant amount of information, and an
     *   average query reads the already-maintained aggregate. We do not retain
     *   the completed journey history, so memory does not grow with every trip.
     */
}
