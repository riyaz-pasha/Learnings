import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * PROBLEM STATEMENT:
 * You are given an integer array, nums, and a positive integer k. 
 * Your task is to determine and return the kth largest possible sum among all subsequences of the array.
 * 
 * Remember: For valid subsequences:
 * - The empty subsequence is valid, and its sum is considered 0.
 * - Duplicate subsequence sums are allowed and counted separately.
 *
 * CONSTRAINTS:
 * 1 <= n <= 10^3
 * -10^3 <= nums[i] <= 10^3
 * 1 <= k <= min(1000, 2^n)
 * 
 * ==========================================================================================
 * CRITICAL INSIGHT - REDUCING THE PROBLEM:
 * Generating all subsequences is O(2^n), which is impossible for N = 1000.
 * Instead, observe how the MAXIMAL sum is formed:
 * - The absolute maximum subsequence sum is simply the sum of all strictly POSITIVE numbers in the array.
 * 
 * Any other subsequence sum is strictly lesser than or equal to this maximal sum.
 * How do we get smaller sums?
 * 1. By EXCLUDING a positive number (penalty of its value).
 * 2. By INCLUDING a negative number (penalty of its absolute value).
 * 
 * Therefore, EVERY choice (include negative or exclude positive) incurs a penalty equal to 
 * the ABSOLUTE VALUE of that element.
 * 
 * The problem transforms beautifully to:
 * 1. Calculate the maximum possible sum (sum of all positive elements).
 * 2. Take the absolute values of all elements in `nums` and sort them in ascending order.
 * 3. Find the k-th SMALLEST subsequence sum of these absolute values (penalties).
 * 4. Return (Maximum Sum - k-th Smallest Penalty).
 * ==========================================================================================
 * LATEST JAVA FEATURES USED:
 * - Records (`record State(...)`, `record TestCase(...)`): For clean, immutable data carriers.
 * - Local Variable Type Inference (`var`): Cleaner syntax.
 * ==========================================================================================
 */
class KthLargestSubsequenceSum {

    // ==========================================================================================
    // SOLUTION 1: Brute Force (Educational Stepping Stone - Fails for Large N)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * Generates all 2^n possible subsequence sums recursively, stores them in a list, 
     * sorts them in descending order, and returns the k-th element.
     * 
     * COMPLEXITY:
     * - Time: O(2^n * log(2^n)) - Extremely slow. Will timeout for n > 20.
     * - Space: O(2^n) - To store all possible sums.
     */
    public static long kthLargestSum_BruteForce(int[] nums, int k) {
        if (nums.length > 20) {
            System.out.println("    [Brute Force] Skipping because N > 20 (would cause OutOfMemory/Timeout)");
            return -1;
        }
        var allSums = new ArrayList<Long>();
        generateSubsequences(nums, 0, 0, allSums);
        allSums.sort(Collections.reverseOrder());
        return allSums.get(k - 1);
    }

    private static void generateSubsequences(int[] nums, int idx, long currentSum, List<Long> allSums) {
        if (idx == nums.length) {
            allSums.add(currentSum);
            return;
        }
        // Exclude current element
        generateSubsequences(nums, idx + 1, currentSum, allSums);
        // Include current element
        generateSubsequences(nums, idx + 1, currentSum + nums[idx], allSums);
    }

    // ==========================================================================================
    // SOLUTION 2: Min-Heap / Dijkstra-like Approach (Optimal)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * After transforming the array into absolute values and sorting ascending, we need 
     * the k-th smallest subsequence sum (penalty).
     * We use a Min-Heap starting with the smallest non-empty penalty `A[0]`.
     * To find the NEXT smallest penalty, we pop `(current_sum, index)` and branch into two options:
     * 1. INCLUDE the next element: `current_sum + A[index + 1]`
     * 2. REPLACE the current element with the next element: `current_sum - A[index] + A[index + 1]`
     * 
     * Because A is sorted, both branches are guaranteed to yield a sum >= current_sum!
     * 
     * VISUAL:
     * nums = [2, 4, -2], k = 3
     * 
     * Max Sum = 2 + 4 = 6.
     * Abs array A = [2, 4, 2] -> Sorted A = [2, 2, 4]
     * 
     * k=1: Penalty = 0. Output: 6 - 0 = 6. (Empty penalty)
     * Heap Init: [ (sum: 2, idx: 0) ]
     * 
     * Loop 1 (Find 2nd smallest):
     *   Pop (2, 0). 
     *   Push INCLUDE: (2 + A[1]=4, 1) -> (4, 1)
     *   Push REPLACE: (2 - 2 + A[1]=2, 1) -> (2, 1)
     *   Heap: [ (2, 1), (4, 1) ]
     * 
     * Loop 2 (Find 3rd smallest):
     *   Pop (2, 1).
     *   Push INCLUDE: (2 + A[2]=6, 2) -> (6, 2)
     *   Push REPLACE: (2 - 2 + A[2]=4, 2) -> (4, 2)
     *   Heap: [ (4, 1), (4, 2), (6, 2) ]
     * 
     * Loop 2 finishes. The 3rd smallest penalty popped was 2.
     * Result = Max Sum (6) - Penalty (2) = 4.
     *
     * COMPLEXITY:
     * - Time: O(N log N + k log k) - N log N to sort, k log k for extracting from heap.
     * - Space: O(N + k) - N for transformed array, k for heap.
     */
    
