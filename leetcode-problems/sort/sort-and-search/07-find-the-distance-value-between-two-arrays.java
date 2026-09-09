/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We have two arrays of integers, arr1 and arr2, and a distance threshold 'd'.
 * For every element in arr1, we must check its absolute difference with EVERY 
 * element in arr2. 
 * If an element in arr1 is "far enough" from ALL elements in arr2 (meaning 
 * every single absolute difference is strictly greater than 'd'), it counts as a 
 * valid element. We need to return the total count of such valid elements.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Are there duplicate values in either arr1 or arr2? 
 *    (Yes, the arrays can contain duplicates. Each element in arr1 is evaluated 
 *     independently based on its index).
 * 2. Can 'd' be zero? 
 *    (Yes, if d = 0, we are essentially counting elements in arr1 that do not 
 *     exist in arr2).
 * 3. Can 'd' be negative? 
 *    (Constraints say 0 <= d <= 100, so 'd' is always non-negative).
 * 4. Are we allowed to sort or mutate the input arrays? 
 *    (Sorting arr2 is highly advantageous and typically allowed. If mutation 
 *     is forbidden, we can clone arr2 before sorting).
 * 5. Does the order of elements in the arrays matter? 
 *    (No, the result depends entirely on the values, not their sequences).
 * 6. Can the arrays be of different lengths? 
 *    (Yes, arr1 and arr2 lengths are independent, ranging up to 500).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (The "Obvious" Baseline):
 *    - For every element `x` in arr1, iterate through all elements `y` in arr2.
 *    - Calculate `|x - y|`. If it's <= d, `x` is invalid. Break early.
 *    - If we check all `y` and no difference is <= d, increment our count.
 *    - Time: O(N * M), Space: O(1). With constraints N, M <= 500, N*M = 250,000. 
 *      This will easily pass, but it's important to discuss optimization.
 * 
 * 2. Binary Search (The "Optimal" Standard):
 *    - Instead of checking all elements in arr2 against `x`, we only care about 
 *      the elements in arr2 that are CLOSEST to `x`. If the closest element 
 *      is strictly > d away, all other elements are definitely > d away.
 *    - Sort arr2 once. 
 *    - For each `x` in arr1, binary search for `x` in arr2.
 *    - Find the insertion point (where `x` would fit). Check the element just 
 *      before and just after this point.
 *    - Time: O(M log M + N log M), Space: O(1) or O(M) for sort memory.
 * 
 * 3. Domain Constraint / Bucket Strategy (The "Constraint-Hacker"):
 *    - Values range from -1000 to 1000. 
 *    - We can use a boolean array of size 2001 to mark which values exist in arr2.
 *    - For each `x` in arr1, just check the boolean array in the range [x-d, x+d].
 *    - Time: O(M + N * d), Space: O(Range). Extremely fast for small ranges.
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Binary Search Approach)
 * ============================================================================
 * Example: arr1 = [4, 5, 8], arr2 = [10, 9, 1, 8], d = 2
 * 
 * 1. Sort arr2:
 *    [1, 8, 9, 10]
 * 
 * 2. Process x = 4 (from arr1):
 *    - Binary Search for 4 in [1, 8, 9, 10].
 *    - It would fit between 1 and 8. (Indices: previous=0, next=1).
 *    - Closest values are arr2[0]=1 and arr2[1]=8.
 *    - |4 - 1| = 3 (3 > 2, valid).
 *    - |4 - 8| = 4 (4 > 2, valid).
 *    - Result: 4 is isolated! count = 1.
 * 
 * 3. Process x = 5 (from arr1):
 *    - Binary Search for 5. Fits between 1 and 8.
 *    - |5 - 1| = 4 (valid). |5 - 8| = 3 (valid).
 *    - Result: 5 is isolated! count = 2.
 * 
 * 4. Process x = 8 (from arr1):
 *    - Binary Search for 8. Found EXACT match at index 1.
 *    - Difference is 0. 0 <= 2 (invalid).
 *    - Result: 8 is NOT isolated. count remains 2.
 * 
 * Final Count = 2.
 */

