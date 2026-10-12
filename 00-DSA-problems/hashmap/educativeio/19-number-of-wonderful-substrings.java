/**
 * ============================================================================
 * L E E T C O D E   1 9 1 5 :   N U M B E R   O F   W O N D E R F U L   S U B S T R I N G S
 * ============================================================================
 *
 * ## 1. Clarifying questions
 * 1. "Are we strictly limited to the first 10 letters ('a' to 'j')?"
 *    (Crucial: Yes. This small alphabet size bounds our bitmask state space to 2^10 = 1024 combinations. If it were all 26 letters, our array would need 67 million indices, forcing a switch to a HashMap or massive memory allocation).
 * 2. "Can the total count of wonderful substrings exceed the 32-bit integer limit?"
 *    (Yes! Even though the prompt constraints say N <= 10^3 (where max subarrays are ~500,000), standard platforms often scale N up to 10^5 in hidden test cases. N=10^5 yields up to 5 billion substrings, overflowing an `int`. We must return a `long`).
 * 3. "Does 'at most one odd' mean 0 odds is also acceptable?"
 *    (Yes. A string where every character appears an even number of times is 'wonderful').
 * 4. "Do we care about the exact frequency of characters, or just their parity (even/odd)?"
 *    (Just the parity. This completely alters the optimal data structure from an int[] array to a single bitmask integer).
 *
 *
 * ## 2. The reasoning journey
 * > Core Constraint: We need to count substrings where characters have specific frequency parities (even/odd). 
 * > The bottleneck is checking the frequency of every character for every possible contiguous subarray.
 *
 * ### Approach 1: Frequency Counting (Brute Force)
 * 1. What I'd naturally try: For every starting index `i` and ending index `j`, I would count the 
 *    frequencies of all characters in `word[i..j]`. Then I'd check if more than 1 character has an odd count.
 * 2. Why it works: It literally simulates the definition of a wonderful string.
 * 3. Why it's too costly:
 *    - Time Complexity: O(N^2) — because for each starting position `i`, we expand `j` and dynamically 
 *      update the frequencies, checking the 10 characters each time.
 *    - Space Complexity: O(1) auxiliary — an array of size 10 to store counts.
 * 4. What work is being repeated: If we know the character counts from index 0 to 5, we shouldn't have to 
 *    recalculate them. We can use prefix logic!
 * 5. What property removes the bottleneck: "Prefix Differences." The count of 'a' in substring `i..j` is 
 *    `prefix[j]['a'] - prefix[i-1]['a']`. But wait—we don't even care about the actual count. We only care 
 *    if the count is even or odd!
 *
 * ### Approach 2: Prefix Parities (Intermediate)
 * 1. What I'd naturally try: Instead of counting, we track `even` or `odd` for each of the 10 letters. 
 *    If 'a' is odd at index `j` and odd at index `i-1`, then in the substring `i..j`, 'a' appears an 
 *    EVEN number of times (odd - odd = even). 
 * 2. Why it works: It reduces math to basic parity arithmetic.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N^2) — We still have to check all pairs of `i` and `j` to compare their parities.
 * 4. What work is being repeated: We are searching backwards for past prefixes that match our current prefix.
 * 5. What property removes the bottleneck: Bitmasks! We can represent the even/odd state of 10 letters as a 
 *    single 10-bit integer. 'a' is bit 0, 'j' is bit 9. If a bit is 1, it's odd. If 0, it's even.
 *
 * ### Approach 3: Bitmask + Frequency Array (The Algorithmic Optimum)
 * 1. What I'd naturally try: As I iterate, I flip the bit for the current character using XOR (`^`). 
 *    - For all letters to be EVEN, my current bitmask must EXACTLY match a previous bitmask.
 *    - For EXACTLY ONE letter to be ODD, my current bitmask must differ from a previous bitmask by EXACTLY ONE BIT.
 *    I'll use an array of size 1024 to remember how many times I've seen each bitmask.
 * 2. Why it works: It transforms an O(N^2) range search into 11 instant O(1) array lookups (1 for exact match, 
 *    10 for single-bit differences).
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N * 10) -> O(N) — because for each of the N characters, we do 11 constant-time array lookups.
 *    - Space Complexity: O(1024) -> O(1) — because our state array size is strictly bound to 2^10, completely 
 *      independent of the string length N.
 *
 * > **Interview Strategy:** Start by explicitly stating: "Subtraction of odd and even is identical to XOR logic." 
 * > Prove that you recognize the alphabet is small enough (10) to map to a 10-bit integer. Confidently write 
 * > Approach 3, and deliberately use a `long` for the result to show foresight against integer overflow.
 *
 *
 * ## 3. Edge cases
 * - All identical characters: `"aaaa"`. Will flip between mask 0 and 1 repeatedly. Array properly catches and pairs them.
 * - Minimum length: `"a"`. Result is 1.
 * - Subarray starting exactly at index 0: CRITICAL. If the prefix bitmask at index 2 is `0000000000`, it means 
 *   the string from index 0 to 2 is wonderful. To count this, the mask `0` must ALREADY exist in our history 
 *   before the loop starts. We MUST initialize `counts[0] = 1`.
 *
 *
 * ## 4. Key Insight & Dry Run
 * 
 * **Key Insight: "XOR is the Parity Operator"**
 * If `Mask_J` is the parity bitmask at index j, and `Mask_I` is the parity bitmask at index i-1:
 * The substring parity is `Mask_J ^ Mask_I`.
 * We want the substring parity to be either `0` (all even) OR a power of 2 (exactly one bit is 1).
 * Thus, we want to find past `Mask_I` states where:
 * 1. `Mask_I == Mask_J`
 * 2. `Mask_I == Mask_J ^ (1 << k)` for k in 0 to 9.
 *
 * **Dry Run of Approach 3:**
 * `word = "aba"`
 * Init: `counts[1024]`, `counts[0] = 1`. `mask = 0`, `result = 0`.
 * 
 * | idx | char | bit | mask (XOR) | Exact Match (`counts[mask]`) | 1-Bit Diff (`counts[mask ^ (1<<k)]`) | Result Add | Map Update |
 * |-----|------|-----|------------|------------------------------|--------------------------------------|------------|------------|
 * |  0  | 'a'  |  0  | 0^1 = 1    | counts[1] = 0                | k=0 -> counts[1^1=0] = 1             | 0 + 1 = 1  | counts[1]=1|
 * |  1  | 'b'  |  1  | 1^2 = 3    | counts[3] = 0                | k=0 -> counts[3^1=2] = 0             |            |            |
 * |     |      |     |            |                              | k=1 -> counts[3^2=1] = 1             | 1 + 1 = 2  | counts[3]=1|
 * |  2  | 'a'  |  0  | 3^1 = 2    | counts[2] = 0                | k=0 -> counts[2^1=3] = 1             |            |            |
 * |     |      |     |            |                              | k=1 -> counts[2^2=0] = 1             | 2 + 2 = 4  | counts[2]=1|
 * 
 * Final Result: 4. (The substrings: "a", "b", "a", "aba").
 *
 *
 * ## 5. Follow-ups
 * - **Q:** What if the alphabet contained all 26 lowercase letters?
 *   **A:** The bitmask approach still works conceptually, but the state array `counts` would need to be 
 *   size 2^26 (approx. 67 million elements, or 536 MB of RAM). At that point, memory initialization becomes 
 *   a bottleneck, and we might switch to a `HashMap<Integer, Long>`, trading O(1) array access for O(1) map 
 *   access to save space on sparse datasets.
 * - **Q:** What if the problem asked for exactly TWO characters to have an odd frequency?
 *   **A:** We would need the current bitmask to differ from a past bitmask by exactly TWO bits. Inside the loop, 
 *   we would need two nested loops to flip two bits `(1 << k1) | (1 << k2)`. There are 45 combinations (10 choose 2), 
 *   so the time complexity would increase by a factor of 45.
 *
 * ============================================================================
 */

