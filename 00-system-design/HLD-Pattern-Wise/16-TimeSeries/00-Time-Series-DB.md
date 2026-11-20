# **Time Series Databases (TSDBs): What, Why, How, and When?**

A **Time Series Database (TSDB)** is a database specifically designed to store, manage, and query data points whose values are associated with timestamps.

It is optimized for workloads where:

- Data continuously arrives over time.
- New data is written frequently.
- Queries often ask what happened during a particular time range.
- Data needs to be aggregated over time.
- Older data may need to be compressed, downsampled, or deleted.

Let's understand the problem first, then see how TSDBs solve it.

# **1. What is a Time Series Database?**

Imagine you operate an application running on 100 servers.

Every 10 seconds, each server reports:

- CPU utilization
- Memory utilization
- Disk utilization
- Network traffic
- Request count
- Error count

For example:

| Timestamp | Server   | CPU Usage | Memory Usage |
| --------- | -------- | --------- | ------------ |
| 10:00:00  | server-1 | 45%       | 60%          |
| 10:00:10  | server-1 | 48%       | 62%          |
| 10:00:20  | server-1 | 55%       | 63%          |
| 10:00:30  | server-1 | 42%       | 61%          |
| 10:00:00  | server-2 | 30%       | 50%          |
| 10:00:10  | server-2 | 35%       | 51%          |

This is called **time series data**.

Each record is a measurement at a particular point in time.

A TSDB is optimized to store and query these records efficiently.

### **A more formal definition**

A time series is a sequence of data points, generally represented as:

\\[ (timestamp, value) \\]

In practice, a data point often contains additional dimensions:

\\[ (timestamp, metric,\ labels,\ value) \\]

For example:

```
timestamp: 10:00:10
metric: cpu_usage
labels:
    server: server-1
    region: ap-southeast-2
value: 48
```

This tells us that server-1 in the Sydney region had 48% CPU utilization at 10:00:10.

### **Where do we see time series data?**

| Domain                    | Time series data                       |
| ------------------------- | -------------------------------------- |
| Infrastructure monitoring | CPU, memory, disk usage                |
| Application monitoring    | Request count, latency, error rate     |
| IoT                       | Temperature, pressure, humidity        |
| Finance                   | Stock prices, exchange rates           |
| Industrial systems        | Machine vibrations, energy consumption |
| E-commerce                | Orders per minute, revenue per hour    |
| Distributed systems       | Queue depth, throughput, consumer lag  |
| Smart devices             | Heart rate, steps, device activity     |

The important characteristic is that **time is a fundamental part of the data model and the queries**.

# **2. Why do we need a separate Time Series Database?**

You might be thinking:

"Can't I just store this data in PostgreSQL or MySQL?"

Absolutely. You can.

For example, a regular PostgreSQL table can store the same information:

```
CREATE TABLE server_metrics (
    timestamp TIMESTAMP,
    server_id VARCHAR(100),
    cpu_usage DOUBLE PRECISION,
    memory_usage DOUBLE PRECISION
);
```

You can insert millions of records and query them:

```
SELECT AVG(cpu_usage)
FROM server_metrics
WHERE server_id = 'server-1'
  AND timestamp >= NOW() - INTERVAL '1 hour';
```

So why do we need TSDBs?

Because time-series workloads have some characteristics that put pressure on traditional database designs, particularly at high volumes.

Let's understand the problems.

## **Problem 1: Extremely high write volume**

Consider:

- 10,000 servers.
- Each server sends 10 metrics.
- Every 10 seconds.

The number of data points per second is:

\\[ \frac{10,000 \times 10}{10} = 10,000 \\]

That is:

- 10,000 data points/second.
- 864 million data points/day.
- Approximately 315 billion data points/year.

And this is just one monitoring system.

A TSDB is designed around this kind of continuous ingestion workload.

It can optimize its storage and ingestion paths for incoming time-series data rather than supporting every possible transactional operation equally.

## **Problem 2: Queries are heavily time-oriented**

Consider these typical monitoring queries:

**Query 1: CPU usage in the last 15 minutes**

```
SELECT timestamp, cpu_usage
FROM server_metrics
WHERE server_id = 'server-1'
  AND timestamp >= NOW() - INTERVAL '15 minutes'
ORDER BY timestamp;
```

**Query 2: Average CPU usage per minute**

```
SELECT
    date_trunc('minute', timestamp) AS minute,
    AVG(cpu_usage)
FROM server_metrics
WHERE server_id = 'server-1'
  AND timestamp >= NOW() - INTERVAL '1 hour'
GROUP BY minute
ORDER BY minute;
```

**Query 3: Maximum CPU usage per server over the last 24 hours**

```
SELECT server_id, MAX(cpu_usage)
FROM server_metrics
WHERE timestamp >= NOW() - INTERVAL '24 hours'
GROUP BY server_id;
```

Notice the common pattern:

- Filter by time.
- Group by time.
- Aggregate values.
- Retrieve the latest measurements.

TSDBs optimize these access patterns.

## **Problem 3: Data grows continuously**

Unlike many traditional business tables, monitoring data keeps accumulating.

For example:

| Age of data       | Typical requirement       |
| ----------------- | ------------------------- |
| Last 15 minutes   | High-resolution data      |
| Last 24 hours     | Detailed monitoring       |
| Last 30 days      | Historical analysis       |
| Last 1 year       | Trends and reporting      |
| Older than 1 year | May no longer be required |

You may not need every individual CPU measurement forever.

For example:

- Keep 10-second measurements for 7 days.
- Keep 1-minute averages for 30 days.
- Keep 1-hour averages for 1 year.

TSDBs commonly provide retention policies, compression, and downsampling capabilities to manage this growth.

## **Problem 4: Storage efficiency**

Time-series data has a lot of repetition.

Consider:

```
server-1, ap-southeast-2, cpu_usage, 45%, 10:00:00
server-1, ap-southeast-2, cpu_usage, 46%, 10:00:10
server-1, ap-southeast-2, cpu_usage, 47%, 10:00:20
server-1, ap-southeast-2, cpu_usage, 48%, 10:00:30
```

Notice:

- The server is repeated.
- The region is repeated.
- The metric name is repeated.
- Timestamps are sequential.
- Values are relatively similar.

This data has opportunities for compression.

TSDBs use specialized encoding and compression techniques to reduce storage requirements.

For example, instead of storing every timestamp as a complete independent value, a system can encode timestamp differences.

Instead of storing:

```
10:00:00
10:00:10
10:00:20
10:00:30
```

It may store:

```
10:00:00
+10
+10
+10
```

The actual compression algorithms vary by database, but the general idea is to exploit the predictable patterns in time-series data.

# **3. How is a Time Series Database different from a traditional database?**

Let's compare the workload characteristics.

