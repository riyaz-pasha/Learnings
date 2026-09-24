/**
 * MAXIMUM XOR OF TWO NUMBERS IN AN ARRAY
 * 
 * PROBLEM UNDERSTANDING:
 * - Given an array of integers, find the maximum XOR result between any two numbers
 * - XOR (exclusive OR): returns 1 when bits are different, 0 when same
 * - Example: 3 XOR 10 = 0011 XOR 1010 = 1001 = 9
 * 
 * KEY INSIGHTS:
 * 1. XOR Properties:
 *    - a XOR a = 0 (same numbers cancel out)
 *    - a XOR 0 = a (XOR with 0 gives the number itself)
 *    - XOR is commutative: a XOR b = b XOR a
 *    - To maximize XOR, we want bits to be as different as possible
 * 
 * 2. Greedy Bit Strategy:
 *    - Build the result from most significant bit (MSB) to least significant bit (LSB)
 *    - Try to set each bit to 1 starting from the highest position
 *    - Higher bits contribute more to the final value (bit 31 = 2^31)
 * 
 * INTERVIEW APPROACH:
 * 1. Start with brute force to show understanding
 * 2. Discuss time complexity issues
 * 3. Introduce bit manipulation optimization
 * 4. Explain Trie-based solution for optimal performance
 * 5. Code the solution you're most comfortable with
 */

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class MaximumXOR {
    
    // ==================== APPROACH 1: BRUTE FORCE ====================
    /**
     * TIME COMPLEXITY: O(n^2) - nested loops comparing all pairs
     * SPACE COMPLEXITY: O(1) - only storing max result
     * 
     * WHEN TO USE IN INTERVIEW:
     * - Always start here to show problem understanding
     * - Good baseline to compare optimizations against
     * - Acceptable for small arrays (n < 1000)
     * 
     * REASONING:
     * - Simply try all possible pairs and track maximum
     * - Straightforward but inefficient for large inputs
     * - No special data structures needed
     */
    public int findMaximumXORBruteForce(int[] nums) {
        int maxXOR = 0;
        int n = nums.length;
        
        // Try every pair (i, j) where i <= j
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                // Calculate XOR and update maximum
                int currentXOR = nums[i] ^ nums[j];
                maxXOR = Math.max(maxXOR, currentXOR);
            }
        }
        
        return maxXOR;
    }
    
    // ==================== APPROACH 2: BIT MANIPULATION WITH HASHSET ====================
    /**
     * TIME COMPLEXITY: O(32 * n) = O(n) - 32 bits, each bit processes n numbers
     * SPACE COMPLEXITY: O(n) - HashSet stores prefixes
     * 
     * WHEN TO USE IN INTERVIEW:
     * - Good middle-ground solution
     * - Shows understanding of bit manipulation
     * - Easier to explain than Trie
     * 
     * KEY INSIGHT - GREEDY BIT BUILDING:
     * We build the answer bit by bit from left (MSB) to right (LSB).
     * For each bit position, we ask: "Can we set this bit to 1?"
     * 
     * How do we check if a bit can be 1?
     * - If we want result bit i to be 1, we need two numbers whose bit i differs
     * - Using XOR property: if (a ^ b) = c, then a = b ^ c
     * - So if we have prefix 'p' and want result 'r', we need prefix (p ^ r) to exist
     * 
     * EXAMPLE: [3, 10, 5, 25, 2, 8]
     * Binary representations:
     * 3  = 00011
     * 10 = 01010
     * 5  = 00101
     * 25 = 11001
     * 2  = 00010
     * 8  = 01000
     * 
     * Bit 4 (leftmost): Try to set it to 1
     * - Prefixes with 4 bits: {0001, 0101, 1100, 0001, 0100}
     * - Want max = 1xxxx (bit 4 = 1)
     * - Check if any prefix p has (p ^ 1xxxx) in set
     * - 1100 exists, and (1100 ^ 11001) could give us bit 4 = 1? No, wait...
     * - Actually check: can we make 10000 (16)?
     * - Need two prefixes p1, p2 where p1 ^ p2 = 10000
     * - 0001 ^ 1001 would work if we had 1001, etc.
     */
    public int findMaximumXORBitManipulation(int[] nums) {
        int maxXOR = 0;
        int mask = 0;
        
        // Process bits from left to right (MSB to LSB)
        // Integer in Java is 32 bits, but we typically process 31 bits (excluding sign)
        for (int i = 31; i >= 0; i--) {
            // Build mask to get prefix of length (32 - i)
            // First iteration: mask = 10000000...0 (only bit 31)
            // Second iteration: mask = 11000000...0 (bits 31 and 30)
            // And so on...
            mask = mask | (1 << i);
            
            // Store all prefixes of current length
            Set<Integer> prefixes = new HashSet<>();
            for (int num : nums) {
                // Extract prefix using mask
                // Example: if num = 25 (11001) and mask = 11000 (masking 3 bits)
                // prefix = 11000 (keeps only the masked bits)
                prefixes.add(num & mask);
            }
            
            // Try to set current bit to 1 in the result
            // Greedy approach: always try to maximize by setting bit to 1
            int candidate = maxXOR | (1 << i);
            
            // Check if this candidate is achievable
            // We need two numbers whose XOR gives us this candidate
            // Using property: if a ^ b = candidate, then a = b ^ candidate
            for (int prefix : prefixes) {
                // If prefix ^ candidate exists in set, we can achieve candidate
                // Because prefix ^ (prefix ^ candidate) = candidate
                if (prefixes.contains(prefix ^ candidate)) {
                    maxXOR = candidate;
                    break; // Found it, no need to check other prefixes
                }
            }
            // If we didn't find it, maxXOR stays the same (bit i remains 0)
        }
        
        return maxXOR;
    }
    
    // ==================== APPROACH 3: TRIE (OPTIMAL) ====================
    /**
     * TIME COMPLEXITY: O(32 * n) = O(n) - insert all numbers, then query each
     * SPACE COMPLEXITY: O(32 * n) = O(n) - Trie nodes
     * 
     * WHEN TO USE IN INTERVIEW:
     * - When you need optimal solution
     * - Shows advanced data structure knowledge
     * - Better constants than HashSet approach
     * 
     * WHY TRIE?
     * A Trie (prefix tree) naturally organizes numbers by their bit patterns.
     * - Each level represents a bit position (31 down to 0)
     * - Each node has two children: 0-bit and 1-bit
     * - Path from root to leaf represents complete binary number
     * 
     * MAXIMIZING XOR WITH TRIE:
     * To maximize XOR with a number, at each bit position:
     * - If current bit is 0, we prefer path with 1 (to make XOR = 1)
     * - If current bit is 1, we prefer path with 0 (to make XOR = 1)
     * - If preferred path doesn't exist, take the other path
     * 
     * EXAMPLE: Finding max XOR for 25 (11001)
     * At bit 4 (value 1): want to go 0-path (different bit) → XOR bit = 1
     * At bit 3 (value 1): want to go 0-path (different bit) → XOR bit = 1  
     * At bit 2 (value 0): want to go 1-path (different bit) → XOR bit = 1
     * At bit 1 (value 0): want to go 1-path (different bit) → XOR bit = 1
     * At bit 0 (value 1): want to go 0-path (different bit) → XOR bit = 1
     * Result: 11111 = 31
     */
    
    // Trie Node: represents a bit position in binary representation
    class TrieNode {
        TrieNode[] children; // children[0] for bit 0, children[1] for bit 1
        
        public TrieNode() {
            children = new TrieNode[2];
        }
    }
    
    class Trie {
        TrieNode root;
        
        public Trie() {
            root = new TrieNode();
        }
        
        /**
         * Insert a number into the Trie
         * Store its binary representation from MSB to LSB
         */
        public void insert(int num) {
            TrieNode node = root;
            
            // Process each bit from position 31 down to 0
            for (int i = 31; i >= 0; i--) {
                // Extract the i-th bit: shift right by i, then AND with 1
                // Example: num = 5 (101), i = 2
                // 101 >> 2 = 1, 1 & 1 = 1
                int bit = (num >> i) & 1;
                
                // Create child node if it doesn't exist
                if (node.children[bit] == null) {
                    node.children[bit] = new TrieNode();
                }
                
                // Move to child node
                node = node.children[bit];
            }
        }
        
        /**
         * Find the maximum XOR for a given number
         * At each bit position, try to take the opposite path
         */
        public int findMaxXOR(int num) {
            TrieNode node = root;
            int maxXOR = 0;
            
            for (int i = 31; i >= 0; i--) {
                // Get current bit of num
                int bit = (num >> i) & 1;
                
                // We want the opposite bit to maximize XOR
                // If current bit is 0, we want 1; if 1, we want 0
                int oppositeBit = 1 - bit;
                
                // Try to take the opposite path (greedy choice)
                if (node.children[oppositeBit] != null) {
                    // We can take opposite path! XOR will be 1 at this position
                    maxXOR = maxXOR | (1 << i); // Set bit i to 1
                    node = node.children[oppositeBit];
                } else {
                    // Can't take opposite path, take the same path
                    // XOR will be 0 at this position (bit already 0 in maxXOR)
                    node = node.children[bit];
                }
            }
            
            return maxXOR;
        }
    }
    
    public int findMaximumXORTrie(int[] nums) {
        Trie trie = new Trie();
        
        // Insert all numbers into the Trie
        for (int num : nums) {
            trie.insert(num);
        }
        
        // Find maximum XOR for each number
        int maxXOR = 0;
        for (int num : nums) {
            maxXOR = Math.max(maxXOR, trie.findMaxXOR(num));
        }
        
        return maxXOR;
    }
    
    // ==================== TEST CASES ====================
    public static void main(String[] args) {
        MaximumXOR solution = new MaximumXOR();
        
        // Test Case 1: Basic example
        int[] nums1 = {3, 10, 5, 25, 2, 8};
        System.out.println("Test 1: " + Arrays.toString(nums1));
        System.out.println("Brute Force: " + solution.findMaximumXORBruteForce(nums1));
        System.out.println("Bit Manipulation: " + solution.findMaximumXORBitManipulation(nums1));
        System.out.println("Trie: " + solution.findMaximumXORTrie(nums1));
        System.out.println("Expected: 28 (5 XOR 25 = 00101 XOR 11001 = 11100 = 28)\n");
        
        // Test Case 2: Two elements
        int[] nums2 = {14, 70};
        System.out.println("Test 2: " + Arrays.toString(nums2));
        System.out.println("Brute Force: " + solution.findMaximumXORBruteForce(nums2));
        System.out.println("Bit Manipulation: " + solution.findMaximumXORBitManipulation(nums2));
        System.out.println("Trie: " + solution.findMaximumXORTrie(nums2));
        System.out.println("Expected: 72 (14 XOR 70 = 001110 XOR 1000110 = 1001000 = 72)\n");
        
        // Test Case 3: All same bits pattern won't give high XOR
        int[] nums3 = {5, 25, 10, 3};
        System.out.println("Test 3: " + Arrays.toString(nums3));
        System.out.println("Brute Force: " + solution.findMaximumXORBruteForce(nums3));
        System.out.println("Bit Manipulation: " + solution.findMaximumXORBitManipulation(nums3));
        System.out.println("Trie: " + solution.findMaximumXORTrie(nums3));
        System.out.println();
        
        // Test Case 4: Single element
        int[] nums4 = {5};
        System.out.println("Test 4: " + Arrays.toString(nums4));
        System.out.println("Brute Force: " + solution.findMaximumXORBruteForce(nums4));
        System.out.println("Bit Manipulation: " + solution.findMaximumXORBitManipulation(nums4));
        System.out.println("Trie: " + solution.findMaximumXORTrie(nums4));
        System.out.println("Expected: 0 (5 XOR 5 = 0)\n");
        
        // Test Case 5: Powers of 2
        int[] nums5 = {1, 2, 4, 8, 16};
        System.out.println("Test 5: " + Arrays.toString(nums5));
        System.out.println("Brute Force: " + solution.findMaximumXORBruteForce(nums5));
        System.out.println("Bit Manipulation: " + solution.findMaximumXORBitManipulation(nums5));
        System.out.println("Trie: " + solution.findMaximumXORTrie(nums5));
        System.out.println();
    }
}

