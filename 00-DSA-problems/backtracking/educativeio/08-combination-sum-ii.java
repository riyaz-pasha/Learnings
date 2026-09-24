/**
 * ============================================================================
 * COMBINATION SUM II - COMPREHENSIVE GUIDE & SOLUTIONS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * We are given a collection of numbered tiles (candidates) and a target number. 
 * We need to find all unique combinations of tiles that add up exactly to the 
 * target. 
 * Two important rules:
 *   1. We can only use each specific physical tile once per combination.
 *   2. The input might have duplicate tiles (e.g., two '1's), but our final list 
 *      of combinations must NOT contain duplicate sets (e.g., if we make [1, 2, 5] 
 *      using the first '1', we shouldn't report [1, 2, 5] again using the second '1').
 * 
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: Can the target or candidates be negative or zero?
 * A: Constraints say candidates[i] >= 1 and target >= 1. This is a massive help 
 *    because we know the sum will only ever INCREASE. If we exceed the target, 
 *    we can stop immediately (no negative numbers to bring the sum back down).
 * 
 * Q: Should the output be sorted?
 * A: The order of combinations doesn't matter, and the order inside each 
 *    combination doesn't matter, but internally sorting makes finding duplicates 
 *    much easier.
 * 
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - DECISION TREE: Like most combination problems, we build a decision tree (DFS).
 * - THE DUPLICATE PROBLEM: If candidates = [1, 1, 2] and target = 3, we could 
 *   pick the first '1' and the '2' (giving [1, 2]), OR we could pick the second '1' 
 *   and the '2' (also giving [1, 2]). We need a way to ignore the second '1' 
 *   if it's acting in the exact same role (same depth in the tree) as the first '1'.
 * - THE SILVER BULLET (SORTING): If we sort the array first [1a, 1b, 2, 5], 
 *   duplicates are adjacent. At any specific step in our combination building, 
 *   if we decide NOT to use '1a' as the starting point, we must also skip '1b' 
 *   as the starting point, because it will just spawn the exact same sub-tree!
 *   Code translation: `if (i > start && candidates[i] == candidates[i-1]) continue;`
 * 
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Point out that because we want combinations without duplicates, 
 *   sorting the array upfront is almost mandatory.
 * - Step 2: Write the DFS/Backtracking structure. It's similar to Combination Sum I, 
 *   but we increment the index by 1 (i + 1) in the recursive call because we 
 *   can't reuse the same element.
 * - Step 3: Implement the duplicate-skipping logic (`i > start`). This single 
 *   line is what the interviewer is actually testing you on.
 * - Step 4: Mention the "early termination" pruning: if candidates[i] > remaining_target, 
 *   we can `break` out of the loop entirely because all subsequent numbers are 
 *   even larger (since it's sorted).
 * 
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * Input: candidates = [10, 1, 2, 7, 6, 1, 5], target = 8
 * Sorted: [1(a), 1(b), 2, 5, 6, 7, 10]
 * 
 * DFS Tree at depth 1 (choosing the first number):
 *                         (empty)
 *          /          /          |       \        \
 *       [1(a)]      [1(b)]      [2]      [5] ...  [10]
 *         |           |
 *      (valid)   (SKIPPED!) 
 * 
 * Why is 1(b) skipped here? Because '1(b)' is at index 1, and our loop started 
 * at index 0. Since 1(b) == 1(a), any combination starting with 1(b) was ALREADY 
 * found when we started with 1(a). We prune the whole branch!
 */

import java.util.*;

class CombinationSumII {

