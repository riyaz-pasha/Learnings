import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

record Job(String id, Set<String> skills, double minSalary) {}
record Seeker(String id, Set<String> skills, double preferredSalary) {}
record Match(Job job, double score) {}

class JobMatcher {

    public List<Job> findBestJobs(Seeker seeker, List<Job> allJobs, int n) {
        PriorityQueue<Match> pq = new PriorityQueue<>(Comparator.comparingDouble(m -> m.score));

        for (Job job : allJobs) {
            double score = calculateScore(seeker, job);
            pq.offer(new Match(job, score));
            
            if (pq.size() > n) {
                pq.poll(); // Remove lowest score
            }
        }

        List<Job> results = new ArrayList<>();
        while (!pq.isEmpty()) {
            results.add(0, pq.poll().job);
        }
        return results;
    }

    private double calculateScore(Seeker s, Job j) {
        // 1. Skill Overlap (Jaccard Similarity)
        Set<String> intersection = new HashSet<>(s.skills());
        intersection.retainAll(j.skills());
        double skillScore = (double) intersection.size() / (s.skills().size() + j.skills().size() - intersection.size());

        // 2. Salary Preference
        double salaryScore = s.preferredSalary() <= j.minSalary() ? 1.0 : 0.5;

        return (skillScore * 0.7) + (salaryScore * 0.3);
    }

}
