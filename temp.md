# LicenseApp — Hosting Migration Analysis
### InMotion VPC  vs.  AWS Serverless (SST v3)

---

## Executive Summary

This document helps you decide between staying on **InMotion VPC** (current setup) or moving to **AWS Serverless**. It covers what each component does today, how it translates to AWS, what the user experience looks like on both sides, and an honest list of what you gain and what you give up.

> **Context:** LicenseApp handles around 20–100 license generation requests per month. The current system runs on a single VPS server using a file-based, polling-driven approach between a PHP frontend and a Python backend.

---

## Section 1: Current Architecture — InMotion VPC

### How It Works Today

Everything runs on one server. The PHP layer accepts requests from the browser, drops them as files into a folder, and the Python backend picks them up from that folder, processes them, and drops results back into another folder. PHP then serves those results back to the browser. Think of it as two processes talking to each other by leaving notes on a shared desk.

### Component Breakdown

| Component | Technology | What It Does |
|---|---|---|
| `submit.php` | PHP | Accepts the browser's request, wraps it in a WEV envelope, drops it in `web_drop/new/` |
| `fetch_next.php` | PHP | Python calls this to pick up the next job from the drop folder |
| `ack.php` | PHP | Python posts its finished result here; PHP writes it to `results/` |
| `status.php` | PHP | Browser keeps calling this every 2 seconds asking "is it done yet?" |
| `download.php` + `prepare_download.php` | PHP | Secure, ticketed download of the generated `.lic` file |
| `upload_license.php` | PHP | Python uploads the finished license file here for PHP to serve |
| `unified_fetcher.py` | Python daemon | Runs 24/7 in Docker, constantly polling `fetch_next.php` for new jobs |
| `LicenseManager.py` | Python daemon | The brain — validates, signs, and generates licenses; reads/writes SQLite |
| `signer.py` | Python | Signs the license JSON using an Ed25519 private key |
| `web_publisher.py` | Python daemon | Uploads the `.lic` file and posts the result back to `ack.php` |
| `manager.sqlite` | SQLite | The database — stores all machines, vouchers, orgs, and seat records |
| Docker Compose | 5 containers | Keeps fetcher, manager, publisher, email courier, and cleanup running |

### Communication Flow

```
Browser  →  submit.php  →  [disk: web_drop/new/]
unified_fetcher.py  →  fetch_next.php  →  [disk: incoming/vetted/]
LicenseManager.py  →  signer.py  →  [disk: dl_store/ + bridge/outbox/]
web_publisher.py  →  upload_license.php  +  ack.php  →  [disk: results/]
Browser  ←  status.php (polling every 2s)  ←  [disk: results/res_{hash}.json]
```

---

## Section 2: Proposed Architecture — AWS Serverless (SST v3)

### How It Works on AWS

No more daemons, no more drop folders, no more polling. When the browser makes a request, AWS API Gateway wakes up a Python Lambda function, it does all the work right there and then, and returns the download link in the same response. The browser never has to wait and check again — it gets everything in one go.

### Component Translation — Legacy to AWS

| Legacy Component | Legacy Tech | AWS Replacement | AWS Service |
|---|---|---|---|
| `submit.php` | PHP file on VPS | `POST /api/v1/license/request` handler | API Gateway V2 + Lambda |
| `web_drop/` folder | Local filesystem queue | **Eliminated** — request handled synchronously | — |
| `fetch_next.php` | PHP polling bridge | **Eliminated** — Lambda is called directly | — |
| `unified_fetcher.py` daemon | Docker container (always-on) | **Eliminated** — Lambda wakes per request | — |
| `ack.php` | PHP result writer | **Eliminated** — Lambda returns result in HTTP response | — |
| `status.php` (polling) | PHP endpoint, polled every 2s | **Eliminated** — response is synchronous | — |
| `LicenseManager.py` | Python daemon, file-watcher | Python Lambda function (runs per request) | AWS Lambda |
| `signer.py` (Ed25519) | Python, reads `.pem` from disk | Same logic; key loaded from Parameter Store | Lambda + SSM |
| `manager.sqlite` | SQLite file on disk | Managed NoSQL database | AWS DynamoDB |
| `dl_store/` directory | Local disk folder | Private object storage | AWS S3 |
| `upload_license.php` | PHP file receiver | Lambda writes directly to S3 | Eliminated |
| `prepare_download.php` | PHP ticket minter | Pre-signed S3 URL (built-in) | AWS S3 Presigned URL |
| `download.php` | PHP file streamer | Client downloads directly from S3 | AWS S3 |
| ACK + cleanup loop | `web_publisher.py` daemon | S3 lifecycle rules handle cleanup | AWS S3 Lifecycle Policy |
| Docker Compose (5 containers) | `docker-compose.yml` | **Eliminated** — Lambda is on-demand | — |
| `secrets/` folder (`.pem`, tokens) | Files on disk | Encrypted secret store | AWS SSM Parameter Store |

