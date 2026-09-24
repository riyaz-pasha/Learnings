import java.util.*;

/**
 * PROBLEM ANALYSIS: TASK SCHEDULER
 * ==================================
 * 
 * PROBLEM UNDERSTANDING:
 * - Given: Array of tasks (each represented by A-Z) and cooldown period n
 * - Constraint: Same task must be separated by at least n intervals
 * - Goal: Find minimum CPU intervals (including idle time) to complete all tasks
 * 
 * KEY INSIGHTS:
 * 1. Tasks can be executed in any order
 * 2. Must wait n intervals before repeating same task
 * 3. Can use idle intervals if no valid task available
 * 4. The task that appears most frequently determines minimum time
 * 
 * CRITICAL REALIZATION:
 * - The most frequent task creates a "skeleton" schedule
 * - Other tasks fill in the gaps between repetitions of most frequent task
 * - If tasks can't fill all gaps, we need idle time
 * - If we have enough tasks to fill gaps, no idle time needed
 * 
 * INTERVIEW APPROACH - HOW TO THINK THROUGH THIS:
 * ================================================
 * 
 * Step 1: Understand with examples
 * tasks = [A,A,A,B,B,B], n = 2
 * - A appears 3 times, B appears 3 times
 * - Need 2 intervals between same task
 * - Schedule: A -> B -> idle -> A -> B -> idle -> A -> B
 * - Total: 8 intervals
 * 
 * Step 2: Identify the pattern
 * - Most frequent task determines structure
 * - Creates (maxFreq - 1) "chunks" of size (n + 1)
 * - Last occurrence doesn't need cooldown
 * - Other tasks fill the gaps
 * 
 * Step 3: Mathematical formula approach
 * - If max frequency is maxFreq:
 *   Base intervals = (maxFreq - 1) * (n + 1)
 * - Add tasks that have maxFreq count
 * - Compare with total tasks (can't be less)
 * 
 * Step 4: Greedy simulation approach
 * - Always schedule most frequent remaining task
 * - Use priority queue to track frequencies
 * - Simulate the actual scheduling
 * 
 * APPROACHES:
 * 1. Mathematical Formula - O(n) time, O(1) space [OPTIMAL]
 * 2. Greedy with Priority Queue - O(n log k) time, O(k) space
 * 3. Greedy with Sorting - O(n log k) time, O(k) space
 */

/**
 * APPROACH 1: MATHEMATICAL FORMULA (OPTIMAL - RECOMMENDED)
 * =========================================================
 * 
 * INTUITION:
 * The task with maximum frequency determines the minimum time needed.
 * Think of it as creating "frames" separated by cooldown periods.
 * 
 * VISUALIZATION:
 * tasks = [A,A,A,B,B,B], n = 2
 * 
 * Max frequency = 3 (both A and B appear 3 times)
 * Create frames: [A _ _] [A _ _] [A]
 *                 ↑     ↑     ↑
 *                Frame1 Frame2 Last
 * 
 * Number of frames (excluding last): maxFreq - 1 = 2
 * Size of each frame: n + 1 = 3
 * Base intervals: (maxFreq - 1) * (n + 1) = 2 * 3 = 6
 * 
 * Tasks with max frequency: 2 (A and B)
 * Total: 6 + 2 = 8
 * 
 * But we have 6 total tasks, so answer is max(8, 6) = 8
 * 
 * FORMULA:
 * 1. Find max frequency: maxFreq
 * 2. Count tasks with max frequency: maxCount
 * 3. Calculate: (maxFreq - 1) * (n + 1) + maxCount
 * 4. Return max(calculated, tasks.length)
 * 
 * WHY max(calculated, tasks.length)?
 * If n is small or we have many different tasks, we might not need
 * any idle time. In that case, just execute all tasks back-to-back.
 * 
 * TIME: O(n) where n is number of tasks
 * SPACE: O(1) - only 26 possible task types
 */
class Solution {
    
