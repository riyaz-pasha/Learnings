
import java.util.Stack;

/**
 * Sum of Subarray Minimums - Complete Solutions
 * 
 * Problem: Given an array of integers, find the sum of min(b) where b ranges over 
 * every contiguous subarray of arr. Return answer modulo 10^9 + 7.
 * 
 * Key Insights:
 * 1. For each element, calculate how many subarrays have it as minimum
 * 2. Use monotonic stack to find boundaries efficiently
 * 3. For element at index i: contribution = arr[i] * (left_count * right_count)
 *    where left_count = distance to previous smaller element
 *    and right_count = distance to next smaller element
 */

class SumOfSubarrayMinimumsSolution {

    public int sumSubarrayMins(int[] arr) {
        int n = arr.length;
        int MOD = 1_000_000_007;

        int[] left = previousSmallerDistance(arr);
        int[] right = nextSmallerDistance(arr);

        long result = 0;
        for (int i = 0; i < n; i++) {
            result = (result + (long) arr[i] * left[i] * right[i]) % MOD;
        }
        return (int) result;
    }

    // Previous Smaller (STRICT)
    private int[] previousSmallerDistance(int[] arr) {
        int n = arr.length;
        int[] result = new int[n];
        Stack<Integer> stack = new Stack<>();

        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && arr[stack.peek()] > arr[i]) {
                stack.pop();
            }
            result[i] = stack.isEmpty() ? i + 1 : i - stack.peek();
            stack.push(i);
        }
        return result;
    }

    // Next Smaller or Equal
    private int[] nextSmallerDistance(int[] arr) {
        int n = arr.length;
        int[] result = new int[n];
        Stack<Integer> stack = new Stack<>();

        for (int i = n - 1; i >= 0; i--) {
            while (!stack.isEmpty() && arr[stack.peek()] >= arr[i]) {
                stack.pop();
            }
            result[i] = stack.isEmpty() ? n - i : stack.peek() - i;
            stack.push(i);
        }
        return result;
    }
}


class SumOfSubarrayMinimums {
    
    private static final int MOD = 1_000_000_007;
    
    // ========================================================================
    // SOLUTION 1: BRUTE FORCE
    // Time Complexity: O(n²)
    // Space Complexity: O(1)
    // ========================================================================
    
    /**
     * Brute Force Approach:
     * - Generate all possible subarrays
     * - Find minimum of each subarray
     * - Sum all minimums
     * 
     * This is straightforward but inefficient for large arrays.
     */
    public static int sumSubarrayMinsBruteForce(int[] arr) {
        int n = arr.length;
        long sum = 0;
        
        // Iterate through all possible starting points
        for (int i = 0; i < n; i++) {
            int min = arr[i];
            
            // Extend to all possible ending points
            for (int j = i; j < n; j++) {
                min = Math.min(min, arr[j]);
                sum = (sum + min) % MOD;
            }
        }
        
        return (int) sum;
    }
    
    // ========================================================================
    // SOLUTION 2: MONOTONIC STACK (OPTIMAL)
    // Time Complexity: O(n)
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Monotonic Stack Approach:
     * 
     * Core Idea:
     * For each element arr[i], we calculate:
     * - left[i]: number of elements to the left where arr[i] is minimum
     * - right[i]: number of elements to the right where arr[i] is minimum
     * - contribution = arr[i] * left[i] * right[i]
     * 
     * We use monotonic stacks to find:
     * - Previous Less Element (PLE): nearest smaller element on left
     * - Next Less Element (NLE): nearest smaller element on right
     * 
     * Handling Duplicates:
     * To avoid counting the same subarray twice when there are duplicate values,
     * we use:
     * - PLE: strictly less than (<)
     * - NLE: less than or equal to (<=)
     * 
     * This ensures each subarray is counted exactly once.
     */
    public static int sumSubarrayMinsMonotonicStack(int[] arr) {
        int n = arr.length;
        long sum = 0;
        
        // left[i] = distance to previous less element (or i+1 if none exists)
        int[] left = new int[n];
        // right[i] = distance to next less element (or n-i if none exists)
        int[] right = new int[n];
        
        // Calculate left array using monotonic increasing stack
        // Stack stores indices
        java.util.Stack<Integer> stack = new java.util.Stack<>();
        
        for (int i = 0; i < n; i++) {
            // Pop elements >= current (strictly greater for PLE)
            while (!stack.isEmpty() && arr[stack.peek()] > arr[i]) {
                stack.pop();
            }
            // Distance to previous smaller element
            left[i] = stack.isEmpty() ? i + 1 : i - stack.peek();
            stack.push(i);
        }
        
        // Calculate right array using monotonic increasing stack
        stack.clear();
        
        for (int i = n - 1; i >= 0; i--) {
            // Pop elements >= current (including equal for NLE to handle duplicates)
            while (!stack.isEmpty() && arr[stack.peek()] >= arr[i]) {
                stack.pop();
            }
            // Distance to next smaller element
            right[i] = stack.isEmpty() ? n - i : stack.peek() - i;
            stack.push(i);
        }
        
        // Calculate the sum
        for (int i = 0; i < n; i++) {
            long contribution = (long) arr[i] * left[i] % MOD * right[i] % MOD;
            sum = (sum + contribution) % MOD;
        }
        
        return (int) sum;
    }
    
