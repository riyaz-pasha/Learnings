import java.util.*;
/*
 * Given an array of strings strs, group the anagrams together. You can return
 * the answer in any order.
 * 
 * Example 1:
 * Input: strs = ["eat","tea","tan","ate","nat","bat"]
 * Output: [["bat"],["nat","tan"],["ate","eat","tea"]]
 * Explanation:
 * There is no string in strs that can be rearranged to form "bat".
 * The strings "nat" and "tan" are anagrams as they can be rearranged to form
 * each other.
 * The strings "ate", "eat", and "tea" are anagrams as they can be rearranged to
 * form each other.
 * 
 * Example 2:
 * Input: strs = [""]
 * Output: [[""]]
 * 
 * Example 3:
 * Input: strs = ["a"]
 * Output: [["a"]]
 */

class GroupAnagrams {

    // Solution 1: Sorting approach - Most intuitive
    // Time: O(n * k log k), Space: O(n * k)
    // where n = number of strings, k = average length of strings
    public List<List<String>> groupAnagrams1(String[] strs) {
        Map<String, List<String>> anagramGroups = new HashMap<>();

        for (String str : strs) {
            // Sort characters to create a key
            char[] chars = str.toCharArray();
            Arrays.sort(chars);
            String key = new String(chars);

            // Group strings by their sorted key
            anagramGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(str);
        }

        return new ArrayList<>(anagramGroups.values());
    }

    // Solution 2: Character frequency counting - More efficient
    // Time: O(n * k), Space: O(n * k)
    public List<List<String>> groupAnagrams2(String[] strs) {
        Map<String, List<String>> anagramGroups = new HashMap<>();

        for (String str : strs) {
            String key = getFrequencyKey(str);
            anagramGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(str);
        }

        return new ArrayList<>(anagramGroups.values());
    }

    private String getFrequencyKey(String str) {
        int[] count = new int[26];

        // Count character frequencies
        for (char c : str.toCharArray()) {
            count[c - 'a']++;
        }

        // Build frequency string
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            if (count[i] > 0) {
                key.append((char) ('a' + i)).append(count[i]);
            }
        }

