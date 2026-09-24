# DIY: Logger Rate Limiter

## Problem statement

Design a logger that receives a stream of `(timestamp, message)` pairs. A message should print only if it wasn't printed in the last 5 seconds.

### Input

```java
shouldPrintMessage(timestamp, message)
```

### Output

`true` or `false`.

## Coding exercise

Implement the `Logger` class with `shouldPrintMessage(timestamp, message)`.

This is exactly [Feature #4: Request Limiter](04-feature-4-request-limiter.md) — a HashMap from message to its last-printed timestamp, with a fixed window (5 seconds here instead of 5 days).

## Solution

```java
import java.util.HashMap;

class Logger {
    private final HashMap<String, Integer> lastPrinted;
    private static final int WINDOW = 5;

    public Logger() {
        lastPrinted = new HashMap<>();
    }

    public boolean shouldPrintMessage(int timestamp, String message) {
        if (!lastPrinted.containsKey(message) || timestamp - lastPrinted.get(message) >= WINDOW) {
            lastPrinted.put(message, timestamp);
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        Logger logger = new Logger();
        System.out.println(logger.shouldPrintMessage(1, "foo"));  // true
        System.out.println(logger.shouldPrintMessage(2, "foo"));  // false (only 1s later)
        System.out.println(logger.shouldPrintMessage(6, "foo"));  // true (5s have passed)
        System.out.println(logger.shouldPrintMessage(6, "bar"));  // true (different message)
    }
}
```

## Complexity measures

Let **r** be the number of distinct messages seen so far.

- **Time:** `O(1)` per call.
- **Space:** `O(r)`.