    // State record to hold the sum of the penalty and the index of the last element added
    private record State(long sum, int index) implements Comparable<State> {
        @Override
        public int compareTo(State other) {
            return Long.compare(this.sum, other.sum);
        }
    }

    public static long kthLargestSum_MinHeap(int[] nums, int k) {
        long maxSum = 0;
        var absVals = new int[nums.length];
        
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] > 0) {
                maxSum += nums[i];
            }
            absVals[i] = Math.abs(nums[i]);
        }
        
        Arrays.sort(absVals);
        
        // 1st largest sum means 0 penalty (the empty subsequence of penalties)
        if (k == 1) return maxSum;
        
        PriorityQueue<State> minHeap = new PriorityQueue<>();
        minHeap.offer(new State(absVals[0], 0));
        
        long kthSmallestPenalty = 0;
        
        // We need the k-th smallest. The 1st is 0. We pop k-1 times to find it.
        for (int step = 1; step < k; step++) {
            State curr = minHeap.poll();
            kthSmallestPenalty = curr.sum();
            
            if (curr.index() + 1 < absVals.length) {
                // Option 1: Include the next element in the penalty
                minHeap.offer(new State(curr.sum() + absVals[curr.index() + 1], curr.index() + 1));
                
                // Option 2: Replace the current element with the next element
                minHeap.offer(new State(curr.sum() - absVals[curr.index()] + absVals[curr.index() + 1], curr.index() + 1));
            }
        }
        
        return maxSum - kthSmallestPenalty;
    }

    // ==========================================================================================
    // SOLUTION 3: Binary Search + DFS (Alternative Optimal)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * Using the same absolute value sorted array, we can binary search the exact penalty value.
     * The lowest possible penalty is 0, the highest is the sum of all absolute values.
     * For a given `mid` penalty, we use a DFS to COUNT how many subsequences have a penalty <= mid.
     * If count >= k, our mid is too large (or just right), so we search lower.
     * If count < k, we search higher.
     *
     * COMPLEXITY:
     * - Time: O(N log N + k * log(Sum of abs(nums))) - The DFS prunes aggressively and never visits more than k valid states per binary search step.
     * - Space: O(N) for recursion stack.
     */
    public static long kthLargestSum_BinarySearch(int[] nums, int k) {
        long maxSum = 0;
        long totalAbsSum = 0;
        var absVals = new int[nums.length];
        
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] > 0) {
                maxSum += nums[i];
            }
            absVals[i] = Math.abs(nums[i]);
            totalAbsSum += absVals[i];
        }
        
        Arrays.sort(absVals);
        
        long low = 0;
        long high = totalAbsSum;
        long kthPenalty = 0;
        
        while (low <= high) {
            long mid = low + (high - low) / 2;
            long[] count = new long[]{1}; // Array to allow reference modification in DFS. 1 represents the empty subsequence.
            
            countSubsequences(absVals, k, mid, 0, 0, count);
            
            if (count[0] >= k) {
                kthPenalty = mid;
                high = mid - 1; // Try to find a smaller valid penalty
            } else {
                low = mid + 1;
            }
        }
        
        return maxSum - kthPenalty;
    }
    
    private static void countSubsequences(int[] A, int k, long limit, int idx, long currentSum, long[] count) {
        if (count[0] >= k) return; // Prune: we only care if count reaches k
        
        for (int i = idx; i < A.length; i++) {
            if (currentSum + A[i] > limit) {
                break; // Because array is sorted, subsequent elements will also exceed the limit
            }
            count[0]++;
            countSubsequences(A, k, limit, i + 1, currentSum + A[i], count);
            if (count[0] >= k) return;
        }
    }

    // ==========================================================================================
    // TESTING SUITE
    // ==========================================================================================
    
    public record TestCase(int[] nums, int k, long expected) {}

    public static void main(String[] args) {
        var testCases = new TestCase[] {
            new TestCase(new int[]{2, 4, -2}, 1, 6),
            new TestCase(new int[]{2, 4, -2}, 2, 4),
            new TestCase(new int[]{2, 4, -2}, 3, 4),
            new TestCase(new int[]{2, 4, -2}, 8, -2), // Smallest possible sum
            new TestCase(new int[]{1, -2, 3, 4, -10, 12}, 10, 16),
            new TestCase(new int[]{1000, 1000, 1000, 1000}, 5, 3000) 
        };

        System.out.println("Running K-th Largest Subsequence Sum Tests...\n");

        for (int i = 0; i < testCases.length; i++) {
            var tc = testCases[i];
            System.out.printf("Test Case %d:\n", i + 1);
            System.out.printf("Array: %s | k: %d | Expected: %d\n", Arrays.toString(tc.nums()), tc.k(), tc.expected());

            // 1. Brute Force
            if (tc.nums().length <= 20) {
                long res1 = kthLargestSum_BruteForce(tc.nums(), tc.k());
                boolean pass1 = (res1 == tc.expected());
                System.out.printf("  [1. Brute Force   ] Result: %d -> %s\n", res1, pass1 ? "PASS" : "FAIL");
            }

            // 2. Min-Heap
            long res2 = kthLargestSum_MinHeap(tc.nums(), tc.k());
            boolean pass2 = (res2 == tc.expected());
            System.out.printf("  [2. Min-Heap      ] Result: %d -> %s\n", res2, pass2 ? "PASS" : "FAIL");

            // 3. Binary Search
            long res3 = kthLargestSum_BinarySearch(tc.nums(), tc.k());
            boolean pass3 = (res3 == tc.expected());
            System.out.printf("  [3. Binary Search ] Result: %d -> %s\n", res3, pass3 ? "PASS" : "FAIL");

            System.out.println("-".repeat(70));
        }
    }
}

