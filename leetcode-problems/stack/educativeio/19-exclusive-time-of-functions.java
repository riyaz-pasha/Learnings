/**
 * ============================================================================
 * EXCLUSIVE TIME OF FUNCTIONS - COMPLETE INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: We have a single-threaded CPU executing functions. We are given 
 * the number of functions `n` and a list of formatted logs (e.g., "0:start:0").
 * When a function starts, it pauses any currently running function. When it 
 * ends, the paused function resumes. We need to compute the total exclusive 
 * time spent executing each function (time spent ONLY on that function, not 
 * including time spent on inner nested function calls).
 * 
 * Core Idea & Intuition:
 * The single-threaded nature with "pause and resume" mechanics perfectly 
 * describes a Call Stack. The Last-In-First-Out (LIFO) property means the 
 * function that started most recently is the one currently executing. 
 * Therefore, a Stack is the ideal data structure.
 * 
 * To calculate exclusive time without complex overlapping math, we can just 
 * record the time difference between events. When a new log arrives, the time 
 * elapsed since the previous log belongs entirely to the function that was 
 * sitting at the top of the stack during that interval.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Are the logs strictly sorted by timestamp chronologically?"
 *    Why: If not, we'd have to parse and sort them first.
 *    Impact: In typical executions, yes, they are generated sequentially in time.
 * 
 * 2. Q: "Can a function call itself recursively?"
 *    Why: To confirm if the stack might contain duplicate IDs consecutively.
 *    Impact: Yes, the same ID can appear multiple times. Our stack just stores 
 *    IDs, so it naturally handles recursion perfectly without any code changes.
 * 
 * 3. Q: "How is the 'end' timestamp measured?"
 *    Why: Time boundaries are notoriously tricky. 
 *    Impact: A start at `2` and end at `5` means the function runs during 
 *    timestamps 2, 3, 4, and 5. That is `5 - 2 + 1 = 4` units of time.
 * 
 * 4. Q: "Can I assume the input logs are perfectly valid and balanced?"
 *    Why: Prevents writing unnecessary stack empty checks for 'end' operations.
 *    Impact: Yes, constraints guarantee each start has a matching end.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example:
 * n = 2, logs = ["0:start:0", "1:start:2", "1:end:5", "0:end:6"]
 * 
 * Timeline Visualization:
 * Time:   0   1   2   3   4   5   6
 *         |---|---|---|---|---|---|
 * Func 0: |=======|               |===| (Total: 2 + 1 = 3)
 * Func 1:         |===============|     (Total: 4)
 * 
 * Notice that Function 0 paused at time 2 when Function 1 started. It resumed 
 * after time 5 (specifically, at time 6) when Function 1 ended.
 * Output: [3, 4]
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Confirm it's just a standard call stack simulation.
 * 2. Clarify: Ask about the inclusive nature of the end timestamp (the +1 rule).
 * 3. Initial Approach (Inner Subtraction): We can push functions onto the stack 
 *    with their start times. When an 'end' arrives, pop it, calculate total 
 *    duration (end - start + 1), add to its exclusive time, and SUBTRACT this 
 *    duration from the parent function currently at the top of the stack.
 * 4. Optimal Approach (Time Gaps): Keep a `prevTime` variable. Every time an 
 *    event happens (start or end), calculate `currentTime - prevTime` and give 
 *    that time directly to whoever was at the top of the stack. This is cleaner 
 *    and avoids backward subtractions.
 * 5. Code: Write the Optimal "Time Gaps" approach.
 * 6. Dry-run: Trace the example carefully, pointing out the +1 logic on ends.
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - Recursive calls: Func 0 calls Func 0. Stack handles this inherently.
 * - Sequential calls: "0:start:0", "0:end:1", "1:start:2", "1:end:3". There 
 *   could be gaps or back-to-back executions.
 * 
 * Common Mistakes:
 * - The Off-By-One Error (OBOE): Forgetting that `end` happens at the *end* of 
 *   the timestamp unit. Time gap for `start` to `start` is `time - prevTime`. 
 *   Time gap for `start` to `end` is `time - prevTime + 1`.
 * - Failing to advance `prevTime` correctly: After an `end` event at time `T`, 
 *   the next time unit starts at `T + 1`. So `prevTime` must become `T + 1`.
 * - Splitting heavily: `String.split(":")` is clean but doing it inside the 
 *   loop is slightly slow. It's acceptable for interviews, but parsing chars 
 *   manually is faster if execution speed is critical.
 */

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Arrays;

