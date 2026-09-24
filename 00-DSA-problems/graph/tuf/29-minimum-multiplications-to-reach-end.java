/*
 * Given start, end, and an array arr of n numbers. At each step, the start is
 * multiplied by any number in the array and then a mod operation with 100000 is
 * done to get the new start.
 * 
 * Find the minimum steps in which the end can be achieved starting from the
 * start. If it is not possible to reach the end, then return -1.
 * 
 * Examples:
 * Input: arr = [2, 5, 7], start = 3, end = 30
 * Output: 2
 * Explanation:
 * Step 1: 3*2 = 6 % 100000 = 6
 * Step 2: 6*5 = 30 % 100000 = 30
 * Therefore, in minimum 2 multiplications, we reach the end number which is
 * treated as a destination node of a graph here.
 * 
 * Input: arr = [3, 4, 65], start = 7, end = 66175
 * Output: 4
 * Explanation:
 * Step 1: 7*3 = 21 % 100000 = 21
 * Step 2: 21*3 = 6 % 100000 = 63
 * Step 3: 63*65 = 4095 % 100000 = 4095
 * Step 4: 4095*65 = 266175 % 100000 = 66175
 * Therefore, in minimum 4 multiplications we reach the end number which is
 * treated as a destination node of a graph here.
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

class MinimumMultiplicationsToReachEnd1 {

    record State(int num, int steps) {
    }

    public int minimumMultiplications(int[] arr, int start, int end) {
        final int MOD = 100_000;
        if (start == end)
            return 0;

        boolean[] visited = new boolean[MOD];
        Queue<State> queue = new ArrayDeque<>();
        queue.offer(new State(start, 0));
        visited[start] = true;

        while (!queue.isEmpty()) {
            State cur = queue.poll();
            int num = cur.num();
            int steps = cur.steps();

            for (int m : arr) {
                int next = (int) ((num * 1L * m) % MOD);
                if (!visited[next]) {
                    if (next == end)
                        return steps + 1;
                    visited[next] = true;
                    queue.offer(new State(next, steps + 1));
                }
            }
        }

        return -1;
    }

    // quick test
    public static void main(String[] args) {
        MinimumMultiplicationsToReachEnd s = new MinimumMultiplicationsToReachEnd();
        System.out.println(s.minimumMultiplications(new int[] { 2, 5, 7 }, 3, 30)); // 2
        System.out.println(s.minimumMultiplications(new int[] { 3, 4, 65 }, 7, 66175)); // 4
    }

}

class MinimumMultiplicationsToReachEnd {

    private static final int MOD = 100000;

    // Solution 1: BFS (RECOMMENDED - Unweighted Shortest Path)
    // Time Complexity: O(MOD * n) where n is array length
    // Space Complexity: O(MOD)
    public int minimumMultiplications(int[] arr, int start, int end) {
        // Edge case: already at destination
        if (start == end) {
            return 0;
        }

        // BFS queue: {current_value, steps}
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { start, 0 });

        // Track visited values to avoid cycles
        boolean[] visited = new boolean[MOD];
        visited[start] = true;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int value = curr[0];
            int steps = curr[1];

            // Try multiplying by each number in array
            for (int multiplier : arr) {
                int newValue = (value * multiplier) % MOD;

                // Check if we reached the destination
                if (newValue == end) {
                    return steps + 1;
                }

                // If not visited, add to queue
                if (!visited[newValue]) {
                    visited[newValue] = true;
                    queue.offer(new int[] { newValue, steps + 1 });
                }
            }
        }

        // If we exhausted all possibilities without reaching end
        return -1;
    }

    // Solution 2: BFS with Level-Order Traversal (More Memory Efficient)
    // Time Complexity: O(MOD * n)
    // Space Complexity: O(MOD)
    public int minimumMultiplicationsLevelOrder(int[] arr, int start, int end) {
        if (start == end) {
            return 0;
        }

        Queue<Integer> queue = new LinkedList<>();
        queue.offer(start);

        boolean[] visited = new boolean[MOD];
        visited[start] = true;

        int steps = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();
            steps++;

            for (int i = 0; i < size; i++) {
                int value = queue.poll();

                for (int multiplier : arr) {
                    int newValue = (value * multiplier) % MOD;

                    if (newValue == end) {
                        return steps;
                    }

                    if (!visited[newValue]) {
                        visited[newValue] = true;
                        queue.offer(newValue);
                    }
                }
            }
        }

        return -1;
    }

    // Solution 3: Dijkstra's Algorithm (Overkill but educational)
    // Time Complexity: O(MOD * n * log(MOD))
    // Space Complexity: O(MOD)
    public int minimumMultiplicationsDijkstra(int[] arr, int start, int end) {
        if (start == end) {
            return 0;
        }

        // Distance array
        int[] dist = new int[MOD];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;

        // Priority queue: {steps, value}
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
        pq.offer(new int[] { 0, start });

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int steps = curr[0];
            int value = curr[1];

            if (value == end) {
                return steps;
            }

            if (steps > dist[value]) {
                continue;
            }

            for (int multiplier : arr) {
                int newValue = (value * multiplier) % MOD;
                int newSteps = steps + 1;

                if (newSteps < dist[newValue]) {
                    dist[newValue] = newSteps;
                    pq.offer(new int[] { newSteps, newValue });
                }
            }
        }

        return dist[end] == Integer.MAX_VALUE ? -1 : dist[end];
    }

    // Solution 4: Bidirectional BFS (Optimization for large search space)
    // Time Complexity: O(MOD * n)
    // Space Complexity: O(MOD)
    public int minimumMultiplicationsBidirectional(int[] arr, int start, int end) {
        if (start == end) {
            return 0;
        }

        // Forward search from start
        Set<Integer> visitedStart = new HashSet<>();
        Queue<Integer> queueStart = new LinkedList<>();
        visitedStart.add(start);
        queueStart.offer(start);

        // Backward search from end (precompute inverse multipliers if needed)
        Set<Integer> visitedEnd = new HashSet<>();
        Queue<Integer> queueEnd = new LinkedList<>();
        visitedEnd.add(end);
        queueEnd.offer(end);

        int steps = 0;

        while (!queueStart.isEmpty() || !queueEnd.isEmpty()) {
            steps++;

            // Expand from start
            int sizeStart = queueStart.size();
            for (int i = 0; i < sizeStart; i++) {
                int value = queueStart.poll();

                for (int multiplier : arr) {
                    int newValue = (value * multiplier) % MOD;

                    if (visitedEnd.contains(newValue)) {
                        return steps;
                    }

                    if (!visitedStart.contains(newValue)) {
                        visitedStart.add(newValue);
                        queueStart.offer(newValue);
                    }
                }
            }

            // Expand from end (going backwards)
            int sizeEnd = queueEnd.size();
            for (int i = 0; i < sizeEnd; i++) {
                int value = queueEnd.poll();

                for (int multiplier : arr) {
                    // Find values that when multiplied give current value
                    for (int candidate = 0; candidate < MOD; candidate++) {
                        if ((candidate * multiplier) % MOD == value) {
                            if (visitedStart.contains(candidate)) {
                                return steps;
                            }

                            if (!visitedEnd.contains(candidate)) {
                                visitedEnd.add(candidate);
                                queueEnd.offer(candidate);
                            }
                        }
                    }
                }
            }
        }

        return -1;
    }

    // Solution 5: DFS with Memoization (Not optimal but educational)
    // Time Complexity: O(MOD * n)
    // Space Complexity: O(MOD)
    public int minimumMultiplicationsDFS(int[] arr, int start, int end) {
        if (start == end) {
            return 0;
        }

        Integer[] memo = new Integer[MOD];
        int result = dfs(arr, start, end, memo, new boolean[MOD]);
        return result == Integer.MAX_VALUE ? -1 : result;
    }

    private int dfs(int[] arr, int current, int target, Integer[] memo, boolean[] visiting) {
        if (current == target) {
            return 0;
        }

        if (memo[current] != null) {
            return memo[current];
        }

        if (visiting[current]) {
            return Integer.MAX_VALUE; // Cycle detection
        }

        visiting[current] = true;
        int minSteps = Integer.MAX_VALUE;

        for (int multiplier : arr) {
            int newValue = (current * multiplier) % MOD;
            int steps = dfs(arr, newValue, target, memo, visiting);

            if (steps != Integer.MAX_VALUE) {
                minSteps = Math.min(minSteps, steps + 1);
            }
        }

        visiting[current] = false;
        memo[current] = minSteps;
        return minSteps;
    }

    // Helper method to visualize the search process
    public void visualizeSearchProcess(int[] arr, int start, int end) {
        if (start == end) {
            System.out.println("Already at destination!");
            return;
        }

        System.out.println("\n=== Search Process Visualization ===");
        System.out.println("Start: " + start + ", End: " + end);
        System.out.println("Multipliers: " + Arrays.toString(arr));
        System.out.println("\nSearch steps:");

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { start, 0, -1 }); // {value, steps, parent}

        boolean[] visited = new boolean[MOD];
        visited[start] = true;

        Map<Integer, int[]> parentMap = new HashMap<>();
        boolean found = false;

        while (!queue.isEmpty() && !found) {
            int size = queue.size();
            System.out.println("\nLevel " + (queue.peek()[1]) + ":");

            for (int i = 0; i < size && !found; i++) {
                int[] curr = queue.poll();
                int value = curr[0];
                int steps = curr[1];

                for (int multiplier : arr) {
                    int newValue = (value * multiplier) % MOD;

                    if (newValue == end) {
                        System.out.printf("  %d * %d = %d (FOUND!)\n", value, multiplier, newValue);
                        parentMap.put(newValue, new int[] { value, multiplier });
                        found = true;
                        reconstructPath(start, end, parentMap);
                        return;
                    }

                    if (!visited[newValue]) {
                        visited[newValue] = true;
                        queue.offer(new int[] { newValue, steps + 1, value });
                        parentMap.put(newValue, new int[] { value, multiplier });

                        if (steps < 2) { // Only print first few levels
                            System.out.printf("  %d * %d = %d\n", value, multiplier, newValue);
                        }
                    }
                }
            }
        }

        if (!found) {
            System.out.println("\nDestination unreachable!");
        }
    }

    private void reconstructPath(int start, int end, Map<Integer, int[]> parentMap) {
        List<String> path = new ArrayList<>();
        int current = end;

        while (current != start) {
            int[] parent = parentMap.get(current);
            path.add(0, parent[0] + " * " + parent[1] + " = " + current);
            current = parent[0];
        }

        System.out.println("\nPath reconstruction:");
        System.out.println("Start: " + start);
        for (String step : path) {
            System.out.println("Step: " + step);
        }
    }

    // Helper to analyze the graph structure
    public void analyzeGraph(int[] arr, int start, int end) {
        System.out.println("\n=== Graph Analysis ===");
        System.out.println("State space size: " + MOD + " possible values (0 to " + (MOD - 1) + ")");
        System.out.println("Branching factor: " + arr.length + " (number of multipliers)");
        System.out.println("Maximum possible steps: " + MOD + " (if we visit all states)");

        // Check for cycles
        Set<Integer> reachable = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(start);
        reachable.add(start);

        while (!queue.isEmpty()) {
            int value = queue.poll();

            for (int multiplier : arr) {
                int newValue = (value * multiplier) % MOD;
                if (!reachable.contains(newValue)) {
                    reachable.add(newValue);
                    queue.offer(newValue);
                }
            }

            if (reachable.size() > 1000) { // Limit for analysis
                break;
            }
        }

        System.out.println("Reachable states from start: " + reachable.size() +
                (reachable.size() > 1000 ? "+" : ""));
        System.out.println("End reachable: " + reachable.contains(end));
    }

    // Test cases
    public static void main(String[] args) {
        MinimumMultiplicationsToReachEnd solution = new MinimumMultiplicationsToReachEnd();

        // Test Case 1
        int[] arr1 = { 2, 5, 7 };
        int start1 = 3, end1 = 30;

        System.out.println("Example 1:");
        System.out.println("Input: arr = [2, 5, 7], start = 3, end = 30");

        System.out.println("\nOutput (BFS): " + solution.minimumMultiplications(arr1, start1, end1));
        System.out.println("Output (BFS Level): " + solution.minimumMultiplicationsLevelOrder(arr1, start1, end1));
        System.out.println("Output (Dijkstra): " + solution.minimumMultiplicationsDijkstra(arr1, start1, end1));
        System.out.println("Output (DFS): " + solution.minimumMultiplicationsDFS(arr1, start1, end1));

        solution.visualizeSearchProcess(arr1, start1, end1);
        solution.analyzeGraph(arr1, start1, end1);
        System.out.println("\nExpected: 2\n");

        System.out.println("=".repeat(60));

        // Test Case 2
        int[] arr2 = { 3, 4, 65 };
        int start2 = 7, end2 = 66175;

        System.out.println("\nExample 2:");
        System.out.println("Input: arr = [3, 4, 65], start = 7, end = 66175");

        System.out.println("\nOutput (BFS): " + solution.minimumMultiplications(arr2, start2, end2));
        System.out.println("Output (BFS Level): " + solution.minimumMultiplicationsLevelOrder(arr2, start2, end2));
        System.out.println("Output (Dijkstra): " + solution.minimumMultiplicationsDijkstra(arr2, start2, end2));

        solution.visualizeSearchProcess(arr2, start2, end2);
        solution.analyzeGraph(arr2, start2, end2);
        System.out.println("\nExpected: 4\n");

        System.out.println("=".repeat(60));

        // Test Case 3: Already at destination
        int[] arr3 = { 2, 3, 5 };
        int start3 = 10, end3 = 10;

        System.out.println("\nTest Case: Already at destination");
        System.out.println("Input: arr = [2, 3, 5], start = 10, end = 10");
        System.out.println("Output: " + solution.minimumMultiplications(arr3, start3, end3));
        System.out.println("Expected: 0\n");

        // Test Case 4: Unreachable destination
        int[] arr4 = { 2 };
        int start4 = 3, end4 = 5;

        System.out.println("Test Case: Unreachable destination");
        System.out.println("Input: arr = [2], start = 3, end = 5");
        System.out.println("Output: " + solution.minimumMultiplications(arr4, start4, end4));
        solution.analyzeGraph(arr4, start4, end4);
        System.out.println("Expected: -1\n");

        // Test Case 5: Single step
        int[] arr5 = { 10 };
        int start5 = 1, end5 = 10;

        System.out.println("Test Case: Single step");
        System.out.println("Input: arr = [10], start = 1, end = 10");
        System.out.println("Output: " + solution.minimumMultiplications(arr5, start5, end5));
        System.out.println("Expected: 1\n");

        // Performance comparison
        System.out.println("=".repeat(60));
        System.out.println("\n=== Performance Comparison ===");
        int[] arrLarge = { 2, 3, 5, 7, 11, 13 };
        int startLarge = 1, endLarge = 99999;

        long start = System.nanoTime();
        int result1 = solution.minimumMultiplications(arrLarge, startLarge, endLarge);
        long end = System.nanoTime();
        System.out.println("BFS: " + result1 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result2 = solution.minimumMultiplicationsLevelOrder(arrLarge, startLarge, endLarge);
        end = System.nanoTime();
        System.out.println("BFS Level-Order: " + result2 + " (Time: " + (end - start) / 1000 + " μs)");

        start = System.nanoTime();
        int result3 = solution.minimumMultiplicationsDijkstra(arrLarge, startLarge, endLarge);
        end = System.nanoTime();
        System.out.println("Dijkstra: " + result3 + " (Time: " + (end - start) / 1000 + " μs)");
    }
}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1: BFS (RECOMMENDED)
 * - Time Complexity: O(MOD * n)
 * MOD = 100,000 possible states
 * n = length of array (multipliers)
 * In worst case, visit all states
 * At each state, try n multipliers
 * - Space Complexity: O(MOD)
 * Visited array: O(MOD)
 * Queue: O(MOD) in worst case
 * 
 * Solution 2: BFS with Level-Order Traversal
 * - Time Complexity: O(MOD * n)
 * - Space Complexity: O(MOD)
 * Slightly more memory efficient
 * Doesn't store steps in queue
 * 
 * Solution 3: Dijkstra's Algorithm
 * - Time Complexity: O(MOD * n * log(MOD))
 * Priority queue operations: O(log(MOD))
 * Overkill since all edges have weight 1
 * - Space Complexity: O(MOD)
 * Distance array + priority queue
 * 
 * Solution 4: Bidirectional BFS
 * - Time Complexity: O(MOD * n)
 * Can be faster in practice
 * Searches from both ends
 * - Space Complexity: O(MOD)
 * Two sets for visited states
 * 
 * Solution 5: DFS with Memoization
 * - Time Complexity: O(MOD * n)
 * Each state computed once
 * - Space Complexity: O(MOD)
 * Recursion stack + memoization
 * Not optimal for this problem
 * 
 * KEY INSIGHTS:
 * 
 * 1. Graph Interpretation:
 * - Each number (0 to 99,999) is a node
 * - Edge exists from u to v if v = (u * multiplier) % MOD
 * - Problem: Find shortest path from start to end
 * 
 * 2. Why BFS is Perfect:
 * - Unweighted graph (each step has cost 1)
 * - BFS guarantees shortest path in unweighted graphs
 * - First time we reach destination = minimum steps
 * 
 * 3. State Space:
 * - Total possible states: 100,000 (0 to 99,999)
 * - Each state can transition to at most n states
 * - Graph can have cycles (e.g., 50000 * 2 = 0)
 * 
 * 4. Modulo Operation Impact:
 * - (a * b) % MOD creates a bounded state space
 * - Without mod, space would be infinite
 * - Mod creates cycles and loops in the graph
 * 
 * 5. Why Not DFS:
 * - DFS doesn't guarantee shortest path
 * - May explore longer paths first
 * - BFS explores level by level (guaranteed optimal)
 * 
 * 6. Why Not Dijkstra:
 * - All edges have equal weight (1 step)
 * - Dijkstra is overkill for unweighted graphs
 * - BFS is simpler and equally efficient
 * 
 * 7. Visited Array Importance:
 * - Prevents infinite loops
 * - Prevents revisiting same state
 * - Each state visited at most once
 * 
 * 8. Early Termination:
 * - Return immediately when destination reached
 * - First occurrence is guaranteed to be shortest
 * - No need to explore further
 * 
 * 9. Edge Cases:
 * - start == end: return 0
 * - end unreachable: return -1
 * - arr has 1 element that creates cycle
 * - arr empty: impossible to reach (return -1)
 * 
 * 10. Cycle Handling:
 * - Cycles naturally handled by visited array
 * - Example: 5 * 20000 % 100000 = 0, 0 * x = 0
 * - Once visited, never enter again
 * 
 * 11. Optimization Opportunities:
 * - Bidirectional BFS: search from both ends
 * - Prune impossible branches early
 * - Use bit array for visited (memory efficient)
 * - Early termination when queue empty
 * 
 * 12. Mathematical Properties:
 * - Multiplication preserves certain patterns
 * - GCD properties affect reachability
 * - Some end values may be unreachable
 * 
 * 13. Real-World Applications:
 * - Modular arithmetic problems
 * - Cryptography (modular exponentiation)
 * - Game state search spaces
 * - Hash collision resolution
 * 
 * 14. Common Mistakes:
 * - Forgetting modulo operation
 * - Not checking if start == end
 * - Using DFS instead of BFS
 * - Not marking visited states
 * - Integer overflow (use % MOD at each step)
 * 
 * 15. Why Level-Order BFS:
 * - Cleaner code (no steps in queue)
 * - Slightly more memory efficient
 * - Easier to understand levels
 * - Same time complexity
 * 
 * 16. Comparison to Similar Problems:
 * - Word Ladder: similar BFS approach
 * - Knight's Tour: similar state space
 * - 0-1 BFS: if different edge weights
 * - Dijkstra: if weighted edges
 * 
 * 17. Graph Properties:
 * - Directed graph (multiplication order matters)
 * - Can have self-loops (if multiplier = 1)
 * - Dense graph (high branching factor)
 * - May not be strongly connected
 * 
 * 18. When Destination is Unreachable:
 * - GCD issues: if gcd(arr, MOD) doesn't divide end
 * - Example: arr = [2], all reachable are even
 * - Visited set shows reachable states
 */
