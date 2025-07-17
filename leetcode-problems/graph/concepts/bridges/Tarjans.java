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
                // neighbor cannot reach any ancestor of currentVertex
                // The only path between them is through edge (currentVertex, neighbor)
                // Removing this edge would disconnect the graph
                // Therefore, it's a bridge!
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

/*
 * MATHEMATICAL EXPLANATION:
 * 
 * The reachableAncestorTime (low-link) value represents:
 * "The earliest discovery time reachable from this vertex"
 * 
 * When we encounter a back edge from vertex u to vertex v:
 * - v was discovered at time discoveryTime[v]
 * - v might be able to reach even earlier vertices (reachableAncestorTime[v] <
 * discoveryTime[v])
 * - But u can only reach v directly, so u can reach "at earliest" the time when
 * v was discovered
 * - Therefore: reachableAncestorTime[u] = min(reachableAncestorTime[u],
 * discoveryTime[v])
 * 
 * If we used reachableAncestorTime[v] instead:
 * - We'd be saying u can reach whatever v can reach
 * - But u can only reach v directly via the back edge
 * - What v can reach beyond that is irrelevant to u's reachability
 * - This would give incorrect low-link values
 */