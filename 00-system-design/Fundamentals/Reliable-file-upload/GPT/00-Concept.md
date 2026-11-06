# Designing a reliable file upload service with S3 multipart uploads

The strongest design is to treat file uploading as a controlled, asynchronous workflow, not simply as an API that generates a presigned URL.

There are three separate responsibilities:

1. Your application/database owns the file's identity, metadata, permissions, upload lifecycle, and business status.
2. S3 owns the actual file bytes, temporary multipart data, and final assembled object.
3. The client transfers the bytes directly to S3 and reports when it believes the transfer is finished.

Your backend coordinates all three.

The most important design decision is:

> The client can tell your backend that an upload appears complete, but your backend must verify the upload with S3 before declaring the file successfully uploaded.

This avoids treating a client-side success message as proof that the file exists correctly.

Let's build this system step by step.

## 1. First, understand what we are actually storing

Imagine a document management system.

A user uploads a 500 MB PDF.

We need two types of storage.

Database: PostgreSQL

Stores information about the file:

- File ID
- Owner/user ID
- Original filename
- Content type
- Expected file size
- S3 bucket and object key
- Upload status
- Checksum
- Created and completed timestamps

Object storage: S3

Stores the actual bytes:

```
s3://my-files-bucket/
    users/
        user-123/
            files/
                file-abc/
                    original.pdf
```

The database might contain:

| Column         | Example                                    |
| -------------- | ------------------------------------------ |
| id             | file-abc                                   |
| user_id        | user-123                                   |
| filename       | original.pdf                               |
| content_type   | application/pdf                            |
| expected_size  | 524288000                                  |
| storage_bucket | my-files-bucket                            |
| storage_key    | users/user-123/files/file-abc/original.pdf |
| status         | UPLOADING                                  |

Notice something important:

We store one logical file record in the database, not one database record for every physical chunk.

We will discuss optional chunk metadata later.

Also, S3 multipart upload does not mean your final application has 100 separate files. S3 temporarily holds uploaded parts and assembles the selected parts into one object when you complete the multipart upload.

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon S3+1

## 2. Evolution of the design: from simple upload to multipart upload

Let's understand why multipart upload exists.

### Approach A: Upload the entire file through your backend

```
Client
  |
  | POST /files (500 MB)
  v
Backend
  |
  | Upload entire file
  v
S3
```

Problems:

- Your backend handles 500 MB of incoming traffic.
- Application servers consume memory, connections, and bandwidth.
- Slow uploads occupy server connections for longer.
- Scaling becomes more expensive.
- Network failures may require uploading the entire file again.

### Approach B: Direct upload using a presigned URL

Instead, let S3 receive the file directly.

```
Client                  Backend                    S3
  |                         |                       |
  | POST /files              |                       |
  |------------------------>|                       |
  |                         | Create DB record      |
  |                         | Generate object key   |
  |                         |                       |
  |                         | Create upload         |
  |                         |---------------------->|
  |                         |                       |
  |                         | Upload URL             |
  |                         |<----------------------|
  |                         |                       |
  | Presigned URL           |                       |
  |<------------------------|                       |
  |                                                 |
  | PUT file bytes                                  |
  |------------------------------------------------>|
```

The backend handles authorization and coordination. S3 handles the heavy data transfer.

This is much better for large files.

However, there is another problem.

What happens when the user's internet connection fails halfway through a 500 MB upload?

With a single `PutObject` request, the client generally needs to retry the entire request.

### Approach C: Multipart upload

Split the file into smaller pieces.

For example:

```
Original file: 500 MB

Part 1:  50 MB
Part 2:  50 MB
Part 3:  50 MB
Part 4:  50 MB
Part 5:  50 MB
Part 6:  50 MB
Part 7:  50 MB
Part 8:  50 MB
Part 9:  50 MB
Part 10: 50 MB
```

Now each part can be uploaded independently.

```
                         S3
                          |
              Multipart Upload ID: M123
                          |
             +------------+------------+
             |            |            |
          Part 1       Part 2       Part 3
           50 MB        50 MB        50 MB
             |            |            |
             +------------+------------+
                          |
                     More parts
                          |
                          v
                  CompleteMultipart
                          |
                          v
                  One final 500 MB
                       object
```

If Part 7 fails, the client needs to retry Part 7, not the entire 500 MB file.

This is the foundation of a reliable large-file upload system.

## 3. Understand the actual S3 multipart upload mechanism

Before designing our APIs and database, let's understand what S3 does internally from an application developer's perspective.

There are three important operations.

### Step 1: CreateMultipartUpload

Your backend calls:

```
CreateMultipartUpload
    Bucket: my-files-bucket
    Key: users/user-123/files/file-abc/original.pdf
    ContentType: application/pdf
    ChecksumAlgorithm: SHA256
```

S3 returns:

```
{
  "UploadId": "upload-xyz-123"
}
```

Important: S3 has not created a normal, readable object yet.

It has initiated a multipart upload session.

Think of it as opening an upload workspace in S3.

The combination of these values identifies that workspace:

```
Bucket
Object Key
Upload ID
```

Every part upload must reference the same upload ID.

The `UploadId` is extremely important. It differentiates one upload attempt from another, even when the object key is the same.

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon S3+1

### Step 2: UploadPart

Suppose the client divides the 500 MB file into ten parts.

Your backend generates presigned URLs for individual parts.

Conceptually, the URL authorizes this operation:

```
PUT /original.pdf?partNumber=1&uploadId=upload-xyz-123
Host: my-files-bucket.s3.amazonaws.com
```

The client uploads the actual bytes directly to S3.

S3 responds with something like:

```
HTTP/1.1 200 OK
ETag: "etag-part-1"
```

The client repeats this for all ten parts.

The resulting information looks like this:

| Part number | Size  | ETag         | Status   |
| ----------- | ----- | ------------ | -------- |
| 1           | 50 MB | etag-part-1  | Uploaded |
| 2           | 50 MB | etag-part-2  | Uploaded |
| 3           | 50 MB | etag-part-3  | Uploaded |
| 4           | 50 MB | etag-part-4  | Uploaded |
| 5           | 50 MB | etag-part-5  | Uploaded |
| 6           | 50 MB | etag-part-6  | Uploaded |
| 7           | 50 MB | etag-part-7  | Uploaded |
| 8           | 50 MB | etag-part-8  | Uploaded |
| 9           | 50 MB | etag-part-9  | Uploaded |
| 10          | 50 MB | etag-part-10 | Uploaded |

The ETag is returned by S3 after a successful part upload.

It is required when completing the multipart upload.

One important S3 behavior:

Uploading the same part number again overwrites the previously uploaded part.

For example:

```
Upload part 7
    |
    v
ETag = etag-old

Network failure / retry
    |
    v
Upload part 7 again
    |
    v
ETag = etag-new
```

Now S3 uses the new version of part 7.

This is useful for retry handling. We don't need to create a new upload session just because one part failed.

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon S3+1

### Step 3: CompleteMultipartUpload

Once all ten parts are uploaded, we ask S3 to complete the upload.

Conceptually:

```
POST /original.pdf?uploadId=upload-xyz-123
```

