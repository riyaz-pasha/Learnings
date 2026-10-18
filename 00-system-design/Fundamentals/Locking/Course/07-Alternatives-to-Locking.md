# Chapter 7: Alternatives to Locking — Designing Systems Without Unnecessary Locks

So far, we've covered pessimistic locking, optimistic locking, PostgreSQL concurrency, Redis locks, and distributed locking.

Now let's explore an important system design principle:

The best locking strategy is sometimes to avoid locking entirely.

That doesn't mean ignoring concurrency. It means choosing a mechanism that protects the required invariant with less contention, simpler failure handling, or better scalability.

We'll cover six alternatives:

1. Atomic database operations.
2. Compare-and-swap (CAS).
3. Optimistic concurrency control.
4. Database constraints.
5. Idempotency keys and deduplication.
6. Queue-based serialization and single-writer designs.

## 1. Atomic operations: Let the database decide

Imagine an e-commerce application has one item left in stock.

Two customers attempt to purchase it simultaneously.

A naive implementation might look like this:

```
1. Read stock.
2. If stock > 0:
       Reduce stock by 1.
       Create the order.
```

The problem is that the read and update are separate operations.

Both requests might read `stock = 1` before either updates it. Both then decide that the purchase is valid.

The result could be two orders for one item.

### 1.1 The locking-based approach

One solution is to lock the inventory row:

```
BEGIN;

SELECT stock
FROM inventory
WHERE product_id = 101
FOR UPDATE;

-- Application checks stock > 0.
-- Application updates stock and creates the order.

COMMIT;
```

This can be correct when the entire workflow is implemented properly. However, it requires a transaction, an explicit row lock, and application logic to manage the state transition.

For a simple decrement, there is a more direct approach.

### 1.2 The atomic conditional update

```
UPDATE inventory
SET stock = stock - 1
WHERE product_id = 101
  AND stock > 0
RETURNING stock;
```

This single statement expresses the business rule:

> Decrease stock only if stock is currently greater than zero.

If two requests compete for the last item, PostgreSQL coordinates the conflicting updates. Under the default `READ COMMITTED` isolation level, a waiting update rechecks its condition against the updated row.

Conceptually:

Request BPostgreSQLRequest ARequest BPostgreSQLRequest A#chatgpt-mermaid-\_r_42v\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_42v\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_42v\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_42v\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_42v\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_42v\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_42v\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_42v\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_42v\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_42v\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_42v\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_42v\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_42v\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_42v\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_42v\_ p{margin:0;}#chatgpt-mermaid-\_r_42v\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_42v\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_42v\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_42v\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_42v\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_42v\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_42v\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_42v\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_42v\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_42v\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_42v\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_42v\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_42v\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_42v\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_42v\_ .labelText,#chatgpt-mermaid-\_r_42v\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_42v\_ .loopText,#chatgpt-mermaid-\_r_42v\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_42v\_ .sectionTitle,#chatgpt-mermaid-\_r_42v\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_42v\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_42v\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_42v\_ .noteText,#chatgpt-mermaid-\_r_42v\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_42v\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_42v\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_42v\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_42v\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_42v\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_42v\_ .actor-man circle,#chatgpt-mermaid-\_r_42v\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_42v\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_42v\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_42v\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_42v\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_42v\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_42v\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_42v\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_42v\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_42v\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_42v\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_42v\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_42v\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_42v\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_42v\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_42v\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_42v\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_42v\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_42v\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_42v\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Decrement if stock > 0Decrement if stock > 0One row updatedZero rows updated

The winning request updates the row. The other request finds that the condition no longer holds and updates zero rows.

The application can interpret the result as follows:

- One row returned: the reservation succeeded.
- Zero rows returned: stock was unavailable or the product did not exist.

If distinguishing those two failure cases matters, perform an appropriate additional check.

### 1.3 Is this really locking?

Yes, at the database implementation level, PostgreSQL still uses locks to coordinate conflicting writes.

The difference is that you don't need to acquire and manage an explicit lock in application code. The database performs the conditional state transition atomically.

This is an important interview distinction: atomic SQL is not "lock-free." It is often a simpler way to express the invariant.

### When should you use this?

Use atomic conditional updates for operations such as:

- Decrementing inventory.
- Reserving capacity.
- Claiming a resource if it is available.
- Updating a balance under a condition.
- Transitioning a record from one status to another.

For example, a payment record can be claimed for processing only if it is still pending:

```
UPDATE payments
SET status = 'PROCESSING'
WHERE payment_id = :payment_id
  AND status = 'PENDING'
RETURNING payment_id;
```

Only one competing transaction can successfully perform this transition from `PENDING` to `PROCESSING`.

The payment workflow still needs appropriate failure recovery and idempotency, but the state transition itself is atomic.

## 2. Compare-and-swap (CAS)

Compare-and-swap is a fundamental concurrency technique used in databases, programming languages, and distributed systems.

The idea is:

