/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design a data structure that takes in an array of strings and efficiently 
 * computes the shortest distance between any two different strings in the array.
 * 
 * Implement the WordDistance class:
 * - WordDistance(String[] wordsDict): Initializes object with string array.
 * - int shortest(String word1, String word2): Returns shortest distance between 
 *   word1 and word2.
 * 
 * Constraints: wordsDict length <= 10^3, word length <= 10. Calls <= 1000.
 * word1 != word2 and both are guaranteed to be in the array.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Will the `wordsDict` be modified after initialization?
 *    (If it's immutable, we can safely precompute and cache. If it changes, our cache invalidates).
 * 2. Are we optimizing for memory or for query speed?
 *    (Determines whether we should eagerly precompute all answers or just organize data for faster queries).
 * 3. Can `word1` and `word2` have wildly different frequencies? 
 *    (If one word appears 10,000 times and the other 1 time, a binary search approach might beat two-pointers).
 * 4. Are the queries known in advance (offline querying)?
 *    (If yes, we could sort queries and do a single pass over the array).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: We need to answer repeated shortest-distance queries on a static array. 
 *   The binding constraint is that repeated linear scans of the entire array for every query 
 *   will waste massive amounts of CPU time re-reading irrelevant words.
 * 
 * --- Approach 1: On-the-fly Brute Force (No Precomputation) ---
 * 1. Intuition: Just keep track of where we last saw each word as we walk the array.
 * 2. Try: Init does nothing (store reference to array). On `shortest(w1, w2)`, loop through 
 *    the array. If we see `w1`, update `index1` and check `distance = abs(index1 - index2)`. 
 *    Do the same for `w2`.
 * 3. Why it works: It definitively finds the closest pair by updating positions as it scans.
 * 4. Why it's too costly: If we make 1,000 queries, we iterate over the exact same 1,000-element 
 *    array 1,000 times.
 * 5. Bottleneck: Scanning words we don't care about. If asking for "apple" and "banana", 
 *    we are wasting time looking at "cherry", "date", and "fig".
 * 6. Observation: We need a way to filter out the noise and *only* look at the indices 
 *    where `word1` and `word2` actually appear.
 * 
 * Time Complexity: Init: O(1). Query: O(N) — because we scan the whole array of size N every time.
 * Space Complexity: Init: O(1). Query: O(1) — no extra memory used.
 * 
 * --- Approach 2: Eager Precomputation (All Pairs Shortest Distance) ---
 * 1. Intuition: If we want queries to be instant, calculate every possible answer in advance.
 * 2. Try: Init uses a nested loop to calculate the shortest distance between EVERY unique pair 
 *    of words and stores it in a `HashMap<String, HashMap<String, Integer>>`.
 *    Query simply returns `map.get(w1).get(w2)`.
 * 3. Why it works: Lookup becomes an instantaneous Hash map retrieval.
 * 4. Why it's too costly: Space and Init time. If the array has 1,000 unique words, we must 
 *    compute and store 1,000,000 pairs. Many of these pairs will never be queried.
 * 5. Bottleneck: Memory explosion and wasted upfront work.
 * 6. Observation: We need a middle ground. Precompute the *locations*, but compute the 
 *    *distances* on demand.
 * 
 * Time Complexity: Init: O(N^2) — checking every pair. Query: O(1) — hash map lookup.
 * Space Complexity: Init: O(U^2) — where U is the number of unique words (up to N). Query: O(1).
 * 
 * --- Approach 3: Hash Map of Indices + Two Pointers (Optimal Balance) ---
 * 1. Intuition: Group the indices by word. "apple" -> [1, 5, 8], "banana" -> [2, 9].
 * 2. Try: Init builds a `HashMap<String, List<Integer>>`. 
 *    Query fetches the two lists. Since we process the array left-to-right during Init, 
 *    the lists are naturally sorted! We can use two pointers to find the min difference.
 * 3. Why it works: We only look at the exact locations of the requested words.
 * 4. Which to write in a 45-min interview?
 *    Approach 3. It elegantly shows knowledge of both spatial hashing (grouping) and 
 *    algorithmic efficiency (two pointers on sorted arrays). 
 * 
 * Time Complexity: Init: O(N) — one pass to populate the map. Query: O(K + L) — where K and L are the occurrences of word1 and word2.
 * Space Complexity: Init: O(N) — storing N integers across the lists. Query: O(1) — auxiliary space for pointers.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Words appearing adjacently multiple times: e.g., ["a", "b", "a", "b"]. The two-pointer 
 *   logic must gracefully handle interleaved coordinates without missing the absolute minimum.
 * - Wildly varying frequencies: "a" appears 999 times, "b" appears 1 time. K+L is still O(N), 
 *   which is safe.
 * - Distance of 1: As soon as we find a distance of 1, we can short-circuit and return 
 *   immediately, because it's impossible to have a smaller valid distance.
 * 
 * ============================================================================
 * 4. ## Key Insights, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Sorted arrays unlock linear comparison." 
 *   If you want to find the minimum difference between two sets of numbers, and they are sorted, 
 *   you NEVER need a nested loop O(K*L). You just advance the pointer of the smaller number, 
 *   trying to close the gap.
 * 
 * ASCII DRY RUN (Two Pointers)
 * words = ["practice", "makes", "perfect", "coding", "makes"]
 * Map built during Init:
 * "coding"  -> [3]
 * "makes"   -> [1, 4]
 * 
 * Query: shortest("coding", "makes")
 * List1 = [3], List2 = [1, 4]
 * P1 = 0 (val: 3), P2 = 0 (val: 1)
 * 
 * Iteration 1:
 *   Diff = |3 - 1| = 2.  (Min = 2)
 *   Which pointer to advance? The one with the SMALLER value, to try to catch up.
 *   List2[P2] is 1. List1[P1] is 3. Advance P2.
 * 
 * Iteration 2:
 *   P1 = 0 (val: 3), P2 = 1 (val: 4)
 *   Diff = |3 - 4| = 1. (Min = 1)
 *   Which pointer to advance? List1[P1] (3) < List2[P2] (4). Advance P1.
 * 
 * P1 is now out of bounds. Loop ends. 
 * Final Min = 1.
 * 
 * Pitfalls:
 * - Nested loop on the index lists: Doing `for(int i : list1) for(int j : list2)` makes the 
 *   query O(K*L). If the array is strictly alternating ["a", "b", "a", "b"...], K and L are 
 *   both N/2. K*L becomes O(N^2), defeating the purpose of the data structure.
 * 
 * Pattern Recognition: "Min difference between two sets" -> Sort + Two Pointers.
 * 
 * Interview Script snippet:
 * "Since we're doing many queries on a static array, calculating on the fly is too slow. 
 * Instead, in the constructor, I'll build a map from each string to a list of its indices. 
 * Because we iterate sequentially, these index lists are naturally sorted. During a query, 
 * I retrieve the two lists and use two pointers to find the minimum difference in linear 
 * time relative to their frequencies, which drops query time from O(N) down to O(K+L)."
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: What if `word1` can be the SAME as `word2`? (Word Distance III)
 * A1: We retrieve the single list for that word. Since it's sorted, the minimum distance 
 *     is simply the minimum difference between *adjacent* elements in that list. We can find 
 *     that in one pass O(K) by checking `list.get(i) - list.get(i-1)`.
 * 
 * Q2: What if the lists are extremely disproportionate in size? (e.g., K = 1, L = 100,000)
 * A2: The two-pointer approach takes O(K + L) = 100,001 operations. We could optimize this 
 *     by iterating over the smaller list, and using Binary Search on the larger list. 
 *     That would take O(K log L) = 1 * log(100,000) ≈ 17 operations.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordDistanceStudy {

    // Core data structure: Maps a word to a strictly increasing list of its indices.
    // E.g., Map: { "apple" -> [0, 4, 9], "banana" -> [2, 5] }
    private Map<String, List<Integer>> wordIndicesMap;

    /**
     * Initializes the object with an array of strings.
     * Time Complexity: O(N) where N is wordsDict.length.
     * Space Complexity: O(N) to store the hash map and all index lists.
     */
    public WordDistanceStudy(String[] wordsDict) {
        wordIndicesMap = new HashMap<>();
        
        // Loop through the dictionary once.
        // As i increases sequentially, the lists naturally become sorted.
        for (int i = 0; i < wordsDict.length; i++) {
            String word = wordsDict[i];
            
            // If word isn't in map yet, initialize its list.
            wordIndicesMap.putIfAbsent(word, new ArrayList<>());
            
            // Append the current index to the word's list.
            wordIndicesMap.get(word).add(i);
        }
    }

    /**
     * Returns the shortest distance between word1 and word2 in the array of strings.
     * Time Complexity: O(K + L) where K and L are the number of occurrences of w1 and w2.
     * Space Complexity: O(1) auxiliary space.
     */
    public int shortest(String word1, String word2) {
        // Retrieve the precomputed sorted lists of indices
        List<Integer> list1 = wordIndicesMap.get(word1);
        List<Integer> list2 = wordIndicesMap.get(word2);
        
        int minDistance = Integer.MAX_VALUE;
        int p1 = 0; // Pointer for list1
        int p2 = 0; // Pointer for list2
        
        // Two-pointer traversal
        // We stop as soon as one list is exhausted because the remaining elements 
        // in the other list will only increase the distance (they are strictly larger).
        while (p1 < list1.size() && p2 < list2.size()) {
            int index1 = list1.get(p1);
            int index2 = list2.get(p2);
            
            // Update the minimum distance found so far
            int currentDistance = Math.abs(index1 - index2);
            minDistance = Math.min(minDistance, currentDistance);
            
            // Optimization: The shortest possible distance between two different words is 1.
            // If we hit 1, we can return immediately and skip the rest of the work.
            if (minDistance == 1) {
                return 1;
            }
            
            // The crucial two-pointer logic:
            // To minimize the difference, we must increment the pointer 
            // that is pointing to the smaller index, hoping it "catches up" to the larger one.
            if (index1 < index2) {
                p1++;
            } else {
                p2++;
            }
        }
        
        return minDistance;
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        // Standard Example
        String[] words = {"practice", "makes", "perfect", "coding", "makes"};
        WordDistanceStudy wordDistance = new WordDistanceStudy(words);
        
        // shortest("coding", "practice") -> |3 - 0| = 3
        System.out.println("Distance 'coding' -> 'practice': " 
            + wordDistance.shortest("coding", "practice")); // Expected: 3
            
        // shortest("makes", "coding") -> "makes" is at 1 and 4, "coding" is at 3.
        // |4 - 3| = 1.
        System.out.println("Distance 'makes' -> 'coding': " 
            + wordDistance.shortest("makes", "coding")); // Expected: 1
            
        // Edge Case: Interleaved Words
        String[] interleaved = {"a", "b", "a", "b", "a", "b"};
        WordDistanceStudy interDistance = new WordDistanceStudy(interleaved);
        
        // The two pointers should gracefully walk up the arrays [0,2,4] and [1,3,5]
        System.out.println("Distance 'a' -> 'b' in interleaved array: " 
            + interDistance.shortest("a", "b")); // Expected: 1
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: HashMap Indexing + Two Pointers.
 * Key Observation: Storing indices in a map during a sequential pass yields naturally 
 * sorted lists. Two pointers can find the minimum absolute difference between two 
 * sorted arrays in linear time.
 * What to Memorize: The pointer increment rule: `if (val1 < val2) p1++; else p2++;`
 * Most Common Trap: Writing a nested loop over the two index lists. This creates an 
 * O(K*L) query time, which devolves to O(N^2) in the worst case (e.g., repeating "a b a b").
 * Mental Trigger: "Repeated distance queries" -> "Map to sorted lists + Two pointer catch-up".
 * ============================================================================
 */

