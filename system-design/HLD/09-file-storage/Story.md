# 📁 Designing a File Storage System — The Story Begins

---

## Chapter 1: The Problem

Imagine it's 2005. You're a software engineer at a small startup. Your colleagues keep emailing files back and forth — "Hey, here's the updated doc", "Wait, I have a newer version", "My hard drive crashed and I lost everything."

Sound familiar? People had **3 core pain points:**

1. 🔥 **"My hard drive died and I lost everything"** → No backup
2. 😩 **"I can't access my files from my other laptop"** → No remote access
3. 🤝 **"How do I send this 2GB video to my friend?"** → No easy sharing

So someone said: *"What if we store files on a server instead of locally?"*

---

## The First Naive Attempt 🧪

A junior engineer thinks: *"Simple! Let's just put one big server, users upload files to it, done."*

```
User → [Upload File] → Single Server (stores files on disk) → [Download File] → User
```

**This actually works... for 10 users.** Let's trace what happens:

- Priya uploads `vacation.mp4` (500MB)
- Server saves it at `/files/priya/vacation.mp4`
- Priya later opens her phone → downloads it → ✅ works!

---

## But then 1000 users join... 💥

Suddenly you notice:

| Problem | What's happening |
|---|---|
| 🐢 Uploads are slow | Single server CPU is maxed out |
| 💾 Disk is almost full | 1TB disk, 1000 users × avg 2GB = 2TB needed |
| 😱 Server goes down for maintenance | **ALL users lose access** |
| 🌍 User in Japan, server in US | High latency, slow downloads |

The single server was a **Single Point of Failure (SPOF)**. One crash = everyone's files gone.

---

## So what do we actually need to design? 🎯

Before jumping to solutions, let's define the **requirements** — this is the first thing you say in any HLD interview.

### Functional Requirements
- Upload a file (any type — doc, image, video)
- Download a file
- Share a file with others (view/edit access)
- Organize files in folders
- See file version history

### Non-Functional Requirements
- **Availability**: 99.99% uptime (files must always be accessible)
- **Durability**: If you store a file, it should **never** be lost (even if hardware fails)
- **Scalability**: Should work for 500 million users storing petabytes of data
- **Low Latency**: Downloads should be fast regardless of user location
- **Consistency**: If you upload a new version, everyone should see the latest

### Scale Estimation (Back of envelope — interviewers love this)
```
500 million users
→ 20% active daily = 100M DAU
→ avg user stores 15GB = 7.5 Petabytes total storage
→ avg 10 uploads/day per active user = 1 Billion uploads/day
→ 1B / 86400 seconds ≈ ~11,500 uploads/second (peak = 2x = ~23,000/sec)
```

This tells us immediately — **one server is a joke.** We need a distributed system.

---

## The High Level Architecture (Bird's Eye View) 🗺️

Here's the skeleton we'll be building and evolving throughout our story:

```
                        ┌─────────────────────┐
                        │     Client Apps      │
                        │  (Web / Mobile / Desktop) │
                        └────────┬────────────┘
                                 │
                        ┌────────▼────────────┐
                        │    Load Balancer     │  ← Distributes traffic
                        └────────┬────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
     ┌────────▼───┐    ┌────────▼───┐    ┌────────▼───┐
     │  API Server │    │ API Server │    │ API Server │  ← Stateless servers
     └────────┬───┘    └────────┬───┘    └────────┬───┘
              │                  │                  │
              └──────────────────┼──────────────────┘
                                 │
         ┌───────────────────────┼────────────────────────┐
         │                       │                        │
┌────────▼───────┐    ┌─────────▼──────┐    ┌───────────▼────────┐
│  Metadata DB   │    │  Block Storage  │    │    Cache Layer     │
│(who owns what, │    │ (actual file    │    │  (Redis - fast     │
│ file names etc)│    │  data/chunks)   │    │   metadata fetch)  │
└────────────────┘    └─────────────────┘    └────────────────────┘
```

---

## What's next? 🚀

Now we have the problem and the skeleton. The **most important and interesting concept** to tackle first is:

> **How do you actually store a file? Do you save it as one whole piece or break it into chunks?**

This one decision changes EVERYTHING — upload speed, resumable uploads, deduplication, storage efficiency.

---

**Shall we dive into Chapter 2: File Chunking — Why storing a file as one piece is a terrible idea?** 

I'll walk you through the story of how engineers discovered the chunking approach, with concrete examples of what breaks without it. Just say **"next"** or **"yes"** when you're ready! 🎯

---

# 📦 Chapter 2: File Chunking — Breaking Files Into Pieces

---

## The Story Continues...

So you've got multiple servers now. A user tries to upload a **2GB video file.**

```
User → [Upload 2GB file] → Server → Save to disk ✅
```

Seems fine. But then Priya, who's on a train with shaky WiFi, tries to upload her `wedding_video.mp4` (2GB). It uploads for **47 minutes**... and at minute 46, her internet drops.

```
Progress: ████████████████████░ 98%... CONNECTION LOST 💀
```

**She has to start from 0% again.** 😭

Not just that — your server engineer notices:

- The server is holding a **2GB file in memory** while uploading
- If 100 people do this simultaneously → **200GB RAM needed** just for uploads
- A single corrupted byte anywhere in the 2GB → **entire file is corrupted**

There had to be a better way.

---

## The "Aha!" Moment: What if we break the file into chunks? 🧩

An engineer says: *"What if instead of uploading one giant file, we cut it into small pieces — like how a book has chapters?"*

### How Chunking Works

```
Original File: wedding_video.mp4 (2GB)
         │
         ▼
    ┌─────────┐
    │ Chunker │  splits into fixed-size pieces (e.g., 4MB each)
    └─────────┘
         │
    ┌────┴─────────────────────────────────────────┐
    │         │         │         │         │      │
  [C-001]  [C-002]  [C-003]  [C-004]  ...  [C-512]
   4MB       4MB      4MB      4MB           4MB
```

