Perfect — let’s go step by step and produce a **complete High-Level System Design for a Web Search Engine** (like Google/Bing-lite).
This will be structured exactly per your requested deliverables.

---

# 📖 High-Level System Design: Web Search Engine

---

## 1. Functional & Non-Functional Requirements

### Functional Requirements

| Requirement                      | Description                                                                      | Priority | Complexity                                                                 |
| -------------------------------- | -------------------------------------------------------------------------------- | -------- | -------------------------------------------------------------------------- |
| **Web Crawling**                 | Continuously crawl web pages, respecting robots.txt and crawl policies.          | P0       | Politeness (rate limiting), avoiding infinite loops, distributed crawling. |
| **Content Parsing & Extraction** | Parse HTML, extract text, metadata, links.                                       | P0       | Handling malformed HTML, different encodings, structured data.             |
| **Indexing**                     | Build inverted index mapping terms → documents. Support updates and deletions.   | P0       | Large-scale storage, sharding, incremental updates.                        |
| **Query Parsing & Search**       | Accept user queries, tokenize, normalize, apply spell-check, synonyms, stemming. | P0       | Handling ambiguous queries, typos, multi-lingual input.                    |
| **Ranking**                      | Rank results using relevance metrics (BM25, PageRank, ML models).                | P0       | Balancing freshness vs. relevance, adversarial SEO spam.                   |
| **Caching**                      | Cache popular queries and results.                                               | P1       | Cache invalidation, balancing memory vs. freshness.                        |
| **Autocomplete**                 | Suggest queries while typing.                                                    | P1       | Latency-critical (<100ms), prefix trie storage.                            |
| **Spell Correction**             | “Did you mean” suggestions.                                                      | P2       | Requires edit distance algorithms or ML models.                            |
| **Advanced Search Filters**      | Site/domain filtering, time ranges, file types.                                  | P2       | More indexing metadata, query parser complexity.                           |

#### User Personas

* **End Users:** Want fast, relevant search results (<200ms).
* **Content Creators / Webmasters:** Expect fair indexing, option to exclude via robots.txt.
* **System Operators:** Need monitoring, scalability, and low operational cost.

---

### Non-Functional Requirements

* **Performance:** P95 latency <200ms for search queries; throughput 100K QPS globally.
* **Scalability:** Support billions of pages; 1B+ documents indexed.
* **Availability:** 99.99% uptime for query serving; 99.9% for crawling.
* **Consistency:** Search results can be eventually consistent (freshness lag allowed).
* **Security:** HTTPS-only, rate limits, bot detection.
* **Compliance:** GDPR (right to be forgotten), DMCA takedown.
* **Maintainability:** Auto-scaling crawlers, rolling updates, observability.

---

### Assumptions & Scope

* **In scope:** Web crawling, indexing, query serving, ranking, caching, autocomplete.
* **Out of scope:** Ads, personalization, knowledge graph.
* **Constraints:** Assume cloud deployment (AWS/GCP/Azure), budget \$5M/year.

---

## 2. Back-of-the-Envelope Estimation

Assumptions:

* **MAU**: 100M
* **DAU**: 20M (20%)
* **QPS**: Avg 20k, Peak 100k
* **Index size**: 1B documents, avg 50KB text each → 50 TB raw.
* **Inverted index** (term → doc postings): 3x raw size ≈ 150 TB.
* **Cache**: Top 10% queries cover 80% traffic → cache 10M queries.

### Calculations

* **Storage**:

  * Raw pages: 50 TB
  * Index: 150 TB
  * Metadata (PageRank, features): 20 TB
  * Total ≈ **220 TB**
* **Query Traffic**:

  * Avg = 20k QPS → 1.7B/day
  * Peak = 100k QPS
* **Cache**:

  * 10M queries × 2KB result JSON = 20 GB (fits in Redis cluster).
* **Messaging throughput**:

  * Crawl frontier updates = 1M URLs/s peak.
* **Monthly Cloud Cost (ballpark)**:

  * Compute: \$500k
  * Storage: \$100k
  * Network: \$50k
  * Total ≈ **\$650k/month**

---

## 3. API Design

### Search API

```http
GET /search?q={query}&page={cursor}&filters={json}
Headers: Authorization: Bearer <token>
```

**Response**

```json
{
  "results": [
    { "url": "https://example.com", "title": "Example", "snippet": "..." }
  ],
  "next_cursor": "eyJwYWdlIjoyfQ=="  // base64({ "page": 2 })
}
```

* **Auth**: OAuth2 / API key
* **Rate-limiting**: 100 req/sec/IP
* **Status codes**: 200 (OK), 400 (bad query), 429 (rate limit)

### Autocomplete API

```http
GET /autocomplete?prefix=mach
```

### Admin APIs

* `POST /crawl/seed`
* `DELETE /index/url`

---

## 4. High-Level Architecture

### ASCII Diagram

```
          +----------+
User ---> |  CDN/ELB | ---> API Gateway ---> Query Service ---> Cache (Redis)
          +----------+                          |              |
                                                v              v
                                        Index Service ---> Inverted Index (ElasticSearch/Custom)
                                                |
                                       Ranking Service (ML/Scoring)
```

**Crawling pipeline**

```
Crawlers -> Kafka -> Parser -> Indexer -> Storage
```

---

## 5. Functional Requirement Deep Dives

### Example: **Search Query Execution (Critical Read)**

1. Client sends query → API Gateway → Query Service.
2. Query Service checks Redis (cache-aside).
3. If cache miss:

   * Query parsed, tokenized.
   * Inverted index lookup across shards.
   * Ranking Service scores results.
   * Top-K returned.
   * Cached in Redis (`SETEX search:{query}`).
4. Results sent back to client.

#### Data Storage

* **Inverted Index** in ElasticSearch / custom Lucene clusters.
* **Redis cache keys**: `search:{query}` TTL=5m.
* **Pagination cursor**: `{ "page": 2, "last_doc_id": 12345 }` → base64.

#### Edge Cases

* Empty results → return fallback “did you mean?”.
* Offline cache node → fallback to DB.
* Partial shard timeout → return partial results (graceful degradation).

---

### Summary Table

