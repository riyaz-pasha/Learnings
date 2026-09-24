## 🔴 Hard (Google / Staff+ Level)

These involve complex distributed systems thinking:

- **Design Google Docs** — Real-time collaborative editing with conflict resolution (OT/CRDT)
- **Design Uber's Surge Pricing Engine** — Real-time demand/supply modeling at scale
- **Design a Distributed Message Queue** (like Kafka) — Partitioning, replication, consumer groups
- **Design Google Photos** — Deduplication, ML-based search, storage tiering
- **Design a Code Execution Engine** (like LeetCode/Replit) — Sandboxing, resource isolation, multi-language
- **Design a Fraud Detection System** — Real-time stream processing + ML inference pipeline
- **Design Twitter's Trending Topics** — Approximate counting, time-decay, geo-specific trends
- **Design a Distributed Tracing System** (like Jaeger/Zipkin) — Trace context propagation, sampling

---

## 🟠 Medium-Hard (Senior SWE Level)

- **Design Airbnb's Search + Ranking** — Geo search, availability calendar, ranking signals
- **Design a Content Moderation System** — Async ML pipelines, human-in-the-loop review queues
- **Design Netflix's Recommendation Engine** — Offline training + online serving, A/B testing infra
- **Design a Live Sports Scoreboard** — Fan-out at massive scale, real-time score updates
- **Design Dropbox Sync** — Delta sync, conflict resolution, offline support
- **Design a Digital Wallet / Payment System** (like PhonePe/GPay) — Double-spend prevention, idempotency
- **Design an Ad Click Aggregation System** — Exactly-once counting, late data handling
- **Design an API Gateway** — Auth, rate limiting, request routing, circuit breakers

---

## 🟡 Specific to Your 3D Furniture Experience

Since you just faced this, similar "creative product feature" questions are trending:

