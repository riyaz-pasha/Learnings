# DIY: Expressive Words

## Problem statement

Given a string `S` and an array of `words`, return how many words in `words` are **stretchy** with respect to `S`. A word is stretchy if `S` can be obtained by extending groups of repeated characters in the word to length `>= 3` (or leaving them as-is).

E.g. `word = "ddiinnso"` has character groups `["dd", "ii", "nn", "s", "o"]`, and is stretchy with respect to `S = "dddiiiinnssssssoooo"` — stretch `dd->ddd`, `ii->iiii`, keep `nn`, stretch `s->ssssss`, `o->oooo`.

### Input

```java
S = "tttttllll"
words = {"tl", "tll", "ttll", "ttl"}
```

### Output

```java
4
```

All four words can be stretched into `S`.

## Coding exercise

Implement `expressiveWords(S, words)`.

This is the exact underlying check from [Feature #5: Flag Words](05-feature-5-flag-words.md) (there called `flagWords`), just applied across a whole array and counted.

## Solution

```java
class Solution {

    public static int expressiveWords(String s, String[] words) {
        int count = 0;
        for (String word : words) {
            if (isStretchy(s, word)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isStretchy(String s, String w) {
        int i = 0;
        int j = 0;

        while (i < s.length() && j < w.length()) {
            if (s.charAt(i) != w.charAt(j)) {
                return false;
            }

            int lenS = repeatedLetters(s, i);
            int lenW = repeatedLetters(w, j);

            if (lenW > lenS || (lenS < 3 && lenS != lenW)) {
                return false;
            }

            i += lenS;
            j += lenW;
        }

        return i == s.length() && j == w.length();
    }

    private static int repeatedLetters(String str, int start) {
        int end = start;
        while (end < str.length() && str.charAt(end) == str.charAt(start)) {
            end++;
        }
        return end - start;
    }

    public static void main(String[] args) {
        String s = "tttttllll";
        String[] words = {"tl", "tll", "ttll", "ttl"};
        System.out.println(expressiveWords(s, words)); // 4
    }
}
```

## Complexity measures

Let **n** be the length of `S`, **k** the number of words, and **l** the average word length.

- **Time:** `O(k × (n + l))` — each word is checked with a two-pointer pass proportional to `S` and the word's own length.
- **Space:** `O(1)` per check.
