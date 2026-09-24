/**
 * ============================================================================
 * ALL PATHS FROM SOURCE TO TARGET (DAG) - COMPREHENSIVE GUIDE & SOLUTIONS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * Imagine you are navigating a city with only one-way streets. You start at 
 * location 0 and want to reach the final location (n-1). Because the streets 
 * are uniquely designed, it's impossible to drive in a circle (Directed Acyclic 
 * Graph - DAG). Your task is to write down the exact sequence of intersections 
 * for every single possible valid route from the start to the destination.
 * 
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: Are there any cycles in the graph? 
 * A: No, the problem specifies it is a Directed Acyclic Graph (DAG). This is 
 *    a massive hint because it means we don't need a `visited` array to prevent 
 *    infinite loops!
 * 
 * Q: Is it possible for a node to be visited in multiple different paths?
 * A: Yes. Node 'X' might be reachable via path A and path B. We must explore 
 *    outgoing edges from 'X' for both paths.
 * 
 * Q: Can the graph be disconnected such that the target is unreachable?
 * A: Yes. In that case, we should simply return an empty list of paths.
 * 
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - GRAPH TRAVERSAL: This is a classic graph traversal problem (DFS or BFS).
 * - NO VISITED SET: Since it's a DAG, we naturally avoid infinite loops. A 
 *   `visited` set would actually be harmful here because we WANT to visit a 
 *   node multiple times if it is part of multiple distinct paths.
 * - BACKTRACKING: When using Depth-First Search (DFS), as we go down a path, 
 *   we add nodes to our current route. When we hit a dead end or the target, 
 *   we must "backtrack" (remove the last node) to explore other branches.
 * 
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Immediately highlight the keyword "DAG" and explicitly state: 
 *   "Because it's a DAG, we won't need a visited array." Interviewers love this.
 * - Step 2: Explain that finding *all combinations/paths* strongly suggests 
 *   using Backtracking (DFS).
 * - Step 3: Write out the Recursive DFS. Emphasize that adding to the `result` 
 *   requires making a *copy* of the current path, otherwise, later modifications 
 *   will alter the saved paths.
 * - Step 4: If asked for alternatives, you can explain Iterative BFS using a 
 *   queue of paths.
 * 
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * Input: graph = [[1,2], [3], [3], []]
 * Target: n - 1 = 4 - 1 = 3
 * 
 * Graph Drawing:
 *     0 ---> 1
 *     |      |
 *     v      v
 *     2 ---> 3
 * 
 * Traversal (DFS):
 * Start at 0. Path: [0]
 *  -> Go to 1. Path: [0, 1]
 *      -> Go to 3. Path: [0, 1, 3] -> TARGET HIT! Add copy to result.
 *      -> Backtrack to 1. No more neighbors.
 *  -> Backtrack to 0. Path: [0]
 *  -> Go to 2. Path: [0, 2]
 *      -> Go to 3. Path: [0, 2, 3] -> TARGET HIT! Add copy to result.
 *      -> Backtrack to 2. No more neighbors.
 * 
 * Result: [[0,1,3], [0,2,3]]
 */

import java.util.*;

class AllPathsSourceToTarget {

    /**
     * SOLUTION 1: Recursive DFS with Backtracking (Most Optimal & Standard)
     * ------------------------------------------------------------------------
     * Pros: Very memory efficient because we reuse a single ArrayList for the
     * path, allocating new memory only when a valid path is found.
     * Cons: Uses the JVM call stack, but constraints (n <= 15) make it 100% safe.
     * 
     * Time Complexity: O(2^N * N). In the worst-case DAG (every node connects to 
     * every subsequent node), there are 2^(N-1) paths, and copying each takes O(N).
     * Space Complexity: O(N) for the recursion stack and the current path list.
     */
    public List<List<Integer>> allPathsSourceTargetDFS(int[][] graph) {
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> currentPath = new ArrayList<>();
        
        // Always start at node 0
        currentPath.add(0);
        dfs(graph, 0, currentPath, result);
        
        return result;
    }

