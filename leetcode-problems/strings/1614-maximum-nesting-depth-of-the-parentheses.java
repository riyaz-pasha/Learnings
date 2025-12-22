import java.util.Stack;

class MaximumNestingDepth {

    /**
     * Approach 1: Counter Approach (Most Intuitive)
     * Time Complexity: O(n), Space Complexity: O(1)
     * Track current depth and maximum depth
     */
    public int maxDepth1(String s) {
        int currentDepth = 0;
        int maxDepth = 0;

        for (char c : s.toCharArray()) {
            if (c == '(') {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            } else if (c == ')') {
                currentDepth--;
            }
        }

        return maxDepth;
    }

    /**
     * Approach 2: Single Pass with Inline Max Update
     * Time Complexity: O(n), Space Complexity: O(1)
     * Slightly optimized version
     */
    public int maxDepth2(String s) {
        int depth = 0;
        int maxDepth = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
                if (depth > maxDepth) {
                    maxDepth = depth;
                }
            } else if (c == ')') {
                depth--;
            }
        }

        return maxDepth;
    }

    /**
     * Approach 3: Using Stack (Educational Purpose)
     * Time Complexity: O(n), Space Complexity: O(n)
     * Shows the stack-based thinking, though not necessary
     */
    public int maxDepth3(String s) {
        Stack<Character> stack = new Stack<>();
        int maxDepth = 0;

        for (char c : s.toCharArray()) {
            if (c == '(') {
                stack.push(c);
                maxDepth = Math.max(maxDepth, stack.size());
            } else if (c == ')') {
                stack.pop();
            }
        }

        return maxDepth;
    }

    /**
     * Approach 4: Stream API (Functional Style)
     * Time Complexity: O(n), Space Complexity: O(n)
     * Modern Java approach using streams
     */
    public int maxDepth4(String s) {
        int[] result = { 0, 0 }; // {currentDepth, maxDepth}

        s.chars().forEach(c -> {
            if (c == '(') {
                result[0]++;
                result[1] = Math.max(result[1], result[0]);
            } else if (c == ')') {
                result[0]--;
            }
        });

        return result[1];
    }

    /**
     * Approach 5: Compact One-Liner Style
     * Time Complexity: O(n), Space Complexity: O(1)
     * For code golf enthusiasts
     */
    public int maxDepth5(String s) {
        int depth = 0, max = 0;
        for (char c : s.toCharArray()) {
            if (c == '(')
                max = Math.max(max, ++depth);
            else if (c == ')')
                depth--;
        }
        return max;
    }

    /**
     * Approach 6: With Detailed Tracking
     * Time Complexity: O(n), Space Complexity: O(1)
     * Shows which character has maximum depth
     */
    public int maxDepth6(String s) {
        int currentDepth = 0;
        int maxDepth = 0;
        int maxDepthIndex = -1;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                currentDepth++;
                if (currentDepth > maxDepth) {
                    maxDepth = currentDepth;
                    maxDepthIndex = i;
                }
            } else if (c == ')') {
                currentDepth--;
            }
        }

        // For debugging: show where max depth occurs
        if (maxDepthIndex != -1) {
            System.out.println("Max depth " + maxDepth +
                    " occurs at index " + maxDepthIndex);
        }

        return maxDepth;
    }

    /**
     * Bonus: Visualize the nesting depth at each position
     * Useful for understanding the problem
     */
    public void visualizeDepth(String s) {
        System.out.println("String: " + s);
        System.out.print("Depth:  ");

        int depth = 0;
        for (char c : s.toCharArray()) {
            if (c == '(') {
                depth++;
                System.out.print(depth);
            } else if (c == ')') {
                System.out.print(depth);
                depth--;
            } else {
                System.out.print(depth);
            }
        }
        System.out.println("\n");
    }

    // Test method
    public static void main(String[] args) {
        MaximumNestingDepth solution = new MaximumNestingDepth();

        // Test Case 1
        System.out.println("Test Case 1:");
        String s1 = "(1+(2*3)+((8)/4))+1";
        System.out.println("Input: s = \"" + s1 + "\"");
        System.out.println("Output: " + solution.maxDepth1(s1));
        System.out.println("Expected: 3");
        solution.visualizeDepth(s1);

        // Test Case 2
        System.out.println("Test Case 2:");
        String s2 = "(1)+((2))+(((3)))";
        System.out.println("Input: s = \"" + s2 + "\"");
        System.out.println("Output: " + solution.maxDepth1(s2));
        System.out.println("Expected: 3");
        solution.visualizeDepth(s2);

        // Test Case 3
        System.out.println("Test Case 3:");
        String s3 = "()(())((()()))";
        System.out.println("Input: s = \"" + s3 + "\"");
        System.out.println("Output: " + solution.maxDepth1(s3));
        System.out.println("Expected: 3");
        solution.visualizeDepth(s3);

        // Test Case 4 - Edge Cases
        System.out.println("Test Case 4 (Empty/Simple):");
        System.out.println("\"\" → " + solution.maxDepth1(""));
        System.out.println("\"abc\" → " + solution.maxDepth1("abc"));
        System.out.println("\"()\" → " + solution.maxDepth1("()"));
        System.out.println("\"(((())))\" → " + solution.maxDepth1("(((())))"));

        // Performance comparison
        System.out.println("\n--- Performance Comparison ---");
        String testStr = "((((((((((1))))))))))".repeat(100);

        long start = System.nanoTime();
        int result1 = solution.maxDepth1(testStr);
        long time1 = System.nanoTime() - start;

        start = System.nanoTime();
        int result3 = solution.maxDepth3(testStr);
        long time3 = System.nanoTime() - start;

        System.out.println("Counter approach: " + result1 + " (" + time1 + " ns)");
        System.out.println("Stack approach: " + result3 + " (" + time3 + " ns)");
        System.out.println("Counter is faster due to O(1) space!");
    }
}
