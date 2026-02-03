
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Shortest Path With Fuel Constraint
 *
 * We use Dijkstra on an expanded state space:
 * State = (city, fuelInTank)
 *
 * Each state transition represents either:
 * 1) Buying 1 unit of fuel at the current city
 * 2) Traveling to a neighboring city if enough fuel is available
 *
 * This is necessary because reaching the same city with different
 * fuel levels leads to different future possibilities.
 */
class ShortestPathWithFuel {

    /**
     * Edge in the road network
     * to   -> destination city
     * dist -> fuel required to travel this edge
     */
    record Edge(int to, int dist) {}

    /**
     * State used in Dijkstra
     * city -> current city
     * fuel -> fuel left in tank
     * cost -> total money spent so far
     */
    record State(int city, int fuel, int cost) {}

    /**
     * Finds the minimum cost to travel from src to dst
     *
     * Time Complexity:
     *   - Number of states = n * (capacity + 1)
     *   - Each state is processed once in Dijkstra
     *   - Each push/pop costs log(n * capacity)
     *
     *   => O((n * capacity + edges * capacity) * log(n * capacity))
     *
     * Space Complexity:
     *   - Distance table: O(n * capacity)
     *   - Priority queue: O(n * capacity)
     */
    public int minCost(
            int n,
            Map<Integer, List<Edge>> adjList,
            int[] fuelPrice,
            int capacity,
            int src,
            int dst
    ) {
        /*
         * Being at the same city with different fuel amounts leads to different future
         * possibilities and costs, so treating them as the same state loses optimal
         * paths.
         */

        /*
         * Because reaching the same city with different remaining fuel can lead to
         * different future costs, (city, fuel) represents fundamentally different
         * states, whereas (city) alone loses optimal solutions.
         */
        // minCost[city][fuel] = minimum cost to reach `city` with `fuel` left
        int[][] minCost = new int[n][capacity + 1];
        for (int[] row : minCost) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        // Min-heap ordered by total cost so far (Dijkstra)
        PriorityQueue<State> pq =
                new PriorityQueue<>(Comparator.comparingInt(State::cost));

        // Start at source with 0 fuel and 0 cost
        pq.offer(new State(src, 0, 0));
        minCost[src][0] = 0;

        while (!pq.isEmpty()) {
            State curr = pq.poll();

            int city = curr.city();
            int fuel = curr.fuel();
            int cost = curr.cost();

            // If destination is reached, this is the minimum cost
            // (Dijkstra guarantees optimality)
            // “This is safe because Dijkstra guarantees the first time we pop dst from the
            // queue, it has the minimum possible cost across all (city, fuel) states.”
            if (city == dst) {
                return cost;
            }

            // Skip outdated (non-optimal) states
            if (cost > minCost[city][fuel]) {
                continue;
            }

            // -------------------------------
            // OPTION 1: Buy 1 unit of fuel
            // -------------------------------
            // We stay in the same city, increase fuel by 1,
            // and pay fuelPrice[city]
            if (fuel < capacity) {
                int nextCost = cost + fuelPrice[city];
                if (nextCost < minCost[city][fuel + 1]) {
                    minCost[city][fuel + 1] = nextCost;
                    pq.offer(new State(city, fuel + 1, nextCost));
                }
            }

            // ---------------------------------
            // OPTION 2: Travel to neighboring city
            // ---------------------------------
            // We can travel only if we have enough fuel
            if (adjList.containsKey(city)) {
                for (Edge edge : adjList.get(city)) {
                    int fuelRequired = edge.dist();

                    if (fuel >= fuelRequired) {
                        int nextCity = edge.to();
                        int remainingFuel = fuel - fuelRequired;

                        // Traveling does NOT cost money, only fuel
                        if (cost < minCost[nextCity][remainingFuel]) {
                            minCost[nextCity][remainingFuel] = cost;
                            pq.offer(new State(nextCity, remainingFuel, cost));
                        }
                    }
                }
            }
        }

        // Destination is unreachable
        return -1;
    }
}


class ShortestPathFuel {
    record State(int node, int fuel, int time) {}

    public int minTimeToReach(int n, int[][] roads, int start, int target, int maxCap, int[] fuelStations) {
        // 1. Build Graph: Map<Integer, List<int[]>> {neighbor, distance}
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] r : roads) {
            adj.get(r[0]).add(new int[]{r[1], r[2]});
            adj.get(r[1]).add(new int[]{r[0], r[2]});
        }

        // 2. Priority Queue: Sort by time (min-heap)
        PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparingInt(State::time));
        
        // 3. DP Table: minTime[node][fuel_remaining]
        int[][] minTime = new int[n][maxCap + 1];
        for (int[] row : minTime) Arrays.fill(row, Integer.MAX_VALUE);

        pq.offer(new State(start, maxCap, 0));
        minTime[start][maxCap] = 0;

        while (!pq.isEmpty()) {
            State curr = pq.poll();

            if (curr.node == target) return curr.time;
            if (curr.time > minTime[curr.node][curr.fuel]) continue;

            // OPTION A: Refuel at current node (if it has a station)
            // Some variants let you refuel 1 unit per 1 unit of time
            if (fuelStations[curr.node] > 0 && curr.fuel < maxCap) {
                int newFuel = maxCap; // Assuming instant full refuel for this example
                int waitTime = 10; // Example: refueling takes 10 mins
                if (curr.time + waitTime < minTime[curr.node][newFuel]) {
                    minTime[curr.node][newFuel] = curr.time + waitTime;
                    pq.offer(new State(curr.node, newFuel, minTime[curr.node][newFuel]));
                }
            }

            // OPTION B: Drive to a neighbor
            for (int[] edge : adj.get(curr.node)) {
                int nextNode = edge[0];
                int dist = edge[1];
                
                if (curr.fuel >= dist) {
                    int remainingFuel = curr.fuel - dist;
                    int nextTime = curr.time + dist; // 1 unit distance = 1 unit time
                    
                    if (nextTime < minTime[nextNode][remainingFuel]) {
                        minTime[nextNode][remainingFuel] = nextTime;
                        pq.offer(new State(nextNode, remainingFuel, nextTime));
                    }
                }
            }
        }
        return -1;
    }
    /*
     * Complexity AnalysisNodes in State Space: $V \times C$ (where $V$ is number of
     * cities and $C$ is max fuel capacity).Edges in State Space: Each state has
     * edges to neighbors + a "refuel" edge.Time Complexity: $O(E \cdot C \log(V
     * \cdot C))$ — This is why these problems usually have small fuel capacities
     * (e.g., $C \le 100$).
     */
}
