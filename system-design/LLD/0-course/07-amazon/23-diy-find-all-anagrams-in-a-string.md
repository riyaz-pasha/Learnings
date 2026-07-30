# DIY: Find All Anagrams in a String

## Problem statement

You are given two strings, `s` and `p`, made up of lowercase English letters only (length up to 20,100 each). Find all the starting indices in `s` where an anagram of `p` begins. The order of the output does not matter.

### Input

```java
s = "aaacbaccbabad"
p = "abc"
```

### Output

```java
{2, 3, 4, 7}
```

(`s.substring(2, 5) = "acb"`, `s.substring(3, 6) = "cba"`, `s.substring(4, 7) = "bac"`, and `s.substring(7, 10) = "cba"` — each a rearrangement of `"abc"`.)

## Coding exercise

Implement `findAnagrams(s, p)`, returning every starting index in `s` where a window of `p`'s length is an anagram of `p`.

This is the exact same pattern as [Feature #6: Products Frequently Viewed Together](06-feature-6-products-frequently-viewed-together.md) — there, Amazon slid a fixed-size window over a sequence of viewed products looking for a matching combination; here it's the bare pattern with no story attached. Keep a 26-length letter-count array for `p` and a matching count array for the current window of `s`; slide the window one character at a time, comparing counts.

## Solution

```java
import java.util.*;

class Solution {
    public static int[] findAnagrams(String s, String p) {
        List<Integer> result = new ArrayList<>();
        if (s.length() < p.length()) return toArray(result);

        int[] need = new int[26];
        int[] window = new int[26];
        for (char c : p.toCharArray()) need[c - 'a']++;

        int windowSize = p.length();
        for (int i = 0; i < s.length(); i++) {
            window[s.charAt(i) - 'a']++;
            if (i >= windowSize) {
                window[s.charAt(i - windowSize) - 'a']--;
            }
            if (i >= windowSize - 1 && Arrays.equals(need, window)) {
                result.add(i - windowSize + 1);
            }
        }
        return toArray(result);
    }

    private static int[] toArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }

    public static void main(String[] args) {
        int[] result = findAnagrams("aaacbaccbabad", "abc");
        System.out.println(Arrays.toString(result));
        // [2, 3, 4, 7]
    }
}
```

## Complexity measures

Let **n** be the length of `s` and **m** the length of `p`.

- **Time:** `O(n)` — each character enters and leaves the sliding window exactly once; comparing the two 26-length count arrays is `O(1)`.
- **Space:** `O(1)` — the two count arrays are fixed at 26 entries regardless of input size.
