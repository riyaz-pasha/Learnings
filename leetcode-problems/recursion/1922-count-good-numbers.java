class GoodDigitStringCounter {

    private static final int MOD = 1_000_000_007;

    // Approach 1: Mathematical Solution - OPTIMAL
    // Time: O(log n), Space: O(1)
    public int countGoodNumbers(long n) {
        // Even indices (0, 2, 4, ...): can be 0, 2, 4, 6, 8 → 5 choices
        // Odd indices (1, 3, 5, ...): can be 2, 3, 5, 7 → 4 choices

        // Calculate number of even and odd positions
        long evenPositions = (n + 1) / 2; // Ceiling division
        long oddPositions = n / 2; // Floor division

        // Total combinations = 5^evenPositions * 4^oddPositions
        long evenChoices = powerMod(5, evenPositions, MOD);
        long oddChoices = powerMod(4, oddPositions, MOD);

        return (int) ((evenChoices * oddChoices) % MOD);
    }

    // Fast modular exponentiation using binary exponentiation
    // Calculates (base^exp) % mod efficiently in O(log exp)
    private long powerMod(long base, long exp, long mod) {
        long result = 1;
        base = base % mod;

        while (exp > 0) {
            // If exp is odd, multiply base with result
            if ((exp & 1) == 1) {
                result = (result * base) % mod;
            }

            // exp must be even now
            exp = exp >> 1; // Divide exp by 2
            base = (base * base) % mod; // Square the base
        }

        return result;
    }

    // Approach 2: Iterative Power (Alternative implementation)
    // Time: O(n), Space: O(1)
    public int countGoodNumbersIterative(long n) {
        long evenPositions = (n + 1) / 2;
        long oddPositions = n / 2;

        long result = 1;

        // Calculate 5^evenPositions % MOD
        for (long i = 0; i < evenPositions; i++) {
            result = (result * 5) % MOD;
        }

        // Calculate 4^oddPositions % MOD
        for (long i = 0; i < oddPositions; i++) {
            result = (result * 4) % MOD;
        }

        return (int) result;
    }

    // Approach 3: Dynamic Programming (For understanding)
    // Time: O(n), Space: O(1) optimized
    public int countGoodNumbersDP(long n) {
        if (n == 0)
            return 0;

        long evenChoices = 5; // 0, 2, 4, 6, 8
        long oddChoices = 4; // 2, 3, 5, 7
        long result = 1;

        for (long i = 0; i < n; i++) {
            if (i % 2 == 0) {
                // Even index
                result = (result * evenChoices) % MOD;
            } else {
                // Odd index
                result = (result * oddChoices) % MOD;
            }
        }

        return (int) result;
    }
}

// Test and explanation class
class GoodDigitStringTester {

