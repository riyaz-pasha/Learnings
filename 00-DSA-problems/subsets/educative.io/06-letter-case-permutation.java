/**
 * ============================================================================
 * PROBLEM STATEMENT: Letter Case Permutation
 * ============================================================================
 * Given a string, s, consisting of letters and digits, generate all possible 
 * variations by modifying each letter independently to be either lowercase or 
 * uppercase while keeping the digits unchanged. 
 * 
 * Return a list containing all such variations in any order.
 * 
 * Constraints:
 * - 1 <= s.length <= 12
 * - s consists of lowercase English letters, uppercase English letters, and digits.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS (To ask in an interview):
 * ============================================================================
 * 1. Are there any special characters (like symbols or spaces) in the string?
 *    (Constraint states only letters and digits, so we don't need to worry 
 *    about punctuation).
 * 2. Does the order of the permutations in the output list matter?
 *    (Problem states "any order").
 * 3. Should the output include the original string itself?
 *    (Yes, the original casing sequence is one of the valid permutations).
 * 
 * ============================================================================
 * INTERVIEW APPROACH:
 * ============================================================================
 * 1. Acknowledge the constraint: s.length <= 12. Since each character can 
 *    only branch at most 2 ways (if it's a letter), the maximum number of 
 *    combinations is 2^12 = 4096. This is a very small search space, making 
 *    backtracking (DFS) or iterative (BFS) approaches ideal.
 * 2. Discuss the three primary ways to solve this:
 *    - Backtracking (DFS): Modify a char array in place to build permutations. 
 *      Very efficient in terms of space.
 *    - Iterative (BFS): Maintain a list of combinations and double its size 
 *      whenever you encounter a letter. Simple to visualize.
 *    - Bit Manipulation: Count the letters (say, K). There are 2^K combinations. 
 *      Use bits 0 to (2^K - 1) to determine the casing of each letter.
 * 3. Start with the Backtracking (DFS) approach, as it directly modifies a 
 *    character array and demonstrates a solid grasp of recursion and memory 
 *    efficiency.
 * 
 * ============================================================================
 * TIME & SPACE COMPLEXITY (Applies to all solutions):
 * ============================================================================
 * - Time Complexity: O(N * 2^N), where N is the number of letters in the string. 
 *   There are 2^N permutations, and it takes O(N) time to construct each string.
 * - Space Complexity: O(N * 2^N) to store the result list. The auxiliary 
 *   recursion stack space for DFS is O(N).
 */

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

class LetterCasePermutationSolver {

    public static void main(String[] args) {
        String s = "a1b2";
        
        System.out.println("Input string: \"a1b2\"\n");

        System.out.println("1. Backtracking (DFS) Approach:");
        System.out.println(letterCasePermutationDFS(s));
        
        System.out.println("\n2. Iterative (BFS) Approach:");
        System.out.println(letterCasePermutationBFS(s));
        
        System.out.println("\n3. Bitmasking Approach (Using Java Streams):");
        System.out.println(letterCasePermutationBitmask(s));
    }

    /**
     * ========================================================================
     * SOLUTION 1: BACKTRACKING (DFS / RECURSION)
     * ========================================================================
     * Idea & Intuition:
     * Traverse the string index by index using a character array. 
     * - If the current character is a digit, we just move to the next index.
     * - If the current character is a letter, we branch into two paths:
     *   1. Make it lowercase and recurse.
     *   2. Make it uppercase and recurse.
     * 
     * Visual Decision Tree for "a1b":
     *                              (Start: index 0)
     *                             /                \
     *                    lower 'a'                  upper 'A'
     *                     ("a1b")                    ("A1b")
     *                      |                          |
     *                 digit '1'                  digit '1' 
     *              (skip to index 2)          (skip to index 2)
     *                   /     \                    /     \
     *           lower 'b'   upper 'B'      lower 'b'   upper 'B'
     *            ("a1b")     ("a1B")        ("A1b")     ("A1B")
     * 
     * Key Observation: Using a `char[]` and modifying it in-place is much 
     * faster and uses less memory than continuously generating StringBuilders.
     */
    public static List<String> letterCasePermutationDFS(String s) {
        var result = new ArrayList<String>();
        dfs(s.toCharArray(), 0, result);
        return result;
    }

    private static void dfs(char[] chars, int index, List<String> result) {
        // Base case: We've processed the entire string
        if (index == chars.length) {
            result.add(new String(chars));
            return;
        }

        // If it's a letter, we branch (toggle case)
        if (Character.isLetter(chars[index])) {
            // Branch 1: Lowercase
            chars[index] = Character.toLowerCase(chars[index]);
            dfs(chars, index + 1, result);

            // Branch 2: Uppercase
            chars[index] = Character.toUpperCase(chars[index]);
            dfs(chars, index + 1, result);
        } else {
            // If it's a digit, just proceed to the next character
            dfs(chars, index + 1, result);
        }
    }

