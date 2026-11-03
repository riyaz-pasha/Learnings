Here are the 5 metrics that matter most, and more importantly, *why* each one drives your design decisions:

**1. Daily Active Users (DAU) / Monthly Active Users (MAU)**
This is your north star number. Everything else derives from it. If an interviewer says "design Twitter," you ask "are we talking 10M DAU or 500M DAU?" — because those are fundamentally different systems.

**2. Read:Write Ratio**
This single number tells you where to optimize. A 100:1 read-heavy system (like Twitter's feed) pushes you toward aggressive caching and read replicas. A write-heavy system (like logging or metrics ingestion) pushes you toward write-optimized storage and async pipelines. Always ask this.

**3. Data Size per Entity (and total storage growth)**
Just the ballpark — "each post is ~1KB, each photo ~2MB." This tells you whether you need blob storage, whether sharding is necessary soon, and how your database will behave over time.

**4. Latency SLA**
Is this a real-time system (sub-100ms)? Or is eventual consistency acceptable (seconds/minutes)? This one question decides whether you need WebSockets vs polling, whether you need a cache in front of your DB, and whether you can afford synchronous calls between services.

**5. Availability Requirement**
99.9% ("three nines") vs 99.999% ("five nines") is the difference between a simple setup and a full multi-region, active-active deployment. Ask it once, and it anchors your fault tolerance decisions throughout the interview.

---

The key insight is that these 5 aren't random — they form a **causal chain**: DAU tells you *scale*, read/write ratio tells you *bottleneck*, data size tells you *storage strategy*, latency tells you *architecture style*, and availability tells you *resilience investment*. Once you have these, every major design decision has a justification rooted in the numbers.
