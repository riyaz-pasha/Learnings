import java.util.*;

/**
 * PARTITION LABELS - COMPREHENSIVE SOLUTION GUIDE
 * 
 * PROBLEM UNDERSTANDING:
 * =====================
 * Given string s, partition into maximum number of parts where:
 * - Each letter appears in at most ONE part
 * - After concatenating parts, we get original string s
 * - Return sizes of these parts
 * 
 * Example: s = "ababcbacadefegdehijhklij"
 * Output: [9, 7, 8]
 * Partitions: "ababcbaca", "defegde", "hijhklij"
 * 
 * Why [9, 7, 8]?
 * - 'a' appears in positions 0,2,4,6,8 → must include all up to position 8
 * - 'b' appears in positions 1,3,5,7 → all within first 8 positions
 * - 'c' appears in positions 4,7,8 → all within first 8 positions
 * - So first partition must be s[0..8] = "ababcbaca"
 * 
 * KEY INSIGHTS FOR INTERVIEWS:
 * ============================
 * 1. This is a GREEDY problem
 * 2. For each character, we need to know its LAST occurrence
 * 3. A partition can end when we've seen all occurrences of chars in that partition
 * 4. Strategy: Expand partition boundary as we discover characters
 * 5. Similar to "merge intervals" but for character positions
 * 
 * CRITICAL INSIGHT - THE GREEDY APPROACH:
 * =======================================
 * Think of it as a "range merging" problem:
 * - Each character has a range [first_occurrence, last_occurrence]
 * - If ranges overlap, they must be in same partition
 * - Find minimal merged ranges
 * 
 * Example: s = "ababcbaca"
 * Ranges:
 * - 'a': [0, 8]  (appears at 0,2,4,6,8)
 * - 'b': [1, 7]  (appears at 1,3,5,7)
 * - 'c': [4, 8]  (appears at 4,7,8)
 * 
 * All ranges overlap! Must be one partition: [0, 8]
 * 
 * ALGORITHM:
 * ==========
 * 1. Find last occurrence of each character
 * 2. Iterate through string, tracking current partition's end
 * 3. For each character, extend partition end to include its last occurrence
 * 4. When current position reaches partition end, finalize partition
 * 5. Start new partition
 */

class PartitionLabels {
    
