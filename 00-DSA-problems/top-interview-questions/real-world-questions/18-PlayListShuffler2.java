import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/*
================================================================================
PROBLEM
--------------------------------------------------------------------------------
Design a system that shuffles songs randomly but avoids repeats until all songs
have been played.

================================================================================
INTERVIEW FLOW (WHAT I SAY OUT LOUD)
--------------------------------------------------------------------------------
Step 1: Avoid naive random picking (causes repeats)
Step 2: Generate a random permutation instead
Step 3: Use Fisher-Yates shuffle
Step 4: Iterate sequentially, reshuffle after exhaustion
================================================================================
*/

class ShufflePlayer {

    private final List<String> songs;
    private int index;
    private final Random random = new Random();

    public ShufflePlayer(List<String> songs) {
        this.songs = new ArrayList<>(songs);
        shuffle();
    }

    /*
     * Returns the next song in shuffled order.
     * Reshuffles automatically after all songs are played.
     */
    public String nextSong() {

        if (index == songs.size()) {
            shuffle(); // all songs played → reshuffle
        }

        return songs.get(index++);
    }

    /*
     * Fisher-Yates shuffle
     * Guarantees uniform randomness
     */
    private void shuffle() {
        for (int i = songs.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(songs, i, j);
        }
        index = 0;
    }

    /*
     * Demo
     */
    public static void main(String[] args) {
        ShufflePlayer player = new ShufflePlayer(
            List.of("Song A", "Song B", "Song C", "Song D")
        );

        for (int i = 0; i < 10; i++) {
            System.out.println(player.nextSong());
        }
    }
}


/*
================================================================================
PROBLEM
--------------------------------------------------------------------------------
Shuffle songs randomly such that:
- No song repeats within the last K played songs

================================================================================
INTERVIEW FLOW (WHAT I SAY OUT LOUD)
--------------------------------------------------------------------------------
1) We must maintain a sliding window of last K songs
2) Randomness must respect exclusion constraints
3) Use a queue + set to track recent songs
4) Randomly pick from eligible songs only
================================================================================
*/

class ShufflePlayerWithKConstraint {

    private final List<String> allSongs;
    private final int k;

    // Last K songs
    private final Deque<String> recentQueue = new ArrayDeque<>();
    private final Set<String> recentSet = new HashSet<>();

    private final Random random = new Random();

    public ShufflePlayerWithKConstraint(List<String> songs, int k) {
        if (k >= songs.size()) {
            throw new IllegalArgumentException(
                "k must be smaller than number of songs"
            );
        }
        this.allSongs = new ArrayList<>(songs);
        this.k = k;
    }

    /*
     * Returns next song while ensuring:
     * - No repeat within last K songs
     */
    public String nextSong() {

        List<String> candidates = new ArrayList<>();

        // Build eligible pool
        for (String song : allSongs) {
            if (!recentSet.contains(song)) {
                candidates.add(song);
            }
        }

        // Randomly select from eligible songs
        String chosen = candidates.get(random.nextInt(candidates.size()));

        // Update sliding window
        recentQueue.offerLast(chosen);
        recentSet.add(chosen);

        if (recentQueue.size() > k) {
            String removed = recentQueue.pollFirst();
            recentSet.remove(removed);
        }

        return chosen;
    }

    /*
     * Demo
     */
    public static void main(String[] args) {
        ShufflePlayerWithKConstraint player =
            new ShufflePlayerWithKConstraint(
                List.of("A", "B", "C", "D", "E"),
                2
            );

        for (int i = 0; i < 15; i++) {
            System.out.print(player.nextSong() + " ");
        }
    }
}


/*
================================================================================
SHUFFLE WITH NO REPEAT IN LAST K SONGS (O(1) AMORTIZED)
================================================================================
Key idea:
- Maintain an "available songs" list
- Remove songs when played
- Re-add them when they leave the last-K window
================================================================================
*/

class ShufflePlayerWithKConstraintOptimized {

    private final List<String> available;          // eligible songs
    private final Map<String, Integer> indexMap;   // song -> index in available
    private final Deque<String> recentQueue;       // last K songs
    private final int k;
    private final Random random = new Random();

    public ShufflePlayerWithKConstraintOptimized(List<String> songs, int k) {
        if (k >= songs.size()) {
            throw new IllegalArgumentException("k must be less than total songs");
        }

        this.k = k;
        this.available = new ArrayList<>(songs);
        this.indexMap = new HashMap<>();
        this.recentQueue = new ArrayDeque<>();

        for (int i = 0; i < songs.size(); i++) {
            indexMap.put(songs.get(i), i);
        }
    }

    /*
     * Returns next random song
     * Guarantees no repeat in last K
     */
    public String nextSong() {

        // 1️⃣ Pick random eligible song
        int randIndex = random.nextInt(available.size());
        String chosen = available.get(randIndex);

        // 2️⃣ Remove chosen song from available (O(1))
        removeFromAvailable(chosen);

        // 3️⃣ Add to recent history
        recentQueue.offerLast(chosen);

        // 4️⃣ Evict old song if window exceeds K
        if (recentQueue.size() > k) {
            String expired = recentQueue.pollFirst();
            addToAvailable(expired);
        }

        return chosen;
    }

    /* ===================== Helper Methods ===================== */

