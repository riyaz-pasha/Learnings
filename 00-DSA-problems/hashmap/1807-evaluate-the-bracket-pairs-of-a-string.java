
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/*
 * You are given a string s that contains some bracket pairs, with each pair
 * containing a non-empty key.
 * 
 * For example, in the string "(name)is(age)yearsold", there are two bracket
 * pairs that contain the keys "name" and "age".
 * You know the values of a wide range of keys. This is represented by a 2D
 * string array knowledge where each knowledge[i] = [keyi, valuei] indicates
 * that key keyi has a value of valuei.
 * 
 * You are tasked to evaluate all of the bracket pairs. When you evaluate a
 * bracket pair that contains some key keyi, you will:
 * 
 * Replace keyi and the bracket pair with the key's corresponding valuei.
 * If you do not know the value of the key, you will replace keyi and the
 * bracket pair with a question mark "?" (without the quotation marks).
 * Each key will appear at most once in your knowledge. There will not be any
 * nested brackets in s.
 * 
 * Return the resulting string after evaluating all of the bracket pairs.
 * 
 * Example 1:
 * Input: s = "(name)is(age)yearsold", knowledge =
 * [["name","bob"],["age","two"]]
 * Output: "bobistwoyearsold"
 * Explanation:
 * The key "name" has a value of "bob", so replace "(name)" with "bob".
 * The key "age" has a value of "two", so replace "(age)" with "two".
 *
 * Example 2:
 * Input: s = "hi(name)", knowledge = [["a","b"]]
 * Output: "hi?"
 * Explanation: As you do not know the value of the key "name", replace "(name)"
 * with "?".
 * 
 * Example 3:
 * Input: s = "(a)(a)(a)aaa", knowledge = [["a","yes"]]
 * Output: "yesyesyesaaa"
 * Explanation: The same key can appear multiple times.
 * The key "a" has a value of "yes", so replace all occurrences of "(a)" with
 * "yes".
 * Notice that the "a"s not in a bracket pair are not evaluated.
 */


class BracketPairEvaluation {
    
    /**
     * Solution 1: Using HashMap and StringBuilder (Most Efficient)
     * Time Complexity: O(n + m) where n is length of string, m is size of knowledge
     * Space Complexity: O(m) for the knowledge map
     */
    public String evaluate(String s, String[][] knowledge) {
        // Build knowledge map for O(1) lookups
        Map<String, String> knowledgeMap = new HashMap<>();
        for (String[] pair : knowledge) {
            knowledgeMap.put(pair[0], pair[1]);
        }
        
        StringBuilder result = new StringBuilder();
        int i = 0;
        
        while (i < s.length()) {
            if (s.charAt(i) == '(') {
                // Find the closing bracket
                int j = i + 1;
                while (j < s.length() && s.charAt(j) != ')') {
                    j++;
                }
                
                // Extract the key
                String key = s.substring(i + 1, j);
                
                // Replace with value or "?"
                if (knowledgeMap.containsKey(key)) {
                    result.append(knowledgeMap.get(key));
                } else {
                    result.append("?");
                }
                
                i = j + 1; // Move past the closing bracket
            } else {
                result.append(s.charAt(i));
                i++;
            }
        }
        
        return result.toString();
    }
    
    /**
     * Solution 2: Using Regular Expressions (More Concise)
     * Time Complexity: O(n * k) where k is number of bracket pairs
     * Space Complexity: O(m) for the knowledge map
     */
    public String evaluateRegex(String s, String[][] knowledge) {
        // Map<String, String> knowledgeMap = new HashMap<>();
        // for (String[] pair : knowledge) {
        //     knowledgeMap.put(pair[0], pair[1]);
        // }
        
        // return s.replaceAll("\\(([^)]+)\\)", match -> {
        //     String key = match.substring(1, match.length() - 1);
        //     return knowledgeMap.getOrDefault(key, "?");
        // });
        return "";
    }
    
