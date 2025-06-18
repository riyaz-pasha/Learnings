import java.util.*;
/*
 * Given an integer array nums where every element appears three times except
 * for one, which appears exactly once. Find the single element and return it.
 * 
 * You must implement a solution with a linear runtime complexity and use only
 * constant extra space.
 * 
 * Example 1:
 * Input: nums = [2,2,3,2]
 * Output: 3
 * 
 * Example 2:
 * Input: nums = [0,1,0,1,0,1,99]
 * Output: 99
 */

class SingleNumberII {

    // Solution 1: Bit Counting (Most intuitive optimal solution)
    public int singleNumber1(int[] nums) {
        int result = 0;

        // Check each of the 32 bits
        for (int i = 0; i < 32; i++) {
            int bitCount = 0;

            // Count how many numbers have this bit set
            for (int num : nums) {
                if ((num >> i) & 1) {
                    bitCount++;
                }
            }

            // If count is not divisible by 3, the single number has this bit set
            if (bitCount % 3 != 0) {
                result |= (1 << i);
            }
        }

        return result;

        // ✅ Key Idea:
        // Every bit in a 32-bit integer can be 0 or 1. If you sum up the number of 1s
        // at each bit position across all numbers:

        // - Bits belonging to numbers that appear 3 times will contribute a total count
        // - that's a multiple of 3.

        // - The number that appears only once will contribute extra 1s, which make the
        // - total not divisible by 3.

        // So, for each of the 32 bits:

        // - Sum the number of 1s at that bit position across all numbers.

        // - If the sum is not divisible by 3, then the unique number has a 1 at that
        // - position.
    }

    // Solution 2: Two Variables State Machine (Most elegant)
    public int singleNumber2(int[] nums) {
        int ones = 0, twos = 0;

        for (int num : nums) {
            // Update twos: add bits that appear for the second time
            twos |= (ones & num);

            // Update ones: toggle bits for first appearance, clear for second
            ones ^= num;

            // Clear bits that appear three times from both ones and twos
            int threes = ones & twos;
            ones &= ~threes;
            twos &= ~threes;
        }

        return ones;
    }

    // Solution 3: Three Variables State Machine (More explicit)
    public int singleNumber3(int[] nums) {
        int ones = 0, twos = 0, threes = 0;

        for (int num : nums) {
            // Calculate what appears exactly three times
            threes = twos & (ones & num);

            // Update twos: appears twice
            twos = twos | (ones & num);

            // Update ones: appears once
            ones = ones ^ num;

            // Remove numbers that appear three times
            ones = ones & (~threes);
            twos = twos & (~threes);
        }

        return ones;
    }

    // Solution 4: Optimized State Machine
    public int singleNumber4(int[] nums) {
        int first = 0, second = 0;

        for (int num : nums) {
            first = (first ^ num) & (~second);
            second = (second ^ num) & (~first);
        }

        return first;
    }

    // Solution 5: Using HashMap (violates space constraint but educational)
    public int singleNumber5(int[] nums) {
        Map<Integer, Integer> count = new HashMap<>();

        for (int num : nums) {
            count.put(num, count.getOrDefault(num, 0) + 1);
        }

        for (Map.Entry<Integer, Integer> entry : count.entrySet()) {
            if (entry.getValue() == 1) {
                return entry.getKey();
            }
        }

        return -1; // Should never reach here
    }

    // Solution 6: Using Set operations (violates space constraint)
    public int singleNumber6(int[] nums) {
        Set<Integer> seen = new HashSet<>();
        Set<Integer> twice = new HashSet<>();

        for (int num : nums) {
            if (twice.contains(num)) {
                // Third occurrence, remove from both sets
                seen.remove(num);
                twice.remove(num);
            } else if (seen.contains(num)) {
                // Second occurrence, move to twice set
                twice.add(num);
            } else {
                // First occurrence
                seen.add(num);
            }
        }

        return seen.iterator().next();
    }

    // Helper method to demonstrate state transitions
    public static void demonstrateStateMachine(int[] nums) {
        System.out.println("State Machine Demonstration:");
        System.out.println("Array: " + Arrays.toString(nums));
        System.out.println("Tracking states for each number:");
        System.out.println("ones = numbers appearing 1 time (mod 3)");
        System.out.println("twos = numbers appearing 2 times (mod 3)");
        System.out.println();

        int ones = 0, twos = 0;

        System.out.printf("%-10s %-10s %-10s %-20s %-20s\n",
                "Step", "Number", "Binary", "ones", "twos");
        System.out.println("-".repeat(80));

        for (int i = 0; i < nums.length; i++) {
            int num = nums[i];

            // Apply state machine logic
            int newTwos = twos | (ones & num);
            int newOnes = ones ^ num;
            int threes = newOnes & newTwos;
            newOnes &= ~threes;
            newTwos &= ~threes;

            System.out.printf("%-10d %-10d %-10s %-20s %-20s\n",
                    i + 1, num, Integer.toBinaryString(num),
                    Integer.toBinaryString(newOnes),
                    Integer.toBinaryString(newTwos));

            ones = newOnes;
            twos = newTwos;
        }

        System.out.printf("\nFinal result: %d (binary: %s)\n\n",
                ones, Integer.toBinaryString(ones));
    }