    private void dfs(int[][] graph, int currentNode, List<Integer> currentPath, List<List<Integer>> result) {
        // Base case: If we've reached the target node (n - 1)
        if (currentNode == graph.length - 1) {
            // MUST MAKE A COPY! If we add 'currentPath' directly, future 
            // backtracks will empty out the lists inside our result.
            result.add(new ArrayList<>(currentPath));
            return;
        }

        // Iterate through all nodes the current node has a directed edge to
        for (int neighbor : graph[currentNode]) {
            // 1. Choose: Add neighbor to current path
            currentPath.add(neighbor);
            
            // 2. Explore: Recurse down this path
            dfs(graph, neighbor, currentPath, result);
            
            // 3. Un-choose (Backtrack): Remove the neighbor to explore the next one
            // We use removeLast() if using Java 21+, otherwise remove(size - 1)
            currentPath.remove(currentPath.size() - 1); 
        }
    }

    /**
     * SOLUTION 2: Iterative Breadth-First Search (BFS)
     * ------------------------------------------------------------------------
     * Pros: Avoids recursion. Finds shortest paths first (in terms of edges).
     * Feature Highlight: We use Java 14+ `record` to tie the current node with 
     * the path taken to reach it. This makes the code exceptionally clean.
     * 
     * Time Complexity: O(2^N * N)
     * Space Complexity: O(2^N * N) to store all partial paths in the queue.
     */
    public List<List<Integer>> allPathsSourceTargetBFS(int[][] graph) {
        List<List<Integer>> result = new ArrayList<>();
        int targetNode = graph.length - 1;

        // Modern Java Feature: Record creates an immutable data carrier.
        // It holds the current node and the path taken to get there.
        record NodePath(int node, List<Integer> path) {}

        Queue<NodePath> queue = new LinkedList<>();
        
        // Initialize the first path
        List<Integer> startPath = new ArrayList<>();
        startPath.add(0);
        queue.offer(new NodePath(0, startPath));

        while (!queue.isEmpty()) {
            NodePath current = queue.poll();
            
            if (current.node() == targetNode) {
                result.add(current.path());
                continue; // No outgoing edges from target in our paths
            }

            for (int neighbor : graph[current.node()]) {
                // Create a new path for this specific neighbor
                List<Integer> newPath = new ArrayList<>(current.path());
                newPath.add(neighbor);
                queue.offer(new NodePath(neighbor, newPath));
            }
        }

        return result;
    }

    /**
     * SOLUTION 3: Iterative Depth-First Search (DFS) Using Stack
     * ------------------------------------------------------------------------
     * Pros: Mirrors recursive DFS but uses heap space (Deque) instead of JVM 
     * stack space.
     * 
     * Time Complexity: O(2^N * N)
     * Space Complexity: O(2^N * N) for the stack storing partial paths.
     */
    public List<List<Integer>> allPathsSourceTargetIterativeDFS(int[][] graph) {
        List<List<Integer>> result = new ArrayList<>();
        int targetNode = graph.length - 1;

        record NodePath(int node, List<Integer> path) {}
        
        // Deque is the modern Java replacement for the legacy Stack class
        Deque<NodePath> stack = new ArrayDeque<>();
        
        List<Integer> startPath = new ArrayList<>();
        startPath.add(0);
        stack.push(new NodePath(0, startPath));

        while (!stack.isEmpty()) {
            NodePath current = stack.pop();

            if (current.node() == targetNode) {
                result.add(current.path());
                continue;
            }

            for (int neighbor : graph[current.node()]) {
                List<Integer> newPath = new ArrayList<>(current.path());
                newPath.add(neighbor);
                stack.push(new NodePath(neighbor, newPath));
            }
        }

        return result;
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        AllPathsSourceToTarget solver = new AllPathsSourceToTarget();

        /*
         * Graph definition:
         * 0 -> [1, 2]
         * 1 -> [3]
         * 2 -> [3]
         * 3 -> []
         */
        int[][] graph = {
            {1, 2}, // Edges from 0
            {3},    // Edges from 1
            {3},    // Edges from 2
            {}      // Edges from 3 (Target)
        };

        System.out.println("--- Testing Solution 1: Recursive DFS ---");
        List<List<Integer>> dfsResult = solver.allPathsSourceTargetDFS(graph);
        dfsResult.forEach(System.out::println);

        System.out.println("\n--- Testing Solution 2: Iterative BFS ---");
        List<List<Integer>> bfsResult = solver.allPathsSourceTargetBFS(graph);
        bfsResult.forEach(System.out::println);

        System.out.println("\n--- Testing Solution 3: Iterative DFS ---");
        // Note: Iterative DFS might output paths in a different order due to stack LIFO property
        List<List<Integer>> iterDfsResult = solver.allPathsSourceTargetIterativeDFS(graph);
        iterDfsResult.forEach(System.out::println);
    }
}
