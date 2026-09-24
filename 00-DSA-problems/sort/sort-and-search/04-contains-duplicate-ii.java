/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an array of integers `nums` and an integer `k`.
 * We need to figure out if there are any duplicate numbers in the array that 
 * are situated "close enough" to each other. Specifically, the distance 
 * between their indices must be less than or equal to `k`. 
 * If we find at least one such pair, we return true. Otherwise, return false.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW (Confirm before writing code)
 * ============================================================================
 * 1. What if `k` is 0? 
 *    (Since we need distinct indices `i != j`, the absolute difference must be >= 1. 
 *     So if k == 0, it's impossible, and we should return false.)
 * 2. Can `k` be larger than the length of the array?
 *    (Yes, constraints say k can be up to 10^4 while array length is up to 10^3. 
 *     Our solution should handle this gracefully without throwing OutOfBounds.)
 * 3. What if the array has fewer than 2 elements?
 *    (Impossible to form a pair, return false immediately.)
 * 4. Are there any space complexity constraints? 
 *    (If memory is extremely tight, we might be forced into an O(1) space Brute 
 *     Force approach, though it trades off heavily on time.)
 * 5. Should we optimize for execution speed or memory usage?
 *    (This helps decide between a HashMap approach vs a Sliding Window Set vs an Array.)
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (The "Obvious" start):
 *    - Use two nested loops. For every element, check the next `k` elements.
 *    - Time: O(N * min(N, k)), Space: O(1). Mention this to show baseline thinking, 
 *      but state that we can do better.
 * 
 * 2. HashMap / Dictionary (The "Intuitive" upgrade):
 *    - We iterate through the array, storing the element as the key and its index 
 *      as the value in a Map. 
 *    - If we see an element that's already in the Map, we check the difference 
 *      between the current index and the stored index. If it's <= k, we found a match!
 *    - Time: O(N), Space: O(N).
 * 
 * 3. HashSet Sliding Window (The "Optimal" Standard):
 *    - We only care about a "window" of size `k`. If an element is repeated within 
 *      this window, we win.
 *    - Maintain a HashSet of the elements currently in our window. 
 *    - As we iterate, if the set already contains the number, return true. 
 *    - Otherwise, add it. If the set grows larger than `k`, remove the oldest element 
 *      (which is at index `i - k`).
 *    - Time: O(N), Space: O(min(N, k)).
 * 
 * 4. Array Mapping (The "Constraint-Hacker" flex):
 *    - Notice the constraint: -10^3 <= nums[i] <= 10^3. 
 *    - This means there are only 2001 possible distinct values! 
 *    - Instead of a Map/Set, we can use a simple integer array of size 2001 to store 
 *      the last seen indices, mapping negative numbers using an offset. 
 *    - Time: O(N), Space: O(1) [Fixed size 2001 array].
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Sliding Window Approach)
 * ============================================================================
 * Example: nums = [1, 2, 3, 1, 2, 3], k = 2
 * 
 * Window size max = 2
 * Set = {}
 * 
 * i = 0, num = 1 -> Not in set. Add 1. Set: {1}
 * i = 1, num = 2 -> Not in set. Add 2. Set: {1, 2}
 * i = 2, num = 3 -> Not in set. Add 3. Set: {1, 2, 3}. 
 *                   Size > k (3 > 2), so remove nums[2-2] = nums[0] = 1. Set: {2, 3}
 * i = 3, num = 1 -> Not in set. Add 1. Set: {2, 3, 1}.
 *                   Size > k, remove nums[3-2] = nums[1] = 2. Set: {3, 1}
 * i = 4, num = 2 -> Not in set. Add 2. Set: {3, 1, 2}.
 *                   Size > k, remove nums[4-2] = nums[2] = 3. Set: {1, 2}
 * i = 5, num = 3 -> Not in set. Loop ends. 
 * 
 * Result: False (No duplicates within distance 2)
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

public class ContainsDuplicateII {

    public static void main(String[] args) {
        int[] nums1 = {1, 2, 3, 1};
        int k1 = 3;

        int[] nums2 = {1, 0, 1, 1};
        int k2 = 1;

        int[] nums3 = {1, 2, 3, 1, 2, 3};
        int k3 = 2;

        System.out.println("Testing nums = [1, 2, 3, 1], k = 3");
        System.out.println("Brute Force: " + containsNearbyDuplicateBruteForce(nums1, k1));
        System.out.println("HashMap:     " + containsNearbyDuplicateMap(nums1, k1));
        System.out.println("HashSet:     " + containsNearbyDuplicateSet(nums1, k1));
        System.out.println("Array Opt:   " + containsNearbyDuplicateArrayOpt(nums1, k1));
        System.out.println("--------------------------------------------------");

        System.out.println("Testing nums = [1, 2, 3, 1, 2, 3], k = 2");
        System.out.println("Brute Force: " + containsNearbyDuplicateBruteForce(nums3, k3));
        System.out.println("HashMap:     " + containsNearbyDuplicateMap(nums3, k3));
        System.out.println("HashSet:     " + containsNearbyDuplicateSet(nums3, k3));
        System.out.println("Array Opt:   " + containsNearbyDuplicateArrayOpt(nums3, k3));
    }