With this request body:

```
{
  "Parts": [
    {
      "PartNumber": 1,
      "ETag": "etag-part-1"
    },
    {
      "PartNumber": 2,
      "ETag": "etag-part-2"
    },
    {
      "PartNumber": 3,
      "ETag": "etag-part-3"
    }
  ]
}
```

The actual request includes every part, in ascending part-number order.

S3 verifies the supplied parts and assembles them.

```
S3 Multipart Upload
       |
       +-- Part 1
       +-- Part 2
       +-- Part 3
       +-- ...
       +-- Part 10
       |
       v
CompleteMultipartUpload
       |
       v
S3 assembles the parts
       |
       v
Single final object
       |
       v
s3://my-files-bucket/users/user-123/files/file-abc/original.pdf
```

After successful completion:

- One normal S3 object exists.
- The final object contains all the assembled bytes.
- The uploaded parts are no longer separate multipart parts.
- You access the result using the original bucket and object key.

You do not need to download all the parts and stitch them together in your backend. S3 does that for you.

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon S3+1

### What are the multipart size limits?

S3's current general-purpose bucket multipart specifications are:

| Property               | Limit      |
| ---------------------- | ---------- |
| Maximum object size    | 48.8 TiB   |
| Maximum parts          | 10,000     |
| Minimum part size      | 5 MiB      |
| Maximum part size      | 5 GiB      |
| Minimum last part size | No minimum |

For example, if you have a 100 MB file, you could upload it as:

```
Part 1 = 25 MB
Part 2 = 25 MB
Part 3 = 25 MB
Part 4 = 25 MB
```

The last part is allowed to be smaller than 5 MiB. Other parts must satisfy the minimum.

AWS generally recommends considering multipart uploads for objects around 100 MB or larger.

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Storage Service

## 4. Now, the most important design question: Where do we store chunk information?

This is where I want to distinguish three different things.

### A. The actual chunk bytes

Stored by S3.

You do not store those chunks in PostgreSQL.

S3 manages the uploaded parts under its multipart upload session.

Before completion:

```
S3
 |
 +-- Upload ID: upload-xyz-123
       |
       +-- Part 1 (50 MB)
       +-- Part 2 (50 MB)
       +-- Part 3 (50 MB)
       +-- Part 4 (50 MB)
```

After completion:

```
S3
 |
 +-- original.pdf (500 MB)
```

### B. Chunk information returned by S3

For every successfully uploaded part, S3 provides information such as:

- Part number
- ETag
- Part size
- Checksum, when configured
- Last modified time

This information is useful for completing the upload and resuming interrupted uploads.

You have two options for managing it.

Option 1: Keep the information on the client

The client maintains a manifest:

```
{
  "uploadSessionId": "session-123",
  "uploadedParts": [
    {
      "partNumber": 1,
      "etag": "etag-part-1"
    },
    {
      "partNumber": 2,
      "etag": "etag-part-2"
    }
  ]
}
```

The client stores this locally, potentially in IndexedDB for browser-based applications, so it can resume after a page refresh.

The limitation is that local information can be lost or become stale.

Option 2: Maintain part metadata in your database

You could introduce an optional table:

```
CREATE TABLE file_upload_parts (
    id UUID PRIMARY KEY,
    upload_session_id UUID NOT NULL,
    part_number INT NOT NULL,
    part_size BIGINT,
    etag VARCHAR(255),
    checksum VARCHAR(255),
    status VARCHAR(30),
    uploaded_at TIMESTAMP,

    UNIQUE (upload_session_id, part_number)
);
```

Example:

| Session ID  | Part | Size  | ETag   | Status   |
| ----------- | ---- | ----- | ------ | -------- |
| session-123 | 1    | 50 MB | etag-1 | UPLOADED |
| session-123 | 2    | 50 MB | etag-2 | UPLOADED |
| session-123 | 3    | 50 MB | etag-3 | UPLOADED |

This can help with:

- Upload progress tracking.
- Server-side resumability.
- Monitoring and troubleshooting.
- Understanding which parts have been acknowledged.
- Supporting more sophisticated upload workflows.

However, here's an important distinction:

Your database's part table should not be considered the authoritative source of whether a part currently exists in S3.

Imagine:

1. Client uploads Part 4.
2. S3 successfully stores it.
3. Client reports success.
4. Your backend saves Part 4 metadata in PostgreSQL.
5. Later, the client retries Part 4 with different bytes.
6. S3 overwrites Part 4.
7. The database still contains the old ETag.

Now PostgreSQL and S3 disagree.

You must reconcile such information.

### C. What is the better approach?

My recommendation:

| Information                      | Where to maintain it |
| -------------------------------- | -------------------- |
| Actual file bytes                | S3                   |
| Multipart upload ID              | PostgreSQL           |
| Logical file metadata            | PostgreSQL           |
| Expected part count              | PostgreSQL           |
| Part size configuration          | PostgreSQL           |
| Actual uploaded part list        | S3                   |
| Part ETags needed for completion | Obtain from S3       |
| Optional upload progress cache   | PostgreSQL           |
| Final object ETag/checksum       | PostgreSQL           |

For most applications, I would initially avoid creating a separate part metadata table.

Use S3's `ListParts` API when you need to determine which parts have actually been uploaded.

This API provides the uploaded part information, including their ETags and sizes. It is also paginated, returning at most 1,000 parts per request.

For a system with advanced resumability, frequent progress reporting, or extensive upload observability, add the part metadata table as a cache or tracking mechanism.

Do not duplicate S3's responsibilities unnecessarily.

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Storage Service+1

## 5. The recommended production architecture

Let's now design the actual application.

We will use:

- PostgreSQL for file metadata and upload state.
- S3 for object storage.
- Backend service for coordination.
- Client for direct file transfer.
- SQS for asynchronous downstream processing, when required.

The architecture:

\#chatgpt-mermaid-\_r_5b2\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5b2\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5b2\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5b2\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5b2\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5b2\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5b2\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5b2\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5b2\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5b2\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5b2\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5b2\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5b2\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5b2\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5b2\_ p{margin:0;}#chatgpt-mermaid-\_r_5b2\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5b2\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5b2\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5b2\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5b2\_ .label text,#chatgpt-mermaid-\_r_5b2\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5b2\_ .node rect,#chatgpt-mermaid-\_r_5b2\_ .node circle,#chatgpt-mermaid-\_r_5b2\_ .node ellipse,#chatgpt-mermaid-\_r_5b2\_ .node polygon,#chatgpt-mermaid-\_r_5b2\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5b2\_ .rough-node .label text,#chatgpt-mermaid-\_r_5b2\_ .node .label text,#chatgpt-mermaid-\_r_5b2\_ .image-shape .label,#chatgpt-mermaid-\_r_5b2\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5b2\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5b2\_ .rough-node .label,#chatgpt-mermaid-\_r_5b2\_ .node .label,#chatgpt-mermaid-\_r_5b2\_ .image-shape .label,#chatgpt-mermaid-\_r_5b2\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5b2\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5b2\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5b2\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5b2\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5b2\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5b2\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5b2\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5b2\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5b2\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5b2\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5b2\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5b2\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5b2\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5b2\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5b2\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5b2\_ .icon-shape,#chatgpt-mermaid-\_r_5b2\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5b2\_ .icon-shape p,#chatgpt-mermaid-\_r_5b2\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5b2\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5b2\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5b2\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5b2\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5b2\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5b2\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5b2\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5b2\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5b2\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5b2\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5b2\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5b2\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5b2\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5b2\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5b2\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5b2\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5b2\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5b2\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5b2\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5b2\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5b2\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5b2\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5b2\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5b2\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5b2\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5b2\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5b2\_ .node rect,#chatgpt-mermaid-\_r_5b2\_ .node circle,#chatgpt-mermaid-\_r_5b2\_ .node ellipse,#chatgpt-mermaid-\_r_5b2\_ .node polygon,#chatgpt-mermaid-\_r_5b2\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5b2\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5b2\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5b2\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5b2\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5b2\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Client / BrowserFile Service APIPostgreSQLAmazon S3SQSFile Processing Worker1. Create upload2. Create file record3. Initiate multipart upload4. Save Upload ID5. Return upload details6. Upload parts using presignedURLs7. Request completion8. Verify parts using ListParts9. Complete multipart upload10. Update file status11. Optional ObjectCreatedevent12. Processing event via outbox13. Validate / scan / process14. Update processing status

