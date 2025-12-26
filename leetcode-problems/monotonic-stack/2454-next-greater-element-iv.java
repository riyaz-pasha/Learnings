import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

class SecondGreaterElement {

    class Solution {
        public int[] secondGreaterElement(int[] nums) {
            int n = nums.length;
            int[] ans = new int[n];
            Arrays.fill(ans, -1);

            Stack<Integer> stack = new Stack<>(); // waiting for first greater
            Queue<Integer> waiting = new ArrayDeque<>(); // waiting for second greater

            for (int i = 0; i < n; i++) {

                // Step 1: resolve second greater
                while (!waiting.isEmpty() && nums[waiting.peek()] < nums[i]) {
                    ans[waiting.poll()] = nums[i];
                }

                // Step 2: promote from stack to waiting
                while (!stack.isEmpty() && nums[stack.peek()] < nums[i]) {
                    waiting.offer(stack.pop());
                }

                // Step 3: current waits for first greater
                stack.push(i);
            }

            return ans;
        }
    }

    
    // Approach 1: Brute Force
    // Time: O(n²), Space: O(1) excluding output
    public int[] secondGreaterElementBruteForce(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        
        for (int i = 0; i < n; i++) {
            int count = 0;
            
            // Find second greater element
            for (int j = i + 1; j < n; j++) {
                if (nums[j] > nums[i]) {
                    count++;
                    if (count == 2) {
                        result[i] = nums[j];
                        break;
                    }
                }
            }
        }
        
        return result;
    }
    
    // Approach 2: Two Stacks (OPTIMAL)
    // Time: O(n), Space: O(n)
    public int[] secondGreaterElement(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        
        Stack<Integer> stack1 = new Stack<>();  // Waiting for first greater
        Stack<Integer> stack2 = new Stack<>();  // Waiting for second greater
        
        for (int i = 0; i < n; i++) {
            // Process stack2: find second greater
            while (!stack2.isEmpty() && nums[stack2.peek()] < nums[i]) {
                result[stack2.pop()] = nums[i];
            }
            
            // Move from stack1 to stack2: found first greater
            List<Integer> toMove = new ArrayList<>();
            while (!stack1.isEmpty() && nums[stack1.peek()] < nums[i]) {
                toMove.add(stack1.pop());
            }
            
            // Add to stack2 in reverse order to maintain monotonic property
            for (int j = toMove.size() - 1; j >= 0; j--) {
                stack2.push(toMove.get(j));
            }
            
            stack1.push(i);
        }
        
        return result;
    }
    
    // Approach 3: Two Stacks with Sorting
    // Ensures stack2 is sorted when elements are moved
    public int[] secondGreaterElementSorted(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        
        Stack<Integer> stack1 = new Stack<>();
        Stack<Integer> stack2 = new Stack<>();
        
        for (int i = 0; i < n; i++) {
            // Process stack2
            while (!stack2.isEmpty() && nums[stack2.peek()] < nums[i]) {
                result[stack2.pop()] = nums[i];
            }
            
            // Collect elements from stack1 that found first greater
            List<Integer> temp = new ArrayList<>();
            while (!stack1.isEmpty() && nums[stack1.peek()] < nums[i]) {
                temp.add(stack1.pop());
            }
            
            // Sort by value before adding to stack2
            // This maintains monotonic decreasing order in stack2
            temp.sort((a, b) -> Integer.compare(nums[b], nums[a]));
            
            for (int idx : temp) {
                stack2.push(idx);
            }
            
            stack1.push(i);
        }
        
        return result;
    }
    
    // Approach 4: Priority Queue + Stack
    // Uses PQ to maintain order when moving elements
    public int[] secondGreaterElementPQ(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        
        Stack<Integer> stack = new Stack<>();
        // Min heap: smaller values have higher priority
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> 
            Integer.compare(nums[a[0]], nums[b[0]]));
        
