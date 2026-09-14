import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Stack;

class NextGreaterElementII {
    
    // Approach 1: Brute Force with Circular Logic
    // Time: O(n²), Space: O(1) excluding output
    public int[] nextGreaterElementsBruteForce(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        
        for (int i = 0; i < n; i++) {
            result[i] = -1;
            
            // Search circularly: from i+1 to i+n-1
            for (int j = 1; j < n; j++) {
                int idx = (i + j) % n;  // Circular indexing
                if (nums[idx] > nums[i]) {
                    result[i] = nums[idx];
                    break;
                }
            }
        }
        
        return result;
    }
    
    // Approach 2: Stack with Double Pass (OPTIMAL)
    // Time: O(n), Space: O(n)
    public int[] nextGreaterElements(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        
        Stack<Integer> stack = new Stack<>();  // Stores indices
        
        // Process array TWICE to simulate circular behavior
        for (int i = 0; i < 2 * n; i++) {
            int idx = i % n;  // Actual index in nums
            
            // Pop elements smaller than current
            while (!stack.isEmpty() && nums[stack.peek()] < nums[idx]) {
                result[stack.pop()] = nums[idx];
            }
            
            // Only push indices in first pass
            if (i < n) {
                stack.push(idx);
            }
        }
        
        return result;
    }
    
