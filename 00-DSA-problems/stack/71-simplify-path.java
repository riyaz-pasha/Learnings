import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Stack;
/*
 * You are given an absolute path for a Unix-style file system, which always
 * begins with a slash '/'. Your task is to transform this absolute path into
 * its simplified canonical path.
 * 
 * The rules of a Unix-style file system are as follows:
 * 
 * A single period '.' represents the current directory.
 * A double period '..' represents the previous/parent directory.
 * Multiple consecutive slashes such as '//' and '///' are treated as a single
 * slash '/'.
 * Any sequence of periods that does not match the rules above should be treated
 * as a valid directory or file name. For example, '...' and '....' are valid
 * directory or file names.
 * The simplified canonical path should follow these rules:
 * 
 * The path must start with a single slash '/'.
 * Directories within the path must be separated by exactly one slash '/'.
 * The path must not end with a slash '/', unless it is the root directory.
 * The path must not have any single or double periods ('.' and '..') used to
 * denote current or parent directories.
 * Return the simplified canonical path.
 * 
 * 
 * Example 1:
 * Input: path = "/home/"
 * Output: "/home"
 * Explanation:
 * The trailing slash should be removed.
 * 
 * Example 2:
 * Input: path = "/home//foo/"
 * Output: "/home/foo"
 * Explanation:
 * Multiple consecutive slashes are replaced by a single one.
 * 
 * Example 3:
 * Input: path = "/home/user/Documents/../Pictures"
 * Output: "/home/user/Pictures"
 * Explanation:
 * A double period ".." refers to the directory up a level (the parent
 * directory).
 * 
 * Example 4:
 * Input: path = "/../"
 * Output: "/"
 * Explanation:
 * Going one level up from the root directory is not possible.
 * 
 * Example 5:
 * Input: path = "/.../a/../b/c/../d/./"
 * Output: "/.../b/d"
 * 
 * Explanation:
 * "..." is a valid name for a directory in this problem.
 * 
 */

class Solution {

    /**
     * Approach 1: Stack-based Solution (Most Intuitive)
     * Time Complexity: O(n) where n is the length of the path
     * Space Complexity: O(n) for the stack and result
     */
    public String simplifyPath(String path) {
        if (path == null || path.length() == 0) {
            return "/";
        }

        Stack<String> stack = new Stack<>();
        String[] components = path.split("/");

        for (String component : components) {
            if (component.equals("") || component.equals(".")) {
                // Skip empty components (from multiple slashes) and current directory
                continue;
            } else if (component.equals("..")) {
                // Go to parent directory - pop from stack if not empty
                if (!stack.isEmpty()) {
                    stack.pop();
                }
            } else {
                // Valid directory name - push to stack
                stack.push(component);
            }
        }

        // Build the result path
        if (stack.isEmpty()) {
            return "/";
        }

        StringBuilder result = new StringBuilder();
        for (String dir : stack) {
            result.append("/").append(dir);
        }

        return result.toString();
    }

