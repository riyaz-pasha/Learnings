import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Problem: Nth Super Ugly Number
 * 
 * Statement:
 * Given an integer n and an array of distinct prime numbers, return the n-th 
 * super ugly number. A super ugly number is a positive integer whose only 
 * prime factors are from a given array primes.
 * 
 * Constraints:
 * - 1 <= n <= 10^5
 * - 1 <= primes.length <= 100
 * - 2 <= primes[i] <= 1000
 * - primes[i] is guaranteed to be a prime number.
 * - All the values of primes are unique and sorted in ascending order.
 * - The n-th super ugly number is guaranteed to fit within a 32-bit signed integer.
 */
class SuperUglyNumber {

    /* ============================================================================
     * APPROACH 1: Dynamic Programming with Pointers
     * ============================================================================
     * Explanation:
     * Every super ugly number is formed by multiplying an existing super ugly 
     * number by one of the given primes. 
     * We can maintain an array `ugly` to store the first `n` super ugly numbers.
     * We also maintain an array of `pointers`, where `pointers[j]` keeps track of 
     * which ugly number the prime `primes[j]` is currently waiting to multiply.
     * 
     * At each step, the next super ugly number is the minimum of 
     * (ugly[pointers[j]] * primes[j]) for all j.
     * After finding the minimum, we increment the pointers for ALL primes that 
     * produced this minimum (this naturally handles duplicate generations).
     * 
     * ASCII Visual:
     * primes = [2, 7, 13], n = 4
     * 
     * Step 0: ugly = [1]
     * pointers = [0, 0, 0] (Indices into the ugly array)
     * multiples = [1*2, 1*7, 1*13] = [2, 7, 13] -> min is 2
     * 
     * Step 1: ugly = [1, 2]
     * Only primes[0] produced 2. Increment pointers[0].
     * pointers = [1, 0, 0]
     * multiples = [ugly[1]*2, ugly[0]*7, ugly[0]*13] = [4, 7, 13] -> min is 4
     * 
     * Step 2: ugly = [1, 2, 4]
     * Increment pointers[0].
     * pointers = [2, 0, 0]
     * multiples = [ugly[2]*2, ugly[0]*7, ugly[0]*13] = [8, 7, 13] -> min is 7
     * 
     * Time Complexity: O(N * K) where N is 'n' and K is 'primes.length'.
     * Space Complexity: O(N + K) for the ugly array and pointers.
     */
    public static int nthSuperUglyNumberDP(int n, int[] primes) {
        int k = primes.length;
        var ugly = new int[n];
        ugly[0] = 1;
        
        var pointers = new int[k];
        // Use long to prevent integer overflow during multiplications
        var nextMultiples = new long[k]; 
        
        for (int i = 0; i < k; i++) {
            nextMultiples[i] = primes[i];
        }

        for (int i = 1; i < n; i++) {
            // Find the minimum next multiple
            long min = nextMultiples[0];
            for (int j = 1; j < k; j++) {
                if (nextMultiples[j] < min) {
                    min = nextMultiples[j];
                }
            }
            
            ugly[i] = (int) min;
            
            // Advance the pointer(s) that match the minimum value to avoid duplicates
            for (int j = 0; j < k; j++) {
                if (nextMultiples[j] == min) {
                    pointers[j]++;
                    nextMultiples[j] = (long) ugly[pointers[j]] * primes[j];
                }
            }
        }
        
        return ugly[n - 1];
    }

    /* ============================================================================
     * APPROACH 2: Min-Heap for Pointer Optimization
     * ============================================================================
     * Explanation:
     * Approach 1 iterates over all `K` primes at every step to find the minimum.
     * When K is large (up to 100), this takes O(N * K) time. We can optimize 
     * the "find minimum" step from O(K) to O(log K) using a Priority Queue (Min-Heap).
     * 
     * The heap stores objects containing: 
     * { current_multiple_value, prime_factor, pointer_index }
     * 
     * 1. Initialize the heap with {primes[i], primes[i], 0}.
     * 2. Loop N-1 times to populate the ugly array.
     * 3. At each step, pop the smallest element. If it's equal to the last added 
     *    ugly number, skip it (handles duplicates like 2*7 == 7*2). Otherwise, add it.
     * 4. Push the next multiple for that prime back into the heap.
     * 
     * Time Complexity: O(N log K) 
     * Space Complexity: O(N + K)
     */
    
