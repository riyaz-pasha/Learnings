# Blob Store — The Story (narrative edition)

> **What this file is.**
> The reference file, `20-Blob-Store-FAANG-Guide.md`, is the one to study from — it has the requirements, capacity math, trade-off tables, and the master cheat sheet.
> This file is a second way into the same material. It tells the same story in plain language, chapter by chapter.
>
> The company in the story is **SnapVault** — a fictional photo-and-video-sharing app. Engineers keep hitting a scaling wall, patch it, and then the patch itself creates the *next* wall. This continues until the design reaches the same place the reference file documents.
>
> Every wall SnapVault hits, and every fix it reaches for, is something a *real, named* system actually did:
> Facebook's Haystack photo store (the 2010 "Finding a needle in Haystack" paper), Facebook's f4 warm storage system, Amazon S3, Google's GFS-to-Colossus migration, Azure Blob Storage, and Dropbox's Magic Pocket.
>
> I'll say clearly every time whether a number is a documented fact or a reasonable stand-in. Stand-ins get an inline `[illustrative]` tag.

**The trigger phrases** for this whole topic: *"design S3,"* *"design a photo/video storage system,"* *"how would you store and serve billions of user-uploaded files."*

Keep one sentence in your head as you read:

> **Storing one big file is easy — just write it to a disk. Knowing *which* of ten thousand disks holds a specific 4 MB slice of it, replicated three times, without a full scan — that is the actual problem.**

Everything below is just this one idea, made harder in small, honest steps.

---

## Chapter 1 — The photo that took ten disk seeks to load

It's 2011. SnapVault is small — a photo app with a few hundred thousand users. Every uploaded photo is saved the obvious way: as its own file, `/storage/user_id/photo_id.jpg`, on an NFS-mounted filer.

This is exactly how Facebook's own photo infrastructure worked in its early years. It works fine — until it doesn't.

By 2012, SnapVault has hundreds of millions of photos. Reads start feeling sluggish, and nobody can point at why. CPU is fine. Network is fine. Disks aren't even full.

**The real culprit** — and this is a documented finding from Facebook's own Haystack paper — is that fetching one small file on a typical filesystem isn't one disk operation. It's *several*:

1. One or more seeks to walk the directory structure and translate the filename into an inode number.
2. Another seek to read that inode.
3. A final seek to read the actual data.

With deep or crowded directories, the paper describes this stacking up to **roughly 10 real disk operations to fetch a single photo** `[illustrative — the paper says "the standard set of Unix file system access assumptions... breaks down," placing it in this range, not a single universally-quoted constant]`.

At ~10ms per HDD seek, that's **~100ms of pure metadata chasing** before a single byte of the actual photo is even read.

With hundreds of millions of tiny files, the metadata working set is far too large to stay cached in RAM. So nearly every request pays the full cost, every time.

```mermaid
sequenceDiagram
    participant App as App
    participant FS as Filesystem
    participant Disk as Disk

    App->>FS: read photo_9981.jpg
    FS->>Disk: seek — walk dir entries
    FS->>Disk: seek — read inode
    FS->>Disk: seek — read data block
    Disk-->>App: bytes (~100ms total)
    Note over App,Disk: 1 photo ≈ 10 seeks, mostly metadata
```

**Why does a filesystem choke on lots of small files specifically?**

Because a general-purpose filesystem is built to answer questions like: *"Does this path exist? What are its permissions? When was it modified?"* It stores rich metadata and checks it on every access. A photo doesn't need any of that. It just needs: *"Give me these exact bytes, fast."*

**The fix — and the first analogy for this story:**

Stop treating each photo as its own file. Instead, pack many photos into one big physical file. Call it a **volume**. Then keep a single, tiny, in-memory index that maps `photo_id → offset inside that volume`.

This is **Haystack packing** — the real fix Facebook shipped.

Think of it like this: instead of a thousand tiny labeled envelopes scattered across different shelves, you have **one big filing cabinet drawer** with a single typed index card taped to the front.

To find a photo, you look up its offset in RAM (a hash lookup, ~100 ns), then do **exactly one** disk seek straight to the bytes. Ten seeks become one.

**New problem, immediately:**

That "one big filing cabinet drawer" is a single physical file, on a single disk, on a single machine. When it fills up, you're stuck. And if that one disk dies, everything inside that volume — potentially tens of thousands of photos — dies with it.

> **How I'd say this in an interview:** "The classic small-file problem isn't about disk capacity — it's about metadata. Every tiny file pays the full cost of filesystem lookups that were designed for rich, general-purpose files. Facebook's Haystack paper solved this by packing many photos into one large file with an in-memory offset index, turning ten seeks into one. That's the move to remember, not just that it was slow."

---

## Chapter 2 — The warehouse drawer that filled up and then vanished

SnapVault ships Haystack-style packing. Reads get genuinely fast — one seek per photo now.

Volumes are capped at a fixed size, roughly **100 GB each** `[illustrative — a round number in the range Haystack-style systems actually use, not a quoted constant]`, so no single file grows without bound.

Six months later, the math catches up.

SnapVault's upload rate — worked number: **20,000 photos/day × ~400 KB average = ~8 GB/day** — means one 100 GB volume fills up in about **12 days**. So SnapVault just keeps opening new volumes on the same machine.