- **Design a Virtual Try-On System** (like Snapchat Lens / Amazon's AR View)
- **Design a Maps Routing Engine** — Beyond Google Maps, focus on real-time traffic + rerouting
- **Design a Location-based Feature Store** — Like Foursquare or Zomato's "near me" at scale

---

## 💡 What to Practice Differently

Given you struggled with the 3D furniture question, the pattern that likely tripped you up is **unfamiliar domain + standard distributed systems**. The fix:

1. **Always decompose first** — Any feature = Storage + Compute + APIs + Client. Don't let the domain scare you.
2. **Identify the hard sub-problems** — For 3D room planner it was: asset delivery (3D models via CDN), real-time rendering (client-side vs server-side), state persistence (room layouts), and collaboration (if multi-user).
3. **Practice "weird" products** — Design a Spotify lyrics sync, a live auction system, or a multiplayer whiteboard. These train you to apply fundamentals to unfamiliar contexts.

---

Which of these would you like to dive into first? I can give you a full structured breakdown with components, trade-offs, and what Google interviewers specifically look for.
---

Great choice — these "weird product" questions are increasingly common at Google, Meta, and Stripe because they test whether you can **think, not just memorize patterns**.

Here's a full bank of them, grouped by the core challenge each one trains:

---

## 🎯 Real-Time Sync Problems
*Trains: WebSockets, CRDTs, conflict resolution, fan-out*

| Question | The Hard Part |
|---|---|
| **Spotify Lyrics Sync** | Syncing lyrics to audio timestamp across devices, handling seek/pause |
| **Multiplayer Whiteboard** (Figma-like) | Concurrent edits, low-latency rendering, undo/redo at scale |
| **Live Polling System** (Slido/Mentimeter) | Millions voting in seconds, real-time result aggregation |
| **Collaborative Spreadsheet** | Cell-level conflict resolution, formula dependency graphs |
| **Live Code Interview Tool** (CoderPad) | Shared editor + code execution + video, all in sync |

---

## 🏗️ Stateful & Session-Heavy Systems
*Trains: Session management, distributed state, consistency*

| Question | The Hard Part |
|---|---|
| **Live Auction System** | Bid ordering guarantees, sniping prevention, exactly-once wins |
| **Online Multiplayer Chess** | Game state machine, move validation, reconnection handling |
| **Virtual Waiting Room** (like Ticketmaster queue) | Fair ordering, queue position updates, preventing skips |
| **Online Exam Proctoring System** | Screen capture streaming, tab-switch detection, result integrity |
| **Digital Escape Room** | Shared puzzle state, hint system, time-bound sessions |

---

## 📍 Location + Physical World Fusion
*Trains: Geo-indexing, IoT data, edge computing*

| Question | The Hard Part |
|---|---|
| **Smart Parking System** | Real-time slot availability, reservation + walk-in conflicts |
| **Airport Navigation App** (indoor maps) | Indoor positioning (no GPS), dynamic gate changes |
| **Grocery Store "Item Finder"** | Aisle-level positioning, inventory sync, store layout changes |
| **Live Marathon Tracker** | GPS pings from 30,000 runners, fan-facing real-time leaderboard |
| **Bike/Scooter Rental System** | IoT lock/unlock, geofencing, battery telemetry |

---

## 🎬 Media & Streaming Edge Cases
*Trains: CDN design, encoding pipelines, adaptive bitrate*

| Question | The Hard Part |
|---|---|
| **Live Sports Clipping Tool** | Clip a live stream 10s ago, process + share before the moment dies |
| **Karaoke App** | Audio/video sync, pitch detection, recording + sharing |
| **Podcast Transcription Service** | Long audio → text pipeline, speaker diarization, searchability |
| **Screen Recording + Sharing** (Loom-like) | Upload while recording, instant shareable link, async processing |
| **Virtual Concert Platform** | Thousands watching same frame in sync, artist interaction at scale |

---

## 💰 Financial & Trust-Critical Systems
*Trains: Idempotency, auditability, distributed transactions*

| Question | The Hard Part |
|---|---|
| **Split Bill App** (Splitwise) | Debt simplification algorithm, multi-currency, settlement flows |
| **Flash Sale System** | Inventory atomicity, thundering herd on "Go Live", oversell prevention |
| **Subscription Billing Engine** | Proration, dunning logic, webhook reliability |
| **Loyalty Points System** | Points expiry, partial redemption, fraud patterns |
| **Peer-to-Peer Lending Platform** | Matching engine, risk scoring, repayment tracking |

---

## 🧠 ML-Serving in a Product Context
*Trains: Feature stores, model serving, feedback loops*

| Question | The Hard Part |
|---|---|
| **Content Trigger Warning System** | Low-latency inference on user-generated content pre-publish |
| **Resume Screening Tool** | Bias-aware ranking, explainability, employer feedback loop |
| **Real-time Sports Commentary Generator** | Event stream → NLP → broadcast latency under 3s |
| **Personalized Push Notification Timing** | Per-user optimal send time, A/B testing, delivery guarantees |

---

## How to Use This List

Rather than solving them linearly, practice them **by the hard part they share**:

```
Week 1 → Pick 2 from Real-Time Sync  (master WebSockets + CRDT thinking)
Week 2 → Pick 2 from Stateful Systems (master distributed state)
Week 3 → Pick 2 from Financial       (master idempotency + consistency)
```

This way you build **transferable mental models**, not just memorized answers. When a new weird question appears in an interview, you'll recognize *which pattern it belongs to* within 60 seconds.

---

Want me to do a **full deep-dive on any one of these** — with architecture diagram, component breakdown, trade-offs, and what an interviewer specifically looks for?

---


Riyaz, this is **exactly the right stage of preparation**.
You already solved the **classic 20–25 system design questions**, which most candidates stop at.

But **Google / Meta / Uber / Airbnb interviews rarely ask those directly anymore**.
Instead they ask **twisted / product-specific / feature-design problems** like the **3D room planner** you got.

Your goal now should be to master **“unknown real-world systems”**, not memorized ones.

Below is a **curated list of 60+ advanced system design interview problems** that frequently appear at **Google, Meta, Uber, Airbnb, Stripe, Netflix, Amazon**.

I organized them so you can practice like a **real interview progression**.

---

# Advanced System Design Questions (Post-Classic Level)

You already solved the **foundation set**.
These are **new problems most candidates haven't practiced**.

---

# 1️⃣ Interactive Product Systems (Very Google Style)

These are **UI + backend + real-time systems**.

```
25-3D-Room-Planner (what you got asked)
26-Google-Docs-Offline-Sync
27-Figma-like-Collaborative-Design-Tool
28-Canva-Design-System
29-Online-Whiteboard (Miro)
30-AR Furniture Placement (IKEA style)
31-Real-time Code Editor (LeetCode / Replit)
32-Google Slides Collaborative Editor
33-Interactive Map Annotation System
34-Real-time Multiplayer Game Backend
```

Why Google asks these:

Because they test

* real-time systems
* state sync
* collaborative editing
* conflict resolution

---

# 2️⃣ ML / AI Infrastructure Systems (Very Google / Meta)

```
35-Feature-Store-for-ML
36-Recommendation-System-Infrastructure
37-Training-Data-Pipeline
38-Online-Inference-System
39-Experimentation-Platform (A/B testing)
40-Personalization-Engine
41-Content-Moderation-System
42-AI-Search-System
43-Embedding-Similarity-Search-System
```

These are **extremely common now**.

---

# 3️⃣ Observability / Infrastructure Systems

Google LOVES these.

```
44-Distributed-Tracing-System (Jaeger)
45-Log-Aggregation-System (ELK)
46-Metrics-Collection-System (Prometheus)
47-Service-Mesh-Control-Plane
48-Feature-Flag-System
49-Configuration-Management-System
50-CI/CD-System
```

---

# 4️⃣ Marketplace / Platform Systems

These test **business modeling + scale**.

```
51-Airbnb
52-Uber-Pricing-System
53-Uber-Driver-Dispatch-System
54-Ad-Auction-System (Google Ads)
55-Payment-System (Stripe)
56-Subscription-Billing-System
57-Crowdfunding-Platform
58-Freelancer-Marketplace
```

---

# 5️⃣ Data Infrastructure Systems

Very common at **Google / Stripe / Snowflake**.

```
59-Real-time-Analytics-System
60-Distributed-OLAP-Database
61-Time-Series-Database
62-Event-Streaming-System (Kafka)
63-CDC-System (Debezium)
64-Data-Lake-System
65-ETL-Pipeline
```

---

# 6️⃣ Very Hard Systems (Senior/Staff Level)

These appear at **Google L5+ interviews**.

```
66-Global-Cache-Invalidation-System
67-Planet-Scale-Distributed-Lock-Service
68-Multi-region-Database
69-Data-Reconciliation-System
70-Search-Indexing-System
71-Large-Scale-Vector-Database
72-Global-Configuration-Distribution-System
```

---

# 7️⃣ Very Product-Specific Questions (Google Style)

These are **the hardest because they are unexpected**.

```
73-Google-Photos-Storage-System
74-Google-Street-View
75-Google-Earth-3D-Tiles
76-Google-Calendar
77-YouTube-Live-Streaming
78-YouTube-Recommendation-System
79-Google-Drive-Sync
80-Google-Meet
```

---

# ⭐ Why you struggled with the 3D Room Planner

Because it's not a **standard backend system**.

It combines:

```
3D Rendering
Asset storage
Physics
Realtime interaction
User state persistence
Collaborative editing
```

Interviewers expect a **hybrid architecture**.

Example:

```
Client
 ├── WebGL Renderer (Three.js)
 ├── Asset Loader
 ├── Scene State Manager
 └── Collaboration Layer

Backend
 ├── Asset CDN
 ├── Scene Storage
 ├── Realtime Collaboration Server
 ├── Recommendation Engine
 └── Rendering Service
```

---

# The Skill You Actually Need

You don't need **100 more questions**.

You need the **ability to handle unknown problems**.

Every system can be decomposed into:

```
1 UI system
2 storage
3 compute
4 realtime
5 scaling
```

Example:

**3D planner**

```
UI → WebGL
storage → scene graph
compute → layout physics
realtime → collaboration
scale → asset CDN
```

Once you do that, **any problem becomes solvable.**

---

# What I Recommend For You (Important)

Since you're targeting **Google / product companies**,
the **next 30 problems you should solve are these**:

```
1  Google Docs
2  Figma
3  Canva
4  Real-time Multiplayer Game
5  AR Furniture Placement
6  Google Photos
7  Distributed Tracing
8  Metrics System
9  Feature Flags
10 Experimentation Platform
11 Payment System
12 Ad Auction
13 Uber Dispatch
14 Uber Surge Pricing
15 Airbnb
16 Vector Database
17 Search Indexing
18 Recommendation Engine
19 Online ML Inference
20 Streaming Analytics
21 Time Series DB
22 Feature Store
23 Log Aggregation
24 Global Cache Invalidation
25 Multi-region DB
26 Planet-scale lock service
27 YouTube Live
28 Google Drive Sync
29 Google Meet
30 Real-time Whiteboard
```

If you solve these **properly**,
**no interview question will surprise you again.**

---

# If you want, I can also give you something extremely valuable

I can give you:

### **40 "unknown-style" interview prompts**

Exactly like interviewers ask.

Example:

```
Design a system that allows users to place furniture in a virtual room
using their phone camera.
```

or

```
Design a system that allows millions of users to collaboratively
edit a whiteboard in real time.
```

or

```
Design an infrastructure that allows running A/B experiments
for billions of users.
```

These are **much harder than typical system design questions.**

---

If you want, I can also give you the **ULTIMATE list**:

**"50 Google-style system design interview questions that almost nobody practices."**

This will **massively increase your confidence in interviews.**