    /**
     * ========================================================================
     * SOLUTION 2: ITERATIVE WITH QUEUE/LIST (BFS CASCADING)
     * ========================================================================
     * Idea & Intuition:
     * Start with a list containing a single empty string. Iterate through each 
     * character of the input string.
     * - If it's a digit, append it to all strings currently in the list.
     * - If it's a letter, duplicate the current list. Append the lowercase 
     *   version to the first half, and the uppercase version to the second half.
     * 
     * Visual Example for "a1b":
     * Initial: [""]
     * Read 'a': ["a", "A"]
     * Read '1': ["a1", "A1"]
     * Read 'b': ["a1b", "A1b", "a1B", "A1B"]
     */
    public static List<String> letterCasePermutationBFS(String s) {
        var result = new ArrayList<String>();
        result.add(""); // Seed the list

        for (char c : s.toCharArray()) {
            // Create a new list for the next generation of strings
            var nextResult = new ArrayList<String>();
            
            if (Character.isLetter(c)) {
                // If it's a letter, double the variations
                for (String str : result) {
                    nextResult.add(str + Character.toLowerCase(c));
                    nextResult.add(str + Character.toUpperCase(c));
                }
            } else {
                // If it's a digit, just append it to existing variations
                for (String str : result) {
                    nextResult.add(str + c);
                }
            }
            
            // Move to the next state
            result = nextResult;
        }
        
        return result;
    }

    /**
     * ========================================================================
     * SOLUTION 3: BIT MANIPULATION (BITMASKING)
     * ========================================================================
     * Idea & Intuition:
     * Suppose the string has 'K' letters. There are exactly 2^K possible 
     * permutations. We can use a binary number (mask) from 0 to (2^K - 1) to 
     * represent the state of each letter.
     * - If the i-th bit in the mask is 0, the i-th letter is lowercase.
     * - If the i-th bit in the mask is 1, the i-th letter is uppercase.
     * 
     * Example for "a1b" (K=2 letters -> 4 combinations):
     * Mask 0 (00): lower 1st, lower 2nd -> a1b
     * Mask 1 (01): lower 1st, upper 2nd -> a1B
     * Mask 2 (10): upper 1st, lower 2nd -> A1b
     * Mask 3 (11): upper 1st, upper 2nd -> A1B
     * 
     * Note: This uses modern Java Streams for conciseness.
     */
    public static List<String> letterCasePermutationBitmask(String s) {
        // Count total letters to determine the bitmask limit
        int letterCount = (int) s.chars().filter(Character::isLetter).count();
        int totalPermutations = 1 << letterCount; // 2^letterCount

        return IntStream.range(0, totalPermutations)
                .mapToObj(mask -> {
                    StringBuilder sb = new StringBuilder();
                    int letterIndex = 0; // Tracks which letter we are on
                    
                    for (char c : s.toCharArray()) {
                        if (Character.isLetter(c)) {
                            // Check the (letterIndex)-th bit of the mask
                            if (((mask >> letterIndex) & 1) == 1) {
                                sb.append(Character.toUpperCase(c));
                            } else {
                                sb.append(Character.toLowerCase(c));
                            }
                            letterIndex++;
                        } else {
                            // Digits pass through unmodified
                            sb.append(c);
                        }
                    }
                    return sb.toString();
                })
                .toList(); // Collect to immutable list (Java 16+)
    }
}

class LetterCasePermutation {

    public static void main(String[] args) {
        String s = "a1b";
        List<String> result = letterCasePermutation(s);
        System.out.println(result);
    }

    public static List<String> letterCasePermutation(String s) {
        List<String> result = new ArrayList<>();

        // Start recursion with empty path
        backtrack(0, s, new StringBuilder(), result);

        return result;
    }

    /**
     * index → current position in string
     * path  → current combination being built
     */
    private static void backtrack(int index, String s, StringBuilder path, List<String> result) {

        // 🟢 Base Case: we processed all characters
        if (index == s.length()) {
            result.add(path.toString());
            return;
        }

        char current = s.charAt(index);

        // 🟡 Case 1: Digit → only one option
        if (Character.isDigit(current)) {
            path.append(current);
            backtrack(index + 1, s, path, result);
            path.deleteCharAt(path.length() - 1); // backtrack
        } else {
            // 🔵 Case 2: Letter → two choices

            // Choice 1: lowercase
            path.append(Character.toLowerCase(current));
            backtrack(index + 1, s, path, result);
            path.deleteCharAt(path.length() - 1);

            // Choice 2: uppercase
            path.append(Character.toUpperCase(current));
            backtrack(index + 1, s, path, result);
            path.deleteCharAt(path.length() - 1);
        }
    }
}
