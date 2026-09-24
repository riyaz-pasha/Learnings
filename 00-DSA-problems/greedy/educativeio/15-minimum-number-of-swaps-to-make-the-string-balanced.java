import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * ============================================================================
 * INTERVIEW GUIDE: MINIMUM NUMBER OF SWAPS TO MAKE THE STRING BALANCED
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "Are we only allowed to swap adjacent characters?" 
 *      (Assumption: No, the problem says 'swaps of any two characters'. This 
 *      is crucial because one swap across the string can fix multiple imbalances.)
 *    - "Can the input string have an unequal number of '[' and ']'?" 
 *      (Assumption: The constraints explicitly guarantee n/2 left and n/2 right 
 *      brackets, so it is always possible to balance it.)
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Find the MINIMUM swaps to make the brackets perfectly balanced.
 *    
 *    - Observation 1 (Valid pairs cancel out): A balanced string is essentially 
 *      a series of valid `[]` pairs. If we remove all naturally matching `[]` 
 *      pairs as we read left to right, we are left strictly with the "bad" parts.
 *      
 *    - Observation 2 (The Shape of the Leftovers): If you delete every valid 
 *      matched pair, the remaining string will ALWAYS look exactly like this:
 *      `]]...][[[...[`
 *      All the unmatched closing brackets will be clustered on the left, and 
 *      all the unmatched opening brackets will be on the right.
 *      
 *    - Observation 3 (The Power of One Swap): If we have a leftover string like 
 *      `][`, it takes exactly 1 swap to make it `[]`. 
 *      What if we have `]][[`? Swapping the FIRST `]` with the LAST `[` gives us 
 *      `[][:`. That's 1 swap fixing 2 pairs!
 *      What if we have `]]][[[`? 1 swap gives `[]][[]` (leaves `][` unmatched inside). 
 *      Then 1 more swap fixes the rest. Total = 2 swaps fixing 3 pairs.
 *      
 *    - The Formula: If there are `m` unmatched left brackets (which implies 
 *      there are exactly `m` unmatched right brackets), the number of swaps 
 *      required is exactly `(m + 1) / 2`.
 * 
 * 3. VISUAL EXPLANATION:
 *    String: "]]][[["
 *    
 *    Let's cancel valid pairs: None exist initially.
 *    Unmatched pairs count (m) = 3.
 *    
 *    Step 1: Swap index 0 and index 5.
 *    " [ ] ] [ [ ] "  --> Notice the outer bounds are now matched!
 *    
 *    Cancel out the new valid pairs (the outer ones):
 *    Leaves us with: "] [" (m = 1)
 *    
 *    Step 2: Swap the remaining mismatched brackets.
 *    " [ ] "
 *    
 *    Total swaps: 2. 
 *    Formula test: (3 + 1) / 2 = 2. It works perfectly!
 * 
 * ============================================================================
 */
public class MinSwapsToMakeStringBalanced {

    /**
     * APPROACH 1: Greedy with O(1) Space (Optimal)
     * 
     * Instead of using an actual stack to cancel things out, we just keep a 
     * counter of how many unmatched '[' we currently have. 
     * 
     * Time Complexity: O(N) where N is the length of the string.
     * Space Complexity: O(1) as we only use an integer counter.
     */
    public int minSwapsOptimal(String s) {
        int unmatchedLeftBrackets = 0;
        
        // We use charAt to avoid creating a new char[] array, 
        // which saves memory for very large strings (up to 1,000,000 characters).
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            
            if (c == '[') {
                // We found a left bracket. We hope a right bracket matches it later.
                unmatchedLeftBrackets++;
            } else {
                // c == ']'
                if (unmatchedLeftBrackets > 0) {
                    // This right bracket successfully matched a previous left bracket!
                    // They cancel out.
                    unmatchedLeftBrackets--;
                }
                // Note: If unmatchedLeftBrackets == 0, this ']' is unmatched. 
                // But we don't need to track unmatched right brackets explicitly 
                // because the total number of '[' and ']' are guaranteed to be equal.
            }
        }
        
        // Apply the mathematical formula to the remaining unmatched pairs
        return (unmatchedLeftBrackets + 1) / 2;
    }

    /**
     * APPROACH 2: Stack-based Simulation (Highly Intuitive)
     * 
     * In an interview, it's great to explain this conceptual approach first.
     * If we push to a stack and pop when a valid pair is formed, whatever is 
     * left in the stack represents our "unmatched" count `m`.
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(N) for the Stack.
     */
    public int minSwapsStack(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            
            if (c == '[') {
                stack.push(c);
            } else {
                // If we see ']' and the top of the stack is '[', they cancel out
                if (!stack.isEmpty() && stack.peek() == '[') {
                    stack.pop();
                } else {
                    // Otherwise, it's an unmatched ']'
                    stack.push(c);
                }
            }
        }
        
        // Since both '[' and ']' are left in the stack representing imbalances,
        // and we know they are perfectly equal in quantity, the number of unmatched 
        // pairs 'm' is exactly half the stack size.
        int m = stack.size() / 2;
        
        return (m + 1) / 2;
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Records (introduced in Java 14) provide a concise way to create immutable data carriers.
     */
    record TestCase(String s, int expected) {}

    public static void main(String[] args) {
        MinSwapsToMakeStringBalanced solver = new MinSwapsToMakeStringBalanced();
        
        // Defining test cases using our Record
        var testCases = List.of(
            new TestCase("][][", 1),         // One swap fixes the alternating mismatch
            new TestCase("]]][[[", 2),       // Two swaps fix the extreme mismatch
            new TestCase("[]", 0),           // Already balanced
            new TestCase("[]][[]", 1),       // Middle mismatch
            new TestCase("[[[]]]", 0)        // Deeply nested but already balanced
        );
        
        System.out.println("--- Running Approach 1 (Greedy O(1) Space) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.minSwapsOptimal(tc.s());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (Stack Simulation O(N) Space) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.minSwapsStack(tc.s());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}
