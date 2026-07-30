# DIY: Open Lock

## Problem statement

You have an old lock in front of you with four circular wheels. Each wheel has ten slots: `'0'` through `'9'`. The wheels can rotate freely and wrap around — turning `'9'` gives `'0'`, and turning `'0'` gives `'9'`. Each move consists of turning one wheel one slot.

Initially, the lock starts at `"0000"`. You are given an array of dead ends: if the lock ever displays one of these codes, the wheels stop turning and it can never be opened.

Given a `target` representing the code that unlocks it, return the minimum number of turns required to reach `target`, or `-1` if it's impossible.

### Input

```java
// deadends = {"2110","0202","1222","2221","1010"}
// target = "2010"
```

### Output

```java
// 3 — "0000" -> "1000" -> "2000" -> "2010"
```

## Coding exercise

Implement the `openLock(deadends, target)` function, where `deadends` is the array of forbidden strings and `target` is the string to reach. The function returns a single integer representing the minimum total number of turns required.

This is exactly [Feature #3: Power Up the Station](03-feature-3-power-up-the-station.md) — the same BFS over 4-digit states with wraparound, just renamed from base-station dials to a combination lock.

## Solution

```java
import java.util.*;

class Solution {
    public static int openLock(String[] deadendsArr, String target) {
        Set<String> deadends = new HashSet<>(Arrays.asList(deadendsArr));
        String start = "0000";
        if (deadends.contains(start)) {
            return -1;
        }
        if (start.equals(target)) {
            return 0;
        }

        Set<String> visited = new HashSet<>();
        visited.add(start);
        Queue<String> queue = new LinkedList<>();
        queue.add(start);
        int turns = 0;

        while (!queue.isEmpty()) {
            turns++;
            int levelSize = queue.size();
            for (int s = 0; s < levelSize; s++) {
                String cur = queue.poll();
                for (String next : neighbors(cur)) {
                    if (visited.contains(next) || deadends.contains(next)) {
                        continue;
                    }
                    if (next.equals(target)) {
                        return turns;
                    }
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return -1;
    }

    private static List<String> neighbors(String state) {
        List<String> result = new ArrayList<>();
        char[] chars = state.toCharArray();
        for (int i = 0; i < 4; i++) {
            char original = chars[i];
            chars[i] = original == '9' ? '0' : (char) (original + 1);
            result.add(new String(chars));
            chars[i] = original == '0' ? '9' : (char) (original - 1);
            result.add(new String(chars));
            chars[i] = original;
        }
        return result;
    }

    public static void main(String[] args) {
        String[] deadends = {"2110", "0202", "1222", "2221", "1010"};
        System.out.println(openLock(deadends, "2010")); // 3
    }
}
```

## Solution walkthrough

BFS explores every state reachable from `"0000"` one turn at a time, skipping dead ends and already-visited states. Since BFS explores in increasing order of distance, the first time it reaches `target`, the current turn count is guaranteed minimal. Live run confirms the path `"0000" -> "1000" -> "2000" -> "2010"` reaches the target in exactly `3` turns.

## Complexity measures

Let **n** be the number of wheels (4), **a** be the digits per wheel (10), and **d** be the number of dead ends.

### Time Complexity

`O(n^2 * a^n + d)` — up to `a^n` states are explored, each generating `2n` neighbors in `O(n)` time to build, plus `O(d)` to construct the dead-end set.

### Space Complexity

`O(a^n + d)` — the visited set can grow to hold every reachable state, alongside the dead-end set.
