import java.util.*;

/**
 * ================================================================
 * 🔥 MAXIMUM RUNNING TIME OF N COMPUTERS — SINGLE REVISION FILE
 * ================================================================
 *
 * 🧠 PROBLEM SUMMARY (Rephrased in simple terms):
 *
 * You are given:
 * - n computers
 * - batteries[] where each value represents how many minutes that battery can run
 *
 * You can:
 * - Attach at most 1 battery to each computer initially
 * - Swap batteries at ANY time (no cost)
 *
 * Goal:
 * 👉 Run ALL n computers simultaneously for MAXIMUM minutes
 *
 * ================================================================
 *
 * 🧠 STEP 1: HOW TO THINK (CRITICAL INTERVIEW STEP)
 *
 * At first glance, it looks like:
 * ❌ Assign batteries to computers (like scheduling / matching problem)
 *
 * BUT:
 * ⚠️ Since swapping is allowed ANY TIME
 *
 * 👉 We are NOT fixing assignments
 * 👉 We are FREE to move energy around
 *
 * So this becomes:
 *
 * 🔥 "Total energy distribution problem"
 *
 * ================================================================
 *
 * 🧠 STEP 2: CORE TRANSFORMATION
 *
 * Let’s assume:
 * 👉 We want to run all computers for T minutes
 *
 * Question becomes:
 *
 * 👉 "Is it possible to run ALL n computers for T minutes?"
 *
 * (Convert optimization → decision problem)
 *
 * ================================================================
 *
 * 🧠 STEP 3: KEY OBSERVATION (VERY IMPORTANT)
 *
 * Each battery contributes:
 *
 * 👉 at most T minutes
 *
 * Why?
 * - A computer only needs T minutes
 * - Even if battery has more, extra is useless
 *
 * So:
 *
 * usable energy from battery[i] = min(battery[i], T)
 *
 * ================================================================
 *
 * 🧠 STEP 4: FEASIBILITY CONDITION
 *
 * Total energy needed = n * T
 *
 * Total usable energy:
 *
 * sum(min(battery[i], T))
 *
 * So:
 *
 * 👉 If sum(min(battery[i], T)) >= n * T
 *      → possible
 * 👉 else
 *      → not possible
 *
 * ================================================================
 *
 * 🧠 STEP 5: WHY BINARY SEARCH ON ANSWER?
 *
 * We are trying to:
 *
 * 👉 MAXIMIZE T
 *
 * Check behavior:
 *
 * If T = 5 is possible
 * → T = 4, 3, 2... also possible
 *
 * If T = 6 is NOT possible
 * → T = 7, 8... also NOT possible
 *
 * So pattern:
 *
 * T T T T F F F F  (Monotonic)
 *
 * 👉 Classic Binary Search on Answer
 *
 * ================================================================
 *
 * 🧠 STEP 6: SEARCH SPACE
 *
 * Minimum time = 0
 *
 * Maximum time:
 * total energy / n
 *
 * Because:
 * even if we evenly distribute all energy
 *
 * ================================================================
 *
 * 🧠 STEP 7: COMPLETE FLOW
 *
 * 1. Calculate total energy
 * 2. Binary search on T
 * 3. For each T → check feasibility
 * 4. Track maximum valid T
 *
 * ================================================================
 *
 * ⏱ TIME COMPLEXITY:
 * O(m * log(total/n))
 *
 * ⏱ SPACE COMPLEXITY:
 * O(1)
 *
 * ================================================================
 */
class MaximumRunningTimeFullGuide {

    public static void main(String[] args) {

        int n = 2;
        int[] batteries = {3, 3, 3};

        long result = maxRunTime(n, batteries);

        System.out.println("Maximum running time = " + result);

        /**
         * ========================================================
         * 🧪 DRY RUN (IMPORTANT FOR INTERVIEW MEMORY)
         * ========================================================
         *
         * Input:
         * n = 2
         * batteries = [3, 3, 3]
         *
         * total energy = 9
         * max possible T = 9 / 2 = 4
         *
         * Binary Search:
         *
         * mid = 2
         * usable = 2+2+2 = 6
         * need = 2*2 = 4
         * → possible
         *
         * mid = 3
         * usable = 3+3+3 = 9
         * need = 2*3 = 6
         * → possible
         *
         * mid = 4
         * usable = 3+3+3 = 9
         * need = 2*4 = 8
         * → possible
         *
         * mid = 5 ❌ (out of bound)
         *
         * Final answer = 4
         *
         * ========================================================
         */
    }

