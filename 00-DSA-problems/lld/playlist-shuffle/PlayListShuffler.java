import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

class Track {
    String id;
}

class PlayListShuffler {

    private final List<Track> tracks;
    private final int k;

    private final Queue<Track> recentTracksQueue;
    private final Set<Track> recentTracks;

    private final Random random;

    public PlayListShuffler(List<Track> tracks, int k) {
        this.tracks = new ArrayList<>(tracks);
        this.k = k;

        this.recentTracks = new HashSet<>();
        this.recentTracksQueue = new LinkedList<>();

        this.random = new Random();
    }

    public Track next() {
        List<Track> nextTracks = new ArrayList<>();

        // (O(n))
        for (Track track : tracks) {
            if (!recentTracks.contains(track)) {
                nextTracks.add(track);
            }
        }

        int nextTrackIndex = random.nextInt(nextTracks.size());
        Track nexTrack = nextTracks.get(nextTrackIndex);

        if (recentTracks.size() >= k) {
            Track trackToRemove = recentTracksQueue.poll();
            recentTracks.remove(trackToRemove);
        }
        recentTracksQueue.offer(nexTrack);
        recentTracks.add(nexTrack);

        return nexTrack;
    }

}

class PlayListShufflerOptimized {

    private final List<Track> availableTracks;
    private final Map<Track, Integer> availableTracksIndexMap;
    private final int k;

    private final Queue<Track> recentTracksQueue;
    private final Set<Track> recentTracks;

    private final Random random;

    public PlayListShufflerOptimized(List<Track> tracks, int k) {
        this.availableTracks = new ArrayList<>(tracks);
        this.availableTracksIndexMap = new HashMap<>();
        for (int idx = 0; idx < availableTracks.size(); idx++) {
            availableTracksIndexMap.put(availableTracks.get(idx), idx);
        }

        this.k = k;

        this.recentTracks = new HashSet<>();
        this.recentTracksQueue = new LinkedList<>();

        this.random = new Random();
    }

    // O(1)
    public Track next() {
        if (availableTracks.isEmpty()) {
            throw new IllegalStateException("No available tracks to play.");
        }

        int nextTrackIndex = random.nextInt(availableTracks.size());
        Track nexTrack = availableTracks.get(nextTrackIndex);

        this.removeFromAvailable(nextTrackIndex, nexTrack);

        if (recentTracks.size() >= k) {
            Track expiredTrack = recentTracksQueue.poll();
            recentTracks.remove(expiredTrack);
            this.addToAvailable(expiredTrack);
        }
        recentTracksQueue.offer(nexTrack);
        recentTracks.add(nexTrack);

        return nexTrack;
    }

    // O(1)
    private void removeFromAvailable(int trackIndex, Track track) {
        Track lastTrack = this.availableTracks.getLast();

        this.availableTracks.set(trackIndex, lastTrack);
        this.availableTracks.removeLast();

        this.availableTracksIndexMap.put(lastTrack, trackIndex);
        this.availableTracksIndexMap.remove(track);
    }

    // O(1)
    private void addToAvailable(Track expiredTrack) {
        this.availableTracksIndexMap.put(expiredTrack, this.availableTracks.size());
        this.availableTracks.add(expiredTrack);
    }

}
