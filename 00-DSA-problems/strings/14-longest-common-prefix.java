class LongestCommonPrefix {

    // Approach 1: Horizontal Scanning (Intuitive)
    // Time: O(S), Space: O(1) where S = sum of all characters
    public String longestCommonPrefix1(String[] strs) {
        if (strs == null || strs.length == 0) {
            return "";
        }

        // Start with first string as prefix
        String prefix = strs[0];

        // Compare with each string and reduce prefix
        for (int i = 1; i < strs.length; i++) {
            while (strs[i].indexOf(prefix) != 0) {
                // Reduce prefix by one character
                prefix = prefix.substring(0, prefix.length() - 1);
                if (prefix.isEmpty()) {
                    return "";
                }
            }
        }

        return prefix;
    }

    // Approach 2: Vertical Scanning (Character by Character)
    // Time: O(S), Space: O(1)
    public String longestCommonPrefix2(String[] strs) {
        if (strs == null || strs.length == 0) {
            return "";
        }

        // Compare character by character across all strings
        for (int i = 0; i < strs[0].length(); i++) {
            char c = strs[0].charAt(i);

            // Check if this character matches in all strings
            for (int j = 1; j < strs.length; j++) {
                if (i >= strs[j].length() || strs[j].charAt(i) != c) {
                    // Mismatch found or string too short
                    return strs[0].substring(0, i);
                }
            }
        }

        // All characters of first string match in all strings
        return strs[0];
    }

    // Approach 3: Divide and Conquer
    // Time: O(S), Space: O(m*log(n)) for recursion
    public String longestCommonPrefix3(String[] strs) {
        if (strs == null || strs.length == 0) {
            return "";
        }
        return divideConquer(strs, 0, strs.length - 1);
    }

    private String divideConquer(String[] strs, int left, int right) {
        if (left == right) {
            return strs[left];
        }

        int mid = left + (right - left) / 2;
        String leftPrefix = divideConquer(strs, left, mid);
        String rightPrefix = divideConquer(strs, mid + 1, right);

        return commonPrefix(leftPrefix, rightPrefix);
    }

    private String commonPrefix(String str1, String str2) {
        int minLen = Math.min(str1.length(), str2.length());
        for (int i = 0; i < minLen; i++) {
            if (str1.charAt(i) != str2.charAt(i)) {
                return str1.substring(0, i);
            }
        }
        return str1.substring(0, minLen);
    }

    // Approach 4: Binary Search on Length
    // Time: O(S*log(m)), Space: O(1) where m = min string length
    public String longestCommonPrefix4(String[] strs) {
        if (strs == null || strs.length == 0) {
            return "";
        }

        int minLen = Integer.MAX_VALUE;
        for (String str : strs) {
            minLen = Math.min(minLen, str.length());
        }

        int left = 0, right = minLen;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (isCommonPrefix(strs, mid)) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return strs[0].substring(0, (left + right) / 2);
    }

    private boolean isCommonPrefix(String[] strs, int len) {
        String prefix = strs[0].substring(0, len);
        for (int i = 1; i < strs.length; i++) {
            if (!strs[i].startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    // Approach 5: Using StringBuilder (Efficient)
    // Time: O(S), Space: O(m)
    public String longestCommonPrefix5(String[] strs) {
        if (strs == null || strs.length == 0) {
            return "";
        }

        StringBuilder prefix = new StringBuilder();

        // Find minimum length
        int minLen = strs[0].length();
        for (String str : strs) {
            minLen = Math.min(minLen, str.length());
        }

        // Check each position
        for (int i = 0; i < minLen; i++) {
            char c = strs[0].charAt(i);

            for (int j = 1; j < strs.length; j++) {
                if (strs[j].charAt(i) != c) {
                    return prefix.toString();
                }
            }

            prefix.append(c);
        }

        return prefix.toString();
    }

    // Approach 6: Sorting-Based (Creative)
    // Time: O(n*m*log(n)), Space: O(1)
    public String longestCommonPrefix6(String[] strs) {
        if (strs == null || strs.length == 0) {
            return "";
        }

        // Sort array - lexicographically closest strings will be first and last
        java.util.Arrays.sort(strs);

        // Only need to compare first and last string
        String first = strs[0];
        String last = strs[strs.length - 1];

        int i = 0;
        while (i < first.length() && i < last.length() &&
                first.charAt(i) == last.charAt(i)) {
            i++;
        }

        return first.substring(0, i);
    }

    // Approach 7: Trie-Based (Advanced)
    // Time: O(S), Space: O(S)
    public String longestCommonPrefix7(String[] strs) {
        if (strs == null || strs.length == 0) {
            return "";
        }

        TrieNode root = new TrieNode();

        // Build trie with first string
        for (char c : strs[0].toCharArray()) {
            root.children.put(c, new TrieNode());
            root = root.children.get(c);
            root.count++;
        }

        // Find prefix where all strings pass through
        root = new TrieNode();
        for (char c : strs[0].toCharArray()) {
            root = root.children.get(c);
        }

        // Simpler: just compare characters
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < strs[0].length(); i++) {
            char c = strs[0].charAt(i);
            boolean allMatch = true;

            for (int j = 1; j < strs.length; j++) {
                if (i >= strs[j].length() || strs[j].charAt(i) != c) {
                    allMatch = false;
                    break;
                }
            }

            if (allMatch) {
                prefix.append(c);
            } else {
                break;
            }
        }

        return prefix.toString();
    }

    static class TrieNode {
        java.util.Map<Character, TrieNode> children = new java.util.HashMap<>();
        int count = 0;
    }

    // Test cases with visualization
    public static void main(String[] args) {
        LongestCommonPrefix solution = new LongestCommonPrefix();

        // Test Case 1
        String[] strs1 = { "flower", "flow", "flight" };
        String result1 = solution.longestCommonPrefix2(strs1);
        System.out.println("Test 1: " + java.util.Arrays.toString(strs1));
        System.out.println("Result: \"" + result1 + "\""); // "fl"
        visualizePrefix(strs1, result1);

        // Test Case 2
        String[] strs2 = { "dog", "racecar", "car" };
        String result2 = solution.longestCommonPrefix2(strs2);
        System.out.println("\nTest 2: " + java.util.Arrays.toString(strs2));
        System.out.println("Result: \"" + result2 + "\""); // ""
        visualizePrefix(strs2, result2);

        // Test Case 3: All same
        String[] strs3 = { "test", "test", "test" };
        String result3 = solution.longestCommonPrefix2(strs3);
        System.out.println("\nTest 3: " + java.util.Arrays.toString(strs3));
        System.out.println("Result: \"" + result3 + "\""); // "test"

        // Test Case 4: Single string
        String[] strs4 = { "alone" };
        String result4 = solution.longestCommonPrefix2(strs4);
        System.out.println("\nTest 4: " + java.util.Arrays.toString(strs4));
        System.out.println("Result: \"" + result4 + "\""); // "alone"

        // Test Case 5: Empty string in array
        String[] strs5 = { "flower", "", "flight" };
        String result5 = solution.longestCommonPrefix2(strs5);
        System.out.println("\nTest 5: " + java.util.Arrays.toString(strs5));
        System.out.println("Result: \"" + result5 + "\""); // ""

        // Compare all approaches
        System.out.println("\nComparing all approaches for Test 1:");
        System.out.println("Approach 1: \"" + solution.longestCommonPrefix1(strs1) + "\"");
        System.out.println("Approach 2: \"" + solution.longestCommonPrefix2(strs1) + "\"");
        System.out.println("Approach 3: \"" + solution.longestCommonPrefix3(strs1) + "\"");
        System.out.println("Approach 4: \"" + solution.longestCommonPrefix4(strs1) + "\"");
        System.out.println("Approach 5: \"" + solution.longestCommonPrefix5(strs1) + "\"");
        System.out.println("Approach 6: \"" + solution.longestCommonPrefix6(strs1) + "\"");
        System.out.println("Approach 7: \"" + solution.longestCommonPrefix7(strs1) + "\"");
    }

    private static void visualizePrefix(String[] strs, String prefix) {
        System.out.println("\nVisualization:");
        int maxLen = 0;
        for (String str : strs) {
            maxLen = Math.max(maxLen, str.length());
        }

        for (String str : strs) {
            System.out.print("  ");
            for (int i = 0; i < str.length(); i++) {
                if (i < prefix.length()) {
                    System.out.print("[" + str.charAt(i) + "]");
                } else {
                    System.out.print(" " + str.charAt(i) + " ");
                }
            }
            System.out.println();
        }
        System.out.println("  ^" + "-".repeat(prefix.length() * 3 - 1) + "^ Common prefix");
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM UNDERSTANDING:
 * - Find the longest string that is a prefix of ALL strings in the array
 * - If no common prefix exists, return ""
 * 
 * KEY INSIGHTS:
 * 1. Common prefix length ≤ length of shortest string
 * 2. If any string is empty, common prefix is ""
 * 3. Need to match character by character across all strings
 * 
 * APPROACH 1 - HORIZONTAL SCANNING:
 * Start with first string as prefix, then reduce it by comparing with each
 * string.
 * 
 * Algorithm:
 * 1. prefix = strs[0]
 * 2. For each string:
 * - While string doesn't start with prefix:
 * - Remove last character from prefix
 * 3. Return prefix
 * 
 * Example: ["flower", "flow", "flight"]
 * - prefix = "flower"
 * - Compare with "flow": "flower" → "flow" → matches ✓
 * - Compare with "flight": "flow" doesn't match → "flo" → "fl" → matches ✓
 * - Result: "fl"
 * 
 * Time: O(S) where S = sum of all characters
 * - Worst case: compare every character
 * Space: O(1)
 * 
 * APPROACH 2 - VERTICAL SCANNING (RECOMMENDED):
 * Compare character by character across all strings.
 * 
 * Algorithm:
 * 1. For each position i in first string:
 * 2. Get character c at position i
 * 3. Check if all other strings have same character at position i
 * 4. If not, return substring [0, i)
 * 5. If all match, return entire first string
 * 
 * Example: ["flower", "flow", "flight"]
 * Position 0: f,f,f → all match ✓
 * Position 1: l,l,l → all match ✓
 * Position 2: o,o,i → mismatch! ✗
 * Return: "fl"
 * 
 * Advantage: Stops early on first mismatch
 * Time: O(S) - might check fewer characters
 * Space: O(1)
 * 
 * APPROACH 3 - DIVIDE AND CONQUER:
 * Recursively find LCP of left half and right half, then merge.
 * 
 * Algorithm:
 * 1. If single string, return it
 * 2. Split array in half
 * 3. Find LCP of left half recursively
 * 4. Find LCP of right half recursively
 * 5. Return LCP of these two prefixes
 * 
 * Example: ["flower", "flow", "flight"]
 * LCP(all)
 * / \
 * LCP("flower","flow") LCP("flight")
 * = "flow" = "flight"
 * \ /
 * LCP("flow", "flight")
 * = "fl"
 * 
 * Time: O(S) - each character compared once
 * Space: O(m*log(n)) - recursion depth
 * 
 * APPROACH 4 - BINARY SEARCH:
 * Binary search on the length of the prefix!
 * 
 * Algorithm:
 * 1. Find minimum string length m
 * 2. Binary search on length [0, m]
 * 3. For each mid length, check if prefix of that length is common
 * 4. Adjust search range based on result
 * 
 * Example: min length = 6
 * left=0, right=6
 * mid=3: Check if prefix of length 3 is common
 * "flo" common? → check all strings
 * Continue binary search...
 * 
 * Time: O(S * log(m)) where m = min string length
 * Space: O(1)
 * 
 * APPROACH 6 - SORTING (CREATIVE):
 * After sorting, strings with longest common prefix will be close.
 * Only need to compare first and last!
 * 
 * Example: ["flower", "flow", "flight"]
 * After sort: ["flight", "flow", "flower"]
 * Compare first and last: "flight" vs "flower"
 * Common prefix: "fl"
 * 
 * Why it works:
 * - Lexicographic sorting groups similar strings
 * - If first and last share prefix, all middle strings must too
 * - "abc", "abd", "abe" → first="abc", last="abe" → "ab" common
 * 
 * Time: O(n*m*log(n)) - dominated by sorting
 * Space: O(1) if sort in-place
 * 
 * COMPLEXITY COMPARISON:
 * 
 * Approach 1 (Horizontal): O(S), O(1)
 * Approach 2 (Vertical): O(S), O(1) ✓ Best
 * Approach 3 (Divide): O(S), O(m*log(n))
 * Approach 4 (Binary): O(S*log(m)), O(1)
 * Approach 6 (Sorting): O(n*m*log(n)), O(1)
 * 
 * S = sum of all characters
 * n = number of strings
 * m = length of shortest string
 * 
 * EDGE CASES:
 * 1. Empty array: return ""
 * 2. Single string: return that string
 * 3. Empty string in array: return ""
 * 4. All strings identical: return entire string
 * 5. No common prefix: return ""
 * 
 * PRACTICAL APPLICATIONS:
 * 1. File path processing (common directory)
 * 2. Auto-complete systems
 * 3. String compression
 * 4. Database query optimization
 * 5. Version control (common file prefix)
 * 
 * INTERVIEW TIPS:
 * 1. Ask about empty array and empty strings
 * 2. Mention multiple approaches
 * 3. Vertical scanning is usually best (simple + efficient)
 * 4. Discuss early termination benefits
 * 5. Consider string length variations
 * 
 * COMMON MISTAKES:
 * 1. Not handling empty strings
 * 2. Not handling single string case
 * 3. Off-by-one errors in substring
 * 4. Not checking string bounds
 * 5. Assuming all strings same length
 */
