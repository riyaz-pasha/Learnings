// how do i know to traver from  one position to another position? are they connected somehow?
// is station positions sorted?

// i start at station 0 with fuels as startfuel

// from here i can go to any station within this fuel
// mayybe we can go to positions[0]

// if positions[0][0]>startfuel then return max value as we cannot go further.
// if we can reach this station we have to options
// either stop at station or proceed to next station.

// if we stop at this station then we add maxfuel so our fuel becomes total available fuel at this station + startfuel-station_distance
// we can increase our stops count
// if we skip this station then we go to the next station and stops count stays same.

// we are solving subproblems multiple times we can memoize the results


import java.util.Arrays;
import java.util.Collections;
import java.util.PriorityQueue;

/**
 * PROBLEM ANALYSIS: MINIMUM REFUELING STOPS
 * ==========================================
 * 
 * PROBLEM UNDERSTANDING:
 * - Car starts with startFuel liters
 * - Need to reach target miles (1 liter = 1 mile)
 * - Gas stations at various positions with different fuel amounts
 * - Find minimum number of refueling stops to reach target
 * - Return -1 if impossible
 * 
 * KEY INSIGHTS:
 * 1. Car uses 1 liter per mile
 * 2. Can reach any station within current fuel range
 * 3. Greedy approach: Always refuel at station with MOST fuel when needed
 * 4. Don't need to decide refueling in advance - can "retroactively" refuel
 * 
 * CRITICAL REALIZATION:
 * - We don't need to decide whether to stop at a station when we pass it
 * - We can keep track of passed stations and refuel from them later when needed
 * - This enables a greedy strategy: always use the largest available fuel
 * 
 * INTERVIEW APPROACH - HOW TO THINK THROUGH THIS:
 * ================================================
 * 
 * Step 1: Understand the problem thoroughly
 * - "Can I refuel at any passed station?" (Conceptually yes, via greedy)
 * - "Do I need to stop at every station?" (No, minimize stops)
 * - "What if I can't reach the target?" (Return -1)
 * 
 * Step 2: Identify approach options
 * - Brute Force: Try all combinations of stations O(2^n)
 * - DP: dp[i][j] = max distance with i stops at first j stations
 * - Greedy with Max Heap: Track passed stations, refuel with largest when needed
 * 
 * Step 3: Key insight for greedy approach
 * "When we run out of fuel, we should refuel from the station
 *  we passed that had the MOST fuel. This minimizes future stops."
 * 
 * Step 4: Algorithm strategy
 * - Drive as far as possible with current fuel
 * - Track all stations we pass
 * - When we can't reach next milestone, refuel from best passed station
 * - Repeat until we reach target or determine it's impossible
 * 
 * APPROACHES:
 * 1. Dynamic Programming - O(n²) time, O(n²) space
 * 2. Greedy with Max Heap - O(n log n) time, O(n) space [OPTIMAL]
 * 3. DP Optimized - O(n²) time, O(n) space
 */

/**
 * APPROACH 1: GREEDY WITH MAX HEAP (OPTIMAL - RECOMMENDED)
 * =========================================================
 * 
 * INTUITION:
 * - Drive as far as possible with current fuel
 * - Keep track of all gas stations we've passed
 * - When we can't go further, refuel from the station with MOST fuel
 * - This greedy choice minimizes total stops
 * 
 * WHY GREEDY WORKS:
 * - If we need to refuel, choosing the largest fuel amount
 *   gives us maximum range for our next segment
 * - This minimizes the number of future refueling stops needed
 * - We never regret this choice because larger fuel is always better
 * 
 * ALGORITHM:
 * 1. Use max heap to store fuel amounts of passed stations
 * 2. Drive forward, adding stations to heap as we pass them
 * 3. When we can't reach next position:
 *    - Refuel from station with most fuel (top of heap)
 *    - Increment stop counter
 *    - Continue driving
 * 4. If heap is empty and we can't reach target, return -1
 * 
 * TIME: O(n log n) - each station added/removed from heap once
 * SPACE: O(n) - heap can contain all stations
 */
