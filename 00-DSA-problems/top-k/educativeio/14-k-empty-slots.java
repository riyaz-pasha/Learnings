import java.util.Arrays;
import java.util.TreeSet;

/**
 * PROBLEM STATEMENT:
 * You are given n bulbs arranged in a row, numbered from 1 to n. Initially, all bulbs are turned off.
 * Each day, exactly one bulb is switched on. You are given an array, `bulbs` of length n 
 * where `bulbs[i] = x` means that on day i + 1 (1-indexed), the bulb at position x (also 1-indexed) is turned on.
 * 
 * Given an integer k, determine the earliest day on which there are two bulbs that are ON 
 * such that exactly k bulbs are OFF between them.
 * If no such day exists, return -1.
 *
 * CONSTRAINTS:
 * n == bulbs.length
 * 1 <= n <= 10^3
 * 1 <= bulbs[i] <= n
 * bulbs is a permutation of numbers from 1 to n
 * 0 <= k <= 10^3
 * 
 * ==========================================================================================
 * LATEST JAVA FEATURES USED:
 * - Records (`record TestCase(...)`): Clean immutable data carriers for the testing suite.
 * - Local Variable Type Inference (`var`): Reduces verbosity.
 * ==========================================================================================
 */
class KEmptySlots {

    // ==========================================================================================
    // SOLUTION 1: TreeSet (Intuitive & Dynamic approach)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * We can simulate the process day by day. We maintain a `TreeSet` to store the positions 
     * of the bulbs that are currently turned ON.
     * 
     * When we turn on a bulb at position `x` on the current day, we only need to check its 
     * IMMEDIATE neighbors in the TreeSet (the closest ON bulb to its left, and the closest 
     * ON bulb to its right). 
     * 
     * If the distance to either neighbor represents exactly `k` off bulbs 
     * (i.e., absolute difference is exactly k + 1), then the current day is our answer!
     * Since we evaluate day by day, the first valid match is guaranteed to be the EARLIEST day.
     *
     * VISUAL:
     * bulbs = [1, 3, 2], k = 1
     * 
     * Day 1: Bulb 1 turns on. 
     *        TreeSet: [1]
     *        No neighbors to check.
     * 
     * Day 2: Bulb 3 turns on.
     *        TreeSet: [1, 3]
     *        Lower neighbor of 3 is 1. 
     *        Gap = 3 - 1 - 1 = 1. This perfectly matches k = 1!
     *        Return Day 2.
     *
     * COMPLEXITY:
     * - Time: O(N log N) - Inserting into a TreeSet and finding higher/lower takes O(log N). We do this N times.
     * - Space: O(N) - To store the elements in the TreeSet.
     */
    public static int kEmptySlots_TreeSet(int[] bulbs, int k) {
        var activeBulbs = new TreeSet<Integer>();
        
        for (int i = 0; i < bulbs.length; i++) {
            int day = i + 1;
            int position = bulbs[i];
            
            activeBulbs.add(position);
            
            Integer lower = activeBulbs.lower(position);
            Integer higher = activeBulbs.higher(position);
            
            // Check if the left neighbor forms a valid gap of 'k' off bulbs
            if (lower != null && position - lower - 1 == k) {
                return day;
            }
            
            // Check if the right neighbor forms a valid gap of 'k' off bulbs
            if (higher != null && higher - position - 1 == k) {
                return day;
            }
        }
        
        return -1;
    }

