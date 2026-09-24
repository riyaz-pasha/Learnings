
/*
 * Given an integer array nums, return an array answer such that answer[i] is
 * equal to the product of all the elements of nums except nums[i].
 * 
 * The product of any prefix or suffix of nums is guaranteed to fit in a 32-bit
 * integer.
 * 
 * You must write an algorithm that runs in O(n) time and without using the
 * division operation.
 * 
 * Example 1:
 * Input: nums = [1,2,3,4]
 * Output: [24,12,8,6]
 * 
 * Example 2:
 * Input: nums = [-1,1,0,-3,3]
 * Output: [0,0,9,0,0]
 */
import java.util.Arrays;

class ProductExceptSelf {

    // Solution 1: Two-Pass with Extra Arrays (Most Intuitive)
    // Time: O(n), Space: O(n)
    public int[] productExceptSelf(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];

        // First pass: calculate left products
        int[] leftProducts = new int[n];
        leftProducts[0] = 1;
        for (int i = 1; i < n; i++) {
            leftProducts[i] = leftProducts[i - 1] * nums[i - 1];
        }

        // Second pass: calculate right products
        int[] rightProducts = new int[n];
        rightProducts[n - 1] = 1;
        for (int i = n - 2; i >= 0; i--) {
            rightProducts[i] = rightProducts[i + 1] * nums[i + 1];
        }

        // Combine left and right products
        for (int i = 0; i < n; i++) {
            result[i] = leftProducts[i] * rightProducts[i];
        }

        return result;
    }

    // Solution 2: Optimized Single Array (Space Efficient)
    // Time: O(n), Space: O(1) - not counting output array
    public int[] productExceptSelfOptimized(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];

        // First pass: store left products in result array
        result[0] = 1;
        for (int i = 1; i < n; i++) {
            result[i] = result[i - 1] * nums[i - 1];
        }

        // Second pass: multiply with right products using a single variable
        int rightProduct = 1;
        for (int i = n - 1; i >= 0; i--) {
            result[i] = result[i] * rightProduct;
            rightProduct *= nums[i];
        }

        return result;
    }

    // Solution 3: Division-based Approach (For Educational Purposes)
    // Note: This approach handles zeros but uses division
    // Time: O(n), Space: O(1)
    public int[] productExceptSelfDivision(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];

        // Count zeros and calculate product of non-zero elements
        int zeroCount = 0;
        int productWithoutZeros = 1;

        for (int num : nums) {
            if (num == 0) {
                zeroCount++;
            } else {
                productWithoutZeros *= num;
            }
        }

        for (int i = 0; i < n; i++) {
            if (zeroCount > 1) {
                // More than one zero means all results are 0
                result[i] = 0;
            } else if (zeroCount == 1) {
                // Exactly one zero
                result[i] = (nums[i] == 0) ? productWithoutZeros : 0;
            } else {
                // No zeros, use division
                result[i] = productWithoutZeros / nums[i];
            }
        }

        return result;
    }

    // Solution 4: Using Prefix and Suffix Products (Clear Logic)
    // Time: O(n), Space: O(1)
    public int[] productExceptSelfPrefixSuffix(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];

        // Calculate prefix products
        int prefix = 1;
        for (int i = 0; i < n; i++) {
            result[i] = prefix;
            prefix *= nums[i];
        }

        // Calculate suffix products and combine
        int suffix = 1;
        for (int i = n - 1; i >= 0; i--) {
            result[i] *= suffix;
            suffix *= nums[i];
        }

        return result;
    }

    // Solution 5: Recursive Approach (Educational - Not Optimal)
    // Time: O(n²), Space: O(n) due to recursion
    public int[] productExceptSelfRecursive(int[] nums) {
        int[] result = new int[nums.length];
        for (int i = 0; i < nums.length; i++) {
            result[i] = productExceptIndex(nums, i);
        }
        return result;
    }

    private int productExceptIndex(int[] nums, int excludeIndex) {
        int product = 1;
        for (int i = 0; i < nums.length; i++) {
            if (i != excludeIndex) {
                product *= nums[i];
            }
        }
        return product;
    }

    // Utility method to print arrays
    private void printArray(String label, int[] arr) {
        System.out.println(label + ": " + Arrays.toString(arr));
    }

    // Test cases
    public static void main(String[] args) {
        ProductExceptSelf solution = new ProductExceptSelf();

        System.out.println("=== Product of Array Except Self Solutions ===\n");

        // Test case 1: [1,2,3,4]
        int[] nums1 = { 1, 2, 3, 4 };
        System.out.println("Test 1: " + Arrays.toString(nums1));
        solution.printArray("Two-Pass", solution.productExceptSelf(nums1));
        solution.printArray("Optimized", solution.productExceptSelfOptimized(nums1));
        solution.printArray("Division", solution.productExceptSelfDivision(nums1));
        solution.printArray("Prefix-Suffix", solution.productExceptSelfPrefixSuffix(nums1));
        System.out.println("Expected: [24, 12, 8, 6]\n");

        // Test case 2: [-1,1,0,-3,3]
        int[] nums2 = { -1, 1, 0, -3, 3 };
        System.out.println("Test 2: " + Arrays.toString(nums2));
        solution.printArray("Two-Pass", solution.productExceptSelf(nums2));
        solution.printArray("Optimized", solution.productExceptSelfOptimized(nums2));
        solution.printArray("Division", solution.productExceptSelfDivision(nums2));
        solution.printArray("Prefix-Suffix", solution.productExceptSelfPrefixSuffix(nums2));
        System.out.println("Expected: [0, 0, 9, 0, 0]\n");

        // Test case 3: Single element
        int[] nums3 = { 5 };
        System.out.println("Test 3: " + Arrays.toString(nums3));
        solution.printArray("Optimized", solution.productExceptSelfOptimized(nums3));
        System.out.println("Expected: [1]\n");

        // Test case 4: Two elements
        int[] nums4 = { 2, 3 };
        System.out.println("Test 4: " + Arrays.toString(nums4));
        solution.printArray("Optimized", solution.productExceptSelfOptimized(nums4));
        System.out.println("Expected: [3, 2]\n");

        // Test case 5: Multiple zeros
        int[] nums5 = { 0, 0, 2, 3 };
        System.out.println("Test 5: " + Arrays.toString(nums5));
        solution.printArray("Division", solution.productExceptSelfDivision(nums5));
        System.out.println("Expected: [0, 0, 0, 0]\n");

        // Test case 6: All negative
        int[] nums6 = { -1, -2, -3, -4 };
        System.out.println("Test 6: " + Arrays.toString(nums6));
        solution.printArray("Optimized", solution.productExceptSelfOptimized(nums6));
        System.out.println("Expected: [24, 12, 8, 6]\n");

        // Performance comparison
        System.out.println("=== Performance Test ===");
        int[] largeArray = new int[10000];
        Arrays.fill(largeArray, 2);

        long startTime = System.nanoTime();
        solution.productExceptSelfOptimized(largeArray);
        long optimizedTime = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        solution.productExceptSelf(largeArray);
        long twoPassTime = System.nanoTime() - startTime;

        System.out.println("Optimized approach: " + optimizedTime / 1000000.0 + " ms");
        System.out.println("Two-pass approach: " + twoPassTime / 1000000.0 + " ms");
    }

}

