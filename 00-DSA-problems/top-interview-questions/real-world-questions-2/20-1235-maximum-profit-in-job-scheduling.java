import java.util.*;

/**
 * PROBLEM: Maximum Profit in Job Scheduling (LeetCode 1053)
 * 
 * INTERVIEW APPROACH - HOW TO THINK THROUGH THIS PROBLEM:
 * 
 * 1. RECOGNITION PHASE (First 30 seconds):
 *    - Keywords: "maximum profit", "no overlapping", "subset selection"
 *    - This screams DYNAMIC PROGRAMMING with optimization
 *    - Similar to: Weighted Interval Scheduling (classic CS problem)
 *    - Pattern: We need to make decisions - take job or skip job
 * 
 * 2. CLARIFICATION QUESTIONS TO ASK:
 *    - Can jobs have same start/end times? (Yes, based on problem)
 *    - Are arrays always same length? (Yes, implied)
 *    - Can profit be negative? (No, typically non-negative)
 *    - Is n bounded? (Yes, usually <= 50000)
 *    - Job ending at X allows starting at X? (Yes, stated in problem)
 * 
 * 3. THOUGHT PROCESS (Share this with interviewer):
 *    
 *    Step 1: "This feels like DP because we're making choices and have overlapping subproblems"
 *    
 *    Step 2: "Let me think about sorting... if I sort by end time, I can process jobs
 *             in order and make decisions: for each job, either take it or skip it"
 *    
 *    Step 3: "If I take a job ending at time X, I need to find the latest job that 
 *             ends at or before the start time of current job. This is a binary search!"
 *    
 *    Step 4: "DP State: dp[i] = max profit using jobs 0 to i"
 *             "Recurrence: dp[i] = max(dp[i-1], profit[i] + dp[j]) where j is latest 
 *              non-overlapping job"
 * 
 * 4. COMPLEXITY ANALYSIS (Always discuss before coding):
 *    - Time: O(n log n) for sorting + O(n log n) for DP with binary search = O(n log n)
 *    - Space: O(n) for DP array
 * 
 * 5. EDGE CASES TO MENTION:
 *    - Empty array
 *    - Single job
 *    - All jobs overlap
 *    - No jobs overlap
 *    - Jobs with same start/end times
 */


class Solution {
    
    /**
     * APPROACH 1: TOP-DOWN DP WITH MEMOIZATION (Most Intuitive for Interviews)
     * 
     * WHY THIS APPROACH FIRST IN INTERVIEWS:
     * - Easier to explain the recursive logic
     * - Natural transition from brute force
     * - Shows clear decision tree: "take or skip"
     * 
     * Time: O(n log n) - sorting + n recursive calls with binary search
     * Space: O(n) - recursion stack + memoization array
     */
    public int jobScheduling(int[] startTime, int[] endTime, int[] profit) {
        int n = startTime.length;
        
        // STEP 1: Create Job objects and sort by END time
        // WHY sort by end time? Because we want to process jobs in chronological order
        // of completion. This allows us to use binary search to find compatible jobs.
        Job[] jobs = new Job[n];
        for (int i = 0; i < n; i++) {
            jobs[i] = new Job(startTime[i], endTime[i], profit[i]);
        }
        
        // Sort by end time (if tied, by start time for consistency)
        Arrays.sort(jobs, (a, b) -> {
            if (a.end != b.end) return a.end - b.end;
            return a.start - b.start;
        });
        
        // STEP 2: Memoization array
        // dp[i] = maximum profit we can get considering jobs from index i to end
        int[] dp = new int[n];
        Arrays.fill(dp, -1);
        
        return dfs(jobs, 0, dp);
    }
    
    /**
     * DFS with memoization
     * @param jobs - sorted array of jobs by end time
     * @param index - current job we're considering
     * @param dp - memoization array
     * @return maximum profit from index to end
     */
    private int dfs(Job[] jobs, int index, int[] dp) {
        // BASE CASE: No more jobs to process
        if (index >= jobs.length) {
            return 0;
        }
        
        // MEMOIZATION: Already computed this subproblem
        if (dp[index] != -1) {
            return dp[index];
        }
        
        // DECISION 1: SKIP current job
        // Move to next job, don't take current profit
        int skipProfit = dfs(jobs, index + 1, dp);
        
        // DECISION 2: TAKE current job
        // Find next compatible job using binary search
        int nextCompatibleJob = findNextCompatibleJob(jobs, index);
        int takeProfit = jobs[index].profit + dfs(jobs, nextCompatibleJob, dp);
        
        // RECURRENCE RELATION: Take maximum of both decisions
        dp[index] = Math.max(skipProfit, takeProfit);
        
        return dp[index];
    }
    
