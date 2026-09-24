import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * PROBLEM: Count the number of distinct substrings in a string.
 * 
 * This is a TRIE (PREFIX TREE) problem testing:
 * 1. Understanding of Trie data structure
 * 2. Generating all substrings efficiently
 * 3. Deduplication using Trie structure
 * 4. Space-time trade-offs
 * 5. Alternative approaches (Set, Suffix Array, etc.)
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. SUBSTRINGS vs SUBSEQUENCES:
 *    - Substring: contiguous characters (abc → a, ab, abc, b, bc, c)
 *    - Subsequence: not necessarily contiguous (abc → a, b, c, ab, ac, bc, abc)
 *    - We need SUBSTRINGS
 * 
 * 2. TRIE FOR DEDUPLICATION:
 *    - Insert all substrings into Trie
 *    - Trie naturally handles duplicates
 *    - Count total nodes = count distinct substrings
 * 
 * 3. GENERATING SUBSTRINGS:
 *    - For string of length N: N(N+1)/2 substrings
 *    - All substrings starting at index i: s[i:j] for j > i
 * 
 * 4. WHY TRIE?
 *    - Efficient prefix sharing (reduces space)
 *    - O(1) insertion check for duplicates
 *    - Can count nodes during insertion
 * 
 * VISUALIZATION:
 * --------------
 * 
 * String: "abc"
 * Substrings: "a", "ab", "abc", "b", "bc", "c" (6 total)
 * 
 * Trie structure:
 *         root
 *        /    \
 *       a      b
 *      / \      \
 *     b   (end)  c
 *    /            \
 *   c            (end)
 *  /
 * (end)
 * 
 * Nodes: root, a, a->b, a->b->c, b, b->c, c (7 nodes)
 * But root doesn't count, so 6 distinct substrings ✓
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Clarify requirements
 *    "We're counting distinct substrings, not subsequences, right?"
 *    "Should we count empty string? (Usually no)"
 * 
 * 2. Explain naive approach first
 *    "I could use a HashSet to store all substrings, but that's O(N²) space."
 * 
 * 3. Introduce Trie optimization
 *    "Using a Trie, I can share common prefixes and count nodes efficiently."
 * 
 * 4. Walk through example
 *    [Draw Trie structure on board]
 * 
 * 5. Discuss complexity
 *    "Time is O(N²) for generating substrings, space is better with Trie."
 */

/**
 * TRIE NODE DEFINITION
 * =====================
 * 
 * Each node represents a character in a substring
 * Children map stores next characters
 */
class TrieNode {
    Map<Character, TrieNode> children;
    
    public TrieNode() {
        this.children = new HashMap<>();
    }
}

class Solution {
    
    /**
     * APPROACH 1: TRIE WITH NODE COUNTING - OPTIMAL
     * ==============================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Create Trie
     * 2. For each starting position i:
     *    - Insert all substrings starting at i into Trie
     *    - Count new nodes created during insertion
     * 3. Return total count of nodes
     * 
     * KEY INSIGHT: Each new Trie node = one distinct substring!
     * 
     * DETAILED WALKTHROUGH:
     * ---------------------
     * String: "aab"
     * 
     * Starting at index 0:
     *   Insert "a": Create node for 'a' → count = 1
     *   Insert "aa": 'a' exists, create node for second 'a' → count = 2
     *   Insert "aab": Use existing path, create 'b' → count = 3
     * 
     * Starting at index 1:
     *   Insert "a": Node 'a' already exists → count = 3
     *   Insert "ab": 'a' exists, create 'b' at different path → count = 4
     * 
     * Starting at index 2:
     *   Insert "b": Create node for 'b' → count = 5
     * 
     * Distinct substrings: "a", "aa", "aab", "ab", "b" (5 total) ✓
     * 
     * TIME COMPLEXITY: O(N²)
     * - N starting positions
     * - For each position, insert up to N characters
     * - Each character insertion: O(1) average
     * - Total: O(N²)
     * 
     * SPACE COMPLEXITY: O(N²)
     * - Worst case: all substrings are distinct
     * - For string "abcd...": N(N+1)/2 substrings
     * - Trie stores each uniquely
     * - But better than HashSet due to prefix sharing!
     * 
     * ADVANTAGES OVER HASHSET:
     * - Prefix sharing reduces actual space
     * - Can handle very long strings better
     * - Inherent deduplication structure
     */
    public int countDistinctSubstrings(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }
        
