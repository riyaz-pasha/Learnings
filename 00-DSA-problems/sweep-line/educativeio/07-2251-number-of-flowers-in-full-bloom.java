/**
 * 2251. Number of Flowers in Full Bloom
 * 
 * ============================================================================
 * 1. PROBLEM STATEMENT
 * ============================================================================
 * You are given a 0-indexed 2D integer array flowers, where 
 * flowers[i] = [start_i, end_i] means the ith flower will be in full bloom 
 * from start_i to end_i (inclusive). 
 * 
 * You are also given a 0-indexed integer array people of size n, where 
 * people[i] is the time that the ith person will arrive to see the flowers.
 * 
 * Return an integer array answer of size n, where answer[i] is the number of 
 * flowers that are in full bloom when the ith person arrives.
 * 
 * Constraints:
 * - 1 <= flowers.length <= 5 * 10^4
 * - 1 <= start_i <= end_i <= 10^9
 * - 1 <= people.length <= 5 * 10^4
 * - 1 <= people[i] <= 10^9
 * 
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (To ask the interviewer)
 * ============================================================================
 * 1. Is the bloom time strictly inclusive? 
 *    (Yes, if a flower blooms [1, 5] and I arrive at 5, I see it. This dictates 
 *    how we handle boundary conditions).
 * 2. Are the arrival times of people guaranteed to be sorted?
 *    (No. If they were, we could just do a sweeping two-pointer pass. Since 
 *    they aren't, we must either sort them while remembering their original 
 *    indices, or use an approach that can answer random queries efficiently).
 * 3. Can the timestamps (start, end, arrival) be extremely large?
 *    (Yes, up to 10^9. This immediately rules out using a fixed-size array 
 *    of size 10^9 for a Line Sweep / Difference Array, as it would cause an OOM).
 * 4. Are we answering these queries offline or online? 
 *    (Offline means we have all the queries upfront and can sort them. Online 
 *    means they come one by one. The problem provides all upfront, but an 
 *    online-capable solution is heavily preferred in real systems).
 * 
 * ============================================================================
 * 3. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement & Constraint: 
 * We need to find the number of overlapping intervals at arbitrary points in time.
 * The binding constraints are the massive coordinate space (10^9) which prevents 
 * array allocation, and the large number of queries/flowers (50,000) which 
 * prevents brute force O(P * F).
 * 
 * --- Approach 1: True Brute Force ---
 * 1. Idea: For every single person, iterate through the entire array of flowers 
 *    and manually count how many are currently blooming.
 * 2. What I'd naturally try: A nested loop. Outer loop over `people`, inner loop 
 *    over `flowers`. If `start <= person <= end`, count++.
 * 3. Why it works: It perfectly evaluates the state of the garden at that exact second.
 * 4. Why it's too slow/costly: 50,000 people * 50,000 flowers = 2.5 billion iterations. 
 *    This will instantly Time Limit Exceed (TLE).
 * 5. What work is repeated: We repeatedly check the exact same flowers for different 
 *    people, entirely discarding the temporal relationships of the flowers.
 * 6. The pivot observation: We need a way to track the active number of flowers 
 *    without iterating over them. A Line Sweep could work if we map events.
 * 
 * Time Complexity: O(P * F) — because for each of the P people, we visit all F flowers.
 * Space Complexity: O(1) auxiliary — because we only allocate the result array.
 * 
 * --- Approach 2: Line Sweep with TreeMap / Offline Queries (Intermediate) ---
 * 1. Idea: A flower starting is a `+1`, ending is a `-1` (at `end + 1`). We can store 
 *    these events in a `TreeMap` to keep them sorted. We also sort the `people` 
 *    array (keeping track of original indices).
 * 2. What I'd naturally try: Dump all starts and ends into a TreeMap. Sweep 
 *    chronologically. When we hit a person's arrival time, we record the current 
 *    running sum. 
 * 3. Why it works: It completely ignores the massive empty spaces (up to 10^9) 
 *    between events and only processes actual state changes.
 * 4. Why it's costly: Using `TreeMap` and creating `Event` objects for 100,000 
 *    data points introduces massive autoboxing and memory overhead in Java.
 * 5. What work is repeated: We are keeping starts and ends bound together in a 
 *    timeline, which forces us to process them chronologically. 
 * 6. The pivot observation: Do we actually need to process them chronologically? 
 *    The number of flowers blooming right now is simply: 
 *    (Total flowers that EVER started blooming) minus (Total flowers that ALREADY died).
 * 
 * Time Complexity: O(F log F + P log P) — sorting events and sorting people.
 * Space Complexity: O(F + P) — storing the events and the mapped people arrays.
 * 
 * --- Approach 3: Binary Search on Independent Starts and Ends (Optimal) ---
 * 1. Idea: Completely decouple the `start` times from the `end` times. 
 *    Create an array of just `starts`, and an array of just `ends`. Sort both.
 * 2. What I'd naturally try: For a person arriving at time `t`, use Binary Search 
 *    on `starts` to find how many flowers started at or before `t`. Use Binary 
 *    Search on `ends` to find how many flowers ended strictly before `t`.
 * 3. Why it works: Because `Starts` and `Ends` are independent! If a flower 
 *    started, it adds 1 to the "active" count. If it ended, it subtracts 1. 
 *    `Active = Started(<= t) - Ended(< t)`.
 * 4. Why this is the ceiling: It answers queries purely "online" (no need to 
 *    sort the `people` array). It uses primitive arrays (zero overhead).
 * 
 * Time Complexity: O(F log F + P log F) — F log F to sort the two arrays. For each 
 *                  of the P people, we do two O(log F) binary searches.
 * Space Complexity: O(F) — because we create two auxiliary arrays of size F to 
 *                   store the extracted starts and ends.
 * 
 * Verdict for 45-minute interview:
 * Write Approach 3 (Binary Search). It is profoundly elegant, avoids the messy 
 * object tracking of Approach 2, and demonstrates a deep mathematical understanding 
 * of interval logic. 
 * 
 * ============================================================================
 * 4. EDGE CASES
 * ============================================================================
 * - Arrival exactly on `start`: Flower `[3, 7]`, Person at `3`.
 *   Starts `<= 3` -> catches the flower. Ends `< 3` -> misses it. Count = 1. (Correct).
 * - Arrival exactly on `end`: Flower `[3, 7]`, Person at `7`.
 *   Starts `<= 7` -> catches. Ends `< 7` -> misses (since 7 is not < 7). Count = 1. (Correct).
 * - Zero-duration bloom: `[5, 5]`. (Technically duration 1 since inclusive).
 *   Person at 5 catches it. Person at 6 misses it.
 * - Massive gaps: `[1, 2]` and `[10^9, 10^9]`. Binary search instantly skips the 
 *   billion-second gap.
 * 
 * ============================================================================
 * 5. KEY INSIGHT, DIAGRAM & DRY RUN
 * ============================================================================
 * > Key Insight: 
 * "Active = (Born) - (Died)."
 * You do not need to know WHICH flower died. You only need to know HOW MANY 
 * died. This allows you to completely detach the start coordinates from the end 
 * coordinates, sorting them as two entirely independent lists of integers.
 * 
 * ASCII Diagram: 
 * Flowers: [1, 6], [3, 7]
 * 
 * Sorted Starts: [1, 3]
 * Sorted Ends:   [6, 7]
 * 
 * Person arrives at T = 4:
 * 1. How many started at or before 4? 
 *    [1, 3] -> Both! (Count = 2)
 * 2. How many died strictly before 4?
 *    [6, 7] -> Neither! (Count = 0)
 * Result = 2 - 0 = 2.
 * 
 * Dry Run (Optimal Approach):
 * flowers = [[1,6],[3,7],[9,12],[4,13]], people = [2,3,7]
 * 
 * 1. Extract and Sort:
 *    starts = [1, 3, 4, 9]
 *    ends   = [6, 7, 12, 13]
 * 
 * 2. Query Person = 2:
 *    upperBound(starts, 2) -> elements <= 2 -> [1] -> count = 1
 *    upperBound(ends, 2 - 1 = 1) -> elements <= 1 -> none! -> count = 0
 *    Active = 1 - 0 = 1.
 * 
 * 3. Query Person = 7:
 *    upperBound(starts, 7) -> elements <= 7 -> [1, 3, 4] -> count = 3
 *    upperBound(ends, 7 - 1 = 6) -> elements <= 6 -> [6] -> count = 1
 *    Active = 3 - 1 = 2.
 * 
 * ============================================================================
 * 6. PITFALLS & PATTERN RECOGNITION
 * ============================================================================
 * Pitfalls:
 * - Using the standard `Arrays.binarySearch()`. In Java, if it doesn't find the 
 *   element, it returns `-(insertion_point) - 1`. If there are duplicates (e.g., 
 *   two flowers start at 3), it doesn't guarantee WHICH index it returns. 
 *   You MUST write a custom `upperBound` binary search that specifically returns 
 *   the count of elements less than or equal to a target.
 * - Querying `ends <= t`. Because blooms are inclusive, a flower ending at `t` 
 *   is still blooming at `t`. You must query `ends < t`, which for integers is 
 *   mathematically identical to `ends <= t - 1`.
 * 
 * Pattern Recognition: 
 * "Counting overlaps at specific points -> Decoupled Binary Search."
 * This differs from Meeting Rooms (where you want the MAX overlaps). Here you 
 * just want the overlaps AT specific queried points. Decoupling is king.
 * 
 * ============================================================================
 * 7. INTERVIEW SCRIPT
 * ============================================================================
 * "To find how many flowers a person sees, we could use a Sweep Line, but sorting 
 * all the people and events introduces a lot of overhead. 
 * Instead, there is a powerful mathematical observation: the number of flowers 
 * currently blooming is simply the total number of flowers that have started blooming 
 * up to this point, minus the total number of flowers that have already finished 
 * blooming. 
 * Since we only care about counts, not identities, I can separate the start times 
 * and end times into two distinct arrays and sort them. For each person arriving 
 * at time `t`, I'll binary search the starts array for values `<= t`, and binary 
 * search the ends array for values `< t`. Subtracting the two gives the exact 
 * active count in O(log F) time per person. This yields O(F log F + P log F) time 
 * and O(F) space, using completely flat, memory-efficient primitive arrays."
 * 
 * ============================================================================
 * 8. FOLLOW-UPS
 * ============================================================================
 * Q: What if people arrive dynamically (an online system where we don't have the 
 *    `people` array upfront)?
 * A: Our binary search approach is already perfectly suited for this! We sort the 
 *    flowers once (O(F log F)), and then any future dynamic query takes only 
 *    O(log F) time. An offline sweep-line approach would fail here.
 * 
 * Q: What if we needed to return the actual IDs of the flowers they see?
 * A: Binary search wouldn't work because it discards identities. We would need 
 *    a Segment Tree or an Interval Tree to physically store and retrieve the 
 *    overlapping intervals, which would take O(F log F) to build and 
 *    O(log F + K) to query, where K is the number of active flowers.
 */

