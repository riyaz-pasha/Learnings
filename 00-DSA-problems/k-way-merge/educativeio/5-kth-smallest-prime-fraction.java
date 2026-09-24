import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Problem: K-th Smallest Prime Fraction
 * 
 * Statement:
 * You are given a sorted array of unique integers, arr, which includes the number 1 
 * and other prime numbers. You are also given an integer k.
 * For every index i and j where 0 <= i < j < arr.length, you form a fraction 
 * arr[i] / arr[j].
 * Return the k-th smallest fraction as an array [numerator, denominator].
 * 
 * Constraints:
 * - 2 <= arr.length <= 10^3
 * - 1 <= arr[i] <= 3 * 10^4
 * - arr[0] = 1, arr[i] is prime for i > 0
 * - 1 <= k <= arr.length * (arr.length - 1) / 2
 */
class KthSmallestPrimeFraction {

    /* ============================================================================
     * APPROACH 1: Brute Force (Generate All and Sort)
     * ============================================================================
     * Explanation:
     * Generate every possible fraction arr[i] / arr[j] where i < j. Store them 
     * in a list, then sort the list based on their floating-point value.
     * Finally, return the k-th element from the sorted list.
     * 
     * Time Complexity: O(N^2 log(N^2)) where N is the length of the array.
     * Space Complexity: O(N^2) to store all possible fractions.
     */
    
    // Record to store fraction details for sorting
    private record Fraction(int num, int den) implements Comparable<Fraction> {
        @Override
        public int compareTo(Fraction other) {
            // Using Double.compare for floating-point comparison
            return Double.compare((double) this.num / this.den, (double) other.num / other.den);
        }
    }

    public static int[] kthSmallestPrimeFractionBruteForce(int[] arr, int k) {
        int n = arr.length;
        var fractions = new ArrayList<Fraction>();

        // Generate all valid fractions
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                fractions.add(new Fraction(arr[i], arr[j]));
            }
        }

        // Sort by actual fraction value
        Collections.sort(fractions);

        // Retrieve the k-th smallest (0-indexed, so k-1)
        var kthFraction = fractions.get(k - 1);
        return new int[]{kthFraction.num(), kthFraction.den()};
    }

    /* ============================================================================
     * APPROACH 2: Min-Heap (Priority Queue)
     * ============================================================================
     * Explanation:
     * Notice that for a fixed denominator arr[j], the fractions arr[i]/arr[j] are 
     * already sorted in increasing order as 'i' increases. 
     * This is identical to having (N-1) sorted lists, where the j-th list is:
     * [arr[0]/arr[j], arr[1]/arr[j], ..., arr[j-1]/arr[j]].
     * 
     * We initialize a Min-Heap with the smallest element of each list (arr[0]/arr[j]).
     * We then extract the minimum fraction k-1 times. When we extract arr[i]/arr[j],
     * we insert the next fraction from the same list: arr[i+1]/arr[j].
     * 
     * ASCII Matrix Analogy:
     * Denominators across columns, Numerators down rows.
     *       j=1 (2)  j=2 (3)  j=3 (5)
     * i=0(1)| 1/2      1/3      1/5  <-- Start Min-Heap with this row
     * i=1(2)| -        2/3      2/5
     * i=2(3)| -        -        3/5
     * 
     * Time Complexity: O(N + K log N)
     * Space Complexity: O(N) for the Priority Queue.
     */
    
    // State needed for the heap: numerator index i, denominator index j, and the decimal value
    private record HeapNode(int i, int j, double val) implements Comparable<HeapNode> {
        @Override
        public int compareTo(HeapNode other) {
            return Double.compare(this.val, other.val);
        }
    }

    public static int[] kthSmallestPrimeFractionMinHeap(int[] arr, int k) {
        int n = arr.length;
        var minHeap = new PriorityQueue<HeapNode>();

        // Step 1: Initialize the heap with the first fraction of every valid denominator
        for (int j = 1; j < n; j++) {
            minHeap.offer(new HeapNode(0, j, (double) arr[0] / arr[j]));
        }

        // Step 2: Extract min k-1 times
        for (int iter = 0; iter < k - 1; iter++) {
            var node = minHeap.poll();
            
            // If there's another numerator for this denominator, push it to the heap
            if (node.i() + 1 < node.j()) {
                int nextI = node.i() + 1;
                minHeap.offer(new HeapNode(nextI, node.j(), (double) arr[nextI] / arr[node.j()]));
            }
        }

        // The root is now the k-th smallest element
        var resultNode = minHeap.poll();
        return new int[]{arr[resultNode.i()], arr[resultNode.j()]};
    }

    /* ============================================================================
     * APPROACH 3: Optimal Binary Search on Value Range
     * ============================================================================
     * Explanation:
     * The fractions fall within the range (0.0, 1.0). We can binary search over 
     * this continuous range. For a chosen decimal 'mid', we use two pointers to 
     * efficiently count how many fractions arr[i]/arr[j] are <= mid.
     * 
     * Two Pointers Logic:
     * As the denominator arr[j] increases, the max numerator arr[i] that satisfies 
     * arr[i] / arr[j] <= mid also increases. Thus, 'i' only moves forward.
     * 
     * While counting, we also keep track of the maximum fraction that is <= mid.
     * If total count == k, the maximum fraction tracked is our exact answer.
     * 
     * Time Complexity: O(N log(Max_Range / epsilon))
     * Space Complexity: O(1) auxiliary space.
     */
    public static int[] kthSmallestPrimeFractionBinarySearch(int[] arr, int k) {
        int n = arr.length;
        double low = 0.0, high = 1.0;

        while (low < high) {
            double mid = low + (high - low) / 2.0;
            
            int count = 0;
            int num = 0, den = 1;
            double maxFrac = 0.0; // Keep track of the largest fraction <= mid
            
            int i = 0; // Numerator pointer
            
            for (int j = 1; j < n; j++) { // Denominator pointer
                // Move 'i' until arr[i] / arr[j] is no longer <= mid
                // (Cross multiplying: arr[i] <= mid * arr[j])
                while (i < j && arr[i] < mid * arr[j]) {
                    i++;
                }
                
                // Elements from 0 to i-1 are <= mid
                count += i;
                
                // Update the maximum fraction found so far <= mid
                if (i > 0) {
                    double currentFrac = (double) arr[i - 1] / arr[j];
                    if (currentFrac > maxFrac) {
                        maxFrac = currentFrac;
                        num = arr[i - 1];
                        den = arr[j];
                    }
                }
            }

            if (count == k) {
                return new int[]{num, den};
            } else if (count < k) {
                low = mid; // We need more fractions, increase 'mid'
            } else {
                high = mid; // We have too many fractions, decrease 'mid'
            }
        }
        
        return new int[]{};
    }

    /* ============================================================================
     * TESTING / MAIN METHOD
     * ============================================================================
     */
    
    // Record for structured test cases
    public record TestCase(int[] arr, int k, int[] expected) {}

    public static void main(String[] args) {
        var testCases = List.of(
            new TestCase(
                new int[]{1, 2, 3, 5}, 
                3, 
                new int[]{2, 5}
            ),
            new TestCase(
                new int[]{1, 7}, 
                1, 
                new int[]{1, 7}
            ),
            new TestCase(
                new int[]{1, 2, 3, 5, 7, 11}, 
                8, 
                new int[]{3, 7}
            )
        );

        System.out.println("Running tests for all 3 approaches...\n");

        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            System.out.println("Test Case " + (i + 1) + ": Array = " + Arrays.toString(tc.arr) + ", k = " + tc.k);
            
            var ans1 = kthSmallestPrimeFractionBruteForce(tc.arr, tc.k);
            var ans2 = kthSmallestPrimeFractionMinHeap(tc.arr, tc.k);
            var ans3 = kthSmallestPrimeFractionBinarySearch(tc.arr, tc.k);

            boolean pass1 = Arrays.equals(ans1, tc.expected);
            boolean pass2 = Arrays.equals(ans2, tc.expected);
            boolean pass3 = Arrays.equals(ans3, tc.expected);

            System.out.println("  Brute Force   : " + (pass1 ? "PASS" : "FAIL") + " -> Result: " + Arrays.toString(ans1));
            System.out.println("  Min-Heap      : " + (pass2 ? "PASS" : "FAIL") + " -> Result: " + Arrays.toString(ans2));
            System.out.println("  Binary Search : " + (pass3 ? "PASS" : "FAIL") + " -> Result: " + Arrays.toString(ans3));
            System.out.println("-".repeat(60));
        }
    }
}

