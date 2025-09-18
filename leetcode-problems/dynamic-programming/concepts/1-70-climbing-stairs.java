/*
 * You are climbing a staircase. It takes n steps to reach the top.
 * 
 * Each time you can either climb 1 or 2 steps. In how many distinct ways can
 * you climb to the top?
 * 
 * Example 1:
 * Input: n = 2
 * Output: 2
 * Explanation: There are two ways to climb to the top.
 * 1. 1 step + 1 step
 * 2. 2 steps
 * 
 * Example 2:
 * Input: n = 3
 * Output: 3
 * Explanation: There are three ways to climb to the top.
 * 1. 1 step + 1 step + 1 step
 * 2. 1 step + 2 steps
 * 3. 2 steps + 1 step
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ClimbingStairsSolution {

    public int climbStairs(int n) {
        if (n == 0 || n == 1)
            return 1;
        return climbStairs(n - 1) + climbStairs(n - 2);
    }

    public int climbStairsMemo(int n) {
        return climbStairsMemo(n, new Integer[n + 1]);
    }

    public int climbStairsMemo(int n, Integer[] memo) {
        if (n == 0 || n == 1)
            return 1;
        if (n < 0) {
            return 0;
        }
        if (memo[n] != null) {
            return memo[n];
        }
        return memo[n] = climbStairsMemo(n - 1, memo) + climbStairsMemo(n - 2, memo);
    }

}

class ClimbingStairs {

    // Solution 1: Recursive (Brute Force)
    // Time Complexity: O(2^n), Space Complexity: O(n) - call stack
    // Note: This will be very slow for large n due to repeated calculations
    public int climbStairs1(int n) {
        if (n <= 1) {
            return 1;
        }
        return climbStairs1(n - 1) + climbStairs1(n - 2);
    }

    // Solution 2: Memoization (Top-down Dynamic Programming)
    // Time Complexity: O(n), Space Complexity: O(n)
    public int climbStairs2(int n) {
        Map<Integer, Integer> memo = new HashMap<>();
        return climbStairsHelper(n, memo);
    }

    private int climbStairsHelper(int n, Map<Integer, Integer> memo) {
        if (n <= 1) {
            return 1;
        }

        if (memo.containsKey(n)) {
            return memo.get(n);
        }

        int result = climbStairsHelper(n - 1, memo) + climbStairsHelper(n - 2, memo);
        memo.put(n, result);
        return result;
    }

    // Solution 3: Bottom-up Dynamic Programming (Tabulation)
    // Time Complexity: O(n), Space Complexity: O(n)
    public int climbStairs3(int n) {
        if (n <= 1) {
            return 1;
        }

        int[] dp = new int[n + 1];
        dp[0] = 1; // 0 steps: 1 way (stay at ground)
        dp[1] = 1; // 1 step: 1 way

        for (int i = 2; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
        }

        return dp[n];
    }

    // Solution 4: Space-Optimized Dynamic Programming
    // Time Complexity: O(n), Space Complexity: O(1)
    // This is the most efficient solution
    public int climbStairs4(int n) {
        if (n <= 1) {
            return 1;
        }

        int prev2 = 1; // ways to reach step i-2
        int prev1 = 1; // ways to reach step i-1

        for (int i = 2; i <= n; i++) {
            int current = prev1 + prev2;
            prev2 = prev1;
            prev1 = current;
        }

        return prev1;
    }

    // Solution 5: Mathematical Formula (Fibonacci)
    // Time Complexity: O(log n), Space Complexity: O(1)
    // Using Binet's formula - less practical due to floating point precision
    public int climbStairs5(int n) {
        if (n <= 1) {
            return 1;
        }

        double sqrt5 = Math.sqrt(5);
        double phi = (1 + sqrt5) / 2;
        double psi = (1 - sqrt5) / 2;

        // F(n+1) where F is Fibonacci sequence
        return (int) Math.round((Math.pow(phi, n + 1) - Math.pow(psi, n + 1)) / sqrt5);
    }

    // Solution 6: Matrix Exponentiation
    // Time Complexity: O(log n), Space Complexity: O(1)
    // Advanced approach using matrix multiplication
    public int climbStairs6(int n) {
        if (n <= 1) {
            return 1;
        }

        int[][] base = { { 1, 1 }, { 1, 0 } };
        int[][] result = matrixPower(base, n);
        return result[0][0];
    }

    private int[][] matrixPower(int[][] matrix, int n) {
        if (n == 1) {
            return matrix;
        }

        int[][] half = matrixPower(matrix, n / 2);
        int[][] result = multiplyMatrix(half, half);

        if (n % 2 == 1) {
            result = multiplyMatrix(result, matrix);
        }

        return result;
    }

    private int[][] multiplyMatrix(int[][] a, int[][] b) {
        return new int[][] {
                { a[0][0] * b[0][0] + a[0][1] * b[1][0], a[0][0] * b[0][1] + a[0][1] * b[1][1] },
                { a[1][0] * b[0][0] + a[1][1] * b[1][0], a[1][0] * b[0][1] + a[1][1] * b[1][1] }
        };
    }

    // Bonus: Method to show all possible ways (for small n)
    public List<List<Integer>> getAllWays(int n) {
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> currentPath = new ArrayList<>();
        generateWays(n, currentPath, result);
        return result;
    }

    private void generateWays(int remaining, List<Integer> currentPath, List<List<Integer>> result) {
        if (remaining == 0) {
            result.add(new ArrayList<>(currentPath));
            return;
        }

        if (remaining >= 1) {
            currentPath.add(1);
            generateWays(remaining - 1, currentPath, result);
            currentPath.remove(currentPath.size() - 1);
        }

        if (remaining >= 2) {
            currentPath.add(2);
            generateWays(remaining - 2, currentPath, result);
            currentPath.remove(currentPath.size() - 1);
        }
    }

    // Test method
    public static void main(String[] args) {
        ClimbingStairs solution = new ClimbingStairs();

        int[] testCases = { 1, 2, 3, 4, 5, 10, 20 };

        System.out.println("Climbing Stairs - All Solutions");
        System.out.println("================================");

        for (int n : testCases) {
            System.out.printf("\nn = %d:\n", n);

            // Test all solutions (skip recursive for large n)
            if (n <= 10) {
                int result1 = solution.climbStairs1(n);
                System.out.printf("Recursive: %d\n", result1);
            } else {
                System.out.println("Recursive: (skipped - too slow)");
            }

            int result2 = solution.climbStairs2(n);
            int result3 = solution.climbStairs3(n);
            int result4 = solution.climbStairs4(n);
            int result5 = solution.climbStairs5(n);
            int result6 = solution.climbStairs6(n);

            System.out.printf("Memoization: %d\n", result2);
            System.out.printf("DP Array: %d\n", result3);
            System.out.printf("Space Optimized: %d\n", result4);
            System.out.printf("Mathematical: %d\n", result5);
            System.out.printf("Matrix Power: %d\n", result6);
        }

        // Show all possible ways for small examples
        System.out.println("\n" + "=".repeat(40));
        System.out.println("All possible ways for small n:");

        for (int n = 1; n <= 5; n++) {
            List<List<Integer>> ways = solution.getAllWays(n);
            System.out.printf("\nn = %d (%d ways):\n", n, ways.size());
            for (int i = 0; i < ways.size(); i++) {
                List<Integer> way = ways.get(i);
                System.out.printf("%d. %s", i + 1, way.toString());

                // Show the sum to verify
                int sum = way.stream().mapToInt(Integer::intValue).sum();
                System.out.printf(" (sum: %d)\n", sum);
            }
        }

        // Performance comparison for larger n
        System.out.println("\n" + "=".repeat(40));
        System.out.println("Performance Test (n = 35):");
        int n = 35;

        long start, end;

        // Test memoization
        start = System.nanoTime();
        int result2 = solution.climbStairs2(n);
        end = System.nanoTime();
        System.out.printf("Memoization: %d (%.2f ms)\n", result2, (end - start) / 1_000_000.0);

        // Test DP array
        start = System.nanoTime();
        int result3 = solution.climbStairs3(n);
        end = System.nanoTime();
        System.out.printf("DP Array: %d (%.2f ms)\n", result3, (end - start) / 1_000_000.0);

        // Test space optimized
        start = System.nanoTime();
        int result4 = solution.climbStairs4(n);
        end = System.nanoTime();
        System.out.printf("Space Optimized: %d (%.2f ms)\n", result4, (end - start) / 1_000_000.0);

        // Test mathematical
        start = System.nanoTime();
        int result5 = solution.climbStairs5(n);
        end = System.nanoTime();
        System.out.printf("Mathematical: %d (%.2f ms)\n", result5, (end - start) / 1_000_000.0);

        // Test matrix power
        start = System.nanoTime();
        int result6 = solution.climbStairs6(n);
        end = System.nanoTime();
        System.out.printf("Matrix Power: %d (%.2f ms)\n", result6, (end - start) / 1_000_000.0);
    }
}

/*
 * ALGORITHM EXPLANATIONS:
 * 
 * 1. RECURSIVE BRUTE FORCE:
 * - Base case: n ≤ 1 returns 1
 * - Recurrence: f(n) = f(n-1) + f(n-2)
 * - Very slow due to overlapping subproblems
 * 
 * 2. MEMOIZATION (Top-down DP):
 * - Same recursive logic but cache results
 * - Eliminates redundant calculations
 * - Good for learning DP concepts
 * 
 * 3. BOTTOM-UP DP (Tabulation):
 * - Build solution from bottom up
 * - Fill array from dp[0] to dp[n]
 * - More intuitive than memoization
 * 
 * 4. SPACE-OPTIMIZED DP:
 * - Only need previous 2 values
 * - Reduces space from O(n) to O(1)
 * - Most practical solution
 * 
 * 5. MATHEMATICAL FORMULA:
 * - Uses Binet's formula for Fibonacci
 * - Fastest but precision issues for large n
 * - More theoretical than practical
 * 
 * 6. MATRIX EXPONENTIATION:
 * - Advanced technique using matrix multiplication
 * - Logarithmic time complexity
 * - Good for very large n
 * 
 * KEY INSIGHTS:
 * - This is essentially the Fibonacci sequence: F(n+1)
 * - f(0) = 1, f(1) = 1, f(n) = f(n-1) + f(n-2)
 * - To reach step n, you can come from step (n-1) or step (n-2)
 * - Dynamic Programming eliminates redundant calculations
 * 
 * PATTERN RECOGNITION:
 * n=1: 1 way [1]
 * n=2: 2 ways [1,1] [2]
 * n=3: 3 ways [1,1,1] [1,2] [2,1]
 * n=4: 5 ways [1,1,1,1] [1,1,2] [1,2,1] [2,1,1] [2,2]
 * n=5: 8 ways ... (Fibonacci: 1,1,2,3,5,8,13...)
 * 
 * INTERVIEW TIP:
 * - Start with recursive solution to show understanding
 * - Optimize to space-efficient DP for the best solution
 * - Mention mathematical approach to show advanced knowledge
 */
