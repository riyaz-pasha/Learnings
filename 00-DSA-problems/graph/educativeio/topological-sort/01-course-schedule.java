import java.util.*;
import java.util.stream.IntStream;

/**
 * ============================================================================
 * COURSE SCHEDULE (LeetCode 207)
 * ============================================================================
 * 
 * STATEMENT:
 * You are given an integer, numCourses, representing the total number of courses 
 * you need to complete, labeled from 0 to numCourses - 1.
 * You are also given a prerequisites array, where prerequisites[i] = [a[i], b[i]] 
 * indicates that you must take course b[i] first if you want to take the course a[i].
 * Return TRUE if all courses can be finished. Otherwise, return FALSE.
 * 
 * ----------------------------------------------------------------------------
 * RESTATING THE PROBLEM:
 * We are dealing with tasks that have dependencies. A dependency is just a directed 
 * relationship: Course B -> Course A. We need to find out if there are any circular 
 * dependencies (cycles). If there is a cycle (e.g., A needs B, and B needs A), 
 * it is impossible to complete the courses. 
 * Therefore, the problem boils down to: "Does this directed graph have a cycle?"
 * If it has a cycle -> return false. If it is a Directed Acyclic Graph (DAG) -> return true.
 * 
 * ----------------------------------------------------------------------------
 * CLARIFYING QUESTIONS FOR THE INTERVIEWER (4-10):
 * 1. Q: Can there be disconnected courses? (Courses with no prerequisites).
 *    A: Yes, these can be taken at any time.
 * 2. Q: What should be returned if numCourses is 0?
 *    A: (Usually, true, but constraints say numCourses >= 1, so we don't need to worry).
 * 3. Q: Can there be duplicate prerequisite pairs in the input?
 *    A: Constraints state all pairs are unique, but good to clarify.
 * 4. Q: Can a course be a prerequisite to itself? (e.g., [0, 0])
 *    A: This immediately creates a cycle. If possible in the input, our cycle 
 *       detection will naturally catch it.
 * 5. Q: Are there multiple disconnected components in the graph?
 *    A: Yes, we must ensure we check all nodes, not just start from node 0.
 * 6. Q: Does the course numbering strictly follow 0 to numCourses - 1?
 *    A: Yes. This allows us to use arrays instead of HashMaps for our graph, 
 *       optimizing performance.
 * 
 * ----------------------------------------------------------------------------
 * HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * 1. Acknowledge the graph nature of the problem immediately. Mention "Nodes 
 *    are courses, Directed Edges are prerequisite requirements."
 * 2. State your goal: "We are looking for cycle detection in a directed graph."
 * 3. Propose the two standard ways to solve this:
 *    a) Topological Sorting using Kahn's Algorithm (BFS) - count in-degrees.
 *    b) Depth First Search (DFS) with a 3-color state system (Unvisited, Visiting, Visited).
 * 4. Discuss Time and Space Complexities (both are O(V + E)).
 * 5. Ask the interviewer which approach they prefer you to implement. (Both are provided below).
 * 
 * ============================================================================
 */
public class CourseSchedule {

    /**
     * ========================================================================
     * SOLUTION 1: KAHN'S ALGORITHM (Breadth-First Search / Topological Sort)
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * Think of "in-degree" as the number of prerequisites a course has.
     * If a course has 0 in-degrees, it means it has no prerequisites, so we 
     * can take it immediately. 
     * Once we take a course, we can "remove" it from the graph. By removing it, 
     * the courses that depended on it now have 1 less prerequisite (their in-degree drops).
     * If any of those dependent courses drop to 0 in-degrees, we can now take them!
     * 
     * KEY OBSERVATIONS:
     * - We process courses in a queue (FIFO).
     * - If we can eventually process all `numCourses`, there are no cycles.
     * - If the queue becomes empty but we haven't processed all courses, a cycle 
     *   exists (courses are trapped waiting for each other, so none drop to 0 in-degree).
     * 
     * VISUAL TRACING & BREAKDOWN:
     * numCourses = 4, prerequisites = [[1,0], [2,1], [3,2]]
     * 
     * Graph:
     * [0] ---> [1] ---> [2] ---> [3]
     * 
     * In-Degrees:
     * 0: 0  (Can take!)
     * 1: 1
     * 2: 1
     * 3: 1
     * 
     * Steps:
     * 1. Queue: [0]. Take 0. Reduce neighbors. Node 1 in-degree becomes 0. Queue: [1].
     * 2. Queue: [1]. Take 1. Reduce neighbors. Node 2 in-degree becomes 0. Queue: [2].
     * 3. Queue: [2]. Take 2. Reduce neighbors. Node 3 in-degree becomes 0. Queue: [3].
     * 4. Queue: [3]. Take 3. 
     * Total taken = 4 == numCourses. Return TRUE.
     */
    public boolean canFinishBFS(int numCourses, int[][] prerequisites) {
        // Modern Java Feature: 'var' for local variable type inference
        var inDegree = new int[numCourses];
        
        // Using an Array of Lists for the Adjacency List.
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) {
            adj.add(new ArrayList<>());
        }
        
        // Build the graph and calculate in-degrees
        for (var pre : prerequisites) {
            int course = pre[0];
            int prerequisite = pre[1];
            adj.get(prerequisite).add(course);
            inDegree[course]++;
        }
        
