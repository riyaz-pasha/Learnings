/**
 * STANDALONE STUDY NOTE: RANSOM NOTE
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Can the ransom note be longer than the magazine?" (If yes, we can immediately return false without any processing).
 * - "Are characters strictly lowercase English letters?" (Confirming constraints guarantees we can use a small fixed-size array instead of a HashMap).
 * - "Can we modify the magazine string in place?" (Strings are immutable in Java, so finding and 'crossing out' characters will cost extra memory).
 * - "Is the magazine provided all at once in memory, or is it a stream?" (A stream would force us to read the whole magazine into a map first).
 * - "Are whitespace and punctuation considered part of the note?" (Changes the data structure size if we need to track spaces).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * The problem asks us to verify a "subset" relationship based on character frequencies: for every 
 * character in the ransom note, the magazine must have at least that many instances. The binding 
 * constraint is ensuring we don't reuse a character from the magazine more times than it appears.
 * 
 * --- APPROACH 1: BRUTE FORCE (Search and Cross-Out) ---
 * 1. What I'd naturally try: For every letter I need for my ransom note, I'll scan the magazine. 
 *    If I find it, I'll "cross it out" so I don't use it again, and move to the next letter.
 * 2. Why it works: It perfectly simulates the physical act of cutting letters out of a magazine.
 * 3. Why it's too slow/costly: In Java, strings are immutable. "Crossing out" a character means 
 *    creating a brand new string via `substring` or `replaceFirst` every single time we find a match.
 * 4. What work is repeated: We repeatedly rescan the magazine string from the beginning for every 
 *    single character in the note, and we repeatedly allocate new strings.
 * 5. Cost Breakdown:
 *    - Time Complexity: O(R * M) — because for each of the R characters in the note, we might scan 
 *      up to M characters in the magazine, and string replacement takes O(M) time.
 *    - Space Complexity: O(M) — because we allocate a new modified string of size M-1, M-2, etc., 
 *      at each step to represent the "crossed out" state.
 * 
 * --- APPROACH 2: INTERMEDIATE (HashMap Frequency Counting) ---
 * 1. The Observation: We don't care *where* the letters are in the magazine, only *how many* there are. 
 *    If we tally up the magazine's inventory first, we can just check our shopping list (ransom note) 
 *    against that inventory.
 * 2. How it leads to the next step: We iterate through the magazine to build a `HashMap<Character, Integer>` 
 *    acting as an inventory counter. Then we iterate through the ransom note, decrementing the count 
 *    for each character. If a character isn't there or its count drops below zero, we fail.
 * 3. Why it works: It decouples the two strings, turning a nested loop (O(R*M)) into sequential loops (O(R+M)).
 * 4. Cost Breakdown:
 *    - Time Complexity: O(R + M) — because we do one pass over the magazine (length M) and one pass 
 *      over the note (length R), doing O(1) hash map operations.
 *    - Space Complexity: O(U) where U is unique characters in the magazine. In Java, this means 
 *      allocating Object wrappers (Character, Integer) and Map Node objects for every unique letter.
 * 
 * --- APPROACH 3: OPTIMAL (Array-based Frequency Counting) ---
 * 1. The Observation: The problem strictly limits the domain to lowercase English letters. We only 
 *    have 26 possible "keys". A HashMap is bloated overkill for a dense, tiny domain.
 * 2. How it leads to the next step: We replace the HashMap with a simple `int[26]` array. The index 
 *    acts as the character (`char - 'a'`) and the value at that index acts as the count.
 * 3. Which one I'd write in an interview: Approach 3 (Array). It proves I know how to optimize for 
 *    known constraints, removes Java autoboxing overhead, and reduces memory fragmentation to zero.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(R + M) — because we linearly scan the magazine and then the note, using 
 *      O(1) primitive array lookups.
 *    - Space Complexity: O(1) — because we allocate exactly one array of 26 integers (104 bytes), 
 *      which remains constant regardless of whether the strings are 10 chars or 10 million chars.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Note is longer than Magazine: `note="abc", mag="ab"`. Must immediately return false.
 * - Missing characters: `note="z", mag="abc"`.
 * - Insufficient quantity: `note="aab", mag="ab"`. (Correctly tests the decrement logic).
 * - Exact match: `note="abc", mag="abc"`.
 * - Magazine has vast excess: `note="a", mag="abcdefghijklmnopqrstuvwxyz"`.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * The key difference between Anagram and Ransom Note is *symmetry*. Anagram requires exact equality. 
 * Ransom Note requires a *subset*. We don't care if the magazine has left over letters.
 * 
 * Mag: "aabcc"   -->   [ a:2, b:1, c:2, d:0 ... ]  (Inventory)
 * Note: "abc"    -->  -[ a:1, b:1, c:1, d:0 ... ]  (Deductions)
 *                      --------------------------
 * Result:              [ a:1, b:0, c:1, d:0 ... ]  -> All >= 0? TRUE!
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: note="aab", mag="ab" (The "Insufficient Quantity" Pitfall)
 * 
 * Initialization: map = [0,0,0...,0] (length 26)
 * Early check: note.length (3) > mag.length (2) -> RETURN FALSE immediately!
 * 
 * Let's trace note="aab", mag="aba" to see the array in action:
 * Step 1: Populate Inventory from mag ("aba")
 *   i=0, char='a' -> map['a'-'a']++  -> map[0]=1
 *   i=1, char='b' -> map['b'-'a']++  -> map[1]=1
 *   i=2, char='a' -> map['a'-'a']++  -> map[0]=2
 *   State: map = [2, 1, 0, 0...]
 * 
 * Step 2: Deduct using note ("aab")
 *   i=0, char='a' -> map['a'-'a']--  -> map[0]=1. Is it < 0? No.
 *   i=1, char='a' -> map['a'-'a']--  -> map[0]=0. Is it < 0? No.
 *   i=2, char='b' -> map['b'-'a']--  -> map[1]=0. Is it < 0? No.
 *   Loop finishes without going negative. Return TRUE.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT (How to talk through it)
 * "Since the characters are strictly lowercase English letters, we don't need a heavy HashMap. 
 * I'll use a fixed-size integer array of length 26 to act as our inventory counter. 
 * First, I'll do an early exit: if the note is longer than the magazine, it's impossible, so return false.
 * Then, I'll iterate through the magazine, incrementing the count for each letter. 
 * Finally, I'll iterate through the ransom note, decrementing the corresponding letter's count. 
 * If a count ever drops below zero, it means the magazine didn't have enough of that specific letter, 
 * so I return false. If we get through the whole note, we return true. 
 * That gives us O(R + M) time and O(1) space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if the magazine is too large to fit in memory (e.g., streaming from disk)?
 * A: Because we only need the frequency counts, we don't need to hold the magazine in memory. 
 *    We can stream the magazine byte-by-byte and build the `int[26]` array, which only takes 104 bytes. 
 *    Once the stream is done, we process the in-memory ransom note against the array.
 * 
 * Q: What if we want to build *multiple* different ransom notes, and check if *all* of them can 
 *    be built from a single magazine?
 * A: We would build the `int[26]` inventory from the magazine once. Then, instead of directly 
 *    modifying it, we pass a clone of the array (`inventory.clone()`) to each ransom note check. 
 *    (Or if they draw from the same shared pool simultaneously, we keep deducting from the shared array).
 * 
 * Q: What if the character set is full Unicode?
 * A: The `int[26]` array fails. I would fall back to a `HashMap<Character, Integer>` to handle 
 *    the sparse, unbounded character domain while maintaining O(1) average lookup time.
 */

