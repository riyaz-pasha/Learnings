import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * PROBLEM STATEMENT:
 * You are given n workers, each characterized by two attributes:
 * - quality[i]: Represents the work quality of the ith worker.
 * - wage[i]: Represents the minimum wage expectation of the ith worker.
 * 
 * You want to hire exactly k workers to form a paid group, and you must follow these payment rules:
 * 1. Wage expectation: Every worker in the group must be paid at least their minimum wage expectation.
 * 2. Proportional pay: The pay for each worker must be directly proportional to their quality.
 * 
 * Your goal is to determine the least amount of money required to hire exactly k workers 
 * while satisfying the above conditions.
 *
 * CONSTRAINTS:
 * n == quality.length == wage.length
 * 1 <= k <= n <= 10^3
 * 1 <= quality[i], wage[i] <= 10^3
 * 
 * ==========================================================================================
 * CRITICAL INSIGHT - "WAGE-TO-QUALITY RATIO":
 * To pay everyone proportionally to their quality, all chosen workers must be paid at the 
 * SAME "rate" (wage per unit of quality).
 * 
 * Let's say we pick a group of k workers. What must the rate be?
 * If we pick worker 'i', their minimum rate is (wage[i] / quality[i]).
 * To satisfy EVERY worker in the chosen group, the group's rate must be the MAXIMUM of 
 * the individual minimum rates in that group.
 * 
 * Therefore, Total Cost = (Maximum Rate in Group) * (Sum of Qualities in Group).
 * 
 * To minimize this cost, we can:
 * 1. Sort workers by their rate (wage / quality) in ascending order.
 * 2. As we iterate, treat each worker as the "captain" who dictates the group's maximum rate.
 * 3. To minimize the (Sum of Qualities) for this rate, we just need to keep the k workers 
 *    with the SMALLEST qualities among all workers seen so far.
 * ==========================================================================================
 * LATEST JAVA FEATURES USED:
 * - Records (`record Worker(...)`): Introduced in Java 14/16 to create immutable data carriers easily.
 * - Local Variable Type Inference (`var`): Introduced in Java 10 for cleaner code.
 * ==========================================================================================
 */
class MinimumCostToHireKWorkers {

    // Record to hold worker data cleanly
    private record Worker(int quality, int wage, double ratio) implements Comparable<Worker> {
        @Override
        public int compareTo(Worker other) {
            return Double.compare(this.ratio, other.ratio);
        }
    }