/**
 * Problem:
 * Find the k-th largest subsequence sum.
 *
 * Key Transformation:
 * -------------------
 * Instead of directly finding k-th largest sum (hard: 2^n subsequences),
 * we convert it into:
 *
 *    k-th largest sum = maxSum - k-th smallest "loss"
 *
 * where:
 *    maxSum = sum of all positive numbers
 *    loss = sum of absolute values of elements we "remove"
 *
 * Why abs?
 * --------
 * - Skipping a positive number => lose its value
 * - Including a negative number => also reduces sum
 * => both contribute as "loss"
 * => treat everything as abs(nums[i])
 *
 * Now problem becomes:
 * --------------------
 * Find k-th smallest subset sum of abs[]
 *
 * This can be solved using a Min Heap (like Dijkstra / merging sorted lists).
 */
class Solution {

    /**
     * Record to store state in heap
     *
     * sum  -> current subset sum (loss)
     * index -> last index used in abs[]
     */
    record State(long sum, int index) {}

    public long kSum(int[] nums, int k) {

        int n = nums.length;

        // ------------------------------------------------------------
        // STEP 1: Compute maxSum and convert nums -> abs array
        // ------------------------------------------------------------
        long maxSum = 0;
        long[] abs = new long[n];

        for (int i = 0; i < n; i++) {
            if (nums[i] > 0) {
                maxSum += nums[i]; // take all positives
            }
            abs[i] = Math.abs(nums[i]); // convert to "loss"
        }

        // ------------------------------------------------------------
        // STEP 2: Sort abs array
        // ------------------------------------------------------------
        // Why sorting?
        // Because we want to generate subset sums in increasing order
        Arrays.sort(abs);

        // ------------------------------------------------------------
        // STEP 3: Min Heap to generate k smallest subset sums
        // ------------------------------------------------------------
        PriorityQueue<State> minHeap =
                new PriorityQueue<>(Comparator.comparingLong(State::sum));

        /**
         * Important:
         * ----------
         * We DO NOT push 0 explicitly.
         * Instead:
         * - 0th smallest = 0 (empty subset)
         * - We start from first element: abs[0]
         */
        minHeap.offer(new State(abs[0], 0));

        long kthSmallestLoss = 0; // this will track k-th smallest subset sum

        // ------------------------------------------------------------
        // STEP 4: Extract k-1 smallest subset sums
        // ------------------------------------------------------------
        // Why k-1?
        // Because:
        // 1st smallest = 0 (empty subset)
        // We already "skip" it logically
        for (int i = 1; i < k; i++) {

            State current = minHeap.poll();

            long sum = current.sum;
            int index = current.index;

            kthSmallestLoss = sum;

            // --------------------------------------------------------
            // Generate next possible subset sums
            // --------------------------------------------------------
            if (index + 1 < n) {

                /**
                 * Case 1: Replace current element with next element
                 *
                 * Example:
                 * current subset uses abs[index]
                 * Now replace it with abs[index+1]
                 *
                 * sum - abs[index] + abs[index+1]
                 */
                minHeap.offer(new State(
                        sum - abs[index] + abs[index + 1],
                        index + 1
                ));

                /**
                 * Case 2: Add next element (extend subset)
                 *
                 * Example:
                 * current subset + abs[index+1]
                 */
                minHeap.offer(new State(
                        sum + abs[index + 1],
                        index + 1
                ));
            }
        }

        // ------------------------------------------------------------
        // STEP 5: Convert back to original answer
        // ------------------------------------------------------------
        return maxSum - kthSmallestLoss;
    }
}
