If you're designing a **LeetCode-like platform**, I'd model it around one core idea:

> **Users solve problems by submitting code, and the system executes that code against hidden/public test cases and records the result.**

I'll keep the first version interview-friendly, then point out where the design needs to evolve for scale.

---

# 1. Core Requirements

### Functional

* User registration/login
* Browse/search coding problems
* View problem statement, constraints, examples
* Submit code in a language
* Execute submission against test cases
* Return:

  * Accepted
  * Wrong Answer
  * Time Limit Exceeded
  * Runtime Error
  * Compilation Error
* Store submission history
* Track solved problems
* Support problem difficulty/tags
* Support multiple programming languages
* Discussions/contests can be added later

### Non-functional

* Code execution must be **isolated**
* One user's code must not affect another user's execution
* Submissions should be asynchronous
* System should handle many concurrent submissions
* Problem/test-case data should be protected from users

---

# 2. Core Entities

I'd start with these:

```text
User
Problem
ProblemTag
Tag
TestCase
Language
Submission
SubmissionResult
```

Relationship:

```text
User
  |
  | 1:N
  v
Submission
  |
  | N:1
  v
Problem
  |
  | 1:N
  v
TestCase

Problem N:M Tag

Submission N:1 Language
```

---

# 3. Database Schema

Assume PostgreSQL.

## User

```sql
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

# 4. Problem

```sql
CREATE TABLE problems (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) NOT NULL UNIQUE,

    description     TEXT NOT NULL,

    difficulty      VARCHAR(20) NOT NULL,
    
    constraints     TEXT,

    is_published    BOOLEAN NOT NULL DEFAULT FALSE,

    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

Example:

```text
id:          1
title:       Two Sum
slug:        two-sum
difficulty:  EASY
```

---

# 5. Tags

```sql
CREATE TABLE tags (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL UNIQUE
);
```

Examples:

```text
array
hash-table
two-pointers
binary-search
dynamic-programming
graph
```

Since one problem can have multiple tags and one tag can belong to many problems:

```sql
CREATE TABLE problem_tags (
    problem_id   BIGINT NOT NULL REFERENCES problems(id),
    tag_id       BIGINT NOT NULL REFERENCES tags(id),

    PRIMARY KEY (problem_id, tag_id)
);
```

This is an important interview point:

> Don't store `"array,hash-table"` inside the `problems` table. This is a many-to-many relationship.

---

# 6. Test Cases

```sql
CREATE TABLE test_cases (
    id              BIGSERIAL PRIMARY KEY,
    problem_id      BIGINT NOT NULL REFERENCES problems(id),

    input_data      TEXT NOT NULL,
    expected_output TEXT NOT NULL,

    is_hidden       BOOLEAN NOT NULL DEFAULT TRUE,

    time_limit_ms   INT NOT NULL,
    memory_limit_mb INT,

    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

For Two Sum:

```text
input:
[2,7,11,15]
9

output:
[0,1]
```

### Why separate test cases?

Because a problem has:

```text
Problem
   |
   +-- Test Case 1
   +-- Test Case 2
   +-- Test Case 3
   +-- ...
