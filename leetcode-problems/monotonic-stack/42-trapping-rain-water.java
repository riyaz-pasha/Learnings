import java.util.Arrays;
import java.util.Stack;

class TrappingRainWater {
    
    // Approach 1: Monotonic Stack (Decreasing) - Layer by Layer
    // Time: O(n), Space: O(n)
    public int trap1(int[] height) {
        Stack<Integer> stack = new Stack<>();
        int water = 0;
        
        for (int i = 0; i < height.length; i++) {
            // While current height is greater than stack top
            while (!stack.isEmpty() && height[i] > height[stack.peek()]) {
                int top = stack.pop(); // Bottom of the container
                
                if (stack.isEmpty()) break;
                
                // Calculate trapped water
                int distance = i - stack.peek() - 1;
                int boundedHeight = Math.min(height[i], height[stack.peek()]) - height[top];
                water += distance * boundedHeight;
            }
            stack.push(i);
        }
        
        return water;
    }
    
    // Approach 2: Two Pointers (Optimal)
    // Time: O(n), Space: O(1)
    public int trap2(int[] height) {
        if (height == null || height.length == 0) return 0;
        
        int left = 0, right = height.length - 1;
        int leftMax = 0, rightMax = 0;
        int water = 0;
        
        while (left < right) {
            if (height[left] < height[right]) {
                if (height[left] >= leftMax) {
                    leftMax = height[left];
                } else {
                    water += leftMax - height[left];
                }
                left++;
            } else {
                if (height[right] >= rightMax) {
                    rightMax = height[right];
                } else {
                    water += rightMax - height[right];
                }
                right--;
            }
        }
        
        return water;
    }
    
    // Approach 3: Dynamic Programming - Precompute Max Heights
    // Time: O(n), Space: O(n)
    public int trap3(int[] height) {
        if (height == null || height.length == 0) return 0;
        
        int n = height.length;
        int[] leftMax = new int[n];
        int[] rightMax = new int[n];
        
        // Compute leftMax: maximum height to the left of each position
        leftMax[0] = height[0];
        for (int i = 1; i < n; i++) {
            leftMax[i] = Math.max(leftMax[i - 1], height[i]);
        }
        
        // Compute rightMax: maximum height to the right of each position
        rightMax[n - 1] = height[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            rightMax[i] = Math.max(rightMax[i + 1], height[i]);
        }
        
        // Calculate trapped water
        int water = 0;
        for (int i = 0; i < n; i++) {
            water += Math.min(leftMax[i], rightMax[i]) - height[i];
        }
        
        return water;
    }
    
    // Approach 4: Brute Force (For Understanding)
    // Time: O(n²), Space: O(1)
    public int trap4(int[] height) {
        int water = 0;
        
        for (int i = 0; i < height.length; i++) {
            int leftMax = 0, rightMax = 0;
            
            // Find max height to the left
            for (int j = 0; j <= i; j++) {
                leftMax = Math.max(leftMax, height[j]);
            }
            
            // Find max height to the right
            for (int j = i; j < height.length; j++) {
                rightMax = Math.max(rightMax, height[j]);
            }
            
            water += Math.min(leftMax, rightMax) - height[i];
        }
        
        return water;
    }
    
