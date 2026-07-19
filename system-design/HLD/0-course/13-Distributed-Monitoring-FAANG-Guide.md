# Distributed Monitoring — FAANG Interview Guide

> **Enhancement notes:** this pass added the pieces a FAANG interviewer
> expects but the original draft assumed or skipped: a **Requirements**
> section (§2), a **Capacity estimation** walkthrough with illustrative
> numbers (§4), an explicit **Data model** section for metric+labels→series
> (§7), and an **API design** section covering push/pull/query endpoints
> (§10). It also deepened existing sections: a cardinality-explosion
> flowchart + retention-tiers table (§9), an architecture **evolution
> diagram v1→v2→v3** (§11), a query-performance-at-scale subsection (§12),
> and a rule-evaluation-at-scale explainer with a sequence diagram (§13).
> All net-new headings are marked 🆕; everything else (mental model, cost
> numbers, failure domains, metric types, push/pull core comparison, alert
> lifecycle, client-side deep-dive, three pillars, real systems) is
> untouched apart from small clarity tweaks and cross-references. Section
> numbers 2–17 shifted to make room — the content order and voice are
> otherwise the original guide's.

## The whole chapter in one picture

```mermaid
mindmap
  root((Distributed<br/>Monitoring))
    Before you design
      Requirements
      Capacity estimation
    Why
      Cascading failures
      Downtime cost
    What to measure
      Metric types
      RED / USE / Golden Signals
      Data model - name+labels=series
    How to collect
      Push vs Pull
    APIs
      Push endpoint
      Pull endpoint
      Query API
    Where to store
      Time-series DB
      Gorilla compression
      Cardinality explosion
      Retention tiers
    How to see it
      Dashboards
      Heatmaps / Top-N
      Query performance
    How to react
      Alert lifecycle
      Rule evaluation at scale
      SLI / SLO / SLA
    Client-side
      RUM
      Local buffer + beacon
    Beyond metrics
      Logs
      Traces
```

Come back to this picture after reading the guide once — if you can regenerate every branch from memory, you're interview-ready.

---

## 1. Mental model

A distributed system is a black box made of thousands of moving parts across
hundreds of servers and dozens of data centers. Monitoring is the
**nervous system** — it converts silent internal state (CPU load, error
rates, queue depth) into signals a human or an automated system can act on.

Three questions a monitoring system must answer, in order of urgency:

1. **Is something on fire right now?** → alerting
2. **What does "normal" look like, and are we drifting?** → metrics + dashboards
3. **Why did it break, after the fact?** → logs + traces (root cause)

Without monitoring, the only failure signal is a user complaint or a
support ticket — by which point the damage is already done. The goal is to
detect failures **before** they cascade, not after.

The whole discipline collapses into one pipeline — every section below is
one box in this picture:

```mermaid
flowchart LR
    I["Instrument<br/>code emits metrics"] --> C["Collect<br/>push or pull"]
    C --> St["Store<br/>time-series DB"]
    St --> Q["Query / Visualize<br/>dashboards"]
    St --> Al["Alert<br/>evaluate rules"]
    Al --> N["Notify<br/>on-call"]
```

### Why cascading failures matter (from the course example)

```mermaid
sequenceDiagram
    participant U as User
    participant A as UI Service (Server A)
    participant B as Service 2 (Server B)
    participant C as Service 3 (Server C)
    participant DX as DB X
    participant DY as DB Y

    U->>A: Upload video
    A->>B: Forward video metadata
    C--xC: Service 3 fails (sync job)
    B->>DX: Write video entry
    DX--xDX: DB X crashes (unsynced with Y)
    U->>DY: Request video playback
    DY--xU: "Video not found"
```

One silent failure (service 3) turns into a **customer-visible** error
several hops downstream. Nobody paged on service 3 failing — the first
signal anyone got was a user-facing 404. That gap is exactly what
monitoring closes.

**Interview cheat-sheet:**
- Frame monitoring as "convert unknowns into knowns, early."
- Always mention **early warning** and **root-causing** as the two jobs — interviewers listen for this split.
- Cascading-failure example is a great 30-second opener to justify *why* the interviewer should care about this building block.

---

## 2. 🆕 Requirements — what to clarify before you design anything

Ask these out loud in the first couple of minutes. They drive every later
decision — push vs. pull, retention length, how strict your cardinality
limits need to be.

**Functional requirements**
- Collect metrics from every host/service/container in the fleet — both OS-level (CPU, memory, disk) and custom application metrics.
- Store metrics as time series, queryable by metric name plus labels, over an arbitrary time range.
- Visualize metrics on dashboards (ad-hoc exploration + saved dashboards).
- Continuously evaluate alerting rules and notify on-call when a rule fires.
- (Stretch, mention and move on) support both real-time queries (seconds-old data) and historical queries (years-old, downsampled data).

**Non-functional requirements**
- **Scale**: a fleet of 100K+ hosts, each emitting hundreds to thousands of metrics — that's millions of active time series (see capacity estimation below).
- **Low collection overhead**: instrumentation must not itself slow down the thing being measured — "death by observability" is a real failure mode.
- **Write-heavy**: ingestion volume dwarfs read volume. Optimize the write path first, the read path second.
- **The monitoring system must be more available than what it watches** — if it shares a failure domain with the systems it monitors, an outage can blind you at the exact moment you need visibility.
- **Durability vs. cost**: losing the last few seconds of raw samples in a crash is tolerable; silently losing historical trend data is not.
- **Fast queries over huge ranges**: "p99 latency for the last 30 days" should render in a couple of seconds, not minutes.