/**
 * ==================== INTERVIEW STRATEGY ====================
 * 
 * STEP 1: CLARIFY THE PROBLEM (2 minutes)
 * - Ask about constraints: array size? value range?
 * - Confirm we can use same element twice (i == j allowed)
 * - Ask about edge cases: empty array? single element?
 * 
 * STEP 2: DISCUSS APPROACH (3-5 minutes)
 * - Start with brute force: "We could try all pairs - O(n²)"
 * - Explain XOR properties: "XOR is maximized when bits differ"
 * - Introduce optimization: "We can build result bit by bit"
 * - Choose between HashSet or Trie based on interviewer interest
 * 
 * STEP 3: CODE (10-15 minutes)
 * - Start with clean structure (helper classes if needed)
 * - Write main logic with comments
 * - Handle edge cases
 * 
 * STEP 4: TEST (5 minutes)
 * - Walk through example: [3, 10, 5, 25]
 * - Test edge cases: single element, two elements
 * - Discuss time/space complexity
 * 
 * STEP 5: OPTIMIZE/DISCUSS (remaining time)
 * - Compare approaches if time permits
 * - Discuss trade-offs
 * - Mention real-world applications
 * 
 * ==================== COMMON MISTAKES TO AVOID ====================
 * 
 * 1. BIT MANIPULATION ERRORS:
 *    - Wrong: checking (1 << 32) - Java integers are 32 bits, max shift is 31
 *    - Wrong: forgetting parentheses in bit operations (& has lower precedence than ==)
 *    - Right: always use parentheses: if ((num & (1 << i)) != 0)
 * 
 * 2. TRIE IMPLEMENTATION:
 *    - Wrong: processing bits LSB to MSB (breaks the greedy approach)
 *    - Wrong: not initializing children array properly
 *    - Right: always process MSB to LSB for maximization
 * 
 * 3. HASHSET APPROACH:
 *    - Wrong: not updating mask correctly in each iteration
 *    - Wrong: checking wrong combinations in the set
 *    - Right: understand the XOR property: a = b ^ c means we need both b and c
 * 
 * 4. EDGE CASES:
 *    - Don't forget: single element returns 0 (num XOR num = 0)
 *    - Don't forget: negative numbers (handle sign bit correctly)
 * 
 * ==================== TIME/SPACE COMPLEXITY SUMMARY ====================
 * 
 * Brute Force:     Time O(n²),  Space O(1)
 * Bit Manipulation: Time O(n),   Space O(n)  - better constants than Trie
 * Trie:            Time O(n),   Space O(n)  - optimal for large inputs
 * 
 * ==================== FOLLOW-UP QUESTIONS ====================
 * 
 * 1. "What if we need to find XOR of exactly 3 numbers?"
 *    - Extend Trie to 3D or use DP with bitmask
 * 
 * 2. "What if array is too large to fit in memory?"
 *    - External sorting + streaming approach
 *    - Divide and conquer with disk-based Trie
 * 
 * 3. "Can we do better than O(n) time?"
 *    - No, we must examine all elements at least once
 * 
 * 4. "What about online queries - adding numbers dynamically?"
 *    - Trie approach works perfectly - insert as numbers arrive
 *    - Query maximum XOR at any point in O(32) = O(1) time
 */
