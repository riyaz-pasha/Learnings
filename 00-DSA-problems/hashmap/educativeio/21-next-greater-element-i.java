/**
 * ============================================================================
 * L E E T C O D E   4 9 6 :   N E X T   G R E A T E R   E L E M E N T   I
 * ============================================================================
 *
 * ## 1. Clarifying questions
 * 1. "Can the arrays contain duplicate numbers?"
 *    (Crucial for mapping logic: Constraints state all integers are distinct. If duplicates existed, we'd have to store indices in our stack and map, not the values themselves).
 * 2. "Is nums1 always a strict subset of nums2, or could it contain elements not in nums2?"
 *    (The prompt guarantees nums1 elements all appear in nums2, preventing NullPointerExceptions during lookups).
 * 3. "Are we optimizing for time or space?"
 *    (Since nums2 length is up to 1000, an O(N*M) brute force passes easily. To show senior-level competency, we must aim for O(N+M) time complexity).
 * 4. "What is the maximum value of the integers?"
 *    (The values are between 0 and 10^4. This bounded range means we can swap a heavy HashMap for a lightning-fast primitive array).
 *
 *
 * ## 2. The reasoning journey
 * > Core Constraint: For every element in `nums1`, we need to find its location in `nums2` and then scan right to find the first larger number. The bottleneck is the repeated scanning of `nums2`.
 *
 * ### Approach 1: Nested Loops (The Brute Force)
 * 1. What I'd naturally try: For each number in `nums1`, iterate through `nums2` until I find it. Once found, continue iterating right until I see a strictly greater number. If the loop finishes without finding one, record -1.
 * 2. Why it works: It perfectly executes the definition of "next greater element to the right."
 * 3. Why it's too costly:
 *    - Time Complexity: O(N * M) — where N is the length of `nums1` and M is the length of `nums2`. In the worst case (a strictly decreasing array), we scan to the end of `nums2` for every single element.
 *    - Space Complexity: O(1) auxiliary — we only use loop indices and a variable or two.
 * 4. What work is being repeated: We are repeatedly walking over the exact same elements in `nums2`. If we see `[..., 3, 2, 1, 5]`, `5` is the next greater element for `3`, `2`, and `1`. But the brute force re-discovers this fact three separate times.
 * 5. What property removes the bottleneck: "Deferred resolution." If we process `nums2` just once, keeping a waiting list of numbers that haven't found their next greater element yet, a single large number can resolve multiple waiting numbers instantly.
 *
 * ### Approach 2: Monotonic Stack + HashMap (The Algorithmic Optimum)
 * 1. What I'd naturally try: I'll read `nums2` from left to right. I'll push numbers onto a Stack. When I encounter a new number, I'll check if it's larger than the top of the Stack. If it is, this new number is the "next greater" for the Stack's top. I'll pop the Stack, record the answer in a `HashMap`, and repeat until the Stack top is larger. Finally, I push the new number.
 * 2. Why it works: The stack inherently keeps elements in strictly decreasing order (a Monotonic Decreasing Stack). The moment a larger element arrives, it resolves the fate of all smaller elements trapped in the stack.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N + M) — because every element in `nums2` is pushed to the stack exactly once and popped at most once. Looking up the `nums1` answers in the map takes O(1) per element.
 *    - Space Complexity: O(M) auxiliary — because in the worst case (a decreasing array), all M elements of `nums2` are pushed onto the Stack and stored in the HashMap.
 * 4. What work is being repeated: We are doing heavy object allocations (Integer wrappers, Map Nodes) for a domain that is strictly bounded between 0 and 10^4.
 * 5. What property removes the bottleneck: A direct-mapped array.
 *
 * ### Approach 3: Monotonic Stack + Array Map (The Hardware-Friendly Optimum)
 * 1. What I'd naturally try: Replace `HashMap<Integer, Integer>` with `int[10001]`. When `5` resolves `3`, I just do `map[3] = 5`.
 * 2. Why it works: Array indices are direct memory addresses. It executes the exact O(N+M) logic but skips the Java garbage collector and hashing overhead entirely.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N + M) — linear single-pass.
 *    - Space Complexity: O(M + K) — where K is the maximum possible value (10^4). Since K is a small constant, this is effectively O(M) for the stack, plus a flat ~40KB for the array.
 *
 * > **Interview Strategy:** Start by writing out the Brute Force verbally. Then say, "We can avoid O(N*M) by processing nums2 exactly once using a Monotonic Stack to defer resolution." Write Approach 2 (HashMap) as it's the most canonical. If they ask for further optimization, drop the HashMap for Approach 3 (Array Map).
 *
 *
 * ## 3. Edge cases
 * - Strictly decreasing array: `[5, 4, 3, 2, 1]`. No element will ever pop from the stack. The map will be empty. We must ensure our lookup gracefully falls back to `-1`.
 * - The largest element is at the end: `[1, 2, 3, 10]`. `10` pops everything in one massive chain reaction.
 * - Array lengths are equal (nums1 == nums2): Handled naturally.
 *
 *
 * ## 4. Key Insight & Dry Run
 *
 * **Key Insight: "Waiting for a Taller Person"**
 * Imagine people standing in a line facing right. You can only see someone to your right if they are taller than you. 
 * A Monotonic Decreasing Stack perfectly models this. We keep a stack of "short people" waiting. The moment a "tall person" walks in, they instantly become the answer for everyone shorter than them at the top of the stack.
 *
 * **Dry Run of Approach 2:**
 * `nums1 = [4, 1, 2]`, `nums2 = [1, 3, 4, 2]`
 * Init: `Stack = []`, `Map = {}`
 *
 * 1. Process `nums2`:
 *    - `num = 1`: Stack is empty. Push `1`. Stack: `[1]`
 *    - `num = 3`: `3` > Stack.peek() (`1`). 
 *                 Pop `1`. Map.put(1, 3). 
 *                 Push `3`. Stack: `[3]`
 *    - `num = 4`: `4` > Stack.peek() (`3`). 
 *                 Pop `3`. Map.put(3, 4). 
 *                 Push `4`. Stack: `[4]`
 *    - `num = 2`: `2` < Stack.peek() (`4`).
 *                 Push `2`. Stack: `[4, 2]`
 * 2. Build Result for `nums1`:
 *    - `4` -> not in Map. Result = -1
 *    - `1` -> Map yields `3`. Result = 3
 *    - `2` -> not in Map. Result = -1
 * Result: `[-1, 3, -1]`
 *
 *
 * ## 5. Follow-ups
 * - **Q:** What if the array was circular (i.e., you can loop around to the front to find a greater element)?
 *   **A:** This is "Next Greater Element II". We would simulate a circular array by running the loop for `2 * n - 1` iterations, and use the modulo operator `i % n` to wrap around.
 * - **Q:** What if we needed to know the *distance* to the next greater element, rather than its value?
 *   **A:** This is "Daily Temperatures". Instead of pushing the array *values* onto the stack, we push their *indices*. When we find a greater element at index `curr`, the distance is simply `curr - popped_index`.
 * - **Q:** What if `nums2` contained duplicate values?
 *   **A:** The Map would fail because keys must be unique. We would have to map `index -> next_greater_value`, requiring us to push indices onto the stack instead of values.
 *
 * ============================================================================
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class NextGreaterElementStudy {

    interface NextGreaterSolver {
        int[] nextGreaterElement(int[] nums1, int[] nums2);
    }

    // ========================================================================
    // APPROACH 2: MONOTONIC STACK + HASHMAP (The Standard Optimal Solution)
    // ========================================================================
    static class MonotonicStackSolver implements NextGreaterSolver {
        @Override
        public int[] nextGreaterElement(int[] nums1, int[] nums2) {
            // Stack keeps elements that haven't found their next greater element yet.
            // Concrete example: Stack holds [5, 3, 2]. They are waiting for a number > 2.
            Stack<Integer> waitingList = new Stack<>();
            
            // Maps a number to its next greater element.
            // Concrete example: {3: 8} means the next greater element for 3 is 8.
            Map<Integer, Integer> nextGreaterMap = new HashMap<>();

            // 1. Process nums2 once to figure out the next greater element for EVERY number
            for (int currentNum : nums2) {
                // While the current number is taller than the guy at the top of the stack,
                // it resolves their wait! Pop them and record the answer.
                while (!waitingList.isEmpty() && currentNum > waitingList.peek()) {
                    int resolvedNum = waitingList.pop();
                    nextGreaterMap.put(resolvedNum, currentNum);
                }
                
                // Now the current number gets in line to wait for someone taller than it.
                waitingList.push(currentNum);
            }

            // Elements left in the stack never found a greater element. 
            // They don't get added to the map.

            // 2. Look up the answers for the specific subset of numbers in nums1
            int[] result = new int[nums1.length];
            for (int i = 0; i < nums1.length; i++) {
                // getOrDefault handles the case where the number never found a greater element
                result[i] = nextGreaterMap.getOrDefault(nums1[i], -1);
            }

            return result;
        }
    }

    // ========================================================================
    // APPROACH 3: MONOTONIC STACK + ARRAY MAP (The Hardware-Friendly Optimum)
    // ========================================================================
    static class ArrayMapSolver implements NextGreaterSolver {
        @Override
        public int[] nextGreaterElement(int[] nums1, int[] nums2) {
            // Constraint: 0 <= nums[i] <= 10^4
            // We use a flat array instead of a HashMap to bypass Object overhead.
            int[] nextGreaterMap = new int[10001];
            
            // Initialize with -1 so unanswered elements naturally yield -1
            Arrays.fill(nextGreaterMap, -1);
            
            // Using a primitive array as a stack is even faster than java.util.Stack
            int[] stack = new int[nums2.length];
            int top = -1; // Points to the top of our stack

            for (int currentNum : nums2) {
                while (top >= 0 && currentNum > stack[top]) {
                    int resolvedNum = stack[top--]; // Pop
                    nextGreaterMap[resolvedNum] = currentNum;
                }
                stack[++top] = currentNum; // Push
            }

            int[] result = new int[nums1.length];
            for (int i = 0; i < nums1.length; i++) {
                result[i] = nextGreaterMap[nums1[i]];
            }

            return result;
        }
    }

    // ========================================================================
    // APPROACH 1: BRUTE FORCE (Conceptual Baseline)
    // ========================================================================
    static class BruteForceSolver implements NextGreaterSolver {
        @Override
        public int[] nextGreaterElement(int[] nums1, int[] nums2) {
            int[] result = new int[nums1.length];
            
            for (int i = 0; i < nums1.length; i++) {
                int target = nums1[i];
                int nextGreater = -1;
                boolean found = false;
                
                // Scan nums2 to find the target, then keep going right
                for (int j = 0; j < nums2.length; j++) {
                    if (nums2[j] == target) {
                        found = true;
                    }
                    if (found && nums2[j] > target) {
                        nextGreater = nums2[j];
                        break; // Stop at the FIRST greater element
                    }
                }
                
                result[i] = nextGreater;
            }
            
            return result;
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        NextGreaterSolver[] solvers = {
            new MonotonicStackSolver(),
            new ArrayMapSolver(),
            new BruteForceSolver()
        };

        // Format: {nums1, nums2, expected_output}
        int[][][] testCases = {
            {{4, 1, 2}, {1, 3, 4, 2}, {-1, 3, -1}},          // Standard example 1
            {{2, 4}, {1, 2, 3, 4}, {3, -1}},                 // Standard example 2
            {{5, 4, 3}, {5, 4, 3, 2, 1}, {-1, -1, -1}},      // Strictly decreasing
            {{1, 2, 3}, {1, 2, 3, 10}, {2, 3, 10}},          // Massive resolution at the end
            {{1}, {1}, {-1}}                                 // Single element
        };

        for (NextGreaterSolver solver : solvers) {
            System.out.println("Testing " + solver.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (int i = 0; i < testCases.length; i++) {
                int[] nums1 = testCases[i][0];
                int[] nums2 = testCases[i][1];
                int[] expected = testCases[i][2];
                
                int[] result = solver.nextGreaterElement(nums1, nums2);
                
                if (!Arrays.equals(result, expected)) {
                    System.out.printf("  [FAIL] Test %d. Expected: %s, Got: %s%n", 
                        i + 1, Arrays.toString(expected), Arrays.toString(result));
                    allPassed = false;
                }
            }
            
            if (allPassed) {
                System.out.println("  [SUCCESS] All test cases passed!\n");
            }
        }
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * - Core pattern: Monotonic Decreasing Stack.
 * - Key observation: Processing the array left-to-right with a stack allows 
 *   every element to be pushed and popped exactly once, reducing O(N^2) 
 *   repeated scanning to an O(N) single pass.
 * - What's worth memorizing: The "While larger, pop and resolve" while-loop block:
 *   `while (!stack.isEmpty() && curr > stack.peek()) { map.put(stack.pop(), curr); }`
 * - Most common trap: Trying to process `nums1` first. You must process `nums2` 
 *   first in its entirety to build a complete dictionary of answers, then map `nums1`.
 * - Mental trigger: "Next greater/smaller element?" -> "Monotonic Stack."
 * ============================================================================
 */



import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

class Solution {

    public int[] nextGreaterElement(int[] nums1, int[] nums2) {

        // Maps each value to its next greater element.
        Map<Integer, Integer> nextGreater = new HashMap<>();

        // Stores elements whose next greater element is not known yet.
        // The stack is monotonic decreasing from bottom to top.
        Deque<Integer> stack = new ArrayDeque<>();

        // Step 1: Precompute next greater elements for nums2.
        for (int current : nums2) {

            // If current is greater than the top, current is the
            // first greater element to the right of that stack element.
            while (!stack.isEmpty() && current > stack.peek()) {
                int smaller = stack.pop();
                nextGreater.put(smaller, current);
            }

            // Current might find its next greater element later.
            stack.push(current);
        }

        // Step 2: Remaining elements have no greater element to the right.
        while (!stack.isEmpty()) {
            nextGreater.put(stack.pop(), -1);
        }

        // Step 3: Build the answer in nums1's original order.
        int[] answer = new int[nums1.length];

        for (int i = 0; i < nums1.length; i++) {
            answer[i] = nextGreater.get(nums1[i]);
        }

        return answer;
    }
}

