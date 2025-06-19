/*
 * Implement pow(x, n), which calculates x raised to the power n (i.e., xn).
 * 
 * Example 1:
 * Input: x = 2.00000, n = 10
 * Output: 1024.00000
 * 
 * Example 2:
 * Input: x = 2.10000, n = 3
 * Output: 9.26100
 *
 *  Example 3:
 * Input: x = 2.00000, n = -2
 * Output: 0.25000
 * Explanation: 2-2 = 1/22 = 1/4 = 0.25
 */

class PowerFunction {

    // Solution 1: Fast Exponentiation (Iterative) - OPTIMAL
    // Time: O(log n), Space: O(1)
    public double myPow1(double x, int n) {
        if (n == 0)
            return 1.0;

        long N = n; // Use long to handle Integer.MIN_VALUE
        if (N < 0) {
            x = 1 / x;
            N = -N;
        }

        double result = 1.0;
        double currentPower = x;

        while (N > 0) {
            if (N % 2 == 1) { // If N is odd
                result *= currentPower;
            }
            currentPower *= currentPower; // Square the current power
            N /= 2; // Divide N by 2
        }

        return result;
    }

    // Solution 2: Fast Exponentiation (Recursive) - ELEGANT
    // Time: O(log n), Space: O(log n) due to recursion stack
    public double myPow2(double x, int n) {
        if (n == 0)
            return 1.0;

        long N = n;
        if (N < 0) {
            x = 1 / x;
            N = -N;
        }

        return fastPow(x, N);
    }

    private double fastPow(double x, long n) {
        if (n == 0)
            return 1.0;

        double half = fastPow(x, n / 2);

        if (n % 2 == 0) {
            return half * half;
        } else {
            return half * half * x;
        }
    }

    // Solution 3: Bit Manipulation Approach
    // Time: O(log n), Space: O(1)
    public double myPow3(double x, int n) {
        if (n == 0)
            return 1.0;

        long N = n;
        if (N < 0) {
            x = 1 / x;
            N = -N;
        }

        double result = 1.0;

        while (N > 0) {
            if ((N & 1) == 1) { // Check if least significant bit is 1
                result *= x;
            }
            x *= x;
            N >>= 1; // Right shift by 1 (equivalent to N /= 2)
        }

        return result;
    }

    // Solution 4: Naive Approach (for comparison) - NOT RECOMMENDED
    // Time: O(n), Space: O(1)
    public double myPow4(double x, int n) {
        if (n == 0)
            return 1.0;

        long N = n;
        if (N < 0) {
            x = 1 / x;
            N = -N;
        }

        double result = 1.0;
        for (long i = 0; i < N; i++) {
            result *= x;
        }

        return result;
    }

    // Solution 5: Using Mathematical Properties
    // Time: O(log n), Space: O(1)
    public double myPow5(double x, int n) {
        if (n == 0)
            return 1.0;
        if (x == 0)
            return 0.0;
        if (x == 1)
            return 1.0;
        if (x == -1)
            return (n % 2 == 0) ? 1.0 : -1.0;

        long N = n;
        if (N < 0) {
            x = 1 / x;
            N = -N;
        }

        return fastPowOptimized(x, N);
    }

    private double fastPowOptimized(double x, long n) {
        if (n == 0)
            return 1.0;
        if (n == 1)
            return x;

        if (n % 2 == 0) {
            double half = fastPowOptimized(x, n / 2);
            return half * half;
        } else {
            return x * fastPowOptimized(x, n - 1);
        }
    }

    // Solution 6: Matrix Exponentiation (for educational purposes)
    // Demonstrates the concept, though not directly applicable to this problem
    public double myPow6(double x, int n) {
        if (n == 0)
            return 1.0;

        // Convert to matrix form for demonstration
        // This is more complex than needed for this problem
        return matrixPower(x, n);
    }

    private double matrixPower(double base, int exp) {
        if (exp == 0)
            return 1.0;

        long N = exp;
        if (N < 0) {
            base = 1 / base;
            N = -N;
        }

        double result = 1.0;
        while (N > 0) {
            if ((N & 1) == 1) {
                result *= base;
            }
            base *= base;
            N >>= 1;
        }

        return result;
    }

    // Helper method to demonstrate the algorithm step by step
    public void demonstrateAlgorithm(double x, int n) {
        System.out.println("\nDemonstrating Fast Exponentiation for x=" + x + ", n=" + n);
        System.out.println("=".repeat(50));

        if (n == 0) {
            System.out.println("Base case: x^0 = 1");
            return;
        }

        long N = n;
        double originalX = x;

        if (N < 0) {
            System.out.println("Negative exponent: converting to positive");
            System.out.println("x = 1/x = 1/" + x + " = " + (1 / x));
            x = 1 / x;
            N = -N;
        }

        System.out.println("Computing " + originalX + "^" + n + " = " + x + "^" + N);
        System.out.println("\nStep-by-step binary exponentiation:");

        double result = 1.0;
        double currentPower = x;
        long step = 1;

        while (N > 0) {
            System.out.printf("Step %d: N=%d, currentPower=%.5f, result=%.5f\n",
                    step, N, currentPower, result);

            if (N % 2 == 1) {
                result *= currentPower;
                System.out.printf("  N is odd: result *= currentPower = %.5f\n", result);
            } else {
                System.out.println("  N is even: skip multiplication");
            }

            currentPower *= currentPower;
            N /= 2;
            System.out.printf("  Square currentPower: %.5f, N = N/2 = %d\n", currentPower, N);
            step++;
        }

        System.out.println("Final result: " + result);
    }

