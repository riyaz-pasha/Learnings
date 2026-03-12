Great approach. This **story-driven learning** is exactly how strong **Google L5 HLD answers** are structured. Instead of jumping to fancy architectures, we evolve the system **step-by-step like engineers did in real life**.

We'll design **Gmail** the same way.

And we will go **phase by phase**.

You must **challenge every step**, because interviewers will.

---

# Story Driven HLD: Designing Gmail

We will evolve Gmail in **8 phases**.

1️⃣ Simple Email System
2️⃣ Handling Attachments
3️⃣ Scaling Storage
4️⃣ Email Delivery (Sending mail reliably)
5️⃣ Search (Gmail's killer feature)
6️⃣ Spam Detection
7️⃣ Real-time Updates
8️⃣ Global Scale (Sharding + Replication)

But we will go **very slowly**.

Today we start with **Phase 1**.

---

# Phase 1 — The First Email System Ever Built

Imagine it's **1995**.

People want something like:

> "Send a message to another user on the internet."

Very small scale.

Example:

```
alice@gmail.com → send mail → bob@gmail.com
```

Basic features needed:

• Send email
• Receive email
• Store email
• Read email

Nothing fancy.

---

# Step 1 — Simplest Possible Architecture

We build a **single server email system**.

```
        Users
          |
          v
     API Server
          |
          v
       Database
```

Server handles:

```
POST /sendEmail
GET /emails
```

---

# Data Model

We start simple.

```
Users
------
user_id
email
password


Emails
------
email_id
sender_id
receiver_id
subject
body
timestamp
```

---

# Sending Email Flow

When Alice sends mail to Bob:

```
1 User writes email
2 Client calls API
3 Server stores email
4 Email appears in Bob inbox
```

API:

```
POST /sendEmail
{
  from: alice@gmail.com
  to: bob@gmail.com
  subject: hello
  body: hi bob
}
```

Server:

```
INSERT INTO emails (...)
```

Done.

---

# Reading Emails

Bob opens inbox.

```
GET /emails?user=bob
```

Query:

```
SELECT * 
FROM emails
WHERE receiver_id = bob
ORDER BY timestamp DESC
```

---

# Why This Actually Worked Initially

Early email systems had:

• small user base
• small email sizes
• very low traffic

Example:

```
Users: 10K
Emails/day: 100K
```

One database can easily handle this.

---

# But Problems Start Appearing

As the system grows.

Imagine:

```
Users: 50 Million
Emails/day: 1 Billion
```

Now problems appear.

---

# Problem 1 — Inbox Query Becomes Slow

This query:

```
SELECT * 
FROM emails
WHERE receiver_id = bob
ORDER BY timestamp DESC
```

Database must scan **huge table**.

Emails table might have:

```
10 Billion rows
```

So reading inbox becomes slow.

---

# Problem 2 — Storage Explosion

Email storage grows very fast.

Example:

```
1B emails/day
average email size = 50KB
```

Daily storage:

```
50 TB/day
```

Yearly:

```
18 PB
```

One database **cannot store this**.

---

# Problem 3 — Sending Mail Becomes Slow

If many people send emails simultaneously:

```
1M requests/sec
```

Single server crashes.

---

# Problem 4 — Attachments

Soon people want:

```
photos
documents
videos
```

Attachments are huge.

Email DB will explode.

---

# Problem 5 — Reliability

What happens if server crashes?

You lose emails.

This is unacceptable.

Email systems require **near 0 data loss**.

---

# At This Point Engineers Realize

We need to redesign.

Next idea:

```
Separate email delivery from storage.
```

This is a **big turning point in email systems.**

Instead of:

```
API → DB
```

We introduce **Email Delivery Service**.

But before moving forward, I want to ask you something an interviewer might ask.

---

# Interview Question

If we keep the current design:

```
Emails table with 10B rows
```

How can we **make inbox query fast**?

```
SELECT * FROM emails
WHERE receiver_id = bob
ORDER BY timestamp DESC
```

What would you try first?

Possible directions:

A) Index
B) Separate inbox table
C) Cache
D) Partition table

What would **you do first**?

Explain your thinking like in an interview.

---

Good. 👍
That’s exactly the **first instinct most engineers (and interviewers) expect**.

But in HLD interviews, what matters is **how deep you go after saying “index”**.

Let’s continue the story.

---

# Phase 1.1 — Engineers Add an Index

You suggested:

> **Add an index on `receiver_id`**

So the table becomes:

```
Emails
------
email_id
sender_id
receiver_id
subject
body
timestamp
```

Index:

```
INDEX(receiver_id)
```

Now when Bob opens inbox:

```sql
SELECT *
FROM emails
WHERE receiver_id = bob
ORDER BY timestamp DESC
```

Instead of scanning **10B rows**, database uses the index.

Internally it works like:

```
receiver_id → pointer to rows
```

Example:

```
bob → [email12, email45, email89]
alice → [email10, email22]
```

So lookup becomes **fast**.

Time complexity roughly:

```
O(log N)
```

instead of

```
O(N)
```

Much better.

---

# But Engineers Immediately Notice Another Problem

The query is:

```
WHERE receiver_id = bob
ORDER BY timestamp DESC
```

But our index is only on:

```
receiver_id
```

So database must still **sort results**.

If Bob has **50,000 emails**, sorting becomes expensive.

---

# Engineers Improve the Index

They create a **composite index**.

```
INDEX(receiver_id, timestamp DESC)
```

Now the index internally looks like:

```
bob:
  2026-03-10 → email98
  2026-03-09 → email85
  2026-03-08 → email72
```

So query becomes extremely fast.

```
O(log N + K)
```

Where:

```
K = emails returned
```

This is **perfect for small systems**.

---

# But Gmail Scale Breaks This Too

