# The Story of URL Shorteners 🧵

Let's start from the very beginning — **why did this problem even exist?**

---

## Chapter 1: The Problem

It's **2001**. The internet is booming. People are sharing links over emails, forums, and early social media.

Imagine you want to share this link with your friend:

```
https://www.amazon.com/Best-Sellers-Books-Mystery-Thrillers-Suspense/zgbs/books/18/ref=zg_bs_nav_books_1_b?ie=UTF8&paginationIndex=1&refinementId=1234567
```

You paste it in an **email**. The email client wraps it. Your friend clicks it. **It breaks.** Half the URL is on line 1, half on line 2.

Or worse — Twitter (2006) gives you only **140 characters**. That URL alone is 150+ characters. **Your entire tweet budget is gone before you even say anything.**

People needed a way to share long URLs in a **short, clean format.**

---

## The Simplest Idea 💡

Someone thought:

> *"What if I just give every long URL a short code, store the mapping in a database, and redirect anyone who visits the short code?"*

So the flow is:

```
User visits:     short.ly/aX3k9
   ↓
Server looks up: aX3k9 → https://www.amazon.com/very/long/url...
   ↓
Server says:     "Go here instead" (HTTP Redirect)
   ↓
User lands on:   The original long URL ✅
```

Simple. Elegant. **Let's build this.**

---

## The Naive Implementation

```
Table: urls
┌──────────┬────────────────────────────────────┐
│ short_id │ long_url                           │
├──────────┼────────────────────────────────────┤
│ aX3k9    │ https://amazon.com/very/long/url   │
│ bY7m2    │ https://google.com/search?q=cats   │
└──────────┴────────────────────────────────────┘
```

**To create a short URL:**
1. Take the long URL
2. Generate a random short code (e.g., `aX3k9`)
3. Store the mapping in DB
4. Return `short.ly/aX3k9`

**To redirect:**
1. Someone visits `short.ly/aX3k9`
2. Look up `aX3k9` in DB
3. Return HTTP **301/302 redirect** to the long URL

---

This works! ...for 10 users. 😄

**But here comes the first question that will break your brain a little:**

---

