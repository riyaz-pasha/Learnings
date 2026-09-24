## 1\. Functional & Non-Functional Requirements

### Functional Requirements:

  * **P0: User Video Upload**: Users can upload video files in various formats. The system must process, transcode, and store them. This includes a user-friendly interface for selecting files, showing upload progress, and displaying completion status.
      * **Complexity**: Video file sizes can be very large, and network interruptions during uploads are common. Direct uploads to a backend server can be a bottleneck. Handling different codecs and resolutions is also complex.
  * **P0: Video Transcoding & Processing**: The uploaded video is converted into multiple formats and resolutions (e.g., 144p, 360p, 720p, 1080p, 4K) to ensure compatibility and optimize streaming for various devices and network conditions.
      * **Complexity**: This is a CPU-intensive and time-consuming process. The system must be scalable to handle a high volume of concurrent transcoding jobs. The process can fail midway, requiring a robust retry mechanism.
  * **P0: Video Streaming & Playback**: Users can watch videos with minimal latency and buffering. The player should support adaptive bitrate streaming (e.g., HLS or DASH) to dynamically adjust video quality based on network speed.
      * **Complexity**: Delivering video segments efficiently requires a global CDN. Ensuring low latency for starting a video and avoiding stalls during playback is critical. The system must manage a massive amount of read traffic.
  * **P1: Video Metadata Management**: Users can add a title, description, tags, and a thumbnail for their videos. This metadata is indexed for search and discovery.
      * **Complexity**: The system needs a fast, scalable search index that can handle a large volume of data and complex queries (full-text search, filtering, etc.).
  * **P1: Search & Discovery**: Users can search for videos by title, tags, or description. The system should also recommend videos based on user viewing history and other factors.
      * **Complexity**: Building a relevance-based search engine and a real-time recommendation system is highly complex. The system needs to analyze user behavior and video metadata.
  * **P2: User Account Management**: Users can sign up, log in, manage their profiles, and view their uploaded content.
      * **Complexity**: Standard authentication and authorization complexities.

### Non-Functional Requirements:

  * **Performance**:
      * **Latency**: Video playback start latency \< 2 seconds. Transcoding completion \< 10 minutes for a 1-hour 1080p video.
      * **Throughput**: Support millions of concurrent video streams.
      * **Peak vs. Average Load**: Assume peak load is 5x the average load during prime viewing hours.
  * **Scalability**:
      * **Growth**: Expected to grow by 50% year-over-year.
      * **Users**: Initial 1M daily active users (DAU), scaling to 100M+ DAU.
      * **QPS**: As estimated in the BOTE section.
  * **Reliability & Availability**:
      * **Uptime**: 99.99% for video playback service. 99.9% for upload and processing services.
      * **Fault Tolerance**: The system should gracefully handle the failure of individual components (e.g., a single transcoder node) without service interruption.
  * **Consistency**:
      * **Eventual Consistency**: Acceptable for video metadata (e.g., search results might be slightly stale after an update).
      * **Strong Consistency**: Required for critical user data (e.g., user profile, video ownership).
  * **Security**:
      * **Authentication**: OAuth2/OIDC for user login.
      * **Authorization**: Ensure users can only access/manage their own videos.
      * **Encryption**: All data in transit (mTLS for internal services, TLS for client communication) and at rest (disk encryption) must be encrypted.
  * **Maintainability**:
      * **Monitoring**: Comprehensive logging, metrics, and tracing for all services.
      * **Updates**: Zero-downtime deployments.

### Assumptions & Scope:

  * **User Base**: The system is designed for a global audience with an initial user base of 10M MAU and 1M DAU.
  * **Video Content**: Videos are primarily user-generated and can range from a few seconds to several hours.
  * **Out of Scope**: Live streaming, video editing features, comments/likes/dislikes, monetization (ads), and advanced recommendation systems (e.g., using deep learning).
  * **Constraints**: Budget is not a primary constraint, prioritizing scalability and reliability.

-----

## 2\. Back-of-the-Envelope Estimation

**Scenario**: A video streaming service with 10M Monthly Active Users (MAU) and 1M Daily Active Users (DAU).

  * **DAU**: 1M
  * **Daily Video Views per DAU**: 5
  * **Average Video Length**: 10 minutes
  * **Average Video Resolution**: 720p (2 Mbps bitrate)
  * **Total Video Uploads per Day**: 100,000 (0.1 video/DAU)
  * **Average Raw Video File Size**: 500 MB (for a 10-minute 1080p video)
  * **Transcoding Output Ratio**: 2x (output size is 2x the raw input size due to multiple formats)

**Read/Write QPS & Storage Needs:**

  * **Video Playback QPS (Read)**:
      * Total daily watch time: $10^6 \\text{ DAU} \\times 5 \\text{ views/DAU} \\times 10 \\text{ min/view} \\approx 5 \\times 10^7 \\text{ minutes/day}$
      * Total daily watch data: $5 \\times 10^7 \\text{ min} \\times 60 \\text{ s/min} \\times 2 \\text{ Mbps} = 6 \\times 10^{15} \\text{ bits/day} \\approx 750 \\text{ TB/day}$
      * **Read QPS**: This is more about streaming throughput. A single stream needs $2 \\text{ Mbps}$.
  * **Video Upload QPS (Write)**:
      * Daily uploads: $100,000$
      * Daily upload data: $100,000 \\text{ uploads} \\times 500 \\text{ MB/upload} = 50 \\text{ TB/day}$
      * **Average Write QPS**: $100,000 \\text{ uploads} / (24 \\text{ hr} \\times 3600 \\text{ s/hr}) \\approx 1.15 \\text{ writes/s}$ (very low QPS, but high throughput)
      * **Peak Write QPS**: Assume 10x average $\\approx 11.5 \\text{ writes/s}$
  * **Metadata Write QPS**:
      * One video upload means one metadata write. So, $\\approx 1.15 \\text{ QPS}$.
  * **Metadata Read QPS**:
      * Assume each DAU performs 10 video metadata reads (search, browsing): $10^6 \\text{ DAU} \\times 10 \\text{ reads/DAU} / (24 \\text{ hr} \\times 3600 \\text{ s/hr}) \\approx 115 \\text{ QPS}$
      * **Peak Read QPS**: 10x average $\\approx 1150 \\text{ QPS}$
  * **Total Storage Needs**:
      * Assuming 3 years of video data retention:
      * Daily storage: $50 \\text{ TB/day} \\text{ (raw)} \\times 2 \\text{ (transcoded)} = 100 \\text{ TB/day}$
      * Total storage: $100 \\text{ TB/day} \\times 3 \\text{ years} \\times 365 \\text{ days/year} \\approx 110 \\text{ PB}$ (Petabytes)