    private void removeFromAvailable(String song) {
        int index = indexMap.get(song);
        int lastIndex = available.size() - 1;

        String lastSong = available.get(lastIndex);

        // swap
        available.set(index, lastSong);
        indexMap.put(lastSong, index);

        // remove last
        available.remove(lastIndex);
        indexMap.remove(song);
    }

    private void addToAvailable(String song) {
        indexMap.put(song, available.size());
        available.add(song);
    }

    /*
     * Demo
     */
    public static void main(String[] args) {
        ShufflePlayerWithKConstraintOptimized player =
            new ShufflePlayerWithKConstraintOptimized(
                List.of("A", "B", "C", "D", "E"),
                2
            );

        for (int i = 0; i < 15; i++) {
            System.out.print(player.nextSong() + " ");
        }
    }
}



/*
================================================================================
SHUFFLE WITH NO REPEAT IN LAST K SONGS — Two-Queue Approach
================================================================================

STRUCTURE:
  nextSongs   → pool of songs eligible to be played (ArrayList for O(1) swap-remove)
  playedSongs → sliding window of the last K played songs (Queue, FIFO)

WHY NO indexMap?
  Original needed it to locate arbitrary songs inside `available` during swap-remove.
  Here, we only ever remove at a randomly-chosen index we already hold in hand.
  Songs re-entering the pool are always appended to the tail — no position lookup needed.

LIFECYCLE OF A SONG:
                 ┌────────────────────────────────────────────┐
                 │                nextSongs                   │
                 │  [D, A, C, E]  ← random pick → chosen = C │
                 └────────────────────────────────────────────┘
                          │ swap-remove C (O(1))
                          ▼
              nextSongs = [D, A, E]    (E swapped into C's slot)
                          │ offer C to playedSongs
                          ▼
              playedSongs = [B, F, C]  (size = k = 3, no eviction yet)

              On next call if size > k → poll B from playedSongs → add B back to nextSongs

TIME COMPLEXITY:
  nextSong()  → O(1) amortized
  - random.nextInt        O(1)
  - swap-remove from list O(1)
  - queue offer/poll      O(1)
  - list append           O(1) amortized

SPACE: O(n) where n = total songs
================================================================================
*/
class ShufflePlayer2 {

    // ── Core fields ──────────────────────────────────────────────────────────

    private final List<Song> nextSongs;     // eligible songs (not in recent window)
    private final Queue<Song> playedSongs;  // last K songs (FIFO eviction)
    private final int k;
    private final Random random = new Random();

    // ── Constructor ──────────────────────────────────────────────────────────

    public ShufflePlayer2(List<Song> library, int k) {
        if (k >= library.size()) {
            throw new IllegalArgumentException(
                "k (" + k + ") must be less than library size (" + library.size() + ")"
            );
        }

        this.k = k;
        this.nextSongs   = new ArrayList<>(library); // mutable copy; no shuffle needed
        this.playedSongs = new LinkedList<>();        // LinkedList = O(1) offer + poll
    }

    // ── Core method ──────────────────────────────────────────────────────────

    /*
     * Returns a random song guaranteed not to repeat within the last K plays.
     *
     * Step-by-step:
     *   1. Random index into nextSongs            → O(1)
     *   2. Swap-remove chosen song from nextSongs → O(1)  [no indexMap needed]
     *   3. Push chosen onto playedSongs           → O(1)
     *   4. If playedSongs overflows K, graduate
     *      the oldest song back into nextSongs    → O(1)
     */
    public Song nextSong() {

        // 1️⃣ Pick a random song from the eligible pool
        int idx = random.nextInt(nextSongs.size());
        Song chosen = nextSongs.get(idx);

        // 2️⃣ O(1) removal via swap-with-tail trick:
        //    - Move the last element into the chosen slot
        //    - Truncate the list by one
        //    No indexMap needed — we already have idx from the random pick above.
        int lastIdx = nextSongs.size() - 1;
        nextSongs.set(idx, nextSongs.get(lastIdx));
        nextSongs.remove(lastIdx);

        // 3️⃣ Record this play — push onto the back of the recent window
        playedSongs.offer(chosen);

        // 4️⃣ Sliding window enforcement:
        //    Once playedSongs holds more than K songs,
        //    the oldest song has "aged out" and is eligible again.
        //    Appending to nextSongs tail is always O(1) — no position tracking needed.
        if (playedSongs.size() > k) {
            Song graduated = playedSongs.poll(); // evict front (oldest)
            nextSongs.add(graduated);            // re-admit to pool at tail
        }

        return chosen;
    }

    // ── Song model ───────────────────────────────────────────────────────────

    record Song(String title, String artist) {
        @Override public String toString() { return title; }
    }

    // ── Demo ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        List<Song> library = List.of(
            new Song("A", "Artist1"),
            new Song("B", "Artist2"),
            new Song("C", "Artist3"),
            new Song("D", "Artist4"),
            new Song("E", "Artist5")
        );

        ShufflePlayer2 player = new ShufflePlayer2(library, 2);

        System.out.println("Shuffle (k=2, no song repeats within last 2 plays):");
        for (int i = 0; i < 15; i++) {
            System.out.print(player.nextSong() + " ");
        }
        // e.g. → C A E B D A C B E D B C A E D
        //         ↑       ↑             (C repeats only after 2+ others played)
    }
}
