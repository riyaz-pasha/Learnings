/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an array of integers representing the number of citations a 
 * researcher has received for each of their publications.
 * We need to calculate the researcher's h-index.
 * The h-index is the largest number 'h' such that the researcher has at least 
 * 'h' papers that each have been cited at least 'h' times.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the citations array be empty? 
 *    (Constraints say length >= 1, but always good to confirm).
 * 2. Can a paper have 0 citations? 
 *    (Yes, constraints say citations[i] >= 0).
 * 3. What if all papers have 0 citations?
 *    (The h-index should be 0).
 * 4. Can the number of citations exceed the total number of papers?
 *    (Yes, e.g., 1 paper with 100 citations. The h-index would be 1, because 
 *     you cannot have an h-index higher than your total number of publications).
 * 5. Are we allowed to modify the input array?
 *    (Sorting it in place might be a good start. If not allowed, we can clone it).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * Key Observation: 
 * The h-index can NEVER be larger than 'n' (the total number of papers). 
 * Even if a researcher has 3 papers with 10,000 citations each, their h-index 
 * is only 3, because they only published 3 papers!
 * 
 * 1. Sorting Approach (Intuitive):
 *    - Sort the citations array in ascending order.
 *    - Iterate through the sorted array. At index `i`, there are exactly `n - i` 
 *      papers with citations greater than or equal to `citations[i]`.
 *    - If `citations[i] >= n - i`, it means we have found at least `n - i` papers 
 *      with at least `n - i` citations. Since we traverse from left to right 
 *      (smaller to larger citations), the first time this condition is met gives 
 *      us the maximum possible h-index.
 *    - Time: O(N log N), Space: O(1) or O(N) depending on sorting implementation.
 * 
 * 2. Counting Sort / Bucket Approach (Optimal O(N)):
 *    - Since the h-index is bounded by `n`, we don't care about the exact 
 *      citation count if it's strictly greater than `n`. Any citation count > n 
 *      can be treated as just `n`.
 *    - Create a frequency array (bucket) of size `n + 1`.
 *    - Iterate through the citations and populate the frequencies. If a citation 
 *      is >= n, increment `bucket[n]`. Otherwise, increment `bucket[citation]`.
 *    - Iterate backwards from `n` down to 0, keeping a running sum of the papers.
 *    - The first time the running sum of papers is greater than or equal to the 
 *      current bucket index `h`, we have found our h-index.
 *    - Time: O(N), Space: O(N).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Bucket Approach)
 * ============================================================================
 * Example: citations = [3, 0, 6, 1, 5], n = 5
 * 
 * 1. Initialize buckets of size 6 (0 to 5):
 *    buckets = [0, 0, 0, 0, 0, 0]
 * 
 * 2. Populate buckets:
 *    - 3 -> buckets[3]++
 *    - 0 -> buckets[0]++
 *    - 6 -> (6 > 5) -> buckets[5]++
 *    - 1 -> buckets[1]++
 *    - 5 -> buckets[5]++
 *    Resulting buckets = [1, 1, 0, 1, 0, 2]
 *    (This means: 1 paper with 0 cites, 1 paper with 1 cite, 0 with 2, 1 with 3, 
 *     0 with 4, and 2 with >= 5 cites).
 * 
 * 3. Iterate backwards, accumulating the number of papers:
 *    - h = 5: count = 2. Is count (2) >= h (5)? False.
 *    - h = 4: count = 2 + 0 = 2. Is count (2) >= h (4)? False.
 *    - h = 3: count = 2 + 1 = 3. Is count (3) >= h (3)? TRUE! 
 * 
 * Return 3.
 */

import java.util.Arrays;

public class HIndex {

    public static void main(String[] args) {
        int[] citations1 = {3, 0, 6, 1, 5};
        int[] citations2 = {1, 3, 1};
        int[] citations3 = {100, 200, 300}; // Few papers, huge citations

        System.out.println("Input: [3, 0, 6, 1, 5]");
        System.out.println("Solution 1 (Sorting): " + hIndexSorting(citations1.clone()));
        System.out.println("Solution 2 (Buckets): " + hIndexCounting(citations1.clone()));
        System.out.println("--------------------------------------------------");

        System.out.println("Input: [1, 3, 1]");
        System.out.println("Solution 1 (Sorting): " + hIndexSorting(citations2.clone()));
        System.out.println("Solution 2 (Buckets): " + hIndexCounting(citations2.clone()));
        System.out.println("--------------------------------------------------");

        System.out.println("Input: [100, 200, 300]");
        System.out.println("Solution 1 (Sorting): " + hIndexSorting(citations3.clone()));
        System.out.println("Solution 2 (Buckets): " + hIndexCounting(citations3.clone()));
    }

    /**
     * SOLUTION 1: Sorting
     * 
     * Idea: Sort the array. The number of papers with at least citations[i] 
     * citations is exactly `n - i`. We find the first instance where the 
     * citation count is at least `n - i`.
     * 
     * Time Complexity: O(N log N) - Dominated by Arrays.sort()
     * Space Complexity: O(1) or O(N) depending on sort implementation.
     */
    public static int hIndexSorting(int[] citations) {
        if (citations == null || citations.length == 0) return 0;

        Arrays.sort(citations);
        int n = citations.length;

        for (int i = 0; i < n; i++) {
            int papersWithAtLeastThisManyCitations = n - i;
            
            // If the current paper has at least as many citations as the 
            // number of papers remaining (including itself), we found the h-index.
            if (citations[i] >= papersWithAtLeastThisManyCitations) {
                return papersWithAtLeastThisManyCitations;
            }
        }

        return 0; // If all papers have 0 citations
    }

    /**
     * SOLUTION 2: Counting Sort / Buckets (Optimal)
     * 
     * Idea: Since the maximum possible h-index is the total number of papers `n`,
     * we can cap all citations > n to n. Use an array to count frequencies, 
     * then scan backwards to find the maximum valid h-index in linear time.
     * 
     * Time Complexity: O(N) - Two separate single passes.
     * Space Complexity: O(N) - For the bucket array of size n + 1.
     */
    public static int hIndexCounting(int[] citations) {
        if (citations == null || citations.length == 0) return 0;

        int n = citations.length;
        int[] buckets = new int[n + 1];

        // 1. Populate the buckets
        for (int c : citations) {
            if (c >= n) {
                buckets[n]++;
            } else {
                buckets[c]++;
            }
        }

        // 2. Iterate backwards to find the h-index
        int accumulatedPapers = 0;
        for (int h = n; h >= 0; h--) {
            accumulatedPapers += buckets[h];
            
            // If the total number of papers with at least 'h' citations 
            // is greater than or equal to 'h', we've found our answer.
            if (accumulatedPapers >= h) {
                return h;
            }
        }

        return 0;
    }
}
