import java.util.Arrays;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * TASK SCHEDULER (LeetCode 621) - COMPREHENSIVE GUIDE
 * 
 * Problem: Schedule CPU tasks with cooling period constraint.
 * Each task type must wait at least n intervals before repeating.
 * Return minimum total intervals (including idle time).
 * 
 * DIFFICULTY: Medium
 * OPTIMAL TIME: O(N) where N = number of tasks
 * OPTIMAL SPACE: O(1) - fixed 26 task types
 * 
 * KEY INSIGHT: The task with highest frequency determines the minimum time!
 * If we have many high-frequency tasks, we need to interleave them.
 * If we don't have enough variety, we need idle time.
 * 
 * CRITICAL CONCEPTS:
 * 1. Mathematical Formula (most elegant)
 * 2. Max-Heap Simulation (most intuitive)
 * 3. Greedy Scheduling
 * 4. Frequency Analysis
 * 
 * COMPANIES: Amazon, Microsoft, Google, Facebook, Bloomberg, Oracle
 * 
 * RELATED PROBLEMS:
 * - Reorganize String (LeetCode 767)
 * - Rearrange String k Distance Apart (LeetCode 358)
 * - CPU Scheduling algorithms
 * 
 * EDGE CASES:
 * 1. n = 0 (no cooling needed)
 * 2. All tasks are different
 * 3. All tasks are the same
 * 4. Multiple tasks with max frequency
 */
class TaskScheduler {

    // ========================================================================
    // APPROACH 1: MATHEMATICAL FORMULA (OPTIMAL - MOST ELEGANT)
    // ========================================================================
    /**
     * Use math to calculate minimum intervals without simulation.
     * 
     * KEY INSIGHT:
     * The task with maximum frequency determines the structure.
     * 
     * VISUALIZATION (max_freq = 3, n = 2):
     * Round 1: A _ _ 
     * Round 2: A _ _
     * Round 3: A
     * 
     * We have (max_freq - 1) complete rounds.
     * Each round has (n + 1) slots.
     * Plus one final round for last occurrence of max_freq tasks.
     * 
     * FORMULA:
     * If we have enough tasks to fill gaps:
     *    result = total_tasks
     * 
     * If not enough tasks:
     *    result = (max_freq - 1) * (n + 1) + count_of_max_freq_tasks
     * 
     * EXAMPLE: tasks = ["A","A","A","B","B","B"], n = 2
     * max_freq = 3
     * count_max = 2 (both A and B have freq 3)
     * 
     * Minimum structure:
     * A B _ A B _ A B
     * 
     * Formula: (3-1) * (2+1) + 2 = 2*3 + 2 = 8 ✓
     * 
     * TIME: O(N) - scan array once for frequencies
     * SPACE: O(1) - fixed 26 letters
     * 
     * INTERVIEW FAVORITE:
     * Most elegant solution. Shows deep problem understanding.
     */
    public int leastInterval(char[] tasks, int n) {
        // Edge case: no cooling needed
        if (n == 0) {
            return tasks.length;
        }
        
        // Count frequency of each task
        int[] freq = new int[26];
        for (char task : tasks) {
            freq[task - 'A']++;
        }
        
        // Find maximum frequency
        int maxFreq = 0;
        for (int f : freq) {
            maxFreq = Math.max(maxFreq, f);
        }
        
        // Count how many tasks have max frequency
        int countMax = 0;
        for (int f : freq) {
            if (f == maxFreq) {
                countMax++;
            }
        }
        
        // Calculate minimum intervals
        // (maxFreq - 1): number of complete rounds
        // (n + 1): slots per round
        // countMax: tasks in final round
        int minIntervals = (maxFreq - 1) * (n + 1) + countMax;
        
        // If we have many different tasks, we might not need any idle time
        // In that case, just return total tasks
        return Math.max(minIntervals, tasks.length);
    }

