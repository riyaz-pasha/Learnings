/**
 * ============================================================================
 * J E W E L S   A N D   S T O N E S
 * ============================================================================
 *
 * ## 1. Clarifying questions
 * 1. "Are we strictly dealing with English letters (a-z, A-Z), or full Unicode?" 
 *    (Crucial for space limits: English letters fit in a tiny 128-byte array, while Unicode requires a HashMap).
 * 2. "Could the 'stones' string be massively large in a real system (e.g., streaming logs)?" 
 *    (If stones is unbounded/streaming, O(J * S) brute force completely fails; we MUST optimize lookup to O(1)).
 * 3. "Are there duplicates in the 'jewels' string?"
 *    (The constraint says 'unique', but if not, we'd need to ensure we don't over-count or redundantly hash).
 * 4. "Is this case-sensitive?"
 *    (Yes, the prompt clarifies 'a' != 'A', meaning we can't just `.toLowerCase()` the inputs).
 * 
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We have a sequence of items (`stones`), and for every single item, we must answer 
 * > "Is this item in the target group (`jewels`)?" The bottleneck is the cost of repeatedly answering 
 * > that "Is it in the group?" question.
 *
 * ### Approach 1: Nested Loops (The Brute Force)
 * 1. What I'd naturally try: Pick up the first stone. Scan the entire `jewels` string to see if it matches. 
 *    Pick up the second stone. Scan the entire `jewels` string again. Repeat.
 * 2. Why it works: It manually verifies every stone against every jewel.
 * 3. Why it's too costly: 
 *    - Time Complexity: O(J * S) — because for every one of the S stones, we do up to J comparisons.
 *    - Space Complexity: O(1) — because we only use loop indices; no extra memory is allocated.
 * 4. What work is being repeated: We are re-reading the `jewels` string from scratch for every single stone. 
 *    If 'a' is a jewel, we shouldn't have to scan the string to remember that.
 * 5. What property removes the bottleneck: Memory. If we read `jewels` exactly once and write down what 
 *    we found in a fast-lookup structure, we never have to scan `jewels` again.
 *
 * ### Approach 2: HashSet (The Standard Developer Way)
 * 1. What I'd naturally try: Iterate through `jewels` once and dump each character into a `HashSet<Character>`. 
 *    Then, iterate through `stones` and check if the set `.contains()` the stone.
 * 2. Why it works: HashSets are mathematically designed to answer "Have I seen this before?" in constant time.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(J + S) — because we do exactly one pass over `jewels` to build the set, and 
 *      exactly one pass over `stones` to check the set.
 *    - Space Complexity: O(J) — because we store up to J characters in the HashSet.
 * 4. What work is being repeated: HashSets in Java require wrapping primitive `char`s into `Character` objects 
 *    (autoboxing), computing hash codes, and allocating heap memory for nodes. For just 52 possible letters, 
 *    this is massive structural overkill.
 * 5. What property removes the bottleneck: The domain is strictly English letters. ASCII characters map 
 *    directly to integer values from 65 ('A') to 122 ('z'). We can use raw memory arrays instead of Objects.
 *
 * ### Approach 3: Boolean Array (The Hardware-Friendly Optimum)
 * 1. What I'd naturally try: Create a `boolean[128]` array. If 'a' (ASCII 97) is a jewel, set `isJewel[97] = true`.
 * 2. Why it works: Array index lookups are bare-metal operations. It's the same O(1) logic as the HashSet, 
 *    but bypasses the Java garbage collector and object overhead entirely.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(J + S) — because we still process each string exactly once, but with near-zero overhead.
 *    - Space Complexity: O(1) — because we allocate exactly one 128-byte array, regardless of whether 
 *      the strings are length 5 or length 5,000,000.
 *
 * > **Interview Strategy:** In a 45-minute interview, I would briefly mention the HashSet as the generic 
 * > solution, but immediately write the Boolean Array (Approach 3). It shows mechanical sympathy (understanding 
 * > how data structures translate to hardware) and is actually fewer lines of code to write than the HashSet.
 *
 * 
 * ## 3. Edge cases
 * - `jewels` has letters, `stones` has NO jewels: e.g., `j="a"`, `s="bbb"`. (Loop runs, count stays 0).
 * - All `stones` are jewels: e.g., `j="aA"`, `s="aAaA"`. (Perfect match, count = 4).
 * - Case collision trap: `j="a"`, `s="A"`. (Returns 0. Validates our case-sensitivity).
 *
 * 
 * ## 4. Key Insight, Dry Run & Pitfalls
 * **Key Insight:** "Domain constraint = Array replacement." Whenever a problem guarantees the input belongs 
 * to a small, contiguous set (like ASCII characters or numbers 1-1000), immediately replace HashMaps/HashSets 
 * with direct-mapped Arrays.
 *
 * **Dry Run of Approach 3:**
 * jewels = "aA", stones = "aAAbbbb"
 * 
 * 1. Build Phase:
 *    - 'a' (ASCII 97) -> isJewel[97] = true
 *    - 'A' (ASCII 65) -> isJewel[65] = true
 * 2. Count Phase:
 *    - stone 'a' (97): isJewel[97] == true. Count = 1.
 *    - stone 'A' (65): isJewel[65] == true. Count = 2.
 *    - stone 'A' (65): isJewel[65] == true. Count = 3.
 *    - stone 'b' (98): isJewel[98] == false. Count = 3.
 *    - (skipping remaining 'b's, all false).
 * Result: 3.
 *
 * **Pitfall: String.indexOf() Illusion**
 * A common beginner mistake is `if (jewels.indexOf(stone) > -1) count++;`.
 * While this looks like a 1-liner, `indexOf()` scans the `jewels` string under the hood. 
 * This is literally Approach 1 (Brute Force) disguised as clean code. It is secretly O(J * S).
 *
 * **Pattern Recognition:** "When you see X, think Y."
 * - When you see: "English letters only" + "Lookup" -> Think: `boolean[128]` or `int[128]`.
 * - Transfers to: First Unique Character in a String, Valid Anagram, Longest Palindrome.
 * 
 * **Interview Script:**
 * "The naive approach is nested loops, checking every stone against the jewels string, which is O(J * S). 
 * We want O(1) lookups. I could use a HashSet, but since the problem guarantees English letters, we can 
 * optimize heavily by using a 128-sized boolean array. ASCII characters evaluate to integers, so the character 
 * itself acts as the array index. That gives us O(J + S) time and strictly O(1) space."
 *
 * 
 * ## 5. Follow-ups
 * - **Q:** What if memory was so restricted you couldn't even use a 128-byte array?
 *   **A:** We could use a single 64-bit integer (`long`) as a Bitmask! The English alphabet (A-Z, a-z) spans 
 *   from ASCII 65 to 122. The difference is 57. A `long` has 64 bits. We can represent the presence of every 
 *   possible letter by flipping specific bits in a single `long` variable. Space becomes exactly 8 bytes.
 * - **Q:** What if the jewels string is updated dynamically while we are checking stones?
 *   **A:** The boolean array handles dynamic updates perfectly in O(1) time (`isJewel[newChar] = true`).
 *
 * ============================================================================
 */

