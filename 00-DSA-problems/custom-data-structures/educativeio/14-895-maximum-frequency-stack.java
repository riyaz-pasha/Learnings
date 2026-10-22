/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design a stack-like data structure that pushes elements and pops the most 
 * frequent element. If there is a tie for the most frequent element, the element 
 * closest to the top of the stack (most recently pushed) is removed.
 * 
 * Implement the FreqStack class:
 * - FreqStack(): Constructs an empty frequency stack.
 * - void push(int val): Pushes an integer val onto the stack.
 * - int pop(): Removes and returns the most frequent element in the stack.
 * 
 * Constraints: 0 <= val <= 10^9. At most 2 * 10^4 calls. Guaranteed at least 
 * one element before calling pop.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Are there negative values? 
 *    (Constraints say 0 to 10^9, so no, but it doesn't affect a Hash Map approach).
 * 2. Will `pop()` ever be called on an empty stack? 
 *    (Constraints guarantee at least one element, saving us from EmptyStackException boilerplate).
 * 3. Does popping an element completely remove it, or just reduce its frequency?
 *    (It removes that *specific instance* of the element, meaning its frequency drops by 1).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: We need to sort by Frequency (highest first), and resolve ties 
 *   by Recency (newest first). The binding constraint is doing this efficiently 
 *   without re-sorting the entire stack on every operation.
 * 
 * --- Approach 1: Brute Force (List + HashMap) ---
 * 1. Intuition: Keep a list of elements to preserve insertion order, and a map for frequencies.
 * 2. Try: `List<Integer> stack` and `HashMap<Integer, Integer> freq`. 
 *    `push`: append to list, update map. 
 *    `pop`: find max frequency in map. Scan the list *backwards* to find the first element 
 *    matching that max frequency. Remove it from the list and update the map.
 * 3. Why it works: Scanning backwards inherently resolves frequency ties by recency.
 * 4. Why it's too costly: `pop` takes O(N) time because we have to scan the list.
 * 5. Bottleneck: Linear search for the most recent max-frequency element.
 * 
 * Time Complexity: Push: O(1). Pop: O(N).
 * Space Complexity: O(N).
 * 
 * --- Approach 2: Priority Queue / Max-Heap ---
 * 1. Intuition: Heaps are great for keeping track of maximums.
 * 2. Try: Store objects `(val, frequency, pushSequenceNumber)`. 
 *    Use a `PriorityQueue` sorted by frequency descending, then sequence descending.
 * 3. Why it works: The heap naturally bubbles the most frequent, most recent element to the top.
 * 4. Why it's suboptimal: Inserting into or popping from a heap takes O(log N) time.
 * 5. Bottleneck: Heap rebalancing overhead. Can we do it in strict O(1) time?
 * 
 * Time Complexity: Push/Pop: O(log N).
 * Space Complexity: O(N).
 * 
 * --- Approach 3: Map of Stacks (Optimal) ---
 * 1. Intuition: Since we only increment/decrement frequency by exactly 1 at a time, we 
 *    don't need a heap to sort frequencies. We can just group elements by their frequency!
 * 2. Try: 
 *    - `HashMap<Integer, Integer> freq`: Maps `value -> current frequency`.
 *    - `HashMap<Integer, Stack<Integer>> group`: Maps `frequency -> Stack of values`.
 *    - `int maxFreq`: Tracks the current highest frequency across all elements.
 * 3. The Breakthrough: If I push `5` three times, I don't just put `5` in "bucket 3". 
 *    I put the *first* `5` in bucket 1, the *second* `5` in bucket 2, and the *third* `5` 
 *    in bucket 3. 
 *    When I `pop()`, I pop from the bucket corresponding to `maxFreq`. By using a Stack 
 *    for each bucket, the LIFO nature perfectly handles the "most recently pushed" tie-breaker!
 * 4. Which to write in an interview?
 *    Approach 3. It is a masterpiece of O(1) data structure composition.
 * 
 * Time Complexity: Push/Pop: O(1) — HashMap lookups and Stack operations are constant time.
 * Space Complexity: O(N) — Storing N elements across the stacks.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Continuous pops: When the `maxFreq` stack becomes empty after a pop, we must decrement 
 *   `maxFreq`. (Since frequency changes strictly by 1, the next highest frequency is ALWAYS `maxFreq - 1`).
 * - Pushing the same element repeatedly: e.g., push(5), push(5), push(5). 
 *   The stacks will cleanly look like: Stack 1: [5], Stack 2: [5], Stack 3: [5]. 
 *   Popping them correctly peels them back layer by layer.
 * 
 * ============================================================================
 * 4. ## Key Insights, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "An element with frequency F actually represents F distinct historical pushes." 
 *   Instead of moving an element from frequency bucket 2 to 3, you LEAVE it in bucket 2 
 *   and ADD it to bucket 3. When you pop from bucket 3, the historical record of it having 
 *   frequency 2 is perfectly waiting for you in bucket 2!
 * 
 * ASCII DRY RUN (Example 1)
 * push(5), push(7), push(5), push(7), push(4), push(5)
 * 
 * Stacks (Group Map):
 * Freq 3: [5]             <-- maxFreq = 3
 * Freq 2: [5, 7]
 * Freq 1: [5, 7, 4]
 * 
 * Action: pop()
 * 1. maxFreq is 3. Pop from Freq 3 Stack. Returns 5. 
 *    Freq 3 Stack is now empty. maxFreq drops to 2.
 *    Global frequency of 5 drops from 3 to 2.
 * 
 * Stacks (Group Map) after pop 1:
 * Freq 2: [5, 7]          <-- maxFreq = 2
 * Freq 1: [5, 7, 4]
 * 
 * Action: pop()
 * 1. maxFreq is 2. Pop from Freq 2 Stack. Returns 7! 
 *    (Notice how the LIFO stack naturally resolved the tie between 5 and 7!)
 *    Global frequency of 7 drops from 2 to 1.
 * 
 * Pitfalls:
 * - Trying to physically remove elements from lower frequency stacks when they get promoted. 
 *   If you push(5) a second time, do NOT remove it from `group.get(1)`. Leaving it there 
 *   is the entire secret to how this algorithm runs in O(1) time without rebuilding history.
 * 
 * Pattern Recognition: "O(1) Frequency + Tie-breaking by Time" -> Stack of Stacks / Map of Stacks.
 * 
 * Interview Script snippet:
 * "To achieve O(1) push and pop, a heap won't work. Instead, I'll map frequencies to Stacks. 
 * The genius of this approach is that if an element's frequency increases to 3, I don't move 
 * it out of the stack for frequency 2. I simply push it onto the stack for frequency 3. 
 * Because each frequency level is a Stack, it automatically preserves the Last-In-First-Out 
 * chronological order, natively solving the tie-breaker requirement in strict O(1) time."
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: What if we needed to implement `remove(val)` to delete an arbitrary element completely?
 * A1: The Map of Stacks approach would break because removing an element from the middle of a 
 *     Java Stack takes O(N) time. We would have to pivot to a Doubly Linked List for each 
 *     frequency bucket, allowing O(1) arbitrary removals (provided we also keep a Map pointing 
 *     to the DLL nodes).
 * 
 * Q2: How does this scale if constraints were 10^9 elements?
 * A2: The space complexity is strictly O(N). For 10^9 elements, we would need gigabytes of RAM. 
 *     Java's object overhead (creating `Integer` wrappers and `Stack` nodes) would cause massive 
 *     memory bloat. We would need to optimize using primitive arrays or a segmented architecture.
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class FreqStackStudy {

    static class FreqStack {
        // Maps a value to its current total frequency
        // e.g., if '5' has been pushed 3 times, freq.get(5) == 3
        private Map<Integer, Integer> freq;
        
        // Maps a frequency count to a Stack of values that have reached that frequency.
        // The Stack natively handles the "most recently pushed" tie-breaker.
        private Map<Integer, Stack<Integer>> group;
        
        // Tracks the highest frequency across all elements to know which stack to pop from in O(1).
        private int maxFreq;

        /**
         * Constructs an empty frequency stack.
         */
        public FreqStack() {
            freq = new HashMap<>();
            group = new HashMap<>();
            maxFreq = 0;
        }

        /**
         * Pushes an integer val onto the top of the stack.
         * Time: O(1)
         */
        public void push(int val) {
            // 1. Update the total frequency of the incoming value
            int currentFreq = freq.getOrDefault(val, 0) + 1;
            freq.put(val, currentFreq);
            
            // 2. Update the global max frequency if this element just set a new record
            if (currentFreq > maxFreq) {
                maxFreq = currentFreq;
            }
            
            // 3. Add the value to the stack corresponding to its NEW frequency.
            // computeIfAbsent ensures we initialize a new Stack if this frequency level was just reached.
            group.computeIfAbsent(currentFreq, k -> new Stack<>()).push(val);
        }

        /**
         * Removes and returns the most frequent element in the stack.
         * Time: O(1)
         */
        public int pop() {
            // 1. We always pop from the stack associated with the absolute maximum frequency.
            // The Stack's pop() method automatically gives us the most recently added element (tie-breaker).
            Stack<Integer> maxFreqStack = group.get(maxFreq);
            int val = maxFreqStack.pop();
            
            // 2. Decrement the frequency of the popped value in the global tracker.
            freq.put(val, freq.get(val) - 1);
            
            // 3. If the stack for the current maxFreq becomes empty, we drop maxFreq by 1.
            // Since frequencies only change by 1 at a time, we are guaranteed that if maxFreq drops,
            // the new max frequency is exactly (maxFreq - 1).
            if (maxFreqStack.isEmpty()) {
                maxFreq--;
            }
            
            return val;
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        FreqStack freqStack = new FreqStack();
        
        // Push elements as per Example 1
        freqStack.push(5); // The stack is [5]
        freqStack.push(7); // The stack is [5,7]
        freqStack.push(5); // The stack is [5,7,5]
        freqStack.push(7); // The stack is [5,7,5,7]
        freqStack.push(4); // The stack is [5,7,5,7,4]
        freqStack.push(5); // The stack is [5,7,5,7,4,5]
        
        // The most frequent element is 5 (count 3).
        System.out.println("Pop 1: " + freqStack.pop()); // Expected: 5
        
        // 5 and 7 both have frequency 2 now. 
        // 7 was pushed more recently at this frequency layer.
        System.out.println("Pop 2: " + freqStack.pop()); // Expected: 7
        
        // 5 is back to being the strict most frequent (count 2).
        System.out.println("Pop 3: " + freqStack.pop()); // Expected: 5
        
        // 4, 5, and 7 all have frequency 1.
        // 4 was pushed most recently out of all of them.
        System.out.println("Pop 4: " + freqStack.pop()); // Expected: 4
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: Map of Frequencies -> Stacks.
 * Key Observation: An element with frequency F represents F distinct historical pushes. 
 * By leaving the element in the buckets for frequencies 1 through F-1, and adding it 
 * to bucket F, we create a layered historical record. When popped from bucket F, its 
 * history natively falls back to bucket F-1.
 * What to Memorize: `group.computeIfAbsent(freq, k -> new Stack<>()).push(val)`. 
 * Most Common Trap: Trying to use a PriorityQueue, which drags the time complexity 
 * down to O(log N) instead of the optimal O(1).
 * Mental Trigger: "Frequency ties resolved by Recency" -> "Layered Stacks mapped by Count".
 * ============================================================================
 */











































import java.util.*;

/**
 * A stack-like data structure that removes the most frequent value.
 *
 * Rules:
 * 1. The value with the highest current frequency is removed first.
 * 2. If multiple values have the same frequency, remove the value
 *    whose most recent occurrence was pushed latest.
 *
 * Both push() and pop() take O(1) average time.
 */
class FreqStack {

    /*
     * PURPOSE:
     * Track the current frequency of every value.
     *
     * Example:
     *   numToFrequency = {5=3, 7=2, 4=1}
     *
     * This means:
     *   5 currently occurs 3 times.
     *   7 currently occurs 2 times.
     *   4 currently occurs 1 time.
     *
     * We need this map to know which frequency a value moves to
     * whenever it is pushed.
     */
    private final Map<Integer, Integer> numToFrequency;

    /*
     * PURPOSE:
     * Group values by the frequency they have just reached.
     *
     * Key   = frequency
     * Value = stack of values that reached that frequency, ordered
     *         from oldest (bottom) to newest (top).
     *
     * Example:
     *
     *   frequencyToNumbers = {
     *       1 -> [5, 7, 4],
     *       2 -> [5, 7],
     *       3 -> [5]
     *   }
     *
     * The rightmost value is the top of each stack.
     *
     * IMPORTANT:
     * These are not lists of every value whose current frequency equals
     * the key in arbitrary order. Each push records a frequency transition.
     * Each pop removes the latest entry from the maximum-frequency group.
     *
     * Why does this help?
     * Instead of searching all values for the most frequent one, we
     * directly access the stack corresponding to the maximum frequency.
     */
    private final Map<Integer, Deque<Integer>> frequencyToNumbers;

    /*
     * PURPOSE:
     * Remember the highest frequency currently present.
     *
     * Without this variable, pop() would have to search the frequency
     * groups to discover the largest nonempty frequency.
     *
     * Maintaining maxFrequency lets pop() find the correct group directly.
     *
     * Invariant:
     *   maxFrequency is the highest frequency among all values currently
     *   stored in the FreqStack.
     *
     * When the structure is empty, maxFrequency is 0.
     */
    private int maxFrequency;

    public FreqStack() {
        numToFrequency = new HashMap<>();
        frequencyToNumbers = new HashMap<>();
        maxFrequency = 0;
    }

    public void push(int value) {

        /*
         * STEP 1: Increase the value's current frequency.
         *
         * merge(value, 1, Integer::sum):
         *   - If value is absent, store frequency 1.
         *   - If value exists, increment its frequency by 1.
         *
         * Example:
         *   Before: numToFrequency = {5=2}
         *   push(5)
         *   After:  numToFrequency = {5=3}
         *
         * newFrequency is the frequency that value has just reached.
         */
        int newFrequency =
                numToFrequency.merge(value, 1, Integer::sum);

        /*
         * STEP 2: Record the value in the stack for its new frequency.
         *
         * If this frequency group does not exist yet, create an empty stack.
         *
         * addLast(value) places the value at the top of the stack.
         *
         * Why is this important?
         *
         * Suppose:
         *   push(5)
         *   push(7)
         *   push(5)
         *   push(7)
         *
         * Both 5 and 7 reach frequency 2, but 7 reaches it later.
         *
         * frequencyToNumbers[2] = [5, 7]
         *                                  ^
         *                                  top
         *
         * When frequency 2 is the maximum, 7 must be popped first.
         *
         * Using addLast() here means pop() must use pollLast() to preserve
         * last-in, first-out behavior within each frequency group.
         */
        frequencyToNumbers
                .computeIfAbsent(
                        newFrequency,
                        ignored -> new ArrayDeque<>()
                )
                .addLast(value);

        /*
         * STEP 3: Update the maximum frequency.
         *
         * A single push increases one value's frequency by exactly one.
         * Therefore, the maximum frequency can never jump by more than one.
         *
         * maxFrequency = max(maxFrequency, newFrequency)
         *
         * Example:
         *   maxFrequency = 2
         *   push(5), where newFrequency = 3
         *   maxFrequency becomes 3.
         *
         * If another value is pushed with frequency 1, the maximum remains
         * unchanged.
         */
        maxFrequency = Math.max(maxFrequency, newFrequency);
    }

    public int pop() {

        /*
         * STEP 1: Access the group with the maximum frequency.
         *
         * By our invariant, the most frequent value must be represented
         * in frequencyToNumbers[maxFrequency].
         *
         * We do not scan numToFrequency or the other groups.
         */
        Deque<Integer> mostFrequentCandidates =
                frequencyToNumbers.get(maxFrequency);

        /*
         * STEP 2: Remove the most recently recorded candidate in this group.
         *
         * pollLast() removes the newest entry because push() used addLast().
         *
         * This handles both requirements:
         *
         *   1. Highest frequency: we chose the maxFrequency group.
         *   2. Most recent among ties: we chose the top of that group.
         *
         * The problem guarantees that pop() is called only when nonempty,
         * so mostFrequentCandidates exists and contains an element.
         */
        int valueToRemove = mostFrequentCandidates.pollLast();

        /*
         * STEP 3: Decrease the frequency of the value we just removed.
         *
         * IMPORTANT:
         * Use the popped value's own recorded frequency.
         *
         * Its frequency was maxFrequency immediately before this pop,
         * so the new frequency will be maxFrequency - 1. However, looking
         * up the actual value's frequency expresses the invariant directly
         * and avoids coupling this update to the global maximum.
         */
        int updatedFrequency = numToFrequency.get(valueToRemove) - 1;

        if (updatedFrequency == 0) {

            /*
             * The value has no remaining occurrences.
             *
             * Remove its entry rather than keeping a stale frequency of 0.
             */
            numToFrequency.remove(valueToRemove);

        } else {

            /*
             * Some occurrences of the value still remain.
             * Update its frequency to reflect the removed occurrence.
             */
            numToFrequency.put(valueToRemove, updatedFrequency);
        }

        /*
         * STEP 4: Determine whether the maximum frequency must decrease.
         *
         * Consider maxFrequency = 3.
         *
         * If frequencyToNumbers[3] still contains values after this pop,
         * another eligible value remains at frequency 3.
         * Therefore, maxFrequency must stay 3.
         *
         * But if frequencyToNumbers[3] becomes empty, no value can still
         * have frequency 3. The maximum frequency must decrease to 2.
         *
         * Because we removed one entry from the maximum group, checking
         * whether that group is empty is sufficient.
         */
        if (mostFrequentCandidates.isEmpty()) {

            // Remove the now-empty frequency group to keep the map clean.
            frequencyToNumbers.remove(maxFrequency);

            // The next possible maximum frequency is one lower.
            maxFrequency--;
        }

        /*
         * Return the value selected by frequency first and recency second.
         */
        return valueToRemove;
    }
}

/*
 * TIME COMPLEXITY
 *
 * push(): O(1) average
 *   - One frequency-map update.
 *   - One frequency-group lookup/insertion.
 *   - One deque insertion.
 *   - One maximum-frequency comparison.
 *
 * pop(): O(1) average
 *   - One frequency-group lookup and deque removal.
 *   - One frequency-map lookup and update/removal.
 *   - One emptiness check and, if needed, one maximum-frequency decrement.
 *
 * SPACE COMPLEXITY: O(n)
 *   Let n be the number of currently stored occurrences.
 *
 *   numToFrequency stores at most one entry per distinct value: O(d).
 *   frequencyToNumbers collectively stores one entry per occurrence: O(n).
 *
 *   Since d <= n, total space is O(n).
 */