> **What gets eliminated:** 3 Python daemons, 5 Docker containers, 3 disk directories (`web_drop/`, `bridge/outbox/`, `results/`), and all polling logic. The PHP layer is fully replaced by API Gateway.

---

## Section 3: User Journey Comparison

### Journey 1 — Individual License Purchase

#### Legacy Flow (InMotion VPC) — 9 Steps

**Step 1** — User generates a `hw_hash` file from their desktop app, uploads it to the website, and completes Stripe checkout.

**Step 2** — Browser POSTs to `submit.php`. PHP wraps it in a WEV envelope, drops it in `web_drop/new/`, and immediately returns an `idempotency_key` to the browser.

**Step 3** — Browser starts a `setInterval` loop, calling `status.php` every 2 seconds. Gets back `status: pending`. Again. And again.

**Step 4** — `unified_fetcher.py` (running 24/7) wakes up on its next cycle, calls `fetch_next.php`, pulls the job, and saves it to `incoming/vetted/`.

**Step 5** — `LicenseManager.py` detects the new file, reads the Stripe session and `hw_hash`, queries SQLite, validates the payment, updates records, calls `signer.py` to sign the license, writes `machine_license.lic` to `dl_store/`, and writes the result to `bridge/outbox/`.

**Step 6** — `web_publisher.py` sees the result, calls `upload_license.php` to push the `.lic` file to the web server, then calls `ack.php` with the result JSON.

**Step 7** — `ack.php` writes `results/res_abc123.json` to disk.

**Step 8** — On the next poll, `status.php` finds the result file and returns `status: complete`. The browser loop finally stops.

**Step 9** — User clicks download. Browser calls `prepare_download.php` (mints a 15-min ticket), then `download.php` streams the file. `download.php` also writes an ACK back to `web_drop/new/` so Python knows delivery succeeded.

> **Total: 9 network trips · 3 disk directories · Up to 60+ seconds of waiting · 5 always-on Docker containers**

---

#### AWS Serverless Flow — 2 Steps

**Step 1** — User generates `hw_hash`, uploads it, completes Stripe. Same as before.

**Step 2** — Browser POSTs to `/api/v1/license/request`. API Gateway wakes up the Lambda. Lambda validates the request, verifies Stripe, reads/writes to DynamoDB, signs the license (key from SSM), writes the `.lic` to S3, generates a presigned download URL, and returns HTTP 200 with `{ ok: true, download_url: "..." }`. Browser immediately starts the download directly from S3.

> **Total: 2 network trips · 0 disk directories · Under 3 seconds · 0 always-on servers**

---

### Journey 2 — Voucher Redemption

#### Legacy Flow (InMotion VPC) — 8 Steps

**Step 1** — User enters their `hw_hash` and voucher code (e.g. `VCH-XYZ-999`) on the Redeem page.

**Step 2** — Browser POSTs to `submit.php` with `scheme: Voucher-request:V1.0`. Returns `idempotency_key`. Polling begins.

**Step 3** — Browser polls `status.php` every 2 seconds. `pending`. `pending`. `pending`.

**Step 4** — `unified_fetcher.py` picks up the job and saves it to `incoming/vetted/`.

**Step 5** — `LicenseManager.py` checks SQLite: does the voucher exist? is it active? does this machine already have a seat? are there seats remaining? Claims a seat, generates the license, calls `signer.py`, writes the `.lic` to `dl_store/`, writes the result to `bridge/outbox/`.

**Step 6** — `web_publisher.py` uploads the `.lic` and posts the result to `ack.php`.

**Step 7** — Browser polling loop finally gets `status: complete`.

**Step 8** — Ticketed download via `prepare_download.php` → `download.php` → ACK written back to disk.

> **Total: 8 network trips · Same polling wait · Same 5 Docker containers running the whole time**

---

#### AWS Serverless Flow — 2 Steps

**Step 1** — User enters `hw_hash` and voucher code. Same as before.

**Step 2** — Browser POSTs to `/api/v1/voucher/request`. Lambda checks DynamoDB for the voucher, atomically claims a seat (DynamoDB's conditional writes make sure two people can't claim the same last seat simultaneously), signs the license, writes it to S3, and returns the presigned download URL in the same response.

