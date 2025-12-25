class MinimumBitFlips {
    
    // Approach 1: XOR + Brian Kernighan's Algorithm (Optimal)
    // Time: O(k), Space: O(1) where k = number of differing bits
    public int minBitFlips1(int start, int goal) {
        // XOR gives us bits that differ
        int xor = start ^ goal;
        
        // Count set bits in XOR result (Kernighan's algorithm)
        int count = 0;
        while (xor != 0) {
            xor &= (xor - 1);  // Remove rightmost set bit
            count++;
        }
        
        return count;
    }
    
    // Approach 2: XOR + Built-in Bit Count
    // Time: O(1), Space: O(1)
    public int minBitFlips2(int start, int goal) {
        return Integer.bitCount(start ^ goal);
    }
    
    // Approach 3: Compare Bits One by One
    // Time: O(32) = O(1), Space: O(1)
    public int minBitFlips3(int start, int goal) {
        int count = 0;
        
        for (int i = 0; i < 32; i++) {
            // Check if bits at position i differ
            if (((start >> i) & 1) != ((goal >> i) & 1)) {
                count++;
            }
        }
        
        return count;
    }
    
    // Approach 4: XOR + Right Shift
    // Time: O(32) = O(1), Space: O(1)
    public int minBitFlips4(int start, int goal) {
        int xor = start ^ goal;
        int count = 0;
        
        while (xor != 0) {
            count += (xor & 1);  // Check rightmost bit
            xor >>>= 1;          // Unsigned right shift
        }
        
        return count;
    }
    
    // Approach 5: Recursive
    // Time: O(k), Space: O(k) for recursion
    public int minBitFlips5(int start, int goal) {
        if (start == goal) {
            return 0;
        }
        
        int diff = (start & 1) != (goal & 1) ? 1 : 0;
        return diff + minBitFlips5(start >>> 1, goal >>> 1);
    }
    
    // Approach 6: String Comparison (Not recommended)
    // Time: O(32), Space: O(32)
    public int minBitFlips6(int start, int goal) {
        String s1 = String.format("%32s", Integer.toBinaryString(start)).replace(' ', '0');
        String s2 = String.format("%32s", Integer.toBinaryString(goal)).replace(' ', '0');
        
        int count = 0;
        for (int i = 0; i < 32; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                count++;
            }
        }
        
        return count;
    }
    
    // Approach 7: XOR with Lookup Table
    // Time: O(1), Space: O(2^16)
    private static final int[] bitCountTable = new int[65536];
    
    static {
        for (int i = 0; i < 65536; i++) {
            bitCountTable[i] = Integer.bitCount(i);
        }
    }
    
    public int minBitFlips7(int start, int goal) {
        int xor = start ^ goal;
        return bitCountTable[xor & 0xFFFF] + bitCountTable[(xor >>> 16) & 0xFFFF];
    }
    
    // Test cases with visualization
    public static void main(String[] args) {
        MinimumBitFlips solution = new MinimumBitFlips();
        
        // Test Case 1
        int start1 = 10, goal1 = 7;
        int result1 = solution.minBitFlips1(start1, goal1);
        System.out.println("Test 1: start=" + start1 + ", goal=" + goal1);
        System.out.println("Output: " + result1);
        visualizeBitFlips(start1, goal1, result1);
        
        // Test Case 2
        int start2 = 3, goal2 = 4;
        int result2 = solution.minBitFlips1(start2, goal2);
        System.out.println("\nTest 2: start=" + start2 + ", goal=" + goal2);
        System.out.println("Output: " + result2);
        visualizeBitFlips(start2, goal2, result2);
        
        // Test Case 3: Same numbers
        int start3 = 5, goal3 = 5;
        int result3 = solution.minBitFlips1(start3, goal3);
        System.out.println("\nTest 3: start=" + start3 + ", goal=" + goal3);
        System.out.println("Output: " + result3);
        
        // Test Case 4: Complete flip
        int start4 = 0, goal4 = 15;
        int result4 = solution.minBitFlips1(start4, goal4);
        System.out.println("\nTest 4: start=" + start4 + ", goal=" + goal4);
        System.out.println("Output: " + result4);
        visualizeBitFlips(start4, goal4, result4);
        
        // Test Case 5: Large numbers
        int start5 = 1023, goal5 = 512;
        int result5 = solution.minBitFlips1(start5, goal5);
        System.out.println("\nTest 5: start=" + start5 + ", goal=" + goal5);
        System.out.println("Output: " + result5);
        
        // Compare all approaches
        System.out.println("\nComparing all approaches for start=10, goal=7:");
        System.out.println("Approach 1 (XOR + Kernighan): " + solution.minBitFlips1(start1, goal1));
        System.out.println("Approach 2 (XOR + Built-in):  " + solution.minBitFlips2(start1, goal1));
        System.out.println("Approach 3 (Compare bits):    " + solution.minBitFlips3(start1, goal1));
        System.out.println("Approach 4 (XOR + Shift):     " + solution.minBitFlips4(start1, goal1));
        System.out.println("Approach 5 (Recursive):       " + solution.minBitFlips5(start1, goal1));
        System.out.println("Approach 6 (String):          " + solution.minBitFlips6(start1, goal1));
        System.out.println("Approach 7 (Lookup):          " + solution.minBitFlips7(start1, goal1));
    }
    
    private static void visualizeBitFlips(int start, int goal, int flips) {
        String startBin = String.format("%8s", Integer.toBinaryString(start)).replace(' ', '0');
        String goalBin = String.format("%8s", Integer.toBinaryString(goal)).replace(' ', '0');
        int xor = start ^ goal;
        String xorBin = String.format("%8s", Integer.toBinaryString(xor)).replace(' ', '0');
        
        System.out.println("\nBinary representation:");
        System.out.println("Start: " + startBin + " (decimal: " + start + ")");
        System.out.println("Goal:  " + goalBin + " (decimal: " + goal + ")");
        System.out.println("XOR:   " + xorBin + " (decimal: " + xor + ")");
        System.out.println("       " + "^".repeat(8).replace("^", " ").substring(0, 8));
        
        // Mark differing positions
        StringBuilder diff = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (startBin.charAt(i) != goalBin.charAt(i)) {
                diff.append("^");
            } else {
                diff.append(" ");
            }
        }
        System.out.println("Diff:  " + diff + " (" + flips + " bits differ)");
        
        // Show which bits need to flip
        System.out.println("\nBits to flip (positions from right, 0-indexed):");
        for (int i = 0; i < 8; i++) {
            if (((xor >> i) & 1) == 1) {
                System.out.println("  Position " + i + ": " + 
                                 ((start >> i) & 1) + " → " + ((goal >> i) & 1));
            }
        }
        
        // Show Kernighan's algorithm trace
        System.out.println("\nKernighan's algorithm trace:");
        int temp = xor;
        int step = 0;
        while (temp != 0) {
            String tempBin = String.format("%8s", Integer.toBinaryString(temp)).replace(' ', '0');
            System.out.printf("Step %d: %s (decimal: %d)%n", ++step, tempBin, temp);
            temp &= (temp - 1);
        }
    }
}

