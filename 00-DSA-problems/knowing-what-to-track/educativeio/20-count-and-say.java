/**
 * STANDALONE STUDY NOTE: COUNT AND SAY
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "What is the maximum value of n?" (Constraints state 1 <= n <= 30, meaning the sequence length grows 
 *   exponentially but stays well within manageable memory limits).
 * - "What is the base case for n = 1?" (The sequence explicitly starts with "1").
 * - "How does run-length encoding format the output?" (Count of characters followed immediately by the character itself, 
 *   e.g., three '1's becomes "31").
 * - "Should we use recursion or iterative simulation?" (Iterative simulation is preferred to avoid deep recursion stacks).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to generate the n-th string in a sequence where each term is the run-length encoding (RLE) of the previous term. 
 * The binding constraint is that string lengths grow exponentially with n, so our string manipulation must be efficient 
 * (using StringBuilder) to avoid severe garbage collection overhead.
 * 
 * --- APPROACH 1: RECURSIVE GENERATION (Pure DFS) ---
 * 1. What I'd naturally try: Write a recursive function `countAndSay(n)` that calls `countAndSay(n - 1)`, 
 *    then encodes the returned string.
 * 2. Why it works: It mirrors the mathematical definition of the sequence naturally.
 * 3. Why it's less optimal: It adds recursion stack overhead and relies on deep function calls when an iterative loop 
 *    is cleaner and safer.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(2^N) — because string lengths roughly double every few iterations, leading to exponential work.
 *    - Space Complexity: O(2^N) — for the recursion stack and storing intermediate strings.
 * 
 * --- APPROACH 2: OPTIMAL (Iterative Simulation with StringBuilder) ---
 * 1. The Observation: We can start with `"1"` at n = 1, and iteratively build up to n using a loop. 
 *    For each step, we scan the current string with two pointers or a streak counter, group identical consecutive characters, 
 *    and append `count` + `character` into a `StringBuilder`.
 * 2. How it leads to the next step: Loop from step 2 up to n. In each iteration, transform the previous string into the new string.
 * 3. Which one I'd write in an interview: Approach 2 (Iterative StringBuilder Simulation). It is clean, avoids recursion 
 *    stack overflow risks, and efficiently manages string concatenation.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(2^N) — bounded by the exponential growth of the string length up to n = 30.
 *    - Space Complexity: O(L) — where L is the maximum length of the resulting string at step n.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - n = 1: Returns `"1"` immediately without entering the loop.
 * - n = 2: Encodes `"1"` into `"11"` (one '1').
 * - n = 4: `"1"` -> `"11"` -> `"21"` -> `"1211"`.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Run-Length Encoding (RLE) Scanning:
 * 
 * Current String: "3322251"
 * 
 * Scan & Count Streaks:
 * - '3' appears 2 times -> Append "2" + "3"
 * - '2' appears 3 times -> Append "3" + "2"
 * - '5' appears 1 time  -> Append "1" + "5"
 * - '1' appears 1 time  -> Append "1" + "1"
 * 
 * Next String: "23321511"
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: n = 4
 * 
 * - Step 1: res = "1" (for n = 1)
 * - Step 2 (i = 2): Transform "1"
 *   - Current char '1' count = 1. End of string. Append "11".
 *   - res = "11"
 * - Step 3 (i = 3): Transform "11"
 *   - Current char '1' count = 2. Append "21".
 *   - res = "21"
 * - Step 4 (i = 4): Transform "21"
 *   - Char '1' count = 1 -> Append "11"
 *   - Char '2' count = 1 -> Append "21"
 *   - res = "1211"
 * 
 * Final Result for n = 4: "1211"
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "To find the n-th term in the count-and-say sequence, we can simulate the process iteratively from step 1 up to n. 
 * We start with the base string '1'. In each iteration, we perform run-length encoding on the current string by scanning 
 * characters, counting consecutive identical streaks, and appending the count followed by the character into a StringBuilder. 
 * Since the string length grows up to n = 30, iterative simulation runs extremely fast and avoids recursion overhead. 
 * This gives us O(2^N) time complexity due to sequence growth and O(L) space complexity."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if n was much larger, say n = 100?
 * A: The string length would grow beyond available memory (exponential growth), making direct simulation impossible. 
 *    We would need mathematical formula approximations or compressed matrix representations if such constraints were possible.
 */

public class CountAndSay {