    public static void main(String[] args) {
        GoodDigitStringCounter counter = new GoodDigitStringCounter();

        System.out.println("=== Count Good Digit Strings ===\n");

        // Example 1: n = 1
        System.out.println("Example 1:");
        int n1 = 1;
        System.out.println("Input: n = " + n1);
        int result1 = counter.countGoodNumbers(n1);
        System.out.println("Output: " + result1);
        System.out.println("Explanation: Good numbers are \"0\", \"2\", \"4\", \"6\", \"8\"");
        System.out.println("  - All are at even index 0, so they must be even digits\n");

        // Example 2: n = 4
        System.out.println("Example 2:");
        int n2 = 4;
        System.out.println("Input: n = " + n2);
        int result2 = counter.countGoodNumbers(n2);
        System.out.println("Output: " + result2);
        System.out.println("Explanation:");
        System.out.println("  - Index 0 (even): 5 choices (0,2,4,6,8)");
        System.out.println("  - Index 1 (odd):  4 choices (2,3,5,7)");
        System.out.println("  - Index 2 (even): 5 choices (0,2,4,6,8)");
        System.out.println("  - Index 3 (odd):  4 choices (2,3,5,7)");
        System.out.println("  - Total: 5 × 4 × 5 × 4 = 400\n");

        // Example 3: n = 50
        System.out.println("Example 3:");
        long n3 = 50;
        System.out.println("Input: n = " + n3);
        int result3 = counter.countGoodNumbers(n3);
        System.out.println("Output: " + result3);
        System.out.println("Explanation:");
        System.out.println("  - Even positions (0,2,4,...,48): 25 positions → 5^25 choices");
        System.out.println("  - Odd positions (1,3,5,...,49): 25 positions → 4^25 choices");
        System.out.println("  - Total: 5^25 × 4^25 = (5×4)^25 = 20^25 mod (10^9+7)\n");

        System.out.println("=== Additional Test Cases ===\n");

        // Test case 4: n = 2
        System.out.println("Test 4: n = 2");
        int result4 = counter.countGoodNumbers(2);
        System.out.println("Output: " + result4);
        System.out.println("Expected: 5 × 4 = 20");
        System.out.println("Examples: \"02\", \"23\", \"45\", \"67\", \"82\", etc.\n");

        // Test case 5: n = 3
        System.out.println("Test 5: n = 3");
        int result5 = counter.countGoodNumbers(3);
        System.out.println("Output: " + result5);
        System.out.println("Expected: 5 × 4 × 5 = 100");
        System.out.println("  - Position 0 (even): 5 choices");
        System.out.println("  - Position 1 (odd):  4 choices");
        System.out.println("  - Position 2 (even): 5 choices\n");

        // Test case 6: Large n
        System.out.println("Test 6: n = 100");
        long n6 = 100;
        int result6 = counter.countGoodNumbers(n6);
        System.out.println("Output: " + result6);
        System.out.println("  - Even positions: 50 → 5^50 mod (10^9+7)");
        System.out.println("  - Odd positions: 50 → 4^50 mod (10^9+7)\n");

        System.out.println("=== Algorithm Explanation ===\n");

        System.out.println("Problem Analysis:");
        System.out.println("-----------------");
        System.out.println("A digit string of length n has indices: 0, 1, 2, 3, ..., n-1");
        System.out.println();
        System.out.println("Rules:");
        System.out.println("  • Even indices (0, 2, 4, ...): must be EVEN digits");
        System.out.println("    Valid: 0, 2, 4, 6, 8 → 5 choices");
        System.out.println();
        System.out.println("  • Odd indices (1, 3, 5, ...): must be PRIME digits");
        System.out.println("    Valid: 2, 3, 5, 7 → 4 choices");
        System.out.println();

        System.out.println("Mathematical Solution:");
        System.out.println("---------------------");
        System.out.println("For a string of length n:");
        System.out.println();
        System.out.println("Number of even positions = ⌈n/2⌉ = (n+1)/2");
        System.out.println("Number of odd positions  = ⌊n/2⌋ = n/2");
        System.out.println();
        System.out.println("Total combinations = 5^(even_positions) × 4^(odd_positions)");
        System.out.println();

        System.out.println("Examples:");
        System.out.println("---------");
        System.out.println("n=1: positions [0]");
        System.out.println("  Even: 1, Odd: 0 → 5^1 × 4^0 = 5");
        System.out.println();
        System.out.println("n=2: positions [0, 1]");
        System.out.println("  Even: 1, Odd: 1 → 5^1 × 4^1 = 20");
        System.out.println();
        System.out.println("n=3: positions [0, 1, 2]");
        System.out.println("  Even: 2, Odd: 1 → 5^2 × 4^1 = 100");
        System.out.println();
        System.out.println("n=4: positions [0, 1, 2, 3]");
        System.out.println("  Even: 2, Odd: 2 → 5^2 × 4^2 = 400");
        System.out.println();

        System.out.println("Modular Exponentiation:");
        System.out.println("-----------------------");
        System.out.println("For large n, we use binary exponentiation to compute:");
        System.out.println("  (base^exp) % MOD efficiently in O(log exp) time");
        System.out.println();
        System.out.println("Algorithm (Fast Power):");
        System.out.println("  result = 1");
        System.out.println("  while exp > 0:");
        System.out.println("    if exp is odd:");
        System.out.println("      result = (result × base) % MOD");
        System.out.println("    base = (base × base) % MOD");
        System.out.println("    exp = exp / 2");
        System.out.println();

        System.out.println("Example: Calculate 5^13 % MOD");
        System.out.println("  13 in binary = 1101");
        System.out.println("  5^13 = 5^8 × 5^4 × 5^1");
        System.out.println("  Only 3 multiplications instead of 12!");
        System.out.println();

        System.out.println("Complexity Analysis:");
        System.out.println("-------------------");
        System.out.println("Optimal Solution (Binary Exponentiation):");
        System.out.println("  Time:  O(log n) - for computing powers");
        System.out.println("  Space: O(1) - only variables");
        System.out.println();
        System.out.println("Naive Iterative:");
        System.out.println("  Time:  O(n) - multiply n times");
        System.out.println("  Space: O(1)");
        System.out.println();

        System.out.println("Key Insights:");
        System.out.println("-------------");
        System.out.println("✓ Independence: Each position's choice is independent");
        System.out.println("✓ Multiplication principle: Multiply choices for each position");
        System.out.println("✓ Pattern recognition: Even/odd positions alternate");
        System.out.println("✓ Modular arithmetic: Keep results within bounds");
        System.out.println("✓ Fast exponentiation: Handle large powers efficiently");

        System.out.println("\n=== Performance Comparison ===\n");

        long startTime, endTime;

        // Test with n = 1000000
        long largeN = 1000000;

        startTime = System.nanoTime();
        counter.countGoodNumbers(largeN);
        endTime = System.nanoTime();
        System.out.println("Binary Exponentiation (n=" + largeN + "): " +
                (endTime - startTime) / 1000 + " microseconds");
    }
}
