import java.util.*;
import java.util.stream.*;

/**
 * ================================================================
 * 🔥 Partition Array Into Two Equal Size Arrays (MITM MASTER)
 * ================================================================
 *
 * IDEA:
 * Split array into 2 halves
 * Generate all subset sums grouped by size
 * Match left + right using Binary Search
 *
 * ================================================================
 */
class MinPartitionDifference {

    public static int minimumDifference(int[] nums) {
        int n = nums.length / 2;

        int[] left = Arrays.copyOfRange(nums, 0, n);
        int[] right = Arrays.copyOfRange(nums, n, 2 * n);

        // total sum
        int totalSum = Arrays.stream(nums).sum();

        // maps: count -> list of subset sums
        List<List<Integer>> leftMap = new ArrayList<>();
        List<List<Integer>> rightMap = new ArrayList<>();

        for (int i = 0; i <= n; i++) {
            leftMap.add(new ArrayList<>());
            rightMap.add(new ArrayList<>());
        }

        // generate subset sums for left
        generate(left, leftMap);

        // generate subset sums for right
        generate(right, rightMap);

        // sort right side for binary search
        for (List<Integer> list : rightMap) {
            Collections.sort(list);
        }

        int answer = Integer.MAX_VALUE;

        // try all splits
        for (int k = 0; k <= n; k++) {

            List<Integer> leftSums = leftMap.get(k);
            List<Integer> rightSums = rightMap.get(n - k);

            for (int leftSum : leftSums) {

                // we want closest to totalSum / 2
                int target = totalSum / 2 - leftSum;

                int candidate = binarySearchClosest(rightSums, target);

                int chosenSum = leftSum + candidate;

                int diff = Math.abs(totalSum - 2 * chosenSum);

                answer = Math.min(answer, diff);
            }
        }

        return answer;
    }

    /**
     * Generate all subset sums grouped by count
     *
     * Time: O(n * 2^n)
     */
    private static void generate(int[] arr, List<List<Integer>> map) {

        int n = arr.length;

        for (int mask = 0; mask < (1 << n); mask++) {

            int sum = 0;
            int count = 0;

            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    sum += arr[i];
                    count++;
                }
            }

