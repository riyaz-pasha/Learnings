## 1\. Functional & Non-Functional Requirements

### P0 Core Features

  * **Real-time Co-editing**: Multiple users can edit a single document simultaneously. Changes are reflected in real-time for all collaborators.
  * **Document Management**: Create, open, save, and delete documents.
  * **User Authentication & Authorization**: Secure access to documents based on user roles (e.g., owner, editor, viewer).
  * **Document History/Versioning**: Track changes and revert to previous versions.

### P1 Features

  * **Offline Editing**: Users can edit a document while offline; changes sync upon reconnection.
  * **Commenting & Suggestions**: Users can add comments and suggest edits that can be accepted or rejected.
  * **Access Control & Sharing**: Share documents with specific users or groups with defined permissions.
  * **Presence & Cursors**: Show who else is in the document and their real-time cursor position.

### P2 Features

  * **Rich Text Formatting**: Basic formatting like bold, italics, underline, and headings.
  * **Notifications**: Notify users about document changes or comments.

### User Personas & Goals

  * **Knowledge Worker**: Needs to collaborate with colleagues on reports and presentations. Goal: Seamless, real-time co-editing.
  * **Student**: Works on group projects. Goal: Easy sharing and version control.
  * **Manager**: Needs to review and comment on documents. Goal: Quick feedback and clear history tracking.

### Technical & Business Complexities

  * **Real-time synchronization**: The core challenge is maintaining a consistent state across all clients with minimal latency. Operational Transformation (OT) or Conflict-free Replicated Data Types (CRDTs) are key here.
  * **Offline Handling**: Merging changes made offline introduces complexity in state reconciliation.
  * **Scalability**: Supporting millions of concurrent users editing documents requires a highly scalable architecture.
  * **Data Consistency**: Ensuring all users see the same, correct version of the document is critical.

### Non-Functional Requirements (NFRs)

  * **Performance**: Sub-100ms latency for real-time changes.
  * **Scalability**: Support 100M+ Monthly Active Users (MAUs), 10M+ Daily Active Users (DAUs).
  * **Reliability**: 99.99% uptime. The system must be resilient to failures.
  * **Consistency**: Strong consistency for document state, eventual consistency for secondary data (e.g., presence).
  * **Security**: End-to-end encryption, robust auth/auth mechanisms.
  * **Maintainability**: Easy to deploy, monitor, and update.

### Assumptions & Scope

  * We'll focus on the core P0 features, with a deep dive into real-time co-editing and offline handling, as they are the most complex.
  * The content of the document will be plain text for simplicity in the deep dive, but the principles extend to rich text.
  * We'll use a WebSocket-based approach for real-time communication.

-----

## 2\. Back-of-the-Envelope Estimation

  * **MAU**: 100M
  * **DAU**: 10M
  * **CCU (Concurrent Users)**: Assume 10% of DAU are concurrently active. $10M \\times 0.10 = 1M$ CCU.
  * **Document Size**: Average document size is small, maybe a few KB, but can grow to MBs. Let's assume average of 10KB.
  * **QPS (Queries per second)**: For edits, maybe 1 edit per user every 5 seconds on average. $1M \\text{ CCU} / 5 \\text{s/edit} = 200K$ QPS for edits. Total QPS could be much higher with reads.
  * **Data Storage**: A document's history can grow.
      * Let's say each edit is 50 bytes on average.
      * $10M \\text{ users} \\times 10 \\text{ docs/user} \\times 1000 \\text{ edits/doc} \\times 50 \\text{ bytes/edit} = 50 \\text{ TB}$ of data for edits.
      * The latest version of the document would be smaller.
  * **Cache Sizing**: To handle 1M CCU, we need to cache active documents.
      * Cache size = $1M \\text{ users} \\times 10 \\text{KB/doc} = 10 \\text{GB}$. We'll need more for metadata and history.
  * **Messaging Sizing**: 200K QPS means our messaging queue (e.g., Kafka) needs to handle at least 200,000 messages/sec.

-----

## 3\. Functional Requirement Deep Dives

### 3.1 Real-time Co-editing: Client → Server Flow

The core of real-time co-editing relies on a **WebSocket connection** and an algorithm like **Operational Transformation (OT)** or **Conflict-Free Replicated Data Types (CRDTs)**. We'll use **Operational Transformation (OT)** for this deep dive.

  * **Client-side**: Each client maintains a local copy of the document and a queue of unsent operations. When a user types, an **Operation (Op)** is generated. An Op is an object representing a change (e.g., `insert`, `delete`). It includes the position and content of the change.
  * **Server-side**: The server acts as the central authority. It receives Ops, transforms them, applies them to the document's state, and broadcasts the transformed Ops to other clients.