There are two important workflows here:

1. Uploading the actual object.
2. Processing the uploaded object.

Do not confuse these.

A file can be successfully uploaded to S3 but still not be safe or ready for application use.

We will return to this distinction later.

## 6. Database design

I recommend separating the logical file record from the upload attempt.

Why?

Because a logical file and an individual upload attempt are not always the same thing.

Suppose:

1. A user starts uploading a file.
2. The connection fails.
3. The multipart upload is aborted.
4. The user resumes or restarts the upload.

The logical file may remain the same, but we might create a new S3 multipart upload session.

We should be able to represent that cleanly.

### Table 1: files

Represents the logical file.

```
CREATE TABLE files (
    id UUID PRIMARY KEY,

    owner_id UUID NOT NULL,

    original_filename VARCHAR(500) NOT NULL,
    content_type VARCHAR(255),

    expected_size_bytes BIGINT NOT NULL,
    actual_size_bytes BIGINT,

    storage_bucket VARCHAR(255),
    storage_key VARCHAR(1024),

    storage_etag VARCHAR(255),
    storage_version_id VARCHAR(255),

    checksum_algorithm VARCHAR(50),
    checksum_value VARCHAR(255),

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    uploaded_at TIMESTAMP,
    available_at TIMESTAMP
);
```

Example:

```
id                  = file-abc
owner_id            = user-123
original_filename   = report.pdf
expected_size_bytes = 524288000
storage_bucket      = my-files-bucket
storage_key         = files/file-abc/attempt-001/original.pdf
status              = UPLOADING
```

### Table 2: file_upload_sessions

Represents an individual multipart upload attempt.

```
CREATE TABLE file_upload_sessions (
    id UUID PRIMARY KEY,

    file_id UUID NOT NULL REFERENCES files(id),

    attempt_number INT NOT NULL,

    s3_upload_id VARCHAR(1024),

    bucket VARCHAR(255) NOT NULL,
    object_key VARCHAR(1024) NOT NULL,

    part_size_bytes BIGINT NOT NULL,
    expected_part_count INT NOT NULL,

    status VARCHAR(30) NOT NULL,

    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,

    UNIQUE (file_id, attempt_number)
);
```

Example:

```
file_id          = file-abc
attempt_number   = 1
s3_upload_id     = upload-xyz-123
part_size_bytes  = 52428800
expected_count   = 10
status           = UPLOADING
```

Why keep an attempt table?

Because each attempt has its own S3 upload ID and potentially its own object key.

When an upload must be restarted, we can create another attempt without losing the history of the first one.

For a simpler system, you can initially put `s3_upload_id` and related fields directly in the `files` table. The separate table becomes useful as retry and recovery requirements grow.

### Upload status design

Use explicit lifecycle states.

| Status     | Meaning                                                  |
| ---------- | -------------------------------------------------------- |
| INITIATING | Database record created; S3 upload session being created |
| UPLOADING  | Multipart upload session established                     |
| COMPLETING | Backend is verifying and completing the upload           |
| UPLOADED   | Final S3 object exists and has been verified             |
| PROCESSING | Malware scan or other validation in progress             |
| AVAILABLE  | File passed validation and can be used                   |
| ABORTED    | Upload explicitly cancelled                              |
| EXPIRED    | Upload abandoned beyond allowed time                     |
| FAILED     | Unrecoverable upload failure                             |

The normal transition:

```
INITIATING
    |
    v
UPLOADING
    |
    v
COMPLETING
    |
    v
UPLOADED
    |
    v
PROCESSING
    |
    v
AVAILABLE
```

Other terminal paths:

```
UPLOADING ---> ABORTED
UPLOADING ---> EXPIRED

COMPLETING --> FAILED (after recovery determines it cannot succeed)
PROCESSING --> FAILED (validation failure)
```

One important rule:

Do not transition a file backward from `UPLOADED` to `UPLOADING` because of a delayed retry or duplicate event.

Use guarded, conditional state transitions.

## 7. Let's walk through the complete workflow

Now let's connect the database, backend, client, and S3.

We will use a 500 MB file in our example.

### Phase 1: Client requests an upload

The client sends:

```
POST /api/v1/files
Idempotency-Key: request-123
Content-Type: application/json
```

Request:

```
{
  "filename": "report.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 524288000
}
```

The backend performs:

1\. Validate the request.

- User authentication and authorization.
- Allowed file types.
- Maximum file size.
- Storage quota.
- User's upload limits.
- Whether this idempotency key has already been used.

2\. Generate identifiers.

```
fileId       = file-abc
uploadSessionId = session-123
objectKey    = files/file-abc/attempt-001/original.pdf
```

The object key is generated by your backend.

Do not use the user-provided filename as your storage identity. Store it separately as metadata.

3\. Create the database record.

```
files:
    id     = file-abc
    status = INITIATING

file_upload_sessions:
    id           = session-123
    file_id      = file-abc
    object_key   = files/file-abc/attempt-001/original.pdf
    status       = INITIATING
```

4\. Initiate multipart upload in S3.

Backend:

```
CreateMultipartUpload(
    bucket = my-files-bucket,
    key = files/file-abc/attempt-001/original.pdf,
    contentType = application/pdf,
    metadata = {
        file-id: file-abc,
        upload-session-id: session-123
    }
)
```

S3 returns:

```
uploadId = upload-xyz-123
```

5\. Update the database.

```
file_upload_sessions:
    s3_upload_id = upload-xyz-123
    status       = UPLOADING

files:
    status       = UPLOADING
```

The application now has everything required to coordinate the upload.

One subtle but important distinction:

S3 does not create an empty object at the key when you initiate the multipart upload. It creates an upload session associated with the bucket and key. The actual object becomes available after successful completion.

### Phase 2: Backend returns upload information

The backend responds:

```
HTTP/1.1 201 Created
```

