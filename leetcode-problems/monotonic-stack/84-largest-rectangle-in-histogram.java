/**
 * Largest Rectangle in Histogram - Complete Solutions
 * 
 * Problem: Given an array of integers representing histogram bar heights (width=1),
 * return the area of the largest rectangle in the histogram.
 * 
 * Key Insights:
 * 1. For each bar, find the maximum rectangle with that bar as the minimum height
 * 2. Rectangle width = right boundary - left boundary + 1
 * 3. Use monotonic stack to find left and right boundaries efficiently
 * 4. Area = height[i] × (right[i] - left[i] - 1)
 * 
 * Core Strategy:
 * - For each bar at index i, find:
 *   • Left boundary: nearest bar on left with height < heights[i]
 *   • Right boundary: nearest bar on right with height < heights[i]
 * - Monotonic increasing stack tracks potential left boundaries
 */

import java.util.Arrays;
import java.util.Stack;

class LargestRectangleHistogram {
    
    // ========================================================================
    // SOLUTION 1: MONOTONIC STACK - TWO PASS (OPTIMAL)
    // Time Complexity: O(n)
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Two-Pass Monotonic Stack Approach
     * 
     * Strategy:
     * 1. First pass: Find left boundary for each bar (previous smaller element)
     * 2. Second pass: Find right boundary for each bar (next smaller element)
     * 3. Calculate area for each bar: height × width
     * 4. Return maximum area
     * 
     * Why Monotonic Stack:
     * - Stack maintains bars in increasing order of height
     * - When we see a shorter bar, it becomes the boundary for taller bars
     * - Each bar pushed/popped once → O(n) time
     * 
     * Example: heights = [2,1,5,6,2,3]
     * 
     * Left boundaries (distance to previous smaller):
     * [0]: no smaller on left → left[0] = -1
     * [1]: no smaller on left → left[1] = -1
     * [2]: 1 is smaller at index 1 → left[2] = 1
     * [3]: 5 is smaller at index 2 → left[3] = 2
     * [4]: 1 is smaller at index 1 → left[4] = 1
     * [5]: 2 is smaller at index 4 → left[5] = 4
     * 
     * Right boundaries (distance to next smaller):
     * [0]: 1 is smaller at index 1 → right[0] = 1
     * [1]: no smaller on right → right[1] = 6
     * [2]: 2 is smaller at index 4 → right[2] = 4
     * [3]: 2 is smaller at index 4 → right[3] = 4
     * [4]: no smaller on right → right[4] = 6
     * [5]: no smaller on right → right[5] = 6
     */
    public static int largestRectangleArea(int[] heights) {
        int n = heights.length;
        if (n == 0) return 0;
        
        // left[i] = index of previous bar with height < heights[i]
        int[] left = new int[n];
        // right[i] = index of next bar with height < heights[i]
        int[] right = new int[n];
        
        Stack<Integer> stack = new Stack<>();
        
        // Find left boundaries (previous smaller element)
        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && heights[stack.peek()] >= heights[i]) {
                stack.pop();
            }
            left[i] = stack.isEmpty() ? -1 : stack.peek();
            stack.push(i);
        }
        
        // Find right boundaries (next smaller element)
        stack.clear();
        for (int i = n - 1; i >= 0; i--) {
            while (!stack.isEmpty() && heights[stack.peek()] >= heights[i]) {
                stack.pop();
            }
            right[i] = stack.isEmpty() ? n : stack.peek();
            stack.push(i);
        }
        
        // Calculate maximum area
        int maxArea = 0;
        for (int i = 0; i < n; i++) {
            int width = right[i] - left[i] - 1;
            int area = heights[i] * width;
            maxArea = Math.max(maxArea, area);
        }
        
        return maxArea;
    }
    
    // ========================================================================
    // SOLUTION 2: MONOTONIC STACK - SINGLE PASS (MOST EFFICIENT)
    // Time Complexity: O(n)
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Single-Pass Monotonic Stack
     * 
     * Strategy:
     * Instead of two passes, calculate area when popping from stack.
     * 
     * When we pop an element:
     * - Popped index is the bar we're calculating area for
     * - Current index is the right boundary
     * - Top of stack after pop is the left boundary
     * 
     * Key Insight:
     * When heights[current] < heights[stack.top], we know:
     * - heights[stack.top] cannot extend beyond current (right boundary found)
     * - All bars between left and current are >= heights[stack.top] (maintained by stack)
     * 
     * This is the MOST ELEGANT solution!
     */
    public static int largestRectangleAreaSinglePass(int[] heights) {
        Stack<Integer> stack = new Stack<>();
        int maxArea = 0;
        int n = heights.length;
        
        for (int i = 0; i <= n; i++) {
            // Use 0 as sentinel value at the end to empty the stack
            int currentHeight = (i == n) ? 0 : heights[i];
            
            // Pop bars that are taller than current
            while (!stack.isEmpty() && heights[stack.peek()] >= currentHeight) {
                int height = heights[stack.pop()];
                int width = stack.isEmpty() ? i : i - stack.peek() - 1;
                maxArea = Math.max(maxArea, height * width);
            }
            
            stack.push(i);
        }
        
        return maxArea;
    }
    
    // ========================================================================
    // SOLUTION 3: DIVIDE AND CONQUER
    // Time Complexity: O(n log n) average, O(n²) worst case
    // Space Complexity: O(log n) recursion stack
    // ========================================================================
    
    /**
     * Divide and Conquer Approach
     * 
     * Strategy:
     * 1. Find minimum height in range
     * 2. Max area is either:
     *    a) minimum_height × range_width (rectangle spanning whole range)
     *    b) Recursively solve for left subarray
     *    c) Recursively solve for right subarray
     * 
     * Not optimal but demonstrates algorithmic thinking.
     */
    public static int largestRectangleAreaDivideConquer(int[] heights) {
        return divideConquer(heights, 0, heights.length - 1);
    }
    
    private static int divideConquer(int[] heights, int left, int right) {
        if (left > right) return 0;
        if (left == right) return heights[left];
        
        // Find minimum height in range
        int minIndex = left;
        for (int i = left; i <= right; i++) {
            if (heights[i] < heights[minIndex]) {
                minIndex = i;
            }
        }
        
        // Area using minimum height as the limiting factor
        int areaWithMin = heights[minIndex] * (right - left + 1);
        
        // Recursively find max in left and right subarrays
        int leftArea = divideConquer(heights, left, minIndex - 1);
        int rightArea = divideConquer(heights, minIndex + 1, right);
        
        return Math.max(areaWithMin, Math.max(leftArea, rightArea));
    }
    
    // ========================================================================
    // SOLUTION 4: BRUTE FORCE (FOR UNDERSTANDING)
    // Time Complexity: O(n²)
    // Space Complexity: O(1)
    // ========================================================================
    
    /**
     * Brute Force Approach
     * 
     * For each bar, expand left and right to find maximum width
     * where all bars are at least as tall as current bar.
     */
    public static int largestRectangleAreaBruteForce(int[] heights) {
        int maxArea = 0;
        
        for (int i = 0; i < heights.length; i++) {
            int minHeight = heights[i];
            
            // Try all possible right boundaries
            for (int j = i; j < heights.length; j++) {
                minHeight = Math.min(minHeight, heights[j]);
                int width = j - i + 1;
                int area = minHeight * width;
                maxArea = Math.max(maxArea, area);
            }
        }
        
        return maxArea;
    }
    
    // ========================================================================
    // SOLUTION 5: VERBOSE VERSION WITH DETAILED TRACKING
    // Time Complexity: O(n)
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Verbose Solution for Understanding
     * 
     * Shows step-by-step calculation of areas.
     */
    public static int largestRectangleAreaVerbose(int[] heights, boolean debug) {
        int n = heights.length;
        if (n == 0) return 0;
        
        if (debug) {
            System.out.println("\n=== Processing heights: " + Arrays.toString(heights) + " ===");
        }
        
        int[] left = new int[n];
        int[] right = new int[n];
        Stack<Integer> stack = new Stack<>();
        
        // Find left boundaries
        if (debug) System.out.println("\nFinding left boundaries:");
        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && heights[stack.peek()] >= heights[i]) {
                stack.pop();
            }
            left[i] = stack.isEmpty() ? -1 : stack.peek();
            if (debug) {
                System.out.printf("  heights[%d]=%d, left boundary index=%d%n", 
                                i, heights[i], left[i]);
            }
            stack.push(i);
        }
        
        // Find right boundaries
        stack.clear();
        if (debug) System.out.println("\nFinding right boundaries:");
        for (int i = n - 1; i >= 0; i--) {
            while (!stack.isEmpty() && heights[stack.peek()] >= heights[i]) {
                stack.pop();
            }
            right[i] = stack.isEmpty() ? n : stack.peek();
            if (debug) {
                System.out.printf("  heights[%d]=%d, right boundary index=%d%n", 
                                i, heights[i], right[i]);
            }
            stack.push(i);
        }
        
        // Calculate areas
        if (debug) System.out.println("\nCalculating areas:");
        int maxArea = 0;
        for (int i = 0; i < n; i++) {
            int width = right[i] - left[i] - 1;
            int area = heights[i] * width;
            maxArea = Math.max(maxArea, area);
            
            if (debug) {
                System.out.printf("  Bar %d: height=%d, width=%d (from index %d to %d), area=%d%n",
                                i, heights[i], width, left[i] + 1, right[i] - 1, area);
            }
        }
        
        if (debug) {
            System.out.println("\nMaximum area: " + maxArea);
        }
        
        return maxArea;
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    /**
     * Visualize the histogram
     */
    public static void visualizeHistogram(int[] heights) {
        System.out.println("\nHistogram Visualization:");
        int maxHeight = 0;
        for (int h : heights) {
            maxHeight = Math.max(maxHeight, h);
        }
        
        // Print from top to bottom
        for (int level = maxHeight; level > 0; level--) {
            for (int height : heights) {
                if (height >= level) {
                    System.out.print("█ ");
                } else {
                    System.out.print("  ");
                }
            }
            System.out.println();
        }
        
        // Print indices
        for (int i = 0; i < heights.length; i++) {
            System.out.print(i + " ");
        }
        System.out.println();
        
        // Print heights
        System.out.print("Heights: ");
        for (int h : heights) {
            System.out.print(h + " ");
        }
        System.out.println();
    }
    
    /**
     * Explain the optimal rectangle for given heights
     */
    public static void explainOptimal(int[] heights) {
        int n = heights.length;
        int[] left = new int[n];
        int[] right = new int[n];
        Stack<Integer> stack = new Stack<>();
        
        // Calculate boundaries
        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && heights[stack.peek()] >= heights[i]) {
                stack.pop();
            }
            left[i] = stack.isEmpty() ? -1 : stack.peek();
            stack.push(i);
        }
        
        stack.clear();
        for (int i = n - 1; i >= 0; i--) {
            while (!stack.isEmpty() && heights[stack.peek()] >= heights[i]) {
                stack.pop();
            }
            right[i] = stack.isEmpty() ? n : stack.peek();
            stack.push(i);
        }
        
        // Find maximum
        int maxArea = 0;
        int maxIndex = 0;
        for (int i = 0; i < n; i++) {
            int width = right[i] - left[i] - 1;
            int area = heights[i] * width;
            if (area > maxArea) {
                maxArea = area;
                maxIndex = i;
            }
        }
        
        System.out.println("\nOptimal Rectangle Analysis:");
        System.out.println("The largest rectangle uses bar at index " + maxIndex);
        System.out.println("  Height: " + heights[maxIndex]);
        System.out.println("  Extends from index " + (left[maxIndex] + 1) + 
                         " to " + (right[maxIndex] - 1));
        System.out.println("  Width: " + (right[maxIndex] - left[maxIndex] - 1));
        System.out.println("  Area: " + maxArea);
    }
    
    /**
     * Test a single case with all solutions
     */
    public static void testCase(String name, int[] heights) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST CASE: " + name);
        System.out.println("=".repeat(70));
        
        visualizeHistogram(heights);
        
        int result1 = largestRectangleArea(heights);
        int result2 = largestRectangleAreaSinglePass(heights);
        int result3 = largestRectangleAreaDivideConquer(heights);
        int result4 = largestRectangleAreaBruteForce(heights);
        
        System.out.println("\nResults:");
        System.out.println("Two-Pass Stack:       " + result1);
        System.out.println("Single-Pass Stack:    " + result2);
        System.out.println("Divide & Conquer:     " + result3);
        System.out.println("Brute Force:          " + result4);
        
        boolean match = (result1 == result2) && (result2 == result3) && (result3 == result4);
        System.out.println("All solutions match:  " + match);
        
        explainOptimal(heights);
        
        System.out.println("\n--- Detailed Trace ---");
        largestRectangleAreaVerbose(heights, true);
    }
    
    // ========================================================================
    // MAIN METHOD WITH TEST CASES
    // ========================================================================
    
    public static void main(String[] args) {
        System.out.println("╔" + "═".repeat(68) + "╗");
        System.out.println("║" + " ".repeat(14) + "LARGEST RECTANGLE IN HISTOGRAM" + " ".repeat(24) + "║");
        System.out.println("╚" + "═".repeat(68) + "╝");
        
        // Example 1
        testCase("Example 1", new int[]{2, 1, 5, 6, 2, 3});
        
        // Example 2
        testCase("Example 2", new int[]{2, 4});
        
        // Additional test cases
        testCase("Single bar", new int[]{5});
        testCase("Increasing heights", new int[]{1, 2, 3, 4, 5});
        testCase("Decreasing heights", new int[]{5, 4, 3, 2, 1});
        testCase("All same height", new int[]{3, 3, 3, 3});
        testCase("Valley shape", new int[]{5, 4, 1, 4, 5});
        testCase("Peak shape", new int[]{1, 4, 5, 4, 1});
        testCase("Multiple peaks", new int[]{2, 1, 2, 3, 1});
        testCase("Large rectangle", new int[]{2, 2, 2, 2, 2});
        testCase("Empty array", new int[]{});
        testCase("Complex histogram", new int[]{6, 2, 5, 4, 5, 1, 6});
        
        // Performance test
        System.out.println("\n" + "=".repeat(70));
        System.out.println("PERFORMANCE TEST");
        System.out.println("=".repeat(70));
        
        int[] largeArray = new int[100000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = (int)(Math.random() * 100) + 1;
        }
        
        long start = System.nanoTime();
        int result = largestRectangleAreaSinglePass(largeArray);
        long end = System.nanoTime();
        
        System.out.println("\nArray size: 100,000");
        System.out.println("Maximum area: " + result);
        System.out.println("Time taken: " + (end - start) / 1_000_000.0 + " ms");
        
        // Complexity analysis
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMPLEXITY ANALYSIS");
        System.out.println("=".repeat(70));
        
        System.out.println("\n1. Single-Pass Stack (MOST RECOMMENDED):");
        System.out.println("   Time:  O(n) - each element pushed/popped once");
        System.out.println("   Space: O(n) - stack space");
        System.out.println("   Pros:  Most elegant, single pass, optimal");
        System.out.println("   Best for: Production code, interviews");
        
        System.out.println("\n2. Two-Pass Stack:");
        System.out.println("   Time:  O(n) - two separate passes");
        System.out.println("   Space: O(n) - stack + arrays");
        System.out.println("   Pros:  Easier to understand, clearer logic");
        System.out.println("   Best for: Learning the concept");
        
        System.out.println("\n3. Divide & Conquer:");
        System.out.println("   Time:  O(n log n) average, O(n²) worst");
        System.out.println("   Space: O(log n) - recursion stack");
        System.out.println("   Pros:  Demonstrates D&C paradigm");
        System.out.println("   Best for: Academic interest");
        
        System.out.println("\n4. Brute Force:");
        System.out.println("   Time:  O(n²)");
        System.out.println("   Space: O(1)");
        System.out.println("   Pros:  Simple to understand");
        System.out.println("   Best for: Understanding problem only");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("KEY INSIGHTS");
        System.out.println("=".repeat(70));
        System.out.println("• For each bar, find max rectangle with that bar as min height");
        System.out.println("• Rectangle width = distance to first shorter bar on each side");
        System.out.println("• Monotonic stack efficiently finds these boundaries");
        System.out.println("• Stack maintains indices of bars in increasing height order");
        System.out.println("• When we pop, we've found the right boundary for that bar");
        System.out.println("• The remaining stack top is the left boundary");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("PATTERN RECOGNITION");
        System.out.println("=".repeat(70));
        System.out.println("\n1. Monotonic Stack Pattern:");
        System.out.println("   - Find nearest smaller element on left/right");
        System.out.println("   - Calculate area/distance between boundaries");
        System.out.println("   - Similar to: Stock span, rain water, subarray problems");
        
        System.out.println("\n2. When to use each approach:");
        System.out.println("   - Interview: Single-pass stack (optimal + elegant)");
        System.out.println("   - Learning: Two-pass stack (clearer logic)");
        System.out.println("   - Small input: Any approach works");
        System.out.println("   - Large input: Only O(n) solutions acceptable");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMMON MISTAKES TO AVOID");
        System.out.println("=".repeat(70));
        System.out.println("✗ Forgetting to handle empty array");
        System.out.println("✗ Not using sentinel value (0) at end for single-pass");
        System.out.println("✗ Incorrect width calculation: should be (right - left - 1)");
        System.out.println("✗ Using >= instead of >= in stack condition (important for duplicates)");
        System.out.println("✗ Not clearing stack between passes in two-pass approach");
    }
}

