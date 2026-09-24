/**
 * ============================================================================
 * DAILY TEMPERATURES - COMPLETE INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given an array of integers representing daily temperatures, return 
 * an array where each element indicates the number of days you must wait to 
 * experience a warmer temperature. If no future day is warmer, set it to 0.
 * 
 * Simple Explanation for Interviewer:
 * "For every day in the array, I need to look ahead and find the very next day 
 * that is hotter than today. I then record the difference in days (indices) 
 * between that future hotter day and today. If winter is coming and it never 
 * gets hotter, I record a 0."
 * 
 * Core Idea & Intuition:
 * This is the classic "Next Greater Element" problem. 
 * If we scan the array and see a decreasing sequence of temperatures (e.g., 75, 71, 69), 
 * we don't know the answer for ANY of them yet. We are "waiting" for a hotter day.
 * When a hot day finally arrives (e.g., 72), it resolves the wait for 69 and 71, 
 * but not 75. 
 * Because the most recently seen cold day (69) is the first one to be resolved 
 * by the incoming hot day, this strictly follows a Last-In-First-Out (LIFO) 
 * pattern. Thus, a Stack is the perfect data structure.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "What is the maximum size of the array?"
 *    Why: If N is 10^3 (as in these constraints), an O(N^2) brute force *might* 
 *    pass, but usually, in interviews, we assume N can be up to 10^5, requiring O(N).
 * 2. Q: "Are the temperatures guaranteed to be positive?"
 *    Why: Negative numbers might affect arrays used for counting sort/bucket 
 *    optimizations, though it doesn't affect a Stack approach.
 * 3. Q: "What should I put if it's the last day?"
 *    Why: Clarifies edge behavior. (Answer: 0, since there are no future days).
 * 4. Q: "Do we count strictly warmer, or is the same temperature okay?"
 *    Why: Differentiates between `>` and `>=`. The problem states "warmer", so 
 *    we strictly look for `>`.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example 1:
 * Input:  [73, 74, 75, 71, 69, 72, 76, 73]
 * Output: [1,  1,  4,  2,  1,  1,  0,  0 ]
 * 
 * Visualizing the "Monotonic Decreasing Stack" (Stores INDICES):
 * 
 * i | Temp | Stack (Top->Bottom) | Action / Output Update
 * -------------------------------------------------------------------------
 * 0 |  73  | [0]                 | Push index 0.
 * 1 |  74  | [1]                 | 74 > 73. Pop 0. out[0] = 1-0=1. Push 1.
 * 2 |  75  | [2]                 | 75 > 74. Pop 1. out[1] = 2-1=1. Push 2.
 * 3 |  71  | [3, 2]              | 71 < 75. Push 3.
 * 4 |  69  | [4, 3, 2]           | 69 < 71. Push 4.
 * 5 |  72  | [5, 2]              | 72 > 69. Pop 4. out[4] = 5-4=1.
 *   |      |                     | 72 > 71. Pop 3. out[3] = 5-3=2.
 *   |      |                     | 72 < 75. Push 5.
 * 6 |  76  | [6]                 | 76 > 72. Pop 5. out[5] = 6-5=1.
 *   |      |                     | 76 > 75. Pop 2. out[2] = 6-2=4. Push 6.
 * 7 |  73  | [7, 6]              | 73 < 76. Push 7.
 * 
 * End of array. Stack has [7, 6]. Their output remains 0 (default).
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Confirm the "Next Greater Element" mapping.
 * 2. Clarify: Ask if O(N^2) is acceptable (it rarely is).
 * 3. Brute Force: Explain the double for-loop approach just to set a baseline.
 * 4. Optimal Approach (Stack): Explain how we only care about unresolved past days.
 *    Propose a stack that stores indices of these unresolved days.
 * 5. Explain the "Monotonic" property: Mention that the temperatures stored in 
 *    the stack will strictly be in decreasing order, hence "Monotonic Decreasing".
 * 6. Code: Write the Stack solution.
 * 7. Dry-run: Trace the `[75, 71, 69, 72]` portion to show the cascading pops.
 * 8. Bonus: If asked to optimize space, present the "Right-to-Left Jump" approach.
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - Strictly decreasing array: [100, 90, 80, 70] -> Stack fills up, nothing pops. Output all 0s.
 * - Flat array: [70, 70, 70] -> Strictly warmer (`>`) is required, so output all 0s.
 * - Single element: [50] -> Output [0].
 * 
 * Common Mistakes:
 * - Storing values in the stack instead of indices. If you store values, you 
 *   don't know *where* to write the distance in the output array!
 * - Doing `stack.pop()` without a `while` loop. An incoming hot day might 
 *   resolve MULTIPLE previous cold days, so a loop is required.
 * - Forgetting to initialize the output array (Java handles this by defaulting 
 *   `int[]` to 0, which perfectly matches our edge cases, but in C++ you must initialize it).
 */