```
{
  "fileId": "file-abc",
  "uploadSessionId": "session-123",
  "status": "UPLOADING",
  "upload": {
    "partSizeBytes": 52428800,
    "totalParts": 10,
    "expiresAt": "2026-11-07T12:00:00Z"
  }
}
```

Notice that we have not necessarily returned all ten presigned URLs.

Why?

Because generating and returning thousands of URLs for very large files is unnecessary.

We can generate them in batches.

### Phase 3: Client requests presigned URLs

Client:

```
POST /api/v1/files/file-abc/upload-urls
```

Request:

```
{
  "partNumbers": [1, 2, 3, 4]
}
```

Backend:

1. Authenticates the user.
2. Verifies ownership of `file-abc`.
3. Verifies that the upload is still active.
4. Checks that the requested part numbers are valid.
5. Generates presigned URLs for those exact parts.

For example:

```
{
  "parts": [
    {
      "partNumber": 1,
      "url": "https://s3...partNumber=1..."
    },
    {
      "partNumber": 2,
      "url": "https://s3...partNumber=2..."
    },
    {
      "partNumber": 3,
      "url": "https://s3...partNumber=3..."
    },
    {
      "partNumber": 4,
      "url": "https://s3...partNumber=4..."
    }
  ]
}
```

Each URL authorizes an upload to one specific part of one specific multipart session.

The backend can generate another batch when the client needs more URLs.

Presigned URL expiration does not mean the multipart session expires.

These are different things:

- Presigned URL: temporary permission to upload a particular part.
- Multipart upload session: the actual S3 upload workspace.

When a URL expires, the client requests another URL. It does not need to restart the upload.

### Phase 4: Client uploads the parts

The client divides its file into ten parts.

Conceptually:

```
const partSize = 50 * 1024 * 1024;

for (let partNumber = 1; partNumber <= 10; partNumber++) {
    const start = (partNumber - 1) * partSize;
    const end = Math.min(start + partSize, file.size);

    const part = file.slice(start, end);

    // Obtain a presigned URL from the backend.
    // Upload the part directly to S3.
}
```

This is conceptual code; in production, we would upload several parts concurrently rather than use a sequential loop.

For each part:

```
PUT <presigned-url>
```

The request body contains the actual bytes.

S3 returns:

```
HTTP/1.1 200 OK
ETag: "etag-part-1"
```

The client records:

```
{
  "partNumber": 1,
  "etag": "\"etag-part-1\""
}
```

It repeats the process for all ten parts.

Important practical detail for browser applications:

Configure S3 bucket CORS to allow the relevant methods and expose the `ETag` response header.

Otherwise, the browser might successfully upload a part but JavaScript may not be able to read the ETag from the response.

The client should maintain its upload progress locally, potentially in IndexedDB, especially if uploads must survive browser refreshes.

Also, use controlled concurrency. For example, start with four or five simultaneous parts and tune based on network conditions and file sizes. Uploading all 10,000 parts simultaneously is not a sensible strategy.

### Phase 5: Client requests completion

Once the client believes all ten parts have been uploaded, it sends:

```
POST /api/v1/files/file-abc/complete
```

Optionally, the client can send its part manifest:

```
{
  "parts": [
    {
      "partNumber": 1,
      "etag": "etag-part-1"
    },
    {
      "partNumber": 2,
      "etag": "etag-part-2"
    }
  ]
}
```

The example is abbreviated; a real manifest would include every part.

Now comes the most important part of our architecture.

The backend must not blindly trust the client manifest.

The client is not the authority on what S3 currently contains.

Instead, the backend uses S3's `ListParts` operation.

Conceptually:

```
Backend
   |
   | ListParts(bucket, key, uploadId)
   |
   v
S3
   |
   v
Returns actual uploaded parts
```

S3 might return:

```
{
  "parts": [
    {
      "partNumber": 1,
      "size": 52428800,
      "etag": "etag-part-1"
    },
    {
      "partNumber": 2,
      "size": 52428800,
      "etag": "etag-part-2"
    },
    {
      "partNumber": 3,
      "size": 52428800,
      "etag": "etag-part-3"
    }
  ]
}
```

Suppose the client claims that all ten parts were uploaded, but S3 reports that only nine exist.

What happens?

We do not complete the upload.

We respond with an incomplete-upload error and let the client upload the missing part.

This is much more reliable than trusting the browser.

### Phase 6: Backend verifies the uploaded parts

Before completing, the backend checks:

1\. Are all expected parts present?

For our example:

```
Expected parts = 10
Actual parts   = 10
```

2\. Are the part numbers correct?

```
1, 2, 3, 4, 5, 6, 7, 8, 9, 10
```

3\. Are the sizes valid?

The total size should match the original expected size.

```
Total uploaded size = 500 MB
Expected size       = 500 MB
```

4\. Are there any unexpected parts?

We should not allow an unrequested part or an incorrectly sized part to enter the final object.

5\. Are the checksums valid?

When configured, validate the relevant checksum values.

After all checks pass, we have a reliable list of the actual parts that S3 has stored.

### Phase 7: Backend completes the multipart upload

Now the backend sends `CompleteMultipartUpload` to S3.

It uses the ETags retrieved from S3, not blindly copied from the client.

```
CompleteMultipartUpload(
    bucket,
    objectKey,
    uploadId,
    parts = [
        { partNumber: 1, etag: "etag-part-1" },
        { partNumber: 2, etag: "etag-part-2" },
        ...
        { partNumber: 10, etag: "etag-part-10" }
    ]
)
```

S3 assembles the parts.

The backend receives the completion result.

One unusual AWS detail worth knowing: `CompleteMultipartUpload` can initially return HTTP 200 while the response body contains an error discovered during processing. AWS SDKs handle this response behavior; do not implement completion by checking only the HTTP status code yourself.

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon S3+1

### Phase 8: Backend verifies the final object

After successful completion, the backend performs:

```
HeadObject(
    bucket,
    objectKey
)
```

It verifies:

- Object exists.
- Actual content length matches expected size.
- Object metadata identifies the correct file and upload attempt.
- Checksum matches the expected checksum, where configured.

S3 provides strong read-after-write consistency for object writes, so a subsequent HEAD request can verify the successfully completed object without waiting for eventual consistency.

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Storage Service+1

Once verified, update PostgreSQL:

```
files:
    status            = UPLOADED
    actual_size_bytes = 524288000
    storage_etag      = final-etag
    uploaded_at       = current_timestamp

file_upload_sessions:
    status            = COMPLETED
    completed_at      = current_timestamp
```

At this point, we can confidently say the object has been uploaded and verified.

## 8. Who should confirm that the file was uploaded: client or S3?

Let's settle this design question properly.

There are three possible approaches.

### Approach A: Trust the client

```
Client uploads all parts
       |
       v
Client says "Upload complete"
       |
       v
Backend updates database to UPLOADED
```

Problem:

The client might be mistaken.

For example:

- A part upload failed.
- A request timed out.
- The client lost the response.
- The user closed the browser.
- The client accidentally skipped a part.

A browser reporting 100% progress is not proof that the complete object exists in S3.

I would not use this approach.

### Approach B: Trust only an S3 event

