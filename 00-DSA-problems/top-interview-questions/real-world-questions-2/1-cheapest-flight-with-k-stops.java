
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

// Cheapest flight with k stops
// flights [from,to,fare]
// n cities, [0,n-1]
// what to return incase of no flights found?
// if such route exists what to return entire chain or just price and stops count?
// costs can be negative?

class CheapestFlightWithKStops {

    record State(int destination, int totalFare, int stopsTaken) {
    }

    record To(int destination, int fare) {
    }

    public int findCheapestFlight(int n, List<List<Integer>> flights, int k, int start, int end) {
        // 1. handle base cases
        if (n == 0 || flights == null || flights.isEmpty() || k == 0) {
            return -1;
        }

        // 2. Build Graph
        List<List<To>> adjList = this.buildGraph(n, flights);

        // 3. Create minHeap to maintain price with shortest fare
        PriorityQueue<State> minHeap = new PriorityQueue<>(Comparator.comparingInt(State::totalFare));

        // 4. Push start into the minHeap
        minHeap.offer(new State(start, 0, 0));

        // 5. Iterate all nodes and find cheapest flight
        while (!minHeap.isEmpty()) {
            State current = minHeap.poll();
            if (current.stopsTaken() >= k) {
                return -1;
            }
            if (current.destination == end) {
                return current.totalFare();
            }

            for (To neighbor : adjList.get(current.destination())) {
                minHeap.offer(new State(neighbor.destination(), current.totalFare() + neighbor.fare(),
                        current.stopsTaken() + 1));
            }
        }

        return -1;
        // TC O((V+E)logE) 
        // V - nodes in the graph . cities in our case
        // E - edges in graph. flights in our case
        // minHeap takes logE time of insertion
    }

    private List<List<To>> buildGraph(int n, List<List<Integer>> flights) {
        List<List<To>> adjList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjList.add(new ArrayList<>());
        }
        for (List<Integer> flight : flights) {
            Integer from = flight.get(0);
            Integer to = flight.get(1);
            Integer fare = flight.get(1);
            adjList.get(from).add(new To(to, fare));
        }

        return adjList;
    }
}
