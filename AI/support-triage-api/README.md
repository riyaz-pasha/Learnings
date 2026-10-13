# Support Triage Helper

Minimal local API for learning. Classifies fictional/sanitized support ticket text into a placeholder category and summary. No external systems, no customer data, no credentials.

## File structure

```
support-triage-api/
├── .mise.toml
├── .python-version
├── pyproject.toml
├── .gitignore
├── README.md
└── main.py
```

## Prerequisites

- [mise](https://mise.jdx.dev/) for Python version management
- [uv](https://docs.astral.sh/uv/) for dependencies

## Dependency installation

```bash
cd /Users/riyaz/Personal/Learning/AI/support-triage-api
mise install          # installs Python 3.13
uv sync              # creates .venv and installs FastAPI/uvicorn
# or: mise run install
```

## Run command

```bash
mise run dev
# equivalent: uv run uvicorn main:app --reload --port 8000
```

The API listens on `http://127.0.0.1:8000`.

## Request/response contract

- `GET /health`
  - 200 -> `{"status": "ok"}`
- `POST /triage-ticket`
  - Request: `Content-Type: application/json`, body `{"ticket_text": "<string>"}`
  - `ticket_text` is required, must be a string, and must not be empty/whitespace-only.
  - Success 200 -> `{"category": "<string>", "summary": "<string (<=140 chars)>"}`
  - Validation error 400 -> `{"detail": "<string>"}`

Categories (placeholder rules): `damaged_item` (text contains "damaged"), `refund_request` (text contains "refund"), otherwise `general_support`.

## Success test command

```bash
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://127.0.0.1:8000/triage-ticket \
  -H 'Content-Type: application/json' \
  -d '{"ticket_text":"My laptop arrived damaged and scratched."}'
```

Actual response:

```text
{"category":"damaged_item","summary":"My laptop arrived damaged and scratched."}
HTTP_STATUS:200
```

## Error test commands

### Missing `ticket_text`

```bash
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://127.0.0.1:8000/triage-ticket \
  -H 'Content-Type: application/json' \
  -d '{}'
```

Actual response:

```text
{"detail":"ticket_text is required"}
HTTP_STATUS:400
```

### Empty `ticket_text`

```bash
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://127.0.0.1:8000/triage-ticket \
  -H 'Content-Type: application/json' \
  -d '{"ticket_text":""}'
```

Actual response:

```text
{"detail":"ticket_text is required"}
HTTP_STATUS:400
```

### Non-string `ticket_text` (numeric)

```bash
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://127.0.0.1:8000/triage-ticket \
  -H 'Content-Type: application/json' \
  -d '{"ticket_text":123}'
```

Actual response:

```text
{"detail":"ticket_text must be a string"}
HTTP_STATUS:400
```

(Whitespace-only values such as `"   "` are also rejected with `{"detail":"ticket_text is required"}` and HTTP 400.)
