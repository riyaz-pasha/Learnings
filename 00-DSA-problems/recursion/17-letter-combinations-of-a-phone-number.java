import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

class LetterCombinations {

    // Phone button mapping
    private static final String[] PHONE_MAP = {
            "", // 0
            "", // 1
            "abc", // 2
            "def", // 3
            "ghi", // 4
            "jkl", // 5
            "mno", // 6
            "pqrs", // 7
            "tuv", // 8
            "wxyz" // 9
    };

    // Approach 1: Backtracking (Optimal)
    // Time: O(4^n * n), Space: O(n)
    public List<String> letterCombinations1(String digits) {
        List<String> result = new ArrayList<>();

        if (digits == null || digits.isEmpty()) {
            return result;
        }

        backtrack(digits, 0, new StringBuilder(), result);
        return result;
    }

    private void backtrack(String digits, int index, StringBuilder current,
            List<String> result) {
        // Base case: formed complete combination
        if (index == digits.length()) {
            result.add(current.toString());
            return;
        }

        // Get letters for current digit
        String letters = PHONE_MAP[digits.charAt(index) - '0'];

        // Try each letter
        for (char letter : letters.toCharArray()) {
            current.append(letter);
            backtrack(digits, index + 1, current, result);
            current.deleteCharAt(current.length() - 1); // Backtrack
        }
    }

