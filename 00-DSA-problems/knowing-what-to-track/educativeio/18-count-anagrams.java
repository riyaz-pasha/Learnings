/**
 * STANDALONE STUDY NOTE: COUNT ANAGRAMS (LEETCODE 2514)
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "What is the definition of an anagram for a multi-word string?" (The problem statement explicitly specifies 
 *   that a string t is an anagram of s if the i-th word of t is a permutation of the i-th word of s).
 * - "Are words separated by a single space?" (Yes, constraints guarantee single spaces between consecutive words).
 * - "Should the final answer be modulo 10^9 + 7?" (Yes, to prevent integer overflow since the result can be massive).
 * - "What is the maximum length of the string s?" (Up to 10^3 characters, meaning words are relatively short, 
 *   allowing factorial and modular inverse calculations).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to find the total number of distinct sentence anagrams, where each word's internal characters can be 
 * rearranged in any valid distinct way, and each word's permutation choice is independent of the others. 
 * The binding constraint is that generating all actual sentence permutations is combinatorially impossible, 
 * so we must use combinatorics and modular arithmetic.
 * 
 * --- APPROACH 1: BRUTE FORCE (Generate All Permutations) ---
 * 1. What I'd naturally try: Generate every possible character permutation of the entire sentence, check if each 
 *    word matches the multiset of characters of the corresponding original word, and keep a distinct count.
 * 2. Why it works: It exhaustively examines every permutation.
 * 3. Why it's too slow/costly: The number of permutations of a 1000-character string is astronomical (N!), 
 *    making it completely intractable.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N!) — factorial time explosion.
 *    - Space Complexity: O(N) — recursion depth and string storage.
 * 
 * --- APPROACH 2: INTERMEDIATE (Independent Word Permutations without Modulo Optimization) ---
 * 1. The Observation: Because the i-th word of the anagram must be a permutation of the i-th word of `s`, 
 *    the choice of permutation for each word is completely independent. By the fundamental counting principle 
 *    (multiplication rule), the total number of distinct anagrams for the sentence is the product of the number 
 *    of distinct permutations for each individual word.
 * 2. How it leads to the next step: Split the sentence by spaces into individual words. For each word of length L 
 *    with character frequencies c_1, c_2, ..., the number of distinct permutations is given by the multinomial coefficient formula:
 *    L! / (c_1! * c_2! * ... * c_k!)
 *    Multiply these values together for all words.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N * L) — where N is the number of words and L is max word length, computing factorials.
 *    - Space Complexity: O(L) — frequency arrays per word.
 * 
 * --- APPROACH 3: OPTIMAL (Multinomial Coefficients + Modular Inverse via Fermat's Little Theorem) ---
 * 1. The Observation: Since the result must be returned modulo 10^9 + 7 (a prime number), division by factorials 
 *    (c_1!, c_2!, etc.) must be handled using modular multiplicative inverses. By Fermat's Little Theorem, 
 *    a^(MOD - 2) % MOD is the modular inverse of a modulo MOD.
 * 2. How it leads to the next step: 
 *    - Precompute factorials up to the maximum word length to ensure O(1) factorial lookups.
 *    - Split `s` by spaces into words.
 *    - For each word, compute `L! * inverse(c_1!) * inverse(c_2!) ... % MOD`.
 *    - Multiply each word's distinct permutation count into a running total modulo 10^9 + 7.
 * 3. Which one I'd write in an interview: Approach 3. It combines fundamental combinatorics, modular arithmetic, 
 *    and Fermat's Little Theorem to solve a seemingly massive counting problem in linear O(N) time with O(1) auxiliary space.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N + M log MOD) — where N is the total length of string `s` and M is max word length 
 *      (for modular exponentiation during inverse calculation).
 *    - Space Complexity: O(M) — to store precomputed factorials up to max word length.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Single word with all unique characters: `s = "abc"` -> 3! = 6 permutations.
 * - Single word with duplicate characters: `s = "aab"` -> 3! / (2! * 1!) = 3 permutations ("aab", "aba", "baa").
 * - Multiple words: `s = "listen silent"` -> (6!) / (...) * (6!) / (...) computed independently and multiplied.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Independent Word Permutation Principle:
 * 
 * Sentence: "listen silent"
 * Word 1: "listen" -> Length 6, frequencies: l:1, i:1, s:1, t:1, e:1, n:1 -> 6! / (1!1!1!1!1!1!) = 720 ways.
 * Word 2: "silent" -> Length 6, frequencies: s:1, i:1, l:1, e:1, n:1, t:1 -> 6! / (1!1!1!1!1!1!) = 720 ways.
 * 
 * Total Sentence Anagrams = 720 * 720 = 518,400 (modulo 10^9 + 7).
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: s = "aab" (Single word case for simplicity)
 * 
 * - Word length L = 3.
 * - Frequencies: 'a' = 2, 'b' = 1.
 * - Formula: 3! / (2! * 1!) = 6 / (2 * 1) = 3.
 * - Result: 3 distinct anagrams ("aab", "aba", "baa").
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "The problem states that a sentence anagram requires each i-th word to be a valid permutation of the i-th word 
 * in the original string. Because each word can be rearranged independently of the others, the total number of 
 * distinct sentence anagrams is simply the product of the number of distinct permutations for each individual word. 
 * For any given word, the number of distinct permutations is given by the multinomial coefficient: the factorial of the 
 * word length divided by the product of the factorials of each character's frequency. Since the answer can be large, 
 * we compute everything modulo 10^9 + 7 using modular inverse via Fermat's Little Theorem for division. This runs 
 * in O(N) time and O(L) space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if words could be anagrams of *each other* across positions (e.g. word 1 and word 2 swapping places)?
 * A: That would introduce multinomial coefficients across identical words in the sentence (like counting distinct 
 *    arrangements of words themselves), requiring an extra division by the factorial of the frequency of each identical word.
 * 
 * Q: How does Fermat's Little Theorem work for modular division?
 * A: Since MOD is a prime number (10^9 + 7), by Fermat's Little Theorem, `a^(MOD - 2) % MOD` gives the modular 
 *    multiplicative inverse of `a`, allowing us to compute `(A / B) % MOD` as `(A * inverse(B)) % MOD`.
 */

