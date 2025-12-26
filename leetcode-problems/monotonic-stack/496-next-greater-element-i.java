import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class NextGreaterElement {
    
    // Approach 1: Brute Force
    // Time: O(n1 * n2), Space: O(1) excluding output
    public int[] nextGreaterElementBruteForce(int[] nums1, int[] nums2) {
        int[] result = new int[nums1.length];
        
        for (int i = 0; i < nums1.length; i++) {
            // Find nums1[i] in nums2
            int j = 0;
            while (j < nums2.length && nums2[j] != nums1[i]) {
                j++;
            }
            
            // Find next greater element
            result[i] = -1;
            for (int k = j + 1; k < nums2.length; k++) {
                if (nums2[k] > nums1[i]) {
                    result[i] = nums2[k];
                    break;
                }
            }
        }
        
        return result;
    }
    
    // Approach 2: HashMap + Brute Force (Optimized)
    // Time: O(n1 + n2²), Space: O(n2)
    public int[] nextGreaterElementHashMap(int[] nums1, int[] nums2) {
        // Build index map for nums2
        Map<Integer, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < nums2.length; i++) {
            indexMap.put(nums2[i], i);
        }
        
        int[] result = new int[nums1.length];
        
        for (int i = 0; i < nums1.length; i++) {
            int index = indexMap.get(nums1[i]);
            result[i] = -1;
            
            // Find next greater from index onwards
            for (int j = index + 1; j < nums2.length; j++) {
                if (nums2[j] > nums1[i]) {
                    result[i] = nums2[j];
                    break;
                }
            }
        }
        
        return result;
    }
    
    // Approach 3: Stack + HashMap (OPTIMAL)
    // Time: O(n1 + n2), Space: O(n2)
    public int[] nextGreaterElement(int[] nums1, int[] nums2) {
        // Map to store next greater element for each number in nums2
        Map<Integer, Integer> nextGreater = new HashMap<>();
        Stack<Integer> stack = new Stack<>();
        
        // Process nums2 to find next greater for all elements
        for (int num : nums2) {
            // Pop elements smaller than current
            while (!stack.isEmpty() && stack.peek() < num) {
                nextGreater.put(stack.pop(), num);
            }
            stack.push(num);
        }
        
        // Remaining elements have no next greater
        while (!stack.isEmpty()) {
            nextGreater.put(stack.pop(), -1);
        }
        
        // Build result for nums1
        int[] result = new int[nums1.length];
        for (int i = 0; i < nums1.length; i++) {
            result[i] = nextGreater.get(nums1[i]);
        }
        
        return result;
    }
    
    // Approach 4: Monotonic Stack (Alternative Implementation)
    // Time: O(n1 + n2), Space: O(n2)
    public int[] nextGreaterElementMonotonic(int[] nums1, int[] nums2) {
        Map<Integer, Integer> map = new HashMap<>();
        Stack<Integer> stack = new Stack<>();
        
        // Build monotonic decreasing stack
        // When we find a greater element, it's the NGE for stack elements
        for (int i = 0; i < nums2.length; i++) {
            while (!stack.isEmpty() && nums2[stack.peek()] < nums2[i]) {
                int idx = stack.pop();
                map.put(nums2[idx], nums2[i]);
            }
            stack.push(i);
        }
        
        // Build result
        int[] result = new int[nums1.length];
        for (int i = 0; i < nums1.length; i++) {
            result[i] = map.getOrDefault(nums1[i], -1);
        }
        
        return result;
    }
    
    // Approach 5: Precompute All Next Greater Elements
    // Time: O(n2² + n1), Space: O(n2)
    public int[] nextGreaterElementPrecompute(int[] nums1, int[] nums2) {
        Map<Integer, Integer> nextGreaterMap = new HashMap<>();
        
        // Precompute next greater for all elements in nums2
        for (int i = 0; i < nums2.length; i++) {
            nextGreaterMap.put(nums2[i], -1);
            for (int j = i + 1; j < nums2.length; j++) {
                if (nums2[j] > nums2[i]) {
                    nextGreaterMap.put(nums2[i], nums2[j]);
                    break;
                }
            }
        }
        
        // Query for nums1
        int[] result = new int[nums1.length];
        for (int i = 0; i < nums1.length; i++) {
            result[i] = nextGreaterMap.get(nums1[i]);
        }
        
        return result;
    }
    
    // Approach 6: Right to Left Stack Processing
    // Time: O(n1 + n2), Space: O(n2)
    public int[] nextGreaterElementRTL(int[] nums1, int[] nums2) {
        Map<Integer, Integer> map = new HashMap<>();
        Stack<Integer> stack = new Stack<>();
        
        // Process from right to left
        for (int i = nums2.length - 1; i >= 0; i--) {
            // Pop elements smaller than or equal to current
            while (!stack.isEmpty() && stack.peek() <= nums2[i]) {
                stack.pop();
            }
            
            // Top of stack is next greater (or empty means -1)
            map.put(nums2[i], stack.isEmpty() ? -1 : stack.peek());
            
            // Push current element
            stack.push(nums2[i]);
        }
        
        // Build result
        int[] result = new int[nums1.length];
        for (int i = 0; i < nums1.length; i++) {
            result[i] = map.get(nums1[i]);
        }
        
        return result;
    }
    
    // Helper: Visualize stack operations
    private static void visualizeStackOperations(int[] nums2) {
        System.out.println("\n=== Stack Operations Visualization ===");
        System.out.println("Array: " + Arrays.toString(nums2));
        System.out.println("\nProcessing left to right:");
        
        Map<Integer, Integer> nextGreater = new HashMap<>();
        Stack<Integer> stack = new Stack<>();
        
        for (int i = 0; i < nums2.length; i++) {
            int num = nums2[i];
            System.out.println("\nStep " + (i + 1) + ": Processing " + num);
            System.out.println("  Stack before: " + stack);
            
            List<Integer> popped = new ArrayList<>();
            while (!stack.isEmpty() && stack.peek() < num) {
                int val = stack.pop();
                popped.add(val);
                nextGreater.put(val, num);
            }
            
            if (!popped.isEmpty()) {
                System.out.println("  Popped: " + popped + " (NGE = " + num + ")");
            }
            
            stack.push(num);
            System.out.println("  Stack after:  " + stack);
        }
        
        System.out.println("\nFinal NGE map: " + nextGreater);
        System.out.println("Elements with no NGE: " + stack);
    }
    
    // Test cases
    public static void main(String[] args) {
        NextGreaterElement solver = new NextGreaterElement();
        
        // Test Case 1
        System.out.println("Test Case 1:");
        int[] nums1_1 = {4, 1, 2};
        int[] nums2_1 = {1, 3, 4, 2};
        System.out.println("nums1: " + Arrays.toString(nums1_1));
        System.out.println("nums2: " + Arrays.toString(nums2_1));
        System.out.println("Brute Force:  " + Arrays.toString(solver.nextGreaterElementBruteForce(nums1_1, nums2_1)));
        System.out.println("HashMap:      " + Arrays.toString(solver.nextGreaterElementHashMap(nums1_1, nums2_1)));
        System.out.println("Stack (LTR):  " + Arrays.toString(solver.nextGreaterElement(nums1_1, nums2_1)));
        System.out.println("Stack (RTL):  " + Arrays.toString(solver.nextGreaterElementRTL(nums1_1, nums2_1)));
        System.out.println("Expected:     [-1, 3, -1]");
        
        // Test Case 2
        System.out.println("\nTest Case 2:");
        int[] nums1_2 = {2, 4};
        int[] nums2_2 = {1, 2, 3, 4};
        System.out.println("nums1: " + Arrays.toString(nums1_2));
        System.out.println("nums2: " + Arrays.toString(nums2_2));
        System.out.println("Result:   " + Arrays.toString(solver.nextGreaterElement(nums1_2, nums2_2)));
        System.out.println("Expected: [3, -1]");
        
        // Test Case 3: Descending order
        System.out.println("\nTest Case 3: Descending Order");
        int[] nums1_3 = {5, 4, 3};
        int[] nums2_3 = {5, 4, 3, 2, 1};
        System.out.println("nums1: " + Arrays.toString(nums1_3));
        System.out.println("nums2: " + Arrays.toString(nums2_3));
        System.out.println("Result:   " + Arrays.toString(solver.nextGreaterElement(nums1_3, nums2_3)));
        System.out.println("Expected: [-1, -1, -1]");
        
        // Test Case 4: Ascending order
        System.out.println("\nTest Case 4: Ascending Order");
        int[] nums1_4 = {1, 2, 3};
        int[] nums2_4 = {1, 2, 3, 4, 5};
        System.out.println("nums1: " + Arrays.toString(nums1_4));
        System.out.println("nums2: " + Arrays.toString(nums2_4));
        System.out.println("Result:   " + Arrays.toString(solver.nextGreaterElement(nums1_4, nums2_4)));
        System.out.println("Expected: [2, 3, 4]");
        
        // Test Case 5: Complex pattern
        System.out.println("\nTest Case 5: Complex Pattern");
        int[] nums1_5 = {3, 1, 5, 7};
        int[] nums2_5 = {1, 3, 4, 2, 5, 7, 6};
        System.out.println("nums1: " + Arrays.toString(nums1_5));
        System.out.println("nums2: " + Arrays.toString(nums2_5));
        System.out.println("Result:   " + Arrays.toString(solver.nextGreaterElement(nums1_5, nums2_5)));
        System.out.println("Expected: [4, 3, 7, -1]");
        
        // Visualization
        visualizeStackOperations(nums2_1);
        
        // Step-by-step for one example
        System.out.println("\n=== Detailed Step-by-Step: nums2 = [1,3,4,2] ===");
        stepByStepTrace(nums2_1);
        
        // Algorithm comparison
        System.out.println("\n=== Algorithm Comparison ===");
        compareAlgorithms();
        
        // Performance test
        System.out.println("\n=== Performance Test ===");
        performanceTest();
    }
    
    private static void stepByStepTrace(int[] nums2) {
        Map<Integer, Integer> map = new HashMap<>();
        Stack<Integer> stack = new Stack<>();
        
        System.out.println("┌────────┬─────────────────┬──────────────┬─────────────────┐");
        System.out.println("│ Number │ Action          │ Stack        │ NGE Found       │");
        System.out.println("├────────┼─────────────────┼──────────────┼─────────────────┤");
        
        for (int num : nums2) {
            StringBuilder ngeFound = new StringBuilder();
            
            while (!stack.isEmpty() && stack.peek() < num) {
                int val = stack.pop();
                map.put(val, num);
                if (ngeFound.length() > 0) ngeFound.append(", ");
                ngeFound.append(val).append("→").append(num);
            }
            
            stack.push(num);
            
            System.out.printf("│   %d    │ %-15s │ %-12s │ %-15s │%n",
                            num,
                            "Push & process",
                            stack.toString(),
                            ngeFound.length() > 0 ? ngeFound.toString() : "-");
        }
        
        System.out.println("└────────┴─────────────────┴──────────────┴─────────────────┘");
        
        System.out.println("\nFinal NGE Map:");
        for (int num : nums2) {
            System.out.println("  " + num + " → " + map.getOrDefault(num, -1));
        }
    }
    
    private static void compareAlgorithms() {
        System.out.println("┌──────────────────────┬──────────────┬──────────────┬─────────────┐");
        System.out.println("│ Approach             │ Time         │ Space        │ Notes       │");
        System.out.println("├──────────────────────┼──────────────┼──────────────┼─────────────┤");
        System.out.println("│ Brute Force          │ O(n1 × n2)   │ O(1)         │ Simple      │");
        System.out.println("│ HashMap + BF         │ O(n1 + n2²)  │ O(n2)        │ Better      │");
        System.out.println("│ Stack (LTR)          │ O(n1 + n2)   │ O(n2)        │ Optimal     │");
        System.out.println("│ Stack (RTL)          │ O(n1 + n2)   │ O(n2)        │ Optimal     │");
        System.out.println("│ Precompute           │ O(n2² + n1)  │ O(n2)        │ Not ideal   │");
        System.out.println("└──────────────────────┴──────────────┴──────────────┴─────────────┘");
    }
    
    private static void performanceTest() {
        NextGreaterElement solver = new NextGreaterElement();
        
        int[] nums1 = new int[500];
        int[] nums2 = new int[1000];
        
        for (int i = 0; i < nums2.length; i++) {
            nums2[i] = i;
        }
        for (int i = 0; i < nums1.length; i++) {
            nums1[i] = i * 2;
        }
        
        // Brute Force
        long start = System.nanoTime();
        solver.nextGreaterElementBruteForce(nums1, nums2);
        long end = System.nanoTime();
        System.out.println("Brute Force: " + (end - start) / 1_000_000.0 + " ms");
        
        // HashMap
        start = System.nanoTime();
        solver.nextGreaterElementHashMap(nums1, nums2);
        end = System.nanoTime();
        System.out.println("HashMap:     " + (end - start) / 1_000_000.0 + " ms");
        
        // Stack (Optimal)
        start = System.nanoTime();
        solver.nextGreaterElement(nums1, nums2);
        end = System.nanoTime();
        System.out.println("Stack:       " + (end - start) / 1_000_000.0 + " ms");
    }
}