class Solution {
    
    public int minRefuelStops(int target, int startFuel, int[][] stations) {
        // Max heap to store fuel amounts of passed stations
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> b - a);
        
        int currentPosition = 0;
        int currentFuel = startFuel;
        int stationIndex = 0;
        int refuelStops = 0;
        
        // Keep going until we reach the target
        while (currentPosition + currentFuel < target) {
            // Add all reachable stations to the heap
            while (stationIndex < stations.length && 
                   stations[stationIndex][0] <= currentPosition + currentFuel) {
                maxHeap.offer(stations[stationIndex][1]);
                stationIndex++;
            }
            
            // If no stations available, we can't reach target
            if (maxHeap.isEmpty()) {
                return -1;
            }
            
            // Refuel from the station with most fuel
            int maxFuel = maxHeap.poll();
            currentFuel += maxFuel;
            refuelStops++;
            
            // Note: We don't update position here because we're
            // "retroactively" refueling at a passed station
        }
        
        return refuelStops;
    }
}

/**
 * APPROACH 2: GREEDY WITH MAX HEAP (ALTERNATIVE IMPLEMENTATION)
 * ==============================================================
 * 
 * Cleaner version that updates position explicitly
 */
class SolutionAlternative {
    
    public int minRefuelStops(int target, int startFuel, int[][] stations) {
        // Max heap stores fuel amounts
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        
        int maxReach = startFuel;  // Farthest we can currently reach
        int stops = 0;
        int i = 0;
        
        while (maxReach < target) {
            // Add all stations within current reach to heap
            while (i < stations.length && stations[i][0] <= maxReach) {
                maxHeap.offer(stations[i][1]);
                i++;
            }
            
            // If no stations available, can't reach target
            if (maxHeap.isEmpty()) {
                return -1;
            }
            
            // Refuel with largest available fuel
            maxReach += maxHeap.poll();
            stops++;
        }
        
        return stops;
    }
}

/**
 * APPROACH 3: DYNAMIC PROGRAMMING
 * ================================
 * 
 * INTUITION:
 * - dp[i] = maximum distance reachable with exactly i refueling stops
 * - For each station, decide whether to refuel there
 * - Update DP table based on this decision
 * 
 * STATE DEFINITION:
 * dp[t] = farthest location we can reach with t refueling stops
 * 
 * TRANSITION:
 * For each station at position[i] with fuel[i]:
 *   If we can reach this station with t stops (dp[t] >= position[i]):
 *     We can reach position[i] + fuel[i] with t+1 stops
 *     dp[t+1] = max(dp[t+1], dp[t] + fuel[i])
 * 
 * TIME: O(n²) - for each station, update up to n DP states
 * SPACE: O(n) - DP array
 */
class SolutionDP {
    
    public int minRefuelStops(int target, int startFuel, int[][] stations) {
        int n = stations.length;
        
        // dp[i] = farthest distance with i refueling stops
        long[] dp = new long[n + 1];
        dp[0] = startFuel;
        
        // Process each station
        for (int i = 0; i < n; i++) {
            int position = stations[i][0];
            int fuel = stations[i][1];
            
            // Update DP table in reverse to avoid using updated values
            for (int t = i; t >= 0; t--) {
                // If we can reach this station with t stops
                if (dp[t] >= position) {
                    // We can reach further with t+1 stops
                    dp[t + 1] = Math.max(dp[t + 1], dp[t] + fuel);
                }
            }
        }
        
        // Find minimum stops needed to reach target
        for (int i = 0; i <= n; i++) {
            if (dp[i] >= target) {
                return i;
            }
        }
        
        return -1;
    }
}

/**
 * APPROACH 4: 2D DYNAMIC PROGRAMMING (EDUCATIONAL)
 * =================================================
 * 
 * More intuitive DP formulation but uses more space
 * 
 * STATE:
 * dp[i][j] = maximum distance reachable using exactly i refueling stops
 *            from first j stations
 * 
 * TIME: O(n²)
 * SPACE: O(n²)
 */
class SolutionDP2D {
    
