/**
 * ============================================================================
 * 299. BULLS AND COWS
 * ============================================================================
 * 
 * ## 1. Clarifying questions
 * 1. "Can the `secret` and `guess` strings contain non-digit characters?" 
 *    (Ensures we can safely use a tiny array of size 10 instead of a full HashMap for character frequencies).
 * 2. "Are the two strings guaranteed to be the exact same length?"
 *    (Prevents `IndexOutOfBoundsException` when iterating through both simultaneously).
 * 3. "If a digit is counted as a Bull, can it also be counted as a Cow?"
 *    (Crucial for game rules: No. A digit is either a Bull, a Cow, or neither. Bulls take priority).
 * 4. "Are there multiple test cases per run, or should I optimize for one massive string?"
 *    (Dictates whether we should optimize object creation; arrays of size 10 are cheap enough to recreate every time).
 * 
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We must identify exact position matches (Bulls) first, because they take priority. 
 * > Then, we need to count how many of the *remaining* characters in the guess exist in the *remaining* 
 * > characters of the secret (Cows). The bottleneck is counting these unmatched characters efficiently.
 * 
 * ### Approach 1: Mark and Sweep (Brute Force)
 * 1. What I'd naturally try: I'd use two loops. In the first pass, I check `secret.charAt(i) == guess.charAt(i)`. 
 *    If yes, increment Bulls and mark that index as "used" in a boolean array. In the second pass, for every 
 *    unused character in `guess`, I scan the entire `secret` array for a matching unused character. If found, 
 *    I mark it as used and increment Cows.
 * 2. Why it works: It perfectly isolates Bulls from Cows and ensures no digit is double-counted.
 * 3. Why it's too slow/costly:
 *    - Time Complexity: O(N^2) — because finding Cows requires scanning the remaining `secret` for every 
 *      remaining `guess` character.
 *    - Space Complexity: O(N) — because we allocate `boolean[] usedSecret` and `boolean[] usedGuess` of size N.
 * 4. What work is being repeated: We are repeatedly scanning the string to find specific numbers. Since 
 *    order doesn't matter for Cows, we only care about *how many* of each digit are available.
 * 5. What property removes the bottleneck: Frequency counting. If we know we have three '7's left in the 
 *    secret, and the guess is asking for two '7's, we have exactly 2 Cows.
 * 
 * ### Approach 2: Two Passes with Frequency Arrays (The Standard Developer Way)
 * 1. What I'd naturally try: Do one pass to find Bulls. If an index is NOT a Bull, count the frequency of 
 *    the secret digit in `int[] sCounts` and the guess digit in `int[] gCounts`. After the pass, the number 
 *    of Cows for any digit 'd' is simply `Math.min(sCounts[d], gCounts[d])`.
 * 2. Why it works: It decouples the spatial arrangement of Cows from their existence, replacing an O(N) 
 *    search with an O(1) array lookup.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we iterate the strings once, plus a tiny fixed loop of 10 for the alphabet.
 *    - Space Complexity: O(1) — because we use two arrays of size 10, regardless of the string length (N).
 * 4. What work is being repeated: We are keeping two separate ledgers for supply (`sCounts`) and demand (`gCounts`), 
 *    and resolving them at the end. Could we track them in a single ledger as we go?
 * 5. What property removes the bottleneck: "Net Balance". A secret digit is a deposit (+1), a guess digit 
 *    is a withdrawal (-1). If you make a deposit and the balance is currently negative, it means someone 
 *    was waiting for that digit (a Cow!). 
 * 
 * ### Approach 3: One Pass with a Shared Ledger (The Elegant Optimum)
 * 1. What I'd naturally try: Use a single `int[10] ledger`. Walk the strings. If it's a Bull, count it. 
 *    If not, the secret digit increments its bucket, and the guess digit decrements its bucket.
 * 2. Why it works: If the secret digit sees its bucket is `< 0`, it knows the guess string already asked 
 *    for this digit earlier. Cow! If the guess digit sees its bucket is `> 0`, it knows the secret string 
 *    already provided this digit earlier. Cow!
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we do exactly one pass over the strings and resolve everything on the fly.
 *    - Space Complexity: O(1) — because we only need a single integer array of size 10.
 * 
 * > **Interview Strategy:** In a real interview, Approach 2 (Two Arrays) is perfectly fine and very easy to 
 * > explain. However, Approach 3 (Single Array) is the "wow" solution. I would write Approach 3 but clearly 
 * > explain the supply/demand logic, as the single-array math can look like magic if unexplained.
 * 
 * 
 * ## 3. Edge cases
 * - All Bulls: `secret = "1234", guess = "1234"`. Return "4A0B".
 * - All Cows: `secret = "1234", guess = "4321"`. Return "0A4B".
 * - Duplicates with partial matches: `secret = "1123", guess = "0111"`. 
 *   The guess has three '1's. The secret has two '1's. One is a Bull (index 1). There is only one '1' left 
 *   in the secret, so the remaining two '1's in the guess can only produce 1 Cow. Return "1A1B".
 * 
 * 
 * ## 4. Key Insight & Dry Run
 * 
 * **Key Insight:** "Supply and Demand." 
 * Instead of keeping two arrays, track the *net balance* of digits. 
 * `secret` supplies digits (balance goes up). `guess` demands digits (balance goes down).
 * If `secret` provides a digit and the balance was negative, it filled a past demand -> Cow.
 * If `guess` demands a digit and the balance was positive, it consumed a past supply -> Cow.
 * 
 * **Dry Run of Approach 3 (Shared Ledger):**
 * `secret = "1807"`, `guess = "7810"`
 * Initial: `bulls=0`, `cows=0`, `ledger[10] = {0}`
 * 
 * | i | s | g | Bull? | s Ledger Check | g Ledger Check | Updates | State |
 * |---|---|---|---|---|---|---|---|
 * | 0 | 1 | 7 | No | `ledger[1] == 0` | `ledger[7] == 0` | `ledger[1]++`, `ledger[7]--` | l[1]=1, l[7]=-1 |
 * | 1 | 8 | 8 | YES | (Skip ledger) | (Skip ledger) | `bulls++` | bulls=1 |
 * | 2 | 0 | 1 | No | `ledger[0] == 0` | `ledger[1] == 1` (>0)! | `cows++`. `ledger[0]++`, `ledger[1]--` | cows=1, l[0]=1, l[1]=0 |
 * | 3 | 7 | 0 | No | `ledger[7] == -1` (<0)! | `ledger[0] == 1` (>0)! | `cows+=2`. `ledger[7]++`, `ledger[0]--`| cows=3, l[0]=0, l[7]=0 |
 * 
 * Result: 1 Bull, 3 Cows -> "1A3B".
 * 
 * 
 * ## 5. Pitfalls
 * - **The String Concatenation Trap:** Returning `bulls + "A" + cows + "B"` in Java is fine for short strings, 
 *   but many candidates write this inside a loop when building results. Here it's O(1) at the end, so it's fine, 
 *   but using `StringBuilder` or `String.format` is sometimes cleaner.
 * - **Counting Bulls as Cows:** If you increment the frequency arrays for *every* digit (even Bulls), you will 
 *   over-count Cows. You must strictly `continue;` or skip array updates if `s == g`.
 * 
 * 
 * ## 6. Follow-ups
 * - **Q:** What if the string could contain any ASCII character, not just digits?
 *   **A:** We simply increase the ledger size from `int[10]` to `int[128]` (or 256). The logic remains exactly 
 *   the same and is still O(1) space.
 * - **Q:** What if the game rules changed so the strings could be different lengths?
 *   **A:** The game would only process up to `Math.min(secret.length(), guess.length())` for Bulls. For Cows, 
 *   we would process the trailing unmatched characters in the longer string as just supply (if secret) or 
 *   demand (if guess) in the ledger.
 * 
 * ============================================================================
 */

