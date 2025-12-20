class SmallestDivisor {

    // Approach 1: Binary Search (Optimal)
    // Time: O(n * log(max)), Space: O(1)
    public int smallestDivisor1(int[] nums, int threshold) {
        int left = 1;
        int right = getMax(nums);

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (computeSum(nums, mid) <= threshold) {
                // Sum is acceptable, try smaller divisor
                right = mid;
            } else {
                // Sum too large, need larger divisor
                left = mid + 1;
            }
        }

        return left;
    }

    // Helper: Compute sum of ceiling divisions
    private int computeSum(int[] nums, int divisor) {
        int sum = 0;
        for (int num : nums) {
            // Ceiling division: (num + divisor - 1) / divisor
            sum += (num + divisor - 1) / divisor;
        }
        return sum;
    }

    private int getMax(int[] nums) {
        int max = nums[0];
        for (int num : nums) {
            max = Math.max(max, num);
        }
        return max;
    }

    // Approach 2: Binary Search with Math.ceil
    // Time: O(n * log(max)), Space: O(1)
    public int smallestDivisor2(int[] nums, int threshold) {
        int left = 1;
        int right = 1_000_000; // or getMax(nums)

        int result = right;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            int sum = 0;

            for (int num : nums) {
                sum += Math.ceil((double) num / mid);
            }

            if (sum <= threshold) {
                result = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return result;
    }

    // Approach 3: Binary Search with Long to Avoid Overflow
    // Time: O(n * log(max)), Space: O(1)
    public int smallestDivisor3(int[] nums, int threshold) {
        int left = 1;
        int right = getMax(nums);

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (getDivisionSum(nums, mid) <= threshold) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }

        return left;
    }

    private long getDivisionSum(int[] nums, int divisor) {
        long sum = 0;
        for (int num : nums) {
            sum += (num + divisor - 1) / divisor;
        }
        return sum;
    }

    // Approach 4: Binary Search with Detailed Comments
    // Time: O(n * log(max)), Space: O(1)
    public int smallestDivisor4(int[] nums, int threshold) {
        // Search space: [1, max(nums)]
        // Why max(nums)? Because dividing by max gives sum = n (each element becomes 1)
        // Dividing by anything larger gives same result
        int left = 1;
        int right = getMax(nums);

        // Binary search for minimum valid divisor
        while (left < right) {
            int mid = left + (right - left) / 2;
            int sum = calculateDivisionSum(nums, mid);

            if (sum <= threshold) {
                // This divisor works, try to find smaller one
                right = mid;
            } else {
                // Sum exceeds threshold, need larger divisor
                left = mid + 1;
            }
        }

        return left;
    }

    private int calculateDivisionSum(int[] nums, int divisor) {
        int sum = 0;
        for (int num : nums) {
            // Ceiling division without using Math.ceil
            // ceil(a/b) = floor((a + b - 1) / b) = (a + b - 1) / b
            sum += (num + divisor - 1) / divisor;
        }
        return sum;
    }

    // Approach 5: Linear Search (Brute Force - Not Optimal)
    // Time: O(n * max), Space: O(1)
    public int smallestDivisor5(int[] nums, int threshold) {
        int maxNum = getMax(nums);

        for (int divisor = 1; divisor <= maxNum; divisor++) {
            if (computeSum(nums, divisor) <= threshold) {
                return divisor;
            }
        }

        return maxNum;
    }

    // Test cases with detailed output
    public static void main(String[] args) {
        SmallestDivisor solution = new SmallestDivisor();

        // Test Case 1
        int[] nums1 = { 1, 2, 5, 9 };
        int threshold1 = 6;
        int result1 = solution.smallestDivisor1(nums1, threshold1);
        System.out.println("Test 1: " + result1); // Output: 5
        explainSolution(nums1, threshold1, result1);

        // Test Case 2
        int[] nums2 = { 44, 22, 33, 11, 1 };
        int threshold2 = 5;
        int result2 = solution.smallestDivisor1(nums2, threshold2);
        System.out.println("\nTest 2: " + result2); // Output: 44
        explainSolution(nums2, threshold2, result2);

        // Test Case 3: All same numbers
        int[] nums3 = { 10, 10, 10, 10 };
        int threshold3 = 10;
        int result3 = solution.smallestDivisor1(nums3, threshold3);
        System.out.println("\nTest 3: " + result3); // Output: 4
        explainSolution(nums3, threshold3, result3);

        // Test Case 4: Large numbers
        int[] nums4 = { 1, 2, 3 };
        int threshold4 = 6;
        int result4 = solution.smallestDivisor1(nums4, threshold4);
        System.out.println("\nTest 4: " + result4); // Output: 1
        explainSolution(nums4, threshold4, result4);

        // Test Case 5: Tight threshold
        int[] nums5 = { 19 };
        int threshold5 = 5;
        int result5 = solution.smallestDivisor1(nums5, threshold5);
        System.out.println("\nTest 5: " + result5); // Output: 4
        explainSolution(nums5, threshold5, result5);
    }

    private static void explainSolution(int[] nums, int threshold, int divisor) {
        System.out.println("Nums: " + java.util.Arrays.toString(nums));
        System.out.println("Threshold: " + threshold);
        System.out.println("Divisor: " + divisor);

        // Show divisions for a few divisors
        System.out.println("\nDivision results:");
        int[] testDivisors = { divisor - 1, divisor, divisor + 1 };

        for (int d : testDivisors) {
            if (d < 1)
                continue;
            int sum = 0;
            System.out.print("Divisor " + d + ": [");
            for (int i = 0; i < nums.length; i++) {
                int result = (nums[i] + d - 1) / d;
                sum += result;
                if (i > 0)
                    System.out.print(", ");
                System.out.print(result);
            }
            System.out.println("] → sum = " + sum +
                    (sum <= threshold ? " ✓" : " ✗"));
        }
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM ANALYSIS:
 * - Given array nums and threshold
 * - Find smallest divisor such that sum of ceil(nums[i]/divisor) <= threshold
 * - Each division is rounded up (ceiling)
 * 
 * KEY INSIGHTS:
 * 1. Binary search on divisor value
 * 2. Divisor range: [1, max(nums)]
 * - Why max(nums)? Dividing by max makes all elements become 1, sum = n
 * - Any divisor > max gives same result
 * 3. Monotonic property: As divisor increases, sum decreases
 * 4. We want SMALLEST divisor where sum <= threshold
 * 
 * CEILING DIVISION WITHOUT Math.ceil:
 * ceil(a/b) = (a + b - 1) / b
 * 
 * Examples:
 * - 7/3 = 2.33... → ceil = 3 → (7+3-1)/3 = 9/3 = 3 ✓
 * - 10/2 = 5.0 → ceil = 5 → (10+2-1)/2 = 11/2 = 5 ✓
 * - 9/4 = 2.25 → ceil = 3 → (9+4-1)/4 = 12/4 = 3 ✓
 * 
 * ALGORITHM:
 * 1. Binary search on divisor in range [1, max(nums)]
 * 2. For each divisor, compute sum of ceiling divisions
 * 3. If sum <= threshold: divisor works, try smaller (search left)
 * 4. If sum > threshold: divisor too small, need larger (search right)
 * 
 * EXAMPLE WALKTHROUGH: nums=[1,2,5,9], threshold=6
 * 
 * Initial: left=1, right=9
 * 
 * Iteration 1: mid=5
 * - 1/5 = ceil(0.2) = 1
 * - 2/5 = ceil(0.4) = 1
 * - 5/5 = ceil(1.0) = 1
 * - 9/5 = ceil(1.8) = 2
 * - Sum = 1+1+1+2 = 5 <= 6 ✓
 * - Try smaller: right=5
 * 
 * Iteration 2: left=1, right=5, mid=3
 * - 1/3 = 1
 * - 2/3 = 1
 * - 5/3 = 2
 * - 9/3 = 3
 * - Sum = 1+1+2+3 = 7 > 6 ✗
 * - Need larger: left=4
 * 
 * Iteration 3: left=4, right=5, mid=4
 * - 1/4 = 1
 * - 2/4 = 1
 * - 5/4 = 2
 * - 9/4 = 3
 * - Sum = 1+1+2+3 = 7 > 6 ✗
 * - Need larger: left=5
 * 
 * Result: left=5, right=5 → Answer: 5
 * 
 * VERIFICATION:
 * Divisor 4: [1,1,2,3] → sum=7 > 6 ✗
 * Divisor 5: [1,1,1,2] → sum=5 <= 6 ✓
 * Divisor 6: [1,1,1,2] → sum=5 <= 6 (but 5 is smaller)
 * 
 * EXAMPLE 2: nums=[44,22,33,11,1], threshold=5
 * 
 * Need sum of 5 divisions <= 5, so each must be <= 1
 * This means divisor >= each number
 * Smallest such divisor = max(nums) = 44
 * 
 * Divisor 44:
 * - 44/44 = 1
 * - 22/44 = 1
 * - 33/44 = 1
 * - 11/44 = 1
 * - 1/44 = 1
 * - Sum = 5 ✓
 * 
 * COMPLEXITY ANALYSIS:
 * - Time: O(n * log(max)) where n = array length, max = max element
 * - Binary search: log(max) iterations
 * - Each iteration: O(n) to compute sum
 * - Space: O(1) - constant extra space
 * 
 * EDGE CASES:
 * 1. threshold = n (array length): divisor must be >= max(nums)
 * 2. threshold very large: divisor = 1
 * 3. All elements same: divisor = ceil(element / (threshold / n))
 * 4. Single element: divisor = ceil(element / threshold)
 * 
 * WHY BINARY SEARCH WORKS:
 * Monotonicity in reverse:
 * - Smaller divisor → larger quotients → larger sum
 * - Larger divisor → smaller quotients → smaller sum
 * 
 * Pattern: sum vs divisor
 * [LARGE, LARGE, ..., LARGE, SMALL, SMALL, ..., SMALL]
 * d=1 d=2 d=x d=x+1 d=max
 * 
 * We want smallest d where sum becomes <= threshold
 * This is the transition point: first position where sum <= threshold
 * 
 * OPTIMIZATION TIPS:
 * 1. Use (num + divisor - 1) / divisor instead of Math.ceil for efficiency
 * 2. Consider using long for sum to avoid overflow
 * 3. Early termination if sum > threshold in calculation
 * 4. Can optimize search range: left could start at threshold/n
 */
