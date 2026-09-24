class PowerOfTwo {
    
    // Approach 1: Bit Manipulation - Single Bit Check (Optimal)
    // Time: O(1), Space: O(1)
    public boolean isPowerOfTwo1(int n) {
        // Power of 2 has exactly one bit set in binary
        // n & (n-1) clears the rightmost set bit
        // If n is power of 2, result should be 0
        return n > 0 && (n & (n - 1)) == 0;
    }
    
    // Approach 2: Count Set Bits
    // Time: O(1), Space: O(1)
    public boolean isPowerOfTwo2(int n) {
        // Power of 2 has exactly one bit set
        return n > 0 && Integer.bitCount(n) == 1;
    }
    
    // Approach 3: Iterative Division
    // Time: O(log n), Space: O(1)
    public boolean isPowerOfTwo3(int n) {
        if (n <= 0) {
            return false;
        }
        
        while (n % 2 == 0) {
            n /= 2;
        }
        
        return n == 1;
    }
    
    // Approach 4: Recursive Division
    // Time: O(log n), Space: O(log n)
    public boolean isPowerOfTwo4(int n) {
        if (n <= 0) {
            return false;
        }
        if (n == 1) {
            return true;
        }
        if (n % 2 != 0) {
            return false;
        }
        return isPowerOfTwo4(n / 2);
    }
    
    // Approach 5: Mathematical - Logarithm
    // Time: O(1), Space: O(1)
    public boolean isPowerOfTwo5(int n) {
        if (n <= 0) {
            return false;
        }
        
        // If n is power of 2, log2(n) should be an integer
        double log2 = Math.log(n) / Math.log(2);
        return log2 == Math.floor(log2);
    }
    
    // Approach 6: Check Against Known Powers
    // Time: O(1), Space: O(1)
    public boolean isPowerOfTwo6(int n) {
        // Largest power of 2 in int range: 2^30 = 1073741824
        // If n is power of 2, it must divide 2^30 evenly
        return n > 0 && (1073741824 % n == 0);
    }
    
    // Approach 7: Turn Off Rightmost Bit
    // Time: O(1), Space: O(1)
    public boolean isPowerOfTwo7(int n) {
        if (n <= 0) {
            return false;
        }
        
        // Turn off the rightmost set bit
        // If power of 2, should become 0
        int rightmostBitCleared = n & (n - 1);
        return rightmostBitCleared == 0;
    }
    
    // Approach 8: Check if Only One Bit is Set
    // Time: O(1), Space: O(1)
    public boolean isPowerOfTwo8(int n) {
        if (n <= 0) {
            return false;
        }
        
        // Get rightmost set bit
        int rightmostBit = n & (-n);
        // If n is power of 2, rightmost bit equals n itself
        return rightmostBit == n;
    }
    
    // Test cases with detailed visualization
    public static void main(String[] args) {
        PowerOfTwo solution = new PowerOfTwo();
        
        // Test Case 1
        int n1 = 1;
        boolean result1 = solution.isPowerOfTwo1(n1);
        System.out.println("Test 1: n = " + n1);
        System.out.println("Output: " + result1); // true
        visualizePowerOfTwo(n1, result1);
        
        // Test Case 2
        int n2 = 16;
        boolean result2 = solution.isPowerOfTwo1(n2);
        System.out.println("\nTest 2: n = " + n2);
        System.out.println("Output: " + result2); // true
        visualizePowerOfTwo(n2, result2);
        
        // Test Case 3
        int n3 = 3;
        boolean result3 = solution.isPowerOfTwo1(n3);
        System.out.println("\nTest 3: n = " + n3);
        System.out.println("Output: " + result3); // false
        visualizePowerOfTwo(n3, result3);
        
        // Test Case 4: Edge cases
        int[] testCases = {0, -1, 2, 4, 8, 15, 32, 64, 100, 1024};
        System.out.println("\nTesting multiple values:");
        for (int n : testCases) {
            boolean result = solution.isPowerOfTwo1(n);
            System.out.printf("n=%-5d → %5s   Binary: %s%n", 
                            n, result, toBinaryString(n));
        }
        
        // Compare all approaches
        System.out.println("\nComparing all approaches for n=16:");
        System.out.println("Approach 1 (Bit trick):     " + solution.isPowerOfTwo1(n2));
        System.out.println("Approach 2 (Count bits):    " + solution.isPowerOfTwo2(n2));
        System.out.println("Approach 3 (Division iter): " + solution.isPowerOfTwo3(n2));
        System.out.println("Approach 4 (Division rec):  " + solution.isPowerOfTwo4(n2));
        System.out.println("Approach 5 (Logarithm):     " + solution.isPowerOfTwo5(n2));
        System.out.println("Approach 6 (Modulo):        " + solution.isPowerOfTwo6(n2));
        System.out.println("Approach 7 (Clear bit):     " + solution.isPowerOfTwo7(n2));
        System.out.println("Approach 8 (Rightmost bit): " + solution.isPowerOfTwo8(n2));
        
        // Show powers of 2
        System.out.println("\nPowers of 2 up to 2^10:");
        for (int i = 0; i <= 10; i++) {
            int power = (int) Math.pow(2, i);
            System.out.printf("2^%-2d = %-5d   Binary: %s%n", 
                            i, power, toBinaryString(power));
        }
    }
    
    private static void visualizePowerOfTwo(int n, boolean isPowerOfTwo) {
        System.out.println("Decimal: " + n);
        System.out.println("Binary:  " + toBinaryString(n));
        
        if (n > 0) {
            int bitCount = Integer.bitCount(n);
            System.out.println("Number of set bits: " + bitCount);
            
            if (isPowerOfTwo) {
                int power = (int) (Math.log(n) / Math.log(2));
                System.out.println("This is 2^" + power + " = " + n);
                System.out.println("✓ Exactly ONE bit set → Power of 2");
            } else {
                System.out.println("✗ Multiple bits set → NOT a power of 2");
            }
            
            // Show n & (n-1) trick
            System.out.println("\nBit manipulation check:");
            System.out.println("n     = " + toBinaryString(n) + " (" + n + ")");
            System.out.println("n-1   = " + toBinaryString(n - 1) + " (" + (n - 1) + ")");
            System.out.println("n&(n-1) = " + toBinaryString(n & (n - 1)) + " (" + (n & (n - 1)) + ")");
            if ((n & (n - 1)) == 0) {
                System.out.println("Result is 0 → Power of 2 ✓");
            } else {
                System.out.println("Result is not 0 → NOT power of 2 ✗");
            }
        } else {
            System.out.println("n ≤ 0 → NOT a power of 2");
        }
    }
    
    private static String toBinaryString(int n) {
        if (n < 0) {
            return Integer.toBinaryString(n);
        }
        String binary = Integer.toBinaryString(n);
        // Pad to 8 bits for readability
        return String.format("%8s", binary).replace(' ', '0');
    }
}

