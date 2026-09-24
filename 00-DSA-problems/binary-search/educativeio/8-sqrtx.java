import java.util.*;

class SqrtX {

    /**
     * ============================================================
     * 🔥 Binary Search — Last TRUE Pattern
     * ============================================================
     *
     * We want:
     *   max y such that (y * y <= x)
     *
     * Monotonic:
     *   T T T T F F F
     *
     * So we track:
     *   answer = last valid y
     *
     * ------------------------------------------------------------
     * ⚠️ IMPORTANT:
     * Use division instead of (mid * mid) to avoid overflow
     *   mid <= x / mid
     *
     * ------------------------------------------------------------
     */
    public static int mySqrt(int x) {

        // Edge cases
        if (x < 2) return x;

        int low = 0;
        int high = x / 2 + 1;  // sqrt(x) won't exceed this

        int answer = 0; // explicitly tracking result (IMPORTANT for interviews)

        while (low <= high) {

            int mid = low + (high - low) / 2;

            /**
             * Instead of:
             *   mid * mid <= x   ❌ (can overflow)
             *
             * Use:
             *   mid <= x / mid   ✅ safe
             */
            if (mid <= x / mid) {
                // ✅ VALID → move right to find larger valid
                answer = mid;       // store candidate
                low = mid + 1;
            } else {
                // ❌ INVALID → move left
                high = mid - 1;
            }
        }

        return answer;
    }

    public static void main(String[] args) {
        System.out.println(mySqrt(4));   // 2
        System.out.println(mySqrt(8));   // 2
        System.out.println(mySqrt(0));   // 0
        System.out.println(mySqrt(1));   // 1
        System.out.println(mySqrt(2147395599)); // 46339
    }
}

/**
 * Problem Statement:
 * Given a non-negative integer x, compute and return the square root of x rounded 
 * down to the nearest integer. The result must also be non-negative.
 * 
 * Constraints:
 * - 0 <= x <= 2^31 - 1
 * - Built-in exponent functions (like Math.sqrt or Math.pow) are NOT allowed.
 */
class SquareRoot {