Let’s see Gmail numbers.

Example:

```
Users: 2 Billion
Emails/day: 300 Billion
```

Total emails stored:

```
Trillions
```

Now problems appear.

---

# Problem 1 — Index Size Explodes

Indexes themselves consume storage.

Example:

```
Email record = 1 KB
Index entry = ~100 bytes
```

If we store:

```
1 trillion emails
```

Index size becomes:

```
100 TB+
```

One DB machine cannot store this.

---

# Problem 2 — Write Performance Dies

Every email insert must update the index.

So every send operation becomes:

```
1 DB write
+ index update
+ disk IO
```

If we have:

```
10M emails/sec
```

Single DB dies instantly.

---

# Problem 3 — Hot Users

Some users receive insane traffic.

Example:

```
support@company.com
newsletter@amazon.com
```

Millions of emails.

Single partition gets overloaded.

---

# Engineers Realize

Indexing helps **query speed**, but it does **nothing for scale**.

The real problem is:

```
All emails are stored in ONE database.
```

That becomes the bottleneck.

---

# The First Big Architectural Change

Engineers introduce **Inbox Table Separation**.

Instead of one big table:

```
Emails
```

They split responsibilities.

---

# New Design

```
Email Content Table
-------------------
email_id
sender_id
subject
body
timestamp


Inbox Table
-----------
user_id
email_id
timestamp
```

Now system works like this.

---

# Sending Email Flow

Alice sends email to Bob.

Step 1 — store email content

```
Email_Content

email_id: 1001
sender: alice
subject: hello
body: hi
```

Step 2 — create inbox entry

```
Inbox

user_id: bob
email_id: 1001
timestamp
```

---

# Why This Is Powerful

Now Bob's inbox query becomes:

```
SELECT email_id
FROM inbox
WHERE user_id = bob
ORDER BY timestamp DESC
```

Inbox table is **MUCH smaller**.

It contains only:

```
user_id
email_id
timestamp
```

No large body fields.

---

# Then we fetch actual emails

Step 2 query:

```
SELECT *
FROM email_content
WHERE email_id IN (...)
```

---

# Benefits

### 1️⃣ Inbox queries are extremely fast

Small table.

Small index.

---

### 2️⃣ Email body stored separately

Large data separated.

---

### 3️⃣ Enables sharding later

We can shard by:

```
user_id
```

Which is **perfect distribution**.

---

# But Another Problem Appears

What if Alice sends email to **1M users**?

Example:

```
Newsletter
```

We must write:

```
1M inbox rows
```

This becomes extremely expensive.

---

# This leads to the **next major concept**.

A system called:

```
Fanout
```

Two strategies appear:

```
Fanout on Write
Fanout on Read
```

And this is a **very famous system design interview topic**.

---

# Before we continue

Let me ask you the **next interview question**.

If Alice sends an email to:

```
1,000,000 users
```

Should we:

### A

Create **1M inbox rows immediately** (fanout on write)

### B

Store email once and generate inbox entries **when users open inbox** (fanout on read)

Which would you choose and **why?**

This is a **very important distributed systems tradeoff**.

---

Great answer. 👍
And this is exactly where **system design interviews become interesting** — because **both approaches are used in real systems**.

Let’s continue the **engineering story**.

---

# Phase 1.2 — Engineers Try Fanout on Read

You proposed:

> **Store the email once and generate inbox entries when users open inbox.**

This approach is called:

```
Fanout on Read
```

Instead of writing 1M rows when sending the email, we do **lazy computation**.

---

# How Fanout on Read Works

Alice sends a newsletter to **1M users**.

Instead of creating 1M inbox rows:

We store just one record.

```
Emails
------
email_id
sender_id
recipient_group
subject
body
timestamp
```

Example:

```
email_id: 5001
sender: newsletter@amazon.com
recipient_group: group_1M_users
```

Now when **Bob opens inbox**, system calculates:

```
All emails sent to:
1) Bob directly
2) Groups Bob belongs to
```

---

# Inbox Query Now Becomes

Instead of:

```sql
SELECT * FROM inbox WHERE user_id = bob
```

We run something like:

```sql
SELECT * 
FROM emails
WHERE recipient_group IN (groups_of_bob)
ORDER BY timestamp DESC
```

This avoids creating **1M writes**.

So sending email becomes extremely cheap.

```
Send cost = O(1)
```

---

# Engineers Initially Loved This

Because large sends like:

```
Newsletters
Mailing lists
Marketing emails
```

became **very cheap**.

---

# But Soon a New Problem Appears

When Bob opens Gmail, he expects:

```
Inbox loads instantly
```

But with fanout on read, the system must now:

```
1) find all groups Bob belongs to
2) fetch emails sent to those groups
3) merge results
4) sort by timestamp
```

This becomes expensive.

---

# Imagine Gmail Scale

Example numbers:

```
Users: 2 Billion
Groups: millions
Emails/day: 300 Billion
```

If each inbox load requires scanning large tables, latency explodes.

Bob clicks inbox.

Query may touch:

```
hundreds of millions of rows
```

Now inbox loads in:

```
5–10 seconds
```

Unacceptable.

---

# Another Problem — Sorting

User inbox must be ordered by:

```
latest email first
```

If emails come from many groups, system must:

```
merge sorted streams
```

This becomes very expensive at scale.

---

# Another Hidden Problem — Unread Counts

Gmail shows:

```
Inbox (3)
```

Unread emails.

With fanout on read, computing unread count requires:

```
dynamic computation
```

For billions of users this becomes very costly.

---

# Engineers Realize Something Important

Reads happen **far more often than writes**.

Example Gmail numbers:

```
Emails sent/day: 300B
Inbox opens/day: 10T+
```

Users refresh inbox constantly.

So optimizing reads is more important.