/*
DETAILED EXPLANATION:

PROBLEM UNDERSTANDING:
- Convert 'start' to 'goal' by flipping bits
- Each flip changes one bit (0→1 or 1→0)
- Find MINIMUM number of flips needed

KEY INSIGHT: XOR + HAMMING WEIGHT

The solution is simply: Count bits that DIFFER between start and goal!

Why? Because:
1. Each differing bit needs exactly ONE flip
2. Bits that are the same don't need flipping
3. XOR operation identifies differing bits
4. Count the 1s in XOR result = minimum flips needed

XOR OPERATION PROPERTIES:
- 0 XOR 0 = 0 (same → no flip needed)
- 1 XOR 1 = 0 (same → no flip needed)
- 0 XOR 1 = 1 (different → flip needed)
- 1 XOR 0 = 1 (different → flip needed)

ALGORITHM:
1. Compute: xor = start ^ goal
2. Count set bits in xor
3. Return count

EXAMPLE WALKTHROUGH 1: start=10, goal=7

Binary representation:
start = 10 = 0b1010
goal  = 7  = 0b0111
XOR        = 0b1101 = 13

Positions where bits differ (marked with ^):
  1010  (start)
  0111  (goal)
  ^^^^  (4 positions differ)
  
But wait - only 3 bits differ!
Let's check again:
Position 0: 0 vs 1 → different ✓
Position 1: 1 vs 1 → same
Position 2: 0 vs 1 → different ✓
Position 3: 1 vs 0 → different ✓

XOR = 0b1101 has 3 set bits → 3 flips needed ✓

Step-by-step flips:
1010 → flip bit 0 → 1011
1011 → flip bit 2 → 1111
1111 → flip bit 3 → 0111 ✓

EXAMPLE WALKTHROUGH 2: start=3, goal=4

Binary:
start = 3 = 0b011
goal  = 4 = 0b100
XOR       = 0b111 = 7

All 3 bits differ:
Position 0: 1 vs 0 → different ✓
Position 1: 1 vs 0 → different ✓
Position 2: 0 vs 1 → different ✓

XOR = 0b111 has 3 set bits → 3 flips needed ✓

Step-by-step:
011 → flip bit 0 → 010
010 → flip bit 1 → 000
000 → flip bit 2 → 100 ✓

WHY XOR WORKS:

Mathematical proof:
- start and goal differ in exactly k bit positions
- Each differing position needs exactly 1 flip
- XOR sets bits to 1 exactly where they differ
- Counting 1s in XOR = k = minimum flips

This is known as HAMMING DISTANCE!

HAMMING DISTANCE:
The number of positions at which corresponding bits differ
= Number of 1s in (start XOR goal)
= Minimum bit flips needed

COMPLEXITY ANALYSIS:

Approach 1 (XOR + Kernighan):
- Time: O(k) where k = differing bits
- Space: O(1)
- ✓✓✓ Optimal

Approach 2 (XOR + Built-in):
- Time: O(1) - highly optimized
- Space: O(1)
- ✓✓✓ Best for production

Approach 3 (Compare each bit):
- Time: O(32) = O(1)
- Space: O(1)
- Checks all bits even if few differ

Approach 4 (XOR + Shift):
- Time: O(32) = O(1)
- Space: O(1)
- Similar to approach 3

Approach 7 (Lookup table):
- Time: O(1) - 2 lookups
- Space: O(2^16)
- Best for repeated calls

STEP-BY-STEP VISUALIZATION:

For start=10, goal=7:

start: 1 0 1 0
goal:  0 1 1 1
       ↓ ↓ ↓ ↓
XOR:   1 1 0 1  ← Three 1s = 3 flips needed

Kernighan's algorithm on XOR (1101):
Step 1: 1101 & 1100 = 1100 (removed rightmost 1)
Step 2: 1100 & 1011 = 1000 (removed another 1)
Step 3: 1000 & 0111 = 0000 (removed last 1)
Total: 3 iterations = 3 flips ✓

EDGE CASES:

1. start == goal: 0 flips needed
   - XOR = 0
   - No differing bits

2. Completely opposite: All bits differ
   - Example: 0 and 15 (0b0000 vs 0b1111)
   - XOR = 15, all 4 bits differ

3. Single bit difference:
   - Example: 4 and 5 (0b100 vs 0b101)
   - XOR = 1, only 1 bit differs

4. Powers of 2: Often just 1-2 flips

PRACTICAL APPLICATIONS:

1. Error detection/correction (Hamming codes)
2. Data transmission verification
3. Genetic algorithms (mutation distance)
4. Image comparison (pixel differences)
5. Network protocols (checksum)
6. DNA sequence comparison
7. Cache coherence protocols

INTERVIEW TIPS:

1. Recognize this as Hamming distance
2. Explain XOR identifies differing bits
3. Mention it's just bit counting after XOR
4. Show example with binary representation
5. Discuss O(k) vs O(32) complexity
6. Can use built-in bitCount for simplicity
7. Draw the XOR operation visually

COMMON MISTAKES:

1. Trying to find actual flip sequence
   - Not needed! Just count differences
2. Forgetting XOR operation
3. Not recognizing this as Hamming distance
4. Using inefficient string comparison
5. Overcomplicating with unnecessary steps

OPTIMIZATION NOTES:

1. XOR is single operation (very fast)
2. Kernighan's algorithm optimal for sparse differences
3. Built-in bitCount is highly optimized (POPCNT instruction)
4. For repeated calls: use lookup table
5. No need to track which bits to flip

RELATED CONCEPTS:

1. Hamming Weight: Count of 1s in a number
2. Hamming Distance: Differing positions between two numbers
3. XOR properties: a ^ a = 0, a ^ 0 = a
4. Bit manipulation tricks
5. Error correcting codes

WHY IT'S OPTIMAL:

Proof of optimality:
- Each differing bit MUST be flipped (no way around it)
- Each flip changes exactly one bit
- Therefore: minimum flips = number of differing bits
- This is exactly what our solution computes

Cannot do better than O(k) where k = differing bits!

MATHEMATICAL INSIGHT:

Hamming distance is a metric:
- d(x,x) = 0 (identity)
- d(x,y) = d(y,x) (symmetry)
- d(x,z) ≤ d(x,y) + d(y,z) (triangle inequality)

This makes it useful for many applications beyond just bit flips!

COMPARISON WITH SIMILAR PROBLEMS:

1. Power of Two: Check if exactly 1 bit set
2. Hamming Weight: Count bits in single number
3. This problem: Count differing bits (XOR + count)
4. Reverse Bits: Completely different operation

All use similar bit manipulation techniques!
*/