    public int minRefuelStops(int target, int startFuel, int[][] stations) {
        int n = stations.length;
        
        // dp[stops][stationIdx] = max distance with 'stops' from first 'stationIdx' stations
        long[][] dp = new long[n + 1][n + 1];
        
        // Base case: with 0 stops, can reach startFuel distance
        for (int j = 0; j <= n; j++) {
            dp[0][j] = startFuel;
        }
        
        // Fill DP table
        for (int i = 1; i <= n; i++) {  // number of stops
            for (int j = i; j <= n; j++) {  // considering first j stations
                int position = stations[j - 1][0];
                int fuel = stations[j - 1][1];
                
                // Option 1: Don't refuel at station j
                dp[i][j] = dp[i][j - 1];
                
                // Option 2: Refuel at station j (if reachable)
                if (dp[i - 1][j - 1] >= position) {
                    dp[i][j] = Math.max(dp[i][j], dp[i - 1][j - 1] + fuel);
                }
            }
        }
        
        // Find minimum stops
        for (int i = 0; i <= n; i++) {
            if (dp[i][n] >= target) {
                return i;
            }
        }
        
        return -1;
    }
}

/**
 * APPROACH 5: GREEDY WITH DETAILED TRACKING (FOR UNDERSTANDING)
 * ==============================================================
 * 
 * More verbose version that explicitly tracks journey
 */
class SolutionDetailed {
    
    public int minRefuelStops(int target, int startFuel, int[][] stations) {
        if (startFuel >= target) {
            return 0;
        }
        
        PriorityQueue<Integer> availableFuel = new PriorityQueue<>(Collections.reverseOrder());
        
        int position = 0;
        int fuel = startFuel;
        int stops = 0;
        int stationIdx = 0;
        
        while (true) {
            // Calculate how far we can go with current fuel
            int maxReach = position + fuel;
            
            // Check if we can reach the target
            if (maxReach >= target) {
                return stops;
            }
            
            // Collect all stations within current reach
            while (stationIdx < stations.length && 
                   stations[stationIdx][0] <= maxReach) {
                availableFuel.offer(stations[stationIdx][1]);
                stationIdx++;
            }
            
            // If no stations available, we're stuck
            if (availableFuel.isEmpty()) {
                return -1;
            }
            
            // Refuel at the best station we've passed
            int bestFuel = availableFuel.poll();
            fuel += bestFuel;
            stops++;
            
            // Continue journey (position stays same as we refueled at passed station)
        }
    }
}

/**
 * TEST CASES AND EXAMPLES
 * ========================
 */
class TestMinRefuelStops {
    
    public static void runTest(String testName, int target, int startFuel, 
                               int[][] stations, int expected) {
        System.out.println("\n" + testName);
        System.out.println("Target: " + target + ", Start Fuel: " + startFuel);
        System.out.println("Stations: " + Arrays.deepToString(stations));
        
        Solution solution = new Solution();
        int result = solution.minRefuelStops(target, startFuel, stations);
        
        System.out.println("Result: " + result);
        System.out.println("Expected: " + expected);
        System.out.println("Status: " + (result == expected ? "✓ PASS" : "✗ FAIL"));
    }
    