/*
DETAILED EXPLANATION:

PROBLEM: Next Greater Element I

Given:
- nums1 (subset of nums2)
- nums2 (main array)

Find: For each element in nums1, find its next greater element in nums2

Next Greater Element (NGE):
- First element to the RIGHT that is GREATER
- If no such element exists, return -1

EXAMPLE WALKTHROUGH:

nums1 = [4,1,2], nums2 = [1,3,4,2]

For 4: Position in nums2 is index 2
  - Check right: 2
  - 2 < 4, so no NGE
  - Result: -1

For 1: Position in nums2 is index 0
  - Check right: 3, 4, 2
  - First greater: 3
  - Result: 3

For 2: Position in nums2 is index 3
  - Check right: nothing
  - Result: -1

Output: [-1, 3, -1]

KEY INSIGHT: MONOTONIC STACK

The optimal solution uses a monotonic decreasing stack.

Why? Because we need to:
1. Track elements waiting for their NGE
2. Quickly identify when we find a greater element
3. Process each element only once

STACK APPROACH (LEFT TO RIGHT):

Maintain a stack of elements in DECREASING order.

When we encounter a new element:
- If it's GREATER than stack top → it's the NGE for stack elements!
- Pop all smaller elements and record their NGE
- Push current element

Example: [1, 3, 4, 2]

Step 1: Process 1
  Stack: [1]
  
Step 2: Process 3
  3 > 1, so 1's NGE is 3
  Pop 1, record: 1 → 3
  Stack: [3]
  
Step 3: Process 4
  4 > 3, so 3's NGE is 4
  Pop 3, record: 3 → 4
  Stack: [4]
  
Step 4: Process 2
  2 < 4, no popping
  Stack: [4, 2]
  
Elements still in stack (4, 2) have no NGE → -1

ALGORITHM:

1. Create a HashMap to store NGE for each element
2. Create a stack (monotonic decreasing)
3. For each element in nums2:
   a. While stack not empty AND stack.top < current:
      - Pop element
      - Record: popped_element → current
   b. Push current element
4. For each element in nums1:
   - Query the HashMap

STACK VISUALIZATION:

Array: [1, 3, 4, 2]

i=0, num=1:
  Stack: [] → [1]
  NGE found: none

i=1, num=3:
  3 > 1, pop 1
  Stack: [1] → [3]
  NGE found: 1→3

i=2, num=4:
  4 > 3, pop 3
  Stack: [3] → [4]
  NGE found: 3→4

i=3, num=2:
  2 < 4, no pop
  Stack: [4] → [4, 2]
  NGE found: none

Final: {1→3, 3→4, 4→-1, 2→-1}

WHY MONOTONIC DECREASING STACK?

The stack maintains elements in DECREASING order:
- When we find a larger element, it's the NGE for ALL smaller elements in stack
- Elements remain in stack if no NGE found yet
- This ensures each element is pushed/popped ONCE → O(n)

ALTERNATIVE: RIGHT TO LEFT PROCESSING

Instead of recording NGEs when we find them, we can:
1. Process from RIGHT to LEFT
2. Maintain stack of elements seen so far
3. For each element, stack top is its NGE

Example: [1, 3, 4, 2]

i=3, num=2:
  Stack empty, NGE = -1
  Stack: [2]
  
i=2, num=4:
  4 > 2, pop 2
  Stack empty, NGE = -1
  Stack: [4]
  
i=1, num=3:
  3 < 4, NGE = 4
  Stack: [4, 3]
  
i=0, num=1:
  1 < 3, NGE = 3
  Stack: [4, 3, 1]

Result: {1→3, 3→4, 4→-1, 2→-1}

Both approaches work! Choose based on preference.

COMPLEXITY ANALYSIS:

Optimal Approach (Stack):
Time: O(n1 + n2)
  - O(n2) to build NGE map (each element pushed/popped once)
  - O(n1) to query results
Space: O(n2)
  - HashMap: O(n2)
  - Stack: O(n2) worst case

Brute Force:
Time: O(n1 × n2)
  - For each element in nums1: O(n1)
  - Find in nums2 and search right: O(n2)
Space: O(1) excluding output

EDGE CASES:

1. All elements decreasing: [5,4,3,2,1]
   - No NGE for any element → all -1
   
2. All elements increasing: [1,2,3,4,5]
   - Each has NGE except last
   
3. Single element: nums1=[1], nums2=[1]
   - No element to right → -1
   
4. nums1 = nums2: Query all elements
   
5. Large differences: [1, 1000000, 2]
   - Stack handles efficiently

COMMON MISTAKES:

1. Forgetting to handle no NGE case
2. Wrong stack condition (≤ vs <)
3. Not maintaining monotonic property
4. Inefficient HashMap lookup
5. Not clearing stack properly

VARIATIONS:

1. Next Greater Element II (circular array)
2. Next Smaller Element
3. Previous Greater Element
4. Next Greater Frequency

INTERVIEW TIPS:

1. Start with brute force
2. Identify optimization opportunity
3. Explain monotonic stack concept
4. Trace through example
5. Discuss complexity
6. Handle edge cases
7. Compare LTR vs RTL approaches

WHEN TO USE MONOTONIC STACK:

- Finding next/previous greater/smaller
- Problems involving "first element that..."
- Range queries
- Histogram problems
- Temperature problems

RELATED PROBLEMS:

1. Daily Temperatures
2. Largest Rectangle in Histogram
3. Trapping Rain Water
4. Stock Span Problem
5. Online Stock Span

This problem teaches fundamental monotonic stack technique!
*/
