# High Level Design: Code Execution Engine

This is one of the most satisfying system design problems you'll encounter — it sits at the intersection of distributed systems, OS-level security, real-time communication, and scalability. Let me walk you through everything an interviewer would want to hear, starting from first principles.

---

## 1. Clarifying Questions (Say These Out Loud First)

Always open with clarifying questions. This signals maturity and prevents wasted effort.

"Before I dive in, I have a few questions. Are we designing more like LeetCode (isolated single-file execution with a known expected output) or Replit (a persistent, collaborative environment with a filesystem, a shell, and long-running processes)? Are we supporting multiple languages? What's our expected scale — hundreds of concurrent users or millions? And do we have real-time output streaming, or is it submit-and-wait?"

For this design, assume: multi-language support (Python, JavaScript, Java, C++), LeetCode-style isolated execution, real-time output streaming, and 1 million daily active users.

---

## 2. Functional Requirements

**Code Submission** — users submit code in a chosen language. The system queues it and returns a submission ID immediately, so the client is never blocked waiting for execution to finish.

**Multi-language Execution** — the engine supports multiple runtimes (Python 3.x, Node.js, Java JVM, GCC for C++). Each language needs its own sandbox profile because security policies differ — Python's `import os` is dangerous, C's `malloc` is not.

**Real-time Output Streaming** — stdout, stderr, and resource usage metrics stream back to the client in real time. Users shouldn't stare at a spinner for 5 seconds.

**Resource Enforcement** — every submission is bound by a CPU time limit (e.g., 2 seconds), memory limit (e.g., 256 MB), and output size limit (e.g., 1 MB). These are the "judge" constraints for competitive programming.

**Test Case Evaluation** — code runs against multiple hidden test cases. The system compares actual output vs. expected output and returns a verdict: Accepted, Wrong Answer, Time Limit Exceeded, Memory Limit Exceeded, Runtime Error, or Compilation Error.

**Submission History** — users can view past submissions with verdicts, execution time, and memory used.

---

## 3. Non-Functional Requirements (Go Deep Here — This Is What Impresses)

**Security** is the single most critical NFR. Arbitrary user code can delete files, fork-bomb the system, call `sys.exit()`, open network connections, or read `/etc/passwd`. If you don't sandbox properly, your infrastructure becomes a free crypto-mining cluster. The interviewer will almost certainly probe this.

**Low Latency** — p50 execution round-trip should be under 3 seconds, and p99 under 10 seconds. Users abandon after 15 seconds.

**High Throughput** — during a contest, you might have 10,000 simultaneous submissions in a 60-second window. The system must queue, not drop.

**Isolation** — one user's code must not observe, affect, or share resources with another user's code. This requires process-level isolation at minimum, container-level isolation preferably.

**Reliability** — verdicts must be deterministic. Running the same correct solution twice should always yield Accepted. This means your execution environment must be hermetic and reproducible — no shared state between runs.

**Scalability** — the execution tier needs to scale horizontally. Adding more worker nodes should linearly increase throughput.

**Fairness** — no single user should be able to monopolize the queue. Rate limiting and per-user job caps are essential.

---

## 4. Capacity Estimation

Walk through the math out loud — interviewers love this.

With 1M DAU, assume an average of 10 submissions per day, giving us 10M submissions per day. That's roughly **115 submissions per second** on average. But traffic is bursty — during a contest, peak load can be 10–20× average, so design for **~2,000 peak submissions/second**.

Each submission execution takes roughly 2–5 seconds of wall-clock time with sandboxing overhead. To support 2,000 concurrent executions at 3s each, we need ~6,000 execution slots across all workers. If each worker handles 4 concurrent executions (to avoid CPU contention), we need **~1,500 worker nodes** at peak.

Storage: each submission stores the code (~5 KB), verdict, and metadata. At 10M submissions/day for 3 years → ~100B rows or about **1.5 TB** of structured data, comfortably manageable.

---

## 5. High-Level Architecture

Here's the bird's-eye view of the system:---

## 6. The Execution Pipeline (Request Lifecycle)

This is the critical flow — trace every submission from click to verdict.---

## 7. The Sandbox: The Heart of the System

This is where most candidates get vague. Go deep here and you will stand out.

The fundamental problem is that you need to run completely untrusted code on your own hardware. There are four layers of isolation you must apply, and they work together:

**Layer 1 — Linux Namespaces** give each process its own view of the OS. You create a new PID namespace (so the process can't see other processes), a new network namespace (so it can't make outbound connections), a new mount namespace (so it only sees a read-only filesystem with the necessary runtime), and a new user namespace (so even root inside the container is unprivileged outside it).