> Update a value only if it still equals the value I previously observed.

CAS typically involves three values:

- Expected value: What you believe the current value should be.
- New value: What you want to write.
- Current value: What is actually stored when the operation executes.

The operation succeeds only if the expected value matches the current value.

### 2.1 A simple example

Suppose a document has this state:

```
document_id = 101
content     = "Version A"
version     = 5
```

Two users read version 5.

- User A edits the document.
- User B edits the document.
- A saves first, producing version 6.
- B then attempts to save based on version 5.

B's change should not silently overwrite A's update if the product requires conflict detection.

We can implement this using a version column:

```
UPDATE documents
SET content = :new_content,
    version = version + 1
WHERE document_id = :document_id
  AND version = :expected_version
RETURNING version;
```

User A sends `expected_version = 5`. If the update succeeds, the version becomes 6.

User B also sends `expected_version = 5`. Since the current version is now 6, B's update affects zero rows.

\#chatgpt-mermaid-\_r_43s\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_43s\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_43s\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_43s\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_43s\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_43s\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_43s\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_43s\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_43s\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_43s\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_43s\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_43s\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_43s\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_43s\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_43s\_ p{margin:0;}#chatgpt-mermaid-\_r_43s\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_43s\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_43s\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_43s\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_43s\_ .label text,#chatgpt-mermaid-\_r_43s\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_43s\_ .node rect,#chatgpt-mermaid-\_r_43s\_ .node circle,#chatgpt-mermaid-\_r_43s\_ .node ellipse,#chatgpt-mermaid-\_r_43s\_ .node polygon,#chatgpt-mermaid-\_r_43s\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_43s\_ .rough-node .label text,#chatgpt-mermaid-\_r_43s\_ .node .label text,#chatgpt-mermaid-\_r_43s\_ .image-shape .label,#chatgpt-mermaid-\_r_43s\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_43s\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_43s\_ .rough-node .label,#chatgpt-mermaid-\_r_43s\_ .node .label,#chatgpt-mermaid-\_r_43s\_ .image-shape .label,#chatgpt-mermaid-\_r_43s\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_43s\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_43s\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_43s\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_43s\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_43s\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_43s\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_43s\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_43s\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_43s\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_43s\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_43s\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_43s\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_43s\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_43s\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_43s\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_43s\_ .icon-shape,#chatgpt-mermaid-\_r_43s\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_43s\_ .icon-shape p,#chatgpt-mermaid-\_r_43s\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_43s\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_43s\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_43s\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_43s\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_43s\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_43s\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_43s\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_43s\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_43s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_43s\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_43s\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_43s\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_43s\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_43s\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_43s\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_43s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_43s\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_43s\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_43s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_43s\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_43s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_43s\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_43s\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_43s\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_43s\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_43s\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_43s\_ .node rect,#chatgpt-mermaid-\_r_43s\_ .node circle,#chatgpt-mermaid-\_r_43s\_ .node ellipse,#chatgpt-mermaid-\_r_43s\_ .node polygon,#chatgpt-mermaid-\_r_43s\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_43s\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_43s\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_43s\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_43s\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_43s\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Read document version 5User A and User B editUser A saves with expectedversion 5Update succeeds; versionbecomes 6User B saves with expectedversion 5Update rejected: staleversion

The application can return an HTTP `409 Conflict` or another appropriate conflict response.

### 2.2 Why is CAS useful?

Without CAS, a stale update may overwrite newer work.

With CAS, each update says:

> Apply my change only if nobody has changed the value since I read it.

This is useful for:

- Editing documents.
- Updating user profiles.
- Managing workflow state.
- Updating configuration.
- Preventing duplicate state transitions.
- Implementing optimistic concurrency control.

### 2.3 CAS vs pessimistic locking

| Pessimistic locking                                           | CAS                                                                       |
| ------------------------------------------------------------- | ------------------------------------------------------------------------- |
| Acquire a lock before performing the critical operation       | Attempt the update and validate the expected state atomically             |
| Other conflicting operations may have to wait                 | Conflicting operations can fail immediately or be retried                 |
| Useful when conflicts are frequent or work must be serialized | Useful when conflicts are relatively infrequent                           |
| Can hold locks while work is performed                        | Usually avoids holding an explicit lock during the read and editing phase |

Remember that CAS does not eliminate the database's internal write coordination. It changes how the application detects and handles conflicts.

### 2.4 Should we retry after a CAS failure?

Not always.

Imagine two users edit the same document:

- A changes the address.
- B changes the phone number.

If B's save fails because the version is stale, the application should not blindly retry B's old update against the new version. Doing so may overwrite changes without the user knowing.

A better approach might be to:

1. Fetch the latest document.
2. Compare the changes.
3. Merge independent edits if safe.
4. Ask the user to resolve conflicting changes if necessary.
5. Retry using the new expected version.

For an atomic counter, retrying the operation may be straightforward. For a user-authored document, it requires more care.

