## High-Level System Design (HLSD) for a Cloud File Storage System

-----

## 1\. Functional & Non-Functional Requirements

### Functional Requirements:

  * **P0: File Sync:** A client must be able to upload, download, and modify files and folders. These changes should be synchronized across all the user's connected devices.
      * **Complexity:** Handling concurrent modifications, resolving conflicts (e.g., last-writer-wins), and efficiently detecting and transferring only modified chunks of a large file.
  * **P0: Metadata Storage:** The system must store and manage file and folder metadata, including name, size, modification timestamp, version history, and ownership.
      * **Complexity:** Ensuring metadata consistency across devices and regions, and providing fast search and retrieval for a large number of files.
  * **P1: Deduplication:** The system should prevent redundant storage of identical files or file blocks across the entire user base.
      * **Complexity:** Maintaining a global hash index of all file blocks and their locations, and handling the complexity of linking multiple users to the same physical data.
  * **P1: File Sharing:** Users must be able to share files and folders with specific users or via a public link. Access control must be enforced.
      * **Complexity:** Managing complex access control lists (ACLs) and propagating permission changes quickly.
  * **P2: Versioning:** The system should retain multiple versions of a file, allowing users to restore previous states.
      * **Complexity:** Storing multiple versions without excessive storage overhead, potentially using a differential or delta-based approach.

### Non-Functional Requirements:

  * **Performance:**
      * **Latency:** File uploads/downloads should have a latency of less than **100ms** for the first byte transfer and be limited by network bandwidth.
      * **Throughput:** The system should support an average of **1,000 QPS** for metadata operations and **14,500 QPS** for file chunk uploads/downloads.
  * **Scalability:**
      * **Expected Growth:** The system should be designed for **10x** growth in the next 3 years.
      * **Concurrent Users:** Support **1 million** concurrent users (CCU).
      * **Storage:** Averages **5 GB** per user, with an expected growth of **50 PB** annually.
  * **Reliability & Availability:**
      * **Uptime:** **99.99%** uptime.
      * **Fault Tolerance:** The system should be resilient to the failure of individual nodes, zones, or even entire regions.
  * **Consistency:**
      * **Metadata:** **Strong consistency** for file metadata.
      * **File Content:** **Eventual consistency** for file content.
  * **Security:**
      * **Authentication:** OAuth2/OIDC.
      * **Encryption:** **End-to-end encryption** for data in transit (TLS) and at rest (AES-256).
  * **Compliance:** Adhere to GDPR and CCPA.
  * **Maintainability:** Robust monitoring, logging, and tracing.

### Assumptions & Scope:

  * **Assumptions:** 100 million Monthly Active Users (MAU), 50 million Daily Active Users (DAU), average of 5 GB/user.
  * **Out of Scope:** Client-side application development, real-time collaborative editing, and selective sync.
  * **Constraints:** Sufficient cloud budget, 12-18 month timeline.

-----

## 2\. Back-of-the-Envelope Estimation

  * **Total Storage:** $100M \\text{ users} \\times 5 \\text{ GB/user} = 500 \\text{ PB}$.
  * **Daily Write QPS (Metadata):** $50M \\text{ DAU} \\times 1 \\text{ change/day} / 86400 \\text{ sec/day} \\approx 580 \\text{ QPS (avg)}$. Peak QPS is approximately **5,800 QPS**.
  * **Daily Write QPS (File Chunks):** $50M \\text{ DAU} \\times 100 \\text{ MB/file} / 86400 \\text{ sec/day} \\approx 57.87 \\text{ GB/s}$. With a 4 MB chunk size, this is approximately **14,500 QPS** for chunk uploads.
  * **Cache Memory Estimate:** Caching 20% of metadata for 100M files at 1 KB/entry requires approximately **20 GB** of cache memory.
  * **Monthly Cloud Cost Ballpark:** The primary cost driver is storage and network egress. A rough estimate would be **$13M+ per month**.

-----

## 3\. API Design

