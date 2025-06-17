import java.util.*;
/*
 * Given an array nums of distinct integers, return all the possible
 * permutations. You can return the answer in any order.
 * 
 * Example 1:
 * Input: nums = [1,2,3]
 * Output: [[1,2,3],[1,3,2],[2,1,3],[2,3,1],[3,1,2],[3,2,1]]
 * 
 * Example 2:
 * Input: nums = [0,1]
 * Output: [[0,1],[1,0]]
 * 
 * Example 3:
 * Input: nums = [1]
 * Output: [[1]]
 */

class Permutations {

    // Solution 1: Classic Backtracking with Used Array
    public List<List<Integer>> permute1(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        boolean[] used = new boolean[nums.length];
        backtrack(nums, used, new ArrayList<>(), result);
        return result;
    }

    private void backtrack(int[] nums, boolean[] used, List<Integer> current,
            List<List<Integer>> result) {
        // Base case: if permutation is complete
        if (current.size() == nums.length) {
            result.add(new ArrayList<>(current));
            return;
        }

        // Try each unused number
        for (int i = 0; i < nums.length; i++) {
            if (!used[i]) {
                used[i] = true;
                current.add(nums[i]);
                backtrack(nums, used, current, result);
                current.remove(current.size() - 1);
                used[i] = false;
            }
        }
    }

    // Solution 2: Backtracking with Swapping (No Extra Space)
    public List<List<Integer>> permute2(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        backtrackSwap(nums, 0, result);
        return result;
    }

    private void backtrackSwap(int[] nums, int start, List<List<Integer>> result) {
        // Base case: if we've placed all elements
        if (start == nums.length) {
            List<Integer> permutation = new ArrayList<>();
            for (int num : nums) {
                permutation.add(num);
            }
            result.add(permutation);
            return;
        }

        // Try placing each remaining element at current position
        for (int i = start; i < nums.length; i++) {
            swap(nums, start, i);
            backtrackSwap(nums, start + 1, result);
            swap(nums, start, i); // backtrack
        }
    }

    private void swap(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }

    // Solution 3: Using Collections.swap (Cleaner)
    public List<List<Integer>> permute3(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> numsList = new ArrayList<>();
        for (int num : nums) {
            numsList.add(num);
        }
        backtrackList(numsList, 0, result);
        return result;
    }

    private void backtrackList(List<Integer> nums, int start, List<List<Integer>> result) {
        if (start == nums.size()) {
            result.add(new ArrayList<>(nums));
            return;
        }

        for (int i = start; i < nums.size(); i++) {
            Collections.swap(nums, start, i);
            backtrackList(nums, start + 1, result);
            Collections.swap(nums, start, i);
        }
    }

    // Solution 4: Iterative Approach
    public List<List<Integer>> permute4(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        result.add(new ArrayList<>());

        for (int num : nums) {
            List<List<Integer>> newResult = new ArrayList<>();

            for (List<Integer> permutation : result) {
                // Insert current number at each possible position
                for (int i = 0; i <= permutation.size(); i++) {
                    List<Integer> newPermutation = new ArrayList<>(permutation);
                    newPermutation.add(i, num);
                    newResult.add(newPermutation);
                }
            }
            result = newResult;
        }

        return result;
    }

    // Solution 5: Using Heap's Algorithm
    public List<List<Integer>> permute5(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        heapPermute(nums, nums.length, result);
        return result;
    }

    private void heapPermute(int[] nums, int size, List<List<Integer>> result) {
        if (size == 1) {
            List<Integer> permutation = new ArrayList<>();
            for (int num : nums) {
                permutation.add(num);
            }
            result.add(permutation);
            return;
        }

        for (int i = 0; i < size; i++) {
            heapPermute(nums, size - 1, result);

            // If size is odd, swap first and last element
            if (size % 2 == 1) {
                swap(nums, 0, size - 1);
            } else {
                // If size is even, swap ith and last element
                swap(nums, i, size - 1);
            }
        }
    }

    // Solution 6: Using Next Permutation Algorithm
    public List<List<Integer>> permute6(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(nums); // Start with lexicographically smallest

        do {
            List<Integer> permutation = new ArrayList<>();
            for (int num : nums) {
                permutation.add(num);
            }
            result.add(permutation);
        } while (nextPermutation(nums));

        return result;
    }

    private boolean nextPermutation(int[] nums) {
        int i = nums.length - 2;

        // Find the first decreasing element from right
        while (i >= 0 && nums[i] >= nums[i + 1]) {
            i--;
        }

        if (i < 0)
            return false; // No next permutation

        // Find the smallest element greater than nums[i] from right
        int j = nums.length - 1;
        while (nums[j] <= nums[i]) {
            j--;
        }

        swap(nums, i, j);
        reverse(nums, i + 1);
        return true;
    }

