# **High-Level System Design: Scalable Chat Application**

## **0. Executive Summary**

**Problem:** Build a chat system supporting 1:1 and group messaging, real-time delivery, multiple devices per user, offline storage, presence tracking, and guaranteed message delivery.

**Recommended Architecture:**

* **Stateless chat servers** handling WebSocket connections.
* **Redis** for device registry, presence, and caching.
* **Kafka** for guaranteed message delivery and fan-out.
* **Relational/NoSQL DB** for message persistence and user/chat data.

**Rationale:**

* Stateless servers + Pub/Sub allow **horizontal scaling**.
* Redis ensures **real-time presence tracking and device management**.
* Kafka guarantees **durable, ordered delivery** for millions of concurrent users.

---

## **1. Functional & Non-Functional Requirements**

| Feature                  | Priority | Technical Complexity                                      | Business Complexity          |
| ------------------------ | -------- | --------------------------------------------------------- | ---------------------------- |
| 1:1 messaging            | P0       | WebSocket handling, DB persistence, multi-device delivery | Critical UX                  |
| Group messaging          | P0       | Fan-out, large groups, efficient delivery                 | Scaling to millions          |
| Offline message delivery | P0       | Detect offline, queue messages, replay on reconnect       | Guaranteed delivery          |
| Multi-device support     | P0       | Device registry, per-device delivery, read receipts       | Consistent UX across devices |
| Online/offline presence  | P0       | Heartbeat, Redis aggregation, multi-server                | Real-time UX                 |
| Last seen                | P1       | Aggregated from device disconnects                        | User expectation             |
| Read receipts            | P1       | Per-device and per-user aggregation                       | UX transparency              |
| Typing indicators        | P2       | WebSocket events, ephemeral                               | Nice-to-have                 |
| Push notifications       | P2       | Offline devices, external push services                   | Optional for engagement      |

**SLOs/SLAs:**

* Message latency ≤ 200ms (real-time delivery)
* Availability ≥ 99.99%
* Maximum message loss = 0 (Kafka ensures durability)

**Tunable Parameters:**

* `heartbeat_interval = 30s`
* `heartbeat_timeout = 90s`
* `offline_message_retention = 30 days`
* `max_group_size = 1000` (configurable)

**User Personas:**

* End user (mobile/web) → send/receive messages, see presence
* Chat service admin → monitor servers, message throughput, failures

---

## **2. Back-of-the-Envelope Estimation**

**Scenario:**

* MAU: 10M users
* DAU: 2M users
* Peak concurrent users (CCU): 500K
* Avg messages per user per day: 100

**Calculations:**

* **Messages/day:** 2M × 100 = 200M messages/day
* **Messages/sec (peak):** 200M / 24\*3600 ≈ 2315 msg/sec avg, peak \~10K msg/sec
* **Redis memory estimate:** 1M active devices × 200 bytes = \~200MB
* **Kafka throughput:** \~10K msg/sec × 1KB/msg ≈ 10 MB/sec
* **DB storage:** 200M × 1 KB ≈ 200 GB/month

**Cloud Cost Ballpark:**

* Kafka cluster: \$500–1000/month
* Redis cluster: \$100–300/month
* DB storage + backups: \~\$500/month
* Load balancer + stateless servers: \~\$200–500/month

---

## **3. API Design**

**WebSocket Messages:**

**Connect:**

```json
{ "type": "connect", "userId": "123", "deviceId": "web-1" }
```

**Send Message:**

```json
{
  "type": "message",
  "chatId": "abc123",
  "senderId": "123",
  "content": "Hello world"
}
```

**Receive Message:**

```json
{
  "type": "message",
  "chatId": "abc123",
  "senderId": "456",
  "content": "Hi!"
}
```

**Presence Update:**

```json
{ "type": "presence", "userId": "456", "status": "online", "lastSeen": null }
```

**Rate Limiting:**

* Max 50 msg/sec per user per device
* Reject or queue messages beyond limit

**Pagination (message history):**

* Cursor-based (opaque base64 JSON):

```json
{ "chatId": "abc123", "lastMessageId": 4567 }
```

---

## **4. High-Level Architecture**

**ASCII Diagram:**

```
       +-----------------+
       |   Load Balancer |
       +--------+--------+
                |
        +-------+--------+
        |  Chat Servers  |  <-- stateless, WS connections
        +---+-------+---+
            |       |
            |       +--> Redis (device registry, presence)
            |
            +--> Kafka (message delivery, fan-out)
            |
            +--> DB (messages, chats, users)
```

