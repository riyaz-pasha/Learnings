import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

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


class KhansTopologicalSort {

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
