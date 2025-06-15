import java.util.*;
/*
 * Given an array nums of size n, return the majority element.
 * 
 * The majority element is the element that appears more than ⌊n / 2⌋ times. You
 * may assume that the majority element always exists in the array.
 * 
 * Example 1:
 * Input: nums = [3,2,3]
 * Output: 3
 * 
 * Example 2:
 * Input: nums = [2,2,1,1,1,2,2]
 * Output: 2
 */

class MajorityElement {

    /**
     * Solution 1: Boyer-Moore Voting Algorithm - Most Optimal
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Key insight: If majority element exists, it will survive the voting process
     * The algorithm maintains a candidate and count, updating based on votes
     */
    public int majorityElement(int[] nums) {
        int candidate = 0;
        int count = 0;

        // Phase 1: Find potential candidate
        for (int num : nums) {
            if (count == 0) {
                candidate = num; // New candidate when count reaches 0
            }
            count += (num == candidate) ? 1 : -1; // Vote for or against
        }

        // Phase 2: Verify candidate (not needed since problem guarantees majority
        // exists)
        // But included for completeness and understanding
        count = 0;
        for (int num : nums) {
            if (num == candidate) {
                count++;
            }
        }

        if (count > nums.length / 2) {
            return candidate;
        }

        throw new IllegalArgumentException("No majority element found");
        // return candidate;
        // Since majority is guaranteed, candidate is always correct
    }

    /**
     * Solution 2: Boyer-Moore (Simplified) - Most Common Interview Answer
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Simplified version without verification since majority is guaranteed
     */
    public int majorityElementSimplified(int[] nums) {
        int candidate = 0;
        int count = 0;

        for (int num : nums) {
            if (count == 0) {
                candidate = num;
            }
            count += (num == candidate) ? 1 : -1;
        }

        return candidate;
    }

    /**
     * Solution 3: HashMap (Frequency Count) - Most Intuitive
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     * 
     * Count frequency of each element and return the one with count > n/2
     */
    public int majorityElementHashMap(int[] nums) {
        Map<Integer, Integer> frequencyMap = new HashMap<>();
        int majorityThreshold = nums.length / 2;

        for (int num : nums) {
            frequencyMap.put(num, frequencyMap.getOrDefault(num, 0) + 1);

            // Early return as soon as we find majority
            if (frequencyMap.get(num) > majorityThreshold) {
                return num;
            }
        }

        // This should never be reached given problem constraints
        return -1;
    }

    /**
     * Solution 4: Sorting Approach - Simple but not optimal
     * Time Complexity: O(n log n)
     * Space Complexity: O(1) or O(log n) depending on sort implementation
     * 
     * Since majority element appears > n/2 times, it will always be at index n/2
     */
    public int majorityElementSorting(int[] nums) {
        Arrays.sort(nums);
        return nums[nums.length / 2]; // Middle element is always majority
    }

    /**
     * Solution 5: Randomized Algorithm - Interesting approach
     * Time Complexity: O(n) expected, O(infinity) worst case
     * Space Complexity: O(1)
     * 
     * Randomly pick elements until we find the majority element
     */
    public int majorityElementRandomized(int[] nums) {
        Random random = new Random();
        int majorityThreshold = nums.length / 2;

        while (true) {
            int candidateIndex = random.nextInt(nums.length);
            int candidate = nums[candidateIndex];

            // Count occurrences of this candidate
            int count = 0;
            for (int num : nums) {
                if (num == candidate) {
                    count++;
                }
            }

            if (count > majorityThreshold) {
                return candidate;
            }
        }
    }

    /**
     * Solution 6: Divide and Conquer - Recursive approach
     * Time Complexity: O(n log n)
     * Space Complexity: O(log n) due to recursion stack
     * 
     * Recursively find majority in left and right halves
     */
    public int majorityElementDivideConquer(int[] nums) {
        return majorityElementHelper(nums, 0, nums.length - 1);
    }

    private int majorityElementHelper(int[] nums, int left, int right) {
        // Base case
        if (left == right) {
            return nums[left];
        }

        int mid = left + (right - left) / 2;
        int leftMajority = majorityElementHelper(nums, left, mid);
        int rightMajority = majorityElementHelper(nums, mid + 1, right);

        // If both halves have same majority, return it
        if (leftMajority == rightMajority) {
            return leftMajority;
        }

        // Count occurrences of both candidates in current range
        int leftCount = countInRange(nums, leftMajority, left, right);
        int rightCount = countInRange(nums, rightMajority, left, right);

        return leftCount > rightCount ? leftMajority : rightMajority;
    }

    private int countInRange(int[] nums, int target, int left, int right) {
        int count = 0;
        for (int i = left; i <= right; i++) {
            if (nums[i] == target) {
                count++;
            }
        }
        return count;
    }

    /**
     * Solution 7: Bit Manipulation - Creative approach
     * Time Complexity: O(32n) = O(n)
     * Space Complexity: O(1)
     * 
     * For each bit position, count 1s and 0s. Majority element's bit is the
     * majority bit
     */
    public int majorityElementBitManipulation(int[] nums) {
        int majorityElement = 0;
        int n = nums.length;

        // Check each bit position (32 bits for integer)
        for (int i = 0; i < 32; i++) {
            int onesCount = 0;

            // Count number of 1s at bit position i
            for (int num : nums) {
                if ((num >> i & 1) == 1) {
                    onesCount++;
                }
            }

            // If majority of numbers have 1 at this position,
            // then majority element also has 1 at this position
            if (onesCount > n / 2) {
                majorityElement |= (1 << i);
            }
        }

        return majorityElement;
    }