**Cache Memory Estimation**:

  * Assume we cache metadata for the top 10% most viewed videos.
  * Total videos: $100,000 \\text{ uploads/day} \\times 3 \\text{ years} \\approx 1.1 \\times 10^8 \\text{ videos}$
  * Top 10% videos: $1.1 \\times 10^7$
  * Metadata size per video: Assume 1 KB.
  * **Total Cache Size**: $1.1 \\times 10^7 \\text{ videos} \\times 1 \\text{ KB/video} \\approx 11 \\text{ GB}$ (A small amount of memory for metadata cache).
  * However, video streaming caches are far larger. The CDN is the primary cache.

**Monthly Cloud Cost Ballpark**:

  * **Video Storage (S3)**: $110 \\text{ PB} \\times $20/\\text{TB/mo} \\approx $2.2 \\text{M/mo}$
  * **CDN (CloudFront)**:
      * Daily egress: $750 \\text{ TB}$
      * Monthly egress: $750 \\text{ TB} \\times 30 \\text{ days} = 22.5 \\text{ PB}$
      * Cost: $22.5 \\text{ PB} \\times $0.01/\\text{GB} \\approx $225,000/\\text{PB} \\times 22.5 \\approx $5 \\text{M/mo}$ (Highly variable based on region and usage tier)
  * **Compute (EC2 for Transcoding)**: Highly variable. Assume 1000 instances running 24/7 at an average cost of $0.50/hr = 1000 \\times 24 \\times 30 \\times 0.5 \\approx $360,000/\\text{mo}$.
  * **Databases & Search**: Assume another $$50,000/\\text{mo}$.
  * **Total**: \~$8M/mo.

-----

## 3\. API Design

### REST Endpoints

**1. Video Upload (`P0`)**

  * **Endpoint**: `POST /api/v1/videos/upload`
  * **Request**: `multipart/form-data` with video file and JSON metadata.
      * `title`: string
      * `description`: string
      * `tags`: string[]
  * **Response**: `201 Created`
      * `id`: string (unique video ID)
      * `status`: string ('pending\_processing')
  * **Auth**: Bearer token (JWT)
  * **Rate-limiting**: 5 uploads/user/hour
  * **Complexity**: Large payload size. The actual video file is uploaded to a pre-signed S3 URL, and this API call only registers the metadata and signals the start of processing.

**2. Video Status (`P0`)**

  * **Endpoint**: `GET /api/v1/videos/{video_id}/status`
  * **Response**: `200 OK`
      * `status`: string ('pending\_processing', 'processing', 'ready', 'failed')
      * `progress`: integer (0-100, if applicable)
  * **Auth**: Bearer token (JWT)
  * **Rate-limiting**: 100 requests/minute

**3. Video Playback (`P0`)**

  * **Endpoint**: `GET /api/v1/videos/{video_id}/stream`
  * **Description**: This endpoint provides the HLS/DASH manifest URL, which is a pointer to the CDN.
  * **Response**: `200 OK`
      * `manifest_url`: string (URL to CDN)
  * **Auth**: Not required for public videos.
  * **Rate-limiting**: N/A (handled by CDN).

**4. Search Videos (`P1`)**

  * **Endpoint**: `GET /api/v1/search/videos`
  * **Request**: Query parameters
      * `q`: string (search query)
      * `page_size`: integer
      * `cursor`: string (opaque cursor for pagination)
  * **Response**: `200 OK`
      * `videos`: VideoMetadata[]
      * `next_cursor`: string | null
  * **Auth**: Optional
  * **Rate-limiting**: 10 requests/second/IP

**Opaque Cursor Format**:
The cursor is a Base64-encoded JSON string that contains the sort key and the ID of the last item from the previous page.

**Example Cursor JSON**:

```json
{
  "search_timestamp_ms": 175654321000,
  "last_video_id": "v-12345",
  "query_hash": "a1b2c3d4e5f6g7h8"
}
```

  * `search_timestamp_ms`: The timestamp when the original query was executed to ensure consistent results across pages.
  * `last_video_id`: The unique ID of the last video in the current page, used to start the next query.
  * `query_hash`: A hash of the original query parameters to prevent tampering and ensure the cursor is used with the correct query.

-----

## 4\. High-Level Architecture

  * **Edge**: Global CDN (Cloudflare, CloudFront) for serving video chunks and static assets, reducing latency and offloading load from the origin servers.
  * **API Gateway**: Manages routing, authentication, rate-limiting, and SSL termination. Routes requests to the appropriate microservices.
  * **Services**:
      * **Upload Service**: Handles pre-signed URL generation for video uploads.
      * **Metadata Service**: Manages video metadata (title, description).
      * **Transcoding Service**: A collection of workers that handle CPU-intensive video conversions.
      * **Search Service**: Provides video search and discovery.
  * **Databases**:
      * **Metadata DB**: Stores video metadata.
      * **User DB**: Stores user profile information.
  * **Storage**:
      * **Raw Video Storage**: High-durability object storage (e.g., S3) for the original, unprocessed video files.
      * **Transcoded Video Storage**: S3 for the transcoded video segments, accessible by the CDN.
  * **Caches**:
      * **Metadata Cache**: Redis or Memcached for frequently accessed video metadata.
  * **Queues**:
      * **Transcoding Queue**: A message queue (e.g., Kafka, SQS) to decouple the upload service from the transcoding workers. The upload service publishes a message, and the workers consume it.
  * **Protocols**:
      * **Client to API GW**: HTTPS
      * **Internal Services**: gRPC for high-performance RPCs, or HTTP/REST for simplicity.
      * **Queues**: Kafka protocol for a robust, persistent message bus.