We will use **REST** for most operations.

  * **`POST /api/v1/files/upload_url`**

      * **Description:** Initiates a file upload, requesting pre-signed URLs.
      * **Request Body:** `{"name": "my_photo.jpg", "size": 5242880, "checksum": "sha256:..."}`
      * **Response Body:** `{"upload_urls": [...], "file_id": "..."}`
      * **Implements:** File Sync (Upload).

  * **`GET /api/v1/files/download_url/{file_id}`**

      * **Description:** Requests a pre-signed URL to download a file.
      * **Response Body:** `{"download_url": "https://s3.aws.com/my-bucket/1234.bin?..."}`
      * **Implements:** File Sync (Download).

  * **`GET /api/v1/files/metadata/{folder_id}`**

      * **Description:** Retrieves a paginated list of file and folder metadata.
      * **Query Params:** `page_size`, `cursor`.
      * **Response Body:** `{"metadata": [...], "next_cursor": "eyJmaWxlX2lkIjoiZm9sZGVyNzg5IiwidGltZXN0YW1wIjoxNjU5MTk4NzY1fQ=="}`
      * **Implements:** Metadata Storage, File Sync.

  * **`POST /api/v1/sharing/{file_id}`**

      * **Description:** Creates a share link or adds a user to a file's ACL.
      * **Implements:** File Sharing.

**Opaque Cursor Format:** A Base64-encoded JSON object that captures the state of the last fetched item to enable consistent pagination.

  * **Example JSON:** `{"file_id": "folder789", "modified_at": 1659198765, "direction": "next"}`

-----

## 4\. High-Level Architecture

```
+------------+     +-----------------+     +----------------+
|   Client   | <-> |    API Gateway  | <-> |  Load Balancer |
|  (Desktop/ |     | (Auth, Rate Lim)|     |                |
|  Mobile)   |     |                 |     |                |
+------------+     +--------+--------+     +--------+-------+
       ^                  |                         |
       |                  |                         |
       v                  v                         v
+--------------+     +----------------+      +----------------+
| Notification |     | Metadata       |      | Chunking/Dedupe|
|  Service     |     | Service        |      | Service        |
| (WebSocket)  |     | (Metadata CRUD)|      | (Hash Indexing)|
+--------------+     +-------+--------+      +-------+--------+
       ^                     |                        |
       |                     |                        |
       v                     v                        v
+--------------+     +----------------+      +----------------+
|    Kafka     |     |  Metadata DB   | <--> |  Hash Index DB |
| (Event Bus)  |     | (DynamoDB/Cassandra)  |  (Cassandra)   |
+--------------+     +----------------+      +----------------+
       ^                                            |
       |                                            v
+------+----------+          +------------------------+
|S3 Event Notif.  |  <-----> |      Object Storage    |
+-----------------+          |       (S3)             |
                             +------------------------+
```

### Data Flows

#### Primary Write Operation (File Upload)

1.  **Client** requests pre-signed URLs from the **API Gateway** via `POST /upload_url`. The request includes file metadata and chunk hashes.
2.  The **Metadata Service** creates a "pending" file entry in the **Metadata DB**. It then requests and returns a list of pre-signed S3 URLs to the client.
3.  **Client** uploads file chunks directly to **S3** using the pre-signed URLs.
4.  Upon completion, **S3** sends an event notification to a pre-configured destination (e.g., SQS queue or Lambda).
5.  A background worker consumes the **S3 Event Notification** and updates the file's status in the **Metadata DB** to "active."
6.  The worker publishes a `file_metadata_changed` event to **Kafka**.
7.  The **Deduplication Service** consumes the event, checks the **Hash Index DB**, and updates references.
8.  The **Notification Service** consumes the event and pushes real-time updates to other online devices via WebSocket.

#### Primary Read Operation (File Download)

1.  **Client** requests a download URL from the **API Gateway** for a `file_id`.
2.  The **Metadata Service** retrieves metadata from the **Metadata DB** (with a **Redis** cache-aside) and checks user permissions.
3.  The service generates a single, time-limited pre-signed URL for the S3 object key.
4.  The **Client** receives the URL and makes a direct HTTP `GET` request to **S3**. It can use `Range` headers for parallel fetching.

