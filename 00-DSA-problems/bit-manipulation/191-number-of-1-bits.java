/*
 * Given a positive integer n, write a function that returns the number of set
 * bits in its binary representation (also known as the Hamming weight).
 * 
 * 
 * Example 1:
 * Input: n = 11
 * Output: 3
 * Explanation:
 * The input binary string 1011 has a total of three set bits.
 * 
 * Example 2:
 * Input: n = 128
 * Output: 1
 * Explanation:
 * The input binary string 10000000 has a total of one set bit.
 * 
 * Example 3:
 * Input: n = 2147483645
 * Output: 30
 * Explanation:
 * The input binary string 1111111111111111111111111111101 has a total of thirty
 * set bits.
 */

class HammingWeight {

    // Solution 1: Check each bit individually (Brute force)
    // Time Complexity: O(32) = O(1)
    // Space Complexity: O(1)
    public int hammingWeight1(int n) {
        int count = 0;
        for (int i = 0; i < 32; i++) {
            if ((n & (1 << i)) != 0) {
                count++;
            }
        }
        return count;
    }

    // Solution 2: Right shift and check LSB (More intuitive)
    // Time Complexity: O(log n) - depends on number of bits
    // Space Complexity: O(1)
    public int hammingWeight2(int n) {
        int count = 0;
        while (n != 0) {
            count += (n & 1); // Add 1 if LSB is set, 0 otherwise
            n >>>= 1; // Unsigned right shift
        }
        return count;
    }

    // Solution 3: Brian Kernighan's Algorithm (Most efficient)
    // Time Complexity: O(k) where k = number of set bits
    // Space Complexity: O(1)
    // Key insight: n & (n-1) removes the rightmost set bit
    // Most efficient for sparse bit patterns (few set bits)
    public int hammingWeight3(int n) {
        int count = 0;
        while (n != 0) {
            n &= (n - 1); // This operation removes the rightmost set bit
            count++;
        }
        return count;

        // n & (n-1) always removes the rightmost set bit
        // Example with n=12 (1100):
        // 1100 & 1011 = 1000 (removed rightmost 1)
        // 1000 & 0111 = 0000 (removed last 1)
        // Count = 2
    }

    // Solution 4: Using built-in function
    public int hammingWeight4(int n) {
        return Integer.bitCount(n);
    }

    // Solution 5: Lookup table approach (for multiple calls)
    private int[] lookupTable = new int[256];
    private boolean tableInitialized = false;

    public int hammingWeight5(int n) {
        if (!tableInitialized) {
            initializeLookupTable();
            tableInitialized = true;
        }

        int count = 0;
        while (n != 0) {
            count += lookupTable[n & 0xFF]; // Count bits in last 8 bits
            n >>>= 8; // Process next 8 bits
        }
        return count;
    }

    private void initializeLookupTable() {
        for (int i = 0; i < 256; i++) {
            int count = 0;
            int num = i;
            while (num != 0) {
                count += (num & 1);
                num >>= 1;
            }
            lookupTable[i] = count;
        }
        tableInitialized = true;
    }

    // Solution 6: Parallel bit counting (Advanced)
    public int hammingWeight6(int n) {
        // Count bits in pairs
        n = n - ((n >>> 1) & 0x55555555);
        // Count bits in groups of 4
        n = (n & 0x33333333) + ((n >>> 2) & 0x33333333);
        // Count bits in groups of 8
        n = (n + (n >>> 4)) & 0x0f0f0f0f;
        // Count bits in groups of 16 and 32
        n = n + (n >>> 8);
        n = n + (n >>> 16);
        return n & 0x3f; // Return the count (max 32, so 6 bits needed)
    }

    // Helper method to display binary representation
    public static String toBinaryString(int n) {
        return String.format("%32s", Integer.toBinaryString(n)).replace(' ', '0');
    }

    // Helper method to demonstrate Brian Kernighan's algorithm step by step
    public static void demonstrateBrianKernighan(int n) {
        System.out.printf("Demonstrating Brian Kernighan's algorithm for n = %d:\n", n);
        System.out.printf("Binary: %s\n", toBinaryString(n));

        int count = 0;
        int original = n;
        while (n != 0) {
            System.out.printf("Step %d: n = %s\n", count + 1, toBinaryString(n));
            System.out.printf("       n-1 = %s\n", toBinaryString(n - 1));
            n &= (n - 1);
            System.out.printf("   n&(n-1) = %s\n", toBinaryString(n));
            count++;
            System.out.println();
        }
        System.out.printf("Total set bits in %d: %d\n\n", original, count);
    }

    // Test all solutions
    public static void main(String[] args) {
        HammingWeight solution = new HammingWeight();

        // Test cases
        int[] testCases = { 11, 128, 2147483645, 0, 1, 2147483647, -1, -2 };

        System.out.println("Testing all solutions:");
        System.out.println("=".repeat(80));

        for (int test : testCases) {
            System.out.printf("Input: %d\n", test);
            System.out.printf("Binary: %s\n", toBinaryString(test));

            int result1 = solution.hammingWeight1(test);
            int result2 = solution.hammingWeight2(test);
            int result3 = solution.hammingWeight3(test);
            int result4 = solution.hammingWeight4(test);
            int result5 = solution.hammingWeight5(test);
            int result6 = solution.hammingWeight6(test);

            System.out.printf("Results: %d %d %d %d %d %d\n",
                    result1, result2, result3, result4, result5, result6);

            boolean allMatch = (result1 == result2) && (result2 == result3) &&
                    (result3 == result4) && (result4 == result5) && (result5 == result6);
            System.out.printf("All solutions match: %s\n", allMatch ? "✓" : "✗");
            System.out.println("-".repeat(40));
        }

        // Demonstrate Brian Kernighan's algorithm
        demonstrateBrianKernighan(11);
        demonstrateBrianKernighan(12);

        // Performance comparison
        System.out.println("Performance test (10 million operations):");
        int testValue = 2147483645;
        int iterations = 10_000_000;

        // Test Solution 2 (Right shift)
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.hammingWeight2(testValue);
        }
        long time2 = System.nanoTime() - start;

        // Test Solution 3 (Brian Kernighan)
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.hammingWeight3(testValue);
        }
        long time3 = System.nanoTime() - start;

        // Test Solution 4 (Built-in)
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.hammingWeight4(testValue);
        }
        long time4 = System.nanoTime() - start;

        // Test Solution 6 (Parallel)
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.hammingWeight6(testValue);
        }
        long time6 = System.nanoTime() - start;

        System.out.printf("Right shift approach: %.2f ms\n", time2 / 1_000_000.0);
        System.out.printf("Brian Kernighan: %.2f ms\n", time3 / 1_000_000.0);
        System.out.printf("Built-in bitCount: %.2f ms\n", time4 / 1_000_000.0);
        System.out.printf("Parallel counting: %.2f ms\n", time6 / 1_000_000.0);

        System.out.printf("Brian Kernighan vs Right shift: %.2fx faster\n",
                (double) time2 / time3);
    }

}
