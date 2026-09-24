import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/*
 * Design a data structure that supports adding new words and finding if a
 * string matches any previously added string.
 * 
 * Implement the WordDictionary class:
 * 
 * WordDictionary() Initializes the object.
 * void addWord(word) Adds word to the data structure, it can be matched later.
 * bool search(word) Returns true if there is any string in the data structure
 * that matches word or false otherwise. word may contain dots '.' where dots
 * can be matched with any letter.
 * 
 * 
 * Example:
 * Input
 * ["WordDictionary","addWord","addWord","addWord","search","search","search",
 * "search"]
 * [[],["bad"],["dad"],["mad"],["pad"],["bad"],[".ad"],["b.."]]
 * Output
 * [null,null,null,null,false,true,true,true]
 * 
 * Explanation
 * WordDictionary wordDictionary = new WordDictionary();
 * wordDictionary.addWord("bad");
 * wordDictionary.addWord("dad");
 * wordDictionary.addWord("mad");
 * wordDictionary.search("pad"); // return False
 * wordDictionary.search("bad"); // return True
 * wordDictionary.search(".ad"); // return True
 * wordDictionary.search("b.."); // return True
 */

/**
 * WordDictionary class that supports adding words and searching with wildcard
 * patterns
 * Uses Trie data structure with support for '.' wildcard matching
 */
class WordDictionary {

    /**
     * TrieNode class for internal representation
     */
    private static class TrieNode {
        private TrieNode[] children;
        private boolean isEndOfWord;

        public TrieNode() {
            children = new TrieNode[26]; // For lowercase a-z
            isEndOfWord = false;
        }
    }

    private TrieNode root;

    /**
     * Initialize your data structure here.
     */
    public WordDictionary() {
        root = new TrieNode();
    }

    /**
     * Adds a word into the data structure.
     * Time Complexity: O(m) where m is the length of the word
     * Space Complexity: O(m) in worst case
     */
    public void addWord(String word) {
        if (word == null || word.isEmpty()) {
            return;
        }

        TrieNode current = root;

        for (char ch : word.toCharArray()) {
            int index = ch - 'a';

            // Validate that character is lowercase letter
            if (index < 0 || index > 25) {
                throw new IllegalArgumentException("Only lowercase letters a-z are supported");
            }

            if (current.children[index] == null) {
                current.children[index] = new TrieNode();
            }
            current = current.children[index];
        }

        current.isEndOfWord = true;
    }

    /**
     * Returns if the word is in the data structure.
     * A word could contain the dot character '.' to represent any one letter.
     * Time Complexity: O(m * 26^k) where m is word length and k is number of dots
     * Space Complexity: O(m) for recursion stack
     */
    public boolean search(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }

