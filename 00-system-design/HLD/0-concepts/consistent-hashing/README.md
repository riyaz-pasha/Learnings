## 1. **What is Consistent Hashing?**

Consistent hashing is a technique for **distributing keys across a set of nodes (servers) in a way that minimizes reassignments** when nodes are added or removed.

### **Why is it needed?**

In a normal hash-based distribution:

```java
serverIndex = hash(key) % N
```

* If `N` changes (a node is added/removed), almost **all keys need remapping** because the modulus changes.
* This leads to high cache misses and data reshuffling.

**Consistent hashing** solves this by:

* Mapping **both keys and nodes** into the same hash space (like a circle/ring).
* Only a small portion of keys move when nodes change.

---

## 2. **Core Idea**

1. Hash each **node** to multiple positions on a ring (using *virtual nodes* for even distribution).
2. Hash each **key** to a point on the same ring.
3. The key belongs to the **first node clockwise** from its hash position.
4. If a node is removed, only keys that were assigned to that node move to the next node.

---

## 3. **Example Diagram (Conceptually)**

```
      0 -----------> 100 -----------> 200
     /                                      \
  900                                          300
     \                                      /
      800 <----------- 700 <----------- 600
```

* Nodes: `A`, `B`, `C` (with virtual nodes)
* Key hash = 750 → belongs to the next clockwise node from 750 (say Node C).

---

## 4. **Java Implementation**

We'll implement:

* A **hash function** (MD5 or MurmurHash).
* **TreeMap** to store node positions (since it’s sorted).
* **Virtual nodes** to balance load.

```java
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ConsistentHashing<T> {
    private final int numberOfReplicas;  // Virtual nodes per real node
    private final SortedMap<Long, T> circle = new TreeMap<>();

    public ConsistentHashing(int numberOfReplicas, Collection<T> nodes) {
        this.numberOfReplicas = numberOfReplicas;
        for (T node : nodes) {
            add(node);
        }
    }

    public void add(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            long hash = hash(node.toString() + i);
            circle.put(hash, node);
        }
    }

    public void remove(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            long hash = hash(node.toString() + i);
            circle.remove(hash);
        }
    }

    public T get(Object key) {
        if (circle.isEmpty()) {
            return null;
        }
        long hash = hash(key.toString());
        if (!circle.containsKey(hash)) {
            SortedMap<Long, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes());
            byte[] digest = md.digest();
            // Use first 8 bytes as a long
            long h = 0;
            for (int i = 0; i < 8; i++) {
                h <<= 8;
                h |= ((int) digest[i]) & 0xFF;
            }
            return h & 0x7FFFFFFFFFFFFFFFL; // make it positive
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
    }

    public static void main(String[] args) {
        List<String> nodes = Arrays.asList("NodeA", "NodeB", "NodeC");
        ConsistentHashing<String> ch = new ConsistentHashing<>(3, nodes);

        // Mapping keys
        for (int i = 0; i < 10; i++) {
            String key = "Key" + i;
            System.out.println(key + " -> " + ch.get(key));
        }

        System.out.println("\nRemoving NodeB...\n");
        ch.remove("NodeB");

        for (int i = 0; i < 10; i++) {
            String key = "Key" + i;
            System.out.println(key + " -> " + ch.get(key));
        }
    }
}
```

---

## 5. **Explanation**

* **`numberOfReplicas`**: Number of virtual nodes per real node.
* **`circle`**: TreeMap where keys = hash positions, values = node identifiers.
* **`add()`**: Creates `numberOfReplicas` virtual nodes for each real node.
* **`remove()`**: Removes all virtual nodes for a given node.
* **`get()`**: Finds the nearest clockwise node in the ring using `tailMap`.
* **`hash()`**: Converts the MD5 hash of a string into a positive long.

---

## 6. **Time Complexity**

| Operation        | Complexity   | Reason                                 |
| ---------------- | ------------ | -------------------------------------- |
| Add node         | O(R log(NR)) | R = replicas, NR = total virtual nodes |
| Remove node      | O(R log(NR)) | Remove R entries from TreeMap          |
| Get node for key | O(log(NR))   | `tailMap` lookup in TreeMap            |

---

## 7. **Space Complexity**

* **O(N × R)** where:

  * `N` = number of physical nodes
  * `R` = number of virtual nodes per physical node
* Additional space for TreeMap overhead.

---

## 8. **Advantages**

* Minimal key remapping on node changes.
* Good load balancing with virtual nodes.
* Works well for distributed systems (like cache, databases, sharding).

---

## 9. **When to Use**

* Distributed caches (e.g., Memcached, Redis clusters).
* Sharding large datasets.
* Peer-to-peer systems (Chord, Cassandra, etc.).