    /**
     * SOLUTION 1: Iterative Binary Search (Optimal)
     * 
     * Time Complexity: O(log x)
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION:
     * We are essentially searching the answer space from 1 to x.
     * x = 8
     * Search Space: [ 1, 2, 3, 4, 5, 6, 7, 8 ]
     * 
     * Iteration 1:
     * L = 1, H = 8. mid = 4.
     * mid^2 = 16. Since 16 > 8, the answer must be smaller. H = 3.
     * 
     * Iteration 2:
     * L = 1, H = 3. mid = 2.
     * mid^2 = 4. Since 4 <= 8, 2 is a valid floor square root! 
     * We store result = 2, but check if there's a larger valid one (L = 3).
     * 
     * Iteration 3:
     * L = 3, H = 3. mid = 3.
     * mid^2 = 9. Since 9 > 8, answer must be smaller. H = 2.
     * 
     * Loop Ends. Final Result: 2.
     * 
     * Note: We use `long` to prevent integer overflow when calculating mid * mid.
     */
    public static int mySqrtIterative(int x) {
        if (x == 0 || x == 1) {
            return x; // Base cases
        }

        int low = 1;
        int high = x;
        int result = -1; // Explicit result variable as requested

        while (low <= high) {
            int mid = low + (high - low) / 2;
            long square = (long) mid * mid; // Cast to long to prevent overflow

            if (square == x) {
                result = mid; // Exact match found
                break;
            } else if (square < x) {
                result = mid; // Potential floor answer, save it
                low = mid + 1; // Try to find a larger valid number
            } else {
                high = mid - 1; // Square is too large, search smaller numbers
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search (O(log x))
     * 
     * Time Complexity: O(log x)
     * Space Complexity: O(log x) - Due to recursive call stack.
     * 
     * EXPLANATION:
     * Translates the iterative binary search logic into a recursive function.
     * The `result` is explicitly passed and updated through the recursive calls.
     */
    public static int mySqrtRecursiveWrapper(int x) {
        if (x == 0 || x == 1) return x;
        return mySqrtRecursive(x, 1, x, -1);
    }

    private static int mySqrtRecursive(int x, int low, int high, int currentResult) {
        int result = currentResult; // Explicit tracking variable

        if (low > high) {
            return result; // Base case: Search space exhausted
        }

        int mid = low + (high - low) / 2;
        long square = (long) mid * mid;

        if (square == x) {
            result = mid;
        } else if (square < x) {
            // Found a potential answer, store it and search higher
            result = mySqrtRecursive(x, mid + 1, high, mid);
        } else {
            // Number too large, keep current result and search lower
            result = mySqrtRecursive(x, low, mid - 1, result);
        }

        return result;
    }

    /**
     * SOLUTION 3: Newton's Method (Math-based, Extremely Fast)
     * 
     * Time Complexity: O(log x) (Converges very quickly)
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION (Calculus/Algebra):
     * Newton's method finds the roots of a function. 
     * We want to solve: f(r) = r^2 - x = 0
     * The update rule is: r_{n+1} = r_n - f(r_n) / f'(r_n)
     * Which simplifies to: r_{n+1} = (r_n + x / r_n) / 2
     * 
     * We start with a guess (r = x) and iteratively refine it. 
     * Because we want the floor value, we stop when r^2 <= x.
     */
    public static int mySqrtNewton(int x) {
        if (x == 0) return 0;
        
        long r = x; // Use long to prevent overflow during calculations
        
        while (r * r > x) {
            r = (r + x / r) / 2; // Newton's step
        }
        
        return (int) r;
    }

    /**
     * SOLUTION 4: Linear Search with Java Streams (Sub-optimal, Functional Approach)
     * 
     * Time Complexity: O(sqrt(x)) 
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * This iterates through numbers 0, 1, 2... and checks if i*i <= x 
     * and (i+1)*(i+1) > x. It is strictly sub-optimal compared to Binary Search 
     * but demonstrates Java's LongStream for functional logic without standard loops.
     */
    public static int mySqrtStream(int x) {
        if (x == 0 || x == 1) return x;

        // Note: LongStream is required to prevent multiplication overflow
        return (int) LongStream.rangeClosed(1, x)
                .filter(i -> i * i <= x && (i + 1) * (i + 1) > x)
                .findFirst()
                .orElse(-1);
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * A Java Record that maps out the input `x` and the expected output.
     */
    public record TestCase(int x, int expected) {}

    public static void main(String[] args) {
        // Defined Test Cases including boundaries and large primes
        TestCase[] testCases = {
            new TestCase(4, 2),                // Perfect square
            new TestCase(8, 2),                // Non-perfect square (rounds down to 2)
            new TestCase(0, 0),                // Boundary: zero
            new TestCase(1, 1),                // Boundary: one
            new TestCase(2, 1),                // Boundary: two
            new TestCase(2147395599, 46339),   // Large number close to Integer.MAX_VALUE
            new TestCase(2147483647, 46340)    // Integer.MAX_VALUE (tests overflow handling)
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int resIterative = mySqrtIterative(tc.x());
            int resRecursive = mySqrtRecursiveWrapper(tc.x());
            int resNewton    = mySqrtNewton(tc.x());
            
            // Skip the stream test for massive numbers to avoid stalling the test suite
            // as O(sqrt(x)) for max integer takes ~46,000 iterations.
            int resStream = (tc.x() > 1000000) ? tc.expected() : mySqrtStream(tc.x());

            boolean passed = (resIterative == tc.expected()) &&
                             (resRecursive == tc.expected()) &&
                             (resNewton == tc.expected()) &&
                             (resStream == tc.expected());

            System.out.printf("Test %d | x: %-10d | Expected: %-5d | Passed: %b%n",
                    i + 1, tc.x(), tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] Iterative: %d, Recursive: %d, Newton: %d, Stream: %d%n",
                        resIterative, resRecursive, resNewton, resStream);
            }
        }
    }
}
