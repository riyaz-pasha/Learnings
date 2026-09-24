import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * PROBLEM STATEMENT:
 * Given an array of points where each element points[i] = [xi, yi] represents a point on the X-Y plane, 
 * along with an integer k. Find and return the k points that are closest to the origin [0, 0].
 * 
 * The distance between two points on the X-Y plane is measured using Euclidean distance:
 * sqrt((x2 - x1)^2 + (y2 - y1)^2).
 *
 * CONSTRAINTS:
 * 1 <= k <= points.length <= 10^3
 * -10^4 <= xi, yi <= 10^4
 * 
 * ==========================================================================================
 * CRITICAL INSIGHT - DISTANCE CALCULATION:
 * Since we are always comparing distances to the origin (0,0), the formula simplifies to:
 * sqrt(x^2 + y^2).
 * Furthermore, square root is a monotonically increasing function. 
 * This means if A^2 < B^2, then sqrt(A^2) < sqrt(B^2).
 * Thus, we can safely ignore the expensive Math.sqrt() operation and just compare 
 * the squared Euclidean distances: (x*x + y*y). 
 * Also, we use 'long' to prevent integer overflow since 10^4 * 10^4 + 10^4 * 10^4 = 200,000,000, 
 * which fits in int, but it's a good practice to prevent bugs if constraints increase.
 * ==========================================================================================
 * LATEST JAVA FEATURES USED:
 * - Records (`record TestCase(...)`): For defining clean immutable test scenarios.
 * - Local Variable Type Inference (`var`): To reduce boilerplate while keeping type safety.
 * - Comparator factory methods and Lambdas: For concise sorting logic.
 * ==========================================================================================
 */
class KClosestPointsToOrigin {

    // Helper method to calculate squared distance
    private static long squaredDistance(int[] point) {
        return (long) point[0] * point[0] + (long) point[1] * point[1];
    }

    // ==========================================================================================
    // SOLUTION 1: Sorting (The Brute-Force / Intuitive Way)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * 1. Sort the entire array of points based on their squared distance from the origin.
     * 2. The points with the smallest distances will be at the beginning of the array.
     * 3. Simply copy the first 'k' elements into a new result array and return them.
     *
     * VISUAL:
     * points = [[3, 3], [5, -1], [-2, 4]], k = 2
     * 
     * Calculate squared distances:
     * [3, 3]   -> 3^2 + 3^2 = 18
     * [5, -1]  -> 5^2 + (-1)^2 = 26
     * [-2, 4]  -> (-2)^2 + 4^2 = 20
     * 
     * Sort by distances:
     * [[3, 3] (18), [-2, 4] (20), [5, -1] (26)]
     * 
     * Take first k=2:
     * [[3, 3], [-2, 4]]
     *
     * COMPLEXITY:
     * - Time: O(N log N) - sorting the entire array of N points.
     * - Space: O(1) auxiliary or O(N) depending on Arrays.sort implementation for objects.
     */
    public static int[][] kClosest_Sorting(int[][] points, int k) {
        var copy = points.clone(); // Clone to prevent modifying the original array
        
        Arrays.sort(copy, Comparator.comparingLong(KClosestPointsToOrigin::squaredDistance));
        
        return Arrays.copyOfRange(copy, 0, k);
    }

    // ==========================================================================================
    // SOLUTION 2: Max-Heap (Optimal for Streaming Data / Large N, Small k)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * Since we want the 'k' CLOSEST (smallest distance) points, we use a MAX-Heap of size 'k'.
     * The Max-Heap keeps the point with the LARGEST distance among the top k closest seen so far at its root.
     * As we iterate through the points:
     * 1. Add the current point to the Max-Heap.
     * 2. If the heap size exceeds 'k', remove the root.
     * 3. Removing the root throws away the point that is farthest away, ensuring our heap 
     *    always contains the 'k' closest points we've encountered.
     *
     * VISUAL:
     * points = [[3,3] (d=18), [5,-1] (d=26), [-2,4] (d=20)], k = 2
     * 
     * Add [3, 3]:   Heap -> [[3,3]] 
     * Add [5, -1]:  Heap -> [[5,-1], [3,3]]  (Max distance 26 is root)
     * Add [-2, 4]:  Heap -> [[5,-1], [-2,4], [3,3]] -> Size > 2! Poll root ([5,-1]).
     *               Heap -> [[-2,4], [3,3]]
     * 
     * Return Heap elements.
     *
     * COMPLEXITY:
     * - Time: O(N log k) - We add N elements, each takes O(log k) to insert/remove.
     * - Space: O(k) - Max-Heap stores at most k elements.
     */
    public static int[][] kClosest_MaxHeap(int[][] points, int k) {
        // Max-Heap: Compare in reverse order (b compared to a)
        PriorityQueue<int[]> maxHeap = new PriorityQueue<>(
            (a, b) -> Long.compare(squaredDistance(b), squaredDistance(a))
        );

        for (var point : points) {
            maxHeap.offer(point);
            if (maxHeap.size() > k) {
                maxHeap.poll(); // Evict the point farthest from the origin
            }
        }

        var result = new int[k][2];
        for (int i = 0; i < k; i++) {
            result[i] = maxHeap.poll();
        }
        return result;
    }

    // ==========================================================================================
    // SOLUTION 3: QuickSelect (Optimal Average Time)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * We can adapt QuickSort's partitioning logic to partially sort the array.
     * We pick a random pivot point and partition the array such that all points closer to the origin 
     * than the pivot are placed to its left, and farther points to its right.
     * - If the pivot ends up exactly at index `k`, the first `k` elements are our closest points!
     * - If the pivot is less than `k`, we recursively QuickSelect the right side.
     * - If the pivot is greater than `k`, we recursively QuickSelect the left side.
     * The order within the top 'k' doesn't matter according to the problem constraints.
     *
     * COMPLEXITY:
     * - Time: O(N) average case. O(N^2) worst case (virtually impossible with randomized pivot).
     * - Space: O(log N) average stack depth for recursion.
     */
    public static int[][] kClosest_QuickSelect(int[][] points, int k) {
        var copy = points.clone();
        quickSelect(copy, 0, copy.length - 1, k, new Random());
        return Arrays.copyOfRange(copy, 0, k);
    }

