class LargestOddNumber {

    // Approach 1: Greedy - Find Last Odd Digit (Optimal)
    // Time: O(n), Space: O(1)
    public String largestOddNumber1(String num) {
        // Scan from right to left to find the last odd digit
        for (int i = num.length() - 1; i >= 0; i--) {
            int digit = num.charAt(i) - '0';
            if (digit % 2 == 1) {
                // Found odd digit, return substring from start to here
                return num.substring(0, i + 1);
            }
        }

        // No odd digit found
        return "";
    }

    // Approach 2: Cleaner Check with Character Arithmetic
    // Time: O(n), Space: O(1)
    public String largestOddNumber2(String num) {
        for (int i = num.length() - 1; i >= 0; i--) {
            // Check if digit is odd by checking last bit
            if ((num.charAt(i) - '0') % 2 == 1) {
                return num.substring(0, i + 1);
            }
        }
        return "";
    }

    // Approach 3: Using Character Values Directly
    // Time: O(n), Space: O(1)
    public String largestOddNumber3(String num) {
        for (int i = num.length() - 1; i >= 0; i--) {
            char c = num.charAt(i);
            // Odd digits: '1', '3', '5', '7', '9'
            if (c == '1' || c == '3' || c == '5' || c == '7' || c == '9') {
                return num.substring(0, i + 1);
            }
        }
        return "";
    }

    // Approach 4: Using Bitwise Operation
    // Time: O(n), Space: O(1)
    public String largestOddNumber4(String num) {
        for (int i = num.length() - 1; i >= 0; i--) {
            // Check if last bit is 1 (odd number)
            if (((num.charAt(i) - '0') & 1) == 1) {
                return num.substring(0, i + 1);
            }
        }
        return "";
    }

    // Approach 5: Alternative with lastIndexOf (Creative but less efficient)
    // Time: O(n), Space: O(1)
    public String largestOddNumber5(String num) {
        // Find the last occurrence of any odd digit
        int lastOddIndex = -1;

        for (char oddDigit : new char[] { '9', '7', '5', '3', '1' }) {
            int index = num.lastIndexOf(oddDigit);
            if (index > lastOddIndex) {
                lastOddIndex = index;
            }
        }

        return lastOddIndex == -1 ? "" : num.substring(0, lastOddIndex + 1);
    }

    // Approach 6: Brute Force (For comparison - inefficient)
    // Time: O(n²), Space: O(n)
    public String largestOddNumber6(String num) {
        // Try all substrings from longest to shortest
        for (int len = num.length(); len >= 1; len--) {
            for (int start = 0; start <= num.length() - len; start++) {
                String substring = num.substring(start, start + len);
                // Check if this substring represents an odd number
                char lastChar = substring.charAt(substring.length() - 1);
                if ((lastChar - '0') % 2 == 1) {
                    return substring;
                }
            }
        }
        return "";
    }

    // Test cases with detailed explanation
    public static void main(String[] args) {
        LargestOddNumber solution = new LargestOddNumber();

        // Test Case 1
        String num1 = "52";
        String result1 = solution.largestOddNumber1(num1);
        System.out.println("Test 1: \"" + num1 + "\" → \"" + result1 + "\""); // "5"
        explainSolution(num1, result1);

        // Test Case 2
        String num2 = "4206";
        String result2 = solution.largestOddNumber1(num2);
        System.out.println("\nTest 2: \"" + num2 + "\" → \"" + result2 + "\""); // ""
        explainSolution(num2, result2);

        // Test Case 3
        String num3 = "35427";
        String result3 = solution.largestOddNumber1(num3);
        System.out.println("\nTest 3: \"" + num3 + "\" → \"" + result3 + "\""); // "35427"
        explainSolution(num3, result3);

        // Test Case 4: All even
        String num4 = "2468";
        String result4 = solution.largestOddNumber1(num4);
        System.out.println("\nTest 4: \"" + num4 + "\" → \"" + result4 + "\""); // ""

        // Test Case 5: Odd in middle
        String num5 = "123456";
        String result5 = solution.largestOddNumber1(num5);
        System.out.println("Test 5: \"" + num5 + "\" → \"" + result5 + "\""); // "12345"

        // Test Case 6: Single digit odd
        String num6 = "9";
        String result6 = solution.largestOddNumber1(num6);
        System.out.println("Test 6: \"" + num6 + "\" → \"" + result6 + "\""); // "9"

        // Test Case 7: Leading zeros handling
        String num7 = "10";
        String result7 = solution.largestOddNumber1(num7);
        System.out.println("Test 7: \"" + num7 + "\" → \"" + result7 + "\""); // "1"

        // Compare all approaches
        System.out.println("\nComparing all approaches for \"35427\":");
        System.out.println("Approach 1: \"" + solution.largestOddNumber1(num3) + "\"");
        System.out.println("Approach 2: \"" + solution.largestOddNumber2(num3) + "\"");
        System.out.println("Approach 3: \"" + solution.largestOddNumber3(num3) + "\"");
        System.out.println("Approach 4: \"" + solution.largestOddNumber4(num3) + "\"");
        System.out.println("Approach 5: \"" + solution.largestOddNumber5(num3) + "\"");
        System.out.println("Approach 6: \"" + solution.largestOddNumber6(num3) + "\"");
    }