    // ========================================================================
    // APPROACH 2: MAX-HEAP SIMULATION (MOST INTUITIVE)
    // ========================================================================
    /**
     * Simulate the actual scheduling using max-heap.
     * 
     * ALGORITHM:
     * 1. Count task frequencies
     * 2. Use max-heap to always schedule most frequent task available
     * 3. Use queue to track cooling tasks
     * 4. After n intervals, tasks from queue can be rescheduled
     * 
     * VISUALIZATION (tasks=["A","A","A","B","B","B"], n=2):
     * Time 0: Schedule A (freq 3→2), cooling=[A:2 at time 3]
     * Time 1: Schedule B (freq 3→2), cooling=[A:2 at 3, B:2 at 4]
     * Time 2: IDLE (nothing available), cooling=[A:2 at 3, B:2 at 4]
     * Time 3: A available again, schedule A (2→1), cooling=[B:2 at 4, A:1 at 6]
     * Time 4: B available, schedule B (2→1), cooling=[A:1 at 6, B:1 at 7]
     * Time 5: IDLE
     * Time 6: Schedule A (1→0), cooling=[B:1 at 7]
     * Time 7: Schedule B (1→0)
     * Total: 8 intervals
     * 
     * TIME: O(N) - each task processed once
     * SPACE: O(26) = O(1) - fixed task types
     * 
     * PROS:
     * + Intuitive and easy to understand
     * + Actually simulates the process
     * + Good for explaining in interviews
     * 
     * CONS:
     * - More code than formula
     * - Slightly slower in practice
     */
    public int leastIntervalSimulation(char[] tasks, int n) {
        // Count frequencies
        int[] freq = new int[26];
        for (char task : tasks) {
            freq[task - 'A']++;
        }
        
        // Max-heap of frequencies (not characters)
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> b - a);
        for (int f : freq) {
            if (f > 0) {
                maxHeap.offer(f);
            }
        }
        
        // Queue to track cooling tasks: [frequency, available_time]
        Queue<int[]> cooling = new LinkedList<>();
        
        int time = 0;
        
        // Continue until heap and cooling queue are empty
        while (!maxHeap.isEmpty() || !cooling.isEmpty()) {
            time++;
            
            if (!maxHeap.isEmpty()) {
                // Schedule most frequent available task
                int freq_count = maxHeap.poll();
                freq_count--; // Task executed once
                
                if (freq_count > 0) {
                    // This task needs to cool down
                    // Will be available at time + n + 1
                    cooling.offer(new int[]{freq_count, time + n});
                }
            }
            // else: IDLE time
            
            // Check if any task finished cooling
            if (!cooling.isEmpty() && cooling.peek()[1] == time) {
                maxHeap.offer(cooling.poll()[0]);
            }
        }
        