        for (int i = 0; i < n; i++) {
            // Process PQ: these are waiting for second greater
            while (!pq.isEmpty() && nums[pq.peek()[0]] < nums[i]) {
                result[pq.poll()[0]] = nums[i];
            }
            
            // Move from stack to PQ: found first greater
            while (!stack.isEmpty() && nums[stack.peek()] < nums[i]) {
                pq.offer(new int[]{stack.pop()});
            }
            
            stack.push(i);
        }
        
        return result;
    }
    
    // Approach 5: Three Stacks (Alternative Concept)
    // Stack for: waiting first, waiting second, processed
    public int[] secondGreaterElementThreeStacks(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        
        Stack<Integer> waitingFirst = new Stack<>();
        Stack<Integer> waitingSecond = new Stack<>();
        
        for (int i = 0; i < n; i++) {
            // Check elements waiting for second greater
            while (!waitingSecond.isEmpty() && nums[waitingSecond.peek()] < nums[i]) {
                result[waitingSecond.pop()] = nums[i];
            }
            
            // Move elements that found their first greater
            Stack<Integer> temp = new Stack<>();
            while (!waitingFirst.isEmpty() && nums[waitingFirst.peek()] < nums[i]) {
                temp.push(waitingFirst.pop());
            }
            
            // Transfer to waitingSecond (in correct order)
            while (!temp.isEmpty()) {
                waitingSecond.push(temp.pop());
            }
            
            waitingFirst.push(i);
        }
        
        return result;
    }
    
    // Approach 6: Precompute Next Greater, then use again
    // Time: O(n), Space: O(n)
    public int[] secondGreaterElementPrecompute(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        
        // First, compute next greater element for each index
        int[] nextGreater = new int[n];
        Arrays.fill(nextGreater, -1);
        Stack<Integer> stack = new Stack<>();
        
        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && nums[stack.peek()] < nums[i]) {
                nextGreater[stack.pop()] = i;
            }
            stack.push(i);
        }
        
        // For each index, find next greater of its next greater
        for (int i = 0; i < n; i++) {
            if (nextGreater[i] != -1) {
                int firstGreaterIdx = nextGreater[i];
                // Find next greater after firstGreaterIdx
                for (int j = firstGreaterIdx + 1; j < n; j++) {
                    if (nums[j] > nums[i]) {
                        result[i] = nums[j];
                        break;
                    }
                }
            }
        }
        
        return result;
    }
    
    // Helper: Visualize the concept
    private static void visualizeConcept(int[] nums) {
        System.out.println("\n=== Second Greater Element Concept ===");
        System.out.println("Array: " + Arrays.toString(nums));
        
        for (int i = 0; i < nums.length; i++) {
            System.out.println("\nIndex " + i + " (value=" + nums[i] + "):");
            
            int count = 0;
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[j] > nums[i]) {
                    count++;
                    if (count == 1) {
                        System.out.println("  First greater:  nums[" + j + "] = " + nums[j]);
                    } else if (count == 2) {
                        System.out.println("  Second greater: nums[" + j + "] = " + nums[j]);
                        break;
                    }
                }
            }
            
            if (count < 2) {
                System.out.println("  Second greater: -1 (not found)");
            }
        }
    }
    
    // Test cases
    public static void main(String[] args) {
        SecondGreaterElement solver = new SecondGreaterElement();
        
        // Test Case 1: Basic example
        System.out.println("Test Case 1: Basic Example");
        int[] nums1 = {2, 4, 0, 9, 6};
        System.out.println("Input:  " + Arrays.toString(nums1));
        System.out.println("Output: " + Arrays.toString(solver.secondGreaterElement(nums1)));
        System.out.println("Expected: [9, 6, 6, -1, -1]");
        visualizeConcept(nums1);
        
        // Test Case 2: All same
        System.out.println("\n\nTest Case 2: All Same");
        int[] nums2 = {3, 3};
        System.out.println("Input:  " + Arrays.toString(nums2));
        System.out.println("Output: " + Arrays.toString(solver.secondGreaterElement(nums2)));
        System.out.println("Expected: [-1, -1]");
        
        // Test Case 3: Increasing sequence
        System.out.println("\n\nTest Case 3: Increasing Sequence");
        int[] nums3 = {1, 2, 3, 4, 5};
        System.out.println("Input:  " + Arrays.toString(nums3));
        System.out.println("Output: " + Arrays.toString(solver.secondGreaterElement(nums3)));
        System.out.println("Expected: [3, 4, 5, -1, -1]");
        
        // Test Case 4: Decreasing sequence
        System.out.println("\n\nTest Case 4: Decreasing Sequence");
        int[] nums4 = {5, 4, 3, 2, 1};
        System.out.println("Input:  " + Arrays.toString(nums4));
        System.out.println("Output: " + Arrays.toString(solver.secondGreaterElement(nums4)));
        System.out.println("Expected: [-1, -1, -1, -1, -1]");
        
        // Test Case 5: Complex pattern
        System.out.println("\n\nTest Case 5: Complex Pattern");
        int[] nums5 = {1, 2, 4, 3};
        System.out.println("Input:  " + Arrays.toString(nums5));
        System.out.println("Output: " + Arrays.toString(solver.secondGreaterElement(nums5)));
        System.out.println("Expected: [4, 3, -1, -1]");
        
        // Test Case 6: With duplicates
        System.out.println("\n\nTest Case 6: With Duplicates");
        int[] nums6 = {1, 3, 3, 5, 7};
        System.out.println("Input:  " + Arrays.toString(nums6));
        System.out.println("Output: " + Arrays.toString(solver.secondGreaterElement(nums6)));
        System.out.println("Expected: [5, 7, 7, -1, -1]");
        
        // Test Case 7: Single element
        System.out.println("\n\nTest Case 7: Single Element");
        int[] nums7 = {1};
        System.out.println("Input:  " + Arrays.toString(nums7));
        System.out.println("Output: " + Arrays.toString(solver.secondGreaterElement(nums7)));
        System.out.println("Expected: [-1]");
        
        // Step-by-step trace
        System.out.println("\n\n=== Step-by-Step Trace: [2,4,0,9,6] ===");
        stepByStepTrace(nums1);
        
        // Two-stack visualization
        System.out.println("\n\n=== Two-Stack Concept ===");
        explainTwoStacks();
        
        // Algorithm comparison
        System.out.println("\n\n=== Algorithm Comparison ===");
        compareAlgorithms();
        
        // Compare all approaches
        System.out.println("\n\n=== Comparing All Approaches ===");
        compareAllApproaches(nums1);
    }
    
    private static void stepByStepTrace(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);
        
        Stack<Integer> stack1 = new Stack<>();
        Stack<Integer> stack2 = new Stack<>();
        
        System.out.println("Stack1: Waiting for FIRST greater");
        System.out.println("Stack2: Waiting for SECOND greater");
        System.out.println("\n┌───────┬────────┬──────────────┬──────────────┬─────────────────────────┐");
        System.out.println("│   i   │ num    │   Stack1     │   Stack2     │ Action                  │");
        System.out.println("├───────┼────────┼──────────────┼──────────────┼─────────────────────────┤");
        
        for (int i = 0; i < n; i++) {
            StringBuilder action = new StringBuilder();
            
            // Process stack2
            List<Integer> foundSecond = new ArrayList<>();
            while (!stack2.isEmpty() && nums[stack2.peek()] < nums[i]) {
                int idx = stack2.pop();
                result[idx] = nums[i];
                foundSecond.add(idx);
            }
            
            if (!foundSecond.isEmpty()) {
                action.append("2nd: ");
                for (int idx : foundSecond) {
                    action.append(idx).append("→").append(nums[i]).append(" ");
                }
                action.append("| ");
            }
            
            // Move from stack1 to stack2
            List<Integer> toMove = new ArrayList<>();
            while (!stack1.isEmpty() && nums[stack1.peek()] < nums[i]) {
                toMove.add(stack1.pop());
            }
            
            if (!toMove.isEmpty()) {
                action.append("Move to S2: ");
                for (int idx : toMove) {
                    action.append(idx).append(" ");
                }
                action.append("| ");
            }
            
            // Add to stack2
            for (int j = toMove.size() - 1; j >= 0; j--) {
                stack2.push(toMove.get(j));
            }
            
            stack1.push(i);
            action.append("Push ").append(i).append(" to S1");
            
            System.out.printf("│   %d   │   %d    │ %-12s │ %-12s │ %-23s │%n",
                            i, nums[i], 
                            formatStack(stack1),
                            formatStack(stack2),
                            action.toString());
        }
        
        System.out.println("└───────┴────────┴──────────────┴──────────────┴─────────────────────────┘");
        System.out.println("\nFinal Result: " + Arrays.toString(result));
    }
    
    private static String formatStack(Stack<Integer> stack) {
        if (stack.isEmpty()) return "[]";
        List<Integer> list = new ArrayList<>(stack);
        String str = list.toString();
        return str.length() > 12 ? str.substring(0, 9) + "..." : str;
    }
    
    private static void explainTwoStacks() {
        System.out.println("Why Two Stacks?");
        System.out.println("\nStack1: Elements waiting for their FIRST greater element");
        System.out.println("Stack2: Elements that found first greater, waiting for SECOND");
        
        System.out.println("\nProcess for each new element:");
        System.out.println("1. Check Stack2: Is this the second greater for anyone?");
        System.out.println("   → Pop and record result");
        System.out.println("\n2. Check Stack1: Is this the first greater for anyone?");
        System.out.println("   → Pop from Stack1 and push to Stack2");
        System.out.println("\n3. Push current index to Stack1");
        
        System.out.println("\nExample: Processing [2, 4, 0, 9, 6]");
        System.out.println("\ni=0, num=2:");
        System.out.println("  Stack1: [0], Stack2: []");
        
        System.out.println("\ni=1, num=4:");
        System.out.println("  4 > 2 → Move 0 from S1 to S2 (found first greater)");
        System.out.println("  Stack1: [1], Stack2: [0]");
        
        System.out.println("\ni=2, num=0:");
        System.out.println("  0 < 4 → No moves");
        System.out.println("  Stack1: [1, 2], Stack2: [0]");
        
        System.out.println("\ni=3, num=9:");
        System.out.println("  9 > 0 → Result[0] = 9 (second greater found!)");
        System.out.println("  9 > 4 → Move 1 from S1 to S2");
        System.out.println("  9 > 0 → Move 2 from S1 to S2");
        System.out.println("  Stack1: [3], Stack2: [1, 2]");
        
        System.out.println("\ni=4, num=6:");
        System.out.println("  6 > 4 → Result[1] = 6 (second greater found!)");
        System.out.println("  6 > 0 → Result[2] = 6 (second greater found!)");
        System.out.println("  Stack1: [3, 4], Stack2: []");
    }
    
    private static void compareAlgorithms() {
        System.out.println("┌──────────────────────┬──────────────┬──────────────┬───────────────────┐");
        System.out.println("│ Approach             │ Time         │ Space        │ Notes             │");
        System.out.println("├──────────────────────┼──────────────┼──────────────┼───────────────────┤");
        System.out.println("│ Brute Force          │ O(n²)        │ O(1)         │ Simple            │");
        System.out.println("│ Two Stacks           │ O(n)         │ O(n)         │ Optimal           │");
        System.out.println("│ Two Stacks + Sort    │ O(n log n)   │ O(n)         │ Maintains order   │");
        System.out.println("│ PQ + Stack           │ O(n log n)   │ O(n)         │ Alternative       │");
        System.out.println("│ Three Stacks         │ O(n)         │ O(n)         │ Conceptual        │");
        System.out.println("│ Precompute NGE       │ O(n²)        │ O(n)         │ Not optimal       │");
        System.out.println("└──────────────────────┴──────────────┴──────────────┴───────────────────┘");
    }
    
    private static void compareAllApproaches(int[] nums) {
        SecondGreaterElement solver = new SecondGreaterElement();
        
        System.out.println("Input: " + Arrays.toString(nums));
        System.out.println("\nBrute Force:   " + Arrays.toString(solver.secondGreaterElementBruteForce(nums)));
        System.out.println("Two Stacks:    " + Arrays.toString(solver.secondGreaterElement(nums)));
        System.out.println("Two Stacks+S:  " + Arrays.toString(solver.secondGreaterElementSorted(nums)));
        System.out.println("PQ + Stack:    " + Arrays.toString(solver.secondGreaterElementPQ(nums)));
        System.out.println("Three Stacks:  " + Arrays.toString(solver.secondGreaterElementThreeStacks(nums)));
        System.out.println("Precompute:    " + Arrays.toString(solver.secondGreaterElementPrecompute(nums)));
        
        // Performance test
        int[] large = new int[5000];
        Random rand = new Random(42);
        for (int i = 0; i < large.length; i++) {
            large[i] = rand.nextInt(10000);
        }
        
        System.out.println("\nPerformance Test (n=5000):");
        
        long start = System.nanoTime();
        solver.secondGreaterElementBruteForce(large);
        long end = System.nanoTime();
        System.out.println("Brute Force:  " + (end - start) / 1_000_000.0 + " ms");
        
        start = System.nanoTime();
        solver.secondGreaterElement(large);
        end = System.nanoTime();
        System.out.println("Two Stacks:   " + (end - start) / 1_000_000.0 + " ms");
        
        start = System.nanoTime();
        solver.secondGreaterElementPQ(large);
        end = System.nanoTime();
        System.out.println("PQ + Stack:   " + (end - start) / 1_000_000.0 + " ms");
    }
}

