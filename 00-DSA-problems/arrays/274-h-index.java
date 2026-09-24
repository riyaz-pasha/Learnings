import java.util.*;
/*
 * Given an array of integers citations where citations[i] is the number of
 * citations a researcher received for their ith paper, return the researcher's
 * h-index.
 * 
 * According to the definition of h-index on Wikipedia: The h-index is defined
 * as the maximum value of h such that the given researcher has published at
 * least h papers that have each been cited at least h times.
 * 
 * Example 1:
 * Input: citations = [3,0,6,1,5]
 * Output: 3
 * Explanation: [3,0,6,1,5] means the researcher has 5 papers in total and each
 * of them had received 3, 0, 6, 1, 5 citations respectively.
 * Since the researcher has 3 papers with at least 3 citations each and the
 * remaining two with no more than 3 citations each, their h-index is 3.
 * 
 * Example 2:
 * Input: citations = [1,3,1]
 * Output: 1
 */

class HIndex {

    // Solution 1: Sorting Approach (Most Intuitive)
    // Time: O(n log n), Space: O(1)
    public int hIndex(int[] citations) {
        Arrays.sort(citations);
        int n = citations.length;

        for (int i = 0; i < n; i++) {
            // Number of papers with at least citations[i] citations
            int papersWithAtLeastThisCitations = n - i;

            // If current citation count >= number of remaining papers,
            // then we found our h-index
            if (citations[i] >= papersWithAtLeastThisCitations) {
                return papersWithAtLeastThisCitations;
            }
        }

        return 0;
    }

    // Solution 2: Reverse Sorting (Cleaner Logic)
    // Time: O(n log n), Space: O(1)
    public int hIndexReverseSorted(int[] citations) {
        // Sort in descending order
        Arrays.sort(citations);
        reverseArray(citations);

        int hIndex = 0;
        for (int i = 0; i < citations.length; i++) {
            // We have (i + 1) papers so far
            // If current paper has at least (i + 1) citations,
            // we can have h-index of (i + 1)
            if (citations[i] >= i + 1) {
                hIndex = i + 1;
            } else {
                break;
            }
        }

        return hIndex;
    }

    // Solution 3: Counting Sort Approach (Optimal)
    // Time: O(n), Space: O(n)
    public int hIndexCountingSort(int[] citations) {
        int n = citations.length;
        int[] buckets = new int[n + 1];

        // Count papers for each citation count
        // All papers with >= n citations go to bucket[n]
        for (int citation : citations) {
            if (citation >= n) {
                buckets[n]++;
            } else {
                buckets[citation]++;
            }
        }

        // Count papers from right to left
        int count = 0;
        for (int i = n; i >= 0; i--) {
            count += buckets[i];
            // If we have at least i papers with >= i citations
            if (count >= i) {
                return i;
            }
        }

        return 0;
    }

    // Solution 4: Binary Search Approach
    // Time: O(n log n), Space: O(1)
    public int hIndexBinarySearch(int[] citations) {
        Arrays.sort(citations);
        int n = citations.length;
        int left = 0, right = n;

        while (left < right) {
            int mid = left + (right - left + 1) / 2;

            // Check if we can achieve h-index of mid
            if (canAchieveHIndex(citations, mid)) {
                left = mid;
            } else {
                right = mid - 1;
            }
        }

        return left;
    }

    private boolean canAchieveHIndex(int[] citations, int h) {
        int n = citations.length;
        int count = 0;

        for (int citation : citations) {
            if (citation >= h) {
                count++;
            }
        }

        return count >= h;
    }

    // Solution 5: One Pass Linear Search
    // Time: O(n²), Space: O(1) - Less efficient but simple
    public int hIndexLinear(int[] citations) {
        int n = citations.length;
        int maxH = 0;

        // Try all possible h values from 0 to n
        for (int h = 0; h <= n; h++) {
            int count = 0;

            // Count papers with at least h citations
            for (int citation : citations) {
                if (citation >= h) {
                    count++;
                }
            }

            // If we have at least h papers with >= h citations
            if (count >= h) {
                maxH = h;
            }
        }

        return maxH;
    }

