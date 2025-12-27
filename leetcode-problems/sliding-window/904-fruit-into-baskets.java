import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class FruitIntoBaskets {
    
    /**
     * APPROACH 1: Sliding Window with HashMap (Optimal Solution)
     * 
     * INTUITION:
     * - This is "longest subarray with at most 2 distinct elements"
     * - Use sliding window to maintain a window with ≤ 2 fruit types
     * - HashMap tracks count of each fruit type in current window
     * 
     * REASONING:
     * - Expand window by adding fruits from right
     * - When we have > 2 types, shrink from left
     * - HashMap.size() tells us number of distinct fruit types
     * - Remove fruit type from map when its count reaches 0
     * 
     * TIME COMPLEXITY: O(n) - each element visited at most twice
     * SPACE COMPLEXITY: O(1) - map stores at most 3 fruit types
     */
    public int totalFruit_HashMap(int[] fruits) {
        Map<Integer, Integer> basket = new HashMap<>(); // fruit_type -> count
        int left = 0;
        int maxFruits = 0;
        
        for (int right = 0; right < fruits.length; right++) {
            // Add current fruit to basket
            basket.put(fruits[right], basket.getOrDefault(fruits[right], 0) + 1);
            
            // If we have more than 2 types, shrink window
            while (basket.size() > 2) {
                // Remove leftmost fruit
                basket.put(fruits[left], basket.get(fruits[left]) - 1);
                
                // If count becomes 0, remove the fruit type entirely
                if (basket.get(fruits[left]) == 0) {
                    basket.remove(fruits[left]);
                }
                left++;
            }
            
            // Update maximum fruits collected
            maxFruits = Math.max(maxFruits, right - left + 1);
        }
        
        return maxFruits;
    }
    
    /**
     * APPROACH 2: Optimized Sliding Window (No Shrinking)
     * 
     * INTUITION:
     * - Instead of shrinking window completely, just slide it forward
     * - Window only grows or maintains size, never shrinks
     * - This works because we only care about maximum length
     * 
     * REASONING:
     * - When basket has > 2 types, remove one fruit from left and move left++
     * - Use 'if' instead of 'while' to maintain window size
     * - Final window size is the answer
     * 
     * TIME COMPLEXITY: O(n) - single pass through array
     * SPACE COMPLEXITY: O(1) - map stores at most 3 fruit types
     */
    public int totalFruit_Optimized(int[] fruits) {
        Map<Integer, Integer> basket = new HashMap<>();
        int left = 0;
        
        for (int right = 0; right < fruits.length; right++) {
            basket.put(fruits[right], basket.getOrDefault(fruits[right], 0) + 1);
            
            // Use 'if' instead of 'while' - slide window forward
            if (basket.size() > 2) {
                basket.put(fruits[left], basket.get(fruits[left]) - 1);
                if (basket.get(fruits[left]) == 0) {
                    basket.remove(fruits[left]);
                }
                left++;
            }
        }
        
        return fruits.length - left;
    }
    
    /**
     * APPROACH 3: Two Pointers with Last Two Fruits Tracking
     * 
     * INTUITION:
     * - Track only the two most recent fruit types
     * - Keep track of where the second-to-last fruit type ends
     * - When we encounter a third type, start new window from where
     *   the last fruit type started appearing continuously
     * 
     * REASONING:
     * - type1, type2: the two fruit types in current window
     * - type2Count: consecutive count of type2 (most recent type)
     * - When new type appears: new window starts from last type2Count trees
     * 
     * TIME COMPLEXITY: O(n) - single pass
     * SPACE COMPLEXITY: O(1) - only a few variables
     */
    public int totalFruit_TwoPointers(int[] fruits) {
        if (fruits.length == 0) return 0;
        
        int maxFruits = 0;
        int type1 = -1;              // First fruit type in basket
        int type2 = -1;              // Second fruit type in basket
        int type2Count = 0;          // Consecutive count of type2
        int currentCount = 0;        // Current window size
        
        for (int fruit : fruits) {
            if (fruit == type1 || fruit == type2) {
                // Fruit fits in one of our baskets
                currentCount++;
            } else {
                // New fruit type - start new window
                currentCount = type2Count + 1;  // Keep only last fruit type + new one
            }
            
            // Update type2Count (consecutive count of most recent type)
            if (fruit == type2) {
                type2Count++;
            } else {
                type2Count = 1;
                type1 = type2;      // Previous type2 becomes type1
                type2 = fruit;      // New fruit becomes type2
            }
            
            maxFruits = Math.max(maxFruits, currentCount);
        }
        
        return maxFruits;
    }
    
    /**
     * APPROACH 4: Sliding Window with Array (Instead of HashMap)
     * 
     * INTUITION:
     * - If fruit types are small integers, use array instead of HashMap
     * - Array access is faster than HashMap operations
     * - Track distinct count separately
     * 
     * REASONING:
     * - Use array to count each fruit type
     * - Maintain distinctCount to know when we have > 2 types
     * - More efficient for small fruit type values
     * 
     * TIME COMPLEXITY: O(n) - single pass
     * SPACE COMPLEXITY: O(k) - where k is range of fruit types
     * 
     * NOTE: This approach assumes fruit types are reasonable integers
     */
    public int totalFruit_Array(int[] fruits) {
        // Find max fruit type to size our array
        int maxType = 0;
        for (int fruit : fruits) {
            maxType = Math.max(maxType, fruit);
        }
        
        int[] count = new int[maxType + 1];
        int left = 0;
        int maxFruits = 0;
        int distinctCount = 0;
        
        for (int right = 0; right < fruits.length; right++) {
            // Add fruit to basket
            if (count[fruits[right]] == 0) {
                distinctCount++;
            }
            count[fruits[right]]++;
            
            // Shrink window if needed
            while (distinctCount > 2) {
                count[fruits[left]]--;
                if (count[fruits[left]] == 0) {
                    distinctCount--;
                }
                left++;
            }
            
            maxFruits = Math.max(maxFruits, right - left + 1);
        }
        
        return maxFruits;
    }
    
    /**
     * APPROACH 5: Brute Force (For Understanding)
     * 
     * INTUITION:
     * - Try starting from each tree
     * - For each start, collect fruits until we need a third basket
     * - Track maximum fruits collected
     * 
     * REASONING:
     * - Simple nested loop approach
     * - Use HashSet to track distinct fruit types
     * - Good for understanding and verification
     * 
     * TIME COMPLEXITY: O(n²) - nested loops
     * SPACE COMPLEXITY: O(1) - set stores at most 3 elements
     */
    public int totalFruit_BruteForce(int[] fruits) {
        int maxFruits = 0;
        
        // Try starting from each position
        for (int start = 0; start < fruits.length; start++) {
            Set<Integer> basket = new HashSet<>();
            int count = 0;
            
            // Collect fruits from this starting position
            for (int end = start; end < fruits.length; end++) {
                basket.add(fruits[end]);
                
                // If we need more than 2 baskets, stop
                if (basket.size() > 2) {
                    break;
                }
                
                count++;
            }
            
            maxFruits = Math.max(maxFruits, count);
        }
        
        return maxFruits;
    }
    
    /**
     * APPROACH 6: Sliding Window with Detailed Explanation
     * 
     * This is the same as Approach 1 but with more detailed comments
     * to help understand the sliding window pattern clearly.
     */
    public int totalFruit_Explained(int[] fruits) {
        // Map to store: fruit_type -> count in current window
        Map<Integer, Integer> basket = new HashMap<>();
        
        int left = 0;        // Left boundary of window
        int maxFruits = 0;   // Maximum fruits we can collect
        
        // Right pointer expands the window
        for (int right = 0; right < fruits.length; right++) {
            // STEP 1: Add current fruit to basket
            int currentFruit = fruits[right];
            basket.put(currentFruit, basket.getOrDefault(currentFruit, 0) + 1);
            
            // STEP 2: Check if we have too many fruit types (> 2)
            // basket.size() tells us how many DISTINCT fruit types we have
            while (basket.size() > 2) {
                // STEP 3: Remove fruits from left until we have ≤ 2 types
                int leftFruit = fruits[left];
                
                // Decrease count of leftmost fruit
                int newCount = basket.get(leftFruit) - 1;
                basket.put(leftFruit, newCount);
                
                // If count reaches 0, remove this fruit type completely
                if (newCount == 0) {
                    basket.remove(leftFruit);
                }
                
                // Move left pointer forward
                left++;
            }
            
            // STEP 4: Update maximum with current window size
            int currentWindowSize = right - left + 1;
            maxFruits = Math.max(maxFruits, currentWindowSize);
        }
        
        return maxFruits;
    }
    
    /**
     * Helper method to visualize the solution
     */
    public void visualizeSolution(int[] fruits) {
        System.out.println("Input: " + Arrays.toString(fruits));
        
        Map<Integer, Integer> basket = new HashMap<>();
        int left = 0;
        int maxFruits = 0;
        int bestLeft = 0, bestRight = 0;
        
        for (int right = 0; right < fruits.length; right++) {
            basket.put(fruits[right], basket.getOrDefault(fruits[right], 0) + 1);
            
            while (basket.size() > 2) {
                basket.put(fruits[left], basket.get(fruits[left]) - 1);
                if (basket.get(fruits[left]) == 0) {
                    basket.remove(fruits[left]);
                }
                left++;
            }
            
            if (right - left + 1 > maxFruits) {
                maxFruits = right - left + 1;
                bestLeft = left;
                bestRight = right;
            }
        }
        
        System.out.println("Best window: [" + bestLeft + ", " + bestRight + "]");
        System.out.print("Fruits collected: [");
        for (int i = bestLeft; i <= bestRight; i++) {
            System.out.print(fruits[i]);
            if (i < bestRight) System.out.print(", ");
        }
        System.out.println("]");
        System.out.println("Total: " + maxFruits);
    }
    
    /**
     * Test cases with comprehensive coverage
     */
    public static void main(String[] args) {
        FruitIntoBaskets solution = new FruitIntoBaskets();
        
        // Test Case 1: Simple case
        int[] fruits1 = {1, 2, 1};
        System.out.println("=== Test Case 1 ===");
        solution.visualizeSolution(fruits1);
        System.out.println("HashMap: " + solution.totalFruit_HashMap(fruits1));
        System.out.println("Optimized: " + solution.totalFruit_Optimized(fruits1));
        System.out.println("Two Pointers: " + solution.totalFruit_TwoPointers(fruits1));
        System.out.println("Array: " + solution.totalFruit_Array(fruits1));
        System.out.println("Brute Force: " + solution.totalFruit_BruteForce(fruits1));
        System.out.println();
        
        // Test Case 2: Need to skip first tree
        int[] fruits2 = {0, 1, 2, 2};
        System.out.println("=== Test Case 2 ===");
        solution.visualizeSolution(fruits2);
        System.out.println("HashMap: " + solution.totalFruit_HashMap(fruits2));
        System.out.println("Optimized: " + solution.totalFruit_Optimized(fruits2));
        System.out.println("Two Pointers: " + solution.totalFruit_TwoPointers(fruits2));
        System.out.println("Array: " + solution.totalFruit_Array(fruits2));
        System.out.println("Brute Force: " + solution.totalFruit_BruteForce(fruits2));
        System.out.println();
        
        // Test Case 3: Multiple types
        int[] fruits3 = {1, 2, 3, 2, 2};
        System.out.println("=== Test Case 3 ===");
        solution.visualizeSolution(fruits3);
        System.out.println("HashMap: " + solution.totalFruit_HashMap(fruits3));
        System.out.println("Optimized: " + solution.totalFruit_Optimized(fruits3));
        System.out.println("Two Pointers: " + solution.totalFruit_TwoPointers(fruits3));
        System.out.println("Array: " + solution.totalFruit_Array(fruits3));
        System.out.println("Brute Force: " + solution.totalFruit_BruteForce(fruits3));
        System.out.println();
        
        // Test Case 4: All same fruit
        int[] fruits4 = {1, 1, 1, 1, 1};
        System.out.println("=== Test Case 4 (All same) ===");
        solution.visualizeSolution(fruits4);
        System.out.println("HashMap: " + solution.totalFruit_HashMap(fruits4));
        System.out.println("Optimized: " + solution.totalFruit_Optimized(fruits4));
        System.out.println("Two Pointers: " + solution.totalFruit_TwoPointers(fruits4));
        System.out.println("Array: " + solution.totalFruit_Array(fruits4));
        System.out.println("Brute Force: " + solution.totalFruit_BruteForce(fruits4));
        System.out.println();
        
        // Test Case 5: Alternating pattern
        int[] fruits5 = {1, 2, 1, 2, 1, 2};
        System.out.println("=== Test Case 5 (Alternating) ===");
        solution.visualizeSolution(fruits5);
        System.out.println("HashMap: " + solution.totalFruit_HashMap(fruits5));
        System.out.println("Optimized: " + solution.totalFruit_Optimized(fruits5));
        System.out.println("Two Pointers: " + solution.totalFruit_TwoPointers(fruits5));
        System.out.println("Array: " + solution.totalFruit_Array(fruits5));
        System.out.println("Brute Force: " + solution.totalFruit_BruteForce(fruits5));
        System.out.println();
        
        // Test Case 6: Complex pattern
        int[] fruits6 = {3, 3, 3, 1, 2, 1, 1, 2, 3, 3, 4};
        System.out.println("=== Test Case 6 (Complex) ===");
        solution.visualizeSolution(fruits6);
        System.out.println("HashMap: " + solution.totalFruit_HashMap(fruits6));
        System.out.println("Optimized: " + solution.totalFruit_Optimized(fruits6));
        System.out.println("Two Pointers: " + solution.totalFruit_TwoPointers(fruits6));
        System.out.println("Array: " + solution.totalFruit_Array(fruits6));
        System.out.println("Brute Force: " + solution.totalFruit_BruteForce(fruits6));
    }
}