**Explicitly out of scope for a first pass** — say this so the interviewer knows you know the boundary: distributed tracing internals, log storage/search, synthetic/blackbox probing. Acknowledge the three pillars exist (§ Logs vs. Metrics vs. Traces below), then keep the rest of the interview focused on metrics.

**Cheat-sheet:**
- Lead with "collect → store → query → visualize → alert," then immediately flag fleet size × metrics-per-host as the number that drives every architecture choice.
- If the interviewer doesn't hand you a fleet size, pick one and label it illustrative (e.g., "let's say 100K hosts") — never leave capacity estimation undefined.

---

## 3. The cost of not monitoring (memorize these numbers)

| Incident | Date | Cost |
|---|---|---|
| Meta (FB/IG/WhatsApp/Oculus) outage | Oct 2021 | ~$13M / hour |
| AWS us-east-1 network congestion outage | Dec 7, 2021 | ~$66,240 / minute |
| General rule of thumb interviewers expect | — | "Every minute of downtime for a top-tier consumer app costs tens of thousands of dollars, plus reputational/SLA damage" |

Use these numbers to justify **why** you're spending interview time on
observability instead of jumping straight to a data model — monitoring is a
first-class non-functional requirement, not an afterthought.

**Cheat-sheet:**
- Downtime cost scales with (revenue/sec + SLA penalties + churn), not just infra cost.
- Root cause of the AWS Dec 2021 outage: an automated capacity-scaling job triggered a connection storm that congested internal network devices — a **feedback loop**, not a hardware failure. Good example of "monitoring must watch for retry storms," not just raw errors.

---

## 4. 🆕 Capacity estimation (the numbers interviewers want to see)

Treat the numbers below as illustrative — state your assumptions out
loud, then compute. The interviewer cares more about the *method*
(what multiplies by what) than the exact digits.

**Ingest rate**
- Assume 100,000 hosts, each emitting 1,000 distinct time series, scraped/pushed every 15s.
- Data points/sec = 100,000 × 1,000 ÷ 15 ≈ **6.7M data points/sec** fleet-wide.
- At roughly 2 bytes/point after Gorilla-style compression (see § TSDB below), that's ~13 MB/sec of compressed write throughput, ~1.1 TB/day.

**Cardinality**
- Active time series = hosts × metrics-per-host = 100,000 × 1,000 = **100M active series**.
- Each series needs an in-memory index entry (metric name + label set). At ~500 bytes/series that's ~50 GB just for the index — before a single sample is stored. This is why cardinality, not raw point volume, is the scarce resource in a TSDB.
- Contrast: a label with unbounded cardinality (e.g., `user_id` with 10M distinct values) can turn 1,000 base metrics into up to **10 billion** series — this is the failure mode covered in the cardinality-explosion section below.

**Storage / retention**
- Raw (15s resolution), kept 15 days: 6.7M points/sec × 86,400s × 15 days × ~2 bytes ≈ **~17 TB** compressed.
- 5-min rollups, kept 13 months: roughly 1/20th the point rate of raw → a few TB.
- 1-hour rollups, kept years: smaller still — effectively "free" to retain forever.
- Rule of thumb: each downsampling tier costs roughly an order of magnitude less than the tier below it. That's why tiered retention — not "keep everything at full resolution forever" — is the only economical option at this scale.

**Query load**
- Reads (dashboards opening, alert rules ticking) are bursty, not constant. Provision for peak concurrent queries, not the average.

**Cheat-sheet:**
- Memorize the *shape* of the calculation, not the digits: `hosts × metrics/host ÷ scrape_interval = points/sec`.
- Cardinality (unique label combinations) — not raw point count — is what actually breaks a TSDB. Call this out explicitly; it's the thing interviewers probe for.

---

## 5. Two failure domains: server-side vs. client-side

| | Server-side errors | Client-side errors |
|---|---|---|
| **HTTP class** | 5xx | 4xx (visible) / nothing at all (invisible) |
| **Visibility** | Always visible to the backend — it generated the error | Sometimes invisible — request never reached the server |
| **Detected via** | APM agents, server logs, exception tracking | RUM (real user monitoring), client-side SDKs, beacon pings |
| **Example** | DB connection pool exhausted → 503 | User's WiFi drops before request is sent → server sees *nothing* |
| **Tooling** | Prometheus, Datadog APM, New Relic | Sentry, Bugsnag, Google Analytics RUM, Firebase Crashlytics |

The invisible case (request never arrives) is the hard one — you cannot
instrument a server for a request it never received. This is why
client-side monitoring needs its **own** pipeline (client SDK batches
events locally, sends them opportunistically) rather than piggybacking on
server logs.

**Cheat-sheet:**
- If the interviewer says "how would you know a user's request never made it to your service" — that's the client-side/invisible-error signal. Answer: client SDK + periodic beacon/heartbeat + local buffering with retry-on-reconnect.

---

## 6. Metrics: the atomic unit of monitoring

A **metric** = what to measure + the unit + a timestamped value.
Good metrics have low collection overhead — measuring must not itself
degrade the system (avoid death-by-observability).

### Metric types (the source glosses over this — know it cold)