    // Utility method for reverse sorting solution
    private void reverseArray(int[] arr) {
        int left = 0, right = arr.length - 1;
        while (left < right) {
            int temp = arr[left];
            arr[left] = arr[right];
            arr[right] = temp;
            left++;
            right--;
        }
    }

    // Test cases
    public static void main(String[] args) {
        HIndex solution = new HIndex();

        // Test case 1
        int[] citations1 = { 3, 0, 6, 1, 5 };
        System.out.println("Test 1:");
        System.out.println("Sorting: " + solution.hIndex(citations1)); // Expected: 3
        System.out.println("Reverse Sort: " + solution.hIndexReverseSorted(citations1.clone())); // Expected: 3
        System.out.println("Counting Sort: " + solution.hIndexCountingSort(citations1)); // Expected: 3
        System.out.println("Binary Search: " + solution.hIndexBinarySearch(citations1.clone())); // Expected: 3
        System.out.println("Linear: " + solution.hIndexLinear(citations1)); // Expected: 3
        System.out.println();

        // Test case 2
        int[] citations2 = { 1, 3, 1 };
        System.out.println("Test 2:");
        System.out.println("Sorting: " + solution.hIndex(citations2)); // Expected: 1
        System.out.println("Counting Sort: " + solution.hIndexCountingSort(citations2)); // Expected: 1
        System.out.println();

        // Test case 3 - All zeros
        int[] citations3 = { 0, 0, 0, 0 };
        System.out.println("Test 3 (all zeros): " + solution.hIndex(citations3)); // Expected: 0

        // Test case 4 - High citations
        int[] citations4 = { 10, 8, 5, 4, 3 };
        System.out.println("Test 4 (high citations): " + solution.hIndex(citations4)); // Expected: 4

        // Test case 5 - Single paper
        int[] citations5 = { 100 };
        System.out.println("Test 5 (single paper): " + solution.hIndex(citations5)); // Expected: 1

        // Test case 6 - Edge case
        int[] citations6 = { 0, 1, 3, 5, 6 };
        System.out.println("Test 6: " + solution.hIndex(citations6)); // Expected: 3
    }

}

/*
 * Algorithm Explanations:
 * 
 * 1. SORTING APPROACH:
 * - Sort citations in ascending order
 * - For each position i, check if citations[i] >= (n-i)
 * - The first position where this is true gives us the h-index
 * - Time: O(n log n), Space: O(1)
 * 
 * 2. REVERSE SORTING:
 * - Sort in descending order for cleaner logic
 * - Check if we can have h-index of (i+1) for each position
 * - Time: O(n log n), Space: O(1)
 * 
 * 3. COUNTING SORT (OPTIMAL):
 * - Use buckets to count papers by citation count
 * - Papers with >= n citations all go to bucket[n]
 * - Traverse from right to left to find maximum valid h-index
 * - Time: O(n), Space: O(n)
 * 
 * 4. BINARY SEARCH:
 * - Binary search on possible h-index values (0 to n)
 * - For each candidate h, check if it's achievable
 * - Time: O(n log n), Space: O(1)
 * 
 * 5. LINEAR SEARCH:
 * - Try all possible h values from 0 to n
 * - For each h, count papers with >= h citations
 * - Time: O(n²), Space: O(1)
 * 
 * Key Insights:
 * - H-index can never exceed the number of papers (n)
 * - We need at least h papers with >= h citations each
 * - The counting sort approach is optimal when citation values are reasonable
 * - Sorting approaches are more intuitive and work well for interviews
 * 
 * Example walkthrough for [3,0,6,1,5]:
 * - Sorted: [0,1,3,5,6]
 * - At position 2: citations[2]=3, remaining papers=3, so h-index=3
 * - We have 3 papers (3,5,6) with at least 3 citations each
 */
