import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/*
 * There are a total of numCourses courses you have to take, labeled from 0 to
 * numCourses - 1. You are given an array prerequisites where prerequisites[i] =
 * [ai, bi] indicates that you must take course bi first if you want to take
 * course ai.
 * 
 * For example, the pair [0, 1], indicates that to take course 0 you have to
 * first take course 1.
 * Return true if you can finish all courses. Otherwise, return false.
 * 
 * Example 1:
 * Input: numCourses = 2, prerequisites = [[1,0]]
 * Output: true
 * Explanation: There are a total of 2 courses to take.
 * To take course 1 you should have finished course 0. So it is possible.
 * 
 * Example 2:
 * Input: numCourses = 2, prerequisites = [[1,0],[0,1]]
 * Output: false
 * Explanation: There are a total of 2 courses to take.
 * To take course 1 you should have finished course 0, and to take course 0 you
 * should also have finished course 1. So it is impossible.
 */

// SOLUTION 1: DFS with Cycle Detection (Most Intuitive)
class Solution {

    public boolean canFinish(int numCourses, int[][] prerequisites) {
        // Build adjacency list
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] prereq : prerequisites) {
            int course = prereq[0];
            int prerequisite = prereq[1];
            graph.get(prerequisite).add(course); // prerequisite -> course
        }

        // DFS with three states: 0=unvisited, 1=visiting, 2=visited
        int[] state = new int[numCourses];

        for (int i = 0; i < numCourses; i++) {
            if (state[i] == 0 && hasCycle(graph, i, state)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasCycle(List<List<Integer>> graph, int course, int[] state) {
        if (state[course] == 1)
            return true; // Back edge found - cycle detected
        if (state[course] == 2)
            return false; // Already processed

        state[course] = 1; // Mark as visiting

        for (int nextCourse : graph.get(course)) {
            if (hasCycle(graph, nextCourse, state)) {
                return true;
            }
        }

        state[course] = 2; // Mark as visited
        return false;
    }

}

// SOLUTION 2: BFS - Kahn's Algorithm (Topological Sort)
class SolutionBFS {

    public boolean canFinish(int numCourses, int[][] prerequisites) {
        // Build adjacency list and calculate in-degrees
        List<List<Integer>> graph = new ArrayList<>();
        int[] inDegree = new int[numCourses];

        for (int i = 0; i < numCourses; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] prereq : prerequisites) {
            int course = prereq[0];
            int prerequisite = prereq[1];
            graph.get(prerequisite).add(course);
            inDegree[course]++;
        }

        // Start with courses that have no prerequisites
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < numCourses; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }

        int completedCourses = 0;

        while (!queue.isEmpty()) {
            int course = queue.poll();
            completedCourses++;

            // Remove this course and update in-degrees
            for (int nextCourse : graph.get(course)) {
                inDegree[nextCourse]--;
                if (inDegree[nextCourse] == 0) {
                    queue.offer(nextCourse);
                }
            }
        }

        return completedCourses == numCourses;
    }

}

// SOLUTION 3: Union-Find with Cycle Detection
class SolutionUnionFind {

    public boolean canFinish(int numCourses, int[][] prerequisites) {
        // For Union-Find cycle detection, we need to process edges carefully
        // We'll use a different approach: detect back edges during DFS

        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] prereq : prerequisites) {
            graph.get(prereq[1]).add(prereq[0]);
        }

        // Use colors: 0=white, 1=gray, 2=black
        int[] color = new int[numCourses];

        for (int i = 0; i < numCourses; i++) {
            if (color[i] == 0 && dfsHasCycle(graph, i, color)) {
                return false;
            }
        }

        return true;
    }

    private boolean dfsHasCycle(List<List<Integer>> graph, int node, int[] color) {
        color[node] = 1; // Gray - currently being processed

        for (int neighbor : graph.get(node)) {
            if (color[neighbor] == 1)
                return true; // Back edge - cycle found
            if (color[neighbor] == 0 && dfsHasCycle(graph, neighbor, color)) {
                return true;
            }
        }

        color[node] = 2; // Black - completely processed
        return false;
    }

}

// SOLUTION 4: Optimized DFS with Early Termination
class SolutionOptimized {

    public boolean canFinish(int numCourses, int[][] prerequisites) {
        if (prerequisites.length == 0)
            return true;

        // Build adjacency list
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) {
            adj.add(new ArrayList<>());
        }

