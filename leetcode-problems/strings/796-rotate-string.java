class RotateString {

    /**
     * Approach 1: Concatenation Trick (Most Elegant)
     * Time Complexity: O(n), Space Complexity: O(n)
     * Key insight: All rotations of s are substrings of s+s
     */
    public boolean rotateString1(String s, String goal) {
        // Must have same length and s should not be empty
        if (s.length() != goal.length()) {
            return false;
        }

        // Concatenate s with itself and check if goal is a substring
        return (s + s).contains(goal);
    }

    /**
     * Approach 2: Simulate All Rotations
     * Time Complexity: O(n²), Space Complexity: O(n)
     * Straightforward approach: try all possible rotations
     */
    public boolean rotateString2(String s, String goal) {
        if (s.length() != goal.length()) {
            return false;
        }

        if (s.equals(goal)) {
            return true;
        }

        // Try all n rotations
        for (int i = 1; i < s.length(); i++) {
            // Rotate: move first i characters to the end
            String rotated = s.substring(i) + s.substring(0, i);
            if (rotated.equals(goal)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Approach 3: Check Each Starting Position
     * Time Complexity: O(n²), Space Complexity: O(1)
     * Check if goal matches s starting from each position
     */
    public boolean rotateString3(String s, String goal) {
        if (s.length() != goal.length()) {
            return false;
        }

        int n = s.length();

        // Try each possible rotation point
        for (int i = 0; i < n; i++) {
            boolean matches = true;

            // Check if s rotated by i positions equals goal
            for (int j = 0; j < n; j++) {
                if (s.charAt((i + j) % n) != goal.charAt(j)) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                return true;
            }
        }

        return false;
    }

    /**
     * Approach 4: Using KMP Algorithm
     * Time Complexity: O(n), Space Complexity: O(n)
     * Most efficient for very long strings
     */
    public boolean rotateString4(String s, String goal) {
        if (s.length() != goal.length()) {
            return false;
        }

        return kmpSearch(s + s, goal);
    }

    // KMP pattern search
    private boolean kmpSearch(String text, String pattern) {
        int[] lps = computeLPS(pattern);
        int i = 0; // index for text
        int j = 0; // index for pattern

        while (i < text.length()) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
            }

            if (j == pattern.length()) {
                return true;
            } else if (i < text.length() && text.charAt(i) != pattern.charAt(j)) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }

        return false;
    }

    // Compute LPS (Longest Proper Prefix which is also Suffix) array
    private int[] computeLPS(String pattern) {
        int[] lps = new int[pattern.length()];
        int len = 0;
        int i = 1;

        while (i < pattern.length()) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }

        return lps;
    }

    /**
     * Approach 5: Using StringBuilder for Rotation
     * Time Complexity: O(n²), Space Complexity: O(n)
     * More memory efficient rotation simulation
     */
    public boolean rotateString5(String s, String goal) {
        if (s.length() != goal.length()) {
            return false;
        }

        StringBuilder sb = new StringBuilder(s);

        for (int i = 0; i < s.length(); i++) {
            if (sb.toString().equals(goal)) {
                return true;
            }
            // Perform one rotation: move first char to end
            char first = sb.charAt(0);
            sb.deleteCharAt(0);
            sb.append(first);
        }

        return false;
    }

    // Test method
    public static void main(String[] args) {
        RotateString solution = new RotateString();

        // Test Case 1
        System.out.println("Test Case 1:");
        System.out.println("s = \"abcde\", goal = \"cdeab\"");
        System.out.println("Result: " + solution.rotateString1("abcde", "cdeab")); // true
        System.out.println("Explanation: \"abcde\" -> \"bcdea\" -> \"cdeab\"");

        // Test Case 2
        System.out.println("\nTest Case 2:");
        System.out.println("s = \"abcde\", goal = \"abced\"");
        System.out.println("Result: " + solution.rotateString1("abcde", "abced")); // false
        System.out.println("Explanation: No rotation can produce \"abced\"");

        // Test Case 3
        System.out.println("\nTest Case 3:");
        System.out.println("s = \"aa\", goal = \"aa\"");
        System.out.println("Result: " + solution.rotateString1("aa", "aa")); // true

        // Test Case 4
        System.out.println("\nTest Case 4:");
        System.out.println("s = \"abc\", goal = \"bca\"");
        System.out.println("Result: " + solution.rotateString1("abc", "bca")); // true

        // Performance comparison
        System.out.println("\n--- Performance Test ---");
        String testS = "abcdefghijklmnopqrstuvwxyz";
        String testGoal = "defghijklmnopqrstuvwxyzabc";

        long start = System.nanoTime();
        boolean result1 = solution.rotateString1(testS, testGoal);
        long time1 = System.nanoTime() - start;

        start = System.nanoTime();
        boolean result2 = solution.rotateString2(testS, testGoal);
        long time2 = System.nanoTime() - start;

        System.out.println("Concatenation approach: " + result1 + " (" + time1 + " ns)");
        System.out.println("Simulation approach: " + result2 + " (" + time2 + " ns)");
    }
}
