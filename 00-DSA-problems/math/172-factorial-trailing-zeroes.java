import java.math.BigInteger;
/*
 * Given an integer n, return the number of trailing zeroes in n!.
 * 
 * Note that n! = n * (n - 1) * (n - 2) * ... * 3 * 2 * 1.
 * 
 * Example 1:
 * Input: n = 3
 * Output: 0
 * Explanation: 3! = 6, no trailing zero.
 * 
 * Example 2:
 * Input: n = 5
 * Output: 1
 * Explanation: 5! = 120, one trailing zero.
 * 
 * Example 3:
 * Input: n = 0
 * Output: 0
 */

class FactorialTrailingZeroes {

    // Solution 1: Optimal Mathematical Approach - Count factors of 5
    // Key insight: Trailing zeros come from factors of 10 = 2 * 5
    // Since there are always more factors of 2 than 5, we only need to count
    // factors of 5
    public int trailingZeroes1(int n) {
        int count = 0;

        // Count multiples of 5, 25, 125, 625, ... (powers of 5)
        while (n >= 5) {
            n /= 5;
            count += n;
        }

        return count;
    }

    // Solution 2: Recursive Approach (same logic, different style)
    public int trailingZeroes2(int n) {
        if (n < 5)
            return 0;
        return n / 5 + trailingZeroes2(n / 5);
    }

    // Solution 3: Iterative with Power of 5 (more explicit)
    public int trailingZeroes3(int n) {
        int count = 0;

        for (int i = 5; i <= n; i *= 5) {
            count += n / i;

            // Prevent overflow for large n
            if (i > Integer.MAX_VALUE / 5)
                break;
        }

        return count;
    }

    // Solution 4: Brute Force (for educational purposes - NOT RECOMMENDED for large
    // n)
    // This approach actually calculates factorial and counts trailing zeros
    public int trailingZeroes4(int n) {
        if (n < 0)
            return 0;

        // Use BigInteger to handle large factorials
        BigInteger factorial = BigInteger.ONE;
        for (int i = 1; i <= n; i++) {
            factorial = factorial.multiply(BigInteger.valueOf(i));
        }

        // Count trailing zeros
        String factStr = factorial.toString();
        int count = 0;
        for (int i = factStr.length() - 1; i >= 0; i--) {
            if (factStr.charAt(i) == '0') {
                count++;
            } else {
                break;
            }
        }

        return count;
    }

    // Solution 5: Mathematical with explicit power calculation
    public int trailingZeroes5(int n) {
        int count = 0, power = 5;

        while (power <= n) {
            count += n / power;

            // Check for overflow before multiplying
            if (power > Integer.MAX_VALUE / 5)
                break;
            power *= 5;
        }

        return count;
    }

    // Helper method to demonstrate the mathematical reasoning
    public void explainTrailingZeroes(int n) {
        System.out.println("\nExplanation for n = " + n + ":");
        System.out.println("Trailing zeros come from factors of 10 = 2 × 5");
        System.out.println("In n!, there are always more factors of 2 than 5");
        System.out.println("So we only need to count factors of 5\n");

        int total = 0;
        int power = 5;

        while (power <= n) {
            int contribution = n / power;
            total += contribution;
            System.out.printf("Multiples of %d: %d / %d = %d\n", power, n, power, contribution);

            if (power > Integer.MAX_VALUE / 5)
                break;
            power *= 5;
        }

        System.out.println("Total trailing zeros: " + total);

        // Show some examples of numbers and their prime factorization
        System.out.println("\nFactors of 5 in some numbers:");
        for (int i = 5; i <= Math.min(n, 30); i += 5) {
            int fives = countFactorsOfFive(i);
            System.out.printf("%d has %d factor(s) of 5\n", i, fives);
        }
    }

    // Helper method to count factors of 5 in a number
    private int countFactorsOfFive(int num) {
        int count = 0;
        while (num % 5 == 0) {
            count++;
            num /= 5;
        }
        return count;
    }

