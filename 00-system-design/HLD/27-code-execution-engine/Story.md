# Designing a Code Execution Engine 🚀

Love this approach! We'll go full story mode — real problems, real pain, real solutions. Let me start from the very beginning.

---

## Chapter 1: The Problem — "Just Run The Code, How Hard Can It Be?"

---

### 🌅 The Story Begins...

It's 2010. A small startup called **CodeNow** wants to build something like LeetCode. The idea is simple:

> User writes code in the browser → clicks "Run" → sees the output.

The founder, let's call him **Arjun**, thinks this is trivial. He spins up a single Ubuntu server and writes this backend logic:

```python
# Arjun's first attempt (please never do this)
import subprocess

def run_code(user_code: str):
    with open("solution.py", "w") as f:
        f.write(user_code)
    
    result = subprocess.run(["python3", "solution.py"], capture_output=True)
    return result.stdout
```

**Simple. Clean. Deployed by Friday.**

---

### 💥 Monday Morning — The First Disaster

A user submits this as their "solution":

```python
import os
os.system("rm -rf /")  # deletes everything on the server
```

**The entire server is wiped.** CodeNow is down. Arjun is crying.

---

### 🤔 Okay, So What's The REAL Problem Here?

Arjun just discovered the **#1 fundamental challenge** of a code execution engine:

> **You are running untrusted code from strangers on your machine.**

Think about what a malicious (or even accidentally bad) user can do:

| Attack Type | Example | Impact |
|---|---|---|
| **File System Attack** | `rm -rf /` | Destroys server data |
| **Network Attack** | `requests.get("http://internal-db")` | Leaks internal services |
| **Fork Bomb** | `while True: os.fork()` | Crashes server (infinite processes) |
| **Memory Bomb** | `x = " " * 10**12` | OOM kills the server |
| **Infinite Loop** | `while True: pass` | CPU stuck at 100% forever |
| **Privilege Escalation** | `sudo rm /etc/passwd` | Take over the machine |

So the **core design challenge** isn't "how to run code" — it's:

> **"How do you run UNTRUSTED code in a way that is SAFE, FAST, FAIR, and SCALABLE?"**

---

### 🏗️ The 4 Pillars We Need to Design For

Before we dive into solutions, let's anchor our entire design around these 4 pillars. Every decision we make will trace back to one of these:

```
┌─────────────────────────────────────────────────────┐
│              CODE EXECUTION ENGINE                   │
│                                                      │
│   🔒 ISOLATION    — malicious code can't escape     │
│   ⚡ PERFORMANCE  — results back in < 3 seconds     │
│   ⚖️  FAIRNESS    — one user can't starve others    │
│   📈 SCALABILITY  — handle 10k submissions/sec      │
└─────────────────────────────────────────────────────┘
```

---

### ✅ Where We Are

You now understand **why** this problem is hard. It's not a "run code" problem — it's a **security + distributed systems** problem.

**Next up → Chapter 2: Isolation** — How do we actually sandbox code so it can't escape? We'll go through 3 approaches (each with their own tradeoffs) the way engineers historically discovered them.

---

**Does this make sense so far? Ready to go to Chapter 2 — Isolation?** 👇
---
# Chapter 2: Isolation — "How Do We Build a Prison for Code?" 🔒

---

## 🌅 The Story Continues...

Arjun survived Monday. He's now obsessed with one question:

> *"How do I let users run ANY code, without letting that code touch anything it shouldn't?"*

He needs a **sandbox** — an isolated environment where code runs, but cannot escape. Think of it like this:

```
WITHOUT ISOLATION:              WITH ISOLATION:
                                
User Code ──► Your Server       User Code ──► 🏛️ Prison Cell
                  │                               │
              Everything!                  Only what we allow
         (files, network, OS)             (nothing else exists)
```

Arjun goes through **3 evolutionary approaches** — each better than the last.

---

## Approach 1: "Just Restrict the Language" 😬

### The Idea
*"What if we just block dangerous function calls in Python?"*

```python
# Arjun's attempt at a "safe" executor
BANNED = ["os", "subprocess", "sys", "open", "eval", "exec"]

def is_safe(code: str) -> bool:
    for banned in BANNED:
        if banned in code:
            return False
    return True

def run_code(user_code: str):
    if not is_safe(user_code):
        return "Error: Banned keyword detected"
    exec(user_code)  # still using exec 💀
```

### Why It Fails — Bypasses Are Trivial

A clever user bypasses this in seconds:

```python
# bypass 1: encode the string
__import__('o'+'s').system('rm -rf /')

# bypass 2: use builtins indirectly
[].__class__.__base__.__subclasses__()[100]('rm -rf /', shell=True)

# bypass 3: access os through another module
import math
math.__loader__.load_module('os').system('rm -rf /')
```

**Blocklist approach = security theater.** Attackers always find a new path. ❌

---

## Approach 2: Virtual Machines 🖥️

### The Story
Arjun asks his senior engineer, Priya. She says:

> *"Don't fight the language. Isolate at the OS level. Give each submission its own Virtual Machine."*

### What is a VM?

A VM is a **complete fake computer** running inside your real computer. It has its own fake CPU, fake RAM, fake hard drive, fake network card — all simulated.

```
┌─────────────────────────────────────────┐
│           YOUR REAL SERVER              │
│                                         │
│  ┌──────────────┐  ┌──────────────┐    │
│  │     VM 1     │  │     VM 2     │    │
│  │  ┌────────┐  │  │  ┌────────┐  │    │
│  │  │User    │  │  │  │User    │  │    │
│  │  │Code A  │  │  │  │Code B  │  │    │
│  │  └────────┘  │  │  └────────┘  │    │
│  │  Fake OS     │  │  Fake OS     │    │
│  └──────────────┘  └──────────────┘    │
│         Hypervisor (VMware/KVM)         │
│              Real Hardware              │
└─────────────────────────────────────────┘
```

Even if User Code A does `rm -rf /`, it only destroys the **fake OS inside VM 1**. The real server is untouched. ✅

### The Problem — VMs Are HEAVY

| Metric | Reality |
|---|---|
| Boot time | 30–60 seconds |
| Memory per VM | 512MB – 2GB |
| Disk per VM | 2–10 GB |
| Time to spin up for a submission | Way too slow |

If LeetCode gets 1000 submissions/minute, you'd need 1000 VMs. At 1GB RAM each = **1 Terabyte of RAM**. 💸

VMs gave us **perfect isolation** but were **too expensive and too slow**. ❌

---

## Approach 3: Containers 🐳 — The Sweet Spot

### The Story
A new engineer joins the team — Rahul. He just heard about this thing called **Docker**.

> *"What if instead of faking an entire computer, we just fake the boundaries for a single process?"*

### What is a Container?

A container is NOT a fake computer. It's a **real process** on your real OS, but with **walls built around it** using Linux kernel features.

Linux has 3 powerful primitives that make this possible:

---

### Primitive 1: Namespaces 🗂️

Namespaces make a process **think it's alone** on the machine. The process gets its own isolated view of:

```
┌───────────────────────────────────────────┐
│ NAMESPACE TYPE    │ WHAT IT ISOLATES       │
├───────────────────┼────────────────────────┤
│ PID namespace     │ Process IDs            │
│ Network namespace │ Network interfaces     │
│ Mount namespace   │ File system view       │
│ User namespace    │ User/Group IDs         │
│ UTS namespace     │ Hostname               │
│ IPC namespace     │ Inter-process comms    │
└───────────────────────────────────────────┘
```

**Example — PID Namespace:**

Without namespace, a process can see ALL processes on the machine:
```bash
# user code could do this and see your DB, your web server, everything
ps aux
# PID 1: init
# PID 345: postgres (your database!)
# PID 892: nginx (your web server!)
# PID 1203: user_code.py  ← the user's process
```

With a PID namespace, the user's process only sees itself:
```bash
ps aux
# PID 1: user_code.py  ← user thinks they're the only one on the machine
```

The process is **blind** to everything outside its namespace. 🎯

---

### Primitive 2: cgroups (Control Groups) ⚖️

Namespaces handle **visibility**. But what about **resource limits**?

A user can still write a fork bomb and exhaust all CPU even inside a namespace. This is where **cgroups** come in.

cgroups let you say:

```
"This group of processes is ONLY allowed to use:
  - 1 CPU core (max)
  - 256MB of RAM (max)
  - 10MB/s of disk I/O (max)
  - 0 network bandwidth (blocked)"
```

Example of setting a cgroup limit:

```bash
# Create a cgroup for this submission
mkdir /sys/fs/cgroup/memory/submission_42

# Limit it to 256MB RAM
echo 268435456 > /sys/fs/cgroup/memory/submission_42/memory.limit_in_bytes

# If it tries to use more → killed immediately
```

Now our fork bomb is **contained**:
```python
# User tries this evil code
import os
while True:
    os.fork()  # tries to create infinite processes

# cgroup says: "You've hit your process limit (50 processes)"
# → All new fork() calls fail
# → Attack is neutralized ✅
```

---

### Primitive 3: seccomp (Secure Computing) 🚫

Even with namespaces + cgroups, a process can still make **system calls** to the kernel. System calls are how any program talks to the OS.

seccomp lets you create a **whitelist of allowed system calls**. Everything else is blocked.

For a code execution sandbox, we might allow:
```
✅ read()      — read from files
✅ write()     — write output
✅ mmap()      — allocate memory
✅ exit()      — terminate

❌ socket()   — create network connections → BLOCKED
❌ fork()     — create new processes → BLOCKED
❌ mount()    — mount file systems → BLOCKED
❌ ptrace()   — spy on other processes → BLOCKED
```

Now even if an attacker finds a clever Python trick, they still can't escape — because at the **kernel level**, those syscalls are simply rejected.

---

### Putting It All Together: The Container Sandbox

```
┌─────────────────────────────────────────────────────┐
│                  YOUR SERVER                         │
│                                                      │
│  ┌───────────────────────────────────────────────┐  │
│  │           CONTAINER (User Submission)          │  │
│  │                                                │  │
│  │  User Code Runs Here                           │  │
│  │                                                │  │
│  │  🗂️  Namespace:  can't see outside processes  │  │
│  │  ⚖️  cgroups:    can't use too much CPU/RAM   │  │
│  │  🚫 seccomp:    can't make dangerous syscalls │  │
│  └───────────────────────────────────────────────┘  │
│                                                      │
│  Real OS Kernel (Linux)                              │
└─────────────────────────────────────────────────────┘
```

### VM vs Container — The Comparison

```
┌────────────────┬──────────────────┬──────────────────┐
│                │    VM            │   Container      │
├────────────────┼──────────────────┼──────────────────┤
│ Boot time      │  30–60 seconds   │  50–500ms        │
│ Memory         │  512MB – 2GB     │  10–50MB         │
│ Isolation      │  Near perfect    │  Very good       │
│ Shares kernel? │  No (own kernel) │  Yes (risky?)    │
│ Density        │  Low             │  High            │
└────────────────┴──────────────────┴──────────────────┘
```