**Components & Protocols:**

* **Chat Server:** WebSocket, gRPC internal API
* **Redis:** Pub/Sub, key-value store
* **Kafka:** durable messaging queue for offline/retry
* **DB:** SQL/NoSQL, persistent storage

**Data Flows:**

**Send Message (write path)**

1. Client → Chat Server (WS)
2. Persist message in DB
3. Publish to Kafka topic `chat.recipient.{recipientId}`
4. Recipient servers consume → fetch recipient devices → push via WS

**Read Message (read path)**

* Client requests via REST/WebSocket → DB / Redis for history → paginate

**Presence Updates:**

* Connect / disconnect → update Redis → broadcast to friends or active chat sessions

---

## **5. Functional Requirement Deep Dives**

### **Send Message (1:1 / Group)**

**Components:** Chat Server, Redis, Kafka, DB

**Step-by-Step Flow:**

1. Client sends message via WS.
2. Chat Server validates → persists in DB.
3. Fetch recipients from DB/Cache.
4. Publish to Kafka topics per recipient.
5. Each recipient server listens → fetch recipient devices from Redis → push WS.
6. Mark delivered/read per device in DB.

**Edge Cases:**

* Offline users → message queued
* Multi-device → deliver to all devices
* Network failure → retry via Kafka + DLQ

**Sample Kafka event:**

```json
{
  "messageId": 1234,
  "chatId": "abc123",
  "senderId": 123,
  "recipientId": 456,
  "timestamp": 1692361234
}
```

**Redis Pub/Sub for live delivery:**

```redis
PUBLISH chat.toDevice.deviceId '{"messageId":1234,"content":"Hello"}'
```

---

### **Presence / Online-Offline**

* **Connect:** HSET `user:{userId}:devices` deviceId → server/socket info
* **Disconnect / heartbeat timeout:** HDEL, update `lastSeen`
* **Aggregate per user:** online if any device online

---

### **Offline Message Handling**

* Detect offline → store messages in DB or Kafka
* On reconnect → deliver queued messages
* Multi-device → send to all devices

---

### **Multi-Device**

* Device registry in Redis per user
* Each message sent to **all active devices**
* Read/delivery flags tracked per device
* Heartbeats + TTL for abrupt disconnects

---

### **Group Chats**

* Recipient list = all members excluding sender
* Same message delivery flow → push to all devices
* Large groups → fan-out via Kafka topic shards

---

## **6. Database & Storage Design**

### **Messages Table**

| Column      | Type      | Notes         |
| ----------- | --------- | ------------- |
| message\_id | BIGINT    | PK            |
| chat\_id    | BIGINT    | FK            |
| sender\_id  | BIGINT    | FK            |
| content     | TEXT      |               |
| created\_at | TIMESTAMP |               |
| delivered   | BOOLEAN   | default false |

### **Message Recipients Table**

| Column        | Type    | Notes                 |
| ------------- | ------- | --------------------- |
| message\_id   | BIGINT  | PK part               |
| recipient\_id | BIGINT  | PK part               |
| device\_id    | VARCHAR | optional multi-device |
| delivered     | BOOLEAN | per device            |
| read          | BOOLEAN | per device            |

### **Chats Table**

| Column      | Type      | Notes           |
| ----------- | --------- | --------------- |
| chat\_id    | BIGINT    | PK              |
| type        | ENUM      | 1:1 / group     |
| members     | JSON/BLOB | list of userIds |
| created\_at | TIMESTAMP |                 |

---

## **7. Caching Strategy**

* Redis for:

  * Device registry: `user:{userId}:devices`
  * Presence: `user:{userId}:status, lastSeen`
  * Chat metadata / group members for quick fetch
* Eviction: LRU or TTL per device (heartbeat timeout)
* Cache-aside for chat messages if needed

---

## **8. Messaging & Eventing**

* Kafka topics per recipient or per group shard
* Consumer group pattern → fan-out to all connected servers
* Async operations for offline devices
* Retry + DLQ + idempotency (messageId as key)

---

## **9. Scalability, HA & Operations**

* Chat servers **stateless**, scale horizontally behind **load balancer**
* Multi-AZ / multi-region Redis and Kafka clusters
* Message persistence in DB with backups / PITR
* Monitoring:

  * WS connections, QPS, Kafka lag, Redis hit ratio
  * Alerts for high undelivered messages, presence anomalies