| Type | Semantics | Example | Aggregation |
|---|---|---|---|
| **Counter** | Monotonically increasing, resets on restart | `requests_total`, `errors_total` | `rate()` / `increase()` over time window |
| **Gauge** | Point-in-time value, goes up or down | `queue_depth`, `memory_used_bytes` | last value, avg, min/max |
| **Histogram** | Distribution of values bucketed by range | `request_latency_ms` buckets: `<10, <50, <100, <500, +Inf` | compute p50/p90/p99 from buckets |
| **Summary** | Pre-computed quantiles client-side | `request_latency_ms{quantile="0.99"}` | can't re-aggregate across instances (major limitation) |

**Trade-off interviewers love to probe:** histogram vs. summary. Histograms
are aggregatable across servers (bucket counts sum), summaries are not
(you cannot average two p99s and get a correct global p99). Always prefer
histograms for anything you'll aggregate across a fleet.

**Pick-the-metric-type flowchart** — run through this in your head any time
you instrument something new:

```mermaid
flowchart TD
    Q1{"Does the value only ever increase?"} -->|Yes| Counter["Counter<br/>use rate over a window"]
    Q1 -->|No, goes up and down| Q2{"Need a distribution / percentiles?"}
    Q2 -->|No, just current value| Gauge["Gauge<br/>e.g. queue_depth"]
    Q2 -->|Yes| Q3{"Will you aggregate across many hosts?"}
    Q3 -->|Yes| Histogram["Histogram<br/>bucket counts sum cleanly"]
    Q3 -->|No, single host view is fine| Summary["Summary<br/>pre-computed quantile, not aggregatable"]
```

### What to actually collect
- OS-level: CPU (user/sys/iowait), memory (RSS, page faults, swap), disk (IOPS, latency, free space), network (throughput, retransmits).
- App-level (via code instrumentation): request rate, error rate, latency percentiles, queue depths, cache hit ratio, thread-pool saturation.
- The **RED method** (for request-driven services): **R**ate, **E**rrors, **D**uration.
- The **USE method** (for resources): **U**tilization, **S**aturation, **E**rrors.

**Cheat-sheet:**
- RED = "how are my services doing" (front-end for user traffic).
- USE = "how are my resources doing" (back-end, hardware/infra).
- Always tie a metric to an **action** — a metric nobody alerts on or dashboards is dead weight and adds collection overhead for nothing.

---

## 7. 🆕 Data model: how a "metric" becomes a "time series"

A **metric name** plus a **set of labels (key/value tags)** together
define one **time series** — a stream of `(timestamp, value)` pairs
ordered in time. Change any label *value* and you get a different
series, not the same one with more data.

```mermaid
flowchart TD
    M["Metric name<br/>http_requests_total"] --> L1["+ labels<br/>{method=GET, status=200, host=web-01}"]
    M --> L2["+ labels<br/>{method=POST, status=500, host=web-02}"]
    L1 --> S1["Series 1<br/>(t1,v1) (t2,v2) (t3,v3) ..."]
    L2 --> S2["Series 2<br/>(t1,v1) (t2,v2) (t3,v3) ..."]
```

Concretely, one sample looks like this (Prometheus exposition style):

```
http_requests_total{method="GET", status="200", host="web-01"} = 42893  @ t=1737200015
```

- **Metric name** — what's being measured (`http_requests_total`, `cpu_usage_percent`).
- **Labels/tags** — dimensions you'll want to slice or filter by (`method`, `status`, `region`, `host`). Every unique combination of label values is its own series, indexed separately.
- **Timestamp** — when the sample was taken (usually collection time, not event time).
- **Value** — a single float for counters/gauges, or a set of bucket counts for a histogram.

This is exactly why label choice *is* the capacity-planning decision —
adding a label multiplies series count by that label's cardinality (see
the cardinality-explosion section under TSDBs, below).

**Cheat-sheet:**
- Mnemonic: **"Name + Labels = Series."** Same name, different label values → different series, each indexed and stored separately.
- Cardinality multiplies across labels, it doesn't add: a metric with 3 labels of cardinality 10, 5, and 2 produces `10 × 5 × 2 = 100` series, not `10 + 5 + 2 = 17`.

---

## 8. Push vs. pull — the central design decision

The course frames this correctly: **always describe push/pull from the
monitoring system's point of view**, not the server's, or you'll confuse
your interviewer mid-explanation.

A sequence diagram makes the key asymmetry obvious — **who initiates, and
on whose schedule**:

```mermaid
sequenceDiagram
    autonumber
    participant App as Monitored App
    participant Mon as Monitoring System
    Note over App,Mon: PULL model (Prometheus, Borgmon)
    Mon->>App: GET /metrics (on Mon's own schedule)
    App-->>Mon: current counter/gauge values
    Note over Mon: A failed scrape IS a liveness signal — free health check
```

```mermaid
sequenceDiagram
    autonumber
    participant App as Monitored App
    participant Mon as Monitoring System
    Note over App,Mon: PUSH model (StatsD, CloudWatch)
    App->>Mon: metric sample (UDP/HTTP), on App's own schedule
    Note over Mon: An app that goes silent looks identical to one that's healthy and idle
```

| | Pull (Prometheus-style) | Push (StatsD/Graphite-style) |
|---|---|---|
| **Who controls cadence** | Monitoring system (scrape interval) | Each server (can flood or under-report) |
| **Overload risk** | Low — monitoring system paces itself | High — many servers can push simultaneously and overwhelm the collector |
| **Firewall/NAT friendly** | No — monitoring system needs network access to every target | Yes — servers only need outbound access |
| **Short-lived jobs (batch/cron)** | Bad fit — job may finish before next scrape | Good fit — push-and-die, or push via a gateway (Prometheus Pushgateway) |
| **Service discovery need** | Yes — monitoring system must know all targets (via Consul/K8s API/DNS) | No — servers self-register by pushing |
| **Real examples** | Prometheus, Google Borgmon/Monarch | StatsD, Graphite, AWS CloudWatch (custom metrics), Facebook ODS |

