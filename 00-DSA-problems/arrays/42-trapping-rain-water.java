import java.util.*;
/*
 * Given n non-negative integers representing an elevation map where the width
 * of each bar is 1, compute how much water it can trap after raining.
 * 
 * Example 1:
 * Input: height = [0,1,0,2,1,0,1,3,2,1,2,1]
 * Output: 6
 * Explanation: The above elevation map (black section) is represented by array
 * [0,1,0,2,1,0,1,3,2,1,2,1]. In this case, 6 units of rain water (blue section)
 * are being trapped.
 * 
 * Example 2:
 * Input: height = [4,2,0,3,2,5]
 * Output: 9
 */

class TrappingRainwater {

    /**
     * Solution 1: Brute Force
     * Time Complexity: O(n²)
     * Space Complexity: O(1)
     * 
     * For each position, find the maximum height to the left and right,
     * then calculate water that can be trapped at that position.
     */
    public int trapBruteForce(int[] height) {
        int n = height.length;
        int totalWater = 0;

        for (int i = 1; i < n - 1; i++) {
            // Find maximum height to the left
            int leftMax = 0;
            for (int j = 0; j < i; j++) {
                leftMax = Math.max(leftMax, height[j]);
            }

            // Find maximum height to the right
            int rightMax = 0;
            for (int j = i + 1; j < n; j++) {
                rightMax = Math.max(rightMax, height[j]);
            }

            // Water level at position i is minimum of left and right max
            int waterLevel = Math.min(leftMax, rightMax);

            // If water level is higher than current height, we can trap water
            if (waterLevel > height[i]) {
                totalWater += waterLevel - height[i];
            }
        }

        return totalWater;
    }

    /**
     * Solution 2: Dynamic Programming (Precompute Max Heights)
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     * 
     * Precompute maximum heights to left and right for each position.
     */
    public int trapDP(int[] height) {
        int n = height.length;
        if (n <= 2)
            return 0;

        int[] leftMax = new int[n];
        int[] rightMax = new int[n];

        // Precompute maximum height to the left of each position
        leftMax[0] = height[0];
        for (int i = 1; i < n; i++) {
            leftMax[i] = Math.max(leftMax[i - 1], height[i]);
        }

        // Precompute maximum height to the right of each position
        rightMax[n - 1] = height[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            rightMax[i] = Math.max(rightMax[i + 1], height[i]);
        }

        int totalWater = 0;
        for (int i = 0; i < n; i++) {
            int waterLevel = Math.min(leftMax[i], rightMax[i]);
            if (waterLevel > height[i]) {
                totalWater += waterLevel - height[i];
            }
        }

        return totalWater;
    }

    /**
     * Solution 3: Two Pointers (Optimal)
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Use two pointers moving towards each other, keeping track of max heights.
     * Key insight: We only need to know if leftMax < rightMax or vice versa.
     */
    public int trapTwoPointers(int[] height) {
        int n = height.length;
        if (n <= 2)
            return 0;

        int left = 0, right = n - 1;
        int leftMax = 0, rightMax = 0;
        int totalWater = 0;

        while (left < right) {
            if (height[left] < height[right]) {
                // Process left side
                if (height[left] >= leftMax) {
                    leftMax = height[left];
                } else {
                    totalWater += leftMax - height[left];
                }
                left++;
            } else {
                // Process right side
                if (height[right] >= rightMax) {
                    rightMax = height[right];
                } else {
                    totalWater += rightMax - height[right];
                }
                right--;
            }
        }

        return totalWater;
    }

    /**
     * Solution 4: Stack-based Approach
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     * 
     * Use a stack to keep track of potential water trapping areas.
     * When we find a bar higher than the top of stack, we can trap water.
     */
    public int trapStack(int[] height) {
        int n = height.length;
        if (n <= 2)
            return 0;

        Stack<Integer> stack = new Stack<>();
        int totalWater = 0;

        for (int i = 0; i < n; i++) {
            // While stack is not empty and current height is greater than
            // height at stack top, we can trap water
            while (!stack.isEmpty() && height[i] > height[stack.peek()]) {
                int bottom = stack.pop();

                if (stack.isEmpty())
                    break;

                int distance = i - stack.peek() - 1;
                int boundedHeight = Math.min(height[i], height[stack.peek()]) - height[bottom];
                totalWater += distance * boundedHeight;
            }
            stack.push(i);
        }

        return totalWater;
    }

