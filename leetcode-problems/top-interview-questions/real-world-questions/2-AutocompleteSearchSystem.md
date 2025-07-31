### 🔁 **1. What if the number of suggestions needs to be dynamic (top-K instead of top-3)?**

**Answer:**

* Replace the fixed-size priority queue (`top3`) with a size-`K` heap.
* `updateTopK()` would maintain a min-heap of size `K` at each node.
* Either:

  * Pass `K` as a constructor parameter to the system.
  * Or maintain a global value `k` and always generate top suggestions by sorting or limiting based on it.

**Trade-off**: Memory usage increases with higher `K`.

---

### ⏳ **2. What is the time complexity to insert and query if we now want to return top-K instead of top-3?**

**Answer:**

Let `L = length of the term`, `K = number of suggestions`:

* **Insert**: `O(L * log K)` — because each node updates a min-heap of size `K`.
* **Query**: `O(P + K log K)` — traverse `P` characters, then sort/heapify top-K suggestions.

---

### 🔄 **3. How do you handle updates to term frequency from external sources? (e.g., logs)**

**Answer:**

* Maintain a `Map<String, Integer>` for frequencies.
* On frequency update:

  * Remove the old term from all relevant trie nodes’ heaps.
  * Re-insert the updated `SearchTerm` with new frequency into each node on the path.
* This ensures the `topK` heaps stay accurate.

---

### 🧹 **4. How would you handle deletion of a term?**

**Answer:**

* Delete the term from the frequency map.
* Traverse its path in the trie and remove it from each node’s `topK` heap.
* If a trie node has no children and empty heap, it can be garbage collected or explicitly removed.

---

### 🔤 **5. What about case sensitivity or punctuation?**

**Answer:**

* Normalize input: convert to lowercase and strip special characters before inserting or querying.
* Use a tokenizer or regex to sanitize inputs.

---

### 💾 **6. Can this system support millions of terms efficiently?**

**Answer:**
Yes, but with some optimizations:

* Use a **compressed trie** (Radix tree) to reduce memory overhead.
* Offload inactive parts of the trie to disk (e.g., via a key-value store like RocksDB).
* Cache recent prefixes and their suggestions in memory.

---

### 🧠 **7. How would you support fuzzy/autocorrect suggestions?**

**Answer:**
Use techniques like:

* **Edit Distance / Levenshtein Automata** on top of trie traversal to allow typos.
* **BK-Trees** for approximate match queries.
* At each trie node, you can allow “fuzzy” transitions and expand candidate nodes up to a given edit distance.

---

### 🌍 **8. How do you rank by recency or location-based popularity instead of just frequency?**

**Answer:**

* Store additional metadata per term (timestamp, location usage stats).
* Replace `SearchTerm` class with a more complex scoring model: `score = w1 * frequency + w2 * recency + w3 * geo relevance`.
* Use a custom comparator to rank suggestions dynamically based on the context.

---

### 🔧 **9. How would you modify this system for a mobile app with limited memory?**

**Answer:**

* Use a **client-server model**: send prefix to the backend, get suggestions.
* Or:

  * Store only the top N prefixes locally.
  * Compress trie using a **DAWG** (Directed Acyclic Word Graph).
  * Trim top-K to top-1 or top-2 for mobile memory constraints.

---

### 🧪 **10. How would you test this system?**

**Answer:**

* **Unit Tests** for:

  * Trie insertions.
  * Frequency updates.
  * Top-K suggestion correctness.
* **Integration Tests** for:

  * Stream input with various sequences and frequency updates.
* **Performance Tests** with:

  * Large datasets (simulate millions of terms).
  * High-frequency term updates.

---

### 🧊 Bonus: **How would you design this as a distributed system?**

**Answer:**

* **Sharding** based on prefix (e.g., a–f, g–l...).
* Each shard owns part of the trie and processes queries independently.
* Use **consistent hashing** if prefix distribution is uneven.
* Use a **coordinator service** to route prefix queries and aggregate results.

---

Let me know if you want to simulate interview dialogues with these follow-ups or need system design diagrams or test cases.