/*
 * ALGORITHM EXPLANATIONS:
 * 
 * 1. TWO-PASS WITH EXTRA ARRAYS:
 * - Create leftProducts array: leftProducts[i] = product of all elements to the
 * left of i
 * - Create rightProducts array: rightProducts[i] = product of all elements to
 * the right of i
 * - result[i] = leftProducts[i] * rightProducts[i]
 * - Time: O(n), Space: O(n)
 * 
 * 2. OPTIMIZED SINGLE ARRAY (RECOMMENDED):
 * - First pass: Store left products in result array
 * - Second pass: Multiply with right products using a single variable
 * - Time: O(n), Space: O(1) excluding output array
 * - This is the optimal solution for the problem constraints
 * 
 * 3. DIVISION-BASED APPROACH:
 * - Calculate total product, then divide by each element
 * - Must handle zeros carefully (count them separately)
 * - Time: O(n), Space: O(1)
 * - Not allowed by problem constraints but educational
 * 
 * 4. PREFIX-SUFFIX APPROACH:
 * - Similar to optimized but with clearer variable names
 * - Build prefix products, then multiply with suffix products
 * - Time: O(n), Space: O(1)
 * 
 * KEY INSIGHTS:
 * 
 * For element at index i, we need:
 * - Product of all elements to the left of i
 * - Product of all elements to the right of i
 * - Multiply these two products
 * 
 * Example walkthrough for [1,2,3,4]:
 * - Left products: [1, 1, 2, 6] (1, 1*1, 1*2, 1*2*3)
 * - Right products: [24, 12, 4, 1] (2*3*4, 3*4, 4, 1)
 * - Result: [1*24, 1*12, 2*4, 6*1] = [24, 12, 8, 6]
 * 
 * The optimized approach builds left products in the result array first,
 * then multiplies with right products calculated on the fly.
 * 
 * EDGE CASES HANDLED:
 * - Single element array
 * - Arrays with zeros
 * - Arrays with negative numbers
 * - Arrays with multiple zeros
 * 
 * The optimized solution is the best approach for interviews as it meets
 * all constraints: O(n) time, O(1) space, no division operation.
 */