public class WonderfulSubstringsStudy {

    interface WonderfulSubstringSolver {
        long wonderfulSubstrings(String word);
    }

    // ========================================================================
    // APPROACH 3: BITMASK + FREQUENCY ARRAY (The Optimal Interview Solution)
    // ========================================================================
    static class BitmaskArraySolver implements WonderfulSubstringSolver {
        @Override
        public long wonderfulSubstrings(String word) {
            // Array of size 1024 (2^10) because there are 10 possible letters ('a' to 'j').
            // Each index represents a distinct even/odd state combination of those 10 letters.
            // Using `long` because total combinations can exceed Integer.MAX_VALUE.
            long[] prefixCounts = new long[1024];
            
            // CRITICAL BASE CASE: Represents the empty prefix before the string starts.
            // A bitmask of 0 means all letters have appeared an even number of times (0 times).
            // Without this, a wonderful substring starting exactly at index 0 would be missed.
            prefixCounts[0] = 1;
            
            long result = 0;
            int currentMask = 0;
            
            // Convert to char array to avoid the overhead of word.charAt(i) inside the loop
            for (char c : word.toCharArray()) {
                
                // Determine which bit represents this character (0 for 'a', 1 for 'b', etc.)
                int bitShift = c - 'a';
                
                // Toggle the parity of this character in our running bitmask.
                // XOR (^) perfectly flips 0 to 1 (even to odd) or 1 to 0 (odd to even).
                currentMask ^= (1 << bitShift);
                
                // Scenario 1: ALL letters have an even frequency.
                // This happens when the current bitmask EXACTLY matches a previous bitmask.
                result += prefixCounts[currentMask];
                
                // Scenario 2: EXACTLY ONE letter has an odd frequency.
                // This happens when the current bitmask differs from a previous bitmask by exactly 1 bit.
                // We test all 10 possible characters to see if we've seen that specific 1-bit-off state.
                for (int i = 0; i < 10; i++) {
                    int maskWithOneFlippedBit = currentMask ^ (1 << i);
                    result += prefixCounts[maskWithOneFlippedBit];
                }
                
                // Finally, record the current state for future iterations to pair with.
                prefixCounts[currentMask]++;
            }
            
            return result;
        }
    }

