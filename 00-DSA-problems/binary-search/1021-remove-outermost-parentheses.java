/*
 * A valid parentheses string is either empty "", "(" + A + ")", or A + B, where
 * A and B are valid parentheses strings, and + represents string concatenation.
 * 
 * For example, "", "()", "(())()", and "(()(()))" are all valid parentheses
 * strings.
 * A valid parentheses string s is primitive if it is nonempty, and there does
 * not exist a way to split it into s = A + B, with A and B nonempty valid
 * parentheses strings.
 * 
 * Given a valid parentheses string s, consider its primitive decomposition: s =
 * P1 + P2 + ... + Pk, where Pi are primitive valid parentheses strings.
 * 
 * Return s after removing the outermost parentheses of every primitive string
 * in the primitive decomposition of s.
 * 
 * Example 1:
 * 
 * Input: s = "(()())(())"
 * Output: "()()()"
 * Explanation:
 * The input string is "(()())(())", with primitive decomposition "(()())" +
 * "(())".
 * After removing outer parentheses of each part, this is "()()" + "()" =
 * "()()()".
 * Example 2:
 * 
 * Input: s = "(()())(())(()(()))"
 * Output: "()()()()(())"
 * Explanation:
 * The input string is "(()())(())(()(()))", with primitive decomposition
 * "(()())" + "(())" + "(()(()))".
 * After removing outer parentheses of each part, this is "()()" + "()" +
 * "()(())" = "()()()()(())".
 * Example 3:
 * 
 * Input: s = "()()"
 * Output: ""
 * Explanation:
 * The input string is "()()", with primitive decomposition "()" + "()".
 * After removing outer parentheses of each part, this is "" + "" = "".
 */

class RemoveOuterParentheses {

    // Approach 1: Counter/Depth Tracking (Optimal)
    // Time: O(n), Space: O(n)
    public String removeOuterParentheses1(String s) {
        StringBuilder result = new StringBuilder();
        int depth = 0;

        for (char c : s.toCharArray()) {
            if (c == '(') {
                // Only add '(' if it's not the outermost opening
                if (depth > 0) {
                    result.append(c);
                }
                depth++;
            } else { // c == ')'
                depth--;
                // Only add ')' if it's not the outermost closing
                if (depth > 0) {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }

    // Approach 2: Alternative Depth Tracking
    // Time: O(n), Space: O(n)
    public String removeOuterParentheses2(String s) {
        StringBuilder result = new StringBuilder();
        int balance = 0;

        for (char c : s.toCharArray()) {
            if (c == '(' && balance++ > 0) {
                result.append(c);
            }
            if (c == ')' && balance-- > 1) {
                result.append(c);
            }
        }

        return result.toString();
    }

    // Approach 3: Explicit Primitive Decomposition
    // Time: O(n), Space: O(n)
    public String removeOuterParentheses3(String s) {
        StringBuilder result = new StringBuilder();
        int start = 0;
        int balance = 0;

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                balance++;
            } else {
                balance--;
            }

            // Found a complete primitive string
            if (balance == 0) {
                // Add substring without first and last characters (outer parentheses)
                result.append(s.substring(start + 1, i));
                start = i + 1;
            }
        }

        return result.toString();
    }

    // Approach 4: Stack-Based Approach
    // Time: O(n), Space: O(n)
    public String removeOuterParentheses4(String s) {
        StringBuilder result = new StringBuilder();
        int openCount = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '(') {
                // Not the outermost opening
                if (openCount > 0) {
                    result.append(c);
                }
                openCount++;
            } else {
                openCount--;
                // Not the outermost closing
                if (openCount > 0) {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }

    // Approach 5: Two-Pass with Marking (Educational)
    // Time: O(n), Space: O(n)
    public String removeOuterParentheses5(String s) {
        // First pass: identify boundaries of primitive strings
        boolean[] isOuter = new boolean[s.length()];
        int depth = 0;

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                if (depth == 0) {
                    isOuter[i] = true; // Outermost opening
                }
                depth++;
            } else {
                depth--;
                if (depth == 0) {
                    isOuter[i] = true; // Outermost closing
                }
            }
        }

        // Second pass: build result excluding outer parentheses
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (!isOuter[i]) {
                result.append(s.charAt(i));
            }
        }

