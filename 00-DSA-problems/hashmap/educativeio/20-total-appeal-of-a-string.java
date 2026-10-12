/**
 * ============================================================================
 * 2262. TOTAL APPEAL OF A STRING
 * ============================================================================
 * 
 * ## 1. Clarifying questions
 * 1. "Can the total appeal exceed the 32-bit integer limit?" 
 *    (For N=1000, max appeal is ~1.3 million, fitting in an int. But if this algorithm were deployed where N=10^5, max appeal hits ~2.6 * 10^10, which overflows an int. Using `long` is safer and industry-standard for aggregations).
 * 2. "Does the string strictly contain lowercase English letters?" 
 *    (Crucial for space limits. Yes, meaning we can use a tiny 26-element array instead of a HashMap to track characters).
 * 3. "Does 'appeal' refer to the total count of characters, or just the distinct ones?"
 *    (The prompt specifies distinct characters. "aba" has an appeal of 2, not 3).
 * 
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We need to calculate a property (unique characters) for every possible contiguous 
 * > substring and sum them up. The bottleneck is the sheer number of substrings. An array of size N 
 * > has N*(N+1)/2 substrings.
 * 
 * ### Approach 1: Expand Around Start (Brute Force)
 * 1. What I'd naturally try: For every starting index `i`, I'll expand an ending index `j` to the right. 
 *    I'll keep a `boolean[26]` array to track what characters I've seen in the current `i...j` window. 
 *    Every time I add a character I haven't seen yet, I increment the window's appeal, and add it to a grand total.
 * 2. Why it works: It exhaustively generates every substring exactly once and counts its unique characters.
 * 3. Why it's too costly:
 *    - Time Complexity: O(N^2) — For N=1000, 1 million operations is perfectly fine. But for scaling to 
 *      N=10^5, this results in 10 billion operations, guaranteeing a Time Limit Exceeded (TLE).
 *    - Space Complexity: O(1) auxiliary — since the boolean array is fixed at size 26.
 * 4. What work is being repeated: We are repeatedly discovering the "first time" a character appears in 
 *    overlapping windows. If we know 'a' is at index 5, we shouldn't have to re-discover it for every 
 *    substring starting at index 0, 1, 2, 3, and 4.
 * 5. What property removes the bottleneck: "End-Anchored State". Instead of asking "what is the appeal 
 *    of each substring?", we should ask "how much does `s[i]` increase the appeal of all substrings 
 *    that came before it?"
 * 
 * ### Approach 2: Substring Ending Contributions (The Algorithmic Optimum)
 * 1. What I'd naturally try: Let's process the string left to right. Assume we already know the sum of 
 *    the appeals of all substrings ending at index `i-1`. When we append `s[i]`, we form new substrings 
 *    ending at `i`. How much does their total appeal increase compared to the previous step?
 * 2. Why it works: Appending `s[i]` adds exactly 1 to the appeal of a substring ONLY IF `s[i]` wasn't 
 *    already in it. Which substrings don't contain `s[i]`? The ones that start *after* the last time we 
 *    saw `s[i]`. If we track the `lastSeen` index of every character, there are exactly `i - lastSeen[s[i]]` 
 *    substrings ending at `i` that get a +1 appeal boost!
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we traverse the string exactly once.
 *    - Space Complexity: O(1) auxiliary — a single array of size 26 tracks the last seen indices.
 * 
 * > **Interview Strategy:** Start by explicitly writing the mathematical transition state out loud: 
 * > `Appeal(i) = Appeal(i-1) + (i - lastSeen[char])`. Once an interviewer hears that, they know 
 * > you've cracked the optimal pattern.
 * 
 * 
 * ## 3. Edge cases
 * - All identical characters: `"aaaa"`. `lastSeen` updates perfectly. Every step only adds 1 new appeal 
 *   (from the single-character substring `"a"`). Total = 4.
 * - All unique characters: `"abcd"`. `lastSeen` is always -1. Every step adds `i - (-1) = i + 1` appeal. 
 *   Maximal possible growth.
 * - String of length 1: Loop runs once, returns 1.
 * 
 * 
 * ## 4. Key Insight & Dry Run
 * 
 * **Key Insight: "The Anchor Array"**
 * When a problem asks for the sum of a property across all subarrays, ask: "If I know the answer for all 
 * subarrays ending at `i-1`, how do I calculate it for `i`?" 
 * 
 * **Dry Run of Approach 2:**
 * `s = "aba"`
 * Init: `lastSeen = [-1, -1, -1...]`, `currEndingAppeal = 0`, `total = 0`
 * 
 * | idx | char | `lastSeen` | Formula (`i - lastSeen`) | `currEndingAppeal` | `total` | Substrings Ending Here & their appeals |
 * |-----|------|------------|--------------------------|--------------------|---------|----------------------------------------|
 * |  0  | 'a'  | -1         | 0 - (-1) = 1             | 0 + 1 = 1          | 1       | "a"(1)                                 |
 * |  1  | 'b'  | -1         | 1 - (-1) = 2             | 1 + 2 = 3          | 4       | "ab"(2), "b"(1)                        |
 * |  2  | 'a'  |  0         | 2 - 0 = 2                | 3 + 2 = 5          | 9       | "aba"(2), "ba"(2), "a"(1)              |
 * 
 * Final Result: 9. 
 * (Notice at index 2, 'a' only added appeal to "ba" and "a". It did NOT add appeal to "aba" because 'a' was already present from index 0!).
 * 
 * 
 * ## 5. Follow-ups
 * - **Q:** What if the string contained any Unicode character, not just lowercase English?
 *   **A:** I would replace the `int[26]` array with a `HashMap<Character, Integer>` to track the `lastSeen` 
 *   index. The time complexity remains O(N), but space scales to O(U) where U is the number of unique characters.
 * - **Q:** What if the appeal was defined as the number of characters that appear EXACTLY ONCE in the substring?
 *   **A:** This is a famous variant (LeetCode 828). A character only contributes +1 to substrings starting *after* 
 *   its second-to-last occurrence, and *before* its next occurrence. We would track `lastSeen` and `prevLastSeen` 
 *   to calculate `(i - lastSeen) * (lastSeen - prevLastSeen)`.
 * 
 * ============================================================================
 */