import java.util.HashSet;
import java.util.Set;

public class JewelsAndStonesStudy {

    /**
     * Common interface to run and test all approaches.
     */
    interface JewelCounter {
        int numJewelsInStones(String jewels, String stones);
    }

    // ========================================================================
    // APPROACH 3: BOOLEAN ARRAY (The Optimal Interview Solution)
    // ========================================================================
    static class BooleanArrayCounter implements JewelCounter {
        @Override
        public int numJewelsInStones(String jewels, String stones) {
            // Space: O(1) - Always 128 elements regardless of input size.
            // Why 128? The standard ASCII table goes up to 127. 'z' is 122.
            boolean[] isJewel = new boolean[128];
            
            // Phase 1: Record the jewels
            // A char acts seamlessly as an integer array index in Java.
            for (char j : jewels.toCharArray()) {
                isJewel[j] = true; 
            }
            
            // Phase 2: Count the matching stones
            int count = 0;
            for (char s : stones.toCharArray()) {
                // Instant O(1) memory lookup
                if (isJewel[s]) {
                    count++;
                }
            }
            
            return count;
        }
    }

    // ========================================================================
    // APPROACH 4: BITMASK (The "Flex" Optimum - 8 bytes of memory)
    // ========================================================================
    static class BitmaskCounter implements JewelCounter {
        @Override
        public int numJewelsInStones(String jewels, String stones) {
            // A single 64-bit primitive. All bits are initially 0.
            long jewelMask = 0L;
            
            // Phase 1: Build the bitmask
            for (char j : jewels.toCharArray()) {
                // Shift a '1' bit to the left by the offset from 'A'.
                // If j is 'A' (65), shift by 0.
                // If j is 'z' (122), shift by 57. (Fits safely in 64 bits).
                jewelMask |= (1L << (j - 'A'));
            }
            
            // Phase 2: Check the stones using bitwise AND
            int count = 0;
            for (char s : stones.toCharArray()) {
                // If the bit at this offset is 1, the result of AND is non-zero.
                if ((jewelMask & (1L << (s - 'A'))) != 0) {
                    count++;
                }
            }
            
            return count;
        }
    }

