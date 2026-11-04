# Real-Time Communication, Fan-Out & Collaboration — HLD System Design Problem Roadmap

This is one of the most important HLD patterns for Google, Meta, Amazon, Uber, LinkedIn and other product-based company interviews.

The central challenge is building systems where events, messages, state changes, or user actions must be delivered to other users or services in real time, often with millions of concurrent connections.

Your examples cover several distinct but interconnected system design patterns:

1. Real-time messaging and communication.
2. Presence and connection management.
3. Fan-out and live event distribution.
4. Collaborative editing and shared state.
5. Real-time data streaming.
6. Multiplayer and synchronized state.
7. Real-time notifications and activity feeds.

I would prepare these as one major HLD pattern, but divide them into subcategories so that you understand the underlying architectural differences rather than memorizing individual designs.

# 1. Complete Problem List

Here is the roadmap I recommend.



Core interview problems

&#x20;These are the problems I would prioritize.



## Category A: Real-Time Messaging & Communication

Focus: WebSockets, message delivery, ordering, persistence, acknowledgments, fan-out, and offline delivery.

[Фон переписки ватсап - Zefirka.club фото и картинки](https://images.openai.com/static-rsc-4/fE-oRz0sImwBtCvd1-HxHENdPb0uMLEPOlzCN9Yoj1LYn_VS_Cvu2pYvaBuJGkYT4sg7Vy3oFLU-l5IRTEnmYicZB3f0uRX03FNwZ5jFmuwErCyg4cKzoJq7mX_m-UTMBen8_KplA13cPoenp1bXxPqRE8eY8QhPo2_cSNqjl_E?purpose=inline)

1\. Design WhatsApp / Messenger

Must do · High priority

- One-to-one and group messaging.
- Online and offline message delivery.
- Message persistence and delivery acknowledgments.
- Read receipts, typing indicators and attachments.
- Message ordering, retries and deduplication.
- End-to-end encryption considerations.

Key challenge: Reliable message delivery at scale, even when recipients are disconnected.

[Slack vs. Discord in 2026: Which Chat App Wins for Gamers and Creators?](https://images.openai.com/static-rsc-4/9v7-k1OAM96qXksaKMGwjqgSYfgciEwSKz57E29AFZKCL6xU1kMa0KVQhglYhjRsRPd3mmmOcoaNAa_IQqf3WwsdmgrtZwt4YSacgF1qKk5W7HJCirxq5TSnG3wtjyP4pg7ehJrlGntRjk_dSOTaUWGC_R56CN_EtcG4V_llhNU?purpose=inline)

2\. Design Slack / Microsoft Teams Messaging

Must do · High priority

- Workspace, channels, direct messages and threads.
- Channel membership and permissions.
- Real-time message distribution to channel subscribers.
- Search, message history and file sharing.
- Mentions, reactions and notifications.
- Large channels with thousands of active members.

Key challenge: Efficiently distributing messages to large groups while supporting persistent history and access control.

[Video na daljavo | Monitor](https://images.openai.com/static-rsc-4/OLpEQVPNqwpqebZDZR0yaoP38Soiq_vFJfVl8HB-TbZF8VpoJ3ZUOn55dD_YLz9vwoAXn3D4raDYfK97n8GRsn0L-oSlIHS0HB2pEYsIvPfhqB8MWJEuGsh8RoYXXU2acs3LxMIndDBd87Rz0GcPXuq6x30LWld207SOIDLwPaE?purpose=inline)

3\. Design Zoom / Google Meet

High priority

- Meeting creation and participant management.
- Real-time audio/video communication.
- Signaling server and session establishment.
- WebRTC, STUN, TURN and ICE.
- Media routing through SFU/MCU.
- Screen sharing, mute/unmute and participant events.

Key challenge: Low-latency media transmission and reliable signaling for large meetings.

4\. Design Discord

High priority

- Servers, channels and communities.
- Real-time text and voice communication.
- Presence and activity status.
- Large group messaging.
- Message fan-out and notification delivery.
- Voice room lifecycle and participant state.

Key challenge: Supporting millions of persistent connections and large communities.

[Cognigy Live Agent | AI-powered Omnichannel Agent Workspace](https://images.openai.com/static-rsc-4/JD4MRf1faUSz6nPMUEiwpqu26XxTDgf5qDxAXo0d1rjcsb0AZklsxR3RtjBGZbPT3Gda-ayXFfYrr7iazVY2EuOdOFtQslsWfQ1UT5-uSmlXWY3ilS8T_iWkN93Fr6BGqKm8Ndp73Mak9i89sAA7S3NC6kzQSRlWzXjDVdy8h74?purpose=inline)

5\. Design Live Customer Support Chat

Additional

- Customer-agent assignment.
- Real-time communication.
- Agent availability and routing.
- Conversation handoff.
- Queue management and SLA tracking.
- Reconnection and conversation recovery.

Key challenge: Combining real-time messaging with intelligent routing and workload distribution.

## Category B: Presence, Connection Management & Notifications

Focus: Maintaining real-time user state, connection lifecycle, heartbeats, and distributed presence.

[Slack可以帮助我们还原什么？ | 人人都是产品经理](https://images.openai.com/static-rsc-4/0yjYPsK73XjdvuCwrz4d2OAPGpINIy6y-9M3U2gbLwrVOhklW0A_QJ7VXnQrDhDBysws_A3O8AKEhPyZDTs1th9eEz79UwdT0epMc1Hcndulqv747VdM_5jaQFho5wbcp_j1M-U3yNg1aJwNofs7-amaKkwgqgIYKsikWp_vQy0?purpose=inline)

6\. Design Online Presence System

Must do · High priority

- Online, offline, away and busy states.
- Heartbeats and connection timeouts.
- Multiple devices per user.
- Distributed presence tracking.
- Presence propagation to friends or contacts.
- Handling server crashes and stale presence.

Key challenge: Maintaining accurate presence without overwhelming the infrastructure with heartbeats and updates.

[How to Hide Message Previews, Read Receipts on Your iPhone - Business Insider](https://images.openai.com/static-rsc-4/OR9uPMhEbC2fgjjxtiL79LjAq2HpdX_zyU6WsBEvnV2g2qsKLzdpNUx8Sc_I0uyMbNWb8O-iV5wHEyKl39iysvJ9_7X7Qe6w4rxiI-zVfDXnSfuhUt9LfYjRDcMHakNIGSl9Q46cAvfrdHz9VzjZWYRXHGpwDBnB4ilG1MJ4wkE?purpose=inline)

7\. Design Typing Indicators & Read Receipts

High priority

- User is typing / stopped typing.
- Message sent, delivered and read states.
- Per-device read positions.
- Expiring transient events.
- Group conversation read receipts.
- State synchronization.

Key challenge: Distinguishing durable events (messages) from ephemeral events (typing status).

[Will the iPhone 15 have Touch ID? Here’s everything we know | TechRadar](https://images.openai.com/static-rsc-4/JxKHzpmNlEZHwPFG1JcxpIV8LcQX6lwF1N8iBAhL2Tzl8b_j699OUzTUS-QN2z_XKCh2y_EHqfXDKIu6t7hDgITR4eeHJnaiN170dXyH7ddj6RmADFsz_3I6M9YSHH_6TlBzFfPHMlC-tQwk4e1XbJQ0mjtktgFuvHOVWPmWuEo?purpose=inline)

8\. Design Real-Time Notification System

Must do · High priority

- In-app real-time notifications.
- Push notifications through APNs and FCM.
- Notification preferences and subscriptions.
- Online versus offline delivery.
- Priority, batching and rate limiting.
- Retry, deduplication and delivery tracking.

Key challenge: Combining WebSocket delivery with asynchronous push notification infrastructure.

[Our CEO, Vinay, just shared an inspiring update about our progress and the incredible support from Jason Calacanis and the LAUNCH team. 🚀  Our dedication to celebrating small wins and being there… | Layerpath](https://images.openai.com/static-rsc-4/XEimqDNmVqWeIJsydnDIUHSSoRAB9lmcJ9uagyzz-VjgNDLQRaxi_9SfJIQI6jq3h4ogR98YAPy6UgD8I7ejw1XT4SgkLCfN3Uly-K98W6LVX-JTQFP8Yabkm7vNa9bF6lA6fAkKcFRRoZv3yVt8XI9Mn3p1RW2IZWzLUajUwSc?purpose=inline)

9\. Design Social Activity Feed / News Feed

Must do · High priority

- Users follow other users or pages.
- New post distribution to followers.
- Fan-out-on-write versus fan-out-on-read.
- Celebrity accounts with millions of followers.
- Feed ranking and pagination.
- Real-time feed updates.

Key challenge: Balancing write amplification, read latency and highly skewed follower distributions.

[Step-by-step guide to contributing on GitHub](https://images.openai.com/static-rsc-4/TXuF2F_hpL5ssIR8iovneL3BnT_lF-jCKPENsZ-Jv6aR08vKZqYED4-pYhkTLr55z4w3MdvR8lVDZiPq7k6rAgs27vIO8_ul63UNOIPcDUlZJr1VjBBOh8CleeWrBb35NnU3f2P_4k3QhIZ4i3HO4v7YSMHOszOkjvI-fEgvKLc?purpose=inline)

10\. Design Live Comments / Collaborative Discussion

Additional

- Comments on posts, documents or pull requests.
- Real-time comment updates.
- Threaded replies and reactions.
- Comment editing and deletion.
- Moderation and permissions.
- Notification fan-out.

Key challenge: Real-time updates combined with persistent, ordered comment history.

## Category C: Live Data Streaming & Event Broadcasting

Focus: High-throughput event ingestion, publish-subscribe, stream processing, and low-latency distribution.

[Baixe UniScore - O aplicativo de visualização de pontuação online mais rápido](https://images.openai.com/static-rsc-4/jpf7kUviMh0nqoLupiKzahEnMH9U3agQMclBoTZlsz1QVdZtKor3P0MoioDd0oJRzZMKVrJoUe8UjBOfJasmJ-Gp5EYXTxAOk44TyWnjFtrXgNn1YG9iC4if_H3BNyS8vue0DIq_gfdn18d6k-AC5Y4v-WB3j8sUtcnBrNXHOHA?purpose=inline)

11\. Design Live Sports Score System

Must do · High priority

- Ingest events from multiple data providers.
- Score and match state updates.
- Distribute updates to millions of subscribers.
- Match subscriptions and personalized alerts.
- Event ordering and duplicate handling.
- Historical match timelines.

Key challenge: Broadcasting high-volume updates to massive audiences with minimal latency.

[How to Invest? More Than Ever, It Depends on Who You Are - WSJ](https://images.openai.com/static-rsc-4/12FgZoddzSg6rr1T5czzranCGzaUoq8oh4sesTlg1MC8qB6bZHrfUdvT-7P9XQFpiWnuuodx8TEWotAgz7i2jN831A_kXSJzZwnlpzkqVxISB4d02kEDqiFKzNHKxk9S2Pzdv3dDs3-Abt-hCNRY8EJqv9QGPxNwXgun-6cgv64?purpose=inline)

12\. Design Real-Time Stock Market Data Streaming

Must do · High priority

- Ingest market price updates.
- Stream data to subscribed clients.
- Symbol-based subscriptions.
- WebSocket connection management.
- Data aggregation and compression.
- Backpressure and slow consumer handling.

Key challenge: High-throughput streaming with low latency and controlled data delivery rates.

[Crypto Trading Dashboard UI/UX Design by Masum Billah on Dribbble](https://images.openai.com/static-rsc-4/aJqpTaemwSYXdkNDNpzgZfPLuchCrH7Giiv1d3z4OUoXPbJle-v38B3nauMJXe_bGYrzAQU1zalj49y8N428OdP-GiDc7787m2ulESnLsN8XwAvGGw2D_HvERy-xn7BFiX4JgnHHpTIjkP3aj4hvtLPiH9I3_jz_-BXrxEarPu4?purpose=inline)

13\. Design Cryptocurrency Exchange Live Market Data

Advanced

- Live order book updates.
- Trade execution events.
- Price ticker distribution.
- Market depth and order book snapshots.
- Sequence numbers and event recovery.
- Separate market-data and order-execution pipelines.

Key challenge: Maintaining a consistent event sequence while supporting extremely high-throughput subscribers.

14\. Design Real-Time Analytics Dashboard

Additional

- Continuous event ingestion.
- Real-time aggregation.
- Time-window-based metrics.
- Live charts and dashboards.
- Multiple dashboard subscribers.
- Historical versus real-time data.

Key challenge: Streaming aggregation and serving frequently changing metrics efficiently.

15\. Design Real-Time Location Tracking (Uber / Delivery Tracking)

High priority

- Continuous GPS updates.
- Driver and customer subscriptions.
- Location ingestion and distribution.
- Geospatial indexing.
- Update frequency and throttling.
- Stale location detection.

Key challenge: Efficiently processing frequent location updates and distributing only relevant updates to interested clients.

## Category D: Collaborative Editing & Shared State

Focus: Concurrent modifications, conflict resolution, synchronization, consistency, and shared application state.

[Real time collaborative editing in your web application | by Aditya Singh | Medium](https://images.openai.com/static-rsc-4/amwIIBXhE3nNe63eMt8vF8d97FvvuOUBKJA663mHKltKzcLTieJrlEd-IfQQr62iGyQDAxicwyO0DEwthqCDCUeZDjSqcWnbQ1NWT8TGrema8tHWQ6iU48B8cEEc6DrCn-Q_2FEJMx_DAlijMN8cHuoExgr2_ZAyQkMhOGBMqhc?purpose=inline)

16\. Design Google Docs (Collaborative Document Editing)

Must do · Very high priority

- Multiple users editing the same document.
- Real-time operation propagation.
- Concurrent edit conflict resolution.
- Operational Transformation (OT) or CRDT.
- Cursor and selection synchronization.
- Document versioning and recovery.
- Offline editing and synchronization.

Key challenge: Ensuring concurrent edits converge to a consistent document state.

[Collaborating in Real-Time with Figma: Real-Time Collaboration and Commenting Tools](https://images.openai.com/static-rsc-4/RNOZeqS0HyaG2cRCq9kQLncF_k8hFKh8DskXHfUpdRc2neiyv6-zIfWzpcFLSeCLWZnxeeNqRcwFIaGL8dm2Usw9XnQ9T0uXU0myFqsbdxnho-HsnkDe9dpd-D2-SAPypurfjZuerPJVeXjk_Aeu7QOOG64Iv3HUL6oRMmI07Nw?purpose=inline)

17\. Design Figma (Collaborative Design Tool)

Must do · Very high priority

- Shared design documents.
- Layers, shapes and object modifications.
- Concurrent editing.
- Live cursor and selection updates.
- Object-level permissions.
- Version history and undo/redo.
- Large document synchronization.

Key challenge: Synchronizing structured graphical state rather than simply synchronizing text.

[Ein Vision Board online erstellen | Miro](https://images.openai.com/static-rsc-4/YXeS5DU85QKck7MiqxibBvPUftzeJV_7L2Y-LoXc70O-NFCvMUveBIsgOqaFExJoKvYMV1NrIHLnYJm3CzUZEExdr6ZZ8U5QmQR8GHQFCkmX5Rr1Ub0wUf000-D5e_BnJi-AwEnWQwVAymfyMrbsXWpIlInit3A3DQbC5gIyk4I?purpose=inline)

18\. Design Miro / Collaborative Whiteboard

High priority

- Shared canvas and drawing objects.
- Real-time drawing and object movement.
- Multiple simultaneous users.
- Object synchronization.
- Conflict resolution and undo.
- Large canvas rendering and state recovery.

Key challenge: High-frequency shared-state updates and efficient synchronization of canvas changes.

[Document Collaboration Made Easy with Notion's Tracking Features](https://images.openai.com/static-rsc-4/N_o9MzRcLImt_6MZ79k0dtUjjV5T8jjQS6gO0eRCMmL44AJ3LePj-hBE-hbubUhGntKtkx5AnCTD8To-ZV18Ze9LSNcxad-O0XzMU4GkZRlk9qRTOM4ERr3MztR_K-_oHlZQV8n1HTGWGsWZHIRXl5EqjjU0S5H3XnL37j2hbrs?purpose=inline)

19\. Design Notion / Collaborative Workspace

Advanced

- Pages, blocks, databases and nested structures.
- Real-time multi-user editing.
- Block-level synchronization.
- Comments and mentions.
- Permissions and sharing.
- Version history and offline recovery.

Key challenge: Synchronizing a complex hierarchical data model across multiple users.

[실시간 협업 소프트웨어의 모든 것: 원격 팀을 위한 완벽한 가이드](https://images.openai.com/static-rsc-4/JdikrPN5td30QaanXZl4LJtaamg3d-X8rx5r9K0GBE2BSJTCTEpijitdq0H5cwgs1cSYLZK_cygpJZWyyyb6wXG34fQVmV2kehpUFMIW6i8xzijbdj316GQ-SZfHgzkAm7tyHALSsxt_VUAnxsCn3oDqIDNf7a8zXS0soUG0NW8?purpose=inline)

20\. Design Collaborative Project Management Tool (Trello / Jira)

Additional

- Shared boards and tasks.
- Real-time card movement.
- Assignment and status updates.
- Comments and activity streams.
- Concurrent modifications.
- Notification and activity fan-out.

Key challenge: Synchronizing shared business objects while preserving authorization and consistency.

## Category E: Multiplayer & Real-Time Interactive Applications

Focus: Low-latency state synchronization, authoritative servers, game rooms, and simulation consistency.

[Fortnite's controversial Battle Royale mode is now live | PC Gamer](https://images.openai.com/static-rsc-4/2k9lhnvxyKTGBvN045jaXS7MPIX8DCe6r4CLsdt8B1DiNxT1JGcxXNLQtol87Lg9U2K3yG9IZ29owRS86xDEzonFxZ-JtyyNP8CwIAkMbX_qOLmfnRM2NwEVyaIw6L1dLDPnU05FtO-3nmsKDSHC3UpCfhUAJTtGg9UPAK0mGVg?purpose=inline)

21\. Design Multiplayer Online Game

Must do · High priority

- Game rooms and matchmaking.
- Player state synchronization.
- Real-time movement and actions.
- Authoritative game servers.
- Tick-based simulation.
- Latency compensation and interpolation.
- Disconnection and reconnection.

Key challenge: Synchronizing rapidly changing game state while minimizing latency and cheating.

[Play Chess Online Free | ACF Echess & Pro Online Chess Tournaments](https://images.openai.com/static-rsc-4/yNHT6mCNttxgrxt0n84u74OlS4IoO9YZenzgKXPDWcDios8Bi7f5dTzvWiC2g4Q5R4O0eB72Zv8naU49cn-b_hzcbWn58mtKFT7ReyADHr89CsXiK0IQiKI6pwAnZT8cpcjPMdR1JsM-ymK3VTNRg4GeBjWX2z-XIIgWaWVbDAk?purpose=inline)

22\. Design Multiplayer Chess / Turn-Based Game

High priority

- Game creation and matchmaking.
- Player turns and move validation.
- Real-time board synchronization.
- Timers and clock management.
- Reconnection and game recovery.
- Move history and consistency.

Key challenge: Maintaining authoritative game state and preventing invalid or duplicate moves.

[Kahoot tunnis: kuidas kasutada mänguliselt ja mõistlikult](https://images.openai.com/static-rsc-4/mxF0IPvmZUZz4YuPX3mIaIHXgBeZP98xBzy3jZ0uoA1xA5sDOf31jzkZBlqfeGqLU1toB4Dr83daJc0-sHZ_6kbhdwkrCoAAxP1TD502EZyRgHOARwcRg39Ld_HUVpw4gK8JBpICdqijYdi3Lxc8YhJsblVw9wP05rkb7WVRDVQ?purpose=inline)

23\. Design Kahoot / Live Quiz Platform

Additional

- Host creates and starts a session.
- Participants join using room codes.
- Real-time questions and answers.
- Countdown timers.
- Live leaderboard.
- Score calculation and result broadcasting.

Key challenge: Coordinating synchronized events across thousands of participants.

[How is the Metaverse changing the event industry?](https://images.openai.com/static-rsc-4/D8lZtJ_G8PV57NQ9jDwRiIm6qdAF4OmyaVHDgXppZzpYXaNFE5fW-GlZwIVI0wzkta9a31QLrxMrbCSAUmc2IKPtOckLbPrc3009Gk42esNj16pLc9FeNCQwJ9jqPJsAlRijgTiFHaHRRIwlt5XCR0pCXXwq6qeuL3sB9dYS798?purpose=inline)

24\. Design Virtual Event / Live Webinar Platform

Additional

- Large-scale participant management.
- Live Q&A and polls.
- Host and moderator controls.
- Real-time reactions.
- Participant presence.
- Live event notifications.

Key challenge: Distributing events to large audiences while managing interaction and participant state.

## Category F: Real-Time Infrastructure & Communication Platforms

These are slightly more infrastructure-oriented. They are valuable because interviewers sometimes ask you to design the underlying building blocks rather than a complete end-user application.

| #  | Problem                                      | What you should design                                                                                        |
| -- | -------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| 25 | WebSocket Gateway / Connection Manager       | Persistent connections, connection routing, heartbeats, reconnects, connection limits and horizontal scaling. |
| 26 | Distributed Pub/Sub System                   | Topics, publishers, subscribers, consumer groups, message delivery and partitioning.                          |
| 27 | Real-Time Event Broadcasting Service         | Ingest one event and distribute it to millions of interested subscribers.                                     |
| 28 | Webhook Delivery Platform                    | Event subscriptions, webhook delivery, retries, exponential backoff, signatures and dead-letter queues.       |
| 29 | Real-Time Collaborative Notification Service | Route user-specific and group-specific events across distributed WebSocket servers.                           |
| 30 | Live Activity / Event Feed                   | Aggregate user activities and distribute incremental updates to followers or subscribers.                     |
| 31 | Real-Time Log Streaming Platform             | Collect, process and stream logs to subscribed dashboards and monitoring clients.                             |
| 32 | IoT Device Communication Platform            | Device connections, telemetry ingestion, device commands, acknowledgments and real-time monitoring.           |

# 2. Which Problems Are Actually the Same Pattern?

This distinction is particularly important.

You should not design 32 completely different systems from scratch. Many share the same underlying architecture, with different consistency, delivery and scaling requirements.

Here is how I would group them.

| Underlying pattern                   | Problems                  |
| ------------------------------------ | ------------------------- |
| Persistent connection management     | 1–8, 25, 29               |
| One-to-one messaging                 | 1, 2, 3, 5                |
| Group messaging and fan-out          | 1, 2, 4, 9, 10            |
| Presence and ephemeral state         | 1, 2, 4, 6, 7             |
| Pub/Sub event distribution           | 8, 11, 12, 13, 14, 26, 27 |
| High-volume streaming                | 11, 12, 13, 14, 31, 32    |
| Geospatial streaming                 | 15                        |
| Collaborative state synchronization  | 16–20                     |
| Multiplayer state synchronization    | 21–24                     |
| Reliable asynchronous event delivery | 8, 27, 28                 |

For example:

- WhatsApp teaches you message delivery and persistence.
- Slack teaches you group messaging and channel fan-out.
- Presence teaches you distributed ephemeral state.
- Stock streaming teaches you high-throughput Pub/Sub.
- Google Docs teaches you OT/CRDT and conflict resolution.
- Figma teaches you collaborative structured-state synchronization.
- Multiplayer games teach you authoritative state and low-latency synchronization.

These are different interview problems, but their underlying concepts overlap significantly.

# 3. Core Concepts You Must Master

This is arguably the most important section of your preparation.

If you master these concepts, you can approach many unfamiliar real-time system design questions.

## A. Connection Management

| Concept             | What you need to understand                              |
| ------------------- | -------------------------------------------------------- |
| WebSockets          | Full-duplex persistent communication                     |
| SSE                 | Server-to-client streaming                               |
| Long polling        | Alternative when persistent connections aren't available |
| Connection Gateway  | Managing millions of concurrent connections              |
| Heartbeats          | Detecting dead or disconnected connections               |
| Reconnection        | Recovering from temporary network failures               |
| Sticky Sessions     | Keeping connections associated with a gateway            |
| Connection Routing  | Finding which gateway owns a user's connection           |
| Backpressure        | Handling slow consumers                                  |
| Connection Draining | Gracefully removing gateway instances                    |

## B. Message Delivery & Reliability

| Concept                     | What you need to understand                         |
| --------------------------- | --------------------------------------------------- |
| At-most-once                | Messages might be lost, but aren't redelivered      |
| At-least-once               | Messages can be delivered more than once            |
| Effectively-once processing | Idempotency and deduplication                       |
| Message Ordering            | Ordering within a conversation, partition or entity |
| Acknowledgments             | Sent, delivered and read states                     |
| Retry Mechanism             | Handling transient delivery failures                |
| Message Persistence         | Durable storage and history retrieval               |
| Offline Delivery            | Queuing messages for disconnected users             |
| Dead Letter Queue           | Handling messages that repeatedly fail              |
| Sequence Numbers            | Detecting gaps and ordering events                  |

Important interview distinction: A message being accepted by your backend does not mean it has been delivered to the recipient's device.

You should explicitly define each delivery state.

## C. Fan-Out & Pub/Sub

| Concept             | What you need to understand                     |
| ------------------- | ----------------------------------------------- |
| Fan-out-on-write    | Push events to recipients when they are created |
| Fan-out-on-read     | Fetch events when recipients request them       |
| Hybrid Fan-out      | Combine both approaches                         |
| Topic-based Pub/Sub | Subscribe to event topics                       |
| User-based Pub/Sub  | Subscribe to individual users                   |
| Group-based Pub/Sub | Distribute to group members                     |
| Consumer Groups     | Parallel consumption                            |
| Partitioning        | Distribute events across brokers                |
| Hot Partitions      | Handling uneven event distribution              |
| Batching            | Reducing overhead                               |
| Event Filtering     | Delivering only relevant updates                |

## D. Presence & Ephemeral State

- Heartbeat intervals and expiration.
- TTL-based Redis keys.
- Multiple active sessions per user.
- Online state across multiple devices.
- Presence aggregation.
- Failure detection.
- Presence subscriptions.
- Avoiding unnecessary presence broadcasts.

A critical design decision: Presence is generally ephemeral state, not durable business data.

## E. Collaborative Editing

This is a separate technical area that deserves special attention.

| Concept                          | Importance |
| -------------------------------- | ---------- |
| Operational Transformation (OT)  | Very high  |
| CRDT                             | Very high  |
| Conflict resolution              | Very high  |
| Version vectors / logical clocks | High       |
| Concurrent edits                 | Very high  |
| Document snapshots               | High       |
| Operation logs                   | High       |
| Undo / redo                      | Medium     |
| Offline editing                  | High       |
| State convergence                | Very high  |

Understand when to choose OT versus CRDT rather than assuming they are interchangeable.

## F. Real-Time Streaming

- Event ingestion.
- Stream processing.
- Event aggregation.
- Snapshot generation.
- Delta updates.
- Sequence numbers.
- Consumer lag.
- Backpressure.
- Rate limiting.
- Data compression.
- Slow consumer handling.
- Replay and recovery.

# 4. Recommended Architecture Patterns to Study

These architectures appear repeatedly across the problems above.

### Architecture 1: WebSocket-based communication

Must master

[WebSockets, Server-Sent Events, and Long Polling: Choosing the Right Real-Time Communication Pattern | CloudRPS](https://images.openai.com/static-rsc-4/M5Z4YYEF8cR13L-fJ7jKZKehIJWrRLnLvbpG59TkVML1IU4iwOox1pjq6Rz2bjPhXHPsDTEqccuC4qSiiDWRjFZxfK0gOcBzYQ2D0bOCiJF3uuQbRN2KcAg83frUVpENsKvLUhQI0RfGRDGQfow5_VOH08Li512Boc62yhwEILo?purpose=inline)

[cloudrps.com](https://cloudrps.com/blog/websockets-sse-long-polling-explained/?utm_source=chatgpt.com)

Typical flow:

1. Client establishes a WebSocket connection.
2. Load balancer routes it to a WebSocket gateway.
3. Gateway maintains the persistent connection.
4. Connection registry tracks user-to-gateway mapping.
5. Events arrive through a message broker.
6. Gateway forwards the event to the appropriate connected clients.

### Architecture 2: Event-driven fan-out

Must master

[🚀 From Chaos to Kafka: How Event-Driven Systems Changed the Way We Move Data | by Arpit Srivastav | Medium](https://images.openai.com/static-rsc-4/b4V0mrve_ZHvpdhraD1O3Ymhv4e8XLUVqt1hxh3OS50dBgnctaiHvJEweWncugDPZeXyV4b_KldIc_SNxGZeZpkwLoeHnpEsnnDmw-gGV9oWsZyqCWJJtd6HnEg3pzYGUyhZxaZ1qaexmXr110fpcs_YfQMvFZz4gy50WlHxKIk?purpose=inline)

[medium.com](https://medium.com/%40arpitsrivastav15s/from-chaos-to-kafka-how-event-driven-systems-changed-the-way-we-move-data-2506ce71940d?utm_source=chatgpt.com)

Typical flow:

1. Producer publishes an event.
2. Event enters a durable broker.
3. Fan-out workers identify subscribers.
4. Events are distributed to relevant gateways.
5. Gateways push events to connected clients.
6. Offline clients receive updates through a separate delivery mechanism.

### Architecture 3: Collaborative editing

Must master

[Design a Collaborative Document Editor like Google Docs | Google PM Interview](https://images.openai.com/static-rsc-4/xdwBx22asjKWDvDazIoxNm1DXSHVKKGTIJP6RAELPxk9DpVbghUafZcBUvFNevYiO_nKo4uFjsZi6URdlbiVd40VjgKJY3tzR3v0Cepn3jv2RKo4Jx5I8xoW8T_8rdWOjkRN7j77ar2qq67iN2Gcrj0YeEIHslPl_lFdDczBMxY?purpose=inline)

[crackpminterview.com](https://www.crackpminterview.com/p/technical-questions-design-collaborative-document-editor-google-docs?utm_source=chatgpt.com)

Typical flow:

1. User performs an editing operation.
2. Client sends the operation to the collaboration server.
3. Server resolves concurrent operations using OT or CRDT.
4. Updated operation is distributed to collaborators.
5. Clients apply operations and converge on a consistent state.
6. Server periodically persists snapshots and operation history.

### Architecture 4: Real-time streaming

Must master

[Apache Kafka and Ably: Building scalable, dependable chat applications](https://images.openai.com/static-rsc-4/WYgqnUxZk78kx9nF-C621whv465yTYRneYWVCJo7_ZlkMRFIpFIQYNt0tENgZbiLHvBS_MEPk9JIJvC47GiMVpUHToI86swaXAxFHjMLbtRxlr8KQ5uiVTSWNX2wZfy3G0ct9a_ZHBYsfGgR1vrmecCrYnfQ3NvgpAQBiU66AT8?purpose=inline)

[ably.com](https://ably.com/topic/apache-kafka-chat-application?utm_source=chatgpt.com)

Typical flow:

1. External producers generate events.
2. Ingestion service receives events.
3. Broker buffers and distributes them.
4. Stream processors aggregate, filter or enrich events.
5. Subscription service identifies interested consumers.
6. WebSocket gateways distribute updates.

# 5. My Recommended Problem-Solving Order

Don't solve these in random order. Build complexity progressively.

This is the sequence I recommend for becoming comfortable with this pattern in HLD interviews.

| Order | Problem                                | Priority   | What you should master                       |
| ----- | -------------------------------------- | ---------- | -------------------------------------------- |
| 1     | WebSocket Gateway / Connection Manager | Core       | Persistent connections and routing           |
| 2     | Real-Time Notification System          | Core       | Push delivery and offline notifications      |
| 3     | Online Presence System                 | Core       | Heartbeats, TTL, distributed state           |
| 4     | WhatsApp / Messenger                   | Core       | Message persistence and delivery             |
| 5     | Slack / Teams                          | Core       | Channels and group fan-out                   |
| 6     | Typing Indicators & Read Receipts      | Core       | Ephemeral versus durable state               |
| 7     | Distributed Pub/Sub System             | Core       | Topics, brokers and subscribers              |
| 8     | Social News Feed                       | Core       | Fan-out-on-write versus fan-out-on-read      |
| 9     | Live Sports Score System               | Core       | Massive fan-out and event distribution       |
| 10    | Stock Market Streaming                 | Core       | Throughput, backpressure and subscriptions   |
| 11    | Uber Live Location Tracking            | Core       | Frequent updates and geospatial distribution |
| 12    | Google Docs                            | Core       | OT, CRDT and concurrent editing              |
| 13    | Figma                                  | Core       | Structured shared-state synchronization      |
| 14    | Multiplayer Chess                      | Advanced   | Authoritative state and recovery             |
| 15    | Multiplayer Online Game                | Advanced   | Tick-based synchronization and latency       |
| 16    | Zoom / Google Meet                     | Advanced   | WebRTC and media infrastructure              |
| 17    | Miro / Collaborative Whiteboard        | Advanced   | High-frequency collaborative updates         |
| 18    | Crypto Exchange Market Data            | Advanced   | Ordered high-throughput streams              |
| 19    | Real-Time Analytics Dashboard          | Additional | Streaming aggregation                        |
| 20    | IoT Communication Platform             | Additional | Device connectivity and telemetry            |

My suggestion: Complete the first 13 properly before moving to the advanced problems.

# 6. Interview-Specific Variations You Should Practice

An interviewer may not directly ask you to design WhatsApp or Google Docs. They might ask a narrower question that tests the same underlying concepts.

Practice these variations too.

| Interview question                                                 | Underlying concept                |
| ------------------------------------------------------------------ | --------------------------------- |
| How would you support 10 million concurrent WebSocket connections? | Connection management             |
| How do you deliver a message when the recipient is offline?        | Durable delivery                  |
| How do you guarantee message ordering?                             | Partitioning and sequencing       |
| How do you avoid duplicate messages?                               | Idempotency                       |
| How do you handle a user connected from multiple devices?          | Session management                |
| How do you distribute a message to a group with 100,000 members?   | Fan-out                           |
| How do you handle a celebrity with 100 million followers?          | Hybrid fan-out                    |
| How do you prevent slow consumers from affecting others?           | Backpressure                      |
| What happens when a WebSocket gateway crashes?                     | Reconnection and recovery         |
| How do you recover missed events after reconnection?               | Replay and offsets                |
| How do you maintain accurate online status?                        | Distributed presence              |
| How do you resolve simultaneous document edits?                    | OT / CRDT                         |
| How do you synchronize a large shared document?                    | Snapshots and incremental updates |
| How do you handle millions of stock price subscribers?             | Streaming and Pub/Sub             |
| How do you handle out-of-order location updates?                   | Event timestamps and sequencing   |

These questions are particularly useful for Google and Meta-style interviews because they test whether you understand the architectural trade-offs behind your design.

# 7. Final Preparation Strategy

I would divide your preparation into three levels.

Level 1 — Must-master problems

Highest priority

1. WhatsApp
2. Slack
3. Online Presence System
4. Real-Time Notification System
5. Social News Feed
6. Live Sports Scores
7. Stock Market Streaming
8. Google Docs
9. Figma
10. Uber Live Location Tracking

Level 2 — Advanced problems

1. Discord
2. Zoom / Google Meet
3. Multiplayer Online Game
4. Multiplayer Chess
5. Miro
6. Cryptocurrency Exchange Streaming
7. Distributed Pub/Sub
8. WebSocket Gateway

Level 3 — Additional coverage

1. Live Customer Support Chat
2. Live Webinar / Virtual Events
3. Kahoot
4. Notion
5. Collaborative Jira / Trello
6. Real-Time Analytics Dashboard
7. IoT Communication Platform
8. Webhook Delivery Platform
9. Real-Time Log Streaming

## What I would expect you to know after completing this pattern

You should be able to confidently answer these architectural questions:

1. When should I choose WebSockets versus SSE versus polling?
2. When should I introduce Redis Pub/Sub, Kafka or another broker?
3. How should I map millions of user connections to distributed gateways?
4. How should I implement message delivery, acknowledgments and offline queues?
5. When should I use fan-out-on-write versus fan-out-on-read?
6. How do I handle millions of subscribers receiving the same event?
7. How do I handle hot users, hot partitions and slow consumers?
8. How do I synchronize state across multiple clients?
9. When should I use OT versus CRDT?
10. How do I recover from gateway failures and missed events?
11. How do I distinguish durable events from ephemeral updates?
12. How do I achieve low latency without sacrificing reliability?

One important recommendation: Don't treat Google Docs, Figma and multiplayer games as just another WebSocket problem. WebSockets are only the transport. Their real engineering challenges are concurrency control, state synchronization, conflict resolution and consistency.

Likewise, don't treat WhatsApp, Slack and stock streaming as identical fan-out problems. Their delivery guarantees, persistence requirements, subscriber patterns and latency requirements are significantly different.

Mastering these distinctions is what will take your HLD preparation beyond memorizing standard architectures.