> 🤔 **How do you generate the short code `aX3k9`?**
> - It needs to be **short** (6-8 chars ideally)
> - It needs to be **unique** (two URLs can't get the same code)
> - It needs to be **fast to generate** (millions of requests/day)

**This is where things get really interesting.**

---

Take a moment — what approach would *you* try first?

Once you're ready, tell me and we'll dive into **Chapter 2: How to Generate Short Codes** — where we explore 3 different approaches, each with their own problems. 🚀

---

# Chapter 2: How Do You Generate the Short Code?

Let's think about this carefully. We need something like `aX3k9` — short, unique, fast.

---

## Approach 1: Just Use a Random String 🎲

The most obvious idea. Just generate a random 6-character string every time.

```
Characters available: a-z, A-Z, 0-9
That's 26 + 26 + 10 = 62 characters

6-character code = 62⁶ = ~56 billion combinations
```

Seems more than enough. Let's code the logic:

```
1. Generate random 6-char string → "aX3k9"
2. Check if "aX3k9" already exists in DB
3. If yes → generate again
4. If no  → store it ✅
```

### The Problem 💀

At small scale? Perfect.

But imagine you're **bit.ly** with **10 billion URLs** stored.

```
Total possible codes:     56 billion
Already used codes:       10 billion
Probability of collision: 10/56 = ~18% chance on every generation
```

So now for **every new URL**, there's a 1-in-5 chance you generate a code that's already taken. You retry. Maybe collide again. Maybe 3-4 times.

At massive scale, this becomes a **slot machine you keep losing.**

And it gets worse — checking the DB on every attempt means **N database reads just to create one URL.** That's expensive.

> ❌ Random strings work at small scale but **collision rate grows as the database fills up.**

---

## Approach 2: Hash the Long URL 🔐

Someone smarter says:

> *"Why random? The long URL itself is unique — let's just hash it!"*

You take the long URL, run it through **MD5 or SHA256**, and take the first 6 characters.

```
Input:  https://amazon.com/very/long/url
MD5:    e3b0c44298fc1c149afb4c8996fb924
Take 6: e3b0c4   ← your short code
```

Beautiful! Same URL always gives same code. No DB check needed to verify uniqueness... right?

### Problem 1: Collisions Still Exist 💥

Two **different** long URLs can produce the same 6-character prefix.

```
https://amazon.com/product-A  →  MD5 → e3b0c4...
https://flipkart.com/item-99  →  MD5 → e3b0c4...  ← collision!
```

Now `e3b0c4` maps to **two different URLs**. You can only store one. The other is silently lost. 😬

### Problem 2: Same URL = Same Short Code

Wait, isn't that a feature? Actually no — it can be a problem.

Two **different users** submit the same long URL expecting their own short link (maybe they want separate analytics). With hashing, they get the **exact same code.**

> ❌ Hashing is deterministic but **collisions are unpredictable** and it breaks personalization.

---

## Approach 3: Auto-Increment ID + Base62 Encoding ✨

This is the approach that actually works. Let's build the intuition slowly.

### Step 1 — The Database Gives You a Free Unique Number

Every database has **auto-increment IDs**. Every time you insert a row, you get a unique integer. Forever increasing. Never repeating.

```
Row 1  → ID: 1
Row 2  → ID: 2
Row 3  → ID: 3
...
Row 9999999 → ID: 9999999
```

**Uniqueness is guaranteed by the DB itself.** No collision checks needed.

### Step 2 — But integers make ugly URLs

`short.ly/9999999` is 7 characters and only numbers. Not great.

What if we could **encode that number in a shorter, alphanumeric way?**

### Step 3 — Base62 Encoding 🔢

You already know **Base10** (digits 0-9, ten symbols).
You know **Base16/Hex** (0-9 + a-f, sixteen symbols).

**Base62** uses 62 symbols: `0-9 + a-z + A-Z`

Just like how:
```
Base10:  255  (3 digits)
Base16:  FF   (2 digits)  ← shorter!
Base62:  47   (2 digits)  ← same length but alphanumeric
```

The **higher the base, the shorter the representation** of large numbers.

Let's encode ID `1000` in Base62:

```
62¹ = 62
62² = 3,844
62³ = 238,328

1000 ÷ 62 = 16 remainder 8
16   ÷ 62 = 0  remainder 16

Base62 digits: [0-9=0-9, 10=a, 11=b ... 16=g, 8=8]
So 1000 in Base62 = "g8"
```

Just **2 characters** for the number 1000!

### How far does this scale?

```
6-character Base62 code = 62⁶ = 56 billion unique URLs
7-character Base62 code = 62⁷ = 3.5 trillion unique URLs
```

### The Full Flow Now:

```
User submits: https://amazon.com/very/long/url

1. INSERT into DB → get back auto-increment ID: 5000
2. Encode 5000 in Base62 → "1cO"
3. Store: { id: 5000, short: "1cO", long: "https://amazon.com/..." }
4. Return: short.ly/1cO  ✅

User visits: short.ly/1cO
1. Decode "1cO" from Base62 → 5000
2. Look up ID 5000 in DB
3. Redirect to long URL ✅
```

**No collision checks. No retries. O(1) lookup by primary key. Mathematically guaranteed unique.**

---

## The Comparison Table

| Approach | Unique? | Collision-Free? | Fast? | Scales? |
|---|---|---|---|---|
| Random String | ✅ | ❌ Retry needed | ❌ Multiple DB reads | ❌ Gets worse |
| Hash (MD5) | ❌ | ❌ Unpredictable | ✅ | ❌ |
| Auto-ID + Base62 | ✅ | ✅ Guaranteed | ✅ O(1) | ✅ |

---

## But Wait... 🤔

Auto-increment ID sounds perfect. But here's the **next problem that will keep you up at night:**

Your short URLs are now **predictable.**

```
short.ly/1cO  → ID 5000
short.ly/1cP  → ID 5001
short.ly/1cQ  → ID 5002
```

A competitor can just **walk through every ID sequentially** and scrape every URL you've ever shortened. Your users' private links? **Gone.**

Also — what happens when your service gets **millions of requests per second** and one database can't keep up?

---

These are the exact problems that lead to **Chapter 3**, where we talk about:
- Making codes **unpredictable** without losing uniqueness
- What happens when **one DB server is not enough**

Whenever you're ready — just say **go!** 🚀

---

# Chapter 3: The Predictability Problem & Scaling the Database

Let's tackle both problems one by one.

---

## Problem 1: Your Short Codes Are Predictable 🔓

Remember our Base62 approach? Sequential IDs mean sequential codes.

```
User 1 creates a URL → short.ly/1cO  (ID: 5000)
User 2 creates a URL → short.ly/1cP  (ID: 5001)
User 3 creates a URL → short.ly/1cQ  (ID: 5002)
```

A malicious actor writes a simple script:

```python
for i in range(1, 10_000_000):
    visit("short.ly/" + base62_encode(i))
    # Now they have EVERY URL ever shortened
```

Imagine someone shortened their **private Google Drive link**, or a **confidential document**, or a **personal photo album.** All exposed. 😨

### Attempt 1: Add Random Salt 🧂

Someone says: *"Just shuffle the ID before encoding!"*

```
ID: 5000
Add random salt: 5000 + 99999 = 104999
Encode: base62(104999) = "q4R"
```

But now you **can't decode it back.** You've lost the mapping. You still need to hit the DB to find the original URL. The salt trick doesn't really solve anything cleanly.

### Attempt 2: Shuffle the Base62 Alphabet 🔀

Clever idea. Instead of the standard alphabet:

```
Standard: 0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ
```

You use a **secret shuffled version** known only to your server:

```
Shuffled: 9q2mZ1xKpLdR8nWvYoGe0BsTfJuAiHc6lEh4tM7NwXrDgbk3yCjQFPOVzUsIa5
```

Now when you encode ID `5000`, you use **your secret alphabet** instead of the standard one. The output looks completely random to outsiders, but your server can always decode it back.

```
ID 5000 → Base62 with standard alphabet → "1cO"
ID 5001 → Base62 with standard alphabet → "1cP"

ID 5000 → Base62 with shuffled alphabet → "mK9"   ← looks random!
ID 5001 → Base62 with shuffled alphabet → "xQ2"   ← no pattern!
```

**The secret is the alphabet order.** Outsiders see random codes. Your server always decodes correctly. No extra DB lookups needed.

> ✅ Codes look random. ✅ Still decodable. ✅ No collision issues.

### Attempt 3: Just Use a UUID Prefix (Simplest Practical Solution)

Many real systems just generate a random 7-8 character string **and accept occasional retries** because at their scale, collisions are rare enough. They combine this with a **unique constraint on the DB column**, so the DB itself rejects duplicates.

```
Generate "mK9xQ2p"
Try INSERT → DB says UNIQUE VIOLATION
Generate "aB3nR7q"  
Try INSERT → Success ✅
```

At most 1-2 retries ever happen in practice. Simple and works fine.

---

## Problem 2: One Database Can't Handle This 🏋️

Let's talk about scale. bit.ly at peak handles:

```
~6 billion clicks/month
~200 million clicks/day  
~2,300 clicks/second
```

And creating new short URLs adds even more write load.

**One MySQL server on your laptop?** Falls over at maybe 1,000 queries/second.

### The First Bottleneck: Read vs Write

First insight — **reads massively outnumber writes.**

```
Creating a short URL:   1 write
Clicking a short URL:   1 read

One URL can be clicked MILLIONS of times.
Ratio is easily 1000:1 reads vs writes.
```

This means we can solve most of the problem by scaling reads separately from writes.

### Solution: Primary-Replica (Master-Slave) Setup

```
                    ┌─────────────┐
   All WRITES  ───► │   Primary   │
                    │     DB      │
                    └──────┬──────┘
                           │ replicates data
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │ Replica 1│ │ Replica 2│ │ Replica 3│
        └──────────┘ └──────────┘ └──────────┘
              ▲            ▲            ▲
   All READS ─┴────────────┴────────────┘
```

- **One Primary DB** handles all writes (creating short URLs)
- **Multiple Replica DBs** handle all reads (redirecting clicks)
- Primary constantly **syncs data to replicas**
- Reads are load-balanced across replicas

Now your read capacity is **3x, 5x, 10x** just by adding replica servers.

### But What About the Auto-Increment ID Problem? 🤯

Remember our short code generation depends on the auto-increment ID from the DB. But now with multiple servers, each one has its own auto-increment counter:

```
Primary Server 1: generates IDs 1, 2, 3, 4, 5...
Replica Server A: generates IDs 1, 2, 3, 4, 5...  ← SAME IDs! COLLISION!
```

This is a real problem. Two servers independently think they're the authority on ID generation.

### Fix: Give Each Server Its Own ID Range

Configure each DB server to increment differently:

```
Server 1: starts at 1, increments by 3 → 1, 4, 7, 10, 13...
Server 2: starts at 2, increments by 3 → 2, 5, 8, 11, 14...
Server 3: starts at 3, increments by 3 → 3, 6, 9, 12, 15...
```

**No server ever generates the same ID.** Problem solved... mostly.

But this is getting complicated. What if you add a 4th server? You have to reconfigure all of them. What if a server goes down?

---

## Enter: The Ticket Server (A Dedicated ID Generator)

This is what Flickr actually built. Instead of trusting each DB to generate IDs, you have **one dedicated service** whose only job is handing out unique IDs:

```
                    ┌────────────────┐
                    │  Ticket Server │
                    │                │
                    │  Next ID: 5001 │
                    └───────┬────────┘
                            │
          ┌─────────────────┼─────────────────┐
          ▼                 ▼                 ▼
   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
   │  App Server │  │  App Server │  │  App Server │
   │    #1       │  │    #2       │  │    #3       │
   └─────────────┘  └─────────────┘  └─────────────┘
```

Any app server that needs to create a short URL asks the Ticket Server:

```
App Server: "Give me an ID"
Ticket Server: "Here's 5001" → atomically increments to 5002
App Server: encodes 5001 → "1cO" → stores URL
```

**Atomically** is the key word here. The Ticket Server uses database-level locks to ensure two servers never get the same ID.

### But Now the Ticket Server is a Single Point of Failure! 😬

If it goes down, **nobody can create new short URLs.**

Solution: Run **two Ticket Servers**, one for odd IDs and one for even:

```
Ticket Server A: 1, 3, 5, 7, 9...   (odd)
Ticket Server B: 2, 4, 6, 8, 10...  (even)
```

If one dies, the other keeps running. You just lose half your ID throughput temporarily. Acceptable.

---

## The Caching Layer: Because The DB Is Still Slow 🚀

Even with replicas, **every single click hits the database.** For a URL clicked 50 million times, that's 50 million DB reads for the **exact same row.**

This is where **Redis** (or Memcached) enters the story.

```
User clicks short.ly/mK9
         │
         ▼
  ┌─────────────┐   Cache HIT?  ──YES──► Return long URL instantly ⚡
  │  Redis Cache│                              (< 1ms)
  └─────────────┘
         │ Cache MISS
         ▼
  ┌─────────────┐
  │  Database   │ ──► Return long URL, ALSO store in cache
  └─────────────┘           (so next time is a cache hit)
```

The first click fetches from DB and **populates the cache.**
Every subsequent click hits **Redis in under 1 millisecond.**

For a viral URL clicked 10 million times:
```
Without cache: 10,000,000 DB reads
With cache:    1 DB read + 9,999,999 Redis reads

DB load reduced by 99.99% ✅
```

---

## Where We Are Now 🗺️

Our system has evolved from a single DB to this:

```
User
 │
 ▼
Load Balancer
 │
 ▼
App Servers (multiple)
 │
 ├──► Redis Cache (reads, < 1ms)
 │         │ miss
 ├──► Replica DBs (reads)
 │
 └──► Primary DB (writes only)
            │
            └──► Ticket Server (ID generation)
```

This handles **millions of requests per day** pretty comfortably.

---

## But here's what we haven't solved yet 🤔

> What happens when your service becomes **globally popular?**
> A user in **Mumbai** hits your short URL.
> Your servers are in **Virginia, USA.**
> The redirect takes **400ms** just due to physical distance.

And also — what does your system do when someone creates the **same long URL twice?** Do you give them the same short code or a new one?

These are exactly what **Chapter 4** covers — **Global Distribution & Deduplication.**

Say **go** whenever you're ready! 🚀

---

# Chapter 4: Global Distribution & Deduplication

---

## Problem 1: The Speed of Light is Your Enemy 🌍

Your servers are in Virginia, USA. A user in Mumbai clicks a short URL.

Here's what physically happens:

```
Mumbai User                          Virginia Server
     │                                      │
     │——— request travels ~13,000 km ———————►│
     │                                      │ processes
     │◄—— response travels ~13,000 km ———————│
     │
  Total: 400-500ms just from distance alone
```

400ms might not sound like much. But:
- Google found that **500ms delay = 20% drop in traffic**
- Amazon found **100ms delay = 1% loss in revenue**
- Every redirect feeling "slow" destroys user trust

And this isn't a code problem. You **cannot** make light travel faster. The only solution is to move your servers **closer to the users.**

---

## The Solution: CDN + Edge Servers 🌐

The idea is simple:

> Instead of one data center in Virginia, have **many small data centers** spread across the globe. Route each user to the **nearest one.**

```
          [Virginia DC]
         /      |       \
   [London]  [Mumbai]  [Singapore]
      DC        DC          DC

Mumbai user ──────────────► Mumbai DC  (5ms, not 400ms) ⚡
London user ──────────────► London DC  (8ms) ⚡
New York user ────────────► Virginia DC (3ms) ⚡
```

These are called **Edge Locations** or **Points of Presence (PoPs).**

Companies like Cloudflare have **300+ edge locations** worldwide. AWS CloudFront has **450+.**

### How Does Routing Work?

When you type `short.ly/mK9`, your DNS lookup returns the IP address of the **nearest edge server**, not your origin server. This is called **GeoDNS** or **Anycast routing.**

```
Mumbai user DNS lookup: short.ly
  → GeoDNS checks user location
  → Returns IP of Mumbai edge server
  → User connects to Mumbai (5ms away)

London user DNS lookup: short.ly
  → GeoDNS checks user location
  → Returns IP of London edge server
  → User connects to London (8ms away)
```

### But What Does the Edge Server Actually Have?

This is the key question. The edge server is a **lightweight cache + redirect machine.** It stores the most popular URL mappings locally.

```
Mumbai Edge Server Cache:
┌──────────┬─────────────────────────────────┐
│ short_id │ long_url                        │
├──────────┼─────────────────────────────────┤
│ mK9      │ https://instagram.com/viral-post│
│ xQ2      │ https://youtube.com/trending    │
│ ...      │ (top 10,000 URLs for this region│
└──────────┴─────────────────────────────────┘
```

**Cache HIT** → Edge server redirects instantly. Never touches Virginia.
**Cache MISS** → Edge server calls Virginia, gets the URL, caches it, redirects user.

```
First click on mK9 from Mumbai:
  Mumbai edge ──miss──► Virginia DB ──► gets URL
  Mumbai edge ──caches mK9──► redirects user   (150ms)

Every subsequent click on mK9 from Mumbai:
  Mumbai edge ──hit──► redirects instantly     (5ms) ⚡
```

---

## But Now We Have a Data Consistency Problem 😬

Imagine this scenario:

```
User in Mumbai:  creates short URL → short.ly/mK9 → stored in Virginia DB
                                                     Virginia syncs to Mumbai
                                                     (sync takes 200ms)

User in London:  clicks short.ly/mK9 IMMEDIATELY
                 London edge: "Never heard of mK9"
                 London edge calls Virginia: "Oh here it is"
                 ✅ Works but slower

User in Mumbai:  clicks short.ly/mK9 IMMEDIATELY  
                 Mumbai edge: "Never heard of mK9" (sync not done yet!)
                 Mumbai edge calls Virginia: ✅ Works but slower
```

This is called **Eventual Consistency** — all nodes will *eventually* have the same data, but there's a brief window where they're out of sync.

For URL shorteners, this is **completely acceptable.** The worst case is one slightly slow redirect right after creation. Not a disaster.

But compare this to a banking system — you transfer money and the balance shows wrong for 200ms. **Unacceptable.** Different systems need different consistency guarantees.

> URL Shorteners are a perfect use case for **eventual consistency** — we trade perfect consistency for massive speed gains. ✅

---

## Problem 2: The Same URL Submitted Twice 🔁

Now let's think about deduplication. What happens here:

```
User A submits: https://youtube.com/watch?v=abc123
System creates: short.ly/mK9

User B submits: https://youtube.com/watch?v=abc123  ← same URL!
System creates: short.ly/xQ2  ← different code!
```

Now two different short URLs point to the same destination. Is this a problem?

**It depends on what you're building:**

```
Scenario 1 — Public shortener (like bit.ly free tier)
  → You probably want deduplication
  → Same URL = same short code
  → Saves storage, avoids confusion

Scenario 2 — Analytics-focused shortener
  → User A and User B each want THEIR OWN link
  → So they can track their own click stats separately
  → Same URL = DIFFERENT short codes ← correct behavior
```

Most real systems offer **both options.** But let's focus on deduplication since it's the harder technical problem.

---

## How Do You Detect Duplicate URLs? 🔍

### Naive Approach: Search the DB

```
User submits: https://youtube.com/watch?v=abc123

SELECT short_id FROM urls WHERE long_url = 'https://youtube.com/...'
→ Found! Return existing short.ly/mK9
```

Sounds easy. But `long_url` is a text column. Long URLs can be **2,000+ characters.** Doing string comparison on 2,000 characters across **billions of rows** is painfully slow.

Even with an index on `long_url`, storing and comparing long strings as index keys is **expensive.**

### Better Approach: Hash the Long URL for Lookup 🔐

Instead of indexing the long URL itself, store a **hash of it:**

```
long_url:  https://youtube.com/watch?v=abc123&list=xyz&t=30s...
MD5 hash:  a3f5c8d2e1b4...  (always 32 chars, regardless of URL length)
```

Now your table looks like:

```
┌──────────┬──────────────────────────────────┬────────────────┐
│ short_id │ long_url                         │ url_hash       │
├──────────┼──────────────────────────────────┼────────────────┤
│ mK9      │ https://youtube.com/watch?v=abc  │ a3f5c8d2e1b4.. │
│ xQ2      │ https://instagram.com/viral-post │ 9d2c1a7f3e8b.. │
└──────────┴──────────────────────────────────┴────────────────┘
```

Index on `url_hash` (fixed 32 chars, fast!):

```
User submits new URL
  ↓
Hash it → a3f5c8d2e1b4...
  ↓
SELECT * FROM urls WHERE url_hash = 'a3f5c8d2e1b4...'
  ↓
Found? → Return existing short code
Not found? → Create new one
```

**Blazing fast lookup.** Fixed-size comparison. Index is small and efficient.

### But Wait — Hash Collisions! 😱

Two different URLs can produce the same MD5 hash (extremely rare, but possible):

```
URL A: https://legitimate-site.com/page  →  hash: a3f5c8d2
URL B: https://different-site.com/other →  hash: a3f5c8d2  ← same hash!
```

You'd return the wrong short URL for URL B!

The fix is simple — **always verify:**

```
1. Hash new URL → a3f5c8d2
2. Look up hash in DB → find a row
3. Compare actual long_url strings to confirm they match
4. If strings match → genuine duplicate, return existing code ✅
5. If strings differ → hash collision, treat as new URL ✅
```

The hash lookup narrows you down to 1-2 rows. Then you do an exact string comparison on just those rows. **Fast AND correct.**

---

## Bloom Filters: Checking Duplicates Without Hitting DB At All 🌸

At extreme scale, even fast hash lookups add up. What if you could check **"have I seen this URL before?"** in microseconds, using almost no memory, **without touching the DB?**

This is what a **Bloom Filter** does.

Think of it as a **magic checklist** with a superpower and one flaw:
- ✅ If it says **"Never seen it"** → 100% correct, definitely new URL
- ⚠️ If it says **"Seen it before"** → probably correct, but rarely wrong (false positive)

How it works internally:

```
Bloom Filter = a bit array of 1 million 0s
               + 3 different hash functions

Adding URL "youtube.com/abc":
  hash1("youtube.com/abc") = position 142  → set bit[142] = 1
  hash2("youtube.com/abc") = position 891  → set bit[891] = 1
  hash3("youtube.com/abc") = position 234  → set bit[234] = 1

Checking URL "youtube.com/abc":
  hash1 → position 142 → bit is 1 ✓
  hash2 → position 891 → bit is 1 ✓
  hash3 → position 234 → bit is 1 ✓
  All set → "Probably seen before" → go verify in DB

Checking URL "netflix.com/xyz":
  hash1 → position 445 → bit is 0 ✗
  → Instantly "Never seen!" → Skip DB entirely → Create new URL ✅
```

The beauty: **one bit array in memory** can represent **billions of URLs.** Checking takes microseconds.

```
Flow with Bloom Filter:

New URL submitted
       │
       ▼
  Bloom Filter check
       │
  ┌────┴────┐
 "No"      "Maybe"
  │            │
  ▼            ▼
Create      Check DB
new URL    (verify)
  ✅      ┌────┴────┐
        Match    No match
          │         │
        Return    Create
        existing  new URL
          ✅        ✅
```

**Result: Most new URLs skip the DB entirely.** Only potential duplicates go to DB for verification.

---

## Our System Now 🗺️

```
User (Mumbai)
    │
    ▼
Mumbai Edge Server
    │
    ├──► Cache HIT → redirect instantly (5ms) ⚡
    │
    └──► Cache MISS → call origin
              │
              ▼
         Load Balancer (Virginia)
              │
              ▼
         App Servers
              │
         ┌────┴──────────────┐
         │                   │
         ▼                   ▼
   Bloom Filter          Redis Cache
   (dedup check)         (hot URLs)
         │                   │
         └────────┬──────────┘
                  │
                  ▼
           Primary DB (writes)
           Replica DBs (reads)
                  │
           Ticket Server (IDs)
```

---

## What We Haven't Solved Yet 🤔

We've handled speed, scale, uniqueness, and deduplication. But there's a whole category of features we haven't talked about:

> - What happens when someone shortens a **malicious URL** (phishing, malware)?
> - What if a URL **expires** after 30 days?
> - How do you give users **analytics** — who clicked, from where, on what device?
> - The **301 vs 302** redirect debate — which one do you use and why does it matter enormously for analytics?

These are the real-world problems that **Chapter 5** covers — **Safety, Expiry, and Analytics.**

Say **go** when you're ready! 🚀


---

# Chapter 4.5: Sharding — Splitting Data Across Multiple Databases

Let's forget everything for a second and start from the raw problem.

---

## The Problem: One DB Can't Hold It All 📦

Let's look at the numbers:

```
bit.ly handles ~6 billion URLs stored
Average URL row size ~ 500 bytes

6,000,000,000 × 500 bytes = 3 TB of data

A single MySQL server comfortably handles ~500GB - 1TB
→ You've already blown past the limit 💀
```

And it's not just storage. Even with replicas:

```
Writes: 1000 new URLs/second → all hit ONE primary DB
Reads:  100,000 clicks/second → replicas help, but...

What if your PRIMARY dies?
→ No writes possible. Service is broken.
```

Replicas solve **read scaling.** They do NOT solve:
- ❌ Write scaling (still one primary)
- ❌ Storage limits (every replica holds ALL data)
- ❌ Single point of failure on writes

**Sharding** solves all three.

---

## What is Sharding? ✂️

Sharding means **splitting your data horizontally across multiple databases**, where each database only holds a **portion** of the total data.

```
WITHOUT SHARDING:
┌─────────────────────────┐
│       One Giant DB      │
│  URL 1, 2, 3 ... 6B     │ ← stores everything
└─────────────────────────┘

WITH SHARDING:
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│    Shard 1    │   │    Shard 2    │   │    Shard 3    │
│  URL 1 - 2B  │   │  URL 2B - 4B  │   │  URL 4B - 6B  │
└───────────────┘   └───────────────┘   └───────────────┘
```

Each shard is an **independent database** with its own storage, CPU, RAM.

Now the key question:

> **How do you decide which shard a piece of data goes to?**

This is called the **Sharding Strategy.** There are 3 main ones. Let's go through each.

---

## Strategy 1: Range-Based Sharding 📏

The simplest idea. Divide data by ID ranges.

```
Shard 1: IDs        1 → 2,000,000,000
Shard 2: IDs 2,000,000,001 → 4,000,000,000
Shard 3: IDs 4,000,000,001 → 6,000,000,000
```

To find which shard to hit:

```
URL ID = 3,500,000,000
→ Falls in range 2B - 4B
→ Hit Shard 2
```

Simple math. No lookup needed.

### The Problem: Hot Shards 🔥

URLs are created sequentially. So **all new writes go to Shard 3** (the latest range).

```
Shard 1: 2 billion URLs, created years ago, barely touched
Shard 2: 2 billion URLs, created last year, moderate traffic
Shard 3: current shard, ALL new writes, ALL recent clicks ← ON FIRE 🔥
```

Shard 3 is overloaded while Shard 1 is basically idle. This is called a **hot shard problem.**

> ❌ Range sharding creates uneven load. Some shards are overwhelmed, others are wasted.

---

## Strategy 2: Hash-Based Sharding 🎯

Instead of ranges, you **hash the ID** and use the result to pick a shard.

```
Number of shards: 3

short_id = "mK9"
hash("mK9") = 18274619
18274619 % 3 = 1  → Go to Shard 1

short_id = "xQ2"  
hash("xQ2") = 93847261
93847261 % 3 = 2  → Go to Shard 2

short_id = "bR7"
hash("bR7") = 55102938
55102938 % 3 = 0  → Go to Shard 0
```

The modulo operation **distributes data evenly** across all shards. No hot shards!

```
Shard 0: ~33% of all URLs, random mix of old and new
Shard 1: ~33% of all URLs, random mix of old and new
Shard 2: ~33% of all URLs, random mix of old and new
```

Writes and reads are spread uniformly. 

### The Problem: Adding a New Shard 😱

Your 3 shards are getting full. You add a 4th shard.

Now your formula changes from `% 3` to `% 4`:

```
BEFORE (3 shards):
hash("mK9") = 18274619
18274619 % 3 = 1  → Shard 1 ✅

AFTER (4 shards):
hash("mK9") = 18274619
18274619 % 4 = 3  → Shard 3 ❌ WRONG!
```

The same URL now maps to a **completely different shard.** But the data is still sitting in Shard 1!

This means when you add a shard, you have to **move almost all your data** to new locations. For 6 billion URLs, that's a **massive, painful migration** that takes days and risks downtime.

> ❌ Hash sharding is great until you need to resize. Resizing is catastrophic.

---

## Strategy 3: Consistent Hashing 🔄 (The Real Solution)

This is the clever idea that solves the resizing problem. Used by DynamoDB, Cassandra, Discord, and many others.

### The Concept: A Ring

Imagine a **circular ring** with positions from 0 to 360 degrees (or 0 to 2³² in practice).

```
              0°
              │
    270° ─────┼───── 90°
              │
             180°
```

You place your **shards** on this ring by hashing their names:

```
hash("Shard A") = 30°   → placed at 30°
hash("Shard B") = 120°  → placed at 120°
hash("Shard C") = 210°  → placed at 210°
hash("Shard D") = 300°  → placed at 300°
```

```
                 0°
                 │
        Shard A (30°)
       /                \
Shard D (300°)        Shard B (120°)
       \                /
        Shard C (210°)
```

### Placing a URL on the Ring

To find which shard stores a URL:
1. Hash the URL's short code → get a position on the ring
2. **Walk clockwise** until you hit a shard
3. That's your shard

```
hash("mK9") → lands at 80°
Walk clockwise from 80° → first shard hit = Shard B (120°)
→ Store in Shard B ✅

hash("xQ2") → lands at 250°
Walk clockwise from 250° → first shard hit = Shard D (300°)
→ Store in Shard D ✅

hash("bR7") → lands at 340°
Walk clockwise from 340° → wraps around → first shard hit = Shard A (30°)
→ Store in Shard A ✅
```

### Now Add a New Shard — The Magic ✨

You add **Shard E** at position 100°:

```
Before:                          After:
hash("mK9") → 80°               hash("mK9") → 80°
Walk clockwise → Shard B (120°) Walk clockwise → Shard E (100°) ← new!
```

Only URLs that fell **between 30° and 100°** need to move (from Shard B to Shard E). Everything else stays exactly where it is.

```
HASH SHARDING:   Adding 1 shard → move ~75% of all data 😱
CONSISTENT HASH: Adding 1 shard → move ~1/N of data     ✅
```

For 4 shards, adding a 5th only moves **~20% of data** instead of 75%.

---

## Virtual Nodes: Making it Even More Balanced ⚖️

In practice, 4 shards on a ring might land unevenly:

```
Shard A at 30°
Shard B at 120°   ← 90° gap
Shard C at 210°   ← 90° gap
Shard D at 300°   ← 90° gap, but then 120° gap back to A
```

Unequal gaps = unequal data distribution. To fix this, each physical shard gets **multiple virtual positions** on the ring:

```
Shard A → also appears at 30°, 150°, 270°
Shard B → also appears at 60°, 180°, 300°
Shard C → also appears at 90°, 210°, 330°
Shard D → also appears at 120°, 240°, 360°
```

Now the ring has **12 evenly distributed points** for 4 shards. Data distributes uniformly AND each shard handles roughly equal load.

---

## Now: How Does Your App Know Which Shard to Hit?

This is the routing problem. Your app server receives a request for `short.ly/mK9`. It needs to know: **which DB server has this URL?**

### Option 1: The App Does the Math

The app server itself runs the consistent hashing algorithm:

```
App Server receives: short.ly/mK9
  ↓
hash("mK9") → position 80°
  ↓
Walk ring → Shard B
  ↓
Connect to Shard B directly ✅
```

**Pros:** Fast. No extra network hop.
**Cons:** Every app server needs to know the full shard map. When shards change, you update all app servers.

### Option 2: A Routing Service (Coordinator)

A dedicated **router service** maintains the shard map and directs traffic:

```
App Server → "Where is mK9?" → Router Service
                                     ↓
                               "Shard B, at 10.0.0.3:3306"
                                     ↓
App Server → connects directly to Shard B ✅
```

**Pros:** One place to update when shards change.
**Cons:** Router is now a bottleneck and single point of failure (needs its own redundancy).

### Option 3: A Proxy Layer (What most production systems use)

A smart **database proxy** sits between app servers and DB shards:

```
App Servers                Proxy Layer           DB Shards
     │                         │
     │──── query + key ────────►│
     │                         │ applies routing logic
     │                         │──────────────────────► Shard B
     │                         │◄─────────────────────── result
     │◄──── result ────────────│
```

App servers talk to the proxy as if it's one database. The proxy handles all routing internally. Tools like **ProxySQL, Vitess (used by YouTube), Envoy** do this.

---

## Replication Within Each Shard 🔄

Here's the part people miss — **sharding and replication work together.** Each shard is not just one server. Each shard is itself replicated:

```
┌─────────────────────────────────────────────────────┐
│                      Shard B                        │
│                                                     │
│  ┌─────────────┐    replicates    ┌─────────────┐  │
│  │  Primary B  │ ───────────────► │ Replica B1  │  │
│  │  (writes)   │ ───────────────► │ Replica B2  │  │
│  └─────────────┘                  └─────────────┘  │
└─────────────────────────────────────────────────────┘
```

So your full setup looks like:

```
                    ┌─────────────────────────────────────┐
                    │            Shard A                  │
                    │  Primary A → Replica A1, A2         │
                    └─────────────────────────────────────┘

                    ┌─────────────────────────────────────┐
                    │            Shard B                  │
                    │  Primary B → Replica B1, B2         │
                    └─────────────────────────────────────┘

                    ┌─────────────────────────────────────┐
                    │            Shard C                  │
                    │  Primary C → Replica C1, C2         │
                    └─────────────────────────────────────┘
```

**Sharding** → splits data so each shard handles a fraction of total load
**Replication** → within each shard, ensures high availability and read scaling

Together:

```
Write "mK9":
  Route to Shard B (consistent hashing)
  → Write to Primary B
  → Primary B replicates to B1, B2

Read "mK9":
  Route to Shard B (consistent hashing)
  → Read from Replica B1 or B2 (load balanced)
  → Primary B only handles writes ✅
```

---

## The Full Picture 🗺️

```
User request: short.ly/mK9
        │
        ▼
   Load Balancer
        │
        ▼
   App Server
        │
        ├──► Redis Cache ──HIT──► return URL ⚡
        │         │
        │        MISS
        │         │
        ▼         ▼
     DB Proxy / Router
        │
        │ hash("mK9") → Shard B
        │
        ▼
┌─── Shard B ───────────────┐
│  Primary B  (writes)      │
│  Replica B1 (reads) ◄──── │── query goes here
│  Replica B2 (reads)       │
└───────────────────────────┘
```

---

## Summary: The 3 Sharding Strategies

```
┌──────────────────┬──────────────────┬──────────────────────────┐
│ Strategy         │ Strength         │ Weakness                 │
├──────────────────┼──────────────────┼──────────────────────────┤
│ Range-based      │ Simple           │ Hot shards               │
│ Hash-based       │ Even spread      │ Catastrophic resizing    │
│ Consistent Hash  │ Even + resizable │ Slightly complex         │
└──────────────────┴──────────────────┴──────────────────────────┘
```

**Consistent hashing wins** for URL shorteners because the data grows forever and you'll always need to add shards.

---

Now that we've handled data scale deeply — shall we continue to **Chapter 5: Safety, Expiry, and Analytics?** That's where we discuss:

- 301 vs 302 redirects and why it matters enormously for analytics
- How to detect malicious URLs before redirecting users
- How to build a click analytics pipeline that doesn't slow down redirects

Say **go!** 🚀

---

# ID Generation: The Full Story for Our Setup

Let me first highlight **why this is tricky** with our sharded setup, then land on the exact solution.

---

## The Problem With Naive ID Generation + Sharding 💥

Remember our setup:

```
3 Shards, each with a Primary DB:

Shard A: Primary A (auto-increment)
Shard B: Primary B (auto-increment)
Shard C: Primary C (auto-increment)
```

Each primary has its **own** auto-increment counter. So:

```
Shard A creates URL → ID: 1
Shard B creates URL → ID: 1   ← SAME ID!
Shard C creates URL → ID: 1   ← SAME ID!

Now encode to Base62:
All three → "1" → short.ly/1

Which shard do you go to? 
Consistent hash("1") → picks ONE shard
The other two URLs are LOST 💀
```

**IDs must be globally unique across ALL shards.** Not just unique within one shard.

---

## Option 1: Single Ticket Server (We Discussed This)

One dedicated service hands out IDs:

```
App Server A ──► Ticket Server: "give me an ID" → 5001
App Server B ──► Ticket Server: "give me an ID" → 5002
App Server C ──► Ticket Server: "give me an ID" → 5003
```

Globally unique. ✅

But:
- Every URL creation needs a **network round trip** to the Ticket Server
- Ticket Server is a **single point of failure**
- At 10,000 URLs/second, the Ticket Server itself becomes a bottleneck

> Works at small/medium scale. Breaks at large scale.

---

## Option 2: Multi-Master Ticket Servers (odd/even)

Two ticket servers, one for odd IDs, one for even:

```
Ticket Server 1: 1, 3, 5, 7, 9...
Ticket Server 2: 2, 4, 6, 8, 10...
```

Better. But still:
- Still need a **network hop** for every ID
- Only 2x the throughput
- Adding a 3rd ticket server means reconfiguring both existing ones

> Marginally better. Still not production-grade for massive scale.

---

## The Real Solution: Snowflake IDs ❄️

This is what **Twitter built in 2010** and what most large-scale systems use today. The idea is beautiful:

> **What if each app server could generate its own globally unique ID — without talking to anyone?**

No network hop. No central coordinator. No single point of failure.

### How? Pack multiple pieces of info into one 64-bit number.

A Snowflake ID is a **64-bit integer** split into 3 parts:

```
┌─────────────────────────────────────────────────────────────────┐
│  63 bits total                                                  │
│                                                                 │
│  ┌──────────────────────┬────────────────┬─────────────────┐   │
│  │   41 bits            │   10 bits      │   12 bits       │   │
│  │   Timestamp          │   Machine ID   │   Sequence No.  │   │
│  └──────────────────────┴────────────────┴─────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

Let's understand each part deeply.

---

### Part 1: Timestamp (41 bits) ⏱️

Not the full Unix timestamp. The **milliseconds elapsed since a custom epoch** — your company's launch date.

```
Twitter's epoch: Nov 04, 2010 (when they launched Snowflake)
Our epoch:       Jan 01, 2024 (our launch date)

Current time:    Mar 04, 2026  02:30:00 UTC
Milliseconds since our epoch: 68,212,200,000 ms

Store this in 41 bits:
41 bits → max value = 2^41 = 2,199,023,255,551 ms
                     = ~69 years of timestamps

So our system works until year 2024 + 69 = 2093 ✅
```

**Why this matters:** IDs generated later are always **numerically larger.** IDs are naturally time-sorted. You can look at any ID and know roughly when it was created.

---

### Part 2: Machine ID (10 bits) 🖥️

Identifies **which app server** generated this ID.

```
10 bits → 2^10 = 1024 possible machine IDs

So you can have up to 1024 app servers, each with a unique ID:
App Server 1  → Machine ID: 1
App Server 2  → Machine ID: 2
...
App Server 47 → Machine ID: 47
```

Machine IDs are assigned when a server starts up. You can use **ZooKeeper** or a simple config file to assign and track them.

```
App Server boots up
  ↓
Registers itself with ZooKeeper
  ↓
ZooKeeper assigns: "You are Machine 47"
  ↓
App Server stores Machine ID = 47 in memory
  ↓
Uses 47 in every ID it generates forever
```

This is the key insight: **two different servers will never generate the same ID because their Machine IDs are different.** Even if they generate an ID at the exact same millisecond.

---

### Part 3: Sequence Number (12 bits) 🔢

What if the **same machine** generates multiple IDs within the **same millisecond?**

```
App Server 47 at timestamp 68,212,200,000:
  URL 1 → sequence 0
  URL 2 → sequence 1
  URL 3 → sequence 2
  ...

12 bits → 2^12 = 4096 unique sequences per millisecond

So one machine can generate 4,096 unique IDs per millisecond
= 4,096,000 IDs per second from ONE machine 🚀
```

When the millisecond ticks over, sequence resets to 0.

---

### Putting It Together: Generating an ID

Let's walk through a real example.

```
App Server 47 generates an ID at Mar 04, 2026 02:30:00.500 UTC
This is the 3rd URL created in this millisecond

Timestamp:  68,212,200,500 ms since epoch
Machine ID: 47
Sequence:   2  (0-indexed, 3rd URL)

Now pack into 64 bits:

Timestamp (41 bits): 68212200500
→ binary: 01111111100101001011011110001110100

Machine ID (10 bits): 47
→ binary: 0000101111

Sequence (12 bits): 2
→ binary: 000000000010

Final 64-bit number:
01111111100101001011011110001110100 | 0000101111 | 000000000010

As a decimal: 7,104,116,791,513,907,202

Encode this in Base62:
→ "mK9xQ2b"   ← your short code! ✅
```

---

### Decoding Works the Same Way

When a user visits `short.ly/mK9xQ2b`:

```
"mK9xQ2b"
  ↓
Base62 decode → 7,104,116,791,513,907,202
  ↓
Binary: 01111111100101001011011110001110100 | 0000101111 | 000000000010
  ↓
Extract bits:
  Timestamp:  68,212,200,500 → Mar 04, 2026 02:30:00.500 UTC
  Machine ID: 47             → generated by App Server 47
  Sequence:   2              → 3rd URL that millisecond

But we don't even need to decode it!
We just need to find it in the DB.
The ID itself IS the primary key.
```

---

## How This Plugs Into Our Sharding Setup 🔌

Now the critical part. We have our Snowflake ID. How does it connect to consistent hashing + sharding?

```
User submits: https://youtube.com/watch?v=abc

Step 1: App Server 47 generates Snowflake ID
        → 7,104,116,791,513,907,202

Step 2: Encode to Base62
        → "mK9xQ2b"

Step 3: Consistent hash("mK9xQ2b")
        → lands at 80° on ring
        → Route to Shard B

Step 4: Write to Shard B's Primary:
        INSERT (id=7104116791513907202, short="mK9xQ2b", long="https://...")

Step 5: Shard B Primary replicates to Replica B1, B2

Step 6: Return short.ly/mK9xQ2b to user ✅
```

When user clicks `short.ly/mK9xQ2b`:

```
Step 1: Check Redis Cache
        → HIT → return long URL instantly ⚡ (done!)

        → MISS → continue

Step 2: Consistent hash("mK9xQ2b")
        → same calculation → Shard B

Step 3: Read from Shard B Replica:
        SELECT long_url WHERE short_id = "mK9xQ2b"

Step 4: Return long URL
        Populate Redis Cache
        Redirect user ✅
```

**The same consistent hash function is used for both writing and reading.** You always land on the same shard. That's the elegance.

---

## The Complete ID Generation Flow Visually

```
User submits long URL
        │
        ▼
   App Server 47
        │
        ├─ Reads its own Machine ID (47) from memory
        ├─ Gets current timestamp (ms since epoch)
        ├─ Increments local sequence counter
        │
        ▼
   Generates Snowflake ID
   [timestamp | 47 | sequence]
        │
        ▼
   Base62 encodes it → "mK9xQ2b"
        │
        ▼
   Consistent Hash("mK9xQ2b") → Shard B
        │
        ▼
   Writes to Shard B Primary
        │
        ▼
   Returns short.ly/mK9xQ2b
```

**Zero network hops for ID generation.**
**Zero coordination between servers.**
**Zero chance of collision.**

---

## Collision Proof: Can Two Servers Ever Generate the Same ID?

```
Same timestamp? → Possible (both generate at same millisecond)
Same Machine ID? → IMPOSSIBLE (each server has unique Machine ID)

Since Machine ID differs → the full 64-bit number differs → no collision ✅

Same server, same timestamp? → Sequence number handles it (4096/ms)

Same server, same timestamp, sequence overflow (>4096/ms)?
→ Server waits for next millisecond before generating
→ No collision, tiny pause ✅
```

---

## Summary: What We Use and Why

```
┌─────────────────────────────────────────────────────────────────┐
│                  Our ID Strategy: Snowflake                     │
│                                                                 │
│  ✅ Globally unique (Machine ID ensures no cross-server clash)  │
│  ✅ No coordination needed (no network hops)                    │
│  ✅ No single point of failure                                  │
│  ✅ Time-sortable (newer URLs have bigger IDs)                  │
│  ✅ 4096 IDs/ms per machine = millions/sec at scale             │
│  ✅ Plugs directly into consistent hashing for shard routing    │
└─────────────────────────────────────────────────────────────────┘
```

---

Ready to move to **Chapter 5: Safety, Expiry & Analytics?**

Where we cover:
- **301 vs 302** and why this single decision can destroy your analytics
- **Malicious URL detection** before users get redirected
- **Click tracking pipeline** that doesn't add latency to redirects

