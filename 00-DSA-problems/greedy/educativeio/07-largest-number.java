import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * INTERVIEW GUIDE: LARGEST NUMBER
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "Can the array contain negative numbers?" 
 *      (Assumption: No, the prompt specifies non-negative integers.)
 *    - "What if the array is full of zeros, like [0, 0]?" 
 *      (Crucial Edge Case: The result should be '0', not '00'.)
 *    - "Can the resulting number exceed standard integer limits?"
 *      (Assumption: Yes, returning as a String handles arbitrarily large numbers.)
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Concatenate the numbers to form the largest possible string value.
 *    - Observation 1 (Why standard sorting fails): We might think sorting descending 
 *      alphabetically works. But consider 3 and 30. Alphabetically, "30" > "3". 
 *      If we put "30" first, we get "303". If we put "3" first, we get "330". 
 *      Clearly, 330 > 303. Standard sorting doesn't capture concatenation logic.
 *    - Observation 2 (The Custom Comparator): To decide whether number 'A' or 
 *      number 'B' should come first, simply simulate both concatenations: 
 *      compare (A + B) with (B + A). 
 *      If (A + B) is larger, 'A' goes first. If (B + A) is larger, 'B' goes first.
 *    - Observation 3 (Transitivity): This custom sorting relation is transitive, 
 *      meaning if A > B and B > C, then A > C. This guarantees our greedy sort 
 *      yields the globally optimal arrangement.
 * 
 * 3. VISUAL EXPLANATION:
 *    Array: [3, 30, 34, 5, 9]
 *    
 *    Let's compare elements to sort them:
 *    - Compare "3" and "30": 
 *      "3" + "30" = "330" 
 *      "30" + "3" = "303"
 *      Since "330" > "303", "3" must come before "30".
 * 
 *    - Compare "34" and "3":
 *      "34" + "3" = "343"
 *      "3" + "34" = "334"
 *      Since "343" > "334", "34" must come before "3".
 * 
 *    Sorted String Array based on rules: ["9", "5", "34", "3", "30"]
 *    Concatenated Result: "9534330"
 * 
 * ============================================================================
 */
class LargestNumber {

    /**
     * APPROACH 1: Standard Sorting with Custom Comparator (Optimal)
     * 
     * Time Complexity: O(N log N * K) where N is the number of elements and K is 
     * the maximum number of digits in an element (due to string comparison).
     * Space Complexity: O(N * K) to store the elements as strings.
     */
    public String largestNumberStandard(int[] nums) {
        // Step 1: Convert integers to strings
        String[] strNums = new String[nums.length];
        for (int i = 0; i < nums.length; i++) {
            strNums[i] = String.valueOf(nums[i]);
        }
        
        // Step 2: Sort strings using our custom concatenation logic
        // b.compareTo(a) handles the descending order for us.
        Arrays.sort(strNums, (a, b) -> (b + a).compareTo(a + b));
        
        // Step 3: Handle the edge case where the largest number is "0" (e.g., [0, 0])
        if (strNums[0].equals("0")) {
            return "0";
        }
        
        // Step 4: Build the final result
        StringBuilder sb = new StringBuilder();
        for (String s : strNums) {
            sb.append(s);
        }
        
        return sb.toString();
    }