    /**
     * ============================================================
     * 🔍 BINARY SEARCH ON ANSWER
     * ============================================================
     *
     * Goal:
     * 👉 Find maximum T such that all computers run T minutes
     *
     * Pattern:
     * "Maximize minimum time"
     *
     * ============================================================
     */
    public static long maxRunTime(int n, int[] batteries) {

        // Step 1: Calculate total energy available
        long totalEnergy = 0;
        for (int battery : batteries) {
            totalEnergy += battery;
        }

        // Step 2: Define search boundaries
        long low = 0;
        long high = totalEnergy / n;

        // Explicit answer tracking (IMPORTANT pattern for interviews)
        long answer = 0;

        // Step 3: Binary Search
        while (low <= high) {

            long mid = low + (high - low) / 2;

            if (canRun(batteries, n, mid)) {
                /**
                 * If current T is feasible:
                 * → try bigger T
                 */
                answer = mid;
                low = mid + 1;
            } else {
                /**
                 * If not feasible:
                 * → reduce T
                 */
                high = mid - 1;
            }
        }

        return answer;
    }

    /**
     * ============================================================
     * 🔍 FEASIBILITY FUNCTION
     * ============================================================
     *
     * Check if we can run all n computers for 'time' minutes
     *
     * Logic:
     * Each battery contributes min(battery[i], time)
     *
     * ============================================================
     */
    private static boolean canRun(int[] batteries, int n, long time) {

        long usableEnergy = 0;

        for (int battery : batteries) {

            /**
             * Key Idea:
             * Even if battery has more than 'time',
             * we only need 'time'
             */
            usableEnergy += Math.min(battery, time);
        }

        /**
         * Check if total usable energy is enough
         */
        return usableEnergy >= (long) n * time;
    }
}

/**
 * Problem Statement:
 * You have `n` computers and an array of `batteries` where batteries[i] is the minutes 
 * of power that battery provides. You can swap batteries instantaneously. 
 * Find the maximum number of minutes you can run ALL `n` computers simultaneously.
 * 
 * Core Logic (The "Usable Power" Concept):
 * If we want to run `n` computers for `T` minutes, we need exactly `n * T` total power.
 * A single battery cannot power more than one computer at the exact same time.
 * Therefore, if a battery has capacity `C`, and `C > T`, the excess capacity `C - T` 
 * is completely useless for reaching the target `T`. 
 * Thus, the usable power from a single battery for a target time `T` is `Math.min(C, T)`.
 * If the sum of all usable power is >= `n * T`, then target `T` is achievable!
 * 
 * Constraints:
 * - 1 <= n <= batteries.length <= 10^5
 * - 1 <= batteries[i] <= 10^5
 */
class MaximumRunTimeNComputers {

    /**
     * Helper Method: Evaluates if a given target time `T` is possible.
     */
    private static boolean canRun(int n, int[] batteries, long targetTime) {
        long totalUsablePower = 0;
        for (int battery : batteries) {
            totalUsablePower += Math.min((long) battery, targetTime);
        }
        // Can we satisfy all 'n' computers for 'targetTime' minutes?
        return totalUsablePower >= n * targetTime;
    }