import java.util.Arrays;

public class NumberOfFlowersInFullBloom {

    /**
     * APPROACH 1: True Brute Force
     * Time Complexity: O(P * F)
     * Space Complexity: O(1) auxiliary
     */
    public int[] fullBloomFlowersBruteForce(int[][] flowers, int[] people) {
        int[] answer = new int[people.length];
        
        for (int i = 0; i < people.length; i++) {
            int arrivalTime = people[i];
            int count = 0;
            
            // Manually check every flower
            for (int[] flower : flowers) {
                if (flower[0] <= arrivalTime && arrivalTime <= flower[1]) {
                    count++;
                }
            }
            answer[i] = count;
        }
        return answer;
    }

    /**
     * APPROACH 3: Binary Search on Independent Starts and Ends (Optimal)
     * Time Complexity: O(F log F + P log F)
     * Space Complexity: O(F)
     */
    public int[] fullBloomFlowers(int[][] flowers, int[] people) {
        int f = flowers.length;
        int[] starts = new int[f];
        int[] ends = new int[f];
        
        // 1. Decouple starts and ends
        for (int i = 0; i < f; i++) {
            starts[i] = flowers[i][0];
            ends[i] = flowers[i][1];
        }
        
        // 2. Sort both arrays independently
        Arrays.sort(starts);
        Arrays.sort(ends);
        
        int[] answer = new int[people.length];
        
        // 3. Process each query independently
        for (int i = 0; i < people.length; i++) {
            int t = people[i];
            
            // How many started at or before time t?
            int started = upperBound(starts, t);
            
            // How many ended STRICTLY before time t?
            // Mathematically equivalent to ending at or before t - 1.
            int ended = upperBound(ends, t - 1);
            
            // Active = Born - Died
            answer[i] = started - ended;
        }
        
        return answer;
    }
    
