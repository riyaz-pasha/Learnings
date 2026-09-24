import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.PriorityQueue;

/**
 * PROBLEM STATEMENT:
 * You are given two integers, n and k, and two integer arrays, speed and efficiency, both of length n. 
 * There are n engineers numbered from 1 to n. 
 * To form a team with the maximum performance, you need to select at most k different engineers.
 * 
 * Performance = (Sum of selected engineers' speeds) * (Minimum efficiency among selected engineers)
 * 
 * Return the maximum performance of the team modulo (10^9 + 7).
 *
 * CONSTRAINTS:
 * 1 <= k <= n <= 10^3  (Note: Typical LeetCode constraints are 10^5, but we optimize for the best anyway)
 * speed.length == n
 * efficiency.length == n
 * 1 <= speed[i] <= 10^3
 * 1 <= efficiency[i] <= 10^4
 * 
 * ==========================================================================================
 * CRITICAL INSIGHT - FIX THE MINIMUM:
 * The formula has two moving parts: a sum of speeds, and a MINIMUM efficiency.
 * Maximizing a product of two independent variable sets is hard. 
 * But what if we FIX one of the variables?
 * 
 * If we sort the engineers by efficiency in DESCENDING order, as we iterate through them, 
 * the CURRENT engineer we are looking at will ALWAYS have the lowest efficiency in our 
 * chosen team so far. 
 * 
 * By "fixing" the current engineer's efficiency as the minimum, our only remaining goal 
 * is to maximize the sum of speeds. To do this, we just need to keep track of the largest 
 * 'k' speeds we have seen so far!
 * ==========================================================================================
 * LATEST JAVA FEATURES USED:
 * - Records (`record Engineer(...)`, `record TestCase(...)`): Clean immutable data carriers.
 * - Local Variable Type Inference (`var`): Cleaner syntax.
 * ==========================================================================================
 */
class MaximizeTeamPerformance {

    private static final int MOD = 1_000_000_007;

    // Record to group an engineer's attributes together
    private record Engineer(int speed, int efficiency) {}

    // ==========================================================================================
    // SOLUTION 1: Sorting + Brute Force Top K-1 Speeds (Educational Stepping Stone)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * 1. Pair speeds and efficiencies, then sort in DESCENDING order of efficiency.
     * 2. Iterate through each engineer. Assume they are the one with the MINIMUM efficiency.
     * 3. To maximize the team's speed, look at all previously visited engineers 
     *    (since they all have >= efficiency) and pick up to k-1 of the largest speeds.
     * 4. Calculate performance and track the maximum.
     *
     * COMPLEXITY:
     * - Time: O(N^2 log N) - For each of the N engineers, we sort an array of up to N previous speeds. 
     *         (This is acceptable ONLY because n <= 1000 in this specific prompt constraint).
     * - Space: O(N) to store the elements and previous speeds.
     */
    public static int maxPerformance_BruteForce(int n, int[] speed, int[] efficiency, int k) {
        var engineers = new Engineer[n];
        for (int i = 0; i < n; i++) {
            engineers[i] = new Engineer(speed[i], efficiency[i]);
        }
        
        // Sort descending by efficiency
        Arrays.sort(engineers, (a, b) -> Integer.compare(b.efficiency(), a.efficiency()));
        
        long maxPerf = 0;
        var previousSpeeds = new ArrayList<Integer>();

        for (var eng : engineers) {
            // Sort previous speeds descending to greedily pick the largest
            previousSpeeds.sort(Collections.reverseOrder());
            
            long currentSpeedSum = eng.speed();
            // Pick up to k-1 largest speeds from the pool of previously seen engineers
            int limit = Math.min(k - 1, previousSpeeds.size());
            for (int j = 0; j < limit; j++) {
                currentSpeedSum += previousSpeeds.get(j);
            }
            
            long currentPerf = currentSpeedSum * eng.efficiency();
            maxPerf = Math.max(maxPerf, currentPerf);
            
            // Add current speed for future engineers to potentially use
            previousSpeeds.add(eng.speed());
        }

        return (int) (maxPerf % MOD);
    }