Containers are **50x lighter** than VMs and start **100x faster**.

For a system that needs to run thousands of submissions per minute, containers win.

---

## But Wait — Container Isolation Isn't Perfect 😬

Here's the catch Rahul discovers: **containers share the host kernel.**

If there's a **kernel vulnerability** (and there are), a malicious user could escape the container and reach the host OS. This is called a **container escape**.

Famous examples:
- **CVE-2019-5736** — runc vulnerability, allowed container escape
- **Dirty COW** — kernel bug that broke container isolation

### The Modern Answer: gVisor & Firecracker 🛡️

For the **highest security** (Google uses this for Google Cloud Run, AWS Lambda uses Firecracker):

**gVisor** — adds a **user-space kernel** between the container and the real kernel. System calls go through gVisor first, which acts as an extra filter.

**Firecracker** — a **microVM** (tiny VM, starts in ~125ms!) that gives you VM-level isolation at near-container speed. Used by AWS Lambda.

```
SECURITY vs SPEED SPECTRUM:

Raw Process ──► Container ──► gVisor ──► Firecracker/MicroVM ──► Full VM
    Fast                                                           Slow
    Unsafe                                                         Safe
```

**LeetCode/competitive coding** → Containers with seccomp (good enough, fast)
**AWS Lambda / production grade** → Firecracker microVMs (best of both worlds)

---

## 📚 Chapter 2 Summary

| Approach | Isolation | Speed | Cost | Used By |
|---|---|---|---|---|
| Keyword blocklist | ❌ Terrible | ✅ Fast | ✅ Cheap | Nobody sane |
| Full VMs | ✅ Perfect | ❌ Slow | ❌ Expensive | Old systems |
| Containers | ✅ Very Good | ✅ Fast | ✅ Cheap | LeetCode, HackerRank |
| Firecracker/gVisor | ✅ Excellent | ✅ Fast | 💛 Medium | AWS Lambda, GCP |

**The core insight:**
> Containers use **Linux kernel features** (namespaces + cgroups + seccomp) to create walls around a process — making it fast AND isolated.

---

**Next up → Chapter 3: Architecture** — Now that we can safely run one piece of code, how do we design the entire system to handle **millions of submissions**? We'll design the request flow, queues, workers, and the API layer end-to-end.

---

# Chapter 3: System Architecture — "From One Server to a Real System" 🏗️

---

## 🌅 The Story Continues...

CodeNow is growing. They've solved isolation with containers. But now Arjun faces a new problem on launch day:

> *"We have 50,000 users. All of them click 'Run' at the same time during a contest. Our single server with a container runner just... dies."*

The server is handling everything — the API, the container spawning, the result storage. It's doing too much. Time to **break it apart**.

---

## The Naive Architecture (What Everyone Starts With)

```
User Browser
     │
     ▼
┌─────────────┐
│  Web Server │  ← handles API + runs containers + stores results
│  (1 server) │    ALL IN ONE
└─────────────┘
```

**Problems:**
- Running a container is **slow (500ms–2s)**. While it's running, the web server is blocked
- One slow submission can delay 100 other users
- No way to scale horizontally — everything is coupled

---

## The Key Insight: Separate Concerns 💡

Arjun's new architect, **Sneha**, draws this on a whiteboard:

> *"Code execution is an async, resource-heavy job. Treat it like a job queue — not a web request."*

This is the **fundamental architectural insight**:

```
WRONG mental model:          RIGHT mental model:

User ──► API ──► Run         User ──► API ──► Enqueue Job
         │                                       │
         │ (waits here,                    Worker picks it up
         │  blocking)                      runs container
         │                                       │
         ▼                              stores result in DB
       Response                                  │
                                        User polls/websocket
                                        gets result
```

This is the **Producer-Consumer pattern**, and it's the backbone of the entire system.

---

## The Full System Architecture

Let me walk you through every component, one by one:

```
                        ┌─────────────────────────────────────────────────┐
                        │                   CLIENTS                        │
                        │         Browser / Mobile / API Client            │
                        └──────────────────┬──────────────────────────────┘
                                           │ HTTP / WebSocket
                        ┌──────────────────▼──────────────────────────────┐
                        │              API GATEWAY                         │
                        │     (Auth, Rate Limiting, Routing)               │
                        └──────────────────┬──────────────────────────────┘
                                           │
               ┌───────────────────────────┼────────────────────────────┐
               │                           │                            │
    ┌──────────▼──────────┐   ┌────────────▼──────────┐   ┌────────────▼───────────┐
    │   Submission        │   │    Problem/User        │   │   Results              │
    │   Service           │   │    Service             │   │   Service              │
    └──────────┬──────────┘   └───────────────────────┘   └────────────▲───────────┘
               │ enqueue                                                │
    ┌──────────▼──────────────────────────────────────────────────────┐│
    │                     MESSAGE QUEUE (Kafka / RabbitMQ)             ││
    │    [job1] [job2] [job3] [job4] [job5] ....                      ││
    └──────────────────────────────┬──────────────────────────────────┘│
                                   │ consume                            │
              ┌────────────────────┼─────────────────────┐             │
              │                    │                      │             │
   ┌──────────▼──────┐  ┌──────────▼──────┐  ┌──────────▼──────┐     │
   │  Worker Node 1  │  │  Worker Node 2  │  │  Worker Node 3  │     │
   │                 │  │                 │  │                 │     │
   │ ┌─────────────┐ │  │ ┌─────────────┐ │  │ ┌─────────────┐ │     │
   │ │  Container  │ │  │ │  Container  │ │  │ │  Container  │ │     │
   │ │  (sandbox)  │ │  │ │  (sandbox)  │ │  │ │  (sandbox)  │ │     │
   │ └─────────────┘ │  │ └─────────────┘ │  │ └─────────────┘ │     │
   └─────────────────┘  └─────────────────┘  └────────┬────────┘     │
                                                       │ write result  │
                                          ┌────────────▼──────────────┘
                                          │      RESULTS DB            │
                                          │   (Redis + PostgreSQL)     │
                                          └───────────────────────────┘
```

---

## Component Deep Dive

### 1. API Gateway 🚪

This is the **front door** of the entire system. It does 3 things:

**Authentication** — *"Who are you?"*
```
Every request must have a JWT token
Invalid token → 401 immediately, never reaches backend
```

**Rate Limiting** — *"You're doing too much"*
```
Free user:  max 10 submissions/minute
Pro user:   max 60 submissions/minute
Contest:    max 5 submissions/minute per problem

→ Enforced at the gateway using Redis counters
→ User hits limit → 429 Too Many Requests
```

**Routing** — *"Where should this go?"*
```
POST /submit     → Submission Service
GET  /problems   → Problem Service
GET  /results    → Results Service
```

This means if the Submission Service crashes, the Problem Service still works. They're **decoupled**.

---

### 2. Submission Service 📬

This service has **one job**: receive a submission and put it on the queue.

```python
# What the Submission Service does
def handle_submission(request):
    # 1. Validate input
    if len(request.code) > MAX_CODE_SIZE:
        return Error("Code too large")
    if request.language not in SUPPORTED_LANGUAGES:
        return Error("Unsupported language")
    
    # 2. Create a job
    job = {
        "job_id": generate_uuid(),       # e.g. "job_8f3a2b"
        "user_id": request.user_id,
        "problem_id": request.problem_id,
        "language": request.language,    # "python3", "java", "cpp"
        "code": request.code,
        "submitted_at": now(),
        "status": "QUEUED"
    }
    
    # 3. Save initial state to DB
    db.save(job)
    
    # 4. Put on the queue
    queue.publish("submissions", job)
    
    # 5. Return immediately — don't wait!
    return { "job_id": job["job_id"], "status": "QUEUED" }
```

**Key design decision**: The API returns **immediately** with a `job_id`. It does NOT wait for execution. This keeps the API fast regardless of how long the code takes to run.

---

### 3. The Message Queue 📨 — The Heart of the System

This is the most important component to understand deeply.

**Why a queue and not direct calls?**

Imagine it's a contest. 10,000 people submit simultaneously. Without a queue:

```
WITHOUT QUEUE:
10,000 users → 10,000 simultaneous container spawns
             → Server needs 10,000 x 256MB RAM = 2.5 TB RAM
             → Impossible. System crashes.

WITH QUEUE:
10,000 users → 10,000 jobs sit in queue (cheap, just memory)
             → 100 workers each pick 1 job at a time
             → 100 containers running at any moment (manageable)
             → Jobs process at the rate workers can handle
             → Users wait a bit longer, but NOTHING crashes
```

The queue acts as a **shock absorber** between spikes in demand and the actual processing capacity.

**What does the queue guarantee?**

```
┌─────────────────────────────────────────────┐
│  MESSAGE QUEUE GUARANTEES                   │
│                                             │
│  ✅ Durability   — jobs aren't lost if a   │
│                    worker crashes            │
│  ✅ At-least-once— every job is processed  │
│                    at least once             │
│  ✅ Backpressure — producers slow down if  │
│                    queue is full             │
└─────────────────────────────────────────────┘
```

**Kafka vs RabbitMQ — Which to use?**

| | Kafka | RabbitMQ |
|---|---|---|
| **Best for** | High throughput, replay | Task queues, routing |
| **Message retention** | Keeps messages (replayable) | Deletes after consumed |
| **Throughput** | Millions/sec | Thousands/sec |
| **Use case here** | Contest with 10k submissions/sec | Normal day ~100/sec |

For a code execution engine: **RabbitMQ or SQS** for simplicity, **Kafka** if you need scale + replay (re-judging all submissions after a test case fix).

---

### 4. Worker Nodes ⚙️ — Where the Magic Happens

A Worker is a service that:
1. Pulls a job from the queue
2. Spins up a container
3. Runs the code inside it
4. Collects the result
5. Writes result to DB
6. Acknowledges the job (removes from queue)

```python
def worker_loop():
    while True:
        # Step 1: Pull job from queue
        job = queue.consume("submissions")  # blocks until a job arrives
        
        # Step 2: Update status
        db.update(job.id, status="RUNNING")
        
        # Step 3: Run in container
        result = run_in_container(
            code=job.code,
            language=job.language,
            time_limit=3,        # seconds
            memory_limit=256,    # MB
        )
        
        # Step 4: Compare against test cases
        verdict = judge(result, expected_output=get_test_cases(job.problem_id))
        
        # Step 5: Save result
        db.update(job.id, status="DONE", verdict=verdict)
        
        # Step 6: Notify user (via WebSocket or push)
        notify_user(job.user_id, verdict)
        
        # Step 7: Acknowledge — tell queue this job is done
        queue.ack(job)
```

**Critical detail — the ACK mechanism:**