| Characteristic   | Traditional relational DB       | Time Series DB                             |
| ---------------- | ------------------------------- | ------------------------------------------ |
| Primary purpose  | General-purpose data management | Time-oriented data management              |
| Data model       | Rows and columns                | Timestamped measurements                   |
| Write pattern    | Inserts, updates, deletes       | Primarily continuous inserts               |
| Read pattern     | Flexible transactional queries  | Time-range queries and aggregations        |
| Updates          | Common                          | Often less common                          |
| Indexing         | General-purpose indexes         | Time and series/dimension-aware strategies |
| Retention        | Often application-managed       | Frequently built-in                        |
| Compression      | General-purpose                 | Time-series-aware compression              |
| Downsampling     | Usually application-managed     | Often supported natively                   |
| Typical use case | Orders, users, payments         | Metrics, monitoring, IoT                   |

An important distinction:

**A TSDB is not necessarily a replacement for PostgreSQL or MySQL. It is specialized for a particular workload.**

For example:

- Store users, orders, and payments in PostgreSQL.
- Store infrastructure metrics in Prometheus.
- Store large-scale analytical time-series data in ClickHouse.

Different databases can coexist in the same architecture.

# **4. How does a Time Series Database work internally?**

Now let's look at the interesting part.

Suppose we have the following data:

```
10:00:00 -> CPU = 45%
10:00:10 -> CPU = 48%
10:00:20 -> CPU = 55%
10:00:30 -> CPU = 42%
```

How does a TSDB ingest, store, index, and retrieve these measurements?

There is no single implementation shared by all TSDBs. However, many systems use variations of the following techniques.

## **4.1 Data model: Measurement, Timestamp, Tags, and Fields**

A typical time-series data point contains:

**1. Measurement / Metric**

What are we measuring?

```
cpu_usage
```

**2. Timestamp**

When was the measurement taken?

```
2026-11-19T10:00:00Z
```

**3. Tags / Labels**

What dimensions describe the measurement?

```
server = server-1
region = ap-southeast-2
environment = production
```

**4. Fields / Values**

What values were recorded?

```
cpu = 45.5
memory = 60.2
```

Conceptually:

```
Measurement: cpu_usage

Timestamp: 2026-11-19T10:00:00Z

Tags:
    server = server-1
    region = ap-southeast-2

Fields:
    value = 45.5
```

Tags and labels allow you to filter and group the data.

For example:

- CPU usage of server-1.
- CPU usage of all servers in Sydney.
- CPU usage of production servers.
- Average CPU usage across a region.

Different TSDBs use different terminology and data models. Prometheus, for example, models metrics as named time series with labels.

## **4.2 Time-based partitioning**

Imagine you have data for an entire year.

You don't necessarily want every query to scan the entire dataset.

For example, suppose you ask:

"Give me CPU usage for the last 10 minutes."

Scanning a year's worth of data is wasteful.

Instead, TSDBs commonly organize data into time-based partitions, chunks, or blocks.

Conceptually:

```
Time Series Database
│
├── Partition 1: November 01
│      └── Time series data
│
├── Partition 2: November 02
│      └── Time series data
│
├── Partition 3: November 03
│      └── Time series data
│
├── ...
│
└── Partition N: November 19
       └── Time series data
```

Suppose you query:

```
SELECT *
FROM metrics
WHERE timestamp >= '2026-11-19 10:00:00'
  AND timestamp <  '2026-11-19 11:00:00';
```

The database can eliminate unrelated time partitions.

This technique is called **partition pruning**.

Instead of searching all historical data, it focuses on the relevant time range.

Not every TSDB uses daily partitions. Some use smaller or larger time-based blocks depending on their implementation.

## **4.3 Indexing: Finding the right time series**

Suppose you have 1 million servers.

You want:

```
CPU usage
WHERE region = ap-southeast-2
AND server = server-100
AND timestamp is within the last hour
```

The database needs to identify the relevant time series and then retrieve the relevant time range.

An important concept here is **series identity**.

A particular metric and set of labels identify a time series.

For example:

```
cpu_usage{server="server-1", region="sydney"}
```

is one time series.

```
cpu_usage{server="server-2", region="sydney"}
```

is another time series.

The database may maintain indexes or metadata structures that map series identities to their stored data.

It can then locate the relevant chunks or blocks.

### **Important: Cardinality**

Cardinality means the number of distinct time series.

Suppose you have:

```
Metric: http_requests
Labels:
    region
    service
    status_code
```

The number of possible series depends on the distinct combinations of those labels.

Now imagine adding:

```
user_id
request_id
```

If these values are highly variable, they may create millions of distinct time series.

This is called a **high-cardinality problem**.

Why is it bad?

Because the database may need to maintain metadata and indexing structures for every distinct series.

Consequences can include:

- Increased memory usage.
- Larger indexes.
- More expensive ingestion.
- Increased query overhead.

This is one of the most important design considerations when working with Prometheus-style monitoring systems.

**Practical rule:** Avoid using highly unique identifiers such as request IDs, user IDs, and transaction IDs as metric labels.

They are usually better suited to logs or traces.

## **4.4 Write path: How does data get stored?**

A simplified TSDB write path often looks like this:

```
      Application / Agent
               |
               v
        Data Ingestion
               |
               v
       Validation and Parsing
               |
               v
       Write-Ahead Log (WAL)
               |
               v
       In-Memory Buffer
               |
               v
    Immutable Storage Blocks
               |
               v
     Compression / Compaction
```

Let's understand the components.

**Step 1: Ingestion**

Your application or monitoring agent sends a measurement.

```
cpu_usage = 45.5
server = server-1
timestamp = 10:00:00
```

The TSDB validates and processes it.

**Step 2: Write-Ahead Log (WAL)**

Many databases write incoming data to a WAL before acknowledging the write.

Why?

Suppose the database receives a measurement and stores it only in memory.

Then the machine crashes.

That measurement might be lost.

A WAL provides a recovery mechanism.

The database can replay the log after a crash to reconstruct recently accepted writes.

**Step 3: In-memory buffer**

The database may keep recent data in memory to make ingestion efficient.

Instead of repeatedly rewriting large storage structures for every incoming point, it can accumulate records in memory.

**Step 4: Persist to disk**

When the buffer reaches a configured threshold or a time interval expires, data can be written into persistent storage blocks or segments.

**Step 5: Compression and compaction**

The database can organize and compress stored data.

For example, several smaller data blocks can be merged into larger blocks.

This can improve storage efficiency and query performance.

### **Is this related to LSM Trees?**

Yes, conceptually, but not every TSDB uses an LSM tree.

Recall the LSM-tree design:

```
Writes
  |
  v
WAL
  |
  v
Memtable
  |
  v
Immutable SSTables
  |
  v
Compaction
```

Some TSDBs use LSM-like storage engines or components of this architecture.

Others use different designs, such as time-partitioned immutable blocks and columnar storage.

For example:

- InfluxDB storage engines have used different designs across versions.
- Prometheus uses a WAL and in-memory head, then writes immutable time-series blocks and compacts them.
- TimescaleDB builds on PostgreSQL and organizes time-series data into chunks.