    /**
     * Helper: Custom Binary Search
     * Returns the number of elements in `arr` that are less than or equal to `target`.
     * Because arrays are 0-indexed, the index of the first element strictly greater 
     * than `target` exactly equals the COUNT of elements <= `target`.
     */
    private int upperBound(int[] arr, int target) {
        int left = 0;
        int right = arr.length;
        
        while (left < right) {
            int mid = left + (right - left) / 2;
            
            // If mid value is <= target, we must look further right 
            // to find the absolute upper bound.
            if (arr[mid] <= target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        
        // 'left' now represents the index of the first element > target,
        // which exactly corresponds to the number of elements <= target.
        return left;
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================
    public static void main(String[] args) {
        NumberOfFlowersInFullBloom solution = new NumberOfFlowersInFullBloom();
        
        // Test Case 1: Standard case with overlaps
        int[][] flowers1 = {{1, 6}, {3, 7}, {9, 12}, {4, 13}};
        int[] people1 = {2, 3, 7, 11};
        System.out.println("Test 1 (Expected [1, 2, 2, 2]): " 
            + Arrays.toString(solution.fullBloomFlowers(flowers1, people1)));
            
        // Test Case 2: Exact boundary touch & Zero-duration
        int[][] flowers2 = {{1, 10}, {3, 3}};
        int[] people2 = {3, 3, 2};
        System.out.println("Test 2 (Expected [2, 2, 1]): " 
            + Arrays.toString(solution.fullBloomFlowers(flowers2, people2)));
            
        // Test Case 3: Empty periods and huge timestamps
        int[][] flowers3 = {{1, 5}, {1000000000, 1000000000}};
        int[] people3 = {6, 1000000000};
        // At t=6, [1,5] is dead, the next hasn't started. Ans = 0.
        // At t=1000000000, first is dead, second is active. Ans = 1.
        System.out.println("Test 3 (Expected [0, 1]): " 
            + Arrays.toString(solution.fullBloomFlowers(flowers3, people3)));
            
        // Cross-checking the two approaches
        System.out.println("\nCross-checking approaches on Test 1:");
        System.out.println("Brute Force:   " + Arrays.toString(solution.fullBloomFlowersBruteForce(flowers1, people1)));
        System.out.println("Binary Search: " + Arrays.toString(solution.fullBloomFlowers(flowers1, people1)));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern: Decoupled Intervals + Binary Search.
 * 
 * Key observation: To answer point-in-time overlap queries, you don't need to 
 * maintain the interval as a single connected object. The overlaps at time T 
 * equal the total intervals started <= T, minus the total intervals ended < T.
 * 
 * Memorize vs Understand: 
 * UNDERSTAND the Custom Binary Search (upperBound). Don't rely on 
 * Arrays.binarySearch for counting problems because of how it handles duplicates 
 * and missing values.
 * 
 * Most common trap: Forgetting to subtract 1 from the target when searching the 
 * ends array. If a flower blooms [1, 5], it is still alive AT 5. It only "dies" 
 * for queries strictly greater than 5.
 * 
 * One-line mental trigger: "Point queries on intervals? Sort starts, sort ends, subtract upper bounds."
 */











































import java.util.*;

class Solution {

    public int[] fullBloomFlowers(int[][] flowers, int[] people) {

        /*
         * STEP 1: Build an event map.
         *
         * At a flower's start time, the active count increases by 1.
         * At end + 1, it decreases by 1.
         *
         * Using end + 1 handles inclusive intervals:
         * a flower ending at t remains active at t itself.
         *
         * For example, [3, 5]:
         *
         *     time 3: +1
         *     time 6: -1
         *
         * So it is counted at times 3, 4 and 5, but not at 6.
         */
        TreeMap<Integer, Integer> map = new TreeMap<>();

        for (int[] flower : flowers) {
            map.merge(flower[0], 1, Integer::sum);
            map.merge(flower[1] + 1, -1, Integer::sum);
        }

        /*
         * STEP 2: Convert deltas into cumulative active counts.
         *
         * A map value initially means "how much does the count change
         * at this time?" It does NOT mean "how many flowers are blooming."
         *
         * We must accumulate the deltas in chronological order.
         *
         * Example:
         *
         *     time:  1   3   4   7
         *     delta: +1  +1  +1  -1
         *     count:  1   2   3   2
         *
         * TreeMap iterates keys in ascending order.
         */
        int active = 0;

        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            active += entry.getValue();

            // Updating an existing entry's value does not structurally
            // modify the map, so this is safe during iteration.
            entry.setValue(active);
        }

        /*
         * STEP 3: Query each person's arrival time.
         *
         * floorEntry(t) returns the greatest event time <= t.
         *
         * Since we replaced deltas with cumulative counts, the value
         * at that event time is the number of flowers blooming at t.
         *
         * If no event exists at or before t, the count is zero.
         */
        int[] answer = new int[people.length];

        for (int i = 0; i < people.length; i++) {
            Map.Entry<Integer, Integer> entry =
                    map.floorEntry(people[i]);

            answer[i] = (entry == null) ? 0 : entry.getValue();
        }

        /*
         * TIME:
         *     Build map: O(F log F)
         *     Prefix sum: O(F)
         *     Queries: O(P log F)
         *
         *     Total: O(F log F + P log F).
         *
         * SPACE:
         *     Event map: O(F)
         *     Answer: O(P)
         *
         *     Total: O(F + P).
         */
        return answer;
    }
}











































import java.util.Arrays;

class Solution {

    public int[] fullBloomFlowers(int[][] flowers, int[] people) {

        /*
         * STEP 1: Extract flower start times and end times.
         *
         * Each flower is an inclusive interval [start, end].
         * A flower is blooming at time t if:
         *
         *     start <= t && t <= end
         *
         * We want to count the intervals satisfying this condition
         * for every person's arrival time.
         *
         * Instead of checking every interval for every person,
         * we will count qualifying start times and end times separately.
         */

        int flowerCount = flowers.length;
        int[] starts = new int[flowerCount];
        int[] ends = new int[flowerCount];

        for (int i = 0; i < flowerCount; i++) {
            starts[i] = flowers[i][0];
            ends[i] = flowers[i][1];
        }

        /*
         * STEP 2: Sort both arrays.
         *
         * WHY?
         *
         * Once an array is sorted, binary search can find how many
         * elements satisfy a boundary condition in O(log F) time.
         *
         * For example:
         *
         *     starts = [1, 3, 4, 9]
         *
         * At time t = 7, the first three starts are <= 7.
         *
         * Sorting time:
         *     O(F log F) for starts
         *     O(F log F) for ends
         *
         * Combined: O(F log F).
         */

        Arrays.sort(starts);
        Arrays.sort(ends);

        /*
         * STEP 3: Answer each person's query independently.
         *
         * We use two binary searches:
         *
         * A. upperBound(starts, t):
         *    Number of start times <= t.
         *
         * B. lowerBound(ends, t):
         *    Number of end times < t.
         *
         * Therefore:
         *
         *     blooming = started - finished
         *
         * The strict inequality for finished is crucial because
         * the problem states that intervals are inclusive.
         */

        int[] answer = new int[people.length];

        for (int i = 0; i < people.length; i++) {
            int time = people[i];

            int started = upperBound(starts, time);
            int finished = lowerBound(ends, time);

            answer[i] = started - finished;
        }

        /*
         * COMPLEXITY ANALYSIS
         *
         * Let F = number of flowers and P = number of people.
         *
         * Time:
         *     Extract arrays: O(F)
         *     Sort arrays: O(F log F)
         *     Two binary searches per person: O(P log F)
         *
         * Total: O(F log F + P log F).
         *
         * Space:
         *     starts: O(F)
         *     ends: O(F)
         *     answer: O(P)
         *
         * Total: O(F + P), including the output array.
         *
         * Since F and P can each be up to 50,000, this is efficient.
         */

        return answer;
    }

    /*
     * UPPER BOUND:
     *
     * Returns the first index whose value is STRICTLY GREATER
     * than target.
     *
     * Therefore, the returned index equals the number of elements
     * that are <= target.
     *
     * Example:
     *
     *     arr    = [1, 3, 3, 4, 9]
     *     target = 3
     *
     *     qualifying elements = [1, 3, 3]
     *     result = 3
     *
     * We use this for start times because a flower starting exactly
     * at the person's arrival time must be counted.
     *
     * Time: O(log F)
     * Space: O(1)
     */
    private int upperBound(int[] arr, int target) {
        int left = 0;
        int right = arr.length;

        // Search within the half-open interval [left, right).
        while (left < right) {
            int mid = left + (right - left) / 2;

            if (arr[mid] <= target) {
                // This start time qualifies.
                // Search to the right for more qualifying elements.
                left = mid + 1;
            } else {
                // This start time is too large.
                right = mid;
            }
        }

        return left;
    }

    /*
     * LOWER BOUND:
     *
     * Returns the first index whose value is GREATER THAN OR EQUAL
     * to target.
     *
     * Therefore, the returned index equals the number of elements
     * strictly smaller than target.
     *
     * Example:
     *
     *     arr    = [3, 6, 7, 7, 13]
     *     target = 7
     *
     *     qualifying elements = [3, 6]
     *     result = 2
     *
     * We use this for end times because a flower ending exactly
     * at the person's arrival time is STILL blooming.
     *
     * Time: O(log F)
     * Space: O(1)
     */
    private int lowerBound(int[] arr, int target) {
        int left = 0;
        int right = arr.length;

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (arr[mid] < target) {
                // This end time is strictly earlier than arrival.
                left = mid + 1;
            } else {
                // This end time is >= arrival.
                // Search left for the first such position.
                right = mid;
            }
        }

        return left;
    }
}
