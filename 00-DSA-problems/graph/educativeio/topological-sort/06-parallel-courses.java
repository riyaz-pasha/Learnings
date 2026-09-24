import java.util.*;
import java.util.stream.IntStream;

/**
 * ============================================================================
 * PARALLEL COURSES (LeetCode 1136)
 * ============================================================================
 * 
 * STATEMENT:
 * You are designing a course schedule for a university with n courses, labeled 
 * from 1 to n. You are given a prerequisites array, relations, where 
 * relations[i] = [prevCourse_i, nextCourse_i] means prevCourse must be completed 
 * before nextCourse.
 * You can take any number of courses during a single semester if prerequisites are met.
 * Return the minimum number of semesters required to complete all courses.
 * If a cycle exists, return -1.
 * 
 * ----------------------------------------------------------------------------
 * RESTATING THE PROBLEM:
 * We need to find the shortest time to complete all interconnected tasks, where 
 * independent tasks can be done in parallel. 
 * Representing this as a Directed Graph:
 * - Nodes are courses.
 * - Edges are prerequisite requirements (prev -> next).
 * 
 * The problem is equivalent to finding the "depth" of a Directed Acyclic Graph 
 * (DAG). 
 * If the graph has a cycle, no valid schedule exists, and we must return -1.
 * 
 * ----------------------------------------------------------------------------
 * CLARIFYING QUESTIONS FOR THE INTERVIEWER (4-10):
 * 1. Q: What should be returned if there are no relations given?
 *    A: All courses can be taken in parallel during the first semester, so return 1.
 * 2. Q: Are the course numbers 0-indexed or 1-indexed?
 *    A: The problem specifies 1 to n, so they are 1-indexed.
 * 3. Q: Is there a limit to how many courses I can take in one semester?
 *    A: No, you can take an unlimited number as long as prerequisites are met.
 * 4. Q: Can there be isolated courses with no prerequisites?
 *    A: Yes. These can be taken immediately in Semester 1.
 * 5. Q: Is it possible for the input to contain duplicate relation pairs?
 *    A: The constraints guarantee all pairs are unique.
 * 
 * ----------------------------------------------------------------------------
 * HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * 1. Acknowledge the core concept: "This is a dependency resolution problem where 
 *    parallel execution is allowed. This perfectly maps to finding the depth of 
 *    a DAG using Topological Sort."
 * 2. Discuss BFS vs DFS:
 *    - BFS (Kahn's Algorithm) is the most intuitive. We process the graph level 
 *      by level. Each level processed equals one semester.
 *    - DFS can also solve this by finding the longest path (max depth) from 
 *      any node. We can combine longest-path memoization with a 3-color cycle 
 *      detection in a single pass.
 * 3. Propose Time & Space Complexity: 
 *    - Both approaches take O(V + E) time and O(V + E) space.
 * 4. Ask the interviewer which approach they prefer. Provide BFS first for 
 *    intuitive "level-by-level" clarity.
 * 
 * ============================================================================
 */
public class ParallelCourses {