| Feature  | Client → Server | Server → DB/Cache    | Server → Recipient  | Notes                          |
| -------- | --------------- | -------------------- | ------------------- | ------------------------------ |
| Search   | Query API call  | Redis → Index        | Return JSON results | Pagination cursor, cache aside |
| Crawling | Seed URL push   | Kafka frontier       | Crawl workers       | Politeness policy              |
| Indexing | Parser pushes   | Write inverted index | Refresh shards      | Eventual consistency           |
| Ranking  | Query → Ranker  | Reads features       | Ordered list        | ML inference latency           |

---

## 6. Database & Storage Deep Dive

### Index DB (ElasticSearch / Lucene)

* **Documents Table**
  \| Field | Type | Notes |
  \|-------|------|-------|
  \| doc\_id | bigint | PK |
  \| url | text | unique |
  \| title | text | analyzed |
  \| body | text | inverted index |
  \| pagerank | float | ranking feature |
  \| crawl\_time | timestamp | freshness |

* **Queries**

```sql
-- Fetch docs containing term
SELECT doc_id FROM inverted_index WHERE term='machine';
```

* **Cache Keys**
  `search:{query}` → list of {url, title, snippet}

### Sharding

* Partition by doc\_id hash → balanced distribution.
* Scatter-gather queries across shards.

---

## 7. Caching Strategy

* **Browser/CDN**: static assets, autocomplete.
* **App-level Redis**: cache-aside for search results.
* **Eviction policy**: LRU.
* **TTL**: 5 minutes for search results.
* **Stampede protection**: mutex key (`SETNX`).

---

## 8. Messaging & Eventing

* **Kafka Topics**

  * `crawl.frontier` (partition by domain)
  * `crawl.parsed`
  * `index.updates`
* **Retention**: 7 days
* **DLQ**: malformed pages
* **Idempotency**: URL digest (SHA256) as key.

---

## 9. Scalability, HA & Operations

* **Autoscaling**: Crawlers scale by queue depth, query servers by QPS.
* **Multi-region**: Read replicas close to users, async index replication.
* **Monitoring**:

  * RED metrics: Rate, Errors, Duration
  * Kafka lag
  * Redis hit ratio
  * Index shard latency
* **Runbooks**: Cache miss storm, Kafka lag > 10k, shard imbalance.

---

## 10. Security, Compliance & Privacy

* **Auth**: OAuth2 for APIs, mTLS internal.
* **Compliance**: GDPR delete endpoint (`DELETE /index/url`).
* **Security**: XSS sanitization, bot detection (CAPTCHA).

---

## 11. Cost, Trade-offs & Future Roadmap

* **Main costs**: Index storage, query serving infra, crawler bandwidth.
* **Trade-offs**:

  * Freshness vs. consistency (delayed index updates).
  * Memory cost vs. cache hit ratio.
  * Fan-out queries vs. pre-computed results.
* **Roadmap**:

  * Personalization (per-user ranking).
  * Multimedia search (images, videos).
  * Federated ML ranking.

---

✅ That’s the **full structured design** for a scalable web search engine.

---

---

Great — let’s do a **deep dive into Crawling (P0 functional requirement)**.
I’ll structure it exactly as you requested: plain-English flows first, then concrete DB/Kafka/Redis examples, then a summary table.

---

# 🕷️ Crawling — Functional Requirement Deep Dive

---

## 1. Request Flow from Client to Server

Unlike search queries, crawling is **system-initiated**, not user-initiated.

**Flow:**

1. **Seeding:**

   * Admin/API seeds new URLs (e.g., `POST /crawl/seed`).
   * These are pushed into the **Crawl Frontier** (a distributed priority queue, usually Kafka + Redis + RocksDB).
2. **Crawl Scheduler:**

   * Continuously dequeues URLs from the frontier.
   * Enforces politeness (rate limits per domain/IP).
   * Deduplicates using a URL digest (SHA256).
3. **Crawlers (workers):**

   * Fetch content via HTTP/HTTPS.
   * Respect `robots.txt` and `crawl-delay`.
   * Store fetched page (raw HTML) in **Object Store** (S3/HDFS).
4. **Parsing Service:**

   * Consumes raw HTML, extracts text, links, metadata.
   * Sends extracted links back to `crawl.frontier` (Kafka).
   * Pushes parsed doc into **Indexing Pipeline**.

---

## 2. Data Storage and Retrieval

* **Frontier Storage:**

  * Redis Sorted Set / Kafka topic for URL queue.
  * Key = `domain:next_fetch_time` → value = list of URLs.
  * Score = priority (PageRank, freshness, recency).
* **Raw Page Store:**

  * S3/HDFS bucket per domain.
  * Key = SHA256(url).
* **Crawl Metadata DB (SQL or NoSQL):**

  * Stores per-URL crawl state.

**Table: `crawl_metadata`**

| Column        | Type      | Notes                      |
| ------------- | --------- | -------------------------- |
| url\_digest   | char(64)  | PK = SHA256(url)           |
| url           | text      | full URL                   |
| last\_crawled | timestamp | last fetch time            |
| crawl\_status | enum      | {pending, success, failed} |
| retries       | int       | exponential backoff        |
| http\_status  | int       | last HTTP response         |

**Redis Examples**

```redis
ZADD crawl:frontier 1693640400 "http://example.com/page1"
ZPOPMIN crawl:frontier   # fetch earliest scheduled URL
```

**Kafka Topics**

* `crawl.frontier` (URL seeds, deduplicated)
* `crawl.raw` (raw HTML payloads)
* `crawl.parsed` (extracted text, links)

---

## 3. Delivery to Other Clients / Devices

* Not delivered to end users directly.
* Parsed docs flow into **Indexing Service** → **Search Index**.
* Extracted links re-enter the **Frontier** (recursive expansion).
* Admin UI may query `crawl_metadata` to show status/progress.

---

## 4. Edge Cases & Failure Handling

* **Dead links / 404:** Mark `crawl_status=failed`, exponential backoff before retry.
* **Robots.txt disallow:** Skip crawl, store metadata.
* **Infinite loops (calendar pages, faceted navigation):** Detect via URL fingerprint (similar hash prefixes), enforce max depth.
* **Large pages:** Truncate at max size (e.g., 5MB).
* **Slow domains:** Scheduler enforces crawl-delay (e.g., 1 req/sec).
* **Crawler crash:** Use Kafka consumer groups with checkpoint offsets for at-least-once delivery.

---

## 5. Optional Features

* **Priority crawling:** Boost score for news domains (freshness).
* **Sitemaps:** Crawl `sitemap.xml` for efficiency.
* **Distributed frontier:** Spread URLs across shards by domain hash.
* **Content deduplication:** Store simhash of body → skip near-duplicates.