Then one Tuesday, that machine's disk controller fails. Every volume it was hosting — months of accumulated photos, tens of millions of them — is gone. There is exactly one copy of each, and it just died.

**Why does "pack files efficiently" have anything to do with "keep a copy alive if a disk dies"?**

It doesn't. Those are two completely separate problems. Haystack packing only ever solved the first one.

**The fix:**

Split the system into two completely separate concerns:

- A **metadata service** (call it the master) tracks *where* each chunk of data lives — which machine, which volume, which offset.
- Dumb **data nodes** just hold bytes. They don't make any decisions.

This is the same split GFS and HDFS use, and it's the backbone of every real blob store from here on.

**The analogy:** a shipping manifest versus the cargo itself. A manifest clerk knows exactly which of ten thousand warehouse racks holds crate #482,991. The racks just hold crates and never get asked a question.

SnapVault also starts supporting video around now. Video and photos alike get split into fixed-size **chunks** (64–128 MB — the same range GFS and HDFS use) so no single chunk is too large to replicate or move quickly.

Each chunk gets written to **three** data nodes, on the master's instruction, before the upload is acknowledged.

```mermaid
flowchart LR
    subgraph Before["Before: one volume, one disk"]
        P1[Upload] --> V1[(Volume file)]
        V1 -. disk dies .-> X1[Gone]
    end
    subgraph After["After: manifest + 3x replication"]
        P2[Upload] --> Master[(Master)]
        Master --> D1[Node A]
        Master --> D2[Node B]
        Master --> D3[Node C]
    end
```

**New problem:**

Three copies of *everything*, forever, is expensive. Most of what SnapVault stores is old vacation photos that nobody has opened in years. The storage bill just tripled, and it's about to keep growing.

> **How I'd say this in an interview:** "Packing small files solves a metadata-overhead problem. It says nothing about durability. The real fix is splitting 'where is it' from 'the bytes' — a metadata service plus dumb data nodes replicating chunks. That's the same shape as GFS's master/chunkserver split. And the very next question that fix raises is: do we really need three full copies of everything, forever?"

---

## Chapter 3 — The storage bill that tripled for photos nobody looks at anymore

Worked number: by year three, SnapVault stores about **20 TB/day** raw across photos and video.

At 3x replication, that's **60 TB/day physical**.
Over a year: roughly **7.3 PB raw → ~21.9 PB physical**.

Finance flags it in a budget review. Most of that 21.9 PB is photos and videos from years ago that get essentially zero reads.

**Does a 5-year-old photo that nobody has viewed in 4 years need the same durability strategy as a photo uploaded an hour ago?**

No. Both need to *never disappear*. But "never disappear" doesn't require three full, ready-to-serve-instantly copies sitting around.

**The fix:**

For aged, infrequently-accessed ("cold") data, switch from replication to **erasure coding**.

This is exactly what Facebook's real **f4** system does. It takes photos that have aged out of Haystack's hot, triple-replicated tier and re-encodes them.

**The analogy:** 10 of 14 puzzle pieces.

Here's how it works: split the data into 10 equal data shards, then compute 4 extra parity shards using Reed-Solomon math. Spread all 14 across 14 different machines.

The key property: you need *any* 10 of the 14 to reconstruct the data — not a *specific* 10. So you can lose up to 4 machines at once and still recover everything.

The storage overhead drops from **3x** (replication) to only **~1.4x** (erasure coding).

```mermaid
quadrantChart
    title Rebuild speed vs. storage cost
    x-axis High storage cost --> Low storage cost
    y-axis Slow rebuild --> Fast rebuild
    quadrant-1 Fast and cheap - does not exist
    quadrant-2 Hot tier sweet spot
    quadrant-3 Cold tier sweet spot
    quadrant-4 Slow and expensive - avoid
    "3x replication": [0.25, 0.75]
    "10+4 erasure coding": [0.75, 0.25]
```

**New problem:**

Rebuilding a lost erasure-coded shard means reading all 10 surviving peers and re-running the encoding math. That is CPU-heavy and network-heavy. It's too slow for data people are actively reading right now.

So erasure coding only makes sense for cold data. SnapVault keeps hot data on 3x replication.

There's also a second problem hiding underneath both strategies: every copy — replicated or erasure-coded — is still sitting in **one region**. A whole-region event (a fire, a multi-hour power failure) takes out every copy at once, hot or cold.

> **How I'd say this in an interview:** "Uniform durability wastes either money or latency. Replicate hot data because rebuild has to be instant. Erasure-code cold data because you can afford a slower, CPU-heavy rebuild in exchange for cutting overhead from 200% to about 40%. This is literally the trigger for Facebook's real Haystack-to-f4 migration. But neither strategy alone survives a whole region going dark — and that's the next gap."

---

## Chapter 4 — The region that went dark

SnapVault's entire footprint — hot replicas and cold erasure-coded shards alike — lives in one region, spread across a few racks and data centers.

One year, that region has a multi-hour outage `[illustrative — a stand-in for "a real regional event," not a specific incident]`.