---

# Engineers Switch Strategy

Instead of **Fanout on Read**

They move to:

```
Fanout on Write
```

Meaning:

When Alice sends email to 1M users:

We create **1M inbox entries immediately**.

```
Send cost = O(N)
```

But reading inbox becomes extremely fast.

```
Read cost = O(1)
```

---

# Why Big Systems Prefer Fanout on Write

Because user experience depends on:

```
fast reads
```

Inbox must load in:

```
<100 ms
```

So systems like:

```
Gmail
Twitter timeline
Facebook feed
```

mostly prefer **fanout on write**.

---

# But Now We Have Another Problem

If we choose **fanout on write**, sending a large email becomes expensive.

Example:

```
Alice sends email to 1M users
```

We must write:

```
1M inbox rows
```

If each write takes even **1 ms**, this would take:

```
1000 seconds
```

Clearly impossible.

---

# Engineers Solve This Using Queues

Instead of writing 1M rows synchronously.

We introduce **message queues**.

New architecture:

```
Client
  |
API Server
  |
Email Service
  |
Message Queue (Kafka / PubSub)
  |
Fanout Workers
  |
Inbox DB
```

---

# Sending Email Now Works Like This

Step 1 — user sends email

```
POST /sendEmail
```

Step 2 — store email content

```
Email_Content_DB
```

Step 3 — push job to queue

```
FanoutJob(email_id, recipients)
```

Step 4 — workers process asynchronously

Workers create inbox entries:

```
Inbox
------
user_id
email_id
timestamp
```

---

# Why Queues Are Critical

Queues give us:

### 1️⃣ Massive Parallelism

1000 workers can process fanout simultaneously.

---

### 2️⃣ Reliability

If worker crashes:

```
job stays in queue
```

No email lost.

---

### 3️⃣ Traffic Smoothing

If large email arrives:

Queue buffers workload.

System stays stable.

---

# At This Point Our System Looks Like

```
                +--------------+
User  ------->  | API Servers  |
                +--------------+
                        |
                        v
                +--------------+
                | Email Store  |
                +--------------+
                        |
                        v
                 +------------+
                 |  Queue     |
                 |  (Kafka)   |
                 +------------+
                        |
                        v
                +---------------+
                | FanoutWorkers |
                +---------------+
                        |
                        v
                  Inbox DB
```

Now we can scale fanout horizontally.

---

# But the Next Massive Problem Appears

Storage.

Gmail offers users:

```
15 GB free storage
```

If there are:

```
2 Billion users
```

Potential storage becomes:

```
30 Exabytes
```

This is **insanely large**.

Traditional databases cannot handle this.

This is where Gmail introduced a **very interesting storage architecture**.

And this leads to the **next major HLD concept**:

```
Distributed Storage + Sharding
```

---

# Next Interview Question

Suppose we have:

```
2 Billion users
```

Each user has their own inbox.

How should we **shard the Inbox database**?

Possible strategies:

A) Shard by `email_id`
B) Shard by `timestamp`
C) Shard by `user_id`

Which one would you choose and why?

This decision **determines the entire Gmail architecture.**

---

Excellent. ✅
This is the **correct answer interviewers expect**.

But again, the **important part is explaining *why***. Let’s go deeper.

---

# Phase 2 — Sharding the Inbox Database

You chose:

```text
Shard by user_id
```

Let’s understand **why engineers at Gmail also reached the same conclusion**.

---

# The Problem Before Sharding

Our inbox table currently looks like this:

```
Inbox
-----
user_id
email_id
timestamp
```

If Gmail has:

```
2 Billion users
```

and each user has ~50k emails on average:

```
2B × 50K = 100 Trillion inbox rows
```

A single database cannot handle this.

So we **split the data across many machines**.

This is called:

```
Sharding
```

---

# Option A — Shard by `email_id`

Example:

```
Shard1 → email_id 1–1B
Shard2 → email_id 1B–2B
Shard3 → email_id 2B–3B
```

Problem appears immediately.

When Bob opens inbox:

```
GET /inbox?user=bob
```

Bob's emails could be on **every shard**.

So we must query:

```
Shard1
Shard2
Shard3
Shard4
...
```

Then merge results.

Latency becomes huge.

So this option is **terrible for inbox queries**.

---

# Option B — Shard by `timestamp`

Example:

```
Shard1 → 2026 Jan
Shard2 → 2026 Feb
Shard3 → 2026 Mar
```

Again, terrible.

Bob's inbox spans **many months or years**.

To show inbox:

```
query multiple shards
merge results
sort by timestamp
```

Very slow.

---

# Option C — Shard by `user_id` (Correct)

Example:

```
Shard1 → users 1–50M
Shard2 → users 50M–100M
Shard3 → users 100M–150M
```

Now Bob’s inbox lives **entirely in one shard**.

So query becomes:

```
GET /inbox?user=bob
```

System does:

```
hash(user_id) → shard
```

Query only **one database**.

Latency stays low.

---

# Gmail Likely Uses Consistent Hashing

Example hashing:

```
shard = hash(user_id) % N
```

Example:

```
hash(bob) % 10 → shard 7
```

So Bob always lives in **Shard 7**.

---

# Why This Is Perfect for Gmail

Because Gmail workload is:

```
read-heavy
```

Typical pattern:

```
User → opens their own inbox
```

Almost all reads are **per user**.

So sharding by `user_id` gives:

```
O(1) shard lookup
```

Very efficient.

---

# But Now Another Problem Appears

What happens if one shard fails?

Example:

```
Shard 7 crashes
```

All users mapped to that shard lose access to:

```
inbox
email history
```

This is unacceptable.

Email systems must be **extremely reliable**.

So engineers introduce the next concept:

```
Replication
```

---

# Phase 3 — Replication