Key takeaway: CAS detects stale state; the application decides how to resolve the conflict.

## 3. Optimistic concurrency control

Optimistic concurrency control is the broader strategy behind version-based CAS.

It assumes conflicts are sufficiently uncommon that we should allow operations to proceed without acquiring an explicit lock in advance, then detect conflicting changes when committing.

A typical flow is:

\#chatgpt-mermaid-\_r_44a\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_44a\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_44a\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_44a\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_44a\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44a\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_44a\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_44a\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_44a\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_44a\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_44a\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_44a\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44a\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44a\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_44a\_ p{margin:0;}#chatgpt-mermaid-\_r_44a\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44a\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44a\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44a\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_44a\_ .label text,#chatgpt-mermaid-\_r_44a\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44a\_ .node rect,#chatgpt-mermaid-\_r_44a\_ .node circle,#chatgpt-mermaid-\_r_44a\_ .node ellipse,#chatgpt-mermaid-\_r_44a\_ .node polygon,#chatgpt-mermaid-\_r_44a\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_44a\_ .rough-node .label text,#chatgpt-mermaid-\_r_44a\_ .node .label text,#chatgpt-mermaid-\_r_44a\_ .image-shape .label,#chatgpt-mermaid-\_r_44a\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_44a\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_44a\_ .rough-node .label,#chatgpt-mermaid-\_r_44a\_ .node .label,#chatgpt-mermaid-\_r_44a\_ .image-shape .label,#chatgpt-mermaid-\_r_44a\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_44a\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_44a\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44a\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44a\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_44a\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_44a\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_44a\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_44a\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_44a\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_44a\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_44a\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44a\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44a\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_44a\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_44a\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_44a\_ .icon-shape,#chatgpt-mermaid-\_r_44a\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_44a\_ .icon-shape p,#chatgpt-mermaid-\_r_44a\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_44a\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_44a\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_44a\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_44a\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_44a\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_44a\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_44a\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_44a\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_44a\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44a\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_44a\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_44a\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_44a\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44a\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_44a\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_44a\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44a\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_44a\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_44a\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44a\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_44a\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_44a\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_44a\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_44a\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_44a\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_44a\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_44a\_ .node rect,#chatgpt-mermaid-\_r_44a\_ .node circle,#chatgpt-mermaid-\_r_44a\_ .node ellipse,#chatgpt-mermaid-\_r_44a\_ .node polygon,#chatgpt-mermaid-\_r_44a\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_44a\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_44a\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_44a\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_44a\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_44a\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Read record and versionPerform workAttempt conditional updateExpected version matches?Commit successful updateDetect conflictReject, merge, or retry safelyYesNo

### 3.1 Where does the version come from?

There are several common approaches.

Version number

```
version = 12
```

Increment it after every successful update.

Timestamp

Compare a stored update timestamp with the value originally read. This can work, but timestamp precision, clock assumptions, and update semantics need careful handling.

Content hash

Compare a hash of the expected content with the current content. This can be useful in certain storage APIs, but hashes are not automatically equivalent to a complete concurrency-control protocol.

A monotonically increasing version number is often the easiest approach to understand and reason about.

### 3.2 What about the ABA problem?

Consider a shared value that changes:

```
A → B → A
```

A client reads `A`. Another client changes it to `B` and then back to `A`.

If the first client checks only whether the current value equals `A`, it may not notice that the state changed in between.

This is known as the ABA problem in concurrency.

A version number can detect this:

```
A, version 1
B, version 2
A, version 3
```

Even though the value is back to `A`, the version has changed.

The first client's expected version of 1 no longer matches the current version of 3.

This is one reason version-based CAS is often safer than comparing a value alone.

### 3.3 When should you use optimistic concurrency?

It's a strong fit when:

- Conflicts are relatively uncommon.
- Users may spend time editing data before saving.
- You want to avoid holding database locks across long application workflows.
- Conflicts can be reported or merged safely.

It is less attractive when conflicts are extremely frequent and repeated retries create substantial wasted work.

## 4. Database constraints: Make invalid states impossible

Sometimes, the best way to prevent concurrency bugs is to define a rule that the database refuses to violate.

Imagine a movie-ticket booking system. Multiple application instances receive requests to book the same seat for the same show.

A Redis lock might coordinate the requests, but the database can enforce the core invariant directly.

```
CREATE TABLE bookings (
    booking_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    show_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    CONSTRAINT unique_seat_per_show
        UNIQUE (show_id, seat_id)
);
```

This constraint means:

> A seat can have at most one booking for a particular show.

Even if two requests attempt the same insert concurrently, PostgreSQL will not allow both transactions to commit conflicting rows.

The losing request receives a uniqueness violation.

### 4.1 Why is this powerful?

Consider two transactions:

```
Transaction A                   Transaction B
-------------                   -------------
Insert show 50, seat 12         Insert show 50, seat 12
             \                   /
              \                 /
               Database checks
               unique constraint
                      |
              Only one succeeds
```

