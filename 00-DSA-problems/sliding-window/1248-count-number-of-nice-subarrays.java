import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

class NiceSubarrays {
    
    // Approach 1: Brute Force
    // Time: O(n²), Space: O(1)
    public int numberOfSubarraysBruteForce(int[] nums, int k) {
        int count = 0;
        
        for (int i = 0; i < nums.length; i++) {
            int oddCount = 0;
            for (int j = i; j < nums.length; j++) {
                if (nums[j] % 2 == 1) {
                    oddCount++;
                }
                if (oddCount == k) {
                    count++;
                } else if (oddCount > k) {
                    break;  // No point continuing
                }
            }
        }
        
        return count;
    }
    
    // Approach 2: Sliding Window (OPTIMAL)
    // atMost(k) - atMost(k-1) = exactly(k)
    // Time: O(n), Space: O(1)
    public int numberOfSubarrays(int[] nums, int k) {
        return atMostKOdd(nums, k) - atMostKOdd(nums, k - 1);
    }
    
    private int atMostKOdd(int[] nums, int k) {
        if (k < 0) return 0;
        
        int count = 0;
        int oddCount = 0;
        int left = 0;
        
        for (int right = 0; right < nums.length; right++) {
            if (nums[right] % 2 == 1) {
                oddCount++;
            }
            
            // Shrink window if too many odd numbers
            while (oddCount > k) {
                if (nums[left] % 2 == 1) {
                    oddCount--;
                }
                left++;
            }
            
            // All subarrays from left to right are valid
            count += right - left + 1;
        }
        
        return count;
    }
    
    // Approach 3: Direct Sliding Window with Counting
    // Time: O(n), Space: O(1)
    public int numberOfSubarraysDirect(int[] nums, int k) {
        int count = 0;
        int oddCount = 0;
        int left = 0;
        int prefixCount = 0;  // Count of even numbers before first odd in window
        
        for (int right = 0; right < nums.length; right++) {
            if (nums[right] % 2 == 1) {
                oddCount++;
                prefixCount = 0;  // Reset prefix when we hit an odd
            }
            
            // When we have exactly k odds, count valid subarrays
            while (oddCount == k) {
                prefixCount++;  // Count this valid starting position
                
                if (nums[left] % 2 == 1) {
                    oddCount--;
                }
                left++;
            }
            
            count += prefixCount;
        }
        
        return count;
    }
    
    // Approach 4: HashMap with Prefix Sum
    // Convert to prefix sum problem: count[odd numbers up to i]
    // Time: O(n), Space: O(n)
    public int numberOfSubarraysHashMap(int[] nums, int k) {
        Map<Integer, Integer> prefixCount = new HashMap<>();
        prefixCount.put(0, 1);  // Base case: 0 odd numbers
        
        int count = 0;
        int oddCount = 0;
        
        for (int num : nums) {
            if (num % 2 == 1) {
                oddCount++;
            }
            
            // Check if there's a prefix with (oddCount - k) odd numbers
            count += prefixCount.getOrDefault(oddCount - k, 0);
            
            // Add current prefix
            prefixCount.put(oddCount, prefixCount.getOrDefault(oddCount, 0) + 1);
        }
        
        return count;
    }
    
    // Approach 5: Convert to Binary and Use Prefix Sum
    // Convert odd->1, even->0, then find subarrays with sum=k
    // Time: O(n), Space: O(n)
    public int numberOfSubarraysBinary(int[] nums, int k) {
        int n = nums.length;
        int[] binary = new int[n];
        
        // Convert: odd->1, even->0
        for (int i = 0; i < n; i++) {
            binary[i] = nums[i] % 2;
        }
        
        // Use prefix sum approach
        Map<Integer, Integer> prefixSum = new HashMap<>();
        prefixSum.put(0, 1);
        
        int count = 0;
        int sum = 0;
        
        for (int val : binary) {
            sum += val;
            count += prefixSum.getOrDefault(sum - k, 0);
            prefixSum.put(sum, prefixSum.getOrDefault(sum, 0) + 1);
        }
        
        return count;
    }
    
