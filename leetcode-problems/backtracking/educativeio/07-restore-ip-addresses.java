/**
 * ============================================================================
 * RESTORE IP ADDRESSES - COMPREHENSIVE GUIDE & SOLUTIONS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * We are given a string of continuous numbers (e.g., "25525511135"). We need 
 * to act as a parser that inserts exactly three dots ('.') into this string to 
 * form a valid IPv4 address (e.g., "255.255.11.135"). 
 * A valid IP address has 4 parts, and each part must:
 *   - Be between 0 and 255.
 *   - Not contain leading zeros (e.g., "01" is bad, but "0" alone is good).
 *   - Be 1 to 3 digits long.
 * We must find and return ALL possible valid IP addresses we can form.
 * 
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: What is the minimum and maximum length of the input string?
 * A: Since an IP has 4 parts, and each part is at least 1 digit and at most 3 
 *    digits, the string MUST be between 4 and 12 characters long. Anything 
 *    outside this range can be instantly rejected.
 * 
 * Q: Can the input contain letters or special characters?
 * A: The constraints specify the string consists of digits only.
 * 
 * Q: Does the order of the returned IP addresses matter?
 * A: No, any order is acceptable.
 * 
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - COMBINATORIAL SEARCH: We are placing dividers (dots) into a string. This 
 *   is a classic "combinations" problem, making Depth-First Search (DFS) with 
 *   Backtracking the most natural fit.
 * - FIXED DEPTH (MAGIC NUMBER 4): Because an IPv4 address ALWAYS has exactly 4 
 *   segments, the depth of our recursion will never exceed 4. This fixed depth 
 *   also means we can solve it without recursion using simple nested loops!
 * - VALIDATION IS KING: The core of the problem is writing a robust `isValid` 
 *   function to strictly enforce the 0-255 range and the "no leading zero" rule.
 * 
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Immediately mention the string length bounds (4 to 12). Add an 
 *   early exit `if (s.length() < 4 || s.length() > 12) return empty;`. 
 *   Interviewers love seeing boundary checks up front.
 * - Step 2: Propose the Backtracking (DFS) approach first. It demonstrates your 
 *   ability to handle generalized combinatorial problems.
 * - Step 3: When writing the helper `isValid`, point out that you only parse to 
 *   integer AFTER checking length and leading zeros to avoid exceptions.
 * - Step 4: After writing Backtracking, mention: "Since an IP address strictly 
 *   has 4 parts, I could actually solve this with 3 nested loops in O(1) time."
 *   This highlights your analytical thinking.
 * 
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * String: "25525511135"
 * 
 * Decision Tree (DFS):
 * Part 1 choices: 
 *   "2" -> recurse for remaining "5525511135" (needs 3 parts, length 10 -> Impossible, max length for 3 parts is 9. Prune!)
 *   "25" -> recurse (needs 3 parts, length 9 -> valid length to continue).
 *   "255" -> recurse (needs 3 parts, length 8 -> valid).
 *      Part 2 choices from "255":
 *         "2" (valid)
 *         "25" (valid)
 *         "255" (valid) -> string left: "11135"
 *            Part 3 choices from "255":
 *               "1" (valid) -> string left: "1135" (Too long for Part 4. Prune!)
 *               "11" (valid) -> string left: "135" -> Part 4 is "135". VALID IP: "255.255.11.135"
 *               "111" (valid) -> string left: "35" -> Part 4 is "35". VALID IP: "255.255.111.35"
 */

import java.util.*;

public class RestoreIPAddresses {