The application does not need to coordinate all instances through a separate Redis lock to preserve this particular invariant.

The database is the final authority for the data it stores.

### 4.2 Other useful constraints

Check constraint

```
ALTER TABLE inventory
ADD CONSTRAINT stock_nonnegative
CHECK (stock >= 0);
```

This prevents committed rows from having negative stock.

Unique constraint

```
ALTER TABLE users
ADD CONSTRAINT unique_email
UNIQUE (email);
```

This prevents duplicate email values under the database's uniqueness semantics.

Foreign key

```
ALTER TABLE orders
ADD CONSTRAINT fk_order_customer
FOREIGN KEY (customer_id)
REFERENCES customers(customer_id);
```

This ensures an order's customer reference points to an existing customer.

### 4.3 Constraints vs locks

A lock coordinates access while operations are in progress.

A constraint enforces a rule on the resulting database state.

| Locks                                  | Constraints                                                                                 |
| -------------------------------------- | ------------------------------------------------------------------------------------------- |
| Coordinate concurrent operations       | Reject invalid data states                                                                  |
| Can be held while work occurs          | Usually checked as part of database statements or transaction processing                    |
| May require careful lock ordering      | Centralize specific invariants in the database                                              |
| Can reduce concurrency through waiting | May cause a competing operation to fail rather than wait for application-level coordination |

Constraints don't replace locks in every situation. For example, a complex multi-row business invariant might require a transaction, locking, serializable isolation, or a different data model.

But when a unique or check constraint fully expresses the rule, use it rather than implementing the same rule only in application code.

Interview takeaway: Enforce important data invariants as close as possible to the resource that owns the data.

## 5. Idempotency keys: Handle duplicate requests safely

Locks and idempotency solve different problems.

A lock attempts to coordinate concurrent execution.

Idempotency ensures that repeating the same logical request does not produce repeated effects.

This is especially important in payment systems and APIs.

### 5.1 The payment retry problem

Imagine a client sends a payment request:

```
POST /payments
Amount: ₹1,000
```

The server processes the payment, but the response is lost because of a network timeout.

The client doesn't know whether the payment succeeded.

It retries.

Without idempotency, the system might charge the customer twice.

Notice that this can happen even if the original request was properly locked. The first operation might have completed before the lock was released, but the client never received its response.

### 5.2 Use an idempotency key

The client sends a stable key representing the logical operation:

```
POST /payments
Idempotency-Key: payment-request-abc123
Content-Type: application/json
```

If the client retries the same logical payment, it reuses the same key.

The server records the key and associates it with the payment result.

Conceptually:

\#chatgpt-mermaid-\_r_46j\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_46j\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_46j\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_46j\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_46j\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46j\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_46j\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_46j\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_46j\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_46j\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_46j\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_46j\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_46j\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_46j\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_46j\_ p{margin:0;}#chatgpt-mermaid-\_r_46j\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46j\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46j\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46j\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_46j\_ .label text,#chatgpt-mermaid-\_r_46j\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46j\_ .node rect,#chatgpt-mermaid-\_r_46j\_ .node circle,#chatgpt-mermaid-\_r_46j\_ .node ellipse,#chatgpt-mermaid-\_r_46j\_ .node polygon,#chatgpt-mermaid-\_r_46j\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_46j\_ .rough-node .label text,#chatgpt-mermaid-\_r_46j\_ .node .label text,#chatgpt-mermaid-\_r_46j\_ .image-shape .label,#chatgpt-mermaid-\_r_46j\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_46j\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_46j\_ .rough-node .label,#chatgpt-mermaid-\_r_46j\_ .node .label,#chatgpt-mermaid-\_r_46j\_ .image-shape .label,#chatgpt-mermaid-\_r_46j\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_46j\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_46j\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_46j\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_46j\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_46j\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_46j\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_46j\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_46j\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_46j\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_46j\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_46j\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46j\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46j\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_46j\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_46j\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_46j\_ .icon-shape,#chatgpt-mermaid-\_r_46j\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_46j\_ .icon-shape p,#chatgpt-mermaid-\_r_46j\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_46j\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_46j\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_46j\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_46j\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_46j\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_46j\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_46j\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_46j\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_46j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_46j\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_46j\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_46j\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_46j\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_46j\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_46j\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_46j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_46j\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_46j\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_46j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_46j\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_46j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_46j\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_46j\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_46j\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_46j\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_46j\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_46j\_ .node rect,#chatgpt-mermaid-\_r_46j\_ .node circle,#chatgpt-mermaid-\_r_46j\_ .node ellipse,#chatgpt-mermaid-\_r_46j\_ .node polygon,#chatgpt-mermaid-\_r_46j\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_46j\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_46j\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_46j\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_46j\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_46j\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Payment request withidempotency keyKey already processed?Return existing resultProcess payment safelyPersist key and resultReturn resultYesNo