    // Approach 3: Stack with Explicit Tracking
    // More verbose but clearer logic
    public int[] nextGreaterElementsExplicit(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        
        Stack<Integer> stack = new Stack<>();
        
        // First pass: normal traversal
        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && nums[stack.peek()] < nums[i]) {
                result[stack.pop()] = nums[i];
            }
            stack.push(i);
        }
        
        // Second pass: circular traversal for remaining elements
        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && nums[stack.peek()] < nums[i]) {
                int idx = stack.pop();
                // Only update if not already found
                if (result[idx] == -1) {
                    result[idx] = nums[i];
                }
            }
            // Don't push in second pass
        }
        
        return result;
    }
    
    // Approach 4: Right to Left Processing
    // Time: O(n), Space: O(n)
    public int[] nextGreaterElementsRTL(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Stack<Integer> stack = new Stack<>();
        
        // Process twice from right to left
        for (int i = 2 * n - 1; i >= 0; i--) {
            int idx = i % n;
            
            // Pop elements <= current
            while (!stack.isEmpty() && stack.peek() <= nums[idx]) {
                stack.pop();
            }
            
            // Set result only in second pass
            if (i < n) {
                result[idx] = stack.isEmpty() ? -1 : stack.peek();
            }
            
            stack.push(nums[idx]);
        }
        
        return result;
    }
    
    // Approach 5: Monotonic Stack with Index Tracking
    // Uses indices throughout for clarity
    public int[] nextGreaterElementsWithIndices(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        
        Stack<Integer> stack = new Stack<>();
        
        for (int i = 0; i < 2 * n; i++) {
            int idx = i % n;
            
            while (!stack.isEmpty() && nums[stack.peek()] < nums[idx]) {
                int prevIdx = stack.pop();
                if (result[prevIdx] == -1) {  // First time finding NGE
                    result[prevIdx] = nums[idx];
                }
            }
            
            if (i < n) {
                stack.push(idx);
            }
        }
        
        return result;
    }
    
    // Approach 6: Using Deque for Flexibility
    // Time: O(n), Space: O(n)
    public int[] nextGreaterElementsDeque(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        
        Deque<Integer> deque = new ArrayDeque<>();
        
        for (int i = 0; i < 2 * n; i++) {
            int idx = i % n;
            
            while (!deque.isEmpty() && nums[deque.peekLast()] < nums[idx]) {
                result[deque.pollLast()] = nums[idx];
            }
            
            if (i < n) {
                deque.offerLast(idx);
            }
        }
        
        return result;
    }
    
    // Helper: Visualize the circular array concept
    private static void visualizeCircular(int[] nums) {
        System.out.println("\n=== Circular Array Visualization ===");
        System.out.print("Array: ");
        for (int num : nums) {
            System.out.print(num + " → ");
        }
        System.out.println("(back to " + nums[0] + ")");
        
        System.out.println("\nCircular indices:");
        for (int i = 0; i < nums.length + 3; i++) {
            System.out.printf("i=%d → nums[%d] = %d%n", i, i % nums.length, nums[i % nums.length]);
        }
    }
    
    // Test cases
    public static void main(String[] args) {
        NextGreaterElementII solver = new NextGreaterElementII();
        
        // Test Case 1: Basic circular
        System.out.println("Test Case 1: Basic Circular");
        int[] nums1 = {1, 2, 1};
        System.out.println("Input:  " + Arrays.toString(nums1));
        System.out.println("Output: " + Arrays.toString(solver.nextGreaterElements(nums1)));
        System.out.println("Expected: [2, -1, 2]");
        visualizeCircular(nums1);
        
        // Test Case 2: Multiple elements
        System.out.println("\nTest Case 2: Multiple Elements");
        int[] nums2 = {1, 2, 3, 4, 3};
        System.out.println("Input:  " + Arrays.toString(nums2));
        System.out.println("Output: " + Arrays.toString(solver.nextGreaterElements(nums2)));
        System.out.println("Expected: [2, 3, 4, -1, 4]");
        
        // Test Case 3: All same elements
        System.out.println("\nTest Case 3: All Same");
        int[] nums3 = {5, 5, 5, 5};
        System.out.println("Input:  " + Arrays.toString(nums3));
        System.out.println("Output: " + Arrays.toString(solver.nextGreaterElements(nums3)));
        System.out.println("Expected: [-1, -1, -1, -1]");
        
        // Test Case 4: Strictly increasing
        System.out.println("\nTest Case 4: Strictly Increasing");
        int[] nums4 = {1, 2, 3, 4, 5};
        System.out.println("Input:  " + Arrays.toString(nums4));
        System.out.println("Output: " + Arrays.toString(solver.nextGreaterElements(nums4)));
        System.out.println("Expected: [2, 3, 4, 5, -1]");
        
        // Test Case 5: Strictly decreasing
        System.out.println("\nTest Case 5: Strictly Decreasing");
        int[] nums5 = {5, 4, 3, 2, 1};
        System.out.println("Input:  " + Arrays.toString(nums5));
        System.out.println("Output: " + Arrays.toString(solver.nextGreaterElements(nums5)));
        System.out.println("Expected: [-1, 5, 5, 5, 5]");
        
        // Test Case 6: Peak in middle
        System.out.println("\nTest Case 6: Peak in Middle");
        int[] nums6 = {1, 3, 2};
        System.out.println("Input:  " + Arrays.toString(nums6));
        System.out.println("Output: " + Arrays.toString(solver.nextGreaterElements(nums6)));
        System.out.println("Expected: [3, -1, 3]");
        
        // Test Case 7: Single element
        System.out.println("\nTest Case 7: Single Element");
        int[] nums7 = {1};
        System.out.println("Input:  " + Arrays.toString(nums7));
        System.out.println("Output: " + Arrays.toString(solver.nextGreaterElements(nums7)));
        System.out.println("Expected: [-1]");
        
        // Test Case 8: Negative numbers
        System.out.println("\nTest Case 8: Negative Numbers");
        int[] nums8 = {-1, -2, -3, -2, -1};
        System.out.println("Input:  " + Arrays.toString(nums8));
        System.out.println("Output: " + Arrays.toString(solver.nextGreaterElements(nums8)));
        System.out.println("Expected: [-1, -1, -2, -1, -1]");
        
        // Step-by-step trace
        System.out.println("\n=== Step-by-Step Trace: [1,2,1] ===");
        stepByStepTrace(nums1);
        
        // Double pass visualization
        System.out.println("\n=== Double Pass Concept ===");
        explainDoublPass(nums1);
        
        // Algorithm comparison
        System.out.println("\n=== Algorithm Comparison ===");
        compareAlgorithms();
        
        // Compare all approaches
        System.out.println("\n=== Comparing All Approaches ===");
        compareAllApproaches(nums2);
    }
    
    private static void stepByStepTrace(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        Stack<Integer> stack = new Stack<>();
        
        System.out.println("Processing array twice: [1,2,1] → [1,2,1,1,2,1]");
        System.out.println("\n┌──────┬───────┬──────────────┬──────────┬─────────────────┐");
        System.out.println("│  i   │ idx  │    nums[idx] │ Stack    │ Action          │");
        System.out.println("├──────┼───────┼──────────────┼──────────┼─────────────────┤");
        
        for (int i = 0; i < 2 * n; i++) {
            int idx = i % n;
            StringBuilder action = new StringBuilder();
            
            // Pop smaller elements
            List<Integer> popped = new ArrayList<>();
            while (!stack.isEmpty() && nums[stack.peek()] < nums[idx]) {
                int poppedIdx = stack.pop();
                popped.add(poppedIdx);
                result[poppedIdx] = nums[idx];
            }
            
            if (!popped.isEmpty()) {
                action.append("Pop: ");
                for (int p : popped) {
                    action.append(p).append("→").append(nums[idx]).append(" ");
                }
            }
            
            // Push only in first pass
            if (i < n) {
                stack.push(idx);
                if (action.length() > 0) action.append("| ");
                action.append("Push ").append(idx);
            } else {
                if (action.length() == 0) action.append("Skip (2nd pass)");
            }
            
            System.out.printf("│  %d   │   %d   │      %d       │ %-8s │ %-15s │%n",
                            i, idx, nums[idx], stack.toString(), action.toString());
        }
        
        System.out.println("└──────┴───────┴──────────────┴──────────┴─────────────────┘");
        System.out.println("\nFinal Result: " + Arrays.toString(result));
    }
    
    private static void explainDoublPass(int[] nums) {
        System.out.println("Original array: " + Arrays.toString(nums));
        System.out.println("\nWhy process twice?");
        System.out.println("- First pass:  Find NGE within normal array bounds");
        System.out.println("- Second pass: Find NGE by wrapping around circularly");
        
        System.out.println("\nVisualization:");
        System.out.println("Pass 1: [1, 2, 1] ← process these indices");
        System.out.println("Pass 2: [1, 2, 1] ← check against these values (circular)");
        
        System.out.println("\nExample for index 2 (value=1):");
        System.out.println("- Pass 1: No element to right > 1");
        System.out.println("- Pass 2: Wraps to index 0,1 → finds 2 > 1");
        System.out.println("- Result[2] = 2");
    }
    
    private static void compareAlgorithms() {
        System.out.println("┌──────────────────────┬──────────────┬──────────────┬─────────────────┐");
        System.out.println("│ Approach             │ Time         │ Space        │ Notes           │");
        System.out.println("├──────────────────────┼──────────────┼──────────────┼─────────────────┤");
        System.out.println("│ Brute Force          │ O(n²)        │ O(1)         │ Simple          │");
        System.out.println("│ Stack (2 Pass)       │ O(n)         │ O(n)         │ Optimal         │");
        System.out.println("│ Stack (Explicit)     │ O(n)         │ O(n)         │ Clear logic     │");
        System.out.println("│ Stack (RTL)          │ O(n)         │ O(n)         │ Alternative     │");
        System.out.println("│ Deque                │ O(n)         │ O(n)         │ Same as stack   │");
        System.out.println("└──────────────────────┴──────────────┴──────────────┴─────────────────┘");
    }
    
    private static void compareAllApproaches(int[] nums) {
        NextGreaterElementII solver = new NextGreaterElementII();
        
        System.out.println("Input: " + Arrays.toString(nums));
        System.out.println("\nBrute Force: " + Arrays.toString(solver.nextGreaterElementsBruteForce(nums)));
        System.out.println("Stack (LTR):  " + Arrays.toString(solver.nextGreaterElements(nums)));
        System.out.println("Stack (RTL):  " + Arrays.toString(solver.nextGreaterElementsRTL(nums)));
        System.out.println("Explicit:     " + Arrays.toString(solver.nextGreaterElementsExplicit(nums)));
        System.out.println("Deque:        " + Arrays.toString(solver.nextGreaterElementsDeque(nums)));
        
        // Performance test
        int[] large = new int[10000];
        for (int i = 0; i < large.length; i++) {
            large[i] = i;
        }
        
        long start = System.nanoTime();
        solver.nextGreaterElementsBruteForce(large);
        long end = System.nanoTime();
        System.out.println("\nPerformance (n=10000):");
        System.out.println("Brute Force: " + (end - start) / 1_000_000.0 + " ms");
        
        start = System.nanoTime();
        solver.nextGreaterElements(large);
        end = System.nanoTime();
        System.out.println("Stack:       " + (end - start) / 1_000_000.0 + " ms");
    }
}

