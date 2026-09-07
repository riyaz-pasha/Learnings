import java.util.*;
import java.util.stream.IntStream;

/**
 * ============================================================================
 * COURSE SCHEDULE II (LeetCode 210)
 * ============================================================================
 * 
 * STATEMENT:
 * You are given 'n' courses, labeled from 0 to n - 1. Some courses have 
 * prerequisites: prerequisites[i] = [a, b] means course 'b' must be completed 
 * before course 'a'. 
 * Return a valid order to finish all courses. If impossible, return an empty array.
 * 
 * ----------------------------------------------------------------------------
 * RESTATING THE PROBLEM:
 * We are given a list of directed dependencies between tasks. We need to 
 * find a linear ordering of these tasks such that every task appears after 
 * all of its dependencies. In graph theory, this is called a "Topological Sort" 
 * of a Directed Acyclic Graph (DAG). If the graph contains a cycle (a circular 
 * dependency), no such ordering is possible, and we should return an empty array.
 * 
 * ----------------------------------------------------------------------------
 * CLARIFYING QUESTIONS FOR THE INTERVIEWER:
 * 1. Q: What should I return if there are multiple valid course orders?
 *    A: The problem states any valid order is acceptable.
 * 2. Q: What if 'n' (number of courses) is 1 and there are no prerequisites?
 *    A: Return [0]. Single disconnected nodes are valid.
 * 3. Q: Can there be courses with no prerequisites at all?
 *    A: Yes, they can be taken at any time and should usually appear early in the order.
 * 4. Q: What if the prerequisites array is entirely empty?
 *    A: Then any permutation of 0 to n-1 is a valid order. (e.g., [0, 1, 2, ... n-1]).
 * 5. Q: Are we guaranteed that the course labels strictly go from 0 to n-1?
 *    A: Yes, which allows us to use array indices directly for our graph representations.
 * 
 * ----------------------------------------------------------------------------
 * HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * 1. Identify the concept aloud: "This is a Topological Sorting problem on a directed graph."
 * 2. Define the Graph: "Nodes represent courses, and directed edges (b -> a) 
 *    represent the prerequisite requirement."
 * 3. Discuss the two standard algorithms for Topological Sort:
 *    a) Kahn's Algorithm (BFS): Count in-degrees (dependencies). Start with courses 
 *       having 0 in-degrees. As you take them, remove their outgoing edges.
 *    b) Depth-First Search (DFS): Explore paths fully. Use a 3-color state system 
 *       to detect cycles. Append to the result array in reverse post-order.
 * 4. Mention Time/Space Complexity: Both algorithms are O(V + E) where V is 
 *    the number of courses and E is the number of prerequisites.
 * 5. Write clean, modular code.
 * 
 * ============================================================================
 */
public class CourseScheduleII {

    /**
     * ========================================================================
     * SOLUTION 1: KAHN'S ALGORITHM (BFS / In-Degree Counting)
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * A course's "in-degree" is the number of prerequisites it still needs.
     * If in-degree is 0, the course can be taken immediately.
     * When we take a course, we can cross it off the prerequisite lists of 
     * all courses that depend on it (meaning we decrease their in-degree by 1).
     * If a dependent course's in-degree hits 0, it joins the queue of ready courses.
     * 
     * KEY OBSERVATIONS:
     * - This naturally builds the correct order from front to back.
     * - If we finish our queue but haven't added all 'n' courses to our result, 
     *   it means the remaining courses are stuck in a cycle (none could reach 0 in-degree).
     * 
     * VISUAL TRACING:
     * n = 4, prerequisites = [[1,0], [2,0], [3,1], [3,2]]
     * 
     * Graph:
     *        -> [1] ->
     *       /         \
     *     [0]         [3]
     *       \         /
     *        -> [2] ->
     * 
     * In-degrees: [0]=0, [1]=1, [2]=1, [3]=2
     * 
     * 1. Queue: [0]. Take 0. Output: [0]. 
     *    Decrease in-degrees for 1 and 2. 
     *    New In-degrees: [1]=0, [2]=0. Queue: [1, 2].
     * 2. Queue: [1, 2]. Take 1. Output: [0, 1].
     *    Decrease in-degree for 3. 
     *    New In-degree: [3]=1. Queue: [2].
     * 3. Queue: [2]. Take 2. Output: [0, 1, 2].
     *    Decrease in-degree for 3.
     *    New In-degree: [3]=0. Queue: [3].
     * 4. Queue: [3]. Take 3. Output: [0, 1, 2, 3]. Queue empty.
     * Result length == n. Return [0, 1, 2, 3].
     */
    public int[] findOrderBFS(int numCourses, int[][] prerequisites) {
        var inDegree = new int[numCourses];
        List<List<Integer>> adj = new ArrayList<>();
        
        for (int i = 0; i < numCourses; i++) {
            adj.add(new ArrayList<>());
        }
        
        // Build graph. Edge is prereq -> course
        for (var pre : prerequisites) {
            int course = pre[0];
            int prereq = pre[1];
            adj.get(prereq).add(course);
            inDegree[course]++;
        }
        
        Queue<Integer> queue = new LinkedList<>();
        // Start with courses that have no prerequisites
        for (int i = 0; i < numCourses; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }
        
        var result = new int[numCourses];
        int index = 0;
        
        while (!queue.isEmpty()) {
            int curr = queue.poll();
            result[index++] = curr;
            
            // "Take" the current course, effectively reducing the dependency count for neighbors
            for (int nextCourse : adj.get(curr)) {
                inDegree[nextCourse]--;
                if (inDegree[nextCourse] == 0) {
                    queue.offer(nextCourse);
                }
            }
        }
        
        // If we couldn't take all courses, there was a cycle.
        if (index != numCourses) {
            return new int[0]; // Return empty array as per requirements
        }
        
        return result;
    }