        TrieNode root = new TrieNode();
        int count = 0;
        int n = s.length();
        
        // Generate all substrings and insert into Trie
        for (int i = 0; i < n; i++) {
            TrieNode current = root;
            
            // Insert all substrings starting at index i
            for (int j = i; j < n; j++) {
                char c = s.charAt(j);
                
                // If character doesn't exist, create new node
                // This represents a new distinct substring
                if (!current.children.containsKey(c)) {
                    current.children.put(c, new TrieNode());
                    count++; // New node = new distinct substring
                }
                
                current = current.children.get(c);
            }
        }
        
        return count;
    }
    
    /**
     * APPROACH 2: TRIE WITH EXPLICIT SUBSTRING INSERTION
     * ===================================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Generate all substrings
     * 2. Insert each into Trie
     * 3. Count total nodes in Trie
     * 
     * More explicit about substring generation
     * Same complexity but clearer for explanation
     * 
     * TIME: O(N²), SPACE: O(N²)
     */
    public int countDistinctSubstringsExplicit(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }
        
        TrieNode root = new TrieNode();
        
        // Generate and insert all substrings
        for (int i = 0; i < s.length(); i++) {
            for (int j = i + 1; j <= s.length(); j++) {
                String substring = s.substring(i, j);
                insertSubstring(root, substring);
            }
        }
        
        // Count nodes in Trie
        return countNodes(root);
    }
    
    private void insertSubstring(TrieNode root, String str) {
        TrieNode current = root;
        for (char c : str.toCharArray()) {
            current.children.putIfAbsent(c, new TrieNode());
            current = current.children.get(c);
        }
    }
    
    private int countNodes(TrieNode node) {
        int count = 0;
        for (TrieNode child : node.children.values()) {
            count++; // Count this node
            count += countNodes(child); // Count descendants
        }
        return count;
    }
    
    /**
     * APPROACH 3: HASHSET - SIMPLER BUT MORE SPACE
     * =============================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Generate all substrings
     * 2. Add to HashSet (automatic deduplication)
     * 3. Return set size
     * 
     * PROS:
     * - Simpler to implement
     * - Easy to understand
     * 
     * CONS:
     * - More space (no prefix sharing)
     * - Each substring stored fully
     * 
     * TIME: O(N²) for generating + O(N²) for storing = O(N²)
     * SPACE: O(N²) - worse than Trie
     * 
     * For string "abc":
     * HashSet stores: "a", "ab", "abc", "b", "bc", "c"
     * Total characters stored: 1 + 2 + 3 + 1 + 2 + 1 = 10
     * 
     * Trie stores: a->b->c and b->c (shared 'c')
     * Total nodes: 6 (more efficient!)
     */
    public int countDistinctSubstringsHashSet(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }
        
        Set<String> distinctSubstrings = new HashSet<>();
        
        // Generate all substrings
        for (int i = 0; i < s.length(); i++) {
            for (int j = i + 1; j <= s.length(); j++) {
                distinctSubstrings.add(s.substring(i, j));
            }
        }
        
        return distinctSubstrings.size();
    }
    
    /**
     * APPROACH 4: SUFFIX ARRAY (ADVANCED)
     * ====================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Build suffix array
     * 2. Compute LCP (Longest Common Prefix) array
     * 3. Count: N(N+1)/2 - sum(LCP)
     * 
     * This is the most space-efficient approach but complex!
     * 
     * TIME: O(N²) or O(N log N) with advanced suffix array construction
     * SPACE: O(N) - much better!
     * 
     * Used in competitive programming for optimal space.
     * Not typically expected in interviews unless specified.
     */
}