    // ==========================================================================================
    // SOLUTION 1: Sorting + Brute Force / Naive (Simpler to grasp conceptually)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * 1. Calculate the wage/quality ratio for each worker and sort them by this ratio.
     * 2. For every possible worker acting as the "rate-setter" (from index k-1 to n-1):
     *    a) Their ratio becomes the group's pay rate.
     *    b) Look at all workers up to this index (since their ratio <= current rate).
     *    c) Sort those available workers by quality in ascending order.
     *    d) Pick the 'k' smallest qualities and calculate the total cost.
     * 3. Track the minimum cost found.
     *
     * COMPLEXITY:
     * - Time: O(N^2 log N) - For each of the N workers, we might sort up to N elements.
     * - Space: O(N) - To hold the workers and temporary lists.
     */
    public static double mincostToHireWorkers_BruteForce(int[] quality, int[] wage, int k) {
        int n = quality.length;
        var workers = new Worker[n];
        for (int i = 0; i < n; i++) {
            workers[i] = new Worker(quality[i], wage[i], (double) wage[i] / quality[i]);
        }

        Arrays.sort(workers);
        double minCost = Double.MAX_VALUE;

        // Start checking from index k-1 because we need at least k workers
        for (int i = k - 1; i < n; i++) {
            double currentRate = workers[i].ratio();
            
            // Collect all valid workers' qualities seen so far
            var validQualities = new ArrayList<Integer>();
            for (int j = 0; j <= i; j++) {
                validQualities.add(workers[j].quality());
            }
            
            // Sort to pick the smallest qualities
            Collections.sort(validQualities);
            
            int qualitySum = 0;
            for (int j = 0; j < k; j++) {
                qualitySum += validQualities.get(j);
            }
            
            minCost = Math.min(minCost, qualitySum * currentRate);
        }

        return minCost;
    }

    // ==========================================================================================
    // SOLUTION 2: Sorting + Max-Heap (Optimal Approach)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * Sorting the available qualities at every step is slow. 
     * Instead, we maintain a Max-Heap of qualities to represent our chosen group.
     * 
     * 1. Sort workers by their wage/quality ratio ascending.
     * 2. Iterate through the workers. Add their quality to a Max-Heap and to a running sum.
     * 3. If the Max-Heap exceeds size 'k', remove the largest quality from the heap 
     *    and subtract it from the running sum.
     * 4. Once the heap has exactly 'k' elements, the cost with the current worker's rate 
     *    (which is the maximum rate in the heap) is: (Running Sum) * (Current Worker's Rate).
     * 5. Record the minimum cost found across all iterations.
     *
     * VISUAL:
     * quality = [10, 20, 5], wage = [70, 50, 30], k = 2
     * 
     * Ratios:
     * Worker 0: 70/10 = 7.0
     * Worker 1: 50/20 = 2.5
     * Worker 2: 30/5  = 6.0
     * 
     * Sorted Workers (by ratio):
     * W1: (Q=20, W=50, R=2.5)
     * W2: (Q=5,  W=30, R=6.0)
     * W0: (Q=10, W=70, R=7.0)
     * 
     * Heap Max Size = 2.
     * 
     * Step 1: Process W1
     *   Add Q=20. Heap: [20]. Sum = 20. (Size < k, skip cost check)
     * 
     * Step 2: Process W2
     *   Add Q=5. Heap: [20, 5]. Sum = 25.
     *   Size == 2! Cost = Sum(25) * Rate(6.0) = 150.0. MinCost = 150.0.
     * 
     * Step 3: Process W0
     *   Add Q=10. Heap: [20, 10, 5]. Size > 2, pop max (20).
     *   Heap: [10, 5]. Sum = 25 - 20 + 10 = 15.
     *   Size == 2! Cost = Sum(15) * Rate(7.0) = 105.0. MinCost = Math.min(150.0, 105.0) = 105.0.
     * 
     * Final Min Cost: 105.0.
     *
     * COMPLEXITY:
     * - Time: O(N log N) - Sorting takes O(N log N). Maintaining a Max-Heap of size k takes O(N log k).
     * - Space: O(N + k) - N for the worker array, k for the Max-Heap.
     */
    public static double mincostToHireWorkers_MaxHeap(int[] quality, int[] wage, int k) {
        int n = quality.length;
        var workers = new Worker[n];
        for (int i = 0; i < n; i++) {
            workers[i] = new Worker(quality[i], wage[i], (double) wage[i] / quality[i]);
        }

        // Sort ascending by ratio
        Arrays.sort(workers);

        // Max-Heap to keep the smallest qualities (we pop the max out)
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        
        double minCost = Double.MAX_VALUE;
        int currentQualitySum = 0;

        for (var worker : workers) {
            maxHeap.offer(worker.quality());
            currentQualitySum += worker.quality();

            // If we have more than k workers, fire the one with the highest quality
            if (maxHeap.size() > k) {
                currentQualitySum -= maxHeap.poll();
            }

            // Once we have exactly k workers, calculate the potential cost
            if (maxHeap.size() == k) {
                double currentCost = currentQualitySum * worker.ratio();
                minCost = Math.min(minCost, currentCost);
            }
        }

        return minCost;
    }

    // ==========================================================================================
    // TESTING SUITE
    // ==========================================================================================
    
    public record TestCase(int[] quality, int[] wage, int k, double expected) {}

    public static void main(String[] args) {
        var testCases = new TestCase[] {
            new TestCase(new int[]{10, 20, 5}, new int[]{70, 50, 30}, 2, 105.00000),
            new TestCase(new int[]{3, 1, 10, 10, 1}, new int[]{4, 8, 2, 2, 7}, 3, 30.666666666666664),
            new TestCase(new int[]{14, 56, 59, 89, 39, 26, 86, 76, 3, 23}, 
                         new int[]{134, 159, 274, 260, 202, 234, 432, 282, 162, 46}, 3, 282.80)
        };

        System.out.println("Running Minimum Cost to Hire K Workers Tests...\n");

        for (int i = 0; i < testCases.length; i++) {
            var tc = testCases[i];
            System.out.printf("Test Case %d:\n", i + 1);
            System.out.printf("Quality: %s\nWage:    %s\nk: %d\n", 
                              Arrays.toString(tc.quality()), Arrays.toString(tc.wage()), tc.k());

            // Run Brute Force Solution
            double res1 = mincostToHireWorkers_BruteForce(tc.quality(), tc.wage(), tc.k());
            // Floating point tolerance check (delta = 1e-5)
            boolean pass1 = Math.abs(res1 - tc.expected()) < 1e-5;
            System.out.printf("  [1. Brute Force] Result: %.5f -> %s\n", res1, pass1 ? "PASS" : "FAIL");

            // Run Max-Heap Solution
            double res2 = mincostToHireWorkers_MaxHeap(tc.quality(), tc.wage(), tc.k());
            boolean pass2 = Math.abs(res2 - tc.expected()) < 1e-5;
            System.out.printf("  [2. Max-Heap   ] Result: %.5f -> %s\n", res2, pass2 ? "PASS" : "FAIL");

            System.out.println("-".repeat(60));
        }
    }
}


