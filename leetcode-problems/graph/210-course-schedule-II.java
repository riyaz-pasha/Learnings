import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;
/*
 * There are a total of numCourses courses you have to take, labeled from 0 to
 * numCourses - 1. You are given an array prerequisites where prerequisites[i] =
 * [ai, bi] indicates that you must take course bi first if you want to take
 * course ai.
 * 
 * For example, the pair [0, 1], indicates that to take course 0 you have to
 * first take course 1.
 * Return the ordering of courses you should take to finish all courses. If
 * there are many valid answers, return any of them. If it is impossible to
 * finish all courses, return an empty array.
 * 
 * Example 1:
 * Input: numCourses = 2, prerequisites = [[1,0]]
 * Output: [0,1]
 * Explanation: There are a total of 2 courses to take. To take course 1 you
 * should have finished course 0. So the correct course order is [0,1].
 * 
 * Example 2:
 * Input: numCourses = 4, prerequisites = [[1,0],[2,0],[3,1],[3,2]]
 * Output: [0,2,1,3]
 * Explanation: There are a total of 4 courses to take. To take course 3 you
 * should have finished both courses 1 and 2. Both courses 1 and 2 should be
 * taken after you finished course 0.
 * So one correct course order is [0,1,2,3]. Another correct ordering is
 * [0,2,1,3].
 * 
 * Example 3:
 * Input: numCourses = 1, prerequisites = []
 * Output: [0]
 */

// Time Complexity: O(V + E) where V = numCourses, E = number of prerequisites
// Space Complexity: O(V + E) for adjacency list and auxiliary data structures
// SOLUTION 1: BFS - Kahn's Algorithm (Most Intuitive for Topological Sort)
class Solution {

    public int[] findOrder(int numCourses, int[][] prerequisites) {
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

        int[] result = new int[numCourses];
        int index = 0;

        while (!queue.isEmpty()) {
            int course = queue.poll();
            result[index++] = course;

            // Remove this course and update in-degrees
            for (int nextCourse : graph.get(course)) {
                inDegree[nextCourse]--;
                if (inDegree[nextCourse] == 0) {
                    queue.offer(nextCourse);
                }
            }
        }

        // If we couldn't process all courses, there's a cycle
        return index == numCourses ? result : new int[0];
    }

}

// SOLUTION 2: DFS with Post-order Traversal (Reverse Topological Sort)
class SolutionDFS {

    public int[] findOrder(int numCourses, int[][] prerequisites) {
        // Build adjacency list
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] prereq : prerequisites) {
            graph.get(prereq[1]).add(prereq[0]);
        }

        // DFS with three states: 0=unvisited, 1=visiting, 2=visited
        int[] state = new int[numCourses];
        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < numCourses; i++) {
            if (state[i] == 0 && hasCycle(graph, i, state, result)) {
                return new int[0]; // Cycle detected
            }
        }

        // Reverse the result since DFS gives reverse topological order
        Collections.reverse(result);
        return result.stream().mapToInt(i -> i).toArray();
    }

    private boolean hasCycle(List<List<Integer>> graph, int course, int[] state, List<Integer> result) {
        if (state[course] == 1)
            return true; // Back edge - cycle detected
        if (state[course] == 2)
            return false; // Already processed

        state[course] = 1; // Mark as visiting

        for (int nextCourse : graph.get(course)) {
            if (hasCycle(graph, nextCourse, state, result)) {
                return true;
            }
        }

        state[course] = 2; // Mark as visited
        result.add(course); // Add to result in post-order
        return false;
    }

}

// SOLUTION 3: DFS with Stack (Iterative Approach)
class SolutionDFSIterative {

    public int[] findOrder(int numCourses, int[][] prerequisites) {
        // Build adjacency list
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] prereq : prerequisites) {
            graph.get(prereq[1]).add(prereq[0]);
        }

        int[] state = new int[numCourses]; // 0=unvisited, 1=visiting, 2=visited
        Stack<Integer> result = new Stack<>();

        for (int i = 0; i < numCourses; i++) {
            if (state[i] == 0) {
                if (hasCycleIterative(graph, i, state, result)) {
                    return new int[0];
                }
            }
        }

        // Convert stack to array in reverse order
        int[] order = new int[numCourses];
        for (int i = 0; i < numCourses; i++) {
            order[i] = result.pop();
        }

        return order;
    }

    private boolean hasCycleIterative(List<List<Integer>> graph, int start, int[] state, Stack<Integer> result) {
        Stack<Integer> stack = new Stack<>();
        stack.push(start);

        while (!stack.isEmpty()) {
            int course = stack.peek();

            if (state[course] == 1)
                return true; // Cycle detected
            if (state[course] == 2) {
                stack.pop();
                continue;
            }

            state[course] = 1; // Mark as visiting

            boolean hasUnvisitedNeighbor = false;
            for (int neighbor : graph.get(course)) {
                if (state[neighbor] == 1)
                    return true; // Back edge
                if (state[neighbor] == 0) {
                    stack.push(neighbor);
                    hasUnvisitedNeighbor = true;
                }
            }

            if (!hasUnvisitedNeighbor) {
                state[course] = 2; // Mark as visited
                result.push(stack.pop());
            }
        }

        return false;
    }

}

// SOLUTION 4: Optimized BFS with Array-based Queue
class SolutionOptimized {