    /**
     * APPROACH 1: GREEDY WITH LAST OCCURRENCE MAP (OPTIMAL)
     * ======================================================
     * Time Complexity: O(n) where n = string length
     * Space Complexity: O(1) - at most 26 characters
     * 
     * ALGORITHM STEPS:
     * ===============
     * 1. Build map of last occurrence for each character: O(n)
     * 2. Iterate through string: O(n)
     *    - Track current partition start and end
     *    - Extend end to include last occurrence of current char
     *    - When index reaches end, we found a complete partition
     * 3. Return list of partition sizes
     * 
     * WHY THIS WORKS:
     * ==============
     * - We must include all occurrences of a character in one partition
     * - So partition must extend to last occurrence of every char in it
     * - By greedily extending, we find the earliest point to cut
     * - This maximizes number of partitions
     * 
     * DETAILED EXAMPLE:
     * ================
     * s = "ababcbacadefegdehijhklij"
     * 
     * Last occurrences:
     * a→8, b→7, c→8, d→14, e→15, f→11, g→13, h→19, i→22, j→23, k→20, l→21
     * 
     * Iteration:
     * i=0: char='a', last['a']=8, partitionEnd=8, start=0
     * i=1: char='b', last['b']=7, partitionEnd=8 (already >= 7)
     * i=2: char='a', last['a']=8, partitionEnd=8
     * i=3: char='b', last['b']=7, partitionEnd=8
     * i=4: char='c', last['c']=8, partitionEnd=8
     * i=5: char='b', last['b']=7, partitionEnd=8
     * i=6: char='a', last['a']=8, partitionEnd=8
     * i=7: char='c', last['c']=8, partitionEnd=8
     * i=8: char='a', last['a']=8, partitionEnd=8
     *      i == partitionEnd! First partition complete: size = 9
     *      
     * i=9: char='d', last['d']=14, partitionEnd=14, start=9
     * i=10: char='e', last['e']=15, partitionEnd=15 (extend!)
     * i=11: char='f', last['f']=11, partitionEnd=15
     * i=12: char='e', last['e']=15, partitionEnd=15
     * i=13: char='g', last['g']=13, partitionEnd=15
     * i=14: char='d', last['d']=14, partitionEnd=15
     * i=15: char='e', last['e']=15, partitionEnd=15
     *       i == partitionEnd! Second partition complete: size = 7
     *       
     * i=16: char='h', last['h']=19, partitionEnd=19, start=16
     * i=17: char='i', last['i']=22, partitionEnd=22 (extend!)
     * i=18: char='j', last['j']=23, partitionEnd=23 (extend!)
     * i=19: char='h', last['h']=19, partitionEnd=23
     * i=20: char='k', last['k']=20, partitionEnd=23
     * i=21: char='l', last['l']=21, partitionEnd=23
     * i=22: char='i', last['i']=22, partitionEnd=23
     * i=23: char='j', last['j']=23, partitionEnd=23
     *       i == partitionEnd! Third partition complete: size = 8
     * 
     * Result: [9, 7, 8]
     */
    public List<Integer> partitionLabels(String s) {
        List<Integer> result = new ArrayList<>();
        
        // Step 1: Find last occurrence of each character
        int[] lastOccurrence = new int[26];
        for (int i = 0; i < s.length(); i++) {
            lastOccurrence[s.charAt(i) - 'a'] = i;
        }
        
        // Step 2: Iterate and find partitions
        int partitionStart = 0;
        int partitionEnd = 0;
        
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            
            // Extend partition to include last occurrence of current char
            partitionEnd = Math.max(partitionEnd, lastOccurrence[c - 'a']);
            
            // If we've reached the end of current partition
            if (i == partitionEnd) {
                // Add partition size
                result.add(partitionEnd - partitionStart + 1);
                
                // Start new partition
                partitionStart = i + 1;
            }
        }
        