/*
DETAILED EXPLANATION:

PROBLEM: Second Greater Element

Find the SECOND element to the right that is greater than current element.

Key requirement:
- There must be EXACTLY ONE greater element between current and second greater

EXAMPLE WALKTHROUGH: [2, 4, 0, 9, 6]

Index 0 (value=2):
  Elements to right: 4, 0, 9, 6
  Greater than 2: 4, 9, 6
  First greater: 4
  Second greater: 9 ✓

Index 1 (value=4):
  Elements to right: 0, 9, 6
  Greater than 4: 9, 6
  First greater: 9
  Second greater: 6 ✓

Index 2 (value=0):
  Elements to right: 9, 6
  Greater than 0: 9, 6
  First greater: 9
  Second greater: 6 ✓

Index 3 (value=9):
  Elements to right: 6
  Greater than 9: none
  Second greater: -1

Index 4 (value=6):
  Elements to right: none
  Second greater: -1

Output: [9, 6, 6, -1, -1]

KEY INSIGHT: TWO STACKS

We need to track:
1. Elements waiting for FIRST greater (Stack1)
2. Elements waiting for SECOND greater (Stack2)

When we see a new element:
- Check if it's the second greater for anyone in Stack2
- Move elements from Stack1 to Stack2 if it's their first greater
- Add current element to Stack1

ALGORITHM (TWO STACKS):

1. Initialize two stacks and result array
2. For each element i:
   a. While Stack2 not empty AND nums[Stack2.top] < nums[i]:
      - This is the second greater!
      - Pop and record: result[popped] = nums[i]
   
   b. While Stack1 not empty AND nums[Stack1.top] < nums[i]:
      - This is the first greater
      - Pop from Stack1 and save to temporary list
   
   c. Move elements from temp to Stack2 (in reverse order)
   
   d. Push i to Stack1

3. Return result

DETAILED TRACE: [2, 4, 0, 9, 6]

i=0, num=2:
  S1: [], S2: []
  → Push 0 to S1
  S1: [0], S2: []

i=1, num=4:
  S1: [0], S2: []
  Check S2: empty
  Check S1: 4 > 2 → move 0 to S2
  → Push 1 to S1
  S1: [1], S2: [0]

i=2, num=0:
  S1: [1], S2: [0]
  Check S2: 0 < 0? No
  Check S1: 0 < 4? No
  → Push 2 to S1
  S1: [1, 2], S2: [0]

i=3, num=9:
  S1: [1, 2], S2: [0]
  Check S2: 9 > 0 → result[0] = 9 (second greater!)
  S1: [1, 2], S2: []
  Check S1: 9 > 0 → move 2, 9 > 4 → move 1
  S1: [], S2: [1, 2] (reverse order)
  → Push 3 to S1
  S1: [3], S2: [1, 2]

i=4, num=6:
  S1: [3], S2: [1, 2]
  Check S2: 6 > 0 → result[2] = 6, 6 > 4 → result[1] = 6
  S1: [3], S2: []
  Check S1: 6 < 9? No
  → Push 4 to S1
  S1: [3, 4], S2: []

Result: [9, 6, 6, -1, -1]

WHY REVERSE ORDER WHEN MOVING TO STACK2?

When moving from Stack1 to Stack2, we need to maintain order:
- Stack1 had: [1, 2] (2 on top)
- After popping: temp = [2, 1]
- Need to push to Stack2: first 1, then 2
- So Stack2 becomes: [1, 2] (2 on top)

This maintains monotonic decreasing property in Stack2.

MONOTONIC STACK PROPERTIES:

Stack1: Monotonic decreasing (by values)
- Elements waiting for first greater
- Larger values at bottom

Stack2: Monotonic decreasing (by values)
- Elements waiting for second greater
- Larger values at bottom

WHY THIS WORKS:

When we encounter nums[i]:
1. All elements in Stack2 with values < nums[i] have found their second greater
2. All elements in Stack1 with values < nums[i] have found their first greater
3. Moving them to Stack2 puts them in position to find second greater

COMPLEXITY ANALYSIS:

Two Stacks Approach:
Time: O(n)
  - Each element pushed to Stack1 once: O(n)
  - Each element moved to Stack2 once: O(n)
  - Each element popped from Stack2 once: O(n)
  - Total: O(3n) = O(n)

Space: O(n)
  - Stack1: O(n) worst case
  - Stack2: O(n) worst case
  - Result: O(n)

Brute Force:
Time: O(n²)
  - For each element: O(n)
  - Count two greater: O(n)
Space: O(1)

ALTERNATIVE: PRIORITY QUEUE + STACK

Instead of manual ordering when moving to Stack2, use a priority queue:

Stack: Waiting for first greater
PQ: Waiting for second greater (min heap by value)

When nums[i] encountered:
1. Pop from PQ all elements < nums[i] → second greater found
2. Pop from stack all elements < nums[i] → add to PQ
3. Push i to stack

Trade-off: O(n log n) time but simpler logic

EDGE CASES:

1. All same: [3, 3, 3]
   - No greater elements
   - Result: [-1, -1, -1]

2. Strictly increasing: [1, 2, 3, 4, 5]
   - Each has at least 2 greater (except last two)
   - Result: [3, 4, 5, -1, -1]

3. Strictly decreasing: [5, 4, 3, 2, 1]
   - No greater elements
   - Result: [-1, -1, -1, -1, -1]

4. Only one greater: [1, 10, 2]
   - 1 has only one greater (10)
   - Result: [-1, -1, -1]

5. Single element: [1]
   - Result: [-1]

COMMON MISTAKES:

1. Not maintaining order when moving stacks
2. Checking Stack1 before Stack2
3. Not handling exactly one greater case
4. Using values instead of indices in stacks
5. Wrong comparison operators

VARIATIONS:

1. Third Greater Element
2. Kth Greater Element
3. Second Smaller Element
4. Second Greater with distance constraint

INTERVIEW STRATEGY:

1. Clarify "second greater" requirement
2. Start with brute force: O(n²)
3. Identify optimization: track state
4. Introduce two-stack concept
5. Trace through example carefully
6. Explain why reverse order matters
7. Discuss complexity: O(n)
8. Handle edge cases

WHY NOT JUST NEXT GREATER TWICE?

Can't just apply NGE algorithm twice because:
- NGE finds NEXT greater (first one encountered)
- We need SECOND occurrence of greater element
- Different problem requiring different state tracking

RELATED PROBLEMS:

1. Next Greater Element I & II
2. Daily Temperatures
3. Remove K Digits
4. Largest Rectangle in Histogram
5. Stock Span Problem

This problem beautifully extends monotonic stack to track TWO levels!
*/