Every single copy of every photo is unreachable. If the event had been permanent instead of temporary, every copy would simply be gone. No amount of in-region replication protects against the region itself disappearing.

**If we already replicate three times, why isn't that enough?**

Because all three copies were placed *close together on purpose* — to keep write latency low. That same closeness is exactly what makes them all fail together.

**The fix:**

Add a fourth kind of copy. This copy is:
- **Asynchronous** — it's written *after* the client already received "upload complete."
- In a **completely different region.**

Each ring of copies now defends a specific, named failure:

| Copy | Placement | Defends against |
|---|---|---|
| Copy 1 | Rack A | Power strip / switch failure |
| Copy 2 | Rack B, same DC | Rack-level failure |
| Copy 3 | Different DC, same region | Fire or flood in one building |
| Copy 4 | Remote region | The entire region going dark |

This tiered placement is the real, documented shape that Azure and S3-style systems actually use.

```mermaid
flowchart TB
    subgraph RegionA["US-East (primary)"]
        R1["Rack A\n(original)"]
        R1 -- sync --> R2["Rack B\n(sync replica)"]
        R1 -- sync --> R3["Rack C\n(sync replica)"]
        R1 -- async --> DC2["DC 2, same region"]
    end
    subgraph RegionB["EU-West (remote)"]
        DC3["Copy 4\n(async, cross-region)"]
    end
    DC2 -- async --> DC3
```

**New problem:**

The remote-region copy is asynchronous. It lands *after* the client already got a success response.

This raises an uncomfortable question SnapVault hasn't answered yet: if a friend on the other side of the world clicks a link 200 milliseconds after upload, and the remote region's copy hasn't arrived yet — what do they see?

> **How I'd say this in an interview:** "Replication needs at least three tiers, not one — rack, data center, and region. Each tier defends against a genuinely different failure. Naming which failure each copy defends against is what makes the answer sound engineered rather than memorized. But an async cross-region copy immediately opens a consistency question: what does a reader in that remote region see before the copy has landed?"

---

## Chapter 5 — The upload that "succeeded" but 404'd for a friend

A user in New York uploads a photo. They see "upload complete" instantly. They share the link with a friend in Singapore.

The friend clicks it **~200ms later** `[illustrative timing — cross-region replication lag is well documented; the exact gap is a stand-in]` and gets a **404 — file not found**.

Nothing is actually lost. The async copy to the Singapore-side region simply hasn't landed yet. But to that friend, it looks exactly like data loss.

**If the upload already said "success," shouldn't "success" mean everyone everywhere can see it right now?**

That's a real design choice, not an accident. SnapVault has to pick a side — explicitly.

**The fix:**

Commit to **strong read-after-write consistency for metadata**, but scope it honestly.

Inside the primary region: don't acknowledge the client's upload until *all three* synchronous replicas have confirmed the write. And never serve a read from a copy that hasn't confirmed it.

This guarantees "success" really means success — everywhere the sync replicas live.

The async cross-region copy is the one piece explicitly allowed to be eventually consistent. This is a named trade-off, not a bug being apologized for.

This is exactly the real move Amazon made with S3: S3 was eventually consistent for years, then **moved to strong read-after-write consistency in December 2020** — a documented, industry-shaping change.

```mermaid
sequenceDiagram
    participant C as Uploader (NY)
    participant S as Sync replicas (x3)
    participant R as Remote region (SG)
    participant F as Friend (SG)

    C->>S: upload photo
    S->>S: all 3 confirm write
    S-->>C: 200 OK
    Note over S,R: async — off critical path
    S-->>R: replicate to remote
    F->>R: click link (~200ms later)
    Note over F,R: if copy not yet landed: 404
    R-->>F: photo bytes (once landed)
```

**New problem:**

Guaranteeing strong consistency this way means every metadata read and write funnels through the same master/metadata service — to check "has this write confirmed everywhere it needs to?" As SnapVault's user base grows, that one clerk starts getting asked an enormous number of questions.

> **How I'd say this in an interview:** "If an interviewer asks whether this design is eventually consistent, the precise answer is: strong within the primary region, eventual only for the async cross-region copy, and only until that copy lands. S3 itself made exactly this move to strong read-after-write in December 2020. The cost of strong consistency is that the metadata service now sits on the critical path of every single read and write."

---

## Chapter 6 — The one clerk everybody has to ask, every single time

SnapVault's master node — the manifest clerk from Chapter 2 — must be consulted for every `getBlob` call. It checks access, looks up which data nodes hold the chunks, and confirms none of them are lagging behind.

Benchmarks show the master tops out around **~10,000 QPS** `[illustrative — the reference design's stand-in ceiling for "one metadata instance," not a hard physical constant]`.

SnapVault's read traffic, driven by viral posts, dwarfs its write traffic. Worked ratio: roughly **400 reads for every 1 write** at peak. That lopsided shape is typical for any consumer photo/video app.

The master — doing a full lookup on every single read — is nowhere near able to keep up.

**Why does every read have to ask the clerk at all, if the answer barely ever changes?**

It doesn't, most of the time. A chunk's location is stable for long stretches. The client just doesn't know that yet on its first request.

**The fix:**

