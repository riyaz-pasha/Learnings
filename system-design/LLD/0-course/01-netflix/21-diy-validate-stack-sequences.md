# DIY: Validate Stack Sequences

## Problem statement

Given two sequences `pushed` and `popped` (distinct values), return `true` if and only if they could have resulted from some sequence of push/pop operations on an initially empty stack.

### Input

```java
pushed = {1, 2, 3, 4, 5}
popped = {1, 2, 3, 4, 5}
```

### Output

```java
true
```

One valid interleaving: `push(1), pop(1), push(2), pop(2), push(3), pop(3), push(4), pop(4), push(5), pop(5)`.

## Coding exercise

Implement `validateStackSequences(pushed, popped)`.

This is exactly [Feature #8: Verify User Session](08-feature-8-verify-user-session.md), without the Netflix framing: simulate greedily with a real stack, popping eagerly whenever the top matches the next expected value.

## Solution

```java
import java.util.Stack;

class Solution {
    public static boolean validateStackSequences(int[] pushed, int[] popped) {
        Stack<Integer> stack = new Stack<>();
        int popIndex = 0;

        for (int value : pushed) {
            stack.push(value);
            while (!stack.isEmpty() && stack.peek() == popped[popIndex]) {
                stack.pop();
                popIndex++;
            }
        }

        return stack.isEmpty();
    }

    public static void main(String[] args) {
        System.out.println(validateStackSequences(
                new int[]{1, 2, 3, 4, 5}, new int[]{4, 5, 3, 2, 1})); // true
        System.out.println(validateStackSequences(
                new int[]{1, 2, 3, 4, 5}, new int[]{4, 3, 5, 1, 2})); // false
    }
}
```

## Complexity measures

Let **n** be the length of the sequences.

- **Time:** `O(n)` — each value is pushed once and popped at most once.
- **Space:** `O(n)` — worst case, all `n` elements sit in the stack at once before any pop.
