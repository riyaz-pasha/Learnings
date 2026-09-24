import java.util.*;
/*
 * Given a pattern and a string s, find if s follows the same pattern.
 * 
 * Here follow means a full match, such that there is a bijection between a
 * letter in pattern and a non-empty word in s. Specifically:
 * 
 * Each letter in pattern maps to exactly one unique word in s.
 * Each unique word in s maps to exactly one letter in pattern.
 * No two letters map to the same word, and no two words map to the same letter.
 * 
 * Example 1:
 * Input: pattern = "abba", s = "dog cat cat dog"
 * Output: true
 * Explanation:
 * The bijection can be established as:
 * 'a' maps to "dog".
 * 'b' maps to "cat".
 *
 * Example 2:
 * Input: pattern = "abba", s = "dog cat cat fish"
 * Output: false
 * 
 * Example 3:
 * Input: pattern = "aaaa", s = "dog cat cat dog"
 * Output: false
 */

class WordPattern {

    // Solution 1: Two HashMaps - Most intuitive approach
    // Time: O(n + m), Space: O(w) where w is number of unique words
    public boolean wordPattern1(String pattern, String s) {
        String[] words = s.split(" ");

        // Early check: pattern length must match word count
        if (pattern.length() != words.length) {
            return false;
        }

        Map<Character, String> charToWord = new HashMap<>();
        Map<String, Character> wordToChar = new HashMap<>();

        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            String word = words[i];

            // Check char -> word mapping
            if (charToWord.containsKey(c)) {
                if (!charToWord.get(c).equals(word)) {
                    return false;
                }
            } else {
                charToWord.put(c, word);
            }

            // Check word -> char mapping (ensure bijection)
            if (wordToChar.containsKey(word)) {
                if (wordToChar.get(word) != c) {
                    return false;
                }
            } else {
                wordToChar.put(word, c);
            }
        }

