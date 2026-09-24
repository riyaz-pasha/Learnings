import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
 * 🔍 Problem
 * Given a text T of length n and a pattern P of length m, find all the
 * positions in T where P occurs.
 */

class RabinKarp {

    // Generate a random prime for the modulus to minimize collisions
    private final long q = BigInteger.probablePrime(31, new Random()).longValue();
    private final int d = 256; // The number of characters in the alphabet (e.g., 256 for extended ASCII)

    public List<Integer> rabinKarpSearch(String text, String pattern) {
        List<Integer> result = new ArrayList<>();

        int patternLength = pattern.length();
        int textLength = text.length();

        // Pre-compute d^(m-1) % q for efficient hash calculation
        long h = 1;
        for (int i = 1; i < patternLength; i++) {
            h = (h * d) % q;
        }

        long patternHash = this.calculateHash(pattern);
        long textHash = this.calculateHash(text.substring(0, patternLength));

        for (int textIndex = 0; textIndex <= (textLength - patternLength); textIndex++) {
            if (patternHash == textHash) {
                boolean match = true;
                for (int patternIndex = 0; patternIndex < patternLength; patternIndex++) {
                    if (pattern.charAt(patternIndex) != text.charAt(textIndex + patternIndex)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    result.add(textIndex);
                }
            }

            if (textIndex < textLength - patternLength) {
                textHash = (this.d * (textHash - text.charAt(textIndex) * h) + text.charAt(textIndex + patternLength))
                        % this.q;

                // Make sure the hash is non-negative
                if (textHash < 0) {
                    textHash += this.q;
                }
            }
        }

        return result;
    }

    private long calculateHash(String text) {
        long hash = 0;

        for (int index = 0; index < text.length(); index++) {
            hash = ((hash * this.d) + text.charAt(index)) % this.q;
        }

        return hash;
    }

    /*
     * 🧮 Time & Space Complexities
     * ✅ Time Complexity
     * - Preprocessing hash of pattern: O(m)
     * - Hashing text windows: O(n - m + 1)
     * - Worst case: O((n - m + 1) × m) → when all hashes match but patterns don't
     * (many false positives)
     * - Average case: O(n + m) — efficient when hash collisions are rare.
     * 
     * ✅ Space Complexity
     * - O(1) extra space for basic version
     * - O(k) if you're storing positions or multiple pattern hashes
     */

}

/*
 * 🚀 Idea Behind Rabin-Karp
 * Rabin-Karp uses a hashing technique:
 * - It computes the hash of the pattern.
 * - Then, it computes the hash of every substring of text of length m using a
 * rolling hash.
 * - If the hashes match, it does a direct string comparison to avoid false
 * positives (hash collisions).
 * 
 * 🧠 Hash Function
 * Given:
 * - Base d (usually 256 for ASCII)
 * - Prime modulus q (to avoid large numbers and reduce collisions)
 * We use:
 * hash("abcd") = (a×d³ + b×d² + c×d¹ + d×d⁰) % q
 * 
 * This can be computed in O(1) for each window using the rolling hash:
 * hash(text[i+1 to i+m]) =
 * (d × (hash(text[i to i+m-1]) - text[i]×h) + text[i+m]) % q
 * Where h = d^(m-1) % q
 */