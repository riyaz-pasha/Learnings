import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/*
================================================================================
PROBLEM
--------------------------------------------------------------------------------
Assign limited resources (e.g., memory) to jobs with varying demands and priorities.

================================================================================
INTERVIEW FLOW (WHAT I SAY OUT LOUD)
--------------------------------------------------------------------------------
1) Jobs compete for limited resources
2) Priority decides scheduling order
3) Use a max-heap to always pick highest priority job
4) Allocate resources greedily if possible
================================================================================
*/

class Job {
    String id;
    int requiredMemory;
    int priority;

    Job(String id, int requiredMemory, int priority) {
        this.id = id;
        this.requiredMemory = requiredMemory;
        this.priority = priority;
    }
}

class ResourceAllocator {

    /*
     * Returns list of jobs that were successfully scheduled O(N log N)
     */
    public static List<Job> assignJobs(
            List<Job> jobs,
            int totalMemory
    ) {
        int availableMemory = totalMemory;

        // Max-heap based on priority
        PriorityQueue<Job> pq = new PriorityQueue<>(
            (a, b) -> Integer.compare(b.priority, a.priority)
        );

        pq.addAll(jobs);

        List<Job> scheduled = new ArrayList<>();

        while (!pq.isEmpty()) {
            Job job = pq.poll();

            if (job.requiredMemory <= availableMemory) {
                // Allocate resources
                availableMemory -= job.requiredMemory;
                scheduled.add(job);
            }
            // else: skip or leave waiting (policy-dependent)
        }

        return scheduled;
    }

    /*
     * DEMO
     */
    public static void main(String[] args) {
        List<Job> jobs = List.of(
            new Job("A", 30, 3),
            new Job("B", 50, 5),
            new Job("C", 20, 1),
            new Job("D", 40, 4)
        );

        int totalMemory = 100;

        List<Job> result = assignJobs(jobs, totalMemory);

        for (Job j : result) {
            System.out.println(
                j.id + " allocated (" + j.requiredMemory + ", priority " + j.priority + ")"
            );
        }
    }
}