    public static void main(String[] args) {
        System.out.println("=== MINIMUM REFUELING STOPS - COMPREHENSIVE TESTING ===\n");
        
        // Test Case 1: Basic example
        runTest(
            "Test 1: Basic case",
            100,
            10,
            new int[][]{{10, 60}, {20, 30}, {30, 30}, {60, 40}},
            2
        );
        // Explanation: Start with 10L, reach station at 10 (refuel 60L)
        // Now have 60L, can reach 70. Refuel at station 60 (40L)
        // Now can reach 100+
        
        // Test Case 2: Can reach without refueling
        runTest(
            "Test 2: No refueling needed",
            100,
            100,
            new int[][]{{10, 60}, {20, 30}},
            0
        );
        
        // Test Case 3: Impossible to reach
        runTest(
            "Test 3: Impossible",
            100,
            10,
            new int[][]{{10, 10}},
            -1
        );
        // Can only reach 20 total
        
        // Test Case 4: Need all stations
        runTest(
            "Test 4: Multiple stops needed",
            100,
            1,
            new int[][]{{10, 100}},
            -1
        );
        // Can't even reach first station
        
        // Test Case 5: Greedy choice matters
        runTest(
            "Test 5: Greedy selection",
            100,
            10,
            new int[][]{{10, 10}, {20, 20}, {30, 30}},
            3
        );
        
        // Test Case 6: Edge case - reach with 0 fuel
        runTest(
            "Test 6: Exact reach",
            100,
            50,
            new int[][]{{50, 50}},
            1
        );
        
        // Test Case 7: No stations
        runTest(
            "Test 7: No stations, sufficient fuel",
            100,
            100,
            new int[][]{},
            0
        );
        
        // Test Case 8: No stations, insufficient fuel
        runTest(
            "Test 8: No stations, insufficient fuel",
            100,
            50,
            new int[][]{},
            -1
        );
        
        // Test Case 9: Large numbers
        runTest(
            "Test 9: Large values",
            1000000,
            1000,
            new int[][]{{1000, 100000}, {100000, 900000}},
            2
        );
        
        // Test Case 10: Multiple stations at different positions
        runTest(
            "Test 10: Strategic refueling",
            100,
            25,
            new int[][]{{25, 25}, {50, 50}},
            1
        );
        // Start: 25L -> reach 25, refuel 25L (total 25L)
        // But we should refuel at 25 -> have 50L -> reach 75
        // Then refuel at 50 -> reach 125+
        
        // Demonstrate greedy vs non-greedy
        System.out.println("\n\n=== GREEDY CORRECTNESS DEMONSTRATION ===");
        System.out.println("\nScenario: Target=100, Start=10");
        System.out.println("Stations: [10,60], [20,30], [30,30], [60,40]");
        System.out.println("\nGreedy approach:");
        System.out.println("1. Start with 10L, add stations 10,20,30 to heap: [60,30,30]");
        System.out.println("2. Can't reach 60, refuel with 60L from station 10");
        System.out.println("3. Now at 70L, can reach station 60, add 40L: [40,30,30]");
        System.out.println("4. Can't reach 100, refuel with 40L from station 60");
        System.out.println("5. Now can reach 100+. Total: 2 stops");
        System.out.println("\nWhy greedy works: Always choosing largest fuel maximizes future range");
    }
}