public class ExclusiveTimeOfFunctions {

    public static void main(String[] args) {
        int n1 = 2;
        List<String> logs1 = Arrays.asList("0:start:0", "1:start:2", "1:end:5", "0:end:6");
        
        int n2 = 1;
        List<String> logs2 = Arrays.asList("0:start:0", "0:start:2", "0:end:5", "0:end:6");

        System.out.println("--- Optimal PrevTime Approach ---");
        System.out.println("Logs 1: " + Arrays.toString(exclusiveTimeOptimal(n1, logs1))); // Expected: [3, 4]
        System.out.println("Logs 2 (Recursive): " + Arrays.toString(exclusiveTimeOptimal(n2, logs2))); // Expected: [7]

        System.out.println("\n--- Alternative Subtraction Approach ---");
        System.out.println("Logs 1: " + Arrays.toString(exclusiveTimeSubtraction(n1, logs1))); // Expected: [3, 4]
    }

    /**
     * SOLUTION 1: OPTIMAL APPROACH (Time Gaps Tracking)
     * ------------------------------------------------------------------------
     * Idea: Maintain a `prevTime` marker. As we process each log, the time 
     * elapsed since `prevTime` belongs EXCLUSIVELY to whatever function was 
     * currently running (the one at the top of the stack). 
     * We add this elapsed time to the top function, then update the stack and 
     * `prevTime` based on whether the current event is a "start" or "end".
     * 
     * Time Complexity: O(L) where L is the number of logs. One pass through the list.
     * Space Complexity: O(L) for the stack in the worst-case (deeply nested calls).
     */
    public static int[] exclusiveTimeOptimal(int n, List<String> logs) {
        int[] res = new int[n];
        // Stack stores ONLY the function IDs
        Deque<Integer> stack = new ArrayDeque<>();
        int prevTime = 0;

        for (String log : logs) {
            // Parse the log
            String[] parts = log.split(":");
            int id = Integer.parseInt(parts[0]);
            boolean isStart = parts[1].equals("start");
            int time = Integer.parseInt(parts[2]);

            if (isStart) {
                // If another function is already running, its uninterrupted 
                // run time ends right now. Add the elapsed time to it.
                if (!stack.isEmpty()) {
                    res[stack.peek()] += time - prevTime;
                }
                
                // Push the new function to the stack and update prevTime
                stack.push(id);
                prevTime = time;
                
            } else {
                // The current function finishes. It runs until the END of the 
                // current time unit, so we add +1 to the difference.
                res[stack.peek()] += time - prevTime + 1;
                stack.pop();
                
                // The next function will resume at the start of the NEXT time unit
                prevTime = time + 1;
            }
        }
        
        return res;
    }

    /**
     * SOLUTION 2: INNER TIME SUBTRACTION (Alternative Valid Approach)
     * ------------------------------------------------------------------------
     * Idea: Instead of giving time in chunks, let a function claim its full 
     * lifetime (end - start + 1). However, since that lifetime includes nested 
     * calls, we must subtract this full lifetime from the parent function 
     * sitting directly underneath it in the stack.
     * 
     * Time Complexity: O(L).
     * Space Complexity: O(L) to store the start times alongside IDs in the stack.
     */
    public static int[] exclusiveTimeSubtraction(int n, List<String> logs) {
        int[] res = new int[n];
        
        // Stack stores an int array: [functionId, startTime]
        Deque<int[]> stack = new ArrayDeque<>();

        for (String log : logs) {
            String[] parts = log.split(":");
            int id = Integer.parseInt(parts[0]);
            boolean isStart = parts[1].equals("start");
            int time = Integer.parseInt(parts[2]);

            if (isStart) {
                stack.push(new int[]{id, time});
            } else {
                // Current function ends
                int[] currentFunc = stack.pop();
                int duration = time - currentFunc[1] + 1;
                
                // Give the current function its full duration
                res[id] += duration;
                
                // Subtract this duration from the parent function (if one exists)
                // because the parent was paused during this time.
                if (!stack.isEmpty()) {
                    res[stack.peek()[0]] -= duration;
                }
            }
        }
        
        return res;
    }

