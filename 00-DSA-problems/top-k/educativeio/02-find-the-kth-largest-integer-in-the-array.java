import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * PROBLEM STATEMENT:
 * Given an array of strings, nums, where each string represents an integer without leading zeros, 
 * and an integer k, find and return the string representing the kth largest integer in the array.
 * 
 * Note: Treat duplicate integers as distinct entities.
 *
 * CONSTRAINTS:
 * 1 <= k <= nums.length <= 10^3
 * 1 <= nums[i].length <= 100
 * nums[i] consists of only digits.
 * nums[i] will not have any leading zeros.
 * 
 * ==========================================================================================
 * CRITICAL INSIGHT:
 * The strings can be up to 100 characters long. This means they can represent numbers far 
 * beyond the maximum capacity of standard primitive types (`long` maxes out at ~19 digits).
 * Attempting to parse these strings into `Long` or `Integer` will result in a NumberFormatException.
 * 
 * Therefore, we MUST compare them as strings. 
 * Since there are no leading zeros:
 * 1. If lengths are different: The longer string represents the larger number.
 * 2. If lengths are equal: Standard lexicographical (dictionary) comparison works perfectly 
 *    because the ASCII values of digits '0' through '9' are sequential.
 * ==========================================================================================
 * LATEST JAVA FEATURES USED:
 * - Records (`record TestCase(...)`): For clean, immutable test data structures.
 * - Local Variable Type Inference (`var`): For cleaner code without losing strong typing.
 * - Lambda Expressions & Method References: For concise custom comparators.
 * ==========================================================================================
 */
public class KthLargestStringInteger {

    // ==========================================================================================
    // CUSTOM COMPARISON LOGIC
    // ==========================================================================================
    /*
     * Returns:
     *  -1 if s1 < s2
     *   0 if s1 == s2
     *   1 if s1 > s2
     */
    private static int compareStrings(String s1, String s2) {
        if (s1.length() != s2.length()) {
            return Integer.compare(s1.length(), s2.length());
        }
        return s1.compareTo(s2);
    }

    // ==========================================================================================
    // SOLUTION 1: Sorting (The Brute-Force/Intuitive Way)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * We define a custom Comparator that uses our `compareStrings` logic.
     * We sort the array in ascending order. The 1st largest is at the end (N - 1),
     * so the kth largest is at index (N - k).
     *
     * VISUAL:
     * nums = ["3", "6", "7", "10"], k = 4
     * 
     * 1. Sort using custom length/lexicographical logic:
     *    ["3", "6", "7", "10"] (Notice "10" is largest because length 2 > length 1)
     *      ^    ^    ^     ^
     * Idx  0    1    2     3
     * 
     * 2. Target index = N - k = 4 - 4 = 0
     *    Element at index 0 is "3".
     *
     * COMPLEXITY:
     * - Time: O(N log N * L) - where N is array length and L is max string length (up to 100).
     *         The `* L` is because string comparison takes O(L) time in the worst case.
     * - Space: O(1) or O(N) depending on Arrays.sort implementation for objects (Timsort uses O(N)).
     */
    public static String kthLargestNumber_Sorting(String[] nums, int k) {
        var copy = nums.clone(); // Clone to preserve original array for tests
        
        Arrays.sort(copy, (a, b) -> compareStrings(a, b));
        
        return copy[copy.length - k];
    }

    // ==========================================================================================
    // SOLUTION 2: Min-Heap (Optimal for large N, small k)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * We use a PriorityQueue (Min-Heap) restricted to size k, backed by our custom Comparator.
     * As we insert elements, if the heap grows beyond size k, we drop the smallest element.
     * What remains are the 'k' largest elements. The smallest of these (at the root) is the 
     * kth largest overall.
     *
     * VISUAL:
     * nums = ["2", "21", "12", "1"], k = 3
     * 
     * Step 1: Add "2"  -> Heap: ["2"]
     * Step 2: Add "21" -> Heap: ["2", "21"]
     * Step 3: Add "12" -> Heap: ["2", "12", "21"] (Lengths: 1, 2, 2. "2" is root)
     * Step 4: Add "1"  -> Heap: ["1", "2", "12", "21"] -> Size > 3, poll root ("1") -> Heap: ["2", "12", "21"]
     * 
     * Final Root (Peek) = "2".
     *
     * COMPLEXITY:
     * - Time: O(N log k * L) - N insertions, each takes O(log k). String comparison adds O(L).
     * - Space: O(k) - Heap stores at most k strings.
     */
    public static String kthLargestNumber_MinHeap(String[] nums, int k) {
        // Min-Heap with custom string number comparator
        PriorityQueue<String> minHeap = new PriorityQueue<>((a, b) -> compareStrings(a, b));
        
        for (var num : nums) {
            minHeap.offer(num);
            if (minHeap.size() > k) {
                minHeap.poll(); // Evict the smallest integer string
            }
        }
        
        return minHeap.peek();
    }