If the worker crashes AFTER consuming but BEFORE completing the job, the queue will **re-deliver** the job to another worker after a timeout. This is how durability works:

```
Worker 1 consumes job_42
Worker 1 crashes (mid-execution)
Queue sees: "job_42 not acked after 30 seconds"
Queue re-delivers job_42 to Worker 2
Worker 2 completes it ✅
```

---

### 5. How Does the User Get Their Result? 🔔

The user submitted code and got back a `job_id`. Now what?

**Option A: Polling (Simple)**
```
Client: GET /result/job_8f3a2b  → { status: "QUEUED" }
Client: GET /result/job_8f3a2b  → { status: "RUNNING" }
Client: GET /result/job_8f3a2b  → { status: "DONE", verdict: "Accepted" }
```
Every 1-2 seconds. Simple but creates constant unnecessary traffic.

**Option B: WebSockets (Better — what LeetCode uses)**
```
Client opens WebSocket connection to server
Client submits code → gets job_id
Client subscribes: "notify me when job_8f3a2b is done"

... time passes ...

Worker completes job → writes to DB → publishes to pub/sub
Server receives pub/sub event → pushes to client's WebSocket
Client sees result INSTANTLY ✅
```

```
Client ←──── WebSocket ────► Server ←── Redis Pub/Sub ──► Worker
                                 
"job done!"  ◄──────────────────────────────────────────  Worker publishes
```

The WebSocket stays open the whole time. The moment the worker finishes, the user sees the result — no polling needed.

---

### 6. The Results Database 🗄️

Two layers:

**Redis (fast, in-memory)** — for active/recent results
```
Key:   "result:job_8f3a2b"
Value: { status: "DONE", verdict: "Accepted", time: 145ms, memory: 18MB }
TTL:   1 hour  (auto-deleted after)
```
Workers write here first. Client reads from here. **Sub-millisecond reads.**

**PostgreSQL (persistent)** — for permanent storage
```sql
-- All submissions stored permanently
submissions (
    id, user_id, problem_id, language,
    code, verdict, runtime_ms, memory_mb,
    submitted_at
)
```
Used for leaderboards, submission history, analytics.

---

## The Full Request Lifecycle (End-to-End)

Let's trace exactly what happens when you click "Submit" on LeetCode:

```
1. Browser        →  POST /submit (code, language, problem_id)
                        ↓
2. API Gateway    →  Authenticate JWT, check rate limit
                        ↓
3. Submission Svc →  Validate, create job_id, save to DB (QUEUED),
                     publish to Kafka, return { job_id }
                        ↓
4. Browser        →  Opens WebSocket, subscribes to job_id updates
                        ↓
5. Kafka Queue    →  Holds job until a worker is free
                        ↓
6. Worker Node    →  Consumes job, spins up Docker container,
                     runs code with time+memory limits,
                     runs against all test cases, gets verdict
                        ↓
7. Worker Node    →  Writes result to Redis + PostgreSQL
                     Publishes "job_done" event to Redis Pub/Sub
                        ↓
8. Server         →  Receives pub/sub event, pushes to user's WebSocket
                        ↓
9. Browser        →  Displays "Accepted ✅ | Runtime: 145ms | Memory: 18MB"

Total time: ~1–3 seconds
```

---

## 📚 Chapter 3 Summary

| Component | Role | Technology |
|---|---|---|
| API Gateway | Auth, rate limiting, routing | Kong / Nginx |
| Submission Service | Validate, enqueue, return job_id | Node.js / Go |
| Message Queue | Decouple producers from workers, absorb spikes | Kafka / RabbitMQ |
| Worker Nodes | Run sandboxed containers, judge output | Go / Rust |
| Results DB | Store verdicts (fast + persistent) | Redis + PostgreSQL |
| Notification | Push result to user in real time | WebSockets + Redis Pub/Sub |

**The core insight:**
> Code execution is async and slow. The queue is what lets your API stay fast while workers handle execution at their own pace. This is the **Producer-Consumer pattern** at scale.

---

**Next up → Chapter 4: Scaling** — Our architecture works. Now how do we scale it to handle LeetCode-level traffic (especially during contests)? We'll cover horizontal scaling of workers, auto-scaling, and how to handle the "contest spike" problem.

---
# Chapter 4: Scaling — "From 100 to 10 Million Submissions" 📈

---

## 🌅 The Story Continues...

CodeNow just landed a partnership with a major university. Every Monday morning, 50,000 students log in and submit code for their assignments — all within the same 30-minute window.

Arjun checks the dashboard during the first assignment:

> *"Queue depth: 47,000 jobs. Workers: 10. Average wait time: 47 minutes. Students are rioting."*

The architecture from Chapter 3 is correct. But it's not **scaled**. There's a difference.

---

## Part 1: Scaling the Workers — The Obvious Fix

### Vertical vs Horizontal Scaling

**Vertical Scaling** — make the server bigger:
```
Worker: 4 CPU, 8GB RAM
   ↓
Worker: 32 CPU, 64GB RAM
```
This works — until it doesn't. There's a physical limit. A 128-core machine costs $50,000/month. And it's still a **single point of failure**.

**Horizontal Scaling** — add more servers:
```
1 Worker Node
   ↓
100 Worker Nodes (each normal-sized, cheap)
```
This is the right answer. Workers are **stateless** — each one just pulls jobs, runs containers, writes results. They don't need to know about each other. Perfect for horizontal scaling.

```
                    ┌─────────────────────────┐
                    │      MESSAGE QUEUE       │
                    │  [j1][j2][j3]...[j50000] │
                    └────────────┬────────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          │                      │                      │
   ┌──────▼──────┐        ┌──────▼──────┐        ┌──────▼──────┐
   │  Worker 1   │        │  Worker 2   │   ...  │  Worker 100 │
   │  (runs j1)  │        │  (runs j2)  │        │  (runs j3)  │
   └─────────────┘        └─────────────┘        └─────────────┘
   
   100 workers × 1 job each = 100 concurrent executions
   50,000 jobs ÷ 100 workers = ~500 seconds if each job takes 1s
```

With 100 workers processing in parallel, the 47-minute wait drops to **~8 minutes**. With 500 workers → **~1.6 minutes**. Linear scaling. ✅

---

## Part 2: The Contest Spike Problem 🎢

### The Story

Arjun adds 100 workers. Life is great on normal days. But then:

> **LeetCode Weekly Contest starts at 10:00 AM.**
> - 9:59 AM — 200 submissions in the queue (normal)
> - 10:00 AM — 15,000 submissions flood in (contest start)
> - 10:05 AM — contest ends, drops back to 300 submissions

The traffic pattern looks like this:

```
Submissions/min
     │
 15k │           ████
     │           █  █
     │           █  █
  5k │           █  █
     │           █  █
  300│  ─────────█  █──────────────────
     └──────────────────────────────── time
           9:50  10:00 10:05  10:10
```

**The Dilemma:**
- If you **always** run 500 workers → costs $50,000/month for capacity that sits idle 23 hours/day
- If you **always** run 50 workers → fine on normal days, fails during contests

This is the **classic cloud scaling problem**. The answer is **Auto-Scaling**.

---

## Part 3: Auto-Scaling — Pay Only For What You Use 💸

### The Idea
Instead of fixed capacity, dynamically add/remove workers based on **queue depth**.

```
Queue depth < 100   →  Run 10 workers  (quiet period)
Queue depth < 1000  →  Run 50 workers  (normal load)
Queue depth < 5000  →  Run 200 workers (high load)
Queue depth < 20000 →  Run 500 workers (contest)
```

### How It Works

A component called the **Auto-Scaler** watches the queue and adjusts:

```python
# Auto-scaler runs every 30 seconds
def auto_scale():
    queue_depth = kafka.get_queue_depth("submissions")
    current_workers = k8s.get_running_workers()
    
    desired_workers = calculate_desired(queue_depth)
    
    if desired_workers > current_workers:
        # Scale UP: spin up new worker pods
        k8s.scale_up("worker-deployment", desired_workers)
        log(f"Scaled UP to {desired_workers} workers")
    
    elif desired_workers < current_workers * 0.7:
        # Scale DOWN: only if significantly over-provisioned
        # (avoid flapping — don't scale down too aggressively)
        k8s.scale_down("worker-deployment", desired_workers)
        log(f"Scaled DOWN to {desired_workers} workers")

def calculate_desired(queue_depth):
    # Target: each worker should have ~20 jobs waiting for it
    # (tune this based on your avg job duration)
    target_jobs_per_worker = 20
    return min(
        math.ceil(queue_depth / target_jobs_per_worker),
        MAX_WORKERS  # hard cap: 1000
    )
```

### Scale Up vs Scale Down Asymmetry

Notice we scale up **aggressively** but scale down **conservatively**. Why?

```
Scale up fast:   Users are waiting. Every second matters.
Scale down slow: Workers are idle. Costs a bit more, but:
                  - Avoids "flapping" (constant up/down)
                  - Ready for the next burst
                  - Spinning up workers takes ~30 seconds
```

A common rule: **scale up in 1 step, scale down in 3 steps** (cooldown period between each).

---

## Part 4: Where to Run Workers — Kubernetes 🚢

### The Story

Arjun has 500 workers to manage. He can't SSH into each one manually. He needs something to:
- Start/stop workers automatically
- Replace crashed workers
- Spread workers across machines
- Manage resource limits per worker

This is exactly what **Kubernetes (K8s)** does.

### How Workers Run in K8s

Each Worker runs as a **Pod** (a container in K8s terms):

```yaml
# worker-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: code-execution-worker
spec:
  replicas: 50        # start with 50 workers
  template:
    spec:
      containers:
      - name: worker
        image: codernow/worker:v2
        resources:
          requests:
            cpu: "2"        # 2 CPU cores per worker
            memory: "1Gi"   # 1GB RAM per worker
          limits:
            cpu: "4"        # max 4 CPU cores
            memory: "2Gi"   # max 2GB RAM
```

The Auto-Scaler simply changes `replicas: 50` to `replicas: 500` — K8s handles the rest.

### The Node Pool Strategy

Not all workers should run on the same machines. Here's why:

