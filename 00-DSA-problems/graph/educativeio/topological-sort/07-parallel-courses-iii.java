import java.util.*;
import java.util.stream.IntStream;

/**
 * ============================================================================
 * PARALLEL COURSES III (LeetCode 2050)
 * ============================================================================
 * 
 * STATEMENT:
 * You are tasked with determining the minimum time required to complete a set 
 * of 'n' courses, labeled from 1 to n. 
 * You are given a 2D array 'relations' where relations[j] = [prevCourse, nextCourse], 
 * meaning prevCourse must be completed before nextCourse.
 * You are also given an array 'time', where time[i] specifies the number of 
 * months required to complete the course labeled i + 1.
 * You can take multiple courses simultaneously if prerequisites are met.
 * Calculate the minimum months needed to finish all courses.
 * Note: The graph is guaranteed to be a Directed Acyclic Graph (DAG).
 * 
 * ----------------------------------------------------------------------------
 * RESTATING THE PROBLEM:
 * We need to find the "Critical Path" in a network of tasks. Since we can run 
 * tasks in parallel, the total time to finish all tasks isn't the sum of all 
 * task times, but rather the length of the *longest path* through the dependency 
 * graph. The longest path dictates the minimum time required because we cannot 
 * finish the final task until its longest chain of prerequisites is fully complete.
 * 
 * ----------------------------------------------------------------------------
 * CLARIFYING QUESTIONS FOR THE INTERVIEWER (4-10):
 * 1. Q: What if the 'relations' array is empty?
 *    A: Then all courses can be taken simultaneously, and the answer is simply 
 *       the maximum value in the 'time' array.
 * 2. Q: Are there multiple independent components (disconnected courses)?
 *    A: Yes, we must evaluate all courses, as the answer will be the maximum 
 *       completion time across any independent sub-graphs.
 * 3. Q: Is there a risk of a cycle in the prerequisites?
 *    A: No, the problem explicitly guarantees it will be a Directed Acyclic Graph.
 * 4. Q: Can the time for a course be zero?
 *    A: The constraints specify time[i] >= 1, so every course takes at least 1 month.
 * 5. Q: With 'n' up to 50,000, can the total time exceed a 32-bit integer?
 *    A: 50,000 courses taking 10,000 months sequentially would take 500,000,000 
 *       months, which easily fits within a standard signed 32-bit int (max ~2.1B).
 * 6. Q: Can the recursion depth of DFS cause a StackOverflow for n = 50,000?
 *    A: In Java, a deep chain of 50,000 nodes might trigger a StackOverflowError. 
 *       Therefore, an iterative BFS (Kahn's) is safer for production systems.
 * 
 * ----------------------------------------------------------------------------
 * HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * 1. Acknowledge the core concept aloud: "This is a Longest Path / Critical Path 
 *    problem on a Directed Acyclic Graph (DAG)."
 * 2. Compare with previous topological sort problems: "Unlike basic Topological Sort 
 *    where we just count levels (unweighted edges), here edges/nodes have weights. 
 *    We must maintain a tracking array of the maximum time taken to reach the end 
 *    of each course."
 * 3. Discuss BFS (Kahn's): 
 *    We propagate the maximum time from prerequisites to dependent courses.
 *    completionTime[next] = max(completionTime[next], completionTime[curr] + time[next])
 * 4. Discuss DFS + Memoization:
 *    The max time to complete a course is its own time plus the max time of all 
 *    its subsequent paths.
 * 5. Provide Time/Space complexity upfront (both are O(V + E)). Mention the Java 
 *    StackOverflow edge-case for DFS, demonstrating deep language expertise.
 * 
 * ============================================================================
 */
public class ParallelCoursesIII {

    /**
     * ========================================================================
     * SOLUTION 1: KAHN'S ALGORITHM (Iterative BFS - Recommended)
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * We keep an array `maxTime` where `maxTime[i]` stores the earliest possible 
     * month we can finish course `i`.
     * 
     * 1. If a course has no prerequisites, its earliest finish time is just its 
     *    own duration. We add it to our BFS queue.
     * 2. As we process a course, we "unlock" its dependents. Because a dependent 
     *    course cannot START until ALL its prerequisites FINISH, the dependent 
     *    course's finish time must be updated to the maximum incoming path.
     *    formula: maxTime[next] = Math.max(maxTime[next], maxTime[curr] + time[next-1])
     * 3. Once a dependent course's in-degree hits 0, ALL its prerequisites have 
     *    been evaluated, meaning its `maxTime` is completely accurate. We then 
     *    push it to the queue.
     * 
     * VISUAL TRACING & BREAKDOWN:
     * n = 3, relations = [[1,3], [2,3]], time = [3, 2, 5]
     * 
     * Graph:
     * [1] (takes 3) ----> [3] (takes 5)
     * [2] (takes 2) ----> [3]
     * 
     * Initial States:
     * In-Degrees: [1]=0, [2]=0, [3]=2
     * maxTime:    [0, 3, 2, 0] (Courses 1 and 2 only take their own time)
     * Queue:      [1, 2]
     * 
     * Step 1: Pop 1.
     * - Evaluate neighbor 3: maxTime[3] = max(0, maxTime[1] + time[3-1]) 
     *                                   = max(0, 3 + 5) = 8.
     * - Reduce In-Degree[3] to 1.
     * 
     * Step 2: Pop 2.
     * - Evaluate neighbor 3: maxTime[3] = max(8, maxTime[2] + time[3-1]) 
     *                                   = max(8, 2 + 5) = 8.
     * - Reduce In-Degree[3] to 0. Add 3 to Queue.
     * 
     * Step 3: Pop 3. No neighbors.
     * 
     * Result: The answer is the absolute maximum in maxTime -> 8.
     * 
     * COMPLEXITY:
     * Time Complexity: O(V + E) or O(n + relations.length). We process every node 
     * and every edge exactly once.
     * Space Complexity: O(V + E). Adjacency list takes O(V + E), inDegree and 
     * maxTime arrays take O(V), Queue takes O(V).
     */
    public int minimumTimeBFS(int n, int[][] relations, int[] time) {
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
        int[] maxTime = new int[n + 1]; // Tracks completion time for each course
        
        // Initialize base courses
        for (int i = 1; i <= n; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
                maxTime[i] = time[i - 1]; 
            }
        }
        
