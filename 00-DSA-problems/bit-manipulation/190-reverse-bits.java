/*
 * Reverse bits of a given 32 bits unsigned integer.
 * 
 * Note:
 * 
 * Note that in some languages, such as Java, there is no unsigned integer type.
 * In this case, both input and output will be given as a signed integer type.
 * They should not affect your implementation, as the integer's internal binary
 * representation is the same, whether it is signed or unsigned.
 * In Java, the compiler represents the signed integers using 2's complement
 * notation. Therefore, in Example 2 above, the input represents the signed
 * integer -3 and the output represents the signed integer -1073741825.
 * 
 * 
 * Example 1:
 * Input: n = 00000010100101000001111010011100
 * Output: 964176192 (00111001011110000010100101000000)
 * Explanation: The input binary string 00000010100101000001111010011100
 * represents the unsigned integer 43261596, so return 964176192 which its
 * binary representation is 00111001011110000010100101000000.
 * 
 * Example 2:
 * Input: n = 11111111111111111111111111111101
 * Output: 3221225471 (10111111111111111111111111111111)
 * Explanation: The input binary string 11111111111111111111111111111101
 * represents the unsigned integer 4294967293, so return 3221225471 which its
 * binary representation is 10111111111111111111111111111111.
 */

class ReverseBits {

    // Solution 1: Bit-by-bit reversal (Most intuitive)
    public int reverseBits1(int n) {
        int result = 0;
        for (int i = 0; i < 32; i++) {
            // Shift result left to make room for next bit
            result <<= 1;
            // Add the least significant bit of n to result
            result |= (n & 1);
            // Shift n right to process next bit
            n >>= 1;
        }
        return result;
    }

    // Solution 2: Alternative bit-by-bit approach
    public int reverseBits2(int n) {
        int result = 0;
        for (int i = 0; i < 32; i++) {
            // Extract bit at position i from right
            int bit = (n >> i) & 1;
            // Place it at position (31-i) from right
            result |= (bit << (31 - i));
        }
        return result;
    }

    // Solution 3: Divide and conquer (Most efficient)
    public int reverseBits3(int n) {
        // Swap 16-bit halves
        n = (n >>> 16) | (n << 16);
        // Swap 8-bit quarters
        n = ((n & 0xff00ff00) >>> 8) | ((n & 0x00ff00ff) << 8);
        // Swap 4-bit groups
        n = ((n & 0xf0f0f0f0) >>> 4) | ((n & 0x0f0f0f0f) << 4);
        // Swap 2-bit pairs
        n = ((n & 0xcccccccc) >>> 2) | ((n & 0x33333333) << 2);
        // Swap individual bits
        n = ((n & 0xaaaaaaaa) >>> 1) | ((n & 0x55555555) << 1);
        return n;
    }

    // Solution 4: Using StringBuilder (Less efficient but readable)
    public int reverseBits4(int n) {
        StringBuilder sb = new StringBuilder();

        // Convert to 32-bit binary string
        for (int i = 0; i < 32; i++) {
            sb.append((n & 1));
            n >>= 1;
        }

        // Convert back to integer
        return Integer.parseUnsignedInt(sb.toString(), 2);
    }

    // Solution 5: Lookup table approach (for multiple calls)
    private int[] lookupTable = new int[256];
    private boolean tableInitialized = false;

    public int reverseBits5(int n) {
        if (!tableInitialized) {
            initializeLookupTable();
            tableInitialized = true;
        }

        int result = 0;
        for (int i = 0; i < 4; i++) {
            result <<= 8;
            result |= lookupTable[n & 0xff];
            n >>= 8;
        }
        return result;
    }

    private void initializeLookupTable() {
        for (int i = 0; i < 256; i++) {
            int reversed = 0;
            int num = i;
            for (int j = 0; j < 8; j++) {
                reversed <<= 1;
                reversed |= (num & 1);
                num >>= 1;
            }
            lookupTable[i] = reversed;
        }
    }