    // Java 14+ Record to cleanly store heap node state
    private record HeapNode(long val, int prime, int pointer) implements Comparable<HeapNode> {
        @Override
        public int compareTo(HeapNode other) {
            return Long.compare(this.val, other.val);
        }
    }

    public static int nthSuperUglyNumberMinHeap(int n, int[] primes) {
        if (n == 1) return 1;

        var ugly = new int[n];
        ugly[0] = 1;
        
        var minHeap = new PriorityQueue<HeapNode>();
        
        // Initialize the heap with the first multiple of each prime
        for (int p : primes) {
            minHeap.offer(new HeapNode(p, p, 0));
        }

        for (int i = 1; i < n; i++) {
            // Peek to get the minimum
            HeapNode minNode = minHeap.peek();
            ugly[i] = (int) minNode.val();
            
            // Extract all nodes from the heap that have this exact same minimum value.
            // This prevents duplicate generation (e.g., Prime A * Ugly B == Prime B * Ugly A).
            while (!minHeap.isEmpty() && minHeap.peek().val() == ugly[i]) {
                var current = minHeap.poll();
                int nextPointer = current.pointer() + 1;
                long nextVal = (long) ugly[nextPointer] * current.prime();
                minHeap.offer(new HeapNode(nextVal, current.prime(), nextPointer));
            }
        }
        
        return ugly[n - 1];
    }

    /* ============================================================================
     * TESTING / MAIN METHOD
     * ============================================================================
     */
    
    // Test case record
    public record TestCase(int n, int[] primes, int expected) {}

    public static void main(String[] args) {
        var testCases = List.of(
            new TestCase(
                12, 
                new int[]{2, 7, 13, 19}, 
                32
                // Sequence: 1, 2, 4, 7, 8, 13, 14, 16, 19, 26, 28, 32
            ),
            new TestCase(
                1, 
                new int[]{2, 3, 5}, 
                1
            ),
            new TestCase(
                5, 
                new int[]{2, 3, 5}, 
                5 // Sequence for regular ugly numbers: 1, 2, 3, 4, 5...
            ),
            new TestCase(
                45, 
                new int[]{2, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47}, 
                116
            )
        );

        System.out.println("Running tests for Super Ugly Number approaches...\n");

        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            System.out.println("Test Case " + (i + 1) + ": n = " + tc.n + ", primes = " + Arrays.toString(tc.primes));
            
            int ans1 = nthSuperUglyNumberDP(tc.n, tc.primes);
            int ans2 = nthSuperUglyNumberMinHeap(tc.n, tc.primes);

            boolean pass1 = (ans1 == tc.expected);
            boolean pass2 = (ans2 == tc.expected);

            System.out.println("  DP (O(N*K))    : " + (pass1 ? "PASS" : "FAIL") + " -> Result: " + ans1);
            System.out.println("  Heap (O(NlogK)): " + (pass2 ? "PASS" : "FAIL") + " -> Result: " + ans2);
            System.out.println("-".repeat(60));
        }
    }
}


class Solution {

    public int nthSuperUglyNumber(int n, int[] primes) {

        // 🔹 Min Heap → always gives the smallest available number
        PriorityQueue<Long> minHeap = new PriorityQueue<>();

        // 🔹 Set → to avoid duplicates (VERY IMPORTANT)
        // Example: 2×3 and 3×2 both produce 6
        Set<Long> seen = new HashSet<>();

        // 🔹 Step 1: Start with 1
        // Why 1? Because it is the base for generating all numbers
        minHeap.add(1L);
        seen.add(1L);

        long curr = 1;

        // 🔹 Step 2: Generate numbers one by one
        // We need the nth number → so repeat n times
        for (int i = 0; i < n; i++) {

            // 🔹 Always take the smallest number available
            curr = minHeap.poll();

            // 🔹 Generate next possible numbers
            // Multiply current number with all primes
            for (int prime : primes) {

                long next = curr * prime;

                // 🔹 Avoid duplicates
                // Without this, heap will have repeated values
                if (!seen.contains(next)) {

                    // Mark as seen
                    seen.add(next);

                    // Add to heap for future processing
                    minHeap.add(next);
                }
            }
        }

        // 🔹 After n iterations, curr will be the nth super ugly number
        return (int) curr;
    }
}