    // ==========================================================================================
    // SOLUTION 2: Sorting + Min-Heap (The Optimal Approach)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * Instead of re-sorting the previous speeds every single time, we can use a Min-Heap!
     * A Min-Heap naturally maintains the top 'k' elements. 
     * 
     * 1. Sort engineers by efficiency DESCENDING.
     * 2. Maintain a Min-Heap of sizes up to 'k' to store the team's speeds.
     * 3. Maintain a running sum of the speeds in the heap.
     * 4. As we process each engineer:
     *    - Add their speed to the heap and the running sum.
     *    - If the heap size exceeds 'k', remove the SMALLEST speed from the heap and subtract 
     *      it from the running sum. (This greedily ensures we always keep the largest speeds).
     *    - Calculate performance: (running sum) * (current engineer's efficiency).
     *    - Update the absolute maximum performance.
     *
     * VISUAL:
     * n = 3, speed = [2, 10, 3], efficiency = [5, 4, 3], k = 2
     * 
     * Sorted Engineers (by eff DESC): 
     * 1. (s:2, e:5)
     * 2. (s:10, e:4)
     * 3. (s:3, e:3)
     * 
     * Heap progression (max size 2):
     * 
     * Eng 1 (2, 5): 
     *   Add 2. Heap: [2]. Sum = 2. 
     *   Perf = 2 * 5 = 10. (Max: 10)
     * 
     * Eng 2 (10, 4): 
     *   Add 10. Heap: [2, 10]. Sum = 12. 
     *   Perf = 12 * 4 = 48. (Max: 48)
     * 
     * Eng 3 (3, 3): 
     *   Add 3. Heap: [2, 3, 10] -> Size > 2! Remove min (2). 
     *   Heap: [3, 10]. Sum = 12 - 2 + 3 = 13.
     *   Perf = 13 * 3 = 39. (Max remains 48)
     * 
     * Final Max Performance = 48.
     *
     * COMPLEXITY:
     * - Time: O(N log N + N log k). Sorting takes O(N log N). Iterating and pushing to a 
     *         heap of size k takes O(N log k). Overall Time: O(N log N).
     * - Space: O(N + k). N to store paired records, k for the priority queue.
     */
    public static int maxPerformance_MinHeap(int n, int[] speed, int[] efficiency, int k) {
        var engineers = new Engineer[n];
        for (int i = 0; i < n; i++) {
            engineers[i] = new Engineer(speed[i], efficiency[i]);
        }
        
        // Sort descending by efficiency
        Arrays.sort(engineers, (a, b) -> Integer.compare(b.efficiency(), a.efficiency()));
        
        // Min-Heap to keep track of the largest 'k' speeds
        PriorityQueue<Integer> speedMinHeap = new PriorityQueue<>(k);
        
        long maxPerformance = 0;
        long currentSpeedSum = 0;
        
        for (var eng : engineers) {
            // Add current engineer's speed to the team
            speedMinHeap.offer(eng.speed());
            currentSpeedSum += eng.speed();
            
            // If team is too large, kick out the slowest engineer
            if (speedMinHeap.size() > k) {
                currentSpeedSum -= speedMinHeap.poll();
            }
            
            // Calculate performance with current engineer as the bottleneck (minimum efficiency)
            long currentPerformance = currentSpeedSum * eng.efficiency();
            maxPerformance = Math.max(maxPerformance, currentPerformance);
        }
        
        return (int) (maxPerformance % MOD);
    }

    // ==========================================================================================
    // TESTING SUITE
    // ==========================================================================================
    
    public record TestCase(int n, int[] speed, int[] efficiency, int k, int expected) {}

    public static void main(String[] args) {
        var testCases = new TestCase[] {
            // Test Case 1: Standard case
            new TestCase(6, new int[]{2, 10, 3, 1, 5, 8}, new int[]{5, 4, 3, 9, 7, 2}, 2, 60), 
            // Select eng with speed 10, eff 4 and eng with speed 5, eff 7. Sum = 15. Min eff = 4. 15 * 4 = 60.

            // Test Case 2: Same arrays, higher k limit
            new TestCase(6, new int[]{2, 10, 3, 1, 5, 8}, new int[]{5, 4, 3, 9, 7, 2}, 3, 68),
            
            // Test Case 3: Pick all engineers limit
            new TestCase(6, new int[]{2, 10, 3, 1, 5, 8}, new int[]{5, 4, 3, 9, 7, 2}, 4, 72),
            
            // Test Case 4: K = 1, just pick the single highest product
            new TestCase(3, new int[]{2, 8, 2}, new int[]{2, 7, 1}, 1, 56),

            // Test Case 5: Large arrays/values checking Modulo logic
            // speeds and efficiencies are high enough that their sum*min exceeds int boundary
            new TestCase(2, new int[]{100000, 100000}, new int[]{100000, 100000}, 2, 999999986) 
            // Sum = 200,000. Min Eff = 100,000. Perf = 20,000,000,000.
            // 20,000,000,000 % 1,000,000,007 = 999999986.
        };

        System.out.println("Running Maximum Team Performance Tests...\n");

        for (int i = 0; i < testCases.length; i++) {
            var tc = testCases[i];
            System.out.printf("Test Case %d:\n", i + 1);
            System.out.printf("k = %d | Expected: %d\n", tc.k(), tc.expected());

            // Run Brute Force Solution
            int res1 = maxPerformance_BruteForce(tc.n(), tc.speed(), tc.efficiency(), tc.k());
            boolean pass1 = (res1 == tc.expected());
            System.out.printf("  [1. Brute Force] Result: %d -> %s\n", res1, pass1 ? "PASS" : "FAIL");

            // Run Min-Heap Solution
            int res2 = maxPerformance_MinHeap(tc.n(), tc.speed(), tc.efficiency(), tc.k());
            boolean pass2 = (res2 == tc.expected());
            System.out.printf("  [2. Min-Heap   ] Result: %d -> %s\n", res2, pass2 ? "PASS" : "FAIL");

            System.out.println("-".repeat(60));
        }
    }
}

class Solution {