    // ==========================================================================================
    // SOLUTION 3: QuickSelect (Optimal Average Time)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * We adapt the QuickSelect algorithm (Hoare's selection algorithm) using our string comparator.
     * We pick a random pivot and partition the strings such that all strings "smaller" than the pivot
     * are to its left, and "larger" are to its right.
     * If the pivot ends up exactly at index `N - k`, we found our answer.
     * Otherwise, we discard the half of the array that doesn't contain `N - k` and recurse.
     *
     * COMPLEXITY:
     * - Time: O(N * L) Average case. O(N^2 * L) Worst case (highly unlikely with random pivot).
     * - Space: O(log N) for the recursion stack depth on average.
     */
    public static String kthLargestNumber_QuickSelect(String[] nums, int k) {
        var copy = nums.clone(); // Clone to preserve original array
        int targetIndex = copy.length - k;
        return quickSelect(copy, 0, copy.length - 1, targetIndex, new Random());
    }

    private static String quickSelect(String[] nums, int left, int right, int targetIndex, Random random) {
        if (left == right) {
            return nums[left];
        }

        // Randomize pivot to avoid worst-case O(N^2)
        int pivotIndex = left + random.nextInt(right - left + 1);
        pivotIndex = partition(nums, left, right, pivotIndex);

        if (pivotIndex == targetIndex) {
            return nums[pivotIndex];
        } else if (pivotIndex < targetIndex) {
            return quickSelect(nums, pivotIndex + 1, right, targetIndex, random);
        } else {
            return quickSelect(nums, left, pivotIndex - 1, targetIndex, random);
        }
    }

    private static int partition(String[] nums, int left, int right, int pivotIndex) {
        String pivotValue = nums[pivotIndex];
        swap(nums, pivotIndex, right); // Move pivot to end
        
        int storeIndex = left;
        for (int i = left; i < right; i++) {
            // If nums[i] < pivotValue
            if (compareStrings(nums[i], pivotValue) < 0) {
                swap(nums, storeIndex, i);
                storeIndex++;
            }
        }
        swap(nums, storeIndex, right); // Move pivot to final place
        return storeIndex;
    }

    private static void swap(String[] nums, int i, int j) {
        String temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }

    // ==========================================================================================
    // TESTING SUITE
    // ==========================================================================================
    
    // Java 14+ Record for concise test case representation
    public record TestCase(String[] nums, int k, String expected) {}

    public static void main(String[] args) {
        var testCases = new TestCase[] {
            new TestCase(new String[]{"3", "6", "7", "10"}, 4, "3"),
            new TestCase(new String[]{"2", "21", "12", "1"}, 3, "2"),
            new TestCase(new String[]{"0", "0"}, 2, "0"),
            // Large strings test case (exceeds long capacity)
            new TestCase(new String[]{
                "123456789012345678901234567890", 
                "99999999999999999999999999999", 
                "1234567890123456789012345678901" // Length 31 (Largest)
            }, 2, "123456789012345678901234567890"), // Length 30 is 2nd largest
            new TestCase(new String[]{"2", "3", "3"}, 3, "2")
        };

        System.out.println("Running tests for Big Integer String approaches...\n");

        for (int i = 0; i < testCases.length; i++) {
            var tc = testCases[i];
            System.out.printf("Test Case %d:\n", i + 1);
            System.out.printf("Array: %s | k: %d | Expected: %s\n", Arrays.toString(tc.nums()), tc.k(), tc.expected());

            // Run & Validate Solution 1
            String res1 = kthLargestNumber_Sorting(tc.nums(), tc.k());
            System.out.printf("  [1. Sorting    ] Result: %s -> %s\n", res1, (res1.equals(tc.expected()) ? "PASS" : "FAIL"));

            // Run & Validate Solution 2
            String res2 = kthLargestNumber_MinHeap(tc.nums(), tc.k());
            System.out.printf("  [2. Min-Heap   ] Result: %s -> %s\n", res2, (res2.equals(tc.expected()) ? "PASS" : "FAIL"));

            // Run & Validate Solution 3
            String res3 = kthLargestNumber_QuickSelect(tc.nums(), tc.k());
            System.out.printf("  [3. QuickSelect] Result: %s -> %s\n", res3, (res3.equals(tc.expected()) ? "PASS" : "FAIL"));
            
            System.out.println("-".repeat(70));
        }
    }
}


