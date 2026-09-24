import java.util.*;
import java.util.stream.IntStream;

/**
 * ============================================================================
 * VERIFYING AN ALIEN DICTIONARY
 * ============================================================================
 * 
 * STATEMENT:
 * In an alien language, the alphabet consists of the same lowercase English 
 * letters but arranged in a different order. Given a list of words written in 
 * this alien language, and a string 'order' representing the order of the alien 
 * alphabet, return TRUE if the words are sorted lexicographically according to 
 * 'order'; otherwise, return FALSE.
 * 
 * Lexicographical rules:
 * 1. At the first position where the two words differ, the character in the 
 *    first word must come before the character in the second word in 'order'.
 * 2. If one word is a prefix of the other, the shorter word must come first 
 *    (e.g., "app" comes before "apple").
 * 
 * ----------------------------------------------------------------------------
 * RESTATING THE PROBLEM:
 * We need to verify if an array of strings is perfectly sorted. However, instead 
 * of using the standard A-Z alphabet dictionary, we are given a custom, scrambled 
 * alphabet. If we simply compare every adjacent pair of words (word[i] and word[i+1]) 
 * and ensure that word[i] <= word[i+1] according to this new alphabet, the entire 
 * list is sorted.
 * 
 * ----------------------------------------------------------------------------
 * CLARIFYING QUESTIONS FOR THE INTERVIEWER (4-10):
 * 1. Q: Will the 'order' string always contain exactly 26 unique lowercase English letters?
 *    A: Yes, the constraints specify it's a permutation of the 26 lowercase letters.
 * 2. Q: What should I return if the 'words' array has only 1 word?
 *    A: A single-element array is trivially sorted, so return TRUE.
 * 3. Q: Can there be duplicate words in the input array?
 *    A: Yes. Duplicates are naturally sorted, so "apple" followed by "apple" is valid.
 * 4. Q: How should we handle the prefix rule if the longer word appears first?
 *    A: For example, if "apple" comes before "app", we immediately return FALSE 
 *       because "app" is shorter and the shared prefix characters match completely.
 * 5. Q: Are there any spaces, numbers, or uppercase letters in the words?
 *    A: No, constraints specify only lowercase English letters are used.
 * 
 * ----------------------------------------------------------------------------
 * HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * 1. Acknowledge the core logic: "To verify if any list is sorted, we only need 
 *    to compare adjacent elements. If every pair (A, B) is sorted, the whole list is sorted."
 * 2. State the bottleneck: Comparing characters using `indexOf()` on the 'order' 
 *    string is O(26) or O(N) per character, which is slow.
 * 3. Propose the optimization: "We can build a mapping of the alien alphabet 
 *    using an integer array of size 26. This gives us O(1) character lookups."
 * 4. Discuss Edge Cases aloud: "I must explicitly handle the prefix edge case 
 *    where a longer word comes before a shorter word (e.g., 'apple', 'app')."
 * 5. Offer two implementations: 
 *    - Approach 1: The standard imperative approach (fastest, O(N * M) time).
 *    - Approach 2: A modern Java approach using custom Comparators and Streams 
 *      (shows strong language knowledge).
 * 
 * ============================================================================
 */
public class AlienDictionaryVerifier {

