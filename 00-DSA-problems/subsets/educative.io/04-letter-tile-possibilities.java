/**
 * ============================================================================
 * PROBLEM STATEMENT: Letter Tile Possibilities
 * ============================================================================
 * You are given a string, tiles, consisting of uppercase English letters. You 
 * can arrange the tiles into sequences of any length (from 1 to the length 
 * of tiles), and each sequence must include at most one tile, tiles[i], from 
 * tiles.
 * 
 * Your task is to return the number of possible non-empty unique sequences you 
 * can make using the letters represented on tiles[i].
 * 
 * Constraints:
 * - 1 <= tiles.length <= 7
 * - The tiles string consists of uppercase English letters.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS (To ask in an interview):
 * ============================================================================
 * 1. Can the input contain duplicate characters? 
 *    (Yes, this is the crux of the problem. If the input is "AAB", the two 'A's 
 *    are distinct tiles, but the sequence "A" formed by the first tile is 
 *    identical to the sequence "A" formed by the second tile. We only want 
 *    *unique* sequences).
 * 2. Does the order of characters in the sequence matter?
 *    (Yes, it's a permutation problem. "AB" is considered different from "BA").
 * 3. Do we need to return the actual sequences, or just the count?
 *    (Just the count. This opens the door to heavily optimized integer-based 
 *    backtracking instead of building strings).
 * 
 * ============================================================================
 * INTERVIEW APPROACH:
 * ============================================================================
 * 1. Acknowledge constraints: n <= 7. This means the maximum number of sequences 
 *    is extremely small (at most ~13,700 for 7 unique letters). Backtracking 
 *    is the perfect fit.
 * 2. Discuss the Naive approach (HashSet + Used Array): Build every possible 
 *    string using standard backtracking, toss them into a HashSet to remove 
 *    duplicates, and return the set size. It works, but it wastes time and 
 *    memory building string combinations that are already known duplicates.
 * 3. Propose the Optimal approach (Frequency Array Backtracking): Instead of 
 *    tracking individual tiles, track the *pool* of available letters. If we 
 *    have two 'A's and one 'B' (A=2, B=1), our choices at step 1 are just 'A' 
 *    or 'B'. This inherently prevents duplicates from forming, completely 
 *    eliminating the need for a HashSet.
 * 
 * ============================================================================
 * TIME & SPACE COMPLEXITY:
 * ============================================================================
 * Approach 1 (Frequency Map): 
 * - Time: O(P) where P is the total number of valid unique permutations. 
 *   Max P for n=7 is ~13,699.
 * - Space: O(26) for the frequency array + O(N) for recursion depth = O(1) overall.
 * 
 * Approach 2 (HashSet):
 * - Time: O(N * N!) in the worst case (string concatenation and hashing).
 * - Space: O(N * N!) to store all generated strings in the HashSet.
 */

import java.util.HashSet;
import java.util.Set;

class TilePossibilitiesSolver {

    public static void main(String[] args) {
        String tiles = "AAB";
        
        System.out.println("Input tiles: \"AAB\"\n");

        System.out.println("1. Optimal Frequency Backtracking Result:");
        System.out.println(numTilePossibilitiesOptimal(tiles));
        
        System.out.println("\n2. Naive HashSet Backtracking Result:");
        System.out.println(numTilePossibilitiesHashSet(tiles));
    }

    /**
     * ========================================================================
     * SOLUTION 1: FREQUENCY ARRAY BACKTRACKING (OPTIMAL)
     * ========================================================================
     * Idea & Intuition:
     * To prevent duplicate strings, we iterate over the *alphabet* rather than 
     * the tiles. If we have multiple 'A's, we just see that we have 'A' 
     * available in our "bucket". We pick one, decrease the count, recursively 
     * build further sequences, and then put it back.
     * 
     * Visual Decision Tree for "AAB" (Frequencies: A=2, B=1):
     *                          (Start)
     *                        /         \
     *                     'A'          'B'
     *                 (A=1, B=1)    (A=2, B=0)
     *                  /      \          |
     *                'A'      'B'       'A'
     *            (A=0,B=1) (A=1,B=0) (A=1,B=0)
     *                |          |        |
     *               'B'        'A'      'A'
     * 
     * Every node (except start) represents a unique sequence.
     * Total nodes = 8 sequences ("A", "AA", "AAB", "AB", "ABA", "B", "BA", "BAA").
     */
    public static int numTilePossibilitiesOptimal(String tiles) {
        int[] freq = new int[26];
        // Build the frequency map
        for (char c : tiles.toCharArray()) {
            freq[c - 'A']++;
        }
        return dfsOptimal(freq);
    }

    private static int dfsOptimal(int[] freq) {
        int sequenceCount = 0;
        
        // Iterate over the 26 possible uppercase letters
        for (int i = 0; i < 26; i++) {
            if (freq[i] > 0) {
                // If we use this letter, it constitutes 1 valid sequence on its own
                sequenceCount++; 
                
                // Choose this letter: decrement its availability
                freq[i]--;
                
                // Explore further sequences starting with the current state,
                // and add their counts to our total
                sequenceCount += dfsOptimal(freq);
                
                // Un-choose (Backtrack): put the letter back in the pool
                freq[i]++;
            }
        }
        
        return sequenceCount;
    }

    /**
     * ========================================================================
     * SOLUTION 2: HASHSET + USED ARRAY (NAIVE BUT INTUITIVE)
     * ========================================================================
     * Idea & Intuition:
     * Treat every tile as distinct (e.g., A1, A2, B1). Generate all possible 
     * string permutations using a standard `used` array backtracking template.
     * Add every non-empty sequence to a HashSet. The set will automatically 
     * filter out the duplicates caused by identical tiles (e.g., it treats 
     * A1-B1 and A2-B1 as just "AB").
     * 
     * Why it's less optimal:
     * It wastes time doing string concatenation and hashing for duplicate paths.
     */
    public static int numTilePossibilitiesHashSet(String tiles) {
        Set<String> uniqueSequences = new HashSet<>();
        boolean[] used = new boolean[tiles.length()];
        
        // StringBuilder is used to efficiently append/remove characters
        dfsHashSet(tiles, used, new StringBuilder(), uniqueSequences);
        
        return uniqueSequences.size();
    }

    private static void dfsHashSet(String tiles, boolean[] used, 
                                   StringBuilder current, Set<String> uniqueSequences) {
        
        // Every non-empty state is a valid sequence to track
        if (current.length() > 0) {
            uniqueSequences.add(current.toString());
        }

        // Iterate over specific tiles
        for (int i = 0; i < tiles.length(); i++) {
            if (used[i]) continue; // Skip if tile is already in the current sequence

            // Choose
            used[i] = true;
            current.append(tiles.charAt(i));
            
            // Explore
            dfsHashSet(tiles, used, current, uniqueSequences);
            
            // Un-choose
            used[i] = false;
            current.deleteCharAt(current.length() - 1);
        }
    }
}
