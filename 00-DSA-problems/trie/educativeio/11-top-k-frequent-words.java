/**
 * ============================================================================
 * CODING PROBLEM STUDY NOTE: TOP K FREQUENT WORDS
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ----------------------------------------------------------------------------
 * - "If two words have the same frequency, they should be sorted lexicographically. Is that ascending (a to z) or descending?"
 *   (Crucial: confirms 'a' comes before 'b' in the final output).
 * - "Can K be larger than the total number of unique words?"
 *   (Prevents popping from an empty heap or out-of-bounds array access. Prompt says no, but always good to verify).
 * - "Is this a static list, or will words be streaming in continuously?"
 *   (If streaming, we'd need a dynamic structure like a HashMap combined with a TreeSet or LFU Cache pattern).
 * - "Are we optimizing for a massive dataset where memory is constrained, or is N small enough to fit in RAM?"
 *   (If memory is constrained and N is huge, we might use MapReduce or external sorting. If RAM is fine, Min-Heap is great).
 * 
 * 
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * [Core Requirement & Constraint]
 * We need to count frequencies and sort the top K elements by a composite key (frequency, 
 * then alphabetical order). The binding constraint is doing this efficiently when N 
 * (total words) is massive, but K is small. Sorting the entire list of unique words is wasteful.
 * 
 * APPROACH 1: HashMap + Full List Sort (The Brute Force / Naive)
 * 1. What I'd naturally try: Count all words using a `HashMap<String, Integer>`. Put all the 
 *    keys into a `List<String>`, sort the list using a custom comparator, and return the first K.
 * 2. Why it works: It perfectly applies both sorting rules to the entire dataset.
 * 3. Why it's too slow (for large N): We spend time sorting thousands of words that only 
 *    appear once, even if K is just 5. 
 * 4. What work is repeated: Fully sorting the bottom (N - K) elements is dead work.
 * 5. Time Complexity: O(N) to count + O(U log U * L) to sort, where U is unique words and 
 *    L is max word length. 
 * 6. Space Complexity: O(U * L) to store the unique words in the map and list.
 * 
 * APPROACH 2: HashMap + Min-Heap (The Intermediate / Industry Standard)
 * 1. What I'd naturally try next: To avoid sorting everything, I just need a data structure 
 *    that remembers only the "best" K elements. A Priority Queue (Min-Heap) restricted to size K!
 * 2. Why it works: As we iterate over the unique words, we push them into the Min-Heap. 
 *    If the heap size exceeds K, we pop the *smallest* element. By the end, only the largest 
 *    K elements remain.
 * 3. Why it's costly (comparatively): Heap operations take O(log K). It's great, but not 
 *    strictly linear time.
 * 4. What property removes the bottleneck: None for general comparison-based sorting, but 
 *    this is the accepted standard optimal solution for this problem.
 * 5. Time Complexity: O(N + U log K * L) 
 *    — O(N) to count, and O(U log K) to maintain the heap, comparing strings of length L.
 * 6. Space Complexity: O(U * L + K * L) 
 *    — HashMap takes O(U), Heap takes O(K).
 * 
 * APPROACH 3: HashMap + Bucket Sort + Trie (The Theoretical Ultra-Optimal)
 * 1. The defining observation: Frequencies are bounded by N. We can create an array of 
 *    size N+1 where `bucket[freq]` holds a Trie of all words with that frequency.
 * 2. Why it works: We iterate from `freq = N` down to 1. At each bucket, we do an in-order 
 *    DFS of the Trie to extract words in perfect lexicographical order until we have K words.
 * 3. Time Complexity: O(N * L) — Strictly linear time! No `log` factor.
 * 4. Space Complexity: O(N * L) — Heavy memory overhead for Trie nodes.
 * 
 * [What I'd write in an interview]
 * I would absolutely write Approach 2 (Min-Heap). It's the industry standard for "Top K" 
 * problems. The Bucket-Trie approach is a great verbal flex for O(N) time, but it's 
 * usually overkill to code in a 45-minute interview.
 * 
 * 
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * - Identical frequencies across the board: `["a", "b", "c", "d"]`, k=2. (Tests the tie-breaker heavily).
 * - K equals the number of unique words: (Heap must cleanly process everything without dropping valid words).
 * - Single element list: `["apple"]`, k=1.
 * 
 * 
 * 4. KEY INSIGHT, PITFALLS & DRY RUN
 * ----------------------------------------------------------------------------
 * [The Ultimate Pitfall: The Inverted Comparator]
 * When maintaining a Min-Heap of size K for the *Top* elements, the heap must be configured 
 * to evict the *Worst* elements. 
 * - Output requires: (Higher Freq) -> (Alphabetical 'a' to 'z').
 * - Therefore, Heap Top (the one to evict) must be: (Lower Freq) -> (Reverse Alphabetical 'z' to 'a').
 * If frequency is tied, we evict the lexicographically *larger* string. `w2.compareTo(w1)` 
 * instead of `w1.compareTo(w2)`. This trips up 90% of candidates.
 * 
 * [Dry Run: words=["i", "love", "leetcode", "i", "love", "coding"], k=2]
 * 1. Frequencies: "i":2, "love":2, "leetcode":1, "coding":1
 * 2. Heap (size 2), Rule: evict lowest freq, then highest alpha.
 *    - Add "i"(2). Heap: ["i"(2)]
 *    - Add "love"(2). Heap: ["love"(2), "i"(2)] -> ("love" > "i", so "love" is at the top to be evicted first if needed)
 *    - Add "leetcode"(1). Heap: ["leetcode"(1), "love"(2), "i"(2)]. Size > 2! Evict top.
 *      "leetcode" has lowest freq(1), so it gets evicted. Heap back to: ["love"(2), "i"(2)].
 *    - Add "coding"(1). Same thing, "coding" has freq(1) and is evicted.
 * 3. Heap remains: ["love"(2), "i"(2)].
 * 4. Pop to list: ["love", "i"].
 * 5. Reverse list to get correct order: ["i", "love"].
 * 
 * [Pattern Recognition]
 * "Top K..." or "K most..." -> Immediately think Min-Heap of size K (or Max-Heap of size K 
 * if looking for the K smallest).
 * 
 * 
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q: How would you solve this if the data is a continuous stream and we want `getTopK()` at any time?
 * A: I would use a `HashMap` for counts, paired with a `TreeSet` (or a Doubly Linked List with 
 *    frequency buckets, like in LFU Cache). The `TreeSet` keeps elements constantly sorted in 
 *    O(log N) time on every update, allowing `getTopK()` to run in O(K) time.
 * 
 * Q: How would you parallelize this if the input was 10 Terabytes?
 * A: MapReduce. Map phase: distribute chunks of text to workers to count local frequencies. 
 *    Reduce phase 1: aggregate local counts into global counts. 
 *    Reduce phase 2: Each worker finds their local Top K, and a master node merges these 
 *    local Top Ks into a final global Top K.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class TopKFrequentWordsStudyNote {

    public List<String> topKFrequent(String[] words, int k) {
        // 1. Count frequencies
        // Example state: {"i": 2, "love": 2, "leetcode": 1, "coding": 1}
        Map<String, Integer> freqMap = new HashMap<>();
        for (String word : words) {
            freqMap.put(word, freqMap.getOrDefault(word, 0) + 1);
        }

        // 2. Build the Min-Heap
        // WHY THIS COMPARATOR? 
        // We want to KEEP the highest frequency and lexicographically smallest words.
        // A Min-Heap pops the "smallest" element. Therefore, we must define "smallest" 
        // as the WORST candidate so it gets popped and thrown away.
        // Worst candidate = lower frequency. If tied, higher alphabetical order (e.g., 'z' is worse than 'a').
        PriorityQueue<String> minHeap = new PriorityQueue<>((w1, w2) -> {
            int freq1 = freqMap.get(w1);
            int freq2 = freqMap.get(w2);
            
            if (freq1 == freq2) {
                // Frequencies match. We want to EVICT the larger string.
                // So w2.compareTo(w1) puts the larger string at the top of the min-heap.
                return w2.compareTo(w1); 
            }
            // Frequencies differ. Evict the lower frequency.
            return freq1 - freq2;
        });

        // 3. Maintain Heap of size K
        for (String word : freqMap.keySet()) {
            minHeap.offer(word);
            
            // If we exceed capacity, throw away the worst candidate (top of the heap)
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        // 4. Extract results
        // The heap currently pops from worst to best.
        List<String> result = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            result.add(minHeap.poll());
        }

        // 5. Reverse to get best to worst (Descending frequency, Ascending alphabetical)
        Collections.reverse(result);
        
        return result;
    }

    // ============================================================================
    // TESTING & EDGE CASES
    // ============================================================================
    public static void main(String[] args) {
        TopKFrequentWordsStudyNote solution = new TopKFrequentWordsStudyNote();

        System.out.println("--- Standard Case ---");
        String[] words1 = {"i", "love", "leetcode", "i", "love", "coding"};
        int k1 = 2;
        // Frequencies: "i":2, "love":2, "leetcode":1, "coding":1
        // Ties broken alphabetically: "i" comes before "love".
        // Expected: ["i", "love"]
        System.out.println("Result: " + solution.topKFrequent(words1, k1));

        System.out.println("\n--- Tie-Breaker Extreme Case ---");
        String[] words2 = {"the", "day", "is", "sunny", "the", "the", "the", "sunny", "is", "is"};
        int k2 = 4;
        // Frequencies: "the":4, "is":3, "sunny":2, "day":1
        // Expected: ["the", "is", "sunny", "day"]
        System.out.println("Result: " + solution.topKFrequent(words2, k2));

        System.out.println("\n--- All Identical Frequencies ---");
        String[] words3 = {"zebra", "alpha", "bravo", "charlie"};
        int k3 = 2;
        // Frequencies: All 1.
        // Expected: ["alpha", "bravo"] (Strictly alphabetical extraction)
        System.out.println("Result: " + solution.topKFrequent(words3, k3));
        
        System.out.println("\n--- K equals Unique Words ---");
        String[] words4 = {"a", "a", "b"};
        int k4 = 2;
        // Expected: ["a", "b"]
        System.out.println("Result: " + solution.topKFrequent(words4, k4));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern:    HashMap + Min-Heap (PriorityQueue).
 * Key observation: To keep the Top K elements, you must maintain a heap of 
 *                  size K that constantly evicts the WORST elements.
 * To memorize:     The inverted comparator. If you want (High Freq, Low Alpha) 
 *                  in the output, the heap must pop (Low Freq, High Alpha). 
 *                  `freq1 == freq2 ? w2.compareTo(w1) : freq1 - freq2`.
 * Common trap:     Returning the heap directly. You must pop all elements into 
 *                  a list and `Collections.reverse()` it because the heap pops 
 *                  the worst valid element first (rank K, then K-1, ..., rank 1).
 * Mental trigger:  "Top K frequent..." -> HashMap + Min-Heap of size K.
 * ============================================================================
 */