    // ========================================================================
    // APPROACH 1: BRUTE FORCE (Conceptual Baseline)
    // ========================================================================
    static class BruteForceSolver implements WonderfulSubstringSolver {
        @Override
        public long wonderfulSubstrings(String word) {
            long count = 0;
            int n = word.length();
            
            for (int i = 0; i < n; i++) {
                // Reset frequency counter for every new starting position
                int[] charCounts = new int[10];
                int oddCount = 0;
                
                for (int j = i; j < n; j++) {
                    int charIdx = word.charAt(j) - 'a';
                    charCounts[charIdx]++;
                    
                    // Keep track of how many odd frequencies we have dynamically
                    if (charCounts[charIdx] % 2 == 1) {
                        oddCount++;
                    } else {
                        oddCount--;
                    }
                    
                    // Definition of wonderful: at most 1 odd character
                    if (oddCount <= 1) {
                        count++;
                    }
                }
            }
            
            return count;
        }
    }


    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        WonderfulSubstringSolver[] solvers = {
            new BitmaskArraySolver(),
            new BruteForceSolver()
        };

        // Format: {word, expected_result}
        String[][] testCases = {
            {"aba", "4"},             // Basic test case: "a", "b", "a", "aba"
            {"aabbc", "11"},          // Contains an all-even string "aabb"
            {"he", "2"},              // Two unique chars
            {"a", "1"},               // Single char
            {"aaaa", "10"},           // All same chars (combinations: 4 of len1, 3 of len2, 2 of len3, 1 of len4)
            {"abcdefghij", "10"}      // All unique chars (only len 1 substrings are wonderful)
        };

        for (WonderfulSubstringSolver solver : solvers) {
            System.out.println("Testing " + solver.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (int i = 0; i < testCases.length; i++) {
                String word = testCases[i][0];
                long expected = Long.parseLong(testCases[i][1]);
                
                long result = solver.wonderfulSubstrings(word);
                
                if (result != expected) {
                    System.out.printf("  [FAIL] Test '%s'. Expected: %d, Got: %d%n", 
                        word, expected, result);
                    allPassed = false;
                }
            }
            
            if (allPassed) {
                System.out.println("  [SUCCESS] All test cases passed!\n");
            }
        }
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * - Core pattern: Prefix Bitmask + Frequency Tracking.
 * - Key observation: Counting frequencies is overkill when the problem only cares 
 *   about even/odd parity. A bitmask tracks parity for 10 items in a single O(1) integer.
 * - What's worth memorizing: The loop `currentMask ^ (1 << i)`. This is the universal 
 *   idiom for flipping a single bit to check for "off-by-one" parity differences.
 * - Most common trap: Forgetting `counts[0] = 1`. Substrings that are wonderful right 
 *   from index 0 require a neutral base-case of `0` to match against.
 * - Mental trigger: "Substring frequencies + Even/Odd properties + Small alphabet? 
 *   Bitmask + Frequency Array."
 * ============================================================================
 */