    // ========================================================================
    // SOLUTION 3: OPTIMIZED SINGLE PASS MONOTONIC STACK
    // Time Complexity: O(n)
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Single Pass Monotonic Stack:
     * Instead of two passes, we can calculate the contribution in one pass.
     * 
     * When we pop an element from the stack, we know:
     * - Its right boundary (current index)
     * - Its left boundary (previous element in stack or start)
     * 
     * We can calculate its total contribution at that moment.
     */
    public static int sumSubarrayMinsSinglePass(int[] arr) {
        int n = arr.length;
        long sum = 0;
        java.util.Stack<Integer> stack = new java.util.Stack<>();
        
        for (int i = 0; i <= n; i++) {
            // Use 0 as sentinel value at the end to clear the stack
            int current = (i == n) ? 0 : arr[i];
            
            while (!stack.isEmpty() && arr[stack.peek()] >= current) {
                int mid = stack.pop();
                int left = stack.isEmpty() ? -1 : stack.peek();
                int right = i;
                
                // Count of subarrays where arr[mid] is minimum
                long count = (long) (mid - left) * (right - mid);
                long contribution = (long) arr[mid] * count % MOD;
                sum = (sum + contribution) % MOD;
            }
            
            stack.push(i);
        }
        
        return (int) sum;
    }
    
    // ========================================================================
    // SOLUTION 4: DYNAMIC PROGRAMMING APPROACH
    // Time Complexity: O(n)
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Dynamic Programming Approach:
     * 
     * dp[i] = sum of minimums of all subarrays ending at index i
     * 
     * For each position i:
     * - Find the previous smaller or equal element position j
     * - All subarrays from j+1 to i have arr[i] as minimum
     * - Count = (i - j)
     * - dp[i] = dp[j] + arr[i] * count
     */
    public static int sumSubarrayMinsDP(int[] arr) {
        int n = arr.length;
        long[] dp = new long[n];
        long sum = 0;
        java.util.Stack<Integer> stack = new java.util.Stack<>();
        
        for (int i = 0; i < n; i++) {
            // Pop elements >= current
            while (!stack.isEmpty() && arr[stack.peek()] >= arr[i]) {
                stack.pop();
            }
            
            if (stack.isEmpty()) {
                // arr[i] is minimum for all subarrays ending at i
                dp[i] = (long) arr[i] * (i + 1);
            } else {
                int j = stack.peek();
                // arr[i] is minimum for subarrays from j+1 to i
                dp[i] = dp[j] + (long) arr[i] * (i - j);
            }
            
            dp[i] %= MOD;
            sum = (sum + dp[i]) % MOD;
            stack.push(i);
        }
        
        return (int) sum;
    }
    
    // ========================================================================
    // HELPER METHODS FOR TESTING AND DEMONSTRATION
    // ========================================================================
    