The common goal is efficient ingestion and storage of large amounts of timestamped data.

## **4.5 Read path: How does a time-range query work?**

Suppose you execute:

```
SELECT AVG(cpu_usage)
FROM metrics
WHERE server = 'server-1'
  AND timestamp >= NOW() - INTERVAL '1 hour';
```

Conceptually, the database performs the following steps:

```
             SQL / Query
                 |
                 v
           Query Planner
                 |
                 v
        Identify Time Range
                 |
                 v
          Find Relevant Series
                 |
                 v
       Locate Relevant Time Blocks
                 |
                 v
         Read Required Data
                 |
                 v
           Aggregate Values
                 |
                 v
             Return Result
```

Several optimizations can help:

1. **Time partition pruning:** Skip blocks outside the requested time range.
2. **Index lookup:** Locate the relevant series using metric names and dimensions.
3. **Columnar reading:** Read only the columns required by the query, where supported.
4. **Compression:** Reduce the amount of physical data read.
5. **Pre-aggregation:** Reuse previously calculated summaries, where available.

These techniques can make time-range queries much more efficient.

# **5. Important TSDB concepts**

These are particularly important in system design interviews and real-world monitoring architectures.

## **5.1 Retention policies**

Retention defines how long the database keeps data.

For example:

```
10-second resolution:
    Keep for 7 days

1-minute resolution:
    Keep for 30 days

1-hour resolution:
    Keep for 1 year
```

Why?

Because keeping every raw measurement forever is often unnecessary and expensive.

After a retention period, the system can delete expired data automatically.

## **5.2 Downsampling**

Downsampling means reducing the resolution of historical data.

Suppose you collect one CPU measurement every 10 seconds.

Original data:

| Time     | CPU |
| -------- | --- |
| 10:00:00 | 40% |
| 10:00:10 | 50% |
| 10:00:20 | 60% |
| 10:00:30 | 40% |
| 10:00:40 | 50% |
| 10:00:50 | 60% |

You could downsample this into a one-minute aggregate:

| Time window | Average CPU | Min CPU | Max CPU |
| ----------- | ----------- | ------- | ------- |
| 10:00–10:01 | 50%         | 40%     | 60%     |

Instead of keeping six individual data points, you retain a summary.

The actual system might store multiple aggregates, depending on the use case.

For example:

```
Raw data:
10-second resolution

Downsampled data:
1-minute resolution
5-minute resolution
1-hour resolution
```

**Important:** Downsampling loses information. You cannot reconstruct every original measurement from an average.

Therefore, retain raw data for as long as detailed investigation requires it.

## **5.3 Late-arriving and out-of-order data**

Suppose measurements should arrive in this order:

```
10:00:00
10:00:10
10:00:20
10:00:30
```

But because of network delays, the database receives:

```
10:00:00
10:00:20
10:00:10
10:00:30
```

This is called out-of-order or late-arriving data.

Why does it happen?

- Network delays.
- Agent buffering.
- Application downtime.
- Batch uploads.
- Clock synchronization problems.

A TSDB needs a strategy to handle these writes.

Some systems support out-of-order ingestion within a configured window. Others impose restrictions or incur extra work to incorporate late data into immutable blocks.

This is important because time-series data is not always received in timestamp order.

## **5.4 Compression**

Time series often has naturally compressible patterns:

- Sequential timestamps.
- Repeated labels.
- Slowly changing measurements.
- Repeated or similar values.

Databases can use specialized encodings to store these efficiently.

Common techniques include:

- Delta encoding.
- Delta-of-delta encoding.
- Run-length encoding.
- Bit packing.
- Dictionary encoding.
- Gorilla-style compression for timestamps and floating-point values.

The particular techniques vary by implementation.

## **5.5 Continuous aggregation**

Suppose a dashboard constantly requests:

"Give me average CPU usage per minute for the last 30 days."

Calculating that from billions of raw measurements every time may be expensive.

A database can periodically calculate and store aggregates.

Conceptually:

```
Raw measurements
       |
       v
Average per minute
       |
       v
Average per hour
       |
       v
Average per day
```

The dashboard can then query the precomputed summaries instead of processing every raw point.

TimescaleDB, for example, supports continuous aggregates.

# **6. Different types of Time Series Databases**

Not every TSDB serves the same purpose.

Some are designed primarily for monitoring. Others are general-purpose analytical databases with strong time-series capabilities.

Here are some important examples.

## **6.1 Prometheus**

Prometheus is widely used for infrastructure and application monitoring.

Typical use cases:

- CPU and memory monitoring.
- API request rates.
- Application latency.
- Error rates.
- Kubernetes monitoring.
- Alerting.

Example metric:

```
http_requests_total{
    service="payment-service",
    method="POST",
    status="200"
} 1500
```

Prometheus uses a metric name and labels to identify a time series.

It has its own TSDB for storing and querying metrics and provides PromQL for querying them.

Example PromQL:

```
rate(http_requests_total[5m])
```

This calculates a per-second rate over a five-minute window.

**Best suited for:** Infrastructure and application metrics, particularly where the Prometheus monitoring ecosystem is useful.

## **6.2 InfluxDB**

InfluxDB is a database platform focused on time-series workloads.

Typical use cases:

- IoT monitoring.
- Sensor data.
- Industrial systems.
- Infrastructure monitoring.
- Application measurements.

It has specialized time-series data models and query capabilities.

**Best suited for:** Time-series ingestion and analysis in applications built around its ecosystem.

## **6.3 TimescaleDB**

TimescaleDB extends PostgreSQL with time-series functionality.

This is an interesting choice because you retain the familiar PostgreSQL ecosystem while gaining time-series-specific capabilities.

For example, you can create a hypertable:

```
CREATE TABLE metrics (
    time        TIMESTAMPTZ NOT NULL,
    server_id   TEXT NOT NULL,
    cpu_usage   DOUBLE PRECISION
);

SELECT create_hypertable('metrics', 'time');
```

A hypertable automatically organizes the data into time-based chunks.

You can continue using SQL and PostgreSQL features.

**Best suited for:** Applications that need time-series optimization but also benefit from relational database capabilities and SQL.

## **6.4 ClickHouse**

ClickHouse is primarily a column-oriented analytical database, rather than a dedicated monitoring TSDB.

However, it can handle large-scale time-series analytical workloads very well.

For example:

- Billions of events.
- Historical application analytics.
- High-volume observability data.
- Large-scale aggregations.
- Analytical dashboards.

It is particularly useful when you want to scan and aggregate large amounts of data across columns.

**Best suited for:** Large analytical workloads over time-oriented event data.

## **6.5 Quick comparison**