Each shard is not just one server.

It becomes a **replica set**.

Example:

```
Shard 7

Primary DB
   |
   +------ Replica 1
   |
   +------ Replica 2
```

---

# How Writes Work

Fanout worker writes inbox entry.

```
write → Primary
```

Primary then replicates to replicas.

```
Primary → Replica1
Primary → Replica2
```

---

# How Reads Work

Inbox queries can go to replicas.

```
User → read from replica
```

This gives:

```
load balancing
```

Example:

```
Replica1 → serves 40% reads
Replica2 → serves 40% reads
Primary → handles writes
```

---

# Benefits

### 1️⃣ High availability

If primary dies:

```
Replica promoted to primary
```

---

### 2️⃣ Read scaling

Millions of users can read simultaneously.

---

### 3️⃣ Durability

Multiple copies of data.

---

# But Replication Introduces a New Problem

Replication is usually **asynchronous**.

So scenario:

```
Alice sends email to Bob
```

Write goes to:

```
Primary
```

But replica hasn't replicated yet.

Bob refreshes inbox and reads from replica.

Result:

```
Email not visible
```

This is called:

```
Replication Lag
```

---

# Gmail Likely Uses Smart Read Routing

For recent writes:

```
read from primary
```

For older emails:

```
read from replicas
```

This avoids consistency issues.

---

# Current Architecture So Far

```
Users
   |
API Servers
   |
Email Content Store
   |
Queue (Kafka)
   |
Fanout Workers
   |
Inbox DB (Sharded by user_id)
   |
Replication
```

This system can now handle:

```
billions of users
```

---

# But Gmail Still Has a Huge Problem

Users expect this to work instantly:

```
search email
```

Example:

```
"invoice from amazon"
```

Search across **thousands of emails**.

A database cannot handle this efficiently.

This leads to the **next major Gmail innovation**:

```
Distributed Search Index
```

Which is why Gmail search feels **almost magical**.

---

# Next Interview Question (Very Important)

Suppose a user has:

```
100,000 emails
```

When they search:

```
"amazon invoice"
```

Which approach would you use?

A) SQL `LIKE '%amazon%'` queries
B) Full Text Search Index (like Elasticsearch)
C) Scan emails in application server

Which one would you choose and why?

---
Excellent. ✅
This is exactly what strong candidates answer.

But in interviews the **important part is explaining why the other options fail** and **how the search system is built**.

Let’s continue the **engineering story of Gmail**.

---

# Phase 4 — Gmail Search (One of Gmail’s Biggest Innovations)

Before Gmail existed, email systems like:

* Yahoo Mail
* Hotmail

relied heavily on **folders** because search was slow.

Users had to manually organize emails like:

```
Inbox
Work
Family
Bills
Receipts
```

Then Gmail came and said:

> “Don’t organize. Just **search**.”

That meant the system must search through **massive email data instantly**.

---

# Why SQL Search Fails

Suppose Bob has:

```
100,000 emails
```

If we run:

```sql
SELECT *
FROM emails
WHERE body LIKE '%amazon%'
```

The database must **scan every email body**.

Complexity:

```
O(N)
```

Now imagine Gmail scale:

```
2 billion users
avg 50k emails per user
```

Total emails:

```
~100 trillion
```

Scanning this is impossible.

Even indexing doesn't help because SQL indexes **don't work well for substring text search**.

---

# The Solution — Full Text Search Engine

So Gmail introduced **search indexing systems**.

Modern systems use things like:

* Elasticsearch
* Apache Solr
* Apache Lucene

These are built specifically for **text search**.

---

# Key Idea — Inverted Index

Instead of storing emails like:

```
email_id → email text
```

Search engines store:

```
word → list of emails containing the word
```

Example email:

```
Email 1:
"amazon invoice payment"
```

The index becomes:

```
amazon  → [email1]
invoice → [email1]
payment → [email1]
```

Another email:

```
Email 2:
"amazon delivery update"
```

Index becomes:

```
amazon   → [email1, email2]
invoice  → [email1]
delivery → [email2]
update   → [email2]
```

This structure is called an:

```
Inverted Index
```

---

# Now Search Becomes Extremely Fast

If user searches:

```
amazon invoice
```

The system does:

```
amazon → [email1, email2]
invoice → [email1]
```

Intersect:

```
[email1]
```

Time complexity becomes:

```
O(words)
```

Instead of scanning emails.

---

# Gmail Search Architecture

When a new email arrives:

```
Alice → Bob
```

The system performs **two writes**.

### 1️⃣ Store email

```
Email Storage
```

### 2️⃣ Update search index

```
Search Index Cluster
```

Architecture now becomes:

```
Users
   |
API Servers
   |
Email Storage
   |
Queue
   |
Fanout Workers
   |
Inbox DB
   |
Search Index Cluster
```

---

# Why Search Is Built Asynchronously

Indexing text is expensive.

It requires:

```
tokenization
stemming
index building
```

So Gmail does:

```
Email arrives
     ↓
store email
     ↓
push indexing job to queue
     ↓
search workers update index
```

This ensures **email delivery is not delayed by indexing**.

---

# Example Flow

Alice sends email to Bob.

```
Step 1
Store email in Email DB
```

```
Step 2
Fanout workers update Bob inbox
```

```
Step 3
Search worker indexes email
```

Bob can now search:

```
amazon invoice
```

and find the email instantly.

---

# But Search Index Also Needs Sharding

Remember Gmail scale:

```
100 trillion emails
```

One search node cannot handle this.

So search clusters also shard.

Example:

```
Shard 1 → email_id 0-1T
Shard 2 → email_id 1T-2T
Shard 3 → email_id 2T-3T
```

Each shard maintains **its own inverted index**.

---

# Query Flow for Search

Bob searches:

```
amazon invoice
```

System does:

```
Search API
   |
Query sent to all shards
   |
Each shard returns matches
   |
Aggregator merges results
   |
Top results returned
```

This is called:

```
Scatter-Gather Search
```

Used in systems like Elasticsearch.

---

# But Now Gmail Faces Another Huge Problem

Spam.

Huge amount of emails are **malicious or unwanted**.

Example:

```
phishing
lottery scams
ads
malware
```

In early email systems:

```
80–90% emails = spam
```

Without spam filtering Gmail becomes unusable.

So engineers built **large-scale spam detection systems**.

This involves:

```
machine learning
reputation systems
content analysis
behavior analysis
```

---

# Current Gmail Architecture So Far

```
Users
   |
Load Balancer
   |
API Servers
   |
Email Storage Cluster
   |
Queue (Kafka)
   |
Fanout Workers
   |
Inbox DB (sharded by user_id)
   |
Search Index Cluster
```

System now supports:

```
billions of users
trillions of emails
fast inbox
fast search
```

---

# Next Interview Question (Very Important)

When Bob opens Gmail he sees **new emails instantly appear** without refreshing.

Example:

```
Alice sends email
Bob sees it immediately
```

How would you implement this?

Possible options:

A) Client polls server every 5 seconds
B) Long polling
C) WebSockets
D) Server Sent Events

Which would you choose **for Gmail and why?**

This question introduces **real-time system design**, which is another **common Google HLD discussion topic**.

---
Good choice. 👍
**Server-Sent Events (SSE)** is a strong answer, and many candidates mention it. But in a **Google L5 HLD interview**, the key is explaining **why and when to use SSE vs other options**.

Let’s continue the **engineering story of Gmail**.

---

# Phase 5 — Real-Time Email Updates

Early email systems worked like this:

```text
User opens inbox
↓
Client refreshes every few seconds
↓
Check for new emails
```

Example:

```text
GET /inbox
GET /inbox
GET /inbox
GET /inbox
```

This is called **polling**.

---

# Problem With Polling

Imagine Gmail scale.

```text
Users online: 500 million
Polling interval: 5 seconds
```

Requests per second:

```text
500M / 5 = 100M requests/sec
```

But **most requests return nothing**.

```text
"no new email"
```

So servers are wasting massive resources.

Engineers needed something better.

---

# Solution — Push Notifications

Instead of clients asking:

```text
"Do I have new email?"
```

The server **pushes the update**.

```text
Server → Client
"New email arrived"
```

Now the client updates the inbox immediately.

---

# Real-Time Communication Options

Engineers considered several approaches.

---

## Option A — Polling

Client repeatedly asks server.

```text
Client → Server → "any new email?"
```

Problems:

* huge request volume
* wasted compute
* bad latency

Rejected.

---

## Option B — Long Polling

Client sends request:

```text
GET /inbox-updates
```

Server **keeps connection open** until an update occurs.

Example:

```text
Client → Server
(waiting...)

Email arrives

Server → Client
"new email"
```

Then client reconnects.

This reduces polling traffic but still has problems:

* constant reconnects
* high overhead
* inefficient at massive scale

---

## Option C — WebSockets

WebSockets create a **persistent bidirectional connection**.

```text
Client ↔ Server
```

Both sides can send messages anytime.

Used in systems like:

* WhatsApp
* Slack
* Discord

But Gmail communication is mostly **one-way**:

```text
Server → Client
```

Client rarely sends messages.

So WebSockets may be **overkill**.

---

## Option D — Server-Sent Events (SSE)

This is what you chose.

SSE creates a **persistent HTTP stream** where the server sends updates.

```text
Client → open connection
Server → keeps streaming events
```

Example flow:

```text
Client opens Gmail
↓
Client opens SSE connection
↓
Server sends events when emails arrive
```

Example event:

```text
event: new_email
data: {
  "email_id": 91823,
  "sender": "alice@gmail.com",
  "subject": "meeting"
}
```

Client updates inbox instantly.

---

# Why SSE Is Good for Gmail

### 1️⃣ One-way communication

Perfect for:

```text
Server → Client updates
```

---

### 2️⃣ Uses HTTP

Works easily with:

```text
load balancers
proxies
CDNs
```

---

### 3️⃣ Lightweight

Lower overhead than WebSockets.

---

# Gmail Real-Time Architecture

When Alice sends email:

```text
Alice → API Server
        ↓
Email stored
        ↓
Fanout workers update Bob inbox
        ↓
Notification service triggers event
        ↓
SSE pushes update to Bob
```

Architecture now includes **Notification Service**.

```text
Users
   |
Load Balancer
   |
API Servers
   |
Email Storage
   |
Queue
   |
Fanout Workers
   |
Inbox DB
   |
Notification Service
   |
SSE Connections → Clients
```

---

# But This Introduces Another Huge Challenge

Millions of users maintain **persistent connections**.

Example:

```text
Active users: 500 million
Each user = 1 open connection
```

Servers must maintain:

```text
500 million connections
```

Traditional servers cannot handle this.

So companies build **connection servers** optimized for persistent connections.

Large companies use technologies similar to:

* Netty
* Envoy
* Nginx

These servers can handle **millions of connections per machine**.

---

# Now Our Gmail System Looks Like This

```text
                     +----------------+
Users -------------->| Load Balancer |
                     +----------------+
                             |
                             v
                       +-----------+
                       | API Tier  |
                       +-----------+
                             |
                             v
                     +------------------+
                     | Email Storage DB |
                     +------------------+
                             |
                             v
                         +-------+
                         |Queue  |
                         |Kafka  |
                         +-------+
                             |
                             v
                     +----------------+
                     | Fanout Workers |
                     +----------------+
                             |
                             v
                       +-----------+
                       | Inbox DB  |
                       | (Sharded) |
                       +-----------+
                             |
                             v
                   +----------------------+
                   | Search Index Cluster |
                   +----------------------+
                             |
                             v
                    +--------------------+
                    | Notification Tier  |
                    +--------------------+
                             |
                             v
                    SSE → Connected Users
```