Cache the **chunk → data-node mapping** on the client side. This is the exact same "resolve once, reuse the answer" trick a DNS cache uses.

- **Cold read** — first access for a given photo. Ask the master, get the mapping, fetch the bytes, then cache the mapping locally.
- **Warm read** — every access after that. Skip the master entirely. Go straight from client to data node.

```mermaid
sequenceDiagram
    participant C as Client
    participant M as Master
    participant D as Data Node

    rect rgb(80,20,20)
    Note over C,D: Cold read (first access)
    C->>M: getBlob(photo_id)
    M-->>C: chunk locations [D1,D2,D3]
    C->>D: fetch bytes
    D-->>C: photo data
    C->>C: cache mapping
    end

    rect rgb(20,60,30)
    Note over C,D: Warm read (every access after)
    C->>C: cache hit
    C->>D: fetch bytes directly
    D-->>C: photo data
    Note over C,M: master not contacted
    end
```

**New problem:**

Caching the mapping only works if the mapping stays true. And the master doesn't actually promise it will.

> **How I'd say this in an interview:** "The master's QPS ceiling only has to absorb cold reads and writes, not the full read volume, once clients cache the chunk-to-node mapping — same shape as a DNS cache or connection reuse. The whole trick only holds up as long as the cached answer stays true, which is exactly the next thing that breaks."

---

## Chapter 7 — The cached address that pointed at an empty rack

The master notices Data Node 12's disk is showing early failure signs. It proactively moves Node 12's chunks over to Data Node 47. This is a sensible, self-healing move.

But thousands of clients are still holding a cached mapping from an hour ago that says "photo 9981 lives on D12."

The next time each of those clients tries a warm read, it hits D12, finds nothing there, and fails. The result: a spike of broken image icons across the app — for photos that are perfectly safe and readable, just not where the cache thinks they are.

**Doesn't a short TTL on the cache fix this?**

Only partially, and clumsily.

- Too short a TTL → clients constantly re-ask the master, which defeats the point of caching.
- Too long → exactly this failure window.

TTL alone answers "how stale can this get?" — not "did it actually go stale?"

**The fix:**

Attach a **version number** (called an epoch or generation number) to each cached mapping.

When a fetch fails against a cached mapping, don't treat it as "the photo is gone." Instead, treat it as "my mapping might be outdated." Then do exactly one fallback round-trip to the master to refresh.

In the normal case, this costs nothing — mappings rarely change. In the rare case where a rebalance happened, it's self-healing without any extra polling or aggressive TTLs.

This is the same "cache coherence via versioning, not TTL alone" fix that shows up anywhere a client caches an answer that the source of truth can move underneath it.

```mermaid
flowchart LR
    A["Cached mapping\n(D12, epoch 3)"] --> B{Fetch OK?}
    B -->|yes| C[Done\ncache valid]
    B -->|no| D[Fallback to master]
    D --> E["New mapping\n(D47, epoch 4)"]
    E --> F[Retry D47\ncache updated]
```

Beyond the client's own cache, SnapVault adds one more layer for reads: a **CDN at the edge**, caching the actual bytes of publicly-viewable photos.

This is safe to do — with zero invalidation logic — because photos are **write-once and immutable**. An immutable object never needs invalidation, only expiry. A new version of a photo just gets a new URL entirely.

When a single meme photo goes viral overnight — millions of reads per second — the CDN edge absorbs nearly all of it. SnapVault's origin storage only ever serves the first cache-fill per edge location.

**New problem:**

Reads are handled well now. But SnapVault adds **albums**, and `listAlbumPhotos` — one of the most common calls in the whole app — is oddly and consistently slow.

> **How I'd say this in an interview:** "Cache invalidation here isn't a TTL problem — it's a coherence problem. Version the cached answer so a miss triggers exactly one refresh, instead of either constant re-checking or silent staleness. And CDN caching in front of all this is only safe because the data is immutable — no invalidation needed, only expiry, since an 'edit' is always a brand new URL."

---

## Chapter 8 — The album list that had to ask sixty different filing cabinets

SnapVault's first approach to partitioning metadata was the obvious one: hash each photo's own ID and spread it across, say, **60 partitions**. Clean, evenly balanced, great for write throughput.

But here's what happens to an album: a user's album with 200 photos ends up with those 200 metadata rows scattered almost uniformly across all 60 partitions. By simple pigeonhole math, that's roughly 3–4 photos per partition.

Calling `listAlbumPhotos` on that one album means fanning a request out to **all 60 partitions** and merging the results — every single time — for one of the most common actions in the whole app.

**Why would you partition by the ID of the thing you're not usually looking up in bulk?**

Because it *felt* balanced. But "balanced writes" is not the goal. "Matches how you actually read the data" is the goal.

**The fix:**

Partition by the **composite key** — `account_id + album_id + photo_id` — so every photo belonging to the same album always lands in the *same* partition.

`listAlbumPhotos` becomes a single partition scan instead of a 60-way fan-out and merge.

```mermaid
flowchart TD
    subgraph Naive["Partition by photo_id alone"]
        N1["200 photos scattered\nacross all 60 partitions"] --> N2["listAlbum\nfan-out to all 60 + merge"]
    end
    subgraph Fixed["Partition by account+album+photo"]
        F1["All album photos\nin one partition"] --> F2["listAlbum\nsingle partition scan"]
    end
    Naive -. drives redesign .-> Fixed
```