| Database        | Primary strength                | Typical use case                             |
| --------------- | ------------------------------- | -------------------------------------------- |
| Prometheus      | Monitoring and alerting         | Kubernetes, infrastructure                   |
| InfluxDB        | Dedicated time-series storage   | IoT and monitoring                           |
| TimescaleDB     | PostgreSQL + time-series        | Application time-series data                 |
| ClickHouse      | Large-scale analytical queries  | Observability analytics                      |
| VictoriaMetrics | Efficient metrics storage       | Large-scale Prometheus-compatible monitoring |
| OpenTSDB        | Distributed time-series storage | Large-scale monitoring                       |

One important distinction: Prometheus is more than just a database, and ClickHouse is more than a time-series database. They are broader systems that can serve time-series workloads.

# **7. When should you use a Time Series Database?**

This is probably the most important question from an architecture perspective.

Use a TSDB when most of the following characteristics apply:

| Requirement                   | Why a TSDB helps                         |
| ----------------------------- | ---------------------------------------- |
| Continuous high-volume writes | Optimized ingestion                      |
| Data is timestamp-oriented    | Specialized data organization            |
| Frequent time-range queries   | Time partitioning and pruning            |
| Frequent aggregations         | Efficient aggregation and downsampling   |
| Large historical datasets     | Compression and retention                |
| Monitoring and alerting       | Metric-specific data models and tooling  |
| IoT or sensor workloads       | Natural fit for timestamped measurements |

### **Example 1: Monitoring a microservices application**

Imagine you have:

```
API Gateway
    |
    ├── User Service
    ├── Payment Service
    ├── Order Service
    └── Inventory Service
```

You want to monitor:

- Requests per second.
- Average response time.
- 95th percentile latency.
- Error rate.
- JVM heap usage.
- Thread count.
- Database connection pool usage.

Your architecture might look like this:

```
          Microservices
                |
                v
       Metrics Instrumentation
                |
                v
          Metrics Exporter
                |
                v
           Prometheus
                |
           PromQL / Rules
                |
          +-----+------+
          |            |
          v            v
       Grafana       Alerting
```

Prometheus collects metrics, Grafana visualizes them, and alerting rules notify you when conditions are violated.

This is a classic use case for a TSDB-based monitoring system.

### **Example 2: IoT platform**

Suppose you have 100,000 industrial sensors.

Each sensor sends its temperature every 5 seconds.

```
Temperature Sensors
         |
         v
      MQTT Broker
         |
         v
     Ingestion Service
         |
         v
      Time Series DB
         |
         +----------------+
         |                |
         v                v
    Real-time       Historical
   Monitoring        Analytics
```

You might ask:

- Which machines exceeded 80°C today?
- What was the average temperature during the last hour?
- Which machines are showing increasing temperatures?
- What was the temperature trend over the last 30 days?

Time-series databases are a natural fit here.

### **Example 3: Stock market data**

You could store:

```
timestamp
symbol
open_price
high_price
low_price
close_price
volume
```

You might query:

- Stock price movement in the last hour.
- Daily average price.
- Highest price during a trading session.
- Volume trends over a period.

A TSDB or analytical database can support these workloads, depending on scale, latency, and query requirements.

# **8. When should you NOT use a TSDB?**

A TSDB is not automatically the best choice just because your table has a timestamp column.

Consider these examples.

| Use case                    | Better starting point                 | Reason                                 |
| --------------------------- | ------------------------------------- | -------------------------------------- |
| User account management     | PostgreSQL / MySQL                    | Relational data and transactions       |
| Orders and payments         | PostgreSQL / MySQL                    | ACID transactions and updates          |
| Product catalog             | PostgreSQL / MySQL                    | Relationships and flexible queries     |
| Infrastructure metrics      | Prometheus or another TSDB            | Continuous metric ingestion            |
| Large-scale event analytics | ClickHouse or an analytical warehouse | Large scans and aggregations           |
| Transaction logs            | Log storage platform                  | Text search and log-specific workflows |
| Distributed tracing         | Trace backend                         | Trace and span relationships           |

You can still store time-series data in PostgreSQL.

In fact, if you have:

- Moderate ingestion volume.
- A relatively small dataset.
- Existing PostgreSQL infrastructure.
- A need to join time-series data with business entities.

Then PostgreSQL, possibly with TimescaleDB, might be a perfectly reasonable choice.

Don't introduce another database without a concrete reason.

# **9. Time Series DB vs OLTP vs OLAP**

This distinction is useful in system design.

| Dimension        | OLTP                          | OLAP                              | TSDB                                    |
| ---------------- | ----------------------------- | --------------------------------- | --------------------------------------- |
| Main purpose     | Transaction processing        | Analytical processing             | Time-oriented data                      |
| Typical workload | Read/write individual records | Scan and aggregate large datasets | Ingest and query measurements over time |
| Updates          | Common                        | Less frequent                     | Often append-oriented                   |
| Query examples   | Get customer by ID            | Revenue by region                 | Average CPU over last hour              |
| Data volume      | Varies                        | Often very large                  | Often continuously growing              |
| Common examples  | PostgreSQL, MySQL             | Snowflake, BigQuery, ClickHouse   | Prometheus, InfluxDB, TimescaleDB       |

One subtle point: these categories are not mutually exclusive.

For example:

- TimescaleDB uses PostgreSQL, an OLTP-oriented relational database, and adds time-series capabilities.
- ClickHouse is an OLAP database that also handles time-series analytics.
- Prometheus is a monitoring system with a specialized time-series storage engine.

A database can support more than one kind of workload.

# **10. How to think about TSDB in a real system design interview**

Suppose an interviewer asks:

**"Design a monitoring system for 100,000 servers."**

You need to think about the following questions.

### **Step 1: Estimate ingestion volume**

Assume:

- 100,000 servers.
- 20 metrics per server.
- One measurement every 10 seconds.

Data points per second:

\\[ \frac{100,000 \times 20}{10} = 200,000 \\]

Data points per day:

\\[ 200,000 \times 86,400 = 17.28\text{ billion} \\]

This is a substantial ingestion workload.

### **Step 2: Choose the storage**

A regular relational database might work for a smaller system.

At this scale, you would evaluate specialized metrics storage such as Prometheus-compatible systems or other TSDBs.

The choice depends on:

- Ingestion throughput.
- Number of distinct time series.
- Query concurrency.
- Retention requirements.
- Availability and replication requirements.
- Operational complexity.

### **Step 3: Think about cardinality**

You need to control the number of distinct series.

Good labels:

```
service
region
environment
status_code
```

Potentially problematic labels:

```
user_id
request_id
transaction_id
```

The latter may generate too many distinct time series.

### **Step 4: Define retention and downsampling**

For example:

```
Raw measurements:
    7 days

1-minute aggregates:
    30 days

1-hour aggregates:
    1 year
```

This controls storage growth while preserving historical trends.

### **Step 5: Design the query layer**

Your system needs to answer questions like:

- What is the current CPU usage?
- What is the average request latency over the last 5 minutes?
- Which services have the highest error rate?
- Which servers exceeded a threshold?
- How has memory usage changed over 30 days?

You should also consider alerting, dashboard performance, and query limits.

# **11. The complete mental model**

