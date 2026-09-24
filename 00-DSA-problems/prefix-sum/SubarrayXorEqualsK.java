import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SubarrayXorEqualsK {

    // Solution 1: Brute Force (Check all subarrays)
    // Time: O(n²), Space: O(1)
    public int subarrayXorBruteForce(int[] arr, int k) {
        int count = 0;
        int n = arr.length;

        // Try all possible subarrays
        for (int i = 0; i < n; i++) {
            int xor = 0;
            for (int j = i; j < n; j++) {
                xor ^= arr[j];

                // If XOR equals k, increment count
                if (xor == k) {
                    count++;
                }
            }
        }

        return count;
    }

    // Solution 2: Optimal - HashMap with Prefix XOR
    // Time: O(n), Space: O(n)
    public int subarrayXorOptimal(int[] arr, int k) {
        /*
         * Key Insight:
         * If prefixXOR[i] ^ prefixXOR[j] = k
         * Then XOR of subarray from (i+1) to j equals k
         * 
         * Rearranging: prefixXOR[i] = prefixXOR[j] ^ k
         * So we look for (currentXOR ^ k) in our map
         */

        Map<Integer, Integer> xorCount = new HashMap<>();
        xorCount.put(0, 1); // Base case: XOR 0 before any element

        int count = 0;
        int prefixXor = 0;

        for (int num : arr) {
            prefixXor ^= num;

            // Check if (prefixXor ^ k) exists in map
            // If exists, means there are subarrays ending at current index with XOR k
            int target = prefixXor ^ k;
            if (xorCount.containsKey(target)) {
                count += xorCount.get(target);
            }

            // Add current prefix XOR to map
            xorCount.put(prefixXor, xorCount.getOrDefault(prefixXor, 0) + 1);
        }

        return count;
    }

    // Solution 3: Optimal with Detailed Comments
    // Time: O(n), Space: O(n)
    public int subarrayXorDetailed(int[] arr, int k) {
        /*
         * XOR Properties:
         * 1. a ^ a = 0
         * 2. a ^ 0 = a
         * 3. XOR is commutative and associative
         * 4. If a ^ b = c, then a ^ c = b and b ^ c = a
         * 
         * Using property 4:
         * If prefixXOR[j] ^ prefixXOR[i] = k
         * Then prefixXOR[i] = prefixXOR[j] ^ k
         * 
         * We maintain a map of prefix XOR frequencies
         * For each position, we check how many times (currentXOR ^ k) appeared before
         */

        if (arr == null || arr.length == 0) {
            return 0;
        }

        // Map to store: prefixXOR -> frequency
        Map<Integer, Integer> xorFrequency = new HashMap<>();
        xorFrequency.put(0, 1); // Empty prefix has XOR 0

        int totalCount = 0;
        int currentXor = 0;

        for (int i = 0; i < arr.length; i++) {
            // Update prefix XOR
            currentXor ^= arr[i];

            // Find complement: what XOR value would give us k?
            // If we want: previousXOR ^ currentXOR = k
            // Then: previousXOR = currentXOR ^ k
            int complement = currentXor ^ k;

            // Add count of all subarrays ending at i with XOR k
            if (xorFrequency.containsKey(complement)) {
                totalCount += xorFrequency.get(complement);
            }

            // Update frequency of current prefix XOR
            xorFrequency.put(currentXor, xorFrequency.getOrDefault(currentXor, 0) + 1);
        }

        return totalCount;
    }

    // Solution 4: With Visualization
    // Time: O(n), Space: O(n)
    public int subarrayXorWithVisualization(int[] arr, int k, boolean showSteps) {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(0, 1);

        int count = 0;
        int xor = 0;

        if (showSteps) {
            System.out.println("\n=== Step-by-step Execution ===");
            System.out.print("Array: ");
            printArray(arr);
            System.out.println(", k = " + k);
            System.out.println("\nXOR Properties used:");
            System.out.println("- If prefixXOR[j] ^ prefixXOR[i] = k");
            System.out.println("- Then prefixXOR[i] = prefixXOR[j] ^ k\n");
        }

        for (int i = 0; i < arr.length; i++) {
            xor ^= arr[i];
            int target = xor ^ k;

            if (showSteps) {
                System.out.printf("Index %d: num=%d, prefixXOR=%d, looking for %d (=%d^%d)",
                        i, arr[i], xor, target, xor, k);
            }

            if (map.containsKey(target)) {
                int occurrences = map.get(target);
                count += occurrences;

                if (showSteps) {
                    System.out.printf(" -> Found %d time(s)! Total count: %d", occurrences, count);
                }
            }

            map.put(xor, map.getOrDefault(xor, 0) + 1);

            if (showSteps) {
                System.out.println();
            }
        }

        if (showSteps) {
            System.out.println("\nFinal count: " + count);
        }

        return count;
    }

    // Helper: Print array
    private static void printArray(int[] arr) {
        System.out.print("[");
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i]);
            if (i < arr.length - 1)
                System.out.print(", ");
        }
        System.out.print("]");
    }

    // Helper: Print subarray
    private static void printSubarray(int[] arr, int start, int end) {
        System.out.print("[");
        for (int i = start; i <= end; i++) {
            System.out.print(arr[i]);
            if (i < end)
                System.out.print(", ");
        }
        System.out.print("]");
    }

    // Helper: Find and print all subarrays with XOR = k
    private static void findAllSubarrays(int[] arr, int k) {
        System.out.println("\nAll subarrays with XOR = " + k + ":");

        int count = 0;
        for (int i = 0; i < arr.length; i++) {
            int xor = 0;
            for (int j = i; j < arr.length; j++) {
                xor ^= arr[j];
                if (xor == k) {
                    count++;
                    System.out.print("  " + count + ". Indices " + i + " to " + j + ": ");
                    printSubarray(arr, i, j);
                    System.out.println(" (XOR = " + xor + ")");
                }
            }
        }

        if (count == 0) {
            System.out.println("  None found");
        }
    }

    // Helper: Demonstrate XOR properties
    private static void demonstrateXorProperties() {
        System.out.println("\n=== XOR Properties Demo ===");
        System.out.println("1. a ^ a = 0:     5 ^ 5 = " + (5 ^ 5));
        System.out.println("2. a ^ 0 = a:     5 ^ 0 = " + (5 ^ 0));
        System.out.println("3. Commutative:   3 ^ 5 = " + (3 ^ 5) + ", 5 ^ 3 = " + (5 ^ 3));
        System.out.println("4. Associative:   (2^3)^4 = " + ((2 ^ 3) ^ 4) + ", 2^(3^4) = " + (2 ^ (3 ^ 4)));
        System.out.println("\n5. If a ^ b = c, then:");
        int a = 5, b = 3, c = a ^ b;
        System.out.println("   a = " + a + ", b = " + b + ", c = a ^ b = " + c);
        System.out.println("   a ^ c = " + (a ^ c) + " (equals b)");
        System.out.println("   b ^ c = " + (b ^ c) + " (equals a)");
    }

    // Test cases
    public static void main(String[] args) {
        SubarrayXorEqualsK solution = new SubarrayXorEqualsK();

        // Demonstrate XOR properties first
        demonstrateXorProperties();

        // Test case 1: Basic example
        int[] arr1 = { 4, 2, 2, 6, 4 };
        int k1 = 6;
        System.out.print("\n\nExample 1 - Input: ");
        printArray(arr1);
        System.out.println(", k = " + k1);
        System.out.println("Output (Optimal): " + solution.subarrayXorOptimal(arr1, k1));
        System.out.println("Output (Brute Force): " + solution.subarrayXorBruteForce(arr1, k1));
        findAllSubarrays(arr1, k1);

        // Test case 2: Another example
        int[] arr2 = { 5, 6, 7, 8, 9 };
        int k2 = 5;
        System.out.print("\nExample 2 - Input: ");
        printArray(arr2);
        System.out.println(", k = " + k2);
        System.out.println("Output: " + solution.subarrayXorOptimal(arr2, k2));
        findAllSubarrays(arr2, k2);

        // Test case 3: All same elements
        int[] arr3 = { 1, 1, 1, 1 };
        int k3 = 0;
        System.out.print("\nAll same (k=0) - Input: ");
        printArray(arr3);
        System.out.println(", k = " + k3);
        System.out.println("Output: " + solution.subarrayXorOptimal(arr3, k3));
        findAllSubarrays(arr3, k3);

        // Test case 4: No subarrays with XOR = k
        int[] arr4 = { 1, 2, 3, 4 };
        int k4 = 10;
        System.out.print("\nNo match - Input: ");
        printArray(arr4);
        System.out.println(", k = " + k4);
        System.out.println("Output: " + solution.subarrayXorOptimal(arr4, k4));

        // Test case 5: Single element
        int[] arr5 = { 5 };
        int k5 = 5;
        System.out.print("\nSingle element - Input: ");
        printArray(arr5);
        System.out.println(", k = " + k5);
        System.out.println("Output: " + solution.subarrayXorOptimal(arr5, k5));

        // Test case 6: Entire array XORs to k
        int[] arr6 = { 1, 2, 3 };
        int k6 = 0; // 1 ^ 2 ^ 3 = 0
        System.out.print("\nEntire array - Input: ");
        printArray(arr6);
        System.out.println(", k = " + k6);
        System.out.println("Output: " + solution.subarrayXorOptimal(arr6, k6));
        findAllSubarrays(arr6, k6);

        // Test case 7: With detailed visualization
        int[] arr7 = { 4, 2, 2, 6, 4 };
        int k7 = 6;
        System.out.print("\nDetailed walkthrough - Input: ");
        printArray(arr7);
        System.out.println(", k = " + k7);
        solution.subarrayXorWithVisualization(arr7, k7, true);

        // Performance comparison
        System.out.println("\n\n=== Performance Comparison ===");
        int[] largeArray = new int[1000];
        Random rand = new Random(42);
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = rand.nextInt(100);
        }
        int testK = 42;

        long start = System.nanoTime();
        int resultBrute = solution.subarrayXorBruteForce(largeArray, testK);
        long timeBrute = System.nanoTime() - start;

        start = System.nanoTime();
        int resultOptimal = solution.subarrayXorOptimal(largeArray, testK);
        long timeOptimal = System.nanoTime() - start;

        System.out.println("Array size: 1000 elements, k = " + testK);
        System.out.println("Brute Force: " + resultBrute + " subarrays (Time: " + timeBrute / 1000000.0 + " ms)");
        System.out.println("Optimal:     " + resultOptimal + " subarrays (Time: " + timeOptimal / 1000000.0 + " ms)");
        System.out.println("Speedup:     " + (timeBrute / (double) timeOptimal) + "x faster");
    }
}