/**
 * ============================================================================
 * DETAILED ALGORITHM WALKTHROUGH - SINGLE PASS
 * ============================================================================
 * 
 * Example: heights = [2, 1, 5, 6, 2, 3]
 * 
 * Goal: Find maximum rectangular area
 * 
 * Process with single-pass monotonic stack:
 * 
 * i=0, h=2:
 *   Stack: []
 *   Push 0
 *   Stack: [0]
 * 
 * i=1, h=1:
 *   Current (1) < stack top height (2)
 *   Pop 0, calculate: height=2, width=1 (1-(-1)-1=1), area=2
 *   Push 1
 *   Stack: [1]
 * 
 * i=2, h=5:
 *   Current (5) >= stack top (1)
 *   Push 2
 *   Stack: [1, 2]
 * 
 * i=3, h=6:
 *   Current (6) >= stack top (5)
 *   Push 3
 *   Stack: [1, 2, 3]
 * 
 * i=4, h=2:
 *   Current (2) < stack top (6)
 *   Pop 3: height=6, width=1 (4-2-1=1), area=6
 *   Current (2) < stack top (5)
 *   Pop 2: height=5, width=2 (4-1-1=2), area=10 ← Maximum!
 *   Current (2) >= stack top (1)
 *   Push 4
 *   Stack: [1, 4]
 * 
 * i=5, h=3:
 *   Current (3) >= stack top (2)
 *   Push 5
 *   Stack: [1, 4, 5]
 * 
 * i=6 (sentinel, h=0):
 *   Pop 5: height=3, width=1 (6-4-1=1), area=3
 *   Pop 4: height=2, width=4 (6-1-1=4), area=8
 *   Pop 1: height=1, width=6 (6-(-1)-1=6), area=6
 *   Stack: []
 * 
 * Maximum area: 10
 * 
 * The rectangle with area 10 uses bars at indices 2 and 3 with height 5.
 * 
 * ============================================================================
 * WHY MONOTONIC STACK WORKS
 * ============================================================================
 * 
 * Key Properties:
 * 
 * 1. Stack maintains indices of bars in INCREASING height order
 * 2. When we encounter a shorter bar, it means:
 *    - All taller bars in stack cannot extend beyond this point (right boundary)
 * 3. When we pop an index from stack:
 *    - Current index = right boundary (first bar shorter than popped)
 *    - New stack top = left boundary (previous bar shorter than popped)
 * 4. Width = right - left - 1 (excluding boundaries)
 * 
 * Visualization for [2, 1, 5, 6, 2, 3]:
 * 
 * For bar at index 2 (height=5):
 * - Left boundary: index 1 (height=1, first smaller on left)
 * - Right boundary: index 4 (height=2, first smaller on right)
 * - Width: 4 - 1 - 1 = 2 (includes indices 2 and 3)
 * - Area: 5 × 2 = 10
 * 
 * This works because all bars between left and right boundaries
 * are guaranteed to be >= 5 (maintained by monotonic stack property).
 * 
 * ============================================================================
 */