    /**
     * APPROACH 2: OPTIMAL (Iterative Simulation with StringBuilder)
     * Time: O(2^N), Space: O(L)
     */
    public String countAndSay(int n) {
        // [Why needed]: Base case for the sequence starting point.
        String current = "1";

        // Iteratively build the sequence from step 2 up to n
        for (int i = 2; i <= n; i++) {
            StringBuilder nextString = new StringBuilder();
            int length = current.length();
            
            // Scan the current string to perform run-length encoding
            int j = 0;
            while (j < length) {
                char c = current.charAt(j);
                int count = 0;

                // Count consecutive identical characters
                while (j < length && current.charAt(j) == c) {
                    count++;
                    j++;
                }

                // [What it does]: Append count followed by the character to our RLE builder
                nextString.append(count).append(c);
            }

            // Move to the newly generated string for the next iteration
            current = nextString.toString();
        }

        return current;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Iterative string transformation via Run-Length Encoding (RLE).
     * 2. Key observation: Each term is generated directly from the previous term; an iterative loop with StringBuilder 
     *    prevents deep recursion overhead.
     * 3. Memorize vs Understand: Understand the inner counting loop (`while (j < length && current.charAt(j) == c)`).
     * 4. Most common trap: Resetting or mismanaging the pointer `j` when tracking character streaks.
     * 5. Mental trigger: "Count and say sequence" -> Iterative loop + RLE streak counting + StringBuilder.
     */
    public static void main(String[] args) {
        CountAndSay solution = new CountAndSay();

        // Test Case 1: n = 1
        System.out.println("Test 1 (n = 1): " + solution.countAndSay(1)); // Expected: "1"

        // Test Case 2: n = 4
        System.out.println("Test 2 (n = 4): " + solution.countAndSay(4)); // Expected: "1211"

        // Test Case 3: n = 5
        System.out.println("Test 3 (n = 5): " + solution.countAndSay(5)); // Expected: "111221"
    }
}



public class Solution {

    public String countAndSay(int n) {

        // Base case: the sequence starts with "1".
        StringBuilder s = new StringBuilder("1");

        // Generate the next term n - 1 times.
        for (int i = 1; i < n; i++) {
            s = getNext(s);
        }

        return s.toString();
    }

    private StringBuilder getNext(StringBuilder s) {

        StringBuilder newS = new StringBuilder();

        // Defensive check: avoids charAt(0) on an empty string.
        if (s.isEmpty()) {
            return newS;
        }

        // Start counting the first group.
        int count = 1;
        char ch = s.charAt(0);

        // Scan from the second character.
        for (int i = 1; i < s.length(); i++) {
            char curr = s.charAt(i);

            if (ch == curr) {
                // Same group: increase its count.
                count++;
            } else {
                // Group ended: append count followed by its character.
                newS.append(count).append(ch);

                // Start a new group with the current character.
                count = 1;
                ch = curr;
            }
        }

        // Don't forget the final group, which has no following character
        // to trigger the else branch.
        newS.append(count).append(ch);

        return newS;
    }
}



public class Solution {

    public String countAndSay(int n) {

        // The first term of the sequence is always "1".
        StringBuilder s = new StringBuilder("1");

        // Generate each subsequent term from the previous term.
        for (int i = 1; i < n; i++) {
            s = getNext(s);
        }

        return s.toString();
    }

    private StringBuilder getNext(StringBuilder s) {

        StringBuilder result = new StringBuilder();
        int i = 0;

        // Process the input string one group of identical characters at a time.
        while (i < s.length()) {

            char ch = s.charAt(i);

            // Remember where this group starts.
            int start = i;

            // Skip all consecutive occurrences of the same character.
            // When this loop ends, i points to the next group or the end.
            while (i < s.length() && s.charAt(i) == ch) {
                i++;
            }

            // The group length is the number of characters we skipped.
            int count = i - start;

            // Describe this group: append its count followed by its character.
            result.append(count).append(ch);
        }

        return result;
    }


    private StringBuilder getNext2(StringBuilder s) {

        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < s.length()) {

            // Identify the character whose consecutive occurrences we will count.
            char ch = s.charAt(i);

            // Every group contains at least the current character.
            int count = 0;

            // Count consecutive occurrences of ch.
            while (i < s.length() && s.charAt(i) == ch) {
                count++;
                i++;
            }

            // Describe the group: count followed by the character.
            result.append(count).append(ch);
        }

        return result;
    }

}

