import java.util.*;
/*
 * Seven different symbols represent Roman numerals with the following values:
 * 
 * Symbol Value
 * I 1
 * V 5
 * X 10
 * L 50
 * C 100
 * D 500
 * M 1000
 * Roman numerals are formed by appending the conversions of decimal place
 * values from highest to lowest. Converting a decimal place value into a Roman
 * numeral has the following rules:
 * 
 * If the value does not start with 4 or 9, select the symbol of the maximal
 * value that can be subtracted from the input, append that symbol to the
 * result, subtract its value, and convert the remainder to a Roman numeral.
 * If the value starts with 4 or 9 use the subtractive form representing one
 * symbol subtracted from the following symbol, for example, 4 is 1 (I) less
 * than 5 (V): IV and 9 is 1 (I) less than 10 (X): IX. Only the following
 * subtractive forms are used: 4 (IV), 9 (IX), 40 (XL), 90 (XC), 400 (CD) and
 * 900 (CM).
 * Only powers of 10 (I, X, C, M) can be appended consecutively at most 3 times
 * to represent multiples of 10. You cannot append 5 (V), 50 (L), or 500 (D)
 * multiple times. If you need to append a symbol 4 times use the subtractive
 * form.
 * Given an integer, convert it to a Roman numeral.
 * 
 * Example 1:
 * Input: num = 3749
 * Output: "MMMDCCXLIX"
 * Explanation:
 * 3000 = MMM as 1000 (M) + 1000 (M) + 1000 (M)
 * 700 = DCC as 500 (D) + 100 (C) + 100 (C)
 * 40 = XL as 10 (X) less of 50 (L)
 * 9 = IX as 1 (I) less of 10 (X)
 * Note: 49 is not 1 (I) less of 50 (L) because the conversion is based on
 * decimal places
 * 
 * Example 2:
 * Input: num = 58
 * Output: "LVIII"
 * Explanation:
 * 50 = L
 * 8 = VIII
 *
 * Example 3:
 * Input: num = 1994
 * Output: "MCMXCIV"
 * Explanation:
 * 1000 = M
 * 900 = CM
 * 90 = XC
 * 4 = IV
 */

class IntegerToRoman {

    /**
     * Solution 1: Greedy Approach with Value-Symbol Pairs
     * Time Complexity: O(1) - at most 13 iterations
     * Space Complexity: O(1)
     * 
     * Uses predefined value-symbol pairs in descending order.
     * Greedy approach: always use the largest possible value.
     */
    public String intToRomanV1(int num) {
        // All possible values in descending order (including subtractive forms)
        int[] values = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        String[] symbols = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            int count = num / values[i]; // How many times can we use this value
            if (count > 0) {
                // Append the symbol 'count' times
                for (int j = 0; j < count; j++) {
                    result.append(symbols[i]);
                }
                num -= values[i] * count; // Reduce the remaining number
            }
        }