    /**
     * RECORD (Java 16+) → Cleaner representation of Engineer
     * Immutable, readable, perfect for pairing data
     */
    record Engineer(int efficiency, int speed) {}

    public int maxPerformance(int n, int[] speed, int[] efficiency, int k) {

        /**
         * ============================================================
         * 🧠 INTUITION
         * ============================================================
         * Performance = (sum of speeds) * (minimum efficiency)
         *
         * KEY OBSERVATION:
         * The "minimum efficiency" dominates the formula.
         *
         * 👉 Instead of picking arbitrary teams,
         *    we FIX the minimum efficiency first.
         *
         * How?
         * → Sort engineers by efficiency DESC
         * → At any step, current engineer defines the MIN efficiency
         *
         * Now problem reduces to:
         * 👉 "Pick up to k engineers with MAX total speed so far"
         *
         * → This is where HEAP comes in
         */

        /**
         * ============================================================
         * 🧱 STEP 1: Build Engineer List
         * ============================================================
         */
        List<Engineer> engineers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            engineers.add(new Engineer(efficiency[i], speed[i]));
        }

        /**
         * ============================================================
         * 🔽 STEP 2: Sort by Efficiency DESC
         * ============================================================
         * Why?
         * → So when we iterate:
         *    current engineer's efficiency is ALWAYS the minimum
         *    among selected team members
         */
        engineers.sort((a, b) -> Integer.compare(b.efficiency(), a.efficiency()));
        // engineers.sort(Comparator.comparingInt(Engineer::efficiency).reversed());

        /**
         * ============================================================
         * 🧰 STEP 3: Min Heap for Speeds
         * ============================================================
         * Why MIN heap?
         * → We want to keep TOP k speeds
         * → If size exceeds k, remove SMALLEST speed
         */
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();

        long speedSum = 0;       // running sum of selected speeds
        long maxPerformance = 0; // answer

        /**
         * ============================================================
         * 🔁 STEP 4: Iterate Engineers
         * ============================================================
         */
        for (Engineer eng : engineers) {

            int currEfficiency = eng.efficiency();
            int currSpeed = eng.speed();

            /**
             * ➕ Add current engineer
             */
            minHeap.offer(currSpeed);
            speedSum += currSpeed;

            /**
             * ❌ If more than k engineers → remove smallest speed
             */
            if (minHeap.size() > k) {
                speedSum -= minHeap.poll();
            }

            /**
             * 📊 Calculate performance
             *
             * IMPORTANT:
             * current efficiency = MIN efficiency in team
             */
            long performance = speedSum * currEfficiency;

            maxPerformance = Math.max(maxPerformance, performance);

            /**
             * ========================================================
             * 🧪 TRACE (Example Thought Process)
             * ========================================================
             * Suppose:
             * speeds = [2, 10, 3]
             * efficiency = [5, 4, 3]
             *
             * After sorting:
             * (5,2), (4,10), (3,3)
             *
             * Iteration 1:
             * heap = [2], sum = 2
             * performance = 2 * 5 = 10
             *
             * Iteration 2:
             * heap = [2,10], sum = 12
             * performance = 12 * 4 = 48
             *
             * Iteration 3:
             * heap = [2,10,3] → remove 2
             * heap = [3,10], sum = 13
             * performance = 13 * 3 = 39
             *
             * Max = 48
             */
        }

        /**
         * ============================================================
         * 🧮 FINAL RESULT
         * ============================================================
         */
        return (int) (maxPerformance % 1_000_000_007);
    }
}
