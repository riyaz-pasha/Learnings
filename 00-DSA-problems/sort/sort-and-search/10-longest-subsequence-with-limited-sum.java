/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an array of numbers `nums` and a list of `queries`. 
 * For each query, we want to know the maximum number of elements we can pick 
 * from `nums` (as a subsequence/subset) such that their total sum is less than 
 * or equal to the query's value. 
 * We must return an array containing the answer for each query.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the elements in `nums` be zero or negative? 
 *    (Constraints say nums[i] >= 1. This means the sum strictly increases as we add elements.)
 * 2. Does the order of the subsequence matter? 
 *    (No, because addition is commutative. Any subset of elements is valid.)
 * 3. Can the total sum of `nums` exceed standard integer limits?
 *    (Max N = 1000, Max nums[i] = 10^5. Total max sum = 10^8. Fits easily in a 32-bit `int`).
 * 4. Can the query value be smaller than the smallest element in `nums`?
 *    (Yes, in which case the answer for that query should be 0).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Greedy Property (Key Observation):
 *    To maximize the *number* of elements we pick without exceeding a sum, 
 *    we should always pick the *smallest* elements available. 
 *    Therefore, sorting the `nums` array is the critical first step.
 * 
 * 2. Prefix Sums:
 *    Once sorted, if we sequentially add elements, we get a running total. 
 *    We can store this in a prefix sum array. 
 *    Example: nums = [4, 5, 2, 1] -> Sorted = [1, 2, 4, 5] -> Prefix = [1, 3, 7, 12].
 *    `prefix[i]` tells us the sum of the smallest `i + 1` elements.
 * 
 * 3. Answering Queries:
 *    - Linear Search (Brute Force): For each query, iterate through the prefix 
 *      array until the sum exceeds the query. Time: O(M * N).
 *    - Binary Search (Optimal): Since all numbers are positive, the prefix sum 
 *      array is strictly increasing. We can use binary search to quickly find 
 *      how many elements fit into the query's budget. Time: O(M log N).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Binary Search Approach)
 * ============================================================================
 * Example: nums = [4, 5, 2, 1], queries = [3, 10, 21]
 * 
 * 1. Sort nums -> [1, 2, 4, 5]
 * 2. Prefix Sum -> [1, 3, 7, 12]
 * 
 * 3. Query 1: q = 3
 *    Binary search for 3 in [1, 3, 7, 12].
 *    Matches exactly at index 1.
 *    Number of elements picked = index + 1 = 2 (elements: 1, 2).
 * 
 * 4. Query 2: q = 10
 *    Binary search for 10 in [1, 3, 7, 12].
 *    10 falls between 7 (index 2) and 12 (index 3).
 *    Insertion point is index 3. 
 *    Number of elements picked = 3 (elements: 1, 2, 4).
 * 
 * 5. Query 3: q = 21
 *    Binary search for 21. It's larger than all elements.
 *    Insertion point is index 4.
 *    Number of elements picked = 4 (all elements).
 * 
 * Result array = [2, 3, 4]
 */

import java.util.Arrays;

public class LongestSubsequenceWithLimitedSum {

    public static void main(String[] args) {
        int[] nums = {4, 5, 2, 1};
        int[] queries = {3, 10, 21};

        System.out.println("Nums: " + Arrays.toString(nums));
        System.out.println("Queries: " + Arrays.toString(queries));

        System.out.println("Solution 1 (Prefix + Linear): " 
            + Arrays.toString(answerQueriesLinear(nums.clone(), queries)));
            
        System.out.println("Solution 2 (Prefix + Binary Search): " 
            + Arrays.toString(answerQueriesBinarySearch(nums.clone(), queries)));
            
        System.out.println("Solution 3 (Java Streams): " 
            + Arrays.toString(answerQueriesStreams(nums.clone(), queries)));
    }

    /**
     * SOLUTION 1: Sorting + Prefix Sum + Linear Search
     * 
     * Idea: Sort the array, then for each query, just add numbers up until 
     * they exceed the query amount. 
     * 
     * Time Complexity: O(N log N + M * N) - Good enough since N, M <= 1000.
     * Space Complexity: O(1) or O(N) depending on in-place sorting.
     */
    public static int[] answerQueriesLinear(int[] nums, int[] queries) {
        Arrays.sort(nums);
        int m = queries.length;
        int[] answer = new int[m];
        
        for (int i = 0; i < m; i++) {
            int currentSum = 0;
            int count = 0;
            
            for (int num : nums) {
                if (currentSum + num <= queries[i]) {
                    currentSum += num;
                    count++;
                } else {
                    break;
                }
            }
            answer[i] = count;
        }
        
        return answer;
    }

    /**
     * SOLUTION 2: Sorting + Prefix Sum + Binary Search (Optimal)
     * 
     * Idea: Build a prefix sum array. Since it is strictly increasing, use 
     * Arrays.binarySearch to locate the maximum number of elements for each query 
     * in O(log N) time per query.
     * 
     * Time Complexity: O(N log N + M log N) - Faster for larger constraints.
     * Space Complexity: O(1) if reusing nums for prefix sum, else O(N).
     */
    public static int[] answerQueriesBinarySearch(int[] nums, int[] queries) {
        // 1. Sort the numbers to greedily pick the smallest first
        Arrays.sort(nums);
        
        // 2. Convert nums into a prefix sum array in-place
        for (int i = 1; i < nums.length; i++) {
            nums[i] += nums[i - 1];
        }
        
        int[] answer = new int[queries.length];
        
        // 3. Answer each query using binary search
        for (int i = 0; i < queries.length; i++) {
            // Arrays.binarySearch returns index of the exact match, or 
            // -(insertion point) - 1 if not found.
            int idx = Arrays.binarySearch(nums, queries[i]);
            
            if (idx >= 0) {
                // Exact match found. Since it's 0-indexed, length is idx + 1
                answer[i] = idx + 1;
            } else {
                // If not found, insertion point represents the number of elements
                // strictly smaller than the target.
                int insertionPoint = Math.abs(idx + 1);
                answer[i] = insertionPoint;
            }
        }
        
        return answer;
    }

    /**
     * SOLUTION 3: Modern Java Approach (Streams)
     * 
     * Idea: Wraps the exact same Binary Search logic in a declarative Java Stream,
     * demonstrating fluency with modern APIs.
     * 
     * Time Complexity: O(N log N + M log N)
     * Space Complexity: O(M) for stream output array.
     */
    public static int[] answerQueriesStreams(int[] nums, int[] queries) {
        Arrays.sort(nums);
        
        // Transform to prefix sum
        for (int i = 1; i < nums.length; i++) {
            nums[i] += nums[i - 1];
        }
        
        // Map queries to answers cleanly using streams
        return Arrays.stream(queries)
                     .map(query -> {
                         int idx = Arrays.binarySearch(nums, query);
                         return idx >= 0 ? idx + 1 : Math.abs(idx + 1);
                     })
                     .toArray();
    }
}