---

## 6. Example Event Payloads

**Kafka: `crawl.frontier`**

```json
{
  "url": "http://example.com/page1",
  "priority": 0.9,
  "depth": 3,
  "seed_source": "example.com",
  "scheduled_time": 1693640400
}
```

**Kafka: `crawl.raw`**

```json
{
  "url": "http://example.com/page1",
  "fetched_at": "2025-09-02T12:00:00Z",
  "http_status": 200,
  "raw_html_ref": "s3://crawl-bucket/sha256(url)"
}
```

**Kafka: `crawl.parsed`**

```json
{
  "url": "http://example.com/page1",
  "title": "Example Page",
  "text": "This is the content...",
  "out_links": [
    "http://example.com/page2",
    "http://other.com/home"
  ]
}
```

---

## 7. Pseudocode Example (Python-style)

### Scheduler Worker

```python
def scheduler_loop():
    while True:
        # Fetch URL with lowest scheduled_time
        url = redis.zpopmin("crawl:frontier")
        if not url:
            time.sleep(1)
            continue

        if allowed_by_robots(url):
            kafka.send("crawl.frontier", {"url": url})
        else:
            update_metadata(url, status="blocked")
```

### Crawler Worker

```python
def crawl_worker():
    for msg in kafka.consume("crawl.frontier"):
        url = msg["url"]
        html, status = fetch_url(url)

        if status == 200:
            ref = store_s3("crawl-bucket", sha256(url), html)
            kafka.send("crawl.raw", {"url": url, "raw_html_ref": ref, "http_status": status})
        else:
            update_metadata(url, status="failed", http_status=status)
```

---

## 8. Summary Table

| Functional Requirement | Client → Server flow     | Server → DB/Cache flow                                 | Server → Recipient flow        | Notes                            |
| ---------------------- | ------------------------ | ------------------------------------------------------ | ------------------------------ | -------------------------------- |
| Crawling (seed)        | Admin seeds URLs via API | Insert URL → `crawl_metadata` + push to Redis frontier | N/A                            | Seeds initial crawl frontier     |
| Scheduling             | N/A                      | Redis frontier → Kafka                                 | Crawlers consume               | Politeness, deduplication        |
| Crawling (fetch)       | N/A                      | Raw HTML stored in S3                                  | Publish to Kafka (`crawl.raw`) | Retries on failure               |
| Parsing                | N/A                      | Extracted text/links → Kafka (`crawl.parsed`)          | Indexer consumes               | Extract links → re-seed frontier |
| Deduplication          | N/A                      | URL digest check in DB                                 | N/A                            | Avoid infinite loops             |

---

## 9. Complexities & Trade-offs

* **Frontier storage:**

  * Redis = fast, but memory heavy → good for hot queue.
  * Kafka = scalable, durable → good for backlog.
    → Recommended: Hybrid (Redis for “next N minutes”, Kafka for backlog).
* **Fetch consistency:**

  * At-least-once → some duplicate fetches possible.
  * Exactly-once is too expensive.
    → Deduplication at parser is cheaper.
* **Freshness vs. Coverage:**

  * Trade-off between re-crawling known pages vs. discovering new pages.
  * Adaptive scheduling: frequently updated sites crawled more often.

---

## 10. Instrumentation Points

* Frontier size (Redis ZSET size, Kafka lag).
* Crawl success rate (200 vs 404 vs 500).
* Average crawl latency/domain.
* Politeness violations (too many hits on one domain).
* Storage growth in S3.

---

✅ That’s a full deep dive into **Crawling**.

---

Perfect — let’s **zoom in specifically on Crawling / Spidering lifecycle** and answer your questions step by step. I’ll explain in **plain English first** and then back it with concrete technical details (storage, queues, events).

---

# 🌐 Web Crawling (Spidering) Deep Dive

---

## 1. Where do seed URLs come from?

* **Sources of seed URLs:**

  1. **Manual / Admin** — an operator bootstraps the system with a list of high-value domains (e.g., `https://wikipedia.org`, `https://cnn.com`).
  2. **Sitemaps** — known `sitemap.xml` from popular domains.
  3. **Partner submissions** — webmasters can submit their site via a `POST /crawl/seed` API.
  4. **Historical data** — previously crawled URLs stored in `crawl_metadata` DB can act as seeds for a new crawl cycle.

* **Storage:**
  Seed URLs are inserted into the **Crawl Frontier** (a priority queue) and persisted in a **Metadata DB** to track crawl history.

```sql
INSERT INTO crawl_metadata (url_digest, url, crawl_status, last_crawled)
VALUES (SHA256('https://wikipedia.org'), 'https://wikipedia.org', 'pending', NULL);
```

---

## 2. Who adds these seed URLs?

* **Admin Service** (via REST API)

  * `POST /crawl/seed { "url": "https://wikipedia.org" }`
* **Automated Jobs**

  * Periodically inject known sitemaps or “high priority” domains.
* **Parser Service**

  * As crawled pages are parsed, new outbound links are discovered and pushed into the frontier automatically.

So: **initial seeds** = Admin / Config.
**continuous seeds** = Discovered links from parsed pages.

---

## 3. Where are these stored?

* **Crawl Metadata DB** (SQL or NoSQL): Keeps canonical record of every known URL (hash digest, last crawled, status).
* **Crawl Frontier Queue** (Redis + Kafka hybrid): Active queue of URLs to crawl next.

### Example Redis Frontier

```redis
ZADD crawl:frontier 1693650000 "https://wikipedia.org"
```

(score = next scheduled fetch time)

---

## 4. Who pushes seed URLs to the queue?

* The **Admin API** or **Parser Service** pushes URLs into **Kafka → Redis**.
* The **Scheduler Service** dequeues from Kafka backlog and pushes to Redis (short-term crawl horizon).

**Kafka: `crawl.frontier` event**

```json
{
  "url": "https://wikipedia.org",
  "priority": 1.0,
  "depth": 0,
  "scheduled_time": 1693650000
}
```

---

## 5. Once crawler consumes this event what happens?

1. **Crawler Worker** (consumer of `crawl.frontier`) picks a URL.
2. Checks **robots.txt**:

   * Fetches `https://wikipedia.org/robots.txt` (cached per domain).
   * If disallowed → update `crawl_metadata` as `blocked`.