This system now supports:

* billions of users
* trillions of emails
* instant inbox updates
* fast search

---

# But Gmail Still Has One of the Hardest Problems

Attachments.

People send:

```text
photos
videos
documents
PDFs
```

Attachments can be **very large**.

Example:

```text
Email attachment = 25 MB
```

If attachments are stored inside the email database:

```text
DB explodes
```

So Gmail had to design a **massive distributed file storage system**.

This leads to another critical HLD concept:

```text
Blob Storage / Object Storage
```

Used by systems like:

* Google Cloud Storage
* Amazon S3

---

# Next Interview Question

When Alice sends an email with a **25MB attachment**, what should we do?

A) Store attachment directly inside the email database
B) Store attachment in distributed object storage and store only the reference in the email
C) Store attachment on the application server disk

Which approach would you choose and **why?**

This decision leads to **large-scale storage architecture**, which is another **major Gmail design component**.

---

Excellent. ✅
This is exactly how **large-scale systems like Gmail are designed**.

Your answer:

```text
Store attachment in distributed object storage and store only the reference in the email
```

is the **industry standard architecture** used by systems like Google Cloud Storage and Amazon S3.

Now let’s continue the **engineering story**.

---

# Phase 6 — The Attachment Problem

Early email systems stored attachments **inside the database**.

Example email table:

```
Emails
------
email_id
sender
receiver
subject
body
attachment_blob
```

If Alice sends a **25 MB attachment**, the DB stores:

```
25 MB inside the row
```

This quickly becomes catastrophic.

---

# Why Storing Attachments in DB Is Bad

### 1️⃣ Database Performance Dies

Databases are optimized for:

```
structured data
indexes
small rows
```

Not huge blobs.

Large rows cause:

```
slow queries
large IO
cache inefficiency
```

---

### 2️⃣ Storage Explosion

Imagine Gmail numbers:

```
emails/day: 300 billion
avg attachment: 10 MB
```

Daily storage:

```
3 exabytes per day
```

No database cluster can handle this.

---

### 3️⃣ Replication Becomes Slow

If attachments are inside DB:

```
Primary → Replica replication
```

Huge network traffic.

Replication lag becomes massive.

---

# Engineers Introduce Blob Storage

Instead of storing attachments in DB, Gmail stores them in **distributed object storage**.

Modern examples:

* Google Cloud Storage
* Amazon S3

These systems are optimized for storing **huge files at massive scale**.

---

# New Email Data Model

Email metadata stays in DB.

```
Emails
------
email_id
sender
receiver
subject
body
attachment_url
timestamp
```

Attachment stored separately.

```
Object Storage
--------------
object_id
file_data
replication_metadata
```

Example:

```
attachment_url =
storage.gmail.com/objects/abcd123
```

---

# Sending Email With Attachment

Step-by-step flow.

### Step 1 — Upload attachment

Client uploads file.

```
Client → Attachment Service → Object Storage
```

File stored.

Example:

```
object_id = obj_918273
```

---

### Step 2 — Send email

User sends email.

```
POST /sendEmail
```

Email record stores:

```
attachment_url = obj_918273
```

---

### Step 3 — Recipient reads email

When Bob opens email:

```
Email Service → fetch metadata
```

Attachment downloaded from object storage.

```
Client → Object Storage
```

This keeps **email database small and fast**.

---

# Gmail Attachment Architecture

```
Users
   |
Load Balancer
   |
API Servers
   |
Email Metadata DB
   |
Queue
   |
Fanout Workers
   |
Inbox DB
   |
Search Index
   |
Attachment Service
   |
Object Storage Cluster
```

---

# Why Object Storage Scales So Well

Object storage systems are designed for:

```
petabytes
exabytes
```

They use:

### Massive Sharding

Files distributed across thousands of machines.

```
hash(object_id) → storage node
```

---

### Replication

Example:

```
File stored on:
Node A
Node B
Node C
```

If one machine fails, data survives.

---

### Chunking

Large files are split.

Example:

```
25 MB file
→ split into chunks
→ stored across servers
```

This improves:

```
parallel reads
fault tolerance
```

---

# Optimization — CDN for Attachments

Attachments are downloaded frequently.

So Gmail likely uses CDNs such as:

* Cloudflare
* Akamai

Flow becomes:

```
Client → CDN → Object Storage
```

Benefits:

```
lower latency
reduced backend load
```

---

# Another Important Optimization — Deduplication

Suppose 1 million people receive the same attachment.

Example:

```
newsletter.pdf
```

Without optimization:

```
store file 1M times
```

Wasteful.

Instead Gmail likely uses **content hashing**.

Example:

```
hash(file) → SHA256
```

If the same hash already exists:

```
reuse existing object
```

So storage becomes:

```
1 file stored
1M references
```

Huge savings.

---

# Gmail System So Far

We have now built almost the **complete Gmail architecture**.

```
                     +----------------+
Users -------------->| Load Balancer |
                     +----------------+
                             |
                             v
                       +-----------+
                       | API Tier  |
                       +-----------+
                             |
                             v
                     +------------------+
                     | Email Metadata DB|
                     +------------------+
                             |
                             v
                         +-------+
                         |Queue  |
                         |Kafka  |
                         +-------+
                             |
                             v
                     +----------------+
                     | Fanout Workers |
                     +----------------+
                             |
                             v
                       +-----------+
                       | Inbox DB  |
                       | (Sharded) |
                       +-----------+
                             |
                             v
                   +----------------------+
                   | Search Index Cluster |
                   +----------------------+
                             |
                             v
                    +--------------------+
                    | Notification Tier  |
                    +--------------------+
                             |
                             v
                       SSE → Clients
                             |
                             v
                    +--------------------+
                    | Object Storage     |
                    | (attachments)      |
                    +--------------------+
```

