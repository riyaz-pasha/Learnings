/*
 * There are n children standing in a line. Each child is assigned a rating
 * value given in the integer array ratings.
 * 
 * You are giving candies to these children subjected to the following
 * requirements:
 * 
 * Each child must have at least one candy.
 * Children with a higher rating get more candies than their neighbors.
 * Return the minimum number of candies you need to have to distribute the
 * candies to the children.
 * 
 * Example 1:
 * Input: ratings = [1,0,2]
 * Output: 5
 * Explanation: You can allocate to the first, second and third child with 2, 1,
 * 2 candies respectively.
 * 
 * Example 2:
 * Input: ratings = [1,2,2]
 * Output: 4
 * Explanation: You can allocate to the first, second and third child with 1, 2,
 * 1 candies respectively.
 * The third child gets 1 candy because it satisfies the above two conditions.
 */

class CandyDistribution {

    /**
     * Solution 1: Two-Pass Greedy Algorithm (Optimal)
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     * 
     * Algorithm:
     * 1. Initialize all children with 1 candy
     * 2. Left to right pass: If current child has higher rating than left neighbor,
     * give them one more candy than the left neighbor
     * 3. Right to left pass: If current child has higher rating than right
     * neighbor,
     * ensure they have at least one more candy than the right neighbor
     */
    public int candy(int[] ratings) {
        int n = ratings.length;
        int[] candies = new int[n];

        // Initialize all children with 1 candy
        for (int i = 0; i < n; i++) {
            candies[i] = 1;
        }

        // Left to right pass
        for (int i = 1; i < n; i++) {
            if (ratings[i] > ratings[i - 1]) {
                candies[i] = candies[i - 1] + 1;
            }
        }

        // Right to left pass
        for (int i = n - 2; i >= 0; i--) {
            if (ratings[i] > ratings[i + 1]) {
                candies[i] = Math.max(candies[i], candies[i + 1] + 1);
            }
        }

        // Calculate total candies
        int total = 0;
        for (int candy : candies) {
            total += candy;
        }

        return total;
    }

    /**
     * Solution 2: One-Pass Optimized (Space Efficient)
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * This approach uses the concept of peaks and valleys to minimize space usage.
     * It's more complex but uses constant extra space.
     */
    public int candyOptimized(int[] ratings) {
        if (ratings.length <= 1)
            return ratings.length;

        int candies = 0;
        int up = 0, down = 0, peak = 0;

        for (int i = 1; i < ratings.length; i++) {
            if (ratings[i - 1] < ratings[i]) {
                // Ascending sequence
                up++;
                down = 0;
                peak = up;
                candies += 1 + up;
            } else if (ratings[i - 1] > ratings[i]) {
                // Descending sequence
                up = 0;
                down++;
                candies += 1 + down;
                if (peak >= down) {
                    candies--;
                }
            } else {
                // Equal ratings
                up = down = peak = 0;
                candies += 1;
            }
        }

        return candies + 1; // Add 1 for the first child
    }

    /**
     * Solution 3: Brute Force with Validation (For Understanding)
     * Time Complexity: O(n²) in worst case
     * Space Complexity: O(n)
     * 
     * This solution repeatedly adjusts candy counts until all constraints are
     * satisfied.
     * Not optimal but helps understand the problem constraints.
     */
    public int candyBruteForce(int[] ratings) {
        int n = ratings.length;
        int[] candies = new int[n];

        // Initialize all children with 1 candy
        for (int i = 0; i < n; i++) {
            candies[i] = 1;
        }

        boolean changed = true;
        while (changed) {
            changed = false;

            // Check all constraints
            for (int i = 0; i < n; i++) {
                // Check left neighbor
                if (i > 0 && ratings[i] > ratings[i - 1] && candies[i] <= candies[i - 1]) {
                    candies[i] = candies[i - 1] + 1;
                    changed = true;
                }

                // Check right neighbor
                if (i < n - 1 && ratings[i] > ratings[i + 1] && candies[i] <= candies[i + 1]) {
                    candies[i] = candies[i + 1] + 1;
                    changed = true;
                }
            }
        }

        int total = 0;
        for (int candy : candies) {
            total += candy;
        }

        return total;
    }

    // Test method to verify solutions
    public static void main(String[] args) {
        CandyDistribution solution = new CandyDistribution();

        // Test Case 1
        int[] ratings1 = { 1, 0, 2 };
        System.out.println("Test 1 - Ratings: [1,0,2]");
        System.out.println("Two-pass result: " + solution.candy(ratings1));
        System.out.println("Optimized result: " + solution.candyOptimized(ratings1));
        System.out.println("Brute force result: " + solution.candyBruteForce(ratings1));
        System.out.println("Expected: 5\n");

        // Test Case 2
        int[] ratings2 = { 1, 2, 2 };
        System.out.println("Test 2 - Ratings: [1,2,2]");
        System.out.println("Two-pass result: " + solution.candy(ratings2));
        System.out.println("Optimized result: " + solution.candyOptimized(ratings2));
        System.out.println("Brute force result: " + solution.candyBruteForce(ratings2));
        System.out.println("Expected: 4\n");

        // Test Case 3 - Edge case
        int[] ratings3 = { 1, 3, 2, 2, 1 };
        System.out.println("Test 3 - Ratings: [1,3,2,2,1]");
        System.out.println("Two-pass result: " + solution.candy(ratings3));
        System.out.println("Optimized result: " + solution.candyOptimized(ratings3));
        System.out.println("Brute force result: " + solution.candyBruteForce(ratings3));
        System.out.println("Expected: 7 (candies: [1,2,1,2,1])\n");

        // Test Case 4 - Descending sequence
        int[] ratings4 = { 5, 4, 3, 2, 1 };
        System.out.println("Test 4 - Ratings: [5,4,3,2,1]");
        System.out.println("Two-pass result: " + solution.candy(ratings4));
        System.out.println("Optimized result: " + solution.candyOptimized(ratings4));
        System.out.println("Brute force result: " + solution.candyBruteForce(ratings4));
        System.out.println("Expected: 15 (candies: [5,4,3,2,1])\n");
    }

}