**Stepwise Flow:**

1.  **Client initiates editing**:

      * Client opens a document.
      * It sends an initial HTTP GET request to `/docs/{doc_id}` to fetch the latest document content.
      * It establishes a persistent **WebSocket connection** to `/ws/docs/{doc_id}`.
      * Authentication is handled via a JWT token in the WebSocket handshake.

2.  **User makes an edit**:

      * User types "hello" at the beginning of the document.
      * Client generates an Op: `op = {type: 'insert', pos: 0, str: 'hello', client_id: '...', version: N}`.
      * `version` is the local version number of the document on the client.
      * The client sends this `op` over the WebSocket.

3.  **Server receives and processes the Op**:

      * The WebSocket server receives the `op`.
      * It validates the request (auth, document ID).
      * It checks the `version` in the `op`. If the client's version is behind the server's, the server might send a merge-conflict or history-diff.
      * The server applies the Op to the current state of the document.
      * Crucially, the server assigns a new, authoritative version number (`N+1`) to the document.
      * The server transforms any pending Ops from other clients against this new Op to ensure they can be applied correctly. This is the core of OT.

4.  **Server broadcasts the Op**:

      * The server sends the **transformed** Op (`op'`) to all other connected clients.
      * The broadcast message includes the new document version: `{op: op', new_version: N+1}`.