```
┌─────────────────────────────────────────────────────┐
│                K8s CLUSTER                          │
│                                                     │
│  ┌────────────────────┐  ┌────────────────────┐    │
│  │  ON-DEMAND NODES   │  │   SPOT NODES       │    │
│  │  (always running)  │  │   (cheap, ~70% off)│    │
│  │                    │  │                    │    │
│  │  - API services    │  │  - Workers         │    │
│  │  - Databases       │  │  (can be killed    │    │
│  │  - Kafka           │  │   by cloud at      │    │
│  │                    │  │   any time)         │    │
│  └────────────────────┘  └────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

Workers are **perfect** for Spot/Preemptible instances because:
- If a spot worker is killed mid-job → queue re-delivers the job to another worker (remember Chapter 3's ACK mechanism!)
- Spot instances are 60–70% cheaper than on-demand
- Workers are stateless — losing one is fine

**Cost impact:** A system that would cost $50,000/month on on-demand nodes costs **$15,000/month** with spot workers.

---

## Part 5: Scaling the Database 🗄️

### The New Problem

Arjun scales his workers to 500. Now every worker is writing results to PostgreSQL simultaneously. The DB becomes the new bottleneck:

```
500 workers writing at the same time
→ PostgreSQL handles ~5,000 writes/sec
→ During contest: 15,000 writes/sec needed
→ DB starts dropping connections
→ Workers timeout → jobs re-delivered → duplicate processing
```

### Solution 1: Write-Ahead with Redis

Workers don't write to PostgreSQL directly. They write to **Redis first** (which handles 100k+ ops/sec), then a background job flushes Redis → PostgreSQL in batches:

```
Worker → Redis (instant, 0.1ms)
                ↓
         Background job (every 5 seconds)
                ↓
         Batch INSERT into PostgreSQL
         (1 query for 500 rows = very efficient)
```

### Solution 2: Read Replicas

For reads (loading submission history, leaderboards), add **read replicas**:

```
                   ┌──────────────────┐
                   │  PostgreSQL      │
                   │  PRIMARY (RW)    │ ← workers write here
                   └────────┬─────────┘
                            │ replicates continuously
              ┌─────────────┼──────────────┐
              │             │              │
   ┌──────────▼──┐  ┌───────▼──────┐  ┌───▼────────────┐
   │  Replica 1  │  │  Replica 2   │  │  Replica 3     │
   │  (read only)│  │  (read only) │  │  (read only)   │
   └─────────────┘  └──────────────┘  └────────────────┘
          ↑                ↑                  ↑
   leaderboard API    submission history    analytics
```

Writes go to one Primary. Reads spread across 3 replicas → **3x read throughput**.

### Solution 3: Sharding Submissions

Eventually even one Primary can't handle all writes. Time to **shard** — split data across multiple databases.

**Sharding Key Decision — This is Critical:**

```
Option A: Shard by user_id
  User 1-1M    → DB Shard 1
  User 1M-2M   → DB Shard 2
  User 2M-3M   → DB Shard 3
  
  ✅ All submissions for a user are on one shard
     (fast: "show me my submission history")
  ❌ Hot users (competitive programmers) create hot shards

Option B: Shard by submission_id (hash-based)
  hash(submission_id) % 4 → which shard
  
  ✅ Perfectly even distribution
  ❌ "Show all submissions by user X" requires querying ALL shards

Option C: Shard by problem_id (used for leaderboards)
  Problem 1-500   → DB Shard 1
  Problem 501-1000 → DB Shard 2
  
  ✅ All submissions for a contest problem are co-located
     (fast leaderboard queries)
  ❌ Popular problems create hot shards
```

**The industry answer:** Use **user_id-based sharding** for the submissions table (most queries are per-user) + a **separate leaderboard service** with its own data store (Redis Sorted Sets — perfect for rankings).

---

## Part 6: Scaling the Queue 📨

### Can Kafka itself become a bottleneck?

Yes. And here's how Kafka handles scale internally — this is worth understanding for interviews:

```
KAFKA TOPIC: "submissions"

Partition 0: [j1] [j5] [j9]  ...  ← Worker Group A reads this
Partition 1: [j2] [j6] [j10] ...  ← Worker Group B reads this
Partition 2: [j3] [j7] [j11] ...  ← Worker Group C reads this
Partition 3: [j4] [j8] [j12] ...  ← Worker Group D reads this
```

Kafka splits a topic into **partitions**. Each partition is consumed by one worker group **in parallel**. More partitions = more parallelism.

For a code execution engine:
- **Normal load**: 4 partitions, 4 consumer groups
- **Contest load**: 32 partitions, 32 consumer groups

**The scaling rule**: number of partitions = max parallelism possible. Set it high upfront — you can't decrease partitions later without data loss.

---

## Part 7: Language-Specific Worker Pools 🐍☕🦀

### The Story

Priya notices something: Python jobs take ~800ms on average. C++ jobs take ~150ms. But they're all in the same queue.

```
PROBLEM: Mixed queue
[Python job] [Python job] [C++ job] [Python job] [C++ job]
     ↓             ↓           ↓
   800ms          800ms      150ms
   
C++ jobs are WAITING behind slow Python jobs
```

### Solution: Separate Queues Per Language

```
submissions.python  →  Python Workers  (needs PyPy runtime)
submissions.java    →  Java Workers    (needs JVM)
submissions.cpp     →  C++ Workers     (needs GCC, fastest)
submissions.javascript → Node Workers

Benefits:
✅ C++ users never wait behind Python users
✅ Can scale each pool independently
   (Python needs more workers due to slowness)
