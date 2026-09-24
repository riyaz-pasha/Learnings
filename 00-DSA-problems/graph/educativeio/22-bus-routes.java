/**
 * ============================================================================
 * BUS ROUTES - STUDY NOTES
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ------------------------------------------------
 * Q: Is the cost based on the number of stations visited, or strictly the number of buses boarded?
 *    -> Why: This is the most crucial question. It completely changes the graph weights (1 cost per bus vs 1 cost per station).
 * Q: Can the source (src) and destination (dest) be the exact same station?
 *    -> Why: Confirms if we need a base case to instantly return 0 buses, bypassing any search.
 * Q: Are the bus routes bidirectional? Can I travel both forwards and backwards on the array?
 *    -> Why: Ensures we don't have to simulate directed circular queues or strictly forward traversal. (Rule: Yes, bidirectional).
 * Q: Can a bus route contain duplicate stations, or loop back on itself?
 *    -> Why: Dictates whether we should deduplicate a route's stations into a Set before processing to save time.
 * Q: What should be returned if the src or dest station doesn't exist in any route?
 *    -> Why: Establishes early exit conditions to prevent building complex graphs for impossible queries.
 *
 *
 * 2. THE REASONING JOURNEY
 * ------------------------------------------------
 * The Core Challenge: We need the shortest path in a network, which strongly suggests Breadth-First Search (BFS). 
 * However, the binding constraint is that the "cost" is incurred ONLY when we switch to a new bus, NOT when we 
 * move between stations. Standard BFS measures distance by edges traversed; if we treat stations as nodes and 
 * adjacent stations as edges, standard BFS will find the path with the fewest stations, not the fewest buses.
 *
 * Approach 1: Station-to-Station 0-1 BFS / Dijkstra (Brute Force)
 * - What I'd naturally try: Treat every station as a node. Build an adjacency list where an edge exists between 
 *   ANY two stations that share the same bus route. Since they share a bus, the cost to travel between them is 1. 
 *   If they share multiple buses, we have to carefully track state.
 * - Why it works: It correctly identifies that traveling anywhere on the same route costs exactly 1 bus trip.
 * - Why it's too slow: In the worst case, a single bus route has 100 stations. Connecting every station to every 
 *   other station on that route creates 100 * 99 = 9,900 edges for JUST ONE route. With 50 routes, this graph 
 *   explodes in size, leading to massive memory overhead and slow traversal times.
 * - Time Complexity: O(R * S^2) — because for each of the R routes, we create an edge between every pair of its S stations.
 * - Space Complexity: O(R * S^2) — because we store all of these dense connections in our adjacency list.
 *
 * Approach 2: Station-to-Bus Bipartite BFS (The Optimum)
 * - What work is being repeated: In Approach 1, checking connection between Station A and Station Z is redundant 
 *   if we simply know they both belong to Bus 1.
 * - What property removes the bottleneck: The cost of 1 is tied to the BUS, not the station. We can change our graph 
 *   structure so that Stations only connect to Buses, and Buses only connect to Stations (a Bipartite Graph). 
 * - How that observation leads to the next approach: We map `Station -> List of Buses`. We queue stations. When we 
 *   pop a station, we "board" every available bus at that station. For each bus boarded, we check all of its stations. 
 *   By tracking which *buses* we have already boarded, we guarantee we never evaluate the same route twice.
 * - Time Complexity: O(R * S) — because we process each of the R buses exactly ONCE. When a bus is processed, we 
 *   iterate over its S stations. The hash map building also takes O(R * S).
 * - Space Complexity: O(R * S) — because the mapping stores every station-to-bus relationship, and the Queue / Visited 
 *   Sets will hold at most all stations and all buses. This is incredibly lean compared to the dense O(R * S^2) graph.
 * - Decision: Approach 2 is exactly what I'd write in an interview. It's an elegant reframing of the graph that 
 *   drastically reduces time complexity and demonstrates strong data-modeling skills.
 *
 *
 * 3. EDGE CASES
 * ------------------------------------------------
 * - src == dest: The user is already at the destination. We must return 0 immediately before checking routes.
 * - src or dest not in network: If either station isn't mapped to any bus, it's unreachable. Handled gracefully if 
 *   our map returns null/empty.
 * - Disconnected network: A graph with two distinct clusters of buses that never share a transfer station. 
 *   The BFS will naturally exhaust its queue and return -1.
 * - Circular / Repeated Routes: e.g., Route = [1, 2, 3, 1]. Processing is handled safely by the `visitedStations` set.
 *
 *
 * 4. ADDITIONAL INSIGHTS
 * ------------------------------------------------
 * Key Insight: 
 * "What exactly incurs a cost?" In this problem, traversing 100 stations on one bus costs the same as traversing 
 * 2 stations on one bus. Therefore, nodes should represent the things that cost money: the Buses! Stations are 
 * merely the "edges" that allow us to jump between Bus nodes.
 *
 * ASCII Diagram (Bipartite Visualization):
 * Instead of routing Station 1 directly to Station 6, we route through the "Bus" hubs.
 * 
 *   (Station 1)        (Station 2)
 *        \                 /
 *         \               /
 *          ----[BUS 0]----  
 *                 |
 *                 | (Transfer Station 7)
 *                 |
 *          ----[BUS 1]----
 *         /               \
 *        /                 \
 *   (Station 3)        (Station 6)
 *
 * Dry Run: 
 * routes = [[1, 2, 7], [3, 6, 7]], src = 1, dest = 6
 * Initial state: map={1:[0], 2:[0], 7:[0,1], 3:[1], 6:[1]}. queue=[1]. visitedBuses=[]. busesTaken=0.
 * Level 1:
 *   - Pop Station 1. 
 *   - Get buses for 1 -> [Bus 0].
 *   - Board Bus 0 (mark visited). Add its stations to queue -> [2, 7].
 * Level 2:
 *   - Pop Station 2. 
 *   - Get buses for 2 -> [Bus 0]. Already visited. Skip.
 *   - Pop Station 7.
 *   - Get buses for 7 -> [Bus 0, Bus 1]. Bus 0 visited. Board Bus 1 (mark visited).
 *   - Add its stations -> [3, 6]. 
 *   - Wait, we see Station 6 matches `dest`! Return busesTaken + 1 = 2.
 *
 * Pitfalls:
 * - Tracking visited STATIONS but forgetting to track visited BUSES. If you don't mark the bus as visited, 
 *   you will iterate over its 100 stations every single time someone transfers at one of its stops, resulting in TLE.
 * - Building a full N x N adjacency matrix. With 10^6 max station ID, a matrix is an instant Memory Limit Exceeded (MLE).
 * 
 * Pattern Recognition: 
 * - When you see: "Minimize transfers", "Group memberships", or "Changing lines/routes".
 * - Think: Bipartite Graph BFS or "Group as Nodes" BFS.
 * - Transfers to: Word Ladder (where wildcard patterns like "h*t" act as the buses connecting "hot", "hat", "hit").
 * 
 * Interview Script:
 * "If we build a standard graph of stations, connecting all stations on a single route creates O(N^2) edges per route, 
 * which is too dense. Since the cost is strictly tied to changing buses, I'll model this as a bipartite graph. 
 * I'll map each station to a list of its Bus IDs. During BFS, I'll queue the stations, but the crucial optimization 
 * is tracking which BUSES I've already evaluated. When I process a station, I 'board' all its unvisited buses, mark 
 * them as visited, and queue all their stations. This guarantees each bus is processed exactly once, giving us an 
 * optimal O(R*S) time and space complexity."
 *
 *
 * 5. FOLLOW-UPS
 * ------------------------------------------------
 * F1: What if we want to minimize total *stops*, but use minimum buses as a tie-breaker?
 *  -> The unweighted BFS breaks. We would need Dijkstra's algorithm. The state for each station would be a tuple 
 *     `(buses_taken, stops_taken)`, and our PriorityQueue would sort primarily by stops, then by buses.
 * 
 * F2: What if the bus routes update dynamically throughout the day (e.g., a route closes)?
 *  -> A static BFS won't handle updates well. We would likely need to maintain the bipartite graph and re-run BFS 
 *     if queries are sparse. If queries are frequent and routes just drop offline, a fully dynamic graph connectivity 
 *     data structure might be needed, or we just dynamically check a `isBusActive(busId)` boolean during our BFS.
 */

