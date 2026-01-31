import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

class CombinationSumII {
    
    // Approach 1: Backtracking with Sorting (Optimal)
    // Time: O(2^n), Space: O(n) for recursion
    public List<List<Integer>> combinationSum2_1(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(candidates); // Sort to handle duplicates
        backtrack(candidates, target, 0, new ArrayList<>(), result);
        return result;
    }
    
    private void backtrack(int[] candidates, int target, int start, 
                          List<Integer> current, List<List<Integer>> result) {
        if (target == 0) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = start; i < candidates.length; i++) {
            // Skip duplicates at the same level
            if (i > start && candidates[i] == candidates[i - 1]) {
                continue;
            }
            
            // Prune: if current candidate exceeds target, no point continuing
            if (candidates[i] > target) {
                break;
            }
            
            // Choose
            current.add(candidates[i]);
            
            // Explore: move to next index (i+1) since each element used once
            backtrack(candidates, target - candidates[i], i + 1, current, result);
            
            // Unchoose (backtrack)
            current.remove(current.size() - 1);
        }
    }
    
    // Approach 2: Backtracking with Explicit Duplicate Tracking
    // Time: O(2^n), Space: O(n)
    public List<List<Integer>> combinationSum2_2(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(candidates);
        backtrackWithTracking(candidates, target, 0, new ArrayList<>(), result);
        return result;
    }
    
    private void backtrackWithTracking(int[] candidates, int target, int start,
                                      List<Integer> current, List<List<Integer>> result) {
        if (target == 0) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        if (target < 0) {
            return;
        }
        
        for (int i = start; i < candidates.length; i++) {
            // Skip duplicates: only process first occurrence at this level
            if (i > start && candidates[i] == candidates[i - 1]) {
                continue;
            }
            
            current.add(candidates[i]);
            backtrackWithTracking(candidates, target - candidates[i], i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
    
    // Approach 3: Backtracking with Frequency Map
    // Time: O(2^n), Space: O(n)
    public List<List<Integer>> combinationSum2_3(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        
        // Count frequency of each number
        Map<Integer, Integer> freq = new TreeMap<>();
        for (int num : candidates) {
            freq.put(num, freq.getOrDefault(num, 0) + 1);
        }
        
        // Convert to list of unique candidates
        List<int[]> uniqueCandidates = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
            uniqueCandidates.add(new int[]{entry.getKey(), entry.getValue()});
        }
        
        backtrackWithFreq(uniqueCandidates, target, 0, new ArrayList<>(), result);
        return result;
    }
    
    private void backtrackWithFreq(List<int[]> candidates, int target, int start,
                                  List<Integer> current, List<List<Integer>> result) {
        if (target == 0) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        if (target < 0) {
            return;
        }
        
        for (int i = start; i < candidates.size(); i++) {
            int num = candidates.get(i)[0];
            int count = candidates.get(i)[1];
            
            // Try using this number 1, 2, ..., count times
            for (int use = 1; use <= count; use++) {
                if (num * use > target) {
                    break;
                }
                
                // Add 'use' copies of num
                for (int j = 0; j < use; j++) {
                    current.add(num);
                }
                
                backtrackWithFreq(candidates, target - num * use, i + 1, current, result);
                
                // Remove 'use' copies
                for (int j = 0; j < use; j++) {
                    current.remove(current.size() - 1);
                }
            }
        }
    }
    
    // Approach 4: Iterative with Queue (Less efficient)
    // Time: O(2^n * n), Space: O(2^n)
    public List<List<Integer>> combinationSum2_4(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(candidates);
        
        Queue<State> queue = new LinkedList<>();
        queue.offer(new State(0, 0, new ArrayList<>()));
        
        while (!queue.isEmpty()) {
            State state = queue.poll();
            
            if (state.sum == target) {
                result.add(new ArrayList<>(state.path));
                continue;
            }
            
            if (state.sum > target || state.index >= candidates.length) {
                continue;
            }
            
            for (int i = state.index; i < candidates.length; i++) {
                if (i > state.index && candidates[i] == candidates[i - 1]) {
                    continue;
                }
                
                List<Integer> newPath = new ArrayList<>(state.path);
                newPath.add(candidates[i]);
                queue.offer(new State(i + 1, state.sum + candidates[i], newPath));
            }
        }
        
        return result;
    }
    
    static class State {
        int index, sum;
        List<Integer> path;
        
        State(int index, int sum, List<Integer> path) {
            this.index = index;
            this.sum = sum;
            this.path = path;
        }
    }
    
    // Test cases with visualization
    public static void main(String[] args) {
        CombinationSumII solution = new CombinationSumII();
        
        // Test Case 1
        int[] candidates1 = {10, 1, 2, 7, 6, 1, 5};
        int target1 = 8;
        System.out.println("Test 1:");
        System.out.println("Input: " + Arrays.toString(candidates1) + ", target = " + target1);
        List<List<Integer>> result1 = solution.combinationSum2_1(candidates1, target1);
        System.out.println("Output: " + result1);
        visualizeBacktracking(candidates1, target1);
        
        // Test Case 2
        int[] candidates2 = {2, 5, 2, 1, 2};
        int target2 = 5;
        System.out.println("\nTest 2:");
        System.out.println("Input: " + Arrays.toString(candidates2) + ", target = " + target2);
        List<List<Integer>> result2 = solution.combinationSum2_1(candidates2, target2);
        System.out.println("Output: " + result2);
        
        // Test Case 3: No solution
        int[] candidates3 = {1, 1};
        int target3 = 3;
        System.out.println("\nTest 3:");
        System.out.println("Input: " + Arrays.toString(candidates3) + ", target = " + target3);
        List<List<Integer>> result3 = solution.combinationSum2_1(candidates3, target3);
        System.out.println("Output: " + result3);
        
        // Test Case 4: Single element
        int[] candidates4 = {1};
        int target4 = 1;
        System.out.println("\nTest 4:");
        System.out.println("Input: " + Arrays.toString(candidates4) + ", target = " + target4);
        List<List<Integer>> result4 = solution.combinationSum2_1(candidates4, target4);
        System.out.println("Output: " + result4);
        
        // Compare approaches
        System.out.println("\nComparing all approaches for Test 1:");
        System.out.println("Approach 1: " + solution.combinationSum2_1(candidates1, target1));
        System.out.println("Approach 2: " + solution.combinationSum2_2(candidates1, target1));
        System.out.println("Approach 3: " + solution.combinationSum2_3(candidates1, target1));
        System.out.println("Approach 4: " + solution.combinationSum2_4(candidates1, target1));
    }
    
    private static void visualizeBacktracking(int[] candidates, int target) {
        System.out.println("\nBacktracking visualization:");
        Arrays.sort(candidates);
        System.out.println("Sorted candidates: " + Arrays.toString(candidates));
        System.out.println("Target: " + target);
        System.out.println("\nDecision tree (partial):");
        System.out.println("                  []");
        System.out.println("        /    /    |    \\    \\");
        System.out.println("      [1]  [2]  [5]  [6]  [7] ...");
        System.out.println("     /  \\");
        System.out.println("  [1,1][1,2] ...");
        System.out.println("\nKey insight: Skip duplicates at same level");
        System.out.println("Example: After trying [1], skip next [1] at same level");
    }
}

/*
DETAILED EXPLANATION:

PROBLEM CHARACTERISTICS:
1. Each number can be used AT MOST ONCE
2. Must find ALL unique combinations
3. Array may contain duplicates
4. Need to avoid duplicate combinations in result

KEY DIFFERENCES FROM COMBINATION SUM I:
- Combination Sum I: Unlimited use of each number
- Combination Sum II: Each number used at most once
- This problem: Must handle duplicate numbers in input

CRITICAL INSIGHT - HANDLING DUPLICATES:

To avoid duplicate combinations:
1. SORT the array first
2. Skip duplicate numbers AT THE SAME RECURSION LEVEL

Example: candidates = [1, 1, 2, 5], target = 8

Without skipping duplicates:
- Try first [1]: [1, ...] → leads to [1, 2, 5]
- Try second [1]: [1, ...] → leads to [1, 2, 5] (duplicate!)

With skipping:
- Try first [1]: [1, ...] → leads to [1, 2, 5]
- Skip second [1] at this level ✓
- Avoid duplicate combinations

THE SKIP CONDITION:
```java
if (i > start && candidates[i] == candidates[i - 1]) {
    continue;
}
```

Why `i > start`?
- `i == start`: First element at this level, must try it
- `i > start`: Not first element, if same as previous, skip it
- This skips duplicates AT SAME LEVEL but allows using them at DIFFERENT LEVELS

BACKTRACKING TEMPLATE:

```java
void backtrack(candidates, target, start, current, result) {
    // Base case: found valid combination
    if (target == 0) {
        result.add(new ArrayList<>(current));
        return;
    }
    
    // Iterate through candidates starting from 'start'
    for (int i = start; i < candidates.length; i++) {
        // Skip duplicates at same level
        if (i > start && candidates[i] == candidates[i-1]) continue;
        
        // Pruning: if current exceeds target, stop
        if (candidates[i] > target) break;
        
        // Choose: add current candidate
        current.add(candidates[i]);
        
        // Explore: recurse with i+1 (each number used once)
        backtrack(candidates, target - candidates[i], i + 1, current, result);
        
        // Unchoose: backtrack
        current.remove(current.size() - 1);
    }
}
```

EXAMPLE WALKTHROUGH: candidates=[1,1,2,5,6,7,10], target=8

After sorting: [1,1,2,5,6,7,10]

Decision Tree:
                        []
        /       /       |      \       \
      [1]     [2]     [5]    [6]    [7]  (skip second 1)
     /  \      |       |       |      |
  [1,1][1,2] [2,6]   [5]     [6]    [7]
    |    |                   ✓       ✓
  [1,1,6][1,2,5]
    ✓       ✓

Valid combinations found:
- [1,1,6]: 1+1+6=8 ✓
- [1,2,5]: 1+2+5=8 ✓
- [1,7]: 1+7=8 ✓
- [2,6]: 2+6=8 ✓

DETAILED TRACE for target=8:

Level 0: start=0, target=8
  i=0: candidates[0]=1
    Choose [1], target becomes 7
    Level 1: start=1, target=7
      i=1: candidates[1]=1
        Choose [1,1], target becomes 6
        Level 2: start=2, target=6
          i=2: candidates[2]=2, too small
          i=3: candidates[3]=5, too small
          i=4: candidates[4]=6
            Choose [1,1,6], target becomes 0 → FOUND! ✓
      i=2: candidates[2]=2 (skip i=1 duplicate)
        Choose [1,2], target becomes 5
        Level 2: start=3, target=5
          i=3: candidates[3]=5
            Choose [1,2,5], target becomes 0 → FOUND! ✓
      i=3: candidates[3]=5
        Choose [1,5], target becomes 2
        Level 2: start=4, target=2
          i=4: candidates[4]=6, exceeds 2, break
      i=4: candidates[4]=6
        Choose [1,6], target becomes 1
        Level 2: start=5, target=1
          i=5: candidates[5]=7, exceeds 1, break
      i=5: candidates[5]=7
        Choose [1,7], target becomes 0 → FOUND! ✓
  i=1: Skip (duplicate of i=0)
  i=2: candidates[2]=2
    Choose [2], target becomes 6
    Level 1: start=3, target=6
      i=3: candidates[3]=5, too small
      i=4: candidates[4]=6
        Choose [2,6], target becomes 0 → FOUND! ✓

Result: [[1,1,6], [1,2,5], [1,7], [2,6]]

WHY SORTING IS CRUCIAL:

1. Duplicate handling: Group duplicates together
   - [1,1,2,5] sorted vs [1,2,1,5] unsorted
   - Sorted allows simple check: candidates[i] == candidates[i-1]

2. Pruning optimization:
   - If candidates[i] > target, all subsequent are also larger
   - Can break early instead of continuing

3. Result consistency:
   - Produces combinations in sorted order
   - Easier to verify and test

COMPLEXITY ANALYSIS:

Time Complexity: O(2^n)
- In worst case, we explore all subsets
- Each candidate has 2 choices: include or exclude
- With pruning, typically much better than 2^n

Space Complexity: O(n)
- Recursion depth: at most n levels
- Current path: at most n elements
- Result list not counted in space complexity

OPTIMIZATIONS:

1. Early termination (pruning):
   ```java
   if (candidates[i] > target) break;
   ```

2. Skip duplicates at same level:
   ```java
   if (i > start && candidates[i] == candidates[i-1]) continue;
   ```

3. Sort array first:
   - Enables pruning and duplicate detection

EDGE CASES:

1. Empty candidates: return []
2. No valid combination: return []
3. All candidates exceed target: return []
4. Target = 0: return [[]]
5. Single element equals target: return [[element]]
6. All duplicates: [2,2,2], target=6 → [[2,2,2]]

COMMON MISTAKES:

1. Forgetting to sort array first
2. Wrong skip condition: `if (i > 0 ...)` instead of `if (i > start ...)`
3. Not making copy of current list when adding to result
4. Using same index in recursion (i instead of i+1)
5. Not handling negative numbers (if problem allows them)

VARIATIONS:

1. Combination Sum I: Unlimited use → recurse with same index
2. Combination Sum II: Use once → recurse with i+1
3. Combination Sum III: Fixed count k → add count parameter
4. Combination Sum IV: Count ways → DP problem

PRACTICAL APPLICATIONS:

1. Subset sum problem
2. Partition problem
3. Knapsack variations
4. Resource allocation
5. Scheduling with constraints
*/
