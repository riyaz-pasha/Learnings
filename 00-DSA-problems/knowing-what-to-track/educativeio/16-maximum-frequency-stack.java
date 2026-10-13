/**
 * STANDALONE STUDY NOTE: MAXIMUM FREQUENCY STACK
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "What is the expected time complexity for both Push and Pop operations?" (The goal is O(1) time for both operations).
 * - "How are ties broken when multiple elements share the highest frequency?" (The prompt explicitly specifies that the most recently pushed element among them is removed and returned).
 * - "What is the range of values being pushed?" (0 to 10^4, which means we can use lookup structures if needed, though a general-purpose map is safer for arbitrary integers).
 * - "Can Pop be called when the stack is empty?" (Constraints guarantee at least one element is in the stack before calling Pop).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need a stack-like data structure where `push` adds elements and `pop` removes the *most frequent* element, 
 * breaking ties by recency. The binding constraint is that finding the max frequency element naively by scanning 
 * frequencies takes O(U) or O(N) time, which violates the requirement for efficient O(1) stack operations.
 * 
 * --- APPROACH 1: BRUTE FORCE (Frequency Map + Linear Scan) ---
 * 1. What I'd naturally try: Maintain a standard `List` or `Stack` to store all pushed elements, and a `HashMap` 
 *    to track frequencies. On `pop()`, iterate through the entire stack to find the element with the highest frequency, 
 *    breaking ties by scanning from top to bottom (most recent).
 * 2. Why it works: It strictly adheres to both frequency and tie-breaking rules.
 * 3. Why it's too slow/costly: Scanning the stack on every `pop()` takes O(N) time, leading to O(N^2) total time 
 *    for a sequence of pops.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(1) for `push`, O(N) for `pop`.
 *    - Space Complexity: O(N) — to store elements and frequencies.
 * 
 * --- APPROACH 2: INTERMEDIATE (Priority Queue / Max-Heap) ---
 * 1. The Observation: We need quick access to the element with the highest frequency and most recent timestamp. 
 *    We can use a custom wrapper object containing `(value, frequency, timestamp)` and store them in a Max-Heap 
 *    (PriorityQueue).
 * 2. How it leads to the next step: `push` updates the frequency map and inserts into the heap. `pop` extracts 
 *    the root of the heap in O(log N) time.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(log N) for both `push` and `pop` due to heap reordering.
 *    - Space Complexity: O(N) — heap and frequency map storage.
 * 
 * --- APPROACH 3: OPTIMAL (Frequency-Bucketed Stacks + Global Max Frequency Tracker) ---
 * 1. The Observation: Notice that frequencies are incremented sequentially (1, 2, 3...). Instead of a single heap, 
 *    we can maintain a map of frequency buckets: `HashMap<Integer, Stack<Integer>> freqStackMap`. 
 *    - Each frequency level has its own independent stack of elements that reached that frequency.
 *    - We also maintain a global `maxFreq` integer tracker.
 * 2. How it leads to the next step:
 *    - On `push(val)`: Increment `val`'s frequency in a `freqMap`. Let this new frequency be `f`. Update `maxFreq = max(maxFreq, f)`. 
 *      Push `val` onto `freqStackMap.get(f)`. (This naturally preserves LIFO/recency order for ties!).
 *    - On `pop()`: Look at `freqStackMap.get(maxFreq)`. Pop the top element (which is the most recent among the most frequent). 
 *      Decrement its frequency in `freqMap`. If `freqStackMap.get(maxFreq).isEmpty()`, decrement `maxFreq`. Return the element.
 * 3. Which one I'd write in an interview: Approach 3 (Frequency Buckets + MaxFreq Tracker). It achieves true O(1) 
 *    time complexity for both `push` and `pop`, perfectly handling the recency tie-breaker via nested stacks.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(1) for both `push` and `pop` — map lookups, stack pushes/pops, and scalar maxFreq updates 
 *      all execute in constant time.
 *    - Space Complexity: O(N) — because every pushed element is stored in the frequency map and bucket stacks.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Single element pushed and popped: `push(5)`, `pop()` -> Returns `5`, maxFreq drops back to 0.
 * - Tie-breaking check: `push(5)`, `push(7)`, `push(5)`, `push(7)` -> Frequencies: 5:2, 7:2. 
 *   `pop()` should return `7` (most recent among frequency 2). Next `pop()` returns `5`.
 * - Frequency jumping: Elements reaching frequencies 1, 2, 3... handled cleanly by the bucket map.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Frequency Bucket Architecture:
 * 
 * Pushes: push(5), push(7), push(5), push(7), push(4), push(5)
 * 
 * freqMap:
 * 4 -> 1
 * 5 -> 3
 * 7 -> 2
 * 
 * maxFreq = 3
 * 
 * freqStackMap (Buckets):
 * Freq 1: [5, 7, 4]  (Stack)
 * Freq 2: [5, 7]     (Stack)
 * Freq 3: [5]        (Stack)
 * 
 * Pop operation looks at `freqStackMap.get(maxFreq)` (Freq 3), pops `5`. 
 * Decrements 5's frequency to 2. If Freq 3 stack becomes empty, `maxFreq` decreases to 2.
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target Operations: push(5), push(7), push(5), push(7), push(4), push(5), pop(), pop(), pop(), pop()
 * 
 * - push(5): freqMap {5:1}, maxFreq = 1. freqStackMap {1: [5]}
 * - push(7): freqMap {5:1, 7:1}, maxFreq = 1. freqStackMap {1: [5, 7]}
 * - push(5): freqMap {5:2, 7:1}, maxFreq = 2. freqStackMap {1: [5, 7], 2: [5]}
 * - push(7): freqMap {5:2, 7:2}, maxFreq = 2. freqStackMap {1: [5, 7], 2: [5, 7]}
 * - push(4): freqMap {5:2, 7:2, 4:1}, maxFreq = 2. freqStackMap {1: [5, 7, 4], 2: [5, 7]}
 * - push(5): freqMap {5:3, 7:2, 4:1}, maxFreq = 3. freqStackMap {1: [5, 7, 4], 2: [5, 7], 3: [5]}
 * 
 * - pop():
 *   - maxFreq is 3. Look at freqStackMap.get(3) -> Stack has [5]. Pop `5`.
 *   - Update freqMap: 5's count becomes 2.
 *   - Freq 3 stack is now empty -> maxFreq decreases to 2.
 *   - Returns `5`.
 * 
 * - pop():
 *   - maxFreq is 2. Look at freqStackMap.get(2) -> Stack has [5, 7]. Top is `7` (most recent!). Pop `7`.
 *   - Update freqMap: 7's count becomes 1.
 *   - Freq 2 stack still has [5], so maxFreq stays 2.
 *   - Returns `7`.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "To achieve O(1) time complexity for both push and pop while satisfying the frequency and recency tie-breaker rules, 
 * we can maintain two maps: a frequency map that tracks how many times each element has appeared, and a frequency-bucket 
 * map (`freqStackMap`) that maps each frequency level to a stack of elements that reached that frequency. We also maintain 
 * a global `maxFreq` tracker. When we push, we increment the element's frequency, update `maxFreq`, and push it onto the 
 * stack corresponding to its new frequency—which naturally preserves LIFO order for ties. When we pop, we pull from the top 
 * of the stack at `maxFreq`, decrement its frequency, and adjust `maxFreq` if that bucket empties. This gives us O(1) time 
 * for both operations and O(N) space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: How would you handle memory cleanup for frequencies that drop to 0?
 * A: In our bucket map approach, when a frequency stack becomes empty, we can optionally remove the entry from 
 *    `freqStackMap` to keep memory tight, though leaving empty stacks is harmless given constraints.
 * 
 * Q: What if elements could be deleted or removed arbitrarily, not just popped by max frequency?
 * A: That would transform the problem into a generalized data structure requiring a Balanced Binary Search Tree (like a TreeMap) 
 *    or augmented doubly-linked lists, which would increase push/pop times to O(log N).
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class FreqStack {
    // Maps each element to its current frequency count
    private final Map<Integer, Integer> freqMap;
    
    // Maps each frequency level to a Stack of elements that have reached that frequency level.
    // This naturally preserves the LIFO (recency) order for breaking ties among elements with the same frequency.
    private final Map<Integer, Stack<Integer>> freqStackMap;
    
    // Tracks the current maximum frequency present in the stack
    private int maxFreq;

    /**
     * CONSTRUCTOR
     * Initializes the Frequency Stack.
     * Time: O(1), Space: O(1)
     */
    public FreqStack() {
        this.freqMap = new HashMap<>();
        this.freqStackMap = new HashMap<>();
        this.maxFreq = 0;
    }

    /**
     * PUSH METHOD
     * Pushes an integer data onto the top of the stack.
     * Time: O(1), Space: O(1) amortized
     */
    public void push(int val) {
        // [What it does]: Increment the frequency of the pushed value
        int freq = freqMap.getOrDefault(val, 0) + 1;
        freqMap.put(val, freq);

        // [Why needed]: Update global max frequency if this element surpassed previous records
        if (freq > maxFreq) {
            maxFreq = freq;
        }

        // [What it does]: Retrieve or create the stack for this specific frequency level, 
        // and push the value onto it. LIFO order inside this stack handles the tie-breaking rule.
        freqStackMap.computeIfAbsent(freq, k -> new Stack<>()).push(val);
    }

    /**
     * POP METHOD
     * Removes and returns the most frequent element in the stack (breaking ties by recency).
     * Time: O(1), Space: O(1) amortized
     */
    public int pop() {
        // [Why needed]: Access the stack corresponding to the current maximum frequency
        Stack<Integer> mostFrequentStack = freqStackMap.get(maxFreq);
        
        // Pop the most recent element from the max-frequency stack
        int poppedVal = mostFrequentStack.pop();

        // [What it does]: Decrement the frequency of the popped value in our frequency map
        freqMap.put(poppedVal, freqMap.get(poppedVal) - 1);

        // [Why needed]: If the stack at maxFreq becomes empty, it means no elements currently 
        // have that maximum frequency, so we decrement maxFreq.
        if (mostFrequentStack.isEmpty()) {
            maxFreq--;
        }

        return poppedVal;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Frequency-bucketed stacks (`Map<Integer, Stack<Integer>>`) + Global `maxFreq` tracker.
     * 2. Key observation: Stacks naturally enforce the tie-breaking rule (most recently pushed) because LIFO 
     *    order keeps recent arrivals at the top of their frequency bucket.
     * 3. Memorize vs Understand: Understand why maintaining separate frequency stacks avoids expensive O(N) or O(log N) heap searches.
     * 4. Most common trap: Forgetting to decrement `maxFreq` when a frequency bucket stack becomes completely empty.
     * 5. Mental trigger: "Stack with most frequent element pop" -> `freqMap` + `freqStackMap` + `maxFreq`.
     */
    public static void main(String[] args) {
        FreqStack freqStack = new FreqStack();

        freqStack.push(5);
        freqStack.push(7);
        freqStack.push(5);
        freqStack.push(7);
        freqStack.push(4);
        freqStack.push(5);

        System.out.println("Pop 1 (Expected 5): " + freqStack.pop()); // freq 3, most recent 5
        System.out.println("Pop 2 (Expected 7): " + freqStack.pop()); // freq 2 tie between 5 and 7 -> 7 is most recent
        System.out.println("Pop 3 (Expected 5): " + freqStack.pop()); // freq 2 -> 5
        System.out.println("Pop 4 (Expected 4): " + freqStack.pop()); // freq 1 tie among 5, 7, 4 -> 4 is most recent
    }
}

