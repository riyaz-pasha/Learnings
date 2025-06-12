import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/*
 * You are given an integer n, the number of nodes in a directed graph
 * where the nodes are labeled from 0 to n - 1. 
 * Each edge is red or blue in this graph, and there could be self-edges and parallel edges.
 * 
 * You are given two arrays redEdges and blueEdges where:
 * 
 * redEdges[i] = [ai, bi] indicates that there is a directed red edge from node ai to node bi in the graph, and
 * blueEdges[j] = [uj, vj] indicates that there is a directed blue edge from node uj to node vj in the graph.
 * 
 * Return an array answer of length n, 
 * where each answer[x] is the length of the shortest path
 * from node 0 to node x such that the edge colors alternate along the path, 
 * or -1 if such a path does not exist.
 * 
 * Example 1:
 * 
 * Input: n = 3, redEdges = [[0,1],[1,2]], blueEdges = []
 * Output: [0,1,-1]
 * Example 2:
 * 
 * Input: n = 3, redEdges = [[0,1]], blueEdges = [[2,1]]
 * Output: [0,1,-1]
 * 
 */

class ShortestPathWithAlternatingColors {

    public int[] shortestAlternatingPaths(int n, int[][] redEdges, int[][] blueEdges) {
        List<Integer>[] redGraph = new ArrayList[n];
        List<Integer>[] blueGraph = new ArrayList[n];

        for (int i = 0; i < n; i++) {
            redGraph[i] = new ArrayList<>();
            blueGraph[i] = new ArrayList<>();
        }

        for (int[] edge : redEdges)
            redGraph[edge[0]].add(edge[1]);
        for (int[] edge : blueEdges)
            blueGraph[edge[0]].add(edge[1]);

        int[][] dist = new int[n][2]; // [][0]=red, [][1]=blue
        for (int i = 0; i < n; i++) {
            Arrays.fill(dist[i], Integer.MAX_VALUE);
        }
        dist[0][0] = dist[0][1] = 0;

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { 0, 0 }); // node, color (0 = red)
        queue.offer(new int[] { 0, 1 }); // node, color (1 = blue)

        boolean[][] visited = new boolean[n][2];

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int node = curr[0];
            int color = curr[1];
            if (visited[node][color])
                continue;
            visited[node][color] = true;

            List<Integer>[] nextGraph = (color == 0) ? blueGraph : redGraph;
            for (int neighbor : nextGraph[node]) {
                if (!visited[neighbor][1 - color]) {
                    dist[neighbor][1 - color] = Math.min(dist[neighbor][1 - color], dist[node][color] + 1);
                    queue.offer(new int[] { neighbor, 1 - color });
                }
            }
        }

        int[] res = new int[n];
        for (int i = 0; i < n; i++) {
            int minDist = Math.min(dist[i][0], dist[i][1]);
            res[i] = (minDist == Integer.MAX_VALUE) ? -1 : minDist;
        }
        return res;
    }

}

// Time Complexity: O(N + E) where N = nodes, E = total edges
// Space Complexity: O(N + E) for adjacency lists and visited array
class Solution {
    /*
     * APPROACH: BFS with State Tracking
     * 
     * Key Insights:
     * 1. We need alternating colors, so the state includes both node and last edge
     * color
     * 2. From each node, we can only take edges of the opposite color from how we
     * arrived
     * 3. We start from node 0 and can take either red or blue as first edge
     * 4. Use BFS to find shortest paths (BFS guarantees shortest path in unweighted
     * graphs)
     * 
     * State representation: (node, lastEdgeColor)
     * - lastEdgeColor: 0 = red, 1 = blue, -1 = no previous edge (start)
     * 
     * Algorithm:
     * 1. Build adjacency lists for red and blue edges separately
     * 2. Use BFS starting from (0, red) and (0, blue) simultaneously
     * 3. For each state (node, color), try all edges of opposite color
     * 4. Track minimum distance to reach each node regardless of last edge color
     */

    public int[] shortestAlternatingPaths(int n, int[][] redEdges, int[][] blueEdges) {
        // Build adjacency lists for red and blue edges
        List<List<Integer>> redGraph = new ArrayList<>();
        List<List<Integer>> blueGraph = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            redGraph.add(new ArrayList<>());
            blueGraph.add(new ArrayList<>());
        }