3. If allowed:

   * Performs **HTTP GET** for the URL.
   * Captures:

     * HTTP status (200, 404, etc.)
     * Response headers
     * Raw HTML body
   * Writes raw HTML to **Object Store** (e.g., S3).
   * Updates **crawl\_metadata** with `last_crawled=now`, `status=success`.
   * Publishes event to `crawl.raw`.

---

## 6. How does crawler know where to fetch data from?

* **URL is in the message payload** (from `crawl.frontier`).
* Crawler has **HTTP client** logic:

  * Resolves DNS for the domain.
  * Opens TCP/TLS connection.
  * Sends `GET /path HTTP/1.1`.
* Politeness rules:

  * Scheduler ensures only N requests/sec/domain.
  * Each crawler process respects `crawl-delay`.

---

## 7. What does crawler download from the URL?

The crawler downloads **entire HTTP response**:

* **Status code** (200, 301, 404, etc.)
* **Headers** (Content-Type, Content-Length, Last-Modified)
* **Body**

  * If `text/html` → raw HTML (primary input for parsing).
  * If non-HTML (PDF, image, video):

    * Either store for specialized indexing OR skip (depending on config).
* **Storage**

  * `raw_html` → S3/HDFS with key = SHA256(url).
  * Metadata (status, fetch time) → SQL DB.

**Example S3 path:**

```
s3://crawl-bucket/36b1c8...f9c.html
```

---

## 📑 Example Lifecycle of a Seed URL

1. Admin calls:

   ```http
   POST /crawl/seed
   { "url": "https://wikipedia.org" }
   ```
2. Service inserts row in `crawl_metadata` + pushes event to `crawl.frontier`.
3. Scheduler pushes to Redis frontier:

   ```redis
   ZADD crawl:frontier 1693650000 "https://wikipedia.org"
   ```
4. Crawler worker consumes:

   * Downloads HTML
   * Stores in `s3://crawl-bucket/sha256(wikipedia.org)`
   * Emits Kafka event:

     ```json
     {
       "url": "https://wikipedia.org",
       "http_status": 200,
       "raw_html_ref": "s3://crawl-bucket/36b1c8.html"
     }
     ```
5. Parser service consumes this → extracts text + links → new URLs fed back into frontier.

---

✅ So in short:

* **Seed URLs** come from admins, partners, sitemaps, or discovered links.
* **Stored** in `crawl_metadata` DB + pushed into `crawl.frontier` queue.
* **Crawler consumes** → fetches via HTTP → downloads HTML + metadata.
* **Outputs** raw content → stored in S3 and event emitted for parsing.

---

---

Perfect question 🚀 — the **Parser** is the glue between the **Crawler** (which only fetches raw bytes) and the **Indexer** (which needs clean text + metadata).
Let’s go step by step into **what the Parser does**.

---

# 📑 Parser — Detailed Deep Dive

---

## 1. Role of Parser

The **Crawler downloads raw HTML (or PDF, etc.)** → the Parser **transforms it into structured data** for indexing and for frontier expansion.

It handles:

* **Content extraction** (text, title, meta tags)
* **Link extraction** (URLs to feed back into crawl frontier)
* **Normalization & cleaning** (strip JS, CSS, ads, etc.)
* **Feature extraction** (language, word counts, freshness signals)

Think of it as:
**Crawler → (raw messy content) → Parser → (structured doc + new URLs)**

---

## 2. Input to Parser

* Kafka topic: `crawl.raw`
* Message includes:

```json
{
  "url": "https://example.com/page1",
  "http_status": 200,
  "fetched_at": "2025-09-02T12:00:00Z",
  "raw_html_ref": "s3://crawl-bucket/36b1c8.html"
}
```

Parser downloads raw HTML from S3 and begins processing.

---

## 3. Steps the Parser Performs

### 🔹 Step 1: MIME-Type Handling

* Check `Content-Type` header.
* If `text/html` → HTML parsing pipeline.
* If `application/pdf`, `image/jpeg`, etc. → specialized extractor or skip.

### 🔹 Step 2: HTML Parsing

* Use robust HTML parser (e.g., **JSoup**, **BeautifulSoup**, **Gumbo**).
* Handle malformed HTML gracefully.
* Extract DOM structure.

### 🔹 Step 3: Content Extraction

* Extract:

  * **Title** (`<title>` tag, OpenGraph meta).
  * **Body text** (main content only, stripping menus/ads).
  * **Meta tags** (`description`, `keywords`).
  * **Headings** (`<h1>…<h6>`).
  * **Language detection** (using n-gram models or CLD3).
  * **Canonical URL** (`<link rel="canonical">`).

*Optional*: Run **boilerplate removal** algorithms (like Readability or Boilerpipe) to extract just article text.

### 🔹 Step 4: Link Extraction

* Collect all `<a href="…">` links.
* Normalize:

  * Resolve relative → absolute URLs.
  * Strip fragments (`#section`).
  * Deduplicate.
* Apply filtering (skip `mailto:`, `javascript:`, blocked domains).
* Send valid outbound links → `crawl.frontier`.

### 🔹 Step 5: Text Normalization

* Lowercase.
* Tokenization (split words, handle Unicode).
* Stemming/Lemmatization (e.g., “running” → “run”).
* Stopword removal (“the”, “is”, “of”).
* Optional: Named Entity Recognition (extract entities).

### 🔹 Step 6: Feature Extraction (for Ranking)

* Word count, unique terms.
* TF-IDF features.
* Page freshness (Last-Modified header).
* Outbound link count (for PageRank).
* Content length.
* Structured metadata (schema.org, OpenGraph, JSON-LD).

### 🔹 Step 7: Structured Document Output

* Emit structured doc to `crawl.parsed` (Kafka).
* Store parsed text in **Document Store** (NoSQL DB, e.g., Cassandra or MongoDB).

---

## 4. Output of Parser

**Kafka Event: `crawl.parsed`**

```json
{
  "url": "https://example.com/page1",
  "title": "Example Page",
  "text": "This is the main content text of the page ...",
  "lang": "en",
  "features": {
    "word_count": 452,
    "out_links_count": 25,
    "last_modified": "2025-08-31T22:00:00Z"
  },
  "out_links": [
    "https://example.com/page2",
    "https://other.com/home"
  ]
}
```

This feeds into:

* **Indexer Service** → inverted index (for search).
* **Crawl Frontier** → new URLs (recursive crawling).

---

## 5. Example Pseudocode (Python-style)

