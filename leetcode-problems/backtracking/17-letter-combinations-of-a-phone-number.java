import java.util.*;
/*
 * Given a string containing digits from 2-9 inclusive, return all possible
 * letter combinations that the number could represent. Return the answer in any
 * order.
 * 
 * A mapping of digits to letters (just like on the telephone buttons) is given
 * below. Note that 1 does not map to any letters.
 * 
 * Example 1:
 * 
 * Input: digits = "23"
 * Output: ["ad","ae","af","bd","be","bf","cd","ce","cf"]
 * Example 2:
 * 
 * Input: digits = ""
 * Output: []
 * Example 3:
 * 
 * Input: digits = "2"
 * Output: ["a","b","c"]
 */

class LetterCombinations {

    // Solution 1: Backtracking (Most Common Approach)
    public List<String> letterCombinations1(String digits) {
        List<String> result = new ArrayList<>();
        if (digits == null || digits.length() == 0) {
            return result;
        }

        String[] mapping = {
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

        backtrack(result, mapping, digits, 0, new StringBuilder());
        return result;
    }

    private void backtrack(List<String> result, String[] mapping, String digits,
            int index, StringBuilder current) {
        // Base case: if we've processed all digits
        if (index == digits.length()) {
            result.add(current.toString());
            return;
        }

        // Get the letters for the current digit
        String letters = mapping[digits.charAt(index) - '0'];

        // Try each letter for the current digit
        for (char letter : letters.toCharArray()) {
            current.append(letter);
            backtrack(result, mapping, digits, index + 1, current);
            current.deleteCharAt(current.length() - 1); // backtrack
        }
    }

    // Solution 2: Iterative Approach using Queue
    public List<String> letterCombinations2(String digits) {
        if (digits == null || digits.length() == 0) {
            return new ArrayList<>();
        }

        String[] mapping = { "", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz" };
        Queue<String> queue = new LinkedList<>();
        queue.offer("");

        for (char digit : digits.toCharArray()) {
            String letters = mapping[digit - '0'];
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                String current = queue.poll();
                for (char letter : letters.toCharArray()) {
                    queue.offer(current + letter);
                }
            }
        }

        return new ArrayList<>(queue);
    }

    // Solution 3: Using HashMap for mapping
    public List<String> letterCombinations3(String digits) {
        List<String> result = new ArrayList<>();
        if (digits == null || digits.length() == 0) {
            return result;
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

        generateCombinations(digits, 0, phoneMap, new StringBuilder(), result);
        return result;
    }

    private void generateCombinations(String digits, int index, Map<Character, String> phoneMap,
            StringBuilder current, List<String> result) {
        if (index == digits.length()) {
            result.add(current.toString());
            return;
        }

        String possibleLetters = phoneMap.get(digits.charAt(index));
        for (char letter : possibleLetters.toCharArray()) {
            current.append(letter);
            generateCombinations(digits, index + 1, phoneMap, current, result);
            current.deleteCharAt(current.length() - 1);
        }
    }

    // Solution 4: Recursive without helper method
    public List<String> letterCombinations4(String digits) {
        if (digits == null || digits.length() == 0) {
            return new ArrayList<>();
        }

        String[] mapping = { "", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz" };

        if (digits.length() == 1) {
            List<String> result = new ArrayList<>();
            String letters = mapping[digits.charAt(0) - '0'];
            for (char c : letters.toCharArray()) {
                result.add(String.valueOf(c));
            }
            return result;
        }

        List<String> prev = letterCombinations4(digits.substring(0, digits.length() - 1));
        List<String> result = new ArrayList<>();
        String letters = mapping[digits.charAt(digits.length() - 1) - '0'];

        for (String s : prev) {
            for (char c : letters.toCharArray()) {
                result.add(s + c);
            }
        }

        return result;
    }

    // Solution 5: Using StringBuilder array for better performance
    public List<String> letterCombinations5(String digits) {
        if (digits == null || digits.length() == 0) {
            return new ArrayList<>();
        }

        String[] mapping = { "", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz" };
        List<String> result = new ArrayList<>();
        char[] combination = new char[digits.length()];

        dfs(digits, 0, mapping, combination, result);
        return result;
    }

    private void dfs(String digits, int index, String[] mapping, char[] combination, List<String> result) {
        if (index == digits.length()) {
            result.add(new String(combination));
            return;
        }

        String letters = mapping[digits.charAt(index) - '0'];
        for (char letter : letters.toCharArray()) {
            combination[index] = letter;
            dfs(digits, index + 1, mapping, combination, result);
        }
    }

    // Test method
    public static void main(String[] args) {
        LetterCombinations lc = new LetterCombinations();

        // Test cases
        System.out.println("Input: \"23\"");
        System.out.println("Output: " + lc.letterCombinations1("23"));

        System.out.println("\nInput: \"\"");
        System.out.println("Output: " + lc.letterCombinations1(""));

        System.out.println("\nInput: \"2\"");
        System.out.println("Output: " + lc.letterCombinations1("2"));

        System.out.println("\nInput: \"234\"");
        System.out.println("Output: " + lc.letterCombinations1("234"));
    }

}

/*
 * Time Complexity Analysis:
 * - All solutions: O(3^N × 4^M) where N is number of digits mapping to 3
 * letters,
 * M is number of digits mapping to 4 letters
 * - Space Complexity: O(3^N × 4^M) for storing results
 * 
 * Solution Comparison:
 * 1. Backtracking: Most intuitive, good for interviews
 * 2. Iterative: Good for understanding the process step by step
 * 3. HashMap: More readable mapping, slightly slower due to HashMap overhead
 * 4. Recursive: Elegant but uses more memory due to substring creation
 * 5. Char Array: Most memory efficient, avoids StringBuilder overhead
 */
