/**
 * ============================================================================
 * ADDITIVE NUMBER - COMPREHENSIVE GUIDE & SOLUTIONS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * We are given a string of digits (like "112358"). We need to check if we can 
 * slice this string into a sequence of numbers that acts like a Fibonacci 
 * sequence. Specifically, starting from the third number, every number must 
 * be the exact sum of the two numbers immediately before it 
 * (e.g., 1 + 1 = 2, 1 + 2 = 3, 2 + 3 = 5, 3 + 5 = 8). 
 * If we can partition the whole string this way without any leftovers and 
 * without using invalid leading zeros, we return true.
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: How large can the numbers get?
 * A: The string length is up to 35. This is a massive hint! A 64-bit `long` 
 *    can only hold up to 19 digits. We will likely need to use `BigInteger` 
 *    (or string addition) to prevent overflow errors.
 * 
 * Q: What about leading zeros?
 * A: The problem states "03" is invalid. This means if a number starts with 
 *    '0', it MUST be exactly the number "0" (length of 1). It cannot be "01", 
 *    "00", etc.
 * 
 * Q: What is the minimum sequence length?
 * A: It must contain at least three numbers (Num1 + Num2 = Num3).
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - FIXED STARTING POINTS: Unlike standard permutations where every step requires 
 *   a choice, an additive sequence is STRICTLY deterministic. Once you choose 
 *   Number 1 and Number 2, Number 3 is mathematically forced.
 * - TWO LOOPS ARE ENOUGH: Because of this deterministic nature, we only need 
 *   to write two nested loops to guess the lengths of Number 1 and Number 2. 
 *   Once guessed, the rest of the string either matches the required sums or it doesn't.
 * - PRUNING: The sum of two numbers will always have at least as many digits 
 *   as the largest of the two numbers. If the remaining string is shorter than 
 *   max(len1, len2), it's impossible, and we can stop checking.
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Point out the constraints. Explicitly state, "Because length is 35, 
 *   standard integers will overflow. I will use BigInteger."
 * - Step 2: Explain the math. "Since Num3 is forced by Num1 and Num2, I don't 
 *   need full recursion to guess every comma. I only loop to find the first two."
 * - Step 3: Write the loops, heavily emphasizing the leading-zero checks and 
 *   length-pruning to show you write optimized code.
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * String: "199100199"
 * 
 * i = 0 (Num1 = "1")
 *   j = 2 (Num2 = "99")
 *     -> Expected Num3 = 1 + 99 = 100
 *     -> Does remaining "100199" start with "100"? YES.
 *     -> Chop "100". Remaining = "199"
 *     
 *     -> New Num1 = 99, New Num2 = 100
 *     -> Expected Num3 = 99 + 100 = 199
 *     -> Does remaining "199" start with "199"? YES.
 *     -> Chop "199". Remaining = "" (Empty!)
 *     -> Return TRUE.
 */

import java.math.BigInteger;

public class AdditiveNumber {