    // ========================================================================
    // APPROACH 2: HASH SET (General purpose, handles Unicode)
    // ========================================================================
    static class HashSetCounter implements JewelCounter {
        @Override
        public int numJewelsInStones(String jewels, String stones) {
            // Allocate memory for Set Node objects and wrapper Character objects.
            Set<Character> jewelSet = new HashSet<>();
            
            for (char j : jewels.toCharArray()) {
                jewelSet.add(j); // Autoboxing: char -> Character
            }
            
            int count = 0;
            for (char s : stones.toCharArray()) {
                if (jewelSet.contains(s)) {
                    count++;
                }
            }
            
            return count;
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        JewelCounter[] counters = {
            new BooleanArrayCounter(),
            new BitmaskCounter(),
            new HashSetCounter()
        };

        // Test Cases {jewels, stones, expected_result}
        String[][] testCases = {
            {"aA", "aAAbbbb", "3"},   // Standard case
            {"z", "ZZ", "0"},         // Case sensitivity test
            {"abc", "def", "0"},      // No overlap
            {"a", "a", "1"},          // Minimal size
            {"A", "AAAAAAAA", "8"}    // All jewels
        };

        for (JewelCounter counter : counters) {
            System.out.println("Testing " + counter.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (String[] test : testCases) {
                String jewels = test[0];
                String stones = test[1];
                int expected = Integer.parseInt(test[2]);
                
                int result = counter.numJewelsInStones(jewels, stones);
                
                if (result != expected) {
                    System.out.printf("  [FAIL] j=\"%s\", s=\"%s\". Expected: %d, Got: %d%n", 
                        jewels, stones, expected, result);
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
 * - Core pattern: Two-pass Set/Dictionary mapping. (Write state, then Query state).
 * - Key observation: You don't need a heavy `HashSet` when the domain is restricted 
 *   to ASCII characters. A primitive array provides the exact same O(1) logic with 
 *   drastically lower overhead.
 * - What's worth memorizing: `boolean[128]` for ASCII presence tracking. 
 *   `char c = 'a'; isJewel[c] = true;`. Java handles the char-to-int conversion natively.
 * - Most common trap: Using `jewels.indexOf(stone) != -1`. It looks clean but 
 *   silently changes the time complexity from O(J+S) to O(J*S).
 * - Mental trigger: "Finding items in a specific alphabet = Boolean Array."
 * ============================================================================
 */


import java.util.*;

/**
 * ============================================================
 * 🔷 PROBLEM: Jewels and Stones
 * ============================================================
 *
 * We are given:
 * - jewels → characters representing types of jewels
 * - stones → characters representing stones we have
 *
 * Goal:
 * Count how many stones are jewels.
 *
 * Key Constraint:
 * - Case-sensitive ('a' != 'A')
 * - jewels characters are UNIQUE
 *
 * ============================================================
 * 🧠 HOW TO THINK (INTERVIEW INSIGHT)
 * ============================================================
 *
 * We need to repeatedly answer:
 *      "Is this stone a jewel?"
 *
 * This is a classic:
 *      🔹 Repeated lookup problem
 *
 * So instead of scanning jewels again and again:
 *      → Convert jewels into a structure with FAST LOOKUP
 *
 * Options:
 * 1. HashSet  → General solution
 * 2. Boolean Array → Best when domain is small (like here)
 *
 * ============================================================
 */


/* ============================================================
 * ✅ APPROACH 1: HashSet (General + Clean)
 * ============================================================
 *
 * IDEA:
 * - Store all jewels in a HashSet
 * - Iterate stones
 * - If stone exists in set → increment count
 *
 * WHY IT WORKS:
 * - HashSet provides O(1) average lookup
 *
 * TIME:  O(J + S)
 * SPACE: O(J)
 *
 * ============================================================
 */
class SolutionHashSet {

    public int numJewelsInStones(String jewels, String stones) {

        // Step 1: Convert jewels → HashSet for O(1) lookup
        Set<Character> jewelSet = new HashSet<>();

        for (char ch : jewels.toCharArray()) {
            jewelSet.add(ch);
        }

        int count = 0;

        // Step 2: Traverse stones
        for (char stone : stones.toCharArray()) {

            // Check if current stone is a jewel
            if (jewelSet.contains(stone)) {
                count++; // Found a jewel
            }
        }

        return count;
    }
}


/* ============================================================
 * ✅ APPROACH 2: Boolean Array (Most Optimal)
 * ============================================================
 *
 * IDEA:
 * - Only 52 possible characters:
 *      'a' to 'z' → 26
 *      'A' to 'Z' → 26
 *
 * - Use boolean[52] instead of HashSet
 * - Map each character → index
 *
 * WHY THIS IS BETTER:
 * - No hashing overhead
 * - True O(1) lookup (array access)
 *
 * TIME:  O(J + S)
 * SPACE: O(1) (constant size = 52)
 *
 * ============================================================
 */
class SolutionBooleanArray {

    public int numJewelsInStones(String jewels, String stones) {

        // Step 1: Create array to mark jewels
        boolean[] isJewel = new boolean[52];

        // Mark all jewel characters
        for (char ch : jewels.toCharArray()) {
            int index = getIndex(ch);   // map char → index
            isJewel[index] = true;
        }

        int count = 0;

        // Step 2: Traverse stones
        for (char stone : stones.toCharArray()) {

            // Direct array lookup → O(1)
            if (isJewel[getIndex(stone)]) {
                count++;
            }
        }

        return count;
    }

    /**
     * Helper function to map character → array index
     *
     * Mapping:
     *  'a' - 'z' → 0 to 25
     *  'A' - 'Z' → 26 to 51
     *
     * This ensures unique mapping for all 52 characters.
     */
    private int getIndex(char ch) {

        if (ch >= 'a' && ch <= 'z') {
            return ch - 'a';          // lowercase → 0-25
        } else {
            return ch - 'A' + 26;     // uppercase → 26-51
        }
    }
}


/* ============================================================
 * ❌ APPROACH 3: Brute Force (For Understanding Only)
 * ============================================================
 *
 * IDEA:
 * - For each stone → scan all jewels
 *
 * WHY BAD:
 * - Repeated work
 * - Inefficient for large inputs
 *
 * TIME:  O(J * S)
 * SPACE: O(1)
 *
 * ============================================================
 */
class SolutionBruteForce {

    public int numJewelsInStones(String jewels, String stones) {

        int count = 0;

        // For every stone
        for (char stone : stones.toCharArray()) {

            // Check against every jewel
            for (char jewel : jewels.toCharArray()) {

                if (stone == jewel) {
                    count++;
                    break; // Avoid double counting
                }
            }
        }

        return count;
    }
}