/**
 * Problem: Minimum Cost to Hire K Workers
 *
 * Core Idea:
 * ----------
 * 1. Pay must be proportional to quality:
 *      pay[i] = quality[i] * R
 *
 * 2. Each worker has minimum wage:
 *      quality[i] * R >= wage[i]
 *      => R >= wage[i] / quality[i]
 *
 * 3. For a group:
 *      R must be the MAX ratio among chosen workers
 *
 * 4. Total Cost:
 *      cost = R * (sum of qualities)
 *
 * ---------------------------------------------------
 * 🔥 Key Transformation:
 * Instead of selecting workers directly,
 * we FIX the maximum ratio (R) and try to minimize total quality.
 *
 * ---------------------------------------------------
 * 🧠 Strategy:
 * 1. Sort workers by ratio (ascending)
 * 2. Iterate and assume current worker defines R
 * 3. Maintain k workers with smallest qualities using max heap
 * 4. Compute cost when heap size == k
 *
 * ---------------------------------------------------
 * 🧠 Interview Thinking:
 * - "Proportional" → think ratio
 * - "Constraint" → max ratio dominates
 * - "Minimize cost" → minimize sum of quality
 * - Use heap to dynamically maintain best k workers
 */
class Solution {

    /**
     * Using record (Java 16+) for cleaner immutable data structure
     */
    record Worker(int quality, int wage, double ratio) {}

    public double mincostToHireWorkers(int[] quality, int[] wage, int k) {

        int n = quality.length;

        // Step 1: Build Worker list with ratio
        // ratio = wage / quality → minimum R required for that worker
        List<Worker> workers = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            workers.add(new Worker(
                    quality[i],
                    wage[i],
                    (double) wage[i] / quality[i]
            ));
        }

        // Step 2: Sort workers by ratio (ascending)
        // Why?
        // We will treat each worker as the "highest ratio (R)" in the group
        workers.sort(Comparator.comparingDouble(w -> w.ratio));

        // Max heap to keep track of k smallest qualities
        // Why max heap?
        // Because if size > k → we remove the LARGEST quality
        PriorityQueue<Integer> maxHeap =
                new PriorityQueue<>(Comparator.reverseOrder());

        int totalQuality = 0;     // running sum of selected qualities
        double minCost = Double.MAX_VALUE;

        // Step 3: Iterate through sorted workers
        for (Worker w : workers) {

            // Include current worker in the group
            maxHeap.offer(w.quality);
            totalQuality += w.quality;

            // If we exceed k workers, remove the one with highest quality
            // Why? We want minimum sum of quality
            if (maxHeap.size() > k) {
                totalQuality -= maxHeap.poll();
            }

            // When we have exactly k workers
            if (maxHeap.size() == k) {

                /**
                 * At this point:
                 * - Current worker defines the MAX ratio (R)
                 * - All workers in heap have ratio <= current ratio
                 *
                 * So:
                 *   cost = R * totalQuality
                 */
                double cost = totalQuality * w.ratio;

                minCost = Math.min(minCost, cost);
            }
        }

        return minCost;
    }
}
