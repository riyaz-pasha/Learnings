I have formatted your system design into a clean markdown document. I've also reviewed the design for consistency and clarity, making minor adjustments to improve the flow and precision of the low-level details while preserving your core architectural choices.

---

### **1. Executive Summary**

This document details the high-level and granular system design for a scalable, real-time chat application, capable of supporting 1:1 and group messaging, multi-device access, online/offline presence, and guaranteed message delivery. The architecture evolves through three phases (Day 0, Day 1, Day 2), progressively introducing greater scale, resilience, and feature robustness. The final Day 2 architecture leverages **stateless WebSocket Gateways**, **Kafka** for durable message fan-out, and **Redis** for ephemeral state management and real-time presence.

---

### **2. Core Functional Requirements & Their Implementation**

#### **2.1. User Connection / Presence (Online/Offline)**

* **Objective:** To allow users to connect via WebSocket and accurately reflect their online or offline status across multiple devices and to their contacts.

* **Request Flow (Detailed):**
    1.  **Client Connect:** A client establishes a WebSocket connection to the chat service via a single, public endpoint (e.g., `wss://chat.myapp.com`).
    2.  **Load Balancer (LB) Routing:** A Load Balancer (e.g., L4 TCP LB like AWS NLB) routes the connection to any available **stateless WS Gateway**.
    3.  **Gateway Registration:** The WS Gateway authenticates the user (e.g., via a JWT/OIDC token). Upon success, it registers the user's device connection in a shared **Redis Cluster**.
        * **Redis Command:** `HSET user:{userId}:devices {deviceId} '{"serverId":"s1","socketId":"abcd","status":"online","lastSeen":1692361234}'`
        * This entry includes the ID of the Gateway (`serverId`) holding the connection.
    4.  **Heartbeating:** The client sends a "ping" heartbeat (e.g., every 30 seconds). The Gateway responds with "pong" and/or refreshes the TTL on the Redis key to indicate the device is still active.
        * **Redis Command:** `EXPIRE conn:{deviceId} 120` (refreshed periodically)
    5.  **Presence Update Publication:** The Gateway publishes a presence event to a **Redis Pub/Sub** channel (`presence:{userId}`). Other services or subscribed Gateways consume this event to update the user's status for their friends.
        * **Redis Command:** `PUBLISH presence:{userId} '{"userId":123,"status":"online","deviceId":"device-1"}'`

* **Disconnect / Heartbeat Timeout Flow:**
    1.  **Abrupt Disconnect:** If the client or network connection is lost, the Gateway relies on the absence of heartbeats.
    2.  **Timeout Detection:** After a configured `heartbeat_timeout` (e.g., 90 seconds), the Gateway assumes the device is disconnected.
    3.  **Redis Deregistration:** The Gateway (or a background cleanup job) removes the device's entry from the Redis `user:{userId}:devices` hash.
        * **Redis Command:** `HDEL user:{userId}:devices {deviceId}`
    4.  **Last Seen Update:** If this was the user's last active device, the `lastSeen` timestamp in Redis is updated, and an "offline" presence event is published.
        * **Redis Command:** `SET presence:{userId}:lastSeen 1692362345`

* **Data Storage & Retrieval:**
    * **Redis (Volatile State):** Stores device connection mappings (`user:{userId}:devices`) and ephemeral presence status (`presence:{userId}:lastSeen`).
    * **DB (Optional):** For long-term presence analytics.

#### **2.2. 1:1 Messaging**

* **Objective:** To enable two users to exchange messages in real-time, reliably, and with multi-device support.