#### Real-time Notifications/Sync

1.  A user's device makes a change (e.g., rename, move) which is written to the **Metadata DB**.
2.  A `file_metadata_changed` event is published to a **Kafka** topic.
3.  The **Notification Service** consumes the event and pushes a notification to all of the user's other devices via a **WebSocket** connection.
4.  **For offline devices**, when they come online, they make a pull-based sync request with a timestamp or sync token. The **Metadata Service** returns all metadata changes since that time. The client then makes separate requests for any new files it needs to download.

-----

## 5\. Functional Requirement Deep Dives

### File Sync Deep Dive

  * **Client → Server Flow (Offline Sync):** The client sends `GET /api/v1/files/metadata/{user_id}?timestamp=...` to the **Metadata Service**. The server returns a list of metadata changes (new files, renames, deletions). The client then iterates through the list, and for any new file, it makes a **separate** `GET /api/v1/files/download_url/{file_id}` request to download the file content.
  * **Data Storage:** The `Files` table in the **Metadata DB** stores file metadata.
  * **Server → Recipient Flow:**
      * **Online:** `file_metadata_changed` event published to Kafka is consumed by the **Notification Service**, which pushes changes via WebSocket.
      * **Offline:** The client will eventually perform a full sync when it comes online, requesting changes based on its last known sync timestamp.
  * **Edge Cases:**
      * **Offline user:** The client will get all pending metadata changes on reconnection and download missing files.
      * **Connection lost:** The client must be able to resume interrupted uploads and downloads using `Range` headers.
  * **Summary Table:**

| Functional requirement | Client → Server flow | Server → DB/Cache flow | Server → Recipient flow | Notes / multi-device / offline handling |
| :--- | :--- | :--- | :--- | :--- |
| File Sync | Upload: Request pre-signed URL(s). Then upload to S3 directly. Download: Request pre-signed URL. Then download from S3. Sync: Request metadata changes by timestamp. | Update Metadata DB on file completion/change. Publish event to Kafka. | **Online:** Push via WebSocket. **Offline:** Pull on reconnection via timestamped API call. | Offline clients use a pull-based model to retrieve all changes since their last sync. |

-----

## 6\. Component — Database & Storage Deep Dive

### Metadata Service - DynamoDB

  * **DB Choice:** **DynamoDB** is chosen for its scalability and low-latency performance at high loads.
  * **Table: `Files`**
      * **Columns:** `user_id` (string), `id` (string), `parent_id` (string), `name` (string), `type` (string), `size` (number), `version` (number), `modified_at` (number), `chunk_hashes` (list of strings), `acl` (map).
      * **PK:** `user_id` (Partition Key), `id` (Sort Key).
      * **Secondary Indexes:** `parent_id-modified_at-gsi` (Global Secondary Index with `parent_id` as PK and `modified_at` as SK) for listing folder contents.
      * **Example Queries:**
          * `SELECT * FROM Files WHERE user_id = 'user123' and id = 'file456';` (fast, PK lookup)
          * `SELECT * FROM Files INDEX parent_id-modified_at-gsi WHERE parent_id = 'folder789';` (fast, GSI lookup for folder listing)
  * **Sharding Strategy:** `user_id` is the primary partition key, distributing data based on users. For large users, additional strategies (e.g., using a hash of the folder ID) can be used to mitigate hotspots.

### Deduplication Service - Cassandra

  * **DB Choice:** **Cassandra** is a highly scalable, distributed NoSQL database with eventual consistency, which is acceptable for this asynchronous process.
  * **Table: `ChunkHashes`**
      * **Columns:** `chunk_hash` (string), `file_id` (list of strings), `user_id` (list of strings), `upload_timestamp` (number).
      * **PK:** `chunk_hash` (Partition Key).
  * **Data Model Trade-offs:** The eventual consistency of Cassandra is a trade-off for extreme write performance and availability. A temporary duplicate upload is acceptable since a background job can clean it up later.