import java.util.*;

public class DailyTemperatures {

    public static void main(String[] args) {
        int[] temps1 = {73, 74, 75, 71, 69, 72, 76, 73};
        int[] temps2 = {30, 40, 50, 60};
        int[] temps3 = {90, 80, 70, 60};

        System.out.println("--- Brute Force ---");
        System.out.println(Arrays.toString(dailyTemperaturesBruteForce(temps1)));

        System.out.println("\n--- Optimal Stack Approach ---");
        System.out.println(Arrays.toString(dailyTemperaturesStack(temps1)));
        System.out.println(Arrays.toString(dailyTemperaturesStack(temps2)));
        System.out.println(Arrays.toString(dailyTemperaturesStack(temps3)));

        System.out.println("\n--- Ultra-Optimal (O(1) Space) Approach ---");
        System.out.println(Arrays.toString(dailyTemperaturesO1Space(temps1)));
    }

    /**
     * SOLUTION 1: BRUTE FORCE
     * ------------------------------------------------------------------------
     * Idea: For every day, scan all future days until a warmer one is found.
     * 
     * Time Complexity: O(N^2) in the worst case (a strictly decreasing array).
     * Space Complexity: O(1) auxiliary space (excluding output array).
     * 
     * When to use: Never write this in an interview unless specifically asked to 
     * start with the absolute simplest code. Just verbalize it.
     */
    public static int[] dailyTemperaturesBruteForce(int[] temperatures) {
        int n = temperatures.length;
        int[] ans = new int[n];
        
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (temperatures[j] > temperatures[i]) {
                    ans[i] = j - i;
                    break;
                }
            }
        }
        return ans;
    }

    /**
     * SOLUTION 2: OPTIMAL (Monotonic Stack)
     * ------------------------------------------------------------------------
     * Idea: We use a stack to keep track of the INDICES of the days we haven't 
     * found a warmer temperature for yet.
     * We iterate through the array. If the current day's temperature is warmer 
     * than the temperature of the day at the top of the stack, we found the 
     * answer for that past day! We pop the past day, calculate the distance 
     * (`current_index - popped_index`), and record it. We keep checking the new 
     * top of the stack until it's no longer colder than today.
     * Then, we push today's index onto the stack to wait for its own warmer day.
     * 
     * Time Complexity: O(N) - Every index is pushed exactly once and popped at 
     * most once. So the inner `while` loop runs at most N times globally.
     * Space Complexity: O(N) - In the worst case (decreasing temperatures), 
     * all indices are pushed onto the stack.
     */
    public static int[] dailyTemperaturesStack(int[] temperatures) {
        if (temperatures == null || temperatures.length == 0) return new int[0];

        int n = temperatures.length;
        // In Java, an int array initializes to 0s, which is exactly what we need
        // for days that never find a warmer temperature.
        int[] ans = new int[n];
        
        // Deque is preferred over Stack for performance
        Deque<Integer> stack = new ArrayDeque<>();

        for (int i = 0; i < n; i++) {
            int currentTemp = temperatures[i];

            // While stack is not empty AND today is hotter than the day at the top of stack
            while (!stack.isEmpty() && currentTemp > temperatures[stack.peek()]) {
                // We found a warmer day for the past unresolved day!
                int prevDayIndex = stack.pop();
                // The wait time is the difference in indices
                ans[prevDayIndex] = i - prevDayIndex;
            }
            
            // Push today's index onto the stack to wait for a warmer day
            stack.push(i);
        }

        return ans;
    }

    /**
     * SOLUTION 3: ULTRA-OPTIMAL (Right-to-Left Jumps / O(1) Space)
     * ------------------------------------------------------------------------
     * Idea: We can solve this without a stack! We already have an output array 
     * that stores the distance to the next warmer day. We can use this calculated 
     * distance to "fast-forward" or "jump" through the array.
     * 
     * We iterate from right to left.
     * To find the next warmer day for `temperatures[i]`, we first look at `i+1`.
     * If `temperatures[i+1]` is warmer, great! Distance is 1.
     * If it's colder, we look at `ans[i+1]` to see where `i+1`'s next warmer day is, 
     * and jump straight there. We repeat this jump until we find a warmer day or hit 0.
     * 
     * Time Complexity: O(N) - Although there's a while loop, we skip massive chunks 
     * of the array on every jump, guaranteeing average O(1) work per element.
     * Space Complexity: O(1) auxiliary space (excluding the output array).
     */
    public static int[] dailyTemperaturesO1Space(int[] temperatures) {
        int n = temperatures.length;
        int[] ans = new int[n];
        
        // The last element has no future days, so it stays 0.
        // We start from the second to last day and go backwards.
        for (int i = n - 2; i >= 0; i--) {
            int next = i + 1;
            
            // Keep jumping forward as long as the future day is colder or equal
            while (temperatures[next] <= temperatures[i]) {
                if (ans[next] > 0) {
                    // Jump directly to the next known warmer day for index 'next'
                    next = next + ans[next];
                } else {
                    // ans[next] == 0 means there is NO warmer day after 'next'.
                    // Therefore, there can't be a warmer day for 'i' either.
                    next = -1; // Flag indicating no warmer day
                    break;
                }
            }
            
            if (next != -1) {
                ans[i] = next - i;
            }
        }
        return ans;
    }

    /**
     * ========================================================================
     * 6. SOLUTION COMPARISON
     * ========================================================================
     * Approach       | Time   | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * Brute Force    | O(N^2) | O(1)  | Trivial but too slow.      | Mention only.
     * Monotonic Stack| O(N)   | O(N)  | Standard pattern, clear.   | ⭐ PRESENT THIS.
     * R-to-L Jumps   | O(N)   | O(1)  | Genius space optimization. | Great Follow-up!
     * 
     * ========================================================================
     * 7. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if the temperatures are coming in as a continuous stream, and we 
     *      don't know the array size in advance?"
     * A1: We cannot use the Right-to-Left jump optimization because it requires 
     *     scanning backwards. We MUST use the Monotonic Stack approach, pushing 
     *     incoming elements as they arrive and emitting resolved elements dynamically.
     * 
     * Q2: "Can you optimize the space if the range of temperatures is very small? 
     *      (e.g., 30 to 100 as in the constraints)"
     * A2: Yes! Instead of an O(N) stack, we can use an array of size 71 (100 - 30 + 1) 
     *     to keep track of the *most recent index* we saw for every specific 
     *     temperature. As we iterate backwards, to find the next warmer day for `T`, 
     *     we just check our array for all temperatures from `T+1` to `100` and pick 
     *     the minimum index. This is O(N * 71) time -> O(N), and O(71) space -> O(1).
     * 
     * Q3: "Why use `ArrayDeque` instead of `Stack` in Java?"
     * A3: `java.util.Stack` extends `Vector` and introduces synchronized method 
     *     overhead, making it slow. `ArrayDeque` is the modern, fast, non-synchronized 
     *     implementation for LIFO structures.
     * 
     * ========================================================================
     * 8. FINAL TAKEAWAYS
     * ========================================================================
     * - Key Pattern: Finding the "Next Greater Element" in an array is almost ALWAYS 
     *   solved optimally with a Monotonic Decreasing Stack.
     * - Storage Trick: Always store the INDICES in the stack, not the values. The index 
     *   gives you both the value (`arr[index]`) AND the position (allowing you to 
     *   calculate distances).
     * - Advanced Optimization: If an output array stores positional jumps, you can 
     *   sometimes use those jumps in a reverse pass to eliminate the stack entirely.
     */
}


