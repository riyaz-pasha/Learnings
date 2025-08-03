import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class Leader {
}

class Vote {

    Leader leader;
    long timestamp;

}

class VotingSystem {

    private final Map<Leader, Integer> voteCounts;
    private Optional<Leader> leadingCandidate;
    private int maxVotes;

    public VotingSystem() {
        this.voteCounts = new ConcurrentHashMap<>();
        this.leadingCandidate = Optional.empty();
        this.maxVotes = 0;
    }

    public void initializeSystem(List<Leader> leaders) {
        leaders.forEach(leader -> this.voteCounts.put(leader, 0));
    }

    public void vote(Vote vote) {
        if (!this.voteCounts.containsKey(vote.leader)) {
            throw new Error("Invalid leader");
        }

        int count = this.voteCounts.getOrDefault(vote.leader, 0) + 1;
        this.voteCounts.put(vote.leader, count);

        if (count > maxVotes) {
            leadingCandidate = Optional.of(vote.leader);
            maxVotes = count;
        }

    }

    public Optional<Leader> getLeadingCandidate() {
        return this.leadingCandidate;
    }

}

class VotingSystemOneVotePerPerson {

    private static class VoteRecord {
        int timestamp;
        String leader;

        VoteRecord(int timestamp, String leader) {
            this.timestamp = timestamp;
            this.leader = leader;
        }
    }

    private final Map<String, Integer> candidateVotes = new HashMap<>();
    private final Map<String, Integer> voterTimestamps = new HashMap<>();
    private final List<VoteRecord> timeline = new ArrayList<>();
    private String currentLeader = null;
    private int maxVotes = 0;

    public boolean vote(String voterId, String candidate, int timestamp) {
        if (voterTimestamps.containsKey(voterId)) {
            return false; // This voter has already voted
        }

        voterTimestamps.put(voterId, timestamp);

        int count = candidateVotes.getOrDefault(candidate, 0) + 1;
        candidateVotes.put(candidate, count);

        if (count >= maxVotes) {
            if (!candidate.equals(currentLeader)) {
                currentLeader = candidate;
                timeline.add(new VoteRecord(timestamp, currentLeader));
            }
            maxVotes = count;
        }

        return true; // Vote accepted
    }

    public String getLeader(int timestamp) {
        int left = 0, right = timeline.size() - 1;
        String leader = null;

        while (left <= right) {
            int mid = (left + right) / 2;
            if (timeline.get(mid).timestamp <= timestamp) {
                leader = timeline.get(mid).leader;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return leader;
    }

}