public class BullsAndCowsStudy {

    /**
     * Common interface to test different implementations uniformly.
     */
    interface BullsAndCowsSolver {
        String getHint(String secret, String guess);
    }

    // ========================================================================
    // APPROACH 3: SHARED LEDGER (The Optimal & Elegant Solution)
    // ========================================================================
    static class SinglePassSolver implements BullsAndCowsSolver {
        @Override
        public String getHint(String secret, String guess) {
            int bulls = 0;
            int cows = 0;
            
            // Only size 10 because constraints guarantee '0' through '9'
            int[] ledger = new int[10];
            
            for (int i = 0; i < secret.length(); i++) {
                int s = secret.charAt(i) - '0';
                int g = guess.charAt(i) - '0';
                
                if (s == g) {
                    // Exact match in value and position.
                    bulls++;
                } else {
                    // Not a match. Check if they resolve past debts/surpluses.
                    
                    // If the secret digit 's' was previously demanded by the guess (< 0),
                    // this supply resolves that demand. We found a Cow!
                    if (ledger[s] < 0) {
                        cows++;
                    }
                    
                    // If the guess digit 'g' was previously supplied by the secret (> 0),
                    // this demand consumes that supply. We found a Cow!
                    if (ledger[g] > 0) {
                        cows++;
                    }
                    
                    // Update the ledger: secret provides supply (+1), guess creates demand (-1)
                    ledger[s]++;
                    ledger[g]--;
                }
            }
            
            return bulls + "A" + cows + "B";
        }
    }