    /**
     * Utility method to print all subarrays and their minimums
     * Useful for understanding the problem with small examples
     */
    public static void printAllSubarraysWithMin(int[] arr) {
        int n = arr.length;
        System.out.println("All subarrays and their minimums:");
        int totalSum = 0;
        
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                System.out.print("[");
                int min = Integer.MAX_VALUE;
                for (int k = i; k <= j; k++) {
                    System.out.print(arr[k]);
                    if (k < j) System.out.print(",");
                    min = Math.min(min, arr[k]);
                }
                System.out.println("] -> min = " + min);
                totalSum += min;
            }
        }
        System.out.println("Total sum: " + totalSum);
    }
    
    /**
     * Method to verify all solutions produce the same result
     */
    public static void verifySolutions(int[] arr) {
        System.out.println("\nInput: " + java.util.Arrays.toString(arr));
        
        int result1 = sumSubarrayMinsBruteForce(arr);
        int result2 = sumSubarrayMinsMonotonicStack(arr);
        int result3 = sumSubarrayMinsSinglePass(arr);
        int result4 = sumSubarrayMinsDP(arr);
        
        System.out.println("Brute Force:          " + result1);
        System.out.println("Monotonic Stack:      " + result2);
        System.out.println("Single Pass Stack:    " + result3);
        System.out.println("Dynamic Programming:  " + result4);
        
        boolean allMatch = (result1 == result2) && (result2 == result3) && (result3 == result4);
        System.out.println("All solutions match:  " + allMatch);
    }
    
    // ========================================================================
    // MAIN METHOD WITH TEST CASES
    // ========================================================================
    
    public static void main(String[] args) {
        System.out.println("=".repeat(70));
        System.out.println("SUM OF SUBARRAY MINIMUMS - ALL SOLUTIONS");
        System.out.println("=".repeat(70));
        
        // Test Case 1
        System.out.println("\n--- Test Case 1 ---");
        int[] arr1 = {3, 1, 2, 4};
        printAllSubarraysWithMin(arr1);
        verifySolutions(arr1);
        
        // Test Case 2
        System.out.println("\n--- Test Case 2 ---");
        int[] arr2 = {11, 81, 94, 43, 3};
        verifySolutions(arr2);
        
        // Test Case 3: All same elements
        System.out.println("\n--- Test Case 3: Duplicate Elements ---");
        int[] arr3 = {2, 2, 2, 2};
        verifySolutions(arr3);
        
        // Test Case 4: Increasing array
        System.out.println("\n--- Test Case 4: Increasing Array ---");
        int[] arr4 = {1, 2, 3, 4, 5};
        verifySolutions(arr4);
        
        // Test Case 5: Decreasing array
        System.out.println("\n--- Test Case 5: Decreasing Array ---");
        int[] arr5 = {5, 4, 3, 2, 1};
        verifySolutions(arr5);
        
        // Test Case 6: Single element
        System.out.println("\n--- Test Case 6: Single Element ---");
        int[] arr6 = {42};
        verifySolutions(arr6);
        
        // Test Case 7: Two elements
        System.out.println("\n--- Test Case 7: Two Elements ---");
        int[] arr7 = {7, 3};
        verifySolutions(arr7);
        
        // Test Case 8: Large values (testing modulo)
        System.out.println("\n--- Test Case 8: Large Array ---");
        int[] arr8 = new int[100];
        for (int i = 0; i < 100; i++) {
            arr8[i] = (i % 10) + 1;
        }
        long startTime = System.currentTimeMillis();
        int result = sumSubarrayMinsMonotonicStack(arr8);
        long endTime = System.currentTimeMillis();
        System.out.println("Array size: 100");
        System.out.println("Result: " + result);
        System.out.println("Time taken: " + (endTime - startTime) + "ms");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMPLEXITY ANALYSIS");
        System.out.println("=".repeat(70));
        System.out.println("\n1. Brute Force:");
        System.out.println("   Time: O(n²) - nested loops for all subarrays");
        System.out.println("   Space: O(1) - only variables");
        System.out.println("\n2. Monotonic Stack (Recommended):");
        System.out.println("   Time: O(n) - two passes, each element pushed/popped once");
        System.out.println("   Space: O(n) - stack and arrays");
        System.out.println("\n3. Single Pass Stack:");
        System.out.println("   Time: O(n) - single pass");
        System.out.println("   Space: O(n) - stack only");
        System.out.println("\n4. Dynamic Programming:");
        System.out.println("   Time: O(n) - single pass with stack");
        System.out.println("   Space: O(n) - dp array and stack");
    }
}

/**
 * DETAILED EXPLANATION OF MONOTONIC STACK APPROACH:
 * 
 * Example: arr = [3, 1, 2, 4]
 * 
 * Step 1: Calculate left[] array (distance to previous smaller element)
 * -----------------------------------------------------------------------
 * i=0, arr[0]=3: stack=[], left[0] = 0-(-1) = 1, stack=[0]
 * i=1, arr[1]=1: pop 0 (3>1), stack=[], left[1] = 1-(-1) = 2, stack=[1]
 * i=2, arr[2]=2: stack=[1], left[2] = 2-1 = 1, stack=[1,2]
 * i=3, arr[3]=4: stack=[1,2], left[3] = 3-2 = 1, stack=[1,2,3]
 * 
 * left[] = [1, 2, 1, 1]
 * 
 * Step 2: Calculate right[] array (distance to next smaller element)
 * -----------------------------------------------------------------------
 * i=3, arr[3]=4: stack=[], right[3] = 4-3 = 1, stack=[3]
 * i=2, arr[2]=2: stack=[3], right[2] = 4-2 = 2, stack=[2,3]
 * i=1, arr[1]=1: pop 3,2 (4>=1, 2>=1), stack=[], right[1] = 4-1 = 3, stack=[1]
 * i=0, arr[0]=3: pop 1 (1>=3 is false), stack=[1], right[0] = 1-0 = 1, stack=[0,1]
 * 
 * right[] = [1, 3, 2, 1]
 * 
 * Step 3: Calculate contribution of each element
 * -----------------------------------------------------------------------
 * i=0: 3 * 1 * 1 = 3
 * i=1: 1 * 2 * 3 = 6
 * i=2: 2 * 1 * 2 = 4
 * i=3: 4 * 1 * 1 = 4
 * 
 * Total = 3 + 6 + 4 + 4 = 17 ✓
 * 
 * Why this works:
 * - For arr[1]=1: left[1]=2 means it's minimum for 2 positions to left (itself and i=0)
 *                 right[1]=3 means it's minimum for 3 positions to right (i=1,2,3)
 * - Total subarrays where arr[1] is min = 2 * 3 = 6 subarrays
 * - These are: [1], [3,1], [1,2], [3,1,2], [1,2,4], [3,1,2,4]
 * - Contribution = 1 * 6 = 6
 */
