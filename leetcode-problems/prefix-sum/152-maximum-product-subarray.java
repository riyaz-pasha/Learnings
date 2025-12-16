class MaximumProductSubarray {

    // Solution 1: Optimal - Track Max and Min (Kadane's Variant)
    // Time: O(n), Space: O(1)
    public int maxProduct(int[] nums) {
        /*
         * Key Insight:
         * - Negative numbers can turn min into max (and vice versa)
         * - We need to track both max and min products
         * - At each step, current element can:
         * 1. Start a new subarray (just itself)
         * 2. Extend max product (if positive or max is negative)
         * 3. Extend min product (if negative, min * negative = max)
         */

        if (nums == null || nums.length == 0)
            return 0;

        int maxProduct = nums[0];
        int currentMax = nums[0];
        int currentMin = nums[0];

        for (int i = 1; i < nums.length; i++) {
            int num = nums[i];

            // When we multiply by a negative, max and min swap
            if (num < 0) {
                int temp = currentMax;
                currentMax = currentMin;
                currentMin = temp;
            }

            // Update current max and min
            currentMax = Math.max(num, currentMax * num);
            currentMin = Math.min(num, currentMin * num);

            // Update global maximum
            maxProduct = Math.max(maxProduct, currentMax);
        }

        return maxProduct;
    }

    // Solution 2: Track Max and Min Without Swapping
    // Time: O(n), Space: O(1)
    public int maxProduct2(int[] nums) {
        if (nums == null || nums.length == 0)
            return 0;

        int result = nums[0];
        int currentMax = nums[0];
        int currentMin = nums[0];

        for (int i = 1; i < nums.length; i++) {
            int num = nums[i];

            // Store previous values before updating
            int tempMax = currentMax;

            // Current max is the maximum of:
            // 1. Current element itself
            // 2. Current element * previous max
            // 3. Current element * previous min (important for negatives!)
            currentMax = Math.max(num, Math.max(num * tempMax, num * currentMin));

            // Current min is the minimum of:
            // 1. Current element itself
            // 2. Current element * previous max
            // 3. Current element * previous min
            currentMin = Math.min(num, Math.min(num * tempMax, num * currentMin));

            // Update result
            result = Math.max(result, currentMax);
        }

        return result;
    }

    // Solution 3: Brute Force (For Understanding)
    // Time: O(n²), Space: O(1)
    public int maxProductBruteForce(int[] nums) {
        int maxProduct = Integer.MIN_VALUE;

        for (int i = 0; i < nums.length; i++) {
            int product = 1;
            for (int j = i; j < nums.length; j++) {
                product *= nums[j];
                maxProduct = Math.max(maxProduct, product);
            }
        }

        return maxProduct;
    }

    // Solution 4: Two-Pass (Left to Right, Right to Left)
    // Time: O(n), Space: O(1)
    public int maxProduct4(int[] nums) {
        /*
         * Alternative approach:
         * - Scan from left to right
         * - Scan from right to left
         * - Reset product to 1 when encountering 0
         * - Take maximum from both scans
         */

        int n = nums.length;
        int maxProduct = Integer.MIN_VALUE;

        // Left to right scan
        int product = 1;
        for (int i = 0; i < n; i++) {
            product *= nums[i];
            maxProduct = Math.max(maxProduct, product);
            if (product == 0)
                product = 1; // Reset after 0
        }

        // Right to left scan
        product = 1;
        for (int i = n - 1; i >= 0; i--) {
            product *= nums[i];
            maxProduct = Math.max(maxProduct, product);
            if (product == 0)
                product = 1; // Reset after 0
        }

        return maxProduct;
    }

    // Solution 5: With Detailed Tracking and Visualization
    // Time: O(n), Space: O(1)
    public int maxProductWithVisualization(int[] nums, boolean showSteps) {
        if (nums == null || nums.length == 0)
            return 0;

        int result = nums[0];
        int maxProd = nums[0];
        int minProd = nums[0];

        if (showSteps) {
            System.out.println("\n=== Step-by-step Execution ===");
            System.out.print("Array: ");
            printArray(nums);
            System.out.println("\n");
            System.out.printf("Index 0: num=%d, max=%d, min=%d, result=%d%n",
                    nums[0], maxProd, minProd, result);
        }

        for (int i = 1; i < nums.length; i++) {
            int num = nums[i];
            int prevMax = maxProd;
            int prevMin = minProd;

            maxProd = Math.max(num, Math.max(num * prevMax, num * prevMin));
            minProd = Math.min(num, Math.min(num * prevMax, num * prevMin));
            result = Math.max(result, maxProd);

            if (showSteps) {
                System.out.printf("Index %d: num=%d", i, num);
                if (num < 0) {
                    System.out.print(" (negative - max/min may swap)");
                }
                System.out.println();
                System.out.printf("  prevMax=%d, prevMin=%d%n", prevMax, prevMin);
                System.out.printf("  newMax=%d, newMin=%d, result=%d%n",
                        maxProd, minProd, result);
            }
        }

        if (showSteps) {
            System.out.println("\nFinal result: " + result);
        }

        return result;
    }

    // Helper: Print array
    private static void printArray(int[] nums) {
        System.out.print("[");
        for (int i = 0; i < nums.length; i++) {
            System.out.print(nums[i]);
            if (i < nums.length - 1)
                System.out.print(", ");
        }
        System.out.print("]");
    }

    // Helper: Find and print the actual subarray
    private static void findMaxProductSubarray(int[] nums) {
        int n = nums.length;
        int maxProduct = Integer.MIN_VALUE;
        int start = 0, end = 0;

        for (int i = 0; i < n; i++) {
            int product = 1;
            for (int j = i; j < n; j++) {
                product *= nums[j];
                if (product > maxProduct) {
                    maxProduct = product;
                    start = i;
                    end = j;
                }
            }
        }

        System.out.print("\nMax product subarray: [");
        for (int i = start; i <= end; i++) {
            System.out.print(nums[i]);
            if (i < end)
                System.out.print(", ");
        }
        System.out.println("] = " + maxProduct);
    }

    // Helper: Explain why we need both max and min
    private static void explainAlgorithm() {
        System.out.println("\n=== Why Track Both Max and Min? ===");
        System.out.println("\nExample: [2, -3, -4]");
        System.out.println("\nWithout tracking min:");
        System.out.println("  At index 0: max = 2");
        System.out.println("  At index 1: max = max(2*-3, -3) = -3");
        System.out.println("  At index 2: max = max(-3*-4, -4) = 12 ✓");
        System.out.println("  But we lose track of the path!");

        System.out.println("\nWith tracking both max and min:");
        System.out.println("  At index 0: max = 2, min = 2");
        System.out.println("  At index 1: max = -3, min = -6 (2*-3)");
        System.out.println("  At index 2: max = 24 (min * -4), min = 12 (-3*-4)");
        System.out.println("  Result = 24 ✓");

        System.out.println("\nKey insight: Negative * Negative = Positive");
        System.out.println("So the minimum product (most negative) can become");
        System.out.println("the maximum when multiplied by a negative number!");
    }

    // Test cases
    public static void main(String[] args) {
        MaximumProductSubarray solution = new MaximumProductSubarray();

        // Explain the algorithm first
        explainAlgorithm();

        // Test case 1
        int[] nums1 = { 2, 3, -2, 4 };
        System.out.print("\n\nExample 1 - Input: ");
        printArray(nums1);
        System.out.println();
        System.out.println("Output (Optimal): " + solution.maxProduct(nums1));
        System.out.println("Output (Brute Force): " + solution.maxProductBruteForce(nums1));
        findMaxProductSubarray(nums1);

        // Test case 2
        int[] nums2 = { -2, 0, -1 };
        System.out.print("\nExample 2 - Input: ");
        printArray(nums2);
        System.out.println();
        System.out.println("Output: " + solution.maxProduct(nums2));
        findMaxProductSubarray(nums2);

        // Test case 3: All negatives
        int[] nums3 = { -2, -3, -4 };
        System.out.print("\nAll negatives - Input: ");
        printArray(nums3);
        System.out.println();
        System.out.println("Output: " + solution.maxProduct2(nums3));
        findMaxProductSubarray(nums3);
        System.out.println("Explanation: -3 * -4 = 12 (even negatives make positive)");

        // Test case 4: With zeros
        int[] nums4 = { 2, 3, 0, 4, 5 };
        System.out.print("\nWith zeros - Input: ");
        printArray(nums4);
        System.out.println();
        System.out.println("Output: " + solution.maxProduct(nums4));
        findMaxProductSubarray(nums4);

        // Test case 5: Single element
        int[] nums5 = { -2 };
        System.out.print("\nSingle element - Input: ");
        printArray(nums5);
        System.out.println();
        System.out.println("Output: " + solution.maxProduct(nums5));

        // Test case 6: All positive
        int[] nums6 = { 1, 2, 3, 4 };
        System.out.print("\nAll positive - Input: ");
        printArray(nums6);
        System.out.println();
        System.out.println("Output: " + solution.maxProduct(nums6));
        findMaxProductSubarray(nums6);

        // Test case 7: Mix of everything
        int[] nums7 = { -2, 3, -4, 0, 5, -1, 2 };
        System.out.print("\nComplex example - Input: ");
        printArray(nums7);
        System.out.println();
        System.out.println("Output: " + solution.maxProduct4(nums7));
        findMaxProductSubarray(nums7);

        // Test case 8: With detailed visualization
        int[] nums8 = { 2, -3, -4 };
        System.out.print("\nDetailed walkthrough - Input: ");
        printArray(nums8);
        solution.maxProductWithVisualization(nums8, true);

        // Performance comparison
        System.out.println("\n\n=== Performance Comparison ===");
        int[] largeArray = new int[1000];
        java.util.Random rand = new java.util.Random(42);
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = rand.nextInt(21) - 10; // Random -10 to 10
        }

        long start = System.nanoTime();
        int resultBrute = solution.maxProductBruteForce(largeArray);
        long timeBrute = System.nanoTime() - start;

        start = System.nanoTime();
        int resultOptimal = solution.maxProduct(largeArray);
        long timeOptimal = System.nanoTime() - start;

        start = System.nanoTime();
        int result2Pass = solution.maxProduct4(largeArray);
        long time2Pass = System.nanoTime() - start;

        System.out.println("Array size: 1000 elements");
        System.out.println("Brute Force: " + resultBrute + " (Time: " + timeBrute / 1000000.0 + " ms)");
        System.out.println("Optimal (Max/Min): " + resultOptimal + " (Time: " + timeOptimal / 1000000.0 + " ms)");
        System.out.println("Two-Pass: " + result2Pass + " (Time: " + time2Pass / 1000000.0 + " ms)");
        System.out.println("Speedup: " + (timeBrute / (double) timeOptimal) + "x faster");
    }
}

/*
 * Given an integer array nums, find a subarray that has the largest product,
 * and return the product.
 * 
 * The test cases are generated so that the answer will fit in a 32-bit integer.
 * 
 * Note that the product of an array with a single element is the value of that
 * element.
 * 
 * Example 1:
 * 
 * Input: nums = [2,3,-2,4]
 * Output: 6
 * Explanation: [2,3] has the largest product 6.
 * Example 2:
 * 
 * Input: nums = [-2,0,-1]
 * Output: 0
 * Explanation: The result cannot be 2, because [-2,-1] is not a subarray.
 */