        // Find all courses with 0 prerequisites to start
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < numCourses; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }
        
        int coursesTaken = 0;
        
        // Process the graph
        while (!queue.isEmpty()) {
            int currentCourse = queue.poll();
            coursesTaken++;
            
            // For every course that depends on the current course...
            for (int dependentCourse : adj.get(currentCourse)) {
                inDegree[dependentCourse]--; // Remove the prerequisite requirement
                if (inDegree[dependentCourse] == 0) {
                    queue.offer(dependentCourse); // It's ready to be taken!
                }
            }
        }
        
        // If we took all courses, there was no cycle.
        return coursesTaken == numCourses;
    }

    /**
     * ========================================================================
     * SOLUTION 2: DEPTH-FIRST SEARCH (Cycle Detection via Graph Coloring)
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * We can explore the graph path by path using DFS. To detect a cycle, we 
     * track the "state" of each node:
     * 0 = UNVISITED (We haven't looked at this course yet)
     * 1 = VISITING  (We are currently exploring this course's path)
     * 2 = VISITED   (We have fully explored this course and its dependents; it's safe)
     * 
     * If, during our DFS traversal, we encounter a node that is currently marked 
     * as VISITING (1), it means we've looped back to a node that is already in 
     * our current recursion stack. THAT IS A CYCLE!
     * 
     * VISUAL TRACING & BREAKDOWN:
     * numCourses = 3, prerequisites = [[1,0], [2,1], [1,2]]
     * 
     * Graph:
     * [0] ---> [1] ---> [2]
     *           ^        |
     *           |________|
     * 
     * Steps:
     * Start DFS at Node 0:
     * - Node 0 state -> 1 (VISITING).
     * - Go to neighbor Node 1:
     *   - Node 1 state -> 1 (VISITING).
     *   - Go to neighbor Node 2:
     *     - Node 2 state -> 1 (VISITING).
     *     - Go to neighbor Node 1:
     *       - Node 1 is already state 1 (VISITING)! CYCLE DETECTED! Return False.
     */
    public boolean canFinishDFS(int numCourses, int[][] prerequisites) {
        // States: 0 = Unvisited, 1 = Visiting, 2 = Visited
        int[] states = new int[numCourses];
        
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) {
            adj.add(new ArrayList<>());
        }
        
        for (int[] pre : prerequisites) {
            adj.get(pre[1]).add(pre[0]);
        }
        
        // Check every course (to handle disconnected graphs)
        for (int i = 0; i < numCourses; i++) {
            if (states[i] == 0) {
                // If hasCycle returns true, we can't finish the courses
                if (hasCycle(adj, states, i)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private boolean hasCycle(List<List<Integer>> adj, int[] states, int node) {
        // If we hit a node we are currently visiting, we found a cycle!
        if (states[node] == 1) return true;
        
        // If we hit a fully processed node, it's safe, no cycle here.
        if (states[node] == 2) return false;
        
        // Mark current node as VISITING
        states[node] = 1;
        
        // Traverse all neighbors
        for (int neighbor : adj.get(node)) {
            if (hasCycle(adj, states, neighbor)) {
                return true;
            }
        }
        
        // Mark current node as VISITED (safe)
        states[node] = 2;
        return false;
    }

    /**
     * ========================================================================
     * MAIN METHOD & TESTING
     * ========================================================================
     * Using modern Java 14+ Record feature to elegantly structure test cases.
     * Using Streams to execute and validate test cases elegantly.
     */
    
    // Record for holding test case data cleanly
    record TestCase(int numCourses, int[][] prerequisites, boolean expectedResult, String description) {}

    public static void main(String[] args) {
        CourseSchedule solver = new CourseSchedule();

        var testCases = List.of(
            new TestCase(
                2, 
                new int[][]{{1, 0}}, 
                true, 
                "Simple Valid DAG (0 -> 1)"
            ),
            new TestCase(
                2, 
                new int[][]{{1, 0}, {0, 1}}, 
                false, 
                "Simple Cycle (0 <-> 1)"
            ),
            new TestCase(
                4, 
                new int[][]{{1, 0}, {2, 1}, {3, 2}}, 
                true, 
                "Linear Chain (0 -> 1 -> 2 -> 3)"
            ),
            new TestCase(
                3, 
                new int[][]{{1, 0}, {2, 1}, {1, 2}}, 
                false, 
                "Chain with Cycle (0 -> 1 -> 2 -> 1)"
            ),
            new TestCase(
                5, 
                new int[][]{{1, 4}, {2, 4}, {3, 1}, {3, 2}}, 
                true, 
                "Complex Valid DAG with Multiple Branches"
            )
        );

        System.out.println("Running test cases...");
        System.out.println("-".repeat(50));

        // Using streams for elegant iteration and reporting
        IntStream.range(0, testCases.size()).forEach(i -> {
            var tc = testCases.get(i);
            boolean resultBFS = solver.canFinishBFS(tc.numCourses(), tc.prerequisites());
            boolean resultDFS = solver.canFinishDFS(tc.numCourses(), tc.prerequisites());
            
            boolean passed = (resultBFS == tc.expectedResult()) && (resultDFS == tc.expectedResult());
            
            System.out.printf("Test Case %d: %s\n", i + 1, tc.description());
            System.out.printf("Expected: %b | BFS Result: %b | DFS Result: %b\n", 
                              tc.expectedResult(), resultBFS, resultDFS);
            System.out.printf("Status: %s\n", passed ? "✅ PASSED" : "❌ FAILED");
            System.out.println("-".repeat(50));
        });
    }
}