    private void reverse(int[] nums, int start) {
        int end = nums.length - 1;
        while (start < end) {
            swap(nums, start, end);
            start++;
            end--;
        }
    }

    // Solution 7: Using Library Functions (Not recommended for interviews)
    public List<List<Integer>> permute7(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> numsList = new ArrayList<>();
        for (int num : nums) {
            numsList.add(num);
        }

        generatePermutations(numsList, new ArrayList<>(), result);
        return result;
    }

    private void generatePermutations(List<Integer> remaining, List<Integer> current,
            List<List<Integer>> result) {
        if (remaining.isEmpty()) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = 0; i < remaining.size(); i++) {
            Integer num = remaining.remove(i);
            current.add(num);
            generatePermutations(remaining, current, result);
            current.remove(current.size() - 1);
            remaining.add(i, num);
        }
    }

    // Solution 8: Using Streams (Java 8+, not efficient but elegant)
    public List<List<Integer>> permute8(int[] nums) {
        if (nums.length == 1) {
            return Arrays.asList(Arrays.asList(nums[0]));
        }

        List<List<Integer>> result = new ArrayList<>();
        for (int i = 0; i < nums.length; i++) {
            int[] remaining = new int[nums.length - 1];
            int idx = 0;
            for (int j = 0; j < nums.length; j++) {
                if (j != i) {
                    remaining[idx++] = nums[j];
                }
            }

            List<List<Integer>> subPermutations = permute8(remaining);
            for (List<Integer> subPerm : subPermutations) {
                List<Integer> newPerm = new ArrayList<>();
                newPerm.add(nums[i]);
                newPerm.addAll(subPerm);
                result.add(newPerm);
            }
        }

        return result;
    }

    // Test method
    public static void main(String[] args) {
        Permutations p = new Permutations();

        System.out.println("Input: [1,2,3]");
        System.out.println("Output: " + p.permute1(new int[] { 1, 2, 3 }));

        System.out.println("\nInput: [0,1]");
        System.out.println("Output: " + p.permute1(new int[] { 0, 1 }));

        System.out.println("\nInput: [1]");
        System.out.println("Output: " + p.permute1(new int[] { 1 }));

        // Performance comparison
        System.out.println("\n=== Performance Comparison (n=6) ===");
        int[] testArray = { 1, 2, 3, 4, 5, 6 };
        long start, end;

        start = System.nanoTime();
        List<List<Integer>> result1 = p.permute1(testArray.clone());
        end = System.nanoTime();
        System.out.println("Backtracking with used array: " + result1.size() +
                " permutations in " + (end - start) / 1000000.0 + " ms");

        start = System.nanoTime();
        List<List<Integer>> result2 = p.permute2(testArray.clone());
        end = System.nanoTime();
        System.out.println("Backtracking with swapping: " + result2.size() +
                " permutations in " + (end - start) / 1000000.0 + " ms");

        start = System.nanoTime();
        List<List<Integer>> result4 = p.permute4(testArray.clone());
        end = System.nanoTime();
        System.out.println("Iterative approach: " + result4.size() +
                " permutations in " + (end - start) / 1000000.0 + " ms");

        start = System.nanoTime();
        List<List<Integer>> result6 = p.permute6(testArray.clone());
        end = System.nanoTime();
        System.out.println("Next permutation algorithm: " + result6.size() +
                " permutations in " + (end - start) / 1000000.0 + " ms");
    }

}

/*
 * Complexity Analysis:
 * 
 * Time Complexity: O(n! * n) for all solutions
 * - n! permutations to generate
 * - n time to copy each permutation
 * 
 * Space Complexity:
 * - Result space: O(n! * n) to store all permutations
 * - Auxiliary space varies:
 * Solution 1: O(n) for used array + O(n) recursion
 * Solution 2: O(n) recursion only
 * Solution 4: O(n! * n) for intermediate results
 * Solution 6: O(1) auxiliary space
 * 
 * Solution Comparison:
 * 1. Backtracking with used array: Most intuitive, easy to understand
 * 2. Backtracking with swapping: More space-efficient, modifies input
 * 3. Collections.swap: Cleaner code, similar to solution 2
 * 4. Iterative: Good for understanding step-by-step building
 * 5. Heap's algorithm: Classic algorithm, generates all permutations
 * 6. Next permutation: Generates in lexicographic order
 * 7. Library approach: Uses list operations, less efficient
 * 8. Recursive divide-and-conquer: Elegant but less efficient
 * 
 * Best for interviews: Solution 1 (most clear) or Solution 2 (space-efficient)
 * Most efficient: Solution 2 (swapping) or Solution 6 (next permutation)
 */
