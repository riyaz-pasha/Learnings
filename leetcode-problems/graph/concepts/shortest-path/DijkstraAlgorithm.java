
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

class Dijkstras {

    public static final int INF = Integer.MAX_VALUE;

    public int[] shortestPaths2(int[][] graph, int sourceVertex) {
        int vertices = graph.length;
        int[] distance = new int[vertices];
        boolean[] visited = new boolean[vertices];
        PriorityQueue<Node> queue = new PriorityQueue<>();

        Arrays.fill(distance, INF);
        distance[sourceVertex] = 0;
        queue.offer(new Node(sourceVertex, 0));

        Node minDistanceNode;
        int currentVertex;
        while (!queue.isEmpty()) {
            minDistanceNode = queue.poll();
            currentVertex = minDistanceNode.vertex;
            if (visited[currentVertex])
                continue;
            visited[currentVertex] = true;
            for (int neighbor = 0; neighbor < vertices; neighbor++) {
                if (!visited[neighbor]
                        && graph[currentVertex][neighbor] != 0
                        && distance[currentVertex] + graph[currentVertex][neighbor] < distance[neighbor]) {
                    distance[neighbor] = distance[currentVertex] + graph[currentVertex][neighbor];
                    queue.offer(new Node(neighbor, distance[neighbor]));
                }
            }
        }
        return distance;
    }

    public int[] shortestPaths(int[][] graph, int sourceVertex) {
        int vertices = graph.length;
        boolean[] visited = new boolean[vertices];
        int[] distance = new int[vertices];
        Arrays.fill(distance, INF);
        distance[sourceVertex] = 0;
        for (int i = 0; i < vertices - 1; i++) {
            int minDistanceVertex = findMinDistanceVertex(distance, visited);
            visited[minDistanceVertex] = true;
            for (int vertex = 0; vertex < vertices; vertex++) {
                if (!visited[vertex]
                        && graph[minDistanceVertex][vertex] != 0
                        && distance[minDistanceVertex] != INF
                        && distance[minDistanceVertex] + graph[minDistanceVertex][vertex] < distance[vertex]) {
                    distance[vertex] = distance[minDistanceVertex] + graph[minDistanceVertex][vertex];
                }
            }
        }
        return distance;
    }

    private int findMinDistanceVertex(int[] distance, boolean[] visited) {
        int vertices = distance.length;
        int minDistanceVertex = -1;
        int minDistance = INF;
        for (int vertex = 0; vertex < vertices; vertex++) {
            if (distance[vertex] < minDistance && !visited[vertex]) {
                minDistance = distance[vertex];
                minDistanceVertex = vertex;
            }
        }
        return minDistanceVertex;
    }

    private class Node implements Comparable<Node> {
        int vertex;
        int distance;

        public Node(int vertex, int distance) {
            this.vertex = vertex;
            this.distance = distance;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.distance, other.distance);
        }

    }

}

/*
 * Dijkstra's Algorithm (Shortest Path from a Source)
 *
 * Works for:
 *  - Directed / Undirected graphs
 *  - Non-negative weights only (0 allowed, but NOT negative)
 *
 * Goal:
 *  dist[v] = shortest distance from source -> v
 *
 * Core Idea (Greedy):
 *  Always expand the node that currently has the smallest known distance.
 *
 * Important Interview Note:
 *  PriorityQueue can contain multiple entries for the same node.
 *  We skip outdated entries using:
 *      if (currDist > dist[node]) continue;
 *
 * Time Complexity:
 *  - Each edge relaxation can push into PQ => O(E log V)
 *  - Each PQ poll is log V => O(V log V)
 *  => Total: O((V + E) log V)
 *
 * Space Complexity:
 *  - dist[] = O(V)
 *  - adjacency list = O(V + E)
 *  - PQ can hold up to O(E) states in worst case
 *  => Total: O(V + E)
 */
class DijkstraAlgorithm1 {

    // Edge: u -> to (with weight)
    static class Edge {
        int to;
        int weight;

        Edge(int to, int weight) {
            this.to = to;
            this.weight = weight;
        }
    }

    // State used in PQ: (node, shortestDistanceSoFar)
    static class State {
        int node;
        int dist;

        State(int node, int dist) {
            this.node = node;
            this.dist = dist;
        }
    }