        // Process graph
        while (!queue.isEmpty()) {
            int currentCourse = queue.poll();
            
            for (int nextCourse : adj.get(currentCourse)) {
                // The time to complete the next course is the max of its current 
                // calculated time, OR the time it takes for this prerequisite path 
                // to finish + the next course's own time.
                maxTime[nextCourse] = Math.max(
                    maxTime[nextCourse], 
                    maxTime[currentCourse] + time[nextCourse - 1]
                );
                
                inDegree[nextCourse]--;
                if (inDegree[nextCourse] == 0) {
                    queue.offer(nextCourse);
                }
            }
        }
        
        // The answer is the time taken by the course that finishes absolutely last
        int totalMinimumMonths = 0;
        for (int t : maxTime) {
            totalMinimumMonths = Math.max(totalMinimumMonths, t);
        }
        
        return totalMinimumMonths;
    }

    /**
     * ========================================================================
     * SOLUTION 2: DFS WITH MEMOIZATION (Longest Path Extraction)
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * Instead of pushing times forward like in BFS, we can pull times backward 
     * using DFS. 
     * The total time required from any specific course `u` to the very end of 
     * its dependency chain is:
     * dfs(u) = time[u] + MAX(dfs(v) for all v that depend on u)
     * 
     * We calculate this recursively and cache (memoize) the results in an array. 
     * We query all nodes from 1 to n, and return the absolute maximum result.
     * 
     * COMPLEXITY:
     * Time Complexity: O(V + E) - Each node and its edges are computed exactly once.
     * Space Complexity: O(V + E) - For graph representation + O(V) for Memoization 
     * array + O(V) for the Call Stack. (Note: May StackOverflow on massive chains).
     */
    public int minimumTimeDFS(int n, int[][] relations, int[] time) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            adj.add(new ArrayList<>());
        }
        
        for (int[] relation : relations) {
            adj.get(relation[0]).add(relation[1]);
        }
        
        int[] memo = new int[n + 1];
        int maxMonths = 0;
        
        // Check Longest Path starting from every node (memoization prevents repeated work)
        for (int i = 1; i <= n; i++) {
            maxMonths = Math.max(maxMonths, dfs(i, adj, time, memo));
        }
        
        return maxMonths;
    }
    
    private int dfs(int node, List<List<Integer>> adj, int[] time, int[] memo) {
        // Return cached result if already computed
        if (memo[node] != 0) {
            return memo[node];
        }
        
        int maxChildPath = 0;
        
        // Find the longest path among all dependents
        for (int nextNode : adj.get(node)) {
            maxChildPath = Math.max(maxChildPath, dfs(nextNode, adj, time, memo));
        }
        
        // The total time from this node onward is its own time + the longest subsequent path
        memo[node] = time[node - 1] + maxChildPath;
        
        return memo[node];
    }

    /**
     * ========================================================================
     * MAIN METHOD & TESTING (Using modern Java Records & Streams)
     * ========================================================================
     */
    
    record TestCase(int n, int[][] relations, int[] time, int expected, String description) {}

    public static void main(String[] args) {
        ParallelCoursesIII solver = new ParallelCoursesIII();

        var testCases = List.of(
            new TestCase(
                3, 
                new int[][]{{1, 3}, {2, 3}}, 
                new int[]{3, 2, 5},
                8, 
                "Standard Parallel Merge (1 and 2 -> 3)"
            ),
            new TestCase(
                5, 
                new int[][]{{1, 5}, {2, 5}, {3, 5}, {3, 4}, {4, 5}}, 
                new int[]{1, 2, 3, 4, 5},
                12, 
                "Complex branching with critical path (3 -> 4 -> 5)"
            ),
            new TestCase(
                4, 
                new int[][]{}, 
                new int[]{10, 20, 30, 40},
                40, 
                "No Dependencies (All taken simultaneously)"
            ),
            new TestCase(
                4, 
                new int[][]{{1, 2}, {2, 3}, {3, 4}}, 
                new int[]{1, 1, 1, 1},
                4, 
                "Strict Sequential Chain"
            )
        );

        System.out.println("Running test cases for Parallel Courses III...");
        System.out.println("-".repeat(75));

        IntStream.range(0, testCases.size()).forEach(i -> {
            var tc = testCases.get(i);
            
            int resBFS = solver.minimumTimeBFS(tc.n(), tc.relations(), tc.time());
            int resDFS = solver.minimumTimeDFS(tc.n(), tc.relations(), tc.time());
            
            boolean bfsPassed = (resBFS == tc.expected());
            boolean dfsPassed = (resDFS == tc.expected());
            
            System.out.printf("Test Case %d: %s\n", i + 1, tc.description());
            System.out.printf("Expected: %d | BFS Result: %d | DFS Result: %d\n", 
                              tc.expected(), resBFS, resDFS);
            System.out.printf("BFS Status: %s | DFS Status: %s\n", 
                              bfsPassed ? "✅ PASSED" : "❌ FAILED", 
                              dfsPassed ? "✅ PASSED" : "❌ FAILED");
            System.out.println("-".repeat(75));
        });
    }
}