    /**
     * Approach 2: Deque-based Solution (More Efficient)
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public String simplifyPath2(String path) {
        if (path == null || path.length() == 0) {
            return "/";
        }

        Deque<String> deque = new ArrayDeque<>();
        String[] components = path.split("/");

        for (String component : components) {
            if (component.equals("") || component.equals(".")) {
            } else if (component.equals("..")) {
                if (!deque.isEmpty()) {
                    deque.pollLast();
                }
            } else {
                deque.offerLast(component);
            }
        }

        if (deque.isEmpty()) {
            return "/";
        }

        StringBuilder result = new StringBuilder();
        while (!deque.isEmpty()) {
            result.append("/").append(deque.pollFirst());
        }

        return result.toString();
    }

    /**
     * Approach 3: Manual Parsing (No Split)
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public String simplifyPath3(String path) {
        if (path == null || path.length() == 0) {
            return "/";
        }

        Stack<String> stack = new Stack<>();
        int i = 0;
        int n = path.length();

        while (i < n) {
            // Skip leading slashes
            while (i < n && path.charAt(i) == '/') {
                i++;
            }

            if (i >= n)
                break;

            // Extract component
            int start = i;
            while (i < n && path.charAt(i) != '/') {
                i++;
            }

            String component = path.substring(start, i);

            if (component.equals(".")) {
                // Current directory - do nothing
                continue;
            } else if (component.equals("..")) {
                // Parent directory - pop if possible
                if (!stack.isEmpty()) {
                    stack.pop();
                }
            } else {
                // Valid directory name
                stack.push(component);
            }
        }

        if (stack.isEmpty()) {
            return "/";
        }

        StringBuilder result = new StringBuilder();
        for (String dir : stack) {
            result.append("/").append(dir);
        }

        return result.toString();
    }

    /**
     * Approach 4: ArrayList-based Solution
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public String simplifyPath4(String path) {
        if (path == null || path.length() == 0) {
            return "/";
        }

        List<String> pathList = new ArrayList<>();
        String[] components = path.split("/");

        for (String component : components) {
            if (component.equals("") || component.equals(".")) {
                continue;
            } else if (component.equals("..")) {
                if (!pathList.isEmpty()) {
                    pathList.remove(pathList.size() - 1);
                }
            } else {
                pathList.add(component);
            }
        }

        if (pathList.isEmpty()) {
            return "/";
        }

        return "/" + String.join("/", pathList);
    }

    // Test cases
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1
        System.out.println("Test 1: " + solution.simplifyPath("/home/"));
        // Expected: "/home"

        // Test case 2
        System.out.println("Test 2: " + solution.simplifyPath("/home//foo/"));
        // Expected: "/home/foo"

        // Test case 3
        System.out.println("Test 3: " + solution.simplifyPath("/home/user/Documents/../Pictures"));
        // Expected: "/home/user/Pictures"

        // Test case 4
        System.out.println("Test 4: " + solution.simplifyPath("/../"));
        // Expected: "/"

        // Test case 5
        System.out.println("Test 5: " + solution.simplifyPath("/.../a/../b/c/../d/./"));
        // Expected: "/.../b/d"

        // Additional test cases
        System.out.println("Test 6: " + solution.simplifyPath("/"));
        // Expected: "/"

        System.out.println("Test 7: " + solution.simplifyPath("/a/./b/../../c/"));
        // Expected: "/c"

        System.out.println("Test 8: " + solution.simplifyPath("/a/../../b/../c//.//"));
        // Expected: "/c"

        System.out.println("Test 9: " + solution.simplifyPath("/a//b////c/d//././/.."));
        // Expected: "/a/b/c"

        System.out.println("Test 10: " + solution.simplifyPath("/..hidden"));
        // Expected: "/..hidden"
    }

}

/**
 * EXPLANATION:
 * 
 * The problem requires us to simplify a Unix-style file path by:
 * 1. Handling current directory references (".")
 * 2. Handling parent directory references ("..")
 * 3. Removing multiple consecutive slashes
 * 4. Removing trailing slashes (except for root)
 * 5. Treating other sequences of periods as valid directory names
 * 
 * Key Insights:
 * 1. Use a stack to keep track of valid directory names
 * 2. Split the path by "/" to get individual components
 * 3. Process each component:
 * - Skip empty strings (from multiple slashes) and "."
 * - For "..", pop from stack if not empty (go to parent)
 * - For other components, push to stack (valid directory)
 * 4. Build final path from stack contents
 * 
 * Algorithm Steps:
 * 1. Split path by "/" to get components
 * 2. Process each component using stack:
 * - Empty or "." → skip
 * - ".." → pop from stack (if not empty)
 * - Other → push to stack
 * 3. Build result by joining stack elements with "/"
 * 4. Handle edge case of empty stack (return "/")
 * 
 * Edge Cases:
 * - Root directory only: "/" → "/"
 * - Going above root: "/../" → "/"
 * - Multiple dots: "/..." → "/..." (valid directory name)
 * - Complex combinations: "/a/./b/../../c/" → "/c"
 * 
 * Time Complexity: O(n) - single pass through the path
 * Space Complexity: O(n) - stack can hold up to n/2 directory names
 */