import java.util.*;

public class TopKFrequentWords {

    public static List<String> topKFrequentWords(String[] words, int k) {

        // -----------------------------
        // STEP 1: Build Frequency Map
        // -----------------------------
        // Count occurrences of each word
        Map<String, Integer> freqMap = new HashMap<>();
        for (String word : words) {
            freqMap.merge(word, 1, Integer::sum);
        }

        // -----------------------------
        // STEP 2: Min Heap (size = k)
        // -----------------------------
        // Heap will store "worst" element on top
        // WHY?
        // Because when size > k → we remove worst element
        //
        // Comparator logic:
        // 1. Lower frequency → worse (comes first in min heap)
        // 2. If same frequency → lexicographically larger → worse
        //
        // IMPORTANT:
        // We reverse lexicographical order for heap so that
        // lexicographically larger word is removed first
        PriorityQueue<Map.Entry<String, Integer>> pq =
            new PriorityQueue<>(
                Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                    .thenComparing((a, b) -> b.getKey().compareTo(a.getKey()))
            );

        // -----------------------------
        // STEP 3: Maintain Top K Elements
        // -----------------------------
        for (Map.Entry<String, Integer> entry : freqMap.entrySet()) {

            pq.offer(entry); // add current word

            // If heap size exceeds k → remove worst element
            if (pq.size() > k) {
                pq.poll();
            }
        }

        // -----------------------------
        // STEP 4: Build Result
        // -----------------------------
        // Heap gives elements from worst → best
        // So we reverse order while building result
        LinkedList<String> result = new LinkedList<>();

        while (!pq.isEmpty()) {
            result.addFirst(pq.poll().getKey());
        }

        return result;
    }

