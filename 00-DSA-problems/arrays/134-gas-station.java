import java.util.Arrays;
/*
 * There are n gas stations along a circular route, where the amount of gas at
 * the ith station is gas[i].
 * 
 * You have a car with an unlimited gas tank and it costs cost[i] of gas to
 * travel from the ith station to its next (i + 1)th station. You begin the
 * journey with an empty tank at one of the gas stations.
 * 
 * Given two integer arrays gas and cost, return the starting gas station's
 * index if you can travel around the circuit once in the clockwise direction,
 * otherwise return -1. If there exists a solution, it is guaranteed to be
 * unique.
 * 
 * Example 1:
 * Input: gas = [1,2,3,4,5], cost = [3,4,5,1,2]
 * Output: 3
 * Explanation:
 * Start at station 3 (index 3) and fill up with 4 unit of gas. Your tank = 0 +
 * 4 = 4
 * Travel to station 4. Your tank = 4 - 1 + 5 = 8
 * Travel to station 0. Your tank = 8 - 2 + 1 = 7
 * Travel to station 1. Your tank = 7 - 3 + 2 = 6
 * Travel to station 2. Your tank = 6 - 4 + 3 = 5
 * Travel to station 3. The cost is 5. Your gas is just enough to travel back to
 * station 3.
 * Therefore, return 3 as the starting index.
 *
 * Example 2:
 * Input: gas = [2,3,4], cost = [3,4,3]
 * Output: -1
 * Explanation:
 * You can't start at station 0 or 1, as there is not enough gas to travel to
 * the next station.
 * Let's start at station 2 and fill up with 4 unit of gas. Your tank = 0 + 4 =
 * 4
 * Travel to station 0. Your tank = 4 - 3 + 2 = 3
 * Travel to station 1. Your tank = 3 - 3 + 3 = 3
 * You cannot travel back to station 2, as it requires 4 unit of gas but you
 * only have 3.
 * Therefore, you can't travel around the circuit once no matter where you
 * start.
 */

class GasStation {

    // Solution 1: Brute Force - Try each starting position
    // Time: O(n²), Space: O(1)
    public int canCompleteCircuitBruteForce(int[] gas, int[] cost) {
        int n = gas.length;

        // Try each starting position
        for (int start = 0; start < n; start++) {
            if (canCompleteFromStart(gas, cost, start)) {
                return start;
            }
        }

        return -1;
    }

    private boolean canCompleteFromStart(int[] gas, int[] cost, int start) {
        int n = gas.length;
        int tank = 0;

        // Try to complete the circuit starting from 'start'
        for (int i = 0; i < n; i++) {
            int currentStation = (start + i) % n;
            tank += gas[currentStation] - cost[currentStation];

            // If tank becomes negative, we can't proceed
            if (tank < 0) {
                return false;
            }
        }

        return true;
    }

    // Solution 2: Two-Pass Approach
    // Time: O(n), Space: O(1)
    public int canCompleteCircuitTwoPass(int[] gas, int[] cost) {
        int totalGas = 0;
        int totalCost = 0;

        // First pass: check if solution exists
        for (int i = 0; i < gas.length; i++) {
            totalGas += gas[i];
            totalCost += cost[i];
        }

        // If total gas < total cost, no solution exists
        if (totalGas < totalCost) {
            return -1;
        }

        // Second pass: find the starting position
        int tank = 0;
        int start = 0;

        for (int i = 0; i < gas.length; i++) {
            tank += gas[i] - cost[i];

            // If tank becomes negative, we can't start from any position
            // from 'start' to 'i', so try starting from 'i+1'
            if (tank < 0) {
                tank = 0;
                start = i + 1;
            }
        }

        return start;
    }

    // Solution 3: One-Pass Optimal Solution (Recommended)
    // Time: O(n), Space: O(1)
    public int canCompleteCircuit(int[] gas, int[] cost) {
        int totalTank = 0; // Total gas - total cost
        int currentTank = 0; // Current gas in tank
        int startingStation = 0; // Candidate starting station

        for (int i = 0; i < gas.length; i++) {
            int netGas = gas[i] - cost[i];
            totalTank += netGas;
            currentTank += netGas;

            // If current tank becomes negative, we can't reach next station
            // from current starting point, so try next station as start
            if (currentTank < 0) {
                startingStation = i + 1;
                currentTank = 0;
            }
        }

        // If total tank >= 0, solution exists and it's at startingStation
        return totalTank >= 0 ? startingStation : -1;
    }