            map.get(count).add(sum);
        }
    }

    /**
     * Binary search closest value
     *
     * Uses explicit answer variable (as you prefer)
     */
    private static int binarySearchClosest(List<Integer> list, int target) {

        int low = 0;
        int high = list.size() - 1;

        int answer = list.get(0); // fallback

        while (low <= high) {

            int mid = low + (high - low) / 2;

            int val = list.get(mid);

            // update best answer
            if (Math.abs(val - target) < Math.abs(answer - target)) {
                answer = val;
            }

            if (val < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return answer;
    }

    // Driver
    public static void main(String[] args) {
        int[] nums = {3, 9, 7, 3};
        System.out.println(minimumDifference(nums)); // Output: 2
    }
}

/**
 * Problem Statement:
 * You are given an integer array `nums` of 2 * n elements.
 * Divide the array into two subarrays of length n such that the absolute 
 * difference between their sums is minimized.
 * Return this minimum absolute difference.
 * 
 * Constraints:
 * - 1 <= n <= 15
 * - nums.length == 2 * n
 * - -10^7 <= nums[i] <= 10^7
 */
class MinimizeSumDifference {

    /**
     * SOLUTION 1: Meet in the Middle + Binary Search (Optimal)
     * 
     * Time Complexity: O(N * 2^N)
     * Space Complexity: O(2^N)
     * 
     * VISUAL EXPLANATION & LOGIC:
     * With 2*n up to 30 elements, iterating all combinations C(30, 15) is too slow.
     * We divide the array into two halves: Left (first n elements) and Right (last n elements).
     * 
     * Let totalSum be the sum of all elements in nums.
     * We want to pick exactly 'n' elements total to form a partition.
     * If we pick 'k' elements from the Left half (sum = X), we MUST pick 'n - k' 
     * elements from the Right half (sum = Y).
     * The sum of our partition will be: partitionSum = X + Y.
     * The other partition will have sum: totalSum - partitionSum.
     * The difference is: abs(totalSum - 2 * (X + Y)).
     * 
     * To minimize this difference, we want (X + Y) to be as close to totalSum / 2 as possible.
     * Therefore, for a known X, we want Y to be as close to (totalSum / 2 - X) as possible.
     * 
     * Steps:
     * 1. Generate all possible subset sums for the Left half and group them by 
     *    how many elements were picked (k).
     * 2. Do the same for the Right half.
     * 3. Sort the Right half sum lists.
     * 4. For each X in leftSums[k], use Binary Search on rightSums[n - k] 
     *    to find the Y that brings (X + Y) closest to totalSum / 2.
     */
    public static int minimumDifferenceMeetInMiddle(int[] nums) {
        int n = nums.length / 2;
        int totalSum = Arrays.stream(nums).sum();

        // Create lists to store sums based on the number of elements picked
        List<List<Integer>> leftSums = new ArrayList<>();
        List<List<Integer>> rightSums = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            leftSums.add(new ArrayList<>());
            rightSums.add(new ArrayList<>());
        }

        // Generate subset sums for left and right halves
        generateSums(nums, 0, n, 0, 0, leftSums);
        generateSums(nums, n, 2 * n, 0, 0, rightSums);

        // Sort right side sums to enable Binary Search
        for (int i = 0; i <= n; i++) {
            Collections.sort(rightSums.get(i));
        }

        int minDiff = Integer.MAX_VALUE;
        int target = totalSum / 2;

        // Iterate through all possible counts of elements picked from the left
        for (int k = 0; k <= n; k++) {
            List<Integer> leftList = leftSums.get(k);
            List<Integer> rightList = rightSums.get(n - k);

            for (int x : leftList) {
                // We want y such that x + y is as close to target as possible
                int targetY = target - x;
                int bestY = binarySearchClosest(rightList, targetY);

                int partitionSum = x + bestY;
                int currentDiff = Math.abs(totalSum - 2 * partitionSum);
                minDiff = Math.min(minDiff, currentDiff);
            }
        }

        return minDiff;
    }

    /**
     * Helper Method: Recursively generates all subset sums for a half of the array.
     */
    private static void generateSums(int[] nums, int idx, int end, int count, int currentSum, List<List<Integer>> sums) {
        if (idx == end) {
            sums.get(count).add(currentSum);
            return;
        }
        
        // Option 1: Include the current element
        generateSums(nums, idx + 1, end, count + 1, currentSum + nums[idx], sums);
        
        // Option 2: Exclude the current element
        generateSums(nums, idx + 1, end, count, currentSum, sums);
    }

    /**
     * Helper Method: Binary Search to find the element closest to the target.
     * Explicitly uses a 'result' variable to track the best match found.
     */
    private static int binarySearchClosest(List<Integer> list, int target) {
        int low = 0;
        int high = list.size() - 1;
        
        // Explicit result variable to keep track of the closest value found
        int closestResult = list.get(0); 

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int val = list.get(mid);

            // Update result if the current mid value is closer to the target
            if (Math.abs(val - target) < Math.abs(closestResult - target)) {
                closestResult = val;
            }

            if (val == target) {
                closestResult = val; // Perfect match found
                break;
            } else if (val < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return closestResult;
    }

    /**
     * SOLUTION 2: Brute Force DFS (Sub-optimal, useful for small inputs)
     * 
     * Time Complexity: O(2^(2N))
     * Space Complexity: O(2N) recursion stack
     * 
     * EXPLANATION:
     * Recursively tries to place every element into either partition 1 or partition 2.
     * Returns the minimum difference only if partition 1 has exactly N elements.
     * Included for testing baseline correctness on small inputs.
     */
    public static int minimumDifferenceBruteForce(int[] nums) {
        int totalSum = Arrays.stream(nums).sum();
        return dfsBruteForce(nums, 0, 0, 0, totalSum);
    }

    private static int dfsBruteForce(int[] nums, int idx, int count, int currentSum, int totalSum) {
        if (idx == nums.length) {
            if (count == nums.length / 2) {
                return Math.abs(totalSum - 2 * currentSum);
            }
            return Integer.MAX_VALUE;
        }

        int diffInclude = dfsBruteForce(nums, idx + 1, count + 1, currentSum + nums[idx], totalSum);
        int diffExclude = dfsBruteForce(nums, idx + 1, count, currentSum, totalSum);

        return Math.min(diffInclude, diffExclude);
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record to structure the test cases elegantly.
     */
    public record TestCase(int[] nums, int expected) {}

    public static void main(String[] args) {
        // Defined Test Cases based on standard logic, negatives, and problem bounds
        TestCase[] testCases = {
            new TestCase(new int[]{3, 9, 7, 3}, 2),                   // Simple Case: [3,9] and [7,3] -> diff = 2
            new TestCase(new int[]{-36, 36}, 72),                     // Negatives
            new TestCase(new int[]{2, -1, 0, 4, -2, -9}, 0),          // Mixed Array: [-1,4,-2] (1) and [2,0,-9] (-7) -> diff = 8? Wait, optimal is [2,-1,-2] (-1) and [0,4,-9] (-5) -> diff 4. Best is 0.
            new TestCase(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 1) // N = 5
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int resOptimal = minimumDifferenceMeetInMiddle(tc.nums());
            
            // Run brute force only on smaller arrays to keep test suite fast
            boolean isSmallTest = tc.nums().length <= 16;
            int resBrute = isSmallTest ? minimumDifferenceBruteForce(tc.nums()) : tc.expected();

            boolean passed = (resOptimal == tc.expected()) && (resBrute == tc.expected());

            // Limit array printing length for neat terminal output
            String arrStr = Arrays.toString(tc.nums());
            if (arrStr.length() > 25) arrStr = arrStr.substring(0, 22) + "...]";

            System.out.printf("Test %d | Array: %-25s -> Expected: %-3d | Passed: %b%n",
                    i + 1, arrStr, tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] Optimal: %d, Brute: %d%n", resOptimal, resBrute);
            }
        }
    }
}