    // Quick test
    public static void main(String[] args) {
        String[] words = {"i", "love", "leetcode", "i", "love", "coding"};
        int k = 2;

        System.out.println(topKFrequentWords(words, k));
        // Expected: ["i", "love"]
    }
}

import java.util.*;

public class TopKFrequentWords_Heap {

    public static List<String> topKFrequent(String[] words, int k) {

        // Step 1: Frequency Map
        Map<String, Integer> freqMap = new HashMap<>();
        for (String word : words) {
            freqMap.merge(word, 1, Integer::sum);
        }

        // Step 2: Min Heap
        PriorityQueue<String> minHeap = new PriorityQueue<>((a, b) -> {
            int freqCompare = Integer.compare(freqMap.get(a), freqMap.get(b)); // min freq first
            if (freqCompare == 0) {
                return b.compareTo(a); // reverse lexicographical (worse word first)
            }
            return freqCompare;
        });

        // Step 3: Maintain size k
        for (String word : freqMap.keySet()) {
            minHeap.offer(word);
            if (minHeap.size() > k) {
                minHeap.poll(); // remove worst
            }
        }

        // Step 4: Build result (reverse order)
        List<String> result = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            result.add(minHeap.poll());
        }

        Collections.reverse(result); // because min heap
        return result;
    }
}