**The trade-off, said out loud:**

This trades a little write-balance purity for read locality. That's the right call here, because SnapVault's dominant access pattern is "show me this album," not "show me a random scatter of photos."

The risk it accepts: one account that goes viral and dumps 50,000 photos into a single public album creates a genuine write hotspot on that one partition.

**New problem:**

SnapVault now supports video, and a 1.8 GB video upload over a spotty mobile connection keeps failing at 92% and restarting from byte zero — every single time.

> **How I'd say this in an interview:** "Partition key choice is a trade-off between write distribution and read locality. You pick based on your dominant access pattern. Here, listing-by-album beats spreading writes evenly, so the partition key is the composite path, not the raw photo ID alone."

---

## Chapter 9 — The 2 GB video that had to restart from zero, every time

A user tries to upload a 1.8 GB birthday video on a flaky connection. It dies at **92%**, three times in a row. They give up.

The whole upload is one giant HTTP request. Any dropped packet near the end means starting completely over from byte zero.

**Why should one dropped packet near the finish line cost the entire transfer?**

Because the upload was designed as one atomic, all-or-nothing call — with no concept of "partial progress that's safe to keep."

**The fix:**

**Multipart upload.** Split the file client-side into parts, upload them in parallel, and retry only the parts that fail — not the whole object.

S3's real limits: **5 MB to 5 GB per part**, up to **5 TB** total object size.

The server stitches the parts together and verifies checksums once everything lands.

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Storage

    C->>S: initiateMultipartUpload()
    S-->>C: uploadId

    par upload parts in parallel
        C->>S: uploadPart(1)
        C->>S: uploadPart(2)
        C->>S: uploadPart(3)
    end

    Note over C,S: part 2 drops -- retry part 2 only
    S-->>C: ETag per part
    C->>S: completeMultipartUpload([ETags])
    S-->>C: 200 OK
```

A second real production optimization SnapVault adopts at the same time: **don't tunnel the video's bytes through the app tier at all.**

Here's how it works:
1. The client tells the app server it wants to upload a file (metadata only — no bytes yet).
2. The app server issues a **pre-signed URL** — a time-limited, capability-scoped link.
3. The client uploads directly to the storage tier using that URL.
4. The storage tier validates the URL's signature and expiry locally — no round trip back to the master or app server needed.

**The analogy:** a scoped, time-limited badge that any door can check on its own, without calling the front desk.

```mermaid
sequenceDiagram
    participant C as Client
    participant App as App tier
    participant S as Storage

    C->>App: I want to upload video.mp4
    App-->>C: pre-signed URL (1hr expiry)
    C->>S: PUT parts directly to URL
    S-->>C: 200 OK
    Note over App,S: app tier never touches the bytes
```

**New problem:**

Uploads and reads are both fast and resilient now. But storage keeps growing forever, because:
1. Deleted photos and videos aren't actually freeing any space.
2. A viral meme photo that 40,000 users independently uploaded is stored as **40,000 separate full copies.**

> **How I'd say this in an interview:** "Multipart upload turns 'restart a multi-gigabyte transfer' into 'resend one 8 MB part,' which is what makes uploads over unreliable mobile networks viable at all. And in real systems — S3, GCS, Azure — the app tier usually doesn't touch the bytes at all. It just issues a pre-signed URL and gets out of the way, removing itself as a bandwidth bottleneck entirely."

---

## Chapter 10 — The delete button that waited for a warehouse audit, and the meme stored 40,000 times

**Problem 1: Deletes are too slow.**

SnapVault's first delete implementation is synchronous and literal. `deleteBlob` physically scrubs the bytes off all three replicas — and, for cold data, coordinates across all 14 erasure-coded shards — before returning success.

Worked cost: confirming physical erasure everywhere, live, takes on the order of **hundreds of milliseconds to a couple of seconds** `[illustrative — the real cost driver, cross-node coordination before ack, is real; the exact figure is a stand-in]`.

The result: a noticeably slow, all-or-nothing delete button.

**Does the user need to wait for the physical bytes to be gone, or just for the photo to disappear from view?**

Just the second one. Nobody is staring at a disk-usage dashboard the moment they hit delete.

**The fix: tombstone now, reclaim later.**

`deleteBlob` instantly writes a "DELETED" marker into metadata. The photo vanishes from the app immediately.

A background garbage collector frees the actual bytes later — off the critical path, on its own schedule.

This trades temporary disk-accounting lag for a fast, non-blocking delete API. It's the same "acknowledge fast, reconcile async" shape as a queue's soft-delete-plus-compaction.

```mermaid
stateDiagram-v2
    [*] --> Active: upload succeeds
    Active --> Tombstoned: deleteBlob() marker only
    Tombstoned --> Pending: async GC scheduled
    Pending --> Reclaimed: bytes freed
    Reclaimed --> [*]