S3 can publish an object-created event when a multipart upload is successfully completed.

For example:

```
S3
 |
 | ObjectCreated:CompleteMultipartUpload
 v
SQS
 |
 v
File Service
 |
 v
Update database
```

This is a useful mechanism for automatically detecting successful object creation.

But it has some important characteristics.

S3 event notifications:

- Are delivered at least once.
- Can contain duplicate events.
- Are not guaranteed to arrive in the same order in which the events occurred.

Therefore, your event consumer must be idempotent.

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Storage Service+1

Also, the client still needs some way to indicate that it has finished uploading and wants the upload completed.

S3 does not automatically assemble a multipart upload just because all expected parts happen to be present.

Someone must call `CompleteMultipartUpload`.

### Approach C: Client signals, backend verifies, S3 confirms

This is my recommended approach.

```
Client
   |
   | Upload parts directly
   v
S3
   |
   | Parts stored
   |
   v
Client
   |
   | POST /files/{id}/complete
   v
Backend
   |
   | ListParts
   v
S3
   |
   | Returns actual uploaded parts
   v
Backend
   |
   | Validate
   |
   | CompleteMultipartUpload
   v
S3
   |
   | Object successfully assembled
   v
Backend
   |
   | HEAD object and verify
   v
Database
   |
   | status = UPLOADED
```

The responsibilities are clear:

| Component | Responsibility                                     |
| --------- | -------------------------------------------------- |
| Client    | Transfer data and request finalization             |
| S3        | Authoritatively store parts and assemble object    |
| Backend   | Validate, coordinate completion, and update status |
| Database  | Maintain the application's file lifecycle          |

The client initiates the completion workflow. S3 determines whether the uploaded object actually exists. The backend decides when the application considers the file successfully uploaded.

This separation is important.

## 9. Handling retries, duplicates, and failures

This is where a production system becomes reliable.

Let's go through the important failure scenarios.

### Scenario 1: Client retries the create-upload API

Imagine:

1. Client calls `POST /files`.
2. Backend creates a database record.
3. Backend initiates multipart upload in S3.
4. Backend returns a successful response.
5. Network connection breaks before the client receives the response.
6. Client retries `POST /files`.

What happens if we do nothing?

We might create:

```
File 1 -> file-abc -> S3 Upload ID 1
File 2 -> file-def -> S3 Upload ID 2
```

Now we have duplicate logical uploads.

Solution: Idempotency key.

The client sends:

```
Idempotency-Key: request-123
```

Store it with the user's upload request.

Add a unique constraint:

```
UNIQUE (owner_id, idempotency_key)
```

When the client retries:

```
POST /files
Idempotency-Key: request-123
```

The backend finds the existing operation and returns its existing result.

```
{
  "fileId": "file-abc",
  "uploadSessionId": "session-123",
  "status": "UPLOADING"
}
```

No new file record and no new S3 multipart session are created.

This should be implemented for other state-changing APIs as well.

### Scenario 2: Backend fails while initiating the S3 multipart upload

This is an important distributed-systems problem.

Consider:

```
1. Insert DB record
       |
       v
2. Call CreateMultipartUpload
       |
       v
3. S3 successfully creates upload session
       |
       v
4. Backend crashes before saving UploadId in DB
```

Now:

```
PostgreSQL:
    status = INITIATING
    upload_id = NULL

S3:
    Active multipart upload exists
    upload_id = upload-xyz
```

Our database and S3 disagree.

This happens because PostgreSQL and S3 do not participate in the same transaction.

You cannot achieve atomic commit across these two systems just by using a database transaction.

Recommended solution: reconciliation and compensation.

Maintain:

```
INITIATING
INITIATION_UNKNOWN
UPLOADING
```

When the S3 initiation request has an ambiguous outcome:

1. Do not blindly initiate another multipart upload.
2. Check S3 using `ListMultipartUploads` for the attempt's object key.
3. Find and reconcile the existing upload session.
4. Save the correct upload ID in PostgreSQL.
5. If multiple orphan sessions exist, abort the unwanted ones.

An additional safety measure is to use a unique object key for each attempt:

```
files/file-abc/attempt-001/original.pdf
files/file-abc/attempt-002/original.pdf
```

This prevents two attempts from accidentally writing into the same logical storage location.

Use a database lock or initiation lease so multiple application instances don't independently initiate the same attempt.

The general principle:

Whenever a database transaction and an external storage operation must work together, design for partial success and provide a recovery mechanism.

### Scenario 3: Presigned URL expires

Suppose the client receives URLs for Parts 1–5.

It uploads Parts 1–3.

Then Part 4's URL expires.

Should we start a new multipart upload?

Absolutely not.

Just request another URL:

```
POST /api/v1/files/file-abc/upload-urls
```

```
{
  "partNumbers": [4, 5]
}
```

The backend generates new URLs using the same:

```
Bucket
Object Key
Upload ID
Part Numbers
```

The upload session remains unchanged.

### Scenario 4: Part upload fails

Suppose Part 7 fails.

```
Part 1 -> Success
Part 2 -> Success
Part 3 -> Success
Part 4 -> Success
Part 5 -> Success
Part 6 -> Success
Part 7 -> Failed
Part 8 -> Success
Part 9 -> Success
Part 10 -> Success
```

Client retries only Part 7.

```
UploadPart(
    uploadId = upload-xyz-123,
    partNumber = 7,
    body = same original bytes
)
```

S3 overwrites the existing Part 7, if any, and returns the current ETag.

This is effectively an idempotent retry strategy as long as the retry sends the same bytes for the same part number.

However, be careful about this situation:

```
First upload of Part 7 = bytes A
Second upload of Part 7 = bytes B
```

The second upload replaces the first.

Therefore, retries must use the same original file slice.

### Scenario 5: S3 successfully receives a part, but the client doesn't receive the response

This is especially common with unreliable internet connections.

```
Client                 S3
   |                     |
   | Upload Part 7       |
   |-------------------->|
   |                     |
   | S3 stores Part 7    |
   |                     |
   |<---- Response ------|
   X
Network breaks
```

The client doesn't know whether the upload succeeded.

What should it do?

It has two options:

Option A: Check the uploaded parts.

Call your backend:

```
GET /api/v1/files/file-abc/upload-parts
```

Backend uses `ListParts`.

If Part 7 exists, the client knows the part was uploaded.

Option B: Retry the same part.

Upload the exact same bytes again using the same part number.

S3 overwrites the previous part.

This is safe, provided the same bytes are used.

For a large upload system, I prefer giving the client a way to query uploaded parts so it can avoid unnecessary retransmission.

### Scenario 6: User uploads all parts but never calls Complete

This is an interesting problem.

Imagine:

```
Part 1 -> Success
Part 2 -> Success
...
Part 10 -> Success

User closes browser.
```

What happens?

S3 still has an active multipart upload session containing all the uploaded parts.

S3 does not automatically assemble the object.

The object does not become a normal readable object until `CompleteMultipartUpload` succeeds.

The uploaded parts also consume storage and incur storage charges until the multipart upload is completed or aborted.

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Storage Service+1

We need an expiration mechanism.