    /**
     * ========================================================================
     * 6. DETAILED TRACING (Optimal Approach)
     * ========================================================================
     * Logs: ["0:start:0", "1:start:2", "1:end:5", "0:end:6"]
     * Init: res = [0, 0], prevTime = 0, Stack = []
     * 
     * Log 1: "0:start:0"
     * - isStart = true. Stack is empty.
     * - push 0. Stack: [0]
     * - prevTime = 0
     * 
     * Log 2: "1:start:2"
     * - isStart = true. Stack not empty (top is 0).
     * - Gap = time - prev = 2 - 0 = 2.
     * - res[0] += 2 -> res = [2, 0]
     * - push 1. Stack: [1, 0]
     * - prevTime = 2
     * 
     * Log 3: "1:end:5"
     * - isStart = false. Top is 1.
     * - Gap = time - prev + 1 = 5 - 2 + 1 = 4.
     * - res[1] += 4 -> res = [2, 4]
     * - pop. Stack: [0]
     * - prevTime = time + 1 = 6
     * 
     * Log 4: "0:end:6"
     * - isStart = false. Top is 0.
     * - Gap = time - prev + 1 = 6 - 6 + 1 = 1.
     * - res[0] += 1 -> res = [3, 4]
     * - pop. Stack: []
     * - prevTime = time + 1 = 7
     * 
     * Final Result: [3, 4]
     * 
     * ========================================================================
     * 7. SOLUTION COMPARISON
     * ========================================================================
     * Approach         | Time | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * Time Gaps (Opt.) | O(L) | O(L)  | Clean, no nested object    | ⭐ STRONGLY REC.
     *                  |      |       | arrays, easy arithmetic.   | 
     * Subtraction      | O(L) | O(L)  | Avoids global tracking var | Good alternative 
     *                  |      |       | but involves negative sums.| if logic flows better.
     * 
     * ========================================================================
     * 8. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if the logs are massive and given as a data stream?"
     * A1: Both stack solutions are perfectly suited for streaming because we process 
     *     each log incrementally and do not need to look ahead. We just update the 
     *     `res` array on the fly.
     * 
     * Q2: "What if the logs were unordered?"
     * A2: We would have to parse all of them, extract the timestamp, sort them by 
     *     timestamp, and then run this logic. That would increase the time 
     *     complexity to O(L log L).
     * 
     * ========================================================================
     * 9. FINAL TAKEAWAYS
     * ========================================================================
     * - Key Pattern: Nested, exclusive lifespans (like CPU processes, HTML tags, 
     *   parentheses) always map to a Stack.
     * - Edge Case Handling: Pay intense attention to whether boundaries are 
     *   inclusive or exclusive. "End at time 5" usually means inclusive of time 5, 
     *   triggering a `+ 1` adjustment.
     * - Data representation matters: Passing the burden of calculation off to 
     *   `time - prevTime` is much cleaner than tracking start-end intervals on 
     *   objects.
     */
}



/**
 * ====================================================================================
 * PROBLEM STATEMENT: EXCLUSIVE TIME OF FUNCTIONS (LeetCode 636)
 * ====================================================================================
 * On a single-threaded CPU, we execute `n` functions (IDs: 0 to n-1).
 * Function calls are nested (a function can call another function or itself).
 * 
 * We receive a log stream formatted as: "function_id:start_or_end:timestamp"
 * - "0:start:3" -> Function 0 started at the BEGINNING of timestamp unit 3.
 * - "0:end:5"   -> Function 0 ended at the END of timestamp unit 5.
 * 
 * GOAL:
 * Return an array `result` of size `n` where `result[id]` represents the total
 * EXCLUSIVE execution time spent solely inside function `id` (excluding time 
 * spent inside child functions called by `id`).
 * ====================================================================================
 */

public class ExclusiveTimeOfFunctions {

    public static void main(String[] args) {
        int n = 2;
        List<String> logs = List.of(
            "0:start:0",
            "1:start:2",
            "1:end:5",
            "0:end:6"
        );

        int[] result = exclusiveTime(n, logs);
        System.out.println("Exclusive Execution Times: " + Arrays.toString(result)); 
        // Output: [3, 4]
    }