    // ==========================================================================================
    // SOLUTION 2: Inverse Days Array + Sliding Window (Optimal O(N) approach)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * Instead of looking at positions day by day, we can invert the relationship:
     * Let `days[p]` be the day that the bulb at position `p` turns on.
     * 
     * A valid gap of `k` OFF bulbs means we need to find two boundaries:
     * `left` and `right = left + k + 1`
     * 
     * For this gap to be valid, EVERY bulb between `left` and `right` must turn on AFTER 
     * both `left` and `right` have turned on.
     * Mathematically: `days[i] > max(days[left], days[right])` for all `left < i < right`.
     * 
     * We can use a sliding window to check this in O(N) time. We test elements between 
     * `left` and `right`. If we find an element `i` that turns on TOO EARLY, then `left` 
     * can jump all the way to `i`, because no boundary before `i` will work if `i` is in the middle.
     *
     * VISUAL:
     * bulbs = [1, 3, 2], k = 1
     * days mapping: 
     * Position 1 turns on at Day 1. (days[1] = 1)
     * Position 2 turns on at Day 3. (days[2] = 3)
     * Position 3 turns on at Day 2. (days[3] = 2)
     * days array = [0, 1, 3, 2] (1-indexed)
     * 
     * Window: left = 1, right = 3. 
     * Max boundary day = max(days[1], days[3]) = max(1, 2) = 2.
     * Check middle elements: `i = 2`. days[2] = 3. 
     * Since 3 > 2, the middle bulb turns on AFTER both boundaries! Valid gap found!
     * 
     * Day formed = 2.
     *
     * COMPLEXITY:
     * - Time: O(N) - `i` and `left` pointers only move forward. We process each position at most twice.
     * - Space: O(N) - For the inverted `days` array.
     */
    public static int kEmptySlots_DaysArray(int[] bulbs, int k) {
        int n = bulbs.length;
        var days = new int[n + 1]; // 1-indexed positions
        
        // Populate the days array: days[position] = day it turns on
        for (int i = 0; i < n; i++) {
            days[bulbs[i]] = i + 1;
        }
        
        int earliestDay = Integer.MAX_VALUE;
        int left = 1;
        int right = k + 2; // Target right boundary for exactly k gaps
        
        int i = left + 1;
        
        while (right <= n) {
            // Check if current middle element i violates the condition
            // It violates if it turns on earlier than BOTH boundaries
            if (i == right) {
                // If we reached the right boundary without violations, we found a valid gap!
                earliestDay = Math.min(earliestDay, Math.max(days[left], days[right]));
                
                // Shift window forward
                left = right;
                right = left + k + 1;
                i = left + 1;
            } else if (days[i] < Math.max(days[left], days[right])) {
                // Violation! We can safely skip `left` directly to `i`
                left = i;
                right = left + k + 1;
                i = left + 1;
            } else {
                // Element is valid, keep moving
                i++;
            }
        }
        
        return (earliestDay == Integer.MAX_VALUE) ? -1 : earliestDay;
    }

    // ==========================================================================================
    // TESTING SUITE
    // ==========================================================================================
    
    public record TestCase(int[] bulbs, int k, int expected) {}

    public static void main(String[] args) {
        var testCases = new TestCase[] {
            new TestCase(new int[]{1, 3, 2}, 1, 2), 
            // Day 1: pos 1 on
            // Day 2: pos 3 on. [1, off, 3]. Gap = 1. Valid. -> Return 2
            
            new TestCase(new int[]{1, 2, 3}, 1, -1), 
            // Day 1: pos 1. Day 2: pos 2. Day 3: pos 3. Gap is always 0. -> Return -1
            
            new TestCase(new int[]{3, 1, 5, 4, 2}, 1, 3),
            // Day 1: pos 3
            // Day 2: pos 1. [1, off, 3]. Gap = 1. Valid! -> Return 3 (Wait, 3, 1 means day 2)
            // Let's trace carefully: 
            // Pos 3 is Day 1. Pos 1 is Day 2.
            // Pos 1 and Pos 3 are ON. Pos 2 is OFF. Gap is exactly 1.
            // Day formed is max(Day 1, Day 2) = Day 2.
            // Wait, looking closely: bulbs = [3, 1, 5, 4, 2]. 
            // Day 1: bulb 3 on.
            // Day 2: bulb 1 on. Gap between 1 and 3 is exactly 1 off bulb (pos 2).
            // So return 2.
            // (Note: Updated expected to 2 for accurate logic representation)
            
            new TestCase(new int[]{10, 1, 9, 3, 5, 7, 6, 4, 8, 2}, 8, -1),
            // Impossible k
            
            new TestCase(new int[]{1, 4, 2, 5, 3}, 2, 2)
            // Day 1: pos 1
            // Day 2: pos 4. Pos 1 and 4 ON. Pos 2, 3 OFF. Gap = 2. -> Return 2
        };

        // Fixing Test Case 3 expected based on manual tracing
        testCases[2] = new TestCase(new int[]{3, 1, 5, 4, 2}, 1, 2);

        System.out.println("Running K Empty Slots Tests...\n");

        for (int i = 0; i < testCases.length; i++) {
            var tc = testCases[i];
            System.out.printf("Test Case %d:\n", i + 1);
            System.out.printf("Bulbs: %s | k: %d | Expected: %d\n", Arrays.toString(tc.bulbs()), tc.k(), tc.expected());

            // Run TreeSet Solution
            int res1 = kEmptySlots_TreeSet(tc.bulbs(), tc.k());
            boolean pass1 = (res1 == tc.expected());
            System.out.printf("  [1. TreeSet       ] Result: %2d -> %s\n", res1, pass1 ? "PASS" : "FAIL");

            // Run Sliding Window Solution
            int res2 = kEmptySlots_DaysArray(tc.bulbs(), tc.k());
            boolean pass2 = (res2 == tc.expected());
            System.out.printf("  [2. Sliding Window] Result: %2d -> %s\n", res2, pass2 ? "PASS" : "FAIL");

            System.out.println("-".repeat(60));
        }
    }
}

