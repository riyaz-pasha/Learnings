# 🔹 1. Upload Flow (Multipart Upload with Presigned URLs)

### Why multipart?

* Handles **large files** reliably (up to 5 TB).
* Retry **per-part** instead of retrying the whole file.
* Supports parallel uploads.

### Step-by-step

1. **Client → Storage Service**

   * Request upload: `{ filename, size, type }`.
   * Example: User uploads `video.mp4` (2 GB).

2. **Storage Service**

   * Ensure bucket/prefix exists (⚠️ don’t create a bucket per user — use one bucket with `userId/` prefixes).
   * Call `CreateMultipartUpload` on S3 → get `UploadId`.
   * Decide **part size**:

     * Min: **5 MB** (except last).
     * Max: **10,000 parts** → if you fix at 5 MB, you can only support \~50 GB. For larger, use **32–128 MB** parts.
   * Compute `partCount = ceil(size / partSize)`.
   * Generate **presigned URLs** (one per part):

     * Each URL = `UploadPart` (includes `uploadId`, `partNumber`).
     * You cannot upload all chunks with a single presigned URL.
   * Return `{ uploadId, partSize, urls[] }` to client.

3. **Client**

   * Slice file into chunks.
   * Upload each chunk via its presigned URL (`PUT`).
   * On success, read `ETag` from response headers (needs CORS `ExposeHeaders: ETag`).
   * Collect `{ partNumber, eTag }`.

4. **Client → Storage Service**

   * Send list of `{ partNumber, eTag }` after all parts uploaded.

5. **Storage Service**

   * Call `CompleteMultipartUpload(uploadId, parts)`.
   * At this moment, the object is **assembled and visible** in S3.

6. **(Optional) Error case**

   * If upload abandoned → call `AbortMultipartUpload` to free storage.

### Notes

* Presigned URLs are valid for up to **7 days**. Consider **batching** (e.g., issue 100 at a time).
* Store an **upload session** in your DB: `{ uploadId, fileId, userId, partSize, createdAt, status }`.
* Support retries: if a part fails, just re-`PUT` it (same URL or ask for a fresh one if expired).
* Metadata (e.g., `Content-Type`, `SSE`) should be set at `CreateMultipartUpload` time, not per part.
* Enable **S3 Versioning** to support conflict resolution & sync.

---

# 🔹 2. Download Flow (Presigned GET + Range Support)

### Why presigned GET?

* Keeps objects private.
* Client downloads directly from S3 without routing through your backend.
* Works with **range requests** (resume, seek, parallel chunks).

### Step-by-step

1. **Client → Storage Service**

   * Request download: `{ fileId }`.

2. **Storage Service**

   * Look up file metadata in DB (size, type, key, version, etc.).
   * Generate **presigned GET URL** (short TTL: 5–15 min).
   * Optionally override headers:

     * `response-content-disposition=attachment; filename="name.ext"`
     * `response-content-type=<mime>`

3. **Client**

   * First `HEAD` request (or tiny `Range: bytes=0-0` GET) to fetch:

     * `Content-Length` (total size)
     * `Accept-Ranges: bytes` (confirms range support)
     * `ETag` (for integrity/resume check)
     * `Last-Modified`
   * Plan download strategy:

     * **Sequential GET** if small.
     * **Range GETs** in parallel if large (`Range: bytes=start-end`).

4. **Parallel/resumable download**

   * Split into 8–32 MB chunks.
   * Fire multiple `Range` GETs concurrently (4–8 streams typical).
   * Stitch in order.
   * If presigned URL expires mid-download, request new URL from service → verify `ETag`/`Last-Modified` unchanged → resume.
   * Optional: use `If-Range: "<etag>"` for safe resume.

### Browser CORS

Expose these headers:
`ETag, Content-Length, Content-Range, Accept-Ranges, Last-Modified, x-amz-request-id, x-amz-id-2`

---

# 🔹 3. Sync Flow (Multi-Device, Online/Offline)

### Problem

* User has multiple devices (desktop, phone, tablet).
* Upload on one device must propagate to others (online instantly, offline later).

### Solution = **ChangeLog + Notifications**

#### ChangeLog Table (append-only, monotonic cursor)