    /**
     * CRITICAL HELPER: Binary Search for Next Compatible Job
     * 
     * WHY BINARY SEARCH?
     * - Jobs are sorted by end time
     * - We need to find the first job that starts >= current job's end time
     * - This is a classic "find first element >= target" binary search
     * 
     * NUANCE: We're searching for start time >= current end time
     * This is because a job ending at X allows starting at X (non-strict inequality)
     */
    private int findNextCompatibleJob(Job[] jobs, int currentIndex) {
        int currentEndTime = jobs[currentIndex].end;
        int left = currentIndex + 1;
        int right = jobs.length;
        
        // Binary search for leftmost job with start >= currentEndTime
        while (left < right) {
            int mid = left + (right - left) / 2;
            
            // If this job starts at or after current job ends, it's compatible
            // Look for earlier compatible jobs
            if (jobs[mid].start >= currentEndTime) {
                right = mid;
            } else {
                // This job overlaps, look for later jobs
                left = mid + 1;
            }
        }
        
        return left; // This could be jobs.length (no compatible job found)
    }
    
    /**
     * APPROACH 2: BOTTOM-UP DP (More Efficient, Show This After Top-Down)
     * 
     * WHY SHOW BOTH IN INTERVIEWS:
     * - Top-down is more intuitive
     * - Bottom-up shows optimization skills
     * - Bottom-up has better space complexity (no recursion stack)
     * 
     * Time: O(n log n)
     * Space: O(n)
     */
    public int jobSchedulingBottomUp(int[] startTime, int[] endTime, int[] profit) {
        int n = startTime.length;
        
        // Create and sort jobs
        Job[] jobs = new Job[n];
        for (int i = 0; i < n; i++) {
            jobs[i] = new Job(startTime[i], endTime[i], profit[i]);
        }
        Arrays.sort(jobs, (a, b) -> a.end - b.end);
        
        // dp[i] = maximum profit considering jobs 0 to i (inclusive)
        int[] dp = new int[n];
        
        // BASE CASE: First job
        dp[0] = jobs[0].profit;
        
        // ITERATION: Process each job
        for (int i = 1; i < n; i++) {
            // OPTION 1: Don't take current job, carry forward previous max
            int skipProfit = dp[i - 1];
            
            // OPTION 2: Take current job
            int takeProfit = jobs[i].profit;
            
            // Find latest non-overlapping job using binary search
            // Search in range [0, i-1] for job with end <= jobs[i].start
            int latestNonOverlapping = findLatestNonOverlapping(jobs, i);
            
            if (latestNonOverlapping != -1) {
                takeProfit += dp[latestNonOverlapping];
            }
            
            // Take maximum
            dp[i] = Math.max(skipProfit, takeProfit);
        }
        
        return dp[n - 1];
    }
    