import java.util.Arrays;
import java.util.TreeSet;

public class DistanceValue {

    public static void main(String[] args) {
        int[] arr1 = {4, 5, 8};
        int[] arr2 = {10, 9, 1, 8};
        int d = 2;

        System.out.println("Solution 1 (Brute Force): " + findTheDistanceValueBruteForce(arr1, arr2, d));
        System.out.println("Solution 2 (Binary Search): " + findTheDistanceValueBinarySearch(arr1, arr2.clone(), d));
        System.out.println("Solution 3 (TreeSet): " + findTheDistanceValueTreeSet(arr1, arr2, d));
    }

    /**
     * SOLUTION 1: Brute Force
     * 
     * Idea: Direct translation of the problem statement using nested loops.
     * 
     * Time Complexity: O(N * M) - Every element in arr1 compared with every in arr2.
     * Space Complexity: O(1) - Constant auxiliary space.
     */
    public static int findTheDistanceValueBruteForce(int[] arr1, int[] arr2, int d) {
        int validCount = 0;

        for (int x : arr1) {
            boolean isValid = true;
            for (int y : arr2) {
                if (Math.abs(x - y) <= d) {
                    isValid = false;
                    break; // Early exit: x is too close to y, no need to check others
                }
            }
            if (isValid) {
                validCount++;
            }
        }
        return validCount;
    }

    /**
     * SOLUTION 2: Binary Search (Optimal)
     * 
     * Idea: Sort arr2. For each number in arr1, find where it belongs in arr2. 
     * You only need to check the distance to its immediate left and right neighbors 
     * in the sorted array, as they are the closest values.
     * 
     * Time Complexity: O(M log M + N log M)
     * Space Complexity: O(1) to O(M) depending on Arrays.sort implementation.
     */
    public static int findTheDistanceValueBinarySearch(int[] arr1, int[] arr2, int d) {
        Arrays.sort(arr2);
        int validCount = 0;

        for (int x : arr1) {
            // Arrays.binarySearch returns the index if found.
            // If not found, it returns -(insertion_point) - 1.
            int idx = Arrays.binarySearch(arr2, x);

            if (idx >= 0) {
                // Exact match found. Distance is 0, which is <= d (since d >= 0).
                // It fails the condition, so we just continue.
                continue;
            }

            // Calculate the actual insertion point
            int insertPoint = -(idx + 1);
            boolean isValid = true;

            // Check the element just to the right (if it exists)
            if (insertPoint < arr2.length) {
                if (Math.abs(arr2[insertPoint] - x) <= d) {
                    isValid = false;
                }
            }

            // Check the element just to the left (if it exists)
            if (isValid && insertPoint > 0) {
                if (Math.abs(arr2[insertPoint - 1] - x) <= d) {
                    isValid = false;
                }
            }

            if (isValid) {
                validCount++;
            }
        }

        return validCount;
    }

    /**
     * SOLUTION 3: Collections (TreeSet) Approach
     * 
     * Idea: Use Java's TreeSet which provides built-in methods for finding the 
     * closest elements (ceiling for next highest/equal, floor for next lowest/equal).
     * This is conceptually identical to Binary Search but abstracts away the 
     * index math, making the logic highly readable.
     * 
     * Time Complexity: O(M log M + N log M)
     * Space Complexity: O(M) - Overhead of storing arr2 in a Red-Black Tree.
     */
    public static int findTheDistanceValueTreeSet(int[] arr1, int[] arr2, int d) {
        TreeSet<Integer> treeSet = new TreeSet<>();
        for (int num : arr2) {
            treeSet.add(num);
        }

        int validCount = 0;

        for (int x : arr1) {
            // Get closest elements
            Integer ceil = treeSet.ceiling(x);
            Integer floor = treeSet.floor(x);

            boolean isValid = true;

            if (ceil != null && Math.abs(ceil - x) <= d) {
                isValid = false;
            } else if (floor != null && Math.abs(x - floor) <= d) {
                isValid = false;
            }

            if (isValid) {
                validCount++;
            }
        }

        return validCount;
    }
}
