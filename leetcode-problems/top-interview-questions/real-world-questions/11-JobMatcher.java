/*
 * Match job seekers to job postings based on skill overlap and preference
 * score.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class JobSeeker {

    String id;
    Set<String> skills;
    Map<String, Double> preferenceScores; // jobId -> score

}

class JobPosting {

    String jobId;
    Set<String> requiredSkills;

}

class MatchResult {

    String jobSeekerId;
    String jobId;
    double score;

    MatchResult(String jobSeekerId, String jobId, double score) {
        this.jobSeekerId = jobSeekerId;
        this.jobId = jobId;
        this.score = score;
    }

}

class JobMatcher {

    double alpha = 0.7; // weight for skill match
    double beta = 0.3; // weight for preference

    public List<MatchResult> match(List<JobSeeker> jobSeekers, List<JobPosting> jobPostings) {
        List<MatchResult> results = new ArrayList<>();

        for (JobSeeker jobSeeker : jobSeekers) {
            for (JobPosting jobPosting : jobPostings) {
                Set<String> matchedSkills = new HashSet<>(jobSeeker.skills);
                matchedSkills.retainAll(jobPosting.requiredSkills);

                double skillMatchScore = (double) matchedSkills.size() / jobPosting.requiredSkills.size();
                double prefScore = jobSeeker.preferenceScores.getOrDefault(jobPosting.jobId, 0.0);

                double totalScore = alpha * skillMatchScore + beta * prefScore;

                if (!matchedSkills.isEmpty()) {
                    results.add(new MatchResult(jobSeeker.id, jobPosting.jobId, totalScore));
                }
            }
        }

        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results;
    }

}

/*
 * ⚡ Performance Boost
 * Before: O(M×N) comparisons
 * Now: O(M × averageSkillsPerSeeker × jobsPerSkill)
 * On average, this drastically reduces unnecessary comparisons.
 */
class OptimizedJobMatcher {

    double alpha = 0.7;
    double beta = 0.3;

    public List<MatchResult> match(List<JobSeeker> seekers, List<JobPosting> jobs) {
        List<MatchResult> results = new ArrayList<>();
        Map<String, List<JobPosting>> skillToJobsMap = this.buildSkillToJobsMap(jobs);

        for (JobSeeker seeker : seekers) {
            Set<String> visitedJobs = new HashSet<>();

            for (String skill : seeker.skills) {
                List<JobPosting> matchedJobs = skillToJobsMap.getOrDefault(skill, new ArrayList<>());

                for (JobPosting job : matchedJobs) {
                    if (visitedJobs.contains(job.jobId)) {
                        continue;
                    }
                    visitedJobs.add(job.jobId);

                    Set<String> matchedSkills = new HashSet<>(seeker.skills);
                    matchedSkills.retainAll(job.requiredSkills);

                    double skillMatchScore = (double) matchedSkills.size() / job.requiredSkills.size();
                    double preferenceScore = seeker.preferenceScores.getOrDefault(job.jobId, 0.0);
                    double totalScore = alpha * skillMatchScore + beta * preferenceScore;

                    if (!matchedSkills.isEmpty()) {
                        results.add(new MatchResult(seeker.id, job.jobId, totalScore));
                    }
                }
            }
        }
        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results;
    }

    private Map<String, List<JobPosting>> buildSkillToJobsMap(List<JobPosting> jobs) {
        Map<String, List<JobPosting>> skillToJobsMap = new HashMap<>();
        for (JobPosting job : jobs) {
            for (String skill : job.requiredSkills) {
                skillToJobsMap
                        .computeIfAbsent(skill, k -> new ArrayList<>())
                        .add(job);
            }
        }
        return skillToJobsMap;
    }

}
