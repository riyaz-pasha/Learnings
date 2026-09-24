# DIY: Evaluate Division

## Problem statement

You're given `equations` (an array of variable pairs) and `values` (an array of real numbers), where `equations[i] = [Ai, Bi]` and `values[i]` represent the equation `Ai / Bi = values[i]`. Each `Ai`/`Bi` is a string variable name.

You're also given `queries`, where `queries[j] = [Cj, Dj]` asks for the answer to `Cj / Dj = ?`.

Return the answers to all queries. If an answer can't be determined, return `-1.0` for that query.

### Input

```java
equations = {{"a","b"}, {"b","c"}}
values = {2.0, 3.0}
queries = {{"a","c"}, {"b","a"}, {"a","e"}, {"a","a"}, {"x","x"}}
```

### Output

```java
{6.0, 0.5, -1.0, 1.0, -1.0}
```

## Coding exercise

Implement `evaluate(equations, values, queries)`, returning the answers array.

This is the exact same pattern as [Feature #3: Plot and Select Path](03-feature-3-plot-and-select-path.md) — there, Uber found whether a driver had a path to the user and its accumulated cost; here, dividing along a chain of variables is the same "does a path exist, and what's the accumulated value along it" problem, just with multiplication instead of addition. Build a weighted graph from the equations (each edge weighted by the ratio, plus its reciprocal in the opposite direction), then DFS from the query's source to its destination, multiplying edge weights along the way.

## Solution

```java
import java.util.*;

class Solution {
    public static double[] evaluate(String[][] equations, double[] values, String[][] queries) {
        Map<String, Map<String, Double>> graph = new HashMap<>();

        for (int i = 0; i < equations.length; i++) {
            String a = equations[i][0], b = equations[i][1];
            graph.computeIfAbsent(a, k -> new HashMap<>()).put(b, values[i]);
            graph.computeIfAbsent(b, k -> new HashMap<>()).put(a, 1.0 / values[i]);
        }

        double[] results = new double[queries.length];
        for (int i = 0; i < queries.length; i++) {
            String src = queries[i][0], dst = queries[i][1];

            if (!graph.containsKey(src) || !graph.containsKey(dst)) {
                results[i] = -1.0;
            } else if (src.equals(dst)) {
                results[i] = 1.0;
            } else {
                results[i] = dfs(graph, src, dst, new HashSet<>());
            }
        }
        return results;
    }

    private static double dfs(Map<String, Map<String, Double>> graph, String current,
                               String target, Set<String> visited) {
        if (current.equals(target)) return 1.0;
        visited.add(current);

        for (Map.Entry<String, Double> neighbor : graph.get(current).entrySet()) {
            if (!visited.contains(neighbor.getKey())) {
                double result = dfs(graph, neighbor.getKey(), target, visited);
                if (result != -1.0) {
                    return result * neighbor.getValue();
                }
            }
        }
        return -1.0;
    }

    public static void main(String[] args) {
        String[][] equations = {{"a", "b"}, {"b", "c"}};
        double[] values = {2.0, 3.0};
        String[][] queries = {{"a", "c"}, {"b", "a"}, {"a", "e"}, {"a", "a"}, {"x", "x"}};

        System.out.println(Arrays.toString(evaluate(equations, values, queries)));
        // [6.0, 0.5, -1.0, 1.0, -1.0]
    }
}
```

## Complexity measures

Let **n** be the number of equations (edges) and **q** the number of queries.

- **Time:** `O(n)` to build the graph, `O(q × n)` in the worst case for the queries — an overall `O(n + q × n)`.
- **Space:** `O(n)` for the graph plus `O(n)` for the visited set per query.