    /**
     * Calculates exclusive CPU execution time for each function in O(L) time and O(N) space.
     *
     * @param n    Total number of functions (0 to n-1)
     * @param logs List of execution log strings formatted as "id:type:timestamp"
     * @return Array where result[i] is the exclusive execution time of function i
     */
    public static int[] exclusiveTime(int n, List<String> logs) {
        int[] result = new int[n];

        /*
         * ----------------------------------------------------------------------------
         * CALL STACK SIMULATION
         * ----------------------------------------------------------------------------
         * Since CPU execution follows a LIFO (Last-In, First-Out) call stack:
         * - Pushing to stack  == Function call (start)
         * - Popping from stack == Function return (end)
         * 
         * `stack` stores ONLY the function IDs currently in an active or paused state.
         * At any point, the top of the stack (`stack.peek()`) is the CPU's ACTIVE function.
         */
        Deque<Integer> stack = new ArrayDeque<>();

        /*
         * `prevTime` tracks the exact timestamp boundary where the CURRENT continuous
         * execution segment began.
         */
        int prevTime = 0;

        for (String log : logs) {
            // Parse log entry: "id:type:timestamp"
            int lastColonIndex = log.lastIndexOf(':');
            int firstColonIndex = log.indexOf(':');

            int functionId = Integer.parseInt(log.substring(0, firstColonIndex));
            boolean isStart = log.charAt(firstColonIndex + 1) == 's'; // 's' for "start"
            int timestamp = Integer.parseInt(log.substring(lastColonIndex + 1));

            if (isStart) {
                /*
                 * ====================================================================
                 * EVENT 1: FUNCTION STARTS ("start")
                 * ====================================================================
                 * A new function `functionId` starts execution at the BEGINNING of `timestamp`.
                 * 
                 * IF a parent function was running (`!stack.isEmpty()`):
                 *   - The parent function is now PAUSED by this child function.
                 *   - The parent ran uninterrupted from `prevTime` up to `timestamp`.
                 *   - Total elapsed duration = `timestamp - prevTime`.
                 *   - We credit this duration to `result[stack.peek()]`.
                 * 
                 * THEN:
                 *   - Push `functionId` onto the call stack.
                 *   - Update `prevTime = timestamp` because `functionId` starts executing 
                 *     at the beginning of this unit time block.
                 */
                if (!stack.isEmpty()) {
                    int parentId = stack.peek();
                    result[parentId] += (timestamp - prevTime);
                }

                stack.push(functionId);
                prevTime = timestamp;

            } else {
                /*
                 * ====================================================================
                 * EVENT 2: FUNCTION ENDS ("end")
                 * ====================================================================
                 * The function at the top of the stack finishes at the END of `timestamp`.
                 * 
                 * CRITICAL INCLUSIVE TIME INVARIANT:
                 * - "end at 5" means the function executed throughout time unit 5.
                 * - Total elapsed duration = `timestamp - prevTime + 1`.
                 * - Pop the function from the stack and credit this full duration to it.
                 * 
                 * AFTER ENDING:
                 * - The next available CPU unit block starts at `timestamp + 1`.
                 * - Update `prevTime = timestamp + 1`.
                 * - If a parent function resumes, it will begin accumulating time 
                 *   starting from `timestamp + 1`.
                 */
                int finishedId = stack.pop();
                result[finishedId] += (timestamp - prevTime + 1);

                prevTime = timestamp + 1;
            }
        }

        return result;
    }
}