    // ========================================================================
    // APPROACH 2: TWO PASSES / TWO ARRAYS (The Standard, Intuitive Approach)
    // ========================================================================
    static class TwoPassSolver implements BullsAndCowsSolver {
        @Override
        public String getHint(String secret, String guess) {
            int bulls = 0;
            int cows = 0;
            
            int[] secretFreq = new int[10];
            int[] guessFreq = new int[10];
            
            // Pass 1: Count Bulls and tally up the remaining mismatched digits
            for (int i = 0; i < secret.length(); i++) {
                if (secret.charAt(i) == guess.charAt(i)) {
                    bulls++;
                } else {
                    secretFreq[secret.charAt(i) - '0']++;
                    guessFreq[guess.charAt(i) - '0']++;
                }
            }
            
            // Pass 2: Calculate Cows by finding the overlap (intersection) of frequencies
            for (int i = 0; i < 10; i++) {
                // If secret has two '7's remaining, and guess wants three '7's,
                // we can only form exactly 2 Cows. We take the minimum.
                cows += Math.min(secretFreq[i], guessFreq[i]);
            }
            
            return bulls + "A" + cows + "B";
        }
    }


    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        BullsAndCowsSolver[] solvers = {
            new SinglePassSolver(),
            new TwoPassSolver()
        };

        // Format: {secret, guess, expected_output}
        String[][] testCases = {
            {"1807", "7810", "1A3B"},      // Standard case
            {"1123", "0111", "1A1B"},      // Duplicate handling
            {"1234", "1234", "4A0B"},      // All bulls
            {"1234", "4321", "0A4B"},      // All cows
            {"0000", "1111", "0A0B"},      // Complete mismatch
            {"1122", "2211", "0A4B"}       // Paired swaps
        };

