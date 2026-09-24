import java.util.*;

/**
 * ============================================================================
 * INTERVIEW GUIDE: TWO CITY SCHEDULING
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "Is the number of candidates always even?" 
 *      (Assumption: Yes, the constraints specify costs.length is even, so 
 *      we can always divide them exactly in half.)
 *    - "Can the costs be negative?" 
 *      (Assumption: No, based on constraints 1 <= cost <= 1000, but the logic 
 *      would actually still hold even if they were negative.)
 *    - "Am I allowed to modify the input array?"
 *      (Assumption: Yes, modifying it for in-place sorting saves space. If not, 
 *      we would clone it or use an index array.)
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Send exactly n/2 people to City A and n/2 to City B at minimum cost.
 *    - Observation 1 (Opportunity Cost): Suppose we decide to send EVERYONE 
 *      to City B. The total cost is the sum of all bCost.
 *    - Observation 2 (The Refund/Penalty): Now, we must correct this by picking 
 *      exactly n/2 people and rerouting them to City A. If we reroute a person, 
 *      we get a "refund" of their bCost, but we have to pay their aCost. 
 *      The net change in our total cost for rerouting person i is: 
 *      CostChange = aCost_i - bCost_i.
 *    - Observation 3 (Greedy Choice): To keep our total cost as low as possible, 
 *      we should pick the n/2 people who have the smallest (most negative) 
 *      CostChange. A negative change means rerouting them to City A actually 
 *      saves us money compared to sending them to B!
 *    - Approach: Sort the array by the difference (aCost - bCost). 
 *      The first n/2 people in this sorted array go to City A. 
 *      The remaining n/2 people go to City B.
 * 
 * 3. VISUAL EXPLANATION:
 *    Costs: [[10, 20], [30, 200], [400, 50], [30, 20]]
 *    
 *    Step 1: Calculate (aCost - bCost) for each person:
 *      Person 0: 10 - 20 = -10
 *      Person 1: 30 - 200 = -170   <-- Huge savings if sent to A!
 *      Person 2: 400 - 50 = +350   <-- Huge loss if sent to A. Send to B!
 *      Person 3: 30 - 20 = +10
 *    
 *    Step 2: Sort based on these differences (ascending):
 *      Sorted Array: [[30, 200], [10, 20], [30, 20], [400, 50]]
 *      Differences:     -170       -10       +10       +350
 *    
 *    Step 3: Split down the middle (n/2 = 2):
 *      First Half -> City A: [30, 200] (pay 30), [10, 20] (pay 10). Total = 40.
 *      Second Half -> City B: [30, 20] (pay 20), [400, 50] (pay 50). Total = 70.
 *      Final Answer: 40 + 70 = 110.
 * 
 * ============================================================================
 */
class TwoCityScheduling {

    /**
     * APPROACH 1: Greedy with In-Place Sorting (Optimal & Standard)
     * 
     * Time Complexity: O(N log N) dominated by the sorting step.
     * Space Complexity: O(1) or O(log N) depending on the sort algorithm 
     * under the hood, since we modify the input array in-place.
     */
    public int twoCitySchedCostOptimal(int[][] costs) {
        // Step 1: Sort the array by the difference of cost: aCost - bCost
        // Using Java 8+ lambda for clean Comparator syntax.
        Arrays.sort(costs, Comparator.comparingInt(candidate -> (candidate[0] - candidate[1])));
        
        int totalCost = 0;
        int n = costs.length / 2;
        
        // Step 2: Sum up the costs
        for (int i = 0; i < n; i++) {
            // First half (0 to n-1) goes to City A
            totalCost += costs[i][0];
            
            // Second half (n to length-1) goes to City B
            // Note: i + n conveniently targets the exact matching element in the second half
            totalCost += costs[i + n][1];
        }
        
        return totalCost;
    }

