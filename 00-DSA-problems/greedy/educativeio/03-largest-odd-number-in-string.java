import java.util.List;

/**
 * ============================================================================
 * INTERVIEW GUIDE: LARGEST ODD NUMBER IN STRING
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "Can the string length exceed standard integer/long data type limits?" 
 *      (Assumption: Yes, length up to 10^4 means we cannot parse it into a Long 
 *      or BigInteger efficiently. We must handle it purely as a String.)
 *    - "What if the string contains no odd digits at all?" 
 *      (Assumption: Return an empty string "" as per the instructions.)
 *    - "Will there be any leading zeros or non-digit characters?" 
 *      (Assumption: The constraints guarantee only digits and no leading zeros.)
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Observation 1 (Magnitude): In base-10 mathematics, a number with more 
 *      digits is ALWAYS larger than a number with fewer digits. Therefore, to 
 *      maximize the value of our integer, the substring must start from the 
 *      very first character (index 0). 
 *    - Observation 2 (Odd Number Rule): A number is odd if and only if its 
 *      last (rightmost) digit is odd ('1', '3', '5', '7', or '9').
 *    - The Strategy: We iterate from the right side of the string to the left. 
 *      The moment we encounter an odd digit at index 'i', we immediately know 
 *      that the substring from index 0 to 'i' is the largest possible odd 
 *      number.
 * 
 * 3. VISUAL EXPLANATION:
 *    Example 1: num = "35427"
 *    Index: [0] [1] [2] [3] [4]
 *    Char:   3   5   4   2   7
 *                            ^ Start here. '7' is odd! 
 *    Result: substring(0, 5) -> "35427"
 * 
 *    Example 2: num = "52"
 *    Index: [0] [1]
 *    Char:   5   2
 *                ^ Start here. '2' is even. Move left.
 *            ^ '5' is odd!
 *    Result: substring(0, 1) -> "5"
 * 
 *    Example 3: num = "4206"
 *    Index: [0] [1] [2] [3]
 *    Char:   4   2   0   6
 *    Traverse right to left: 6(even) -> 0(even) -> 2(even) -> 4(even).
 *    Result: No odd digits found -> ""
 * 
 * ============================================================================
 */
class LargestOddNumber {

    /**
     * APPROACH 1: Right-to-Left Traversal (Optimal)
     * 
     * Time Complexity: O(N) where N is the length of the string. In the worst case, 
     * we check every character once.
     * Space Complexity: O(1) auxiliary space (ignoring the space used by the 
     * returned substring, which is unavoidable).
     */
    public String largestOddNumberOptimal(String num) {
        // Iterate from the last character down to the first
        for (int i = num.length() - 1; i >= 0; i--) {
            // Subtracting '0' converts the char to its integer value (e.g., '7' - '0' = 7)
            int digit = num.charAt(i) - '0';
            
            // Check if the digit is odd
            if (digit % 2 != 0) {
                // If it's odd, the largest odd number is the substring from 
                // the start up to and including this digit.
                // Note: substring(startIndex, endIndex) is exclusive of endIndex, so we use i + 1.
                return num.substring(0, i + 1);
            }
        }
        
        // If loop completes without finding an odd digit, return empty string
        return "";
    }

    /**
     * APPROACH 2: Regex One-Liner (Modern / Clever)
     * 
     * While not optimal for maximum performance due to regex compilation overhead, 
     * showing a one-liner demonstrates deep API knowledge.
     * We simply strip all trailing even digits from the end of the string.
     * 
     * Time Complexity: O(N) practically, though regex engines have overhead.
     * Space Complexity: O(N) for string immutability/replacements.
     */
    public String largestOddNumberRegex(String num) {
        // Replace 1 or more trailing even digits ([02468]+$) with an empty string
        return num.replaceAll("[02468]+$", "");
    }

    /**
     * Modern Java Feature: Using Records for clean, immutable test cases.
     */
    record TestCase(String num, String expected) {}

    public static void main(String[] args) {
        LargestOddNumber solver = new LargestOddNumber();
        
        // Defining test cases
        var testCases = List.of(
            new TestCase("52", "5"),
            new TestCase("4206", ""),
            new TestCase("35427", "35427"),
            new TestCase("10133890", "1013389"), // Stops at the 9
            new TestCase("8", ""),
            new TestCase("9", "9")
        );
        
        System.out.println("--- Running Approach 1 (Optimal) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            String result = solver.largestOddNumberOptimal(tc.num());
            System.out.printf("Test %d: Expected = \"%s\", Got = \"%s\" -> %s%n", 
                i + 1, tc.expected(), result, (result.equals(tc.expected()) ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (Regex) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            String result = solver.largestOddNumberRegex(tc.num());
            System.out.printf("Test %d: Expected = \"%s\", Got = \"%s\" -> %s%n", 
                i + 1, tc.expected(), result, (result.equals(tc.expected()) ? "PASS" : "FAIL"));
        }
    }
}