/**
 * DETAILED TRIE VISUALIZATION
 * ============================
 * 
 * Example: s = "aba"
 * Substrings: "a", "ab", "aba", "b", "ba", "a" (duplicate)
 * Distinct: "a", "ab", "aba", "b", "ba" (5 total)
 * 
 * Building Trie step by step:
 * 
 * Insert "a":
 *   root -> a [count = 1]
 * 
 * Insert "ab":
 *   root -> a -> b [count = 2]
 * 
 * Insert "aba":
 *   root -> a -> b -> a [count = 3]
 * 
 * Insert "b":
 *   root -> a -> b -> a
 *        \-> b [count = 4]
 * 
 * Insert "ba":
 *   root -> a -> b -> a
 *        \-> b -> a [count = 5]
 * 
 * Insert "a" (duplicate):
 *   root -> a (already exists, no new node)
 *   [count = 5]
 * 
 * Final count: 5 distinct substrings ✓
 */

/**
 * COMPLEXITY COMPARISON
 * =====================
 * 
 * Approach       | Time  | Space  | Notes
 * ---------------|-------|--------|---------------------------
 * Trie Optimal   | O(N²) | O(N²)* | Best with prefix sharing
 * Trie Explicit  | O(N²) | O(N²)* | Clearer implementation
 * HashSet        | O(N²) | O(N²)  | Simple but more space
 * Suffix Array   | O(N²) | O(N)   | Advanced, space optimal
 * 
 * * Trie space is O(N²) worst case but typically better due to prefix sharing
 * 
 * RECOMMENDATION:
 * - Interview: Trie Optimal (Approach 1) - best balance
 * - If time constrained: HashSet (Approach 3) - simplest
 * - Competitive programming: Suffix Array (Approach 4)
 */

/**
 * SUBSTRING vs SUBSEQUENCE
 * =========================
 * 
 * SUBSTRING: Contiguous characters
 * --------------------------------
 * String: "abc"
 * Substrings: "a", "ab", "abc", "b", "bc", "c"
 * Count: 6
 * 
 * Generated by: s.substring(i, j) for all i < j
 * 
 * 
 * SUBSEQUENCE: Not necessarily contiguous
 * ----------------------------------------
 * String: "abc"
 * Subsequences: "", "a", "b", "c", "ab", "ac", "bc", "abc"
 * Count: 8 (2^N)
 * 
 * Generated by: include or exclude each character
 * 
 * 
 * THIS PROBLEM: SUBSTRINGS
 */

/**
 * EDGE CASES & TESTING
 * =====================
 * 
 * EDGE CASES:
 * 1. Empty string → 0
 * 2. Single character → 1 ("a")
 * 3. All same characters → N ("aaa" → "a", "aa", "aaa" = 3)
 * 4. All different characters → N(N+1)/2 ("abc" → 6)
 * 5. Repeated patterns ("abab" → "a", "ab", "aba", "abab", "b", "ba", "bab")
 */