/**
 * ============================================================
 * 🔥 DAILY TEMPERATURES — ALL 3 VARIANTS (INTERVIEW MASTER FILE)
 * ============================================================
 *
 * Problem:
 * For each day, find how many days until a warmer temperature.
 *
 * If no warmer day exists → answer = 0
 *
 * ------------------------------------------------------------
 * 🧠 CORE PATTERN:
 * "Next Greater Element on Right"
 *
 * ------------------------------------------------------------
 * 🎯 GOAL:
 * Given index i → find closest j > i such that:
 *      temperatures[j] > temperatures[i]
 *
 * ------------------------------------------------------------
 * 🚀 APPROACHES INCLUDED:
 *
 * 1. Forward Monotonic Stack (Left → Right)
 * 2. Reverse Monotonic Stack (Right → Left)  ← CLEANEST
 * 3. Reverse DP Jumping (No Stack)
 *
 * ------------------------------------------------------------
 * ⏱ COMPLEXITIES:
 *
 * All optimal solutions:
 * Time:  O(n)
 * Space: O(n)
 *
 * ------------------------------------------------------------
 */
public class DailyTemperaturesAllVariants {

    public static void main(String[] args) {
        int[] temps = {73, 74, 75, 71, 69, 72, 76, 73};

        System.out.println("Forward Stack: " + Arrays.toString(forwardStack(temps)));
        System.out.println("Reverse Stack: " + Arrays.toString(reverseStack(temps)));
        System.out.println("DP Jumping   : " + Arrays.toString(dpJumping(temps)));
    }