**Layer 2 — cgroups (Control Groups)** enforce hard resource limits at the kernel level. You cap CPU shares, memory (e.g., 256 MB), the number of open file descriptors, and the number of processes the container can fork. Without the process count limit, a single `while True: os.fork()` will freeze your machine.

**Layer 3 — seccomp-BPF** is a system call filter. You define an allow-list of the ~30 syscalls a typical program needs (read, write, exit, mmap, etc.) and block everything else. `execve`, `ptrace`, `socket`, `clone` with network flags — all blocked. If the process tries a blocked syscall, the kernel sends it SIGKILL instantly.

**Layer 4 — gVisor or Firecracker** adds a second kernel between the container and the host kernel. gVisor (Google's sandbox) intercepts all syscalls from the guest and handles them in a user-space kernel called Sentry, written in Go. Firecracker (Amazon's microVM) goes further — it runs a full lightweight Linux kernel in a KVM virtual machine in under 125ms. The tradeoff is cold-start time vs. security depth.

---

## 8. The Worker Node Internals

Each worker node is the execution engine itself. Here's what happens inside it when a job arrives.

The worker pulls a job from Kafka and immediately downloads the user's code from S3 using the stored S3 key. It then selects the appropriate Docker base image for the language — a pre-warmed Python 3.11 image, a Node 20 image, etc. These images are pre-pulled on every worker at startup, so there's no cold pull latency during execution.

The worker then runs the code against each test case in a loop. For each test case, it pipes the test input to stdin, captures stdout and stderr, and measures wall-clock time and peak RSS memory using `/usr/bin/time -v` or `getrusage`. After the process exits (or is killed), it compares the actual stdout against the expected output — typically with a whitespace-normalized diff, not a byte-by-byte comparison, to handle trailing newlines.

The verdict decision tree: if compilation fails → Compilation Error. If the process is killed by SIGKILL at the time limit → TLE. If it's killed by the OOM killer or a cgroup memory.limit event → MLE. If it exits with a non-zero code → Runtime Error. If stdout doesn't match expected → Wrong Answer. If all test cases pass → Accepted.

---

## 9. Real-time Output Streaming

This is a differentiator between LeetCode (batch verdict) and Replit (live terminal). Since you promised streaming output in the requirements, the architecture needs a real-time path.

The flow: the worker writes each line of stdout to Redis using `XADD` on a stream keyed by `submission:{id}:stdout`. The WebSocket server (a separate service, horizontally scalable) subscribes to that stream via `XREAD` with blocking reads and pushes chunks to the client's open WebSocket connection. When the job is complete, the worker sends a terminal message (`{type: "done", verdict: "Accepted", time_ms: 342, memory_kb: 18400}`) on the same stream.

The WebSocket server is stateless — any instance can serve any client, because all state lives in Redis. This is critical for horizontal scalability. You could alternatively use Server-Sent Events (SSE) over HTTP/2 for clients that prefer it.

---

## 10. Database Schema

```sql
-- Core submissions table
CREATE TABLE submissions (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID NOT NULL,
  problem_id   UUID NOT NULL,
  language     VARCHAR(20) NOT NULL,       -- 'python3', 'cpp17', 'java21'
  code_s3_key  TEXT NOT NULL,              -- Never store code in RDBMS
  status       VARCHAR(30) NOT NULL,       -- 'pending','running','accepted','tle'...
  verdict      JSONB,                      -- {test_cases: [{input, expected, actual, passed}]}
  time_ms      INTEGER,
  memory_kb    INTEGER,
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  finished_at  TIMESTAMPTZ
);

-- Indexed for leaderboard and history queries
CREATE INDEX idx_submissions_user    ON submissions(user_id, created_at DESC);
CREATE INDEX idx_submissions_problem ON submissions(problem_id, status);
```

Notice that the actual code is stored in S3, not in the database. Code blobs belong in object storage — they're immutable, can be large, and are read infrequently after submission. Only the S3 key lives in Postgres.

---

## 11. Scaling Strategy: The Worker Fleet