This system supports:

```
billions of users
trillions of emails
petabytes of attachments
real-time updates
fast search
```

---

# But One Final Massive Problem Remains

Spam.

Rough statistics for email systems:

```
~85% of all emails sent globally = spam
```

Without spam filtering Gmail would be **unusable**.

So Gmail built one of the **largest ML spam detection systems ever created**.

This introduces concepts like:

```
sender reputation
IP reputation
content classification
user feedback loops
```

---

# Next Interview Question (Important)

When a new email arrives, **when should spam filtering happen?**

A) Before storing the email
B) After storing but before delivering to inbox
C) After delivering to inbox
D) Periodic background scanning

Which would you choose and why?

This question introduces **email processing pipelines**, another important system design concept.

---

Good thinking — but in **real email systems this would not be the primary approach**.
Your answer (**D – periodic background scanning**) can exist, but **not as the main spam filter**.

In a **Google L5 HLD interview**, the stronger answer is usually:

```
B) After storing but before delivering to inbox
```

Let’s walk through the **engineering reasoning step by step** (the story style we’re following).

---

# Phase 7 — The Spam Problem

Global email statistics are scary.

Typical numbers:

```
~85% of all emails sent globally = spam
```

Examples:

```
phishing
fake bank alerts
malware attachments
lottery scams
ads
```

If Gmail simply delivered every email directly to inbox:

```
User inbox = unusable
```

So Gmail must detect spam **before users see it**.

---

# Option A — Spam Filter Before Storing Email

Flow would be:

```
Incoming email
→ spam filter
→ if valid → store
```

Problem:

Spam detection models are **not perfect**.

Sometimes they produce **false positives**.

Example:

```
important email marked as spam
```

If we drop emails before storing them:

```
data loss
```

That is unacceptable.

Email systems must **never lose data**.

So Option A is risky.

---

# Option C — Spam Filter After Delivering to Inbox

Flow:

```
email arrives
→ delivered to inbox
→ spam detection later
```

Problem:

User already saw the spam.

Imagine phishing email:

```
"Your bank account is locked"
```

User clicks the link immediately.

Too late.

So this approach is **unsafe**.

---

# Option D — Periodic Background Scanning

This means:

```
store emails
run spam detection later
```

This can help detect **new spam patterns**, but it cannot be the primary filter because:

```
spam would reach inbox first
```

So this is usually used as **secondary analysis**, not the main filter.

---

# Best Approach — Option B

```
Store email
↓
Spam detection
↓
Deliver to inbox OR spam folder
```

This is what large systems like Gmail do.

---

# Gmail Email Processing Pipeline

When Alice sends an email:

```
Alice → Gmail
```

The email passes through multiple stages.

```
Incoming Mail Server
       ↓
Email Stored
       ↓
Spam Detection Service
       ↓
Fanout Workers
       ↓
Inbox OR Spam Folder
```

---

# Architecture

```
Incoming Mail
      |
      v
+------------------+
| Email Storage DB |
+------------------+
        |
        v
+------------------+
| Spam Detection   |
| Service (ML)     |
+------------------+
        |
   +----+----+
   |         |
   v         v
Inbox DB   Spam DB
```

This ensures:

```
no data loss
spam filtered
safe user experience
```

---

# How Spam Detection Actually Works

Spam detection is **multi-layered**.

Large systems like Gmail analyze many signals.

---

## 1️⃣ Sender Reputation

Email servers maintain **reputation scores**.

Example:

```
IP: 192.168.1.20
```

If this IP sends:

```
10M spam emails
```

It gets blacklisted.

Future emails are blocked quickly.

---

## 2️⃣ Domain Reputation

Example sender:

```
offers@cheap-lottery-win.biz
```

Domain flagged as suspicious.

---

## 3️⃣ Content Analysis

ML models analyze email content.

Example spam patterns:

```
"Congratulations you won"
"Claim prize now"
"Urgent bank verification"
```

Modern systems use **NLP models** to detect patterns.

---

## 4️⃣ Attachment Scanning

Attachments are checked for malware.

Example:

```
.exe files
macro malware in docs
```

---

## 5️⃣ User Feedback Loop

When users click:

```
"Mark as Spam"
```

Gmail updates spam models.

Over time:

```
spam detection improves
```

---

# Spam Detection Infrastructure

Spam detection runs on **huge ML pipelines**.

Architecture might look like:

```
Incoming Mail
      |
      v
+------------------+
| Feature Extractor|
+------------------+
      |
      v
+------------------+
| ML Spam Model    |
+------------------+
      |
      v
Spam Score
```

If:

```
score > threshold
```

Email goes to **Spam folder**.

---

# Gmail System So Far

We now have almost the **complete architecture**.

```
Users
   |
Load Balancer
   |
API Servers
   |
Email Storage
   |
Spam Detection
   |
Queue (Kafka)
   |
Fanout Workers
   |
Inbox DB (sharded by user_id)
   |
Search Index
   |
Notification Service
   |
SSE Connections
   |
Object Storage (attachments)
```

This system supports:

```
billions of users
trillions of emails
real-time inbox
fast search
spam protection
```

---

# But There Is One Final Huge Challenge

Disaster recovery.

Imagine something catastrophic:

```
datacenter outage
hardware failure
network partition
```

Gmail must **never lose emails**.

So they implement:

```
multi-region replication
geo-redundancy
backup systems
```

---

# Next Interview Question (Very Important)