    // Solution 4: Greedy with Detailed Tracking
    // Time: O(n), Space: O(1)
    public int canCompleteCircuitGreedy(int[] gas, int[] cost) {
        int n = gas.length;
        int totalSurplus = 0;
        int surplus = 0;
        int start = 0;

        for (int i = 0; i < n; i++) {
            totalSurplus += gas[i] - cost[i];
            surplus += gas[i] - cost[i];

            if (surplus < 0) {
                // Current starting point doesn't work
                // Reset and try next position
                start = i + 1;
                surplus = 0;
            }
        }

        return totalSurplus >= 0 ? start : -1;
    }

    // Solution 5: Mathematical Approach with Prefix Sums
    // Time: O(n), Space: O(n)
    public int canCompleteCircuitPrefixSum(int[] gas, int[] cost) {
        int n = gas.length;
        int[] prefixSum = new int[n];
        int totalSum = 0;

        // Calculate prefix sums of (gas[i] - cost[i])
        for (int i = 0; i < n; i++) {
            int diff = gas[i] - cost[i];
            totalSum += diff;
            prefixSum[i] = (i == 0) ? diff : prefixSum[i - 1] + diff;
        }

        // If total sum < 0, no solution
        if (totalSum < 0) {
            return -1;
        }

        // Find the minimum prefix sum
        int minSum = prefixSum[0];
        int minIndex = 0;

        for (int i = 1; i < n; i++) {
            if (prefixSum[i] < minSum) {
                minSum = prefixSum[i];
                minIndex = i;
            }
        }

        // Starting position is right after the minimum prefix sum
        return (minIndex + 1) % n;
    }

    // Utility method for testing
    private void testSolution(String solutionName, int[] gas, int[] cost, int expected) {
        int result = -1;

        switch (solutionName) {
            case "BruteForce":
                result = canCompleteCircuitBruteForce(gas, cost);
                break;
            case "TwoPass":
                result = canCompleteCircuitTwoPass(gas, cost);
                break;
            case "Optimal":
                result = canCompleteCircuit(gas, cost);
                break;
            case "Greedy":
                result = canCompleteCircuitGreedy(gas, cost);
                break;
            case "PrefixSum":
                result = canCompleteCircuitPrefixSum(gas, cost);
                break;
        }

        System.out.printf("%-12s: %d %s\n", solutionName, result,
                result == expected ? "✓" : "✗ (expected " + expected + ")");
    }

    public static void main(String[] args) {
        GasStation solution = new GasStation();

        System.out.println("=== Gas Station Problem Solutions ===\n");

        // Test case 1: Standard case
        int[] gas1 = { 1, 2, 3, 4, 5 };
        int[] cost1 = { 3, 4, 5, 1, 2 };
        System.out.println("Test 1: gas=" + Arrays.toString(gas1) +
                ", cost=" + Arrays.toString(cost1));
        System.out.println("Expected: 3");
        solution.testSolution("BruteForce", gas1, cost1, 3);
        solution.testSolution("TwoPass", gas1, cost1, 3);
        solution.testSolution("Optimal", gas1, cost1, 3);
        solution.testSolution("Greedy", gas1, cost1, 3);
        solution.testSolution("PrefixSum", gas1, cost1, 3);
        System.out.println();

        // Test case 2: No solution
        int[] gas2 = { 2, 3, 4 };
        int[] cost2 = { 3, 4, 3 };
        System.out.println("Test 2: gas=" + Arrays.toString(gas2) +
                ", cost=" + Arrays.toString(cost2));
        System.out.println("Expected: -1");
        solution.testSolution("Optimal", gas2, cost2, -1);
        solution.testSolution("TwoPass", gas2, cost2, -1);
        System.out.println();

        // Test case 3: Single station
        int[] gas3 = { 5 };
        int[] cost3 = { 4 };
        System.out.println("Test 3: gas=" + Arrays.toString(gas3) +
                ", cost=" + Arrays.toString(cost3));
        System.out.println("Expected: 0");
        solution.testSolution("Optimal", gas3, cost3, 0);
        System.out.println();

        // Test case 4: Start from beginning
        int[] gas4 = { 3, 1, 1 };
        int[] cost4 = { 1, 2, 2 };
        System.out.println("Test 4: gas=" + Arrays.toString(gas4) +
                ", cost=" + Arrays.toString(cost4));
        System.out.println("Expected: 0");
        solution.testSolution("Optimal", gas4, cost4, 0);
        System.out.println();

        // Test case 5: Complex case
        int[] gas5 = { 5, 8, 2, 8 };
        int[] cost5 = { 6, 5, 6, 6 };
        System.out.println("Test 5: gas=" + Arrays.toString(gas5) +
                ", cost=" + Arrays.toString(cost5));
        System.out.println("Expected: 3");
        solution.testSolution("Optimal", gas5, cost5, 3);
        System.out.println();

        // Performance test
        System.out.println("=== Performance Test ===");
        int[] largeGas = new int[10000];
        int[] largeCost = new int[10000];

        // Create a solvable case
        Arrays.fill(largeGas, 5);
        Arrays.fill(largeCost, 4);
        largeCost[0] = 10; // Make it so we can't start from 0

        long startTime = System.nanoTime();
        int resultOptimal = solution.canCompleteCircuit(largeGas, largeCost);
        long optimalTime = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        int resultBruteForce = solution.canCompleteCircuitBruteForce(largeGas, largeCost);
        long bruteForceTime = System.nanoTime() - startTime;

        System.out.println("Array size: 10,000");
        System.out.println("Optimal solution: " + resultOptimal +
                " (Time: " + optimalTime / 1000000.0 + " ms)");
        System.out.println("Brute force: " + resultBruteForce +
                " (Time: " + bruteForceTime / 1000000.0 + " ms)");
        System.out.println("Speedup: " + (bruteForceTime / (double) optimalTime) + "x");
    }

}