    public int leastInterval(char[] tasks, int n) {
        // Count frequency of each task
        int[] frequencies = new int[26];
        for (char task : tasks) {
            frequencies[task - 'A']++;
        }
        
        // Find maximum frequency
        int maxFreq = 0;
        for (int freq : frequencies) {
            maxFreq = Math.max(maxFreq, freq);
        }
        
        // Count how many tasks have the maximum frequency
        int maxCount = 0;
        for (int freq : frequencies) {
            if (freq == maxFreq) {
                maxCount++;
            }
        }
        
        // Calculate minimum intervals needed
        // (maxFreq - 1) creates the "frames"
        // (n + 1) is the size of each frame
        // maxCount is added for the last occurrence of max frequency tasks
        int intervalsNeeded = (maxFreq - 1) * (n + 1) + maxCount;
        
        // If we have enough variety of tasks, we might not need idle time
        // In that case, just return the total number of tasks
        return Math.max(intervalsNeeded, tasks.length);
    }
}

/**
 * APPROACH 2: GREEDY WITH PRIORITY QUEUE (SIMULATION)
 * ====================================================
 * 
 * INTUITION:
 * Simulate the actual scheduling process:
 * 1. Always pick the most frequent remaining task
 * 2. After scheduling, put it in cooldown
 * 3. After n intervals, task becomes available again
 * 
 * This approach actually builds the schedule step by step.
 * 
 * ALGORITHM:
 * 1. Count task frequencies
 * 2. Use max heap to always get most frequent task
 * 3. In each cycle of (n+1) intervals:
 *    - Schedule up to (n+1) most frequent tasks
 *    - Decrease their frequencies
 *    - Add them back to heap if frequency > 0
 * 4. Continue until all tasks scheduled
 * 
 * TIME: O(n log 26) = O(n) where n is number of tasks
 * SPACE: O(26) = O(1)
 */
class SolutionGreedy {
    
    public int leastInterval(char[] tasks, int n) {
        // Count frequencies
        int[] frequencies = new int[26];
        for (char task : tasks) {
            frequencies[task - 'A']++;
        }
        
        // Max heap by frequency
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> b - a);
        for (int freq : frequencies) {
            if (freq > 0) {
                maxHeap.offer(freq);
            }
        }
        
        int intervals = 0;
        
        // Process tasks in cycles
        while (!maxHeap.isEmpty()) {
            List<Integer> temp = new ArrayList<>();
            
            // Schedule up to (n + 1) tasks in this cycle
            for (int i = 0; i <= n; i++) {
                if (!maxHeap.isEmpty()) {
                    int freq = maxHeap.poll();
                    if (freq > 1) {
                        temp.add(freq - 1);
                    }
                    intervals++;
                } else if (!temp.isEmpty()) {
                    // Need idle time if there are more tasks to schedule
                    intervals++;
                }
            }
            
            // Add remaining tasks back to heap
            for (int freq : temp) {
                maxHeap.offer(freq);
            }
        }
        
        return intervals;
    }
}

/**
 * APPROACH 3: GREEDY WITH COOLDOWN QUEUE
 * =======================================
 * 
 * More intuitive simulation with explicit cooldown tracking
 */
class SolutionCooldown {
    
    public int leastInterval(char[] tasks, int n) {
        // Count frequencies
        int[] frequencies = new int[26];
        for (char task : tasks) {
            frequencies[task - 'A']++;
        }
        
        // Max heap
        PriorityQueue<Integer> available = new PriorityQueue<>((a, b) -> b - a);
        for (int freq : frequencies) {
            if (freq > 0) {
                available.offer(freq);
            }
        }
        
        // Queue to track tasks in cooldown: [frequency, availableTime]
        Queue<int[]> cooldown = new LinkedList<>();
        
        int time = 0;
        
        while (!available.isEmpty() || !cooldown.isEmpty()) {
            time++;
            
            // Check if any task finished cooldown
            if (!cooldown.isEmpty() && cooldown.peek()[1] == time) {
                available.offer(cooldown.poll()[0]);
            }
            
            // Schedule most frequent available task
            if (!available.isEmpty()) {
                int freq = available.poll();
                if (freq > 1) {
                    // Put in cooldown, available at time + n + 1
                    cooldown.offer(new int[]{freq - 1, time + n + 1});
                }
            }
            // If no task available, this is idle time (still counts)
        }
        
        return time;
    }
}

/**
 * APPROACH 4: DETAILED MATHEMATICAL EXPLANATION
 * ==============================================
 * 
 * With extensive comments for understanding
 */
class SolutionMathDetailed {
    