Suppose an entire **datacenter goes down**.

How should Gmail ensure emails are not lost?

Possible approaches:

A) Replicate data to another datacenter
B) Periodic backups only
C) Store emails only in one region but rebuild later

Which one would you choose and why?

This question introduces **geo-replication and disaster recovery**, which is often the **final stage of large system designs**.

---
Perfect — let’s continue the story.
We are now at the **final major stage of Gmail design: Disaster Recovery & Multi-Region Architecture**. This is usually where **senior system design interviews end**, because it shows you understand **planet-scale reliability**.

---

# Phase 8 — The Datacenter Disaster Problem

Imagine Gmail only runs in **one datacenter**.

```text
Users
  |
  v
+-------------+
| Datacenter  |
|   (US)      |
+-------------+
```

Inside that datacenter:

```
Email Storage
Inbox DB
Search Cluster
Attachment Storage
Notification Servers
```

Now imagine something catastrophic happens.

Examples:

```
power outage
network failure
fire
earthquake
software bug
```

Entire datacenter goes down.

Result:

```
billions of users cannot access email
```

Even worse:

```
emails could be lost
```

For an email system this is **unacceptable**.

So engineers introduce the next major concept:

```
Multi-Region Replication
```

---

# Multi-Region Gmail Architecture

Instead of one datacenter, Gmail runs in **multiple geographic regions**.

Example:

```
US-East
US-West
Europe
Asia
```

Architecture:

```
                +------------------+
Users --------->| Global Load Bal. |
                +------------------+
                        |
        +---------------+---------------+
        |               |               |
        v               v               v
   +---------+     +---------+     +---------+
   | Region  |     | Region  |     | Region  |
   | US-East |     | Europe  |     | Asia    |
   +---------+     +---------+     +---------+
```

Each region contains a **complete Gmail stack**.

---

# Each Region Contains

```
API Servers
Email Storage
Inbox DB
Search Index
Spam Detection
Notification Servers
Attachment Storage
```

Meaning:

```
each region can run Gmail independently
```

---

# Data Replication Between Regions

Whenever an email is stored:

```
write → primary region
replicate → other regions
```

Example:

Alice sends email to Bob.

```
Email stored in US-East
```

Replication happens:

```
US-East → Europe
US-East → Asia
```

So now the email exists in **multiple locations worldwide**.

---

# Replication Model

Two common approaches exist.

## 1️⃣ Asynchronous Replication

```
write → primary region
replication happens later
```

Pros:

```
fast writes
low latency
```

Cons:

```
small window of possible data loss
```

---

## 2️⃣ Synchronous Replication

```
write → multiple regions simultaneously
```

Pros:

```
zero data loss
```

Cons:

```
high latency
```

---

# Gmail Likely Uses Hybrid Replication

Typical approach:

```
synchronous replication within region
asynchronous replication across regions
```

Example:

```
US-East cluster nodes → synchronous
US-East → Europe → async
US-East → Asia → async
```

This gives a good balance:

```
durability
low latency
global resilience
```

---

# What Happens If a Datacenter Fails?

Suppose:

```
US-East datacenter crashes
```

Global load balancer detects failure.

Traffic reroutes automatically.

```
Users → Europe region
Users → Asia region
```

Since data was replicated, emails are still available.

Downtime becomes:

```
seconds
```

instead of hours.

---

# Global Load Balancing

Large companies use systems like:

* Google Cloud Load Balancing
* AWS Route 53

These systems route users to the **nearest healthy region**.

Example:

```
User in India → Asia datacenter
User in Germany → Europe datacenter
User in US → US datacenter
```

Benefits:

```
lower latency
higher availability
```

---

# Another Critical Feature — Backups

Replication protects against **hardware failure**, but not against:

```
software bugs
data corruption
accidental deletion
```

Example:

```
bug deletes 1M emails
```

Replication would replicate the bug.

So Gmail also maintains **backups**.

Typical strategy:

```
daily snapshots
incremental backups
long-term archive
```

This allows restoring old data.

---

# Final Gmail Architecture (Complete)

Now we can combine everything we built in the story.

```
                    +----------------------+
Users ------------->| Global Load Balancer |
                    +----------------------+
                             |
           +-----------------+------------------+
           |                                    |
           v                                    v
   +------------------+                +------------------+
   | Region US-East   |                | Region Europe    |
   +------------------+                +------------------+
   | API Servers      |                | API Servers      |
   | Email Storage    |                | Email Storage    |
   | Spam Detection   |                | Spam Detection   |
   | Queue (Kafka)    |                | Queue (Kafka)    |
   | Fanout Workers   |                | Fanout Workers   |
   | Inbox DB         |                | Inbox DB         |
   | Search Cluster   |                | Search Cluster   |
   | Notification     |                | Notification     |
   | Object Storage   |                | Object Storage   |
   +------------------+                +------------------+
```

---

# Key Concepts Covered (What Interviewers Expect)

If you walk through Gmail design like we did, you naturally cover **all major HLD concepts**:

```
API design
fanout on write
message queues
sharding (user_id)
replication
event-driven architecture
search indexing
real-time notifications
object storage
spam detection
multi-region disaster recovery
```

This is exactly the level expected for **Senior / Staff interviews**.

Since you're preparing for **product companies / Google level interviews**, this is the **right depth**.

---

# What Interviewers Often Ask After Gmail Design

Very common follow-ups:

```
How to delete emails safely?
How to implement email threads?
How to support labels like Gmail?
How to support undo-send?
How to handle email ordering with eventual consistency?
```

---

✅ **My suggestion:**
Next we should do one of the **most frequently asked follow-ups** in Gmail design:

```
How Gmail implements conversation threads
```

This question appears surprisingly often because it tests **data modeling and indexing**.

If you want, we can continue the story with that next.