    /**
     * SOLUTION 1: Brute Force
     * 
     * Idea: Check every element against the next `k` elements.
     * 
     * Time Complexity: O(N * min(N, k)) - In the worst case, we check k elements for every element.
     * Space Complexity: O(1) - No extra space used.
     * 
     * Note: Might hit Time Limit Exceeded (TLE) on large arrays with large k.
     */
    public static boolean containsNearbyDuplicateBruteForce(int[] nums, int k) {
        if (nums == null || nums.length < 2 || k == 0) return false;

        for (int i = 0; i < nums.length; i++) {
            // Check the next k elements, ensuring we don't go out of bounds
            int limit = Math.min(nums.length - 1, i + k);
            for (int j = i + 1; j <= limit; j++) {
                if (nums[i] == nums[j]) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * SOLUTION 2: HashMap Approach
     * 
     * Idea: Keep a history of the most recently seen index for each number.
     * If we encounter the number again, check if the distance from its last 
     * seen index is <= k. If not, update the map with the new, closer index.
     * 
     * Time Complexity: O(N) - One pass through the array. Map operations are O(1) avg.
     * Space Complexity: O(N) - In the worst case, all elements are unique and stored.
     */
    public static boolean containsNearbyDuplicateMap(int[] nums, int k) {
        if (nums == null || nums.length < 2 || k == 0) return false;

        Map<Integer, Integer> lastSeenMap = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            int currentNum = nums[i];
            // If the number exists in our map and the distance is <= k
            if (lastSeenMap.containsKey(currentNum) && i - lastSeenMap.get(currentNum) <= k) {
                return true;
            }
            // Update the most recent index for this number
            lastSeenMap.put(currentNum, i);
        }

        return false;
    }

    /**
     * SOLUTION 3: HashSet (Sliding Window) Approach - (Most standard optimal)
     * 
     * Idea: Maintain a "moving window" of the last `k` elements using a HashSet.
     * Since Set.add() returns false if the element already exists, we can do 
     * the check and the add in one clean step.
     * 
     * Time Complexity: O(N) - One pass. Set operations are O(1) avg.
     * Space Complexity: O(min(N, k)) - The Set never grows larger than k.
     * 
     * Why not Streams? Streams don't map cleanly to sliding windows where early
     * termination and localized state mutation (the Set) are required. A classic
     * loop is much more readable and performant here.
     */
    public static boolean containsNearbyDuplicateSet(int[] nums, int k) {
        if (nums == null || nums.length < 2 || k == 0) return false;

        Set<Integer> window = new HashSet<>();

        for (int i = 0; i < nums.length; i++) {
            // If adding fails, it means the number is already in the window!
            if (!window.add(nums[i])) {
                return true;
            }
            // Maintain the window size: if it exceeds k, remove the oldest element
            if (window.size() > k) {
                window.remove(nums[i - k]);
            }
        }

        return false;
    }

    /**
     * SOLUTION 4: Array-Based Constraints Optimization
     * 
     * Idea: The constraints say nums[i] is strictly between -1000 and +1000.
     * This means there are exactly 2001 possible numbers. We don't need a Heavy 
     * HashMap/HashSet! We can use a flat array of size 2001, tracking the 
     * "last seen index". To differentiate index `0` from "never seen", we can 
     * store `i + 1` or prefill the array with -1.
     * 
     * Time Complexity: O(N) - One pass.
     * Space Complexity: O(1) - The array size is fixed at 2001 regardless of N.
     */
    public static boolean containsNearbyDuplicateArrayOpt(int[] nums, int k) {
        if (nums == null || nums.length < 2 || k == 0) return false;

        // Domain: -1000 to +1000 -> 2001 elements. Offset by +1000.
        int[] lastSeen = new int[2001];
        // Fill with -1 to indicate "not seen yet"
        Arrays.fill(lastSeen, -1);

        for (int i = 0; i < nums.length; i++) {
            int mappedValue = nums[i] + 1000;
            
            int prevIndex = lastSeen[mappedValue];
            if (prevIndex != -1 && i - prevIndex <= k) {
                return true;
            }
            
            // Update the index
            lastSeen[mappedValue] = i;
        }

        return false;
    }
}
