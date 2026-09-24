import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * The DNA sequence is composed of a series of nucleotides abbreviated as 'A',
 * 'C', 'G', and 'T'.
 * 
 * For example, "ACGAATTCCG" is a DNA sequence.
 * When studying DNA, it is useful to identify repeated sequences within the
 * DNA.
 * 
 * Given a string s that represents a DNA sequence, return all the
 * 10-letter-long sequences (substrings) that occur more than once in a DNA
 * molecule. You may return the answer in any order.
 * 
 * Example 1:
 * Input: s = "AAAAACCCCCAAAAACCCCCCAAAAAGGGTTT"
 * Output: ["AAAAACCCCC","CCCCCAAAAA"]
 * 
 * Example 2:
 * Input: s = "AAAAAAAAAAAAA"
 * Output: ["AAAAAAAAAA"]
 */

class RepeatedDNASequences {

    public List<String> findRepeatedDnaSequences(String s) {
        if (s.length() < 10) {
            return Collections.emptyList();
        }
        HashSet<String> DNAs = new HashSet<>();
        HashSet<String> repeatedDNAs = new HashSet<>();
        for (int i = 0; i <= s.length() - 10; i++) {
            String dna = s.substring(i, i + 10);
            if (!DNAs.add(dna)) {
                repeatedDNAs.add(dna);
            }
        }
        return new ArrayList<>(repeatedDNAs);
    }

}

class Solution2 {
    public List<String> findRepeatedDnaSequences(String s) {
        if (s == null || s.length() < 10)
            return Collections.emptyList();

        // map nucleotides to 2-bit values
        int[] map = new int[128];
        map['A'] = 0; // 00
        map['C'] = 1; // 01
        map['G'] = 2; // 10
        map['T'] = 3; // 11

        Set<Integer> seen = new HashSet<>();
        Set<Integer> repeated = new HashSet<>();

        int bitmask = 0; // stores current 10-letter window encoded in 20 bits
        int windowBits = 20; // 10 letters * 2 bits
        int mask = (1 << windowBits) - 1; // keep lower 20 bits

        // Build initial window of first 9 chars (we will add the 10th in loop)
        for (int i = 0; i < 9; i++) {
            bitmask = (bitmask << 2) | map[s.charAt(i)];
        }

        for (int i = 9; i < s.length(); i++) {
            // add next char (10th in window)
            bitmask = ((bitmask << 2) | map[s.charAt(i)]) & mask;

            if (!seen.add(bitmask)) {
                repeated.add(bitmask);
            }
        }

        // decode repeated encoded integers back to strings
        List<String> result = new ArrayList<>(repeated.size());
        for (int code : repeated)
            result.add(decode(code));
        return result;
    }

    // decode 20-bit code back into 10-char DNA string
    private String decode(int code) {
        char[] chars = new char[10];
        for (int i = 9; i >= 0; i--) {
            int val = code & 3; // 2 bits
            chars[i] = valToChar(val);
            code >>= 2;
        }
        return new String(chars);
    }

    private char valToChar(int v) {
        switch (v) {
            case 0:
                return 'A';
            case 1:
                return 'C';
            case 2:
                return 'G';
            default:
                return 'T';
        }
    }

}

class DNASequences {

    /**
     * Solution 1: HashSet Approach (Most Straightforward)
     * Time Complexity: O(n) where n is length of string
     * Space Complexity: O(n)
     */
    public List<String> findRepeatedDnaSequences1(String s) {
        if (s.length() < 10)
            return new ArrayList<>();

        Set<String> seen = new HashSet<>();
        Set<String> repeated = new HashSet<>();

        for (int i = 0; i <= s.length() - 10; i++) {
            String sequence = s.substring(i, i + 10);
            if (seen.contains(sequence)) {
                repeated.add(sequence);
            } else {
                seen.add(sequence);
            }
        }

        return new ArrayList<>(repeated);
    }

