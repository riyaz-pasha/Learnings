import java.util.*;
/*
 * Roman numerals are represented by seven different symbols: I, V, X, L, C, D
 * and M.
 * 
 * Symbol Value
 * I 1
 * V 5
 * X 10
 * L 50
 * C 100
 * D 500
 * M 1000
 * For example, 2 is written as II in Roman numeral, just two ones added
 * together. 12 is written as XII, which is simply X + II. The number 27 is
 * written as XXVII, which is XX + V + II.
 * 
 * Roman numerals are usually written largest to smallest from left to right.
 * However, the numeral for four is not IIII. Instead, the number four is
 * written as IV. Because the one is before the five we subtract it making four.
 * The same principle applies to the number nine, which is written as IX. There
 * are six instances where subtraction is used:
 * 
 * I can be placed before V (5) and X (10) to make 4 and 9.
 * X can be placed before L (50) and C (100) to make 40 and 90.
 * C can be placed before D (500) and M (1000) to make 400 and 900.
 * Given a roman numeral, convert it to an integer.
 * 
 */

class RomanToInteger {

    /**
     * Solution 1: Left to Right with Lookahead
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Process from left to right, checking if current character forms
     * a subtraction case with the next character.
     */
    public int romanToIntV1(String s) {
        Map<Character, Integer> romanMap = new HashMap<>();
        romanMap.put('I', 1);
        romanMap.put('V', 5);
        romanMap.put('X', 10);
        romanMap.put('L', 50);
        romanMap.put('C', 100);
        romanMap.put('D', 500);
        romanMap.put('M', 1000);

        int result = 0;
        int n = s.length();

        for (int i = 0; i < n; i++) {
            char current = s.charAt(i);
            int currentValue = romanMap.get(current);

            // Check if this is a subtraction case
            if (i < n - 1) {
                char next = s.charAt(i + 1);
                int nextValue = romanMap.get(next);

                if (currentValue < nextValue) {
                    // Subtraction case (IV, IX, XL, XC, CD, CM)
                    result += nextValue - currentValue;
                    i++; // Skip the next character
                    continue;
                }
            }

            // Normal case - just add the value
            result += currentValue;
        }

        return result;
    }

    /**
     * Solution 2: Right to Left Processing
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Process from right to left. Add current value if it's >= previous value,
     * otherwise subtract it (subtraction case).
     */
    public int romanToIntV2(String s) {
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

        // Process from right to left
        for (int i = s.length() - 1; i >= 0; i--) {
            int currentValue = romanMap.get(s.charAt(i));

            if (currentValue < prevValue) {
                // Subtraction case
                result -= currentValue;
            } else {
                // Addition case
                result += currentValue;
            }

            prevValue = currentValue;
        }

        return result;
    }

    /**
     * Solution 3: Optimized with Switch Statement
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Uses switch statement instead of HashMap for better performance.
     * Processes right to left.
     */
    public int romanToIntV3(String s) {
        int result = 0;
        int prevValue = 0;

        for (int i = s.length() - 1; i >= 0; i--) {
            int currentValue = getValue(s.charAt(i));

            if (currentValue < prevValue) {
                result -= currentValue;
            } else {
                result += currentValue;
            }

            prevValue = currentValue;
        }

        return result;
    }

    private int getValue(char c) {
        switch (c) {
            case 'I':
                return 1;
            case 'V':
                return 5;
            case 'X':
                return 10;
            case 'L':
                return 50;
            case 'C':
                return 100;
            case 'D':
                return 500;
            case 'M':
                return 1000;
            default:
                return 0;
        }
    }

    /**
     * Solution 4: Pattern Matching Approach
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     * 
     * Explicitly handles all subtraction patterns first, then processes remaining
     * characters.
     */
    public int romanToIntV4(String s) {
        int result = 0;
        int i = 0;

        // Handle all two-character subtraction patterns
        while (i < s.length()) {
            if (i < s.length() - 1) {
                String twoChar = s.substring(i, i + 2);
                switch (twoChar) {
                    case "IV":
                        result += 4;
                        i += 2;
                        continue;
                    case "IX":
                        result += 9;
                        i += 2;
                        continue;
                    case "XL":
                        result += 40;
                        i += 2;
                        continue;
                    case "XC":
                        result += 90;
                        i += 2;
                        continue;
                    case "CD":
                        result += 400;
                        i += 2;
                        continue;
                    case "CM":
                        result += 900;
                        i += 2;
                        continue;
                }
            }

            // Handle single character
            char c = s.charAt(i);
            switch (c) {
                case 'I':
                    result += 1;
                    break;
                case 'V':
                    result += 5;
                    break;
                case 'X':
                    result += 10;
                    break;
                case 'L':
                    result += 50;
                    break;
                case 'C':
                    result += 100;
                    break;
                case 'D':
                    result += 500;
                    break;
                case 'M':
                    result += 1000;
                    break;
            }
            i++;
        }

        return result;
    }

