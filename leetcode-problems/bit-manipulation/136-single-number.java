import java.util.*;
/*
* Given a non-empty array of integers nums, every element appears twice except
* for one. Find that single one.
* 
* You must implement a solution with a linear runtime complexity and use only
* constant extra space.
* 
* 
* Example 1:
* Input: nums = [2,2,1]
* Output: 1
* 
* Example 2:
* Input: nums = [4,1,2,1,2]
* Output: 4
* 
* Example 3:
* Input: nums = [1]
* Output: 1
*/

class SingleNumber {

    // Solution 1: XOR Approach (Optimal - meets all constraints)
    // Time Complexity: O(n)
    // Space Complexity: O(1)
    public int singleNumber1(int[] nums) {
        int result = 0;
        for (int num : nums) {
            result ^= num; // XOR operation
        }
        return result;
    }

    // Solution 2: XOR with different implementation style
    public int singleNumber2(int[] nums) {
        int result = 0;
        for (int i = 0; i < nums.length; i++) {
            result ^= nums[i];
        }
        return result;
    }

    // Solution 3: Using HashSet (violates space constraint but educational)
    public int singleNumber3(int[] nums) {
        Set<Integer> set = new HashSet<>();
        for (int num : nums) {
            if (set.contains(num)) {
                set.remove(num);
            } else {
                set.add(num);
            }
        }
        return set.iterator().next();
    }