Let's consolidate everything.

A time-series database is built around a fundamental observation:

**When your workload consists of continuously arriving timestamped measurements, storing and querying those measurements efficiently requires specialized data organization and operations.**

The overall picture:

```
                 DATA SOURCES
                      |
          +-----------+-----------+
          |           |           |
       Servers      IoT       Applications
          |           |           |
          +-----------+-----------+
                      |
                      v
                INGESTION
                      |
                      v
              TIME SERIES DB
                      |
          +-----------+-----------+
          |           |           |
          v           v           v
       Indexing   Compression  Retention
          |           |           |
          +-----------+-----------+
                      |
                      v
                 QUERY ENGINE
                      |
          +-----------+-----------+
          |           |           |
          v           v           v
       Time-range  Aggregation  Downsampling
        queries
          |           |           |
          +-----------+-----------+
                      |
                      v
              DASHBOARDS / ALERTS
```

The concepts to remember:

| Concept                 | What it solves                                   |
| ----------------------- | ------------------------------------------------ |
| Time-based partitioning | Avoid scanning irrelevant historical data        |
| Indexing                | Find the required time series                    |
| Compression             | Reduce storage requirements                      |
| WAL                     | Recover recently accepted writes after crashes   |
| Retention               | Automatically remove old data                    |
| Downsampling            | Preserve historical trends with lower resolution |
| Cardinality management  | Control the number of distinct time series       |
| Continuous aggregation  | Avoid repeatedly calculating expensive summaries |
| Out-of-order handling   | Accommodate delayed measurements                 |

## **Final takeaway**

Think about database selection like this:

- **PostgreSQL/MySQL:** I need to manage business entities and transactions.
- **Redis:** I need very fast access to frequently used data.
- **Kafka:** I need to transport and distribute event streams.
- **ClickHouse:** I need to perform analytical queries over large datasets.
- **Prometheus/InfluxDB/TimescaleDB:** I need to efficiently store and query timestamped measurements.

A TSDB is particularly useful when **time is central to the data model, ingestion is continuous, and time-range queries and aggregations dominate the workload.**

One final distinction worth remembering: a *time-series database* stores and queries time-series data. A *stream-processing system* such as Flink processes events continuously as they arrive. They solve related but different problems, and real-world architectures often use both.












































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------



















# **Compression Techniques in Time Series Databases (TSDBs)**

Compression is one of the most important design aspects of a Time Series Database.

Why? Because TSDBs often store billions of measurements, and storing every timestamp, label, and value independently can consume enormous amounts of storage.

The interesting thing is that **time-series data has predictable patterns that make it highly compressible.**

For example, consider CPU measurements collected every 10 seconds:

| Timestamp | CPU Usage |
| --------- | --------- |
| 10:00:00  | 45.0      |
| 10:00:10  | 45.1      |
| 10:00:20  | 45.2      |
| 10:00:30  | 45.1      |
| 10:00:40  | 45.1      |
| 10:00:50  | 45.1      |

Notice two things:

1. Timestamps follow a predictable interval of 10 seconds.
2. Values change very little and sometimes remain identical.

Instead of storing all these values independently, we can exploit these patterns.

This is the fundamental idea behind time-series compression.

We'll understand the techniques from basic to advanced, with examples and trade-offs.

# **1. First, understand what we want to compress**

A time-series data point typically looks like this:

```
timestamp: 1000
metric: cpu_usage
labels:
    server: server-1
    region: ap-southeast-2
value: 45.5
```

There are several components that can be compressed independently.

| Component       | Example                      | Compression opportunity |
| --------------- | ---------------------------- | ----------------------- |
| Timestamps      | 1000, 1010, 1020             | Sequential values       |
| Numeric values  | 45.0, 45.1, 45.2             | Small changes           |
| Repeated values | 0, 0, 0, 0                   | Repetition              |
| Labels          | server-1, server-1, server-1 | Repeated strings        |
| Metadata        | cpu_usage, region            | Dictionary encoding     |

An important observation:

**A TSDB doesn't necessarily compress the entire record as one unit. It can use different compression techniques for different components.**

For example:

- Delta-of-delta encoding for timestamps.
- XOR encoding for floating-point values.
- Dictionary encoding for labels.
- General-purpose compression for the resulting blocks.

Let's explore each.

# **2. Delta Encoding**

This is one of the simplest and most important compression techniques.

### **The problem**

Suppose we have these timestamps:

```
1000
1010
1020
1030
1040
1050
```

If we store each timestamp independently, we store six numbers.

But notice something:

Every timestamp increases by 10.

Instead of storing all timestamps, store:

```
First timestamp: 1000

Differences:
10
10
10
10
10
```

Mathematically:

\\[ \Delta_i = T_i - T\_{i-1} \\]

Where:

- \\(T_i\\) is the current timestamp.
- \\(T\_{i-1}\\) is the previous timestamp.
- \\(\Delta_i\\) is the difference.

The original data:

\\[ 1000,1010,1020,1030,1040,1050 \\]

Compressed representation:

\\[ 1000,10,10,10,10,10 \\]

Now we store smaller numbers instead of repeating large timestamp values.

### **Another example**

Consider these CPU values:

```
1000
1001
1002
1003
1004
```

Delta encoded:

```
1000, +1, +1, +1, +1
```

Again, we exploit the fact that values change only slightly.

### **How decompression works**

It's straightforward.

Given:

```
1000, 10, 10, 10, 10
```

Reconstruct:

```
1000
1000 + 10 = 1010
1010 + 10 = 1020
1020 + 10 = 1030
1030 + 10 = 1040
```

### **When is delta encoding useful?**

- Sequential timestamps.
- Increasing counters.
- Measurements with small differences.
- Monotonically increasing IDs.

**Limitation:** If values change drastically between consecutive records, the differences may not be smaller than the original values.

This leads us to a more powerful technique.

# **3. Delta-of-Delta Encoding**

Delta encoding stores differences.

Delta-of-delta encoding stores differences between those differences.

This is particularly useful for timestamps.

### **Understand the problem first**

Suppose a monitoring agent sends CPU metrics every 10 seconds.

Timestamps:

```
1000
1010
1020
1030
1040
1050
```

With delta encoding:

```
1000, 10, 10, 10, 10, 10
```

Notice something interesting:

The deltas themselves are repeated.

Instead of storing the same delta repeatedly, we can store the difference between consecutive deltas.

### **Step 1: Calculate deltas**

Original timestamps:

```
1000, 1010, 1020, 1030, 1040, 1050
```

First-order deltas:

```
10, 10, 10, 10, 10
```

### **Step 2: Calculate delta-of-delta**

Formula:

\\[ \Delta^2_i = \Delta_i - \Delta\_{i-1} \\]

Result:

```
0, 0, 0, 0
```

Our representation becomes:

```
First timestamp: 1000
First delta:     10
Delta-of-deltas: 0, 0, 0, 0
```

Instead of repeatedly storing 10, we store zeros.

