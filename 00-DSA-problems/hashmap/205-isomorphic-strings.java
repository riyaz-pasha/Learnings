import java.util.*;
/*
 * Given two strings s and t, determine if they are isomorphic.
 * 
 * Two strings s and t are isomorphic if the characters in s can be replaced to
 * get t.
 * 
 * All occurrences of a character must be replaced with another character while
 * preserving the order of characters. No two characters may map to the same
 * character, but a character may map to itself.
 * 
 * Example 1:
 * Input: s = "egg", t = "add"
 * Output: true
 * Explanation:
 * The strings s and t can be made identical by:
 * Mapping 'e' to 'a'.
 * Mapping 'g' to 'd'.
 * 
 * Example 2:
 * Input: s = "foo", t = "bar"
 * Output: false
 * Explanation:
 * The strings s and t can not be made identical as 'o' needs to be mapped to
 * both 'a' and 'r'.
 * 
 * Example 3:
 * Input: s = "paper", t = "title"
 * Output: true
 */

class IsomorphicStrings {

    // Solution 1: Two HashMaps - Most intuitive approach
    // Time: O(n), Space: O(k) where k is unique characters
    public boolean isIsomorphic1(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        Map<Character, Character> mapS = new HashMap<>();
        Map<Character, Character> mapT = new HashMap<>();

        for (int i = 0; i < s.length(); i++) {
            char sChar = s.charAt(i);
            char tChar = t.charAt(i);

            // Check mapping from s to t
            if (mapS.containsKey(sChar)) {
                if (mapS.get(sChar) != tChar) {
                    return false;
                }
            } else {
                mapS.put(sChar, tChar);
            }

            // Check mapping from t to s (ensure bijection)
            if (mapT.containsKey(tChar)) {
                if (mapT.get(tChar) != sChar) {
                    return false;
                }
            } else {
                mapT.put(tChar, sChar);
            }
        }

        return true;
    }

    // Solution 2: Single HashMap with Set - Space optimized
    // Time: O(n), Space: O(k)
    public boolean isIsomorphic2(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        Map<Character, Character> mapping = new HashMap<>();
        Set<Character> mapped = new HashSet<>();

        for (int i = 0; i < s.length(); i++) {
            char sChar = s.charAt(i);
            char tChar = t.charAt(i);

            if (mapping.containsKey(sChar)) {
                if (mapping.get(sChar) != tChar) {
                    return false;
                }
            } else {
                // Check if tChar is already mapped to another character
                if (mapped.contains(tChar)) {
                    return false;
                }
                mapping.put(sChar, tChar);
                mapped.add(tChar);
            }
        }

        return true;
    }

    // Solution 3: Array-based mapping (for ASCII characters)
    // Time: O(n), Space: O(1) - constant space for 256 ASCII chars
    public boolean isIsomorphic3(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        int[] mapS = new int[256]; // ASCII character mapping
        int[] mapT = new int[256];

        // Initialize with -1 (no mapping)
        Arrays.fill(mapS, -1);
        Arrays.fill(mapT, -1);

        for (int i = 0; i < s.length(); i++) {
            char sChar = s.charAt(i);
            char tChar = t.charAt(i);

            if (mapS[sChar] == -1 && mapT[tChar] == -1) {
                // Create new mapping
                mapS[sChar] = tChar;
                mapT[tChar] = sChar;
            } else if (mapS[sChar] != tChar || mapT[tChar] != sChar) {
                return false;
            }
        }

        return true;
    }

    // Solution 4: Pattern matching approach
    // Time: O(n), Space: O(k)
    public boolean isIsomorphic4(String s, String t) {
        return getPattern(s).equals(getPattern(t));
    }

    private String getPattern(String str) {
        Map<Character, Integer> charToIndex = new HashMap<>();
        StringBuilder pattern = new StringBuilder();
        int index = 0;

        for (char c : str.toCharArray()) {
            if (!charToIndex.containsKey(c)) {
                charToIndex.put(c, index++);
            }
            pattern.append(charToIndex.get(c)).append(",");
        }

        return pattern.toString();
    }

    // Solution 5: First occurrence index comparison
    // Time: O(n), Space: O(k)
    public boolean isIsomorphic5(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        Map<Character, Integer> mapS = new HashMap<>();
        Map<Character, Integer> mapT = new HashMap<>();

        for (int i = 0; i < s.length(); i++) {
            char sChar = s.charAt(i);
            char tChar = t.charAt(i);

            // Get the first occurrence index of each character
            Integer sIndex = mapS.put(sChar, i);
            Integer tIndex = mapT.put(tChar, i);

            // If first occurrence, both should be null
            // If not first occurrence, both indices should be equal
            if (!Objects.equals(sIndex, tIndex)) {
                return false;
            }
        }

        return true;
    }

    // Solution 6: Optimized array approach with early termination
    // Time: O(n), Space: O(1)
    public boolean isIsomorphic6(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        char[] mapS = new char[256];
        char[] mapT = new char[256];

        for (int i = 0; i < s.length(); i++) {
            char sChar = s.charAt(i);
            char tChar = t.charAt(i);

            if (mapS[sChar] == 0 && mapT[tChar] == 0) {
                mapS[sChar] = tChar;
                mapT[tChar] = sChar;
            } else if (mapS[sChar] != tChar || mapT[tChar] != sChar) {
                return false;
            }
        }

        return true;
    }

    // Test method
    public static void main(String[] args) {
        IsomorphicStrings solution = new IsomorphicStrings();

        // Test cases
        System.out.println(solution.isIsomorphic3("egg", "add")); // true
        System.out.println(solution.isIsomorphic3("foo", "bar")); // false
        System.out.println(solution.isIsomorphic3("paper", "title")); // true
        System.out.println(solution.isIsomorphic3("ab", "aa")); // false
        System.out.println(solution.isIsomorphic3("ab", "ca")); // true
        System.out.println(solution.isIsomorphic3("badc", "baba")); // false
    }

}

/*
 * Analysis of Solutions:
 * 
 * 1. Two HashMaps (isIsomorphic1):
 * - Most intuitive and clear logic
 * - Explicitly handles bidirectional mapping
 * - Good for interviews to show understanding
 * 
 * 2. HashMap + Set (isIsomorphic2):
 * - Slight space optimization
 * - Single mapping direction with set for reverse check
 * - Clear logic flow
 * 
 * 3. Array Mapping (isIsomorphic3):
 * - Most efficient for ASCII characters
 * - Constant space complexity O(1)
 * - Best performance for typical use cases
 * 
 * 4. Pattern Matching (isIsomorphic4):
 * - Creative approach using pattern comparison
 * - Converts strings to normalized patterns
 * - Higher space complexity but interesting concept
 * 
 * 5. First Occurrence (isIsomorphic5):
 * - Elegant solution using index comparison
 * - Leverages Map.put() return value
 * - Compact and efficient
 * 
 * 6. Optimized Array (isIsomorphic6):
 * - Similar to solution 3 but cleaner
 * - Uses default char value (0) for initialization
 * - Best performance overall
 * 
 * Key Insights:
 * - Must check both directions: s->t and t->s mapping
 * - Characters can map to themselves
 * - One-to-one correspondence is required (bijection)
 * - Array approach is fastest for ASCII/limited character sets
 * 
 * Recommendation: Use isIsomorphic3 or isIsomorphic6 for best performance,
 * or isIsomorphic1 for clarity in interviews.
 */
