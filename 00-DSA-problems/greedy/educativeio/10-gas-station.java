import java.util.List;

/**
 * ============================================================================
 * INTERVIEW GUIDE: GAS STATION
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "What happens if the total gas available across all stations is less 
 *      than the total cost to travel the full circle?" 
 *      (Assumption: It's impossible to complete the circle, so we return -1.)
 *    - "Can there be multiple valid starting stations?" 
 *      (Assumption: The prompt guarantees that if a solution exists, it is UNIQUE.)
 *    - "Do we start with an empty tank?" 
 *      (Assumption: Yes, we start with 0 gas.)
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Observation 1 (Global Solvability): If the sum of all `gas` is LESS than 
 *      the sum of all `cost`, it is mathematically impossible to complete the 
 *      journey. If the sum of `gas` >= sum of `cost`, there is ALWAYS exactly 
 *      one valid starting point.
 *    - Observation 2 (The "Stuck" Rule): Suppose we start at station A and 
 *      successfully travel to B, but at B we don't have enough gas to reach B+1. 
 *      Can we start at any station BETWEEN A and B to successfully reach B+1? 
 *      NO. Because if we started at A, we reached those intermediate stations 
 *      with some surplus gas (gas >= 0). If we start at an intermediate station 
 *      with 0 gas, we will only run out of gas even faster!
 *    - Observation 3 (Greedy Strategy): Therefore, if we get stuck trying to 
 *      travel from index 'i' to 'i+1', the NEXT possible valid starting station 
 *      must be 'i+1'. We just shift our starting point to 'i+1' and reset our 
 *      current tank to 0. 
 * 
 * 3. VISUAL EXPLANATION:
 *    gas  = [1, 2, 3, 4, 5]
 *    cost = [3, 4, 5, 1, 2]
 *    Net (gas - cost): [-2, -2, -2, +3, +3]
 *    
 *    Variables: totalGas = 0, currentTank = 0, startStation = 0
 *    
 *    Station 0 (net = -2): 
 *      - tank becomes -2. We are stuck!
 *      - reset tank = 0. New startStation = 0 + 1 = 1.
 *      - totalGas accumulates to -2.
 *      
 *    Station 1 (net = -2):
 *      - tank becomes -2. Stuck again!
 *      - reset tank = 0. New startStation = 1 + 1 = 2.
 *      - totalGas accumulates to -4.
 *      
 *    Station 2 (net = -2):
 *      - tank becomes -2. Stuck again!
 *      - reset tank = 0. New startStation = 2 + 1 = 3.
 *      - totalGas accumulates to -6.
 *      
 *    Station 3 (net = +3):
 *      - tank becomes +3. We survived!
 *      - totalGas accumulates to -3.
 *      
 *    Station 4 (net = +3):
 *      - tank becomes 3 + 3 = 6. Survived!
 *      - totalGas accumulates to 0.
 *      
 *    End of loop. 
 *    Since totalGas (0) >= 0, a solution exists. 
 *    Return startStation (3).
 * 
 * ============================================================================
 */
public class GasStation {

    /**
     * APPROACH 1: Greedy Traversal (Optimal)
     * 
     * Time Complexity: O(N) where N is the number of stations. We iterate exactly once.
     * Space Complexity: O(1) auxiliary space.
     */
    public int canCompleteCircuitOptimal(int[] gas, int[] cost) {
        int totalSurplus = 0;   // Tracks the net gas over the ENTIRE journey
        int currentTank = 0;    // Tracks gas from the proposed start station
        int startStation = 0;   // Proposes the starting index
        
        for (int i = 0; i < gas.length; i++) {
            int netGasAtStation = gas[i] - cost[i];
            
            totalSurplus += netGasAtStation;
            currentTank += netGasAtStation;
            
            // If at any point our tank runs dry, we can't reach station i + 1
            // from our current startStation.
            if (currentTank < 0) {
                // Any station between startStation and i is invalid.
                // The next viable candidate is i + 1.
                startStation = i + 1;
                // Reset the tank for the new starting point
                currentTank = 0;
            }
        }
        
        // If the total gas overall is less than total cost, it's impossible.
        // Otherwise, our proposed startStation is guaranteed to be correct.
        return totalSurplus >= 0 ? startStation : -1;
    }

    /**
     * APPROACH 2: Brute Force (Good for explaining intuition, but will Time Out)
     * 
     * Simulate starting the journey from every single station.
     * 
     * Time Complexity: O(N^2)
     * Space Complexity: O(1)
     */
    public int canCompleteCircuitBruteForce(int[] gas, int[] cost) {
        int n = gas.length;
        
        // Try every station as a starting point
        for (int i = 0; i < n; i++) {
            int currentTank = 0;
            boolean completed = true;
            
            // Traverse the circle
            for (int step = 0; step < n; step++) {
                // Use modulo arithmetic to wrap around the circular array
                int currStation = (i + step) % n;
                
                currentTank += gas[currStation] - cost[currStation];
                
                // If tank drops below zero, this starting point failed
                if (currentTank < 0) {
                    completed = false;
                    break;
                }
            }
            
            if (completed) {
                return i;
            }
        }
        
        return -1;
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Records (introduced in Java 14) provide a concise way to create immutable data carriers.
     */
    record TestCase(int[] gas, int[] cost, int expected) {}

    public static void main(String[] args) {
        GasStation solver = new GasStation();
        
        // Defining test cases using our Record
        var testCases = List.of(
            new TestCase(new int[]{1, 2, 3, 4, 5}, new int[]{3, 4, 5, 1, 2}, 3),
            new TestCase(new int[]{2, 3, 4}, new int[]{3, 4, 3}, -1),
            new TestCase(new int[]{5, 1, 2, 3, 4}, new int[]{4, 4, 1, 5, 1}, 4),
            new TestCase(new int[]{2}, new int[]{2}, 0),          // Edge case: length 1, exactly enough gas
            new TestCase(new int[]{2}, new int[]{3}, -1)          // Edge case: length 1, not enough gas
        );
        
        System.out.println("--- Running Approach 1 (Greedy O(N)) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.canCompleteCircuitOptimal(tc.gas(), tc.cost());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (Brute Force O(N^2)) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.canCompleteCircuitBruteForce(tc.gas(), tc.cost());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}