    /**
     * Solution 8: Stack-based Approach - Alternative to Boyer-Moore
     * Time Complexity: O(n)
     * Space Complexity: O(n) worst case, but typically much less
     * 
     * Use stack to cancel out different elements
     */
    public int majorityElementStack(int[] nums) {
        Stack<Integer> stack = new Stack<>();

        for (int num : nums) {
            if (stack.isEmpty() || stack.peek() == num) {
                stack.push(num);
            } else {
                stack.pop(); // Cancel out different element
            }
        }

        // The remaining elements in stack are all the same (majority element)
        return stack.isEmpty() ? -1 : stack.peek();
    }

    // Helper method to verify a candidate is actually majority (for testing)
    private boolean isMajority(int[] nums, int candidate) {
        int count = 0;
        for (int num : nums) {
            if (num == candidate) {
                count++;
            }
        }
        return count > nums.length / 2;
    }

    // Test method to demonstrate all approaches
    public static void main(String[] args) {
        MajorityElement solution = new MajorityElement();

        // Test case 1: [3,2,3]
        System.out.println("=== Test Case 1: [3,2,3] ===");
        int[] nums1 = { 3, 2, 3 };
        System.out.println("Array: " + Arrays.toString(nums1));
        System.out.println("Boyer-Moore: " + solution.majorityElement(nums1));
        System.out.println("HashMap: " + solution.majorityElementHashMap(nums1));
        System.out.println("Sorting: " + solution.majorityElementSorting(nums1.clone()));
        System.out.println("Bit Manipulation: " + solution.majorityElementBitManipulation(nums1));

        // Test case 2: [2,2,1,1,1,2,2]
        System.out.println("\\n=== Test Case 2: [2,2,1,1,1,2,2] ===");
        int[] nums2 = { 2, 2, 1, 1, 1, 2, 2 };
        System.out.println("Array: " + Arrays.toString(nums2));
        System.out.println("Boyer-Moore: " + solution.majorityElement(nums2));
        System.out.println("HashMap: " + solution.majorityElementHashMap(nums2));
        System.out.println("Divide & Conquer: " + solution.majorityElementDivideConquer(nums2));

        // Test case 3: [1]
        System.out.println("\\n=== Test Case 3: [1] ===");
        int[] nums3 = { 1 };
        System.out.println("Array: " + Arrays.toString(nums3));
        System.out.println("Boyer-Moore: " + solution.majorityElement(nums3));

        // Test case 4: Large array with clear majority
        System.out.println("\\n=== Test Case 4: Large Array ===");
        int[] nums4 = { 1, 1, 1, 1, 2, 3, 1, 1, 1 };
        System.out.println("Array: " + Arrays.toString(nums4));
        System.out.println("Boyer-Moore: " + solution.majorityElement(nums4));
        System.out.println("Stack-based: " + solution.majorityElementStack(nums4));

        // Test case 5: All same elements
        System.out.println("\\n=== Test Case 5: All Same Elements ===");
        int[] nums5 = { 5, 5, 5, 5, 5 };
        System.out.println("Array: " + Arrays.toString(nums5));
        System.out.println("Boyer-Moore: " + solution.majorityElement(nums5));

        // Performance comparison simulation
        System.out.println("\\n=== Performance Analysis ===");
        System.out.println("Boyer-Moore Voting: O(n) time, O(1) space - OPTIMAL");
        System.out.println("HashMap: O(n) time, O(n) space - Good but uses extra space");
        System.out.println("Sorting: O(n log n) time, O(1) space - Simple but slower");
        System.out.println("Randomized: O(n) expected time, O(1) space - Interesting but unpredictable");
        System.out.println("Divide & Conquer: O(n log n) time, O(log n) space - Educational");
        System.out.println("Bit Manipulation: O(n) time, O(1) space - Creative approach");

        // Demonstrate Boyer-Moore algorithm step by step
        System.out.println("\\n=== Boyer-Moore Step-by-Step Demo ===");
        int[] demoArray = { 2, 2, 1, 1, 1, 2, 2 };
        System.out.println("Array: " + Arrays.toString(demoArray));

        int candidate = 0, count = 0;
        System.out.println("Step-by-step execution:");

        for (int i = 0; i < demoArray.length; i++) {
            int num = demoArray[i];
            if (count == 0) {
                candidate = num;
                System.out.printf("Step %d: num=%d, count=0 -> new candidate=%d\\n", i + 1, num, candidate);
            }
            count += (num == candidate) ? 1 : -1;
            System.out.printf("Step %d: num=%d, candidate=%d, count=%d\\n", i + 1, num, candidate, count);
        }
        System.out.println("Final result: " + candidate);

        // Verify all results
        System.out.println("\\n=== Verification ===");
        System.out.println("Is " + candidate + " actually majority in " + Arrays.toString(demoArray) + "? " +
                solution.isMajority(demoArray, candidate));
    }

}
