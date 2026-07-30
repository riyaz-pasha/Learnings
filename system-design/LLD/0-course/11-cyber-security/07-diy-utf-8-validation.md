# DIY: UTF-8 Validation

## Problem statement

Given an integer array `data`, return whether it is a valid UTF-8 encoding.

A character in UTF-8 can be `1` to `4` bytes long, subject to the following rules:

- For a 1-byte character, the first bit is `0`, followed by its Unicode code.
- For an `n`-byte character, the first `n` bits are all `1`, the `(n + 1)`-th bit is `0`, followed by `n - 1` bytes, with the two most significant bits of each being `10`.

| Char. number range (hex) | UTF-8 octet sequence (binary) |
|---|---|
| `0000 0000` – `0000 007F` | `0xxxxxxx` |
| `0000 0080` – `0000 07FF` | `110xxxxx 10xxxxxx` |
| `0000 0800` – `0000 FFFF` | `1110xxxx 10xxxxxx 10xxxxxx` |
| `0001 0000` – `0010 FFFF` | `11110xxx 10xxxxxx 10xxxxxx 10xxxxxx` |

**Note:** the input is an array of integers, but only the least significant 8 bits of each integer matter — each entry represents just 1 byte of data.

### Input

```java
// Example 1
data = [198, 150, 9, 8]

// Example 2
data = [255, 129, 129, 129, 129, 129, 129, 129]
```

### Output

```java
// Example 1
true

// Example 2
false
```

## Coding exercise

Implement `validUtf8(data)`.

This is the exact same pattern as [Feature #1: Validate Packet Structure](01-feature-1-validate-packet-structure.md) — there, a proprietary encryption scheme borrowed UTF-8's self-describing byte-length header to verify a packet stream hadn't been tampered with; here it's the bare UTF-8 rule set with no story attached. The approach is identical: at the start of each character, count its header's leading `1` bits with a shifting mask to learn how many bytes it spans, then confirm every continuation byte in that span has its top two bits set to `10`.

## Solution

```java
class Solution {
    public static boolean validUtf8(int[] data) {
        int i = 0;
        int n = data.length;

        while (i < n) {
            int firstByte = data[i] & 0xFF;
            int numBytes = 0;
            int mask = 1 << 7;
            while ((firstByte & mask) != 0) {
                numBytes++;
                mask >>= 1;
            }

            if (numBytes == 0) {
                i += 1; // 1-byte character.
                continue;
            }
            if (numBytes == 1 || numBytes > 4) {
                return false;
            }
            if (i + numBytes > n) {
                return false;
            }
            for (int j = i + 1; j < i + numBytes; j++) {
                int b = data[j] & 0xFF;
                boolean topBitSet = (b & (1 << 7)) != 0;
                boolean secondBitClear = (b & (1 << 6)) == 0;
                if (!topBitSet || !secondBitClear) {
                    return false;
                }
            }
            i += numBytes;
        }
        return true;
    }

    public static void main(String[] args) {
        int[] data1 = {198, 150, 9, 8};
        int[] data2 = {255, 129, 129, 129, 129, 129, 129, 129};

        System.out.println(validUtf8(data1));
        // true
        System.out.println(validUtf8(data2));
        // false
    }
}
```

We scan the array left to right. Each time we're at the start of a character, we count its header's leading `1` bits — that count tells us the character's total byte length (`0` leading ones means a plain 1-byte character). A header with exactly one leading `1`, or more than four, can never be valid. Otherwise, we check that every following continuation byte in that character's span matches the `10xxxxxx` pattern, then jump ahead by the character's full length. If we walk off either check, the array isn't valid UTF-8; if we consume the whole array cleanly, it is.

## Complexity measures

Let **n** be the length of `data`.

- **Time:** `O(n)` — every byte is visited once, either as a header byte (at most 4 mask shifts) or a continuation byte (constant-time check).
- **Space:** `O(1)` — only a fixed number of integer counters are used.