/*
DETAILED EXPLANATION:

PROBLEM: Next Greater Element II (Circular Array)

Key difference from NGE I:
- Array is CIRCULAR: last element wraps to first
- Need to search "around the corner"

CIRCULAR ARRAY CONCEPT:

Normal: [1, 2, 1]
- Index 0 → right: [2, 1]
- Index 1 → right: [1]
- Index 2 → right: []

Circular: [1, 2, 1] → [1, 2, 1, 1, 2, 1, ...]
- Index 0 → right: [2, 1] (normal)
- Index 1 → right: [1, 1, 2, 1, ...] (wraps)
- Index 2 → right: [1, 2, 1] (wraps to beginning)

EXAMPLE WALKTHROUGH: [1, 2, 1]

Index 0 (value=1):
  Search right: 2, 1, (wrap) 1, 2
  First greater: 2
  Result[0] = 2

Index 1 (value=2):
  Search right: 1, (wrap) 1, 2
  No element > 2
  Result[1] = -1

Index 2 (value=1):
  Search right: (wrap) 1, 2
  First greater: 2
  Result[2] = 2

Output: [2, -1, 2]

KEY INSIGHT: DOUBLE PASS

Instead of complex circular logic, process array TWICE!

Why this works:
- First pass: normal NGE search
- Second pass: circular NGE search
- Combined: covers all circular possibilities

ALGORITHM (OPTIMAL):

1. Initialize result array with -1
2. Create stack for indices
3. Process array TWICE (i from 0 to 2n-1):
   a. idx = i % n (circular index)
   b. While stack not empty AND nums[stack.top] < nums[idx]:
      - Pop index
      - Set result[popped] = nums[idx]
   c. If i < n: push idx (only in first pass)
4. Return result

DETAILED TRACE: [1, 2, 1]

Pass 1 (i = 0 to 2):
  i=0, idx=0, num=1:
    Stack: [] → [0]
  
  i=1, idx=1, num=2:
    2 > 1, pop 0 → result[0] = 2
    Stack: [0] → [1]
  
  i=2, idx=2, num=1:
    1 < 2, no pop
    Stack: [1] → [1, 2]

Pass 2 (i = 3 to 5):
  i=3, idx=0, num=1:
    1 < 2, no pop
    Don't push (i >= n)
    Stack: [1, 2]
  
  i=4, idx=1, num=2:
    2 > 1, pop 2 → result[2] = 2
    2 = 2, no more pops
    Don't push
    Stack: [1]
  
  i=5, idx=2, num=1:
    1 < 2, no pop
    Stack: [1]

Final: [2, -1, 2]

WHY ONLY PUSH IN FIRST PASS?

If we push in both passes:
- Elements would be duplicated in stack
- We'd process same comparisons twice
- Inefficient and incorrect

First pass establishes waiting elements.
Second pass resolves them circularly.

MONOTONIC STACK PROPERTY:

Stack maintains DECREASING order (by values):
- When we find larger element → it's NGE for smaller ones
- Elements stay in stack until NGE found or end reached

Example stack states:
[5, 4, 3] ← valid (decreasing)
[5, 7, 3] ← invalid (7 would have popped 5)

HANDLING EDGE CASES:

1. All same elements: [5, 5, 5, 5]
   - No element > any other
   - Result: [-1, -1, -1, -1]

2. Strictly increasing: [1, 2, 3, 4, 5]
   - Each has NGE except last
   - Last wraps but nothing greater
   - Result: [2, 3, 4, 5, -1]

3. Strictly decreasing: [5, 4, 3, 2, 1]
   - Each wraps to find 5
   - 5 has no NGE
   - Result: [-1, 5, 5, 5, 5]

4. Single element: [1]
   - No other element
   - Result: [-1]

CIRCULAR INDEX CALCULATION:

i % n gives circular index:
- i=0 → idx=0
- i=1 → idx=1
- i=2 → idx=2
- i=3 → idx=0 (wraps)
- i=4 → idx=1 (wraps)
- i=5 → idx=2 (wraps)

This simulates circular traversal!

COMPLEXITY ANALYSIS:

Optimal Approach:
Time: O(n)
  - Process 2n elements
  - Each element pushed once, popped once
  - Stack operations: O(1)
  - Total: O(2n) = O(n)

Space: O(n)
  - Stack: worst case O(n)
  - Result array: O(n)

Brute Force:
Time: O(n²)
  - For each element: O(n)
  - Search circularly: O(n)
Space: O(1)

COMPARISON WITH NGE I:

NGE I:
- Two separate arrays
- No circular logic
- HashMap needed

NGE II:
- Single circular array
- Double pass trick
- No HashMap needed

WHY TWO PASSES WORK:

Mathematical insight:
- Any circular search covers at most n elements
- Two passes cover 2n positions
- This is sufficient to check all circular possibilities

Example: n=5, searching from index 3
- First pass: indices 3, 4
- Second pass: indices 0, 1, 2, 3, 4
- Combined: covers full circle from 3

ALTERNATIVE: RIGHT TO LEFT

Process from right to left, twice:

[1, 2, 1]

Pass 1 (right to left):
  i=2, num=1: Stack: [1], result[2] = -1
  i=1, num=2: pop 1, Stack: [2], result[1] = -1
  i=0, num=1: Stack: [2, 1], result[0] = 2

Pass 2 (right to left):
  Already resolved? Check and update

Both LTR and RTL work!

INTERVIEW STRATEGY:

1. Recognize it's NGE with circular twist
2. Explain brute force: O(n²)
3. Introduce double pass optimization
4. Trace through example
5. Explain why only push in first pass
6. Discuss complexity: O(n)
7. Handle edge cases

COMMON MISTAKES:

1. Pushing in both passes (wrong!)
2. Not using modulo for circular index
3. Forgetting to initialize result with -1
4. Wrong stack comparison (≤ vs <)
5. Not handling single element case

VARIATIONS:

1. Next Smaller Element (circular)
2. Previous Greater Element (circular)
3. K-th next greater element
4. Circular with duplicates handling

PRACTICAL APPLICATIONS:

1. Temperature prediction
2. Stock price analysis
3. Circular buffer problems
4. Round-robin scheduling
5. Circular arrays in general

This problem beautifully extends NGE I with circular logic!
*/