import java.util.*;
import java.util.stream.*;

public class TopKFrequentWords_Sorting {

    public static List<String> topKFrequent(String[] words, int k) {

        // Step 1: Frequency Map
        Map<String, Integer> freqMap = new HashMap<>();
        for (String word : words) {
            freqMap.merge(word, 1, Integer::sum); // cleaner Java 8+
        }

        // Step 2: Sort
        return freqMap.keySet()
                .stream()
                .sorted((a, b) -> {
                    int freqCompare = Integer.compare(freqMap.get(b), freqMap.get(a)); // desc freq
                    if (freqCompare == 0) {
                        return a.compareTo(b); // lexicographical
                    }
                    return freqCompare;
                })
                .limit(k)
                .toList();
    }
}

import java.util.*;

public class TopKFrequentWords_Bucket {

    public static List<String> topKFrequent(String[] words, int k) {

        // ---------------------------------------------------
        // STEP 1: Build Frequency Map
        // ---------------------------------------------------
        // Example:
        // words = ["i", "love", "leetcode", "i", "love", "coding"]
        //
        // freqMap:
        // i -> 2
        // love -> 2
        // leetcode -> 1
        // coding -> 1
        Map<String, Integer> frequencyMap = new HashMap<>();
        for (String word : words) {
            frequencyMap.merge(word, 1, Integer::sum);
        }

        // ---------------------------------------------------
        // STEP 2: Create Buckets (Index = Frequency)
        // ---------------------------------------------------
        // Idea:
        // Instead of sorting all words, we group words by frequency.
        //
        // bucket[i] = list of words having frequency = i
        //
        // Max possible frequency = words.length
        //
        // Example:
        // bucket[2] -> ["i", "love"]
        // bucket[1] -> ["leetcode", "coding"]
        List<String>[] buckets = new List[words.length + 1];

        for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
            String word = entry.getKey();
            int freq = entry.getValue();

            if (buckets[freq] == null) {
                buckets[freq] = new ArrayList<>();
            }

            buckets[freq].add(word);
        }

        // ---------------------------------------------------
        // STEP 3: Traverse Buckets from High → Low Frequency
        // ---------------------------------------------------
        // Why reverse?
        // Because we want highest frequency words first
        //
        // IMPORTANT:
        // If multiple words have same frequency,
        // we must return them in lexicographical order
        //
        // Example:
        // bucket[2] = ["love", "i"]
        // After sorting → ["i", "love"]
        List<String> result = new ArrayList<>();

        for (int freq = buckets.length - 1; freq >= 0 && result.size() < k; freq--) {

            List<String> sameFreqWords = buckets[freq];

            if (sameFreqWords == null) continue;

            // ---------------------------------------------------
            // CRITICAL STEP: Sort lexicographically
            // ---------------------------------------------------
            // Why needed?
            // Because problem requires:
            // If frequency is same → lexicographically smaller first
            //
            // Example:
            // ["love", "i"] → ["i", "love"]
            Collections.sort(sameFreqWords);

            // ---------------------------------------------------
            // Add words to result
            // ---------------------------------------------------
            for (String word : sameFreqWords) {
                result.add(word);

                // Stop when we collected top k
                if (result.size() == k) {
                    break;
                }
            }
        }

        return result;
    }

    // Quick test
    public static void main(String[] args) {
        String[] words = {"i", "love", "leetcode", "i", "love", "coding"};
        int k = 2;

        System.out.println(topKFrequent(words, k));
        // Expected Output: ["i", "love"]
    }
}