class Solution {

    /**
     * Record to represent a fraction using indices.
     * 
     * i -> numerator index
     * j -> denominator index
     */
    record Fraction(int i, int j) {}

    public int[] kthSmallestPrimeFraction(int[] arr, int k) {

        int n = arr.length;

        /**
         * Min Heap (PriorityQueue)
         * 
         * We compare fractions WITHOUT converting to double
         * to avoid precision issues.
         * 
         * a/b < c/d  ⇒  a * d < c * b
         */
        PriorityQueue<Fraction> minHeap = new PriorityQueue<>(
            (f1, f2) -> Integer.compare(
                arr[f1.i] * arr[f2.j],
                arr[f2.i] * arr[f1.j]
            )
        );

        /**
         * Step 1: Initialize heap
         * 
         * For every denominator j, push the smallest fraction:
         * 
         * arr[0] / arr[j]
         * 
         * Why?
         * Because for a fixed j:
         * arr[0]/arr[j] < arr[1]/arr[j] < arr[2]/arr[j] ...
         * 
         * So each "column" is sorted.
         */
        for (int j = 1; j < n; j++) {
            minHeap.offer(new Fraction(0, j));
        }

        /**
         * Step 2: Extract k-1 smallest fractions
         * 
         * Each time:
         * - Remove smallest fraction
         * - Move to next fraction in same column
         * 
         * Think like merging sorted lists:
         * Each denominator j is a sorted list:
         * [arr[0]/arr[j], arr[1]/arr[j], ...]
         */
        for (int count = 1; count < k; count++) {

            Fraction smallest = minHeap.poll();

            int i = smallest.i();
            int j = smallest.j();

            /**
             * Move to next numerator in same column
             * 
             * Current: arr[i] / arr[j]
             * Next:    arr[i+1] / arr[j]
             */
            if (i + 1 < j) { // ensure valid fraction (i < j)
                minHeap.offer(new Fraction(i + 1, j));
            }
        }

        /**
         * Step 3: Top of heap is kth smallest fraction
         */
        Fraction result = minHeap.peek();

        return new int[]{arr[result.i()], arr[result.j()]};
    }
}
