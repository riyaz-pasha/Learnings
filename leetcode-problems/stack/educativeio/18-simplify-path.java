/**
 * ============================================================================
 * SIMPLIFY PATH - COMPLETE INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given an absolute path for a Unix-style file system, transform it 
 * into its simplified canonical form. 
 * Rules:
 * - "." means current directory (ignore).
 * - ".." means parent directory (go up one level).
 * - Multiple slashes "//" are treated as a single slash "/".
 * - "..." or other period sequences are valid directory names.
 * - Output must start with "/", have no trailing "/", and be clean.
 * 
 * Simple Explanation for Interviewer:
 * "We are given a messy file path and need to clean it up. Moving through a 
 * file system is like moving in and out of folders. When we see a folder name, 
 * we enter it. When we see '..', we exit the current folder and go back to the 
 * previous one. This 'entering and exiting' behaves exactly like a Last-In-First-Out 
 * (LIFO) structure, which means a Stack is the ideal tool for the job."
 * 
 * Core Idea & Intuition:
 * By splitting the string by "/", we isolate the components of the path.
 * We can push valid directory names onto a stack. When we see "..", we pop 
 * the most recent directory from the stack (if it's not empty). We ignore "." 
 * and empty strings (caused by "//"). Finally, we join the stack back together 
 * with "/" to form the canonical path.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "What happens if we use '..' at the root directory?"
 *    Why: We need to know if going up from root throws an error or just stays at root.
 *    Impact: In Unix, `cd ..` at `/` keeps you at `/`. We must safely ignore pops 
 *    when the stack is empty.
 * 
 * 2. Q: "Can the path contain spaces or special characters?"
 *    Why: Helps determine if we need special parsing logic.
 *    Impact: Prompt guarantees only letters, digits, '.', '/', and '_'.
 * 
 * 3. Q: "Are directory names with three or more dots like '...' valid?"
 *    Why: Edge case verification.
 *    Impact: Yes, '...' is a valid folder name. We must explicitly check for 
 *    strictly "." and ".." rather than just counting dots.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example 1:
 * Input: "/home//foo/" -> Output: "/home/foo"
 * 
 * Example 2:
 * Input: "/a/./b/../../c/" -> Output: "/c"
 * 
 * Visualizing the Stack approach for "/a/./b/../../c/":
 * 
 * Token | Action                  | Stack State (Bottom -> Top)
 * -----------------------------------------------------------------------
 * "a"   | Valid dir -> Push       | ["a"]
 * "."   | Current dir -> Ignore   | ["a"]
 * "b"   | Valid dir -> Push       | ["a", "b"]
 * ".."  | Parent dir -> POP       | ["a"]
 * ".."  | Parent dir -> POP       | []
 * "c"   | Valid dir -> Push       | ["c"]
 * 
 * Finally, join the stack with "/": return "/" + "c" => "/c".
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Confirm Unix rules (root bounds, dots, slashes).
 * 2. Clarify: Ask about '..' at root and '...' as a valid name.
 * 3. Approach 1 (Split + Stack): "I will split the string by '/'. I'll use a 
 *    Deque to store directory names. If I see '..', I pop. If I see '.' or empty, 
 *    I do nothing. Otherwise, I push."
 * 4. Approach 2 (Custom Parser): If asked for absolute peak efficiency without 
 *    `split()` overhead, mention iterating character by character.
 * 5. Code: Write the Split + Stack solution (it's the industry standard for this).
 * 6. Dry-run: Trace Example 2 out loud, showing stack state changes.
 * 7. Complexity: O(N) Time and O(N) Space.
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - Tons of slashes: `/////home///////foo` -> Split handles this by producing `""`.
 * - Root bound limits: `/../` -> Should just return `/`.
 * - `...` as a directory name: `/.../` -> Should return `/...`.
 * - Only slashes: `/` or `///` -> Should return `/`.
 * 
 * Common Mistakes:
 * - Using `java.util.Stack`. It is legacy, synchronized, and iterators traverse 
 *   it from Bottom-to-Top (which is actually what we want here, but `ArrayDeque` 
 *   is much faster and safer to use).
 * - Building the final string using `+` in a loop. Strings are immutable; use 
 *   `StringBuilder` or `String.join`.
 * - Using `dir.contains("..")` instead of `dir.equals("..")`. This breaks on `...`.
 */

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;

public class SimplifyPath {

    public static void main(String[] args) {
        String path1 = "/home/";
        String path2 = "/../";
        String path3 = "/home//foo/";
        String path4 = "/a/./b/../../c/";
        String path5 = "/a/../../b/../c//.//";

        System.out.println("--- Standard Deque Approach ---");
        System.out.println(path1 + " -> " + simplifyPathDeque(path1)); // "/home"
        System.out.println(path2 + " -> " + simplifyPathDeque(path2)); // "/"
        System.out.println(path3 + " -> " + simplifyPathDeque(path3)); // "/home/foo"
        System.out.println(path4 + " -> " + simplifyPathDeque(path4)); // "/c"
        System.out.println(path5 + " -> " + simplifyPathDeque(path5)); // "/c"

        System.out.println("\n--- Ultra-Optimal Two-Pointer Parser ---");
        System.out.println(path4 + " -> " + simplifyPathOptimal(path4)); // "/c"
    }

