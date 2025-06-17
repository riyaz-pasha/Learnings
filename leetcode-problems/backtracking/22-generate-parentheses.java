import java.util.*;
/*
 * Given n pairs of parentheses, write a function to generate all combinations
 * of well-formed parentheses.
 * 
 * Example 1:
 * Input: n = 3
 * Output: ["((()))","(()())","(())()","()(())","()()()"]
 * 
 * Example 2:
 * Input: n = 1
 * Output: ["()"]
 */

// Solution 1: Basic Backtracking with String
// Time: O(4^n / √n) Space: O(4^n / √n)
class Solution1 {

    public List<String> generateParenthesis(int n) {
        List<String> result = new ArrayList<>();
        backtrack(result, "", 0, 0, n);
        return result;
    }

    private void backtrack(List<String> result, String current, int open, int close, int max) {
        // Base case: we've used all n pairs
        if (current.length() == max * 2) {
            result.add(current);
            return;
        }

        // Add opening parenthesis if we haven't used all n
        if (open < max) {
            backtrack(result, current + "(", open + 1, close, max);
        }

        // Add closing parenthesis if it won't make the string invalid
        if (close < open) {
            backtrack(result, current + ")", open, close + 1, max);
        }
    }

}

// Solution 2: Optimized with StringBuilder (Better Performance)
class Solution2 {

    public List<String> generateParenthesis(int n) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        backtrack(result, sb, 0, 0, n);
        return result;
    }

    private void backtrack(List<String> result, StringBuilder current, int open, int close, int max) {
        if (current.length() == max * 2) {
            result.add(current.toString());
            return;
        }

        if (open < max) {
            current.append('(');
            backtrack(result, current, open + 1, close, max);
            current.deleteCharAt(current.length() - 1); // backtrack
        }

        if (close < open) {
            current.append(')');
            backtrack(result, current, open, close + 1, max);
            current.deleteCharAt(current.length() - 1); // backtrack
        }
    }

}

// Solution 3: Dynamic Programming Approach
class Solution3 {

    public List<String> generateParenthesis(int n) {
        List<List<String>> dp = new ArrayList<>();

        // Base case
        dp.add(Arrays.asList(""));

        for (int i = 1; i <= n; i++) {
            List<String> current = new ArrayList<>();

            // For each way to split i pairs into left and right parts
            for (int j = 0; j < i; j++) {
                List<String> left = dp.get(j);
                List<String> right = dp.get(i - 1 - j);

                // Combine all possibilities
                for (String l : left) {
                    for (String r : right) {
                        current.add("(" + l + ")" + r);
                    }
                }
            }
            dp.add(current);
        }

        return dp.get(n);
    }

}

// Solution 4: Iterative BFS Approach
class Solution4 {

    public List<String> generateParenthesis(int n) {
        List<String> result = new ArrayList<>();
        if (n == 0)
            return result;

        Queue<Node> queue = new LinkedList<>();
        queue.offer(new Node("", 0, 0));

        while (!queue.isEmpty()) {
            Node node = queue.poll();

            if (node.str.length() == n * 2) {
                result.add(node.str);
                continue;
            }

            // Add opening parenthesis
            if (node.open < n) {
                queue.offer(new Node(node.str + "(", node.open + 1, node.close));
            }

            // Add closing parenthesis
            if (node.close < node.open) {
                queue.offer(new Node(node.str + ")", node.open, node.close + 1));
            }
        }

        return result;
    }

    class Node {
        String str;
        int open, close;

        Node(String s, int o, int c) {
            str = s;
            open = o;
            close = c;
        }
    }

}

// Solution 5: Memoized Recursive Approach
class Solution5 {

    private Map<String, List<String>> memo = new HashMap<>();

    public List<String> generateParenthesis(int n) {
        return helper(n, n);
    }

    private List<String> helper(int open, int close) {
        String key = open + "," + close;
        if (memo.containsKey(key)) {
            return memo.get(key);
        }

        List<String> result = new ArrayList<>();

        if (open == 0 && close == 0) {
            result.add("");
        } else {
            if (open > 0) {
                for (String s : helper(open - 1, close)) {
                    result.add("(" + s);
                }
            }
            if (close > open) {
                for (String s : helper(open, close - 1)) {
                    result.add(")" + s);
                }
            }
        }

        memo.put(key, result);
        return result;
    }

}

// Solution 6: Catalan Number Formula Based (Mathematical Approach)
class Solution6 {

    public List<String> generateParenthesis(int n) {
        List<String> result = new ArrayList<>();
        if (n == 0) {
            result.add("");
            return result;
        }

        // Using the recursive relation for Catalan numbers
        for (int i = 0; i < n; i++) {
            List<String> left = generateParenthesis(i);
            List<String> right = generateParenthesis(n - 1 - i);

            for (String l : left) {
                for (String r : right) {
                    result.add("(" + l + ")" + r);
                }
            }
        }

        return result;
    }

}

// Test class to demonstrate all solutions
class GenerateParenthesesTest {

    public static void main(String[] args) {
        Solution1 sol1 = new Solution1();
        Solution2 sol2 = new Solution2();
        Solution3 sol3 = new Solution3();
        Solution4 sol4 = new Solution4();
        Solution5 sol5 = new Solution5();
        Solution6 sol6 = new Solution6();

        int[] testCases = { 1, 2, 3 };

        for (int n : testCases) {
            System.out.println("n = " + n + ":");
            System.out.println("Solution 1: " + sol1.generateParenthesis(n));
            System.out.println("Solution 2: " + sol2.generateParenthesis(n));
            System.out.println("Solution 3: " + sol3.generateParenthesis(n));
            System.out.println("Solution 4: " + sol4.generateParenthesis(n));
            System.out.println("Solution 5: " + sol5.generateParenthesis(n));
            System.out.println("Solution 6: " + sol6.generateParenthesis(n));
            System.out.println();
        }

        // Performance comparison for larger n
        System.out.println("Performance test for n = 4:");
        long start = System.nanoTime();
        List<String> result = sol2.generateParenthesis(4);
        long end = System.nanoTime();
        System.out.println("Result size: " + result.size() + ", Time: " + (end - start) / 1000000.0 + " ms");
    }

}