    /**
     * APPROACH 2: Modern Java Streams (Expressive & Concise)
     * 
     * This approach uses the exact same underlying logic but leverages Java 8+ 
     * Streams to do it in effectively one statement. This highlights strong 
     * API knowledge during an interview.
     * 
     * Time Complexity: O(N log N * K)
     * Space Complexity: O(N * K)
     */
    public String largestNumberStreams(int[] nums) {
        String result = Arrays.stream(nums)
                              .mapToObj(String::valueOf)
                              .sorted((a, b) -> (b + a).compareTo(a + b))
                              .collect(Collectors.joining(""));
                              
        // Handle the all-zeros edge case concisely
        return result.startsWith("0") ? "0" : result;
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Records (introduced in Java 14) provide a concise way to create immutable data carriers.
     */
    record TestCase(int[] nums, String expected) {}

    public static void main(String[] args) {
        LargestNumber solver = new LargestNumber();
        
        // Defining test cases using our Record
        var testCases = List.of(
            new TestCase(new int[]{10, 2}, "210"),
            new TestCase(new int[]{3, 30, 34, 5, 9}, "9534330"),
            new TestCase(new int[]{0, 0}, "0"),          // Edge case: multiple zeros
            new TestCase(new int[]{1}, "1"),             // Edge case: single element
            new TestCase(new int[]{34323, 3432}, "343234323") 
        );
        
        System.out.println("--- Running Approach 1 (Standard Optimal) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            String result = solver.largestNumberStandard(tc.nums());
            System.out.printf("Test %d: Expected = %s, Got = %s -> %s%n", 
                i + 1, tc.expected(), result, (result.equals(tc.expected()) ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (Modern Streams) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            String result = solver.largestNumberStreams(tc.nums());
            System.out.printf("Test %d: Expected = %s, Got = %s -> %s%n", 
                i + 1, tc.expected(), result, (result.equals(tc.expected()) ? "PASS" : "FAIL"));
        }
    }
}

import java.util.*;

/**
 * ================================================================
 * 🔥 Largest Number — GREEDY + CUSTOM SORT
 * ================================================================
 *
 * Core Idea:
 * Instead of comparing numbers directly, compare concatenations.
 *
 * Example:
 * 3 and 30
 * "330" > "303" → 3 should come before 30
 *
 * ================================================================
 */

class LargestNumber {

    public static void main(String[] args) {
        int[] nums = {3, 30, 34, 5, 9};

        System.out.println(largestNumber(nums)); // Expected: "9534330"
    }

    public static String largestNumber(int[] nums) {

        // 🔹 Step 1: Convert int → String
        String[] arr = Arrays.stream(nums)
                .mapToObj(String::valueOf)
                .toArray(String[]::new);

        // 🔹 Step 2: Custom sort based on concatenation
        Arrays.sort(arr, (a, b) -> (b + a).compareTo(a + b));

        /*
         * Why (b+a)?
         * We want descending order:
         * if (b+a) > (a+b) → b comes before a
         */

        // 🔹 Step 3: Edge case (all zeros)
        if (arr[0].equals("0")) return "0";

        // 🔹 Step 4: Build result
        StringBuilder result = new StringBuilder();
        for (String s : arr) {
            result.append(s);
        }

        return result.toString();
    }

    public static String largestNumberFunctional(int[] nums) {
        return Arrays.stream(nums)
                .mapToObj(String::valueOf)
                .sorted((a, b) -> (b + a).compareTo(a + b))
                .reduce("", String::concat)
                .replaceFirst("^0+$", "0"); // handle all zeros
    }
}

/*
 * ================================================================
 * 🔥 CUSTOM COMPARATOR — FULL INTUITION + PROOF
 * ================================================================
 *
 * Problem:
 * Arrange numbers such that their concatenation forms the LARGEST number.
 *
 * Key Challenge:
 * Normal sorting (numeric or lexicographic) DOES NOT WORK.
 *
 * Example:
 * nums = [3, 30]
 * Numeric sort → [30, 3] → "303" ❌
 * Correct answer → "330"
 *
 * ------------------------------------------------
 * 💡 CORE IDEA (Greedy Decision)
 * ------------------------------------------------
 *
 * For ANY two numbers a and b:
 *
 * We must decide:
 *    Should "a" come before "b" OR "b" before "a"?
 *
 * Instead of comparing a and b directly,
 * we compare the TWO possible concatenations:
 *
 *    option1 = a + b
 *    option2 = b + a
 *
 * Example:
 * a = "3", b = "30"
 *
 * a + b = "330"
 * b + a = "303"
 *
 * Since "330" > "303",
 * → placing "3" before "30" gives a larger result.
 *
 * ------------------------------------------------
 * ⚙️ HOW COMPARATOR WORKS
 * ------------------------------------------------
 *
 * Arrays.sort expects:
 *   negative → a comes before b
 *   positive → b comes before a
 *
 * We want DESCENDING order based on concatenation.
 *
 * So we write:
 *
 *    (b + a).compareTo(a + b)
 *
 * Why reversed?
 * Because compareTo gives ASCENDING by default.
 *
 * If (b+a) > (a+b):
 *     → comparator returns positive
 *     → b comes before a ✅ (correct for max number)
 *
 * ------------------------------------------------
 * 🧠 INTUITION SIMPLIFIED
 * ------------------------------------------------
 *
 * Think:
 * "Which order gives BIGGER combined number?"
 *
 * Place that order first.
 *
 * That's it.
 *
 * ------------------------------------------------
 * 🔍 PROOF OF CORRECTNESS (WHY GREEDY WORKS)
 * ------------------------------------------------
 *
 * Claim:
 * Sorting with this comparator produces the globally largest number.
 *
 * Reasoning (Exchange Argument):
 *
 * Assume we have an optimal arrangement.
 * If there exists any adjacent pair (a, b) such that:
 *
 *      a + b < b + a
 *
 * Then swapping them increases the total number.
 *
 * → This means the arrangement was NOT optimal.
 *
 * Therefore, in the optimal solution:
 * For every adjacent pair:
 *
 *      a + b >= b + a
 *
 * This is EXACTLY what our comparator enforces.
 *
 * Hence, sorting by this rule ensures:
 * ✔ No improving swap exists
 * ✔ Global optimum is reached
 *
 * ------------------------------------------------
 * 🔁 TRANSITIVITY (Why sorting is valid)
 * ------------------------------------------------
 *
 * This comparator forms a valid ordering because:
 *
 * If a should come before b
 * and b should come before c
 * → it will also correctly order a and c
 *
 * (Non-trivial, but proven for this problem)
 *
 * So sorting is SAFE.
 *
 * ------------------------------------------------
 * ⚠️ EDGE CASE
 * ------------------------------------------------
 *
 * nums = [0, 0, 0]
 * After sorting → ["0", "0", "0"]
 *
 * We must return "0", not "000"
 *
 * ------------------------------------------------
 * ⏱ COMPLEXITY
 * ------------------------------------------------
 *
 * Sorting: O(n log n)
 * Each comparison: O(k) (string length)
 *
 * Total: O(n log n * k)
 *
 * ================================================================
 */

