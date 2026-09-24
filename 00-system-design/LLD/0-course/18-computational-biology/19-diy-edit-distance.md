# DIY: Edit Distance

## Problem statement

Given two strings `str1` and `str2`, return the minimum number of edit operations required to convert `str1` into `str2`.

Note: you are allowed to insert, delete, or replace characters for this conversion.

### Input

```java
// Sample Input 1: str1 = "intention", str2 = "execution"
// Sample Input 2: str1 = "abdca", str2 = "cbda"
// Sample Input 3: str1 = "passport", str2 = "ppsspqrt"
```

### Output

```java
// Sample Output 1: 5
// Sample Output 2: 2
// Sample Output 3: 2
```

## Coding exercise

Implement the `minDistance(str1, str2)` function, where `str1` and `str2` are the input strings. The function returns the minimum number of edit operations required to convert `str1` into `str2`.

This is exactly [Feature #8: Similarity Measure Between DNA Samples](08-feature-8-similarity-measure-between-dna-samples.md) — the same 2D dynamic-programming table, just renamed away from the DNA framing.

## Solution

```java
class Solution {
    public static int minDistance(String str1, String str2) {
        int n = str1.length();
        int m = str2.length();
        int[][] d = new int[n + 1][m + 1];

        for (int i = 0; i <= n; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= m; j++) {
            d[0][j] = j;
        }

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    d[i][j] = d[i - 1][j - 1];
                } else {
                    int delete = d[i - 1][j];
                    int insert = d[i][j - 1];
                    int replace = d[i - 1][j - 1];
                    d[i][j] = 1 + Math.min(delete, Math.min(insert, replace));
                }
            }
        }
        return d[n][m];
    }

    public static void main(String[] args) {
        System.out.println(minDistance("intention", "execution")); // 5
        System.out.println(minDistance("abdca", "cbda"));          // 2
        System.out.println(minDistance("passport", "ppsspqrt"));   // 2
    }
}
```

Tracing `"abdca" -> "cbda"`: the table reaches `d[5][4] = 2`, and backtracking through it recovers the actual two-edit sequence — **replace** the leading `a` with `c` (`"abdca" -> "cbdca"`), keep `b` and `d` as they are (they already match), then **delete** the `c` at position 4 (`"cbdca" -> "cbda"`), and the final `a` already matches. Two edits, matching the expected output of `2`.

## Complexity measures

Let **n** and **m** be the lengths of `str1` and `str2`.

### Time Complexity

`O(n * m)` — every cell of the table is computed once with constant work.

### Space Complexity

`O(n * m)` — the full table is kept in memory.
