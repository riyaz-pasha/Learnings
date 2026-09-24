import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

class CheapestFlightWithCoupon {

    record To(int destination, int fare) {
    }

    // Added a Comparator to the PriorityQueue via the constructor
    record State(int destination, int totalFare, int stopsUsed, boolean isCouponUsed) {
    }

    public int flightWithCoupon(int n, List<List<Integer>> flights, int k, int src, int dst) {
        List<List<To>> adjList = this.buildGraph(n, flights);

        // Sort by totalFare ascending
        PriorityQueue<State> minHeap = new PriorityQueue<>(Comparator.comparingInt(State::totalFare));

        // Track the cheapest cost to reach a node with (isCouponUsed) status
        // costs[node][0] = price without coupon, costs[node][1] = price with coupon
        int[][] minCosts = new int[n][2];
        for (int[] row : minCosts)
            Arrays.fill(row, Integer.MAX_VALUE);

        minHeap.offer(new State(src, 0, 0, false));

        while (!minHeap.isEmpty()) {
            State current = minHeap.poll();

            if (current.destination() == dst)
                return current.totalFare();

            // If we've already found a cheaper way to this node with the same coupon status
            // and fewer or equal stops, we could prune. For simplicity in K-stops,
            // we check if this fare is worse than what we've seen.
            int couponIdx = current.isCouponUsed() ? 1 : 0;
            if (current.totalFare() > minCosts[current.destination()][couponIdx] && current.stopsUsed() > k) {
                continue;
            }

            // If we've exceeded allowed intermediate stops, stop exploring this path
            if (current.stopsUsed() > k)
                continue;

            for (To neighbor : adjList.get(current.destination())) {
                // Option 1: Use coupon now (if not already used)
                if (!current.isCouponUsed()) {
                    minHeap.offer(
                            new State(neighbor.destination(), current.totalFare(), current.stopsUsed() + 1, true));
                }

                // Option 2: Don't use coupon now (keep current status)
                int nextFare = current.totalFare() + neighbor.fare();
                minHeap.offer(
                        new State(neighbor.destination(), nextFare, current.stopsUsed() + 1, current.isCouponUsed()));
            }
        }

        return -1;
    }

    private List<List<To>> buildGraph(int n, List<List<Integer>> flights) {
        List<List<To>> adjList = new ArrayList<>();
        for (int i = 0; i < n; i++)
            adjList.add(new ArrayList<>());
        for (List<Integer> flight : flights) {
            adjList.get(flight.get(0)).add(new To(flight.get(1), flight.get(2)));
        }
        return adjList;
    }
}

/*
 * Time
 * States = n * k * 2
 * Edges = flights.size()
 * 
 * Dijkstra: O((n * k) log (n * k))
 * 
 * Space
 * O(n * k * 2) for distance table
 * O(n * k) for priority queue
 * 
 */



/**
 * Cheapest Flight With One Coupon and At Most K Stops
 *
 * State = (city, stopsUsed, couponUsed)
 *
 * We run Dijkstra on an expanded state space because:
 * - Using the coupon changes future costs
 * - Stop count affects reachability
 */
class CheapestFlightWithCoupon3 {

    /**
     * Directed edge in flight graph
     * destination -> next city
     * fare        -> ticket cost
     */
    record To(int destination, int fare) {}

    /**
     * State used in priority queue
     * destination  -> current city
     * totalFare   -> total cost so far
     * stopsUsed   -> number of flights taken so far
     * couponUsed  -> whether coupon has been used
     */
    record State(int destination, int totalFare, int stopsUsed, boolean couponUsed) {}

    /**
     * Time Complexity:
     *   States = n * (k + 1) * 2
     *   Each state processed once in Dijkstra
     *
     *   O((n * k) log (n * k) + flights * k)
     *
     * Space Complexity:
     *   O(n * k * 2)
     */
    public int flightWithCoupon(
            int n,
            List<List<Integer>> flights,
            int k,
            int src,
            int dst
    ) {
        List<List<To>> graph = buildGraph(n, flights);

        // dist[city][stops][couponUsed]
        int[][][] dist = new int[n][k + 1][2];
        for (int i = 0; i < n; i++) {
            for (int s = 0; s <= k; s++) {
                Arrays.fill(dist[i][s], Integer.MAX_VALUE);
            }
        }

        PriorityQueue<State> pq =
                new PriorityQueue<>(Comparator.comparingInt(State::totalFare));

        // Start from source with 0 cost, 0 stops, coupon unused
        pq.offer(new State(src, 0, 0, false));
        dist[src][0][0] = 0;

        while (!pq.isEmpty()) {
            State curr = pq.poll();

            int city = curr.destination();
            int cost = curr.totalFare();
            int stops = curr.stopsUsed();
            int couponIdx = curr.couponUsed() ? 1 : 0;

            // If destination is reached, this is the minimum cost
            if (city == dst) {
                return cost;
            }

            // Stop constraint
            if (stops == k) continue;

            // Skip outdated (dominated) states
            if (cost > dist[city][stops][couponIdx]) continue;

            for (To edge : graph.get(city)) {
                int nextCity = edge.destination();
                int nextStops = stops + 1;

                // Option 1: Fly normally (pay fare)
                int normalCost = cost + edge.fare();
                if (normalCost < dist[nextCity][nextStops][couponIdx]) {
                    dist[nextCity][nextStops][couponIdx] = normalCost;
                    pq.offer(new State(nextCity, normalCost, nextStops, curr.couponUsed()));
                }

                // Option 2: Use coupon (only once)
                if (!curr.couponUsed()) {
                    if (cost < dist[nextCity][nextStops][1]) {
                        dist[nextCity][nextStops][1] = cost;
                        pq.offer(new State(nextCity, cost, nextStops, true));
                    }
                }
            }
        }

        return -1;
    }