    /**
     * Solution 3: Stack-based approach (Good for understanding)
     * Time Complexity: O(n + m)
     * Space Complexity: O(n + m)
     */
    public String evaluateStack(String s, String[][] knowledge) {
        Map<String, String> knowledgeMap = new HashMap<>();
        for (String[] pair : knowledge) {
            knowledgeMap.put(pair[0], pair[1]);
        }
        
        StringBuilder result = new StringBuilder();
        Stack<Character> keyBuilder = new Stack<>();
        boolean insideBrackets = false;
        
        for (char c : s.toCharArray()) {
            if (c == '(') {
                insideBrackets = true;
            } else if (c == ')') {
                // Build the key from stack
                StringBuilder key = new StringBuilder();
                while (!keyBuilder.isEmpty()) {
                    key.insert(0, keyBuilder.pop());
                }
                
                // Replace with value or "?"
                String keyStr = key.toString();
                if (knowledgeMap.containsKey(keyStr)) {
                    result.append(knowledgeMap.get(keyStr));
                } else {
                    result.append("?");
                }
                
                insideBrackets = false;
            } else if (insideBrackets) {
                keyBuilder.push(c);
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Test method to verify all solutions
     */
    public static void main(String[] args) {
        BracketPairEvaluation solution = new BracketPairEvaluation();
        
        // Test Case 1
        String s1 = "(name)is(age)yearsold";
        String[][] knowledge1 = {{"name", "bob"}, {"age", "two"}};
        System.out.println("Test 1:");
        System.out.println("Input: " + s1);
        System.out.println("Expected: bobistwoyearsold");
        System.out.println("Solution 1: " + solution.evaluate(s1, knowledge1));
        System.out.println("Solution 2: " + solution.evaluateRegex(s1, knowledge1));
        System.out.println("Solution 3: " + solution.evaluateStack(s1, knowledge1));
        System.out.println();
        
        // Test Case 2
        String s2 = "hi(name)";
        String[][] knowledge2 = {{"a", "b"}};
        System.out.println("Test 2:");
        System.out.println("Input: " + s2);
        System.out.println("Expected: hi?");
        System.out.println("Solution 1: " + solution.evaluate(s2, knowledge2));
        System.out.println("Solution 2: " + solution.evaluateRegex(s2, knowledge2));
        System.out.println("Solution 3: " + solution.evaluateStack(s2, knowledge2));
        System.out.println();
        
        // Test Case 3
        String s3 = "(a)(a)(a)aaa";
        String[][] knowledge3 = {{"a", "yes"}};
        System.out.println("Test 3:");
        System.out.println("Input: " + s3);
        System.out.println("Expected: yesyesyesaaa");
        System.out.println("Solution 1: " + solution.evaluate(s3, knowledge3));
        System.out.println("Solution 2: " + solution.evaluateRegex(s3, knowledge3));
        System.out.println("Solution 3: " + solution.evaluateStack(s3, knowledge3));
        System.out.println();
        
        // Additional Test Case - Empty brackets and edge cases
        String s4 = "hello()world(key)";
        String[][] knowledge4 = {{"key", "value"}};
        System.out.println("Test 4 (Edge case):");
        System.out.println("Input: " + s4);
        System.out.println("Expected: hello?worldvalue");
        System.out.println("Solution 1: " + solution.evaluate(s4, knowledge4));
        System.out.println("Solution 2: " + solution.evaluateRegex(s4, knowledge4));
        System.out.println("Solution 3: " + solution.evaluateStack(s4, knowledge4));
    }
}

/**
 * LeetCode-style class for submission
 */
class Solution {

    public String evaluate(String s, String[][] knowledge) {
        Map<String, String> knowledgeMap = new HashMap<>();
        for (String[] pair : knowledge) {
            knowledgeMap.put(pair[0], pair[1]);
        }
        
        StringBuilder result = new StringBuilder();
        int i = 0;
        
        while (i < s.length()) {
            if (s.charAt(i) == '(') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) != ')') {
                    j++;
                }
                
                String key = s.substring(i + 1, j);
                
                if (knowledgeMap.containsKey(key)) {
                    result.append(knowledgeMap.get(key));
                } else {
                    result.append("?");
                }
                
                i = j + 1;
            } else {
                result.append(s.charAt(i));
                i++;
            }
        }
        
        return result.toString();
    }

}


 class EvaluateBracketPairs {

    public String evaluate(String s, List<List<String>> knowledge) {

        Map<String, String> map = new HashMap<>();
        for (List<String> pair : knowledge) {
            map.put(pair.get(0), pair.get(1));
        }

        StringBuilder result = new StringBuilder();
        int i = 0;
        int n = s.length();

        while (i < n) {
            char ch = s.charAt(i);
            if (ch == '(') {
                int j = i + 1;
                while (j < n && s.charAt(j) != ')') {
                    j++;
                }
                String key = s.substring(i + 1, j);
                result.append(map.getOrDefault(key, "?"));
                i = j + 1; // Skip past ')'
            } else {
                result.append(ch);
                i++;
            }
        }

        return result.toString();
    }

    // Test it
    public static void main(String[] args) {
        EvaluateBracketPairs solution = new EvaluateBracketPairs();

        System.out.println(solution.evaluate(
                "(name)is(age)yearsold",
                Arrays.asList(Arrays.asList("name", "bob"), Arrays.asList("age", "two"))
        )); // Output: "bobistwoyearsold"

        System.out.println(solution.evaluate(
                "hi(name)",
                Arrays.asList(Arrays.asList("a", "b"))
        )); // Output: "hi?"

        System.out.println(solution.evaluate(
                "(a)(a)(a)aaa",
                Arrays.asList(Arrays.asList("a", "yes"))
        )); // Output: "yesyesyesaaa"
    }
    
}