**🆕 Why Prometheus actually chose pull:** three reasons, in order of how
often interviewers ask for them —
1. **The monitoring system stays in control of its own load.** With push, a bug that makes every host push twice as often can overwhelm the collector; with pull, the scraper paces itself no matter what the targets do.
2. **A failed scrape is a free liveness check.** You don't need a separate heartbeat mechanism — "can I reach `/metrics`" already tells you the process is up and responsive.
3. **It composes cleanly with service discovery.** Point Prometheus at Kubernetes'/Consul's target list and it scrapes whatever exists right now — no per-host config push needed when hosts come and go.

The trade-off it accepts: the monitoring system needs network reachability
to every target (harder across firewalls/NAT), and short-lived jobs need
a workaround (Pushgateway) since they may not exist at the next scrape.

**Interview answer skeleton:** "I'd default to pull for long-running
services — it's simpler to reason about load on the monitoring system and
plays well with service discovery in Kubernetes. I'd add a push path (via a
gateway) for short-lived batch/cron jobs that die before a scrape would
catch them."

**Cheat-sheet:**
- Pull = monitoring system in control, self-throttling, needs service discovery.
- Push = server in control, better through firewalls, needs a push-gateway for ephemeral jobs, risk of thundering herd.
- Real systems often run **both**: Prometheus pulls app instances directly but pulls a Pushgateway that batch jobs push into.

---

## 9. Persisting the data: time-series databases

A centralized in-memory store works at small scale. At FAANG scale (millions
of time series, thousands of samples/sec), you need a **time-series
database (TSDB)** purpose-built for:
- Append-only, timestamp-ordered writes (never random-access updates)
- Massive compression (timestamps and values are highly predictable)
- Efficient range queries ("give me this metric for the last 6 hours")
- Downsampling / rollups for long retention without unbounded storage

### Key compression trick (bring this up for depth)
Facebook's **Gorilla** (paper: "Gorilla: A Fast, Scalable, In-Memory Time
Series Database") compresses timestamp+value pairs ~12x using:
- **Delta-of-delta encoding** for timestamps (samples arrive at near-fixed intervals, so the *second* delta is usually 0)
- **XOR encoding** for floating-point values (consecutive values are usually close, so XOR-ing them yields mostly leading/trailing zero bits)

This is the single most "I've done my homework" fact you can drop in a
monitoring deep-dive.

### Real-world TSDBs
| System | Origin | Notes |
|---|---|---|
| Prometheus TSDB | CNCF/Kubernetes ecosystem | Local disk, 2-hour blocks, pull-based scraping |
| InfluxDB | Open source | Push-based, SQL-like query language |
| OpenTSDB | Built on HBase | Horizontally scalable, older-generation |
| Gorilla / Beringei | Facebook | In-memory, ~12x compression, feeds Grafana-like dashboards |
| M3DB | Uber | Built for horizontal scale + long retention, powers Uber's M3 stack |
| Amazon CloudWatch | AWS managed | Push-based, integrates natively with AWS services |
| Monarch | Google | Multi-tenant, hierarchical, backs Google's internal monitoring |

Retention is a straight-line pipeline — picture it as a conveyor belt that
gets coarser as data ages:

```mermaid
flowchart LR
    Raw["Raw samples<br/>full resolution<br/>hours to days"] --> Down1["5-min rollups<br/>weeks"]
    Down1 --> Down2["1-hour rollups<br/>months to years"]
    Down2 --> Archive["Cold archive / delete<br/>past retention policy"]
```

#### 🆕 Retention tiers, with illustrative numbers

| Tier | Resolution | Typical retention | Storage vs. raw | Use case |
|---|---|---|---|---|
| Raw | Every scrape (e.g., 15s) | Hours–days (Prometheus default: 15d) | 1x (baseline) | "What happened 10 minutes ago" |
| 5-min rollup | avg/max/min per 5 min | Weeks–13 months | ~1/20th | Dashboards over days/weeks |
| 1-hour rollup | avg/max/min per hour | Months–years | ~1/240th | Long-term/year-over-year trends |
| Cold archive | Same as last rollup, on object storage (S3/GCS) | Years, or forever | Negligible marginal cost | Compliance, historical analysis |

Mnemonic for the tiers: **Raw → Rolled → Really-old (archive)** —
resolution drops and retention length grows at every step.

**Cheat-sheet:**
- Retention strategy: keep raw samples for hours/days, downsample (avg/max per 5-min bucket) for weeks, further downsample for years. This bounds storage growth (a classic system design trade-off: **precision vs. storage cost**).
- **If a query's range is more than a few hours, it should read from a downsampled tier, not raw** — otherwise the query engine scans far more points than the dashboard can even render.

#### 🆕 Cardinality-explosion protection flowchart

Cardinality explosion is the #1 operational risk in a TSDB — a single
poorly-chosen label can multiply series count by orders of magnitude.
**Example:** 1,000 base time series with a `user_id` label carrying
100,000 distinct values turns into up to **100,000,000 series** — the
in-memory index alone can exceed available memory and take the whole
TSDB down. Real incidents at this scale are almost always a label like
`user_id`, `request_id`, `session_id`, `raw_url`, or a client IP baked
directly into a label.