---

## **10. Security & Compliance**

* Auth via OAuth2/OIDC, mTLS for internal services
* End-to-end encryption (optional: sender encrypts content, only recipient devices decrypt)
* ACLs: user can only read messages in chats they are member of
* GDPR/HIPAA: deletion, export, consent
* Rate-limiting, input sanitization, anti-abuse

---

## **11. Cost, Trade-offs & Future Roadmap**

* **Cost drivers:** Kafka storage, DB storage, Redis cluster, WS connections
* **Trade-offs:**

  * Real-time presence vs scalability (partial updates)
  * Memory vs latency for device registry
  * Push vs pull for offline users
* **Future features:** typing indicators, message reactions, end-to-end media sharing

---

Ah! Got it — you want a **step-by-step narrative flow** for each functional requirement, explaining **how requests go from client → server → DB/cache → other clients**, in **plain words**. I’ll break it down per requirement:

---

# **1. User Connection / Presence (Online/Offline)**

**Flow:**

1. **Client opens app** → establishes a WebSocket connection to a chat server.
2. Chat server **registers the device** in Redis (`user:{userId}:devices`) with server ID, socket ID, and status = online.
3. Chat server **publishes presence event** to the user’s friends or subscribers (via Redis Pub/Sub).
4. **Friends’ clients** receive the presence event → update UI (online/offline).
5. **Disconnect / heartbeat timeout:**

   * If app closes or heartbeat not received, server deletes device entry in Redis, updates last seen timestamp, and publishes offline event.
6. **Aggregated status:** Any device online → user is online; all devices offline → user is offline.

---

# **2. 1:1 Messaging**

**Flow:**

1. User types a message → **sends it over WebSocket** to the chat server.
2. Server **validates message** (sender is part of chat, message not empty, rate limit).
3. Server **persists message** in DB (`messages` table) and inserts a row in `message_recipients` for the recipient.
4. Server **checks Redis** for recipient’s online devices.
5. **If online:** pushes message to all recipient devices via WebSocket.
6. **If offline:** message remains in DB (or optionally pushed to Kafka offline queue) for later delivery.
7. Recipient devices mark message as delivered/read → update DB and optionally notify sender.

---

# **3. Group Messaging**

**Flow (very similar to 1:1):**

1. Client sends message to server via WebSocket.
2. Server validates → persists in DB (`messages`) and inserts rows in `message_recipients` for **all group members except sender**.
3. Server fetches each member’s **active devices** from Redis.
4. For each device:

   * **Online:** push via WebSocket
   * **Offline:** store in DB/Kafka for later delivery
5. Sender may receive delivery/read receipts from each recipient device.
6. Large groups are optimized via Kafka topics per shard or per recipient group.

---

# **4. Multi-Device Support**

**Flow:**

1. When a user logs in from multiple devices → **each device registers** in Redis (`user:{userId}:devices`).
2. Messages are **delivered to all active devices**.
3. Delivery/read receipts are tracked **per device**.
4. Device disconnect → update Redis and lastSeen timestamp.
5. Any offline device will receive queued messages on reconnect.

---

# **5. Offline Message Delivery**

**Flow:**

1. Server receives a message, checks Redis → recipient devices offline.
2. Server **persists message in DB** (delivered=false) and optionally **pushes to Kafka offline queue**.
3. Recipient reconnects → client requests **undelivered messages**.
4. Server fetches from DB (`message_recipients` where delivered=false).
5. Server pushes messages via WebSocket → marks delivered=true.

---

# **6. Presence / Last Seen**

**Flow:**

1. Connect / disconnect or heartbeat timeout triggers **Redis update**.
2. Aggregated per-user status: **any device online → online**, all offline → lastSeen timestamp updated.
3. Server **broadcasts presence** to friends via Redis Pub/Sub.
4. Clients receive events → update UI.

---

# **7. Message History / Pagination**

**Flow:**

1. Client opens chat → requests recent messages from server via REST/WS.
2. Server checks **Redis cache** for recent messages → fallback to DB query if cache miss.
3. Server returns **page of messages + cursor**.
4. Client uses **cursor** for next page request → server queries older messages.

---

# **8. Typing Indicator**

**Flow:**

1. Client detects typing → sends **typing event** via WebSocket to server.
2. Server fetches **online recipient devices** from Redis.
3. Server broadcasts **typing event** to online recipients.
4. Recipients’ clients show typing indicator.
5. No persistence required.

