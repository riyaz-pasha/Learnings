# 🧠 Detect Arbitrage in Currency Exchange — Full Explanation

---

## 1️⃣ What Is Arbitrage (Mathematically)?

You start with **1 unit** of some currency.

You go through a **cycle of exchanges** and end up back at the same currency.

If:

```
rate1 × rate2 × ... × ratek > 1
```

👉 You made profit → **arbitrage exists**

---

## 2️⃣ Why Graph Algorithms Struggle With This Directly

Graph algorithms like:

* Dijkstra
* Bellman-Ford

Work with:

* **addition**
* **comparisons**

But arbitrage is about **multiplication**.

So we need to convert multiplication → addition.

---

## 3️⃣ The Key Trick — Logarithms (This Is the Core Insight)

Recall:

```
log(a × b) = log(a) + log(b)
```

Now rewrite the arbitrage condition:

Original:

```
rate1 × rate2 × ... × ratek > 1
```

Take log on both sides:

```
log(rate1) + log(rate2) + ... + log(ratek) > 0
```

Still not perfect — graph algorithms detect **negative cycles**, not positive ones.

So we multiply everything by **−1**:

```
- log(rate1) - log(rate2) - ... - log(ratek) < 0
```

---

## 4️⃣ Graph Interpretation (This Is the Mental Shift)

Now we model this as a graph:

* Each **currency** → a node
* Each **exchange rate** `A → B` with rate `r` → edge:

  ```
  weight = -log(r)
  ```

Now the problem becomes:

> **Does this directed graph contain a negative-weight cycle?**

If yes → arbitrage exists.

---

## 5️⃣ Why This Works (Intuition)

* Profitable exchange cycle ⇒ product of rates > 1
* Product > 1 ⇒ sum of logs > 0
* Sum of `-log(rate)` < 0 ⇒ **negative cycle**

So:

```
Arbitrage ⇔ Negative cycle
```

---

## 6️⃣ Which Algorithm Detects Negative Cycles?

✅ **Bellman–Ford**

Why?

* Works with negative edges
* Can detect negative cycles
* Time complexity acceptable for given constraints

---

## 7️⃣ How Bellman–Ford Is Used Here

* We don’t care about shortest paths
* We only care if **any negative cycle exists**
* We can start Bellman–Ford from **any node**

  * Or add a super-source connected to all nodes with 0-weight edges

If after `V - 1` relaxations:

* Any edge can still be relaxed → **negative cycle exists**

---

## 8️⃣ Time & Space Complexity

* **Time:** `O(V × E)`

  * ~500 × 10,000 = OK
* **Space:** `O(V)`

---

## 9️⃣ Clean Interview-Grade Java Code

```java
class ArbitrageDetector {

    static class Edge {
        int from, to;
        double weight;

        Edge(int f, int t, double w) {
            from = f;
            to = t;
            weight = w;
        }
    }

    public boolean hasArbitrage(
            int n,
            List<String[]> exchanges, // [fromCurrency, toCurrency, rate]
            Map<String, Integer> currencyIndex
    ) {
        List<Edge> edges = new ArrayList<>();

        // Build edges with weight = -log(rate)
        for (String[] e : exchanges) {
            int from = currencyIndex.get(e[0]);
            int to = currencyIndex.get(e[1]);
            double rate = Double.parseDouble(e[2]);
            edges.add(new Edge(from, to, -Math.log(rate)));
        }

        // Super source trick
        double[] dist = new double[n];
        Arrays.fill(dist, 0.0);

        // Bellman-Ford
        for (int i = 0; i < n - 1; i++) {
            for (Edge e : edges) {
                if (dist[e.from] + e.weight < dist[e.to]) {
                    dist[e.to] = dist[e.from] + e.weight;
                }
            }
        }

        // Check for negative cycle
        for (Edge e : edges) {
            if (dist[e.from] + e.weight < dist[e.to]) {
                return true;
            }
        }

        return false;
    }
}
```

---

## 🎤 How to Explain This in the Interview (Memorize This)

> “Arbitrage corresponds to a cycle whose exchange-rate product is greater than one.
> By taking negative logarithms of rates, this becomes a negative cycle detection problem in a directed graph, which I solve using Bellman–Ford.”

If you say this confidently, you will **100% pass this round**.