    // Performance comparison method
    public void performanceTest(int n) {
        System.out.println("\nPerformance Test for n = " + n);

        long start, end;

        // Test Solution 1
        start = System.nanoTime();
        int result1 = trailingZeroes1(n);
        end = System.nanoTime();
        System.out.printf("Solution 1 (Optimal): %d, Time: %d ns\n", result1, end - start);

        // Test Solution 2
        start = System.nanoTime();
        int result2 = trailingZeroes2(n);
        end = System.nanoTime();
        System.out.printf("Solution 2 (Recursive): %d, Time: %d ns\n", result2, end - start);

        // Test Solution 3
        start = System.nanoTime();
        int result3 = trailingZeroes3(n);
        end = System.nanoTime();
        System.out.printf("Solution 3 (Iterative): %d, Time: %d ns\n", result3, end - start);

        // Only test brute force for small n to avoid timeout
        if (n <= 20) {
            start = System.nanoTime();
            int result4 = trailingZeroes4(n);
            end = System.nanoTime();
            System.out.printf("Solution 4 (Brute Force): %d, Time: %d ns\n", result4, end - start);
        } else {
            System.out.println("Solution 4 (Brute Force): Skipped for large n");
        }
    }

    public static void main(String[] args) {
        FactorialTrailingZeroes solution = new FactorialTrailingZeroes();

        // Test cases
        int[] testCases = { 0, 3, 5, 10, 25, 100, 1000, 10000 };

        System.out.println("Factorial Trailing Zeroes Solutions");
        System.out.println("===================================");

        for (int n : testCases) {
            System.out.printf("\nn = %d:\n", n);

            int result1 = solution.trailingZeroes1(n);
            int result2 = solution.trailingZeroes2(n);
            int result3 = solution.trailingZeroes3(n);

            System.out.printf("Solution 1: %d\n", result1);
            System.out.printf("Solution 2: %d\n", result2);
            System.out.printf("Solution 3: %d\n", result3);

            // Only calculate brute force for small numbers
            if (n <= 20) {
                int result4 = solution.trailingZeroes4(n);
                System.out.printf("Solution 4: %d\n", result4);
            }

            // Verify all solutions match
            boolean allMatch = (result1 == result2) && (result2 == result3);
            System.out.println("Solutions match: " + allMatch);
        }

        // Detailed explanation for a specific case
        solution.explainTrailingZeroes(25);

        // Performance comparison
        solution.performanceTest(1000);

        // Edge cases
        System.out.println("\nEdge Cases:");
        System.out.println("n = 0: " + solution.trailingZeroes1(0));
        System.out.println("n = 1: " + solution.trailingZeroes1(1));
        System.out.println("n = 4: " + solution.trailingZeroes1(4));
        System.out.println("n = 5: " + solution.trailingZeroes1(5));

        System.out.println("\nKey Insight:");
        System.out.println("The number of trailing zeros in n! equals the number of times 10 divides n!");
        System.out.println("Since 10 = 2 × 5, and there are always more factors of 2 than 5 in n!,");
        System.out.println("we only need to count the total number of factors of 5 in 1, 2, 3, ..., n");
        System.out.println("This includes: n/5 + n/25 + n/125 + n/625 + ...");
    }
    
}

/*
 * Mathematical Analysis:
 * 
 * WHY THIS WORKS:
 * - Trailing zeros are created by factors of 10
 * - 10 = 2 × 5
 * - In any factorial, there are always more factors of 2 than factors of 5
 * - Therefore, the number of trailing zeros = number of factors of 5 in n!
 * 
 * HOW TO COUNT FACTORS OF 5:
 * - Numbers divisible by 5: contribute 1 factor each
 * - Numbers divisible by 25: contribute an additional factor (total 2)
 * - Numbers divisible by 125: contribute another additional factor (total 3)
 * - And so on...
 * 
 * FORMULA: floor(n/5) + floor(n/25) + floor(n/125) + floor(n/625) + ...
 * 
 * EXAMPLES:
 * - 5! = 120 → 1 trailing zero (from 5)
 * - 10! = 3,628,800 → 2 trailing zeros (from 5 and 10)
 * - 25! → 6 trailing zeros (5 from multiples of 5, plus 1 extra from 25)
 * 
 * TIME COMPLEXITY: O(log n) - we divide by 5 repeatedly
 * SPACE COMPLEXITY: O(1) - constant extra space
 * 
 * RECOMMENDED SOLUTION: Solution 1 (trailingZeroes1)
 * - Most efficient and clean
 * - Easy to understand once you know the insight
 * - Handles all edge cases
 * - No risk of integer overflow in the counting logic
 */