/*
 * ALGORITHM EXPLANATIONS:
 * 
 * 1. BRUTE FORCE APPROACH:
 * - Try each starting position and simulate the journey
 * - For each start, check if we can complete the entire circuit
 * - Time: O(n²), Space: O(1)
 * 
 * 2. TWO-PASS APPROACH:
 * - First pass: Check if total gas >= total cost (necessary condition)
 * - Second pass: Find the starting position using greedy approach
 * - Time: O(n), Space: O(1)
 * 
 * 3. ONE-PASS OPTIMAL SOLUTION (RECOMMENDED):
 * - Combine both checks in a single pass
 * - Track total tank and current tank simultaneously
 * - If current tank goes negative, reset start position
 * - Time: O(n), Space: O(1)
 * 
 * 4. GREEDY APPROACH:
 * - Similar to optimal but with more explicit variable names
 * - The greedy choice: if we can't reach from current start, try next position
 * - Time: O(n), Space: O(1)
 * 
 * 5. PREFIX SUM APPROACH:
 * - Calculate prefix sums of (gas[i] - cost[i])
 * - Find minimum prefix sum - start after this position
 * - Time: O(n), Space: O(n)
 * 
 * KEY INSIGHTS:
 * 
 * 1. NECESSARY CONDITION: Total gas must be >= total cost
 * 2. GREEDY PROPERTY: If we can't reach station j from station i,
 * then we can't reach j from any station between i and j-1
 * 3. UNIQUENESS: The problem guarantees a unique solution if one exists
 * 
 * ALGORITHM WALKTHROUGH for [1,2,3,4,5] and [3,4,5,1,2]:
 * 
 * Station 0: gas=1, cost=3, net=-2, tank=-2 (can't proceed, try start=1)
 * Station 1: gas=2, cost=4, net=-2, tank=-2 (can't proceed, try start=2)
 * Station 2: gas=3, cost=5, net=-2, tank=-2 (can't proceed, try start=3)
 * Station 3: gas=4, cost=1, net=+3, tank=3 (good so far)
 * Station 4: gas=5, cost=2, net=+3, tank=6 (still good)
 * 
 * Total net = -2-2-2+3+3 = 0 >= 0, so solution exists at start=3
 * 
 * WHY THE GREEDY APPROACH WORKS:
 * - If starting from station i, we run out of gas at station j
 * - Then starting from any station between i and j-1 will also fail
 * - This is because we had accumulated positive net gas from i to j-1
 * - So we should try starting from station j
 * 
 * The one-pass optimal solution is best for interviews because:
 * - O(n) time, O(1) space
 * - Single pass through the array
 * - Elegant and easy to explain
 * - Handles all edge cases naturally
 */