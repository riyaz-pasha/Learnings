import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class ValidAnagram {

    /**
     * Approach 1: Sorting
     * Time Complexity: O(n log n), Space Complexity: O(n)
     * Most straightforward approach
     */
    public boolean isAnagram1(String s, String t) {
        // Different lengths cannot be anagrams
        if (s.length() != t.length()) {
            return false;
        }

        // Convert to char arrays and sort
        char[] sArr = s.toCharArray();
        char[] tArr = t.toCharArray();

        Arrays.sort(sArr);
        Arrays.sort(tArr);

        // Compare sorted arrays
        return Arrays.equals(sArr, tArr);
    }

    /**
     * Approach 2: Hash Map (Frequency Counter)
     * Time Complexity: O(n), Space Complexity: O(n)
     * More efficient for longer strings
     */
    public boolean isAnagram2(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        Map<Character, Integer> count = new HashMap<>();

        // Count characters in s
        for (char c : s.toCharArray()) {
            count.put(c, count.getOrDefault(c, 0) + 1);
        }

        // Decrease count for characters in t
        for (char c : t.toCharArray()) {
            if (!count.containsKey(c)) {
                return false;
            }
            count.put(c, count.get(c) - 1);
            if (count.get(c) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Approach 3: Array Counter (for lowercase letters)
     * Time Complexity: O(n), Space Complexity: O(1) - fixed size array
     * Most efficient for English letters only
     */
    public boolean isAnagram3(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        int[] count = new int[26]; // For 'a' to 'z'

        // Increment for s, decrement for t
        for (int i = 0; i < s.length(); i++) {
            count[s.charAt(i) - 'a']++;
            count[t.charAt(i) - 'a']--;
        }

        // Check if all counts are zero
        for (int c : count) {
            if (c != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Approach 4: Array Counter with Single Pass Check
     * Time Complexity: O(n), Space Complexity: O(1)
     * Optimized version of approach 3
     */
    public boolean isAnagram4(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        int[] count = new int[26];

        // Count frequency of characters in s
        for (char c : s.toCharArray()) {
            count[c - 'a']++;
        }

        // Decrease count for characters in t
        for (char c : t.toCharArray()) {
            count[c - 'a']--;
            // Early return if count goes negative
            if (count[c - 'a'] < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Approach 5: Array Counter for Unicode (any characters)
     * Time Complexity: O(n), Space Complexity: O(1) - fixed size for extended ASCII
     * Works for any ASCII characters, not just lowercase letters
     */
    public boolean isAnagram5(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        int[] count = new int[256]; // Extended ASCII

        for (int i = 0; i < s.length(); i++) {
            count[s.charAt(i)]++;
            count[t.charAt(i)]--;
        }

        for (int c : count) {
            if (c != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Approach 6: Using Stream API (Modern Java)
     * Time Complexity: O(n log n), Space Complexity: O(n)
     * Functional programming style
     */
    public boolean isAnagram6(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        String sortedS = s.chars()
                .sorted()
                .collect(StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append)
                .toString();

        String sortedT = t.chars()
                .sorted()
                .collect(StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append)
                .toString();

        return sortedS.equals(sortedT);
    }

    // Test method
    public static void main(String[] args) {
        ValidAnagram solution = new ValidAnagram();

        // Test Case 1
        System.out.println("Test Case 1:");
        System.out.println("s = \"anagram\", t = \"nagaram\"");
        System.out.println("Result: " + solution.isAnagram3("anagram", "nagaram")); // true

        // Test Case 2
        System.out.println("\nTest Case 2:");
        System.out.println("s = \"rat\", t = \"car\"");
        System.out.println("Result: " + solution.isAnagram3("rat", "car")); // false

        // Test Case 3
        System.out.println("\nTest Case 3:");
        System.out.println("s = \"listen\", t = \"silent\"");
        System.out.println("Result: " + solution.isAnagram3("listen", "silent")); // true

        // Test Case 4
        System.out.println("\nTest Case 4:");
        System.out.println("s = \"a\", t = \"ab\"");
        System.out.println("Result: " + solution.isAnagram3("a", "ab")); // false

        // Performance comparison
        System.out.println("\n--- Performance Comparison ---");
        String testS = "thequickbrownfoxjumpsoverthelazydog";
        String testT = "dogzylehtrevopmujxofnworbkciuqeht";

        long start = System.nanoTime();
        boolean result1 = solution.isAnagram1(testS, testT);
        long time1 = System.nanoTime() - start;

        start = System.nanoTime();
        boolean result2 = solution.isAnagram2(testS, testT);
        long time2 = System.nanoTime() - start;

        start = System.nanoTime();
        boolean result3 = solution.isAnagram3(testS, testT);
        long time3 = System.nanoTime() - start;

        System.out.println("Sorting: " + result1 + " (" + time1 + " ns)");
        System.out.println("HashMap: " + result2 + " (" + time2 + " ns)");
        System.out.println("Array Counter: " + result3 + " (" + time3 + " ns)");
    }
}