public class CountAnagrams {
    private static final int MOD = 1_000_000_007;

    /**
     * APPROACH 3: OPTIMAL (Multinomial Coefficients + Modular Inverse)
     * Time: O(N + M log MOD), Space: O(M) where M is max word length
     */
    public int countAnagrams(String s) {
        String[] words = s.split(" ");
        long totalWays = 1;

        // Find the maximum word length to precompute factorials up to that size
        int maxLen = 0;
        for (String word : words) {
            maxLen = Math.max(maxLen, word.length());
        }

        // Precompute factorials up to maxLen for O(1) lookup
        long[] fact = new long[maxLen + 1];
        fact[0] = 1;
        for (int i = 1; i <= maxLen; i++) {
            fact[i] = (fact[i - 1] * i) % MOD;
        }

        // Process each word independently
        for (String word : words) {
            int L = word.length();
            int[] charCount = new int[26];
            
            for (int i = 0; i < L; i++) {
                charCount[word.charAt(i) - 'a']++;
            }

            // [What it does]: Number of distinct permutations for this word = L! / (c_1! * c_2! * ... * c_26!)
            long wordWays = fact[L];

            for (int count : charCount) {
                if (count > 1) {
                    // Divide by count! using modular inverse
                    wordWays = (wordWays * power(fact[count], MOD - 2)) % MOD;
                }
            }

            // Multiply independent word permutations into the running total modulo MOD
            totalWays = (totalWays * wordWays) % MOD;
        }

        return (int) totalWays;
    }

    /**
     * Helper method for modular exponentiation: computes (base^exp) % MOD
     * Time: O(log exp), Space: O(1)
     */
    private long power(long base, long exp) {
        long res = 1;
        base %= MOD;
        while (exp > 0) {
            if ((exp & 1) == 1) {
                res = (res * base) % MOD;
            }
            base = (base * base) % MOD;
            exp >>= 1;
        }
        return res;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Independent word permutations via Multinomial Coefficients + Modular Inverse.
     * 2. Key observation: Each word's permutation choices are completely independent; multiply individual word ways together.
     * 3. Memorize vs Understand: Understand how Fermat's Little Theorem (`power(val, MOD - 2)`) enables modular division.
     * 4. Most common trap: Forgetting to apply modulo at each multiplication step, leading to long overflow.
     * 5. Mental trigger: "Count distinct anagrams of multi-word string modulo 10^9 + 7" -> Word splitting + Multinomial factorials + Fermat inverse.
     */
    public static void main(String[] args) {
        CountAnagrams solution = new CountAnagrams();

        // Test Case 1: Standard Example
        String s1 = "too hot";
        // "too" -> 3! / 2! = 3 ways ("too", "oto", "oot")
        // "hot" -> 3! / 1! = 6 ways
        // Total = 3 * 6 = 18
        System.out.println("Test 1 (too hot): " + solution.countAnagrams(s1)); // Expected: 18

        // Test Case 2: Multi-word sentence
        String s2 = "aa a";
        // "aa" -> 2! / 2! = 1 way
        // "a" -> 1 way
        // Total = 1 * 1 = 1
        System.out.println("Test 2 (aa a): " + solution.countAnagrams(s2)); // Expected: 1
    }
}