    // Helper method to print binary representation
    public static String toBinaryString(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 31; i >= 0; i--) {
            sb.append((n >> i) & 1);
        }
        return sb.toString();
    }

    // Test the solutions
    public static void main(String[] args) {
        ReverseBits solution = new ReverseBits();

        // Test cases
        int[] testCases = {
                0b00000010100101000001111010011100, // 43261596
                0b11111111111111111111111111111101, // -3 (4294967293 unsigned)
                0b00000000000000000000000000000001, // 1
                0b10000000000000000000000000000000, // -2147483648
                0b00000000000000000000000000000000 // 0
        };

        System.out.println("Testing all solutions:");
        for (int test : testCases) {
            System.out.printf("Input:  %s (%d)\n", toBinaryString(test), test);

            int result1 = solution.reverseBits1(test);
            int result2 = solution.reverseBits2(test);
            int result3 = solution.reverseBits3(test);
            int result4 = solution.reverseBits4(test);
            int result5 = solution.reverseBits5(test);

            System.out.printf("Output: %s (%d)\n", toBinaryString(result1), result1);

            boolean allMatch = (result1 == result2) && (result2 == result3) &&
                    (result3 == result4) && (result4 == result5);
            System.out.printf("All solutions match: %s\n\n", allMatch ? "✓" : "✗");
        }

        // Performance comparison for large number of operations
        System.out.println("Performance test (1 million operations):");
        int testValue = 0b00000010100101000001111010011100;
        int iterations = 1000000;

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.reverseBits1(testValue);
        }
        long time1 = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.reverseBits3(testValue);
        }
        long time3 = System.nanoTime() - start;

        System.out.printf("Solution 1 (bit-by-bit): %.2f ms\n", time1 / 1_000_000.0);
        System.out.printf("Solution 3 (divide & conquer): %.2f ms\n", time3 / 1_000_000.0);
        System.out.printf("Speedup: %.2fx\n", (double) time1 / time3);
    }

}

// This function efficiently reverses the bits of a 32-bit integer using a
// divide and conquer approach.
// Instead of reversing each bit individually, it swaps groups of bits (16-bit
// halves, 8-bit chunks, etc.)
// in multiple stages using bitwise operations. This makes it extremely fast and
// constant time: O(1).
//
// ------------------------------------------------------------
// 🔧 Bitwise Operators Used (Clean Explanation)
// ------------------------------------------------------------
//
// 1. >>> Unsigned Right Shift
// - Shifts all bits to the right by a given number of positions.
// - Leftmost bits are filled with 0, regardless of the sign.
// - Used to move higher-order bits to lower positions.
// - Example: 10000000 >>> 1 = 01000000
//
// 2. << Left Shift
// - Shifts all bits to the left by a given number of positions.
// - Rightmost bits are filled with 0.
// - Used to move lower-order bits to higher positions.
// - Example: 00000001 << 1 = 00000010
//
// 3. & Bitwise AND
// - Compares corresponding bits of two numbers.
// - The result bit is 1 if both bits are 1, else 0.
// - Often used with masks to filter out (zero out) unwanted bits.
// - Example: 1100 & 1010 = 1000
//
// 4. | Bitwise OR
// - Compares corresponding bits of two numbers.
// - The result bit is 1 if either bit is 1.
// - Used to merge/combine results after shifting.
// - Example: 1100 | 1010 = 1110
//
// ------------------------------------------------------------
// 🧠 Step-by-Step Bit Reversal Process
// ------------------------------------------------------------
//
// Step 1: Swap 16-bit halves
// - Use right shift (>>>) and left shift (<<) to move the left 16 bits to the
// right
// and right 16 bits to the left.
// - Combine using OR (|) to get swapped halves.
//
// Step 2: Swap 8-bit quarters (each byte) within the halves
// - Use mask 0xff00ff00 to isolate higher bytes, then shift them right by 8
// bits.
// - Use mask 0x00ff00ff to isolate lower bytes, then shift them left by 8 bits.
// - OR the two results to swap the positions of each 8-bit chunk.
//
// Step 3: Swap 4-bit groups (nibbles) within each byte
// - Use mask 0xf0f0f0f0 to extract the upper 4 bits of each byte.
// - Use mask 0x0f0f0f0f to extract the lower 4 bits of each byte.
// - Shift and recombine to swap the nibbles in place.
//
// Step 4: Swap 2-bit pairs
// - Use mask 0xcccccccc (binary: 11001100...) to get upper 2 bits in each
// nibble.
// - Use mask 0x33333333 (binary: 00110011...) to get lower 2 bits.
// - Shift and recombine to swap 2-bit pairs.
//
// Step 5: Swap individual bits
// - Use mask 0xaaaaaaaa (binary: 10101010...) to extract bits at even
// positions.
// - Use mask 0x55555555 (binary: 01010101...) to extract bits at odd positions.
// - Shift and recombine to swap each bit with its neighbor.
//
// ------------------------------------------------------------
// ✅ Final Result
// ------------------------------------------------------------
// - After these 5 stages, the 32-bit integer is fully bit-reversed.
// - This method uses no loops and only a fixed number of operations,
// so it is highly optimized and runs in constant time (O(1)).
// - Commonly used in low-level applications, graphics, bitboards, and network
// protocols.