```python
def parser_worker():
    for msg in kafka.consume("crawl.raw"):
        url = msg["url"]
        html = fetch_from_s3(msg["raw_html_ref"])

        soup = BeautifulSoup(html, "html.parser")

        # Content extraction
        title = soup.title.string if soup.title else ""
        text = extract_main_content(soup)  # boilerplate removal
        meta_desc = soup.find("meta", {"name": "description"})

        # Links
        links = []
        for a in soup.find_all("a", href=True):
            link = normalize_url(url, a['href'])
            if is_valid_url(link):
                links.append(link)

        # Build parsed doc
        parsed_doc = {
            "url": url,
            "title": title,
            "text": text,
            "features": {"word_count": len(text.split()), "out_links_count": len(links)},
            "out_links": links
        }

        # Emit
        kafka.send("crawl.parsed", parsed_doc)
        for link in links:
            kafka.send("crawl.frontier", {"url": link, "depth": msg.get("depth",0)+1})
```

---

## 6. Edge Cases & Handling

* **Non-HTML docs** → Skip or route to specialized parsers (PDF, image OCR).
* **Duplicate content** → Use **simhash / shingling** to detect near-duplicates, avoid indexing twice.
* **Malformed HTML** → Use tolerant parsers (like Gumbo or JSoup).
* **Spam / Cloaking detection** → Compare text vs. visible content; detect hidden links.
* **Infinite loops** → Hash content; if identical body seen multiple times → stop.

---

## 7. Summary Table

| Step | Parser Action       | Input             | Output               | Notes                |
| ---- | ------------------- | ----------------- | -------------------- | -------------------- |
| 1    | MIME check          | `crawl.raw` event | choose parser        | Skip non-supported   |
| 2    | HTML parsing        | Raw HTML          | DOM tree             | Robust parser needed |
| 3    | Content extraction  | DOM               | Title, text, meta    | Remove boilerplate   |
| 4    | Link extraction     | `<a href>`        | Outbound links       | Normalized, deduped  |
| 5    | Text normalization  | Text              | Tokens, stems        | For indexing         |
| 6    | Feature extraction  | Text + metadata   | Word count, lang     | For ranking          |
| 7    | Emit structured doc | All above         | `crawl.parsed` event | Goes to Indexer      |

---

✅ So, **Parser = Extractor + Cleaner + Link Discoverer**.
It **bridges raw HTML → searchable index**, and **feeds new URLs** back into the crawl loop.

---

---

Perfect — let’s move to the **Indexer**, which is the next stage after the Parser.
This is where the system transforms **parsed documents** into a **searchable inverted index**.

---

# 📚 Indexer — How Parsed Docs Become Searchable

---

## 1. Role of the Indexer

The **Indexer** ingests structured documents (from `crawl.parsed`) and builds/updates an **inverted index** that maps **terms → documents**.
It enables **fast retrieval** when users search.

Think of it like:

```
Parsed Doc  →  Indexer  →  Inverted Index (sharded, queryable)
```

---

## 2. Input to Indexer

* From Kafka topic: `crawl.parsed`
* Message includes:

```json
{
  "url": "https://example.com/page1",
  "title": "Example Page",
  "text": "This is the main content text ...",
  "lang": "en",
  "features": {
    "word_count": 452,
    "out_links_count": 25
  }
}
```

---

## 3. Steps the Indexer Performs

### 🔹 Step 1: Tokenization

* Break text into tokens/words.
* Example: `"This is the main content"` → `[this, is, the, main, content]`.

### 🔹 Step 2: Normalization

* Lowercasing.
* Remove punctuation.
* Handle Unicode.
* Language-specific stemming/lemmatization (e.g., running → run).
* Stopword removal (“the”, “is”, “and”).

### 🔹 Step 3: Term → Document Mapping

* Build posting lists:

  * Term → (doc\_id, positions, term frequency).
* Example:

  ```
  "content" → [(doc123, pos=4)]
  "main"    → [(doc123, pos=3)]
  ```

### 🔹 Step 4: Store Metadata

* Store per-doc features:

  * URL, title, snippet (first \~200 chars), language.
  * PageRank or importance score.
  * Word count, freshness timestamp.
* Stored in a **Document Store** (NoSQL or SQL) for quick retrieval.

### 🔹 Step 5: Sharding

* Partition inverted index across many servers.
* Common strategy: **hash(term) → shard**.
* Each shard stores postings for its assigned terms.

### 🔹 Step 6: Index Updates

* Insert new docs.
* Update existing docs (if re-crawled).
* Delete docs if expired / robots.txt noindex.
* Support **eventual consistency** (updates take time to propagate).

---

## 4. Storage Structures

### Inverted Index (NoSQL or Lucene-like)

* **Table: `inverted_index`**
  \| Column | Type | Notes |
  \|--------|------|-------|
  \| term | text | Partition key |
  \| doc\_id | bigint | clustering key |
  \| positions | list<int> | word positions |
  \| tf | int | term frequency |

Example query:

```sql
-- Get postings for "content"
SELECT doc_id, positions FROM inverted_index WHERE term = 'content';
```

### Document Store (Metadata)

* **Table: `documents`**
  \| Column | Type | Notes |
  \|--------|------|-------|
  \| doc\_id | bigint PK | Internal ID |
  \| url | text | Canonical URL |
  \| title | text | Extracted title |
  \| snippet | text | Short preview |
  \| lang | text | Language |
  \| pagerank | float | Precomputed score |
  \| last\_crawled | timestamp | Freshness |

---

## 5. Cache Usage

* **Redis** caches:

  * `doc:{doc_id}` → metadata for rendering results.
  * `term:{query}` → postings for frequent terms.
* TTL: 5–10 minutes.
* Eviction: LRU.

---

## 6. Example Event Payloads

**Kafka: `index.updates`**

```json
{
  "doc_id": 12345,
  "url": "https://example.com/page1",
  "terms": {
    "example": [1],
    "page": [2],
    "content": [3, 7]
  },
  "features": {
    "word_count": 452,
    "pagerank": 0.6
  }
}
```

---

## 7. Pseudocode Example (Python-style)

### Indexer Worker