    /**
     * ========================================================================
     * SOLUTION 1: KAHN'S ALGORITHM (BFS / Level-by-Level Topological Sort)
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * In-degree represents the number of prerequisites a course currently has.
     * Courses with an in-degree of 0 have no pending prerequisites, meaning 
     * they can be taken THIS semester.
     * 
     * We load all 0 in-degree courses into a queue. As we process them, we 
     * "complete" them by decrementing the in-degree of courses that depend on them. 
     * If a dependent course's in-degree drops to 0, it becomes available for 
     * the NEXT semester. By processing the queue size by size, we accurately 
     * group courses into discrete "semesters".
     * 
     * KEY OBSERVATIONS:
     * - We track how many total courses we take. If we finish the BFS and haven't 
     *   taken all 'n' courses, some are trapped in a cycle.
     * 
     * VISUAL TRACING:
     * n = 3, relations = [[1,3], [2,3]]
     * 
     * Graph:
     * [1] ---> [3]
     * [2] ---> [3]
     * 
     * In-Degrees: [1]=0, [2]=0, [3]=2
     * 
     * Semester 1:
     * - Queue: [1, 2] -> Take both!
     * - Decrement neighbors of 1 -> In-Degree[3] = 1
     * - Decrement neighbors of 2 -> In-Degree[3] = 0 -> Add to Queue
     * 
     * Semester 2:
     * - Queue: [3] -> Take it!
     * 
     * Total Courses Taken: 3 == n. Total Semesters: 2. Return 2.
     */
    public int minimumSemestersBFS(int n, int[][] relations) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            adj.add(new ArrayList<>());
        }
        
        int[] inDegree = new int[n + 1];
        
        // Build graph
        for (int[] relation : relations) {
            int prev = relation[0];
            int next = relation[1];
            adj.get(prev).add(next);
            inDegree[next]++;
        }
        
        Queue<Integer> queue = new LinkedList<>();
        // Enqueue courses that can be taken in the first semester (no prerequisites)
        for (int i = 1; i <= n; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }
        
        int semesters = 0;
        int coursesTaken = 0;
        
        // Process level by level
        while (!queue.isEmpty()) {
            semesters++;
            int size = queue.size();
            
            // Iterate over all courses in the current semester
            for (int i = 0; i < size; i++) {
                int currentCourse = queue.poll();
                coursesTaken++;
                
                // Unlock dependent courses
                for (int nextCourse : adj.get(currentCourse)) {
                    inDegree[nextCourse]--;
                    if (inDegree[nextCourse] == 0) {
                        queue.offer(nextCourse); // Ready for next semester
                    }
                }
            }
        }
        
        return coursesTaken == n ? semesters : -1;
    }

    /**
     * ========================================================================
     * SOLUTION 2: DFS WITH MEMOIZATION & CYCLE DETECTION
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * If we look at this problem from a single node's perspective, the minimum 
     * number of semesters to complete that node and everything it depends on 
     * is equal to the "Longest Path" starting from that node. 
     * 
     * We can use a single array `depth` to handle BOTH memoization and cycle detection:
     * - depth[i] == 0  -> Unvisited
     * - depth[i] == -1 -> Visiting (If we see this again, we found a CYCLE!)
     * - depth[i] > 0   -> Visited. Holds the max path length from this node.
     * 
     * We find the max depth across all nodes. If any node detects a cycle, 
     * the whole process halts and returns -1.
     */
    public int minimumSemestersDFS(int n, int[][] relations) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            adj.add(new ArrayList<>());
        }
        
        for (int[] relation : relations) {
            adj.get(relation[0]).add(relation[1]);
        }
        
        // depth[] tracks both the visitation state AND the longest path answer
        int[] depth = new int[n + 1];
        int maxSemesters = 0;
        
        for (int i = 1; i <= n; i++) {
            int currentDepth = dfs(i, adj, depth);
            if (currentDepth == -1) {
                return -1; // Cycle found
            }
            maxSemesters = Math.max(maxSemesters, currentDepth);
        }
        
        return maxSemesters;
    }
    
    private int dfs(int node, List<List<Integer>> adj, int[] depth) {
        // If node is not 0, it means we've seen it.
        // It's either currently VISITING (-1 = cycle!) or ALREADY PROCESSED (returns max depth)
        if (depth[node] != 0) {
            return depth[node]; 
        }
        
        // Mark as VISITING
        depth[node] = -1;
        
        int maxLength = 1; // Base case: path length is at least 1 (the course itself)
        
        // Traverse dependencies
        for (int nextNode : adj.get(node)) {
            int nextDepth = dfs(nextNode, adj, depth);
            if (nextDepth == -1) {
                return -1; // Propagate cycle alert upwards
            }
            maxLength = Math.max(maxLength, 1 + nextDepth);
        }
        
        // Mark as VISITED by storing the maximum path length from this node
        depth[node] = maxLength;
        
        return maxLength;
    }

    /**
     * ========================================================================
     * MAIN METHOD & TESTING (Using modern Java Records & Streams)
     * ========================================================================
     */
    
    record TestCase(int n, int[][] relations, int expected, String description) {}

    public static void main(String[] args) {
        ParallelCourses solver = new ParallelCourses();

        var testCases = List.of(
            new TestCase(
                3, 
                new int[][]{{1, 3}, {2, 3}}, 
                2, 
                "Standard Parallel DAG"
            ),
            new TestCase(
                3, 
                new int[][]{{1, 2}, {2, 3}, {3, 1}}, 
                -1, 
                "Cycle (Triangle)"
            ),
            new TestCase(
                5, 
                new int[][]{{1, 5}, {2, 5}, {3, 5}, {4, 5}}, 
                2, 
                "Star Pattern (4 Prerequisites for 1 Course)"
            ),
            new TestCase(
                4, 
                new int[][]{}, 
                1, 
                "No Relations (All taken in Semester 1)"
            ),
            new TestCase(
                4, 
                new int[][]{{1, 2}, {2, 3}, {3, 4}}, 
                4, 
                "Linear Chain"
            )
        );

        System.out.println("Running test cases for Parallel Courses...");
        System.out.println("-".repeat(70));

        IntStream.range(0, testCases.size()).forEach(i -> {
            var tc = testCases.get(i);
            
            int resBFS = solver.minimumSemestersBFS(tc.n(), tc.relations());
            int resDFS = solver.minimumSemestersDFS(tc.n(), tc.relations());
            
            boolean bfsPassed = (resBFS == tc.expected());
            boolean dfsPassed = (resDFS == tc.expected());
            
            System.out.printf("Test Case %d: %s\n", i + 1, tc.description());
            System.out.printf("Expected: %d | BFS: %d | DFS: %d\n", tc.expected(), resBFS, resDFS);
            System.out.printf("BFS Status: %s | DFS Status: %s\n", 
                              bfsPassed ? "✅" : "❌", 
                              dfsPassed ? "✅" : "❌");
            System.out.println("-".repeat(70));
        });
    }
}
