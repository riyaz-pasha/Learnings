/**
 * ============================================================================
 * PROBLEM STATEMENT: String Permutations
 * ============================================================================
 * Given an input string, word, return all possible permutations of the string.
 * Note: The order of permutations does not matter.
 * 
 * Constraints:
 * - All characters in word are unique.
 * - 1 <= word.length <= 6
 * - All characters in word are lowercase English letters.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS (To ask in an interview):
 * ============================================================================
 * 1. Are there duplicate characters in the string?
 *    (The constraint says they are unique. If there were duplicates, we would 
 *    need to use a HashSet or skip duplicates after sorting to avoid duplicate 
 *    permutations - e.g., Permutations II).
 * 2. Can the input string be null or empty?
 *    (Constraint says length >= 1, so no, but always good to check).
 * 3. Does the order of the output list matter?
 *    (Problem explicitly states order does not matter).
 * 
 * ============================================================================
 * INTERVIEW APPROACH:
 * ============================================================================
 * 1. Acknowledge the math: For a string of length N, there are N! permutations.
 *    Since N <= 6, 6! = 720. This is a very small search space, so generating 
 *    all permutations is perfectly efficient.
 * 2. Mention the primary ways to solve this:
 *    - Backtracking with Swapping (In-place): The most optimal space-wise as 
 *      it modifies the character array in place.
 *    - Backtracking with a 'visited' array: Very intuitive for beginners, builds 
 *      the string character by character.
 *    - Iterative Insertion: Progressively building permutations by inserting the 
 *      next character into all possible positions of the previous permutations.
 * 3. Start by coding the Swapping approach, as it demonstrates strong 
 *    understanding of in-place array manipulation and recursion.
 * 
 * ============================================================================
 * TIME & SPACE COMPLEXITY (Applies to all solutions):
 * ============================================================================
 * - Time Complexity: O(N * N!). There are N! permutations, and for each, we 
 *   spend O(N) time to construct the string and add it to the result list.
 * - Space Complexity: O(N) auxiliary space for the recursion stack (or iteration 
 *   queue). Storing the actual output requires O(N * N!) space.
 */

import java.util.*;

class PermutationsSolver {

    public static void main(String[] args) {
        String word = "abc";
        
        System.out.println("Input string: \"abc\"\n");

        System.out.println("1. Backtracking (Swapping In-Place):");
        System.out.println(permutationsSwap(word));
        
        System.out.println("\n2. Backtracking (Visited Array):");
        System.out.println(permutationsVisited(word));
        
        System.out.println("\n3. Iterative (Insertion Method):");
        System.out.println(permutationsIterative(word));
    }

    /**
     * ========================================================================
     * SOLUTION 1: BACKTRACKING WITH SWAPPING (OPTIMAL SPACE)
     * ========================================================================
     * Idea & Intuition:
     * We can generate permutations by choosing which character goes at the 
     * current `index`. We swap the character at `index` with every character 
     * from `index` to the end of the array, recurse for `index + 1`, and then 
     * swap back (backtrack) to restore the original state.
     * 
     * Visual Decision Tree for "abc" (Swapping):
     *                          [a,b,c]  (index 0)
     *               /             |             \
     *      swap(0,0)          swap(0,1)        swap(0,2)
     *        [a,b,c]            [b,a,c]          [c,b,a]
     *       /       \          /       \        /       \
     *   s(1,1)     s(1,2)  s(1,1)     s(1,2) s(1,1)    s(1,2)
     *   [a,b,c]   [a,c,b]  [b,a,c]   [b,c,a] [c,b,a]   [c,a,b]
     * 
     * Key Observation: Modifying a char array in place saves us from creating 
     * multiple StringBuilders or substring copies during the recursion.
     */
    public static List<String> permutationsSwap(String word) {
        var result = new ArrayList<String>();
        backtrackSwap(word.toCharArray(), 0, result);
        return result;
    }

    private static void backtrackSwap(char[] chars, int index, List<String> result) {
        // Base case: If we've considered all positions, we have a valid permutation
        if (index == chars.length) {
            result.add(new String(chars));
            return;
        }

        // Try placing every character (from 'index' to end) at the current 'index'
        for (int i = index; i < chars.length; i++) {
            swap(chars, index, i);                 // Choose
            backtrackSwap(chars, index + 1, result); // Explore
            swap(chars, index, i);                 // Un-choose (Backtrack)
        }
    }

