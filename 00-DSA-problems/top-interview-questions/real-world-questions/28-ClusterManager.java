import java.util.List;

record Resources(int cpu, int ram) {}

class Job {
    String id;
    int priority; // Higher is better
    Resources demand;

    public Job(String id, int priority, int cpu, int ram) {
        this.id = id;
        this.priority = priority;
        this.demand = new Resources(cpu, ram);
    }
}

class Machine {
    String id;
    Resources capacity;
    Resources used;

    public Machine(String id, int cpu, int ram) {
        this.id = id;
        this.capacity = new Resources(cpu, ram);
        this.used = new Resources(0, 0);
    }

    public boolean canFit(Resources demand) {
        return (used.cpu() + demand.cpu() <= capacity.cpu()) &&
               (used.ram() + demand.ram() <= capacity.ram());
    }

    public void assign(Resources demand) {
        this.used = new Resources(used.cpu() + demand.cpu(), used.ram() + demand.ram());
    }
}

class ClusterManager {
    public void schedule(List<Job> jobs, List<Machine> machines) {
        // 1. Sort jobs by Priority (descending)
        jobs.sort((a, b) -> Integer.compare(b.priority, a.priority));

        for (Job job : jobs) {
            boolean scheduled = false;
            for (Machine m : machines) {
                if (m.canFit(job.demand)) {
                    m.assign(job.demand);
                    System.out.println("Assigned Job " + job.id + " to Machine " + m.id);
                    scheduled = true;
                    break;
                }
            }
            if (!scheduled) {
                System.out.println("Job " + job.id + " is pending: No available resources.");
            }
        }
    }
}