        // Populate red edges
        for (int[] edge : redEdges) {
            redGraph.get(edge[0]).add(edge[1]);
        }

        // Populate blue edges
        for (int[] edge : blueEdges) {
            blueGraph.get(edge[0]).add(edge[1]);
        }

        // BFS with state: [node, lastEdgeColor, distance]
        // lastEdgeColor: 0 = red, 1 = blue
        Queue<int[]> queue = new LinkedList<>();

        // visited[node][color] = true if we've visited this node with this last edge
        // color
        boolean[][] visited = new boolean[n][2];

        // Result array - initialize with -1 (unreachable)
        int[] result = new int[n];
        Arrays.fill(result, -1);
        result[0] = 0; // Distance to node 0 is always 0

        // Start BFS from node 0 with both possible first edge colors
        // We can start with either red or blue edge from node 0
        queue.offer(new int[] { 0, 0, 0 }); // [node, lastColor=red, distance]
        queue.offer(new int[] { 0, 1, 0 }); // [node, lastColor=blue, distance]
        visited[0][0] = visited[0][1] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int node = current[0];
            int lastColor = current[1]; // 0=red, 1=blue
            int distance = current[2];

            /*
             * From current state (node, lastColor), we can only take edges of opposite
             * color
             * - If lastColor was red (0), we can only take blue edges (1)
             * - If lastColor was blue (1), we can only take red edges (0)
             */

            // Try opposite color edges
            List<List<Integer>> nextGraph = (lastColor == 0) ? blueGraph : redGraph;
            int nextColor = 1 - lastColor; // Toggle color: 0->1, 1->0

            for (int neighbor : nextGraph.get(node)) {
                // If we haven't visited this neighbor with this edge color
                if (!visited[neighbor][nextColor]) {
                    visited[neighbor][nextColor] = true;

                    // Update result if this is first time reaching this neighbor
                    // or if we found a shorter path
                    if (result[neighbor] == -1) {
                        result[neighbor] = distance + 1;
                    }

                    queue.offer(new int[] { neighbor, nextColor, distance + 1 });
                }
            }
        }

        return result;
    }

}

// Alternative Solution: Cleaner BFS Implementation
// Time Complexity: O(N + E) where N = nodes, E = total edges
// Space Complexity: O(N + E) for adjacency lists and visited array
class Solution2 {
    /*
     * CLEANER APPROACH: Using custom class for better readability
     */

    class State {
        int node;
        int lastColor; // 0=red, 1=blue
        int distance;

        State(int node, int lastColor, int distance) {
            this.node = node;
            this.lastColor = lastColor;
            this.distance = distance;
        }
    }

    public int[] shortestAlternatingPaths(int n, int[][] redEdges, int[][] blueEdges) {
        // Build adjacency lists
        List<Integer>[][] graph = new List[n][2]; // graph[node][color]

        for (int i = 0; i < n; i++) {
            graph[i][0] = new ArrayList<>(); // red edges
            graph[i][1] = new ArrayList<>(); // blue edges
        }

        // Add red edges (color 0)
        for (int[] edge : redEdges) {
            graph[edge[0]][0].add(edge[1]);
        }

        // Add blue edges (color 1)
        for (int[] edge : blueEdges) {
            graph[edge[0]][1].add(edge[1]);
        }

        // BFS
        Queue<State> queue = new LinkedList<>();
        boolean[][] visited = new boolean[n][2];
        int[] result = new int[n];
        Arrays.fill(result, -1);
        result[0] = 0;

        // Start with both colors from node 0
        queue.offer(new State(0, 0, 0)); // Start with red
        queue.offer(new State(0, 1, 0)); // Start with blue
        visited[0][0] = visited[0][1] = true;

        while (!queue.isEmpty()) {
            State current = queue.poll();

            // Try opposite color edges
            int nextColor = 1 - current.lastColor;

            for (int neighbor : graph[current.node][nextColor]) {
                if (!visited[neighbor][nextColor]) {
                    visited[neighbor][nextColor] = true;

                    if (result[neighbor] == -1) {
                        result[neighbor] = current.distance + 1;
                    }

                    queue.offer(new State(neighbor, nextColor, current.distance + 1));
                }
            }
        }

        return result;
    }

}