    /**
     * Binary search for latest job that doesn't overlap with current job
     * We need: jobs[result].end <= jobs[currentIndex].start
     */
    private int findLatestNonOverlapping(Job[] jobs, int currentIndex) {
        int targetStart = jobs[currentIndex].start;
        int left = 0;
        int right = currentIndex - 1;
        int result = -1;
        
        // Find rightmost job with end <= targetStart
        while (left <= right) {
            int mid = left + (right - left) / 2;
            
            if (jobs[mid].end <= targetStart) {
                result = mid; // This job is compatible, but check if there's a later one
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return result;
    }
    
    /**
     * APPROACH 3: OPTIMIZED WITH TREEMAP (Advanced - Mention If Time Permits)
     * 
     * WHEN TO USE:
     * - If you need to handle dynamic job insertions
     * - If interviewer asks for space optimization variations
     * 
     * Time: O(n log n)
     * Space: O(n)
     */
    public int jobSchedulingTreeMap(int[] startTime, int[] endTime, int[] profit) {
        int n = startTime.length;
        
        // Create jobs with index to maintain original order reference
        int[][] jobs = new int[n][3]; // [start, end, profit]
        for (int i = 0; i < n; i++) {
            jobs[i] = new int[]{startTime[i], endTime[i], profit[i]};
        }
        
        // Sort by start time (different approach)
        Arrays.sort(jobs, (a, b) -> a[0] - b[0]);
        
        // TreeMap: key = end time, value = max profit achievable up to that end time
        TreeMap<Integer, Integer> dp = new TreeMap<>();
        dp.put(0, 0); // Base case: at time 0, profit is 0
        
        for (int[] job : jobs) {
            int start = job[0];
            int end = job[1];
            int curProfit = job[2];
            
            // Find max profit achievable before or at start time
            // floorEntry gets the entry with largest key <= start
            int maxProfitBefore = dp.floorEntry(start).getValue();
            
            // Current max profit if we take this job
            int newProfit = maxProfitBefore + curProfit;
            
            // Only update if this gives us better profit
            // ceilingEntry gets entry with smallest key >= end
            if (dp.ceilingEntry(end) == null || newProfit > dp.ceilingEntry(end).getValue()) {
                dp.put(end, newProfit);
                
                // Remove all entries between end and next that have lower profit
                // This maintains the invariant that profit increases with time
                Integer nextKey = dp.higherKey(end);
                while (nextKey != null && dp.get(nextKey) <= newProfit) {
                    dp.remove(nextKey);
                    nextKey = dp.higherKey(end);
                }
            }
        }
        
        return dp.lastEntry().getValue();
    }
    
    /**
     * Job class to encapsulate job data
     * INTERVIEW TIP: Creating a class makes code cleaner and shows OOP thinking
     */
    static class Job {
        int start;
        int end;
        int profit;
        
        Job(int start, int end, int profit) {
            this.start = start;
            this.end = end;
            this.profit = profit;
        }
    }
    
    /**
     * TESTING SUITE - ALWAYS WALK THROUGH TEST CASES IN INTERVIEWS
     */
    public static void main(String[] args) {
        Solution sol = new Solution();
        
        // TEST CASE 1: Example from problem
        // Jobs: [1,2,3,3], [3,4,5,6], [20,20,100,70]
        // Expected: 120 (take job 0 and job 2)
        System.out.println("Test 1: " + sol.jobScheduling(
            new int[]{1,2,3,3}, 
            new int[]{3,4,5,6}, 
            new int[]{20,20,100,70}
        ) + " (Expected: 120)");
        
        // TEST CASE 2: All overlapping jobs
        // Should pick the one with max profit
        System.out.println("Test 2: " + sol.jobScheduling(
            new int[]{1,1,1}, 
            new int[]{2,3,4}, 
            new int[]{5,6,4}
        ) + " (Expected: 6)");
        
        // TEST CASE 3: No overlapping jobs
        // Should take all jobs
        System.out.println("Test 3: " + sol.jobScheduling(
            new int[]{1,2,3,4,5}, 
            new int[]{2,3,4,5,6}, 
            new int[]{1,2,3,4,5}
        ) + " (Expected: 15)");
        
        // TEST CASE 4: Edge case - single job
        System.out.println("Test 4: " + sol.jobScheduling(
            new int[]{1}, 
            new int[]{2}, 
            new int[]{50}
        ) + " (Expected: 50)");
        
        // TEST CASE 5: Job ending at X allows starting at X
        System.out.println("Test 5: " + sol.jobScheduling(
            new int[]{1,3}, 
            new int[]{3,5}, 
            new int[]{10,20}
        ) + " (Expected: 30)");
    }
}

/**
 * ============================================================================
 * INTERVIEW STRATEGY SUMMARY
 * ============================================================================
 * 
 * 1. START WITH BRUTE FORCE (1-2 minutes):
 *    "We could try all subsets of jobs and check each for validity"
 *    Time: O(2^n), clearly not acceptable
 * 
 * 2. IDENTIFY OPTIMIZATION (2-3 minutes):
 *    "This is weighted interval scheduling - classic DP problem"
 *    "Key insight: Sort by end time, then for each job decide: take or skip"
 *    "If we take it, we need to find the next compatible job - binary search!"
 * 
 * 3. CODE TOP-DOWN FIRST (10-15 minutes):
 *    - Easier to explain
 *    - Natural recursion with clear base cases
 *    - Shows problem-solving process
 * 
 * 4. OPTIMIZE TO BOTTOM-UP (5 minutes):
 *    "We can eliminate recursion stack by building up from base case"
 * 
 * 5. ANALYZE COMPLEXITY (2 minutes):
 *    - Time: O(n log n) for sort + O(n log n) for DP = O(n log n)
 *    - Space: O(n) for DP array
 * 
 * 6. TEST WITH EDGE CASES (3-5 minutes):
 *    - Empty array
 *    - Single job
 *    - All overlap
 *    - None overlap
 *    - Boundary conditions (job ending at X, next starting at X)
 * 
 * ============================================================================
 * COMMON MISTAKES TO AVOID
 * ============================================================================
 * 
 * 1. SORTING BY START TIME: Wrong! You need end time to find compatible jobs
 * 
 * 2. BINARY SEARCH BUGS: 
 *    - Off-by-one errors
 *    - Wrong comparison (>= vs >)
 *    - Not handling edge case when no compatible job exists
 * 
 * 3. DP STATE CONFUSION:
 *    - Forgetting to memoize
 *    - Wrong base case
 *    - Not considering both "take" and "skip" options
 * 
 * 4. OVERLAP DEFINITION:
 *    - Remember: ending at X allows starting at X (use >= not >)
 * 
 * ============================================================================
 * FOLLOW-UP QUESTIONS YOU MIGHT GET
 * ============================================================================
 * 
 * Q: "What if jobs have dependencies?"
 * A: Model as a DAG, use topological sort + DP
 * 
 * Q: "What if we can only take k jobs?"
 * A: Add another dimension to DP: dp[i][j] = max profit using i jobs up to index j
 * 
 * Q: "What if we want to track which jobs we took?"
 * A: Backtrack through DP array to reconstruct solution
 * 
 * Q: "Can you do better than O(n log n)?"
 * A: No, sorting is necessary. This is optimal.
 * 
 * ============================================================================
 */

/**
 * Job Scheduling with Profit (Weighted Interval Scheduling)
 *
 * Time Complexity:
 *   Sorting: O(n log n)
 *   DP states: O(n)
 *   Each state does binary search: O(log n)
 *   Total: O(n log n)
 *
 * Space Complexity:
 *   O(n) for dp + jobs array
 */
class JobSchedulingProfit {

    record Job(int start, int end, int profit) {}

    private Job[] jobs;
    private int[] starts;
    private Integer[] memo;

    public int jobScheduling(int[] startTime, int[] endTime, int[] profit) {
        int n = startTime.length;

        jobs = new Job[n];
        for (int i = 0; i < n; i++) {
            jobs[i] = new Job(startTime[i], endTime[i], profit[i]);
        }

        // Sort jobs by start time
        Arrays.sort(jobs, Comparator.comparingInt(Job::start));

        // Pre-store all start times for binary search
        starts = new int[n];
        for (int i = 0; i < n; i++) {
            starts[i] = jobs[i].start();
        }

        memo = new Integer[n];

        return dfs(0);
    }

    /**
     * dfs(i) = maximum profit achievable starting from job index i
     */
    private int dfs(int i) {
        if (i >= jobs.length) return 0;

        if (memo[i] != null) return memo[i];

        // Option 1: skip this job
        int skip = dfs(i + 1);

        // Option 2: take this job
        Job job = jobs[i];

        // Find next job that starts >= current job end
        int next = lowerBound(starts, job.end());

        int take = job.profit() + dfs(next);

        return memo[i] = Math.max(skip, take);
    }

    /**
     * Standard lowerBound:
     * returns smallest index where arr[idx] >= target
     */
    private int lowerBound(int[] arr, int target) {
        int low = 0, high = arr.length;

        while (low < high) {
            int mid = low + (high - low) / 2;

            if (arr[mid] >= target) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }

        return low;
    }
}