### Data Flows

**Primary Write Operation (Video Upload & Processing)**

1.  **Client** → **API Gateway** → **Upload Service**: The client requests a pre-signed S3 URL to upload a video.
2.  **Upload Service** → **S3**: The service generates and returns a temporary, secure URL.
3.  **Client** → **S3**: The client uploads the raw video file directly to S3, bypassing the backend server.
4.  **S3** → **Transcoding Queue (via S3 event notification)**: Once the upload is complete, S3 triggers an event (e.g., a Lambda function or SQS message) which publishes a message to the Transcoding Queue.
5.  **Transcoding Service (Workers)** → **Transcoding Queue**: Workers consume the message, download the raw video from S3.
6.  **Transcoding Service** → **S3**: Workers transcode the video and upload the transcoded segments (HLS/DASH) back to S3.
7.  **Transcoding Service** → **Metadata Service**: The worker updates the video status in the Metadata DB to 'ready' and pushes new metadata (e.g., available resolutions).

**Potential Bottlenecks & Resiliency**:

  * **Transcoding**: This is the main bottleneck. The Transcoding Queue and worker pool must be able to scale horizontally.
  * **Resiliency**:
      * **DLQ (Dead Letter Queue)**: For messages that fail to be processed by workers, they are moved to a DLQ for manual inspection and retry.
      * **Retries**: Workers should have a built-in retry mechanism with exponential backoff for transient failures.
      * **Circuit Breakers**: In case of a cascading failure, a circuit breaker pattern can prevent overloading downstream services.

**Primary Read Operation (Video Playback)**

1.  **Client** → **API Gateway** → **Metadata Service**: The client requests the video's playback URL using its ID.
2.  **Metadata Service** → **Metadata Cache (Redis)**: The service first checks the cache for the video's manifest URL.
3.  **Metadata Cache** → **Metadata Service**: If found, return the URL.
4.  **Metadata Service** → **Metadata DB**: If not found in the cache, the service fetches the URL from the DB and populates the cache.
5.  **Metadata Service** → **API Gateway** → **Client**: The service returns the manifest URL.
6.  **Client** → **CDN**: The client's video player requests the manifest and video segments directly from the CDN.

**Potential Bottlenecks & Resiliency**:

  * **CDN**: The CDN must handle the vast majority of traffic. The origin servers should only be hit for new or less popular content.
  * **Metadata DB**: A highly sharded, fast DB is required to handle the read load, with caching as the first line of defense.
  * **Resiliency**: The system should use a multi-region CDN and have failover mechanisms for the Metadata DB.

-----

## 5\. Functional Requirement Deep Dives

### P0: Video Transcoding & Processing

**Components involved**: Upload Service, Transcoding Queue (Kafka), Transcoding Workers, S3, Metadata Service.

**Step-by-step data flow**:

1.  **Request Flow (Client → Server)**: The client initiates a file upload via the Upload Service. The service responds with a temporary, pre-signed S3 URL. The client then uploads the video file directly to S3.
2.  **Data Storage & Retrieval**:
      * S3's event notification system is configured to trigger on a new object creation in the `raw-videos` bucket.
      * This event publishes a message to a **Kafka topic `transcoding_jobs`**. The message payload contains the S3 object key and video ID.
      * A group of stateless **Transcoding Workers** (e.g., EC2 instances) acts as consumers for the `transcoding_jobs` topic.
      * Each worker processes a message by downloading the video from S3.
      * It uses a video processing tool like **FFmpeg** to create multiple video formats (e.g., HLS and DASH) at different resolutions.
      * The transcoded segments and manifest files are uploaded to another S3 bucket, `transcoded-videos`.
      * The worker then calls the Metadata Service to update the video's status in the database from `processing` to `ready`. It also stores the S3 URL of the manifest file.
3.  **Delivery to other clients**: This is an asynchronous process. Once the video is marked `ready`, a playback request from any client will retrieve the manifest URL from the Metadata Service, which points to the CDN.
4.  **Edge cases & failure handling**:
      * **Transcoding Failure**: If a worker fails (e.g., due to a corrupt file or an instance crash), the Kafka message remains uncommitted. Another worker in the consumer group will pick it up and retry the job.
      * **Idempotency**: The transcoding worker is designed to be idempotent. It uses the video ID to prevent duplicate processing. Before starting, it can check a distributed lock or the DB status. If the video is already `ready`, it skips processing.
      * **Dead Letter Queue (DLQ)**: After a few failed retries, the message is moved to a `transcoding_dlq` topic. An alert is triggered, and a developer can manually inspect the failed job.

**Summary Table**:

| Functional Requirement      | Client → Server Flow                                                                 | Server → DB/Cache Flow                                                                    | Server → Recipient Flow | Notes / multi-device / offline handling                                        |
| --------------------------- | ------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------- | ----------------------- | ------------------------------------------------------------------------------ |
| Video Upload & Transcoding  | Client requests pre-signed URL. Uploads directly to S3.                              | S3 event → Kafka message → Worker downloads video from S3. Transcodes and uploads to S3. | N/A (async process)     | Transcoding is asynchronous. Status is eventually updated. Works for any device. |

### P0: Video Streaming & Playback

**Components involved**: Client (Player), CDN, API Gateway, Metadata Service, Metadata Cache (Redis), Metadata DB.

**Step-by-step data flow**:

1.  **Request Flow (Client → Server)**: The user clicks on a video. The client player makes an API call to `GET /api/v1/videos/{video_id}/stream`.
2.  **Data Retrieval**:
      * The request hits the **API Gateway** and is routed to the **Metadata Service**.
      * The Metadata Service first checks the **Redis cache** for the key `video:{video_id}:manifest_url`.
      * **Redis Command**: `GET video:v-12345:manifest_url`
      * If a cache hit occurs, the URL is returned immediately.
      * If a cache miss, the service queries the **Metadata DB** for the manifest URL and resolution details.
      * The manifest URL and other details are stored in the cache for future requests.
      * The service returns a `200 OK` response with the `manifest_url`.
3.  **Delivery**: The client's video player uses the `manifest_url` to request the HLS/DASH manifest file from the **CDN**. The manifest contains pointers to the video segments. The player then requests the video segments from the CDN.
4.  **Edge cases & failure handling**:
      * **CDN failure**: If the CDN goes down, the client can be configured to fall back to the origin server, though this would cause a significant increase in latency and load. A multi-CDN strategy is a better approach.
      * **Stale Cache**: A short TTL on the cache ensures that changes to the video metadata (e.g., a thumbnail update) are propagated.
      * **Hotspot Scenario**: A sudden spike in views for a new, popular video can be handled by the cache. The first request to the origin populates the cache, and subsequent requests are served from the cache, preventing the database from being overloaded.

**Summary Table**:

| Functional Requirement | Client → Server Flow                                                                 | Server → DB/Cache Flow                                                            | Server → Recipient Flow   | Notes / multi-device / offline handling                                |
| ---------------------- | ------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------- | ------------------------- | ---------------------------------------------------------------------- |
| Video Playback         | Client requests manifest URL from API. Player requests video segments from CDN.      | Server checks Redis cache first. On a miss, queries the Metadata DB. Caches data. | Client requests video segments from CDN. | CDN handles multi-device playback (via different resolutions). |

-----

## 6\. Component — Database & Storage Deep Dive