import java.util.*;

public class BusRoutes {

    /**
     * OPTIMAL APPROACH: Station-to-Bus Bipartite BFS
     * Time Complexity: O(R * S) — R is number of routes, S is max stations per route. We process each bus exactly once.
     * Space Complexity: O(R * S) — For the stationToBuses map and the BFS queue.
     */
    public int numBusesToDestination(int[][] routes, int src, int dest) {
        // Edge Case 1: Already at the destination. Costs 0 buses.
        if (src == dest) {
            return 0;
        }

        // Map each station to a list of bus route indices that stop there.
        // e.g., if routes = [[1, 2, 7], [3, 6, 7]], then stationToBuses.get(7) -> [0, 1]
        Map<Integer, List<Integer>> stationToBuses = new HashMap<>();
        
        for (int busId = 0; busId < routes.length; busId++) {
            for (int station : routes[busId]) {
                stationToBuses.computeIfAbsent(station, k -> new ArrayList<>()).add(busId);
            }
        }

        // Edge Case 2: Source or Destination isn't in any route at all.
        if (!stationToBuses.containsKey(src) || !stationToBuses.containsKey(dest)) {
            return -1;
        }

        // BFS Queue to store the stations we are currently visiting.
        // Starts with the source station.
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(src);

        // visitedBuses prevents us from iterating over the same bus route twice.
        // e.g., visitedBuses[0] = true means we've already boarded Bus 0 and added its stops to the queue.
        boolean[] visitedBuses = new boolean[routes.length];
        
        // visitedStations prevents us from re-evaluating stations we've already reached.
        Set<Integer> visitedStations = new HashSet<>();
        visitedStations.add(src);

        int busesTaken = 0;

        // Standard level-by-level BFS traversal
        while (!queue.isEmpty()) {
            // Every time we process a new level, it implies we had to transfer to a new set of buses.
            busesTaken++;
            
            int levelSize = queue.size();
            
            for (int i = 0; i < levelSize; i++) {
                int currentStation = queue.poll();
                
                // Get all buses that stop at this current station
                for (int busId : stationToBuses.get(currentStation)) {
                    
                    // If we've already ridden this bus, skip it to save O(S) work!
                    if (visitedBuses[busId]) {
                        continue;
                    }
                    
                    // Mark the bus as boarded
                    visitedBuses[busId] = true;
                    
                    // Add every station on this bus route to the BFS queue for the next level
                    for (int nextStation : routes[busId]) {
                        
                        // If we find the destination, we can exit immediately!
                        if (nextStation == dest) {
                            return busesTaken;
                        }
                        
                        // Only add the station to the queue if it's our first time seeing it
                        if (visitedStations.add(nextStation)) {
                            queue.offer(nextStation);
                        }
                    }
                }
            }
        }

        // If the queue exhausts and we never hit 'dest', it's unreachable.
        return -1;
    }