```python
def indexer_worker():
    for msg in kafka.consume("crawl.parsed"):
        doc_id = assign_doc_id(msg["url"])
        tokens = tokenize_and_normalize(msg["text"])

        postings = defaultdict(list)
        for pos, token in enumerate(tokens):
            postings[token].append(pos)

        # Write postings into inverted index (sharded DB)
        for term, positions in postings.items():
            db.insert("inverted_index", term=term, doc_id=doc_id,
                      positions=positions, tf=len(positions))

        # Write metadata
        db.insert("documents", doc_id=doc_id, url=msg["url"],
                  title=msg["title"], snippet=msg["text"][:200],
                  pagerank=compute_pagerank(msg["out_links_count"]))
```

---

## 8. Edge Cases & Handling

* **Duplicate content**:

  * If same content hash seen → skip indexing.
* **Spam pages**:

  * Detect keyword stuffing (too high TF).
* **Huge documents**:

  * Limit to first N tokens (e.g., 10k).
* **Frequent re-crawls**:

  * Replace old postings, update freshness.
* **Deleted/noindexed pages**:

  * Remove from index, mark in metadata.

---

## 9. Summary Table

| Step | Indexer Action | Input                | Output            | Notes                   |
| ---- | -------------- | -------------------- | ----------------- | ----------------------- |
| 1    | Tokenization   | Text from parser     | Tokens            | Language-specific rules |
| 2    | Normalization  | Tokens               | Normalized terms  | Lowercase, stemming     |
| 3    | Build postings | Terms → doc\_id      | Posting lists     | For inverted index      |
| 4    | Store metadata | URL, title, features | Doc Store         | For rendering           |
| 5    | Sharding       | Hash(term) → shard   | Distributed index | Balances load           |
| 6    | Updates        | Parsed docs          | Index updates     | Eventual consistency    |

---

✅ In summary:

* The **Parser** hands off clean text + links.
* The **Indexer** tokenizes text, builds posting lists, updates document metadata, and distributes it across shards.
* This is what powers **fast search lookups** later.

---
---
Perfect ✅ — you’ve already outlined the **Crawl → Parse → Index pipeline** really well.
Let me polish this into a **complete, structured, step-by-step flow** with extra details (queues, DB tables, payloads, edge cases). This will give you a **blueprint** you could hand to an engineer.

---

# 🌐 Web Search Engine Data Pipeline

---

## 1. Crawl Process

**Goal:** Fetch raw HTML from the web and store it for downstream processing.

### Flow

1. **Seeding / Queueing**

   * URLs get added into the **crawl queue** (`crawl.frontier`).
   * Sources:

     * User/Admin submissions (via **URL Manager Service**).
     * Backlinks discovered by the **Parser**.
     * Preloaded seeds (Wikipedia, news sites, etc.).

   **Kafka event (`crawl.frontier`):**

   ```json
   {
     "url": "https://example.com/page1",
     "priority": 0.9,
     "depth": 2,
     "scheduled_time": "2025-09-02T12:00:00Z"
   }
   ```

2. **Crawler Worker**

   * Consumes `crawl.frontier`.
   * Performs:

     * **DNS Lookup** → resolves `example.com` → IP.
     * **Robots.txt fetch & check** → ensure URL is allowed.
     * **HTTP(S) GET request** → download raw HTML.
   * Stores raw HTML in **Object Storage** (S3, GCS, HDFS).

   **S3 path:**

   ```
   s3://crawl-bucket/sha256(url).html
   ```

3. **DB Update**

   * Store metadata in **Crawl Metadata DB**:

     * URL
     * S3 key
     * Last fetched timestamp
     * HTTP status

   **Table: `crawl_metadata`**

   | url\_digest | url | s3\_key | last\_fetched | http\_status | status |
   | ----------- | --- | ------- | ------------- | ------------ | ------ |

   Example row:

   ```
   ("36b1c8...", "https://example.com/page1", "s3://crawl-bucket/36b1c8.html", "2025-09-02", 200, "success")
   ```

4. **Emit crawl.raw**

   * After successful fetch, publish an event:

   ```json
   {
     "url": "https://example.com/page1",
     "s3_key": "s3://crawl-bucket/36b1c8.html",
     "http_status": 200,
     "fetched_at": "2025-09-02T12:01:00Z"
   }
   ```

---

## 2. Parsing Process

**Goal:** Transform raw HTML into structured, searchable content + discover new URLs.

### Flow

1. **Consume crawl.raw**

   * Parser fetches raw HTML from S3.

2. **Build DOM Tree**

   * Use HTML parser library (Jsoup, BeautifulSoup).
   * DOM = structured tree of `<title>`, `<body>`, `<h1>`, `<p>`, `<a>`.

3. **Content Extraction**

   * Remove boilerplate (ads, navbars, scripts).
   * Extract:

     * Visible text
     * Title
     * Metadata (keywords, description)
     * Headings
   * Pass extracted text → Indexing System.

4. **Link Extraction**

   * Find all `<a href="...">` links.
   * Normalize (absolute URLs, strip fragments).
   * Check if crawled:

     ```sql
     SELECT 1 FROM crawl_metadata WHERE url_digest = SHA256(new_url);
     ```
   * If not crawled → publish to `crawl.frontier`.

   **Example new link:**

   ```json
   { "url": "https://example.com/page2", "priority": 0.8, "depth": 3 }
   ```

5. **Store Parsed Text**

   * Save structured doc in **Document Store**:

   **Table: `documents`**

   | doc\_id | url | title | text | lang | snippet | features |
   | ------- | --- | ----- | ---- | ---- | ------- | -------- |

6. **Emit crawl.parsed**

   * Publish event:

   ```json
   {
     "url": "https://example.com/page1",
     "doc_id": 12345,
     "title": "Example Page",
     "text": "This is the main content...",
     "out_links": ["https://example.com/page2", "https://other.com/home"]
   }
   ```

---

## 3. Indexer Process

**Goal:** Build the **Inverted Index** (term → documents).

### Flow

1. **Consume crawl.parsed**

   * Fetch parsed doc from Document Store.

2. **Normalization**

   * Tokenization: `"Running fast"` → `[running, fast]`.
   * Lowercasing: `[running, fast]` → `[running, fast]`.
   * Stemming: `running → run`.
   * Stopword removal: remove `the, is, of`.

3. **Inverted Index Update**

   * For each term, add `(docId, frequency, [positions])`.

   Example:

   ```
   Term "run" → [(doc123, tf=1, pos=[0])]
   Term "fast" → [(doc123, tf=1, pos=[1])]
   ```

   **Table: `inverted_index`**

   | term | doc\_id | positions | tf |
   | ---- | ------- | --------- | -- |
   | run  | 12345   | \[0]      | 1  |
   | fast | 12345   | \[1]      | 1  |