    /**
     * SOLUTION 1: STANDARD DEQUE (Split & Stack) - OPTIMAL FOR INTERVIEWS
     * ------------------------------------------------------------------------
     * Idea: Split the path by '/'. Iterate through the resulting tokens.
     * - Ignore empty tokens (from "//") and "." (current directory).
     * - If token is "..", pop from the Deque if it's not empty.
     * - Otherwise, it's a valid directory name, push to the Deque.
     * 
     * Time Complexity: O(N), where N is the length of the string. 
     * `split()` takes O(N), and we iterate over the components in O(N).
     * Space Complexity: O(N) to store the components in the Deque and the 
     * array created by `split()`.
     */
    public static String simplifyPathDeque(String path) {
        if (path == null || path.isEmpty()) return "/";

        // ArrayDeque is the modern, non-synchronized Stack/Queue in Java.
        Deque<String> stack = new ArrayDeque<>();
        
        // Splitting by "/" handles multiple consecutive slashes by emitting empty strings
        String[] components = path.split("/");

        for (String dir : components) {
            // Ignore "." and empty strings (caused by consecutive slashes)
            if (dir.isEmpty() || dir.equals(".")) {
                continue;
            } 
            // ".." means go up one directory (pop the stack)
            else if (dir.equals("..")) {
                if (!stack.isEmpty()) {
                    stack.removeLast(); // removes the most recently added element
                }
            } 
            // Valid directory name (including "..." or hidden files like ".m2")
            else {
                stack.addLast(dir);
            }
        }

        // If the stack is empty after parsing (e.g., "/../"), return root
        if (stack.isEmpty()) {
            return "/";
        }

        // Reconstruct the canonical path.
        // String.join iterators over Deque from first added to last added (Bottom -> Top).
        return "/" + String.join("/", stack);
    }

    /**
     * SOLUTION 2: ULTRA-OPTIMAL (No Split Overhead)
     * ------------------------------------------------------------------------
     * Idea: `String.split()` creates many intermediate String objects and an 
     * array. In highly memory-constrained environments, we can manually parse 
     * the string using two pointers to extract directory names and store them 
     * in an ArrayList (simulating a stack).
     * 
     * Time Complexity: O(N) - One pass.
     * Space Complexity: O(N) - Only stores the final valid components.
     * 
     * Interview Note: Usually overkill, but brilliant to mention as an optimization 
     * if the interviewer asks: "How can we do this without creating the large array 
     * that split() returns?"
     */
    public static String simplifyPathOptimal(String path) {
        if (path == null || path.isEmpty()) return "/";
        
        List<String> stack = new ArrayList<>();
        int n = path.length();
        int i = 0;
        
        while (i < n) {
            // Skip consecutive slashes
            while (i < n && path.charAt(i) == '/') {
                i++;
            }
            if (i == n) break;
            
            // Extract the directory name
            int start = i;
            while (i < n && path.charAt(i) != '/') {
                i++;
            }
            String dir = path.substring(start, i);
            
            // Process the directory name
            if (dir.equals("..")) {
                if (!stack.isEmpty()) {
                    stack.remove(stack.size() - 1);
                }
            } else if (!dir.equals(".")) {
                stack.add(dir);
            }
        }
        
        if (stack.isEmpty()) {
            return "/";
        }
        
        StringBuilder res = new StringBuilder();
        for (String dir : stack) {
            res.append('/').append(dir);
        }
        
        return res.toString();
    }

    /**
     * ========================================================================
     * 6. SOLUTION COMPARISON
     * ========================================================================
     * Approach       | Time | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * Split + Deque  | O(N) | O(N)  | Very readable, idiomatic.  | ⭐ STRONGLY REC.
     * Manual Parse   | O(N) | O(N)  | Leaner memory footprint.   | Good follow-up.
     * 
     * ========================================================================
     * 7. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if the input could be a relative path (doesn't start with '/')?"
     * A1: We would need a way to know the "current working directory" (CWD). 
     *     We would initialize our stack by first parsing the CWD, and then parse 
     *     the relative path on top of it. If it starts with '/', we clear the 
     *     CWD and start fresh from root.
     * 
     * Q2: "Can we do this in O(1) space?"
     * A2: In Java, strings are immutable, so returning a modified string inherently 
     *     takes O(N) space. In languages with mutable strings like C/C++, we can 
     *     modify the string in-place using two pointers (a read pointer and a 
     *     write pointer), achieving O(1) auxiliary space. The write pointer acts 
     *     as the top of our stack.
     * 
     * Q3: "Why use `String.join` instead of a `StringBuilder` loop?"
     * A3: `String.join` internally uses a `StringJoiner` which is highly optimized 
     *     in modern Java for exactly this use case. It correctly sizes the underlying 
     *     buffer and handles the separators elegantly without needing trailing 
     *     character checks.
     * 
     * ========================================================================
     * 8. FINAL TAKEAWAYS
     * ========================================================================
     * - Key Pattern: Navigating a hierarchy where you can "go back" (like file 
     *   paths, browser history, or bracket matching) always maps to a Stack.
     * - Java Tip: `path.split("/")` handles multiple delimiters by inserting 
     *   empty strings `""`. You must check for `.isEmpty()` when processing the array.
     * - Data Structure Choice: `ArrayDeque` is the standard for Stacks in Java. 
     *   Never use the legacy `java.util.Stack`.
     */
}
