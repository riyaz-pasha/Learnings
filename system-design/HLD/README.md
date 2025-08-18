# ✅ HLD Interview Prep – Markdown Checklist (5 Weeks)

---

## 🗓️ **Week 1 – Foundational & Classic HLD Systems**

| Day   | System                  | ✅ Status | Prompt                                                                                                                            |
| ----- | ----------------------- | -------- | --------------------------------------------------------------------------------------------------------------------------------- |
| Day 1 | URL Shortener (TinyURL) | \[ ]     | `Design a URL shortening service like TinyURL. Include API design, DB schema, hashing strategy, scalability, and custom aliases.` |
| Day 2 | Pastebin                | \[ ]     | `Design a Pastebin-like service. Support pasting code/text, public/private links, expiry, storage, and scalability.`              |
| Day 3 | Rate Limiter            | \[ ]     | `Design a scalable rate limiting system for APIs using sliding window or token bucket algorithm. Include multi-instance support.` |
| Day 4 | Notification System     | \[ ]     | `Design a unified Notification System for email, SMS, and push. Handle retries, failure handling, bulk send, and extensibility.`  |
| Day 5 | Review & Diagrams       | \[ ]     | `Create component + sequence diagrams for URL Shortener and Notification System. Discuss trade-offs in availability vs latency.`  |

---

## 🗓️ **Week 2 – Data-Heavy & Realtime Systems**

| Day   | System                                       | ✅ Status | Prompt                                                                                                                               |
| ----- | -------------------------------------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| Day 1 | News Feed System (Twitter/Facebook)          | \[ ]     | `Design a news feed system like Facebook. Include fan-out vs pull model, ranking, deduplication, and real-time updates.`             |
| Day 2 | Chat System (WhatsApp)                       | \[ ]     | `   .` |
| Day 3 | Real-Time Collaborative Editor (Google Docs) | \[ ]     | `Design a collaborative document editing tool like Google Docs. Support real-time edits, consistency, operational transforms.`       |
| Day 4 | Search Autocomplete                          | \[ ]     | `Design a search autocomplete system. Support prefix search, ranking by popularity, and incremental updates.`                        |
| Day 5 | Review & Scale Plan                          | \[ ]     | `Compare fan-out vs fan-in feed systems, discuss consistency in collaborative editors, and sketch scaling plan for chat backend.`    |

---

## 🗓️ **Week 3 – Storage, Files & Media Systems**

| Day   | System                              | ✅ Status | Prompt                                                                                                                    |
| ----- | ----------------------------------- | -------- | ------------------------------------------------------------------------------------------------------------------------- |
| Day 1 | File Storage (Dropbox/Google Drive) | \[ ]     | `Design a cloud file storage system like Dropbox. Include file sync, metadata storage, deduplication, and sharing.`       |
| Day 2 | YouTube / Video Streaming           | \[ ]     | `Design a video streaming service like YouTube. Include upload, transcoding, CDN usage, metadata indexing, and playback.` |
| Day 3 | Image Hosting (Imgur/Instagram)     | \[ ]     | `Design an image hosting system. Include upload, compression, CDN, metadata search, and resizing.`                        |
| Day 4 | Large File Upload                   | \[ ]     | `Design a system for large file uploads with chunking, resumable uploads, integrity checks, and cloud storage support.`   |
| Day 5 | Review & Caching Patterns           | \[ ]     | `Discuss CDN vs origin-server architecture for YouTube, and caching strategies for Dropbox metadata and thumbnails.`      |

---

## 🗓️ **Week 4 – Search, Queues, Analytics, Infrastructure**

| Day   | System                                    | ✅ Status | Prompt                                                                                                                            |
| ----- | ----------------------------------------- | -------- | --------------------------------------------------------------------------------------------------------------------------------- |
| Day 1 | Distributed Message Queue (Kafka-like)    | \[ ]     | `Design a distributed messaging queue like Kafka. Include partitions, brokers, durability, replication, and offset tracking.`     |
| Day 2 | Search Engine (Mini-Google)               | \[ ]     | `Design a web search engine. Include crawling, indexing, ranking, sharding, and caching.`                                         |
| Day 3 | Metrics & Logging System (Prometheus/ELK) | \[ ]     | `Design a scalable monitoring system. Support logs, metrics, dashboards, alerting, storage, and ingestion pipeline.`              |
| Day 4 | Analytics Platform                        | \[ ]     | `Design a user analytics system. Collect events, perform aggregations, and generate dashboards with real-time and batch support.` |
| Day 5 | Review + Tradeoffs                        | \[ ]     | `Discuss Kafka vs RabbitMQ, real-time vs batch analytics, and design tradeoffs in monitoring system reliability.`                 |

---

## 🗓️ **Week 5 – MAANG-Level Composites & Mock Practice**

| Day   | System                         | ✅ Status | Prompt                                                                                                                                  |
| ----- | ------------------------------ | -------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| Day 1 | Instagram-like System          | \[ ]     | `Design a social media platform like Instagram. Include user profiles, photo posts, feed generation, comments, likes, and scalability.` |
| Day 2 | Netflix-like Streaming Service | \[ ]     | `Design a video-on-demand system like Netflix. Cover content ingestion, video encoding, catalog, playback, and recommendations.`        |
| Day 3 | Ride-Sharing App (Uber)        | \[ ]     | `Design a ride-hailing service like Uber. Cover driver location updates, ETA, surge pricing, and dispatch algorithm.`                   |
| Day 4 | E-Commerce System (Amazon)     | \[ ]     | `Design an e-commerce platform. Include product catalog, cart, order, inventory, payments, recommendations, and search.`                |
| Day 5 | Mock Interview / Review        | \[ ]     | `Pick 2 systems from above. Run mock interviews with constraints: time, failure scenarios, scale. Focus on communication + diagrams.`   |

---

## 💡 Bonus LLM Prompts for Any System:

* `Give me the component, database, and sequence diagram for this design.`
* `List trade-offs in CAP theorem for this system and justify your choices.`
* `How would you scale this system to handle 100M users?`
* `What would you change if consistency was more important than latency?`
* `What are bottlenecks in this design and how can we mitigate them?`

---