4. **Emit index.updates**

   * So ranking/scoring can run later.

---

## 📑 End-to-End Summary

| Stage   | Input            | Processing                                         | Output          | Storage                     |
| ------- | ---------------- | -------------------------------------------------- | --------------- | --------------------------- |
| Crawl   | `crawl.frontier` | DNS lookup, robots.txt, fetch HTML                 | `crawl.raw`     | S3 (raw), Crawl Metadata DB |
| Parsing | `crawl.raw`      | DOM parse, extract text + links                    | `crawl.parsed`  | Document Store              |
| Indexer | `crawl.parsed`   | Tokenization, normalization, inverted index update | `index.updates` | Inverted Index DB           |

---

✅ So, in short:

* **Crawlers** fetch → **Parsers** clean & expand → **Indexers** tokenize & index.
* This loop runs continuously, discovering billions of pages and updating the search index.

---

---

Spot on ✅ — **Crawl Politeness** is critical because without it, crawlers can look like an **unintentional DDoS**. Let’s expand your notes into a complete, production-style strategy.

---

# 🤝 Load on Websites (Crawl Politeness)

---

## 1. The Problem

* Crawlers can easily overload a site by making **hundreds/thousands of requests per second**.
* This can:

  * Violate **robots.txt** rules.
  * Cause sites to **block crawler IPs**.
  * Look like a DDoS attack.

---

## 2. Core Politeness Rules

* **Respect robots.txt** (allow/disallow rules + optional `Crawl-delay`).
* **Throttle requests per domain** (don’t exceed allowed rate).
* **Back off on errors** (especially 429 Too Many Requests, 503 Service Unavailable).
* **Avoid parallel fetches** to same domain.

---

## 3. Solutions in Detail

### 🔹 1. Robots.txt Fetch & Storage

* When crawler first encounters a domain → fetch `https://domain.com/robots.txt`.
* Parse disallowed paths + crawl-delay.
* Store in **Robots DB** (keyed by domain).

**Table: `robots_rules`**

| domain      | disallowed\_paths | crawl\_delay | last\_fetched |
| ----------- | ----------------- | ------------ | ------------- |
| example.com | \["/private/\*"]  | 5 sec        | 2025-09-02    |

* TTL: refresh every 24h.
* If robots.txt missing/unreachable → assume default policy (allow all, crawl-delay = N seconds, e.g., 1s).

---

### 🔹 2. Per-Domain Crawl Delay Enforcement

* Maintain a **per-domain queue** with lastFetchedTimestamp.
* Before fetching a URL:

  ```python
  if now - lastFetchedTimestamp < crawl_delay:
      requeue(url)
      continue
  ```
* This ensures only **one request per domain** at a time.
* Crawl delay can come from:

  * Robots.txt
  * Default config (e.g., 1s/domain)

---

### 🔹 3. Error Response Handling

* If **429 Too Many Requests**:

  * Exponential backoff (retry after 2s, 4s, 8s...).
  * Respect `Retry-After` header if present.
* If **503 Service Unavailable**:

  * Similar backoff.
* If **5xx persists** for N retries:

  * Mark domain temporarily inactive.

---

### 🔹 4. Adaptive Rate Control

* Track success/error rate per domain.
* If error rate > threshold → slow down crawl rate.
* If fast responses and no errors → cautiously increase crawl rate.

---

### 🔹 5. Distributed Crawlers

* Even with politeness rules, central IP ranges can get blocked.
* Use:

  * Multiple IP pools / subnets.
  * Geographic distribution (closer to sites, less latency).
* Still enforce **per-domain rate limit across the whole system** (global coordination).

---

## 4. Implementation Approach

### Redis Example (per-domain crawl scheduling)

```redis
HSET crawl:domain:example.com lastFetchedTimestamp 1693650000 crawlDelay 5
```

Crawler logic:

```python
def can_crawl(domain):
    record = redis.hgetall(f"crawl:domain:{domain}")
    if not record:  # robots.txt not fetched yet
        fetch_and_store_robots(domain)
        return False

    delay = record.get("crawlDelay", DEFAULT_DELAY)
    last = record.get("lastFetchedTimestamp", 0)

    if time.time() - last < delay:
        return False

    redis.hset(f"crawl:domain:{domain}", "lastFetchedTimestamp", time.time())
    return True
```

---

## 5. Example Flow

1. Scheduler assigns `https://example.com/page1`.
2. Crawler checks Redis → `lastFetchedTimestamp=1693650000`, `crawlDelay=5`.
3. If <5s since last fetch → requeue.
4. Otherwise:

   * Fetch URL.
   * Store content in S3.
   * Update timestamp.
   * If 429 received → backoff and reschedule.

---

## 6. ASCII Diagram

```
Crawl Queue
    |
    v
Scheduler ----> Domain Politeness Check (Redis/DB)
                   |
                   | allowed
                   v
              Crawler Worker
                   |
            [Robots.txt Policy]
                   |
                Fetch HTML
                   |
                   v
              Object Storage
```

---

## 7. Key Optimizations

* **Robots.txt Cache:** Don’t fetch for every URL, cache per domain.
* **Per-Domain Queues:** Avoid simultaneous requests.
* **Exponential Backoff:** On 429/503.
* **Distributed Crawler Coordination:** Use global state store (Redis/ZooKeeper) to enforce limits across datacenters.
* **Heuristic crawl delay:** If no crawl-delay in robots.txt, assume safe default (1–2s).

---

✅ With these rules:

* We **don’t hammer websites**.
* We stay **compliant** with robots.txt.
* Crawling scales without looking like an attack.

---
---


## 🔄 Detailed Indexing Flow

### 1. Event Consumption

* **Source:** `crawl.parsed` event → `{ url, documentId }`
* **Service:** Indexer Service subscribes to this topic.

---

### 2. Document Retrieval

* Fetch document text + metadata from the **Document Store** using `documentId`.
  Example:

  ```json
  {
    "documentId": "doc1",
    "url": "https://example.com/page",
    "title": "Running fast and slow",
    "content": "Running is great. Some people run fast, others run slow."
  }
  ```

---

### 3. Text Processing Pipeline

Each stage is modular → easy to extend later:

1. **Tokenization**

   ```
   ["Running", "is", "great", "Some", "people", "run", "fast", "others", "run", "slow"]
   ```