The worker fleet needs autoscaling tied to queue depth, not CPU utilization. A queue backlog means users are waiting — even if workers are at 40% CPU (because they're waiting for compilation), you still need more of them.

The autoscaler reads the Kafka consumer group lag (backlog length) every 10 seconds. If lag > 500 and rising, it provisions new workers via the cloud provider's APIs (EC2 `RunInstances`, Kubernetes HPA, or Fargate tasks). Workers are pre-warmed — when they start, they pull all Docker images before accepting jobs, which takes ~30 seconds. This warm-up latency is why you want to scale out proactively, before the queue is already overflowing.

Workers are stateless and disposable. When a worker receives SIGTERM during scale-in, it finishes its current job, nacks any jobs it hasn't started, and exits cleanly. The nacked jobs go back to Kafka for another worker to pick up.

---

## 12. Key Design Decisions (Talk Through Trade-offs)

**Queue-based vs. synchronous execution** — the async queue design means the API returns immediately and the client polls via WebSocket. The alternative (sync HTTP, wait for result) fails at scale because it holds HTTP connections open for seconds, exhausting connection pools quickly. The queue approach is more complex but far more resilient.

**Container-per-job vs. language daemon** — you could run a long-lived Python process per worker that accepts code over a pipe (like a REPL server), avoiding container spin-up overhead. LeetCode reportedly uses this. The trade-off is that state can leak between submissions — a previous submission could set a global variable or import a module that pollutes the next run. Container-per-job is safer but adds ~50–200ms overhead.

**gVisor vs. bare seccomp** — gVisor is significantly more secure (the attack surface of the Sentry kernel is much smaller than the Linux kernel) but adds ~10–30% runtime overhead. For a competitive programming judge where the time limit is 2 seconds, this overhead matters and must be accounted for in the limits. You might expose this as a "judge overhead factor" you calibrate per language.

**Kafka vs. SQS** — Kafka gives you partitioning by user_id (fairness), replay capability, and very high throughput. SQS is simpler to operate but doesn't give you partition-key-based ordering. For this system, Kafka's partitioning is worth the operational cost because it's the primary fairness mechanism.

---

## 13. Advanced Topics to Impress the Interviewer

**Contest mode** deserves a separate design. During a contest, you expect 50,000 submissions in 2 hours from a fixed set of problems. You can pre-warm test case data in memory on workers (not fetching from S3 per run), use a priority queue partition to give contest submissions lower latency, and provision the worker fleet ahead of time rather than relying on autoscaling lag.

**Cheating detection** is a real concern at scale. Identical code submitted by different users within a short window (especially for a contest problem) is a signal of plagiarism. You can hash and compare submission code using Jaccard similarity on token streams, flagging suspicious pairs for human review. This is a background batch job, not in the hot path.

**Multi-region deployment** reduces latency for global users and improves availability. Each region runs its own worker fleet and queue. Submissions are routed to the nearest region. Verdict data is replicated to a global primary database (Postgres with logical replication, or CockroachDB for true multi-region). The key insight is that execution is stateless — workers don't care which region they're in.

**Warm container pools** can dramatically reduce p99 latency. Instead of spinning up a fresh container for each job, you maintain a pool of pre-warmed containers per language. When a job arrives, you grab one from the pool, inject the code, run it, then destroy it and spin up a fresh one for the pool. This cuts cold-start overhead from 200ms to ~10ms, dramatically improving the experience for the first test case.

**The judge's reliability** is often overlooked. The same code run on two different machines should produce identical verdicts. This means the execution environment must be hermetic — same Docker image digest, same kernel version, same clock resolution. Time limits need a calibration factor because a fast machine will run code faster than a slow one, potentially changing a borderline TLE to AC. Some systems measure CPU cycles rather than wall-clock time for this reason.

---

## Summary: What Impresses Interviewers

The candidates who land the offer go beyond drawing boxes. They explain *why* each component exists, what breaks without it, what the trade-offs are, and how they'd handle the failure modes. Here are the things to make sure you hit:

Security is the highest-signal topic here — know the four layers of sandboxing cold. Fairness via Kafka partition-by-user is a subtle point most candidates miss. Explaining that you store code in S3, not Postgres, signals storage maturity. Discussing warm container pools shows you've thought about p99 latency, not just p50. And bringing up contest mode, cheating detection, or multi-region deployment shows you can think about the product holistically, not just the technical plumbing.

---


## 5. High-Level Architecture

Think of the system as five horizontal tiers, where a request flows downward through each layer and the result bubbles back up.

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT TIER                          │
│          Browser / Mobile  ←──WebSocket──→  CDN             │
└───────────────────────────────┬─────────────────────────────┘
                                │ HTTPS
┌───────────────────────────────▼─────────────────────────────┐
│                        API LAYER                            │
│    API Gateway → Auth Service → Submission Service          │
└───────────────────────────────┬─────────────────────────────┘
                                │ enqueue
┌───────────────────────────────▼─────────────────────────────┐
│                    MESSAGE QUEUE (Kafka)                     │
│    submission-topic  │  result-topic  │  dead-letter-topic   │
└───────────────────────────────┬─────────────────────────────┘
                                │ consume
┌───────────────────────────────▼─────────────────────────────┐
│                   EXECUTION TIER (auto-scales)              │
│   Orchestrator → Worker A  │  Worker B  │  Worker N...      │
│                   [sandbox]    [sandbox]    [sandbox]        │
└──────────┬────────────────────────────────────┬─────────────┘
           │ write verdict                       │ stream output
┌──────────▼──────────────────────────────────────────────────┐
│                      STORAGE TIER                           │
│   PostgreSQL (verdicts)  │  Redis (cache + pub/sub)         │
│   S3 (code + test cases) │  Time-series DB (metrics)        │
└─────────────────────────────────────────────────────────────┘
```

The key architectural principle here is that **the client never blocks waiting for execution**. The moment a submission hits the API layer, the server hands back a `submission_id` and says "I've got it." From that point, the real work happens asynchronously in the execution tier, and results flow back up through Redis pub/sub to the WebSocket connection the client is holding open.

The reason the queue sits in the middle — rather than the API calling workers directly — is resilience and fairness. If a burst of 10,000 contest submissions arrives at once, the queue absorbs the spike. Workers consume at their own pace. Nobody gets dropped.

---

## 6. The Execution Pipeline (Request Lifecycle)

Here's the full journey of a single submission, step by step. Think of it as a baton race — each component does its part and hands off cleanly.

```
User clicks "Submit"
        │
        ▼
┌───────────────────┐
│  1. API Gateway   │  ── Rate limit check (token bucket per user)
│                   │  ── Validate: language known? code < 64KB?
└────────┬──────────┘
         │ passes validation
         ▼
┌───────────────────┐
│ 2. Submission     │  ── Upload code to S3, store S3 key
│    Service        │  ── Write row to DB: status = "pending"
│                   │  ── Return { submission_id } to client ◄── client opens WebSocket
└────────┬──────────┘
         │ publish message
         ▼
┌───────────────────┐
│  3. Kafka Queue   │  ── Message keyed by user_id (one user can't flood the queue)
│  (submission-q)   │  ── Contest jobs go to priority partition
└────────┬──────────┘
         │ worker pulls job
         ▼
┌───────────────────┐
│ 4. Orchestrator   │  ── Picks the right worker (language affinity helps cache warm images)
│                   │  ── Fetches code from S3
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  5. Sandbox       │  ── Spin up isolated container (namespaces + cgroups + seccomp)
│     Worker        │  ── Compile (if compiled language)
│                   │  ── Run against test case 1, 2, 3... N
│                   │      ├─ Pipe test input → stdin
│                   │      ├─ Capture stdout, stderr
│                   │      └─ Monitor: CPU time, peak memory
└────────┬──────────┘
         │
         ▼
┌───────────────────┐      Verdict logic:
│   6. Judge        │  ──  compile failed?       → Compilation Error
│                   │  ──  killed by timer?       → Time Limit Exceeded
│                   │  ──  killed by OOM?         → Memory Limit Exceeded
│                   │  ──  non-zero exit?         → Runtime Error
│                   │  ──  stdout ≠ expected?     → Wrong Answer
│                   │  ──  all test cases pass?   → Accepted ✓
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  7. Publish       │  ── Write final verdict to PostgreSQL
│   & Notify        │  ── Publish to Redis stream: submission:{id}
└────────┬──────────┘
         │ Redis pub/sub
         ▼
┌───────────────────┐
│ 8. WebSocket      │  ── Picks up the Redis event
│    Server         │  ── Pushes verdict + stats to waiting client
└───────────────────┘
         │
         ▼
    User sees result
```

A few things worth pausing on here. Notice that in **step 2**, the client immediately gets back a `submission_id` and opens a WebSocket — it doesn't wait. This is called the *async acknowledge* pattern, and it's what makes the system feel fast even when the actual execution takes 3–4 seconds.

In **step 5**, each test case is run in isolation inside the sandbox. This matters because a submission that passes test case 1 might crash on test case 3, and you want to report *which* test case failed, not just that something went wrong.

In **step 6**, the judge uses a normalized diff rather than a byte-by-byte comparison. A solution that prints `42\n` and one that prints `42 \n` (trailing space) should both be Accepted — otherwise you're punishing people for irrelevant formatting, not logic.

The **Redis stream** in step 7 is the glue between the execution world and the real-time world. Stdout lines can be pushed to this stream *during* execution (not just at the end), which is how you get live streaming output for Replit-style environments where the user can watch their program's print statements appear one by one.

---