        for (int[] prereq : prerequisites) {
            adj.get(prereq[1]).add(prereq[0]);
        }

        // 0: unvisited, 1: visiting, 2: visited
        int[] visited = new int[numCourses];

        for (int i = 0; i < numCourses; i++) {
            if (visited[i] == 0) {
                if (isCyclic(adj, i, visited)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isCyclic(List<List<Integer>> adj, int node, int[] visited) {
        visited[node] = 1; // Mark as visiting

        for (int neighbor : adj.get(node)) {
            if (visited[neighbor] == 1) {
                return true; // Cycle detected
            }
            if (visited[neighbor] == 0 && isCyclic(adj, neighbor, visited)) {
                return true;
            }
        }

        visited[node] = 2; // Mark as visited
        return false;
    }

}

// SOLUTION 5: BFS with Detailed Tracking
class SolutionBFSDetailed {

    public boolean canFinish(int numCourses, int[][] prerequisites) {
        // Create adjacency list and in-degree array
        List<Integer>[] graph = new List[numCourses];
        int[] inDegree = new int[numCourses];

        for (int i = 0; i < numCourses; i++) {
            graph[i] = new ArrayList<>();
        }

        // Build graph and calculate in-degrees
        for (int[] edge : prerequisites) {
            int to = edge[0];
            int from = edge[1];
            graph[from].add(to);
            inDegree[to]++;
        }

        // Initialize queue with nodes having no incoming edges
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < numCourses; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }

        int processedNodes = 0;

        while (!queue.isEmpty()) {
            int current = queue.poll();
            processedNodes++;

            // Process all neighbors
            for (int neighbor : graph[current]) {
                inDegree[neighbor]--;
                if (inDegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // If we processed all nodes, there's no cycle
        return processedNodes == numCourses;
    }

}

/*
 * ALGORITHM COMPARISON:
 * 
 * 1. DFS APPROACH (Solution 1):
 * - Uses three states: unvisited, visiting, visited
 * - Detects back edges which indicate cycles
 * - Time: O(V + E), Space: O(V)
 * - Most intuitive for cycle detection
 * 
 * 2. BFS/KAHN'S ALGORITHM (Solution 2):
 * - Uses topological sorting approach
 * - Starts with nodes having no incoming edges
 * - Time: O(V + E), Space: O(V)
 * - Natural for scheduling problems
 * 
 * 3. UNION-FIND APPROACH:
 * - Not ideal for directed graphs with cycles
 * - Better suited for undirected graph cycle detection
 * - Modified to use DFS for directed graphs
 * 
 * DETAILED EXAMPLE WALKTHROUGH:
 * Input: numCourses = 4, prerequisites = [[1,0],[2,0],[3,1],[3,2]]
 * 
 * DFS APPROACH:
 * 1. Build adjacency list:
 * 0 -> [1, 2]
 * 1 -> [3]
 * 2 -> [3]
 * 3 -> []
 * 
 * 2. DFS from each unvisited node:
 * - Start DFS(0): 0->1->3 (mark all as visited), 0->2->3 (3 already visited)
 * - All nodes processed without finding back edges
 * - Return true
 * 
 * BFS APPROACH:
 * 1. Calculate in-degrees: [0, 1, 1, 2]
 * 2. Start with nodes having in-degree 0: queue = [0]
 * 3. Process 0: remove edges 0->1, 0->2, update in-degrees to [0, 0, 0, 2],
 * queue = [1, 2]
 * 4. Process 1: remove edge 1->3, update in-degrees to [0, 0, 0, 1], queue =
 * [2]
 * 5. Process 2: remove edge 2->3, update in-degrees to [0, 0, 0, 0], queue =
 * [3]
 * 6. Process 3: no outgoing edges, queue = []
 * 7. Processed 4 nodes = numCourses, return true
 * 
 * TIME COMPLEXITY: O(V + E) where V = numCourses, E = prerequisites.length
 * SPACE COMPLEXITY: O(V + E) for adjacency list and auxiliary arrays
 * 
 * WHEN TO USE EACH:
 * - DFS: When you want to understand the cycle detection mechanism
 * - BFS/Kahn's: When you need the actual topological order or for scheduling
 * - Both are equally efficient for just detecting if completion is possible
 */