```

---

**Problem 2: Duplicate bytes.**

Someone on the data team notices the 40,000-copies problem. A viral meme photo, uploaded independently by 40,000 different users, is stored as 40,000 identical full-size copies.

**The fix: content-addressable deduplication.**

At upload time, hash the bytes (SHA-256). If that hash already exists in storage, don't write new bytes at all. Instead, just add a metadata pointer and increment a reference count.

This is only safe *because* photos are immutable. Nobody can "edit" a shared blob out from under the other 39,999 people pointing at it.

---

**Now the two fixes have to talk to each other — or dedup silently breaks.**

The garbage collector must **not** reclaim a chunk just because one tombstoned blob points at it. It has to check the chunk's reference count first.

A chunk is only eligible for reclaim when *all three* of these conditions are true:
1. The retention window has passed (e.g., 30 days).
2. The reference count is zero — no other user is pointing at the same chunk hash.
3. No in-flight read is still holding it.

```mermaid
flowchart TD
    A["GC finds tombstoned chunk"] --> B{Retention\nwindow passed?}
    B -->|no| C[Wait\nnot eligible]
    B -->|yes| D{Reference\ncount zero?}
    D -->|no| E[Decrement refcount\nnot eligible yet]
    D -->|yes| F[Free the bytes]
```

**New problem:**

Every fix so far — the master, the cache, the partition scheme, the GC — lives on one logical metadata service. SnapVault keeps growing every year. Eventually, even a well-cached, well-partitioned master's *total index* outgrows what one instance's memory and CPU can hold.

> **How I'd say this in an interview:** "Delete has to be instant from the user's point of view and lazy from the disk's point of view — tombstone immediately, reclaim asynchronously. Content-addressable dedup is the natural next move once you notice the same bytes uploaded by different people, and it's safe specifically because the data is immutable. But it means garbage collection now has to check a reference count, not just a tombstone, before freeing anything."

---

## Chapter 11 — The clerk that had to be cloned

Even with client-side caching absorbing most reads, the master's remaining traffic — cold reads, all writes, all rebalancing decisions — eventually creeps past its own **~10,000 QPS** ceiling `[illustrative, same stand-in ceiling as Chapter 6]` once SnapVault crosses a few hundred million accounts.

This is the same ceiling GFS itself eventually hit with its single master. It's a well-documented, real scaling wall — not a SnapVault-specific quirk.

**If data itself scales by sharding across many machines, why would metadata be any different?**

It isn't. The fix is the same shape.

**The fix:**

Shard the metadata service itself.

A thin routing layer sits in front. Behind it, **N metadata shards**, each owning a disjoint range of the partition key — the same `account_id`-based ranges Chapter 8 already introduced for data.

This is exactly the real move Google made going from **GFS's single master to Colossus's sharded metadata layer** (via what Google calls Curator processes).

It's also the same two-layer shape Azure Blob Storage documents publicly: a **stream layer** doing the replicated byte storage, and a **partition layer** — itself horizontally partitioned — playing the master's role.

```mermaid
flowchart LR
    C([Client]) --> R["Routing layer\n(range lookup)"]
    R --> M1[("Shard 1\nA-F")]
    R --> M2[("Shard 2\nG-P")]
    R --> M3[("Shard 3\nQ-Z")]
    M1 --> DN["Data nodes\n(dumb chunk storage)"]
    M2 --> DN
    M3 --> DN
```

Sharding fixes throughput. But each individual shard is, on its own, still a single point of failure.

So each shard gets a hot standby and **consensus-based leader election** — Raft/Paxos, or an external coordinator like ZooKeeper/etcd.

If a shard fails: the failure means a few seconds of unavailability for its slice of accounts. It never means data loss.

The new leader always rebuilds its state from the **durable metadata store** — never by re-scanning data nodes. Re-scanning would be far too slow.

```mermaid
stateDiagram-v2
    [*] --> Active: leader elected
    Active --> Suspected: heartbeat missed
    Suspected --> Failed: timeout confirmed
    Failed --> Electing: standby runs election
    Electing --> Active: loads from metadata store
```

This is roughly where SnapVault's design actually lands.

It's also the same place Facebook's real **Tectonic** system landed — unifying Haystack-style hot storage and f4-style cold storage into one exabyte-scale system with a sharded, disaggregated metadata layer, instead of maintaining several bespoke storage systems side by side.

Dropbox's own **Magic Pocket** made a related bet — migrating off S3 entirely to build custom erasure-coded storage tuned to Dropbox's own access patterns and cost targets.

From here, further evolution is tuning and operations, not new architecture.

> **How I'd say this in an interview:** "The master in any first-pass diagram is a stand-in for a metadata *service*. At real scale, it's sharded the same way the data itself is — which is exactly Google's move from GFS to Colossus, and Azure's partition-layer design. Naming that unprompted pre-empts the follow-up before the interviewer has to ask it."

---

## Where the story actually lands

```mermaid
flowchart TD
    A["Ch 1: one file per photo\n~10 seeks per read"]
    B["Ch 2: Haystack packing\nfast reads, single disk"]
    C["Ch 3: master + data nodes\n3x replication"]
    D["Ch 4: erasure coding\nfor cold data"]
    E["Ch 5: multi-region\nasync replication"]
    F["Ch 6: strong\nread-after-write"]
    G["Ch 7: client-side\nchunk cache"]
    H["Ch 8: versioned cache\n+ CDN"]
    I["Ch 9: composite\npartition key"]
    J["Ch 10: multipart\n+ pre-signed URL"]
    K["Ch 11: tombstone GC\n+ content dedup"]
    L["Ch 12: sharded\nmetadata service"]

    A -->|"fix: fast reads\nnew: single copy"| B
    B -->|"fix: durability\nnew: 3x cost"| C
    C -->|"fix: storage cost\nnew: single region"| D
    D -->|"fix: disaster recovery\nnew: stale remote reads"| E
    E -->|"fix: consistency\nnew: master overload"| F
    F -->|"fix: master load\nnew: stale mappings"| G
    G -->|"fix: staleness, viral reads\nnew: album fan-out"| H
    H -->|"fix: read locality\nnew: failed big uploads"| I
    I -->|"fix: upload resilience\nnew: slow delete, duplicates"| J
    J -->|"fix: fast delete, no waste\nnew: master ceiling"| K
    K -->|"fix: metadata scale"| L