### Metadata Service Database

  * **DB choice**: **Cassandra** (or another distributed NoSQL DB like DynamoDB).

      * **Justification**: Video metadata is primarily read-heavy. The data model is simple (video ID as the primary key). Cassandra's distributed nature and high write/read throughput make it an excellent choice for a massive-scale system. It also provides eventual consistency, which is acceptable for video metadata.

  * **Tables / Collections / Indexes**:

      * **Table Name**: `videos_by_id`

      * **Columns**:

          * `video_id` (uuid, PK)
          * `title` (text)
          * `description` (text)
          * `tags` (set\<text\>)
          * `uploader_id` (uuid)
          * `upload_timestamp` (timestamp)
          * `status` (text)
          * `manifest_url` (text)
          * `duration` (int)
          * `views` (counter)

      * **Primary Key**: `video_id`

      * **Indexes**: None needed on this table as we query by `video_id` which is the PK.

      * **Table Name**: `videos_by_user`

      * **Columns**:

          * `uploader_id` (uuid, PK)
          * `upload_timestamp` (timestamp, clustering key)
          * `video_id` (uuid)
          * `title` (text)
          * `views` (counter)

      * **Primary Key**: `(uploader_id, upload_timestamp)`

      * **Indexes**: Supports `SELECT` queries for a user's uploaded videos, ordered by `upload_timestamp`.

      * **TTL / Retention**: No TTL on video metadata. Videos are permanently stored unless deleted by the user.

      * **Example Queries**:

          * **Write (from Transcoding Worker)**:
            ```cql
            UPDATE videos_by_id SET status = 'ready', manifest_url = '...' WHERE video_id = v-12345;
            ```
          * **Read (for playback)**:
            ```cql
            SELECT manifest_url FROM videos_by_id WHERE video_id = v-12345;
            ```
          * **Read (user's videos)**:
            ```cql
            SELECT video_id, title FROM videos_by_user WHERE uploader_id = u-6789 ORDER BY upload_timestamp DESC;
            ```

  * **Cache Keys (Redis)**:

      * `video:{video_id}`: Stores the JSON blob of the video metadata.
      * `user:{uploader_id}:videos`: Stores a sorted set of `video_id`s with `upload_timestamp` as the score.

  * **Sharding / Partitioning Strategy**:

      * **Partition Key**: `video_id` for `videos_by_id`, `uploader_id` for `videos_by_user`.
      * **Rationale**: `video_id` is a UUID, which guarantees an even distribution of data across nodes, mitigating hotspots. `uploader_id` is also a good partition key for a user's videos.
      * **Hotspot Mitigation**: A highly popular video might have its `views` counter updated frequently, but Cassandra handles high-volume writes well. For search, we use a separate search index (Elasticsearch) to avoid scatter-gather queries.

  * **Data Model Trade-offs**:

      * **Denormalization**: We denormalize the video metadata by creating a separate `videos_by_user` table. This allows us to answer the query "get all videos for a user" efficiently without a costly cross-node join or index lookup.
      * **Consistency**: We use eventual consistency. A newly uploaded video might not appear in search results immediately, which is an acceptable trade-off for performance.

-----

## 7\. Caching Strategy

  * **Layers**:

    1.  **Browser Cache**: Caches static assets (JS/CSS) and small video segments.
    2.  **CDN**: The most critical layer. Caches video manifests and segments globally. Serves the vast majority of video playback traffic.
    3.  **App-level Cache (Redis)**: Caches frequently accessed video metadata (manifest URLs, titles, view counts).
    4.  **DB-level Cache**: A local in-memory cache within each service instance (e.g., Guava cache in Java) for the most recently accessed items.

  * **Caching Patterns**:

      * **Cache-Aside**: The recommended pattern for the app-level cache. The application code first checks the cache. If a miss, it fetches from the DB and writes to the cache. This ensures data consistency.
      * **Write-Through**: Used for the CDN. When a new video manifest is ready, the Metadata Service "pushes" an update or invalidates the CDN cache for that video.

  * **Eviction Policy**:

      * **Redis**: **LRU (Least Recently Used)**. Hotter items remain in the cache.
      * **TTL**: A reasonable starting point for metadata is **1 hour**. For a popular video, the cache entry will be continuously refreshed.

  * **Stampede Protection**:

      * A **thundering herd** problem occurs when a cache item expires, and many concurrent requests for the same key hit the database.
      * **Solution**: Use a distributed lock (e.g., `SETNX` in Redis). The first request acquires the lock, fetches data from the DB, and populates the cache. Subsequent requests for the same key wait for a short period and then retry the cache read.

-----

## 8\. Messaging & Eventing

  * **Messaging System**: **Kafka**

      * **Justification**: High-throughput, persistent, and scalable. It decouples the various services, provides a robust retry mechanism, and supports multiple consumer groups for different use cases.

  * **Kafka Topics**:

      * `transcoding_jobs`:
          * **Message Schema**: `{"video_id": "...", "uploader_id": "...", "s3_key": "..."}`
          * **Partition Key**: `video_id` (ensures all messages for a single video go to the same partition, simplifying processing order if needed).
          * **Retention**: 7 days.
      * `video_metadata_updates`:
          * **Message Schema**: `{"video_id": "...", "event_type": "upload", "title": "..."}`
          * **Partition Key**: `video_id`
          * **Retention**: 1 day.

  * **Asynchronous Operations**:

      * Video transcoding is the primary asynchronous operation. The upload service responds to the client immediately, and the transcoding happens in the background.
      * Search index updates are also asynchronous. The Metadata Service publishes a message to a topic, and the Search Service consumes it to update its index.

  * **Consumer Group Patterns**:

      * **Transcoding Workers**: A single consumer group for `transcoding_jobs`. All workers are part of this group, and Kafka ensures each message is processed by only one worker. This pattern is for a 1:1 job distribution.
      * **Fan-out**: A new topic for a different consumer group can be created if multiple services need to act on the same event (e.g., both the Search Service and a Recommendation Service need to know about a new video).

  * **Idempotency & Deduplication**:

      * **Idempotency Key**: For each message published to Kafka, a unique `request_id` is included.
      * **Transcoding Worker**: A worker maintains a record (e.g., in a DB or a simple in-memory set with persistence) of processed `request_id`s. Before processing a message, it checks if the `request_id` has been processed. This prevents duplicate processing if a message is redelivered.

-----

## 9\. Scalability, HA & Operations

  * **Autoscaling Strategy**:

      * **API / Metadata Service**: Scale based on CPU utilization or request QPS.
      * **Transcoding Workers**: Scale based on the number of messages in the `transcoding_jobs` Kafka topic. High queue length implies a need for more workers.

  * **Multi-AZ / Multi-Region**:

      * Deploy all services in at least three Availability Zones (AZs) within a region for high availability.
      * For a global service, deploy to multiple regions. Use a global CDN for low-latency delivery. Databases should be replicated asynchronously across regions.

  * **Data Backup / Restore**:

      * S3 handles data durability and replication automatically.
      * Cassandra has built-in snapshots and tools like `nodetool snapshot` for backups.
      * Regularly test the restore process from backups.

  * **Monitoring & Tracing**:

      * **Metrics**:
          * **RED metrics**: Request rate, error rate, and duration for all services.
          * **Kafka Lag**: Monitor the consumer group lag on the `transcoding_jobs` topic. High lag means workers are not keeping up.
          * **Cache Hit Ratio**: Monitor the Redis cache hit ratio. A low ratio indicates ineffective caching.
      * **SLOs**: Define Service Level Objectives for critical paths, e.g., "99% of video playback requests must have a latency under 500ms."

  * **Runbooks**:

      * **Kafka Lag Spike**: Check if a new deployment has broken the worker consumer. Check worker health and logs. Scale up the worker pool.
      * **Redis Partition Fail**: The system should be able to fail over to the DB. Alerts should trigger the recreation of the failed Redis node.

-----

## 10\. Security, Compliance & Privacy

  * **Authentication & Authorization**:

      * **User Auth**: Use OAuth2/OIDC. Clients get a JWT from an Identity Provider (IdP) service. The API Gateway validates the JWT.
      * **Service-to-Service Auth**: Use mTLS (mutual TLS) where each service has a certificate. The API Gateway and other services verify the client's certificate.

  * **Row/Field-Level ACLs**:

      * Video metadata in the database should have an `uploader_id` field. The Metadata Service must verify that the `uploader_id` in the request matches the user ID from the authenticated token before allowing update/delete operations.

  * **Compliance Requirements (GDPR/CCPA)**:

      * **Right to Deletion**: When a user account is deleted, a Kafka event (`user_deleted`) can be published. Services (Metadata, etc.) consume this event to purge user-related data.
      * **Right to Export**: A service can be built to export all a user's data to a file.

  * **Input Sanitization**:

      * All user-provided inputs (title, description, tags) must be sanitized to prevent XSS attacks.

-----

## 11\. Cost, Trade-offs & Future Roadmap

  * **Main Cost Drivers**:

    1.  **CDN Egress**: This is the single largest cost. Optimizing video encoding, using more efficient codecs (e.g., AV1), and leveraging multi-CDN strategies can reduce this.
    2.  **Object Storage (S3)**: The cost of storing petabytes of video data.
    3.  **Transcoding Compute**: The cost of running a large fleet of worker instances.

  * **Trade-offs**:

      * **Consistency vs. Freshness**: We chose eventual consistency for search results. A newly uploaded video might not appear in search for a few minutes. This is a good trade-off for the scalability of the search system.
      * **Memory vs. Latency**: The more data we cache in Redis, the better the read latency, but the higher the cost of the cache. We balance this by caching only the most critical and frequently accessed metadata.
      * **Fan-out vs. Pull**: For real-time updates (e.g., live streaming), a push model (WebSocket) is better. For video streaming, a pull model (the client pulling segments from the CDN) is more efficient and scalable.

  * **Future Roadmap**:

      * **Live Streaming**: Add a live video ingestion pipeline and real-time transcoding.
      * **Monetization**: Implement an ad-serving platform.
      * **Recommendation Engine**: Build a more sophisticated machine learning-based recommendation system to increase user engagement.
      * **Advanced Analytics**: Provide creators with detailed analytics on their video performance.
  
---

Based on the provided upload flow, here is a detailed breakdown of the P0 - Upload Flow, with an emphasis on the functional deep dive requirements.

-----

### 5\. Functional Requirement Deep Dives: Video Upload

**Components Involved**: Client, Load Balancer, API Gateway, Upload Service, Metadata Service, Blob Storage (e.g., S3), Upload Table (in a database), Messaging System (e.g., Kafka), Transcoding Service.

#### 1\. Request Flow from Client to Server

The user initiates a video upload via their client application.

1.  **Client initiates upload**: The user's client app makes a `POST` request to `https://api.youtube.com/api/v1/videos/upload`. The request body contains essential video metadata like `fileName`, `fileSize`, and `mimeType`.
2.  **Request Routing**: The request first hits a **Load Balancer** and then the **API Gateway**. The API Gateway handles authentication and rate-limiting before routing the request to the **Upload Service**.
3.  **Upload Service logic**:
      * The **Upload Service** receives the request.
      * It generates a unique `videoId` and creates an initial entry for the video in the **Metadata DB** with a `status` of 'pending\_upload'.
      * It then interacts with the **Blob Storage** (S3) to initiate a `CreateMultipartUpload` request. S3 responds with a unique `uploadId`.
      * The Upload Service divides the video file into chunks (e.g., 5MB each) and generates a pre-signed URL for each chunk using the `uploadId` and a unique `chunkId`. These URLs grant temporary, secure access to upload a specific part of the file directly to S3.
      * The service also creates an entry in a dedicated **Upload Table** to track the status of each chunk for this upload.
      * Finally, the Upload Service returns the `uploadId` and the list of pre-signed URLs back to the client.
4.  **Client-S3 Direct Upload**: The client application now uploads each video chunk directly to S3 using the provided pre-signed URLs. This bypasses the backend services, preventing them from becoming a bottleneck due to high network traffic.
5.  **Chunk Completion**: For each successful chunk upload, S3 returns an `ETag`. The client collects a list of `ETags` and their corresponding `PartNumbers`.
6.  **Finalize Upload**: Once all chunks are uploaded, the client sends a `CompleteMultipartUpload` request to the Upload Service, providing the `uploadId` and the list of `ETags`.
7.  **Upload Service finalization**: The Upload Service forwards this `CompleteMultipartUpload` request to S3. S3 validates the chunks and merges them into a single, complete video object. The Upload Service then updates the status of the video in the Metadata DB to 'uploaded'.

#### 2\. Data Storage and Retrieval

  * **Databases**:

      * **Metadata DB**: A document store like **MongoDB** or a scalable relational DB like **PostgreSQL** can be used.
          * **Table/Collection Name**: `videos`
          * **Schema**:
            ```json
            {
              "id": "v-12345678",      // Primary Key
              "authorId": "user-abcde", // Index for per-user queries
              "title": "My Awesome Video",
              "description": "...",
              "status": "pending_upload", // 'pending_upload', 'uploaded', 'processing', 'ready', 'failed'
              "uploadTimestamp": ISODate(),
              "manifestUrl": null,     // Will be filled after transcoding
              "size": 524288000,
              "uploadId": "xyz...",
              "chunkStatus": [
                {"chunkId": 1, "etag": "...", "status": "uploaded"},
                // ...
              ]
            }
            ```
              * **Indexes**: An index on `authorId` is crucial for retrieving all videos uploaded by a specific user.
              * **Purpose**: Stores the current state of the video, from upload initiation to transcoding completion.

  * **Cache**: Caching is not a primary concern during the upload phase, as it is a write-heavy operation.

#### 3\. Delivery to Other Clients / Devices

This is a write-path flow, so there is no immediate delivery to other clients. The flow is asynchronous. The final status update of a video to 'ready' triggers the video's availability for playback.

#### 4\. Edge Cases and Failure Handling

  * **Client Upload Failure**: If the client fails to upload all chunks (e.g., due to a lost connection), the upload remains incomplete. S3 has a lifecycle policy to automatically delete incomplete multipart uploads after a specified period (e.g., 7 days) to save storage costs.
  * **Failed Chunk Upload**: If a specific chunk upload fails (e.g., network error), the client can simply retry that single chunk. The pre-signed URL is temporary, but the client can request a new one for a specific chunk if needed.
  * **Backend Failure**: If the Upload Service goes down after a multipart upload is initiated, the `uploadId` is stored in the database. When the service recovers, it can use the `uploadId` to check the status of the upload directly with S3.
  * **Idempotency**: The `CompleteMultipartUpload` request from the client to the Upload Service should be idempotent. If the client retries the request, the service should check if the video's status is already 'uploaded' and simply respond with success, without attempting to re-finalize the upload.

#### 5\. Summary Table

| Functional Requirement      | Client → Server Flow                                                                                                                                                             | Server → DB/Cache Flow                                                                                     | Server → Recipient Flow | Notes / multi-device / offline handling                                                                                                                            |
| --------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- | ----------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Video Upload (Multipart)    | 1. Client POSTs metadata to Upload Service. 2. Service returns pre-signed URLs. 3. Client uploads chunks directly to S3. 4. Client POSTs final `ETags` to Upload Service. | 1. Service creates video entry in Metadata DB. 2. Service updates chunk status in Upload Table. 3. Service updates video status to 'uploaded'. | N/A (async process)     | The multipart upload is robust to network interruptions. The client can resume by re-requesting pre-signed URLs for failed chunks. The process is asynchronous and non-blocking. |

---

Once a video is uploaded to S3, the transcoding service takes over to convert the raw video file into multiple formats and resolutions. This process ensures the video can be streamed efficiently to different devices and network conditions.

-----

### 5\. Functional Requirement Deep Dives: Transcoding Service

**Components Involved**: Transcoding Service (Workers), Kafka (Transcoding Queue), S3 (Raw & Transcoded buckets), Metadata Service.

#### 1\. Request Flow from S3 to Transcoding Service

1.  **Event Trigger**: Upon a successful video upload to the S3 `raw-videos` bucket, an S3 event notification is configured to publish a message to the **Kafka `transcoding_jobs` topic**.
2.  **Message Payload**: The message payload contains the necessary information for the transcoding worker, such as:
    ```json
    {
      "videoId": "v-12345678",
      "s3Key": "raw-videos/v-12345678.mov",
      "uploaderId": "user-abcde"
    }
    ```
3.  **Worker Consumption**: The **Transcoding Workers**, which are part of a Kafka consumer group, consume this message. Each worker is a dedicated process (e.g., an EC2 instance or a container) that is ready to perform the compute-intensive transcoding task.

#### 2\. Transcoding Process in Detail

1.  **Download from S3**: A worker consumes a message from the queue and downloads the raw video file from the `raw-videos` S3 bucket to its local storage.
2.  **Transcoding with FFmpeg**: The worker uses a tool like **FFmpeg** to transcode the video. It generates multiple video files in different resolutions (e.g., 144p, 360p, 720p, 1080p, 4K). For each resolution, it also creates an **Adaptive Bitrate Streaming (ABS)** format like HLS (HTTP Live Streaming) or MPEG-DASH.
      * **HLS**: The worker segments the video into small `.ts` (transport stream) chunks and creates a `.m3u8` manifest file that lists these chunks.
      * **MPEG-DASH**: Similarly, it creates `.mpd` (Media Presentation Description) manifest file and segments.
3.  **Upload to S3**: The worker uploads all the transcoded video segments and their manifest files to a separate S3 bucket, `transcoded-videos`. The file structure is organized by video ID and resolution, for example, `transcoded-videos/v-12345678/hls/1080p.m3u8`.
4.  **Metadata Update**: After successful transcoding and upload, the worker calls the **Metadata Service** to update the video's status in the database to 'ready'. It also stores the S3 URL of the main manifest file (e.g., `s3://transcoded-videos/v-12345678/hls/master.m3u8`).
5.  **Clean-up**: The worker deletes the local raw and transcoded files to free up disk space for the next job.

#### 3\. Why is Transcoding Necessary?

  * **Device Compatibility**: Different devices and platforms support various video codecs and containers. Transcoding converts the video into a universally playable format.
  * **Adaptive Bitrate Streaming (ABS)**: By creating multiple resolutions, the system enables ABS. The video player can automatically switch between resolutions based on the user's network speed. A fast connection gets 1080p, while a slow one gets 360p, ensuring a smooth, buffer-free viewing experience.
  * **Bandwidth Optimization**: Transcoded files are smaller and more efficient for streaming. The original raw file is often very large and unsuitable for direct streaming.

#### 4\. Edge Cases and Failure Handling

  * **Worker Failure**: If a transcoding worker crashes, the Kafka message remains uncommitted. Kafka will re-deliver the message to another available worker, ensuring the job is not lost.
  * **Corrupt Video File**: If the raw video file is corrupt, the FFmpeg process will fail. The worker should catch this error and move the message to a **Dead Letter Queue (DLQ)** after a few retries, logging the failure for manual inspection.
  * **Idempotency**: Workers use the `videoId` to check the current status in the Metadata DB. If a video is already marked 'ready', a worker can safely skip the job, preventing duplicate transcoding.

---

Your P0 transcoding flow is correct and well-structured. It covers the essential steps from event consumption to final metadata update. To improve it, we can add more technical depth, address your specific questions about streaming, HLS structure, and audio/subtitles, and highlight key design choices for a production-ready system.

-----

### 1\. Improvements to the Transcoding Flow

Your current flow is a solid foundation. Here's how you can make it more robust and detailed:

  * **Idempotency & Concurrency**: Explicitly mention how you handle **idempotency**. What happens if the same event is delivered twice? The worker should use the `videoId` to check the current video status in the metadata database. If it's already `ready` or in `processing` by another worker, the current worker should safely exit. This prevents redundant work and data corruption.
  * **Error Handling and Retries**: Detail the retry strategy. When a transcoding job fails (e.g., corrupted file, worker crash), the Kafka consumer should not commit the message. Kafka's consumer group will then re-deliver the message to a different worker. After a few failed attempts, the message should be moved to a **Dead Letter Queue (DLQ)** for manual inspection. This makes the system resilient to transient failures.
  * **Instrumentation**: Mention the importance of **monitoring**. What metrics would you collect? Examples include transcoding job success/failure rates, duration per job, number of messages in the Kafka queue, and worker resource utilization (CPU/memory). This allows you to set up alerts and auto-scaling rules.

### 2\. Do I have to download the entire raw file? Can we stream from S3?

Yes, for most video transcoding tools like **FFmpeg**, you typically need to download the entire raw file before you can begin processing it. This is because FFmpeg needs to parse the entire file's header and metadata to understand the stream properties (codec, duration, frames per second, etc.) and to seek to specific points for accurate segmenting and processing.

While some tools and cloud services offer partial file access, the most reliable and common approach for a high-volume transcoding service is to download the file to the local disk of the worker instance. This has a few advantages:

  * **Performance**: Local disk access is much faster and more reliable than repeated network reads from S3.
  * **Simplified Logic**: It simplifies the worker's logic, as it's just reading from a local file, not managing complex stream positions and network retries.

However, for very large files, a hybrid approach could be considered where the worker streams the raw file into a pipe that feeds directly into FFmpeg, without waiting for the full download. But this adds complexity and is generally not the first choice for a simple, robust design.

### 3\. How HLS files look like (duration), and how are they created?

The HLS format consists of a **master manifest file** and multiple **media manifest files**, each corresponding to a different resolution.

  * **Master Manifest File (`master.m3u8`)**: This file is the entry point for the video player. It lists all available video streams (resolutions) and links to their respective media manifest files. This allows the player to choose the best quality based on the user's network.

    ```m3u8
    #EXTM3U
    #EXT-X-VERSION:3

    #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=426x240,CODECS="avc1.42c01e,mp4a.40.2"
    144p/video.m3u8

    #EXT-X-STREAM-INF:BANDWIDTH=1500000,RESOLUTION=854x480,CODECS="avc1.4d401f,mp4a.40.2"
    360p/video.m3u8

    #EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1280x720,CODECS="avc1.4d401f,mp4a.40.2"
    720p/video.m3u8
    ```

  * **Media Manifest File (`video.m3u8`)**: This file lists the individual video segments (`.ts` files) for a specific resolution. Each segment is typically a short duration, like **6 seconds**.

    ```m3u8
    #EXTM3U
    #EXT-X-VERSION:3
    #EXT-X-TARGETDURATION:6
    #EXT-X-MEDIA-SEQUENCE:0
    #EXT-X-PLAYLIST-TYPE:VOD
    #EXTINF:6.000,
    segment00000.ts
    #EXTINF:6.000,
    segment00001.ts
    #EXTINF:6.000,
    segment00002.ts
    #EXT-X-ENDLIST
    ```

The worker creates all these files in **one single job**. FFmpeg is run with a single command that outputs multiple resolutions and manifest files simultaneously. The process is: fetch the raw file, process it, and output all the required files to the local disk. After all files are generated, they are uploaded together to the `transcoded-videos` S3 bucket.

### 4\. How are audio and subtitles handled?

In HLS, audio and subtitles are often handled as separate streams.

  * **Audio**: The master manifest can have separate audio entries for different languages or quality levels. The HLS player can then select the audio stream independently from the video. In the example above, the `CODECS` field includes `mp4a.40.2` for AAC audio, which means the audio is packaged with the video segments. Alternatively, you can have a separate audio manifest:

    ```m3u8
    #EXTM3U
    #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="English",DEFAULT=YES,URI="audio/eng_audio.m3u8"
    #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="Spanish",URI="audio/spa_audio.m3u8"
    ```

  * **Subtitles**: Subtitles are also a separate media type. They can be included in the master manifest as `EXT-X-MEDIA` tags with `TYPE=SUBTITLES`. They are typically delivered as `.vtt` or `.ttml` files. The player can then fetch and display them based on user selection.

The transcoder's job is to create these separate audio and subtitle manifest files and segments alongside the video. This modular approach provides flexibility and allows the player to load only what is needed, which is crucial for a smooth user experience.


---

Your description of the video streaming process is correct and provides a clear high-level overview. To make it more detailed and robust for a system design, we can expand on the key components and add more technical depth.

***
## High-Level Flow Breakdown

1.  **Client Request**: The client (video player) initiates a `GET /api/v1/videos/{videoId}/stream` request. This is the **read path** for video playback.
2.  **API Gateway & Metadata Service**: The request goes through the API Gateway, which handles authentication and routing. It is then forwarded to the **Metadata Service**.
3.  **Caching Strategy**: The Metadata Service is designed with a **cache-aside pattern**.
    * It first checks the **Redis cache** using the key `video:{videoId}:manifest_url`. This is the most crucial optimization for the read path, as most video playback requests will be served directly from the cache.
    * **Cache Hit**: If the manifest URL is found in Redis, the service immediately returns it, providing very low latency.
    * **Cache Miss**: If not found, the service queries the **Video Table** (in a database like Cassandra or MongoDB) to retrieve the `manifestUrl`. After retrieving the URL, it populates the Redis cache to serve future requests faster.
4.  **Manifest URL Return**: The Metadata Service returns a `200 OK` response with the manifest URL. This URL is a pointer to the video's master manifest file on the CDN.
5.  **CDN-based Playback**: The client's video player receives the manifest URL and makes subsequent requests directly to the **CDN** (Content Delivery Network).
    * The player first requests the **master manifest file (`master.m3u8`)** from the CDN.
    * The player parses the master manifest to get a list of all available resolutions (e.g., 1080p, 720p).
    * Based on network conditions and user settings, the player chooses a specific resolution's manifest (e.g., `720p/video.m3u8`).
    * The player then requests the individual video segments (`.ts` or `.mp4` chunks) listed in the media manifest from the CDN. The CDN serves these chunks from a cache, minimizing latency and server load.

***
## Improvements and Deeper Dive

### 1. Caching & Performance
* **Cache Keys and TTL**: A good starting point for the manifest URL cache key is `video:{videoId}:manifest_url`. A TTL (Time-To-Live) of 1 hour is a good balance between freshness and performance.
* **Cache Stampede Protection**: A key issue to address is the **thundering herd problem** (also known as cache stampede). When a popular video's manifest URL expires from the cache, thousands of concurrent requests might hit the database simultaneously. To prevent this, implement a distributed lock (e.g., `SETNX` in Redis) or a mutex around the cache-miss logic. The first request acquires the lock, fetches from the DB, and populates the cache. Subsequent requests for the same key wait briefly or return a stale value until the cache is populated.
* **CDN Caching**: The CDN is the most critical layer for streaming. It serves the video segments directly from its edge locations. The S3 bucket acts as the **origin server** for the CDN. CDN cache policies (e.g., setting appropriate `Cache-Control` headers) are crucial for minimizing origin hits and costs.

### 2. Data Model & Queries
* **Database Table**:
    * **Table Name**: `videos` (in Cassandra/DynamoDB)
    * **Columns**: `video_id` (PK), `manifest_url`, `title`, `description`, etc.
* **Database Query**: The query for a cache miss would be very simple and fast, optimized for a direct lookup by primary key.
    * **CQL Query (Cassandra)**: `SELECT manifest_url FROM videos WHERE video_id = 'v-12345';`

### 3. Latency & User Experience
* The goal is a low **Time To First Byte (TTFB)** for the manifest URL and subsequent low latency for the video segments.
* The CDN is key here. By having a global network of edge servers, the video content is physically closer to the user, significantly reducing latency and buffering.
* **Client-Side Player Logic**: The video player itself is a critical part of the system. It handles the adaptive bitrate switching, selecting the optimal resolution based on real-time network conditions to ensure a smooth, uninterrupted playback experience.