    private static void explainSolution(String num, String result) {
        System.out.println("Input: " + num);

        // Show each digit and whether it's odd/even
        System.out.print("Digits: ");
        for (int i = 0; i < num.length(); i++) {
            char c = num.charAt(i);
            int digit = c - '0';
            System.out.print(c + (digit % 2 == 1 ? "(odd) " : "(even) "));
        }
        System.out.println();

        if (result.isEmpty()) {
            System.out.println("No odd digit found → return \"\"");
        } else {
            int lastOddIndex = result.length() - 1;
            char lastOddDigit = result.charAt(lastOddIndex);
            System.out.println("Last odd digit: '" + lastOddDigit +
                    "' at index " + lastOddIndex);
            System.out.println("Largest odd substring: \"" + result + "\"");

            // Show why this is the largest
            if (result.length() == num.length()) {
                System.out.println("(Entire string is odd)");
            } else {
                System.out.println("(Taking substring from start to last odd digit)");
            }
        }
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM UNDERSTANDING:
 * 
 * Goal: Find the largest odd number that is a substring of the input
 * - Must be contiguous
 * - Must be odd (last digit is odd)
 * - Want maximum value
 * 
 * KEY INSIGHTS:
 * 
 * 1. A number is ODD if and only if its LAST DIGIT is odd
 * - 135 is odd (last digit 5)
 * - 136 is even (last digit 6)
 * - The other digits don't matter for oddness!
 * 
 * 2. To maximize the value of a substring:
 * - We want it to be as LONG as possible
 * - Longer number = larger value (assuming no leading zeros)
 * - "123" > "23" > "3"
 * 
 * 3. Combining insights 1 and 2:
 * - We want the LONGEST substring that ends with an odd digit
 * - Best strategy: start from beginning, end at LAST odd digit
 * - This gives us the maximum length odd substring
 * 
 * ALGORITHM (Greedy Approach):
 * 
 * Scan from RIGHT to LEFT:
 * - Find the first (rightmost) odd digit
 * - Return substring from start to this position
 * - If no odd digit exists, return ""
 * 
 * Why scan right to left?
 * - We want the LAST odd digit for maximum length
 * - No point checking digits after the last odd digit
 * 
 * EXAMPLE WALKTHROUGH 1: num = "52"
 * 
 * Index: 0 1
 * Digit: 5 2
 * Odd? Y N
 * 
 * Scan from right:
 * - i=1: '2' is even (2%2=0), continue
 * - i=0: '5' is odd (5%2=1), return num[0:1] = "5"
 * 
 * Result: "5"
 * 
 * Why not "52"? Because 52 is even (ends in 2)
 * Why not "2"? Because 2 is even
 * Only "5" is odd, so it's the answer.
 * 
 * EXAMPLE WALKTHROUGH 2: num = "4206"
 * 
 * Index: 0 1 2 3
 * Digit: 4 2 0 6
 * Odd? N N N N
 * 
 * Scan from right:
 * - i=3: '6' is even
 * - i=2: '0' is even
 * - i=1: '2' is even
 * - i=0: '4' is even
 * No odd digit found → return ""
 * 
 * EXAMPLE WALKTHROUGH 3: num = "35427"
 * 
 * Index: 0 1 2 3 4
 * Digit: 3 5 4 2 7
 * Odd? Y Y N N Y
 * 
 * Scan from right:
 * - i=4: '7' is odd, return num[0:5] = "35427"
 * 
 * Result: "35427" (entire string)
 * 
 * The entire string is odd because it ends in 7!
 * 
 * EXAMPLE 4: num = "123456"
 * 
 * Index: 0 1 2 3 4 5
 * Digit: 1 2 3 4 5 6
 * Odd? Y N Y N Y N
 * 
 * Scan from right:
 * - i=5: '6' is even
 * - i=4: '5' is odd, return num[0:5] = "12345"
 * 
 * Result: "12345"
 * 
 * Why not "123456"? Ends in 6 (even)
 * Why not "5"? Too short, "12345" is larger
 * "12345" is the longest odd substring.
 * 
 * WHY GREEDY WORKS:
 * 
 * Proof by contradiction:
 * - Suppose there's a larger odd substring S that doesn't start at index 0
 * - S must end at some odd digit at position k
 * - But num[0:k+1] is also odd (same last digit) and longer
 * - Contradiction! So the largest must start at index 0
 * 
 * Therefore: Optimal = substring from start to last odd digit
 * 
 * COMPLEXITY ANALYSIS:
 * 
 * Approach 1-4 (Optimal):
 * - Time: O(n) - scan string once from right
 * - Worst case: scan entire string if first digit is odd
 * - Best case: O(1) if last digit is odd
 * - Space: O(1) - only use constant extra space
 * - substring() creates new string but that's the output
 * 
 * Approach 6 (Brute Force):
 * - Time: O(n²) - try all substrings
 * - n substrings of each length
 * - n different lengths
 * - Space: O(n) - store substrings
 * - Not efficient!
 * 
 * EDGE CASES:
 * 
 * 1. All digits even: "2468" → ""
 * 2. All digits odd: "13579" → "13579"
 * 3. Single digit odd: "5" → "5"
 * 4. Single digit even: "4" → ""
 * 5. Odd only at start: "52468" → "5"
 * 6. Odd only at end: "24685" → "24685"
 * 7. Leading zeros: "10" → "1" (not "10" because it's even)
 * 
 * ODD DIGIT CHECK METHODS:
 * 
 * Method 1: digit % 2 == 1
 * Method 2: (digit & 1) == 1 (bitwise - faster)
 * Method 3: digit in {1,3,5,7,9} (explicit check)
 * 
 * All equivalent, but % 2 is most readable.
 * 
 * COMMON MISTAKES TO AVOID:
 * 
 * 1. Don't check if entire number is odd arithmetically
 * - Number might be too large for int/long
 * - Just check last digit!
 * 
 * 2. Don't scan left to right
 * - You'd find first odd, not last
 * - Result would be shorter than optimal
 * 
 * 3. Don't try to parse string to integer
 * - Number can be very large (up to 10^5 digits)
 * - String operations are sufficient
 * 
 * 4. Remember: substring(0, i+1) includes index i
 * - substring(0, 3) gives indices 0,1,2 (not 0,1,2,3)
 * 
 * PRACTICAL APPLICATIONS:
 * 
 * 1. Number processing in large-scale systems
 * 2. Financial calculations with large numbers
 * 3. Cryptography (working with huge numbers as strings)
 * 4. String manipulation in data processing
 * 5. Validation systems
 * 
 * INTERVIEW TIPS:
 * 
 * 1. Immediately recognize: only last digit matters for oddness
 * 2. Explain greedy approach: want longest substring
 * 3. Mention O(n) time, O(1) space complexity
 * 4. Handle edge cases: all even, all odd, single digit
 * 5. Note: works for arbitrarily large numbers
 */