        for (BullsAndCowsSolver solver : solvers) {
            System.out.println("Testing " + solver.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (int i = 0; i < testCases.length; i++) {
                String secret = testCases[i][0];
                String guess = testCases[i][1];
                String expected = testCases[i][2];
                
                String result = solver.getHint(secret, guess);
                
                if (!result.equals(expected)) {
                    System.out.printf("  [FAIL] secret='%s', guess='%s'. Expected: %s, Got: %s%n", 
                        secret, guess, expected, result);
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
 * - Core pattern: Multi-set intersection (Counting overlapping frequencies).
 * - Key observation: Bulls MUST be processed first (or excluded from frequency arrays) 
 *   because an exact positional match removes those characters from the Cow pool.
 * - What's worth memorizing: The Shared Ledger trick (`ledger[s]++; ledger[g]--;`). 
 *   It drops the space complexity of counting overlapping arrays in half and allows 
 *   single-pass resolution.
 * - Most common trap: Including matched characters (Bulls) in the frequency maps. 
 *   This will cause a digit to be counted as both a Bull and a Cow!
 * - Mental trigger: "Finding misplaced items? Supply and demand ledger."
 * ============================================================================
 */


import java.util.*;

/**
 * Bulls and Cows Problem (HashMap Approach - 2 Pass)
 *
 * Goal:
 * Return a string in format "xAyB"
 *   x = Bulls  → correct digit at correct position
 *   y = Cows   → correct digit but wrong position
 *
 * ---------------------------------------------------------
 * 🧠 Approach (2 Pass Strategy)
 * ---------------------------------------------------------
 *
 * Pass 1:
 *   - Count Bulls directly (same index, same digit)
 *   - For non-bull positions:
 *       store frequency of digits from SECRET in a HashMap
 *
 * Pass 2:
 *   - Iterate again over non-bull positions
 *   - For each digit in GUESS:
 *       check if it exists in the map (i.e., exists in secret unmatched digits)
 *       → if yes, it's a COW
 *       → reduce frequency to avoid double counting
 *
 * ---------------------------------------------------------
 * ⚠️ Important:
 * ---------------------------------------------------------
 * We only track unmatched digits from SECRET.
 * This ensures:
 *   - No double counting
 *   - Correct handling of duplicates
 *
 * ---------------------------------------------------------
 * ⏱ Complexity:
 * ---------------------------------------------------------
 * Time  : O(n)
 * Space : O(1) → at most 10 digits (0–9), even though using HashMap
 *
 * ---------------------------------------------------------
 * 🧠 Interview Insight:
 * ---------------------------------------------------------
 * This is a clean and intuitive approach.
 * Then you can optimize to 1-pass using an array (follow-up optimization).
 */
public class Solution {

    public static String getHint(String secret, String guess) {

        // Map to store frequency of unmatched digits from secret
        Map<Character, Integer> cows = new HashMap<>();

        int n = secret.length();
        int bullsCount = 0;

        // -------------------------------------------------
        // Pass 1: Count Bulls + build frequency map
        // -------------------------------------------------
        for (int i = 0; i < n; i++) {

            char s = secret.charAt(i);
            char g = guess.charAt(i);

            if (s == g) {
                // Exact match → Bull
                bullsCount++;
            } else {
                // Store only unmatched secret digits
                cows.merge(s, 1, Integer::sum);
            }
        }

        int cowsCount = 0;

        // -------------------------------------------------
        // Pass 2: Count Cows using the frequency map
        // -------------------------------------------------
        for (int i = 0; i < n; i++) {

            // Skip already counted bulls
            if (secret.charAt(i) == guess.charAt(i)) continue;

            char g = guess.charAt(i);

            /**
             * If this digit exists in the map,
             * it means this digit was present in secret (but at different position)
             * → so it's a COW
             */
            if (cows.getOrDefault(g, 0) > 0) {

                cowsCount++;

                // Reduce frequency since we've used this occurrence
                cows.put(g, cows.get(g) - 1);

                // Clean up map when count becomes zero
                if (cows.get(g) == 0) {
                    cows.remove(g);
                }
            }
        }

        // Format the result
        return bullsCount + "A" + cowsCount + "B";
    }
}



import java.util.*;

/**
 * Bulls and Cows Problem
 *
 * Goal:
 * Return hint in format "xAyB"
 *  - x = Bulls  (correct digit + correct position)
 *  - y = Cows   (correct digit but wrong position)
 *
 * Key Challenge:
 * Handle duplicates WITHOUT over-counting cows.
 *
 * ---------------------------------------------------------
 * 🔥 Optimal Approach: One Pass + Frequency Balance Array
 * ---------------------------------------------------------
 *
 * Core Idea:
 * We use an int[10] array to track the "balance" of digits.
 *
 * Interpretation of freq array:
 *   freq[d] > 0 → extra occurrences of digit 'd' from SECRET
 *   freq[d] < 0 → extra occurrences of digit 'd' from GUESS
 *
 * When we see a mismatch:
 *   1. If freq[s] < 0 → this digit was previously seen in guess → forms a cow
 *   2. If freq[g] > 0 → this digit was previously seen in secret → forms a cow
 *
 * Then update:
 *   freq[s]++  (add current secret digit)
 *   freq[g]--  (add current guess digit)
 *
 * ---------------------------------------------------------
 * 🧠 Intuition (Interview Explanation)
 * ---------------------------------------------------------
 *
 * Instead of:
 *   - storing unmatched digits
 *   - then matching later (2-pass)
 *
 * We:
 *   - match on the fly (1-pass)
 *   - maintain surplus/deficit
 *
 * This avoids:
 *   - extra space
 *   - extra loops
 *
 * ---------------------------------------------------------
 * ⏱ Complexity:
 * ---------------------------------------------------------
 * Time  : O(n)
 * Space : O(1) (only 10 digits)
 *
 */
class Solution {

    public String getHint(String secret, String guess) {

        int bulls = 0; // exact matches
        int cows = 0;  // correct digits but wrong position

        // Since digits are from 0-9, we use fixed size array instead of HashMap
        int[] freq = new int[10];

        for (int i = 0; i < secret.length(); i++) {

            int s = secret.charAt(i) - '0'; // current digit in secret
            int g = guess.charAt(i) - '0';  // current digit in guess

            // Case 1: Exact match → Bull
            if (s == g) {
                bulls++;
            } else {

                /**
                 * Case 2: Not a bull → check for possible cows
                 */

                // If freq[s] < 0 → this digit was seen earlier in guess
                // So now we found a matching pair → cow
                if (freq[s] < 0) {
                    cows++;
                }

                // If freq[g] > 0 → this digit was seen earlier in secret
                // So now we found a matching pair → cow
                if (freq[g] > 0) {
                    cows++;
                }

                // Update frequency balance
                freq[s]++; // add this digit from secret
                freq[g]--; // subtract this digit from guess
            }
        }

        // Format result
        return bulls + "A" + cows + "B";
    }
}


import java.util.*;

/**
 * Bulls and Cows Problem (Optimal One-Pass Solution)
 *
 * Goal:
 * Return a hint in the format "xAyB"
 *   - x = Bulls  → correct digit at correct position
 *   - y = Cows   → correct digit but wrong position
 *
 * ---------------------------------------------------------
 * 🔥 Key Idea: Frequency Balance Array (One Pass)
 * ---------------------------------------------------------
 *
 * We use an int[10] array (since digits are 0–9) to track balance.
 *
 * Meaning of count array:
 *   count[d] > 0 → extra occurrences of digit 'd' from SECRET
 *   count[d] < 0 → extra occurrences of digit 'd' from GUESS
 *
 * ---------------------------------------------------------
 * 🧠 How It Works
 * ---------------------------------------------------------
 *
 * For each index i:
 *
 * Case 1: secret[i] == guess[i]
 *   → This is a BULL (correct position)
 *
 * Case 2: secret[i] != guess[i]
 *   → Possible cow situation
 *
 *   We check:
 *
 *   1. If count[s] < 0
 *      → This digit 's' was previously seen in guess (unmatched)
 *      → Now we found a match → COW++
 *
 *   2. If count[g] > 0
 *      → This digit 'g' was previously seen in secret (unmatched)
 *      → Now we found a match → COW++
 *
 *   Then update balance:
 *      count[s]++  (add current secret digit)
 *      count[g]--  (add current guess digit)
 *
 * ---------------------------------------------------------
 * 🧠 Intuition (Very Important for Interviews)
 * ---------------------------------------------------------
 *
 * Instead of:
 *   - storing all unmatched digits
 *   - then matching later (2-pass)
 *
 * We:
 *   - match dynamically while iterating (1-pass)
 *   - maintain surplus/deficit of digits
 *
 * This avoids:
 *   - extra space
 *   - extra iteration
 *
 * ---------------------------------------------------------
 * ⏱ Complexity:
 * ---------------------------------------------------------
 * Time  : O(n)
 * Space : O(1) → fixed size (10 digits)
 *
 * ---------------------------------------------------------
 * 🧪 Example:
 * ---------------------------------------------------------
 * secret = "1807"
 * guess  = "7810"
 *
 * Bulls = 1 (digit '8')
 * Cows  = 3 (1,0,7)
 *
 * Output: "1A3B"
 */
class Solution {

    public String getHint(String secret, String guess) {

        int bulls = 0; // exact matches
        int cows = 0;  // correct digits but wrong positions

        // Balance array to track surplus/deficit of digits
        int[] count = new int[10];

        for (int i = 0; i < secret.length(); i++) {

            int s = secret.charAt(i) - '0'; // current digit from secret
            int g = guess.charAt(i) - '0';  // current digit from guess

            // Case 1: Exact match → Bull
            if (s == g) {
                bulls++;
            } else {

                /**
                 * Case 2: Mismatch → check for cows
                 */

                // If count[s] < 0:
                // This digit 's' was previously seen in guess (unmatched)
                // → Now we found its pair → it's a cow
                if (count[s] < 0) {
                    cows++;
                }

                // If count[g] > 0:
                // This digit 'g' was previously seen in secret (unmatched)
                // → Now we found its pair → it's a cow
                if (count[g] > 0) {
                    cows++;
                }

                // Update balance after checking
                count[s]++; // record this digit from secret
                count[g]--; // record this digit from guess
            }
        }

        // Return formatted result
        return bulls + "A" + cows + "B";
    }
}