        return searchHelper(word, 0, root);
    }

    /**
     * Recursive helper method for searching with wildcard support
     */
    private boolean searchHelper(String word, int index, TrieNode node) {
        // Base case: reached end of word
        if (index == word.length()) {
            return node.isEndOfWord;
        }

        char ch = word.charAt(index);

        if (ch == '.') {
            // Wildcard: try all possible children
            for (int i = 0; i < 26; i++) {
                if (node.children[i] != null) {
                    if (searchHelper(word, index + 1, node.children[i])) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            // Regular character
            int charIndex = ch - 'a';

            // Validate character
            if (charIndex < 0 || charIndex > 25) {
                return false;
            }

            if (node.children[charIndex] == null) {
                return false;
            }

            return searchHelper(word, index + 1, node.children[charIndex]);
        }
    }

    /**
     * Additional utility methods for enhanced functionality
     */

    /**
     * Get all words that match a given pattern
     */
    public List<String> getAllMatches(String pattern) {
        List<String> matches = new ArrayList<>();
        if (pattern != null && !pattern.isEmpty()) {
            findAllMatches(pattern, 0, root, new StringBuilder(), matches);
        }
        return matches;
    }

    /**
     * Helper method to find all matches for a pattern
     */
    private void findAllMatches(String pattern, int index, TrieNode node,
            StringBuilder current, List<String> matches) {
        if (index == pattern.length()) {
            if (node.isEndOfWord) {
                matches.add(current.toString());
            }
            return;
        }

        char ch = pattern.charAt(index);

        if (ch == '.') {
            // Try all possible characters
            for (int i = 0; i < 26; i++) {
                if (node.children[i] != null) {
                    current.append((char) ('a' + i));
                    findAllMatches(pattern, index + 1, node.children[i], current, matches);
                    current.deleteCharAt(current.length() - 1);
                }
            }
        } else {
            int charIndex = ch - 'a';
            if (charIndex >= 0 && charIndex < 26 && node.children[charIndex] != null) {
                current.append(ch);
                findAllMatches(pattern, index + 1, node.children[charIndex], current, matches);
                current.deleteCharAt(current.length() - 1);
            }
        }
    }

    /**
     * Check if any word exists with the given pattern
     * More efficient than search when you just need boolean result
     */
    public boolean hasMatch(String pattern) {
        return search(pattern);
    }

    /**
     * Get count of words that match the pattern
     */
    public int countMatches(String pattern) {
        return getAllMatches(pattern).size();
    }

    /**
     * Display all words in the dictionary (for debugging)
     */
    public void displayAllWords() {
        List<String> allWords = new ArrayList<>();
        collectAllWords(root, new StringBuilder(), allWords);
        System.out.println("All words in dictionary: " + allWords);
    }

    /**
     * Helper to collect all words
     */
    private void collectAllWords(TrieNode node, StringBuilder current, List<String> words) {
        if (node.isEndOfWord) {
            words.add(current.toString());
        }

        for (int i = 0; i < 26; i++) {
            if (node.children[i] != null) {
                current.append((char) ('a' + i));
                collectAllWords(node.children[i], current, words);
                current.deleteCharAt(current.length() - 1);
            }
        }
    }

    /**
     * Main method demonstrating the functionality
     */
    public static void main(String[] args) {
        System.out.println("=== WordDictionary Demo ===");

        WordDictionary wordDictionary = new WordDictionary();

        // Test the example from the problem
        System.out.println("\n1. Adding words: bad, dad, mad");
        wordDictionary.addWord("bad");
        wordDictionary.addWord("dad");
        wordDictionary.addWord("mad");

        // Display all words
        wordDictionary.displayAllWords();

        System.out.println("\n2. Testing searches:");

        // Test exact matches
        System.out.println("search('pad'): " + wordDictionary.search("pad")); // false
        System.out.println("search('bad'): " + wordDictionary.search("bad")); // true

        // Test wildcard patterns
        System.out.println("search('.ad'): " + wordDictionary.search(".ad")); // true
        System.out.println("search('b..'): " + wordDictionary.search("b..")); // true

        // Additional test cases
        System.out.println("\n3. Additional test cases:");
        System.out.println("search('..d'): " + wordDictionary.search("..d")); // true
        System.out.println("search('.'): " + wordDictionary.search(".")); // false
        System.out.println("search('....'): " + wordDictionary.search("....")); // false
        System.out.println("search('...'): " + wordDictionary.search("...")); // true

        // Test getAllMatches functionality
        System.out.println("\n4. Get all matches for patterns:");
        System.out.println("Matches for '.ad': " + wordDictionary.getAllMatches(".ad"));
        System.out.println("Matches for 'b..': " + wordDictionary.getAllMatches("b.."));
        System.out.println("Matches for '...': " + wordDictionary.getAllMatches("..."));

        // Add more words for comprehensive testing
        System.out.println("\n5. Adding more words for comprehensive testing:");
        wordDictionary.addWord("cat");
        wordDictionary.addWord("car");
        wordDictionary.addWord("care");
        wordDictionary.addWord("bat");
        wordDictionary.addWord("ball");

        wordDictionary.displayAllWords();

        System.out.println("\n6. Testing with new words:");
        System.out.println("search('ca.'): " + wordDictionary.search("ca.")); // true
        System.out.println("search('ca..'): " + wordDictionary.search("ca..")); // true
        System.out.println("search('b..'): " + wordDictionary.search("b..")); // true
        System.out.println("search('ba..'): " + wordDictionary.search("ba..")); // true

        System.out.println("\nMatches for 'ca.': " + wordDictionary.getAllMatches("ca."));
        System.out.println("Matches for 'b..': " + wordDictionary.getAllMatches("b.."));

        // Test count functionality
        System.out.println("\n7. Count matches:");
        System.out.println("Count for 'ca.': " + wordDictionary.countMatches("ca."));
        System.out.println("Count for 'b..': " + wordDictionary.countMatches("b.."));
        System.out.println("Count for '...': " + wordDictionary.countMatches("..."));

        // Edge cases
        System.out.println("\n8. Edge cases:");
        wordDictionary.addWord("a");
        System.out.println("search('.'): " + wordDictionary.search(".")); // true (matches 'a')
        System.out.println("search('a'): " + wordDictionary.search("a")); // true

        // Performance demonstration
        System.out.println("\n9. Performance test with many words:");
        WordDictionary perfTest = new WordDictionary();

        // Add many words
        String[] words = { "apple", "application", "apply", "banana", "band", "bandana",
                "cat", "car", "care", "careful", "card", "carnival" };

        for (String word : words) {
            perfTest.addWord(word);
        }

        System.out.println("Added " + words.length + " words");
        System.out.println("search('app..'): " + perfTest.search("app.."));
        System.out.println("search('car.'): " + perfTest.search("car."));
        System.out.println("search('.....'): " + perfTest.search("....."));

        System.out.println("All matches for 'car.': " + perfTest.getAllMatches("car."));
    }
}

/**
 * Alternative implementation using HashMap for different trade-offs
 * This version groups words by length for potentially better performance with
 * many dots
 */
class WordDictionaryAlternative {

    private Map<Integer, Set<String>> wordsByLength;

    public WordDictionaryAlternative() {
        wordsByLength = new HashMap<>();
    }

    public void addWord(String word) {
        if (word == null || word.isEmpty())
            return;

        int len = word.length();
        wordsByLength.computeIfAbsent(len, k -> new HashSet<>()).add(word);
    }

    public boolean search(String word) {
        if (word == null || word.isEmpty())
            return false;

        int len = word.length();
        Set<String> candidates = wordsByLength.get(len);

        if (candidates == null)
            return false;

        for (String candidate : candidates) {
            if (matches(word, candidate)) {
                return true;
            }
        }

        return false;
    }

    private boolean matches(String pattern, String word) {
        if (pattern.length() != word.length())
            return false;

        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.charAt(i) != '.' && pattern.charAt(i) != word.charAt(i)) {
                return false;
            }
        }

        return true;
    }

}

class WordDictionary2 {

    private static class TrieNode {
        TrieNode[] children = new TrieNode[26];
        boolean isEndOfWord = false;
    }

    private final TrieNode root;

    public WordDictionary2() {
        root = new TrieNode();
    }

    // 🔹 Add a word to the trie
    public void addWord(String word) {
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            int index = ch - 'a';
            if (node.children[index] == null)
                node.children[index] = new TrieNode();
            node = node.children[index];
        }
        node.isEndOfWord = true;
    }

    // 🔍 Search with support for '.'
    public boolean search(String word) {
        return dfs(word.toCharArray(), 0, root);
    }

    // 🔄 DFS helper method
    private boolean dfs(char[] chars, int pos, TrieNode node) {
        if (node == null)
            return false;

        if (pos == chars.length)
            return node.isEndOfWord;

        char ch = chars[pos];

        if (ch == '.') {
            // Try all 26 children
            for (TrieNode child : node.children) {
                if (child != null && dfs(chars, pos + 1, child)) {
                    return true;
                }
            }
            return false;
        } else {
            int index = ch - 'a';
            return dfs(chars, pos + 1, node.children[index]);
        }
    }

}