    // Approach 6: Track Odd Indices
    // Store positions of odd numbers, then count combinations
    // Time: O(n), Space: O(n)
    public int numberOfSubarraysIndices(int[] nums, int k) {
        List<Integer> oddIndices = new ArrayList<>();
        oddIndices.add(-1);  // Sentinel for before array
        
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] % 2 == 1) {
                oddIndices.add(i);
            }
        }
        oddIndices.add(nums.length);  // Sentinel for after array
        
        int count = 0;
        
        // For each window of k consecutive odd numbers
        for (int i = 1; i + k < oddIndices.size(); i++) {
            int leftGap = oddIndices.get(i) - oddIndices.get(i - 1);
            int rightGap = oddIndices.get(i + k) - oddIndices.get(i + k - 1);
            count += leftGap * rightGap;
        }
        
        return count;
    }
    
    // Approach 7: Array Prefix Count (Space Optimized HashMap)
    // Time: O(n), Space: O(n)
    public int numberOfSubarraysArray(int[] nums, int k) {
        int[] prefixCount = new int[nums.length + 1];
        prefixCount[0] = 1;
        
        int count = 0;
        int oddCount = 0;
        
        for (int num : nums) {
            if (num % 2 == 1) {
                oddCount++;
            }
            
            if (oddCount >= k) {
                count += prefixCount[oddCount - k];
            }
            
            prefixCount[oddCount]++;
        }
        
        return count;
    }
    
    // Helper: Visualize subarrays
    private static void visualizeSubarrays(int[] nums, int k) {
        System.out.println("\n=== All Nice Subarrays (k=" + k + ") ===");
        System.out.println("Array: " + Arrays.toString(nums));
        System.out.println("\nNice subarrays:");
        
        int count = 0;
        for (int i = 0; i < nums.length; i++) {
            int oddCount = 0;
            for (int j = i; j < nums.length; j++) {
                if (nums[j] % 2 == 1) {
                    oddCount++;
                }
                if (oddCount == k) {
                    count++;
                    System.out.print("  [");
                    for (int idx = i; idx <= j; idx++) {
                        System.out.print(nums[idx]);
                        if (idx < j) System.out.print(", ");
                    }
                    System.out.println("]  (indices " + i + " to " + j + ")");
                } else if (oddCount > k) {
                    break;
                }
            }
        }
        System.out.println("\nTotal count: " + count);
    }
    
    // Test cases
    public static void main(String[] args) {
        NiceSubarrays solver = new NiceSubarrays();
        
        // Test Case 1
        System.out.println("Test Case 1:");
        int[] nums1 = {1, 1, 2, 1, 1};
        int k1 = 3;
        System.out.println("Input: nums = " + Arrays.toString(nums1) + ", k = " + k1);
        System.out.println("Output: " + solver.numberOfSubarrays(nums1, k1));
        System.out.println("Expected: 2");
        visualizeSubarrays(nums1, k1);
        
        // Test Case 2
        System.out.println("\n\nTest Case 2:");
        int[] nums2 = {2, 4, 6};
        int k2 = 1;
        System.out.println("Input: nums = " + Arrays.toString(nums2) + ", k = " + k2);
        System.out.println("Output: " + solver.numberOfSubarrays(nums2, k2));
        System.out.println("Expected: 0");
        visualizeSubarrays(nums2, k2);
        
        // Test Case 3
        System.out.println("\n\nTest Case 3:");
        int[] nums3 = {2, 2, 2, 1, 2, 2, 1, 2, 2, 2};
        int k3 = 2;
        System.out.println("Input: nums = " + Arrays.toString(nums3) + ", k = " + k3);
        System.out.println("Output: " + solver.numberOfSubarrays(nums3, k3));
        System.out.println("Expected: 16");
        visualizeSubarrays(nums3, k3);
        
        // Test Case 4: All odd
        System.out.println("\n\nTest Case 4: All Odd");
        int[] nums4 = {1, 3, 5, 7};
        int k4 = 2;
        System.out.println("Input: nums = " + Arrays.toString(nums4) + ", k = " + k4);
        System.out.println("Output: " + solver.numberOfSubarrays(nums4, k4));
        System.out.println("Expected: 3");
        
        // Test Case 5: k = 0
        System.out.println("\n\nTest Case 5: k = 0");
        int[] nums5 = {2, 2, 2};
        int k5 = 0;
        System.out.println("Input: nums = " + Arrays.toString(nums5) + ", k = " + k5);
        System.out.println("Output: " + solver.numberOfSubarrays(nums5, k5));
        System.out.println("Expected: 6 (all subarrays with no odd numbers)");
        
        // Test Case 6: Single element
        System.out.println("\n\nTest Case 6: Single Element");
        int[] nums6 = {1};
        int k6 = 1;
        System.out.println("Input: nums = " + Arrays.toString(nums6) + ", k = " + k6);
        System.out.println("Output: " + solver.numberOfSubarrays(nums6, k6));
        System.out.println("Expected: 1");
        
        // Step-by-step trace
        System.out.println("\n\n=== Step-by-Step: Sliding Window ===");
        stepByStepSlidingWindow(nums1, k1);
        
        // AtMost technique explanation
        System.out.println("\n\n=== AtMost Technique Explanation ===");
        explainAtMostTechnique(nums1, k1);
        
        // Prefix sum approach
        System.out.println("\n\n=== Prefix Sum Approach ===");
        explainPrefixSum(nums1, k1);
        
        // Algorithm comparison
        System.out.println("\n\n=== Algorithm Comparison ===");
        compareAlgorithms();
        
        // Compare all approaches
        System.out.println("\n\n=== Comparing All Approaches ===");
        compareAllApproaches(nums3, k3);
    }
    
    private static void stepByStepSlidingWindow(int[] nums, int k) {
        System.out.println("Array: " + Arrays.toString(nums) + ", k = " + k);
        System.out.println("\nUsing: atMost(k) - atMost(k-1)");
        
        System.out.println("\nComputing atMost(" + k + "):");
        traceAtMost(nums, k);
        
        System.out.println("\nComputing atMost(" + (k-1) + "):");
        traceAtMost(nums, k - 1);
    }
    
    private static void traceAtMost(int[] nums, int k) {
        int count = 0;
        int oddCount = 0;
        int left = 0;
        
        System.out.println("┌───────┬─────────┬──────────┬──────────┬───────────────────┐");
        System.out.println("│ right │ nums[r] │ oddCount │ window   │ subarrays added   │");
        System.out.println("├───────┼─────────┼──────────┼──────────┼───────────────────┤");
        
        for (int right = 0; right < nums.length; right++) {
            if (nums[right] % 2 == 1) {
                oddCount++;
            }
            
            while (oddCount > k) {
                if (nums[left] % 2 == 1) {
                    oddCount--;
                }
                left++;
            }
            
            int added = right - left + 1;
            count += added;
            
            System.out.printf("│   %d   │    %d    │    %d     │ [%d,%d]   │ %d (total: %d)    │%n",
                            right, nums[right], oddCount, left, right, added, count);
        }
        
        System.out.println("└───────┴─────────┴──────────┴──────────┴───────────────────┘");
        System.out.println("Total: " + count);
    }
    
    private static void explainAtMostTechnique(int[] nums, int k) {
        NiceSubarrays solver = new NiceSubarrays();
        
        System.out.println("Key Insight: exactly(k) = atMost(k) - atMost(k-1)");
        System.out.println("\nWhy this works:");
        System.out.println("- atMost(k): counts subarrays with ≤ k odd numbers");
        System.out.println("- atMost(k-1): counts subarrays with ≤ k-1 odd numbers");
        System.out.println("- Difference: counts subarrays with exactly k odd numbers");
        
        int atMostK = solver.atMostKOdd(nums, k);
        int atMostK1 = solver.atMostKOdd(nums, k - 1);
        int exactlyK = atMostK - atMostK1;
        
        System.out.println("\nFor nums = " + Arrays.toString(nums) + ", k = " + k + ":");
        System.out.println("atMost(" + k + ") = " + atMostK);
        System.out.println("atMost(" + (k-1) + ") = " + atMostK1);
        System.out.println("exactly(" + k + ") = " + exactlyK);
        
        System.out.println("\nExample:");
        System.out.println("atMost(3) includes: subarrays with 0,1,2,3 odd numbers");
        System.out.println("atMost(2) includes: subarrays with 0,1,2 odd numbers");
        System.out.println("Difference: only subarrays with 3 odd numbers!");
    }
    
    private static void explainPrefixSum(int[] nums, int k) {
        System.out.println("Convert to Prefix Sum Problem:");
        System.out.println("\nOriginal: " + Arrays.toString(nums));
        
        System.out.print("Odd/Even: [");
        for (int i = 0; i < nums.length; i++) {
            System.out.print(nums[i] % 2 == 1 ? "O" : "E");
            if (i < nums.length - 1) System.out.print(", ");
        }
        System.out.println("]");
        
        System.out.print("Binary:   [");
        for (int i = 0; i < nums.length; i++) {
            System.out.print(nums[i] % 2);
            if (i < nums.length - 1) System.out.print(", ");
        }
        System.out.println("]");
        
        System.out.print("\nPrefix:   [0, ");
        int sum = 0;
        for (int i = 0; i < nums.length; i++) {
            sum += nums[i] % 2;
            System.out.print(sum);
            if (i < nums.length - 1) System.out.print(", ");
        }
        System.out.println("]");
        
        System.out.println("\nNow find subarrays where: prefix[j] - prefix[i] = k");
        System.out.println("Or equivalently: prefix[j] = prefix[i] + k");
        System.out.println("\nUse HashMap to count occurrences of each prefix sum!");
    }
    
    private static void compareAlgorithms() {
        System.out.println("┌──────────────────────┬──────────────┬──────────────┬─────────────────┐");
        System.out.println("│ Approach             │ Time         │ Space        │ Notes           │");
        System.out.println("├──────────────────────┼──────────────┼──────────────┼─────────────────┤");
        System.out.println("│ Brute Force          │ O(n²)        │ O(1)         │ Simple          │");
        System.out.println("│ AtMost Technique     │ O(n)         │ O(1)         │ Optimal         │");
        System.out.println("│ Direct Sliding Win   │ O(n)         │ O(1)         │ Complex logic   │");
        System.out.println("│ HashMap Prefix       │ O(n)         │ O(n)         │ Intuitive       │");
        System.out.println("│ Binary + Prefix      │ O(n)         │ O(n)         │ Clear transform │");
        System.out.println("│ Odd Indices          │ O(n)         │ O(n)         │ Elegant         │");
        System.out.println("│ Array Prefix         │ O(n)         │ O(n)         │ Space efficient │");
        System.out.println("└──────────────────────┴──────────────┴──────────────┴─────────────────┘");
    }
    
    private static void compareAllApproaches(int[] nums, int k) {
        NiceSubarrays solver = new NiceSubarrays();
        
        System.out.println("Input: nums = " + Arrays.toString(nums) + ", k = " + k);
        System.out.println("\nBrute Force:  " + solver.numberOfSubarraysBruteForce(nums, k));
        System.out.println("AtMost:       " + solver.numberOfSubarrays(nums, k));
        System.out.println("Direct:       " + solver.numberOfSubarraysDirect(nums, k));
        System.out.println("HashMap:      " + solver.numberOfSubarraysHashMap(nums, k));
        System.out.println("Binary:       " + solver.numberOfSubarraysBinary(nums, k));
        System.out.println("Indices:      " + solver.numberOfSubarraysIndices(nums, k));
        System.out.println("Array:        " + solver.numberOfSubarraysArray(nums, k));
        
        // Performance test
        int[] large = new int[50000];
        Random rand = new Random(42);
        for (int i = 0; i < large.length; i++) {
            large[i] = rand.nextInt(100);
        }
        
        System.out.println("\nPerformance Test (n=50000, k=100):");
        
        long start = System.nanoTime();
        solver.numberOfSubarrays(large, 100);
        long end = System.nanoTime();
        System.out.println("AtMost:   " + (end - start) / 1_000_000.0 + " ms");
        
        start = System.nanoTime();
        solver.numberOfSubarraysHashMap(large, 100);
        end = System.nanoTime();
        System.out.println("HashMap:  " + (end - start) / 1_000_000.0 + " ms");
        
        start = System.nanoTime();
        solver.numberOfSubarraysIndices(large, 100);
        end = System.nanoTime();
        System.out.println("Indices:  " + (end - start) / 1_000_000.0 + " ms");
    }
}