/*
DETAILED EXPLANATION:

WHAT IS A POWER OF TWO?
Numbers that can be expressed as 2^x for some integer x ≥ 0:
1 = 2^0
2 = 2^1
4 = 2^2
8 = 2^3
16 = 2^4
...

BINARY REPRESENTATION INSIGHT:
Powers of 2 have EXACTLY ONE bit set in binary:
1   = 0b00000001 (one bit set)
2   = 0b00000010 (one bit set)
4   = 0b00000100 (one bit set)
8   = 0b00001000 (one bit set)
16  = 0b00010000 (one bit set)

Non-powers have multiple bits set:
3   = 0b00000011 (two bits)
5   = 0b00000101 (two bits)
6   = 0b00000110 (two bits)
15  = 0b00001111 (four bits)

KEY INSIGHT - BIT MANIPULATION TRICK:

For power of 2: n & (n-1) == 0

Why does this work?

Example: n = 8
n   = 8   = 0b00001000
n-1 = 7   = 0b00000111
n & (n-1) = 0b00000000 = 0 ✓

Example: n = 16
n   = 16  = 0b00010000
n-1 = 15  = 0b00001111
n & (n-1) = 0b00000000 = 0 ✓

Example: n = 6 (not power of 2)
n   = 6   = 0b00000110
n-1 = 5   = 0b00000101
n & (n-1) = 0b00000100 = 4 ≠ 0 ✗

EXPLANATION OF n & (n-1):
- Subtracting 1 from n flips all bits after the rightmost set bit
- For power of 2 (only one bit set), this flips that bit and all zeros after it
- ANDing them gives 0 since they have no common set bits

DETAILED WALKTHROUGH:

Power of 2 case (n = 8):
n   = 0b00001000  (bit at position 3)
n-1 = 0b00000111  (all bits after position 3 are flipped)
&   = 0b00000000  (no common bits)

Non-power case (n = 6):
n   = 0b00000110  (bits at positions 1 and 2)
n-1 = 0b00000101  (rightmost bit flipped)
&   = 0b00000100  (common bit at position 2)

APPROACH COMPARISON:

Approach 1 (n & (n-1)):
- Time: O(1) - single operation
- Space: O(1)
- Most elegant
- ✓✓✓ Best for interviews

Approach 2 (Count bits):
- Time: O(1) - built-in function
- Space: O(1)
- Clean and readable
- ✓✓ Good alternative

Approach 3 (Division):
- Time: O(log n) - divide until 1
- Space: O(1)
- More intuitive
- ✓ Acceptable but slower

Approach 5 (Logarithm):
- Time: O(1)
- Space: O(1)
- Watch for floating point precision issues
- ⚠️ Not recommended (precision issues)

EDGE CASES:

1. n = 0: NOT a power of 2
   - 2^x is always ≥ 1 for x ≥ 0
   
2. n = 1: IS a power of 2 (2^0 = 1)
   - Binary: 0b00000001 (one bit set)
   
3. Negative numbers: NOT powers of 2
   - Powers of 2 are always positive
   
4. n = 2: IS a power of 2 (2^1 = 2)

5. Very large: 2^30 = 1,073,741,824
   - Largest power of 2 in 32-bit signed int

COMPLEXITY ANALYSIS:

Approach 1 (Optimal):
Time: O(1) - constant time bit operation
Space: O(1) - no extra space

Approach 3 (Division):
Time: O(log n) - divide by 2 each iteration
Space: O(1)

MATHEMATICAL PROPERTIES:

1. All powers of 2 are even (except 1)
2. Powers of 2 in binary: exactly one 1 bit
3. log2(n) is integer iff n is power of 2
4. n & (n-1) removes rightmost set bit
5. For powers of 2: n & (-n) == n

PRACTICAL APPLICATIONS:

1. Memory allocation (typically in powers of 2)
2. Binary trees (levels, nodes)
3. Hash table sizing
4. Bit manipulation algorithms
5. Computer architecture (cache sizes)
6. Signal processing (FFT sizes)

INTERVIEW TIPS:

1. Mention the bit manipulation trick first
2. Explain WHY n & (n-1) works
3. Draw binary representation
4. Handle edge cases (0, 1, negative)
5. Discuss O(1) time complexity
6. Can mention alternative approaches
7. Show example with binary numbers

COMMON MISTAKES:

1. Forgetting to check n > 0
2. Using division (inefficient)
3. Using logarithm (precision issues)
4. Not handling n = 1 correctly
5. Confusing n & (n-1) with other operations

RELATED BIT TRICKS:

1. n & (n-1): Clear rightmost set bit
2. n & (-n): Isolate rightmost set bit
3. n | (n-1): Set all bits after rightmost
4. n ^ (n-1): Flip all bits after rightmost
5. ~n & (n+1): Get rightmost 0 bit

PROOF OF CORRECTNESS:

For n = 2^k (some k ≥ 0):
- Binary: 10...0 (1 followed by k zeros)
- n-1 = 01...1 (0 followed by k ones)
- n & (n-1) = 00...0 (all zeros)

For n ≠ 2^k:
- n has at least 2 set bits
- n-1 flips bits after rightmost set bit
- At least one higher bit remains set
- n & (n-1) ≠ 0

Therefore: n & (n-1) == 0 ⟺ n is power of 2 (and n > 0)
*/