        return result.toString();
    }

    /**
     * Solution 2: Optimized Greedy (Most Efficient)
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     * 
     * Similar to V1 but uses while loop for better performance.
     */
    public String intToRomanV2(int num) {
        int[] values = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        String[] symbols = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            while (num >= values[i]) {
                result.append(symbols[i]);
                num -= values[i];
            }
        }

        return result.toString();
    }

    /**
     * Solution 3: Digit-by-Digit Conversion
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     * 
     * Processes each decimal place (thousands, hundreds, tens, ones) separately.
     * Pre-defines all possible Roman representations for each digit position.
     */
    public String intToRomanV3(int num) {
        String[] thousands = { "", "M", "MM", "MMM" };
        String[] hundreds = { "", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM" };
        String[] tens = { "", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC" };
        String[] ones = { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX" };

        return thousands[num / 1000] +
                hundreds[(num % 1000) / 100] +
                tens[(num % 100) / 10] +
                ones[num % 10];
    }

    /**
     * Solution 4: Recursive Approach
     * Time Complexity: O(log num)
     * Space Complexity: O(log num) due to recursion stack
     * 
     * Recursively breaks down the number using the largest possible Roman value.
     */
    public String intToRomanV4(int num) {
        if (num == 0)
            return "";

        if (num >= 1000)
            return "M" + intToRomanV4(num - 1000);
        if (num >= 900)
            return "CM" + intToRomanV4(num - 900);
        if (num >= 500)
            return "D" + intToRomanV4(num - 500);
        if (num >= 400)
            return "CD" + intToRomanV4(num - 400);
        if (num >= 100)
            return "C" + intToRomanV4(num - 100);
        if (num >= 90)
            return "XC" + intToRomanV4(num - 90);
        if (num >= 50)
            return "L" + intToRomanV4(num - 50);
        if (num >= 40)
            return "XL" + intToRomanV4(num - 40);
        if (num >= 10)
            return "X" + intToRomanV4(num - 10);
        if (num >= 9)
            return "IX" + intToRomanV4(num - 9);
        if (num >= 5)
            return "V" + intToRomanV4(num - 5);
        if (num >= 4)
            return "IV" + intToRomanV4(num - 4);
        return "I" + intToRomanV4(num - 1);
    }

    /**
     * Solution 5: Map-Based Approach
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     * 
     * Uses TreeMap to maintain values in descending order automatically.
     */
    public String intToRomanV5(int num) {
        TreeMap<Integer, String> romanMap = new TreeMap<>(Collections.reverseOrder());
        romanMap.put(1000, "M");
        romanMap.put(900, "CM");
        romanMap.put(500, "D");
        romanMap.put(400, "CD");
        romanMap.put(100, "C");
        romanMap.put(90, "XC");
        romanMap.put(50, "L");
        romanMap.put(40, "XL");
        romanMap.put(10, "X");
        romanMap.put(9, "IX");
        romanMap.put(5, "V");
        romanMap.put(4, "IV");
        romanMap.put(1, "I");

        StringBuilder result = new StringBuilder();

        for (Map.Entry<Integer, String> entry : romanMap.entrySet()) {
            int value = entry.getKey();
            String symbol = entry.getValue();

            while (num >= value) {
                result.append(symbol);
                num -= value;
            }
        }

        return result.toString();
    }

    /**
     * Helper method: Roman to Integer for validation
     */
    public int romanToInt(String s) {
        Map<Character, Integer> romanMap = new HashMap<>();
        romanMap.put('I', 1);
        romanMap.put('V', 5);
        romanMap.put('X', 10);
        romanMap.put('L', 50);
        romanMap.put('C', 100);
        romanMap.put('D', 500);
        romanMap.put('M', 1000);

        int result = 0;
        int prevValue = 0;

        for (int i = s.length() - 1; i >= 0; i--) {
            int currentValue = romanMap.get(s.charAt(i));

            if (currentValue < prevValue) {
                result -= currentValue;
            } else {
                result += currentValue;
            }

            prevValue = currentValue;
        }

        return result;
    }

    /**
     * Validation method to check if Roman numeral follows all rules
     */
    public boolean isValidRoman(String roman) {
        // Check for invalid patterns
        String[] invalidPatterns = { "IIII", "VV", "XXXX", "LL", "CCCC", "DD", "MMMM" };
        for (String pattern : invalidPatterns) {
            if (roman.contains(pattern)) {
                return false;
            }
        }

        // Check for invalid subtractive forms
        String[] invalidSubtractive = { "IL", "IC", "ID", "IM", "VX", "VL", "VC", "VD", "VM",
                "XD", "XM", "LC", "LD", "LM", "DM" };
        for (String pattern : invalidSubtractive) {
            if (roman.contains(pattern)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Method to demonstrate the conversion rules step by step
     */
    public void explainConversion(int num) {
        System.out.println("\nExplaining conversion of " + num + ":");

        int[] values = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        String[] symbols = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };

        StringBuilder result = new StringBuilder();
        int remaining = num;

        for (int i = 0; i < values.length; i++) {
            int count = remaining / values[i];
            if (count > 0) {
                System.out.println(remaining + " ÷ " + values[i] + " = " + count +
                        " (add " + count + " × '" + symbols[i] + "')");
                for (int j = 0; j < count; j++) {
                    result.append(symbols[i]);
                }
                remaining -= values[i] * count;
                System.out.println("Current result: " + result + ", remaining: " + remaining);
            }
        }

        System.out.println("Final result: " + result);
    }

    // Test all solutions
    public static void main(String[] args) {
        IntegerToRoman solution = new IntegerToRoman();

        // Test cases
        int[] testCases = { 3, 58, 1994, 4, 9, 40, 90, 400, 900, 1444, 3999, 1, 2023 };
        String[] expected = { "III", "LVIII", "MCMXCIV", "IV", "IX", "XL", "XC", "CD", "CM",
                "MCDXLIV", "MMMCMXCIX", "I", "MMXXIII" };

        System.out.println("=== Testing All Solutions ===");
        for (int i = 0; i < testCases.length; i++) {
            int num = testCases[i];
            String expectedRoman = expected[i];

            System.out.println("\nInteger: " + num + " -> Expected: " + expectedRoman);

            String result1 = solution.intToRomanV1(num);
            String result2 = solution.intToRomanV2(num);
            String result3 = solution.intToRomanV3(num);
            String result4 = solution.intToRomanV4(num);
            String result5 = solution.intToRomanV5(num);

            System.out.println("V1 (Greedy-For): " + result1);
            System.out.println("V2 (Greedy-While): " + result2);
            System.out.println("V3 (Digit-by-Digit): " + result3);
            System.out.println("V4 (Recursive): " + result4);
            System.out.println("V5 (TreeMap): " + result5);

            boolean allCorrect = result1.equals(expectedRoman) && result2.equals(expectedRoman) &&
                    result3.equals(expectedRoman) && result4.equals(expectedRoman) &&
                    result5.equals(expectedRoman);
            System.out.println("All correct: " + allCorrect);

            // Validate Roman numeral rules
            System.out.println("Valid Roman: " + solution.isValidRoman(result2));

            // Round-trip validation
            int backToInt = solution.romanToInt(result2);
            System.out.println("Round-trip: " + num + " -> " + result2 + " -> " + backToInt +
                    (num == backToInt ? " ✓" : " ✗"));
        }

        // Performance comparison
        System.out.println("\n=== Performance Test ===");
        int iterations = 100000;
        int testNum = 1994;

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.intToRomanV2(testNum);
        }
        long greedyTime = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.intToRomanV3(testNum);
        }
        long digitTime = System.nanoTime() - startTime;

        System.out.println("Greedy While: " + greedyTime / 1000000.0 + " ms");
        System.out.println("Digit-by-Digit: " + digitTime / 1000000.0 + " ms");
        System.out.println("Digit approach is " + String.format("%.2f", (double) greedyTime / digitTime) + "x faster");

        // Demonstration of conversion rules
        solution.explainConversion(1994);
        solution.explainConversion(3999);

        // Edge cases
        System.out.println("\n=== Edge Cases ===");
        int[] edgeCases = { 1, 3, 4, 5, 9, 10, 49, 50, 99, 100, 399, 400, 499, 500, 899, 900, 999, 1000 };
        for (int num : edgeCases) {
            String roman = solution.intToRomanV2(num);
            System.out.println(String.format("%4d -> %-8s (valid: %s)",
                    num, roman, solution.isValidRoman(roman)));
        }
    }

}