    private static void swap(char[] arr, int i, int j) {
        char temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    /**
     * ========================================================================
     * SOLUTION 2: BACKTRACKING WITH VISITED ARRAY (INTUITIVE)
     * ========================================================================
     * Idea & Intuition:
     * Imagine you have N empty slots. You iterate through the characters of 
     * the string. If a character hasn't been used in the current sequence, 
     * you add it to the sequence and mark it as 'used'. Once the sequence 
     * reaches the target length, you've formed a permutation.
     * 
     * Key Observation: This mimics human logic for building permutations 
     * (pick the 1st letter, then pick the 2nd from what's left, etc.).
     */
    public static List<String> permutationsVisited(String word) {
        var result = new ArrayList<String>();
        boolean[] used = new boolean[word.length()];
        backtrackVisited(word, used, new StringBuilder(), result);
        return result;
    }

    private static void backtrackVisited(String word, boolean[] used, StringBuilder current, List<String> result) {
        // Base case: The current permutation is complete
        if (current.length() == word.length()) {
            result.add(current.toString());
            return;
        }

        for (int i = 0; i < word.length(); i++) {
            // Skip characters that are already in the current permutation
            if (used[i]) continue;

            // Choose
            used[i] = true;
            current.append(word.charAt(i));
            
            // Explore
            backtrackVisited(word, used, current, result);
            
            // Un-choose (Backtrack)
            used[i] = false;
            current.deleteCharAt(current.length() - 1);
        }
    }

    /**
     * ========================================================================
     * SOLUTION 3: ITERATIVE INSERTION (CASCADING)
     * ========================================================================
     * Idea & Intuition:
     * Start with a list containing an empty string. For each character in the 
     * input word, take all existing permutations and insert the new character 
     * into every possible position (from 0 to the length of the string).
     * 
     * Visual Example for "abc":
     * 1. Start: [""]
     * 2. Insert 'a': ["a"]
     * 3. Insert 'b' into "a": 
     *    - At index 0: "ba"
     *    - At index 1: "ab"
     *    Result: ["ba", "ab"]
     * 4. Insert 'c' into "ba": "cba", "bca", "bac"
     *    Insert 'c' into "ab": "cab", "acb", "abc"
     *    Result: ["cba", "bca", "bac", "cab", "acb", "abc"]
     */
    public static List<String> permutationsIterative(String word) {
        var result = new ArrayList<String>();
        result.add(""); // Start with empty string

        for (char c : word.toCharArray()) {
            var nextResult = new ArrayList<String>();
            
            for (String perm : result) {
                // Insert the character 'c' into all possible positions of 'perm'
                for (int i = 0; i <= perm.length(); i++) {
                    // Modern Java string concatenation using StringBuilder for efficiency
                    StringBuilder sb = new StringBuilder(perm);
                    sb.insert(i, c);
                    nextResult.add(sb.toString());
                }
            }
            // Move to the next generation of permutations
            result = nextResult;
        }
        
        return result;
    }
}

/**
 * ============================================================
 * 🔥 PERMUTATIONS OF STRING — BACKTRACKING (CORE SOLUTION)
 * ============================================================
 *
 * IDEA:
 * -----
 * Build permutations by choosing characters one by one.
 *
 * At each step:
 *  - Pick an unused character
 *  - Add it to current path
 *  - Recurse
 *  - Backtrack (remove it)
 *
 * WHY BACKTRACKING?
 * -----------------
 * Because we need ALL possible arrangements (explore all paths)
 *
 * TIME COMPLEXITY:
 * ----------------
 * O(n! * n)
 *  - n! permutations
 *  - each takes O(n) to build
 *
 * SPACE COMPLEXITY:
 * -----------------
 * O(n) recursion stack + O(n) temp storage
 *
 */
class StringPermutations {

    public static List<String> permutations(String word) {
        List<String> result = new ArrayList<>();

        // visited array to track used characters
        boolean[] used = new boolean[word.length()];

        backtrack(word, new StringBuilder(), used, result);

        return result;
    }

    private static void backtrack(String word, StringBuilder path,
                                  boolean[] used, List<String> result) {

        // ✅ Base case: full permutation formed
        if (path.length() == word.length()) {
            result.add(path.toString());
            return;
        }

        // Try all characters
        for (int i = 0; i < word.length(); i++) {

            // Skip if already used
            if (used[i]) continue;

            // Choose
            used[i] = true;
            path.append(word.charAt(i));

            // Explore
            backtrack(word, path, used, result);

            // Undo (Backtrack)
            path.deleteCharAt(path.length() - 1);
            used[i] = false;
        }
    }

    public static void main(String[] args) {
        System.out.println(permutations("abc"));
    }
}

/**
 * ============================================================
 * 🔥 PERMUTATIONS USING SWAPPING (IN-PLACE)
 * ============================================================
 *
 * IDEA:
 * -----
 * Fix each index and swap remaining characters into that position
 *
 * Example:
 * index = 0 → try all chars at position 0
 *
 * TIME: O(n! * n)
 * SPACE: O(n) recursion
 *
 */
class PermutationsSwap {

    public static List<String> permutations(String word) {
        List<String> result = new ArrayList<>();
        char[] arr = word.toCharArray();

        backtrack(arr, 0, result);

        return result;
    }

    private static void backtrack(char[] arr, int index, List<String> result) {

        // Base case
        if (index == arr.length) {
            result.add(new String(arr));
            return;
        }

        for (int i = index; i < arr.length; i++) {

            // Swap
            swap(arr, index, i);

            // Recurse
            backtrack(arr, index + 1, result);

            // Undo swap
            swap(arr, index, i);
        }
    }

    private static void swap(char[] arr, int i, int j) {
        char temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    public static void main(String[] args) {
        System.out.println(permutations("abc"));
    }
}

/**
 * ============================================================
 * 🔥 PERMUTATIONS USING INCLUDE-EXCLUDE STYLE
 * ============================================================
 *
 * IDEA:
 * -----
 * Instead of visited[], we:
 *  - Pick a character (include)
 *  - Remove it from remaining string (exclude)
 *
 * So each recursive call:
 *   prefix = chosen so far
 *   remaining = characters left
 *
 * TIME: O(n! * n)
 * SPACE: O(n)
 *
 */
public class PermutationsIncludeExclude {

    public static List<String> permutations(String word) {
        List<String> result = new ArrayList<>();
        backtrack("", word, result);
        return result;
    }

    private static void backtrack(String prefix, String remaining,
                                  List<String> result) {

        // Base case
        if (remaining.isEmpty()) {
            result.add(prefix);
            return;
        }

        // Include each character one by one
        for (int i = 0; i < remaining.length(); i++) {

            char chosen = remaining.charAt(i);

            // Exclude chosen char from remaining
            String newRemaining =
                    remaining.substring(0, i) + remaining.substring(i + 1);

            // Include chosen char in prefix
            backtrack(prefix + chosen, newRemaining, result);
        }
    }

    public static void main(String[] args) {
        System.out.println(permutations("abc"));
    }
}