2. **Normalization**

   * Lowercasing
   * Replace symbols: `& → and`, etc.

   ```
   ["running", "is", "great", "some", "people", "run", "fast", "others", "run", "slow"]
   ```

3. **Stop-word Removal**

   * Remove common words: `is, some, and, the, a`

   ```
   ["running", "great", "people", "run", "fast", "others", "run", "slow"]
   ```

4. **Stemming / Lemmatization**

   * Convert to root forms:

   ```
   ["run", "great", "people", "run", "fast", "others", "run", "slow"]
   ```

5. **Position Tracking + Frequency Counting**

   * Build per-document term stats:

   ```json
   {
     "run": { "freq": 3, "positions": [0, 3, 5] },
     "great": { "freq": 1, "positions": [1] },
     "people": { "freq": 1, "positions": [2] },
     "fast": { "freq": 1, "positions": [4] },
     "others": { "freq": 1, "positions": [6] },
     "slow": { "freq": 1, "positions": [7] }
   }
   ```

---

### 4. Index Update

Now we **merge per-document stats into the global inverted index**.

**Global Inverted Index Structure**:

```
term → { docId → {freq, positions[]} }
```

Example after adding `doc1` and `doc3`:

```json
"run": {
   "doc1": { "freq": 3, "positions": [0,3,5] },
   "doc3": { "freq": 1, "positions": [21] }
},
"slow": {
   "doc1": { "freq": 1, "positions": [7] }
}
```

---

### 5. Storage Options

* **In-memory prototype:** HashMap/dictionary
* **Production:**

  * **Elasticsearch / OpenSearch** – handles inverted index, sharding, query ranking out-of-the-box
  * **Custom storage:**

    * Key-Value store (RocksDB, LevelDB, Cassandra)
    * Schema:

      ```
      Key = term
      Value = postings list → [(docId, freq, positions[])]
      ```

---

### 6. Metadata & Ranking Stats

Along with postings list, store:

* Document Length (for normalization in BM25)
* Global Document Frequency (for IDF computation)
* PageRank / URL Score

---

### 7. Event Publication

* After updating index, emit `index.updated` event:

  ```json
  {
    "documentId": "doc1",
    "url": "https://example.com/page",
    "status": "indexed"
  }
  ```
* Downstream consumers: Search API, Analytics, Ranking Service.

---

## 📊 Data Flow Recap

```
[crawl.parsed] event → [Indexer Service] 
   → [Document Store lookup] 
   → [Tokenization → Normalization → Stopwords → Stemming] 
   → [Term frequencies + positions] 
   → [Update Inverted Index Store] 
   → [Emit index.updated]
```

---

## 🛠️ Example: Search-time Flow (quick preview)

When a user searches `"running slow"`:

1. Query is normalized → `"run slow"`
2. Index lookup:

   * `run → {doc1, doc3}`
   * `slow → {doc1}`
3. Candidate documents = `{doc1, doc3}`
4. Rank by TF-IDF/BM25 → return top results

---


---

💯 That’s exactly the **core Search Service flow** — you’ve nailed it!
Let’s refine and extend your flow into a **production-ready design**, adding the missing details around ranking, snippet generation, and response formatting.

---

# 🔍 Search Service Flow (Detailed)

### 1. Query Submission

* **Frontend → Search API**
  User types `"running slow"` into the frontend app.
  Request hits:

  ```
  GET /search?q=running+slow&page=1&size=10
  ```

---

### 2. Query Processing

The Search Service applies **same pipeline as Indexer** (to ensure consistency):

* **Tokenization**: `"running slow"` → `["running", "slow"]`
* **Lowercasing**: → `["running", "slow"]`
* **Normalization**: remove punctuation, symbols
* **Stop-word Removal**: (none here, but removes things like `"the", "is", "a"`)
* **Stemming/Lemmatization**: `"running" → "run"`
  → Final query tokens: `["run", "slow"]`

---

### 3. Index Lookup

Look up tokens in the **Inverted Index**:

```
"run"  → {doc1: freq=3, doc3: freq=1}
"slow" → {doc1: freq=1}
```

Candidate set = {doc1, doc3}

---

### 4. Ranking Engine

**Step A: Compute TF (Term Frequency per document)**

* `run` appears **3 times** in `doc1`, **1 time** in `doc3`
* `slow` appears **1 time** in `doc1`

**Step B: Compute IDF (Inverse Document Frequency)**

* Formula:

  $$
  idf(t) = \log \frac{N}{1 + df(t)}
  $$

  * `N = total docs`
  * `df(t) = number of docs containing t`

If `N=1000`:

* `df(run)=200` → `idf(run) = log(1000/201) ≈ 1.6`
* `df(slow)=10`  → `idf(slow) = log(1000/11) ≈ 4.5`

**Step C: Final Score (TF-IDF per doc)**

$$
score(q,d) = \sum_{t \in q} tf(t,d) \times idf(t)
$$

* For `doc1`:

  * run: `3 * 1.6 = 4.8`
  * slow: `1 * 4.5 = 4.5`
  * **Total = 9.3**

* For `doc3`:

  * run: `1 * 1.6 = 1.6`
  * **Total = 1.6**

✅ Ranking → `doc1` ranked higher than `doc3`

---

### 5. Fetch Metadata & Snippets

From the **Document Store**:

* Title
* URL
* Extract snippet text around matched terms, highlight tokens.

Example:

```
doc1 snippet → "… people <b>run</b> fast, others <b>run</b> <b>slow</b> …"
```

---

### 6. Return Results

JSON response to user:

```json
{
  "query": "running slow",
  "results": [
    {
      "docId": "doc1",
      "title": "Running fast and slow",
      "url": "https://example.com/page",
      "score": 9.3,
      "snippet": "... people <b>run</b> fast, others <b>run</b> <b>slow</b> ..."
    },
    {
      "docId": "doc3",
      "title": "Benefits of daily runs",
      "url": "https://example.com/runs",
      "score": 1.6,
      "snippet": "... best time to <b>run</b> every day ..."
    }
  ]
}
```

---

# ⚙️ Search Service – Responsibilities Recap

* ✅ Receive queries from frontend
* ✅ Apply **same text analysis as indexer**
* ✅ Look up tokens in **inverted index**
* ✅ Rank using **TF-IDF (or BM25 for better results)**
* ✅ Fetch snippets from Document Store
* ✅ Return ranked JSON results

---