* `change_id` (bigint, increasing) → **cursor**
* `user_id`
* `file_id`
* `op` (`CREATE`, `UPDATE`, `DELETE`)
* `s3_key`, `version_id`, `etag`, `content_length`, `content_type`, `checksum`
* `metadata` (filename, folder, etc.)
* `at` (timestamp)
* `actor_device_id` (optional)

#### Workflow

1. **Upload complete (service)**

   * After `CompleteMultipartUpload`, service writes to `ChangeLog` and publishes `FileCreated/Updated/Deleted` event.

2. **Online devices**

   * Notification service pushes `{ userId, latestChangeId }` via WebSocket/FCM/APNs.
   * Device then calls `/delta?since=<lastCursor>` to fetch new changes.

3. **Offline devices**

   * When back online → call `/delta?since=<lastCursor>`.
   * Service queries `ChangeLog` → returns ordered list of changes.
   * Devices apply them sequentially.

4. **/delta Response**

```json
{
  "items": [
    {
      "changeId": 12345,
      "fileId": "f_abc",
      "op": "CREATE",
      "key": "users/123/files/f_abc",
      "versionId": "3L4k...",
      "etag": "9b2cf...",
      "size": 734003200,
      "contentType": "application/zip",
      "metadata": { "name": "report.zip" }
    }
  ],
  "nextCursor": 12345
}
```

* Return **metadata + versionId**, not long-lived presigned URLs.
* Clients fetch presigned URLs on demand.

5. **Conflict resolution**

* Enable **S3 Versioning**.
* On update, client must include base version (`If-Match` ETag or versionId).
* If mismatch → reject with `409 Conflict`. Device/app can resolve by overwriting, keeping both, or prompting merge.

6. **Deletes**

* Write tombstones (`op=DELETE`) in ChangeLog → so offline devices can sync deletions.

7. **Retries & deduplication**

* ChangeLog is the source of truth.
* Notifications are just nudges (at-least-once, may duplicate).
* Devices keep `lastAppliedChangeId` locally to ensure idempotency.

---

# 🔹 4. Security, Integrity & Operations

* **Presigned URL TTLs:** keep short (minutes).
* **ETags:**

  * Single-part = MD5.
  * Multipart = not MD5 → use checksums if needed.
* **Checksums:** use `x-amz-checksum-sha256` on upload + `x-amz-checksum-mode: ENABLED` on GET.
* **CORS:** configure to allow `PUT, GET, HEAD` and expose headers (`ETag`, `Content-Length`, `Content-Range`, etc.).
* **Abort stale uploads:** periodically call `ListMultipartUploads` + `AbortMultipartUpload`.
* **CloudFront (recommended):**

  * For global performance.
  * Use Signed URLs/cookies with Origin Access Control (OAC).
  * Still supports Range, resume, video streaming.

---

# 🔹 5. End-to-End API Sketch

**Start Upload**

```
POST /upload/init
{ filename, size, type }
→ { uploadId, partSize, presignedUrls[], key }
```

**Complete Upload**

```
POST /upload/complete
{ uploadId, parts: [{partNumber, eTag}] }
→ { fileId, changeId, versionId }
```

**Delta (sync)**

```
GET /delta?since=12340
→ { items: [...], nextCursor }
```

**Download**

```
POST /download-url
{ fileId, versionId }
→ { url, expiresIn: 600, suggestedFilename, contentType }
```

---

# ✅ Final Takeaway

* **Uploads:** multipart with presigned URLs (one per part), client collects ETags, backend completes.
* **Downloads:** presigned GET, `HEAD` for size/etag, then Range GETs (parallel/resumable).
* **Sync:** maintain a ChangeLog with a monotonic cursor; push online devices, pull deltas for offline; issue presigned GETs on demand.
* **Best practices:** short URL TTLs, S3 versioning, tombstones for deletes, optimistic concurrency, CORS headers, checksum validation, CloudFront for scale.

---

You can make uploads **pausable and resumable** by leaning on S3 **Multipart Upload** + a small amount of session state on your side. Key facts:

* Each **part** is independent. Already-uploaded parts don’t need to be sent again.
* A part is **all-or-nothing**: if the network drops mid-part, you must re-upload that **entire part** (you can’t range-resume inside a part).
* You can generate **new presigned URLs** for the **same `uploadId` + `partNumber`** at any time (e.g., after reconnect).