-----

## 7\. Caching Strategy

  * **Layers:** **Redis** for app-level caching and **CDN** for static assets.
  * **Pattern:** **Cache-aside** is used. Services first check Redis before hitting the database.
  * **Eviction Policy:** **LRU** (Least Recently Used) with a TTL of **30 minutes** for metadata.
  * **Pagination Cache:** Pages of metadata are cached with keys like `metadata:folder:{folder_id}:{page_size}:{cursor}`.

-----

## 8\. Messaging & Eventing

  * **System:** **Kafka**.
  * **Topics:**
      * `metadata-updates`:
          * **Partition Key:** `user_id`. This guarantees ordered processing of events for a single user.
          * **Payload:** `{"event_id": "uuid123", "user_id": "user456", "file_id": "file789", "type": "file_modified", ...}`
  * **Idempotency:** The `event_id` in the message payload is used as an idempotency key. Consumers use a seen-set (e.g., a Redis Bloom filter) to ensure each message is processed only once.

-----

## 9\. Scalability, HA & Operations

  * **Autoscaling:** HPA on Kubernetes based on CPU/QPS for API services and consumer group lag for Kafka workers.
  * **Multi-AZ / Multi-Region:** Deploy services across multiple Availability Zones for HA and use multi-region active-passive for disaster recovery. Data replication via DynamoDB Global Tables and S3 Cross-Region Replication is essential.
  * **Monitoring:** Track RED metrics (Rate, Errors, Duration), Kafka consumer lag, and Redis hit ratio.
  * **Runbooks:** Document procedures for hot partitions, Kafka lag spikes, and service failures.

-----

## 10\. Security, Compliance & Privacy

  * **Auth:** OAuth2/OIDC for user authentication. mTLS for service-to-service communication.
  * **ACLs:** Row-level permissions are stored in the `acl` map within the `Files` table.
  * **Encryption:** Data is encrypted at rest and in transit.
  * **Compliance:** Systems are designed for data deletion and export to comply with GDPR/CCPA.

-----

## 11\. Cost, Trade-offs & Future Roadmap

  * **Cost Drivers:** S3 storage and network egress are the primary costs.
  * **Cost Optimization:**
      * **Deduplication** to minimize storage.
      * **Tiered Storage** (e.g., S3 Glacier) for old data.
      * **Caching** to reduce database costs.
  * **Trade-offs:** We chose strong consistency for metadata (for predictable behavior) and eventual consistency for file content and deduplication (for high performance at scale).
  * **Future Roadmap:** Selective sync, webhooks, real-time collaboration, and full-text search.


---

To make uploads pausable and resumable, a cloud file storage system must leverage the underlying object storage's support for **multipart uploads** and handle state management on both the client and server. This approach allows a user to stop an upload at any point and resume it later from the last successfully uploaded chunk.

***

### 1. The Core Technology: Multipart Uploads

The key to pausable and resumable uploads is the **multipart upload** feature provided by object storage services like Amazon S3. Instead of uploading a single large file, the client divides it into smaller, fixed-size chunks (e.g., 5 MB parts). Each chunk is uploaded independently and can be retried if it fails. Once all chunks are successfully uploaded, a final request "completes" the upload, and the storage service combines the chunks into a single file. 

***

### 2. Making Uploads Pausable

The mechanism for pausing an upload is straightforward: the client simply **stops sending new chunks**. The partially uploaded chunks remain in a temporary state within the object storage, tied to an `uploadId`. This means no data is lost when the user hits the "pause" button.

### 3. Resuming a Paused Upload

Resuming an upload is a three-step process that requires coordination between the client and your backend services.

#### Step 1: Client State Management

When a user initiates an upload, the client application must **store the state of the upload** in a local database (e.g., SQLite on mobile, IndexedDB in a browser). This state should include:
* The unique **`uploadId`** returned by the server.
* The list of all **chunks** and their current status (e.g., `uploaded`, `pending`, `failed`).
* The **ETag** or other identifier for each successfully uploaded chunk.