/**
 * COMPLEXITY ANALYSIS
 * ===================
 * 
 * Approach 1 - Greedy with Max Heap (OPTIMAL):
 * Time:  O(n log n)
 *        - Process each station once: O(n)
 *        - Each heap operation: O(log n)
 *        - At most n heap operations
 * Space: O(n) - heap can store all stations
 * 
 * Approach 2 - Dynamic Programming (1D):
 * Time:  O(n²)
 *        - For each station: O(n)
 *        - Update up to n DP states: O(n)
 * Space: O(n) - DP array
 * 
 * Approach 3 - Dynamic Programming (2D):
 * Time:  O(n²)
 * Space: O(n²) - 2D DP table
 * 
 * 
 * INTERVIEW STRATEGY
 * ==================
 * 
 * 1. CLARIFY REQUIREMENTS:
 *    Q: "Can the car reach a station with exactly 0 fuel?"
 *    A: Yes, problem states this is allowed
 *    
 *    Q: "Are stations sorted by position?"
 *    A: Usually yes, but code should handle unsorted too
 *    
 *    Q: "Can we refuel partially?"
 *    A: No, take all fuel from station
 *    
 *    Q: "What if multiple stations at same position?"
 *    A: Rare, but handle in order given
 * 
 * 2. START WITH BRUTE FORCE:
 *    "The brute force would be to try all combinations of stations.
 *     That's 2^n subsets - way too slow.
 *     
 *     We need a smarter approach."
 * 
 * 3. BUILD INTUITION:
 *    "Key insight: When we run out of fuel, we want to refuel
 *     from the station with the MOST fuel that we've passed.
 *     
 *     Why? Because more fuel gives us more range, minimizing
 *     the number of future stops needed.
 *     
 *     This is a greedy strategy, and it's optimal!"
 * 
 * 4. EXPLAIN THE ALGORITHM:
 *    "Here's the plan:
 *     
 *     1. Use a max heap to track fuel amounts of passed stations
 *     2. Drive forward as far as possible
 *     3. Add all reachable stations to the heap
 *     4. When we can't go further:
 *        - Refuel from station with most fuel (top of heap)
 *        - This is like 'retroactively' stopping there
 *     5. Repeat until we reach target or run out of options
 *     
 *     The 'retroactive' refueling is key - we don't need to
 *     decide when passing a station, we decide later when needed."
 * 
 * 5. WALK THROUGH EXAMPLE:
 *    Target=100, Start=10, Stations=[[10,60],[20,30],[30,30],[60,40]]
 *    
 *    Step 1: maxReach=10, can add stations at 10
 *            heap=[60], maxReach=10
 *    
 *    Step 2: Can't reach target, refuel with 60
 *            maxReach=70, stops=1
 *    
 *    Step 3: Add stations at 20,30,60 (all reachable)
 *            heap=[40,30,30]
 *    
 *    Step 4: Can't reach 100, refuel with 40
 *            maxReach=110, stops=2
 *    
 *    Step 5: Can reach target! Return 2
 * 
 * 6. PROVE CORRECTNESS:
 *    "Why is greedy optimal?
 *     
 *     Suppose there's a better solution that doesn't use
 *     the largest available fuel at some point.
 *     
 *     We could swap that choice with the largest fuel and
 *     only improve (or maintain) our range.
 *     
 *     Therefore, greedy is optimal."
 * 
 * 7. DISCUSS ALTERNATIVES:
 *    "There's also a DP solution:
 *     dp[i] = farthest reach with i refueling stops
 *     
 *     For each station, update: dp[i+1] = dp[i] + fuel
 *     
 *     This works but is O(n²) vs O(n log n) for heap.
 *     
 *     The heap solution is better for this problem."
 * 
 * 8. EDGE CASES:
 *    ✓ Can reach target without refueling
 *    ✓ Impossible to reach (return -1)
 *    ✓ No stations available
 *    ✓ Can barely reach with 0 fuel left
 *    ✓ Need to use all stations
 *    ✓ Large values (use long if needed)
 *    ✓ Stations not sorted (sort them first)
 * 
 * 9. OPTIMIZATION NOTES:
 *    - Could use array instead of PriorityQueue if we know max size
 *    - Could break early once target is reached
 *    - Could preprocess to remove unreachable stations
 * 
 * 10. COMMON MISTAKES:
 *     ✗ Trying to decide at each station (too complex)
 *     ✗ Not using max heap (min heap doesn't work)
 *     ✗ Updating position when refueling (conceptual error)
 *     ✗ Not checking if heap is empty before polling
 *     ✗ Integer overflow with large values
 *     ✗ Off-by-one in reachability checks
 * 
 * FOLLOW-UP QUESTIONS:
 * ====================
 * 
 * Q: "What if we want to minimize fuel cost, not stops?"
 * A: Would need to track costs and use different greedy strategy
 *    or dynamic programming with cost optimization
 * 
 * Q: "What if stations have limited fuel capacity?"
 * A: Would need to track remaining fuel at each station
 *    More complex state management required
 * 
 * Q: "What if we can go backwards?"
 * A: Fundamentally different problem - might need BFS/Dijkstra
 * 
 * Q: "How would you handle real-time traffic/delays?"
 * A: Would need to incorporate time factor, more complex model
 * 
 * Q: "What about multiple cars/coordination?"
 * A: Game theory problem, much more complex
 * 
 * RECOMMENDED SOLUTION:
 * Approach 1 (Greedy with Max Heap) is the optimal solution
 * for interviews. It's efficient, elegant, and demonstrates
 * strong problem-solving skills with the "retroactive refueling"
 * insight.
 */
