/**
 * STANDALONE STUDY NOTE: PAIRS OF SONGS WITH TOTAL DURATIONS DIVISIBLE BY 60
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Can song durations be zero or negative?" (Constraints state 1 <= time[i] <= 500, so durations are strictly positive).
 * - "Can the total number of valid pairs exceed the maximum capacity of a 32-bit integer?" (With max N = 1000, max pairs = 1000 * 999 / 2 = 499,500, which easily fits in a standard `int`).
 * - "Are we looking for pairs of indices (i < j) rather than pairs of song values?" (Yes, prompt specifies i < j, meaning distinct positions).
 * - "Can multiple songs have the exact same duration?" (Yes, and identical song durations can pair with each other if their sum is a multiple of 60).
 * - "Is the input array modifiable?" (We don't need to modify it; a single linear pass is sufficient).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to count pairs (i, j) where i < j and `(time[i] + time[j]) % 60 == 0`. The binding constraint 
 * on naive solutions is checking every possible pair, which scales quadratically and falls over at scale.
 * 
 * --- APPROACH 1: BRUTE FORCE (Nested Loops) ---
 * 1. What I'd naturally try: Use two nested loops to check every combination of `i` and `j` where `i < j`, 
 *    summing `time[i] + time[j]` and checking if it's divisible by 60 using the modulo operator.
 * 2. Why it works: It exhaustively examines every valid index pair.
 * 3. Why it's too slow/costly: For N = 1000, it performs around 500,000 operations. While small N runs 
 *    quickly, for N = 10^5, it scales to 5 billion operations (O(N^2) time).
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N^2) — because we check every unique pair combination via nested loops.
 *    - Space Complexity: O(1) — because we only use a counter variable.
 * 
 * --- APPROACH 2: INTERMEDIATE (HashMap Remainder Frequency) ---
 * 1. The Observation: We don't need to check raw sums. Modular arithmetic tells us that 
 *    `(a + b) % 60 == 0` is equivalent to saying `(a % 60 + b % 60) % 60 == 0`. 
 *    This means for any song with remainder `r`, we need a complement song with remainder `(60 - r) % 60`.
 * 2. How it leads to the next step: We can iterate through the array and store song remainders in a `HashMap`. 
 *    As we encounter each song, we look up its complement in the map, add its count to our total, and then 
 *    register the current song's remainder.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we visit each element once and perform O(1) average hash map operations.
 *    - Space Complexity: O(U) where U is unique remainders (max 60). However, HashMap incurs heavy Java object 
 *      boxing and node allocation overhead.
 * 
 * --- APPROACH 3: OPTIMAL (Array Remainder Frequency Map) ---
 * 1. The Observation: Since remainders modulo 60 are strictly bounded between 0 and 59, we don't need a HashMap. 
 *    We can use a fixed-size `int[60]` array where the index represents the remainder.
 * 2. How it leads to the next step: As we process each song `t`, we compute its remainder `t % 60`. Its required 
 *    complement remainder is `(60 - (t % 60)) % 60`. We add `remainderCounts[complement]` to our pair count, 
 *    then increment `remainderCounts[t % 60]`.
 * 3. Which one I'd write in an interview: Approach 3 (Array Remainder Map). It's blindingly fast, uses zero 
 *    heap allocations, eliminates autoboxing, and showcases mathematical number-theory insight.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — because we make a single linear pass over the `time` array doing constant-time 
 *      primitive array lookups.
 *    - Space Complexity: O(1) — because we allocate an array of exactly 60 integers (240 bytes), which remains 
 *    completely constant regardless of how large `time.length` grows.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Remainder 0 edge case: If a song has duration 60 (remainder 0), its complement remainder is `(60 - 0) % 60 = 0`. 
 *   The formula naturally looks up index 0, correctly pairing it with previous songs of remainder 0 (like 60, 120, etc.).
 * - Remainder 30 edge case: If a song has duration 30 (remainder 30), its complement is `(60 - 30) % 60 = 30`. 
 *   It pairs with other 30s.
 * - Minimum array length: `time = [30, 30]` -> Returns 1.
 * - No valid pairs: `time = [10, 20, 30]` -> Returns 0.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Modular Complement Matching:
 * 
 * Target Condition: (A + B) % 60 == 0
 * 
 * For any song duration `t`:
 * Remainder `r = t % 60`
 * Needed Complement `comp = (60 - r) % 60`
 * 
 * Example: time = [60, 60, 60]
 * i=0: t=60 -> r=0, comp=0. Map[0] is 0. Total = 0. Map[0] becomes 1.
 * i=1: t=60 -> r=0, comp=0. Map[0] is 1. Total += 1 (Total = 1). Map[0] becomes 2.
 * i=2: t=60 -> r=0, comp=0. Map[0] is 2. Total += 2 (Total = 3). Map[0] becomes 3.
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: time = [30, 20, 150, 100, 40]
 * 
 * Init: remainders = [0, 0, ..., 0] (size 60), count = 0
 * 
 * i=0, t=30:
 *   r = 30 % 60 = 30
 *   comp = (60 - 30) % 60 = 30
 *   count += remainders[30] (0) -> 0
 *   remainders[30]++ -> remainders[30] = 1
 * 
 * i=1, t=20:
 *   r = 20 % 60 = 20
 *   comp = (60 - 20) % 60 = 40
 *   count += remainders[40] (0) -> 0
 *   remainders[20]++ -> remainders[20] = 1
 * 
 * i=2, t=150:
 *   r = 150 % 60 = 30 (since 150 = 2*60 + 30)
 *   comp = (60 - 30) % 60 = 30
 *   count += remainders[30] (1) -> 1 (Pairs with the first song duration 30!)
 *   remainders[30]++ -> remainders[30] = 2
 * 
 * i=3, t=100:
 *   r = 100 % 60 = 40
 *   comp = (60 - 40) % 60 = 20
 *   count += remainders[20] (1) -> 2 (Pairs with the song duration 20!)
 *   remainders[40]++ -> remainders[40] = 1
 * 
 * i=4, t=40:
 *   r = 40 % 60 = 40
 *   comp = (60 - 40) % 60 = 20
 *   count += remainders[20] (1) -> 3 (Pairs with song duration 20!)
 *   remainders[40]++ -> remainders[40] = 2
 * 
 * Final Result: 3 pairs.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "Instead of checking every pair with nested loops in O(N^2) time, we can use modular arithmetic. 
 * Two song durations are divisible by 60 if the sum of their remainders modulo 60 is either 0 or 60. 
 * As we iterate through the array, for each song with remainder `r`, we need to look up how many previous 
 * songs we've seen with the complement remainder `(60 - r) % 60`. We can maintain a fixed-size integer array 
 * of size 60 to track remainder frequencies on the fly. This allows us to find all valid pairs in a single 
 * linear pass, giving us O(N) time complexity and O(1) space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if the divisor wasn't 60, but a variable K passed as a parameter?
 * A: The fixed-size `int[60]` array would change to an `int[K]` array (or a `HashMap` if K is massive). 
 *    The remainder formula `(K - (t % K)) % K` remains completely identical.
 * 
 * Q: What if we needed to return the actual pairs of song indices instead of just the count?
 * A: We would change our remainder storage from a simple frequency count to a `List<Integer>` mapping 
 *    each remainder to the list of indices where that remainder occurred. When a match is found, we can 
 *    generate all index pairs.
 */