    /**
     * SOLUTION 1: Standard Backtracking (DFS)
     * ------------------------------------------------------------------------
     * Pros: Extensible. If IPv6 was requested (8 segments), this approach 
     * easily scales just by changing the segment limit from 4 to 8.
     * Cons: Slightly more overhead due to recursion and list manipulation.
     * 
     * Time Complexity: O(1) or O(3^4). We place at most 3 dots, and each dot 
     * has a max of 3 positions. Since the depth is capped at 4 and string length 
     * capped at 12, the time complexity is strictly bounded by a small constant.
     * Space Complexity: O(1). The recursion depth is at most 4.
     */
    public List<String> restoreIpAddressesDFS(String s) {
        List<String> result = new ArrayList<>();
        // Early exit for impossible lengths
        if (s.length() < 4 || s.length() > 12) {
            return result;
        }
        
        backtrack(s, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrack(String s, int startIndex, List<String> currentSegments, List<String> result) {
        // Base case: If we have exactly 4 segments
        if (currentSegments.size() == 4) {
            // And we've consumed the entire string
            if (startIndex == s.length()) {
                // String.join is an elegant modern Java way to combine strings with a delimiter
                result.add(String.join(".", currentSegments));
            }
            return;
        }

        // Pruning optimization: 
        // If the remaining characters are more than what the remaining segments can hold
        // (each segment can hold max 3 characters), we stop exploring this path.
        int remainingChars = s.length() - startIndex;
        int remainingSegments = 4 - currentSegments.size();
        if (remainingChars > remainingSegments * 3) {
            return;
        }

        // Explore picking 1, 2, or 3 characters for the current segment
        for (int len = 1; len <= 3; len++) {
            if (startIndex + len > s.length()) break; // Don't go out of bounds
            
            String segment = s.substring(startIndex, startIndex + len);
            
            if (isValid(segment)) {
                currentSegments.add(segment);                       // Choose
                backtrack(s, startIndex + len, currentSegments, result); // Explore
                currentSegments.remove(currentSegments.size() - 1); // Un-choose (Backtrack)
            }
        }
    }

    /**
     * SOLUTION 2: Iterative 3-Nested Loops
     * ------------------------------------------------------------------------
     * Pros: Incredibly fast, zero recursion overhead, very straightforward 
     * for a fixed-depth problem.
     * Cons: Hardcoded to 4 segments (IPv4). Not dynamically extensible.
     * 
     * Time Complexity: O(1). Outer loops run max 3 times each. (3 * 3 * 3 = 27 iterations max).
     * Space Complexity: O(1). No auxiliary stack space used.
     */
    public List<String> restoreIpAddressesIterative(String s) {
        List<String> result = new ArrayList<>();
        int n = s.length();
        
        if (n < 4 || n > 12) return result;

        // Loop 1 places the first dot (i is the length of the first segment)
        for (int i = 1; i <= 3 && i < n - 2; i++) {
            // Loop 2 places the second dot (j is the length of the second segment)
            for (int j = 1; j <= 3 && i + j < n - 1; j++) {
                // Loop 3 places the third dot (k is the length of the third segment)
                for (int k = 1; k <= 3 && i + j + k < n; k++) {
                    
                    int l = n - i - j - k; // l is the length of the fourth segment
                    if (l > 3) continue;   // Segment 4 cannot be larger than 3 chars

                    String s1 = s.substring(0, i);
                    String s2 = s.substring(i, i + j);
                    String s3 = s.substring(i + j, i + j + k);
                    String s4 = s.substring(i + j + k, n);

                    if (isValid(s1) && isValid(s2) && isValid(s3) && isValid(s4)) {
                        // StringBuilder is more efficient than s1 + "." + s2... in loops
                        StringBuilder sb = new StringBuilder();
                        sb.append(s1).append('.').append(s2).append('.')
                          .append(s3).append('.').append(s4);
                        result.add(sb.toString());
                    }
                }
            }
        }
        return result;
    }

    /**
     * HELPER: Validates if a string segment is a proper IPv4 block.
     */
    private boolean isValid(String segment) {
        int len = segment.length();
        // Segment length must be between 1 and 3
        if (len == 0 || len > 3) return false;
        
        // No leading zeros allowed unless the segment is exactly "0"
        if (len > 1 && segment.charAt(0) == '0') return false;
        
        // Value must be between 0 and 255
        // Safe to use Integer.parseInt because we already constrained length to max 3
        int val = Integer.parseInt(segment);
        return val >= 0 && val <= 255;
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        RestoreIPAddresses solver = new RestoreIPAddresses();

        String test1 = "25525511135";
        String test2 = "0000";
        String test3 = "101023";

        System.out.println("--- Test Case 1: " + test1 + " ---");
        System.out.println("DFS Solution:       " + solver.restoreIpAddressesDFS(test1));
        System.out.println("Iterative Solution: " + solver.restoreIpAddressesIterative(test1));
        System.out.println();

        System.out.println("--- Test Case 2 (Edge Case: Zeros): " + test2 + " ---");
        System.out.println("DFS Solution:       " + solver.restoreIpAddressesDFS(test2));
        System.out.println("Iterative Solution: " + solver.restoreIpAddressesIterative(test2));
        System.out.println();

        System.out.println("--- Test Case 3: " + test3 + " ---");
        System.out.println("DFS Solution:       " + solver.restoreIpAddressesDFS(test3));
        System.out.println("Iterative Solution: " + solver.restoreIpAddressesIterative(test3));
    }
}

/**
 * ============================================================
 * 🔥 Restore IP Addresses — Backtracking Master Solution
 * ============================================================
 *
 * IDEA:
 * Build 4 segments. At each step:
 * - Take 1 to 3 digits
 * - Validate segment
 * - Recurse
 *
 * WHY BACKTRACKING?
 * Because we try all valid partitions of string into 4 parts.
 */
public class RestoreIPAddresses {

    public List<String> restoreIpAddresses(String s) {
        List<String> result = new ArrayList<>();

        // Edge pruning: impossible lengths
        if (s.length() < 4 || s.length() > 12) return result;

        backtrack(s, 0, 0, new ArrayList<>(), result);
        return result;
    }

    /**
     * @param index    Current position in string
     * @param segments Number of segments formed so far
     * @param path     Current segments list
     */
    private void backtrack(String s, int index, int segments,
                           List<String> path, List<String> result) {

        // ✅ Base case: 4 segments formed
        if (segments == 4) {
            // Check if we used all characters
            if (index == s.length()) {
                result.add(String.join(".", path));
            }
            return;
        }

        // ❌ Pruning: too many chars left OR too few
        int remainingChars = s.length() - index;
        int remainingSegments = 4 - segments;

        if (remainingChars < remainingSegments || remainingChars > remainingSegments * 3) {
            return;
        }

        // 🔁 Try segment lengths 1 to 3
        for (int len = 1; len <= 3 && index + len <= s.length(); len++) {

            String segment = s.substring(index, index + len);

            // 🚫 Skip invalid segments
            if (!isValid(segment)) continue;

            // Choose
            path.add(segment);

            // Explore
            backtrack(s, index + len, segments + 1, path, result);

            // Undo (backtrack)
            path.remove(path.size() - 1);
        }
    }

    /**
     * Check if segment is valid:
     * 1. No leading zero unless "0"
     * 2. Value <= 255
     */
    private boolean isValid(String segment) {

        // ❌ Leading zero case
        if (segment.length() > 1 && segment.charAt(0) == '0') {
            return false;
        }

        int value = Integer.parseInt(segment);

        return value >= 0 && value <= 255;
    }

    // Driver
    public static void main(String[] args) {
        RestoreIPAddresses sol = new RestoreIPAddresses();
        System.out.println(sol.restoreIpAddresses("25525511135"));
    }
}