```

```mermaid
mindmap
  root((Why a blob store\nneeds all of this))
    Small files
      one inode per photo = too many seeks
      pack into volumes, index in RAM
    Durability
      one disk = one copy = SPOF
      split metadata from bytes, replicate 3x
    Cost
      3x on cold data is wasteful
      erasure code aged data instead
    Disaster recovery
      one region = one blast radius
      async cross-region copy
    Consistency
      async copy can lag
      strong within region, eventual only for remote
    Read scale
      master cannot answer every read
      client caches mapping, CDN caches bytes
    Cache staleness
      master can move data under a cache
      version the mapping, not just TTL it
    Listing
      wrong partition key scatters an album
      partition by the key you read by
    Big uploads
      one dropped packet restarts everything
      multipart + pre-signed direct upload
    Delete and dedup
      sync delete is slow, duplicates waste space
      tombstone + async GC, content-addressable dedup
    Metadata scale
      one master hits a QPS ceiling
      shard the metadata service itself
```

Every real production blob store sits *somewhere* on this chain. The skill isn't reciting all eleven chapters — it's knowing where to stop, based on the stated requirements.

- A small internal file-upload tool might reasonably stop around Chapter 4.
- A YouTube- or S3-scale system needs to reach Chapters 9 through 12.
- If nobody has mentioned multi-region disaster recovery, walking there unprompted reads as padding, not depth.

---

## Grill me — adversarial follow-ups

**Q1: "Why not just buy bigger disks instead of doing all this Haystack packing?"**

Bigger disks push the wall further out but don't remove it. The problem isn't capacity — it's that every small file pays a fixed metadata-lookup cost regardless of disk size. Haystack packing fixes the actual bottleneck (seeks per read), not just the symptom (running out of space).

**Q2: "If erasure coding is cheaper, why not use it everywhere and skip replication entirely?"**

Because erasure coding's rebuild is CPU- and network-heavy — it requires reading 10 surviving shards and re-running the encoding math. That's fine for cold data you rarely touch. But for hot data, a node failure needs to be invisible to an active reader *right now*. Replication rebuilds instantly. You pick per data temperature, not universally.

**Q3: "Your strong-consistency fix in Chapter 6 waits for all sync replicas before acking — doesn't that make every write slow?"**

It makes every write as slow as the slowest of three nearby, same-region replicas. Because those replicas are deliberately placed close together, that's fast. The genuinely slow, eventually-consistent piece is the cross-region copy — and that one happens *after* the client already has their answer, so it's never on the latency path the user feels.

**Q4: "Isn't client-side caching of the chunk mapping just moving the staleness problem around instead of solving it?"**

Partially yes — that's an honest trade-off, not a hidden flaw. Caching removes the master as a bottleneck for the 99% common case. Versioning the cache turns the rare staleness case into one cheap fallback round-trip instead of either silent failure or paying the master-round-trip cost on every single read.

**Q5: "Why partition by account+album+blob instead of just sharding by account_id alone — isn't that simpler?"**

It's close, but album-level co-location still matters once one account has many large albums spread thin. The point is picking the key that matches the dominant read (`listAlbumPhotos`). Composite keys let you tune that precisely, instead of only getting account-level locality.

**Q6: "Multipart upload retries failed parts — what stops a client from completing the upload with a corrupted part?"**

Each part carries a checksum, verified independently when it lands. The final `completeMultipartUpload` call re-verifies against the ETags returned for every part before stitching. A part that doesn't match its expected checksum gets rejected and re-uploaded, not silently accepted.

**Q7: "Content-addressable dedup sounds risky — what if two different photos happen to hash to the same value?"**

With SHA-256, a collision is astronomically unlikely — far less likely than an undetected hardware failure. Every real system using this technique (backup systems, git itself) accepts that risk as effectively zero rather than engineering around it.

**Q8: "Doesn't tombstone-then-reclaim mean a determined attacker could 'delete' something and still read it during the retention window?"**

Yes, and that's deliberate, not a bug. The retention window exists specifically so an *accidental* delete is recoverable. If a design genuinely needs "gone means gone, instantly, with no recovery window," that's a different, explicit requirement to negotiate up front — not the default.

**Q9: "You keep saying 'shard the master' — isn't the routing layer in front of the shards just a new single point of failure?"**

The routing layer itself needs to be stateless and horizontally replicated. It only holds a small, mostly-static range map, so it's cheap to run many copies behind a load balancer. The actual hard consistency problem is inside each metadata shard's own leader election — which is why that piece specifically gets a real consensus protocol, not the routing layer.

**Q10: "Given this whole story, if someone says 'design a blob store' cold, where do you actually start?"**

Get two things clear before drawing anything:
1. Is this write-once-read-many? (Almost always yes for blob stores.)
2. What's the durability target?

Those two answers dictate the replication vs. erasure coding choice, the consistency model, and the multi-region strategy — all at once.

Then walk forward only as far as the stated scale requires. Chunking and basic replication are close to a given. Multi-region, dedup, and sharded metadata are things you earn by naming a specific scale or requirement — not defaults you bolt on for their own sake.

---

## Pacing note

**If this is 60 seconds inside a bigger question:**

Say the manifest-clerk line — split "where is it" from "the bytes," pack small files, chunk big ones, replicate hot data and erasure-code cold data, cache the metadata not just the bytes. Then say: "I'd go deeper into consistency, partitioning, or garbage collection if you want a specific deep dive." That's the whole shape in one breath.

**If this is the whole 15–20 minute focus:**

Walk the chapters in order:
1. Why small files hurt filesystems.
2. Splitting metadata from bytes.
3. Replication vs. erasure coding by data temperature.
4. Multi-region disaster recovery and what that does to consistency.
5. Client-side caching and its staleness fix.
6. Partitioning by read pattern.
7. Multipart upload and pre-signed URLs.
8. Tombstone-based delete and dedup.
9. Sharding the metadata service if scale comes up.

Don't walk all eleven chapters unprompted. Follow the interviewer's questions, and use the skipped chapters as your "if I had more time" closer.

---

## Active recall — no answers, test yourself cold

1. What's the one-sentence reason a naive filesystem struggles with millions of small photos?
2. What does Haystack packing actually do, and why does it turn ~10 seeks into 1?
3. Why does 3x replication on *all* data eventually become a cost problem, and what's the fix for cold data specifically?
4. Name the three tiers of replica placement and the specific failure each tier defends against.
5. What's the precise difference between what's strongly consistent and what's eventually consistent in this design?
6. Why does client-side caching of the chunk-to-node mapping need a version/epoch number instead of just a TTL?
7. Walk through why partitioning photo metadata by `photo_id` alone breaks `listAlbumPhotos`.
8. What two separate problems does multipart upload plus a pre-signed URL solve, and which one is about resilience vs. which is about bandwidth?
9. Why must garbage collection check a reference count, not just a tombstone, before reclaiming a chunk's bytes?
10. What real, documented system made the exact move of sharding a single master into a distributed metadata layer?

*Spaced repetition: test this list today, again in 2–3 days, again in a week.*

---

## Cheat sheet — one line per stop on the story

- **Small-file overhead**: a plain filesystem pays multiple real disk seeks per tiny file — Haystack packing turns that into one seek by packing many files into one volume with an in-memory offset index.
- **Metadata/data split**: a master (manifest clerk) tracks where bytes live; dumb data nodes just hold them — this split, plus chunking big objects, is what lets replication and placement scale independently of the bytes.
- **Replication vs. erasure coding**: replicate hot data (fast rebuild, 3x cost); erasure-code cold data (slow, CPU-heavy rebuild, ~1.4x cost) — pick by data temperature, not universally.
- **Multi-region placement**: rack copy defends against a power failure, same-region DC copy against a fire or flood, remote-region copy against the region itself going dark.
- **Consistency**: strong read-after-write within the primary region; the async cross-region copy is the only piece allowed to be eventually consistent, and only until it lands.
- **Client-side metadata caching**: skip the master on every warm read by caching the chunk-to-node mapping — version it with an epoch so a rebalance triggers one cheap refresh instead of silent staleness.
- **CDN in front**: safe with zero invalidation logic because objects are immutable — a new version is always a new URL.
- **Partition key choice**: partition by the composite key you actually read by (account + album + blob), not by the ID that merely balances writes evenly.
- **Multipart upload + pre-signed URLs**: retry only the failed part instead of the whole object, and let the client talk directly to storage instead of tunneling bytes through the app tier.
- **Tombstone + async GC**: delete is instant in metadata, byte reclaim happens later, gated by a retention window and a reference count of zero.
- **Content-addressable dedup**: hash the bytes; if it already exists, add a pointer and bump a refcount instead of storing a duplicate copy — safe only because objects are immutable.
- **Sharded metadata service**: once one master hits its QPS ceiling, shard it the same way the data itself is sharded, with per-shard leader election for failover — the real move from GFS to Colossus, and Azure's stream-layer-plus-partition-layer design.
- **The meta-lesson**: every fix in this story buys one property (fast reads, durability, cost efficiency, disaster tolerance, correctness, read scale, cache freshness, read locality, upload resilience, fast delete, no wasted bytes, or metadata scale) by spending or risking a different one — say the trade in the same sentence you propose the fix.