```mermaid
flowchart TD
    New["New label value observed<br/>at ingestion"] --> Check{"Is this label unbounded?<br/>(user_id, request_id, IP, raw URL)"}
    Check -->|Yes| Reject["Reject or strip the label<br/>at the exporter / ingestion gateway"]
    Check -->|No, bounded set| Count{"Series count for this metric<br/>over the configured limit?"}
    Count -->|Yes| Throttle["Drop the new series,<br/>emit a 'cardinality limit exceeded' meta-metric"]
    Count -->|No| Accept["Accept — index and store the series"]
```

**Cheat-sheet:**
- **If a label's set of possible values is unbounded or user-controlled → never put it in a label.** Bucket it, hash it into a bounded set, or move it to a log/trace instead — metrics are for low-cardinality dimensions only.
- Always mention a hard per-metric series limit and an ingestion-time rejection rule — this is the concrete mechanism that turns "watch out for cardinality" from a slogan into a design decision.

---

## 10. 🆕 API design: push, pull, and query

Three API surfaces worth naming explicitly — interviewers want to see
you separate "how metrics get in" from "how metrics get out."

**1. Pull endpoint (exposed by every monitored instance)**
```
GET /metrics
```
Response — plain text, one line per series (this is the real Prometheus
exposition format, worth reciting from memory):
```
# TYPE http_requests_total counter
http_requests_total{method="GET",status="200"} 42893
http_requests_total{method="POST",status="500"} 12
# TYPE queue_depth gauge
queue_depth 47
```
The scraper hits this on a fixed interval (e.g., every 15s) per target.

**2. Push endpoint (for the push model, or a gateway for ephemeral jobs)**
```
POST /api/v1/push
Body: [{ "metric": "job_duration_seconds",
         "labels": {"job": "nightly-etl"},
         "value": 812.4,
         "timestamp": 1737200015 }]
```

**3. Query API (used by dashboards and the alerting engine alike)**
```
GET /api/v1/query_range?query=rate(http_requests_total{status="500"}[5m])&start=...&end=...&step=60s
```
Returns `(timestamp, value)` points per matching series. The same
query language (PromQL-style) powers both a dashboard panel and an
alert rule — one query engine, two consumers, by design.

**Cheat-sheet:**
- Three verbs to say out loud: **expose** (pull target), **ingest** (push target), **query** (read). Don't conflate the write API with the query API — they have very different load profiles: writes are constant and high-volume, queries are bursty.
- The query API is the one both Grafana and the alerting engine call — say this explicitly to show alerting is "just another query consumer," not a separate data path.

---

## 11. High-level architecture of a monitoring system

```mermaid
graph TD
    subgraph Monitored Fleet
        A1[App Server 1 + exporter]
        A2[App Server 2 + exporter]
        A3[App Server N + exporter]
    end

    A1 & A2 & A3 -->|pull/scrape| C[Collector / Scraper Layer]
    C --> Q[Ingestion Queue - buffering, backpressure]
    Q --> AGG[Aggregator - rollups, downsampling]
    AGG --> TSDB[(Time-series DB)]
    TSDB --> QE[Query Engine - PromQL/InfluxQL]
    QE --> DASH[Dashboard - Grafana]
    QE --> ALERT[Alerting Engine]
    ALERT --> NOTIF[Notification Fan-out - PagerDuty/Slack/Email]
    ALERT -->|dedup, group, silence| ALERT
```

**Components to name in an interview, in this order:**
1. **Agents/exporters** on every host — expose or push metrics (node_exporter, StatsD client, custom app instrumentation).
2. **Collector/scraper layer** — horizontally sharded, does service discovery to know what to scrape.
3. **Ingestion buffer/queue** — absorbs bursts, gives backpressure protection (a Kafka-like buffer is common at scale).
4. **Aggregator** — computes rollups, downsamples for long-term storage.
5. **Time-series storage** — the durable store discussed above.
6. **Query engine** — PromQL-style query layer for both dashboards and alert rules.
7. **Dashboarding** — Grafana-style visualization.
8. **Alerting engine** — evaluates rules against the query engine, handles **deduplication, grouping, silencing, and escalation** (Prometheus Alertmanager is the canonical example).

**Cheat-sheet:**
- Always mention that the monitoring system itself must be **more available and more decoupled** than the systems it monitors — if the primary DB and the monitoring system share infra, an outage can blind you exactly when you need visibility most (monitor the monitor / use a separate failure domain).
- Sharding the collector layer by service or by data center avoids one scraper trying to pull from every host globally.

#### 🆕 Architecture evolution: v1 → v2 → v3

Walk through this progression out loud when asked "how would this scale
as the fleet grows" — it shows you know *why* each extra piece of
complexity gets added, not just that it exists.

```mermaid
graph LR
    subgraph V1["v1 — single node (up to a few hundred hosts)"]
        A1[App hosts] -->|scrape| P1[Single Prometheus instance]
        P1 --> D1[Local-disk TSDB]
        P1 --> G1[Grafana]
    end
```
*Why it breaks:* one process can't hold millions of series in memory,
and one scraper can't reach thousands of hosts inside a 15s interval.

