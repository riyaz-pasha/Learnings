# DIY: Read N Characters Given Read4 II - Call Multiple Times

## Problem statement

You can only read a file through an API method `read4(buf)`, which reads the next 4 consecutive characters from the file (or fewer, if the file runs out) into the array `buf`, and returns how many characters it read. `read4` keeps its own internal file pointer across calls.

Implement `read(buffer, n)`, which reads `n` characters from the file (via `read4`) into `buffer` and returns the number of characters actually read. The key difficulty: `read` may be called **multiple times**, and a single `read4` call can return characters that spill over past what the current `read` call needs — those leftover characters must be buffered and handed to the *next* `read` call rather than discarded.

### Input

```java
file = "abcdefghijklmno"
// calls to read(buffer, n), in order:
read(buffer, 2)
read(buffer, 3)
read(buffer, 7)
read(buffer, 3)
```

### Output

```java
2  // buffer = "ab"
3  // buffer = "cde"
7  // buffer = "fghijkl"
3  // buffer = "mno"
```

## Coding exercise

Implement the `read(buffer, n)` method (backed by a `read4` method that reads from the file).

This is the exact same pattern as [Feature #11: Ad Serving](11-feature-11-ad-serving.md) — there, Amazon needed to serve `n` ads at a time from a legacy `read4()`-style API; here it's the bare pattern with no story attached. The trick is keeping a small internal 4-character buffer plus a read/write cursor into it as instance state that survives between calls to `read`. Each call to `read` first drains whatever is left in that internal buffer, and only calls `read4` again once it's empty, so characters `read4` over-fetched in a previous call aren't lost.

## Solution

```java
class Solution {
    // Simulates the file-backed read4 API described in the problem.
    static class Reader4 {
        private final String file;
        private int filePos = 0;
        Reader4(String file) { this.file = file; }

        int read4(char[] buf4) {
            int count = 0;
            while (filePos < file.length() && count < 4) {
                buf4[count++] = file.charAt(filePos++);
            }
            return count;
        }
    }

    private final Reader4 reader;
    // Characters read4 already pulled from the file but that a previous
    // read(n) call didn't have room to consume yet.
    private final char[] buf4 = new char[4];
    private int buf4Len = 0; // how many valid characters are in buf4
    private int buf4Pos = 0; // how many of them have been consumed so far

    Solution(Reader4 reader) {
        this.reader = reader;
    }

    public int read(char[] buffer, int n) {
        int total = 0;
        while (total < n) {
            if (buf4Pos == buf4Len) {
                // Internal buffer is empty — pull the next 4 characters.
                buf4Len = reader.read4(buf4);
                buf4Pos = 0;
                if (buf4Len == 0) {
                    break; // end of file
                }
            }
            while (total < n && buf4Pos < buf4Len) {
                buffer[total++] = buf4[buf4Pos++];
            }
        }
        return total;
    }

    public static void main(String[] args) {
        String file = "abcdefghijklmno";
        Solution solution = new Solution(new Reader4(file));
        char[] buffer = new char[50];

        int r1 = solution.read(buffer, 2);
        System.out.println(r1 + " " + new String(buffer, 0, r1)); // 2 ab

        int r2 = solution.read(buffer, 3);
        System.out.println(r2 + " " + new String(buffer, 0, r2)); // 3 cde

        int r3 = solution.read(buffer, 7);
        System.out.println(r3 + " " + new String(buffer, 0, r3)); // 7 fghijkl

        int r4 = solution.read(buffer, 3);
        System.out.println(r4 + " " + new String(buffer, 0, r4)); // 3 mno
    }
}
```

## Complexity measures

Let **n** be the number of characters requested in a single `read` call.

- **Time:** `O(n)` per call to `read` — each character is copied out of the internal buffer at most once, and `read4` is called at most `n/4 + 1` times.
- **Space:** `O(1)` — the internal buffer is a fixed 4-character array regardless of how many times `read` is called.
