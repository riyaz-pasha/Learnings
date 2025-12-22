import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class IsomorphicStrings {

    /**
     * Approach 1: Using Two HashMaps
     * Time Complexity: O(n), Space Complexity: O(n)
     * Most intuitive approach using two mappings
     */
    public boolean isIsomorphic1(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        Map<Character, Character> sToT = new HashMap<>();
        Map<Character, Character> tToS = new HashMap<>();

        for (int i = 0; i < s.length(); i++) {
            char c1 = s.charAt(i);
            char c2 = t.charAt(i);

            // Check s -> t mapping
            if (sToT.containsKey(c1)) {
                if (sToT.get(c1) != c2) {
                    return false;
                }
            } else {
                sToT.put(c1, c2);
            }

            // Check t -> s mapping (ensures one-to-one)
            if (tToS.containsKey(c2)) {
                if (tToS.get(c2) != c1) {
                    return false;
                }
            } else {
                tToS.put(c2, c1);
            }
        }

        return true;
    }

    /**
     * Approach 2: Using Arrays (for ASCII characters)
     * Time Complexity: O(n), Space Complexity: O(1) - fixed size arrays
     * More efficient for ASCII characters
     */
    public boolean isIsomorphic2(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        int[] sToT = new int[256]; // Mapping from s to t
        int[] tToS = new int[256]; // Mapping from t to s

        for (int i = 0; i < s.length(); i++) {
            char c1 = s.charAt(i);
            char c2 = t.charAt(i);

            // Check if mapping exists and is consistent
            if (sToT[c1] != 0) {
                if (sToT[c1] != c2) {
                    return false;
                }
            } else {
                sToT[c1] = c2;
            }

            if (tToS[c2] != 0) {
                if (tToS[c2] != c1) {
                    return false;
                }
            } else {
                tToS[c2] = c1;
            }
        }

        return true;
    }

    /**
     * Approach 3: Using First Occurrence Indices
     * Time Complexity: O(n), Space Complexity: O(n)
     * Clever approach comparing first occurrence patterns
     */
    public boolean isIsomorphic3(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        Map<Character, Integer> sMap = new HashMap<>();
        Map<Character, Integer> tMap = new HashMap<>();

        for (int i = 0; i < s.length(); i++) {
            // If both characters appear for the first time at different indices
            // or if they appeared before but at different relative positions
            Integer sIndex = sMap.put(s.charAt(i), i);
            Integer tIndex = tMap.put(t.charAt(i), i);

            // Compare the previous indices (null if first occurrence)
            if (!Objects.equals(sIndex, tIndex)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Approach 4: Using Single HashMap with Value Check
     * Time Complexity: O(n), Space Complexity: O(n)
     * Uses one map but checks values to ensure bijection
     */
    public boolean isIsomorphic4(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        Map<Character, Character> map = new HashMap<>();

        for (int i = 0; i < s.length(); i++) {
            char c1 = s.charAt(i);
            char c2 = t.charAt(i);

            if (map.containsKey(c1)) {
                if (map.get(c1) != c2) {
                    return false;
                }
            } else {
                // Check if c2 is already mapped to a different character
                if (map.containsValue(c2)) {
                    return false;
                }
                map.put(c1, c2);
            }
        }

        return true;
    }

    /*
     * Two strings are isomorphic if each character in the first string consistently
     * maps to exactly one character in the second string, and vice versa.
     *
     * We check this by comparing the pattern of character occurrences rather than
     * the characters themselves.
     *
     * Use two arrays of size 256 to store the last index where each character
     * appeared in each string.
     *
     * As we scan both strings from left to right:
     * - If the last-seen index of the current characters does not match, the
     * mapping is inconsistent, so return false.
     * - Otherwise, update both characters’ last-seen index to the current position.
     *
     * If all positions match, the strings are isomorphic.
     *
     * Time Complexity: O(N), where N is the length of the strings.
     * Space Complexity: O(1), since the arrays have a fixed size (256).
     */

    public boolean isIsomorphic(String s, String t) {
        // Arrays to track last seen positions of characters in s and t
        int[] m1 = new int[256], m2 = new int[256];

        // Get length of the strings
        int n = s.length();

        // Loop through all characters in the strings
        for (int i = 0; i < n; ++i) {
            // Return false if mapping is inconsistent
            if (m1[s.charAt(i)] != m2[t.charAt(i)])
                return false;

            // Update last seen index for both characters
            m1[s.charAt(i)] = i + 1;
            m2[t.charAt(i)] = i + 1;
        }

        // Return true if all character mappings are consistent
        return true;
    }

    // Test method
    public static void main(String[] args) {
        IsomorphicStrings solution = new IsomorphicStrings();

        // Test cases
        System.out.println("Test Case 1:");
        System.out.println("s = \"egg\", t = \"add\"");
        System.out.println("Result: " + solution.isIsomorphic1("egg", "add")); // true

        System.out.println("\nTest Case 2:");
        System.out.println("s = \"foo\", t = \"bar\"");
        System.out.println("Result: " + solution.isIsomorphic1("foo", "bar")); // false

        System.out.println("\nTest Case 3:");
        System.out.println("s = \"paper\", t = \"title\"");
        System.out.println("Result: " + solution.isIsomorphic1("paper", "title")); // true

        System.out.println("\nTest Case 4:");
        System.out.println("s = \"badc\", t = \"baba\"");
        System.out.println("Result: " + solution.isIsomorphic1("badc", "baba")); // false
    }
}