/**
 * 🔥 K EMPTY SLOTS (ALL APPROACHES IN ONE FILE)
 *
 * ------------------------------------------------------------
 * 🧠 PROBLEM SUMMARY
 * ------------------------------------------------------------
 * You are given bulbs that turn ON one by one each day.
 *
 * bulbs[i] = x → On day (i+1), position x turns ON.
 *
 * You need to find the earliest day such that:
 *
 * 👉 There exist two ON bulbs with exactly k OFF bulbs between them.
 *
 * If no such day exists → return -1
 *
 *
 * ------------------------------------------------------------
 * 🚨 KEY INTERVIEW INSIGHT
 * ------------------------------------------------------------
 * Instead of thinking:
 * ❌ "Which bulbs are ON at day X?"
 *
 * Think:
 * ✅ "On which day does each bulb turn ON?"
 *
 * → Convert into a "days[]" array
 *
 *
 * ------------------------------------------------------------
 * 🔄 TRANSFORMATION
 * ------------------------------------------------------------
 * bulbs = [6,5,8,9,7,1,10,2,3,4]
 *
 * Day-wise:
 * Day 1 → 6 ON
 * Day 2 → 5 ON
 * ...
 *
 * Convert to:
 *
 * days[pos] = day when this bulb turns ON
 *
 * Position:  1  2  3  4  5  6  7  8  9  10
 * days[]  =  6  8  9 10  2  1  5  3  4   7
 *
 *
 * ------------------------------------------------------------
 * 🎯 GOAL AFTER TRANSFORMATION
 * ------------------------------------------------------------
 * Find two indices:
 *
 * left = i
 * right = i + k + 1
 *
 * Such that:
 *
 * ALL bulbs in between:
 * days[mid] > max(days[left], days[right])
 *
 *
 * WHY?
 * Because we want:
 * 👉 when both ends are ON, middle bulbs are still OFF
 *
 * That means:
 * middle bulbs turn ON later
 *
 *
 * ------------------------------------------------------------
 * 🚀 APPROACHES
 * ------------------------------------------------------------
 * 1. Brute Force → O(n^2)
 * 2. Sliding Window (Greedy Jump) → O(n)
 * 3. Monotonic Queue → O(n)
 *
 */
class KEmptySlotsAllSolutions {

    /* ============================================================
     * 🧱 UTILITY: BUILD DAYS ARRAY
     * ============================================================
     */
    private int[] buildDays(int[] bulbs) {
        int n = bulbs.length;
        int[] days = new int[n];

        for (int i = 0; i < n; i++) {
            days[bulbs[i] - 1] = i + 1;
        }

        return days;
    }


    /* ============================================================
     * 🧠 APPROACH 1: BRUTE FORCE
     * ============================================================
     *
     * IDEA:
     * Try every possible pair (left, right)
     *
     * For each:
     *  - check all mid elements
     *
     *
     * TIME:  O(n^2)
     * SPACE: O(n)
     */
    public int kEmptySlots_BruteForce(int[] bulbs, int k) {

        int n = bulbs.length;
        int[] days = buildDays(bulbs);

        int result = Integer.MAX_VALUE;

        for (int left = 0; left < n; left++) {

            int right = left + k + 1;
            if (right >= n) break;

            int boundaryMax = Math.max(days[left], days[right]);

            boolean valid = true;

            for (int mid = left + 1; mid < right; mid++) {

                // ❌ If any bulb turns ON earlier → invalid
                if (days[mid] < boundaryMax) {
                    valid = false;
                    break;
                }
            }

            if (valid) {
                result = Math.min(result, boundaryMax);
            }
        }

        return result == Integer.MAX_VALUE ? -1 : result;
    }


