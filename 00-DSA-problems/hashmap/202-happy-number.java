import java.util.*;
/*
 * Write an algorithm to determine if a number n is happy.
 * 
 * A happy number is a number defined by the following process:
 * 
 * Starting with any positive integer, replace the number by the sum of the
 * squares of its digits.
 * Repeat the process until the number equals 1 (where it will stay), or it
 * loops endlessly in a cycle which does not include 1.
 * Those numbers for which this process ends in 1 are happy.
 * Return true if n is a happy number, and false if not.
 * 
 * Example 1:
 * Input: n = 19
 * Output: true
 * Explanation:
 * 1^2 + 9^2 = 82
 * 8^2 + 2^2 = 68
 * 6^2 + 8^2 = 100
 * 1^2 + 0^2 + 0^2 = 1
 * 
 * Example 2:
 * Input: n = 2
 * Output: false
 */

class HappyNumber {

    // Helper method to calculate sum of squares of digits
    private int getSumOfSquares(int n) {
        int sum = 0;
        while (n > 0) {
            int digit = n % 10;
            sum += digit * digit;
            n /= 10;
        }
        return sum;
    }

    // Solution 1: Using HashSet to detect cycles
    // Time Complexity: O(log n), Space Complexity: O(log n)
    public boolean isHappyHashSet(int n) {
        Set<Integer> seen = new HashSet<>();

        while (n != 1 && !seen.contains(n)) {
            seen.add(n);
            n = getSumOfSquares(n);
        }

        return n == 1;
    }

    // Solution 2: Floyd's Cycle Detection (Two Pointers)
    // Time Complexity: O(log n), Space Complexity: O(1)
    public boolean isHappy(int n) {
        int slow = n;
        int fast = n;

        do {
            slow = getSumOfSquares(slow); // Move one step
            fast = getSumOfSquares(getSumOfSquares(fast)); // Move two steps
        } while (slow != fast);

        return slow == 1;
    }

    // Solution 3: Using mathematical insight (cycles always contain 4)
    // Time Complexity: O(log n), Space Complexity: O(1)
    public boolean isHappyMath(int n) {
        while (n != 1 && n != 4) {
            n = getSumOfSquares(n);
        }
        return n == 1;
    }

    // Solution 4: Recursive approach with memoization
    // Time Complexity: O(log n), Space Complexity: O(log n)
    private Set<Integer> memo = new HashSet<>();

    public boolean isHappyRecursive(int n) {
        if (n == 1)
            return true;
        if (memo.contains(n))
            return false;

        memo.add(n);
        return isHappyRecursive(getSumOfSquares(n));
    }

    // Alternative helper method using string manipulation (less efficient)
    private int getSumOfSquaresString(int n) {
        String str = String.valueOf(n);
        int sum = 0;
        for (char c : str.toCharArray()) {
            int digit = c - '0';
            sum += digit * digit;
        }
        return sum;
    }

    // Detailed step-by-step version for understanding
    public boolean isHappyDetailed(int n) {
        Set<Integer> seen = new HashSet<>();
        System.out.println("Checking if " + n + " is happy:");

        while (n != 1 && !seen.contains(n)) {
            seen.add(n);
            int newN = getSumOfSquares(n);
            System.out.println(n + " -> " + newN + " (sum of squares of digits)");
            n = newN;
        }

        if (n == 1) {
            System.out.println("Result: HAPPY! 🎉");
            return true;
        } else {
            System.out.println("Result: Not happy (cycle detected) 😞");
            return false;
        }
    }

    // Test method
    public static void main(String[] args) {
        HappyNumber solution = new HappyNumber();

        // Test cases
        int[] testCases = { 19, 2, 7, 10, 1, 23, 82, 68, 100 };

        System.out.println("=== Testing Happy Numbers ===\n");

        for (int num : testCases) {
            boolean result = solution.isHappy(num);
            System.out.printf("n = %d: %s\n", num, result ? "Happy ✓" : "Not Happy ✗");
        }

        System.out.println("\n=== Detailed Example for n = 19 ===");
        solution = new HappyNumber(); // Reset memo for recursive version
        solution.isHappyDetailed(19);

        System.out.println("\n=== Detailed Example for n = 2 ===");
        solution.isHappyDetailed(2);

        System.out.println("\n=== Performance Comparison ===");
        int testNum = 19;
        long startTime, endTime;

        // HashSet approach
        startTime = System.nanoTime();
        boolean result1 = solution.isHappyHashSet(testNum);
        endTime = System.nanoTime();
        System.out.printf("HashSet approach: %s (Time: %d ns)\n", result1, endTime - startTime);

        // Floyd's cycle detection
        startTime = System.nanoTime();
        boolean result2 = solution.isHappy(testNum);
        endTime = System.nanoTime();
        System.out.printf("Floyd's cycle: %s (Time: %d ns)\n", result2, endTime - startTime);

        // Mathematical approach
        startTime = System.nanoTime();
        boolean result3 = solution.isHappyMath(testNum);
        endTime = System.nanoTime();
        System.out.printf("Math approach: %s (Time: %d ns)\n", result3, endTime - startTime);

        // Known happy numbers up to 100
        System.out.println("\n=== Happy Numbers from 1 to 100 ===");
        List<Integer> happyNumbers = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            if (solution.isHappy(i)) {
                happyNumbers.add(i);
            }
        }
        System.out.println("Happy numbers: " + happyNumbers);
    }

}