        return key.toString();
    }

    // Solution 3: Prime number multiplication - Mathematical approach
    // Time: O(n * k), Space: O(n * k)
    // Note: Risk of overflow for very long strings
    public List<List<String>> groupAnagrams3(String[] strs) {
        // Prime numbers for each letter a-z
        long[] primes = { 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41,
                43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101 };

        Map<Long, List<String>> anagramGroups = new HashMap<>();

        for (String str : strs) {
            long key = 1;
            boolean overflow = false;

            for (char c : str.toCharArray()) {
                long oldKey = key;
                key *= primes[c - 'a'];

                // Simple overflow check
                if (key < oldKey) {
                    overflow = true;
                    break;
                }
            }

            if (overflow) {
                // Fallback to sorting approach for this string
                char[] chars = str.toCharArray();
                Arrays.sort(chars);
                String sortedKey = new String(chars);
                anagramGroups.computeIfAbsent(sortedKey.hashCode() + 1000000L,
                        k -> new ArrayList<>()).add(str);
            } else {
                anagramGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(str);
            }
        }

        return new ArrayList<>(anagramGroups.values());
    }

    // Solution 4: Compact frequency array approach
    // Time: O(n * k), Space: O(n * k)
    public List<List<String>> groupAnagrams4(String[] strs) {
        Map<String, List<String>> anagramGroups = new HashMap<>();

        for (String str : strs) {
            String key = getCompactFrequencyKey(str);
            anagramGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(str);
        }

        return new ArrayList<>(anagramGroups.values());
    }

    private String getCompactFrequencyKey(String str) {
        int[] count = new int[26];

        for (char c : str.toCharArray()) {
            count[c - 'a']++;
        }

        // Create a compact string representation
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            key.append('#').append(count[i]);
        }

        return key.toString();
    }

    // Solution 5: Using Arrays.toString() for frequency key
    // Time: O(n * k), Space: O(n * k)
    public List<List<String>> groupAnagrams5(String[] strs) {
        Map<String, List<String>> anagramGroups = new HashMap<>();

        for (String str : strs) {
            int[] count = new int[26];

            for (char c : str.toCharArray()) {
                count[c - 'a']++;
            }

            String key = Arrays.toString(count);
            anagramGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(str);
        }

        return new ArrayList<>(anagramGroups.values());
    }

    // Solution 6: Optimized sorting with StringBuilder
    // Time: O(n * k log k), Space: O(n * k)
    public List<List<String>> groupAnagrams6(String[] strs) {
        Map<String, List<String>> anagramGroups = new HashMap<>();

        for (String str : strs) {
            String key = getSortedKey(str);
            anagramGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(str);
        }

        return new ArrayList<>(anagramGroups.values());
    }

    private String getSortedKey(String str) {
        char[] chars = str.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    // Solution 7: Stream API approach - Functional style
    // Time: O(n * k log k), Space: O(n * k)
    public List<List<String>> groupAnagrams7(String[] strs) {
        return Arrays.stream(strs)
                .collect(Collectors.groupingBy(str -> {
                    char[] chars = str.toCharArray();
                    Arrays.sort(chars);
                    return new String(chars);
                }))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    // Solution 8: Custom hash function approach
    // Time: O(n * k), Space: O(n * k)
    public List<List<String>> groupAnagrams8(String[] strs) {
        Map<Integer, List<String>> anagramGroups = new HashMap<>();

        for (String str : strs) {
            int hash = getAnagramHash(str);
            anagramGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(str);
        }

        return new ArrayList<>(anagramGroups.values());
    }

    private int getAnagramHash(String str) {
        int[] count = new int[26];

        for (char c : str.toCharArray()) {
            count[c - 'a']++;
        }

        // Create hash from frequency array
        int hash = 0;
        for (int i = 0; i < 26; i++) {
            hash = hash * 31 + count[i];
        }

        return hash;
    }

    // Solution 9: Radix-based frequency encoding
    // Time: O(n * k), Space: O(n * k)
    public List<List<String>> groupAnagrams9(String[] strs) {
        Map<Long, List<String>> anagramGroups = new HashMap<>();

        for (String str : strs) {
            long key = getRadixKey(str);
            anagramGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(str);
        }

        return new ArrayList<>(anagramGroups.values());
    }

    private long getRadixKey(String str) {
        int[] count = new int[26];

        for (char c : str.toCharArray()) {
            count[c - 'a']++;
        }

        long key = 0;
        for (int i = 0; i < 26; i++) {
            key = key * 100 + count[i]; // Assuming max 99 occurrences per char
        }

        return key;
    }

    // Solution 10: Memory-optimized approach with string interning
    // Time: O(n * k log k), Space: O(n * k) but with better memory usage
    public List<List<String>> groupAnagrams10(String[] strs) {
        Map<String, List<String>> anagramGroups = new HashMap<>();

        for (String str : strs) {
            char[] chars = str.toCharArray();
            Arrays.sort(chars);
            String key = new String(chars).intern(); // String interning for memory

            anagramGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(str);
        }

        return new ArrayList<>(anagramGroups.values());
    }

    // Test method
    public static void main(String[] args) {
        GroupAnagrams solution = new GroupAnagrams();

        // Test case 1
        String[] strs1 = { "eat", "tea", "tan", "ate", "nat", "bat" };
        List<List<String>> result1 = solution.groupAnagrams2(strs1);
        System.out.println("Test 1: " + result1);

        // Test case 2
        String[] strs2 = { "" };
        List<List<String>> result2 = solution.groupAnagrams2(strs2);
        System.out.println("Test 2: " + result2);

        // Test case 3
        String[] strs3 = { "a" };
        List<List<String>> result3 = solution.groupAnagrams2(strs3);
        System.out.println("Test 3: " + result3);

        // Test case 4 - Edge cases
        String[] strs4 = { "abc", "bca", "cab", "xyz", "zyx", "yxz" };
        List<List<String>> result4 = solution.groupAnagrams2(strs4);
        System.out.println("Test 4: " + result4);
    }

}

/*
 * Analysis of Solutions:
 * 
 * 1. Sorting Approach (groupAnagrams1):
 * - Most intuitive and commonly used
 * - Easy to understand and implement
 * - Time: O(n * k log k), Space: O(n * k)
 * 
 * 2. Character Frequency (groupAnagrams2):
 * - More efficient than sorting
 * - Best overall performance for most cases
 * - Time: O(n * k), Space: O(n * k)
 * 
 * 3. Prime Multiplication (groupAnagrams3):
 * - Creative mathematical approach
 * - Risk of overflow with long strings
 * - Time: O(n * k), Space: O(n * k)
 * 
 * 4. Compact Frequency (groupAnagrams4):
 * - Optimized frequency key generation
 * - Good balance of efficiency and readability
 * - Time: O(n * k), Space: O(n * k)
 * 
 * 5. Arrays.toString() (groupAnagrams5):
 * - Simple but less efficient key generation
 * - Easy to implement
 * - Time: O(n * k), Space: O(n * k)
 * 
 * 6. Optimized Sorting (groupAnagrams6):
 * - Clean sorting implementation
 * - Good for readability
 * - Time: O(n * k log k), Space: O(n * k)
 * 
 * 7. Stream API (groupAnagrams7):
 * - Functional programming style
 * - Concise but less readable for some
 * - Time: O(n * k log k), Space: O(n * k)
 * 
 * 8. Custom Hash (groupAnagrams8):
 * - Fast hash-based grouping
 * - Risk of hash collisions
 * - Time: O(n * k), Space: O(n * k)
 * 
 * 9. Radix Encoding (groupAnagrams9):
 * - Efficient numeric encoding
 * - Limited by character frequency bounds
 * - Time: O(n * k), Space: O(n * k)
 * 
 * 10. String Interning (groupAnagrams10):
 * - Memory optimization technique
 * - Reduces duplicate string storage
 * - Time: O(n * k log k), Space: O(n * k) optimized
 * 
 * Performance Ranking (Best to Worst):
 * 1. groupAnagrams2 (Character Frequency) - Best overall
 * 2. groupAnagrams4 (Compact Frequency) - Good alternative
 * 3. groupAnagrams1 (Sorting) - Most readable
 * 4. groupAnagrams8 (Custom Hash) - Fast but collision risk
 * 5. groupAnagrams5 (Arrays.toString) - Simple but slower
 * 
 * Key Design Decisions:
 * - Frequency counting beats sorting for efficiency
 * - String keys vs numeric keys trade-off
 * - Memory optimization vs simplicity
 * - Hash collision handling
 * 
 * Recommendation: Use groupAnagrams2 (Character Frequency) for production,
 * groupAnagrams1 (Sorting) for interviews due to simplicity.
 */