    // Helper method to explain bit counting approach
    public static void demonstrateBitCounting(int[] nums) {
        System.out.println("Bit Counting Approach Demonstration:");
        System.out.println("Array: " + Arrays.toString(nums));
        System.out.println();

        // Find maximum number of bits needed
        int maxBits = 0;
        for (int num : nums) {
            if (num != 0) {
                maxBits = Math.max(maxBits, 32 - Integer.numberOfLeadingZeros(Math.abs(num)));
            }
        }
        maxBits = Math.max(maxBits, 8); // Show at least 8 bits

        System.out.printf("%-5s %-15s %-10s\n", "Bit", "Count", "Count%3");
        System.out.println("-".repeat(35));

        int result = 0;
        for (int i = 0; i < maxBits; i++) {
            int bitCount = 0;

            for (int num : nums) {
                if ((num >> i) & 1) {
                    bitCount++;
                }
            }

            boolean setBit = (bitCount % 3 != 0);
            if (setBit) {
                result |= (1 << i);
            }

            System.out.printf("%-5d %-15d %-10d %s\n",
                    i, bitCount, bitCount % 3,
                    setBit ? "← Set in result" : "");
        }

        System.out.printf("\nResult: %d (binary: %s)\n\n",
                result, Integer.toBinaryString(result));
    }

    // Test all solutions
    public static void main(String[] args) {
        SingleNumberII solution = new SingleNumberII();

        // Test cases
        int[][] testCases = {
                { 2, 2, 3, 2 }, // Expected: 3
                { 0, 1, 0, 1, 0, 1, 99 }, // Expected: 99
                { -2, -2, 1, 1, 4, 1, 4, 4, -2 }, // Expected: -2 (testing negative)
                { 1 }, // Expected: 1
                { 5, 5, 5, 8 } // Expected: 8
        };

        int[] expected = { 3, 99, -2, 1, 8 };

        System.out.println("Testing all solutions:");
        System.out.println("=".repeat(70));

        for (int i = 0; i < testCases.length; i++) {
            int[] nums = testCases[i];
            int exp = expected[i];

            System.out.printf("Test case %d: %s\n", i + 1, Arrays.toString(nums));
            System.out.printf("Expected: %d\n", exp);

            // Test optimal solutions
            int result1 = solution.singleNumber1(nums);
            int result2 = solution.singleNumber2(nums);
            int result3 = solution.singleNumber3(nums);
            int result4 = solution.singleNumber4(nums);

            System.out.printf("Bit Counting: %d %s\n", result1, result1 == exp ? "✓" : "✗");
            System.out.printf("State Machine 2-var: %d %s\n", result2, result2 == exp ? "✓" : "✗");
            System.out.printf("State Machine 3-var: %d %s\n", result3, result3 == exp ? "✓" : "✗");
            System.out.printf("Optimized State: %d %s\n", result4, result4 == exp ? "✓" : "✗");

            // Test non-optimal solutions
            int result5 = solution.singleNumber5(nums);
            int result6 = solution.singleNumber6(nums);

            System.out.printf("HashMap: %d %s (O(n) space)\n", result5, result5 == exp ? "✓" : "✗");
            System.out.printf("Set operations: %d %s (O(n) space)\n", result6, result6 == exp ? "✓" : "✗");

            System.out.println("-".repeat(50));
        }

        // Demonstrate approaches with examples
        System.out.println();
        demonstrateBitCounting(new int[] { 2, 2, 3, 2 });
        demonstrateStateMachine(new int[] { 2, 2, 3, 2 });

        // Performance comparison
        System.out.println("Performance test (100,000 operations):");
        int[] largeArray = new int[3001]; // 1000 numbers × 3 + 1 single
        for (int i = 0; i < 1000; i++) {
            largeArray[i * 3] = i;
            largeArray[i * 3 + 1] = i;
            largeArray[i * 3 + 2] = i;
        }
        largeArray[3000] = 99999; // Single number

        int iterations = 100;

        // Test bit counting approach
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.singleNumber1(largeArray);
        }
        long bitCountTime = System.nanoTime() - start;

        // Test state machine approach
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.singleNumber2(largeArray);
        }
        long stateMachineTime = System.nanoTime() - start;

        // Test HashMap approach
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.singleNumber5(largeArray);
        }
        long hashMapTime = System.nanoTime() - start;

        System.out.printf("Bit Counting: %.2f ms\n", bitCountTime / 1_000_000.0);
        System.out.printf("State Machine: %.2f ms\n", stateMachineTime / 1_000_000.0);
        System.out.printf("HashMap: %.2f ms\n", hashMapTime / 1_000_000.0);

        System.out.printf("State Machine is %.2fx faster than Bit Counting\n",
                (double) bitCountTime / stateMachineTime);
        System.out.printf("State Machine is %.2fx faster than HashMap\n",
                (double) hashMapTime / stateMachineTime);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("RECOMMENDED SOLUTIONS:");
        System.out.println("1. State Machine (Solution 2) - Most elegant and fastest");
        System.out.println("2. Bit Counting (Solution 1) - Most intuitive to understand");
        System.out.println("Both meet O(n) time and O(1) space requirements");
        System.out.println("=".repeat(70));
    }
}