Zeros can be represented very efficiently using compact bit encodings.

### **Example with slightly irregular timestamps**

Suppose:

```
1000
1010
1021
1030
1040
```

First-order deltas:

```
10, 11, 9, 10
```

Delta-of-deltas:

```
+1, -2, +1
```

Even with slight irregularities, the second-order differences are small.

### **Why is this so effective for TSDBs?**

Time-series data frequently follows a regular sampling interval.

For example:

- Every 1 second.
- Every 10 seconds.
- Every 1 minute.
- Every 5 minutes.

With regular sampling:

\\[ T_i = T\_{i-1} + \text{constant} \\]

Therefore:

\\[ \Delta^2_i = 0 \\]

This makes delta-of-delta particularly effective for timestamps.

### **Visual understanding**

```
Original timestamps
1000 -> 1010 -> 1020 -> 1030 -> 1040
          |      |      |      |
          v      v      v      v
Deltas
        10     10     10     10
          |      |      |
          v      v      v
Delta-of-deltas
         0      0      0
```

**Key takeaway:** Delta-of-delta is especially useful when timestamps are regularly spaced or have only small variations in their sampling intervals.

# **4. Run-Length Encoding (RLE)**

RLE is used when the same value appears consecutively.

### **The problem**

Imagine an application that reports whether a server is healthy.

For an extended period, the values might be:

```
1, 1, 1, 1, 1, 1, 1, 1
```

Storing eight individual values is unnecessary.

Instead, store:

```
(value = 1, count = 8)
```

This means:

Repeat the value 1 eight times.

Mathematically:

\\[ (1,1,1,1,1,1,1,1) \\]

becomes:

\\[ (1,8) \\]

### **Another example**

Original data:

```
0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 2
```

RLE representation:

```
(0, 4)
(1, 3)
(2, 4)
```

### **Where is this useful?**

Consider:

- Server availability.
- Boolean metrics.
- Device status.
- Repeated sensor measurements.
- Counters that don't change for some periods.
- Sparse data.

For example:

```
10:00:00 -> temperature = 25
10:00:10 -> temperature = 25
10:00:20 -> temperature = 25
10:00:30 -> temperature = 25
```

Instead of storing four copies of 25, RLE can represent the repetition.

### **Limitation**

Consider:

```
10, 20, 30, 40, 50, 60
```

There are no repeated consecutive values.

RLE doesn't help much here and may even increase storage because every value needs a count.

**Key takeaway:** RLE is effective when values repeat consecutively, but not when values change frequently.

# **5. XOR Encoding (Gorilla Compression)**

This is one of the most interesting techniques used in time-series compression, particularly for floating-point measurements.

It is associated with the Gorilla time-series compression approach.

Why do we need it?

Consider these CPU measurements:

```
45.0
45.1
45.2
45.3
45.4
```

Delta encoding works well for integers with small differences.

But what about floating-point values?

Floating-point numbers are stored in binary using the IEEE 754 representation.

For example, a `double` occupies 64 bits:

```
1 bit  -> Sign
11 bits -> Exponent
52 bits -> Fraction
```

Even when two floating-point values are very close, storing both complete 64-bit representations can be wasteful.

### **The key idea**

Instead of storing the entire current floating-point value, XOR its binary representation with the previous value.

\\[ X_i = Bits(V_i) \oplus Bits(V\_{i-1}) \\]

Remember the XOR truth table:

| A | B | A XOR B |
| - | - | ------- |
| 0 | 0 | 0       |
| 0 | 1 | 1       |
| 1 | 0 | 1       |
| 1 | 1 | 0       |

The important property is:

**When two binary representations have identical bits, XOR produces zeros in those positions.**

And similar floating-point values often have a lot of common bits.

### **Step-by-step example**

Suppose we have two floating-point measurements:

```
Previous value = 45.0
Current value  = 45.1
```

Conceptually, their IEEE 754 representations look like:

```
45.0 -> 010000...001
45.1 -> 010000...101
```

The actual binary representations are 64 bits; the simplified illustration above is only meant to show the XOR principle.

Now calculate:

```
Previous:  010000...001
Current:   010000...101
           ----------------
XOR:       000000...100
```

The XOR result contains many zeros because most bits are unchanged.

Instead of storing the complete current 64-bit value, we can store the changed portion along with enough metadata to reconstruct it.

### **How does Gorilla compression take advantage of this?**

A simplified version of its value encoding works like this:

1. Store the first value in full.
2. XOR the next value against the previous value.
3. Count the leading and trailing zero bits in the XOR result.
4. Store only the significant portion, along with metadata describing where those bits belong.
5. Reuse previously stored zero-boundary information when possible.

Consider:

```
V1 = 45.0
V2 = 45.1
V3 = 45.2
V4 = 45.3
```

Conceptually:

```
V1: Store complete 64-bit value.

V2: Store XOR difference from V1.

V3: Store XOR difference from V2.

V4: Store XOR difference from V3.
```

The encoder stores the necessary control information and significant XOR bits, rather than repeatedly storing all 64 bits.

The decoder reverses the operation:

\\[ Bits(V_i) = X_i \oplus Bits(V\_{i-1}) \\]

Then it interprets the reconstructed bits as a floating-point number.

### **Why is XOR useful for time series?**

Measurements such as:

- CPU utilization.
- Memory utilization.
- Temperature.
- Pressure.
- Network utilization.
- Sensor readings.

Often change gradually.

Their binary floating-point representations can have substantial common bit regions.

XOR encoding exploits that similarity.

**Important:** XOR compression is lossless. The original floating-point values can be reconstructed exactly.

# **6. Bit Packing**

Bit packing is another important technique.

The central observation is:

**Why use 64 bits to store a number that needs only 4 bits?**

### **Example**

Suppose a metric contains these integer values:

```
1, 2, 3, 4, 5, 6, 7, 8
```

A conventional 32-bit integer representation uses 32 bits per number.

For eight numbers:

\\[ 8 \times 32 = 256 \text{ bits} \\]

However, all these values fit within 4 bits.

In binary:

| Value | Binary |
| ----- | ------ |
| 1     | 0001   |
| 2     | 0010   |
| 3     | 0011   |
| 4     | 0100   |
| 5     | 0101   |
| 6     | 0110   |
| 7     | 0111   |
| 8     | 1000   |

We can pack them:

```
0001 0010 0011 0100 0101 0110 0111 1000
```

Total:

\\[ 8 \times 4 = 32 \text{ bits} \\]

We reduced the storage requirement from 256 bits to 32 bits, ignoring small block metadata overhead.

### **How does this help TSDBs?**

Consider a metric that stores HTTP response status codes:

```
200, 200, 201, 200, 500, 200, 404
```

These values can be transformed into smaller integer representations or stored using a suitable encoding.

Similarly, counters with small ranges can benefit from bit packing.

### **What about large values?**

Suppose your block contains:

```
1, 2, 3, 4, 5, 1000000
```