---

# **9. Push Notifications (Optional)**

**Flow:**

1. Server detects **all recipient devices offline** when message arrives.
2. Server sends message to **push notification service** (APNs/FCM) with message summary.
3. Push service delivers notification → client wakes → fetches full message via WebSocket/REST.

---

# **10. Read Receipts**

**Flow:**

1. User opens message → client sends **read event** to server via WebSocket.
2. Server updates `message_recipients.read=true` for that device.
3. Server optionally aggregates **per-user read status**.
4. Server notifies sender (via WebSocket or Pub/Sub) → sender UI shows “read”.

---

# **Summary Table of Request Flow per Functional Requirement**

| Feature                  | Client → Server                 | Server → DB/Cache                 | Server → Recipients                           | Notes                            |
| ------------------------ | ------------------------------- | --------------------------------- | --------------------------------------------- | -------------------------------- |
| Connect / Presence       | WebSocket connect               | Redis HSET user\:devices          | Pub/Sub presence updates                      | Multi-device aggregated          |
| 1:1 Message              | WS send message                 | Persist messages + recipients     | Push WS to online devices / queue offline     | Delivery/read per device         |
| Group Message            | WS send message                 | Persist + multiple recipients     | Push WS to all online devices / queue offline | Large groups → Kafka shard       |
| Multi-Device Support     | Device registers via WS         | Redis user\:devices               | Push to all devices                           | Read/delivery tracked per device |
| Offline Message Delivery | -                               | Persist message + delivered=false | Delivered on reconnect via WS/Kafka           | Ensures guaranteed delivery      |
| Last Seen / Presence     | WS connect/disconnect/heartbeat | Redis HSET/HDEL + lastSeen        | Pub/Sub presence updates                      | Aggregated across devices        |
| Message History          | REST/WS fetch                   | DB / Redis cache                  | Response to client                            | Pagination via cursor            |
| Typing Indicator         | WS typing event                 | -                                 | Push WS to online recipients                  | Ephemeral, no DB persistence     |
| Push Notifications       | WS/DB detects offline           | -                                 | External push (APNs/FCM)                      | Optional, offline users only     |
| Read Receipts            | WS read event                   | Update message\_recipients        | Notify sender                                 | Aggregated per user/device       |

---

# **1. User Connection / Presence (Online/Offline)**

### **Request Flow**

**Connect (WebSocket)**

```json
{ 
  "type": "connect", 
  "userId": "123", 
  "deviceId": "device-1" 
}
```

**Server Actions:**

1. Register device in **Redis**:

```redis
HSET user:123:devices device-1 '{"serverId":"s1","socketId":"abcd","status":"online","lastSeen":1692361234}'
```

2. Publish **presence update** to friends:

```redis
PUBLISH presence:123 '{"userId":123,"status":"online"}'
```

**Disconnect / Heartbeat Timeout**

```redis
HDEL user:123:devices device-1
SET presence:123:lastSeen 1692362345
```

**Aggregated user presence**

```redis
HGETALL user:123:devices
# if empty → offline
```

---

# **2. 1:1 Messaging**

### **Client Request**

```json
{
  "type": "message",
  "chatId": "chat-101",
  "senderId": "123",
  "content": "Hello"
}
```

### **Server Flow**

1. Persist message in **DB**:

```sql
INSERT INTO messages(message_id, chat_id, sender_id, content, created_at)
VALUES (12345, 101, 123, 'Hello', NOW());
```

2. Insert **recipient row**:

```sql
INSERT INTO message_recipients(message_id, recipient_id, delivered, read)
VALUES (12345, 456, false, false);
```

3. Fetch recipient devices from **Redis**:

```redis
HGETALL user:456:devices
```

4. Push message to **all online devices** via WS (or Pub/Sub if multiple servers):

```redis
PUBLISH chat.toDevice.device-2 '{"messageId":12345,"content":"Hello"}'
```

5. If recipient offline → message remains in DB for later delivery.

---

### **DB Schema (1:1)**

**Messages Table**

| Column      | Type      | Notes |
| ----------- | --------- | ----- |
| message\_id | BIGINT PK |       |
| chat\_id    | BIGINT    | FK    |
| sender\_id  | BIGINT    | FK    |
| content     | TEXT      |       |
| created\_at | TIMESTAMP |       |

* Index: `chat_id` for message history retrieval
* Index: `sender_id` if needed for search

**Message Recipients Table**