    public int[] dijkstra(int V, List<List<Edge>> graph, int source) {

        // dist[v] = shortest distance from source to v
        int[] dist = new int[V];
        Arrays.fill(dist, Integer.MAX_VALUE);

        // distance to source is always 0
        dist[source] = 0;

        // MinHeap: always gives node with smallest distance first
        PriorityQueue<State> pq =
                new PriorityQueue<>(Comparator.comparingInt(s -> s.dist));

        // start from source
        pq.offer(new State(source, 0));

        // Main loop
        while (!pq.isEmpty()) {

            State curr = pq.poll();
            int node = curr.node;
            int currDist = curr.dist;

            /*
             * IMPORTANT (most common interview confusion):
             *
             * PQ may contain outdated entries.
             *
             * Example:
             *   First we found dist[5] = 20, so we pushed (5,20)
             *   Later we found dist[5] = 10, so we pushed (5,10)
             *
             * PQ now contains both (5,20) and (5,10)
             * When (5,20) comes out, we must ignore it.
             */
            if (currDist > dist[node]) {
                continue;
            }

            // Relax all outgoing edges from this node
            for (Edge edge : graph.get(node)) {

                int neighbor = edge.to;
                int weight = edge.weight;

                /*
                 * Relaxation condition:
                 *
                 * If we can reach neighbor with a smaller cost,
                 * update dist[neighbor].
                 */
                if (dist[node] != Integer.MAX_VALUE &&
                        dist[node] + weight < dist[neighbor]) {

                    dist[neighbor] = dist[node] + weight;

                    // Push the updated best distance into PQ
                    pq.offer(new State(neighbor, dist[neighbor]));
                }
            }
        }

        return dist;
    }

}


/**
 * Dijkstra's Algorithm Implementation with Time Complexity Analysis
 * 
 * TIME COMPLEXITY: O((V + E) log V)
 * 
 * BREAKDOWN:
 * 1. Main while loop: Runs V times (each vertex extracted once)
 * - Each poll() operation: O(log V)
 * - Total for vertex extractions: O(V log V)
 * 
 * 2. Edge relaxation: Total of E edges processed across all iterations
 * - Each offer() operation: O(log V)
 * - Total for edge operations: O(E log V)
 * 
 * 3. Overall: O(V log V) + O(E log V) = O((V + E) log V)
 * 
 * SPACE COMPLEXITY: O(V) for distances array, visited array, and priority queue
 * 
 * WHY THIS COMPLEXITY?
 * - Binary heap (PriorityQueue) operations are O(log V)
 * - Each vertex is processed exactly once due to visited[] check
 * - Each edge is relaxed at most once
 * - Priority queue can contain at most V elements at any time
 */
public class DijkstraAlgorithm {

    static class Edge {
        int destination;
        int weight;

        Edge(int destination, int weight) {
            this.destination = destination;
            this.weight = weight;
        }
    }

    static class Node implements Comparable<Node> {
        int vertex;
        int distance;