        return result;
    }
    
    /**
     * APPROACH 2: USING INTERVALS (ALTERNATIVE THINKING)
     * ===================================================
     * Time Complexity: O(n)
     * Space Complexity: O(26) = O(1)
     * 
     * This approach explicitly creates intervals and merges them.
     * More intuitive but slightly more code. Good for explaining the concept.
     */
    public List<Integer> partitionLabelsIntervals(String s) {
        List<Integer> result = new ArrayList<>();
        
        // Find first and last occurrence of each character
        int[] first = new int[26];
        int[] last = new int[26];
        Arrays.fill(first, -1);
        Arrays.fill(last, -1);
        
        for (int i = 0; i < s.length(); i++) {
            int idx = s.charAt(i) - 'a';
            if (first[idx] == -1) {
                first[idx] = i;
            }
            last[idx] = i;
        }
        
        // Find merged intervals
        int start = 0;
        int end = 0;
        
        for (int i = 0; i < s.length(); i++) {
            int idx = s.charAt(i) - 'a';
            
            // If this char's range extends beyond current interval, extend
            end = Math.max(end, last[idx]);
            
            // If we've covered the entire current interval
            if (i == end) {
                result.add(end - start + 1);
                start = i + 1;
            }
        }
        
        return result;
    }
    
    /**
     * APPROACH 3: TWO-PASS WITH SET (MORE EXPLICIT)
     * ==============================================
     * Time Complexity: O(n)
     * Space Complexity: O(26) = O(1)
     * 
     * This version is more explicit about tracking which characters
     * we've seen in current partition. Good for understanding but
     * not the most efficient.
     */
    public List<Integer> partitionLabelsTwoPass(String s) {
        List<Integer> result = new ArrayList<>();
        
        // First pass: find last occurrence
        Map<Character, Integer> lastIndex = new HashMap<>();
        for (int i = 0; i < s.length(); i++) {
            lastIndex.put(s.charAt(i), i);
        }
        
        // Second pass: find partitions
        Set<Character> currentPartitionChars = new HashSet<>();
        int start = 0;
        int maxEnd = 0;
        
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            currentPartitionChars.add(c);
            maxEnd = Math.max(maxEnd, lastIndex.get(c));
            
            // Check if all characters in current partition are complete
            if (i == maxEnd) {
                result.add(i - start + 1);
                start = i + 1;
                currentPartitionChars.clear();
            }
        }
        
        return result;
    }
    
    /**
     * VISUALIZATION HELPER
     * Prints the partition process step by step
     */
    public static void visualizePartitioning(String s) {
        System.out.println("String: \"" + s + "\"");
        System.out.println("Index:   " + getIndexString(s.length()));
        System.out.println();
        
        // Find last occurrences
        int[] lastOcc = new int[26];
        for (int i = 0; i < s.length(); i++) {
            lastOcc[s.charAt(i) - 'a'] = i;
        }
        
        // Print last occurrences
        System.out.println("Last occurrences:");
        Set<Character> seen = new HashSet<>();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!seen.contains(c)) {
                System.out.println("  '" + c + "' → index " + lastOcc[c - 'a']);
                seen.add(c);
            }
        }
        System.out.println();
        
        // Trace through
        System.out.println("Partitioning process:");
        int partitionStart = 0;
        int partitionEnd = 0;
        
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int prevEnd = partitionEnd;
            partitionEnd = Math.max(partitionEnd, lastOcc[c - 'a']);
            
            System.out.printf("i=%2d: char='%c', last=%2d, partitionEnd=%2d",
                i, c, lastOcc[c - 'a'], partitionEnd);
            
            if (partitionEnd != prevEnd) {
                System.out.print(" (extended!)");
            }
            
            if (i == partitionEnd) {
                int size = partitionEnd - partitionStart + 1;
                System.out.printf(" → Partition complete! Size=%d [%s]",
                    size, s.substring(partitionStart, partitionEnd + 1));
                partitionStart = i + 1;
            }
            
            System.out.println();
        }
    }
    
    private static String getIndexString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%2d ", i));
        }
        return sb.toString();
    }
    
    // TEST CASES
    public static void main(String[] args) {
        PartitionLabels solution = new PartitionLabels();
        
        System.out.println("=== PARTITION LABELS TEST CASES ===\n");
        
        // Test Case 1: Example from problem
        System.out.println("Test 1: Standard example");
        String s1 = "ababcbacadefegdehijhklij";
        System.out.println("Input: \"" + s1 + "\"");
        List<Integer> result1 = solution.partitionLabels(s1);
        System.out.println("Output: " + result1);
        System.out.println("Expected: [9, 7, 8]");
        System.out.println("Partitions: [\"ababcbaca\", \"defegde\", \"hijhklij\"]");
        System.out.println();
        
        // Test Case 2: Simple example
        System.out.println("Test 2: Simple case");
        String s2 = "eccbbbbdec";
        System.out.println("Input: \"" + s2 + "\"");
        List<Integer> result2 = solution.partitionLabels(s2);
        System.out.println("Output: " + result2);
        System.out.println("Expected: [10]");
        System.out.println("Explanation: All chars interleaved, one partition needed");
        System.out.println();
        
        // Test Case 3: No overlap
        System.out.println("Test 3: No character overlap");
        String s3 = "abcdef";
        System.out.println("Input: \"" + s3 + "\"");
        List<Integer> result3 = solution.partitionLabels(s3);
        System.out.println("Output: " + result3);
        System.out.println("Expected: [1, 1, 1, 1, 1, 1]");
        System.out.println("Explanation: Each char appears once, max partitions");
        System.out.println();
        
        // Test Case 4: All same character
        System.out.println("Test 4: All same character");
        String s4 = "aaaa";
        System.out.println("Input: \"" + s4 + "\"");
        List<Integer> result4 = solution.partitionLabels(s4);
        System.out.println("Output: " + result4);
        System.out.println("Expected: [4]");
        System.out.println();
        
        // Test Case 5: Two groups
        System.out.println("Test 5: Two clear groups");
        String s5 = "ababccdd";
        System.out.println("Input: \"" + s5 + "\"");
        List<Integer> result5 = solution.partitionLabels(s5);
        System.out.println("Output: " + result5);
        System.out.println("Expected: [4, 2, 2] or [4, 4]");
        System.out.println("Actual partitions: ");
        visualizePartitioning(s5);
        System.out.println();
        
        // Test Case 6: Single character
        System.out.println("Test 6: Single character");
        String s6 = "a";
        System.out.println("Input: \"" + s6 + "\"");
        List<Integer> result6 = solution.partitionLabels(s6);
        System.out.println("Output: " + result6);
        System.out.println("Expected: [1]");
        System.out.println();
        
        // Detailed visualization
        System.out.println("=== DETAILED VISUALIZATION ===\n");
        System.out.println("Example 1:");
        visualizePartitioning("ababcbaca");
        System.out.println();
        
        System.out.println("Example 2:");
        visualizePartitioning("abcabc");
        System.out.println();
        
        System.out.println("=== COMPLEXITY ANALYSIS ===");
        System.out.println("Time Complexity: O(n)");
        System.out.println("  - First pass to find last occurrences: O(n)");
        System.out.println("  - Second pass to find partitions: O(n)");
        System.out.println("  - Total: O(n)");
        System.out.println();
        System.out.println("Space Complexity: O(1)");
        System.out.println("  - Last occurrence array: O(26) = O(1)");
        System.out.println("  - Result list: O(n) in worst case, but required for output");
        System.out.println();
        
        System.out.println("=== KEY INSIGHTS ===");
        System.out.println("1. Greedy approach: cut as early as possible");
        System.out.println("2. Track last occurrence of each character");
        System.out.println("3. Extend partition boundary as we discover characters");
        System.out.println("4. Similar to interval merging");
        System.out.println("5. Single pass solution after preprocessing");
    }
}

