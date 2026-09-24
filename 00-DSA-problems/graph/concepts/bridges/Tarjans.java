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



/*
 ============================================================
 Tarjan's Algorithm to Find Bridges in an Undirected Graph
 ============================================================

 A BRIDGE is an edge whose removal increases the number
 of connected components in the graph.

 Core idea:
 ----------
 Use DFS + timestamps to detect whether a subtree can
 reach an ancestor without using the parent edge.

 Key arrays:
 -----------
 disc[u] → discovery time of node u (when DFS first visits it)
 low[u]  → earliest discovery time reachable from u
           (using tree edges + at most one back edge)

 Bridge condition:
 -----------------
 For an edge (u, v) where u is parent of v in DFS:

     if (low[v] > disc[u]) → (u, v) is a BRIDGE

 ============================================================
 */

class FindBridgesTarjansAlgo2 {

    // Global DFS timer
    private int time = 0;

    // Stores all bridges found
    private final List<List<Integer>> bridges = new ArrayList<>();

    /*
     ------------------------------------------------------------
     Public API
     ------------------------------------------------------------
     numVertices → number of vertices (0 to V-1)
     adjList     → adjacency list of an UNDIRECTED graph
     ------------------------------------------------------------
     */
    public List<List<Integer>> findBridgesUsingTarjansAlgorithm(
            int numVertices,
            List<List<Integer>> adjList) {

        // discovery time of each vertex
        int[] disc = new int[numVertices];

        // low-link value of each vertex
        int[] low = new int[numVertices];

        // marks whether a vertex is visited in DFS
        boolean[] visited = new boolean[numVertices];

        // Initialize discovery times to "unvisited"
        Arrays.fill(disc, -1);
        Arrays.fill(low, -1);

        // Graph may be disconnected → run DFS from every vertex
        for (int v = 0; v < numVertices; v++) {
            if (!visited[v]) {
                dfs(v, -1, visited, disc, low, adjList);
            }
        }

        return bridges;
    }

    /*
     ------------------------------------------------------------
     DFS helper
     ------------------------------------------------------------
     u      → current vertex
     parent → vertex from which u was discovered
     ------------------------------------------------------------
     */
    private void dfs(int u,
                     int parent,
                     boolean[] visited,
                     int[] disc,
                     int[] low,
                     List<List<Integer>> adjList) {

        // Mark current node as visited
        visited[u] = true;

        // Set discovery time and low-link value
        disc[u] = low[u] = time++;

        // Explore all neighbors
        for (int v : adjList.get(u)) {

            // Ignore the edge back to parent
            if (v == parent) {
                continue;
            }

            /*
             ----------------------------------------------------
             CASE 1: Tree Edge (v not visited)
             ----------------------------------------------------
             */
            if (!visited[v]) {

                // DFS deeper
                dfs(v, u, visited, disc, low, adjList);

                /*
                 After returning from DFS(v),
                 v's subtree might reach an ancestor of u.
                 So we update low[u] using low[v].
                 */
                low[u] = Math.min(low[u], low[v]);

                /*
                 BRIDGE CHECK:
                 -------------
                 If v (and its subtree) cannot reach u or any
                 ancestor of u, then the edge (u, v) is the
                 ONLY connection → BRIDGE.
                 */
                if (low[v] > disc[u]) {
                    bridges.add(Arrays.asList(u, v));
                }

            }
            /*
             ----------------------------------------------------
             CASE 2: Back Edge (v already visited and not parent)
             ----------------------------------------------------
             */
            else {

                /*
                 IMPORTANT LINE (you asked about this):
                 -------------------------------------
                 We use disc[v], NOT low[v].

                 Why?
                 - v is an ancestor of u
                 - u can directly reach v via this edge
                 - u CANNOT use paths that v's subtree uses
                 - Therefore, the earliest u can reach via
                   this back edge is disc[v]
                 */
                low[u] = Math.min(low[u], disc[v]);
            }
        }
    }
}

/*
 * WHY low[u] = min(low[u], disc[v])  (and NOT low[v]) ?
 *
 * This line happens in the "back edge" case:
 *
 *      u -----> v
 *
 * where v is already visited and v is NOT parent of u.
 * That means (u -> v) is a BACK EDGE and v is an ANCESTOR of u.
 *
 * ------------------------------------------------------------
 * EXAMPLE GRAPH:
 * ------------------------------------------------------------
 *
 *          0
 *         / \
 *        1   2
 *       /
 *      3
 *     / \
 *    4   5
 *
 * plus one back edge: 4 ----> 1
 *
 * DFS path could be:
 * 0 -> 1 -> 3 -> 4
 *
 * So:
 * disc[0]=0
 * disc[1]=1
 * disc[3]=2
 * disc[4]=3
 *
 * Now at node u = 4:
 * we see an edge (4 -> 1)
 *
 * - node 1 is already visited
 * - node 1 is NOT parent of 4
 * => this is a BACK EDGE to ancestor 1
 *
 * ------------------------------------------------------------
 * What does this back edge mean?
 * ------------------------------------------------------------
 *
 * From node 4, we can directly jump to node 1.
 *
 * So the earliest ancestor reachable from 4 is exactly:
 * disc[1] = 1
 *
 * Therefore:
 * low[4] = min(low[4], disc[1])
 *
 * ------------------------------------------------------------
 * Why NOT low[1] ?
 * ------------------------------------------------------------
 *
 * Suppose node 1 has another back edge somewhere in its subtree
 * that reaches node 0, so low[1] could become 0.
 *
 * Example: if 1's subtree had an edge reaching 0, then:
 * low[1] = 0
 *
 * If we incorrectly do:
 * low[4] = min(low[4], low[1])
 *
 * then we would be saying:
 * "Node 4 can reach node 0"
 *
 * BUT THAT IS WRONG because:
 * - The back edge we have is ONLY (4 -> 1)
 * - From 4 we can reach 1, but we cannot magically use
 *   1's subtree paths to reach 0.
 *
 * low[1] includes information about paths reachable from
 * node 1's subtree, which node 4 cannot directly access
 * through the single back edge (4 -> 1).
 *
 * ------------------------------------------------------------
 * Conclusion:
 * ------------------------------------------------------------
 *
 * For a back edge (u -> v):
 * - u can reach v directly
 * - so the earliest reachable ancestor is disc[v]
 * - not low[v] (which includes v's subtree reachability)
 */
