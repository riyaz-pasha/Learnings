import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class FindBridgesTarjansAlgo {
    private int timestamp = 0;
    private final List<List<Integer>> bridgeList = new ArrayList<>();

    public List<List<Integer>> findBridgesUsingTarjansAlgorithm(int numVertices, List<List<Integer>> adjList) {
        int[] discoveryTime = new int[numVertices];
        int[] reachableAncestorTime = new int[numVertices];
        boolean[] visited = new boolean[numVertices];
        Arrays.fill(discoveryTime, -1);
        Arrays.fill(reachableAncestorTime, -1);

        for (int currentVertex = 0; currentVertex < numVertices; currentVertex++) {
            if (!visited[currentVertex]) {
                dfs(currentVertex, -1, visited, discoveryTime, reachableAncestorTime, adjList);
            }
        }
        return bridgeList;
    }

    private void dfs(int currentVertex, int parentVertex,
            boolean[] visited,
            int[] discoveryTime, int[] reachableAncestorTime,
            List<List<Integer>> adjList) {
        visited[currentVertex] = true;
        discoveryTime[currentVertex] = reachableAncestorTime[currentVertex] = timestamp++;
        for (int neighbor : adjList.get(currentVertex)) {
            if (neighbor == parentVertex) {
                continue;
            }
            if (!visited[neighbor]) {
                dfs(neighbor, currentVertex, visited, discoveryTime, reachableAncestorTime, adjList);
                reachableAncestorTime[currentVertex] = Math.min(
                        reachableAncestorTime[neighbor],
                        reachableAncestorTime[currentVertex]);
                if (reachableAncestorTime[neighbor] > discoveryTime[currentVertex]) {
                    bridgeList.add(Arrays.asList(currentVertex, neighbor));
                }
            } else {
                reachableAncestorTime[currentVertex] = Math.min(reachableAncestorTime[currentVertex],
                        discoveryTime[neighbor]);
            }
        }
    }
}

/*
 * Algorithm: Tarjan's Algorithm to Find Bridges in an Undirected Graph
 *
 * Step-by-step:
 * 1. Initialize:
 *    - discoveryTime[]: stores the time each node is first visited during DFS.
 *    - earliestReachableTime[]: stores the lowest discovery time reachable from the current node or its descendants.
 *    - visited[]: marks nodes that have already been visited.
 *    - time: global counter incremented on each DFS visit.
 *
 * 2. For each unvisited node in the graph, start a DFS traversal:
 *    - Set discoveryTime and earliestReachableTime to current time, then increment time.
 *
 * 3. For each neighbor of the current node:
 *    a. If neighbor is the parent (i.e., the node we came from), skip it.
 *    b. If neighbor is not visited:
 *       - Recursively DFS into neighbor.
 *       - After returning, update earliestReachableTime[currentNode] = min(earliestReachableTime[currentNode], earliestReachableTime[neighbor]).
 *       - If earliestReachableTime[neighbor] > discoveryTime[currentNode], then (currentNode, neighbor) is a bridge.
 *    c. If neighbor is already visited and is not the parent:
 *       - Update earliestReachableTime[currentNode] = min(earliestReachableTime[currentNode], discoveryTime[neighbor]) (back edge case).
 *
 * 4. Collect all such (u, v) pairs where the bridge condition holds.
 *
 * Time Complexity: O(V + E), where V is the number of nodes and E is the number of edges.
 */