```mermaid
graph LR
    subgraph V2["v2 — sharded scrapers + remote-write (thousands of hosts)"]
        A2[App hosts, sharded by team/DC] -->|scrape| P2a[Scraper shard 1]
        A2 -->|scrape| P2b[Scraper shard 2]
        P2a -->|remote_write| TS2[(Horizontally-scaled TSDB<br/>e.g., Thanos / M3 / Mimir)]
        P2b -->|remote_write| TS2
        TS2 --> G2[Grafana / query federation layer]
    end
```
*Why it still isn't enough:* downsampling is ad hoc (or missing), and
alert-rule evaluation shares the same query engine as dashboards — a
burst of dashboard traffic can slow down alert evaluation right when
something is on fire.

```mermaid
graph LR
    subgraph V3["v3 — dedicated downsampling + alerting tiers (100K+ hosts)"]
        A3[App hosts] -->|scrape| P3[Sharded scraper layer]
        P3 -->|remote_write| Q3[Ingestion queue]
        Q3 --> TS3[(Sharded TSDB cluster)]
        TS3 --> DS3[Downsampling / rollup workers]
        DS3 --> TS3
        TS3 --> QE3[Query engine]
        QE3 --> G3[Dashboards]
        QE3 --> RE3[Rule-evaluation tier<br/>sharded by rule-group]
        RE3 --> AM3[Alertmanager<br/>dedup / group / route]
        AM3 --> N3[PagerDuty / Slack / Email]
    end
```
*What changed:* downsampling runs as its own background pipeline
instead of a one-off script, and rule evaluation is a horizontally
shardable tier that only needs read access to the query engine — so a
spike in dashboard queries never delays a page. Same "monitor the
monitor" principle as above, applied to the alerting path specifically.

---

## 12. Visualizing enormous volumes of data

Dashboards at scale can't render millions of raw points — a few techniques
worth naming:
- **Downsampling for display**: querying a week-long range returns 5-min averages, not raw per-second samples.
- **Heatmaps** for latency distributions over time (better than overlaying hundreds of percentile lines).
- **Top-N / anomaly-highlighting views**: instead of showing every host, surface the outliers (e.g., "these 3 of 10,000 hosts have p99 > 3x fleet median").
- **Golden signals dashboards**: one screen per service showing rate, errors, duration, saturation — the first thing an on-call engineer opens.

**Cheat-sheet:**
- If asked "how do you show a human millions of data points without melting their brain," answer: aggregate before you render, and surface outliers, don't dump raw series.

#### 🆕 Query performance over huge time ranges

A dashboard panel asking for "error rate over the last 90 days" must
not scan 90 days of raw 15-second samples. Four techniques make this
fast:

- **Recording rules**: pre-compute expensive queries (e.g., `rate(...)[5m]`) on a schedule and store the *result* as its own time series. The dashboard then reads the cheap pre-computed series instead of recomputing the aggregation on every page load.
- **Automatic resolution selection**: the query engine picks raw, 5-min, or 1-hour data based on the requested range and display width — no point returning more points than there are pixels on screen.
- **Query fan-out + merge**: a sharded TSDB runs the query on every shard in parallel and merges partial results at the query layer — the same scatter-gather pattern as any other sharded datastore.
- **Result caching**: cache query results keyed on `(query, range, step)` for a few seconds — an auto-refreshing dashboard shouldn't recompute an unchanged historical range on every tick.

**If X then Y:** if a query's range spans more than a few hours, route it to a downsampled tier and/or a recording rule — never let one ad-hoc dashboard query force a full raw-resolution scan across weeks of data.

---

## 13. Alerting

An alert = **condition/threshold** + **action**. Two components, but the
hard part in practice is avoiding **alert fatigue**.

Alerts aren't just on/off — they move through a real state machine
(this mirrors Prometheus Alertmanager's actual states, worth reciting
verbatim in an interview):

```mermaid
stateDiagram-v2
    [*] --> Inactive
    Inactive --> Pending: condition breached
    Pending --> Firing: still breached after "for" duration
    Pending --> Inactive: condition cleared before duration elapses
    Firing --> Resolved: condition clears
    Firing --> Silenced: on-call mutes it
    Silenced --> Firing: silence expires, still breached
    Resolved --> [*]
```

The `Pending` state is the quiet hero here — it's what stops a single
5-second blip from paging someone; only a breach that survives the `for`
window becomes `Firing`.

| Technique | Purpose |
|---|---|
| **Deduplication** | Same root cause firing 500 alerts across 500 hosts → collapse to 1 |
| **Grouping** | Alerts for the same service/incident bundled into a single notification |
| **Silencing/muting** | Suppress known, expected alerts (e.g., during planned maintenance) |
| **Escalation policies** | Page on-call → escalate to secondary → escalate to manager, on a timer |
| **Multi-window, multi-burn-rate alerts** | Alert only if an SLO error budget is burning fast (Google SRE technique) — avoids paging for tiny blips |

### SLI / SLO / SLA — bring these up, interviewers expect it
- **SLI** (indicator): the actual measured metric, e.g., "% of requests under 300ms."
- **SLO** (objective): the internal target, e.g., "99.9% of requests under 300ms over 30 days."
- **SLA** (agreement): the external, often contractual, commitment with financial penalties for breach — usually looser than the SLO to leave margin.
- **Error budget**: `1 - SLO`. If SLO is 99.9%, you have a 0.1% error budget per period — alerting logic should be built around **burn rate** against this budget, not raw thresholds.

**Cheat-sheet:**
- Alert on **symptoms** (user-facing latency/error rate), not causes (CPU is at 80%) — causes should feed dashboards, not pages, or you get paged for things that don't actually hurt users.
- Alerting engine should be a distinct component from the query/storage engine — Alertmanager pattern (rule evaluation is stateless and can run independently of storage).