A durable implementation needs database-level protection against concurrent requests using the same key.

For example:

```
CREATE TABLE idempotency_records (
    idempotency_key TEXT PRIMARY KEY,
    request_hash TEXT NOT NULL,
    status TEXT NOT NULL,
    response_json JSONB
);
```

The primary key prevents two committed records with the same key. The request hash helps detect accidental reuse of the same key for different request contents.

A complete payment implementation also needs a transaction strategy, status transitions, error recovery, and careful handling of external payment-provider calls. A table like this is only the starting point.

### 5.3 Why isn't the unique key enough?

Suppose two identical requests arrive simultaneously.

Both check whether the key exists and find that it doesn't.

Both start processing.

A later unique-key violation doesn't necessarily undo an external payment already made by the losing request.

Therefore, the operation needs an appropriate protocol that claims the key before executing side effects, records durable progress, and handles retries after failures.

For external payment providers, use their idempotency support where available, alongside durable local state and reconciliation.

### 5.4 Idempotency vs distributed locking

| Distributed lock                                 | Idempotency                                                    |
| ------------------------------------------------ | -------------------------------------------------------------- |
| Coordinates who may execute work                 | Deduplicates the effects of repeated logical requests          |
| Often temporary because of TTLs                  | Usually backed by durable records and retention rules          |
| Can fail to prevent overlap if ownership expires | Can make retries return an existing result                     |
| Useful for shared coordination                   | Useful for API retries, payment processing, and event handling |

These techniques can complement each other. Neither guarantees exactly-once execution across arbitrary independent systems by itself.

Key takeaway: If the problem is duplicate requests or retries, consider idempotency before introducing a distributed lock.

## 6. Queue-based serialization

Sometimes, you don't need many processes to compete for a lock. You can organize work so that commands are processed in a controlled order.

This is the idea behind queue-based serialization.

Imagine a bank account receives the following commands:

```
1. Deposit ₹500
2. Withdraw ₹200
3. Deposit ₹100
```

If operations must be applied in order for a given account, you can route them to a single processing lane for that account.

\#chatgpt-mermaid-\_r_479\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_479\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_479\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_479\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_479\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_479\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_479\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_479\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_479\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_479\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_479\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_479\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_479\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_479\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_479\_ p{margin:0;}#chatgpt-mermaid-\_r_479\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_479\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_479\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_479\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_479\_ .label text,#chatgpt-mermaid-\_r_479\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_479\_ .node rect,#chatgpt-mermaid-\_r_479\_ .node circle,#chatgpt-mermaid-\_r_479\_ .node ellipse,#chatgpt-mermaid-\_r_479\_ .node polygon,#chatgpt-mermaid-\_r_479\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_479\_ .rough-node .label text,#chatgpt-mermaid-\_r_479\_ .node .label text,#chatgpt-mermaid-\_r_479\_ .image-shape .label,#chatgpt-mermaid-\_r_479\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_479\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_479\_ .rough-node .label,#chatgpt-mermaid-\_r_479\_ .node .label,#chatgpt-mermaid-\_r_479\_ .image-shape .label,#chatgpt-mermaid-\_r_479\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_479\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_479\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_479\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_479\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_479\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_479\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_479\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_479\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_479\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_479\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_479\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_479\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_479\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_479\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_479\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_479\_ .icon-shape,#chatgpt-mermaid-\_r_479\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_479\_ .icon-shape p,#chatgpt-mermaid-\_r_479\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_479\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_479\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_479\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_479\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_479\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_479\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_479\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_479\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_479\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_479\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_479\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_479\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_479\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_479\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_479\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_479\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_479\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_479\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_479\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_479\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_479\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_479\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_479\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_479\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_479\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_479\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_479\_ .node rect,#chatgpt-mermaid-\_r_479\_ .node circle,#chatgpt-mermaid-\_r_479\_ .node ellipse,#chatgpt-mermaid-\_r_479\_ .node polygon,#chatgpt-mermaid-\_r_479\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_479\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_479\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_479\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_479\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_479\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Commands for account 101Partitioned queueWorker for partitionApply commands in orderAccount state

This design reduces concurrent writes to the same logical entity without requiring every command to acquire a distributed lock.

### 6.1 Partition by entity ID

For example:

```
partition = hash(account_id) % number_of_partitions
```

All commands for the same account are routed to the same partition, allowing them to be processed sequentially within that partition.

Different partitions can be processed concurrently.

```
Partition 0: Account A → Command 1 → Command 2
Partition 1: Account B → Command 1 → Command 2
Partition 2: Account C → Command 1 → Command 2
```

This creates concurrency across entities while preserving order within each entity's processing lane.

### 6.2 What problems remain?

A queue does not eliminate every concurrency problem.

You still need to think about:

- Duplicate message delivery.
- Consumer crashes.
- Message acknowledgment.
- Retry ordering.
- Poison messages.
- Partition reassignment.
- Idempotent processing.
- Whether external writes can occur out of order.

