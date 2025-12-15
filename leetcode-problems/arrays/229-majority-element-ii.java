/*
 * Given an integer array of size n, find all elements that appear more than ⌊
 * n/3 ⌋ times.
 * 
 * Example 1:
 * 
 * Input: nums = [3,2,3]
 * Output: [3]
 * Example 2:
 * 
 * Input: nums = [1]
 * Output: [1]
 * Example 3:
 * 
 * Input: nums = [1,2]
 * Output: [1,2]
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MajorityElementII {

    // Solution 1: Boyer-Moore Voting Algorithm (Optimal)
    // Time: O(n), Space: O(1)
    public List<Integer> majorityElement1(int[] nums) {
        /*
         * Key Insight: At most 2 elements can appear more than n/3 times
         * Use two candidates and two counters
         */

        // Step 1: Find potential candidates
        int candidate1 = 0, candidate2 = 0;
        int count1 = 0, count2 = 0;

        for (int num : nums) {
            if (num == candidate1) {
                count1++;
            } else if (num == candidate2) {
                count2++;
            } else if (count1 == 0) {
                candidate1 = num;
                count1 = 1;
            } else if (count2 == 0) {
                candidate2 = num;
                count2 = 1;
            } else {
                count1--;
                count2--;
            }
        }

        // Step 2: Verify candidates
        count1 = 0;
        count2 = 0;
        for (int num : nums) {
            if (num == candidate1)
                count1++;
            else if (num == candidate2)
                count2++;
        }

        List<Integer> result = new ArrayList<>();
        int threshold = nums.length / 3;
        if (count1 > threshold)
            result.add(candidate1);
        if (count2 > threshold)
            result.add(candidate2);

        return result;
    }

    // Solution 2: HashMap Approach (Easy to Understand)
    // Time: O(n), Space: O(n)
    public List<Integer> majorityElement2(int[] nums) {
        Map<Integer, Integer> countMap = new HashMap<>();
        List<Integer> result = new ArrayList<>();
        int threshold = nums.length / 3;

        // Count occurrences
        for (int num : nums) {
            countMap.put(num, countMap.getOrDefault(num, 0) + 1);
        }

        // Find elements that appear more than n/3 times
        for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
            if (entry.getValue() > threshold) {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    // Solution 3: Boyer-Moore with Detailed Comments
    // Time: O(n), Space: O(1)
    public List<Integer> majorityElement3(int[] nums) {
        /*
         * Boyer-Moore Majority Voting Algorithm Extended
         * 
         * Why it works:
         * - At most 2 elements can appear > n/3 times
         * - We maintain 2 candidates with counters
         * - When we see a candidate, increment its counter
         * - When we see a different element and both counters > 0, decrement both
         * - When a counter reaches 0, replace that candidate
         * 
         * After first pass, we have 2 potential candidates
         * Second pass verifies if they actually appear > n/3 times
         */

        if (nums == null || nums.length == 0) {
            return new ArrayList<>();
        }

        // Initialize candidates and counters
        Integer cand1 = null, cand2 = null;
        int cnt1 = 0, cnt2 = 0;

        // First pass: Find potential candidates
        for (int num : nums) {
            if (cand1 != null && cand1 == num) {
                cnt1++;
            } else if (cand2 != null && cand2 == num) {
                cnt2++;
            } else if (cnt1 == 0) {
                cand1 = num;
                cnt1 = 1;
            } else if (cnt2 == 0) {
                cand2 = num;
                cnt2 = 1;
            } else {
                // Different element, decrement both counters
                cnt1--;
                cnt2--;
            }
        }

        // Second pass: Verify candidates
        cnt1 = 0;
        cnt2 = 0;
        for (int num : nums) {
            if (cand1 != null && num == cand1)
                cnt1++;
            if (cand2 != null && num == cand2)
                cnt2++;
        }

        List<Integer> result = new ArrayList<>();
        int threshold = nums.length / 3;

        if (cnt1 > threshold)
            result.add(cand1);
        if (cand2 != null && cand2 != cand1 && cnt2 > threshold) {
            result.add(cand2);
        }

        return result;
    }

    // Solution 4: Sorting Approach
    // Time: O(n log n), Space: O(1) or O(n) depending on sort implementation
    public List<Integer> majorityElement4(int[] nums) {
        Arrays.sort(nums);
        List<Integer> result = new ArrayList<>();
        int threshold = nums.length / 3;
        int count = 1;

        for (int i = 1; i < nums.length; i++) {
            if (nums[i] == nums[i - 1]) {
                count++;
            } else {
                if (count > threshold) {
                    result.add(nums[i - 1]);
                }
                count = 1;
            }
        }

        // Check last element
        if (count > threshold) {
            result.add(nums[nums.length - 1]);
        }

        return result;
    }

    // Helper: Explain Boyer-Moore algorithm with visualization
    private static void explainBoyerMoore(int[] nums) {
        System.out.println("\n=== Boyer-Moore Algorithm Walkthrough ===");
        System.out.print("Array: ");
        printArray(nums);
        System.out.println("Threshold: > " + (nums.length / 3));

        int c1 = 0, c2 = 0;
        int cnt1 = 0, cnt2 = 0;

        System.out.println("\nFirst Pass (Finding Candidates):");
        for (int i = 0; i < nums.length; i++) {
            int num = nums[i];

            if (num == c1) {
                cnt1++;
            } else if (num == c2) {
                cnt2++;
            } else if (cnt1 == 0) {
                c1 = num;
                cnt1 = 1;
            } else if (cnt2 == 0) {
                c2 = num;
                cnt2 = 1;
            } else {
                cnt1--;
                cnt2--;
            }

            System.out.printf("Index %d: num=%d, cand1=%d(cnt=%d), cand2=%d(cnt=%d)%n",
                    i, num, c1, cnt1, c2, cnt2);
        }

        System.out.println("\nCandidates: " + c1 + " and " + c2);

        // Verify
        cnt1 = 0;
        cnt2 = 0;
        for (int num : nums) {
            if (num == c1)
                cnt1++;
            else if (num == c2)
                cnt2++;
        }

        System.out.println("\nSecond Pass (Verification):");
        System.out.println("Candidate " + c1 + " appears " + cnt1 + " times");
        System.out.println("Candidate " + c2 + " appears " + cnt2 + " times");

        List<Integer> result = new ArrayList<>();
        int threshold = nums.length / 3;
        if (cnt1 > threshold)
            result.add(c1);
        if (cnt2 > threshold)
            result.add(c2);

        System.out.println("\nResult: " + result);
    }

    // Helper: Print array
    private static void printArray(int[] nums) {
        System.out.print("[");
        for (int i = 0; i < nums.length; i++) {
            System.out.print(nums[i]);
            if (i < nums.length - 1)
                System.out.print(", ");
        }
        System.out.println("]");
    }

    // Test cases
    public static void main(String[] args) {
        MajorityElementII solution = new MajorityElementII();

        // Test case 1
        int[] nums1 = { 3, 2, 3 };
        System.out.print("Example 1 - Input: ");
        printArray(nums1);
        System.out.println("Output: " + solution.majorityElement1(nums1));
        System.out.println("Explanation: 3 appears 2 times > " + (nums1.length / 3));

        // Test case 2
        int[] nums2 = { 1 };
        System.out.print("\nExample 2 - Input: ");
        printArray(nums2);
        System.out.println("Output: " + solution.majorityElement1(nums2));

        // Test case 3
        int[] nums3 = { 1, 2 };
        System.out.print("\nExample 3 - Input: ");
        printArray(nums3);
        System.out.println("Output: " + solution.majorityElement1(nums3));

        // Test case 4: Two majority elements
        int[] nums4 = { 1, 1, 1, 2, 2, 2, 3 };
        System.out.print("\nTwo majorities - Input: ");
        printArray(nums4);
        System.out.println("Output: " + solution.majorityElement2(nums4));
        System.out.println("Threshold: > " + (nums4.length / 3) + " (both 1 and 2 appear 3 times)");

        // Test case 5: No majority element
        int[] nums5 = { 1, 2, 3, 4, 5 };
        System.out.print("\nNo majority - Input: ");
        printArray(nums5);
        System.out.println("Output: " + solution.majorityElement1(nums5));

        // Test case 6: All same
        int[] nums6 = { 5, 5, 5, 5 };
        System.out.print("\nAll same - Input: ");
        printArray(nums6);
        System.out.println("Output: " + solution.majorityElement1(nums6));

        // Detailed walkthrough
        explainBoyerMoore(new int[] { 1, 1, 1, 2, 2, 3, 4 });

        // Compare all solutions
        System.out.println("\n=== Solution Comparison ===");
        int[] test = { 3, 2, 3 };
        System.out.print("Test array: ");
        printArray(test);
        System.out.println("Solution 1 (Boyer-Moore): " + solution.majorityElement1(test));
        System.out.println("Solution 2 (HashMap): " + solution.majorityElement2(test));
        System.out.println("Solution 3 (Detailed BM): " + solution.majorityElement3(test));
        System.out.println("Solution 4 (Sorting): " + solution.majorityElement4(test));
    }
}

/*
 * I've provided four solutions for finding elements that appear more than ⌊n/3⌋
 * times:
 * 
 * ## **Key Mathematical Insight:**
 ** 
 * At most 2 elements can appear more than n/3 times!**
 * 
 * Why? If 3 elements each appeared more than n/3 times:
 * - 3 × (n/3 + 1) > n ❌ (Impossible!)
 * 
 * This is why Boyer-Moore works with just 2 candidates.
 * 
 * ## **Solutions Provided:**
 * 
 * ### **Solution 1: Boyer-Moore Voting Algorithm (Optimal) ⭐**
 * - **Time**: O(n), **Space**: O(1)
 * - Two-pass algorithm:
 * 1. **First pass**: Find 2 potential candidates using voting
 * 2. **Second pass**: Verify candidates actually appear > n/3 times
 * - Most efficient for interviews
 * 
 * ### **Solution 2: HashMap Approach**
 * - **Time**: O(n), **Space**: O(n)
 * - Count all occurrences in HashMap
 * - Filter elements with count > n/3
 * - Easiest to understand and implement
 * 
 * ### **Solution 3: Detailed Boyer-Moore**
 * - Same as Solution 1 with extensive comments
 * - Handles null candidates properly
 * - Best for learning the algorithm
 * 
 * ### **Solution 4: Sorting Approach**
 * - **Time**: O(n log n), **Space**: O(1)
 * - Sort array, then count consecutive elements
 * - Simple but slower
 * 
 * ## **Boyer-Moore Algorithm Explained:**
 * 
 * ```
 * Array: [1, 1, 1, 2, 2, 3, 4]
 * Threshold: > 2 (7/3 = 2)
 * 
 * First Pass (Finding Candidates):
 * Index 0: num=1, cand1=1(cnt=1), cand2=0(cnt=0)
 * Index 1: num=1, cand1=1(cnt=2), cand2=0(cnt=0)
 * Index 2: num=1, cand1=1(cnt=3), cand2=0(cnt=0)
 * Index 3: num=2, cand1=1(cnt=3), cand2=2(cnt=1)
 * Index 4: num=2, cand1=1(cnt=3), cand2=2(cnt=2)
 * Index 5: num=3, cand1=1(cnt=2), cand2=2(cnt=1) // Both decremented
 * Index 6: num=4, cand1=1(cnt=1), cand2=2(cnt=0) // Both decremented
 * 
 * Candidates: 1 and 2
 * 
 * Second Pass (Verification):
 * Candidate 1 appears 3 times > 2 ✓
 * Candidate 2 appears 2 times = 2 ✗
 * 
 * Result: [1]
 * ```
 * 
 * ## **How Boyer-Moore Works:**
 * 
 * 1. **Maintain 2 candidates** with counters
 * 2. When we see a candidate, **increment** its counter
 * 3. When we see a new element:
 * - If a counter is 0, **replace** that candidate
 * - If both counters > 0, **decrement** both
 * 4. **Verify** candidates in second pass (critical!)
 * 
 * ## **Complexity Comparison:**
 * 
 * | Solution | Time | Space | Best For |
 * |----------|------|-------|----------|
 * | Boyer-Moore (1, 3) | O(n) | O(1) | Optimal |
 * | HashMap (2) | O(n) | O(n) | Clarity |
 * | Sorting (4) | O(n log n) | O(1) | Simple |
 * 
 * ## **Edge Cases Handled:**
 * - Single element
 * - Two elements (both are majority)
 * - No majority elements
 * - All elements same
 * - Exactly 2 majority elements
 * 
 * ## **Why Verification is Necessary:**
 * 
 * Boyer-Moore finds **potential** candidates, but doesn't guarantee they appear
 * > n/3 times. Example:
 * - Array: `[1, 2, 3]` would suggest candidates, but none appear > 1 time
 ** 
 * For interviews, use Solution 1** - it's optimal O(n) time with O(1) space,
 * demonstrating understanding of the advanced Boyer-Moore algorithm!
 */