    /**
     * APPROACH 2: Non-Destructive Wrapper (Modern Java & OOP Principles)
     * 
     * In many real-world enterprise applications, mutating input parameters 
     * (like sorting an array passed by reference) is a bad practice. 
     * This approach leaves the input pristine by mapping it to a Stream of 
     * wrapper objects, sorting them, and processing them.
     * 
     * Time Complexity: O(N log N)
     * Space Complexity: O(N) for the new array creation.
     */
    public int twoCitySchedCostNonDestructive(int[][] costs) {
        // Create an array of indices to avoid cloning the 2D arrays (saves space)
        Integer[] indices = new Integer[costs.length];
        for (int i = 0; i < costs.length; i++) {
            indices[i] = i;
        }
        
        // Sort the indices based on the cost differences in the original array
        Arrays.sort(indices, Comparator.comparingInt(i -> (costs[i][0] - costs[i][1])));
        
        int totalCost = 0;
        int n = costs.length / 2;
        
        for (int i = 0; i < n; i++) {
            int personA = indices[i];       // Goes to City A
            int personB = indices[i + n];   // Goes to City B
            
            totalCost += costs[personA][0] + costs[personB][1];
        }
        
        return totalCost;
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Deeply nesting multi-dimensional arrays in records makes testing a breeze.
     */
    record TestCase(int[][] costs, int expected) {}

    public static void main(String[] args) {
        TwoCityScheduling solver = new TwoCityScheduling();
        
        // Defining test cases using our Record
        var testCases = List.of(
            new TestCase(new int[][]{{10, 20}, {30, 200}, {400, 50}, {30, 20}}, 110),
            new TestCase(new int[][]{{259, 770}, {448, 54}, {926, 667}, {184, 139}, {840, 118}, {577, 469}}, 1859),
            new TestCase(new int[][]{{515, 563}, {451, 713}, {537, 709}, {343, 819}, {855, 779}, {457, 60}, {650, 359}, {631, 42}}, 3086)
        );
        
        System.out.println("--- Running Approach 1 (Optimal, Destructive) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            
            // Deep clone the 2D array so the first approach doesn't ruin the data for the second
            int[][] clonedCosts = new int[tc.costs().length][];
            for (int j = 0; j < tc.costs().length; j++) {
                clonedCosts[j] = tc.costs()[j].clone();
            }
            
            int result = solver.twoCitySchedCostOptimal(clonedCosts);
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (Non-Destructive OOP) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.twoCitySchedCostNonDestructive(tc.costs());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}

class TwoCityScheduling2 {

    public static void main(String[] args) {

        int[][] costs = {
            {10, 20},
            {30, 200},
            {400, 50},
            {30, 20}
        };

        System.out.println(twoCitySchedCost(costs)); // Expected: 110
    }

    public static int twoCitySchedCost(int[][] costs) {

        /*
         * ================================================================
         * 🧠 CORE IDEA (VERY IMPORTANT FOR INTERVIEW)
         * ================================================================
         *
         * We must send exactly n/2 people to City A and n/2 to City B.
         *
         * Instead of directly deciding "A or B",
         * think in terms of "ADVANTAGE" or "SAVINGS".
         *
         * For each person:
         *   diff = costA - costB
         *
         * Meaning:
         *   diff < 0 → A is cheaper → prefer sending to A
         *   diff > 0 → B is cheaper → prefer sending to B
         *
         * So we sort people based on this difference.
         *
         * WHY SORT WORKS (Greedy Proof Intuition):
         *   - People with biggest advantage for A should go first
         *   - Others naturally go to B
         *
         * This ensures global minimum cost (exchange argument).
         */

        Arrays.sort(costs, Comparator.comparingInt(person -> person[0] - person[1]));

        int n = costs.length;
        int half = n / 2;

        int totalCost = 0;

        /*
         * ================================================================
         * 🧮 ASSIGNMENT PHASE
         * ================================================================
         *
         * After sorting:
         *
         * First half → smallest (A - B) → strong preference for A
         * Second half → larger (A - B) → better for B
         *
         * So:
         *   - First n/2 → send to A
         *   - Remaining → send to B
         */

        for (int i = 0; i < n; i++) {

            if (i < half) {
                /*
                 * These people have the lowest (A - B),
                 * meaning A is MUCH cheaper than B for them.
                 *
                 * So we send them to City A.
                 */
                totalCost += costs[i][0];

            } else {
                /*
                 * These people have higher (A - B),
                 * meaning B is cheaper (or less costly compared to A).
                 *
                 * So we send them to City B.
                 */
                totalCost += costs[i][1];
            }
        }

        /*
         * ================================================================
         * ⏱ COMPLEXITY
         * ================================================================
         *
         * Time  : O(n log n) → due to sorting
         * Space : O(1)       → in-place sorting (ignoring input)
         */

        return totalCost;
    }
}
