# DIY: Remove All Adjacent Duplicates in String II

## Problem statement

Given a string `str` and an integer `k`, repeatedly find and remove `k` adjacent duplicate characters from `str` until no such run remains.

### Constraints

- `1 <= str.length <= 10^5`
- `2 <= k <= 10^4`
- `str` contains only lowercase English letters.

### Input

```java
// str = "akkkabbba", k = 2
```

### Output

```java
"akaba"
```

## Coding exercise

This is [Feature #3: Balloon Splash](03-feature-3-balloon-splash.md) with the story stripped away — it's the identical general-`k` problem, not just a related one. Implement `removeDuplicates(str, k)`.

## Solution

```java
import java.util.*;

class Solution {
    public static String removeDuplicates(String str, int k) {
        Deque<int[]> stack = new ArrayDeque<>(); // Each entry: {character, runLength}.

        for (char c : str.toCharArray()) {
            if (!stack.isEmpty() && stack.peek()[0] == c) {
                stack.peek()[1]++;
                if (stack.peek()[1] == k) {
                    stack.pop(); // This run reached k — remove it.
                }
            } else {
                stack.push(new int[]{c, 1});
            }
        }

        StringBuilder result = new StringBuilder();
        Iterator<int[]> bottomToTop = stack.descendingIterator();
        while (bottomToTop.hasNext()) {
            int[] entry = bottomToTop.next();
            for (int i = 0; i < entry[1]; i++) {
                result.append((char) entry[0]);
            }
        }
        return result.toString();
    }

    public static void main(String[] args) {
        System.out.println(removeDuplicates("akkkabbba", 2));
        // akaba
    }
}
```

Walking `"akkkabbba"` with `k = 2`: `a` pushes `(a,1)`; `k` pushes `(k,1)`; the next `k` bumps it to `(k,2)` — that hits `k`, so it's popped, leaving `(a,1)` on top; the third `k` doesn't match `a`, so it pushes `(k,1)` again; `a` doesn't match `k`, pushes `(a,1)`; `b` pushes `(b,1)`; the next `b` bumps to `(b,2)` — popped, exposing `(a,1)`; the third `b` doesn't match `a`, pushes `(b,1)`; final `a` doesn't match `b`, pushes `(a,1)`. Reading the stack bottom to top: `a`, `k`, `a`, `b`, `a` → `"akaba"`, matching the expected output.

## Complexity measures

Let **n** be the length of `str`.

- **Time:** `O(n)` — each character is pushed once and popped at most once.
- **Space:** `O(n)` — the stack can hold an entry for every character in the worst case, when no run ever reaches length `k`.