/**
 * ====================================================================================
 * 🔥 EXCLUSIVE TIME OF FUNCTIONS (LEETCODE 636) - MASTER INTERVIEW REFERENCE
 * ====================================================================================
 *
 * 🧠 1. PROBLEM UNDERSTANDING & ESSENCE
 * ------------------------------------------------------------------------------------
 * On a single-threaded CPU, `n` functions (IDs: 0 to n-1) are executed.
 * Function calls are strictly sequential or nested (i.e., a function can invoke another
 * function, or call itself recursively).
 *
 * We are provided a log stream where each entry is formatted as a string:
 *     "function_id : start_or_end : timestamp"
 *
 *   - "0:start:3" -> Function 0 started at the BEGINNING of unit time slot 3.
 *   - "0:end:5"   -> Function 0 ended at the END of unit time slot 5.
 *
 * 🎯 GOAL:
 * Compute the EXCLUSIVE TIME for each function (0 to n-1).
 * "Exclusive time" is the total time spent executing code inside function `i` ONLY.
 * Any time spent executing child functions invoked by `i` is EXCLUDED from `i`'s total.
 *
 * ------------------------------------------------------------------------------------
 *
 * 💡 2. CORE INTUITION: CALL STACK SIMULATION
 * ------------------------------------------------------------------------------------
 * CPU call stacks operate under LIFO (Last-In, First-Out) semantics:
 *   - When Function B is called inside Function A:
 *     Function A is PAUSED, and Function B is placed on top of the call stack.
 *   - When Function B completes:
 *     Function B is REMOVED (popped), and Function A RESUMES execution.
 *
 * Therefore, a Monotonic Stack (`Deque<Integer>`) perfectly models the active CPU state!
 *
 *   - Stack Top (`stack.peek()`) = Currently active function on the CPU.
 *   - Stack Push                 = Function invocation / START log.
 *   - Stack Pop                  = Function return / END log.
 *
 * ------------------------------------------------------------------------------------
 *
 * ⚠️ 3. CRITICAL INVARIANT: DISCRETE TIME UNIT MODEL (THE "+1" TRICK)
 * ------------------------------------------------------------------------------------
 * Timestamps represent UNIT SLOTS, not zero-width geometric points on a number line!
 *
 *  Timestamp:      0         1         2         3         4         5         6
 *  Time Slot:   [  0  ]  [  1  ]  [  2  ]  [  3  ]  [  4  ]  [  5  ]  [  6  ]
 *               ^               ^                                  ^        ^
 *          "0:start:0"     "1:start:2"                        "1:end:5"  "0:end:6"
 *
 * - "start:2" means execution begins at the START of unit slot 2.
 * - "end:5"   means execution completes at the END of unit slot 5.
 *
 * 📐 DURATION CALCULATIONS:
 * 1. Pause Parent (on "start" event at time T):
 *    Parent ran from `prevTime` up to `T` (exclusive of slot T, as child starts at T).
 *    Duration = T - prevTime
 *
 * 2. Complete Child (on "end" event at time T):
 *    Child ran from `prevTime` through slot `T` (INCLUSIVE of slot T).
 *    Duration = T - prevTime + 1
 *    New `prevTime` = T + 1 (the next available unassigned unit slot).
 *
 * ------------------------------------------------------------------------------------
 *
 * 📊 4. STATE TRANSITIONS & ALGORITHM INVARIANTS
 * ------------------------------------------------------------------------------------
 * Maintain two variables:
 *   - `stack`    : Call stack holding active function IDs.
 *   - `prevTime` : Beginning of the currently unassigned continuous CPU execution segment.
 *
 * FOR EACH LOG ENTRY:
 *   Parse `(functionId, type, timestamp)`
 *
 *   IF type == "start":
 *     1. IF `stack` is not empty:
 *        `result[stack.peek()] += (timestamp - prevTime)`  // Credit parent CPU time
 *     2. `stack.push(functionId)`                           // Activate child
 *     3. `prevTime = timestamp`                             // Reset timeline pointer
 *
 *   IF type == "end":
 *     1. `finishedFunc = stack.pop()`                       // Deactivate child
 *     2. `result[finishedFunc] += (timestamp - prevTime + 1)` // Credit child full inclusive time
 *     3. `prevTime = timestamp + 1`                         // Point to next CPU slot
 *
 * ------------------------------------------------------------------------------------
 *
 * 🔍 5. FULL DRY RUN WALKTHROUGH
 * ------------------------------------------------------------------------------------
 * Input: n = 2, logs = ["0:start:0", "1:start:2", "1:end:5", "0:end:6"]
 * Initial State: stack = [], prevTime = 0, result = [0, 0]
 *
 * ┌──────┬─────────────┬──────────┬──────────┬──────────────┬──────────────────────────────────────────┐
 * │ Step │ Parsed Log  │  Stack   │ prevTime │ result Array │ Action Details                           │
 * ├──────┼─────────────┼──────────┼──────────┼──────────────┼──────────────────────────────────────────┤
 * │  0   │ [Initial]   │ []       │    0     │ [0, 0]       │ Initialize data structures.              │
 * │  1   │ "0:start:0" │ [0]      │    0     │ [0, 0]       │ Push 0. Stack had no parent to credit.   │
 * │  2   │ "1:start:2" │ [1, 0]   │    2     │ [2, 0]       │ Credit parent 0: (2 - 0) = 2. Push 1.    │
 * │  3   │ "1:end:5"   │ [0]      │    6     │ [2, 4]       │ Pop 1. Credit 1: (5 - 2 + 1) = 4.        │
 * │  4   │ "0:end:6"   │ []       │    7     │ [3, 4]       │ Pop 0. Credit 0: (6 - 6 + 1) = 1.        │
 * └──────┴─────────────┴──────────┴──────────┴──────────────┴──────────────────────────────────────────┘
 * Final Output: [3, 4]
 *
 * ------------------------------------------------------------------------------------
 *
 * ⏱ 6. COMPLEXITY ANALYSIS
 * ------------------------------------------------------------------------------------
 * - Time Complexity  : O(L), where L is the total number of logs.
 *                      We iterate through the log list once. Parsing strings and 
 *                      stack operations (push/pop/peek) are all O(1) time.
 * - Space Complexity : O(N + L), where N is the number of functions (for result array)
 *                      and L/2 is the maximum possible call stack depth.
 *
 * ====================================================================================
 */