    // Approach 2: Iterative BFS-style
    // Time: O(4^n * n), Space: O(4^n * n)
    public List<String> letterCombinations2(String digits) {
        if (digits == null || digits.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        result.add(""); // Start with empty string

        for (char digit : digits.toCharArray()) {
            String letters = PHONE_MAP[digit - '0'];
            List<String> temp = new ArrayList<>();

            // For each existing combination, append each possible letter
            for (String combo : result) {
                for (char letter : letters.toCharArray()) {
                    temp.add(combo + letter);
                }
            }

            result = temp;
        }

        return result;
    }

    // Approach 3: Queue-based BFS
    // Time: O(4^n * n), Space: O(4^n * n)
    public List<String> letterCombinations3(String digits) {
        if (digits == null || digits.isEmpty()) {
            return new ArrayList<>();
        }

        Queue<String> queue = new LinkedList<>();
        queue.offer("");

        for (int i = 0; i < digits.length(); i++) {
            String letters = PHONE_MAP[digits.charAt(i) - '0'];
            int size = queue.size();

            // Process all combinations at current level
            for (int j = 0; j < size; j++) {
                String current = queue.poll();

                // Add each possible letter
                for (char letter : letters.toCharArray()) {
                    queue.offer(current + letter);
                }
            }
        }

        return new ArrayList<>(queue);
    }

    // Approach 4: Recursive without helper function
    // Time: O(4^n * n), Space: O(4^n * n)
    public List<String> letterCombinations4(String digits) {
        List<String> result = new ArrayList<>();

        if (digits == null || digits.isEmpty()) {
            return result;
        }

        if (digits.length() == 1) {
            String letters = PHONE_MAP[digits.charAt(0) - '0'];
            for (char c : letters.toCharArray()) {
                result.add(String.valueOf(c));
            }
            return result;
        }

        // Get combinations for remaining digits
        List<String> subCombos = letterCombinations4(digits.substring(1));
        String letters = PHONE_MAP[digits.charAt(0) - '0'];

        // Prepend each letter to each sub-combination
        for (char letter : letters.toCharArray()) {
            for (String subCombo : subCombos) {
                result.add(letter + subCombo);
            }
        }

        return result;
    }

    // Approach 5: Using HashMap for mapping
    // Time: O(4^n * n), Space: O(n)
    public List<String> letterCombinations5(String digits) {
        if (digits == null || digits.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Character, String> phoneMap = new HashMap<>();
        phoneMap.put('2', "abc");
        phoneMap.put('3', "def");
        phoneMap.put('4', "ghi");
        phoneMap.put('5', "jkl");
        phoneMap.put('6', "mno");
        phoneMap.put('7', "pqrs");
        phoneMap.put('8', "tuv");
        phoneMap.put('9', "wxyz");

        List<String> result = new ArrayList<>();
        backtrackWithMap(digits, 0, new StringBuilder(), result, phoneMap);
        return result;
    }

    private void backtrackWithMap(String digits, int index, StringBuilder current,
            List<String> result, Map<Character, String> phoneMap) {
        if (index == digits.length()) {
            result.add(current.toString());
            return;
        }

        String letters = phoneMap.get(digits.charAt(index));
        for (char letter : letters.toCharArray()) {
            current.append(letter);
            backtrackWithMap(digits, index + 1, current, result, phoneMap);
            current.deleteCharAt(current.length() - 1);
        }
    }

    // Test cases with visualization
    public static void main(String[] args) {
        LetterCombinations solution = new LetterCombinations();

        // Test Case 1
        String digits1 = "23";
        List<String> result1 = solution.letterCombinations1(digits1);
        System.out.println("Test 1: digits = \"" + digits1 + "\"");
        System.out.println("Output: " + result1);
        System.out.println("Count: " + result1.size() + " combinations");
        visualizeCombinations(digits1, result1);

        // Test Case 2
        String digits2 = "2";
        List<String> result2 = solution.letterCombinations1(digits2);
        System.out.println("\nTest 2: digits = \"" + digits2 + "\"");
        System.out.println("Output: " + result2);

        // Test Case 3: Empty input
        String digits3 = "";
        List<String> result3 = solution.letterCombinations1(digits3);
        System.out.println("\nTest 3: digits = \"" + digits3 + "\"");
        System.out.println("Output: " + result3);

        // Test Case 4: Longer input
        String digits4 = "234";
        List<String> result4 = solution.letterCombinations1(digits4);
        System.out.println("\nTest 4: digits = \"" + digits4 + "\"");
        System.out.println("Output: " + result4);
        System.out.println("Count: " + result4.size() + " combinations");

        // Test Case 5: With digit 7 and 9 (4 letters each)
        String digits5 = "79";
        List<String> result5 = solution.letterCombinations1(digits5);
        System.out.println("\nTest 5: digits = \"" + digits5 + "\"");
        System.out.println("Output: " + result5);
        System.out.println("Count: " + result5.size() + " combinations (4*4=16)");

        // Compare all approaches
        System.out.println("\nComparing approaches for \"23\":");
        System.out.println("Approach 1: " + solution.letterCombinations1(digits1).size() + " results");
        System.out.println("Approach 2: " + solution.letterCombinations2(digits1).size() + " results");
        System.out.println("Approach 3: " + solution.letterCombinations3(digits1).size() + " results");
        System.out.println("Approach 4: " + solution.letterCombinations4(digits1).size() + " results");
        System.out.println("Approach 5: " + solution.letterCombinations5(digits1).size() + " results");
    }

    private static void visualizeCombinations(String digits, List<String> combinations) {
        System.out.println("\nPhone button mapping:");
        for (int i = 0; i < digits.length(); i++) {
            char digit = digits.charAt(i);
            String letters = PHONE_MAP[digit - '0'];
            System.out.println("  Digit '" + digit + "' → [" +
                    String.join(", ", letters.split("")) + "]");
        }

        System.out.println("\nDecision tree visualization:");
        if (digits.length() == 2) {
            String letters1 = PHONE_MAP[digits.charAt(0) - '0'];
            String letters2 = PHONE_MAP[digits.charAt(1) - '0'];

            System.out.println("                 root");
            System.out.print("         ");
            for (int i = 0; i < letters1.length(); i++) {
                System.out.print("    " + letters1.charAt(i) + "   ");
            }
            System.out.println();

            for (int i = 0; i < letters1.length(); i++) {
                System.out.print("       ");
                for (int k = 0; k < i; k++) {
                    System.out.print("        ");
                }
                System.out.print("  /");
                for (int j = 0; j < letters2.length() - 1; j++) {
                    System.out.print(" | ");
                }
                System.out.println("\\");

                System.out.print("       ");
                for (int k = 0; k < i; k++) {
                    System.out.print("        ");
                }
                for (int j = 0; j < letters2.length(); j++) {
                    System.out.print(letters1.charAt(i) + "" + letters2.charAt(j) + " ");
                }
                System.out.println();
            }
        }

        System.out.println("\nAll combinations: " + combinations);
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM UNDERSTANDING:
 * - Phone keypad mapping: 2→abc, 3→def, ..., 9→wxyz
 * - Given string of digits, generate all letter combinations
 * - Each digit contributes one letter to each combination
 * 
 * PHONE MAPPING:
 * 2 → abc
 * 3 → def
 * 4 → ghi
 * 5 → jkl
 * 6 → mno
 * 7 → pqrs
 * 8 → tuv
 * 9 → wxyz
 * 
 * KEY INSIGHT:
 * This is a CARTESIAN PRODUCT problem!
 * For digits "23":
 * {a,b,c} × {d,e,f} = {ad,ae,af,bd,be,bf,cd,ce,cf}
 * 
 * BACKTRACKING APPROACH (Optimal):
 * 
 * Think of it as building combinations character by character:
 * 1. Start with empty string
 * 2. For first digit, try each possible letter
 * 3. For each letter, recursively build rest of combination
 * 4. When we've processed all digits, save combination
 * 
 * Decision Tree for "23":
 * 
 * ""
 * / | \
 * a b c
 * / | \ / | \ / | \
 * ad ae af bd be bf cd ce cf
 * 
 * Each path from root to leaf is one combination!
 * 
 * EXAMPLE WALKTHROUGH: digits = "23"
 * 
 * Digit '2' → letters "abc"
 * Digit '3' → letters "def"
 * 
 * Recursion trace:
 * 1. index=0, current=""
 * - Try 'a': current="a"
 * - index=1, current="a"
 * - Try 'd': current="ad" → index=2, add "ad" ✓
 * - Try 'e': current="ae" → index=2, add "ae" ✓
 * - Try 'f': current="af" → index=2, add "af" ✓
 * - Try 'b': current="b"
 * - index=1, current="b"
 * - Try 'd': current="bd" → index=2, add "bd" ✓
 * - Try 'e': current="be" → index=2, add "be" ✓
 * - Try 'f': current="bf" → index=2, add "bf" ✓
 * - Try 'c': current="c"
 * - index=1, current="c"
 * - Try 'd': current="cd" → index=2, add "cd" ✓
 * - Try 'e': current="ce" → index=2, add "ce" ✓
 * - Try 'f': current="cf" → index=2, add "cf" ✓
 * 
 * Result: ["ad","ae","af","bd","be","bf","cd","ce","cf"]
 * 
 * ITERATIVE APPROACH (BFS):
 * 
 * Start with [""]
 * For digit '2':
 * - Append 'a' to "" → ["a"]
 * - Append 'b' to "" → ["a","b"]
 * - Append 'c' to "" → ["a","b","c"]
 * 
 * For digit '3':
 * - Append 'd' to "a" → ["ad"]
 * - Append 'e' to "a" → ["ad","ae"]
 * - Append 'f' to "a" → ["ad","ae","af"]
 * - Append 'd' to "b" → ["ad","ae","af","bd"]
 * - ... continue for all
 * 
 * Final: ["ad","ae","af","bd","be","bf","cd","ce","cf"]
 * 
 * NUMBER OF COMBINATIONS:
 * 
 * For each digit, multiply number of letters:
 * - digits "23": 3 × 3 = 9 combinations
 * - digits "234": 3 × 3 × 3 = 27 combinations
 * - digits "79": 4 × 4 = 16 combinations (7 and 9 have 4 letters)
 * 
 * General formula:
 * If digit d_i has l_i letters, total = l_1 × l_2 × ... × l_n
 * 
 * Maximum: "7777" → 4^4 = 256 combinations
 * 
 * COMPLEXITY ANALYSIS:
 * 
 * Let n = number of digits
 * Let m = average letters per digit (≈3.5, max 4)
 * 
 * Time Complexity: O(m^n × n)
 * - Total combinations: m^n
 * - Building each combination: O(n) to construct string
 * - Overall: O(m^n × n)
 * 
 * Space Complexity:
 * - Recursion stack: O(n) depth
 * - Result storage: O(m^n × n) - output space
 * - Working space: O(n) for current combination
 * 
 * For worst case (all digits are 7 or 9):
 * - Time: O(4^n × n)
 * - Space: O(4^n × n)
 * 
 * APPROACH COMPARISON:
 * 
 * Approach 1 (Backtracking):
 * - Most intuitive
 * - Space efficient (O(n) extra)
 * - Clear structure
 * - ✓ Recommended for interviews
 * 
 * Approach 2 (Iterative):
 * - No recursion overhead
 * - Easier to understand for beginners
 * - More memory (stores intermediate results)
 * 
 * Approach 3 (Queue BFS):
 * - Level-by-level building
 * - Good for visualization
 * - Similar to approach 2
 * 
 * Approach 4 (Recursive division):
 * - Functional style
 * - Less efficient (creates substrings)
 * - Not recommended for large inputs
 * 
 * EDGE CASES:
 * 
 * 1. Empty string: "" → []
 * 2. Single digit: "2" → ["a","b","c"]
 * 3. Digits with 4 letters: "7" → ["p","q","r","s"]
 * 4. Long input: "2345" → 3×3×3×4 = 108 combinations
 * 5. Maximum case: "9999" → 4^4 = 256 combinations
 * 
 * PRACTICAL APPLICATIONS:
 * 
 * 1. Password generation
 * 2. T9 text prediction (old phones)
 * 3. Mnemonic generation
 * 4. Vanity phone numbers
 * 5. Combinatorial enumeration
 * 6. Game word puzzles
 * 
 * INTERVIEW TIPS:
 * 
 * 1. Clarify: Do we return in specific order? (Usually any order)
 * 2. Ask about empty string handling
 * 3. Mention this is Cartesian product
 * 4. Draw decision tree to explain
 * 5. Discuss time/space complexity
 * 6. Mention early termination for empty input
 * 7. Consider iterative vs recursive tradeoffs
 * 
 * COMMON MISTAKES:
 * 
 * 1. Forgetting to handle empty input
 * 2. Not using StringBuilder (inefficient string concatenation)
 * 3. Forgetting to backtrack (remove last character)
 * 4. Off-by-one errors in indexing
 * 5. Hardcoding phone map incorrectly
 * 6. Not creating new string when adding to result
 * 
 * OPTIMIZATION NOTES:
 * 
 * 1. Use StringBuilder instead of String concatenation
 * 2. Use array instead of HashMap for phone mapping (faster)
 * 3. Pre-calculate result list size if needed
 * 4. Early return for empty input
 * 5. Consider character arrays instead of strings
 * 
 * RELATED PROBLEMS:
 * 
 * - Generate Parentheses: Similar backtracking structure
 * - Permutations: Explores all orderings
 * - Subsets: Explores all combinations of presence/absence
 * - Combination Sum: Backtracking with constraints
 */