> **Total: 2 network trips · Under 3 seconds · Seat race condition handled correctly by default**

---

## Section 4: Side-by-Side Comparison

| | InMotion VPC (Current) | AWS Serverless (Proposed) |
|---|---|---|
| **Architecture style** | Polling, async, file-based | Synchronous, event-driven, stateless |
| **Request flow** | 9 steps, multiple disk reads/writes | 2 steps, in-memory processing |
| **User wait time** | 10–60+ seconds | Under 3 seconds |
| **Compute model** | 5 always-on Docker containers | 0 always-on servers; Lambda wakes per request |
| **Database** | SQLite (single file, no replication) | DynamoDB (managed, replicated, auto-scaling) |
| **File storage** | Local disk directories | AWS S3 (durable, globally accessible) |
| **Secret management** | `.pem` and token files on disk | AWS SSM Parameter Store (encrypted, IAM-scoped) |
| **Scaling** | Manual (upgrade VPS plan) | Automatic |
| **Availability** | Depends on VPS uptime | 99.95%+ SLA, multi-AZ |
| **Cost at 100 req/month** | ~$15–30/mo (VPS always-on) | ~$0.00/mo (within free tier) |
| **Maintenance** | Manual OS updates, Docker, PHP, Python deps | Zero server maintenance |
| **Code complexity** | High (daemons, bridges, WEV envelopes) | Low (one handler per endpoint) |
| **Deployment** | SSH + `docker compose up` | `npx sst deploy` |
| **Rollback** | Manual file restoration via SSH | Single command deploy of previous version |

---

## Section 5: Advantages & Disadvantages

### AWS Serverless

#### ✅ Advantages

**It costs nothing at your scale.**
You are running 20–100 requests a month. AWS Lambda gives you 1 million free requests every single month, forever — not a trial, just permanently free. DynamoDB gives you 25 GB of free storage, also permanently. At your volume, your AWS bill will genuinely be $0.

**Your users stop waiting.**
Right now someone submits a license request and stares at a loading screen for anywhere between 10 and 60 seconds because your server has to pass files around between processes before anything gets done. On AWS, they click submit and the download link comes back in the same breath — under 3 seconds. That's a completely different experience.

**You stop paying for a server that's doing nothing.**
Your VPS runs 24 hours a day, 7 days a week, 365 days a year — even when nobody is requesting a license. You're essentially paying a monthly fee for a server to sit idle. Lambda only runs when someone actually makes a request. For 100 requests a month, that's a lot of money for a lot of nothing.

**Your signing key is actually safe.**
Right now, the Ed25519 private key that signs every license lives as a plain file on the server. If someone ever gets into that server, they get the key — and can sign fake licenses. On AWS, that key lives in Parameter Store, which is encrypted, access-controlled, and completely separate from your code. The code never even sees the raw key directly.

**Two users can't accidentally claim the same last voucher seat.**
This is a quiet bug in the current setup. If two people hit "Redeem" at the exact same moment on the last available seat, SQLite can give both of them the seat because it doesn't handle that kind of simultaneous conflict well. DynamoDB handles this correctly by design — it checks and claims the seat in a single atomic operation.

**You delete half the codebase.**
Three Python daemons disappear. Five Docker containers disappear. Three disk directories disappear. All the WEV envelope wrapping, the polling loops on the browser, the `idempotency_key` tracking — gone. What's left is just the actual business logic. Less code means fewer bugs and less to maintain.

**If you need to deploy a fix, you just run one command.**
Currently a deployment means SSH-ing into the server, pulling code, and restarting Docker containers — and if something goes wrong, rolling back is manual. With SST, you run `sst deploy` and it's done. Roll back? Run `sst deploy` on the previous version.

#### ❌ Disadvantages

**You have to do a migration.**
The system works today. Moving to AWS means refactoring `LicenseManager.py` to talk to DynamoDB instead of SQLite, and to write files to S3 instead of disk. It also means moving your existing database records across. It's not a flip-of-a-switch change — there's real work involved.

**AWS has a learning curve.**
If you've never worked with IAM roles, DynamoDB, or SSM Parameter Store before, there will be a period of figuring things out. The console can be overwhelming at first. It's very learnable, but it's not zero friction.

**The first request after a quiet period might be slightly slower.**
If nobody has requested a license in a while, Lambda has to "wake up" on the first call. This takes maybe 200–800ms extra, just that one time. Everything after that is fast. At 100 requests a month it's barely noticeable, but it's worth knowing.