class Solution {

    public String kthLargestNumber(String[] nums, int k) {

        /*
         * Step 1: Define a custom comparator for numeric strings
         *
         * Comparison logic:
         * 1. Compare based on length (longer string = larger number)
         * 2. If lengths are equal, compare lexicographically
         *
         * This works because:
         * - "123" > "45" (length-based)
         * - "456" > "123" (lexicographic when same length)
         */
        Comparator<String> numericStringComparator = (a, b) -> {
            if (a.length() != b.length()) {
                return a.length() - b.length(); // smaller length = smaller number
            }
            return a.compareTo(b); // lexicographic comparison
        };

        /*
         * Step 2: Create a Min Heap of size K
         *
         * Why Min Heap?
         * - We want to track top K largest elements
         * - Smallest among them (root) will be kth largest
         */
        PriorityQueue<String> minHeap = new PriorityQueue<>(numericStringComparator);

        /*
         * Step 3: Process each number
         */
        for (String num : nums) {

            // Add current number to heap
            minHeap.offer(num);

            /*
             * If heap size exceeds k,
             * remove the smallest element
             *
             * This ensures:
             * Heap always contains only the K largest elements
             */
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        /*
         * Step 4: The root of heap is kth largest
         */
        return minHeap.peek();
    }
}


class Solution2 {

    /*
     * Main function:
     * Converts kth largest problem into kth smallest index
     */
    public String kthLargestNumber(String[] nums, int k) {

        int n = nums.length;

        // Convert kth largest → kth smallest index
        int targetIndex = n - k;

        return quickSelect(nums, 0, n - 1, targetIndex);
    }

    /*
     * QuickSelect:
     * Finds the element that would be at index 'k'
     * if the array were sorted
     */
    private String quickSelect(String[] nums, int left, int right, int k) {

        // Base case: only one element
        if (left == right) {
            return nums[left];
        }

        /*
         * Partition the array and get pivot's final sorted position
         */
        int pivotIndex = partition(nums, left, right);

        /*
         * Now decide where to go:
         *
         * Case 1: pivot is exactly at kth position
         */
        if (pivotIndex == k) {
            return nums[pivotIndex];
        }

        /*
         * Case 2: kth element lies on right side
         */
        else if (pivotIndex < k) {
            return quickSelect(nums, pivotIndex + 1, right, k);
        }

        /*
         * Case 3: kth element lies on left side
         */
        else {
            return quickSelect(nums, left, pivotIndex - 1, k);
        }
    }

    /*
     * Partition function (like QuickSort)
     *
     * Goal:
     * Rearrange array such that:
     *
     * [ smaller elements | pivot | larger elements ]
     *
     * and return pivot's final position
     */
    private int partition(String[] nums, int left, int right) {

        // Choose last element as pivot
        String pivot = nums[right];

        /*
         * 'i' will track position where next smaller element should go
         */
        int i = left;

        /*
         * Traverse all elements except pivot
         */
        for (int j = left; j < right; j++) {

            /*
             * If current element is <= pivot,
             * move it to the left partition
             */
            if (compare(nums[j], pivot) <= 0) {

                // Place it at correct position
                swap(nums, i, j);

                // Move pointer forward
                i++;
            }
        }

        /*
         * Place pivot at its correct sorted position
         */
        swap(nums, i, right);

        return i; // pivot index
    }

    /*
     * Custom comparator for numeric strings
     *
     * Returns:
     * negative → a < b
     * zero     → a == b
     * positive → a > b
     */
    private int compare(String a, String b) {

        /*
         * Step 1: Compare lengths
         * Longer string = larger number
         */
        if (a.length() != b.length()) {
            return a.length() - b.length();
        }

        /*
         * Step 2: If same length,
         * lexicographic comparison works
         */
        return a.compareTo(b);
    }

    /*
     * Swap helper
     */
    private void swap(String[] nums, int i, int j) {
        String temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }
}