When the user pauses the upload, this state is persisted. If the app closes or the device loses power, the state is still available for a later resume.

#### Step 2: Client Initiates a Resume Request

When the user wants to resume, the client checks its local state. It sends a request to your **Storage Service** with the stored `uploadId`. This tells the server that the client wants to continue a specific, existing multipart upload session.

#### Step 3: Server and Client Resume Synchronization

The **Storage Service** receives the `uploadId` and queries the object storage to get a list of all parts that have already been uploaded for that session. It then returns this list to the client.

The client compares the list of parts from the server with its own local state. It then **resumes uploading only the chunks that are missing or incomplete**. This ensures that no data is re-uploaded unnecessarily.

### Example Workflow: Handling an Internet Disruption

Let's say a user is uploading a 100 MB file (20 chunks of 5 MB).

1.  The client uploads chunks 1 through 10 and stores their `ETag`s locally.
2.  The internet connection is lost. The client detects the failure for chunk 11 and stops. The local state shows chunks 1-10 are complete.
3.  The user reconnects. The client checks its local state and finds an incomplete upload with a specific `uploadId`.
4.  The client sends a resume request with the `uploadId` to the server.
5.  The server queries S3 and confirms that chunks 1-10 are present. It sends this confirmation back to the client.
6.  The client, knowing chunks 1-10 are already on the server, **begins uploading from chunk 11**. It continues until all 20 chunks are uploaded.
7.  The client sends the final `CompleteMultipartUpload` request to the server, providing the `ETag`s for all 20 chunks.
8.  The server sends the final completion request to S3, and the file is assembled.

This method ensures a seamless and efficient user experience, even with poor or intermittent network conditions.

---

No, you don't need to re-upload the entire 100MB file for a small change. Re-uploading the whole file is highly inefficient and wastes bandwidth and storage. A much better solution is to use a **delta sync** or **block-level sync** approach.

## Delta Sync Solution 🚀

The core idea of delta sync is to only upload the parts of the file that have changed. This is a common strategy used by services like Dropbox and Google Drive to optimize sync for large files.

Here's how it works:

### 1. File Chunking

When a file is first uploaded, the client doesn't treat it as a single block of data. Instead, it breaks the file into smaller, fixed-size chunks (e.g., 4MB). It then computes a cryptographic hash (like SHA-256) for each chunk. The server stores these chunks and the list of their hashes, which together represent the entire file.

### 2. Detecting Changes

When you modify the file on your local device, the client application re-processes it. It re-chunks the file and recalculates the hashes for all the chunks.

### 3. Comparing Hashes

The client now has a new list of chunk hashes. It compares this new list with the old list of hashes stored on the server.
* **Unchanged chunks:** If a chunk's hash remains the same, it means the content of that chunk hasn't changed. The client knows this chunk already exists on the server, so it doesn't need to be re-uploaded.
* **Changed chunks:** If a chunk's hash is different, it means the content of that chunk has been altered. This is the **delta** or "the change."

### 4. Uploading the Delta

The client only uploads the new or changed chunks to the server. The server then updates its metadata for the file, replacing the old hashes with the new ones and linking to the newly uploaded chunks.

For your 100MB text file, if you change only a few characters, it's highly likely that only a single 4MB chunk will be affected. In this case, the client would only upload that 4MB chunk, saving 96% of the bandwidth compared to a full upload.

### 5. Efficient Checksumming

A more advanced version of this, used by some systems, is called **rolling checksums** or **Rsync-style delta encoding**. This method can detect changes even if they shift the position of chunks within the file. For example, if you insert a few characters at the beginning of the file, it would normally cause every subsequent chunk to have a different hash. With rolling checksums, the system can identify and re-use the majority of the unchanged blocks, even if their positions have shifted. 

By using these methods, a cloud storage system can significantly reduce the amount of data transferred and improve the sync experience for users, especially for large files.

---
