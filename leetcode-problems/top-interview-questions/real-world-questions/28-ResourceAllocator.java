import java.util.ArrayList;
import java.util.List;

class Job {

    int id;
    int machinesRequired;
    int memoryRequired;
    int priority;
}

/*
 * Resources: We have a fixed total number of machines (M) and a fixed total
 * amount of memory (C) (e.g., in gigabytes).
 * 
 * Jobs: We have a set of N jobs. Each job i has:
 * - A machine demand: m_i
 * - A memory demand: c_i
 * - A priority/value: v_i
 * 
 * Objective: The goal is to select a subset of jobs to run such that the total
 * number of machines and memory used does not exceed the total available
 * resources, and the sum of the priorities of the selected jobs is maximized.
 * 
 * Nature of the problem: This is a one-time, static resource allocation
 * problem. Jobs are not pre-emptable.
 */
class ResourceAllocator {

    private List<Job> bestAllocation = new ArrayList<>();
    private int maxPriority = 0;

    public void findOptimalAllocation(List<Job> jobs, int totalMachines, int totalMemory) {
        this.generateSubsets(jobs, 0, new ArrayList<>(), totalMachines, totalMemory);

        System.out.println("Brute Force Optimal Priority: " + maxPriority);
        System.out.println("Brute Force Optimal Jobs: ");
        for (Job job : bestAllocation) {
            System.out.println("  - Job ID: " + job.id + ", Machines: " + job.machinesRequired + ", Memory: "
                    + job.machinesRequired + ", Priority: " + job.priority);
        }
    }

    // O(2^N * N)
    private void generateSubsets(List<Job> jobs,
            int index,
            List<Job> currentSubset,
            int totalMachines,
            int totalMemory) {

        if (index == jobs.size()) {
            // Base case: we have a complete subset.

            int currentMachinesUsed = 0;
            int currentMemoryUsed = 0;
            int currentPriority = 0;

            // O(N)
            for (Job job : currentSubset) {
                currentMachinesUsed += job.machinesRequired;
                currentMemoryUsed += job.memoryRequired;
                currentPriority += job.priority;
            }

            if (currentMachinesUsed <= totalMachines && currentMemoryUsed <= totalMemory) {
                if (currentPriority > maxPriority) {
                    maxPriority = currentPriority;
                    bestAllocation = new ArrayList<>(currentSubset);
                }
            }
            return;
        }

        // O(2^N)
        // Recursive step:
        // 1. Exclude the current job
        this.generateSubsets(jobs, index + 1, currentSubset, totalMachines, totalMemory);

        // 2. Include the current job
        currentSubset.add(jobs.get(index));
        this.generateSubsets(jobs, index + 1, currentSubset, totalMachines, totalMemory);
        currentSubset.removeLast(); // Backtrack
    }

    /*
     * Time Complexity: $O(2^N * N)$. There are 2^N possible subsets. For each
     * subset, we iterate through it to calculate the total resources and priority,
     * which takes O(N) time.
     * 
     * Space Complexity: O(N). The space is dominated by the recursion depth of the
     * function call stack, which can go up to N.
     */

}

/*
 * Optimal Solution (Dynamic Programming)
 * The brute force method is highly inefficient. We can solve this with dynamic
 * programming, which is the standard approach for the multi-dimensional
 * knapsack problem.
 * 
 * Let's define the DP state:
 * 
 * We need a multi-dimensional array to store the maximum priority for a given
 * number of jobs, machines, and memory.
 * Let dp[i][j][k] be the maximum priority we can achieve by considering only
 * the first i jobs, using at most j machines and at most k memory.
 * 
 * DP State Transition:
 * 
 * For the i-th job, we have two choices:
 * 
 * - Don't include job i: The maximum priority is the same as the maximum
 * priority we could get with the first i-1 jobs using j machines and k memory.
 * -- dp[i][j][k] = dp[i-1][j][k]
 * 
 * - Include job i (if possible): We can only include job i if we have enough
 * resources. If we include it, the remaining resources are j−m_i machines and
 * k−c_i memory. The priority gained is v_i.
 * -- dp[i][j][k] = dp[i-1][j-m_i][k-c_i] + v_i
 * 
 * Combining these, the recurrence relation is:
 * dp[i][j][k]= max(dp[i−1][j][k], dp[i−1][j−m_i][k−c_i]+v_i)
 * 
 * Base Cases:
 * - dp[0][j][k] = 0 for all j, k (no jobs, no priority).
 * - dp[i][j][k] = 0 if j < 0 or k < 0.
 */
class DPOptimalResourceAllocator {

    /*
     * Time Complexity: O(N * M * C). We iterate through three nested loops for
     * jobs, machines, and memory.
     * 
     * Space Complexity: O(N * M * C). The size of the DP table is proportional
     * to the product of these three variables.
     */
    public int findOptimalAllocation(List<Job> jobs, int totalMachines, int totalMemory) {
        int n = jobs.size();

        // DP table: dp[jobs][machines][memory]
        int[][][] dp = new int[n + 1][totalMachines + 1][totalMemory + 1];

        for (int i = 1; i <= n; i++) {
            Job currentJob = jobs.get(i - 1);
            for (int m = 0; m <= totalMachines; m++) {
                for (int c = 0; c <= totalMemory; c++) {
                    // Case 1: Exclude the current job
                    dp[i][m][c] = dp[i - 1][m][c];

                    // Case 2: Include the current job if resources allow
                    if (m >= currentJob.machinesRequired && c >= currentJob.memoryRequired) {
                        int priorityWithJob = currentJob.priority
                                + dp[i - 1][m - currentJob.machinesRequired][c - currentJob.memoryRequired];
                        dp[i][m][c] = Math.max(dp[i][m][c], priorityWithJob);
                    }
                }
            }
        }
        return dp[n][totalMachines][totalMemory];
    }

}