My recommendation:

```
Upload session expiry = 24 hours
```

This is an application policy, not an S3 requirement.

When an upload expires:

1. Mark the session as `EXPIRED`.
2. Abort the multipart upload in S3.
3. Release any reserved user quota.
4. Keep the logical file record according to your retention policy.

Additionally, configure an S3 lifecycle rule:

```
AbortIncompleteMultipartUpload
    |
    v
Abort incomplete uploads older than 7 days
```

The seven-day period is just an example configuration.

Why have both?

- Application cleanup provides business-level control.
- S3 lifecycle cleanup provides a safety net if your application cleanup fails.

AWS specifically recommends this lifecycle configuration to control storage costs from abandoned multipart uploads.

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Storage Service+1

### Scenario 7: CompleteMultipartUpload succeeds, but backend crashes

This is probably the most important failure to handle correctly.

Consider:

```
Client
  |
  v
Backend
  |
  v
S3 CompleteMultipartUpload
  |
  v
SUCCESS
  |
  v
Backend crashes before DB update
```

Now:

```
S3:
    Final object exists.

PostgreSQL:
    status = COMPLETING
```

What happens when the client retries the completion API?

A naive implementation will call `CompleteMultipartUpload` again.

But the multipart session has already been completed.

S3 may return `NoSuchUpload` because that upload session no longer exists.

This does not necessarily mean the file upload failed!

It may mean that the first completion request actually succeeded.

Correct recovery strategy: Check the final object first.

```
Client retries completion
         |
         v
Backend sees COMPLETING
         |
         v
HEAD object in S3
         |
         +---------------------+
         |                     |
      Exists                Not found
         |                     |
         v                     v
Verify metadata,          Check ListParts /
size and checksum         multipart session
         |                     |
         v                     v
Mark UPLOADED             Recover or retry
```

For verification, compare:

- Expected object key.
- File ID metadata.
- Upload attempt ID metadata.
- Expected size.
- Expected checksum, when available.

If the final object exists and matches the expected upload, mark the database record `UPLOADED`.

If it does not exist but the multipart upload remains active with all expected parts, retry completion.

If neither the object nor a recoverable upload session exists, mark the attempt failed or initiate a new attempt according to your retry policy.

This is a good example of why our backend needs a reconciliation process.

### Scenario 8: Multiple completion requests arrive simultaneously

Imagine two application instances receive the same completion request.

```
Instance A            Instance B
    |                     |
    | Complete             | Complete
    v                     v
```

Without protection, both might try to complete the same upload.

Use an atomic state transition:

```
UPDATE files
SET status = 'COMPLETING'
WHERE id = :file_id
  AND status = 'UPLOADING';
```

Only one instance should successfully transition the record.

The winner owns the completion operation.

The other instance can return the current upload status rather than initiating another completion.

For long-running operations, use a completion lease or timeout so a reconciliation worker can recover if the winning instance crashes.

Do not hold a database transaction open while waiting for a potentially lengthy S3 operation.

### Scenario 9: Same file uploaded twice

There are two different meanings of duplicate uploads.

Duplicate API request

Solved using an idempotency key.

Two separate upload requests containing identical file contents

These are separate logical uploads unless you deliberately implement content deduplication.

You can optionally calculate a content checksum and use it to detect duplicate content.

But do not assume files are identical just because their filenames and sizes match.

Content-based deduplication also introduces access-control, tenant-isolation, and reference-counting considerations.

For most systems, I would initially store separate file objects and introduce deduplication only when it provides meaningful storage or business benefits.

## 10. Checksum and file integrity: don't blindly trust ETag

This deserves a separate explanation because it is a common mistake.

You might think:

```
ETag = MD5(file)
```

That assumption is not generally correct.

For multipart uploads, the final ETag is not the MD5 checksum of the entire object.

It is calculated using the individual part information. Its format often includes a suffix indicating the number of parts.

Also, encryption can affect ETag behavior for single-part uploads.

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Storage Service+1

What should we do instead?

Use S3's checksum functionality.

For example:

```
Checksum algorithm = SHA256
```

You can configure checksum validation as part of the multipart upload process.

Depending on the chosen checksum mode, S3 can validate the full object checksum or calculate a composite checksum from part-level checksums.

For supported full-object checksum modes, S3 validates the supplied checksum before storing the object.

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Storage Service+1

Recommended approach:

1. Client calculates the expected file checksum where practical.
2. Client submits that checksum when creating the upload.
3. Backend configures the corresponding S3 checksum behavior.
4. S3 validates the uploaded data.
5. Backend stores the resulting checksum information in PostgreSQL.

For very large files, checksum calculation should be streaming rather than loading the entire file into memory.

Also remember: a client-provided checksum provides an integrity reference, not proof that the file is trustworthy. A malicious client can intentionally submit malicious content and its correct checksum.

You still need file validation and malware scanning.

## 11. File upload and file processing are two different workflows

Suppose the user uploads a PDF successfully.

Can your system immediately expose it for everyone to download?

Not necessarily.

The file might:

- Contain malware.
- Have an invalid file format.
- Be corrupted.
- Contain malicious embedded content.
- Violate application-specific validation rules.

Separate the two concepts:

```
UPLOADED != AVAILABLE
```

A strong architecture looks like this:

\#chatgpt-mermaid-\_r_5kf\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5kf\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5kf\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5kf\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5kf\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5kf\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5kf\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5kf\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5kf\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5kf\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5kf\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5kf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5kf\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5kf\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5kf\_ p{margin:0;}#chatgpt-mermaid-\_r_5kf\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5kf\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5kf\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5kf\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5kf\_ .label text,#chatgpt-mermaid-\_r_5kf\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5kf\_ .node rect,#chatgpt-mermaid-\_r_5kf\_ .node circle,#chatgpt-mermaid-\_r_5kf\_ .node ellipse,#chatgpt-mermaid-\_r_5kf\_ .node polygon,#chatgpt-mermaid-\_r_5kf\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5kf\_ .rough-node .label text,#chatgpt-mermaid-\_r_5kf\_ .node .label text,#chatgpt-mermaid-\_r_5kf\_ .image-shape .label,#chatgpt-mermaid-\_r_5kf\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5kf\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5kf\_ .rough-node .label,#chatgpt-mermaid-\_r_5kf\_ .node .label,#chatgpt-mermaid-\_r_5kf\_ .image-shape .label,#chatgpt-mermaid-\_r_5kf\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5kf\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5kf\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5kf\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5kf\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5kf\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5kf\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5kf\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5kf\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5kf\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5kf\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5kf\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5kf\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5kf\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5kf\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5kf\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5kf\_ .icon-shape,#chatgpt-mermaid-\_r_5kf\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5kf\_ .icon-shape p,#chatgpt-mermaid-\_r_5kf\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5kf\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5kf\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5kf\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5kf\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5kf\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5kf\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5kf\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5kf\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5kf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5kf\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5kf\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5kf\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5kf\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5kf\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5kf\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5kf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5kf\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5kf\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5kf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5kf\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5kf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5kf\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5kf\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5kf\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5kf\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5kf\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5kf\_ .node rect,#chatgpt-mermaid-\_r_5kf\_ .node circle,#chatgpt-mermaid-\_r_5kf\_ .node ellipse,#chatgpt-mermaid-\_r_5kf\_ .node polygon,#chatgpt-mermaid-\_r_5kf\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5kf\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5kf\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5kf\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5kf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5kf\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Client uploads fileS3 multipart completionVerify final objectDatabase: UPLOADEDTransactional outboxSQSFile processing workerMalware scanFormat and businessvalidationValidation passed?Database: AVAILABLEDatabase: REJECTEDYesNo