    // Solution 4: Using HashMap to count (violates space constraint)
    public int singleNumber4(int[] nums) {
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

    // Solution 5: Sorting approach (violates space constraint if not in-place)
    public int singleNumber5(int[] nums) {
        Arrays.sort(nums);

        // Check pairs, the single number will break the pattern
        for (int i = 0; i < nums.length - 1; i += 2) {
            if (nums[i] != nums[i + 1]) {
                return nums[i];
            }
        }

        // If we reach here, the single number is the last element
        return nums[nums.length - 1];
    }

    // Solution 6: Mathematical approach using sum (violates space constraint due to
    // Set)
    public int singleNumber6(int[] nums) {
        Set<Integer> uniqueNums = new HashSet<>();
        int arraySum = 0;

        for (int num : nums) {
            uniqueNums.add(num);
            arraySum += num;
        }

        int uniqueSum = 0;
        for (int num : uniqueNums) {
            uniqueSum += num;
        }

        // 2 * (sum of unique numbers) - (sum of array) = single number
        return 2 * uniqueSum - arraySum;
    }

    // Solution 7: Functional approach using Java 8 Streams (XOR)
    public int singleNumber7(int[] nums) {
        return Arrays.stream(nums).reduce(0, (a, b) -> a ^ b);
    }

    // Helper method to demonstrate XOR properties
    public static void demonstrateXOR() {
        System.out.println("XOR Properties Demonstration:");
        System.out.println("=".repeat(40));

        // Property 1: a ^ a = 0
        System.out.println("a ^ a = 0:");
        System.out.printf("5 ^ 5 = %d\n", 5 ^ 5);
        System.out.printf("7 ^ 7 = %d\n", 7 ^ 7);

        // Property 2: a ^ 0 = a
        System.out.println("\na ^ 0 = a:");
        System.out.printf("5 ^ 0 = %d\n", 5 ^ 0);
        System.out.printf("7 ^ 0 = %d\n", 7 ^ 0);

        // Property 3: XOR is commutative and associative
        System.out.println("\nXOR is commutative and associative:");
        int a = 2, b = 3, c = 5;
        System.out.printf("(%d ^ %d) ^ %d = %d\n", a, b, c, (a ^ b) ^ c);
        System.out.printf("%d ^ (%d ^ %d) = %d\n", a, b, c, a ^ (b ^ c));
        System.out.printf("%d ^ %d ^ %d = %d\n", a, b, c, a ^ b ^ c);
        System.out.printf("%d ^ %d ^ %d = %d\n", b, a, c, b ^ a ^ c);

        // Example with duplicates
        System.out.println("\nExample with array [4,1,2,1,2]:");
        int[] example = { 4, 1, 2, 1, 2 };
        int result = 0;
        for (int i = 0; i < example.length; i++) {
            System.out.printf("Step %d: %d ^ %d = %d\n", i + 1, result, example[i], result ^ example[i]);
            result ^= example[i];
        }
        System.out.printf("Final result: %d\n", result);
        System.out.println();
    }

    // Performance and correctness testing
    public static void main(String[] args) {
        SingleNumber solution = new SingleNumber();

        // Test cases
        int[][] testCases = {
                { 2, 2, 1 }, // Expected: 1
                { 4, 1, 2, 1, 2 }, // Expected: 4
                { 1 }, // Expected: 1
                { 7, 3, 5, 3, 5 }, // Expected: 7
                { -1, -2, -3, -2, -3 } // Expected: -1
        };

        int[] expected = { 1, 4, 1, 7, -1 };

        System.out.println("Testing all solutions:");
        System.out.println("=".repeat(60));

        for (int i = 0; i < testCases.length; i++) {
            int[] nums = testCases[i];
            int exp = expected[i];

            System.out.printf("Test case %d: %s\n", i + 1, Arrays.toString(nums));
            System.out.printf("Expected: %d\n", exp);

            // Test solutions that meet the constraints
            int result1 = solution.singleNumber1(nums);
            int result2 = solution.singleNumber2(nums);
            int result7 = solution.singleNumber7(nums);

            System.out.printf("XOR Solution 1: %d %s\n", result1, result1 == exp ? "✓" : "✗");
            System.out.printf("XOR Solution 2: %d %s\n", result2, result2 == exp ? "✓" : "✗");
            System.out.printf("Stream XOR: %d %s\n", result7, result7 == exp ? "✓" : "✗");

            // Test solutions that violate space constraint (for educational purposes)
            int result3 = solution.singleNumber3(nums.clone());
            int result4 = solution.singleNumber4(nums.clone());
            int result5 = solution.singleNumber5(nums.clone());
            int result6 = solution.singleNumber6(nums.clone());

            System.out.printf("HashSet: %d %s (O(n) space)\n", result3, result3 == exp ? "✓" : "✗");
            System.out.printf("HashMap: %d %s (O(n) space)\n", result4, result4 == exp ? "✓" : "✗");
            System.out.printf("Sorting: %d %s (O(log n) time)\n", result5, result5 == exp ? "✓" : "✗");
            System.out.printf("Math: %d %s (O(n) space)\n", result6, result6 == exp ? "✓" : "✗");

            System.out.println("-".repeat(40));
        }

        // Demonstrate XOR properties
        demonstrateXOR();

        // Performance comparison
        System.out.println("Performance test (1 million operations):");
        int[] largeArray = new int[1001]; // 500 pairs + 1 single
        for (int i = 0; i < 500; i++) {
            largeArray[i * 2] = i;
            largeArray[i * 2 + 1] = i;
        }
        largeArray[1000] = 999; // Single number

        int iterations = 1000;

        // Test XOR approach
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.singleNumber1(largeArray);
        }
        long xorTime = System.nanoTime() - start;

        // Test HashSet approach
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.singleNumber3(largeArray);
        }
        long setTime = System.nanoTime() - start;

        System.out.printf("XOR approach: %.2f ms\n", xorTime / 1_000_000.0);
        System.out.printf("HashSet approach: %.2f ms\n", setTime / 1_000_000.0);
        System.out.printf("XOR is %.2fx faster\n", (double) setTime / xorTime);

        System.out.println("\n" + "=".repeat(60));
        System.out.println("OPTIMAL SOLUTION: XOR approach (Solution 1)");
        System.out.println("✓ O(n) time complexity");
        System.out.println("✓ O(1) space complexity");
        System.out.println("✓ No additional data structures needed");
        System.out.println("✓ Works with negative numbers");
        System.out.println("=".repeat(60));
    }

}
