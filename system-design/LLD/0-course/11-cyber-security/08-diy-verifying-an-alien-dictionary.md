# DIY: Verifying an Alien Dictionary

## Problem statement

You are given a list of words written in an alien language. The aliens also use lowercase English letters, but possibly in a different order — the order of their alphabet is some permutation of the usual 26 letters.

Given a vector of words written in the alien language and the order of the alien alphabet, return `true` if and only if the given words are sorted lexicographically according to this alien order.

### Constraints

- `1 <= words.length <= 100`
- `1 <= words[i].length <= 20`
- `order.length == 26`
- All characters in `words[i]` and `order` are lowercase English letters.

### Input

```java
// Sample Input 1
words = ["hello", "world"]
order = "hwabcdefgijklmnopqrstuvxyz"

// Sample Input 2
words = ["educated", "educate"]
order = "educatebfghijklmnopqrsvwxyz"
```

### Output

```java
// Sample Output 1
true

// Sample Output 2
false
```

## Coding exercise

Implement `verifyAlienDictionary(words, order)`.

This is the exact same pattern as [Feature #2: Verify Message Integrity](02-feature-2-verify-message-integrity.md) — there, a session's encrypted messages had to appear in sorted order under a secret dictionary to prove they hadn't been tampered with; here it's the bare "is this word list sorted under a custom alphabet?" check with no story attached. Build a rank lookup for each letter's position in `order`, then walk adjacent word pairs comparing their first differing letter (falling back to length when one word is a prefix of the other).

## Solution

```java
import java.util.*;

class Solution {
    public static boolean verifyAlienDictionary(String[] words, String order) {
        int[] rank = new int[26];
        for (int i = 0; i < order.length(); i++) {
            rank[order.charAt(i) - 'a'] = i;
        }

        for (int i = 0; i < words.length - 1; i++) {
            if (!inOrder(words[i], words[i + 1], rank)) {
                return false;
            }
        }
        return true;
    }

    private static boolean inOrder(String a, String b, int[] rank) {
        int minLen = Math.min(a.length(), b.length());
        for (int i = 0; i < minLen; i++) {
            char ca = a.charAt(i);
            char cb = b.charAt(i);
            if (ca != cb) {
                return rank[ca - 'a'] < rank[cb - 'a'];
            }
        }
        return a.length() <= b.length();
    }

    public static void main(String[] args) {
        System.out.println(verifyAlienDictionary(
            new String[]{"hello", "world"}, "hwabcdefgijklmnopqrstuvxyz"));
        // true

        System.out.println(verifyAlienDictionary(
            new String[]{"educated", "educate"}, "educatebfghijklmnopqrsvwxyz"));
        // false
    }
}
```

`rank[]` turns "is letter x before letter y" into a simple integer comparison. For each adjacent pair of words, we scan for the first index where the two words differ and compare ranks there; if no such index exists within the shorter word's length, the pair is only correctly ordered if the first word is the shorter (or equal-length) one — a longer word can never sort before its own prefix.

## Complexity measures

Let **m** be the total number of letters across all words.

- **Time:** `O(m)` — building the 26-entry rank table is `O(1)`, and comparing every adjacent pair of words touches each letter at most once.
- **Space:** `O(1)` — the rank table always holds exactly 26 entries.