    /**
     * ========================================================================
     * SOLUTION 2: DEPTH-FIRST SEARCH (DFS) WITH GRAPH COLORING
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * In DFS, we go as deep as possible down a prerequisite chain. The first 
     * course to finish its DFS is the one that has NO further dependencies 
     * (the ultimate prerequisite for that path). Thus, the course that finishes 
     * its DFS first should go LAST in our final topological order. 
     * We can place finished courses into an array starting from the very end.
     * 
     * To prevent infinite loops and detect cycles, we use a 3-state system:
     * State 0: Unvisited
     * State 1: Visiting (currently in our recursive call stack)
     * State 2: Visited (fully processed and verified cycle-free)
     * 
     * KEY OBSERVATIONS:
     * - Hitting a node in State 1 means we've looped back on ourselves -> CYCLE!
     * - We must loop through all 'n' nodes initially to ensure we don't miss 
     *   disconnected components.
     */
    public int[] findOrderDFS(int numCourses, int[][] prerequisites) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) {
            adj.add(new ArrayList<>());
        }
        for (int[] pre : prerequisites) {
            adj.get(pre[1]).add(pre[0]);
        }
        
        var states = new int[numCourses];
        var result = new int[numCourses];
        
        // Array of size 1 to pass the index by reference across recursive calls.
        // We fill the result array from back to front.
        int[] insertIndex = {numCourses - 1};
        
        for (int i = 0; i < numCourses; i++) {
            if (states[i] == 0) { // If unvisited
                if (hasCycle(i, adj, states, result, insertIndex)) {
                    return new int[0]; // Cycle found, impossible to finish
                }
            }
        }
        
        return result;
    }
    
    private boolean hasCycle(int node, List<List<Integer>> adj, int[] states, int[] result, int[] insertIndex) {
        if (states[node] == 1) return true;  // Cycle detected!
        if (states[node] == 2) return false; // Already processed, safe.
        
        states[node] = 1; // Mark as visiting
        
        for (int neighbor : adj.get(node)) {
            if (hasCycle(neighbor, adj, states, result, insertIndex)) {
                return true;
            }
        }
        
        states[node] = 2; // Mark as fully visited
        // Since this node has no unvisited dependents left, it goes to the end of the available spots
        result[insertIndex[0]--] = node;
        
        return false;
    }

    /**
     * ========================================================================
     * MAIN METHOD & TESTING
     * ========================================================================
     */
    
    record TestCase(int numCourses, int[][] prerequisites, boolean expectsCycle, String description) {}

    public static void main(String[] args) {
        CourseScheduleII solver = new CourseScheduleII();

        var testCases = List.of(
            new TestCase(
                2, 
                new int[][]{{1, 0}}, 
                false, 
                "Simple DAG (0 -> 1)"
            ),
            new TestCase(
                4, 
                new int[][]{{1, 0}, {2, 0}, {3, 1}, {3, 2}}, 
                false, 
                "Diamond Pattern DAG"
            ),
            new TestCase(
                2, 
                new int[][]{{1, 0}, {0, 1}}, 
                true, 
                "Simple Cycle (0 <-> 1)"
            ),
            new TestCase(
                3, 
                new int[][]{{1, 0}, {2, 1}, {1, 2}}, 
                true, 
                "Chain with Cycle at end"
            ),
            new TestCase(
                1, 
                new int[][]{}, 
                false, 
                "Single Course, No Prerequisites"
            )
        );

        System.out.println("Running test cases for Course Schedule II...");
        System.out.println("-".repeat(70));

        IntStream.range(0, testCases.size()).forEach(i -> {
            var tc = testCases.get(i);
            int[] resultBFS = solver.findOrderBFS(tc.numCourses(), tc.prerequisites());
            int[] resultDFS = solver.findOrderDFS(tc.numCourses(), tc.prerequisites());
            
            // If expectsCycle is true, the arrays should be empty.
            boolean bfsPassed = tc.expectsCycle() ? resultBFS.length == 0 : resultBFS.length == tc.numCourses();
            boolean dfsPassed = tc.expectsCycle() ? resultDFS.length == 0 : resultDFS.length == tc.numCourses();
            
            System.out.printf("Test Case %d: %s\n", i + 1, tc.description());
            System.out.printf("Expects empty (Cycle)? %b\n", tc.expectsCycle());
            System.out.printf("BFS Result: %s | Passed? %s\n", Arrays.toString(resultBFS), bfsPassed ? "✅" : "❌");
            System.out.printf("DFS Result: %s | Passed? %s\n", Arrays.toString(resultDFS), dfsPassed ? "✅" : "❌");
            System.out.println("-".repeat(70));
        });
    }
}
