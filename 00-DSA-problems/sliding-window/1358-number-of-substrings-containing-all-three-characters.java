import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SubstringsWithABC {
    
    // Approach 1: Brute Force
    // Time: O(n²), Space: O(1)
    public int numberOfSubstringsBruteForce(String s) {
        int count = 0;
        int n = s.length();
        
        for (int i = 0; i < n; i++) {
            int[] freq = new int[3];  // a, b, c
            
            for (int j = i; j < n; j++) {
                freq[s.charAt(j) - 'a']++;
                
                // Check if all three characters present
                if (freq[0] > 0 && freq[1] > 0 && freq[2] > 0) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    // Approach 2: Sliding Window (OPTIMAL)
    // Time: O(n), Space: O(1)
    public int numberOfSubstrings(String s) {
        int count = 0;
        int[] freq = new int[3];
        int left = 0;
        
        for (int right = 0; right < s.length(); right++) {
            freq[s.charAt(right) - 'a']++;
            
            // While window contains all three characters
            while (freq[0] > 0 && freq[1] > 0 && freq[2] > 0) {
                // All substrings from [left, right] to [left, n-1] are valid
                count += s.length() - right;
                
                // Shrink window
                freq[s.charAt(left) - 'a']--;
                left++;
            }
        }
        
        return count;
    }
    
    // Approach 3: Track Last Occurrence
    // Time: O(n), Space: O(1)
    public int numberOfSubstringsLastOccurrence(String s) {
        int count = 0;
        int[] lastSeen = {-1, -1, -1};  // Last index of a, b, c
        
        for (int i = 0; i < s.length(); i++) {
            lastSeen[s.charAt(i) - 'a'] = i;
            
            // If all three seen, count valid substrings ending at i
            if (lastSeen[0] != -1 && lastSeen[1] != -1 && lastSeen[2] != -1) {
                // Minimum of last seen positions + 1
                count += Math.min(lastSeen[0], Math.min(lastSeen[1], lastSeen[2])) + 1;
            }
        }
        
        return count;
    }
    
    // Approach 4: Two Pointers with Early Break
    // Time: O(n), Space: O(1)
    public int numberOfSubstringsTwoPointers(String s) {
        int count = 0;
        int n = s.length();
        
        for (int i = 0; i < n; i++) {
            int[] freq = new int[3];
            
            for (int j = i; j < n; j++) {
                freq[s.charAt(j) - 'a']++;
                
                if (freq[0] > 0 && freq[1] > 0 && freq[2] > 0) {
                    // Once we have all three, all remaining extensions are valid
                    count += n - j;
                    break;
                }
            }
        }
        
        return count;
    }
    
    // Approach 5: Using Total - Invalid
    // Count total subarrays, subtract those without all three
    // Time: O(n), Space: O(1)
    public int numberOfSubstringsComplement(String s) {
        int n = s.length();
        int total = n * (n + 1) / 2;  // Total substrings
        
        // Subtract substrings that don't have all three
        return total - countWithoutAllThree(s);
    }
    
    private int countWithoutAllThree(String s) {
        // Count substrings missing at least one character
        int count = 0;
        
        // Missing 'a'
        count += countSubstringsWithout(s, 'a');
        // Missing 'b'
        count += countSubstringsWithout(s, 'b');
        // Missing 'c'
        count += countSubstringsWithout(s, 'c');
        
        // Add back those missing two (counted twice)
        count -= countSubstringsWithout(s, 'a', 'b');
        count -= countSubstringsWithout(s, 'b', 'c');
        count -= countSubstringsWithout(s, 'a', 'c');
        
        // Subtract those missing all three (counted thrice)
        count += countSubstringsWithout(s, 'a', 'b', 'c');
        
        return count;
    }
    
    private int countSubstringsWithout(String s, char... chars) {
        Set<Character> excluded = new HashSet<>();
        for (char c : chars) excluded.add(c);
        
        int count = 0;
        int len = 0;
        
        for (char c : s.toCharArray()) {
            if (excluded.contains(c)) {
                len = 0;
            } else {
                len++;
                count += len;
            }
        }
        
        return count;
    }
    
    // Approach 6: Greedy with Position Tracking
    // Time: O(n), Space: O(1)
    public int numberOfSubstringsGreedy(String s) {
        int count = 0;
        int[] pos = {-1, -1, -1};
        
        for (int i = 0; i < s.length(); i++) {
            pos[s.charAt(i) - 'a'] = i;
            
            int minPos = Math.min(pos[0], Math.min(pos[1], pos[2]));
            if (minPos != -1) {
                count += minPos + 1;
            }
        }
        
        return count;
    }
    
    // Helper: Visualize valid substrings
    private static void visualizeSubstrings(String s) {
        System.out.println("\n=== All Valid Substrings ===");
        System.out.println("String: " + s);
        System.out.println("\nValid substrings (containing a, b, and c):");
        
        int count = 0;
        List<String> substrings = new ArrayList<>();
        
        for (int i = 0; i < s.length(); i++) {
            boolean hasA = false, hasB = false, hasC = false;
            
            for (int j = i; j < s.length(); j++) {
                char c = s.charAt(j);
                if (c == 'a') hasA = true;
                if (c == 'b') hasB = true;
                if (c == 'c') hasC = true;
                
                if (hasA && hasB && hasC) {
                    String substr = s.substring(i, j + 1);
                    substrings.add(substr + " [" + i + "," + j + "]");
                    count++;
                }
            }
        }
        
        // Group by length for better visualization
        substrings.sort(Comparator.comparingInt(str -> 
            str.indexOf('[') - str.indexOf(' ')));
        
        for (String substr : substrings) {
            System.out.println("  " + substr);
        }
        
        System.out.println("\nTotal count: " + count);
    }
    
    // Test cases
    public static void main(String[] args) {
        SubstringsWithABC solver = new SubstringsWithABC();
        
        // Test Case 1
        System.out.println("Test Case 1:");
        String s1 = "abcabc";
        System.out.println("Input: s = \"" + s1 + "\"");
        System.out.println("Output: " + solver.numberOfSubstrings(s1));
        System.out.println("Expected: 10");
        visualizeSubstrings(s1);
        
        // Test Case 2
        System.out.println("\n\nTest Case 2:");
        String s2 = "aaacb";
        System.out.println("Input: s = \"" + s2 + "\"");
        System.out.println("Output: " + solver.numberOfSubstrings(s2));
        System.out.println("Expected: 3");
        visualizeSubstrings(s2);
        
        // Test Case 3
        System.out.println("\n\nTest Case 3:");
        String s3 = "abc";
        System.out.println("Input: s = \"" + s3 + "\"");
        System.out.println("Output: " + solver.numberOfSubstrings(s3));
        System.out.println("Expected: 1");
        visualizeSubstrings(s3);
        
        // Test Case 4: All same character
        System.out.println("\n\nTest Case 4: Missing characters");
        String s4 = "aaaa";
        System.out.println("Input: s = \"" + s4 + "\"");
        System.out.println("Output: " + solver.numberOfSubstrings(s4));
        System.out.println("Expected: 0");
        
        // Test Case 5: Complex pattern
        System.out.println("\n\nTest Case 5: Complex Pattern");
        String s5 = "abacbc";
        System.out.println("Input: s = \"" + s5 + "\"");
        System.out.println("Output: " + solver.numberOfSubstrings(s5));
        
        // Test Case 6: Long string with pattern
        System.out.println("\n\nTest Case 6: Repeating Pattern");
        String s6 = "abcabcabc";
        System.out.println("Input: s = \"" + s6 + "\"");
        System.out.println("Output: " + solver.numberOfSubstrings(s6));
        
        // Step-by-step trace
        System.out.println("\n\n=== Step-by-Step: Sliding Window ===");
        stepByStepSlidingWindow(s1);
        
        // Last occurrence approach
        System.out.println("\n\n=== Last Occurrence Approach ===");
        stepByStepLastOccurrence(s1);
        
        // Key insight explanation
        System.out.println("\n\n=== Key Insight Explanation ===");
        explainKeyInsight();
        
        // Algorithm comparison
        System.out.println("\n\n=== Algorithm Comparison ===");
        compareAlgorithms();
        
        // Compare all approaches
        System.out.println("\n\n=== Comparing All Approaches ===");
        compareAllApproaches(s1);
    }
    
    private static void stepByStepSlidingWindow(String s) {
        System.out.println("String: \"" + s + "\"");
        System.out.println("\nKey: When window has all 3 chars, ALL extensions to right are valid!");
        
        int count = 0;
        int[] freq = new int[3];
        int left = 0;
        
        System.out.println("\n┌───────┬──────┬──────────┬─────────────┬──────────────────────┐");
        System.out.println("│ right │ char │  freq    │ window      │ Action               │");
        System.out.println("├───────┼──────┼──────────┼─────────────┼──────────────────────┤");
        
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            freq[c - 'a']++;
            
            StringBuilder action = new StringBuilder();
            int addedThisStep = 0;
            
            while (freq[0] > 0 && freq[1] > 0 && freq[2] > 0) {
                int added = s.length() - right;
                addedThisStep += added;
                count += added;
                
                action.append("Valid! Add ").append(added);
                action.append(", shrink left");
                
                freq[s.charAt(left) - 'a']--;
                left++;
                
                if (freq[0] > 0 && freq[1] > 0 && freq[2] > 0) {
                    action.append(" | ");
                }
            }
            
            if (action.length() == 0) {
                action.append("Not all 3 yet");
            } else {
                action.append(" (total: ").append(count).append(")");
            }
            
            System.out.printf("│   %d   │  %c   │ [%d,%d,%d] │ [%d,%d] \"%s\" │ %-20s │%n",
                            right, c, freq[0], freq[1], freq[2], 
                            left, right, s.substring(left, right + 1),
                            action.length() > 20 ? action.substring(0, 17) + "..." : action.toString());
        }
        
        System.out.println("└───────┴──────┴──────────┴─────────────┴──────────────────────┘");
        System.out.println("Total: " + count);
    }
    
    private static void stepByStepLastOccurrence(String s) {
        System.out.println("String: \"" + s + "\"");
        System.out.println("\nKey: For each position, count how many valid substrings END here!");
        System.out.println("Valid starting positions: 0 to min(last_a, last_b, last_c)");
        
        int count = 0;
        int[] lastSeen = {-1, -1, -1};
        
        System.out.println("\n┌───────┬──────┬─────────────────┬─────────────────────────────┐");
        System.out.println("│   i   │ char │   lastSeen      │ Valid starts & count        │");
        System.out.println("├───────┼──────┼─────────────────┼─────────────────────────────┤");
        
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            lastSeen[c - 'a'] = i;
            
            StringBuilder info = new StringBuilder();
            
            if (lastSeen[0] != -1 && lastSeen[1] != -1 && lastSeen[2] != -1) {
                int minPos = Math.min(lastSeen[0], Math.min(lastSeen[1], lastSeen[2]));
                int added = minPos + 1;
                count += added;
                
                info.append("Positions 0-").append(minPos);
                info.append(" (add ").append(added).append(")");
                info.append(" total: ").append(count);
            } else {
                info.append("Not all 3 seen yet");
            }
            
            System.out.printf("│   %d   │  %c   │ [%d, %d, %d]   │ %-27s │%n",
                            i, c, lastSeen[0], lastSeen[1], lastSeen[2],
                            info.toString());
        }
        
        System.out.println("└───────┴──────┴─────────────────┴─────────────────────────────┘");
        System.out.println("Total: " + count);
    }
    
    private static void explainKeyInsight() {
        System.out.println("TWO KEY INSIGHTS:\n");
        
        System.out.println("INSIGHT 1: Extension Counting");
        System.out.println("When window [left, right] contains all 3 characters:");
        System.out.println("- Subarray [left, right] is valid");
        System.out.println("- Subarray [left, right+1] is valid");
        System.out.println("- Subarray [left, right+2] is valid");
        System.out.println("- ... all the way to [left, n-1]");
        System.out.println("→ Total: (n - right) valid subarrays");
        
        System.out.println("\nExample: s = \"abcabc\", at position where left=0, right=2");
        System.out.println("  Window \"abc\" has all 3");
        System.out.println("  Valid subarrays: \"abc\", \"abca\", \"abcab\", \"abcabc\"");
        System.out.println("  Count: 6 - 2 = 4 subarrays");
        
        System.out.println("\n\nINSIGHT 2: Minimum Last Position");
        System.out.println("For substring ending at position i:");
        System.out.println("- Must include at least one a, b, c");
        System.out.println("- Valid starting positions: 0 to min(last_a, last_b, last_c)");
        System.out.println("- Count: min(last_a, last_b, last_c) + 1");
        
        System.out.println("\nExample: s = \"abcabc\", at i=5 (char='c')");
        System.out.println("  last_a=4, last_b=4, last_c=5");
        System.out.println("  min = 4");
        System.out.println("  Valid starts: 0,1,2,3,4 → 5 substrings ending at 5");
        
        System.out.println("\n\nWhy minimum?");
        System.out.println("- The RAREST character determines valid range");
        System.out.println("- Can't start before the last occurrence of rarest char");
    }
    
    private static void compareAlgorithms() {
        System.out.println("┌──────────────────────┬──────────────┬──────────────┬─────────────────┐");
        System.out.println("│ Approach             │ Time         │ Space        │ Notes           │");
        System.out.println("├──────────────────────┼──────────────┼──────────────┼─────────────────┤");
        System.out.println("│ Brute Force          │ O(n²)        │ O(1)         │ Check all       │");
        System.out.println("│ Sliding Window       │ O(n)         │ O(1)         │ Optimal         │");
        System.out.println("│ Last Occurrence      │ O(n)         │ O(1)         │ Most elegant    │");
        System.out.println("│ Two Pointers         │ O(n²) worst  │ O(1)         │ Early break     │");
        System.out.println("│ Complement           │ O(n)         │ O(1)         │ Complex logic   │");
        System.out.println("│ Greedy               │ O(n)         │ O(1)         │ Same as last    │");
        System.out.println("└──────────────────────┴──────────────┴──────────────┴─────────────────┘");
    }
    
    private static void compareAllApproaches(String s) {
        SubstringsWithABC solver = new SubstringsWithABC();
        
        System.out.println("Input: s = \"" + s + "\"");
        System.out.println("\nBrute Force:      " + solver.numberOfSubstringsBruteForce(s));
        System.out.println("Sliding Window:   " + solver.numberOfSubstrings(s));
        System.out.println("Last Occurrence:  " + solver.numberOfSubstringsLastOccurrence(s));
        System.out.println("Two Pointers:     " + solver.numberOfSubstringsTwoPointers(s));
        System.out.println("Complement:       " + solver.numberOfSubstringsComplement(s));
        System.out.println("Greedy:           " + solver.numberOfSubstringsGreedy(s));
        
        // Performance test
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            sb.append((char)('a' + i % 3));
        }
        String large = sb.toString();
        
        System.out.println("\nPerformance Test (n=100000):");
        
        long start = System.nanoTime();
        solver.numberOfSubstrings(large);
        long end = System.nanoTime();
        System.out.println("Sliding Window:  " + (end - start) / 1_000_000.0 + " ms");
        
        start = System.nanoTime();
        solver.numberOfSubstringsLastOccurrence(large);
        end = System.nanoTime();
        System.out.println("Last Occurrence: " + (end - start) / 1_000_000.0 + " ms");
        
        start = System.nanoTime();
        solver.numberOfSubstringsGreedy(large);
        end = System.nanoTime();
        System.out.println("Greedy:          " + (end - start) / 1_000_000.0 + " ms");
    }
}