public class RansomNote {

    /**
     * APPROACH 3: OPTIMAL (Array Frequency Map)
     * Time: O(R + M), Space: O(1)
     */
    public boolean canConstructOptimal(String ransomNote, String magazine) {
        // [Why needed]: Physical impossibility check. A 5-letter note can't be made from 4 letters.
        // [What it does]: Prevents useless O(M) iteration for obvious failure cases.
        if (ransomNote.length() > magazine.length()) {
            return false;
        }

        // [Why needed]: We need an inventory to track available letters.
        // Array of size 26 maps 'a' to index 0, 'z' to index 25.
        int[] inventory = new int[26];

        // Phase 1: Build the inventory from the magazine
        for (int i = 0; i < magazine.length(); i++) {
            // charAt(i) - 'a' converts ASCII value (e.g. 97 for 'a') to a 0-25 index.
            inventory[magazine.charAt(i) - 'a']++;
        }

        // Phase 2: Deduct letters required by the ransom note
        for (int i = 0; i < ransomNote.length(); i++) {
            int charIndex = ransomNote.charAt(i) - 'a';
            
            // Deduct the required character from our inventory
            inventory[charIndex]--;
            
            // [Why needed]: If the count drops below 0, we needed more instances 
            // of this letter than the magazine possessed.
            if (inventory[charIndex] < 0) {
                return false;
            }
        }

        // If we successfully fulfilled all letter requirements, it can be constructed.
        return true;
    }