**You're on AWS's terms.**
AWS could change pricing. AWS could have an outage (rare, but possible). Your architecture becomes dependent on their services. Leaving later would mean redoing the DynamoDB and S3 integrations. You trade control for convenience.

**The email inbox feature needs a rethink.**
The current system has a loop that monitors an email inbox for license requests sent via email. On AWS, you can't just run a background process watching an inbox. You'd need to replace that with AWS SES (Simple Email Service) and a trigger — it's doable, but it's an extra piece to sort out if that feature matters to you.

**Debugging feels different.**
Instead of running `docker logs -f manager` and reading a live log on your server, you're reading CloudWatch logs in a browser. It's not harder once you're used to it, but it's different — and for some people, that directness of SSH + local logs is something they really value.

---

### InMotion VPC (Current)

#### ✅ Advantages

**Nothing needs to change right now.**
The system is running. Users are getting their licenses. If it ain't broke and you don't have the bandwidth to deal with a migration, that's a completely valid reason to stay put.

**You can see exactly what's happening at any moment.**
SSH in, run `docker compose logs -f manager`, and you see every single thing happening in real time. It's direct and honest.

**You own the whole thing.**
No cloud dependency. No IAM to figure out. No AWS bill to monitor. If InMotion has an outage, you can in theory move to any other VPS provider. Everything is yours.

**The email license request feature already works.**
If people are sending license requests via email, the IMAP polling loop already handles that. It just works.

#### ❌ Disadvantages

**You're paying every month for a server that's mostly idle.**
100 requests a month means the server is actually doing license work for maybe a few minutes out of the entire month. The rest of the time it's just running and costing money.

**Users are waiting a long time for no good reason.**
The 10–60 second wait isn't because license generation is slow — it's because files have to get passed between processes through a folder queue, with a daemon waking up on a timer. The actual signing takes milliseconds. The rest is just waiting in line.

**The whole thing goes down if one thing goes wrong.**
If the VPS has a problem — a bad deploy, a full disk, a crashed container — everything stops. There's no redundancy. One server, one point of failure.

**Your signing key is on the same server as your web code.**
That's a risk. If someone finds a vulnerability in the PHP layer and gets shell access, they get the private key. That's the worst-case scenario for a licensing system.

**The codebase is genuinely complex for what it does.**
File-based queues, WEV envelopes, five Docker containers, three daemons watching folders — this is a lot of moving parts to generate a signed JSON file. If a new developer looks at this, it takes a while to understand what connects to what.

---

## Section 6: Cost Analysis

### Monthly Cost at 100 Requests/Month

| Service | InMotion VPC | AWS Serverless | Notes |
|---|---|---|---|
| Compute | ~$15–30/mo | $0.00/mo | Lambda: 1M free req/mo; 100 used |
| Database | Included in VPS | $0.00/mo | DynamoDB: 25 GB always-free |
| File Storage | Included in VPS | $0.00/mo | S3: 5 GB free; license files are tiny |
| Secrets Management | Free (files on disk) | $0.00/mo | SSM: free for standard parameters |
| Bandwidth | Included in VPS | ~$0.00/mo | 100 × ~5 KB files = negligible |
| SSL / TLS | Let's Encrypt | $0.00/mo | API Gateway provides TLS by default |
| **TOTAL** | **~$15–30/mo** | **~$0.00/mo** | AWS stays in free tier at this volume |

> **Note:** AWS Lambda's free tier (1 million requests/month) and DynamoDB's always-free tier (25 GB storage) are **permanent** — not 12-month introductory offers. At 100 license requests/month, LicenseApp uses less than 0.01% of the free Lambda quota.

---

## Section 7: Recommendation

### Our Recommendation: Migrate to AWS

For a system doing 20–100 license requests a month, AWS is the better home — not because VPS is bad, but because the scale doesn't justify its cost or complexity.

The monthly VPS fee goes away. The user wait time drops from up to a minute to under 3 seconds. The private signing key moves somewhere genuinely secure. And more than half the codebase — the daemons, the drop folders, the polling loops — simply gets deleted.

### When Staying on InMotion Makes Sense

- You need the IMAP email request feature and don't want to deal with replacing it.
- The team is stretched and there's no good window to do a migration safely.
- You prefer having direct SSH access to the server and aren't comfortable with AWS concepts yet.

---

### Bottom Line

> For a system handling 20–100 license requests per month, AWS Serverless eliminates the monthly server cost entirely, reduces user wait time from up to 60 seconds to under 3 seconds, removes most of the codebase complexity, and properly secures the signing key — all for **$0.00/month**.