#### 🆕 How rule evaluation actually runs at scale

An alert rule is just a query, run on a timer, compared against a
threshold. The scale problem isn't the metrics again — it's **sharding
the rules themselves** so one node isn't evaluating every rule for the
whole fleet.

```mermaid
sequenceDiagram
    autonumber
    participant RE as Rule Evaluator (one shard)
    participant QE as Query Engine / TSDB
    participant AM as Alertmanager
    participant OC as On-call (Slack/PagerDuty)

    loop every evaluation_interval (e.g., 30s)
        RE->>QE: run rule query (e.g., error_rate > 1%)
        QE-->>RE: current value per matching series
        alt condition breached
            RE->>RE: mark series Pending (start "for" timer)
        else condition clear
            RE->>RE: reset to Inactive
        end
    end
    RE->>AM: still breached after "for" duration -> Firing
    AM->>AM: dedup + group with other firing alerts
    AM->>OC: notify (respecting escalation policy)
```

Rule groups are sharded across many evaluator workers — by service,
team, or a hash of the rule name — so no single node evaluates every
rule in the fleet. Each worker only needs read access to the query
engine, not the raw storage layer, which keeps the alerting tier
decoupled from TSDB internals (same "monitor the monitor" principle as
the v3 architecture diagram above).

**Cheat-sheet:**
- Rule evaluation is "a query on a timer" — the interesting scale problem is sharding *rules*, not re-sharding metrics.
- If asked "what happens if the rule evaluator falls behind," the answer is: it's just another consumer of the query API, so it can be scaled out horizontally like any other stateless read client.

---

## 14. Client-side monitoring deep-dive

Server-side pain is always visible somewhere; client-side pain can be
**totally invisible** to the backend (the request never arrived). Real
systems solve this with:

- **RUM (Real User Monitoring)** SDKs embedded in the client (web/mobile) that record page-load time, JS exceptions, API call failures, and crash reports.
- **Local buffering + batch upload**: client buffers events and flushes them opportunistically (on a timer, on app foreground, or via `navigator.sendBeacon` on page unload) so flaky connectivity doesn't lose data.
- **Sampling**: at billions of events/day, sample (e.g., 1% of successful requests, 100% of errors) to control cost while still catching every failure.
- **Session replay / breadcrumbs** (Sentry-style): capture the sequence of user actions leading up to a crash for debugging without full video capture.
- **Beaconing/heartbeat**: a lightweight periodic "I'm still alive and my last N requests looked like X" ping lets you detect the case where the client can't reach the primary service at all, distinguishing "client crashed" from "client can't reach us."

The resilience trick is the local buffer sitting *between* the event and
the network call — draw this whenever asked "how does client-side
monitoring survive a flaky connection":

```mermaid
sequenceDiagram
    autonumber
    participant U as User action
    participant SDK as Client SDK (in-memory buffer)
    participant Disk as Local storage
    participant Srv as Ingestion endpoint

    U->>SDK: page view / API call / exception
    SDK->>Disk: persist event (survives app crash)
    alt network available
        SDK->>Srv: batched flush (timer, or sendBeacon on page unload)
        Srv-->>SDK: 200 OK
        SDK->>Disk: clear flushed events
    else network unavailable
        SDK->>SDK: keep buffered, retry on next launch/reconnect
    end
```

**Real-world examples:** Sentry, Bugsnag, Firebase Crashlytics, Google
Analytics/Search Console Core Web Vitals, New Relic Browser, Datadog RUM.

**Cheat-sheet:**
- Client-side pipeline is architecturally separate from server-side: client SDK → batched HTTP POST to an ingestion endpoint → same pipeline (queue → aggregator → TSDB) from there on.
- The invisible-failure case (packet never leaves the device) can only be caught by watching for **drop-offs in expected client heartbeats**, not by anything server logs can show.

---

## 15. Logs vs. Metrics vs. Traces (the three pillars)

| | Metrics | Logs | Traces |
|---|---|---|---|
| **Shape** | Numeric time series | Unstructured/structured text events | Causally-linked spans across services |
| **Cardinality** | Low (aggregatable) | High (every event) | High (per-request) |
| **Cost** | Cheap to store long-term | Expensive at scale, needs retention limits | Expensive, usually sampled |
| **Answers** | "Is something wrong, and how bad?" | "What exactly happened at 3:02:17am?" | "Which of these 12 microservices added the latency?" |
| **Tools** | Prometheus, CloudWatch, Datadog | ELK/OpenSearch, Splunk, Loki | Jaeger, Zipkin, OpenTelemetry, AWS X-Ray |

Pick the right pillar for the question actually being asked — this is the
#1 place candidates lose points by forcing metrics to answer a tracing
question:

```mermaid
flowchart TD
    Start{"What do you need to know?"} --> A["Is something wrong,<br/>and how bad?"]
    Start --> B["What exactly happened<br/>at a specific moment?"]
    Start --> C["Which hop in a multi-service<br/>call added the latency?"]
    A --> Metrics["Use Metrics"]
    B --> Logs["Use Logs"]
    C --> Traces["Use Traces"]
```

**Cheat-sheet:**
- If the interviewer pushes into "how do you find *why* a specific slow request was slow across 10 microservices" — that's distributed tracing, not metrics. Know the difference and pivot correctly instead of forcing metrics to answer a tracing question.
- OpenTelemetry is the current industry-standard instrumentation layer that emits all three (metrics, logs, traces) — worth name-dropping as the modern unification point.

---