The largest value needs 20 bits.

A simple fixed-width encoding might require 20 bits for every value.

Some implementations use:

- Variable-width integer encoding.
- Frame-of-reference encoding.
- Bit-packed blocks.
- Specialized integer encodings.

These help avoid wasting bits on smaller values.

**Key takeaway:** Bit packing is useful when values fit into fewer bits than their native machine representation.

# **7. Dictionary Encoding**

Time-series measurements frequently contain repeated labels and strings.

Consider:

```
server-1
server-1
server-1
server-2
server-2
server-3
server-3
```

Storing the same strings repeatedly is wasteful.

Instead, create a dictionary:

```
Dictionary:

1 -> server-1
2 -> server-2
3 -> server-3
```

Replace the original values with integer references:

```
1, 1, 1, 2, 2, 3, 3
```

Now we store the string dictionary once and use compact integer IDs.

### **How is this useful in TSDBs?**

Consider these labels:

```
region = ap-southeast-2
environment = production
service = payment-service
```

They may appear repeatedly across thousands or millions of measurements.

Dictionary encoding avoids repeatedly storing the same strings.

### **Another example**

Original records:

| Server   | Region | Environment |
| -------- | ------ | ----------- |
| server-1 | Sydney | Production  |
| server-2 | Sydney | Production  |
| server-3 | Sydney | Production  |
| server-4 | Mumbai | Production  |

Dictionaries:

```
Server:
1 -> server-1
2 -> server-2
3 -> server-3
4 -> server-4

Region:
1 -> Sydney
2 -> Mumbai

Environment:
1 -> Production
```

Encoded records:

| Server ID | Region ID | Environment ID |
| --------- | --------- | -------------- |
| 1         | 1         | 1              |
| 2         | 1         | 1              |
| 3         | 1         | 1              |
| 4         | 2         | 1              |

This significantly reduces repeated string storage.

### **Important distinction: Dictionary encoding vs Cardinality**

These two concepts are related, but they solve different problems.

Dictionary encoding reduces repeated string storage.

Cardinality describes the number of distinct time series.

Even if strings are dictionary-encoded efficiently, having millions of unique label combinations can still create substantial index and metadata overhead.

Dictionary encoding does not solve high cardinality.

# **8. General-purpose Block Compression**

So far, we've discussed techniques that exploit specific time-series patterns.

But TSDBs can also apply general-purpose compression algorithms to stored blocks.

Examples include:

- LZ4
- Snappy
- Zstandard (Zstd)
- gzip/DEFLATE

Let's understand the general idea.

Suppose you have already transformed your data using:

- Delta encoding.
- Bit packing.
- Dictionary encoding.
- XOR encoding.

The resulting byte stream may still contain repeated patterns.

A general-purpose compressor looks for additional redundancy.

For example:

```
ABCDABCDABCDABCD
```

A compressor may represent repeated sequences using references to earlier data rather than storing every occurrence.

### **Why combine compression techniques?**

Because each technique solves a different problem.

| Technique           | What it exploits                    |
| ------------------- | ----------------------------------- |
| Delta encoding      | Small differences                   |
| Delta-of-delta      | Predictable timestamp intervals     |
| RLE                 | Consecutive repetitions             |
| XOR                 | Similar floating-point bit patterns |
| Bit packing         | Small integer ranges                |
| Dictionary encoding | Repeated strings and symbols        |
| LZ4 / Zstd          | General repeated byte patterns      |

A TSDB can apply multiple techniques at different stages.

However, not every database uses all of them, and the exact sequence depends on its storage engine.

# **9. Lossless vs Lossy Compression**

This is an extremely important distinction.

There are two broad categories of compression.

## **9.1 Lossless compression**

The original data can be reconstructed exactly.

For example:

Original:

```
45.1, 45.2, 45.3, 45.4
```

Compressed:

```
45.1, +0.1, +0.1, +0.1
```

Decompressed:

```
45.1, 45.2, 45.3, 45.4
```

No information is lost.

Examples:

- Delta encoding.
- Delta-of-delta encoding.
- RLE.
- XOR encoding.
- Dictionary encoding.
- Bit packing.

These can all be lossless when implemented with exact reconstruction.

## **9.2 Lossy compression**

Some information is intentionally discarded to reduce storage.

Consider sensor measurements:

```
25.123
25.124
25.126
25.127
25.128
```

Suppose your application doesn't need three decimal places.

You might store:

```
25.1
25.1
25.1
25.1
25.1
```

This is a form of quantization.

The original measurements cannot be reconstructed exactly.

### **Another important example: Downsampling**

Original measurements, collected every 10 seconds:

```
10:00:00 -> 40
10:00:10 -> 50
10:00:20 -> 60
10:00:30 -> 40
10:00:40 -> 50
10:00:50 -> 60
```

Instead of retaining every measurement, calculate:

```
Average = 50
Minimum = 40
Maximum = 60
```

Store the aggregate for the one-minute window.

This reduces storage, but the original sequence is lost.

Strictly speaking, downsampling is not merely a compression codec. It is a transformation that reduces the resolution of the data.

### **Lossless vs lossy comparison**

| Property          | Lossless                         | Lossy                                   |
| ----------------- | -------------------------------- | --------------------------------------- |
| Original data     | Fully recoverable                | Not fully recoverable                   |
| Data accuracy     | Exact                            | Approximate                             |
| Storage reduction | Depends on data patterns         | Potentially much greater                |
| Suitable for      | Detailed investigation, auditing | Long-term trends, approximate analytics |
| Examples          | XOR, RLE, delta encoding         | Quantization, downsampling              |

**Practical recommendation:** Keep high-resolution raw data for the period needed for debugging and investigation. Use downsampled data for long-term historical trends when approximate summaries are sufficient.

# **10. How do these techniques work together inside a TSDB?**

Now let's combine everything.

Suppose we have the following measurements:

| Timestamp | CPU  | Server   |
| --------- | ---- | -------- |
| 1000      | 45.0 | server-1 |
| 1010      | 45.1 | server-1 |
| 1020      | 45.2 | server-1 |
| 1030      | 45.2 | server-1 |
| 1040      | 45.2 | server-1 |
| 1050      | 45.3 | server-1 |

A TSDB might organize and encode the data into separate streams.

### **Step 1: Compress timestamps**

Original:

```
1000, 1010, 1020, 1030, 1040, 1050
```

Delta-of-delta:

```
First timestamp: 1000
First delta:     10
Delta-of-deltas: 0, 0, 0, 0
```

### **Step 2: Compress CPU values**

Original:

```
45.0, 45.1, 45.2, 45.2, 45.2, 45.3
```

Possible approaches:

- XOR encoding for floating-point values.
- RLE for the repeated 45.2 values.
- Other value-specific encodings.

The appropriate approach depends on the codec and the data distribution.

### **Step 3: Compress labels**

Original:

```
server-1, server-1, server-1, server-1, server-1, server-1
```

Dictionary:

