import java.util.*;
/*
 * Given a directed acyclic graph, with n vertices numbered from 0 to n-1, and
 * an array edges where edges[i] = [fromi, toi] represents a directed edge from
 * node fromi to node toi.
 * 
 * Find the smallest set of vertices from which all nodes in the graph are
 * reachable. It's guaranteed that a unique solution exists.
 * 
 * Notice that you can return the vertices in any order.
 * 
 * Example 1:
 * Input: n = 6, edges = [[0,1],[0,2],[2,5],[3,4],[4,2]]
 * Output: [0,3]
 * Explanation: It's not possible to reach all the nodes from a single vertex.
 * From 0 we can reach [0,1,2,5]. From 3 we can reach [3,4,2,5]. So we output
 * [0,3].
 * 
 * Example 2:
 * Input: n = 5, edges = [[0,1],[2,1],[3,1],[1,4],[2,4]]
 * Output: [0,2,3]
 * Explanation: Notice that vertices 0, 3 and 2 are not reachable from any other
 * node, so we must include them. Also any of these vertices can reach nodes 1
 * and 4.
 */

class Solution {

    /**
     * Approach 1: Count In-degrees
     * Time: O(V + E), Space: O(V)
     */
    public List<Integer> findSmallestSetOfVertices(int n, List<List<Integer>> edges) {
        // Count in-degrees for each vertex
        int[] inDegree = new int[n];

        for (List<Integer> edge : edges) {
            int to = edge.get(1);
            inDegree[to]++;
        }

        // Find all vertices with in-degree 0
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                result.add(i);
            }
        }

        return result;
    }

    /**
     * Approach 2: Using HashSet (Alternative)
     * Time: O(V + E), Space: O(V)
     */
    public List<Integer> findSmallestSetOfVerticesV2(int n, List<List<Integer>> edges) {
        // Set to track vertices that have incoming edges
        Set<Integer> hasIncoming = new HashSet<>();

        for (List<Integer> edge : edges) {
            hasIncoming.add(edge.get(1));
        }

        // Find vertices that don't have incoming edges
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!hasIncoming.contains(i)) {
                result.add(i);
            }
        }

        return result;
    }

    /**
     * Test the solution
     */
    public static void main(String[] args) {
        Solution sol = new Solution();

        // Test case 1: [[0,1],[0,2],[2,5],[3,4],[4,2]]
        // Expected: [0,3] (vertices with no incoming edges)
        List<List<Integer>> edges1 = Arrays.asList(
                Arrays.asList(0, 1),
                Arrays.asList(0, 2),
                Arrays.asList(2, 5),
                Arrays.asList(3, 4),
                Arrays.asList(4, 2));
        System.out.println("Test 1: " + sol.findSmallestSetOfVertices(6, edges1));

        // Test case 2: [[0,1],[2,1],[3,1],[1,4],[2,4]]
        // Expected: [0,2,3] (vertices with no incoming edges)
        List<List<Integer>> edges2 = Arrays.asList(
                Arrays.asList(0, 1),
                Arrays.asList(2, 1),
                Arrays.asList(3, 1),
                Arrays.asList(1, 4),
                Arrays.asList(2, 4));
        System.out.println("Test 2: " + sol.findSmallestSetOfVertices(5, edges2));
    }

}

/**
 * Algorithm Explanation:
 * 
 * 1. In a DAG, to reach all vertices, we need to start from vertices that
 * cannot be reached from any other vertex (in-degree = 0)
 * 
 * 2. Any vertex with incoming edges can be reached from some other vertex,
 * so we don't need to include it in our starting set
 * 
 * 3. We count in-degrees and return all vertices with in-degree 0
 * 
 * Time Complexity: O(V + E) where V = vertices, E = edges
 * - We iterate through all edges once: O(E)
 * - We iterate through all vertices once: O(V)
 * 
 * Space Complexity: O(V) for the in-degree array or HashSet
 */