public class PairsOfSongsDivisibleBy60 {

    /**
     * APPROACH 3: OPTIMAL (Array Remainder Frequency Map)
     * Time: O(N), Space: O(1)
     */
    public int numPairsDivisibleBy60(int[] time) {
        // [Why needed]: Tracks frequency of remainders from 0 to 59.
        // Size 60 covers all possible remainder buckets modulo 60.
        int[] remainderCounts = new int[60];
        int pairCount = 0;

        for (int t : time) {
            // Compute the remainder of the current song's duration
            int remainder = t % 60;
            
            // Calculate the required complement remainder to sum up to a multiple of 60.
            // Example: if remainder is 20, complement is (60 - 20) % 60 = 40.
            // If remainder is 0, complement is (60 - 0) % 60 = 0.
            int complement = (60 - remainder) % 60;

            // [What it does]: If we have previously seen songs with the complement remainder, 
            // each of them can form a valid pair with the current song.
            pairCount += remainderCounts[complement];

            // [Why needed]: Register this song's remainder so future songs can pair with it.
            remainderCounts[remainder]++;
        }

        return pairCount;
    }

    /**
     * APPROACH 2: INTERMEDIATE (HashMap Remainder Frequency)
     * Time: O(N), Space: O(U) where U <= 60 (High object overhead)
     */
    public int numPairsDivisibleBy60HashMap(int[] time) {
        java.util.Map<Integer, Integer> map = new java.util.HashMap<>();
        int pairCount = 0;

        for (int t : time) {
            int remainder = t % 60;
            int complement = (60 - remainder) % 60;

            pairCount += map.getOrDefault(complement, 0);
            map.put(remainder, map.getOrDefault(remainder, 0) + 1);
        }

        return pairCount;
    }