### Why use an outbox here?

Consider:

```
1. Update database to UPLOADED
2. Send SQS message
```

What happens if the process crashes after step 1 but before step 2?

The database says the file was uploaded, but no processing message exists.

You have lost a processing event.

Use the transactional outbox pattern.

In one PostgreSQL transaction:

```
BEGIN

UPDATE files
SET status = 'UPLOADED'
WHERE id = 'file-abc';

INSERT INTO outbox_events (
    aggregate_id,
    event_type,
    payload
)
VALUES (
    'file-abc',
    'FILE_UPLOADED',
    ...
);

COMMIT
```

An outbox publisher reads these events and publishes them to SQS.

It retries publishing if necessary.

The processing worker must also be idempotent because SQS delivery and outbox publishing can result in duplicate processing attempts.

You can use the S3 object-created event as an additional reconciliation or detection mechanism, but I would avoid having independent event consumers perform the same business processing without deduplication.

S3 events are useful, but they are not exactly-once delivery.

## 12. REST API design for the complete system

Here is the API design I would use.

| Method | Endpoint                              | Responsibility                              |
| ------ | ------------------------------------- | ------------------------------------------- |
| POST   | `/api/v1/files`                       | Create file record and initiate upload      |
| GET    | `/api/v1/files/{fileId}`              | Retrieve file and upload status             |
| POST   | `/api/v1/files/{fileId}/upload-urls`  | Generate presigned URLs for selected parts  |
| GET    | `/api/v1/files/{fileId}/upload-parts` | Retrieve actual uploaded part information   |
| POST   | `/api/v1/files/{fileId}/complete`     | Verify and complete multipart upload        |
| POST   | `/api/v1/files/{fileId}/abort`        | Abort multipart upload                      |
| GET    | `/api/v1/files/{fileId}/download-url` | Generate download URL for an available file |

A few important design decisions:

Create endpoint

Accepts metadata, not file bytes.

Upload URLs endpoint

Generates URLs only for an authorized user's upload session and valid part numbers.

Upload parts endpoint

Uses `ListParts` to obtain actual S3 information. It should not simply return a database cache as the authoritative part list.

Complete endpoint

Performs the entire backend-side verification and completion workflow.

Download endpoint

Should generate a download URL only if the file is authorized and in `AVAILABLE` status.

All state-changing endpoints should be designed for retries.

## 13. How should we select the chunk size?

There is no universally optimal chunk size.

It depends on:

- File size.
- Network speed.
- Network reliability.
- Client memory.
- Upload concurrency.
- S3 request overhead.

Some reasonable starting configurations:

| File size   | Example part size |
| ----------- | ----------------- |
| 100–500 MB  | 16–32 MB          |
| 500 MB–2 GB | 32–64 MB          |
| 2–10 GB     | 64–128 MB         |
| 10–100 GB   | 128–256 MB        |

These are engineering starting points, not S3 requirements.

A useful calculation is:

\\[ \text{Part Count} = \left\lceil \frac{\text{File Size}}{\text{Part Size}} \right\rceil \\]

You must stay within S3's 10,000-part limit.

A general strategy:

\\[ \text{Part Size} = \max\left( \text{Configured Part Size}, \left\lceil\frac{\text{File Size}}{10000}\right\rceil \right) \\]

Round the result up to a convenient MiB boundary and enforce S3's part-size limits.

Also, don't use multipart upload for every tiny file.

For smaller files, a normal presigned `PutObject` URL is usually simpler.

AWS's recommendation to consider multipart uploads at approximately 100 MB is a useful starting threshold. You can tune this based on your workloads.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Storage Service



## 14. Security considerations

Because clients communicate directly with S3, the security model deserves attention.

1\. Keep the bucket private.

Never make uploaded objects publicly readable just to simplify uploads.

2\. Treat presigned URLs as bearer credentials.

Anyone possessing a valid URL can use its authorized operation until it expires.

Keep expiry relatively short, avoid logging full URLs, and issue URLs only after authorization.

3\. Restrict the URL's scope.

A URL should authorize:

- The correct bucket.
- The correct object key.
- The correct upload ID.
- The correct part number.
- The intended HTTP method.
- Only the required headers.

4\. Validate user-supplied metadata.

Do not trust:

```
filename
file extension
Content-Type
file size
```

The client declares these values. Your backend verifies them against the actual uploaded object and its content.

5\. Enforce quotas.

For example:

```
Maximum file size: 10 GB
Maximum active uploads per user: 5
Maximum outstanding upload sessions: configured limit
```

These are application-level policies.

6\. Validate the actual contents.

Run format validation and malware scanning before exposing files.

7\. Use encryption.

Use S3 server-side encryption, such as SSE-S3 or SSE-KMS, according to your security requirements.

## 15. What happens when the system scales?

Let's consider 100,000 users uploading files.

Suppose:

- Average file size: 500 MB.
- Ten parts per file.
- Each upload generates ten individual part requests.

That means:

```
100,000 logical uploads
1,000,000 S3 part upload requests
```

Your backend does not need to receive one million file-data requests.

That is one of the major advantages of direct-to-S3 uploads.

Your backend handles:

- Upload creation.
- Authorization.
- Presigned URL generation.
- Upload completion.
- Metadata updates.
- Processing coordination.

S3 handles the actual data transfer.

### Where can scaling problems occur?

PostgreSQL

Avoid writing a database row for every part unless there is a genuine business need.

A file-level row and a session-level row are often sufficient.

Use appropriate indexes:

```
CREATE INDEX idx_files_owner_status
ON files(owner_id, status);

CREATE INDEX idx_upload_sessions_status_expiry
ON file_upload_sessions(status, expires_at);
```

Backend

Make the service stateless so it can scale horizontally.

Use conditional updates or distributed coordination for operations such as initiation and completion.

S3

Generate presigned URLs using AWS SDKs and appropriate IAM permissions.

Avoid proxying file bytes through your backend.

SQS

Use it for downstream asynchronous work, such as:

- Virus scanning.
- Thumbnail generation.
- Document extraction.
- Metadata extraction.
- Transcoding.
- Notifications.

Cleanup workers

Run scheduled jobs to find expired sessions and stale records.

You can partition or batch cleanup operations as volume increases.

## 16. Failure recovery matrix

Here is a useful production checklist.

