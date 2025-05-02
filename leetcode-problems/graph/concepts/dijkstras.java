
import java.util.Arrays;
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
 * Dijkstra's Algorithm - Step-by-Step Explanation
 *
 * Step 1: Initialize
 *   - Set distance[] to ∞ (Integer.MAX_VALUE) for all vertices.
 *   - Set distance[source] = 0.
 *   - Set visited[] to false for all vertices.
 *   - Use a PriorityQueue (min-heap) to always process the vertex with the smallest distance.
 *
 * Step 2: Add the source vertex to the priority queue with distance 0.
 *
 * Step 3: Loop until the priority queue is empty:
 *   - Extract the vertex 'u' with the minimum distance from the queue.
 *   - If 'u' is already visited, skip it (continue).
 *   - Mark 'u' as visited.
 *
 * Step 4: For each unvisited neighbor 'v' of 'u':
 *   - If distance[u] + weight(u, v) < distance[v], then update:
 *       distance[v] = distance[u] + weight(u, v)
 *   - Add 'v' to the priority queue with the new distance.
 *
 * Step 5: After the loop ends, the distance[] array contains
 *         the shortest distances from the source vertex to all other vertices.
 */