// Comprehensive test suite
class Main {
    public static void main(String[] args) {
        Solution sol = new Solution();
        
        System.out.println("=== Test Case 1: Simple String ===");
        String s1 = "abc";
        System.out.println("String: \"" + s1 + "\"");
        System.out.println("Substrings: a, ab, abc, b, bc, c");
        System.out.println("Trie:     " + sol.countDistinctSubstrings(s1));
        System.out.println("HashSet:  " + sol.countDistinctSubstringsHashSet(s1));
        System.out.println("Expected: 6");
        
        System.out.println("\n=== Test Case 2: Repeated Characters ===");
        String s2 = "aaa";
        System.out.println("String: \"" + s2 + "\"");
        System.out.println("Substrings: a, aa, aaa");
        System.out.println("Trie:     " + sol.countDistinctSubstrings(s2));
        System.out.println("HashSet:  " + sol.countDistinctSubstringsHashSet(s2));
        System.out.println("Expected: 3");
        
        System.out.println("\n=== Test Case 3: Duplicates ===");
        String s3 = "aba";
        System.out.println("String: \"" + s3 + "\"");
        System.out.println("All substrings: a, ab, aba, b, ba, a (duplicate)");
        System.out.println("Distinct: a, ab, aba, b, ba");
        System.out.println("Trie:     " + sol.countDistinctSubstrings(s3));
        System.out.println("HashSet:  " + sol.countDistinctSubstringsHashSet(s3));
        System.out.println("Expected: 5");
        
        System.out.println("\n=== Test Case 4: Single Character ===");
        String s4 = "a";
        System.out.println("String: \"" + s4 + "\"");
        System.out.println("Trie:     " + sol.countDistinctSubstrings(s4));
        System.out.println("HashSet:  " + sol.countDistinctSubstringsHashSet(s4));
        System.out.println("Expected: 1");
        
        System.out.println("\n=== Test Case 5: Pattern ===");
        String s5 = "abab";
        System.out.println("String: \"" + s5 + "\"");
        System.out.println("Distinct: a, ab, aba, abab, b, ba, bab");
        System.out.println("Trie:     " + sol.countDistinctSubstrings(s5));
        System.out.println("HashSet:  " + sol.countDistinctSubstringsHashSet(s5));
        System.out.println("Expected: 7");
        
        System.out.println("\n=== Test Case 6: All Different ===");
        String s6 = "abcd";
        System.out.println("String: \"" + s6 + "\"");
        System.out.println("Formula: N(N+1)/2 = 4*5/2 = 10");
        System.out.println("Trie:     " + sol.countDistinctSubstrings(s6));
        System.out.println("HashSet:  " + sol.countDistinctSubstringsHashSet(s6));
        System.out.println("Expected: 10");
        
        System.out.println("\n=== Visual Demonstration ===");
        System.out.println("String: \"aba\"");
        System.out.println();
        System.out.println("Trie structure:");
        System.out.println("     root");
        System.out.println("      / \\");
        System.out.println("     a   b");
        System.out.println("    / \\   \\");
        System.out.println("   b   *   a");
        System.out.println("  /         *");
        System.out.println(" a");
        System.out.println(" *");
        System.out.println();
        System.out.println("Nodes (each = distinct substring):");
        System.out.println("1. a → 'a'");
        System.out.println("2. a->b → 'ab'");
        System.out.println("3. a->b->a → 'aba'");
        System.out.println("4. b → 'b'");
        System.out.println("5. b->a → 'ba'");
        System.out.println();
        System.out.println("Total: 5 distinct substrings");
        
        System.out.println("\n=== Space Comparison ===");
        String s7 = "abcdef";
        int n = s7.length();
        int totalSubstrings = n * (n + 1) / 2;
        System.out.println("String: \"" + s7 + "\" (length " + n + ")");
        System.out.println("Total substrings: " + totalSubstrings);
        System.out.println();
        System.out.println("HashSet approach:");
        System.out.println("  Stores each substring fully");
        System.out.println("  No prefix sharing");
        System.out.println();
        System.out.println("Trie approach:");
        System.out.println("  Shares common prefixes");
        System.out.println("  More space efficient");
        System.out.println();
        System.out.println("Result: " + sol.countDistinctSubstrings(s7));
    }
}

/**
 * INTERVIEW TALKING POINTS
 * =========================
 * 
 * When presenting this solution in an interview:
 * 
 * 1. "I need to count distinct substrings. A substring is a contiguous
 *    sequence of characters, different from subsequences."
 * 
 * 2. "The naive approach would be to generate all N(N+1)/2 substrings
 *    and use a HashSet for deduplication. This works but uses O(N²) space
 *    since each substring is stored fully."
 *    [Write HashSet approach]
 * 
 * 3. "A better approach uses a Trie. As I insert substrings, the Trie
 *    naturally handles duplicates and shares common prefixes, reducing
 *    actual space used."
 *    [Draw Trie on board]
 * 
 * 4. "The key insight: each new Trie node represents a new distinct
 *    substring. So I count nodes as I insert all substrings."
 *    [Write Trie solution]
 * 
 * 5. "Time is O(N²) to generate all substrings - can't avoid this.
 *    Space is O(N²) worst case but better in practice due to prefix
 *    sharing in the Trie."
 * 
 * 6. "Edge cases: empty string (0), single character (1), all same
 *    characters (N), and handling duplicates correctly."
 * 
 * KEY POINTS TO EMPHASIZE:
 * - Substring vs subsequence distinction
 * - Trie for efficient prefix sharing
 * - Each Trie node = one distinct substring
 * - O(N²) is unavoidable for substring generation
 * 
 * FOLLOW-UP QUESTIONS TO EXPECT:
 * - Can you optimize further? [Suffix array for O(N) space]
 * - What about longest substring without repeating chars? [Different problem]
 * - How would you find all palindromic substrings? [Different check]
 * - What if we want distinct subsequences? [DP problem, 2^N]
 * - Can you do it without Trie? [Yes, HashSet simpler but more space]
 */
