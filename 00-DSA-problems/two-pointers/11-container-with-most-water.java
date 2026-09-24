/*
 * You are given an integer array height of length n. There are n vertical lines
 * drawn such that the two endpoints of the ith line are (i, 0) and (i,
 * height[i]).
 * 
 * Find two lines that together with the x-axis form a container, such that the
 * container contains the most water.
 * 
 * Return the maximum amount of water a container can store.
 * 
 * Notice that you may not slant the container.
 * 
 * Example 1:
 * Input: height = [1,8,6,2,5,4,8,3,7]
 * Output: 49
 * Explanation: The above vertical lines are represented by array
 * [1,8,6,2,5,4,8,3,7]. In this case, the max area of water (blue section) the
 * container can contain is 49.
 * 
 * Example 2:
 * Input: height = [1,1]
 * Output: 1
 */

class ContainerWithMostWater {

    // Solution 1: Brute Force Approach
    // Time Complexity: O(n²), Space Complexity: O(1)
    public int maxAreaBruteForce(int[] height) {
        int maxArea = 0;
        int n = height.length;

        // Check all possible pairs of lines
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                // Width is the distance between the two lines
                int width = j - i;
                // Height is limited by the shorter line
                int h = Math.min(height[i], height[j]);
                // Calculate area
                int area = width * h;
                maxArea = Math.max(maxArea, area);
            }
        }

        return maxArea;
    }

    // Solution 2: Two Pointer Approach (Optimal)
    // Time Complexity: O(n), Space Complexity: O(1)
    public int maxArea(int[] height) {
        int left = 0;
        int right = height.length - 1;
        int maxArea = 0;

        while (left < right) {
            // Calculate current area
            int width = right - left;
            int h = Math.min(height[left], height[right]);
            int currentArea = width * h;
            maxArea = Math.max(maxArea, currentArea);

            // Move the pointer with smaller height
            // This is the key insight: moving the taller pointer
            // will never give us a larger area
            if (height[left] < height[right]) {
                left++;
            } else {
                right--;
            }
        }

        return maxArea;
    }

    // Solution 3: Two Pointer with Early Termination Optimization
    // Time Complexity: O(n), Space Complexity: O(1)
    public int maxAreaOptimized(int[] height) {
        int left = 0;
        int right = height.length - 1;
        int maxArea = 0;

        while (left < right) {
            int width = right - left;
            int leftHeight = height[left];
            int rightHeight = height[right];
            int h = Math.min(leftHeight, rightHeight);
            int currentArea = width * h;

            maxArea = Math.max(maxArea, currentArea);

            // Move pointer with smaller height
            if (leftHeight < rightHeight) {
                // Skip all heights smaller than or equal to current left height
                while (left < right && height[left] <= leftHeight) {
                    left++;
                }
            } else {
                // Skip all heights smaller than or equal to current right height
                while (left < right && height[right] <= rightHeight) {
                    right--;
                }
            }
        }

        return maxArea;
    }

    // Test method to demonstrate all solutions
    public static void main(String[] args) {
        ContainerWithMostWater solution = new ContainerWithMostWater();

        // Test case 1
        int[] height1 = { 1, 8, 6, 2, 5, 4, 8, 3, 7 };
        System.out.println("Test case 1: [1,8,6,2,5,4,8,3,7]");
        System.out.println("Brute Force: " + solution.maxAreaBruteForce(height1));
        System.out.println("Two Pointer: " + solution.maxArea(height1));
        System.out.println("Optimized: " + solution.maxAreaOptimized(height1));
        System.out.println();

        // Test case 2
        int[] height2 = { 1, 1 };
        System.out.println("Test case 2: [1,1]");
        System.out.println("Brute Force: " + solution.maxAreaBruteForce(height2));
        System.out.println("Two Pointer: " + solution.maxArea(height2));
        System.out.println("Optimized: " + solution.maxAreaOptimized(height2));
        System.out.println();

        // Test case 3
        int[] height3 = { 1, 2, 1 };
        System.out.println("Test case 3: [1,2,1]");
        System.out.println("Brute Force: " + solution.maxAreaBruteForce(height3));
        System.out.println("Two Pointer: " + solution.maxArea(height3));
        System.out.println("Optimized: " + solution.maxAreaOptimized(height3));
    }

}

/*
 * ALGORITHM EXPLANATION:
 * 
 * The key insight for the optimal solution is the two-pointer approach:
 * 
 * 1. Start with two pointers at the beginning and end of the array
 * 2. Calculate the area formed by these two lines
 * 3. Move the pointer that points to the shorter line inward
 * 4. Repeat until pointers meet
 * 
 * WHY THIS WORKS:
 * - The area is limited by the shorter line: area = width × min(height[left],
 * height[right])
 * - If we move the pointer with the taller line, we decrease width but the
 * height
 * is still limited by the shorter line, so area can only decrease
 * - By moving the pointer with the shorter line, we have a chance to find a
 * taller
 * line that could potentially give us a larger area
 * 
 * TIME COMPLEXITY ANALYSIS:
 * - Brute Force: O(n²) - check all pairs
 * - Two Pointer: O(n) - each element is visited at most once
 * - Optimized: O(n) - same as two pointer but with early termination for
 * duplicates
 * 
 * SPACE COMPLEXITY: O(1) for all solutions - only using constant extra space
 */
