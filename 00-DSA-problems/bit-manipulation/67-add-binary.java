/*
 * Given two binary strings a and b, return their sum as a binary string.
 * 
 * Example 1:
 * Input: a = "11", b = "1"
 * Output: "100"
 * 
 * Example 2:
 * Input: a = "1010", b = "1011"
 * Output: "10101"
 */

class BinaryStringAddition {

    // Solution 1: Bit-by-bit addition with carry
    // Time Complexity: O(max(m, n)) where m and n are lengths of the strings
    // Space Complexity: O(max(m, n)) for the result
    public String addBinary1(String a, String b) {
        StringBuilder result = new StringBuilder();
        int i = a.length() - 1;
        int j = b.length() - 1;
        int carry = 0;

        while (i >= 0 || j >= 0 || carry > 0) {
            int sum = carry;

            if (i >= 0) {
                sum += a.charAt(i) - '0';
                i--;
            }

            if (j >= 0) {
                sum += b.charAt(j) - '0';
                j--;
            }

            result.append(sum % 2);
            carry = sum / 2;
        }

        return result.reverse().toString();
    }

    // Solution 2: Using BigInteger (simpler but less efficient)
    // Time Complexity: O(max(m, n))
    // Space Complexity: O(max(m, n))
    public String addBinary2(String a, String b) {
        java.math.BigInteger numA = new java.math.BigInteger(a, 2);
        java.math.BigInteger numB = new java.math.BigInteger(b, 2);
        java.math.BigInteger sum = numA.add(numB);
        return sum.toString(2);
    }

    // Solution 3: Recursive approach
    public String addBinary3(String a, String b) {
        if (a.length() < b.length())
            return addBinary3(b, a);

        int diff = a.length() - b.length();
        String padding = "";
        for (int i = 0; i < diff; i++) {
            padding += "0";
        }
        b = padding + b;

        return addBinaryHelper(a, b, 0, a.length() - 1);
    }

    private String addBinaryHelper(String a, String b, int carry, int index) {
        if (index < 0) {
            return carry == 1 ? "1" : "";
        }

        int sum = carry + (a.charAt(index) - '0') + (b.charAt(index) - '0');
        String result = (sum % 2) + addBinaryHelper(a, b, sum / 2, index - 1);
        return result;
    }

    // Solution 4: Optimized version with single pass
    // Time Complexity: O(max(m, n))
    // Space Complexity: O(max(m, n))
    // Uses bitwise operations (& for modulo, >> for division)
    // Slightly more efficient than Solution 1
    public String addBinary4(String a, String b) {
        StringBuilder sb = new StringBuilder();
        int i = a.length() - 1, j = b.length() - 1, carry = 0;

        while (i >= 0 || j >= 0) {
            int sum = carry;
            if (i >= 0)
                sum += a.charAt(i--) - '0';
            if (j >= 0)
                sum += b.charAt(j--) - '0';
            sb.append(sum & 1); // equivalent to sum % 2
            carry = sum >> 1; // equivalent to sum / 2
        }

        if (carry != 0)
            sb.append(carry);
        return sb.reverse().toString();
    }

    // Test the solutions
    public static void main(String[] args) {
        BinaryStringAddition solution = new BinaryStringAddition();

        // Test cases
        String[][] testCases = {
                { "11", "1", "100" },
                { "1010", "1011", "10101" },
                { "0", "0", "0" },
                { "1", "111", "1000" },
                { "1111", "1111", "11110" }
        };

        System.out.println("Testing all solutions:");
        for (String[] test : testCases) {
            String a = test[0], b = test[1], expected = test[2];

            String result1 = solution.addBinary1(a, b);
            String result2 = solution.addBinary2(a, b);
            String result3 = solution.addBinary3(a, b);
            String result4 = solution.addBinary4(a, b);

            System.out.printf("Input: a=\"%s\", b=\"%s\"\n", a, b);
            System.out.printf("Expected: %s\n", expected);
            System.out.printf("Solution 1: %s %s\n", result1, result1.equals(expected) ? "✓" : "✗");
            System.out.printf("Solution 2: %s %s\n", result2, result2.equals(expected) ? "✓" : "✗");
            System.out.printf("Solution 3: %s %s\n", result3, result3.equals(expected) ? "✓" : "✗");
            System.out.printf("Solution 4: %s %s\n", result4, result4.equals(expected) ? "✓" : "✗");
            System.out.println();
        }
    }

}