    /**
     * Solution 2: Rolling Hash Approach (Most Efficient)
     * Uses bit manipulation to represent DNA sequences as integers
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public List<String> findRepeatedDnaSequences2(String s) {
        if (s.length() < 10)
            return new ArrayList<>();

        // Map characters to 2-bit values: A=00, C=01, G=10, T=11
        int[] charMap = new int[256];
        charMap['A'] = 0;
        charMap['C'] = 1;
        charMap['G'] = 2;
        charMap['T'] = 3;

        Set<Integer> seen = new HashSet<>();
        Set<String> repeated = new HashSet<>();

        int hash = 0;
        int mask = (1 << 20) - 1; // 20 bits mask for 10 nucleotides

        for (int i = 0; i < s.length(); i++) {
            // Shift hash left by 2 bits and add new character
            hash = (hash << 2) & mask | charMap[s.charAt(i)];

            if (i >= 9) { // We have a complete 10-character window
                if (seen.contains(hash)) {
                    repeated.add(s.substring(i - 9, i + 1));
                } else {
                    seen.add(hash);
                }
            }
        }

        return new ArrayList<>(repeated);
    }

    /**
     * Solution 3: HashMap with Count Approach
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public List<String> findRepeatedDnaSequences3(String s) {
        if (s.length() < 10)
            return new ArrayList<>();

        Map<String, Integer> sequenceCount = new HashMap<>();
        List<String> result = new ArrayList<>();

        for (int i = 0; i <= s.length() - 10; i++) {
            String sequence = s.substring(i, i + 10);
            sequenceCount.put(sequence, sequenceCount.getOrDefault(sequence, 0) + 1);

            // Add to result when count reaches 2 (first time it becomes repeated)
            if (sequenceCount.get(sequence) == 2) {
                result.add(sequence);
            }
        }

        return result;
    }

    /**
     * Solution 4: Space-Optimized Rolling Hash with Rabin-Karp
     * Uses polynomial rolling hash instead of bit manipulation
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public List<String> findRepeatedDnaSequences4(String s) {
        if (s.length() < 10)
            return new ArrayList<>();

        final int BASE = 4;
        final int WINDOW = 10;
        final long MOD = (long) 1e9 + 7;

        // Calculate base^(WINDOW-1) % MOD
        long basePower = 1;
        for (int i = 0; i < WINDOW - 1; i++) {
            basePower = (basePower * BASE) % MOD;
        }

        Map<Character, Integer> charToNum = Map.of('A', 0, 'C', 1, 'G', 2, 'T', 3);
        Set<Long> seen = new HashSet<>();
        Set<String> repeated = new HashSet<>();

        long hash = 0;

        for (int i = 0; i < s.length(); i++) {
            // Add new character to hash
            hash = (hash * BASE + charToNum.get(s.charAt(i))) % MOD;

            if (i >= WINDOW) {
                // Remove leftmost character
                hash = (hash - charToNum.get(s.charAt(i - WINDOW)) * basePower % MOD + MOD) % MOD;
            }

            if (i >= WINDOW - 1) {
                if (seen.contains(hash)) {
                    repeated.add(s.substring(i - WINDOW + 1, i + 1));
                } else {
                    seen.add(hash);
                }
            }
        }

        return new ArrayList<>(repeated);
    }

    // Test method to demonstrate all solutions
    public static void main(String[] args) {
        DNASequences solution = new DNASequences();

        // Test cases
        String test1 = "AAAAACCCCCAAAAACCCCCCAAAAAGGGTTT";
        String test2 = "AAAAAAAAAAAAA";
        String test3 = "AAAAAAAAAAA"; // Edge case: exactly 11 characters

        System.out.println("Test Case 1: " + test1);
        System.out.println("Solution 1: " + solution.findRepeatedDnaSequences1(test1));
        System.out.println("Solution 2: " + solution.findRepeatedDnaSequences2(test1));
        System.out.println("Solution 3: " + solution.findRepeatedDnaSequences3(test1));
        System.out.println("Solution 4: " + solution.findRepeatedDnaSequences4(test1));

        System.out.println("\nTest Case 2: " + test2);
        System.out.println("Solution 1: " + solution.findRepeatedDnaSequences1(test2));
        System.out.println("Solution 2: " + solution.findRepeatedDnaSequences2(test2));
        System.out.println("Solution 3: " + solution.findRepeatedDnaSequences3(test2));
        System.out.println("Solution 4: " + solution.findRepeatedDnaSequences4(test2));

        System.out.println("\nTest Case 3: " + test3);
        System.out.println("Solution 1: " + solution.findRepeatedDnaSequences1(test3));
        System.out.println("Solution 2: " + solution.findRepeatedDnaSequences2(test3));
    }

}