```

Also, hidden test cases **must never be returned through the public problem API**.

---

# 7. Languages

```sql
CREATE TABLE languages (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50) NOT NULL UNIQUE,
    version         VARCHAR(50) NOT NULL,

    compiler_command TEXT,
    runtime_command  TEXT,

    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE
);
```

Example:

```text
Java       21
Python     3.13
C++        17
JavaScript Node 22
Go         1.24
```

---

# 8. Submissions

This is one of the most important tables.

```sql
CREATE TABLE submissions (
    id              BIGSERIAL PRIMARY KEY,

    user_id         BIGINT NOT NULL REFERENCES users(id),
    problem_id      BIGINT NOT NULL REFERENCES problems(id),
    language_id     BIGINT NOT NULL REFERENCES languages(id),

    source_code     TEXT NOT NULL,

    status          VARCHAR(30) NOT NULL,

    execution_time_ms INT,
    memory_used_mb    INT,

    error_message   TEXT,

    submitted_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
```

Possible status:

```text
QUEUED
RUNNING
ACCEPTED
WRONG_ANSWER
TIME_LIMIT_EXCEEDED
MEMORY_LIMIT_EXCEEDED
RUNTIME_ERROR
COMPILATION_ERROR
SYSTEM_ERROR
```

---

# 9. Submission Test Results

A submission may execute against 100 test cases.

Therefore:

```text
Submission
   |
   +---- Test Case 1 → PASS
   +---- Test Case 2 → PASS
   +---- Test Case 3 → FAIL
   +---- ...
```

So we can have:

```sql
CREATE TABLE submission_results (
    id                  BIGSERIAL PRIMARY KEY,

    submission_id       BIGINT NOT NULL
                        REFERENCES submissions(id),

    test_case_id        BIGINT NOT NULL
                        REFERENCES test_cases(id),

    status              VARCHAR(30) NOT NULL,

    execution_time_ms   INT,
    memory_used_mb      INT,

    actual_output       TEXT,
    error_message       TEXT
);
```

---

# 10. Complete ER Model

Conceptually:

```text
                       ┌──────────────┐
                       │    users     │
                       └──────┬───────┘
                              │
                              │ 1:N
                              ▼
                       ┌──────────────┐
                       │ submissions  │
                       └──────┬───────┘
                              │
                   ┌──────────┴──────────┐
                   │                     │
                  N:1                   N:1
                   │                     │
                   ▼                     ▼
             ┌───────────┐       ┌───────────┐
             │ problems  │       │ languages │
             └─────┬─────┘       └───────────┘
                   │
             ┌─────┴──────┐
             │            │
            1:N          N:M
             │            │
             ▼            ▼
       ┌───────────┐   ┌───────────┐
       │ test_cases│   │   tags    │
       └─────┬─────┘   └─────┬─────┘
             │                │
             │                │
             └───────┐  ┌─────┘
                     ▼  ▼
                problem_tags


submissions
     │
     │ 1:N
     ▼
submission_results
     │
     │ N:1
     ▼
 test_cases
```

---

# 11. API Design

Now let's design REST APIs.

## Authentication

### Register

```http
POST /api/v1/auth/register
```

Request:

```json
{
  "username": "riyaz",
  "email": "riyaz@example.com",
  "password": "********"
}
```

Response:

```json
{
  "userId": 101,
  "username": "riyaz"
}
```

---

### Login

```http
POST /api/v1/auth/login
```

Response:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 900
}
```

---

# 12. Problem APIs

### List problems

```http
GET /api/v1/problems
```

Possible query parameters:

```http
GET /api/v1/problems?page=0&size=20
GET /api/v1/problems?difficulty=EASY
GET /api/v1/problems?tag=array
GET /api/v1/problems?search=two
```

Response:

```json
{
  "content": [
    {
      "id": 1,
      "title": "Two Sum",
      "slug": "two-sum",
      "difficulty": "EASY"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 500
}
```

---

### Get problem

```http
GET /api/v1/problems/{problemId}
```

Response:

```json
{
  "id": 1,
  "title": "Two Sum",
  "difficulty": "EASY",
  "description": "...",
  "constraints": [
    "2 <= nums.length <= 10^4"
  ],
  "examples": [
    {
      "input": "[2,7,11,15], target = 9",
      "output": "[0,1]"
    }
  ],
  "tags": [
    "array",
    "hash-table"
  ]
}
```

Notice:

**We don't return hidden test cases.**

---

# 13. Submission API

This is the most important API.

```http
POST /api/v1/submissions
```

Request:

```json
{
  "problemId": 1,
  "languageId": 1,
  "sourceCode": "class Solution { ... }"
}
```

Response:

```json
{
  "submissionId": 98765,
  "status": "QUEUED"
}
```

Why return `QUEUED` rather than waiting?

Because code execution can take seconds or even minutes.

We don't want:

```text
HTTP Request
     |
     | wait 30 seconds
     |
     ▼
Code execution
     |
     ▼
HTTP Response
```

Instead:

```text
Client
  |
  | POST submission
  ▼
API Server
  |
  | create submission
  ▼
DB
  |
  | publish job
  ▼
Queue
  |
  | 202 Accepted
  ▼
Client
```

Then workers execute it asynchronously.

---

# 14. Submission Status

```http
GET /api/v1/submissions/{submissionId}
```

Response while running:

```json
{
  "id": 98765,
  "status": "RUNNING"
}
```

Later:

```json
{
  "id": 98765,
  "status": "ACCEPTED",
  "executionTimeMs": 42,
  "memoryUsedMb": 128
}
```

---

# 15. Submission History

```http
GET /api/v1/users/me/submissions
```

Possible filters:

```http
GET /api/v1/users/me/submissions?problemId=1
GET /api/v1/users/me/submissions?status=ACCEPTED
GET /api/v1/users/me/submissions?page=0&size=20
```

---

# 16. Architecture Behind Submission API

This is where the LeetCode design becomes interesting.

Don't do:

```text
API Server
    |
    | directly execute Java/Python/C++
    ▼
Machine
```

That's extremely dangerous.

A user can submit:

```python
while True:
    pass
```

or potentially malicious code.

Instead:

```text
                     ┌───────────────┐
                     │   API Server  │
                     └───────┬───────┘
                             │
                             ▼
                         PostgreSQL
                             │
                             │ submission
                             ▼
                         Message Queue
                             │
                    ┌────────┴────────┐
                    │                 │
                    ▼                 ▼
               Worker 1          Worker 2
                    │                 │
                    ▼                 ▼
              Sandbox/VM        Sandbox/VM
                    │                 │
                    └────────┬────────┘
                             │
                             ▼
                          Results
                             │
                             ▼
                         PostgreSQL
```

---

# 17. Code Execution Flow

Suppose user submits:

```text
Two Sum
Java
source code
```

### Step 1

API creates:

```text
submission_id = 98765
status = QUEUED
```

### Step 2

Publish:

```json
{
  "submissionId": 98765
}
```

to a queue.

### Step 3

Execution worker consumes it.

```text
QUEUED
   ↓
RUNNING
```

### Step 4

Worker retrieves:

```text
source code
language
problem
test cases
limits
```

### Step 5

Create isolated execution environment:

```text
Sandbox
 ├── CPU limit
 ├── Memory limit
 ├── Execution time limit
 ├── Network disabled
 ├── Filesystem restricted
 └── Process limit
```

### Step 6

Compile if necessary.

For Java:

```text
source.java
    ↓
javac
    ↓
.class
```

If compilation fails:

```text
COMPILATION_ERROR
```

### Step 7

Run test cases.

```text
Test 1 → PASS
Test 2 → PASS
Test 3 → PASS
Test 4 → FAIL
```

Therefore:

```text
WRONG_ANSWER
```

### Step 8

Store result:

```text
submission.status = WRONG_ANSWER
```

and individual results in:

```text
submission_results
```

---

# 18. API vs Execution Service

I'd separate these responsibilities.

```text
                ┌──────────────────┐
                │   API Service    │
                │                  │
                │ Problems         │
                │ Users            │
                │ Submissions      │
                └────────┬─────────┘
                         │
                         ▼
                      Queue
                         │
                         ▼
                ┌──────────────────┐
                │ Execution Service│
                │                  │
                │ Compile          │
                │ Run              │
                │ Sandbox          │
                │ Resource limits  │
                └──────────────────┘
```

Why?

Because API servers should not be responsible for running arbitrary user code.

---

# 19. Important Database Indexes

We shouldn't just create tables.

For example:

```sql
CREATE INDEX idx_submissions_user
ON submissions(user_id, submitted_at DESC);
```

Useful for:

```http
GET /users/me/submissions
```

Problem submissions:

```sql
CREATE INDEX idx_submissions_problem
ON submissions(problem_id, submitted_at DESC);
```

Status filtering:

```sql
CREATE INDEX idx_submissions_user_status
ON submissions(user_id, status);
```

Test cases:

```sql
CREATE INDEX idx_test_cases_problem
ON test_cases(problem_id);
```

Problem tags:

```sql
CREATE INDEX idx_problem_tags_tag
ON problem_tags(tag_id, problem_id);
```

---

# 20. One Important Design Question

A common interview question:

> **Should `submission_results` store every test case result?**

It depends.

### Option A — Store every result

```text
submission_results
```

Advantages:

* Debugging
* Analytics
* Admin visibility
* Detailed execution history

Disadvantages:

If:

```text
10M submissions/day
×
100 test cases
```

that's:

```text
1 billion rows/day
```

Huge.

### Option B — Store only aggregate result

`submissions`:

```text
status
execution_time
memory
failed_test_case
```

Much cheaper.

### Production approach

Often:

```text
Submission DB
    |
    +── aggregate result
    |
    +── detailed execution logs → object storage / log system
```

You don't necessarily need a permanent relational row for every test-case execution.

---

# 21. Another Important Design Question: Duplicate Submissions

Suppose a user clicks **Submit** 10 times.

You probably still want 10 submissions because they represent actual attempts.

But you may want to identify duplicate source code:

```text
SHA-256(source_code)
```

Add:

```sql
source_hash VARCHAR(64)
```

Then you can detect:

```text
same user
same problem
same language
same source hash
```

This can help with:

* duplicate submission analysis
* abuse detection
* caching opportunities

But **don't blindly cache execution results** unless the execution environment and test-suite version are identical.

---

# 22. Problem Versioning — Important Production Concern

Imagine problem #1 has:

```text
20 test cases
```

You later add:

```text
10 new test cases
```

What happens to old submissions?

Their result was generated against the old test suite.

Therefore a production system should consider:

```text
problem_version
```

For example:

```text
Problem 1
   |
   ├── Version 1
   │     └── Test Cases
   │
   └── Version 2
         └── Test Cases
```

Submission stores:

```text
problem_version_id
```

instead of assuming the current problem definition forever.

This is a **very good system-design interview point**.

---

# 23. Final API Set

For the basic system:

```text
Authentication
────────────────────────────────
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh


Problems
────────────────────────────────
GET    /api/v1/problems
GET    /api/v1/problems/{id}


Submissions
────────────────────────────────
POST   /api/v1/submissions
GET    /api/v1/submissions/{id}
GET    /api/v1/users/me/submissions


Tags
────────────────────────────────
GET    /api/v1/tags
GET    /api/v1/problems?tag=array
```

Later:

```text
Discussions
Contests
Leaderboards
User profiles
Problem lists
Favorites
Progress tracking
Editorials
Solutions
Achievements
```

---

# 24. High-Level Architecture

The initial interview design I'd draw is:

```text
                         ┌──────────────┐
                         │    Client    │
                         └──────┬───────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   API Gateway   │
                       └────────┬────────┘
                                │
               ┌────────────────┼────────────────┐
               │                │                │
               ▼                ▼                ▼
          User Service    Problem Service   Submission API
               │                │                │
               │                │                ▼
               │                │           PostgreSQL
               │                │                │
               └────────────────┴────────────────┘
                                │
                                ▼
                              Queue
                                │
                     ┌──────────┴──────────┐
                     ▼                     ▼
              Execution Worker      Execution Worker
                     │                     │
                     ▼                     ▼
                  Sandbox               Sandbox
                     │                     │
                     └──────────┬──────────┘
                                │
                                ▼
                           Result Store
```

The **most important architectural boundary** is:

```text
                     UNTRUSTED
                       CODE
                        │
                        ▼
                ┌───────────────┐
                │    Sandbox    │
                │               │
                │ CPU limit     │
                │ Memory limit  │
                │ Time limit    │
                │ No network    │
                │ Restricted FS │
                └───────────────┘
```

That is the part I'd expect an interviewer to spend the most time on after the basic DB/API design.

### The next level of this problem

For an interview, I'd then deliberately **break this simple design** with progressively harder requirements:

1. **Why can't API server execute code directly?**
2. **How do we securely sandbox arbitrary Java/Python/C++ code?**
3. **How do we handle 100K submissions/minute?**
4. **How do we prevent one language's workers from becoming overloaded?**
5. **How do we prioritize submissions?**
6. **How do we handle worker crashes/timeouts?**
7. **How do we guarantee a submission isn't executed twice?**
8. **How do we scale the execution workers independently?**
9. **How do we protect hidden test cases?**
10. **How do we support contests and leaderboards?**
11. **How do we version problems/test cases?**
12. **How do we handle rejudging 10 million old submissions when test cases change?**

Those questions turn this from a CRUD application into a **strong system-design interview problem**.