    /**
     * SOLUTION 1: Iterative Binary Search on Answer Space (Optimal)
     * 
     * Time Complexity: O(B * log(Sum / N)) where B is number of batteries.
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION & LOGIC:
     * n = 2, batteries = [3, 3, 3]
     * Sum = 9. Max theoretical time = 9 / 2 = 4.
     * Search Space for T: [1, 2, 3, 4]
     * 
     * Iteration 1:
     * low = 1, high = 4. mid (T) = 2.
     * Usable power = min(3, 2) + min(3, 2) + min(3, 2) = 2 + 2 + 2 = 6.
     * Required power = n * T = 2 * 2 = 4.
     * 6 >= 4? YES! 
     * Target 2 is possible. result = 2. Try for a higher time. low = 3.
     * 
     * Iteration 2:
     * low = 3, high = 4. mid (T) = 3.
     * Usable power = min(3, 3) + min(3, 3) + min(3, 3) = 3 + 3 + 3 = 9.
     * Required power = 2 * 3 = 6.
     * 9 >= 6? YES!
     * Target 3 is possible. result = 3. Try for a higher time. low = 4.
     * 
     * Iteration 3:
     * low = 4, high = 4. mid (T) = 4.
     * Usable power = min(3, 4) + min(3, 4) + min(3, 4) = 3 + 3 + 3 = 9.
     * Required power = 2 * 4 = 8.
     * 9 >= 8? YES!
     * Target 4 is possible. result = 4. Try higher. low = 5.
     * 
     * Loop Ends. Result is 4.
     */
    public static long maxRunTimeIterativeBS(int n, int[] batteries) {
        long sum = 0;
        for (int b : batteries) sum += b;

        long low = 1;
        long high = sum / n;
        long result = 0; // Explicit result variable

        while (low <= high) {
            long mid = low + (high - low) / 2;

            if (canRun(n, batteries, mid)) {
                // Time 'mid' is possible, save it and try to find a longer time
                result = mid;
                low = mid + 1;
            } else {
                // Time 'mid' is too long, we don't have enough power
                high = mid - 1;
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search
     * 
     * Time Complexity: O(B * log(Sum / N))
     * Space Complexity: O(log(Sum / N)) - recursive call stack overhead
     * 
     * EXPLANATION:
     * Replicates the binary search logic functionally, passing the `result` 
     * explicitly through the call stack.
     */
    public static long maxRunTimeRecursiveBSWrapper(int n, int[] batteries) {
        long sum = 0;
        for (int b : batteries) sum += b;
        return maxRunTimeRecursiveBS(n, batteries, 1, sum / n, 0);
    }

    private static long maxRunTimeRecursiveBS(int n, int[] batteries, long low, long high, long currentResult) {
        long result = currentResult;

        if (low > high) {
            return result; // Base case
        }

        long mid = low + (high - low) / 2;

        if (canRun(n, batteries, mid)) {
            // Found a valid time, store it and search higher
            result = maxRunTimeRecursiveBS(n, batteries, mid + 1, high, mid);
        } else {
            // Not enough power, search lower
            result = maxRunTimeRecursiveBS(n, batteries, low, mid - 1, result);
        }

        return result;
    }

    /**
     * SOLUTION 3: Greedy Sorting Approach (Alternative Optimal)
     * 
     * Time Complexity: O(B log B) for sorting + O(B) for sweep.
     * Space Complexity: O(1) or O(log B) depending on sorting algorithm internals.
     * 
     * EXPLANATION:
     * This is a beautiful mathematical approach. 
     * If the largest battery provides more power than the theoretical average (`sum / n`), 
     * it means this battery can power one computer indefinitely without needing swaps. 
     * We assign this battery permanently to one computer, decrease `n` by 1, 
     * subtract its power from the total sum, and check the next largest battery.
     * Once the largest remaining battery is <= the new `sum / n`, it implies ALL 
     * remaining batteries can be perfectly multiplexed among the remaining computers.
     * The answer is exactly the final `sum / n`.
     */
    public static long maxRunTimeGreedy(int n, int[] batteries) {
        Arrays.sort(batteries);
        long sum = 0;
        for (int b : batteries) sum += b;

        // Iterate from the largest battery downwards
        for (int i = batteries.length - 1; i >= 0; i--) {
            // If the largest battery exceeds the average time for the remaining computers
            if (batteries[i] > sum / n) {
                sum -= batteries[i]; // Remove it from the shared pool
                n--;                 // This computer is fully taken care of
            } else {
                // All remaining batteries are small enough to be seamlessly swapped
                return sum / n;
            }
        }
        
        return 0;
    }

    /**
     * SOLUTION 4: Pure Java Streams
     * 
     * Time Complexity: O(B log B)
     * Space Complexity: O(B) for Boxed arrays
     * 
     * EXPLANATION:
     * Wraps the Greedy Sorting logic into a purely functional format using Streams.
     */
    public static long maxRunTimeStream(int n, int[] batteries) {
        long sum = Arrays.stream(batteries).asLongStream().sum();
        
        // Sort in descending order to apply the greedy logic functionally
        int[] sortedDesc = Arrays.stream(batteries)
                                 .boxed()
                                 .sorted((a, b) -> Integer.compare(b, a))
                                 .mapToInt(Integer::intValue)
                                 .toArray();
        
        long currentSum = sum;
        int currentN = n;
        
        for (int b : sortedDesc) {
            if (b > currentSum / currentN) {
                currentSum -= b;
                currentN--;
            } else {
                return currentSum / currentN;
            }
        }
        return 0;
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record to cleanly map input scenarios and expected outputs.
     */
    public record TestCase(int n, int[] batteries, long expected) {}

    public static void main(String[] args) {
        // Defined Test Cases spanning examples and edge constraints
        TestCase[] testCases = {
            new TestCase(2, new int[]{3, 3, 3}, 4),                // Example 1
            new TestCase(2, new int[]{1, 1, 1, 1}, 2),             // Example 2
            new TestCase(3, new int[]{10, 10, 3, 5}, 8),           // Large batteries that get excluded in greedy
            new TestCase(1, new int[]{1000}, 1000),                // Single computer
            new TestCase(5, new int[]{1, 2, 3, 4, 5, 6, 7}, 5)     // More batteries than computers, varied sizes
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            long resIterativeBS = maxRunTimeIterativeBS(tc.n(), tc.batteries());
            long resRecursiveBS = maxRunTimeRecursiveBSWrapper(tc.n(), tc.batteries());
            long resGreedy      = maxRunTimeGreedy(tc.n(), tc.batteries());
            long resStream      = maxRunTimeStream(tc.n(), tc.batteries());

            boolean passed = (resIterativeBS == tc.expected()) &&
                             (resRecursiveBS == tc.expected()) &&
                             (resGreedy == tc.expected()) &&
                             (resStream == tc.expected());

            // Limit array printing length for neat terminal output
            String arrStr = Arrays.toString(tc.batteries());
            if (arrStr.length() > 25) arrStr = arrStr.substring(0, 22) + "...]";

            System.out.printf("Test %d | n: %-2d | Batteries: %-25s -> Expected: %-3d | Passed: %b%n",
                    i + 1, tc.n(), arrStr, tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] IterBS: %d, RecBS: %d, Greedy: %d, Stream: %d%n",
                        resIterativeBS, resRecursiveBS, resGreedy, resStream);
            }
        }
    }
}