```
1 -> server-1
```

Encoded representation:

```
1, 1, 1, 1, 1, 1
```

### **Step 4: Store the compressed data in blocks**

Conceptually:

```
              TSDB STORAGE BLOCK
                      |
          +-----------+-----------+
          |           |           |
          v           v           v
      Timestamps    Values      Labels
          |           |           |
          v           v           v
       Delta-of-    XOR /       Dictionary
         delta       RLE         encoding
          |           |           |
          +-----------+-----------+
                      |
                      v
             Encoded data blocks
                      |
                      v
            Optional block compression
                      |
                      v
                Persistent Storage
```

This is a conceptual model, not a mandatory implementation pipeline.

The important idea is that each component is encoded using a technique appropriate to its characteristics.

# **11. Why do TSDBs use chunks?**

Another important concept is **chunk-based storage**.

Instead of compressing every measurement independently, TSDBs often group multiple consecutive measurements belonging to the same time series into a chunk.

For example:

```
Time series:
cpu_usage{server="server-1"}

Chunk 1:
    10:00:00 -> 45.0
    10:00:10 -> 45.1
    10:00:20 -> 45.2
    10:00:30 -> 45.3

Chunk 2:
    10:00:40 -> 45.2
    10:00:50 -> 45.1
    10:01:00 -> 45.4
    10:01:10 -> 45.5
```

Why is chunking useful?

**1. Better compression**

The encoder can exploit patterns across multiple measurements rather than treating every measurement independently.

**2. Efficient reads**

Suppose you want measurements between 10:00:00 and 10:00:30.

The database can locate the relevant chunk and decode the required data.

**3. Manageable memory usage**

It doesn't need to keep an entire year's worth of data in memory to encode or decode a small section.

**4. Better storage management**

Chunks or blocks can be written, merged, compacted, and deleted independently.

### **How does chunk size affect compression?**

Generally:

- Smaller chunks: Lower memory requirements and more granular reads, but more metadata and potentially less compression opportunity.
- Larger chunks: More opportunities to identify patterns, but potentially more data to decode and greater memory requirements.

There is no universally optimal chunk size. It depends on the workload and implementation.

# **12. What happens when data is updated or arrives out of order?**

Let's consider an interesting scenario.

Your TSDB has already stored:

```
1000
1010
1020
1030
```

The deltas are:

```
10, 10, 10
```

Now a delayed measurement arrives:

```
1015
```

The correct sequence is:

```
1000
1010
1015
1020
1030
```

The deltas are now:

```
10, 5, 5, 10
```

The existing encoded representation may no longer be suitable as-is.

Some TSDBs handle this by:

- Writing out-of-order samples through a separate ingestion path.
- Buffering late-arriving data.
- Re-encoding affected data during compaction.
- Restricting how far back late data can arrive.

This is one reason why TSDBs often use immutable blocks and compaction instead of constantly modifying compressed historical data in place.

It also connects to the WAL and LSM-like architecture we discussed earlier.

# **13. Compression techniques: Complete comparison**

Here is the summary I would recommend remembering.

| Technique                   | Compresses            | Main idea                                    | Particularly useful when                                    |
| --------------------------- | --------------------- | -------------------------------------------- | ----------------------------------------------------------- |
| Delta Encoding              | Timestamps / integers | Store differences                            | Consecutive values are close                                |
| Delta-of-Delta              | Timestamps            | Store differences between differences        | Sampling intervals are regular                              |
| RLE                         | Repeated values       | Store value + repetition count               | Consecutive values repeat                                   |
| XOR Encoding                | Floating-point values | Store XOR against previous value             | Consecutive floating-point values have similar bit patterns |
| Bit Packing                 | Integers              | Use only the required number of bits         | Values have a small numeric range                           |
| Dictionary Encoding         | Strings / labels      | Store dictionary IDs                         | Many values repeat                                          |
| General-purpose compression | Encoded byte streams  | Identify repeated byte patterns              | Additional redundancy exists                                |
| Quantization                | Numeric values        | Reduce precision                             | Approximate values are acceptable                           |
| Downsampling                | Entire time series    | Store aggregates instead of raw measurements | Historical high-resolution data is unnecessary              |

# **14. Which compression techniques should you understand deeply?**

For system design and backend engineering interviews, I would prioritize them as follows:

| Priority | Technique                        | Importance                                     |
| -------- | -------------------------------- | ---------------------------------------------- |
| 1        | Delta Encoding                   | Fundamental                                    |
| 2        | Delta-of-Delta                   | Very important for timestamps                  |
| 3        | XOR / Gorilla Encoding           | Very important for floating-point measurements |
| 4        | Run-Length Encoding              | Fundamental                                    |
| 5        | Bit Packing                      | Important                                      |
| 6        | Dictionary Encoding              | Important for labels and metadata              |
| 7        | General-purpose compression      | Important                                      |
| 8        | Lossy compression / Downsampling | Important for retention architecture           |

You don't necessarily need to implement every codec from scratch. But you should understand why it exists, what patterns it exploits, and when it is effective.

# **15. A few important design trade-offs**

Compression isn't free. A TSDB must balance several competing requirements.

| Trade-off                           | Explanation                                                         |
| ----------------------------------- | ------------------------------------------------------------------- |
| Compression ratio vs CPU            | Stronger compression can require more processing.                   |
| Compression vs query latency        | Queries may need to decompress blocks before processing them.       |
| Chunk size vs memory                | Larger chunks can require more memory during encoding and decoding. |
| Compression vs ingestion throughput | Encoding every incoming measurement consumes CPU.                   |
| Lossless vs lossy                   | More aggressive reduction may discard information.                  |
| Compression vs out-of-order writes  | Late data can complicate compressed block organization.             |

A particularly important design decision is whether to compress immediately during ingestion or buffer measurements and compress them in batches.

Batching often makes it easier to exploit patterns across multiple measurements, but it introduces buffering and persistence considerations.

# **16. Final mental model**

Think about compression in a TSDB in three layers.

**Layer 1: Exploit time patterns**

```
1000, 1010, 1020, 1030
```

Use:

- Delta encoding.
- Delta-of-delta encoding.

**Layer 2: Exploit value patterns**

```
45.0, 45.1, 45.2, 45.2, 45.2
```

Use:

- XOR encoding.
- Run-length encoding.
- Bit packing.

**Layer 3: Exploit repeated metadata and bytes**

```
server-1, server-1, server-1
```

Use:

- Dictionary encoding.
- General-purpose compression.

And when high-resolution historical data is no longer required:

```
10-second measurements
          |
          v
    1-minute averages
          |
          v
     1-hour averages
```

Use downsampling and retention policies.

**The core principle:**

A TSDB doesn't need to store every measurement as an independent, fully expanded record. It exploits relationships between timestamps, values, labels, and measurements to minimize storage while preserving the information required by its queries.

That's the fundamental reason time-series data can often be compressed much more efficiently than storing every record independently in a conventional row-oriented format.