    public int leastInterval(char[] tasks, int n) {
        // Step 1: Count frequency of each task type
        int[] freq = new int[26];
        int maxFrequency = 0;
        
        for (char task : tasks) {
            freq[task - 'A']++;
            maxFrequency = Math.max(maxFrequency, freq[task - 'A']);
        }
        
        // Step 2: Count how many tasks have the maximum frequency
        int numberOfMaxFreqTasks = 0;
        for (int f : freq) {
            if (f == maxFrequency) {
                numberOfMaxFreqTasks++;
            }
        }
        
        /*
         * Step 3: Calculate minimum intervals
         * 
         * Imagine the most frequent task creates a "skeleton":
         * If task A appears 4 times and n = 2:
         * 
         * A _ _ A _ _ A _ _ A
         * 
         * We have (maxFreq - 1) = 3 groups of size (n + 1) = 3
         * Plus the final A
         * 
         * If we have k tasks with max frequency:
         * A _ _ A _ _ A _ _ A
         * B     B     B     B
         * 
         * Total = (maxFreq - 1) * (n + 1) + k
         * 
         * However, if we have LOTS of different tasks, we might
         * fill all the slots and not need any idle time.
         * In that case, answer is just tasks.length
         */
        
        int partCount = maxFrequency - 1;  // Number of "groups" excluding last
        int partLength = n + 1;            // Size of each group
        int emptySlots = partCount * partLength;  // Total slots in groups
        int tasksInLastGroup = numberOfMaxFreqTasks;  // Tasks in final position
        
        int answer = emptySlots + tasksInLastGroup;
        
        // Can't be less than total number of tasks
        return Math.max(answer, tasks.length);
    }
}

/**
 * APPROACH 5: GREEDY WITH ARRAY SORTING
 * ======================================
 * 
 * Alternative greedy without priority queue
 */
class SolutionSort {
    
    public int leastInterval(char[] tasks, int n) {
        int[] frequencies = new int[26];
        for (char task : tasks) {
            frequencies[task - 'A']++;
        }
        
        int time = 0;
        
        while (true) {
            // Sort to get most frequent tasks first
            Arrays.sort(frequencies);
            
            // If max frequency is 0, we're done
            if (frequencies[25] == 0) {
                break;
            }
            
            // Schedule up to (n + 1) tasks
            int tasksScheduled = 0;
            for (int i = 25; i >= 0 && tasksScheduled <= n; i--) {
                if (frequencies[i] > 0) {
                    frequencies[i]--;
                    tasksScheduled++;
                }
            }
            
            // Add intervals
            if (frequencies[25] > 0) {
                // Still have tasks remaining, so full cycle
                time += n + 1;
            } else {
                // Last cycle, only count actual tasks
                time += tasksScheduled;
            }
        }
        
        return time;
    }
}

/**
 * TEST CASES
 * ==========
 */
class TestTaskScheduler {
    
    public static void runTest(String testName, char[] tasks, int n, int expected) {
        System.out.println("\n" + testName);
        System.out.println("Tasks: " + Arrays.toString(tasks));
        System.out.println("Cooldown (n): " + n);
        
        Solution sol = new Solution();
        int result = sol.leastInterval(tasks, n);
        
        System.out.println("Result: " + result);
        System.out.println("Expected: " + expected);
        System.out.println("Status: " + (result == expected ? "✓ PASS" : "✗ FAIL"));
        
        // Show the logic
        int[] freq = new int[26];
        for (char task : tasks) {
            freq[task - 'A']++;
        }
        
        int maxFreq = 0;
        for (int f : freq) {
            maxFreq = Math.max(maxFreq, f);
        }
        
        int maxCount = 0;
        for (int f : freq) {
            if (f == maxFreq) maxCount++;
        }
        
        System.out.println("Max Frequency: " + maxFreq);
        System.out.println("Tasks with Max Frequency: " + maxCount);
        System.out.println("Formula: (" + maxFreq + "-1)*(" + n + "+1)+" + maxCount + " = " 
                          + ((maxFreq - 1) * (n + 1) + maxCount));
        System.out.println("Total tasks: " + tasks.length);
    }
    