## 16. Real-world systems to cite

| Company | System | Notable design choice |
|---|---|---|
| Google | Borgmon → Monarch | Hierarchical, multi-tenant time-series monitoring; Monarch trades some query flexibility for massive scale |
| Facebook/Meta | ODS + Gorilla + Scuba | Gorilla TSDB for in-memory time series; Scuba for ad-hoc real-time analytics on structured events |
| Amazon | CloudWatch | Push-based, deeply integrated with every AWS service, per-account/region isolation |
| Uber | M3 (M3DB + M3 Aggregator) | Built for horizontal scalability of Prometheus-compatible metrics at Uber's scale |
| CNCF/Kubernetes ecosystem | Prometheus + Grafana + Alertmanager | De facto open-source standard; pull-based scraping, PromQL, became the template most interviewers expect |
| Netflix | Atlas | In-memory dimensional time-series DB, built for very high cardinality |

---

## 17. How to identify this topic in an interview

Signals that the interviewer wants a monitoring-system design (not just a
mention):
- "How would you know if your service is degrading before customers complain?"
- "Design a system to track metrics/logs/alerts across thousands of servers."
- "How do you detect and alert on failures in a distributed system?"
- Follow-ups on any other system design ("Design YouTube") asking "how would you monitor this in production?"

Common trap: candidates jump straight to "I'd use Prometheus and Grafana"
without explaining **why** those tools embody the right trade-offs (pull
model, PromQL for aggregation, Alertmanager for dedup). Naming tools is
fine, but always justify with the underlying design decision.

---

## Master Cheat Sheet

**🆕 Requirements in one breath:** collect → store → query → visualize → alert, at fleet scale, without the monitoring itself becoming a bottleneck or a single point of failure.

**🆕 Capacity math (illustrative):** `hosts × metrics/host ÷ scrape_interval = points/sec`. Example: 100K hosts × 1,000 metrics ÷ 15s ≈ 6.7M points/sec; same fleet ≈ 100M active series.

**🆕 Data model mnemonic:** "Name + Labels = Series." Cardinality multiplies across labels (10 × 5 × 2 = 100 series), it doesn't add.

**🆕 API surfaces:** expose (`GET /metrics` for pull), ingest (`POST /push` for push/gateway), query (`GET /query_range` — the one both dashboards and alert rules call).

**Two failure domains:** server-side (5xx, always visible) vs. client-side (4xx or fully invisible — request never arrived).

**Metric types:** counter (monotonic, use `rate()`), gauge (point-in-time), histogram (aggregatable buckets — prefer this), summary (client-side quantiles, NOT aggregatable across hosts).

**RED method** (services): Rate, Errors, Duration. **USE method** (resources): Utilization, Saturation, Errors.

**Push vs pull:**
- Pull (Prometheus): monitoring system controls cadence, self-throttling, needs service discovery, bad for ephemeral jobs (use a Pushgateway).
- Push (StatsD/CloudWatch): server controls cadence, firewall-friendly, risk of thundering herd on the collector.

**TSDB compression trick:** Facebook Gorilla — delta-of-delta timestamps + XOR'd float values, ~12x compression.

**Architecture pipeline:** exporters/agents → collector/scraper (sharded, service discovery) → ingestion buffer → aggregator/downsampler → TSDB → query engine → {dashboard, alerting engine} → notification fan-out (dedup/group/escalate).

**🆕 Architecture evolution:** v1 single-node scraper+TSDB → v2 sharded scrapers + remote-write into a horizontally-scaled TSDB → v3 adds a dedicated downsampling pipeline and a separately-shardable rule-evaluation tier, so dashboard load never delays a page.

**Cardinality explosion** is the #1 way to accidentally kill a monitoring system — never put unbounded-cardinality fields (user_id, request_id) directly into metric labels. If a label's values are unbounded or user-controlled, reject/strip it at ingestion or bucket it instead.

**🆕 Retention tiers mnemonic:** Raw → Rolled → Really-old (archive). Each tier costs roughly an order of magnitude less than the one before it.

**🆕 Query performance over big ranges:** recording rules (pre-aggregate), automatic resolution selection (don't return more points than pixels), sharded fan-out + merge, short-lived result caching.

**🆕 Rule evaluation at scale:** an alert rule is just a query on a timer; scale by sharding rule groups across evaluator workers, not by re-sharding metrics.

**SLI/SLO/SLA:** SLI = measured value, SLO = internal target, SLA = external contractual commitment (looser than SLO). Error budget = `1 - SLO`; alert on burn rate, not raw threshold.

**Alert on symptoms (user-facing), not causes (CPU%)** — causes belong on dashboards, not pages.

**Three pillars:** metrics (is it broken + how bad), logs (what exactly happened), traces (which hop in the call chain added the latency). OpenTelemetry unifies instrumentation for all three.

**Downtime costs to quote:** Meta Oct 2021 ≈ $13M/hour; AWS Dec 2021 ≈ $66,240/minute (root cause: automated capacity-scaling job → connection storm → network congestion, a feedback-loop failure, not hardware).

**Client-side monitoring:** RUM SDK, local buffer + batch/beacon upload, sampling (100% errors, 1% success), heartbeats to catch "never reached us" failures that server logs can never show.

**Real systems to namedrop:** Prometheus + Grafana + Alertmanager (open source standard), Google Monarch/Borgmon, Facebook Gorilla/ODS/Scuba, AWS CloudWatch, Uber M3, Netflix Atlas, Sentry/Datadog RUM (client-side).