    /**
     * APPROACH 2: INTERMEDIATE (HashMap)
     * Time: O(R + M), Space: O(U) -> High constant overhead due to Object Boxing.
     */
    public boolean canConstructHashMap(String ransomNote, String magazine) {
        if (ransomNote.length() > magazine.length()) return false;

        java.util.Map<Character, Integer> counts = new java.util.HashMap<>();

        // Heavy memory overhead: Every unique character creates Map Nodes and boxed Integers.
        for (char c : magazine.toCharArray()) {
            counts.put(c, counts.getOrDefault(c, 0) + 1);
        }

        for (char c : ransomNote.toCharArray()) {
            int currentCount = counts.getOrDefault(c, 0);
            
            if (currentCount == 0) {
                // Character not in magazine, or we ran out of it.
                return false; 
            }
            // Update the count to reflect we consumed one instance.
            counts.put(c, currentCount - 1);
        }

        return true;
    }

    /**
     * APPROACH 1: BRUTE FORCE (String Modification)
     * Time: O(R * M), Space: O(M) -> Very inefficient in Java.
     */
    public boolean canConstructBruteForce(String ransomNote, String magazine) {
        if (ransomNote.length() > magazine.length()) return false;

        // Using StringBuilder because pure String re-assignment creates too much garbage,
        // but even this is O(M) space and O(M) time per deletion.
        StringBuilder magBuilder = new StringBuilder(magazine);

        for (char c : ransomNote.toCharArray()) {
            // Find if the required character exists in the remaining magazine
            int index = magBuilder.indexOf(String.valueOf(c));
            
            if (index == -1) {
                return false; // Not found
            }
            
            // "Cross it out" by deleting it so it cannot be reused.
            // This is an O(M) operation because elements must shift.
            magBuilder.deleteCharAt(index);
        }
        
        return true;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Frequency counting & verification (Subset check).
     * 2. Key observation: We don't care about order, only about quantities. Early exit if note > magazine.
     * 3. Memorize vs Understand: Understand that Anagram == Exact Match, Ransom Note == Subset. 
     *    Both use the same `int[26]` frequency map pattern.
     * 4. Most common trap: Forgetting to check if `ransomNote.length() > magazine.length()`, 
     *    which wastes time processing massive magazines for an obviously impossible note.
     * 5. Mental trigger: "subset of lowercase letters" -> `int[26] map` + `if (count < 0) return false`.
     */
    public static void main(String[] args) {
        RansomNote solution = new RansomNote();

        // Test Case 1: Simple Failure (Missing char)
        System.out.println("Test 1 (a from b): " + 
            solution.canConstructOptimal("a", "b")); // Expected: false

        // Test Case 2: Insufficient Quantity Pitfall
        System.out.println("Test 2 (aa from ab): " + 
            solution.canConstructOptimal("aa", "ab")); // Expected: false

        // Test Case 3: Standard Success
        System.out.println("Test 3 (aa from aab): " + 
            solution.canConstructOptimal("aa", "aab")); // Expected: true

        // Test Case 4: Length Edge Case Early Exit
        System.out.println("Test 4 (Note > Mag): " + 
            solution.canConstructOptimal("hello", "hi")); // Expected: false
            
        // Test Case 5: Exact Match
        System.out.println("Test 5 (Exact Match): " + 
            solution.canConstructOptimal("code", "code")); // Expected: true
    }
}

