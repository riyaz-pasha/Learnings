## 1. Why Use ZooKeeper with Snowflake?

Snowflake requires each machine to have a **unique Machine ID**.
If you hardcode it in configs, it’s prone to errors (two nodes accidentally using the same ID).

**ZooKeeper** can:

* **Assign unique Machine IDs dynamically** to new servers when they join.
* **Track active nodes** so IDs can be reused after a node goes down.
* **Avoid configuration mistakes** where multiple nodes think they’re the same.

---

## 2. Snowflake ID Recap

A typical Snowflake 64-bit ID format:

```
[41 bits: timestamp] [10 bits: machine ID] [12 bits: sequence per ms]
```

* **Timestamp** → milliseconds since a custom epoch.
* **Machine ID** → unique number (0–1023 if 10 bits).
* **Sequence** → counter for IDs generated in the same millisecond.

---

## 3. ZooKeeper + Snowflake Setup

### Step 1: ZooKeeper Node Structure

We create a parent ZNode for ID generator registration:

```
/id_generator
    /worker-0000001
    /worker-0000002
    ...
```

### Step 2: Assigning Machine IDs

* When a server starts:

  1. It connects to ZooKeeper.
  2. It **creates an *ephemeral sequential node*** under `/id_generator`:

     ```
     create -e -s /id_generator/worker-
     ```
  3. ZooKeeper returns something like:

     ```
     /id_generator/worker-0000004
     ```
  4. The **sequence number** at the end (`4` here) is the **Machine ID**.

     * If we’re using 10 bits for Machine ID, max = `1023` (fits fine).
* Since nodes are **ephemeral**, if the server dies or loses ZooKeeper connection, the node is deleted and the Machine ID can be reused by new nodes.

---

### Step 3: Generating IDs

* Each API server now has:

  * Its **Machine ID** from ZooKeeper.
  * Local **Snowflake generator**.
* ID generation steps:

  1. Get current timestamp in ms.
  2. Shift left 22 bits (10 for Machine ID + 12 for sequence).
  3. Add Machine ID (10 bits).
  4. Add Sequence number (12 bits).
  5. Base62 encode to get short URL.

---

### Step 4: Handling Clock Drift

* If system clock goes backwards:

  * Block ID generation until time catches up **OR**
  * Increment sequence number in a reserved “time rollback” range.
* NTP time sync across all servers is a must.

---

## 4. Example Flow

**Server startup:**

1. Server connects to ZooKeeper at `zk://zk1,zk2,zk3`.
2. Creates `/id_generator/worker-` ephemeral sequential node.
3. Receives `/id_generator/worker-0000007` → Machine ID = `7`.

**Paste creation:**

1. API server generates Snowflake ID using:

   ```
   timestamp = current_ms - custom_epoch
   id = (timestamp << 22) | (machineId << 12) | sequence
   ```
2. Encodes `id` in Base62 → returns as `paste_id`.

---

## 5. Challenges & Mitigations

| Challenge                 | Cause                                                 | Mitigation                                                       |
| ------------------------- | ----------------------------------------------------- | ---------------------------------------------------------------- |
| **ZooKeeper down**        | No machine ID assignment possible for new servers     | Use multiple ZK nodes, quorum config                             |
| **Machine ID exhaustion** | More servers than ID space (e.g., >1024 with 10 bits) | Increase bits for machine ID, reduce sequence bits               |
| **Clock skew**            | Servers out of sync                                   | NTP sync, fail-fast if drift detected                            |
| **ZooKeeper performance** | Many servers registering                              | Ephemeral sequential node creation is lightweight; caching helps |

---

## 6. Why This Works Well

* **No collisions** — Machine ID from ZooKeeper is guaranteed unique.
* **Dynamic scaling** — Adding/removing servers is automatic.
* **Stateless ID generation** — Once assigned, each server generates IDs locally without coordination.