    /* ============================================================
     * 🚀 APPROACH 2: SLIDING WINDOW (GREEDY JUMP)
     * ============================================================
     *
     * 💡 INTUITION:
     *
     * Instead of checking all pairs:
     * - Fix window size = k + 2
     * - left = i, right = i + k + 1
     *
     *
     * 🔥 KEY OPTIMIZATION:
     *
     * If any mid breaks condition:
     * → Move left = mid (jump!)
     *
     * WHY?
     * Because that mid proves all earlier windows are invalid
     *
     *
     * TIME:  O(n)
     * SPACE: O(n)
     */
    public int kEmptySlots_SlidingWindow(int[] bulbs, int k) {

        int n = bulbs.length;
        int[] days = buildDays(bulbs);

        int left = 0;
        int right = k + 1;

        int result = Integer.MAX_VALUE;

        while (right < n) {

            boolean valid = true;

            for (int mid = left + 1; mid < right; mid++) {

                // ❌ If any mid bulb turns ON too early
                if (days[mid] < days[left] || days[mid] < days[right]) {

                    // 🚀 Jump optimization
                    left = mid;
                    right = left + k + 1;

                    valid = false;
                    break;
                }
            }

            if (valid) {

                int boundaryMax = Math.max(days[left], days[right]);
                result = Math.min(result, boundaryMax);

                // Move to next window
                left = right;
                right = left + k + 1;
            }
        }

        return result == Integer.MAX_VALUE ? -1 : result;
    }


    /* ============================================================
     * 🔥 APPROACH 3: MONOTONIC QUEUE
     * ============================================================
     *
     * 💡 KEY TRANSFORMATION:
     *
     * Instead of checking ALL mid:
     *
     * Check:
     * min(days[mid]) > max(days[left], days[right])
     *
     *
     * So we need:
     * 👉 Sliding window minimum → use Deque
     *
     *
     * 🎯 WHAT DEQUE STORES:
     * - Indices of days[]
     * - Maintains increasing order
     *
     *
     * TIME:  O(n)
     * SPACE: O(n)
     */
    public int kEmptySlots_MonotonicQueue(int[] bulbs, int k) {

        int n = bulbs.length;
        int[] days = buildDays(bulbs);

        Deque<Integer> dq = new ArrayDeque<>();

        int result = Integer.MAX_VALUE;

        for (int i = 0; i < n; i++) {

            // Remove out-of-window elements
            if (!dq.isEmpty() && dq.peekFirst() <= i - k - 1) {
                dq.pollFirst();
            }

            // Maintain monotonic increasing queue
            while (!dq.isEmpty() && days[dq.peekLast()] >= days[i]) {
                dq.pollLast();
            }

            dq.offerLast(i);

            // Start checking when window is ready
            if (i >= k + 1) {

                int left = i - k - 1;
                int right = i;

                int minMid = days[dq.peekFirst()];
                int boundaryMax = Math.max(days[left], days[right]);

                if (minMid > boundaryMax) {
                    result = Math.min(result, boundaryMax);
                }
            }
        }

        return result == Integer.MAX_VALUE ? -1 : result;
    }


    /* ============================================================
     * 🧪 DRY RUN EXAMPLE
     * ============================================================
     *
     * bulbs = [1,3,2], k = 1
     *
     * days = [1,3,2]
     *
     * Window:
     * [1, 3, 2]
     *  L  M  R
     *
     * max(L,R) = 2
     *
     * mid = 3 → > 2 → VALID
     *
     * Answer = 2
     *
     *
     * ------------------------------------------------------------
     *
     * Example 2:
     *
     * bulbs = [6,5,8,9,7,1,10,2,3,4]
     *
     * days = [6,8,9,10,2,1,5,3,4,7]
     *
     * Window:
     * [2,3,4,5]
     *
     * left = 2 → day 8
     * right = 5 → day 2
     *
     * mid = [9,10]
     *
     * min(mid) = 9
     *
     * boundary max = 8
     *
     * 9 > 8 → VALID
     *
     * Answer = 8
     *
     */


    /* ============================================================
     * 🧠 FINAL INTERVIEW SUMMARY
     * ============================================================
     *
     * STEP 1:
     * Convert bulbs → days[]
     *
     * STEP 2:
     * Fix window size = k + 2
     *
     * STEP 3:
     * Check:
     * min(mid) > max(boundaries)
     *
     *
     * PATTERN MATCH:
     *
     * "check all elements in range"
     * → convert to min/max
     * → use monotonic queue
     *
     *
     * ------------------------------------------------------------
     * 📊 COMPLEXITY
     * ------------------------------------------------------------
     *
     * Brute Force        → O(n^2)
     * Sliding Window     → O(n)
     * Monotonic Queue    → O(n)
     *
     *
     * ------------------------------------------------------------
     * 🏁 RECOMMENDATION
     * ------------------------------------------------------------
     *
     * Interview:
     * 👉 Start with brute force
     * 👉 Optimize to sliding window
     * 👉 Mention monotonic queue (bonus points)
     *
     */
}