public class ExclusiveTimeOfFunctions {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("🔥 EXCLUSIVE TIME OF FUNCTIONS - TEST SUITE");
        System.out.println("=================================================\n");

        // Test Case 1: Standard nested invocation
        int n1 = 2;
        List<String> logs1 = List.of(
            "0:start:0",
            "1:start:2",
            "1:end:5",
            "0:end:6"
        );
        int[] res1 = exclusiveTime(n1, logs1);
        System.out.println("Test Case 1 Output : " + Arrays.toString(res1));
        System.out.println("Expected Output    : [3, 4]\n");

        // Test Case 2: Deeply nested and sequential calls
        int n2 = 3;
        List<String> logs2 = List.of(
            "0:start:0",
            "1:start:2",
            "2:start:4",
            "2:end:5",
            "1:end:7",
            "0:end:8"
        );
        int[] res2 = exclusiveTime(n2, logs2);
        System.out.println("Test Case 2 Output : " + Arrays.toString(res2));
        System.out.println("Expected Output    : [3, 4, 2]\n");

        // Test Case 3: Recursive / Self-invoking function
        int n3 = 1;
        List<String> logs3 = List.of(
            "0:start:0",
            "0:start:2",
            "0:end:4",
            "0:end:6"
        );
        int[] res3 = exclusiveTime(n3, logs3);
        System.out.println("Test Case 3 Output : " + Arrays.toString(res3));
        System.out.println("Expected Output    : [7]\n");
    }

    /**
     * Calculates the exclusive CPU running time for each function.
     *
     * @param n    Number of functions (IDs 0 to n-1)
     * @param logs Execution logs formatted as "function_id:start|end:timestamp"
     * @return Array of size n containing exclusive time allocated to each function ID
     */
    public static int[] exclusiveTime(int n, List<String> logs) {
        // Output array holding exclusive times for function IDs 0 through n-1
        int[] result = new int[n];

        // Explicit call stack simulating CPU context switching (stores function IDs)
        Deque<Integer> stack = new ArrayDeque<>();

        // Tracks the start boundary timestamp of the current uninterrupted execution block
        int prevTime = 0;

        for (String log : logs) {
            // -------------------------------------------------------------------------
            // 🔹 STEP 1: PARSE LOG ENTRY EFFICIENTLY
            // -------------------------------------------------------------------------
            // Using direct String indexing avoids overhead from String.split() array creation
            int firstColon = log.indexOf(':');
            int lastColon = log.lastIndexOf(':');

            int functionId = Integer.parseInt(log.substring(0, firstColon));
            boolean isStart = log.charAt(firstColon + 1) == 's'; // 's' implies "start"
            int timestamp = Integer.parseInt(log.substring(lastColon + 1));

            // -------------------------------------------------------------------------
            // 🔹 STEP 2: PROCESS EVENT TRANSITION
            // -------------------------------------------------------------------------
            if (isStart) {
                /*
                 * EVENT: "start"
                 * A new or recursive function begins execution.
                 *
                 * 1. If a parent function is currently active at stack top:
                 *    - Parent played continuously from `prevTime` up to `timestamp`.
                 *    - Accrue this duration (timestamp - prevTime) to parent's account.
                 * 2. Push the newly starting functionId onto the stack.
                 * 3. Set `prevTime = timestamp` since the new function starts NOW.
                 */
                if (!stack.isEmpty()) {
                    int parentId = stack.peek();
                    result[parentId] += (timestamp - prevTime);
                }

                stack.push(functionId);
                prevTime = timestamp;

            } else {
                /*
                 * EVENT: "end"
                 * The currently active function finishes execution.
                 *
                 * 1. Pop the finished function from top of call stack.
                 * 2. Add full inclusive execution time: (timestamp - prevTime + 1).
                 * 3. Advance `prevTime` to `timestamp + 1` (beginning of next slot).
                 */
                int finishedFuncId = stack.pop();
                result[finishedFuncId] += (timestamp - prevTime + 1);

                prevTime = timestamp + 1;
            }
        }

        return result;
    }
}