    /**
     * Builds adjacency list from flight list
     */
    private List<List<To>> buildGraph(int n, List<List<Integer>> flights) {
        List<List<To>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());

        for (List<Integer> f : flights) {
            graph.get(f.get(0)).add(new To(f.get(1), f.get(2)));
        }
        return graph;
    }
}



class CheapestFlightWithCoupon2 {

    // Edge
    record To(int destination, int fare) {}

    // Dijkstra state
    record State(int destination, int totalFare, int stopsUsed, boolean couponUsed) {}

    public int flightWithCoupon(int n, List<List<Integer>> flights, int k, int src, int dst) {
        if (n <= 0 || flights == null || flights.isEmpty()) {
            return -1;
        }

        List<List<To>> graph = buildGraph(n, flights);

        // dist[node][stops][couponUsed]
        int INF = Integer.MAX_VALUE;
        int[][][] dist = new int[n][k + 1][2];

        for (int i = 0; i < n; i++) {
            for (int s = 0; s <= k; s++) {
                Arrays.fill(dist[i][s], INF);
            }
        }

        PriorityQueue<State> pq = new PriorityQueue<>(
                Comparator.comparingInt(State::totalFare)
        );

        pq.offer(new State(src, 0, 0, false));
        dist[src][0][0] = 0;

        while (!pq.isEmpty()) {
            State cur = pq.poll();

            int u = cur.destination();
            int cost = cur.totalFare();
            int stops = cur.stopsUsed();
            int used = cur.couponUsed() ? 1 : 0;

            if (u == dst) {
                return cost;
            }

            if (stops == k) continue;

            for (To edge : graph.get(u)) {
                int v = edge.destination();
                int fare = edge.fare();

                // Case 1: pay normally
                int newCost = cost + fare;
                if (newCost < dist[v][stops + 1][used]) {
                    dist[v][stops + 1][used] = newCost;
                    pq.offer(new State(v, newCost, stops + 1, cur.couponUsed()));
                }

                // Case 2: use coupon (only once)
                if (!cur.couponUsed()) {
                    if (cost < dist[v][stops + 1][1]) {
                        dist[v][stops + 1][1] = cost;
                        pq.offer(new State(v, cost, stops + 1, true));
                    }
                }
            }
        }

        return -1;
    }

    private List<List<To>> buildGraph(int n, List<List<Integer>> flights) {
        List<List<To>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());

        for (List<Integer> f : flights) {
            int from = f.get(0);
            int to = f.get(1);
            int fare = f.get(2);
            graph.get(from).add(new To(to, fare));
        }
        return graph;
    }
}



class CheapestPathWithOneCoupon {

    // Edge: from current city to destination city with cost
    record Edge(int to, int cost) {}

    // State for Dijkstra
    // city        -> current city
    // couponUsed  -> whether the coupon has already been used
    // totalCost  -> total cost so far
    record State(int city, int couponUsed, int totalCost) {}

    public int cheapestPath(
            int n,
            List<int[]> flights, // [from, to, cost]
            int src,
            int dst
    ) {
        // Build graph
        List<List<Edge>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());

        for (int[] f : flights) {
            graph.get(f[0]).add(new Edge(f[1], f[2]));
        }

        // dist[city][0] -> min cost to reach city without using coupon
        // dist[city][1] -> min cost to reach city after using coupon
        int[][] dist = new int[n][2];
        for (int i = 0; i < n; i++) {
            Arrays.fill(dist[i], Integer.MAX_VALUE);
        }

        // Min-heap ordered by total cost
        PriorityQueue<State> pq =
                new PriorityQueue<>(Comparator.comparingInt(State::totalCost));

        // Start at src with coupon unused
        pq.offer(new State(src, 0, 0));
        dist[src][0] = 0;

        while (!pq.isEmpty()) {
            State cur = pq.poll();

            int city = cur.city();
            int used = cur.couponUsed();
            int cost = cur.totalCost();

            // If destination reached, this is the minimum cost
            if (city == dst) {
                return cost;
            }

            // Skip outdated state
            if (cost > dist[city][used]) continue;

            for (Edge edge : graph.get(city)) {
                int next = edge.to();
                int edgeCost = edge.cost();

                // Option 1: Do NOT use coupon
                int newCost = cost + edgeCost;
                if (newCost < dist[next][used]) {
                    dist[next][used] = newCost;
                    pq.offer(new State(next, used, newCost));
                }

                // Option 2: Use coupon (only if not used yet)
                if (used == 0) {
                    if (cost < dist[next][1]) {
                        dist[next][1] = cost;
                        pq.offer(new State(next, 1, cost));
                    }
                }
            }
        }

        return -1;
    }
    /*
     * Time:O((V+E)log(2V))≈O((V+E)logV)
     * space O(2V)=O(V)
     */
}