    /**
     * SOLUTION 1: Standard Backtracking with Sorting (Most Optimal & Common)
     * ------------------------------------------------------------------------
     * Pros: Easiest to write, highly optimized via pruning (break) and skipping 
     * duplicates (continue). This is the gold-standard interview answer.
     * 
     * Time Complexity: O(2^N) in the worst case (e.g., all unique numbers). 
     * Sorting takes O(N log N).
     * Space Complexity: O(N) auxiliary space for the recursion stack and current list.
     */
    public List<List<Integer>> combinationSum2_DFS(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        
        // 1. Sort the array to handle duplicates and enable early stopping
        Arrays.sort(candidates);
        
        backtrack(candidates, target, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrack(int[] candidates, int remain, int start, List<Integer> current, List<List<Integer>> result) {
        // Base case: Target reached exactly
        if (remain == 0) {
            result.add(List.copyOf(current)); // Modern Java: Unmodifiable copy
            return;
        }

        for (int i = start; i < candidates.length; i++) {
            // PRUNING 1: Skip duplicates at the same recursion depth
            // If i > start, it means we already processed candidates[i-1] as a 
            // starting candidate for this position. If they are equal, skip it.
            if (i > start && candidates[i] == candidates[i - 1]) {
                continue;
            }

            // PRUNING 2: Early stopping
            // Since the array is sorted, if the current number is bigger than 
            // what we have left, ALL subsequent numbers will be too big.
            if (candidates[i] > remain) {
                break;
            }

            // Choose
            current.add(candidates[i]);
            
            // Explore
            // Notice we pass 'i + 1' because we cannot reuse the exact same tile.
            backtrack(candidates, remain - candidates[i], i + 1, current, result);
            
            // Un-choose (Backtrack)
            current.remove(current.size() - 1);
        }
    }

    /**
     * SOLUTION 2: Backtracking with Frequency Map
     * ------------------------------------------------------------------------
     * Pros: Conceptual alternative. Instead of skipping adjacent duplicates, we 
     * group them. E.g., [1, 1, 2, 5] becomes {1: count 2, 2: count 1, 5: count 1}.
     * We then decide: "Take zero 1s, take one 1, or take two 1s". 
     * Feature Highlight: We use Java 14+ `record` to hold distinct values and counts.
     * 
     * Time Complexity: O(2^N)
     * Space Complexity: O(N) for the frequency map and recursion stack.
     */
    public List<List<Integer>> combinationSum2_FreqMap(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        
        // Modern Java Feature: Record for immutable grouping
        record Element(int value, int count) {}

        // Use a TreeMap to keep the keys sorted (enables early stopping)
        Map<Integer, Integer> freqMap = new TreeMap<>();
        for (int num : candidates) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }

        // Convert to a list of distinct elements and their frequencies
        List<Element> distinctElements = freqMap.entrySet().stream()
                .map(e -> new Element(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        backtrackFreq(distinctElements, target, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrackFreq(List<Element> elements, int remain, int index, List<Integer> current, List<List<Integer>> result) {
        if (remain == 0) {
            result.add(List.copyOf(current));
            return;
        }
        
        if (remain < 0 || index == elements.size()) {
            return;
        }

        Element elem = elements.get(index);
        
        // Early stopping (since we used a TreeMap, elements are sorted by value)
        if (elem.value() > remain) {
            return;
        }

        // We can choose to take this specific element 0, 1, ..., up to 'count' times
        for (int take = 0; take <= elem.count(); take++) {
            // The total sum added by taking 'take' instances of this value
            int sumAdded = take * elem.value();
            
            if (sumAdded > remain) break; // Exceeds target

            // Add the elements to our current path
            for (int i = 0; i < take; i++) {
                current.add(elem.value());
            }

            // Move to the NEXT distinct element
            backtrackFreq(elements, remain - sumAdded, index + 1, current, result);

            // Backtrack: remove the elements we just added
            for (int i = 0; i < take; i++) {
                current.remove(current.size() - 1);
            }
        }
    }

    /**
     * UTILITY: Print combinations beautifully.
     */
    private static void printCombinations(List<List<Integer>> combs) {
        System.out.println("Total unique combinations: " + combs.size());
        for (List<Integer> comb : combs) {
            System.out.println(" -> " + comb);
        }
        System.out.println();
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        CombinationSumII solver = new CombinationSumII();

        int[] candidates1 = {10, 1, 2, 7, 6, 1, 5};
        int target1 = 8;
        
        System.out.println("--- Test Case 1 ---");
        System.out.println("Candidates: " + Arrays.toString(candidates1) + " | Target: " + target1);
        System.out.println("Solution 1 (Sorting DFS):");
        printCombinations(solver.combinationSum2_DFS(candidates1.clone(), target1));
        System.out.println("Solution 2 (Frequency Map):");
        printCombinations(solver.combinationSum2_FreqMap(candidates1.clone(), target1));

        int[] candidates2 = {2, 5, 2, 1, 2};
        int target2 = 5;

        System.out.println("--- Test Case 2 ---");
        System.out.println("Candidates: " + Arrays.toString(candidates2) + " | Target: " + target2);
        System.out.println("Solution 1 (Sorting DFS):");
        printCombinations(solver.combinationSum2_DFS(candidates2.clone(), target2));
    }
}
