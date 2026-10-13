from fastapi import FastAPI, HTTPException, Request

app = FastAPI(title="Support Triage Helper")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/triage-ticket")
async def triage_ticket(request: Request):
    # Parse defensively so a missing/malformed body yields a controlled
    # JSON error instead of an unhandled exception.
    try:
        body = await request.json()
    except Exception:
        body = {}

    if not isinstance(body, dict):
        raise HTTPException(status_code=400, detail="Request body must be a JSON object")

    ticket_text = body.get("ticket_text", "")

    if not isinstance(ticket_text, str):
        raise HTTPException(status_code=400, detail="ticket_text must be a string")

    ticket_text = ticket_text.strip()

    if not ticket_text:
        raise HTTPException(status_code=400, detail="ticket_text is required")

    lower_text = ticket_text.lower()

    if "damaged" in lower_text:
        category = "damaged_item"
    elif "refund" in lower_text:
        category = "refund_request"
    else:
        category = "general_support"

    return {
        "category": category,
        "summary": ticket_text[:140],
    }