    public boolean isAdditiveNumber(String num) {

        /*
         * ============================================================
         * HOW TO THINK ABOUT THIS PROBLEM
         * ============================================================
         *
         * My first instinct might be full backtracking:
         * "Try placing commas everywhere in the string."
         *
         * But full backtracking is OVERKILL. 
         *
         * ============================================================
         * THE IMPORTANT MENTAL SHIFT
         * ============================================================
         *
         * Instead of making a decision for every single digit, 
         * we ONLY need to make a decision for the FIRST TWO numbers.
         *
         * If I know the 1st number and the 2nd number, the 3rd number 
         * is mathematically FORCED (1st + 2nd). 
         * 
         * Once the first two are chosen, we just simulate the additions 
         * and see if the remaining string perfectly matches our sums.
         */

        int n = num.length();

        /*
         * ============================================================
         * WHAT ARE THE DECISIONS?
         * ============================================================
         *
         * Outer loop 'i': End index of the FIRST number.
         * Inner loop 'j': End index of the SECOND number.
         *
         * 'i' only goes up to n/2 because the remaining string must be 
         * long enough to hold the sum of the first two numbers.
         */
        for (int i = 0; i < n / 2; i++) {

            /*
             * LEADING ZERO CHECK FOR NUMBER 1
             * If the first char is '0', the only valid first number is exactly "0".
             * If i > 0, it means we are trying to form "0x", which is invalid.
             */
            if (num.charAt(0) == '0' && i > 0) {
                break; 
            }

            for (int j = i + 1; j < n - 1; j++) {

                /*
                 * LEADING ZERO CHECK FOR NUMBER 2
                 * If it starts with '0', it can only be exactly "0" (when j == i + 1).
                 * If j > i + 1, it's "0x", which is invalid. Break inner loop.
                 */
                if (num.charAt(i + 1) == '0' && j > i + 1) {
                    break;
                }

                /*
                 * ============================================================
                 * PRUNING: LENGTH CONSTRAINTS
                 * ============================================================
                 * The sum of num1 and num2 will have AT LEAST max(len1, len2) digits.
                 * If the remaining string is shorter than that, it is mathematically 
                 * impossible for it to contain the sum. Skip checking entirely!
                 */
                int len1 = i + 1;
                int len2 = j - i;
                int remainingLen = n - 1 - j;

                if (remainingLen < Math.max(len1, len2)) {
                    break;
                }

                String first = num.substring(0, i + 1);
                String second = num.substring(i + 1, j + 1);

                // Start the deterministic verification
                if (isValidAdditiveSequence(first, second, num.substring(j + 1))) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isValidAdditiveSequence(String first, String second, String remaining) {
        
        /*
         * ============================================================
         * WHY BIGINTEGER?
         * ============================================================
         * The string length is up to 35. A 64-bit `long` holds max 19 digits.
         * If we parse a 20-digit string, it will throw NumberFormatException.
         */
        BigInteger num1 = new BigInteger(first);
        BigInteger num2 = new BigInteger(second);

        /*
         * ============================================================
         * THE DETERMINISTIC SIMULATION
         * ============================================================
         * Keep calculating (num1 + num2).
         * Check if 'remaining' STARTS with that exact sum.
         * If it does, chop off that sum from 'remaining' and repeat!
         */
        while (remaining.length() > 0) {
            
            BigInteger nextSum = num1.add(num2);
            String nextSumStr = nextSum.toString();

            // If the remaining string does NOT start with the exact digits 
            // of our forced sum, this entire path is a failure.
            if (!remaining.startsWith(nextSumStr)) {
                return false;
            }

            // The remaining string DOES start with the sum!
            // Chop off the part we just matched.
            remaining = remaining.substring(nextSumStr.length());

            // Shift our pointers for the next iteration
            num1 = num2;
            num2 = nextSum;
        }

        /*
         * BASE CASE REACHED
         * The loop finished (remaining.length() == 0). We successfully matched 
         * every digit against our forced sequence. It's valid!
         */
        return true;
    }
}

public class AdditiveNumberDeepExplanation {

    public static void main(String[] args) {
        System.out.println(isAdditiveNumber("112358"));     // true
        System.out.println(isAdditiveNumber("199100199"));  // true
        System.out.println(isAdditiveNumber("1023"));       // false
    }

    /**
     * ===============================================================
     * 🧠 PROBLEM UNDERSTANDING (VERY IMPORTANT FOR INTERVIEW)
     * ===============================================================
     *
     * We are given a string of digits.
     * We need to check if we can split it into numbers such that:
     *
     *      a, b, c, d, ...
     *
     * where:
     *      c = a + b
     *      d = b + c
     *      ...
     *
     * 👉 Minimum length of sequence = 3 numbers
     *
     * ===============================================================
     * 💡 KEY OBSERVATION (CRITICAL INSIGHT)
     * ===============================================================
     *
     * Only the FIRST TWO numbers are choices.
     *
     * Once we fix:
     *      first = a
     *      second = b
     *
     * Then:
     *      third MUST be (a + b)
     *      fourth MUST be (b + c)
     *
     * 👉 So after picking first 2 numbers → NO MORE CHOICES
     * 👉 Just VALIDATION
     *
     * This reduces exponential → O(N^3)
     *
     * ===============================================================
     * 🔁 WHY TWO LOOPS?
     * ===============================================================
     *
     * We try ALL possible splits:
     *
     * Example: "112358"
     *
     * i = end of first number
     * j = end of second number
     *
     * first  = num[0...i-1]
     * second = num[i...j-1]
     *
     * Then validate rest
     *
     * ===============================================================
     * ⛔ LEADING ZERO RULE
     * ===============================================================
     *
     * "0"     ✅ valid
     * "01"    ❌ invalid
     *
     * So:
     * If number starts with '0', it must be exactly length 1
     *
     * ===============================================================
     * ⏱️ TIME COMPLEXITY
     * ===============================================================
     * Outer loops: O(N^2)
     * Validation: O(N)
     * Total: O(N^3)
     *
     * ===============================================================
     * 🧠 SPACE COMPLEXITY
     * ===============================================================
     * O(1) (ignoring BigInteger)
     */
    public static boolean isAdditiveNumber(String num) {

        int n = num.length();

        // ----------------------------------------------------------
        // LOOP 1: choose end of first number
        // ----------------------------------------------------------
        for (int i = 1; i <= n - 2; i++) {

            /**
             * 🚫 Leading zero check for FIRST number
             *
             * If string starts with '0', then only valid first number is "0"
             * So if length > 1 → break (no need to continue further)
             */
            if (num.charAt(0) == '0' && i > 1) break;

            // ------------------------------------------------------
            // LOOP 2: choose end of second number
            // ------------------------------------------------------
            for (int j = i + 1; j <= n - 1; j++) {

                /**
                 * 🚫 Leading zero check for SECOND number
                 *
                 * If second number starts with '0',
                 * it must be exactly "0"
                 */
                if (num.charAt(i) == '0' && (j - i) > 1) break;

                // Extract first and second numbers
                String first = num.substring(0, i);
                String second = num.substring(i, j);

                /**
                 * 🔍 Now we FIXED first and second
                 *
                 * Next step:
                 * Check if entire remaining string follows additive rule
                 */
                if (isValid(first, second, j, num)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * ===============================================================
     * 🔥 VALIDATION FUNCTION (CORE LOGIC)
     * ===============================================================
     *
     * Given:
     *      first, second
     *
     * We simulate:
     *
     *      third = first + second
     *      fourth = second + third
     *      ...
     *
     * And check if string matches exactly.
     *
     * ===============================================================
     * WHY BigInteger?
     * ===============================================================
     *
     * Length can be up to 35 digits.
     * That exceeds long range → use BigInteger
     *
     * ===============================================================
     * 🧠 THINKING MODEL
     * ===============================================================
     *
     * Move pointer forward while matching expected sums
     *
     */
    private static boolean isValid(String first, String second, int start, String num) {

        // Convert to BigInteger to handle large values
        BigInteger a = new BigInteger(first);
        BigInteger b = new BigInteger(second);

        // Count numbers to ensure >= 3
        int count = 2;

        /**
         * Continue until we consume entire string
         */
        while (start < num.length()) {

            // Expected next number
            BigInteger sum = a.add(b);
            String sumStr = sum.toString();

            /**
             * 🔍 Check if current part of string starts with expected sum
             *
             * Example:
             * num = "112358"
             * start = 2
             * expected = "2"
             *
             * So num.startsWith("2", 2) → true
             */
            if (!num.startsWith(sumStr, start)) {
                return false;
            }

            /**
             * Move pointer forward
             */
            start += sumStr.length();

            /**
             * Shift window:
             * (a, b) → (b, sum)
             *
             * Example:
             * 1, 1 → 1, 2 → 2, 3 → 3, 5
             */
            a = b;
            b = sum;

            count++;
        }

        /**
         * Ensure at least 3 numbers exist
         */
        return count >= 3;
    }
}
