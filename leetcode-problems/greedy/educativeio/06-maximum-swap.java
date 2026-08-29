import java.util.List;

/**
 * ============================================================================
 * INTERVIEW GUIDE: MAXIMUM SWAP
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "Can I swap the same digit with itself?" 
 *      (Assumption: Yes, but it wouldn't change the number. We want to maximize it).
 *    - "What if the number is already the maximum possible (e.g., 9973)?" 
 *      (Assumption: We swap nothing or swap identical digits, returning the original number).
 *    - "Are there any negative numbers?" 
 *      (Assumption: Constraints say 0 <= num <= 10^5, so no negatives).
 *    - "What if the highest digit appears multiple times? Which one do I swap?"
 *      (Crucial Question: If num = 1993, swapping the first '9' gives 9193, but 
 *       swapping the second '9' gives 9913. We must pick the RIGHTMOST max digit).
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Swap at most two digits to get the largest possible value.
 *    - Observation 1 (Magnitude): To maximize a number, the most significant digits 
 *      (the ones on the left) should be as large as possible. 
 *    - Observation 2 (Greedy Strategy): We should scan the number from left to right. 
 *      For each digit, we check if there is a LARGER digit anywhere to its right. 
 *      If there are multiple larger digits, we want the ABSOLUTE LARGEST one.
 *    - Observation 3 (The Rightmost Rule): If the largest digit appears multiple times 
 *      (e.g., 1993), swapping with the one furthest to the right yields a bigger 
 *      number (9913 > 9193).
 *    - Approach: Record the last seen index of every digit (0-9). Then, iterate 
 *      through the number from left to right. For each position, check if a larger 
 *      digit (9 down to current_digit + 1) exists at a later index. The first time 
 *      we find such a case, we swap and immediately return.
 * 
 * 3. VISUAL EXPLANATION:
 *    Example: num = 2736
 *    
 *    Step 1: Record last occurrences of each digit.
 *    last['2'] = 0, last['7'] = 1, last['3'] = 2, last['6'] = 3
 *    
 *    Step 2: Iterate left to right and look for a bigger digit later in the array.
 *    Index 0: '2'. 
 *      - Does '9' appear after index 0? No.
 *      - Does '8' appear after index 0? No.
 *      - Does '7' appear after index 0? Yes, at index 1!
 *      -> Swap index 0 ('2') and index 1 ('7'). 
 *      -> Result: 7236. (Return immediately).
 * 
 *    Example: num = 1993
 *    last['1']=0, last['9']=2, last['3']=3
 *    Index 0: '1'. 
 *      - Look for '9'. Found at index 2 (rightmost '9'!).
 *      -> Swap index 0 and index 2.
 *      -> Result: 9913.
 * 
 * ============================================================================
 */
class MaximumSwap {

    /**
     * APPROACH 1: Greedy with Last Occurrence Tracking (Optimal)
     * 
     * Time Complexity: O(N) where N is the number of digits in num. 
     * Since num <= 10^5, N <= 6. This is effectively O(1).
     * Space Complexity: O(N) to store the character array of the number.
     */
    public int maximumSwapOptimal(int num) {
        char[] digits = Integer.toString(num).toCharArray();
        
        // Bucket to store the last occurrence index of each digit (0-9)
        int[] last = new int[10];
        for (int i = 0; i < digits.length; i++) {
            // Convert char to integer value ('7' - '0' = 7)
            last[digits[i] - '0'] = i; 
        }
        
        // Scan the number from left to right
        for (int i = 0; i < digits.length; i++) {
            int currentDigit = digits[i] - '0';
            
            // Check if there is a larger digit (from 9 down to currentDigit + 1)
            for (int d = 9; d > currentDigit; d--) {
                // If a larger digit exists and it appears AFTER the current index
                if (last[d] > i) {
                    // Swap them
                    char temp = digits[i];
                    digits[i] = digits[last[d]];
                    digits[last[d]] = temp;
                    
                    // We only get one swap, so we return immediately
                    return Integer.parseInt(new String(digits));
                }
            }
        }
        
        // If no advantageous swap was found, return the original number
        return num;
    }

    /**
     * APPROACH 2: Brute Force (Highly practical for interviews)
     * 
     * Because N <= 10^5, the maximum number of digits is 6. 
     * A nested loop comparing all pairs takes at most 6 * 5 / 2 = 15 operations.
     * In an interview, acknowledging that Brute Force is effectively O(1) time 
     * due to tiny constraints shows great engineering pragmatism.
     * 
     * Time Complexity: O(N^2) where N is the number of digits.
     * Space Complexity: O(N) for string conversions.
     */
    public int maximumSwapBruteForce(int num) {
        char[] digits = Integer.toString(num).toCharArray();
        int maxNum = num;
        
        // Try all possible pairs to swap
        for (int i = 0; i < digits.length; i++) {
            for (int j = i + 1; j < digits.length; j++) {
                
                // Swap i and j
                char temp = digits[i];
                digits[i] = digits[j];
                digits[j] = temp;
                
                // Check if this new number is the maximum we've seen
                int currentVal = Integer.parseInt(new String(digits));
                if (currentVal > maxNum) {
                    maxNum = currentVal;
                }
                
                // Backtrack (swap back) to evaluate the next pair
                temp = digits[i];
                digits[i] = digits[j];
                digits[j] = temp;
            }
        }
        
        return maxNum;
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     */
    record TestCase(int num, int expected) {}

    public static void main(String[] args) {
        MaximumSwap solver = new MaximumSwap();
        
        // Defining test cases
        var testCases = List.of(
            new TestCase(2736, 7236),
            new TestCase(9973, 9973),
            new TestCase(1993, 9913), // Verifies the "rightmost" rule
            new TestCase(10, 10),     // Edge case: small number, no good swap
            new TestCase(0, 0),       // Edge case: zero
            new TestCase(98368, 98863)
        );
        
        System.out.println("--- Running Approach 1 (Greedy Optimal) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.maximumSwapOptimal(tc.num());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (Brute Force) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.maximumSwapBruteForce(tc.num());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}