    // ============================================================================
    // MAIN METHOD - TEST CASES
    // ============================================================================
    public static void main(String[] args) {
        BusRoutes solver = new BusRoutes();
        
        System.out.println("--- Testing Bus Routes ---");

        // Test Case 1: Standard intersection (From ASCII Diagram)
        // Bus 0: 1->2->7, Bus 1: 3->6->7. Transfer at 7.
        int[][] routes1 = {{1, 2, 7}, {3, 6, 7}};
        int src1 = 1, dest1 = 6;
        System.out.println("Test Case 1 (Expected 2): " + solver.numBusesToDestination(routes1, src1, dest1));

        // Test Case 2: Source equals Destination
        int[][] routes2 = {{1, 2, 7}, {3, 6, 7}};
        int src2 = 7, dest2 = 7;
        System.out.println("Test Case 2 (Expected 0): " + solver.numBusesToDestination(routes2, src2, dest2));

        // Test Case 3: Disconnected graph / Unreachable
        // Bus 0: 1->2, Bus 1: 3->4. No shared stations.
        int[][] routes3 = {{1, 2}, {3, 4}};
        int src3 = 1, dest3 = 4;
        System.out.println("Test Case 3 (Expected -1): " + solver.numBusesToDestination(routes3, src3, dest3));

        // Test Case 4: Reaching target on the very first bus
        // Bus 0 goes directly from 1 to 5.
        int[][] routes4 = {{1, 5, 9}, {5, 6, 7}};
        int src4 = 1, dest4 = 5;
        System.out.println("Test Case 4 (Expected 1): " + solver.numBusesToDestination(routes4, src4, dest4));

        // Test Case 5: Missing source/dest in network
        int[][] routes5 = {{1, 2}, {3, 4}};
        int src5 = 1, dest5 = 99; // 99 doesn't exist
        System.out.println("Test Case 5 (Expected -1): " + solver.numBusesToDestination(routes5, src5, dest5));
    }
}