| Situation                                       | What should happen?                                           |
| ----------------------------------------------- | ------------------------------------------------------------- |
| Create API response lost                        | Retry using the same idempotency key                          |
| S3 initiation response lost                     | Reconcile the multipart session before creating another       |
| Presigned URL expired                           | Generate another URL for the same part                        |
| Part upload fails                               | Retry that part                                               |
| Part upload succeeded but response lost         | ListParts or retry identical bytes using the same part number |
| Client refreshes browser                        | Recover the session and query uploaded parts                  |
| User abandons upload                            | Expire and abort the session                                  |
| Missing part during completion                  | Reject completion and ask for the missing part                |
| Duplicate completion requests                   | Lock/conditionally transition the upload state                |
| S3 completion succeeds but DB update fails      | HEAD the final object and reconcile DB                        |
| Complete request times out                      | Check whether the final object exists before retrying         |
| Processing message is duplicated                | Idempotent worker                                             |
| S3 event arrives twice                          | Deduplicate event processing                                  |
| Database record exists but S3 upload is missing | Reconciliation and expiration                                 |
| S3 multipart upload is orphaned                 | Abort through cleanup worker or lifecycle rule                |

## 17. My final recommended design

Let's bring everything together into a set of architectural decisions.

| Concern                      | Recommended decision                           |
| ---------------------------- | ---------------------------------------------- |
| File metadata                | PostgreSQL                                     |
| Actual file bytes            | S3                                             |
| Large file upload            | S3 Multipart Upload                            |
| Small file upload            | Presigned PutObject                            |
| Object key generation        | Backend generates unique key per attempt       |
| Upload session ID            | Store in PostgreSQL                            |
| Chunk storage                | Temporarily managed by S3                      |
| Final file storage           | Single assembled S3 object                     |
| Chunk metadata table         | Optional, not required initially               |
| Actual uploaded parts        | S3 ListParts is authoritative                  |
| Presigned URL generation     | Backend, in batches                            |
| Upload transfer              | Client directly to S3                          |
| Upload completion            | Backend calls CompleteMultipartUpload          |
| Completion verification      | ListParts, CompleteMultipartUpload, HeadObject |
| Integrity                    | S3 checksum validation                         |
| Retry handling               | Idempotency keys and part-number retries       |
| Duplicate completion         | Conditional status transitions                 |
| Orphan cleanup               | Application worker + S3 lifecycle              |
| File processing              | SQS + workers                                  |
| Processing event reliability | Transactional outbox                           |
| File availability            | Only after validation                          |

### The final architecture in one diagram

\#chatgpt-mermaid-\_r_5lj\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5lj\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5lj\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5lj\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5lj\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5lj\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5lj\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5lj\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5lj\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5lj\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5lj\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5lj\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5lj\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5lj\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5lj\_ p{margin:0;}#chatgpt-mermaid-\_r_5lj\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5lj\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5lj\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5lj\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5lj\_ .label text,#chatgpt-mermaid-\_r_5lj\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5lj\_ .node rect,#chatgpt-mermaid-\_r_5lj\_ .node circle,#chatgpt-mermaid-\_r_5lj\_ .node ellipse,#chatgpt-mermaid-\_r_5lj\_ .node polygon,#chatgpt-mermaid-\_r_5lj\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5lj\_ .rough-node .label text,#chatgpt-mermaid-\_r_5lj\_ .node .label text,#chatgpt-mermaid-\_r_5lj\_ .image-shape .label,#chatgpt-mermaid-\_r_5lj\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5lj\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5lj\_ .rough-node .label,#chatgpt-mermaid-\_r_5lj\_ .node .label,#chatgpt-mermaid-\_r_5lj\_ .image-shape .label,#chatgpt-mermaid-\_r_5lj\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5lj\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5lj\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5lj\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5lj\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5lj\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5lj\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5lj\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5lj\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5lj\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5lj\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5lj\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5lj\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5lj\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5lj\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5lj\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5lj\_ .icon-shape,#chatgpt-mermaid-\_r_5lj\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5lj\_ .icon-shape p,#chatgpt-mermaid-\_r_5lj\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5lj\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5lj\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5lj\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5lj\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5lj\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5lj\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5lj\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5lj\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5lj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5lj\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5lj\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5lj\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5lj\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5lj\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5lj\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5lj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5lj\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5lj\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5lj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5lj\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5lj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5lj\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5lj\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5lj\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5lj\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5lj\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5lj\_ .node rect,#chatgpt-mermaid-\_r_5lj\_ .node circle,#chatgpt-mermaid-\_r_5lj\_ .node ellipse,#chatgpt-mermaid-\_r_5lj\_ .node polygon,#chatgpt-mermaid-\_r_5lj\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5lj\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5lj\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5lj\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5lj\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5lj\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}User / BrowserFile Upload ServicePostgreSQLS3 Object StorageSQSProcessing WorkerCreate uploadCreate file + sessionCreateMultipartUploadUploadIdSave UploadIdUpload session detailsRequest part URLsPresigned UploadPart URLsPart 1, 2, 3 ... NComplete requestListPartsUploaded parts + ETagsCompleteMultipartUploadFinal object createdHEAD verificationVerified object metadataUPLOADED + Outbox eventOutbox publisherScan / ValidateAVAILABLE / REJECTED

## 18. Important distinctions to remember

There are a few principles that I would particularly remember for system design interviews and real implementations.

1\. File record and physical file are different things.

PostgreSQL maintains the logical file record. S3 maintains the actual object.

2\. Initiating multipart upload doesn't create the final object.

It creates an upload session.

3\. Chunks are not separate application files.

S3 manages the temporary parts and assembles one final object.

4\. The client is not the source of truth for upload completion.

The client requests finalization. Your backend verifies S3.

5\. A part upload success does not mean the file upload succeeded.

All required parts must be assembled successfully.

6\. Multipart upload and file processing are separate workflows.

An uploaded file might still need malware scanning or other validations.

7\. PostgreSQL and S3 do not share a transaction.

You need idempotency, compensating actions, and reconciliation.

8\. Retries must be designed around the operation being retried.

Retrying an individual part is different from retrying multipart initialization or completion.

9\. Abandoned uploads need cleanup.

Incomplete multipart uploads consume storage until completed or aborted.

10\. S3 events are useful signals, not exactly-once guarantees.

Consumers must tolerate duplicates and out-of-order delivery.

## 19. How I would evolve this implementation

I would not build every possible mechanism on day one.

I would evolve the solution in these stages:

| Stage | Features                                                                              |
| ----- | ------------------------------------------------------------------------------------- |
| V1    | PostgreSQL file record, S3 presigned upload, backend verification                     |
| V2    | Multipart upload, concurrent parts, retries and resumability                          |
| V3    | Idempotency, completion locking, reconciliation and cleanup                           |
| V4    | Checksum validation, malware scanning and processing pipeline                         |
| V5    | Advanced observability, progress tracking, upload analytics and content deduplication |

The foundation should be correct from V1: unique object keys, private storage, explicit state transitions, idempotent APIs, and recovery from partial failures.

The architecture I would choose for a production document/file management service is:

PostgreSQL + S3 Multipart Upload + presigned part URLs + backend-controlled completion + S3 verification + transactional outbox + SQS processing workers.

This gives you direct and scalable data transfer, resumability, reliable completion, and a clean separation between storage and business logic.

The same fundamental design works for document management, media uploads, backups, data imports, user attachments, and large dataset ingestion.