Below is a compact blueprint you can drop in.

---

## 1) Server: track an “upload session”

Keep enough state to rebuild progress if the client disappears.

**`upload_sessions`**

* `session_id` (uuid)
* `user_id`
* `bucket`, `key`
* `upload_id` (from S3 `CreateMultipartUpload`)
* `part_size_bytes`
* `expected_parts` (ceil(size/part\_size))
* `status` (`active|completed|aborted|expired`)
* `created_at`, `updated_at`
* optional: `file_size`, `content_type`, `checksum_total`

**`upload_parts`** (optional, but useful if you want server-side progress)

* `(session_id, part_number)` PK
* `etag`, `size`, `uploaded_at`
* optional: `checksum_sha256`

> You can also derive part status on demand from S3 via `ListParts(uploadId)`; storing it locally just avoids extra S3 calls.

---

## 2) APIs you’ll need

* **Init** → returns `sessionId`, `uploadId`, `partSize`, and **initial batch** of presigned URLs.
* **Refresh URLs** → given `sessionId` and a list of **pending partNumbers**, return **fresh presigned URLs**.
* **Status** → returns **which parts are done** (from DB or S3 `ListParts`).
* **Complete** → takes `{ partNumber, eTag }[]` (or pulls from your DB) and calls `CompleteMultipartUpload`.
* **Abort** → cancels the upload (cleanup).

---

## 3) Client behavior (browser/mobile/desktop)

### Pause

* Stop dequeuing new part uploads; let in-flight parts finish (or **abort** fetches to stop immediately).
* Keep `sessionId`, `uploadId`, `partSize`, completed parts’ `{partNumber, eTag}` in local storage.

### Resume (same device, or after app restart)

1. Lookup `sessionId` (stored locally).
2. Call **Status**: `GET /upload/status?sessionId=...`

   * Or directly call S3 `ListParts(uploadId)` if your client has creds (usually not when using presigns).
3. Build the **remaining partNumbers** list.
4. Call **Refresh URLs** for the remaining parts (old URLs may have expired).
5. Re-enqueue only the **incomplete** parts.
6. After all done, call **Complete**.

### Resume (lost all local state)

* Client only knows `fileId` (or some external reference).
* Service queries `upload_sessions` by `(userId,fileId)` where `status=active`.
* Service returns `sessionId`, `uploadId`, `partSize`, and **which parts are done**.
* Client continues as above.

### Concurrency & retries

* Parallelism: 4–8 parts at a time is a good default.
* Retry policy: exponential backoff per part; if URL expired → call **Refresh URLs** and retry.
* If a `PUT` fails mid-transfer, **re-upload that whole part**.

---

## 4) Practical knobs

* **Part size**: choose with resume in mind. Smaller parts = finer-grained resume but more overhead. Sweet spot: **8–64 MiB** (mobile: 8–16 MiB; very large files: 64–128 MiB to keep ≤10,000 parts).
* **URL expiry**: minutes, not hours. Issue presigns in **batches** (e.g., 100 at a time) and refresh as needed.
* **Integrity**:

  * Prefer **checksums** on each `UploadPart` (`x-amz-checksum-sha256`). S3 will enforce, you store the value.
  * Always capture the **ETag** from each part response.
* **Idempotency**:

  * Make **Init** idempotent via a client-supplied `idempotencyKey` (or return an existing session if the same file is already uploading).
  * Make **Complete** safe to retry (store a “completed” marker; if called twice, no harm).
* **Lifetimes / cleanup**: auto-abort sessions idle for, say, 24–72 hours (cron: `ListMultipartUploads` + `AbortMultipartUpload`).

---

## 5) Edge cases and answers

* **Can I resume inside a part?** No. Re-upload that part. If you need super fine-grained resume, **reduce part size**.
* **User switched networks / lost internet?** Already-uploaded parts remain. On reconnect, **refresh presigns** and continue with remaining parts.
* **Do I need the client to tell me ETags?**

  * Option A (simple): yes; client collects `{partNumber, eTag}` and posts to **Complete**.
  * Option B (server-driven): server calls `ListParts(uploadId)` and completes without client sending ETags (handy when resuming after client crash).
