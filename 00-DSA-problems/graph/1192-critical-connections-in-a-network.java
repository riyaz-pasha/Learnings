
/*
 * There are n servers numbered from 0 to n - 1 connected by undirected
 * server-to-server connections forming a network where connections[i] = [ai,
 * bi] represents a connection between servers ai and bi. Any server can reach
 * other servers directly or indirectly through the network.
 * 
 * A critical connection is a connection that, if removed, will make some
 * servers unable to reach some other server.
 * 
 * Return all critical connections in the network in any order.
 * 
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class CriticalConnectionsInANetwork {
    private int time = 0;

    public List<List<Integer>> criticalConnections(int n, List<List<Integer>> connections) {
        List<List<Integer>> adjList = buildAdjList(n, connections);
        List<List<Integer>> bridges = new ArrayList<>();
        int[] discoveryTime = new int[n];
        int[] lowTime = new int[n];
        boolean[] visited = new boolean[n];
        Arrays.fill(discoveryTime, -1);
        Arrays.fill(lowTime, -1);

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                dfs(i, -1, discoveryTime, lowTime, visited, bridges, adjList);
            }
        }
        return bridges;
    }

    private void dfs(int currentNode, int parentNode, int[] discoveryTime, int[] lowTime, boolean[] visited,
            List<List<Integer>> bridges,
            List<List<Integer>> adjList) {
        visited[currentNode] = true;
        discoveryTime[currentNode] = lowTime[currentNode] = time++;
        for (int neighbor : adjList.get(currentNode)) {
            if (neighbor == parentNode)
                continue;
            if (!visited[neighbor]) {
                dfs(neighbor, currentNode, discoveryTime, lowTime, visited, bridges, adjList);
                lowTime[neighbor] = Math.min(lowTime[neighbor], lowTime[currentNode]);
                if (lowTime[neighbor] > discoveryTime[currentNode]) {
                    bridges.add(Arrays.asList(currentNode, neighbor));
                }
            } else {
                lowTime[currentNode] = Math.min(lowTime[currentNode], discoveryTime[neighbor]);
            }
        }
    }

    private List<List<Integer>> buildAdjList(int n, List<List<Integer>> connections) {
        List<List<Integer>> adjList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adjList.add(new ArrayList<>());
        }
        for (List<Integer> connection : connections) {
            adjList.get(connection.get(0)).add(connection.get(1));
            adjList.get(connection.get(1)).add(connection.get(0));
        }
        return adjList;
    }

}

class Solution {

    /**
     * Tarjan's Bridge-Finding Algorithm
     * Time: O(V + E), Space: O(V + E)
     */
    public List<List<Integer>> criticalConnections(int n, List<List<Integer>> connections) {
        // Build adjacency list
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (List<Integer> conn : connections) {
            int u = conn.get(0), v = conn.get(1);
            graph.get(u).add(v);
            graph.get(v).add(u);
        }

        List<List<Integer>> bridges = new ArrayList<>();
        boolean[] visited = new boolean[n];
        int[] discoveryTime = new int[n]; // When node was first visited
        int[] lowLink = new int[n]; // Lowest discovery time reachable
        int[] parent = new int[n];
        Arrays.fill(parent, -1);

        int[] time = { 0 }; // Use array to pass by reference

        // Run DFS from all unvisited nodes (handles disconnected components)
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                bridgeUtil(i, visited, discoveryTime, lowLink, parent, graph, bridges, time);
            }
        }

        return bridges;
    }

    private void bridgeUtil(int u, boolean[] visited, int[] disc, int[] low,
            int[] parent, List<List<Integer>> graph,
            List<List<Integer>> bridges, int[] time) {
        visited[u] = true;
        disc[u] = low[u] = ++time[0];

        for (int v : graph.get(u)) {
            if (!visited[v]) {
                parent[v] = u;
                bridgeUtil(v, visited, disc, low, parent, graph, bridges, time);

                // Update low-link value of u for parent function calls
                low[u] = Math.min(low[u], low[v]);

                // If low[v] > disc[u], then u-v is a bridge
                if (low[v] > disc[u]) {
                    bridges.add(Arrays.asList(u, v));
                }
            }
            // Back edge (not tree edge to parent)
            else if (v != parent[u]) {
                low[u] = Math.min(low[u], disc[v]);
            }
        }
    }

    /**
     * Alternative implementation with cleaner structure
     */
    public List<List<Integer>> criticalConnectionsV2(int n, List<List<Integer>> connections) {
        List<Set<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new HashSet<>());
        }

        for (List<Integer> conn : connections) {
            int u = conn.get(0), v = conn.get(1);
            graph.get(u).add(v);
            graph.get(v).add(u);
        }

        List<List<Integer>> result = new ArrayList<>();
        int[] rank = new int[n];
        Arrays.fill(rank, -2); // -2: unvisited, -1: visiting, >=0: visited

        dfs(graph, 0, 0, rank, result);
        return result;
    }

    private int dfs(List<Set<Integer>> graph, int node, int depth,
            int[] rank, List<List<Integer>> result) {
        if (rank[node] >= 0)
            return rank[node]; // Already visited

        rank[node] = depth;
        int minRank = depth;

        for (int neighbor : graph.get(node)) {
            if (rank[neighbor] == depth - 1)
                continue; // Skip parent

            int neighborRank = dfs(graph, neighbor, depth + 1, rank, result);

            // If neighbor can't reach back to current node or higher, it's a bridge
            if (neighborRank > depth) {
                result.add(Arrays.asList(node, neighbor));
            }

            minRank = Math.min(minRank, neighborRank);
        }

        return minRank;
    }

    /**
     * Brute Force Approach (for understanding/comparison)
     * Time: O(E * (V + E)), Space: O(V + E)
     * Remove each edge and check if graph becomes disconnected
     */
    public List<List<Integer>> criticalConnectionsBruteForce(int n, List<List<Integer>> connections) {
        List<List<Integer>> result = new ArrayList<>();

        for (int i = 0; i < connections.size(); i++) {
            // Remove edge i and check connectivity
            if (isDisconnected(n, connections, i)) {
                result.add(connections.get(i));
            }
        }

        return result;
    }

    private boolean isDisconnected(int n, List<List<Integer>> connections, int skipEdge) {
        // Build graph without the skipped edge
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        for (int i = 0; i < connections.size(); i++) {
            if (i == skipEdge)
                continue;
            List<Integer> conn = connections.get(i);
            int u = conn.get(0), v = conn.get(1);
            graph.get(u).add(v);
            graph.get(v).add(u);
        }

        // Check if all nodes are reachable from node 0
        boolean[] visited = new boolean[n];
        dfsVisit(0, graph, visited);

        for (boolean v : visited) {
            if (!v)
                return true; // Found unvisited node
        }
        return false;
    }

    private void dfsVisit(int node, List<List<Integer>> graph, boolean[] visited) {
        visited[node] = true;
        for (int neighbor : graph.get(node)) {
            if (!visited[neighbor]) {
                dfsVisit(neighbor, graph, visited);
            }
        }
    }

    // Helper method to convert int[][] to List<List<Integer>>
    public static List<List<Integer>> arrayToList(int[][] connections) {
        List<List<Integer>> result = new ArrayList<>();
        for (int[] conn : connections) {
            result.add(Arrays.asList(conn[0], conn[1]));
        }
        return result;
    }

    // Test the solution
    public static void main(String[] args) {
        Solution sol = new Solution();

        // Test case 1: n=4, connections=[[0,1],[1,2],[2,0],[1,3]]
        int[][] conn1 = { { 0, 1 }, { 1, 2 }, { 2, 0 }, { 1, 3 } };
        List<List<Integer>> connections1 = arrayToList(conn1);
        System.out.println("Test 1: " + sol.criticalConnections(4, connections1));
        // Expected: [[1,3]] - removing this edge disconnects node 3

        // Test case 2: n=2, connections=[[0,1]]
        int[][] conn2 = { { 0, 1 } };
        List<List<Integer>> connections2 = arrayToList(conn2);
        System.out.println("Test 2: " + sol.criticalConnections(2, connections2));
        // Expected: [[0,1]] - only one edge, so it's critical

        // Test case 3: Triangle + extra node
        int[][] conn3 = { { 0, 1 }, { 1, 2 }, { 2, 0 }, { 2, 3 }, { 3, 4 } };
        List<List<Integer>> connections3 = arrayToList(conn3);
        System.out.println("Test 3: " + sol.criticalConnections(5, connections3));
        // Expected: [[2,3],[3,4]] - edges outside the triangle cycle

        // Test with alternative implementation
        System.out.println("Test 1 (V2): " + sol.criticalConnectionsV2(4, connections1));
    }
}