    // Performance comparison
    public void performanceComparison(double x, int n) {
        System.out.println("\nPerformance Comparison for x=" + x + ", n=" + n);
        System.out.println("=".repeat(50));

        long start, end;
        double result;

        // Test Fast Exponentiation (Iterative)
        start = System.nanoTime();
        result = myPow1(x, n);
        end = System.nanoTime();
        System.out.printf("Fast Exp (Iterative): %.6f, Time: %d ns\n", result, end - start);

        // Test Fast Exponentiation (Recursive)
        start = System.nanoTime();
        result = myPow2(x, n);
        end = System.nanoTime();
        System.out.printf("Fast Exp (Recursive): %.6f, Time: %d ns\n", result, end - start);

        // Test Bit Manipulation
        start = System.nanoTime();
        result = myPow3(x, n);
        end = System.nanoTime();
        System.out.printf("Bit Manipulation: %.6f, Time: %d ns\n", result, end - start);

        // Test Naive approach only for small n
        if (Math.abs(n) <= 1000) {
            start = System.nanoTime();
            result = myPow4(x, n);
            end = System.nanoTime();
            System.out.printf("Naive Approach: %.6f, Time: %d ns\n", result, end - start);
        } else {
            System.out.println("Naive Approach: Skipped for large n");
        }

        // Built-in Math.pow for comparison
        start = System.nanoTime();
        result = Math.pow(x, n);
        end = System.nanoTime();
        System.out.printf("Built-in Math.pow: %.6f, Time: %d ns\n", result, end - start);
    }

    public static void main(String[] args) {
        PowerFunction solution = new PowerFunction();

        // Test cases
        double[][] testCases = {
                { 2.0, 10 },
                { 2.1, 3 },
                { 2.0, -2 },
                { 1.0, 2147483647 }, // Edge case: large positive n
                { 2.0, -2147483648 }, // Edge case: Integer.MIN_VALUE
                { 0.0, 0 }, // Edge case: 0^0 (typically returns 1)
                { -2.0, 3 }, // Negative base, odd exponent
                { -2.0, 4 }, // Negative base, even exponent
                { 0.5, 10 }, // Fractional base
                { 1.0, -1000000 } // Large negative exponent
        };

        System.out.println("Power Function Implementation Test Results");
        System.out.println("=========================================");

        for (double[] testCase : testCases) {
            double x = testCase[0];
            int n = (int) testCase[1];

            System.out.printf("\nTesting x=%.1f, n=%d:\n", x, n);

            double result1 = solution.myPow1(x, n);
            double result2 = solution.myPow2(x, n);
            double result3 = solution.myPow3(x, n);
            double builtIn = Math.pow(x, n);

            System.out.printf("Fast Exp (Iterative): %.6f\n", result1);
            System.out.printf("Fast Exp (Recursive): %.6f\n", result2);
            System.out.printf("Bit Manipulation: %.6f\n", result3);
            System.out.printf("Built-in Math.pow: %.6f\n", builtIn);

            // Check if results are approximately equal (handling floating point precision)
            boolean match = Math.abs(result1 - result2) < 1e-9 &&
                    Math.abs(result2 - result3) < 1e-9;
            System.out.println("All custom solutions match: " + match);
        }

        // Demonstrate algorithm
        solution.demonstrateAlgorithm(2.0, 10);

        // Performance comparison
        solution.performanceComparison(2.0, 1000);

        // Edge cases explanation
        System.out.println("\n" + "=".repeat(60));
        System.out.println("IMPORTANT EDGE CASES:");
        System.out.println("1. Integer.MIN_VALUE: Use long to avoid overflow when negating");
        System.out.println("2. x = 0: Handle 0^0 case (typically returns 1)");
        System.out.println("3. x = 1: Always returns 1 regardless of n");
        System.out.println("4. x = -1: Returns 1 for even n, -1 for odd n");
        System.out.println("5. Negative exponents: Convert to 1/x and make exponent positive");
    }
}

/*
 * Algorithm Analysis:
 * 
 * FAST EXPONENTIATION PRINCIPLE:
 * The key insight is that x^n can be computed efficiently using the binary
 * representation of n.
 * 
 * For example, 2^10:
 * - 10 in binary: 1010
 * - 2^10 = 2^8 * 2^2 = 2^(8+2)
 * - We compute: 2^1, 2^2, 2^4, 2^8 by repeatedly squaring
 * - Then multiply the powers corresponding to 1-bits in the binary
 * representation
 * 
 * TIME COMPLEXITY:
 * - Fast Exponentiation: O(log n)
 * - Naive Approach: O(n)
 * 
 * SPACE COMPLEXITY:
 * - Iterative Fast Exp: O(1)
 * - Recursive Fast Exp: O(log n) due to recursion stack
 * 
 * KEY OPTIMIZATIONS:
 * 1. Use long to handle Integer.MIN_VALUE edge case
 * 2. Convert negative exponents to positive by taking reciprocal of base
 * 3. Use bit manipulation or modulo operations to check odd/even
 * 4. Handle special cases (x=0, x=1, x=-1) for efficiency
 * 
 * RECOMMENDED SOLUTION: Solution 1 (myPow1)
 * - Optimal time complexity O(log n)
 * - Constant space complexity O(1)
 * - Clean, iterative implementation
 * - Handles all edge cases correctly
 * - Most commonly expected in interviews
 * 
 * The fast exponentiation algorithm is a classic example of the
 * "divide and conquer"
 * approach, reducing the problem size by half at each step.
 */