/*
DETAILED EXPLANATION:

PROBLEM: Count Substrings with All Three Characters

Count substrings that contain at least one 'a', 'b', and 'c'.

EXAMPLE WALKTHROUGH: "abcabc"

Valid substrings:
1. "abc" [0,2]
2. "abca" [0,3]
3. "abcab" [0,4]
4. "abcabc" [0,5]
5. "bca" [1,3]
6. "bcab" [1,4]
7. "bcabc" [1,5]
8. "cab" [2,4]
9. "cabc" [2,5]
10. "abc" [3,5]

Total: 10

KEY INSIGHT 1: EXTENSION COUNTING (Sliding Window)

When we have a valid window [left, right] with all 3 characters:
- ALL extensions to the right are also valid!
- Substrings: [left, right], [left, right+1], ..., [left, n-1]
- Count: n - right

Example: left=0, right=2, n=6, window="abc"
- Valid: "abc", "abca", "abcab", "abcabc"
- Count: 6 - 2 = 4

ALGORITHM (SLIDING WINDOW):

1. Use left and right pointers
2. Expand right, track character frequencies
3. When all three present:
   a. Add (n - right) to count
   b. Shrink from left
   c. Repeat while still valid
4. Continue expanding right

DETAILED TRACE: "abcabc"

right=0, char='a': freq=[1,0,0], window="a"
  Not all 3 yet

right=1, char='b': freq=[1,1,0], window="ab"
  Not all 3 yet

right=2, char='c': freq=[1,1,1], window="abc"
  Valid! Add 6-2=4
  Shrink: left=1, freq=[0,1,1], window="bc"

right=3, char='a': freq=[1,1,1], window="bca"
  Valid! Add 6-3=3
  Shrink: left=2, freq=[1,0,1], window="ca"

right=4, char='b': freq=[1,1,1], window="cab"
  Valid! Add 6-4=2
  Shrink: left=3, freq=[1,1,0], window="ab"

right=5, char='c': freq=[1,1,1], window="abc"
  Valid! Add 6-5=1
  Shrink: left=4, freq=[0,1,1], window="bc"

Total: 4 + 3 + 2 + 1 = 10 ✓

WHY SHRINK MULTIPLE TIMES?

When we shrink and window is still valid:
- We found MORE valid starting positions
- Each shrink represents a different valid subarray
- Continue until window becomes invalid

KEY INSIGHT 2: MINIMUM LAST POSITION

Alternative elegant approach:
For each position i, count valid substrings ENDING at i.

Valid starting positions: 0 to min(last_a, last_b, last_c)

Why? The substring must include:
- At least one 'a' (must start at or after position 0, before last_a)
- At least one 'b' (must start at or after position 0, before last_b)
- At least one 'c' (must start at or after position 0, before last_c)

The RAREST character (last seen earliest) limits our range.

ALGORITHM (LAST OCCURRENCE):

1. Track last seen position of a, b, c
2. For each position i:
   a. Update last seen for current char
   b. If all three seen: count += min(last_a, last_b, last_c) + 1

DETAILED TRACE: "abcabc"

i=0, char='a': lastSeen=[0,-1,-1]
  Not all seen yet

i=1, char='b': lastSeen=[0,1,-1]
  Not all seen yet

i=2, char='c': lastSeen=[0,1,2]
  All seen! min=0, add 0+1=1
  Substring: "abc" [0,2]

i=3, char='a': lastSeen=[3,1,2]
  All seen! min=1, add 1+1=2
  Substrings: "bca" [1,3], "abca" [0,3]

i=4, char='b': lastSeen=[3,4,2]
  All seen! min=2, add 2+1=3
  Substrings: "cab" [2,4], "bcab" [1,4], "abcab" [0,4]

i=5, char='c': lastSeen=[3,4,5]
  All seen! min=3, add 3+1=4
  Substrings: "abc" [3,5], "cabc" [2,5], "bcabc" [1,5], "abcabc" [0,5]

Total: 1 + 2 + 3 + 4 = 10 ✓

COMPARISON OF APPROACHES:

Sliding Window:
- Counts by extending right
- When valid: all right extensions count
- Shrinks to find more valid windows

Last Occurrence:
- Counts by ending position
- When valid: all left starting points count
- Simpler logic, more elegant

Both O(n) time, O(1) space!

COMPLEXITY ANALYSIS:

Sliding Window:
Time: O(n)
  - Right pointer traverses once: O(n)
  - Left pointer moves at most n times total: O(n)
  - Amortized: O(n)
Space: O(1)
  - Fixed size frequency array

Last Occurrence:
Time: O(n)
  - Single pass through string
Space: O(1)
  - Three integers for last positions

Brute Force:
Time: O(n²)
  - For each start: O(n)
  - Find end with all three: O(n)
Space: O(1)

EDGE CASES:

1. All same character: "aaaa"
   - No valid substrings → 0

2. Missing one character: "aabb"
   - No valid substrings → 0

3. Minimum valid: "abc"
   - One valid substring → 1

4. All different arrangements: "abcabc"
   - Many valid substrings

5. Long repeating pattern
   - Efficient with O(n) solution

WHEN TO USE EACH APPROACH:

Sliding Window:
- When thinking about extending ranges
- Natural for "at least" problems
- Good for explaining window concept

Last Occurrence:
- When thinking about ending positions
- More elegant and concise
- Easier to code in interview

RELATED PROBLEMS:

1. Longest Substring with At Least K Repeating
2. Longest Substring with At Most K Distinct
3. Subarrays with K Different Integers
4. Count Nice Subarrays
5. Minimum Window Substring

INTERVIEW STRATEGY:

1. Clarify problem: at least one of each
2. Recognize pattern: sliding window or counting
3. Key insight: count extensions OR endings
4. Choose approach based on comfort
5. Trace through example carefully
6. Discuss O(n) time, O(1) space
7. Handle edge cases

COMMON MISTAKES:

1. Counting each substring individually (O(n²))
2. Not realizing all extensions are valid
3. Wrong calculation of valid starting positions
4. Forgetting to handle uninitialized positions
5. Off-by-one errors in counting

OPTIMIZATION:

Both optimal approaches are already O(n)!
Choose based on:
- Code simplicity: Last Occurrence
- Conceptual clarity: Sliding Window
- Personal preference: Either works!

This problem beautifully demonstrates counting technique!
*/
