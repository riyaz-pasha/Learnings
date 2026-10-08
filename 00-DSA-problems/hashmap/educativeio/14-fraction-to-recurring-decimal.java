/**
 * ============================================================================
 * 166. FRACTION TO RECURRING DECIMAL
 * ============================================================================
 * 
 * ## 1. Clarifying questions
 * 1. "Can the inputs be negative?"
 *    (Crucial for sign handling. A negative divided by a negative is positive, but one negative makes the whole string negative. We need an XOR check.)
 * 2. "What happens if the numerator is 0?"
 *    (Prevents unnecessary processing; we should just return "0" immediately rather than "0.0" or "-0".)
 * 3. "Are the numbers strictly within the 10^5 constraint, or could they hit Integer.MIN_VALUE in real scenarios?"
 *    (Although the prompt guarantees a small range, in real system design, taking `Math.abs(Integer.MIN_VALUE)` overflows a 32-bit signed integer. We should cast to `long` to be bulletproof.)
 * 4. "Is there a maximum length for the repeating sequence?"
 *    (Mathematical property: the repeating sequence of a fraction n/d is strictly less than 'd'. This guarantees our memory won't exceed O(d) for the hash map.)
 * 
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We must simulate division and identify exactly when a decimal sequence begins to repeat. 
 * > The bottleneck is detecting a cycle without infinite looping or re-calculating previous digits.
 * 
 * ### Approach 1: Naive Long Division Simulation (Brute Force)
 * 1. What I'd naturally try: Calculate the integer part (`numerator / denominator`). If there's a remainder, 
 *    append a `.`. Then, loop: multiply the remainder by 10, append `rem / denominator`, and update the 
 *    remainder to `rem % denominator`. Stop when remainder is 0.
 * 2. Why it works: It perfectly mimics how a human does long division on paper.
 * 3. Why it's flawed: 
 *    - Time/Space: Infinite! If we divide 1 by 3, the remainder is 1. We multiply by 10, divide by 3, get 
 *      remainder 1. It never reaches 0. We hit an infinite loop and OutOfMemoryError.
 * 4. What work is being repeated: We are calculating the result of `10 / 3` over and over again. Every time 
 *    we see a remainder of `1`, the exact same sequence of math will follow.
 * 5. What property removes the bottleneck: Determinism. In math, division state is 100% defined by the 
 *    current remainder. If you see a remainder you have seen before, the sequence of digits *must* repeat.
 * 
 * ### Approach 2: State Tracking via HashMap (The Optimal Way)
 * 1. What I'd naturally try: Keep the long division loop, but add a `HashMap<Long, Integer>`. The key is 
 *    the *remainder*, and the value is the *index in the string* where we first saw this remainder.
 * 2. Why it works: When we calculate a remainder, we check the map. If it exists, we know exactly where 
 *    the cycle started! We just insert a `(` at that saved index, append a `)`, and stop the loop.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(D) — where D is the absolute value of the denominator. By the Pigeonhole Principle, 
 *      a remainder can only take values from 1 to D-1. Thus, we will either hit 0 or repeat a remainder 
 *      within D steps.
 *    - Space Complexity: O(D) — because we store at most D distinct remainders in the HashMap and our 
 *      StringBuilder will grow to at most size D.
 * 
 * > **Interview Strategy:** There is really only one true way to solve this problem optimally. I would 
 * > explicitly talk through the "paper and pencil" long division method out loud, point out that a repeating 
 * > remainder means a repeating quotient, and then immediately write Approach 2.
 * 
 * 
 * ## 3. Edge cases
 * - Numerator is 0: Return `"0"` immediately to avoid `-0` edge cases.
 * - Negative signs: `1 / -2` is `"-0.5"`, but `-1 / -2` is `"0.5"`. Use `XOR (^)` to check if exactly one is negative.
 * - Integer boundaries: Even with constraints of 10^5, it's an industry standard to cast to `long` before 
 *   using `Math.abs()` to avoid the catastrophic `-2147483648` absolute value overflow trap.
 * - No fractional part: `4 / 2` -> Just return `"2"`. Don't append `.` if remainder is 0.
 * 
 * 
 * ## 4. Key Insight & Dry Run
 * 
 * **Key Insight:** "Remainder = State." 
 * You don't track the *quotient digits* to find repeats (e.g., looking for "1212"), because digits can 
 * repeat without being part of the cycle (e.g., `0.11(2)`). You must track the *remainder*. The remainder 
 * guarantees mathematical repetition.
 * 
 * **Long Division ASCII Visual (4 / 33):**
 *      0. 1  2  1 ...
 *    +---------
 * 33 | 4. 0  0  0
 *      3  3  |  |    <-- (rem 4 * 10 = 40. 40/33 = 1. rem = 7)
 *      ----  |  |
 *         7  0  |    <-- (rem 7 * 10 = 70. 70/33 = 2. rem = 4)
 *         6  6  |
 *         ----  |
 *            4  0    <-- Wait! We just got rem 4 again! Cycle detected.
 * 
 * **Dry Run on 4 / 33:**
 * 1. res = "0."
 * 2. rem = 4. 
 *    - Map: `{4 -> index 2}` (since res.length() is 2).
 *    - 4 * 10 = 40. 40 / 33 = 1. res = "0.1". rem = 7.
 * 3. rem = 7. 
 *    - Map: `{4 -> 2, 7 -> 3}`.
 *    - 7 * 10 = 70. 70 / 33 = 2. res = "0.12". rem = 4.
 * 4. rem = 4.
 *    - 4 is IN THE MAP at index 2! 
 *    - `res.insert(2, "(")` -> "0.(12"
 *    - `res.append(")")` -> "0.(12)"
 *    - Break. Return.
 * 
 * 
 * ## 5. Follow-ups
 * - **Q: What is the maximum possible length of the repeating string?**
 *   **A:** D - 1, where D is the denominator. A number divided by D can only produce remainders from 1 to D-1. 
 *   If we hit D distinct remainders, the Pigeonhole Principle dictates the next one MUST be a repeat.
 * - **Q: How would you modify this if we wanted the output in base 2 (binary) instead of base 10?**
 *   **A:** The logic is identical, except inside the loop we multiply the remainder by `2` instead of `10` 
 *   before dividing by the denominator.
 * - **Q: Why use StringBuilder's `.insert()` instead of manipulating strings manually?**
 *   **A:** `StringBuilder.insert()` does an array copy under the hood, which is O(N). Since our max string 
 *   length is bounded by D, doing one O(N) array shift at the very end is vastly more performant than creating 
 *   new immutable String objects.
 * 
 * ============================================================================
 */

