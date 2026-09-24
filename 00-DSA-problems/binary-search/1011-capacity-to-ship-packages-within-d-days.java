class ShipCapacity {

    // Approach 1: Binary Search on Capacity (Optimal)
    // Time: O(n * log(sum)), Space: O(1)
    public int shipWithinDays1(int[] weights, int days) {
        int left = getMax(weights); // Min capacity = heaviest package
        int right = getSum(weights); // Max capacity = ship all in 1 day

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (canShip(weights, days, mid)) {
                // Can ship with capacity mid, try smaller
                right = mid;
            } else {
                // Cannot ship, need larger capacity
                left = mid + 1;
            }
        }

        return left;
    }

    // Helper: Check if we can ship within 'days' with given capacity
    private boolean canShip(int[] weights, int days, int capacity) {
        int daysNeeded = 1;
        int currentLoad = 0;

        for (int weight : weights) {
            if (currentLoad + weight > capacity) {
                // Start new day
                daysNeeded++;
                currentLoad = weight;

                // Early termination
                if (daysNeeded > days) {
                    return false;
                }
            } else {
                // Add to current day
                currentLoad += weight;
            }
        }

        return daysNeeded <= days;
    }

    private int getMax(int[] arr) {
        int max = arr[0];
        for (int val : arr) {
            max = Math.max(max, val);
        }
        return max;
    }

    private int getSum(int[] arr) {
        int sum = 0;
        for (int val : arr) {
            sum += val;
        }
        return sum;
    }

    // Approach 2: Binary Search with Detailed Day Counting
    // Time: O(n * log(sum)), Space: O(1)
    public int shipWithinDays2(int[] weights, int days) {
        int minCapacity = 0;
        int maxCapacity = 0;

        for (int weight : weights) {
            minCapacity = Math.max(minCapacity, weight);
            maxCapacity += weight;
        }

        int left = minCapacity;
        int right = maxCapacity;

        while (left < right) {
            int mid = left + (right - left) / 2;
            int daysRequired = calculateDays(weights, mid);

            if (daysRequired <= days) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }

        return left;
    }

    private int calculateDays(int[] weights, int capacity) {
        int days = 1;
        int currentWeight = 0;

        for (int weight : weights) {
            if (currentWeight + weight > capacity) {
                days++;
                currentWeight = weight;
            } else {
                currentWeight += weight;
            }
        }

        return days;
    }

    // Approach 3: Binary Search with Alternative Logic
    // Time: O(n * log(sum)), Space: O(1)
    public int shipWithinDays3(int[] weights, int days) {
        int left = 1;
        int right = 50000 * 500; // Max possible sum based on constraints
        int result = right;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (isPossible(weights, days, mid)) {
                result = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return result;
    }

    private boolean isPossible(int[] weights, int days, int capacity) {
        int daysUsed = 1;
        int load = 0;

        for (int weight : weights) {
            // Check if single package exceeds capacity
            if (weight > capacity) {
                return false;
            }

            if (load + weight <= capacity) {
                load += weight;
            } else {
                daysUsed++;
                load = weight;
            }
        }

        return daysUsed <= days;
    }

    // Approach 4: Binary Search with Greedy Loading
    // Time: O(n * log(sum)), Space: O(1)
    public int shipWithinDays4(int[] weights, int days) {
        int low = 0, high = 0;

        for (int weight : weights) {
            low = Math.max(low, weight);
            high += weight;
        }

        while (low < high) {
            int mid = low + (high - low) / 2;

            if (needsDays(weights, mid) <= days) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }

        return low;
    }

    private int needsDays(int[] weights, int capacity) {
        int days = 1, currentCapacity = 0;

        for (int weight : weights) {
            currentCapacity += weight;
            if (currentCapacity > capacity) {
                days++;
                currentCapacity = weight;
            }
        }

        return days;
    }

    // Test cases with detailed visualization
    public static void main(String[] args) {
        ShipCapacity solution = new ShipCapacity();

        // Test Case 1
        int[] weights1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int days1 = 5;
        int result1 = solution.shipWithinDays1(weights1, days1);
        System.out.println("Test 1: " + result1); // Output: 15
        visualizeShipping(weights1, days1, result1);

        // Test Case 2
        int[] weights2 = { 3, 2, 2, 4, 1, 4 };
        int days2 = 3;
        int result2 = solution.shipWithinDays1(weights2, days2);
        System.out.println("\nTest 2: " + result2); // Output: 6
        visualizeShipping(weights2, days2, result2);

        // Test Case 3
        int[] weights3 = { 1, 2, 3, 1, 1 };
        int days3 = 4;
        int result3 = solution.shipWithinDays1(weights3, days3);
        System.out.println("\nTest 3: " + result3); // Output: 3
        visualizeShipping(weights3, days3, result3);

        // Test Case 4: Single package
        int[] weights4 = { 10 };
        int days4 = 1;
        int result4 = solution.shipWithinDays1(weights4, days4);
        System.out.println("\nTest 4: " + result4); // Output: 10

        // Test Case 5: All same weight
        int[] weights5 = { 5, 5, 5, 5 };
        int days5 = 2;
        int result5 = solution.shipWithinDays1(weights5, days5);
        System.out.println("Test 5: " + result5); // Output: 10
        visualizeShipping(weights5, days5, result5);
    }

    private static void visualizeShipping(int[] weights, int days, int capacity) {
        System.out.println("Weights: " + java.util.Arrays.toString(weights));
        System.out.println("Days available: " + days);
        System.out.println("Ship capacity: " + capacity);
        System.out.println("Shipping schedule:");

        int day = 1;
        int currentLoad = 0;
        StringBuilder schedule = new StringBuilder();

        for (int i = 0; i < weights.length; i++) {
            if (currentLoad + weights[i] > capacity) {
                // Print current day
                System.out.println("Day " + day + ": " + schedule.toString() +
                        " (total: " + currentLoad + ")");
                day++;
                schedule = new StringBuilder();
                currentLoad = 0;
            }

            if (schedule.length() > 0) {
                schedule.append(", ");
            }
            schedule.append(weights[i]);
            currentLoad += weights[i];
        }

        // Print last day
        if (schedule.length() > 0) {
            System.out.println("Day " + day + ": " + schedule.toString() +
                    " (total: " + currentLoad + ")");
        }

        System.out.println("Total days used: " + day);
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM ANALYSIS:
 * - Ship packages in order (no reordering allowed)
 * - Each day, load packages until capacity reached
 * - Find minimum capacity to ship all within 'days' days
 * 
 * KEY INSIGHTS:
 * 1. Search space for capacity:
 * - Minimum: max(weights) - must handle heaviest package
 * - Maximum: sum(weights) - ship everything in 1 day
 * 
 * 2. Binary search property (Monotonic):
 * - If capacity C works, any capacity > C also works
 * - If capacity C fails, any capacity < C also fails
 * - Pattern: [NO, NO, ..., NO, YES, YES, ..., YES]
 * - We want first YES = minimum working capacity
 * 
 * 3. Greedy loading strategy:
 * - Load packages in order
 * - Add to current day while capacity allows
 * - Start new day when next package would exceed capacity
 * 
 * ALGORITHM:
 * 1. Binary search on capacity [max(weights), sum(weights)]
 * 2. For each capacity, simulate shipping:
 * - Greedily pack packages day by day
 * - Count days needed
 * 3. If days needed ≤ target: capacity works, try smaller
 * 4. If days needed > target: need larger capacity
 * 
 * EXAMPLE WALKTHROUGH: weights=[1,2,3,4,5,6,7,8,9,10], days=5
 * 
 * Initial: left=10 (max), right=55 (sum)
 * 
 * Iteration 1: mid=32
 * Simulate with capacity 32:
 * - Day 1: 1+2+3+4+5+6+7+8 = 36 > 32, back to 1+2+3+4+5+6+7 = 28
 * - Day 2: 8+9+10 = 27
 * - Total: 2 days ≤ 5 ✓
 * Try smaller: right=32
 * 
 * Iteration 2: left=10, right=32, mid=21
 * - Day 1: 1+2+3+4+5+6 = 21
 * - Day 2: 7+8 = 15
 * - Day 3: 9+10 = 19
 * - Total: 3 days ≤ 5 ✓
 * Try smaller: right=21
 * 
 * Iteration 3: left=10, right=21, mid=15
 * - Day 1: 1+2+3+4+5 = 15
 * - Day 2: 6+7 = 13
 * - Day 3: 8
 * - Day 4: 9
 * - Day 5: 10
 * - Total: 5 days ≤ 5 ✓
 * Try smaller: right=15
 * 
 * Iteration 4: left=10, right=15, mid=12
 * - Day 1: 1+2+3+4 = 10
 * - Day 2: 5+6 = 11
 * - Day 3: 7
 * - Day 4: 8
 * - Day 5: 9
 * - Day 6: 10
 * - Total: 6 days > 5 ✗
 * Need larger: left=13
 * 
 * Continue until left=15, right=15 → Answer: 15
 * 
 * EXAMPLE 2: weights=[3,2,2,4,1,4], days=3
 * 
 * Capacity 6:
 * - Day 1: 3+2 = 5
 * - Day 2: 2+4 = 6
 * - Day 3: 1+4 = 5
 * - Total: 3 days ✓
 * 
 * Capacity 5:
 * - Day 1: 3+2 = 5
 * - Day 2: 2 (can't add 4, exceeds)
 * - Day 3: 4
 * - Day 4: 1+4 = 5
 * - Total: 4 days > 3 ✗
 * 
 * Answer: 6
 * 
 * COMPLEXITY ANALYSIS:
 * - Time: O(n * log(sum))
 * - n = number of packages
 * - sum = sum of all weights
 * - Binary search: log(sum) iterations
 * - Each iteration: O(n) to simulate shipping
 * - Space: O(1) - constant extra space
 * 
 * EDGE CASES:
 * 1. Single package: capacity = weight of that package
 * 2. days = 1: capacity = sum of all weights
 * 3. days = n: capacity = max(weights)
 * 4. All weights equal: capacity = ceil(sum / days) * weight
 * 5. Very large weights: ensure no integer overflow
 * 
 * WHY BINARY SEARCH WORKS:
 * The feasibility function is monotonic:
 * - Feasible(capacity) returns whether we can ship in ≤ days
 * - If Feasible(C) = true, then Feasible(C+1) = true
 * - If Feasible(C) = false, then Feasible(C-1) = false
 * 
 * This creates sorted pattern perfect for binary search:
 * Capacity: 10 11 12 13 14 15 16 ... 55
 * Can ship: NO NO NO NO NO YES YES ... YES
 * 
 * We find the first YES (transition point) = minimum capacity
 * 
 * OPTIMIZATION NOTES:
 * 1. Early termination in canShip when days exceeded
 * 2. Calculate min/max in single pass
 * 3. No need to check capacity < max(weights)
 * 4. Greedy approach is optimal for this problem
 */