        return time;
    }

    // ========================================================================
    // APPROACH 3: GREEDY WITH COUNTING (ALTERNATIVE SIMULATION)
    // ========================================================================
    /**
     * Greedy approach: fill rounds with available tasks.
     * 
     * ALGORITHM:
     * 1. Count frequencies
     * 2. In each round of (n+1) slots:
     *    - Schedule up to (n+1) most frequent tasks
     *    - Decrement their frequencies
     * 3. Continue until all tasks scheduled
     * 
     * TIME: O(N) - each task counted once
     * SPACE: O(1)
     */
    public int leastIntervalGreedy(char[] tasks, int n) {
        // Count frequencies
        int[] freq = new int[26];
        for (char task : tasks) {
            freq[task - 'A']++;
        }
        
        int time = 0;
        
        while (true) {
            // Sort to get highest frequencies first
            Arrays.sort(freq);
            
            // Check if all tasks done
            if (freq[25] == 0) {
                break;
            }
            
            // Schedule up to n+1 tasks in this round
            int i = 0;
            while (i <= n) {
                if (freq[25] == 0) {
                    // All tasks completed
                    break;
                }
                
                // Schedule highest frequency task available
                if (i < 26 && freq[25 - i] > 0) {
                    freq[25 - i]--;
                }
                
                time++;
                i++;
            }
        }
        
        return time;
    }

    // ========================================================================
    // APPROACH 4: OPTIMIZED FORMULA WITH EXPLANATION
    // ========================================================================
    /**
     * Enhanced version of formula approach with detailed explanation.
     * 
     * KEY OBSERVATIONS:
     * 
     * 1. Most frequent task determines minimum structure
     * 2. We need (max_freq - 1) complete cycles
     * 3. Each cycle has (n + 1) slots
     * 4. Final cycle has tasks with max frequency
     * 
     * EXAMPLE BREAKDOWN:
     * tasks = ["A","A","A","B","B","B"], n = 2
     * 
     * Max frequency = 3
     * Tasks with max freq = 2 (A and B)
     * 
     * Structure:
     * [Cycle 1: A B _] [Cycle 2: A B _] [Final: A B]
     * 
     * Calculation:
     * - Complete cycles: max_freq - 1 = 2
     * - Slots per cycle: n + 1 = 3
     * - Final tasks: countMax = 2
     * - Total: 2 * 3 + 2 = 8
     * 
     * WHY max(formula, tasks.length)?
     * If we have many different tasks, we might not need idle time.
     * Example: tasks = ["A","B","C","D","E","F"], n = 2
     * We can just schedule all 6 tasks: A B C D E F
     * No idle needed!
     * 
     * TIME: O(N)
     * SPACE: O(1)
     */
    public int leastIntervalOptimized(char[] tasks, int n) {
        if (n == 0) return tasks.length;
        
        // Frequency count
        int[] freq = new int[26];
        int maxFreq = 0;
        
        for (char task : tasks) {
            freq[task - 'A']++;
            maxFreq = Math.max(maxFreq, freq[task - 'A']);
        }
        
        // Count tasks with max frequency
        int maxCount = 0;
        for (int f : freq) {
            if (f == maxFreq) {
                maxCount++;
            }
        }
        
        // Calculate minimum intervals needed
        // Case 1: Need idle time
        int withIdle = (maxFreq - 1) * (n + 1) + maxCount;
        
        // Case 2: No idle needed (enough task variety)
        int withoutIdle = tasks.length;
        
        return Math.max(withIdle, withoutIdle);
    }

    // ========================================================================
    // COMMON MISTAKES & DEBUGGING TIPS
    // ========================================================================
    /**
     * CRITICAL MISTAKES:
     * 
     * 1. WRONG: Not considering tasks with max frequency in final round
     *    RIGHT: Add maxCount to formula
     *    Example: ["A","A","A","B","B","B"], n=2
     *    Wrong: (3-1)*(2+1) = 6
     *    Right: (3-1)*(2+1) + 2 = 8
     * 
     * 2. WRONG: Forgetting Math.max(formula, tasks.length)
     *    RIGHT: Return max of formula and total tasks
     *    Example: Many different tasks need no idle time
     * 
     * 3. WRONG: Using (maxFreq) instead of (maxFreq - 1) for cycles
     *    RIGHT: Last occurrence doesn't need cooling period after it
     * 
     * 4. WRONG: Counting only one task with max frequency
     *    RIGHT: Multiple tasks can have same max frequency
     * 
     * 5. WRONG: Not handling n = 0 case
     *    RIGHT: When n=0, answer is just tasks.length
     * 
     * 6. WRONG: Thinking we need to track actual scheduling order
     *    RIGHT: We only need to count intervals, not schedule
     * 
     * 7. WRONG: Confusing cooling period n with n+1 slots
     *    RIGHT: Cooling period n means n+1 total slots per cycle
     *    (current task + n other tasks/idle)
     * 
     * 8. WRONG: Not considering all 26 possible task types
     *    RIGHT: Array size 26 for 'A' to 'Z'
     * 
     * DEBUGGING TIPS:
     * - Visualize with small examples
     * - Draw out the cycles manually
     * - Check formula with edge cases
     * - Verify max frequency calculation
     * - Test with all same tasks
     * - Test with all different tasks
     */

    // ========================================================================
    // INTERVIEW COMMUNICATION STRATEGY
    // ========================================================================
    /**
     * OPTIMAL INTERVIEW FLOW (25-30 minutes):
     * 
     * PHASE 1: CLARIFICATION (2-3 min)
     * Questions to ask:
     * - "Are tasks limited to A-Z?" (YES)
     * - "Can n be 0?" (YES - no cooling needed)
     * - "Do we need to return the actual schedule or just count?" (Just count)
     * - "Can we have more tasks than 26 types?" (NO - 26 max)
     * 
     * PHASE 2: EXAMPLES (3-5 min)
     * Work through example visually:
     * tasks = ["A","A","A","B","B","B"], n = 2
     * 
     * "Let me visualize this:
     * A has frequency 3, B has frequency 3
     * Need to space each task by at least 2 intervals
     * 
     * Optimal schedule:
     * Time: 0  1  2  3  4  5  6  7
     * Task: A  B  _  A  B  _  A  B
     * 
     * Total: 8 intervals"
     * 
     * PHASE 3: APPROACH DISCUSSION (3-5 min)
     * 
     * Naive approach:
     * "Could simulate actual scheduling with queue/heap.
     * Track cooling times, schedule greedily.
     * This works but involves complex simulation."
     * 
     * Better approach:
     * "Key insight: The most frequent task determines structure!
     * If task A appears 5 times with n=2:
     * A _ _ A _ _ A _ _ A _ _ A
     * 
     * Formula:
     * - Need (max_freq - 1) complete rounds
     * - Each round has (n + 1) slots
     * - Plus final round with all max-freq tasks
     * 
     * Result: (max_freq - 1) * (n + 1) + count_of_max_freq_tasks"
     * 
     * PHASE 4: COMPLEXITY ANALYSIS (1 min)
     * - Time: O(N) - scan array for frequencies
     * - Space: O(1) - fixed 26 task types
     * 
     * PHASE 5: CODING (12-15 min)
     * - Count frequencies
     * - Find max frequency and count
     * - Apply formula
     * - Return max of formula and tasks.length
     * 
     * PHASE 6: TESTING (3-5 min)
     * Test cases:
     * 1. Given examples
     * 2. n = 0: ["A","A","B","B"] → 4 (no cooling)
     * 3. All same: ["A","A","A"], n=2 → 7 (A _ _ A _ _ A)
     * 4. All different: ["A","B","C"], n=2 → 3
     * 5. Multiple max: ["A","A","B","B"], n=1 → 4
     * 
     * PHASE 7: EDGE CASES (if time)
     * - Single task: ["A"], n=5 → 1
     * - No tasks: [], n=2 → 0
     * - Large n: ["A","A"], n=100 → 201
     */

    // ========================================================================
    // KEY INSIGHTS & PATTERNS
    // ========================================================================
    /**
     * PATTERN RECOGNITION:
     * 
     * 1. FREQUENCY-BASED PROBLEMS:
     *    - Count frequencies first
     *    - Max frequency often determines answer
     *    - Examples: Task Scheduler, Reorganize String
     * 
     * 2. GREEDY SCHEDULING:
     *    - Always schedule most frequent available task
     *    - Minimizes future constraints
     *    - Works because of cooling period
     * 
     * 3. MATHEMATICAL OPTIMIZATION:
     *    - Sometimes simulation can be replaced by formula
     *    - Analyze structure to find pattern
     *    - Much faster and cleaner
     * 
     * 4. WHY FORMULA WORKS:
     *    
     *    Most frequent task creates "frame":
     *    T _ _ _ T _ _ _ T _ _ _ T
     *    
     *    If cooling period is n:
     *    - Between each T, we have n slots
     *    - We have (freq - 1) gaps
     *    - Each gap has (n + 1) total positions
     *    - Final position has all max-freq tasks
     *    
     *    If we have enough variety:
     *    - Can fill all gaps without idle
     *    - Just return tasks.length
     * 
     * 5. IDLE TIME CALCULATION:
     *    idle_slots = max(0, (maxFreq-1)*(n+1) + maxCount - tasks.length)
     *    total_time = tasks.length + idle_slots
     *    
     *    Simplified to:
     *    total_time = max(formula, tasks.length)
     */

    // ========================================================================
    // RELATED PROBLEMS & VARIATIONS
    // ========================================================================
    /**
     * SIMILAR LEETCODE PROBLEMS:
     * 
     * 1. LeetCode 767 - Reorganize String
     *    - Same idea but with strings
     *    - Return string or empty if impossible
     *    - n = 1 (adjacent chars must differ)
     * 
     * 2. LeetCode 358 - Rearrange String k Distance Apart
     *    - Generalized version with distance k
     *    - Must return actual string
     * 
     * 3. LeetCode 1054 - Distant Barcodes
     *    - Array version with adjacent constraint
     *    - No duplicates should be adjacent
     * 
     * 4. LeetCode 984 - String Without AAA or BBB
     *    - Build string with constraints
     *    - No three consecutive same
     * 
     * VARIATIONS:
     * 
     * 1. RETURN ACTUAL SCHEDULE:
     *    - Can't just use formula
     *    - Need heap simulation
     *    - Track actual order
     * 
     * 2. DIFFERENT COOLING FOR DIFFERENT TASKS:
     *    - Store cooling period per task type
     *    - More complex tracking needed
     * 
     * 3. PRIORITY TASKS:
     *    - Some tasks must finish earlier
     *    - Schedule high priority first
     * 
     * 4. MULTIPLE CPUS:
     *    - Can run k tasks in parallel
     *    - Different scheduling algorithm
     * 
     * 5. VARIABLE TASK DURATION:
     *    - Tasks take different time
     *    - Cooling based on finish time
     */

    // ========================================================================
    // COMPLEXITY COMPARISON TABLE
    // ========================================================================
    /**
     * ┌────────────────────┬──────────────┬──────────────┬─────────────┐
     * │ Approach           │ Time         │ Space        │ Interview   │
     * ├────────────────────┼──────────────┼──────────────┼─────────────┤
     * │ Mathematical       │ O(N)         │ O(1)         │ ⭐⭐⭐⭐⭐  │
     * │ Max-Heap Sim       │ O(N)         │ O(1)         │ ⭐⭐⭐⭐☆  │
     * │ Greedy Counting    │ O(N)         │ O(1)         │ ⭐⭐⭐☆☆  │
     * │ Formula Enhanced   │ O(N)         │ O(1)         │ ⭐⭐⭐⭐⭐  │
     * └────────────────────┴──────────────┴──────────────┴─────────────┘
     * 
     * WHERE:
     * - N = number of tasks
     * - All use O(1) space (26 task types max)
     * 
     * RECOMMENDATION:
     * 
     * Default choice: Mathematical Formula
     * ✓ Most elegant and efficient
     * ✓ Shows deep understanding
     * ✓ Minimal code
     * ✓ No simulation overhead
     * 
     * Use Heap Simulation when:
     * ✓ Need to return actual schedule
     * ✓ Want to explain process step-by-step
     * ✓ Interviewer prefers simulation
     * 
     * Avoid Greedy Counting:
     * ✗ Sorting in each round is expensive
     * ✗ More complex than formula
     * ✗ Same time complexity but worse constant
     */

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================
    
    // Helper: Visualize schedule (for testing)
    public String visualizeSchedule(char[] tasks, int n) {
        if (tasks.length == 0) return "";
        
        int[] freq = new int[26];
        for (char task : tasks) {
            freq[task - 'A']++;
        }
        
        PriorityQueue<int[]> maxHeap = new PriorityQueue<>((a, b) -> b[1] - a[1]);
        for (int i = 0; i < 26; i++) {
            if (freq[i] > 0) {
                maxHeap.offer(new int[]{i, freq[i]});
            }
        }
        
        StringBuilder schedule = new StringBuilder();
        Queue<int[]> cooling = new LinkedList<>();
        int time = 0;
        
        while (!maxHeap.isEmpty() || !cooling.isEmpty()) {
            time++;
            
            if (!maxHeap.isEmpty()) {
                int[] task = maxHeap.poll();
                schedule.append((char)('A' + task[0]));
                task[1]--;
                
                if (task[1] > 0) {
                    cooling.offer(new int[]{task[0], task[1], time + n});
                }
            } else {
                schedule.append("idle");
            }
            
            if (!cooling.isEmpty() && cooling.peek()[2] == time) {
                int[] ready = cooling.poll();
                maxHeap.offer(new int[]{ready[0], ready[1]});
            }
            
            if (!maxHeap.isEmpty() || !cooling.isEmpty()) {
                schedule.append(" -> ");
            }
        }
        
        return schedule.toString();
    }

    // ========================================================================
    // TEST CASES & VALIDATION
    // ========================================================================
    public static void main(String[] args) {
        TaskScheduler solution = new TaskScheduler();
        
        System.out.println("=".repeat(70));
        System.out.println("TASK SCHEDULER - COMPREHENSIVE TEST SUITE");
        System.out.println("=".repeat(70));
        
        // Test Case 1: Example 1
        char[] tasks1 = {'A','A','A','B','B','B'};
        int n1 = 2;
        System.out.println("\nTest 1: Standard Example");
        System.out.println("Tasks: " + Arrays.toString(tasks1) + ", n = " + n1);
        System.out.println("Expected: 8");
        System.out.println("Formula:     " + solution.leastInterval(tasks1, n1));
        System.out.println("Simulation:  " + solution.leastIntervalSimulation(tasks1, n1));
        System.out.println("Schedule: " + solution.visualizeSchedule(tasks1, n1));
        
        // Test Case 2: Example 2
        char[] tasks2 = {'A','C','A','B','D','B'};
        int n2 = 1;
        System.out.println("\nTest 2: Enough Variety");
        System.out.println("Tasks: " + Arrays.toString(tasks2) + ", n = " + n2);
        System.out.println("Expected: 6");
        System.out.println("Result: " + solution.leastInterval(tasks2, n2));
        System.out.println("Schedule: " + solution.visualizeSchedule(tasks2, n2));
        
        // Test Case 3: Example 3
        char[] tasks3 = {'A','A','A','B','B','B'};
        int n3 = 3;
        System.out.println("\nTest 3: Large Cooling Period");
        System.out.println("Tasks: " + Arrays.toString(tasks3) + ", n = " + n3);
        System.out.println("Expected: 10");
        System.out.println("Result: " + solution.leastInterval(tasks3, n3));
        System.out.println("Schedule: " + solution.visualizeSchedule(tasks3, n3));
        
        // Test Case 4: n = 0
        char[] tasks4 = {'A','A','B','B'};
        int n4 = 0;
        System.out.println("\nTest 4: No Cooling (n=0)");
        System.out.println("Tasks: " + Arrays.toString(tasks4) + ", n = " + n4);
        System.out.println("Expected: 4");
        System.out.println("Result: " + solution.leastInterval(tasks4, n4));
        
        // Test Case 5: All same task
        char[] tasks5 = {'A','A','A','A','A'};
        int n5 = 2;
        System.out.println("\nTest 5: All Same Task");
        System.out.println("Tasks: " + Arrays.toString(tasks5) + ", n = " + n5);
        System.out.println("Expected: 13 (A _ _ A _ _ A _ _ A _ _ A)");
        System.out.println("Result: " + solution.leastInterval(tasks5, n5));
        System.out.println("Schedule: " + solution.visualizeSchedule(tasks5, n5));
        
        // Test Case 6: All different tasks
        char[] tasks6 = {'A','B','C','D','E','F'};
        int n6 = 2;
        System.out.println("\nTest 6: All Different Tasks");
        System.out.println("Tasks: " + Arrays.toString(tasks6) + ", n = " + n6);
        System.out.println("Expected: 6 (no idle needed)");
        System.out.println("Result: " + solution.leastInterval(tasks6, n6));
        
        // Test Case 7: Multiple with max frequency
        char[] tasks7 = {'A','A','A','B','B','B','C','C','C'};
        int n7 = 2;
        System.out.println("\nTest 7: Three Tasks with Same Max Frequency");
        System.out.println("Tasks: " + Arrays.toString(tasks7) + ", n = " + n7);
        System.out.println("Expected: 9");
        System.out.println("Result: " + solution.leastInterval(tasks7, n7));
        System.out.println("Schedule: " + solution.visualizeSchedule(tasks7, n7));
        
        // Test Case 8: Single task
        char[] tasks8 = {'A'};
        int n8 = 5;
        System.out.println("\nTest 8: Single Task");
        System.out.println("Tasks: " + Arrays.toString(tasks8) + ", n = " + n8);
        System.out.println("Expected: 1");
        System.out.println("Result: " + solution.leastInterval(tasks8, n8));
        
        // Compare all approaches
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMPARING ALL APPROACHES");
        System.out.println("=".repeat(70));
        
        char[] testTasks = {'A','A','A','B','B','B'};
        int testN = 2;
        
        System.out.println("Test: " + Arrays.toString(testTasks) + ", n = " + testN);
        System.out.println("Formula:        " + solution.leastInterval(testTasks, testN));
        System.out.println("Simulation:     " + solution.leastIntervalSimulation(testTasks, testN));
        System.out.println("Greedy:         " + solution.leastIntervalGreedy(testTasks, testN));
        System.out.println("Optimized:      " + solution.leastIntervalOptimized(testTasks, testN));
        
        System.out.println("\n" + "=".repeat(70));
    }
}

