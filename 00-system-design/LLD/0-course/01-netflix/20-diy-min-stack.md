# DIY: Min Stack

## Problem statement

Implement a `MinStack` class with a `min()` function that returns the stack's minimum value in `O(1)` — without popping it off.

### Input

A stream of numbers to push onto the stack.

### Output

The minimum value, returned in `O(1)` whenever `min()` is called.

## Coding exercise

Implement `push`, `pop`, and `min`.

This is the mirror image of [Feature #7: Browse Ratings](07-feature-7-browse-ratings.md) — there we tracked a running **maximum** with a second stack; here it's the same trick tracking a running **minimum** instead.

## Solution

```java
import java.util.Stack;

public class MinStack {
    private final Stack<Integer> mainStack;
    private final Stack<Integer> minStack;

    public MinStack() {
        mainStack = new Stack<>();
        minStack = new Stack<>();
    }

    public void push(int value) {
        mainStack.push(value);
        if (minStack.isEmpty() || value < minStack.peek()) {
            minStack.push(value);
        } else {
            minStack.push(minStack.peek());
        }
    }

    public Integer pop() {
        if (mainStack.isEmpty()) {
            return null;
        }
        minStack.pop();
        return mainStack.pop();
    }

    public int min() {
        return minStack.peek();
    }

    public static void main(String[] args) {
        MinStack stack = new MinStack();
        stack.push(5);
        stack.push(2);
        stack.push(7);
        stack.push(1);

        System.out.println(stack.min()); // 1
        stack.pop();                     // removes 1
        System.out.println(stack.min()); // 2
    }
}
```

## Complexity measures

- **`push` / `pop` / `min`:** `O(1)` each — every operation only touches the top of one or both stacks.
- **Space:** `O(n)` — `minStack` grows one-for-one with `mainStack` in the worst case (a strictly decreasing push sequence).