    /**
     * Helper method to visualize the elevation map and trapped water
     */
    public void visualize(int[] height) {
        int maxHeight = Arrays.stream(height).max().orElse(0);
        System.out.println("Elevation Map:");

        for (int level = maxHeight; level > 0; level--) {
            for (int i = 0; i < height.length; i++) {
                if (height[i] >= level) {
                    System.out.print("█ ");
                } else {
                    // Check if water can be trapped at this level
                    int leftMax = 0, rightMax = 0;
                    for (int j = 0; j < i; j++) {
                        leftMax = Math.max(leftMax, height[j]);
                    }
                    for (int j = i + 1; j < height.length; j++) {
                        rightMax = Math.max(rightMax, height[j]);
                    }

                    if (Math.min(leftMax, rightMax) >= level) {
                        System.out.print("~ "); // Water
                    } else {
                        System.out.print("  "); // Air
                    }
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    // Test all solutions
    public static void main(String[] args) {
        TrappingRainwater solution = new TrappingRainwater();

        // Test Case 1
        int[] height1 = { 0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1 };
        System.out.println("Test 1 - Height: " + Arrays.toString(height1));
        solution.visualize(height1);
        System.out.println("Brute Force: " + solution.trapBruteForce(height1));
        System.out.println("Dynamic Programming: " + solution.trapDP(height1));
        System.out.println("Two Pointers: " + solution.trapTwoPointers(height1));
        System.out.println("Stack: " + solution.trapStack(height1));
        System.out.println("Expected: 6\n");

        // Test Case 2
        int[] height2 = { 4, 2, 0, 3, 2, 5 };
        System.out.println("Test 2 - Height: " + Arrays.toString(height2));
        solution.visualize(height2);
        System.out.println("Brute Force: " + solution.trapBruteForce(height2));
        System.out.println("Dynamic Programming: " + solution.trapDP(height2));
        System.out.println("Two Pointers: " + solution.trapTwoPointers(height2));
        System.out.println("Stack: " + solution.trapStack(height2));
        System.out.println("Expected: 9\n");

        // Test Case 3 - Edge cases
        int[] height3 = { 3, 0, 2, 0, 4 };
        System.out.println("Test 3 - Height: " + Arrays.toString(height3));
        solution.visualize(height3);
        System.out.println("Two Pointers: " + solution.trapTwoPointers(height3));
        System.out.println("Expected: 10\n");

        // Test Case 4 - No water trapped
        int[] height4 = { 1, 2, 3, 4, 5 };
        System.out.println("Test 4 - Height: " + Arrays.toString(height4));
        System.out.println("Two Pointers: " + solution.trapTwoPointers(height4));
        System.out.println("Expected: 0 (ascending order - no water trapped)\n");

        // Performance comparison for large input
        System.out.println("=== Performance Test ===");
        int[] largeInput = new int[10000];
        Random rand = new Random(42);
        for (int i = 0; i < largeInput.length; i++) {
            largeInput[i] = rand.nextInt(100);
        }

        long startTime = System.nanoTime();
        int result1 = solution.trapDP(largeInput);
        long dpTime = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        int result2 = solution.trapTwoPointers(largeInput);
        long twoPointerTime = System.nanoTime() - startTime;

        System.out.println("Large input (10000 elements):");
        System.out.println("DP result: " + result1 + ", Time: " + dpTime / 1000000.0 + " ms");
        System.out.println("Two Pointers result: " + result2 + ", Time: " + twoPointerTime / 1000000.0 + " ms");
        System.out.println("Results match: " + (result1 == result2));
    }

}