    private static void quickSelect(int[][] points, int left, int right, int k, Random random) {
        if (left >= right) return;

        int pivotIndex = left + random.nextInt(right - left + 1);
        pivotIndex = partition(points, left, right, pivotIndex);

        if (pivotIndex == k) {
            return; // Exactly k elements are smaller
        } else if (pivotIndex < k) {
            quickSelect(points, pivotIndex + 1, right, k, random);
        } else {
            quickSelect(points, left, pivotIndex - 1, k, random);
        }
    }

    private static int partition(int[][] points, int left, int right, int pivotIndex) {
        long pivotDist = squaredDistance(points[pivotIndex]);
        swap(points, pivotIndex, right); // move pivot to end
        
        int storeIndex = left;
        for (int i = left; i < right; i++) {
            if (squaredDistance(points[i]) < pivotDist) {
                swap(points, storeIndex, i);
                storeIndex++;
            }
        }
        swap(points, storeIndex, right); // restore pivot to final place
        return storeIndex;
    }

    private static void swap(int[][] points, int i, int j) {
        int[] temp = points[i];
        points[i] = points[j];
        points[j] = temp;
    }

    // ==========================================================================================
    // TESTING SUITE
    // ==========================================================================================
    
    // Using a record to neatly encapsulate test cases
    public record TestCase(int[][] points, int k, int[][] expected) {}

    // Helper method to check if two 2D arrays contain the same elements (ignoring order)
    private static boolean areArraysEqualUnordered(int[][] arr1, int[][] arr2) {
        if (arr1.length != arr2.length) return false;
        
        Comparator<int[]> cmp = (a, b) -> {
            if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
            return Integer.compare(a[1], b[1]);
        };
        
        var copy1 = arr1.clone();
        var copy2 = arr2.clone();
        Arrays.sort(copy1, cmp);
        Arrays.sort(copy2, cmp);
        
        for (int i = 0; i < copy1.length; i++) {
            if (copy1[i][0] != copy2[i][0] || copy1[i][1] != copy2[i][1]) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        var testCases = new TestCase[] {
            new TestCase(new int[][]{{1, 3}, {-2, 2}}, 1, new int[][]{{-2, 2}}),
            new TestCase(new int[][]{{3, 3}, {5, -1}, {-2, 4}}, 2, new int[][]{{3, 3}, {-2, 4}}),
            new TestCase(new int[][]{{0, 1}, {1, 0}}, 2, new int[][]{{0, 1}, {1, 0}}),
            new TestCase(new int[][]{{2, 2}, {2, 2}, {3, 3}}, 2, new int[][]{{2, 2}, {2, 2}})
        };

        System.out.println("Running K Closest Points Tests...\n");

        for (int i = 0; i < testCases.length; i++) {
            var tc = testCases[i];
            System.out.printf("Test Case %d:\n", i + 1);
            System.out.printf("Points: %s | k: %d\n", Arrays.deepToString(tc.points()), tc.k());

            var res1 = kClosest_Sorting(tc.points(), tc.k());
            var res2 = kClosest_MaxHeap(tc.points(), tc.k());
            var res3 = kClosest_QuickSelect(tc.points(), tc.k());

            boolean pass1 = areArraysEqualUnordered(res1, tc.expected());
            boolean pass2 = areArraysEqualUnordered(res2, tc.expected());
            boolean pass3 = areArraysEqualUnordered(res3, tc.expected());

            System.out.printf("  [1. Sorting    ] Result: %s -> %s\n", Arrays.deepToString(res1), pass1 ? "PASS" : "FAIL");
            System.out.printf("  [2. Max-Heap   ] Result: %s -> %s\n", Arrays.deepToString(res2), pass2 ? "PASS" : "FAIL");
            System.out.printf("  [3. QuickSelect] Result: %s -> %s\n", Arrays.deepToString(res3), pass3 ? "PASS" : "FAIL");
            System.out.println("-".repeat(60));
        }
    }
}


class Solution {

    public int[][] kClosest(int[][] points, int k) {

        /*
         * Max Heap based on distance
         *
         * Comparator:
         * - comparingLong(this::squaredDistance) → min-heap by default
         * - reversed() → converts it into a max-heap
         *
         * So the point with the LARGEST distance stays at the top.
         */
        PriorityQueue<int[]> maxHeap = new PriorityQueue<>(
                Comparator.comparingLong(this::squaredDistance).reversed()
        );

        /*
         * Step 1: Iterate through all points
         */
        for (int[] point : points) {

            // Add point into heap
            maxHeap.offer(point);

            /*
             * If heap size exceeds k,
             * remove the farthest point (top of max heap)
             */
            if (maxHeap.size() > k) {
                maxHeap.poll();
            }
        }

        /*
         * Step 2: Extract k closest points
         */
        int[][] result = new int[k][2];

        for (int i = 0; i < k; i++) {
            result[i] = maxHeap.poll(); // order doesn't matter
        }

        return result;
    }

    /*
     * Helper method: squared distance from origin
     *
     * Why squared?
     * - Avoid sqrt (faster)
     * - Order remains same
     *
     * Why long?
     * - Prevent overflow when squaring values
     */
    private long squaredDistance(int[] point) {
        return (long) point[0] * point[0]
             + (long) point[1] * point[1];
    }
}