    public static void main(String[] args) {
        System.out.println("=== TASK SCHEDULER - COMPREHENSIVE TESTING ===");
        
        // Test Case 1: Basic example
        runTest(
            "Test 1: Basic case",
            new char[]{'A','A','A','B','B','B'},
            2,
            8
        );
        // A -> B -> idle -> A -> B -> idle -> A -> B
        
        // Test Case 2: No idle time needed
        runTest(
            "Test 2: Enough variety",
            new char[]{'A','A','A','B','B','B','C','C','C','D','D','D'},
            2,
            12
        );
        // A -> B -> C -> A -> B -> C -> A -> B -> C -> D -> D -> D
        
        // Test Case 3: n = 0 (no cooldown)
        runTest(
            "Test 3: No cooldown",
            new char[]{'A','A','A','B','B','B'},
            0,
            6
        );
        
        // Test Case 4: Large n
        runTest(
            "Test 4: Large cooldown",
            new char[]{'A','A','A','B','B','B'},
            50,
            104
        );
        
        // Test Case 5: Single task type
        runTest(
            "Test 5: All same tasks",
            new char[]{'A','A','A','A','A'},
            2,
            13
        );
        // A -> idle -> idle -> A -> idle -> idle -> A -> idle -> idle -> A -> idle -> idle -> A
        
        // Test Case 6: All different tasks
        runTest(
            "Test 6: All different",
            new char[]{'A','B','C','D','E','F'},
            2,
            6
        );
        
        // Test Case 7: Two with high frequency
        runTest(
            "Test 7: Multiple max frequency",
            new char[]{'A','A','A','A','B','B','B','B'},
            2,
            10
        );
        
        // Test Case 8: Example from LeetCode
        runTest(
            "Test 8: LeetCode example",
            new char[]{'A','A','A','A','A','A','B','C','D','E','F','G'},
            2,
            16
        );
        
        // Demonstrate the mathematical formula
        System.out.println("\n\n=== FORMULA EXPLANATION ===");
        System.out.println("For tasks=['A','A','A','B','B','B'], n=2:");
        System.out.println("\nVisualization:");
        System.out.println("[A _ _] [A _ _] [A]");
        System.out.println(" ↑ ↑ ↑   ↑ ↑ ↑   ↑");
        System.out.println("Frame1  Frame2  Last");
        System.out.println("\nMaxFreq = 3");
        System.out.println("MaxCount = 2 (both A and B)");
        System.out.println("\nFrames = maxFreq - 1 = 2");
        System.out.println("Frame size = n + 1 = 3");
        System.out.println("Base intervals = 2 * 3 = 6");
        System.out.println("Add maxCount = 6 + 2 = 8");
        System.out.println("\nSchedule: A B idle A B idle A B");
        System.out.println("Total: 8 intervals");
    }
}