| Column        | Type      | Notes                      |
| ------------- | --------- | -------------------------- |
| message\_id   | BIGINT PK | FK                         |
| recipient\_id | BIGINT PK | FK                         |
| delivered     | BOOLEAN   | per device if multi-device |
| read          | BOOLEAN   | per device if multi-device |

**Cache Keys**

* `user:{userId}:devices` → Hash of devices
* `chat:{chatId}:recentMessages` → List of last N messages for fast retrieval

---

# **3. Group Messaging**

**Request (same as 1:1)**

```json
{
  "type": "message",
  "chatId": "chat-202",
  "senderId": "123",
  "content": "Hello Group"
}
```

### **Server Flow**

1. Persist message in **DB** (same as 1:1).
2. Fetch **group members** (excluding sender):

```sql
SELECT user_id FROM chat_members WHERE chat_id = 202 AND user_id != 123;
```

3. Insert **message\_recipients** rows for each member:

```sql
INSERT INTO message_recipients(message_id, recipient_id, delivered, read) VALUES (12346, 456, false, false), (12346, 789, false, false);
```

4. Check each member's **Redis devices** → push WS message or queue offline delivery.

**Optimizations for large groups:**

* Fan-out via **Kafka topic per recipient shard**
* Redis **Pub/Sub channels** per device

---

# **4. Multi-Device Support**

**Device Registry (Redis)**

```redis
HSET user:123:devices device-1 '{"serverId":"s1","socketId":"abcd","status":"online"}'
HSET user:123:devices device-2 '{"serverId":"s2","socketId":"efgh","status":"online"}'
```

**Message Delivery Logic**

* Iterate all devices from `user:{recipientId}:devices`
* Push message → WebSocket (live)
* If offline → queue in Kafka / mark `delivered=false` in DB

**Read Receipts**

```sql
UPDATE message_recipients SET read=true WHERE message_id=12345 AND recipient_id=456 AND device_id='device-2';
```

---

# **5. Offline Message Delivery**

**Flow**

1. Message sent → recipient offline (Redis shows no active devices)
2. Persist in DB → delivered = false
3. Optionally publish to **Kafka offline queue**: `offlineMessages.{recipientId}`
4. On device reconnect → fetch undelivered messages:

```sql
SELECT * FROM message_recipients mr
JOIN messages m ON m.message_id = mr.message_id
WHERE recipient_id=456 AND delivered=false;
```

5. Send via WS → mark delivered=true

---

# **6. Presence / Last Seen**

**Redis Keys**

```redis
HSET user:{userId}:devices {deviceId} {serverId, socketId, status, lastSeen}
SET presence:{userId}:lastSeen <timestamp>
```

**Aggregated User Status**

* Online: `HGETALL user:{userId}:devices` → not empty
* Offline: empty → lastSeen = value

**Optional: Broadcast presence updates**

```redis
PUBLISH presence:{friendId} '{"userId":123,"status":"online"}'
```

---

# **7. Message History (Pagination)**

**Query**

```sql
SELECT * FROM messages
WHERE chat_id=101 AND message_id < :lastMessageId
ORDER BY message_id DESC
LIMIT 50;
```

**Cache**

* `chat:{chatId}:recentMessages` → Redis list, capped to last N messages for fast access

**Cursor Example**

```json
{"chatId":"101","lastMessageId":12345}
# base64 encode for opaque cursor
```

---

# **8. Typing Indicator (Ephemeral Feature)**

**Request**

```json
{ "type": "typing", "chatId": "101", "senderId": "123" }
```

**Flow**

* Broadcast to all **online recipient devices** via Redis Pub/Sub
* No DB persistence needed

---

# **9. Push Notifications (Optional)**

**Flow**

* If all devices offline → send push notification using APNs/FCM
* Triggered by Kafka offline message event

---

# **10. Summary Table of Key DB & Cache Structures**

| Component          | Storage     | PK / Indexes                 | Notes / Cache Keys              |
| ------------------ | ----------- | ---------------------------- | ------------------------------- |
| Messages           | SQL / NoSQL | message\_id PK, chat\_id idx | cache recent messages           |
| Message Recipients | SQL         | (message\_id, recipient\_id) | track delivered/read per device |
| Chat Members       | SQL / NoSQL | (chat\_id, user\_id)         | fetch group members             |
| Device Registry    | Redis       | Hash per user                | `user:{userId}:devices`         |
| Presence           | Redis       | Key per user                 | `presence:{userId}`             |