    /**
     * APPROACH 1: BRUTE FORCE (Nested Loops)
     * Time: O(N^2), Space: O(1)
     */
    public int numPairsDivisibleBy60BruteForce(int[] time) {
        int pairCount = 0;
        int n = time.length;

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if ((time[i] + time[j]) % 60 == 0) {
                    pairCount++;
                }
            }
        }

        return pairCount;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Modular arithmetic complement matching (Two-Sum variant).
     * 2. Key observation: `(a + b) % 60 == 0` means we can track remainders in a fixed-size `int[60]` array 
     *    instead of checking nested pairs.
     * 3. Memorize vs Understand: Understand the modular complement formula: `(60 - (t % 60)) % 60`.
     * 4. Most common trap: Forgetting the outer modulo for remainder 0, which would cause an out-of-bounds 
     *    or incorrect lookup when complement evaluates to 60 instead of 0.
     * 5. Mental trigger: "Sum divisible by K" -> Remainder frequency array of size K + complement formula.
     */
    public static void main(String[] args) {
        PairsOfSongsDivisibleBy60 solution = new PairsOfSongsDivisibleBy60();

        // Test Case 1: Standard Example
        int[] time1 = {30, 20, 150, 100, 40};
        System.out.println("Test 1 (Standard): " + solution.numPairsDivisibleBy60(time1)); // Expected: 3

        // Test Case 2: Identical durations (All divisible by 60)
        int[] time2 = {60, 60, 60};
        System.out.println("Test 2 (All 60s): " + solution.numPairsDivisibleBy60(time2)); // Expected: 3

        // Test Case 3: No valid pairs
        int[] time3 = {10, 20, 30};
        // 30 pairs with itself? Wait, i < j required. 10+20=30, 10+30=40, 20+30=50. None divisible by 60.
        System.out.println("Test 3 (No pairs): " + solution.numPairsDivisibleBy60(time3)); // Expected: 0

        // Test Case 4: Remainder 30 pairs
        int[] time4 = {30, 30, 30};
        // i=0, j=1 (30+30=60). i=0, j=2 (60). i=1, j=2 (60). Total = 3 pairs.
        System.out.println("Test 4 (All 30s): " + solution.numPairsDivisibleBy60(time4)); // Expected: 3
    }
}



class Solution {
    public int numPairsDivisibleBy60(int[] time) {
        // Only 60 possible remainders: 0 through 59.
        int[] remainderCount = new int[60];
        int count = 0;

        for (int duration : time) {
            int remainder = duration % 60;

            // Find the remainder needed to make the sum divisible by 60.
            int complement = (60 - remainder) % 60;

            // Every previously seen complementary remainder forms
            // a valid pair with the current song.
            count += remainderCount[complement];

            // Record this song only after counting pairs, so that
            // we never pair a song with itself.
            remainderCount[remainder]++;
        }

        return count;
    }
}

/*
Complexity
- Time: \(O(n)\) — processes each song once.
- Space: \(O(1)\) — the frequency array always has exactly 60 entries.
*/