/**
 * ============================================================================
 * FINAL INTERVIEW CHECKLIST
 * ============================================================================
 * 
 * BEFORE CODING:
 * □ Clarified task types (A-Z only)
 * □ Confirmed n can be 0
 * □ Asked if need actual schedule or just count
 * □ Drew visualization of example
 * □ Explained formula approach
 * □ Stated time and space complexity
 * 
 * WHILE CODING:
 * □ Counted frequencies in O(N)
 * □ Found max frequency correctly
 * □ Counted tasks with max frequency
 * □ Applied formula: (maxFreq-1)*(n+1) + maxCount
 * □ Used Math.max with tasks.length
 * □ Handled n = 0 case
 * □ Used array size 26 for task types
 * 
 * AFTER CODING:
 * □ Traced through example step by step
 * □ Tested with n = 0
 * □ Tested with all same tasks
 * □ Tested with all different tasks
 * □ Verified formula correctness
 * □ Explained why Math.max is needed
 * 
 * KEY TALKING POINTS:
 * ✓ "Most frequent task determines minimum structure"
 * ✓ "Need (maxFreq-1) complete cycles of (n+1) slots each"
 * ✓ "Final cycle contains all tasks with max frequency"
 * ✓ "If enough variety, no idle time needed"
 * ✓ "Formula beats simulation - O(N) with minimal constants"
 * 
 * COMMON PITFALLS AVOIDED:
 * ✗ Using maxFreq instead of maxFreq-1 for cycles
 * ✗ Forgetting to count all tasks with max frequency
 * ✗ Not using Math.max for sufficient task variety
 * ✗ Confusing n (cooling) with n+1 (slots)
 * ✗ Not handling n = 0
 * 
 * ============================================================================
 */