import java.util.Arrays;

public class TotalAppealOfAStringStudy {

    interface AppealSolver {
        long appealSum(String s);
    }

    // ========================================================================
    // APPROACH 2: SUBSTRING ENDING CONTRIBUTIONS (The Optimal Solution)
    // ========================================================================
    static class OptimalSolver implements AppealSolver {
        @Override
        public long appealSum(String s) {
            // Tracks the index where each character was last seen.
            // Using an array of size 26 because constraints guarantee lowercase English letters.
            int[] lastSeen = new int[26];
            
            // Crucial Base Case: If a character hasn't been seen yet, its "last seen" 
            // position is virtually -1. This ensures the math `i - (-1)` works perfectly 
            // for calculating distances.
            Arrays.fill(lastSeen, -1);
            
            long totalAppeal = 0;
            long currentEndingAppeal = 0;
            
            for (int i = 0; i < s.length(); i++) {
                int charIdx = s.charAt(i) - 'a';
                
                // How many substrings ending at 'i' DO NOT already contain this character?
                // Exactly the ones starting after the index where we last saw it.
                int contribution = i - lastSeen[charIdx];
                
                // The sum of appeals for all substrings ending at 'i' is the sum of appeals
                // for substrings ending at 'i-1', plus the new contribution.
                currentEndingAppeal += contribution;
                
                // Add this ending segment's total to our grand total
                totalAppeal += currentEndingAppeal;
                
                // Record the current character's index for future iterations
                lastSeen[charIdx] = i;
            }
            
            return totalAppeal;
        }
    }

    // ========================================================================
    // APPROACH 1: EXPAND AROUND START (The Brute Force Baseline)
    // ========================================================================
    static class BruteForceSolver implements AppealSolver {
        @Override
        public long appealSum(String s) {
            long totalAppeal = 0;
            int n = s.length();
            
            // Fix the starting point of the substring
            for (int i = 0; i < n; i++) {
                boolean[] seenInWindow = new boolean[26];
                int currentUniqueCount = 0;
                
                // Expand the ending point to the right
                for (int j = i; j < n; j++) {
                    int charIdx = s.charAt(j) - 'a';
                    
                    // If we haven't seen this character in our current window yet, it adds to the appeal
                    if (!seenInWindow[charIdx]) {
                        seenInWindow[charIdx] = true;
                        currentUniqueCount++;
                    }
                    
                    totalAppeal += currentUniqueCount;
                }
            }
            
            return totalAppeal;
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        AppealSolver[] solvers = {
            new OptimalSolver(),
            new BruteForceSolver()
        };

        // Format: {input_string, expected_total_appeal}
        String[][] testCases = {
            {"aba", "9"},               // Simple overlap case
            {"abc", "14"},              // All unique (1*3 + 2*2 + 3*1 = 3 + 4 + 3 = 10? Wait: a=1, b=1, c=1, ab=2, bc=2, abc=3 -> 1+1+1+2+2+3 = 10. Wait, "abc" test case expected is 10.)
            {"aaaa", "10"},             // All identical. Sum of 1 for 4+3+2+1 substrings = 10
            {"code", "20"}              // All unique. Substrings: 4 len1, 3 len2, 2 len3, 1 len4. Total = 4*1 + 3*2 + 2*3 + 1*4 = 4 + 6 + 6 + 4 = 20.
        };
        
        // Correcting expected value for "abc" in code test array
        testCases[1][1] = "10";

        for (AppealSolver solver : solvers) {
            System.out.println("Testing " + solver.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (int i = 0; i < testCases.length; i++) {
                String input = testCases[i][0];
                long expected = Long.parseLong(testCases[i][1]);
                
                long result = solver.appealSum(input);
                
                if (result != expected) {
                    System.out.printf("  [FAIL] Test '%s'. Expected: %d, Got: %d%n", 
                        input, expected, result);
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
 * - Core pattern: Iterative State Transition (DP) anchoring on the end-index.
 * - Key observation: You don't need to count the unique characters of every substring. 
 *   You only need to know how appending a new character changes the total appeal of 
 *   everything that came before it.
 * - What's worth memorizing: The formula `(i - lastSeen)`. This is the exact number 
 *   of substrings ending at `i` that DO NOT contain the current character.
 * - Most common trap: Trying to use the Inclusion-Exclusion principle by counting total 
 *   substrings and subtracting overlaps. While mathematically possible, tracking it 
 *   incrementally via `currentEndingAppeal` is significantly easier to write bug-free.
 * - Mental trigger: "Sum of properties of all substrings? Track the difference made 
 *   by adding the Nth character."
 * ============================================================================
 */