        return true;
    }

    // Solution 2: Single HashMap with Set - Space optimized
    // Time: O(n + m), Space: O(w)
    public boolean wordPattern2(String pattern, String s) {
        String[] words = s.split(" ");

        if (pattern.length() != words.length) {
            return false;
        }

        Map<Character, String> mapping = new HashMap<>();
        Set<String> usedWords = new HashSet<>();

        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            String word = words[i];

            if (mapping.containsKey(c)) {
                if (!mapping.get(c).equals(word)) {
                    return false;
                }
            } else {
                // Check if word is already mapped to another character
                if (usedWords.contains(word)) {
                    return false;
                }
                mapping.put(c, word);
                usedWords.add(word);
            }
        }

        return true;
    }

    // Solution 3: Using Map.put() return values - Elegant approach
    // Time: O(n + m), Space: O(w)
    public boolean wordPattern3(String pattern, String s) {
        String[] words = s.split(" ");

        if (pattern.length() != words.length) {
            return false;
        }

        Map<Character, Integer> charMap = new HashMap<>();
        Map<String, Integer> wordMap = new HashMap<>();

        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            String word = words[i];

            // Put returns previous value or null if first occurrence
            Integer charIndex = charMap.put(c, i);
            Integer wordIndex = wordMap.put(word, i);

            // Both should be null (first occurrence) or equal (same previous index)
            if (!Objects.equals(charIndex, wordIndex)) {
                return false;
            }
        }

        return true;
    }

    // Solution 4: Pattern normalization approach
    // Time: O(n + m), Space: O(w)
    public boolean wordPattern4(String pattern, String s) {
        return normalizePattern(pattern).equals(normalizeWords(s));
    }

    private String normalizePattern(String pattern) {
        Map<Character, Integer> charToIndex = new HashMap<>();
        StringBuilder normalized = new StringBuilder();
        int index = 0;

        for (char c : pattern.toCharArray()) {
            if (!charToIndex.containsKey(c)) {
                charToIndex.put(c, index++);
            }
            normalized.append(charToIndex.get(c)).append(",");
        }

        return normalized.toString();
    }

    private String normalizeWords(String s) {
        String[] words = s.split(" ");
        Map<String, Integer> wordToIndex = new HashMap<>();
        StringBuilder normalized = new StringBuilder();
        int index = 0;

        for (String word : words) {
            if (!wordToIndex.containsKey(word)) {
                wordToIndex.put(word, index++);
            }
            normalized.append(wordToIndex.get(word)).append(",");
        }

        return normalized.toString();
    }

    // Solution 5: Single pass with arrays (for limited pattern characters)
    // Time: O(n + m), Space: O(k) where k is alphabet size
    public boolean wordPattern5(String pattern, String s) {
        String[] words = s.split(" ");

        if (pattern.length() != words.length) {
            return false;
        }

        // Assuming lowercase letters only
        String[] charToWord = new String[26];
        Map<String, Character> wordToChar = new HashMap<>();

        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            String word = words[i];
            int charIndex = c - 'a';

            if (charToWord[charIndex] == null && !wordToChar.containsKey(word)) {
                // Create new mapping
                charToWord[charIndex] = word;
                wordToChar.put(word, c);
            } else if (!word.equals(charToWord[charIndex]) || wordToChar.get(word) != c) {
                return false;
            }
        }

        return true;
    }

    // Solution 6: Optimized with early string splitting check
    // Time: O(n + m), Space: O(w)
    public boolean wordPattern6(String pattern, String s) {
        // Quick length check before splitting
        int spaceCount = 0;
        for (char c : s.toCharArray()) {
            if (c == ' ')
                spaceCount++;
        }

        if (pattern.length() != spaceCount + 1) {
            return false;
        }

        String[] words = s.split(" ");
        Map<Character, String> charToWord = new HashMap<>();
        Map<String, Character> wordToChar = new HashMap<>();

        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            String word = words[i];

            String expectedWord = charToWord.get(c);
            Character expectedChar = wordToChar.get(word);

            if (expectedWord == null && expectedChar == null) {
                charToWord.put(c, word);
                wordToChar.put(word, c);
            } else if (!word.equals(expectedWord) || expectedChar != c) {
                return false;
            }
        }

        return true;
    }

    // Solution 7: Using StringBuilder for efficient string operations
    // Time: O(n + m), Space: O(w)
    public boolean wordPattern7(String pattern, String s) {
        List<String> words = splitWords(s);

        if (pattern.length() != words.size()) {
            return false;
        }

        Map<Character, String> charToWord = new HashMap<>();
        Set<String> usedWords = new HashSet<>();

        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            String word = words.get(i);

            if (charToWord.containsKey(c)) {
                if (!charToWord.get(c).equals(word)) {
                    return false;
                }
            } else {
                if (usedWords.contains(word)) {
                    return false;
                }
                charToWord.put(c, word);
                usedWords.add(word);
            }
        }

        return true;
    }

    private List<String> splitWords(String s) {
        List<String> words = new ArrayList<>();
        StringBuilder word = new StringBuilder();

        for (char c : s.toCharArray()) {
            if (c == ' ') {
                if (word.length() > 0) {
                    words.add(word.toString());
                    word.setLength(0);
                }
            } else {
                word.append(c);
            }
        }

        if (word.length() > 0) {
            words.add(word.toString());
        }

        return words;
    }

    // Test method
    public static void main(String[] args) {
        WordPattern solution = new WordPattern();

        // Test cases
        System.out.println(solution.wordPattern1("abba", "dog cat cat dog")); // true
        System.out.println(solution.wordPattern1("abba", "dog cat cat fish")); // false
        System.out.println(solution.wordPattern1("aaaa", "dog cat cat dog")); // false
        System.out.println(solution.wordPattern1("abba", "dog dog dog dog")); // false
        System.out.println(solution.wordPattern1("abc", "dog cat fish")); // true
        System.out.println(solution.wordPattern1("a", "dog")); // true
        System.out.println(solution.wordPattern1("ab", "dog")); // false
    }

}

/*
 * Analysis of Solutions:
 * 
 * 1. Two HashMaps (wordPattern1):
 * - Most intuitive and clear
 * - Explicit bidirectional mapping
 * - Best for interviews to demonstrate understanding
 * 
 * 2. HashMap + Set (wordPattern2):
 * - Slight space optimization
 * - Single direction mapping with set for reverse check
 * - Clean and efficient
 * 
 * 3. Map.put() Return Values (wordPattern3):
 * - Elegant and concise
 * - Uses Map.put() return value cleverly
 * - Less intuitive but very efficient
 * 
 * 4. Pattern Normalization (wordPattern4):
 * - Creative approach using pattern matching
 * - Converts both to normalized forms for comparison
 * - Higher space complexity but interesting concept
 * 
 * 5. Array + HashMap Hybrid (wordPattern5):
 * - Optimized for limited character sets
 * - Combines array efficiency with HashMap flexibility
 * - Good for lowercase letter patterns
 * 
 * 6. Early Length Check (wordPattern6):
 * - Optimizes by counting spaces before splitting
 * - Avoids unnecessary string operations
 * - Good for performance-critical applications
 * 
 * 7. Custom String Splitting (wordPattern7):
 * - Avoids built-in split() method
 * - More control over string processing
 * - Useful when split() behavior needs customization
 * 
 * Key Insights:
 * - Must maintain bijection: char ↔ word one-to-one mapping
 * - Pattern length must equal word count
 * - Empty words are not allowed
 * - Characters can map to identical words if they're the same character
 * 
 * Time Complexity: O(n + m) where n = pattern length, m = string length
 * Space Complexity: O(w) where w = number of unique words
 * 
 * Recommendation: Use wordPattern1 for clarity in interviews,
 * wordPattern2 or wordPattern3 for production code.
 */
