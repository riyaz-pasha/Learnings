import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

class Song {
}

class PlaylistShuffler {

    private final List<Song> librarySongs;
    private Queue<Song> nextSongs;
    private Queue<Song> playedSongs;

    PlaylistShuffler(List<Song> songLibrary) {
        this.librarySongs = new ArrayList<>(songLibrary);
        this.nextSongs = new LinkedList<>();
        this.playedSongs = new LinkedList<>();
        this.initializeNewCycle();
    }

    private void initializeNewCycle() {
        List<Song> temp = new ArrayList<>(this.librarySongs); // O(n)
        Collections.shuffle(temp); // Java Collections.shuffle() is based on Fisher-Yates → O(n)

        this.nextSongs.clear(); // O(1)
        this.nextSongs.addAll(temp); // O(n)

        this.playedSongs.clear(); // O(1)
    }

    /*
     * Average case: O(1)
     * Worst case (when reshuffle is triggered): O(n)
     */
    public Song getNextSong() {
        if (this.librarySongs.isEmpty()) { // O(1)
            return null;
        }

        if (this.nextSongs.isEmpty()) {
            this.initializeNewCycle(); // O(n) worst-case
        }

        Song nextSong = this.nextSongs.poll(); // O(1)

        this.playedSongs.offer(nextSong); // O(1)

        return nextSong;
    }

    public void addSong(Song song) {
        librarySongs.add(song);
        initializeNewCycle(); // restart entire shuffle
    }

    public void removeSong(Song song) {
        librarySongs.remove(song);
        initializeNewCycle(); // restart
    }

}

class ShufflePlayer {

    private List<String> allSongs;
    private Queue<String> playQueue;
    private Random rand;

    public ShufflePlayer(List<String> songs) {
        this.allSongs = new ArrayList<>(songs);
        this.rand = new Random();
        reshuffle();
    }

    private void reshuffle() {
        List<String> temp = new ArrayList<>(allSongs);
        Collections.shuffle(temp, rand);
        playQueue = new LinkedList<>(temp);
    }

    public String playNext() {
        if (playQueue.isEmpty()) {
            reshuffle();
        }
        return playQueue.poll();
    }

}

// --------------------------------------------------------------------------------------------------------
/*
 * Design a playlist shuffler such that:
 * - Songs are played in random order.
 * -No song is repeated within the last k songs.
 * -After k other songs are played, a song can be played again.
 */

class PlaylistShufflerWithCooldown {

    private final int k;
    private final Random rand;
    private final List<Song> librarySongs;
    private final Deque<Song> recentSongsQueue;
    private final Set<Song> recentSongs;

    public PlaylistShufflerWithCooldown(List<Song> songs, int k) {
        this.librarySongs = new ArrayList<>(songs);
        this.recentSongsQueue = new ArrayDeque<>();
        this.recentSongs = new HashSet<>();
        this.k = k;
        this.rand = new Random();
    }

    // O(n)
    public Song getSong() {
        if (this.librarySongs.isEmpty()) {
            return null;
        }

        List<Song> nextSongs = this.librarySongs.stream()
                .filter(song -> !this.recentSongs.contains(song))
                .toList();

        int nextSongIndex = rand.nextInt(nextSongs.size());
        Song nextSong = nextSongs.get(nextSongIndex);

        this.recentSongsQueue.offer(nextSong);
        this.recentSongs.add(nextSong);

        if (this.recentSongsQueue.size() > k) {
            Song songToRemove = this.recentSongsQueue.poll();
            this.recentSongs.remove(songToRemove);
        }

        return nextSong;
    }

}

class PlaylistShufflerWithCooldownFast {

    private final List<Song> librarySongs;
    private final Deque<Song> recentHistory;
    private final Set<Song> recentSet;
    private final int k;
    private final Random rand;

    public PlaylistShufflerWithCooldownFast(List<Song> songs, int k) {
        this.librarySongs = new ArrayList<>(songs);
        this.recentHistory = new ArrayDeque<>();
        this.recentSet = new HashSet<>();
        this.k = k;
        this.rand = new Random();
    }

    // O(1) average (O(k) worst-case)
    public Song getNextSong() {
        if (librarySongs.isEmpty())
            return null;

        int maxRetries = 20;
        Song candidate = null;

        for (int i = 0; i < maxRetries; i++) {
            int index = rand.nextInt(librarySongs.size());
            candidate = librarySongs.get(index);
            if (!recentSet.contains(candidate)) {
                break;
            }
        }

        // Fallback: if all retries failed, allow repeat
        if (candidate == null || recentSet.contains(candidate)) {
            candidate = librarySongs.get(rand.nextInt(librarySongs.size()));
        }

        // Update recent history
        recentHistory.addLast(candidate);
        recentSet.add(candidate);

        if (recentHistory.size() > k) {
            Song removed = recentHistory.removeFirst();
            recentSet.remove(removed);
        }

        return candidate;
    }
}