For example, if a consumer processes a message, updates a database, then crashes before acknowledging the message, the broker may deliver the message again.

The database operation should therefore be idempotent or protected by durable deduplication.

Also, a queue's ordering guarantee is usually scoped to a partition, message group, or other documented boundary—not necessarily to every message in the entire system.

### When is this approach useful?

- Account-level event processing.
- Order lifecycle updates.
- Inventory event processing.
- Per-customer workflow execution.
- Systems where commands for one entity must be applied in sequence.

Queue-based serialization is particularly useful when work is naturally asynchronous and can tolerate queueing latency.

## 7. Single-writer architecture

A single-writer design takes queue-based serialization one step further: for a particular resource or partition, one designated writer is responsible for applying mutations.

This doesn't necessarily mean one writer for the entire system. A scalable design can have many writers, each responsible for a different partition.

### 7.1 Example: Collaborative document service

Suppose multiple clients edit the same document.

Instead of allowing every application instance to write directly to the document state, route updates for that document to one logical writer.

\#chatgpt-mermaid-\_r_47u\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_47u\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_47u\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_47u\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_47u\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47u\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_47u\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_47u\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_47u\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_47u\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_47u\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_47u\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_47u\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_47u\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_47u\_ p{margin:0;}#chatgpt-mermaid-\_r_47u\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47u\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47u\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47u\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_47u\_ .label text,#chatgpt-mermaid-\_r_47u\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47u\_ .node rect,#chatgpt-mermaid-\_r_47u\_ .node circle,#chatgpt-mermaid-\_r_47u\_ .node ellipse,#chatgpt-mermaid-\_r_47u\_ .node polygon,#chatgpt-mermaid-\_r_47u\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_47u\_ .rough-node .label text,#chatgpt-mermaid-\_r_47u\_ .node .label text,#chatgpt-mermaid-\_r_47u\_ .image-shape .label,#chatgpt-mermaid-\_r_47u\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_47u\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_47u\_ .rough-node .label,#chatgpt-mermaid-\_r_47u\_ .node .label,#chatgpt-mermaid-\_r_47u\_ .image-shape .label,#chatgpt-mermaid-\_r_47u\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_47u\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_47u\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_47u\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_47u\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_47u\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_47u\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_47u\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_47u\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_47u\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_47u\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_47u\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47u\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47u\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_47u\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47u\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_47u\_ .icon-shape,#chatgpt-mermaid-\_r_47u\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_47u\_ .icon-shape p,#chatgpt-mermaid-\_r_47u\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_47u\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_47u\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_47u\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_47u\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_47u\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_47u\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_47u\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_47u\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_47u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_47u\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_47u\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_47u\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_47u\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_47u\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_47u\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_47u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_47u\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_47u\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_47u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_47u\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_47u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_47u\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_47u\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_47u\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_47u\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_47u\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_47u\_ .node rect,#chatgpt-mermaid-\_r_47u\_ .node circle,#chatgpt-mermaid-\_r_47u\_ .node ellipse,#chatgpt-mermaid-\_r_47u\_ .node polygon,#chatgpt-mermaid-\_r_47u\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_47u\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_47u\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_47u\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_47u\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_47u\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Client ADocument command streamClient BClient CSingle logical writerDocument state

The writer processes document commands in order and applies the required validation.

This reduces conflicts between writers because there is only one active logical write path for that document.

### 7.2 Does single-writer mean one server?

Not necessarily.

You can partition resources:

```
Writer 1 → Documents 1–1000
Writer 2 → Documents 1001–2000
Writer 3 → Documents 2001–3000
```

The system can scale horizontally while maintaining a single logical writer per resource or partition.

### 7.3 What about writer failure?

You still need failover. When a writer fails, another instance must take over.

This creates the same stale-owner problem we discussed in distributed locking.

Suppose Writer A is paused, Writer B takes over, and then A resumes. If both can write, the single-writer guarantee has been violated.

Therefore, a robust single-writer design may need:

- Leader election or partition ownership.
- A mechanism to prevent stale writers from committing.
- Fencing tokens or epoch validation.
- Durable state and replay.
- Idempotent command processing.

Important: Single-writer is an architectural strategy, not an automatic guarantee. Its correctness depends on how ownership and failover are implemented.

## 8. How to choose the right alternative

Let's consolidate the approaches we've learned.

| Technique                  | Best suited for                                           | Main trade-off                                        |
| -------------------------- | --------------------------------------------------------- | ----------------------------------------------------- |
| Atomic conditional update  | Simple state transitions                                  | Requires the operation to be expressible atomically   |
| CAS                        | Rejecting updates based on stale state                    | Conflicts require handling or retrying                |
| Optimistic concurrency     | Read-heavy workflows with relatively infrequent conflicts | Repeated conflicts waste work                         |
| Database constraints       | Enforcing data invariants                                 | Must be expressible as supported constraints          |
| Idempotency keys           | Duplicate API requests and retries                        | Requires durable state and a defined retry protocol   |
| Queue-based serialization  | Ordered asynchronous processing                           | Adds queueing latency and operational complexity      |
| Single-writer architecture | Ordered mutations for a resource or partition             | Ownership changes and failover require careful design |
| Pessimistic locking        | High-contention operations requiring serialization        | Waiting, deadlocks, and lock contention               |
| Distributed locks          | Coordination across independent application instances     | Leases, failure handling, and stale-owner risks       |