Each chunk gets a **unique ID** — typically its hash (we'll come back to why this is genius).

```
chunk_001 → hash → "a3f8c2..." 
chunk_002 → hash → "91bc4e..."
chunk_003 → hash → "77dd12..."
```

---

## What does the client do now?

```javascript
// Pseudo-code of what happens on the client side

function uploadFile(file) {
  const CHUNK_SIZE = 4 * 1024 * 1024; // 4MB
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
  
  for (let i = 0; i < totalChunks; i++) {
    const chunk = file.slice(i * CHUNK_SIZE, (i + 1) * CHUNK_SIZE);
    const chunkHash = computeHash(chunk); // SHA-256
    
    uploadChunk({
      fileId: "wedding_video_123",
      chunkIndex: i,
      totalChunks: totalChunks,
      chunkHash: chunkHash,
      data: chunk
    });
  }
}
```

The server receives and stores chunks **independently**. Each chunk is a separate, verifiable unit.

---

## Problem 1 Solved: Resumable Uploads ✅

Now when Priya's internet drops at 98%:

```
Uploaded so far:
[C-001✅][C-002✅][C-003✅]...[C-511✅][C-512❌] ← only this one failed

On reconnect → Client asks server: "Which chunks do you have?"
Server: "I have 1 to 511"
Client: "Cool, I'll only re-upload chunk 512"

Re-upload time: 4MB instead of 2GB 🎉
```

This is exactly how **Google Drive and Dropbox resumable uploads** work. They use a protocol called **TUS (resumable upload protocol)**.

---

## Problem 2 Solved: Parallel Uploads ⚡

With chunking, you don't have to upload one chunk at a time. You can upload **multiple chunks simultaneously!**

```
Without chunking:          With chunking (4 parallel streams):
                           
[====2GB====] 47 min       [==512MB==]  ↗
                           [==512MB==]  → 12 min total! 🚀
                           [==512MB==]  ↘
                           [==512MB==]  ↙
```

---

## Problem 3 Solved: Deduplication 🤯 (This one is mind-blowing)

Remember each chunk gets a **hash**? Here's where it gets clever.

Imagine 1 million users upload the same viral YouTube video (or the same Microsoft Office installer).

**Without chunking:**
```
1,000,000 users × 500MB = 500 Terabytes wasted storing the same file 1M times
```

**With chunk hashing:**
```
User 1 uploads video.mp4
→ Chunk hashes: ["a3f8c2", "91bc4e", "77dd12", ...]
→ Server stores the actual chunk data

User 2 uploads the SAME video.mp4
→ Client computes chunk hashes BEFORE uploading
→ Client asks server: "Do you have chunks a3f8c2, 91bc4e, 77dd12?"
→ Server: "YES, I have all of them!"
→ Client: doesn't upload anything! Just says "map these chunks to my file"

Upload time for user 2: ~1 second (just metadata) instead of 47 minutes!
```

This is called **Content-Defined Deduplication** and it's how Dropbox saves **petabytes** of storage.

---

## What does the Metadata look like?

The server needs to remember which chunks belong to which file, in what order:

```
Files Table:
┌──────────┬─────────────────────┬──────────┬───────────┬──────────────┐
│ file_id  │ file_name           │ owner_id │ size      │ created_at   │
├──────────┼─────────────────────┼──────────┼───────────┼──────────────┤
│ FILE_123 │ wedding_video.mp4   │ user_42  │ 2,048 MB  │ 2024-01-15   │
└──────────┴─────────────────────┴──────────┴───────────┴──────────────┘

File_Chunks Table:
┌──────────┬─────────────┬────────────────┬────────────┐
│ file_id  │ chunk_index │ chunk_hash     │ chunk_size │
├──────────┼─────────────┼────────────────┼────────────┤
│ FILE_123 │ 0           │ a3f8c2...      │ 4MB        │
│ FILE_123 │ 1           │ 91bc4e...      │ 4MB        │
│ FILE_123 │ 2           │ 77dd12...      │ 4MB        │
│ ...      │ ...         │ ...            │ ...        │
│ FILE_123 │ 511         │ 88cd91...      │ 4MB        │
└──────────┴─────────────┴────────────────┴────────────┘

Chunks Table (actual storage reference):
┌────────────────┬──────────────────────────────────┐
│ chunk_hash     │ storage_path                     │
├────────────────┼──────────────────────────────────┤
│ a3f8c2...      │ /storage/node-7/a3/f8/a3f8c2.bin │
│ 91bc4e...      │ /storage/node-2/91/bc/91bc4e.bin │
└────────────────┴──────────────────────────────────┘
```

Notice — the **chunks table doesn't have an owner**. A chunk is just data. Multiple files from multiple users can **point to the same chunk**. Pure storage efficiency.

---

## Download: Reassembling the File

```
User requests wedding_video.mp4
         │
         ▼
  Fetch file metadata → get list of chunk hashes in order
         │
         ▼
  Fetch each chunk from storage (in parallel!)
         │
         ▼
  Reassemble: chunk_0 + chunk_1 + chunk_2 + ... + chunk_511
         │
         ▼
  Stream to user ✅
```

---

## New Problem Discovered: What chunk size should we use? 🤔

This is a classic interview follow-up. There's a tradeoff:

| Chunk Size | Pros | Cons |
|---|---|---|
| **Too Small** (64KB) | Fine-grained resume, better dedup | Millions of chunks, DB gets huge, too many network round trips |
| **Too Large** (100MB) | Few chunks, simple | Bad for resumability, poor parallelism, less dedup |
| **Just Right** (4–8MB) | Balance of all factors | — |

**Google Drive uses ~256KB to a few MB. Dropbox uses 4MB.** It also depends on the network — mobile clients might use smaller chunks.

---

## Chapter 2 Summary 🎯

```
File Chunking gives us:

✅ Resumable uploads    → only re-upload failed chunks
✅ Parallel uploads     → faster transfers
✅ Deduplication        → massive storage savings
✅ Integrity checks     → hash mismatch = corrupted chunk, re-fetch it
✅ Efficient streaming  → start playing video before full download
```

---

## What breaks next? 👀

Now we have chunks stored on a server. But here's the scary question:

> **"What if the storage server's hard drive physically dies? All those chunks are gone forever."**

This leads us to one of the most critical concepts in distributed systems...

**Chapter 3: Replication & Durability — How do we make sure files are NEVER lost, even when hardware fails?**

Say **"next"** when you're ready! The story is about to get very interesting — we'll see how Google achieves **eleven 9s of durability (99.999999999%)** 🔥

---

# 🛡️ Chapter 3: Replication & Durability — Never Lose a File

---

## The Story Continues...

It's 2 AM. Your on-call phone rings. A storage server's hard drive just **physically died**. 

You check the logs:
```
[02:14:33] ERROR: Disk /dev/sdb on storage-node-7 → READ FAILURE
[02:14:33] ERROR: 847,293 chunks are now INACCESSIBLE
[02:14:33] ERROR: Affected users: 12,847
```

12,847 users just **permanently lost their files.** Your inbox floods with angry emails. The news headline reads *"Startup loses user data, shuts down."*

This is not hypothetical — it happened to companies. So engineers asked:

> *"How do we make sure a file survives even if a hard drive, an entire server, or even an entire data center burns down?"*

---

## First Attempt: Just Copy It Twice 📋

Simple idea — every time you write a chunk, write it to **2 servers** instead of 1.

```
Upload chunk "a3f8c2"
        │
        ├──→ Storage Node 3  ✅ (primary)
        └──→ Storage Node 7  ✅ (replica)
```

Now if Node 3 dies → Node 7 still has it. Problem solved?

**Not quite.** What if Node 3 AND Node 7 are in the same room, and the room catches fire? 🔥

Or what if Node 3 writes successfully, but Node 7's write fails silently? Now you *think* you have 2 copies but you only have 1.

Engineers needed a more rigorous approach. This led to the formal concept of **Replication**.

---

## Understanding Replication Properly

### The 3-2-1 Rule (Industry Standard)
```
3 → Keep 3 copies of the data
2 → Store on 2 different types of media/servers  
1 → Keep 1 copy in a geographically separate location
```

In practice for a cloud system:
```
Chunk "a3f8c2" gets written to:

├── Data Center: US-East
│   ├── Rack 4, Server 12, Disk 2   ← Copy 1
│   └── Rack 9, Server 3,  Disk 5   ← Copy 2
│
└── Data Center: US-West            ← Copy 3 (geo-redundant)
    └── Rack 2, Server 8,  Disk 1
```

Now you're protected against:
- ✅ Single disk failure
- ✅ Single server failure  
- ✅ Entire rack failure (power/network)
- ✅ Entire data center failure (fire, flood, earthquake)

---

## How Replication Actually Works: The Write Path

When a chunk arrives, here's what happens step by step:

```
Client uploads chunk "a3f8c2"
              │
              ▼
       ┌─────────────┐
       │  API Server  │
       └──────┬──────┘
              │ "Store this chunk on 3 nodes"
              ▼
    ┌─────────────────────┐
    │  Replication Manager │  ← decides WHERE to store
    └──────────┬──────────┘
               │
    ┌──────────┼──────────┐
    │          │          │
    ▼          ▼          ▼
 Node-A      Node-B     Node-C
 (US-E R4)  (US-E R9)  (US-W R2)
    │          │          │
    ✅ ACK     ✅ ACK    ✅ ACK
               │
               ▼
    All 3 confirmed → Tell client: "Upload successful ✅"
```

But here's the critical question — **when exactly do you tell the client "success"?**

---

## Sync vs Async Replication — A Core Tradeoff

### Option A: Synchronous Replication (Strong Consistency)

```
Client → Write to Node A → Write to Node B → Write to Node C → "Success ✅"

Only tells client SUCCESS after ALL 3 copies are written.
```

**Pro:** You NEVER lose data. If client got "success", all 3 copies exist.  
**Con:** Upload is as slow as the **slowest** node. If Node C (US-West) is laggy → everyone waits.

### Option B: Asynchronous Replication (Eventual Consistency)

```
Client → Write to Node A → "Success ✅" (immediately)
                    │
                    └──→ [Background] replicate to Node B, Node C
```

**Pro:** Super fast. Client doesn't wait for all copies.  
**Con:** If server crashes right after telling client "success" but before replicating → **data loss.**

### Option C: Quorum Write (The Sweet Spot) ⭐

This is what systems like **Cassandra, DynamoDB, and GFS** use.

```
You have 3 nodes. Write is "successful" when at least 2 out of 3 confirm.

Client → Write to Node A ✅
       → Write to Node B ✅   → 2/3 confirmed → "Success ✅"
       → Write to Node C ⏳   → replicates in background
```

The formula:
```
W + R > N

W = nodes that must confirm a write
R = nodes that must confirm a read  
N = total replicas

Example: N=3, W=2, R=2 → 2+2=4 > 3 ✅
This guarantees you always read the latest write.
```

**Google Drive uses a variant of this.** You get speed AND safety.

---

## New Problem: Detecting Failures — How does the system know a node is dead?

You can't just wait for errors. You need to **proactively** know when a node goes down.

### Heartbeat Mechanism

Every storage node sends a "I'm alive!" ping every few seconds:

```
Storage Node → [ping every 5 sec] → Master/Controller Node

If Master doesn't hear from a node for 30 seconds:
→ Mark node as SUSPECT
→ After 60 seconds: Mark as DEAD
→ Trigger re-replication of all chunks that were on that node
```

```
Timeline:
T=0s   Node-7 crashes (disk failure)
T=35s  Master: "Node-7 hasn't pinged in 30s... marking SUSPECT"
T=65s  Master: "Node-7 confirmed dead. It had 847,293 chunks."
T=66s  Master: "Starting re-replication to Node-12 and Node-15"
T=~2h  Re-replication complete. Back to 3 copies everywhere. ✅
```

This is called **Self-Healing** — the system automatically recovers without human intervention.

---

## Even Sneakier Problem: Silent Data Corruption (Bit Rot) 🐛

Hard drives don't always scream when they fail. Sometimes they silently return **wrong data**. This is called **bit rot** — magnetic bits flip over time due to cosmic rays, heat, electrical interference.

```
You stored:  01101100 11001010 00110101...
You read:    01101100 11001011 00110101...
                              ^ this bit flipped! 
```

How do you detect this? **Checksums.**

Remember we computed a hash for each chunk during upload?

```
On Write:
chunk_data = [bytes...]
hash = SHA256(chunk_data) = "a3f8c2d9..."
Store both: chunk_data + hash

On Read:
read chunk_data from disk
recompute SHA256(chunk_data) = "a3f8c2d9..." ← must match stored hash!

If it doesn't match → chunk is corrupted → fetch from another replica
```

Google's storage system runs **background scrubbing jobs** — periodically reading every chunk and verifying its checksum, even if nobody requested it. Like a janitor quietly checking every file cabinet every night.

---

## Geographic Replication — Surviving Data Center Failures

Now, what if an entire data center goes down? Earthquake in Virginia knocked out AWS US-East once in 2012. Millions of sites went down.

The solution: **Multi-region replication**

```
          ┌─────────────────┐
          │   US-East DC    │  ← Primary Region
          │  [Chunk Store]  │
          └────────┬────────┘
                   │ async replication
          ┌────────┴────────────────────┐
          │                             │
┌─────────▼───────┐           ┌────────▼────────┐
│   US-West DC    │           │   Europe DC     │
│  [Chunk Store]  │           │  [Chunk Store]  │
└─────────────────┘           └─────────────────┘
```

**Tradeoff here:**
- Synchronous cross-region replication → too slow (US to Europe = ~100ms latency per write)
- Asynchronous cross-region replication → tiny window of potential data loss (RPO > 0)

Most systems (including Google Drive) use **async cross-region** replication with a very small **RPO (Recovery Point Objective)** — meaning in the absolute worst case, you might lose the last few seconds of writes. For a file storage system, this is acceptable.

---

## Putting It All Together — Durability Numbers

This is how Google achieves **eleven 9s of durability (99.999999999%)**:

```
Single disk failure rate: ~0.5% per year

With 3 replicas across different racks:
Probability all 3 fail simultaneously = 0.005 × 0.005 × 0.005 
= 0.000000125 = 0.0000125% per year

With geo-replication + checksums + scrubbing:
→ 99.999999999% durability (11 nines) 🏆
```

In plain English: if you store **10 million files**, you'd expect to lose **1 file every 10 million years.**

---

## Chapter 3 Summary 🎯

```
Durability Arsenal:

✅ 3x Replication          → survive disk/server failures
✅ Cross-rack placement    → survive rack failures  
✅ Geo-replication         → survive data center failures
✅ Quorum writes (W+R > N) → consistency + speed balance
✅ Heartbeat detection     → auto-detect dead nodes
✅ Self-healing            → auto re-replicate to maintain 3 copies
✅ Checksums + Scrubbing   → detect and fix silent corruption
```

---

## What's the next crisis? 🔥

You now have **durable, chunked storage**. But 500 million users are hammering your system. Every upload/download goes through your servers. You're spending a fortune on bandwidth, and users in Tokyo are getting slow downloads because your servers are in Virginia.

> *"How do we make downloads FAST for users all over the world, and stop paying for the same data to be transferred a million times?"*

**Chapter 4: CDN + Caching — How Netflix serves a video to 1 million people simultaneously without melting their servers.**

Say **"next"** when ready! 🚀

---

# ⚡ Chapter 4: CDN & Caching — Making Files Fast Everywhere

---

## The Story Continues...

Your storage system is durable now. But users are complaining:

```
User in Tokyo:     "Why does my file take 8 seconds to download?!" 😤
User in London:    "Videos keep buffering!" 😤  
User in Mumbai:    "Upload is fast but download is SO slow" 😤
```

You check your architecture. Every single download request travels like this:

```
User in Tokyo
     │
     │ ~150ms just for the round trip to US-East! 
     │ (speed of light limitation, ~10,000 km)
     ▼
US-East Data Center → fetch chunk from disk → send back
     │
     │ ~150ms back
     ▼
User in Tokyo finally gets data
```

**300ms+ per chunk × 512 chunks = minutes of latency just from geography.**

And the real kicker? 10,000 users in Tokyo are all downloading the **same popular file** — your server is sending the same bytes **10,000 times** across the Pacific Ocean. That's insane bandwidth cost.

---

## First Instinct: Add a Cache Layer 🗄️

An engineer says: *"What if we remember recently accessed files in fast memory (RAM) so we don't hit the disk every time?"*

### What is Caching?

```
Without Cache:                    With Cache:
                                  
Request → Disk Read (10ms)        Request → Cache check (0.1ms) → HIT! ✅
                                            └─ if MISS → Disk (10ms) → store in cache
```

RAM is **100x faster** than SSD, **1000x faster** than HDD.

### What do we cache?

Two totally different things need caching:

```
1. METADATA Cache (Redis/Memcached)
   → File names, ownership, chunk locations
   → "Who owns file_123? Where are its chunks?"
   → Small data, accessed very frequently
   → Perfect for Redis (in-memory key-value store)

2. CHUNK Cache (local disk/memory on edge servers)
   → Actual file bytes
   → Large data, cache only popular chunks
   → Perfect for CDN edge nodes
```

---

## The Metadata Cache in Action

```
User requests file info for "wedding_video.mp4"

Step 1: Check Redis cache
        Key: "file_metadata:FILE_123"
        
        CACHE HIT  → return in 0.1ms ✅
        CACHE MISS → query PostgreSQL (5-10ms) 
                   → store result in Redis with TTL
                   → return result
```

```javascript
// Pseudo-code for metadata fetch
async function getFileMetadata(fileId) {
  const cacheKey = `file_metadata:${fileId}`;
  
  // Try cache first
  const cached = await redis.get(cacheKey);
  if (cached) return JSON.parse(cached);       // ~0.1ms ⚡
  
  // Cache miss - hit the database
  const metadata = await postgres.query(
    "SELECT * FROM files WHERE id = ?", [fileId]
  );
  
  // Store in cache for 5 minutes
  await redis.set(cacheKey, JSON.stringify(metadata), "EX", 300);
  
  return metadata;                              // ~10ms
}
```

### Cache Invalidation — The Hardest Problem in CS 😅

What if a user **renames** or **deletes** a file while it's cached?

```
T=0:  File "report.pdf" cached → { name: "report.pdf", owner: "priya" }
T=1:  Priya renames it to "final_report.pdf" in the DB
T=2:  Another user requests file → gets STALE cached name "report.pdf" ❌
```

Solutions:
```
Option A: TTL (Time To Live)
→ Cache expires after 5 mins automatically
→ Simple but stale data window exists

Option B: Write-through invalidation  
→ When file is updated → immediately delete cache entry
→ Next request rebuilds cache from DB
→ More complex but always fresh ✅

// On file rename:
await postgres.update("UPDATE files SET name=? WHERE id=?", [...]);
await redis.del(`file_metadata:${fileId}`);  // ← invalidate immediately
```

Google Drive uses **write-through invalidation** for critical metadata.

---

## Now The Bigger Problem: Geography 🌍

Caching helps with speed inside your data center. But it doesn't solve the **Tokyo user waiting 300ms** just because of physical distance.

You need the data to be **physically closer to the user.**

This is where **CDN (Content Delivery Network)** comes in.

---

## What is a CDN? 🌐

A CDN is a network of servers **spread all over the world**, each storing copies of popular content. When a user requests a file, they get it from the **nearest CDN node** instead of your origin server.

```
Without CDN:                      With CDN:
                                  
Tokyo User                        Tokyo User
    │ 150ms                           │ 5ms
    ▼                                 ▼
US-East Server                    CDN Node (Tokyo)
                                      │ (already has the file!)
                                      │ serves instantly ⚡
```

### CDN Network Visualization

```
                    Your Origin Server
                      (US-East DC)
                           │
          ┌────────────────┼────────────────┐
          │                │                │
    ┌─────▼─────┐    ┌─────▼─────┐   ┌─────▼─────┐
    │ CDN Node  │    │ CDN Node  │   │ CDN Node  │
    │ New York  │    │  London   │   │  Tokyo    │
    └─────┬─────┘    └─────┬─────┘   └─────┬─────┘
          │                │                │
    ┌─────▼─────┐    ┌─────▼─────┐   ┌─────▼─────┐
    │ CDN Node  │    │ CDN Node  │   │ CDN Node  │
    │  Chicago  │    │  Paris    │   │ Singapore │
    └───────────┘    └───────────┘   └───────────┘
    
Users connect to whichever node is geographically closest.
Cloudflare has 300+ such nodes. Akamai has 4000+.
```

---

## How CDN Works: The First Request vs Repeat Requests

### First time anyone in Tokyo requests a file:

```
Tokyo User → Tokyo CDN Node
                  │ "I don't have this file" (CACHE MISS)
                  │
                  └──→ Origin Server (US-East)
                              │ sends the file
                              ▼
                       Tokyo CDN Node
                       stores it locally 💾
                              │
                              ▼
                       Tokyo User ✅
                       (300ms this one time)
```

### Every subsequent request from anyone in Japan/Asia:

```
Tokyo User 2  ──→ Tokyo CDN Node → "Got it!" → serves in 5ms ⚡
Osaka User    ──→ Tokyo CDN Node → "Got it!" → serves in 8ms ⚡
Seoul User    ──→ Tokyo CDN Node → "Got it!" → serves in 12ms ⚡
```

The origin server is **never hit again** for this file from Asian users. 

---

## What Files Should Go on CDN? 🤔

Not everything belongs on a CDN. Think about it:

```
✅ GOOD for CDN:
   → Publicly shared files (anyone can access)
   → Frequently downloaded files (popular docs, images)
   → Static content (thumbnails, previews)
   → Large media files (videos, high-res images)

❌ BAD for CDN:
   → Private files (security risk — wrong person might get them)
   → Rarely accessed files (wastes CDN storage)
   → Files that change very frequently (constant cache invalidation)
```

For **private files** (most of Google Drive), you use a different trick — **signed URLs**.

---

## Signed URLs — Secure Temporary Access 🔐

Here's the problem with private files on CDN:

```
Priya's private file is on CDN Node Tokyo.
How do we make sure only Priya (and people she shared with) can access it?
We can't just give a public URL — anyone could download it!
```

Solution: **Pre-signed URLs with expiry**

```
Step 1: Priya requests to download her file
        → Goes to your API server (authenticated with her token)
        
Step 2: API server generates a signed URL:
        "https://cdn.yourdrive.com/chunks/a3f8c2
         ?expires=1705320000          ← valid for 15 minutes only
         &signature=HMAC(secret_key, chunk_id + expiry)"
         
Step 3: API returns this signed URL to Priya's client

Step 4: Priya's client downloads directly from CDN using this URL
        → CDN verifies the signature (can't be faked without secret key)
        → CDN checks expiry (can't be reused after 15 mins)
        → Serves the file ✅
        
Step 5: If someone steals this URL and tries after 15 mins → 403 Forbidden ❌
```

```
Why is this brilliant?
→ Your API server handles AUTH (knows who Priya is)
→ CDN handles BANDWIDTH (serves the actual bytes)
→ Your servers are never in the data transfer path → saves huge bandwidth cost!
```

This is **exactly how AWS S3 pre-signed URLs work**, and how Google Drive generates download links.

---

## Cache Eviction — CDN Storage is Finite

CDN nodes have limited storage. They can't keep everything forever. When storage fills up, they need to evict (remove) some cached content. Which ones?

### LRU — Least Recently Used (Most Common)

```
CDN Node has 5 slots. All full:
[FileA: used 1hr ago][FileB: 2hr][FileC: 5hr][FileD: 8hr][FileE: 10hr ago]

New file (FileF) arrives → evict FileE (least recently used)
[FileA: used 1hr ago][FileB: 2hr][FileC: 5hr][FileD: 8hr][FileF: just now]
```

### LFU — Least Frequently Used

```
Evict the file that has been accessed the FEWEST times overall.
Better for files that were popular once but never again (trending news article).
```

### In Practice:
Most CDNs use a **combination** — considering both recency AND frequency. 
Netflix reportedly uses a custom algorithm that also considers **file size** 
(don't evict a 1GB video to make room for a 10KB image).

---

## The Full Download Flow (Now with CDN) 🔄

```
Priya clicks download on wedding_video.mp4
              │
              ▼
     ┌─────────────────┐
     │   API Server    │  ← validates auth, checks permissions
     └────────┬────────┘
              │ generates signed URLs for all 512 chunks
              ▼
     ┌─────────────────┐
     │  Metadata DB /  │  ← "FILE_123 has chunks: [a3f8c2, 91bc4e, ...]"
     │  Redis Cache    │
     └────────┬────────┘
              │ returns 512 signed URLs (one per chunk)
              ▼
     Client downloads all 512 chunks IN PARALLEL from CDN
              │
    ┌─────────┼─────────┐
    ▼         ▼         ▼
 Chunk 1   Chunk 2   Chunk 3  ...  (parallel streams)
 from CDN  from CDN  from CDN
    │         │         │
    └─────────┴─────────┘
              │
     Client reassembles chunks in order
              │
              ▼
     wedding_video.mp4 fully downloaded ✅ (fast, from nearby CDN)
```

---

## Chapter 4 Summary 🎯

```
Speed Arsenal:

✅ Metadata Cache (Redis)    → 0.1ms lookups vs 10ms DB queries
✅ CDN Edge Nodes            → serve from 5ms away vs 300ms across ocean
✅ Signed URLs               → security without routing through your servers
✅ Parallel chunk downloads  → use full bandwidth, not serial
✅ Cache invalidation        → no stale data on updates
✅ LRU/LFU eviction          → smart use of finite CDN storage

Cost savings:
→ Without CDN: Every download hits your origin → you pay $0.09/GB bandwidth
→ With CDN:    95% of downloads served by CDN → you pay $0.01/GB (CDN is cheaper)
→ At petabyte scale: millions of dollars saved monthly 💰
```

---

## What breaks next? 🔥

You've got fast, durable, chunked storage. But now look at your **Metadata Database**:

```
500M users × avg 1000 files each = 500 Billion rows in your Files table 😱
Every upload, download, rename, share → hits this one database
Peak traffic: 23,000 writes/second + 100,000 reads/second
```

A single PostgreSQL server **cannot handle this.** It will melt. 🔥

> *"How do you split your database across hundreds of machines while keeping queries fast and data consistent?"*

**Chapter 5: Database Sharding & Scaling — The art of splitting data without losing your mind.**

Say **"next"** when ready! 🚀

---

# 🗄️ Chapter 5: Database Sharding & Scaling — Splitting Data Across Machines

---

## The Story Continues...

Everything is working great. Then your DBA (Database Admin) calls you at 9 AM:

```
DBA: "Our PostgreSQL server is at 98% CPU."
You: "Can we just add more RAM?"
DBA: "We're already at 512GB RAM. It's a $40,000 machine."
You: "Can we add a faster disk?"
You: "..."
DBA: "We've hit the ceiling. One machine simply cannot handle this anymore."
```

This is called hitting the **Vertical Scaling Limit.**

```
Vertical Scaling (Scale Up):        Horizontal Scaling (Scale Out):
                                    
[Small Server]                      [Server][Server][Server]
     ↓ upgrade                           ↑ add more machines
[Bigger Server]                     [Server][Server][Server]
     ↓ upgrade
[Massive Server] ← you are here     ← this is where we need to go
     ↓ ???
 NO MORE UPGRADES POSSIBLE 😭
```

You need to **split your database across multiple machines.** This is called **Sharding.**

---

## First, Let's Talk About Read Replicas (The Easy Win)

Before sharding (which is complex), you try something simpler first.

Look at your traffic pattern:
```
Reads  (downloads, file listings) : 85% of queries
Writes (uploads, renames, deletes): 15% of queries
```

**Most traffic is reads!** So you add **Read Replicas:**

```
                ┌─────────────────┐
                │  Primary DB     │  ← ALL writes go here
                │  (Read + Write) │
                └────────┬────────┘
                         │ continuously replicates
          ┌──────────────┼──────────────┐
          │              │              │
   ┌──────▼──────┐ ┌─────▼──────┐ ┌────▼───────┐
   │  Replica 1  │ │ Replica 2  │ │ Replica 3  │
   │ (Read Only) │ │ (Read Only)│ │ (Read Only)│
   └─────────────┘ └────────────┘ └────────────┘
   
Reads are distributed across replicas → 3x read capacity instantly!
Writes still go to primary only.
```

This buys you time. But **writes are still bottlenecked** on one primary. And your table has **500 billion rows** — even reads on one machine are slow because the indexes are massive.

You need **Sharding.**

---

## What is Sharding? 🍕

Sharding = splitting your data **horizontally** across multiple database machines, where **each machine owns a subset of the rows.**

Think of it like a phone book:

```
Instead of one massive phone book (A-Z):

Shard 1: A - F   → DB Machine 1
Shard 2: G - M   → DB Machine 2  
Shard 3: N - S   → DB Machine 3
Shard 4: T - Z   → DB Machine 4

Each machine handles only 25% of the data and queries!
```

Now the question is: **how do you decide which row goes to which shard?**
This is the **Sharding Strategy** — and choosing wrong will haunt you forever.

---

## Sharding Strategy 1: Range-Based Sharding

Split data based on a range of values.

```
Shard by user_id ranges:

Shard 1: user_id 1        → 25,000,000      → DB Machine 1
Shard 2: user_id 25M+1    → 50,000,000      → DB Machine 2
Shard 3: user_id 50M+1    → 75,000,000      → DB Machine 3
Shard 4: user_id 75M+1    → 100,000,000     → DB Machine 4
```

**Looks clean. But watch what happens:**

```
Your app launches. First users sign up.
user_id 1, 2, 3, 4, 5... → ALL go to Shard 1

Shard 1: 🔥🔥🔥 OVERLOADED (all new signups)
Shard 2: 😴 empty
Shard 3: 😴 empty
Shard 4: 😴 empty
```

This is called a **Hot Shard** problem — one shard gets all the traffic while others sit idle. Range-based sharding is simple but creates uneven load.

---

## Sharding Strategy 2: Hash-Based Sharding ⭐

Take the user_id, **hash it**, and use the hash to pick a shard.

```
shard_number = hash(user_id) % total_shards

Examples (4 shards):
user_id = 1    → hash = 8392810  → 8392810 % 4 = 2  → Shard 2
user_id = 2    → hash = 1923847  → 1923847 % 4 = 3  → Shard 3
user_id = 3    → hash = 5839201  → 5839201 % 4 = 1  → Shard 1
user_id = 4    → hash = 2910483  → 2910483 % 4 = 0  → Shard 0
user_id = 5    → hash = 7382910  → 7382910 % 4 = 2  → Shard 2
```

Hash functions distribute data **uniformly** — no hot shards!

```
Shard 0: ~25% of users  ✅
Shard 1: ~25% of users  ✅
Shard 2: ~25% of users  ✅
Shard 3: ~25% of users  ✅
```

**This is what Google Drive, Instagram, and most large systems use.**

---

## But Hash Sharding Has a Dark Secret... 😈

Everything works great with 4 shards. Then you grow and need to add a 5th shard.

```
Before (4 shards): shard = hash(user_id) % 4
After  (5 shards): shard = hash(user_id) % 5

user_id = 1: was on Shard 2 → now goes to Shard 1  ← DIFFERENT!
user_id = 2: was on Shard 3 → now goes to Shard 2  ← DIFFERENT!
user_id = 3: was on Shard 1 → now goes to Shard 3  ← DIFFERENT!
```

**Almost every row maps to a different shard!** You'd have to move ~80% of your data to new shards. For a 500-billion-row database, that's a **migration that takes weeks** and your system is broken the whole time. 😱

The solution? **Consistent Hashing.**

---

## Consistent Hashing — The Elegant Solution 🎯

Instead of `hash % N`, imagine a **circular ring** of hash values from 0 to 2³²:

```
                    0
                  ──┬──
            2³²/8   │   2³²/8
               ╲    │    ╱
        3×2³²/8  ╲  │  ╱  2×2³²/8
          ────────[  RING  ]────────
        5×2³²/8  ╱  │  ╲  4×2³²/8
               ╱    │    ╲
            7×2³²/8   6×2³²/8
                  ──┴──
```

Place your **DB nodes** on the ring by hashing their names:

```
hash("DB-Node-A") = 92         → placed at position 92 on ring
hash("DB-Node-B") = 213        → placed at position 213
hash("DB-Node-C") = 310        → placed at position 310
```

To find which node stores a user's data:
```
hash(user_id) = 150
→ Walk clockwise on ring from 150
→ First node you hit = DB-Node-B (at 213)
→ user goes to DB-Node-B ✅
```

### Now add a new node (DB-Node-D at position 260):

```
Before:                          After adding Node-D at 260:

Node-B owns: 92→213             Node-B owns: 92→213  (unchanged ✅)
Node-C owns: 213→310            Node-D owns: 213→260  (new)
                                 Node-C owns: 260→310  (smaller range)

Only data between 213-260 moves from Node-C to Node-D!
That's ~16% of data, not 80%! 🎉
```

**Consistent hashing means adding/removing nodes only moves a minimal amount of data.** This is used by **Cassandra, DynamoDB, Amazon S3.**

---

## What is our Sharding Key for the File System?

This is a critical interview question. The choice of shard key determines everything.

### Option A: Shard by user_id

```
All files belonging to user_42 → live on Shard 7

Pros:
✅ "List all files for user_42" → hits only 1 shard (fast!)
✅ "Get file owned by user_42" → hits only 1 shard
✅ Natural data locality

Cons:
❌ A celebrity with 10M shared files → hot shard
❌ Uneven data if some users store 1TB and others store 1MB
```

### Option B: Shard by file_id

```
Each file hashed independently, spread across all shards

Pros:
✅ Perfectly even distribution
✅ No hot shards

Cons:
❌ "List all files for user_42" → must query ALL shards and merge results
   (called a scatter-gather query — expensive!)
```

### Option C: Composite Key (user_id + file_id) ⭐ Best for file systems

```
Shard key = hash(user_id) + file_id as sort key within shard

All of a user's files land on the same shard (locality)
Files within a user sorted by file_id (efficient range scans)

This is how Google Bigtable and DynamoDB design it:
Row key = "user_42#file_123"
```

```
Shard 1:  user_1#file_*, user_8#file_*, user_15#file_*
Shard 2:  user_2#file_*, user_6#file_*, user_19#file_*
...

Query "all files for user_8" → hash(user_8) → Shard 1 → range scan ✅
Query "file_123 of user_8"   → hash(user_8) → Shard 1 → point lookup ✅
```

**This is the approach used in production file storage systems.**

---

## Cross-Shard Queries — The Painful Reality

Some queries naturally span multiple shards. Example:

```
"Find all files shared with user_42 by OTHER users"

user_42's files → Shard 1
Files shared BY other users → could be on ANY shard!

You must query all shards → merge results → sort → return
```

This is a **scatter-gather** query and it's slow. How do you handle it?

### Solution: Separate Index Table for Sharing

```
Instead of scanning all shards, maintain a dedicated "shared_with" index:

shared_files_index table (sharded by recipient user_id):
┌──────────────────┬──────────┬──────────┬─────────────┐
│ recipient_user   │ file_id  │ owner_id │ permissions │
├──────────────────┼──────────┼──────────┼─────────────┤
│ user_42          │ file_890 │ user_17  │ read        │
│ user_42          │ file_445 │ user_33  │ edit        │
└──────────────────┴──────────┴──────────┴─────────────┘

"Files shared with user_42"
→ hash(user_42) → find shard for this index
→ single shard lookup ✅
```

**Denormalization** (storing data redundantly for query efficiency) is a key pattern in sharded systems. You trade storage for query speed.

---

## Handling the Metadata DB Schema (Full Picture)

```
Files Table          (sharded by user_id)
┌──────────┬─────────┬──────────┬────────┬───────────┬──────────┐
│ file_id  │ user_id │ name     │ size   │ parent_id │ status   │
├──────────┼─────────┼──────────┼────────┼───────────┼──────────┤
│ f_123    │ u_42    │ report   │ 4MB    │ folder_5  │ active   │
└──────────┴─────────┴──────────┴────────┴───────────┴──────────┘

Chunks Table         (sharded by chunk_hash — content addressed)
┌──────────────┬───────────────────────────┬────────────┐
│ chunk_hash   │ storage_locations         │ size       │
├──────────────┼───────────────────────────┼────────────┤
│ a3f8c2...    │ [node7, node12, node_EU1] │ 4MB        │
└──────────────┴───────────────────────────┴────────────┘

File_Chunks Table    (sharded by file_id, co-located with files)
┌──────────┬─────────────┬──────────────┐
│ file_id  │ chunk_index │ chunk_hash   │
├──────────┼─────────────┼──────────────┤
│ f_123    │ 0           │ a3f8c2...    │
│ f_123    │ 1           │ 91bc4e...    │
└──────────┴─────────────┴──────────────┘
```

---

## Chapter 5 Summary 🎯

```
Scaling the Database:

✅ Read Replicas           → scale reads (85% of traffic) instantly
✅ Hash-based Sharding     → distribute writes evenly
✅ Consistent Hashing      → add/remove shards with minimal data movement
✅ Composite Shard Keys    → keep related data together (user_id + file_id)
✅ Denormalized Index      → avoid expensive scatter-gather for sharing queries

The Golden Rule of Sharding:
→ Pick a shard key that keeps related data together
  AND distributes load evenly
→ These goals often conflict — find the right tradeoff for your access patterns
```

---

## What breaks next? 🔥

Your database scales beautifully now. But then you realize:

```
User A uploads a file → "Upload successful! ✅"
User A immediately opens the file list → file is NOT there! 😱

User edits a document
→ Saves from laptop  ✅
→ Opens on phone     → sees OLD version ❌
```

Your **replicas are slightly behind** the primary. Your shards are **not perfectly in sync**. Users are experiencing the dark side of distributed systems:

> *"How do you make sure every user always sees the correct, up-to-date version of their files — even across replicas, shards, and data centers?"*

**Chapter 6: Consistency Models — The battle between "always correct" and "always fast", and how real systems choose their battles.**

Say **"next"** when ready! 🚀


---

# 🔄 Chapter 6: Consistency Models — The Battle Between Correct and Fast

---

## The Story Continues...

Your system is scaled. Millions of users are happy. Then you get this bug report:

```
User: "I just uploaded my resume.pdf and immediately 
       opened my file list — it's NOT there! I thought 
       I lost it! I panicked and uploaded it again. 
       Now I have duplicates!"

Another user: "I edited my project_plan.doc on my laptop,
               saved it, then opened it on my phone 
               30 seconds later — I'm seeing yesterday's version!"
```

You dig into the logs:

```
T=0ms:   Write "resume.pdf" → Primary DB ✅
T=5ms:   Client gets "Upload successful!" response
T=8ms:   Client queries file list → hits Replica 2
T=12ms:  Replica 2 hasn't received the update yet 😱
T=150ms: Replica 2 finally syncs → file appears
```

There's a **150ms gap** where the data exists on primary but not on replicas. The user queried during that gap.

This is the **consistency problem** — and it's one of the most fundamental challenges in distributed systems.

---

## First, Let's Understand WHY This Happens

In a distributed system, data lives on multiple machines. When you update data, you update **one machine first**, then it propagates:

```
Write "resume.pdf"
        │
        ▼
   Primary DB         ← updated immediately (T=0ms)
        │
        │ replication lag (network delay, disk writes)
        │
   ┌────┴───────────────────┐
   │                        │
Replica 1               Replica 2
(updated at T=80ms)     (updated at T=150ms)
```

During this propagation window, different users reading from different replicas see **different versions of reality.** This is called **Eventual Consistency** — eventually all replicas will agree, but not immediately.

---

## The CAP Theorem — The Fundamental Law 📜

In 2000, Eric Brewer stated something that became the bible of distributed systems:

> **"A distributed system can only guarantee 2 out of these 3 properties simultaneously:"**

```
        C — Consistency
       ╱ (every read returns the 
      ╱   most recent write)
     ╱
    ●
   ╱ ╲
  ╱   ╲
 ╱     ╲
●───────●
A       P
Availability    Partition Tolerance
(system always  (system works even when
responds)       network splits occur)
```

### What is a Network Partition?

```
Data Center 1 ════╪════ Data Center 2
                  │
              Network
               Cable
                CUTS ✂️
                  
Now DC1 and DC2 can't talk to each other.
What does your system do?
```

**Partition Tolerance (P) is NOT optional.** Networks WILL fail. Cables get cut. Routers crash. You MUST handle partitions. So really you're choosing between **CP or AP:**

```
CP System (Consistency + Partition Tolerance):
→ During a partition, refuse to answer rather than give stale data
→ "I'd rather be unavailable than wrong"
→ Example: Bank balance, file version conflicts

AP System (Availability + Partition Tolerance):  
→ During a partition, answer with possibly stale data
→ "I'd rather give an old answer than no answer"
→ Example: Social media likes count, search suggestions
```

### Where does a File Storage System fall?

```
For FILE METADATA (who owns what, sharing permissions):
→ CP: Must be consistent. 
  Wrong permissions = security breach.
  
For FILE CONTENT (the actual bytes):  
→ AP leaning: Showing a slightly old version briefly is OK.
  Better than refusing to serve the file at all.

For UPLOAD CONFIRMATION:
→ CP: "Your file is saved" must be true. 
  Can't lie to the user.
```

**Real systems (Google Drive, Dropbox) make different consistency choices for different parts of the system.** This nuance is what impresses interviewers.

---

## The Consistency Spectrum

It's not just "consistent or not." There's a whole spectrum:

```
STRONG                                                    WEAK
◄──────────────────────────────────────────────────────────►

Linearizability → Sequential → Causal → Read-your-writes → Eventual
   (strongest)                                            (weakest)
```

Let's understand each with real examples from our file system:

---

## Level 1: Eventual Consistency (Weakest)

```
"All replicas will EVENTUALLY agree, but no guarantee on when."

Example:
T=0:   Priya uploads photo.jpg
T=5ms: Priya's friend in Tokyo requests file list
       → might NOT see photo.jpg yet
T=200ms: Tokyo replica syncs → photo.jpg appears

Is this OK? For a social media feed? YES.
For a file you just uploaded? Annoying but tolerable.
For security permissions? ABSOLUTELY NOT.
```

Amazon's DynamoDB defaults to eventual consistency (but offers strong as an option).

---

## Level 2: Read-Your-Writes Consistency ⭐

```
"After YOU write something, YOU will always see your own write.
 Other users might not see it yet, but YOU always do."

This directly solves the bug report we started with!

How to implement:
→ After a write, store a token: "user_42 wrote at timestamp T"
→ When user_42 reads, route to Primary DB (or a replica 
  that has caught up to timestamp T)
```

```
Implementation using "sticky sessions" or "read-after-write tokens":

User uploads file:
→ Primary DB write ✅
→ Server returns: { success: true, write_token: "T=1705320150" }
→ Client stores this token

User's next read request:
→ Client sends: { request: "list files", min_freshness: "T=1705320150" }
→ Load balancer routes to a replica with data ≥ T=1705320150
→ User sees their own upload ✅
→ Other users might still get eventual consistency (that's OK)
```

**This is the minimum consistency guarantee any decent file system must provide.** Google Drive implements this.

---

## Level 3: Causal Consistency

```
"Operations that are causally related must be seen in order."

Example:
T=1: User creates folder "Project"
T=2: User uploads "report.pdf" INTO "Project" folder

Causal relationship: report.pdf DEPENDS ON Project folder existing.

With causal consistency:
→ Any user who sees "report.pdf" MUST also see "Project" folder
→ You can never see the file without the folder it's in

Without causal consistency:
→ User opens shared link to "Project/report.pdf"
→ Sees the file but "Project" folder doesn't exist yet on their replica 
→ Broken state! 😱
```

This is implemented using **vector clocks** or **logical timestamps:**

```
Each write carries a vector clock:

Create folder:  { event: "mkdir Project",  clock: {server1: 1} }
Upload file:    { event: "upload report",  clock: {server1: 2}, 
                  depends_on: {server1: 1} }

Any replica that gets "upload report" MUST first apply
the event with clock {server1: 1} → causal order guaranteed ✅
```

---

## Level 4: Strong Consistency / Linearizability (Strongest)

```
"Every read returns the MOST RECENT write, period.
 The system behaves as if there's only ONE copy of the data."

Example: File sharing permissions

T=0: Owner revokes Bob's access to secret.pdf
T=1: Bob tries to download secret.pdf
     → Must be DENIED, no matter which replica Bob hits
     
If any replica could serve a stale "Bob has access" response
→ Security breach → unacceptable
```

How to achieve this? **Consensus protocols:**

### Raft Consensus (Used by etcd, CockroachDB)

```
5 nodes. One is elected Leader.

Write request comes in:
1. Client writes to Leader
2. Leader sends write to all followers: "Hey, write this"
3. Wait for majority (3/5) to confirm: "Got it!"
4. Leader commits and responds "Success" to client
5. Followers apply the write

          Client
            │ write
            ▼
         [Leader]
        ╱    │    ╲
[Node2]  [Node3]  [Node4]  [Node5]
  ✅       ✅       ...      ...
  
3/5 confirmed → committed ✅

Even if 2 nodes are down, system works (3 remaining = majority).
Even if Leader dies, nodes elect a new Leader automatically.
```

The **tradeoff:** Every write now requires a network round trip to majority of nodes. **Slower but always correct.**

---

## The Real-World Solution: Different Consistency Per Operation

Here's how a production file storage system actually handles this:

```
Operation                  Consistency Level      Why
─────────────────────────────────────────────────────────────────
Upload file                Read-your-writes      User must see own upload
List user's files          Read-your-writes      Own file list must be fresh  
Download file content      Eventual              Serving slightly old bytes OK
Check permissions          Strong (Linearizable) Security cannot be stale
Create shared link         Causal               Link must outlive creation
Delete/revoke access       Strong (Linearizable) Security — revoke must be instant
File version history       Eventual              Historical data, staleness OK
Storage quota check        Strong               Must not allow over-quota uploads
```

---

## Solving the Original Bug: Practical Implementation

Remember the user who couldn't see their uploaded file immediately?

```
Solution: Read-your-writes with Write Tokens

UPLOAD FLOW:
Client → API Server → Write to Primary DB
                    → Get write timestamp: T=1705320150423
                    → Return to client: { 
                        success: true, 
                        file_id: "f_123",
                        consistency_token: "1705320150423" 
                      }

Client stores token in local state/cookie.

LIST FILES FLOW:
Client → API Server (sends consistency_token: "1705320150423")
       → Server checks: "Do I have a replica caught up to T=...423?"
       → YES → serve from that replica ✅
       → NO  → route to Primary DB (slower but correct)
```

This way:
- **Fresh writes** go to primary (correct but slightly slower)
- **Old reads** go to replicas (fast, eventual consistency fine)
- **User's own writes** always visible to themselves ✅

---

## Conflict Resolution — What if Two Users Edit the Same File? ✏️

```
T=0:  File "doc.txt" content = "Hello World"
T=1:  User A (offline on train) edits → "Hello World from Alice"
T=2:  User B edits online → "Hello World from Bob"  ← synced to server
T=3:  User A comes back online → tries to sync their version
      
Server has Bob's version. Alice has a different version.
CONFLICT! 💥 Which one wins?
```

### Strategy 1: Last Write Wins (LWW)

```
Compare timestamps. Latest timestamp wins.

T=2 (Bob) vs T=1 (Alice) → Bob wins. Alice's changes LOST. 😢

Simple but lossy. Good for: profile pictures, settings.
Bad for: documents (you lose work!)
```

### Strategy 2: Operational Transformation (Google Docs approach)

```
Instead of storing full document versions, store the OPERATIONS:

Alice's op: INSERT "from Alice" at position 12
Bob's op:   INSERT "from Bob" at position 12

Transform Alice's op relative to Bob's already-applied op:
→ Bob inserted 8 chars at position 12
→ Alice's insert position shifts to 12+8 = 20

Final result: "Hello World from Bob from Alice" ✅

Both changes preserved! This is how Google Docs real-time 
collaboration works.
```

### Strategy 3: Create a Conflict Copy (Dropbox approach)

```
Don't try to merge. Keep BOTH versions.

Result:
→ doc.txt                           ← Bob's version (server version)
→ doc (Alice's conflicted copy).txt ← Alice's version

Let the human decide which one to keep.
Simple, never loses data, but requires human resolution.
```

---

## Chapter 6 Summary 🎯

```
Consistency Toolkit:

✅ CAP Theorem              → you're always choosing between C and A during partitions
✅ Eventual Consistency     → fast, replicas catch up "eventually" 
✅ Read-Your-Writes         → minimum bar — users see their own changes
✅ Causal Consistency       → causally related ops stay in order (folder before files)
✅ Strong Consistency       → always correct, used for permissions/security
✅ Raft Consensus           → how to achieve strong consistency across nodes
✅ Different consistency    → different operations need different guarantees
   per operation
✅ Conflict resolution      → LWW / OT / Conflict copies for concurrent edits

The Key Insight for Interviews:
→ Don't say "we'll use strong consistency everywhere"
   (too slow, interviewer knows you don't understand tradeoffs)
→ Don't say "we'll use eventual consistency everywhere"  
   (security nightmare)
→ Say "we apply strong consistency where correctness is critical,
   and eventual/read-your-writes where performance matters more"
   ← THIS is the senior engineer answer 🏆
```

---

## What Breaks Next? 🔥

Your system is consistent, durable, fast, and scaled. Users love it. 

Then your product manager asks:

```
PM: "Can we add version history? Users want to see 
     old versions of their files."
     
You: "Sure, we just keep old file versions in storage..."

PM: "Also can we support real-time collaboration 
     like Google Docs?"

You: "Uh..."

PM: "And if a user accidentally deletes a file, 
     can they restore it within 30 days?"

You: "..."
```

> *"How do you design versioning, file recovery, and real-time sync without exploding your storage costs?"*

**Chapter 7: Versioning, Sync Engine & Real-time Collaboration — How Google Drive knows exactly what changed, when, and can reconstruct any past state.**

Say **"next"** when ready! 🚀

---

# 🕐 Chapter 7: Versioning, Sync & Real-time Collaboration

---

## The Story Continues...

Your PM drops three requirements on your desk:

```
1. "Keep 30 days of version history for every file"
2. "Let multiple people edit a doc at the same time (like Google Docs)"
3. "Sync files across all devices — phone, laptop, tablet — instantly"
```

You open a spreadsheet and do quick math on requirement #1:

```
500M users × avg 10 files edited/day × avg 2MB per file
= 10 Billion MB = 10 Petabytes of NEW storage PER DAY

30 days × 10 PB = 300 Petabytes just for version history 😱

At $0.02/GB storage cost = $6,000,000/month JUST for versions
```

Clearly you can't just save a full copy of every version. You need to be smarter.

---

## The Naive Approach: Store Full Copies ❌

```
User edits report.pdf (10MB) 5 times today:

Version 1: report_v1.pdf  → 10MB stored
Version 2: report_v2.pdf  → 10MB stored  (maybe changed 3 lines)
Version 3: report_v3.pdf  → 10MB stored  (maybe changed 1 word)
Version 4: report_v4.pdf  → 10MB stored
Version 5: report_v5.pdf  → 10MB stored

Total: 50MB stored for what is essentially 10MB of data + tiny edits
90% of storage is DUPLICATE data 😭
```

There has to be a smarter way. And there is — **Delta Encoding.**

---

## Delta Encoding — Store Only What Changed 🔬

Instead of storing full copies, store the **original + a series of changes (deltas).**

```
Version 1 (full copy):  "The quick brown fox jumps"          → 25 bytes stored
Version 2 (delta):      REPLACE "brown" WITH "red"           → 20 bytes stored
Version 3 (delta):      INSERT " lazy" BEFORE "fox"          → 18 bytes stored
Version 4 (delta):      DELETE "quick "                      → 15 bytes stored

Total: 25 + 20 + 18 + 15 = 78 bytes

vs Full copies: 25 × 4 = 100 bytes (and scales horribly for large files!)
```

For a **10MB PDF** where user only changed a paragraph:

```
Full copy approach:   10MB × 5 versions = 50MB
Delta approach:       10MB + 4 × (avg 2KB delta) = 10.008MB

Storage savings: 99.98%  🎉
```

---

## How Deltas Work at the Chunk Level

Remember our chunking from Chapter 2? Deltas and chunks combine beautifully:

```
Original file: [C1][C2][C3][C4][C5]  (5 chunks × 4MB = 20MB)
User edits middle section (only C3 changes):

Version 2:     [C1][C2][C3'][C4][C5]
                        ↑
                   only this chunk is new!

What we store for Version 2:
→ C1: same hash → point to existing chunk (0 bytes extra!)
→ C2: same hash → point to existing chunk (0 bytes extra!)
→ C3': NEW hash → store new 4MB chunk
→ C4: same hash → point to existing chunk (0 bytes extra!)
→ C5: same hash → point to existing chunk (0 bytes extra!)

Extra storage for Version 2: 4MB (just one new chunk!)
instead of 20MB for the whole file ✅
```

This is called **Copy-on-Write (COW)** — unchanged chunks are shared between versions.

---

## Versioning Data Model

```
file_versions table:
┌──────────┬────────────┬─────────────────────┬────────────┬───────────────┐
│ file_id  │ version_id │ chunk_map           │ size       │ created_at    │
├──────────┼────────────┼─────────────────────┼────────────┼───────────────┤
│ f_123    │ v1         │ [c1, c2, c3, c4, c5]│ 20MB       │ Jan 1 09:00  │
│ f_123    │ v2         │ [c1, c2, c3',c4, c5]│ 20MB       │ Jan 1 11:00  │
│ f_123    │ v3         │ [c1, c2, c3',c4',c5]│ 20MB       │ Jan 1 14:00  │
└──────────┴────────────┴─────────────────────┴────────────┴───────────────┘

chunks table:
┌──────┬────────────────────────────────────────────┐
│ hash │ storage_path                               │
├──────┼────────────────────────────────────────────┤
│ c1   │ /store/c1.bin  ← shared by v1, v2, v3      │
│ c2   │ /store/c2.bin  ← shared by v1, v2, v3      │
│ c3   │ /store/c3.bin  ← only v1 uses this         │
│ c3'  │ /store/c3p.bin ← shared by v2, v3          │
│ c4   │ /store/c4.bin  ← shared by v1, v2          │
│ c4'  │ /store/c4p.bin ← only v3 uses this         │
│ c5   │ /store/c5.bin  ← shared by v1, v2, v3      │
└──────┴────────────────────────────────────────────┘

Restoring v1 = fetch [c1, c2, c3, c4, c5] ✅
Restoring v2 = fetch [c1, c2, c3',c4, c5] ✅
No data duplication!
```

---

## Garbage Collection — When to Delete Old Chunks?

Here's a subtle problem. If user deletes version history after 30 days:

```
v1 is deleted → c3 is no longer referenced by ANY version
              → c3 can now be deleted from storage to free space

But how do you know c3 is unreferenced?
You can't delete it at delete-time (too slow, too risky).
```

Solution: **Reference Counting + Background GC**

```
chunks table gains a reference_count column:

┌──────┬─────────────────┐
│ hash │ reference_count │
├──────┼─────────────────┤
│ c1   │ 3  ← v1+v2+v3 all use it  │
│ c3   │ 1  ← only v1 uses it      │
│ c3'  │ 2  ← v2+v3 use it         │
└──────┴─────────────────┘

When v1 is deleted:
→ Decrement ref count for c1 (3→2), c2 (3→2), c3 (1→0), c4 (3→2), c5 (3→2)
→ c3 hits 0 → mark as "eligible for deletion"

Background GC job runs every hour:
→ Find all chunks with reference_count = 0
→ Verify (double-check nothing else points to them)
→ Delete from storage 🗑️
→ Free disk space reclaimed ✅
```

---

## Part 2: The Sync Engine — Keeping All Devices in Sync 📱💻🖥️

Now let's tackle the sync problem. Priya has:
- MacBook (editing a file)
- iPhone (should see changes)
- iPad (should see changes)
- Work PC (should see changes)

How does editing on MacBook instantly appear on all other devices?

---

## The Naive Approach: Polling ❌

```
Every device asks the server every N seconds: "Any changes?"

iPhone:  "Any changes?" → No  (T=0)
iPhone:  "Any changes?" → No  (T=5s)
iPhone:  "Any changes?" → YES (T=10s) ← picks up change

Problems:
→ 500M users × polling every 5 sec = 100M requests/second JUST for polling
→ Most responses are "No changes" → pure waste
→ 5-10 second delay before changes appear
```

---

## The Smart Approach: Long Polling + WebSockets

### Long Polling

```
Client → Server: "Any changes? I'll wait."
Server: [holds the connection open]
         ...
         ...10 seconds later: a change happens...
         ...
Server → Client: "YES! Here's the change!" (immediately)

Client processes change → immediately opens another long poll
Client → Server: "Any more changes? I'll wait."

Benefits:
→ Change delivered INSTANTLY when it happens
→ No wasted "No changes" responses
→ Works through firewalls (it's just HTTP)
```

### WebSockets (Even Better for Real-time)

```
Client ←————— persistent bidirectional connection ——————→ Server

Server can PUSH changes to client at ANY time
Client can PUSH changes to server at ANY time
No polling at all!

Perfect for real-time collaboration (Google Docs style)
```

```
Connection setup:
Client → "GET /sync HTTP/1.1"
         "Upgrade: websocket" 
Server → "101 Switching Protocols" ✅
         [connection stays open forever]
         
Server → Client: { type: "file_changed", file_id: "f_123", version: "v7" }
Client → Server: { type: "chunk_uploaded", chunk_hash: "a3f8c2", index: 3 }
```

---

## The Sync Protocol — What Actually Gets Synced?

A critical insight: **don't sync the whole file, sync the delta.**

```
Priya edits report.pdf on MacBook:

Step 1: Client computes which chunks CHANGED
        Old chunks: [c1, c2, c3, c4, c5]
        New chunks: [c1, c2, c3', c4, c5]  ← only c3 changed
        
Step 2: Client uploads ONLY changed chunks
        → Upload c3' (4MB) ← not the whole 20MB file!
        
Step 3: Server updates version metadata
        → file f_123 now at version v2 = [c1, c2, c3', c4, c5]
        
Step 4: Server notifies ALL other devices via WebSocket:
        { type: "file_updated", file_id: "f_123", 
          version: "v2", changed_chunks: ["c3'"] }
          
Step 5: iPhone, iPad, Work PC receive notification
        → Each downloads ONLY c3' (4MB) ← not 20MB!
        → Each reassembles locally ✅

Total bandwidth: 4MB × upload + 4MB × 3 devices = 16MB
Without delta sync: 20MB × upload + 20MB × 3 devices = 80MB
Savings: 80% bandwidth reduction! 🎉
```

---

## Part 3: Real-Time Collaboration — Multiple Editors 👥

Now the hardest part. Two people editing the **same document simultaneously.**

```
Document: "The cat sat on the mat"
                                    
Alice (position 4): DELETE "cat" → "The     sat on the mat"
Bob   (position 16): INSERT "blue " before "mat" → "The cat sat on the blue mat"

Both edits happen at T=100ms. Neither has seen the other's edit.
Both send their operations to the server simultaneously.
```

What should the final document look like?

```
Correct: "The     sat on the blue mat"  (both edits applied correctly)
Wrong:   "The blue cat sat on the mat"  (Bob's edit applied to wrong position)
         (because after Alice deleted "cat", positions shifted)
```

This is exactly the problem **Operational Transformation (OT)** solves.

---

## Operational Transformation (OT) — How Google Docs Works

The key idea: **before applying an operation, transform it relative to concurrent operations that came in first.**

```
Server receives (nearly simultaneously):
  Op A from Alice: DELETE(position=4, length=3)    "cat"
  Op B from Bob:   INSERT(position=16, text="blue "

Server applies Op A first (arbitrary choice, whoever arrived first):
Document becomes: "The     sat on the mat"
Positions have shifted! "mat" is now at position 12, not 16.

Server must TRANSFORM Op B relative to Op A:
Original Op B: INSERT(position=16, ...)
Alice deleted 3 chars before position 16
Transformed Op B: INSERT(position=16-3=13, text="blue ")

Apply transformed Op B:
"The     sat on the blue mat" ✅

Server sends to Alice: "Apply transformed Op B: INSERT(13, 'blue ')"
Alice's document also becomes: "The     sat on the blue mat" ✅

Both clients converge to the same state! 🎉
```

---

## The OT Architecture

```
                    ┌───────────────────────┐
                    │   Collaboration       │
                    │   Server              │
                    │                       │
                    │  - Maintains document │
                    │    state              │
                    │  - Transforms ops     │
                    │  - Broadcasts to all  │
                    └───────────┬───────────┘
                                │ WebSocket
              ┌─────────────────┼─────────────────┐
              │                 │                 │
    ┌─────────▼──────┐  ┌───────▼────────┐  ┌────▼───────────┐
    │ Alice's Client │  │  Bob's Client  │  │ Charlie's Client│
    │                │  │                │  │                 │
    │ Local copy of  │  │ Local copy of  │  │ Local copy of   │
    │ document +     │  │ document +     │  │ document +      │
    │ pending ops    │  │ pending ops    │  │ pending ops     │
    └────────────────┘  └────────────────┘  └─────────────────┘
```

Each client maintains:
```
{
  document_state: "current text...",
  server_version: 47,          ← last version we're synced to
  pending_ops: [op1, op2],     ← ops sent but not yet confirmed
  local_ops: [op3]             ← ops typed but not yet sent
}
```

---

## CRDTs — The Modern Alternative to OT

OT is complex and hard to implement correctly. Modern systems (Figma, Linear, Notion) use **CRDTs (Conflict-free Replicated Data Types).**

The idea: design your data structure so that **concurrent edits ALWAYS merge correctly by math, not by transformation logic.**

```
Example: Grow-Only Counter CRDT

Each user has their own counter:
Alice's counter: 5
Bob's counter:   3

Merge = take MAX of each counter = 5

No conflict possible! The merge function is mathematically guaranteed
to be commutative, associative, and idempotent.

(commutative = A merge B = B merge A)
(associative = (A merge B) merge C = A merge (B merge C))
(idempotent  = A merge A = A)
```

For text documents, the most common CRDT is **RGA (Replicated Growable Array)** or **YATA** (used by Y.js, which powers many collaborative editors):

```
Instead of storing text as a plain string with positions,
store each character as a node with a unique ID and
a "comes after" pointer:

[START] → [T: id=1] → [h: id=2] → [e: id=3] → [END]

Alice inserts "A" after id=2:
[START] → [T:1] → [h:2] → [A: id=4, after=2] → [e:3] → [END]

Bob inserts "B" after id=2 simultaneously:
[START] → [T:1] → [h:2] → [B: id=5, after=2] → [e:3] → [END]

CRDT merge rule: sort concurrent inserts by ID
[START] → [T:1] → [h:2] → [A:4] → [B:5] → [e:3] → [END]

Both Alice and Bob converge to same result with ZERO server coordination!
The merge is deterministic and conflict-free by design ✅
```

---

## Offline Support — What Happens When You Lose Internet?

```
Priya is on a plane. She edits 3 files offline:
→ Edits report.pdf (5 changes)
→ Creates new_doc.txt 
→ Deletes old_draft.pdf

She lands. Phone reconnects to internet.

Sync engine kicks in:
1. Fetch server state since last sync: 
   "server version was 47 when you went offline"
2. Check: has anyone else changed these files?
   → report.pdf: Bob also edited it (conflict!) 
   → new_doc.txt: doesn't exist on server (no conflict)
   → old_draft.pdf: still exists on server (no conflict)
   
3. Handle each case:
   → new_doc.txt: upload it ✅
   → old_draft.pdf: delete it ✅
   → report.pdf: CONFLICT → apply conflict resolution strategy
                 (create conflict copy, or use OT to merge)
```

The **vector clock** from Chapter 6 is how you know whether a conflict happened:

```
Vector clock on device when going offline: { server: 47, alice: 12 }
Vector clock on server now:                { server: 52, bob: 8 }

Server advanced from 47→52 while you were offline.
Bob made changes (bob:8).
→ Potential conflict on any file both you and Bob touched.
```

---

## Chapter 7 Summary 🎯

```
Versioning & Sync Arsenal:

✅ Delta/Chunk-based versioning  → 90%+ storage savings vs full copies
✅ Copy-on-Write chunks          → unchanged chunks shared across versions
✅ Reference counting + GC       → safely clean up unreferenced chunks
✅ WebSockets                    → instant push notifications, no polling
✅ Delta sync                    → only transfer changed chunks
✅ Operational Transformation    → correct real-time collaboration (Google Docs)
✅ CRDTs                        → conflict-free merging without server coordination
✅ Vector clocks                 → detect conflicts after offline edits
✅ Offline-first design          → queue changes, sync on reconnect

The Key Insight for Interviews:
→ Versioning = Copy-on-Write at the chunk level
→ Sync = event-driven (WebSocket push) + delta transfer
→ Collaboration = OT or CRDT, both valid answers
   (mention tradeoffs: OT needs central server, CRDTs work peer-to-peer)
```

---

## What's Next? 🔥

Your system handles billions of files, syncs instantly, and lets teams collaborate. But now your security team walks in:

```
Security Engineer: "Who has access to what file, and how do 
                   we make sure they can't access anything else?"

You: "We check permissions on every request..."

Security: "We have 500M users, nested folder permissions, 
           shared links, team drives, external sharing...
           How do you check permissions in under 1ms 
           without querying the database every time?"

You: "..."
```

> *"How do you design an authorization system that handles millions of permission checks per second, supports complex sharing rules, and never accidentally leaks a private file?"*

**Chapter 8: Access Control, Security & Permission Systems — How Google Drive knows in milliseconds whether you can view, edit, or own a file.**

Say **"next"** when ready! 🚀

---

# 🔐 Chapter 8: Access Control & Security — Who Can See What

---

## The Story Continues...

Your security team pulls up a scenario:

```
Priya creates a folder: "Company Secrets"
  └── "Q4_Revenue.xlsx"        ← confidential
  └── "Product_Roadmap.pdf"    ← confidential
      └── shares with Bob (view only)
  └── "Team_Photos" folder
      └── shares publicly (anyone with link)

Questions that need answers in <1ms on every request:
→ Can Bob download Q4_Revenue.xlsx?          (NO — not shared with him)
→ Can Bob edit Product_Roadmap.pdf?          (NO — view only)
→ Can a stranger view Team_Photos?           (YES — public link)
→ Can Bob share Product_Roadmap with Carol?  (NO — can't reshare)
→ If Priya deletes Bob's access to the folder,
  does he instantly lose access to everything inside? (YES)
```

500 million users. Billions of files. Millions of sharing relationships. Every single file access — upload, download, preview, share — needs a permission check.

If you get this wrong:
```
Too strict  → users can't access their own files 😤
Too lenient → private files leak to wrong people 😱 (lawsuits, GDPR fines)
```

---

## First Attempt: Simple ACL Table

An ACL (Access Control List) is the most intuitive approach.
For every file, store a list of who can do what:

```
file_permissions table:
┌──────────┬──────────┬────────────┬─────────────────────┐
│ file_id  │ user_id  │ permission │ granted_by          │
├──────────┼──────────┼────────────┼─────────────────────┤
│ f_roadmap│ user_bob │ VIEW       │ user_priya          │
│ f_roadmap│ user_priya│ OWNER     │ system              │
│ f_photos │ PUBLIC   │ VIEW       │ user_priya          │
└──────────┴──────────┴────────────┴─────────────────────┘
```

Permission check:
```sql
SELECT permission FROM file_permissions 
WHERE file_id = 'f_roadmap' AND user_id = 'user_bob';
-- Returns: VIEW ✅
```

**Works for 100 files. What about nested folders with 1 billion files?**

---

## The Inheritance Problem 🌳

Real file systems have **hierarchical permissions.** Permissions flow DOWN through folders:

```
/Company Secrets/          ← Priya: OWNER
  /Engineering/            ← Bob: EDITOR (inherited from parent? or explicit?)
    /Backend/              ← Carol: VIEWER
      /API_docs/           
        design.pdf         ← Who can access this?
```

To know if Carol can access `design.pdf`, you need to check:
```
1. Does Carol have explicit permission on design.pdf?
2. Does Carol have permission on /API_docs/?
3. Does Carol have permission on /Backend/? ← YES (VIEWER)
4. Does Carol have permission on /Engineering/?
5. Does Carol have permission on /Company Secrets/?
```

This is called **permission inheritance** — walking up the tree until you find a matching rule.

```
Naive implementation:

function canAccess(userId, fileId) {
  // Check file itself
  let node = getFile(fileId);
  while (node != null) {
    const perm = checkACL(userId, node.id);  // DB query each time!
    if (perm) return perm;
    node = getParent(node.id);               // another DB query!
  }
  return NO_ACCESS;
}

For a file 6 levels deep → 6 DB queries PER ACCESS CHECK 😱
At 100,000 requests/second → 600,000 DB queries/second
Database melts 🔥
```

---

## Solution 1: Materialized Permissions (Denormalization)

Instead of computing permissions at query time, **precompute and store the effective permission** for every (user, file) pair:

```
materialized_permissions table:
┌──────────┬──────────┬────────────┬──────────────────┐
│ file_id  │ user_id  │ permission │ source           │
├──────────┼──────────┼────────────┼──────────────────┤
│ design.pdf│ carol   │ VIEW       │ inherited:/Backend│
│ design.pdf│ bob     │ EDIT       │ inherited:/Eng    │
│ design.pdf│ priya   │ OWNER      │ inherited:/Company│
└──────────┴──────────┴────────────┴──────────────────┘
```

Now permission check is a **single indexed lookup:**
```sql
SELECT permission FROM materialized_permissions
WHERE file_id = 'design.pdf' AND user_id = 'carol';
-- Returns: VIEW  in ~1ms ✅
```

### The Catch: What happens when you change a folder permission?

```
Priya removes Bob's access from /Engineering/ folder
→ Bob loses access to EVERYTHING inside /Engineering/
→ /Engineering/ has 50,000 files inside it
→ You need to update 50,000 rows in materialized_permissions 😱

At write time: slow, complex, potentially inconsistent during update
```

This is the classic **read vs write tradeoff.** Fast reads, expensive writes.

---

## Solution 2: Permission Tokens + Caching ⭐

The approach used by Google Drive. Instead of precomputing all permissions, use a **layered caching strategy:**

```
Step 1: Check permission cache (Redis) → hit in ~0.1ms
Step 2: Check materialized table → hit in ~1ms  
Step 3: Walk the ACL tree (fallback) → ~10ms, then cache result

95% of requests hit Step 1.
4% hit Step 2.
1% fall through to Step 3.
```

```javascript
async function checkPermission(userId, fileId, requiredPerm) {
  // Layer 1: Redis cache (0.1ms)
  const cacheKey = `perm:${userId}:${fileId}`;
  const cached = await redis.get(cacheKey);
  if (cached) return hasPermission(cached, requiredPerm);

  // Layer 2: Materialized table (1ms)
  const materialized = await db.query(
    `SELECT permission FROM materialized_permissions 
     WHERE file_id=? AND user_id=?`, [fileId, userId]
  );
  if (materialized) {
    await redis.set(cacheKey, materialized, 'EX', 300); // cache 5 min
    return hasPermission(materialized, requiredPerm);
  }

  // Layer 3: Walk ACL tree (10ms, rare)
  const perm = await walkACLTree(userId, fileId);
  await redis.set(cacheKey, perm, 'EX', 300);
  return hasPermission(perm, requiredPerm);
}
```

### Cache Invalidation on Permission Change

```
Priya removes Bob's access from /Engineering/:

Step 1: Update ACL table (remove Bob's row)
Step 2: Find all files under /Engineering/ (background job)
Step 3: Delete Redis cache keys for Bob + each file
        → redis.del("perm:bob:design.pdf")
        → redis.del("perm:bob:api_spec.md")
        → ... (async, fan-out job)

During propagation window (~seconds):
→ Bob might still have cached access
→ Acceptable? For most files: YES (seconds of lag)
→ For sensitive revocation: NO

For IMMEDIATE revocation (e.g., security breach):
→ Store a "revocation timestamp" per user
→ Any cached permission older than revocation_timestamp → INVALID
→ Forces re-check even if cache hasn't expired
```

---

## The Permission Hierarchy (Roles)

Rather than arbitrary permissions, use a **role hierarchy:**

```
OWNER > EDITOR > COMMENTER > VIEWER > NO_ACCESS

OWNER:      read, write, delete, share, change permissions, transfer ownership
EDITOR:     read, write, share (if allowed by owner)
COMMENTER:  read, add comments (Google Docs style)
VIEWER:     read only
NO_ACCESS:  blocked (even if parent folder is shared)
```

**NO_ACCESS is special** — it's an explicit DENY that overrides inherited grants:

```
/Engineering/ → Bob: EDITOR (inherited)
  /Secrets/   → Bob: NO_ACCESS (explicit deny)
    /plan.pdf → Bob: ???

Rule: Explicit DENY always wins over inherited ALLOW
Answer: Bob gets NO_ACCESS to plan.pdf ✅

This lets owners share a folder broadly but protect
specific sub-folders — a very common real use case.
```

---

## Shared Links — Public Access Without Accounts

```
Priya creates a shareable link for her presentation:
https://drive.yourdrive.com/file/f_123?share_token=abc123xyz

Anyone with this link can VIEW (not edit) the file.
No login required.
```

How do you implement this securely?

```
share_links table:
┌──────────────┬──────────┬─────────┬─────────────┬────────────┬───────────┐
│ token        │ file_id  │ perm    │ created_by  │ expires_at │ max_views │
├──────────────┼──────────┼─────────┼─────────────┼────────────┼───────────┤
│ abc123xyz    │ f_123    │ VIEW    │ user_priya  │ 2024-02-01 │ NULL      │
│ def456uvw    │ f_456    │ EDIT    │ user_bob    │ NULL       │ 1000      │
└──────────────┴──────────┴─────────┴─────────────┴────────────┴───────────┘
```

The **token must be cryptographically random** — not guessable:

```python
# BAD - guessable sequential ID
token = f"share_{file_id}_{timestamp}"  # ❌ predictable

# GOOD - cryptographically random
import secrets
token = secrets.token_urlsafe(32)  
# generates: "K8hJ2mNpQ7vR3xL9wY5uT1oI6eA4cF0b" ✅
# 256 bits of entropy → practically impossible to brute force
```

Permission check for shared link access:

```
Request: GET /file/f_123?share_token=abc123xyz

1. Look up token in share_links table
2. Verify token exists and is not expired
3. Verify file_id matches
4. Check if owner has revoked the link
5. Grant VIEW access (no account needed) ✅

Rate limit by IP: max 100 requests/min per IP
→ prevents brute force enumeration of tokens
```

---

## Team Drives & Group Permissions

What if a whole TEAM needs access to a folder? You can't add 500 employees one by one.

Solution: **Group-based permissions**

```
groups table:
┌──────────┬───────────────────┐
│ group_id │ name              │
├──────────┼───────────────────┤
│ g_eng    │ Engineering Team  │
│ g_design │ Design Team       │
└──────────┴───────────────────┘

group_members table:
┌──────────┬──────────┐
│ group_id │ user_id  │
├──────────┼──────────┤
│ g_eng    │ user_bob │
│ g_eng    │ user_alice│
│ g_eng    │ user_carol│ ← 500 members...
└──────────┴──────────┘

file_permissions table now supports group_id:
┌──────────┬─────────────┬────────────┐
│ file_id  │ principal   │ permission │
├──────────┼─────────────┼────────────┤
│ f_roadmap│ group:g_eng │ EDIT       │  ← entire engineering team gets EDIT
│ f_roadmap│ user_priya  │ OWNER      │
└──────────┴─────────────┴────────────┘
```

Permission check now expands to:

```
Can user_bob access f_roadmap?

1. Check direct user permission: user_bob → none
2. Check group memberships: user_bob is in [g_eng, g_all_staff]
3. Check group permissions:
   g_eng → EDIT on f_roadmap ← found!
4. Result: EDIT ✅

New employee joins Engineering team:
→ Add to group_members (1 row insert)
→ Instantly inherits access to ALL files shared with g_eng 🎉
```

---

## Authentication vs Authorization

Interviewers often conflate these. Be crisp about the difference:

```
Authentication: WHO are you?
→ "I am Priya, here is my JWT token / OAuth token"
→ Handled by: Auth service, OAuth2, SSO

Authorization: What are YOU ALLOWED to do?
→ "Can Priya VIEW/EDIT/DELETE this file?"
→ Handled by: Permission system (what we've been building)
```

### JWT Token Flow

```
Login flow:
1. Priya logs in with Google OAuth
2. Auth service verifies with Google
3. Auth service issues JWT:
   {
     "user_id": "user_priya",
     "email": "priya@example.com",
     "exp": 1705320000,          ← expires in 1 hour
     "signature": "HMAC_SHA256(...)" ← signed with secret key
   }
4. JWT stored in client (browser cookie / mobile keychain)

Every API request:
Client → API Server: request + JWT in header
API Server: 
  1. Verify JWT signature (no DB lookup needed!)
  2. Check expiry
  3. Extract user_id from JWT
  4. THEN check permissions for this user_id
  
JWT verification = pure crypto math = ~0.1ms, no DB hit! ✅
```

---

## Encryption — Protecting Files at Rest and in Transit

Two types of encryption matter:

### Encryption in Transit (TLS)

```
Client ←——[TLS 1.3 encrypted tunnel]——→ Server

All data in motion is encrypted.
Even if someone intercepts the network packets → unreadable gibberish.
Standard HTTPS. Non-negotiable for any system.
```

### Encryption at Rest

```
Chunks stored on disk are encrypted:

chunk_data_plaintext → AES-256-GCM encryption → chunk_data_ciphertext
                              ↑
                       encryption key

Even if someone steals the physical hard drive → unreadable ✅
```

**Key management** is where it gets interesting:

```
Naive: one master key encrypts everything
→ If master key leaks → ALL data compromised 😱

Better: per-user keys
→ Each user's files encrypted with their own key
→ Keys stored in a secure Key Management Service (KMS)
→ If one user's key leaks → only that user's data at risk

Even better: per-file keys (Google's approach)
┌─────────────────────────────────────────────┐
│ File Key (random, per file)                 │
│   → encrypts the actual file chunks         │
│                                             │
│ User's Key Encryption Key (KEK)             │
│   → encrypts the file key                  │
│                                             │
│ Master KMS Key                              │
│   → encrypts all KEKs                      │
└─────────────────────────────────────────────┘

Envelope Encryption:
chunk → encrypted with file_key
file_key → encrypted with user_KEK  
user_KEK → encrypted with master_key (in hardware HSM)

To read a file:
→ Fetch encrypted file_key
→ Decrypt with user_KEK (requires user auth)
→ Use file_key to decrypt chunks
→ Never expose raw keys outside KMS ✅
```

---

## Audit Logging — Who Did What, When

For enterprise customers (and GDPR compliance), you need a complete audit trail:

```
audit_log table (append-only, never updated):
┌─────────────────────┬──────────┬──────────┬──────────┬─────────────────┐
│ timestamp           │ user_id  │ action   │ file_id  │ metadata        │
├─────────────────────┼──────────┼──────────┼──────────┼─────────────────┤
│ 2024-01-15 09:23:01 │ user_bob │ VIEW     │ f_123    │ ip: 1.2.3.4     │
│ 2024-01-15 09:23:45 │ user_bob │ DOWNLOAD │ f_123    │ size: 4MB       │
│ 2024-01-15 09:31:12 │ user_priya│ REVOKE  │ f_123    │ from: user_bob  │
│ 2024-01-15 09:31:15 │ user_bob │ VIEW     │ f_123    │ DENIED          │
└─────────────────────┴──────────┴──────────┴──────────┴─────────────────┘

This log answers:
→ "Did Bob download the file before his access was revoked?" → YES
→ "Who accessed this sensitive file in the last 30 days?" → query audit log
→ GDPR: "What data did you access for user X?" → query audit log
```

Audit logs must be:
```
→ Append-only (never modified or deleted — legal requirement)
→ Write to separate storage (not same DB as main data)
→ Shipped to cold storage (S3/Glacier) after 90 days
→ Indexed by user_id, file_id, timestamp for fast queries
```

---

## Chapter 8 Summary 🎯

```
Security & Access Control Arsenal:

✅ ACL with role hierarchy        → OWNER > EDITOR > COMMENTER > VIEWER
✅ Explicit DENY override         → sub-folder lockdown overrides parent grant
✅ Materialized permissions       → precomputed, fast single-row lookup
✅ Permission cache (Redis)       → 0.1ms checks, invalidated on change
✅ Revocation timestamps          → immediate access revocation on security events
✅ Cryptographic share tokens     → 256-bit random, not guessable
✅ Group-based permissions        → team access in one row, not N rows
✅ JWT authentication             → stateless, no DB lookup for auth
✅ Envelope encryption            → per-file keys, layered protection
✅ Audit logging                  → append-only, GDPR-compliant access trail

The Key Insight for Interviews:
→ "AuthN vs AuthZ are separate concerns handled by separate systems"
→ "Permission checks must be <1ms → cache aggressively, invalidate carefully"
→ "DENY always beats ALLOW — security must be explicit"
→ "Audit logs are as important as the permission system itself" 🏆
```

---

## What's the Final Chapter? 🔥

You've now built a complete, production-grade file storage system. But in an interview, after all this, the interviewer leans back and asks:

```
"Walk me through what happens when your system 
 receives 10x normal traffic suddenly. 
 How does it handle failures gracefully?
 What's your disaster recovery plan?
 How do you monitor all of this?"
```

> *"How do you make a system that doesn't just work — but keeps working even when everything around it is on fire?"*

**Chapter 9: Scalability, Failure Handling & Observability — The final layer that separates a good system from a great one. And the full architecture diagram tying everything together.**

Say **"next"** when ready! This is the final chapter where everything comes together! 🚀

---

# 🔥 Chapter 9: Scalability, Failure Handling & Observability — Keeping It All Alive

---

## The Story Continues...

It's Black Friday. Your file storage system is humming along. Then at 9:00 AM:

```
[09:00:00] Traffic: 23,000 req/sec    ← normal peak
[09:00:47] Traffic: 89,000 req/sec    ← someone's viral video just got shared
[09:01:12] Traffic: 247,000 req/sec   ← it's on Reddit front page
[09:01:33] API Server 3: CPU 100% 🔥
[09:01:34] API Server 5: CPU 100% 🔥
[09:01:35] API Server 1: OUT OF MEMORY — CRASHED 💀
[09:01:36] Database connections: 10,000/10,000 — EXHAUSTED
[09:01:37] Error rate: 34% of requests failing
[09:01:38] Your phone: 📱📱📱📱📱 (PagerDuty going insane)
```

Everything you built in Chapters 1-8 is useless if it falls over under load. This chapter is about making your system **resilient, observable, and self-healing.**

---

## Part 1: Scalability — Handling Traffic Spikes

### Auto-Scaling — Elastic Infrastructure

The core idea: **automatically add servers when load increases, remove them when it drops.**

```
Normal Traffic:                 Traffic Spike:

[API-1] [API-2] [API-3]        [API-1] [API-2] [API-3]
   23k req/sec total               [API-4] [API-5] [API-6]  ← auto-added
                                   [API-7] [API-8]           ← auto-added
                                      247k req/sec total ✅
```

How Auto-Scaling works:

```
Auto-Scaler monitors metrics every 30 seconds:

IF avg_cpu > 70% for 2 consecutive minutes:
   → Launch 2 new API server instances
   → Register them with Load Balancer
   → They start receiving traffic in ~90 seconds

IF avg_cpu < 30% for 10 consecutive minutes:
   → Terminate 1 server instance (gracefully drain connections first)
   → Saves money during off-peak hours 💰

Scale-out trigger:  CPU > 70%  OR  request latency > 500ms
Scale-in trigger:   CPU < 30%  AND latency < 100ms
Min instances: 3   (never go below this — always have some capacity)
Max instances: 100 (cost cap — alert humans if we hit this)
```

### But Auto-Scaling Has a Weakness: Cold Start Lag

```
Traffic spike happens at T=0
Auto-scaler detects it at T=60s (needs 2 min of sustained high CPU)
New servers launch at T=90s  
Servers warm up (JVM, caches) at T=150s
New servers accept traffic at T=180s

3 minutes of degraded service during the spike! 😬
```

Solution: **Predictive Scaling**

```
Historical data shows traffic spikes every day at 9 AM:

→ At 8:45 AM: pre-emptively scale UP to 8 servers
→ At 11 PM:   scale DOWN to 3 servers (off-peak)

ML model predicts traffic 30 min ahead based on:
- Day of week patterns
- Historical hourly patterns  
- Upcoming events (Super Bowl, product launches)
- Current growth trend

AWS calls this "Scheduled Scaling" + "Predictive Scaling" ✅
```

---

## Part 2: The Load Balancer — Traffic Director

Your Load Balancer sits in front of all API servers and distributes requests. But how does it decide where to send each request?

### Load Balancing Algorithms

```
Algorithm 1: Round Robin (simplest)
→ Request 1 → Server A
→ Request 2 → Server B  
→ Request 3 → Server C
→ Request 4 → Server A (cycle repeats)

Problem: Server A might be handling a heavy 2GB upload
         while Server B sits idle. Ignores server load.

Algorithm 2: Least Connections ⭐
→ Always route to server with FEWEST active connections
→ Naturally handles varying request weights

Algorithm 3: Weighted Round Robin
→ Server A (32 CPU cores): weight 4 → gets 4x more traffic
→ Server B (8 CPU cores):  weight 1 → gets 1x traffic
→ Useful when servers have different capacities

Algorithm 4: IP Hash (for session stickiness)
→ hash(client_ip) % num_servers → always same server
→ Useful when server stores session state locally
→ BUT: violates stateless server design (avoid if possible)
```

### Health Checks — Load Balancer as Gatekeeper

```
Load Balancer pings every server every 5 seconds:
GET /health → expects HTTP 200 in <100ms

Server response:
{
  "status": "healthy",
  "cpu": 45,
  "memory": 67,
  "db_connection": "ok",
  "cache_connection": "ok"
}

If server fails 3 consecutive health checks:
→ Load Balancer removes it from rotation
→ In-flight requests: wait for natural completion
→ New requests: routed to healthy servers only
→ Alert sent to on-call engineer

When server recovers:
→ Passes 2 consecutive health checks
→ Load Balancer adds it back → traffic flows again ✅
```

---

## Part 3: Failure Handling — Embracing the Inevitable

In distributed systems, **failure is not exceptional — it's the norm.** Hard drives fail. Networks partition. Processes crash. Power goes out.

The goal is not to prevent all failures (impossible) but to **handle them gracefully so users barely notice.**

### The Failure Taxonomy

```
Type 1: Single Server Crash
→ Load Balancer detects via health check in 15 seconds
→ Traffic rerouted to other servers
→ User might see 1-2 failed requests → client retries → success
→ Impact: tiny blip ✅

Type 2: Database Primary Failure  
→ Replica election (Raft consensus) picks new primary in ~30 seconds
→ Writes paused during election
→ Reads continue from replicas
→ Impact: 30 seconds of write unavailability 😬

Type 3: Entire Data Center Down
→ DNS failover to secondary data center
→ Traffic rerouted globally
→ Secondary DC takes over all traffic
→ Impact: 2-5 minutes of degraded service 😱

Type 4: Network Partition (split brain)
→ Two data centers can't talk to each other
→ Apply CP vs AP choice from Chapter 6
→ Writes paused until partition heals OR
→ Accept eventual consistency risk
→ Impact: depends on design choices
```

### Circuit Breaker Pattern 🔌

Imagine your Metadata DB is struggling. Requests are timing out after 30 seconds. Meanwhile new requests keep piling up, waiting 30 seconds each, filling up your connection pool, cascading failure to everything else.

**Circuit Breaker stops this cascade:**

```
Circuit Breaker has 3 states:

CLOSED (normal operation):
→ All requests flow through to DB
→ Monitor failure rate

                    failure rate > 50%
                    in last 60 seconds
                           │
                           ▼
OPEN (DB is struggling):
→ ALL requests FAIL IMMEDIATELY (don't even try DB)
→ Return cached data or error instantly
→ No waiting 30 seconds → connection pool not exhausted
→ DB gets breathing room to recover

                    after 30 second cooldown
                           │
                           ▼
HALF-OPEN (testing recovery):
→ Allow 1 request through to DB
→ If SUCCESS → back to CLOSED ✅
→ If FAILURE → back to OPEN, wait longer

┌─────────┐   fail threshold    ┌──────┐
│ CLOSED  │ ─────────────────→  │ OPEN │
│(normal) │ ←────────────────── │      │
└─────────┘    success test      └──────┘
                                    │  ↑
                                    │  │
                               30s  ▼  │ fail
                              ┌──────────┐
                              │HALF-OPEN │
                              └──────────┘
```

```javascript
class CircuitBreaker {
  constructor(threshold=5, timeout=30000) {
    this.state = 'CLOSED';
    this.failureCount = 0;
    this.threshold = threshold;  // 5 failures → OPEN
    this.timeout = timeout;      // 30s before trying again
  }

  async call(dbOperation) {
    if (this.state === 'OPEN') {
      // Fail fast — don't even try
      return getCachedFallback();  // serve stale data
    }

    try {
      const result = await dbOperation();
      this.onSuccess();
      return result;
    } catch (error) {
      this.onFailure();
      throw error;
    }
  }

  onSuccess() {
    this.failureCount = 0;
    this.state = 'CLOSED';
  }

  onFailure() {
    this.failureCount++;
    if (this.failureCount >= this.threshold) {
      this.state = 'OPEN';
      setTimeout(() => this.state = 'HALF_OPEN', this.timeout);
    }
  }
}
```

### Retry with Exponential Backoff

When a request fails, you retry. But if 10,000 clients all retry simultaneously every second, you **DDoS your own recovering server.**

```
Bad retry strategy (thundering herd):
T=0s:  DB crashes
T=1s:  10,000 clients retry simultaneously → DB overwhelmed again
T=2s:  10,000 clients retry simultaneously → DB overwhelmed again
DB never recovers 😱

Good retry strategy (exponential backoff + jitter):
Client 1: retry after 1s + random(0-500ms)  = 1.3s
Client 2: retry after 1s + random(0-500ms)  = 1.7s
...
If still failing:
Client 1: retry after 2s + random(0-1s)    = 2.8s
Client 2: retry after 2s + random(0-1s)    = 2.1s
...
If still failing:
Client 1: retry after 4s + random(...)
Client 2: retry after 4s + random(...)

Max retries: 5. Max backoff: 32 seconds.

Clients spread out their retries → DB recovers gradually ✅
```

```
Backoff formula:
wait = min(base * 2^attempt, max_wait) + random(0, jitter)

Attempt 1: min(1 * 2^1, 32) + random = 2 + jitter seconds
Attempt 2: min(1 * 2^2, 32) + random = 4 + jitter seconds
Attempt 3: min(1 * 2^3, 32) + random = 8 + jitter seconds
Attempt 4: min(1 * 2^4, 32) + random = 16 + jitter seconds
Attempt 5: min(1 * 2^5, 32) + random = 32 + jitter seconds
```

---

## Part 4: Rate Limiting — Protecting Against Abuse

Without rate limiting, one user can flood your system:

```
Malicious user writes a script:
while True:
    upload_10GB_file()  ← runs forever, eats all your bandwidth
```

Or a bug in a client app:
```
Client bug: retry loop with no backoff
→ sends 50,000 requests/second from one account
→ crowds out legitimate users
```

### Token Bucket Algorithm (Most Common)

```
Each user gets a "bucket" of tokens:
→ Bucket capacity: 100 tokens
→ Refill rate: 10 tokens/second
→ Each API request costs 1 token (or more for expensive ops)

Normal user:
Makes 5 requests/second → uses 5 tokens/sec, refills 10/sec → bucket stays full ✅

Abusive user:
Makes 1000 requests/second:
→ Drains 100 tokens instantly
→ Token bucket EMPTY → requests rejected with HTTP 429 "Too Many Requests"
→ Bucket refills at 10/sec → they can only make 10 req/sec ✅
```

```
Different limits for different operations:

Operation              Rate Limit
────────────────────────────────────────────
File Upload            100 uploads/hour per user
File Download          1,000 downloads/hour per user  
Share Link Creation    50 links/hour per user
API calls (general)    1,000 requests/minute per user
Anonymous access       100 requests/hour per IP
```

### Distributed Rate Limiting (The Challenge)

With multiple API servers, each server needs to know the user's current token count — across all servers:

```
Problem:
User sends 3 requests simultaneously to 3 different API servers:
→ API Server 1: "User has 100 tokens, use 1 → 99 left" ✅
→ API Server 2: "User has 100 tokens, use 1 → 99 left" ✅  
→ API Server 3: "User has 100 tokens, use 1 → 99 left" ✅

Each server thinks the user has 100 tokens!
User effectively bypasses rate limiting! 😱

Solution: Centralized token bucket in Redis
→ Redis INCR is atomic → no race conditions

// Redis Lua script (atomic check-and-decrement):
local current = redis.call('GET', key)
if current and tonumber(current) >= cost then
    redis.call('DECRBY', key, cost)
    return 1  -- allowed
else
    return 0  -- rate limited
end

All API servers share the same Redis counter → consistent limiting ✅
```

---

## Part 5: Observability — Seeing Inside Your System

You can't fix what you can't see. Observability is **the ability to understand your system's internal state from its external outputs.**

### The Three Pillars of Observability

```
┌─────────────┬──────────────────┬─────────────────────────────────┐
│   METRICS   │      LOGS        │            TRACES               │
├─────────────┼──────────────────┼─────────────────────────────────┤
│ Numbers     │ Events           │ Request journeys                │
│ over time   │ (what happened)  │ (how long each step took)       │
│             │                  │                                 │
│ CPU: 45%    │ "User priya      │ Request abc123:                 │
│ Latency:    │  uploaded        │  API Server:     2ms            │
│   120ms     │  file f_123      │  Auth check:     1ms            │
│ Error rate: │  at 09:23:01"    │  Permission:     0.5ms          │
│   0.1%      │                  │  DB write:       8ms ← slow!    │
│             │ "ERROR: DB       │  Chunk upload:   45ms           │
│ Graph over  │  connection      │  CDN notify:     3ms            │
│ time →      │  timeout"        │  Total:          59ms           │
│ alerts when │                  │                                 │
│ anomalous   │ Full text search │ Identify bottlenecks instantly  │
└─────────────┴──────────────────┴─────────────────────────────────┘
```

### Key Metrics to Monitor

```
Infrastructure Metrics:
→ CPU, Memory, Disk I/O per server
→ Network bandwidth (in/out)
→ Active connections per server

Application Metrics:
→ Request rate (req/sec) per endpoint
→ Error rate (% of 4xx, 5xx responses)
→ Latency percentiles: p50, p95, p99, p99.9

  p50  = 50ms  → half of users see ≤50ms     (median)
  p95  = 200ms → 95% of users see ≤200ms
  p99  = 800ms → 99% of users see ≤800ms
  p99.9= 3000ms → 0.1% of users see ≤3s (these are your angriest users!)

Business Metrics:
→ Files uploaded per minute
→ Storage used (GB/day growth)
→ Active users
→ Upload/download success rate

Storage Metrics:
→ Chunk replication factor (should be 3.0, alert if <2.5)
→ Nodes with failed disks
→ GC backlog (unreferenced chunks awaiting deletion)
```

### Distributed Tracing — Following a Request Through the System

When a request touches 10 different services, how do you know which one is slow?

```
Request "download wedding_video.mp4" gets a trace ID: "trace-abc123"

Every service stamps this ID on their log entries:

[trace-abc123] Load Balancer     → API Server 4          2ms
[trace-abc123] API Server 4      → Auth check (JWT)       1ms
[trace-abc123] API Server 4      → Permission check Redis 0.3ms ← cache hit
[trace-abc123] API Server 4      → Metadata DB query     42ms  ← THIS IS SLOW
[trace-abc123] API Server 4      → Generate 512 signed URLs  8ms
[trace-abc123] API Server 4      → Return URLs to client   1ms

Total: 54ms
Culprit: Metadata DB at 42ms → investigate DB query, add index?
```

This is what **Jaeger, Zipkin, and AWS X-Ray** provide.

---

## Alerting — Being Woken Up for the Right Reasons

```
Bad alerts (too noisy → engineers ignore all alerts):
→ Alert: CPU > 50%  (fires constantly, who cares)
→ Alert: Any error occurs (errors are normal at scale)
→ Alert: Latency > 100ms (too sensitive)

Good alerts (meaningful signals):
→ Alert: Error rate > 1% sustained for 5 minutes
→ Alert: p99 latency > 2 seconds for 3 minutes
→ Alert: Replication factor drops below 2 (data risk!)
→ Alert: Storage node disk > 90% full
→ Alert: Failed health checks on >20% of servers
→ Alert: Circuit breaker OPEN on database pool

Alert routing:
→ Severity: P1 (data loss risk)    → page on-call immediately (24/7)
→ Severity: P2 (degraded service)  → page during business hours
→ Severity: P3 (warning)           → Slack notification, no page
```

---

## Disaster Recovery — When Everything Goes Wrong

### RPO and RTO — The Two Key Numbers

```
RPO (Recovery Point Objective):
"How much data can we afford to lose?"
→ RPO = 0:    zero data loss (synchronous replication required)
→ RPO = 1hr:  we can lose up to 1 hour of data (async replication OK)
→ File storage: RPO = seconds (async cross-region replication)

RTO (Recovery Time Objective):  
"How long can we be down?"
→ RTO = 0:     zero downtime (active-active multi-region)
→ RTO = 1hr:   back online within 1 hour
→ RTO = 24hr:  restore from backup within 24 hours
→ File storage: RTO = minutes (automatic failover)
```

### Backup Strategy

```
Level 1: Real-time Replication (Chapter 3)
→ 3 copies across racks, 1 geo-redundant
→ Protects against: hardware failure, DC failure
→ RPO: seconds

Level 2: Daily Snapshots
→ Full snapshot of metadata DB daily to cold storage (S3 Glacier)
→ Protects against: data corruption, accidental deletion at scale
→ RPO: 24 hours
→ Kept for: 30 days

Level 3: Quarterly Backups
→ Archived to offline tape storage (yes, tapes still exist!)
→ Protects against: ransomware, catastrophic data center loss
→ Kept for: 7 years (legal compliance)

Backup Testing (often forgotten!):
→ Every month: restore a random sample of files from backup
→ Verify integrity via checksums
→ Untested backups = no backups
→ "Netflix Chaos Engineering" — deliberately break things to test recovery
```

---

## The Complete System Architecture — Everything Together

```
                           ┌──────────────────────────────────┐
                           │           CLIENTS                │
                           │   Web / Mobile / Desktop App     │
                           │   (chunking, delta sync, retry)  │
                           └──────────────┬───────────────────┘
                                          │ HTTPS / WebSocket
                           ┌──────────────▼───────────────────┐
                           │         CDN (Cloudflare)         │
                           │  Edge nodes worldwide            │
                           │  Signed URL verification         │
                           └──────────────┬───────────────────┘
                                          │ cache miss → origin
                           ┌──────────────▼───────────────────┐
                           │        Load Balancer             │
                           │  Health checks, rate limiting    │
                           │  Least-connections routing       │
                           └────┬─────────┬────────┬──────────┘
                                │         │        │
                    ┌───────────▼─┐  ┌────▼─────┐  ┌▼──────────┐
                    │  API Server │  │API Server│  │API Server │
                    │  (stateless)│  │(stateless│  │(stateless)│
                    │  Auth/JWT   │  │          │  │           │
                    │  Circuit    │  │          │  │           │
                    │  Breaker    │  │          │  │           │
                    └──────┬──────┘  └────┬─────┘  └─────┬─────┘
                           │              │               │
          ┌────────────────┼──────────────┼───────────────┤
          │                │              │               │
   ┌──────▼──────┐  ┌──────▼──────┐  ┌───▼──────┐  ┌────▼────────┐
   │  Metadata   │  │   Redis     │  │  Collab  │  │   Queue     │
   │  DB Cluster │  │   Cache     │  │  Server  │  │  (Kafka)    │
   │  (sharded   │  │  (metadata, │  │(WebSocket│  │ async jobs  │
   │  +replicas) │  │   perms,    │  │  OT/CRDT)│  │ replication │
   │             │  │   sessions) │  │          │  │ GC, notify  │
   └──────┬──────┘  └─────────────┘  └──────────┘  └────┬────────┘
          │                                               │
          │                                    ┌──────────▼──────────┐
          │                                    │   Worker Services   │
          │                                    │  - Chunk replicator │
          │                                    │  - GC (ref counting)│
          │                                    │  - Virus scanner    │
          │                                    │  - Thumbnail gen    │
          │                                    └─────────────────────┘
          │
   ┌──────▼──────────────────────────────────────────────────────┐
   │                    BLOCK STORAGE LAYER                      │
   │                                                             │
   │  ┌─────────────────────────────────────────────────────┐   │
   │  │              Consistent Hash Ring                   │   │
   │  │                                                     │   │
   │  │  [Node-1]──[Node-2]──[Node-3]──...──[Node-N]        │   │
   │  │  US-E R1    US-E R4    US-E R9       US-W R2        │   │
   │  │                                                     │   │
   │  │  Each chunk: 3 replicas across racks + geo          │   │
   │  │  Heartbeat monitoring + self-healing                │   │
   │  │  AES-256 encryption at rest                        │   │
   │  └─────────────────────────────────────────────────────┘   │
   └─────────────────────────────────────────────────────────────┘
          │
   ┌──────▼──────────────────────────────────────────────────────┐
   │                  OBSERVABILITY STACK                        │
   │                                                             │
   │  Metrics (Prometheus) → Dashboards (Grafana)               │
   │  Logs (ELK Stack)     → Search & Alert                     │
   │  Traces (Jaeger)      → Request journey visualization      │
   │  Alerts (PagerDuty)   → On-call routing by severity        │
   └─────────────────────────────────────────────────────────────┘
```

---

## The "Interviewer's Checklist" — Full Coverage Summary

Here is every concept, grouped by interview question type:

```
"How does upload work?"
✅ Client-side chunking (4MB chunks)
✅ Parallel chunk upload
✅ Deduplication via content hashing
✅ Resumable upload (only re-upload failed chunks)
✅ Metadata written to sharded DB

"How does download work?"
✅ Fetch metadata from Redis cache → DB fallback
✅ Generate signed URLs per chunk (security)
✅ Client downloads chunks in parallel from CDN
✅ Reassemble locally
✅ Verify checksums

"How do you store data reliably?"
✅ 3x replication (different racks + geo)
✅ Quorum writes (W+R > N)
✅ Heartbeat + self-healing re-replication
✅ Checksum scrubbing (bit rot detection)
✅ Consistent hashing for chunk placement

"How do you scale?"
✅ Stateless API servers → horizontal auto-scaling
✅ Read replicas for DB reads (85% of traffic)
✅ Sharded DB with composite keys (user_id + file_id)
✅ Consistent hashing for shard management
✅ CDN for global file distribution
✅ Redis for metadata caching

"How do you handle consistency?"
✅ Read-your-writes for user's own files
✅ Strong consistency for permissions (Raft)
✅ Eventual consistency for file content
✅ Different guarantees per operation type
✅ Vector clocks for offline conflict detection

"How do you handle failures?"
✅ Circuit breaker (stop cascade failures)
✅ Retry with exponential backoff + jitter
✅ Load balancer health checks
✅ Automatic DB primary failover (Raft election)
✅ Multi-region failover (DNS switching)
✅ RPO/RTO: seconds/minutes for file storage

"How do you handle security?"
✅ JWT for stateless authentication
✅ ACL with role hierarchy (OWNER > EDITOR > VIEWER)
✅ Explicit DENY overrides inherited ALLOW
✅ Signed URLs with expiry for CDN access
✅ Envelope encryption (per-file keys)
✅ Rate limiting with token bucket (Redis)
✅ Audit logging (append-only)

"How do you observe the system?"
✅ Metrics (CPU, latency p99, error rate)
✅ Distributed tracing (find slow services)
✅ Structured logs (searchable events)
✅ Alerting (P1/P2/P3 severity routing)
✅ Chaos engineering (test recovery)
```

---

## The Senior Engineer Answer Framework 🏆

When the interviewer asks "how would you design X?", structure your answer like this:

```
1. REQUIREMENTS (2 min)
   → Functional: what does it do?
   → Non-functional: scale, availability, consistency needs
   → Back-of-envelope: storage, bandwidth, QPS

2. HIGH LEVEL DESIGN (5 min)
   → Draw the boxes: client, LB, API, DB, storage, cache, CDN
   → Trace a request end to end (upload + download)

3. DEEP DIVES (10 min each, pick what interviewer cares about)
   → "Let me deep dive into chunking..."
   → "For the DB, here's why I'd use sharding with consistent hashing..."
   → "The tricky part is consistency — here's how I'd handle it..."

4. FAILURE & SCALE (5 min)
   → "Here's what breaks first and how I'd handle it..."
   → "At 10x traffic: auto-scale API, read replicas help first..."

5. TRADEOFFS (ongoing)
   → Never present one option without discussing tradeoffs
   → "I chose AP here because X, but the cost is Y"
   → This is what separates senior from junior answers
```

---

## 🎉 The Complete Story — What You've Learned

```
Chapter 1: The Problem
  → Requirements, scale estimation, system skeleton
  
Chapter 2: File Chunking
  → Resumable uploads, deduplication, parallel transfer
  
Chapter 3: Replication & Durability  
  → 3x replication, quorum writes, self-healing, bit rot
  
Chapter 4: CDN & Caching
  → Redis for metadata, CDN for content, signed URLs
  
Chapter 5: Database Sharding
  → Read replicas, consistent hashing, composite shard keys
  
Chapter 6: Consistency Models
  → CAP theorem, read-your-writes, strong consistency for permissions
  
Chapter 7: Versioning & Sync
  → Delta encoding, copy-on-write, WebSockets, OT/CRDTs
  
Chapter 8: Access Control
  → ACL hierarchy, materialized permissions, JWT, encryption
  
Chapter 9: Scalability & Resilience
  → Auto-scaling, circuit breakers, rate limiting, observability
```

You now have the full mental model to walk into any HLD interview and design a file storage system from scratch — not by memorizing answers, but by **understanding the story of why each decision was made.** 

That's what interviewers are really testing. 🚀

---


