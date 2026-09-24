class MinimumDaysBouquets {

    // Approach 1: Binary Search on Days (Optimal)
    // Time: O(n * log(max)), Space: O(1)
    public int minDays1(int[] bloomDay, int m, int k) {
        // Check if it's possible to make m bouquets
        int n = bloomDay.length;
        if ((long) m * k > n) {
            return -1; // Not enough flowers
        }

        // Binary search on days
        int left = getMin(bloomDay);
        int right = getMax(bloomDay);

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (canMakeBouquets(bloomDay, m, k, mid)) {
                // Can make m bouquets by day mid, try earlier
                right = mid;
            } else {
                // Cannot make m bouquets, need more days
                left = mid + 1;
            }
        }

        return left;
    }

    // Helper: Check if we can make m bouquets by day 'day'
    private boolean canMakeBouquets(int[] bloomDay, int m, int k, int day) {
        int bouquets = 0;
        int consecutiveFlowers = 0;

        for (int bloom : bloomDay) {
            if (bloom <= day) {
                // Flower has bloomed
                consecutiveFlowers++;
                if (consecutiveFlowers == k) {
                    // Made one bouquet
                    bouquets++;
                    consecutiveFlowers = 0;

                    // Early termination
                    if (bouquets >= m) {
                        return true;
                    }
                }
            } else {
                // Flower hasn't bloomed, reset consecutive count
                consecutiveFlowers = 0;
            }
        }

        return bouquets >= m;
    }

    private int getMin(int[] arr) {
        int min = arr[0];
        for (int val : arr) {
            min = Math.min(min, val);
        }
        return min;
    }

    private int getMax(int[] arr) {
        int max = arr[0];
        for (int val : arr) {
            max = Math.max(max, val);
        }
        return max;
    }

    // Approach 2: Binary Search with Cleaner Logic
    // Time: O(n * log(max)), Space: O(1)
    public int minDays2(int[] bloomDay, int m, int k) {
        if ((long) m * k > bloomDay.length) {
            return -1;
        }

        int left = 1;
        int right = 1_000_000_000; // Max possible day
        int result = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (canMakeBouquets(bloomDay, m, k, mid)) {
                result = mid;
                right = mid - 1; // Try earlier day
            } else {
                left = mid + 1;
            }
        }

        return result;
    }

    // Approach 3: Binary Search with Detailed Bouquet Counting
    // Time: O(n * log(max)), Space: O(1)
    public int minDays3(int[] bloomDay, int m, int k) {
        int n = bloomDay.length;

        // Impossible case
        if (m > n / k) {
            return -1;
        }

        // Find search range
        int minDay = Integer.MAX_VALUE;
        int maxDay = Integer.MIN_VALUE;
        for (int day : bloomDay) {
            minDay = Math.min(minDay, day);
            maxDay = Math.max(maxDay, day);
        }

        int left = minDay;
        int right = maxDay;

        while (left < right) {
            int mid = left + (right - left) / 2;
            int bouquetsCount = countBouquets(bloomDay, k, mid);

            if (bouquetsCount >= m) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }

        return left;
    }

    // Count how many bouquets we can make by day 'day'
    private int countBouquets(int[] bloomDay, int k, int day) {
        int bouquets = 0;
        int flowers = 0;

        for (int bloom : bloomDay) {
            if (bloom <= day) {
                flowers++;
                if (flowers == k) {
                    bouquets++;
                    flowers = 0;
                }
            } else {
                flowers = 0;
            }
        }

        return bouquets;
    }

    // Approach 4: Binary Search with Visualization Helper
    // Time: O(n * log(max)), Space: O(1)
    public int minDays4(int[] bloomDay, int m, int k) {
        if ((long) m * k > bloomDay.length) {
            return -1;
        }

        int left = getMin(bloomDay);
        int right = getMax(bloomDay);
        int answer = right;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (isPossible(bloomDay, m, k, mid)) {
                answer = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return answer;
    }

    private boolean isPossible(int[] bloomDay, int m, int k, int day) {
        int totalBouquets = 0;
        int currentFlowers = 0;

        for (int i = 0; i < bloomDay.length; i++) {
            if (bloomDay[i] <= day) {
                currentFlowers++;

                if (currentFlowers == k) {
                    totalBouquets++;
                    currentFlowers = 0;
                }
            } else {
                currentFlowers = 0;
            }
        }

        return totalBouquets >= m;
    }

    // Test cases with detailed explanation
    public static void main(String[] args) {
        MinimumDaysBouquets solution = new MinimumDaysBouquets();

        // Test Case 1
        int[] bloomDay1 = { 1, 10, 3, 10, 2 };
        int m1 = 3, k1 = 1;
        int result1 = solution.minDays1(bloomDay1, m1, k1);
        System.out.println("Test 1: " + result1); // Output: 3
        explainTest(bloomDay1, m1, k1, result1);

        // Test Case 2: Impossible
        int[] bloomDay2 = { 1, 10, 3, 10, 2 };
        int m2 = 3, k2 = 2;
        int result2 = solution.minDays1(bloomDay2, m2, k2);
        System.out.println("\nTest 2: " + result2); // Output: -1
        System.out.println("Impossible: need " + (m2 * k2) +
                " flowers, only have " + bloomDay2.length);

        // Test Case 3
        int[] bloomDay3 = { 7, 7, 7, 7, 12, 7, 7 };
        int m3 = 2, k3 = 3;
        int result3 = solution.minDays1(bloomDay3, m3, k3);
        System.out.println("\nTest 3: " + result3); // Output: 12
        explainTest(bloomDay3, m3, k3, result3);

        // Test Case 4: All bloom same day
        int[] bloomDay4 = { 5, 5, 5, 5, 5, 5 };
        int m4 = 2, k4 = 3;
        int result4 = solution.minDays1(bloomDay4, m4, k4);
        System.out.println("\nTest 4: " + result4); // Output: 5

        // Test Case 5: Need all flowers
        int[] bloomDay5 = { 1, 2, 3, 4, 5 };
        int m5 = 1, k5 = 5;
        int result5 = solution.minDays1(bloomDay5, m5, k5);
        System.out.println("Test 5: " + result5); // Output: 5
    }

    private static void explainTest(int[] bloomDay, int m, int k, int days) {
        System.out.println("BloomDay: " + java.util.Arrays.toString(bloomDay));
        System.out.println("Need " + m + " bouquets, each with " + k + " adjacent flowers");
        System.out.println("Answer: " + days + " days");

        // Show state after 'days'
        System.out.print("After day " + days + ": [");
        for (int i = 0; i < bloomDay.length; i++) {
            if (i > 0)
                System.out.print(", ");
            System.out.print(bloomDay[i] <= days ? "x" : "_");
        }
        System.out.println("]");

        // Count bouquets
        int bouquets = 0;
        int consecutive = 0;
        for (int day : bloomDay) {
            if (day <= days) {
                consecutive++;
                if (consecutive == k) {
                    bouquets++;
                    consecutive = 0;
                }
            } else {
                consecutive = 0;
            }
        }
        System.out.println("Can make " + bouquets + " bouquets");
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM ANALYSIS:
 * - Garden has n flowers, flower i blooms on day bloomDay[i]
 * - Need to make m bouquets, each with k adjacent flowers
 * - Find minimum days to wait to make m bouquets
 * - Return -1 if impossible
 * 
 * KEY INSIGHTS:
 * 1. Impossible check: if m * k > n, impossible (need more flowers than
 * available)
 * 2. Binary search on days: search space is [min(bloomDay), max(bloomDay)]
 * 3. Monotonic property: if we can make m bouquets on day d, we can also on day
 * d+1
 * 4. For each candidate day, count consecutive bloomed flowers
 * 
 * ALGORITHM:
 * 1. Check if possible: m * k <= n
 * 2. Binary search on days in range [min, max]
 * 3. For each day, simulate and count bouquets:
 * - Iterate through flowers
 * - Count consecutive bloomed flowers
 * - When we reach k consecutive, we have 1 bouquet
 * - Reset counter when we hit unbloomed flower
 * 4. If bouquets >= m: try earlier day (search left)
 * 5. If bouquets < m: need more days (search right)
 * 
 * EXAMPLE WALKTHROUGH: bloomDay=[1,10,3,10,2], m=3, k=1
 * 
 * Initial: left=1, right=10
 * 
 * Iteration 1: mid=5
 * After day 5: [x, _, x, _, x] (flowers bloomed: 1,3,2)
 * Consecutive groups of 1: positions 0, 2, 4 → 3 bouquets ✓
 * Can make 3 bouquets, try earlier: right=5
 * 
 * Iteration 2: left=1, right=5, mid=3
 * After day 3: [x, _, x, _, x] (flowers bloomed: 1,3,2)
 * Consecutive groups of 1: positions 0, 2, 4 → 3 bouquets ✓
 * Can make 3 bouquets, try earlier: right=3
 * 
 * Iteration 3: left=1, right=3, mid=2
 * After day 2: [x, _, _, _, x] (flowers bloomed: 1,2)
 * Consecutive groups of 1: positions 0, 4 → 2 bouquets ✗
 * Cannot make 3 bouquets, need more days: left=3
 * 
 * Result: left=3, right=3 → Answer: 3
 * 
 * EXAMPLE 2: bloomDay=[7,7,7,7,12,7,7], m=2, k=3
 * 
 * After day 7: [x,x,x,x,_,x,x]
 * - First 4 consecutive: one bouquet [0-2]
 * - Position 4 not bloomed: breaks chain
 * - Last 2 consecutive: not enough for bouquet
 * - Total: 1 bouquet (need 2) ✗
 * 
 * After day 12: [x,x,x,x,x,x,x]
 * - First 3: one bouquet [0-2]
 * - Next 3: one bouquet [3-5] or [4-6]
 * - Total: 2 bouquets ✓
 * 
 * Answer: 12
 * 
 * COMPLEXITY ANALYSIS:
 * - Time: O(n * log(max)) where n = number of flowers, max = max bloom day
 * - Binary search: log(max) iterations
 * - Each iteration: O(n) to check all flowers
 * - Space: O(1) - constant extra space
 * 
 * EDGE CASES:
 * 1. Impossible: m * k > n → return -1
 * 2. Need all flowers: m * k = n → must wait for last flower
 * 3. k = 1: any bloomed flower can be a bouquet
 * 4. All flowers bloom same day: answer = that day (if possible)
 * 5. m = 1: only need one bouquet, easier to achieve
 * 
 * WHY BINARY SEARCH WORKS:
 * Monotonicity: If we can make m bouquets on day d, we can also make them on
 * any day after d.
 * Pattern: [NO, NO, ..., NO, YES, YES, ..., YES]
 * We want the first YES = minimum day when it becomes possible.
 * 
 * OPTIMIZATION NOTES:
 * 1. Early termination in canMakeBouquets when bouquets >= m
 * 2. Use long for m * k to avoid integer overflow
 * 3. Can optimize min/max finding by combining in single pass
 */