### A practical decision tree

\#chatgpt-mermaid-\_r_48d\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_48d\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_48d\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_48d\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_48d\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48d\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_48d\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_48d\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_48d\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_48d\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_48d\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_48d\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_48d\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_48d\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_48d\_ p{margin:0;}#chatgpt-mermaid-\_r_48d\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48d\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48d\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48d\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_48d\_ .label text,#chatgpt-mermaid-\_r_48d\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48d\_ .node rect,#chatgpt-mermaid-\_r_48d\_ .node circle,#chatgpt-mermaid-\_r_48d\_ .node ellipse,#chatgpt-mermaid-\_r_48d\_ .node polygon,#chatgpt-mermaid-\_r_48d\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_48d\_ .rough-node .label text,#chatgpt-mermaid-\_r_48d\_ .node .label text,#chatgpt-mermaid-\_r_48d\_ .image-shape .label,#chatgpt-mermaid-\_r_48d\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_48d\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_48d\_ .rough-node .label,#chatgpt-mermaid-\_r_48d\_ .node .label,#chatgpt-mermaid-\_r_48d\_ .image-shape .label,#chatgpt-mermaid-\_r_48d\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_48d\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_48d\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_48d\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_48d\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_48d\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_48d\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_48d\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_48d\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_48d\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_48d\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_48d\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48d\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48d\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_48d\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48d\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_48d\_ .icon-shape,#chatgpt-mermaid-\_r_48d\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_48d\_ .icon-shape p,#chatgpt-mermaid-\_r_48d\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_48d\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_48d\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_48d\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_48d\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_48d\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_48d\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_48d\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_48d\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_48d\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_48d\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_48d\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_48d\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_48d\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_48d\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_48d\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_48d\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_48d\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_48d\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_48d\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_48d\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_48d\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_48d\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_48d\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_48d\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_48d\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_48d\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_48d\_ .node rect,#chatgpt-mermaid-\_r_48d\_ .node circle,#chatgpt-mermaid-\_r_48d\_ .node ellipse,#chatgpt-mermaid-\_r_48d\_ .node polygon,#chatgpt-mermaid-\_r_48d\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_48d\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_48d\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_48d\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_48d\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_48d\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Need to protect a concurrentoperationIs it a database invariant?Can a constraint or atomicupdate enforce it?Use constraint or atomic SQLConsider transaction, rowlock, or SERIALIZABLEIs the issue duplicaterequests?Use idempotency anddurable deduplicationMust commands beprocessed in order?Consider a partitioned queueor single writerDo multiple instances needshared coordination?Consider a distributed lockor coordinatorUse local synchronization orno lock if safeYesYesNoNoYesNoYesNoYesNo

The decision tree is a starting point, not a substitute for understanding the failure modes. Some systems combine multiple techniques.

For example, an order-processing system might use a queue for ordering, idempotency for retries, a PostgreSQL transaction for durable state, and a conditional update for state transitions.

## 9. System design example: Preventing duplicate order processing

Suppose an e-commerce system processes orders asynchronously.

Requirements:

1. Multiple workers process orders concurrently.
2. An order must not be charged twice.
3. Failed jobs must be retried.
4. Processing must recover after worker crashes.

A distributed lock alone is not a complete solution.

### A better design