* **What if URLs expired during pause?** They’re just signatures. **Reissue** for the same uploadId/partNumber.
* **Can I auto-complete without client?** Yes: a backend worker can poll `ListParts` until all expected parts are present and then call **Complete**. (Useful for “fire-and-forget” desktop agents.)

---

## 6) Minimal client pseudo (TypeScript-ish)

```ts
type Part = { partNumber: number; etag?: string };

class MultipartUploader {
  constructor(private svc: ServiceAPI, private file: FileLike) {}

  session!: { sessionId: string; uploadId: string; partSize: number };
  parts: Part[] = [];

  async initOrResume(idempotencyKey: string) {
    const existing = await this.svc.findActiveSession(idempotencyKey);
    if (existing) {
      this.session = existing.session;
      const done = await this.svc.status(this.session.sessionId);
      this.parts = this.planParts(this.file.size, this.session.partSize, done.partNumbersDone);
    } else {
      this.session = await this.svc.init({ name: this.file.name, size: this.file.size, type: this.file.type, idempotencyKey });
      this.parts = this.planParts(this.file.size, this.session.partSize, []);
    }
  }

  planParts(size: number, partSize: number, completed: number[]): Part[] {
    const total = Math.ceil(size / partSize);
    const set = new Set(completed);
    return Array.from({ length: total }, (_, i) => ({ partNumber: i + 1 }))
                .filter(p => !set.has(p.partNumber));
  }

  paused = false;

  pause() { this.paused = true; /* optionally abort in-flight fetches */ }
  resume() { this.paused = false; return this.loop(); }

  async loop(concurrency = 6) {
    const queue = [...this.parts];
    const workers = Array.from({ length: Math.min(concurrency, queue.length) }, () => this.worker(queue));
    await Promise.all(workers);
    // Either client posts etags or server uses ListParts:
    const etags = await this.svc.collectEtags(this.session.sessionId); // or keep locally
    await this.svc.complete(this.session.sessionId, etags);
  }

  private async worker(queue: Part[]) {
    while (!this.paused) {
      const next = queue.pop();
      if (!next) return;
      const start = (next.partNumber - 1) * this.session.partSize;
      const end = Math.min(start + this.session.partSize, this.file.size);
      const blob = this.file.slice(start, end);

      // get/refresh presign on demand
      const url = await this.svc.presignPart(this.session.sessionId, next.partNumber);

      try {
        const { etag } = await putPart(url, blob); // fetch PUT; read ETag
        await this.svc.markPartDone(this.session.sessionId, next.partNumber, etag);
      } catch (e) {
        if (isExpired(e) || isTransient(e)) {
          queue.push(next); // retry later; will presign again
        } else {
          throw e;
        }
      }
    }
  }
}
```

---

## 7) Mobile specifics (quick notes)

* **iOS**: use `URLSession` **background** transfers (survives app suspends). Keep `sessionId` in Keychain/UserDefaults; on relaunch, ask server for **Status** and continue.
* **Android**: use **WorkManager** (foreground service for big transfers), or libraries like OkHttp + `WorkManager`. Persist `sessionId` and resume exactly as above.

---

## 8) CORS & headers (for browsers)

* **AllowedMethods**: `PUT, GET, HEAD, POST, OPTIONS`
* **AllowedHeaders**: `*` (or at least `content-type, x-amz-checksum-sha256, x-amz-acl` if used)
* **ExposeHeaders**: `ETag, x-amz-request-id, x-amz-id-2`
* (For downloads) also expose: `Content-Length, Content-Range, Accept-Ranges, Last-Modified`

---

### TL;DR

* **Pause**: stop scheduling parts; keep `sessionId/uploadId`; let in-flight parts finish or abort.
* **Resume**: discover completed parts (`Status` or `ListParts`), **re-presign** remaining parts, re-upload only those, then `CompleteMultipartUpload`.
* **Cannot resume inside a part** → pick sane **part sizes** for good UX.
* Keep sessions short-lived, presigns short, and add retries/backoff + checksums for robustness.

If you want, I can adapt this into a tiny **server interface** (Java/Node) with concrete handlers for `init`, `status`, `presignPart`, `complete`, and `abort`.

---