    /**
     * ========================================================================
     * SOLUTION 1: IMPERATIVE ADJACENT COMPARISON (Most Optimal)
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * We don't need to sort the array and compare it to the original; we just 
     * need to validate that no two adjacent words are out of order.
     * 
     * We map each character in the 'order' string to its index (rank).
     * For example, if order = "hlabc...", then map['h' - 'a'] = 0, map['l' - 'a'] = 1.
     * We then iterate through adjacent word pairs. We compare their characters 
     * one by one:
     * - If char1 < char2 (in alien order), this pair is sorted! Break and check the next pair.
     * - If char1 > char2, the list is NOT sorted. Return false immediately.
     * - If they are equal, continue checking the next character.
     * 
     * KEY OBSERVATIONS (Prefix Rule):
     * If we finish looking at all characters of the shorter word and haven't 
     * found a difference, we must check their lengths. If word1 is longer than 
     * word2 (e.g., word1="apple", word2="app"), it's out of order!
     * 
     * VISUAL TRACING:
     * words = ["hello", "leetcode"], order = "hlabcdefgijkmnopqrstuvwxyz"
     * 
     * 1. Build Map: 'h'=0, 'l'=1, 'a'=2, 'b'=3...
     * 2. Compare words[0]="hello" and words[1]="leetcode":
     *    - Index 0: 'h' vs 'l'. 
     *    - Map lookup: 'h' is rank 0, 'l' is rank 1.
     *    - 0 < 1. The pair is sorted! Break inner loop.
     * 3. End of array. Return TRUE.
     * 
     * Complexity:
     * Time: O(N * M) where N is number of words, M is max length of a word.
     * Space: O(1) because the mapping array is always size 26.
     */
    public boolean isAlienSorted(String[] words, String order) {
        // Step 1: Build the alien dictionary mapping
        int[] alienRank = new int[26];
        for (int i = 0; i < order.length(); i++) {
            alienRank[order.charAt(i) - 'a'] = i;
        }

        // Step 2: Compare each adjacent pair of words
        for (int i = 0; i < words.length - 1; i++) {
            String word1 = words[i];
            String word2 = words[i + 1];
            
            int length1 = word1.length();
            int length2 = word2.length();
            int minLength = Math.min(length1, length2);
            boolean foundDifference = false;
            
            // Compare characters up to the length of the shorter word
            for (int j = 0; j < minLength; j++) {
                char c1 = word1.charAt(j);
                char c2 = word2.charAt(j);
                
                if (c1 != c2) {
                    // If out of order, return false
                    if (alienRank[c1 - 'a'] > alienRank[c2 - 'a']) {
                        return false;
                    }
                    // If in order, this pair is safe. Move to the next word pair.
                    foundDifference = true;
                    break;
                }
            }
            
            // If all compared characters were the same, but the first word is longer,
            // it violates the prefix rule (e.g., "apple" before "app").
            if (!foundDifference && length1 > length2) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * ========================================================================
     * SOLUTION 2: MODERN JAVA WITH CUSTOM COMPARATOR & STREAMS
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * Instead of writing manual loops, we can encapsulate the alien lexicographical 
     * rules inside a standard Java `Comparator<String>`. 
     * Once we have this comparator, we can use Java Streams to elegantly check if 
     * all adjacent pairs satisfy `comparator.compare(w1, w2) <= 0`.
     * 
     * While slightly slower due to Lambda/Stream overhead, this shows excellent 
     * command of modern Java APIs and Object-Oriented Design principles.
     */
    public boolean isAlienSortedModern(String[] words, String order) {
        // Build rank map
        int[] alienRank = new int[26];
        for (int i = 0; i < order.length(); i++) {
            alienRank[order.charAt(i) - 'a'] = i;
        }
        
        // Define Custom Comparator
        Comparator<String> alienComparator = (w1, w2) -> {
            int minLen = Math.min(w1.length(), w2.length());
            for (int i = 0; i < minLen; i++) {
                char c1 = w1.charAt(i);
                char c2 = w2.charAt(i);
                if (c1 != c2) {
                    return Integer.compare(alienRank[c1 - 'a'], alienRank[c2 - 'a']);
                }
            }
            // If prefix matches, shorter word is smaller
            return Integer.compare(w1.length(), w2.length());
        };
        
        // Use IntStream to compare all adjacent pairs
        return IntStream.range(0, words.length - 1)
                        .allMatch(i -> alienComparator.compare(words[i], words[i + 1]) <= 0);
    }

    /**
     * ========================================================================
     * MAIN METHOD & TESTING
     * ========================================================================
     * Utilizing Java Records for clean test case definitions.
     */
    
    record TestCase(String[] words, String order, boolean expected, String description) {}

    public static void main(String[] args) {
        AlienDictionaryVerifier verifier = new AlienDictionaryVerifier();

        var testCases = List.of(
            new TestCase(
                new String[]{"hello", "leetcode"}, 
                "hlabcdefgijkmnopqrstuvwxyz", 
                true, 
                "Standard sorted words"
            ),
            new TestCase(
                new String[]{"word", "world", "row"}, 
                "worldabcefghijkmnpqstuvxyz", 
                false, 
                "'d' comes after 'l' in alien order"
            ),
            new TestCase(
                new String[]{"apple", "app"}, 
                "abcdefghijklmnopqrstuvwxyz", 
                false, 
                "Prefix rule violation (longer word first)"
            ),
            new TestCase(
                new String[]{"app", "apple"}, 
                "abcdefghijklmnopqrstuvwxyz", 
                true, 
                "Prefix rule valid (shorter word first)"
            ),
            new TestCase(
                new String[]{"kuvp", "q"}, 
                "ngxlkthsjuoqcpavbfdermiywz", 
                true, 
                "Different lengths, sorted based on first character"
            )
        );

        System.out.println("Running test cases for Alien Dictionary Verifier...");
        System.out.println("-".repeat(75));

        IntStream.range(0, testCases.size()).forEach(i -> {
            var tc = testCases.get(i);
            
            boolean res1 = verifier.isAlienSorted(tc.words(), tc.order());
            boolean res2 = verifier.isAlienSortedModern(tc.words(), tc.order());
            
            boolean passed = (res1 == tc.expected()) && (res2 == tc.expected());
            
            System.out.printf("Test Case %d: %s\n", i + 1, tc.description());
            System.out.printf("Words: %s\n", Arrays.toString(tc.words()));
            System.out.printf("Expected: %b | Sol1: %b | Sol2 (Stream): %b\n", 
                              tc.expected(), res1, res2);
            System.out.printf("Status: %s\n", passed ? "✅ PASSED" : "❌ FAILED");
            System.out.println("-".repeat(75));
        });
    }
}
