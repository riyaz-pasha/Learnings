
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/*
 * Given are N cities and M roads that travel between the given pair of cities
 * and time it takes to travel that road. Also we are given a list of favourite
 * cities L and a source city S . we have to tell the favourite city which can
 * be reached from source city the fastest(in minimum time)
 */

public class ShortestFavouriteCity {

    class Pair {
        int city, time;

        public Pair(int city, int time) {
            this.city = city;
            this.time = time;
        }

    }

    public int fastestFavouriteCity(int n, int[][] roads, int[] favourites, int source) {
        List<List<Pair>> graph = this.buildAdjMatrix(roads, n);

        // for faster looksups
        Set<Integer> favoritesSet = this.buildFavouritesSet(favourites);

        PriorityQueue<Pair> pq = new PriorityQueue<>(Comparator.comparingInt(p -> p.time));
        boolean[] visited = new boolean[n];

        pq.offer(new Pair(source, 0));
        visited[source] = true;

        while (!pq.isEmpty()) {
            Pair current = pq.poll();

            if (visited[current.city]) {
                continue;
            }
            visited[current.city] = true;

            if (favoritesSet.contains(current.city)) {
                return current.city;
            }

            for (Pair neighbor : graph.get(current.city)) {
                if (!visited[neighbor.city]) {
                    pq.offer(new Pair(neighbor.city, neighbor.time + current.time));
                }
            }
        }

        return -1;
    }

    private List<List<Pair>> buildAdjMatrix(int[][] roads, int n) {
        List<List<Pair>> adjList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            adjList.add(new ArrayList<>());
        }

        for (int[] road : roads) {
            int u = road[0], v = road[1], t = road[2];
            adjList.get(u).add(new Pair(v, t));
            adjList.get(v).add(new Pair(u, t));
        }

        return adjList;
    }

    private Set<Integer> buildFavouritesSet(int[] favourites) {
        Set<Integer> favoritesSet = new HashSet<>();

        for (int fav : favourites) {
            favoritesSet.add(fav);
        }
        return favoritesSet;
        // return new HashSet<Integer>(Arrays.asList(favourites));
    }

}

/*
 * Time Complexity Explanation of Dijkstra's Algorithm using a Binary Heap:
 *
 * Given:
 * N = number of nodes (cities)
 * M = number of edges (roads)
 *
 * Goal:
 * Find the shortest distances from a source node to all other nodes.
 *
 * Steps:
 * 1. Initialization:
 * - Distances to all nodes are set to infinity except the source node.
 *
 * 2. Priority Queue Operations:
 * - Use a min-heap (PriorityQueue) to always pick the node with the smallest
 * current distance.
 * - Each node can be inserted and extracted at most once.
 * - Each insertion/extraction in the heap costs O(log N).
 * - Total cost for node queue operations: O(N log N).
 *
 * 3. Edge Relaxations:
 * - For each node, relax all outgoing edges.
 * - There are M edges in total.
 * - Each relaxation may update a node's distance and cause a new insertion
 * into the priority queue.
 * - Each insertion takes O(log N).
 * - Total cost for edge relaxations: O(M log N).
 *
 * Total Time Complexity:
 * O((N + M) log N)
 *
 * Explanation:
 * - The algorithm performs at most N insertions/extractions for nodes.
 * - It performs up to M edge relaxations, each potentially causing a heap
 * update.
 * - Using a binary heap, each heap operation takes O(log N).
 *
 * Notes:
 * - With more advanced heaps (e.g., Fibonacci Heap), complexity can improve to
 * O(N log N + M), but binary heaps are often faster in practice.
 */
