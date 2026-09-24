# DIY: Number of Matching Subsequences

## Problem statement

Given a string `S` and an array of strings `words`, find the number of strings in `words` that are a subsequence of `S`.

### Input

```java
S = "abcde"
words = {"a", "bb", "acd", "ace"}
```

### Output

```java
3
```

`"a"`, `"acd"`, and `"ace"` are all subsequences of `S`; `"bb"` is not (`S` only contains one `b`). So the count is `3`.

## Coding exercise

Implement `numMatchingSubseq(S, words)`, returning the number of strings in `words` that are a subsequence of `S`.

This is the exact same problem as [Feature #1: Possible Matches](01-feature-1-possible-matches.md) — there, we counted how many students' token strings a plagiarised document could have come from; here it's the bare pattern with no story attached. Bucket every word by the character it's currently waiting for, and advance the buckets in a single pass over `S`.

## Solution

```java
import java.util.*;

class Solution {

    static class Progress {
        final String word;
        int pos;
        Progress(String word) { this.word = word; this.pos = 0; }
    }

    public static int numMatchingSubseq(String S, String[] words) {
        List<Progress>[] waitingList = new List[128];
        for (int c = 0; c < 128; c++) {
            waitingList[c] = new ArrayList<>();
        }

        for (String word : words) {
            waitingList[word.charAt(0)].add(new Progress(word));
        }

        int count = 0;
        for (char c : S.toCharArray()) {
            List<Progress> waiting = waitingList[c];
            waitingList[c] = new ArrayList<>();
            for (Progress p : waiting) {
                p.pos++;
                if (p.pos == p.word.length()) {
                    count++;
                } else {
                    waitingList[p.word.charAt(p.pos)].add(p);
                }
            }
        }
        return count;
    }

    public static void main(String[] args) {
        String S = "abcde";
        String[] words = {"a", "bb", "acd", "ace"};

        System.out.println(numMatchingSubseq(S, words));
        // 3
    }
}
```

## Complexity measures

Let **n** be the length of `S` and **w** be the total length of all strings in `words` combined.

- **Time:** `O(n + w)` — each character of `S` is visited once, and each character of every word moves between buckets at most once.
- **Space:** `O(w)` — the buckets together hold at most one entry per character across all words.