\#chatgpt-mermaid-\_r_48m\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_48m\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_48m\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_48m\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_48m\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48m\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_48m\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_48m\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_48m\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_48m\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_48m\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_48m\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_48m\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_48m\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_48m\_ p{margin:0;}#chatgpt-mermaid-\_r_48m\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48m\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48m\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48m\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_48m\_ .label text,#chatgpt-mermaid-\_r_48m\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48m\_ .node rect,#chatgpt-mermaid-\_r_48m\_ .node circle,#chatgpt-mermaid-\_r_48m\_ .node ellipse,#chatgpt-mermaid-\_r_48m\_ .node polygon,#chatgpt-mermaid-\_r_48m\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_48m\_ .rough-node .label text,#chatgpt-mermaid-\_r_48m\_ .node .label text,#chatgpt-mermaid-\_r_48m\_ .image-shape .label,#chatgpt-mermaid-\_r_48m\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_48m\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_48m\_ .rough-node .label,#chatgpt-mermaid-\_r_48m\_ .node .label,#chatgpt-mermaid-\_r_48m\_ .image-shape .label,#chatgpt-mermaid-\_r_48m\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_48m\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_48m\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_48m\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_48m\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_48m\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_48m\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_48m\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_48m\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_48m\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_48m\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_48m\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48m\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48m\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_48m\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_48m\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_48m\_ .icon-shape,#chatgpt-mermaid-\_r_48m\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_48m\_ .icon-shape p,#chatgpt-mermaid-\_r_48m\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_48m\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_48m\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_48m\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_48m\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_48m\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_48m\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_48m\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_48m\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_48m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_48m\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_48m\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_48m\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_48m\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_48m\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_48m\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_48m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_48m\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_48m\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_48m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_48m\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_48m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_48m\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_48m\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_48m\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_48m\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_48m\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_48m\_ .node rect,#chatgpt-mermaid-\_r_48m\_ .node circle,#chatgpt-mermaid-\_r_48m\_ .node ellipse,#chatgpt-mermaid-\_r_48m\_ .node polygon,#chatgpt-mermaid-\_r_48m\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_48m\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_48m\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_48m\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_48m\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_48m\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Order createdDurable job queueWorker claims orderCheck durable order stateAlready processed?Return or acknowledgeexisting resultProcess using idempotencyPersist state and resultAcknowledge jobYesNo

A production implementation could include:

1. A durable queue or outbox-based delivery mechanism.
2. A database record that tracks the order's processing state.
3. A conditional transition from `PENDING` to `PROCESSING`.
4. An idempotency key for the payment operation.
5. Durable success or failure state.
6. Retry and reconciliation handling for uncertain outcomes.

The conditional state transition might be:

```
UPDATE orders
SET status = 'PROCESSING'
WHERE order_id = :order_id
  AND status = 'PENDING'
RETURNING order_id;
```

This prevents two competing transactions from both successfully claiming the same pending state.

However, a crash can occur after claiming the order but before completing it. The system therefore needs a recovery strategy for abandoned processing states.

If payment processing is external to the database, the database transaction cannot atomically commit both the local order state and the provider's payment. Provider idempotency, durable state transitions, and reconciliation are needed to handle this cross-system boundary.

The key lesson is that reliable processing is usually built from several complementary guarantees, not from one lock.

## 10. Interview questions

### Q1. What is the difference between a lock and CAS?

A lock grants temporary exclusive access to a critical section. CAS performs an update only if the current state matches an expected value. CAS often allows conflicts to be detected without explicitly acquiring a lock in advance.

### Q2. Why is an atomic SQL update often better than a Redis lock?

If the invariant belongs to a database row, the database can perform the condition check and state transition atomically. A Redis lock introduces a separate coordination dependency and does not automatically make the database operation atomic with the lock.

### Q3. Does a unique constraint prevent race conditions?

It prevents committed data from violating the specified uniqueness rule, even when competing requests race. It doesn't prevent all possible concurrency problems or replace transaction logic for other invariants.

### Q4. What is the difference between idempotency and locking?

Locking coordinates concurrent execution. Idempotency makes repeated execution of the same logical operation safe or allows the system to return the existing result. Both may be needed.

### Q5. Why use a queue instead of a distributed lock?

A queue can provide controlled processing order and distribute work across consumers without making every worker compete for a global lock. It is useful when the work is naturally asynchronous, though delivery, retry, and deduplication semantics still matter.

### Q6. What is a single-writer architecture?

It routes mutations for a resource or partition through one logical writer at a time. It can reduce write conflicts and preserve ordering, but failover must prevent stale writers from continuing to modify state.

### Q7. What is the ABA problem?

A value changes from A to B and then back to A. A client checking only the current value may incorrectly conclude that nothing changed. A version number can detect the intervening updates.

### Q8. Can we guarantee exactly-once execution using a distributed lock?

Not generally. A worker can complete a side effect and crash before recording or acknowledging success. Durable idempotency, transactions, deduplication, and recovery protocols are usually required to provide effectively-once business effects.

## Chapter 7 summary

The key idea is to choose the smallest mechanism that correctly protects the invariant.

- Use atomic SQL for simple conditional state transitions.
- Use CAS and optimistic concurrency to detect stale updates.
- Use constraints to make invalid database states impossible.
- Use idempotency to handle duplicate requests and retries.
- Use queues to serialize asynchronous work where ordering matters.
- Use single-writer designs when one logical writer per resource simplifies correctness.
- Use locks when coordination is genuinely required.

These techniques are not mutually exclusive. Production systems often combine them.

### Next chapter — Chapter 8: Locking in High-Level Design and Production Systems

We'll apply the concepts to real design problems:

- Inventory and flash sales.
- Movie-ticket booking.
- Payment processing.
- Distributed job schedulers.
- Cache stampedes.
- Leader election and scheduled jobs.
- Lock contention, deadlocks, observability, timeouts, and retries.

We'll compare PostgreSQL, Redis, queues, and constraints for each scenario and explain how to defend the design in an interview.

Type `next` to continue.