* **Request Flow (Detailed):**
    1.  **Client Sends Message:** User A sends a message over their established WebSocket connection to any WS Gateway.
        * **WS Message:** `{ "type": "message", "chatId": "chat-101", "senderId": "123", "content": "Hello" }`
    2.  **Gateway Processing:** The receiving WS Gateway validates the message.
    3.  **Persistence (Cassandra/DB):** The Gateway (or a dedicated Message Service) persists the message to a **Cassandra/NoSQL DB** (for high write throughput). A `server_msg_id` (e.g., Snowflake ID or TimeUUID) is generated.
        * **Cassandra Insert:** `INSERT INTO messages_by_chat (chat_id, bucket, server_msg_id, sender_user_id, ciphertext, created_at) VALUES ('chat-101', '2025-08-14', '1747268850-0001', '123', ..., NOW()) IF NOT EXISTS;`
    4.  **Kafka Publish (Send Event):** The Gateway publishes a "message send" event to a **Kafka topic** (`chat.message.send`), keyed by `chatId` for per-chat ordering.
        * **Kafka Record:** `ProducerRecord(topic="chat.message.send", key="chat-101", value={serverMsgId, chatId, sender, recipientDeviceIds, hint})`
    5.  **Offline Inbox Update (Redis):** For each recipient device, the `serverMsgId` is added to a **Redis Sorted Set (`ZSET`)** representing a per-device inbox for pending messages.
        * **Redis Command:** `ZADD inbox:{deviceId} NX {serverMsgIdScore} "{serverMsgId}"`
    6.  **Delivery Processing (Kafka Consumer):** A **Delivery Worker** service (a Kafka consumer group) processes the event. It determines recipient devices by looking up active connections in Redis.
        * **Idempotency Check:** Before pushing, it uses `SETNX delivered:{deviceId}:{serverMsgId}` to prevent duplicate pushes from rebalancing consumers.
        * **Delivery:** It either sends the message directly over the local WebSocket (if the device's Gateway is co-located) or publishes a message to the remote Gateway's dedicated Redis Pub/Sub channel.
    7.  **Client Receives Message:** The target Gateway receives the push, hydrates the message payload, and sends it over the WebSocket to the recipient's device.

#### **2.3. Group Messaging**

* **Objective:** To enable multiple users to communicate in a shared chatroom.

* **Flow (Unified with 1:1):** The flow is largely the same, with one key difference:
    * **Recipient Resolution:** Instead of one recipient, the Message Service retrieves all members of the `chatId` from a **PostgreSQL `chat_members` table** (often cached in a **Redis `SET`**). The Kafka event then includes the list of all recipient device IDs.

* **Data Storage & Retrieval:**
    * **PostgreSQL:** `groups` and `group_members` tables manage group metadata and memberships.
    * **Redis:** `chat:{chatId}:members` stores member user IDs for quick fan-out lookups.

#### **2.4. Multi-Device Support**

* **Objective:** To enable a single user to concurrently use multiple clients and receive all messages on all active devices.

* **Implementation:**
    * **Device Identification:** Each client is assigned a unique `deviceId`.
    * **Device Registry (Redis):** The `user:{userId}:devices` Redis Hash tracks all active devices for a user.
    * **Unified Delivery Path:** The delivery path is designed to iterate through all active devices for each recipient, pushing the message to each one.
    * **Read Receipts per Device:** The database schema tracks read status per device, allowing for granular "seen by all devices" flags.

#### **2.5. Offline Message Delivery**

* **Objective:** To ensure messages sent to an offline user are reliably stored and delivered upon reconnection.

* **Implementation:**
    * **Offline Detection:** If a Delivery Worker checks Redis and finds no active Gateway for a device, it is considered offline.
    * **Queuing for Offline Delivery:** The message remains in the durable **Cassandra DB** and is queued in the `inbox:{deviceId}` Redis ZSET for fast access upon reconnect.
    * **Delivery on Reconnect:** When a user's device reconnects, the Gateway immediately queries the `inbox:{deviceId}` ZSET for pending messages, pushes them over the new WebSocket connection, and removes them from the queue after a client acknowledgment.

#### **2.6. Message History / Pagination**

* **Objective:** To allow clients to retrieve older messages in a chat, efficiently and in paginated chunks.

* **Implementation:**
    * **Client Request:** A client requests history via an **HTTP GET** to the chat server, including a `chatId` and an opaque cursor.
    * **Server Retrieval (Cache-Aside):** The server first checks a Redis cache (`chat:{chatId}:recentMessages`) for the most recent messages. If not found, or if an older history is requested, it queries the **Cassandra/PostgreSQL `messages_by_chat` table** using the cursor. The `server_msg_id` (a time-ordered UUID) is used for efficient range queries.

#### **2.7. Typing Indicator (Ephemeral Feature)**

* **Objective:** To show when another user is typing, without persisting the state.

* **Implementation:**
    * **Client Sends Event:** A lightweight WebSocket event is sent to the server.
    * **Server Broadcast:** The server looks up all online recipients for that `chatId` from Redis and broadcasts the event via Redis Pub/Sub or RPC calls. No database persistence is needed.

---

### **3. Comprehensive Data Models**

#### **3.1. PostgreSQL (Metadata & Relationships)**

| Table Name | **`users`** | **`devices`** | **`chats`** | **`chat_members`** | **`contacts`** |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Columns** | `id BIGSERIAL PK`, `phone TEXT UNIQUE`, `display_name TEXT` | `id BIGSERIAL PK`, `user_id BIGINT FK`, `device_type TEXT`, `push_token TEXT` | `id UUID PK`, `kind TEXT`, `owner_id BIGINT FK` | `chat_id UUID FK`, `user_id BIGINT FK`, `role TEXT` | `user_id BIGINT FK`, `contact_user_id BIGINT FK` |
| **PK** | `id` | `id` | `id` | `(chat_id, user_id)` | `(user_id, contact_user_id)` |
| **Indexes** | `phone` | `user_id` | N/A | `user_id` | N/A |

#### **3.2. Cassandra/Scylla (Message History)**

| Table Name | **`messages_by_chat`** |
| :--- | :--- |
| **Columns** | `chat_id TEXT`, `bucket DATE`, `server_msg_id TIMEUUID`, `sender_user_id TEXT`, `ciphertext BLOB`, `media TEXT`, `created_at TIMESTAMP` |
| **Primary Key** | `PRIMARY KEY ((chat_id, bucket), server_msg_id)` |
| **Queries** | `SELECT * FROM messages_by_chat WHERE chat_id = ? AND bucket = ? AND server_msg_id > ? ORDER BY server_msg_id ASC LIMIT ?;` |

#### **3.3. Redis Cluster (Real-time State)**

* **Connection/Device Registry:**
    * `conn:{deviceId}`: String, `value={gatewayId}`, **EX 120** (TTL refreshed by heartbeat).
    * `user:{userId}:devices`: Hash, stores `deviceId` to connection details (`serverId`, `socketId`).
* **Inboxes (Pending Messages):**
    * `inbox:{deviceId}`: **ZSET**, `score={snowflakeIdAsLong}`, `member={serverMsgId}`.
* **Message Metadata Cache:**
    * `msg:{serverMsgId}`: Hash, stores `sender`, `ts`, `hint`. **EX 3600**.
* **Presence:**
    * `presence:{userId}`: String, `value="online|offline"`.
    * `typing:{chatId}:{userId}`: String, **EX 8**.
* **Idempotency Guards:**
    * `idemp:{userId}:{clientMsgId}`: String, `SETNX`, **EX 86400**.
    * `delivered:{deviceId}:{serverMsgId}`: String, `SETNX`, **EX 172800**.

#### **3.4. S3/Blob Storage (Media)**

* **Object Keys:** `m/yyyy/mm/dd/{uuid}`
* **Access:** Handled via a separate Media Service that generates **signed URLs**.
* **Cost Optimization:** Uses lifecycle policies to transition data from Standard to Infrequent Access to Glacier.