✅ Worker images are smaller (Python worker doesn't need JVM)
✅ Resource limits differ per language
   (Java needs more RAM for JVM warmup)
```

---

## Part 8: Geo-Distribution 🌍

### The Story

CodeNow goes global. Users in Singapore are complaining:

> *"It takes 4 seconds for my code to run. US users get results in 1.5 seconds."*

The problem: all workers are in `us-east-1`. A Singapore user's request travels:
```
Singapore → us-east-1 (150ms RTT)
         → runs in container (1000ms)  
         → result back to Singapore (150ms)
Total: ~1.3 seconds just in network latency
```

### Solution: Regional Deployments

```
            ┌─────────────────────────────────────────┐
            │           GLOBAL LOAD BALANCER           │
            │      (routes to nearest region)          │
            └───────────┬──────────────┬───────────────┘
                        │              │
         ┌──────────────▼──┐     ┌─────▼────────────────┐
         │  us-east-1      │     │  ap-southeast-1       │
         │                 │     │  (Singapore)          │
         │  - API          │     │  - API                │
         │  - Queue        │     │  - Queue              │
         │  - Workers      │     │  - Workers            │
         │  - DB (primary) │     │  - DB (replica)       │
         └─────────────────┘     └───────────────────────┘
```

**What's replicated, what's not:**

```
Replicated globally:
  ✅ Worker pools (run code locally — no cross-region latency)
  ✅ Redis cache (fast reads everywhere)
  ✅ DB read replicas (submission history)

Central (not replicated):
  ❌ DB Primary (one source of truth for writes)
  ❌ Problem test cases (sync'd via CDN)
  ❌ User accounts (single source of truth)
```

Singapore user's new latency:
```
Singapore → Singapore region API (5ms)
          → runs in Singapore container (1000ms)
          → result back (5ms)
Total: ~1.01 seconds ✅
```

---

## 📚 Chapter 4 Summary

```
SCALING DIMENSION    SOLUTION                     KEY INSIGHT
─────────────────────────────────────────────────────────────────
Worker throughput  → Horizontal scaling          Workers are stateless,
                     (add more workers)           add infinitely

Contest spikes     → Auto-scaling on             Pay per use, not
                     queue depth                  for peak capacity

Worker cost        → Spot/Preemptible VMs        Jobs are retryable,
                                                  so lost workers are OK

DB write load      → Redis write buffer          Batch writes are
                   → Read replicas                10x more efficient
                   → Sharding by user_id

Queue throughput   → Kafka partitions            Parallelism =
                                                  num partitions

Fairness           → Per-language queues         Fast languages
                                                  shouldn't wait on slow

Latency            → Regional deployments        Code runs near the user
```

**The core insight:**
> Scaling isn't one decision — it's a **layered set of decisions** at each component. The queue absorbs spikes. Workers scale horizontally. The DB scales with replicas + sharding. Everything is designed to fail gracefully and recover automatically.

---

**Next up → Chapter 5: Fault Tolerance & Error Handling** — What happens when things go wrong? A worker crashes mid-execution, a container runs forever, a test case has a bug. We'll cover timeouts, retries, idempotency, and how to build a system that **never loses a submission**.
---
# Chapter 5: Fault Tolerance & Error Handling — "Everything That Can Go Wrong, Will Go Wrong" 💥

---

## 🌅 The Story Continues...

CodeNow is scaled up. 500 workers. Auto-scaling. Regional deployments. Arjun is feeling confident.

Then, during a live contest with 100,000 users, the on-call engineer gets paged:

> *"Alert: 3,000 submissions stuck in RUNNING state for over 10 minutes. Users are angry. Leaderboard is frozen."*

Arjun digs in. He finds:
- Some workers **crashed mid-execution** — their jobs never completed
- Some users submitted **infinite loops** — containers running forever, blocking workers
- One user found a way to **exhaust all worker memory** — cascading failures
- A deployment bug caused **duplicate verdicts** — same submission judged twice

Each of these is a different class of failure. Let's solve them one by one.

---

## Failure Class 1: The Infinite Loop ♾️ — "My Worker is Stuck Forever"

### The Problem

A user submits this:

```python
def solve(n):
    while True:   # oops
        pass
```

Without protection, this container runs **forever**. The worker that picked it up is now permanently occupied. Over time, enough of these accumulate and **all workers are stuck** — the system grinds to a halt.

### Solution: The Timeout Layer

Every job needs **three independent timeout mechanisms**. Not one. Three. Because any single one can fail.

```
┌────────────────────────────────────────────────────────────┐
│                   TIMEOUT LAYERS                            │
│                                                             │
│  Layer 1: Container-level timeout (inside the sandbox)     │
│  Layer 2: Worker-level timeout (outside the container)     │
│  Layer 3: Queue-level timeout (outside the worker)         │
└────────────────────────────────────────────────────────────┘
```

**Layer 1 — Container Timeout (innermost)**

When the worker spawns the container, it sets a hard time limit:

```python
def run_in_container(code, language, time_limit_sec=3):
    container = docker.run(
        image=f"executor-{language}",
        command=["timeout", str(time_limit_sec), "python3", "solution.py"],
        # ↑ Linux 'timeout' command: kills process after N seconds
    )
    return container.wait(timeout=time_limit_sec + 1)
    # ↑ Python-level wait: 1 extra second buffer
```

If the code runs longer than 3 seconds → `timeout` sends `SIGKILL` → container stops → worker gets `TLE` (Time Limit Exceeded) verdict.

```
User's infinite loop starts
↓
t=0s: Container starts running
↓
t=3s: Linux `timeout` sends SIGKILL
↓
t=3s: Container dead, worker reads exit code
↓
Worker: "exit code 124 = TLE"
↓
Verdict: "Time Limit Exceeded" ← user sees this ✅
```

**Layer 2 — Worker-level Watchdog Timer**

What if the container timeout itself hangs? (Rare, but containers can get stuck in weird kernel states.) The worker runs a watchdog:

```python
def worker_execute(job):
    container = spawn_container(job)
    
    # Watchdog: if container doesn't finish in 10s, we kill it
    try:
        result = container.wait(timeout=10)   # 3s limit + 7s buffer
    except TimeoutError:
        container.kill()              # force kill the container
        container.remove(force=True)  # clean up
        return Verdict.TLE
    
    return parse_result(result)
```

**Layer 3 — Queue Visibility Timeout**

What if the worker itself crashes while running the job? The container is gone, but the job is still marked as "in progress" in the queue.

Kafka/SQS solves this with a **visibility timeout**:

```
Worker consumes job → job becomes "invisible" in queue
                      (other workers can't see it)
                            ↓
If worker ACKs within 30s → job deleted from queue ✅
If worker does NOT ack    → after 30s, job becomes
  (crash, timeout)           visible again → re-delivered
                             to another worker ✅
```

```
Timeline:
t=0:   Worker A consumes job_42 (job hidden for 30s)
t=3:   Worker A finishes, ACKs → job_42 deleted ✅

OR:

t=0:   Worker A consumes job_42 (job hidden for 30s)
t=15:  Worker A crashes (OOM, kernel panic, network split)
t=30:  Queue: "job_42 not acked" → visibility resets
t=31:  Worker B consumes job_42 → processes it ✅
```

**All three layers working together:**

```
Scenario: infinite loop + worker crash
→ Layer 1: TLE kills the container at t=3s
→ Layer 2: Watchdog kills stuck container at t=10s
→ Layer 3: Queue re-delivers if worker dies at any point

Even in the worst case, the job WILL be processed. ✅
```

---

## Failure Class 2: The Memory Bomb 💣 — "My Worker is Out of Memory"

### The Problem

```python
# User submits this
x = []
while True:
    x.append(" " * 10**6)  # eats 1MB per iteration
```

Without limits, this process grows until it consumes all RAM on the worker node, causing the worker (and potentially the entire host machine) to crash.

### Solution: cgroup Memory Limits (Revisited with Details)

Remember cgroups from Chapter 2? Here's exactly how the worker configures them:

```python
def spawn_container(job):
    return docker.run(
        image=f"executor-{job.language}",
        mem_limit="256m",          # hard limit: container dies at 256MB
        memswap_limit="256m",      # no swap (swap = slow, unpredictable)
        cpu_period=100000,         # CPU quota period: 100ms
        cpu_quota=200000,          # allowed CPU in period: 200ms = 2 cores max
        pids_limit=50,             # max 50 processes (stops fork bombs)
        network_disabled=True,     # no internet access
        read_only=True,            # read-only filesystem
        tmpfs={"/tmp": "size=64m"} # writable temp dir, 64MB max
    )
```

When the container hits 256MB:
```
Kernel OOM killer activates
→ Container process receives SIGKILL
→ Container exits with code 137
→ Worker reads exit code 137 = "Memory Limit Exceeded"
→ Verdict: MLE ✅
```

### The Noisy Neighbor Problem

Even with per-container limits, a Worker Node hosts multiple containers. If 5 containers each use 256MB simultaneously:

```
Worker Node: 4GB RAM total
5 containers × 256MB = 1.28GB (fine)
But peak: 5 containers all allocate at the same time
→ 5 × 512MB peak = 2.56GB (still fine)
→ If 10 containers spike simultaneously → OOM at node level
```

**Solution: Limit concurrent containers per worker node**

```python
# Each worker node has a semaphore
MAX_CONCURRENT_CONTAINERS = 8  # tuned based on node RAM

semaphore = threading.Semaphore(MAX_CONCURRENT_CONTAINERS)

def worker_loop():
    while True:
        job = queue.consume()
        
        with semaphore:  # blocks if 8 containers already running
            result = run_in_container(job)
        
        write_result(result)
```

This is **backpressure in action** — the worker doesn't consume more jobs than it can safely handle.

---

## Failure Class 3: Duplicate Processing 👥 — "The Same Submission Judged Twice"

### The Story

During a deployment, a bug caused workers to sometimes process a job, then crash before ACKing the queue. The queue re-delivered the job. The user got **two results** — one "Accepted", one "Wrong Answer" (from different test case orderings). The leaderboard showed contradictory data.

### Why This Is Hard

The re-delivery from the queue (Layer 3 timeout) is a feature — it prevents lost jobs. But it creates a new problem: **at-least-once delivery** means the same job might be processed more than once.

```
"At-least-once" delivery:
  ✅ Every job is processed at least once (no lost jobs)
  ❌ Some jobs processed more than once (duplicates)

"Exactly-once" delivery:
  ✅ Every job processed exactly once
  ❌ Extremely hard to guarantee. Kafka supports it but
     with significant performance overhead.
```

The practical answer is: **design for at-least-once, but make processing idempotent.**

### Solution: Idempotency

**Idempotent** means: running the same operation twice produces the same result as running it once.

```python
def process_job(job):
    # Check if already processed
    existing = db.get_result(job.job_id)
    
    if existing and existing.status == "DONE":
        # Already processed! Safe to skip.
        queue.ack(job)
        return
    
    # Check if currently being processed by another worker
    lock_acquired = redis.set(
        key=f"lock:job:{job.job_id}",
        value=worker_id,
        nx=True,      # only set if not exists
        ex=60         # auto-expire after 60 seconds
    )
    
    if not lock_acquired:
        # Another worker is processing this right now
        queue.nack(job)  # put back in queue
        return
    
    try:
        result = run_in_container(job)
        db.save_result(job.job_id, result)
        queue.ack(job)
    finally:
        redis.delete(f"lock:job:{job.job_id}")
```

**The distributed lock pattern:**

```
Worker A and Worker B both receive job_42 (re-delivery race)

Worker A: SET lock:job:42 "worker_a" NX EX 60
          → Success! Lock acquired. Proceeds to execute.

Worker B: SET lock:job:42 "worker_b" NX EX 60
          → FAILS (key already exists). 
          → Worker B puts job back in queue and moves on.

Worker A finishes → deletes lock → writes result → ACKs queue ✅
```

Only one worker ever executes a given job. Duplicates are neutralized.

---

## Failure Class 4: Worker Crashes 💀 — "My Worker Just Died"

### Types of Worker Crashes

```
1. Soft crash:   Worker process exits (OOM, unhandled exception)
   Recovery:     K8s restarts the pod automatically in ~10 seconds
   Job fate:     Queue re-delivers after visibility timeout

2. Hard crash:   Node dies (hardware failure, spot interruption)
   Recovery:     K8s schedules new pod on another node
   Job fate:     Queue re-delivers after visibility timeout

3. Slow crash:   Worker alive but not processing
   (stuck in infinite wait, network partition)
   Recovery:     Health check fails → K8s restarts pod
   Job fate:     Queue re-delivers after visibility timeout
```

### Health Checks — How K8s Knows When to Restart

```yaml
# In the worker K8s deployment
livenessProbe:
  exec:
    command: ["python3", "-c", "import health; health.check()"]
  initialDelaySeconds: 10
  periodSeconds: 15      # check every 15 seconds
  failureThreshold: 3    # restart after 3 consecutive failures

readinessProbe:
  httpGet:
    path: /ready          # returns 200 if worker is ready for jobs
    port: 8080
  periodSeconds: 5
```

The `health.check()` function verifies:
```python
def check():
    # Is the queue connection alive?
    assert kafka.ping()
    
    # Are we processing jobs? (detect stuck worker)
    last_job_processed = redis.get("worker:last_job_time")
    if last_job_processed:
        idle_seconds = now() - last_job_processed
        assert idle_seconds < 300, "Worker stuck for >5 min"
    
    # Is container runtime available?
    assert docker.ping()
```

If any check fails 3 times in a row → K8s kills and restarts the worker pod. **Mean recovery time: ~15 seconds.**

---

## Failure Class 5: The Poison Pill ☠️ — "One Job Keeps Crashing Every Worker"

### The Story

A specific submission causes a bug in the container runtime itself — not the user's code, but a bug in the executor image. Every worker that picks this job immediately crashes. The job keeps getting re-delivered. Worker after worker dies.

```
Queue: [normal] [normal] [POISON] [normal] [normal]

Worker 1 picks POISON → crashes
Queue re-delivers POISON
Worker 2 picks POISON → crashes
Queue re-delivers POISON
Worker 3 picks POISON → crashes
...
```

This is called a **poison pill** — a message that kills every consumer that touches it.

### Solution: Dead Letter Queue (DLQ)

```
Normal queue:  [j1][j2][POISON][j4][j5]

POISON fails attempt 1 → re-queued
POISON fails attempt 2 → re-queued
POISON fails attempt 3 → MOVED TO DEAD LETTER QUEUE

Dead Letter Queue: [POISON]  ← isolated, not re-delivered automatically

Normal queue continues: [j1][j2][j4][j5]  ← everyone else unaffected
```

Configuration in Kafka/SQS:

```python
# SQS queue config
{
    "RedrivePolicy": {
        "maxReceiveCount": 3,        # after 3 failures
        "deadLetterTargetArn": "arn:aws:sqs:...:dlq-submissions"
        # ↑ move to Dead Letter Queue
    }
}
```

**What happens to DLQ messages?**

```
1. Alert fires: "DLQ has messages" → on-call engineer notified
2. Engineer inspects the poison pill manually
3. Fix the bug in the executor
4. Reprocess from DLQ after fix
```

The DLQ is your **safety valve** — it isolates bad messages without stopping the entire system.

---

## Failure Class 6: The Cascade 🌊 — "One Failure Causes Everything to Fail"

### The Story

The Results DB goes down for 30 seconds (routine maintenance). During this time:

```
Workers finish executing code
→ Try to write result to DB
→ DB unavailable → exception
→ Workers retry → DB still down
→ Workers retry again and again
→ Workers now spending all time retrying DB writes
→ No workers available to process new submissions
→ Queue backlog grows to 50,000
→ DB comes back up
→ All 50,000 workers simultaneously hammer DB
→ DB goes down again under the load
→ Cascade failure
```

This is a **thundering herd** problem leading to a cascade.

### Solution 1: Exponential Backoff with Jitter

Don't retry immediately. Wait longer each time. Add randomness to prevent all workers retrying at the same moment:

```python
def write_result_with_retry(job_id, result, max_retries=5):
    for attempt in range(max_retries):
        try:
            db.write(job_id, result)
            return  # success
        except DatabaseUnavailable:
            if attempt == max_retries - 1:
                raise  # give up after 5 attempts
            
            # Exponential backoff: 1s, 2s, 4s, 8s, 16s
            base_wait = 2 ** attempt
            
            # Jitter: randomize ±50% to avoid thundering herd
            jitter = random.uniform(0.5, 1.5)
            wait_time = base_wait * jitter
            
            time.sleep(wait_time)

# Without jitter: all 500 workers retry at t=2s → spike
# With jitter: workers retry between t=1s and t=3s → smooth
```

### Solution 2: Circuit Breaker 🔌

A circuit breaker **stops trying** when failures exceed a threshold, giving the downstream service time to recover:

```
CLOSED state (normal):
  Requests flow through. Track failure rate.
  If failure rate > 50% in last 60s → OPEN

OPEN state (DB is down):
  Requests FAIL FAST (no waiting, no retrying)
  After 30 seconds → try one request (HALF-OPEN)

HALF-OPEN state:
  If that request succeeds → back to CLOSED ✅
  If it fails → back to OPEN
```

```python
class CircuitBreaker:
    def __init__(self, failure_threshold=0.5, timeout=30):
        self.state = "CLOSED"
        self.failure_count = 0
        self.last_opened = None
    
    def call(self, fn, *args):
        if self.state == "OPEN":
            if time.now() - self.last_opened > 30:
                self.state = "HALF_OPEN"
            else:
                raise CircuitOpenError("DB circuit open, fail fast")
        
        try:
            result = fn(*args)
            if self.state == "HALF_OPEN":
                self.state = "CLOSED"  # recovery!
            return result
        except Exception:
            self.failure_count += 1
            if self.failure_count > threshold:
                self.state = "OPEN"
                self.last_opened = time.now()
            raise

# Usage:
db_breaker = CircuitBreaker()
db_breaker.call(db.write, job_id, result)
```

**What workers do when circuit is open:**

```python
try:
    db_breaker.call(db.write, job_id, result)
except CircuitOpenError:
    # DB is down. Don't lose the result.
    # Buffer in Redis temporarily.
    redis.lpush("pending_db_writes", json.dumps({job_id: result}))
    # Background job will flush Redis → DB when circuit closes
```

Results are **never lost** — they buffer in Redis during the outage.

---

## Putting It All Together: The Fault Tolerance Map

```
FAILURE                  DETECTION              RECOVERY
──────────────────────────────────────────────────────────────────
Infinite loop         →  3-layer timeout    →  TLE verdict, job done

Memory bomb           →  cgroup OOM killer  →  MLE verdict, job done

Worker soft crash     →  K8s liveness probe →  Pod restart in ~15s
                         Queue visibility      Job re-delivered

Worker node death     →  K8s node monitor   →  New pod on new node
                         Queue visibility      Job re-delivered

Duplicate processing  →  Redis distributed  →  Idempotent check,
                         lock                  second run skipped

Poison pill           →  Retry counter      →  Moved to DLQ after
                                               3 failures, system
                                               continues normally

DB cascade failure    →  Circuit breaker    →  Fail fast, buffer in
                         + exponential         Redis, flush on recovery
                           backoff + jitter

Contest spike causing →  Queue depth alarm  →  Auto-scaler adds workers
worker exhaustion                              within 60 seconds
```

---

## 📚 Chapter 5 Summary

The key philosophy of fault tolerance:

```
┌─────────────────────────────────────────────────────────┐
│  FAULT TOLERANCE PRINCIPLES                              │
│                                                          │
│  1. ASSUME FAILURE  Everything will fail.                │
│                     Design for it, not against it.       │
│                                                          │
│  2. FAIL FAST       Detect failure immediately.          │
│                     Don't let it spread.                 │
│                                                          │
│  3. ISOLATE         One bad job/worker/service           │
│                     should not affect others.            │
│                                                          │
│  4. RECOVER AUTO    No human should be needed            │
│                     for routine failures.                │
│                                                          │
│  5. NEVER LOSE DATA Buffer > drop. Always find           │
│                     somewhere to park the data.          │
└─────────────────────────────────────────────────────────┘
```

---

**Next up → Chapter 6: The Judging System** — How does the system actually decide "Accepted" vs "Wrong Answer"? We'll go deep on test case management, custom checkers, partial scoring, anti-cheat (detecting identical submissions), and how to handle problems where multiple correct outputs exist.

---

# Chapter 6: The Judging System — "How Do We Know If The Code Is Actually Correct?" ⚖️

---

## 🌅 The Story Continues...

The execution engine is solid. Code runs safely, scales beautifully, fails gracefully. But Arjun realizes something fundamental:

> *"Running code is the easy part. Knowing if it's **correct** — that's where it gets really interesting."*

A new engineer, **Divya**, is hired to own the judging system. On her first day she asks what seems like a simple question:

> *"How do we check if the user's output is correct?"*

Arjun says: *"Compare it to the expected output."*

Divya stares at him. *"That's it? That's the whole plan?"*

Over the next hour, she finds **7 problems** with that plan. Let's discover them one by one.

---

## Problem 1: "What Even Is Correct?" 🤔

### The Naive Approach

```python
def judge(user_output: str, expected_output: str) -> Verdict:
    if user_output == expected_output:
        return Verdict.ACCEPTED
    else:
        return Verdict.WRONG_ANSWER
```

This looks fine. Until it isn't.

**Case 1: Trailing whitespace**
```
Expected: "42\n"
User:     "42"      ← missing newline

Naive judge: WRONG ANSWER ❌
Should be:   ACCEPTED ✅
```

**Case 2: Floating point**
```
Expected: "3.14159265358979"
User:     "3.141592653589793"   ← more precise, still correct

Naive judge: WRONG ANSWER ❌
Should be:   ACCEPTED ✅
```

**Case 3: Multiple valid answers**
```
Problem: "Print any prime number between 10 and 20"
Expected: "11"
User:     "13"       ← also a valid prime!

Naive judge: WRONG ANSWER ❌
Should be:   ACCEPTED ✅
```

**Case 4: Graph problems**
```
Problem: "Print a valid topological sort of this graph"
Expected: "A B C D"
User:     "A C B D"  ← also valid! Multiple valid orderings exist.

Naive judge: WRONG ANSWER ❌
Should be:   ACCEPTED ✅
```

String comparison is **not** a judging strategy. It's a starting point.

---

## The Solution: A Layered Judging Architecture

Divya designs a system with **4 types of judges**, selected per problem:

```
┌─────────────────────────────────────────────────────────────┐
│                    JUDGE TYPES                               │
│                                                              │
│  1. Token Judge      — whitespace-tolerant string compare   │
│  2. Float Judge      — comparison with epsilon tolerance     │
│  3. Special Judge    — custom checker program               │
│  4. Interactive Judge— back-and-forth with user program     │
└─────────────────────────────────────────────────────────────┘
```

---

## Judge Type 1: Token Judge (Default) 📝

Tokenize both outputs and compare tokens, ignoring whitespace:

```python
def token_judge(user_output: str, expected_output: str) -> bool:
    # Split on any whitespace (spaces, newlines, tabs)
    user_tokens = user_output.split()
    expected_tokens = expected_output.split()
    
    return user_tokens == expected_tokens

# Examples:
token_judge("42\n", "42")           # True ✅
token_judge("1 2 3\n", "1  2  3")   # True ✅ (multiple spaces ok)
token_judge("YES\n", "Yes\n")       # False ❌ (case-sensitive by default)
```

This handles 80% of problems correctly. Simple, fast, no configuration.

---

## Judge Type 2: Float Judge 🔢

For geometry, physics, or math problems where precision matters:

```python
def float_judge(user_output: str, expected_output: str,
                epsilon: float = 1e-6) -> bool:
    try:
        user_vals = [float(x) for x in user_output.split()]
        expected_vals = [float(x) for x in expected_output.split()]
        
        if len(user_vals) != len(expected_vals):
            return False
        
        return all(
            abs(u - e) <= epsilon or           # absolute error
            abs(u - e) / (abs(e) + 1e-9) <= epsilon  # relative error
            for u, e in zip(user_vals, expected_vals)
        )
    except ValueError:
        return False  # couldn't parse as float

# Examples:
float_judge("3.14159",  "3.141592653")  # True ✅ (within 1e-6)
float_judge("3.14",     "3.141592653")  # False ❌ (too imprecise)
float_judge("1000000.1","1000000.0")    # True ✅ (relative error fine)
```

The epsilon value is **configured per problem** by the problem setter:
```json
{
  "problem_id": "geometry_001",
  "judge_type": "float",
  "epsilon": 1e-9   // this problem requires more precision
}
```

---

## Judge Type 3: Special Judge (Custom Checker) 🧑‍⚖️

This is the most powerful. For problems with **multiple valid answers**, the problem setter writes a **checker program** that validates correctness instead of comparing strings.

### How It Works

```
Normal judging:
  user_output ──► compare ──► ACCEPTED/WA

Special judging:
  user_output ──►
  input        ──► checker_program ──► ACCEPTED/WA/PARTIAL
  expected_output ──►
```

The checker is a **separate program** (usually C++ for speed) that receives:
- The original **input** to the problem
- The **user's output**
- The **expected output** (optional, for reference)

And returns a verdict.

### Example: The Topological Sort Problem

```cpp
// checker.cpp — written by problem setter
#include <bits/stdc++.h>
using namespace std;

int main() {
    // Read the original input (the graph)
    int n, m;
    cin >> n >> m;
    
    vector<pair<int,int>> edges(m);
    set<pair<int,int>> edge_set;
    for (auto& [u, v] : edges) {
        cin >> u >> v;
        edge_set.insert({u, v});
    }
    
    // Read user's output
    vector<int> user_order(n);
    for (auto& x : user_order) cin >> x;
    
    // Validate: is this a valid topological sort?
    map<int, int> position;
    for (int i = 0; i < n; i++)
        position[user_order[i]] = i;
    
    // Check: for every edge u→v, u must come before v
    for (auto [u, v] : edge_set) {
        if (position[u] > position[v]) {
            cout << "WRONG_ANSWER: edge " << u << "→" << v
                 << " violated\n";
            return 1;
        }
    }
    
    cout << "ACCEPTED\n";
    return 0;
}
```

This checker accepts **any valid topological ordering**, not just one specific one.

### The Checker Runs Inside a Sandbox Too

```
⚠️  Critical: The checker is code that runs on your servers.
    What if a malicious problem setter uploads a checker
    that deletes your database?
```

Yes — the **checker itself** must run in a sandbox:

```
┌─────────────────────────────────────────────────────────┐
│  Judging Pipeline for Special Judge                      │
│                                                          │
│  User Code → Container 1 → user_output.txt              │
│                                                          │
│  user_output.txt ─┐                                     │
│  input.txt ───────┼──► Checker → Container 2 → verdict  │
│  expected.txt ────┘     (also sandboxed!)               │
└─────────────────────────────────────────────────────────┘
```

Two sandboxes. One for user code. One for the checker. Belt and suspenders.

---

## Judge Type 4: Interactive Judge 🤝

Some problems are **interactive** — the user's program must have a **conversation** with the judge:

```
Classic problem: "Guess a number between 1-1000 in 10 guesses"

User program → "500" (guess)
Judge        → "TOO HIGH"
User program → "250"
Judge        → "TOO LOW"
User program → "375"
Judge        → "CORRECT"
```

### The Architecture Challenge

In a normal judge, user code runs, terminates, outputs. Here, user code must **stay alive and communicate bidirectionally** with a judge process.

```
┌────────────────────────────────────────────────────────┐
│                                                         │
│  Judge Process ←──── pipe ────► User Process           │
│  (in container 2)    stdin/     (in container 1)       │
│                      stdout                            │
│                                                         │
│  Both run simultaneously, communicating via pipes      │
└────────────────────────────────────────────────────────┘
```

Implementation using Unix pipes:

```python
def run_interactive(user_code, judge_code):
    # Create two pipes: one for each direction
    user_to_judge_r, user_to_judge_w = os.pipe()
    judge_to_user_r, judge_to_user_w = os.pipe()
    
    # Start user process
    user_proc = subprocess.Popen(
        ["./user_solution"],
        stdin=judge_to_user_r,   # reads from judge
        stdout=user_to_judge_w,  # writes to judge
    )
    
    # Start judge process
    judge_proc = subprocess.Popen(
        ["./judge"],
        stdin=user_to_judge_r,   # reads from user
        stdout=judge_to_user_w,  # writes to user
    )
    
    # Wait for judge to finish (it decides the verdict)
    verdict = judge_proc.wait(timeout=10)
    user_proc.kill()  # clean up user process
    
    return verdict
```

---

## Problem 2: Test Case Management 📦

### The Story

Divya now asks: *"Where do we store the test cases? The input and expected output for each problem?"*

A typical competitive programming problem has:
- **10–50 test cases** for normal problems
- **100+ test cases** for hard problems with edge cases
- Each test case: input file + output file
- Some inputs can be **hundreds of MB** (large graph problems)

### Where Do Test Cases Live?

```
❌ Bad: Store in PostgreSQL (blobs in relational DB)
   → Slow reads, expensive storage, hard to update

❌ Bad: Store on worker nodes
   → Need to sync across hundreds of workers
   → Worker disk fills up

✅ Good: Object Storage (S3/GCS) + CDN
```

```
┌────────────────────────────────────────────────────────────┐
│                TEST CASE STORAGE                            │
│                                                             │
│  S3 Bucket: "codernow-testcases"                           │
│    /problem_001/                                            │
│        case_01.in   (input)                                 │
│        case_01.out  (expected output)                       │
│        case_02.in                                           │
│        case_02.out                                          │
│        ...                                                  │
│        checker.cpp  (if special judge)                      │
│                                                             │
│  Worker downloads test cases:                               │
│    → Cached locally on first use                            │
│    → Subsequent submissions use cache                       │
│    → Cache invalidated when problem is updated              │
└────────────────────────────────────────────────────────────┘
```

### Test Case Caching on Workers

Downloading 50 test cases from S3 for every submission is slow (~200ms). Workers cache them locally:

```python
class TestCaseCache:
    def __init__(self):
        self.cache_dir = "/tmp/testcases"
        self.metadata = {}  # problem_id → (etag, local_path)
    
    def get(self, problem_id: str) -> list[TestCase]:
        s3_etag = s3.get_etag(f"testcases/{problem_id}/")
        
        # Cache hit: etag matches (test cases haven't changed)
        if self.metadata.get(problem_id) == s3_etag:
            return self.load_from_disk(problem_id)
        
        # Cache miss: download from S3
        self.download(problem_id)
        self.metadata[problem_id] = s3_etag
        return self.load_from_disk(problem_id)
```

The **ETag** is S3's content hash. If the test cases are updated (problem setter fixes a bug), the ETag changes → workers re-download. Otherwise, they use the local cache.

---

## Problem 3: Running Test Cases Efficiently ⚡

### Fail Fast Strategy

For a problem with 50 test cases, do you run all 50 even if case 1 fails?

```
Option A: Run all 50, report all failures
  → Takes full execution time even for broken code
  → Useful for: feedback-heavy learning platforms (Replit)

Option B: Stop at first failure (short-circuit)
  → Fast: wrong answer detected immediately
  → Useful for: competitive programming (LeetCode)

Option C: Run all in parallel
  → Fastest total time
  → Expensive: 50 containers simultaneously
```

**LeetCode's approach — Short-circuit with early termination:**

```python
def run_all_test_cases(code, problem_id, language):
    test_cases = cache.get(problem_id)
    
    for i, test_case in enumerate(test_cases):
        result = run_single(code, test_case.input, language)
        
        verdict = judge(result.output, test_case.expected_output,
                       problem.judge_type)
        
        if verdict != ACCEPTED:
            return JudgeResult(
                verdict=verdict,
                failed_case=i + 1,     # "Failed on test case 3"
                total_cases=len(test_cases),
                runtime=result.runtime_ms
            )
    
    # All passed!
    return JudgeResult(
        verdict=ACCEPTED,
        passed_cases=len(test_cases),
        runtime=max_runtime,           # worst case runtime
        memory=max_memory              # worst case memory
    )
```

### The Runtime Reporting Problem

When LeetCode shows "Runtime: 145ms, faster than 87% of users" — how is that calculated?

```
Naive: report the runtime of the user's solution
Problem: non-deterministic! Same code runs at 145ms vs 200ms
         depending on server load

Solution: Run the solution MULTIPLE TIMES, take the median

for _ in range(5):
    run test cases → collect runtime
take median of 5 runs → stable, reproducible measurement

Why median not average? Outliers (GC pause, context switch)
won't skew the result.
```

The **"faster than X%"** percentile is computed by comparing against historical runs stored in a Redis Sorted Set:

```python
# After judging
redis.zadd("runtimes:problem_001:python", 
           {submission_id: runtime_ms})

# Compute percentile
rank = redis.zrank("runtimes:problem_001:python", submission_id)
total = redis.zcard("runtimes:problem_001:python")
percentile = (rank / total) * 100

# "Faster than 87.3% of Python submissions"
```

---

## Problem 4: Anti-Cheat — Detecting Plagiarism 🕵️

### The Story

During a campus coding contest, Divya notices something suspicious: 47 students submitted the **exact same solution** to a hard problem within 2 minutes of each other.

### Layer 1: Exact Match Detection (Trivial)

```python
import hashlib

def check_exact_copy(code: str, problem_id: str) -> bool:
    code_hash = hashlib.sha256(code.encode()).hexdigest()
    
    # Check if this exact hash was submitted before
    existing = redis.get(f"submission_hash:{problem_id}:{code_hash}")
    if existing:
        return True, existing  # (is_copy, original_submission_id)
    
    # Store this submission's hash
    redis.set(f"submission_hash:{problem_id}:{code_hash}", 
              current_submission_id)
    return False, None
```

But students are smart. They change variable names:
```python
# Original:          # Copy with renamed vars:
def solve(n, k):     def solution(x, y):
    res = n + k          ans = x + y
    return res           return ans
```

Same logic. Different hash.

### Layer 2: Structural Similarity (Intermediate)

Normalize the code before hashing:
1. Remove comments
2. Rename all variables to generic names (var_1, var_2...)
3. Remove whitespace
4. Hash the normalized form

```python
def normalize_python(code: str) -> str:
    import ast, astor
    
    tree = ast.parse(code)
    
    # Rename all variables to var_0, var_1, etc.
    renamer = VariableRenamer()
    renamer.visit(tree)
    
    # Convert back to code (normalized form)
    normalized = astor.to_source(tree)
    return normalized

# Now exact structural copies have the same hash
# even with different variable names
```

### Layer 3: MOSS (Measure of Software Similarity)

For serious academic plagiarism detection, the industry standard is **MOSS** (Stanford's algorithm). It works on **fingerprinting substrings**:

```
1. Generate all k-grams (subsequences of length k) from the code
2. Hash each k-gram
3. Select a subset of hashes as "fingerprints" (using winnowing)
4. Two submissions with many shared fingerprints → similar code

Advantage: handles reordering, insertion, deletion
           not fooled by variable renaming or reformatting
```

This is the same algorithm used by universities worldwide to detect essay plagiarism.

### What Happens After Detection?

```
Similarity > 95%:  Flag for manual review, notify contest admin
Similarity > 99%:  Auto-flag both submissions, hold verdicts
Similarity = 100%: Exact copy — auto-disqualify (policy dependent)
```

The **judging system doesn't auto-penalize** — that's a human decision. The system flags and provides evidence.

---

## Problem 5: Partial Scoring 🏆

### The Story

LeetCode is one model. But **ICPC (International Collegiate Programming Contest)** and **IOI** have partial scoring: you get points based on how many test cases pass, not just all-or-nothing.

```
Problem: 100 points total, 10 test cases (10 points each)

User solves 7/10 test cases → 70 points
(maybe they have the right algorithm but wrong edge case handling)
```

### Implementation

```python
def judge_with_partial_scoring(code, problem_id, language):
    test_cases = cache.get(problem_id)
    total_score = 0
    results = []
    
    for test_case in test_cases:
        result = run_single(code, test_case.input, language)
        passed = judge(result.output, test_case.expected_output)
        
        if passed:
            total_score += test_case.points  # each case has a point value
        
        results.append({
            "case_id": test_case.id,
            "passed": passed,
            "points": test_case.points if passed else 0,
            "runtime": result.runtime_ms
        })
    
    return {
        "total_score": total_score,
        "max_score": sum(tc.points for tc in test_cases),
        "breakdown": results  # shown in the UI
    }
```

### Subtask Architecture

IOI-style problems group test cases into **subtasks** of increasing difficulty:

```
Subtask 1 (20 pts): n ≤ 100       (brute force passes)
Subtask 2 (30 pts): n ≤ 10,000    (O(n²) passes)
Subtask 3 (50 pts): n ≤ 10^6      (O(n log n) needed)

Rule: You must pass ALL cases in a subtask to earn its points.
      Passing subtask 1 + 3 but not 2 → only 20 points.
```

---

## The Complete Judging Pipeline

Putting it all together — what happens from "worker gets job" to "verdict is stored":

```
┌─────────────────────────────────────────────────────────────┐
│                  JUDGING PIPELINE                            │
│                                                             │
│  1. Worker receives job                                     │
│          │                                                  │
│  2. Download test cases (from cache or S3)                  │
│          │                                                  │
│  3. Compile code (if compiled language)                     │
│     C++: g++ -O2 solution.cpp -o solution  (~500ms)        │
│     Java: javac Solution.java               (~800ms)        │
│     Python: no compile needed               (0ms)           │
│          │                                                  │
│  4. For each test case:                                     │
│       a. Run code in container with input                   │
│       b. Enforce time + memory limits                       │
│       c. Capture stdout as user_output                      │
│       d. Run judge (token/float/special/interactive)        │
│       e. Record verdict per test case                       │
│       f. If WA/TLE/MLE → stop (unless partial scoring)     │
│          │                                                  │
│  5. Compute final verdict + runtime + memory stats          │
│          │                                                  │
│  6. Anti-cheat similarity check (async, doesn't block)      │
│          │                                                  │
│  7. Write to Redis (immediate) + PostgreSQL (batched)       │
│          │                                                  │
│  8. Push to user via WebSocket                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 📚 Chapter 6 Summary

| Problem | Solution |
|---|---|
| Whitespace/format differences | Token Judge (default) |
| Floating point precision | Float Judge with epsilon |
| Multiple valid answers | Special Judge (custom checker) |
| Interactive problems | Piped bidirectional communication |
| Test case storage | S3 + local worker cache with ETags |
| Runtime fairness | Median of 5 runs |
| Runtime percentile | Redis Sorted Set |
| Plagiarism (exact) | SHA-256 hash matching |
| Plagiarism (structural) | AST normalization + MOSS fingerprinting |
| Partial scoring | Per-test-case points + subtask grouping |

**The core insight:**
> Judging is not string comparison. It's a **pluggable system** where the judge type is selected per-problem. The hardest part isn't the infrastructure — it's defining what "correct" even means for each problem.

---

**Next up → Chapter 7: The Complete Picture** — We'll draw the full system diagram end-to-end, revisit every component together, and do a **mock HLD interview walkthrough** so you can answer any variation of this question confidently. This is where everything clicks into one coherent story.

---

# Chapter 7: The Complete Picture — "Everything Clicks" 🎯Let me build the complete system diagram first, then walk you through the full interview prep.That's the full system in one view. Now let's make sure you can **own this topic in any interview**.

---

## The Complete Story in One Breath

Before questions, internalize this single paragraph — it's your anchor:

> *"A code execution engine is fundamentally a **secure, async, distributed job processing system**. Users submit code via an API Gateway which rate-limits and routes to a Submission Service. That service enqueues a job to Kafka and immediately returns a job_id. Worker nodes consume jobs, run user code inside sandboxed containers using Linux namespaces + cgroups + seccomp, then pass output through a pluggable judging system. Results are written to Redis instantly and PostgreSQL in batches. Users get notified via WebSocket through Redis Pub/Sub. Workers auto-scale based on queue depth. Everything is fault-tolerant via ACK mechanisms, dead letter queues, circuit breakers, and idempotency."*

If you can say that fluently, you've already won 60% of the interview.

---

## Mock Interview — Every Question They'll Ask

---

### Q1: "Walk me through your design."

**The right structure — always start with requirements:**

> "Before I draw anything, let me clarify requirements. Are we designing for competitive programming like LeetCode, or general code execution like Replit? What languages? What scale — submissions per second? What's our latency target — under 3 seconds? Any special security concerns?"

Then walk the diagram **top to bottom**: Client → Gateway → Submission Service → Kafka → Workers → Judge → Storage → Notification.

**Never start with the database. Never start with "so we'll have microservices." Start with the user journey.**

---

### Q2: "How do you isolate user code? What if they run `rm -rf /`?"

Three-layer answer:

> "We use Linux containers with three kernel primitives layered together. **Namespaces** give the process an isolated view of the system — it can't see other processes, the real network, or the real filesystem. **cgroups** enforce hard resource limits: 256MB RAM, 2 CPU cores, 50 processes max — a fork bomb is contained. **seccomp** whitelists only safe syscalls — `socket()`, `fork()`, `mount()` are blocked at the kernel level. Even if an attacker bypasses namespace isolation, they still can't make the dangerous syscalls. For highest security, we can use Firecracker microVMs which give VM-level isolation at ~125ms boot time — AWS Lambda uses this."

---

### Q3: "Why a message queue? Why not just run code directly?"

This is a systems design taste question. The answer reveals whether you think in systems:

> "Three reasons. First, **decoupling** — the API returns instantly with a job_id. The user isn't blocked waiting for execution. Second, **load leveling** — during a contest, 10,000 users submit simultaneously. The queue absorbs that spike. Only 500 workers execute concurrently instead of spawning 10,000 containers at once. Third, **durability** — if a worker crashes mid-execution, the queue re-delivers the job automatically via the visibility timeout / ACK mechanism. Without a queue, a worker crash means a lost submission."

---

### Q4: "How do you scale during a contest spike?"

> "The Auto-Scaler watches Kafka queue depth as the primary metric. When depth spikes, it tells Kubernetes to increase worker pod replicas. We scale up aggressively — every second of wait time matters for users — but scale down conservatively with a cooldown to avoid flapping. Workers run on Spot/Preemptible instances for 60-70% cost reduction. Since jobs are re-deliverable, losing a spot instance mid-job is fine — the queue re-delivers it. We also pre-warm workers 5 minutes before known contest start times."

---

### Q5: "How do you handle an infinite loop?"

Three-layer timeout answer:

> "Three independent layers. Layer 1: the container itself uses Linux's `timeout` command — at 3 seconds it sends SIGKILL. Layer 2: the worker has a watchdog — if the container doesn't exit within 10 seconds, the worker force-kills it. Layer 3: the Kafka visibility timeout — if the worker crashes entirely, the job reappears in the queue after 30 seconds and is re-delivered to another worker. You need all three because each protects against different failure modes — a hung container, a crashed worker, and a network partition."

---

### Q6: "How do you shard the database?"

> "The submissions table is the hot table. I'd shard by **user_id** because most query patterns are per-user — 'show my submission history', 'did I solve this problem'. This keeps a user's data co-located on one shard. For leaderboards, I'd use a separate **Redis Sorted Set** per problem — `ZADD problem:42:leaderboard <score> <user_id>` gives O(log N) inserts and O(log N) rank queries. Redis Sorted Sets are perfect for this. The Postgres shards don't need to handle leaderboard reads at all."

---

### Q7: "What if the same job gets processed twice?"

> "We make processing **idempotent**. Before executing, the worker checks Redis for an existing result for that job_id. If it exists and is DONE, skip and ACK. If no result, acquire a distributed Redis lock using SET NX EX before executing. Only one worker can hold the lock — the other backs off. After execution, write the result and release the lock. The combination of the idempotency check and the distributed lock means even if Kafka delivers a job twice, it gets executed exactly once."

---

### Q8: "How do you check if the output is correct?"

> "String comparison is only the starting point. We have four judge types. **Token Judge** — the default — tokenizes both outputs and compares, ignoring whitespace differences. **Float Judge** — for geometry/math problems — checks that values agree within an epsilon, using both absolute and relative error. **Special Judge** — a custom checker program written by the problem setter — validates correctness for problems with multiple valid answers like 'print any valid topological sort'. The checker itself runs in a second sandbox. **Interactive Judge** — for problems that require back-and-forth communication — runs the user program and judge program simultaneously connected by Unix pipes."

---

### Q9: "What's your caching strategy?"

> "Two layers. **Redis** for hot data — active job results with 1-hour TTL, rate limit counters, distributed locks, WebSocket pub/sub. Sub-millisecond reads, handles 100k+ ops/sec. **PostgreSQL read replicas** for persistent reads — submission history, leaderboards fed by Redis, problem metadata. Workers cache test cases locally using S3 ETags — if the ETag hasn't changed, use the local copy; if it has, re-download. This avoids an S3 round-trip on every submission for the same problem."

---

### Q10: "How do you prevent a cascade failure?"

> "Two patterns. **Circuit Breaker** — if the DB fails, workers detect it and open the circuit: subsequent calls fail fast instead of waiting. Results buffer to Redis during the outage, then flush to Postgres when the circuit closes. This prevents the thundering herd when the DB comes back. **Exponential backoff with jitter** — when retrying, each attempt waits 2^n seconds, randomized ±50%. Without jitter, all 500 workers retry simultaneously at the same second, re-crushing the DB. With jitter, retries spread across a window."

---

## The Cheat Sheet — Every Design Decision & Why

```
DECISION                 CHOICE              WHY
─────────────────────────────────────────────────────────────
Sandbox tech           Container            Fast (50ms), cheap, good isolation
                       + Firecracker        For max security (AWS Lambda does this)

Queue tech             Kafka                High throughput, replay for re-judging
                       RabbitMQ/SQS         Simpler, fine for moderate scale

Worker scaling         K8s HPA on           Queue depth is the real signal,
                       queue depth          not CPU

Spot instances         Yes for workers      Jobs are re-deliverable → crash is ok

DB sharding key        user_id              Most queries are per-user

Leaderboard storage    Redis Sorted Set     O(log N) rank, perfect data structure

Result notification    WebSocket            Instant push vs polling overhead
                       + Redis Pub/Sub      Decouples worker from connection state

Idempotency            Redis SET NX EX      Distributed lock → exactly-once execution

Retry poison pills     Dead Letter Queue    After 3 failures, isolate bad job

Float comparison       Relative + absolute  Handles both small and large numbers
                       epsilon              correctly

Anti-cheat             AST normalization    Catches variable renaming, whitespace
                       + MOSS              tricks, structural copies

Language pools         Separate queues      C++ users don't wait behind Python jobs
                       per language

Test case storage      S3 + ETag cache      Avoids repeated downloads; invalidates
                                           when problem setter updates test cases
```

---

## The Narrative Arc — Your Full Story

If an interviewer gives you 45 minutes, here's the exact sequence:

1. **Clarify requirements** — scale, languages, latency, security level (2 min)
2. **Identify the core challenge** — untrusted code, 4 pillars: isolation, performance, fairness, scalability (2 min)
3. **Isolation** — containers, namespaces + cgroups + seccomp, why not VMs (5 min)
4. **High-level architecture** — client → gateway → service → queue → workers → judge → storage, explain the async pattern (8 min)
5. **Scaling** — horizontal workers, auto-scaling on queue depth, spot instances, language pools (5 min)
6. **Fault tolerance** — timeout layers, ACK/re-delivery, idempotency, DLQ, circuit breaker (5 min)
7. **Judging** — token/float/special/interactive, test case storage, S3+cache (5 min)
8. **Deep dives** — sharding, leaderboard (Redis sorted set), WebSocket notification, anti-cheat (10 min)
9. **Trade-offs** — Kafka vs RabbitMQ, containers vs Firecracker, polling vs WebSocket (3 min)

---

## 📚 Full Series Summary — 6 Chapters, One System

| Chapter | Core Insight |
|---|---|
| 1. The Problem | This is a security + distributed systems problem, not a "run code" problem |
| 2. Isolation | Containers = namespaces + cgroups + seccomp. Fast, cheap, good isolation |
| 3. Architecture | Producer-Consumer via Kafka. Async execution. WebSocket for results |
| 4. Scaling | Queue absorbs spikes. Workers scale horizontally. DB scales with replicas + sharding |
| 5. Fault Tolerance | 3-layer timeouts. ACK for durability. Idempotency. DLQ. Circuit breaker |
| 6. Judging | Pluggable judge types. S3+cache for test cases. Redis Sorted Set for leaderboards |

**You now know this system end-to-end** — from why `rm -rf /` was the founding disaster, to why Redis Sorted Sets are perfect for leaderboards. You didn't memorize it. You *derived* it, problem by problem. That's what makes an answer in an interview land with confidence. 🎯