/**
 * INTERVIEW STRATEGY GUIDE
 * ========================
 * 
 * 1. CLARIFY THE PROBLEM (2 minutes)
 *    Q: "Can characters repeat in different partitions?" A: No
 *    Q: "Should we maximize number of partitions?" A: Yes
 *    Q: "What about empty string?" A: Usually n ≥ 1
 *    Q: "Only lowercase letters?" A: Usually yes
 *    
 * 2. PROVIDE EXAMPLES (3 minutes)
 *    "Let me trace through 'ababcbaca'..."
 *    - 'a' appears at 0,2,4,6,8 → must go to position 8
 *    - 'b' appears at 1,3,5,7 → all within [0,8]
 *    - 'c' appears at 4,7,8 → all within [0,8]
 *    - First partition: [0,8] = "ababcbaca"
 *    
 * 3. EXPLAIN APPROACH (5 minutes)
 *    "This is a greedy problem. Key insight:"
 *    "For each character, we need to know its last occurrence"
 *    "A partition can only end when we've included all occurrences"
 *    "As we scan, we extend the partition boundary"
 *    "When current index reaches boundary, we can cut"
 *    
 * 4. DISCUSS COMPLEXITY (2 minutes)
 *    "Two passes: O(n) to find last occurrences, O(n) to partition"
 *    "Total: O(n) time"
 *    "Space: O(1) for last occurrence array (only 26 chars)"
 *    
 * 5. CODE THE SOLUTION (10 minutes)
 *    Start with lastOccurrence array
 *    Iterate through string
 *    Track partition start and end
 *    Add size when partition completes
 *    
 * 6. TRACE THROUGH EXAMPLE (5 minutes)
 *    Use a simple example like "ababcc"
 *    Show how partition boundary extends
 *    Show where cuts happen
 *    
 * 7. TEST EDGE CASES (3 minutes)
 *    - Single character
 *    - All different characters (max partitions)
 *    - All same character (one partition)
 *    - Two groups with no overlap
 * 
 * 
 * COMMON MISTAKES TO AVOID
 * ========================
 * 
 * 1. NOT TRACKING LAST OCCURRENCE
 *    ❌ Only looking at current character
 *    ✓  Must know where character appears last
 *    
 * 2. CUTTING TOO EARLY
 *    ❌ Cutting when first occurrence of char ends
 *    ✓  Must extend to LAST occurrence
 *    
 * 3. WRONG PARTITION SIZE
 *    ❌ result.add(i) or result.add(partitionEnd)
 *    ✓  result.add(partitionEnd - partitionStart + 1)
 *    
 * 4. FORGETTING TO UPDATE START
 *    ❌ Not updating partitionStart after cutting
 *    ✓  partitionStart = i + 1 after each partition
 *    
 * 5. OFF-BY-ONE ERRORS
 *    ❌ Comparing i < partitionEnd (wrong!)
 *    ✓  Comparing i == partitionEnd (correct!)
 * 
 * 
 * ALTERNATIVE WAYS TO THINK ABOUT IT
 * ==================================
 * 
 * 1. INTERVAL MERGING:
 *    Each character defines interval [first, last]
 *    Merge overlapping intervals
 *    Count merged interval sizes
 *    
 * 2. GRAPH PROBLEM:
 *    Characters that must be together form a component
 *    Find connected components
 *    (Overkill for this problem!)
 *    
 * 3. GREEDY CHOICE:
 *    At each position, can we cut here?
 *    Cut if: all characters seen so far have no future occurrences
 *    (This is what our algorithm implements!)
 * 
 * 
 * FOLLOW-UP QUESTIONS TO PREPARE FOR
 * ==================================
 * 
 * Q: "What if we want minimum number of partitions instead?"
 * A: The algorithm would be the same! Greedy cutting maximizes partitions,
 *    so we can't do better. For minimum, we'd need different constraints.
 * 
 * Q: "What if characters can appear in at most k partitions?"
 * A: More complex - would need DP or different greedy strategy
 * 
 * Q: "What if we want to return the actual partition strings?"
 * A: Easy modification - store substrings instead of just sizes
 * 
 * Q: "Can you do this in one pass without preprocessing?"
 * A: No - we need to know last occurrences before we can decide where to cut
 *    Two passes (both O(n)) is optimal
 * 
 * Q: "What if string is very large (streaming)?"
 * A: Would need to see entire string first to find last occurrences
 *    Can't partition in streaming fashion
 * 
 * Q: "How would you handle Unicode characters?"
 * A: Use HashMap instead of array for last occurrences
 *    Time complexity stays O(n), space becomes O(unique chars)
 * 
 * 
 * PATTERN RECOGNITION
 * ===================
 * 
 * This problem is similar to:
 * - Merge Intervals (LC 56)
 * - Non-overlapping Intervals (LC 435)
 * - Minimum Number of Arrows (LC 452)
 * 
 * Common pattern: Greedy interval processing
 * 
 * Key difference: Here we're building intervals as we go,
 * not given them upfront
 * 
 * 
 * KEY TAKEAWAYS
 * =============
 * 
 * 1. This is a GREEDY problem - make locally optimal choices
 * 2. Preprocessing (last occurrences) enables greedy algorithm
 * 3. Think of it as interval merging
 * 4. Extend partition boundary as you discover character ranges
 * 5. Cut when current position reaches partition boundary
 * 6. O(n) time, O(1) space - optimal solution
 * 7. Drawing out examples really helps!
 * 
 * Good luck! 🎯
 */