        return result.toString();
    }

    // Test cases with detailed visualization
    public static void main(String[] args) {
        RemoveOuterParentheses solution = new RemoveOuterParentheses();

        // Test Case 1
        String s1 = "(()())(())";
        String result1 = solution.removeOuterParentheses1(s1);
        System.out.println("Test 1:");
        System.out.println("Input:  " + s1);
        System.out.println("Output: " + result1); // "()()()"
        visualizeDecomposition(s1);

        // Test Case 2
        String s2 = "(()())(())(()(()))";
        String result2 = solution.removeOuterParentheses1(s2);
        System.out.println("\nTest 2:");
        System.out.println("Input:  " + s2);
        System.out.println("Output: " + result2); // "()()()()(())"
        visualizeDecomposition(s2);

        // Test Case 3
        String s3 = "()()";
        String result3 = solution.removeOuterParentheses1(s3);
        System.out.println("\nTest 3:");
        System.out.println("Input:  " + s3);
        System.out.println("Output: " + result3); // ""
        visualizeDecomposition(s3);

        // Test Case 4: Single primitive
        String s4 = "(())";
        String result4 = solution.removeOuterParentheses1(s4);
        System.out.println("\nTest 4:");
        System.out.println("Input:  " + s4);
        System.out.println("Output: " + result4); // "()"

        // Test Case 5: Deeply nested
        String s5 = "(((())))";
        String result5 = solution.removeOuterParentheses1(s5);
        System.out.println("\nTest 5:");
        System.out.println("Input:  " + s5);
        System.out.println("Output: " + result5); // "((()))"

        // Compare all approaches
        System.out.println("\nComparing all approaches for Test 1:");
        System.out.println("Approach 1: " + solution.removeOuterParentheses1(s1));
        System.out.println("Approach 2: " + solution.removeOuterParentheses2(s1));
        System.out.println("Approach 3: " + solution.removeOuterParentheses3(s1));
        System.out.println("Approach 4: " + solution.removeOuterParentheses4(s1));
        System.out.println("Approach 5: " + solution.removeOuterParentheses5(s1));
    }

    private static void visualizeDecomposition(String s) {
        System.out.println("\nStep-by-step decomposition:");

        int depth = 0;
        int primitiveStart = 0;
        int primitiveNum = 1;

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                depth++;
            } else {
                depth--;
            }

            if (depth == 0) {
                // Found complete primitive
                String primitive = s.substring(primitiveStart, i + 1);
                String inner = primitive.substring(1, primitive.length() - 1);

                System.out.printf("Primitive %d: %s → Remove outer → %s%n",
                        primitiveNum++, primitive,
                        inner.isEmpty() ? "\"\"" : inner);
                primitiveStart = i + 1;
            }
        }

        // Show depth diagram
        System.out.println("\nDepth diagram:");
        System.out.println(s);
        StringBuilder depthDiagram = new StringBuilder();
        depth = 0;
        for (char c : s.toCharArray()) {
            if (c == '(') {
                depth++;
                depthDiagram.append(depth);
            } else {
                depthDiagram.append(depth);
                depth--;
            }
        }
        System.out.println(depthDiagram);
        System.out.println("^ Depth 1 = outermost (removed)");
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM UNDERSTANDING:
 * 
 * 1. Primitive Parentheses String:
 * - Cannot be split into two valid nonempty parts
 * - Examples: "()", "(())", "(()(()))"
 * - Counter-examples: "()()" = "()" + "()", "(())()" = "(())" + "()"
 * 
 * 2. Goal: Remove outer layer of each primitive substring
 * 
 * KEY INSIGHT - DEPTH/BALANCE TRACKING:
 * 
 * The depth (balance) of parentheses tells us:
 * - depth = 0: We're at the boundary between primitives
 * - depth = 1: We're at the first layer (outermost parentheses)
 * - depth > 1: We're inside, these should be kept
 * 
 * ALGORITHM (Approach 1):
 * 1. Track depth as we scan
 * 2. When seeing '(':
 * - If depth > 0: not outermost, add it
 * - Increment depth
 * 3. When seeing ')':
 * - Decrement depth
 * - If depth > 0: not outermost, add it
 * 
 * EXAMPLE WALKTHROUGH: s = "(()())(())"
 * 
 * Index: 0 1 2 3 4 5 6 7 8 9
 * Char: ( ( ) ( ) ) ( ( ) )
 * Depth: 1 2 1 2 1 0 1 2 1 0
 * 
 * Process each character:
 * i=0: '(' depth=0→1, outermost opening, skip
 * i=1: '(' depth=1→2, inner, add '('
 * i=2: ')' depth=2→1, inner, add ')'
 * i=3: '(' depth=1→2, inner, add '('
 * i=4: ')' depth=2→1, inner, add ')'
 * i=5: ')' depth=1→0, outermost closing, skip
 * → First primitive complete: "(()())" → "()()"
 * 
 * i=6: '(' depth=0→1, outermost opening, skip
 * i=7: '(' depth=1→2, inner, add '('
 * i=8: ')' depth=2→1, inner, add ')'
 * i=9: ')' depth=1→0, outermost closing, skip
 * → Second primitive complete: "(())" → "()"
 * 
 * Result: "()()" + "()" = "()()()"
 * 
 * VISUAL REPRESENTATION:
 * 
 * Input: ( ( ) ( ) ) ( ( ) )
 * Depth: 1 2 1 2 1 0 1 2 1 0
 * Keep? N Y Y Y Y N N Y Y N
 * └─primitive 1─┘ └primitive 2┘
 * 
 * Kept: ( ) ( ) ( )
 * Result: "()()" + "()" = "()()()"
 * 
 * EXAMPLE 2: s = "()()"
 * 
 * Index: 0 1 2 3
 * Char: ( ) ( )
 * Depth: 1 0 1 0
 * 
 * i=0: '(' depth=0→1, outermost, skip
 * i=1: ')' depth=1→0, outermost, skip (depth becomes 0, it's outermost)
 * → First primitive: "()" → ""
 * 
 * i=2: '(' depth=0→1, outermost, skip
 * i=3: ')' depth=1→0, outermost, skip
 * → Second primitive: "()" → ""
 * 
 * Result: "" + "" = ""
 * 
 * WHY THE ALGORITHM WORKS:
 * 
 * 1. Primitive strings always:
 * - Start at depth 0 and return to depth 0
 * - Have exactly one occurrence of depth 1 at the boundaries
 * 
 * 2. Outermost parentheses are exactly those at depth 1:
 * - Opening: depth goes from 0→1
 * - Closing: depth goes from 1→0
 * 
 * 3. Everything at depth > 1 is inside and should be kept
 * 
 * APPROACH 2 EXPLANATION (Compact version):
 * ```java
 * if (c == '(' && balance++ > 0) result.append(c);
 * ```
 * - Increments balance AFTER checking
 * - If balance was > 0 before increment, it's not outermost
 * 
 * ```java
 * if (c == ')' && balance-- > 1) result.append(c);
 * ```
 * - Decrements balance AFTER checking
 * - If balance was > 1 before decrement, it's not outermost
 * 
 * COMPLEXITY ANALYSIS:
 * 
 * Time: O(n)
 * - Single pass through string
 * - Each character processed once
 * - O(1) operations per character
 * 
 * Space: O(n)
 * - StringBuilder to store result
 * - In worst case, result is nearly same size as input
 * - Example: "((()))" → "(())" (only 2 chars removed)
 * 
 * EDGE CASES:
 * 
 * 1. Single primitive: "()" → ""
 * 2. Deeply nested: "((()))" → "(())"
 * 3. Multiple primitives: "()()" → ""
 * 4. Complex nesting: "(()())(())" → "()()()"
 * 5. Maximum nesting: Each inner level is kept
 * 
 * PRACTICAL APPLICATIONS:
 * 
 * 1. Expression simplification
 * 2. Parsing nested structures
 * 3. Code formatting/minification
 * 4. Tree structure manipulation
 * 5. Mathematical expression processing
 * 
 * COMMON MISTAKES TO AVOID:
 * 
 * 1. Don't use actual stack (unnecessary overhead)
 * 2. Don't try to parse primitives first (inefficient)
 * 3. Remember depth 1 = outermost (not depth 0)
 * 4. Handle both '(' and ')' correctly with depth
 * 
 * ALTERNATIVE UNDERSTANDING:
 * 
 * Think of depth as "layers of an onion":
 * - Depth 0: Outside the onion
 * - Depth 1: Outer skin (remove this)
 * - Depth 2+: Inner layers (keep these)
 * 
 * We want to peel off just the outer skin!
 */