/*
 * COMPREHENSIVE ANALYSIS AND KEY INSIGHTS:
 * 
 * 1. PROBLEM TRANSFORMATION:
 *    Original: "Pick fruits with 2 baskets, each holding one type"
 *    Transformed: "Find longest subarray with at most 2 distinct elements"
 *    This makes it a classic sliding window problem!
 * 
 * 2. WHY SLIDING WINDOW WORKS:
 *    - We want a contiguous sequence (can't skip trees)
 *    - We have a constraint (max 2 types)
 *    - We want to maximize length
 *    → Perfect for sliding window!
 * 
 * 3. PATTERN RECOGNITION - This problem is identical to:
 *    - "Longest substring with at most 2 distinct characters"
 *    - "Longest subarray with at most K distinct elements" (where K=2)
 *    - All these use the SAME sliding window template
 * 
 * 4. THE SLIDING WINDOW TEMPLATE (Universal):
 *    ```
 *    left = 0
 *    for right in range(n):
 *        add_to_window(right)
 *        while window_invalid():
 *            remove_from_window(left)
 *            left++
 *        update_result()
 *    ```
 * 
 * 5. WHEN TO USE EACH APPROACH:
 *    
 *    ├─ HashMap (Approach 1): ⭐ BEST FOR INTERVIEWS
 *    │  • Most intuitive and clear
 *    │  • Easy to explain
 *    │  • Works for any fruit type values
 *    │  • Use this by default
 *    │
 *    ├─ Optimized Window (Approach 2): Best for production
 *    │  • Cleanest code
 *    │  • Same time complexity but slightly faster
 *    │  • Requires understanding of why it works
 *    │
 *    ├─ Two Pointers (Approach 3): Clever but tricky
 *    │  • O(1) space (no HashMap)
 *    │  • Hard to come up with in interview
 *    │  • Easy to make mistakes
 *    │
 *    ├─ Array (Approach 4): When fruit types are small
 *    │  • Faster than HashMap (array access vs hash)
 *    │  • Only if fruit types ≤ 100,000 or so
 *    │
 *    └─ Brute Force (Approach 5): For understanding only
 *       • O(n²) - too slow
 *       • Good for testing and verification
 * 
 * 6. COMMON MISTAKES TO AVOID:
 *    ✗ Forgetting to remove fruit type when count reaches 0
 *    ✗ Not using basket.size() to check distinct types
 *    ✗ Using 'if' when you should use 'while' (or vice versa)
 *    ✗ Off-by-one errors in window size calculation
 * 
 * 7. HOW TO EXTEND THIS PATTERN:
 *    - Change 2 → K: "At most K distinct elements"
 *    - Change "at most" → "exactly": Add another condition
 *    - Change "distinct" → "sum/product": Different window property
 *    - The template remains the same!
 * 
 * 8. INTERVIEW STRATEGY:
 *    Step 1: Recognize it's "longest subarray with constraint"
 *    Step 2: Identify constraint: "at most 2 distinct types"
 *    Step 3: Apply sliding window template
 *    Step 4: Use HashMap to track distinct types
 *    Step 5: Explain time/space complexity
 *    
 *    If asked to optimize:
 *    → Mention Approach 2 (optimized window)
 *    → Mention Approach 3 (O(1) space) if interviewer impressed
 * 
 * 9. TIME/SPACE COMPLEXITY SUMMARY:
 *    All approaches (except brute force): O(n) time
 *    - HashMap/Array: O(1) space (max 3 elements/reasonable range)
 *    - Two Pointers: O(1) space (only variables)
 *    - Brute Force: O(n²) time, O(1) space
 * 
 * 10. RELATED PROBLEMS (Same Pattern):
 *     - Max Consecutive Ones III (K zeros allowed)
 *     - Longest Substring Without Repeating Characters
 *     - Minimum Window Substring
 *     - Longest Repeating Character Replacement
 *     All use variations of this sliding window template!
 */
