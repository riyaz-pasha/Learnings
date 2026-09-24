# DIY: Maximum Frequency Stack

## Problem statement

Design a stack-like structure where `pop()` removes and returns the **most frequently pushed** element (ties broken by most recently pushed). Implement:

- `FreqStack()` — construct an empty frequency stack.
- `push(data)` — push an integer onto the stack.
- `pop()` — remove and return the most frequent element (return `-1` if the stack is empty).

### Input

```java
push(5)
```

### Output

```java
pop() ==> 5
```

## Coding exercise

Implement the `FreqStack` class.

Same design as [Feature #12: Maintain Continue Watching Bar](12-feature-12-maintain-continue-watching-bar.md): a `frequency` map, a `group` map bucketing elements by frequency (each bucket a stack, so ties resolve to "most recent"), and a `maxFrequency` counter so `pop` never has to search.

## Solution

```java
import java.util.*;

class FreqStack {
    private final Map<Integer, Integer> frequency;
    private final Map<Integer, Stack<Integer>> group;
    private int maxFrequency;

    public FreqStack() {
        frequency = new HashMap<>();
        group = new HashMap<>();
        maxFrequency = 0;
    }

    public void push(int data) {
        int newFreq = frequency.getOrDefault(data, 0) + 1;
        frequency.put(data, newFreq);

        if (newFreq > maxFrequency) {
            maxFrequency = newFreq;
        }

        group.computeIfAbsent(newFreq, f -> new Stack<>()).push(data);
    }

    public int pop() {
        if (maxFrequency == 0) {
            return -1;
        }

        Stack<Integer> topBucket = group.get(maxFrequency);
        int data = topBucket.pop();

        frequency.put(data, frequency.get(data) - 1);
        if (topBucket.isEmpty()) {
            maxFrequency--;
        }

        return data;
    }

    public static void main(String[] args) {
        FreqStack stack = new FreqStack();
        stack.push(5);
        stack.push(7);
        stack.push(5);
        stack.push(7);
        stack.push(4);
        stack.push(5); // 5 now pushed 3 times

        System.out.println(stack.pop()); // 5 (freq 3, highest)
        System.out.println(stack.pop()); // 7 (freq 2, tie with 5 broken by recency)
        System.out.println(stack.pop()); // 5 (freq 2, the other tied element, now on top)
    }
}
```

## Complexity measures

Let **n** be the number of elements pushed so far.

- **`push` / `pop`:** `O(1)` each — map lookups plus a stack push/pop.
- **Space:** `O(n)`.