/**
 * COMPLEXITY ANALYSIS
 * ===================
 * 
 * Approach 1 - Mathematical Formula (OPTIMAL):
 * Time:  O(n) where n = number of tasks
 *        - Count frequencies: O(n)
 *        - Find max and count: O(26) = O(1)
 * Space: O(1) - fixed size array of 26
 * 
 * Approach 2 - Greedy with Priority Queue:
 * Time:  O(n log k) where k = unique task types (max 26)
 *        = O(n) since k is constant
 * Space: O(k) = O(1)
 * 
 * Approach 3 - Greedy with Cooldown Queue:
 * Time:  O(n)
 * Space: O(k) = O(1)
 * 
 * Approach 4 - Greedy with Sorting:
 * Time:  O(n * k log k) where k = 26
 *        = O(26n log 26) = O(n)
 * Space: O(1)
 * 
 * 
 * INTERVIEW STRATEGY
 * ==================
 * 
 * 1. CLARIFY THE PROBLEM:
 *    Q: "Can I execute tasks in any order?"
 *    A: Yes, only constraint is n intervals between same task
 *    
 *    Q: "What counts as an interval - execution or wait?"
 *    A: Both execution and idle time count as intervals
 *    
 *    Q: "Is there a limit on task types?"
 *    A: Only A-Z (26 types)
 *    
 *    Q: "Can n be 0?"
 *    A: Yes, means no cooldown needed
 * 
 * 2. START WITH EXAMPLES:
 *    "Let me work through an example:
 *     tasks = [A,A,A,B,B,B], n = 2
 *     
 *     A appears 3 times, need 2 intervals between each A
 *     Layout: A _ _ A _ _ A
 *     
 *     Fill with B: A B _ A B _ A B
 *     
 *     Total: 8 intervals (including 2 idle)"
 * 
 * 3. IDENTIFY THE PATTERN:
 *    "The key insight: The most frequent task determines structure.
 *     
 *     If max frequency is 3 and n = 2:
 *     - Create 2 'frames' of size 3
 *     - Plus final occurrence
 *     - Other tasks fill the gaps
 *     
 *     Formula: (maxFreq - 1) * (n + 1) + maxCount"
 * 
 * 4. EXPLAIN THE SOLUTION:
 *    "I'll use a mathematical formula:
 *     
 *     1. Find the maximum frequency
 *     2. Count tasks with max frequency
 *     3. Calculate: (maxFreq - 1) * (n + 1) + maxCount
 *     4. Return max(calculated, total tasks)
 *     
 *     The max() handles cases where we have enough task variety
 *     that no idle time is needed."
 * 
 * 5. WALK THROUGH THE MATH:
 *    tasks = [A,A,A,B,B,B], n = 2
 *    
 *    maxFreq = 3
 *    maxCount = 2 (A and B both appear 3 times)
 *    
 *    (3-1) * (2+1) + 2 = 2 * 3 + 2 = 8
 *    
 *    Total tasks = 6
 *    Answer = max(8, 6) = 8
 * 
 * 6. DISCUSS ALTERNATIVE (if time):
 *    "There's also a greedy simulation approach:
 *     - Use max heap to track frequencies
 *     - Always schedule most frequent task
 *     - Put scheduled tasks in cooldown
 *     
 *     This is O(n log k) but more intuitive for some."
 * 
 * 7. EDGE CASES:
 *    ✓ n = 0 (no cooldown needed)
 *    ✓ All same tasks (maximum idle time)
 *    ✓ All different tasks (no idle time)
 *    ✓ Single task
 *    ✓ Multiple tasks with same max frequency
 *    ✓ Large n value
 * 
 * 8. WHY THE FORMULA WORKS:
 *    "The most frequent task creates a 'skeleton' schedule.
 *     
 *     Other tasks fit into the gaps. If not enough tasks
 *     to fill gaps, we need idle time.
 *     
 *     If more than enough tasks, we can schedule without
 *     idle time, so answer is just total task count."
 * 
 * 9. PROVE CORRECTNESS:
 *    "Why is this optimal?
 *     
 *     1. We MUST have at least (maxFreq - 1) * n idle slots
 *        between repetitions of the most frequent task
 *     
 *     2. We can't do better than this structure
 *     
 *     3. We use all remaining tasks to fill gaps
 *     
 *     4. If gaps overflow, we just append tasks (no idle)"
 * 
 * 10. COMMON MISTAKES:
 *     ✗ Forgetting the max(formula, tasks.length)
 *     ✗ Off-by-one: using maxFreq instead of (maxFreq - 1)
 *     ✗ Not counting all tasks with max frequency
 *     ✗ Wrong formula: forgetting the "+ maxCount"
 *     ✗ Trying to simulate actual scheduling (overcomplicated)
 * 
 * FOLLOW-UP QUESTIONS:
 * ====================
 * 
 * Q: "What if tasks have different execution times?"
 * A: Would need different approach, likely dynamic programming
 *    or more complex greedy with time tracking
 * 
 * Q: "What if we want the actual schedule, not just count?"
 * A: Would need simulation approach (greedy with heap)
 *    to build actual task sequence
 * 
 * Q: "What if tasks have priorities?"
 * A: Would need to incorporate priority in scheduling logic,
 *    possibly multi-level priority queue
 * 
 * Q: "What if we have multiple CPUs?"
 * A: Parallel scheduling problem, much more complex
 *    Would need to track state of each CPU
 * 
 * Q: "How to optimize for real-time systems?"
 * A: Would need to consider deadline constraints,
 *    preemption, and real-time scheduling algorithms
 * 
 * RECOMMENDED SOLUTION:
 * Approach 1 (Mathematical Formula) is optimal and elegant.
 * It demonstrates strong analytical thinking and pattern
 * recognition. Start with examples, build intuition, then
 * present the formula with clear explanation.
 */

/*
 * The Logic: Thinking in "Frames"The bottleneck is always the task that occurs
 * most frequently. Let's say the task with the maximum frequency is 'A', and it
 * appears maxFreq times.
 * To satisfy the $n$ cooling period, we must place at
 * least $n$ gaps between each 'A'. This creates a "frame" structure:
 * Create Slots: We have maxFreq - 1 complete frames.
 * Frame Size: Each frame has a size of n + 1 (the task itself plus the cooling
 * period).
 * The Last Row: After the last 'A', we don't necessarily need cooling slots; we
 * just need to append any
 * other tasks that also have the same maxFreq.
 */