import java.util.HashMap;
import java.util.Map;

public class FractionToDecimalStudy {

    interface FractionConverter {
        String fractionToDecimal(int numerator, int denominator);
    }

    // ========================================================================
    // APPROACH 2: REMAINDER STATE HASHMAP (The Optimal Solution)
    // ========================================================================
    static class HashMapSolver implements FractionConverter {
        @Override
        public String fractionToDecimal(int numerator, int denominator) {
            // Edge case: numerator is 0. Return instantly.
            if (numerator == 0) {
                return "0";
            }

            StringBuilder res = new StringBuilder();

            // 1. Sign handling: If signs differ, the result is negative.
            // Using XOR (^) is the cleanest way to check if exactly one is negative.
            if ((numerator < 0) ^ (denominator < 0)) {
                res.append("-");
            }

            // 2. Cast to long and take absolute value to prevent overflow
            // e.g., Math.abs(-2147483648) overflows a 32-bit int.
            long num = Math.abs((long) numerator);
            long den = Math.abs((long) denominator);

            // 3. Integral part
            res.append(num / den);
            long rem = num % den;

            // If there's no fractional part, we are done.
            if (rem == 0) {
                return res.toString();
            }

            // 4. Fractional part
            res.append(".");
            
            // Map tracks <Remainder, Index in StringBuilder>
            // Concrete example: if we see remainder 4 at index 2, we store {4: 2}
            Map<Long, Integer> remainderMap = new HashMap<>();

            while (rem != 0) {
                // If we've seen this remainder before, a cycle has started!
                if (remainderMap.containsKey(rem)) {
                    int cycleStartIndex = remainderMap.get(rem);
                    res.insert(cycleStartIndex, "(");
                    res.append(")");
                    break;
                }

                // Record the index where this remainder's quotient digit will be placed
                remainderMap.put(rem, res.length());

                // Simulate bringing down a zero
                rem *= 10;
                
                // Append the quotient digit
                res.append(rem / den);
                
                // Update the remainder for the next iteration
                rem %= den;
            }

            return res.toString();
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        FractionConverter solver = new HashMapSolver();

        // Format: {numerator, denominator, expected_output}
        String[][] testCases = {
            {"1", "2", "0.5"},             // Simple terminating decimal
            {"2", "1", "2"},               // Integer result, no decimal
            {"2", "3", "0.(6)"},           // Single repeating digit
            {"4", "33", "0.(12)"},         // Multiple repeating digits
            {"1", "6", "0.1(6)"},          // Repeating digit starting after a non-repeating digit
            {"-1", "4", "-0.25"},          // Negative numerator
            {"1", "-4", "-0.25"},          // Negative denominator
            {"-1", "-4", "0.25"},          // Both negative
            {"0", "99", "0"},              // Zero numerator
            // Although prompt says bounds are 10^5, it's good to verify MIN_VALUE robustness
            {"-2147483648", "-1", "2147483648"} 
        };

        System.out.println("Testing HashMapSolver...\n");
        boolean allPassed = true;

        for (int i = 0; i < testCases.length; i++) {
            int num = Integer.parseInt(testCases[i][0]);
            int den = Integer.parseInt(testCases[i][1]);
            String expected = testCases[i][2];

            String result = solver.fractionToDecimal(num, den);

            if (!result.equals(expected)) {
                System.out.printf("  [FAIL] %d / %d. Expected: %s, Got: %s%n", num, den, expected, result);
                allPassed = false;
            } else {
                System.out.printf("  [PASS] %d / %d = %s%n", num, den, result);
            }
        }

        if (allPassed) {
            System.out.println("\n[SUCCESS] All edge cases and examples passed!");
        }
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * - Core pattern: Long division simulation + State tracking.
 * - Key observation: In division, the remainder uniquely defines the state. 
 *   If you see the exact same remainder again, the math will infinitely loop 
 *   producing the exact same digits.
 * - What's worth memorizing: The sequence: `rem *= 10; res.append(rem/den); rem %= den;`.
 * - Most common trap: Trying to look for repeated strings in the quotient. 
 *   The string "0.11" has repeated '1's, but the fraction 11/100 does not have a 
 *   repeating mathematical cycle. ONLY track remainders.
 * - Mental trigger: "Recurring decimals? Track the remainder with a HashMap."
 * ============================================================================
 */