        Node(int vertex, int distance) {
            this.vertex = vertex;
            this.distance = distance;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.distance, other.distance);
        }
    }

    public static int[] dijkstra(List<List<Edge>> graph, int source) {
        int V = graph.size();
        int[] distances = new int[V]; // O(V) space
        boolean[] visited = new boolean[V]; // O(V) space

        // Initialize distances to infinity - O(V) time
        Arrays.fill(distances, Integer.MAX_VALUE);
        distances[source] = 0;

        // Priority queue to store vertices with their distances - O(V) space worst case
        PriorityQueue<Node> pq = new PriorityQueue<>();
        pq.offer(new Node(source, 0)); // O(log V) - but initially log 1 = O(1)

        // MAIN LOOP: This runs exactly V times (each vertex extracted once)
        // Each iteration extracts one vertex and processes all its edges
        while (!pq.isEmpty()) {

            // VERTEX EXTRACTION: O(log V) operation
            // This happens exactly V times across the entire algorithm
            // Total contribution to complexity: V * O(log V) = O(V log V)
            Node current = pq.poll();
            int u = current.vertex;

            // Skip if already processed - ensures each vertex is processed only once
            // This is crucial for the V-time guarantee of the main loop
            if (visited[u])
                continue;
            visited[u] = true;

            // EDGE RELAXATION: Process all outgoing edges from vertex u
            // Across ALL iterations, each edge in the graph is processed exactly once
            // Total edges processed across entire algorithm: E
            for (Edge edge : graph.get(u)) {
                int v = edge.destination;
                int weight = edge.weight;

                // Relaxation step: Check if we found a shorter path
                if (!visited[v] && distances[u] + weight < distances[v]) {
                    distances[v] = distances[u] + weight;

                    // INSERT INTO PRIORITY QUEUE: O(log V) operation
                    // This happens at most once per edge across the entire algorithm
                    // Total contribution to complexity: E * O(log V) = O(E log V)
                    pq.offer(new Node(v, distances[v]));

                    // Note: We might insert the same vertex multiple times with different
                    // distances,
                    // but the visited[] check ensures we only process each vertex once
                }
            }
        }

        return distances;
    }

    /*
     * DETAILED COMPLEXITY ANALYSIS:
     * 
     * 1. INITIALIZATION: O(V)
     * - Arrays.fill(): O(V)
     * - Initial pq.offer(): O(1)
     * 
     * 2. MAIN LOOP ITERATIONS: Exactly V iterations
     * - Each vertex is extracted exactly once due to visited[] check
     * - Some iterations might be skipped due to visited[] check, but total
     * extractions = V
     * 
     * 3. VERTEX EXTRACTIONS: V times, each O(log V)
     * - pq.poll(): O(log V)
     * - Total: O(V log V)
     * 
     * 4. EDGE RELAXATIONS: E times total across all iterations
     * - Each edge is considered exactly once when its source vertex is processed
     * - For each relaxation, we might do pq.offer(): O(log V)
     * - Total: O(E log V)
     * 
     * 5. FINAL COMPLEXITY: O(V) + O(V log V) + O(E log V) = O((V + E) log V)
     * 
     * COMPARISON WITH OTHER IMPLEMENTATIONS:
     * - Array-based (without priority queue): O(V²) - better for dense graphs
     * - Fibonacci heap: O(E + V log V) - better theoretical bound
     * - Binary heap (this implementation): O((V + E) log V) - practical and
     * efficient
     */

    public static void main(String[] args) {
        // Example usage
        int V = 5;
        List<List<Edge>> graph = new ArrayList<>();

        // Initialize adjacency list
        for (int i = 0; i < V; i++) {
            graph.add(new ArrayList<>());
        }

        // Add edges (u -> v with weight w)
        graph.get(0).add(new Edge(1, 10));
        graph.get(0).add(new Edge(4, 5));
        graph.get(1).add(new Edge(2, 1));
        graph.get(1).add(new Edge(4, 2));
        graph.get(2).add(new Edge(3, 4));
        graph.get(3).add(new Edge(2, 6));
        graph.get(3).add(new Edge(0, 7));
        graph.get(4).add(new Edge(1, 3));
        graph.get(4).add(new Edge(2, 9));
        graph.get(4).add(new Edge(3, 2));

        int[] distances = dijkstra(graph, 0);

        System.out.println("Shortest distances from vertex 0:");
        for (int i = 0; i < distances.length; i++) {
            System.out.println("To vertex " + i + ": " + distances[i]);
        }
    }

}

/*
 * Dijkstra's Algorithm - Step-by-Step Explanation
 *
 * Step 1: Initialize
 * - Set distance[] to ∞ (Integer.MAX_VALUE) for all vertices.
 * - Set distance[source] = 0.
 * - Set visited[] to false for all vertices.
 * - Use a PriorityQueue (min-heap) to always process the vertex with the
 * smallest distance.
 *
 * Step 2: Add the source vertex to the priority queue with distance 0.
 *
 * Step 3: Loop until the priority queue is empty:
 * - Extract the vertex 'u' with the minimum distance from the queue.
 * - If 'u' is already visited, skip it (continue).
 * - Mark 'u' as visited.
 *
 * Step 4: For each unvisited neighbor 'v' of 'u':
 * - If distance[u] + weight(u, v) < distance[v], then update:
 * distance[v] = distance[u] + weight(u, v)
 * - Add 'v' to the priority queue with the new distance.
 *
 * Step 5: After the loop ends, the distance[] array contains
 * the shortest distances from the source vertex to all other vertices.
 */