    public int[] findOrder(int numCourses, int[][] prerequisites) {
        // Use arrays instead of lists for better performance
        int[][] graph = new int[numCourses][];
        int[] outDegree = new int[numCourses];
        int[] inDegree = new int[numCourses];

        // Count out-degrees to initialize arrays
        for (int[] prereq : prerequisites) {
            outDegree[prereq[1]]++;
            inDegree[prereq[0]]++;
        }

        // Initialize adjacency arrays
        for (int i = 0; i < numCourses; i++) {
            graph[i] = new int[outDegree[i]];
        }

        // Build the graph
        int[] indices = new int[numCourses];
        for (int[] prereq : prerequisites) {
            int prerequisite = prereq[1];
            int course = prereq[0];
            graph[prerequisite][indices[prerequisite]++] = course;
        }

        // BFS using array-based queue
        int[] queue = new int[numCourses];
        int front = 0, rear = 0;

        for (int i = 0; i < numCourses; i++) {
            if (inDegree[i] == 0) {
                queue[rear++] = i;
            }
        }

        int[] result = new int[numCourses];
        int index = 0;

        while (front < rear) {
            int course = queue[front++];
            result[index++] = course;

            for (int nextCourse : graph[course]) {
                inDegree[nextCourse]--;
                if (inDegree[nextCourse] == 0) {
                    queue[rear++] = nextCourse;
                }
            }
        }

        return index == numCourses ? result : new int[0];
    }

}

// SOLUTION 5: BFS with Priority Queue (Lexicographically Smallest Order)
class SolutionPriorityQueue {

    public int[] findOrder(int numCourses, int[][] prerequisites) {
        // Build adjacency list and calculate in-degrees
        List<List<Integer>> graph = new ArrayList<>();
        int[] inDegree = new int[numCourses];

        for (int i = 0; i < numCourses; i++) {
            graph.add(new ArrayList<>());
        }

        for (int[] prereq : prerequisites) {
            graph.get(prereq[1]).add(prereq[0]);
            inDegree[prereq[0]]++;
        }

        // Use PriorityQueue to get lexicographically smallest order
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        for (int i = 0; i < numCourses; i++) {
            if (inDegree[i] == 0) {
                pq.offer(i);
            }
        }

        int[] result = new int[numCourses];
        int index = 0;

        while (!pq.isEmpty()) {
            int course = pq.poll();
            result[index++] = course;

            for (int nextCourse : graph.get(course)) {
                inDegree[nextCourse]--;
                if (inDegree[nextCourse] == 0) {
                    pq.offer(nextCourse);
                }
            }
        }

        return index == numCourses ? result : new int[0];
    }

}

/*
 * DETAILED ALGORITHM WALKTHROUGH:
 * Input: numCourses = 4, prerequisites = [[1,0],[2,0],[3,1],[3,2]]
 * 
 * BFS APPROACH (Kahn's Algorithm):
 * 
 * STEP 1: Build Graph and Calculate In-degrees
 * Graph (prerequisite -> course):
 * 0 -> [1, 2]
 * 1 -> [3]
 * 2 -> [3]
 * 3 -> []
 * 
 * In-degrees (number of prerequisites):
 * Course 0: 0 prerequisites
 * Course 1: 1 prerequisite (course 0)
 * Course 2: 1 prerequisite (course 0)
 * Course 3: 2 prerequisites (courses 1 and 2)
 * inDegree = [0, 1, 1, 2]
 * 
 * STEP 2: Initialize Queue with Zero In-degree Nodes
 * queue = [0] (only course 0 has no prerequisites)
 * 
 * STEP 3: Process Courses in Topological Order
 * Iteration 1:
 * - Remove course 0 from queue
 * - Add 0 to result: result = [0]
 * - Process neighbors of 0: courses 1 and 2
 * - Decrease inDegree[1] from 1 to 0, add 1 to queue
 * - Decrease inDegree[2] from 1 to 0, add 2 to queue
 * - queue = [1, 2], inDegree = [0, 0, 0, 2]
 * 
 * Iteration 2:
 * - Remove course 1 from queue
 * - Add 1 to result: result = [0, 1]
 * - Process neighbor of 1: course 3
 * - Decrease inDegree[3] from 2 to 1
 * - queue = [2], inDegree = [0, 0, 0, 1]
 * 
 * Iteration 3:
 * - Remove course 2 from queue
 * - Add 2 to result: result = [0, 1, 2]
 * - Process neighbor of 2: course 3
 * - Decrease inDegree[3] from 1 to 0, add 3 to queue
 * - queue = [3], inDegree = [0, 0, 0, 0]
 * 
 * Iteration 4:
 * - Remove course 3 from queue
 * - Add 3 to result: result = [0, 1, 2, 3]
 * - No neighbors to process
 * - queue = [], inDegree = [0, 0, 0, 0]
 * 
 * STEP 4: Verification
 * - Processed 4 courses = numCourses
 * - Return result = [0, 1, 2, 3]
 * 
 * DFS APPROACH:
 * Uses post-order traversal to get reverse topological order, then reverses the
 * result.
 * 
 * COMPLEXITY ANALYSIS:
 * 
 * BFS (Kahn's Algorithm):
 * - Time Complexity: O(V + E) where V = numCourses, E = prerequisites.length
 * - Space Complexity: O(V + E) for adjacency list and auxiliary arrays
 * - Best for: Getting actual topological order, intuitive understanding
 * 
 * DFS Approach:
 * - Time Complexity: O(V + E)
 * - Space Complexity: O(V + E) for adjacency list and recursion stack
 * - Best for: Understanding graph structure, when you need to detect cycles
 * 
 * WHEN TO USE EACH:
 * 1. BFS/Kahn's Algorithm: When you need the actual ordering (most common)
 * 2. DFS: When you need to understand the dependency structure
 * 3. Priority Queue variant: When you need lexicographically smallest order
 * 4. Optimized version: When performance is critical and input is large
 * 
 * KEY INSIGHTS:
 * - Topological sort is only possible in DAGs (Directed Acyclic Graphs)
 * - Multiple valid orderings may exist
 * - The problem is equivalent to finding any valid topological ordering
 * - If cycle exists, no valid ordering is possible
 */