    /**
     * Solution 5: Replace and Sum Approach
     * Time Complexity: O(n)
     * Space Complexity: O(n) due to string operations
     * 
     * Replace all subtraction patterns with single characters, then sum up.
     */
    public int romanToIntV5(String s) {
        // Replace subtraction patterns with placeholder characters
        s = s.replace("IV", "a"); // a = 4
        s = s.replace("IX", "b"); // b = 9
        s = s.replace("XL", "c"); // c = 40
        s = s.replace("XC", "d"); // d = 90
        s = s.replace("CD", "e"); // e = 400
        s = s.replace("CM", "f"); // f = 900

        int result = 0;
        for (char c : s.toCharArray()) {
            switch (c) {
                case 'I':
                    result += 1;
                    break;
                case 'V':
                    result += 5;
                    break;
                case 'X':
                    result += 10;
                    break;
                case 'L':
                    result += 50;
                    break;
                case 'C':
                    result += 100;
                    break;
                case 'D':
                    result += 500;
                    break;
                case 'M':
                    result += 1000;
                    break;
                case 'a':
                    result += 4;
                    break; // IV
                case 'b':
                    result += 9;
                    break; // IX
                case 'c':
                    result += 40;
                    break; // XL
                case 'd':
                    result += 90;
                    break; // XC
                case 'e':
                    result += 400;
                    break; // CD
                case 'f':
                    result += 900;
                    break; // CM
            }
        }

        return result;
    }

    /**
     * Bonus: Integer to Roman conversion for testing
     */
    public String intToRoman(int num) {
        String[] thousands = { "", "M", "MM", "MMM" };
        String[] hundreds = { "", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM" };
        String[] tens = { "", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC" };
        String[] ones = { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX" };

        return thousands[num / 1000] + hundreds[(num % 1000) / 100] +
                tens[(num % 100) / 10] + ones[num % 10];
    }

    // Test all solutions
    public static void main(String[] args) {
        RomanToInteger solution = new RomanToInteger();

        // Test cases
        String[] testCases = {
                "III", // 3
                "LVIII", // 58
                "MCMXC", // 1990
                "IV", // 4
                "IX", // 9
                "XL", // 40
                "XC", // 90
                "CD", // 400
                "CM", // 900
                "MCDXLIV", // 1444
                "MMMCMXCIX" // 3999
        };

        int[] expected = { 3, 58, 1990, 4, 9, 40, 90, 400, 900, 1444, 3999 };

        System.out.println("=== Testing All Solutions ===");
        for (int i = 0; i < testCases.length; i++) {
            String roman = testCases[i];
            int expectedValue = expected[i];

            System.out.println("\nRoman: " + roman + " -> Expected: " + expectedValue);

            int result1 = solution.romanToIntV1(roman);
            int result2 = solution.romanToIntV2(roman);
            int result3 = solution.romanToIntV3(roman);
            int result4 = solution.romanToIntV4(roman);
            int result5 = solution.romanToIntV5(roman);

            System.out.println("V1 (Left->Right): " + result1);
            System.out.println("V2 (Right->Left): " + result2);
            System.out.println("V3 (Switch): " + result3);
            System.out.println("V4 (Pattern): " + result4);
            System.out.println("V5 (Replace): " + result5);

            boolean allCorrect = result1 == expectedValue && result2 == expectedValue &&
                    result3 == expectedValue && result4 == expectedValue &&
                    result5 == expectedValue;
            System.out.println("All correct: " + allCorrect);
        }

        // Performance test
        System.out.println("\n=== Performance Test ===");
        String testString = "MMMCMXCIX"; // 3999
        int iterations = 100000;

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.romanToIntV2(testString);
        }
        long rightToLeftTime = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            solution.romanToIntV3(testString);
        }
        long switchTime = System.nanoTime() - startTime;

        System.out.println("Right-to-Left (HashMap): " + rightToLeftTime / 1000000.0 + " ms");
        System.out.println("Switch Statement: " + switchTime / 1000000.0 + " ms");
        System.out.println("Switch is " + String.format("%.2f", (double) rightToLeftTime / switchTime) + "x faster");

        // Validation with reverse conversion
        System.out.println("\n=== Validation with Reverse Conversion ===");
        for (int num = 1; num <= 20; num++) {
            String roman = solution.intToRoman(num);
            int converted = solution.romanToIntV3(roman);
            System.out.println(num + " -> " + roman + " -> " + converted +
                    (num == converted ? " ✓" : " ✗"));
        }
    }

}

class Solution {

    Map<Character, Integer> roman = new HashMap<>();

    Solution() {
        roman.put('I', 1);
        roman.put('V', 5);
        roman.put('X', 10);
        roman.put('L', 50);
        roman.put('C', 100);
        roman.put('D', 500);
        roman.put('M', 1000);
    }

    public int romanToInt(String s) {
        int res = 0;
        for (int i = 0; i < s.length() - 1; i++) {
            if (roman.get(s.charAt(i)) < roman.get(s.charAt(i + 1))) {
                res -= roman.get(s.charAt(i));
            } else {
                res += roman.get(s.charAt(i));
            }
        }

        return res + roman.get(s.charAt(s.length() - 1));
    }

}