    /**
     * ============================================================
     * 🔥 1. FORWARD MONOTONIC STACK (Left → Right)
     * ============================================================
     *
     * 🧠 INTUITION:
     * We process days in order.
     *
     * Stack = indices of days waiting for a warmer temperature
     *
     * When current temperature is higher:
     *   → we resolve previous colder days
     *
     * ------------------------------------------------------------
     * 📌 STACK INVARIANT:
     * temperatures[stack] = strictly decreasing
     *
     * ------------------------------------------------------------
     * 🧪 THINK:
     * "I will solve previous days when I find a warmer day"
     *
     * ------------------------------------------------------------
     */
    public static int[] forwardStack(int[] temperatures) {

        int n = temperatures.length;
        int[] answer = new int[n];

        // Stack stores indices (NOT values)
        Deque<Integer> stack = new ArrayDeque<>();

        for (int i = 0; i < n; i++) {

            /**
             * 🔥 Resolve all previous colder days
             *
             * Why?
             * Current temp is warmer → it's the answer for them
             */
            while (!stack.isEmpty() &&
                    temperatures[i] > temperatures[stack.peek()]) {

                int prevIndex = stack.pop();

                /**
                 * Distance between current index and previous index
                 */
                answer[prevIndex] = i - prevIndex;
            }

            /**
             * Add current day as a candidate
             */
            stack.push(i);
        }

        /**
         * Remaining indices → no warmer day → default 0
         */
        return answer;
    }


    /**
     * ============================================================
     * 🔥 2. REVERSE MONOTONIC STACK (Right → Left) ⭐ BEST
     * ============================================================
     *
     * 🧠 INTUITION:
     * Instead of solving previous elements,
     * we directly find answer for current element.
     *
     * ------------------------------------------------------------
     * 📌 STACK MEANING:
     * Stack contains indices of future days that are:
     *   - Warmer than current
     *   - Useful candidates
     *
     * ------------------------------------------------------------
     * 🧪 THINK:
     * "Remove useless days, top becomes answer"
     *
     * ------------------------------------------------------------
     */
    public static int[] reverseStack(int[] temperatures) {

        int n = temperatures.length;
        int[] answer = new int[n];

        Deque<Integer> stack = new ArrayDeque<>();

        for (int i = n - 1; i >= 0; i--) {

            /**
             * 🔥 Remove useless candidates
             *
             * Why remove?
             * If temp <= current:
             *   - Cannot be answer for current
             *   - Cannot help any previous element either
             */
            while (!stack.isEmpty() &&
                    temperatures[stack.peek()] <= temperatures[i]) {

                stack.pop();
            }

            /**
             * After cleanup:
             * Top of stack = next warmer day
             */
            if (!stack.isEmpty()) {
                answer[i] = stack.peek() - i;
            }

            /**
             * Add current index as candidate
             */
            stack.push(i);
        }

        return answer;
    }


    /**
     * ============================================================
     * 🔥 3. DP + JUMPING (NO STACK)
     * ============================================================
     *
     * 🧠 INTUITION:
     * Use already computed answers to "jump forward"
     *
     * Instead of checking every next element,
     * we skip using previous results
     *
     * ------------------------------------------------------------
     * 🧪 THINK:
     * "If next is not useful → jump using its answer"
     *
     * ------------------------------------------------------------
     */
    public static int[] dpJumping(int[] temperatures) {

        int n = temperatures.length;
        int[] answer = new int[n];

        /**
         * Start from second last element
         * (last element always 0)
         */
        for (int i = n - 2; i >= 0; i--) {

            int j = i + 1;

            /**
             * Try to find warmer day
             */
            while (j < n && temperatures[j] <= temperatures[i]) {

                /**
                 * If j has no answer → no point आगे जाना
                 */
                if (answer[j] == 0) {
                    j = n; // terminate
                } else {
                    /**
                     * 🔥 Jump ahead
                     *
                     * Instead of j++, we skip directly
                     */
                    j = j + answer[j];
                }
            }

            /**
             * If valid warmer day found
             */
            if (j < n) {
                answer[i] = j - i;
            }
        }

        return answer;
    }


    /**
     * ============================================================
     * 🧪 DRY RUN TEMPLATE (Use in Interviews)
     * ============================================================
     *
     * Input:
     * [73, 74, 75, 71, 69, 72, 76, 73]
     *
     * Reverse Stack Thinking:
     *
     * i = 7 → 73 → stack empty → push
     * i = 6 → 76 → pop 73 → push
     * i = 5 → 72 → next = 76 → ans[5]=1
     * i = 4 → 69 → next = 72 → ans[4]=1
     * i = 3 → 71 → pop 69 → next=72 → ans[3]=2
     *
     * ------------------------------------------------------------
     *
     * KEY OBSERVATION:
     * Each element is pushed once and popped once
     *
     * → That's why O(n)
     *
     * ------------------------------------------------------------
     */
}