    // Approach 5: Monotonic Stack Variant - Calculating Area Differently
    public int trap5(int[] height) {
        Stack<Integer> stack = new Stack<>();
        int water = 0;
        int n = height.length;
        
        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && height[i] > height[stack.peek()]) {
                int popHeight = height[stack.pop()];
                
                if (stack.isEmpty()) break;
                
                int distance = i - stack.peek() - 1;
                int minHeight = Math.min(height[stack.peek()], height[i]);
                water += distance * (minHeight - popHeight);
            }
            stack.push(i);
        }
        
        return water;
    }
    
    // Approach 6: Divide and Conquer
    // Time: O(n log n), Space: O(log n)
    public int trap6(int[] height) {
        return divideConquer(height, 0, height.length - 1);
    }
    
    private int divideConquer(int[] height, int left, int right) {
        if (left >= right) return 0;
        
        // Find max height index in range
        int maxIdx = left;
        for (int i = left + 1; i <= right; i++) {
            if (height[i] > height[maxIdx]) {
                maxIdx = i;
            }
        }
        
        // Calculate water on left side
        int leftWater = 0;
        int leftMax = 0;
        for (int i = left; i < maxIdx; i++) {
            if (height[i] > leftMax) {
                leftMax = height[i];
            } else {
                leftWater += leftMax - height[i];
            }
        }
        
        // Calculate water on right side
        int rightWater = 0;
        int rightMax = 0;
        for (int i = right; i > maxIdx; i--) {
            if (height[i] > rightMax) {
                rightMax = height[i];
            } else {
                rightWater += rightMax - height[i];
            }
        }
        
        return leftWater + rightWater;
    }
    
    // Test and visualization
    public static void main(String[] args) {
        TrappingRainWater solution = new TrappingRainWater();
        
        // Test Case 1
        int[] height1 = {0,1,0,2,1,0,1,3,2,1,2,1};
        int result1 = solution.trap1(height1);
        System.out.println("Test 1: " + Arrays.toString(height1));
        System.out.println("Water trapped: " + result1); // Output: 6
        visualizeTrapping(height1, result1);
        
        // Test Case 2
        int[] height2 = {4,2,0,3,2,5};
        int result2 = solution.trap1(height2);
        System.out.println("\nTest 2: " + Arrays.toString(height2));
        System.out.println("Water trapped: " + result2); // Output: 9
        visualizeTrapping(height2, result2);
        
        // Test Case 3: No water
        int[] height3 = {1,2,3,4,5};
        int result3 = solution.trap1(height3);
        System.out.println("\nTest 3 (ascending): " + Arrays.toString(height3));
        System.out.println("Water trapped: " + result3); // Output: 0
        
        // Test Case 4: Simple container
        int[] height4 = {3,0,0,2,0,4};
        int result4 = solution.trap1(height4);
        System.out.println("\nTest 4: " + Arrays.toString(height4));
        System.out.println("Water trapped: " + result4);
        
        // Compare all approaches
        System.out.println("\n=== Comparing All Approaches ===");
        compareApproaches(height1);
        
        // Detailed trace of monotonic stack
        System.out.println("\n=== Monotonic Stack Trace for [0,1,0,2,1,0,1,3,2,1,2,1] ===");
        traceMonotonicStack(height1);
    }
    
    private static void visualizeTrapping(int[] height, int water) {
        int maxHeight = 0;
        for (int h : height) maxHeight = Math.max(maxHeight, h);
        
        System.out.println("\nVisualization:");
        
        // Draw from top to bottom
        for (int level = maxHeight; level > 0; level--) {
            for (int i = 0; i < height.length; i++) {
                if (height[i] >= level) {
                    System.out.print("█ ");
                } else {
                    // Check if water can be trapped at this level
                    int leftMax = 0, rightMax = 0;
                    for (int j = 0; j < i; j++) leftMax = Math.max(leftMax, height[j]);
                    for (int j = i + 1; j < height.length; j++) rightMax = Math.max(rightMax, height[j]);
                    
                    if (Math.min(leftMax, rightMax) >= level) {
                        System.out.print("~ ");
                    } else {
                        System.out.print("  ");
                    }
                }
            }
            System.out.println();
        }
        
        // Print indices
        for (int i = 0; i < height.length; i++) {
            System.out.print(i + " ");
        }
        System.out.println("\n█ = wall, ~ = water");
    }
    
    private static void compareApproaches(int[] height) {
        TrappingRainWater solution = new TrappingRainWater();
        
        long start1 = System.nanoTime();
        int result1 = solution.trap1(height);
        long time1 = System.nanoTime() - start1;
        
        long start2 = System.nanoTime();
        int result2 = solution.trap2(height);
        long time2 = System.nanoTime() - start2;
        
        long start3 = System.nanoTime();
        int result3 = solution.trap3(height);
        long time3 = System.nanoTime() - start3;
        
        System.out.println("┌────────────────────────┬────────┬────────┬─────────┐");
        System.out.println("│ Approach               │ Result │ Time   │ Space   │");
        System.out.println("├────────────────────────┼────────┼────────┼─────────┤");
        System.out.println("│ Monotonic Stack        │ " + result1 + "      │ O(n)   │ O(n)    │");
        System.out.println("│ Two Pointers           │ " + result2 + "      │ O(n)   │ O(1) ✓  │");
        System.out.println("│ Dynamic Programming    │ " + result3 + "      │ O(n)   │ O(n)    │");
        System.out.println("└────────────────────────┴────────┴────────┴─────────┘");
    }
    
    private static void traceMonotonicStack(int[] height) {
        Stack<Integer> stack = new Stack<>();
        int water = 0;
        
        System.out.println("┌──────┬─────────┬──────────────────┬──────────┬───────────┐");
        System.out.println("│ i    │ h[i]    │ Stack (indices)  │ Action   │ Water     │");
        System.out.println("├──────┼─────────┼──────────────────┼──────────┼───────────┤");
        
        for (int i = 0; i < height.length; i++) {
            String action = "";
            int waterAdded = 0;
            
            while (!stack.isEmpty() && height[i] > height[stack.peek()]) {
                int top = stack.pop();
                
                if (stack.isEmpty()) {
                    action = "Pop " + top + " (no container)";
                    break;
                }
                
                int distance = i - stack.peek() - 1;
                int boundedHeight = Math.min(height[i], height[stack.peek()]) - height[top];
                waterAdded = distance * boundedHeight;
                water += waterAdded;
                action = String.format("Pop %d, add %d", top, waterAdded);
            }
            
            if (action.isEmpty()) {
                action = "Push";
            }
            
            stack.push(i);
            
            System.out.printf("│ %-4d │ %-7d │ %-16s │ %-8s │ %-9d │%n", 
                            i, height[i], stackToString(stack), action, water);
        }
        
        System.out.println("└──────┴─────────┴──────────────────┴──────────┴───────────┘");
    }
    
    private static String stackToString(Stack<Integer> stack) {
        if (stack.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < stack.size(); i++) {
            sb.append(stack.get(i));
            if (i < stack.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}

/*
COMPREHENSIVE EXPLANATION:

=== PROBLEM UNDERSTANDING ===

Given elevation map, calculate trapped rainwater.
Water trapped at position i = min(leftMax, rightMax) - height[i]

Key insight: Water level at each position determined by:
- Maximum height to its left
- Maximum height to its right
- Water fills up to min(leftMax, rightMax)

=== APPROACH 1: MONOTONIC STACK (LAYER BY LAYER) ===

CORE CONCEPT: Calculate water HORIZONTALLY, layer by layer

Stack maintains DECREASING heights (indices)
When we find a taller bar, it can trap water with previous bars

Algorithm:
1. Maintain stack of indices in decreasing height order
2. When current height > stack.top:
   - We found right boundary!
   - Pop stack (this is the bottom)
   - Stack.peek() is left boundary
   - Calculate water in this container
3. Push current index

WHY IT WORKS:
Stack keeps potential LEFT boundaries
Current position is RIGHT boundary
Popped element is the BOTTOM of container

EXAMPLE: [0,1,0,2,1,0,1,3,2,1,2,1]

i=0, h=0: Stack: [0]
i=1, h=1: h[1]>h[0], pop 0, stack empty, push 1. Stack: [1]
i=2, h=0: h[2]<h[1], push 2. Stack: [1,2]
i=3, h=2: h[3]>h[2]
  - Pop 2 (bottom)
  - Left boundary: h[1]=1, Right: h[3]=2
  - Distance: 3-1-1=1
  - Height: min(1,2)-0=1
  - Water: 1*1=1 ✓
  - Pop 1, stack empty
  - Push 3. Stack: [3]

MONOTONIC STACK PATTERN:
- Decreasing stack for finding next greater element
- Calculates area/water horizontally
- Each element pushed/popped once → O(n)

=== APPROACH 2: TWO POINTERS (MOST OPTIMAL) ===

CORE CONCEPT: Process from both ends simultaneously

Key insight: Water level depends on SMALLER of leftMax/rightMax
We can process the side with smaller maximum safely!

Algorithm:
1. Two pointers: left, right
2. Track leftMax, rightMax
3. Move pointer with smaller max:
   - If current ≥ max: update max
   - If current < max: add (max - current) water
4. Converge to middle

WHY IT WORKS:
If leftMax < rightMax:
  - Water at left determined by leftMax
  - We know rightMax is higher, so it won't limit
  - Safe to process left side

EXAMPLE: [4,2,0,3,2,5]
left=0, right=5, leftMax=0, rightMax=0

Step 1: h[0]=4 < h[5]=5
  Update leftMax=4, move left to 1

Step 2: h[1]=2 < h[5]=5
  h[1]=2 < leftMax=4, water += 4-2=2
  Move left to 2

Continue...

Time: O(n) single pass
Space: O(1) ✓✓✓ Best!

=== APPROACH 3: DYNAMIC PROGRAMMING ===

CORE CONCEPT: Precompute max heights

Algorithm:
1. Compute leftMax[i] = max height from 0 to i
2. Compute rightMax[i] = max height from i to n-1
3. Water[i] = min(leftMax[i], rightMax[i]) - height[i]

Time: O(n) - three passes
Space: O(n) - two arrays

Clear and intuitive, good for understanding!

=== COMPARISON ===

┌─────────────────────┬────────┬────────┬──────────────┐
│ Approach            │ Time   │ Space  │ Notes        │
├─────────────────────┼────────┼────────┼──────────────┤
│ Brute Force         │ O(n²)  │ O(1)   │ Too slow     │
│ Dynamic Programming │ O(n)   │ O(n)   │ Clear logic  │
│ Two Pointers        │ O(n)   │ O(1)   │ ✓✓✓ Best     │
│ Monotonic Stack     │ O(n)   │ O(n)   │ Layer-wise   │
└─────────────────────┴────────┴────────┴──────────────┘

=== MONOTONIC STACK VARIANTS ===

Variant 1: Calculate vertically (layer by layer)
- Our Approach 1
- Pops create horizontal containers

Variant 2: Different water calculation
- Same stack maintenance
- Different area formula

Both O(n) time, O(n) space

=== KEY INSIGHTS ===

1. WATER LEVEL FORMULA:
   water[i] = min(leftMax[i], rightMax[i]) - height[i]

2. MONOTONIC STACK:
   - Maintains decreasing sequence
   - Finds boundaries for containers
   - Processes horizontally

3. TWO POINTERS:
   - Process side with smaller maximum
   - Single pass, O(1) space
   - Most elegant solution!

4. WHY MONOTONIC STACK?
   - Natural for "next greater element" problems
   - Handles nested containers elegantly
   - Educational value for stack problems

=== EDGE CASES ===

1. Empty array: return 0
2. Single element: return 0
3. Ascending: [1,2,3,4,5] → 0
4. Descending: [5,4,3,2,1] → 0
5. Flat: [3,3,3,3] → 0
6. Valley: [3,0,3] → 3
7. Multiple peaks: handle correctly

=== VISUALIZATION ===

[3,0,0,2,0,4]

Height 4:           █
Height 3: █ ~ ~ ~ ~ █
Height 2: █ ~ ~ █ ~ █
Height 1: █ ~ ~ █ ~ █
Height 0: █ █ █ █ █ █
Index:    0 1 2 3 4 5

Water trapped: 3+3+1+3 = 10 units

=== INTERVIEW TIPS ===

1. Start with brute force to show understanding
2. Optimize to DP (easier to explain)
3. Present Two Pointers as optimal
4. Mention Monotonic Stack as alternative
5. Draw visualization
6. Discuss trade-offs
7. Handle edge cases

=== RELATED PROBLEMS ===

1. Container With Most Water
2. Largest Rectangle in Histogram
3. Maximal Rectangle
4. Daily Temperatures (monotonic stack)
5. Next Greater Element

=== WHY THIS PROBLEM IS IMPORTANT ===

1. Tests multiple skill areas:
   - Monotonic stack pattern
   - Two pointers technique
   - Dynamic programming
   - Space optimization

2. Common in interviews:
   - Amazon, Google, Microsoft
   - Tests problem-solving ability
   - Multiple valid approaches

3. Foundation for harder problems:
   - 2D version (3D rain water)
   - Variations with constraints

This problem beautifully demonstrates:
- Monotonic stack applications
- Space-time tradeoffs
- Multiple solution paradigms
- Optimization techniques
*/