/*
DETAILED EXPLANATION:

PROBLEM: Count Nice Subarrays

A subarray is "nice" if it contains exactly k odd numbers.
Count all nice subarrays.

EXAMPLE WALKTHROUGH: [1,1,2,1,1], k=3

Let O=odd, E=even: [O, O, E, O, O]

All subarrays with exactly 3 odd numbers:
1. [1,1,2,1] - indices 0-3: has 3 odds ✓
2. [1,2,1,1] - indices 1-4: has 3 odds ✓

Total: 2

KEY INSIGHT 1: atMost(k) - atMost(k-1) = exactly(k)

Instead of directly counting exactly k odd numbers,
we can use: exactly(k) = atMost(k) - atMost(k-1)

Why this works:
- atMost(k): all subarrays with ≤ k odd numbers
- atMost(k-1): all subarrays with ≤ k-1 odd numbers
- Difference: subarrays with exactly k odd numbers

ALGORITHM (ATMOST TECHNIQUE):

atMostKOdd(nums, k):
1. Use sliding window with left, right pointers
2. Expand right: add elements
3. If oddCount > k: shrink from left
4. For each right position: count = right - left + 1
   (all subarrays ending at right are valid)

DETAILED TRACE: [1,1,2,1,1], k=3

Computing atMost(3):

right=0, num=1 (odd): oddCount=1, window=[0,0]
  Valid subarrays ending at 0: [1]
  Count: 1

right=1, num=1 (odd): oddCount=2, window=[0,1]
  Valid subarrays ending at 1: [1], [1,1]
  Count: 2

right=2, num=2 (even): oddCount=2, window=[0,2]
  Valid subarrays ending at 2: [2], [1,2], [1,1,2]
  Count: 3

right=3, num=1 (odd): oddCount=3, window=[0,3]
  Valid subarrays ending at 3: [1], [2,1], [1,2,1], [1,1,2,1]
  Count: 4

right=4, num=1 (odd): oddCount=4 > 3!
  Shrink: left=1, oddCount=3
  Valid subarrays ending at 4: [1,2,1,1], [2,1,1], [1,1]
  Count: 4 (from index 1 to 4)

Total atMost(3) = 1 + 2 + 3 + 4 + 4 = 14

Computing atMost(2):
Following similar logic... = 12

exactly(3) = 14 - 12 = 2 ✓

WHY "right - left + 1" COUNTS ALL VALID SUBARRAYS?

At each position right, all subarrays from [left, right] to [right, right]
are valid (have ≤ k odd numbers).

Number of such subarrays = right - left + 1

Example: left=1, right=4
Valid subarrays:
- [1, 4]: starting at 1
- [2, 4]: starting at 2
- [3, 4]: starting at 3
- [4, 4]: starting at 4
Total: 4 subarrays = 4 - 1 + 1 = 4

KEY INSIGHT 2: CONVERT TO PREFIX SUM

Alternative approach: treat odd as 1, even as 0.
Problem becomes: find subarrays with sum = k

Array: [1, 1, 2, 1, 1]
Binary: [1, 1, 0, 1, 1]
Prefix: [0, 1, 2, 2, 3, 4]

For subarray [i, j] to have k odd numbers:
  prefix[j+1] - prefix[i] = k
  prefix[j+1] = prefix[i] + k

Use HashMap to count prefix sums!

HASHMAP APPROACH:

Map: prefix_sum → count

For each position:
1. Compute current prefix sum (oddCount)
2. Check if (oddCount - k) exists in map
   → If yes, those positions can be start points
3. Add current prefix to map

Example: [1,1,2,1,1], k=3

i=0, num=1: oddCount=1
  Check: oddCount-k = 1-3 = -2 (not in map)
  count += 0
  map: {0:1, 1:1}

i=1, num=1: oddCount=2
  Check: oddCount-k = 2-3 = -1 (not in map)
  count += 0
  map: {0:1, 1:1, 2:1}

i=2, num=2: oddCount=2
  Check: oddCount-k = 2-3 = -1 (not in map)
  count += 0
  map: {0:1, 1:1, 2:2}

i=3, num=1: oddCount=3
  Check: oddCount-k = 3-3 = 0 (in map, count=1)
  count += 1
  map: {0:1, 1:1, 2:2, 3:1}

i=4, num=1: oddCount=4
  Check: oddCount-k = 4-3 = 1 (in map, count=1)
  count += 1
  map: {0:1, 1:1, 2:2, 3:1, 4:1}

Total: 2 ✓

KEY INSIGHT 3: ODD INDICES APPROACH

Store positions of all odd numbers.
For each window of k consecutive odds,
count combinations of left and right extensions.

Example: [2,2,2,1,2,2,1,2,2,2], k=2
Odd positions: [-1, 3, 6, 10] (with sentinels)

Window 1: odds at positions 3 and 6
  Left gap: 3 - (-1) = 4 positions
  Right gap: 10 - 6 = 4 positions
  Combinations: 4 × 4 = 16 ✓

This gives ALL valid subarrays!

COMPLEXITY ANALYSIS:

AtMost Technique:
Time: O(n)
  - Two passes: atMost(k) and atMost(k-1)
  - Each pass: O(n)
Space: O(1)

HashMap Prefix Sum:
Time: O(n)
  - Single pass
Space: O(n)
  - HashMap stores up to n prefix sums

Odd Indices:
Time: O(n)
  - Find odd positions: O(n)
  - Count combinations: O(number of odds)
Space: O(n)
  - Store odd positions

Brute Force:
Time: O(n²)
  - For each start: O(n)
  - Find end with k odds: O(n)
Space: O(1)

EDGE CASES:

1. k = 0: Count subarrays with no odd numbers
   Example: [2,2,2] → 6 subarrays

2. All odd: [1,3,5,7], k=2 → 3 subarrays
   [1,3], [3,5], [5,7]

3. All even: [2,4,6], k=1 → 0 subarrays

4. k > count of odds: return 0

5. Single element: [1], k=1 → 1

6. k = 1: Many valid subarrays

COMPARISON WITH SIMILAR PROBLEMS:

Subarray Sum Equals K:
- Use prefix sum + HashMap
- Similar technique applicable

Subarrays with K Different Integers:
- Use atMost(k) - atMost(k-1)
- Same trick!

Binary Subarrays with Sum:
- Direct application
- Treat as 0/1 array

INTERVIEW STRATEGY:

1. Recognize pattern: "exactly k" is hard
2. Transform to "at most k"
3. Use formula: exactly(k) = atMost(k) - atMost(k-1)
4. Implement sliding window for atMost
5. Explain prefix sum alternative
6. Discuss complexity: O(n) time, O(1) or O(n) space
7. Handle edge cases

COMMON MISTAKES:

1. Trying to directly track exactly k (complex)
2. Not understanding atMost formula
3. Wrong window shrinking logic
4. Forgetting to count all subarrays at each step
5. Not handling k=0 or k > array length

RELATED PROBLEMS:

1. Subarray Sum Equals K
2. Subarray Product Less Than K
3. Longest Substring with At Most K Distinct
4. Count Number of Nice Subarrays (this one!)
5. Subarrays with K Different Integers

The atMost(k) - atMost(k-1) technique is POWERFUL!
*/
