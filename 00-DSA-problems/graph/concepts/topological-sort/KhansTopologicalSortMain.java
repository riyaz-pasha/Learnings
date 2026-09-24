import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Kahn's Algorithm (Topological Sort using BFS)
 *
 * Topological Sort:
 * - Ordering of vertices in a Directed Acyclic Graph (DAG)
 * - For every directed edge u -> v, u must come before v in the ordering.
 *
 * Key Idea:
 * - inDegree[v] = number of incoming edges into v
 * - Nodes with inDegree = 0 have NO prerequisites, so they can be processed first.
 *
 * If we cannot process all vertices => Cycle exists.
 *
 * Time Complexity: O(V + E)
 * Space Complexity: O(V + E)
 */
class KhansTopologicalSort {

    public List<Integer> topologicalSort(int V, List<List<Integer>> adjList) {

        // --------------------------------------
        // Step 1: Calculate indegree of each node
        // --------------------------------------
        int[] inDegree = new int[V];

        for (int u = 0; u < V; u++) {
            for (int v : adjList.get(u)) {
                inDegree[v]++;  // v has one more incoming edge (dependency)
            }
        }

        // -------------------------------------------------------
        // Step 2: Push all nodes with indegree 0 into the queue
        //
        // These are nodes that can be processed immediately because
        // they have no prerequisites.
        // -------------------------------------------------------
        Queue<Integer> queue = new ArrayDeque<>();

        for (int node = 0; node < V; node++) {
            if (inDegree[node] == 0) {
                queue.offer(node);
            }
        }

        // This list will store the final topological order
        List<Integer> topoOrder = new ArrayList<>();

        // -------------------------------------------------------
        // Step 3: BFS-like processing
        //
        // Take a node with indegree 0, add it to result.
        // Then "remove" its outgoing edges by reducing indegree
        // of its neighbors.
        // -------------------------------------------------------
        while (!queue.isEmpty()) {

            int node = queue.poll();
            topoOrder.add(node);

            // Remove outgoing edges: node -> neighbor
            for (int neighbor : adjList.get(node)) {

                inDegree[neighbor]--;

                // If indegree becomes 0, it means all prerequisites are done
                if (inDegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // -------------------------------------------------------
        // Step 4: Cycle detection
        //
        // If topoOrder does NOT contain all vertices,
        // it means some vertices were never reduced to indegree 0.
        // That only happens when a cycle exists.
        // -------------------------------------------------------
        if (topoOrder.size() != V) {
            throw new IllegalStateException(
                    "Cycle detected! Topological sort is not possible.");
        }

        return topoOrder;
    }
}

/*
 * indegree[node] tells how many prerequisites are still pending.
 * A node can only be processed when indegree becomes 0.
 *
 * Kahn's algorithm keeps removing nodes with indegree 0.
 * If a cycle exists, nodes inside the cycle will never reach indegree 0.
 */


public class KhansTopologicalSortMain {

    public static void main(String[] args) {
        int numVertices = 6;
        List<List<Integer>> adjList = new ArrayList<>();
        for (int i = 0; i < numVertices; i++) {
            adjList.add(new ArrayList<>());
        }

        // Graph edges
        adjList.get(5).add(2);
        adjList.get(5).add(0);
        adjList.get(4).add(0);
        adjList.get(4).add(1);
        adjList.get(2).add(3);
        adjList.get(3).add(1);

        KhansTopologicalSort sorter = new KhansTopologicalSort();
        List<Integer> result = sorter.topologicalSort(numVertices, adjList);
        System.out.println("Topological Sort Order: " + result);
    }

}


class KhansTopologicalSort2 {

    public List<Integer> topologicalSort(int numVertices, List<List<Integer>> adjList) {
        int[] inDegree = new int[numVertices];
        for (int inVertex = 0; inVertex < numVertices; inVertex++) {
            for (int outVertex : adjList.get(inVertex)) {
                inDegree[outVertex]++;
            }
        }

        Queue<Integer> queue = new ArrayDeque<>();
        for (int vertex = 0; vertex < numVertices; vertex++) {
            if (inDegree[vertex] == 0) {
                queue.offer(vertex);
            }
        }

        List<Integer> topoOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            int currentVertex = queue.poll();
            topoOrder.add(currentVertex);
            for (int neighborVertex : adjList.get(currentVertex)) {
                inDegree[neighborVertex]--;
                if (inDegree[neighborVertex] == 0) {
                    queue.offer(neighborVertex);
                }
            }
        }

        if (topoOrder.size() != numVertices) {
            throw new IllegalStateException(
                    "Cycle detected! Topological sort not possible for graph with " + numVertices + " vertices.");
        }
        return topoOrder;
    }

}

/*
    Kahn's Topological Sort Algorithm (BFS-based)

    1. Compute in-degree for all vertices (i.e., count of incoming edges).
    2. Add all vertices with in-degree 0 to a queue (these have no dependencies).
    3. While the queue is not empty:
       a. Remove a vertex from the queue and add it to the topological order.
       b. For each neighbor of this vertex:
          i.   Reduce its in-degree by 1 (because one of its prerequisites is processed).
          ii.  If its in-degree becomes 0, add it to the queue.
    4. After processing all vertices:
       - If the size of the topological order == number of vertices, return it.
       - Otherwise, a cycle exists → Topological sort not possible.
*/

/*
    Time Complexity: O(V + E)
    - V = number of vertices
    - E = number of edges
    - Reason: 
      -> Calculating in-degrees takes O(E)
      -> Initializing the queue takes O(V)
      -> Each vertex is processed once in the BFS loop: O(V)
      -> Each edge is processed once when reducing in-degree: O(E)
      -> Total: O(V + E)

    Space Complexity: O(V + E)
    - inDegree array: O(V)
    - queue and topoOrder list: O(V)
    - adjacency list to store the graph: O(E)
    - Total: O(V + E)
*/