5.  **Recipient Client processes the Op**:

      * The recipient client receives the transformed `op'`.
      * It applies the `op'` to its local document state.
      * It updates its local version number to `N+1`.
      * Any of the recipient's own pending local Ops must be transformed against this incoming `op'` before being sent to the server.

**Multi-device/Offline Handling:**

  * **Multi-device**: Each device connects with its own client ID. The server treats them as separate collaborators. \`\`
  * **Offline Handling**:
      * The client caches the last known state of the document and all pending Ops in a local database (e.g., IndexedDB on a browser, SQLite on mobile).
      * When the user is offline, they continue to edit. All changes are stored as Ops in the local queue.
      * Upon reconnection, the client sends all its stored Ops to the server in a batch.
      * The server processes these Ops, applying the same OT logic to merge them with the server's current state. The client's Ops are transformed against any changes that happened while it was offline.

### 3.2 Data Storage & Retrieval

  * **DB Choice**: We need a database that can handle high write throughput and provide low-latency reads. A **NoSQL document store** like MongoDB or a **key-value store** like Cassandra or DynamoDB is a good choice for storing document history. For metadata (user info, permissions), a relational DB like PostgreSQL is fine. For the deep dive, we'll use a **document store** like **MongoDB**. It's flexible for storing Ops and document states.

  * **DB Schema (MongoDB)**:

      * **`documents` collection**: Stores the latest document state.
        ```json
        {
          "_id": "doc_id_123",
          "title": "My Design Document",
          "content": "Latest document content...",
          "version": 156,
          "owner_id": "user_abc",
          "collaborators": [
            { "user_id": "user_xyz", "role": "editor" },
            { "user_id": "user_pqr", "role": "viewer" }
          ],
          "last_modified": ISODate(...)
        }
        ```
      * **`document_history` collection**: Stores the history of Ops. This is critical for versioning and offline reconciliation.
        ```json
        {
          "_id": ObjectId("..."),
          "doc_id": "doc_id_123",
          "version": 156,
          "op": {
            "type": "insert",
            "pos": 10,
            "str": "new text",
            "client_id": "user_xyz_device_A"
          },
          "timestamp": ISODate(...)
        }
        ```
          * **Indexes**: `db.document_history.createIndex({ doc_id: 1, version: 1 })` for efficient history lookups.

  * **Example Read/Write Queries (Pseudocode)**:

      * **Write (Apply Op)**:
        ```javascript
        // 1. Get current document state
        const doc = db.documents.findOne({ _id: docId });
        const currentVersion = doc.version;

        // 2. Apply OT transformation to incoming op (pseudo-function)
        const transformedOp = transformOp(incomingOp, doc.content);

        // 3. Update the document and increment version in a single atomic transaction
        const result = db.documents.updateOne(
          { _id: docId, version: currentVersion }, // Optimistic locking
          { $set: { content: applyOp(doc.content, transformedOp) },
            $inc: { version: 1 },
            $set: { last_modified: new Date() }
          }
        );

        // 4. If successful, insert the new op into history
        if (result.modifiedCount > 0) {
          db.document_history.insertOne({
            doc_id: docId,
            version: currentVersion + 1,
            op: transformedOp,
            timestamp: new Date()
          });
        }
        ```
      * **Read (Get Document)**:
        ```javascript
        // Latest document content
        const doc = db.documents.findOne({ _id: docId });

        // Get history since a specific version for reconciliation
        const historyOps = db.document_history.find({
          doc_id: docId,
          version: { $gt: clientVersion }
        }).sort({ version: 1 });
        ```

  * **Caching (Redis)**:

      * We'll use a Redis cache to store the latest state of actively edited documents to reduce DB load.
      * **Cache Key**: `doc:{doc_id}:latest`
      * **Value**: JSON string of the document object (`{ content: "...", version: 156 }`).
      * **TTL**: A long TTL (e.g., 24 hours) with a "lazy expiration" policy. When a document is accessed, its TTL is reset.
      * **Eviction Policy**: LRU (Least Recently Used) is a good choice for active documents.
      * **Cache Command (Pseudocode)**:
        ```shell
        # Set latest document state
        SET doc:{doc_id}:latest '{"content": "...", "version": 156}' EX 86400

        # Get document state
        GET doc:{doc_id}:latest
        ```

  * **Sharding**: Shard the `documents` and `document_history` collections by `doc_id`. This ensures all data for a single document resides on the same shard, simplifying queries and maintaining atomicity. Cross-shard queries for user-specific data (e.g., "all docs for user X") would be handled by a fan-out query or a dedicated user-document mapping table.

-----

### 3.3 Server → Recipient Flow

  * **Primary Push Mechanism**: **WebSockets**. This is essential for real-time collaboration. The server pushes transformed Ops directly to all clients connected to the document's WebSocket channel.
  * **Presence**: Presence is a crucial part of the user experience.
      * When a user connects, their client sends a `presence_update` message over the WebSocket.
      * The server maintains a Redis set for each document to track active users.
      * `SADD doc:{doc_id}:presence {user_id}:{cursor_pos}`
      * The server broadcasts `presence_update` messages to all clients.
      * When a user disconnects, the server removes them from the set and broadcasts a `presence_leave` message.
  * **Offline/Multi-device**: The server doesn't push real-time changes to offline clients. Instead, when an offline client reconnects, it sends its current document version. The server then sends all the Ops that occurred since that version, allowing the client to catch up.

-----

### 3.4 Messaging & Eventing

  * **`[MESSAGING_SYSTEM]`**: We'll use **Kafka** for its high throughput, durability, and scalability.

  * **Kafka Topic**: `document_ops`.

  * **Partition Key**: `doc_id`. This is critical. By partitioning by `doc_id`, all ops for a single document go to the same partition, ensuring ordered processing and preventing race conditions.

  * **Schema (JSON)**:

    ```json
    {
      "docId": "doc_id_123",
      "opId": "uuid_from_server",
      "op": { ... },
      "version": 156,
      "timestamp": "2025-08-18T21:11:03Z",
      "clientId": "user_xyz_device_A"
    }
    ```

  * **Event Flow**:

    1.  **API Gateway**: A client sends an Op via WebSocket to the API Gateway.
    2.  **WebSocket Server**: Receives the Op.
    3.  **DB**: The server applies the Op and updates the DB (as described in 3.2).
    4.  **Messaging**: After a successful DB write, the server publishes the transformed Op to the `document_ops` Kafka topic.
    5.  **Worker (Broadcast Service)**: A dedicated service consumes from the `document_ops` topic.
    6.  **Cache**: The worker can update a shared cache (e.g., Redis) with the latest document state, but the primary job is to broadcast.
    7.  **Clients**: The worker pushes the Op to all connected clients via their WebSockets.

  * **Async vs. Sync**: The DB update is synchronous, ensuring the change is persisted before broadcasting. The broadcast to Kafka and subsequent worker processing are asynchronous, decoupling the DB write from the real-time push.

  * **Idempotency**: Ops sent by clients should have a unique ID (`opId` or a combination of `clientId` and a sequence number) to prevent re-processing in case of retries. The server can use a `seen_set` in Redis for each user to track processed Ops.

-----

### 3.5 Edge Cases & Failure Handling

  * **Offline Users**: As described in 3.1, the client caches ops and syncs upon reconnection. The server will send a diff of all ops since the client's last known version.
  * **Partial Failures (e.g., DB is down)**:
      * The write to the DB fails. The WebSocket server cannot increment the version.
      * The server should not broadcast the Op. It sends an error message to the client, which can then retry the Op.
  * **Cache Misses**:
      * A read request for a document hits the cache but finds no data.
      * The application falls back to the DB to fetch the document.
      * After fetching, it populates the cache for future requests.
  * **Reconciliation (for offline users)**:
      * Client reconnects and sends its current version number `V_client`.
      * Server fetches all ops from `document_history` where `version > V_client`.
      * The server sends this list of ops to the client.
      * The client applies these ops to its local document, performing OT transformations against any of its own pending ops. This merges the server's changes with the local offline edits.

-----

### 3.6 Pagination & Cursor Handling

  * This is not a primary requirement for the real-time editing flow but is critical for fetching document history.
  * **Cursor-building Algorithm**: A cursor can be an opaque, Base64-encoded string containing the `timestamp` and `_id` of the last item fetched.
  * **Example Cursor**: `{"ts": "2025-08-18T21:10:00Z", "id": "ObjectId('...')"}` encoded as Base64.
  * **Step-by-step Read Logic**:
    1.  Client requests history for `doc_id` with an optional `cursor`.
    2.  **Server receives request**:
    3.  If `cursor` is present, it's decoded to get `ts` and `id`.
    4.  **DB Query**: `db.document_history.find({ doc_id: "...", timestamp: { $gte: "ts" }, _id: { $gt: "id" } }).sort({ timestamp: 1 }).limit(50)`
    5.  The server fetches the next page of history ops.
    6.  It creates a new cursor from the last op in the result set and sends the ops and the new cursor back to the client.

-----

### 3.7 Performance & Scalability Considerations

  * **Bottlenecks**: The primary bottleneck is the **WebSocket server** for handling a large number of concurrent connections and the **database** for high write throughput.
  * **Autoscaling**:
      * **WebSocket Servers**: Monitor the number of open connections and CPU usage. Use an autoscaling group to spin up new instances during peak hours.
      * **DB**: Sharding by `doc_id` helps distribute the load. Replicas can be used for read-heavy operations.
      * **Kafka**: Add more brokers and partitions to handle increased throughput.
  * **Multi-AZ/Multi-Region**:
      * Deploy services across multiple availability zones for high availability.
      * For multi-region, use a global database solution (e.g., CockroachDB, multi-region DynamoDB) or set up active-passive replication.
  * **Cache Hit Ratio**: Aim for a high cache hit ratio (\>90%) for actively edited documents to reduce DB load and latency.

-----

### 3.8 Instrumentation & Monitoring

  * **Metrics (Prometheus/Grafana)**:
      * **RED**: Request/Rate, Errors, Duration. Per endpoint (e.g., `/ws/docs/{doc_id}`).
      * **WebSocket-specific**: Number of open connections, messages sent/received per second, connection latency.
      * **Cache**: Cache hit/miss ratio, cache size.
      * **Messaging**: Kafka consumer lag (critical), producer latency.
      * **DB**: Read/write latency, connection pool usage.
  * **Logging & Tracing**: Use a distributed tracing system (e.g., Jaeger) to trace a single Op from client to server, DB, Kafka, and back to other clients. Log key events like connection open/close, Op receipt, DB write success/failure, and broadcast.

-----

### 3.9 Summary Table

| Feature | Client→Server | Server→DB/Cache | Server→Recipient | Messaging Events | Notes / Multi-device / Offline Handling |
|:---:|:---:|:---:|:---:|:---:|:---:|
| **Real-time Editing** | WebSocket with an Op (insert, delete) including a client-side version. | Server validates, applies OT, and atomically updates `documents` collection and inserts into `document_history`. Cache updated. | Pushed via WebSocket. | Producer publishes transformed Op to `document_ops` topic. Consumer broadcasts to clients. | **Offline**: Client caches Ops locally, syncs upon reconnection. Server sends a diff based on client's version. **Multi-device**: Each device is a separate WebSocket connection. |
| **Document History** | HTTP GET for `/docs/{doc_id}/history?cursor=...`. | DB query on `document_history` collection, sorted by version. | N/A | N/A | **Pagination**: Uses a cursor based on timestamp and ID for efficient, stateless pagination of history. |
| **Presence** | WebSocket message (`presence_update`) with cursor position. | Server updates a Redis `SET` key `doc:{doc_id}:presence`. | Broadcasted via WebSocket to all collaborators. | N/A | Simple, real-time presence tracking with minimal DB/messaging overhead. |
| **Auth/Auth** | JWT in WebSocket handshake. | Middleware validates JWT. DB lookup for user permissions in `collaborators` field of `documents` collection. | N/A | N/A | Handled at the API Gateway and WebSocket server level. |
